/*
 * TclInputStream.java
 *
 * Copyright (c) 2003 Mo DeJong
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclInputStream.java,v 1.2 2006/07/07 23:36:00 mdejong Exp $
 */

// A TclInputStream is a cross between a Java InputStream and
// a Reader. The class supports reading raw bytes as well as
// encoded characters. It manages buffering and supports
// line oriented reading of data. It also supports a user
// configurable EOF marker and line ending translations.

package tcl.lang;

import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;


class TclInputStream {

    /**
     * The Java byte stream object we pull data in from.
     */

    private InputStream input;

    /**
     * If nonzero, use this character as EOF marker.
     */

    private char eofChar;

    /**
     * Flag that is set on each read. If the read encountered EOF
     * or a custom eofChar is found, the it is set to true.
     */

    private boolean eofCond = false;
    private boolean stickyEofCond = false;

    /**
     * Translation mode for end-of-line character
     */

    protected int translation;

    /**
     * Name of Java encoding for this Channel.
     * A null value means use no encoding (binary).
     */

    protected String encoding;

    /**
     * Charset decoder object. A null value means
     * that no conversions have been done yet.
     */

    protected CharsetDecoder csd = null;

    /**
     * Buffering
     */

    protected int buffering;

    /**
     * Blocking
     */

    protected boolean blocking;

    /**
     * Blocked
     */

    protected boolean blocked = false;

    /**
     * Buffer size in bytes
     */

    protected int bufSize;

    /**
     * Used to track EOL state
     */

    protected boolean needNL = false;
    protected boolean sawCR = false;

    protected boolean needMoreData = false;

    /**
     * Flags used to track encoding states.
     * The encodingState member of called inputEncodingState
     * in the C ChannelState type. The encodingStart and encodingEnd
     * members combined are called inputEncodingFlags
     * and have the bit values TCL_ENCODING_END and TCL_ENCODING_START.
     */

    Object  encodingState = null;
    boolean encodingStart = true;
    boolean encodingEnd = false;

    /**
     * First and last buffers in the input queue.
     */

    ChannelBuffer inQueueHead = null;
    ChannelBuffer inQueueTail = null;
    ChannelBuffer saveInBuf = null;

    /**
     * Constructor for Tcl input stream class. We require
     * a byte stream source at init time, the stram can't
     * be changed after the TclInputStream is created.
     */

    TclInputStream(InputStream inInput) {
        input = inInput;
    }

    // Helper used by getsObj and filterBytes

    private class GetsState {
        TclObject obj;
        //int dst;
        String encoding;
        ChannelBuffer buf;
        Object state;
        IntPtr rawRead = new IntPtr();
        //IntPtr bytesWrote = new IntPtr();
        IntPtr charsWrote = new IntPtr();
        int totalChars;
    }

    /**
     * Tcl_GetsObj -> getsObj
     *
     * Accumulate input from the input channel until end-of-line or
     * end-of-file has been seen.  Bytes read from the input channel
     * are converted to Unicode using the encoding specified by the
     * channel.
     *
     * Returns the number of characters accumulated in the object
     * or -1 if error, blocked, or EOF. If -1, use Tcl_GetErrno()
     * to retrieve the POSIX error code for the error or condition
     * that occurred.
     *
     * FIXME: Above setting of error code is not fully implemented.
     *
     * Will consume input from the channel.
     * On reading EOF, leave channel at EOF char.
     * On reading EOL, leave channel after EOL, but don't
     * return EOL in dst buffer.
     */

