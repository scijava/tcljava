/*
 * FileChannel.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: FileChannel.java,v 1.7 2001/11/17 06:13:23 mdejong Exp $
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
	
	if (((modeFlags & TclIO.CREAT) != 0)  && !fileObj.exists() ) {
	    // Creates the file and closes it so it may be
	    // reopened with the correct permissions. (w, w+, a+)

	    file = new RandomAccessFile(fileObj, "rw");
	    file.close();
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

	if((modeFlags & TclIO.APPEND) != 0) {      
	    file.seek(file.length());
	}

	// In standard Tcl fashion, set the channelId to be "file" + the
	// value of the current FileDescriptor.

	String fName = "file" + getNextFileNum(interp);
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

    String read(Interp interp, int readType, int numBytes) 
            throws IOException, TclException {

	if (file == null) {
	    throw new TclRuntimeError("FileChannel.read: null file object");
	}
	if ((mode & TclIO.WRONLY) != 0) {
	    throw new TclException(interp, "channel \"" +
	        getChanName() + "\" wasn't opened for reading");
	}

	// Create the Buffered Reader if it does not already exist
	if (reader == null) {
	    reader = new BufferedReader( new FileReader(file.getFD()) );
	}

	eofCond = false;

	switch (readType) {
	    case TclIO.READ_ALL: {
		char[] charArr = new char[BUF_SIZE];
		StringBuffer sbuf = new StringBuffer((int) file.length());
		int numRead;
		    
		while((numRead = reader.read(charArr, 0, BUF_SIZE)) != -1) {
		    sbuf.append(charArr,0, numRead);
		}
		eofCond = true;
		return sbuf.toString();
	    }
	    case TclIO.READ_LINE: {
		String line = reader.readLine();
		if (line == null) {
		    eofCond = true;
		    return "";
		} else {
		    return line;
		}
	    }
	    case TclIO.READ_N_BYTES: {
		char[] charArr = new char[numBytes];
		int numRead;
		numRead = reader.read(charArr, 0, numBytes);
		if (numRead == -1) {
		    eofCond = true;
		    return "";
		}
		return( new String(charArr,0,numRead) );
	    }
	    default: {
	        throw new TclRuntimeError(
                        "FileChannel.read(): Incorrect read mode.");
	    }
	}
    }


    /**
     * Write data to a file.  The file MUST be open or a TclRuntimeError
     * is thrown.
     * 
     * @param interp currrent interpreter.
     * @param s string of data to write to the file
     * @exception TclException is thrown if read occurs on RDONLY channel.
     * @exception IOException is thrown when an IO error occurs that was not
     *                correctly tested for.  Most cases should be caught.
     */

    void write(Interp interp, String s) 
            throws IOException, TclException {

	if (file == null) {
	    throw new TclRuntimeError(
                    "FileChannel.write(): null file object");
	}
	if ((mode & TclIO.RDONLY) != 0) {
	    throw new TclException(interp, "channel \"" +
	        getChanName() + "\" wasn't opened for writing");
	}
        file.writeBytes(s);
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
        file.close();
    }


    /**
     * Flush the a file.  The file MUST be open or a TclRuntimeError
     * is thrown.
     * Note: Since we only have synchronous file IO right now, this 
     * is a no-op.  
     */

    void flush(Interp interp) throws IOException, TclException {
	if (file == null) {
	    throw new TclRuntimeError("FileChannel.flush(): null file object");
	}
    }


    /**
     * Move the file pointer internal to the RandomAccessFile object. 
     * The file MUST be open or a TclRuntimeError is thrown.
     * 
     * @param offset The number of bytes to move the file pointer.
     * @param where to begin incrementing the file pointer; beginning,
     * current, or end of the file.
     */

    void seek(long offset, int mode)  throws IOException {
      
	if (file == null) {
	    throw new TclRuntimeError("FileChannel.seek(): null file object");
	}

        switch (mode) {
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
     * Checks the read/write permissions on the File object.  If mode less
     * than 0 it checks for read permissions, if mode greater than 0 it checks
     *  for write permissions, and if it equals 0 then it checks both.
     *
     * @param interp currrent interpreter.
     * @param fileObj a java.io.File object of the file for this channel.
     * @param mode what permissions to check for.
     */

    private void checkReadWritePerm(Interp interp, File fileObj, int mode) 
        throws TclException {
	boolean error = false;

	if (mode <= 0) {
	    if (!fileObj.canRead()) {
	        error = true;
	    } 
	}
	if (mode >= 0) {
	    if (!fileObj.canWrite()) {
	        error = true;
	    }
	}
	if (error) {
	    throw new TclPosixException(interp, TclPosixException.EACCES,
		    true, "couldn't open \"" + fileObj.getName() + "\"");
	}
    }

  
    /**
     * Really ugly function that attempts to get the next integer for
     * the channelId name.  In C the FD returned in the native open call
     * returns this value, but we dont have that...   
     *
     * For now we just iterate through the hashtable keys and finds the 
     * first key matching file<X> that is not already taken.  
     *
     * @param interp currrent interpreter.
     * @return the next integer to use in the channelId name.
     */

    private int getNextFileNum(Interp interp) {
        int i;
	Hashtable htbl = TclIO.getInterpChanTable(interp);

        for (i = 0; (htbl.get("file" + i)) != null; i++) {
	    // Do nothing...
	}
	return i;
    }
}
