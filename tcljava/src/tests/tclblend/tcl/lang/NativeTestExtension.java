/*
 * NativeTestExtension.java --
 *
 *	This Extension class contains commands used by the Tcl Blend
 *	test suite.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: NativeTestExtension.java,v 1.1 1999/05/10 04:08:52 dejong Exp $
 *
 */

package tcl.lang;

/*
 * This Extension class contains commands used by the Jacl
 * test suite.
 */

public class NativeTestExtension extends Extension {

/*
 *----------------------------------------------------------------------
 *
 * init --
 *
 *	Initializes the NativeTestExtension.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Commands are created in the interpreter.
 *
 *----------------------------------------------------------------------
 */

public void
init(
    Interp interp)
{
    interp.createCommand("jtest", 	      new JtestCmd());
    interp.createCommand("testcompcode",    new TestcompcodeCmd());
}

} // NativeTestExtension

