/*
 * RegexMatcher.java --
 *
 *	Interface for regular expression matcher.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: RegexMatcher.java,v 1.1 1998/10/14 21:09:20 cvsadmin Exp $
 *
 */

package tcl.lang;

/*
 * This is an interface for regular expression matcher.
 *
 * Currently, Jacl has no built-in support of regular
 * expression. Regexp functionality for Jacl is provided via the
 * tcl.regex package, which may not be available in the CLASSPATH when
 * Jacl is being compiled or executed. Therefore, Jacl does not
 * directly refer to the tcl.regex package. Instead, when Jacl starts
 * up, it dynamically looks up the class tcl.regex.OroRegexMatcher,
 * which implements the RegexMatcher interface. If the OroRegexMatcher
 * class cannot be loaded, then Jacl will continue to execute, but
 * regexp functionality will not be supported.
 *
 */

public interface RegexMatcher {


/*
 *-----------------------------------------------------------------------------
 *
 * match --
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

public boolean
match(
    Interp interp,   			// Current interpreter. 
    String string,			// The string to match.
    String pattern)			// The regular expression.
throws
    TclException;			// If regex cannot be compiled.

} // end RegexMatcher
