/*
 * QSort.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: QSort.java,v 1.2 1999/05/09 01:14:07 dejong Exp $
 *
 */

package tcl.lang;

/*
 * This file is adapted from the JDK 1.0 QSortAlgorithm.java demo program.
 * Original copyright notice is preserveed below.
 *
 * @(#)QSortAlgorithm.java	1.3   29 Feb 1996 James Gosling
 *
 * Copyright (c) 1994-1996 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL or COMMERCIAL purposes and
 * without fee is hereby granted. 
 * Please refer to the file http://www.javasoft.com/copy_trademarks.html
 * for further important copyright and trademark information and to
 * http://www.javasoft.com/licensing.html for further important
 * licensing information for the Java (tm) Technology.
 * 
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES. ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 * 
 * THIS SOFTWARE IS NOT DESIGNED OR INTENDED FOR USE OR RESALE AS ON-LINE
 * CONTROL EQUIPMENT IN HAZARDOUS ENVIRONMENTS REQUIRING FAIL-SAFE
 * PERFORMANCE, SUCH AS IN THE OPERATION OF NUCLEAR FACILITIES, AIRCRAFT
 * NAVIGATION OR COMMUNICATION SYSTEMS, AIR TRAFFIC CONTROL, DIRECT LIFE
 * SUPPORT MACHINES, OR WEAPONS SYSTEMS, IN WHICH THE FAILURE OF THE
 * SOFTWARE COULD LEAD DIRECTLY TO DEATH, PERSONAL INJURY, OR SEVERE
 * PHYSICAL OR ENVIRONMENTAL DAMAGE ("HIGH RISK ACTIVITIES").  SUN
 * SPECIFICALLY DISCLAIMS ANY EXPRESS OR IMPLIED WARRANTY OF FITNESS FOR
 * HIGH RISK ACTIVITIES.
 */

/**
 * Sorts an array of TclObjects.
 */
final class QSort {
    static final int ASCII      = 0;
    static final int INTEGER    = 1;
    static final int REAL       = 2;
    static final int COMMAND    = 3;
    static final int DICTIONARY = 4;

    // Data used during sort.

    private int sortMode;
    private int sortIndex;
    private boolean sortIncreasing;
    private String sortCommand;
    private Interp sortInterp;

    /**
     * This is a generic version of C.A.R Hoare's Quick Sort 
     * algorithm.  This will handle arrays that are already
     * sorted, and arrays with duplicate keys.<BR>
     *
     * If you think of a one dimensional array as going from
     * the lowest index on the left to the highest index on the right
     * then the parameters to this function are lowest index or
     * left and highest index or right.  The first time you call
     * this function it will be with the parameters 0, a.length - 1.
     *
     * @param a       an integer array
     * @param lo0     left boundary of array partition
     * @param hi0     right boundary of array partition
     */
    private final void quickSort(TclObject a[], int lo0, int hi0)
	    throws TclException {
	int lo = lo0;
	int hi = hi0;
	TclObject mid;

	if ( hi0 > lo0) {
	    // Arbitrarily establishing partition element as the midpoint of
	    // the array.
	    mid = a[ ( lo0 + hi0 ) / 2 ];

	    // loop through the array until indices cross
	    while( lo <= hi ) {
		// find the first element that is greater than or equal to 
		// the partition element starting from the left Index.

		while( ( lo < hi0 ) && (compare(a[lo],mid) < 0)) {
		    ++lo;
		}

		// find an element that is smaller than or equal to 
		// the partition element starting from the right Index.

		while( ( hi > lo0 ) && (compare(a[hi],mid) > 0)) {
		    --hi;
		}

		// if the indexes have not crossed, swap
		if( lo <= hi ) {
		    swap(a, lo, hi);
		    ++lo;
		    --hi;
		}
	    }

	    // If the right index has not reached the left side of array
	    // must now sort the left partition.

	    if( lo0 < hi ) {
		quickSort( a, lo0, hi );
	    }

	    // If the left index has not reached the right side of array
	    // must now sort the right partition.

	    if( lo < hi0 ) {
		quickSort( a, lo, hi0 );
	    }
	}
    }

