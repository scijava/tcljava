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
 * RCS: @(#) $Id: javaNotifier.c,v 1.2.2.4 2000/10/27 11:42:58 mdejong Exp $
 */

#include "java.h"
#include "javaNative.h"

typedef struct ThreadSpecificData {

  jobject notifierObj;

  int eventQueued;

} ThreadSpecificData;

static Tcl_ThreadDataKey dataKey;

/* Define this here so that we do not need to include tclInt.h */
#define TCL_TSD_INIT(keyPtr)	(ThreadSpecificData *)Tcl_GetThreadData((keyPtr), sizeof(ThreadSpecificData))

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

jlong JNICALL
Java_tcl_lang_Notifier_init(
    JNIEnv *env,		/* Java environment. */
    jobject notifierObj)	/* Handle to Notifier object. */
{
    ThreadSpecificData *tsdPtr = TCL_TSD_INIT(&dataKey);

    tsdPtr->notifierObj    = (*env)->NewGlobalRef(env, notifierObj);
    tsdPtr->eventQueued    = 0;

    /* If we segfault near here under Windows, try removing tclblend.dll
     * from the current directory.  Tcl Blend has problems loading
     * dlls from a remote directory if there is a dll with the
     * same name in the local directory.
     */
    Tcl_CreateEventSource(NotifierSetup, NotifierCheck, NULL);

    return (jlong) Tcl_GetCurrentThread();
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
    ThreadSpecificData *tsdPtr = TCL_TSD_INIT(&dataKey);

    Tcl_DeleteEventSource(NotifierSetup, NotifierCheck, NULL);
    (*env)->DeleteGlobalRef(env, tsdPtr->notifierObj);
    tsdPtr->notifierObj = NULL;
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
    jobject notifierObj,        /* Handle to Notifier object. */
    jlong tid)	                /* Tcl_ThreadId for the notifier thread */
{
    /* FIXME: there has got to be a better way to do this */
    Tcl_ThreadAlert((Tcl_ThreadId) tid);
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
    JNIEnv *env = JavaGetEnv();
    JavaInfo* jcache = JavaGetCache();
    ThreadSpecificData *tsdPtr = TCL_TSD_INIT(&dataKey);

    if ((*env)->CallBooleanMethod(env, tsdPtr->notifierObj, jcache->hasEvents)
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
    Tcl_Event *ePtr;
    JNIEnv *env = JavaGetEnv();
    JavaInfo* jcache = JavaGetCache();
    ThreadSpecificData *tsdPtr = TCL_TSD_INIT(&dataKey);

    /*
     * Only queue a new event if there isn't already one queued and
     * there are events on the Java event queue.
     */

    if (!tsdPtr->eventQueued && 
        (*env)->CallBooleanMethod(env, tsdPtr->notifierObj,
                                  jcache->hasEvents) == JNI_TRUE) {
	ePtr = (Tcl_Event *) ckalloc(sizeof(Tcl_Event));
	ePtr->proc = JavaEventProc;
	Tcl_QueueEvent(ePtr, TCL_QUEUE_TAIL);
	tsdPtr->eventQueued = 1;
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
    JNIEnv *env = JavaGetEnv();
    JavaInfo* jcache = JavaGetCache();
    ThreadSpecificData *tsdPtr = TCL_TSD_INIT(&dataKey);

    /*
     * Call Notifier.serviceEvent() to handle invoking the next event and
     * signaling any threads that are waiting on the event.
     */

    tsdPtr->eventQueued = 0;
    
    (void) (*env)->CallIntMethod(env, tsdPtr->notifierObj, 
                                 jcache->serviceEvent, flags);
    return 1;
}

