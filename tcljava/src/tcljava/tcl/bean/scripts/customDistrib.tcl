#CUSTOM_BEGIN
# customDistrib.tcl --
#
# This script allows you to create a distrib bean that works just like
# the Distrib bean bundled with Java Studio.  As well, the Customizer
# interface is generated in Tcl between the CUSTOM_BEGIN and CUSTOM_END
# lines.
#
# RCS: @(#) $Id: customDistrib.tcl,v 1.1 1998/10/14 21:09:17 cvsadmin Exp $
#
# Copyright (c) 1998 Sun Microsystems, Inc.
#
# See the file "license.terms" for information on usage and redistribution
# of this file, and for a DISCLAIMER OF ALL WARRANTIES.


# customMain --
#
#  	Initialize data and bindings and create the customizer page.
#
# Arguments:
# 	None.
#
# Results:
# 	The binding to commit is created and the custom panel
#	is populated with the UI.

proc customMain {} {
    global pinList

    set pinList [studio::custom getBeanData]
    createPage  [studio::custom getPanel]
    studio::custom commitScript {
	set x $pinList
	return $x
    }
}

# createPage --
#	
#	Create the custom page for the Switch component.
#	
# Arguments:
#	panel	The panel to draw the UI into.  Should be retrieved
#		by a call to studio::custom panel.
#
# Results:
#	The widgets are created as well as the bindings on their 
#	events.  If the global var pinList contains data then the 
#	customList is populated with this information.

proc createPage {panel} {
    global pinList
    global nameText
    global descText
    global sideChoice
    global currentList

    set gbl [java::new java.awt.GridBagLayout]
    set gbc [java::new java.awt.GridBagConstraints]
    java::field $gbc insets [java::new java.awt.Insets 1 1 1 1]
    $panel setLayout $gbl

    set widget [java::new java.awt.Label "Connector Name:"]
    set gbc [setGrid $gbc 0 0 -1 -1 0.0 -1 WEST -1]
    addComponent $gbl $gbc $panel $widget

    set widget [java::new java.awt.Label "Description:"]
    set gbc [setGrid $gbc 1 0 -1 -1 -1 -1 WEST -1]
    addComponent $gbl $gbc $panel $widget

    set widget [java::new java.awt.Label "Location:"]
    set gbc [setGrid $gbc 2 0 -1 -1 -1 -1 WEST -1]
    addComponent $gbl $gbc $panel $widget

    set nameText \
	    [java::new {java.awt.TextField java.lang.String} "New Connector"]
    $nameText setColumns 20
    set gbc [setGrid $gbc 0 1 -1 -1 -1 -1 WEST NONE]
    addComponent $gbl $gbc $panel $nameText

    set descText \
	    [java::new {java.awt.TextField java.lang.String} "Sends Message"]
    set gbc [setGrid $gbc 1 1 RELATIVE -1 1.0 -1 WEST HORIZONTAL]
    addComponent $gbl $gbc $panel $descText

    set sideChoice [java::new java.awt.Choice]
    $sideChoice {add java.lang.String} Top
    $sideChoice {add java.lang.String} Bottom
    $sideChoice {add java.lang.String} Left
    $sideChoice {add java.lang.String} Right
    $sideChoice {select java.lang.String} Right
    set gbc [setGrid $gbc 2 1 -1 -1 -1 -1 WEST -1]
    addComponent $gbl $gbc $panel $sideChoice

    set buttonPanel [java::new java.awt.Panel]
    set addButton [java::new java.awt.Button "Add"]
    set remButton [java::new java.awt.Button "Remove"]
    $buttonPanel {add java.awt.Component} $addButton
    $buttonPanel {add java.awt.Component} $remButton
    set gbc [setGrid $gbc 0 2 REMAINDER -1 1.0 -1 CENTER -1]
    addComponent $gbl $gbc $panel $buttonPanel

    set widget [java::new java.awt.Label "Current Dynamic Connectors:"]
    set gbc [setGrid $gbc 0 3 REMAINDER -1 1.0 -1 WEST -1]
    addComponent $gbl $gbc $panel $widget

    set currentList [java::new java.awt.List 5]
    set gbc [setGrid $gbc 0 4 REMAINDER REMAINDER 1.0 1.0 -1 BOTH]
    addComponent $gbl $gbc $panel $currentList

    if [info exists pinList] {
	foreach i $pinList {
	    $currentList {add java.lang.String} \
		"[lindex $i 0] : [lindex $i 1]"
	}
    }

    java::bind $addButton actionPerformed {addSwitch}
    java::bind $remButton actionPerformed {remSwitch}
    java::bind $currentList itemStateChanged {synchSelection}
}

# addSwitch --
# 
# 	This is the callback script for the addButton.  Add an
#	element to the customList.
# 
# Arguments:
#	None.
#
# Results:
#	The strings in the nameText/descText TextFields are added to 
#	the currentList List component.  The strings in nameText and 
#	descText components, the value of the Choice box and a 
#	translation to a directional value are added to the pinList.  
# 	Each element in the pinList specifies a pin to create.

proc addSwitch {} {
    global pinList 
    global nameText
    global descText
    global sideChoice
    global currentList

    set name [$nameText getText]
    set desc [$descText getText]
    set side [$sideChoice getSelectedItem]  
    switch $side {
	Top    {set loc north}
	Bottom {set loc south}
	Left   {set loc west}
	Right  {set loc east}
    }
    $currentList {add java.lang.String} "$name : $desc"
    lappend pinList [list $name $desc $side $loc]
}

