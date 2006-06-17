// default package

import tcl.lang.*;

public class TestTJC {
    // Test pushing a local variable frame, evaling
    // a body block, and then returning.

    public static int testEvalProcBody(Interp interp) throws TclException {
        Namespace ns = null; // Frame in global namespace
        CallFrame callFrame = TJC.pushLocalCallFrame(interp, ns);
        try {
        // Set local variable i and then return it
        String body = "set i -1 ; set j 0 ; set i";
        TJC.evalProcBody(interp, body);
        TclObject result = interp.getResult();
        int i = TclInteger.get(interp, result);
        return i;
        } catch (TclException te) {
            TJC.checkTclException(interp, te, "test");
            return -333;
        } finally {
            TJC.popLocalCallFrame(interp, callFrame);
        }
    }

    // Check that eval of a return command will set the interp
    // result and return normally.

    public static int testEvalProcBody2(Interp interp) throws TclException {
        Namespace ns = null; // Frame in global namespace
        CallFrame callFrame = TJC.pushLocalCallFrame(interp, ns);
        try {
        String body = "return 2";
        TJC.evalProcBody(interp, body);
        return -1;
        } catch (TclException te) {
            TJC.checkTclException(interp, te, "test");
            // Return would be handled above
            TclObject result = interp.getResult();
            int i = TclInteger.get(interp, result);
            return i;
        } finally {
            TJC.popLocalCallFrame(interp, callFrame);
        }
    }

    // Test assignment of proc arguments, this method will push
    // a new frame, assign locals like a proc would, and then
    // return after popping the frame.

    public static boolean testArg1(Interp interp) throws TclException {
        boolean passed = false;
        CallFrame frame = TJC.pushLocalCallFrame(interp, null);
        try {

        // proc foo { name addr } {}

        // Tcl call: [foo joe 123]
        TclObject[] objv = new TclObject[3];
        objv[0] = TclString.newInstance("foo");
        objv[1] = TclString.newInstance("joe");
        objv[2] = TclString.newInstance("123");

        if (objv.length != 3) {
            throw new TclNumArgsException(interp, 1, objv, "name");
        }

        interp.setVar("name", null, objv[1], 0);
        interp.setVar("addr", null, objv[2], 0);

        interp.eval("list $name $addr");

        if (interp.getResult().toString().equals("joe 123")) {
            passed = true;
        }

        } finally {
        TJC.popLocalCallFrame(interp, frame);
        }

        return passed;
    }

    // Test assignment of proc arguments, this method will push
    // a new frame, assign locals like a proc would, and then
    // return after popping the frame.

    public static boolean testArg2(Interp interp) throws TclException {
        boolean passed = false;
        CallFrame frame = TJC.pushLocalCallFrame(interp, null);
        try {

        // proc p { {name joe} } {}

        // Tcl call: [p]
        TclObject[] objv = new TclObject[1];
        objv[0] = TclString.newInstance("p");

        if (objv.length > 2) {
            throw new TclNumArgsException(interp, 1, objv, "?name?");
        }
        TclObject const0 = TclString.newInstance("joe");
        interp.setVar("name", null,
            ((objv.length <= 1) ? const0 : objv[1]), 0);

        interp.eval("set name");

        if (interp.getResult().toString().equals("joe")) {
            passed = true;
        }

        } finally { TJC.popLocalCallFrame(interp, frame); }

        return passed;
    }

    // Test assignment of proc arguments, this method will push
    // a new frame, assign locals like a proc would, and then
    // return after popping the frame.

