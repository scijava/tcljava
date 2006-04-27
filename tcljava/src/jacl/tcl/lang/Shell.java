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
 * RCS: @(#) $Id: Shell.java,v 1.16 2006/04/27 02:16:13 mdejong Exp $
 */

package tcl.lang;

import java.util.*;
import java.io.*;

/**
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

    // Create the interpreter. This will also create the built-in
    // Tcl commands.

    Interp interp = new Interp();

    // Make command-line arguments available in the Tcl variables "argc"
    // and "argv".  If the first argument doesn't start with a "-" then
    // strip it off and use it as the name of a script file to process.
    // We also set the argv0 and tcl_interactive vars here.

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

    // Normally we would do application specific initialization here.
    // However, that feature is not currently supported.

    // If a script file was specified then just source that file
    // and quit.

    if (fileName != null) {
	int exitCode = 0;
	try {
	    interp.evalFile(fileName);
	} catch (TclException e) {
	    int code = e.getCompletionCode();
	    if (code == TCL.RETURN) {
		code = interp.updateReturnInfo();
		if (code != TCL.OK) {
		    System.err.println("command returned bad code: " + code);
		    exitCode = 2;
		}
	    } else if (code == TCL.ERROR) {
		System.err.println(interp.getResult().toString());
		exitCode = 1;
	    } else {
		System.err.println("command returned bad code: " + code);
		exitCode = 2;
	    }
	}

	// Note that if the above interp.evalFile() returns the main
	// thread will exit.  This may bring down the VM and stop
	// the execution of Tcl.
	//
	// If the script needs to handle events, it must call
	// vwait or do something similar.
	//
	// Note that the script can create AWT widgets. This will
	// start an AWT event handling thread and keep the VM up. However,
	// the interpreter thread (the same as the main thread) would
	// have exited and no Tcl scripts can be executed.

        interp.dispose();
        System.exit(exitCode);
    }

    if (fileName == null) {
	// We are running in interactive mode. Start the ConsoleThread
	// that loops, grabbing stdin and passing it to the interp.

	ConsoleThread consoleThread = new ConsoleThread(interp);
	consoleThread.setDaemon(true);
	consoleThread.start();

	// Loop forever to handle user input events in the command line.
	// This method will loop until "exit" is called or the interp
	// is interrupted.

	Notifier notifier = interp.getNotifier();

        try {
            Notifier.processTclEvents(notifier);
        } finally {
            interp.dispose();
        }
    }
}
} // end class Shell


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

// Interpreter associated with this console thread.

Interp interp;

// Collect the user input in this buffer until it forms a complete Tcl
// command.

StringBuffer sbuf;

// Used to for interactive input/output

private Channel out;
private Channel err;

// set to true to get extra debug output
private static final boolean debug = false;

// used to keep track of wether or not System.in.available() works
private static boolean sysInAvailableWorks = false;

static {
    try {
	// There is no way to tell whether System.in will block AWT
	// threads, so we assume it does block if we can use
	// System.in.available().

	System.in.available();
	sysInAvailableWorks = true;
    } catch (Exception e) {
	// If System.in.available() causes an exception -- it's probably
	// no supported on this platform (e.g. MS Java SDK). We assume
	// sysInAvailableWorks is false and let the user suffer ...
    }

    // Sun's JDK 1.2 on Windows systems is screwed up, it does not
    // echo chars to the console unless blocking IO is used.
    // For this reason we need to use blocking IO under Windows.

    if (Util.isWindows()) {
	sysInAvailableWorks = false;
    }

    if (debug) {
        System.out.println("sysInAvailableWorks = " + sysInAvailableWorks);
    }

}


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
    setName("ConsoleThread");
    interp = i;
    sbuf = new StringBuffer(100);

    out = TclIO.getStdChannel(StdChannel.STDOUT);
    err = TclIO.getStdChannel(StdChannel.STDERR);
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

public synchronized void run() {
    if (debug) {
        System.out.println("entered ConsoleThread run() method");
    }


    put(out, "% ");

    while (true) {
	// Loop forever to collect user inputs in a StringBuffer.
	// When we have a complete command, then execute it and print
	// out the results.
	//
	// The loop is broken under two conditions: (1) when EOF is
	// received inside getLine(). (2) when the "exit" command is
	// executed in the script.

        getLine();

	final String command = sbuf.toString();

        if (debug) {
            System.out.println("got line from console");
            System.out.println("\"" + command + "\"");
        }

	// When interacting with the interpreter, one must
	// be careful to never call a Tcl method from
	// outside of the event loop thread. If we did
	// something like just call interp.eval() it
	// could crash the whole process because two
	// threads might write over each other.

	// The only safe way to interact with Tcl is
	// to create an event and add it to the thread
	// safe event queue.

	TclEvent event = new TclEvent() {
	    public int processEvent (int flags) {
		
		// See if the command is a complete Tcl command

		if (Interp.commandComplete(command)) {
		    if (debug) {
			System.out.println("line was a complete command");
		    }

		    boolean eval_exception = true;
		    TclObject commandObj = TclString.newInstance(command);

		    try {
			commandObj.preserve();
			interp.recordAndEval(commandObj, 0);
			eval_exception = false;
		    } catch (TclException e) {
			if (debug) {
			    System.out.println("eval returned exceptional condition");
			}

			int code = e.getCompletionCode();
			switch (code) {
			    case TCL.ERROR:
				// This really sucks. The getMessage() call on
				// a TclException will not always return a msg.
				// See TclException for super() problem.
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
		    } finally {
			commandObj.release();
		    }

		    if (!eval_exception) {
			if (debug) {
			    System.out.println("eval returned normally");
			}

			String evalResult = interp.getResult().toString();

			if (debug) {
			    System.out.println("eval result was \"" + evalResult + "\"");
			}

			if (evalResult.length() > 0) {
			    putLine(out, evalResult);
			}
		    }

		    // Empty out the incoming command buffer
		    sbuf.setLength(0);

		    // See if the user set a custom shell prompt for the next command

		    TclObject prompt;

		    try {
			prompt = interp.getVar("tcl_prompt1", TCL.GLOBAL_ONLY);
		    } catch (TclException e) {
			prompt = null;
		    }
		    if (prompt != null) {
			try {
			    interp.eval(prompt.toString(), TCL.EVAL_GLOBAL);
			} catch (TclException e) {
			    put(out, "% ");
			}
		    } else {
			put(out, "% ");
		    }

		    return 1;
		} else { // Interp.commandComplete() returned false

		    if (debug) {
			System.out.println("line was not a complete command");
		    }

		    // We don't have a complete command yet. Print out a level 2
		    // prompt message and wait for further inputs.

		    TclObject prompt;

		    try {
			prompt = interp.getVar("tcl_prompt2", TCL.GLOBAL_ONLY);
		    } catch (TclException e) {
			prompt = null;
		    }
		    if (prompt != null) {
			try {
			    interp.eval(prompt.toString(), TCL.EVAL_GLOBAL);
			} catch (TclException e) {
			    put(out, "");
			}
		    } else {
			put(out, "");
		    }

		    return 1;
		}
	    } // end processEvent method
	}; // end TclEvent innerclass

	// Add the event to the thread safe event queue
	interp.getNotifier().queueEvent(event, TCL.QUEUE_TAIL);

	// Tell this thread to wait until the event has been processed.
	event.sync();
    }
}

/*
 *----------------------------------------------------------------------
 *
 * getLine --
 *
 *	Gets a new line from System.in and put it in sbuf.
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

private void getLine() {
    // On Unix platforms, System.in.read() will block the delivery of
    // of AWT events. We must make sure System.in.available() is larger
    // than zero before attempting to read from System.in. Since
    // there is no asynchronous IO in Java, we must poll the System.in
    // every 100 milliseconds.

    int availableBytes = -1;

    if (sysInAvailableWorks) {
	try {
	    // Wait until there are inputs from System.in. On Unix,
	    // this usually means the user has pressed the return key.

	    availableBytes = 0;

	    while (availableBytes == 0) {
                availableBytes = System.in.available();

                //if (debug) {
                //    System.out.println(availableBytes +
                //        " bytes can be read from System.in");
                //}

		Thread.sleep(100);
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

    // Loop until user presses return or EOF is reached.
    char c2 = ' ';
    char c = ' ';

    if (debug) {
        System.out.println("now to read from System.in");
        System.out.println("availableBytes = " + availableBytes);
    }

    while (availableBytes != 0) {
	try {
	    int i = System.in.read();

	    if (i == -1) {
		if (sbuf.length() == 0) {
		    System.exit(0);
		} else {
		    return;
		}
	    }

	    c = (char) i;
            availableBytes--;

            if (debug) {
                System.out.print("(" + (availableBytes+1) + ") ");
                System.out.print("'" + c + "', ");
            }

	    // Temporary hack until Channel drivers are complete.  Convert
	    // the Windows \r\n to \n.

	    if (c == '\r') {
                if (debug) {
                    System.out.println("checking windows hack");
                }

	        i = System.in.read();
	        if (i == -1) {
		    if (sbuf.length() == 0) {
		        System.exit(0);
		    } else {
		        return;
		    }
		}
		c2 = (char) i; 
		if (c2 == '\n') {
		  c = c2;
		} else {
		  sbuf.append(c);
		  c = c2;
		}
	    }
	} catch (IOException e) {
	    // IOException shouldn't happen when reading from
	    // System.in. The only exceptional state is the EOF event,
	    // which is indicated by a return value of -1.

	    e.printStackTrace();
	    System.exit(0);
	}


	sbuf.append(c);

        //System.out.println("appending char '" + c + "' to sbuf");

	if (c == '\n') {
	    return;
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
    Channel channel,		// The Channel to print to.
    String s)			// The String to print.
{
    try {
        channel.write(interp,s);
        channel.write(interp,"\n");
        channel.flush(interp);
    } catch (IOException ex) {
        System.err.println("IOException in Shell.putLine()");
        ex.printStackTrace(System.err);
    } catch (TclException ex) {
        System.err.println("TclException in Shell.putLine()");
        ex.printStackTrace(System.err);
    }
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
    Channel channel,		// The Channel to print to.
    String s)			// The String to print.
{
    try {
        channel.write(interp,s);
        channel.flush(interp);
    } catch (IOException ex) {
        System.err.println("IOException in Shell.put()");
        ex.printStackTrace(System.err);
    } catch (TclException ex) {
        System.err.println("TclException in Shell.put()");
        ex.printStackTrace(System.err);
    }
}
} // end of class ConsoleThread
