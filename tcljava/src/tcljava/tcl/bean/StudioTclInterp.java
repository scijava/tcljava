/* 
 * StudioTclInterp.java --
 *
 *	Implements a container for the TclInterp bean to allow it
 *	to be used inside of Java Studio.
 *
 * Copyright (c) 1998 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: StudioTclInterp.java,v 1.1 1998/10/14 21:09:16 cvsadmin Exp $
 */

package tcl.bean;

import java.util.*;
import tcl.lang.*;
import java.io.*;
import java.net.URL;
import com.sun.jpropub.vj.vjcomp.*;
import java.awt.Panel;

public class StudioTclInterp extends VJComponent implements Command {

/*
 * Options for studio::port and studio::custom cmdProcs
 */

private String[] customOpts = {
    "getPanel",
    "getBeanData",
    "commitScript"
};
static final private String portOpts[] = {
    "in",
    "out",
    "twoway"
};
static final private String portSwitch[] = {
    "-portname",
    "-transfer",
    "-location",
    "-description"
};

/*
 * JavaStudio ID
 */

static final long serialVersionUID = 7;

/*
 * The bean that this class wraps around.
 */

private TclInterp tclInterp;

/*
 * Cache of existing ports.  The receiveMessage() method is passed 
 * an index into this array to retrieve the port object.
 */

StudioTclPort[] indexLookup = null;

/*
 * Used to store pins to keep, create or remove.
 */

Vector currentVec = null;
Vector reuseVec   = null;
Vector pendingVec = null;

/*
 * The interp used to create and manage a custom UI for the beans 
 * customizer.
 */

private transient Interp customInterp = null;

/*
 * The panel that the custom script draws into.
 */

private transient Panel customPanel  = null;

/*
 * Set if a custom UI exists and should be drawn when initializing
 * the customizer.
 */

private boolean customExists = false;

/*
 * Set if the custom panel is showing.  Used by the commitPage()
 * method to determine if the customExixts flag should be set and 
 * the customCallback should be eval-ed.
 */

private boolean customShowing = false;

/*
 * Data passed from the custom script to the bean script.
 */

private String customData = null;

/*
 * Script to eval when the customizer's commitPage() method is called.
 * The script retrieves info from the custom UI to set in customData.
 */

private String customCallback = null;

/*
 * Script that defines who to create and interact with the custom UI.
 */

private String customScript = null;

/*
 *----------------------------------------------------------------------
 *
 * StudioTclInterp --
 *
 *	Default constructor necessary for the bean to work.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public 
StudioTclInterp() 
{
}

/*
 *----------------------------------------------------------------------
 *
 * VJComponentInit --
 *
 *	Initialize the Bean and the VJ wrapper.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public void
VJComponentInit(
    String nm)
{
    /*
     * We start with no ports.
     */

