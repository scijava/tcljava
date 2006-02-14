#
#  Copyright (c) 2005 Advanced Micro Devices, Inc.
#
#  See the file "license.amd" for information on usage and
#  redistribution of this file, and for a DISCLAIMER OF ALL
#   WARRANTIES.
#
#  RCS: @(#) $Id: compileproc.tcl,v 1.12 2006/02/14 04:13:27 mdejong Exp $
#
#

# The compileproc module will parse and compile the contents of a proc.

set _compileproc(debug) 0

# Convert a proc declaration to a list of proc arguments.
# This method assumes that the script string is already
# a valid Tcl list.

proc compileproc_script_to_proc_list { script } {
    set len [llength $script]
    if {$len != 4} {
        error "expected proc name args body: got $len args"
    }
    if {[lindex $script 0] != "proc"} {
        error "expected proc at index 0, got \"[lindex $script 0]\""
    }
    return [lrange $script 1 end]
}

# Split arguments to a proc up into three types.
# The return value is a list of length 3 consisting
# of {NON_DEFAULT_ARGS DEFAULT_ARGS ARGS}. The
# first list is made up of those argumens that
# have no default value. The second list is made
# up of those arguments that have a default value.
# The third list is a single boolean element, it is
# true if the special "args" argument was found as
# the last element.

proc compileproc_args_split { proc_args } {
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_args_split : \{$proc_args\}"
    }

    set non_default_args [list]
    set default_args [list]
    set args [list false]
    set min_num_args 0
    set max_num_args {}

    foreach proc_arg $proc_args {
        if {$proc_arg == "args"} {
            set args true
        } elseif {[llength $proc_arg] == 1} {
            lappend non_default_args $proc_arg
            incr min_num_args
        } else {
            lappend default_args $proc_arg
        }
    }

    # Calculate max number of arguments as long as "args" was not found

    if {!$args} {
        set max_num_args [expr {$min_num_args + [llength $default_args]}]
    }

    if {$debug} {
        puts "returning \{$non_default_args\} \{$default_args\} $args : $min_num_args $max_num_args"
    }

    return [list $non_default_args $default_args $args $min_num_args $max_num_args]
}


# Process proc arguments and emit code to set local variables
# named in the proc argument to the values in the passed in objv.

proc compileproc_args_assign { proc_args } {
    global _compileproc_key_info

    if {[info exists _compileproc_key_info(cmd_needs_init)]} {
        set cmd_needs_init $_compileproc_key_info(cmd_needs_init)
    } else {
        set cmd_needs_init 0
    }

    if {[info exists _compileproc_key_info(constants_found)]} {
        set constants_found $_compileproc_key_info(constants_found)
    } else {
        set constants_found 0
    }

    if {[llength $proc_args] == 0} {
        # No arguments to proc
        return [emitter_empty_args]
    }

    set buffer ""
    set result [compileproc_args_split $proc_args]
    set non_default_args [lindex $result 0]
    set default_args [lindex $result 1]
    set has_args [lindex $result 2]
    set min_num_args [lindex $result 3]
    set max_num_args [lindex $result 4]

    if {[llength $non_default_args] == 0 && [llength $default_args] == 0 && !$has_args} {
        error "no arguments found"
    } elseif {!$has_args && [llength $default_args] == 0} {
        # Non-default arguments only
        if {$min_num_args != $max_num_args} {
            error "expected min to match max num args: $min_num_args $max_num_args"
        }
        set args_str [join $non_default_args " "]
        append buffer [emitter_num_args $max_num_args $args_str]

        set index 1
        foreach arg $non_default_args {
            append buffer [compileproc_assign_arg $arg $index]
            incr index
        }
    } elseif {!$has_args && [llength $non_default_args] == 0} {
        # Default arguments only
        if {$min_num_args != 0} {
            error "expected min num arguments to be zero, got $min_num_args"
        }
        set arg_names [list]
        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            lappend arg_names "?${name}?"
        }
        set args_str [join $arg_names " "]

        append buffer [emitter_num_default_args $max_num_args $args_str]

        set index 1
        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            compileproc_constant_cache_add $default
            set constants_found 1
            set default_sym [compileproc_constant_cache_get $default]

            append buffer [compileproc_assign_default_arg $name $index $default_sym]
            incr index
        }
    } elseif {$has_args && \
            [llength $non_default_args] == 0 && \
            [llength $default_args] == 0} {
        # args argument only
        append buffer [compileproc_assign_args_arg 1]
    } elseif {$has_args && \
            [llength $non_default_args] > 0 && \
            [llength $default_args] == 0} {
        # Non-default arguments and args
        if {$max_num_args != {}} {
            error "expected empty max num args, got $max_num_args"
        }
        set all_args $non_default_args
        lappend all_args args

        set args_str [join $all_args " "]
        append buffer [emitter_num_min_args $min_num_args $args_str]

        set index 1
        foreach arg $non_default_args {
            append buffer [compileproc_assign_arg $arg $index]
            incr index
        }

        append buffer [compileproc_assign_args_arg $index]
    } elseif {$has_args && \
            [llength $non_default_args] == 0 && \
            [llength $default_args] > 0} {
        # Default arguments and args
        if {$max_num_args != {}} {
            error "expected empty max num args, got $max_num_args"
        }
        if {$min_num_args != 0} {
            error "expected zero min num args, got $min_num_args"
        }
        set arg_names [list]
        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            lappend arg_names "?${name}?"
        }
        lappend arg_names "args"
        set args_str [join $arg_names " "]

        # No num args check

        set index 1
        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            compileproc_constant_cache_add $default
            set constants_found 1
            set default_sym [compileproc_constant_cache_get $default]

            append buffer [compileproc_assign_default_arg $name $index $default_sym]
            incr index
        }

        append buffer [compileproc_assign_args_arg $index]
    } elseif {!$has_args && \
            [llength $non_default_args] > 0 && \
            [llength $default_args] > 0} {
        # Non-default args and default args

        set arg_names $non_default_args
        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            lappend arg_names "?${name}?"
        }
        set args_str [join $arg_names " "]

        append buffer [emitter_num_range_args $min_num_args $max_num_args $args_str]

        set index 1
        foreach arg $non_default_args {
            set name [lindex $arg 0]

            append buffer [compileproc_assign_arg $name $index]
            incr index
        }

        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            compileproc_constant_cache_add $default
            set constants_found 1
            set default_sym [compileproc_constant_cache_get $default]

            append buffer [compileproc_assign_default_arg $name $index $default_sym]
            incr index
        }
    } elseif {$has_args && \
            [llength $non_default_args] > 0 && \
            [llength $default_args] > 0} {
        # Non-default args, default args, and args

        set arg_names $non_default_args
        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            lappend arg_names "?${name}?"
        }
        lappend arg_names "args"
        set args_str [join $arg_names " "]

        append buffer [emitter_num_min_args $min_num_args $args_str]

        set index 1
        foreach arg $non_default_args {
            set name [lindex $arg 0]

            append buffer [compileproc_assign_arg $name $index]
            incr index
        }

        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            compileproc_constant_cache_add $default
            set constants_found 1
            set default_sym [compileproc_constant_cache_get $default]

            append buffer [compileproc_assign_default_arg $name $index $default_sym]
            incr index
        }

        append buffer [compileproc_assign_args_arg $index]
    } else {
        error "unhandled args case: non_default_args \{$non_default_args\}, default_args \{$default_args\}, has_args $has_args"
    }

    # Set constant flags if needed

    if {$constants_found} {
        set cmd_needs_init 1
    }

    set _compileproc_key_info(cmd_needs_init) $cmd_needs_init
    set _compileproc_key_info(constants_found) $constants_found

    return $buffer
}

# Emit code to assign a procedure argument to the local variable
# table. Name is the name of the argument to the procedure.
# Index is the integer index from the passed in objv array where
# the value of the argument will be found.

proc compileproc_assign_arg { name index } {
    if {![string is integer $index]} {
        error "index $index is not an integer"
    }
    if {$index < 1} {
        error "index $index must be greater than 0"
    }
    set value "objv\[$index\]"
    return [emitter_statement \
        [compileproc_set_variable $name true $value false]]
}

# Emit code to assign a procedure argument with a default value
# to the local variable table. Name is the name of the argument
# to the procedure.
# Index is the integer index from the passed in objv array where
# the value of the argument will be found.

proc compileproc_assign_default_arg { name index default_symbol } {
    if {![string is integer $index]} {
        error "index $index is not an integer"
    }
    if {$index < 1} {
        error "index $index must be greater than 0"
    }

    set buffer ""

    append buffer [emitter_indent]
    emitter_indent_level +1
    set sp "\n[emitter_indent]"
    emitter_indent_level -1

    set value "${sp}((objv.length <= $index) ? $default_symbol : objv\[$index\])"
    append buffer [compileproc_set_variable $name true $value false] \
        "\;\n"

    return $buffer
}

# Emit code to assign procedure arguments to a local named "args"
# starting from the given index.

proc compileproc_assign_args_arg { index } {
    if {![string is integer $index]} {
        error "index $index is not an integer"
    }
    if {$index < 1} {
        error "index $index must be greater than 0"
    }

    set buffer ""
    append buffer [emitter_indent] \
    "if ( objv.length <= $index ) \{\n"
    emitter_indent_level +1
    set value ""
    append buffer [emitter_statement \
        [compileproc_set_variable "args" true $value true]]
    emitter_indent_level -1
    append buffer [emitter_indent] \
    "\} else \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent] \
    "TclObject argl = TclList.newInstance()\;\n"
    append buffer [emitter_indent] \
    "for (int i = $index; i < objv.length\; i++) \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent] \
    "TclList.append(interp, argl, objv\[i\])\;\n"
    emitter_indent_level -1
    append buffer [emitter_indent] \
    "\}\n"

    set value argl
    append buffer [emitter_statement \
        [compileproc_set_variable "args" true $value false]]

    emitter_indent_level -1
    append buffer [emitter_indent] \
    "\}\n"

    return $buffer
}

# Process proc that has the -compile option set. This method will
# generate a Java class that will just eval the proc body string much
# like the Tcl proc command would.

proc compileproc_nocompile { proc_list class_name } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_nocompile [lindex $proc_list 0] : \{$proc_list\} $class_name"
    }

    if {[llength $proc_list] != 3} {
        error "expected \{PROC_NAME PROC_ARGS PROC_BODY\} : passed [llength $proc_list] args"
    }

    compileproc_init

    set name [lindex $proc_list 0]
    set args [lindex $proc_list 1]
    set body [lindex $proc_list 2]

    set pair [compileproc_split_classname $class_name]
    set _compileproc(package) [lindex $pair 0]
    set _compileproc(classname) [lindex $pair 1]

    set buffer ""

    set buffer ""
    # class comment
    append buffer [emitter_class_comment $name]
    # package statement
    append buffer [emitter_package_name $_compileproc(package)]
    # import statement
    append buffer [emitter_import_tcl]
    # class declaration
    append buffer "\n" [emitter_class_start $_compileproc(classname)]
    # cmdProc declaration
    append buffer [emitter_cmd_proc_start]

    # Emit command initilization
    append buffer [emitter_init_cmd_check]

    # Setup local variable table. The wcmd identifier here
    # is inherited from TJC.CompiledCommand.
    append buffer [emitter_callframe_push wcmd.ns]
    append buffer [emitter_callframe_try]

    # Process proc args
    append buffer [compileproc_args_assign $args]

    # Invoke interp.eval() for proc body
    append buffer [emitter_eval_proc_body $body]

    # end callframe block
    append buffer [emitter_callframe_pop $name]

    # end cmdProc declaration
    append buffer [emitter_cmd_proc_end]

    # Emit class constants
    set cdata [compileproc_constant_cache_generate]
    if {$cdata != {}} {
        append buffer "\n" $cdata
    }

    # Variable cache not supported in -compile mode

    # end class declaration
    append buffer [emitter_class_end $_compileproc(classname)]

    set _compileproc(class) $buffer
    return $_compileproc(class)
}

# Split class name like foo.bar.OneCmd into package and class
# name parts.

proc compileproc_split_classname { class_name } {
    set elems [split $class_name .]
    if {[llength $elems] == 1} {
        return [list default $class_name]
    } else {
        return [list [join [lrange $elems 0 end-1] .] [lindex $elems end]]
    }
}

# Invoked by main module to compile a proc into
# Java source code. This method should catch
# errors raised during compilation and print a
# diagnostic "interal error" type of message to
# indicate where something went wrong.
#
# The filename argument is the name of the Tcl
# file that the proc was defined in. It is used
# in error reporting and is returned in the
# result tuple. Can be "".
#
# The proc_tuple argument is a tuple of:
# {PROC_NAME PROC_JAVA_CLASS_NAME PROC_LIST}
#
# PROC_NAME is the plain name of the Tcl proc
# PROC_JAVA_CLASS_NAME is the short name
#     of the Java class.
# PROC_LIST is a list containing the proc declaration.
#     The list length is 4: like {proc p {} {}}

proc compileproc_entry_point { filename proc_tuple } {
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    set proc_name [lindex $proc_tuple 0]

    if {$debug} {
        puts "compileproc_entry_point $filename $proc_name"
        puts "proc_tuple is \{$proc_tuple\}"
    }

    set package [module_query PACKAGE]
    set compile_option [module_option_value compile]

    if {$debug} {
        puts "compile options is $compile_option"
    }

    set class_name [lindex $proc_tuple 1]

    set proc [lindex $proc_tuple 2]
    set proc_list [lrange $proc 1 end]

    # If -compile or +compile is not set for specific proc, use module setting
    set proc_compile_option [module_option_value compile $proc_name]
    if {$proc_compile_option == {}} {
        set proc_compile_option $compile_option
    }

    if {$proc_compile_option} {
        if {[catch {
            set class_data [compileproc_compile $proc_list $class_name \
                compileproc_query_module_flags]
        } err]} {
            global _tjc

            if {![info exists _tjc(parse_error)] || $_tjc(parse_error) == ""} {
                # Not a handled parse error. Print lots of info.

                puts stderr "Interal error while compiling proc \"$proc_name\" in file $filename"
                #puts stderr "$err"
                # Print stack trace until the call to compileproc_compile is found.
                set lines [split $::errorInfo "\n"]
                foreach line $lines {
                    puts stderr $line
                    if {[string first {compileproc_compile} $line] != -1} {
                        break
                    }
                }
            } else {
                # A parse error that was caught at the source. Print a
                # error that a user might find helpful.
                puts stderr "Parse error while compiling proc \"$proc_name\" in file $filename"
                puts stderr $_tjc(parse_error)
                puts stderr "While parsing script text:"
                puts stderr $_tjc(parse_script)
            }

            return "ERROR"
        }

        if {$debug} {
            puts "generated $class_name data from proc \"$proc_name\""
            puts "class data is:\n$class_data"
        }

        return [list $filename $proc_name $class_name $class_data]
    } else {
        if {[catch {
            set class_data [compileproc_nocompile $proc_list $class_name]
        } err]} {
            puts stderr "Interal error while generating proc \"$proc_name\" in file $filename"
            puts stderr "$err"
            return "ERROR"
        }

        if {$debug} {
            puts "generated $class_name data from proc \"$proc_name\""
            puts "class data is:\n$class_data"
        }

        return [list $filename $proc_name $class_name $class_data]
    }
}


# Generate TJCExtension class that will be included in the JAR.
# The init method of the TJCExtension class will be invoked
# as a result of running the TJC::package command to load
# a TJC compiled package.

proc compileproc_tjcextension { package tcl_files init_file } {
    if {[llength $tcl_files] == 0} {
        error "empty tcl_files argument to compileproc_tjcextension"
    }
    if {$init_file == ""} {
        error "empty string init_file argument to compileproc_tjcextension"
    }

    set buffer ""
    # package statement
    append buffer [emitter_package_name $package]
    # import statement
    append buffer [emitter_import_tcl]

    if {$package == "default"} {
        set prefix "/library/"
    } else {
        set prefix "/"
        foreach elem [split $package .] {
            append prefix $elem "/"
        }
        append prefix "library/"
    }

    append buffer "
public class TJCExtension extends Extension \{
    public void init(Interp interp)
            throws TclException
    \{
        String init_file = \"$init_file\";
        String\[\] files = \{
"

    for {set len [llength $tcl_files] ; set i 0} {$i < $len} {incr i} {
        set fname [lindex $tcl_files $i]
        append buffer \
            "            " \
            "\"$fname\""
        if {$i == ($len - 1)} {
            # No-op
        } else {
            append buffer ","
        }
        append buffer "\n"
    }

    append buffer "        \}\;
        String prefix = \"$prefix\"\;

        TJC.sourceInitFile(interp, init_file, files, prefix)\;
    \}
\}
"

    if {$package == "default"} {
        set full_classname "TJCExtension"
    } else {
        set full_classname "${package}.TJCExtension"
    }

    return [list $full_classname $buffer]
}


# The functions below are used when compiling a proc body into
# a set of commands, words, and inlined methods.

proc compileproc_init {} {
    global _compileproc _compileproc_ckeys _compileproc_key_info
    global _compileproc_command_cache _compileproc_variable_cache

    if {[info exists _compileproc]} {
        unset _compileproc
    }
    if {[info exists _compileproc_ckeys]} {
        unset _compileproc_ckeys
    }
    if {[info exists _compileproc_key_info]} {
        unset _compileproc_key_info
    }
    if {[info exists _compileproc_command_cache]} {
        unset _compileproc_command_cache
    }
    if {[info exists _compileproc_variable_cache]} {
        unset _compileproc_variable_cache
    }

    # Counter for local variable names inside cmdProc scope
    compileproc_tmpvar_reset

    descend_init
    compileproc_constant_cache_init
    emitter_indent_level zero

    # Set to 1 to enabled debug output for each method invocation
    set _compileproc(debug) 0

    # Init variables needed for recursive var and word iteration
    set _compileproc(var_scan_key) {}
    set _compileproc(var_scan_results) {}

    set _compileproc(word_scan_key) {}
    set _compileproc(word_scan_results) {}

    set _compileproc(expr_eval_key) {}
    set _compileproc(expr_eval_buffer) {}
    set _compileproc(expr_eval_expressions) 0

    # Init OPTIONS settings

    # Set to {} if containers should not be inlined.
    # Set to all or a list of containers that should
    # be inlined for fine tuned control while testing.
    set _compileproc(options,inline_containers) {}

    # Is set to 1 if break/continue can be inlined
    # inside loops and if return can be inlined
    # in the method body. The stack records
    # this info for each control scope.
    set _compileproc(options,inline_controls) 0
    set _compileproc(options,controls_stack) {}
    compileproc_push_controls_context proc 0 0 1

    # Is set to 1 if the commands invoked inside
    # a compiled proc are resolved to Command
    # references and cached the first time
    # the containing command is invoked.
    set _compileproc(options,cache_commands) 0

    # Is set to 1 when preserve() and release()
    # should be skipped for constant value arguments.
    set _compileproc(options,skip_constant_increment) 0

    # Is set to 1 if variable access inside a proc
    # makes use of cached Var refrences.
    set _compileproc(options,cache_variables) 0

    # Is set to 1 if built-in Tcl command can
    # be replaced by inline code.
    set _compileproc(options,inline_commands) 0
}

proc compileproc_start { proc_list } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {[llength $proc_list] != 3} {
        error "expected \{PROC_NAME PROC_ARGS PROC_BODY\} : passed [llength $proc_list] args"
    }

    set name [lindex $proc_list 0]
    set args [lindex $proc_list 1]
    set body [lindex $proc_list 2]

    set _compileproc(name) $name
    set _compileproc(args) $args
    set _compileproc(body) $body
    # Keys for commands that appear directly inside the proc body.
    # Commands that are contained inside other commands or are
    # nested arguments do not appear in this list.
    set _compileproc(keys) [list]

    descend_report_command_callback compileproc_command_start_callback start
    descend_report_command_callback compileproc_command_finish_callback finish
    return [descend_start $body]
}

# Return descend command keys for those commands in the first
# level of the proc body. Nested commands or commands inside
# containers are not included in this list.

proc compileproc_keys {} {
    return [descend_commands]
}

# Return info tuple for each key parsed while processing the keys.
# The order of the key info is the parse order.

proc compileproc_keys_info {} {
    global _compileproc_ckeys
    return $_compileproc_ckeys(info_keys)
}

# Return a list of command keys that are children of
# the passed in parent key. If no children exist
# then {} is returned.

proc compileproc_key_children { pkey } {
    global _compileproc _compileproc_ckeys

    if {![info exists _compileproc_ckeys($pkey)]} {
        return {}
    } else {
        return $_compileproc_ckeys($pkey)
    }
}

# Invoked when a new command is being processed. This method
# will determine if the command is at the toplevel of the
# proc or if it is an embedded command or a contained command
# and save the results accordingly.

proc compileproc_command_start_callback { key } {
    global _compileproc _compileproc_ckeys

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_command_start_callback $key"
    }

    set result [descend_get_command_name $key]
    #set container_stack [descend_get_container_stack]

    if {[descend_arguments_undetermined $key]} {
        # Command known to be invoked but arguments not
        # known at compile time. Just ignore.
        return
    } elseif {[lindex $result 0]} {
        # Command name could be determined statically
        set cmdname [lindex $result 1]
        if {$debug} {
            puts "parsed command \"$cmdname\""
        }
    } else {
        set cmdname _UNKNOWN
    }

    set info_token [list]
    lappend info_token $key $cmdname

    lappend _compileproc_ckeys(info_keys) $info_token
    set _compileproc_ckeys($key,info_key) $info_token
}

# Invoked when a command is no longer being processed.

proc compileproc_command_finish_callback { key } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_command_finish_callback $key"
    }
}

# Entry point for all compiled proc variations. This method
# is invoked by compileproc_entry_point and returns a buffer
# containing the generated Java source code.

proc compileproc_compile { proc_list class_name {config_init {}} } {
    global _compileproc
    global _compileproc_key_info

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_compile [lindex $proc_list 0] : \{$proc_list\} $class_name"
    }

    if {[llength $proc_list] != 3} {
        error "expected \{PROC_NAME PROC_ARGS PROC_BODY\} : passed [llength $proc_list] args"
    }

    set name [lindex $proc_list 0]
    set args [lindex $proc_list 1]
    set body [lindex $proc_list 2]

    compileproc_init

    # Invoke module flag query callback

    if {$config_init != {}} {
        namespace eval :: [list $config_init $name]
    }

    compileproc_start $proc_list
    compileproc_scan_keys [compileproc_keys]

    # Process proc args, this needs to be done before
    # emitting the command body so that an argument
    # that makes use of a constant will be handled.

    emitter_indent_level +2
    set args_buffer [compileproc_args_assign $args]
    emitter_indent_level -2

    # Generate Java source.

    set pair [compileproc_split_classname $class_name]
    set _compileproc(package) [lindex $pair 0]
    set _compileproc(classname) [lindex $pair 1]

    set buffer ""
    # class comment
    append buffer [emitter_class_comment $name]
    # package statement
    append buffer [emitter_package_name $_compileproc(package)]
    # import statement
    append buffer [emitter_import_tcl]
    # class declaration
    append buffer "\n" [emitter_class_start $_compileproc(classname)]

    # cmdProc declaration
    append buffer [emitter_cmd_proc_start]

    # If the command needs to be initialized, then do that
    # the first time the cmdProc is invoked. There may
    # be cases where we emit this check but the command
    # does not actually init any constants or commands.
    if {$_compileproc_key_info(cmd_needs_init)} {
        if {$_compileproc(options,inline_commands)} {
            lappend flags {inlineCmds}
        } else {
            set flags {}
        }
        append buffer [emitter_init_cmd_check $flags]
    }

    # Setup local variable table. The wcmd identifier here
    # is inherited from TJC.CompiledCommand.
    append buffer [emitter_callframe_push wcmd.ns]

    set body_bufer ""

    # Start try block
    append body_buffer [emitter_callframe_try]

    # Process proc args
    append body_buffer $args_buffer

    # Walk over commands at the toplevel and emit invocations
    # for each toplevel command.

    foreach key [compileproc_keys] {
        append body_buffer [compileproc_emit_invoke $key]
    }

    # If cached variables were used, then clear the cached
    # variable table just after pushing the new call frame.

    if {[compileproc_variable_cache_is_used]} {
        append buffer [emitter_statement "updateVarCache(interp, 0)"]
    }
    append buffer $body_buffer

    # end callframe block
    set clear_varcache [compileproc_variable_cache_is_used]
    append buffer [emitter_callframe_pop $name $clear_varcache]

    # end cmdProc declaration
    append buffer [emitter_cmd_proc_end]

    # Emit constant TclObject values and an initConstants() method.
    # It is possible that constant were found while scanning but
    # none were actually used, so this needs to appear after
    # the cmdProc() method has been emitted.

    if {$_compileproc_key_info(constants_found)} {
        set cdata [compileproc_constant_cache_generate]
        if {$cdata != {}} {
            append buffer "\n" $cdata
        }
    }

    # Emit code needed to cache command refrences.

    if {$_compileproc(options,cache_commands)} {
        set cdata [compileproc_command_cache_init_generate]
        if {$cdata != ""} {
            append buffer "\n" $cdata
        }
    }

    # Emit variable cache methods

    if {$_compileproc(options,cache_variables) && \
            [compileproc_variable_cache_is_used]} {
        append buffer "\n" [compileproc_variable_cache_generate]
    }

    # end class declaration
    append buffer [emitter_class_end $_compileproc(classname)]

    # Top of controls context stack should be proc context
    compileproc_pop_controls_context proc

    set _compileproc(class) $buffer
    return $_compileproc(class)
}

# Reset the compiled in constant cache for a given proc.

proc compileproc_constant_cache_init {} {
    global _compileproc_constant_cache

    if {[info exists _compileproc_constant_cache]} {
        unset _compileproc_constant_cache
    }

    set _compileproc_constant_cache(ordered_keys) {}
}

# Add a constant Tcl string value to the constant cache.

proc compileproc_constant_cache_add { tstr } {
    global _compileproc_constant_cache

    set key const,$tstr

    if {![info exists _compileproc_constant_cache($key)]} {
        set ident {}
        set type [compileproc_constant_cache_type $tstr]
        set tuple [list $ident $type $tstr]
        set _compileproc_constant_cache($key) $tuple
    }
    return
}

# Determine the type for a constant TclObject based on
# what type the string looks like. Note that this
# implementation will determine the type based on
# integer and double ranges that are valid in Java.

proc compileproc_constant_cache_type { tstr } {
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_constant_cache_type \"$tstr\""
    }

    if {$tstr == ""} {
        return STRING
    } elseif {$tstr == "false" || $tstr == "true"} {
        if {$debug} {
            puts "string \"$tstr\" looks like an BOOLEAN"
        }
        return BOOLEAN
    } elseif {[compileproc_string_is_java_integer $tstr]} {
        if {$debug} {
            puts "string \"$tstr\" looks like an INTEGER"
        }
        return INTEGER
    } elseif {[compileproc_string_is_java_double $tstr]} {
        if {$debug} {
            puts "string \"$tstr\" looks like a DOUBLE"
        }
        return DOUBLE
    } else {
        if {$debug} {
            puts "string \"$tstr\" must be a STRING"
        }
        return STRING
    }
}

# Return a class instance scoped reference for the
# given constant Tcl string. Note that a constant
# added to the pool will not actually appear in
# the Java file unless this method is invoked
# for that constant.

# FIXME: Write some tests for string that are the
# same after any backslash and output subst done
# in the emitter layer. Should not have duplicated
# constant strings in the cache.

proc compileproc_constant_cache_get { tstr } {
    global _compileproc_constant_cache

    set key const,$tstr

    if {![info exists _compileproc_constant_cache($key)]} {
        error "constant cache variable not found for\
            \"$tstr\", should have been setup via compileproc_constant_cache_add"
    }

    set tuple $_compileproc_constant_cache($key)
    set ident [lindex $tuple 0]
    if {$ident == {}} {
        # Generate name for instance variable and
        # save it so that this constant will appear
        # in the output pool.

        set type [lindex $tuple 1]
        set tstr [lindex $tuple 2]

        if {![info exists _compileproc_constant_cache(const_id)]} {
            set _compileproc_constant_cache(const_id) 0
        } else {
            incr _compileproc_constant_cache(const_id)
        }
        set ident "const$_compileproc_constant_cache(const_id)"
        set tuple [list $ident $type $tstr]
        set _compileproc_constant_cache($key) $tuple
        lappend _compileproc_constant_cache(ordered_keys) $key
    }
    return $ident
}

