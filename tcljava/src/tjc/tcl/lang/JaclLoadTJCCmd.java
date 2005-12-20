/*
 * Copyright (c) 2005 Advanced Micro Devices, Inc.
 *
 * See the file "license.amd" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: JaclLoadTJCCmd.java,v 1.1 2005/12/20 23:00:11 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This class implements a small helper function that is used to
 * load the TJC package into Jacl without requiring that the
 * Java package be loaded into Jacl.
 */

class JaclLoadTJCCmd implements Command {

public void 
cmdProc(
    Interp interp,   			// Current interpreter. 
    TclObject[] objv)			// Arguments to "jaclloadtjc" cmd
throws TclException
{
    // This method takes no arguments
    if (objv.length != 1) {
	throw new TclNumArgsException(interp, 1, objv, "");
    }

    interp.eval("namespace eval TJC {}");
    interp.createCommand("::TJC::command", new TJCCommandCmd());
    interp.createCommand("::TJC::package", new TJCPackageCmd());

    // Now that we have loaded the TJC package we can delete this command
    // from the interp.

    interp.deleteCommand(objv[0].toString());
}

}
