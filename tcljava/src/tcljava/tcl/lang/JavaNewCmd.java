/*
 * JavaNewCmd.java --
 *
 *	Implements the built-in "java::new" command.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc. All rights reserved.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: JavaNewCmd.java,v 1.2.10.1 2000/10/25 11:01:24 mdejong Exp $
 *
 */

package tcl.lang;

/*
 * This class implements the built-in "java::new" command.
 */

class JavaNewCmd implements Command {


/*
 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked to process the "java::new" Tcl
 *	comamnd.  See the user documentation for details on what
 *	it does.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	A standard Tcl result is stored in the interpreter.
 *
 *----------------------------------------------------------------------
 */

public void
cmdProc(
    Interp interp,			// Current interpreter.
    TclObject argv[])			// Argument list.
throws
    TclException			// A standard Tcl exception.
{
    if (argv.length < 2) {
	throw new TclNumArgsException(interp, 1, argv,
		"signature ?arg arg ...?");
    }

    // The "java::new" command can take both array signatures and
    // constructor signatures. We want to know what type of signature
    // is given without throwing and catching exceptions. Thus, we
    // call ArraySig.looksLikeArraySig() to determine quickly whether
    // a argv[1] can be interpreted as an array signature or a
    // constructor signature. This is a much less expensive way than
    // calling ArraySig.get() and then calling JavaInvoke.newInstance()
    // if that fails.

    if (ArraySig.looksLikeArraySig(interp, argv[1])) {
	// Create a new Java array object.

	if ((argv.length < 3) || (argv.length > 4)) {
	    throw new TclNumArgsException(interp, 2, argv,
		"sizeList ?valueList?");
	}

	ArraySig sig = ArraySig.get(interp, argv[1]);
	Class componentType = sig.componentType;
	int dimensions = sig.dimensions;

	TclObject sizeListObj = argv[2];
	int sizeListLen = TclList.getLength(interp, sizeListObj);

	if (sizeListLen > dimensions) {
	    throw new TclException(interp,
		"size list \"" +  sizeListObj + 
		"\" doesn't match array dimension (" + dimensions + ")");
	}
	
	TclObject valueListObj = null;
	if (argv.length == 4) {
	    valueListObj = argv[3];
	}

	// Initialize arrayObj according to dimensions of both
	// sizeListObj and valueListObj.

	Object obj = ArrayObject.initArray(interp, sizeListObj, 
		sizeListLen,  0, dimensions, componentType, valueListObj);

	interp.setResult(ReflectObject.newInstance(interp,componentType,obj));
    } else {
	// Create a new (scalar) Java object.

	int startIdx = 2;
	int count = argv.length - startIdx;

	interp.setResult(JavaInvoke.newInstance(interp, argv[1], argv,
	    startIdx, count));
    }
}

} // end JavaNewCmd


// The ArraySig class is used internally by the JavaNewCmd
// class. ArraySig implements a new Tcl object type that represents an
// array signature used for creating Java arrays. Examples or array
// signatures are "int[][]", "java.lang.Object[]" or "[[D".

class ArraySig implements InternalRep {

// The base component type of the array. For example, the component
// type for int[][][] is int.

Class componentType;

// The number of dimensions specified by the signature. For example, 
// int[][][] has a dimension of 3.

int dimensions;


/*
 *----------------------------------------------------------------------
 *
 * ArraySig --
 *
 *	Creates a new ArraySig instance.
 *
 * Side effects:
 *	Member fields are initialized.
 *
 *----------------------------------------------------------------------
 */

ArraySig(
    Class cType,		// Initial value for componentType.
    int n)			// Initial value for dimensions.
{
    componentType = cType;
    dimensions = n;
}

/*
 *----------------------------------------------------------------------
 *
 * duplicate --
 *
 *	Make a copy of an object's internal representation.
 *
 * Results:
 *	Returns a newly allocated instance of the appropriate type.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public InternalRep duplicate()
{
    return new ArraySig(componentType, dimensions);
}

/**
  * Implement this no-op for the InternalRep interface.
  */

public void dispose() {}


/*
 *----------------------------------------------------------------------
 *
 * looksLikeArraySig --
 *
 *	This method quickly determines whether a TclObject can be
 *	interpreted as an array signature or a constructor signature.
 *
 * Results:
 *	True if the object looks like an array signature, false
 *	otherwise.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static boolean
looksLikeArraySig(
    Interp interp,		// Current interpreter.
    TclObject signature)	// TclObject to check.
throws
    TclException
{
    InternalRep rep = signature.getInternalRep();

    if (rep instanceof FuncSig) {
	// The string rep of FuncSig can never represent an ArraySig,
	// so we know for sure that signature doesn't look like an
	// ArraySig.

	return false;
    }
    if (rep instanceof ArraySig) {
	return true;
    }

    if (TclList.getLength(interp, signature) < 1) {
	return false;
    }

    String clsName = TclList.index(interp, signature, 0).toString();
    if (clsName.endsWith("[]") || clsName.startsWith("[")) {
	return true;
    } else {
	return false;
    }
}

/*
 *----------------------------------------------------------------------
 *
 * get --
 *
 *	Returns the ArraySig internal representation of the constructor
 *	or method that matches with the signature and the parameters.
 *
 * Results:
 *	The ArraySig given by the signature.
 *
 * Side effects:
 *	When successful, the internalRep of the signature object is
 *	converted to ArraySig.
 *
 *----------------------------------------------------------------------
 */

static ArraySig
get(
    Interp interp,		// Current interpreter. Stores error message
    				// if signature doesn't contain an array sig.
    TclObject signature)	// The TclObject to convert.
throws
    TclException		// Standard Tcl exception.
{
    InternalRep rep = signature.getInternalRep();
    if ((rep instanceof ArraySig)) {
	// The cached internal rep is a valid array signature, return it.

	return (ArraySig)rep;
    }

    trying: {
	if (TclList.getLength(interp, signature) != 1) {
	    break trying;
	}

	String clsName = TclList.index(interp, signature, 0).toString();
	if (!(clsName.endsWith("[]")) && !(clsName.startsWith("["))) {
	    break trying;
	}
	Class componentType = JavaInvoke.getClassByName(interp, clsName);
	int dimensions = 0;

	if (clsName.charAt(0) == '[') {
	    // If the string begins with '[', count the leading '['s.
    
	    String tmp = clsName;
	    while (tmp.charAt(++dimensions) == '[') {
	    }

	} else {
	    // If the string is of the form className[][]..., count
	    // the trailing "[]"s.
    
	    String tmp = clsName;
	    for (; tmp.endsWith("[]"); dimensions++) {
		tmp = tmp.substring(0, tmp.length() - 2);
	    }
	}

	ArraySig sigRep = new ArraySig(componentType, dimensions);

	signature.setInternalRep(sigRep);
	return sigRep;
    }

    throw new TclException(interp, "bad array signature \"" + signature
	    + "\"");
}

} // end ArraySig

