/*
 * Channel.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Channel.java,v 1.23 2002/01/21 06:34:26 mdejong Exp $
 */

package tcl.lang;
import java.io.*;
import java.util.Hashtable;

/**
 * The Channel class provides functionality that will
 * be needed for any type of Tcl channel. It performs
 * generic reads, writes, without specifying how a
 * given channel is actually created. Each new channel
 * type will need to extend the abstract Channel class
 * and override any methods it needs to provide a
 * specific implementation for.
 */

abstract class Channel {

    /**
     * The read, write, append and create flags are set here.  The 
     * variables used to set the flags are found in the class TclIO.
     */

    protected int mode;

    /**
     * This is a unique name that sub-classes need to set.  It is used
     * as the key in the hashtable of registered channels (in interp).
     */

    private String chanName;

    /**
     * How many interpreters hold references to this IO channel?
     */

    protected int refCount = 0;

    /**
     * Generic Reader/Writer objects. A BufferedReader is required to gain
     * access to line oriented input inside the read() method.
     */

    protected BufferedReader reader = null;
    protected BufferedWriter writer = null;

    /**
     * Flag that is set on each read/write.  If the read/write encountered an EOF
     * then it's set to true, else false.
     */

    protected boolean eofCond = false;

    /**
     * Set to false when channel is in non-blocking mode.
     */
    
    protected boolean blocking = true;

    /**
     * Buffering (full,line, or none)
     */

    protected int buffering = TclIO.BUFF_FULL;

    /**
     * Buffer size, in bytes, allocated for channel to store input or output
     */

    protected int bufferSize = 4096;

    /**
     * Name of Java encoding for this Channel.
     * A null value means use no encoding (binary).
     */

    // FIXME: Check to see if this field is updated after a call
    // to "encoding system $enc" for new Channel objects!

    protected String encoding;
    protected int bytesPerChar;

    /**
     * Translation mode for end-of-line character
     */

    protected int inputTranslation = TclIO.TRANS_AUTO;
    protected int outputTranslation = TclIO.TRANS_PLATFORM;

    /**
     * If nonzero, use this as a signal of EOF on input.
     */

    protected char inEofChar = 0;

    /**
     * If nonzero, append this to a writeable channel on close.
     */

    protected char outEofChar = 0;

    Channel() {
        setEncoding(EncodingCmd.systemJavaEncoding);
    }

    /**
     * Tcl_ReadChars -> read
     *
     * Read data from the Channel.
     * 
     * @param interp is used for TclExceptions.  
     * @param readType pecifies if the read should read the entire
     *     buffer (TclIO.READ_ALL), the next line (TclIO.READ_LINE),
     *     of a specified number of bytes (TclIO.READ_N_BYTES).
     * @param numBytes the number of bytes/chars to read. Used only when
     *     the readType is TclIO.READ_N_BYTES.
     * @return TclObject that holds the read in data.
     * @exception TclException is thrown if read occurs on WRONLY channel.
     * @exception IOException is thrown when an IO error occurs that was not
     *                correctly tested for.  Most cases should be caught.
     */

    TclObject read(Interp interp, int readType, int numBytes) 
            throws IOException, TclException {

        checkRead(interp);

        eofCond = false;

        switch (readType) {
            case TclIO.READ_ALL: {
                char[] charArr = new char[bufferSize];
                StringBuffer sbuf = new StringBuffer(bufferSize);
                int numRead;

                while((numRead = reader.read(charArr, 0, bufferSize)) != -1) {
                    sbuf.append(charArr,0, numRead);
                }
                eofCond = true;
                return TclString.newInstance(sbuf);
            }
            case TclIO.READ_LINE: {
                String line = reader.readLine();
                if (line == null) {
                    eofCond = true;
                    return TclString.newInstance("");
                } else {
                    // FIXME: Is it possible to check for EOF using
                    // reader.ready() when a whole line is returned
                    // but we ran into an EOF (no newline termination)?
                    return TclString.newInstance(line);
                }
            }
            case TclIO.READ_N_BYTES: {
                char[] charArr = new char[numBytes];
                int numRead = reader.read(charArr, 0, numBytes);
                if (numRead == -1) {
                    eofCond = true;
                    return TclString.newInstance("");
                }
                return TclString.newInstance(new String(charArr,0,numRead));
            }
            default : {
                throw new TclRuntimeError(
                    "Channel.read: Invalid read mode.");
            }
        }
    }

    /**
     * Tcl_WriteObj -> write
     *
     * Write data to the Channel
     * 
     * @param interp is used for TclExceptions.  
     * @param outData the TclObject that holds the data to write.
     */

    void write(Interp interp, TclObject outData)
	    throws IOException, TclException {

        checkWrite(interp);

        if (writer != null) {
            String outStr = outData.toString();
            writer.write(outStr);
        }
    }

    /** 
     * Tcl_WriteChars -> write
     *
     * Write string data to the Channel.
     * 
     * @param interp is used for TclExceptions.  
     * @param outStr the String object to write.
     */

    void write(Interp interp, String outStr)
            throws IOException, TclException {
        write(interp, TclString.newInstance(outStr));
    }

    /** 
     * Close the Channel.  The channel is only closed, it is 
     * the responsibility of the "closer" to remove the channel from 
     * the channel table.
     */

