/*
 * StudioTclInterpCustomizer.java --
 *
 *	Implements the customized interface for a Tcl interp.
 *
 * Copyright (c) 1998 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: StudioTclInterpCustomizer.java,v 1.1 1998/10/14 21:09:17 cvsadmin Exp $
 */

package tcl.bean;

import tcl.lang.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.Vector;
import java.util.Enumeration;
import java.io.*;
import com.sun.jpro.vj.designtime.*;

public class StudioTclInterpCustomizer extends VJSimpleCustomizer 
{

/*
 * VJCustomizerPage for the script.
 */

private VJInterpPage interpPage;

/*
 * VJCustomizerPage for the info gif.
 */

private VJInfoPage infoPage;

/*
 *----------------------------------------------------------------------
 *
 * StudioTclInterpCustomizer --
 *
 *	The main object that stores and manipulates the customizer pages
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public 
StudioTclInterpCustomizer() 
{
    interpPage = new VJInterpPage();
    addCard(interpPage, "Variables");

    infoPage = new VJInfoPage();
    addCard(infoPage, "Info");
}
} // end StudioTclInterpCustomizer


/*
 *----------------------------------------------------------------------
 * VJInterpPage --
 *
 *	Implements the VJCustomizerPage for the script input.
 *
 *----------------------------------------------------------------------
 */


class VJInterpPage extends VJCustomizerPage implements ActionListener
{

/*
 * This is the JavaStudio bean which the customizer manipulates.  
 * It is passed in as an Object in the initPage() method.
 */

private StudioTclInterp studioInterp;

/*
 * The script that represents the beans behavior.  The value of the script
 * can change throughout the lifetime of the customizer, but it only stored
 * in the studioInterp when commitPage() is called. 
 */

private String beanScript;

/*
 * Cache of the last directory browsed.  Used by the showFileDialog
 * method.
 */

private static String lastDirName = null;

/*
 * The name of the script to read.  It can be a file or a resource in
 * the jar file.
 */

private TextField scriptInput;

/*
 * If a default customizer is used, this will display the contents
 * of the script.
 */

private TextArea scriptText;

/*
 * Simple label for default customization mode.  Used in the 
 * paintDefaultPanel() and loadScriptFile() methods
 */

private Label scriptLabel;

/*
 * Has to be a class variable to use it in the event bindings.
 */

private Choice scriptChoice;

/*
 * Used in the initPage() and paintXXXPanel() methods.
 */

private GridBagLayout gbl;
private GridBagConstraints gbc;


/*
 *----------------------------------------------------------------------
 *
 * VJInterpPage --
 *
 *	Default constructor that has to exist for the customizer to 
 *	work.  It does nothing.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public
VJInterpPage()
{
}

/*
 *----------------------------------------------------------------------
 *
 * initPage --
 *
 *	Draw the common widgets in the window and call the method
 *	to draw either the default UI or the custom.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The Object passed as an argument is the StudioTclInterp bean.
 *	Store a reference to this object in the studioInterp variable.
 *
 *----------------------------------------------------------------------
 */

public void 
initPage(
    Object obj) 	// The Java Studio bean.
{
    Panel padding;
    Label fileLabel;
    Button browseButton;

    /*
     * Cast the Object to a StudioTclInterp bean.
     */

    studioInterp = (StudioTclInterp) obj;

    /*
     * Create all the widgets that are common in default and
     * custom interfaces.
     */

    fileLabel = new Label("File:");
    scriptInput = new TextField(20);
    scriptInput.addActionListener(this);
    browseButton = new Button("Browse");
    browseButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent event) {
	    showFileDialog();
	}
    });
    scriptChoice = new Choice();
    scriptChoice.add("None");
    scriptChoice.add("choiceValue.tcl");
    scriptChoice.add("distrib.tcl");
    scriptChoice.add("eval.tcl");
    scriptChoice.add("exec.tcl");
    scriptChoice.add("inspect.tcl");
    scriptChoice.add("memory.tcl");
    scriptChoice.add("switch.tcl");
    scriptChoice.add("customDistrib.tcl");
    scriptChoice.add("customMemory.tcl");
    scriptChoice.add("customSwitch.tcl");
    scriptChoice.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent event) {
	    if (scriptChoice.getSelectedItem().equals("None")) {
		repaintCustomizer(null, "");
		return;
	    }
	    scriptInput.setText(scriptChoice.getSelectedItem());
	    loadScriptFile();
	}
    });
    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.NORTH;
    gbc.ipadx = 5;
    gbc.ipady = 5;
    setInsets(new Insets(10,10,10,10));
    setLayout(gbl);

    /*
     * File Label
     */

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth  = 1;
    gbc.gridheight = 1;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(fileLabel, gbc);
    add(fileLabel);

    /*
     * Script Input Text
     */

    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.gridwidth  = 1;
    gbc.gridheight = 1;
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(scriptInput, gbc);
    add(scriptInput);

    /*
     * Padding between Input Text and Browse Button
     */

    padding = new Panel();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.gridwidth  = 1;
    gbc.gridheight = 1;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(padding, gbc);
    add(padding);

    /*
     * Browse Button
     */

    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.gridwidth  = 1;
    gbc.gridheight = 1;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(browseButton, gbc);
    add(browseButton);

    /*
     * Padding between Browse Button and Script Choice
     */

    padding = new Panel();
    gbc.gridx = 4;
    gbc.gridy = 0;
    gbc.gridwidth  = 1;
    gbc.gridheight = 1;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(padding, gbc);
    add(padding);

    /*
     * Script Choice
     */

    gbc.gridx = 5;
    gbc.gridy = 0;
    gbc.gridwidth  = 1;
    gbc.gridheight = 1;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(scriptChoice, gbc);
    add(scriptChoice);

    /*
     * Padding between File row and Script row.
     */

    padding = new Panel();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth  = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(padding, gbc);
    add(padding);

    /*
     * If the customExists flag was set in the commitPage() method
     * draw the custom UI, else draw the default.
     */

    if (studioInterp.customExists()) {
	paintCustomPanel();
    } else {
	paintDefaultPanel();
    }

}

