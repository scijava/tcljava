/*
 * AfterCmd.java --
 *
 *	Implements the built-in "after" Tcl command.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: AfterCmd.java,v 1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang;

import java.util.Vector;

/*
 * This class implements the built-in "after" command in Tcl.
 */

class AfterCmd implements Command {

/*
 * The list of handler are stored as AssocData in the interp.
 */

AfterAssocData assocData = null;

/*
 * Valid command options.
 */

static final private String validOpts[] = {
    "cancel",
    "idle",
    "info"
};

static final int OPT_CANCEL = 0;
static final int OPT_IDLE   = 1;
static final int OPT_INFO   = 2;


/*
 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked as part of the Command interface to
 *	process the "after" Tcl command.  See the user documentation
 *	for details on what it does.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	See the user documentation.
 *
 *----------------------------------------------------------------------
 */

public void
cmdProc(
    Interp interp,		// Current interpreter.
    TclObject argv[])		// Argument list.
throws 
    TclException 		// A standard Tcl exception.
{
    int i;
    Notifier notifier = (Notifier)interp.getNotifier();
    Object info;

    if (assocData == null) {
	/*
	 * Create the "after" information associated for this
	 * interpreter, if it doesn't already exist.
	 */

	assocData = (AfterAssocData)interp.getAssocData("tclAfter");
	if (assocData == null) {
	    assocData = new AfterAssocData();
	    interp.setAssocData("tclAfter", assocData);
	}
    }

    if (argv.length < 2) {
	throw new TclNumArgsException(interp, 1, argv, "option ?arg arg ...?");
    }

    /*
     * First lets see if the command was passed a number as the first argument.
     */

    boolean isNumber = false;
    int ms = 0;

    if (argv[1].getInternalRep() instanceof TclInteger) {
	ms = TclInteger.get(interp, argv[1]);
	isNumber = true;
    } else {
	String s = argv[1].toString();
	if ((s.length() > 0) && (Character.isDigit(s.charAt(0)))) {
	    ms = TclInteger.get(interp, argv[1]);
	    isNumber = true;
	}
    }

    if (isNumber) {
	if (ms < 0) {
	    ms = 0;
	}
	if (argv.length == 2) {
	    /*
	     * Sleep for at least the given milliseconds and return.
	     */

	    long endTime = System.currentTimeMillis() + ms;
	    while (true) {
		try {
		    Thread.sleep(ms);
		    return;
		} catch (InterruptedException e) {
		    /*
		     * We got interrupted. Sleep again if we havn't slept
		     * long enough yet.
		     */

		    long sysTime = System.currentTimeMillis();
		    if (sysTime >= endTime) {
			return;
		    }
		    ms = (int)(endTime - sysTime);
		    continue;
		}
	    }
	}

	TclObject cmd = getCmdObject(argv);
	cmd.preserve();

	assocData.lastAfterId ++;
	TimerInfo timerInfo = new TimerInfo(notifier, ms);
	timerInfo.interp = interp;
	timerInfo.command = cmd;
	timerInfo.id = assocData.lastAfterId;

	assocData.handlers.addElement(timerInfo);

	interp.setResult("after#" + timerInfo.id);

	return;
    }

    /*
     * If it's not a number it must be a subcommand.
     */

    int index;

    try {
	index = TclIndex.get(interp, argv[1], validOpts, "option", 0);
    } catch (TclException e) {
	throw new TclException(interp, "bad argument \"" + argv[1] +
		"\": must be cancel, idle, info, or a number");
    }

    switch (index) {
    case OPT_CANCEL:
	if (argv.length < 3) {
	    throw new TclNumArgsException(interp, 2, argv, "id|command");
	}
	
	TclObject arg = getCmdObject(argv);
	arg.preserve();

	/*
	 * Search the timer/idle handler by id or by command.
	 */

	info = null;
	for (i = 0; i < assocData.handlers.size(); i++) {
	    Object obj = assocData.handlers.elementAt(i);
	    if (obj instanceof TimerInfo) {
		TclObject cmd = ((TimerInfo)obj).command;

		if ((cmd == arg) || cmd.toString().equals(arg.toString())) {
		    info = obj;
		    break;
		}
	    } else {
		TclObject cmd = ((IdleInfo)obj).command;

		if ((cmd == arg) || cmd.toString().equals(arg.toString())) {
		    info = obj;
		    break;
		}
	    }
	}
	if (info == null) {
	    info = getAfterEvent(arg.toString());
	}
	arg.release();

	/*
	 * Cancel the handler.
	 */

	if (info != null) {
	    if (info instanceof TimerInfo) {
		((TimerInfo)info).cancel();
		((TimerInfo)info).command.release();
	    } else {
		((IdleInfo)info).cancel();
		((IdleInfo)info).command.release();
	    }

	    assocData.handlers.removeElement(info);
	}
	break;

    case OPT_IDLE:
	if (argv.length < 3) {
	    throw new TclNumArgsException(interp, 2, argv,"script script ...");
	}

	TclObject cmd = getCmdObject(argv);
	cmd.preserve();
	assocData.lastAfterId ++;

	IdleInfo idleInfo = new IdleInfo(notifier);
	idleInfo.interp = interp;
	idleInfo.command = cmd;
	idleInfo.id = assocData.lastAfterId;

	assocData.handlers.addElement(idleInfo);

	interp.setResult("after#" + idleInfo.id);
	break;

    case OPT_INFO:
	if (argv.length == 2) {
	    /*
	     * No id is given. Return a list of current after id's.
	     */

	    TclObject list = TclList.newInstance();
	    for (i = 0; i < assocData.handlers.size(); i++) {
		int id;
		Object obj = assocData.handlers.elementAt(i);
		if (obj instanceof TimerInfo) {
		    id = ((TimerInfo)obj).id;
		} else {
		    id = ((IdleInfo)obj).id;
		}
		TclList.append(interp, list, TclString.newInstance("after#" +
			id));
	    }
	    interp.setResult(list);
	    return;
	}
	if (argv.length != 3) {
	    throw new TclNumArgsException(interp, 2, argv, "?id?");
	}	    

	/*
	 * Return command and type of the given after id.
	 */

	info = getAfterEvent(argv[2].toString());
	if (info == null) {
	   throw new TclException(interp, "event \"" + argv[2] +
		    "\" doesn't exist");
	}
	TclObject list = TclList.newInstance();
	TclList.append(interp, list,
		((info instanceof TimerInfo) ?
		((TimerInfo)info).command : ((IdleInfo)info).command));
	TclList.append(interp, list, TclString.newInstance(
		(info instanceof TimerInfo) ? "timer" : "idle"));

	interp.setResult(list);
	break;
    }
}

/*
 *----------------------------------------------------------------------
 *
 * getCmdObject --
 *
 *	Returns a TclObject that contains the command script passed
 *	to the "after" command.
 *
 * Results:
 *	A concatenation of the objects from argv[2] through argv[n-1].
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private TclObject
getCmdObject(
    TclObject argv[])		// Argument list passed to the "after" command.
{
    if (argv.length == 3) {
	return argv[2];
    } else {
	TclObject cmd = TclString.newInstance(Util.concat(2, argv.length-1,
		argv));
	return cmd;
    }
}

/*
 *----------------------------------------------------------------------
 *
 * getAfterEvent --
 *
 *	This procedure parses an "after" id such as "after#4" and
 *	returns an AfterInfo object.
 *
 * Results:
 *	The return value is an AfterInfo object.  if one is
 *	found that corresponds to "string", or null if no
 *	corresponding after event can be found.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private Object
getAfterEvent(
    String string)		// Textual identifier for after event, such
				// as "after#6".
{
    if (!string.startsWith("after#")) {
	return null;
    }

    StrtoulResult res = Util.strtoul(string, 6, 10);
    if (res.errno != 0) {
	return null;
    }

    for (int i = 0; i < assocData.handlers.size(); i++) {
	Object obj = assocData.handlers.elementAt(i);
	if (obj instanceof TimerInfo) {
	    if (((TimerInfo)obj).id == res.value) {
		return obj;
	    }
	} else {
	    if (((IdleInfo)obj).id == res.value) {
		return obj;
	    }
	}
    }

    return null;
}

/*
 * This inner class manages the list of handlers created by the
 * "after" command. We keep the handler has an AssocData so that they
 * will continue to exist even if the "after" command is deleted.
 */

class AfterAssocData implements AssocData {

/*
 * The set of handlers created but not yet fired.
 */

Vector handlers = new Vector();

/*
 * Timer identifier of most recently created timer.	
 */

int lastAfterId = 0;


/*
 *----------------------------------------------------------------------
 *
 * disposeAssocData --
 *
 *	This method is called when the interpreter is destroyed or
 *	when Interp.deleteAssocData is called on a registered
 *	AssocData instance.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	All unfired handler are cancelled; their command objects are
 *	released.
 *
 *----------------------------------------------------------------------
 */

public void
disposeAssocData(
    Interp interp)		// The interpreter in which this AssocData
				// instance is registered in.
{
    for (int i = assocData.handlers.size() - 1; i >= 0; i--) {
	Object info = assocData.handlers.elementAt(i);
	assocData.handlers.removeElementAt(i);
	if (info instanceof TimerInfo) {
	    ((TimerInfo)info).cancel();
	    ((TimerInfo)info).command.release();
	} else {
	    ((IdleInfo)info).cancel();
	    ((IdleInfo)info).command.release();
	}
    }
    assocData = null;
}
    
} // end AfterCmd.AfterAssocData


/*
 * This inner class implement timer handlers for the "after"
 * command. It stores a script to be executed when the timer event is
 * fired.
 */

class TimerInfo extends TimerHandler {

/*
 * Interpreter in which the script should be executed.
 */

Interp interp;

/*
 * Command to execute when the timer fires.
 */

TclObject command;

/*
 * Integer identifier for command;  used to cancel it.
 */

int id;


/*
 *----------------------------------------------------------------------
 *
 * TimerInfo --
 *
 *	Constructs a new TimerInfo instance.
 *
 * Side effects:
 *	The timer is initialized by the super class's constructor.
 *
 *----------------------------------------------------------------------
 */

TimerInfo(
    Notifier n,			// The notifier to fire the event.
    int milliseconds)		// How many milliseconds to wait
				// before invoking processTimerEvent().
{
    super(n, milliseconds);
}

/*
 *----------------------------------------------------------------------
 *
 * processTimerEvent --
 *
 *	Process the timer event.
 *
 * Results:	
 *	None.
 *
 * Side effects:
 *	The command executed by this timer can have arbitrary side
 *	effects.
 *
 *----------------------------------------------------------------------
 */

public void
processTimerEvent()
{
    try {
	assocData.handlers.removeElement(this);
	interp.eval(command, TCL.GLOBAL_ONLY);
    } catch (TclException e) {
	interp.addErrorInfo("\n    (\"after\" script)");
	interp.backgroundError();
    } finally {
	command.release();
	command = null;
    }
}

} // end AfterCmd.AfterInfo


/*
 * This inner class implement idle handlers for the "after"
 * command. It stores a script to be executed when the idle event is
 * fired.
 */

class IdleInfo extends IdleHandler {

/*
 * Interpreter in which the script should be executed.
 */

Interp interp;

/*
 * Command to execute when the idle event fires.
 */

TclObject command;

/*
 * Integer identifier for command;  used to cancel it.
 */

int id;


/*
 *----------------------------------------------------------------------
 *
 * IdleInfo --
 *
 *	Constructs a new IdleInfo instance.
 *
 * Side effects:
 *	The idle handler is initialized by the super class's
 *	constructor.
 *
 *----------------------------------------------------------------------
 */

IdleInfo(
    Notifier n)			// The notifier to fire the event.
{
    super(n);
}

/*
 *----------------------------------------------------------------------
 *
 * processIdleEvent --
 *
 *	Process the idle event.
 *
 * Results:	
 *	None.
 *
 * Side effects:
 *	The command executed by this idle handler can have arbitrary side
 *	effects.
 *
 *----------------------------------------------------------------------
 */

public void
processIdleEvent()
{
    try {
	assocData.handlers.removeElement(this);
	interp.eval(command, TCL.GLOBAL_ONLY);
    } catch (TclException e) {
	interp.addErrorInfo("\n    (\"after\" script)");
	interp.backgroundError();
    } finally {
	command.release();
	command = null;
    }
}

} // end AfterCmd.AfterInfo

} // end AfterCmd
