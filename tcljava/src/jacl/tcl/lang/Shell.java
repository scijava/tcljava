/*
 * Shell.java --
 *
 *	Implements the start up shell for Tcl.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Shell.java,v 1.1 1998/10/14 21:09:18 cvsadmin Exp $
 */

package tcl.lang;

import java.util.*;
import java.io.*;

/*
 * The Shell class is similar to the Tclsh program: you can use it to
 * execute a Tcl script or enter Tcl command interactively at the
 * command prompt.
 */

public class Shell {

/*
 *----------------------------------------------------------------------
 *
 * main --
 *
 *	Main program for tclsh and most other Tcl-based applications.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	This procedure initializes the Tcl world and then starts
 *	interpreting commands; almost anything could happen, depending
 *	on the script being interpreted.
 *
 *----------------------------------------------------------------------
 */

public static void 
main(
    String args[]) 	// Array of command-line argument strings.
{
    String fileName = null;

    /*
     * Create the interpreter. This will also create the built-in
     * Tcl commands.
     */

    Interp interp = new Interp();

    /*
     * Make command-line arguments available in the Tcl variables "argc"
     * and "argv".  If the first argument doesn't start with a "-" then
     * strip it off and use it as the name of a script file to process.
     * We also set the argv0 and tcl_interactive vars here.
     */

    if ((args.length > 0) && !(args[0].startsWith("-"))) {
	fileName = args[0];
    }

    TclObject argv = TclList.newInstance();
    argv.preserve();
    try {
	int i = 0;
	int argc = args.length;
	if (fileName == null) {
	    interp.setVar("argv0", "tcl.lang.Shell", TCL.GLOBAL_ONLY);
	    interp.setVar("tcl_interactive", "1", TCL.GLOBAL_ONLY);
	} else {
	    interp.setVar("argv0", fileName, TCL.GLOBAL_ONLY);
	    interp.setVar("tcl_interactive", "0", TCL.GLOBAL_ONLY);
	    i++;
	    argc--;
	}
	for (; i < args.length; i++) {
	    TclList.append(interp, argv, 
		    TclString.newInstance(args[i]));
	}
	interp.setVar("argv", argv, TCL.GLOBAL_ONLY);
	interp.setVar("argc", java.lang.Integer.toString(argc),
		TCL.GLOBAL_ONLY);
    } catch (TclException e) {
	throw new TclRuntimeError("unexpected TclException: " + e);
    } finally {
	argv.release();
    }

    /*
     * Normally we would do application specific initialization here.
     * However, that feature is not currently supported.
     */

    /*
     * If a script file was specified then just source that file
     * and quit.
     */

    if (fileName != null) {
	try {
	    interp.evalFile(fileName);
	} catch (TclException e) {
	    int code = e.getCompletionCode();
	    if (code == TCL.RETURN) {
		code = interp.updateReturnInfo();
		if (code != TCL.OK) {
		    System.err.println("command returned bad code: " + code);
		}
	    } else if (code == TCL.ERROR) {
		System.err.println(interp.getResult().toString());
	    } else {
		System.err.println("command returned bad code: " + code);
	    }
	}

	/*
	 * Note that if the above interp.evalFile() returns the main
	 * thread will exit.  This may bring down the VM and stop
	 * the execution of Tcl.
	 *
	 * If the script needs to handle events, it must call
	 * vwait or do something similar.
	 *
	 * Note that the script can create AWT widgets. This will
	 * start an AWT event handling thread and keep the VM up. However,
	 * the interpreter thread (the same as the main thread) would
	 * have exited and no Tcl scripts can be executed.
	 */
    }

    if (fileName == null) {
	/*
	 * We are running in interactive mode. Start the ConsoleThread
	 * that loops, grabbing stdin and passing it to the interp.
	 */

	ConsoleThread consoleThread = new ConsoleThread(interp);
	consoleThread.setDaemon(true);
	consoleThread.start();

	/*
	 * Loop forever to handle user input events in the command line.
	 */

	Notifier notifier = interp.getNotifier();
	while (true) {
	    /*
	     * process events forevet until "exit" is called.
	     */

	    notifier.doOneEvent(TCL.ALL_EVENTS);
	}
    } else {
	System.exit(0);
    }
}
} // end Shell


