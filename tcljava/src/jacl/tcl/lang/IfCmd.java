/*
 * PutsCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: IfCmd.java,v 1.2 2003/02/07 03:41:49 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "if" command in Tcl.
 */
class IfCmd implements Command {

    /**
     * See Tcl user documentation for details.
     * @exception TclException If incorrect number of arguments.
     */
    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	int i;
	boolean value;

	i = 1;
	while (true) {
	    /*
	     * At this point in the loop, argv and argc refer to an
	     * expression to test, either for the main expression or
	     * an expression following an "elseif".  The arguments
	     * after the expression must be "then" (optional) and a
	     * script to execute if the expression is true.
	     */
	     
	    if (i >= argv.length) {
		throw new TclException(interp,
			"wrong # args: no expression after \"" +
		        argv[i-1] +  "\" argument");
	    }
	    try {
	        value = interp.expr.evalBoolean(interp, argv[i].toString());
	    } catch (TclException e) {
		switch (e.getCompletionCode()) {
		case TCL.ERROR:
		    interp.addErrorInfo("\n    (\"if\" test expression)");
		    break;
		}
		throw e;
	    }

	    i++;
	    if ((i < argv.length) && (argv[i].toString().equals("then"))) {
		i++;
	    }
	    if (i >= argv.length) {
		throw new TclException(interp,
	    		"wrong # args: no script following \"" +
		    	argv[i-1] + "\" argument");
	    }
	    if (value) {
		try {
		    interp.eval(argv[i], 0);
		} catch (TclException e) {
		    switch (e.getCompletionCode()) {
		    case TCL.ERROR:
		        interp.addErrorInfo("\n    (\"if\" then script line " +
		                interp.errorLine + ")");
		        break;
		    }
		    throw e;
		}
		return;
	    }

	    /*
	     * The expression evaluated to false.  Skip the command, then
	     * see if there is an "else" or "elseif" clause.
	     */

	    i++;
	    if (i >= argv.length) {
		interp.resetResult();
		return ;
	    }
	    if (argv[i].toString().equals("elseif")) {
		i++;
		continue;
	    }
	    break;
	}

	/*
	 * Couldn't find a "then" or "elseif" clause to execute.
	 * Check now for an "else" clause.  We know that there's at
	 * least one more argument when we get here.
	 */

	if (argv[i].toString().equals("else")) {
	    i++;
	    if (i >= argv.length) {
		throw new TclException(interp,
		       "wrong # args: no script following \"else\" argument");
	    } else if (i != (argv.length - 1)) {
		throw new TclException(interp,
		    "wrong # args: extra words after \"else\" clause in " +
		    "\"if\" command");
	    }
	}
	try {
	    interp.eval(argv[i], 0);
	} catch (TclException e) {
	    switch (e.getCompletionCode()) {
	    case TCL.ERROR:
	        interp.addErrorInfo("\n    (\"if\" else script line " +
	                interp.errorLine + ")");
	       break;
	    }
	    throw e;
	}
    }
}

