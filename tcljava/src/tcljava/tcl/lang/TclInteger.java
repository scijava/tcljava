/*
 * TclInteger.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclInteger.java,v 1.4.2.1 2000/10/25 11:01:24 mdejong Exp $
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
    }

    /**
     * Returns a dupilcate of the current object.
     * @param obj the TclObject that contains this internalRep.
     */
    public InternalRep duplicate() {
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
	    if (b) {
		tobj.setInternalRep(new TclInteger(1));
	    } else {
		tobj.setInternalRep(new TclInteger(0));
	    }
	} else {
	    // (ToDo) other short-cuts

	    tobj.setInternalRep(new TclInteger(interp, tobj.toString()));
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
	setIntegerFromAny(interp, tobj);
	TclInteger tint = (TclInteger) tobj.getInternalRep();
	return tint.value;
    }

    /**
     * Changes the integer value of the object.
     *
     * @param interp current interpreter.
     * @param tobj the object to operate on.
     * @paran i the new integer value.
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
}