    int getsObj(TclObject obj) {
        GetsState gs;
        ChannelBuffer buf;
        boolean oldEncodingStart, oldEncodingEnd;
        int oldRemoved, skip, inEofChar;
        int copiedTotal, oldLength;
        boolean in_binary_encoding = false;
        int dst, dstEnd, eol, eof;
        Object oldState;
        final boolean debug = false;

        buf = inQueueHead;
        //encoding = this.encoding;

        // Preserved so we can restore the channel's state in case we don't
        // find a newline in the available input.

        oldLength = 0;
        oldEncodingStart = encodingStart;
        oldEncodingEnd = encodingEnd;
        oldState = encodingState;
        oldRemoved = buf.BUFFER_PADDING;
        if (buf != null) {
            oldRemoved = buf.nextRemoved;
        }

        // If there is no encoding, use "iso8859-1" -- readLine() doesn't
        // produce ByteArray objects.

        if (encoding == null) {
            in_binary_encoding = true;
            encoding = EncodingCmd.getJavaName("iso8859-1");
        }

        if (debug) {
            System.out.println("getsObj encoding is " + encoding);
        }

        // Object used by filterBytes to keep track of how much data has
        // been consumed from the channel buffers.

        gs = new GetsState();
        gs.obj = obj;
        //gs.dst = &dst;
        gs.encoding = encoding;
        gs.buf = buf;
        gs.state = oldState;
        gs.rawRead.i = 0;
        //gs.bytesWrote.i = 0;
        gs.charsWrote.i = 0;
        gs.totalChars = 0;

        // Ensure that tobj is an empty TclString object.
        // Cheat a bit and grab the StringBuffer out of
        // the TclString so we can query the data that
        // was just added to the buffer.
        TclString.empty(obj);
        StringBuffer obj_sbuf =  ((TclString) obj.getInternalRep()).sbuf;

        dst = 0;
        dstEnd = dst;

        skip = 0;
        eof = -1;
        inEofChar = eofChar;

        // Used to implement goto like functionality for restore
        // and goteol loop terminaltion blocks.

        boolean restore = false;
        boolean goteol = false;

        // This is just here so that eol and copiedTotal are
        // definitely assigned before the try block.
        eol = -1;
        copiedTotal = -1;

        restore_or_goteol: {
            while (true) {
                if (dst >= dstEnd) {
                    if (filterBytes(gs) != 0) {
                        restore = true;
                        break restore_or_goteol; //goto restore
                    }
                    dstEnd += gs.charsWrote.i; // dstEnd = dst + gs.bytesWrote;
                }

                // Remember if EOF char is seen, then look for EOL anyhow, because
                // the EOL might be before the EOF char.

                if (inEofChar != '\0') {
                    for (eol = dst; eol < dstEnd; eol++) {
                        if (obj_sbuf.charAt(eol) == inEofChar) {
                            dstEnd = eol;
                            eof = eol;
                            break;
                        }
                    }
                }

                // On EOL, leave current file position pointing after the EOL, but
                // don't store the EOL in the output string.

                switch (translation) {
                    case TclIO.TRANS_LF: {
                        for (eol = dst; eol < dstEnd; eol++) {
                            if (obj_sbuf.charAt(eol) == '\n') {
                                skip = 1;
                                goteol = true;
                                break restore_or_goteol; //goto goteol
                            }
                        }
                        break;
                    }
                    case TclIO.TRANS_CR: {
                        for (eol = dst; eol < dstEnd; eol++) {
                            if (obj_sbuf.charAt(eol) == '\r') {
                                skip = 1;
                                goteol = true;
                                break restore_or_goteol; //goto goteol
                            }
                        }
                        break;
                    }
                    case TclIO.TRANS_CRLF: {
                        for (eol = dst; eol < dstEnd; eol++) {
                            if (obj_sbuf.charAt(eol) == '\r') {
                                eol++;

                                // If a CR is at the end of the buffer,
                                // then check for a LF at the begining
                                // of the next buffer.

                                if (eol >= dstEnd) {
                                    //int offset;
			    
                                    //offset = eol - objPtr->bytes;
                                    dst = dstEnd;
                                    if (filterBytes(gs) != 0) {
                                        restore = true;
                                        break restore_or_goteol; //goto restore
                                    }
                                    dstEnd += gs.charsWrote.i; // dstEnd = dst + gs.bytesWrote
                                    //eol = objPtr->bytes + offset;
                                    if (eol >= dstEnd) {
                                        skip = 0;
                                        goteol = true;
                                        break restore_or_goteol; //goto goteol
                                    }
                                }
                                if (obj_sbuf.charAt(eol) == '\n') {
                                    eol--;
                                    skip = 2;
                                    goteol = true;
                                    break restore_or_goteol; //goto goteol
                                }
                            }
                        }
                        break;
                    }
                    case TclIO.TRANS_AUTO: {
                        eol = dst;
                        skip = 1;
                        if (sawCR) {
                            sawCR = false;
                            if ((eol < dstEnd) && (obj_sbuf.charAt(eol) == '\n')) {
                                // Skip the raw bytes that make up the '\n'.

                                char[] tmp = new char[1];
                                IntPtr rawRead = new IntPtr();

                                buf = gs.buf;
                                // FIXME: Should gs.state be passed here?
                                externalToUnicode(buf.buf, buf.nextRemoved, gs.rawRead.i,
                                        tmp, 0, 1,
                                        rawRead, null, null);
                                buf.nextRemoved += rawRead.i;
                                gs.rawRead.i -= rawRead.i;
                                //gs.bytesWrote.i--;
                                gs.charsWrote.i--;
                                obj_sbuf.deleteCharAt(dst);
                                dstEnd--;
                            }
                        }
                        for (eol = dst; eol < dstEnd; eol++) {
                            if (obj_sbuf.charAt(eol) == '\r') {
                                eol++;
                                if (eol == dstEnd) {
                                    // If buffer ended on \r, peek ahead to see if a
                                    // \n is available.

                                    //int offset;
                                    //IntPtr dstEndPtr = new IntPtr();

                                    //offset = eol /* - objPtr->bytes*/;
                                    dst = dstEnd;

                                    // FIXME: Why does this peek in AUTO mode
                                    // but filter in CRLF mode?
                                    peekAhead(/*dstEndPtr,*/ gs);
                                    //dstEnd = dstEndPtr.i;
                                    dstEnd += gs.charsWrote.i;
                                    //eol = /*objPtr->bytes + */ offset;
                                    if (eol >= dstEnd) {
                                        eol--;
                                        sawCR = true;
                                        goteol = true; 
                                        break restore_or_goteol; //goto goteol
                                    }
                                }
                                if (obj_sbuf.charAt(eol) == '\n') {
                                    skip++;
                                }
                                eol--;
                                goteol = true; //goto goteol
                                break restore_or_goteol;
                            } else if (obj_sbuf.charAt(eol) == '\n') {
                                goteol = true;
                                break restore_or_goteol; //goto goteol
                            }
                        }
                    }
                }
                if (eof != -1) {
                    // EOF character was seen.  On EOF, leave current file position
                    // pointing at the EOF character, but don't store the EOF
                    // character in the output string.

                    dstEnd = eof;
                    eofCond = true;
                    stickyEofCond = true;
                    encodingEnd = true;
                }
                if (eofCond) {
                    skip = 0;
                    eol = dstEnd;
                    if (eol == /*objPtr->bytes + */ oldLength) {
                        // If we didn't append any bytes before encountering EOF,
                        // caller needs to see -1.

                        obj_sbuf.setLength(oldLength);
                        commonGetsCleanup();
                        copiedTotal = -1;
                        break restore_or_goteol; //goto done
                    }
                    goteol = true;
                    break restore_or_goteol; //goto goteol
                }
                dst = dstEnd;
            }
	} // end restore_or_goteol: block

        if (goteol) {
            // Found EOL or EOF, but the output buffer may now contain too many
            // characters.  We need to know how many raw bytes correspond to
            // the number of characters we want, plus how many raw bytes
            // correspond to the character(s) making up EOL (if any), so we can
            // remove the correct number of bytes from the channel buffer.

            int linelen = eol - dst + skip;
            char[] tmp = new char[linelen];

            buf = gs.buf;
            encodingState = gs.state;
            externalToUnicode(buf.buf, buf.nextRemoved, gs.rawRead.i,
                    tmp, 0, linelen,
                    gs.rawRead, null, gs.charsWrote);
            buf.nextRemoved += gs.rawRead.i;

            // Recycle all the emptied buffers.

            obj_sbuf.setLength(eol /* - objPtr->bytes*/);
            commonGetsCleanup();
            blocked = false;
            copiedTotal = gs.totalChars + gs.charsWrote.i - skip;
        }
        if (restore) {
            // Couldn't get a complete line.  This only happens if we get a error
            // reading from the channel or we are non-blocking and there wasn't
            // an EOL or EOF in the data available.

            buf = inQueueHead;
            buf.nextRemoved = oldRemoved;

            for (buf = buf.next; buf != null; buf = buf.next) {
                buf.nextRemoved = buf.BUFFER_PADDING;
            }
            commonGetsCleanup();

            encodingState = oldState;
            encodingStart = oldEncodingStart;
            encodingEnd = oldEncodingEnd;
            obj_sbuf.setLength(oldLength);

            // We didn't get a complete line so we need to indicate to UpdateInterest
            // that the gets blocked.  It will wait for more data instead of firing
            // a timer, avoiding a busy wait.  This is where we are assuming that the
            // next operation is a gets.  No more file events will be delivered on 
            // this channel until new data arrives or some operation is performed
            // on the channel (e.g. gets, read, fconfigure) that changes the blocking
            // state.  Note that this means a file event will not be delivered even
            // though a read would be able to consume the buffered data.

            needMoreData = true;
            copiedTotal = -1;
        }

        // Update the notifier state so we don't block while there is still
        // data in the buffers.

        //done:
        // Reset original encoding in case it was set to binary
        if (in_binary_encoding) {
            encoding = null;
        }

        updateInterest();

        // FIXME: copiedTotal seems to be returning incorrect values
        // for some tests, need to make caller code use the return
        // value instead of the length of the returned object before
        // these errors can be detected by the test suite.
        return copiedTotal;
    }

    /**
     * FilterInputBytes -> filterBytes
     *
     * Helper function for getsObj. Appends Unicode characters
     * onto the TclObject associated with the GetsState after
     * converting them from raw bytes encoded in the Channel.
     * 
     * Consumes available bytes from channel buffers.  When channel
     * buffers are exhausted, reads more bytes from channel device into
     * a new channel buffer.  It is the caller's responsibility to
     * free the channel buffers that have been exhausted.
     *
     * The return value is -1 if there was an error reading from the
     * channel, 0 otherwise.
     *
     * FIXME: Doc modification of object's StringBuffer
     *
     * Status object keeps track of how much data from channel buffers
     * has been consumed and where characters should be stored.
     */

