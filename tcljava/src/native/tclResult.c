/* 
 * tclResult.c --
 *
 *	This file contains code to manage the interpreter result.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: tclResult.c,v 1.1 1998/10/14 21:09:18 cvsadmin Exp $
 */

#include "java.h"
#include "tclInt.h"


/*
 *----------------------------------------------------------------------
 *
 * Tcl_SaveResult --
 *
 *      Takes a snapshot of the current result state of the interpreter.
 *      The snapshot can be restored at any point by
 *      Tcl_RestoreResult. Note that this routine does not 
 *	preserve the errorCode, errorInfo, or flags fields so it
 *	should not be used if an error is in progress.
 *
 *      Once a snapshot is saved, it must be restored by calling
 *      Tcl_RestoreResult, or discarded by calling
 *      Tcl_DiscardResult.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Resets the interpreter result.
 *
 *----------------------------------------------------------------------
 */

TCLBLEND_EXTERN void
Tcl_SaveResult(interp, statePtr)
    Tcl_Interp *interp;		/* Interpreter to save. */
    Tcl_SavedResult *statePtr;	/* Pointer to state structure. */
{
    Interp *iPtr = (Interp *) interp;

    /*
     * Move the result object into the save state.  Note that we don't need
     * to change its refcount because we're moving it, not adding a new
     * reference.  Put an empty object into the interpreter.
     */

    statePtr->objResultPtr = iPtr->objResultPtr;
    iPtr->objResultPtr = Tcl_NewObj(); 
    Tcl_IncrRefCount(iPtr->objResultPtr); 

    /*
     * Save the string result. 
     */

    statePtr->freeProc = iPtr->freeProc;
    if (iPtr->result == iPtr->resultSpace) {
	/*
	 * Copy the static string data out of the interp buffer.
	 */

	statePtr->result = statePtr->resultSpace;
	strcpy(statePtr->result, iPtr->result);
    } else if (iPtr->result == iPtr->appendResult) {
	/*
	 * Move the append buffer out of the interp.
	 */

	statePtr->appendResult = iPtr->appendResult;
	statePtr->appendAvl = iPtr->appendAvl;
	statePtr->appendUsed = iPtr->appendUsed;
	statePtr->result = statePtr->appendResult;
	iPtr->appendResult = NULL;
	iPtr->appendAvl = 0;
	iPtr->appendUsed = 0;
    } else {
	/*
	 * Move the dynamic or static string out of the interpreter.
	 */

	statePtr->result = iPtr->result;
	statePtr->appendResult = NULL;
    }

    iPtr->result = iPtr->resultSpace;
    iPtr->resultSpace[0] = 0;
    iPtr->freeProc = 0;
}

/*
 *----------------------------------------------------------------------
 *
 * Tcl_RestoreResult --
 *
 *      Restores the state of the interpreter to a snapshot taken
 *      by Tcl_SaveResult.  After this call, the token for
 *      the interpreter state is no longer valid.
 *
 * Results:
 *      None.
 *
 * Side effects:
 *      Restores the interpreter result.
 *
 *----------------------------------------------------------------------
 */

TCLBLEND_EXTERN void
Tcl_RestoreResult(interp, statePtr)
    Tcl_Interp* interp;		/* Interpreter being restored. */
    Tcl_SavedResult *statePtr;	/* State returned by Tcl_SaveResult. */
{
    Interp *iPtr = (Interp *) interp;

    Tcl_ResetResult(interp);

    /*
     * Restore the string result.
     */

    iPtr->freeProc = statePtr->freeProc;
    if (statePtr->result == statePtr->resultSpace) {
	/*
	 * Copy the static string data into the interp buffer.
	 */

	iPtr->result = iPtr->resultSpace;
	strcpy(iPtr->result, statePtr->result);
    } else if (statePtr->result == statePtr->appendResult) {
	/*
	 * Move the append buffer back into the interp.
	 */

	if (iPtr->appendResult != NULL) {
	    ckfree((char *)iPtr->appendResult);
	}

	iPtr->appendResult = statePtr->appendResult;
	iPtr->appendAvl = statePtr->appendAvl;
	iPtr->appendUsed = statePtr->appendUsed;
	iPtr->result = iPtr->appendResult;
    } else {
	/*
	 * Move the dynamic or static string back into the interpreter.
	 */

	iPtr->result = statePtr->result;
    }

    /*
     * Restore the object result.
     */

    Tcl_DecrRefCount(iPtr->objResultPtr);
    iPtr->objResultPtr = statePtr->objResultPtr;
}

/*
 *----------------------------------------------------------------------
 *
 * Tcl_DiscardResult --
 *
 *      Frees the memory associated with an interpreter snapshot
 *      taken by Tcl_SaveResult.  If the snapshot is not
 *      restored, this procedure must be called to discard it,
 *      or the memory will be lost.
 *
 * Results:
 *      None.
 *
 * Side effects:
 *      None.
 *
 *----------------------------------------------------------------------
 */

void
Tcl_DiscardResult(statePtr)
    Tcl_SavedResult *statePtr;	/* State returned by Tcl_SaveResult. */
{
    Tcl_DecrRefCount(statePtr->objResultPtr);

    if (statePtr->result == statePtr->appendResult) {
	ckfree(statePtr->appendResult);
    } else if (statePtr->freeProc) {
	if ((statePtr->freeProc == TCL_DYNAMIC)
	        || (statePtr->freeProc == (Tcl_FreeProc *) free)) {
	    ckfree(statePtr->result);
	} else {
	    (*statePtr->freeProc)(statePtr->result);
	}
    }
}


