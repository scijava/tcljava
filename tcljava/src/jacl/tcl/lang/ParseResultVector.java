/*
 * ParseResultVector.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ParseResultVector.java,v 1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang;

import java.util.*;

/**
 * This class stores a Vector of words that are the result of a 
 * Interp.ParseWords() call.
 */
final class ParseResultVector {
    Vector words;

    /**
     * The end of the current command (excluding the trailing word separator,
     * if any. E.g. Command = "set a 10;" cmdEnd = 7.
     *
     * The value is -1 if the command is an empty string.
     */
    int cmdEnd;

    /**
     * Points to the beginning of the next command.
     */
    int nextIndex;

    /**
     * True if we have reached a termChar that is not "\n" or ";"
     */
    boolean gotTerm;

    ParseResultVector() {
	cmdEnd = 0;
	nextIndex = 0;
	gotTerm = false;
	disposed = false;
    }

    /**
     * Adds one word to the vector of words. TclObject.Attach() is called
     * so that the refCount of TclObject's are maintained properly. This is
     * needed in parsing commands such as the following:
     * <pre>
     *	    set x ""
     *	    list [append x first] [append x second] [append x third] $x
     * </pre>
     *
     * @param o the object to add into the vector.
     */
    final void addElement(TclObject tobj) {
	words.addElement(tobj);
    }

    /**
     * This is set to true if the objects in the vector has been explictly
     * disposed by a dispose() call.
     */
    boolean disposed;

    final void dispose() {
	if (!disposed) {
	    disposed = true;
	    for (int i=words.size()-1; i>=0; i--) {
		((TclObject)words.elementAt(i)).release();
	    }
	}
    }

}

