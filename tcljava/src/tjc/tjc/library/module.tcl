#
#  Copyright (c) 2005 Advanced Micro Devices, Inc.
#
#  See the file "license.amd" for information on usage and
#  redistribution of this file, and for a DISCLAIMER OF ALL
#   WARRANTIES.
#
#  RCS: @(#) $Id: module.tcl,v 1.3 2006/01/11 21:24:50 mdejong Exp $
#
#

# Parse tjc module info in the given file

proc module_parse_file { file } {
    global _module
    set _module(fileename) [file tail $file]
    return [module_parse [tjc_util_file_read $file]]
}

# Return name of module file currently being processed

proc module_get_filename {} {
    global _module
    if {![info exists _module(fileename)]} {
        return
    } else {
        return $_module(fileename)
    }
}

# Parse tjc module info from the given data buffer.

proc module_parse { data } {
    global _module

    # Clear out old settings
    if {[info exists _module]} {
        unset _module
    }
    set _module(package) ""
    set _module(options) ""
    # _module(source) must be set in the file
    set _module(include_source) ""
    # _module(init_source) must be set in the file
    set _module(proc_options) ""

    set lines [split $data \n]
    set num_cmds 0

    for {set i 0 ; set max [llength $lines]} {$i < $max} {incr i} {
        set line [string trim [lindex $lines $i]]
        if {[string length $line] == 0} {
            continue
        } elseif {[string index $line 0] == "#"} {
            # Comment line, just ignore it
            continue
        } elseif {[string index $line end] == "\\"} {
            # Command continued on next line
            set line [string range $line 0 end-1]
            for {set i [expr {$i + 1}]} {$i < $max} {incr i} {
                set next_line [string trim [lindex $lines $i]]
                if {[string index $next_line 0] == "#"} {
                    continue
                }
                append line $next_line
                if {[string index $next_line end] != "\\"} {
                    break
                }
            }
        }

        module_parse_command $line [expr {$i + 1}]
        incr num_cmds
    }

    return $num_cmds
}

# Pase a single command line. Multiple input lines that were
# continued would have already been compressed by module_parse.

proc module_parse_command { cmdstr linenum } {
    global _module

    # count of parsed arguments +1 for the command
    set nparsed 0

    if {![regexp {^([A-Z|_]+)} $cmdstr whole sub1]} {
        error "Module command not found in input \"$cmdstr\", at line $linenum"
    }
    set cmd $sub1

    switch -exact -- $cmd {
        "PACKAGE" {
            set clist [module_space_split $cmdstr]
            if {[llength $clist] != 2} {
                error "Module command PACKAGE requires 1 argument, at line $linenum"
            }
            set nparsed [llength $clist]
            set package [lindex $clist 1]
            # Valid characters in Java package name
            if {$::tcl_platform(platform) == "java"} {
                # Jacl old regexp impl
                set pat {[^0-9|A-Z|a-z|_|.]}
            } else {
                # Tcl new regexp impl
                set pat {[^[:alnum:]|_|.]}
            }
            if {[regexp $pat $package whole]} {
                if {$whole == "\""} {
                    set whole "\\\""
                } elseif {$whole == "\t"} {
                    set whole "\\t"
                }
                error "Module command PACKAGE name contains an invalid character \"$whole\""
            }
            set _module(package) $package
        }
        "OPTIONS" {
            set clist [module_space_split $cmdstr]
            # Zero OPTIONS arguments is allowed.
            if {[llength $clist] == 0} {error "no elements in OPTIONS string"}
            set nparsed [llength $clist]
            set cargs [lrange $clist 1 end]
            set options [list]

            foreach arg $cargs {
                # Each option arg must be +word or -word
                set pat {^(\+|\-)([a-z|_|\-]+)$}
                if {![regexp $pat $arg whole sub1 sub2]} {
                    error "Module command OPTIONS argument \"$arg\" is invalid, at line $linenum"
                }
                lappend options $sub2 [expr {($sub1 == "+") ? 1 : 0}]
            }

            set _module(options) $options
        }
        "SOURCE" {
            set clist [module_space_split $cmdstr]
            if {[llength $clist] < 2} {
                error "Module command SOURCE requires 1 or more arguments, at line $linenum"
            }
            set nparsed [llength $clist]
            set _module(source) [lrange $clist 1 end]
        }
        "INCLUDE_SOURCE" {
            set clist [module_space_split $cmdstr]
            if {[llength $clist] < 2} {
                error "Module command INCLUDE_SOURCE requires 1 or more arguments, at line $linenum"
            }
            set nparsed [llength $clist]
            set _module(include_source) [lrange $clist 1 end]
        }
        "INIT_SOURCE" {
            set clist [module_space_split $cmdstr]
            if {[llength $clist] != 2} {
                error "Module command INIT_SOURCE requires 1 argument, at line $linenum"
            }
            set nparsed [llength $clist]
            set _module(init_source) [lindex $clist 1]
        }
        "PROC_OPTIONS" {
            if {![regexp {^PROC_OPTIONS +\{([A-Z|a-z|0-9|_|:| ]*)\} +(.*)$} $cmdstr whole sub1 sub2]} {
                error "Module command PROC_OPTIONS format not matched, at line $linenum"
            }
            set plist [module_space_split $sub1]
            if {[llength $plist] == 0} {
                error "Module command PROC_OPTIONS requires 1 or more proc names, at line $linenum"
            }
            set alist [module_space_split $sub2]
            if {[llength $alist] == 0} {
                error "Module command PROC_OPTIONS requires 1 or more option arguments, at line $linenum"
            }
            set nparsed [expr {2 + [llength $alist]}]
            # Validate proc options

            set options [list]

            foreach arg $alist {
                # Each option arg must be +word or -word
                set pat {^(\+|\-)([a-z|_|\-]+)$}
                if {![regexp $pat $arg whole sub1 sub2]} {
                    error "Module command PROC_OPTIONS argument \"$arg\" is invalid, at line $linenum"
                }
                lappend options $sub2 [expr {($sub1 == "+") ? 1 : 0}]
            }

            foreach pname $plist {
                lappend _module(proc_options) [list $pname $options]
            }
        }
        default {
            error "Unknown module command $cmd, at line $linenum"
        }
    }

    return $nparsed
}