/*
 *----------------------------------------------------------------------
 *
 * paintCustomPanel --
 *
 *	Draw the custom UI.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The custom script is evaled in the customInterp.
 *
 *----------------------------------------------------------------------
 */

void
paintCustomPanel()
{
    /*
     * Eval the custom script which will create the custom UI within the 
     * customPanel.
     */

    try {
	studioInterp.evalCustom();
    } catch (TclException e) {
	studioInterp.invalidateCustom();
	return;
    }

    /*
     * Custom Panel
     */

    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth  = GridBagConstraints.REMAINDER;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbl.setConstraints(studioInterp.getCustomPanel(), gbc);
    add(studioInterp.getCustomPanel());

    /*
     * Set the flag that specifiess the custom panel is showing.  
     * Note: It does not work to call the isShowing() method because
     * there are situations in Java Studio where the Customizer is
     * destroied before all of the Customizer methods are called.
     */

    studioInterp.setCustomShowing(true);
}

/*
 *----------------------------------------------------------------------
 *
 * paintDefaultPanel --
 *
 *	If no customScript was specified, draw the default UI which
 *	consists of a label and a textArea containing the script 
 *	contents.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public void
paintDefaultPanel()
{
    scriptLabel = new Label("Script:");
    scriptText = new TextArea("", 10, 50);
    scriptText.setText(studioInterp.getScript());

    studioInterp.setCustomShowing(false);

    /*
     * Script Label
     */

    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth  = 1;
    gbc.gridheight = 1;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(scriptLabel, gbc);
    add(scriptLabel);

    /*
     * Script TextArea 
     */
    
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.gridwidth  = GridBagConstraints.REMAINDER;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbl.setConstraints(scriptText, gbc);
    add(scriptText);
}

/*
 *----------------------------------------------------------------------
 *
 * commitPage --
 *
 *	The commitPage method has three tasks.  
 *	1) If the custom panel is showing, set the exists flag to true
 *	   and eval the customCallback (this sets the return value for
 *	   calls to studio::getdata.)
 *	2) Eval the bean script.
 *	3) Remove old pins, reuse existing pins and make new pins.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The customInterp and interp may contain new state. 
 *
 *----------------------------------------------------------------------
 */

