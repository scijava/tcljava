/*
 * RenameCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: RenameCmd.java,v 1.1 1998/10/14 21:09:20 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "rename" command in Tcl.
 */

class RenameCmd implements Command {
    /**
     * See Tcl user documentation for details.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv.length != 3) {
	    throw new TclNumArgsException(interp, 1, argv, "oldName newName");
	}

	boolean delete;
	String oldName = argv[1].toString();
	String newName = argv[2].toString();

	if (newName.length() == 0) {
	    delete = true;
	} else {
	    delete = false;
	}

	Command cmd = (Command)interp.cmdTable.get(oldName);
	if (cmd == null) {
	    if (delete) {
		throw new TclException(interp, "can't delete \"" + oldName +
		        "\": command doesn't exist");
	    } else {
		throw new TclException(interp, "can't rename \"" + oldName +
		        "\": command doesn't exist");
	    }
	}

	if (interp.cmdTable.get(newName) != null) {
	    throw new TclException(interp, "can't rename to \"" + newName +
		    "\": command already exists");
	}


	if (delete) {
	    interp.deleteCommand(oldName);
	} else {
	    interp.cmdTable.remove(oldName);
	    interp.cmdTable.put(newName, cmd);
	}
    }
}

