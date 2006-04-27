# SetInterrupted.tcl --
#
#	Utility Tcl procs loaded into an interp for testing
#	the Interp.setInterrupted() API.
#

#puts "loaded SetInterrupted.tcl"

proc setup {} {
    package require java
}


proc ti1_cmd1 {} {
    #puts "ti1_cmd1"
}

proc ti1_cmd2 {} {
    global loop

    while {$loop < 100} {
        ti1_cmd1

        if {$loop == 50} {
            [java::getinterp] setInterrupted
        }

        incr loop
    }
    set result END
}

proc ti2_cmd1 {} {
    #puts "ti2_cmd1"
}

proc ti2_cmd2 {} {
    global loop

    while {$loop < 100000} {
        ti2_cmd1
        incr loop
    }
    set result END
}

proc ti4_cmd1 {} {
    #puts "ti4_cmd1"
}

proc ti4_cmd2 {} {
    global loop

    while {$loop < 100000} {
        ti4_cmd1
        incr loop
    }
    set result END
}

proc ti4_cmd3 {} {
    set ::loop 0
    after 0 ti4_cmd2
}

proc ti5_cmd1 {} {
    #puts "ti5_cmd1"
}

proc ti5_cmd2 {} {
    global loop

    while {$loop < 1000} {
        ti5_cmd1

        if {$loop > 10} {
            # Let a few loops run before a vwait
            after 1 "set done 1"
            vwait done
        }

        incr loop
    }
    set result END
}

proc ti5_cmd3 {} {
    set ::loop 0
    after 0 ti5_cmd2
}

proc ti6_cmd1 {} {
    #puts "ti6_cmd1"
    vwait forever
    set result END
}

proc ti6_cmd2 {} {
    after 0 ti6_cmd1
}

proc ti7_cmd1 {} {
    #puts "ti7_cmd1"
}

proc ti7_cmd2 {} {
    global loop

    while {$loop < 100000} {
        ti7_cmd1
        incr loop
    }
    set result END
}

proc ti7_cmd3 {} {
    set ::loop 0
    after 0 ti7_cmd2
}


# Test series 2: two interps running these procs

proc ti20_cmd1 {} {
    global INUM
    #puts "ti20_cmd1 in $INUM"
}

proc ti20_cmd2 {} {
    global calls
    global nextAfter

    ti20_cmd1

    if {$calls < 1000} {
        # Queue up another event so that this
        # method is invoked again. This is needed
        # so that the outermost event loop is
        # entered and events from both interps
        # are processed.
        set nextAfter [after 50 ti20_cmd2]
    }

    incr calls
}

proc ti20_cmd3 { id } {
    set ::INUM $id
    set ::calls 0
    after 0 ti20_cmd2

    # Interrupt second thread after a few seconds. The
    # first thread should be interrupted right away.

    if {$id == "TWO"} {
         after [expr {2 * 1000}] {
             # Cancel after command that would invoke
             # ti20_cmd2 again. This makes sure that
             # there are no pending after events when
             # the interp is interrupted.
             after cancel $nextAfter
             [java::getinterp] setInterrupted
         }
    }
}

proc ti21_cmd1 {} {
    global INUM
    #puts "ti21_cmd1 in $INUM"
}

proc ti21_cmd2 {} {
    global calls
    global nextAfter

    ti21_cmd1

    if {$calls < 1000} {
        # Queue up another event so that this
        # method is invoked again. This is needed
        # so that the outermost event loop is
        # entered and events from both interps
        # are processed.
        set nextAfter [after 50 ti21_cmd2]
    }

    incr calls
}

proc ti21_cmd3 { id } {
    set ::INUM $id
    set ::calls 0
    after 0 ti21_cmd2

    # Queue up an event in the first thread. This
    # event should automatically be canceled when
    # the interp is interrupted.

    if {$id == "ONE"} {
        after [expr {1 * 1000}] {puts "processed after event in ONE"}
    }

    # Interrupt second thread after a few seconds. The
    # first thread should be interrupted right away.

    if {$id == "TWO"} {
         after [expr {2 * 1000}] {
             # Cancel after command that would invoke
             # ti20_cmd2 again. This makes sure that
             # there are no pending after events when
             # the interp is interrupted.
             after cancel $nextAfter
             [java::getinterp] setInterrupted
         }
    }
}

