/*
 * TestsetplatformCmd.java --
 *
 *	This file contains the Jacl implementation of the built-in Tcl test
 *	commands:  testsetplatform.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TestsetplatformCmd.java,v 1.1 1999/05/10 04:08:52 dejong Exp $
 *
 */

package tcl.lang;

/*
 * This class implements the built-in test command:  testsetplatform.
 */

class TestsetplatformCmd implements Command {

    static Class procClass = null;

    static final private String validCmds[] = {
	"unix",
	"windows",
	"mac"
    };


/*
 *----------------------------------------------------------------------
 *
 * CmdProc --
 *
 *	This procedure is invoked to process the "testsetplatform" Tcl command.
 *	This command is only used in the test suite.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	From now on, the "fileCmd" object will behave as though we are running
 *	on the platform specified in this procedure call.
 *
 *----------------------------------------------------------------------
 */

public void 
cmdProc(
    Interp interp,  			// Current interp to eval the file cmd.
    TclObject argv[])
throws
    TclException
{
    if (argv.length != 2) {
	throw new TclNumArgsException(interp, 1, argv, "platform");
    }

    JACL.PLATFORM = TclIndex.get(interp, argv[1], validCmds, "platform", 0);
    interp.setResult("");
    return;
}

} // end class TestsetplatformCmd

