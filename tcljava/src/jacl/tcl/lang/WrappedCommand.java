/*
 * WrappedCommand.java
 *
 *	Wrapper for commands located inside a Jacl interp.
 *
 * Copyright (c) 1999 Mo DeJong.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: WrappedCommand.java,v 1.1 1999/08/03 03:20:24 mo Exp $
 */

package tcl.lang;


import java.util.*;

/**
 * A Wrapped Command is like the Command struct defined in the C version
 * in the file generic/tclInt.h. It is "wrapped" around a TclJava Command
 * interface reference. We need to wrap Command references so that we
 * can keep track of sticky issues like what namespace the command is
 * defined in without requiring that every implementation of a Command
 * interface provide method to do this. This class is only used in
 * the internal implementation of Jacl.
 */

class WrappedCommand {
    Hashtable table;  // Reference to the table that this command is
                      // defined inside. The hashKey member can be
                      // used to lookup this CommandWrapper instance
                      // in the table of CommandWrappers. The table
                      // member combined with the hashKey member are
                      // are equivilent to the C version's Command->hPtr.
    String hashKey;   // A string that stores the name of the command.
                      // This name is NOT fully qualified.


    NamespaceCmd.Namespace ns; // The namespace where the command is located

    Command cmd;      // The actual Command interface that we are wrapping.

    boolean deleted;  // Means that the command is in the process
                      // of being deleted. Other attempts to
                      // delete the command should be ignored.


    public String toString() {
	StringBuffer sb = new StringBuffer();

	sb.append("Wrapper for ");
	if (ns != null) {
	    sb.append(ns.fullName);
	    if (!ns.fullName.equals("::")) {
		sb.append("::");
	    }
	}
	if (table != null) {
	    sb.append(hashKey);
	}

	sb.append(" -> ");
	sb.append(cmd.getClass().getName());

	return sb.toString();
    }
}