    int filterBytes(GetsState gs) {
        ChannelBuffer buf;
        byte[] raw;
        int rawStart, rawEnd;
        char[] dst;
        int offset, toRead, /*dstNeeded,*/ spaceLeft, result, rawLen, length;
        TclObject obj;
        final int ENCODING_LINESIZE = 20; // Lower bound on how many bytes
                                          // to convert at a time. Since we
                                          // don't know a priori how many
                                          // bytes of storage this many
                                          // source bytes will use, we
                                          // actually need at least
                                          // ENCODING_LINESIZE bytes of room.

        boolean goto_read = false;        // Set to true when jumping to the read
                                          // label, used to simulate a goto.

        final boolean debug = false;

        obj = gs.obj;

        // Subtract the number of bytes that were removed from channel buffer
        // during last call.

        buf = gs.buf;
        if (buf != null) {
            buf.nextRemoved += gs.rawRead.i;
            if (buf.nextRemoved >= buf.nextAdded) {
                buf = buf.next;
            }
        }
        gs.totalChars += gs.charsWrote.i;

    read: while (true) {
        if (goto_read || (buf == null) || (buf.nextAdded == buf.BUFFER_PADDING)) {
            // All channel buffers were exhausted and the caller still hasn't
            // seen EOL.  Need to read more bytes from the channel device.
            // Side effect is to allocate another channel buffer.

            //read:
            if (blocked) {
                if (!blocking) {
                    gs.charsWrote.i = 0;
                    gs.rawRead.i = 0;
                    return -1;
                }
                blocked = false;
            }
            if (getInput() != 0) {
                gs.charsWrote.i = 0;
                gs.rawRead.i = 0;
                return -1;
            }
            buf = inQueueTail;
            gs.buf = buf;
        }

        // Convert some of the bytes from the channel buffer to characters.
        // Space in obj's string rep is used to hold the characters.

        rawStart = /* bufPtr->buf + */ buf.nextRemoved;
        raw = buf.buf;
        rawEnd = /* bufPtr->buf + */ buf.nextAdded;
        rawLen = rawEnd - rawStart;

        //dst = *gsPtr->dstPtr;
        //offset = dst - objPtr->bytes;
        toRead = ENCODING_LINESIZE;
        if (toRead > rawLen) {
            toRead = rawLen;
        }
        //dstNeeded = toRead * TCL_UTF_MAX + 1;
        //spaceLeft = objPtr->length - offset - TCL_UTF_MAX - 1;
        //if (dstNeeded > spaceLeft) {
        //    length = offset * 2;
        //    if (offset < dstNeeded) {
        //        length = offset + dstNeeded;
        //    }
        //    length += TCL_UTF_MAX + 1;
        //    Tcl_SetObjLength(objPtr, length);
        //    spaceLeft = length - offset;
        //    dst = objPtr->bytes + offset;
        //    *gsPtr->dstPtr = dst;
        //}
        dst = new char[toRead];
        gs.state = encodingState;
        result = externalToUnicode(raw, rawStart, rawLen,
                         dst, 0, toRead,
                         gs.rawRead, /*gs.bytesWrote*/ null, gs.charsWrote);
        TclString.append(gs.obj, dst, 0, gs.charsWrote.i);

        if (debug) {
            System.out.println("filterBytes chars");

            String srep = gs.obj.toString();
            int len = srep.length();

            for (int i=0; i < len; i++) {
                char c = srep.charAt(i);
                String prep;
                if (c == '\r') {
                    prep = "\\r";
                } else if (c == '\n') {
                    prep = "\\n";
                } else {
                    prep = "" + c;
                }

                System.out.println("filtered[" + i + "] = '" +
                    prep +
                    "'  (int) " + ((int) srep.charAt(i)) +
                    " 0x" + Integer.toHexString((int) srep.charAt(i)));
            }
        }

        // Make sure that if we go through 'gets', that we reset the
        // TCL_ENCODING_START flag still. [Bug #523988]

        encodingStart = false;

        if (result == TCL_CONVERT_MULTIBYTE) {
            // The last few bytes in this channel buffer were the start of a
            // multibyte sequence.  If this buffer was full, then move them to
            // the next buffer so the bytes will be contiguous.  

            if (debug) {
                System.out.println("TCL_CONVERT_MULTIBYTE decode result found");
            }

            ChannelBuffer next;
            int extra;

            next = buf.next;
            if (buf.nextAdded < buf.bufLength) {
                if (gs.rawRead.i > 0) {
                    // Some raw bytes were converted to UTF-8.  Fall through,
                    // returning those UTF-8 characters because a EOL might be
                    // present in them.
                } else if (eofCond) {
                    // There was a partial character followed by EOF on the
                    // device.  Fall through, returning that nothing was found.

                    buf.nextRemoved = buf.nextAdded;
                } else {
                    // There are no more cached raw bytes left.  See if we can
                    // get some more.

                    goto_read = true;
                    continue read; //goto read;
                }
            } else {
                if (next == null) {
                    next = new ChannelBuffer(bufSize);
                    buf.next = next;
                    inQueueTail = next;
                }
                extra = rawLen - gs.rawRead.i;
                System.arraycopy(raw, rawStart + gs.rawRead.i,
                        next.buf, buf.BUFFER_PADDING - extra, extra);
                next.nextRemoved -= extra;
                buf.nextAdded -= extra;

                if (debug) {
                    System.out.println("copied " + extra + " bytes to " +
                        "next ChannelBuffer");
                }
            }
        }

        break read; // End loop in the normal case
    } // End read labeled while loop

        gs.buf = buf;
        return 0;
    }

    /**
     * PeekAhead -> peekAhead
     *
     * Helper function used by getsObj.  Called when we've seen a
     * \r at the end of the string and want to look ahead one
     * character to see if it is a \n.
     *
     * Characters read from the channel are appended to gs.obj
     * via the filterBytes method.
     */

    void peekAhead(GetsState gs) {
        ChannelBuffer buf;
        //Tcl_DriverBlockModeProc *blockModeProc;
        int bytesLeft;
        boolean goto_cleanup = false;  // Set to true when jumping to the
                                       // cleanup label, used to simulate a goto.

        buf = gs.buf;

        // If there's any more raw input that's still buffered, we'll peek into
        // that.  Otherwise, only get more data from the channel driver if it
        // looks like there might actually be more data.  The assumption is that
        // if the channel buffer is filled right up to the end, then there
        // might be more data to read.

        cleanup:{
            //blockModeProc = NULL;
            if (buf.next == null) {
                bytesLeft = buf.nextAdded - (buf.nextRemoved + gs.rawRead.i);
                if (bytesLeft == 0) {
                    if (buf.nextAdded < buf.bufLength) {
                        // Don't peek ahead if last read was short read.
                        goto_cleanup = true;
                        break cleanup;
                    }
                    // FIXME: This non-blocking check is currently disabled, non-blocking
                    // is not currently supported and it is not clean why we would
                    // need to depend on non-blocking IO when peeking anyway.
                    if (blocking) {
                        //blockModeProc = Tcl_ChannelBlockModeProc(chanPtr->typePtr);
                        if (false /*blockModeProc == NULL*/) {
                            // Don't peek ahead if cannot set non-blocking mode.
                            goto_cleanup = true;
                            break cleanup;
                        }
                        //StackSetBlockMode(chanPtr, TCL_MODE_NONBLOCKING);
                    }
                }
            }
            //if (filterBytes(gs) == 0) {
            //    dstEndPtr.i = gs.charsWrote.i; // *gsPtr->dstPtr + gs.bytesWrote.i
            //}
            filterBytes(gs);
            //if (blockModeProc != NULL) {
            //    StackSetBlockMode(chanPtr, TCL_MODE_BLOCKING);
            //}
        }

        if (goto_cleanup) {
            buf.nextRemoved += gs.rawRead.i;
            gs.rawRead.i = 0;
            gs.totalChars += gs.charsWrote.i;
            //gs.bytesWrote.i = 0;
            gs.charsWrote.i = 0;
        }
    }

    /**
     * CommonGetsCleanup -> commonGetsCleanup
     *
     * Helper function used by getsObj to restore the channel after
     * a "gets" operation.
     *
     */

