/*
 * LreplaceCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LreplaceCmd.java,v 1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "lreplace" command in Tcl.
 */

class LreplaceCmd implements Command {
    /**
     * See Tcl user documentation for details.
     * @exception TclException If incorrect number of arguments.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv.length < 4) {
	    throw new TclNumArgsException(interp, 1, argv, 
                    "list first last ?element element ...?");
        }
	int size = TclList.getLength(interp, argv[1]);
	int first;
	int last;
	int count;

	first = TclInteger.getForIndex(interp, argv[2], size-1);
	last  = TclInteger.getForIndex(interp, argv[3], size-1);

	if (first < 0) {
	    first = 0;
	}
	if (first >= size) {
	    throw new TclException(interp, "list doesn't contain element " +
		    argv[2]);
	}
	if (last >= size) {
	    last = size - 1;
	}

	TclObject list = argv[1];
	list.preserve();
	list = list.takeExclusive();

	try {
	    TclList.replace(interp, list, first, last-first+1, argv, 4,
		    argv.length-1);
	    interp.setResult(list);
	} finally {
	    list.release();
	}
    }
}
