# switch.tcl --
#
# This script allows you to create a switch bean that works just like
# the Switch bean bundled with Java Studio.
#
# RCS: @(#) $Id: switch.tcl,v 1.1 1998/10/14 21:09:17 cvsadmin Exp $
#
# Copyright (c) 1998 Sun Microsystems, Inc.
#
# See the file "license.terms" for information on usage and redistribution
# of this file, and for a DISCLAIMER OF ALL WARRANTIES.

set switchOn {value1 value2 value3}
set state(passThrough) 1

# You shouldn't need to change anything below this line

set state(value) 0
set state(message) 0

foreach x $switchOn {
    studio::port out $x -location east -transfer stringToBasic
}
studio::port out default -transfer stringToBasic -location east
studio::port in switchValue -location north
studio::port in switchMessage

studio::bind switchValue {evalSwitch value}
studio::bind switchMessage {evalSwitch message}

proc evalSwitch {pin} {
    global switchOn state switchValue switchMessage default

    if {$pin == "value"} {
	set state(value) 1
    } elseif {$pin == "message"} {
	set state(message) 1
    }
    
    if {$state(value) && $state(message)} {

	set value [$switchValue toString]
	set message [$switchMessage toString]
	if {[lsearch -exact $switchOn $value] >= 0} {
	    upvar #0 $value output
	    set output $message
	} else {
	    set default $message
	}
	if {!$state(passThrough)} {
	    set state(value) 0
	    set state(message) 0
	}
    }
}
