/*
 * SocketCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: SocketCmd.java,v 1.1 1998/10/14 21:09:20 cvsadmin Exp $
 *
 */

package tcl.lang;
import java.util.*;

/**
 * This class implements the built-in "socket" command in Tcl.
 */

class SocketCmd implements Command {

    static final private String validCmds[] = {
	"-async",
	"-myaddr",
	"-myport",
        "-server",
    };

    /**
     * This procedure is invoked to process the "socket" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     */

    public void cmdProc(Interp interp, TclObject argv[])
            throws TclException {
     
	boolean server    = false;    /* True if this is a server socket */
	boolean async     = false;    /* True if this is asynchronous */
	String  channelId = "";       /* Key into chanTable */
	String  myaddr    = "";       /* DNS or IP address of the server */
	String  script    = "";       /* Script for server to run */
	String  host      = "";       /* The server fot the client */
	int     myport    = 0;        /* The port to connect to */
	int     index;                /* Index to the correct cmd */
	int     i;                    /* Index to the current arg from argv */
	
	for (i = 1; (i < argv.length); i++) {
	    if ((argv[i].toString().length() > 0) &&
		    (argv[i].toString().charAt(0) == '-')) {
	        index = TclIndex.get(interp, argv[i], 
                        validCmds, "option", 0);
	    } else { 
	        break;
	    }

 	    switch (index) {
	        case 0: {        /* async  */
		    if (server) {
		        throw new TclException(interp,
                                "cannot set -async option for server sockets");
		    }
		    async = true;
		    break;
		}
	        case 1: {        /* myaddr */
		    i++;
		    if (i >= argv.length) {
		        throw new TclException(interp,
			        "no argument given for -myaddr option");
		    }
		    myaddr = argv[i].toString();
		    break;
		}
	        case 2: {        /* myport */
		    i++;
		    if (i >= argv.length) {
		        throw new TclException(interp,
			        "no argument given for -myport option");
		    }
		    //myport = get a valid port from argv[i]
		    break;
		}
	        case 3: {        /* server */
		    if (async) {
		        throw new TclException(interp,
                                "cannot set -async option for server sockets");
		    }
		    server = true;
		    i++;
		    if (i >= argv.length) {
		        throw new TclException(interp,
		                "no argument given for -server option");
		    }
		    script = argv[i].toString();
		    break;
		}
	        default : {	    
		    throw new TclException(interp,
	                    "bad option \"" + argv[i] +
                            "\", must be -async, -myaddr, -myport," +
                            " or -server");
		}
	    }
	}

	if (server) {
	    host = myaddr;
	    if (myport != 0) {
	        throw new TclException(interp,
		        "Option -myport is not valid for servers");
	    }
	} else if (i < argv.length) {
	    host = argv[i].toString();
	    i++;
	} else {
	    errorWrongNumArgs(interp, argv[0].toString());
	}

	if (i == argv.length-1) {
	    //myport = get a valid port
	} else {
	    errorWrongNumArgs(interp, argv[0].toString());
	}

	if (server) {
 	    /*
	     * Register with the interpreter to let us know when the
	     * interpreter is deleted (by having the callback set the
	     * acceptCallbackPtr->interp field to NULL). This is to
	     * avoid trying to eval the script in a deleted interpreter.
	     */
	  
	    /*
	     * Register a close callback. This callback will inform the
	     * interpreter (if it still exists) that this channel does not
	     * need to be informed when the interpreter is deleted.
	     */

	} else {
      
	}
	throw new TclException(interp, "socket command not implemented yet");
    }

    /**
     * A unique error msg is printed for socket, therefore dont call this 
     * instead of the standard TclException.wrongNumArgs().
     *
     * @param interp the current interpreter.
     * @param cmd the name of the command (extracted form argv[0] of cmdProc)
     */

    private static void errorWrongNumArgs(Interp interp, String cmdName) 
            throws TclException {
        throw new TclException(interp,
                "wrong # args: should be either:\n" +
		cmdName +
                " ?-myaddr addr? ?-myport myport? ?-async? host port\n" +
		cmdName +
                " -server command ?-myaddr addr? port");
    }
}
