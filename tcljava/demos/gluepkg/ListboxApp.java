/*
 * ListboxCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ListboxApp.java,v 1.1 1998/10/14 21:09:23 cvsadmin Exp $
 *
 */

import java.awt.*;
import java.awt.event.*;

/*
 * A simple application that displays a listbox, with one or more 
 * items, and two buttons, 'OK' and 'Cancel'.  Once the constructor
 * is called, it runs the "application" and polls until an actionEvent
 * occurs (i.e. OK or Cancel is pressed..  At this point, items is 
 * set with a list of selected list items and can be retieved by 
 * calling 'getSelectedItems()'.
 */

public class ListboxApp implements ActionListener {

    /*
     * Used to implement a wait mechanism
     */

    boolean wait = true;

    /*
     * The list of selected items from the AWT List widget.
     */

    private String[] items;

    /*
     * The Java AWT Widgets (ummm... I mean Components.) 
     */

    private List     lst;
    private Frame    frm;
    private Button   but;

    /*
     * ListboxApp --
     *
     * Create the application and insert listItems into the AWT List.
     */

    public ListboxApp(String[] listItems) {

        GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();

	frm = new Frame();
	frm.setLayout(gbl);

	gbc.gridwidth = GridBagConstraints.REMAINDER;
	gbc.gridx = 0;
	gbc.gridy = 0;
		
	lst = new List(6, true);
	for (int i = 0; i < listItems.length; i++) {
	    lst.add(listItems[i]);
	}
	gbl.setConstraints(lst, gbc);
	frm.add(lst);

	gbc.gridwidth = 1;
	gbc.gridx = 0;
	gbc.gridy = 1;

	but = new Button("OK");
	but.addActionListener(this);
	gbl.setConstraints(but, gbc);
	frm.add(but);

	gbc.gridx = 1;
	gbc.gridy = 1;

	but = new Button("Cancel");
	but.addActionListener(this);
	gbl.setConstraints(but, gbc);
	frm.add(but);

	frm.setSize(170,170);
	frm.show();
	
	/*
	 * Since the current version of Jacl dosent have event bindins
	 * or vwait, we must poll until an action event occurs.  Otherwise
	 * there is now clean way to grab the selected list items.
	 */

	try {
	    while (wait) {
	        Thread.currentThread().sleep(100); 
	    }
	} catch (InterruptedException e) {
	    /*
	     * Do nothing...
	     */ 
	}
    }

    /*
     * getSelectedItems --
     *
     * Gets the selected list items.
     */

    public String[] getSelectedItems() {
        return(items);
    }

   /*
    * actionPerformed --
    *
    * This is the callback when either 'OK' or 'Cancel' are pressed.
    * In either case the window is removed, items is set, and the 
    * wait variable is set to true, thus breaking the while loop in
    * the constructor. If 'OK' has been pressed, the class variable
    * 'items' equals the list of selected items, else null.  
    */

    public void actionPerformed(ActionEvent evt) {
	
	if ("Cancel".equals(evt.getActionCommand())) {
	    items = null;
	} else if ("OK".equals(evt.getActionCommand())) {
	    items = lst.getSelectedItems();
	}
	frm.dispose();
	wait = false;
    }
}


