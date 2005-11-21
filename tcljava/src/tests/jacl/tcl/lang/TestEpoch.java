/*
 * TestEpoch.java --
 *
 * Copyright (c) 2005 Mo DeJong
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TestEpoch.java,v 1.1 2005/11/21 02:02:41 mdejong Exp $
 *
 */

package tcl.lang;

class TestEpoch
{
    // Increment a command's WrappedCommand.cmdEpoch when deleting
    // a command because the command is redefined.

    public static String testCmdEpoch1(Interp interp) throws TclException {
        String cmdName = "testCmdEpoch1";

        // Create proc
        interp.eval("proc " + cmdName + " {} {return " + cmdName + "}", TCL.EVAL_GLOBAL);

        // Get WrappedCommand for proc
        WrappedCommand wcmd = Namespace.findCommand(interp,
            cmdName, null, TCL.GLOBAL_ONLY);

        // Save cmdEpoch
        int cmdEpoch = wcmd.cmdEpoch;

        // Redefine proc
        interp.eval("proc " + cmdName + " {} {return dummy}", TCL.EVAL_GLOBAL);

        // Double check that command was actually redefined
        WrappedCommand new_wcmd = Namespace.findCommand(interp,
            cmdName, null, TCL.GLOBAL_ONLY);
        if (wcmd == new_wcmd) {
            return "ERROR: command not redefined";
        }

        // Check that cmdEpoch was incremented on original cmd ref

        String result;
        if (wcmd.cmdEpoch == (cmdEpoch + 1)) {
            result = "OK";
        } else {
            result = "expected cmdEpoch " + (cmdEpoch + 1) + " but got " +
                wcmd.cmdEpoch;
        }

        return result;
    }

    // Increment a command's WrappedCommand.cmdEpoch when deleting
    // a command.

    public static String testCmdEpoch2(Interp interp) throws TclException {
        String cmdName = "testCmdEpoch2";

        // Create proc
        interp.eval("proc " + cmdName + " {} {return " + cmdName + "}", TCL.EVAL_GLOBAL);

        // Get WrappedCommand for proc
        WrappedCommand wcmd = Namespace.findCommand(interp,
            cmdName, null, TCL.GLOBAL_ONLY);

        // Save cmdEpoch
        int cmdEpoch = wcmd.cmdEpoch;

        // Delete proc
        interp.eval("rename " + cmdName + " {}", TCL.EVAL_GLOBAL);

        // Check that cmdEpoch was incremented on original cmd ref

        String result;
        if (wcmd.cmdEpoch == (cmdEpoch + 1)) {
            result = "OK";
        } else {
            result = "expected cmdEpoch " + (cmdEpoch + 1) + " but got " +
                wcmd.cmdEpoch;
        }

        return result;
    }

    // Increment a command's WrappedCommand.cmdEpoch when the
    // command is renamed.

    public static String testCmdEpoch3(Interp interp) throws TclException {
        String cmdName = "testCmdEpoch3";

        // Create proc
        interp.eval("proc " + cmdName + " {} {return " + cmdName + "}", TCL.EVAL_GLOBAL);

        // Get WrappedCommand for proc
        WrappedCommand wcmd = Namespace.findCommand(interp,
            cmdName, null, TCL.GLOBAL_ONLY);

        // Save cmdEpoch
        int cmdEpoch = wcmd.cmdEpoch;

        // Rename proc
        interp.eval("catch {rename dummy" + cmdName + " {}}", TCL.EVAL_GLOBAL);
        interp.eval("rename " + cmdName + " dummy" + cmdName, TCL.EVAL_GLOBAL);

        // Double check that command was actually renamed
        WrappedCommand new_wcmd = Namespace.findCommand(interp,
            cmdName, null, TCL.GLOBAL_ONLY);
        if (new_wcmd != null) {
            return "ERROR: command not renamed";
        }

        // Check that cmdEpoch was incremented on original cmd ref

        String result;
        if (wcmd.cmdEpoch == (cmdEpoch + 1)) {
            result = "OK";
        } else {
            result = "expected cmdEpoch " + (cmdEpoch + 1) + " but got " +
                wcmd.cmdEpoch;
        }

        return result;
    }
}