# Split string up into a list based on spaces.

if {[catch {package require Tcl 8.4} err]} {

# Jacl impl, lacks new regexp command

proc module_space_split { str } {
    set pat {[^ ]+}
    set results [list]

    while {1} {
        # Find next non-space character  
        if {![regexp -indices $pat $str whole]} {
            # Rest of characters are spaces
            break
        }
        set i1 [lindex $whole 0]
        set i2 [lindex $whole 1]

        lappend results [string range $str $i1 $i2]

        set str [string range $str [expr {$i2 + 1}] end]
    }

    return $results
}

} else {

proc module_space_split { str } {
    set pat {[^ ]+}
    set results [list]
    foreach match [regexp -all -inline $pat $str] {
        lappend results $match
    }
    return $results
}

}

# Query current value and expand out path pattern into a
# list of file names.

proc module_expand { cmd } {
    global _module

    set debug 0

    # Already expanded source list, return it now
    set lower [string tolower $cmd]
    if {[info exists _module(expanded_$lower)]} {
        return $_module(expanded_$lower)
    }

    set vals [module_query $cmd]
    set evals [list]

    if {$debug} {
        puts "vals is \{$vals\}"
    }

    foreach val $vals {
        if {$debug} {
            puts "val is \"$val\""
        }

        # If file looks like a glob pattrns, then to expand
        # it with global. Otherwise it is a file name, if
        # the file does not exists then raise an error.

        if {![module_looks_like_glob $val]} {
            if {![file exists $val]} {
                error "$cmd declaration contains file named \"$val\" that does not exist"
            }
            if {$debug} {
                puts "appending existing filename val \"$val\""
            }
            lappend evals $val
        } else {
            set fnames [glob -nocomplain $val]
            if {$debug} {
                puts "glob pattern \"$val\" expands to \{$fnames\}"
            }
            foreach fname $fnames {
                lappend evals $fname
            }
        }
    }

    set svals [lsort -dictionary $evals]
    set _module(expanded_$lower) $svals

    if {$debug} {
        puts "set _module(expanded_$lower) to \{$svals\}"
    }

    return $svals
}

# Return 1 if the input string looks like a glob pattern.

proc module_looks_like_glob { pat } {
    if {[string first "?" $pat] != -1} {
        return 1
    } elseif {[string first "*" $pat] != -1} {
        return 1
    } elseif {[regexp {\[.*\]} $pat] > 0} {
        return 1
    } elseif {[regexp {\{.*\}} $pat] > 0} {
        return 1
    } else {
        return 0
    }
}


