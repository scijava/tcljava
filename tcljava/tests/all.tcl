# all.tcl --
#
# This file contains a top-level script to run all of the Tcl
# tests.  Execute it by invoking "source all.test" when running tcltest
# in this directory.
#
# Copyright (c) 1998-2000 Ajuba Solutions.
# All rights reserved.
# 
# RCS: @(#) $Id: all.tcl,v 1.6 2005/10/20 21:35:55 mdejong Exp $

if {[lsearch [namespace children] ::tcltest] == -1} {
    package require tcltest
    namespace import -force ::tcltest::*
}
# Load in helper procs
if {[info commands setupJavaPackage] == {}} {
    source defs
}

# Set verbose to max
if {0} {
    if {$tcl_platform(platform) == "java"} {
        set tcltest::verbose pb
    } else {
        configure -verbose pb
    }
}

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

    set tests [glob -nocomplain tcljava/*.test \
	jacl/*.test inprogress/*.test tcl/*.test]
    set tests [lsort -dictionary $tests]

    foreach test [lsort -dictionary [glob -nocomplain itcl/*.test]] {
        lappend tests $test
    }
    foreach test [lsort -dictionary [glob -nocomplain tclparser/*.test]] {
        lappend tests $test
    }
} elseif {[info exists env(TCLBLEND_RUN_ALL_TESTS)]} {
    # run the Tcl Blend tests

    set tests [glob -nocomplain tcljava/*.test \
	inprogress/*.test tclblend/*.test tcl/*.test]
    set tests [lsort -dictionary $tests]
} else {
    # run the Tcl Blend tests

    set tests [glob -nocomplain tcljava/*.test \
	tclblend/*.test ]
    set tests [lsort -dictionary $tests]
}


# Run only parser tests
if {0} {
    if {$tcl_platform(platform) == "java"} {
        set tcltest::verbose pb
    } else {
        configure -verbose pb
    }

    set tests [list]
    foreach test [lsort -dictionary [glob -nocomplain tclparser/*.test]] {
        lappend tests $test
    }
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

