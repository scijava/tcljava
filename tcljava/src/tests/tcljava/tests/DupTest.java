/*
 * DupTest.java --
 *
 * This class is used to regression test a trcky case where an
 * inner class and a toplevel class have the same name.
 *
 * Copyright (c) 2006 by Moses DeJong
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: DupTest.java,v 1.1 2006/04/13 07:36:51 mdejong Exp $st.java,v 1.1 1999/08/09 08:52:36 mo Exp $
 *
 */

package tests;

public class DupTest {

  public static Object call() {
      // Resolved to the static inner class constructor
      // at compile time.

      return new tests.InnerTest.DupName();
  }

}

