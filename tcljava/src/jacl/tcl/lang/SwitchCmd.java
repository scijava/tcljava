/*
 * SwitchCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: SwitchCmd.java,v 1.1 1998/10/14 21:09:20 cvsadmin Exp $
 *
 */

package tcl.lang;

/*
 * This class implements the built-in "switch" command in Tcl.
 */

class SwitchCmd implements Command {

static final private String validCmds[] = {
    "-exact",
    "-glob",
    "-regexp",
    "--"
};
private static final int EXACT  = 0;
private static final int GLOB   = 1;
private static final int REGEXP = 2;
private static final int LAST   = 3;

/*
 *-----------------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked to process the "switch" Tcl statement.
 *	See the user documentation for details on what it does.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	See the user documentation.
 *
 *-----------------------------------------------------------------------------
 */

public void 
cmdProc(
    Interp interp,   			// Current interpreter. 
    TclObject argv[])			// Arguments to "switch" statement.
throws TclException
{
    int i, code, mode, body;
    boolean matched;
    String string;
    TclObject switchArgv[] = null;

    mode = EXACT;
    for (i = 1; i < argv.length; i++) {
	if (!argv[i].toString().startsWith("-")) {
	    break;
	}
	int opt = TclIndex.get(interp, argv[i], validCmds, "option", 1);
	if (opt == LAST) {
	    i++;
	    break;
	} else if (opt > LAST) {
	    throw new TclException(interp, 
		    "SwitchCmd.cmdProc: bad option " + opt 
		    + " index to validCmds");
	} else {
	    mode = opt;
	}
    }

    if (argv.length - i < 2) {
	throw new TclNumArgsException(interp, 1, argv, 
		"?switches? string pattern body ... ?default body?");
    }
    string = argv[i].toString();
    i++;

    /*
     * If all of the pattern/command pairs are lumped into a single
     * argument, split them out again.
     */
    
    if (argv.length - i == 1) {
	switchArgv = TclList.getElements(interp, argv[i]);
	i = 0;
    } else {
	switchArgv = argv;
    }

    for (; i < switchArgv.length; i += 2) {
	if (i == (switchArgv.length-1)) {
	    throw new TclException(interp,
		    "extra switch pattern with no body");
	}

	/*
	 * See if the pattern matches the string.
	 */

	matched = false;
	String pattern = switchArgv[i].toString();
	
	if ((i == switchArgv.length-2) && pattern.equals("default")) {
	    matched = true;
	} else {
	    switch (mode) {
	    case EXACT:
		matched = string.equals(pattern);
		break;
	    case GLOB:
		matched = Util.stringMatch(string, pattern);
		break;
	    case REGEXP:
		matched = Util.regExpMatch(interp, string, switchArgv[i]);
		break;
	    }
	}
	if (!matched) {
	    continue;
	}

	/*
	 * We've got a match.  Find a body to execute, skipping bodies
	 * that are "-".
	 */

	for (body = i+1; ; body += 2) {
	    if (body >= switchArgv.length) {
		throw new TclException(interp,
			"no body specified for pattern \"" + 
			switchArgv[i] + "\"");
	    }
	    if (!switchArgv[body].toString().equals("-")) {
		break;
	    }
	}
	
	try {
	    interp.eval(switchArgv[body], 0);
	    return;
	} catch (TclException e) {
	    if (e.getCompletionCode() == TCL.ERROR) {
		interp.addErrorInfo(
		    "\n    (\"" + switchArgv[i] + 
		    "\" arm line " + interp.errorLine + ")");
	    }
	    throw e;
	}
    }
    
    /*
     * Nothing matched:  return nothing.
     */
}

} // end SwitchCmd

