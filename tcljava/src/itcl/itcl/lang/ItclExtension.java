/*
 * ItclExtension.java
 *
 *    Load Itcl package commands/ 
 *
 * Copyright (c) 2004 Mo DeJong
 *
 * See the file "license.itcl" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ItclExtension.java,v 1.1 2005/09/11 20:56:57 mdejong Exp $
 *
 */

package itcl.lang;

import tcl.lang.*;

public class
ItclExtension extends Extension implements Command {
    /*
     * Called via [java::load itcl.lang.ItclExtension]
     * or from the jaclloaditcl command implemented below.
     */

    public void init(Interp interp)
        throws TclException
    {
        boolean issafe = false;

        TclObject result;
        interp.eval("interp issafe {}");
        result = interp.getResult();
        issafe = TclBoolean.get(interp, result);

        if (issafe) {
            Cmds.SafeInit(interp);
        } else {
            Cmds.Init(interp);
        }
    }

    /*
     * Invoked when [package require Itcl] is run from Tcl.
     * This method is needed so that Itcl can be loaded
     * without having first loaded the Java package.
     */

    public void 
    cmdProc(
        Interp interp,   			// Current interpreter. 
        TclObject[] objv)			// Arguments to "jaclloaditcl" command.
        throws TclException
    {
        // This method takes no arguments
        if (objv.length != 1) {
            throw new TclNumArgsException(interp, 1, objv, "");
        }

        this.init(interp);

        interp.deleteCommand(objv[0].toString());
    }
}

