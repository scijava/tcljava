/* 
 * TestParsevarCmd.java --
 *
 *	This procedure implements the "testparsevar" command.  It is
 *	used for testing Parser.parseVar.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: TestParsevarCmd.java,v 1.1 1998/10/14 21:09:13 cvsadmin Exp $
 */

package tcl.lang;

public class TestParsevarCmd implements Command  {

public void
cmdProc(
    Interp interp,		/* Current interpreter. */
    TclObject  objv[])	 	/* The argument objects. */
throws
    TclException
{
    String name;
    ParseResult parseResult;
    TclObject objResult;

    if (objv.length != 2) {
	throw new TclNumArgsException(interp, 1, objv, "varName");
    }
    name = objv[1].toString();
    parseResult = Parser.parseVar(interp, name);
    if (parseResult == null) {
	throw new TclException(TCL.ERROR);
    }
    
    interp.appendElement(parseResult.value.toString());
    if ((parseResult.nextIndex > 0) && 
	    (parseResult.nextIndex < name.length())) {
	interp.appendElement(name.substring(parseResult.nextIndex));
    } else {
	interp.appendElement("");
    }

    return;
}
} // end TestParsevarCmd