# Query the current value of the given command.

proc module_query { cmd } {
    global _module

    switch -exact -- $cmd {
        "PACKAGE" {
            return $_module(package)
        }
        "OPTIONS" {
            return $_module(options)
        }
        "SOURCE" {
            return $_module(source)
        }
        "INCLUDE_SOURCE" {
            return $_module(include_source)
        }
        "INIT_SOURCE" {
            return $_module(init_source)
        }
        "PROC_OPTIONS" {
            return $_module(proc_options)
        }
        default {
            error "unsupported cmd \"$cmd\""
        }
    }
}

# Input is a list of filenames expanded from a SOURCE
# declaration inside a module file and a list of
# filenames expanded from an INCLUDE_SOURCE
# declaration. This method will filter out those
# filenames that appear in the include_source
# from the source list.

proc module_filter_include_source { source_files include_source_files } {
    if {[llength $include_source_files] == 0} {
        return $source_files
    }
    foreach include_file $include_source_files {
        set include_tail [file tail $include_file]
        set include_tails($include_tail) ""
    }
    set filtered_source_files [list]
    foreach source_file $source_files {
        set source_tail [file tail $source_file]
        if {[info exists include_tails($source_tail)]} {
            # Filter out of filtered_source_files
        } else {
            lappend filtered_source_files $source_file
        }
    }
    return $filtered_source_files
}

# Validate options and proc options. If a combination of options
# is not valid, then an error will be raised.

proc module_options_validate {} {
    set debug 0
    
    if {$debug} {
        puts "module_options_validate"
    }

    set options [module_query OPTIONS]
    if {$debug} {
        puts "OPTIONS $options"
    }

    set len [llength $options]
    if {($len % 2) != 0} {
        error "expected even num options, got $len"
    }
    set num_options [expr {$len / 2}]
    set index 0
    for {set i 0} {$i < $len} {incr i 2} {
        set op [lindex $options $i]
        set val [lindex $options [expr {$i + 1}]]
        incr index
        module_option_validate $op $val $index $num_options $options
    }

    set proc_options [module_query PROC_OPTIONS]
    foreach pair $proc_options {
        set p_name [lindex $pair 0]
        # Combine OPTIONS and PROC_OPTIONS for specific proc
        set p_options [lindex $pair 1]
        set len [llength $p_options]
        if {($len % 2) != 0} {
            error "expected even num options, got $len"
        }
        # If an OPTION setting also appears in the PROC_OPTIONS,
        # then ignore the OPTION setting.
        catch {unset options_map}
        if {$debug} {
        puts "options is \{$options\}"
        }
        array set options_map $options
        if {$debug} {
        parray options_map
        }

        set cp_options [list]
        foreach {op val} $p_options {
            if {[info exists options_map($op)]} {
                unset options_map($op)
            }
        }
        set cp_options [list]
        foreach {op val} $options {
            if {[info exists options_map($op)]} {
                lappend cp_options $op $options_map($op)
            }
        }
        foreach {op val} $p_options {
            lappend cp_options $op $val
        }
        set len [llength $cp_options]
        if {($len % 2) != 0} {
            error "expected even num options, got $len"
        }
        set num_options [expr {$len / 2}]
        set index 0
        for {set i 0} {$i < $len} {incr i 2} {
            set op [lindex $cp_options $i]
            set val [lindex $cp_options [expr {$i + 1}]]
            incr index
            module_option_validate $op $val $index $num_options $cp_options
        }
    }
}

# Validate a single option name and value

