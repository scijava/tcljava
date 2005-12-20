/*
 * Copyright (c) 2005 Advanced Micro Devices, Inc.
 *
 * See the file "license.amd" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TJC.java,v 1.1 2005/12/20 23:00:11 mdejong Exp $
 *
 */

// Runtime support for TJC compiler implementation.

package tcl.lang;

import java.util.Hashtable;
import java.util.Arrays;

public class TJC {

    // Constants

    public static final int SWITCH_MODE_EXACT = 0;
    public static final int SWITCH_MODE_GLOB = 1;
    public static final int SWITCH_MODE_REGEXP = 2;

    public static final int EXPR_OP_MULT = Expression.MULT;
    public static final int EXPR_OP_DIVIDE = Expression.DIVIDE;
    public static final int EXPR_OP_MOD = Expression.MOD;
    public static final int EXPR_OP_PLUS = Expression.PLUS;
    public static final int EXPR_OP_MINUS = Expression.MINUS;
    public static final int EXPR_OP_LEFT_SHIFT = Expression.LEFT_SHIFT;
    public static final int EXPR_OP_RIGHT_SHIFT = Expression.RIGHT_SHIFT;
    public static final int EXPR_OP_LESS = Expression.LESS;
    public static final int EXPR_OP_GREATER = Expression.GREATER;
    public static final int EXPR_OP_LEQ = Expression.LEQ;
    public static final int EXPR_OP_GEQ = Expression.GEQ;
    public static final int EXPR_OP_EQUAL = Expression.EQUAL;
    public static final int EXPR_OP_NEQ = Expression.NEQ;
    public static final int EXPR_OP_BIT_AND = Expression.BIT_AND;
    public static final int EXPR_OP_BIT_XOR = Expression.BIT_XOR;
    public static final int EXPR_OP_BIT_OR = Expression.BIT_OR;
    public static final int EXPR_OP_STREQ = Expression.STREQ;
    public static final int EXPR_OP_STRNEQ = Expression.STRNEQ;

    public static final int EXPR_OP_UNARY_MINUS = Expression.UNARY_MINUS;
    public static final int EXPR_OP_UNARY_PLUS = Expression.UNARY_PLUS;
    public static final int EXPR_OP_UNARY_NOT = Expression.NOT;
    public static final int EXPR_OP_UNARY_BIT_NOT = Expression.BIT_NOT;

    public static final WrappedCommand INVALID_COMMAND_CACHE;

    static {
        // Setup a fake command wrapper. The only
        // point of this is to have a wrapper with
        // and invalid cmdEpoch that will never be
        // equal to a valid cmdEpoch for a command.
        INVALID_COMMAND_CACHE = new WrappedCommand();
        INVALID_COMMAND_CACHE.deleted = true;
        INVALID_COMMAND_CACHE.cmdEpoch = -1;
    }

    // Invoked to create and push a new CallFrame for local
    // variables when a command begins to execute. This
    // method does not assign method arguments to local
    // variables inside the CallFrame. The logic here is
    // copied from CallFrame.chain().

    public static CallFrame pushLocalCallFrame(Interp interp,
            Namespace ns) {
        CallFrame frame = interp.newCallFrame();

        // Namespace the command is defined in, default to global namespace

        if (ns != null) {
            frame.ns = ns;
        }

        // ignore objv

        // isProcCallFrame should be true
        if (frame.isProcCallFrame == false) {
            throw new TclRuntimeError("expected isProcCallFrame to be true");
        }

	frame.level           = (interp.varFrame == null) ? 1 : (interp.varFrame.level + 1);
	frame.caller          = interp.frame;
	frame.callerVar       = interp.varFrame;
	interp.frame          = frame;
	interp.varFrame       = frame;

        return frame;
    }

    // Invoked to pop a local CallFrame when a command is finished.

    public static void popLocalCallFrame(Interp interp, CallFrame frame) {
        // Cleanup code copied from Procedure.java. See the cmdProc
        // implementation in that class for more info.

        if (interp.errInProgress) {
            frame.dispose();
            interp.errInProgress = true;
        } else {
            frame.dispose();
        }
    }

    // Evaluate a Tcl string that is the body of a Tcl procedure.

