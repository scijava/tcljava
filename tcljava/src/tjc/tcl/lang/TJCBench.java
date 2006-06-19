/*
 * Copyright (c) 2006 Mo DeJong
 *
 * See the file "license.amd" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TJCBench.java,v 1.8 2006/06/19 02:54:27 mdejong Exp $
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
        } else if (testname.equals("InternalIncr")) {
             InternalIncr(interp);
        } else if (testname.equals("InternalTclListAppend")) {
             InternalTclListAppend(interp);
        } else if (testname.equals("InternalTclListLength")) {
             InternalTclListLength(interp);
        } else if (testname.equals("InternalTclListLindex")) {
             InternalTclListLindex(interp);
        } else if (testname.equals("InternalTclStringNewInstance")) {
             InternalTclStringNewInstance(interp);
        } else if (testname.equals("InternalTclIntegerNewInstance")) {
             InternalTclIntegerNewInstance(interp);
        } else if (testname.equals("InternalTclDoubleNewInstance")) {
             InternalTclDoubleNewInstance(interp);
        } else if (testname.equals("InternalTclListNewInstance")) {
             InternalTclListNewInstance(interp);
        } else if (testname.equals("InternalTclStringDuplicate")) {
             InternalTclStringDuplicate(interp);
        } else if (testname.equals("InternalTclIntegerDuplicate")) {
             InternalTclIntegerDuplicate(interp);
        } else if (testname.equals("InternalTclDoubleDuplicate")) {
             InternalTclDoubleDuplicate(interp);
        } else if (testname.equals("InternalTclListDuplicate")) {
             InternalTclListDuplicate(interp);
        } else if (testname.equals("InternalTclIntegerType")) {
             InternalTclIntegerType(interp);
        } else if (testname.equals("InternalTclDoubleType")) {
             InternalTclDoubleType(interp);
        } else if (testname.equals("InternalTclStringType")) {
             InternalTclStringType(interp);
        } else if (testname.equals("InternalTclListType")) {
             InternalTclListType(interp);
        } else if (testname.equals("InternalTclIntegerGet")) {
             InternalTclIntegerGet(interp);
        } else if (testname.equals("InternalExprGetKnownInt")) {
             InternalExprGetKnownInt(interp);
        } else if (testname.equals("InternalExprInlineGetInt")) {
             InternalExprInlineGetInt(interp);
        } else if (testname.equals("InternalTclDoubleGet")) {
             InternalTclDoubleGet(interp);
        } else if (testname.equals("InternalExprGetKnownDouble")) {
             InternalExprGetKnownDouble(interp);
        } else if (testname.equals("InternalSetTclObjectResult")) {
             InternalSetTclObjectResult(interp);
        } else if (testname.equals("InternalResetResult")) {
             InternalResetResult(interp);
        } else if (testname.equals("InternalSetBooleanResult")) {
             InternalSetBooleanResult(interp);
        } else if (testname.equals("InternalSetIntResult")) {
             InternalSetIntResult(interp);
        } else if (testname.equals("InternalSetIntResultViaExprValue")) {
             InternalSetIntResultViaExprValue(interp);
        } else if (testname.equals("InternalExprSetIntResult")) {
             InternalExprSetIntResult(interp);
        } else if (testname.equals("InternalExprOpIntPlus")) {
             InternalExprOpIntPlus(interp);
        } else if (testname.equals("InternalExprOpDoublePlus")) {
             InternalExprOpDoublePlus(interp);
        } else if (testname.equals("InternalExprOpIntNot")) {
             InternalExprOpIntNot(interp);
        } else if (testname.equals("InternalExprOpIntNotGrabReleaseResult")) {
             InternalExprOpIntNotGrabReleaseResult(interp);
        } else if (testname.equals("InternalExprOpIntNotStackValueResult")) {
             InternalExprOpIntNotStackValueResult(interp);
        } else if (testname.equals("InternalExprOpIntNotStackValueIntResult")) {
             InternalExprOpIntNotStackValueIntResult(interp);
        } else {
             throw new TclException(interp, "unknown test name \"" + testname + "\"");
        }
    }

    // Each test must take special care to save
    // the result of operations to a variable
    // so that the optimizer does not incorrectly
    // eliminate what it thinks is dead code.

    static int RESULT_INT = 0;
    static Object RESULT_OBJ = null;

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
        RESULT_INT = TclInteger.get(interp, tobj);
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
        RESULT_INT = (int) TclDouble.get(interp, tobj);
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
        RESULT_INT = (b ? 1 : 0);
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
        RESULT_INT = (b ? 1 : 0);
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
        RESULT_INT = (b ? 1 : 0);
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
        RESULT_INT = TclInteger.get(interp, tobj);
    }

    // Invoke TclList.getLength() on an unshared
    // TclObject with the TclList type. This
    // will get timing info for this commonly
    // used low level operation.

    void InternalTclListLength(Interp interp)
        throws TclException
    {
        TclObject tlist = TclList.newInstance();
        TclObject tobj = interp.checkCommonString(null); // Empty string

        // Create list of length 3
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        int size = 0;

        for (int i=0; i < 5000; i++) {
            size += TclList.getLength(interp, tlist);
        }
        RESULT_INT = size;
    }

    // Invoke TclList.index() in a loop to get
    // timing info for this low level operation.

    void InternalTclListLindex(Interp interp)
        throws TclException
    {
        TclObject tlist = TclList.newInstance();
        TclObject tobj = interp.checkCommonString(null); // Empty string

        // Create list of length 10
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);

        for (int i=0; i < 5000; i++) {
            tobj = TclList.index(interp, tlist, 6);
        }
        RESULT_OBJ = tobj;
    }

    // Invoke TclList.append() on an unshared
    // TclObject with the TclList type. This
    // will get timing info for TclList.append(),
    // a low level and commonly used operation.

    void InternalTclListAppend(Interp interp)
        throws TclException
    {
        TclObject tlist = TclList.newInstance();
        TclObject tobj = interp.checkCommonString(null); // Empty string

        for (int i=0; i < 5000; i++) {
            TclList.append(interp, tlist, tobj);
        }
        RESULT_OBJ = tlist;
    }

    // Establish timing results for allocation of a
    // TclObject with a TclString internal rep.

    void InternalTclStringNewInstance(Interp interp)
        throws TclException
    {
        TclObject tobj = null;

        for (int i=0; i < 5000; i++) {
            tobj = TclString.newInstance("foo");
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for allocation of a
    // TclObject with a TclInteger internal rep.

    void InternalTclIntegerNewInstance(Interp interp)
        throws TclException
    {
        TclObject tobj = null;

        for (int i=0; i < 5000; i++) {
            tobj = TclInteger.newInstance(1);
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for allocation of a
    // TclObject with a TclDouble internal rep.

    void InternalTclDoubleNewInstance(Interp interp)
        throws TclException
    {
        TclObject tobj = null;

        for (int i=0; i < 5000; i++) {
            tobj = TclDouble.newInstance(1.0);
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for allocation of a
    // TclObject with a TclList internal rep.

    void InternalTclListNewInstance(Interp interp)
        throws TclException
    {
        TclObject tobj = null;

        for (int i=0; i < 5000; i++) {
            tobj = TclList.newInstance();
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for TclObject.duplicate()
    // of TclObject with a TclString internal rep.

    void InternalTclStringDuplicate(Interp interp)
        throws TclException
    {
        TclObject tobj = TclString.newInstance("foo");;

        for (int i=0; i < 5000; i++) {
            tobj = tobj.duplicate();
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for TclObject.duplicate()
    // of TclObject with a TclInteger internal rep.

    void InternalTclIntegerDuplicate(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);

        for (int i=0; i < 5000; i++) {
            tobj = tobj.duplicate();
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for TclObject.duplicate()
    // of TclObject with a TclDouble internal rep.

    void InternalTclDoubleDuplicate(Interp interp)
        throws TclException
    {
        TclObject tobj = TclDouble.newInstance(1.0);

        for (int i=0; i < 5000; i++) {
            tobj = tobj.duplicate();
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for TclObject.duplicate()
    // of TclObject with a TclList internal rep.

    void InternalTclListDuplicate(Interp interp)
        throws TclException
    {
        TclObject tobj = TclList.newInstance();

        for (int i=0; i < 5000; i++) {
            tobj = tobj.duplicate();
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for TclObject.isIntType() API.

    void InternalTclIntegerType(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);
        boolean b = false;

        for (int i=0; i < 5000; i++) {
            b = tobj.isIntType();
        }
        RESULT_INT = (b ? 1 : 0);
    }

    // Establish timing results for TclObject.isDoubleType() API.

    void InternalTclDoubleType(Interp interp)
        throws TclException
    {
        TclObject tobj = TclDouble.newInstance(1.0);
        boolean b = false;

        for (int i=0; i < 5000; i++) {
            b = tobj.isDoubleType();
        }
        RESULT_INT = (b ? 1 : 0);
    }

    // Establish timing results for TclObject.isStringType() API.

    void InternalTclStringType(Interp interp)
        throws TclException
    {
        TclObject tobj = TclString.newInstance("foo");
        boolean b = false;

        for (int i=0; i < 5000; i++) {
            b = tobj.isStringType();
        }
        RESULT_INT = (b ? 1 : 0);
    }

    // Establish timing results for TclObject.isListType() API.

    void InternalTclListType(Interp interp)
        throws TclException
    {
        TclObject tobj = TclList.newInstance();
        boolean b = false;

        for (int i=0; i < 5000; i++) {
            b = tobj.isListType();
        }
        RESULT_INT = (b ? 1 : 0);
    }

    // Establish timing results for TclInteger.get().

    void InternalTclIntegerGet(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);
        int ivalue = 0;

        for (int i=0; i < 5000; i++) {
            ivalue = TclInteger.get(interp, tobj);
        }
        RESULT_INT = ivalue;
    }

    // Establish timing results for TJC.exprGetKnownInt(),
    // this API is used to access the int value inside
    // a TclObject known to contain an int in expr code.

    void InternalExprGetKnownInt(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);
        int ivalue = 0;

        for (int i=0; i < 5000; i++) {
            ivalue = TJC.exprGetKnownInt(tobj);
        }
        RESULT_INT = ivalue;
    }

    // Grab the int value out of a TclObject by
    // directly accessing the package protected
    // field. Testing seems to indicate that
    // the method inliner is working well and
    // that the TJC.exprGetKnownInt() method
    // works just as fast as directly accessing
    // the field in this case.

    void InternalExprInlineGetInt(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);
        int ivalue = 0;

        for (int i=0; i < 5000; i++) {
            ivalue = tobj.ivalue;
        }
        RESULT_INT = ivalue;
    }

    // Establish timing results for TclDouble.get().

    void InternalTclDoubleGet(Interp interp)
        throws TclException
    {
        TclObject tobj = TclDouble.newInstance(1.0);
        double d = 0.0;

        for (int i=0; i < 5000; i++) {
            d = TclDouble.get(interp, tobj);
        }
        RESULT_INT = (int) d;
    }

    // Establish timing results for TJC.exprGetKnownInt(),
    // this API is used to access the int value inside
    // a TclObject known to contain an int in expr code.

    void InternalExprGetKnownDouble(Interp interp)
        throws TclException
    {
        TclObject tobj = TclDouble.newInstance(1.0);
        double d = 0.0;

        for (int i=0; i < 5000; i++) {
            d = TJC.exprGetKnownDouble(tobj);
        }
        RESULT_INT = (int) d;
    }

    // Note that there is no test like
    // InternalExprInlineGetInt since there
    // is no double field to access.


    // Establish timing results for integer.setResult(TclObject).

    void InternalSetTclObjectResult(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);

        for (int i=0; i < 5000; i++) {
            interp.setResult(tobj);
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // Establish timing results for integer.resetResult().

    void InternalResetResult(Interp interp)
        throws TclException
    {
        for (int i=0; i < 5000; i++) {
            interp.resetResult();
        }
        RESULT_OBJ = interp.getResult();
    }

    // Establish timing results for integer.setResult(boolean).
    // Both the true and false values have a shared object.

    void InternalSetBooleanResult(Interp interp)
        throws TclException
    {
        boolean b1 = true;
        boolean b2 = false;
        if (RESULT_INT == 0) {
            // Make sure booleans are not seen as compile
            // time constant values.
            b1 = false;
            b2 = true;
        }

        for (int i=0; i < 5000; i++) {
            interp.setResult(b1);
            interp.setResult(b2);
        }
        RESULT_INT = (TclBoolean.get(interp, interp.getResult()) ? 1 : 0);
    }

    // Establish timing results for integer.setResult(int).
    // Both the 0 and 1 values have a shared object.

    void InternalSetIntResult(Interp interp)
        throws TclException
    {
        int i1 = 1;
        int i2 = 0;
        if (RESULT_INT == 0) {
            // Make sure ints are not seen as compile
            // time constant values.
            i1 = 0;
            i2 = 1;
        }

        for (int i=0; i < 5000; i++) {
            interp.setResult(i1);
            interp.setResult(i2);
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // Invoke integer.setResult(int) with a result
    // stored in an ExprValue. This is like the
    // test above except that it includes execution
    // time for getting the int value out of the
    // ExprValue object. In that way, it is like
    // TJC.exprSetResult() but without a type switch.

    void InternalSetIntResultViaExprValue(Interp interp)
        throws TclException
    {
        int i1 = 1;
        int i2 = 0;
        if (RESULT_INT == 0) {
            // Make sure ints are not seen as compile
            // time constant values.
            i1 = 0;
            i2 = 1;
        }
        ExprValue value1 = TJC.exprGetValue(interp, i1, null);
        ExprValue value2 = TJC.exprGetValue(interp, i2, null);

        for (int i=0; i < 5000; i++) {
            interp.setResult(value1.getIntValue());
            interp.setResult(value2.getIntValue());
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // Establish timing results for TJC.exprSetResult()
    // when invoked with an int type. These results
    // can be compared to InternalSetIntResultViaExprValue
    // to see how long the type query and branch
    // operation is taking.

    void InternalExprSetIntResult(Interp interp)
        throws TclException
    {
        int i1 = 1;
        int i2 = 0;
        if (RESULT_INT == 0) {
            // Make sure ints are not seen as compile
            // time constant values.
            i1 = 0;
            i2 = 1;
        }
        ExprValue value1 = TJC.exprGetValue(interp, i1, null);
        ExprValue value2 = TJC.exprGetValue(interp, i2, null);

        for (int i=0; i < 5000; i++) {
            TJC.exprSetResult(interp, value1);
            TJC.exprSetResult(interp, value2);
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // Invoke binary + operator on a TclInteger.
    // This tests the execution time for just the
    // Expression.evalBinaryOperator() API.

    void InternalExprOpIntPlus(Interp interp)
        throws TclException
    {
        ExprValue value1 = new ExprValue(1, null);
        ExprValue value2 = new ExprValue(2, null);

        for (int i=0; i < 5000; i++) {
            Expression.evalBinaryOperator(interp, TJC.EXPR_OP_PLUS, value1, value2);
        }
        RESULT_INT = value1.getIntValue();
    }

    // Invoke binary + operator on a TclDouble.
    // This tests the execution time for just the
    // Expression.evalBinaryOperator() API.

    void InternalExprOpDoublePlus(Interp interp)
        throws TclException
    {
        ExprValue value1 = new ExprValue(1.0, null);
        ExprValue value2 = new ExprValue(2.0, null);

        for (int i=0; i < 5000; i++) {
            Expression.evalBinaryOperator(interp, TJC.EXPR_OP_PLUS, value1, value2);
        }
        RESULT_INT = (int) value1.getDoubleValue();
    }

    // Invoke unary ! operator on a TclInteger.
    // This tests the execution time for just the
    // Expression.evalBinaryOperator() API.

    void InternalExprOpIntNot(Interp interp)
        throws TclException
    {
        ExprValue value = new ExprValue(1, null);

        for (int i=0; i < 5000; i++) {
            Expression.evalUnaryOperator(interp, TJC.EXPR_OP_UNARY_NOT, value);
        }
        RESULT_INT = value.getIntValue();
    }

    // This is the logic emitted for a unary
    // logical ! operator with +inline-containers.
    // This code will grab an ExprValue, init it,
    // invoke the operator method, and then
    // set the interp result.

    void InternalExprOpIntNotGrabReleaseResult(Interp interp)
        throws TclException
    {
        // expr {!1}

        for (int i=0; i < 5000; i++) {
            ExprValue tmp0 = TJC.exprGetValue(interp, 1, null);
            TJC.exprUnaryOperator(interp, TJC.EXPR_OP_UNARY_NOT, tmp0);
            TJC.exprSetResult(interp, tmp0);
            TJC.exprReleaseValue(interp, tmp0);
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // This is the logic emitted for a unary
    // logical ! operator with +inline-expr.
    // Unlike InternalExprOpIntNotGrabReleaseResult
    // this implementation will reuse an ExprValue
    // saved on the stack and avoid having to
    // grab and release during each loop iteration.

    void InternalExprOpIntNotStackValueResult(Interp interp)
        throws TclException
    {
        // expr {!1}

        ExprValue evs0 = TJC.exprGetValue(interp);

        for (int i=0; i < 5000; i++) {
            ExprValue tmp0 = evs0;
            tmp0.setIntValue(1);
            TJC.exprUnaryOperator(interp, TJC.EXPR_OP_UNARY_NOT, tmp0);
            TJC.exprSetResult(interp, tmp0);
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // This method is like InternalExprOpIntNotStackValueResult
    // except that it invokes the Interp.setResult(int) method
    // directly instead of calling TJC.exprSetResult().
    // This optimization is valid because we know the result
    // of a unary not is always of type int. This optimization
    // avoids a method invocation, a branching operation,
    // and a call to ExprValue.getType() for each iteration
    // of the loop.

    void InternalExprOpIntNotStackValueIntResult(Interp interp)
        throws TclException
    {
        // expr {!1}

        ExprValue evs0 = TJC.exprGetValue(interp);

        for (int i=0; i < 5000; i++) {
            ExprValue tmp0 = evs0;
            tmp0.setIntValue(1);
            TJC.exprUnaryOperator(interp, TJC.EXPR_OP_UNARY_NOT, tmp0);
            interp.setResult( tmp0.getIntValue() );
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

} // end class TJCBench

