#CUSTOM_BEGIN
# customSwitch.tcl --
#
# This script allows you to create a switch bean that works just like
# the Switch bean bundled with Java Studio.  As well, the Customizer
# interface is generated in Tcl between the CUSTOM_BEGIN and CUSTOM_END
# lines.
#
# RCS: @(#) $Id: customSwitch.tcl,v 1.1 1998/10/14 21:09:17 cvsadmin Exp $
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

    # Create the callback to set the data in the Studio Bean

    studio::custom commitScript {
	if [info exists pinList] {
	    set x $pinList
	    return $x
	}
	return ""
    }

    set pinList [studio::custom getBeanData]
    createPage [studio::custom getPanel]
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
    global valueText
    global sideChoice
    global currentList

    set gbl [java::new java.awt.GridBagLayout]
    set gbc [java::new java.awt.GridBagConstraints]
    $panel setLayout $gbl

    set widget [java::new java.awt.Label "New value to compare:"]
    set gbc [setGrid $gbc 0 0 -1 -1 1.0 -1 WEST -1]
    addComponent $gbl $gbc $panel $widget

    set widget [java::new java.awt.Label "Location:"]
    set gbc [setGrid $gbc 1 0 -1 -1 -1 -1 WEST -1]
    addComponent $gbl $gbc $panel $widget

    set valueText [java::new {java.awt.TextField java.lang.String} "Value 1"]
    set gbc [setGrid $gbc 0 1 RELATIVE -1 1.0 -1 WEST HORIZONTAL]
    addComponent $gbl $gbc $panel $valueText

    set sideChoice [java::new java.awt.Choice]
    $sideChoice {add java.lang.String} Top
    $sideChoice {add java.lang.String} Bottom
    $sideChoice {add java.lang.String} Left
    $sideChoice {add java.lang.String} Right
    $sideChoice {select java.lang.String} Right
    set gbc [setGrid $gbc 1 1 -1 -1 -1 -1 WEST -1]
    addComponent $gbl $gbc $panel $sideChoice

    set buttonPanel [java::new java.awt.Panel]
    set addButton [java::new java.awt.Button "Add"]
    set remButton [java::new java.awt.Button "Remove"]
    $buttonPanel {add java.awt.Component} $addButton
    $buttonPanel {add java.awt.Component} $remButton
    set gbc [setGrid $gbc 0 2 REMAINDER -1 1.0 -1 CENTER -1]
    addComponent $gbl $gbc $panel $buttonPanel

    set widget [java::new java.awt.Label "Current Values:"]
    set gbc [setGrid $gbc 0 3 REMAINDER -1 1.0 -1 WEST -1]
    addComponent $gbl $gbc $panel $widget

    set currentList [java::new java.awt.List 5]
    set gbc [setGrid $gbc 0 4 REMAINDER REMAINDER 1.0 1.0 -1 BOTH]
    addComponent $gbl $gbc $panel $currentList

    if [info exists pinList] {
	foreach i $pinList {
	    $currentList {add java.lang.String} [lindex $i 0]
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
#	The string in the valueText TextField is added to the 
#	currentList List component.  The string in valueText, 
#	the selected string in the Choice box and a translation
#	to a directional value are added to the pinList.  Each
# 	element in the pinList specifies a pin to create.

proc addSwitch {} {
    global pinList 
    global valueText
    global sideChoice
    global currentList

    set text [$valueText getText]
    set side [$sideChoice getSelectedItem]  
    switch $side {
	Top    {set loc north}
	Bottom {set loc south}
	Left   {set loc west}
	Right  {set loc east}
    }
    $currentList {add java.lang.String} $text
    lappend pinList [list $text $side $loc]
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
    $currentList {remove int} $index
    set pinList [lreplace $pinList $index $index]
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
    if {$index >= 0} {
	set side  [lindex [lindex $pinList $index] 1]
	$sideChoice {select java.lang.String} $side
    }
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
# Call the proc to execute the  script.
#

customMain

#CUSTOM_END

# beanMain --
#
#  	Create an input pin to retireve the message and trigger the 
#	callback that executes the switch function.  Always create a
# 	default output, then create any additional output pins based
#	on the content of the bean data.  The state array contains 
#	three values.  The passThrough index will turn on the ability
#	to fire the switch result when either the message or value index
# 	is true (1) or when both are true (0).  The message and value
#	indicies become true when a message is sent to the associated
#	pin.
#
# Arguments:
# 	None.
#
# Results:
# 	Pins and bindings are created.

proc beanMain {} {
    global state
    global default
    global switchOn
    global switchValue 
    global switchMessage 

    set state(passThrough) 0
    set state(message) 0
    set state(value) 0
    set switchOn [studio::custom getBeanData]

    # Create the value and message input pins.

    studio::port in switchValue -location north
    studio::port in switchMessage
    studio::bind switchValue {evalSwitch value}
    studio::bind switchMessage {evalSwitch message}

    # Create a port for each switch element retrieved from the 
    # Studio Bean's data.

    set pid 1
    foreach item $switchOn {
	set name [lindex $item 0]
	set loc  [lindex $item 2]
	studio::port out [concat $name $pid] \
	    -portname $name \
	    -location $loc \
	    -transfer stringToBasic
	incr pid
    }

    studio::port out default -transfer stringToBasic -location east
}

# evalSwitch --
#
# 	Send the message through the out pin the matches
#	the value variable.
#
# Arguments:
#	pin 	The in pin that was triggered.
#
# Results:
# 	Data is sent out a pin.

proc evalSwitch {pin} {
    global switchOn
    global state
    global switchValue 
    global switchMessage 
    global default

    if {$pin == "value"} {
	set state(value) 1
    } elseif {$pin == "message"} {
	set state(message) 1
    }

    if {$state(value) && $state(message)} {
	# Reset the input pins to their initial state.

	if {!$state(passThrough)} {
	    set state(value) 0
	    set state(message) 0
	}

	# Extract the value and message from the input pins

	set value [$switchValue toString]
	set message [$switchMessage toString]

	# Compare each output pins (the cases) with the value.
	# If you find a match send the message through this 
	# port, else send it through the default port.

	set pid 1
	foreach item $switchOn {
	    set switchCase [lindex $item 0]
	    if {[string compare $switchCase $value] == 0} {
		upvar #0 [concat $value $pid] output
		set output $message
		return
	    }
	    incr pid
	}
	set default $message
    }
}

#
# Call the proc to execute the bean script.
#

beanMain