# Generate code to setup constant TclObject instance
# variables. These are used when a constant word
# value in a Tcl proc is used over and over again.

proc compileproc_constant_cache_generate {} {
    global _compileproc_constant_cache

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    set tlist [list]

    foreach key $_compileproc_constant_cache(ordered_keys) {
        set tuple $_compileproc_constant_cache($key)

        if {$debug} {
            puts "processing key $key, tuple is \{$tuple\}"
        }

        set ident [lindex $tuple 0]
        if {$ident == {}} {
            # Skip unused constant
            continue
        }

        lappend tlist $tuple
    }

    # If no constants were actually used, nothing to generate
    if {$tlist == {}} {
        return {}
    }

    return [emitter_init_constants $tlist]
}

# The list of all commands that could be invoked during
# this method is iterated here to create a large switch
# method to update the command cache. Command names
# are resolved into command refrences that are checked
# on a per-instance basis.

proc compileproc_command_cache_init_generate {} {
    global _compileproc
    global _compileproc_command_cache

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_command_cache_init_generate"
    }

    if {![info exists _compileproc_command_cache(ordered_cmds)]} {
        return ""
    }

    if {[emitter_indent_level] != 0} {
        error "expected enter indent level of 0, got [emitter_indent_level]"
    }

    set buffer ""

    emitter_indent_level +1

    # Emit cmdEpoch for this command
    append buffer \
        [emitter_statement "int wcmd_cmdEpoch = 0"]

    # Emit instance scoped variables that hold a Command reference.
    set symbol_ids [list]

    foreach cmdname $_compileproc_command_cache(ordered_cmds) \
            key $_compileproc_command_cache(ordered_keys) {
        set symbol $_compileproc_command_cache($key)

        set cacheId [compileproc_get_cache_id_from_symbol $symbol]

        lappend symbol_ids $cacheId

        append buffer \
            [emitter_statement "WrappedCommand $symbol"] \
            [emitter_statement "int ${symbol}_cmdEpoch"]
    }

    append buffer \
        "\n" \
        [emitter_indent] \
        "void updateCmdCache(Interp interp, int cacheId) throws TclException \{\n"

    emitter_indent_level +1

    append buffer \
        [emitter_statement "String cmdName"]

    # Emit switch on cacheId to determine command name

    append buffer [emitter_indent] \
        "switch ( cacheId ) \{\n"

    emitter_indent_level +1

    # Special case for id 0, it will update all the commands
    # and reset wcmd_cmdEpoch.
    set cacheId 0

    append buffer [emitter_indent] \
        "case $cacheId: \{\n"

    emitter_indent_level +1

    foreach cmdname $_compileproc_command_cache(ordered_cmds) \
            key $_compileproc_command_cache(ordered_keys) \
            cacheId $symbol_ids {
        set symbol $_compileproc_command_cache($key)

        append buffer \
            [emitter_statement "$symbol = TJC.INVALID_COMMAND_CACHE"] \
            [emitter_statement "${symbol}_cmdEpoch = 0"]
    }

    append buffer \
        [emitter_statement "wcmd_cmdEpoch = wcmd.cmdEpoch"] \
        [emitter_statement "return"]

    emitter_indent_level -1

    # End switch case
    append buffer [emitter_indent] \
        "\}\n"

    # Resolve command names in namespace the command is defined in.

    foreach cmdname $_compileproc_command_cache(ordered_cmds) \
            key $_compileproc_command_cache(ordered_keys) \
            cacheId $symbol_ids {
        set symbol $_compileproc_command_cache($key)

        append buffer [emitter_indent] \
            "case $cacheId: \{\n"

        emitter_indent_level +1

        set jsym [emitter_double_quote_tcl_string $cmdname]

        append buffer \
            [emitter_statement "cmdName = $jsym"] \
            [emitter_statement "break"]

        emitter_indent_level -1

        # End switch case
        append buffer [emitter_indent] \
            "\}\n"
    }

    # Emit default block, this branch would never be taken.

    append buffer [emitter_indent] \
        "default: \{\n"

    emitter_indent_level +1

    append buffer \
        [emitter_statement \
        "throw new TclRuntimeError(\"default: cacheId \" + cacheId)"]

    emitter_indent_level -1

    # End default switch case
    append buffer [emitter_indent] \
        "\}\n"

    emitter_indent_level -1

    # End switch block

    append buffer [emitter_indent] \
        "\}\n"

    # Allocate locals to hold command ref and epoch

    append buffer \
        [emitter_statement \
            "WrappedCommand lwcmd = TJC.resolveCmd(interp, cmdName)"] \
        [emitter_statement "int cmdEpoch"] \
        [emitter_container_if_start "lwcmd == null"] \
        [emitter_statement "lwcmd = TJC.INVALID_COMMAND_CACHE"] \
        [emitter_statement "cmdEpoch = 0"] \
        [emitter_container_if_else] \
        [emitter_statement "cmdEpoch = lwcmd.cmdEpoch"] \
        [emitter_container_if_end]

    # Emit switch on cacheId to assign cache variable

    append buffer [emitter_indent] \
        "switch ( cacheId ) \{\n"

    emitter_indent_level +1

    foreach cmdname $_compileproc_command_cache(ordered_cmds) \
            key $_compileproc_command_cache(ordered_keys) \
            cacheId $symbol_ids {
        set symbol $_compileproc_command_cache($key)

        append buffer [emitter_indent] \
            "case $cacheId: \{\n"

        emitter_indent_level +1

        append buffer \
            [emitter_statement "$symbol = lwcmd"] \
            [emitter_statement "${symbol}_cmdEpoch = cmdEpoch"] \
            [emitter_statement "break"]

        emitter_indent_level -1

        # End switch case
        append buffer [emitter_indent] \
            "\}\n"
    }

    emitter_indent_level -1

    # End switch block

    append buffer [emitter_indent] \
        "\}\n"

    emitter_indent_level -1

    # End updateCmdCache

    append buffer [emitter_indent] \
        "\}\n"

    emitter_indent_level -1

    if {[emitter_indent_level] != 0} {
        error "expected exit indent level of 0, got [emitter_indent_level]"
    }

    return $buffer
}

# Return a buffer that checks to see if a WrappedCommand ref
# is still valid and returns a Command value (or null).

proc compileproc_command_cache_epoch_check { symbol } {
    set buffer ""

    append buffer \
        "((${symbol}_cmdEpoch == ${symbol}.cmdEpoch)\n"

    emitter_indent_level +1

    append buffer [emitter_indent] \
        "? ${symbol}.cmd : null)"

    emitter_indent_level -1

    return $buffer
}

# If a cached command is no longer valid, try to update
# the cached value by looking the command up again.
# This method will also check to see if the containing
# command's cmdEpoch was changed and flush all the
# cached symbols in that case.

proc compileproc_command_cache_update { symbol } {
    set buffer ""

    set if_cond "${symbol}_cmdEpoch != ${symbol}.cmdEpoch"

    append buffer [emitter_container_if_start $if_cond]

    set cacheId [compileproc_get_cache_id_from_symbol $symbol]

    append buffer \
        [emitter_statement "updateCmdCache(interp, $cacheId)"] \
        [emitter_container_if_end]

    return $buffer
}

# Given "cmdcache1" return integer cache id "1".

proc compileproc_get_cache_id_from_symbol { symbol } {
    # Get cache id number from symbol
    if {![regexp {^[a-z]+cache([0-9]+)$} $symbol whole cacheId]} {
        error "could not match cache id in \"$symbol\""
    }
    return $cacheId
}

# Emit code to check the command epoch
# for the "this" command that contains other
# cached commands. The this epoch could
# be changed when a command is renamed
# or moved into another namespace. When
# the this command epoch is changed, all
# cached commands inside this command
# should be flushed. This check needs to
# be done before a specific cached command's
# epoch is checked.

proc compileproc_command_cache_this_check {} {
    set cond {wcmd_cmdEpoch != wcmd.cmdEpoch}

    set buffer ""

    append buffer \
        [emitter_container_if_start $cond] \
        [emitter_statement "updateCmdCache(interp, 0)"] \
        [emitter_container_if_end]

    return $buffer
}

# Lookup a command cache symbol given a command key.
# The order which the commands appear in the proc
# define what order the commands are initialized in.

proc compileproc_command_cache_lookup { dkey } {
    global _compileproc_command_cache
    global _compileproc_ckeys

    if {![info exists _compileproc_command_cache(counter)]} {
        set _compileproc_command_cache(counter) 1
    }

    # Determine the name of the command, if the command
    # name can't be determined statically just return
    # {} so that no cache will be used.

    set tuple [compileproc_argument_printable $dkey 0]
    set type [lindex $tuple 0]
    if {$type != "constant"} {
        return {}
    }

    # The command name is a constant string. Find
    # the actual name of the command. There is no
    # way to know how the runtime will resolve
    # different commands with namespace qualifiers
    # and so on, so a command string must match
    # exactly to use the same cache value.

    set cmdname [lindex $_compileproc_ckeys($dkey,info_key) 1]

    set key "symbol,$cmdname"

    if {![info exists _compileproc_command_cache($key)]} {
        # Create cache symbol for new command
        set symbol "cmdcache$_compileproc_command_cache(counter)"
        incr _compileproc_command_cache(counter)
        set _compileproc_command_cache($key) $symbol
        lappend _compileproc_command_cache(ordered_keys) $key
        lappend _compileproc_command_cache(ordered_cmds) $cmdname
    } else {
        # Return existing symbol for this command
        set symbol $_compileproc_command_cache($key)
    }

    return $symbol
}

# Lookup a cached scalar variable by name. This
# method is used to get a token for a scalar
# variable read or write operation. This method
# assumes that the passed in vname is a constant str.

proc compileproc_variable_cache_lookup { vname } {
    global _compileproc_variable_cache

    if {![info exists _compileproc_variable_cache(counter)]} {
        set _compileproc_variable_cache(counter) 1
    }

    set key "symbol,$vname"

    if {![info exists _compileproc_variable_cache($key)]} {
        # Create cache symbol for new scalar variable
        set symbol "varcache$_compileproc_variable_cache(counter)"
        incr _compileproc_variable_cache(counter)
        set _compileproc_variable_cache($key) $symbol
        lappend _compileproc_variable_cache(ordered_keys) $key
        lappend _compileproc_variable_cache(ordered_vars) $vname
    } else {
        # Return existing symbol for this variable name
        set symbol $_compileproc_variable_cache($key)
    }

    return $symbol
}

# Return 1 if there are cached variables, this method is used
# to detect when cached variable support should be enabled
# in the generated code.

proc compileproc_variable_cache_is_used {} {
    global _compileproc_variable_cache

    if {![info exists _compileproc_variable_cache(ordered_keys)]} {
        return 0
    }

    return 1
}

# Generate code to setup variable cache refrences as well
# as update and validate cached variable values.

proc compileproc_variable_cache_generate {} {
    global _compileproc_variable_cache

    if {![compileproc_variable_cache_is_used]} {
        return ""
    }
    return [compileproc_variable_cache_update_generate]
}

# Return code that implements "updateVarCache" method
# for the current variable cache info.

proc compileproc_variable_cache_update_generate {} {
    global _compileproc
    global _compileproc_variable_cache

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    set buffer ""

    set cacheIds [list]

    # declare class scoped cache vars

    emitter_indent_level +1

    foreach key $_compileproc_variable_cache(ordered_keys) {
        set symbol $_compileproc_variable_cache($key)
        append buffer [emitter_statement "Var $symbol = null"]

        set cacheId [compileproc_get_cache_id_from_symbol $symbol]
        lappend cacheIds $cacheId
    }

    set decl \
"    protected
    Var updateVarCache(
        Interp interp,
        int cacheId)
    \{
        String part1;
        String part2 = null;
        int flags = 0;
        Var lvar;
"

    set decl_end "    \}\n"

    append buffer \
        "\n" \
        $decl \
        "\n"

    emitter_indent_level +1

    # first switch on cacheId

    append buffer [emitter_indent] "switch ( cacheId ) \{\n"
    emitter_indent_level +1

    append buffer [emitter_indent] "case 0: \{\n"
    emitter_indent_level +1

    # init all cache symbols to null

    foreach key $_compileproc_variable_cache(ordered_keys) {
        set symbol $_compileproc_variable_cache($key)
        append buffer [emitter_statement "$symbol = null"]
    }
    append buffer [emitter_statement "return null"]
    emitter_indent_level -1
    append buffer [emitter_indent] "\}\n"

    # case block for each id

    foreach vname $_compileproc_variable_cache(ordered_vars) \
            key $_compileproc_variable_cache(ordered_keys) \
            cacheId $cacheIds {
        set symbol $_compileproc_variable_cache($key)

        append buffer [emitter_indent] "case $cacheId: \{\n"
        emitter_indent_level +1

        set jstr [emitter_backslash_tcl_string $vname]
        append buffer \
            [emitter_statement "part1 = \"$jstr\""] \
            [emitter_statement "break"]

        emitter_indent_level -1
        append buffer [emitter_indent] "\}\n"
    }

    append buffer [emitter_indent] "default: \{\n"
    emitter_indent_level +1
    append buffer [emitter_statement "throw new TclRuntimeError(\"default: cacheId \" + cacheId)"]
    emitter_indent_level -1
    append buffer [emitter_indent] "\}\n"

    # end second switch

    emitter_indent_level -1
    append buffer [emitter_indent] "\}\n"

    # resolve var

    append buffer \
        "\n" \
        [emitter_statement "lvar = TJC.resolveVarScalar(interp, part1, flags)"] \
        "\n"

    # second switch on cacheId

    append buffer [emitter_indent] "switch ( cacheId ) \{\n"
    emitter_indent_level +1

    # case block for each id

    foreach vname $_compileproc_variable_cache(ordered_vars) \
            key $_compileproc_variable_cache(ordered_keys) \
            cacheId $cacheIds {
        set symbol $_compileproc_variable_cache($key)

        append buffer [emitter_indent] "case $cacheId: \{\n"
        emitter_indent_level +1

        append buffer \
            [emitter_statement "$symbol = lvar"] \
            [emitter_statement "break"]

        emitter_indent_level -1
        append buffer [emitter_indent] "\}\n"
    }

    # end second switch

    emitter_indent_level -1
    append buffer [emitter_indent] "\}\n"

    append buffer [emitter_statement "return lvar"]

    emitter_indent_level -1

    # end method decl

    append buffer [emitter_indent] "\}\n"

    emitter_indent_level -1

    return $buffer
}

# Loop over parsed command keys and determine information
# about each command and its arguments. Scanning the commands
# generates meta-data about the parse trees that is then
# used to generate with specific optimizations. The scan
# starts with the toplevel keys for a specific proc and
# descends into all the keys that are children of these
# toplevel keys. The scan will also descend into container
# commands if that option is enabled.

proc compileproc_scan_keys { keys } {
    global _compileproc _compileproc_key_info

#    set debug 0
#    if {$::_compileproc(debug)} {set debug 1}
#
#    if {$debug} {
#        puts "compileproc_scan_keys: $keys"
#    }

    if {[info exists _compileproc_key_info(cmd_needs_init)]} {
        set cmd_needs_init $_compileproc_key_info(cmd_needs_init)
    } else {
        set cmd_needs_init 0
    }

    if {[info exists _compileproc_key_info(constants_found)]} {
        set constants_found $_compileproc_key_info(constants_found)
    } else {
        set constants_found 0
    }

    # Create lookup table for command arguments. Figure out
    # types and values for each word as we iterate over the
    # command keys. The first time compileproc_scan_keys
    # in invoked, the toplevel keys in the proc are passed.

    foreach key $keys {
        # If the command name is a simple string, then get
        # the command name String.
        if {[descend_arguments_undetermined $key]} {
            error "unexpected undetermined arguments command key \"$key\""
        }
        set script [descend_get_data $key script]
        set tree [descend_get_data $key tree]
        set num_args [llength $tree]
        if {$num_args < 1} {error "num args ($num_args) must be positive"}

#        if {$debug} {
#            puts "key is $key"
#            puts "script is ->$script<-"
#            puts "tree is \{$tree\}"
#        }

        set types [list] ; # type of value for argument
        set values [list] ; # value depends on type
        set cmaps [list] ; # map (orignal -> values) chars per character
        set instrs [list] ; # original Tcl string for argument

        # Get list of the keys for nested commands in each argument
        # to this command.
        set argument_commands [descend_commands $key]
        if {[llength $argument_commands] != $num_args} {
            # A command can't have zero arguments, so {} should not be returned by
            # descend_commands here. The descend module inits the commands flag
            # to a list of empty lists based on the number of arguments to the command.
            error "mismatched num_args ($num_args) and num argument_commands\
                [llength $argument_commands] for key $key, argument_commands
                is \{$argument_commands\}"
        }

        # Walk over each argument to the command looking for constant
        # TclObject values and register any that are found.
        set i 0
        foreach telem $tree {
            # See if this argument is a constant (simple/text) type
            # and create a class constant if it is one.
            if {[parse_is_simple_text $telem]} {
                set qtext [parse_get_simple_text $script $telem]
                if {[string index $qtext 0] == "\{" &&
                    [string index $qtext end] == "\}"} {
                    set brace_quoted 1
                } else {
                    set brace_quoted 0
                }
#                if {$debug} {
#                    puts "found simple/text ->$qtext<- at argument index $i"
#                    if {$brace_quoted} {
#                        puts "argument is a brace quoted"
#                    } else {
#                        puts "argument is not a brace quoted"
#                    }
#                }
                set uqtext [parse_get_simple_text $script $telem "text"]
#                if {$debug} {
#                    puts "found unquoted simple/text ->$uqtext<- at argument index $i"
#                }

                # A brace quoted argument like {foo\nbar} must be written
                # as the Java String "foo\\nbar". Find all backslash
                # chars and double backslash them. Also, save a map
                # of original Tcl characters to backslashed characters.

                if {$brace_quoted && [string first "\\" $uqtext] != -1} {
                    set cmap {}
                    set bs_uqtext ""

                    set len [string length $uqtext]
                    for {set i 0} {$i < $len} {incr i} {
                        set c [string index $uqtext $i]
                        if {$c == "\\"} {
                            append bs_uqtext "\\\\"
                            lappend cmap 2
                        } else {
                            append bs_uqtext $c
                            lappend cmap 1
                        }
                    }

#                    if {$debug} {
#                        puts "doubled up escapes in brace quoted string"
#                        puts "uqtext    ->$uqtext<-"
#                        puts "bs_uqtext ->$bs_uqtext<-"
#                        puts "cmap is \{$cmap\}"
#                    }

                    set uqtext $bs_uqtext
                } else {
                    set cmap {}
                }

                set constants_found 1
                lappend types constant
                lappend values $uqtext
                lappend instrs $qtext
                lappend cmaps $cmap
                compileproc_constant_cache_add $uqtext
            } elseif {[parse_is_word_variable $telem]} {
                # A word that contains a single variable can
                # have multiple types. The most simple is
                # a scalar. More complex types like arrays
                # will require a full evaluation.

#                if {$debug} {
#                    puts "found word/variable type at argument index $i"
#                }

                set commands [lindex $argument_commands $i]
                if {$commands != {}} {
                    # An array key contains commands
                    compileproc_childkey_reset $key $commands
                }

                set vstree [lindex $telem 2 0]
                set vinfo [compileproc_scan_variable $key $script $vstree]
                if {$commands != {}} {
                    compileproc_childkey_validate $key
                }

                lappend types variable
                lappend values $vinfo
                lappend instrs [parse_get_word_variable $script $telem]
                lappend cmaps {}
            } elseif {[parse_is_word_command $telem]} {
                # A word element that contains 0 to N nested
                # commands. Loop over each of the keys for
                # each nested command.

#                if {$debug} {
#                    puts "found word/command type: [parse_get_word_command $script $telem] at argument index $i"
#                }
                set commands [lindex $argument_commands $i]
                if {[compileproc_is_empty_command $commands]} {
                    # An empty command has no key. The result of an empty
                    # command is the empty list, so just pretend this is
                    # a constant ref to {}.
                    set uqtext {}
                    set constants_found 1
                    lappend types constant
                    lappend values $uqtext
                    lappend instrs {[]}
                    lappend cmaps {}
                    compileproc_constant_cache_add $uqtext
#                    if {$debug} {
#                        puts "found empty word/command, subst {} at argument index $i"
#                    }
                } else {
#                    if {$debug} {
#                        puts "found word/command keys: $commands at argument index $i"
#                    }

                    lappend types command
                    lappend values $commands
                    lappend instrs [parse_get_word_command $script $telem]
                    lappend cmaps {}
#                    if {$debug} {
#                    puts "scanning commands \{$commands\} (child of $key)"
#                    }
                    compileproc_scan_keys $commands
#                    if {$debug} {
#                    puts "done scanning commands \{$commands\} (child of $key)"
#                    }
                }
            } elseif {[parse_is_word $telem]} {
                # A word element made up of text, variables, and or commands

#                if {$debug} {
#                    puts "found word element type at index $i"
#                }

                set wrange [lindex $telem 1]
                set qtext [parse getstring $script $wrange]

                set commands [lindex $argument_commands $i]
                if {$commands != {}} {
                    # An array key that contains commands
                    compileproc_childkey_reset $key $commands
                }

                set wstree $telem
                set type_winfo [compileproc_scan_word $key $script $wstree]
                if {$commands != {}} {
                    compileproc_childkey_validate $key
                }
                set type [lindex $type_winfo 0]
                set value [lindex $type_winfo 1]
                set cmap [lindex $type_winfo 2]

                if {$type == "constant"} {
                    set constants_found 1
                    compileproc_constant_cache_add $value
                }
                lappend types $type
                lappend values $value
                lappend instrs $qtext
                lappend cmaps $cmap
            } else {
                error "unsupported type at argument index $i, telem is: \{$telem\}"
            }

            incr i
        }

        if {[llength $types] != [llength $values]} {
            error "num types vs values mismatch, [llength $types] != [llength $values]"
        }
        if {[llength $types] != $num_args} {
            error "num types vs num_args mismatch, [llength $types] != $num_args"
        }

        set _compileproc_key_info($key,types) $types
        set _compileproc_key_info($key,values) $values
        set _compileproc_key_info($key,instrs) $instrs
        set _compileproc_key_info($key,num_args) $num_args
        set _compileproc_key_info($key,cmaps) $cmaps

        # If inlining of containers is enabled and this
        # container can be inlined, then scan the
        # commands contained inside it.

        if {[compileproc_is_container_command $key] &&
                [compileproc_can_inline_container $key]} {
#            if {$debug} {
#            puts "descend into container keys for command $key"
#            }
            set ccmds [descend_commands $key container]
#            if {$debug} {
#            puts "container keys for command $key are \{$ccmds\}"
#            }
            set klist [list]
            foreach list [descend_commands $key container] {
                if {[llength $list] == 1} {
                    lappend klist [lindex $list 0]
                } else {
                    foreach elem $list {
                        if {$elem != {}} {
                            lappend klist $elem
                        }
                    }
                }
            }
            if {$klist != {}} {
#                if {$debug} {
#                puts "scanning container commands \{$klist\} (child of $key)"
#                }
                compileproc_scan_keys $klist
#                if {$debug} {
#                puts "done scanning container commands \{$klist\} (child of $key)"
#                }
            }
        }
    } ; # end foreach key $keys loop

    if {$constants_found} {
        set cmd_needs_init 1
#        if {$debug} {
#            puts "compileproc_scan_keys: key_info printout"
#            parray _compileproc_key_info
#        }
    }

    set _compileproc_key_info(cmd_needs_init) $cmd_needs_init
    set _compileproc_key_info(constants_found) $constants_found
}

# Returns true if the given dkey is an empty command, meaning
# it has no child keys.

proc compileproc_is_empty_command { keys } {
    #if {$keys == {} || $keys == {{}}}
    if {$keys == {}} {
        return 1
    } else {
        return 0
    }
}

# Reset child key counter and list of child keys.
# This list is used when iterating over a variable
# or a word that is an argument to a the command
# denoted by the key argument.

proc compileproc_childkey_reset { key ckeys } {
    global _compileproc _compileproc_ckeys
    set _compileproc_ckeys($key) $ckeys
    set _compileproc($key,childkey_counter) 0
    return
}

# Return the next child key for the given key and
# increment the child key counter.

proc compileproc_childkey_next { key } {
    global _compileproc
    set children [compileproc_key_children $key]
    set len [llength $children]
    if {$_compileproc($key,childkey_counter) >= $len} {
        error "no more children for $key, children are \{$children\}, index is\
            $_compileproc($key,childkey_counter)"
    }
    set ckey [lindex $children $_compileproc($key,childkey_counter)]
    incr _compileproc($key,childkey_counter)
    return $ckey
}

proc compileproc_childkey_validate { key } {
    global _compileproc
    set children [compileproc_key_children $key]
    set expected_children [llength $children]
    set num_processed $_compileproc($key,childkey_counter)

    if {$_compileproc($key,childkey_counter) != $expected_children} {
        error "expected $expected_children children, but processed\
            $num_processed for key $key, children are \{$children\}"
    }
}

# Given a variable subtree and the script it was generated from,
# scan the contents of the variable and return a list that
# describes how to evaluate the variable starting from the
# inner most value and progressing to the outer most one.

proc compileproc_scan_variable { key script vstree } {
    global _compileproc
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {[parse_is_scalar_variable $vstree]} {
        set vname [parse_get_scalar_variable $script $vstree]
        set vinfo [list scalar $vname]

        if {$debug} {
            puts "found scalar variable type"
        }
    } else {
        # Parse array variable into a vinfo list that is
        # evaluated by compileproc_emit_variable.

        set saved_var_scan_key $_compileproc(var_scan_key)
        set saved_var_scan_results $_compileproc(var_scan_results)
        set _compileproc(var_scan_key) $key
        set _compileproc(var_scan_results) {}

        parse_variable_iterate $script $vstree \
            _compileproc_scan_variable_iterator

        set vinfo $_compileproc(var_scan_results)
        set _compileproc(var_scan_key) $saved_var_scan_key
        set _compileproc(var_scan_results) $saved_var_scan_results

        if {$debug} {
            puts "found array variable type"
        }
    }

    return $vinfo
}

# Callback invoked while variable elements are being scanned. Variables
# are fully scanned before any code is generated.

proc _compileproc_scan_variable_iterator { script stree type values ranges } {
    global _compileproc
    upvar #0 _compileproc(var_scan_results) results

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "_compileproc_scan_variable_iterator : \{$type\} \{$values\} \{$ranges\}"
    }

    # Use array type info from parse layer to decide how to
    # handle the variable.

    switch -exact -- $type {
        {scalar} {
            # Scalar variable: $v
            lappend results $type [lindex $values 0]
        }
        {array text} {
            # Array with a text string key: $a(k)
            lappend results $type $values
        }
        {array scalar} {
            # Array with a scalar variable key: $a($k)
            lappend results $type $values
        }
        {array command} {
            # Array with a command key: $a([cmd])
            set key $_compileproc(var_scan_key)
            set ckeys [compileproc_childkey_next $key]
            if {[compileproc_is_empty_command $ckeys]} {
                # An empty command evaluates to the empty list,
                # in an array variable word this is a key
                # that is the empty string.
                set ckeys {}
            } else {
                if {$debug} {
                puts "scanning child keys \{$ckeys\} (child of $key)"
                }
                # There does not appear to be a way for an {array command}
                # to have anything other than a flat list of keys, so
                # no need to loop over keys looking for {}.
                compileproc_scan_keys $ckeys
                if {$debug} {
                puts "done scanning child keys \{$ckeys\} (child of $key)"
                }
            }
            lappend results $type [list [lindex $values 0] $ckeys]
        }
        {array word} {
            # complex array key case, either a word made
            # up of text, command, and variable elements
            # or an array key that is itself an array.
            lappend results $type $values
        }
        {word begin} {
            # Begin processing word elements for complex
            # word as array key
        }
        {word end} {
            # End processing of word elements for complex
            # word as array key
        }
        {word text} {
            # word element that is a text string

            set word [parse_variable_iterate_word_value]
            lappend word [list "text" [lindex $values 0]]
            parse_variable_iterate_word_value $word
        }
        {word scalar} {
            # word element that is a scalar variable

            set word [parse_variable_iterate_word_value]
            lappend word [list scalar [lindex $values 0]]
            parse_variable_iterate_word_value $word
        }
        {word command} {
            # word element that is a command
            set key $_compileproc(var_scan_key)
            set ckeys [compileproc_childkey_next $key]
            if {[compileproc_is_empty_command $ckeys]} {
                # An empty command evaluates to the empty list,
                # in an array variable word this is a key
                # that is the empty string.
                set ckeys {}
                compileproc_constant_cache_add {}
            } else {
                if {$debug} {
                puts "scanning child keys \{$ckeys\} (child of $key)"
                }
                compileproc_scan_keys $ckeys
                if {$debug} {
                puts "done scanning child keys \{$ckeys\} (child of $key)"
                }
            }

            set word [parse_variable_iterate_word_value]
            lappend word [list $type $ckeys]
            parse_variable_iterate_word_value $word
        }
        {word array text} {
            # word element that is an array variable with a text key

            set word [parse_variable_iterate_word_value]
            lappend word [list {array text} $values]
            parse_variable_iterate_word_value $word
        }
        {word array scalar} {
            # word element that is an array variable with a scalar key

            set word [parse_variable_iterate_word_value]
            lappend word [list {array scalar} $values]
            parse_variable_iterate_word_value $word
        }
        {word array command} {
            # word element that is an array variable with a command key
            set key $_compileproc(var_scan_key)
            set ckeys [compileproc_childkey_next $key]
            if {[compileproc_is_empty_command $ckeys]} {
                # An empty command evaluates to the empty list,
                # in an array variable word this is a key
                # that is the empty string.
                set ckeys {}
                compileproc_constant_cache_add {}
            } else {
                if {$debug} {
                puts "scanning child key \{$ckeys\} (child of $key)"
                }
                compileproc_scan_keys $ckeys
                if {$debug} {
                puts "done scanning child key \{$ckeys\} (child of $key)"
                }
            }

            set word [parse_variable_iterate_word_value]
            lappend word [list {array command} \
                [list [lindex $values 0] $ckeys]]
            parse_variable_iterate_word_value $word
        }
        {word array word} {
            # word element that is an array with a word key

            set word [parse_variable_iterate_word_value]
            lappend word [list {array word} $values]
            parse_variable_iterate_word_value $word
        }
        default {
            error "unknown variable type \{$type\}"
        }
    }
}