    tclInterp = new TclInterp();
    try {
	VJComponentInit(tclInterp, null, null);
    } catch (Exception e) {
	printError("VJComponentInit Error: " + e);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * receiveMessage --
 *
 *	Handles an incoming message for some port.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Will set a varialble in the Tcl interp which can have
 *	almost any side-effect.
 *
 *----------------------------------------------------------------------
 */

public void 
receiveMessage(
    Object msg, 
    int port) 
{
    Interp interp = tclInterp.getInterp();
    try {
	
	if (tclInterp == null) {
	    printError("  oh my!!!");
	}
	if (interp == null) {
	    printError("  yikes!!!");
	}
	StudioTclPort tclPort = this.indexLookup[port];
	if (tclPort == null) {
	    printError("  bummer!!!");
	}
	String varName = tclPort.inVarName;
        TclObject obj = ReflectObject.newInstance(interp, Object.class ,msg);
	
	interp.setVar(varName, obj, TCL.GLOBAL_ONLY|TCL.LEAVE_ERR_MSG);
    } catch (TclException e) {
 	printError(interp.getResult());
    } catch (Exception e) {
 	printError("   ouch!!!");
    }
}

/*
 *----------------------------------------------------------------------
 *
 * getScript --
 *
 *	Encapsulation of the TclInterp's method.
 *
 * Results:
 *	The script that represents the curtrent state of the Tcl Bean.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

String
getScript()
{
    return(tclInterp.getScript());
}

/*
 *----------------------------------------------------------------------
 *
 * setScript --
 *
 *	Encapsulation of the TclInterp's method.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Sets a new value for the script, but does not eval it.
 *
 *----------------------------------------------------------------------
 */

void
setScript(
    String script)	// Script that defines what the Tcl Bean does.
{
    tclInterp.setScript(script);
}

/*
 *----------------------------------------------------------------------
 *
 * getInterp --
 *
 *	Encapsulation of the TclInterp's method.
 *
 * Results:
 *	Returns the current interp.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

Interp
getInterp()
{
    return(tclInterp.getInterp());
}

/*
 *----------------------------------------------------------------------
 *
 * resetInterp --
 *
 *	Resets the TclInterp associated with this object and sources
 * 	the util.tcl script.
 *
 * Results:
 *	The Interp object that has just been reset.
 *
 * Side effects:
 *	All interp state is lost, and a new interp is allocated with 
 *	the default file(s) sourced.
 *
 *----------------------------------------------------------------------
 */

Interp
resetInterp()
{
    Interp interp;
    InputStream dataStream = null;
    
    interp = tclInterp.resetInterp();
    interp.createCommand("studio::port", this);
    interp.createCommand("studio::custom", this);
    
    reuseVec   = null;
    pendingVec = null;

    try {
 	URL url = getClass().getResource("scripts/util.tcl");
 	dataStream = url.openStream();
 	byte charArray[] = new byte[dataStream.available()];
 	dataStream.read(charArray);
 	interp.eval(new String(charArray), TCL.GLOBAL_ONLY);
    } catch (TclException e) {
 	printError(interp.getResult());
    } catch (Exception e) {
 	printError(e);
    } finally {
  	try {
 	    dataStream.close();
 	} catch (IOException e) {;}
    }
    return interp;
}

/*
 *----------------------------------------------------------------------
 *
 * readObject --
 *
 *	Re-initialize the interp and evaluate the script if there is
 *	one associated with this bean.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	If the script is valid, it is evaled in the current interp.
 *
 *----------------------------------------------------------------------
 */

private void 
readObject(
    java.io.ObjectInputStream in)
throws 
    IOException, ClassNotFoundException
{
    in.defaultReadObject();
    Interp interp = null;
    
    try {
	interp = resetInterp();
	String script = getScript();
	
	if ((script != null) && !(script.equals(""))) {
	    interp.eval(script);
	}
	
	for (Enumeration e = currentVec.elements();
	     e.hasMoreElements(); ) {
	    
	    StudioTclPort count = (StudioTclPort) e.nextElement();
	    count.initWithInterp(interp);
	}
    } catch (TclException e) {
	printError(interp.getResult());
    } catch (Exception e) {
	printError(e);
    }	
}

/*
 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	Determine which command proc to call.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public void 
cmdProc(
    Interp interp,      // the current interpreter.
    TclObject argv[])   // command arguments.
throws
    TclException        // If errors occur in the port or custom procs.
{
    if (argv[0].toString().equals("studio::port")) {
	portCmdProc(interp, argv);
    } else {
	customCmdProc(interp, argv);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * portCmdProc --
 *
 *	Implements a Tcl command for interacting with Java Studio.  This
 *	command will let you set input & output ports, thier types & 
 *	location.  
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	New ports may be created.
 *
 *----------------------------------------------------------------------
 */

void
portCmdProc(
    Interp interp,      // the current interpreter.
    TclObject argv[])   // command arguments.
throws
    TclException        // If errors occur in the port or custom procs.
{
    int i, opt, arg;
    int pinType = -1;
    String key = "";
    TclObject variable;
    TclObject portname = null;
    TclObject transfer = null;
    TclObject location = null;
    TclObject description  = null;
    StudioTclPort tclPort = null;

    if (argv.length < 3) {
	throw new TclNumArgsException(interp, 1, argv, 
		"option variable ?arg arg ...?");
    }
    
    opt = TclIndex.get(interp, argv[1], portOpts, "option", 0);
    variable = argv[2];
    for (i = 3; i < argv.length; i += 2) {
	arg = TclIndex.get(interp, argv[i], portSwitch, "switch", 0);
	if ((i + 1) >= argv.length) {
	    throw new TclException(interp, "value for \"" +
		    argv[i] + "\" missing");
	}
	switch (arg) {
	case 0:		/* portname */
	    portname = argv[i + 1];
	    break;

	case 1:		/* transfer */ 
	    transfer = argv[i + 1];
	    break;

	case 2:		/* location */ 
	    location = argv[i + 1];
	    break;

	case 3:		/* description */ 
	    description = argv[i + 1];
	    break;
	}
    }
    
    switch (opt) {
    case 0:	/* in */
	/*
	 * Create a new port with the OUT var and transfer type as null.
	 */
	
	pinType = VJPortDescriptor.IN_ONLY;
	tclPort = new StudioTclPort(this, pinType, location, portname,
		description, variable, null, transfer, null);
	break;
	
    case 1:	/* out */
	/*
	 * Create a new port with the IN var and transfer type as null.
	 */
	
	pinType = VJPortDescriptor.OUT_ONLY;
	tclPort = new StudioTclPort(this, pinType, location, portname,
		description, null, variable, null, transfer);
	break;
	
    case 2:	/* twoway */
	TclObject inTrans, outTrans;
	
	if (TclList.getLength(interp, variable) != 2) {
	    throw new TclException(interp, 
		    "variable must be a list of two elements");
	}
	if (transfer != null) {
	    if (TclList.getLength(interp, transfer) != 2) {
		throw new TclException(interp, 
			"transfer argument must be a list of two elements");
	    }
	    inTrans  = TclList.index(interp, transfer, 0);
	    outTrans = TclList.index(interp, transfer, 1);
	} else {
	    inTrans  = null;
	    outTrans = null;
	}
	
	/*
	 * Create a new port supplying values for in and out
	 * var and transfer types.
	 */
	
	pinType = VJPortDescriptor.TWO_WAY;
	tclPort = new StudioTclPort(this, pinType, location, portname,
		    description, 
		    TclList.index(interp, variable, 0),
		    TclList.index(interp, variable, 1),
		    inTrans, outTrans);
	break;
    }
	
    /*
     * See if the tclPort already exists in the currentVec.  If so then
     * put the existion port in the reuseVec to cache the object.
     */
    
    tclPort = reusePort(tclPort);
	
    if (ambiguousPort(tclPort)) {
	printError("cannot create two ports with identical values");
	return;
    }
    if (pendingVec == null) {
	pendingVec = new Vector();
    }
    pendingVec.addElement(tclPort);
}

/*
 *----------------------------------------------------------------------
 *
 * reusePort --
 *
 *	Determines if the port already exists.  If so the put the 
 * 	existing port on the reuseVec to cache the object.
 *
 * Results:
 *	If the port already exists, return that port, else return
 *	the port that was passed in.
 *
 * Side effects:
 *	If the port exists, then it will be added to the reuseVec.
 *
 *----------------------------------------------------------------------
 */

StudioTclPort
reusePort(
    StudioTclPort port) 
{
   int index;

    try {
	if (currentVec != null) {
	    /*
	     * If the index is >= 0, then the port exists and has the
	     * identicle variable, portname, etc.  Reuse this port.
	     */

	    index = currentVec.indexOf(port);
	    if (index >= 0) {
		if (reuseVec == null) {
		    reuseVec = new Vector();
		}
		port = (StudioTclPort)currentVec.elementAt(index);
		reuseVec.addElement(port);
	    }
	}
    } catch (Exception e) {
	printError("portExist Error: " + e);
    }
    return port;
}

/*
 *----------------------------------------------------------------------
 *
 * ambiguousPort --
 *
 *	Check to see if a StudioTclPort with identical values to port 
 *	is already on the pendingVec.
 *
 * Results:
 *	True if an identical port is already on the pendingVec.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

boolean
ambiguousPort(
    StudioTclPort port)
{
    if (pendingVec == null) {
	return false;
    }
    /*
     * If the index is >= 0, then the port exists and has the
     * identicle variable, portname, etc.
     */
    
    if ((pendingVec.indexOf(port)) >= 0) {
	return true;
    } else {
	return false;
    }
}

/*
 *----------------------------------------------------------------------
 *
 * customCmdProc --
 *
 *	Implement the Tcl command for manipulating the JavaStudio
 *	bean's customizer.  This command will return a Panel object 
 *	for the script to draw into, retrieve data from the UI, and 
 *	set a callback to be fired when the page is commited that sets
 *	the return value for data.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

void
customCmdProc(
    Interp interp,      // the current interpreter.
    TclObject argv[])   // command arguments.
throws
    TclException        // If errors occur in the port or custom procs.
{
    int cmdOpt;

    cmdOpt = TclIndex.get(interp, argv[1], customOpts, "option", 0);
    switch(cmdOpt) {
    case 0:		/* getPanel */
	if (argv.length != 2) {
	    throw new TclNumArgsException(interp, 1, argv, "getPanel");
	}
	interp.setResult(
             ReflectObject.newInstance(interp, Panel.class, getCustomPanel()));
	break;
    case 1:		/* getBeanData */
	if (argv.length != 2) {
	    throw new TclNumArgsException(interp, 1, argv, "getBeanData");
	}
	interp.setResult(getCustomData());
	break;
    case 2:		/* commitScript */
	if (argv.length != 3) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "commitScript script");
	}
	customCallback = argv[2].toString();
	break;
    }
}

