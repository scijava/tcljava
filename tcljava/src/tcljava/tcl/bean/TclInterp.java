/*
 * TclInterp.java
 *
 *	The class represents the Tcl interpreter wrapped into a bean.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclInterp.java,v 1.1 1998/10/14 21:09:16 cvsadmin Exp $
 */

package tcl.bean;

import java.lang.*;
import java.awt.*;
import java.io.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import tcl.lang.*;

public class  TclInterp implements java.io.Serializable
{
static final long serialVersionUID = 7850252431308604578L;
       
private String  script;
transient Interp interp = null;

public
TclInterp()
{
    script = "";
    interp = new Interp();
}

public Interp
getInterp()
{
    if (interp == null) {
	interp = new Interp();
    }	    
    return interp;
}

public Interp
resetInterp()
{
    if (interp != null) {
	interp.dispose();
    }
    interp = new Interp();
    return interp;
}

public String
getScript()
{
    return script;
}

public void
setScript(String s)
{
    script = s;
}
} // TclIntrerp