# Scan contents of a word subtree and return a formatted
# description of the word elements.

proc compileproc_scan_word { key script wstree } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_scan_word $key \{$script\} \{$wstree\}"
    }

    # Parse word elements into a list that describes
    # each word element.

    set saved_word_scan_key $_compileproc(word_scan_key)
    set saved_word_scan_results $_compileproc(word_scan_results)
    set _compileproc(word_scan_key) $key
    set _compileproc(word_scan_results) {}

    parse_word_iterate $script $wstree _compileproc_scan_word_iterate

    set winfo $_compileproc(word_scan_results)
    set _compileproc(word_scan_key) $saved_word_scan_key
    set _compileproc(word_scan_results) $saved_word_scan_results

    if {$debug} {
        puts "checking returned word info \{$winfo\}"
    }

    # If each element is a {text} element, then combine them
    # all into a single constant string and return a constant
    # string type. This can happen when a backslash is found
    # inside an otherwise constant string.
    # Return a word type if a non-text element is found.

    set all_text 1
    foreach wi $winfo {
        set type [lindex $wi 0]
        if {$type != "text"} {
            set all_text 0
            break
        }
    }
    if {$all_text} {
        if {$debug} {
            puts "all word types were {text}, returning constant string"
        }
        set all_str ""
        set has_escapes 0
        set cmap [list]
        foreach wi $winfo {
            set str [lindex $wi 1]
            append all_str $str

            if {[string index $str 0] == "\\"} {
                set has_escapes 1
                lappend cmap [string length $str]
            } else {
                # Not an escape string, add a map
                # entry for each plain character.
                set len [string length $str]
                for {set i 0} {$i < $len} {incr i} {
                    lappend cmap 1
                }
            }
        }
        # Don't bother with map for string with no escapes.
        if {!$has_escapes} {
            set cmap {}
        }
        return [list constant $all_str $cmap]
    } else {
        return [list word $winfo {}]
    }
}

proc _compileproc_scan_word_iterate { script stree type values ranges } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    upvar #0 _compileproc(word_scan_results) results
    set key $_compileproc(word_scan_key)

    if {$debug} {
        puts "_compileproc_scan_word_iterate : \"$script\" \{$stree\} $type \{$values\} \{$ranges\}"
    }

    switch -exact -- $type {
        {backslash} {
            # If backslashed element can be represented as an identical
            # backslash element in Java source code, then use it as is.
            # Otherwise, convert it to a representation that is valid.
            set elem "\\[lindex $values 0]"
            set jelem [emitter_backslash_tcl_elem $elem]

            set word [parse_word_iterate_word_value]
            lappend word [list {text} $jelem]
            parse_word_iterate_word_value $word
        }
        {command} {
            set ckeys [compileproc_childkey_next $key]
            if {[compileproc_is_empty_command $ckeys]} {
                # An empty command evaluates to the empty list,
                # in an array variable word this is a key
                # that is the empty string.
                set ckeys {}
                compileproc_constant_cache_add {}
            } else {
                if {$debug} {
                puts "scanning child key \{$ckeys\} (child of $key)"
                }
                compileproc_scan_keys $ckeys
                if {$debug} {
                puts "done scanning child key \{$ckeys\} (child of $key)"
                }
            }

            set word [parse_word_iterate_word_value]
            lappend word [list $type $ckeys]
            parse_word_iterate_word_value $word
        }
        {text} {
            set word [parse_word_iterate_word_value]
            lappend word [list $type [lindex $values 0]]
            parse_word_iterate_word_value $word
        }
        {variable} {
            # Create list that describes this variable, it
            # will be used to emit code to query the variable
            # value.

            set vstree $stree
            set vinfo [compileproc_scan_variable $key $script $vstree]

            set word [parse_word_iterate_word_value]
            lappend word [list $type $vinfo]
            parse_word_iterate_word_value $word
        }
        {word begin} {
            # No-op
        }
        {word end} {
            # No-op
        }
        {word} {
            # Word result available
            set results $values
        }
        default {
            error "unknown type \"$type\""
        }
    }
}

# Return a tuple of {TYPE PSTR} for an argument.
# The pstr value is a printable string that
# describes the argument. This is commonly used
# to print an argument description inside a comment.
# If the extra_info flag is set, then extra info
# about the command will be included if it would
# print nicely.

proc compileproc_argument_printable { key i } {
    global _compileproc_key_info

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_argument_printable $key"
    }

    # FIXME: This 20 character max word limit seems a bit too small
    # but changing it would require a lot of test updates.
    set maxvar 20
    #set maxword 40
    set maxword 20

    set types $_compileproc_key_info($key,types)
    set values $_compileproc_key_info($key,values)
    set instrs $_compileproc_key_info($key,instrs)

    # Return string the describes the argument.
    # Use "..." if the string would not print
    # as simple text.

    if {$i < 0 || $i >= [llength $types]} {
        error "index $i out of argument range"
    } else {
        set type [lindex $types $i]
        set print 1
        switch -exact -- $type {
            "constant" {
                set str [lindex $instrs $i]
            }
            "variable" {
                set str [lindex $instrs $i]
                if {[string length $str] > $maxvar} {
                    set str "\$..."
                }
            }
            "command" {
                # No need to print nested command text here since
                # the command name will be printed in the invocation.
                set str "\[...\]"
            }
            "word" {
                set str [lindex $instrs $i]
                if {[string length $str] > $maxword} {
                    set str "\"...\""
                }
            }
            default {
                error "unknown type \"$type\" at index $i of types \{$types\}"
            }
        }
        # Don't print argument that is too long
        if {[string length $str] > $maxword} {
            set print 0
        }
        # Don't print argument that contains a funky string like a newline.
        foreach funky [list "\a" "\b" "\f" "\n" "\r" "\t" "\v" "\\"] {
            if {[string first $funky $str] != -1} {
                set print 0
            }
        }

        if {!$print} {
            set str "..."
        }
        return [list $type $str]
    }
}

# Emit code that will invoke a Tcl method. The descend key for
# the method invocation to be written is passed.

proc compileproc_emit_invoke { key } {
    global _compileproc_key_info

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_emit_invoke $key"
    }

    set num_args $_compileproc_key_info($key,num_args)

    # Create string the describes the command
    # and all the arguments to the command.

    set cmdstr ""
    set cmdname "..."
    for {set i 0} {$i < $num_args} {incr i} {
        set tuple [compileproc_argument_printable $key $i]
        set type [lindex $tuple 0]
        set str [lindex $tuple 1]
        if {$i == 0 && $type == "constant"} {
            set cmdname $str
        }

        if {$i < ($num_args - 1)} {
            append cmdstr $str " "
        } else {
            append cmdstr $str
        }
    }

    # Open method invocation block
    append buffer [emitter_invoke_start $cmdstr]

    # If the command can be inlined, then do that now.
    # Otherwise, emit a regular invoke() call.

    if {[compileproc_is_container_command $key] &&
            [compileproc_can_inline_container $key]} {
        append buffer [compileproc_emit_container $key]
    } elseif {[compileproc_can_inline_control $key]} {
        append buffer [compileproc_emit_control $key]
    } elseif {[compileproc_can_inline_command $key]} {
        append buffer [compileproc_emit_inline_command $key]
    } else {
        append buffer [compileproc_emit_invoke_call $key]
    }

    # Close method invocation block
    append buffer [emitter_invoke_end $cmdname]

    return $buffer
}

# Emit call to TJC.invoke() to directly invoke a Tcl
# command via its Command.cmdProc() implementation.
# Pass the descend key for the command to invoke.

proc compileproc_emit_invoke_call { key } {
    return [compileproc_emit_objv_assignment \
        $key \
        0 end \
        0 \
        compileproc_emit_invoke_call_impl \
        {} \
        ]
}

# Emit code to allocate an array of TclObjects and
# assign command arguments to the array. This
# command will invoke a specific callback to
# emit code that appears after the array has
# been allocated and populated.

proc compileproc_emit_objv_assignment { key starti endi decl_tmpsymbol callback userdata } {
    global _compileproc
    global _compileproc_key_info

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_emit_invoke_call $key"
    }

    set num_args $_compileproc_key_info($key,num_args)

    if {$endi == "end"} {
        set endi $num_args
    }

    # Init buffer with array symbol declaration
    # and a tmp symbol inside the try block.

    set buffer ""
    set arraysym [compileproc_tmpvar_next objv]
    set objv_size [expr {$num_args - $starti}]
    append buffer [emitter_invoke_command_start $arraysym $objv_size]

    # If all the arguments are constant values and
    # the emitted code should skip constant increments,
    # then there is no need to declare a tmp local.

    if {$_compileproc(options,skip_constant_increment)} {
        set use_tmp_local 0

        for {set i $starti} {$i < $endi} {incr i} {
            set tuple [compileproc_get_argument_tuple $key $i]
            set type [lindex $tuple 0]
            if {$type != "constant"} {
                set use_tmp_local 1
            }
        }
    } else {
        set use_tmp_local 1
    }

    if {!$use_tmp_local && $decl_tmpsymbol} {
        set use_tmp_local 1
    }

    if {$use_tmp_local} {
        set tmpsymbol [compileproc_tmpvar_next]
        append buffer [emitter_statement "TclObject $tmpsymbol"]
    } else {
        # constant arguments will not use this tmpsymbol
        set tmpsymbol {}
    }

    # Evaluate each argument to the command, increment the
    # ref count if needed, and save the TclObject into
    # the argument array.

    set const_unincremented_indexes {}

    for {set i $starti} {$i < $endi} {incr i} {
        set tuple [compileproc_emit_argument $key $i 0 $tmpsymbol]
        set type [lindex $tuple 0]
        set symbol [lindex $tuple 1]
        set symbol_buffer [lindex $tuple 2]

        # Generate a string description of the argument before
        # evaluating the value. This makes code easier to debug
        # since constant values appear in the comments.

        set print_tuple [compileproc_argument_printable $key $i]
        set print_type [lindex $print_tuple 0]
        set print_str [lindex $print_tuple 1]

        append buffer \
            [emitter_comment "Arg $i $print_type: $print_str"]

        set array_i [expr {$i - $starti}]

        if {$_compileproc(options,skip_constant_increment) \
                && $type == "constant"} {
            append buffer [emitter_array_assign $arraysym $array_i $symbol]

            lappend const_unincremented_indexes $array_i
        } else {
            # emitter_invoke_command_assign will notice when the
            # tmpsymbol is being assigned to itself and skip it.

            append buffer $symbol_buffer \
                [emitter_invoke_command_assign $arraysym \
                    $array_i $tmpsymbol $symbol]
        }
    }

    # Invoke user supplied callback to emit code that appears
    # after the array has been populated.

    append buffer [$callback $key $arraysym $tmpsymbol $userdata]

    # Close try block and emit finally block

    append buffer [emitter_invoke_command_finally]

    # When constant increment is skipped, set refs
    # that are constant values in the objv array to
    # null before invoking releaseObjv() so that
    # TclObject.release() is not invoked for the
    # constant TclObject refs.

    set end_size $objv_size

    if {[llength $const_unincremented_indexes] > 0} {
        if {[llength $const_unincremented_indexes] == $objv_size} {
            # Special case, all the arguments are constants.
            # Pass zero as the size argument to releaseObjv()
            # so that all array elements are set to null
            # inside releaseObjv(), instead of here.

            set end_size 0
        } else {
            foreach i $const_unincremented_indexes {
                append buffer [emitter_array_assign $arraysym $i null]
            }
        }
    }

    append buffer [emitter_invoke_command_end $arraysym $end_size]

    return $buffer
}

# Emit TJC.invoke() for either the runtime lookup case or the
# cached command case. The arraysym is the symbol declared
# as a TclObject[]. The tmpsymbol is a symbol declared as
# as TclObject used to store a tmp result inside the try block,
# it will be {} if no tmpsymbol was declared.

proc compileproc_emit_invoke_call_impl { key arraysym tmpsymbol userdata } {
    global _compileproc

    set buffer ""

    # Check to see if cached command pointer should be passed.

    if {$_compileproc(options,cache_commands)} {
        set cmdsym [compileproc_command_cache_lookup $key]

        if {$cmdsym == {}} {
            set cmdsym_buffer {}
        } else {
            # Emit code to check that cached command is valid
            set cmdsym_buffer [compileproc_command_cache_epoch_check $cmdsym]
        }

        # Check for change in containing commands's cmdEpoch
        if {$cmdsym != {}} {
            append buffer [compileproc_command_cache_this_check]
        }

        # Emit invoke()
        append buffer [emitter_invoke_command_call $arraysym $cmdsym_buffer 0]

        # Emit command cache update check and function call
        if {$cmdsym != {}} {
            append buffer [compileproc_command_cache_update $cmdsym]
        }
    } else {
        # Emit invoke()
        append buffer [emitter_invoke_command_call $arraysym {} 0]
    }

    return $buffer
}

# Emit code to evaluate the value of a Tcl command
# argument. This method will return a tuple of
# {TYPE SYMBOL BUFFER}. A constant argument will be
# added to the constant pool by this method. By
# default a TclObject will be declared to contain
# the result. If declare_flag is false then the variable
# declaration will be left up to the caller.

proc compileproc_emit_argument { key i {declare_flag 1} {symbol_name {}} } {
    global _compileproc_key_info

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_emit_argument $key $i $declare_flag \{$symbol_name\}"
    }

    if {$declare_flag} {
        set declare "TclObject "
    } else {
        set declare ""
    }

    set types $_compileproc_key_info($key,types)
    set values $_compileproc_key_info($key,values)
    set num_args $_compileproc_key_info($key,num_args)

    set buffer ""
    set symbol ""

    if {$i < 0 || $i >= $num_args} {
        error "command argument index $i is out of range : num_args is $num_args"
    } else {
        set type [lindex $types $i]
        switch -exact -- $type {
            "constant" {
                set str [lindex $values $i]
                set ident [compileproc_constant_cache_get $str]
                set symbol $ident
            }
            "variable" {
                # Evaluate variable, save result into a tmp symbol,
                # then add the tmp to the argument array.
                set vinfo [lindex $values $i]

                if {$symbol_name == {}} {
                    set tmpsymbol [compileproc_tmpvar_next]
                } else {
                    set tmpsymbol $symbol_name
                }
                set symbol $tmpsymbol

                append buffer \
                    [compileproc_emit_variable $tmpsymbol $vinfo $declare_flag]
            }
            "command" {
                set ckeys [lindex $values $i]
                # Empty command replaced with ref to {} in key scan
                if {[compileproc_is_empty_command $ckeys]} {
                    error "unexpected empty nested command for key $key"
                }

                if {$symbol_name == {}} {
                    set tmpsymbol [compileproc_tmpvar_next]
                } else {
                    set tmpsymbol $symbol_name
                }
                set symbol $tmpsymbol

                # Emit 1 to N invocations
                foreach ckey $ckeys {
                    append buffer [compileproc_emit_invoke $ckey]
                }

                append buffer [emitter_indent] \
                    "${declare}$tmpsymbol = interp.getResult()\;\n"
            }
            "word" {
                # Concatenate word elements together and save the
                # result into a tmp symbol.
                set winfo [lindex $values $i]

                if {$symbol_name == {}} {
                    set tmpsymbol [compileproc_tmpvar_next]
                } else {
                    set tmpsymbol $symbol_name
                }
                set symbol $tmpsymbol

                append buffer \
                    [compileproc_emit_word $tmpsymbol $winfo $declare_flag]
            }
            default {
                error "unknown type \"$type\""
            }
        }
    }

    if {$symbol == ""} {error "empty symbol"}

    return [list $type $symbol $buffer]
}

# Return a tuple {TYPE INFO INSTR CMAP} for the argument
# at the given index.

proc compileproc_get_argument_tuple { key i } {
    global _compileproc_key_info

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_get_argument_type $key $i"
    }

    set types $_compileproc_key_info($key,types)
    set values $_compileproc_key_info($key,values)
    set num_args $_compileproc_key_info($key,num_args)
    set instrs $_compileproc_key_info($key,instrs)
    set cmaps $_compileproc_key_info($key,cmaps)

    if {$i < 0 || $i >= $num_args} {
        error "command argument index $i is out of range : num_args is $num_args"
    } else {
        set type [lindex $types $i]
        switch -exact -- $type {
            "constant" -
            "variable" -
            "command" -
            "word" {
                return [list $type \
                    [lindex $values $i] \
                    [lindex $instrs $i] \
                    [lindex $cmaps $i] \
                    ]
            }
            default {
                error "unknown type \"$type\""
            }
        }
    }
}

# Generate code to determine a variable value at runtime
# and assign the value the the given tmpsymbol. This
# method is the primary entry point for variable evaluation.

proc compileproc_emit_variable { tmpsymbol vinfo {declare_flag 1} } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    set buffer ""
    if {$declare_flag} {
        set declare "TclObject "
    } else {
        set declare ""
    }
    set vtype [lindex $vinfo 0]

    switch -exact -- $vtype {
        {scalar} {
            set vname [lindex $vinfo 1]
            append buffer \
                [emitter_statement \
                    "${declare}${tmpsymbol} = [compileproc_emit_scalar_variable_get $vname]"]
        }
        {array text} {
            set avname [lindex $vinfo 1 0]
            if {$avname == ""} {error "empty array name in \{$vinfo\}"}
            set kname [lindex $vinfo 1 1]
            if {$kname == ""} {error "empty array key name in \{$vinfo\}"}
            append buffer [emitter_indent] \
                "${declare}${tmpsymbol} = [emitter_get_var $avname true $kname true 0]\;\n"
        }
        {array scalar} {
            set avname [lindex $vinfo 1 0]
            if {$avname == ""} {error "empty array name in \{$vinfo\}"}
            set kvname [lindex $vinfo 1 1]
            if {$kvname == ""} {error "empty array key variable name in \{$vinfo\}"}
            append buffer \
                [emitter_statement \
                    "${declare}${tmpsymbol} = [compileproc_emit_scalar_variable_get $kvname]"] \
                [emitter_statement \
                    "$tmpsymbol = [emitter_get_var $avname true $tmpsymbol.toString() false 0]"]
        }
        {array command} {
            set avname [lindex $vinfo 1 0]
            if {$avname == ""} {error "empty array name in \{$vinfo\}"}
            set ckeys [lindex $vinfo 1 1]
            if {[compileproc_is_empty_command $ckeys]} {
                # Empty command
                append buffer [emitter_indent] \
                    "${declare}${tmpsymbol} = [emitter_get_var $avname true {} true 0]\;\n"
            } else {
                # Emit 1 to N invocations, then query results
                foreach ckey $ckeys {
                    append buffer [compileproc_emit_invoke $ckey]
                }
                append buffer \
                    [emitter_statement \
                        "${declare}${tmpsymbol} = interp.getResult()"] \
                    [emitter_statement \
                        "$tmpsymbol = [emitter_get_var $avname true $tmpsymbol.toString() false 0]"]
            }
        }
        {word command} {
            set ckeys [lindex $vinfo 1 0]
            if {[compileproc_is_empty_command $ckeys]} {
                # Empty command, emit ref to constant empty Tcl string.
                set esym [compileproc_constant_cache_get {}]
                append buffer [emitter_indent] \
                    "${declare}${tmpsymbol} = $esym\;\n"
            } else {
                # Emit 1 to N invocations, then query results
                foreach ckey $ckeys {
                    append buffer [compileproc_emit_invoke $ckey]
                }
                append buffer \
                    [emitter_statement \
                        "${declare}${tmpsymbol} = interp.getResult()"]
            }
        }
        {array word} {
            set avname [lindex $vinfo 1 0]
            if {$avname == ""} {error "empty array name in \{$vinfo\}"}
            set values [lindex $vinfo 1 1]
            if {[llength $values] == 0} {error "empty array values in \{$vinfo\}"}
            # Declare tmp variable unless it was already defined earlier.
            if {$declare_flag} {
                append buffer [emitter_indent] \
                    "${declare}${tmpsymbol}\;\n"
            }
            if {[llength $values] > 1} {
                # Multiple values to concatenate into a single word
                set sbtmp [compileproc_tmpvar_next sbtmp]
                append buffer \
                    [emitter_statement \
                    "StringBuffer $sbtmp = new StringBuffer(64)"]
                foreach value $values {
                    set type [lindex $value 0]
                    if {$type == "text"} {
                        set str [emitter_backslash_tcl_string [lindex $value 1]]
                        # A constant string, just append it to the StringBuffer
                        append buffer [emitter_indent] \
                            "$sbtmp.append(\"$str\")\;\n"
                    } else {
                        # A variable or command that must be evaluated then appended
                        append buffer \
                            [compileproc_emit_variable $tmpsymbol $value 0] \
                            [emitter_statement "$sbtmp.append($tmpsymbol.toString())"]
                    }
                }
                set result $sbtmp.toString()
            } else {
                # A single word value, no need to concat results together.
                set value [lindex $values 0]
                set type [lindex $value 0]
                if {$type == "text"} {
                    # A complex word with a single value would not be a constant
                    error "unexpected single constant value for word"
                }
                append buffer [compileproc_emit_variable $tmpsymbol $value 0]
                set result $tmpsymbol.toString()
            }
            # Finally, evaluate the array with the word value as the key
            append buffer [emitter_indent] \
                "$tmpsymbol = [emitter_get_var $avname true $result false 0]\;\n"
        }
        default {
            error "unhandled non-scalar type \"$vtype\" in vinfo \{$vinfo\}"
        }
    }

    return $buffer
}

# Emit code to get the value of a scalar variable. This method
# generates an assignable value of type TclObject. This method
# is used by compileproc_emit_variable for scalars.

proc compileproc_emit_scalar_variable_get { vname } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_emit_scalar_variable_get $vname"
    }

    if {[info exists _compileproc(options,cache_variables)] && \
            $_compileproc(options,cache_variables)} {
        set symbol [compileproc_variable_cache_lookup $vname]
        set cacheId [compileproc_get_cache_id_from_symbol $symbol]

        set buffer [emitter_get_cache_scalar_var $vname true 0 $symbol $cacheId]
    } else {
        set buffer [emitter_get_var $vname true null false 0]
    }
    return $buffer
}

# Emit code to set the value of a scalar variable. This method
# assigns a new value to a scalar variable and returns an
# assignable value of type TclObject. This method is used
# throughout this module to set a scalar variable value.

proc compileproc_emit_scalar_variable_set { vname value } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_emit_scalar_variable_set $vname $value"
    }

    if {[info exists _compileproc(options,cache_variables)] && \
            $_compileproc(options,cache_variables)} {

        set symbol [compileproc_variable_cache_lookup $vname]
        set cacheId [compileproc_get_cache_id_from_symbol $symbol]
        set buffer [emitter_set_cache_scalar_var $vname true $value 0 $symbol $cacheId]
    } else {
        set buffer [emitter_set_var $vname true null false $value 0]
    }

    return $buffer
}

# Determine a word value at runtime and emit code
# to assign the value of this variable to a tmp local.

proc compileproc_emit_word { tmpsymbol winfo {declare_flag 1}} {
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_emit_word $tmpsymbol $winfo $declare_flag"
    }

    if {$declare_flag} {
        set declare "TclObject "
    } else {
        set declare ""
    }

    set buffer ""

    set len [llength $winfo]
    if {$len == 0} {
        error "empty winfo"
    } elseif {$len == 1} {
        # A word that contains a single element, either a variable
        # or a command.
        set wi [lindex $winfo 0]
        append buffer [compileproc_emit_word_element $wi $tmpsymbol false {} $declare_flag]
    } else {
        # A word that contains multiple elements that should be concatenated together
        set sbtmp [compileproc_tmpvar_next sbtmp]

        if {$declare_flag} {
            append buffer [emitter_indent] \
                "${declare}${tmpsymbol}\;\n"
        }

        append buffer [emitter_indent] \
            "StringBuffer $sbtmp = new StringBuffer(64)\;\n"

        foreach wi $winfo {
            append buffer [compileproc_emit_word_element $wi $tmpsymbol true $sbtmp $declare_flag]
        }

        # Create new TclString object that contains the new StringBuffer
        append buffer [emitter_indent] \
            "$tmpsymbol = TclString.newInstance($sbtmp)\;\n"
    }

    return $buffer
}

# Emit code to either assign a value to or append to tmpsymbol.
# This code will append to a StringBuffer tmpsymbol if the
# append flag is true. By default, a TclObject local variable
# will be declared. If declare_flag is false, then the variable
# will be assigned but it is up to the caller to declare it
# before hand.

proc compileproc_emit_word_element { winfo_element tmpsymbol append sbtmp {declare_flag 1} } {
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_emit_word_element \{$winfo_element\} $tmpsymbol $append $sbtmp $declare_flag"
    }

    set buffer ""

    if {$declare_flag} {
        set declare "TclObject "
    } else {
        set declare ""
    }

    set type [lindex $winfo_element 0]

    if {$type == "text"} {
        # Emit code to append constant string to word value
        set str [emitter_backslash_tcl_string [lindex $winfo_element 1]]
        if {$append} {
            append buffer [emitter_indent] \
                "$sbtmp.append(\"$str\")\;\n"
        } else {
            error "constant text can't be assigned"
        }
    } elseif {$type == "variable"} {
        # Emit variable query code
        set vinfo [lindex $winfo_element 1]
        set decl $declare_flag
        if {$append} {
            set decl 0
        }
        append buffer [compileproc_emit_variable $tmpsymbol $vinfo $decl]
        if {$append} {
            append buffer [emitter_indent] \
                "$sbtmp.append($tmpsymbol.toString())\;\n"
        }
    } elseif {$type == "command"} {
        # Emit command evaluation code
        set ckeys [lindex $winfo_element 1]

        if {[compileproc_is_empty_command $ckeys]} {
            # Empty command, emit ref to constant empty Tcl string.
            set esym [compileproc_constant_cache_get {}]
            if {$append} {
                append buffer [emitter_indent] \
                    "$sbtmp.append($esym.toString())\;\n"
            } else {
                append buffer [emitter_indent] \
                    "${declare}$tmpsymbol = $esym\;\n"
            }
        } else {
            # 1 or more command keys, emit a invocation for
            # each non-empty key.
            foreach ckey $ckeys {
                if {$ckey == {}} {continue} ; # Skip empty command
                append buffer [compileproc_emit_invoke $ckey]
            }
            if {$append} {
                append buffer \
                    [emitter_statement "$tmpsymbol = interp.getResult()"] \
                    [emitter_statement "$sbtmp.append($tmpsymbol.toString())"]
            } else {
                append buffer \
                    [emitter_statement "${declare}$tmpsymbol = interp.getResult()"]
            }
        }
    } else {
        error "unknown word info element type \"$type\" in winfo_element \{$winfo_element\}"
    }

    return $buffer
}

