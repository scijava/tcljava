/* 
 * MethodInvoker4.java --
 *
 * This test is used to check the exceptions to the
 * method resolver rules when the class Object is
 * involved. When resolving we consider a signature
 * with the Object class as less important then
 * an interface that matches the signature.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: MethodInvoker6.java,v 1.1 1998/10/14 21:09:12 cvsadmin Exp $
 *
 */

package tests.signature;

public class MethodInvoker6 {

  private static interface I {}
  private static class A extends Object implements I {}
  
  public static String call(Object obj) {
    return "O";
  }
  public static String call(I obj) {
    return "I";
  }


  public static Object getO() {
    return new A();
  }

  public static A getA() {
    return new A();
  }


  public static void main(String[] argv) {

    Object o = getO();
    A a = getA();

    String s;

    s = call( o ); //should return "O"
    p(s);

    s = call( a ); //should return "I"
    p(s);

  }

  public static void p(String arg) {
    System.out.println(arg);
  }

}
