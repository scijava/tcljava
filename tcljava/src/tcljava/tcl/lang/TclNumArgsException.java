/*
 * TclNumArgsException.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclNumArgsException.java,v 1.1 1998/10/14 21:09:14 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This exception is used to report wrong number of arguments in Tcl scripts.
 */

public class TclNumArgsException extends TclException {

    /**
     * Creates a TclException with the appropiate Tcl error
     * message for having the wring number of arguments to a Tcl command.
     * <p>
     * Example: <pre>
     *     
     *  if (argv.length != 3) {
     *     throw new TclNumArgsException(interp, 1, argv, "option name");
     *  }
     * </pre>
     *
     * @param interp current Interpreter.
     * @param argc the number of arguments to copy from the offending
     *     command to put into the error message.
     * @param argv the arguments of the offending command.
     * @param message extra message to appear in the error message that
     *     explains the proper usage of the command.
     * @exception TclException is always thrown.
     */

     public TclNumArgsException(Interp interp, int argc, 
	     TclObject argv[], String message)
	     throws TclException
    {
	super(TCL.ERROR);

	if (interp != null) {
	    InternalRep rep;
	    String s = "wrong # args: should be \"";

	    for (int i = 0; i < argc; i++) {
		if (argv[i].getInternalRep() instanceof TclIndex) {
		    s = s.concat(argv[i].getInternalRep().toString());
		} else {
		    s = s.concat(argv[i].toString());
		}
		if (i < (argc - 1)) {
		    s = s.concat(" ");
		}
	    }
	    if ((message != null) && (message.length() != 0)) {
		s = s.concat(" " + message);
	    }
	    s = s.concat("\"");
	    interp.setResult(s);
	}
    }
}

