/*
 * ContinueCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ContinueCmd.java,v 1.1 1998/10/14 21:09:20 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "continue" command in Tcl.
 */

class ContinueCmd implements Command {
    /**
     * This procedure is invoked to process the "continue" Tcl command.
     * See the user documentation for details on what it does.
     * @exception TclException is always thrown.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv.length != 1) {
	    throw new TclNumArgsException(interp, 1, argv, null);
        }
	throw new TclException(interp, null, TCL.CONTINUE);
    }
}

