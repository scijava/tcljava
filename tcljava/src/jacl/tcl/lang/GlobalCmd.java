/*
 * GlobalCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: GlobalCmd.java,v 1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "global" command in Tcl.
 */

class GlobalCmd implements Command {
    /**
     * See Tcl user documentation for details.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv.length < 2) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "varName ?varName ...?");
	}
	interp.resetResult();

	if (interp.varFrame == interp.globalFrame) {
	    /*
	     * We are already in the global scope. This command has
	     * no effect.
	     */
	    return;
	}
	for (int i =1; i<argv.length; i++) {
	    interp.varFrame.makeUpVar(interp.globalFrame, argv[i].toString(),
		    null, argv[i].toString(), 0);
	}
    }
}

