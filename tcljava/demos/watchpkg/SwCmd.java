/*
 * SwCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 *   This file implements the SwCmd class.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */

import tcl.lang.*;

/*
 * This class implements the "sw" command in StopWatchPackage.
 */

class SwCmd implements Command {
    
    StopWatchThread swThread = null;  /* GUI and internal time monitor */

    /*
     * This procedure is invoked to process the "sw" Tcl command
     * -- it takes the following subcommands:  new, set, stop, resume,
     *    and die
     *
     * sw new       --> starts a new stopwatch at time 0
     *
     * sw set [sec] --> sets the stopwatch to specified time,
     *                  starts counting down
     *
     * sw stop      --> returns current time, halts the stopwatch
     *
     * sw resume    --> returns current time, starts counting down
     *
     * sw die       --> kills the stopwatch (by hiding the frame and
     *                     stopping the StopWatchThread)
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {

	if (argv.length < 2) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "[new|set seconds|stop|resume|die]");
	}

	if("new".compareTo(argv[1].toString()) == 0) {
	    if (argv.length != 2) {
		throw new TclNumArgsException(interp, 2, argv, "");
	    }
	    if (swThread != null) {
		throw new TclException(interp, 
			"error:  stopwatch is already running");
	    }
	    interp.setResult(0);
	    swThread = new StopWatchThread();
	    swThread.start();	
	    return;
	}

	if (swThread == null) {
	    throw new TclException(interp, 
		    "error:  no stopwatch currently running");
	}

	if("set".compareTo(argv[1].toString()) == 0) {
	    if (argv.length != 3) {
		throw new TclNumArgsException(interp, 2, argv, "seconds");
	    }
	    swThread.setTime(TclInteger.get(interp, argv[2]));
	    return;
	}
	if (argv.length != 2) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "[set seconds|stop|resume|die]");
	}
	if("stop".compareTo(argv[1].toString()) == 0) {
	    int sec = swThread.stopCountdown();
	    interp.setResult(sec);
	    return;
	}
	if("resume".compareTo(argv[1].toString()) == 0) {
	    int sec = swThread.resumeCountdown();
	    interp.setResult(sec);
	    return;
	}
	if("die".compareTo(argv[1].toString()) == 0) {
	    swThread.die();
	    swThread = null;
	    return;
	}
	throw new TclException(interp, "bad sw option \""
                    + argv[1].toString() 
                    + "\": must be new, set, stop, resume, or die");
    }
}