    public static void evalProcBody(Interp interp, String body)
        throws TclException
    {
        interp.eval(body);
    }

    // Check a TclException raised while a compiled Tcl procedure
    // is executing. This method should be invoked from a catch
    // block around the body code for a proc. This method could
    // handle the exception and stop if from propagating in the
    // case of a return, or it could allow it to propogate for
    // a normal error case. The logic in this method is copied
    // from Procedure.cmdProc().

    public static void checkTclException(
            Interp interp,
            TclException e,
            String procName)
        throws TclException
    {
        // FIXME: Ugh this is nasty. How can TCL.OK be package private if this
        // kind of mess could appear in the result from any eval()? All of this
        // should be in an interp method to handle these cases.

        int code = e.getCompletionCode();
        if (code == TCL.RETURN) {
            int realCode = interp.updateReturnInfo();
	    if (realCode != TCL.OK) {
                e.setCompletionCode(realCode);
	        throw e;
	    }
	} else if (code == TCL.ERROR) {
	    interp.addErrorInfo(
		    "\n    (procedure \"" + procName + "\" line 1)");
	    throw e;
	} else if (code == TCL.BREAK) {
	    throw new TclException(interp,
		    "invoked \"break\" outside of a loop");
	} else if (code == TCL.CONTINUE) {
	    throw new TclException(interp,
		    "invoked \"continue\" outside of a loop");
	} else {
	    throw e;
	}
    }

    // Base class for TJC compiled commands

    public static abstract class CompiledCommand implements Command {
        public WrappedCommand wcmd = null;

        // A CompiledCommand implementation must define cmdProc

        // A CompiledCommand implementation may want to init
        // instance data members when first invoked. This
        // flag and method are used to implement this init
        // check.

        protected boolean initCmd = false;

        protected void initCmd(Interp interp) throws TclException {
            if (initCmd) {
                throw new TclRuntimeError("initCmd already invoked");
            }
            builtinCommandsCheck(interp);
            initConstants(interp);
            initCmd = true;
        }

        // The following method should be defined in a specific
        // command implementations to init constant instance data.

        protected void initConstants(Interp interp) throws TclException {}

        // Verify that this compiled command is not being used
        // in a namespace that contains commands with the same
        // name Tcl commands that could have been inlined.
        // Since this check is only done whan the command is
        // first invoked, it should not a performance concern.

        protected void builtinCommandsCheck(Interp interp)
            throws TclException
        {
            if (wcmd.ns.fullName.compareTo("::") == 0) {
                return; // loaded into global namespace
            }
            String[] builtin = {
                "break",
                "catch",
                "continue",
                "expr",
                "for",
                "foreach",
                "if",
                "return",
                "switch",
                "while"
            };
            WrappedCommand cmd;
            String cmdName;
            for (int i=0; i < builtin.length; i++) {
                cmdName = builtin[i];
                cmd = Namespace.findCommand(interp, cmdName,
                    wcmd.ns, TCL.NAMESPACE_ONLY);
                if (cmd != null) {
                    throw new TclException(interp, "TJC compiled command" +
                        " can't be loaded into the namespace " +
                        wcmd.ns.fullName +
                        " as it defines the builtin Tcl command \"" +
                        cmdName + "\" (" + cmd.toString() + ")");
                }
            }
        }
    }

    // Used to create a TJC compiled command. This method will
    // use the fully qualified command name passed in to
    // create the command in the correct namespace.

    public static void createCommand(
        Interp interp,            // Interp to create command in
        String cmdName,           // Fully qualified or short name of command
        TJC.CompiledCommand cmd)  // Instance for new compiled command
            throws TclException
    {
        ProcCmd.FindCommandNamespaceResult result =
            ProcCmd.FindCommandNamespace(interp, cmdName);

        interp.createCommand(result.cmdFullName, cmd);

        cmd.wcmd = Namespace.findCommand(interp,
            result.cmdName, result.ns, TCL.NAMESPACE_ONLY);
    }

    // Used to load an init file from the package JAR file.
    // This command should be invoked from a TJCExtension
    // to source the init Tcl script in the JAR. It is
    // possible that the TJC package is not yet loaded
    // if java::load was used to load the Extension. Just
    // require TJC package to take care of this case.