    public static boolean testArg3(Interp interp) throws TclException {
        boolean passed = false;
        CallFrame frame = TJC.pushLocalCallFrame(interp, null);
        try {

        // proc p { args } {}

        // Tcl call: [p 1 2 3]
        TclObject[] objv = new TclObject[4];
        objv[0] = TclString.newInstance("p");
        objv[1] = TclString.newInstance("1");
        objv[2] = TclString.newInstance("2");
        objv[3] = TclString.newInstance("3");

        if ( 1 >= objv.length ) {
            interp.setVar("args", null, "", 0);
        } else {
            TclObject argl = TclList.newInstance();
            for (int i = 1; i < objv.length; i++) {
                TclList.append(interp, argl, objv[i]);
            }
            interp.setVar("args", null, argl, 0);
        }

        interp.eval("set args");

        if (interp.getResult().toString().equals("1 2 3")) {
            passed = true;
        }

        } finally { TJC.popLocalCallFrame(interp, frame); }

        return passed;
    }

    // Test assignment of proc arguments, this method will push
    // a new frame, assign locals like a proc would, and then
    // return after popping the frame.

    public static boolean testArg4(Interp interp) throws TclException {
        boolean passed = false;
        CallFrame frame = TJC.pushLocalCallFrame(interp, null);
        try {

        // proc p { name args } {}

        // Tcl call: [p joe]
        TclObject[] objv = new TclObject[2];
        objv[0] = TclString.newInstance("p");
        objv[1] = TclString.newInstance("joe");

        if (objv.length < 2) {
            throw new TclNumArgsException(interp, 1, objv, "name args");
        }
        interp.setVar("name", null, objv[1], 0);
        if ( 2 >= objv.length ) {
            interp.setVar("args", null, "", 0);
        } else {
            TclObject argl = TclList.newInstance();
            for (int i = 2; i < objv.length; i++) {
                TclList.append(interp, argl, objv[i]);
            }
            interp.setVar("args", null, argl, 0);
        }

        interp.eval("list $name $args");

        if (interp.getResult().toString().equals("joe {}")) {
            passed = true;
        }

        } finally { TJC.popLocalCallFrame(interp, frame); }

        return passed;
    }

    // Test assignment of proc arguments, this method will push
    // a new frame, assign locals like a proc would, and then
    // return after popping the frame.

    public static boolean testArg5(Interp interp) throws TclException {
        boolean passed = false;
        CallFrame frame = TJC.pushLocalCallFrame(interp, null);
        try {

        // proc p { {name joe} {addr 123} args } {}

        // Tcl call: [p bob]
        TclObject[] objv = new TclObject[2];
        objv[0] = TclString.newInstance("p");
        objv[1] = TclString.newInstance("bob");

        interp.setVar("name", null, objv[1], 0);
        TclObject const0 = TclString.newInstance("123");
        interp.setVar("addr", null,
            ((objv.length <= 2) ? const0 : objv[2]), 0);
        if ( 3 >= objv.length ) {
            interp.setVar("args", null, "", 0);
        } else {
            TclObject argl = TclList.newInstance();
            for (int i = 3; i < objv.length; i++) {
                TclList.append(interp, argl, objv[i]);
            }
            interp.setVar("args", null, argl, 0);
        }

        interp.eval("list $name $addr $args");

        if (interp.getResult().toString().equals("bob 123 {}")) {
            passed = true;
        }

        } finally { TJC.popLocalCallFrame(interp, frame); }

        return passed;
    }

    // Test creation of a TJC.CompiledCommand derived class.

    public static boolean testCreateCompiledCommand1(Interp interp) throws TclException {
        class TCC1 extends TJC.CompiledCommand {
            public void cmdProc(Interp interp, TclObject[] objv) {
                interp.setResult("OK");
            }
        }

        String full_cmdname = "::tcc_test";
        String short_cmdname = "tcc_test";
        TJC.createCommand(interp, full_cmdname, new TCC1());
        interp.eval(short_cmdname);
        String result = interp.getResult().toString();
        return result.equals("OK");
    }

    public static boolean testCreateCompiledCommand2(Interp interp) throws TclException {
        class TCC2 extends TJC.CompiledCommand {
            public void cmdProc(Interp interp, TclObject[] objv) {
                interp.setResult("OK");
            }
        }

        interp.eval("namespace eval ::one::two {}");

        String full_cmdname = "::one::two::tcc_test";
        String short_cmdname = "one::two::tcc_test";
        TJC.createCommand(interp, full_cmdname, new TCC2());
        interp.eval(short_cmdname);
        String result = interp.getResult().toString();
        interp.eval("namespace delete one");
        return result.equals("OK");
    }