/*
 *----------------------------------------------------------------------
 *
 * initCustomInfo --
 *
 *	Initialize the custom data.  Currently, only the customScript
 *	is set because all other data is re-initialized on every call 
 * 	to evalCustom().
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

void
initCustom(
    String script)	// Script that defines the Customizer interface.
{
    customScript = script;
}

/*
 *----------------------------------------------------------------------
 *
 * evalCustom --
 *
 *	Evaluate the custom script which is responsible for populating
 *	the the customPanel.  Each time this is called, a new Interp
 *	and Panel must be created because the state of the previous
 *	Panel may not accurately reflect the actual state of the bean
 *	(e.g. The user types into a textbox and selects "CANCEL".  The
 *	chars typed must not appear the next time the Panel is 
 *	displayed.)
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	A new Interp and Panel are created.
 *
 *----------------------------------------------------------------------
 */

void
evalCustom()
throws
    TclException
{
    if (customScript == null) {
	printError("customInterp never initialized.");
	return;
    }
    try {
	customInterp = new Interp();
	customInterp.createCommand("studio::custom", this);
	customPanel = new Panel();
	customInterp.eval(customScript);
    } catch (TclException e) {
	printError(customInterp.getResult());
	throw new TclException(TCL.ERROR);
    } 
}

/*
 *----------------------------------------------------------------------
 *
 * invalidateCustomInfo --
 *
 *	Clears the custom state to its initial values.  The customPanel
 *	must be removed before this is called or the handle to the 
 *	object is lost.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The customizer will draw the default UI the next time the 
 *	initPage() method is called.  
 *
 *----------------------------------------------------------------------
 */

