/*
 * JavaInfoCmd.java --
 *
 *	Implements the built-in "java::null" command.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: JavaNullCmd.java,v 1.1 1998/10/14 21:09:14 cvsadmin Exp $
 *
 */

package tcl.lang;

/*
 * Implements the built-in "java::null" command.
 */

class JavaNullCmd implements Command {

/*
 * The internal representation of java::null.
 */

private static final String nullRep = "java0x0";

/*----------------------------------------------------------------------
 *
 * cmdProc --
 *
 * 	This procedure is invoked to process the "java::null" Tcl
 * 	command. See the user documentation for details on what it
 * 	does.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	A standard Tcl result is stored in the interpreter.
 *
 *----------------------------------------------------------------------
 */

public void
cmdProc(
    Interp interp,			// Current interpreter.
    TclObject argv[])			// Argument list.
{
    interp.setResult(nullRep);
}

/*
 *----------------------------------------------------------------------
 *
 *  getNullObj --
 *
 *	Returns a string containing the Tcl representation of null.
 *
 * Results:
 *	Returns a string containing the Tcl representation of null.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static TclObject
getNullObj()
{
    return TclString.newInstance(nullRep);
}


/*
 *----------------------------------------------------------------------
 *
 *  getNullString --
 *
 *	Returns a Java string containing the Tcl representation of null.
 *
 * Results:
 *	Returns a Java string containing the Tcl representation of null.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static String
getNullString()
{
    return nullRep;
}

} // end CallCmd

