/*
 * AppendCmd.java --
 *
 *	Implements the built-in "append" Tcl command.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: AppendCmd.java,v 1.1 1998/10/14 21:09:20 cvsadmin Exp $
 *
 */

package tcl.lang;

/*
 * This class implements the built-in "append" command in Tcl.
 */

class AppendCmd implements Command {

/*
 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked as part of the Command interface to
 *	process the "append" Tcl command.  See the user documentation
 *	for details on what it does.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	See the user documentation.
 *
 *----------------------------------------------------------------------
 */

public void
cmdProc(
    Interp interp,		// Current interpreter.
    TclObject argv[])		// Argument list.
throws 
    TclException 		// A standard Tcl exception.
{
    if (argv.length < 2) {
	throw new TclNumArgsException(interp, 1, argv, 
		"varName ?value value ...?");
    } else if (argv.length == 2) {
	interp.setResult(interp.getVar(argv[1], 0));
    } else {
	TclObject result = null;

	for (int i = 2; i < argv.length; i++) {
	    result = interp.setVar(argv[1], argv[i], TCL.APPEND_VALUE);
	}

	if (result != null) {
	    interp.setResult(result);
	} else {
	    interp.resetResult();
	}
    }
}

} // end AppendCmd

