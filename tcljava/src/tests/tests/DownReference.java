/* 
 * DownReference.java --
 *
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: DownReference.java,v 1.1 1998/10/14 21:09:11 cvsadmin Exp $
 *
 */

package tests;

import java.util.*;

public class DownReference extends Object {

  public DownReference() {}

  public static Object newInstance() {
    return new DownReference();
  }

  public String getSecret() {
    return "X123";
  }

}

