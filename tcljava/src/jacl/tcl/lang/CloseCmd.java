/*
 * CloseCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: CloseCmd.java,v 1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang;
import java.util.*;
import java.io.*;

/**
 * This class implements the built-in "close" command in Tcl.
 */

class CloseCmd implements Command {
    /**
     * This procedure is invoked to process the "close" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     */

    public void cmdProc(Interp interp, TclObject argv[])
            throws TclException {
	      
        Channel chan;        /* The channel being operated on this method */

	if (argv.length != 2) {
	    throw new TclNumArgsException(interp, 1, argv, "channelId");
	}
    
	chan = TclIO.getChannel(interp, argv[1].toString());
	if (chan == null) {
	    throw new TclException(interp, "can not find channel named \""
                    + argv[1].toString() + "\"");
	}

	try {
	    chan.close();
	    TclIO.unregisterChannel(interp, chan);
	} catch (IOException e) {
	    throw new TclRuntimeError(
		    "CloseCmd.cmdProc() Error: IOException when closing " +
		    chan.getChanName());
	}
    }
}
