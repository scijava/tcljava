/*
 * OpenCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: OpenCmd.java,v 1.4 2003/03/06 22:53:07 mdejong Exp $
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
	    TclObject mode = argv[2];
	    String modeStr = mode.toString();
	    int len = modeStr.length();

	    if ((len == 0)) {
	        throw new TclException(interp, 
                         "illegal access mode \"" + modeStr + "\"");
	    }

            if (len <  3) { // "r", "w", "r+", "w+" and so on
	        switch (modeStr.charAt(0)) {
	            case 'r': {
		        if (len == 1) {
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
		        if (len == 1) {
		            modeFlags = (TclIO.WRONLY|TclIO.CREAT);
			    break;
		        } else if (modeStr.charAt(1) == '+') {
		            modeFlags = (TclIO.RDWR|TclIO.CREAT);
			    break;
		        }
		    }
	            case 'a': {
		        if (len == 1) {
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
            } else {
	        modeFlags = 0;
	        boolean gotRorWflag = false;
	        final int mlen = TclList.getLength(interp, mode);
	        for (int i=0; i < mlen; i++) {
	            TclObject marg = TclList.index(interp, mode, i);
	            if (marg.toString().equals("RDONLY")) {
	                modeFlags |= TclIO.RDONLY;
	                gotRorWflag = true;
	            } else if (marg.toString().equals("WRONLY")) {
	                modeFlags |= TclIO.WRONLY;
	                gotRorWflag = true;
	            } else if (marg.toString().equals("RDWR")) {
	                modeFlags |= TclIO.RDWR;
	                gotRorWflag = true;
	            } else if (marg.toString().equals("APPEND")) {
	                modeFlags |= TclIO.APPEND;
	            } else if (marg.toString().equals("CREAT")) {
	                modeFlags |= TclIO.CREAT;
	            } else if (marg.toString().equals("EXCL")) {
	                modeFlags |= TclIO.EXCL;
	            } else if (marg.toString().equals("TRUNC")) {
	                modeFlags |= TclIO.TRUNC;
	            } else {
	                throw new TclException(interp,
	                        "illegal access mode \"" + marg.toString() + "\"");
	            }
	        }
	        if (!gotRorWflag) {
	            throw new TclException(interp,
	                    "must specify one of RDONLY, WRONLY, or RDWR");
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
		interp.setResult(file.getChanName());
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
