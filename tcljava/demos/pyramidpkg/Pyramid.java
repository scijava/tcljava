/*
 * Pyramid.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 *   This file implements the Pyramid class.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Pyramid.java,v 1.2 1999/05/08 23:25:23 dejong Exp $
 */

import tcl.lang.*;
import java.awt.*;
import java.awt.event.*;
import java.applet.*;

/**
 * This class implements an applet with three components:
 * 1) a drawing of a partially built pyramid,
 * 2) buttons which allow the user to complete or erase the drawing, and
 * 3) a console which runs a Jacl interpreter.
 */

public class Pyramid extends Applet {

    public static void main(String args[]) {
    	new Pyramid();
    }

    Button buildButton;
    Button removeButton;
    Interp interp;
    AppletConsole console;

    public int numBlocks;
    private int maxBlocks;

    // Initialize the applet.

    public void init() {
	numBlocks = 4;
	maxBlocks = 6;

	// Create buttons to manipulate the pyramid.

	buildButton = new Button(" Build ");
	removeButton = new Button(" remove ");
	add(buildButton);
	add(removeButton);

	// Create an interpreter.  Add two new commands to the
	// interpreter:  build and remove.

	interp = new Interp();
	interp.createCommand("build", new BuildCmd(this));
	interp.createCommand("remove", new RemoveCmd(this));

	// Create an AppletConsole and add it to the applet.

	console = new AppletConsole(interp, 15, 50);
	add(console.GetText());
	console.start();

	buildButton.addActionListener(new BuildButtonListener(this));
	removeButton.addActionListener(new RemoveButtonListener(interp, console));
    }

    // Update the pyramid as shown in the applet.

    public void paint(Graphics g) {

	int w       = 70;        // width of each block
	int dRow    = w + 30;    // dist btw base of two adjacent rows
	int h       = 35;        // height of each block
	int dColumn = h + 30;    // dist btw left side of two adjacent cols
	int dTop    = 300;       // dist from top of window

	int dLeft   = dRow / 2;  // dist from left side of window
	int tempBlocks = 0;

 	for (int column = 3; column >= 1; column--) { 
 	    for (int row = 1; row <= column; row++) {
		if (tempBlocks >= numBlocks) {
		    g.drawLine((row * dRow) + ((3 - column) * dLeft),  
			    (column * dColumn) + dTop + h, 
			    (row * dRow) + ((3 - column) * dLeft) + w, 
			    (column * dColumn) + dTop + h); 
		} else {
		    g.fillRect((row * dRow) + ((3 - column) * dLeft), 
			    (column * dColumn) + dTop, w, h);
		}
		++tempBlocks;
		g.drawString("" + tempBlocks, 
			(row * dRow) + ((3 - column) * dLeft) + h, 
			(column * dColumn) + dTop + dLeft);
	    }
	}
    }

    // Build 1 block in the pyramid.

    public boolean buildBlock() {
	if (numBlocks >= maxBlocks) {
	    return false;
	}
	++numBlocks;
	repaint();
	return true;
    }

    // Remove 1 block from the pyramid.

    public boolean removeBlock() {
	if (numBlocks == 0) {
	    return false;
	}
	--numBlocks;
	repaint();
	return true;
    }
}

/**
 * This class implements the "build" button in the Pyramid applet.
 *
 * Pressing the "build" button simply results in the invocation of
 * pyramid.buildBlock.  The embedded Tcl interpreter is not involved.
 */

class BuildButtonListener implements ActionListener {
    Pyramid pyramid;

    BuildButtonListener(Pyramid p) {
	pyramid = p;
    }
    
    public void actionPerformed(ActionEvent event) {
	pyramid.buildBlock();
    }
}

/**
 * This class implements the "remove" button in the Pyramid applet.
 *
 * Pressing the "remove" button results in the evaluation of the 
 * "remove" command in the embedded Tcl interpreter.
 */

class RemoveButtonListener implements ActionListener {
    Interp interp;
    AppletConsole console;

    RemoveButtonListener(Interp i, AppletConsole c) {
	interp = i;
	console = c;
    }
    
    public void actionPerformed(ActionEvent event) {
	String s = "remove\n";
	int compCode;

	try {
	    interp.eval(s);
	    String result = interp.getResult().toString();
	    if (result.length() > 0) {
		console.PutLine(result);
	    }
	} catch (TclException e) {
	    compCode = e.getCompletionCode();
	    if (compCode == TCL.ERROR) {
		console.PutLine(interp.getResult().toString());
	    } else {
		console.PutLine("command returned bad code: " + compCode);
	    }
	}
    }
}

/**
 * This class implements the "build" command in PyramidPackage.
 */

class BuildCmd extends Applet implements Command {
    
    Pyramid pyr;

    BuildCmd(Pyramid p) {
	pyr = p;
    }

    /**
     * This procedure is invoked to process the "build" Tcl command.
     * We simply call the pyramid's buildBlock method.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {

	if (argv.length != 1) {
	    throw new TclNumArgsException(interp, 1, argv, "");
	}
	if (!pyr.buildBlock()) {
	    throw new TclException(interp, 
		    "error in build:  pyramid is already full");
	}
	return;
    }
}

/**
 * This class implements the "remove" command in PyramidPackage.
 */

class RemoveCmd extends Applet implements Command {
    
    Pyramid pyr;

    RemoveCmd(Pyramid p) {
	pyr = p;
    }

    // This procedure is invoked to process the "remove" Tcl command.
    // We simply call the pyramid's removeBlock method.

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {

	if (argv.length != 1) {
	    throw new TclNumArgsException(interp, 1, argv, "");
	}
	if (!pyr.removeBlock()) {
	    throw new TclException(interp, 
		    "error in remove: pyramid is already empty");
	}
	return;
    }
}

