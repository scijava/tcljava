/*
 * ReadCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ReadCmd.java,v 1.6 2001/11/20 19:12:15 mdejong Exp $
 *
 */

package tcl.lang;
import java.util.*;
import java.io.*;

/**
 * This class implements the built-in "read" command in Tcl.
 */

class ReadCmd implements Command {

    /**
     * This procedure is invoked to process the "read" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     */

    public void cmdProc(Interp interp, TclObject argv[])
            throws TclException {

	Channel chan;              // The channel being operated on this 
				   // method 
	int     i         = 1;     // Index to the next arg in argv
	int     numBytes  = 0;     // Num of bytes to read from channel
	boolean readAll   = true;  // If true read-all else numBytes
	boolean noNewline = false; // If true, strip the newline if there


	if ((argv.length != 2) && (argv.length != 3)) {
	    errorWrongNumArgs(interp, argv[0].toString());
	}

	if (argv[i].toString().equals("-nonewline")) {
	    noNewline = true;
	    i++;
	}

	if (i == argv.length) {
	    errorWrongNumArgs(interp, argv[0].toString());
	}

	chan = TclIO.getChannel(interp, argv[i].toString());
	if (chan == null) {
	    throw new TclException(interp, "can not find channel named \""
                    + argv[i].toString() + "\"");
	}

	// Consumed channel name. 

	i++;	

	// Compute how many bytes to read, and see whether the final
	// noNewline should be dropped.
	
	if (i < argv.length) {
	    String arg = argv[i].toString();
	  
	    if (Character.isDigit(arg.charAt(0))) {
	        numBytes = TclInteger.get(interp, argv[i]);
		readAll = false;
	    } else if (arg.equals("nonewline")) {
	        noNewline = true;
	    } else {
	        throw new TclException(interp, "bad argument \"" + arg +
		        "\": should be \"nonewline\"");
	    }
	}

	try { 
	    String inStr;
	    if (readAll) {
	        inStr = chan.read(interp, TclIO.READ_ALL, 0);

		// If -nonewline was specified, the inStr is not "" and
		// the last char is a "\n", then remove it and return.

		if ((noNewline) && (inStr.length() != 0) &&
		        (inStr.charAt(inStr.length() - 1)) == '\n') {
		    interp.setResult(inStr.substring(0, (inStr.length() - 2)));
		    return;
		}
	    } else {
	        inStr = chan.read(interp, TclIO.READ_N_BYTES, numBytes);
	    }

	    interp.setResult(inStr);

	} catch (IOException e) {
	    throw new TclRuntimeError(
		    "ReadCmd.cmdProc() Error: IOException when reading " +
		    chan.getChanName());
	}
    }

    /**
     * A unique error msg is printed for read, therefore dont call this 
     * instead of the standard TclNumArgsException().
     *
     * @param interp the current interpreter.
     * @param cmd the name of the command (extracted form argv[0] of cmdProc)
     */

    private void errorWrongNumArgs(Interp interp, String cmd) 
            throws TclException {
        throw new TclException(interp, "wrong # args: should be \"" +
                "read channelId ?numBytes?\" " +
		"or \"read ?-nonewline? channelId\"");
    }

}
