/*
 * TclBoolean.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclBoolean.java,v 1.1.1.1.10.1 2000/10/25 11:01:24 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the boolean object type in Tcl.
 */

public class TclBoolean implements InternalRep {
    /**
     * Internal representation of a boolean value.
     */
    private boolean value;

    /**
     * Construct a TclBoolean representation with the given boolean value.
     *
     * @param b initial boolean value.
     */
    private TclBoolean(boolean b) {
	value = b;
    }

    /**
     * Construct a TclBoolean representation with the initial value taken
     * from the given string.
     *
     * @param interp current interpreter.
     * @exception TclException if the string is not a well-formed Tcl boolean
     *    value.
     */
    private TclBoolean(Interp interp, String str) throws TclException {
	value = Util.getBoolean(interp, str);
    }

    /**
     * Returns a dupilcate of the current object.
     *
     * @param tobj the TclObject that contains this ObjType.
     */
    public InternalRep duplicate() {
	return new TclBoolean(value);
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
	if (value) {
	    return "1";
	} else {
	    return "0";
	}
    }

    /**
     * Creates a new instance of a TclObject with a TclBoolean internal
     * representation.
     *
     * @param b initial value of the boolean object.
     * @return the TclObject with the given boolean value.
     */

    public static TclObject newInstance(boolean b) {
	return new TclObject(new TclBoolean(b));
    }

    /**
     * Called to convert the other object's internal rep to boolean.
     *
     * @param interp current interpreter.
     * @param tobj the TclObject to convert to use the
     *     representation provided by this class.
     */
    private static void setBooleanFromAny(Interp interp, TclObject tobj)
	    throws TclException {
	InternalRep rep = tobj.getInternalRep();

	if (rep instanceof TclBoolean) {
	    /*
	     * Do nothing.
	     */
	} else if (rep instanceof TclInteger) {
	    int i = TclInteger.get(interp, tobj);
	    tobj.setInternalRep(new TclBoolean(i != 0));
	} else {
	    /*
	     * (ToDo) other short-cuts
	     */
	    tobj.setInternalRep(new TclBoolean(interp, tobj.toString()));
	}
    }

    /**
     * Returns the value of the object as an boolean.
     *
     * @param interp current interpreter.
     * @param tobj the TclObject to use as an boolean.
     * @return the boolean value of the object.
     * @exception TclException if the object cannot be converted into a
     *     boolean.
     */
    public static boolean get(Interp interp, TclObject tobj)
	    throws TclException {
	setBooleanFromAny(interp, tobj);
	TclBoolean tbool = (TclBoolean)(tobj.getInternalRep());
	return tbool.value;
    }
}

