/*
 * Channel.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Channel.java,v 1.9 2001/11/20 00:53:07 mdejong Exp $
 */

package tcl.lang;
import java.io.*;
import java.util.Hashtable;

/**
 * The Channel interface specifies the methods that
 * for any Class that performs generic reads, writes, 
 * etc.  Note: open is not an interface, because it
 * takes unique arguments for each new channel type.
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
     * Buffer size used when reading blocks of input.
     */

    protected static final int BUF_SIZE = 1024;

    /**
     * Perform a read on the sub-classed channel.  
     * 
     * @param interp is used for TclExceptions.  
     * @param readType is used to specify the type of read (line, all, etc).
     * @param numBytes the number of byte to read (if applicable).
     * @return String of data that was read from the Channel (can not be null)
     * @exception TclException is thrown if read occurs on WRONLY channel.
     * @exception IOException is thrown when an IO error occurs that was not
     *                correctly tested for.  Most cases should be caught.
     */

    abstract String read(Interp interp, int readType, int numBytes) 
            throws IOException, TclException;


    /** 
     * Interface to write data to the Channel
     * 
     * @param interp is used for TclExceptions.  
     * @param outStr the string to write to the sub-classed channel.
     */

    void write(Interp interp, String outStr)
	    throws IOException, TclException {

        if ((mode & (TclIO.WRONLY|TclIO.RDWR)) == 0)
            throw new TclException(interp, "channel \"" + getChanName() +
                "\" wasn't opened for writing.");

        if (writer != null) {
            eofCond = false;

            try
            {
                writer.write(outStr);
            }
            catch (EOFException e)
            {
                eofCond = true;
                throw e;
            }
        }
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
            reader.close();
            reader = null;
        }
        if (writer != null) {
           try { writer.close(); } catch (IOException e) { ex = e; }
            writer.close();
            writer = null;
        }

        if (ex != null)
            throw ex;
    }

    /** 
     * Interface to flush the Channel.
     *
     * @exception TclException is thrown when attempting to flush a 
     *            read only channel.
     * @exception IOEcception is thrown for all other flush errors.
     */

    void flush(Interp interp) 
            throws IOException, TclException {

        if ((mode & (TclIO.WRONLY|TclIO.RDWR)) == 0)
            throw new TclException(interp, "channel \"" + getChanName() +
                    "\" wasn't opened for writing");

        if (writer != null) {
            try {
                writer.flush();
            } catch (EOFException e) {
                // FIXME: We need to clear eofCond on next write if set!
                eofCond = true;
                throw e;
            }
        }
    }

    /** 
     * Interface move the current Channel pointer.
     * Used in file channels to move the file pointer.
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


    /** 
     * Gets the mode that is the read-write etc settings for this channel.
     * @return mode
     */

    int getMode() {
        return mode;
    }

    /**
     * Really ugly function that attempts to get the next available
     * channelId name.  In C the FD returned in the native open call
     * returns this value, but we don't have that so we need to do
     * this funky iteration over the Hashtable.
     *
     * @param interp currrent interpreter.
     * @return the next integer to use in the channelId name.
     */

    protected String getNextDescriptor(Interp interp, String prefix) {
        int i;
	Hashtable htbl = TclIO.getInterpChanTable(interp);

        for (i = 0; (htbl.get(prefix + i)) != null; i++) {
	    // Do nothing...
	}
	return prefix + i;
    }
}
