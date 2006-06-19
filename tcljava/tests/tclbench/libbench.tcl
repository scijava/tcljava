#
# libbench.tcl <testPattern> <interp> <outChannel> <benchFile1> ?...?
#
# This file has to have code that works in any version of Tcl that
# the user would want to benchmark.
#
# RCS: @(#) $Id: libbench.tcl,v 1.9 2006/06/19 02:54:27 mdejong Exp $
#
# Copyright (c) 2000-2001 Jeffrey Hobbs.

# We will put our data into these named globals
global BENCH bench

#
# We claim all procedures starting with bench*
#

# bench_tmpfile --
#
#   Return a temp file name that can be modified at will
#
# Arguments:
#   None
#
# Results:
#   Returns file name
#
proc bench_tmpfile {} {
    global tcl_platform env BENCH
    if {![info exists BENCH(uniqid)]} { set BENCH(uniqid) 0 }
    set base "tclbench[incr BENCH(uniqid)].dat"
    if {[info exists tcl_platform(platform)]} {
	if {$tcl_platform(platform) == "unix"} {
	    return "/tmp/$base"
	} elseif {$tcl_platform(platform) == "windows" ||
            (0&& $tcl_platform(platform) == "java" &&
            $tcl_platform(host_platform) == "windows")} {
	    return [file join $env(TEMP) $base]
	} else {
	    return $base
	}
    } else {
	# The Good Ol' Days (?) when only Unix support existed
	return "/tmp/$base"
    }
}

# bench_rm --
#
#   Remove a file silently (no complaining)
#
# Arguments:
#   args	Files to delete
#
# Results:
#   Returns nothing
#
proc bench_rm {args} {
    foreach file $args {
	if {[info tclversion] > 7.4} {
	    catch {file delete $file}
	} else {
	    catch {exec /bin/rm $file}
	}
    }
}