    public static boolean testCreateCompiledCommand3(Interp interp) throws TclException {
        class TCC3 extends TJC.CompiledCommand {
            public void cmdProc(Interp interp, TclObject[] objv) {
                interp.setResult("OK");
            }
        }

        interp.eval("namespace eval ::one::two {}");

        String full_cmdname = "::one::two::tcc_test";
        String short_cmdname = "one::two::tcc_test";
        TCC3 cmd = new TCC3();
        TJC.createCommand(interp, full_cmdname, cmd);
        // Check that command namespace is correct
        return cmd.wcmd.ns.fullName.equals("::one::two");
    }

    // Test TJC.grabObjv() and TJC.invoke()

    public static boolean testInvoke1(Interp interp) throws TclException {
        boolean passed = false;

        TclObject[] objv = TJC.grabObjv(interp, 3);
        objv[0] = TclString.newInstance("set");
        objv[1] = TclString.newInstance("i");
        objv[2] = TclString.newInstance("-1");
        objv[0].preserve();
        objv[1].preserve();
        objv[2].preserve();
        TJC.invoke(interp, null, objv, 0);
        TJC.releaseObjvElems(interp, objv, objv.length);

        if (interp.getResult().toString().equals("-1")) {
            passed = true;
        }

        return passed;
    }

    // Test TJC.grabObjv() and TJC.invoke() at global scope

    public static boolean testInvoke2(Interp interp) throws TclException {
        boolean passed = false;

        TclObject[] objv = TJC.grabObjv(interp, 3);
        objv[0] = TclString.newInstance("set");
        objv[1] = TclString.newInstance("i");
        objv[2] = TclString.newInstance("-1");
        objv[0].preserve();
        objv[1].preserve();
        objv[2].preserve();
        TJC.invoke(interp, null, objv, TCL.EVAL_GLOBAL);
        TJC.releaseObjvElems(interp, objv, objv.length);

        if (interp.getResult().toString().equals("-1")) {
            passed = true;
        }

        return passed;
    }

    // Test invocation of unknown command at global scope when a
    // command is not defined.

    public static void testInvoke3(Interp interp) throws TclException {
        TclObject[] objv = TJC.grabObjv(interp, 3);
        objv[0] = TclString.newInstance("foobar");
        objv[1] = TclString.newInstance("1");
        objv[2] = TclString.newInstance("2");
        objv[0].preserve();
        objv[1].preserve();
        objv[2].preserve();
        TJC.invoke(interp, null, objv, TCL.EVAL_GLOBAL);
        TJC.releaseObjvElems(interp, objv, objv.length);
        return;
    }

    // Test invocation where the Command ref has already been resolved.

    public static void testInvoke4(Interp interp) throws TclException {
        TclObject[] objv = TJC.grabObjv(interp, 2);
        objv[0] = TclString.newInstance("not_ziggy"); // Would fail to resolve
        objv[1] = TclString.newInstance("11");
        objv[0].preserve();
        objv[1].preserve();
        Command cmd = interp.getCommand("ziggy");
        TJC.invoke(interp, cmd, objv, TCL.EVAL_GLOBAL);
        TJC.releaseObjvElems(interp, objv, objv.length);
        return;
    }

    // Test invocation that raises an error. This should raise a TclException
    // and set a global named err to the error message.

    public static int testInvoke5(Interp interp) throws TclException {
        TclObject[] objv = TJC.grabObjv(interp, 2);
        objv[0] = TclString.newInstance("raise_error");
        objv[1] = TclString.newInstance("2");
        objv[0].preserve();
        objv[1].preserve();
        try {
            TJC.invoke(interp, null, objv, TCL.EVAL_GLOBAL);
        } catch (TclException ex) {
            interp.setVar("err", interp.getResult(), 0);
            return 1;
        } finally {
            TJC.releaseObjvElems(interp, objv, objv.length);
        }
        return 0;
    }

