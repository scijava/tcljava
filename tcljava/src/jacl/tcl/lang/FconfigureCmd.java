/*
 * FconfigureCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: FconfigureCmd.java,v 1.2 1999/05/09 00:10:53 dejong Exp $
 *
 */

package tcl.lang;
import java.util.*;

/**
 * This class implements the built-in "fconfigure" command in Tcl.
 */

class FconfigureCmd implements Command {

    static final private String validCmds[] = { 
        "-blocking",
	"-buffering",
	"-buffersize",
	"-eofchar",
	"-translation",
    };

    static final int OPT_BLOCKING	= 0;
    static final int OPT_BUFFERING	= 1;
    static final int OPT_BUFFERSIZE	= 2;
    static final int OPT_EOFCHAR	= 3;
    static final int OPT_TRANSLATION	= 4;


    /**
     * This procedure is invoked to process the "fconfigure" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     */

    public void cmdProc(Interp interp, TclObject argv[])
            throws TclException {
	      
	Channel chan;        // The channel being operated on this method

        if ((argv.length < 2) || (((argv.length % 2) == 1) && 
                (argv.length != 3))) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "channelId ?optionName? ?value? ?optionName value?...");
	}
	
        chan = TclIO.getChannel(interp, argv[1].toString());
	if (chan == null) {
	    throw new TclException(interp, "can not find channel named \""
                    + argv[1].toString() + "\"");
	}

	if (argv.length == 2) {
	    // return list of all name/value pairs for this channelId

	}
	if (argv.length == 3) {
	    // return value for supplied name

	    int index = TclIndex.get(interp, argv[2], validCmds, 
                    "option", 0);


	    switch (index) {
	        case OPT_BLOCKING: {    // -blocking
		    break;
		}
	        case OPT_BUFFERING: {    // -buffering
		    break;
		}
	        case OPT_BUFFERSIZE: {    // -buffersize
		    break;
		}
	        case OPT_EOFCHAR: {    // -eofchar
		    break;
		}
	        case OPT_TRANSLATION: {    // -translation
		    break;
		}
	        default: {
		    throw new TclRuntimeError("Fconfigure.cmdProc() error: " +
                            "incorrect index returned from TclIndex.get()");
		}
	    }
	}
	for (int i = 3; i < argv.length; i += 2) {
	    // Iterate through the list setting the name with the 
	    // corresponding value.

	    int index = TclIndex.get(interp, argv[i-1], validCmds, 
                    "option", 0);

	    switch (index) {
	        case OPT_BLOCKING: {    // -blocking
		  break;
		}
	        case OPT_BUFFERING: {    // -buffering
		    String arg = argv[i].toString();
		    if (arg.equals("full")) {

		    } else if (arg.equals("line")) {

		    } else if (arg.equals("none")) {

		    } else {
		        throw new TclException(interp, 
                                "bad value for -buffering: must be " +
				"one of full, line, or none");
		    }
		    break;
		}
	        case OPT_BUFFERSIZE: {    // -buffersize
		    break;
		}
	        case OPT_EOFCHAR: {    // -eofchar
		    break;
		}
	        case OPT_TRANSLATION: {    // -translation
		    String arg = argv[i].toString();
		    if (arg.equals("auto")) {

		    } else if (arg.equals("binary")) {

		    } else if (arg.equals("cr")) {

		    } else if (arg.equals("crlf")) {

		    } else if (arg.equals("lf")) {

		    } else {
		        throw new TclException(interp,
                                "bad value for -translation: must be one " +
				"of auto, binary, cr, lf, crlf, or platform");
		    }
		    break;
		}
	        default: {
  		    throw new TclRuntimeError("Fconfigure.cmdProc() error: " +
                            "incorrect index returned from TclIndex.get()");
		}
	    }
	}

	throw new TclException(interp, "fconfigure command not implemented yet");
    }
}
