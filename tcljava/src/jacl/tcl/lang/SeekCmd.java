/*
 * SeekCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: SeekCmd.java,v 1.1 1998/10/14 21:09:18 cvsadmin Exp $
 *
 */

package tcl.lang;
import java.util.*;
import java.io.*;

/**
 * This class implements the built-in "seek" command in Tcl.
 */

class SeekCmd implements Command {
    /**
     * This procedure is invoked to process the "seek" Tcl command.
     * See the user documentation for details on what it does.
     */

    public void cmdProc(Interp interp, TclObject argv[])
            throws TclException {

	Channel chan;        /* The channel being operated on this method */
	int mode;            /* Stores the search mode, either beg, cur or end
			      * of file.  See the TclIO class for more info */ 

	if (argv.length != 3 && argv.length != 4) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "channelId offset ?origin?");
	}

	/*
	 * default is the beginning of the file
	 */

	mode = TclIO.SEEK_SET;

	if (argv.length == 4) {
	    if ("start".equals(argv[3].toString())) {
	        mode = TclIO.SEEK_SET;
	    } else if ("current".equals(argv[3].toString())) {
	        mode = TclIO.SEEK_CUR;
	    } else if ("end".equals(argv[3].toString())) {
	        mode = TclIO.SEEK_END;
	    } else {
	        throw new TclException(interp, "bad origin \"" + 
		        argv[3].toString() +
                        "\": should be start, current, or end");
	    }
	}
	
	chan = TclIO.getChannel(interp, argv[1].toString());
	if (chan == null) {
	    throw new TclException(interp, "can not find channel named \""
                    + argv[1].toString() + "\"");
	}
	long offset = TclInteger.get(interp, argv[2]);

	try {
	    chan.seek(offset, mode);
	} catch ( IOException e ) {
	    throw new TclException(interp, "error during seek on \"" +
                    chan.getChanName() + "\": invalid argument");
	}
    }
}
