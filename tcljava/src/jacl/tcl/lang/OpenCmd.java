/*
 * OpenCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: OpenCmd.java,v 1.1 1998/10/14 21:09:18 cvsadmin Exp $
 *
 */

package tcl.lang;
import java.util.*;
import java.io.*;

/**
 * This class implements the built-in "open" command in Tcl.
 */

class OpenCmd implements Command {
    /**
     * This procedure is invoked to process the "open" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     */

    public void cmdProc(Interp interp, TclObject argv[])
            throws TclException {

	boolean pipeline  = false;        /* True if opening pipeline chan */
	int     prot      = 0666;         /* Final rdwr permissions of file */
	int     modeFlags = TclIO.RDONLY; /* Rdwr mode for the file.  See the
					   * TclIO class for more info on the
					   * valid modes */

        if ((argv.length < 2) || (argv.length > 4)) {
	    throw new TclNumArgsException(interp, 1, argv, 
	        "fileName ?access? ?permissions?");
	}

	if (argv.length > 2) {
	    String modeStr = argv[2].toString();
	    int len = modeStr.length();

	    if ((len == 0) || (len > 2)) {
	        throw new TclException(interp, 
                         "illegal access mode \"" + modeStr + "\"");
	    }

	    switch (modeStr.charAt(0)) {
	        case 'r': {
		    if(len == 1) {
		        modeFlags = TclIO.RDONLY;
			break;
		    } else if (modeStr.charAt(1) == '+') {
		        modeFlags = TclIO.RDWR;
			break;
		    }
		}
	        case 'w': {
		    File f = FileUtil.getNewFileObj(interp, 
			    argv[1].toString());
		    if (f.exists()) {
			f.delete();
		    }
		    if(len == 1) {
		        modeFlags = (TclIO.WRONLY|TclIO.CREAT);
			break;
		    } else if (modeStr.charAt(1) == '+') {
		        modeFlags = (TclIO.RDWR|TclIO.CREAT);
			break;
		    }
		}
	        case 'a': {
		    if(len == 1) {
		        modeFlags = (TclIO.WRONLY|TclIO.APPEND);
			break;
		    } else if (modeStr.charAt(1) == '+') {
		        modeFlags = (TclIO.RDWR|TclIO.CREAT|TclIO.APPEND);
			break;
		    }
		}
	        default: {
		    throw new TclException(interp, "illegal access mode \""
                            + modeStr + "\"");
		}
	    }
	}

	if (argv.length == 4) {
	    prot = TclInteger.get(interp, argv[3]);
	    throw new TclException(interp, 
		    "setting permissions not implemented yet");
	}
	if ((argv[1].toString().length() > 0) &&
		(argv[1].toString().charAt(0) == '|')) {
	    pipeline = true;
	    throw new TclException(interp, "pipelines not implemented yet");
	}

	/*
	 * Open the file or create a process pipeline.
	 */

	if (!pipeline) {
	    try {
	        FileChannel file = new FileChannel();
		file.open(interp, argv[1].toString(), modeFlags);
		TclIO.registerChannel(interp, file);
		interp.setResult(TclString.newInstance(file.getChanName()));
	    } catch (IOException e) {
		throw new TclException(interp, "cannot open file: " + 
                        argv[1].toString());
	    }
	} else {
	    /*
	     * Pipeline code here...
	     */

	}
    }
}
