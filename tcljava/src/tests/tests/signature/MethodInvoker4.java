/* 
 * MethodInvoker4.java --
 *
 * This class is used to test the method invoker
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: MethodInvoker4.java,v 1.1 1998/10/14 21:09:12 cvsadmin Exp $
 *
 */

package tests.signature;

import java.util.*;

public class MethodInvoker4 {

  public static String call(A obj) {
    return "A";
  }
  public static String call(I obj) {
    return "I";
  }
  
  private static interface I {}
  private static class A {}
  private static class B implements I {}
  

  public static A getA() {
    return new A();
  }

  public static B getB() {
    return new B();
  }

  public static I getI() {
    return new B();
  }

  public static void main(String[] argv) {

    A a = getA();
    B b = getB();
    I i = getI();
    
    String s;
    
    s = call(a); //should return "A"
    p(s);
    
    s = call(i); //should return "I"
    p(s);

    s = call(b); //should return "I"
    p(s);

  }


  public static void p(String arg) {
    System.out.println(arg);
  }

}
