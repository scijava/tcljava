/*
 * Var.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Var.java,v 1.11 2003/01/09 02:15:39 mdejong Exp $
 *
 */
package tcl.lang;

import java.util.*;

/*
 * Implements variables in Tcl. The Var class encapsulates most of the functionality
 * of the methods in generic/tclVar.c and the structure Tcl_Var from the C version.
 */

class Var {

    /**
     * Flag bits for variables. The first three (SCALAR, ARRAY, and
     * LINK) are mutually exclusive and give the "type" of the variable.
     * UNDEFINED is independent of the variable's type. 
     *
     * SCALAR -			1 means this is a scalar variable and not
     *				an array or link. The value field points
     *				to the variable's value, a Tcl object.
     * ARRAY -			1 means this is an array variable rather
     *				than a scalar variable or link. The
     *				table field points to the array's
     *				hashtable for its elements.
     * LINK -			1 means this Var structure contains a
     *				reference to another Var structure that
     *				either has the real value or is itself
     *				another LINK pointer. Variables like
     *				this come about through "upvar" and "global"
     *				commands, or through references to variables
     *				in enclosing namespaces.
     * UNDEFINED -		1 means that the variable is in the process
     *				of being deleted. An undefined variable
     *				logically does not exist and survives only
     *				while it has a trace, or if it is a global
     *				variable currently being used by some
     *				procedure.
     * IN_HASHTABLE -		1 means this variable is in a hashtable. 0 if
     *				a local variable that was assigned a slot
     *				in a procedure frame by	the compiler so the
     *				Var storage is part of the call frame.
     * TRACE_ACTIVE -		1 means that trace processing is currently
     *				underway for a read or write access, so
     *				new read or write accesses should not cause
     *				trace procedures to be called and the
     *				variable can't be deleted.
     * ARRAY_ELEMENT -		1 means that this variable is an array
     *				element, so it is not legal for it to be
     *				an array itself (the ARRAY flag had
     *				better not be set).
     * NAMESPACE_VAR -		1 means that this variable was declared
     *				as a namespace variable. This flag ensures
     *				it persists until its namespace is
     *				destroyed or until the variable is unset;
     *				it will persist even if it has not been
     *				initialized and is marked undefined.
     *				The variable's refCount is incremented to
     *				reflect the "reference" from its namespace.
     *
     */
    
    static final int SCALAR	   = 0x1;
    static final int ARRAY	   = 0x2;
    static final int LINK	   = 0x4;
    static final int UNDEFINED	   = 0x8;
    static final int IN_HASHTABLE  = 0x10;
    static final int TRACE_ACTIVE  = 0x20;
    static final int ARRAY_ELEMENT = 0x40;
    static final int NAMESPACE_VAR = 0x80;

    // Methods to read various flag bits of variables.

    final boolean isVarScalar() {
	return ((flags & SCALAR) != 0);
    }

    final boolean isVarLink() {
	return ((flags & LINK) != 0);
    }

    final boolean isVarArray() {
	return ((flags & ARRAY) != 0);
    }

    final boolean isVarUndefined() {
	return ((flags & UNDEFINED) != 0);
    }

    final boolean isVarArrayElement() {
	return ((flags & ARRAY_ELEMENT) != 0);
    }

    // Methods to ensure that various flag bits are set properly for variables.

    final void setVarScalar() {
	flags = (flags & ~(ARRAY|LINK)) | SCALAR;
    }

    final void setVarArray() {
	flags = (flags & ~(SCALAR|LINK)) | ARRAY;
    }

    final void setVarLink() {
	flags = (flags & ~(SCALAR|ARRAY)) | LINK;
    }

    final void setVarArrayElement() {
	flags = (flags & ~ARRAY) | ARRAY_ELEMENT;
    }

    final void setVarUndefined() {
	flags |= UNDEFINED;
    }

    final void clearVarUndefined() {
	flags &= ~UNDEFINED;
    }

    /**
     * Stores the "value" of the variable. It stored different information
     * depending on the type of the variable: <ul>
     *  <li>Scalar variable - (TclObject) value is the object stored in the
     *	   variable.
     *	<li> Array variable - (Hashtable) value is the hashtable that stores
     *     all the elements. <p>
     *  <li> Upvar (Link) - (Var) value is the variable associated by this upvar.
     * </ul>
     */

    Object value;

    /**
     * Vector that holds the traces that were placed in this Var
     */

    Vector traces;


    Vector sidVec;

    /**
     * Miscellaneous bits of information about variable.
     *
     * @see Var#SCALAR
     * @see Var#ARRAY
     * @see Var#LINK
     * @see Var#UNDEFINED
     * @see Var#IN_HASHTABLE
     * @see Var#TRACE_ACTIVE
     * @see Var#ARRAY_ELEMENT
     * @see Var#NAMESPACE_VAR
     */       

    int flags;

    /**
     * If variable is in a hashtable, either the
     * hash table entry that refers to this
     * variable or null if the variable has been
     * detached from its hash table (e.g. an
     * array is deleted, but some of its
     * elements are still referred to in
     * upvars). null if the variable is not in a
     * hashtable. This is used to delete an
     * variable from its hashtable if it is no
     * longer needed.
     */

    Hashtable table;

    /**
     * The key under which this variable is stored in the hash table.
     */

    String hashKey;

    /**
     * Counts number of active uses of this
     * variable, not including its entry in the
     * call frame or the hash table: 1 for each
     * additional variable whose link points
     * here, 1 for each nested trace active on
     * variable, and 1 if the variable is a 
     * namespace variable. This record can't be
     * deleted until refCount becomes 0.
     */

    int refCount;

    /**
     * Reference to the namespace that contains
     * this variable or null if the variable is
     * a local variable in a Tcl procedure.
     */

    NamespaceCmd.Namespace ns;

    /**
     * NewVar -> Var
     * 
     * Construct a variable and initialize its fields.
     */

    Var() {
	value    = null;
	//name     = null; // Like hashKey in Jacl
	ns       = null;
	hashKey  = null;  // Like hPtr in the C implementation
	table    = null;  // Like hPtr in the C implementation
	refCount = 0;
	traces   = null;
	//search   = null;
	sidVec   = null; // Like search in the C implementation
	flags    = (SCALAR | UNDEFINED | IN_HASHTABLE);
    }

    /**
     * Used to create a String that describes this variable
     *
     */

    public String toString() {
	StringBuffer sb = new StringBuffer();
	sb.append(ns);
	if (sb.length() == 2) {
	    // It is in the global namespace
	    sb.append(hashKey);
	} else {
	    // It is not in the global namespaces
	     sb.append("::");
	     sb.append(hashKey);
	}
	return sb.toString();
    }

    /**
     * Used by ArrayCmd to create a unique searchId string.  If the 
     * sidVec Vector is empty then simply return 1.  Else return 1 
     * plus the SearchId.index value of the last Object in the vector.
     * 
     * @param None
     * @return The int value for unique SearchId string.
     */

    protected synchronized int getNextIndex() {
        if (sidVec.size() == 0) {
	    return 1;
	}
	SearchId sid = (SearchId) sidVec.lastElement();
	return (sid.getIndex()+1);
    }

    /**
     * Find the SearchId that in the sidVec Vector that is equal the 
     * unique String s and returns the enumeration associated with
     * that SearchId.
     *
     * @param s String that ia a unique identifier for a SearchId object
     * @return Enumeration if a match is found else null.
     */

    protected Enumeration getSearch(String s) {
        SearchId sid;
        for(int i=0; i < sidVec.size(); i++) {
	    sid = (SearchId) sidVec.elementAt(i);
	    if (sid.equals(s)){
	        return sid.getEnum();
	    }
	}
	return null;
     }
        

    /**
     * Find the SearchId object in the sidVec Vector and remove it.
     *
     * @param sid String that ia a unique identifier for a SearchId object.
     */

    protected boolean removeSearch(String sid) {
        SearchId curSid;

        for(int i=0; i < sidVec.size(); i++) {
	    curSid = (SearchId) sidVec.elementAt(i);
	    if (curSid.equals(sid)){
	        sidVec.removeElementAt(i);
		return true;
	    }
	} 
	return false;
    }





