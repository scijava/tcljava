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
 * RCS: @(#) $Id: UpvarCmd.java,v 1.2 1999/06/30 00:13:39 mo Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "upvar" command in Tcl.
 */

class UpvarCmd implements Command {
    /**
     * Tcl_UpvarObjCmd -> UpvarCmd.cmdProc
     *
     * This procedure is invoked to process the "upvar" Tcl command.
     * See the user documentation for details on what it does.
     */

    public void cmdProc(Interp interp, TclObject[] objv)
	    throws TclException {
	if (objv.length < 3) {
	    throw new TclNumArgsException(interp, 1, objv, 
		    "?level? otherVar localVar ?otherVar localVar ...?");
        }

	// FIXME : need to port over 8.1 impl of upvar

	CallFrame frame = interp.varFrame.getFrame(objv[1].toString());
	int start;
	if (frame != null) {
	    // The level is specified.

	    if (objv.length %2 != 0) {
		throw new TclNumArgsException(interp, 1, objv, 
			"?level? otherVar localVar ?otherVar localVar ...?");
	    }
	    start = 2;
	} else {
	    // The level is not specified. We are not in the global frame
	    // (otherwise getFrame() would have thrown an exception).

	    if (objv.length %2 == 0) {
		throw new TclNumArgsException(interp, 1, objv, 
			"?level? otherVar localVar ?otherVar localVar ...?");
	    }
	    frame = interp.varFrame.callerVar;
	    start = 1;
	}

	for (int i=start; i < objv.length; i+=2) {
	    Var.makeUpvar(interp, frame, objv[i].toString(), null, 0,
		    objv[i+1].toString(), 0);
	}

	interp.resetResult();
    }
}

