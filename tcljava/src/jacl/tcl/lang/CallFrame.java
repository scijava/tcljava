/*
 * CallFrame.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997-1998 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: CallFrame.java,v 1.6 1999/07/12 02:38:27 mo Exp $
 *
 */

package tcl.lang;

import java.util.*;

/**
 * This class implements a frame in the call stack.
 *
 * This class can be overridden to define new variable scoping rules for
 * the Tcl interpreter.
 */

class CallFrame {
    /**
     * The interpreter associated with this call frame.
     */

    protected Interp interp;


    /**
     * The Namespace this CallFrame is executing in.
     * Used to resolve commands and global variables.
     */

    NamespaceCmd.Namespace ns;

    /**
     * If true, the frame was pushed to execute a Tcl procedure
     * and may have local vars. If false, the frame was pushed to execute
     * a namespace command and var references are treated as references
     * to namespace vars; varTable is ignored.
     */

    boolean isProcCallFrame;

    /**
     * Stores the arguments of the procedure associated with this CallFrame.
     * Is null for global level.
     */

    TclObject[] objv;

    /**
     * Value of interp.frame when this procedure was invoked
     * (i.e. next in stack of all active procedures).
     */

    protected CallFrame caller;

    /**
     * Value of interp.varFrame when this procedure was invoked
     * (i.e. determines variable scoping within caller; same as
     * caller unless an "uplevel" command or something equivalent
     * was active in the caller).
     */

    protected CallFrame callerVar;

    /**
     * Level of recursion. = 0 for the global level.
     */

    protected int level;

    /**
     * Stores the variables of this CallFrame.
     */

    protected Hashtable varTable;


    /**
     * Creates a CallFrame for the global variables.
     * @param interp current interpreter.
     */

    CallFrame(Interp i) {
	interp	        = i;
	ns              = i.globalNs;
	varTable        = new Hashtable();
	caller          = null;
	callerVar       = null;
	objv            = null;
	level           = 0;
	isProcCallFrame = true;
    }

    /**
     * Creates a CallFrame. It changes the following variables:
     *
     * <ul>
     *     <li> this.caller
     *	   <li> this.callerVar
     *	   <li> interp.frame
     *	   <li> interp.varFrame
     * </ul>
     * @param i current interpreter.
     * @param proc the procedure to invoke in this call frame.
     * @param objv the arguments to the procedure.
     * @exception TclException if error occurs in parameter bindings.
     */
    CallFrame(Interp i, Procedure proc, TclObject[] objv)
	throws TclException {
	this(i);

	try {
	    chain(proc, objv);
	} catch (TclException e) {
	    dispose();
	    throw e;
	}
    }

    /**
     * Chain this frame into the call frame stack and binds the parameters
     * values to the formal parameters of the procedure.
     *
     * @param proc the procedure.
     * @param proc argv the parameter values.
     * @exception TclException if wrong number of arguments.
     */
    void chain(Procedure proc, TclObject[] objv)
	    throws TclException {
	// FIXME: double check this ns thing in case where proc is renamed to different ns.
	this.ns         = proc.ns;
	this.objv       = objv;
	// FIXME : quick level hack : fix later
	level           = (interp.varFrame == null) ? 1 : (interp.varFrame.level + 1);
	caller          = interp.frame;
	callerVar       = interp.varFrame;
	interp.frame    = this;
	interp.varFrame = this;

	// parameter bindings
	
	int numArgs = proc.argList.length;

	if ((!proc.isVarArgs) && (objv.length-1 > numArgs)) {
	    throw new TclException(interp, "called \"" + objv[0] +
		    "\" with too many arguments");
	}

	int i, j;
	for (i=0, j=1; i < numArgs; i++, j++) {
	    // Handle the special case of the last formal being
	    // "args".  When it occurs, assign it a list consisting of
	    // all the remaining actual arguments.

	    TclObject varName = proc.argList[i][0];
	    TclObject value = null;

	    if ((i == (numArgs-1)) && proc.isVarArgs) {
		value = TclList.newInstance();
		value.preserve();
		for (int k=j; k < objv.length; k++) {
		    TclList.append(interp, value, objv[k]);
		}
		interp.setVar(varName, value, 0);
		value.release();
	    }
	    else {
		if (j < objv.length) {
		    value = objv[j];
		} else if (proc.argList[i][1] != null) {
		    value = proc.argList[i][1];
		} else {
		    throw new TclException(interp,
			    "no value given for parameter \""+
		    	    varName + "\" to \"" + objv[0] + "\"");
		}
		interp.setVar(varName, value, 0);
	    }
	}
    }


    /**
     * @param name the name of the variable.
     *
     * @return true if a variable exists and is defined inside this
     *     CallFrame, false otherwise
     */

    static boolean exists(Interp interp, String name) {
	try {
	    Var[] result = Var.lookupVar(interp, name, null, 0,
					 "lookup", false, false);
	    if (result == null) {
		return false;
	    }
	    if (result[0].isVarUndefined()) {
		return false;
	    }
	    return true;
	} catch (TclException e) {
	    throw new TclRuntimeError("unexpected TclException: " + e);
	}
    }

    /**
     * @return an Vector the names of the (defined) variables
     *     in this CallFrame.
     */

