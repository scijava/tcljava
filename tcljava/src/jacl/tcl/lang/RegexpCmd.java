/*
 * RegexpCmd.java --
 *
 * 	This file contains the Jacl implementation of the built-in Tcl
 *	"regexp" command. 
 *
 * Copyright (c) 1997-1999 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: RegexpCmd.java,v 1.3 2000/02/23 22:07:23 mo Exp $
 */

package tcl.lang;

import sunlabs.brazil.util.regexp.Regexp;

/**
 * This class implements the built-in "regexp" command in Tcl.
 */

class RegexpCmd implements Command {

static final private String validOpts[] = {
    "-indices",
    "-nocase",
    "--"
};
static final private int OPT_INDICES 	= 0;
static final private int OPT_NOCASE   	= 1;
static final private int OPT_LAST 	= 2;

/*
 *-----------------------------------------------------------------------------
 *
 * init --
 *
 *	This procedure is invoked to connect the regexp and regsub commands to
 *	the CmdProc method of the RegexpCmd and RegsubCmd classes,
 *	respectively.  Avoid the AutoloadStub class because regexp and regsub
 *	need a stub with a switch to check for the existence of the tcl.regexp
 *	package.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The regexp and regsub s]commands are now connected to the CmdProc
 *	method of the RegexpCmd and RegsubCmd classes, respectively.
 *
 *-----------------------------------------------------------------------------
 */

static void
init(
    Interp interp)  			// Current interpreter. 
{
    interp.createCommand("regexp", new tcl.lang.RegexpCmd());
    interp.createCommand("regsub", new tcl.lang.RegsubCmd());
}	    

/*
 *-----------------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked to process the "regexp" Tcl command.
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
    TclObject argv[])			// Arguments to "regexp" command.
throws TclException 
{
    boolean nocase = false;
    boolean indices = false;

    try {
	int i = 1;
opts:
	while (argv[i].toString().startsWith("-")) {
	    int index = TclIndex.get(interp, argv[i], validOpts, "switch", 0);
	    i++;
	    switch (index) {
		case OPT_INDICES: {
		    indices = true;
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

	int matches = argv.length - i;

	Regexp r = TclRegexp.compile(interp, exp, nocase);

	int[] args = new int[matches * 2];
	boolean matched = r.match(string, args);
	if (matched) {
	    for (int match = 0; i < argv.length; i++) {
		TclObject obj;

		int start = args[match++];
		int end = args[match++];
		if (indices) {
		    if (end >= 0) {
			end--;
		    }
		    obj = TclList.newInstance();
		    TclList.append(interp, obj, TclInteger.newInstance(start));
		    TclList.append(interp, obj, TclInteger.newInstance(end));
		} else {
		    String range = (start >= 0)
			    ? string.substring(start, end)
			    : "";
		    obj = TclString.newInstance(range);
		}
		try {
		    interp.setVar(argv[i].toString(), obj, 0);
		} catch (TclException e) {
		    throw new TclException(interp,
			    "couldn't set variable \"" + argv[i] + "\"");
		}
	    }
	}
	interp.setResult(matched);
    } catch (IndexOutOfBoundsException e) {
	throw new TclNumArgsException(interp, 1, argv,
		"?switches? exp string ?matchVar? ?subMatchVar subMatchVar ...?");
    }
}

} // end RegexpCmd
