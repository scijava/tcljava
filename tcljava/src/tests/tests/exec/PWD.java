/* 
 * PWD.java --
 *
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: PWD.java,v 1.1 1998/10/14 21:09:12 cvsadmin Exp $
 *
 */

package tests.exec;

public class PWD {
  public static void main(String[] argv) {
    String dirName = System.getProperty("user.dir");
    
    String os = System.getProperty("os.name");

    if (os.toLowerCase().startsWith("win")) {
      dirName = dirName.replace('\\', '/');
    }
    
    System.out.println( dirName );
    System.exit(0);
  }
}

