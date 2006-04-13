/*
 * Copyright (c) 2005 Advanced Micro Devices, Inc.
 *
 * See the file "license.amd" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TJCPackageCmd.java,v 1.3 2006/04/13 07:36:51 mdejong Exp $
 *
 */

package tcl.lang;

public class TJCPackageCmd implements Command {

// Implementation of TJC::package used to load
// packages at runtime via a Java package name.

public void
    cmdProc(
        Interp interp,
        TclObject[] objv)
            throws TclException
    {
        if (objv.length != 2) {
            throw new TclNumArgsException(interp, 1, objv,
                "package");
        }
        String pkgname = objv[1].toString();
        String classname =
            (pkgname.equals("default") ? "TJCExtension" : pkgname + ".TJCExtension");

        // Create instance of Extension
        Class c = null;
        try {
            ClassLoader tclClassLoader = interp.getClassLoader();
            c = tclClassLoader.loadClass(classname);
        } catch (ClassNotFoundException cnfe) {
            throw new TclException(interp,
                "class " + classname + " not found");
        } catch (PackageNameException pne) {
            throw new TclException(interp,
                "class " + classname + " could not be loaded");
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
        if (!(o instanceof Extension)) {
            throw new TclException(interp,
                "class " + classname + " must extend Extension");
        }
        Extension ext = (Extension) o;
        ext.init(interp);
        interp.resetResult();
        return;
    }
}

