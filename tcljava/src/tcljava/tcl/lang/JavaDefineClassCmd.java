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
 * RCS: @(#) $Id: JavaDefineClassCmd.java,v 1.3 2003/03/19 02:35:57 mdejong Exp $
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

    // FIXME: It would be better if the TclByteArray class
    // was available in both Tcl Blend and Jacl so that we
    // could query bytes directly instead of converting to
    // a string and then converting back to bytes.
    String str = argv[1].toString();
    final int str_length = str.length();
    classData = new byte[str_length];
    for (int i=0; i < str_length; i++) {
        classData[i] = (byte) str.charAt(i);
    }

    tclClassLoader = new TclClassLoader(interp, null);
    result = tclClassLoader.defineClass(null, classData);

    interp.setResult(ReflectObject.newInstance(interp,
            Class.class, result));
}

} // end JavaNewCmd

