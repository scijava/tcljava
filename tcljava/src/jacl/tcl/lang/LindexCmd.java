/*
 * LindexCmd.java - -
 *
 *	Implements the built-in "lindex" Tcl command.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LindexCmd.java,v 1.2 2000/03/17 23:31:30 mo Exp $
 *
 */

package tcl.lang;

/*
 * This class implements the built-in "lindex" command in Tcl.
 */

class LindexCmd implements Command {


/*
 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked as part of the Command interface to
 *	process the "lindex" Tcl command.  See the user documentation
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
    if (argv.length != 3) {
	throw new TclNumArgsException(interp, 1, argv, "list index");
    }

    int size = TclList.getLength(interp, argv[1]);
    int index = Util.getIntForIndex(interp, argv[2], size-1);
    TclObject element = TclList.index(interp, argv[1], index);

    if (element != null) {
	interp.setResult(element);
    } else {
	interp.resetResult();
    }
}

} // end 

