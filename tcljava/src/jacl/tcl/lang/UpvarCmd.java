/*
 * UpvarCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: UpvarCmd.java,v 1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "upvar" command in Tcl.
 */

class UpvarCmd implements Command {
    /**
     * This procedure is invoked to process the "upvar" Tcl command.
     * See the user documentation for details on what it does.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv.length < 3) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "?level? otherVar localVar ?otherVar localVar ...?");
        }

	CallFrame frame = interp.varFrame.getFrame(argv[1].toString());
	int start;
	if (frame != null) {
	    /*
	     * The level is specified.
	     */

	    if (argv.length %2 != 0) {
		throw new TclNumArgsException(interp, 1, argv, 
			"?level? otherVar localVar ?otherVar localVar ...?");
	    }
	    start = 2;
	} else {
	    /*
	     * The level is not specified. We are not in the global frame
	     * (otherwise getFrame() would have thrown an exception).
	     */

	    if (argv.length %2 == 0) {
		throw new TclNumArgsException(interp, 1, argv, 
			"?level? otherVar localVar ?otherVar localVar ...?");
	    }
	    frame = interp.varFrame.callerVar;
	    start = 1;
	}

	for (int i=start; i<argv.length; i+=2) {
	    interp.varFrame.makeUpVar(frame, argv[i],
		    argv[i+1].toString(), 0);
	}

	interp.resetResult();
    }
}