# Reset the counter used to generate temp variable names in the scope
# of a command. We don't want to have to deal with any issues related to
# reuse of variable names in different scoped blocks, so number all tmp
# variables in the cmdProc scope.

proc compileproc_tmpvar_reset {} {
    global _compileproc
    set _compileproc(local_counter) 0
}

# Return the next temp var name

proc compileproc_tmpvar_next { {prefix "tmp"} } {
    global _compileproc
    set vname "${prefix}$_compileproc(local_counter)"
    incr _compileproc(local_counter)
    return $vname
}

# Return true if the key corresponds to a container command.
# This type of command contains other commands and can be
# inlined so that logic tests and looping can be done in Java.

proc compileproc_is_container_command { key } {
    global _compileproc_ckeys
    if {![info exists _compileproc_ckeys($key,info_key)]} {
        return 0
    }
    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    if {$cmdname == "_UNKNOWN"} {
        return 0
    }
    switch -exact -- $cmdname {
        "::catch" -
        "catch" -
        "::expr" -
        "expr" -
        "::for" -
        "for" -
        "::foreach" -
        "foreach" -
        "::if" -
        "if" -
        "::switch" -
        "switch" -
        "::while" -
        "while" {
            return 1
        }
        default {
            return 0
        }
    }
}

# Return true if this container command can be inlined.
# Typically, a container command that is staticly
# defined and has the correct number of arguments
# can be inlined. For example, a while loop where
# the body code is contained in a braced string and
# the expr test is in a braced string can be inlined.

proc compileproc_can_inline_container { key } {
    global _compileproc _compileproc_ckeys
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_can_inline_container $key"
    }

    if {$_compileproc(options,inline_containers) == {}} {
        if {$debug} {
            puts "inline_containers is {}, returning 0"
        }
        return 0
    }

    # Determine the command name
    if {![info exists _compileproc_ckeys($key,info_key)]} {
        if {$debug} {
            puts "no info_key for key $key"
        }
        return 0
    }
    set qcmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    if {$qcmdname == "_UNKNOWN"} {
        error "container should not have non-constant name"
    }
    set cmdname [namespace tail $qcmdname]
    if {$debug} {
        puts "container command name \"$cmdname\" for key $key"
    }

    # Determine if command can be inlined based on module config.
    set inline 0

    if {$_compileproc(options,inline_containers) == "all"} {
        set inline 1
    }

    if {$inline == 0} {
        set ind [lsearch -exact $_compileproc(options,inline_containers) $cmdname]
        if {$ind == -1} {
            if {$debug} {
                puts "container \"$cmdname\" not found in inline_containers list\
                    \{$_compileproc(options,inline_containers)\}"
            }
            return 0
        } else {
            set inline 1
        }
    }

    if {$inline == 0} {
        error "expected inline to be 1, not \"$inline\""
    }

    # The command container could be inlined if it passed
    # the container validation test and all the body
    # blocks are static.

    if {[descend_container_is_valid $key] && \
            [descend_container_is_static $key]} {

        if {$debug} {
            puts "container is valid and static, cmdname is \"$cmdname\""
        }

        #if {$cmdname == "foreach"} {
        #    # Don't try to inline foreach with multiple lists
        #    if {![descend_container_foreach_has_single_list $key]} {
        #        return 0
        #    }
        #    # Don't try to inline foreach with multiple vars in varlist
        #    if {![descend_container_foreach_has_single_variable $key]} {
        #        return 0
        #    }
        #}

        # Compile switch command with a string and a patbody list

        #if {$cmdname == "switch"} {
        #    if {![descend_container_switch_has_patbody_list $key]} {
        #        return 0
        #    }
        #}

        return 1
    } else {
        if {$debug} {
            puts "container command \"$cmdname\" is not valid or not static"
            puts "is_valid [descend_container_is_valid $key]"
            puts "is_static [descend_container_is_static $key]"
        }
        return 0
    }
}

# Return true if the command identified by key is
# a break, continue, or return command that can
# be inlined. The break and continue commands
# can be inlined inside a loop context. A return
# command can be inlined in a procedure unless
# it is contained inside a catch block. This
# method is invoked as code is emitted.

proc compileproc_can_inline_control { key } {
    global _compileproc
    global _compileproc_ckeys

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_can_inline_control $key"
    }

    if {!$_compileproc(options,inline_controls)} {
        if {$debug} {
            puts "inline_controls option is not enabled"
        }
        return 0
    }
    if {![info exists _compileproc_ckeys($key,info_key)]} {
        return 0
    }
    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    if {$cmdname == "_UNKNOWN"} {
        return 0
    }
    switch -exact -- $cmdname {
        "::break" -
        "break" {
            set is_break 1
            set is_continue 0
            set is_return 0
        }
        "::continue" -
        "continue" {
            set is_break 0
            set is_continue 1
            set is_return 0
        }
        "::return" -
        "return" {
            set is_break 0
            set is_continue 0
            set is_return 1
        }
        default {
            if {$debug} {
                puts "command \"$cmdname\" is not a break, continue, or return command"
            }
            return 0
        }
    }

    # A break or continue command must have a single argument.
    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$is_break || $is_continue} {
        if {$num_args != 1} {
            if {$debug} {
                puts "invalid num_args $num_args to break or continue command"
            }
            return 0
        }
    } elseif {$is_return} {
        # Let runtime handle a return with special options.
        if {$num_args != 1 && $num_args != 2} {
            if {$debug} {
                puts "unhandled return with $num_args arguments"
            }
            return 0
        }
    }

    # The command is break, continue, or return and the
    # inline controls flag is enabled. Check the current
    # controls stack tuple to determine if the command
    # can be inlined in this control context.

    set stack $_compileproc(options,controls_stack)
    if {[llength $stack] == 0} {
        if {$debug} {
            puts "empty controls_stack, can't inline break/continue"
        }
        return 0
    }
    set tuple [lindex $stack 0]
    if {[llength $tuple] != 7 \
            || [lindex $tuple 1] != "break" \
            || [lindex $tuple 3] != "continue" \
            || [lindex $tuple 5] != "return"} {
        error "expected controls_stack top tuple\
            {type break 0|1 continue 0|1 return 0|1} but got \{$tuple\}"
    }
    if {$debug} {
        puts "checking command $cmdname : controls context is \{$tuple\}"
    }
    array set stack_map [lrange $tuple 1 end]
    if {$is_break && $stack_map(break)} {
        # break can be inlined
        if {$debug} {
            puts "can inline break command"
        }
        return 1
    }
    if {$is_continue && $stack_map(continue)} {
        # continue can be inlined
        if {$debug} {
            puts "can inline continue command"
        }
        return 1
    }
    if {$is_return && $stack_map(return)} {
        # return can be inlined
        if {$debug} {
            puts "can inline return command"
        }
        return 1
    }

    if {$debug} {
        puts "can't inline break/continue/return command, not valid in context [lindex $tuple 0]"
    }
    return 0
}

# Return true if the key corresponds to a built-in Tcl command
# that can be inlined. Container commands are handled elsewhere,
# inlined commands are single commands like "set" that can
# be replaced with optimized code.

proc compileproc_can_inline_command { key } {
    global _compileproc
    global _compileproc_ckeys

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_can_inline_command $key"
    }

    if {!$_compileproc(options,inline_commands)} {
        return 0
    }

    if {![info exists _compileproc_ckeys($key,info_key)]} {
        return 0
    }
    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    if {$cmdname == "_UNKNOWN"} {
        return 0
    }

    # See if commands is one of the built-in Tcl
    # commands that we know how to inline. Note
    # that we will not support an inline of the
    # following commands:
    #
    # variable, upvar, uplevel

    switch -exact -- $cmdname {
        "::append" -
        "append" {
            return [compileproc_can_inline_command_append $key]
        }
        "::global" -
        "global" {
            return [compileproc_can_inline_command_global $key]
        }
        "::incr" -
        "incr" {
            return [compileproc_can_inline_command_incr $key]
        }
        "::lappend" -
        "lappend" {
            return [compileproc_can_inline_command_lappend $key]
        }
        "::lindex" -
        "lindex" {
            return [compileproc_can_inline_command_lindex $key]
        }
        "::list" -
        "list" {
            return [compileproc_can_inline_command_list $key]
        }
        "::llength" -
        "llength" {
            return [compileproc_can_inline_command_llength $key]
        }
        "::set" -
        "set" {
            return [compileproc_can_inline_command_set $key]
        }
        "::string" -
        "string" {
            return [compileproc_can_inline_command_string $key]
        }
        default {
            return 0
        }
    }
}

# Return code to inline a specific built-in Tcl command.

proc compileproc_emit_inline_command { key } {
    global _compileproc
    global _compileproc_ckeys

    if {!$_compileproc(options,inline_commands)} {
        error "compileproc_emit_inline_command invoked when inline_commands option is false"
    }
    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    # convert input like "::set" to "set" to simplify switch
    set cmdname [namespace tail $cmdname]

    # invoke specific method to emit command code

    switch -exact -- $cmdname {
        "append" {
            return [compileproc_emit_inline_command_append $key]
        }
        "global" {
            return [compileproc_emit_inline_command_global $key]
        }
        "incr" {
            return [compileproc_emit_inline_command_incr $key]
        }
        "lappend" {
            return [compileproc_emit_inline_command_lappend $key]
        }
        "lindex" {
            return [compileproc_emit_inline_command_lindex $key]
        }
        "list" {
            return [compileproc_emit_inline_command_list $key]
        }
        "llength" {
            return [compileproc_emit_inline_command_llength $key]
        }
        "set" {
            return [compileproc_emit_inline_command_set $key]
        }
        "string" {
            return [compileproc_emit_inline_command_string $key]
        }
        default {
            error "unsupported inlined command \"$cmdname\""
        }
    }
}

# Invoke to emit code for a specific type of container
# command.

proc compileproc_emit_container { key } {
    global _compileproc_ckeys

    if {![info exists _compileproc_ckeys($key,info_key)]} {
        error "expected _compileproc_ckeys entry for container $key"
    }
    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    if {$cmdname == "_UNKNOWN"} {
        error "expected command name for $key"
    }

    # If command name has a namespace qualifier, just use
    # the tail part.
    set tail [namespace tail $cmdname]

    switch -exact -- $tail {
        "catch" {
            return [compileproc_emit_container_catch $key]
        }
        "expr" {
            return [compileproc_emit_container_expr $key]
        }
        "for" {
            return [compileproc_emit_container_for $key]
        }
        "foreach" {
            return [compileproc_emit_container_foreach $key]
        }
        "if" {
            return [compileproc_emit_container_if $key]
        }
        "switch" {
            return [compileproc_emit_container_switch $key]
        }
        "while" {
            return [compileproc_emit_container_while $key]
        }
        default {
            error "expected a supported container command, got \"$tail\""
        }
    }
}

# Emit code for an inlined if container.

proc compileproc_emit_container_if { key } {
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    set buffer ""

    if {![descend_container_is_valid $key] || \
            ![descend_container_is_static $key]} {
        error "NOT valid or NOT static for key $key"
    }

    set ccmds [descend_commands $key container]
    if {[llength $ccmds] < 3} {
        error "bad num container commands \{$ccmds\}"
    }

    set expr_keys [lindex $ccmds 0]
    set true_keys [lindex $ccmds 1]

    if {[llength $ccmds] > 3} {
        set elseif_keys [lrange $ccmds 2 end-1]
        if {([llength $elseif_keys] % 2) != 0} {
            error "expected even number of elseif keys, got \{$elseif_keys\}"
        }
    } else {
        set elseif_keys {}
    }
    set else_keys [lindex $ccmds end]

    # Evaluate if {expr} expression
    set expr_index [lindex [descend_container_if_expr_body $key] 0]

    set tuple [compileproc_expr_evaluate_boolean_emit $key $expr_index]
    set tmpsymbol [lindex $tuple 0]
    append buffer [lindex $tuple 1]
    set is_constant [lindex $tuple 2]
    append buffer [emitter_container_if_start $tmpsymbol]

    # true block
    foreach true_key $true_keys {
        append buffer [compileproc_emit_invoke $true_key]
    }

    # elseif blocks
    set elseif_depth 0
    if {$elseif_keys != {}} {
        foreach {elseif_expr_keys elseif_body_keys} $elseif_keys \
                {expr_index body_index} [descend_container_if_iterate_elseif_body $key] {

            set tuple [compileproc_expr_evaluate_boolean_emit $key $expr_index]
            set tmpsymbol [lindex $tuple 0]
            set is_constant [lindex $tuple 2]
            if {$is_constant} {
                # A constant has no evaluation code, so it can be
                # placed directly in an if/elseif expression.
                append buffer \
                    [lindex $tuple 1] \
                    [emitter_container_if_else_if $tmpsymbol]
            } else {
                # The expression needs to be evaluated with multiple
                # statements. Use multiple if/else blocks to simulate
                # the if/elseif/else Tcl command.

                append buffer \
                    [emitter_container_if_else] \
                    [lindex $tuple 1] \
                    [emitter_container_if_start $tmpsymbol]
                incr elseif_depth
            }

            foreach elseif_body_key $elseif_body_keys {
                append buffer [compileproc_emit_invoke $elseif_body_key]
            }
        }
    }

    # else block
    if {$else_keys != {}} {
        append buffer [emitter_container_if_else]
        foreach else_key $else_keys {
            append buffer [compileproc_emit_invoke $else_key]
        }
    }

    # Finish up if/else blocks
    for {set i 0} {$i < $elseif_depth} {incr i} {
        append buffer [emitter_container_if_end]
    }

    append buffer [emitter_container_if_end]

    return $buffer
}

# This method is invoked when a container command that
# has a boolean expr block wants to evaluate the expr
# as a boolean value. A tuple of {tmpsymbol buffer is_constant}
# is returned by this method.

proc compileproc_expr_evaluate_boolean_emit { key expr_index } {
    global _compileproc_key_info
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    set buffer ""

    set expr_result [compileproc_expr_evaluate $key $expr_index]
    set type [lindex $expr_result 0]

    # Determine if the expression has a constant boolean value
    switch -exact -- $type {
        {constant} -
        {constant boolean} -
        {constant string} {
            set value [lindex $expr_result 1]
            if {$debug} {
                puts "got constant value \"$value\""
            }
            # Convert constant value to a constant boolean.
            if {$type == {constant string}} {
                set ovalue "\"$value\""
            } else {
                set ovalue $value
            }
            if {[catch {expr "!!$ovalue"} result]} {
                # Not a constant boolean expression
                if {$debug} {
                    puts "expr \{!!$ovalue\} returned: $err"
                }
            } else {
                if {$result} {
                    set simple_bool_value true
                } else {
                    set simple_bool_value false
                }
                # Constant value expression
                return [list $simple_bool_value $buffer 1]
            }
        }
    }

    # Determine if the expression is a plain scalar variable
    # that can be handled with optimized code.

    if {$type == {variable scalar}} {
        set sname [lindex $expr_result 1]
        set vinfo [list scalar $sname]
        set tmpsymbol1 [compileproc_tmpvar_next]
        append buffer \
            [compileproc_emit_variable $tmpsymbol1 $vinfo]
        set tmpsymbol2 [compileproc_tmpvar_next]
        append buffer [emitter_indent] \
            "boolean $tmpsymbol2 = " \
            [emitter_tclobject_to_boolean $tmpsymbol1] \
            "\;\n"
        set retsymbol $tmpsymbol2
        return [list $tmpsymbol2 $buffer 0]
    }

    # Otherwise, the expression is non-trivial so handle
    # it with the expr command. In some testing usage,
    # we want to disable expr inlining when compiling
    # commands like "if". In the normal case, a expr
    # inside a command is inlined.

    set inline 0
    if {$_compileproc(options,inline_containers) == "all"} {
        set inline 1
    }
    if {$inline == 0} {
        set ind [lsearch -exact $_compileproc(options,inline_containers) \
            "expr"]
        if {$ind != -1} {
            set inline 1
        }
    }
    #set inline 0

    if {!$inline} {
        # Invoke expr command with a constant string
        # as the expression.
        set values $_compileproc_key_info($key,values)
        set expr_str [lindex $values $expr_index]

        # Generate a fake key that invokes the
        # expr method with the passed in argument
        # string. This only works for constant
        # strings right now.

        set gkey [compileproc_generate_key expr \
            [list $expr_str] [list "\{$expr_str\}"]]
        append buffer [compileproc_emit_invoke $gkey]
        set tmpsymbol1 [compileproc_tmpvar_next]
        append buffer [emitter_indent] \
            "TclObject $tmpsymbol1 = interp.getResult()\;\n"

        set tmpsymbol2 [compileproc_tmpvar_next]
        append buffer [emitter_indent] \
            "boolean $tmpsymbol2 = " \
        [emitter_tclobject_to_boolean $tmpsymbol1] "\;\n"

        return [list $tmpsymbol2 $buffer 0]
    } else {
        # Generate expr evaluation buffer and then
        # determine a boolean value for the expression.

        set eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
            $expr_result]
        set infostr [lindex $eval_tuple 0]
        set ev [lindex $eval_tuple 1]
        append buffer [lindex $eval_tuple 2]

        set tmpsymbol [compileproc_tmpvar_next]
        append buffer \
            [emitter_statement "boolean $tmpsymbol = $ev.getBooleanValue(interp)"] \
            [emitter_statement "TJC.exprReleaseValue(interp, $ev)"]

        return [list $tmpsymbol $buffer 0]
    }
}

# Evaluate an expression and return the parse tree
# for the expression. This method is used by a
# compile expr command and container commands
# like if and while.

proc compileproc_expr_evaluate { key expr_index } {
    global _compileproc _compileproc_key_info

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_expr_evaluate $key $expr_index"
    }

    set types $_compileproc_key_info($key,types)
    set values $_compileproc_key_info($key,values)

    if {[lindex $types $expr_index] != "constant"} {
        error "expected constant argument type at expr index $expr_index of \{$types\}"
    }

    set expr_str [lindex $values $expr_index]
    set simple_bool 0
    if {$expr_str == "1" || $expr_str == "true"} {
        set simple_bool 1
        set simple_bool_value "true"
    } elseif {$expr_str == "0" || $expr_str == "false"} {
        set simple_bool 1
        set simple_bool_value "false"
    }

    if {$simple_bool} {
        if {$debug} {
            puts "compileproc_expr_evaluate returning constant bool $simple_bool_value"
        }
        return [list {constant boolean} $simple_bool_value $expr_str]
    }

    if {$debug} {
        puts "compileproc_expr_evaluate: a non-constant-boolean expr will be iterated"
    }

    # FIXME: Is there any way we could save the etree created during the descend
    # parse so we don't need to do this all over again. Also, if we parsed the
    # expr once in the descend module and it turned out to be a simple type
    # like a scalar variable, could we save that info an use it here?

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]
    set stree [lindex $tree $expr_index]
    set range [parse_get_simple_text_range $stree text]
    set etree [parse expr $script $range]

    # When iterating over an expr command or a command
    # that contains an implicit expr, we need to map
    # command operators to command keys. This is
    # implemented using the childkey API. An expr
    # that is compiled will never have regular
    # nested commands, so there is no danger of
    # the childkey API being used for different
    # things here. Note that we need to treat
    # the "expr" command itself with special care
    # since its container commands data applies to
    # the whole expression.
    set ccmds [descend_commands $key container]
    if {$debug} {
        puts "container commands for key $key are \{$ccmds\}"
    }
    set tuple [compileproc_expr_container_index $key $expr_index]
    if {[lindex $tuple 0] == "expr"} {
        set argument_ccmds $ccmds
        if {$debug} {
            puts "expr command found, argument_ccmds is \{$argument_ccmds\}"
        }
    } else {
        set container_index [lindex $tuple 1]
        set argument_ccmds [lindex $ccmds $container_index]
        if {$debug} {
            puts "[lindex $tuple 0] command with expr found"
            puts "argument $expr_index maps to container index $container_index"
            puts "argument_ccmds is \{$argument_ccmds\}"
        }
    }
    compileproc_childkey_reset $key $argument_ccmds

    # Save values (needed for recursive safety), then
    # iterate and restore saved values.

    set saved_expr_eval_key $_compileproc(expr_eval_key)
    set saved_expr_eval_buffer $_compileproc(expr_eval_buffer)
    set saved_expr_eval_expressions $_compileproc(expr_eval_expressions)

    set _compileproc(expr_eval_key) $key
    set _compileproc(expr_eval_buffer) {}
    set _compileproc(expr_eval_expressions) 0

    set type_value [parse_expr_iterate $script $etree \
        compileproc_expr_evaluate_callback]

    set expr_eval_buffer $_compileproc(expr_eval_buffer)
    set expr_eval_expressions $_compileproc(expr_eval_expressions)

    set _compileproc(expr_eval_key) $saved_expr_eval_key
    set _compileproc(expr_eval_buffer) $saved_expr_eval_buffer
    set _compileproc(expr_eval_expressions) $saved_expr_eval_expressions

    # Check that each container command was processed.
    if {$debug} {
        puts "calling compileproc_childkey_validate for $key with argument_ccmds \{$argument_ccmds\}"
    }
    compileproc_childkey_validate $key

    # Examine results of expr iteration

    set type [lindex $type_value 0]
    set value [lindex $type_value 1]

    if {$debug} {
        puts "type value is: $type_value"
        puts "num expressions is $_compileproc(expr_eval_expressions)"
        if {$_compileproc(expr_eval_buffer) != {}} {
        puts "eval buffer is:\n$_compileproc(expr_eval_buffer)"
        }
    }

    return $type_value
}

# Map expr argument index to an index into the
# container command list. Every container command
# except the "if" command has a trivial mapping.
# This method returns the name of the command and
# the index into the container command list that
# the expr index would be found at. For the "expr"
# command, the index -1 is returned since there
# is no mapping.

proc compileproc_expr_container_index { key in_expr_index } {
    global _compileproc_ckeys

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
    puts "compileproc_expr_container_index $key $in_expr_index"
    }

    if {$in_expr_index == {}} {
        error "empty in_expr_index argument"
    }

    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]

    if {$cmdname == "if"} {
        set expr_indexes [list]
        # if expr body block
        set tuple [descend_container_if_expr_body $key]
        set expr_index [lindex $tuple 0]
        lappend expr_indexes $expr_index {}
        # else/if blocks
        if {[descend_container_if_has_elseif_body $key]} {
            foreach {expr_index body_index} \
                    [descend_container_if_iterate_elseif_body $key] {
                lappend expr_indexes $expr_index {}
            }
        }
        # else block -> No-op

        # Find index in expr_indexes for in_expr_index
        if {$debug} {
            puts "searching list \{$expr_indexes\} for $in_expr_index"
        }
        set ind [lsearch -exact $expr_indexes $in_expr_index]
        if {$ind == -1} {
            error "if: failed to find expr index $in_expr_index\
                in \{$expr_indexes\}"
        }
        return [list $cmdname $ind]
    } elseif {$cmdname == "expr"} {
        return [list $cmdname -1]
    }

    # Command other than if have a trivial mapping
    return [list $cmdname [expr {$in_expr_index - 1}]]
}

# Invoked as expr is iterated over.

proc compileproc_expr_evaluate_callback { script etree type values ranges } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    # Init
    #set _compileproc(expr_eval_key) $key
    #set _compileproc(expr_eval_buffer) {}
    #set _compileproc(expr_eval_expressions) 0

    if {$debug} {
        puts "compileproc_expr_evaluate_callback:\
            \{$type\} \{$values\} \{$ranges\}"
    }

    switch -exact -- $type {
        {literal operand} {
            set type [lindex $values 0 0]
            set literal [lindex $values 0 1]

            # The literals 1 and 1.0 are handled differently
            # than the literal strings "1" and {1}.
            if {$type == {text}} {
                parse_expr_iterate_type_value constant $literal
            } elseif {$type == {string}} {
                parse_expr_iterate_type_value {constant string} $literal
            } elseif {$type == {braced string}} {
                # A brace quoted operand like {foo\nbar} must be written
                # as the Java String "foo\\nbar". Find all backslash
                # chars and double backslash them.
                if {[string first "\\" $literal] != -1} {
                    set dbs_str ""
                    set len [string length $literal]
                    for {set i 0} {$i < $len} {incr i} {
                        set c [string index $literal $i]
                        if {$c == "\\"} {
                            append dbs_str "\\\\"
                        } else {
                            append dbs_str $c
                        }
                    }
                    set literal $dbs_str
                }
                parse_expr_iterate_type_value {constant braced string}\
                    $literal
            } else {
                error "unhandled literal operand type \"$type\""
            }
        }
        {command operand} {
            # Descend into container commands inside the expr.
            set key $_compileproc(expr_eval_key)
            set ccmds [compileproc_childkey_next $key]
            parse_expr_iterate_type_value {command operand} $ccmds
        }
        {variable operand} {
            # Check variable element. Treat a scalar variable as a special
            # case since it can be looked up as a value.

            if {[parse_is_scalar_variable $etree]} {
                set scalar [parse_get_scalar_variable $script $etree]
                parse_expr_iterate_type_value {variable scalar} $scalar
            } else {
                set key $_compileproc(expr_eval_key)
                set script [descend_get_data $key script]
                set vinfo [compileproc_scan_variable $key $script $etree]
                parse_expr_iterate_type_value {variable array} $vinfo
            }
        }
        {word operand} {
            # Scan word elements looking for nested commands,
            # variables, and backslash escapes. If the word
            # contained only backslashes, then convert to
            # a constant double quoted string operand.

            set key $_compileproc(expr_eval_key)

            set wstree $etree
            set type_winfo [compileproc_scan_word $key $script $wstree]

            set type [lindex $type_winfo 0]
            set value [lindex $type_winfo 1]
            set cmap [lindex $type_winfo 2]

            if {$debug} {
            puts "compileproc_scan_word returned type/value/cmap: $type \{$value\} \{$cmap\}"
            }

            if {$type == "constant"} {
                parse_expr_iterate_type_value {constant string} $value
            } else {
                # Don't have a types/values/cmap record for expr
                # operands like we do for command arguments, so
                # just pass them along as the value list.
                set value [list $type $values $cmap]
                parse_expr_iterate_type_value {word operand} $type_winfo
            }
        }
        {unary operator} {
            incr _compileproc(expr_eval_expressions)
            parse_expr_iterate_type_value {unary operator} $values
        }
        {binary operator} {
            incr _compileproc(expr_eval_expressions)
            parse_expr_iterate_type_value {binary operator} $values
        }
        {ternary operator} {
            incr _compileproc(expr_eval_expressions)
            parse_expr_iterate_type_value {ternary operator} $values
        }
        {math function} {
            incr _compileproc(expr_eval_expressions)
            parse_expr_iterate_type_value {math function} $values
        }
        {subexpression} {
            # No-op
        }
        {value} {
            # No-op
        }
        default {
            error "unknown type \"$type\""
        }
    }
}

# Invoked to query module file flags and set flags in the compileproc module
# that depend on these flags. This method is invoked dynamically inside
# compileproc_compile after compileproc_init has been invoked so that
# the compileproc module can be tested without depending on other modules.

