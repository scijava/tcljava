/*
 * ExitCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ExitCmd.java,v 1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "exit" command in Tcl.
 */
class ExitCmd implements Command {

    /**
     * See Tcl user documentation for details.
     */
    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	int code;

	if (argv.length > 2) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "?returnCode?");
	}
	if (argv.length == 2) {
	    code = TclInteger.get(interp, argv[1]);
	} else {
	    code = 0;
	}

	System.exit(code);
    }
}

