/*
 * Channel.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Channel.java,v 1.3 2000/08/01 06:50:48 mo Exp $
 */

package tcl.lang;
import java.io.*;

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

    protected String chanName;

    /**
     * How many interpreters hold references to this IO channel?
     */

    protected int refCount = 0;

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

    abstract void write(Interp interp, String outStr) 
            throws IOException, TclException;


    /** 
     * Interface to close the Channel.  The channel is only closed, it is 
     * the responsibility of the "closer" to remove the channel from 
     * the channel table.
     */

    abstract void close() throws IOException;


    /** 
     * Interface to flush the Channel.
     *
     * @exception TclException is thrown when attempting to flush a 
     *            read only channel.
     * @exception IOEcception is thrown for all other flush errors.
     */

    abstract void flush(Interp interp) 
            throws IOException, TclException ;


    /** 
     * Interface move the current Channel pointer.
     * Used in file channels to move the file pointer.
     * 
     * @param offset The number of bytes to move the file pointer.
     * @param mode where to begin incrementing the file pointer; beginning,
     *             current, end.
     */

    abstract void seek(long offset, int mode) throws IOException;


    /** 
     * Interface to tell the value for the Channel pointer.
     * Used in file channels to return the current file pointer.
     */

    abstract long tell()  throws IOException;


    /**
     * Interface that returns true if the last read reached the EOF.
     */

    abstract boolean eof();


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
}