# bench --
#
#   Main bench procedure.
#   The bench test is expected to exit cleanly.  If an error occurs,
#   it will be thrown all the way up.  A bench proc may return the
#   special code 666, which says take the string as the bench value.
#   This is usually used for N/A feature situations.
#
# Arguments:
#
#   -pre	script to run before main timed body
#   -body	script to run as main timed body
#   -post	script to run after main timed body
#   -desc	message text
#   -iterations	<#>
#
# Results:
#
#   Returns nothing
#
# Side effects:
#
#   Sets up data in bench global array
#
proc bench {args} {
    global BENCH bench errorInfo errorCode

    # -pre script
    # -body script
    # -desc msg
    # -post script
    # -iterations <#>
    array set opts {
	-pre	{}
	-body	{}
	-desc	{}
	-post	{}
    }
    set opts(-iter) $BENCH(ITERS)
    while {[llength $args]} {
	set key [lindex $args 0]
	switch -glob -- $key {
	    -res*	{ set opts(-res)  [lindex $args 1] }
	    -pr*	{ set opts(-pre)  [lindex $args 1] }
	    -po*	{ set opts(-post) [lindex $args 1] }
	    -bo*	{ set opts(-body) [lindex $args 1] }
	    -de*	{ set opts(-desc) [lindex $args 1] }
	    -it*	{
		# Only change the iterations when it is smaller than
		# the requested default
		set val [lindex $args 1]
		if {$opts(-iter) > $val} { set opts(-iter) $val }
	    }
	    default {
		error "unknown option $key"
	    }
	}
	set args [lreplace $args 0 1]
    }
    if {($BENCH(MATCH) != "") && ![string match $BENCH(MATCH) $opts(-desc)]} {
	return
    }
    if {($BENCH(RMATCH) != "") && ![regexp $BENCH(RMATCH) $opts(-desc)]} {
	return
    }
    if {$opts(-pre) != ""} {
	uplevel \#0 $opts(-pre)
    }
    if {$opts(-body) != ""} {
        # Removed code that would execute command once here,
        # this is now handled by prepare_and_run_body.

	set results [prepare_and_run_body $opts(-body) $opts(-iter)]
	if {[lindex $results 0] == "ERROR"} {
	    error [lindex $results 1]
	}
	# Note: Removed Thread running code since it is never used.
	# Get just the microseconds value from the time result
        set msecs $results
        set bench($opts(-desc)) $msecs
        puts $BENCH(OUTFID) [list Sourcing "$opts(-desc): $msecs"]
    }
    if {($opts(-post) != "") && [catch {uplevel \#0 $opts(-post)} err] \
	    && $BENCH(ERRORS)} {
	return -code error "post code threw error:\n$err"
    }
    return
}

proc usage {} {
    set me [file tail [info script]]
    puts stderr "Usage: $me ?options?\
	    \n\t-help			# print out this message\
	    \n\t-rmatch <regexp>	# only run tests matching this pattern\
	    \n\t-match <glob>		# only run tests matching this pattern\
	    \n\t-interp	<name>		# name of interp (tries to get it right)\
	    \n\t-tjc	0|1		# 1 if TJC compiler will be used\
	    \n\tfileList		# files to benchmark"
    exit 1
}

# This method will run the command and catch errors.
# If an error was caught, the pair {ERROR MSG} will
# be returned. Otherwise, an integer indicating the
# time the method took to complete will be returned.
#
# This method takes care to run the tests a couple
# of times to make sure it is fully compiled.

proc prepare_and_run_body { body iterations } {
    set debug 0

    if {$debug} {
        puts stderr "prepare_and_run_body \[$body\] $iterations"
    }

    # Run test to make sure no error is generated. After this block,
    # just assume that the body will not generate
    # an error.
    if {[catch {
        namespace eval :: [list time $body 1]
    } err]} {
        if {$debug} {
            puts stderr "prepare_and_run_body returning ERROR \"$err\""
        }
        return [list ERROR $err]
    }
    # Run the body iterations times over and over
    # again, saving the time results. We do this
    # so that the body can be run enough times
    # that any code it depends on is fully compiled
    # by the JIT or HotSpot compiler.

    set times [list]
    set deltas [list]
    set percents [list]

    set t_last -1

    set restarted 0

    set converged_before 0

    set max_loops 10
    if {$iterations == 1} {
        # Must be a really long test, don't try to run
        # it multiple times
        set max_loops 1
        if {$debug} {
            puts stderr "really long test detected, will iterate once"
        }
    }

    for {set i 0} {$i < $max_loops} {incr i} {
        if {$debug} {
            puts stderr "loop $i, time \[$body\] $iterations"
        }

        set t [run_body $body $iterations]
        lappend times $t
        if {$debug} {
            puts stderr "time is $t"
        }
        if {$t_last != -1} {
            set dt [expr {$t - $t_last}]
            lappend deltas $dt
            if {$t == 0} {
                set pt 0.0
            } else {
                set pt [expr {$dt / double($t)}]
            }
            lappend percents $pt
        }
        set t_last $t

        if {$i < 4} {
            # Always run at least 4 iterations
            if {$debug} {
                puts stderr "continue until 4 iterations"
            }
            continue
        } else {
            # It the results are converging, then
            # no need to run tests anymore. Test
            # that do not take long have a smaller
            # threshhold for convergence.

            if {$t <= 400} {
                set small_delta 0.08
            } else {
                set small_delta 0.01
            }

            if {$debug} {
                puts stderr "checking convergence delta\
                    (abs($pt) < $small_delta) at loop $i"
            }

            if {abs($pt) < $small_delta} {
                # Require that the results converge twice
                # before stopping the loop.

                if {!$converged_before} {
                    if {$debug} {
                        puts stderr "delta percent $pt, converged_before flag set"
                    }
                    set converged_before 1
                    continue
                }

                if {$debug} {
                    puts stderr "delta percent $pt is no longer converging at $i"
                }

                # If the command seems to be taking no time at all,
                # throw out the current results and rerun the tests
                # with more iterations to try to get a more accurate
                # average of the time data.

                set small 50

                if {!$restarted && ($t <= $small)} {
                    if {$debug} {
                    puts stderr "Times :\t\t$times"
                    puts stderr "restarted because time $t is very small"
                    }
                    set restarted 1
                    set times [list]
                    set deltas [list]
                    set percents [list]
                    set t_last -1
                    set converged_before 0

                    set iterations [expr {$iterations * 5}]
                    set i 0
                    continue
                }

                if {$debug} {
                   puts stderr "break out of test loop, times converged twice"
                }
                break
            } else {
                if {$converged_before} {
                    set converged_before 0
                }
            }

            if {$debug} {
               puts stderr "delta percent $pt could still be converging at $i"
            }
        }
    }

    if {$debug} {
        puts stderr "i is $i"
        puts stderr "max_loops is $max_loops"
    }

    if {$debug} {
        puts stderr "body $body : $iterations"
        puts stderr "Times :\t\t$times"
        puts stderr "Deltas :\t$deltas"
        puts stderr "Percents :\t$percents"
    }

    if {([llength $times] < 4) && ($max_loops != 1)} {
        error "times should have at least 4 entries : \{$times\}"
    }

    # The last couple of times in the list indicate
    # the times that were converged to. Get an
    # average of the last 3 times and report that.

    set ctimes [lrange $times end-2 end]

    if {[llength $ctimes] != 3} {
        error "error getting last 3 times from \{$times\}"
    }

    if {$debug} {
        puts stderr "converged to times \{$ctimes\}"
    }

    set t1 [lindex $ctimes 0]
    set t2 [lindex $ctimes 1]
    set t3 [lindex $ctimes 2]

    set mt [expr {round(($t1 + $t2 + $t3) / 3.0)}]

    if {$debug} {
        puts stderr "reporting test time $mt"
    }
    return $mt
}

# Run the body iterations number of times and return the time
# it took per iteration in integer ms.

proc run_body { body iterations } {
#    puts stderr "running \{$body\} $iterations times"
    set results [namespace eval :: [list time $body $iterations]]
    set t [lindex $results 0]
#    puts stderr "timing results were $t"
    return [expr round($t)]
}

#
# Process args
#
if {[catch {set BENCH(INTERP) [info nameofexec]}]} {
    set BENCH(INTERP) $argv0
}
foreach {var val} {
	ERRORS		1
	MATCH		{}
	RMATCH		{}
	OUTFILE		stdout
	FILES		{}
	ITERS		2000
	THREADS		0
	TJC		0
	EXIT		"[info exists tk_version]"
} {
    if {![info exists BENCH($var)]} {
	set BENCH($var) [subst $val]
    }
}
set BENCH(EXIT) 1

if {[llength $argv]} {
    while {[llength $argv]} {
	set key [lindex $argv 0]
	switch -glob -- $key {
	    -help*	{ usage }
	    -err*	{ set BENCH(ERRORS)  [lindex $argv 1] }
	    -int*	{ set BENCH(INTERP)  [lindex $argv 1] }
	    -rmat*	{ set BENCH(RMATCH)  [lindex $argv 1] }
	    -mat*	{ set BENCH(MATCH)   [lindex $argv 1] }
	    -iter*	{ set BENCH(ITERS)   [lindex $argv 1] }
	    -thr*	{ set BENCH(THREADS) [lindex $argv 1] }
	    -tjc	{
                set BENCH(TJC) [lindex $argv 1]
                # Double check that this interp supports compilation
                # at runtime with TJC::compile.
                if {$BENCH(TJC)} {
                    #puts stderr "now to do \[package require TJC\]"
                    package require TJC
                    if {[info commands TJC::compile] == {}} {
                        error "Runtime TJC compilation not supported"
                    }
                    #puts stderr "done with package require"
                }
            }
	    default {
		foreach arg $argv {
		    if {![file exists $arg]} { usage }
		    lappend BENCH(FILES) $arg
		}
		break
	    }
	}
	set argv [lreplace $argv 0 1]
    }
}

rename exit exit.true
proc exit args {
    error "called \"exit $args\" in benchmark test"
}

if {[string compare $BENCH(OUTFILE) stdout]} {
    set BENCH(OUTFID) [open $BENCH(OUTFILE) w]
} else {
    set BENCH(OUTFID) stdout
}

if {1} {
    set debug 0

    foreach BENCH(file) $BENCH(FILES) {
	if {[file exists $BENCH(file)]} {
	    puts $BENCH(OUTFID) [list Sourcing $BENCH(file)]

            if {$BENCH(TJC)} {
                # This is kind of tricky, we need to compile procs
                # defined in the .bench file but the tests are
                # run right after the procs are defined. Work around
                # this by renaming the proc command while this
                # file is being sourced.

                if {$debug} {
                    puts stderr "TJC compile mode was enabled"
                }

                proc __tjc_proc { name args body } {
                    global __tjc_proc_ready debug

                    if {$debug} {
                        puts stderr "DEFINED proc $name in TJC proc handler"
                    }

                    # Be careful to execute the proc command
                    # and the TJC::compile command in the calling
                    # context, so that the proc is defined in
                    # the correct namespace.

                    uplevel 1 [list __tjc_proc $name $args $body]
                    uplevel 1 [list \
                    TJC::compile $name \
                        -readyvar __tjc_proc_ready \
                    ]

                    # Wait for __tjc_proc_ready to be set when
                    # the command has been compiled.
                    if {$debug} {
                        puts stderr "waiting for __tjc_proc_ready"
                    }
                    vwait __tjc_proc_ready
                    if {$debug} {
                        puts stderr "__tjc_proc_ready is \{$__tjc_proc_ready\}"
                    }
                    if {[lindex $__tjc_proc_ready 0] != "OK"} {
                        puts stderr "TJC compile status: $__tjc_proc_ready"
                    }
                    unset __tjc_proc_ready
                }
                rename proc __tmp
                rename __tjc_proc proc
                rename __tmp __tjc_proc
            } else {
                if {$debug} {
                    puts stderr "TJC compile mode was NOT enabled"
                }
            }

            # Sourcing the .bench file will define the procs
            # or compiled commands and run the bench tests
            # via the "bench" command that appears at the
            # bottom of the .bench file.
            if {$debug} {
                puts stderr "source $BENCH(file)"
            }
            if {[catch {
	    source $BENCH(file)
            } err]} {
                puts stderr "ERROR Sourcing $BENCH(file)"
                puts stderr "ERROR: $err"
                puts stderr "errorInfo: $::errorInfo"
            }

            if {$BENCH(TJC)} {
                # Reset original proc command
                rename proc {}
                rename __tjc_proc proc
            }
	}
    }

    # Print test name and timing results:
    foreach desc [lsort -dictionary [array names bench]] {
	puts $BENCH(OUTFID) [list TIMING $desc $bench($desc)]
    }

    if {$BENCH(EXIT)} {
	exit.true ; # needed for Tk tests
    }
    #puts stdout "exit at end of bench tests"
    #flush stdout
}

