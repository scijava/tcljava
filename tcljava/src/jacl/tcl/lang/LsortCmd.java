/*
 * LsortCmd.java
 *
 *	The file implements the Tcl "lsort" command.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LsortCmd.java,v 1.3 2003/01/09 02:15:39 mdejong Exp $
 */

package tcl.lang;

/*
 * This LsortCmd class implements the Command interface for specifying a new
 * Tcl command.  The Lsort command implements the built-in Tcl command "lsort"
 * which is used to sort Tcl lists.  See user documentation for more details.
 */

class LsortCmd implements Command {

/*
 * List of switches that are legal in the lsort command.
 */

static final private String validOpts[] = {
    "-ascii",
    "-command",
    "-decreasing",
    "-dictionary",
    "-increasing",
    "-index",
    "-integer",
    "-real"
};

/*
 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked as part of the Command interface to 
 *	process the "lsort" Tcl command.  See the user documentation for
 *	details on what it does.
 *
 * Results:
 *	A standard Tcl result.
 *
 * Side effects:
 *	See the user documentation.
 *
 *----------------------------------------------------------------------
 */

public void cmdProc(
    Interp interp,	/* Current interpreter. */
    TclObject argv[])	/* Argument list. */
throws 
    TclException 	/* A standard Tcl exception. */
{
    if (argv.length < 2) {
	throw new TclNumArgsException(interp, 1, argv, "?options? list");
    }
       
    String command = null;
    int sortMode = QSort.ASCII;
    int sortIndex = -1;
    boolean sortIncreasing = true;

    for (int i = 1; i < argv.length - 1; i++) {
	int index = TclIndex.get(interp, argv[i], validOpts, "option", 0);

	switch (index) {
	case 0:		/* -ascii */
	    sortMode = QSort.ASCII;
	    break;

	case 1:		/* -command */
	    if (i == argv.length - 2) {
		throw new TclException(interp,
			"\"-command\" option must be" +
			" followed by comparison command");
	    }
	    sortMode = QSort.COMMAND;
	    command = argv[i + 1].toString();
	    i++;
	    break;

	case 2:		/* -decreasing */
	    sortIncreasing = false;
	    break;

	case 3:		/* -dictionary */
	    sortMode = QSort.DICTIONARY;
	    break;

	case 4:		/* -increasing */
	    sortIncreasing = true;
	    break;

	case 5:		/* -index */
	    if (i == argv.length - 2) {
		throw new TclException(interp, 
			"\"-index\" option must be followed by list index");
	    }
	    sortIndex = Util.getIntForIndex(interp, argv[i + 1], -2);
	    command = argv[i + 1].toString();
	    i++;
	    break;

	case 6:		/* -integer */
	    sortMode = QSort.INTEGER;
	    break;

	case 7:		/* -real */
	    sortMode = QSort.REAL;
	    break;
	}
    }

    TclObject list = argv[argv.length - 1];
    boolean isDuplicate = false;

    // If the list object is unshared we can modify it directly. Otherwise
    // we create a copy to modify: this is "copy on write".

    if (list.isShared()) {
	list = list.duplicate();
	isDuplicate = true;
    }

    try {
	TclList.sort(interp, list, sortMode, sortIndex,
		sortIncreasing, command);
	interp.setResult(list);
    } catch (TclException e) {
        if (isDuplicate) {
	    list.release();
	}
	throw e;
    }
}

} // LsortCmd
