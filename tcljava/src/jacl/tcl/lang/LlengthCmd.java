/*
 * LlengthCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LlengthCmd.java,v 1.1 1998/10/14 21:09:21 cvsadmin Exp $
 *
 */

package tcl.lang;



import java.io.*;
import java.util.*;

/**
 * This class implements the built-in "llength" command in Tcl.
 */

class LlengthCmd implements Command {
    /**
     * See Tcl user documentation for details.
     * @exception TclException If incorrect number of arguments.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv.length != 2) {
	    throw new TclNumArgsException(interp, 1, argv, "list");
        }
	interp.setResult(TclInteger.newInstance(TclList.getLength(interp,
		argv[1])));
    }
}