proc compileproc_query_module_flags { proc_name } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    # Query global debug flag and set debug flag for this
    # module if global flag is set. This is typically
    # set via the -debug command line flag.
    if {0 && $::_tjc(debug)} {
        set _compileproc(debug) 1
        set debug 1
    }

    if {$debug} {
        puts "compileproc_query_module_flags $proc_name"
    }

    # If +inline-containers if set for this proc of for the whole module, then
    # enable the flag in this module.
    set inline_containers_option [module_option_value inline-containers $proc_name]
    if {$inline_containers_option == {}} {
        set inline_containers_option [module_option_value inline-containers]
    }
    if {$inline_containers_option} {
        set _compileproc(options,inline_containers) all
    } else {
        # Expect option to be set to {}
        if {$_compileproc(options,inline_containers) != {}} {
            error "inline_containers option set to \
                \"$_compileproc(options,inline_containers)\", expected empty string"
        }
    }

    set inline_controls_option \
        [module_option_value inline-controls $proc_name]
    if {$inline_controls_option == {}} {
        set inline_controls_option \
            [module_option_value inline-controls]
    }
    if {$inline_controls_option} {
        set _compileproc(options,inline_controls) 1
    }

    set cache_commands_option \
        [module_option_value cache-commands $proc_name]
    if {$cache_commands_option == {}} {
        set cache_commands_option \
            [module_option_value cache-commands]
    }
    if {$cache_commands_option} {
        set _compileproc(options,cache_commands) 1
    }

    set constant_increment_option \
        [module_option_value constant-increment $proc_name]
    if {$constant_increment_option == {}} {
        set constant_increment_option \
            [module_option_value constant-increment]
    }
    if {$constant_increment_option == 0} {
        set _compileproc(options,skip_constant_increment) 1
    }

    set cache_variables_option \
        [module_option_value cache-variables $proc_name]
    if {$cache_variables_option == {}} {
        set cache_variables_option \
            [module_option_value cache-variables]
    }
    if {$cache_variables_option} {
        set _compileproc(options,cache_variables) 1
    }

    set inline_commands_option \
        [module_option_value inline-commands $proc_name]
    if {$inline_commands_option == {}} {
        set inline_commands_option \
            [module_option_value inline-commands]
    }
    if {$inline_commands_option} {
        set _compileproc(options,inline_commands) 1
    }

    if {$debug} {
        puts "compileproc module options set to:"
        puts "inline_containers = $_compileproc(options,inline_containers)"
        puts "inline_controls = $_compileproc(options,inline_controls)"
        puts "cache_commands = $_compileproc(options,cache_commands)"
        puts "skip_constant_increment = $_compileproc(options,skip_constant_increment)"
        puts "cache_variables = $_compileproc(options,cache_variables)"
        puts "inline_commands = $_compileproc(options,inline_commands)"
    }
}

# Generate fake key that we can pass to compileproc_emit_invoke
# so that a named method with a constant argument type will
# be emitted. The argl list is a list of constant strings.
# The inargl list is a list of instrs (original quoted arguments)
# for the key

proc compileproc_generate_key { cmdname argl inargl } {
    global _compileproc_key_info

    set num_args [llength $argl]
    incr num_args 1

    if {![info exists _compileproc_key_info(generated)]} {
        set _compileproc_key_info(generated) 0
    }
    set key "gkey$_compileproc_key_info(generated)"
    incr _compileproc_key_info(generated)

    set types [list]
    for {set i 0} {$i < $num_args} {incr i} {
        lappend types constant
    }
    set values [list]
    set instrs [list]

    lappend values $cmdname
    lappend instrs $cmdname
    for {set i 0} {$i < ($num_args - 1)} {incr i} {
        lappend values [lindex $argl $i]
        lappend instrs [lindex $inargl $i]
    }

    foreach str $values {
        compileproc_constant_cache_add $str
    }

    set _compileproc_key_info($key,types) $types
    set _compileproc_key_info($key,values) $values
    set _compileproc_key_info($key,instrs) $instrs
    set _compileproc_key_info($key,num_args) $num_args

    return $key
}

# Emit code for an inlined while container.

proc compileproc_emit_container_while { key } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    set buffer ""

    if {![descend_container_is_valid $key] || \
            ![descend_container_is_static $key]} {
        error "NOT valid or NOT static for key $key"
    }

    set ccmds [descend_commands $key container]
    if {[llength $ccmds] != 2} {
        error "bad num container commands \{$ccmds\} for key $key"
    }

    set expr_keys [lindex $ccmds 0]
    set body_keys [lindex $ccmds 1]

    # Evaluate while {expr} expression
    set expr_index [descend_container_while_expr $key]

    # Evaluate expression: Note that an inlined expr
    # could contain break or continue commands that
    # should not be inlined.

    # Push loop break/continue context
    if {$_compileproc(options,inline_controls)} {
        compileproc_push_controls_context while 0 0
    }

    emitter_indent_level +1 ; # Indent expr evaluation code properly
    set tuple [compileproc_expr_evaluate_boolean_emit $key $expr_index]
    emitter_indent_level -1

    # Pop loop break/continue context
    if {$_compileproc(options,inline_controls)} {
        compileproc_pop_controls_context while
    }

    set tmpsymbol [lindex $tuple 0]
    set tmpbuffer [lindex $tuple 1]
    set is_constant [lindex $tuple 2]

    set is_constant_loopvar 1
    if {!$is_constant} {
        # A non-constant expression is evaluated inside the loop
        set is_constant_loopvar 1
    } else {
        if {$tmpsymbol == "false"} {
            # The constant expression "false" must have a non-constant
            # loop expression to avoid a compile time error.
            set is_constant_loopvar 0
        } elseif {$tmpsymbol == "true" && $body_keys == {}} {
            # The constant expression "true" must have a non-constant
            # value when there are no commands in the loop so as to
            # avoid a compile time error.
            set is_constant_loopvar 0
        } elseif {$tmpsymbol == "true" && $body_keys != {}} {
            set is_constant_loopvar 1
        }
    }

    if {$is_constant_loopvar} {
        append buffer [emitter_container_for_start {} true]
        if {!$is_constant} {
            # evaluate expr, break out of loop if false
            append buffer \
                $tmpbuffer \
                [emitter_container_for_expr $tmpsymbol]
        }
    } else {
        set bval $tmpsymbol
        set tmpsymbol [compileproc_tmpvar_next]
        set init_buffer "boolean $tmpsymbol = $bval"
        append buffer [emitter_container_for_start $init_buffer $tmpsymbol]
    }

    # body block
    if {$body_keys != {}} {
        if {!$is_constant} {
            append buffer "\n"
        }

        # Setup try/catch for break/continue/return
        append buffer \
            [emitter_container_for_try_start] \
            [emitter_container_fake_tclexception]

        # Push loop break/continue/return context
        if {$_compileproc(options,inline_controls)} {
            compileproc_push_controls_context while 1 1
        }

        foreach body_key $body_keys {
            append buffer [compileproc_emit_invoke $body_key]
        }

        # Pop loop break/continue/return context
        if {$_compileproc(options,inline_controls)} {
            compileproc_pop_controls_context while
        }

        # Finish try/catch for break and continue exceptions
        append buffer [emitter_container_for_try_end]
    }

    # Close loop block and call interp.resetResult()
    append buffer [emitter_container_for_end]

    return $buffer
}

# Emit code for an inlined for container.

proc compileproc_emit_container_for { key } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    set buffer ""

    if {![descend_container_is_valid $key] || \
            ![descend_container_is_static $key]} {
        error "NOT valid or NOT static for key $key"
    }

    set ccmds [descend_commands $key container]
    if {[llength $ccmds] != 4} {
        error "bad num container commands \{$ccmds\} for key $key"
    }

    set start_keys [lindex $ccmds 0]
    set expr_keys [lindex $ccmds 1]
    set next_keys [lindex $ccmds 2]
    set body_keys [lindex $ccmds 3]

    set start_index [descend_container_for_start $key]
    set expr_index [descend_container_for_expr $key]
    set next_index [descend_container_for_next $key]
    set body_index [descend_container_for_body $key]

    # start block executes before the loop.

    if {$start_keys != {}} {
        foreach start_key $start_keys {
            append buffer [compileproc_emit_invoke $start_key]
        }
    }

    # Evaluate expression: Note that an inlined expr
    # could contain break or continue commands that
    # should not be inlined.

    # Push loop break context
    if {$_compileproc(options,inline_controls)} {
        compileproc_push_controls_context for 0 0
    }

    emitter_indent_level +1 ; # Indent expr evaluation code properly
    set tuple [compileproc_expr_evaluate_boolean_emit $key $expr_index]
    emitter_indent_level -1

    # Pop loop break context
    if {$_compileproc(options,inline_controls)} {
        compileproc_pop_controls_context for
    }

    set tmpsymbol [lindex $tuple 0]
    set tmpbuffer [lindex $tuple 1]
    set is_constant [lindex $tuple 2]

    set is_constant_loopvar 1
    if {!$is_constant} {
        # A non-constant expression is evaluated inside the loop
        set is_constant_loopvar 1
    } else {
        if {$tmpsymbol == "false"} {
            # The constant expression "false" must have a non-constant
            # loop expression to avoid a compile time error.
            set is_constant_loopvar 0
        } elseif {$tmpsymbol == "true" && $body_keys == {} && $next_keys == {}} {
            # The constant expression "true" must have a non-constant
            # value when there are no commands in the loop so as to
            # avoid a compile time error.
            set is_constant_loopvar 0
        } elseif {$tmpsymbol == "true" && ($body_keys != {} || $next_keys != {})} {
            set is_constant_loopvar 1
        }
    }

    # next block may require a special skip flag local
    if {$next_keys != {}} {
        set skip_tmpsymbol [compileproc_tmpvar_next skip]
        set skip_init "$skip_tmpsymbol = true"
        set skip_decl "boolean $skip_init"
    } else {
        set skip_tmpsymbol ""
    }

    if {$is_constant_loopvar} {
        set init_buffer ""
        if {$skip_tmpsymbol != ""} {
            set init_buffer ${skip_decl}
        }
        append buffer [emitter_container_for_start $init_buffer true]
    } else {
        # Non-constant loopvar
        set bval $tmpsymbol
        set tmpsymbol [compileproc_tmpvar_next]
        set init_buffer "boolean $tmpsymbol = $bval"

        if {$skip_tmpsymbol != ""} {
            append init_buffer ", $skip_init"
        }
        append buffer [emitter_container_for_start $init_buffer $tmpsymbol]
    }

    # next block executes before the body code inside the loop and
    # is skipped in the first iteration.

    # next block
    if {$next_keys != {}} {
        # check skip local
        append buffer [emitter_container_for_skip_start $skip_tmpsymbol]

        # Setup try/catch for break exception
        append buffer \
            [emitter_container_for_try_start] \
            [emitter_container_fake_tclexception]

        # Push loop break context
        if {$_compileproc(options,inline_controls)} {
            compileproc_push_controls_context for 1 0
        }

        foreach next_key $next_keys {
            append buffer [compileproc_emit_invoke $next_key]
        }

        # Pop loop break context
        if {$_compileproc(options,inline_controls)} {
            compileproc_pop_controls_context for
        }

        # Finish try/catch for break
        append buffer \
            [emitter_container_for_try_end 0] \
            [emitter_container_for_skip_end] \
            "\n"
    }

    if {!$is_constant} {
        # evaluate expr, break out of loop if false
        append buffer \
            $tmpbuffer \
            [emitter_container_for_expr $tmpsymbol]
    }

    # body block
    if {$body_keys != {}} {
        if {!$is_constant} {
            append buffer "\n"
        }

        # Setup try/catch for break and continue exceptions
        append buffer \
            [emitter_container_for_try_start] \
            [emitter_container_fake_tclexception]

        # Push loop break/continue/return context
        if {$_compileproc(options,inline_controls)} {
            compileproc_push_controls_context for 1 1
        }

        foreach body_key $body_keys {
            append buffer [compileproc_emit_invoke $body_key]
        }

        # Pop loop break/continue/return context
        if {$_compileproc(options,inline_controls)} {
            compileproc_pop_controls_context for
        }

        # Finish try/catch for break and continue exceptions
        append buffer [emitter_container_for_try_end]
    }

    # Close loop block and call interp.resetResult()
    append buffer [emitter_container_for_end]

    return $buffer
}


# Emit code for an inlined catch container.

proc compileproc_emit_container_catch { key } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_emit_container_catch $key"
    }

    set buffer ""

# FIXME: If catch variable is not a static string, could we still
# evaluate it and inline the catch block part?

    if {![descend_container_is_valid $key] || \
            ![descend_container_is_static $key]} {
        error "NOT valid or NOT static for key $key"
    }

    # container commands is 0 to N keys
    set body_keys [descend_commands $key container]
    if {$debug} {
        puts "body_keys is \{$body_keys\}"
        puts "descend_container_catch_has_variable is [descend_container_catch_has_variable $key]"
    }

    if {$body_keys == {}} {
        # No body commands to executed, just reset
        # the interp result and set a variable if
        # catch has three arguments.
        if {[descend_container_catch_has_variable $key]} {
            set varname [descend_container_catch_variable $key]
            append buffer [compileproc_container_catch_handler {} true $varname]
        } else {
            append buffer [compileproc_container_catch_handler {} true]
        }
    } else {
        set tmpsymbol [compileproc_tmpvar_next code]
        append buffer \
            [emitter_container_catch_try_start $tmpsymbol] \
            [emitter_container_fake_tclexception]

        # Push loop break/continue context
        if {$_compileproc(options,inline_controls)} {
            compileproc_push_controls_context catch 0 0 0
        }

        foreach body_key $body_keys {
            append buffer [compileproc_emit_invoke $body_key]
        }

        # Pop loop break/continue context
        if {$_compileproc(options,inline_controls)} {
            compileproc_pop_controls_context catch
        }

        append buffer [emitter_container_catch_try_end $tmpsymbol]

        if {[descend_container_catch_has_variable $key]} {
            set varname [descend_container_catch_variable $key]
            append buffer [compileproc_container_catch_handler $tmpsymbol false $varname]
        } else {
            append buffer [compileproc_container_catch_handler $tmpsymbol false]
        }
    }
    return $buffer
}

# Emit catch block handler that typically appears
# after the try block. Pass the name of a variable
# that will be set or null if no variable. If
# no exception is possible then pass null as
# the exsymbol value.

proc compileproc_container_catch_handler { tmpsymbol empty_body {varname "__TJC_NO_VARIABLE"} } {
    set buffer ""
    if {$varname == "__TJC_NO_VARIABLE"} {
        set emitvar 0
    } else {
        set emitvar 1
    }
    if {$emitvar} {
        if {!$empty_body} {
            append buffer [emitter_indent] \
                "TclObject result = interp.getResult()\;\n"
            set value result
        } else {
            set value ""
        }
        append buffer [emitter_indent] \
            "try \{\n"
        emitter_indent_level +1

        # Emit variable assignment

        append buffer [emitter_indent] \
            [compileproc_set_variable $varname true \
                $value [expr {($value == "") ? true : false}]] \
            "\;\n"

        emitter_indent_level -1
        append buffer [emitter_indent] \
            "\} catch (TclException ex) \{\n"
        emitter_indent_level +1
        append buffer [emitter_indent] \
            "TJC.catchVarErr(interp)\;\n"
        emitter_indent_level -1
        append buffer [emitter_indent] \
            "\}\n"
    }

    append buffer [emitter_reset_result]
    if {$empty_body} {
        append buffer [emitter_indent] \
            "interp.setResult(TCL.OK)\;\n"
    } else {
        append buffer [emitter_indent] \
            "interp.setResult($tmpsymbol)\;\n"
    }

    return $buffer
}

# Emit code for an inlined foreach container.

proc compileproc_emit_container_foreach { key } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_emit_container_foreach $key"
    }

    set buffer ""

    if {![descend_container_is_valid $key] || \
            ![descend_container_is_static $key]} {
        error "NOT valid or NOT static for key $key"
    }

    # container commands is 0 to N keys for the body block
    set body_keys [descend_commands $key container]
    if {$debug} {
        puts "body_keys is \{$body_keys\}"
    }

    # The varlist needs to be constant, but the values
    # lists to be iterated over can be a constant, a
    # variable, a command, or a word.

    set varlistvalues [descend_container_foreach_varlistvalues $key]
    if {([llength $varlistvalues] % 2) != 0} {
        error "expected even number of varnames and varlists, got \{$varlistvalues\}"
    }
    set varnamelist_indexes [list]
    set valuelist_indexes [list]

    foreach {i1 i2} $varlistvalues {
        lappend varnamelist_indexes $i1
        lappend valuelist_indexes $i2
    }

    set varnamelists [list]
    set list_symbols [list]

    # Figure out how many list objects will be looped over

    foreach index $varnamelist_indexes {
        set varlist [descend_container_foreach_varlist $key $index]
        lappend varnamelists $varlist
    }

    if {$debug} {
        puts "varnamelists is \{$varnamelists\}"
        puts "varnamelist_indexes is \{$varnamelist_indexes\}"
        puts "valuelist_indexes is \{$valuelist_indexes\}"
    }

    # Declare a local variable to hold a ref to each list value.

    set num_locals [llength $valuelist_indexes]
    for {set i 0} {$i < $num_locals} {incr i} {
        set tmpsymbol [compileproc_tmpvar_next]
        append buffer [emitter_statement "TclObject $tmpsymbol = null"]
        lappend list_symbols $tmpsymbol
    }

    # Open try block after declaring locals that hold the list refs

    append buffer [emitter_container_foreach_try_finally_start]

    # Generate code to evaluate argument into a TclObject symbol

    foreach index $valuelist_indexes tmpsymbol $list_symbols {
        set tuple [compileproc_emit_argument $key $index 0 $tmpsymbol]
        set list_type [lindex $tuple 0]
        set list_symbol [lindex $tuple 1]
        set list_buffer [lindex $tuple 2]

        append buffer $list_buffer
        if {$list_type == "constant"} {
            append buffer [emitter_indent] \
                "$tmpsymbol = $list_symbol\;\n"
        }

        # Invoke TclObject.preserve() to hold a ref. Don't worry
        # about the skip constant incr flag for this module since
        # a foreach is almost always use with a non-constant list
        # and the complexity of not emitting the enclosing try
        # block is not worth it.

        append buffer [emitter_container_foreach_list_preserve $tmpsymbol]
    }

    append buffer [compileproc_container_foreach_loop_start $varnamelists $list_symbols]

    # Emit body commands
    if {$body_keys != {}} {
        append buffer "\n"

        # Setup try/catch for break/continue/return
        append buffer \
            [emitter_container_for_try_start] \
            [emitter_container_fake_tclexception]

        # Push loop break/continue context
        if {$_compileproc(options,inline_controls)} {
            compileproc_push_controls_context foreach 1 1
        }

        foreach body_key $body_keys {
            append buffer [compileproc_emit_invoke $body_key]
        }

        # Pop loop break/continue/return context
        if {$_compileproc(options,inline_controls)} {
            compileproc_pop_controls_context foreach
        }

        append buffer [emitter_container_for_try_end]
    }

    append buffer [compileproc_container_foreach_loop_end $list_symbols]

    return $buffer
}

# Start foreach loop. Pass a list of the varlist arguments
# to the foreach command and a list of the evaluated symbols
# that contain list values.

proc compileproc_container_foreach_loop_start { varlists list_symbols } {
    set buffer ""
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_container_foreach_loop_start: varlists \{$varlists\} : list_symbols \{$list_symbols\}"
    }

    if {[llength $varlists] == 0} {
        error "varlists can't be {}"
    }
    if {[llength $list_symbols] == 0} {
        error "list_symbols can't be {}"
    }

    set multilists [expr {[llength $list_symbols] > 1}]
    if {$multilists} {
        set multivars 1
    } else {
        set varlist [lindex $varlists 0]
        set multivars [expr {[llength $varlist] > 1}]
    }

    set tmpsymbols [list]
    set list_symbol_lengths [list]

    foreach list_symbol $list_symbols {
        append buffer [emitter_container_foreach_list_length $list_symbol]
        lappend list_symbol_lengths "${list_symbol}_length"
    }
    if {$multilists} {
        set num_loops_tmpsymbol [compileproc_tmpvar_next num_loops]
        set max_loops_tmpsymbol [compileproc_tmpvar_next max_loops]
        append buffer [emitter_indent] \
            "int $num_loops_tmpsymbol, $max_loops_tmpsymbol = 0\;\n"
        foreach varlist $varlists list_symbol $list_symbols {
            set num_variables [llength $varlist]
            if {$num_variables > 1} {
                append buffer [emitter_indent] \
                    "$num_loops_tmpsymbol =\
                        (${list_symbol}_length + $num_variables - 1) / $num_variables\;\n"
            } else {
                append buffer [emitter_indent] \
                    "$num_loops_tmpsymbol = ${list_symbol}_length\;\n"
            }
            append buffer [emitter_indent] \
                "if ( $num_loops_tmpsymbol > $max_loops_tmpsymbol ) \{\n"
            emitter_indent_level +1
            append buffer [emitter_indent] \
                "$max_loops_tmpsymbol = $num_loops_tmpsymbol\;\n"
            emitter_indent_level -1
            append buffer [emitter_indent] \
                "\}\n"
        }
    }

    # Newline before for loop
    append buffer "\n"

    # Create for loop
    set tmpi [compileproc_tmpvar_next index]
    set init_buffer "int $tmpi = 0"

    if {!$multilists} {
        # Iterating over a single list
        #set list_symbol [lindex $list_symbols 0]
        set list_symbol_length [lindex $list_symbol_lengths 0]
        set test_buffer "$tmpi < $list_symbol_length"
    } else {
        # Iterating over multiple lists
        set test_buffer "$tmpi < $max_loops_tmpsymbol"
    }

    if {$multilists || !$multivars} {
        set incr_buffer "${tmpi}++"
    } else {
        set varlist [lindex $varlists 0]
        set incr_buffer "$tmpi += [llength $varlist]"
    }
    append buffer \
        [emitter_container_for_start $init_buffer $test_buffer $incr_buffer]

    # Map tmpsymbol to tuple {varname list_symbol list_symbol_length mult offset index}

    if {!$multilists} {
        set list_symbol [lindex $list_symbols 0]
        set list_symbol_length [lindex $list_symbol_lengths 0]
        set varlist [lindex $varlists 0]
        for {set i 0} {$i < [llength $varlist]} {incr i} {
            set varname [lindex $varlist $i]
            set tmpsymbol [compileproc_tmpvar_next]
            lappend tmpsymbols $tmpsymbol
            set tuple [list $varname $list_symbol $list_symbol_length 1 $i $tmpi]
            set tmpsymbols_map($tmpsymbol) $tuple
        }
    } else {
        foreach varlist $varlists \
                list_symbol $list_symbols \
                list_symbol_length $list_symbol_lengths {
            set mult [llength $varlist]
            for {set i 0} {$i < [llength $varlist]} {incr i} {
                set varname [lindex $varlist $i]
                set tmpsymbol [compileproc_tmpvar_next]
                lappend tmpsymbols $tmpsymbol
                if {$mult > 1} {
                    set isym "${list_symbol}_index"
                } else {
                    set isym $tmpi
                }
                set tuple [list $varname $list_symbol $list_symbol_length \
                    $mult $i $isym]
                set tmpsymbols_map($tmpsymbol) $tuple
            }
        }
    }

    if {$debug} {
        parray tmpsymbols_map
    }

    # Determine indexes for each list if needed

    if {$multilists} {
        foreach tmpsymbol $tmpsymbols {
            set tuple $tmpsymbols_map($tmpsymbol)
            foreach {varname list_symbol list_symbol_length mult offset ivar} \
                $tuple break

            if {$mult > 1} {
                if {[info exists declared_ivar($ivar)]} {
                    continue
                }
                append buffer [emitter_indent] \
                    "int $ivar = $tmpi * $mult\;\n"
                set declared_ivar($ivar) 1
            }
        }
    }

    # Query TclObject value at list index.

    foreach tmpsymbol $tmpsymbols {
        set tuple $tmpsymbols_map($tmpsymbol)
        foreach {varname list_symbol list_symbol_length mult offset ivar} \
            $tuple break

        if {!$multivars && !$multilists} {
            set index $tmpi
        } elseif {$multivars || $multilists} {
            if {$offset == 0} {
                set index "$ivar"
            } else {
                set index "$ivar + $offset"
            }
        }

        if {!$multivars && !$multilists} {
            append buffer [emitter_indent] \
                "TclObject $tmpsymbol = TclList.index(interp, $list_symbol, $index)\;\n"
        } elseif {$multilists || $multivars} {
            append buffer \
                [emitter_statement "TclObject $tmpsymbol = null"] \
                [emitter_indent] \
                "if ( $index < $list_symbol_length ) \{\n"
            emitter_indent_level +1
            append buffer [emitter_indent] \
                "$tmpsymbol = TclList.index(interp, $list_symbol, $index)\;\n"
            emitter_indent_level -1
            append buffer [emitter_indent] \
                "\}\n"
        }
    }

    # Set variables

    foreach tmpsymbol $tmpsymbols {
        set tuple $tmpsymbols_map($tmpsymbol)
        foreach {varname list_symbol list_symbol_length mult offset ivar} \
            $tuple break

        append buffer [emitter_container_foreach_var_try_start]
        if {!$multivars && !$multilists} {
            append buffer [emitter_indent] \
                [compileproc_set_variable $varname true $tmpsymbol false] "\;\n"
        } elseif {$multilists || $multivars} {
            append buffer [emitter_indent] \
                "if ( $tmpsymbol == null ) \{\n"
            emitter_indent_level +1
            append buffer [emitter_indent] \
                [compileproc_set_variable $varname true "" true] "\;\n"
            emitter_indent_level -1
            append buffer [emitter_indent] \
                "\} else \{\n"
            emitter_indent_level +1
            append buffer [emitter_indent] \
                [compileproc_set_variable $varname true $tmpsymbol false] "\;\n"
            emitter_indent_level -1
            append buffer [emitter_indent] \
                "\}\n"
        }
        append buffer [emitter_container_foreach_var_try_end $varname]
    }

    return $buffer
}

# End foreach loop

proc compileproc_container_foreach_loop_end { list_symbols } {
    set buffer ""

    # End for loop and reset interp result
    append buffer [emitter_container_for_end]

    # Add finally block to decrement ref count for lists
    append buffer [emitter_container_foreach_try_finally]

    foreach list_symbol $list_symbols {
        append buffer [emitter_container_foreach_list_release $list_symbol]
    }

    # Close try/finally block around loop
    append buffer [emitter_container_foreach_try_finally_end]

    return $buffer
}

# Emit code for an inlined switch container. This container
# has two flavors. In the general case, use the switch
# command at runtime to search for a body to execute.
# If the mode is exact matching and each of the patterns
# is a constant string then an optimized search is used.