# remSwitch --
# 
#	This is the callback script for the remButton.  Remove the
#	currently selected List item.
#
# Arguments:
#	None.
#
# Results:
#	The currently selected item is removed from the currentList
# 	component, and the associated data is removed from pinList.

proc remSwitch {} {
    global pinList
    global currentList

    set index [$currentList getSelectedIndex]
    if {$index >= 0} {
	$currentList {remove int} $index
	set pinList [lreplace $pinList $index $index]
    }
}

# synchSelection --
# 
#	This is the callback script for the currentList.  Synchronize
# 	the currently selected item in the currentList with the 
#	value in the sideChoice Choice component.
#
# Arguments:
#	None.
#
# Results:
#	The index of the newly selected list item also indexes
#	the data in pinList.  Extract the info on which Choice
#	selection is valid for this item.

proc synchSelection {} {
    global pinList 
    global sideChoice
    global currentList

    set index [$currentList getSelectedIndex]
    set side  [lindex [lindex $pinList $index] 2]
    $sideChoice {select java.lang.String} $side
}

# setGrid --
#
# 	Helper function to set the fields for a GridBagConstraint
# 	object.
#
# Arguments:
#	gbc	The GridBagConstraint object.
#	x	The column to place the component in.
#	y 	The row to place the component in.
#	width	How many coloumns to span.
#	height	How many rows to span.
#	weightx	Priority when resizing the window.
#	weighty	Priority when resizing the window.
#	anchor	Which side to stick to.
#	fill	How the component should expand as the grid expands.
#
# Results:
#	The gbc object is initialized to defaults if any of the 
#	values are -1, otherwise the use the specified value.

proc setGrid {gbc x y width height weightx weighty anchor fill} {
    # Set to the default value if any of the variables
    # are -1.

    if {$width == "REMAINDER" || $width == "RELATIVE"} {
	set width [java::field $gbc $width]
    } elseif {$width < 0} {
	set width 1
    }
    if {$height == "REMAINDER" || $height == "RELATIVE"} {
	set height [java::field $gbc $height]
    } elseif {$height < 0} {
	set height 1
    }
    if {$weightx < 0.0} {
	set weightx 0.0
    }
    if {$weighty < 0.0} {
	set weighty 0.0
    }
    if { $anchor < 0 } {
	set anchor CENTER
    }
    if { $fill < 0 } {
	set fill NONE
    }

    # Set the GridBagConstraint object to the current settings

    java::field $gbc gridx $x
    java::field $gbc gridy $y
    java::field $gbc gridwidth $width
    java::field $gbc gridheight $height
    java::field $gbc weightx $weightx
    java::field $gbc weighty $weighty
    java::field $gbc anchor [java::field $gbc $anchor]
    java::field $gbc fill [java::field $gbc $fill]

    return $gbc
}

# addComponent --
#
#	Given an initialized gbc, insert the component into the panel.
#
# Arguments:
#	gbl	The GridBagLayout object.
#	gb	The GridBagConstraint object.
#	panel	The Panel to put the component into.
#	widget	The component to be packed.
#
# Results:
#	The component is packed into the panel.

proc addComponent {gbl gbc panel widget} {
    $gbl setConstraints $widget $gbc
    $panel {add java.awt.Component} $widget
}

#
# Call the proc to execute the custom script.
#

customMain

#CUSTOM_END

# beanMain --
#
# 	This script allows you to create a simple merge & dirstribute 
#	bean.  Any data on an input pin will be sent to every output 
#	pin.  If there is only one output pin this acts like the 
#	Merger bean.  If there is only one input pin then this is 
#	just like the Distributer bean.  By default this is a 
#	distributor bean, with the names of the output pins being 
#	generated from the custom Customizer.  Alter the initializtion
#	of inputPins & outputPins in this procedure to change the 
#	behavior.
#
# Arguments:
# 	None.
#
# Results:
#	New pins are created and bindings on input pins are created. 	

proc beanMain {} {
    global inputPins
    global outputPins

    set inputPins {input}
    set outputPins [studio::custom getBeanData]

    set pid 1
    foreach name $inputPins {
	set var ${name}_${pid}
	studio::port in $var -portname $name -transfer dynamic 
	studio::bind $var "distribData $var"
	incr pid
    }

    set pid 1
    foreach x $outputPins {
	set name [lindex $x 0]
	set desc [lindex $x 1]
	set side [lindex $x 3]
	set var  ${name}_${pid}
	studio::port out $var \
	    -portname $name \
	    -location $side \
	    -transfer dynamic \
	    -description $desc
	incr pid
    }
}

# distribData --
#
#  	Callback for the binding on the input pin(s).  Sends
#	object to all output pins.
#
# Arguments:
# 	Name of the input pin that fired the callback.
#
# Results:
# 	None.

proc distribData {name} {
    global outputPins
    upvar #0 $name input

    set pid 1
    foreach x $outputPins {
	set var "[lindex $x 0]_${pid}"
        upvar #0 $var output
        set output $input
	incr pid
    }
}

#
# Call the proc to execute the bean script.
#

beanMain
