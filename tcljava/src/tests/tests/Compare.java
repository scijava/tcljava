/* 
 * CompareTest.java --
 *
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: Compare.java,v 1.1 1998/10/14 21:09:11 cvsadmin Exp $
 *
 */

package tests;

import java.util.*;

public class Compare {
  private Hashtable thing = new Hashtable();

  public Compare() {}
  
  public void empty() {
    thing = null;
  }

  public Object get1() {
    return thing;
  }

  public Dictionary get2() {
    return thing;
  }

  public Hashtable get3() {
    return thing;
  }

  public boolean compare(Object o1, Object o2) {
    return (o1 == o2);
  }
}

