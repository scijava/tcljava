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
 * RCS: @(#) $Id: UnsetCmd.java,v 1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang;



import java.util.*;

/**
 * This class implements the built-in "unset" command in Tcl.
 */

class UnsetCmd implements Command {
    /**
     * Unsets Tcl variable (s). See Tcl user documentation * for
     * details.
     * @exception TclException If tries to unset a variable that does
     * not exist.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv.length < 2) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "varName ?varName ...?");
	}

	for (int i = 1; i < argv.length; i++) {
	    interp.unsetVar(argv[i], 0);
	}

	return;
    }
}