    public static String testSwitchInvoke1(Interp interp) throws TclException {
        TclObject[] objv = TJC.grabObjv(interp, 4);
        objv[0] = TclString.newInstance("switch"); // Name of command (unused)
        objv[1] = TclString.newInstance("Foo");    // Value of string (unused)
        objv[2] = TclString.newInstance("Foo");    // First pattern
        objv[3] = null;                            // First body
        objv[0].preserve();
        objv[1].preserve();
        objv[2].preserve();
        int bOff = TJC.invokeSwitch(interp, objv, 2, "Foo", TJC.SWITCH_MODE_EXACT);
        TJC.releaseObjvElems(interp, objv, objv.length);
        if ( bOff == 1 ) {
            return "pass";
        } else {
            return "fail " + String.valueOf(bOff);
        }
    }

    public static String testSwitchInvoke2(Interp interp) throws TclException {
        TclObject[] objv = TJC.grabObjv(interp, 4);
        objv[0] = TclString.newInstance("Boo");    // First pattern
        objv[1] = null;                            // First body
        objv[2] = TclString.newInstance("Foo");    // Second pattern
        objv[3] = null;                            // Second body
        objv[0].preserve();
        objv[2].preserve();
        int bOff = TJC.invokeSwitch(interp, objv, 0, "Foo", TJC.SWITCH_MODE_EXACT);
        objv[0].release();
        objv[2].release();
        TJC.releaseObjv(interp, objv, objv.length);
        if ( bOff == 3 ) {
            return "pass";
        } else {
            return "fail " + String.valueOf(bOff);
        }
    }

    // Test evaluation of a expr operator, it should operate
    // on the given argument(s).

    public static int testExprOp1(Interp interp) throws TclException {
        ExprValue value = TJC.exprGetValue(interp, 1, null);
        TJC.exprUnaryOperator(interp, TJC.EXPR_OP_UNARY_MINUS, value);
        int result = value.getIntValue();
        TJC.exprReleaseValue(interp, value);
        return result;
    }

    public static int testExprOp2(Interp interp) throws TclException {
        TclObject tobj = TclString.newInstance("1234");
        ExprValue value = TJC.exprGetValue(interp, tobj);
        TJC.exprUnaryOperator(interp, TJC.EXPR_OP_UNARY_MINUS, value);
        int result = value.getIntValue();
        TJC.exprReleaseValue(interp, value);
        return result;
    }

    public static int testExprOp3(Interp interp) throws TclException {
        TclObject tobj1 = TclString.newInstance("1234");
        TclObject tobj2 = TclString.newInstance("234");

        ExprValue value = TJC.exprGetValue(interp, tobj1);
        ExprValue value2 = TJC.exprGetValue(interp, tobj2);

        TJC.exprBinaryOperator(interp, TJC.EXPR_OP_MINUS, value, value2);
        TJC.exprReleaseValue(interp, value2);
        int result = value.getIntValue();
        TJC.exprReleaseValue(interp, value);
        return result;
    }

    public static double testExprOp4(Interp interp) throws TclException {
        // expr {(1000 / 10) + 1.0}
        ExprValue value = TJC.exprGetValue(interp, 1000, null);
        ExprValue value2 = TJC.exprGetValue(interp, 10, null);

        TJC.exprBinaryOperator(interp, TJC.EXPR_OP_DIVIDE, value, value2);
        TJC.exprReleaseValue(interp, value2);

        value2 = TJC.exprGetValue(interp, 1.0, null);

        TJC.exprBinaryOperator(interp, TJC.EXPR_OP_PLUS, value, value2);
        TJC.exprReleaseValue(interp, value2);

        double result = value.getDoubleValue();

        TJC.exprReleaseValue(interp, value);

        return result;
    }

