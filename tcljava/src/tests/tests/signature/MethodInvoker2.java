/* 
 * MethodInvoker2.java --
 *
 * This class is used to test the method invoker
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: MethodInvoker2.java,v 1.1 1998/10/14 21:09:12 cvsadmin Exp $
 *
 */

package tests.signature;

public class MethodInvoker2 {

  public static class A {}
  public static class B extends A {}
  public static class C extends B {}


  public static A getA() {
    return new A();
  }
  public static B getB() {
    return new B();
  }
  public static C getC() {
    return new C();
  }
  
  public static String call(A arg1, A arg2) {
    return "A+A";
  }
  public static String call(A arg1, C arg2) {
    return "A+C";
  }
  public static String call(B arg1, C arg2) {
    return "B+C";
  }


  public static void main(String[] argv) {
    A a = getA();
    B b = getB();
    C c = getC();

    String s;

    s = call(a,a); //should return A+A
    p(s);
    
    s = call(a,c); //should return A+C
    p(s);

    s = call(c,a); //should return A+A
    p(s);

    s = call(c,c); //should return B+C
    p(s);
  }


  public static void p(String arg) {
    System.out.println(arg);
  }

}