proc ti22_cmd1 {} {
    global INUM
    global results

    #puts "ti22_cmd1 in $INUM"

    if {[llength $results] == 0} {
        lappend results 1
    } elseif {[llength $results] == 1} {
        lappend results 2
    }
}

proc ti22_ONE_cmd2 {} {
    global calls
    global done

    #puts "ti22_ONE_cmd2"

    # Invoke ti22_cmd1 in a loop and
    # vwait so that events are processed
    # during each iteration.

    if {$calls < 1000} {

    for {set i 0} {$i < 10} {incr i} {
        #puts "ti22_ONE_cmd2 iteration $i"

        ti22_cmd1

        after 100 "set done 1"
        #puts "start vwait"
        vwait done
        #puts "done vwait"
    }

    }

    incr calls
}

proc ti22_TWO_cmd2 {} {
    global calls
    #puts "ti22_TWO_cmd2"

    ti22_cmd1

    if {$calls < 1000} {
        # Queue up another event so that this
        # method is invoked again. This is needed
        # so that the outermost event loop is
        # entered and events from both interps
        # are processed.

        after 10 ti22_TWO_cmd2
    }

    incr calls
}

proc ti22_cmd3 { id } {
    set ::INUM $id
    set ::calls 0
    set ::done 0
    set ::results [list]

    if {$id == "ONE"} {
        after 0 ti22_ONE_cmd2
        #puts "after info in ONE : [after info]"
    } elseif {$id == "TWO"} {
        after 0 ti22_TWO_cmd2
        #puts "after info in TWO : [after info]"

        after [expr {1 * 1000}] {
            [java::getinterp] setInterrupted
        }
    } else {
        error "unknown id $id"
    }
}

# Test 23 will interrupt the first interp
# and continue to process events in a
# second interp. The second interp
# will continue to execute until it
# is also interrupted.

proc ti23_ONE_cmd1 {} {
#    puts "ti23_ONE_cmd1"

    [java::getinterp] setInterrupted
}

proc ti23_TWO_cmd1 {} {
#    puts "ti23_TWO_cmd1"
}

proc ti23_ONE_cmd2 {} {
    global results

#    puts "ti23_ONE_cmd2"

    # Invoke ti23_ONE_cmd1, the command should interrupt itself.
    # When the interrupt is handled it should invoke additional
    # Tcl commands while cleaning up this stack frame. It is
    # not possible to catch a TclInterruptedException using
    # catch or java::try, but a finally block for a java::try
    # command will get run.

    java::try {
        lappend results "invoking"
        ti23_ONE_cmd1
    } finally {
        lappend results "finally"
    }
}

proc ti23_TWO_cmd2 {} {
    global results

#    puts "ti23_TWO_cmd2"

    # Invoke ti23_TWO_cmd1 ten times then interrupt this interp.
    # Invoke vwait in each loop so that we are sure events
    # continue to be processed.

    lappend results start
    for {set i 0} {$i < 10} {incr i} {
        after 100 "set done 1"
        ti23_TWO_cmd1
        vwait done
    }
    lappend results done
    [java::getinterp] setInterrupted
}

proc ti23_cmd3 { id } {
    set ::results [list]

    if {$id == "ONE"} {
        after 0 ti23_ONE_cmd2
    } elseif {$id == "TWO"} {
        after 0 ti23_TWO_cmd2
    } else {
        error "unknown id $id"
    }
}

# Test 24 will interrupt the first interp
# and continue to process events in the
# second interp. The first interp
# will unwind the stack and process
# multiple finally blocks during
# the interrupt cleanup.

proc ti24_ONE_cmd1 {} {
#    puts "ti24_ONE_cmd1"

    java::try {
        [java::getinterp] setInterrupted
    } catch {TclException e} {}
}

proc ti24_ONE_cmd2 {} {
#    puts "ti24_ONE_cmd2"

    java::try {
        ti24_ONE_cmd1
    } catch {Throwable e} {
#        puts "Throwable caught in ti24_ONE_cmd2"
        lappend ::results E2
    } finally {
#        puts "finally in ti24_ONE_cmd2"
        lappend ::results F2
    }
}