    public static int testExprOp5(Interp interp) throws TclException {
        // expr {" 10 " eq 10}
        ExprValue value = TJC.exprGetValue(interp, 10, " 10 ");
        ExprValue value2 = TJC.exprGetValue(interp, 10, null);

        TJC.exprBinaryOperator(interp, TJC.EXPR_OP_STREQ, value, value2);
        TJC.exprReleaseValue(interp, value2);

        int result = value.getIntValue();

        TJC.exprReleaseValue(interp, value);

        return result;
    }

    // This method shows how a compiled expr command would
    // set the interp result to the value of a evaluated expr.

    public static String testExprOp6(Interp interp) throws TclException {
        // expr {" 10 " == 10}
        ExprValue value = TJC.exprGetValue(interp, 10, " 10 ");
        ExprValue value2 = TJC.exprGetValue(interp, 10, null);

        TJC.exprBinaryOperator(interp, TJC.EXPR_OP_EQUAL, value, value2);
        TJC.exprReleaseValue(interp, value2);

        TJC.exprSetResult(interp, value);
        TJC.exprReleaseValue(interp, value);

        TclObject result = interp.getResult();

        return interp.getResult().toString();
    }

    // This method shows how a compiled container with an expr
    // blocks would evaluate the block as a boolean value.

    public static boolean testExprOp7(Interp interp) throws TclException {
        // expr {" 10.0 " == 10}
        ExprValue value = TJC.exprGetValue(interp, 10.0, " 10.0 ");
        ExprValue value2 = TJC.exprGetValue(interp, 10, null);

        TJC.exprBinaryOperator(interp, TJC.EXPR_OP_EQUAL, value, value2);
        TJC.exprReleaseValue(interp, value2);

        return value.getBooleanValue(interp);
    }

    // Compares two string values

    public static boolean testExprOp8(Interp interp) throws TclException {
        // expr {"one" == "two"}
        ExprValue value = TJC.exprGetValue(interp, "one");
        ExprValue value2 = TJC.exprGetValue(interp, "two");

        TJC.exprBinaryOperator(interp, TJC.EXPR_OP_EQUAL, value, value2);
        TJC.exprReleaseValue(interp, value2);

        return value.getBooleanValue(interp);
    }

    // Invoke an expr math function

    public static String testExprOp9(Interp interp) throws TclException {
        // expr {pow(2,2)}
        ExprValue[] values = new ExprValue[2];
        values[0] = TJC.exprGetValue(interp, 2, null);
        values[1] = TJC.exprGetValue(interp, 2, null);
        ExprValue result = TJC.exprGetValue(interp, 2, null);
        TJC.exprMathFunction(interp, "pow", values, result);
        TJC.exprReleaseValue(interp, values[1]);
        TJC.exprReleaseValue(interp, values[0]);

        TJC.exprSetResult(interp, result);
        TJC.exprReleaseValue(interp, result);

        return interp.getResult().toString();
    }

    // Invoke expr empty string compare function.

    public static String testExprOp10(Interp interp) throws TclException {
        // expr {$obj == ""}
        TclObject obj = TclString.newInstance("");
        ExprValue result = new ExprValue(0, null);
        TJC.exprEqualsEmptyString(result, obj, false);

        TJC.exprSetResult(interp, result);
        TJC.exprReleaseValue(interp, result);

        return interp.getResult().toString();
    }

    // Invoke expr empty string compare function.

    public static String testExprOp11(Interp interp) throws TclException {
        // expr {$obj != {}}
        TclObject obj = TclList.newInstance();
        ExprValue result = new ExprValue(0, null);
        TJC.exprEqualsEmptyString(result, obj, true);

        TJC.exprSetResult(interp, result);
        TJC.exprReleaseValue(interp, result);

        return interp.getResult().toString();
    }

    // Invoke int() math function

