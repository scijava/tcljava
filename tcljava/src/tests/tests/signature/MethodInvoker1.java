/* 
 * MethodInvoker1.java --
 *
 * This class is used to test the method invoker
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: MethodInvoker1.java,v 1.1 1998/10/14 21:09:12 cvsadmin Exp $
 *
 */

package tests.signature;

public class MethodInvoker1 {

  public static class A {}
  public static class B extends A {}


  public static A getA() {
    return new A();
  }
  public static B getB() {
    return new B();
  }
  
  public static String call(A arg1, A arg2) {
    return "A+A";
  }

  public static String call(A arg1, B arg2) {
    return "A+B";
  }


  public static void main(String[] argv) {
    A a = getA();
    B b = getB();

    String s;

    s = call(a,a); //should return A+A
    p(s);
    
    s = call(a,b); //should return A+B
    p(s);

    s = call(b,b); //should return A+B
    p(s);

    s = call(b,a); //should return A+A
    p(s);
  }


  public static void p(String arg) {
    System.out.println(arg);
  }

}
