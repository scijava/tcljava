/*
 * SetCmd.java --
 *
 *	Implements the built-in "set" Tcl command.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: SetCmd.java,v 1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang;

/*
 * This class implements the built-in "set" command in Tcl.
 */

class SetCmd implements Command {

/*
 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked as part of the Command interface to
 *	process the "set" Tcl command.  See the user documentation
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
    if (argv.length == 2) {
	interp.setResult(interp.getVar(argv[1], 0));
    } else if (argv.length == 3) {
	interp.setResult(interp.setVar(argv[1], argv[2], 0));
    } else {
	throw new TclNumArgsException(interp, 1, argv, 
		"varName ?newValue?");
    }
}

} // end SetCmd