    void commonGetsCleanup() {
        ChannelBuffer buf, next;

        buf = inQueueHead;
        for ( ; buf != null; buf = next) {
            next = buf.next;
            if (buf.nextRemoved < buf.nextAdded) {
                break;
            }
            recycleBuffer(buf, false);
        }
        inQueueHead = buf;
        if (buf == null) {
            inQueueTail = null;
        } else {
            // If any multi-byte characters were split across channel buffer
            // boundaries, the split-up bytes were moved to the next channel
            // buffer by filterBytes().  Move the bytes back to their
            // original buffer because the caller could change the channel's
            // encoding which could change the interpretation of whether those
            // bytes really made up multi-byte characters after all.

             next = buf.next;
             for ( ; next != null; next = buf.next) {
                 int extra;

                 extra = buf.bufLength - buf.nextAdded;
                 if (extra > 0) {
                     System.arraycopy(next.buf, buf.BUFFER_PADDING - extra,
                             buf.buf, buf.nextAdded, extra);
                     buf.nextAdded += extra;
                     next.nextRemoved = buf.BUFFER_PADDING;
                 }
                 buf = next;
             }
         }
         if (encoding != null) {
             //Tcl_FreeEncoding(encoding);
         }
    }

    // CloseChannel -> close

    void close() throws IOException {
        discardQueued(true);
        // FIXME: More close logic in CloseChannel
    }

    boolean eof() {
        return eofCond;
    }

    void setEncoding(String inEncoding) {
        encoding = inEncoding;
    }

    void setEofChar(char inEofChar) {
        eofChar = inEofChar;
    }

    void setTranslation(int inTranslation) {
        translation = inTranslation;
    }

    void setBuffering(int inBuffering) {
        buffering = inBuffering;
    }

    void setBufferSize(int inBufSize) {
        bufSize = inBufSize;
    }

    void setBlocking(boolean inBlocking) {
        blocking = inBlocking;
    }

    boolean isBlocked() {
        return blocked;
    }

    boolean sawCR() {
        return sawCR;
    }

    // Helper class to implement integer pass by reference
    // for methods like doReadChars, readBytes and so on.

    private class IntPtr {
        int i;

        IntPtr() {}

        IntPtr(int value) {
            i = value;
        }
    }

    /**
     * DoReadChars -> doReadChars
     *
     * Reads from the channel until the requested number of characters
     * have been seen, EOF is seen, or the channel would block.  EOL
     * and EOF translation is done.  If reading binary data, the raw
     * bytes are wrapped in a Tcl byte array object.  Otherwise, the raw
     * bytes are converted to characters using the channel's current
     * encoding and stored in a Tcl string object.
     *
     * @param obj Input data is stored in this object.
     * @param toRead Maximum number of characters to store,
     *               or -1 to read all available data (up to EOF
     *               or when channel blocks).
     */

    int doReadChars(TclObject obj, int toRead)
           throws IOException {
        final boolean debug = false;
        ChannelBuffer buf;
        int copied, copiedNow, result;
        IntPtr offset = new IntPtr();

        if (encoding == null) {
            TclByteArray.setLength(null, obj, 0);
        } else {
            TclString.empty(obj);
        }
        offset.i = 0;

        // if toRead is negative, read until EOF
        if (toRead < 0) {
            toRead = Integer.MAX_VALUE;
        }

        done: {
            for (copied = 0; toRead > 0; ) {
                copiedNow = -1;
                if (inQueueHead != null) {
                    if (encoding == null) {
                        if (debug) { System.out.println("calling readBytes " + toRead); }
                        copiedNow = readBytes(obj, toRead, offset);
                    } else {
                        if (debug) { System.out.println("calling readChars " + toRead); }
                        copiedNow = readChars(obj, toRead);
                    }

                    // If the current buffer is empty recycle it.

                    buf = inQueueHead;
                    if (debug) {
                        System.out.println("after read* buf.nextRemoved is " + buf.nextRemoved);
                        System.out.println("after read* buf.nextAdded is " + buf.nextAdded);
                    }
                    if (buf.nextRemoved == buf.nextAdded) {
                        if (debug) { System.out.println("recycling empty buffer"); }
                        ChannelBuffer next;

                        next = buf.next;
                        recycleBuffer(buf, false);
                        inQueueHead = next;
                        if (next == null) {
                            if (debug) { System.out.println("inQueueTail set to null"); }
                            inQueueTail = null;
                        } else {
                            if (debug) { System.out.println("inQueueTail is not null"); }
                        }
                    }
                }
                if (copiedNow < 0) {
		    if (debug) { System.out.println("copiedNow < 0"); }
                    if (eofCond) {
                        if (debug) { System.out.println("eofCond"); }
                        break;
                    }
                    if (blocked) {
                        if (debug) { System.out.println("blocked"); }
                        if (!blocking) {
                            break;
                        }
                        blocked = false;
                    }
                    result = getInput();
                    if (result != 0) {
                        if (debug) { System.out.println("non-zero result"); }
                        if (result == TclPosixException.EAGAIN) {
                            break;
                        }
                        copied = -1;
                        break done; //goto done
                    }
                } else {
                    copied += copiedNow;
                    if (debug) { System.out.println("copied incremented to " + copied); }
                    toRead -= copiedNow;
                    if (debug) { System.out.println("toRead decremented to " + toRead); }
                }
            }

            blocked = false;

            if (encoding == null) {
                TclByteArray.setLength(null, obj, offset.i);
                if (debug) {
                    System.out.println("set byte array length to " + offset.i);
                }
            }
        } // end done: block

        //done:
        updateInterest();

        if (debug) {
            System.out.println("returning copied = " + copied);
            System.out.println("returning string \"" + obj + "\"");
            obj.invalidateStringRep();
            System.out.println("returning string \"" + obj + "\"");
        }

        return copied;
    }

    /**
     * ReadBytes -> readBytes
     *
     * Reads from the channel until the requested number of
     * bytes have been seen, EOF is seen, or the channel would
     * block. Bytes from the channel are stored in obj as a
     * ByteArray object.  EOL and EOF translation are done.
     *
     * 'bytesToRead' can safely be a very large number because
     * space is only allocated to hold data read from the channel
     * as needed.
     * 
     * The return value is the number of bytes appended to
     * the object.
     *
     * @param obj, the TclByteArrayObject we are operating on
     * @param bytesToRead, Maximum number of bytes to store.
     *                     Bytes are obtained from the first
     *                     buffer in the queue -- even if this number
     *                     is larger than the number of bytes only
     *                     the bytes from the first buffer are returned.
     * @param offsetPtr    On input, contains how many bytes of
     *                     obj have been used to hold data. On
     *                     output, how many bytes are now being used.
     */

