/*
 * GetsCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: GetsCmd.java,v 1.4 2001/11/20 19:07:30 mdejong Exp $
 *
 */

package tcl.lang;
import java.util.*;
import java.io.*;

/**
 * This class implements the built-in "gets" command in Tcl.
 */

class GetsCmd implements Command {

    /**
     * This procedure is invoked to process the "gets" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     */

    public void cmdProc(Interp interp, TclObject argv[])
            throws TclException {

	boolean writeToVar = false;  // If true write to var passes as arg
	String  varName    = "";     // The variable to write value to
	Channel chan;                // The channel being operated on

	if ((argv.length < 2) || (argv.length > 3)) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "channelId ?varName?");
	}

	if (argv.length == 3) {
	    writeToVar = true;
	    varName = argv[2].toString();
	}
	
	chan = TclIO.getChannel(interp, argv[1].toString());
	if (chan == null) {
	    throw new TclException(interp, "can not find channel named \""
                    + argv[1].toString() + "\"");
	}

	try {
	    String inStr = chan.read(interp, TclIO.READ_LINE, 0);
 	    if (writeToVar) {
	        interp.setVar(varName, TclString.newInstance(inStr), 0);
		if (chan.eof()) {
		    interp.setResult(-1);
		} else {
		    interp.setResult(inStr.length());
		}
	    } else {
	        interp.setResult(TclString.newInstance(inStr));
	    }
	} catch (IOException e) {
	    throw new TclRuntimeError(
		    "GetsCmd.cmdProc() Error: IOException when getting " +
		    chan.getChanName());
	}

    }
}
