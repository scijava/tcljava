/*
 * StdChannel.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: StdChannel.java,v 1.18 2003/03/05 23:19:26 mdejong Exp $
 *
 */

package tcl.lang;
import java.util.*;
import java.io.*;

/**
 * Subclass of the abstract class Channel.  It implements all of the 
 * methods to perform read, write, open, close, etc on system stdio channels.
 */

class StdChannel extends Channel {
  
    /**
     * stdType store which type, of the three below, this StdChannel is.
     */

    private int stdType = -1;

    /**
     * Flags indicating the type of this StdChannel.
     */

    static final int STDIN    = 0;
    static final int STDOUT   = 1; 
    static final int STDERR   = 2; 

    /**
     * Constructor that does nothing.  Open() must be called before
     * any of the subsequent read, write, etc calls can be made.
     */

    StdChannel() {}

    /**
     * Constructor that will automatically call open.
     *
     * @param stdName name of the stdio channel; stdin, stderr or stdout.
     */

    StdChannel(String stdName) {
	if (stdName.equals("stdin")) {
	    open(STDIN);
	} else if (stdName.equals("stdout")) {
	    open(STDOUT);
	} else if (stdName.equals("stderr")) {
	    open(STDERR);
	} else {
	    throw new TclRuntimeError(
                    "Error: unexpected type for StdChannel");
	}
    }


    StdChannel(int type) {
        open(type);
    }


    /**
     * Set the channel type to one of the three stdio types.  Throw a 
     * tclRuntimeEerror if the stdName is not one of the three types.  If
     * it is a stdin channel, initialize the "in" data member.  Since "in"
     * is static it may have already be initialized, test for this case 
     * first.  Set the names to fileX, this will be the key in the chanTable 
     * hashtable to access this object.  Note: it is not put into the hash 
     * table in this function.  The calling function is responsible for that.
     *
     * @param stdName String that equals stdin, stdout, stderr
     * @return The name of the channelId
     */

    String open(int type) {

        switch (type) {
	    case STDIN:
	        mode = TclIO.RDONLY;
	        setBuffering(TclIO.BUFF_LINE);
	        setChanName("stdin");
		if (reader == null) {
		    reader = new BufferedReader(
                        new InputStreamReader(System.in)); 
		}
		break;
	    case STDOUT:
	        mode = TclIO.WRONLY;
	        setBuffering(TclIO.BUFF_LINE);
	        setChanName("stdout");
		break;
	    case STDERR:
	        mode = TclIO.WRONLY;
	        setBuffering(TclIO.BUFF_NONE);
	        setChanName("stderr");
		break;
	    default:
		throw new RuntimeException(
		    "type does not match one of STDIN, STDOUT, or STDERR");
	}

        stdType = type;
	
	return getChanName();
    }

    /**
     * Write to stdout or stderr.  If the stdType is not set to 
     * STDOUT or STDERR this is an error; either the stdType wasnt
     * correctly initialized, or this was called on a STDIN channel.
     *
     * @param interp the current interpreter.
     * @param s the string to write 
     */

    void write(Interp interp, TclObject outData) 
            throws IOException, TclException {

        checkWrite(interp);

        if (stdType == STDERR) {
            System.err.print(outData.toString());
        } else {
            String s = outData.toString();
            System.out.print(s);
            if (buffering == TclIO.BUFF_NONE ||
                    (buffering == TclIO.BUFF_LINE && s.endsWith("\n"))) {
                System.out.flush();
            }
        }
    }

    /**
     * Check for any output that might still need to be flushed
     * when the channel is closed.
     */

    void close() throws IOException {
        super.close();

        if (stdType == STDOUT)
            System.out.flush();
    }

    String getChanType() {
        return "tty";
    }
}
