/* 
 * TimerHandler.java --
 *
 *	This class is used to create handlers for timer events.
 *
 * Copyright (c) 1998 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: TimerHandler.java,v 1.1 1998/10/14 21:09:10 cvsadmin Exp $
 */

package tcl.lang;

abstract public class TimerHandler {

/*
 * True if the cancel() method has been called.
 */

boolean isCancelled;

/*
 * C level Tcl_TimerToken handle.
 */

long token;


/*
 *----------------------------------------------------------------------
 *
 * TimerHandler --
 *
 *	Create a timer handler to be fired after the given time lapse.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The timer is registered in the list of timers in the given
 *	notifier. After milliseconds have elapsed, the
 *	processTimerEvent() method will be invoked exactly once inside
 *	the primary thread of the notifier.
 *
 *----------------------------------------------------------------------
 */

public
TimerHandler(
    Notifier n,			// The notifier to fire the event.
    int milliseconds)		// How many milliseconds to wait
				// before invoking processTimerEvent().
{
    isCancelled = false;
    token = createTimerHandler(milliseconds);
}

/*
 *----------------------------------------------------------------------
 *
 * cancel --
 *
 *	Mark this timer handler as cancelled so that it won't be
 *	invoked.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The timer handler is marked as cancelled so that its
 *	processTimerEvent() method will not be called. If the timer
 *	has already fired, then nothing this call has no effect.
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

    deleteTimerHandler(token);
}

/*
 *----------------------------------------------------------------------
 *
 * invoke --
 *
 *	Execute the timer handler if it has not been cancelled. This
 *	method should be called by the notifier only.
 *
 *	Because the timer handler may be being cancelled by another
 *	thread, both this method and cancel() must be synchronized to
 *	ensure correctness.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The timer handler may have arbitrary side effects.
 *
 *----------------------------------------------------------------------
 */

synchronized final void
invoke()
{
    /*
     * The timer may be cancelled after it was put on the
     * event queue. Check its isCancelled field to make sure it's
     * not cancelled.
     */

    if (!isCancelled) {
	processTimerEvent();
    }
    token = 0;
}

/*
 *----------------------------------------------------------------------
 *
 * processTimerEvent --
 *
 *	This method is called when the timer is expired. Override
 *	This method to implement your own timer handlers.
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
processTimerEvent();

/*
 *----------------------------------------------------------------------
 *
 * createTimerHandler --
 *
 *	Create a C level timer handler for this object.
 *
 * Results:
 *	Returns a Tcl_TimerToken.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private final native long
createTimerHandler(
    int milliseconds);		// How many milliseconds to wait
				// before calling invoke().

/*
 *----------------------------------------------------------------------
 *
 * deleteTimerHandler --
 *
 *	Delete a C level timer handler.
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
deleteTimerHandler(
    long token);		// Tcl_TimerToken for timer to delete.

} // end TimerHandler

