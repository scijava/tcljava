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
 * RCS: @(#) $Id: FindElemResult.java,v 1.1 1998/10/14 21:09:21 cvsadmin Exp $
 *
 */

package tcl.lang;

/*
 * Result returned by Util.findElement().
 */

class FindElemResult {

/*
 * The end of the element in the original string -- the index of the
 * character immediately behind the element.
 */

int elemEnd;

/*
 * The element itself.
 */

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
    int i,			// Initial value for elemEnd.
    String s)			// Initial value for elem.
{
    elemEnd = i;
    elem = s;
}

} // end FindElemResult

