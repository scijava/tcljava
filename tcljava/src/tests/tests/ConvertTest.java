/* 
 * ConvertTest.java --
 *
 *	This file contains the ConvertTest class used by convert.test to
 *	test conversion between Tcl and Java objects.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: ConvertTest.java,v 1.1 1998/10/14 21:09:11 cvsadmin Exp $
 *
 */

package tests;

/*
 * This class is used by convert.test to test conversion between Tcl
 * and Java objects.
 */

public class ConvertTest {
    public static int staticTestParam(Object s) {
	if (s == null) {
	    return 2;
	}
	if (s instanceof String) {
	    if (s.equals("")) {
		return 1;
	    }
	    if (s.equals("null")) {
		return 3;
	    } else if (s.toString().startsWith("java")) {
		return 4;
	    } else {
		return 5;
	    }
	} else {
		return 6;
	}
    }

    public int testParam(Object s) {
	return staticTestParam(s);
    }

    public String getString(int i) {
	return staticGetString(i);
    }

    public static String staticGetString(int i) {
	if (i == 0) {
	    return null;
	} else if (i == 1) {
	    return "";
	} else if (i == 2) {
	    return "null";
	} else {
	    return "foo";
	}
    }

    public String strField0 = null;
    public String strField1 = "";
    public String strField2 = "null";
    public String strField3 = "foo";

    private String strProp0 = null;
    private String strProp1 = "";
    private String strProp2 = "null";
    private String strProp3 = "foo";

    public String getStrProp0() {
	return strProp0;
    }
    public String getStrProp1() {
	return strProp1;
    }
    public String getStrProp2() {
	return strProp2;
    }
    public String getStrProp3() {
	return strProp3;
    }

    public void setStrProp0(String s) {
	strProp0 = s;
    }
    public void setStrProp1(String s) {
	strProp1 = s;
    }
    public void setStrProp2(String s) {
	strProp2 = s;
    }
    public void setStrProp3(String s) {
	strProp3 = s;
    }

    public void voidMethod() {
	/*
	 * A method of void return type. The Tcl result should
	 * be empty string.
	 */
    }

    public Object nullMethod() {
	/*
	 * The Tcl result should be the same as [java::null].
	 */
	return null;
    }

    public Object emptyStringMethod() {
	/*
	 * The Tcl result should be "".
	 */
	return "";
    }
}

