/* 
 * JavaThreadLoadTclBlend.java --
 *
 *      This class implements a thread that uses Tcl Blend.
 *
 * Copyright (c) 2002 by Mo DeJong
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: JavaThreadLoadTclBlend.java,v 1.1 2002/07/20 05:36:54 mdejong Exp $
 */

package tests;

import tcl.lang.*;

public class JavaThreadLoadTclBlend implements Runnable {
    final boolean debug = false;

    public void run() {
        try {
            if (debug)
                System.out.println("run()");
            Interp interp = new Interp();
            interp.eval("expr {1 + 2}");
            String num = interp.getResult().toString();
            if (!num.equals("3"))
                System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        if (debug)
            System.out.println("run() finished");
        return; // Thread dies at this point
    }
}

