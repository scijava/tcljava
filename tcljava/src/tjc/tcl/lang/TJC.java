/*
 * Copyright (c) 2005 Advanced Micro Devices, Inc.
 *
 * See the file "license.amd" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TJC.java,v 1.14 2006/03/04 22:18:33 mdejong Exp $
 *
 */

// Runtime support for TJC compiler implementation.

package tcl.lang;

import java.util.HashMap;
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
    public static final Var VAR_NO_CACHE;

    static {
        // Setup a fake command wrapper. The only
        // point of this is to have a wrapper with
        // and invalid cmdEpoch that will never be
        // equal to a valid cmdEpoch for a command.

        INVALID_COMMAND_CACHE = new WrappedCommand();
        INVALID_COMMAND_CACHE.deleted = true;
        INVALID_COMMAND_CACHE.cmdEpoch = -1;

        // Create a fake Var reference that will be used
        // when a variable is resolved but can't be cached.
        // When a cached variable is set to this value,
        // no further attempts to cache the variable will
        // be made.

        VAR_NO_CACHE = new Var();
        VAR_NO_CACHE.setVarUndefined();
        VAR_NO_CACHE.setVarNoCache();
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

        // This flag is used to indicate that the command was
        // compiled with inlined Tcl commands.

        protected boolean inlineCmds = false;

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
        // Since this check is only done when the command is
        // first invoked, it should not be a performance concern.

        protected void builtinCommandsCheck(Interp interp)
            throws TclException
        {
            if (wcmd.ns.fullName.equals("::")) {
                return; // loaded into global namespace
            }
            String[] containers = {
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
            String[] containers_and_inlines = {
                "break",
                "catch",
                "continue",
                "expr",
                "for",
                "foreach",
                "global",
                "if",
                "list",
                "llength",
                "return",
                "set",
                "switch",
                "while"
            };
            String[] builtin =
                (inlineCmds ? containers_and_inlines : containers);
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

        // The following methods are used in compiled commands
        // that make use of cached variable access.

        // updateVarCache() will refresh a cached variable
        // refrence if needed before a cached variable value
        // is retrieved or after a cached variable value is set.
        // A class that extends CompiledCommand will implement
        // updateVarCache() if it supports cached variables.

        protected Var updateVarCache(
            Interp interp,
            int cacheId)
        {
            throw new TclRuntimeError("updateVarCache() should be overloaded");
        }

        // getVarScalar() will get a variable value, if a
        // cached variable is available then it will be used,
        // otherwise interp.getVar() will be invoked to get
        // the value. This method will raise a TclException
        // on error, it will never return null.

        protected final
        TclObject getVarScalar(
            Interp interp,
            String name,
            int flags,
            Var var,
            int cacheId)
                throws TclException
        {
            if (var == null ||
                    ((var != TJC.VAR_NO_CACHE) &&
                    var.isVarCacheInvalid())) {
                var = updateVarCache(interp, cacheId);
            }

            if (var == TJC.VAR_NO_CACHE) {
                return interp.getVar(name, null, flags);
            } else {
                return (TclObject) var.value;
            }
        }

        // setVarScalar() will set a variable value, if a
        // cached variable is available then it will be used,
        // otherwise interp.setVar() will be invoked to set
        // the value. This method will raise a TclException
        // on error, it will never return null.

        protected final
        TclObject setVarScalar(
            Interp interp,
            String name,
            TclObject value,
            int flags,
            Var var,
            int cacheId)
                throws TclException
        {
            TclObject retValue;
            boolean update = false;

            if (var == null ||
                    ((var != TJC.VAR_NO_CACHE) &&
                    var.isVarCacheInvalid())) {
                update = true;
            }

            if (update || (var == TJC.VAR_NO_CACHE)) {
                retValue = interp.setVar(name, null, value, flags);
            } else {
                retValue = TJC.setVarScalar(var, value);
            }

            if (update) {
                updateVarCache(interp, cacheId);
            }

            return retValue;
        }

        protected final
        TclObject setVarScalar(
            Interp interp,
            String name,
            String value,
            int flags,
            Var var,
            int cacheId)
                throws TclException
        {
            TclObject tobj = interp.checkCommonString(value);
            return setVarScalar(interp, name, tobj, flags, var, cacheId);
        }

        protected final
        TclObject incrVarScalar(
            Interp interp,
            String name,
            int incrAmount,
            int flags,
            Var var,
            int cacheId)
                throws TclException
        {
            if (var == null ||
                    ((var != TJC.VAR_NO_CACHE) &&
                    var.isVarCacheInvalid())) {
                var = updateVarCache(interp, cacheId);
            }

            if (var == TJC.VAR_NO_CACHE) {
                return TJC.incrVar(interp, name, incrAmount);
            } else {
                TclObject varValue = (TclObject) var.value;

                boolean createdNewObj = false;
                if (varValue.isShared()) {
                    varValue = varValue.duplicate();
                    createdNewObj = true;
                }
                try {
                    TclInteger.incr(interp, varValue, incrAmount);
                } catch (TclException ex) {
                    if (createdNewObj) {
                        varValue.release(); // free unneeded copy
                    }
                    throw ex;
                }

                // If we create a new TclObject, then save it
                // as the variable value.

                if (createdNewObj) {
                    return TJC.setVarScalar(var, varValue);
                } else {
                    return varValue;
                }
            }
        }

        protected final
        TclObject lappendVarScalar(
            Interp interp,
            String varName,
            TclObject[] values,
            Var var,
            int cacheId)
                throws TclException
        {
            // Use runtime impl of lappend if cached var
            // is not valid or not set. The lappend command
            // accepts an undefined variable name, but we
            // don't optimize that case.

            if ((var == null) ||
                    (var == TJC.VAR_NO_CACHE) ||
                    var.isVarCacheInvalid()) {
                return TJC.lappendVar(interp, varName, values);
            }

            // The cache var is valid, but it might indicate
            // a shared value. If the value is shared then
            // we need to duplicate it and invoke setVar()
            // to implement "copy on write".

            TclObject varValue = (TclObject) var.value;
            boolean createdNewObj = false;

            if (varValue.isShared()) {
                varValue = varValue.duplicate();
                createdNewObj = true;
            }

            // Insert the new elements at the end of the list.

            final int len = values.length;
            if (len == 1) {
                TclList.append(interp, varValue, values[0]);
            } else {
                TclList.append(interp, varValue, values, 0, len);
            }

            if (createdNewObj) {
                TJC.setVarScalar(var, varValue);
            }

            return varValue;
        }

        protected final
        TclObject appendVarScalar(
            Interp interp,
            String varName,
            TclObject[] values,
            Var var,
            int cacheId)
                throws TclException
        {
            // Use runtime impl of append if cached var
            // is not valid or not set. The append command
            // accepts an undefined variable name, but we
            // don't optimize that case.

            if ((var == null) ||
                    (var == TJC.VAR_NO_CACHE) ||
                    var.isVarCacheInvalid()) {
                return TJC.appendVar(interp, varName, values);
            }

            // The cache var is valid, but it might indicate
            // a shared value. If the value is shared then
            // we need to create a new TclString object
            // and drop refs to the previous TclObject value.

            TclObject varValue = (TclObject) var.value;
            boolean createdNewObj = false;

            if (varValue.isShared()) {
                varValue = TclString.newInstance(varValue.toString());
                createdNewObj = true;
            }

            // Insert the new elements at the end of the string.

            final int len = values.length;
            if (len == 1) {
                TclString.append(varValue, values[0].toString());
            } else {
                TclString.append(varValue, values, 0, len);
            }

            if (createdNewObj) {
                TJC.setVarScalar(var, varValue);
            }

            return varValue;
        }
    } // end class CompiledCommand

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
        HashMap filesTable;

        InitSourceCmd(String init_file, String[] files, String prefix)
        {
            this.init_file = init_file;
            this.files = files;
            this.prefix = prefix;

            filesTable = new HashMap();
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

    // Resolve a scalar variable name into a Var reference that
    // can be cached. Interpreted code would normally invoke
    // interp.getVar() or interp.setVar() to query or set a
    // scalar variable. This method is used to lookup a
    // variable by name and then cache the lookup result so
    // that the lookup step need not be done on each variable
    // access inside the command. If the variable is undefined,
    // has traces, or can't be cached for some reason then
    // this method will return the special ref TJC.VAR_NO_CACHE.
    // This method will never return null.
    //
    // This command is always invoked after a new CallFrame
    // has been pushed, so it is safe to assume that the
    // current namespace is the namespace the command is
    // defined in and that a local variable call frame exists.

    public static
    Var resolveVarScalar(
        Interp interp,
        String name,
        int flags)    // 0, TCL.GLOBAL_ONLY, or TCL.NAMESPACE_ONLY
    {
        boolean nocache = false;

        // Double check that this method is being invoked
        // from inside a Tcl procedure implementation with
        // a local call frame.

        CallFrame frame = interp.frame;

        if (frame == null) {
            throw new TclRuntimeError("unexpected null CallFrame");
        } else if (frame.isProcCallFrame == false) {
            throw new TclRuntimeError("expected isProcCallFrame to be true");
        }

        // Double check that scalar name is not actually an array
        // name like "arr(foo)".

        if (name.indexOf('(') != -1) {
            if (name.charAt(name.length() - 1) == ')') {
                throw new TclRuntimeError("unexpected array variable name \"" +
                    name + "\"");
            }
        }

        // Note that TCL.LEAVE_ERR_MSG is not passed here
        // since a failed lookup will return TJC.VAR_NO_CACHE
        // instead of raising a TclExeption. This is needed
        // so that the proper error message is raised by
        // a later call to either getVar() or setVar().

        Var[] result;
        try {
            result = Var.lookupVar(interp, name, null,
        			 flags,
        			 "", false, false);
        } catch (TclException te) {
            // Variable lookup should never raise a TclException
            // since  we don't pass TCL.LEAVE_ERR_MSG.
            result = null;
        }

        if (result == null) {
            // Variable lookup failed so no var cache is possible
            return TJC.VAR_NO_CACHE;
        }

	Var var = result[0];
	Var array = result[1];

        if (var == null) {
            throw new TclRuntimeError("unexpected null var result from lookupVar()");
        }

        // Don't cache the variable if read or write traces exist.

        if (var.traces != null) {
            nocache = true;
        }

        // Variable should be a scalar and it should be defined.

        else if (array != null ||
                var.isVarUndefined() ||
                !var.isVarScalar()) {
            nocache = true;
        }

        // isVarInHashtable() is true for all variables. The
        // C implementation also supports "compiled locals"
        // that are not stored in a hashtable.

        else if (var.isVarArray()) {
            throw new TclRuntimeError("unexpected array var");
        }
        else if (var.isVarLink()) {
            throw new TclRuntimeError("unexpected link var");
        }

        // If the variable is an array element then we can't cache it.
        // This would only happen when an upvar linked a local scalar
        // to an array element.

        // If the variable was marked as invalid for cache purposes
        // because of a redirected upvar, then don't cache it.

        // FIXME: The C Tcl impl marks variables resolved by a namespace
        // or interp resolver as invalid for cache purposes.

        else if (var.isVarArrayElement() ||
                var.isVarCacheInvalid()) {
            nocache = true;
        }

        if (nocache) {
            return TJC.VAR_NO_CACHE;
        }

        // Return the Var reference for a scalar local variable.

        return var;
    }

    // This method is used to set the TclObject value
    // contained inside a cached scalar Var reference.
    // This method works like interp.setVar(), it should
    // be used when a valid cached var reference is
    // held.

    public static final
    TclObject setVarScalar(
        Var var,
        TclObject newValue)    // New value to set varible to, can't be null
    {
        if (var.value != newValue) {
            TclObject oldValue = (TclObject) var.value;
            var.value = newValue;
            newValue.preserve();
            if (oldValue != null) {
                oldValue.release();
            }
        }
        // Unlike Var.setVar(), this method does not invoke
        // setVarScalar() or clearVarUndefined() since a
        // cached array variable will never be passed to
        // this method and an undefined variable will never
        // be cached.
        return newValue;
    }

    // This method is invoked when a compiled incr command
    // is inlined. This methods works for both scalar
    // variables and array scalars. This method is not
    // used for cached variables.

    public static final
    TclObject incrVar(
        Interp interp,
        String name,
        int incrAmount)
	    throws TclException
    {
        return Var.incrVar(interp, name, null,
            incrAmount, TCL.LEAVE_ERR_MSG);
    }

    // Get a TclObject[] of the given size. This array will
    // be allocated quickly from a cache if it is a common size.
    // The array must be released by releaseObjv(). Each element
    // in the returned array will be null.

    public static TclObject[] grabObjv(
        Interp interp,
        final int size)
    {
        return Parser.grabObjv(interp, size);
    }

    // For each non-null TclObject ref in the array, invoke
    // TclObject.release() and return the array to the
    // cache of common array values. The array must have
    // been allocated with grabObjv(). If zero is passed
    // as the size argument, this method will not invoke
    // TclObject.release() for any array values.
    // In either case, all array indexes are set to null
    // by the Parser.releaseObjv() method.

    public static void releaseObjv(
        Interp interp,
        TclObject[] objv,
        final int size)
    {
        if (size == 0) {
            Parser.releaseObjv(interp, objv, objv.length);
        } else {
            for (int i=0; i < size; i++) {
                TclObject tobj = objv[i];
                if (tobj != null) {
                    tobj.release();
                }
            }
            Parser.releaseObjv(interp, objv, size);
        }
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
            String cmdName = objv[0].toString();
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
                int len = objv.length;
                if (len == 0) {
                    throw new TclRuntimeError("zero length objv array");
                }
                TclObject[] newObjv = Parser.grabObjv(interp, len + 1);
                newObjv[0] = TclString.newInstance("unknown");
                newObjv[0].preserve();
                for (int i = (len - 1); i >= 0; i--) {
                    newObjv[i+1] = objv[i];
                }
                objv = newObjv;
                grabbed_objv = true;
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

                int len = objv.length;
                if (len == 0) {
                    throw new TclRuntimeError("zero length objv array");
                }
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
                Parser.releaseObjv(interp, objv, objv.length);
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
        InternalRep rep = obj.getInternalRep();

        if (rep instanceof TclInteger) {
            return (TclInteger.get(interp, obj) != 0);
        } else if (rep instanceof TclBoolean) {
            return TclBoolean.get(interp, obj);
        } else if (rep instanceof TclDouble) {
            return (TclDouble.get(interp, obj) != 0.0);
        } else if (rep instanceof TclList) {
            if (obj.hasNoStringRep() && (TclList.getLength(interp, obj) == 1)) {
                // If a pure list is of length one, then
                // check for the case of an integer or boolean.

                TclObject elem = TclList.index(interp, obj, 0);
                if (elem.getInternalRep() instanceof TclInteger) {
                    return (TclInteger.get(interp, elem) != 0);
                }
            }
        }
        return Util.getBoolean(interp, obj.toString());
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

    // Return the expr value for the boolean, this
    // boolean has no string rep and is represented
    // by an integer type.

    public static
    ExprValue exprGetValue(
        Interp interp,
        boolean bval)
            throws TclException
    {
        ExprValue value = interp.expr.grabExprValue();
        value.setIntValue(bval);
        return value;
    }

    // Return the expr value contained in the TclObject

    public static
    ExprValue exprGetValue(
        Interp interp,
        TclObject tobj)
            throws TclException
    {
        ExprValue value = interp.expr.grabExprValue();
        Expression.ExprParseObject(interp, tobj, value);
        return value;
    }

    // Evaluate a unary expr operator.

    public static
    void exprUnaryOperator(
        Interp interp, // current interp, can't be null.
        int op,        // One of the EXPR_OP_* values
        ExprValue value)
            throws TclException
    {
        Expression.evalUnaryOperator(interp, op, value);
    }

    // Evaluate a binary expr operator. Note that this
    // method will always release the value2 argument,
    // so don't invoke exprReleaseValue for value2
    // or make use of value2 after this method finishes.

    public static
    void exprBinaryOperator(
        Interp interp, // current interp, can't be null.
        int op,        // One of the EXPR_OP_* values
        ExprValue value,
        ExprValue value2)
            throws TclException
    {
        Expression.evalBinaryOperator(interp, op, value, value2);
        interp.expr.releaseExprValue(value2);
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
    ExprValue exprEqualsEmptyString(
        Interp interp,
        TclObject obj,
        final boolean negate)
	    throws TclException
    {
        boolean isEmptyString;

        if (obj.hasNoStringRep() &&
                (obj.getInternalRep() instanceof TclList)) {
            // A pure Tcl list is equal to the empty string
            // when the list length is zero. This check
            // avoids the possibly slow generation of
            // a string rep from a pure TclList object.
            isEmptyString = ( TclList.getLength(interp, obj) == 0 );
        } else {
            // TclObject already has a string rep, check if it is a
            // ref to the interned empty string or if the len is 0.
            String s = obj.toString();
            isEmptyString = ( s == "" || s.length() == 0 );
        }
        if (negate) {
            isEmptyString = !isEmptyString;
        }
        ExprValue value = interp.expr.grabExprValue();
        value.setIntValue( isEmptyString );
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

    // Implements inlined global command, this method
    // will create a local var linked to a global var.

    public static final
    void makeGlobalLinkVar(
        Interp interp,
        String varName,    // Fully qualified name of global variable.
        String varTail)    // Variable name without namespace qualifiers.
            throws TclException
    {
	// Link to the variable "varName" in the global :: namespace.
        // A local link var name varTail is defined.

	Var.makeUpvar(interp, null,
		varName, null, TCL.GLOBAL_ONLY,
	        varTail, 0);
    }

    // Implements inlined lindex command for non-constant integer
    // index values. This implementation is used only with lindex
    // commands that have 3 arguments. If the index argument
    // is already a TclInteger type then an optimized lindex
    // impl is used. The interp result is always set by this
    // method.

    public static final
    void lindexNonconst(
        Interp interp,
        TclObject listObj,     // List value
        TclObject indexValue)  // List index to be resolved.
            throws TclException
    {
        // Optimized check for integer indexValue argument.
        // This is the most common use case:
        // set obj [lindex $list $i]

        boolean intIndex = false;
        int index = -1;

        try {
            if (indexValue.getInternalRep() instanceof TclInteger) {
                index = TclInteger.get(interp, indexValue);
                intIndex = true;
            }
        } catch (TclException te) {
            throw new TclRuntimeError(
                "unexpected TclException getting index value");
        }

        if (intIndex) {
            TclObject result = TclList.index(interp, listObj, index);
            if (result == null) {
                interp.resetResult();
            } else{
                interp.setResult(result);
            }
            return;
        } else {
            // Invoke the static lindex command impl.

            TclObject[] objv = TJC.grabObjv(interp, 3);
            try {
                //objv[0] = null;

                objv[1] = listObj;

                objv[2] = indexValue;
                indexValue.preserve();

                TclObject elem = LindexCmd.TclLindexList(interp, listObj, objv, 2);
                interp.setResult(elem);
                elem.release();
            } finally {
                objv[1] = null; // Caller should preserve() list
                TJC.releaseObjv(interp, objv, 3);
            }
            return;
        }
    }

    // Implements inlined lappend command that appends 1 or more
    // TclObject values to a variable. This implementation
    // makes use of runtime support found in the LappendCmd class.
    // The new variable value after the lappend operation is returned.

    public static final
    TclObject lappendVar(
        Interp interp,
        String varName,        // Name of variable
        TclObject[] values)    // Array of TclObject values to append
            throws TclException
    {
        return LappendCmd.lappendVar(interp, varName, values, 0);
    }

    // Implements inlined append command that appends 1 or more
    // TclObject values to a variable. This implementation
    // duplicates the logic found in AppendCmd. The new
    // variable value after the lappend operation is returned.

    public static final
    TclObject appendVar(
        Interp interp,
        String varName,        // Name of variable
        TclObject[] values)    // Array of TclObject values to append
            throws TclException
    {
        TclObject varValue = null;
        final int len = values.length;

	for (int i = 0; i < len; i++) {
	    varValue = interp.setVar(varName, values[i], TCL.APPEND_VALUE);
	}

        if (varValue == null) {
            // Return empty result object if null
            varValue = interp.checkCommonString(null);
        }

        return varValue;
    }

    // Implements inlined string index command. This
    // implementation duplicates the logic found
    // in StringCmd.java. The new value is returned.
    // If the index is out of range the null result
    // will be returned.

    public static final
    TclObject stringIndex(
        Interp interp,
        String str,          // string
        TclObject indObj)    // index into string
            throws TclException
    {
        int len = str.length();
        int i;

        if (indObj.getInternalRep() instanceof TclInteger) {
	    i = TclInteger.get(interp, indObj);
        } else {
            i = Util.getIntForIndex(interp, indObj, len - 1);
        }

        if ((i >= 0) && (i < len)) {
            TclObject obj = interp.checkCommonCharacter(str.charAt(i));
            if (obj == null) {
                obj = TclString.newInstance(
                    str.substring(i, i+1));
            }
            return obj;
        } else {
            return interp.checkCommonString(null);
        }
    }

    // Implements inlined string range command. This
    // implementation duplicates the logic found
    // in StringCmd.java. The new value is returned.
    // This method assumes that the firstObj TclObject
    // was preserved() before this method is invoked.
    // This method will always release() the firstObj.

    public static final
    TclObject stringRange(
        Interp interp,
        String str,          // string
        TclObject firstObj,  // first index (ref count incremented)
        TclObject lastObj)   // last index
            throws TclException
    {
        int len = str.length();
        int first, last;

        try {
            if (firstObj.getInternalRep() instanceof TclInteger) {
	        first = TclInteger.get(interp, firstObj);
            } else {
                first = Util.getIntForIndex(interp, firstObj, len - 1);
            }
            if (first < 0) {
                first = 0;
            }

            if (lastObj.getInternalRep() instanceof TclInteger) {
	        last = TclInteger.get(interp, lastObj);
            } else {
                last = Util.getIntForIndex(interp, lastObj, len - 1);
            }
            if (last >= len) {
                last = len - 1;
            }
        } finally {
            // Release firstObj after lastObj has been queried.
            // There could only be a ref count problem if firstObj
            // was released before lastObj and lastObj was a list
            // element of firstObj and lastObj had a ref count of 1.

            firstObj.release();
        }

        if (first > last) {
            return interp.checkCommonString(null);
        } else {
            String substr = str.substring(first, last+1);
            return TclString.newInstance(substr);
        }
    }

    // Implements inlined "string first" command, this
    // duplicates the logic found in StringCmd.java.
    // A TclObject that holds the new value is
    // returned.

    public static final
    TclObject stringFirst(
        Interp interp,
        String substr,       // substring to search for
        String str,          // string to search in
        TclObject startObj)  // start index (null if start is 0)
            throws TclException
    {
        int substrLen = substr.length();
        int strLen = str.length();
        int index;

        int start;

        if (startObj == null) {
            start = 0;
        } else {
            // If a startIndex is specified, we will need to fast
            // forward to that point in the string before we think
            // about a match.

            start = Util.getIntForIndex(interp, startObj, strLen-1);
            if (start >= strLen) {
                return interp.checkCommonInteger(-1);
            }
        }

        if (substrLen == 0) {
            index = -1;
        } else if (substrLen == 1) {
            char c = substr.charAt(0);
            index = str.indexOf(c, start);
        } else {
            index = str.indexOf(substr, start);
        }
        return interp.checkCommonInteger(index);
    }

    // Implements inlined "string last" command, this
    // duplicates the logic found in StringCmd.java.
    // A TclObject that holds the new value is
    // returned.

    public static final
    TclObject stringLast(
        Interp interp,
        String substr,       // substring to search for
        String str,          // string to search in
        TclObject lastObj)   // last index (null if last is 0)
            throws TclException
    {
        int substrLen = substr.length();
        int strLen = str.length();
        int index;
        int last;

        if (lastObj == null) {
            last = 0;
        } else {
            last = Util.getIntForIndex(interp, lastObj, strLen-1);
            if (last < 0) {
                return interp.checkCommonInteger(-1);
            } else if (last < strLen) {
                str = str.substring(0, last+1);
            }
        }

        if (substrLen == 0) {
            index = -1;
        } else if (substrLen == 1) {
            char c = substr.charAt(0);
            index = str.lastIndexOf(c);
        } else {
            index = str.lastIndexOf(substr);
        }
        return interp.checkCommonInteger(index);
    }

}

