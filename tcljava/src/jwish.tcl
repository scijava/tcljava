#!/bin/sh
# the	next line restarts using wish \
 exec wish "$0" "$@"

# It is unlikely that the lines above started wish up with the proper
# CLASSPATH and LD_LIBRARY_PATH.  See the ../unix/jwish.in script
# for more information.

# This script starts up a Tcl/Tk wish that uses the tclBlend Java interface.
# the jwish script passes this script to wish. 

# Author:  Christopher Hylands
# RCS: @(#) $Id: jwish.tcl,v 1.1 1998/10/14 21:09:10 cvsadmin Exp $
#
# Copyright (c) 1997-1998 The Regents of the University of California.
# 	All Rights Reserved.
#
# Permission is hereby granted, without written agreement and without
# license or royalty fees, to use, copy, modify, and distribute this
# software and its documentation for any purpose, provided that the
# above copyright notice and the following two paragraphs appear in all
# copies of this software.
# 
# IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
# FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
# ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
# THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
# SUCH DAMAGE.
# 
# THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
# INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
# PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
# CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
# ENHANCEMENTS, OR MODIFICATIONS.
# 
# 						PT_COPYRIGHT_VERSION_2
# 						COPYRIGHTENDKEY

if {$argc == 0} {
    error "You must call jwish.tcl like: wish jwish.tcl"
}


package require java

# Return the Java stack trace from the Exception.
proc getJavaStackTrace {exception} {
    set stream [java::new java.io.ByteArrayOutputStream]
    set printWriter [java::new \
	    {java.io.PrintWriter java.io.OutputStream} $stream]
    $exception {printStackTrace java.io.PrintWriter} $printWriter
    $printWriter flush
    return [$stream toString]
}


# Set the javaErrorInfo global variable with a stack trace
# from the exception in the errorCode variable
proc setJavaErrorInfo {} {
    global javaErrorInfo errorCode
    if {"[lindex $errorCode 0]" == "JAVA"} {
	set javaErrorInfo [getJavaStackTrace [lindex $errorCode 1]]
    } else {
	set javaErrorInfo ""
    }
}

# Global variable that contains the Java stack trace if we call Tcl Blend
# code that throws a Java Exception.

set javaErrorInfo ""

if { $argc > 1 } {
    # The user called jwish or jtclsh with some args, so we source
    # the first one.
    source [lindex $argv 1]
} else {
    if [info exists tk_version] {
	set tcl_prompt1 "jwish% "
    } else {
	set tcl_prompt1 "jtclsh% "
    }

    # Source the RC file.
    # Under Unix: ~/.tclshrc or ~/.wishrc
    if [file exists [glob -nocomplain $tcl_rcFileName]] {
	source [glob $tcl_rcFileName]
    }

    # This is a really lame effort to implement a prompt.
    # We should be using tkCon or something. 

    set _tychoTtyCommand ""
    while 1 {
	puts -nonewline $tcl_prompt1
	flush stdout
	set _tychoTtyCommand [gets stdin]
	while { ![info complete $_tychoTtyCommand] \
		|| [regexp {\\$} $_tychoTtyCommand] } {
	    if { [regexp {\\$} $_tychoTtyCommand] } {
		set _tychoTtyCommand \
			[string range $_tychoTtyCommand 0 [expr \
			[string length $_tychoTtyCommand] -2]]
	    } else {
		append _tychoTtyCommand \n
	    }
	    puts -nonewline "> "
	    flush stdout
	    append _tychoTtyCommand [gets stdin]
	}
	if [catch {puts [eval $_tychoTtyCommand]} msg] {
	    # puts stderr $msg
	    puts stderr $errorInfo

	    # If this is a Java error, then get the Java stack trace
	    if {"[lindex $errorCode 0]" == "JAVA"} {
		# Set the global javaErrorInfo with a stack trace
		setJavaErrorInfo
		puts stderr $javaErrorInfo
	    }
	}
    }
}

