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
 * RCS: @(#) $Id: CallFrame.java,v 1.3 1999/06/21 03:32:38 mo Exp $
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

    // The strings below are used to indicate what went wrong when a
    // variable access is denied.

    static final String noSuchVar     =	"no such variable";
    static final String isArray       =	"variable is array";
    static final String needArray     =	"variable isn't array";
    static final String noSuchElement =	"no such element in array";
    static final String danglingElement = 
	"upvar refers to element in deleted array";
    static final String danglingVar = 
	"upvar refers to variable in deleted namespace";
    static final String badNamespace =	"parent namespace doesn't exist";
    static final String missingName =	"missing variable name";


    /**
     * Used in flags for lookupVar(). Indicates that if part1 of the
     * variable doesn't exist, then it should be created.
     *
     * @see CallFrame#lookupVar
     */

    protected static final int CRT_PART1 = 1;

    /**
     * Used in flags for lookupVar(). Indicates that if part2 of the
     * variable doesn't exist, then it should be created.
     *
     * @see CallFrame#lookupVar
     */

    protected static final int CRT_PART2 = 2;

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
	interp	   = i;
	varTable   = new Hashtable();
	caller     = null;
	callerVar  = null;
	objv     = null;
	level    = 0;
    }

    /**
     * Creates a CallFrame. It changes the following (global) variables:
     * <ul>
     *     <li> this.caller
     *	   <li> this.callerVar
     *	   <li> interp.frame
     *	   <li> interp.VarFrame
     * </ul>
     * @param ainterp current interpreter.
     * @param proc the procedure to invoke in this call frame.
     * @param argv the arguments to the procedure.
     * @exception TclException if error occurs in parameter bindings.
     */
    CallFrame(Interp ainterp, Procedure proc, TclObject[] argv)
	    throws TclException {
	interp	 = ainterp;
	varTable = new Hashtable();

	try {
	    chain(proc, argv);
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
    void chain(Procedure proc, TclObject[] argv)
	    throws TclException {
	objv          = argv;
	level         = interp.varFrame.level + 1;
	caller          = interp.frame;
	callerVar       = interp.varFrame;
	interp.frame    = this;
	interp.varFrame = this;

	// parameter bindings
	
	int numArgs = proc.argList.length;

	if ((!proc.isVarArgs) && (argv.length-1 > numArgs)) {
	    throw new TclException(interp, "called \"" + argv[0] +
		    "\" with too many arguments");
	}

	int i, j;
	for (i=0, j=1; i<numArgs; i++, j++) {
	    // Handle the special case of the last formal being
	    // "args".  When it occurs, assign it a list consisting of
	    // all the remaining actual arguments.

	    TclObject varName = proc.argList[i][0];
	    TclObject value = null;

	    if ((i == numArgs-1) && proc.isVarArgs) {
		value = TclList.newInstance();
		value.preserve();
		for (int k=j; k<argv.length; k++) {
		    TclList.append(interp, value, argv[k]);
		}
		interp.setVar(varName, value, 0);
		value.release();
	    }
	    else {
		if (j < argv.length) {
		    value = argv[j];
		} else if (proc.argList[i][1] != null) {
		    value = proc.argList[i][1];
		} else {
		    throw new TclException(interp,
			    "no value given for parameter \""+
		    	    varName + "\" to \"" + argv[0] + "\"");
		}
		interp.setVar(varName, value, 0);
	    }
	}
    }

    /**
     * This procedure is used by virtually all of the variable
     * code to locate a variable given its name(s).
     *
     * @param part1 if part2 isn't NULL, this is the name of an array.
     *      Otherwise, this is a full variable name that could include 
     *      a parenthesized array elemnt or a scalar.
     * @param part2 Name of an element within array, or null.
     * @param flags Only the TCL.GLOBAL_ONLY bit matters.
     * @param msg Verb to use in error messages, e.g.  "read" or "set".
     * @param create OR'ed combination of CRT_PART1 and CRT_PART2.
     *      Tells which entries to create if they don't already exist.
     * @param throwException true if an exception should be throw if the
     *	    variable cannot be found.
     * @return a two element array. a[0] is the variable indicated by
     *      part1 and part2, or null if the variable couldn't be
     *      found and throwException is false.
     *      <p>
     *      If the variable is found, a[1] is the array that
     *      contains the variable (or null if the variable is a scalar).
     *      Note: it's possible that var.value of the returned variable
     *      may be null (variable undefined), even if CRT_PART1 and
     *      CRT_PART2 are specified (these only cause the hash table
     *      entry and/or array to be created).
     * @exception TclException if the variable cannot be found and 
     *      throwException is true.
     *
     */

    Var[] lookupVar(String part1, String part2, int flags, String msg,
	    int create, boolean throwException) throws TclException {
    	Hashtable table;
	Var var;

	// Parse part1 into array name and index.
	// Always check if part1 is an array element name and allow it only if
	// part2 is not given.   
	// (if one does not care about creating array elements that can't be used
	//  from tcl, and prefer slightly better performance, one can put
	//  the following in an   if (part2 == NULL) { ... } block and remove
	//  the part2's test and error reporting  or move that code in array set)

	int len = part1.length();

	if (len > 0) {
	    if (part1.charAt(len-1) == ')') {
		int i = part1.indexOf('(');
		if (i != -1) {
		    if (part2 != null) {
			throw new TclVarException(interp,
				part1, part2, msg, needArray);
		    }
		    char[] n1;
		    char[] n2; 
		    if (i < len-2) {
			n1 = new char[i];
			n2 = new char[len-2-i];
			part1.getChars(0,   i,     n1, 0);
			part1.getChars(i+1, len-1, n2, 0);
			
			part1 = new String(n1);
			part2 = new String(n2);
		    }
		}
	    }
	}

	// Lookup part1. Look it up as either a namespace variable or as a
	// local variable in a procedure call frame (varFramePtr).
	// Interpret part1 as a namespace variable if:
	//    1) so requested by a TCL_GLOBAL_ONLY or TCL_NAMESPACE_ONLY flag,
	//    2) there is no active frame (we're at the global :: scope),
	//    3) the active frame was pushed to define the namespace context
	//       for a "namespace eval" or "namespace inscope" command,
	//    4) the name has namespace qualifiers ("::"s).
	// Otherwise, if part1 is a local variable, search first in the
	// frame's array of compiler-allocated local variables, then in its
	// hashtable for runtime-created local variables.
	//
	// If createPart1 and the variable isn't found, create the variable and,
	// if necessary, create varFramePtr's local var hashtable.

	if ((flags & TCL.GLOBAL_ONLY) != 0) {
	    table = interp.globalFrame.varTable;
	} else {
	    table = interp.varFrame.varTable;
	}

	if ((create & CRT_PART1) != 0) {
	    var = (Var) table.get(part1);
	    if (var == null) {
		var = new Var();
		var.table = table;
		var.hashKey = part1;
		table.put(part1, var);
	    }
	} else {
	    var = (Var) table.get(part1);
	    if (var == null) {
		if (throwException) {
		    throw new TclVarException(interp, 
			    part1, part2, msg, noSuchVar);
		}
		return null;
	    }
	}

	if ((var.flags & Var.UPVAR) != 0) {
	    var = ((Var) var.value);
	}

	if (part2 == null) {
	    Var[] ret = new Var[2];
	    ret[0] = var;
	    ret[1] = null;
	    return ret;
	}

	// We're dealing with an array element, so make sure the variable
	// is an array and lookup the element (create it if desired).

	if ((var.flags & Var.UNDEFINED) != 0) {
	    if ((create & CRT_PART1) == 0) {
		if (throwException) {
		    throw new TclVarException(interp, 
			    part1, part2, msg, noSuchVar);
		}
		return null;
	    }
	    var.flags = Var.ARRAY;
	    var.value = new Hashtable();

	} else if ((var.flags & Var.ARRAY) == 0) {
	    if (throwException) {
		throw new TclVarException(interp, 
		        part1, part2, msg, needArray);
	    }
	    return null;
	}

	Var av = var;
	Hashtable arrayTable = (Hashtable) av.value;
	if ((create & CRT_PART2) != 0) {
	    var = (Var)arrayTable.get(part2);
	    if (var == null) {
		var = new Var();
		var.table = arrayTable;
		var.hashKey = part2;
		arrayTable.put(part2, var);

		// We have added one new element into the array. Remove all
		// outstanding searches.

		var.sidVec = null;
	    }
	} else {
	    var = (Var) arrayTable.get(part2);
	    if (var == null) {
		if (throwException) {
		    throw new TclVarException(interp, 
		            part1, part2, msg, noSuchElement);
		}
		return null;
	    }
	}

	Var[] ret = new Var[2];
	ret[0] = var;
	ret[1] = av;
	return ret;
    }

    /**
     * Set a variable whose name is stored in a Tcl object.
     *
     * @param nameObj name of the variable.
     * @param value the new value for the variable
     * @param flags misc flags that control the actions of this method.
     */

    TclObject setVar(TclObject nameObj, TclObject value, int flags)
	    throws TclException {
	return setVar(nameObj.toString(), null, value, flags);
    }

    /**
     * Set a variable.
     *
     * @param name name of the variable.
     * @param value the new value for the variable
     * @param flags misc flags that control the actions of this method
      */

    TclObject setVar(String name, TclObject value, int flags)
	    throws TclException {
	return setVar(name, null, value, flags);
    }

    /**
     * Set a variable, given a two-part name consisting of array name and
     * element within array..
     *
     * @param part1 1st part of the variable name.
     * @param part2 2nd part of the variable name.
     * @param tobj the new value for the variable
     * @param flags misc flags that control the actions of this method
     *
     * @return the value of the variable after the set operation.
     */

    TclObject setVar(String part1, String part2, TclObject tobj,
	    int flags) throws TclException
    {
	Var[] result = lookupVar(part1, part2, flags, "set",
		CRT_PART1|CRT_PART2, true);
	Var var = result[0];
	Var array = result[1];

	// If the variable's table field is null, it means that this is an
	// upvar to an array element where the array was deleted, leaving
	// the element dangling at the end of the upvar. Generate an error
	// (allowing the variable to be reset would screw up our storage
	// allocation and is meaningless anyway).

	if (var.table == null) {
	    throw new TclVarException(interp, part1, part2, "set",
		    danglingElement);
	}

	if ((var.flags & Var.ARRAY) != 0) {
	    throw new TclVarException(interp, 
		    part1, part2, "set", isArray);
	}
	boolean hasTraces = ((var.traces != null)
	       || ((array != null) && (array.traces != null)));

	try {

	    // Call read trace if variable is being appended to.

	    if (((flags & TCL.APPEND_VALUE) != 0) && hasTraces) {
		String msg = callTraces(array, var, part1, part2,
			(flags & TCL.GLOBAL_ONLY) | TCL.TRACE_READS);
		if (msg != null) {
		    throw new TclVarException(interp, part1, part2, "read",
			    msg);
		}
	    } 

	    TclObject value = (TclObject) var.value;

	    if (value == null) {
		if ((flags & TCL.LIST_ELEMENT) == 0) {
		    value = tobj;
		    value.preserve();
		} else {
		    value = TclList.newInstance();
		    value.preserve();
		    TclList.append(interp, value, tobj);
		}
	    } else if ((flags & TCL.APPEND_VALUE) == 0) {
		if (value != tobj) {
		    // Change to another value.

		    value.release();
		    value = tobj;
		    value.preserve();
		}
	    } else {
		// value != null && (flag & TCL.APPEND_VALUE) != 0

		if ((flags & TCL.LIST_ELEMENT) == 0) {
		    // A string append.

		    value = value.takeExclusive();
		    TclString.append(value, tobj);
		} else {
		    // A list append.

		    value = value.takeExclusive();
		    TclList.append(interp, value, tobj);
		}
	    }

	    // If an array is being set, remove all search IDs associated
	    // with this array UNLESS the array index is already defined.

	    if ((array != null) && (var.flags & Var.UNDEFINED) != 0) {
	        array.sidVec = null;
	    }

	    var.value = value;
	    var.flags &= ~Var.UNDEFINED;

	    // Invoke any write traces for the variable.

	    if (hasTraces) {
		String msg = callTraces(array, var, part1, part2,
		        (flags & TCL.GLOBAL_ONLY) | TCL.TRACE_WRITES);
	        if (msg != null) {
		    throw new TclVarException(interp, part1, part2, "set",
			    msg);
		}
	    }

	    // If the variable was changed in some gross way by a trace (e.g.
	    // it was unset and then recreated as an array) then just return
	    // an empty string;  otherwise return the variable's current
	    // value.

	    if ((var.flags & (Var.UNDEFINED|Var.UPVAR|Var.ARRAY)) == 0) {
		return (TclObject)(var.value);
	    } else {
		return TclString.newInstance("");
	    }
	} finally {
	    // If the variable doesn't exist anymore and no-one's using it,
	    // then free up the relevant structures and hash table entries.

	    if ((var.flags & Var.UNDEFINED) != 0) {
	        cleanupVar(var, array);
	    }
	}
     }

    /**
     * Query the value of a variable whose name is stored in a Tcl object.
     *
     * @param nameObj name of the variable.
     * @param flags misc flags that control the actions of this method.
     * @return the value of the variable.
     */

    TclObject getVar(TclObject nameObj, int flags) 
	    throws TclException {
	return getVar(nameObj.toString(), null, flags);
    }

    /**
     * Query the value of a variable.
     *
     * @param name name of the variable.
     * @param flags misc flags that control the actions of this method.
     * @return the value of the variable.
     */

    TclObject getVar(String name, int flags) 
	    throws TclException {
	return getVar(name, null, flags);
    }

    /**
     * Query the value of a variable, given a two-part name consisting
     * of array name and element within array.
     *
     * @param part1 1st part of the variable name.
     * @param part2 2nd part of the variable name.
     * @param flags misc flags that control the actions of this method.
     * @return the value of the variable.
     */

    TclObject getVar(String part1, String part2, int flags) 
	    throws TclException
    {
	boolean throwException = ((flags & TCL.DONT_THROW_EXCEPTION) == 0);
	Var[] result = lookupVar(part1, part2, flags, "read", CRT_PART2,
		throwException);

	if (result == null) {
	    // lookupVar() returns null only if throwException is true
	    // and the variable cannot be found. We return null to
	    // indicate error.

	    return null;
	}

	Var var = result[0];
	Var array = result[1];

	try {
	    // Invoke any traces that have been set for the variable.

	    if ((var.traces != null)
	            || ((array != null) && (array.traces != null))) {
		String msg = callTraces(array, var, part1, part2,
		        (flags & TCL.GLOBAL_ONLY) | TCL.TRACE_READS);
		if (msg != null) {
		    if (throwException) {
		        throw new TclVarException(interp, part1, part2,
				"read", msg);
		    }
		    return null;
		}
	    }

	    if ((var.flags & (Var.UNDEFINED|Var.UPVAR|Var.ARRAY)) == 0) {
		return (TclObject)var.value;
	    }
	    if (throwException) {
		String msg;

		if (((var.flags & Var.UNDEFINED) != 0) && (array != null)
		        && ((array.flags & Var.UNDEFINED) == 0)) {
		    msg = noSuchElement;
		} else if ((var.flags & Var.ARRAY) != 0) {
		    msg = isArray;
		} else {
		    msg = noSuchVar;
		}
		throw new TclVarException(interp, 
			part1, part2, "read", msg);
	    }
	    return null;
	} finally {
	    // If the variable doesn't exist anymore and no-one's using it,
	    // then free up the relevant structures and hash table entries.

	    if ((var.flags & Var.UNDEFINED) != 0) {
		cleanupVar(var, array);
	    }
	}
    }

    /**
     * Unset a variable whose name is stored in a Tcl object.
     *
     * @param nameObj name of the variable.
     * @param flags misc flags that control the actions of this method.
     */

    void unsetVar(TclObject nameObj, int flags) 
	    throws TclException {
	unsetVar(nameObj.toString(), null, flags);
    }

    /**
     * Unset a variable.
     *
     * @param name name of the variable.
     * @param flags misc flags that control the actions of this method.
     */

    void unsetVar(String name, int flags) 
	    throws TclException {
	unsetVar(name, null, flags);
    }

    /**
     * Unset a variable, given a two-part name consisting of array
     * name and element within array.
     *
     * @param part1 1st part of the variable name.
     * @param part2 2nd part of the variable name.
     * @param flags misc flags that control the actions of this method.
     */

    void unsetVar(String part1, String part2, int flags)
	    throws TclException
    {
	Var[] result = lookupVar(part1, part2, flags, "unset", 0, true);
	Var var = result[0];
	Var array = result[1];
	boolean undefined = ((var.flags & Var.UNDEFINED) != 0);

	if (array != null) {
	    array.sidVec = null;
	}

	// The code below is tricky, because of the possibility that
	// a trace procedure might try to access a variable being
	// deleted.  To handle this situation gracefully, do things
	// in three steps:
	// 1. Copy the contents of the variable to a dummy variable
	//    structure, and mark the original structure as undefined.
	// 2. Invoke traces and clean up the variable, using the copy.
	// 3. If at the end of this the original variable is still
	//    undefined and has no outstanding references, then delete
	//    it (but it could have gotten recreated by a trace).

	if ((var.value != null) && (var.value instanceof TclObject)) {
	    ((TclObject)(var.value)).release();
	    var.value = null;
	}

	Var dummyVar = new Var();
	dummyVar.value    = var.value;
	dummyVar.traces   = var.traces;
	dummyVar.flags    = var.flags;
	dummyVar.hashKey  = var.hashKey;
	dummyVar.table    = var.table;
	dummyVar.refCount = var.refCount;

	var.flags  = Var.UNDEFINED;
	var.traces = null;
	var.value  = null;
	var.sidVec = null;

	// Call trace procedures for the variable being deleted and delete
	// its traces. Be sure to abort any other traces for the variable
	// that are still pending.  Special tricks:
	// 1. Increment var's refCount around this: callTraces() will
	//    use dummyVar so it won't increment var's refCount.
	// 2. Turn off the Var.TRACE_ACTIVE flag in dummyVar: we want to
	//    call unset traces even if other traces are pending.

	if ((dummyVar.traces != null)
	       || ((array != null) && (array.traces != null))) {
	    var.refCount++;
	    dummyVar.flags &= ~Var.TRACE_ACTIVE;
	    callTraces(array, dummyVar, part1, part2,
		    (flags & TCL.GLOBAL_ONLY) | TCL.TRACE_UNSETS);
	    dummyVar.traces = null;
	    var.refCount--;
	}

	// If the variable is an array, delete all of its elements.  This
	// must be done after calling the traces on the array, above (that's
	// the way traces are defined).

	if ((dummyVar.flags & Var.ARRAY) != 0) {
	    deleteArray(part1, dummyVar,
	        (flags & TCL.GLOBAL_ONLY) | TCL.TRACE_UNSETS);
	}

	// Finally, if the variable is truly not in use then free up its
	// record and remove it from the hash table.

	cleanupVar(var, array);

	if (undefined) {
	    throw new TclVarException(interp, part1, part2, "unset", 
		    (array == null) ? noSuchVar : noSuchElement);
	}
    }

    /**
     * Trace a variable whose name is stored in a Tcl object.
     *
     * @param nameObj name of the variable.
     * @param trace the trace to add.
     * @param flags misc flags that control the actions of this method.
     */

    void traceVar(TclObject nameObj, VarTrace trace, int flags) 
	    throws TclException {
	traceVar(nameObj.toString(), null, trace, flags);
    }

    /**
     * Trace a variable.
     *
     * @param name name of the variable.
     * @param trace the trace to add.
     * @param flags misc flags that control the actions of this method.
     */

    void traceVar(String name, VarTrace trace, int flags) 
	    throws TclException {
	traceVar(name, null, trace, flags);
    }

    /**
     * Trace a variable, given a two-part name consisting of array
     * name and element within array.
     *
     * @param part1 1st part of the variable name.
     * @param part2 2nd part of the variable name.
     * @param trace the trace to add.
     * @param flags misc flags that control the actions of this method.
     */

    void traceVar(String part1, String part2, VarTrace trace,int flags)
	    throws TclException
    {
	Var result[], var, array;
	result = lookupVar(part1, part2, flags,	"trace", CRT_PART1|CRT_PART2,
		true);
	var = result[0];
	array = result[1];

	if (var.traces == null) {
	    var.traces = new Vector();
	}
	TraceRecord rec = new TraceRecord();
	rec.flags = flags;
	rec.trace = trace;

	var.traces.insertElementAt(rec, 0);

	// When inserting a trace for an array on an UNDEFINED variable,
	// the search IDs for that array are reset.

	if(array != null && (var.flags & Var.UNDEFINED) != 0) {
	    array.sidVec = null;
	}
    }

    /**
     * Untrace a variable whose name is stored in a Tcl object.
     *
     * @param nameObj name of the variable.
     * @param trace the trace to delete.
     * @param flags misc flags that control the actions of this method.
     */

    void untraceVar(TclObject nameObj, VarTrace trace, int flags) 
    {
	untraceVar(nameObj.toString(), null, trace, flags);
    }

    /**
     * Untrace a variable.
     *
     * @param name name of the variable.
     * @param trace the trace to delete.
     * @param flags misc flags that control the actions of this method.
     */

    void untraceVar(String name, VarTrace trace, int flags) 
    {
	untraceVar(name, null, trace, flags);
    }

    /**
     * Untrace a variable, given a two-part name consisting of array
     * name and element within array.
     *
     * @param part1 1st part of the variable name.
     * @param part2 2nd part of the variable name.
     * @param trace the trace to delete.
     * @param flags misc flags that control the actions of this method.
     */

    void untraceVar(String part1, String part2, VarTrace trace,
	    int flags)
    {
	Var result[], var;

	try {
	    result = lookupVar(part1, part2, flags, "trace",
		    CRT_PART1|CRT_PART2, false);
	    if (result == null) {
		return;
	    }
	} catch (TclException e) {
	    // We have set throwException argument to false in the
	    // lookupVar() call, so an exception should never be
	    // thrown.

	    throw new TclRuntimeError("unexpected TclException: " + e);
	}

	var = result[0];

	if (var.traces != null) {
	    int len = var.traces.size();
	    for (int i=0; i<len; i++) {
		TraceRecord rec = (TraceRecord) (var.traces.elementAt(i));
		if (rec.trace == trace) {
		    var.traces.removeElementAt(i);
		    break;
		}
	    }
	}
    }

    /**
     * @param nameObj name of the variable.
     * @param flags misc flags that control the actions of this method.
     * @return the list of traces of a variable whose name is stored in
     *     a Tcl object.
     */

    protected Vector getTraces(TclObject nameObj, int flags) 
	    throws TclException {
	return getTraces(nameObj.toString(), null, flags);
    }

    /**
     * @param name name of the variable.
     * @param trace the trace to add.
     * @param flags misc flags that control the actions of this method.
     * @return the list of traces of a variable.
     */

    protected Vector getTraces(String name, int flags) 
	    throws TclException {
	return getTraces(name, null, flags);
    }

    /**
     * @return the list of traces of a variable.
     *
     * @param part1 1st part of the variable name.
     * @param part2 2nd part of the variable name.
     * @param trace the trace to add.
     * @param flags misc flags that control the actions of this method.
     */

    protected Vector getTraces(String part1, String part2,int flags)
	    throws TclException {
	Var result[], var;

	flags &= TCL.GLOBAL_ONLY;
	result = lookupVar(part1, part2, flags,	"lookup", 0, false);
	if (result == null) {
	    return null;
	}

	var = result[0];
	return var.traces;
    }

    /**
     * Create a reference of a variable in otherFrame in the current
     * CallFrame. The name of the other variable is stored in a Tcl object.
     *
     * @param frame CallFrame containing "other" variable.
     *     null means use global context.
     * @param otherNameObj name of the variable in the "other" frame.
     * @param myName the name of the var in the current frame.
     * @param flags 0 or TCL.GLOBAL_ONLY: indicates scope of myName.
     * @exception TclException if the upvar cannot be created. 
     */

    protected void makeUpVar(CallFrame frame, TclObject otherNameObj,
	    String myName, int flags) throws TclException {	
	makeUpVar(frame, otherNameObj.toString(), null, myName, flags);
    }

    /**
     * Create a reference of a variable in otherFrame in the current
     * CallFrame.
     *
     * @param frame CallFrame containing "other" variable.
     *     null means use global context.
     * @param otherName name of the variable in the "other" frame.
     * @param myName the name of the var in the current frame.
     * @param flags 0 or TCL.GLOBAL_ONLY: indicates scope of myName.
     * @exception TclException if the upvar cannot be created. 
     */

    protected void makeUpVar(CallFrame frame, String otherName,
	    String myName, int flags) throws TclException {	
	makeUpVar(frame, otherName, null, myName, flags);
    }

    /**
     * Create a reference of a variable in otherFrame in the current
     * CallFrame, given a two-part name consisting of array name and
     * element within array.
     *
     * @param frame CallFrame containing "other" variable.
     *     null means use global context.
     * @param otherP1 the 1st part name of the variable in the "other" frame.
     * @param otherP2 the 2nd part name of the variable in the "other" frame.
     * @param myName the name of the var in the current frame.
     * @param flags only the TCL.GLOBAL_ONLY bit matters, 
     *    indicating the scope of myName.
     * @exception TclException if the upvar cannot be created. 
     */

    protected void makeUpVar(CallFrame frame, String otherP1, String otherP2,
	    String myName, int flags) throws TclException {
	Hashtable table;
	Var result[], other, var;
	CallFrame savedFrame;

	// In order to use LookupVar to find "other", temporarily replace
	// the current frame pointer in the interpreter.

	savedFrame = interp.varFrame;
	interp.varFrame = frame;
	result = lookupVar(otherP1, otherP2, flags,
	    	"access", CRT_PART1|CRT_PART2, true);
	interp.varFrame = savedFrame;
	other = result[0];

	if ((flags & TCL.GLOBAL_ONLY) != 0) {
	    table = interp.globalFrame.varTable;
	} else {
	    table = interp.varFrame.varTable;
	}
	var = (Var) table.get(myName);
	if (var == null) {
	    var = new Var();
	    var.table = table;
	    var.hashKey = myName;
	    table.put(myName, var);
	} else {
	    // The variable already exists.  Make sure that this variable
	    // isn't also "otherVar" (avoid circular links).  Also, if it's
	    // not an upvar then it's an error.  If it is an upvar, then
	    // just disconnect it from the thing it currently refers to.

	    if (var == other) {
		throw new TclException(interp, 
	    		"can't upvar from variable to itself");
	    }
	    if ((var.flags & Var.UPVAR) != 0) {
		Var upvar;

		upvar = (Var) var.value;
		if (upvar == other) {
		    return;
		}
		upvar.refCount--;
		if ((upvar.flags & Var.UNDEFINED) != 0) {
		    cleanupVar(upvar, null);
		}
	    } else if ((var.flags & Var.UNDEFINED) == 0) {
		throw new TclException(interp, "variable \"" + myName +
			"\" already exists");
	    } else if (var.traces != null) {
		throw new TclException(interp, "variable \"" + myName +
			"\" has traces: can't use for upvar");
	    }
	}

	var.flags = (var.flags & ~Var.UNDEFINED) | Var.UPVAR;
	var.value = other;
	other.refCount++;
    }

    /**
     * @param name the name of the variable.
     *
     * @return true if a variable exists and is defined inside this
     *     CallFrame, false otherwise
     */

    boolean exists(String name) {
	try {
	    Var[] result = lookupVar(name, null, 0, "lookup", 0, false);
	    if (result == null) {
		return false;
	    }
	    if ((result[0].flags & Var.UNDEFINED) != 0) {
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
	    Var v = (Var)e1.nextElement();
	    if ((v.flags & Var.UNDEFINED) == 0) {
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
	    Var v = (Var)e1.nextElement();
	    if ((v.flags & (Var.UNDEFINED|Var.UPVAR)) == 0) {
		vector.addElement(v.hashKey);
	    }
	}
	return vector;
    }

    /**
     * Returns a frame given by the level specifier.
     *
     * @param s a string that specifies the level.
     * @return null if s is not a valid level specifier.
     * @exception TclException if s is a valid level specifier but
     *     refers to a bad level that doesn't exist.
     */

    CallFrame getFrame(String s) throws TclException {
	int i = -1;

	if ((s.length() > 0) && (s.charAt(0) == '#')) {
	    String sub = s.substring(1, s.length());
	    int j = Util.getInt(interp, sub);
	    if (j < 0) {
		throw new TclException(interp, "bad level \"" + s + "\"");
	    }
	    i = level - j;
	} else {
	    try {
		i = Util.getInt(interp, s);
	    } catch (TclException e) {
		if (interp.varFrame == interp.globalFrame) {
		    throw new TclException(interp, "bad level \"" + s + "\"");
		}
		return null;
	    }
	}

	if (i < 0) {
	    throw new TclException(interp, "bad level \"" + s + "\"");
	}

	CallFrame frame = interp.varFrame;
	for (int j=0; j<i; j++) {
	    if (frame == interp.globalFrame) {
		// You asked for more levels that you can travel up to.

		throw new TclException(interp, "bad level \"" + s + "\"");
	    } else {
		frame = frame.callerVar;
	    }
	}

	return frame;
    }

    /**
     * This procedure is invoked to find and invoke relevant
     * trace procedures associated with a particular operation on
     * a variable.  This procedure invokes traces both on the
     * variable and on its containing array (where relevant).
     *
     * @param array array variable that contains the variable, or null
     *   if the variable isn't an element of an array.
     * @param var Variable whose traces are to be invoked.
     * @param part1 the first part of a variable name.
     * @param part2 the second part of a variable name.
     * @param flags Flags to pass to trace procedures: indicates
     *   what's happening to variable, plus other stuff like
     *   TCL.GLOBAL_ONLY and TCL.INTERP_DESTROYED.
     * @return null if no trace procedures were invoked, or
     *   if all the invoked trace procedures returned successfully.
     *   The return value is non-null if a trace procedure returned an
     *   error (in this case no more trace procedures were invoked
     *   after the error was returned). In this case the return value
     *   is a pointer to a string describing the error.
     */

    protected String callTraces(Var array, Var var, String part1,
	    String part2, int flags) {

	// If there are already similar trace procedures active for the
	// variable, don't call them again.

	if ((var.flags & Var.TRACE_ACTIVE) != 0) {
	    return null;
	}
	var.flags |= Var.TRACE_ACTIVE;
	var.refCount++;

	// If the variable name hasn't been parsed into array name and
	// element, do it here.  If there really is an array element,
	// make a copy of the original name so that NULLs can be
	// inserted into it to separate the names (can't modify the name
	// string in place, because the string might get used by the
	// callbacks we invoke).

	if (part2 == null) {
	    int len = part1.length();

	    if (len > 0) {
		if (part1.charAt(len-1) == ')') {
		    int i;
		    for (i=0; i<len-1; i++) {
			if (part1.charAt(i) == '(') {
			    break;
			}
		    }
		    if (i < len-1) {
			char[] n1;
			char[] n2;

			if (i < len-2) {
			    n1 = new char[i];
			    n2 = new char[len-2-i];
			    part1.getChars(0,   i,     n1, 0);
			    part1.getChars(i+1, len-1, n2, 0);

			    part1 = new String(n1);
			    part2 = new String(n2);
			}
		    }
		}
	    }
	}

	TclObject oldResult;
	int i;

	oldResult = interp.getResult();
	oldResult.preserve();
	interp.resetResult();

	try {
	    // Invoke traces on the array containing the variable, if relevant.

	    if (array != null) {
		array.refCount ++;
	    }
	    if ((array != null) && (array.traces != null)) {
		for (i=0; (array.traces != null) && (i<array.traces.size());
			i++) {
		    TraceRecord rec = (TraceRecord)(array.traces.elementAt(i));
		    if ((rec.flags & flags) != 0) {
			try {
			    rec.trace.traceProc(interp, part1, part2, flags);
			} catch (TclException e) {
			    if ((flags & TCL.TRACE_UNSETS) == 0) {
				return interp.getResult().toString();
			    }
			}
		    }
		}
	    }

	    // Invoke traces on the variable itself.

	    if ((flags & TCL.TRACE_UNSETS) != 0) {
		flags |= TCL.TRACE_DESTROYED;
	    }

	    for (i=0; (var.traces != null) && (i<var.traces.size()); i++) {
		TraceRecord rec = (TraceRecord)(var.traces.elementAt(i));
		if ((rec.flags & flags) != 0) {
		    try {
			rec.trace.traceProc(interp, part1, part2, flags);
		    } catch (TclException e) {
			if ((flags & TCL.TRACE_UNSETS) == 0) {
			    return interp.getResult().toString();
			}
		    }
		}
	    }

	    return null;
	} finally {
	    if (array != null) {
		array.refCount--;
	    }
	    var.flags &= ~Var.TRACE_ACTIVE;
	    var.refCount--;

	    interp.setResult(oldResult);
	    oldResult.release();
	}
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

    protected void dispose() {
	// Unchain this frame from the call stack.

	interp.frame = caller;
	interp.varFrame = callerVar;
	caller = null;
	callerVar = null;

	int flags = TCL.TRACE_UNSETS;

	if (this == interp.globalFrame) {
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

	    if ((var.flags & Var.UPVAR) != 0) {
		Var upvar = (Var)(var.value);
		upvar.refCount--;
		if ((upvar.refCount == 0) && ((upvar.flags & Var.UNDEFINED)!=0)
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
		callTraces(null, var, var.hashKey, null, flags);
	    }

	    if ((var.flags & Var.ARRAY) != 0) {
		deleteArray(var.hashKey, var, flags);
	    } else {
		if ((var.value != null) && (var.value instanceof TclObject)) {
		    ((TclObject)var.value).release();
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

    /**
     * This procedure is called to free up everything in an array
     * variable.  It's the caller's responsibility to make sure
     * that the array is no longer accessible before this procedure
     * is called.
     *
     * @param arrayName name of array (used for trace callbacks).
     * @param var the array variable to delete.
     * @param flags Flags to pass to CallTraces: TCL.TRACE_UNSETS and
     *   sometimes TCL.INTERP_DESTROYED and/or TCL.GLOBAL_ONLY.
     */

    protected void deleteArray(String arrayName, Var var, int flags) {
	var.sidVec = null;
	Hashtable table = (Hashtable) var.value;

	Var dummyVar = null;
	for (Enumeration e1 = table.elements();
		e1.hasMoreElements(); ) {
	    Var el = (Var) e1.nextElement();
	    TclObject value = (TclObject)el.value;
	    if (value != null) {
		value.release();
	    }
	    el.table = null;
	    if (el.traces != null) {
		if (dummyVar == null) {
		    dummyVar = new Var();
		}
		dummyVar.traces = el.traces;
		dummyVar.flags  = el.flags;
		dummyVar.flags &= ~Var.TRACE_ACTIVE;
		el.traces = null;
		el.refCount ++;
		callTraces(null, dummyVar, arrayName, el.hashKey, flags);
		el.refCount --;
	    }
	    el.flags = Var.UNDEFINED;
	}
	var.value = null;
    }

    /**
     * This procedure is called when it looks like it may be OK
     * to free up the variable's record and hash table entry, and
     * those of its containing parent.  It's called, for example,
     * when a trace on a variable deletes the variable.
     *
     * @param var variable that may be a candidate for being expunged.
     * @param array Array that contains the variable, or NULL if this
     *   variable isn't an array element.
     */

    protected void cleanupVar(Var var, Var array) {
	if (((var.flags & Var.UNDEFINED) != 0) && (var.refCount == 0)
	        && (var.traces == null)) {
	    if (var.table != null) {
		var.table.remove(var.hashKey);
		var.table = null;
	    }
	}
	if (array != null) {
	    if (((array.flags & Var.UNDEFINED) != 0) && (array.refCount == 0)
		    && (array.traces == null)) {
		if (array.table != null) {
		    array.table.remove(array.hashKey);
		    array.table = null;
		}
	    }
	}
    }
}

