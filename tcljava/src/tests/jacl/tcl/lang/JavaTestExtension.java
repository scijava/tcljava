/*
 * JavaTestExtension.java --
 *
 *	This file contains loads classes needed by the Jacl
 *	test suite.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: JavaTestExtension.java,v 1.2 2002/01/23 09:53:50 mdejong Exp $
 *
 */

package tcl.lang;

import java.util.*;

/*
 * This Extension class contains commands used by the Jacl
 * test suite.
 */

public class JavaTestExtension extends Extension {

/*
 *----------------------------------------------------------------------
 *
 * init --
 *
 *	Initializes the JavaTestExtension.
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
    interp.createCommand("testeval2",         new TestEval2Cmd());
    interp.createCommand("testparser",        new TestParserCmd());
    interp.createCommand("testparsevar",      new TestParsevarCmd());
    interp.createCommand("testparsevarname",  new TestParsevarnameCmd());
    interp.createCommand("testevalobjv",      new TestEvalObjvCmd());
    interp.createCommand("testcompcode",      new TestcompcodeCmd());
    interp.createCommand("testsetplatform",   new TestsetplatformCmd());
    interp.createCommand("testtranslatefilename",
            new TesttranslatefilenameCmd());
    interp.createCommand("testchannel",
            new TestChannelCmd());
}

} // JavaTestExtension

class TestAssocData implements AssocData {
Interp interp;
String testCmd;
String removeCmd;

public TestAssocData(Interp i, String tcmd, String rcmd)
{
    interp = i;
    testCmd = tcmd;
    removeCmd = rcmd;
}

public void
disposeAssocData(Interp interp)
{
    try {
	interp.eval(removeCmd);
    } catch (TclException e) {
	throw new TclRuntimeError("unexpected TclException: " + e);
    }
}

public void test() throws TclException
{
    interp.eval(testCmd);
}

public String getData() throws TclException
{
    return testCmd;
}

} // end TestAssocData

class Test2AssocData implements AssocData {
String internalData;

public Test2AssocData(String data)
{
    internalData = data;
}

public void
disposeAssocData(Interp interp)
{
}

public String getData() throws TclException
{
    return internalData;
}

} // end Test2AssocData
