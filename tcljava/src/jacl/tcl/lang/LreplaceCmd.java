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
 * RCS: @(#) $Id: LreplaceCmd.java,v 1.4 2000/08/21 04:48:11 mo Exp $
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
        int size  = TclList.getLength(interp, argv[1]);
        int first = Util.getIntForIndex(interp, argv[2], size-1);
        int last  = Util.getIntForIndex(interp, argv[3], size-1);
        int numToDelete;

        if (first < 0) {
            first = 0;
        }

        // Complain if the user asked for a start element that is greater
        // than the list length. This won't ever trigger for the "end*"
        // case as that will be properly constrained by getIntForIndex
        // because we use size-1 (to allow for replacing the last elem).

        if ((first >= size) && (size > 0)) {
            throw new TclException(interp, "list doesn't contain element " +
                argv[2]);
        }
        if (last >= size) {
            last = size - 1;
        }
        if (first <= last) {
            numToDelete = (last - first + 1);
        } else {
            numToDelete = 0;
        }

	TclObject list = argv[1];
	list.preserve();
	list = list.takeExclusive();

	try {
	    TclList.replace(interp, list, first, numToDelete, argv, 4,
		    argv.length-1);
	    interp.setResult(list);
	} finally {
	    list.release();
	}
    }
}