    public static String testExprOp12(Interp interp) throws TclException {
        // expr {int(1.0)}
        ExprValue value = TJC.exprGetValue(interp, 1.0, null);
        TJC.exprIntMathFunction(interp, value);
        TJC.exprSetResult(interp, value);
        TJC.exprReleaseValue(interp, value);
        return interp.getResult().toString();
    }

    // Invoke double() math function

    public static String testExprOp13(Interp interp) throws TclException {
        // expr {double(1)}
        ExprValue value = TJC.exprGetValue(interp, 1, null);
        TJC.exprDoubleMathFunction(interp, value);
        TJC.exprSetResult(interp, value);
        TJC.exprReleaseValue(interp, value);
        return interp.getResult().toString();
    }

    // Invoke an expr math function, this tests
    // a special case in the impl of exprMathFunction()
    // that will keep the result in values[0]
    // when null is passed as the result argument.

    public static String testExprOp14(Interp interp) throws TclException {
        // expr {pow(2,2)}
        ExprValue[] values = new ExprValue[2];
        values[0] = TJC.exprGetValue(interp, 2, null);
        values[1] = TJC.exprGetValue(interp, 2, null);

        TJC.exprMathFunction(interp, "pow", values, null);
        TJC.exprReleaseValue(interp, values[1]);
        // Don't release values[0] yet, since it is the result.
        ExprValue result = values[0];

        TJC.exprSetResult(interp, result);
        TJC.exprReleaseValue(interp, result);

        return interp.getResult().toString();
    }


    // Check TclObject ref count issues

    public static String testRefCount1(Interp interp) throws TclException {
        TclObject tstr = TclString.newInstance("hello");
        tstr.preserve(); // incr ref count to 1
        tstr.release(); // decr ref count to 0, deallocate

        String err = "NONE";
        try {
            tstr.toString();
        } catch (TclRuntimeError tre) {
            // error raised because object was deallocated
            err = tre.getMessage();
        }
        return err;
    }

    public static String testRefCount2(Interp interp) throws TclException {
        TclObject tstr = TclString.newInstance("hello");
        tstr.release(); // decr with ref count of 0, deallocate

        String err = "NONE";
        try {
            tstr.toString();
        } catch (TclRuntimeError tre) {
            // error raised because object was deallocated
            err = tre.getMessage();
        }
        return err;
    }

    public static String testRefCount3(Interp interp) throws TclException {
        TclObject tstr = TclString.newInstance("hello");
        // Set interp result to object, this increments the ref
        // count to 1.
        interp.setResult(tstr);
        // Reset result, drop ref count and deallocate object.
        interp.resetResult();

        String err = "NONE";
        try {
            tstr.toString();
        } catch (TclRuntimeError tre) {
            // error raise because object was deallocated
            err = tre.getMessage();
        }
        return err;
    }

    public static String testRefCount4(Interp interp) throws TclException {
        TclObject tstr = TclString.newInstance("hello");
        // increment the ref count before setting the interp result.
        tstr.preserve();
        // increment ref count.
        interp.setResult(tstr);
        // decrement ref count, does not deallocate.
        interp.resetResult();
        // Should not fail
        tstr.toString();
        // Now drop the ref count
        tstr.release();

        String err = "NONE";
        try {
            tstr.toString();
        } catch (TclRuntimeError tre) {
            // error raised because object was deallocated
            err = tre.getMessage();
        }
        return err;
    }

    public static String testRefCount5(Interp interp) throws TclException {
        TclObject tstr = TclString.newInstance("hello");
        // Set variable to the object, this will increment the ref count.
        interp.setVar("one", null, tstr, 0);
        // Set variable to a new boolean value, this will decrement the ref
        // count and deallocate the object.
        interp.setVar("one", null, "newvalue", 0);

        String err = "NONE";
        try {
            tstr.toString();
        } catch (TclRuntimeError tre) {
            // error raised because object was deallocated
            err = tre.getMessage();
        }
        return err;
    }

