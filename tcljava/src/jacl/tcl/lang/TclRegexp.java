/*
 * TclRegexp.java
 *
 * Copyright (c) 1999 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * SCCS: %Z% %M% %I% %E% %U%
 */

package tcl.lang;

import sunlabs.brazil.util.regexp.Regexp;

public class TclRegexp 
{
    private
    TclRegexp()
    {
    }

    public static Regexp
    compile(Interp interp, TclObject exp, boolean nocase)
	throws TclException
    {
	try {
	    return new Regexp(exp.toString(), nocase);
	} catch (IllegalArgumentException e) {
	    String msg = e.getMessage();
	    if (msg.equals("missing )")) {
		msg = "unmatched ()";
	    } else if (msg.equals("missing ]")) {
		msg = "unmatched []";
	    }
	    msg = "couldn't compile regular expression pattern: " + msg;
	    throw new TclException(interp, msg);
	}
    }
}
    