    // End of the instance method for the Var class, the rest of the methods
    // are Var related methods ported from the code in generic/tclVar.c


    
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
     * TclLookupVar -> lookupVar
     *
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
     *      If the variable can't be found and either createPart1 or
     *      createPart2 are true, a new as-yet-undefined (VAR_UNDEFINED)
     *      variable instance is created, entered into a hash
     *      table, and returned.
     *      Note: it's possible that var.value of the returned variable
     *      may be null (variable undefined), even if createPart1 or createPart2
     *      are true (these only cause the hash table entry or array to be created).
     *      For example, the variable might be a global that has been unset but
     *      is still referenced by a procedure, or a variable that has been unset
     *      but it only being kept in existence by a trace.
     * @exception TclException if the variable cannot be found and 
     *      throwException is true.
     *
     */

    static Var[] lookupVar(
	Interp interp,          // Interpreter to use for lookup.
	String part1,           // If part2 isn't null, this is the name of
                                // an array. Otherwise, this
                                // is a full variable name that could
                                // include a parenthesized array element.
	String part2,           // Name of element within array, or null.
	int flags,              // Only TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY,
                                // and TCL.LEAVE_ERR_MSG bits matter.
	String msg,             // Verb to use in error messages, e.g.
                                // "read" or "set". Only needed if
				// TCL.LEAVE_ERR_MSG is set in flags.
	boolean createPart1,    // If true, create hash table entry for part 1
                                // of name, if it doesn't already exist. If
                                // false, return error if it doesn't exist.
	boolean createPart2     // If true, create hash table entry for part 2
				// of name, if it doesn't already exist. If
				// false, throw exception if it doesn't exist.
	)
	throws TclException
    {
	CallFrame varFrame = interp.varFrame;
				// Reference to the procedure call frame whose
				// variables are currently in use. Same as
				// the current procedure's frame, if any,
				// unless an "uplevel" is executing.
	Hashtable table;        //  to the hashtable, if any, in which
				// to look up the variable.
	Var var;                // Used to search for global names.
	String elName;          // Name of array element or null.
	int openParen;
                                // If this procedure parses a name into
				// array and index, these point to the
				// parens around the index.  Otherwise they
				// are -1. These are needed to restore
				// the parens after parsing the name.
	NamespaceCmd.Namespace varNs, cxtNs;
	Interp.ResolverScheme res;
	int p;
	int i, result;

	var = null;
	openParen = -1;
	varNs = null;		// set non-null if a nonlocal variable

	// Parse part1 into array name and index.
	// Always check if part1 is an array element name and allow it only if
	// part2 is not given.   
	// (if one does not care about creating array elements that can't be used
	// from tcl, and prefer slightly better performance, one can put
	// the following in an   if (part2 == null) { ... } block and remove
	// the part2's test and error reporting  or move that code in array set)
	elName = part2;
	int len = part1.length();
	for (p = 0; p < len ; p++) {
	    if (part1.charAt(p) == '(') {
		openParen = p;
		p = len - 1;
		if (part1.charAt(p) == ')') {
		    if (part2 != null) {
			if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
			    throw new TclVarException(interp,
			        part1, part2, msg, needArray);
			}
			return null;
		    }
		    elName = part1.substring(openParen+1, len - 1);
		    part2 = elName; // same as elName, only used in error reporting
		    part1  = part1.substring(0, openParen);
		}
		break;
	    }
	}



	// If this namespace has a variable resolver, then give it first
	// crack at the variable resolution.  It may return a Var
	// value, it may signal to continue onward, or it may signal
	// an error.

	if (((flags & TCL.GLOBAL_ONLY) != 0) || (interp.varFrame == null)) {
	    cxtNs = interp.globalNs;
	} else {
	    cxtNs = interp.varFrame.ns;
	}

	if (cxtNs.resolver != null || interp.resolvers != null) {
	    try {
		if (cxtNs.resolver != null) {
		    var = cxtNs.resolver.resolveVar(interp,
			      part1, cxtNs, flags);
		} else {
		    var = null;
		}

		if (var == null && interp.resolvers != null) {
		    Enumeration enum = interp.resolvers.elements();
		    while (var == null && enum.hasMoreElements()) {
			res = (Interp.ResolverScheme) enum.nextElement();
			var = res.resolver.resolveVar(interp,
				  part1, cxtNs, flags);
		    }
		}
	    } catch (TclException e) {
		var = null;
	    }
 	}

	// Look up part1. Look it up as either a namespace variable or as a
	// local variable in a procedure call frame (varFrame).
	// Interpret part1 as a namespace variable if:
	//    1) so requested by a TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY flag,
	//    2) there is no active frame (we're at the global :: scope),
	//    3) the active frame was pushed to define the namespace context
	//       for a "namespace eval" or "namespace inscope" command,
	//    4) the name has namespace qualifiers ("::"s).
	// Otherwise, if part1 is a local variable, search first in the
	// frame's array of compiler-allocated local variables, then in its
	// hashtable for runtime-created local variables.
	//
	// If createPart1 and the variable isn't found, create the variable and,
	// if necessary, create varFrame's local var hashtable.
	
