/*
 * UnsetCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: UnsetCmd.java,v 1.2 1999/07/28 03:28:52 mo Exp $
 *
 */

package tcl.lang;



import java.util.*;

/**
 * This class implements the built-in "unset" command in Tcl.
 */

class UnsetCmd implements Command {
    /**
     * Tcl_UnsetObjCmd -> UnsetCmd.cmdProc
     *
     * Unsets Tcl variable (s). See Tcl user documentation * for
     * details.
     * @exception TclException If tries to unset a variable that does
     * not exist.
     */

    public void cmdProc(Interp interp, TclObject[] objv)
	    throws TclException {
	if (objv.length < 2) {
	    throw new TclNumArgsException(interp, 1, objv, 
		    "varName ?varName ...?");
	}

	for (int i = 1; i < objv.length; i++) {
	    interp.unsetVar(objv[i], 0);
	}

	return;
    }
}

