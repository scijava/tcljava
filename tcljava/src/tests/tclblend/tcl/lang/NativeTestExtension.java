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
 * RCS: @(#) $Id: NativeTestExtension.java,v 1.2 2002/12/18 07:07:19 mdejong Exp $
 *
 */

package tcl.lang;

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
    interp.createBTestCommand();
}

} // NativeTestExtension

