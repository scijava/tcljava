/*
 * ProcCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ProcCmd.java,v 1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "proc" command in Tcl.
 */

class ProcCmd implements Command {
    /**
     * Creates a new Tcl procedure.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     * @exception TclException If incorrect number of arguments.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv.length != 4) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "name args body");
	}
	Procedure p = new Procedure(interp, argv[1].toString(),
		argv[2], argv[3], interp.getScriptFile(),
		interp.getArgLineNumber(3));

	interp.createCommand(argv[1].toString(), p);
    }
}
