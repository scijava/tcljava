/*
 * VariableCmd.java
 *
 * Copyright (c) 1999 Moses DeJong.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: VariableCmd.java,v 1.1 1999/05/30 01:16:54 dejong Exp $
 */

package tcl.lang;

/**
 * This class implements the built-in "variable" command in Tcl.
 */

class VariableCmd implements Command {

/*
 *----------------------------------------------------------------------
 *
 * VariableCmd --
 *
 *	Invoked to implement the "variable" command that creates one or more
 *	global variables. Handles the following syntax:
 *
 *	    variable ?name value...? name ?value?
 *
 *	One or more variables can be created. The variables are initialized
 *	with the specified values. The value for the last variable is
 *	optional.
 *
 *	If the variable does not exist, it is created and given the optional
 *	value. If it already exists, it is simply set to the optional
 *	value. Normally, "name" is an unqualified name, so it is created in
 *	the current namespace. If it includes namespace qualifiers, it can
 *	be created in another namespace.
 *
 *	If the variable command is executed inside a Tcl procedure, it
 *	creates a local variable linked to the newly-created namespace
 *	variable.
 *
 * Results:
 *	Returns TCL_OK if the variable is found or created. Returns
 *	TCL_ERROR if anything goes wrong.
 *
 * Side effects:
 *	If anything goes wrong, this procedure returns an error message
 *	as the result in the interpreter's result object.
 *
 *----------------------------------------------------------------------
 */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {

	// unimplemented.	 

	throw new TclException(interp, "variable command is not yet implemented");
    }
}
