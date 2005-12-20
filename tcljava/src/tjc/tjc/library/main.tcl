#
#  Copyright (c) 2005 Advanced Micro Devices, Inc.
#
#  See the file "license.amd" for information on usage and
#  redistribution of this file, and for a DISCLAIMER OF ALL
#   WARRANTIES.
#
#  RCS: @(#) $Id: main.tcl,v 1.1 2005/12/20 23:00:11 mdejong Exp $
#
#

# Process list of command line argments and assign
# values to the _cmdline array.

proc process_cmdline { argv } {
    global _cmdline

    set files [list]
    set options [list]

    foreach arg $argv {
        if {[string match *.tjc $arg]} {
            lappend files $arg
        } else {
            lappend options $arg
        }
    }

    set _cmdline(files) $files
    set _cmdline(options) $options

    return $options
}

# Validate command line options passed to tjc executable.

proc validate_options {} {
    global _tjc
    global _cmdline

    foreach option $_cmdline(options) {
        # If option string does not start with a - character
        # then it is not a valid option.
        if {[string match *.tcl $option]} {
            error "Tcl source file $option is not a valid argument, pass TJC module file"
        } elseif {[string index $option 0] == "-"} {
            # Valid option
            switch -- $option {
                "-debug" {
                    # Print extra debug info
                }
                "-nocompile" {
                    # Don't invoke javac, just emit code and exit
                    set _tjc(nocompile) 1
                }
            }
        } else {
            error "option $option is invalid"
        }
    }
}

# Process jdk config values defined in jdk.cfg
# in root/bin/jdk.cfg.

proc process_jdk_config {} {
    global _tjc env

    # Support for running tjc executable from build dir
    if {[info exists env(TJC_LIBRARY)] && \
            [info exists env(TJC_BUILD_DIR)]} {
        set jdk_cfg $env(TJC_BUILD_DIR)/jdk.cfg
    } else {
        set jdk_cfg [file join $_tjc(root) bin jdk.cfg]
    }

    set res [jdk_config_parse_file $jdk_cfg]
    if {[lindex $res 0] == 0} {
        puts stderr "Error loading $jdk_cfg : [lindex $res 1]"
        return -1
    }
    # JDK config values are now validated, they
    # can be quiried via jdk_config_var.
    return 0
}

# Test out each jdk tool to make sure it actually works.

proc check_jdk_config {} {
    set fname [jdk_tool_javac_save Test {
public class Test {}
    }]
    set result [jdk_tool_javac [list $fname]]
    if {[lindex $result 0] == "ERROR"} {
        puts stderr "jdk tool check failed: JAVAC not working"
        return -1
    }

    set jarname [jdk_tool_jar test.jar]
    if {[lindex $result 0] == "ERROR"} {
        puts stderr "jdk tool check failed: JAR not working"
        return -1
    }

    return 0
}

