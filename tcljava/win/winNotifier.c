/* 
 * winNotifier.c --
 *
 *	This file contains the Windows specific portion of the
 *	Notifier class.  Most of this file will be redundant in
 *	the multithreaded version of Tcl.
 *
 * Copyright (c) 1998 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: winNotifier.c,v 1.2 1999/08/31 00:46:40 redman Exp $
 */

#include "java.h"

#define WM_WAKEUP WM_USER	/* Message that is send by
				 * TclpAlertNotifier. */

/*
 * Declaration for private  Tcl function.
 */

EXTERN HINSTANCE TclWinGetTclInstance();

/*
 * The following static structure contains the state information for the
 * Windows implementation of the Tcl notifier.
 */

static struct {
    CRITICAL_SECTION crit;	/* Monitor for this notifier. */
    int pending;		/* Alert message pending. */
    HWND hwnd;
} notifier;

/*
 * Static routines defined in this file.
 */

static void		NotifierExitHandler(ClientData clientData);
static LRESULT CALLBACK	NotifierProc(HWND hwnd, UINT message,
			    WPARAM wParam, LPARAM lParam);

/*
 *----------------------------------------------------------------------
 *
 * JavaInitNotifier --
 *
 *	Initialize the platform specific notifier state.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Creates a messaging window.
 *
 *----------------------------------------------------------------------
 */

void
JavaInitNotifier()
{
    WNDCLASS class;

    InitializeCriticalSection(&notifier.crit);
    notifier.pending = 0;

    class.style = 0;
    class.cbClsExtra = 0;
    class.cbWndExtra = 0;
    class.hInstance = TclWinGetTclInstance();
    class.hbrBackground = NULL;
    class.lpszMenuName = NULL;
    class.lpszClassName = "JavaNotifier";
    class.lpfnWndProc = NotifierProc;
    class.hIcon = NULL;
    class.hCursor = NULL;

    if (!RegisterClassA(&class)) {
	panic("Unable to register JavaNotifier window class");
    }
    notifier.hwnd = CreateWindow("JavaNotifier", "JavaNotifier", WS_TILED,
	    0, 0, 0, 0, NULL, NULL, TclWinGetTclInstance(), NULL);
    Tcl_CreateExitHandler(NotifierExitHandler, NULL);
}

/*
 *----------------------------------------------------------------------
 *
 * JavaDisposeNotifier --
 *
 *	Clean up Windows specific notifier state.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Unregisters the notifier window and class.
 *
 *----------------------------------------------------------------------
 */
TCLBLEND_EXTERN void
JavaDisposeNotifier()
{
    Tcl_DeleteExitHandler(NotifierExitHandler, NULL);
    NotifierExitHandler(NULL);
}

/*
 *----------------------------------------------------------------------
 *
 * NotifierExitHandler --
 *
 *	This function is called to cleanup the notifier state before
 *	Tcl is unloaded.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Destroys the notifier window.
 *
 *----------------------------------------------------------------------
 */

static void
NotifierExitHandler(
    ClientData clientData)	/* Old window proc */
{
    if (notifier.hwnd) {
	DestroyWindow(notifier.hwnd);
	UnregisterClass("JavaNotifier", TclWinGetTclInstance());
	notifier.hwnd = NULL;
    }
}

/*
 *----------------------------------------------------------------------
 *
 * NotifierProc --
 *
 *	This procedure is invoked by Windows to process events on
 *	the notifier window.  Messages will be sent to this window
 *	in response to external timer events or calls to
 *	TclpAlertNotifier.
 *
 * Results:
 *	A standard windows result.
 *
 * Side effects:
 *	Services any pending events.
 *
 *----------------------------------------------------------------------
 */

static LRESULT CALLBACK
NotifierProc(
    HWND hwnd,
    UINT message,
    WPARAM wParam,
    LPARAM lParam)
{
    if (message == WM_USER) {
	EnterCriticalSection(&notifier.crit);
	notifier.pending = 0;
	LeaveCriticalSection(&notifier.crit);
    } else {
	return DefWindowProc(hwnd, message, wParam, lParam);
    }
	
    /*
     * Process all of the runnable events.
     */

    Tcl_ServiceAll();
    return 0;
}

/*
 *----------------------------------------------------------------------
 *
 * JavaAlertNotifier --
 *
 *	Wake up the Tcl notifier.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Sends an event to the Tcl event window.
 *
 *----------------------------------------------------------------------
 */

void
JavaAlertNotifier()
{
    EnterCriticalSection(&notifier.crit);
    if (!notifier.pending) {
	PostMessage(notifier.hwnd, WM_WAKEUP, 0, 0);
	notifier.pending = 1;
    }
    LeaveCriticalSection(&notifier.crit);
}


