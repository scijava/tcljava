/* 
 * JavaDefineClassCmd.java --
 *
 *	 This class implements the built-in "java::defineclass" command.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: JavaDefineClassCmd.java,v 1.1 1998/10/14 21:09:14 cvsadmin Exp $
 */

package tcl.lang;

class JavaDefineClassCmd implements Command {

/*
 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked to process the "java::defineclass" 
 *	Tcl comamnd.  See the user documentation for details on what
 *	it does.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	A standard Tcl result is stored in the interpreter.
 *
 *----------------------------------------------------------------------
 */

public void
cmdProc(
    Interp interp,			// Current interpreter.
    TclObject argv[])			// Argument list.
throws
    TclException			// A standard Tcl exception.
{
    byte[] classData;
    Class  result;
    TclClassLoader tclClassLoader;
    
    if (argv.length != 2) {
	throw new TclNumArgsException(interp, 1, argv,
		"classbytes");
    }

    classData = argv[1].toString().getBytes();

    tclClassLoader = new TclClassLoader(interp, null);
    result = tclClassLoader.defineClass(null, classData);
    
    try {
	interp.setResult(ReflectObject.newInstance(interp,
					      Class.class, result));
    } catch (Exception e) {
	throw new ReflectException(interp, e);
    }
}

} // end JavaNewCmd