    void close() throws IOException {

        IOException ex = null;

        if (reader != null) {
            try { reader.close(); } catch (IOException e) { ex = e; }
            reader = null;
        }

        // If channel has a custom eof marker, write it to the
        // stream before closing. Invoke the write method
        // instead of writing directly to the stream since
        // some channels might not set the writer field.

        if ((outEofChar != 0) && (isWriteOnly() || isReadWrite())) {
            try {
                write(null, String.valueOf(outEofChar));
            } catch (TclException ex2) {
                throw new TclRuntimeError(
		    "Channel.close: TclException while writing eof " +
		    ex2.getMessage());
            }
        }

        if (writer != null) {
            try { writer.close(); } catch (IOException e) { ex = e; }
            writer = null;
        }

        if (ex != null)
            throw ex;
    }

    /** 
     * Flush the Channel.
     *
     * @exception TclException is thrown when attempting to flush a 
     *            read only channel.
     * @exception IOEcception is thrown for all other flush errors.
     */

    void flush(Interp interp) 
            throws IOException, TclException {

        checkWrite(interp);

        if (writer != null) {
            writer.flush();
        }
    }

    /** 
     * Move the current file pointer. If seek is not supported on the
     * given channel then -1 will be returned. A subclass should
     * override this method if it supports the seek operation.
     * 
     * @param interp currrent interpreter.
     * @param offset The number of bytes to move the file pointer.
     * @param mode where to begin incrementing the file pointer; beginning,
     *             current, end.
     */

    void seek(Interp interp, long offset, int mode)
            throws IOException, TclException {
        throw new TclPosixException(interp, TclPosixException.EINVAL, true,
		"error during seek on \"" + getChanName() + "\"");
    }

    /** 
     * Return the current file pointer. If tell is not supported on the
     * given channel then -1 will be returned. A subclass should override
     * this method if it supports the tell operation.
     */

    long tell() throws IOException {
        return (long) -1;
    }

    /**
     * Returns true if the last read reached the EOF.
     */

    final boolean eof() {
        return eofCond;
    }

    /** 
     * Gets the chanName that is the key for the chanTable hashtable.
     * @return channelId
     */

    String getChanName() {
        return chanName;
    }


    /** 
     * Sets the chanName that is the key for the chanTable hashtable.
     * @param chan the unique channelId
     */

    void setChanName(String chan) {
        chanName = chan;
    }

    boolean isReadOnly() {
        return ((mode & TclIO.RDONLY) != 0);
    }

    boolean isWriteOnly() {
        return ((mode & TclIO.WRONLY) != 0);
    }

    boolean isReadWrite() {
        return ((mode & TclIO.RDWR) != 0);
    }
    
    // Helper methods to check read/write permission and raise a
    // TclException if reading is not allowed.

    protected void checkRead(Interp interp) throws TclException {
        if (!isReadOnly() && !isReadWrite()) {
            throw new TclException(interp, "channel \"" + getChanName() +
                "\" wasn't opened for reading");
        }
    }

    protected void checkWrite(Interp interp) throws TclException {
        if (!isWriteOnly() && !isReadWrite()) {
            throw new TclException(interp, "channel \"" + getChanName() +
                "\" wasn't opened for writing");
        }
    }

    /** 
     * Query blocking mode.
     */

    boolean getBlocking() {
        return blocking;
    }


    /** 
     * Set blocking mode.
     *
     * @param blocking new blocking mode
     */

    void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    /** 
     * Query buffering mode.
     */

    int getBuffering() {
        return buffering;
    }


    /** 
     * Set buffering mode
     *
     * @param buffering One of TclIO.BUFF_FULL, TclIO.BUFF_LINE,
     *     or TclIO.BUFF_NONE
     */

    void setBuffering(int buffering) {
        if (buffering < TclIO.BUFF_FULL || buffering > TclIO.BUFF_NONE)
            throw new TclRuntimeError(
                "invalid buffering mode in Channel.setBlocking()");

        this.buffering = buffering;
    }

    /** 
     * Query buffer size
     */

    int getBufferSize() {
        return bufferSize;
    }


    /** 
     * Tcl_SetChannelBufferSize -> setBufferSize
     *
     * @param size new buffer size
     */

    void setBufferSize(int size) {

        // If the buffer size is smaller than 10 bytes or larger than 1 Meg
        // do not accept the requested size and leave the current buffer size.

        if ((size < 10) || (size > (1024 * 1024))) {
            return;
        }

        bufferSize = size;
    }

    /** 
     * Query encoding
     *
     * @return Name of Channel's Java encoding (null if no encoding)
     */

    String getEncoding() {
        return encoding;
    }


    /** 
     * Set new Java encoding
     *
     */

    void setEncoding(String en) {
        if (en == null) {
            encoding = null;
            bytesPerChar = 1;
        } else {
            encoding = en;
            bytesPerChar = EncodingCmd.getBytesPerChar(en);
        }
    }

    /** 
     * Query input translation
     */

    int getInputTranslation() {
        return inputTranslation;
    }

    /** 
     * Set new input translation
     */

    void setInputTranslation(int translation) {
        inputTranslation = translation;
    }

    /** 
     * Query output translation
     */

    int getOutputTranslation() {
        return outputTranslation;
    }

    /** 
     * Set new output translation
     */

    void setOutputTranslation(int translation) {
        outputTranslation = translation;
    }

    /** 
     * Query input eof character
     */

    char getInputEofChar() {
        return inEofChar;
    }

    /** 
     * Set new input eof character
     */

    void setInputEofChar(char inEof) {
        inEofChar = inEof;
    }

    /** 
     * Query output eof character
     */

    char getOutputEofChar() {
        return outEofChar;
    }

    /** 
     * Set new output eof character
     */

    void setOutputEofChar(char outEof) {
        outEofChar = outEof;
    }

}
