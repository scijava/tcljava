/*
 * NamespaceCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: NamespaceCmd.java,v 1.2 1999/05/09 00:44:38 dejong Exp $
 */

package tcl.lang;

/**
 * This class implements the built-in "namespace" command in Tcl.
 */

class NamespaceCmd implements Command {
    /**
     * This procedure is invoked to process the "namespace" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     * @exception TclException if error occurs.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {

	// unimplemented.	 

	throw new TclException(interp, "Namespace command is not yet implemented");
    }
}
