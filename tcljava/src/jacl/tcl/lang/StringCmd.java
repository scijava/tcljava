/*
 * StringCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: StringCmd.java,v 1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang;

import java.util.*;

/**
 * This class implements the built-in "string" command in Tcl.
 */
class StringCmd implements Command {
    static final private String validCmds[] = {
	"compare",
	"first",
	"index",
	"last",
	"length",
	"match",
	"range",
	"tolower",
	"toupper",
	"trim",
	"trimleft",
	"trimright",
	"wordend",
	"wordstart"
    };

    static final private int OPT_COMMAND 	= 0;
    static final private int OPT_FIRST   	= 1;
    static final private int OPT_INDEX 		= 2;
    static final private int OPT_LAST 		= 3;
    static final private int OPT_LENGTH 	= 4;
    static final private int OPT_MATCH 		= 5;
    static final private int OPT_RANGE 		= 6;
    static final private int OPT_TOLOWER 	= 7;
    static final private int OPT_TOUPPER	= 8;
    static final private int OPT_TRIM 		= 9;
    static final private int OPT_TRIMLEFT 	= 10;
    static final private int OPT_TRIMRIGHT	= 11;
    static final private int OPT_WORDEND	= 12;
    static final private int OPT_WORDSTART	= 13;

    /**
     * See Tcl user documentation for details.
     */
    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv.length < 2) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "option arg ?arg ...?");
	}
	int opt = TclIndex.get(interp, argv[1], validCmds, "option", 0);

	switch (opt) {
	    case OPT_COMMAND: {
		if (argv.length != 4) {
		    throw new TclNumArgsException(interp, 1, argv, 
			"compare string1 string2");
		}
		if (argv[2] == argv[3]) {
		    /*
		     * Short cut -- they are the same object. So must be equal.
		     */
		    interp.setResult(0);
		    return;
		}

		int i = argv[2].toString().compareTo(argv[3].toString());
		if (i > 0) {
		    interp.setResult(1);
		} else if (i < 0) {
		    interp.setResult(-1);
		} else {
		    interp.setResult(0);
		}
		break;
	    }
	    case OPT_LAST: {
		if (argv.length != 4) {
		    throw new TclNumArgsException(interp, 1, argv, 
			"last string1 string2");
		}
		String str1 = argv[2].toString();
		String str2 = argv[3].toString();
		interp.setResult(str2.lastIndexOf(str1));
		break;
	    }
	    case OPT_FIRST: {
		if (argv.length != 4) {
		    throw new TclNumArgsException(interp, 1, argv, 
		            "first string1 string2");
		}
		String str1 = argv[2].toString();
		if (str1.length() == 0) {
		    interp.setResult(-1);
		} else {
		    String str2 = argv[3].toString();
		    interp.setResult(str2.indexOf(str1));
		}
		break;
	    }
	    case OPT_INDEX: {
		if (argv.length != 4) {
		    throw new TclNumArgsException(interp, 1, argv, 
			    "index string charIndex");
		}
		int i = TclInteger.get(interp, argv[3]);
		if (i < 0 || i >= argv[2].toString().length()) {
		    interp.resetResult();
		} else {
		    char c[] = {argv[2].toString().charAt(i)};
		    interp.setResult(new String(c));
		}
		break;
	    }
	    case OPT_LENGTH: {
		if (argv.length != 3) {
		    throw new TclNumArgsException(interp, 1, argv, 
			    "length string");
		}
		interp.setResult(argv[2].toString().length());
		break;
	    }
	    case OPT_MATCH: {
		if (argv.length != 4) {
		    throw new TclNumArgsException(interp, 1, argv, 
			    "match pattern string");
		}
		interp.setResult(Util.stringMatch(argv[3].toString(),
			argv[2].toString()));
		break;
	    }
	    case OPT_RANGE: {
		if (argv.length != 5) {
		    throw new TclNumArgsException(interp, 1, argv, 
			    "range string first last");
		}

		String str = argv[2].toString();
		int first, strlen, last;
		strlen = str.length();

		first = TclInteger.getForIndex(interp, argv[3], strlen-1);
		if (first < 0) {
		    first = 0;
		}
		last = TclInteger.getForIndex(interp, argv[4], strlen-1);
		if (last >= strlen) {
		    last = strlen-1;
		}

		if (first > last) {
		    interp.resetResult();
		} else {
		    interp.setResult(str.substring(first, last+1));
		}
		break;
	    }
	    case OPT_TOLOWER: {
		if (argv.length != 3) {
		    throw new TclNumArgsException(interp, 1, argv, 
			    "tolower string");
		}
		interp.setResult(argv[2].toString().toLowerCase());
		break;
	    }
	    case OPT_TOUPPER: {
		if (argv.length != 3) {
		    throw new TclNumArgsException(interp, 1, argv, 
			    "toupper string");
		}
		interp.setResult(argv[2].toString().toUpperCase());
		break;
	    }
	    case OPT_TRIM: {
		if (argv.length == 3) {
		    /*
		     * Case 1: "string trim str" --
		     * Remove leading and trailing white space
		     */ 
		    interp.setResult(argv[2].toString().trim());
		} else if (argv.length == 4) {

		    /*
		     * Case 2: "string trim str chars" --
		     * Remove leading and trailing chars in the chars set
		     */ 

		    String tmp = Util.TrimLeft(argv[2].toString(),
			    argv[3].toString());
		    interp.setResult(Util.TrimRight(tmp, argv[3].toString()));
		} else {
		    /*
		     * Case 3: Wrong # of args
		     */ 
		    throw new TclNumArgsException(interp, 1, argv, 
			    "trim string ?chars?");
		}
		break;
	    }
	    case OPT_TRIMLEFT: {
		if (argv.length == 3) {
		    /*
		     * Case 1: "string trimleft str" --
		     * Remove leading and trailing white space
		     */ 
		    interp.setResult(Util.TrimLeft(argv[2].toString()));
		} else if (argv.length == 4) {
		    /*
		     * Case 2: "string trimleft str chars" --
		     * Remove leading and trailing chars in the chars set
		     */ 
		    interp.setResult(Util.TrimLeft(argv[2].toString(),
			    argv[3].toString()));
		} else {
		    /*
		     * Case 3: Wrong # of args
		     */ 
		    throw new TclNumArgsException(interp, 1, argv, 
			    "trimleft string ?chars?");
		}
		break;
	    }
	    case OPT_TRIMRIGHT: {
		if (argv.length == 3) {
		    /*
		     * Case 1: "string trimright str" --
		     * Remove leading and trailing white space
		     */ 
		    interp.setResult(Util.TrimRight(argv[2].toString()));
		} else if (argv.length == 4) {
		    /*
		     * Case 2: "string trimright str chars" --
		     * Remove leading and trailing chars in the chars set
		     */ 
		    interp.setResult(Util.TrimRight(argv[2].toString(),
			    argv[3].toString()));
		} else {
		    /*
		     * Case 3: Wrong # of args
		     */ 
		    throw new TclNumArgsException(interp, 1, argv, 
			    "trimright string ?chars?");
		}
		break;
	    }
	    case OPT_WORDEND: {
		if (argv.length != 4) {
		    throw new TclNumArgsException(interp, 1, argv, 
			    "wordend string index");
		}

		String str = argv[2].toString();
		char strArray[] = str.toCharArray();
		int cur;
		int length = str.length();
		int index = TclInteger.get(interp, argv[3]);

		if (index < 0) {
		    index = 0;
		}
		if (index > length) {
		    interp.setResult(length);
		    return;
		}
		for (cur = index ; cur < length; cur++) {
		    char c = strArray[cur];
		    if (!Util.isLetterOrDigit(c) && (c != '_')) {
			break;
		    }
		}
		if (cur == index) {
		    cur = index+1;
		}
		interp.setResult(cur);
		break;
	    }
	    case OPT_WORDSTART: {
		if (argv.length != 4) {
		    throw new TclNumArgsException(interp, 1, argv, 
					  "wordstart string index");
		}

		String str = argv[2].toString();
		char strArray[] = str.toCharArray();
		int cur;
		int length = str.length();
		int index = TclInteger.get(interp, argv[3]);

		if (index > length) {
		    index = length-1;
		}
		if (index < 0) {
		    interp.setResult(0);
		    return;
		}
		for (cur = index ; cur >= 0; cur--) {
		    char c = strArray[cur];
		    if (!Util.isLetterOrDigit(c) && (c != '_')) {
			break;
		    }
		}
		if (cur != index) {
		    cur += 1;
		}
		interp.setResult(cur);
		break;
	    }
	    default: { 
		throw new TclRuntimeError("TclIndex.get() error");
	    }
	}
    }
}


