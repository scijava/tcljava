/*
 * TclIO.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclIO.java,v 1.1 1998/10/14 21:09:20 cvsadmin Exp $
 *
 */

package tcl.lang;
import java.util.*;
import java.io.*;

class TclIO {

    static final int READ_ALL     = 1;
    static final int READ_LINE    = 2;
    static final int READ_N_BYTES = 3;

    static final int SEEK_SET     = 1;
    static final int SEEK_CUR     = 2;  
    static final int SEEK_END     = 3;

    static final int RDONLY = 1;
    static final int WRONLY = 2;
    static final int RDWR   = 4; 
    static final int APPEND = 8;
    static final int CREAT  = 16;

    /**
     * Table of channels currently registered for all interps.  The 
     * interpChanTable has "virtual" references into this table that
     * stores the registered channels for the individual interp.
     */

    private static StdChannel stdinChan = null;
    private static StdChannel stdoutChan = null;
    private static StdChannel stderrChan = null;

    static Channel getChannel(Interp interp, String chanName) {
        Hashtable chanTable;
	Channel   chan = null;

	if ((chanName.length() > 0) && (chanName.charAt(0) == 's')) {
	    if (chanName.equals("stdin")) {
	        chan = getStdChannel(StdChannel.STDIN);
	    } else if (chanName.equals("stdout")) {
	        chan = getStdChannel(StdChannel.STDOUT);
	    } else if (chanName.equals("stderr")) {
	        chan = getStdChannel(StdChannel.STDERR);
	    }
	    if (chan != null) {
	        chanName = chan.getChanName();
	    }
	}

	chanTable = getInterpChanTable(interp);
        return((Channel)chanTable.get(chanName));
    }


    static void registerChannel(Interp interp, Channel chan) {

        if (interp != null) {
            Hashtable chanTable = getInterpChanTable(interp);
	    chanTable.put(chan.getChanName(), chan);
	}
    }


    static void unregisterChannel(Interp interp, Channel chan) {
        
        Hashtable chanTable = getInterpChanTable(interp);
	chanTable.remove(chan.getChanName());
    }


    static Hashtable getInterpChanTable(Interp interp) { 
         Channel chan;

        if (interp.interpChanTable == null) {
	    
	    interp.interpChanTable = new Hashtable();

	    chan = getStdChannel(StdChannel.STDIN);
	    registerChannel(interp, chan);

	    chan = getStdChannel(StdChannel.STDOUT);
	    registerChannel(interp, chan);

	    chan = getStdChannel(StdChannel.STDERR);
	    registerChannel(interp, chan);
	}
	
	return interp.interpChanTable;
    }


    static Channel getStdChannel(int type) {
        Channel chan = null;
      
        switch (type) {
            case StdChannel.STDIN:
	        if (stdinChan == null) {
		    stdinChan = new StdChannel(StdChannel.STDIN);
		}
		chan = stdinChan;
	        break;
            case StdChannel.STDOUT:
	        if (stdoutChan == null) {
		    stdoutChan = new StdChannel(StdChannel.STDOUT);
		}
		chan = stdoutChan;
	        break;
            case StdChannel.STDERR:
	        if (stderrChan == null) {
		    stderrChan = new StdChannel(StdChannel.STDERR);
		}
		chan = stderrChan;
	        break;
	    default:
	        throw new TclRuntimeError("Invalid type for StdChannel");
	}

	return(chan);
    }
}