    /**
     * Swaps two items in the array.
     *
     * @param a the array.
     * @param i index of first item.
     * @param j index of first item.
     */
    private static final void swap(TclObject a[], int i, int j) {
	TclObject T;
	T = a[i]; 
	a[i] = a[j];
	a[j] = T;
    }

    /**
     * Starts the quick sort with the given parameters.
     *
     * @param interp if cmd is specified, it is evaluated inside this
     *     interp.
     * @param a the array of TclObject's to sort.
     * @param mode the sortng mode.
     * @param increasing true if the sorted array should be in increasing
     *     order.
     * @param cmd the command to use for comparing items. It is used only
     *     if sortMode is COMMAND.
     *
     * @exception TclException if an error occurs during sorting.
     */
    final void sort(Interp interp, TclObject a[], int mode, int index,
	    boolean increasing, String cmd) throws TclException {
	sortInterp = interp;
	sortMode = mode;
	sortIndex = index;
	sortIncreasing = increasing;
	sortCommand = cmd;
	quickSort(a, 0, a.length - 1);
    }

    /**
     * Compares the order of two items in the array.
     *
     * @param obj1 first item.
     * @param obj2 second item.
     * @return 0 if they are equal, 1 if obj1 > obj2, -1 otherwise.
     *
     * @exception TclException if an error occurs during sorting.
     */
    private final int compare(TclObject obj1, TclObject obj2)
	    throws TclException {

	int index;
	int code = 0;

	if (sortIndex != -1) {
	    // The "-index" option was specified.  Treat each object as a
	    // list, extract the requested element from each list, and
	    // compare the elements, not the lists.  The special index "end"
	    // is signaled here with a negative index (other than -1).

	    TclObject obj;
	    if (sortIndex < -1) {
		index = TclList.getLength(sortInterp, obj1) - 1;
	    } else {
		index = sortIndex;
	    }

	    obj = TclList.index(sortInterp, obj1, index);
	    if (obj == null) {
		throw new TclException(sortInterp, "element " + index + 
			" missing from sublist \"" + obj1 + "\"");
	    }
	    obj1 = obj;

	    if (sortIndex < -1) {
		index = TclList.getLength(sortInterp, obj2) - 1;
	    } else {
		index = sortIndex;
	    }

	    obj = TclList.index(sortInterp, obj2, index);
	    if (obj == null) {
		throw new TclException(sortInterp, "element " + index + 
			" missing from sublist \"" + obj2 + "\"");
	    }
	    obj2 = obj;
	}

	switch (sortMode) {
	case ASCII:
	    code = obj1.toString().compareTo(obj2.toString());
	    break;
	case DICTIONARY:
	    code = doDictionary(obj1.toString(), obj2.toString());
	    break;
	case INTEGER:
	    try {
		int int1 = TclInteger.get(sortInterp, obj1);
		int int2 = TclInteger.get(sortInterp, obj2);

		if (int1 > int2) {
		    code = 1;
		} else if (int2 > int1) {
		    code = -1;
		}
	    } catch (TclException e1) {
		sortInterp.addErrorInfo(
		     "\n    (converting list element from string to integer)");
		throw e1;
	    }
	    break;
	case REAL:
	    try {
		double f1 = TclDouble.get(sortInterp, obj1);
		double f2 = TclDouble.get(sortInterp, obj2);

		if (f1 > f2) {
		    code = 1;
		} else if (f2 > f1) {
		    code = -1;
		}
	    } catch (TclException e2) {
		sortInterp.addErrorInfo(
		     "\n    (converting list element from string to real)");
		throw e2;
	    }
	    break;
	case COMMAND:
	    StringBuffer sbuf = new StringBuffer(sortCommand);
	    Util.appendElement(sortInterp, sbuf, obj1.toString());
	    Util.appendElement(sortInterp, sbuf, obj2.toString());
	    try {
		sortInterp.eval(sbuf.toString(), 0);
	    } catch (TclException e3) {
		sortInterp.addErrorInfo(
		     "\n    (user-defined comparison command)");  
		throw e3;
	    }

	    try {
		code = TclInteger.get(sortInterp, sortInterp.getResult());
	    } catch (TclException e) {
		sortInterp.resetResult();
		TclException e4 = new TclException(sortInterp,
			"comparison command returned non-numeric result");
		throw e4;
	    }
	    break;

	default:
	    // Should never come to here.

	    throw new TclRuntimeError("Unknown sortMode " + sortMode);
	}

	if (sortIncreasing) {
	    return code;
	} else {
	    return -code;
	}
    }


