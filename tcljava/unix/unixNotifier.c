/* 
 * unixNotifier.c --
 *
 *	This file contains the Unix specific portion of the
 *	Notifier class.  Most of this file will be redundant in
 *	the multithreaded version of Tcl.
 *
 * Copyright (c) 1998 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: unixNotifier.c,v 1.1 1998/10/14 21:09:22 cvsadmin Exp $
 */

#include "java.h"
#include <unistd.h>

/*
 * The following static structure contains the state information for the
 * Windows implementation of the Tcl notifier.
 */

static struct {
    Tcl_Channel readChan;	/* Read side used in main thread. */
    int writeFd;		/* Write side used in other threads. */
} notifier;

/*
 * Static routines defined in this file.
 */

static void	NotifierChannelProc(ClientData clientData, int mask);
static void	NotifierExitHandler(ClientData clientData);

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
    int fds[2];

    pipe(fds);
    notifier.readChan = Tcl_MakeFileChannel((ClientData)fds[0], TCL_READABLE);
    notifier.writeFd = fds[1];
    Tcl_SetChannelOption(NULL, notifier.readChan, "-blocking", "0");
    Tcl_CreateChannelHandler(notifier.readChan, TCL_READABLE,
	    NotifierChannelProc, NULL);
    Tcl_CreateExitHandler(NotifierExitHandler, NULL);
}

/*
 *----------------------------------------------------------------------
 *
 * JavaDisposeNotifier --
 *
 *	Clean up Unix specific notifier state.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Unregisters the notifier window and class.
 *
 *----------------------------------------------------------------------
 */

void
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
    Tcl_Close(NULL, notifier.readChan);
    close(notifier.writeFd);
}

/*
 *----------------------------------------------------------------------
 *
 * NotifierChannelProc --
 *
 *	This routine is called whenever the notifier pipe becomes
 *	readable.  It consumes all of the data on the pipe before
 *	returning.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static void
NotifierChannelProc(
    ClientData clientData,	/* Not used. */
    int mask)			/* TCL_READABLE. */
{
    char buf[256];
    while (Tcl_Read(notifier.readChan, buf, 255) > 0) {}
}

/*
 *----------------------------------------------------------------------
 *
 * JavaAlertNotifier --
 *
 *	Wake up the Tcl notifier by writing onto the end of the
 *	notifier pipe.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	May cause the main notifier thread to break out of its select
 *	loop.
 *
 *----------------------------------------------------------------------
 */

void
JavaAlertNotifier()
{
    write(notifier.writeFd, "a", 1);
}

