/*
 * EvalCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: EvalCmd.java,v 1.1 1998/10/14 21:09:18 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "eval" command in Tcl.
 */

class EvalCmd implements Command {
    /**
     * This procedure is invoked to process the "eval" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     * @exception TclException if script causes error.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv.length < 2) {
	    throw new TclNumArgsException(interp, 1, argv, "arg ?arg ...?");
	}

	try {
	    if (argv.length == 2) {
		interp.eval(argv[1], 0);
	    } else {
		String s = Util.concat(1, argv.length-1, argv);
		interp.eval(s, 0);
	    }
	} catch (TclException e) {
	    if (e.getCompletionCode() == TCL.ERROR) {
		interp.addErrorInfo("\n    (\"eval\" body line " +
				interp.errorLine + ")");
	    }
	    throw e;
	}
    }
}

