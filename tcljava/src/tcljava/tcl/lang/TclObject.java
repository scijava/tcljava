/*
 * TclObject.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclObject.java,v 1.3.10.1 2000/10/25 09:38:53 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the basic notion of an "object" in Tcl. The
 * fundamental representation of an object is its string value. However,
 * an object can also have an internal representation, which is a "cached"
 * reprsentation of this object in another form. The type of the internal
 * rep of Tcl objects can mutate. This class provides the storage of the
 * string rep and the internal rep, as well as the facilities for mutating
 * the internal rep.
 */

public final class TclObject {
    // Internal representation of the object.

    protected InternalRep internalRep;

    // Reference count of this object. When 0 the object will be deallocated.

    protected int refCount;

    // String  representation of the object.

    protected String stringRep;

    /**
     * Creates a TclObject with the given InternalRep. This method should be
     * called only by an InternalRep implementation.
     *
     * @param rep the initial InternalRep for this object.
     */
    public TclObject(InternalRep rep) {
	internalRep = rep;
	stringRep = null;
	refCount = 0;
    }

    /**
     * Creates a TclObject with the given InternalRep and stringRep.
     * This constructor is used by the TclString class only. No other place
     * should call this constructor.
     *
     * @param rep the initial InternalRep for this object.
     * @param s the initial string rep for this object.
     */
    protected TclObject(TclString rep, String s) {
	internalRep = rep;
	stringRep = s;
	refCount = 0;
    }

    /**
     * Returns the handle to the current internal rep. This method should be
     * called only by an InternalRep implementation.
     *
     * @return the handle to the current internal rep.
     */
    public final InternalRep getInternalRep() {
	return internalRep;
    }

    /**
     * Change the internal rep of the object. The old internal rep
     * will be deallocated as a result. This method should be
     * called only by an InternalRep implementation.
     *
     * @param rep the new internal rep.
     */
    public final void setInternalRep(InternalRep rep) {
	if (rep == internalRep) {
	    return;
	}

	// In the special case where the internal representation is a CObject,
	// we want to call the special interface to convert the underlying
	// native object into a reference to the Java TclObject.  Note that
	// this test will always fail if we are not using the native
	// implementation.

	if ((internalRep instanceof CObject) && !(rep instanceof CObject)) {
	    // We must ensure that the string rep is copied into Java
	    // before we lose the reference to the underlying CObject.
	    // Otherwise we will lose the original string information
	    // when the backpointer is lost.

	    if (stringRep == null) {
		stringRep = internalRep.toString();
	    }
	    ((CObject) internalRep).makeReference(this);
	}
	internalRep.dispose();
	internalRep = rep;
    }

    /**
     * Returns the string representation of the object.
     *
     * @return the string representation of the object.
     */
    public final String toString() {
	if (stringRep == null) {
	    stringRep = internalRep.toString();
	}
	return stringRep;
    }

    /**
     * Sets the string representation of the object to null.  Next
     * time when toString() is called, getInternalRep().toString() will
     * be called. This method should be called ONLY when an InternalRep
     * is about to modify the value of a TclObject.
     *
     * @exception TclRuntimeError if object is not exclusively owned.
     */
    public final void invalidateStringRep() throws TclRuntimeError {
	if (refCount > 1) {
	    throw new TclRuntimeError("string representation of object \"" +
		    toString() + "\" cannot be invalidated: refCount = " +
		    refCount);
	}
	stringRep = null;
    }

    /**
     * Returns true if the TclObject is shared, false otherwise.
     * @return true if the TclObject is shared, false otherwise.
     */
    public final boolean isShared() {
	return (refCount > 1);
    }

    /**
     * Takes exclusive ownership of this object. This method should be
     * called before invoking any method that will modify the value of
     * the object. E.g.
     *
     *		tobj = tobj.takeExclusive();
     *		TclString.append(tobj, "hello");
     *
     * The result of this method depends on the refCount of the object:
     *
     *     refCount == 1: the object itself is returned.
     *     refCount >  1: a copy of the object will be returned. The refCount
     *		          of the copy is set to 1.
     *     refCount <  1: TclRuntimeError will be thrown.
     *
     * @return an TclObject with a refCount of 1.
     * @exception TclRuntimeError if the refCount <= 0
     */
    public final TclObject takeExclusive() throws TclRuntimeError {
	if (refCount == 1) {
	    return this;
	} else if (refCount > 1) {
	    if (internalRep instanceof TclString) {
		if (stringRep == null) {
		    stringRep = internalRep.toString();
		}
	    }
	    TclObject newObj = new TclObject(internalRep.duplicate());
	    newObj.stringRep = this.stringRep;
	    newObj.refCount = 1;
	    refCount--;
	    return newObj;
	} else {
	    throw new TclRuntimeError("takeExclusive() called on object \"" +
		    toString() + "\" with: refCount = 0");
	}
    }

    /**
     * Tcl_IncrRefCount -> preserve
     *
     * Increments the refCount to indicate the caller's intent to
     * preserve the value of this object. Each preserve() call must be matched
     * by a corresponding release() call.
     *
     * @exception TclRuntimeError if the object has already been deallocated.
     */
    public final void preserve() throws TclRuntimeError {
	if (internalRep == null) {
	    throw new TclRuntimeError("Attempting to preserve object " +
		    "after it was deallocated");
	} 
	refCount++;
    }

    /**
     * Tcl_DecrRefCount -> release
     *
     * Decrements the refCount to indicate that the caller is no longer
     * interested in the value of this object. If the refCount reaches 0,
     * the obejct will be deallocated.
     */
    public final void release() {
	refCount--;

	if (refCount <= 0) {
	    internalRep.dispose();

	    // Setting these to null will ensure that any attempt to use
	    // this object will result in a Java NullPointerException.

	    internalRep = null;
	    stringRep = null;
	}
    }

    /**
     * Returns the refCount of this object. This is used only fot debugging
     * purposes by JavainfoCmd.
     *
     * @return refCount.
     */
    protected final int getRefCount() {
	return refCount;
    }
}

