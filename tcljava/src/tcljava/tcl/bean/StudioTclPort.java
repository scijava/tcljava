/* 
 * StudioTclPort.java --
 *
 *	This object holds all the information about a port
 *	used in Java Studio.
 *
 * Copyright (c) 1998 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: StudioTclPort.java,v 1.1 1998/10/14 21:09:17 cvsadmin Exp $
 */

package tcl.bean;

import tcl.lang.*;
import com.sun.jpropub.vj.vjcomp.*;
import com.sun.jpropub.vj.vjcomp.util.*;
import java.io.Serializable;

class StudioTclPort implements VarTrace, Serializable {
/*
 * List of valid option to specify the type of port to create.
 */

private static final String inTransfers[] = {
    "basicToDouble", 
    "basicToFloat", 
    "basicToInteger",
    "basicToLong", 
    "basicToString", 
    "basicToBoolean",
    "numberToDouble", 
    "numberToFloat", 
    "numberToInteger",
    "numberToLong",
    "object", 
    "trigger",
    "dynamic"
};
private static final String outTransfers[] = {
    "doubleToBasic", 
    "floatToBasic", 
    "integerToBasic",
    "longToBasic", 
    "object", 
    "stringToBasic", 
    "booleanToBasic", 
    "dynamic"
};
private static final String locationStrings[] = {
    "anywhere", 
    "north", 
    "northLeft", 
    "northCenter",
    "northRight", 
    "south", 
    "southLeft", 
    "southCenter", 
    "southRight",	
    "west", 
    "westTop", 
    "westCenter", 
    "westBottom", 
    "east", 
    "eastTop", 
    "eastCenter", 
    "eastBottom"
};

/*
 * The port type: IN, OUT or TWO_WAY.
 */

int pinType = -1;		

/*
 * The pin location: EAST, WEST, etc.
 */

int location = -1;

/*
 * Name of input variable, if one exists.
 */

String inVarName = null;

/*
 * Name of output variable, if one exists.
 */

String outVarName = null;

/*
 * Name of port label.
 */

String portName = null;

/*
 * Gives a brief description of how to use the port, as well as 
 * providing for another degree of uniqueness.
 */

String description = null;

/*
 * String rep of the type of input port.  Used to test equality.
 */

String inTransfer = null;

/*
 * String rep of the type of output port.  Used to test equality.
 */

String outTransfer = null;

/*
 * Denotes that the port has been drawn and should not be redrawn.
 */

boolean added = false;  

/*
 * The port object that Studio knows about.
 */

VJPort port = null;
VJDynamicPortDescriptor newDesc = null;

/*
 *----------------------------------------------------------------------
 *
 * StudioTclPort --
 *
 *	Constructor for the StudioTclPort type.  This will create a
 *	Studio port based on the information passed into this function.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	A new VJPort & descriptor is created (on success).  Also, the
 *	Tcl objects may now be cached index objects.
 *
 *----------------------------------------------------------------------
 */

StudioTclPort(
    StudioTclInterp studioInterp, // Our Studio bean.
    int newPinType, 		  // Either IN_ONLY, OUT_ONLY or TWO_WAY.
    TclObject locObj,		  // The location constant (see ParseLocation).
    TclObject newPortName,	  // Name of port, null uses varName.
    TclObject descObj,		  // A description on how the port is used.
    TclObject inVar,		  // The variable to place input msgs.
    TclObject outVar,		  // The variable for output msgs.
    TclObject inputType,	  // The input data transfer type.  See
				  // the function ParseInputType for details.
    TclObject outputType)	  // The output data transfer type.  See
				  // the function ParseOutputType for details.
throws
    TclException        	  // Can happen if transferType is not valid
{
    pinType = newPinType;
    inVarName = null;
    outVarName = null;

    if ((inVar == null) && (outVar == null)) {
	throw new TclException(studioInterp.getInterp(),
	    "null values cannot be passed for inVar and outVar");
    }
    if (inVar != null) {
	inVarName = inVar.toString();
    }
    if (outVar != null) {
	outVarName = outVar.toString();
    }
    if (newPortName != null) {
	portName = newPortName.toString();
    } else {
	if (inVar != null) {
	    portName = inVarName;
	} else {
	    portName = outVarName;
	}
    }

    if (locObj == null) {
	if (pinType ==  VJPortDescriptor.IN_ONLY) {
	    location = VJPortDescriptor.WEST;
	} else if (pinType == VJPortDescriptor.OUT_ONLY) {
	    location = VJPortDescriptor.EAST;
	} else if (pinType == VJPortDescriptor.TWO_WAY) {
	    location = VJPortDescriptor.NORTH;
	} else {
	    throw new TclException(studioInterp.getInterp(),
		    "invalid pin type");
	}
    } else {
	location = ParseLocation(studioInterp, locObj);
    }
    if (descObj == null) {
	description = " : variable value in the formula";
    } else {
	description = descObj.toString();
    }

    try {
	InputDataTransfer inputTransfer;
	OutputDataTransfer outputTransfer;

	if (pinType == VJPortDescriptor.IN_ONLY) {
	    inputTransfer = ParseInputType(studioInterp, inputType);
	    port = new VJPort(studioInterp, inputTransfer);
	} else if (pinType == VJPortDescriptor.OUT_ONLY) {
	    outputTransfer = ParseOutputType(studioInterp, outputType);
	    port = new VJPort(studioInterp, outputTransfer);
	} else if (pinType == VJPortDescriptor.TWO_WAY) {
	    inputTransfer = ParseInputType(studioInterp, inputType);
	    outputTransfer = ParseOutputType(studioInterp, outputType);
	    port = new VJPort(studioInterp, outputTransfer, inputTransfer);
	} else {
	    throw new TclException(studioInterp.getInterp(),
		    "invalid pin type: must be in, out or twoway");
	}
    } catch (TclException e) {
	throw e;
    } catch (Exception e) {
	throw new TclException(studioInterp.getInterp(), e.toString());
    }
    if (port == null) {
	throw new TclException(studioInterp.getInterp(),
		"port object is null");
    }
    
    newDesc = new VJDynamicPortDescriptor(port,
	    portName, description,
	    pinType, location, studioInterp.getClass());
    if (newDesc == null) {
	throw new TclException(studioInterp.getInterp(),
	    "newDesc is null: VJDynamicPortDescriptor instantion error");
    }
}

/*
 *----------------------------------------------------------------------
 *
 * ParseOutputType --
 *
 *	Given a TclObject this command returns the appropiate
 *	OutputDataTransfer object
 *
 * Results:
 *	The OutputDataTransfer object..
 *
 * Side effects:
 *	The Tcl object is turned into an index object.
 *
 *----------------------------------------------------------------------
 */

private OutputDataTransfer
ParseOutputType(
    StudioTclInterp studioInterp, // The Tcl component.
    TclObject transferType)	  // The Tcl Object holding output type.
throws
    TclException        	  // Can happen if location string is bad.
{
    if (transferType == null) {
	outTransfer = "dynamic";
	return new VJDynamicOutputTransfer();
    }

    int type = TclIndex.get(studioInterp.getInterp(), 
	    transferType, outTransfers, "option", 0);

    outTransfer = transferType.toString();
      
    switch (type) {
    case 0:		/* doubleToBasic */
	return new DoubleToBasicOutputTransfer();
    case 1:		/* floatToBasic */
	return new FloatToBasicOutputTransfer();
    case 2:		/* integerToBasic */
	return new IntegerToBasicOutputTransfer();
    case 3:		/* longToBasic */
	return new LongToBasicOutputTransfer();
    case 4:		/* object */
	return new ObjectOutputTransfer();
    case 5:		/* stringToBasic */
	return new StringToBasicOutputTransfer();
    case 6:		/* booleanToBasic */
	return new BooleanToBasicOutputTransfer();
    case 7:		/* dynamic */
	return new VJDynamicOutputTransfer();
    }

    /*
     * This last line is need to avoid compiler warnings.  It should
     * never be reached.
     */

    return null;
}

/*
 *----------------------------------------------------------------------
 *
 * ParseInputType --
 *
 *	Given a TclObject this command returns the appropiate
 *	InputDataTransfer object
 *
 * Results:
 *	The InputDataTransfer object..
 *
 * Side effects:
 *	The Tcl object is turned into an index object.
 *
 *----------------------------------------------------------------------
 */

private InputDataTransfer
ParseInputType(
    StudioTclInterp studioInterp, // The Tcl component.
    TclObject transferType)	  // The Tcl Object holding output type.
throws
    TclException        // Can happen if location string is bad.
{
    if (transferType == null) {
	inTransfer = "dynamic";
	return new VJDynamicInputTransfer();
    }

    int type = TclIndex.get(studioInterp.getInterp(), 
	    transferType, inTransfers, "option", 0);
    inTransfer = transferType.toString();
      
    switch (type) {
    case 0:		/* basicToDouble */
	return new BasicToDoubleInputTransfer();
    case 1:		/* basicToFloat */
	return new BasicToFloatInputTransfer();
    case 2:		/* basicToInteger */
	return new BasicToIntegerInputTransfer();
    case 3:		/* basicToLong */
	return new BasicToLongInputTransfer();
    case 4:		/* basicToString */
	return new BasicToStringInputTransfer();
    case 5:		/* basicToBoolean */
	return new BasicToBooleanInputTransfer();
    case 6:		/* numberToDouble */
	return new NumberToDoubleInputTransfer();
    case 7:		/* numberToFloat */
	return new NumberToFloatInputTransfer();
    case 8:		/* numberToInteger */
	return new NumberToIntegerInputTransfer();
    case 9:		/* numberToLong */
	return new NumberToLongInputTransfer();
    case 10:		/* object */
	return new ObjectInputTransfer();
    case 11:		/* trigger */
	return new VJTriggerInputTransfer();
    case 12:		/* dynamic */
	return new VJDynamicInputTransfer();
    }

    /*
     * This last line is need to avoid compiler warnings.  It should
     * never be reached.
     */

    return null;
}

/*
 *----------------------------------------------------------------------
 *
 * ParseLocation --
 *
 *	Given a TclObject this command returns the PortDescriptor
 *	location.  This value is used to denote the location of the
 *	data input/output pins.
 *
 * Results:
 *	The location value.
 *
 * Side effects:
 *	The Tcl object is turned into an index object.
 *
 *----------------------------------------------------------------------
 */

private static int
ParseLocation(
    StudioTclInterp studioInterp, // The Tcl component.
    TclObject loc)		  // The Tcl Object holding location.
throws
    TclException	          // Can happen if location string is bad.
{
    int index = TclIndex.get(studioInterp.getInterp(), 
	    loc, locationStrings, "option", 0);

    switch (index) {
    case 0:		/* anywhere */
	return VJPortDescriptor.ANYWHERE;
    case 1:		/* north */
	return VJPortDescriptor.NORTH;
    case 2:		/* northLeft */
	return VJPortDescriptor.NORTH_LEFT;
    case 3:		/* northCenter */
	return VJPortDescriptor.NORTH_CENTER;
    case 4:		/* northRight */
	return VJPortDescriptor.NORTH_RIGHT;
    case 5:		/* south */
	return VJPortDescriptor.SOUTH;
    case 6:		/* southLeft */
	return VJPortDescriptor.SOUTH_LEFT;
    case 7:		/* southCenter */
	return VJPortDescriptor.SOUTH_CENTER;
    case 8:		/* southRight */
	return VJPortDescriptor.SOUTH_RIGHT;
    case 9:		/* west */
	return VJPortDescriptor.WEST;
    case 10:		/* westTop */
	return VJPortDescriptor.WEST_TOP;
    case 11:		/* westCenter */
	return VJPortDescriptor.WEST_CENTER;
    case 12:		/* westBottom */
	return VJPortDescriptor.WEST_BOTTOM;
    case 13:		/* east */
	return VJPortDescriptor.EAST;
    case 14:		/* eastTop */
	return VJPortDescriptor.EAST_TOP;
    case 15:		/* eastCenter */
	return VJPortDescriptor.EAST_CENTER;
    case 16:		/* eastBottom */
	return VJPortDescriptor.EAST_BOTTOM;
    }

    /*
     * This last line is need to avoid compiler warnings.  It should
     * never be reached.
     */

    return -1;
}

/*
 *----------------------------------------------------------------------
 *
 * initWithInterp --
 *
 *	Given a new Tcl interpreter this function initializes the port
 *	with information specific to this new interp.  Currently, this
 *	only effects out-put ports by creating a trace on the output
 *	variable so data is sent out the port with the variable is set.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	A trace is set in the interpreter is this is an output port.
 *
 *----------------------------------------------------------------------
 */

public void
initWithInterp(
    Interp interp)
{
    if ((pinType == VJPortDescriptor.OUT_ONLY) ||
	    (pinType == VJPortDescriptor.TWO_WAY)) {
	/*
	 * The outVarName was set when the object was created.  The trace
	 * is set for the global var named by outVarName.  The trace will
	 * fire when the variable is set.
	 */

	try {
	    interp.traceVar(outVarName, this, 
		    TCL.TRACE_WRITES + TCL.GLOBAL_ONLY);
	} catch (TclException e) {
	    System.out.println("trace creation error");
	}
    }
}

/*
 *----------------------------------------------------------------------
 *
 * traceProc --
 *
 *	This is called when the interp wants to send data..
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
traceProc(
    Interp interp, 	// The current interpreter.
    String part1,	// A Tcl variable or array name.
    String part2,	// Array element name or NULL.
    int flags)		// Mode flags: TCL.TRACE_READS, TCL.TRACE_WRITES or
{
    TclObject tclObj = null;

    try {
	tclObj = interp.getVar(outVarName, TCL.GLOBAL_ONLY);
    } catch (Exception e) {
	return;
    }

    try {
	Object javaObj = ReflectObject.get(interp, tclObj);
	this.port.sendMessage(javaObj);
    } catch (Exception e1) {
	String strMsg = tclObj.toString();
	try {
	    this.port.sendMessage(strMsg);
	} catch (Exception e2) {
	    System.out.println("error 2: " + e2);
	}
    }
}

/*
 *----------------------------------------------------------------------
 *
 * equals --
 *
 *	Override the Object.equals() method to test equality of ports.
 *	The new criteria for equality is identical portname, varname(s),
 *	location, and pinType.
 *
 * Results:
 *	True if equal.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public boolean
equals (
    Object portObject)
{
    StudioTclPort port = (StudioTclPort)portObject;
    if (port == null) {
	return false;
    }
    if (!this.portName.equals(port.portName)) {
	return false;
    }
    if (this.inVarName != null) {
	if (!this.inVarName.equals(port.inVarName)) {
	    return false;
	}
    } else if (port.inVarName != null) {
	return false;
    }
    if (this.outVarName != null) {
	if (!this.outVarName.equals(port.outVarName)) {
	    return false;
	}
    } else if (port.outVarName != null) {
	return false;
    }
    if (!this.description.equals(port.description)) {
	return false;
    }
    if (this.pinType != port.pinType) {
	return false;
    }
    if (this.location != port.location) {
	return false;
    }
    if (this.inTransfer != null) {
	if (!this.inTransfer.equals(port.inTransfer)) {
	    return false;
	}
    } else if (port.inTransfer != null) {
	return false;
    }
    if (this.outTransfer != null) {
	if (!this.outTransfer.equals(port.outTransfer)) {
	    return false;
	}
    } else if (port.outTransfer != null) {
	return false;
    }
    return true;
}
} // end StudioTclPort