proc compileproc_emit_container_switch { key } {
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    set buffer ""

    if {![descend_container_is_valid $key] || \
            ![descend_container_is_static $key]} {
        error "NOT valid or NOT static for key $key"
    }

    # Check flag that disables the special constant string
    # code for a switch command.
    set inline_constant_strings 1

    global _compileproc
    set ind [lsearch -exact $_compileproc(options,inline_containers) \
        switch_no_constant_strings]
    if {$ind != -1} {
        set inline_constant_strings 0
    }

    # No special inline for -glob and -regexp modes.

    set switch_mode [descend_container_switch_mode $key]
    if {$switch_mode != "default" && $switch_mode != "exact"} {
        set inline_constant_strings 0
        if {$debug} {
            puts "inline_constant_strings set to 0 because mode is $switch_mode"
        }
    }

    # container commands is 0 to N lists of keys for each body block
    set body_keys [descend_commands $key container]
    if {$debug} {
        puts "body_keys is \{$body_keys\}"
    }

    set pbIndexes [descend_container_switch_patbody_indexes $key]
    set pbIndex [lindex $pbIndexes 0]

    # If one of the patterns is a variable or a command, then
    # can't use special inline code.

    if {$inline_constant_strings} {
        # Check that each pattern argument is a constant string.
        foreach {pIndex bIndex} $pbIndexes {
            set tuple [compileproc_get_argument_tuple $key $pIndex]
            set type [lindex $tuple 0]
            if {$type != "constant"} {
                set inline_constant_strings 0
                if {$debug} {
                    puts "inline_constant_strings set to 0 because pattern index $pIndex is type $type"
                }
            }
        }
    }

    set sIndex [descend_container_switch_string $key]

    # Get string argument as TclObject
    set tuple [compileproc_emit_argument $key $sIndex]
    set str_type [lindex $tuple 0]
    set str_symbol [lindex $tuple 1]
    set str_buffer [lindex $tuple 2]
    append buffer $str_buffer
    # Get string argument as a String
    set string_tmpsymbol [compileproc_tmpvar_next]
    append buffer [emitter_indent] \
        "String $string_tmpsymbol = $str_symbol.toString()\;\n"

    # If no -- appears before the string argument
    # then the string can't start with a "-" character.

    if {![descend_container_switch_has_last $key]} {
        append buffer [emitter_indent] \
            "TJC.switchStringIsNotOption(interp, $string_tmpsymbol)\;\n"
    }

    if {$inline_constant_strings} {
        append buffer \
            [compileproc_emit_container_switch_constant $key $string_tmpsymbol]
        return $buffer
    }

    # Not the optimized version for constant strings, invoke
    # the runtime implementation for the switch command that
    # will locate the switch block to execute. Don't worry
    # about the skip constant incrment switch here.

    # Declare match offset variable
    set offset_tmpsymbol [compileproc_tmpvar_next]
    append buffer [emitter_statement "int $offset_tmpsymbol"]

    # Allocate array of TclObject and open try block
    set array_tmpsymbol [compileproc_tmpvar_next objv]
    set size [llength $pbIndexes]
    append buffer [emitter_container_switch_start $array_tmpsymbol $size]

    # Assign values to the proper indexes in the array.

    set tmpsymbol [compileproc_tmpvar_next]
    append buffer [emitter_statement "TclObject $tmpsymbol"]

    set pattern_comments [list]
    set i 0
    foreach {patIndex bodyIndex} $pbIndexes {
        # Pattern description
        set tuple [compileproc_argument_printable $key $patIndex]
        set str [lindex $tuple 1]
        set comment "Pattern $str"
        lappend pattern_comments $comment
        append buffer [emitter_comment $comment]

        # Evaluate argument code
        set tuple [compileproc_emit_argument $key $patIndex 0 $tmpsymbol]
        set pat_type [lindex $tuple 0]
        set pat_symbol [lindex $tuple 1]
        set pat_buffer [lindex $tuple 2]
        append buffer \
            $pat_buffer \
            [emitter_container_switch_assign $array_tmpsymbol $i \
            $tmpsymbol $pat_symbol]
        incr i 1

        # If the fallthrough "-" body was given, then pass a constant
        # string to indicate that. Otherwise, this method would assume
        # a body block was compiled if a null TclObject body is passed.

        if {[descend_container_switch_is_fallthrough $key $bodyIndex]} {
            # Body description
            append buffer [emitter_comment "- fallthrough"]

            set tuple [compileproc_emit_argument $key $bodyIndex]
            set body_type [lindex $tuple 0]
            if {$body_type != "constant"} {
                error "expected body to be constant type, got $body_type"
            }
            set body_symbol [lindex $tuple 1]
            set body_buffer [lindex $tuple 2]
            append buffer \
                $body_buffer \
                [emitter_container_switch_assign $array_tmpsymbol $i \
                    $tmpsymbol $body_symbol]
        }
        incr i 1
    }

    # call invokeSwitch(), close try block, and releaseObjv() in finally block

    append buffer [emitter_container_switch_invoke \
        $offset_tmpsymbol \
        $array_tmpsymbol 0 $size \
        $string_tmpsymbol \
        $switch_mode \
        ]

    # If blocks for body blocks based on body offset.
    set offsets [list]
    foreach {patIndex bodyIndex} $pbIndexes {
        if {[descend_container_switch_is_fallthrough $key $bodyIndex]} {
            lappend offsets ""
        } else {
            set offset [expr {$bodyIndex - $pbIndex}]
            lappend offsets $offset
        }
    }
    if {[llength $offsets] == 0} {error "empty offsets list"}
    append buffer \
        [emitter_reset_result] \
        [emitter_container_if_start "$offset_tmpsymbol == -1"] \
        [emitter_indent] \
        "// No match\n"
    for {set i 0} {$i < [llength $offsets]} {incr i} {
        set offset [lindex $offsets $i]
        if {$offset == ""} {
            # Fall through body block
            continue
        }
        append buffer [emitter_container_if_else_if "$offset_tmpsymbol == $offset"]

        set pattern_comment [lindex $pattern_comments $i]
        if {$pattern_comment != ""} {
            append buffer [emitter_comment $pattern_comment]
        }

        foreach body_key [lindex $body_keys $i] {
            append buffer [compileproc_emit_invoke $body_key]
        }
    }
    append buffer \
        [emitter_container_if_else] \
        [emitter_indent] \
        "throw new TclRuntimeError(\"bad switch body offset \" +\n"
    emitter_indent_level +1
    append buffer [emitter_statement "String.valueOf($offset_tmpsymbol))"]
    emitter_indent_level -1
    append buffer [emitter_container_if_end]

    return $buffer
}

# Emit inlined constant string switch code. This is a faster
# version that is only used when the mode is exact and the
# patterns are all constant strings.

proc compileproc_emit_container_switch_constant { key string_tmpsymbol } {
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_emit_container_switch_constant $key $string_tmpsymbol"
    }

    set buffer ""

    set body_keys [descend_commands $key container]
    set pbIndexes [descend_container_switch_patbody_indexes $key]

    set length "${string_tmpsymbol}_length"
    set first "${string_tmpsymbol}_first"

    append buffer [emitter_indent] \
        "int $length = ${string_tmpsymbol}.length()\;\n" \
        [emitter_indent] \
        "char $first = '\\n'\;\n" \
        [emitter_container_if_start "$length > 0"] \
        [emitter_indent] \
        "$first = ${string_tmpsymbol}.charAt(0)\;\n" \
        [emitter_container_if_end] \
        [emitter_reset_result]

    emitter_indent_level +2
    set spacer2 [emitter_indent]
    emitter_indent_level -1
    set spacer1 [emitter_indent]
    emitter_indent_level -1

    set i 0
    set last_pattern [lindex $pbIndexes end-1]
    set ifnum 0
    set fallthrough_expression ""

    foreach {patIndex bodyIndex} $pbIndexes {
        # Inline pattern strings as Java String objects.

        set tuple [compileproc_get_argument_tuple $key $patIndex]
        set pattern [lindex $tuple 1]
        set cmap [lindex $tuple 3]
        if {$debug} {
            puts "pattern ->$pattern<- at index $patIndex"
            puts "pattern cmap is \{$cmap\}"
        }

        if {$cmap == {}} {
            # Grab the first character out of a pattern
            # that contains no backslash elements.
            set pattern_first [string index $pattern 0]
            set pattern_len [string length $pattern]
        } else {
            # The constant string pattern contains backslashes.
            # Extract from 1 to N characters from the pattern
            # that correspond to 1 character from the original
            # Tcl string.
            set first_num_characters [lindex $cmap 0]
            set first_end_index [expr {$first_num_characters - 1}]
            set pattern_first [string range $pattern 0 $first_end_index]

            # Pattern length is length of Tcl string, not the
            # length of the escaped string.
            set pattern_len [llength $cmap]
        }
        if {$debug} {
            puts "pattern_first is ->$pattern_first<-"
            puts "pattern_len is $pattern_len"
        }

        set pattern_jstr [emitter_backslash_tcl_string $pattern]
        set pattern_first_jstr [emitter_backslash_tcl_string $pattern_first]
        if {$debug} {
            puts "pattern_jstr is ->$pattern_jstr<-"
            puts "pattern_first_jstr is '$pattern_first_jstr'"
        }

        set expression ""
        if {$pattern_len > 0} {
            append expression \
                "$length == $pattern_len && $first == '$pattern_first_jstr'"
            if {$pattern_len > 1} {
                append expression "\n" $spacer2 \
                    "&& ${string_tmpsymbol}.compareTo(\"$pattern_jstr\") == 0"
            }
        } else {
            append expression \
                "$length == 0"
        }

        if {[descend_container_switch_is_fallthrough $key $bodyIndex]} {
            # Double check fallthrough container commands
            if {[lindex $body_keys $i] != {}} {
                error "expected empty body keys for index $i, got \{[lindex $body_keys $i]\}"
            }
        
            if {$fallthrough_expression != ""} {
                append fallthrough_expression $spacer1
            }
            append fallthrough_expression "( $expression ) ||\n"
            incr i
            continue
        }
        if {$fallthrough_expression != ""} {
            append fallthrough_expression \
                $spacer1 \
                "( $expression )"
            set expression $fallthrough_expression
            set fallthrough_expression ""
        }

        if {($patIndex == $last_pattern) && ($pattern == "default")} {
            if {$ifnum == 0} {
                append buffer [emitter_container_if_start true]
            } else {
                append buffer [emitter_container_if_else]
            }
        } elseif {$ifnum == 0} {
            append buffer [emitter_container_if_start $expression]
        } else {
            append buffer [emitter_container_if_else_if $expression]
        }
        incr ifnum

        # Argument description
        set tuple [compileproc_argument_printable $key $patIndex]
        set astr [lindex $tuple 1]
        append buffer [emitter_indent] \
            "// Pattern $astr\n"

        # Emit commands

        foreach body_key [lindex $body_keys $i] {
            append buffer [compileproc_emit_invoke $body_key]
        }

        incr i
    }
    if {$fallthrough_expression != ""} {
        error "should not have fallen through past last body"
    }

    append buffer [emitter_container_if_end]

    return $buffer
}

# Generate code to set a variable to a value. This method
# assumes that a variable name is statically defined.
# This method will emit different code for scalar vs
# array variables.

proc compileproc_set_variable { varname varname_is_string value value_is_string } {
    if {$value_is_string} {
        # FIXME: Would be better to do this in the emitter layer, would
        # require adding is_value_string argument to emitter_set_var + scalar func.
        set jstr [emitter_backslash_tcl_string $value]
        set value "\"$jstr\""
    }

    if {$varname_is_string} {
        # static variable name, emit either array or scalar assignment
        set vinfo [descend_simple_variable $varname]
        if {[lindex $vinfo 0] == "array"} {
            set p1 [lindex $vinfo 1]
            set p2 [lindex $vinfo 2]
            # FIXME: We pass a flags argument here, but not to updateCache(),
            # should flags be passed there or should we just always inline
            # 0 as the flags value?
            return [emitter_set_var $p1 true $p2 true $value 0]
        } elseif {[lindex $vinfo 0] == "scalar"} {
            return [compileproc_emit_scalar_variable_set $varname $value]
        } else {
            error "unexpected result \{$vinfo\} from descend_simple_variable"
        }
    } else {
        # Non-static variable name, can't use cache so handle with
        # interp.setVar()
        return [emitter_set_var $varname false null false $value 0]
    }
}

# Generate code to get a variable to a value. This method
# assumes that a variable name is statically defined.
# This method will emit different code for scalar vs
# array variables.

proc compileproc_get_variable { varname varname_is_string } {
    if {$varname_is_string} {
        # static variable name, emit either array or scalar assignment
        set vinfo [descend_simple_variable $varname]
        if {[lindex $vinfo 0] == "array"} {
            set p1 [lindex $vinfo 1]
            set p2 [lindex $vinfo 2]
            return [emitter_get_var $p1 true $p2 true 0]
        } elseif {[lindex $vinfo 0] == "scalar"} {
            return [compileproc_emit_scalar_variable_get $varname]
        } else {
            error "unexpected result \{$vinfo\} from descend_simple_variable"
        }
    } else {
        # Non-static variable name, can't use cache so handle with
        # interp.getVar()
        return [emitter_get_var $varname false null false 0]
    }
}

# Push a controls context. The default controls context is the whole
# procedure. A new context is pushed when a loop is entered or
# a catch command is encountered. The controls context is used
# to determine when a control command like break, continue, and
# return can be inlined. The error command is a control command
# but it is never inlined.

proc compileproc_push_controls_context { type can_break can_continue {can_return keep} } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_push_controls_context $type $can_break\
            $can_continue $can_return"
    }

    # The default can_return means that the new context should retain the
    # can_return value from the current controls context. This is so that
    # a return in a loop in a proc can be inlined while a return in a catch
    # block is not.

    if {$can_return == "keep"} {
        set tuple [lindex $_compileproc(options,controls_stack) 0]
        set can_return [lindex $tuple 6]
    }

    set tuple [list $type "break" $can_break "continue" $can_continue \
        "return" $can_return]
    if {$debug} {
        puts "push controls stack tuple \{$tuple\}"
    }

    set _compileproc(options,controls_stack) [linsert \
        $_compileproc(options,controls_stack) 0 $tuple]

    if {$debug} {
        puts "controls_stack:"
        foreach tuple $_compileproc(options,controls_stack) {
            puts $tuple
        }
    }
}

# Pop a controls context off the stack.

proc compileproc_pop_controls_context { type } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_pop_controls_context $type"
    }

    set stack $_compileproc(options,controls_stack)
    if {[llength $stack] == 0} {
        error "attempt to pop off empty controls stack : type was $type"
    }
    set tuple [lindex $stack 0]
    set stack [lrange $stack 1 end]

    # Double check that the type matches, this might
    # catch a problem with mismatched push/pops.

    if {$type != [lindex $tuple 0]} {
        error "found controls type [lindex $tuple 0] but expected $type\
            : controls_stack \{$_compileproc(options,controls_stack)\}"
    } elseif {"proc" == [lindex $tuple 0] && $type != "proc"} {
        error "popped proc control context off of controls stack"
    }

    if {$debug} {
        puts "popped controls stack tuple \{$tuple\}"
        puts "controls_stack:"
        foreach tuple $_compileproc(options,controls_stack) {
            puts $tuple
        }
    }

    set _compileproc(options,controls_stack) $stack
}

# Emit an inlined control statement. The control commands
# are break, continue, and return. The error command is
# always raised via a normal command invocation.

proc compileproc_emit_control { key } {
    global _compileproc
    global _compileproc_ckeys

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_emit_control $key"
    }

    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    switch -exact -- $cmdname {
        "::break" -
        "break" {
            set statement "break"
        }
        "::continue" -
        "continue" {
            set statement "continue"
        }
        "::return" -
        "return" {
           set statement "return"
        }
        default {
            error "should have been break or continue command, got $cmdname"
        }
    }

    if {$statement == "return"} {
        return [compileproc_emit_control_return $key]
    } else {
        return [emitter_container_loop_break_continue $statement]
    }
}

# Emit an inlined return command. This is used for
# a return command that has either 0 or 1 arguments.
# A return command that appears inside a catch
# block is not inlined.

proc compileproc_emit_control_return { key } {
    global _compileproc
    global _compileproc_ckeys

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    set buffer ""

    if {$num_args == 1} {
        append buffer \
            [emitter_reset_result] \
            [emitter_control_return]
        return $buffer
    } elseif {$num_args == 2} {
        # No-op
    } else {
        error "expected return with 1 or 2 arguments, got num_args $num_args"
    }

    # Set interp result to TclObject argument. Handle the
    # case of a nested command with optimized code.

    set buffer ""

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]
    set ckeys [lindex $tuple 1]

    if {$type == "command"} {
        # Invoke nested command(s) and leave interp result as-is.
        foreach ckey $ckeys {
            append buffer [compileproc_emit_invoke $ckey]
        }
        append buffer [emitter_control_return]
    } else {
        set tuple [compileproc_emit_argument $key 1]
        set obj_type [lindex $tuple 0]
        set obj_symbol [lindex $tuple 1]
        set obj_buffer [lindex $tuple 2]
        append buffer \
            $obj_buffer \
            [emitter_control_return_argument $obj_symbol]
    }

    # FIXME: It is not really clear that a resetResult() invocation
    # is needed before the interp result is set. We know that resetResult
    # is always invoked before cmdProc() entry. Is there any way that
    # an error can be raised and caught during normal execution in
    # a way the leaves the errorInfo vars in the interp set? I think
    # catch would clear these but more research is needed.

    return $buffer
}

# Emit a container expr command. Note that other container
# commands that have an expr block will use the command
# compileproc_expr_evaluate_boolean_emit to evaluate an
# expression as a boolean.

proc compileproc_emit_container_expr { key } {
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_emit_container_expr $key"
    }

    set buffer ""

    if {![descend_container_is_valid $key] || \
            ![descend_container_is_static $key]} {
        error "NOT valid or NOT static for key $key"
    }

    append buffer [compileproc_expr_evaluate_result_emit $key]
    return $buffer
}

# This method is invoked when an expr command wants
# to evaluate an expression string and set the interp
# result to the value of the expression. A buffer
# containing the code to set the interp result is
# returned by this method.

proc compileproc_expr_evaluate_result_emit { key } {
    global _compileproc_key_info

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_expr_evaluate_result_emit $key"
    }

    set buffer ""

    set expr_index 1
    set expr_result [compileproc_expr_evaluate $key $expr_index]
    if {$debug} {
        puts "expr_result is \{$expr_result\}"
    }
    set eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
        $expr_result]
    set infostr [lindex $eval_tuple 0]
    set ev [lindex $eval_tuple 1]
    append buffer [lindex $eval_tuple 2]

    append buffer [emitter_indent] \
        "TJC.exprSetResult(interp, $ev)\;\n"

    return $buffer
}

# Emit a unary operator after evaluating a value.
# Return a tuple of {EXPRVALUE BUFFER} containing
# the name of an ExprValue variable and
# the bufer that will evaluate the expr value.

proc compileproc_expr_evaluate_emit_unary_operator { op_tuple } {
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_expr_evaluate_emit_unary_operator \{$op_tuple\}"
    }

    set buffer ""

    set type [lindex $op_tuple 0]
    if {$type != {unary operator}} {
        error "expected \{unary operator\} but got type \{$type\}"
    }

    set vtuple [lindex $op_tuple 1]
    set op [lindex $vtuple 0]
    set values [lindex $vtuple 1]

    if {[llength $values] != 1} {
        error "values length should be 1,\
            not [llength $values],\
            values_list was \{$values\}"
    }

    set tuple [lindex $values 0]
    set eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
        $tuple]

    set infostr [lindex $eval_tuple 0]
    set ev [lindex $eval_tuple 1]
    set eval_buffer [lindex $eval_tuple 2]
    set value_info [lindex $eval_tuple 3]
    set value_info_type [lindex $value_info 0]
    set value_info_value [lindex $value_info 1]

    if {$value_info_type == {int literal} ||
            $value_info_type == {double literal}} {
        set is_numeric_literal 1
        set numeric_literal $value_info_value
    } else {
        set is_numeric_literal 0
    }

    if {$debug} {
        puts "op is $op"
        puts "passed tuple \{$tuple\} to compileproc_expr_evaluate_emit_exprvalue"
        puts "value_info_type is $value_info_type"
        puts "value_info_value is $value_info_value"
    }

    # Check for the special case of the smallest
    # possible integer -0x80000000. This number
    # needs to be negated otherwise it will
    # not be a valid 32 bit integer. Regen literal
    # with a negative sign added to the front.

    if {$op == "-" && $value_info_type == "String"} {
        if {$debug} {
            puts "possible smallest int negation: op is $op,\
                \{$value_info_type $value_info_value\}"
        }
        set numeric_literal [string range \
            $value_info_value 1 end-1]
        set numeric_literal [string trim $numeric_literal]
        set first [string index $numeric_literal 0]
        if {$first == "-"} {
            set is_already_negative 1
        } else {
            set is_already_negative 0
        }
        set min "-0x80000000"
        set wneg "-${numeric_literal}"
        set is_jint [compileproc_string_is_java_integer $wneg]
        if {$debug} {
            puts "wneg is $wneg"
            puts "min is $min"
            puts "is_already_negative is $is_already_negative"
            puts "is java integer is $is_jint"
            puts "expr == compare is [expr {$wneg == $min}]"
        }
        if {!$is_already_negative && \
                $is_jint && \
                ($wneg == $min)} {
            set negate_number 1
        } else {
            set negate_number 0
        }
    } elseif {$op == "-" && $is_numeric_literal && \
            ($numeric_literal > 0)} {
        set negate_number 1
    } else {
        set negate_number 0
    }

    if {$negate_number} {
        # Negate numeric literal and regerate
        # eval buffer. This skips a tmpsymbol
        # but it is no big deal.
        set tuple [list [lindex $tuple 0] "-${numeric_literal}"]
        set eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
            $tuple]

        # Don't reset infostr, use the old one
        set ev [lindex $eval_tuple 1]
        set eval_buffer [lindex $eval_tuple 2]
        set skip_unary_op_call 1
    } else {
        set skip_unary_op_call 0
    }

    # Printable info describing this operator and
    # the left and right operands:
    append buffer [emitter_indent] \
        "// Unary operator: $op $infostr\n"

    # Append code to evaluate value
    append buffer $eval_buffer

    switch -exact -- $op {
        "+" {
            set opval TJC.EXPR_OP_UNARY_PLUS
        }
        "-" {
            set opval TJC.EXPR_OP_UNARY_MINUS
        }
        "!" {
            set opval TJC.EXPR_OP_UNARY_NOT
        }
        "~" {
            set opval TJC.EXPR_OP_UNARY_BIT_NOT
        }
        default {
            error "unsupported unary operator \"$op\""
        }
    }

    if {!$skip_unary_op_call} {
        append buffer [emitter_indent] \
            "TJC.exprUnaryOperator(interp, $opval, $ev)\;\n"
    }

    append buffer [emitter_indent] \
        "// End Unary operator: $op\n"

    return [list $ev $buffer]
}

# Emit a binary operator after evaluating a left
# and right value. Return a tuple of {EXPRVALUE BUFFER}
# containing the name of an ExprValue variable and
# the bufer that will evaluate the expr value.

proc compileproc_expr_evaluate_emit_binary_operator { op_tuple } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}
    
    if {$debug} {
        puts "compileproc_expr_evaluate_emit_binary_operator \{$op_tuple\}"
    }

    set buffer ""

    set type [lindex $op_tuple 0]
    if {$type != {binary operator}} {
        error "expected \{binary operator\} but got type \{$type\}"
    }

    set vtuple [lindex $op_tuple 1]
    set op [lindex $vtuple 0]
    set values [lindex $vtuple 1]

    if {[llength $values] != 2} {
        error "values length should be 2,\
            not [llength $values],\
            values_list was \{$values\}"
    }

    # Figure out which operator this is
    set logic_op 0 ; # true if && or || operators

    switch -exact -- $op {
        "*" {
            set opval TJC.EXPR_OP_MULT
        }
        "/" {
            set opval TJC.EXPR_OP_DIVIDE
        }
        "%" {
            set opval TJC.EXPR_OP_MOD
        }
        "+" {
            set opval TJC.EXPR_OP_PLUS
        }
        "-" {
            set opval TJC.EXPR_OP_MINUS
        }
        "<<" {
            set opval TJC.EXPR_OP_LEFT_SHIFT
        }
        ">>" {
            set opval TJC.EXPR_OP_RIGHT_SHIFT
        }
        "<" {
            set opval TJC.EXPR_OP_LESS
        }
        ">" {
            set opval TJC.EXPR_OP_GREATER
        }
        "<=" {
            set opval TJC.EXPR_OP_LEQ
        }
        ">=" {
            set opval TJC.EXPR_OP_GEQ
        }
        "==" {
            set opval TJC.EXPR_OP_EQUAL
        }
        "!=" {
            set opval TJC.EXPR_OP_NEQ
        }
        "&" {
            set opval TJC.EXPR_OP_BIT_AND
        }
        "^" {
            set opval TJC.EXPR_OP_BIT_XOR
        }
        "|" {
            set opval TJC.EXPR_OP_BIT_OR
        }
        "eq" {
            set opval TJC.EXPR_OP_STREQ
        }
        "ne" {
            set opval TJC.EXPR_OP_STRNEQ
        }
        "&&" -
        "||" {
            # These do not invoke a binary operator method.
            set logic_op 1
        }
        default {
            error "unsupported binary operator \"$op\""
        }
    }

    set left_tuple [lindex $values 0]
    set right_tuple [lindex $values 1]

    if {$debug} {
        puts "left_tuple is \{$left_tuple\}"
        puts "right_tuple is \{$right_tuple\}"
    }

    set left_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
        $left_tuple]
    set right_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
        $right_tuple]

    if {1 && $debug} {
        puts "left_eval_tuple is \{$left_eval_tuple\}"
        puts "right_eval_tuple is \{$right_eval_tuple\}"
    }

    set left_infostr [lindex $left_eval_tuple 0]
    set right_infostr [lindex $right_eval_tuple 0]

    set ev1 [lindex $left_eval_tuple 1]
    set ev2 [lindex $right_eval_tuple 1]

    # Check for the common special case of comparing a
    # TclObject to the empty string. This check will
    # work for a variable that has been resolved into
    # a TclObject or a command that has been resolved
    # into a TclObject.

    set left_value_info [lindex $left_eval_tuple 3]
    set right_value_info [lindex $right_eval_tuple 3]

    set left_value_info_type [lindex $left_value_info 0]
    set left_value_info_value [lindex $left_value_info 1]

    set right_value_info_type [lindex $right_value_info 0]
    set right_value_info_value [lindex $right_value_info 1]

    set is_tclobject_string_compare 0
    set opt_tclobject_empty_string_compare 0
    set opt_tclobject_string_compare 0
    set is_left_operand_tclobject 0 ; # Only useful when above is true

    if {$debug} {
        puts "pre empty_string_compare check: op is $op"
        puts "left_value_info_type $left_value_info_type"
        puts "left_value_info_value $left_value_info_value"
        puts "right_value_info_type $right_value_info_type"
        puts "right_value_info_value $right_value_info_value"
    }

    if {($left_value_info_type == "TclObject" && \
            $right_value_info_type == "String") || \
            ($left_value_info_type == "String" && \
                $right_value_info_type == "TclObject")} {
        set is_tclobject_string_compare 1
        set is_left_operand_tclobject [expr {$left_value_info_type == "TclObject"}]
    }

    if {$is_tclobject_string_compare && \
            ($op == "==" || $op == "!=" || $op == "eq" || $op == "ne")} {
        if {$is_left_operand_tclobject} {
            set str $right_value_info_value
        } else {
            set str $left_value_info_value
        }
        # Remove double quotes and test for empty string compare
        set string_literal [string range $str 1 end-1]
        if {$string_literal eq ""} {
            set opt_tclobject_empty_string_compare 1
        } else {
            # If constant string is not empty, use
            # optimization for a non-empty string compare.
            set opt_tclobject_string_compare 1
        }
    }

    if {$debug} {
        puts "post empty_string_compare check: op is $op"
        puts "is_tclobject_string_compare $is_tclobject_string_compare"
        puts "opt_tclobject_empty_string_compare $opt_tclobject_empty_string_compare"
        puts "opt_tclobject_string_compare $opt_tclobject_string_compare"
        puts "is_left_operand_tclobject $is_left_operand_tclobject"
    }

    # Check for special flag used only during code generation testing
    if {[info exists _compileproc(options,expr_no_string_compare_optimizations)]} {
        set opt_tclobject_string_compare 0
        set opt_tclobject_empty_string_compare 0
        if {$debug} {
            puts "expr_no_string_compare_optimizations flag set"
        }
    }

    # Printable info describing this operator and
    # the left and right operands:
    append buffer [emitter_indent] \
        "// Binary operator: $left_infostr $op $right_infostr\n"

    if {$logic_op} {
        # Handle && and || logic operators, the right
        # value is not evaluated until the value of
        # the left one is known.
        append buffer [lindex $left_eval_tuple 2]
        if {$op == "&&"} {
            set not ""
        } elseif {$op == "||"} {
            set not "!"
        } else {
            error "unmatched logic_op \"$op\""
        }

        append buffer \
            [emitter_indent] \
            "if ($not$ev1.getBooleanValue(interp)) \{\n" \
            [lindex $right_eval_tuple 2] \
            [emitter_indent] \
            "$ev1.setIntValue($ev2.getBooleanValue(interp))\;\n" \
            [emitter_indent] \
            "TJC.exprReleaseValue(interp, $ev2)\;\n" \
            [emitter_indent] \
            "\} else \{\n"

        set else_value [expr {($not == "") ? 0 : 1}]
        append buffer \
            [emitter_indent] \
            "$ev1.setIntValue($else_value)\;\n" \
            [emitter_indent] \
            "\} // End if: $not$left_infostr\n"
    } elseif {$opt_tclobject_empty_string_compare} {
        # Special case for: expr {$obj == ""}. Note that
        # we need to regenerate the eval buffer so that
        # the TclObject is not converted to an ExprValue.
        if {$is_left_operand_tclobject} {
            set left_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
                $left_tuple 1]
            set tclobject_sym [lindex $left_eval_tuple 1]
            append buffer [lindex $left_eval_tuple 2]
        } else {
            set right_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
                $right_tuple 1]
            set tclobject_sym [lindex $right_eval_tuple 1]
            append buffer [lindex $right_eval_tuple 2]
        }
        set tmpsymbol [compileproc_tmpvar_next]
        append buffer [emitter_indent] \
            "ExprValue $tmpsymbol = TJC.exprEqualsEmptyString(interp, $tclobject_sym)\;\n"
        if {$op == "!=" || $op == "ne"} {
            # Negate equality test
            append buffer [emitter_indent] \
                "$tmpsymbol.setIntValue(!$tmpsymbol.getBooleanValue(interp))\;\n"
        }
        set ev1 $tmpsymbol
    } elseif {$opt_tclobject_string_compare} {
        # Special case for: expr {$obj == "foo"}. The
        # string that will be compared is non-empty.
        # Note that we need to regenerate the eval
        # buffer so that the TclObject is not converted
        # to an ExprValue.
        if {$is_left_operand_tclobject} {
            set left_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
                $left_tuple 1]
            set tclobject_sym [lindex $left_eval_tuple 1]
            append buffer [lindex $left_eval_tuple 2]
        } else {
            set right_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
                $right_tuple 1]
            set tclobject_sym [lindex $right_eval_tuple 1]
            append buffer [lindex $right_eval_tuple 2]
        }
        # Use string literal from first left/right type test
        set jstr $string_literal
        set int_tmpsymbol [compileproc_tmpvar_next]
        append buffer [emitter_indent] \
            "int $int_tmpsymbol = $tclobject_sym.toString().compareTo(\"$jstr\")\;\n"
        if {$op == "!=" || $op == "ne"} {
            # Negate equality test
            set eqint 0
            set neqint 1
        } else {
            set eqint 1
            set neqint 0
        }
        append buffer [emitter_indent] \
            "$int_tmpsymbol = (($int_tmpsymbol == 0) ? $eqint : $neqint)\;\n"
        set tmpsymbol [compileproc_tmpvar_next]
        append buffer [emitter_indent] \
            "ExprValue $tmpsymbol = TJC.exprGetValue(interp, $int_tmpsymbol, null)\;\n"
        set ev1 $tmpsymbol
    } else {
        # Append code to evaluate left and right values
        append buffer \
            [lindex $left_eval_tuple 2] \
            [lindex $right_eval_tuple 2]

        # Emit TJC binary operator invocation
        append buffer [emitter_indent] \
            "TJC.exprBinaryOperator(interp, $opval, $ev1, $ev2)\;\n"
    }

    append buffer [emitter_indent] \
        "// End Binary operator: $op\n"

    return [list $ev1 $buffer]
}