void
invalidateCustom()
{
    customExists   = false;
    customShowing  = false;
    customCallback = null;
    customData     = null;
    customInterp   = null;
    customPanel    = null;
    customScript   = null;
}

/*
 *----------------------------------------------------------------------
 *
 * customExists --
 *
 *	True denotes that the custom interface should be drawn on
 *	subsequent calls to the customizer's initPage() method.  
 *
 * Results:
 *	Boolean value of the customExists variable.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

boolean
customExists()
{
    return customExists;
}

/*
 *----------------------------------------------------------------------
 *
 * setCustomExists --
 *
 *	Sets the return value to the customExists() method.  This value 
 *	should be set to true if the customPanel is showing when the 
 *	commitPage() method of the customizer is called..
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

void
setCustomExists(
    boolean b)
{
    customExists = b;
}

/*
 *----------------------------------------------------------------------
 *
 * customShowing --
 *
 *	Custom is showing when the customizer is displaying the
 *	customPanel.  It is used to set the value of customExists
 *	and to determine which widget to destroy.
 *
 * Results:
 *	True if the customPanel is not null and is showing.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

boolean
customShowing()
{
    return customShowing;
}

/*
 *----------------------------------------------------------------------
 *
 * setCustomShowing --
 *
 *	Sets the value for the customShowing flag.  The isShowing() 
 *	method cannot be used because there are situations where 
 *	Studio destroys the window before the commitPage() method
 *	of the customizer is called.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

void
setCustomShowing(
    boolean b)
{
    customShowing = b;
}

/*
 *----------------------------------------------------------------------
 *
 * getCustomPanel --
 *
 *	Return the Panel object that displays the custom UI.
 *
 * Results:
 *	A java.awt.Panel object.
 *
 * Side effects:
 *	If the customPanel is null, create a new Panel.
 *
 *----------------------------------------------------------------------
 */