    public static String testRefCount6(Interp interp) throws TclException {
        TclObject tstr = TclString.newInstance("hello");
        // Set variable to the object, this will increment the ref count 1.
        interp.setVar("one", null, tstr, 0);
        // Setting the same variable to the same value is ok since
        // the new ref is incremented before the old ref is decremented.
        // Note that this would not work if APPEND_VALUE or LIST_ELEMENT
        // since that logic seems broken in this respect.
        interp.setVar("one", null, tstr, 0);
        // Now reset the variable to a new value to deallocate the object.
        interp.setVar("one", null, "newvalue", 0);

        String err = "NONE";
        try {
            tstr.toString();
        } catch (TclRuntimeError tre) {
            // error raised because object was deallocated
            err = tre.getMessage();
        }
        return err;
    }

    public static String testRefCount7(Interp interp) throws TclException {
        TclObject tstr = TclString.newInstance("hello");
        // Set interp result to object, this will increment the ref count to 1.
        interp.setResult(tstr);
        // Set a variable to the object in the interp result, this will
        // increment the ref count to 2.
        interp.setVar("one", null, interp.getResult(), 0);
        // Now reset the interp result, this will decrement the ref count to 1.
        interp.resetResult();
        // Now reset the variable, this will decrement the ref count to 0
        // and deallocate the object.
        interp.setVar("one", null, "newvalue", 0);

        String err = "NONE";
        try {
            tstr.toString();
        } catch (TclRuntimeError tre) {
            // error raised because object was deallocated
            err = tre.getMessage();
        }
        return err;
    }

    public static String testRefCount8(Interp interp) throws TclException {
        TclObject tstr = TclString.newInstance("hello");
        // Hold two refs to the object so we are sure it is shared with ref count of 2.
        tstr.preserve();
        tstr.preserve();
        // Set variable to the object, this will increment the ref count to 3.
        interp.setVar("one", null, tstr, 0);
        // Now append to a copy and decrement the ref count by one.
        interp.setVar("one", null, tstr, TCL.APPEND_VALUE);
        tstr.release();
        tstr.release();

        String err = "NONE";
        try {
            tstr.toString();
        } catch (TclRuntimeError tre) {
            // error raised because object was deallocated
            err = tre.getMessage();
        }
        return err;
    }

    public static String testRefCount9(Interp interp) throws TclException {
        TclObject tstr = TclString.newInstance("hello");
        // Hold two refs to the object so we are sure it is shared with ref count of 2.
        tstr.preserve();
        tstr.preserve();
        // Set variable to the object, this will increment the ref count to 3.
        interp.setVar("one", null, tstr, 0);
        // Append to duplicate of object and decrement original ref count by one.
        interp.setVar("one", null, tstr, TCL.APPEND_VALUE | TCL.LIST_ELEMENT);
        tstr.release();
        tstr.release();
        // Release last ref to object by unsetting
        interp.unsetVar("one", 0);

        String err = "NONE";
        try {
            tstr.toString();
        } catch (TclRuntimeError tre) {
            // error raised because object was deallocated
            err = tre.getMessage();
        }
        return err;
    }

    public static String testRefCount10(Interp interp) throws TclException {
        TclObject tstr = TclString.newInstance("1");
        // Set variable to the object, this will increment the ref count to 1.
        interp.setVar("one", null, tstr, 0);

        String err = "NONE";
        try {
            // Set the same variable to the same object with the
            // special TCL.LIST_ELEMENT flag set. This will cause
            // an error because the logic in Var.setVar() will
            // decrement the ref count of the old object before
            // getting the string rep of the new object, so the
            // toString() will fail on a deallocated object.
            interp.setVar("one", null, tstr, TCL.LIST_ELEMENT);
            // This is clearly a bug, but it seems that setVar()
            // could be updated to the Tcl 8.4 implementation
            // since it seems to be different in this respect.
        } catch (TclRuntimeError tre) {
            // error raised because object was deallocated
            err = tre.getMessage();
        }
        return err;
    }

}

