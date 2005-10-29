/* 
 * TestParsevarnameCmd.java --
 *
 *	This procedure implements the "testparsevarname" command.  It is
 *	used for testing Parser.parseVarName.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: TestParsevarnameCmd.java,v 1.2 2005/10/29 00:27:43 mdejong Exp $
 */

package tcl.lang;

public class TestParsevarnameCmd implements Command {

public void
cmdProc(
    Interp interp,		// Current interpreter.
    TclObject[] objv)		// The argument objects.
throws 
    TclException
{
    String string;
    int length;
    boolean append;
    TclParse parse = null;
    CharPointer script;

    if (objv.length != 4) {
	throw new TclNumArgsException(interp, 1, objv, "script length append");
    }
    string = objv[1].toString();
    length = TclInteger.get(interp, objv[2]);
    if (length == 0) {
	length = string.length();
    }
    append = TclBoolean.get(interp, objv[3]);
    script = new CharPointer(string);
    parse = Parser.parseVarName(interp, script.array, script.index,
				length, parse, append);

    if (parse.result != TCL.OK) {
	interp.addErrorInfo("\n    (remainder of script: \"");
	interp.addErrorInfo(new String(parse.string, parse.termIndex, 
		(string.length() - parse.termIndex)));
	interp.addErrorInfo("\")");
	throw new TclException(TCL.ERROR);
    }

    // The parse completed successfully.  Just print out the contents
    // of the parse structure into the interpreter's result.

    parse.commentSize = 0;
    parse.commandStart = script.index + parse.tokenList[0].size;
    parse.commandSize = 0;
    try {
        TclObject tlist = TclList.newInstance();
        TestExprParserCmd.PrintParse(interp, parse, tlist);
        interp.setResult(tlist);
    } finally {
        parse.release();
    }
    return;
}
} // end TestParsevarnameCmd
