/*
 * MethodFailure1.java --
 *
 * tcljava/tests/signature/MethodFailure1.java
 *
 * Copyright (c) 1998 by Moses DeJong
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: MethodFailure1.java,v 1.1 1999/05/10 04:09:07 dejong Exp $
 *
 */

package tests.signature;

import java.util.*;

public class MethodFailure1 {

  public static String call(A obj) {
    return "A";
  }
  public static String call(I obj) {
    return "I";
  }


  // this method invocation is ambiguous
  // call( getB() );


  private static interface I {}
  private static class A {}
  private static class B extends A implements I {}
  

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
