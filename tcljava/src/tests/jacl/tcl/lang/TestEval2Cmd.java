/* 
 * TestEval2Cmd.java --
 *
 *	|>Description.<|
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: TestEval2Cmd.java,v 1.1 1999/05/10 04:08:51 dejong Exp $
 */

package tcl.lang;

public class TestEval2Cmd implements Command {

/*
 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure implements the "testeval2" command.  It is
 *	used to test Tcl_Eval2.
 *
 * Results:
 *	None.
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
    int code, oldFlags, length, flags;
    CharPointer script;
    String string;

    if (objv.length == 1) {
	/*
	 * The command was invoked with no arguments, so just toggle
	 * the flag that determines whether we use Tcl_Eval2.
	 */

	if ((interp.flags & Parser.USE_EVAL_DIRECT) != 0) {
	    interp.flags &= ~Parser.USE_EVAL_DIRECT;
	    interp.setResult("disabling direct evaluation");
	} else {
	    interp.flags |= Parser.USE_EVAL_DIRECT;
	    interp.setResult("enabling direct evaluation");
	}
	return;
    }

    flags = 0;
    if (objv.length == 3) {
	string = objv[2].toString();
	if (!string.equals("global")) {
	    interp.setResult("bad value \"" + string + "\": must be global");
	    throw new TclException(TCL.ERROR);
	}
	flags |= Parser.TCL_EVAL_GLOBAL;
    } else if (objv.length != 2) {
	throw new TclNumArgsException(interp, 1, objv, "script ?global?");
    }
    interp.setResult("xxx");

    /*
     * Note, we have to set the USE_EVAL_DIRECT flag in the interpreter
     * in addition to calling Tcl_Eval2.  This is needed so that even nested
     * commands are evaluated directly.
     */

    oldFlags = interp.flags;
    interp.flags |= Parser.USE_EVAL_DIRECT;
    string = objv[1].toString();
    script = new CharPointer(string);
    Parser.eval2(interp, script.array, script.index, script.length(), flags); 
    interp.flags = (interp.flags & ~Parser.USE_EVAL_DIRECT)
	    | (oldFlags & Parser.USE_EVAL_DIRECT);
    return;
}
} // end TestEval2Cmd
