/*
 * IncrCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997-1998 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: IncrCmd.java,v 1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "incr" command in Tcl.
 */
class IncrCmd implements Command {
    /**
     * This procedure is invoked to process the "incr" Tcl command.
     * See the user documentation for details on what it does.
     * @exception TclException if wrong # of args or increment is not an
     *     integer.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if ((argv.length != 2) && (argv.length != 3)) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "varName ?increment?");
        }

	TclObject tobj;
	int oldValue;
	int increment;

	tobj = interp.getVar(argv[1], 0);

	try {
	    oldValue = TclInteger.get(interp, tobj);
	} catch (TclException e) {
	    interp.addErrorInfo(
		    "\n    (reading value of variable to increment)");
	    throw e;
	}

	if (argv.length == 2) {
	    increment = 1;
	} else {
	    try {
		increment = TclInteger.get(interp, argv[2]);
	    } catch (TclException e) {
		interp.addErrorInfo("\n    (reading increment)");
		throw e;
	    }
	}

	TclObject newValObj = TclInteger.newInstance(oldValue + increment);
	interp.setVar(argv[1], newValObj, 0);
	interp.setResult(newValObj);
    }
}

