/*
 * ThrowCmd.java --
 *
 *	Implements the java::throw command.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: JavaThrowCmd.java,v 1.1 1998/10/14 21:09:13 cvsadmin Exp $
 */

package tcl.lang;

import java.lang.reflect.*;
import java.beans.*;

/*
 * This class implements the built-in "java::throw" command in Tcl.
 */

class JavaThrowCmd implements Command {


/*
 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked as part of the Command interface to
 *	process the "java::throw" Tcl command. It constructs a Throwable
 *	object and throws it by encapsulating the Throwable inside
 *	a ReflectException.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	If the Throwable object is successfully constructed, throws a
 *	ReflectException which encapusulates the Throwable object.
 *
 *----------------------------------------------------------------------
 */

public void
cmdProc(
    Interp interp,		// Current interpreter.
    TclObject argv[])		// Argument list.
throws 
    TclException 		// Standard Tcl exception.
{
    if (argv.length != 2) {
	throw new TclNumArgsException(interp, 1, argv, "throwableObj");
    }

    Object javaObj = null;
    javaObj = ReflectObject.get(interp, argv[1]);

    if (!(javaObj instanceof Throwable)) {
	throw new TclException(interp,
		"bad object: must be an instance of Throwable");
    } else {
	throw new ReflectException(interp, (Throwable)javaObj);
    }
}

} // end JavaThrowCmd

