/* 
 * javaNotifier.c --
 *
 *	 This file contains the native methods for the Notifier class.
 *
 * Copyright (c) 1998 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: javaNotifier.c,v 1.2.2.1 2000/07/30 07:17:09 mo Exp $
 */

#include "java.h"
#include "javaNative.h"

/*
 * Global Notifier object.
 */

static jobject globalNotifierObj;
static int eventQueued = 0;

/*
 * Declarations for functions used only in this file.
 */

static int	JavaEventProc(Tcl_Event *evPtr, int flags);
static void	NotifierCheck(ClientData data, int flags);
static void	NotifierSetup(ClientData data, int flags);

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Notifier_init --
 *
 *	Initialize the Java Notifier event source.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

void JNICALL
Java_tcl_lang_Notifier_init(
    JNIEnv *env,		/* Java environment. */
    jobject notifierObj)	/* Handle to Notifier object. */
{

    /* If we segfault near here under Windows, try removing tclblend.dll
     * from the current directory.  Tcl Blend has problems loading
     * dlls from a remote directory if there is a dll with the
     * same name in the local directory.
     */
    Tcl_CreateEventSource(NotifierSetup, NotifierCheck, NULL);
    JavaInitNotifier();
    globalNotifierObj = (*env)->NewGlobalRef(env, notifierObj);
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Notifier_dispose --
 *
 *	Clean up the Java Notifier event source.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

void JNICALL
Java_tcl_lang_Notifier_dispose(
    JNIEnv *env,		/* Java environment. */
    jobject notifierObj)	/* Handle to Notifier object. */
{
    Tcl_DeleteEventSource(NotifierSetup, NotifierCheck, NULL);
    JavaDisposeNotifier();
    (*env)->DeleteGlobalRef(env, globalNotifierObj);
    globalNotifierObj = NULL;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Notifier_doOneEvent --
 *
 *	Process one event from the event queue.
 *
 * Results:
 *	Returns the result of Tcl_DoOneEvent().
 *
 * Side effects:
 *	May invoke arbitrary code.
 *
 *----------------------------------------------------------------------
 */

jint JNICALL
Java_tcl_lang_Notifier_doOneEvent(
    JNIEnv *env,		/* Java environment. */
    jobject notifierObj,	/* Handle to Notifier object. */
    jint flags)			/* Miscellaneous flag values: may be any
				 * combination of TCL.DONT_WAIT,
				 * TCL.WINDOW_EVENTS, TCL.FILE_EVENTS,
				 * TCL.TIMER_EVENTS, TCL.IDLE_EVENTS,
				 * or others defined by event sources. */
{
    int result;

    result = Tcl_DoOneEvent(flags);
    return result;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Notifier_alertNotifier --
 *
 *	Wake up the Tcl Notifier so it can check the event sources.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

void JNICALL
Java_tcl_lang_Notifier_alertNotifier(
    JNIEnv *env,		/* Java environment. */
    jobject notifierObj)	/* Handle to Notifier object. */
{
    /*
     * This interface is intended to be called from other threads,
     * so we should not grab the monitor.
     */

    JavaAlertNotifier();
}

/*
 *----------------------------------------------------------------------
 *
 * NotifierSetup --
 *
 *	This routine checks to see if there are any events on the
 *	Java Notifier queue.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	May set the block time to 0.
 *
 *----------------------------------------------------------------------
 */

static void
NotifierSetup(
    ClientData data,		/* Not used. */
    int flags)			/* Same as for Tcl_DoOneEvent. */
{
    JNIEnv *env = JavaGetEnv(NULL);

    if ((*env)->CallBooleanMethod(env, globalNotifierObj, java.hasEvents)
	    == JNI_TRUE) {
	Tcl_Time timeout = { 0, 0 };
	Tcl_SetMaxBlockTime(&timeout);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * NotifierCheck --
 *
 *	This routine checks to see if there are any events on the
 *	Java Notifier queue.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	May queue a JavaEvent on the event queue.
 *
 *----------------------------------------------------------------------
 */

static void
NotifierCheck(
    ClientData data,		/* Not used. */
    int flags)			/* Same as for Tcl_DoOneEvent. */
{
    JNIEnv *env = JavaGetEnv(NULL);
    Tcl_Event *ePtr;

    /*
     * Only queue a new event if there isn't already one queued and
     * there are events on the Java event queue.
     */

    if (!eventQueued && (*env)->CallBooleanMethod(env, globalNotifierObj,
	    java.hasEvents) == JNI_TRUE) {
	ePtr = (Tcl_Event *) ckalloc(sizeof(Tcl_Event));
	ePtr->proc = JavaEventProc;
	Tcl_QueueEvent(ePtr, TCL_QUEUE_TAIL);
	eventQueued = 1;
    }
}

/*
 *----------------------------------------------------------------------
 *
 * JavaEventProc --
 *
 *	This procedure is invoked when a JavaEvent is processed from
 *	the Tcl event queue.  
 *
 * Results:
 *	None.  
 *
 * Side effects:
 *	Invokes arbitrary Java code.
 *
 *----------------------------------------------------------------------
 */

static int
JavaEventProc(
    Tcl_Event *evPtr,		/* The event that is being processed. */
    int flags)			/* The flags passed to Tcl_ServiceEvent. */
{
    JNIEnv *env = JavaGetEnv(NULL);
    
    /*
     * Call Notifier.serviceEvent() to handle invoking the next event and
     * signaling any threads that are waiting on the event.
     */

    eventQueued = 0;

    /*
     * It is safe to leave the monitor here since we assume nothing about the
     * state of the world after we return.
     */
    
    (void) (*env)->CallIntMethod(env, globalNotifierObj, java.serviceEvent,
	    flags);
    return 1;
}

