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
 * RCS: @(#) $Id: JavaTestExtension.java,v 1.8 2006/06/17 20:48:11 mdejong Exp $
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
    interp.createCommand("testevalex",        new TestEvalExCmd());
    interp.createCommand("testparser",        new TestParserCmd());
    interp.createCommand("testparsevar",      new TestParsevarCmd());
    interp.createCommand("testparsevarname",  new TestParsevarnameCmd());
    interp.createCommand("testexprparser",    new TestExprParserCmd());
    interp.createCommand("testevalobjv",      new TestEvalObjvCmd());
    interp.createCommand("testcompcode",      new TestcompcodeCmd());
    interp.createCommand("testsetplatform",   new TestsetplatformCmd());
    interp.createCommand("testtranslatefilename",
            new TesttranslatefilenameCmd());
    interp.createCommand("testchannel",       new TestChannelCmd());
    interp.createCommand("testvarframe",      new TestVarFrameCmd());
    interp.createCommand("testtclobjectmemory",      new TclObjectMemory());

    // Create "testobj" and friends
    TestObjCmd.init(interp);

    // Create "T1" and "T2" test expr functions
    createExprTestFunctions(interp);
}

void createExprTestFunctions(Interp interp) {
    Expression expr = interp.expr;
    expr.registerMathFunction("T1", new TestMathFunc(123));
    expr.registerMathFunction("T2", new TestMathFunc(345));
    expr.registerMathFunction("T3", new TestMathFunc2());
}

} // JavaTestExtension

// Implement "T1" and "T2" expr math functions

class TestMathFunc extends NoArgMathFunction {
    int clientData;

    TestMathFunc(int clientData) {
        this.clientData = clientData;
    }

    void apply(Interp interp, ExprValue value)
	    throws TclException {
        value.setIntValue(clientData);
    }
}

// Implement "T3" expr math function

class TestMathFunc2 extends MathFunction {

    TestMathFunc2() {
    	argTypes = new int[2];
	argTypes[0] = EITHER;
	argTypes[1] = EITHER;
    }

    // Return the maximum of the two arguments with the correct type.

    void apply(Interp interp, ExprValue[] values)
	    throws TclException
    {
        ExprValue arg0 = values[0];
        ExprValue arg1 = values[1];

        if (arg0.isIntType()) {
            int i0 = arg0.getIntValue();

            if (arg1.isIntType()) {
                int i1 = arg1.getIntValue();
                arg0.setIntValue( ((i0 > i1)? i0 : i1) );
            } else if (arg1.isDoubleType()) {
                double d0 = (double) i0;
                double d1 = arg1.getDoubleValue();
                arg0.setDoubleValue( ((d0 > d1)? d0 : d1) );
            } else {
                throw new TclException(interp,
                    "T3: wrong type for arg 2");
            }
        } else if (arg0.isDoubleType()) {
            double d0 = arg0.getDoubleValue();

            if (arg1.isIntType()) {
                double d1 = (double) arg1.getIntValue();
                arg0.setDoubleValue( ((d0 > d1)? d0 : d1) );
            } else if (arg1.isDoubleType()) {
                double d1 = arg1.getDoubleValue();
                arg0.setDoubleValue( ((d0 > d1)? d0 : d1) );
            } else {
                throw new TclException(interp,
                    "T3: wrong type for arg 2");
            }
        } else {
            throw new TclException(interp,
                "T3: wrong type for arg 1");
        }
    }
}


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
