/*
 * FindElemResult.java --
 *
 *	Result returned by Util.findElement().
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: FindElemResult.java,v 1.2 2005/10/19 23:37:38 mdejong Exp $
 *
 */

package tcl.lang;

/*
 * Result returned by Util.findElement().
 */

class FindElemResult {

// The start index of the element in the original string -- the index of the
// first character in the element.

int elemStart;

// The end index of the element in the original string -- the index of the
// character immediately behind the element.

int elemEnd;

// The element itself.

String elem;


/*
 *----------------------------------------------------------------------
 *
 * FindElemResult --
 *
 *	Construct a new FindElemResult object.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The member fields are initialized.
 *
 *----------------------------------------------------------------------
 */

FindElemResult(
    int start,			// Initial value for elemStart.
    int end,			// Initial value for elemEnd.
    String e)			// Initial value for elem.
{
    elemStart = start;
    elemEnd = end;
    elem = e;
}

} // end FindElemResult