public void
commitPage() 
{
    int length = 0;
    Interp interp;
    Vector newVec = null;
    StudioTclPort oldPort, diePort, newPort, indexPort;

    try {

	/*
	 * Store the current bean script in the studioInterp.
	 */
	if (beanScript != null) {
	    studioInterp.setScript(beanScript);
	}

	/*
	 * Reset the interp - which deletes the old one and creates
	 * a new one.  Update any place that might have cached the value.
	 * We also reload the utility script at this point.
	 */

	interp = studioInterp.resetInterp();
 
	/*
	 * If a customScript exists, then eval the callback to update
	 * the value of customData.  Set the customExist flag to true 
	 * so the initPage method knows to draw the custom panel.
	 */

	if (studioInterp.customShowing()) {
	    studioInterp.evalCustomCallback(interp);
	    studioInterp.setCustomExists(true);
	} else {
	    studioInterp.setCustomExists(false);
	    studioInterp.setScript(scriptText.getText());
	}

   
	/*
	 * Source the bean script.
	 */
	
	try {
	    interp.eval(studioInterp.getScript(), TCL.GLOBAL_ONLY);
	} catch (TclException e) {
	    StudioTclInterp.printError(interp.getResult());
	    return;
	}

	/*
	 * If pendingVec is null, that means no new pins are to be created.
	 * All the rest of the work is to delete existing pins.
	 */

	if (studioInterp.pendingVec == null) {
	    newVec = new Vector();
	} else {
	    newVec = studioInterp.pendingVec;
	    studioInterp.pendingVec = null;
	}

	/*
	 * If reuseVec is not null, then there are pins on the pendingVec
	 * list that already exist.  Remove these pins from the 
	 * currentVec (a list of all existing pins).  The remaining
	 * pins on currentVec is a list of pins to be removed.
	 */

	if (studioInterp.reuseVec != null) {
	    for (Enumeration e = studioInterp.reuseVec.elements();
	            e.hasMoreElements();) {
		oldPort = (StudioTclPort)e.nextElement();
		studioInterp.currentVec.removeElement(oldPort);
	    }
	    studioInterp.reuseVec = null;
	}

	if (studioInterp.currentVec != null) {
	    try {
		for (Enumeration e = studioInterp.currentVec.elements();
		        e.hasMoreElements();) {
		    diePort = (StudioTclPort)e.nextElement();
		    studioInterp.removeDynamicPort(diePort.port);
		}
	    } catch (Exception e) {
		StudioTclInterp.printError("   remove error" + e);
	    }
	}
	
	/*
	 * Go through the newVec list adding new pins.  If the port
	 * already exists then newPort.added is true.
	 */

	for (Enumeration e = newVec.elements(); e.hasMoreElements();) {
	    newPort = (StudioTclPort)e.nextElement();
	    if (newPort == null) {
		StudioTclInterp.printError("   damn");
	    }
	    if (newPort.added) {
		continue;
	    }
	    if (newPort.port == null) {
		StudioTclInterp.printError("   really!!");
	    }
	    studioInterp.addDynamicPort(newPort.port);
	    newPort.added = true;
	}

	studioInterp.currentVec = newVec;
	
	/*
	 * Create an array look up table to match the port index to our
	 * port object.  This also set the index slot in the object.  We
	 * also bind each port to the interp at this time.
	 */
	
	int max = -1;
	for (Enumeration e = studioInterp.currentVec.elements();
	        e.hasMoreElements(); ) {
	    indexPort = (StudioTclPort)e.nextElement();
	    indexPort.initWithInterp(interp);
	    int index = indexPort.port.getIndex();
	    if (index > max) {
		max = index;
	    }
	}
	
	studioInterp.indexLookup = new StudioTclPort[max + 1];
	for (Enumeration e = studioInterp.currentVec.elements();
	        e.hasMoreElements();) {
	    indexPort = (StudioTclPort)e.nextElement();
	    studioInterp.indexLookup[indexPort.port.getIndex()] = indexPort;
	}
		
    } catch (Exception e) {
	StudioTclInterp.printError(e);
	e.printStackTrace();
    }
}

/*
 *----------------------------------------------------------------------
 *
 * loadScriptFile --
 *
 *	The loadScriptFile method has three tasks:
 *	1) Read the file denoted by the contents of the scriptInput
 *	   widget.  The filename may specify a file on the system
 *	   or as a resource in the jar file.
 *	2) Parce the script into two separate scripts; the customScript
 *	   and the beanScript.  Either may be null.
 *	3) Remove the existing UI and call methods to create the new UI.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The value of beanScript is set.
 *
 *----------------------------------------------------------------------
 */