    /**
     * Compares the order of two strings in "dictionary" order.
     *
     * @param str1 first item.
     * @param str2 second item.
     * @return 0 if they are equal, 1 if obj1 > obj2, -1 otherwise.
     */
    private final int doDictionary(String str1, String str2) {
	int diff = 0, zeros;
	int secondaryDiff = 0;

	boolean cont = true;
	int i1 = 0, i2 = 0;
	int len1 = str1.length();
	int len2 = str2.length();


	while (cont) {
	    if(i1 >= len1 || i2 >= len2) {
		break;
	    }

	    if (Character.isDigit(str2.charAt(i2)) && 
		    Character.isDigit(str1.charAt(i1))) {

		// There are decimal numbers embedded in the two
		// strings.  Compare them as numbers, rather than
		// strings.  If one number has more leading zeros than
		// the other, the number with more leading zeros sorts
		// later, but only as a secondary choice.

		zeros = 0;
		while ((i2 < (len2 - 1)) && (str2.charAt(i2) == '0')) {
		    i2++;
		    zeros--;
		}
		while ((i1 < (len1 - 1)) && (str1.charAt(i1) == '0')) {
		    i1++;
		    zeros++;
		}
		if (secondaryDiff == 0) {
		    secondaryDiff = zeros;
		}


		// The code below compares the numbers in the two
		// strings without ever converting them to integers.  It
		// does this by first comparing the lengths of the
		// numbers and then comparing the digit values.

		diff = 0;
		while (true) {

		    if(i1 >= len1 || i2 >= len2) {
		        cont = false;
			break;
		    }
		    if (diff == 0) {
			diff = str1.charAt(i1) - str2.charAt(i2);
		    }
		    i1++;
		    i2++;
		    if(i1 >= len1 || i2 >= len2) {
		        cont = false;
		        break;
		    }


		    if (!Character.isDigit(str2.charAt(i2))) {
			if (Character.isDigit(str1.charAt(i1))) {
			    return 1;
			} else {
			    if (diff != 0) {
				return diff;
			    }
			    break;
			}
		    } else if (!Character.isDigit(str1.charAt(i1))) {
			return -1;
		    }
		}
		continue;
	    }
	    diff = str1.charAt(i1) - str2.charAt(i2);
	    if (diff != 0) {
		if (Character.isUpperCase(str1.charAt(i1)) && 
			Character.isLowerCase(str2.charAt(i2))) {
		    diff = Character.toLowerCase(str1.charAt(i1)) - 
			str2.charAt(i2);
		    if (diff != 0) {
			return diff;
		    } else if (secondaryDiff == 0) {
			secondaryDiff = -1;
		    }
		} else if (Character.isUpperCase(str2.charAt(i2)) && 
			Character.isLowerCase(str1.charAt(i1))) {
		    diff = str1.charAt(i1) - 
			Character.toLowerCase(str2.charAt(i2));
		    if (diff != 0) {
			return diff;
		    } else if (secondaryDiff == 0) {
			secondaryDiff = 1;
		    }
		} else {
		    return diff;
		}
	    }
	    i1++;
	    i2++;

	}

	if(i1 >= len1 && i2 < len2) {
	    if (!Character.isDigit(str2.charAt(i2))) {
	        return 1;
	    } else {
	        return -1;
	    }

	} else if(i2 >= len2 && i1 < len1) {
	    if (!Character.isDigit(str1.charAt(i1))) {
	        return -1;
	    } else {
	        return 1;
	    }

	}

	if (diff == 0) {
	    diff = secondaryDiff;
	}
	return diff;
    }
}