proc ti24_ONE_cmd3 {} {
#    puts "ti24_ONE_cmd3"

    java::try {
        ti24_ONE_cmd2
    } catch {Exception e} {
#        puts "Exception caught ti24_ONE_cmd3"
        lappend ::results E3
    } finally {
#        puts "finally in ti24_ONE_cmd3"
        lappend ::results F3
    }
}

proc ti24_ONE_cmd4 {} {
#    puts "ti24_ONE_cmd4"

    java::try {
        ti24_ONE_cmd3
    } catch {RuntimeException e} {
#        puts "RuntimeException caught in ti24_ONE_cmd4"
        lappend ::results E4
    } finally {
#        puts "finally in ti24_ONE_cmd4"
        lappend ::results F4
    }
}

proc ti24_TWO_cmd2 {} {
    lappend ::results start
    for {set i 0} {$i < 20} {incr i} {
        after 100 "set done 1"
#        puts "ti24_TWO_cmd2"
        vwait done
    }
    lappend ::results done
#    puts "now to interrupt in ti24_TWO_cmd2"
    [java::getinterp] setInterrupted
}

proc ti24_cmd5 { id } {
    set ::results [list]

    if {$id == "ONE"} {
        after 0 ti24_ONE_cmd4
    } elseif {$id == "TWO"} {
        after 0 ti24_TWO_cmd2
    } else {
        error "unknown id $id"
    }
}


# Test 25 will interrupt the first interp
# and continue to process events in the
# second interp. The first interp will
# attempt a catch around code that
# raises a TclInterruptedException.
# The catch should not be able to catch
# an TclInterruptedException. This test
# will also queue up a few after events
# in a finally block to make sure that
# these are canceled.

proc ti25_ONE_cmd1 {} {
    # Can't catch TclInterruptedException
    if {[catch {
        [java::getinterp] setInterrupted
    } err]} {
        # Should not reach this block
        lappend ::results catch
    }
}

proc ti25_ONE_cmd2 {} {
#    puts "ti25_ONE_cmd2"

    java::try {
        ti25_ONE_cmd1
    } finally {
        # Add a couple of events, these should be canceled
        lappend ::results finally

        after 1000 "puts BADOUT1"
        after 1500 "puts BADOUT2"
    }
}

proc ti25_TWO_cmd2 {} {
    lappend ::results start
    for {set i 0} {$i < 20} {incr i} {
        after 100 "set done 1"
#        puts "ti25_TWO_cmd2"
        vwait done
    }
    lappend ::results done
#    puts "now to interrupt in ti25_TWO_cmd2"
    [java::getinterp] setInterrupted
}

proc ti25_cmd3 { id } {
    set ::results [list]

    if {$id == "ONE"} {
        after 0 ti25_ONE_cmd2
    } elseif {$id == "TWO"} {
        after 0 ti25_TWO_cmd2
    } else {
        error "unknown id $id"
    }
}


# Test 26 will interrupt the first interp
# and continue to process events in the
# second interp. The first interp will
# use java::try to execute a finally
# block that raises an error. This error
# must be ignored since the original
# TclInterruptedException needs to
# be thrown again after the finally block
# is done.

proc ti26_ONE_cmd1 {} {
    [java::getinterp] setInterrupted
}

proc ti26_ONE_cmd2 {} {
#    puts "ti26_ONE_cmd2"

    java::try {
        ti26_ONE_cmd1
    } finally {
        lappend ::results finally
        # Raise error during finally block.
        # This error must be ignored so that
        # the original TclInterruptedException
        # is thrown after finally block.
        error "unknown"
    }
}

proc ti26_TWO_cmd2 {} {
    lappend ::results start
    for {set i 0} {$i < 20} {incr i} {
        after 100 "set done 1"
#        puts "ti26_TWO_cmd2"
        vwait done
    }
    lappend ::results done
#    puts "now to interrupt in ti26_TWO_cmd2"
    [java::getinterp] setInterrupted
}

proc ti26_cmd3 { id } {
    set ::results [list]

    if {$id == "ONE"} {
        after 0 ti26_ONE_cmd2
    } elseif {$id == "TWO"} {
        after 0 ti26_TWO_cmd2
    } else {
        error "unknown id $id"
    }
}

