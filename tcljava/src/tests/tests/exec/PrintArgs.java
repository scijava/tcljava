/* 
 * PrintArgs.java --
 *
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: PrintArgs.java,v 1.1 1998/10/14 21:09:12 cvsadmin Exp $
 *
 */

package tests.exec;

public class PrintArgs {
  public static void main(String[] argv) {
    for (int i=0;i<argv.length;i++) {
      System.out.println(argv[i]);
    }
    System.exit(0);
  }
}