proc module_option_validate { op val index num_options options } {
    set debug 0

    if {$debug} {
        puts "module_option_validate \{$op $val\} $index $num_options \{$options\}"
    }

    if {$num_options > 1 && $index >= 2} {
        set rstart 0
        set rend [expr {(2 * $index) - 2 - 1}]
        set options_before [lrange $options $rstart $rend]
        if {$debug} {
            puts "options_before range $rstart $rend \{$options_before\}"
        }
        if {([llength $options_before] % 2) != 0} {
            error "uneven options_before \{$options_before\}: options \{$options\} : range 0 [expr {$index - 1}]"
        }
    } else {
        set options_before {}
        if {$debug} {
            puts "options_before \{\}"
        }
    }

    switch -exact -- $op {
        "cache-commands" {
            # No-op
        }
        "cache-variables" {
            # No-op
        }
        "compile" {
            if {$val == 0 && $num_options != 1} {
                error "-compile option must appear with no other options"
            }
        }
        "constant-increment" {
            # -constant-increment can appear anywhere
            if {$val == 1} {
                error "+constant-increment not supported"
            }
        }
        "dummy" {
            # No-op, this option is just for testing
        }
        "inline-containers" {
            # No-op
            # FIXME: If +inline is already given, then invalid ?
        }
        "inline-controls" {
            # If +inline-controls is found then +inline-containers
            # must appear before it.
            if {$val} {
                array set options_before_map $options_before
                #if {[info exists options_before_map]} { parray options_before_map }

                if {![info exists options_before_map(inline-containers)] ||
                        $options_before_map(inline-containers) == 0} {
                    error "+inline-controls option must appear after +inline-containers"
                }
            }
        }
        default {
            error "unknown option \"$op\""
        }
    }
    return
}

# Query the value of a specific option. If a proc name is given
# as the second argument then the option for a specific proc
# will be returned.

proc module_option_value { option {proc {}} } {
    array set options [module_query OPTIONS]

    if {$proc == {}} {
        if {[info exists options($option)]} {
            return $options($option)
        } else {
            return [module_option_default $option]
        }
    } else {
        # Option for specific proc, if not set then
        # check the main options, and then the default

        foreach pair [module_query PROC_OPTIONS] {
            set p_name [lindex $pair 0]
            set p_options [lindex $pair 1]

            if {$proc == $p_name} {
                array set proc_options $p_options

                if {[info exists proc_options($option)]} {
                    return $proc_options($option)
                } else {
                    # Empty result means option not set for proc
                    return
                }
            }
        }
    }
}

# Return default value for an option that is not set by the user
# The "dummy" option is just used for testing.

proc module_option_default { option } {
    switch -exact -- $option {
        "cache-commands" {return 0}
        "cache-variables" {return 0}
        "compile" {return 1}
        "constant-increment" {return 1}
        "dummy" {return 0}
        "inline-containers" {return 0}
        "inline-controls" {return 0}
        default {
            error "unknown option \"$option\""
        }
    }
}


# Validate a module file data after all of the entries have
# been parsed.

proc module_parse_validate {} {
    global _module

    if {![info exist _module(source)]} {
        error "No SOURCE declaration found in module config"
    }
    if {![info exist _module(init_source)]} {
        error "No INIT_SOURCE declaration found in module config"
    }

    # Expand glob patterns in SOURCE and create list of tails

    set expanded_source_files [module_expand SOURCE]
    if {[llength $expanded_source_files] == 0} {
        error "SOURCE declaration expanded to zero files"
    }

    foreach file $expanded_source_files {
        set tail [file tail $file]
        if {[info exists source_tails($tail)]} {
            # Duplicate file name in SOURCE (in different dirs)
            error "SOURCE declaration contains more than 1 file named \"$tail\""
        }
        set source_tails($tail) $file
    }

    # File name in INIT_SOURCE must exists in the expanded SOURCE list

    set init_file [module_query INIT_SOURCE]
    if {[module_looks_like_glob $init_file]} {
        error "INIT_SOURCE \"$init_file\" is a glob pattern, must be a filename"
    }
    set tail [file tail $init_file]
    if {![info exists source_tails($tail)]} {
        error "INIT_SOURCE file \"$tail\" does not appear in SOURCE declaration"
    }

    # Each file in INCLUDE_SOURCE must appear in SOURCE

    foreach file [module_expand INCLUDE_SOURCE] {
        set tail [file tail $file]
        if {![info exists source_tails($tail)]} {
            error "INCLUDE_SOURCE file \"$tail\" does not appear in SOURCE declaration"
        }
    }

    # Check that PACKAGE does not contain a Java keyword
    set pkg [module_query PACKAGE]
    set contains_keyword 0
    foreach elem [split $pkg "."] {
        if {[emitter_is_java_keyword $elem]} {
            set contains_keyword 1
        }
    }
    if {$pkg == "default"} {
        # Don't raise error for default package
        set contains_keyword 0
    }
    if {$contains_keyword} {
        error "PACKAGE must not be a Java keyword"
    }

    return
}

