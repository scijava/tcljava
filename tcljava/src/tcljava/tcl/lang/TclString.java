/*
 * TclList.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclString.java,v 1.1.1.1.10.2 2000/10/25 11:01:24 mdejong Exp $
 *
 */

package tcl.lang;

// This class implements the string object type in Tcl.

public class TclString implements InternalRep {

    // Used to perform "append" operations. After an append op,
    // sbuf.toString() will contain the latest value of the string and
    // tobj.stringRep will be set to null.

    private StringBuffer sbuf;

    private TclString() {
	sbuf = null;
    }

    private TclString(StringBuffer sb) {
	sbuf = sb;
    }

    /**
     * Returns a dupilcate of the current object.
     * @param obj the TclObject that contains this internalRep.
     */

    public InternalRep duplicate() {
	return new TclString();
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
	if (sbuf == null) {
	    return "";
	} else {
	    return sbuf.toString();
	}
    }

    /**
     * Create a new TclObject that has a string representation with
     * the given string value.
     */
    public static TclObject newInstance(String str) {
	return new TclObject(new TclString(), str);
    }

    /**
     * Create a new TclObject that has a string representation with
     * the string value specified 
     */
    static TclObject newInstance(StringBuffer sb) {
	return new TclObject(new TclString(sb));
    }

    static final TclObject newInstance(Object o) {
	return newInstance(o.toString());
    }

    /**
     * Create a TclObject with an internal TclString representation
     * whose initial value is a string with the single character.
     *
     * @param c initial value of the string.
     */

    static final TclObject newInstance(char c) {
	char charArray[] = new char[1];
	charArray[0] = c;
	return newInstance(new String(charArray));
    }

    /**
     * Called to convert the other object's internal rep to string.
     *
     * @param tobj the TclObject to convert to use the TclString internal rep.
     */
    private static void setStringFromAny(TclObject tobj) {
	InternalRep rep = tobj.getInternalRep();

	if (!(rep instanceof TclString)) {
	    // make sure that this object now has a valid string rep.

	    tobj.toString();

	    // Change the type of the object to TclString.

	    tobj.setInternalRep(new TclString());
	}
    }

    /*
     * public static String get(TclObject tobj) {;}
     *
     * There is no "get" class method for TclString representations.
     * Use tobj.toString() instead.
     */


    /**
     * Appends a string to a TclObject object. This method is equivalent to
     * Tcl_AppendToObj() in Tcl 8.0.
     *
     * @param tobj the TclObject to append a string to.
     * @param string the string to append to the object.
     * @return the TclObject whose string value is tobj.toString() + string.
     *     Could be different than tobj if tobj has refCount > 1.
     */
    public static final void append(TclObject tobj, String string) {	
	setStringFromAny(tobj);

	TclString tstr = (TclString) tobj.getInternalRep();
	if (tstr.sbuf == null) {
	    tstr.sbuf = new StringBuffer(tobj.toString());
	}
	tobj.invalidateStringRep();
	tstr.sbuf.append(string);
    }

    /**
     * Appends ab object to a TclObject object. This method is equivalent to
     * Tcl_AppendToObj() in Tcl 8.0.
     *
     * The type of the TclObject will be a TclString that contains the
     * string value:
     *		tobj.toString() + tobj2.toString();
     */
    static final void append(TclObject tobj, TclObject tobj2) {
	append(tobj, tobj2.toString());
    }
}

