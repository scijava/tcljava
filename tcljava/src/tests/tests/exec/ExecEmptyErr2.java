/* 
 * ExecEmptyErr2.java --
 *
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: ExecEmptyErr2.java,v 1.1 1998/10/14 21:09:12 cvsadmin Exp $
 *
 */

package tests.exec;

public class ExecEmptyErr2 {
  public static void main(String[] argv) {
    System.out.println("!stdout!");
    System.exit(-1);
  }
}

