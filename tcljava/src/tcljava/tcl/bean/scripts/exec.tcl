# exec.tcl --
#
# This script demonstrates the exec command built into Tcl.  The 
# UNIX commands are executed by the Tcl interpreter, and the 
# result is returned.
#
# RCS: @(#) $Id: exec.tcl,v 1.1 1998/10/14 21:09:17 cvsadmin Exp $
#
# Copyright (c) 1998 Sun Microsystems, Inc.
#
# See the file "license.terms" for information on usage and redistribution
# of this file, and for a DISCLAIMER OF ALL WARRANTIES.


# Create the triggers that fire the corresponding exec event.

studio::port in ls -transfer trigger -location north
studio::port in cat -transfer trigger -location north

# Create the In ports that read the args to the above commands.

studio::port in dirName -transfer basicToString
studio::port in fileName -transfer basicToString

# The result port sends the result of the exec.  The clear port
# is used to notify a component the text is the coming.  It is
# used, for example, to clear any existing text before the new
# data arrives.

studio::port out result
studio::port out clear

# All In port events are bound to the same script, but each 
# passes a string telling the proc who the caller is.

studio::bind ls {execProc ls}
studio::bind cat {execProc cat}
studio::bind dirName {execProc dirName}
studio::bind fileName {execProc fileName}

# execProc --
#
#  	Store data on the current dir and file, and execute
#	the command if there is enough information
#
# Arguments:
# 	Type	Who called this proc.
#
# Results:
# 	None.

proc execProc {type} {
    global state
    global dirName
    global fileName
    global result
    global clear
    global label

    switch $type {
	ls {
	    if [info exists state(dirName)] {
		set clear 1
		set result [exec ls $state(dirName)]
	    }
	}
	cat {
	    if [info exists state(dirName)] {
		if [info exists state(fileName)] {
		    set clear 1
		    set result [exec cat [file join $state(dirName) $state(fileName)]]
		} 
	    }
	}
	dirName {
	    set state(dirName) [$dirName toString]
	}
	fileName {
	    set state(fileName) [$fileName toString]
	}
    }
}