    int readBytes(TclObject obj, int bytesToRead, IntPtr offsetPtr) {
        final boolean debug = false;
        int toRead, srcOff, srcLen, offset, length;
        ChannelBuffer buf;
        IntPtr srcRead, dstWrote;
        byte[] src, dst;

        offset = offsetPtr.i;
 
        buf = inQueueHead;
        src = buf.buf;
        srcOff = buf.nextRemoved;
        srcLen = buf.nextAdded - buf.nextRemoved;
        if (debug) {
            System.out.println("readBytes() : src buffer len is " + buf.buf.length);
            System.out.println("readBytes() : buf.nextRemoved is " + buf.nextRemoved);
            System.out.println("readBytes() : buf.nextAdded is " + buf.nextAdded);
        }

        toRead = bytesToRead;
        if (toRead > srcLen) {
	    toRead = srcLen;
            if (debug)
                System.out.println("readBytes() : toRead set to " + toRead);
        }

        length = TclByteArray.getLength(null, obj);
        dst = TclByteArray.getBytes(null, obj);
        if (debug) {
            System.out.println("readBytes() : toRead is " + toRead);
            System.out.println("readBytes() : length is " + length);
            System.out.println("readBytes() : array length is " + dst.length);
        }

        if (toRead > length - offset - 1) {
            if (debug) {
                System.out.println("readBytes() : TclObject too small");
            }

            // Double the existing size of the object or make enough room to
            // hold all the characters we may get from the source buffer,
            // whichever is larger.

            length = offset * 2;
            if (offset < toRead) {
                length = offset + toRead + 1;
            }
            dst = TclByteArray.setLength(null, obj, length);
        }

        if (needNL) {
            needNL = false;
            if ((srcLen == 0) || (src[srcOff] != '\n')) {
                dst[offset] = (byte) '\r';
                offsetPtr.i += 1;
                return 1;
            }
            dst[offset++] = (byte) '\n';
            srcOff++;
            srcLen--;
            toRead--;
        }

        srcRead = new IntPtr(srcLen);
        dstWrote = new IntPtr(toRead);

        if (translateEOL(dst, offset, src, srcOff, dstWrote, srcRead) != 0) {
            if (dstWrote.i == 0) {
                return -1;
            }
        }

        buf.nextRemoved += srcRead.i;
        offsetPtr.i += dstWrote.i;
        return dstWrote.i;
    }

    /**
     * ReadChars -> readChars
     *
     * Reads from the channel until the requested number of
     * characters have been seen, EOF is seen, or the channel would
     * block.  Raw bytes from the channel are converted to characters
     * and stored in obj.  EOL and EOF translation is done.
     *
     * 'charsToRead' can safely be a very large number because
     * space is only allocated to hold data read from the channel
     * as needed.
     * 
     * The return value is the number of characters appended to
     * the object.
     *
     * @param obj, the TclByteArrayObject we are operating on
     * @param charsToRead, Maximum number of chars to store.
     *                     Chars are obtained from the first
     *                     buffer in the queue -- even if this number
     *                     is larger than the number of chars only
     *                     the chars from the first buffer are returned.
     */

    int readChars(TclObject obj, int charsToRead)
            throws IOException {
        final boolean debug = false;

        int toRead, factor, spaceLeft, length, srcLen, dstNeeded;
        int srcOff, dstOff;
        IntPtr srcRead, numChars, dstRead, dstWrote;
        ChannelBuffer buf;
        byte[] src;
        char[] dst;

        Object oldState;

        if (debug) {
            System.out.println("readChars(tobj, " + charsToRead + ")");
        }

        srcRead = new IntPtr();
        numChars = new IntPtr();
        dstRead = new IntPtr();
        dstWrote = new IntPtr();

        buf = inQueueHead; 
        src = buf.buf;
        srcOff =  buf.nextRemoved;
        srcLen = buf.nextAdded - buf.nextRemoved;

        /* FIXME: Include final Tcl patch for srcLen == 0 case */

        if (srcLen == 0) {
            if (debug) {
                System.out.println("srcLen is zero, checking for needNL");
            }

            if (needNL) {
                TclString.append(obj, "\r");
                return 1;
            }
            return -1;
        }

        toRead = charsToRead;
        if (toRead > srcLen) {
	    toRead = srcLen;
        }

        // FIXME : Do something to cache conversion buffer, or it might also
        // to pass the TclObject directly into the externalToUnicode method
        // so as to avoid the need for this extra buffer.
        dstNeeded = toRead;
        dst = new char[dstNeeded];
        dstOff = 0;

        oldState = encodingState;
        if (needNL) {
            // We want a '\n' because the last character we saw was '\r'.
            needNL = false;

            externalToUnicode(src, srcOff, srcLen,
                              dst, dstOff, 1,
                              srcRead, dstWrote, numChars);
            if ((numChars.i > 0) && (dst[dstOff] == '\n')) {
                // The next char was a '\n'.  Consume it and produce a '\n'.
                buf.nextRemoved += srcRead.i;
            } else {
                // The next char was not a '\n'.  Produce a '\r'.
                dst[dstOff] = '\r';
            }
            encodingStart = false;
            TclString.append(obj, dst, dstOff, 1);
            return 1;
        }

        externalToUnicode(src, srcOff, srcLen,
                          dst, dstOff, dstNeeded,
                          srcRead, dstWrote, numChars);

        if (srcRead.i == 0) {
            // Not enough bytes in src buffer to make a complete char.  Copy
            // the bytes to the next buffer to make a new contiguous string,
            // then tell the caller to fill the buffer with more bytes.

            ChannelBuffer next;

            next = buf.next;
            if (next == null) {
                if (srcLen > 0) {
                    // There isn't enough data in the buffers to complete the next
                    // character, so we need to wait for more data before the next
                    // file event can be delivered.
                    //
                    // SF #478856.
                    //
                    // The exception to this is if the input buffer was
                    // completely empty before we tried to convert its
                    // contents. Nothing in, nothing out, and no incomplete
                    // character data. The conversion before the current one
                    // was complete.

                    needMoreData = true;
                }
                return -1;
            }

            // Space is made at the beginning of the buffer to copy the
            // previous unused bytes there. Check first if the buffer we
            // are using actually has enough space at its beginning for
            // the data we are copying. Because if not we will write over the
            // buffer management information, especially the 'nextPtr'.
            //
            // Note that the BUFFER_PADDING (See AllocChannelBuffer) is
            // used to prevent exactly this situation. I.e. it should
            // never happen. Therefore it is ok to panic should it happen
            // despite the precautions.

            if ((next.nextRemoved - srcLen) < 0) {
                throw new TclRuntimeError("Buffer Underflow, BUFFER_PADDING not enough");
            }
            next.nextRemoved -= srcLen;
            System.arraycopy(src, srcOff, next.buf, next.nextRemoved, srcLen);
            recycleBuffer(buf, false);
            inQueueHead = next;
            return readChars(obj, charsToRead);
        }

        dstRead.i = dstWrote.i;
        if (translateEOL(dst, dstOff, dst, dstOff, dstWrote, dstRead) != 0) {
            // Hit EOF char.  How many bytes of src correspond to where the
            // EOF was located in dst? Run the conversion again with an
            // output buffer just big enough to hold the data so we can
            // get the correct value for srcRead.

            if (dstWrote.i == 0) {
                return -1;
            }
            encodingState = oldState;
            externalToUnicode(src, srcOff, srcLen,
                              dst, dstOff, dstRead.i,
                              srcRead, dstWrote, numChars);
            translateEOL(dst, dstOff, dst, dstOff, dstWrote, dstRead);
        }

	// The number of characters that we got may be less than the number
	// that we started with because "\r\n" sequences may have been
	// turned into just '\n' in dst.

        numChars.i -= (dstRead.i - dstWrote.i);

        if (numChars.i > toRead) {
            // Got too many chars.

            int eof;
            eof = toRead;
            encodingState = oldState;
            externalToUnicode(src, srcOff, srcLen,
                              dst, dstOff, (eof - dstOff),
                              srcRead, dstWrote, numChars);
            dstRead.i = dstWrote.i;
            translateEOL(dst, dstOff, dst, dstOff, dstWrote, dstRead);
            numChars.i -= (dstRead.i - dstWrote.i);
        }
        encodingStart = false;

        buf.nextRemoved += srcRead.i;

        TclString.append(obj, dst, dstOff, numChars.i);

        return numChars.i;
    }

