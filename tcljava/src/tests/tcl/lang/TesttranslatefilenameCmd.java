/*
 * TesttranslatefilenameCmd.java --
 *
 *	This file contains the Jacl implementation of the built-in Tcl test
 *	commands:  testtranslatefilename.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TesttranslatefilenameCmd.java,v 1.1 1998/10/14 21:09:13 cvsadmin Exp $
 *
 */

package tcl.lang;

/*
 * This class implements the built-in test command:  testtranslatefilename.
 * It is used to test the FileUtil.translateFileName method.
 */

class TesttranslatefilenameCmd implements Command {


/*
 *----------------------------------------------------------------------
 *
 * CmdProc --
 *
 *	This procedure is invoked to process the "testtranslatefilename"
 *	Tcl command.  This command is only used in the test suite.
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
    Interp interp,  			// Current interp to eval the file cmd.
    TclObject argv[])
throws
    TclException
{
    if (argv.length != 2) {
	throw new TclNumArgsException(interp, 1, argv, "path");
    }

    String result = FileUtil.translateFileName(interp, argv[1].toString());
    interp.setResult(result);
    return;
}

} // end class TesttranslatefilenameCmd

