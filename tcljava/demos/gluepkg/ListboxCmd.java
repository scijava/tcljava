/*
 * ListboxCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ListboxCmd.java,v 1.2 1999/05/08 23:20:30 dejong Exp $
 *
 */

import tcl.lang.*;

/**
 * This class implements the "listbox" demo command.
 *
 * The role of the GUI is to take a list of TclObjects and add them to an
 * AWT List component.  Two Buttons are added; OK and Cancel.  When either
 * of the buttons are pressed, the class variable 'items' is set to the 
 * selected item(s) or null if no items are selected or Cancel was pressed.
 */

public class ListboxCmd implements Command {
  
    /**
     * cmdProc --
     *
     * This procedure is invoked to process the listbox command.
     * If no arguments are supplied then the listbox directory begins
     * in the current directory, otherwise it begins in the supplied dir.
     * If dir and file are valid the result is set to the valid absolute path.
     */

    public void cmdProc(Interp interp, TclObject argv[])
            throws TclException {

	TclObject  result = null;   // The Tcl result if this command
        ListboxApp listbox;         // The List widget
	String     dirName = null;  // The dir to start with, if any
	String[]   args;            // Args passed to Listbox constructor
	String[]   items;           // List of selected list items

	if ((argv.length < 2)) {
	    throw new TclNumArgsException(interp, 1, argv, "arg ?arg...?");
	}

	args = new String[argv.length - 1];
	for (int i = 1; i < argv.length; i++) {
	    args[i-1] = argv[i].toString();
	}

	listbox = new ListboxApp(args);

	// Get the list of selected items.  items is null 
	// if no item selected

	items = listbox.getSelectedItems();

	// If there were selected items returned, append 
	// each one onto the list
	
	if (items != null) {
	    result = TclList.newInstance();
	
	    for (int i = 0; i < items.length; i++) {
	        TclList.append(interp, result, 
                        TclString.newInstance(items[i]));
	    }
	}
	if (result != null) {
	    interp.setResult(result);
	}
    }
}