public void
loadScriptFile()
{
    String src;
    String customScript = null;
    File sourceFile;
    FileInputStream fs;
    InputStream in = null;
    int index;

    beanScript = null;

    /*
     * If the file is not an absolute path, then see if it is 
     * a resource.  Set 'in' to be the InputStream to read from.
     */ 

    src = scriptInput.getText();
    sourceFile = new File(src);
    if (!sourceFile.isAbsolute()) {
      URL url = null;
      try {
	  url = getClass().getResource("scripts/" + src);
	  in = url.openStream();
      } catch (Exception e) {
      }
    }

    /*
     * If 'in' is null, then either the sourceFile is absolute or the 
     * src was not a valid Resource.  Try to open as a file and set 
     * 'in' to be the InputStream to read from.
     */

    if (in == null) {
	try {
	    fs = new FileInputStream(sourceFile);
	    in = (InputStream)fs;
	} catch (FileNotFoundException e) {
	} catch (SecurityException e) {
	}
    }

    /*
     * If 'in' is not null, then read the data and set the script string
     * to the contents.  Otherwise nothing could be found, set the script
     * to the appropriate error message.
     */

    if (in != null) {
	try {
	    int available, read;
	    
	    available = in.available();
	    read = 0;
		
	    byte charArray[] = new byte[available];
	    read = in.read(charArray);
	    while (read < available) {
		read += in.read(charArray, read, (available - read));
	    }
	    beanScript = new String(charArray);
	} catch (IOException e) {
	} finally {
	    try {
		in.close();
	    } catch (IOException e) {
	    }
	}
    }
    if (beanScript == null) {
	beanScript = "script file not found...";
    }

    /*
     * Determine if this is a special Tcl script that will take part
     * in the customization procedure.
     */
    
    if (beanScript.startsWith("#CUSTOM_BEGIN")) {
	index = beanScript.indexOf("#CUSTOM_END");
	if (index < 0) {
	    customScript = beanScript;
	    beanScript = "";
	} else {
	    customScript = beanScript.substring(0, index);
	    beanScript = beanScript.substring(index);
	}
    }

    repaintCustomizer(customScript, beanScript);
}

/*
 *----------------------------------------------------------------------
 *
 * repaintCustomizer --
 *
 *	|>description<|
 *
 * Results:
 *	|>None.<|
 *
 * Side effects:
 *	|>None.<|
 *
 *----------------------------------------------------------------------
 */

void
repaintCustomizer(
    String customScript,
    String beanScript
)
{
    /*
     * Remove the currently displayed UI, so the new one can be drawn
     * in its place.
     */
    
    if (studioInterp.customShowing()) {
	remove(studioInterp.getCustomPanel());
	studioInterp.invalidateCustom();
    } else {
	if (scriptLabel != null) {
	    remove(scriptLabel);
	}
	if (scriptText != null) {
	    remove(scriptText);
	}
    }

    /*
     * If a custom script was parced from the original file, initialize
     * the custom interp and draw the custom interface.  Otherwise draw
     * the default textArea and insert the contents of the script.
     */

    if (customScript != null) {
	studioInterp.initCustom(customScript);
	paintCustomPanel();
    } else {
	paintDefaultPanel();
	scriptText.setText(beanScript);
    }

    /*
     * This must be called to update the InterpPage's new UI.
     */

    validate();
}

/*
 *----------------------------------------------------------------------
 *
 * actionPerformed --
 *
 *	When action events are recieved, call the loadScriptFile()
 *	method to read the file specified int the scriptInput entry box.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public void 
actionPerformed(
    ActionEvent action) 
{
    loadScriptFile();
}

/*
 *----------------------------------------------------------------------
 *
 * showFileDialog --
 *
 *	Displays an AWT FileDialog
 *
 * Results:
 *	The selected file or and empty string;
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public void
showFileDialog()
{
    String dir;
    String file;
    FileDialog fd;
    
    fd = new FileDialog(new Frame(), "Select a script file.");
    if (lastDirName != null) {
	fd.setDirectory(lastDirName);
    }
    fd.show();
        
    /*
     * Pull out the dir and filename from the fileDialog object
     */
    
    dir  = fd.getDirectory();
    file = fd.getFile();
    
    /*
     * If a dir and filename are not null then a valid path
     * been choosen (i.e. The Cancel Button wasnt pressed).
     * Also set lastDirName to the current dir so the next time
     * the fd appears the entire path dosen't need to be reselected.
     */
    
    if ((dir != null) && (file != null) && (!file.equals(""))) {
	lastDirName = dir;
	scriptInput.setText(dir + file);
	loadScriptFile();
    }
}
} // end VJInterpPage


/*
 *----------------------------------------------------------------------
 * VJInterpPage --
 *
 *	Implements the VJCustomizerPage for the script input.
 *
 *----------------------------------------------------------------------
 */


