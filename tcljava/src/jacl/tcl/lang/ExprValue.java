/*
 * ExprValue.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ExprValue.java,v 1.1 1998/10/14 21:09:21 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * Describes an expression value, which can be either an integer (the
 * usual case), a double-precision floating-point value, or a string.
 * A given number has only one value at a time.
 */

class ExprValue {
    static final int ERROR  = 0;
    static final int INT    = 1;
    static final int DOUBLE = 2;
    static final int STRING = 3;

    /**
     * Integer value, if any.
     */
    long intValue;

    /**
     * Floating-point value, if any.
     */
    double  doubleValue;

    /**
     * Used to hold a string value, if any.
     */
    String stringValue;
    
    /**
     * Type of value: INT, DOUBLE, or STRING.
     */
    int type;

    /**
     * Constructor.
     */
    ExprValue() {
	type = ERROR;
    }
    ExprValue(long i) {
	intValue = i;
	type = INT;
    }
    ExprValue(double d) {
	doubleValue = d;
	type = DOUBLE;
    }
    ExprValue(String s) {
	stringValue = s;
	type = STRING;
    }
}

