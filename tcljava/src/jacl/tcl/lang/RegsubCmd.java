/*
 * RegsubCmd.java
 *
 * 	This contains the Jacl implementation of the built-in Tcl
 *	"regsub" command.
 *
 * Copyright (c) 1997-1999 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: RegsubCmd.java,v 1.4 2000/02/23 22:07:23 mo Exp $
 */

package tcl.lang;

import sunlabs.brazil.util.regexp.Regexp;
import sunlabs.brazil.util.regexp.Regsub;

/**
 * This class implements the built-in "regsub" command in Tcl.
 */

class RegsubCmd implements Command {

static final private String validOpts[] = {
    "-all",
    "-nocase",
    "--"
};
static final private int OPT_ALL	= 0;
static final private int OPT_NOCASE   	= 1;
static final private int OPT_LAST 	= 2;

/*
 *-----------------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked to process the "regsub" Tcl command.
 *	See the user documentation for details on what it does.
 *
 * Results:
 *	A standard Tcl result.
 *
 * Side effects:
 *	See the user documentation.
 *
 *-----------------------------------------------------------------------------
 */

public void
cmdProc(
    Interp interp,   			// Current interpreter. 
    TclObject argv[])			// Arguments to "regsub" command.
throws TclException 
{
    boolean all = false;
    boolean nocase = false;

    try {
	int i = 1;
opts:
	while (argv[i].toString().startsWith("-")) {
	    int index = TclIndex.get(interp, argv[i], validOpts, "switch", 0);
	    i++;
	    switch (index) {
		case OPT_ALL: {
		    all = true;
		    break;
		}
		case OPT_NOCASE: {
		    nocase = true;
		    break;
		}
		case OPT_LAST: {
		    break opts;
		}
	    }
	}

	TclObject exp = argv[i++];
	String string = argv[i++].toString();
	String subSpec = argv[i++].toString();
	String varName = argv[i++].toString();
	if (i != argv.length) {
	    throw new IndexOutOfBoundsException();
	}

	Regexp r = TclRegexp.compile(interp, exp, nocase);

	int count = 0;
	String result;

	if (all == false) {
	    result = r.sub(string, subSpec);
	    if (result == null) {
		result = string;
	    } else {
		count++;
	    } 
	} else {
	    StringBuffer sb = new StringBuffer();
	    Regsub s = new Regsub(r, string);
	    while (s.nextMatch()) {
		count++;
		sb.append(s.skipped());
		Regexp.applySubspec(s, subSpec, sb);
	    }
	    sb.append(s.rest());
	    result = sb.toString();
	}

	TclObject obj = TclString.newInstance(result);
	try {
	    interp.setVar(varName, obj, 0);
	} catch (TclException e) {
	    throw new TclException(interp,
		    "couldn't set variable \"" + varName + "\"");
	}
	interp.setResult(count);
    } catch (IndexOutOfBoundsException e) {
	throw new TclNumArgsException(interp, 1, argv,
		"?switches? exp string subSpec varName");
    }
}
} // end RegsubCmd
