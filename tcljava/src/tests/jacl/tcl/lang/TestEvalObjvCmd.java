/* 
 * TestEvalObjvCmd.java --
 *
 *	|>Description.<|
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: TestEvalObjvCmd.java,v 1.1 1999/05/10 04:08:51 dejong Exp $
 */
package tcl.lang;

public class TestEvalObjvCmd implements Command {


/*
 *----------------------------------------------------------------------
 *
 * TestevalobjvObjCmd --
 *
 *	This procedure implements the "testevalobjv" command.  It is
 *	used to test Tcl_EvalObjv.
 *
 * Results:
 *	A standard Tcl result.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public void 
cmdProc(
    Interp interp,		/* Current interpreter. */
    TclObject objv[])		/* The argument objects. */
throws
    TclException
{
    int length;
    boolean evalGlobal;
    String command;
    CharPointer script;
    TclObject[] newObjv;

    if (objv.length < 5) {
	throw new TclNumArgsException(interp, 1, objv,
		"command length global word ?word ...?");
    }
    command = objv[1].toString();
    script = new CharPointer(command);
    length = TclInteger.get(interp, objv[2]);
    evalGlobal = (TclInteger.get(interp, objv[3]) != 0);
    newObjv = new TclObject[(objv.length - 4)];
    System.arraycopy(objv, 4, newObjv, 0, (objv.length - 4));

    Parser.evalObjv(interp, newObjv, /*script,*/ length,
	    ((evalGlobal) ? Parser.TCL_EVAL_GLOBAL : 0));
}
} // end TestEvalObjvCmd