    public static void sourceInitFile(
        Interp interp,
        String init_file,
        String[] files,
        String prefix)
            throws TclException
    {
        // Install temp source command
        interp.eval("package require TJC");
        interp.eval("rename ::source ::TJC::source");
        interp.createCommand("::source",
            new InitSourceCmd(init_file, files, prefix));

        //System.out.println("sourceInitFile: will source \"" + (prefix + init_file) + "\"");
        TclException tex = null;
        try {
            interp.evalResource(prefix + init_file);
        } catch (TclException ex) {
            tex = ex;
        } finally {
            // Remove temp source command
            interp.eval("rename ::source {}");
            interp.eval("rename ::TJC::source ::source");

            if (tex != null) {
                interp.setResult(tex.getMessage());
                throw tex;
            }
        }
    }

    // This class implements a fake "source" command that is
    // only active while a package init file is being sourced
    // inside sourceInitFile().

    static class InitSourceCmd implements Command {
        String init_file;
        String[] files;
        String prefix;
        Hashtable filesTable;

        InitSourceCmd(String init_file, String[] files, String prefix)
        {
            this.init_file = init_file;
            this.files = files;
            this.prefix = prefix;

            filesTable = new Hashtable();
            for (int i=0; i < files.length; i++) {
                filesTable.put(files[i], "");
            }
        }

        public void cmdProc(
            Interp interp,
            TclObject[] objv)
                throws TclException
        {
            boolean handled = false;

            // Expected syntax "source filename"

            if (objv.length == 2) {
                String filepath = objv[1].toString();

                interp.eval("file tail {" + filepath + "}");

                String filename = interp.getResult().toString();

                if (filesTable.containsKey(filename)) {
                    // Sourced a file in files array, this
                    // file should be read from a resource.
                    handled = true;

                    //System.out.println("sourceInitFile.files: will source \"" + (prefix + filename) + "\"");
                    interp.evalResource(prefix + filename);
                }
            }

            if (!handled) {
                // Invoke TJC::source, this is the original source command.
                // The original command will deal with error reporting
                // as well as invocations that source a file not in "files".
                Command cmd = interp.getCommand("TJC::source");
                cmd.cmdProc(interp, objv);
            }
        }
    }

    // Resolve a command name into a WrappedCommand reference
    // that is cached. Note that this method is only ever
    // invoked after a command has been successfully invoked,
    // so there should be no need to worry about loading the
    // command or dealing with stub commands. If the command
    // can't be located, null is returned. This command is
    // always invoked after the CallFrame for a compiled
    // command has been pushed, so it is safe to assume
    // that the current namespace is the namespace the
    // command is defined in.

    public static
    WrappedCommand resolveCmd(
        Interp interp,
        String cmdName)
	    throws TclException
    {
        return Namespace.findCommand(interp, cmdName, null, 0);
    }

    // Get a TclObject[] of the given size. This array will
    // be allocated quickly from a cache if it is a common size.
    // The array must be released by releaseObjv(). Each element
    // in the returned array will be null.

    public static TclObject[] grabObjv(
        Interp interp,
        int size)
    {
        TclObject[] objv = Parser.grabObjv(interp, size);
        Arrays.fill(objv, null);
        return objv;
    }

    // Release each object in the array and return the array
    // to a cache of common values. The array must have been
    // allocated with grabObjv().

    public static void releaseObjv(
        Interp interp,
        TclObject[] objv)
    {
        final int len = objv.length;
        for (int i=0; i < len; i++) {
            TclObject tobj = objv[i];
            if (tobj != null) {
                tobj.release();
                objv[i] = null;
            }
        }
        Parser.releaseObjv(interp, objv);
    }

    // Invoke a Command with TclObject arguments. This method
    // will invoke an already resolved Command passed in "cmd" or
    // it will resolve objv[0] into a command. This method
    // assumes that the ref count of each element in the
    // objv was already incremented by the caller. This method
    // does not modify or cleanup the passed in objv array.

