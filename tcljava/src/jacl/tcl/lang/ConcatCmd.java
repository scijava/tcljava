/*
 * ConcatCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ConcatCmd.java,v 1.1 1998/10/14 21:09:18 cvsadmin Exp $
 *
 */

package tcl.lang;



import java.io.*;
import java.util.*;

/**
 * This class implements the built-in "concat" command in Tcl.
 */
class ConcatCmd implements Command {

    /**
     * See Tcl user documentation for details.
     */
    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
		interp.setResult(Util.concat(1, argv.length, argv));
    }
}