Panel
getCustomPanel()
{
    if (customPanel == null) {
	customPanel = new Panel();
    }
    return customPanel;
}

/*
 *----------------------------------------------------------------------
 *
 * getCustomData --
 *
 *	Return the value of the customData that is set in calls to 
 *	evalCustomCallback.  If customData is null return an empty 
 *	string.
 *
 * Results:
 *	String that is the customData.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

String
getCustomData()
throws
    TclException
{
    if (customData == null) {
	return "";
    } 
    return customData;
}

/*
 *----------------------------------------------------------------------
 *
 * evalCustomCallback --
 *
 *	Evaluate the customCallback script which retrieves data from the
 *	custom UI.  Set the result of the evaluation in customData.
 *	This method should be called during the commitPage() method of
 *	the customizer to get the most recent state of the custom UI.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The customData variable is set with info on the state of the 
 *	custom UI.
 *
 *----------------------------------------------------------------------
 */

void
evalCustomCallback(
    Interp interp)
throws 
    TclException
{
    int compCode;

    try {
	customInterp.eval(customCallback);
	customData = customInterp.getResult().toString();
    } catch (TclException e) {
	compCode = e.getCompletionCode();

	if (compCode == TCL.RETURN) {
	    /*
	     * The script invoked the "return" command. We treat this
	     * as a normal completion -- even if the command
	     * was "return -code error".
	     */

	    customData = customInterp.getResult().toString();
	} else if (compCode == TCL.BREAK) {
	    throw new TclException(interp, 
		    "invoked \"break\" outside of a loop");
	} else if (compCode == TCL.CONTINUE) {
	    throw new TclException(interp, 
		    "invoked \"continue\" outside of a loop");
	} else {
	    throw new TclException(interp, 
		    customInterp.getResult().toString());
	}	
    }
}

/*
 *----------------------------------------------------------------------
 *
 * printError --
 *
 *	Currently this function does nothing but print the object.  
 *	However all error messages are sent to this function in case
 *	we want to add alternative ways to print error messages in the 
 *	future.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static void
printError(
    Object msg)
{
    System.out.println(msg);
}
} // end StudioTclInterp

