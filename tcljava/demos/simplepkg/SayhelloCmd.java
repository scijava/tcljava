/*
 * SayhelloCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 *   This file implements the SayhelloCmd class.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: SayhelloCmd.java,v 1.1 1998/10/14 21:09:23 cvsadmin Exp $
 */

import tcl.lang.*;

/*
 * This class implements the "sayhello" command in SimplePackage.
 */

class SayhelloCmd implements Command {
    /*
     * This procedure is invoked to process the "sayhello" Tcl command
     * -- it takes no arguments and returns "Hello World!" string as
     * its result.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv.length != 1) {
	    throw new TclNumArgsException(interp, 1, argv, "");
	}
	interp.setResult("Hello World!");
    }
}
