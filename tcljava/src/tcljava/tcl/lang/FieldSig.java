/*
 * FieldSig.java --
 *
 *	This class implements the internal representation of a Java
 *	field signature.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: FieldSig.java,v 1.2.10.1 2000/10/25 11:01:24 mdejong Exp $
 *
 */

package tcl.lang;

import tcl.lang.reflect.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * This class implements the internal representation of a Java field
 * signature.
 */

class FieldSig implements InternalRep {

// The class that the field signature is used against. If the
// java::field command is given a class name, then the targetCls is
// that class; if If the java::field command is given a Java object,
// then the targetCls is the class of that object.  targetCls is used
// to test the validity of a cached FieldSig internal rep.

Class targetCls;

// The class specified by the signature. If the signature only gives
// the name of a field and omits the declaring class, then sigCls is
// the same as targetCls.

Class sigCls;

// The handle to the field as given by the field signature.

Field field;

// The PkgInvoker used to access the field. 

PkgInvoker pkgInvoker;


/*
 *----------------------------------------------------------------------
 *
 * FieldSig --
 *
 *	Creates a new FieldSig instance.
 *
 * Side effects:
 *	Member fields are initialized.
 *
 *----------------------------------------------------------------------
 */

FieldSig(
    Class tc,			// Initial value for targetCls.
    Class sc,			// Initial value for sigCls.
    PkgInvoker p,		// Initial value for pkgInvoker.
    Field f)			// Initial value for field.
{
    targetCls = tc;
    sigCls = sc;
    pkgInvoker = p;
    field = f;
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
    return new FieldSig(targetCls, sigCls, pkgInvoker, field);
}

/**
  * Implement this no-op for the InternalRep interface.
  */

public void dispose() {}

/*
 *----------------------------------------------------------------------
 *
 * get --
 *
 *	Returns the FieldSig internal rep given by the field signature.
 *
 * Results:
 *	The FieldSig given by the signature.
 *
 * Side effects:
 *	When successful, the internalRep of the signature object is
 *	converted to FieldSig.
 *
 *----------------------------------------------------------------------
 */

static FieldSig
get(
    Interp interp,		// Current interpreter
    TclObject signature,	// The TclObject that holds a field signature.
    Class targetCls)		// The target class of the field signature.
throws
    TclException		// Standard Tcl exception.
{
    InternalRep rep = signature.getInternalRep();

    if ((rep instanceof FieldSig) && (((FieldSig)rep).targetCls == targetCls)){
	// The cached internal rep is a valid field signature for
	// the given targetCls.

	return (FieldSig)rep;
    }

    String fieldName;
    TclObject sigClsObj;
    Class sigCls;
    Field field;

    int len = TclList.getLength(interp, signature);
    if ((len < 1) || (len > 2)) {
	throw new TclException(interp, "bad field signature \"" +
		signature + "\"");
    }

    fieldName = TclList.index(interp, signature, 0).toString();
    sigClsObj = TclList.index(interp, signature, 1);

    if (sigClsObj != null) {
	sigCls = ClassRep.get(interp, sigClsObj);
	if (!sigCls.isAssignableFrom(targetCls)) {
	    throw new TclException(interp, "\"" + sigCls.getName() +
		    "\" is not a superclass of \"" + targetCls.getName() +
		    "\"");
	}
    } else {
	sigCls = targetCls;
    }

    try {
	field = sigCls.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
	throw new TclException(interp, "field \"" + signature +
		"\" doesn't exist");
    }

    FieldSig sig = new FieldSig(targetCls, sigCls, PkgInvoker.getPkgInvoker(
	    targetCls), field);
    signature.setInternalRep(sig);

    return sig;
}

} // end FieldSig

