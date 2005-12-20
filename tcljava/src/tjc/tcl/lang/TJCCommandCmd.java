/*
 * Copyright (c) 2005 Advanced Micro Devices, Inc.
 *
 * See the file "license.amd" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TJCCommandCmd.java,v 1.1 2005/12/20 23:00:11 mdejong Exp $
 *
 */

package tcl.lang;

public class TJCCommandCmd implements Command {

// Implementation of TJC::command used to create
// compiled command instances at runtime.

public void 
    cmdProc(
        Interp interp,
        TclObject[] objv)
            throws TclException
    {
        if (objv.length != 3) {
            throw new TclNumArgsException(interp, 1, objv,
                "cmdname classname");
        }
        String cmdname   = objv[1].toString();
        String classname = objv[2].toString();

        // FIXME: Currently, we don't make use of a Tcl
        // class loader here. The class needs to be on
        // the CLASSPATH right now anyway, so it does
        // not seem critical.

        // Create instance of named command

        Class c = null;
        try {
            c = Class.forName(classname);
        } catch (ClassNotFoundException cnfe) {
            throw new TclException(interp,
                "class " + classname + " not found");
        }
        Object o = null;
        try {
            o = c.newInstance();
        } catch  (InstantiationException ie) {
            throw new TclException(interp,
                "class " + classname + " could not be created");        
        } catch  (IllegalAccessException iae) {
            throw new TclException(interp,
                "class " + classname + " could not be created");        
        }
        if (!(o instanceof TJC.CompiledCommand)) {
            throw new TclException(interp,
                "class " + classname + " must extend TJC.CompiledCommand");
        }
        TJC.CompiledCommand cmd = (TJC.CompiledCommand) o;
        TJC.createCommand(interp, cmdname, cmd);
        interp.resetResult();
        return;
    }
}