    public static void invoke(
        Interp interp,            // Interp to invoke command in
        Command cmd,              // Resolved command ref, null to lookup objv[0]
        TclObject[] objv,         // TclObject arguments to command
        int flags)                // Either 0 or TCL.EVAL_GLOBAL. If 0 is passed
                                  // then objv[0] is resolved in the current
                                  // namespace and then the global one. If
                                  // TCL.EVAL_GLOBAL is passed then the command
                                  // is resolved and evaluated in the global namespace.
                                  // If the cmd argument is non-null, then this
                                  // flag only controls evaluation scope.
            throws TclException
    {
        boolean grabbed_objv = false;

        if (objv == null) {
            throw new TclRuntimeError("null objv array");
        }
        int len = objv.length;
        if (len == 0) {
            throw new TclRuntimeError("zero length objv array");
        }

        String cmdName = objv[0].toString();
        // Save copy of interp.varFrame in case TCL.EVAL_GLOBAL is set.
        CallFrame savedVarFrame = interp.varFrame;

        // If cmd is null, then resolve objv[0] into a Command.

        if (cmd == null) {
            WrappedCommand wcmd;
            int fflags = 0;
            if ((flags & TCL.EVAL_GLOBAL) != 0) {
                fflags |= TCL.GLOBAL_ONLY;
            }
            // Find the procedure to execute this command. If there isn't one,
            // then see if there is a command "unknown".  If so, create a new
            // word array with "unknown" as the first word and the original
            // command words as arguments.
            wcmd = Namespace.findCommand(interp, cmdName, null, fflags);
            if (wcmd != null) {
                cmd = wcmd.cmd;
            }
            if (cmd == null) {
                wcmd = Namespace.findCommand(interp, "unknown",
                    null, TCL.GLOBAL_ONLY);
                if (wcmd != null) {
                    cmd = wcmd.cmd;
                }
                if (cmd == null) {
                    throw new TclException(interp, "invalid command name \""
                        + cmdName + "\"");
                }
                TclObject[] newObjv = Parser.grabObjv(interp, len + 1);
                newObjv[0] = TclString.newInstance("unknown");
                newObjv[0].preserve();
                for (int i = (len - 1); i >= 0; i--) {
                    newObjv[i+1] = objv[i];
                }
                objv = newObjv;
                grabbed_objv = true;
                len = objv.length;
            }
        }

        try {
            interp.preserve();
            interp.resetResult();
            interp.allowExceptions();

            // If the interpreter was deleted, return an error.

            if (interp.deleted){
                interp.setResult("attempt to call eval in deleted interpreter");
                interp.setErrorCode(TclString.newInstance(
                    "CORE IDELETE {attempt to call eval in deleted interpreter}"));
                throw new TclException(TCL.ERROR);
            }

            // Check depth of nested calls to eval:  if this gets too large,
            // it's probably because of an infinite loop somewhere.

            if (interp.nestLevel >= interp.maxNestingDepth) {
                Parser.infiniteLoopException(interp);
            }
            interp.nestLevel++;
            interp.cmdCount++;

            // Invoke cmdProc for the command.

            if ((flags & TCL.EVAL_GLOBAL) != 0) {
                interp.varFrame = null;
            }

            cmd.cmdProc(interp, objv);
        } catch (TclException ex) {
            // Generate error info that includes the arguments
            // to the command and add these to the errorInfo var.

            if ( ex.getCompletionCode() == TCL.ERROR &&
                    !(interp.errAlreadyLogged)) {
                StringBuffer cmd_strbuf = new StringBuffer(64);
                
                for (int i=0; i < len; i++) {
                    Util.appendElement(interp, cmd_strbuf, objv[i].toString());
                }
                String cmd_str = cmd_strbuf.toString();
                char[] script_array = cmd_str.toCharArray();
                int script_index = 0;
                int command_start = 0;
                int command_length = cmd_str.length();
                Parser.logCommandInfo(interp, script_array, script_index,
                    command_start, command_length, ex);
            }

            throw ex;
        } finally {
            if (grabbed_objv) {
                objv[0].release();
                Arrays.fill(objv, null);
                Parser.releaseObjv(interp, objv);
            }
            interp.nestLevel--;
            interp.varFrame = savedVarFrame;
            interp.release();
        }
    }

