/*
 * FlushCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: FlushCmd.java,v 1.1 1998/10/14 21:09:18 cvsadmin Exp $
 *
 */

package tcl.lang;
import java.util.*;
import java.io.*;

/**
 * This class implements the built-in "flush" command in Tcl.
 */

class FlushCmd implements Command {

    /**
     * This procedure is invoked to process the "flush" Tcl command.
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
	    chan.flush(interp);
	} catch (IOException e) {
	    throw new TclRuntimeError(
		    "FlushCmd.cmdProc() Error: IOException when flushing " +
		    chan.getChanName());
	}
    }
}
