/* 
 * NativeLock.java --
 *
 *	This class is only used by the native code to act as a global
 *	lock for all of the native routines.
 *
 * Copyright (c) 1998 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: NativeLock.java,v 1.1 1998/10/14 21:09:10 cvsadmin Exp $
 */

package tcl.lang;

class NativeLock {
/*
 * There are no methods or fields for this class. Its only use is as an
 * object for synchronization.  Every native method in Tcl Blend should
 * grab the monitor for this class before doing anything that would
 * require locking.
 */
} // end NativeLock

