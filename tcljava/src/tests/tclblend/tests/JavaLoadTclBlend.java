/* 
 * JavaLoadTclBlend.java --
 *
 *      This file tests loading of Tcl Blend into a JVM.
 *
 * Copyright (c) 2002 by Mo DeJong
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: JavaLoadTclBlend.java,v 1.1 2002/07/20 05:36:54 mdejong Exp $
 */

package tests;

import tcl.lang.*;

public class JavaLoadTclBlend {
    public static void main(String[] args) throws Exception {
        Interp interp = new Interp();
        interp.eval("expr {1 + 2}");
        String num = interp.getResult().toString();
        if (!num.equals("3"))
            System.exit(-1);
        interp.dispose();
        System.exit(0);
    }
}

