/*
 * Test5Extension.java
 *
 *    Loads multiple classes from a different classpaths
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Test5Extension.java,v 1.1 1998/10/14 21:09:11 cvsadmin Exp $
 *
 */

import tcl.lang.*; 

public class
Test5Extension extends Extension {
    public void init(Interp interp) {
	interp.createCommand("test5", new tests.javaload.Test1Cmd());
	interp.createCommand("test5b", new Test2Cmd());
    }
}