/*
 *----------------------------------------------------------------------
 *
 * ConsoleThread --
 *
 * This class implements the Console Thread: it is started by
 * tcl.lang.Shell if the user gives no initial script to evaluate, or
 * when the -console option is specified. The console thread loops
 * forever, reading from the standard input, executing the user input
 * and writing the result to the standard output.
 *
 *----------------------------------------------------------------------
 */

class ConsoleThread extends Thread {

/*
 * Interpreter associated with this console thread.
 */

Interp interp;

/*
 * Temporarily holds refcount on the results of the interactive
 * commands so that an object command returned the java::* calls
 * can be used even when they are not saved in variables.
 */

Vector historyObjs;

/*
 * Collect the user input in this buffer until it forms a complete Tcl
 * command.
 */

StringBuffer sbuf;

/*
 * Used to for interactive input/output
 */

private Channel in;
private Channel out;
private Channel err;

/*
 *----------------------------------------------------------------------
 *
 * ConsoleThread --
 *
 *	Create a ConsoleThread.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Member fields are initialized.
 *
 *----------------------------------------------------------------------
 */

ConsoleThread(
    Interp i)			// Initial value for interp.
{
    interp = i;
    sbuf = new StringBuffer();
    historyObjs = new Vector();

    out = new StdChannel("stdout");
    err = new StdChannel("stderr");
}

/*
 *----------------------------------------------------------------------
 *
 * run --
 *
 *	Called by the JVM to start the execution of the console thread.
 *	It loops forever to handle user inputs.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	This method never returns. During its execution, some
 *	TclObjects may be locked inside the historyObjs vector.
 *	Remember to free them at "appropriate" times!
 *
 *----------------------------------------------------------------------
 */

public synchronized void
run()
{
    put(out, "% ");

    while (true) {
	/*
	 * Loop forever to collect user inputs in a StringBuffer.
	 * When we have a complete command, then execute it and print
	 * out the results.
	 *
	 * The loop is broken under two conditions: (1) when EOF is
	 * received inside getLine(). (2) when the "exit" command is
	 * executed in the script.
	 */

	TclObject prompt;

        sbuf.append(getLine());

	/*
	 * We have a complete command now. Execute it.
	 */

	if (Interp.commandComplete(sbuf.toString())) {
	    ConsoleEvent evt = new ConsoleEvent(interp, sbuf.toString());
	    interp.getNotifier().queueEvent(evt, TCL.QUEUE_TAIL);
	    evt.sync();

	    if (evt.evalResult != null) {
		String s = evt.evalResult.toString();
		if (s.length() > 0) {
		    putLine(out, s);
		}

		/*
		 * The "history limit" controls how many TclObject's
		 * we record from the results of the Console inputs.
		 */

		int limit = 0;
//		int limit = 10;

		try {
		    TclObject histLimit = interp.getVar("historyLimit",
			    TCL.GLOBAL_ONLY | TCL.DONT_THROW_EXCEPTION);
		    if (histLimit != null) {
			limit = TclInteger.get(interp, histLimit);
		    }
		} catch (TclException e) {
		    /*
		     * histLimit doesn't exist or contains an invalid integer.
		     * Ignore it.
		     */
		}

		/*
		 * This cleans up all intermediate objects that
		 * are no longer referenced in Tcl. This check is
		 * needed to clean up object references in the
		 * string->object translation table.
		 */

		if (limit > 0) {
		    historyObjs.addElement(evt.evalResult);
		} else {
		    evt.evalResult.release();
		}

		if (historyObjs.size() > limit && historyObjs.size() > 0) {
		    TclObject cobj =(TclObject)historyObjs.elementAt(0);
		    historyObjs.removeElementAt(0);
		    cobj.release();
		}
	    } else {
		TclException e = evt.evalException;
		int code = e.getCompletionCode();

		check_code: {
		    if (code == TCL.RETURN) {
			code = interp.updateReturnInfo();
			if (code == TCL.OK) {
			    break check_code;
			}
		    }

		    switch (code) {
		    case TCL.ERROR:
			putLine(err, interp.getResult().toString());
			break;
		    case TCL.BREAK:
			putLine(err, "invoked \"break\" outside of a loop");
			break;
		    case TCL.CONTINUE:
			putLine(err, "invoked \"continue\" outside of a loop");
			break;
		    default:
			putLine(err, "command returned bad code: " + code);
		    }
		}
	    }

	    sbuf.setLength(0);
	    try {
		prompt = interp.getVar("tcl_prompt1", TCL.GLOBAL_ONLY);
	    } catch (TclException e) {
		prompt = null;
	    }
	    if (prompt != null) {
		try {
		    interp.eval(prompt.toString(), TCL.GLOBAL_ONLY);
		} catch (TclException e) {
		    put(out, "% ");
		}
	    } else {
		put(out, "% ");
	    }
	} else {

	    /*
	     * We don't have a complete command yet. Print out a level 2
	     * prompt message and wait for further inputs.
	     */

	    try {
		prompt = interp.getVar("tcl_prompt2", TCL.GLOBAL_ONLY);
	    } catch (TclException e) {
		prompt = null;
	    }
	    if (prompt != null) {
		try {
		    interp.eval(prompt.toString(), TCL.GLOBAL_ONLY);
		} catch (TclException e) {
		    put(out, "> ");
		}
	    } else {
		put(out, "> ");
	    }
	}
    }
}

/*
 *----------------------------------------------------------------------
 *
 * getLine --
 *
 *	Gets a new line from System.in.
 *
 * Result:
 *	The new line of user input, including the trailing carriage
 *	return character.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private String
getLine()
{
    StringBuffer sbuf = new StringBuffer();

    /*
     * On Unix platforms, System.in.read() will block the delivery of
     * of AWT events. We must make sure System.in.available() is larger
     * than zero before attempting to read from System.in. Since
     * there is no asynchronous IO in Java, we must poll the System.in
     * every 100 milliseconds.
     */

