/*
 * LappendCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LappendCmd.java,v 1.1 1998/10/14 21:09:20 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "lappend" command in Tcl.
 */
class LappendCmd implements Command {
    /**
     * This procedure is invoked to process the "lappend" Tcl command.
     * See the user documentation for details on what it does.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv.length < 2) {
	    throw new TclNumArgsException(interp, 1, argv, 
                    "varName ?value value ...?");
        } else if (argv.length == 2) {
	    try {
		interp.setResult(interp.getVar(argv[1], 0));
	    } catch (TclException e) {
		/*
		 * The variable doesn't exist yet. Just create it with an empty
		 * initial value.
		 */

		interp.setVar(argv[1], TclList.newInstance(), 0);
		interp.resetResult();
	    }
	} else {
	    TclObject result = null;
	    for (int i = 2; i < argv.length; i++) {
		result = interp.setVar(argv[1], argv[i],
		    TCL.APPEND_VALUE | TCL.LIST_ELEMENT);
	    }

	    interp.setResult(result);
	}
    }
}

