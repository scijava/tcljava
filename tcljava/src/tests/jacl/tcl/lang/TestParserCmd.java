/* 
 * TestParserCmd.java --
 *
 *	This procedure implements the "testparser" command.  It is
 *	used for testing Parser.parseCommand.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: TestParserCmd.java,v 1.2 2005/10/29 00:27:43 mdejong Exp $
 */

package tcl.lang;

public class TestParserCmd  implements Command {

/*
 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	|>description<|
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
    Interp interp,		// Current interpreter.
    TclObject[] objv)		// The argument objects.
throws
    TclException
{
    String string, fileName;
    int lineNumber, length;
    TclParse parse = null;
    CharPointer script;

    if (objv.length != 3) {
	throw new TclNumArgsException(interp, 1, objv, 
		"script length");
    }

    string = objv[1].toString();
    length = TclInteger.get(interp, objv[2]);
    if (length == 0) {
	length = string.length();
    }
    script = new CharPointer(string);
    fileName = "";
    lineNumber = -1;

    parse = Parser.parseCommand(interp, script.array, script.index,
	    length, fileName, lineNumber, false);

    if (parse.result != TCL.OK) {
	interp.addErrorInfo("\n    (remainder of script: \"");
	interp.addErrorInfo(new String(parse.string, parse.termIndex, 
		(parse.endIndex - parse.termIndex)));
	interp.addErrorInfo("\")");
	throw new TclException(TCL.ERROR);
    }

    // The parse completed successfully.  Just print out the contents
    // of the parse structure into the interpreter's result.

    parse.endIndex = string.length();
    interp.setResult(parse.get());
    return;
}
}