    boolean sysInBlocks = false;

    try {
	/*
	 * There is no way to tell whether System.in will block AWT
	 * threads, so we assume it does block if we can use
	 * System.in.available().
	 */

	System.in.available();
	sysInBlocks = true;
    } catch (Exception e) {
	/*
	 * System.in.available() causes an exception -- it's probably
	 * no supported on this platform (e.g. MS Java SDK). We assume
	 * sysInBlocks is false and let the user suffer ...
	 */

	sysInBlocks = false;
    }

    if (sysInBlocks) {
	try {
	    /*
	     * Wait until there are inputs from System.in. On Unix,
	     * this usually means the user has pressed the return key.
	     */

	    while (System.in.available() == 0) {
		Thread.currentThread().sleep(100);
	    }
	} catch (InterruptedException e) {
	    System.exit(0);
	} catch (EOFException e) {
	    System.exit(0);
	} catch (IOException e) {
	    e.printStackTrace();
	    System.exit(0);
	}
    }

    /* 
     * Loop until user presses return or EOF is reached.
     */
    char c2 = ' ';

    while (true) {
	char c = ' ';

	try {
	    int i = System.in.read();
	    if (i == -1) {
		if (sbuf.length() == 0) {
		    System.exit(0);
		} else {
		    return sbuf.toString();
		}
	    }
	    c = (char)i;

	    /*
	     * Temporary hack until Channel drivers are complete.  Convert
	     * the Windows \r\n to \n.
	     */

	    if (c == '\r') {
	        i = System.in.read();
	        if (i == -1) {
		    if (sbuf.length() == 0) {
		        System.exit(0);
		    } else {
		        return sbuf.toString();
		    }
		}
		c2 = (char)i; 
		if (c2 == '\n') {
		  c = c2;
		} else {
		  sbuf.append(c);
		  c = c2;
		}
	    }
	} catch (IOException e) {
	    /*
	     * IOException shouldn't happen when reading from
	     * System.in. The only exceptional state is the EOF event,
	     * which is indicated by a return value of -1.
	     */

	    e.printStackTrace();
	    System.exit(0);
	}

	sbuf.append(c);
	if (c == '\n') {
	    return sbuf.toString();
	}
    }
}

/*
 *----------------------------------------------------------------------
 *
 * putLine --
 *
 *	Prints a string into the given channel with a trailing carriage
 *	return.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private void
putLine(
    Channel chan, 		// The channel to print to.
    String s)			// The string to print.
{
    put(chan, s);
    put(chan, "\n");
}

/*
 *----------------------------------------------------------------------
 *
 * put --
 *
 *	Prints a string into the given channel without a trailing
 *	carriage return.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private
void put(
    Channel chan,  		// The channel to print to.
    String s)			// The string to print.
{
    try {
	out.write(interp, s);
    } catch (Exception  e) {
	throw new TclRuntimeError("unexpected Exception " + e);
    }
}
} // end ConsoleThread