    // FIXME: Only define the ones that we actually need/use.

    // The following definitions are the error codes returned by externalToUnicode
    //
    // TCL.OK:			All characters were converted.
    //
    // TCL_CONVERT_NOSPACE:	The output buffer would not have been large
    //				enough for all of the converted data; as many
    //				characters as could fit were converted though.
    //
    // TCL_CONVERT_MULTIBYTE:	The last few bytes in the source string were
    //				the beginning of a multibyte sequence, but
    //				more bytes were needed to complete this
    //				sequence.  A subsequent call to the conversion
    //				routine should pass the beginning of this
    //				unconverted sequence plus additional bytes
    //				from the source stream to properly convert
    //				the formerly split-up multibyte sequence.
    //
    // TCL_CONVERT_SYNTAX:		The source stream contained an invalid
    //				character sequence.  This may occur if the
    //				input stream has been damaged or if the input
    //				encoding method was misidentified.  This error
    //				is reported only if TCL_ENCODING_STOPONERROR
    //				was specified.
    // 
    // TCL_CONVERT_UNKNOWN:		The source string contained a character
    //				that could not be represented in the target
    //				encoding.  This error is reported only if
    //				TCL_ENCODING_STOPONERROR was specified.

    private final int TCL_CONVERT_MULTIBYTE = -1;
    private final int TCL_CONVERT_SYNTAX = -2;
    private final int TCL_CONVERT_UNKNOWN = -3;
    private final int TCL_CONVERT_NOSPACE = -4;

    /**
     * Tcl_ExternalToUtf -> externalToUnicode
     *
     * Convert a source buffer from the specified encoding into Unicode.
     *
     * FIXME: Add doc for return values
     *
     * @param src,         Source bytes in specified encoding.
     * @param srcOff,      First index in src input array.
     * @param srcLen,      Number of bytes in src buffer.
     * @param dst,         Array to store unicode characters in.
     * @param dstOff,      First available index in dst array.
     * @param dstLen,      Length of dst array.
     * @param srcReadPtr,  Filled with the number of bytes from
     *                     the source string that were converted.
     *                     This may be less than the original source
     *                     length if there was a problem converting
     *                     some source characters.
     * @param dstWrotePtr, Filled with the number of chars that were
     *                     stored in the output buffer as a result of
     *                     the conversion
     * @param dstCharsPtr, Filled with the number of characters that
     *                     correspond to the bytes stored in the
     *                     output buffer.
     */

    int externalToUnicode(byte[] src, int srcOff, int srcLen,
                          char[] dst, int dstOff, int dstLen,
                          IntPtr srcReadPtr, IntPtr dstWrotePtr, IntPtr dstCharsPtr) {
        final boolean debug = false;
        int result = TCL.OK;
        //Object state;

        if (encoding == null) {
            // This should never happen
            throw new TclRuntimeError("externalToUnicode called with null encoding");
        }

        if (debug) {
            System.out.println("externalToUnicode( " + srcLen + " " + dstLen + " )");
        }

        // If decoder was flushed already then return 0.

        if ((srcLen == 0) && !encodingEnd) {
            if (debug) {
                System.out.println("srcLen is zero and encodingEnd is false");
            }

            srcReadPtr.i = 0;
            if (dstWrotePtr != null)
                dstWrotePtr.i = 0;
            if (dstCharsPtr != null)
                dstCharsPtr.i = 0;
            return 0;
        }

        // Convert bytes from src into unicode chars and store them in dst.

        // FIXME: This allocated a buffer for the String and then copies the
        // encoded data into a second buffer. Need to decode the data directly
        // into the dst array since this is performance critical.

        if (debug) {
            System.out.println("now to decode byte array of length " + srcLen);
            System.out.println("srcOff is " + srcOff);
            for (int i=srcOff ; i < (srcOff+srcLen); i++) {
                System.out.println("(byte) '" + ((char) src[i]) + "'" +
                    "  (int) " + ((int) src[i]) +
                    "  (hex) 0x" + Integer.toHexString(src[i]) );
            }
            System.out.println("encoded as " + encoding);
            System.out.println("eofCond is " + eofCond);
        }

        // FIXME: In the cases where we know that we don't actually want
        // to copy the data, we could pass a flag so that we could
        // take advantage of encodings that had a one to one mapping
        // from bytes to chars (now need to copy then to find bytes used).

        if (csd == null) {
            // Note that UnsupportedCharsetException should never be raised
            // here since EncodingCmd.isSupported() should have already
            // returned true for this encoding.

            Charset chrset = Charset.forName(encoding);
            csd = chrset.newDecoder();
        }

        int bytes_read, chars_written;

        // A ByteBuffer wraps the src byte[] and
        // handles buffer size issues. A CharBuffer
        // wraps the dst char[] and handles buffer size.

        ByteBuffer srcb = ByteBuffer.wrap(src, srcOff, srcLen);
        CharBuffer dstb = CharBuffer.wrap(dst, dstOff, dstLen);

        int srcbStartPos = srcb.position();
        int dstbStartPos = dstb.position();
        boolean atEOF = (eofCond && encodingEnd);

        // Note that the default action on bad encoded
        // input is to stop and and report the error.

        CoderResult cresult = csd.decode(srcb, dstb, atEOF);

        bytes_read = srcb.position() - srcbStartPos;
        chars_written = dstb.position() - dstbStartPos;

        if (debug) {
            System.out.println("decoded " + chars_written + " chars from " +
                bytes_read + " bytes");

            for (int i=0 ; i < chars_written; i++) {
                int ind = dstOff + i;
                System.out.println("(char) '" + dst[ind] + "'" +
                    "  (int) " + ((int) dst[ind]) +
                    "  (hex) 0x" + Integer.toHexString(dst[ind]) );
            }
        }

        if (cresult == CoderResult.UNDERFLOW) {
            // Data from src has been decoded. If there are no more
            // bytes then decode is finished. If there are more
            // bytes then another decode operation may be needed.

            if (debug) {
                System.out.println("UNDERFLOW detected");
            }

            if (srcb.remaining() > 0) {
                if (debug) {
                    System.out.println("TCL_CONVERT_MULTIBYTE set");
                }

                result = TCL_CONVERT_MULTIBYTE;
            }
        } else if (cresult == CoderResult.OVERFLOW) {
            // The dst buffer is full. Often we need to
            // do a conversion with a known number of
            // chars so we can find out how many bytes
            // were consumed as a result.

            if (debug) {
                System.out.println("OVERFLOW detected");
            }

            result = TCL_CONVERT_NOSPACE;
        }

        if (atEOF && (result == TCL.OK)) {
            // When EOF is read from the input file, eof flag
            // is passed to the decoder. Flush the decoder
            // as long as the last decode did not fail.

            cresult = csd.flush(dstb);

            if (cresult == CoderResult.OVERFLOW) {
                // The dst buffer is full. Often we need to
                // do a conversion with a known number of
                // chars so we can find out how many bytes
                // were consumed as a result.

                result = TCL_CONVERT_NOSPACE;
            } else {
                // A char may have been flushed, reset
                // the decoder since the caller might
                // attempt to invoke the decode operation
                // again. The decoder should be stateless,
                // so this should cause no harm.

                csd.reset();

                encodingEnd = false;
            }

            int chars_flushed = dstb.position() - chars_written;
            chars_written += chars_flushed;

            if (debug) {
                System.out.println("flushed " + chars_flushed + " chars at EOF");
            }

            // If a partial character is followed by EOF, then
            // set the result to TCL_CONVERT_MULTIBYTE. The
            // earlier logic that checks for UNDERFLOW assumes
            // that some data was decoded, but that does not
            // hold in this case.

            if ((bytes_read == 0) &&
                    (chars_written == 0) &&
                    (srcb.remaining() > 0)) {
                result = TCL_CONVERT_MULTIBYTE;
            }
        }

        srcReadPtr.i = bytes_read;
        if (dstWrotePtr != null)
            dstWrotePtr.i = chars_written;
        if (dstCharsPtr != null)
            dstCharsPtr.i = chars_written;

        return result;
    }

