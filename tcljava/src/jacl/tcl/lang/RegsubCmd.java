/*
 * RegsubCmd.java
 *
 * 	This contains Jacl implementation of the built-in Tcl "regsub" command.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: RegsubCmd.java,v 1.1 1998/10/14 21:09:20 cvsadmin Exp $
 */

package tcl.lang;

/*
 * This class implements the built-in "regsub" command in Tcl.
 */

class RegsubCmd implements Command {

static final private String validCmds[] = {
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
 *	This procedure is invoked to load the RegsubCmd class on demand.  If
 *	the tcl.regexp.RegsubCmd exists, use that version.  Otherwise, use the
 *	tcl.lang.RegsubCmd stub class.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Connects the "regsub" command in the given interp to the cmdProc of the
 *	RegsubCmd class in the appropriate package.
 *
 *-----------------------------------------------------------------------------
 */

public void
cmdProc(
    Interp interp,   			// Current interpreter. 
    TclObject argv[])			// Arguments to "regsub" command.
throws TclException 
{
    Class cmdClass = null;
    Command cmd = null;
    try {
	cmdClass = Class.forName("tcl.regex.OroRegsubCmd");
    } catch (ClassNotFoundException e) {
	stubCmdProc(interp, argv);
	return;
    }

    try {
	cmd = (Command)cmdClass.newInstance();
    } catch (IllegalAccessException e1) {
	throw new TclException(interp,
		"IllegalAccessException for class \"" + cmdClass.getName()
		+ "\"");
    } catch (InstantiationException e2) {
	throw new TclException(interp,
		"InstantiationException for class \"" + cmdClass.getName()
		+ "\"");
    } catch (ClassCastException e3) {
	throw new TclException(interp,
		"ClassCastException for class \"" + cmdClass.getName()
		+ "\"");
    }
    interp.createCommand("regsub", cmd);
    cmd.cmdProc(interp, argv);
}

/*
 *-----------------------------------------------------------------------------
 *
 * stubCmdProc --
 *
 *	This procedure is invoked to process the "regsub" Tcl command in the
 *	event that the tcl.regexp.RegsubCmd class cannot be found.  This method
 *	is just a stub which throws a TclException indicating that "regsub is
 *	not yet implemented". 
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	See the user documentation.
 *
 *-----------------------------------------------------------------------------
 */

private static void 
stubCmdProc(
    Interp interp,  			// Current interp to eval the file cmd.
    TclObject argv[])			// Args passed to the file command.
throws TclException 
{
    int currentObjIndex, stringObjIndex, matchIndex;
    int objc = argv.length - 1;
    boolean noCase = false;
    boolean all = false;
    boolean last = false;
    String pattern, string;

    if (argv.length < 3) {
	throw new TclNumArgsException(interp, 1, argv, 
	        "?switches? exp string subSpec varName");
    }
    for (currentObjIndex = 1; (objc > 0) && (!last); 
	 currentObjIndex++, objc--) {
	if (!argv[currentObjIndex].toString().startsWith("-")) {
	    break;
	}
	int opt = TclIndex.get(interp, argv[currentObjIndex],
		validCmds, "switch", 1);
	switch (opt) {
	case OPT_ALL:
	    all = true;
	    break;
	case OPT_NOCASE:
	    noCase = true;
	    break;
	case OPT_LAST:
	    last = true;
	    break;
	default:
	    throw new TclException(interp, 
		    "RegsubCmd.cmdProc: bad option " + opt 
		    + " index to cmds");
	}
    }
    if (objc != 4) {
	throw new TclNumArgsException(interp, 1, argv, 
	        "?switches? exp string subSpec varName");
    }
    
    /*
     * Convert the string and pattern to lower case, if desired.
     */

    stringObjIndex = currentObjIndex + 1;
    matchIndex = stringObjIndex + 1;
    if (noCase) {
	pattern = argv[currentObjIndex].toString().toLowerCase();
	string = argv[stringObjIndex].toString().toLowerCase();
    } else {
	pattern = argv[currentObjIndex].toString();
	string = argv[stringObjIndex].toString();
    }

    String subSpec = argv[stringObjIndex + 1].toString();
    String varName = argv[stringObjIndex + 2].toString();

    throw new TclException(interp,
	    "Can't execute regsub \"" + pattern + " " + string + " "
            + subSpec + " " + varName + "\": not yet implemented");
}
} // end RegsubCmd


