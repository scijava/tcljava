/*
 * SplitCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Cache.java,v 1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang;

public class Cache {
  private static long hit = 0;
  private static long miss = 0;
  private static long recache = 0;
  private static long release = 0;

  public static void info() {
    System.out.println("hit     = " + hit);
    System.out.println("miss    = " + miss);
    System.out.println("recache = " + recache);
    System.out.println("release = " + release);
  }

  static void hit() {
    hit++;
  }

  static void miss() {
    miss++;
  }

  static void recache() {
    recache++;
  }

  static void release() {
    release++;
  }

}
