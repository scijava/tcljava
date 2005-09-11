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
 * RCS: @(#) $Id: WrappedCommand.java,v 1.3 2005/09/11 20:56:57 mdejong Exp $
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

public
class WrappedCommand {
    public
    Hashtable table;  // Reference to the table that this command is
                      // defined inside. The hashKey member can be
                      // used to lookup this WrappedCommand instance
                      // in the table of WrappedCommands. The table
                      // member combined with the hashKey member are
                      // are equivilent to the C version's Command->hPtr.
    public
    String hashKey;   // A string that stores the name of the command.
                      // This name is NOT fully qualified.

    public
    NamespaceCmd.Namespace ns; // The namespace where the command is located

    public
    Command cmd;      // The actual Command interface that we are wrapping.

    public
    boolean deleted;  // Means that the command is in the process
                      // of being deleted. Other attempts to
                      // delete the command should be ignored.

    ImportRef importRef;  // List of each imported Command created in
                          // another namespace when this command is
			  // imported. These imported commands
			  // redirect invocations back to this
			  // command. The list is used to remove all
			  // those imported commands when deleting
			  // this "real" command.


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
