/* 
 * Util.java --
 *
 *	This file implements the native version of the tcl.lang.Util class.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: Util.java,v 1.1 1998/10/14 21:09:10 cvsadmin Exp $
 */

package tcl.lang;

/**
 * This class handles parsing of various data types.
 */
public class Util {

/**
 * Given a string, return a boolean value corresponding
 * to the string.
 *
 * @param interp current interpreter.
 * @param string string representation of the boolean.
 * @exception TclException for malformed boolean values.
 */
static final native boolean getBoolean(Interp interp, String s)
throws TclException;

/**
 * Converts an ASCII string to an integer.
 *
 * @param interp current interpreter.
 * @param s the string to convert from. Must be in valid Tcl integer
 *      format.
 * @return the integer value of the string.
 * @exception TclException if the string is not a valid Tcl integer.
 */    

static final native int getInt(Interp interp, String s)
throws TclException;

/**
 * Converts an ASCII string to a double.
 *
 * @param interp current interpreter.
 * @param s the string to convert from. Must be in valid Tcl double
 *      format.
 * @return the double value of the string.
 * @exception TclException if the string is not a valid Tcl double.
 */    

static final native double getDouble(Interp interp, String s)
throws TclException;


/*
 *----------------------------------------------------------------------
 *
 * printDouble --
 *
 *	Returns the string form of a double. The exact formatting
 *	of the string depends on the tcl_precision variable.  Calls
 *	Tcl_PrintDouble() in C.
 *
 * Results:
 *	Returns the string form of double number.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static final native String
printDouble(
    double number);		// The number to format into a string.

/*
 *----------------------------------------------------------------------
 *
 * getCwd --
 *
 *	Retrieve the current working directory.
 *
 * Results:
 *	Returns the string value of the working directory.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static final native String
getCwd();

/*
 *----------------------------------------------------------------------
 *
 * stringMatch --
 *
 *	Compare a string to a globbing pattern.  The matching operation
 *	permits the following special characters in the pattern: *?\[]
 *	(see the manual entry for details).
 *
 * Results:
 *	Returns true if the string matches the pattern.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static public final native boolean
stringMatch(
    String string,		// String to match.
    String pattern);		// Pattern to compare against.

} // end Util