    /**
     * GetInput -> getInput
     *
     * Reads input data from a device into a channel buffer.
     *
     * The return value is the Posix error code if an error occurred while
     * reading from the file, or 0 otherwise.  
     */

    private int getInput() {
        final boolean debug = false;
        int toRead;
        int result;
        int nread;

        // if (checkForDeadChannel()) return EINVAL;

        // Skipped pushback processing code for stacked Channels


        // See if we can fill an existing buffer. If we can, read only
        // as much as will fit in it. Otherwise allocate a new buffer,
        // add it to the input queue and attempt to fill it to the max.

        ChannelBuffer buf = inQueueTail;

        if ((buf != null) && (buf.nextAdded < buf.bufLength)) {
            if (debug) {
                System.out.println("smaller than buffer");
            }
            toRead = buf.bufLength - buf.nextAdded;
	} else {
            if (debug) {
                System.out.println("fits in existing buffer");
            }

            buf = saveInBuf;
            saveInBuf = null;

            // Check the actual buffersize against the requested
            // buffersize. Buffers which are smaller than requested are
            // squashed. This is done to honor dynamic changes of the
            // buffersize made by the user.

            if ((buf != null) && ((buf.bufLength - buf.BUFFER_PADDING) < bufSize)) {
                buf = null;
            }
            if (buf == null) {
                if (debug) {
                    System.out.println("allocated ChannelBuffer of size " + bufSize);
                }
                buf = new ChannelBuffer(bufSize);
            }
            buf.next = null;

            // Use the actual size of the buffer to determine
            // the number of bytes to read from the channel and not the
            // size for new buffers. They can be different if the
            // buffersize was changed between reads.

            toRead = buf.bufLength - buf.nextAdded;
            if (debug) {
                System.out.println("toRead set to " + toRead);
            }

            if (inQueueTail == null) {
                inQueueHead = buf;
            } else {
                inQueueTail.next = buf;
            }

            inQueueTail = buf;
        }

        // If EOF is set, we should avoid calling the driver because on some
        // platforms it is impossible to read from a device after EOF.

        if (eofCond) {
            if (debug) {
                System.out.println("eofCond was true, no error return");
            }
            return 0;
        }

        // FIXME: We do not handle non-blocking or this CHANNEL_TIMER_FEV flag yet

        if (/*CHANNEL_TIMER_FEV &&*/
                !blocking) {
            return TclPosixException.EWOULDBLOCK;
        } else {
            result = 0;

            // Can we even use this for a brain-dead nonblocking IO check?
            int numAvailable = /*input.available();*/ 0;

            if (!blocking && (numAvailable < toRead)) {
                result = TclPosixException.EWOULDBLOCK;
                nread = -1;
            } else {
                try {
                    if (debug) {
                        System.out.println("now to read " + toRead + " bytes");
                    }

                    nread = input.read(buf.buf, buf.nextAdded, toRead);

                    // read() returns -1 on EOF
                    if (nread == -1) {
                        if (debug) {
                            System.out.println("got EOF from read() call");
                        }
                        nread = 0;
                    }
                } catch (IOException ex) {
                    // FIXME: How do we recover from IO errors here?
                    // I think we need to set result to a POSIX error
                    ex.printStackTrace(System.err);
                    nread = -1;
                }
            }
        }

        if (nread > 0) {
            if (debug) {
                System.out.println("nread is " + nread);
            }
            buf.nextAdded += nread;

            // should avoid calling the driver because on some platforms we
            // will block in the low level reading code even though the
            // channel is set into nonblocking mode.

            if (nread < toRead) {
                blocked = true;
            }
        } else if (nread == 0) {
            eofCond = true;
            encodingEnd = true;
            if (debug) {
                System.out.println("nread is zero, eofCond set, encodingEnd set");
            }
        } else if (nread < 0) {
            if (debug) {
                System.out.println("nread is " + nread);
            }
            if ((result == TclPosixException.EWOULDBLOCK) ||
                    (result == TclPosixException.EAGAIN)) {
                blocked = true;
	        result = TclPosixException.EAGAIN;
	    }
            // FIXME: Called needs to raise a TclException
            //Tcl_SetErrno(result);
            return result;
        }
        if (debug) {
            System.out.println("no error return");
        }
        return 0;
    }

    /**
     * RecycleBuffer -> recycleBuffer
     *
     * Helper function to recycle input buffers. Ensures that
     * two input buffers are saved (one in the input queue and
     * another in the saveInBuf field). Only if these conditions
     * are met is the buffer released so that it can be
     * garbage collected.
     */

    private void recycleBuffer(ChannelBuffer buf, boolean mustDiscard) {

        if (mustDiscard)
            return;

        // Only save buffers which are at least as big as the requested
        // buffersize for the channel. This is to honor dynamic changes
        // of the buffersize made by the user.

        if ((buf.bufLength - buf.BUFFER_PADDING) < bufSize) {
            return;
        }

        if (inQueueHead == null) {
            inQueueHead = buf;
            inQueueTail = buf;

            buf.nextRemoved = buf.BUFFER_PADDING;
            buf.nextAdded = buf.BUFFER_PADDING;
            buf.next = null;
            return;
        }
        if (saveInBuf == null) {
            saveInBuf = buf;

            buf.nextRemoved = buf.BUFFER_PADDING;
            buf.nextAdded = buf.BUFFER_PADDING;
            buf.next = null;
            return;
        }
    }

    /**
     * DiscardInputQueued -> discardQueued
     *
     * Discards any input read from the channel but not yet consumed
     * by Tcl reading commands.
     */

    private void discardQueued(boolean discardSavedBuffers) {
        ChannelBuffer buf, nxt;

        buf = inQueueHead;
        inQueueHead = null;
        inQueueTail = null;
        for (; buf != null; buf = nxt) {
            nxt = buf.next;
            recycleBuffer(buf, discardSavedBuffers);
        }

        // If discardSavedBuffers is true, must also discard any previously
        // saved buffer in the saveInBuf field.
    
        if (discardSavedBuffers) {
            if (saveInBuf != null) {
                saveInBuf = null;
            }
        }
    }

    /**
     * TranslateInputEOL -> translateEOL
     *
     * Perform input EOL and EOF translation on the source buffer,
     * leaving the translated result in the destination buffer.
     *
     * Results:
     * The return value is 1 if the EOF character was found when 
     * copying bytes to the destination buffer, 0 otherwise.  
     *
     * @param dstArray, Output buffer to fill with translated bytes or chars.
     * @param dstStart, First unused index in the dst output array.
     * @param srcArray, Input buffer that holds the bytes or chars to translate
     * @param srcStart, Index of first available byte in src array.

     * @param dstLenPtr, On entry, the maximum length of output
     *                   buffer in bytes or chars; must be <= srcLenPtr.i.  On
     *                   exit, the number of bytes or chars actually used in
     *                   output buffer.
     * @param srcLenPtr, On entry, the length of source buffer.
     *                   On exit, the number of bytes or chars read from
     *                   the source buffer.
     */

