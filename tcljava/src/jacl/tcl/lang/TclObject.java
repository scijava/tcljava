/*
 * TclObject.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclObject.java,v 1.7 2006/06/09 20:13:39 mdejong Exp $
 *
 */

package tcl.lang;

import java.util.Hashtable;

/**
 * This class extends TclObjectBase to implement the basic notion of
 * an object in Tcl.
 */

public final class TclObject extends TclObjectBase {

    static final boolean saveObjRecords = TclObjectBase.saveObjRecords;
    static Hashtable objRecordMap = TclObjectBase.objRecordMap;

    /**
     * Creates a TclObject with the given InternalRep. This method should be
     * called only by an InternalRep implementation.
     *
     * @param rep the initial InternalRep for this object.
     */
    public TclObject(final InternalRep rep) {
        super(rep);
    }

    /**
     * Creates a TclObject with the given InternalRep and stringRep.
     * This constructor is used by the TclString class only. No other place
     * should call this constructor.
     *
     * @param rep the initial InternalRep for this object.
     * @param s the initial string rep for this object.
     */
    protected TclObject(final TclString rep, final String s) {
        super(rep, s);
    }

    /**
     * Creates a TclObject with the given integer value.
     * This constructor is used by the TclInteger class only. No other place
     * should call this constructor.
     *
     * @param ivalue the integer value
     */
    protected TclObject(final int ivalue) {
        super(ivalue);
    }

    /**
     * Tcl_IncrRefCount -> preserve
     *
     * Increments the refCount to indicate the caller's intent to
     * preserve the value of this object. Each preserve() call must be matched
     * by a corresponding release() call. This method is Jacl specific
     * and is intended to be easily inlined in calling code.
     *
     * @exception TclRuntimeError if the object has already been deallocated.
     */
    public final void preserve() {
        if (refCount < 0) {
            throw DEALLOCATED;
        }
        refCount++;
    }

    /**
     * Tcl_DecrRefCount -> release
     *
     * Decrements the refCount to indicate that the caller is no longer
     * interested in the value of this object. If the refCount reaches 0,
     * the object will be deallocated. This method is Jacl specific
     * an is intended to be easily inlined in calling code.
     *
     * @exception TclRuntimeError if the object has already been deallocated.
     */
    public final void release() {
        if (--refCount <= 0) {
            disposeObject();
        }
    }

    /**
     * Return a String that describes TclObject and internal
     * rep type allocations and conversions. The string is
     * in lines separated by newlines. The saveObjRecords
     * needs to be set to true and Jacl recompiled for
     * this method to return a useful value.
     */

    public static String getObjRecords() {
        return TclObjectBase.getObjRecords();
    }
}

