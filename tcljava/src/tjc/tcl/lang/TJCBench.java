/*
 * Copyright (c) 2006 Mo DeJong
 *
 * See the file "license.amd" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TJCBench.java,v 1.3 2006/06/06 04:48:03 mdejong Exp $
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

    static String fiveHundredZeros;
    static String newlineStr;

    static {
        StringBuffer sb1 = new StringBuffer( 500 );

        // Generate string of 500 zeros
        for (int i=0; i < 50 ; i++) {
            sb1.append("0000000000");
        }
        fiveHundredZeros = sb1.toString();

        // Generate newlinestr
        String rep = "ONETWOTHREE\n54985348543538434535435\nxysssksdalsdjjalsk\n";
        StringBuffer sb2 = new StringBuffer( rep.length() * 500 );
        for (int i=0; i < 500 ; i++) {
            sb2.append(rep);
        }
        newlineStr = sb2.toString();
    }

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
        } else if (testname.equals("InternalSplitCharCmd")) {
             InternalSplitCharCmd(interp);
        } else if (testname.equals("InternalSplitDefaultCmd")) {
             InternalSplitDefaultCmd(interp);
        } else if (testname.equals("InternalSplitEveryCharCmd")) {
             InternalSplitEveryCharCmd(interp);
        } else if (testname.equals("InternalSplitAppendElement")) {
             InternalSplitAppendElement(interp);
        } else if (testname.equals("InternalSplitAppendEmptyString")) {
             InternalSplitAppendEmptyString(interp);
        } else if (testname.equals("InternalSplitAppendNewline")) {
             InternalSplitAppendNewline(interp);
        } else if (testname.equals("InternalSplitAppendSubstring")) {
             InternalSplitAppendSubstring(interp);
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

    // Invoke "split" command to get execution time results.
    // The split command is generating some very strange
    // timing results WRT the int/double internal rep
    // changes to TclObject.

    void InternalSplitCharCmd(Interp interp)
        throws TclException
    {
        TclObject tstr = TclString.newInstance(newlineStr);

        // Lookup "split" command
        Command cmd = TJC.resolveCmd(interp, "split").cmd;

        // Invoke [split $s "\n"] 5 times
        TclObject[] objv = new TclObject[3];
        objv[0] = TclString.newInstance("split");
        objv[1] = tstr;
        objv[2] = TclString.newInstance("\n");

        for (int i=0; i < 5; i++) {
            cmd.cmdProc(interp, objv);
        }
    }

    // Invoke "split" with default chars to get timing info.
    // This invocation also shows some very strange slowdown
    // results WRT the int/double TclObject rewrite.

    void InternalSplitDefaultCmd(Interp interp)
        throws TclException
    {
        TclObject tstr = TclString.newInstance(newlineStr);

        // Lookup "split" command
        Command cmd = TJC.resolveCmd(interp, "split").cmd;

        // Invoke [split $s] 5 times
        TclObject[] objv = new TclObject[2];
        objv[0] = TclString.newInstance("split");
        objv[1] = tstr;

        for (int i=0; i < 5; i++) {
            cmd.cmdProc(interp, objv);
        }
    }

    // Invoke "split" with empty string argument so that
    // string is split into a list containing every character.
    // This invocation does not display a strange slowdown
    // that is seen in the two tests above.

    void InternalSplitEveryCharCmd(Interp interp)
        throws TclException
    {
        TclObject tstr = TclString.newInstance(fiveHundredZeros);

        // Lookup "split" command
        Command cmd = TJC.resolveCmd(interp, "split").cmd;

        // Invoke [split $s ""] 5 times
        TclObject[] objv = new TclObject[3];
        objv[0] = TclString.newInstance("split");
        objv[1] = tstr;
        objv[2] = TclString.newInstance("");

        for (int i=0; i < 5; i++) {
            cmd.cmdProc(interp, objv);
        }
    }

    // Invoke the SplitCmd.appendElement() method
    // in a loop to get timing results for the
    // list append and cache value lookup logic.
    // This inlines logic from the SplitCmd class
    // to get timing info for a split on one char.

    void InternalSplitAppendElement(Interp interp)
        throws TclException
    {
        String string = newlineStr;
        int slen = string.length();

        // Invoke [split $s "\n"] 5 times
        final char splitChar = '\n';

        for (int loop=0; loop < 5; loop++) {
            int i = 0;
            int elemStart = 0;

            // Create TclList that will be appended to.
            TclObject list = TclList.newInstance();

            for (; i < slen; i++) {
                if (string.charAt(i) == splitChar) {
                    SplitCmd.appendElement(interp, list, string, elemStart, i);
                    elemStart = i+1;
                }
            }
            if (i != 0) {
                SplitCmd.appendElement(interp, list, string, elemStart, i);
            }
        }
    }

    // Invoke SplitCmd.appendElement() to append
    // a empty string element to the list.

    void InternalSplitAppendEmptyString(Interp interp)
        throws TclException
    {
        String string = newlineStr;
        int slen = string.length();

        // Append empty string each time a newline
        // is found in the string.
        final char splitChar = '\n';

        for (int loop=0; loop < 5; loop++) {
            int i = 0;
            int elemStart = 0;

            // Create TclList that will be appended to.
            TclObject list = TclList.newInstance();

            for (; i < slen; i++) {
                if (string.charAt(i) == splitChar) {
                    SplitCmd.appendElement(interp, list, string, i, i);
                }
            }
        }
    }

    // Invoke SplitCmd.appendElement() to append
    // a newline character element to the list.

    void InternalSplitAppendNewline(Interp interp)
        throws TclException
    {
        String string = newlineStr;
        int slen = string.length();

        // Append empty string each time a newline
        // is found in the string.
        final char splitChar = '\n';

        for (int loop=0; loop < 5; loop++) {
            int i = 0;
            int elemStart = 0;

            // Create TclList that will be appended to.
            TclObject list = TclList.newInstance();

            for (; i < slen; i++) {
                if (string.charAt(i) == splitChar) {
                    SplitCmd.appendElement(interp, list, string, i, i+1);
                }
            }
        }
    }

    // Invoke SplitCmd.appendElement() to append
    // a substring element to the list. The
    // append operation is only done when a
    // string of length 2 or more is found.

    void InternalSplitAppendSubstring(Interp interp)
        throws TclException
    {
        String string = newlineStr;
        int slen = string.length();

        final char splitChar = '\n';

        for (int loop=0; loop < 5; loop++) {
            int i = 0;
            int elemStart = 0;

            // Create TclList that will be appended to.
            TclObject list = TclList.newInstance();

            for (; i < slen; i++) {
                if ((string.charAt(i) == splitChar) && ((i - elemStart) >= 2)) {
                    SplitCmd.appendElement(interp, list, string, elemStart, i);
                    elemStart = i+1;
                }
            }
        }
    }

} // end class TJCBench