# Emit a ternary operator like ($b ? 1 : 0). Return a
# tuple of {EXPRVALUE BUFFER} containing the name of
# an ExprValue variable and the bufer that will
# evaluate the expr value.

proc compileproc_expr_evaluate_emit_ternary_operator { op_tuple } {
    #puts "compileproc_expr_evaluate_emit_ternary_operator \{$op_tuple\}"

    set vtuple [lindex $op_tuple 1]
    set op [lindex $vtuple 0]
    set values [lindex $vtuple 1]

    if {[llength $values] != 3} {
        error "values length should be 3,\
            not [llength $values],\
            values_list was \{$values\}"
    }

    set cond_tuple [lindex $values 0]
    set cond_eval_tuple [compileproc_expr_evaluate_emit_exprvalue $cond_tuple]

    set true_tuple [lindex $values 1]
    set true_eval_tuple [compileproc_expr_evaluate_emit_exprvalue $true_tuple]

    set false_tuple [lindex $values 2]
    set false_eval_tuple [compileproc_expr_evaluate_emit_exprvalue $false_tuple]

    set cond_infostr [lindex $cond_eval_tuple 0]
    set true_infostr [lindex $true_eval_tuple 0]
    set false_infostr [lindex $false_eval_tuple 0]

    set ev1 [lindex $cond_eval_tuple 1]
    set ev2 [lindex $true_eval_tuple 1]
    set ev3 [lindex $false_eval_tuple 1]

    append buffer \
        [emitter_indent] \
        "// Ternary operator: $cond_infostr ? $true_infostr : $false_infostr\n" \
        [lindex $cond_eval_tuple 2] \
        [emitter_indent] \
        "if ($ev1.getBooleanValue(interp)) \{\n" \
        [lindex $true_eval_tuple 2] \
        [emitter_indent] \
        "TJC.exprReleaseValue(interp, $ev1)\;\n" \
        [emitter_indent] \
        "$ev1 = $ev2\;\n" \
        [emitter_indent] \
        "\} else \{\n" \
        [lindex $false_eval_tuple 2] \
        [emitter_indent] \
        "TJC.exprReleaseValue(interp, $ev1)\;\n" \
        [emitter_indent] \
        "$ev1 = $ev3\;\n" \
        [emitter_indent] \
        "\}\n" \
        [emitter_indent] \
        "// End Ternary operator: ?\n"

    return [list $ev1 $buffer]
}

# Emit a math function like pow(2,2). Return a
# tuple of {EXPRVALUE BUFFER} containing the name of
# an ExprValue variable and the bufer that will
# evaluate the expr value.

proc compileproc_expr_evaluate_emit_math_function { op_tuple } {
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_expr_evaluate_emit_math_function \{$op_tuple\}"
    }

    set buffer ""

    # There is no checking of the number of arguments to
    # a math function. We just pass in however many
    # the user passed and expect that an exception will
    # be raised if the wrong number of args were passed.

    set vtuple [lindex $op_tuple 1]
    set funcname [lindex $vtuple 0]
    set values [lindex $vtuple 1]

    set infostrs [list]
    set evsyms [list]
    set buffers [list]

    # Emit ExprValue for each argument to the math function
    set len [llength $values]
    for {set i 0} {$i < $len} {incr i} {
        set tuple [lindex $values $i]
        set eval_tuple [compileproc_expr_evaluate_emit_exprvalue $tuple]
        foreach {infostr ev eval_buffer} $eval_tuple {break}

        lappend infostrs $infostr
        lappend evsyms $ev
        lappend buffers $eval_buffer
    }

    append buffer [emitter_indent] \
        "// Math function: $funcname"
    foreach infostr $infostrs {
        append buffer " " $infostr
    }
    append buffer "\n"

    # Append code to create ExprValue objects and assign to values array

    for {set i 0} {$i < $len} {incr i} {
        set eval_buffer [lindex $buffers $i]
        append buffer $eval_buffer
    }

    # Invoke runtime access to math function. Use an
    # optimized runtime method for int() and double().

    if {$len == 1 && ($funcname == "int" || $funcname == "double")} {
        set ev [lindex $evsyms 0]
        if {$funcname == "int"} {
            set tjc_func "TJC.exprIntMathFunction"
        } else {
            set tjc_func "TJC.exprDoubleMathFunction"
        }
        append buffer [emitter_indent] \
            "${tjc_func}(interp, $ev)\;\n"
        set result_tmpsymbol $ev
    } else {
        set values_tmpsymbol [compileproc_tmpvar_next]
        append buffer [emitter_indent] \
            "ExprValue\[\] $values_tmpsymbol = new ExprValue\[$len\]\;\n"

        for {set i 0} {$i < $len} {incr i} {
            set ev [lindex $evsyms $i]
            append buffer [emitter_indent] \
                "$values_tmpsymbol\[$i\] = $ev\;\n"
        }

        set result_tmpsymbol [compileproc_tmpvar_next]
        set jstr [emitter_backslash_tcl_string $funcname]
        append buffer [emitter_indent] \
            "ExprValue $result_tmpsymbol = " \
            "TJC.exprMathFunction(interp, \"$jstr\", $values_tmpsymbol)\;\n"
    }

    append buffer [emitter_indent] \
        "// End Math function: $funcname\n"

    return [list $result_tmpsymbol $buffer]
}

# Emit code to get an expression value for the given string.
# Note that a value could require that a subexpression with
# an operator could need to be evaluated before a value is known.
# If the no_exprvalue_for_tclobject flag is set to true then
# an ExprValue object will not be allocated in the case where
# the value is a TclObject.

proc compileproc_expr_evaluate_emit_exprvalue { tuple {no_exprvalue_for_tclobject 0} } {
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_expr_evaluate_emit_exprvalue \{$tuple\} $no_exprvalue_for_tclobject"
    }

    set buffer ""
    set infostr ""

    set result_type ""
    set result_symbol ""
    set srep ""

    set type [lindex $tuple 0]
    set value [lindex $tuple 1]

    switch -exact -- $type {
        {constant boolean} -
        {constant string} -
        {constant braced string} -
        {constant} {
            if {$type == {constant boolean}} {
                # Don't need shortcut for constant boolean value
                set value [lindex $tuple 2]
            }
            # Generate Java string before possible backslash subst
            set jvalue [emitter_backslash_tcl_string $value]

            # A double quoted constant string could contain
            # backslashes like \n or \t at this point.
            # Subst them while testing if number is a
            # integer or double.
            if {$type == {constant string}} {
                set value [subst -nocommands -novariables \
                    $value]
            }
            set is_integer [compileproc_string_is_java_integer $value]
            set is_double [compileproc_string_is_java_double $value]
            if {$is_integer || $is_double} {
                set tuple [compileproc_parse_value $value]
                set stringrep_matches [lindex $tuple 0]
                set parsed_number [lindex $tuple 1]
                set printed_number [lindex $tuple 2]

                if {$stringrep_matches} {
                    set srep null
                } else {
                    set srep "\"$jvalue\""
                }
                if {$is_integer} {
                    set result_type "int literal"
                } else {
                    set result_type "double literal"
                }
                set result_symbol $printed_number
                set infostr $printed_number
            } else {
                set result_type "String"
                set result_symbol "\"$jvalue\""
                set infostr $result_symbol

                # FIXME: Create a better printable string
                # scanning function that can determine which
                # characters in a Tcl string can't appear in
                # a Java comment and does not print those.
                # This method will print "\n" right now,
                # which is likely ok but banned by the
                # earlier printing checks.
            }
        }
        {variable scalar} -
        {variable array} {
            # Evaluate variable
            if {$type == {variable scalar}} {
                set varname $value
                set vinfo [list scalar $value]
            } else {
                set varname "[lindex $value 1 0](...)"
                set vinfo $value
            }

            set tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                [compileproc_emit_variable $tmpsymbol $vinfo]

            set result_type "TclObject"
            set result_symbol $tmpsymbol
            set infostr "\$$varname"
        }
        {unary operator} {
            set eval_tuple [compileproc_expr_evaluate_emit_unary_operator \
                $tuple]
            set infostr "()"
            set result_type "ExprValue"
            set result_symbol [lindex $eval_tuple 0]
            append buffer [lindex $eval_tuple 1]
        }
        {binary operator} {
            set eval_tuple [compileproc_expr_evaluate_emit_binary_operator \
                $tuple]
            set infostr "()"
            set result_type "ExprValue"
            set result_symbol [lindex $eval_tuple 0]
            append buffer [lindex $eval_tuple 1]
        }
        {ternary operator} {
            set eval_tuple [compileproc_expr_evaluate_emit_ternary_operator \
                $tuple]
            set infostr "(?:)"
            set result_type "ExprValue"
            set result_symbol [lindex $eval_tuple 0]
            append buffer [lindex $eval_tuple 1]
        }
        {math function} {
            set eval_tuple [compileproc_expr_evaluate_emit_math_function \
                $tuple]
            set infostr "math()"
            set result_type "ExprValue"
            set result_symbol [lindex $eval_tuple 0]
            append buffer [lindex $eval_tuple 1]
        }
        {command operand} {
            set ccmds $value
            if {$debug} {
                puts "container commands for command operand are \{$ccmds\}"
            }
            if {$ccmds == {}} {
                set infostr "\[\]"
                set result_type "String"
                set result_symbol "\"\"" ; # empty Java String
            } else {
                set infostr "\[...\]"
                foreach ckey $ccmds {
                    append buffer [compileproc_emit_invoke $ckey]
                }
                set tmpsymbol [compileproc_tmpvar_next]
                append buffer [emitter_indent] \
                    "TclObject $tmpsymbol = interp.getResult()\;\n"
                set result_type "TclObject"
                set result_symbol $tmpsymbol
            }
        }
        {word operand} {
            # A word is made up of command, string, and variable elements.
            set types [lindex $value 0]
            set values [lindex $value 1]
            set cmap [lindex $value 2]
            if {$debug} {
                puts "expr value is a {word operand}:"
                puts "types is \{$types\}"
                puts "values is \{$values\}"
                puts "cmap is \{$cmap\}"
            }

            set winfo $values

            set tmpsymbol [compileproc_tmpvar_next]

            # FIXME: Pass declare_flag here?
            append buffer \
                [compileproc_emit_word $tmpsymbol $winfo]

            set result_type "TclObject"
            set result_symbol $tmpsymbol
            set infostr "\"...\""
        }
        default {
            error "unhandled type \"$type\""
        }
    }

    if {$debug} {
        puts "result_type is \"$result_type\""
        puts "result_symbol is ->$result_symbol<-"
        puts "srep is \"$srep\""
    }

    # Invoke one of the TJC.exprGetValue() methods, there is
    # one for int, double, String, and TclObject argument types.

    if {$no_exprvalue_for_tclobject && $result_type == "TclObject"} {
        # Return TclObject symbol instead of creating an ExprValue
        set value_symbol $result_symbol
    } elseif {$result_type != "ExprValue"} {
        set tmpsymbol [compileproc_tmpvar_next]
        append buffer [emitter_indent] \
            "ExprValue $tmpsymbol = "
        set value_symbol $tmpsymbol
    } else {
        set value_symbol $result_symbol
    }

    if {$no_exprvalue_for_tclobject && $result_type == "TclObject"} {
       # No-op
    } elseif {$result_type == "int literal" || $result_type == "double literal"} {
        append buffer "TJC.exprGetValue(interp, $result_symbol, $srep)"
    } elseif {$result_type == "String" || $result_type == "TclObject"} {
        append buffer "TJC.exprGetValue(interp, $result_symbol)"
    } elseif {$result_type == "ExprValue"} {
        # No-op
    } else {
        error "unknown result type \"$result_type\""
    }
    if {$no_exprvalue_for_tclobject && $result_type == "TclObject"} {
        # No-op
    } elseif {$result_type != "ExprValue"} {
        append buffer "\;\n"
    }

    set result_info [list $result_type $result_symbol]

    return [list $infostr $value_symbol $buffer $result_info]
}

# Return true if the given string can be
# represented as a Java integer type.
# This means it fits into a 32bit int
# type.

proc compileproc_string_is_java_integer { tstr } {
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_string_is_java_integer \"$tstr\""
    }

    # A Java int type must be in the range -2147483648 to 2147483647.
    # A Tcl implementation might support only 32 bit integer operations,
    # or it might promote integers to wide integers if numbers get too
    # large. Check that the value can be represented as some kind of integer
    # and then check the range.

    if {$tstr == "" || ![string is integer $tstr]} {
        if {$debug} {
            puts "string \"$tstr\" is not an integer as defined by string is integer"
        }
        return 0
    }

    # Check that the integer value is within the range of valid
    # integers. Do this by comparing floating point values.
    set min -2147483648.0
    set max  2147483647.0

    # For integers that consist of only decimal digits, it is
    # possible to parse the integer as a double. For example,
    # 2147483648 can't be represented as an integer but it
    # could be represented as a 2147483648.0 double value.
    # If the integer looks like a decimal integer, then
    # append a decimal point, otherwise convert it to a
    # double after parsing as an integer.

    set tstr [string trim $tstr]
    if {[regexp {^(\-|\+)?(0x|0X)?0+$} $tstr]} {
        set fnum "0.0"
    } elseif {[regexp {^(\-|\+)?[1-9][0-9]*$} $tstr]} {
        if {$debug} {
            puts "tstr looks like a decimal integer, will parse it as a double"
        }
        set fnum "${tstr}.0"
    } else {
        if {$debug} {
            puts "tstr does not look like a decimal integer, parsing as int"
        }

        # An integer in a non-decimal base needs to be checked for
        # overflow by parsing as an unsigned number and checking
        # to see if the integer became negative.

        set is_neg 0
        set c [string index ${tstr} 0]
        if {$c == "+" || $c == "-"} {
            if {$c == "-"} {
                set is_neg 1
            }
            set ptstr [string range $tstr 1 end]
        } else {
            set ptstr $tstr
        }
        set fnum [expr {int($ptstr)}]
        if {$is_neg && $fnum == $min} {
            # Special case for -0x80000000, this is a valid java integer
            set fnum $min
        } elseif {$fnum <= 0} {
            # Unsigned number must have overflowed, or been chopped to zero
            if {$debug} {
                puts "insigned integer \"$ptstr\" causes 32bit overflow, not a java integer"
            }
            return 0
        } else {
            if {$is_neg} {
                set fnum [expr {-1.0 * $fnum}]
            }
        }
    }
    set fnum [expr {double($fnum)}]
    if {$debug} {
        puts "tstr is $tstr"
        puts "fnum is $fnum"
    }

    if {($fnum > 0.0 && $fnum > $max) ||
            ($fnum < 0.0 && $fnum < $min)} {
        if {$debug} {
            puts "string \"$tstr\" is outside the java integer range"
        }
        return 0
    }
    return 1
}

# Return true if the given string can be
# represented as a Java double type. If
# the string could be an integer then
# this method will return 0.

proc compileproc_string_is_java_double { tstr } {
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_string_is_java_double \"$tstr\""
    }

    if {$tstr == "" || ![string is double $tstr]} {
        return 0
    }
    # String like "1.0" and "1e6" are valid doubles,
    # but "9999999999999999" is not.
    set has_decimal [expr {[string first "." $tstr] != -1}]
    set has_exp [expr {[string first "e" $tstr] != -1}]

    if {$has_decimal || (!$has_decimal && $has_exp)} {
        # Looks like a valid floating point
    } else {
        # Not a valid floating point
        return 0
    }

    # If this number is smaller than the smallest Java
    # double or larger than the largest double, then
    # reject it and let the runtime worry about parsing
    # it from a string.

    if {0} {
    # FIXME: These tests don't work at all. Not clear
    # how we can tell if a floating point number is
    # not going to be a valid Java constant.

    set dmin1 -4.9e-324
    set dmin2 4.9e-324
    if {$tstr < $dmin1} {
        puts "$tstr is smaller than $dmin1"
        return 0
    }
    if {$tstr < $dmin2} {
        puts "$tstr is smaller than $dmin2"
        return 0
    }

    set dmax1 -1.7976931348623157e+308
    set dmax2 1.7976931348623157e+308
    if {$tstr > $dmax1} {
        puts "$tstr is larger than $dmax1"
        return 0
    }
    if {$tstr > $dmax2} {
        puts "$tstr is larger than $dmax2"
        return 0
    }

    }

    return 1
}

# Return a tuple of {MATCHES PARSED PRINTED} that indicates if a
# parsed value exactly matches the string rep of the passed in
# value. The PARSED value is the format of the number used by Tcl.
# The PRINTED value is the preferred output format that a Java
# literal would be printed as. For example, the integer literal
# "100" would exactly match the string rep of "100" so there is
# no reason to save the string rep. The integer literal "0xFF"
# would have a parsed value of "255" so it would not exactly match
# the string rep. In integer cases, the PARSED and PRINTED values
# will be the same. For floating point numbers like "1.0e16", the
# PARSED value would be "1e+016" and the PRINTED value would be "1e16".

proc compileproc_parse_value { value } {
    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_parse_value \"$value\""
    }

    set is_integer [compileproc_string_is_java_integer $value]
    if {!$is_integer} {
        set is_double [compileproc_string_is_java_double $value]
    } else {
        set is_double 0
    }

    if {$debug} {
        puts "is_integer $is_integer"
        puts "is_double $is_double"
    }

    if {!$is_integer && !$is_double} {
        error "value \"$value\" is not a valid Java integer or double"
    }

    set parsed [expr {$value}]
    set matching_strrep [expr {$parsed eq $value}]
    set compare [string compare $parsed $value]
    # Double check results
    if {$matching_strrep && ($compare != 0)} {
        error "matching_strrep is true but compare is $compare"
    }
    if {!$matching_strrep && ($compare == 0)} {
        error "matching_strrep is false but compare is $compare"
    }
    if {$debug} {
        puts "value  is \"$value\""
        puts "parsed is \"$parsed\""
        puts "\$parsed eq \$value is $matching_strrep"
    }

    set printed $parsed

    # Attempt to simplify a double like "1e+016" so that
    # it prints like "1e16".

    if {$is_double} {
        set estr [format %g $value]
        if {$debug} {
            puts "exponent string is \"$estr\""
        }

        if {[regexp {^([0-9|.]+)e([\+|\-][0-9][0-9][0-9])$} \
                $estr whole npart epart]} {
            if {$debug} {
                puts "number part is \"$npart\""
                puts "exponent part is \"$epart\""
            }
            set printed $npart
            append printed "e"

            # Skip + if it appears at front of exponent
            set sign [string index $epart 0]
            if {$sign == "-"} {
                append printed "-"
            }
            # Skip leading zeros
            set d1 [string index $epart 1]
            if {$d1 != 0} {
                append printed $d1
            }
            set d2 [string index $epart 2]
            if {$d2 != 0} {
                append printed $d2
            }
            set d3 [string index $epart 3]
            append printed $d3
        }
    }

    if {$debug} {
        puts "printed is \"$printed\""
    }

    return [list $matching_strrep $parsed $printed]
}

# Return 1 if append command can be inlined.

proc compileproc_can_inline_command_append { key } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_can_inline_command_append $key"
    }

    # The append command accepts 2, 3, or more than 3 arguments.
    # Pass to runtime impl if there are less than 3 arguments.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args < 3} {
        if {$debug} {
            puts "returning false since there are $num_args args"
        }
        return 0
    }

    # 2nd argument to inlined append must be a constant scalar or
    # array variable name, otherwise pass to runtime append command.

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]

    if {$type != "constant"} {
        if {$debug} {
            puts "returning false since argument 1 is non-constant type $type"
        }

        return 0
    }

    return 1
}

# Emit code to implement inlined append command. The
# inlined append command would have already been validated
# to have a constant variable name and 3 or more arguments
# when this method is invoked. The inlined append command
# is a bit more tricky that then other commands because
# it must emit special code when cache variables is
# enabled.

proc compileproc_emit_inline_command_append { key } {
    set buffer ""

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    # Determine constant variable name as a String.

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]
    if {$type != "constant"} {
        error "expected constant variable name"
    }
    set varname [lindex $tuple 1]

    # Emit code to allocate an array of TclObject and
    # assign command argument values to the array
    # before including append specific inline code.

    append buffer [compileproc_emit_objv_assignment \
        $key \
        2 end \
        1 \
        compileproc_emit_append_call_impl \
        $varname \
        ]

    return $buffer
}

# Invoked to emit code for inlined TJC.appendVar() call
# inside try block.

proc compileproc_emit_append_call_impl { key arraysym tmpsymbol userdata } {
    global _compileproc

    if {$_compileproc(options,cache_variables)} {
        set cache_variables 1
    } else {
        set cache_variables 0
    }

    set buffer ""

    # Reset interp result before the append
    # method is invoked. If the variable
    # contains the same TclObject saved
    # in the interp result then it would
    # be considered shared and we want
    # to avoid a pointless duplication of
    # the TclObject.

    append buffer [emitter_reset_result]

    set varname $userdata
    set varname_symbol [emitter_double_quote_tcl_string $userdata]

    if {!$cache_variables} {
        append buffer \
            [emitter_statement \
                "$tmpsymbol = TJC.appendVar(interp, $varname_symbol, $arraysym)"]
    } else {
        # In cache variable mode, inline special code for scalar var

        set vinfo [descend_simple_variable $varname]
        if {[lindex $vinfo 0] == "array"} {
            append buffer \
                [emitter_statement \
                    "$tmpsymbol = TJC.appendVar(interp, $varname_symbol, $arraysym)"]
        } elseif {[lindex $vinfo 0] == "scalar"} {
            set cache_symbol [compileproc_variable_cache_lookup $varname]
            set cacheId [compileproc_get_cache_id_from_symbol $cache_symbol]

            append buffer \
                [emitter_statement \
                    "$tmpsymbol = appendVarScalar(interp, $varname_symbol,\
                        $arraysym, $cache_symbol, $cacheId)"]
        } else {
            error "unexpected result \{$vinfo\} from descend_simple_variable"
        }
    }

    append buffer [emitter_set_result $tmpsymbol false]

    return $buffer
}

# Return 1 if global command can be inlined.

proc compileproc_can_inline_command_global { key } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_can_inline_command_global $key"
    }

    # The global command accepts 1 to N args, If no arguments
    # are given just pass to runtime global command impl to
    # raise error message.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args < 2} {
        if {$debug} {
            puts "returning false since there are $num_args args"
        }
        return 0
    }

    # Each argument variable name must be a constant string
    # otherwise pass the arguments to the runtime global command.

    for {set i 1} {$i < $num_args} {incr i} {
        set tuple [compileproc_get_argument_tuple $key $i]
        set type [lindex $tuple 0]

        if {$type != "constant"} {
            if {$debug} {
                puts "returning false since argument $i is non-constant type $type"
            }
            return 0
        }
    }

    return 1
}

# Emit code to implement inlined global command invocation.

proc compileproc_emit_inline_command_global { key } {
    global _compileproc

    set buffer ""

    # Reset interp result in case result of command is checked.

    append buffer [emitter_reset_result]

    # Emit global function invocation for each argument

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    for {set i 1} {$i < $num_args} {incr i} {
        set tuple [compileproc_get_argument_tuple $key $i]
        set varname [lindex $tuple 1]

        append buffer [emitter_make_global_link_var $varname]
    }

    return $buffer
}

# Return 1 if incr command can be inlined.

proc compileproc_can_inline_command_incr { key } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_can_inline_command_incr $key"
    }

    # The incr command accepts 2 or 3 arguments. If wrong number
    # of args given just pass to runtime incr command impl to
    # raise error message.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args != 2 && $num_args != 3} {
        if {$debug} {
            puts "returning false since there are $num_args args"
        }
        return 0
    }

    # 2nd argument to inlined incr must be a constant scalar or
    # array variable name, otherwise pass to runtime incr command.

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]

    if {$type != "constant"} {
        if {$debug} {
            puts "returning false since argument 1 is non-constant type $type"
        }

        return 0
    }

    # 3rd argument can be a constant or a runtime evaluation result

    return 1
}

# Emit code to implement inlined incr command invocation. The
# inlined incr command would have already been validated
# to have a constant variable name and 2 or 3 arguments
# when this method is invoked. The inlined incr command
# is a bit more tricky that then other commands because
# it must emit special code when cache variables is
# enabled.