	if (((flags & (TCL.GLOBAL_ONLY | TCL.NAMESPACE_ONLY)) != 0)
	    || (varFrame == null)
	    || !varFrame.isProcCallFrame
	    || (part1.indexOf("::") != -1)) {
	    String tail;

	    // Don't pass TCL.LEAVE_ERR_MSG, we may yet create the variable,
	    // or otherwise generate our own error!

	    var = NamespaceCmd.findNamespaceVar(interp, part1, null,
						flags & ~TCL.LEAVE_ERR_MSG);
	    if (var == null) {
		if (createPart1) {   // var wasn't found so create it

		    // Java does not support passing an address so we pass
		    // an array of size 1 and then assign arr[0] to the value
		    NamespaceCmd.Namespace[] varNsArr  = new NamespaceCmd.Namespace[1];
		    NamespaceCmd.Namespace[] dummyArr = new NamespaceCmd.Namespace[1];
		    String[]    tailArr   = new String[1];

		    NamespaceCmd.getNamespaceForQualName(interp, part1, null,
		       flags, varNsArr, dummyArr, dummyArr, tailArr);

		    // Get the values out of the arrays!
		    varNs  = varNsArr[0];
		    tail   = tailArr[0];

		    if (varNs == null) {
			if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
			    throw new TclVarException(interp,
			        part1, part2, msg, badNamespace);
			}
			return null;
		    }
		    if (tail == null) {
			if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
			    throw new TclVarException(interp,
			        part1, part2, msg, missingName);
			}
			return null;
		    }
		    var = new Var();
		    varNs.varTable.put(tail, var);

		    // There is no hPtr member in Jacl, The hPtr combines the table
		    // and the key used in a table lookup.
		    var.hashKey = tail;
		    var.table   = varNs.varTable;

		    var.ns = varNs;
		} else {		// var wasn't found and not to create it
		    if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
			throw new TclVarException(interp,
			        part1, part2, msg, noSuchVar);
		    }
		    return null;
		}
	    }
	} else {			// local var: look in frame varFrame
	    // removed code block that searches for local compiled vars

	    if (var == null) {	// look in the frame's var hash table
		table = varFrame.varTable;
		if (createPart1) {
		    if (table == null) {
			table = new Hashtable();
			varFrame.varTable = table;
		    }
		    var = (Var) table.get(part1);
		    if (var == null) { // we are adding a new entry
			var = new Var();
			table.put(part1, var);

		        // There is no hPtr member in Jacl, The hPtr combines
			// the table and the key used in a table lookup.
			var.hashKey = part1;
			var.table   = table;

			var.ns = null; // a local variable
		    }
		} else {
		    if (table != null) {
			var = (Var) table.get(part1);
		    }
		    if (var == null) {
			if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
			    throw new TclVarException(interp,
			        part1, part2, msg, noSuchVar);
			}
			return null;
		    }
		}
	    }
	}

	// If var is a link variable, we have a reference to some variable
	// that was created through an "upvar" or "global" command. Traverse
	// through any links until we find the referenced variable.
	
	while (var.isVarLink()) {
	    var = (Var) var.value;
	}

	// If we're not dealing with an array element, return var.
    
	if (elName == null) {
	    Var[] ret = new Var[2];
	    ret[0] = var;
	    ret[1] = null;
	    return ret;
	}

	// We're dealing with an array element. Make sure the variable is an
	// array and look up the element (create the element if desired).
	
	if (var.isVarUndefined() && !var.isVarArrayElement()) {
	    if (!createPart1) {
		if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
		    throw new TclVarException(interp,
		        part1, part2, msg, noSuchVar);
		}
		return null;
	    }

	    // Make sure we are not resurrecting a namespace variable from a
	    // deleted namespace!

	    if (((var.flags & IN_HASHTABLE) != 0) && (var.table == null)) {
		if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
		    throw new TclVarException(interp,
		        part1, part2, msg, danglingVar);
		}
		return null;
	    }

	    var.setVarArray();
	    var.clearVarUndefined();
	    var.value = new Hashtable();
	} else if (!var.isVarArray()) {
	    if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
		throw new TclVarException(interp,
		    part1, part2, msg, needArray);
	    }
	    return null;
	}
	
	Var arrayVar = var;
	Hashtable arrayTable = (Hashtable) var.value;
	if (createPart2) {
	    Var searchvar = (Var) arrayTable.get(elName);

	    if (searchvar == null) { // new entry
		if (var.sidVec != null) {
		    deleteSearches(var);
		}

		var = new Var();
		arrayTable.put(elName, var);

		// There is no hPtr member in Jacl, The hPtr combines the table
		// and the key used in a table lookup.
		var.hashKey = elName;
		var.table   = arrayTable;

		var.ns = varNs;
		var.setVarArrayElement();
	    } else {
		var = searchvar;
	    }
	} else {
	    var = (Var) arrayTable.get(elName);
	    if (var == null) {
		if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
		    throw new TclVarException(interp,
		        part1, part2, msg, noSuchElement);
		}
		return null;
	    }
	}

	Var[] ret = new Var[2];
	ret[0] = var;      // The Var in the array
	ret[1] = arrayVar; // The array (Hashtable) Var
	return ret;
    }


    /**
     * Query the value of a variable whose name is stored in a Tcl object.
     *
     * @param interp the interp that holds the variable
     * @param nameObj name of the variable.
     * @param flags misc flags that control the actions of this method.
     * @return the value of the variable.
     */

    static TclObject getVar(Interp interp, TclObject nameObj, int flags) 
	    throws TclException {
	return getVar(interp, nameObj.toString(), null, flags);
    }

    /**
     * Query the value of a variable.
     *
     * @param interp the interp that holds the variable
     * @param name name of the variable.
     * @param flags misc flags that control the actions of this method.
     * @return the value of the variable.
     */

    static TclObject getVar(Interp interp, String name, int flags) 
	    throws TclException {
	return getVar(interp, name, null, flags);
    }

    /**
     * Tcl_ObjGetVar2 -> getVar
     *
     * Query the value of a variable.
     *
     * @param interp the interp that holds the variable
     * @param part1 1st part of the variable name.
     * @param part2 2nd part of the variable name.
     * @param flags misc flags that control the actions of this method.
     * @return the value of the variable.
     */

    static TclObject getVar(Interp interp, TclObject part1Obj,
			                   TclObject part2Obj, int flags)
 	throws TclException
    {
	String part1, part2;

	part1 = part1Obj.toString();

	if (part2Obj != null) {
	    part2 = part2Obj.toString();
	} else {
	    part2 = null;
	}

	return getVar(interp, part1, part2, flags);
    }

    /**
     * Tcl_GetVar2Ex -> getVar
     *
     * Query the value of a variable, given a two-part name consisting
     * of array name and element within array.
     *
     * @param interp the interp that holds the variable
     * @param part1 1st part of the variable name.
     * @param part2 2nd part of the variable name.
     * @param flags misc flags that control the actions of this method.
     * @return the value of the variable.
     */

    static TclObject getVar(
        Interp interp,       // interpreter to look for the var in
	String part1,        // Name of an array (if part2 is non-null)
	                     // or the name of a variable.
	String part2,        // If non-null, gives the name of an element
                             // in the array part1.
	int flags            // OR-ed combination of TCL.GLOBAL_ONLY,
                             // and TCL.LEAVE_ERR_MSG bits.
	)
	throws TclException
    {
	Var[] result = lookupVar(interp, part1, part2, flags, "read",
				 false, true);
	
	if (result == null) {
	    // lookupVar() returns null only if TCL.LEAVE_ERR_MSG is
	    // not part of the flags argument, return null in this case.

	    return null;
	}

	Var var = result[0];
	Var array = result[1];

	try {
	    // Invoke any traces that have been set for the variable.

	    if ((var.traces != null)
		|| ((array != null) && (array.traces != null))) {
		String msg = callTraces(interp, array, var, part1, part2,
		    (flags & (TCL.NAMESPACE_ONLY|TCL.GLOBAL_ONLY)) | TCL.TRACE_READS);
		if (msg != null) {
		    if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
			throw new TclVarException(interp, part1, part2,
				"read", msg);
		    }
		    return null;
		}
	    }

	    if (var.isVarScalar() && !var.isVarUndefined()) {
		return (TclObject) var.value;
	    }

	    if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
		String msg;
		if (var.isVarUndefined() && (array != null)
		    && !array.isVarUndefined()) {
		    msg = noSuchElement;
		} else if (var.isVarArray()) {
		    msg = isArray;
		} else {
		    msg = noSuchVar;
		}
		throw new TclVarException(interp, part1, part2, "read", msg);
	    }
	} finally {
	    // If the variable doesn't exist anymore and no-one's using it,
	    // then free up the relevant structures and hash table entries.

	    if (var.isVarUndefined()) {
		cleanupVar(var, array);
	    }
	}

	return null;
    }
    
    /**
     * Set a variable whose name is stored in a Tcl object.
     *
     * @param interp the interp that holds the variable
     * @param nameObj name of the variable.
     * @param value the new value for the variable
     * @param flags misc flags that control the actions of this method.
     */

    static TclObject setVar(Interp interp, TclObject nameObj, TclObject value, int flags)
	    throws TclException {
	return setVar(interp, nameObj.toString(), null, value, flags);
    }

    /**
     * Set a variable.
     *
     * @param interp the interp that holds the variable
     * @param name name of the variable.
     * @param value the new value for the variable
     * @param flags misc flags that control the actions of this method
      */

    static TclObject setVar(Interp interp, String name, TclObject value, int flags)
	    throws TclException {
	return setVar(interp, name, null, value, flags);
    }

    /**
     * Tcl_ObjSetVar2 -> setVar
     *
     * Set the value of a variable.
     *
     * @param interp the interp that holds the variable
     * @param part1 1st part of the variable name.
     * @param part2 2nd part of the variable name.
     * @param newValue the new value for the variable
     * @param flags misc flags that control the actions of this method
     */

    static TclObject setVar(Interp interp, TclObject part1Obj,
			                   TclObject part2Obj,
			                   TclObject newValue, int flags)
 	throws TclException
    {
	String part1, part2;

	part1 = part1Obj.toString();

	if (part2Obj != null) {
	    part2 = part2Obj.toString();
	} else {
	    part2 = null;
	}

	return setVar(interp, part1, part2, newValue, flags);
    }


    /**
     * Tcl_SetVar2Ex -> setVar
     *
     *	Given a two-part variable name, which may refer either to a scalar
     *	variable or an element of an array, change the value of the variable
     *	to a new Tcl object value. If the named scalar or array or element
     *	doesn't exist then create one.
     *
     * @param interp the interp that holds the variable
     * @param part1 1st part of the variable name.
     * @param part2 2nd part of the variable name.
     * @param newValue the new value for the variable
     * @param flags misc flags that control the actions of this method
     *
     *	Returns a pointer to the TclObject holding the new value of the
     *	variable. If the write operation was disallowed because an array was
     *	expected but not found (or vice versa), then null is returned; if
     *	the TCL.LEAVE_ERR_MSG flag is set, then an exception will be raised.
     *  Note that the returned object may not be the same one referenced
     *  by newValue because variable traces may modify the variable's value.
     *	The value of the given variable is set. If either the array or the
     *	entry didn't exist then a new variable is created.
     *
     *	The reference count is decremented for any old value of the variable
     *	and incremented for its new value. If the new value for the variable
     *	is not the same one referenced by newValue (perhaps as a result
     *	of a variable trace), then newValue's ref count is left unchanged
     *	by Tcl_SetVar2Ex. newValue's ref count is also left unchanged if
     *	we are appending it as a string value: that is, if "flags" includes
     *	TCL.APPEND_VALUE but not TCL.LIST_ELEMENT.
     *
     *	The reference count for the returned object is _not_ incremented: if
     *	you want to keep a reference to the object you must increment its
     *	ref count yourself.
     */

    static TclObject setVar(
	    Interp interp,      // interp to search for the var in
	    String part1,       // Name of an array (if part2 is non-null)
	                        // or the name of a variable.
	    String part2,       // If non-null, gives the name of an element
				// in the array part1.
	    TclObject newValue, // New value for variable.
	    int flags           // Various flags that tell how to set value:
				// any of TCL.GLOBAL_ONLY,
				// TCL.NAMESPACE_ONLY, TCL.APPEND_VALUE,
				// TCL.LIST_ELEMENT or TCL.LEAVE_ERR_MSG.
	    )
	throws TclException
    {
	Var var;
	Var array;
	TclObject oldValue;
	String bytes;

	Var[] result = lookupVar(interp, part1, part2, flags, "set",
				 true, true);
	if (result == null) {
	    return null;
	}

	var   = result[0];
	array = result[1];

	// If the variable is in a hashtable and its table field is null, then we
	// may have an upvar to an array element where the array was deleted
	// or an upvar to a namespace variable whose namespace was deleted.
	// Generate an error (allowing the variable to be reset would screw up
	// our storage allocation and is meaningless anyway).

	if (((var.flags & IN_HASHTABLE) != 0) && (var.table == null)) {
	    if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
		if (var.isVarArrayElement()) {
		    throw new TclVarException(interp, part1, part2, "set",
					      danglingElement);
		} else {
		    throw new TclVarException(interp, part1, part2, "set",
					      danglingVar);
		}
	    }
	    return null;
	}

	// It's an error to try to set an array variable itself.
	
	if (var.isVarArray() && !var.isVarUndefined()) {
	    if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
		throw new TclVarException(interp, part1, part2, "set",
					      isArray);
	    }
	    return null;
	}


	// At this point, if we were appending, we used to call read traces: we
	// treated append as a read-modify-write. However, it seemed unlikely to
	// us that a real program would be interested in such reads being done
	// during a set operation.

	// Set the variable's new value. If appending, append the new value to
	// the variable, either as a list element or as a string. Also, if
	// appending, then if the variable's old value is unshared we can modify
	// it directly, otherwise we must create a new copy to modify: this is
	// "copy on write".

	try {
	    oldValue = (TclObject) var.value;
	    
	    if ((flags & TCL.APPEND_VALUE) != 0) {
		if (var.isVarUndefined() && (oldValue != null)) {
		    oldValue.release();          // discard old value
		    var.value = null;
		    oldValue  = null;
		}
		if ((flags & TCL.LIST_ELEMENT) != 0) {	// append list element
		    if (oldValue == null) {
			oldValue = TclList.newInstance();
			var.value = oldValue;
			oldValue.preserve(); // since var is referenced
		    } else if (oldValue.isShared()) { // append to copy
			var.value = oldValue.duplicate();
			oldValue.release();
			oldValue = (TclObject) var.value;
			oldValue.preserve(); // since var is referenced
		    }
		    TclList.append(interp, oldValue, newValue);
		} else {		               // append string
		    // We append newValuePtr's bytes but don't change its ref count.

		    bytes = newValue.toString();
		    if (oldValue == null) {
			var.value = TclString.newInstance(bytes);
			((TclObject) var.value).preserve();
		    } else {
			if (oldValue.isShared()) { // append to copy
			    var.value = oldValue.duplicate();
			    oldValue.release();
			    oldValue = (TclObject) var.value;
			    oldValue.preserve(); // since var is referenced
			}
			TclString.append(oldValue, newValue);
		    }
		}
	    } else {
		if ((flags & TCL.LIST_ELEMENT) != 0) {  // set var to list element
		    int listFlags;

		    // We set the variable to the result of converting newValue's
		    // string rep to a list element. We do not change newValue's
		    // ref count.

		    if (oldValue != null) {
			oldValue.release(); // discard old value
		    }
		    bytes = newValue.toString();
		    listFlags = Util.scanElement(interp, bytes);
		    oldValue = TclString.newInstance(
			           Util.convertElement(bytes, listFlags));
		    var.value = oldValue;
		    ((TclObject) var.value).preserve();
		} else if (newValue != oldValue) {
		    var.value = newValue;
		    newValue.preserve();               // var is another ref
		    if (oldValue != null) {
			oldValue.release();            // discard old value
		    }
		}
	    }
	    var.setVarScalar();
	    var.clearVarUndefined();
	    if (array != null) {
		array.clearVarUndefined();
	    }

	    // Invoke any write traces for the variable.

	    if ((var.traces != null)
		|| ((array != null) && (array.traces != null))) {
		
		String msg = callTraces(interp, array, var, part1, part2,
			 (flags & (TCL.GLOBAL_ONLY|TCL.NAMESPACE_ONLY)) | TCL.TRACE_WRITES);
		if (msg != null) {
		    if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
			throw new TclVarException(interp, part1, part2, "set", msg);
		    }
		    return null; // Same as "goto cleanup" in C verison
		}
	    }

	    // Return the variable's value unless the variable was changed in some
	    // gross way by a trace (e.g. it was unset and then recreated as an
	    // array).

	    if (var.isVarScalar() && !var.isVarUndefined()) {
		return (TclObject) var.value;
	    }

	    // A trace changed the value in some gross way. Return an empty string
	    // object.

	    return TclString.newInstance("");
	} finally {
	    // If the variable doesn't exist anymore and no-one's using it,
	    // then free up the relevant structures and hash table entries.

	    if (var.isVarUndefined()) {
		cleanupVar(var, array);
	    }
	}
    }


    /**
     *  TclIncrVar2 -> incrVar
     *
     *	Given a two-part variable name, which may refer either to a scalar
     *	variable or an element of an array, increment the Tcl object value
     *  of the variable by a specified amount.
     *
     * @param part1 1st part of the variable name.
     * @param part2 2nd part of the variable name.
     * @param incrAmount Amount to be added to variable.
     * @param flags misc flags that control the actions of this method
     *
     * Results:
     *	Returns a reference to the TclObject holding the new value of the
     *	variable. If the specified variable doesn't exist, or there is a
     *	clash in array usage, or an error occurs while executing variable
     *	traces, then a TclException will be raised.
     *
     * Side effects:
     *	The value of the given variable is incremented by the specified
     *	amount. If either the array or the entry didn't exist then a new
     *	variable is created. The ref count for the returned object is _not_
     *	incremented to reflect the returned reference; if you want to keep a
     *	reference to the object you must increment its ref count yourself.
     *
     *----------------------------------------------------------------------
     */

    static TclObject incrVar(
        Interp interp,		// Command interpreter in which variable is
				// to be found.
	TclObject part1,	// Reference to an object holding the name of
				// an array (if part2 is non-null) or the
				// name of a variable.
	TclObject part2,	// If non-null, points to an object holding
				// the name of an element in the array
				// part1.
	int incrAmount,	        // Amount to be added to variable.
	int flags              // Various flags that tell how to incr value:
				// any of TCL.GLOBAL_ONLY,
				// TCL.NAMESPACE_ONLY, TCL.APPEND_VALUE,
				// TCL.LIST_ELEMENT, TCL.LEAVE_ERR_MSG.
	)
	throws TclException
    {
	TclObject varValue = null;
	boolean createdNewObj;	// Set to true if var's value object is shared
				// so we must increment a copy (i.e. copy
				// on write).
	int i;
	boolean err;

	// There are two possible error conditions that depend on the setting of
	// TCL.LEAVE_ERR_MSG. an exception could be raised or null could be returned
	err = false;
	try {
	    varValue = getVar(interp, part1, part2, flags);
	} catch (TclException e) {
	    err = true;
	    throw e;
	} finally {
	    // FIXME : is this the correct way to catch the error?
	    if (err || varValue == null)
		interp.addErrorInfo(
		    "\n    (reading value of variable to increment)");
	}


	// Increment the variable's value. If the object is unshared we can
	// modify it directly, otherwise we must create a new copy to modify:
	// this is "copy on write". Then free the variable's old string
	// representation, if any, since it will no longer be valid.

	createdNewObj = false;
	if (varValue.isShared()) {
	    varValue = varValue.duplicate();
	    createdNewObj = true;
	}

	try {
	    i = TclInteger.get(interp, varValue);
	} catch (TclException e) {
	    if (createdNewObj) {
		varValue.release();  // free unneeded copy
	    }
	    throw e;
	}
	
	TclInteger.set(varValue, (i + incrAmount));

	// Store the variable's new value and run any write traces.

	return setVar(interp, part1, part2, varValue, flags);
    }
    
    /**
     * Unset a variable whose name is stored in a Tcl object.
     *
     * @param nameObj name of the variable.
     * @param flags misc flags that control the actions of this method.
     */

    static void unsetVar(Interp interp, TclObject nameObj, int flags) 
	    throws TclException {
	unsetVar(interp, nameObj.toString(), null, flags);
    }

    /**
     * Unset a variable.
     *
     * @param name name of the variable.
     * @param flags misc flags that control the actions of this method.
     */

    static void unsetVar(Interp interp, String name, int flags) 
	    throws TclException {
	unsetVar(interp, name, null, flags);
    }

    /**
     * Tcl_UnsetVar2 -> unsetVar
     *
     * Unset a variable, given a two-part name consisting of array
     * name and element within array.
     *
     * @param part1 1st part of the variable name.
     * @param part2 2nd part of the variable name.
     * @param flags misc flags that control the actions of this method.
     *
     *	If part1 and part2 indicate a local or global variable in interp,
     *	it is deleted.  If part1 is an array name and part2 is null, then
     *	the whole array is deleted.
     *
     */

    static void unsetVar(
        Interp interp,		// Command interpreter in which var is
				// to be looked up.
	String part1,		// Name of variable or array.
	String part2,		// Name of element within array or null.
	int flags		// OR-ed combination of any of
				// TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY,
				// TCL.LEAVE_ERR_MSG.
	)
	throws TclException
    {
	Var dummyVar;
	Var var;
	Var array;
	//ActiveVarTrace active;
	TclObject obj;
	int result;

	// FIXME : what about the null return vs exception thing here?
	Var[] lookup_result = lookupVar(interp, part1, part2, flags, "unset",
				 false, false);
	if (lookup_result == null) {
	    throw new TclRuntimeError("unexpected null reference");
	}

	var = lookup_result[0];
	array = lookup_result[1];

	result = (var.isVarUndefined() ? TCL.ERROR : TCL.OK);

	if ((array != null) && (array.sidVec != null)) {
	    deleteSearches(array);
	}


	// The code below is tricky, because of the possibility that
	// a trace procedure might try to access a variable being
	// deleted. To handle this situation gracefully, do things
	// in three steps:
	// 1. Copy the contents of the variable to a dummy variable
	//    structure, and mark the original Var structure as undefined.
	// 2. Invoke traces and clean up the variable, using the dummy copy.
	// 3. If at the end of this the original variable is still
	//    undefined and has no outstanding references, then delete
	//	  it (but it could have gotten recreated by a trace).

	dummyVar          = new Var();
	//FIXME: Var class really should implement clone to make a bit copy. 
	dummyVar.value    = var.value;
	dummyVar.traces   = var.traces;
	dummyVar.flags    = var.flags;
	dummyVar.hashKey  = var.hashKey;
	dummyVar.table    = var.table;
	dummyVar.refCount = var.refCount;
	dummyVar.ns       = var.ns;

	var.setVarUndefined();
	var.setVarScalar();
	var.value  = null;  // dummyVar points to any value object
	var.traces = null;
	var.sidVec = null;

	// Call trace procedures for the variable being deleted. Then delete
	// its traces. Be sure to abort any other traces for the variable
	// that are still pending. Special tricks:
	// 1. We need to increment var's refCount around this: CallTraces
	//    will use dummyVar so it won't increment var's refCount itself.
	// 2. Turn off the TRACE_ACTIVE flag in dummyVar: we want to
	//    call unset traces even if other traces are pending.

	if ((dummyVar.traces != null)
	    || ((array != null) && (array.traces != null))) {
	    var.refCount++;
	    dummyVar.flags &= ~TRACE_ACTIVE;
	    callTraces(interp, array, dummyVar, part1, part2,
	        (flags & (TCL.GLOBAL_ONLY|TCL.NAMESPACE_ONLY)) | TCL.TRACE_UNSETS);

	    dummyVar.traces = null;	    

	    // Active trace stuff is not part of Jacl's interp

	    var.refCount--;
	}

	// If the variable is an array, delete all of its elements. This must be
	// done after calling the traces on the array, above (that's the way
	// traces are defined). If it is a scalar, "discard" its object
	// (decrement the ref count of its object, if any).

	if (dummyVar.isVarArray() && !dummyVar.isVarUndefined()) {
	    deleteArray(interp, part1, dummyVar,
	        (flags & (TCL.GLOBAL_ONLY|TCL.NAMESPACE_ONLY)) | TCL.TRACE_UNSETS);
	}
	if (dummyVar.isVarScalar()
	    && (dummyVar.value != null)) {
	    obj = (TclObject) dummyVar.value;
	    obj.release();
	    dummyVar.value = null;
	}

	// If the variable was a namespace variable, decrement its reference count.
    
	if ((var.flags & NAMESPACE_VAR) != 0) {
	    var.flags &= ~NAMESPACE_VAR;
	    var.refCount--;
	}

	// Finally, if the variable is truly not in use then free up its Var
	// structure and remove it from its hash table, if any. The ref count of
	// its value object, if any, was decremented above.
	
	cleanupVar(var, array);

	// It's an error to unset an undefined variable.
	
	if (result != TCL.OK) {
	    if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
		throw new TclVarException(interp, part1, part2, "unset",
		        ((array == null) ? noSuchVar : noSuchElement));
	    }
	}
    }


    /**
     * Trace a variable whose name is stored in a Tcl object.
     *
     * @param nameObj name of the variable.
     * @param trace the trace to add.
     * @param flags misc flags that control the actions of this method.
     */

    static void traceVar(Interp interp, TclObject nameObj, int flags, VarTrace proc) 
	    throws TclException {
	traceVar(interp, nameObj.toString(), null, flags, proc);
    }

    /**
     * Trace a variable.
     *
     * @param name name of the variable.
     * @param trace the trace to add.
     * @param flags misc flags that control the actions of this method.
     */

    static void traceVar(Interp interp, String name, int flags, VarTrace proc) 
	    throws TclException {
	traceVar(interp, name, null, flags, proc);
    }

    /**
     * Tcl_TraceVar2 -> traceVar
     *
     * Trace a variable, given a two-part name consisting of array
     * name and element within array.
     *
     * @param part1 1st part of the variable name.
     * @param part2 2nd part of the variable name.
     * @param flags misc flags that control the actions of this method.
     * @param trace the trace to comand to add.
     */

    static void traceVar(
        Interp interp,		// interp in which var is located
	String part1,		// Name of scalar variable or array.
	String part2,		// Name of element within array;  null means
				// trace applies to scalar variable or array
				// as-a-whole.
	int flags,		// OR-ed collection of bits, including any
				// of TCL.TRACE_READS, TCL.TRACE_WRITES,
				// TCL.TRACE_UNSETS, TCL.GLOBAL_ONLY,
				// and TCL.NAMESPACE_ONLY.
	VarTrace proc   	// Procedure to call when specified ops are
				// invoked upon var.
	)
	throws TclException
    {
	Var[] result;
	Var var, array;

	// FIXME: what about the exception problem here?
	result = lookupVar(interp, part1, part2, (flags | TCL.LEAVE_ERR_MSG),
			"trace", true, true);
	if (result == null) {
	    throw new TclException(interp, "");
	}

	var = result[0];
	array = result[1];

	// Set up trace information.

	if (var.traces == null) {
	    var.traces = new Vector();
	}

	TraceRecord rec = new TraceRecord();
	rec.trace = proc;
	rec.flags =
	    flags & (TCL.TRACE_READS | TCL.TRACE_WRITES | TCL.TRACE_UNSETS | 
		     TCL.TRACE_ARRAY);

	var.traces.insertElementAt(rec, 0);


	// FIXME: is this needed ?? It was in Jacl but not 8.1

	/*
	// When inserting a trace for an array on an UNDEFINED variable,
	// the search IDs for that array are reset.

	if (array != null && var.isVarUndefined()) {
	    array.sidVec = null;
	}
	*/
    }

    
    /**
     * Untrace a variable whose name is stored in a Tcl object.
     *
     * @param nameObj name of the variable.
     * @param trace the trace to delete.
     * @param flags misc flags that control the actions of this method.
     */

    static void untraceVar(Interp interp, TclObject nameObj, int flags, VarTrace proc) 
    {
	untraceVar(interp, nameObj.toString(), null, flags, proc);
    }

    /**
     * Untrace a variable.
     *
     * @param name name of the variable.
     * @param trace the trace to delete.
     * @param flags misc flags that control the actions of this method.
     */

    static void untraceVar(Interp interp, String name, int flags, VarTrace proc) 
    {
	untraceVar(interp, name, null, flags, proc);
    }

    /**
     * Tcl_UntraceVar2 -> untraceVar
     *
     * Untrace a variable, given a two-part name consisting of array
     * name and element within array. This will Remove a
     * previously-created trace for a variable.
     *
     * @param interp Interpreter containing variable.
     * @param part1 1st part of the variable name.
     * @param part2 2nd part of the variable name.
     * @param flags misc flags that control the actions of this method.
     * @param proc the trace to delete.
     */

    static void untraceVar(
	Interp interp,       // Interpreter containing variable.
	String part1,        // Name of variable or array.
	String part2,        // Name of element within array; null means
                             // trace applies to scalar variable or array
                             // as-a-whole. 
	int flags,           // OR-ed collection of bits describing
	                     // current trace, including any of
	                     // TCL.TRACE_READS, TCL.TRACE_WRITES,
	                     // TCL.TRACE_UNSETS, TCL.GLOBAL_ONLY,
	                     // and TCL.NAMESPACE_ONLY.
	VarTrace proc        // Procedure assocated with trace.
	)
    {
	Var[] result = null;
	Var var;

	try {
	    result = lookupVar(interp, part1, part2,
			      flags & (TCL.GLOBAL_ONLY|TCL.NAMESPACE_ONLY),
			      null, false, false);
	    if (result == null) {
		return;
	    }
	} catch (TclException e) {
	    // FIXME: check for problems in exception in lookupVar

	    // We have set throwException argument to false in the
	    // lookupVar() call, so an exception should never be
	    // thrown.

	    throw new TclRuntimeError("unexpected TclException: " + e);
	}

	var = result[0];

	if (var.traces != null) {
	    int len = var.traces.size();
	    for (int i=0; i < len; i++) {
		TraceRecord rec = (TraceRecord) var.traces.elementAt(i);
		if (rec.trace == proc) {
		    var.traces.removeElementAt(i);
		    break;
		}
	    }
	}

	// If this is the last trace on the variable, and the variable is
	// unset and unused, then free up the variable.

	if (var.isVarUndefined()) {
	    cleanupVar(var, null);
	}
    }

    /**
     * Tcl_VarTraceInfo -> getTraces
     *
     * @param interp Interpreter containing variable.
     * @param name name of the variable.
     * @param flags flags that control the actions of this method.
     * @return the Vector of traces of a variable.
     */

    static protected Vector getTraces(
        Interp interp, // Interpreter containing variable.
	String name,   // Name of variable;  may end with "(index)"
	               // to signify an array reference.
	int flags      // OR-ed combination of TCL.GLOBAL_ONLY,
	               // TCL.NAMESPACE_ONLY.
	) 
	throws TclException
    {
	return getTraces(interp, name, null, flags);
    }

    /**
     * Tcl_VarTraceInfo2 -> getTraces
     *
     * @return the list of traces of a variable.
     *
     * @param interp Interpreter containing variable.
     * @param part1 1st part of the variable name.
     * @param part2 2nd part of the variable name (can be null).
     * @param flags misc flags that control the actions of this method.
     */

    static protected Vector getTraces(
	Interp interp, // Interpreter containing variable.
	String part1,  // Name of variable or array.
	String part2,  // Name of element within array; null means
	               // trace applies to scalar variable or array
	               // as-a-whole.
	int flags      // OR-ed combination of TCL.GLOBAL_ONLY,
	               // TCL.NAMESPACE_ONLY.
	) 
	throws TclException
    {
	Var[] result;

	result = lookupVar(interp, part1, part2,
		     flags & (TCL.GLOBAL_ONLY|TCL.NAMESPACE_ONLY),	   
		     null,
		     false, false);
	
	if (result == null) {
	    return null;
	}

	return result[0].traces;
    }


    /**
     * MakeUpvar -> makeUpvar
     *
     * Create a reference of a variable in otherFrame in the current
     * CallFrame, given a two-part name consisting of array name and
     * element within array.
     *
     * @param interp Interp containing the variables
     * @param frame CallFrame containing "other" variable.
     *     null means use global context.
     * @param otherP1 the 1st part name of the variable in the "other" frame.
     * @param otherP2 the 2nd part name of the variable in the "other" frame.
     * @param otherFlags the flags for scaope of "other" variable
     * @param myName Name of scalar variable which will refer to otherP1/otherP2.
     * @param myFlags only the TCL.GLOBAL_ONLY bit matters, 
     *    indicating the scope of myName.
     * @exception TclException if the upvar cannot be created. 
     */

    protected static void makeUpvar(
	Interp interp,		// Interpreter containing variables. Used
				// for error messages, too.
	CallFrame frame,	// Call frame containing "other" variable.
				// null means use global :: context.
	String otherP1,         // Two-part name of variable in framePtr. 
	String otherP2,
	int otherFlags,		// 0, TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY:
				// indicates scope of "other" variable.
	String myName,		// Name of variable which will refer to
				// otherP1/otherP2. Must be a scalar.
	int myFlags		// 0, TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY:
				// indicates scope of myName.
	)
	throws TclException
    {
	Var other, var, array;
	Var[] result;
	CallFrame varFrame;
	CallFrame savedFrame = null;
	Hashtable table;
	NamespaceCmd.Namespace ns, altNs;
	String tail;
	boolean newvar = false;

	// Find "other" in "frame". If not looking up other in just the
	// current namespace, temporarily replace the current var frame
	// pointer in the interpreter in order to use TclLookupVar.
	
	if ((otherFlags & TCL.NAMESPACE_ONLY) == 0) {
	    savedFrame = interp.varFrame;
	    interp.varFrame = frame;
	}
	result = lookupVar(interp, otherP1, otherP2,
			   (otherFlags | TCL.LEAVE_ERR_MSG), "access",
			   true, true);

	if ((otherFlags & TCL.NAMESPACE_ONLY) == 0) {
	    interp.varFrame = savedFrame;
	}
	
	other = result[0];
	array = result[1];

	if (other == null) {
	    // FIXME : leave error message thing again
	    throw new TclRuntimeError("unexpected null reference");
	}

	// Now create a hashtable entry for "myName". Create it as either a
	// namespace variable or as a local variable in a procedure call
	// frame. Interpret myName as a namespace variable if:
	//    1) so requested by a TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY flag,
	//    2) there is no active frame (we're at the global :: scope),
	//    3) the active frame was pushed to define the namespace context
	//       for a "namespace eval" or "namespace inscope" command,
	//    4) the name has namespace qualifiers ("::"s).
	// If creating myName in the active procedure, look in its
	// hashtable for runtime-created local variables. Create that
	// procedure's local variable hashtable if necessary.

	varFrame = interp.varFrame;
	if (((myFlags & (TCL.GLOBAL_ONLY | TCL.NAMESPACE_ONLY)) != 0)
	    || (varFrame == null)
	    || !varFrame.isProcCallFrame
	    || (myName.indexOf("::") != -1)) {

	    // Java does not support passing an address so we pass
	    // an array of size 1 and then assign arr[0] to the value
	    NamespaceCmd.Namespace[] nsArr      = new NamespaceCmd.Namespace[1];
	    NamespaceCmd.Namespace[] altNsArr   = new NamespaceCmd.Namespace[1];
	    NamespaceCmd.Namespace[] dummyNsArr = new NamespaceCmd.Namespace[1];
	    String[]    tailArr                 = new String[1];

	    NamespaceCmd.getNamespaceForQualName(interp, myName, null,
		       myFlags, nsArr, altNsArr, dummyNsArr, tailArr);

	    // Get the values out of the arrays!
	    ns      = nsArr[0];
	    altNs   = altNsArr[0];
	    tail    = tailArr[0];

	    if (ns == null) {
		ns = altNs;
	    }
	    if (ns == null) {
		throw new TclException(interp, "bad variable name \"" +
				               myName +
				               "\": unknown namespace");
	    }

	    // Check that we are not trying to create a namespace var linked to
	    // a local variable in a procedure. If we allowed this, the local
	    // variable in the shorter-lived procedure frame could go away
	    // leaving the namespace var's reference invalid.

	    if (((otherP2 != null) ? array.ns : other.ns) == null) {
		throw new TclException(interp, "bad variable name \"" +
				               myName +
		"\": upvar won't create namespace variable that refers to procedure variable");
	    }

	    var = (Var) ns.varTable.get(tail);
	    if (var == null) { // we are adding a new entry
		newvar = true;
		var = new Var();
		ns.varTable.put(tail, var);

		// There is no hPtr member in Jacl, The hPtr combines the table
		// and the key used in a table lookup.
		var.hashKey = tail;
		var.table   = ns.varTable;
		
		var.ns = ns;
	    }
	} else {
	    // Skip Compiled Local stuff
	    var = null;
	    if (var == null) {	// look in frame's local var hashtable
		table = varFrame.varTable;
		if (table == null) {
		    table = new Hashtable();
		    varFrame.varTable = table;
		}

		var = (Var) table.get(myName);
		if (var == null) { // we are adding a new entry
		    newvar = true;
		    var = new Var();
		    table.put(myName, var);
		    
		    // There is no hPtr member in Jacl, The hPtr combines the table
		    // and the key used in a table lookup.
		    var.hashKey = myName;
		    var.table   = table;

		    var.ns = varFrame.ns;
		}
	    }
	}

	if (!newvar) {
	    // The variable already exists. Make sure this variable "var"
	    // isn't the same as "other" (avoid circular links). Also, if
	    // it's not an upvar then it's an error. If it is an upvar, then
	    // just disconnect it from the thing it currently refers to.

	    if (var == other) {
		throw new TclException(interp, "can't upvar from variable to itself");
	    }
	    if (var.isVarLink()) {
		Var link = (Var) var.value;
		if (link == other) {
		    return;
		}
		link.refCount--;
		if (link.isVarUndefined()) {
		    cleanupVar(link, null);
		}
	    } else if (! var.isVarUndefined()) {
		throw new TclException(interp, "variable \"" +
				               myName +
				               "\" already exists");
	    } else if (var.traces != null) {
		throw new TclException(interp, "variable \"" +
				               myName +
				               "\" has traces: can't use for upvar");
	    }
	}

	var.setVarLink();
	var.clearVarUndefined();
	var.value = other;
	other.refCount++;
	return;
    }

    /*
     *----------------------------------------------------------------------
     *
     * Tcl_GetVariableFullName -> getVariableFullName
     *
     *  Given a Var token returned by NamespaceCmd.FindNamespaceVar, this
     *	procedure appends to an object the namespace variable's full
     *	name, qualified by a sequence of parent namespace names.
     *
     * Results:
     *  None.
     *
     * Side effects:
     *  The variable's fully-qualified name is returned.
     *
     *----------------------------------------------------------------------
     */

    static String getVariableFullName(
         Interp interp,	        // Interpreter containing the variable.
	 Var var                // Token for the variable returned by a
				// previous call to Tcl_FindNamespaceVar.
	 )
    {
	StringBuffer buff = new StringBuffer();

	// Add the full name of the containing namespace (if any), followed by
	// the "::" separator, then the variable name.

	if (var != null) {
	    if (! var.isVarArrayElement()) {
		if (var.ns != null) {
		    buff.append(var.ns.fullName);
		    if (var.ns != interp.globalNs) {
			buff.append("::");
		    }
		}
		// Jacl's Var class does not include the "name" member
		// We use the "hashKey" member which is equivalent

		if (var.hashKey != null) {
		    buff.append(var.hashKey);
		}
	    }
	}

	return buff.toString();
    }

    /**
     * CallTraces -> callTraces
     *
     * This procedure is invoked to find and invoke relevant
     * trace procedures associated with a particular operation on
     * a variable.  This procedure invokes traces both on the
     * variable and on its containing array (where relevant).
     *
     * @param interp Interpreter containing variable.
     * @param array array variable that contains the variable, or null
     *   if the variable isn't an element of an array.
     * @param var Variable whose traces are to be invoked.
     * @param part1 the first part of a variable name.
     * @param part2 the second part of a variable name.
     * @param flags Flags to pass to trace procedures: indicates
     *   what's happening to variable, plus other stuff like
     *   TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY, and TCL.INTERP_DESTROYED.
     * @return null if no trace procedures were invoked, or
     *   if all the invoked trace procedures returned successfully.
     *   The return value is non-null if a trace procedure returned an
     *   error (in this case no more trace procedures were invoked
     *   after the error was returned). In this case the return value
     *   is a pointer to a string describing the error.
     */

    static protected String callTraces(Interp interp, Var array, Var var,
				       String part1, String part2, int flags) {	
	TclObject oldResult;
	int i;

	// If there are already similar trace procedures active for the
	// variable, don't call them again.

	if ((var.flags & Var.TRACE_ACTIVE) != 0) {
	    return null;
	}
	var.flags |= Var.TRACE_ACTIVE;
	var.refCount++;

	// If the variable name hasn't been parsed into array name and
	// element, do it here.  If there really is an array element,
	// make a copy of the original name so that nulls can be
	// inserted into it to separate the names (can't modify the name
	// string in place, because the string might get used by the
	// callbacks we invoke).

	// FIXME : come up with parsing code to use for all situations!
	if (part2 == null) {
	    int len = part1.length();

	    if (len > 0) {
		if (part1.charAt(len-1) == ')') {
		    for (i=0; i < len-1; i++) {
			if (part1.charAt(i) == '(') {
			    break;
			}
		    }
		    if (i < len-1) {
			if (i < len-2) {
			    part2 = part1.substring(i+1, len - 1);
			    part1 = part1.substring(0, i);
			}
		    }
		}
	    }
	}

	oldResult = interp.getResult();
	oldResult.preserve();
	interp.resetResult();

	try {
	    // Invoke traces on the array containing the variable, if relevant.

	    if (array != null) {
		array.refCount++;
	    }
	    if ((array != null) && (array.traces != null)) {
		for (i=0; (array.traces != null) && (i < array.traces.size());
			i++) {
		    TraceRecord rec = (TraceRecord) array.traces.elementAt(i);
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

	    for (i=0; (var.traces != null) && (i < var.traces.size()); i++) {
		TraceRecord rec = (TraceRecord) var.traces.elementAt(i);
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
	    var.flags &= ~TRACE_ACTIVE;
	    var.refCount--;

	    interp.setResult(oldResult);
	    oldResult.release();
	}
    }

    /**
     * DeleteSearches -> deleteSearches
     *
     *	This procedure is called to free up all of the searches
     *	associated with an array variable.
     *
     * @param interp Interpreter containing array.
     * @param arrayVar the array variable to delete searches from.
     */  

    static protected void deleteSearches(
        Var arrayVar) // Variable whose searches are to be deleted.
    {
	arrayVar.sidVec = null;
    }

    /**
     * TclDeleteVars -> deleteVars
     *
     *	This procedure is called to recycle all the storage space
     *	associated with a table of variables. For this procedure
     *	to work correctly, it must not be possible for any of the
     *	variables in the table to be accessed from Tcl commands
     *	(e.g. from trace procedures).
     *
     * @param interp Interpreter containing array.
     * @param table Hashtbale that holds the Vars to delete
     */

    static protected void deleteVars(Interp interp, Hashtable table)
    {
	Enumeration search;
	String hashKey;
	Var var;
	Var link;
	int flags;
	//ActiveVarTrace active;
	TclObject obj;
	NamespaceCmd.Namespace currNs = NamespaceCmd.getCurrentNamespace(interp);

	// Determine what flags to pass to the trace callback procedures.

	flags = TCL.TRACE_UNSETS;
	if (table == interp.globalNs.varTable) {
	    flags |= (TCL.INTERP_DESTROYED | TCL.GLOBAL_ONLY);
	} else if (table == currNs.varTable) {
	    flags |= TCL.NAMESPACE_ONLY;
	}

	
	for (search = table.elements(); search.hasMoreElements(); ) {
	    var = (Var) search.nextElement();
	    
	    // For global/upvar variables referenced in procedures, decrement
	    // the reference count on the variable referred to, and free
	    // the referenced variable if it's no longer needed. Don't delete
	    // the hash entry for the other variable if it's in the same table
	    // as us: this will happen automatically later on.

	    if (var.isVarLink()) {
		link = (Var) var.value;
		link.refCount--;
		if ((link.refCount == 0) && link.isVarUndefined()
		    && (link.traces == null)
		    && ((link.flags & IN_HASHTABLE) != 0)) {
		    
		    if (link.hashKey == null) {
			var.value = null; // Drops reference to the link Var
		    } else if (link.table != table) {
			link.table.remove(link.hashKey);
			link.table = null; // Drops the link var's table reference
			var.value = null;  // Drops reference to the link Var
		    }
		}
	    }

	    // free up the variable's space (no need to free the hash entry
	    // here, unless we're dealing with a global variable: the
	    // hash entries will be deleted automatically when the whole
	    // table is deleted). Note that we give callTraces the variable's
	    // fully-qualified name so that any called trace procedures can
	    // refer to these variables being deleted.

	    if (var.traces != null) {
		String fullname = getVariableFullName(interp, var);

		callTraces(interp, null, var,
			   fullname, null, flags);

		// The var.traces = null statement later will drop all the
		// references to the traces which will free them up
	    }
	    
	    if (var.isVarArray()) {
		deleteArray(interp, var.hashKey, var,
			    flags);
		var.value = null;
	    }
	    if (var.isVarScalar() && (var.value != null)) {
		obj = (TclObject) var.value;
		obj.release();
		var.value = null;
	    }

	    // There is no hPtr member in Jacl, The hPtr combines the table
	    // and the key used in a table lookup.
	    var.hashKey = null;
	    var.table   = null;
	    var.traces = null;
	    var.setVarUndefined();
	    var.setVarScalar();

	    // If the variable was a namespace variable, decrement its 
	    // reference count. We are in the process of destroying its
	    // namespace so that namespace will no longer "refer" to the
	    // variable.

	    if ((var.flags & NAMESPACE_VAR) != 0) {
		var.flags &= ~NAMESPACE_VAR;
		var.refCount--;
	    }

	    // Recycle the variable's memory space if there aren't any upvar's
	    // pointing to it. If there are upvars to this variable, then the
	    // variable will get freed when the last upvar goes away.

	    if (var.refCount == 0) {
		// When we drop the last reference it will be freeded
	    }
	}
	table.clear();
    }


    /**
     * DeleteArray -> deleteArray
     *
     * This procedure is called to free up everything in an array
     * variable.  It's the caller's responsibility to make sure
     * that the array is no longer accessible before this procedure
     * is called.
     *
     * @param interp Interpreter containing array.
     * @param arrayName name of array (used for trace callbacks).
     * @param var the array variable to delete.
     * @param flags Flags to pass to CallTraces.
     */
    
    static protected void deleteArray(
        Interp interp,     // Interpreter containing array.
	String arrayName,  // Name of array (used for trace callbacks)
	Var var,           // Reference to Var instance
	int flags          // Flags to pass to callTraces:
	                   // TCL.TRACE_UNSETS and sometimes
	                   // TCL.INTERP_DESTROYED,
	                   // TCL.NAMESPACE_ONLY, or
	                   // TCL.GLOBAL_ONLY.
	)
    {
	Enumeration search;
	Var el;
	TclObject obj;

	deleteSearches(var);
	Hashtable table = (Hashtable) var.value;

	Var dummyVar = null;
	for (search = table.elements();
		search.hasMoreElements(); ) {
	    el = (Var) search.nextElement();

	    if (el.isVarScalar() && (el.value != null)) {
		obj = (TclObject) el.value;
		obj.release();
		el.value = null;
	    }

	    String tmpkey = (String) el.hashKey;
	    // There is no hPtr member in Jacl, The hPtr combines the table
	    // and the key used in a table lookup.
	    el.hashKey = null;
	    el.table   = null;
	    if (el.traces != null) {
		el.flags &= ~TRACE_ACTIVE;
		// FIXME : Old Jacl impl passed a dummy var to callTraces, should we?
		callTraces(interp, null, el, arrayName,
		    tmpkey, flags);
		el.traces = null;
		// Active trace stuff is not part of Jacl
	    }
	    el.setVarUndefined();
	    el.setVarScalar();
	    if (el.refCount == 0) {
		// We are no longer using the element
		// element Vars are IN_HASHTABLE
	    }
	}
	((Hashtable) var.value).clear();
	var.value = null;
    }


    /**
     * CleanupVar -> cleanupVar
     *
     * This procedure is called when it looks like it may be OK
     * to free up the variable's record and hash table entry, and
     * those of its containing parent.  It's called, for example,
     * when a trace on a variable deletes the variable.
     *
     * @param var variable that may be a candidate for being expunged.
     * @param array Array that contains the variable, or NULL if this
     *   variable isn't an array element.
     */

    static protected void cleanupVar(Var var, Var array) {
	if (var.isVarUndefined() && (var.refCount == 0)
	    && (var.traces == null)
	    && ((var.flags & IN_HASHTABLE) != 0)) {
	    if (var.table != null) {
		var.table.remove(var.hashKey);
		var.table = null;
		var.hashKey = null;
	    }
	}
	if (array != null) {
	    if (array.isVarUndefined() && (array.refCount == 0)
		&& (array.traces == null)
		&& ((array.flags & IN_HASHTABLE) != 0)) {
		if (array.table != null) {
		    array.table.remove(array.hashKey);
		    array.table = null;
		    array.hashKey = null;
		}
	    }
	}
    }


} // End of Var class
