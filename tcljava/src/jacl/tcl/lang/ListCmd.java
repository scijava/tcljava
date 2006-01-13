/*
 * ListCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ListCmd.java,v 1.2 2006/01/13 03:40:11 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "list" command in Tcl.
 */
class ListCmd implements Command {

    /**
     * See Tcl user documentation for details.
     */
    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	TclObject list = TclList.newInstance();

	try {
	    for (int i = 1; i<argv.length; i++) {
		TclList.append(interp, list, argv[i]);
	    }
	    interp.setResult(list);
        } catch (TclException te) {
            list.release();
            throw te;
        }
    }
}

