/*
 * FileChannel.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: FileChannel.java,v 1.19 2003/03/06 22:53:06 mdejong Exp $
 *
 */

package tcl.lang;
import java.util.*;
import java.io.*;

/**
 * Subclass of the abstract class Channel.  It implements all of the 
 * methods to perform read, write, open, close, etc on a file.
 */

class FileChannel extends Channel {

    /**
     * The file needs to have a file pointer that can be moved randomly
     * within the file.  The RandomAccessFile is the only java.io class
     * that allows this behavior.
     */

    private RandomAccessFile file = null;

    /**
     * Open a file with the read/write permissions determined by modeFlags.
     * This method must be called before any other methods will function
     * properly.
     *
     * @param interp currrent interpreter.
     * @param fileName the absolute path or name of file in the current 
     *                 directory to open
     * @param modeFlags modes used to open a file for reading, writing, etc
     * @return the channelId of the file.
     * @exception TclException is thrown when the modeFlags try to open
     *                a file it does not have permission for or if the
     *                file dosent exist and CREAT wasnt specified.
     * @exception IOException is thrown when an IO error occurs that was not
     *                correctly tested for.  Most cases should be caught.
     */

    String open(Interp interp, String fileName, int modeFlags) 
            throws IOException, TclException {

	mode = modeFlags;
	File fileObj = FileUtil.getNewFileObj(interp, fileName);

	// Raise error if file exists and both CREAT and EXCL are set

	if (((modeFlags & TclIO.CREAT) != 0) &&
	        ((modeFlags & TclIO.EXCL) != 0) &&
	        fileObj.exists()) {
	    throw new TclException (interp, "couldn't open \"" +
	            fileName + "\": file exists");
	}

	if (((modeFlags & TclIO.CREAT) != 0) && !fileObj.exists() ) {
	    // Creates the file and closes it so it may be
	    // reopened with the correct permissions. (w, w+, a+)

	    file = new RandomAccessFile(fileObj, "rw");
	    file.close();
	}

	// Truncate file to zero length if it exists.

	if (((modeFlags & TclIO.TRUNC) != 0) && fileObj.exists()) {
	    file = new RandomAccessFile(fileObj, "rw");
	    file.close ();
	}

	if ((modeFlags & TclIO.RDWR) != 0) { 
	    // Opens file (r+), error if file does not exist.

	    checkFileExists(interp, fileObj);
	    checkReadWritePerm(interp, fileObj, 0);

	    if (fileObj.isDirectory()) {
		throw new TclException(interp, "couldn't open \"" +
			fileName + "\": illegal operation on a directory");
	    }

	    file = new RandomAccessFile(fileObj, "rw");

	} else if ((modeFlags & TclIO.RDONLY) != 0) { 
	    // Opens file (r), error if file does not exist.

	    checkFileExists(interp, fileObj);
	    checkReadWritePerm(interp, fileObj, -1);

	    if (fileObj.isDirectory()) {
		throw new TclException(interp, "couldn't open \"" +
			fileName + "\": illegal operation on a directory");
	    }

	    file = new RandomAccessFile(fileObj, "r");

	} else if ((modeFlags & TclIO.WRONLY) != 0) {
	    // Opens file (a), error if dosent exist.

	    checkFileExists(interp, fileObj);
	    checkReadWritePerm(interp, fileObj, 1);

	    if (fileObj.isDirectory()) {
		throw new TclException(interp, "couldn't open \"" +
			fileName + "\": illegal operation on a directory");
	    }
	    
	    // Currently there is a limitation in the Java API.
	    // A file can only be opened for read OR read-write.
	    // Therefore if the file is write only, Java cannot
	    // open the file.  Throw an error indicating this
	    // limitation.

	    if (!fileObj.canRead()) {
	        throw new TclException(interp, 
		        "Java IO limitation: Cannot open a file "+
                        "that has only write permissions set.");
	    }
	    file = new RandomAccessFile(fileObj, "rw");

	} else {
	    throw new TclRuntimeError("FileChannel.java: invalid mode value");
	}

	// If we are appending, move the file pointer to EOF.

	if ((modeFlags & TclIO.APPEND) != 0) {      
	    file.seek(file.length());
	}

	// In standard Tcl fashion, set the channelId to be "file" + the
	// value of the current FileDescriptor.

	String fName = TclIO.getNextDescriptor(interp, "file");
	setChanName(fName);
	return fName;
    }


