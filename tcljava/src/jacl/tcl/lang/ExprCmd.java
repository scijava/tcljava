/*
 * ExprCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ExprCmd.java,v 1.1 1998/10/14 21:09:20 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "expr" command in Tcl.
 */

class ExprCmd implements Command {
    /**
     * Evaluates a Tcl expression. See Tcl user documentation for
     * details.
     * @exception TclException If malformed expression.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv.length < 2) {
	    throw new TclNumArgsException(interp, 1, argv, "arg ?arg ...?");
	}

	if (argv.length == 2) {
	    interp.setResult(interp.expr.eval(interp,
		    argv[1].toString()));
	} else {
	    StringBuffer sbuf = new StringBuffer();
	    sbuf.append(argv[1]);
	    for (int i = 2; i < argv.length; i++) {
		sbuf.append(" ");
		sbuf.append(argv[i].toString());
	    }
	    interp.setResult(interp.expr.eval(interp, sbuf.toString()));
	}
    }
}

