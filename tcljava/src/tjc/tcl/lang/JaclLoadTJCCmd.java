/*
 * Copyright (c) 2005 Advanced Micro Devices, Inc.
 *
 * See the file "license.amd" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: JaclLoadTJCCmd.java,v 1.2 2006/02/14 04:13:27 mdejong Exp $
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

    // Init the namespace so commands can be created.
    interp.eval("namespace eval TJC {}");

    // Load TJC class files as needed.
    Extension.loadOnDemand(interp, "::TJC::command", "tcl.lang.TJCCommandCmd");
    Extension.loadOnDemand(interp, "::TJC::compile", "tcl.lang.TJCCompileCmd");
    Extension.loadOnDemand(interp, "::TJC::package", "tcl.lang.TJCPackageCmd");

    // Now that we have loaded the TJC package we can delete this command
    // from the interp.

    interp.deleteCommand(objv[0].toString());
}

}