    /**
     * Read data from a file.  The read can be for the entire buffer, line 
     * or a specified number of bytes.  The file MUST be open or a 
     * TclRuntimeError is thrown.
     *
     * @param interp currrent interpreter.
     * @param readType specifies if the read should read the entire buffer, 
     *            the next line, or a specified number of bytes.
     * @param numBytes number of bytes to read.  Only used when the readType
     *            is TclIO.READ_N_BYTES.
     * @return String of data that was read from file. (can not be null)
     * @exception TclException is thrown if read occurs on WRONLY channel.
     * @exception IOException is thrown when an IO error occurs that was not
     *                correctly tested for.  Most cases should be caught.
     */

    TclObject read(Interp interp, int readType, int numBytes) 
            throws IOException, TclException {

	if (file == null) {
	    throw new TclRuntimeError("FileChannel.read: null file object");
	}

	// Create the Buffered Reader if it does not already exist
	if (reader == null) {
	    reader = new BufferedReader( new FileReader(file.getFD()) );
	}

        return super.read(interp, readType, numBytes);
    }


    /**
     * Write data to a file.  The file MUST be open or a TclRuntimeError
     * is thrown.
     * 
     * @param interp currrent interpreter.
     * @param outData the TclObject that holds the data to write.
     * @exception TclException is thrown if read occurs on RDONLY channel.
     * @exception IOException is thrown when an IO error occurs that was not
     *                correctly tested for.  Most cases should be caught.
     */

    void write(Interp interp, TclObject outData) 
            throws IOException, TclException {

        if (file == null) {
            throw new TclRuntimeError(
                "FileChannel.write(): null file object");
        }

        checkWrite(interp);
        file.writeBytes(outData.toString());
    }


    /**
     * Close the file.  The file MUST be open or a TclRuntimeError
     * is thrown.
     */

    void close() throws IOException {
        if (file == null) {
            throw new TclRuntimeError(
                "FileChannel.close(): null file object");
        }

        // Invoke super.close() first since it might write an eof char
        try {
            super.close();
        } finally {
            file.close();
        }
    }


    /**
     * Flush the Channel
     *
     *  The file MUST be open or a TclRuntimeError. Note that since
     *  we only have synchronous file IO right now, this is a no-op.
     */

    void flush(Interp interp) throws IOException, TclException {
        if (file == null) {
            throw new TclRuntimeError("FileChannel.flush(): null file object");
        }

        checkWrite(interp);
    }


    /**
     * Move the file pointer internal to the RandomAccessFile object. 
     * The file MUST be open or a TclRuntimeError is thrown.
     * 
     * @param offset The number of bytes to move the file pointer.
     * @param inmode to begin incrementing the file pointer; beginning,
     * current, or end of the file.
     */

    void seek(Interp interp, long offset, int inmode)
            throws IOException, TclException {

	if (file == null) {
	    throw new TclRuntimeError("FileChannel.seek(): null file object");
	}

        switch (inmode) {
	    case TclIO.SEEK_SET: {
	        file.seek(offset);
		break;
	    }
	    case TclIO.SEEK_CUR: {
	        file.seek(file.getFilePointer() + offset);
		break;
	    }
	    case TclIO.SEEK_END: {
	        file.seek(file.length() + offset);
		break;
	    }
	}
    }


    /**
     * Return the current offset of the file pointer in number of bytes from 
     * the beginning of the file.  The file MUST be open or a TclRuntimeError
     * is thrown.
     *
     * @return The current value of the file pointer.
     */

    long tell()  throws IOException {
	if (file == null) {
	    throw new TclRuntimeError("FileChannel.tell(): null file object");
	}
        return(file.getFilePointer());
    }


    /**
     * If the file dosent exist then a TclExcpetion is thrown. 
     *
     * @param interp currrent interpreter.
     * @param fileObj a java.io.File object of the file for this channel.
     */


    private void checkFileExists(Interp interp, File fileObj) 
        throws TclException {
	  if (!fileObj.exists()) {
	      throw new TclPosixException(interp, TclPosixException.ENOENT,
		      true, "couldn't open \"" + fileObj.getName() + "\"");
	  }
    }


    /**
     * Checks the read/write permissions on the File object.  If inmode is less
     * than 0 it checks for read permissions, if mode greater than 0 it checks
     * for write permissions, and if it equals 0 then it checks both.
     *
     * @param interp currrent interpreter.
     * @param fileObj a java.io.File object of the file for this channel.
     * @param inmode what permissions to check for.
     */

    private void checkReadWritePerm(Interp interp, File fileObj, int inmode) 
        throws TclException {
	boolean error = false;

	if (inmode <= 0) {
	    if (!fileObj.canRead()) {
	        error = true;
	    } 
	}
	if (inmode >= 0) {
	    if (!fileObj.canWrite()) {
	        error = true;
	    }
	}
	if (error) {
	    throw new TclPosixException(interp, TclPosixException.EACCES,
		    true, "couldn't open \"" + fileObj.getName() + "\"");
	}
    }

    String getChanType() {
        return "file";
    }
}
