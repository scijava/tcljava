/*
 * LinsertCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LinsertCmd.java,v 1.1 1998/10/14 21:09:21 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "linsert" command in Tcl.
 */

class LinsertCmd implements Command {
    /**
     * See Tcl user documentation for details.
     * @exception TclException If incorrect number of arguments.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv.length < 4) {
	    throw new TclNumArgsException(interp, 1, argv, 
                     "list index element ?element ...?");
        }

	int size = TclList.getLength(interp, argv[1]);
	int index = TclInteger.getForIndex(interp, argv[2], size);
	TclObject list = argv[1];

	list.preserve();
	list = list.takeExclusive();
	try {
	    TclList.insert(interp, list, index, argv, 3, argv.length - 1);
	    interp.setResult(list);
	} finally {
	    list.release();
	}
    }
}

