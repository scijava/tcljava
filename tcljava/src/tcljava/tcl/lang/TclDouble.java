/*
 * TclDouble.java --
 *
 *	Implements the TclDouble internal object representation, as well
 *	variable traces for the tcl_precision variable.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclDouble.java,v 1.4 2005/09/30 02:12:17 mdejong Exp $
 *
 */

package tcl.lang;

/*
 * This class implements the double object type in Tcl.
 */

public class TclDouble implements InternalRep {

/*
 * Internal representation of a double value.
 */

private double value;


/*
 *----------------------------------------------------------------------
 *
 * TclDouble --
 * 
 *	Construct a TclDouble representation with the given double
 *	value.
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
TclDouble(
    double i)		// Initial value.
{
    value = i;

    if (TclObject.saveObjRecords) {
        String key = "TclDouble";
        Integer num = (Integer) TclObject.objRecordMap.get(key);
        if (num == null) {
            num = new Integer(1);
        } else {
            num = new Integer(num.intValue() + 1);
        }
        TclObject.objRecordMap.put(key, num);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * TclDouble --
 *
 *	Construct a TclDouble representation with the initial value
 *	taken from the given string.
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
TclDouble(
    Interp interp,		// Current interpreter.
    String str)			// String that contains the initial value.
throws
    TclException		// If error occurs in string conversion.
{
    value = Util.getDouble(interp, str);

    if (TclObject.saveObjRecords) {
        String key = "TclDouble";
        Integer num = (Integer) TclObject.objRecordMap.get(key);
        if (num == null) {
            num = new Integer(1);
        } else {
            num = new Integer(num.intValue() + 1);
        }
        TclObject.objRecordMap.put(key, num);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * duplicate --
 *
 *	Duplicate the current object.
 *
 * Results:
 *	A dupilcate of the current object.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public InternalRep duplicate()
{
    if (TclObject.saveObjRecords) {
        String key = "TclDouble.duplicate()";
        Integer num = (Integer) TclObject.objRecordMap.get(key);
        if (num == null) {
            num = new Integer(1);
        } else {
            num = new Integer(num.intValue() + 1);
        }
        TclObject.objRecordMap.put(key, num);
    }

    return new TclDouble(value);
}

/**
 * Implement this no-op for the InternalRep interface.
 */

public void dispose() {}

/*
 *----------------------------------------------------------------------
 *
 * newInstance --
 *
 *	Creates a new instance of a TclObject with a TclDouble internal
 *	representation.
 *
 * Results:
 *	The newly created TclObject.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public static TclObject
newInstance(
    double d)			// Initial value.
{
    return new TclObject(new TclDouble(d));
}

/*
 *----------------------------------------------------------------------
 *
 * setDoubleFromAny --
 *
 *	Called to convert a TclObject's internal rep to TclDouble.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	When successful, the internal representation of tobj is
 *	changed to TclDouble, if it is not already so.
 *
 *----------------------------------------------------------------------
 */

private static void
setDoubleFromAny(
    Interp interp,		// Current interpreter. May be null.
    TclObject tobj)		// The object to convert.
throws
    TclException		// If error occurs in type conversion.
				// Error message will be left inside
				// the interp if it's not null.

{
    InternalRep rep = tobj.getInternalRep();

    if (rep instanceof TclDouble) {
	/*
	 * Do nothing.
	 */

    } else if (rep instanceof TclBoolean) {
	/*
	 * Short-cut.
	 */

	boolean b = TclBoolean.get(interp, tobj);

	// Can't convert from a "pure" boolean
	// to a "pure" double since it is possible
	// that the expr code would want to check
	// for an integer that was converted to
	// a double. If a "pure" boolean has no
	// string rep then generate one that
	// looks like an integer.

	if (tobj.hasNoStringRep()) {
	    tobj.toString();
	}

	if (b) {
	    tobj.setInternalRep(new TclDouble(1.0));
	} else {
	    tobj.setInternalRep(new TclDouble(0.0));
	}

	if (TclObject.saveObjRecords) {
	    String key = "TclBoolean -> TclDouble";
	    Integer num = (Integer) TclObject.objRecordMap.get(key);
	    if (num == null) {
	        num = new Integer(1);
	    } else {
	        num = new Integer(num.intValue() + 1);
	    }
	    TclObject.objRecordMap.put(key, num);
	}
    } else if (rep instanceof TclInteger) {
	/*
	 * Short-cut.
	 */

	int i = TclInteger.get(interp, tobj);

	// Can't convert from a "pure" integer
	// to a "pure" double since it is possible
	// that the expr code would want to check
	// for an integer that was converted to
	// a double. If a "pure" integer has no
	// string rep then generate one that
	// looks like an integer.

	if (tobj.hasNoStringRep()) {
	    tobj.toString();
	}

	tobj.setInternalRep(new TclDouble((double) i));

	if (TclObject.saveObjRecords) {
	    String key = "TclInteger -> TclDouble";
	    Integer num = (Integer) TclObject.objRecordMap.get(key);
	    if (num == null) {
	        num = new Integer(1);
	    } else {
	        num = new Integer(num.intValue() + 1);
	    }
	    TclObject.objRecordMap.put(key, num);
	}
    } else {
	tobj.setInternalRep(new TclDouble(interp, tobj.toString()));

	if (TclObject.saveObjRecords) {
	    String key = "TclString -> TclDouble";
	    Integer num = (Integer) TclObject.objRecordMap.get(key);
	    if (num == null) {
	        num = new Integer(1);
	    } else {
	        num = new Integer(num.intValue() + 1);
	    }
	    TclObject.objRecordMap.put(key, num);
	}
    }
}

/*
 *----------------------------------------------------------------------
 *
 * get --
 *
 *	Returns the double value of the object.
 *
 * Results:
 *	The double value of the object.
 *
 * Side effects:
 *	When successful, the internal representation of tobj is
 *	changed to TclDouble, if it is not already so.
 *
 *----------------------------------------------------------------------
 */

public static double
get(
    Interp interp,		// Current interpreter. May be null.
    TclObject tobj)		// The object to query.
throws
    TclException		// If the object does not have a TclDouble
				// representation and a conversion fails.
				// Error message will be left inside
				// the interp if it's not null.
{
    InternalRep rep = tobj.getInternalRep();
    TclDouble tdouble;

    if (!(rep instanceof TclDouble)) {
	setDoubleFromAny(interp, tobj);
	tdouble = (TclDouble) tobj.getInternalRep();
    } else {
	tdouble = (TclDouble) rep;
    }

    return tdouble.value;
}

/*
 *----------------------------------------------------------------------
 *
 * set --
 *
 *	Changes the double value of the object.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The internal representation of tobj is
 *	changed to TclDouble, if it is not already so.
 *
 *----------------------------------------------------------------------
 */

public static void
set(
    TclObject tobj,		// The object to modify.
    double d)			// The new value for the object. 
{
    tobj.invalidateStringRep();
    InternalRep rep = tobj.getInternalRep();

    if (rep instanceof TclDouble) {
	TclDouble tdouble = (TclDouble) rep;
	tdouble.value = d;
    } else {
	tobj.setInternalRep(new TclDouble(d));
    }
}

/*
 *----------------------------------------------------------------------
 *
 * toString --
 *
 *	Called to query the string representation of the Tcl
 *	object. This method is called only by TclObject.toString()
 *	when TclObject.stringRep is null.
 *
 * Results:
 * 	Returns the string representation of the TclDouble object.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public String
toString()
{
    return Util.printDouble(value);
}

} // end TclDouble

