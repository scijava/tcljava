/* 
 * Util.java --
 *
 *	This class handles Tcl expressions.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997-1998 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: Util.java,v 1.2 1999/05/09 01:38:44 dejong Exp $
 */

package tcl.lang;

import java.io.*;
import java.util.*;

public class Util {

static final int TCL_DONT_USE_BRACES = 1;
static final int USE_BRACES		 = 2;
static final int BRACES_UNMATCHED	 = 4;

// Some error messages.

static final String intTooBigCode =
"ARITH IOVERFLOW {integer value too large to represent}";
static final String fpTooBigCode =
"ARITH OVERFLOW {floating-point value too large to represent}";

// This table below is used to convert from ASCII digits to a
// numerical equivalent.  It maps from '0' through 'z' to integers
// (100 for non-digit characters).

static char cvtIn[] = {
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,		// '0' - '9'
    100, 100, 100, 100, 100, 100, 100,		// punctuation 
    10, 11, 12, 13, 14, 15, 16, 17, 18, 19,	// 'A' - 'Z' 
    20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
    30, 31, 32, 33, 34, 35,
    100, 100, 100, 100, 100, 100,		// punctuation 
    10, 11, 12, 13, 14, 15, 16, 17, 18, 19,	// 'a' - 'z'
    20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
    30, 31, 32, 33, 34, 35
};

// Largest possible base 10 exponent.  Any
// exponent larger than this will already
// produce underflow or overflow, so there's
// no need to worry about additional digits.

static final int maxExponent = 511;

// Table giving binary powers of 10. Entry
// is 10^2^i.  Used to convert decimal
// exponents into floating-point numbers.

static final double powersOf10[] = {
    10.,
    100.,
    1.0e4,
    1.0e8,
    1.0e16,
    1.0e32,
    1.0e64,
    1.0e128,
    1.0e256
};

// Used in the regex Methods below.

static RegexMatcher regexMatcher = checkRegexPackage();

// Determines if the VM has a broken implementation of this method.

static boolean broken_isLetterOrDigit = checkIsLetterOrDigit();

// Default precision for converting floating-point values to strings.

static final int DEFAULT_PRECISION = 12;

// The following variable determine the precision used when converting
// floating-point values to strings. This information is linked to all
// of the tcl_precision variables in all interpreters inside a JVM via 
// PrecTraceProc.
//
// Note: since multiple threads may change precision concurrently, race
// conditions may occur.
//
// It should be modified only by the PrecTraceProc class.

static int precision = DEFAULT_PRECISION;

/*
 *----------------------------------------------------------------------
 *
 * Util --
 *	Dummy constructor to keep Java from automatically creating a
 *	default public constructor for the Util class.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private
Util()
{
    // Do nothing.  This should never be called.
}

/*
 *----------------------------------------------------------------------
 *
 * strtoul --
 *
 *	Implements the same functionality as the strtoul() function
 * 	in the standard C library.
 *
 * 	Converts the leading digits of a string into a 32-bit (signed)
 * 	integer and report the index of the character immediately
 * 	following the digits.
 *
 *		E.g.:	"0x7fffffff"	->  2147483647
 *			"0x80000000"	-> -2147483648
 *			"0x100000000"	-> errno = TCL.INTEGER_RANGE
 *
 * 	Note: although the name of this function is strtoul, it is
 * 	meant to have the same behavior as the strtoul() function in
 * 	NativeTcl, which returns a 32-bit word, which is used as a
 * 	signed integer by tclExpr.c.
 *
 * Results:
 *	if the leading non-blank charactes(s) in the string are
 *      digits, returns the integer represented by these digits and the
 *      index of the character immediately following the digits. Otherwise
 *      returns null.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static StrtoulResult 
strtoul(
    String s, 		// String of ASCII digits, possibly preceded by
  			// white space.  For bases greater than 10, either
    			// lower- or upper-case digits may be used.
    int start, 		// The index of s where the number starts.
    int base) 		// Base for conversion.  Must be less than 37.  If 0,
			// then the base is chosen from the leading characters
			// of string:  "0x" means hex, "0" means octal, 
			// anything else means decimal.
{
    long result = 0;
    int digit;
    boolean anyDigits = false;
    int len = s.length();
    int i = start;
    char c;
    
    // Skip any leading blanks.
    
    while (i < len && Character.isWhitespace(s.charAt(i))) {
	i ++;
    }
    if (i >= len) {
	return new StrtoulResult(0, 0, TCL.INVALID_INTEGER);
    }
    
    // If no base was provided, pick one from the leading characters
    // of the string.
    
    if (base == 0) {
	c = s.charAt(i);
	if (c == '0') {
	    if (i < len-1) {
		i++;
		c = s.charAt(i);
		if (c == 'x') {
		    i += 1;
		    base = 16;
		}
	    }
	    if (base == 0) {
		// Must set anyDigits here, otherwise "0" produces a
		// "no digits" error.

		anyDigits = true;
		base = 8;
	    }
	} else {
	    base = 10;
	}
    } else if (base == 16) {
	if (i < len-2) {
	    // Skip a leading "0x" from hex numbers.

	    if ((s.charAt(i) == '0') && (s.charAt(i+1) == 'x')) {
		i += 2;
	    }
	}
    }

    long max = (((long) ((long) 1 << 32)) / ((long) base));
    boolean overflowed = false;

    for ( ; ; i += 1) {
	if (i >= len) {
	    break;
	}
	digit = s.charAt(i) - '0';
	if (digit < 0 || digit > ('z' - '0')) {
	    break;
	}
	digit = cvtIn[digit];
	if (digit >= base) {
	    break;
	}

	if (result > max) {
	    overflowed = true;
	}

	result = result*base + digit;
	anyDigits = true;
    }
	    
    // See if there were any digits at all.
	
    if (!anyDigits) {
	return new StrtoulResult(0, 0, TCL.INVALID_INTEGER);
    } else if (overflowed) {
	return new StrtoulResult(0, i, TCL.INTEGER_RANGE);
    } else {
	return new StrtoulResult(result, i, 0);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * getInt --
 *
 *	Converts an ASCII string to an integer.
 *
 * Results:
 *	The integer value of the string.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static int 
getInt(
    Interp interp, 	// The current interpreter.
    String s) 		// The string to convert from. Must be in valid
			// Tcl integer format.
throws
    TclException	// If the string is not a valid Tcl integer.
{
    int len = s.length();
    boolean sign;
    int i = 0;

    // Skip any leading blanks.

    while (i < len && Character.isWhitespace(s.charAt(i))) {
	i ++;
    }
    if (i >= len) {
	throw new TclException(interp, "expected integer but got \"" +
		s + "\"");
    }

    char c = s.charAt(i);
    if (c == '-') {
	sign = true;
	i +=1;
    } else {
	if (c == '+') {
	    i +=1;
	}
	sign = false;
    }

    StrtoulResult res = strtoul(s, i, 0);
    if (res.errno < 0) {
	if (res.errno == TCL.INTEGER_RANGE) {
	    if (interp != null) {
		interp.setErrorCode(TclString.newInstance(intTooBigCode));
	    }
	    throw new TclException(interp,
		    "integer value too large to represent");
	} else {
	    throw new TclException(interp, "expected integer but got \"" +
		    s + "\"");
	}
    } else if (res.index < len) {
	for (i = res.index; i<len; i++) {
	    if (!Character.isWhitespace(s.charAt(i))) {
		throw new TclException(interp,
			"expected integer but got \"" + s + "\"");
	    }
	}
    }

    if (sign) {
	return (int)(- res.value);
    } else {
	return (int)(  res.value);
    }
}


/*
 *----------------------------------------------------------------------
 *
 * strtod --
 *
 *	Converts the leading decimal digits of a string into double
 * 	and report the index of the character immediately following the
 * 	digits.
 *
 * Results:
 *	Converts the leading decimal digits of a string into double
 * 	and report the index of the character immediately following the
 * 	digits.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static StrtodResult 
strtod(
    String s, 	// String of ASCII digits, possibly preceded by
	    	// white space.  For bases greater than 10, either lower- or
   		// upper-case digits may be used.
    int start)	// The index to the char where the number starts.
{
    boolean sign = false;
    char c;
    int mantSize;		// Number of digits in mantissa.
    int decPt;			// Number of mantissa digits BEFORE decimal
				// point. 
    int len = s.length();
    int i = start;

    // Skip any leading blanks.

    while (i < len && Character.isWhitespace(s.charAt(i))) {
	i ++;
    }
    if (i >= len) {
	return new StrtodResult(0, 0, TCL.INVALID_DOUBLE);
    }

    c = s.charAt(i);
    if (c == '-') {
	sign = true;
	i +=1;
    } else {
	if (c == '+') {
	    i +=1;
	}
	sign = false;
    }

    // Count the number of digits in the mantissa (including the decimal
    // point), and also locate the decimal point.

    decPt = -1;
    for (mantSize = 0; ; mantSize += 1) {
	c = CharAt(s, i, len);
	if (!Character.isDigit(c)) {
	    if ((c != '.') || (decPt >= 0)) {
		break;
	    }
	    decPt = mantSize;
	}
	i++;
    }

    // Skim off the exponent.

    if ((CharAt(s, i, len) == 'E') || (CharAt(s, i, len) == 'e')) {
	i += 1;
	if (CharAt(s, i, len) == '-') {
	    i += 1;
	} else if (CharAt(s, i, len) == '+') {
	    i += 1;
	}

	while (Character.isDigit(CharAt(s, i, len))) {
	    i += 1;
	}
    }

    s = s.substring(start, i);
    double result = 0;

    try {
	result = Double.valueOf(s).doubleValue();
    } catch (NumberFormatException e) {
	return new StrtodResult(0, 0, TCL.INVALID_DOUBLE);
    }

    if ((result == Double.NEGATIVE_INFINITY) ||
	    (result == Double.POSITIVE_INFINITY)) {
	return new StrtodResult(0, i, TCL.DOUBLE_RANGE);
    }

    if (result == Double.NaN) {
	return new StrtodResult(0, 0, TCL.INVALID_DOUBLE);
    }

    return new StrtodResult(result, i, 0);
}

/*
 *----------------------------------------------------------------------
 *
 * CharAt --
 *
 *	|>description<|
 *
 * Results:
 *	|>None.<|
 *
 * Side effects:
 *	|>None.<|
 *
 *----------------------------------------------------------------------
 */

static final char 
CharAt(
    String s, 
    int index, 
    int len)
{
    if (index >= 0 && index < len) {
	return s.charAt(index);
    } else {
	return '\0';
    }
}

/*
 *----------------------------------------------------------------------
 *
 * getDouble --
 *
 *	Converts an ASCII string to a double.
 *
 * Results:
 *	The double value of the string.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static double 
getDouble(
    Interp interp, 	// The current interpreter.
    String s)	 	// The string to convert from. Must be in valid
			// Tcl double format.
throws 
    TclException	// If the string is not a valid Tcl double.
{
    int len = s.length();
    boolean sign;
    int i = 0;

    // Skip any leading blanks.

    while (i < len && Character.isWhitespace(s.charAt(i))) {
	i ++;
    }
    if (i >= len) {
	throw new TclException(interp, 
		"expected floating-point number but got \"" + s + "\"");
    }

    char c = s.charAt(i);
    if (c == '-') {
	sign = true;
	i +=1;
    } else {
	if (c == '+') {
	    i +=1;
	}
	sign = false;
    }

    StrtodResult res = strtod(s, i);
    if (res.errno != 0) {
	if (res.errno == TCL.DOUBLE_RANGE) {
	    if (interp != null) {
		interp.setErrorCode(TclString.newInstance(fpTooBigCode));
	    }
	    throw new TclException(interp,
		    "floating-point value too large to represent");
	} else {
	    throw new TclException(interp,
		    "expected floating-point number but got \"" + s + "\"");
	}
    } else if (res.index < len) {
	for (i = res.index; i<len; i++) {
	    if (!Character.isWhitespace(s.charAt(i))) {
		throw new TclException(interp,
			"expected floating-point number but got \"" +
			s + "\"");
	    }
	}
    }

    if (sign) {
	return (double)(- res.value);
    } else {
	return (double)(  res.value);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * concat --
 *
 *	Concatenates strings in an CmdArgs object into one string.
 *
 * Results:
 *	The concatenated string.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static String 
concat(
    int from, 		// The starting index.
    int to,  		// The endinf index (inclusive).
    TclObject argv[]) 	// The CmdArgs.
{
    StringBuffer sbuf;

    if (from > argv.length) {
	return "";
    }
    if (to <= argv.length) {
	to = argv.length - 1;
    }

    sbuf = new StringBuffer();
    for (int i = from; i <= to; i++) {
	String str = TrimLeft(argv[i].toString());
	str = TrimRight(str);
	if (str.length() == 0) {
	    continue;
	}
	sbuf.append(str);
	if (i < to) {
	    sbuf.append(" ");
	}
    }

    return sbuf.toString();
}

/*
 *----------------------------------------------------------------------
 *
 * stringMatch --
 *
 *	 See if a particular string matches a particular pattern. The
 * 	 matching operation permits the following special characters in
 *	 the pattern: *?\[] (see the manual entry for details on what
 *	 these mean).
 *
 * Results:
 *	True if the string matches with the pattern.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public static final boolean 
stringMatch(
    String str,			//String to compare pattern against.
    String pat)			//Pattern which may contain special characters.
{
    char[] strArr = str.toCharArray();
    char[] patArr = pat.toCharArray();
    int    strLen = str.length();	// Cache the len of str.
    int    patLen = pat.length();	// Cache the len of pat.
    int    pIndex = 0;           	// Current index into patArr.
    int    sIndex = 0;          	// Current index into patArr.
    char   strch;                 	// Stores current char in string.
    char   ch1;                 	// Stores char after '[' in pat.
    char   ch2;                 	// Stores look ahead 2 char in pat.
    boolean incrIndex = false;  	// If true it will incr both p/sIndex.

    while (true) {
	
	if (incrIndex == true) {
	    pIndex++;
	    sIndex++;
	    incrIndex = false;
	}

	// See if we're at the end of both the pattern and the string.
	// If so, we succeeded.  If we're at the end of the pattern
	// but not at the end of the string, we failed.
	
	if (pIndex == patLen) {
	    if (sIndex == strLen) {
		return true;
	    } else {
		return false;
	    }
	}
	if ((sIndex == strLen) && (patArr[pIndex] != '*')) {
	    return false;
	}

	// Check for a "*" as the next pattern character.  It matches
	// any substring.  We handle this by calling ourselves
	// recursively for each postfix of string, until either we
	// match or we reach the end of the string.
	
	if (patArr[pIndex] == '*') {
	    pIndex++;
	    if (pIndex == patLen) {
		return true;
	    }
	    while (true) {
		if (stringMatch(str.substring(sIndex), 
			pat.substring(pIndex))) {
		    return true;
		}
		if (sIndex == strLen) {
		    return false;
		}
		sIndex++;
	    }
	}
	
	// Check for a "?" as the next pattern character.  It matches
	// any single character.
	  
	if (patArr[pIndex] == '?') {
	    incrIndex = true;
	    continue;
	}

	// Check for a "[" as the next pattern character.  It is followed
	// by a list of characters that are acceptable, or by a range
	// (two characters separated by "-").
	
	if (patArr[pIndex] == '[') {
	    pIndex++;
	    
	    while (true) {
		if ((pIndex == patLen) || (patArr[pIndex] == ']')) {
		    return false;
		}
		if (sIndex == strLen) {
		    return false;
		}
		ch1 = patArr[pIndex];
		strch = strArr[sIndex];
		if (((pIndex + 1) != patLen) && (patArr[pIndex + 1] == '-')) {
		    if ((pIndex += 2) == patLen) {
			return false;
		    }
		    ch2 = patArr[pIndex];
		    if (((ch1 <= strch) && (ch2 >= strch)) ||
			    ((ch1 >= strch) && (ch2 <= strch))) {
			break;
		    }
		} else if (ch1 == strch) {
		    break;
		}
		pIndex++;
	    }
	    
	    for (pIndex++; ((pIndex != patLen) && (patArr[pIndex] != ']'));
		 pIndex++) {
	    }
	    if (pIndex == patLen) {
		return false;
	    }
	    incrIndex = true;
	    continue;
	}
	
	// If the next pattern character is '/', just strip off the '/'
	// so we do exact matching on the character that follows.
	
	if (patArr[pIndex] == '\\') {
	    pIndex++;
	    if (pIndex == patLen) {
		return false;
	    }
	}

	// There's no special character.  Just make sure that the next
	// characters of each string match.
	
	if ((sIndex == strLen) || (patArr[pIndex] != strArr[sIndex])) {
	    return false;
	}
	incrIndex = true;
    }
}

/*
 *-----------------------------------------------------------------------------
 *
 * regExpMatch --
 *
 *	See if a string matches a regular expression.
 *
 * Results:
 *	Returns a boolean whose value depends on whether a match was made.
 *
 * Side effects:
 *	None.
 *
 *-----------------------------------------------------------------------------
 */

// Checks if regex package exists.

static final RegexMatcher
checkRegexPackage()
{
    try {
	Class cls = Class.forName("tcl.regex.OroRegexMatcher");
	return (RegexMatcher)cls.newInstance();
    } catch (Exception e) {
	return null;
    }
}

static final boolean
regExpMatch(
    Interp interp,   			// Current interpreter.
    String string,   			// The string to match.
    TclObject pattern)   		// The regular expression.
throws TclException
{
    // If the regex package is available, the following code will call
    // tcl.regex.OroRegexMatche.match(). Otherwise, return false
    // because this function is not implemented internally.

    if (regexMatcher != null) {
	return regexMatcher.match(interp, string, pattern.toString());
    } else {
	return false;
    }
}

/*
 *-----------------------------------------------------------------------------
 *
 * appendElement --
 *
 *	Append a string to the string buffer.  If the string buffer is not
 *	empty, append a space before appending "s".
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The value of "sbuf" is changesd.
 *
 *-----------------------------------------------------------------------------
 */

static final void
appendElement(
    Interp interp,    			// Current interpreter.
    StringBuffer sbuf,   		// The buffer to append to. 
    String s)   			// The string to append.
throws TclException
{
    if (sbuf.length() > 0) {
	sbuf.append(' ');
    }

    int flags = scanElement(interp, s);
    sbuf.append(convertElement(s, flags));
}

/*
 *----------------------------------------------------------------------
 *
 * findElement --
 *
 *	Given a pointer into a Tcl list, locate the first (or next)
 *	element in the list.
 *
 * Results:
 *	The string value of the element and the index of the character
 *	immediately behind the element.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static final FindElemResult
findElement(
    Interp interp,		// Current interpreter. If non-null, is used
				// to store error messages.
    String s,			// The string to locate an element.
    int i,			// The index inside s to start locating an
				// element.
    int len)			// The length of the string.
throws
    TclException
{
    int openBraces = 0;
    boolean inQuotes = false;

    for (; i<len && Character.isWhitespace(s.charAt(i)); i++) {
	;
    }
    if (i >= len) {
	return null;
    }
    char c = s.charAt(i);
    if (c == '{') {
	openBraces = 1;
	i++;
    } else if (c == '"') {
	inQuotes = true;
	i++;
    }
    StringBuffer sbuf = new StringBuffer();

    while (true) {
	if (i >= len) {
	    if (openBraces != 0) {
		throw new TclException(interp,
			"unmatched open brace in list");
	    } else if (inQuotes) {
		throw new TclException(interp, 
			"unmatched open quote in list");
	    }
	    return new FindElemResult(i, sbuf.toString());
	}

	c = s.charAt(i);
	switch(c) {
	    // Open brace: don't treat specially unless the element is
	    // in braces.  In this case, keep a nesting count.

	case '{':
	    if (openBraces != 0) {
		openBraces++;
	    }
	    sbuf.append(c);
	    i++;
	    break;

	    // Close brace: if element is in braces, keep nesting
	    // count and quit when the last close brace is seen.

	case '}':
	    if (openBraces == 1) {
		if (i == len-1 || Character.isWhitespace(s.charAt(i+1))) {
		    return new FindElemResult(i+1, sbuf.toString());
		} else {
		    int errEnd;
		    for (errEnd = i+1; errEnd<len; errEnd++) {
			if (Character.isWhitespace(s.charAt(errEnd))) {
			    break;
			}
		    }
		    throw new TclException(interp,
			    "list element in braces followed by \"" +
			    s.substring(i+1, errEnd) +
			    "\" instead of space");
		}
	    } else if (openBraces != 0) {
		openBraces--;
	    }
	    sbuf.append(c);
	    i++;
	    break;

	    // Backslash:  skip over everything up to the end of the
	    // backslash sequence.

	case '\\':
	    if (openBraces > 0) {
		// Quotes are ignored in brace-quoted stuff

		sbuf.append(c);
		i++;
	    } else {
		BackSlashResult bs = Interp.backslash(s, i, len);
		sbuf.append(bs.c);
		i = bs.nextIndex;
	    }

	    break;

	    // Space: ignore if element is in braces or quotes;  otherwise
	    // terminate element.

	case ' ':
	case '\f':
	case '\n':
	case '\r':
	case '\t':
	    if ((openBraces == 0) && !inQuotes) {
		return new FindElemResult(i+1, sbuf.toString());
	    } else {
		sbuf.append(c);
		i++;
	    }
	    break;

	    // Double-quote:  if element is in quotes then terminate it.

	case '"':
	    if (inQuotes) {
		if (i == len-1 || Character.isWhitespace(s.charAt(i+1))) {
		    return new FindElemResult(i+1, sbuf.toString());
		} else {
		    int errEnd;
		    for (errEnd = i+1; errEnd<len; errEnd++) {
			if (Character.isWhitespace(s.charAt(errEnd))) {
			    break;
			}
		    }
		    throw new TclException(interp,
			    "list element in quotes followed by \"" +
			    s.substring(i+1, errEnd) +
			    "\" instead of space");
		}
	    } else {
		sbuf.append(c);
		i++;
	    }
	    break;

	default:
	    sbuf.append(c);
	    i++;
	}
    }
}

/*
 *----------------------------------------------------------------------
 *
 * scanElement --
 *
 *	This procedure is a companion procedure to convertElement.
 *	It scans a string to see what needs to be done to it (e.g.
 * 	add backslashes or enclosing braces) to make the string into
 *	a valid Tcl list element.
 *
 * Results:
 *	The flags needed by Tcl_ConvertElement when doing the actual
 * 	conversion.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static int 
scanElement(
    Interp interp, 	// The current interpreter.
    String string) 	// The String to scan.
throws 
    TclException
{
    int flags, nestingLevel;
    char c;
    int len;
    int i;

    // This procedure and Tcl_ConvertElement together do two things:
    //
    // 1. They produce a proper list, one that will yield back the
    // argument strings when evaluated or when disassembled with
    // Tcl_SplitList.  This is the most important thing.
    // 
    // 2. They try to produce legible output, which means minimizing the
    // use of backslashes (using braces instead).  However, there are
    // some situations where backslashes must be used (e.g. an element
    // like "{abc": the leading brace will have to be backslashed.  For
    // each element, one of three things must be done:
    //
    // (a) Use the element as-is (it doesn't contain anything special
    // characters).  This is the most desirable option.
    //
    // (b) Enclose the element in braces, but leave the contents alone.
    // This happens if the element contains embedded space, or if it
    // contains characters with special interpretation ($, [, ;, or \),
    // or if it starts with a brace or double-quote, or if there are
    // no characters in the element.
    //
    // (c) Don't enclose the element in braces, but add backslashes to
    // prevent special interpretation of special characters.  This is a
    // last resort used when the argument would normally fall under case
    // (b) but contains unmatched braces.  It also occurs if the last
    // character of the argument is a backslash or if the element contains
    // a backslash followed by newline.
    //
    // The procedure figures out how many bytes will be needed to store
    // the result (actually, it overestimates).  It also collects
    // information about the element in the form of a flags word.

    final boolean debug = false;

    nestingLevel = 0;
    flags = 0;

    i = 0;
    len = string.length();
    if (len == 0) {
	string = String.valueOf('\0');

	// FIXME : pizza compiler workaround
	// We really should be able to use the "\0" form but there
	// is a nasty bug in the pizza compiler shipped with kaffe
	// that causes "\0" to be read as the empty string.

	//string = "\0";
    }

    if (debug) {
	System.out.println("scanElement string is \"" + string + "\"");
    }

    c = string.charAt(i);
    if ((c == '{') || (c == '"') || (c == '\0')) {
	flags |= USE_BRACES;
    }
    for ( ; i < len; i++) {
	if (debug) {
	    System.out.println("getting char at index " + i);
	    System.out.println("char is '" + string.charAt(i) + "'");
	}

	c = string.charAt(i);
	switch (c) {
	case '{':
	    nestingLevel++;
	    break;
	case '}':
	    nestingLevel--;
	    if (nestingLevel < 0) {
		flags |= TCL_DONT_USE_BRACES|BRACES_UNMATCHED;
	    }
	    break;
	case '[':
	case '$':
	case ';':
	case ' ':
	case '\f':
	case '\n':
	case '\r':
	case '\t':
	case 0x0b:

	    // 0x0b is the character '\v' -- this escape sequence is
	    // not available in Java, so we hard-code it. We need to
	    // support \v to provide compatibility with native Tcl.

	    flags |= USE_BRACES;
	    break;
	case '\\':
	    if ((i >= len-1) || (string.charAt(i+1)== '\n')) {
		flags = TCL_DONT_USE_BRACES|BRACES_UNMATCHED;
	    } else {
		BackSlashResult bs = Interp.backslash(string, i, len);

		// Subtract 1 because the for loop will automatically
		// add one on the next iteration.

		i = (bs.nextIndex - 1);
		flags |= USE_BRACES;
	    }
	    break;
	}
    }
    if (nestingLevel != 0) {
	flags = TCL_DONT_USE_BRACES | BRACES_UNMATCHED;
    }

    return flags;
}

/*
 *----------------------------------------------------------------------
 *
 * convertElement --
 *
 *	This is a companion procedure to scanElement.  Given the
 * 	information produced by scanElement, this procedure converts
 * 	a string to a list element equal to that string.
 *
 * Results:
 *	Conterts a string so to a new string so that Tcl List information
 *	is not lost.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static String 
convertElement(
    String s, 		// Source information for list element.
    int flags) 		// Flags produced by ccanElement
{
    int i = 0;
    char c;
    int len = s.length();

    // See the comment block at the beginning of the ScanElement
    // code for details of how this works.

    if ((s == null) || (s.length() == 0) || (s.charAt(0) == '\0')) {
	return "{}";
    }

    StringBuffer sbuf = new StringBuffer();

    if (((flags & USE_BRACES) != 0) &&
	    ((flags & TCL_DONT_USE_BRACES) == 0)) {
	sbuf.append('{');
	for (i=0; i<len; i++) {
	    sbuf.append(s.charAt(i));
	}
	sbuf.append('}');	    
    } else {
	c = s.charAt(0);
	if (c == '{') {
	    // Can't have a leading brace unless the whole element is
	    // enclosed in braces.  Add a backslash before the brace.
	    // Furthermore, this may destroy the balance between open
	    // and close braces, so set BRACES_UNMATCHED.

	    sbuf.append('\\');
	    sbuf.append('{');
	    i++;
	    flags |= BRACES_UNMATCHED;
	}

	for (; i<len; i++) {
	    c = s.charAt(i);
	    switch (c) {
	    case ']':
	    case '[':
	    case '$':
	    case ';':
	    case ' ':
	    case '\\':
	    case '"':
		sbuf.append('\\');
		break;

	    case '{':
	    case '}':
		// It may not seem necessary to backslash braces, but
		// it is.  The reason for this is that the resulting
		// list element may actually be an element of a sub-list
		// enclosed in braces (e.g. if Tcl_DStringStartSublist
		// has been invoked), so there may be a brace mismatch
		// if the braces aren't backslashed.

		if ((flags & BRACES_UNMATCHED) != 0) {
		    sbuf.append('\\');
		}
		break;

	    case '\f':
		sbuf.append('\\');
		sbuf.append('f');
		continue;

	    case '\n':
		sbuf.append('\\');
		sbuf.append('n');
		continue;

	    case '\r':
		sbuf.append('\\');
		sbuf.append('r');
		continue;

	    case '\t':
		sbuf.append('\\');
		sbuf.append('t');
		continue;
	    case 0x0b:
		// 0x0b is the character '\v' -- this escape sequence is
		// not available in Java, so we hard-code it. We need to
		// support \v to provide compatibility with native Tcl.

		sbuf.append('\\');
		sbuf.append('v');
		continue;
	    }

	    sbuf.append(c);
	}
    }

    return sbuf.toString();
}

/*
 *----------------------------------------------------------------------
 *
 * trimLeft --
 *
 *	Trim characters in "pattern" off the left of a string
 * 	If pattern isn't supplied, whitespace is trimmed
 *
 * Results:
 *	|>None.<|
 *
 * Side effects:
 *	|>None.<|
 *
 *----------------------------------------------------------------------
 */

static String 
TrimLeft 
(String str, 
	String pattern) 
{
    int i,j;
    char c;
    int strLen = str.length();
    int patLen = pattern.length();
    boolean done = false;
    
    for (i=0; i<strLen ; i++) {
	c = str.charAt(i);
	done = true;
	for (j=0; j<patLen; j++) {
	    if (c == pattern.charAt(j)) {
		done = false;
		break;
	    }
	}
	if (done) {
	    break;
	}      
    }
    return str.substring(i,strLen);
}

/*
 *----------------------------------------------------------------------
 *
 * TrimLeft --
 *
 *	|>description<|
 *
 * Results:
 *	|>None.<|
 *
 * Side effects:
 *	|>None.<|
 *
 *----------------------------------------------------------------------
 */

static String TrimLeft (
    String str)
{
    return TrimLeft (str, " \n\t\r");
}

/*
 *----------------------------------------------------------------------
 *
 * TrimRight --
 *
 *	Trim characters in "pattern" off the right of a string
 * 	If pattern isn't supplied, whitespace is trimmed
 *
 * Results:
 *	|>None.<|
 *
 * Side effects:
 *	|>None.<|
 *
 *----------------------------------------------------------------------
 */

static String 
TrimRight (
    String str, 
    String pattern) 
{
    int last = str.length()-1;
    char strArray[] = str.toCharArray();
    int c;
    
    // Remove trailing characters...

    while (last >= 0) {
	c = strArray[last];
	if (pattern.indexOf(c) == -1) {
	    break;
	}
	last--;
    }
    return str.substring(0, last+1);
}

static String TrimRight (
    String str) 
{
    return TrimRight (str, " \n\t\r");
}

/*
 *----------------------------------------------------------------------
 *
 * getBoolean --
 *
 *	Given a string, return a boolean value corresponding
 *	to the string.
 *
 * Results:
 *	
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static boolean 
getBoolean(
    Interp interp, 	// The current interpreter.
    String string) 	// The string representation of the boolean.
throws 
    TclException 	// For malformed boolean values.
{
    String s = string.toLowerCase();

    // The length of 's' needs to be > 1 if it begins with 'o', 
    // in order to compare between "on" and "off".

    if (s.length() > 0) {
	if ("yes".startsWith(s)) {
	    return true;
	} else if ("no".startsWith(s)) {
	    return false;
	} else if ("true".startsWith(s)) {
	    return true;
	} else if ("false".startsWith(s)) {
	    return false;
	} else if ("on".startsWith(s) && s.length() > 1) {
	    return true;
	} else if ("off".startsWith(s) && s.length() > 1) {
	    return false;
	} else if (s.equals("0")) {
	    return false;
	} else if (s.equals("1")) {
	    return true;
	}
    }

    throw new TclException(interp, "expected boolean value but got \"" +
	    string + "\"");
}

/*
 *----------------------------------------------------------------------
 *
 * checkIsLetterOrDigit --
 *
 *	Checks if this VM has a broken Character.isLetterOrDigit()
 * 	implementation.
 *
 * Results:
 *	if this VM has a broken Character.isLetterOrDigit()
 * 	implementation.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

final static boolean 
checkIsLetterOrDigit() 
{
    try {
	Character.isLetterOrDigit('c');
	return false;
    }
    catch (Exception e) {
	return true;
    }
}

/*
 *----------------------------------------------------------------------
 *
 * isLetterOrDigit --
 *
 *	Character.IsLetterOrDigit() is broken in MS JDK. This function is a
 * 	work-around. If it detects the bug, it uses a simplified algorithm
 * 	that returns whether the character is an ASCII letter or digit.
 *
 * Results:
 *	Trye if this character is a letter or digit.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

final static boolean
isLetterOrDigit(
    char c)		// The character toi check.
{
    if (!broken_isLetterOrDigit) {
	return Character.isLetterOrDigit(c);
    } else {
	return ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') ||
		(c >= '0' && c <= '9'));
    }
}


/*
 *-----------------------------------------------------------------------------
 *
 * getActualPlatform -- 
 *
 *	This static procedure returns the integer code for the actuall platform
 *	on which Jacl is running.
 *
 * Results:
 *	Returns and integer.
 *
 * Side effects:
 *	None.
 *
 *-----------------------------------------------------------------------------
 */

final static int
getActualPlatform()
{
    if (Util.isWindows()) {
	return JACL.PLATFORM_WINDOWS;
    }
    if (Util.isMac()) {
	return JACL.PLATFORM_MAC;
    }
    return JACL.PLATFORM_UNIX;
}

/*
 *----------------------------------------------------------------------
 *
 * isUnix --
 *
 *	Returns true if running on a Unix platform.
 *
 * Results:
 *	Returns a boolean.
 *
 * Side effects:
 *	 None.
 *
 *----------------------------------------------------------------------
 */

final static boolean 
isUnix() {
    if (isMac() || isWindows()) {
	return false;
    }
    return true;
}

/*
 *----------------------------------------------------------------------
 *
 * isMac --
 *
 *	Returns true if running on a Mac platform.
 *
 * Results:
 *	Returns a boolean.
 *
 * Side effects:
 *	 None.
 *
 *----------------------------------------------------------------------
 */

final static boolean 
isMac() {
    String os = System.getProperty("os.name");
    if (os.toLowerCase().startsWith("mac")) {
	return true;
    }
    return false;
}

/*
 *----------------------------------------------------------------------
 *
 * isWindows --
 *
 *	Returns true if running on a Windows platform.
 *
 * Results:
 *	Returns a boolean.
 *
 * Side effects:
 *	 None.
 *
 *----------------------------------------------------------------------
 */

final static boolean 
isWindows() {
    String os = System.getProperty("os.name");
    if (os.toLowerCase().startsWith("win")) {
	return true;
    }
    return false;
}

/*
 *----------------------------------------------------------------------
 *
 * setupPrecisionTrace --
 *
 *	Sets up the variable trace of the tcl_precision variable.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	A variable trace is set up for the tcl_precision global
 *	variable.
 *
 *----------------------------------------------------------------------
 */

static void
setupPrecisionTrace(
    Interp interp)		// Current interpreter.
{
    try {
	interp.traceVar("tcl_precision", new PrecTraceProc(),
		TCL.GLOBAL_ONLY|TCL.TRACE_WRITES|TCL.TRACE_READS|
		TCL.TRACE_UNSETS);
    } catch (TclException e) {
	throw new TclRuntimeError("unexpected TclException: " + e);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * printDouble --
 *
 *	Returns the string form of a double number. The exact formatting
 *	of the string depends on the tcl_precision variable.
 *
 * Results:
 * 	Returns the string form of double number.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static String
printDouble(
    double number)	// The number to format into a string.
{
    String s = FormatCmd.toString(number, precision, 10);
    int length = s.length();
    for (int i = 0; i < length; i ++) {
	if ((s.charAt(i) == '.') || Character.isLetter(s.charAt(i))) {
	    return s;
	}
    }
    return s.concat(".0");
}

/*
 *----------------------------------------------------------------------
 *
 * tryGetSystemProperty --
 *
 *	Tries to get a system property. If it fails because of security
 *	exceptions, then return the default value.
 *
 * Results:
 *	The value of the system property. If it fails because of security
 *	exceptions, then return the default value.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static String
tryGetSystemProperty(
    String propName,		// Name of the property
    String defautlValue)	// Default value.
{
    try {
	return System.getProperty(propName);
    } catch (SecurityException e) {
	return defautlValue;
    }
}

} // end Util

/* 
 *----------------------------------------------------------------------
 *
 * PrecTraceProc.java --
 *
 *	 The PrecTraceProc class is used to implement variable traces for
 * 	the tcl_precision variable to control precision used when
 * 	converting floating-point values to strings.
 *
 *----------------------------------------------------------------------
 */

final class PrecTraceProc implements VarTrace {

// Maximal precision supported by Tcl.

static final int TCL_MAX_PREC = 17;


/*
 *----------------------------------------------------------------------
 *
 * traceProc --
 *
 *	This function gets called when the tcl_precision variable is
 *	accessed in the given interpreter.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	If the new value doesn't make sense then this procedure undoes
 *	the effect of the variable modification. Otherwise it modifies
 *	Util.precision that's used by Util.printDouble().
 *
 *----------------------------------------------------------------------
 */

public void
traceProc(
    Interp interp,		// Interpreter containing variable.
    String name1,		// Name of variable.
    String name2,		// Second part of variable name.
    int flags)			// Information about what happened.
throws
    TclException		// If the action is a TCL.TRACES_WRITE and
				// the new value doesn't make sense.
{
    // If the variable is unset, then recreate the trace and restore
    // the default value of the format string.

    if ((flags & TCL.TRACE_UNSETS) != 0) {
	if (((flags & TCL.TRACE_DESTROYED) != 0) && 
		((flags & TCL.INTERP_DESTROYED) == 0)) {
	    interp.traceVar(name1, name2, new PrecTraceProc(),
		    TCL.GLOBAL_ONLY|TCL.TRACE_WRITES|TCL.TRACE_READS|
		    TCL.TRACE_UNSETS);
	    Util.precision = Util.DEFAULT_PRECISION;
	}
	return;
    }

    // When the variable is read, reset its value from our shared
    // value. This is needed in case the variable was modified in
    // some other interpreter so that this interpreter's value is
    // out of date.

    if ((flags & TCL.TRACE_READS) != 0) {
	interp.setVar(name1, name2,
		TclInteger.newInstance(Util.precision),
		flags & TCL.GLOBAL_ONLY);
	return;
    }

    // The variable is being written. Check the new value and disallow
    // it if it isn't reasonable.
    //
    // (ToDo) Disallow it if this is a safe interpreter (we don't want
    // safe interpreters messing up the precision of other
    // interpreters).

    TclObject tobj = interp.getVar(name1, name2, 
	    (flags & TCL.GLOBAL_ONLY) | TCL.DONT_THROW_EXCEPTION);
    String value;

    if (tobj != null) {
	value = tobj.toString();
    } else {
	value = "";
    }

    StrtoulResult r = Util.strtoul(value, 0, 10);

    if ((r == null) || (r.value <= 0) || (r.value > TCL_MAX_PREC) ||
	    (r.value > 100) || (r.index == 0) ||
	    (r.index != value.length())) {
	interp.setVar(name1, name2,
		TclInteger.newInstance(Util.precision),
		TCL.GLOBAL_ONLY);
	throw new TclException(interp, "improper value for precision");
    }

    Util.precision = (int) r.value;
}

} // end PrecTraceProc
