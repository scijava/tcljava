/* 
 * BlendExtension.java --
 *
 *	This extension encapsulates the java::* commands.
 *
 * Copyright (c) 1997-1998 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: BlendExtension.java,v 1.3 1999/01/15 03:06:54 hylands Exp $
 */

package tcl.lang;

class BlendExtension extends Extension {

/*
 *----------------------------------------------------------------------
 *
 * init --
 *
 *	Initialize the java pacakge.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Creates the java namespace and java::* commands.
 *
 *----------------------------------------------------------------------
 */

public void
init(
    Interp interp)		// Interpreter to intialize.
throws TclException
{
    loadOnDemand(interp, "java::bind",        "tcl.lang.JavaBindCmd");
    loadOnDemand(interp, "java::call",        "tcl.lang.JavaCallCmd");
    loadOnDemand(interp, "java::cast",        "tcl.lang.JavaCastCmd");
    loadOnDemand(interp, "java::defineclass", "tcl.lang.JavaDefineClassCmd");
    loadOnDemand(interp, "java::event",       "tcl.lang.JavaEventCmd");
    loadOnDemand(interp, "java::field",       "tcl.lang.JavaFieldCmd");
    loadOnDemand(interp, "java::getinterp",   "tcl.lang.JavaGetInterpCmd");
    loadOnDemand(interp, "java::info",        "tcl.lang.JavaInfoCmd");
    loadOnDemand(interp, "java::instanceof",  "tcl.lang.JavaInstanceofCmd");
    loadOnDemand(interp, "java::isnull",      "tcl.lang.JavaIsNullCmd");
    loadOnDemand(interp, "java::load",        "tcl.lang.JavaLoadCmd");
    loadOnDemand(interp, "java::new",         "tcl.lang.JavaNewCmd");
    loadOnDemand(interp, "java::null",        "tcl.lang.JavaNullCmd");
    loadOnDemand(interp, "java::prop",        "tcl.lang.JavaPropCmd");
    loadOnDemand(interp, "java::throw",       "tcl.lang.JavaThrowCmd");

    /*
     * Part of the java package is defined in Tcl code.  We source
     * in that code now.
     */
    
    // See src/pkgIndex.tcl for a list of other files that should
    // be updated if the version or patchLevel changes.

    String version = "1.1";
    // For minor versions:
    //String patchLevel = version + "b1";
    String patchLevel = version;

    interp.evalResource("/tcl/lang/library/java/javalock.tcl");

    /*
     * Note that we cannot set a variable in a namespace until the namespace
     * exists, so we must to do it after we create the commands.
     */

    interp.setVar("java::jdkVersion", TclString.newInstance(
	System.getProperty("java.version")), TCL.GLOBAL_ONLY);
    interp.setVar("java::patchLevel", TclString.newInstance(patchLevel),
        TCL.GLOBAL_ONLY);
    interp.eval("namespace eval ::java namespace export bind call defineclass event field getinterp instanceof lock new null prop throw unlock");
    interp.eval("package provide java " + version);

}

} // end BlendExtension

