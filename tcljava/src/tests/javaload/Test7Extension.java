/*
 * Test7Extension.java
 *
 *    Test instantiatoion failures.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Test7Extension.java,v 1.1 1998/10/14 21:09:11 cvsadmin Exp $
 *
 */

import tcl.lang.*; 

public class
Test7Extension extends Extension {
    
    public Test7Extension() throws InstantiationException {
	throw new InstantiationException();
    }
    
    public void init(Interp interp) {
	interp.createCommand("test2", new Test2Cmd());
    }
}

