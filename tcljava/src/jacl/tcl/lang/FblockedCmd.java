/*
 * FblockedCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: FblockedCmd.java,v 1.3 2001/11/20 19:07:30 mdejong Exp $
 *
 */

package tcl.lang;
import java.util.*;

/**
 * This class implements the built-in "fblocked" command in Tcl.
 */

class FblockedCmd implements Command {
    /**
     * This procedure is invoked to process the "fblocked" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     */

    public void cmdProc(Interp interp, TclObject argv[])
            throws TclException {

	Channel chan;        // The channel being operated on this method

	if (argv.length != 2) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "channelId");
	}

        chan = TclIO.getChannel(interp, argv[1].toString());
	if (chan == null) {
	    throw new TclException(interp, "can not find channel named \""
                    + argv[1].toString() + "\"");
	}

	// Java only has synchronous io so always return 0

	interp.setResult(0);
    }
}
