# all.tcl --
#
# This file contains a top-level script to run all of the Tcl
# tests.  Execute it by invoking "source all.test" when running tcltest
# in this directory.
#
# Copyright (c) 1998-2000 Ajuba Solutions.
# All rights reserved.
# 
# RCS: @(#) $Id: all.tcl,v 1.1 2000/12/23 23:40:29 mdejong Exp $

if {[lsearch [namespace children] ::tcltest] == -1} {

    # Fake up the "tcltest" package with our own local copy
    # if we can't load it in the Tcl shell.

    if {[catch {package require tcltest}]} {
        source tcltest.tcl
    }

    namespace import -force ::tcltest::*

    # Load in some older helper procs
    source defs
}

# Set verbose to max
#set ::tcltest::verbose pb


set ::tcltest::testSingleFile false
set ::tcltest::testsDirectory [file dir [info script]]

# We need to ensure that the testsDirectory is absolute
::tcltest::normalizePath ::tcltest::testsDirectory

puts stdout "Tcl $tcl_patchLevel tests running in interp:  [info nameofexecutable]"
puts stdout "Tests running in working dir:  $::tcltest::testsDirectory"
if {[llength $::tcltest::skip] > 0} {
    puts stdout "Skipping tests that match:  $::tcltest::skip"
}
if {[llength $::tcltest::match] > 0} {
    puts stdout "Only running tests that match:  $::tcltest::match"
}

if {[llength $::tcltest::skipFiles] > 0} {
    puts stdout "Skipping test files that match:  $::tcltest::skipFiles"
}
if {[llength $::tcltest::matchFiles] > 0} {
    puts stdout "Only sourcing test files that match:  $::tcltest::matchFiles"
}

set timeCmd {clock format [clock seconds]}
puts stdout "Tests began at [eval $timeCmd]"


# source each of the specified tests
#foreach file [lsort [::tcltest::getMatchingFiles]] {
#    set tail [file tail $file]
#    puts stdout $tail
#    if {[catch {source $file} msg]} {
#	puts stdout $msg
#    }
#}


if {$tcl_platform(platform) == "java"} {
    # run the Jacl tests

    eval lappend tests [glob -nocomplain tcljava/*.test \
	jacl/*.test inprogress/*.test tcl/*.test]

} elseif {[info exists env(TCLBLEND_RUN_ALL_TESTS)]} {
    # run the Tcl Blend tests

    set tests [glob -nocomplain tcljava/*.test \
	inprogress/*.test tclblend/*.test tcl/*.test ]
} else {
    # run the Tcl Blend tests

    set tests [glob -nocomplain tcljava/*.test \
	tclblend/*.test ]
}

foreach i $tests {

    if {[string match l.*.test [file tail $i]]} {
	# This is an SCCS lock file;  ignore it.
	continue
    }

    puts stdout $i
    flush stdout

    if {[catch {source $i} msg]} {
	puts $msg
    }

}



# cleanup
puts stdout "\nTests ended at [eval $timeCmd]"
::tcltest::cleanupTests 1
return

