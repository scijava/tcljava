/* 
 * MethodFailure2.java --
 *
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: MethodFailure2.java,v 1.1 1998/10/14 21:09:12 cvsadmin Exp $
 *
 */

package tests.signature;

import java.util.*;

public class MethodFailure2 {

  public static String call(A obj) {
    return "A";
  }
  public static String call(I obj) {
    return "I";
  }


  // this method invocation is ambiguous
  // call( getB() );


  private static interface I {}
  private static interface I2 extends I {}

  private static class A {}
  private static class B extends A implements I2 {}
  

  public static A getA() {
    return new A();
  }

  public static B getB() {
    return new B();
  }

  public static I getI() {
    return new B();
  }

}