class VJInfoPage extends VJCustomizerPage
{

/*
 * The GIF that displays all the info.
 */

private static final String imageName = "images/logoMed.gif";

/*
 * Height and Width of the GIF.
 */

private static final int width = 460;
private static final int height = 270;

/*
 *----------------------------------------------------------------------
 *
 * VJInfoPage --
 *
 *	Creates an instance os VJCanvasImage, which is responisibe
 *	for loading and displaying imageName.
 *
 * Side effects:
 *	The image is loaded.
 *
 *----------------------------------------------------------------------
 */

public 
VJInfoPage() 
{
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.NORTH;

    setInsets(new Insets(10,10,10,10));
    setBackground(Color.white);
    setLayout(gbl);

    VJCanvasImage canImg = new VJCanvasImage(imageName, width, height);

    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.gridwidth  = 1;
    gbc.gridheight = 1;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(canImg, gbc);
    add(canImg);
}

/*
 *----------------------------------------------------------------------
 *
 * initPage --
 *
 *	Called when the TclBean is created or when the Graphical Bean
 *	is double clicked.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public void
initPage(
    Object obj)
{
    // Do nothing.
}

/*
 *----------------------------------------------------------------------
 *
 * initPage --
 *
 *	Called when the 'OK' or 'Apply' button is pressed in the 
 *	customizer window.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public void
commitPage()
{
    // Do nothing.
}
} // end VJInfoPage

/*
 *----------------------------------------------------------------------
 * VJCanvasImage --
 *
 *	Subclass the Canvas component and draw into it the info page
 *	text and graphics about the Tcl Bean.
 *
 *----------------------------------------------------------------------
 */

class VJCanvasImage extends Canvas
{

/*
 * The name of the image to extract from the Resource.
 */

private String imageName;

/*
 * The Image object of to display the Tcl logo.
 */

private Image img;

/*
 * Text to display who we are.
 */

private static final String what1 = new String(
    "The Tcl Bean is an implementation of the Tcl8.0");
private static final String what2 = new String(
    "scripting language created especially for Java Studio.");
private static final String who1 = new String(
    "The Tcl Bean is brought to you by");
private static final String who2 = new String(
    "Ray Johnson:  Project Lead");
private static final String who3 = new String("Scott Stanton");
private static final String who4 = new String("Melissa Herschl");
private static final String who5 = new String("Ioi Lam");
private static final String who6 = new String("Bryan Surles");
private static final String where1 = new String("http://www.scriptics.com/java");

/*
 *----------------------------------------------------------------------
 *
 * VJCanvasImage --
 *
 *	Set the size of the Canvas, and attempt to load the image.  If
 * 	the Peer object does not yet exist, img will be set to null.
 *
 * Side effects:
 *	Loads the image or sets it to null.
 *
 *----------------------------------------------------------------------
 */

public 
VJCanvasImage(
    String imageName,	// Filename of the logo.
    int width,		// Width of the display space necessary.
    int height) 	// Height of the display space necessary.
{
    setSize(width, height);
    setBackground(Color.white);
    this.imageName = imageName;
    loadImage();
}

/*
 *----------------------------------------------------------------------
 *
 * loadImage --
 *
 *	Attempts to create an Image object.  The call all to 
 *	imageCreate() may return null if the Peer object dosen't exist.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Loads the image or sets it to null.
 *
 *----------------------------------------------------------------------
 */

public void
loadImage()
{
    try {
	java.net.URL url = getClass().getResource(imageName);
	java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();
	img = tk.createImage((java.awt.image.ImageProducer)url.getContent());
    } catch (Exception e) {
	img = null;
    }
}

/*
 *----------------------------------------------------------------------
 *
 * paint --
 *
 *	Draws the image onto the canvas.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	If the image is null, the initial call to loadImage in the 
 *	constructor failed.  By now the peer should exist and the
 *	image will be loaded correctly.
 *
 *----------------------------------------------------------------------
 */

public void
paint(
    Graphics g) 
{
    int width;
    FontMetrics fm;
    Dimension size;

    if (img == null) {
	loadImage();
    }
    g.drawImage(img, 15, 70, this);
    g.setFont(new Font("Courier", Font.BOLD, 14));
    g.drawString(what1, 15, 30);
    g.drawString(what2, 15, 45);
    
    fm = g.getFontMetrics();
    width = fm.stringWidth(what2);

    g.setFont(new Font("Courier", Font.PLAIN, 12));
    g.drawString(who1, 180, 105);
    g.drawString(who2, 190, 125);
    g.drawString(who3, 190, 140);
    g.drawString(who4, 190, 155);
    g.drawString(who5, 190, 170);
    g.drawString(who6, 190, 185);
    g.drawString(where1, 180, 215);

    size = getSize();
    if (width > size.width) {
	size.width = width + 20;
	setSize(size);
    }
}
} // end VJCanvasImage
    

