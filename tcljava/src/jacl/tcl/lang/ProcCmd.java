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
 * RCS: @(#) $Id: ProcCmd.java,v 1.2 1999/08/03 03:04:19 mo Exp $
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
	String fullName, procName;
	NamespaceCmd.Namespace ns, altNs, cxtNs;
	Command cmd;
	StringBuffer ds;

	if (objv.length != 4) {
	    throw new TclNumArgsException(interp, 1, objv, 
		    "name args body");
	}

	// Determine the namespace where the procedure should reside. Unless
	// the command name includes namespace qualifiers, this will be the
	// current namespace.
    
	fullName = objv[1].toString();

	// Java does not support passing an address so we pass
	// an array of size 1 and then assign arr[0] to the value
	NamespaceCmd.Namespace[] nsArr    = new NamespaceCmd.Namespace[1];
	NamespaceCmd.Namespace[] altNsArr = new NamespaceCmd.Namespace[1];
	NamespaceCmd.Namespace[] cxtNsArr = new NamespaceCmd.Namespace[1];
	String[]     procNameArr          = new String[1];

	NamespaceCmd.getNamespaceForQualName(interp, fullName, null,
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
	if ((ns != NamespaceCmd.getGlobalNamespace(interp))
	    && (procName != null)
	    && ((procName.length() > 0) && (procName.charAt(0) == ':'))) {
	    throw new TclException(interp, "can't create procedure \"" + procName +
				           "\" in non-global namespace with name starting with \":\"");
	}
	
	//  Create the data structure to represent the procedure.

	proc = new Procedure(interp, ns, procName,
		objv[2], objv[3], interp.getScriptFile(),
		interp.getArgLineNumber(3));

	// Now create a command for the procedure. This will initially be in
	// the current namespace unless the procedure's name included namespace
	// qualifiers. To create the new command in the right namespace, we
	// generate a fully qualified name for it.

	ds = new StringBuffer();
	if (ns != NamespaceCmd.getGlobalNamespace(interp)) {
	    ds.append(ns.fullName);
	    ds.append("::");
	}
	ds.append(procName);

	interp.createCommand(ds.toString(), proc);

	// Now initialize the new procedure's cmdPtr field. This will be used
	// later when the procedure is called to determine what namespace the
	// procedure will run in. This will be different than the current
	// namespace if the proc was renamed into a different namespace.
	
	// FIXME : we do not handle renaming into another namespace correctly yet!
	//procPtr->cmdPtr = (Command *) cmd;

	return;
    }
}