proc process_module_file { filename } {
    global _tjc

    set debug 0

    if {$debug} {
    puts "process_module_file $filename"
    }

    # Read in module file configuration
    if {![file exists $filename]} {
        puts stderr "module file \"$filename\" does not exists"
        return -1
    }

    if {[catch {module_parse_file $filename} err]} {
        puts stderr "module file $filename parse failure: $err"
        return -1
    }

    # Validate module commands parsed above
    if {[catch {module_parse_validate} err]} {
        puts stderr "module file $filename validation failure: $err"
        return -1
    }

    # Validate module options and proc options
    if {[catch {module_options_validate} err]} {
        puts stderr "module file $filename options validation failure: $err"
        return -1
    }

    # Query package name
    set pkg [module_query PACKAGE]

    # Query and possibly expand names of Tcl source files.
    set source_files [module_expand SOURCE]

    # Query and possibly expand names of Tcl source files to include as Tcl.
    set include_source_files [module_expand INCLUDE_SOURCE]

    set source_files [module_filter_include_source \
        $source_files $include_source_files]

    # Parse source Tcl file, extract proc declarations, and save
    # the modified Tcl source into the build directory.

    set TJC [file join [pwd] [jdk_tjc_rootdir]]
    set TJC_BUILD [file join $TJC build]
    set TJC_BUILD_PACKAGE_LIBRARY [file join $TJC_BUILD [jdk_package_library $pkg 0]]

    file mkdir $TJC_BUILD_PACKAGE_LIBRARY

    set TJC_SOURCE [file join $TJC source]
    set TJC_SOURCE_PACKAGE_LIBRARY [file join $TJC_SOURCE [jdk_package_library $pkg 1]]

    file mkdir $TJC_SOURCE_PACKAGE_LIBRARY

    # Init Tcl proc name to Java class name module
    nameproc_init $pkg

    # List of {FILENAME PARSED_PROCS} for each parse file
    set file_and_procs [list]
    set num_procs_parsed 0

    foreach source_file $source_files {
        if {$debug} {
        puts "processing procs in source \"$source_file\""
        }

        set script [tjc_util_file_read $source_file]

        # Make a copy of input Tcl source
        file copy $source_file $TJC_SOURCE_PACKAGE_LIBRARY

        parseproc_init
        # Parse Tcl commands from file, if an error is generated
        # then print a diagnostic message and quit.
        if {[catch {
        set results [parseproc_start $script $source_file]
        } err]} {
            puts stderr "[module_get_filename]: Interal error while parsing Tcl file $source_file:"
            puts stderr "$err"
            return -1
        }

        set mod_script [lindex $results 0]
        set proc_tuples [lindex $results 1]
        incr num_procs_parsed [llength $proc_tuples]

        if {$debug} {
        puts "got mod_script \"$mod_script\""
        puts "got proc_tuples \{$proc_tuples\}"
        }

        lappend file_and_procs [list $source_file $proc_tuples]

        # Write modified Tcl script to the build library directory
        set tail [file tail $source_file]
        set script_out [file join $TJC_BUILD_PACKAGE_LIBRARY $tail]

        tjc_util_file_saveas $script_out $mod_script

        if {$debug} {
        puts "wrote proc parsed script \"$script_out\""
        }
    }

    # If no procs were parsed out of the Tcl files, generate
    # an error. It is unlikely that the user intended to
    # compile procs and none were found.

    if {$num_procs_parsed == 0} {
        puts stderr "no compilable procs found in SOURCE files"
        return -1
    }

    # Generate Java code for each proc found in the given file.
    # This method return a list of tuples of the following format:
    # {TCL_FILENAME PROC_NAME JAVA_CLASSNAME JAVA_SOURCE}

    set java_files [list]

    set tuples [compileproc_entry_point $file_and_procs]
    if {$tuples == "ERROR"} {
        # Error caught in compileproc_entry_point, diagnostic
        # message already printed so stop compilation now.
        return -1
    }
    #if {[catch {
    #} err]} {
    #    puts stderr "compileproc error: $err"
    #    return -1
    #}

    foreach tuple $tuples {
        set tcl_filename [lindex $tuple 0]
        set proc_name [lindex $tuple 1]
        set java_class [lindex $tuple 2]
        set java_source [lindex $tuple 3]

        set java_filename [jdk_tool_javac_save $java_class $java_source]
        lappend java_files $java_filename
    }

    # Generate Java code for TJCExtension class for the package.
    # This code assumes that we have already checked that no two
    # files have the same filename and that all the files
    # that appear in INCLUDE_SOURCE appear in the SOURCE.
    # Also, we assume that INIT_SOURCE is a filename and not a pattern.

    set init_source [module_query INIT_SOURCE]
    set init_source [file tail $init_source]

    set source_tails [list]
    foreach file [concat $source_files $include_source_files] {
        set tail [file tail $file]
        lappend source_tails $tail
    }

    set tuple [compileproc_tjcextension $pkg $source_tails $init_source]
    set java_class [lindex $tuple 0]
    set java_source [lindex $tuple 1]
    set java_filename [jdk_tool_javac_save $java_class $java_source]
    lappend java_files $java_filename

    # Compile Java code

    if {[info exists _tjc(nocompile)] && $_tjc(nocompile)} {
        # Don't compile or create JAR file, just exit
        puts "Skipped JAVAC compile step since -nocompile flags was passed"
        return 0
    }

    set result [jdk_tool_javac $java_files]
    if {[lindex $result 0] == "ERROR"} {
        return -1
    }

    # Add INCLUDE_SOURCE Tcl files before creating jar files
    set include_source_files
    foreach include_source_file $include_source_files {
        file copy $include_source_file $TJC_SOURCE_PACKAGE_LIBRARY
        file copy $include_source_file $TJC_BUILD_PACKAGE_LIBRARY
    }

    # Create jar file for converted Tcl and class files

    set tail [file tail $filename]
    set tailr [file rootname $tail]
    set jar_name "${tailr}.jar"
    set jar_location [jdk_tool_jar $jar_name]

    # Create jar file for original Tcl source and generated Java source

    set src_jar_name "${tailr}src.jar"
    set src_jar_location [jdk_tool_jar $src_jar_name SOURCE]

    # Move generated jar files into working directory (above TJC)

    set jar_filename [file join [pwd] $jar_name]
    if {$debug} {
    puts "now to rename \"$jar_location\" to \"$jar_filename\""
    }
    if {[file exists $jar_filename]} {
        file delete $jar_filename
    }
    file rename $jar_location $jar_filename

    set src_jar_filename [file join [pwd] $src_jar_name]
    if {$debug} {
    puts "now to rename \"$src_jar_location\" to \"$src_jar_filename\""
    }
    if {[file exists $src_jar_filename]} {
        file delete $src_jar_filename
    }
    file rename $src_jar_location $src_jar_filename

    # Nuke TJC subdirectory
    jdk_tool_cleanup

    return 0
}

# entry point invoked when tjc is run normally.
# This method processes command line arguments
# in the argv list.

proc main { argv } {
    global argv0 _cmdline _tjc

    set debug 0

    if {$debug} {
    puts "tjc compiler loaded ..."
    puts "tjc_root is \"$_tjc(root)\""
    puts "cwd is [pwd]"
    puts "argv is \{$argv\}"
    puts "argv0 is \{$argv0\}"
    }

    process_cmdline $argv

    # Check that options (non TJC files) are valid
    if {[catch {validate_options} err]} {
        puts stderr $err
        return 1
    }

    # Anything that is not a .tjc file is filtered as an option
    if {!$debug && [llength $_cmdline(files)] == 0} {
        puts "usage: tjc module.tjc ..."
        return 1
    }

    set ret [process_jdk_config]
    if {$ret != 0} {
        return $ret
    }
    set ret [check_jdk_config]
    if {$ret != 0} {
        jdk_tool_cleanup
        return $ret
    }
    # Cleanup after check so build is clean
    # when we start processing module files.
    jdk_tool_cleanup

    foreach file $_cmdline(files) {
        set ret [process_module_file $file]
        if {$ret != 0} {
            return $ret
        }
    }

    jdk_tool_cleanup
    return 0
}

