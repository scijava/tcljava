/*
 * Test1Extension.java
 *
 *    Test the loading of a java package.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Test1Extension.java,v 1.1 1998/10/14 21:09:11 cvsadmin Exp $
 *
 */

package tests.javaload;
import tcl.lang.*; 

public class
Test1Extension extends Extension {
    public void init(Interp interp) {
	interp.createCommand("test1", new Test1Cmd());
    }
}


