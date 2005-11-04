# all.tcl --
#
# This file contains a top-level script to run all of the Tcl
# tests.  Execute it by invoking "source all.test" when running tcltest
# in this directory.
#
# Copyright (c) 1998-2000 Ajuba Solutions.
# All rights reserved.
# 
# RCS: @(#) $Id: all.tcl,v 1.7 2005/11/04 21:02:14 mdejong Exp $

if {[lsearch [namespace children] ::tcltest] == -1} {
    package require tcltest
    namespace import -force ::tcltest::*
}

set ::tcltest::testSingleFile false
set ::tcltest::testsDirectory [file dir [info script]]

# We need to ensure that the testsDirectory is absolute
::tcltest::normalizePath ::tcltest::testsDirectory

# Implement helper proc that will return the relative directory
# in comparison to the original $::tcltest::testsDirectory.
# This works like the proc in the newer TclTest module.

proc tcltest::testsDirectory { {dir {}} } {
    if {$dir == "INIT"} {
        # Init vars
        set ::tcltest::originalTestsDirectory $::tcltest::testsDirectory
        set ::tcltest::currentTestsDirectory $::tcltest::testsDirectory
    } elseif {$dir == "RESET"} {
        # Release changed dir
        set ::tcltest::currentTestsDirectory $::tcltest::originalTestsDirectory
    } elseif {$dir == {}} {
        # Query current dir
        return $::tcltest::currentTestsDirectory
    } else {
        # Set new dir
        set ::tcltest::currentTestsDirectory $dir
    }
}

tcltest::testsDirectory INIT


# Load in helper procs
if {[info commands setupJavaPackage] == {}} {
    source [tcltest::testsDirectory]/defs
}

# Set verbose to max
if {0} {
    if {$tcl_platform(platform) == "java"} {
        set tcltest::verbose pb
    } else {
        configure -verbose pb
    }
}


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

cd [tcltest::testsDirectory]

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

cd $env(BUILD_DIR)

foreach i $tests {

    if {[string match l.*.test [file tail $i]]} {
	# This is an SCCS lock file;  ignore it.
	continue
    }

    puts stdout $i
    flush stdout

    # Get tcltest::testsDirectory to report the directory that
    # a .test file is being sourced from.
    tcltest::testsDirectory RESET
    #puts "check $i"
    #puts "testsDirectory before change: [tcltest::testsDirectory]"
    set dname [file dirname $i]
    if {$dname != "."} {
        tcltest::testsDirectory [file join [tcltest::testsDirectory] [file dirname $i]]
    }
    #puts "testsDirectory after change: [tcltest::testsDirectory]"
    #puts "current directory after change: [pwd]"

    set fname [file tail $i]
    set ffname [file join [tcltest::testsDirectory] $fname]
    #puts "source: \"$ffname\""
    if {[catch {source $ffname} msg]} {
	puts $msg
    }

}



# cleanup
puts stdout "\nTests ended at [eval $timeCmd]"
::tcltest::cleanupTests 1
return

