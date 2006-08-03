/* 
 * TestInterpDeleteCmd.java --
 *
 *	This procedure implements the "testinterpdelete" command.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: TestInterpDeleteCmd.java,v 1.1 2006/08/03 23:24:03 mdejong Exp $
 */

package tcl.lang;

public class TestInterpDeleteCmd implements Command  {

public void
cmdProc(
    Interp interp,		// Current interpreter.
    TclObject[] objv)	 	// The argument objects.
throws
    TclException
{
    Interp slaveToDelete;

    if (objv.length != 2) {
	throw new TclNumArgsException(interp, 1, objv, "path");
    }
    slaveToDelete = InterpSlaveCmd.getSlave(interp, objv[1]);
    slaveToDelete.dispose();
}
} // end TestParsevarCmd

