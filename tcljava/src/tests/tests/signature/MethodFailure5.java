/* 
 * MethodFailure5.java --
 *
 * The purpose of this class is to test out method invocations
 * that are defined as ambiguous byt the JLS. We should not
 * be able to call these methods by passing in two derived
 * types as arguments ie call(Hashtable,Hashtable).
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: MethodFailure5.java,v 1.1 1998/10/14 21:09:12 cvsadmin Exp $
 *
 */

package tests.signature;

import java.util.*;

public class MethodFailure5 {

  public static String call(Dictionary a, Hashtable b) {
    return "D+H";
  }

  public static String call(Hashtable b, Dictionary a) {
    return "H+D";
  }

  public static Hashtable getH() {
    return new Hashtable();
  }

  public static Dictionary getD() {
    return new Hashtable();
  }



  /*

  public static void main(String[] args) {
    Hashtable b = new Hashtable();
    call(b,b); //can not compile this ambiguous method reference
  }

  */
  
}
