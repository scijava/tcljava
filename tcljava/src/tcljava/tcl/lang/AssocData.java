/*
 * AssocData.java --
 *
 *	The API for registering named data objects in the Tcl
 *	interpreter.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: AssocData.java,v 1.1 1998/10/14 21:09:14 cvsadmin Exp $
 *
 */

package tcl.lang;

/*
 * This interface is the API for registering named data objects in the
 * Tcl interpreter.
 */

public interface AssocData {


/*
 *----------------------------------------------------------------------
 *
 * disposeAssocData --
 *
 *	This method is called when the interpreter is destroyed or
 *	when Interp.deleteAssocData is called on a registered
 *	AssocData instance.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	This method may cause any arbitrary side effects.
 *
 *----------------------------------------------------------------------
 */

public void
disposeAssocData(
    Interp interp);		// The interpreter in which this AssocData
				// instance is registered in.

}