proc compileproc_emit_inline_command_incr { key } {
    global _compileproc

    if {$_compileproc(options,cache_variables)} {
        set cache_variables 1
    } else {
        set cache_variables 0
    }

    set buffer ""

    # Determine constant variable name as a String.

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]
    if {$type != "constant"} {
        error "expected constant variable name"
    }
    set varname [lindex $tuple 1]

    # Determine the incr amount, if there are 2
    # arguments then the incr amount is 1,
    # otherwise it could be a constant value
    # or a value to be evaluated at runtime.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args == 2} {
        # incr by one
        set incr_symbol 1
    } elseif {$num_args == 3} {
        # determine incr amount

        set constant_integer_increment 0

        set tuple [compileproc_get_argument_tuple $key 2]
        set type [lindex $tuple 0]
        set value [lindex $tuple 1]

        if {$type == "constant"} {
            set is_integer [compileproc_string_is_java_integer $value]
            if {$is_integer} {
                set tuple [compileproc_parse_value $value]
                set stringrep_matches [lindex $tuple 0]
                set parsed_number [lindex $tuple 1]

                if {$stringrep_matches} {
                    set constant_integer_increment 1
                    set incr_symbol $parsed_number
                }
            }
        }

        if {!$constant_integer_increment} {
            # Evaluate value argument
            set tuple [compileproc_emit_argument $key 2 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]
            append buffer $value_buffer

            set tmpsymbol [compileproc_tmpvar_next]
            append buffer [emitter_statement \
                "int $tmpsymbol = TclInteger.get(interp, $value_symbol)"]

            set incr_symbol $tmpsymbol
        }
    } else {
        error "expected 2 or 3 arguments to incr"
    }

    # Reset the interp result in case it holds a ref
    # to the TclObject inside the variable.

    append buffer [emitter_reset_result]

    # Emit incr statement, incr_symbol is an int value
    # to add to the current value.

    set result_tmpsymbol [compileproc_tmpvar_next]
    set qvarname [emitter_double_quote_tcl_string $varname]

    append buffer [emitter_indent] \
        "TclObject $result_tmpsymbol = "

    if {!$cache_variables} {
        append buffer \
            "TJC.incrVar(interp, $qvarname, $incr_symbol)\;\n"
    } else {
        # In cache variable mode, inline special code for scalar var

        set vinfo [descend_simple_variable $varname]
        if {[lindex $vinfo 0] == "array"} {
            set p1 [lindex $vinfo 1]
            set p2 [lindex $vinfo 2]
            append buffer \
                "TJC.incrVar(interp, $qvarname, $incr_symbol)\;\n"
        } elseif {[lindex $vinfo 0] == "scalar"} {
            set cache_symbol [compileproc_variable_cache_lookup $varname]
            set cacheId [compileproc_get_cache_id_from_symbol $cache_symbol]

            append buffer \
                "incrVarScalar(interp, $qvarname, $incr_symbol, 0, $cache_symbol, $cacheId)\;\n"
        } else {
            error "unexpected result \{$vinfo\} from descend_simple_variable"
        }
    }

    # Set interp result to the returned value.
    # This code always calls setResult(), so there
    # is no need to worry about calling resetResult()
    # before the inlined set impl begins.

    append buffer [emitter_set_result $result_tmpsymbol false]

    return $buffer
}

# Return 1 if lappend command can be inlined.

proc compileproc_can_inline_command_lappend { key } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_can_inline_command_lappend $key"
    }

    # The lappend command accepts 2, 3, or more than 3 arguments.
    # Pass to runtime impl if there are less than 3 arguments.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args < 3} {
        if {$debug} {
            puts "returning false since there are $num_args args"
        }
        return 0
    }

    # 2nd argument to inlined lappend must be a constant scalar or
    # array variable name, otherwise pass to runtime lappend command.

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]

    if {$type != "constant"} {
        if {$debug} {
            puts "returning false since argument 1 is non-constant type $type"
        }

        return 0
    }

    return 1
}

# Emit code to implement inlined lappend command. The
# inlined lappend command would have already been validated
# to have a constant variable name and 3 or more arguments
# when this method is invoked. The inlined lappend command
# is a bit more tricky that then other commands because
# it must emit special code when cache variables is
# enabled.

proc compileproc_emit_inline_command_lappend { key } {
    set buffer ""

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    # Determine constant variable name as a String.

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]
    if {$type != "constant"} {
        error "expected constant variable name"
    }
    set varname [lindex $tuple 1]

    # Emit code to allocate an array of TclObject and
    # assign command argument values to the array
    # before including lappend specific inline code.

    append buffer [compileproc_emit_objv_assignment \
        $key \
        2 end \
        1 \
        compileproc_emit_lappend_call_impl \
        $varname \
        ]

    return $buffer
}

# Invoked to emit code for inlined TJC.lappendVar() call
# inside try block.

proc compileproc_emit_lappend_call_impl { key arraysym tmpsymbol userdata } {
    global _compileproc

    if {$_compileproc(options,cache_variables)} {
        set cache_variables 1
    } else {
        set cache_variables 0
    }

    set buffer ""

    # Reset interp result before the lappend
    # method is invoked. If the variable
    # contains the same TclObject saved
    # in the interp result then it would
    # be considered shared and we want
    # to avoid a pointless duplication of
    # the TclObject.

    append buffer [emitter_reset_result]

    set varname $userdata
    set varname_symbol [emitter_double_quote_tcl_string $userdata]

    if {!$cache_variables} {
        append buffer \
            [emitter_statement \
                "$tmpsymbol = TJC.lappendVar(interp, $varname_symbol, $arraysym)"]
    } else {
        # In cache variable mode, inline special code for scalar var

        set vinfo [descend_simple_variable $varname]
        if {[lindex $vinfo 0] == "array"} {
            append buffer \
                [emitter_statement \
                    "$tmpsymbol = TJC.lappendVar(interp, $varname_symbol, $arraysym)"]
        } elseif {[lindex $vinfo 0] == "scalar"} {
            set cache_symbol [compileproc_variable_cache_lookup $varname]
            set cacheId [compileproc_get_cache_id_from_symbol $cache_symbol]

            append buffer \
                [emitter_statement \
                    "$tmpsymbol = lappendVarScalar(interp, $varname_symbol,\
                        $arraysym, $cache_symbol, $cacheId)"]
        } else {
            error "unexpected result \{$vinfo\} from descend_simple_variable"
        }
    }

    append buffer [emitter_set_result $tmpsymbol false]

    return $buffer
}

# Return 1 if lindex command can be inlined.

proc compileproc_can_inline_command_lindex { key } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_can_inline_command_lindex $key"
    }

    # lindex accepts 2 to N argument, but we only
    # compile a lindex command that has 3 arguments.
    # Pass to runtime impl for other num args.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args != 3} {
        if {$debug} {
            puts "returning false since there are $num_args args"
        }
        return 0
    }

    return 1
}

# Emit code to implement inlined lindex command invocation
# that has 3 arguments.

proc compileproc_emit_inline_command_lindex { key } {
    global _compileproc

    set buffer ""

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    # Other that 3 argument is an error.

    if {$num_args != 3} {
        error "expected 3 arguments"
    }

    # Check for a constant integer literal as the
    # index argument. For example: [lindex $list 0]

    set constant_integer_index 0

    set tuple [compileproc_get_argument_tuple $key 2]
    set type [lindex $tuple 0]
    set value [lindex $tuple 1]

    if {$type == "constant"} {
        set is_integer [compileproc_string_is_java_integer $value]
        if {$is_integer} {
            set tuple [compileproc_parse_value $value]
            set stringrep_matches [lindex $tuple 0]
            set parsed_number [lindex $tuple 1]

            if {$stringrep_matches} {
                set constant_integer_index 1
                set index_symbol $parsed_number
            }
        }
    }

    if {$constant_integer_index} {
        # Emit optimized code when a constant integer
        # argument is passed to lindex. There is no
        # need to preserve and release the list
        # since the index is not evaluated.

        # Evaluate value argument
        set tuple [compileproc_emit_argument $key 1 true {}]
        set value_type [lindex $tuple 0]
        set value_symbol [lindex $tuple 1]
        set value_buffer [lindex $tuple 2]

        if {$value_type == "constant"} {
            set result_symbol [compileproc_tmpvar_next]
            set declare_result_symbol 1
        } else {
            append buffer $value_buffer
            set result_symbol $value_symbol
            set declare_result_symbol 0
        }

        if {$declare_result_symbol} {
            append buffer [emitter_indent] \
                "TclObject $result_symbol = "
        } else {
            append buffer [emitter_indent] \
                "$result_symbol = "
        }

        append buffer \
            "TclList.index(interp, $value_symbol, $index_symbol)\;\n" \
            [emitter_container_if_start "$result_symbol == null"] \
            [emitter_reset_result] \
            [emitter_container_if_else] \
            [emitter_set_result $result_symbol false] \
            [emitter_container_if_end]

        return $buffer
    }

    # Not a constant integer index argument.
    # Evaluate the list and index arguments
    # and invoke TJC.lindexNonconst().

    set list_tmpsymbol [compileproc_tmpvar_next]
    set index_tmpsymbol [compileproc_tmpvar_next]

    append buffer \
        [emitter_statement "TclObject $list_tmpsymbol = null"] \
        [emitter_statement "TclObject $index_tmpsymbol"]

    # open try block

    append buffer [emitter_container_try_start]

    # Evaluate list argument
    set i 1
    set tuple [compileproc_emit_argument $key $i false $list_tmpsymbol]
    set value_type [lindex $tuple 0]
    set value_symbol [lindex $tuple 1]
    set value_buffer [lindex $tuple 2]
    if {$value_type == "constant"} {
        append buffer [emitter_statement \
            "$list_tmpsymbol = $value_symbol"]
    } else {
        append buffer $value_buffer
    }
    append buffer [emitter_tclobject_preserve $list_tmpsymbol]

    # Evaluate index argument
    set i 2
    set tuple [compileproc_emit_argument $key $i false $index_tmpsymbol]
    set value_type [lindex $tuple 0]
    set value_symbol [lindex $tuple 1]
    set value_buffer [lindex $tuple 2]
    if {$value_type == "constant"} {
        append buffer [emitter_statement \
            "$index_tmpsymbol = $value_symbol"]
    } else {
        append buffer $value_buffer
    }

    # Call TJC.lindexNonconst()
    append buffer [emitter_statement \
        "TJC.lindexNonconst(interp, $list_tmpsymbol, $index_tmpsymbol)"]

    # finally block

    append buffer \
        [emitter_container_try_finally] \
        [emitter_container_if_start "$list_tmpsymbol != null"] \
        [emitter_tclobject_release $list_tmpsymbol] \
        [emitter_container_if_end] \
        [emitter_container_try_end]

    return $buffer
}

# Return 1 if list command can be inlined.
# The list command accepts 0 to N arguments
# of any type, so it can always be inlined.

proc compileproc_can_inline_command_list { key } {
    return 1
}

# Emit code to implement inlined list command invocation.
# This code generates a "pure" Tcl list, that is a list
# without a string rep.

proc compileproc_emit_inline_command_list { key } {
    global _compileproc

    set buffer ""

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    #puts "compileproc_emit_inline_command_list $key, num_args is $num_args"

    set tmpsymbol [compileproc_tmpvar_next]

    append buffer [emitter_statement \
        "TclObject $tmpsymbol = TclList.newInstance()"]

    # Set a special flag if all arguments are constant
    # strings.

    set constant_args 1
    for {set i 1} {$i < $num_args} {incr i} {
        set tuple [compileproc_get_argument_tuple $key $i]
        set type [lindex $tuple 0]

        if {$type != "constant"} {
            set constant_args 0
        }
    }

    # Declare a second symbol to hold evaluated list values
    # in the case where 1 or more arguments are non-constant.

    if {$constant_args} {
        set value_tmpsymbol {}
    } else {
        set value_tmpsymbol [compileproc_tmpvar_next]
        append buffer [emitter_statement "TclObject $value_tmpsymbol"]
    }

    if {$num_args == 1} {
        # No-op
    } elseif {$num_args == 2} {
        # Special case of list of length 1 shows up quite
        # a bit so create a special case without the
        # try block. Since only one element is added to
        # the list, there is no need to worry about
        # the preserve() and release() logic.

        append buffer \
            [compileproc_emit_inline_command_list_argument $key 1 \
                $tmpsymbol $value_tmpsymbol]
    } else {
        # Setup try block to release list object in case of an exception
        append buffer [emitter_indent] "try \{\n"
        emitter_indent_level +1

        for {set i 1} {$i < $num_args} {incr i} {
            append buffer \
                [compileproc_emit_inline_command_list_argument $key $i \
                    $tmpsymbol $value_tmpsymbol]
        }

        emitter_indent_level -1
        append buffer [emitter_indent] "\} catch (TclException ex) \{\n"
        emitter_indent_level +1
        append buffer \
            [emitter_tclobject_release $tmpsymbol] \
            [emitter_statement "throw ex"]
        emitter_indent_level -1
        append buffer [emitter_indent] "\}\n"
    }

    # Set interp result to the returned value.
    # This code always calls setResult(), so there
    # is no need to worry about calling resetResult()
    # before the inlined set impl begins.

    append buffer [emitter_set_result $tmpsymbol false]

    return $buffer
}

# Evaluate argument append statement and return buffer 

proc compileproc_emit_inline_command_list_argument { key i listsymbol valuesymbol } {
    set buffer ""

    # Evaluate value argument
    set tuple [compileproc_emit_argument $key $i false $valuesymbol]
    set value_type [lindex $tuple 0]
    set value_symbol [lindex $tuple 1]
    set value_buffer [lindex $tuple 2]

    # check for constant symbol special case, no
    # need to eval anything for a constant. Note
    # that we don't bother to preserve() and
    # release() the TclObject since
    # TclList.append() calls preserve().

    if {$value_type == "constant"} {
        set value $value_symbol
    } else {
        append buffer $value_buffer
        set value $value_symbol
    }

    # FIXME: This generated code invokes append()
    # over and over, an optimized approach that
    # invoked a TJC method optmized to know that
    # the TclObject is already of type list
    # could be even more efficient. A list append
    # is done often, so speed is important here.

    append buffer [emitter_statement \
        "TclList.append(interp, $listsymbol, $value)"]

    return $buffer
}

# Return 1 if llength command can be inlined.

proc compileproc_can_inline_command_llength { key } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_can_inline_command_llength $key"
    }

    # The llength command accepts 1 argument. Pass to runtime
    # llength command impl to raise error message otherwise.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args != 2} {
        if {$debug} {
            puts "returning false since there are $num_args args"
        }
        return 0
    }

    return 1
}

# Emit code to implement inlined llength command invocation.

proc compileproc_emit_inline_command_llength { key } {
    global _compileproc

    set buffer ""

    # Evaluate value argument
    set tuple [compileproc_emit_argument $key 1 true {}]
    set value_type [lindex $tuple 0]
    set value_symbol [lindex $tuple 1]
    set value_buffer [lindex $tuple 2]

    append buffer $value_buffer

    # Get list length and set interp result. We don't
    # need to worry about the ref count here since
    # there is only one value and we are finished
    # with it by the time setResult() is invoked.

    set tmpsymbol [compileproc_tmpvar_next]

    append buffer \
        [emitter_statement \
            "int $tmpsymbol = TclList.getLength(interp, $value_symbol)"] \
        [emitter_set_result $tmpsymbol false]

    return $buffer
}

# Return 1 if set command can be inlined.

proc compileproc_can_inline_command_set { key } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_can_inline_command_set $key"
    }

    # The set command accepts 2 or 3 arguments. If wrong number
    # of args given just pass to runtime set command impl to
    # raise error message.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args != 2 && $num_args != 3} {
        if {$debug} {
            puts "returning false since there are $num_args args"
        }
        return 0
    }

    # 2nd argument to inlined set must be a constant scalar or
    # array variable name, otherwise pass to runtime set command.

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]

    if {$type != "constant"} {
        if {$debug} {
            puts "returning false since argument 1 is non-constant type $type"
        }

        return 0
    }

    # 3rd argument can be a constant or a runtime evaluation result

    return 1
}

# Emit code to implement inlined set command invocation. The
# inlined set command would have already been validated
# to have a constant variable name and 2 or 3 arguments
# when this method is invoked.

proc compileproc_emit_inline_command_set { key } {
    global _compileproc

    set buffer ""

    # Determine constant variable name as a String.

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]
    if {$type != "constant"} {
        error "expected constant variable name"
    }
    set varname [lindex $tuple 1]

    # Determine if this is a get or set operation

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    set tmpsymbol [compileproc_tmpvar_next]

    if {$num_args == 2} {
        # get variable value
        set result [compileproc_get_variable $varname true]
        append buffer [emitter_statement \
            "TclObject $tmpsymbol = $result"]
    } elseif {$num_args == 3} {
        append buffer [emitter_statement \
            "TclObject $tmpsymbol"]

        # Evaluate value argument
        set tuple [compileproc_emit_argument $key 2 false $tmpsymbol]
        set value_type [lindex $tuple 0]
        set value_symbol [lindex $tuple 1]
        set value_buffer [lindex $tuple 2]

        # check for constant symbol special case, no
        # need to eval anything for a constant. Note
        # that we don't bother to preserve() and
        # release() the TclObject since we are only
        # dealing with one TclObject and we are passing
        # it to setVar() which will take care of
        # preserving the value.

        if {$value_type == "constant"} {
            set value $value_symbol
        } else {
            append buffer $value_buffer
            set value $tmpsymbol
        }

        # set variable value, save result in tmp
        set result [compileproc_set_variable $varname true $value 0]
        append buffer [emitter_statement "$tmpsymbol = $result"]
    } else {
        error "expected 2 or 3 arguments to set"
    }

    # Set interp result to the returned value.
    # This code always calls setResult(), so there
    # is no need to worry about calling resetResult()
    # before the inlined set impl begins.

    append buffer [emitter_set_result $tmpsymbol false]

    return $buffer
}

# Return 1 if string command can be inlined.

proc compileproc_can_inline_command_string { key } {
    global _compileproc

    set debug 0
    if {$::_compileproc(debug)} {set debug 1}

    if {$debug} {
        puts "compileproc_can_inline_command_string $key"
    }

    # Some subcommands of the string command can be inlined
    # while others must be processed at runtime. The string
    # command accepts 3 or more arguments.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args < 3} {
        if {$debug} {
            puts "returning false since there are $num_args args"
        }
        return 0
    }

    # 2nd argument to inlined string must be a constant string
    # that indicates the subcommand name.

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]
    set subcmdname [lindex $tuple 1]

    if {$type != "constant"} {
        if {$debug} {
            puts "returning false since argument 1 is non-constant type $type"
        }

        return 0
    }

    # Check for supported string subcommand names. This code
    # will validate the expected number of args for each
    # string subcommand.

    switch -exact -- $subcmdname {
        "compare" -
        "equal" {
            # usage: string compare string1 string2
            # usage: string equal string1 string2

            # FIXME: could add -nocase support

            if {$num_args != 4} {
                if {$debug} {
                    puts "returning false since string $subcmdname\
                        requires 4 arguments, there were $num_args args"
                }
                return 0
            }
        }
        "first" -
        "last" {
            # usage: string first subString string ?startIndex?
            # usage: string last subString string ?lastIndex?

            if {$num_args != 4 && $num_args != 5} {
                if {$debug} {
                    puts "returning false since string $subcmdname\
                        requires 4 or 5 arguments, there were $num_args args"
                }
                return 0
            }
        }
        "index" {
            # usage: string index string charIndex

            if {$num_args != 4} {
                if {$debug} {
                    puts "returning false since string length\
                        requires 4 arguments, there were $num_args args"
                }
                return 0
            }
        }
        "length" {
            # usage: string length string

            if {$num_args != 3} {
                if {$debug} {
                    puts "returning false since string length\
                        requires 3 arguments, there were $num_args args"
                }
                return 0
            }
        }
        "range" {
            # usage: string range string first last

            if {$num_args != 5} {
                if {$debug} {
                    puts "returning false since string range\
                        requires 5 arguments, there were $num_args args"
                }
                return 0
            }
        }
        default {
            if {$debug} {
                puts "returning false since argument 1 is\
                    unsupported subcommand $subcmdname"
            }
            return 0
        }
    }

    return 1
}

# Emit code to implement inlined string command invocation.
# The inlined string command would have already been validated
# at this point.

proc compileproc_emit_inline_command_string { key } {
    set buffer ""

    # Emit code for specific inlined subcommand

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    set tuple [compileproc_get_argument_tuple $key 1]
    set subcmdname [lindex $tuple 1]

    switch -exact -- $subcmdname {
        "compare" -
        "equal" {
            # usage: string compare string1 string2
            # usage: string equal string1 string2

            # Evaluate string1 argument, save in String tmp local
            set tuple [compileproc_emit_argument $key 2 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set string1_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "String $string1_tmpsymbol = $value_symbol.toString()"]

            # Evaluate string2 argument, save in String tmp local
            set tuple [compileproc_emit_argument $key 3 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set string2_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "String $string2_tmpsymbol = $value_symbol.toString()"]

            set result_tmpsymbol [compileproc_tmpvar_next]

            # Emit either "compare" or "equal" operation
            if {$subcmdname == "compare"} {
                append buffer \
                    [emitter_statement \
                        "int $result_tmpsymbol =\
                        $string1_tmpsymbol.compareTo($string2_tmpsymbol)"] \
                    [emitter_statement \
                        "$result_tmpsymbol = (($result_tmpsymbol > 0) ? 1 :\
                        ($result_tmpsymbol < 0) ? -1 : 0)"] \
                    [emitter_set_result $result_tmpsymbol false]
            } elseif {$subcmdname == "equal"} {
                append buffer \
                    [emitter_statement \
                        "boolean $result_tmpsymbol =\
                        $string1_tmpsymbol.equals($string2_tmpsymbol)"] \
                    [emitter_set_result $result_tmpsymbol false]
            } else {
                error "unknown subcommand \"$subcmdname\""
            }
        }
        "first" {
            # usage: string first subString string ?startIndex?

            # The string first command is often used to search for
            # a single character in a string, like so:
            #
            # [string first "\n" $str]
            #
            # Optimize this case by inlining a call to Java's
            # String.indexOf() method when the subString
            # is a single character or a String.

            # Evaluate subString argument
            set tuple [compileproc_get_argument_tuple $key 2]
            set value_type [lindex $tuple 0]

            if {$value_type == "constant"} {
                # Inline constant subString instead of adding
                # it to the constant table.

                set substr_is_constant 1
                set substr_constant_string [lindex $tuple 1]
                set cmap [lindex $tuple 3]

                if {$cmap == {}} {
                    # Grab the first character out of a pattern
                    # that contains no backslash elements.

                    set substr_first [string index $substr_constant_string 0]
                    set substr_len [string length $substr_constant_string]
                } else {
                    # The constant string substr contains backslashes.
                    # Extract from 1 to N characters from the substr_constant_string
                    # that correspond to 1 character from the original

                    set first_num_characters [lindex $cmap 0]
                    set first_end_index [expr {$first_num_characters - 1}]
                    set substr_first [string range $substr_constant_string 0 $first_end_index]

                    # The subString length is length of Tcl string, not the
                    # length of the escaped string.
                    set substr_len [llength $cmap]
                }

                # Emit either a constant String or a constant char decl.
                # When a start index is passed, just declare a String.

                set substr_tmpsymbol [compileproc_tmpvar_next]

                if {$substr_len == 1 && ($num_args == 4)} {
                    set jstr [emitter_backslash_tcl_string \
                        $substr_constant_string]
                    append buffer \
                        [emitter_statement "char $substr_tmpsymbol = '$jstr'"]
                } else {
                    # Invoke TJC method for empty string.
                    if {$substr_len == 0} {
                        set substr_is_constant 0
                    }

                    set jstr [emitter_backslash_tcl_string \
                        $substr_constant_string]
                    append buffer \
                        [emitter_statement "String $substr_tmpsymbol = \"$jstr\""]
                }
            } else {
                # Emit non-constant argument

                set substr_is_constant 0

                set tuple [compileproc_emit_argument $key 2 true {}]
                set value_type [lindex $tuple 0]
                set value_symbol [lindex $tuple 1]
                set value_buffer [lindex $tuple 2]

                set substr_tmpsymbol [compileproc_tmpvar_next]

                append buffer \
                    $value_buffer \
                    [emitter_statement "String $substr_tmpsymbol = $value_symbol.toString()"]
            }

            # Evaluate string argument, save in String tmp local
            set tuple [compileproc_emit_argument $key 3 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set string_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "String $string_tmpsymbol = $value_symbol.toString()"]

            # See if optional startIndex is given, pass TclObject if it was found

            if {$num_args == 5} {
                set tuple [compileproc_emit_argument $key 4 true {}]
                set value_type [lindex $tuple 0]
                set value_symbol [lindex $tuple 1]
                set value_buffer [lindex $tuple 2]

                append buffer \
                    $value_buffer
                set start_symbol $value_symbol
            } else {
                set start_symbol null
            }

            # Inline direct call to String.indexOf(char)
            # or String.indexOf(String) when the substr
            # is a compile time constant and no startIndex
            # is passed.

            set result_tmpsymbol [compileproc_tmpvar_next]

            if {$substr_is_constant && ($num_args == 4)} {
                append buffer \
                    [emitter_statement "int $result_tmpsymbol =\
                        $string_tmpsymbol.indexOf($substr_tmpsymbol)"]
            } else {
                append buffer \
                    [emitter_statement "TclObject $result_tmpsymbol =\
                        TJC.stringFirst(interp, $substr_tmpsymbol,\
                        $string_tmpsymbol, $start_symbol)"]
            }
            append buffer [emitter_set_result $result_tmpsymbol false]
        }
        "index" {
            # usage: string index string charIndex

            # Evaluate string argument, save in String tmp local
            set tuple [compileproc_emit_argument $key 2 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set string_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "String $string_tmpsymbol = $value_symbol.toString()"]

            # Evaluate charIndex argument
            set tuple [compileproc_emit_argument $key 3 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set result_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement \
                    "TclObject $result_tmpsymbol =\
                    TJC.stringIndex(interp, $string_tmpsymbol, $value_symbol)"] \
                [emitter_set_result $result_tmpsymbol false]
        }
        "last" {
            # usage: string last subString string ?lastIndex?

            # string last is basically the same as string first, but
            # this implementation does not try to declare the subString
            # as a Java literal or invoke String.lastIndexOf(). Use
            # of string last is less common than string first.

            # Evaluate subString argument, save in String tmp local
            set tuple [compileproc_get_argument_tuple $key 2]
            set tuple [compileproc_emit_argument $key 2 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set substr_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "String $substr_tmpsymbol = $value_symbol.toString()"]

            # Evaluate string argument, save in String tmp local
            set tuple [compileproc_emit_argument $key 3 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set string_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "String $string_tmpsymbol = $value_symbol.toString()"]

            # See if optional lastIndex is given, pass TclObject if it was found

            if {$num_args == 5} {
                set tuple [compileproc_emit_argument $key 4 true {}]
                set value_type [lindex $tuple 0]
                set value_symbol [lindex $tuple 1]
                set value_buffer [lindex $tuple 2]

                append buffer \
                    $value_buffer
                set last_symbol $value_symbol
            } else {
                set last_symbol null
            }

            # Invoke runtime method and set interp result

            set result_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                [emitter_statement "TclObject $result_tmpsymbol =\
                    TJC.stringLast(interp, $substr_tmpsymbol,\
                    $string_tmpsymbol, $last_symbol)"] \
                [emitter_set_result $result_tmpsymbol false]
        }
        "length" {
            # usage: string length string

            # Evaluate string argument
            set tuple [compileproc_emit_argument $key 2 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set int_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "int $int_tmpsymbol = $value_symbol.toString().length()"] \
                [emitter_set_result $int_tmpsymbol false]
        }
        "range" {
            # usage: string range string first last

            # Evaluate string argument, save in String tmp local
            set tuple [compileproc_emit_argument $key 2 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set string_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "String $string_tmpsymbol = $value_symbol.toString()"]

            # Evaluate first argument
            set tuple [compileproc_emit_argument $key 3 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            if {$value_type == "constant"} {
                set first_tmpsymbol [compileproc_tmpvar_next]
                append buffer \
                    [emitter_statement \
                        "TclObject $first_tmpsymbol = $value_symbol"]
            } else {
                set first_tmpsymbol $value_symbol
                append buffer $value_buffer
            }

            append buffer \
                [emitter_tclobject_preserve $first_tmpsymbol]

            # Evaluate last argument
            set tuple [compileproc_emit_argument $key 4 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            if {$value_type == "constant"} {
                set last_tmpsymbol [compileproc_tmpvar_next]
                append buffer \
                    [emitter_statement \
                        "TclObject $last_tmpsymbol = $value_symbol"]
            } else {
                set last_tmpsymbol $value_symbol
                append buffer $value_buffer
            }

            # Invoke method and set interp result
            set result_tmpsymbol [compileproc_tmpvar_next]
            append buffer \
                [emitter_statement \
                    "TclObject $result_tmpsymbol =\
                        TJC.stringRange(interp, $string_tmpsymbol,\
                            $first_tmpsymbol, $last_tmpsymbol)"] \
                [emitter_set_result $result_tmpsymbol false]
        }
        default {
            # subcommand name should have already been validated
            error "unsupported subcommand $subcmdname"
        }
    }

    return $buffer
}

