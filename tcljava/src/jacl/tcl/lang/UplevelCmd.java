/*
 * UplevelCmd.java --
 *
 *	Implements the "uplevel" command.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: UplevelCmd.java,v 1.2 1999/06/30 00:13:39 mo Exp $
 *
 */

package tcl.lang;

/*
 * This class implements the built-in "uplevel" command in Tcl.
 */

class UplevelCmd implements Command {


/*
 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked as part of the Command interface to
 *	process the "uplevel" Tcl command.  See the user documentation
 *	for details on what it does.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	See the user documentation.
 *
 *----------------------------------------------------------------------
 */

public void
cmdProc(
    Interp interp,		// Current interpreter.
    TclObject argv[])		// Argument list.
throws 
    TclException 		// A standard Tcl exception.
{
    if (argv.length < 2) {
	throw new TclNumArgsException(interp, 1, argv, 
		"?level? command ?arg ...?");
    }

    CallFrame savedVarFrame;
    CallFrame frame = null;
    TclObject cmd;
    int startIdx;

    // FIXME : varFrame hack (does getFrame throw an eception here?
    if (interp.varFrame != null) {
	frame = interp.varFrame.getFrame(argv[1].toString());
    } else {
	// uplevel called from global scope, is this bad?
    }

    if (frame != null) {
	// The level is specified.

	if (argv.length < 3) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "?level? command ?arg ...?");
	}
	startIdx = 2;
    } else {
	// The level is not specified. We are not in the global frame
	// (otherwise getFrame() would have thrown an exception).

	startIdx = 1;
	// FIXME : varFrame hack. see above
	if (interp.varFrame == null) {
	    frame = null;
	} else {
	    frame = interp.varFrame.callerVar;
	}
    }

    if (startIdx == argv.length -1) {
	cmd = argv[startIdx];
    } else {
	cmd = TclString.newInstance(Util.concat(startIdx, argv.length-1,
		argv));
    }
    cmd.preserve();

    savedVarFrame = interp.varFrame;
    interp.varFrame = frame;

    try {
	interp.eval(cmd, 0);
    } catch (TclException e) {
	if (e.getCompletionCode() == TCL.ERROR) {
	    interp.addErrorInfo("\n    (\"uplevel\" body line " +	
		    interp.errorLine + ")");
	}
	throw e;
    } finally {
	interp.varFrame = savedVarFrame;
	cmd.release();
    }
}

} // end UplevelCmd
