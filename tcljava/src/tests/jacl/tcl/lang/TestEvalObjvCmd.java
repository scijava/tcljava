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
 * RCS: @(#) $Id: TestEvalObjvCmd.java,v 1.3 2005/10/29 00:27:43 mdejong Exp $
 */
package tcl.lang;

public class TestEvalObjvCmd implements Command {


/*
 *----------------------------------------------------------------------
 *
 * TestevalobjvObjCmd -> cmdProc
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
    Interp interp,		// Current interpreter.
    TclObject[] objv)		// The argument objects.
throws
    TclException
{
    boolean evalGlobal;
    TclObject[] newObjv;

    if (objv.length < 3) {
	throw new TclNumArgsException(interp, 1, objv,
		"global word ?word ...?");
    }
    evalGlobal = (TclInteger.get(interp, objv[1]) != 0);
    newObjv = new TclObject[objv.length-2];
    System.arraycopy(objv, 2, newObjv, 0, objv.length-2);
    Parser.evalObjv(interp, newObjv, -1,
	    ((evalGlobal) ? TCL.EVAL_GLOBAL : 0));
}
} // end TestEvalObjvCmd
