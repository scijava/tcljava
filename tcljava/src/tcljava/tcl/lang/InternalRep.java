/*
 * InternalRep.java
 *
 *	This file contains the abstract class declaration for the
 *	internal representations of TclObjects.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: InternalRep.java,v 1.1 1998/10/14 21:09:14 cvsadmin Exp $
 *
 */

package tcl.lang;

/*
 * This is the interface for implementing internal representation of Tcl
 * objects.  A class that implements InternalRep should define the
 * following:
 *
 * (1) the three abstract methods specified in this base class:
 *		dispose()
 *		duplicate()
 *	   	getName()
 *
 * (2) The method toString()
 *
 * (3) class method(s) newInstance() if appropriate
 *
 * (4) class method set<Type>FromAny() if appropriate
 *
 * (5) class method get() if appropriate
 */

abstract public class InternalRep {

/*
 *----------------------------------------------------------------------
 *
 * dispose --
 *
 *	Free any state associated with the object's internal rep.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Leaves the object in an unusable state.
 *
 *----------------------------------------------------------------------
 */

protected void
dispose()
{
    /*
     * The default implementation does nothing.
     */
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

abstract protected InternalRep duplicate();

} // end InternalRep