    Vector getVarNames() {
	Vector vector = new Vector();

	for (Enumeration e1 = varTable.elements();
		e1.hasMoreElements(); ) {
	    Var v = (Var) e1.nextElement();
	    if (! v.isVarUndefined()) {
		vector.addElement(v.hashKey);
	    }
	}
	return vector;
    }

    /**
     * @return an Vector the names of the (defined) local variables
     *     in this CallFrame (excluding upvar's)
     */

    Vector getLocalVarNames() {
	Vector vector = new Vector();

	for (Enumeration e1 = varTable.elements();
		e1.hasMoreElements(); ) {
	    Var v = (Var) e1.nextElement();
	    if (!v.isVarUndefined() && !v.isVarLink()) {
		vector.addElement(v.hashKey);
	    }
	}
	return vector;
    }

    /**
     * Tcl_GetFrame -> getFrame
     *
     *	Given a description of a procedure frame, such as the first
     *	argument to an "uplevel" or "upvar" command, locate the
     *	call frame for the appropriate level of procedure.
     *
     *	The return value is 1 if string was either a number or a number
     *  preceded by "#" and it specified a valid frame. 0 is returned
     *  if string isn't one of the two things above (in this case,
     *  the lookup acts as if string were "1"). The frameArr[0] reference
     *  will be filled by the reference of the desired frame (unless an
     *  error occurs, in which case it isn't modified).
     *
     * @param string a string that specifies the level.

     * @exception TclException if s is a valid level specifier but
     *     refers to a bad level that doesn't exist.
     */

    static int getFrame(Interp interp, String string,
			      CallFrame[] frameArr) throws TclException {
	int curLevel, level, result;
	CallFrame frame;

	// Parse string to figure out which level number to go to.
	
	result = 1;
	curLevel = (interp.varFrame == null) ? 0 : interp.varFrame.level;

	if ((string.length() > 0) && (string.charAt(0) == '#')) {
	    level = Util.getInt(interp, string.substring(1));
	    if (level < 0) {
		throw new TclException(interp, "bad level \"" + string + "\"");
	    }
	} else if ((string.length() > 0) && Character.isDigit(string.charAt(0))) {
	    level = Util.getInt(interp, string);
	    level = curLevel - level;
	} else {
	    level = curLevel - 1;
	    result = 0;
	}

	// FIXME: is this a bad comment from some other proc?
	// Figure out which frame to use, and modify the interpreter so
	// its variables come from that frame.
	
	if (level == 0) {
	    frame = null;
	} else {
	    for (frame = interp.varFrame; frame != null;
		 frame = frame.callerVar) {
		if (frame.level == level) {
		    break;
		}
	    }
	    if (frame == null) {
		throw new TclException(interp, "bad level \"" + string + "\"");
	    }
	}
	frameArr[0] = frame;
	return result;
    }


    /**
     * This method is called when this CallFrame is no longer needed.
     * Removes the reference of this object from the interpreter so
     * that this object can be garbage collected.
     * <p>
     * For this procedure to work correctly, it must not be possible
     * for any of the variable in the table to be accessed from Tcl
     * commands (e.g. from trace procedures).
     */

    // FIXME : still need port of DeleteVars?

    protected void dispose() {
	// Unchain this frame from the call stack.

	interp.frame = caller;
	interp.varFrame = callerVar;
	caller = null;
	callerVar = null;

	int flags = TCL.TRACE_UNSETS;
	
	// FIXME : double check this change, what will happend when an interp is destroyed?
	//if (this == interp.globalFrame) {
	if (interp.varFrame == null) {
	    flags |= TCL.INTERP_DESTROYED | TCL.GLOBAL_ONLY;
	}
	for (Enumeration e = varTable.elements(); e.hasMoreElements(); ) {
	    Var var = (Var) e.nextElement();

	    // For global/upvar variables referenced in procedures,
	    // decrement the reference count on the variable referred
	    // to, and free the referenced variable if it's no longer
	    // needed.  Don't delete the hash entry for the other
	    // variable if it's in the same table as us: this will
	    // happen automatically later on.

	    if (var.isVarLink()) {
		Var upvar = (Var) var.value;
		upvar.refCount--;
		if ((upvar.refCount == 0) && upvar.isVarUndefined()
		        && (upvar.traces == null)) {
		    if ((upvar.table != null) && (upvar.table != varTable)) {
			// No need to remove upvar.value because it is already
			// undefined.

			upvar.table.remove(upvar.hashKey);
			upvar.table = null;
		    }
		}
	    }

	    // Invoke traces on the variable that is being deleted, then
	    // free up the variable's space (no need to free the hash entry
	    // here, unless we're dealing with a global variable:  the
	    // hash entries will be deleted automatically when the whole
	    // table is deleted).

	    if (var.traces != null) {
		Var.callTraces(interp, null, var, var.hashKey, null, flags);
	    }

	    if (var.isVarArray()) {
		Var.deleteArray(interp, var.hashKey, var, flags);
	    } else {
		if ((var.value != null) && (var.value instanceof TclObject)) {
		    ((TclObject) var.value).release();
		}
	    }
	    var.value  = null;
	    var.traces = null;
	    var.flags  = Var.UNDEFINED;
	    var.table  = null;
	    var.hashKey= null;
	}
	varTable = null;
    }

}

