/* 
 * CharPointer.java --
 *
 *	Used in the Parser, this class implements the functionality
 * 	of a C character pointer.  CharPointers referencing the same
 *	script share a reference to one array, while maintaining there
 * 	own current index into the array.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: CharPointer.java,v 1.3 1999/07/28 01:50:46 mo Exp $
 */

package tcl.lang;

class CharPointer {

// A string of characters.

char[] array;

// The current index into the array.

int index;

/*
 *----------------------------------------------------------------------
 *
 * CharPointer --
 *
 *	Default initialization.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

CharPointer() 
{
    this.array = null;
    this.index = -1;
}

/*
 *----------------------------------------------------------------------
 *
 * CharPointer --
 *
 *	Make a "copy" of the argument.  This is used when the index
 *	of the original CharPointer shouldn't change.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

CharPointer(
    CharPointer c) 
{
    this.array = c.array;
    this.index = c.index;
}

/*
 *----------------------------------------------------------------------
 *
 * CharPointer --
 *
 *	Create an array of chars that is one char more than the length
 *	of str.  This is used to store \0 after the last char in the 
 * 	string without causing exceptions.
 *
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

CharPointer(
    String str) 
{
    int len = str.length();
    this.array = new char[len + 1];
    str.getChars(0, len, this.array, 0);
    this.array[len] = '\0';
    this.index = 0;
}

/*
 *----------------------------------------------------------------------
 *
 * charAt --
 *
 *	Used to map C style '*ptr' into Java. 
 *
 * Results:
 *	A character at the current index
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

char
charAt() 
{
    return(array[index]);
}

/*
 *----------------------------------------------------------------------
 *
 * charAt --
 *	Used to map C style 'ptr[x]' into Java. 
 *	 
 *
 * Results:
 *	A character at the current index plus some value.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

char
charAt(
    int x) 
{
    return(array[index + x]);
}

/*
 *----------------------------------------------------------------------
 *
 * length --
 *
 *	Since a '\0' char is stored at the end of the script the true
 *	length of the strinf is one less than the length of array.
 *
 * Results:
 *	The true size of the string.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

int
length()
{
    return (array.length - 1);
}

/*
 *----------------------------------------------------------------------
 *
 * toString --
 *
 *	Get debug info about the string being referenced.  The attempt
 *	is to print the array and a ^ below the output that indicates
 *	where the index is pointing to.  This only work if the string
 *	fits on one line of text.
 *
 * Results:
 *	A String used for debug.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public String
toString() 
{
    return new String(array,0,array.length-1);

    /*
    String str = new String(array,array.length-1,

    StringBuffer sbuf = new StringBuffer();
    sbuf.append(new String(array));
    sbuf.append("\n");
    for(int i=0; i<index; i++) {
	sbuf.append(" ");
    }
    sbuf.append("^");
    return (sbuf.toString());
    */
}
} // end CharPointer
