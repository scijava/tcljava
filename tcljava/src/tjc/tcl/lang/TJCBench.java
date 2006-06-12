/*
 * Copyright (c) 2006 Mo DeJong
 *
 * See the file "license.amd" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TJCBench.java,v 1.6 2006/06/12 21:33:02 mdejong Exp $
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
        size += 1; // Don't optimize away int assignment
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
        tlist = tobj; // Don't optimize away assignment
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
        tobj.toString(); // Don't optimize away assignment
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
        tobj.toString(); // Don't optimize away assignment
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
        tobj.toString(); // Don't optimize away assignment
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
        tobj.toString(); // Don't optimize away assignment
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
        tobj.toString(); // Don't optimize away assignment
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
        tobj.toString(); // Don't optimize away assignment
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
        tobj.toString(); // Don't optimize away assignment
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
        tobj.toString(); // Don't optimize away assignment
    }

    // Establish timing results for TclObject.isIntegerType() API.

    void InternalTclIntegerType(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);
        boolean b = false;

        for (int i=0; i < 5000; i++) {
            b = tobj.isIntegerType();
        }
        b = !b; // Don't optimize away assignment
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
        b = !b; // Don't optimize away assignment
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
        b = !b; // Don't optimize away assignment
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
        b = !b; // Don't optimize away assignment
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
        ivalue++; // Don't optimize away assignment
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
        ivalue++; // Don't optimize away assignment
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
        ivalue++; // Don't optimize away assignment
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
        d++; // Don't optimize away assignment
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
        d++; // Don't optimize away assignment
    }

    // Note that there is no test like
    // InternalExprInlineGetInt since there
    // is no double field to access.

} // end class TJCBench

