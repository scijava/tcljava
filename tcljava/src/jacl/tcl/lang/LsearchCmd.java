/*
 * LsearchCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LsearchCmd.java,v 1.1 1998/10/14 21:09:20 cvsadmin Exp $
 *
 */

package tcl.lang;

/*
 * This class implements the built-in "lsearch" command in Tcl.
 */

class LsearchCmd implements Command {
  
static final private String validCmds[] = {
    "-exact",
    "-glob",
    "-regexp"
};
static final int EXACT  = 0;
static final int GLOB   = 1;
static final int REGEXP = 2;

/*
 *-----------------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked to process the "lsearch" Tcl command.
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
    TclObject argv[])			// Arguments to "lsearch" command.
throws TclException
{
    int mode = GLOB;
    TclObject pattern = null;
    TclObject list = null;

    if (argv.length == 4) {
	mode = TclIndex.get(interp, argv[1], validCmds, "search mode", 0);
	list = argv[2];
	pattern = argv[3];
    } else if (argv.length == 3) {
	list = argv[1];
	pattern = argv[2];
    } else {
	throw new TclNumArgsException(interp, 1, argv, "?mode? list pattern");
    }

    boolean match = false;
    int size = TclList.getLength(interp, list);

    for (int i = 0; i < size; i++) {
	TclObject o = TclList.index(interp, list, i);
	
	if ((o == pattern) || o.equals(pattern)) {
	    match = true;
	} else {
	    switch (mode) {
	    case EXACT:
		match = (o.toString().compareTo(pattern.toString()) == 0);
		break;
		
	    case GLOB:
		match = Util.stringMatch(o.toString(), pattern.toString());
		break;
		
	    case REGEXP:
		match = Util.regExpMatch(interp, o.toString(), pattern);
	    }
	}

	if (match) {
	    interp.setResult(i);
	    return;
	}
    }
    
    interp.setResult(-1);
    return;
}

} // end LsearchCmd
