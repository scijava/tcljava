/* 
 * IdleHandler.java --
 *
 *	This class is used to create handlers for idle events.
 *
 * Copyright (c) 1998 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: IdleHandler.java,v 1.1 1998/10/14 21:09:10 cvsadmin Exp $
 */

package tcl.lang;

abstract public class IdleHandler {

/*
 * True if the cancel() method has been called.
 */

boolean isCancelled;

/*
 *----------------------------------------------------------------------
 *
 * IdleHandler --
 *
 *	Create a idle handler to be fired when the notifier is idle.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The idle is registered in the list of idle handlers in the
 *	given notifier. When the notifier is idle, the
 *	processIdleEvent() method will be invoked exactly once inside
 *	the primary thread of the notifier.
 *
 *----------------------------------------------------------------------
 */

public
IdleHandler(
    Notifier n)			// The notifier to fire the event.
{
    isCancelled = false;
    doWhenIdle();
}

/*
 *----------------------------------------------------------------------
 *
 * cancel --
 *
 *	Mark this idle handler as cancelled so that it won't be invoked.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The idle handler is marked as cancelled so that its
 *	processIdleEvent() method will not be called. If the idle
 *	event has already fired, then nothing this call has no effect.
 *
 *----------------------------------------------------------------------
 */

public synchronized void
cancel()
{
    if (isCancelled) {
	return;
    }

    isCancelled = true;
    cancelIdleCall();
}

/*
 *----------------------------------------------------------------------
 *
 * invoke --
 *
 *	Execute the idle handler if it has not been cancelled. This
 *	method should be called by the notifier only.
 *
 *	Because the idle handler may be being cancelled by another
 *	thread, both this method and cancel() must be synchronized to
 *	ensure correctness.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The idle handler may have arbitrary side effects.
 *
 *----------------------------------------------------------------------
 */

synchronized final void
invoke()
{
    /*
     * The idle handler may be cancelled after it was registered in
     * the notifier. Check the isCancelled field to make sure it's not
     * cancelled.
     */

    if (!isCancelled) {
	processIdleEvent();
    }
}

/*
 *----------------------------------------------------------------------
 *
 * processIdleEvent --
 *
 *	This method is called when the idle is expired. Override
 *	This method to implement your own idle handlers.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	It can do anything.
 *
 *----------------------------------------------------------------------
 */

abstract public void
processIdleEvent();

/*
 *----------------------------------------------------------------------
 *
 * doWhenIdle --
 *
 *	Create a C level idle handler for this object.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private final native void
doWhenIdle();

/*
 *----------------------------------------------------------------------
 *
 * cancelIdleCall --
 *
 *	Delete a C level idle handler.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private final native void
cancelIdleCall();

} // end IdleHandler

