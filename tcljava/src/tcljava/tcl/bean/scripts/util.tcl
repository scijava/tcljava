# util.tcl --
#
# Utility functions used by the Tcl Studio Component.  These functions are
# loaded and available in the interpreter before user code is run.
#
# RCS: @(#) $Id: util.tcl,v 1.1 1998/10/14 21:09:17 cvsadmin Exp $
#
# Copyright (c) 1997 Sun Microsystems, Inc.
#
# See the file "license.terms" for information on usage and redistribution
# of this file, and for a DISCLAIMER OF ALL WARRANTIES.

# studio::bind --
# This function binds a callback to an input variable (pin).  The 
# callback script is executed when the pin is set by another studio
# component.
#
# Arguments: 
# pinVar -			The Tcl variable (representing the pin)
#				that the script will be bound to.
# script -			The Tcl script to run which pin is set.

proc studio::bind {args} {    
    set len [llength $args]
    set var [lindex $args 0]

    if {($len != 1) && ($len != 2)} {
	error "wrong # args: should be \"studio::bind varName ?script?\""
    }

    # If there is only one argument, then return the script

    if {($len == 1) && ([info procs $var:inScript] != "")} {
	return [lindex [info body $var:inScript] 2]
    }

    # Parse the args list and make the var global.

    set script [lindex $args 1]
    global $var

    # If there is already a trace on a variable, remove it.

    if {[trace vinfo $var] != ""} {
	trace vdelete $var w $var:inScript
    }

    # Now bind the trace command in the global frame and define
    # the proc to be evaluated when var is written to.  Note:
    # this must also be done in the global frame because of
    # namespace issues.

    uplevel #0 trace variable $var w $var:inScript
    set code [list proc $var:inScript {name other op} "uplevel #0 {$script}"]
    uplevel #0 $code
}
