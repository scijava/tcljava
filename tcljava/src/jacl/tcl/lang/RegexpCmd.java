/*
 * RegexpCmd.java --
 *
 * 	This file contains Jacl implementation of the built-in Tcl "regexp"
 * 	command. 
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: RegexpCmd.java,v 1.1 1998/10/14 21:09:18 cvsadmin Exp $
 */

package tcl.lang;

/*
 * This class implements the built-in "regexp" command in Tcl.
 */

class RegexpCmd implements Command {

static final private String validCmds[] = {
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
 *	This procedure is invoked to load the RegexpCmd class on demand.  If
 *	the tcl.regexp.RegexpCmd exists, use that version.  Otherwise, use the
 *	tcl.lang.RegexpCmd stub class.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Connects the "regexp" command in the given interp to the cmdProc of the
 *	RegexpCmd class in the appropriate package.
 *
 *-----------------------------------------------------------------------------
 */

public void
cmdProc(
    Interp interp,   			// Current interpreter. 
    TclObject argv[])			// Arguments to "regexp" command.
throws TclException 
{
    Class cmdClass = null;
    Command cmd = null;
    try {
	cmdClass = Class.forName("tcl.regex.OroRegexpCmd");
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
    interp.createCommand("regexp", cmd);
    cmd.cmdProc(interp, argv);
}

/*
 *-----------------------------------------------------------------------------
 *
 * stubCmdProc --
 *
 *	This procedure is invoked to process the "regexp" Tcl command in the
 *	event that the tcl.regexp.RegexpCmd class cannot be found.  This method
 *	is just a stub which throws a TclException indicating that "regexp is
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
    boolean indices = false;
    boolean last = false;
    String pattern, string;

    if (argv.length < 3) {
	throw new TclNumArgsException(interp, 1, argv, 
	     "?switches? exp string ?matchVar? ?subMatchVar subMatchVar ...?");
    }
    for (currentObjIndex = 1; (objc > 0) && (!last); 
	 objc--, currentObjIndex++) {
	if (!argv[currentObjIndex].toString().startsWith("-")) {
	    break;
	}
	int opt = TclIndex.get(interp, argv[currentObjIndex],
		validCmds, "switch", 1);
	switch (opt) {
	case OPT_INDICES:
	    indices = true;
	    break;
	case OPT_NOCASE:
	    noCase = true;
	    break;
	case OPT_LAST:
	    last = true;
	    break;
	default:
	    throw new TclException(interp, 
		    "RegexpCmd.cmdProc: bad option " + opt 
		    + " index to validCmds");
	}
    }
    if (objc < 2) {
	throw new TclNumArgsException(interp, 1, argv, 
	        "?switches? exp string ?matchVar? ?subMatchVar subMatchVar ...?");
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
    throw new TclException(interp,
	    "Can't execute regexp \"" + pattern + " " + string
	    + "\": not yet implemented");
}
} // end RegexpCmd