    // Most efficient way to query a TclObject
    // to determine its boolean value. This method
    // will not change the internal rep of the obj.

    public static boolean getBoolean(
        Interp interp,
        TclObject obj)
            throws TclException
    {
        TclObject orig_obj = obj;
        InternalRep rep = obj.getInternalRep();

        if (obj.hasNoStringRep() && (rep instanceof TclList)) {
            if (TclList.getLength(interp, obj) == 1) {
                // If a pure list is of length one, then use
                // the list element in the typed tests below.
                obj = TclList.index(interp, obj, 0);
                rep = obj.getInternalRep();
            }
        }

        if (rep instanceof TclBoolean) {
            return TclBoolean.get(interp, obj);
        } else if (rep instanceof TclInteger) {
            return (TclInteger.get(interp, obj) != 0);
        } else if (rep instanceof TclDouble) {
            return (TclDouble.get(interp, obj) != 0.0);
        } else {
            // Attempt to convert the String to a boolean
            if (obj != orig_obj) {
                obj = orig_obj;
            }
            return Util.getBoolean(interp, obj.toString());
        }
    }

    // This method will invoke logic for the switch
    // command at runtime. If no body is matched,
    // then -1 will be returned. Otherwise, the
    // offset from the first pattern is returned.
    // The caller must take care to release the
    // pbObjv array after invoking this method.

    public static int invokeSwitch(
        Interp interp,
        TclObject[] pbObjv,
        int pbStart,         // Index > 0 of first pattern
        String string,       // String to be matched against patterns
        int mode)            // Either TJC.SWITCH_MODE_EXACT
                             // or     TJC.SWITCH_MODE_GLOB
                             // or     TJC.SWITCH_MODE_REGEXP
            throws TclException
    {
        int offset = SwitchCmd.getBodyOffset(interp,
            pbObjv, pbStart, string, mode);
        return offset;
    }

    // Check a switch string and raise an error
    // if it starts with a '-' character. This
    // runtime check is needed for a compiled
    // switch command like [switch $str {...}]
    // which has no option terminator "--".

    public static void switchStringIsNotOption(
        Interp interp,
        String str)
            throws TclException
    {
        if ( str.startsWith("-") ) {
            TclObject[] objv = new TclObject[1];
            objv[0] = TclString.newInstance("switch");
            throw new TclNumArgsException(interp, 1, objv,
                "?switches? string pattern body ... ?default body?");
        }
    }

    // Raise exception when a variable could not
    // be set during a catch command. This
    // exception is defined here so that the
    // constant string need not appear in
    // every class file.

    public static void catchVarErr(
        Interp interp)
            throws TclException
    {
        throw new TclException(interp,
            "couldn't save command result in variable");
    }

    // Raise exception when a loop variable could
    // not be set in a foreach command.

    public static void foreachVarErr(
        Interp interp,
        String varname)
            throws TclException
    {
        throw new TclException(interp,
            "couldn't set loop variable: \"" +
            varname + "\"");
    }

    // Release an ExprValue that was returned by
    // one of the exprGetValue methods.

    public static void
    exprReleaseValue(
        Interp interp,
        ExprValue value)
    {
        interp.expr.releaseExprValue(value);
    }

    // Return the ExprValue for the given int value.
    // If the value was parsed from a String that
    // differs from the parsed value of the integer,
    // then it should be passed as the srep.

    public static
    ExprValue exprGetValue(
        Interp interp,
        int ival,
        String srep)
            throws TclException
    {
        ExprValue value = interp.expr.grabExprValue();
        value.setIntValue(ival, srep);
        return value;
    }

    // Return the expr value contained in the double

    public static
    ExprValue exprGetValue(
        Interp interp,
        double dval,
        String srep)
            throws TclException
    {
        ExprValue value = interp.expr.grabExprValue();
        value.setDoubleValue(dval, srep);
        return value;
    }

    // Return the expr value for the String

    public static
    ExprValue exprGetValue(
        Interp interp,
        String srep)
            throws TclException
    {
        ExprValue value = interp.expr.grabExprValue();
        value.setStringValue(srep);
        return value;
    }

