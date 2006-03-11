/* 
 * TestExprParserCmd.java --
 *
 *	This procedure implements the "testexprparser" command.  It is
 *	used for testing Parser.parseCommand.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: TestExprParserCmd.java,v 1.2 2006/03/11 22:35:54 mdejong Exp $
 */

package tcl.lang;

public class TestExprParserCmd implements Command {

/*
 *----------------------------------------------------------------------
 *
 * TestexprparserObjCmd -> cmdProc
 *
 *	This procedure implements the "testexprparser" command.  It is
 *	used for testing the new Tcl expression parser in Tcl 8.1.
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
    TclObject objv[])		// The argument objects.
throws
    TclException
{
    CharPointer script;
    String string;
    int length;
    TclParse parse = null;

    if (objv.length != 3) {
	throw new TclNumArgsException(interp, 1, objv, 
		"expr length");
    }

    string = objv[1].toString();
    script = new CharPointer(string);

    length = TclInteger.get(interp, objv[2]);
    if (length == 0) {
	length = string.length();
    }

    parse = ParseExpr.parseExpr(interp, script.array, script.index, length);

    if (parse.result != TCL.OK) {
	interp.addErrorInfo("\n    (remainder of script: \"");
	interp.addErrorInfo(new String(parse.string, parse.termIndex, 
		(parse.endIndex - parse.termIndex)));
	interp.addErrorInfo("\")");
	throw new TclException(TCL.ERROR);
    }

    // The parse completed successfully.  Just print out the contents
    // of the parse structure into the interpreter's result.

    try {
        TclObject tlist = TclList.newInstance();
        PrintParse(interp, parse, tlist);
        interp.setResult(tlist);
    } finally {
        parse.release();
    }
    return;
}

/*
 *----------------------------------------------------------------------
 *
 * PrintParse -> PrintParse
 *
 *	This procedure prints out the contents of a TclParse structure
 *	in the result of an interpreter.
 *
 * Results:
 *	Appends to the TclList obj containing the contents of parse.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static void
PrintParse(
    Interp interp,
    TclParse parse,
    TclObject obj)
        throws TclException
{
    String typeString;
    TclToken token;
    int i;

    if (parse.commentStart != -1) {
	TclList.append(interp, obj,
	    TclString.newInstance(
	        new String(parse.string, parse.commentStart, parse.commentSize)));
    } else {
	TclList.append(interp, obj,
	    TclString.newInstance("-"));
    }
    if (parse.commandStart != -1) {
        TclList.append(interp, obj,
            TclString.newInstance(
                new String(parse.string, parse.commandStart, parse.commandSize)));
    } else {
	TclList.append(interp, obj,
	    TclString.newInstance(""));
    }
    TclList.append(interp, obj,
        TclInteger.newInstance(parse.numWords));

    for (i = 0; i < parse.numTokens; i++) {
	token = parse.getToken(i);
	switch (token.type) {
	    case Parser.TCL_TOKEN_WORD:
		typeString = "word";
		break;
	    case Parser.TCL_TOKEN_SIMPLE_WORD:
		typeString = "simple";
		break;
	    case Parser.TCL_TOKEN_TEXT:
		typeString = "text";
		break;
	    case Parser.TCL_TOKEN_BS:
		typeString = "backslash";
		break;
	    case Parser.TCL_TOKEN_COMMAND:
		typeString = "command";
		break;
	    case Parser.TCL_TOKEN_VARIABLE:
		typeString = "variable";
		break;
	    case Parser.TCL_TOKEN_SUB_EXPR:
		typeString = "subexpr";
		break;
	    case Parser.TCL_TOKEN_OPERATOR:
		typeString = "operator";
		break;
	    default:
		typeString = "??";
		break;
	}
	TclList.append(interp, obj,
	    TclString.newInstance(typeString));
	TclList.append(interp, obj,
	    TclString.newInstance(
	        new String(token.script_array, token.script_index, token.size)));
	TclList.append(interp, obj,
	    TclInteger.newInstance(token.numComponents));
    }
    if (parse.commandStart != -1) {
        int index = parse.commandStart + parse.commandSize;
        // Append rest of command string, going past parse termination index.
        //int len = parse.endIndex - index;
        int len = (parse.string.length - 1) - index;
        TclList.append(interp, obj,
            TclString.newInstance(
                new String(parse.string, index, len)));
    } else {
	TclList.append(interp, obj,
	    TclString.newInstance(""));
    }
}

} // end class TestExprParserCmd

