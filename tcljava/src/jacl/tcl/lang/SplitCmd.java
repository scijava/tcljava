/*
 * SplitCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: SplitCmd.java,v 1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "split" command in Tcl.
 */

class SplitCmd implements Command {
    /**
     * Default characters for splitting up strings.
     */

    private static char defSplitChars[] = {' ', '\n', '\t', '\r'};

    /**
     * This procedure is invoked to process the "split" Tcl
     * command. See Tcl user documentation for details.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     * @exception TclException If incorrect number of arguments.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	char splitChars[] = null;
	String string;

	if (argv.length == 2) {
	    splitChars = defSplitChars;
	} else if (argv.length == 3) {
	    splitChars = argv[2].toString().toCharArray();
	} else {
	    throw new TclNumArgsException(interp, 1, argv, "string ?splitChars?");
	}

	string = argv[1].toString();
	int len = string.length();
	int num = splitChars.length;

	/*
	 * Handle the special case of splitting on every character.
	 */

	if (num == 0) {
	    TclObject list = TclList.newInstance();

	    list.preserve();
	    try {
		for (int i=0; i<len; i++) {
		    TclList.append(interp, list, TclString.newInstance(
			    string.charAt(i)));
		}
		interp.setResult(list);
	    } finally {
		list.release();
	    }
	    return;
	}

	/*
	 * Normal case: split on any of a given set of characters.
	 * Discard instances of the split characters.
	 */
	TclObject list = TclList.newInstance();
	int elemStart = 0;
	
	list.preserve();
	try {
	    int i, j;
	    for (i=0; i<len; i++) {
		char c = string.charAt(i);
		for (j=0; j<num; j++) {
		    if (c == splitChars[j]) {
			TclList.append(interp, list, TclString.newInstance(
			    string.substring(elemStart, i)));
			elemStart = i+1;
			break;
		    }
		}
	    }
	    if (i != 0) {
		TclList.append(interp, list, TclString.newInstance(
		        string.substring(elemStart)));
	    }
	    interp.setResult(list);
	} finally {
	    list.release();
	}
    }
}