    // Return the expr value contained in the TclObject

    public static
    ExprValue exprGetValue(
        Interp interp,
        TclObject tobj)
            throws TclException
    {
        return interp.expr.ExprParseObject(interp, tobj);
    }

    // Evaluate a unary expr operator.

    public static
    void exprUnaryOperator(
        Interp interp, // current interp, can't be null.
        int op,        // One of the EXPR_OP_* values
        ExprValue value)
            throws TclException
    {
        interp.expr.evalUnaryOperator(interp, op, value);
    }

    // Evaluate a binary expr operator. Note that this
    // method will always release the value2 argument,
    // so don't invoke exprReleaseValue for value2
    // or make use of the value after the method finishes.

    public static
    void exprBinaryOperator(
        Interp interp, // current interp, can't be null.
        int op,        // One of the EXPR_OP_* values
        ExprValue value,
        ExprValue value2)
            throws TclException
    {
        interp.expr.evalBinaryOperator(interp, op, value, value2);
    }

    // Evaluate a math function. This method will release
    // the values ExprValue objects when finished.

    public static
    ExprValue exprMathFunction(
        Interp interp, // current interp, can't be null.
        String funcName, // Name of math function
        ExprValue[] values) // Array of arguments
            throws TclException
    {
        return interp.expr.evalMathFunction(interp, funcName, values);
    }

    // Set the interp result to the given expr value. This
    // method is used only for a compiled version of the expr
    // command. Note that this method will release the value.

    public static
    void exprSetResult(Interp interp, ExprValue value)
	    throws TclException
    {
	switch (value.getType()) {
	case ExprValue.INT:
	    interp.setResult( value.getIntValue() );
	    break;
	case ExprValue.DOUBLE:
	    interp.setResult( value.getDoubleValue() );
	    break;
	case ExprValue.STRING:
	    interp.setResult( value.getStringValue() );
	    break;
	default:
	    throw new TclRuntimeError("internal error: expression, unknown");
	}
	interp.expr.releaseExprValue(value);
	return;
    }

    // Determine if the given TclObject is equal to
    // the empty string. This method implements an
    // optimized version of an expr comparison
    // like expr {$obj == ""} or expr {$obj != {}}.
    // This method will return an ExprValue object
    // that contains either the integer 1 or 0.

    public static
    ExprValue exprEqualsEmptyString(Interp interp, TclObject obj)
	    throws TclException
    {
        boolean isEmptyString;
        if (obj.hasNoStringRep() &&
                (obj.getInternalRep() instanceof TclList)) {
            // A pure Tcl list is equal to the empty string
            // when the list length is zero. This check
            // avoids the possibly slow generation of
            // a string rep from a pure TclList object.
            isEmptyString = (TclList.getLength(interp, obj) == 0);
        } else {
            isEmptyString = (obj.toString().length() == 0);
        }
        ExprValue value = interp.expr.grabExprValue();
        value.setIntValue(isEmptyString);
        return value;
    }

    // Implement an optimized version of the
    // int() math function used in an expr.
    // This method will raise an error if
    // the value is non-numeric, otherwise
    // it will case a double type to an int.

    public static
    void exprIntMathFunction(Interp interp, ExprValue value)
	    throws TclException
    {
        if (value.isStringType()) {
            throw new TclException(interp,
                "argument to math function didn't have numeric value");
        } else if (value.isDoubleType()) {
            double d = value.getDoubleValue();
            if (((d < 0) && (d < ((double) TCL.INT_MIN))) ||
                    ((d > 0) && (d > ((double) TCL.INT_MAX)))) {
                Expression.IntegerTooLarge(interp);
            }
            value.setIntValue((int) d);
        }
    }

    // Implement an optimized version of the
    // double() math function used in an expr.
    // This method will raise an error if
    // the value is non-numeric, otherwise
    // it will case an int type to a double.

    public static
    void exprDoubleMathFunction(Interp interp, ExprValue value)
	    throws TclException
    {
        if (value.isStringType()) {
            throw new TclException(interp,
                "argument to math function didn't have numeric value");
        } else if (value.isIntType()) {
            value.setDoubleValue( (double) value.getIntValue() );
        }
    }

}

