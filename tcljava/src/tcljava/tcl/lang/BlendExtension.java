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
 * RCS: @(#) $Id: BlendExtension.java,v 1.6 1999/05/09 21:12:32 dejong Exp $
 */

package tcl.lang;

public class BlendExtension extends Extension {

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
    // init Java object reflection system
    ReflectObject.init(interp);

    // Create the commands in the Java package

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
    loadOnDemand(interp, "java::try",         "tcl.lang.JavaTryCmd");


    // Part of the java package is defined in Tcl code.  We source
    // in that code now.
    
    interp.evalResource("/tcl/lang/library/java/javalock.tcl");

    // Note that we cannot set a variable in a namespace until the namespace
    // exists, so we must to do it after we create the commands.

    interp.setVar("java::version", TclString.newInstance(
	System.getProperty("java.version")), TCL.GLOBAL_ONLY);

    // FIXME : provide automatic way to set java package version

    // Provide the Tcl/Java package with its version info.
    // The version is also set in:
    // src/jacl/tcl/lang/Interp.java
    // src/pkgIndex.tcl
    // win/makefile.vc
    // unix/configure.in

    interp.eval("package provide java 1.2.1");

}

} // end BlendExtension