    int translateEOL(Object dstArray, int dstStart,
            Object srcArray, int srcStart,
            IntPtr dstLenPtr, IntPtr srcLenPtr) {
        final boolean debug = false;

        // Figure out if the srcArray and dstArray buffers
        // are byte or char arrays.
        boolean isCharType;
        char[] srcArrayChar, dstArrayChar;
        byte[] srcArrayByte, dstArrayByte;

        if ((srcArray instanceof char[]) && (dstArray instanceof char[])) {
            isCharType = true;
            srcArrayChar = (char[]) srcArray;
            dstArrayChar = (char[]) dstArray;
            srcArrayByte = null;
            dstArrayByte = null;
        } else if ((srcArray instanceof byte[]) && (dstArray instanceof byte[])) {
            isCharType = false;
            srcArrayChar = null;
            dstArrayChar = null;
            srcArrayByte = (byte[]) srcArray;
            dstArrayByte = (byte[]) dstArray;
        } else {
            throw new TclRuntimeError("unknown array argument types");
        }

        int dstLen, srcLen, inEofChar, index;
        int eof;

        dstLen = dstLenPtr.i;

        eof = -1;
        inEofChar = eofChar;
        if (inEofChar != '\0') {
            // Find EOF in translated buffer then compress out the EOL.  The
            // source buffer may be much longer than the destination buffer --
            // we only want to return EOF if the EOF has been copied to the
            // destination buffer.

            int src, srcMax;

            srcMax = srcStart + srcLenPtr.i;
            for (src = srcStart; src < srcMax; src++) {
                if (isCharType) {
                    index = srcArrayChar[src];
                } else {
                    index = srcArrayByte[src];
                }
                if (index == inEofChar) {
                    eof = src;
                    srcLen = src - srcStart;
                    if (srcLen < dstLen) {
                        dstLen = srcLen;
                    }
                    srcLenPtr.i = srcLen;
                    break;
                }
            }
        }
        switch (translation) {
            case TclIO.TRANS_LF: {
                if ((dstArray != srcArray) || ((dstArray == srcArray) && (dstStart != srcStart))) {
                    System.arraycopy(srcArray, srcStart, dstArray, dstStart, dstLen);
                }
                srcLen = dstLen;
                break;
            }
            case TclIO.TRANS_CR: {
                int dst, dstEnd;
	    
                if ((dstArray != srcArray) || ((dstArray == srcArray) && (dstStart != srcStart))) {
                    System.arraycopy(srcArray, srcStart, dstArray, dstStart, dstLen);
                }
                dstEnd = dstStart + dstLen;
                if (isCharType) {
                    for (dst = dstStart; dst < dstEnd; dst++) {
                        if (dstArrayChar[dst] == '\r') {
                            dstArrayChar[dst] = '\n';
                        }
                    }
                } else {
                    for (dst = dstStart; dst < dstEnd; dst++) {
                        if (dstArrayByte[dst] == '\r') {
                            dstArrayByte[dst] = (byte) '\n';
                        }
                    }
                }
                srcLen = dstLen;
                break;
            }
            case TclIO.TRANS_CRLF: {
                int dst;
                int src, srcEnd, srcMax;

                dst = dstStart;
                src = srcStart;
                srcEnd = srcStart + dstLen;
                srcMax = srcStart + srcLenPtr.i;

                if (isCharType) {
                    for ( ; src < srcEnd; ) {
                        if (srcArrayChar[src] == '\r') {
                            src++;
                            if (src >= srcMax) {
                                needNL = true;
                            } else if (srcArrayChar[src] == '\n') {
                                dstArrayChar[dst++] = srcArrayChar[src++];
                            } else {
                                dstArrayChar[dst++] = '\r';
                            }
                        } else {
                            dstArrayChar[dst++] = srcArrayChar[src++];
                        }
                    }
                } else {
                    for ( ; src < srcEnd; ) {
                        if (srcArrayByte[src] == '\r') {
                            src++;
                            if (src >= srcMax) {
                                needNL = true;
                            } else if (srcArrayByte[src] == '\n') {
                                dstArrayByte[dst++] = srcArrayByte[src++];
                            } else {
                                dstArrayByte[dst++] = (byte) '\r';
                            }
                        } else {
                            dstArrayByte[dst++] = srcArrayByte[src++];
                        }
                    }
                }

                srcLen = src - srcStart;
                dstLen = dst - dstStart;
                break;
            }
            case TclIO.TRANS_AUTO: {
                int dst;
                int src, srcEnd, srcMax;

                dst = dstStart;
                src = srcStart;
                srcEnd = srcStart + dstLen;
                srcMax = srcStart + srcLenPtr.i;

                if (sawCR && (src < srcMax)) {
                    if (isCharType) {
                        index = srcArrayChar[src];
                    } else {
                        index = srcArrayByte[src];
                    }
                    if (index == '\n') {
                        src++;
                    }
                    sawCR = false;
                }
                if (isCharType) {
                    for ( ; src < srcEnd; ) {
                        if (srcArrayChar[src] == '\r') {
                            src++;
                            if (src >= srcMax) {
                                sawCR = true;
                            } else if (srcArrayChar[src] == '\n') {
                                if (srcEnd < srcMax) {
                                    srcEnd++;
                                }
                                src++;
                            }
                            dstArrayChar[dst++] = '\n';
                        } else {
                            dstArrayChar[dst++] = srcArrayChar[src++];
                        }
                    }
                } else {
                    for ( ; src < srcEnd; ) {
                        if (srcArrayByte[src] == '\r') {
                            src++;
                            if (src >= srcMax) {
                                sawCR = true;
                            } else if (srcArrayByte[src] == '\n') {
                                if (srcEnd < srcMax) {
                                    srcEnd++;
                                }
                                src++;
                            }
                            dstArrayByte[dst++] = (byte) '\n';
                        } else {
                            dstArrayByte[dst++] = srcArrayByte[src++];
                        }
                    }
                }             
                srcLen = src - srcStart;
                dstLen = dst - dstStart;
                break;
            }
            default: {
                throw new TclRuntimeError("invalid translation");
            }
        }
        dstLenPtr.i = dstLen;

        if ((eof != -1) && (srcStart + srcLen >= eof)) {
            // EOF character was seen in EOL translated range.  Leave current
            // file position pointing at the EOF character, but don't store the
            // EOF character in the output string.

            eofCond = true;
            stickyEofCond = true;
            encodingEnd = true;
            sawCR = false;
            needNL = false;
            return 1;
        }

        srcLenPtr.i = srcLen;
        return 0;
    }

    /**
     * UpdateInterest -> updateInterest
     *
     * Arrange for the notifier to call us back at appropriate times
     * based on the current state of the channel.
     */

    void updateInterest() {
        // FIXME: Currently unimplemented
    }

    /**
     * Tcl_InputBuffered -> getNumBufferedBytes
     *
     * Return the number of bytes that are current buffered.
     */

    int getNumBufferedBytes() {
        ChannelBuffer buf;
        int IOQueued;
        for (IOQueued = 0, buf = inQueueHead;
                buf != null; buf = buf.next) {
            IOQueued += buf.nextAdded - buf.nextRemoved;
        }
        return IOQueued;
    }

    /**
     * seekReset
     *
     * Helper method used to reset state info when doing a seek.
     */

    void seekReset() {
        discardQueued(false);
        eofCond = false;
        stickyEofCond = false;
        blocked = false;
        sawCR = false;
        // FIXME: Change needed in Tcl
        //needNL = false;
    }
}
