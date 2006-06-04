/*
 * Copyright (c) 2006 Mo DeJong
 *
 * See the file "license.amd" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TJCBench.java,v 1.1 2006/06/04 20:35:21 mdejong Exp $
 *
 */

// This class is an ungly workaround for a bug in the
// JDK 1.4 JVM that makes it impossible to load code
// in the tcl.lang.* package via a classloader. This
// class define Tcl tests that check implementation
// runtimes for the Tcl Bench suite. These tests
// should be defined in code compiled by Tcl Bench
// but compiling code inside a package does not
// work when classes access other package members.

package tcl.lang;

public class TJCBench extends TJC.CompiledCommand {

    public void cmdProc(
        Interp interp,
        TclObject[] objv)
            throws TclException
    {
	if (objv.length != 2) {
	    throw new TclNumArgsException(interp, 2, objv, "testname");
	}
        String testname = objv[1].toString();

        if (testname.equals("InternalExprParseIntValue")) {
             InternalExprParseIntValue(interp);
        } else if (testname.equals("InternalExprParseDoubleValue")) {
             InternalExprParseDoubleValue(interp);
        } else if (testname.equals("InternalExprGetBooleanInt")) {
             InternalExprGetBooleanInt(interp);
        } else if (testname.equals("InternalExprGetBooleanDouble")) {
             InternalExprGetBooleanDouble(interp);
        } else if (testname.equals("InternalExprGetBooleanString")) {
             InternalExprGetBooleanString(interp);
        } else if (testname.equals("InternalExprOpIntPlus")) {
             InternalExprOpIntPlus(interp);
        } else if (testname.equals("InternalExprOpDoublePlus")) {
             InternalExprOpDoublePlus(interp);
        } else if (testname.equals("InternalExprOpIntNot")) {
             InternalExprOpIntNot(interp);
        } else if (testname.equals("InternalIncr")) {
             InternalIncr(interp);
        } else {
             throw new TclException(interp, "unknown test name \"" + testname + "\"");
        }
    }

    // Invoke ExprParseObject() over and over again on a
    // TclObject with a TclInteger internal rep.

    void InternalExprParseIntValue(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);
        ExprValue value = new ExprValue(0, null);

        for (int i=0; i < 5000; i++) {
            Expression.ExprParseObject(interp, tobj, value);
        }
    }

    // Invoke ExprParseObject() over and over again on a
    // TclObject with a TclDouble internal rep.

    void InternalExprParseDoubleValue(Interp interp)
        throws TclException
    {
        TclObject tobj = TclDouble.newInstance(1.0);
        ExprValue value = new ExprValue(0, null);

        for (int i=0; i < 5000; i++) {
            Expression.ExprParseObject(interp, tobj, value);
        }
    }

    // Invoke TJC.getBoolean() over and over with a TclInteger

    void InternalExprGetBooleanInt(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);
        boolean b = false;

        for (int i=0; i < 5000; i++) {
            b = TJC.getBoolean(interp, tobj);
        }

        b = !b; // Don't optimize away boolean assignment
    }

    // Invoke TJC.getBoolean() over and over with a TclDouble

    void InternalExprGetBooleanDouble(Interp interp)
        throws TclException
    {
        TclObject tobj = TclDouble.newInstance(1.0);
        boolean b = false;

        for (int i=0; i < 5000; i++) {
            b = TJC.getBoolean(interp, tobj);
        }

        b = !b; // Don't optimize away boolean assignment
    }

    // Invoke TJC.getBoolean() over and over with a TclString

    void InternalExprGetBooleanString(Interp interp)
        throws TclException
    {
        TclObject tobj = TclString.newInstance("true");
        boolean b = false;

        for (int i=0; i < 5000; i++) {
            b = TJC.getBoolean(interp, tobj);
        }

        b = !b; // Don't optimize away boolean assignment
    }

    // Invoke binary + operator on a TclInteger.

    void InternalExprOpIntPlus(Interp interp)
        throws TclException
    {
        ExprValue value1 = new ExprValue(1, null);
        ExprValue value2 = new ExprValue(2, null);

        for (int i=0; i < 5000; i++) {
            Expression.evalBinaryOperator(interp, TJC.EXPR_OP_PLUS, value1, value2);
        }
    }

    // Invoke binary + operator on a TclDouble.

    void InternalExprOpDoublePlus(Interp interp)
        throws TclException
    {
        ExprValue value1 = new ExprValue(1.0, null);
        ExprValue value2 = new ExprValue(2.0, null);

        for (int i=0; i < 5000; i++) {
            Expression.evalBinaryOperator(interp, TJC.EXPR_OP_PLUS, value1, value2);
        }
    }

    // Invoke unary ! operator on a TclInteger.

    void InternalExprOpIntNot(Interp interp)
        throws TclException
    {
        ExprValue value = new ExprValue(1, null);

        for (int i=0; i < 5000; i++) {
            Expression.evalUnaryOperator(interp, TJC.EXPR_OP_UNARY_NOT, value);
        }
    }

    // Invoke "incr" operation on an unshared TclInteger.
    // This checks the runtime execution speed
    // of the TclInteger.incr() operation in the
    // most common case of an unshared int.

    void InternalIncr(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(0);

        for (int i=0; i < 5000; i++) {
            TclInteger.incr(interp, tobj, 1);
        }
    }

} // end class TJCBench

