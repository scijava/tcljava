/*
 * TclInterpBeanInfo.java
 *
 *	The class represents the Tcl interpreter wrapped into a bean.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclInterpBeanInfo.java,v 1.1 1998/10/14 21:09:17 cvsadmin Exp $
 */

package tcl.bean;

import java.beans.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;


public class TclInterpBeanInfo extends SimpleBeanInfo
{
    private final static Class beanClass = tcl.bean.TclInterp.class;

public BeanDescriptor
getBeanDescriptor()
{
    BeanDescriptor bd = new BeanDescriptor(tcl.bean.TclInterp.class);
    bd.setDisplayName("Tcl Interpreter");
    return bd;
}

/*
 *----------------------------------------------------------------------
 *
 * getIcon --
 *
 *	Get the specified icon for the Tcl bean.
 *
 * Results:
 *	A gif image
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public java.awt.Image 
getIcon(int iconKind) 
{
    if (iconKind == BeanInfo.ICON_MONO_16x16 ||
	    iconKind == BeanInfo.ICON_COLOR_16x16 ) {
	java.awt.Image img = loadImage("jaclbean16.gif");
	return img;
    }
    if (iconKind == BeanInfo.ICON_MONO_32x32 ||
	    iconKind == BeanInfo.ICON_COLOR_32x32 ) {
	java.awt.Image img = loadImage("jaclbean32.gif");
	return img;
    }
    return null;
}

}  // TclInterpBeanInfo

