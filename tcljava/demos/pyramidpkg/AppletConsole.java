/*
 * AppletConsole.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 *   This file implements the Appletconsole class.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: AppletConsole.java,v 1.1 1998/10/14 21:09:23 cvsadmin Exp $
 */

import java.awt.*;
import java.awt.event.*;
import tcl.lang.*;

/*
 * The AppletConsole object is essentially a thread containing a text box.
 * This thread allows the user to alternate between interacting with other 
 * features of the applet and the embedded tcl console.
 */

public class AppletConsole extends Thread {
    Interp interp;
    StringBuffer sbuf = new StringBuffer();
    TextArea text;

    public AppletConsole(Interp i, int rows, int columns) {
	StringBuffer sbuf = new StringBuffer();
	interp = i;
	
	/*
	 * Create a text box and capture the key and mouse events via
	 * the ConsoleKeyListener and ConsoleMouseListener interfaces.
	 */

	text = new TextArea(rows, columns);
	text.addKeyListener(new ConsoleKeyListener(this));
	text.addMouseListener(new ConsoleMouseListener());

	/*
	 * The console thread runs as a daemon so that it gets terminated 
	 * automatically when all other user threads are terminated.
	 */

	setDaemon(true);
    }

    public TextArea GetText() {
	return text;
    }

    public void PutLine(String s) {
	text.insert(s + "\n", 100000);
    }

    public void Put(String s) {
	text.insert(s, 100000);
    }

    /*
     * The AppletConsole thread loops waiting for notification from the
     * ConsoleKeyListener object, via the LineReadyNotify method.
     */

    public synchronized void run() {
	PutLine("\n  This widget is a tcl console with two added commands,");
	PutLine("  \"build\" and \"remove\", by which you can manipulate the");
	PutLine("  pyramid below.");
	PutLine("\n  To use the console, click your mouse to the right of the");
	PutLine("  prompt.");
	Put("\n% ");
	while (true) {
	    try {
		wait();
		ProcessLine();
	    }
	    catch (InterruptedException e) {
		System.out.println("AppletConsole: wait error");		
	    }
	}
    }

    /*
     * The ConsoleKeyListener object tells the console thread that a 
     * line of input is available and run() can proceed.
     */

    public synchronized void LineReadyNotify(String s) {
        sbuf.append(s);
	notify();
    }

    /*
     * If sbuf contains a complete command, evaluate it and display the
     * result in the text box.  Otherwise, display the secondary prompt.
     */

    private void ProcessLine() {
	String s = sbuf.toString();
	int compCode;

	if (interp.commandComplete(s)) {

	    try {
		interp.eval(s);
		String result = interp.getResult().toString();
		if (result.length() > 0) {
		    PutLine(result);
		}
	    } catch (TclException e) {
		compCode = e.getCompletionCode();
		if (compCode == TCL.ERROR) {
		    PutLine(interp.getResult().toString());
		} else {
		    PutLine("command returned bad code: " 
                                 + compCode);
		}
	    }
	    sbuf.setLength(0);
	    Put("% ");
	} else {
	    Put("> ");
	}
    }
}

