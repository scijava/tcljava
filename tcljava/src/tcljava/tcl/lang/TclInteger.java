/*
 * TclInteger.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclInteger.java,v 1.9 2006/01/14 01:29:26 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the integer object type in Tcl.
 */

public class TclInteger implements InternalRep {
    /**
     * Internal representation of a integer value.
     */
    private int value;

    /**
     * Construct a TclInteger representation with the given integer value.
     */
    private TclInteger(int i) {
	value = i;

	if (TclObject.saveObjRecords) {
	    String key = "TclInteger";
	    Integer num = (Integer) TclObject.objRecordMap.get(key);
	    if (num == null) {
	        num = new Integer(1);
	    } else {
	        num = new Integer(num.intValue() + 1);
	    }
	    TclObject.objRecordMap.put(key, num);
	}
    }

    /**
     * Construct a TclInteger representation with the initial value taken
     * from the given string.
     *
     * @param interp current interpreter.
     * @param str string rep of the integer.
     * @exception TclException if the string is not a well-formed Tcl integer
     *    value.
     */
    private TclInteger(Interp interp, String str) throws TclException {
	value = Util.getInt(interp, str);

	if (TclObject.saveObjRecords) {
	    String key = "TclInteger";
	    Integer num = (Integer) TclObject.objRecordMap.get(key);
	    if (num == null) {
	        num = new Integer(1);
	    } else {
	        num = new Integer(num.intValue() + 1);
	    }
	    TclObject.objRecordMap.put(key, num);
	}
    }

    /**
     * Returns a dupilcate of the current object.
     */
    public InternalRep duplicate() {
	if (TclObject.saveObjRecords) {
	    String key = "TclInteger.duplicate()";
	    Integer num = (Integer) TclObject.objRecordMap.get(key);
	    if (num == null) {
	        num = new Integer(1);
	    } else {
	        num = new Integer(num.intValue() + 1);
	    }
	    TclObject.objRecordMap.put(key, num);
	}

	return new TclInteger(value);
    }

    /**
     * Implement this no-op for the InternalRep interface.
     */

    public void dispose() {}

    /**
     * Called to query the string representation of the Tcl object. This
     * method is called only by TclObject.toString() when
     * TclObject.stringRep is null.
     *
     * @return the string representation of the Tcl object.
     */
    public String toString() {
	return Integer.toString(value);
    }

    /**
     * Tcl_NewIntObj -> TclInteger.newInstance
     *
     * Creates a new instance of a TclObject with a TclInteger internal
     * representation.
     *
     * @param b initial value of the integer object.
     * @return the TclObject with the given integer value.
     */

    public static TclObject newInstance(int i) {
	return new TclObject(new TclInteger(i));
    }

    /**
     * SetIntFromAny -> TclInteger.setIntegerFromAny
     *
     * Called to convert the other object's internal rep to this type.
     *
     * @param interp current interpreter.
     * @param forIndex true if this methid is called by getForIndex.
     * @param tobj the TclObject to convert to use the
     *     representation provided by this class.
     */

    private static void setIntegerFromAny(Interp interp, TclObject tobj)
	    throws TclException {
	InternalRep rep = tobj.getInternalRep();

	if (rep instanceof TclInteger) {
	    // Do nothing.
	} else if (rep instanceof TclBoolean) {
	    boolean b = TclBoolean.get(interp, tobj);
	    if (tobj.hasNoStringRep()) {
		// A "pure" boolean can be converted
		// directly to an integer.
		tobj.setInternalRep(new TclInteger(b ? 1 : 0));
	    } else if (b) {
		// The integer "2" would be converted to
		// a true boolean value. Converting it
		// back to an integer should not return
		// the value 1. If the string rep is "1"
                // then take the shortcut. Otherwise,
                // reparse the integer from the string.
		TclInteger irep;
		String srep = tobj.toString();
		if (srep.compareTo("1") == 0) {
		    irep = new TclInteger(1);
		} else {
		    irep = new TclInteger(interp, srep);
		}
		tobj.setInternalRep(irep);
	    } else {
		// A boolean false value can be converted
		// directly to the integer value 0.
		tobj.setInternalRep(new TclInteger(0));
	    }

	    if (TclObject.saveObjRecords) {
	        String key = "TclBoolean -> TclInteger";
	        Integer num = (Integer) TclObject.objRecordMap.get(key);
	        if (num == null) {
	            num = new Integer(1);
	        } else {
	            num = new Integer(num.intValue() + 1);
	        }
	        TclObject.objRecordMap.put(key, num);
	    }
	} else {
	    // Note that conversion from a double to an
	    // integer internal rep should always raise
	    // an error.

	    tobj.setInternalRep(new TclInteger(interp, tobj.toString()));

	    if (TclObject.saveObjRecords) {
	        String key = "TclString -> TclInteger";
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

    /**
     * Tcl_GetIntFromObj -> TclInteger.get
     *
     * Returns the integer value of the object.
     *
     * @param interp current interpreter.
     * @param tobj the object to operate on.
     * @return the integer value of the object.
     */

    public static int get(Interp interp, TclObject tobj)
	    throws TclException {
	InternalRep rep = tobj.getInternalRep();
	TclInteger tint;

	if (!(rep instanceof TclInteger)) {
	    setIntegerFromAny(interp, tobj);
	    tint = (TclInteger) tobj.getInternalRep();
	} else {
	    tint = (TclInteger) rep;
	}
	return tint.value;
    }

    /**
     * Changes the integer value of the object.
     *
     * @param interp current interpreter.
     * @param tobj the object to operate on.
     * @param i the new integer value.
     */
    public static void set(TclObject tobj, int i) {
	tobj.invalidateStringRep();
	InternalRep rep = tobj.getInternalRep();
	TclInteger tint;

	if (rep instanceof TclInteger) {
	    tint = (TclInteger)rep;
	    tint.value = i;
	} else {
	    tobj.setInternalRep(new TclInteger(i));
	}
    }

    /**
     * Increments the integer value of the object by the given
     * amount. One could implement this same operation by
     * calling get() and then set(), this method provides an
     * optimized implementation. This method is not public
     * since it will only be invoked by the incr command.
     *
     * @param interp current interpreter.
     * @param tobj the object to operate on.
     * @param incrAmount amount to increment
     */
    static void incr(
        Interp interp,
        TclObject tobj,
        int incrAmount)
            throws TclException
    {
	InternalRep rep = tobj.getInternalRep();

	if (!(rep instanceof TclInteger)) {
	    setIntegerFromAny(interp, tobj);
	    rep = tobj.getInternalRep();
	}
	tobj.invalidateStringRep();
	TclInteger tint = (TclInteger) rep;
	tint.value += incrAmount;
    }
}

