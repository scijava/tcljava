/* 
 * JavaEval.java --
 *
 *      This class creates a Tcl interpreter, evaluates a command,
 *      and returns the result as a Java String.
 *
 * Copyright (c) 2002 by Mo DeJong
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: JavaEval.java,v 1.1 2002/07/20 05:36:54 mdejong Exp $
 */

package tests;

import tcl.lang.*;

public class JavaEval {
    public static String eval(String cmd) throws Exception {
        Interp interp = new Interp();
        interp.eval(cmd);
        String result = interp.getResult().toString();
        interp.dispose();
        interp = null;
        return result;
    }

    public static String eval(Interp interp, String cmd) throws Exception {
        interp.eval(cmd);
        String result = interp.getResult().toString();
        return result;
    }

    public static void main(String[] args) throws Exception {
        String cmd = null, expected = null, result;
        if (args.length == 0) {
            cmd = "";
        } else if (args.length == 1) {
            cmd = args[0];
        } else if (args.length == 2) {
            cmd = args[0];
            expected = args[1];
        } else {
            System.out.println("Wrong # args : should be \"cmd ?expected?\"");
            System.exit(-1);
        }
        result = eval(cmd);
        if (expected != null) {
            if (!result.equals(expected))
                System.out.println("result mismatch");
        }
        System.exit(-2); // We expect the script to call exit
    }
}
