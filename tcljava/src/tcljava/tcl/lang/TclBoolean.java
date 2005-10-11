/*
 * TclBoolean.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclBoolean.java,v 1.4 2005/10/11 20:03:23 mdejong Exp $
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
     * Returns a dupilcate of the current object.
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
     * SetBooleanFromAny -> setBooleanFromAny
     *
     * Called to convert the other object's internal rep to boolean.
     *
     * @param interp current interpreter.
     * @param tobj the TclObject to convert to use the
     *     representation provided by this class.
     */
    private static void setBooleanFromAny(Interp interp, TclObject tobj)
	    throws TclException {
	InternalRep rep = tobj.getInternalRep();
	// Get the string representation. Make it up-to-date if necessary.
	String string = tobj.toString();

	if (rep instanceof TclBoolean) {
	    // Do nothing.
	} else if (rep instanceof TclInteger) {
	    int i = TclInteger.get(interp, tobj);
	    tobj.setInternalRep(new TclBoolean(i != 0));

	    if (TclObject.saveObjRecords) {
	        String key = "TclInteger -> TclBoolean";
	        Integer num = (Integer) TclObject.objRecordMap.get(key);
	        if (num == null) {
	            num = new Integer(1);
	        } else {
	            num = new Integer(num.intValue() + 1);
	        }
	        TclObject.objRecordMap.put(key, num);
	    }
	} else if (rep instanceof TclDouble) {
	    double d = TclDouble.get(interp, tobj);
	    tobj.setInternalRep(new TclBoolean(d != 0.0));

	    if (TclObject.saveObjRecords) {
	        String key = "TclDouble -> TclBoolean";
	        Integer num = (Integer) TclObject.objRecordMap.get(key);
	        if (num == null) {
	            num = new Integer(1);
	        } else {
	            num = new Integer(num.intValue() + 1);
	        }
	        TclObject.objRecordMap.put(key, num);
	    }
	} else {
	    // Copy the string converting its characters to lower case.

	    string = string.toLowerCase();
	    String lowerCase = string.toLowerCase();

	    // Parse the string as a boolean. We use an implementation here that
	    // doesn't report errors in interp if interp is null.

	    boolean b;
	    boolean badBoolean = false;

	    try {
	        b = Util.getBoolean(interp, lowerCase);
	    } catch (TclException te) {
	        // Boolean values can be extracted from ints or doubles.  Note
	        // that we don't use strtoul or strtoull here because we don't
	        // care about what the value is, just whether it is equal to
	        // zero or not.

                badBoolean = true;
                b = false; // Always reassigned below
                if (interp != null) {
                    interp.resetResult();
                }

	        try {
                    b = (Util.getInt(interp, lowerCase) != 0);
                    badBoolean = false;
	        } catch (TclException te2) {}

                if (badBoolean) {
	            try {
                        b = (Util.getDouble(interp, lowerCase) != 0.0);
                        badBoolean = false;
	            } catch (TclException te2) {}
                }
	    }
            if (badBoolean) {
	        if (interp != null) {
	            interp.resetResult();
	        }
	        throw new TclException(interp,
	            "expected boolean value but got \"" +
	            string + "\"");
            }
	    if (b) {
	        tobj.setInternalRep(new TclBoolean(true));
	    } else {
	        tobj.setInternalRep(new TclBoolean(false));
	    }

	    if (TclObject.saveObjRecords) {
	        String key = "TclString -> TclBoolean";
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
	TclBoolean tbool = (TclBoolean) tobj.getInternalRep();
	return tbool.value;
    }
}

