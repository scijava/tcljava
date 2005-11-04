/*
 * ProcCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ProcCmd.java,v 1.5 2005/11/04 21:02:14 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "proc" command in Tcl.
 */

class ProcCmd implements Command {
    /**
     *
     * Tcl_ProcObjCmd -> ProcCmd.cmdProc
     *
     * Creates a new Tcl procedure.
     *
     * @param interp the current interpreter.
     * @param objv command arguments.
     * @exception TclException If incorrect number of arguments.
     */

    public void cmdProc(Interp interp, TclObject[] objv)
	    throws TclException {
	Procedure proc;
	String fullName;
	Command cmd;
	FindCommandNamespaceResult result;

	if (objv.length != 4) {
	    throw new TclNumArgsException(interp, 1, objv, 
		    "name args body");
	}

	// Determine the namespace where the procedure should reside. Unless
	// the command name includes namespace qualifiers, this will be the
	// current namespace.

	fullName = objv[1].toString();
	result = FindCommandNamespace(interp, fullName);

	//  Create the data structure to represent the procedure.

	proc = new Procedure(interp, result.ns, result.cmdName,
		objv[2], objv[3], interp.getScriptFile(),
		interp.getArgLineNumber(3));

	// Now create a command for the procedure. This will be in
	// the current namespace unless the procedure's name included namespace
	// qualifiers. To create the new command in the right namespace, we
	// use fully qualified name for it.

	interp.createCommand(result.cmdFullName, proc);

	// Now initialize the new procedure's cmdPtr field. This will be used
	// later when the procedure is called to determine what namespace the
	// procedure will run in. This will be different than the current
	// namespace if the proc was renamed into a different namespace.

	WrappedCommand wcmd = Namespace.findCommand(interp,
	   result.cmdFullName, result.ns, TCL.NAMESPACE_ONLY);
	proc.wcmd = wcmd;

	return;
    }

    // Helper class used to return namespace lookup results for
    // a command name.

    static class FindCommandNamespaceResult {
        String fullName;                // Initial name of command: like "foo" or "::one::two::foo"
        String cmdName;                 // Result short name of command: like "foo"
        String cmdFullName;             // Result full name of command: like "::one::two::foo"
        Namespace ns;      // Result namespace the command lives in
    }

    // Helper used to lookup the namespace and associated info for a command.
    // Use when defining a new command.

    static FindCommandNamespaceResult FindCommandNamespace(Interp interp, String fullName)
	    throws TclException {
	String procName;
	Namespace ns, altNs, cxtNs;
	StringBuffer ds;

	// Java does not support passing an address so we pass
	// an array of size 1 and then assign arr[0] to the value
	Namespace[] nsArr    = new Namespace[1];
	Namespace[] altNsArr = new Namespace[1];
	Namespace[] cxtNsArr = new Namespace[1];
	String[]     procNameArr          = new String[1];

	Namespace.getNamespaceForQualName(interp, fullName, null,
				   0, nsArr, altNsArr, cxtNsArr, procNameArr);

	// Get the values out of the arrays
	ns       = nsArr[0];
	altNs    = altNsArr[0];
	cxtNs    = cxtNsArr[0]; 
	procName = procNameArr[0];

	if (ns == null) {
	    throw new TclException(interp, "can't create procedure \"" + fullName +
				           "\": unknown namespace");
	}
	if (procName == null) {
	    throw new TclException(interp, "can't create procedure \"" + fullName +
				           "\": bad procedure name");
	}
	// FIXME : could there be a problem with a command named ":command" ?
	if ((ns != Namespace.getGlobalNamespace(interp))
	    && (procName != null)
	    && ((procName.length() > 0) && (procName.charAt(0) == ':'))) {
	    throw new TclException(interp, "can't create procedure \"" + procName +
				           "\" in non-global namespace with name starting with \":\"");
	}

	// Return the command info, if the command is defined in a namespace
	// other than the global namespace then cmdFullName will be the
	// fully qualified name for the command.

	ds = new StringBuffer();
	if (ns != Namespace.getGlobalNamespace(interp)) {
	    ds.append(ns.fullName);
	    ds.append("::");
	}
	ds.append(procName);

	FindCommandNamespaceResult result = new FindCommandNamespaceResult();
	result.fullName = fullName;
	result.cmdName = procName;
	result.cmdFullName = ds.toString();
	result.ns = ns;

	return result;
    }
}
