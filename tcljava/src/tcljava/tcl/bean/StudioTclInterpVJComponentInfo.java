/* 
 * StudioTclInterpVJComponentInfo.java --
 *
 *	The class defines the VJComponentInfo for our Tcl bean for
 *	Java Studio.  This class defines such things as the icon for the 
 *	bean, the help pages, and other meta information used by Studio.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: StudioTclInterpVJComponentInfo.java,v 1.1 1998/10/14 21:09:17 cvsadmin Exp $
 */

package tcl.bean;

import java.awt.Image;
import java.net.URL;
import com.sun.jpropub.vj.vjcomp.*;
import com.sun.jpro.vj.util.ConsoleWarning;

public class StudioTclInterpVJComponentInfo extends SimpleVJComponentInfo 
{
private Class vjClass;
private Class custClass;
private Class beanClass;
private static final String SMALL_IMG = "images/tclbean16.gif";
private static final String MEDIUM_IMG = "images/tclbean24.gif";
private static final String LARGE_IMG = "images/tclbean32.gif";
private static final String DOC_URL = "help.htm";
private static final String MANUFACTURER = "SUN";

public 
StudioTclInterpVJComponentInfo() 
{
    try {
	vjClass = Class.forName("tcl.bean.StudioTclInterp");
	custClass = Class.forName("tcl.bean.StudioTclInterpCustomizer");
	beanClass = Class.forName("tcl.bean.TclInterp");
    } catch (Exception e) {
	vjClass = null;
	custClass = null;
	System.out.println("error");
	ConsoleWarning.printStackTrace(e);
    }
}

public VJComponentDescriptor 
getVJComponentDescriptor()
{
    if (vjClass != null) {
	VJComponentDescriptor c;
	c = new VJComponentDescriptor(vjClass, beanClass, custClass);
	c.setCustomizerShownOnInstantiate(true);
	c.setDisplayName("Tcl Interp");
	c.setManufacturerName(MANUFACTURER);
	return c;
    }
    return null;
}

public Image 
getSmallImage() 
{
    return loadImage(SMALL_IMG);
}

public Image
getMediumImage()
{
    return loadImage(MEDIUM_IMG);
}

public Image
getLargeImage()
{
    return loadImage(LARGE_IMG);
}

public URL 
getHelpURL() 
{
    URL url = null;
    try {
	url = getClass().getResource(DOC_URL);
    } catch (Exception e) {
    }
    return url;
}
} // end StudioTclInterpComponentInfo

