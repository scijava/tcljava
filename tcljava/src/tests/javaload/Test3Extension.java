/*
 * Test3Extension.java
 *
 *    This Extension dosent subclass from tcl.lang.Extension
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Test3Extension.java,v 1.1 1998/10/14 21:09:11 cvsadmin Exp $
 *
 */

import tcl.lang.*;

public class
Test3Extension {
    public void init(Interp interp) {
	interp.createCommand("test3", new Test2Cmd());
    }
}

