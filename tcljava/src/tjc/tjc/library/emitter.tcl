#
#  Copyright (c) 2005 Advanced Micro Devices, Inc.
#
#  See the file "license.amd" for information on usage and
#  redistribution of this file, and for a DISCLAIMER OF ALL
#   WARRANTIES.
#
#  RCS: @(#) $Id: emitter.tcl,v 1.6 2006/01/24 03:48:22 mdejong Exp $
#
#

# This module will write Java source code given
# parse info from Tcl source.

set _emitter(indent) "    "
set _emitter(indent_level) 0


# Emit code to handle the case of zero arguments to
# a procedure.

proc emitter_empty_args {} {
    global _emitter
    set buffer ""
    append buffer [emitter_indent]
    append buffer \
    "if (objv.length != 1) \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent]
    append buffer \
    "throw new TclNumArgsException(interp, 1, objv, \"\")\;\n"
    emitter_indent_level -1
    append buffer [emitter_indent]
    append buffer \
    "\}\n"
    return $buffer
}

# Emit code to check for a single number of args.

proc emitter_num_args { num arg_str } {
    global _emitter
    if {![string is integer $num]} {
        error "expected integer but got \"$num\""
    }
    if {$num == 0} {
        error "should use emitter_empty_args for zero args"
    }
    set bs_arg_str [emitter_backslash_tcl_string $arg_str]
    if {![string equal $bs_arg_str $arg_str]} {
        error "arg_str \"$arg_str\" contains backslash subst chars"
    }

    set buffer ""
    append buffer [emitter_indent]
    append buffer \
    "if (objv.length != [expr {$num + 1}]) \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent]
    append buffer \
    "throw new TclNumArgsException(interp, 1, objv, \"$arg_str\")\;\n"
    emitter_indent_level -1
    append buffer [emitter_indent]
    append buffer \
    "\}\n"
    return $buffer
}


# Emit code to check for a number of arguments that
# have default values.

proc emitter_num_default_args { num arg_str } {
    global _emitter
    if {![string is integer $num]} {
        error "expected integer but got \"$num\""
    }
    if {$num == 0} {
        error "should use emitter_empty_args for zero args"
    }
    set bs_arg_str [emitter_backslash_tcl_string $arg_str]
    if {![string equal $bs_arg_str $arg_str]} {
        error "arg_str \"$arg_str\" contains backslash subst chars"
    }

    set buffer ""
    append buffer [emitter_indent]
    append buffer \
    "if (objv.length > [expr {$num + 1}]) \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent]
    append buffer \
    "throw new TclNumArgsException(interp, 1, objv, \"$arg_str\")\;\n"
    emitter_indent_level -1
    append buffer [emitter_indent]
    append buffer \
    "\}\n"
    return $buffer
}

# Emit code to signal an error when less than a
# min number of args is passed. This is used
# for the case of a proc with some number of
# non-default arguments and then args.
# Like: proc p {name args} {}

proc emitter_num_min_args { num arg_str } {
    global _emitter
    if {![string is integer $num]} {
        error "expected integer but got \"$num\""
    }
    if {$num == 0} {
        error "should use emitter_empty_args for zero args"
    }
    set bs_arg_str [emitter_backslash_tcl_string $arg_str]
    if {![string equal $bs_arg_str $arg_str]} {
        error "arg_str \"$arg_str\" contains backslash subst chars"
    }

    set buffer ""
    append buffer [emitter_indent]
    append buffer \
    "if (objv.length < [expr {$num + 1}]) \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent]
    append buffer \
    "throw new TclNumArgsException(interp, 1, objv, \"$arg_str\")\;\n"
    emitter_indent_level -1
    append buffer [emitter_indent]
    append buffer \
    "\}\n"
    return $buffer
}


# Emit code to signal an error when a number
# of arguments outside a valid range is found.
# This is used for a case where some non-default
# and default args appear together in a proc.
# Like: proc p { name {addr 123} } {}

proc emitter_num_range_args { min max arg_str } {
    global _emitter
    if {![string is integer $min]} {
        error "expected integer but got \"$min\""
    }
    if {![string is integer $max]} {
        error "expected integer but got \"$max\""
    }
    set bs_arg_str [emitter_backslash_tcl_string $arg_str]
    if {![string equal $bs_arg_str $arg_str]} {
        error "arg_str \"$arg_str\" contains backslash subst chars"
    }

    set buffer ""
    append buffer [emitter_indent]
    append buffer \
    "if (objv.length < [expr {$min + 1}] || objv.length > [expr {$max + 1}]) \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent]
    append buffer \
    "throw new TclNumArgsException(interp, 1, objv, \"$arg_str\")\;\n"
    emitter_indent_level -1
    append buffer [emitter_indent]
    append buffer \
    "\}\n"
    return $buffer
}



# Query or set the current indent level.

proc emitter_indent_level { {change current} } {
    global _emitter

    if {$change == "current"} {
        # Just return current value
    } elseif {$change == "zero"} {
        set _emitter(indent_level) 0
    } else {
        set newlevel [expr {$_emitter(indent_level) + $change}]
        if {$newlevel < 0} {
            error "error new indent level ($newlevel) would be negative"
        }
        set _emitter(indent_level) $newlevel
    }
    return $_emitter(indent_level)
}

# Return the number of spaces that corresponds to the
# current indent level.

proc emitter_indent {} {
    global _emitter
    return [string repeat $_emitter(indent) $_emitter(indent_level)]
}

# Comment at start of Java file

proc emitter_class_comment { proc_name } {
    if {[emitter_indent_level] != 0} {
        error "expected indent level of zero, got [emitter_indent_level]"
    }
    return "// TJC implementation of procedure ${proc_name}\n"
}

# Given the name of a package that a class will appear in
# emit a Java package statement.

proc emitter_package_name { package } {
    if {$package == "default"} {
        return ""
    } else {
        return "package ${package}\;\n"
    }
}

# Import all identifiers in Tcl/Jacl package.

proc emitter_import_tcl {} {
    return "import tcl.lang.*;\n"
}

# Import all identifiers in a named package

proc emitter_import_package { package } {
    return "import ${package}.*;\n"
}

# Import a specific class in a named package

proc emitter_import_class { identifier } {
    return "import ${identifier};\n"
}

# Emit a single statement followed by a semicolon
# and newline at the current indent level.

proc emitter_statement { code } {
    return "[emitter_indent]${code}\;\n"
}

# Emit a comment containing the given text
# at the current indent level.

proc emitter_comment { text } {
    set buffer ""
    append buffer [emitter_indent] \
        "// $text\n"
    return $buffer
}

proc emitter_tclobject_preserve { tobj } {
    set buffer ""
    append buffer [emitter_indent] \
        "$tobj.preserve()\;\n"
    return $buffer
}

proc emitter_tclobject_release { tobj } {
    set buffer ""
    append buffer [emitter_indent] \
        "$tobj.release()\;\n"
    return $buffer
}

# Assign value in valsym to the given index in arraysym

proc emitter_array_assign { arraysym index valsym } {
    return [emitter_statement "$arraysym\[$index\] = $valsym"]
}

# Start a class declaration.

proc emitter_class_start { class_name } {
    if {[emitter_indent_level] != 0} {
        error "expected indent level of zero, got [emitter_indent_level]"
    }
    set buffer ""
    append buffer \
    "public class $class_name extends TJC.CompiledCommand \{\n"
    return $buffer
}

# End a class declaration

proc emitter_class_end { class_name } {
    if {[emitter_indent_level] != 0} {
        error "expected indent level of zero, got [emitter_indent_level]"
    }
    set buffer ""
    append buffer \
    "\} // end class $class_name\n"
    return $buffer
}

# Start a cmdProc method declaration.

proc emitter_cmd_proc_start {} {
    if {[emitter_indent_level] != 0} {
        error "expected indent level of zero, got [emitter_indent_level]"
    }
    emitter_indent_level +1
    set buffer ""
    append buffer [emitter_indent]
    append buffer \
    "public void cmdProc(\n"
    emitter_indent_level +1
    append buffer [emitter_indent]
    append buffer \
    "Interp interp,\n"
    append buffer [emitter_indent]
    append buffer \
    "TclObject\[\] objv)\n"
    emitter_indent_level +1
    append buffer [emitter_indent]
    append buffer \
    "throws TclException\n"
    emitter_indent_level -1
    emitter_indent_level -1
    append buffer [emitter_indent]
    append buffer \
    "\{\n"
    emitter_indent_level +1
    # Indent level left at 2 at start of method
    if {[emitter_indent_level] != 2} {
        error "expected indent level of 2 at method start, got [emitter_indent_level]"
    }
    return $buffer
}

# End a cmdProc method declaration.

proc emitter_cmd_proc_end {} {
    if {[emitter_indent_level] != 2} {
        error "expected indent level of 2, got [emitter_indent_level]"
    }
    emitter_indent_level -1
    set buffer ""
    append buffer [emitter_indent]
    append buffer \
    "\}\n"
    emitter_indent_level -1
    return $buffer
}

proc emitter_eval_proc_body { body } {
    # Double up any escapes in the body
    regsub -all {\\} $body {\\\\} body
    set bsbody [emitter_backslash_tcl_string $body]

    set buffer ""
    append buffer [emitter_indent]
    append buffer "String body = \"$bsbody\"\;"
    append buffer "\n"
    append buffer [emitter_indent]
    append buffer {TJC.evalProcBody(interp, body);}
    append buffer "\n"
    append buffer [emitter_indent]
    append buffer {return;}
    append buffer "\n"

    return $buffer
}

# Substitute backslashed characters so that a Tcl string
# can be represented as a Java string literal. For example
# a LF character can't be included directly in a Java string,
# so replace it with a \n character. A more complex example
# would be an octal escape like \777, it would be replaced
# by \u00FF in the Java string. This method will also check
# for known escape characters and subst Java escapes for them.
#
# Java language escape spec:
# http://java.sun.com/docs/books/jls/second_edition/html/lexical.doc.html#101089

# FIXME: If a \u sequence appears in the Tcl string but it
# would break something in the Java string (like a newline
# or a " character represented in unicode) then replace
# the unicode char with \n or \".
# Both \n ( \u000a ) and \r ( \u000d ) are invalid in a string.

proc emitter_backslash_tcl_string { tstr } {
    set debug 0
    if {$debug} {
        puts "emitter_backslash_tcl_string \"$tstr\""
    }

    set buffer ""
    set tlen [string length $tstr]
    for {set i 0} {$i < $tlen} {incr i} {
        set c [string index $tstr $i]
        if {$debug} {
            puts "index $i : char is \'$c\'"
        }
        switch -exact -- $c {
            "\a" {
                append buffer "\\u0007"
            }
            "\b" {
                append buffer "\\b"
            }
            "\f" {
                append buffer "\\f"
            }
            "\n" {
                append buffer "\\n"
            }
            "\r" {
                append buffer "\\r"
            }
            "\t" {
                append buffer "\\t"
            }
            "\v" {
                append buffer "\\u000B"
            }
            "\"" {
                append buffer "\\\""
            }
            "\\" {
                # Backslash sequence. Look ahead in the string
                # to figure out if this Tcl backslash sequence
                # is a valid Java backslash sequence.
                # This method assumes that the backslash
                # is a regular form generated by the method
                # emitter_backslash_tcl_elem. Backslashed
                # sequences inside brace quoted works should
                # have been double backslashed so that they
                # would not be considered here.

                if {$debug} {
                    puts "backslash element at index $i"
                }

                if {[string index $tstr [expr {$i + 1}]] == "\\"} {
                    # Double backslash, ignore this escape sequence
                    append buffer "\\\\"
                    incr i
                    continue
                }

                set rest [string range $tstr $i end]

                if {$debug} {
                    puts "backslash element rest is \"$rest\""
                }

                switch -regexp -- $rest {
                    {^\\a} {
                        set supported 0
                        set len 2
                        set subst {\u0007}
                    }
                    {^\\b} -
                    {^\\f} -
                    {^\\n} -
                    {^\\r} -
                    {^\\t} {
                        set supported 1
                        set len 2
                    }
                    {^\\v} {
                        set supported 0
                        set len 2
                        set subst {\u000B}
                    }
                    {^\\x[0-9|A-F][0-9|A-F]} {
                        # Regular hex escape
                        set supported 0
                        set len 4
                        set subst "\\u00[string range $rest 2 3]"
                    }
                    {^\\[0-7][0-7][0-7]} {
                        # Regular octal escape
                        set supported 0
                        set len 4
                        set esc [string range $rest 0 3]
                        if {$debug} {
                            puts "scanning octal escape sequence \"$esc\""
                        }
                        # Convert octal escape to string of length 1
                        set esc [subst -nocommands -novariables $esc]
                        if {[scan $esc %c decimal] == 0} {
                            error "scan for escape character failed, esc was \"$esc\""
                        }
                        if {$debug} {
                            puts "scanned octal escape sequence \"$esc\", got integer $decimal"
                        }
                        set subst [format "\\u%04X" $decimal]
                    }
                    {^\\u[0-9|A-F][0-9|A-F][0-9|A-F][0-9|A-F]} {
                        # Regular unicode escape
                        set supported 1
                        set len 6
                    }
                    default {
                        # Unmatched escape, just append the char
                        append buffer $c
                        continue
                    }
                }
                if {$supported} {
                    set bs [string range $tstr $i [expr {$i + $len - 1}]]
                    if {$debug} {
                        puts "appending supported backslash sequance \"$bs\""
                    }
                    append buffer $bs
                    incr i [expr {[string length $bs] - 1}]
                } else {
                    if {$debug} {
                        puts "appending subst sequance \"$subst\""
                    }
                    append buffer $subst
                    incr i [expr {$len - 1}]
                }
            }
            default {
                append buffer $c
            }
        }

        if {$debug} {
            puts "for loop end: i is $i : tlen is $tlen"
        }
    }
    return $buffer
}

# Given a Tcl backslash sequence return a Tcl
# string that represents the backslashed
# sequence. A valid backslash sequence will
# typically be left as is. A backslash sequence
# that is not known will simply return the
# text without the backslash. This method is
# not directly involved with the emitter but
# its output is used by emitter_backslash_tcl_string
# so it appears in this module.

proc emitter_backslash_tcl_elem { elem } {
    set debug 0

    set elen [string length $elem]
    if {$elen == 0 || $elen == 1} {
        error "elem \"$elem\" is invalid"
    }
    if {[string index $elem 0] != "\\"} {
        error "elem \"$elem\" is invalid"
    }
    incr elen -1
    if {$debug} {
        puts "elem ->$elem<- has $elen chars after the backslash"
    }

    # Don't process octal escape as one character escape
    if {$elen == 1 && [regexp {^\\[0-7]$} $elem]} {
        set is_octal 1
    } else {
        set is_octal 0
    }

    if {$elen == 1 && !$is_octal} {
        switch -exact -- $elem {
            {\a} -
            {\b} -
            {\f} -
            {\n} -
            {\r} -
            {\t} -
            {\v} -
            {\\} {
                return $elem
            }
            "\\\n" {
                # backslash newline becomes a space character.
                return " "
            }
            default {
                # Unrecognized backslash, return the character
                return [string range $elem 1 end]
            }
        }
    } else {
        set first [string index $elem 1]
        set rest [string range $elem 1 end]
        if {$debug} {
            puts "first is \'$first\'"
            puts "rest is \"$rest\""
        }
        switch -exact -- $first {
            "u" {
                # \uXXXX : 1 to 4 hex digits
                set p {[0-9|a-f|A-F]}
                set wp "^u($p|$p$p|$p$p$p|$p$p$p$p)\$"
                if {![regexp $wp $rest]} {
                    error "unicode escape \"$elem\" is not valid, should match \"\\uXXXX\""
                }
                set digits [string range $rest 1 end]
                set digits [string toupper $digits]
                set dlen [string length $digits]
                if {$dlen == 0} {
                    error "unexpected empty digits for elem \"$elem\""
                } else {
                    return [format "\\u%04s" $digits]
                }
            }
            "x" {
                # \xXX : last 2 hex digits
                if {![regexp {^x[0-F]+$} $rest]} {
                    error "hex escape \"$elem\" is not valid, should match \"\\xXX\""
                }
                set digits [string range $rest 1 end]
                set dlen [string length $digits]
                if {$dlen == 0} {
                    error "unexpected empty digits for elem \"$elem\""
                } elseif {$dlen == 1} {
                    return "\\x0[string index $digits 0]"
                } else {
                    return "\\x[string range $digits end-1 end]"
                }
            }
            default {
                # Octal escape sequence is 1 to 3 octal numbers
                set p {[0-7]}
                set wp "^($p|$p$p|$p$p$p)\$"
                if {[regexp $wp $rest]} {
                    set digits [string range $rest 0 end]
                    set dlen [string length $digits]
                    if {$dlen == 0} {
                        error "unexpected empty digits for elem \"$elem\""
                    } else {
                        return [format "\\%03s" $digits]
                    }
                } elseif {[regexp "^\n\[ |\t\]+\$" $rest]} {
                    # backslash newline + spaces or tabs becomes a space character
                    return " "
                } else {
                    error "unknown escape sequence \"\\$elem\""
                }
            }
        }
    }
}

# Push a local variable call frame. The ns argument is
# the identifier for the namespace the CallFrame should
# be defined in (same as the one the command is in).
# This method is invoked at the start of a compiled
# command implementation.

proc emitter_callframe_push { ns } {
    return [emitter_statement \
        "CallFrame callFrame = TJC.pushLocalCallFrame(interp, $ns)"]
}

# Emit try statement at start of method impl block

proc emitter_callframe_try {} {
    set buffer ""
    append buffer [emitter_indent] \
        "try \{\n"
    return $buffer
}

# Close the command implementation, check for TclExceptions
# cases that need to be handled, and finally pop the local
# variable call frame. This code appears at the end of a
# cmdProc declaration to close the try block opened by
# emitter_callframe_try.

proc emitter_callframe_pop { proc_name {clear_varcache 0} } {
    set buffer ""

    append buffer [emitter_indent]
    append buffer "\} catch (TclException te) \{"
    append buffer "\n"
    emitter_indent_level +1
    append buffer [emitter_indent]
    set jstr [emitter_backslash_tcl_string $proc_name]
    append buffer "TJC.checkTclException(interp, te, \"$jstr\")\;"
    append buffer "\n"
    emitter_indent_level -1
    append buffer [emitter_indent]
    append buffer "\} finally \{"
    append buffer "\n"
    emitter_indent_level +1
    if {$clear_varcache} {
        append buffer [emitter_statement "updateVarCache(interp, 0)"]
    }
    append buffer [emitter_statement \
        "TJC.popLocalCallFrame(interp, callFrame)"]
    emitter_indent_level -1
    append buffer [emitter_indent]
    append buffer "\}"
    append buffer "\n"

    return $buffer    
}

# Emit instance variable declarations and assignment
# code inside a method named initConstants(). These
# members are instance variable so that we don't
# need to be concerned about a command being used
# in multiple interps and threads. The initConstants()
# method is invoked once the first time cmdProc()
# in called (via initCmd() in TJC.CompiledCommand).

# The tlist is a list of {NAME TYPE VALUE} tuples.
# NAME is the Java identifier this constant value
# will be assigned to.
# TYPE is (INTEGER, BOOLEAN, DOUBLE, or STRING).
# VALUE is a Tcl string.

# FIXME: Would be nice to have runtime TJC support for
# combining the runtime constants into a large constant
# pool that is shared across all proc implementations.

proc emitter_init_constants { tlist } {
    if {[emitter_indent_level] != 0} {
        error "expected indent level of 0, got [emitter_indent_level]"
    }

    set buffer ""

    emitter_indent_level +1

    # Emit class scoped declarations for each constant

    foreach tuple $tlist {
        set name  [lindex $tuple 0]
        append buffer [emitter_indent]
        # Note: constants can't be made "final" since they are
        # note assigned until cmdProc is first invoked.
        append buffer "TclObject $name\;"
        append buffer "\n"
    }

    append buffer "\n"

    append buffer [emitter_indent]
    append buffer "protected void initConstants(Interp interp) throws TclException \{"
    append buffer "\n"
    emitter_indent_level +1

    foreach tuple $tlist {
        set name  [lindex $tuple 0]
        set type  [lindex $tuple 1]
        set value [lindex $tuple 2]

        append buffer [emitter_indent]
        append buffer "$name = "

        switch -exact -- $type {
            "BOOLEAN" {
                if {$value} {
                    set jval "true"
                } else {
                    set jval "false"
                }
                # Create a TclString instance then convert the
                # internal rep to a boolean type so that original
                # string representation is retained.
                append buffer "TclString.newInstance(\"$jval\")\;"
                append buffer "\n"
                append buffer [emitter_indent]
                # Convert the internal rep of the constant
                # to a boolean type.
                append buffer "TclBoolean.get(interp, $name)\;"
            }
            "DOUBLE" {
                # Double values like "1.0" can be created with
                # a constant double value. Doubles like
                # "1.0e0" have a string rep that is different
                # than the parsed integer value, so these
                # need to be created with a string rep.

                set parsed_double [expr {$value}]
                if {$parsed_double eq $value} {
                    append buffer "TclDouble.newInstance($value)\;"
                } else {
                    set jstr [emitter_backslash_tcl_string $value]
                    append buffer "TclString.newInstance(\"$jstr\")\;"
                    append buffer "\n"
                    append buffer [emitter_indent]
                    append buffer "TclDouble.get(interp, $name)\;"
                }
            }
            "INTEGER" {
                # Integers like "100" can be created with a
                # constant integer value. Integers like
                # "0xFF" have a string rep that is different
                # than the parsed integer value, so these
                # need to be created with a string rep.

                set parsed_int [expr {$value}]
                if {$parsed_int eq $value} {
                    append buffer "TclInteger.newInstance($value)\;"
                } else {
                    set jstr [emitter_backslash_tcl_string $value]
                    append buffer "TclString.newInstance(\"$jstr\")\;"
                    append buffer "\n"
                    append buffer [emitter_indent]
                    append buffer "TclInteger.get(interp, $name)\;"
                }
            }
            "STRING" {
                set jstr [emitter_backslash_tcl_string $value]
                append buffer "TclString.newInstance(\"$jstr\")\;"
            }
            default {
                error "unmatched type \"$type\""
            }
        }
        append buffer "\n"
        append buffer [emitter_indent]
        # We used to call toString() here since it would generate
        # a string rep and that worked around a bug in the code
        # for converting a boolean to an integer internal rep.
        # This is no longer needed and should not be done since
        # we want boolean, integer, and double constant to be
        # "pure" numbers for use with expr.
        append buffer "$name.preserve()\; $name.preserve()\;"
        append buffer "\n"
    }

    emitter_indent_level -1
    append buffer [emitter_indent]
    append buffer "\}"
    append buffer "\n"
    emitter_indent_level -1

    if {[emitter_indent_level] != 0} {
        error "expected exit indent level of 0, got [emitter_indent_level]"
    }

    return $buffer    
}

# Emit code to check that the TclObject constants have been initialized.
# If not, then invoke initConstants() and do other checks via initCmd()

proc emitter_init_cmd_check { {cflags {}} } {
    set buffer ""
    if {$cflags == {}} {
        append buffer [emitter_indent] \
            "if (!initCmd) \{ initCmd(interp)\; \}"
    } else {
        set buffer ""
        append buffer [emitter_indent] \
            "if (!initCmd) \{\n"
        emitter_indent_level +1
        foreach cflag $cflags {
            append buffer [emitter_statement "$cflags = true"]
        }
        append buffer [emitter_statement "initCmd(interp)"]
        emitter_indent_level -1
        append buffer \
            [emitter_indent] "\}"
    }
    append buffer "\n"
    return $buffer    
}

# Open a command invocation block. This defines a scope
# for variables that are only accessable for this
# command invocation. It also adds a level of indentation.

proc emitter_invoke_start { start_cmd_str } {
    set buffer ""
    append buffer [emitter_indent] \
        "\{ // Invoke: ${start_cmd_str}\n"
    emitter_indent_level +1
    return $buffer
}

proc emitter_invoke_end { end_cmd_str } {
    set buffer ""
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\} // End Invoke: ${end_cmd_str}\n"
    return $buffer
}

# Setup a TclObject[] array and invoke a Tcl command
# with the array of arguments.

proc emitter_invoke_command_start { arraysym size } {
    if {$size == "" || ![string is integer $size] || $size <= 0} {
        error "size \"$size\" must be a positive integer"
    }
    set buffer ""
    append buffer [emitter_indent] \
        "TclObject\[\] $arraysym = TJC.grabObjv(interp, $size)\;\n"
    append buffer [emitter_container_try_start]
    return $buffer
}

# Assign the value indicated by valsym to tmpsym, increment
# the refcount, and then assign to the index indicated
# in the arraysym array.

proc emitter_invoke_command_assign { arraysym index tmpsym valsym } {
    set buffer ""
    if {$tmpsym ne $valsym} {
        append buffer [emitter_indent] \
            "$tmpsym = $valsym\;\n"
    }
    append buffer [emitter_tclobject_preserve $tmpsym]
    append buffer [emitter_array_assign $arraysym $index $tmpsym]
    return $buffer
}

# Emit TJC.invoke()
#
# The cmdref argument is a symbol name for
# an already resolved Command ref. If the isglobal
# flag is true and the cmdref is null, then the
# command will be resolved only at the global scope.

proc emitter_invoke_command_call { arraysym cmdref isglobal } {
    set buffer ""
    if {$cmdref == {}} {
        set cmdref null
    }
    if {$isglobal} {
        set gflag TCL.EVAL_GLOBAL
    } else {
        set gflag 0
    }
    append buffer [emitter_statement \
        "TJC.invoke(interp, $cmdref, $arraysym, $gflag)"]
    return $buffer
}

# Close invoke try block and open finally block

proc emitter_invoke_command_finally {} {
    return [emitter_container_try_finally]
}

# invoke releaseObjv() and close finally block

proc emitter_invoke_command_end { arraysym size } {
    set buffer ""
    append buffer [emitter_statement "TJC.releaseObjv(interp, $arraysym, $size)"]
    append buffer [emitter_container_try_end]
    return $buffer
}


proc emitter_container_switch_start { arraysym size } {
    return [emitter_invoke_command_start $arraysym $size]
}

proc emitter_container_switch_assign { arraysym index tmpsym valsym } {
    return [emitter_invoke_command_assign $arraysym $index $tmpsym $valsym]
}

# This method is used to emit code to invoke the switch
# command at runtime to match a string to a pattern.
# The integer return value is assigned to tmpsymbol.

proc emitter_container_switch_invoke { tmpsymbol objv pbIndex size stringsymbol mode } {
    set buffer ""

    switch -exact -- $mode {
        "default" -
        "exact" {
            set modestr "TJC.SWITCH_MODE_EXACT"
        }
        "glob" {
            set modestr "TJC.SWITCH_MODE_GLOB"
        }
        "regexp" {
            set modestr "TJC.SWITCH_MODE_REGEXP"
        }
    }
    append buffer [emitter_indent] \
        "$tmpsymbol = TJC.invokeSwitch(interp, $objv, $pbIndex,\n"
    emitter_indent_level +1
    append buffer [emitter_indent] \
        "$stringsymbol, $modestr)\;\n"
    emitter_indent_level -1
    append buffer [emitter_container_try_finally]
    append buffer [emitter_indent] \
        "TJC.releaseObjv(interp, $objv, $size)\;\n"
    append buffer [emitter_container_try_end]
    return $buffer
}

# Return true if the string is a Java keyword.
# One is not allowed to use a Java keword for
# things like package names, class names, variable
# names and so one.

proc emitter_is_java_keyword { str } {
    switch -exact -- $str {
        abstract -
        boolean -
        break -
        byte -
        case -
        catch -
        char -
        class -
        const -
        continue -
        default -
        double -
        do -
        else -
        extends -
        finally -
        final -
        float -
        for -
        goto -
        if -
        implements -
        import -
        instanceof -
        interface -
        int -
        long -
        native -
        new -
        package -
        private -
        protected -
        public -
        return -
        short -
        static -
        strictfp -
        super -
        switch -
        synchronized -
        this -
        throws -
        throw -
        transient -
        try -
        void -
        volatile -
        while {
            return 1
        }
        default {
            return 0
        }
    }
}

# Close block and reduce indent

proc emitter_container_block_end {} {
    set buffer ""    
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\}\n"
    return $buffer
}

# Emit code that will inline an if container command.
# The expr_symbol argument should be the name of
# the boolean symbol that will appear in the expression.

proc emitter_container_if_start { expr_symbol } {
    set buffer ""
    append buffer [emitter_indent] \
        "if ( $expr_symbol ) \{\n"
    emitter_indent_level +1
    return $buffer
}

proc emitter_container_if_else_if { expr_symbol } {
    set buffer ""
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\} else if ( $expr_symbol ) \{\n"
    emitter_indent_level +1
    return $buffer
}

proc emitter_container_if_else {} {
    set buffer ""
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\} else \{\n"
    emitter_indent_level +1
    return $buffer
}

proc emitter_container_if_end {} {
    return [emitter_container_block_end]
}

proc emitter_container_try_start {} {
    set buffer ""
    append buffer [emitter_indent] \
        "try \{\n"
    emitter_indent_level +1
    return $buffer
}

proc emitter_container_try_catch { exception } {
    set buffer ""
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\} catch ( $exception ) \{\n"
    emitter_indent_level +1
    return $buffer
}

proc emitter_container_try_finally {} {
    set buffer ""
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\} finally \{\n"
    emitter_indent_level +1
    return $buffer
}

proc emitter_container_try_end {} {
    return [emitter_container_block_end]
}

# Emit code to query a boolean value
# from a TclObject without changing
# the internal rep.

proc emitter_tclobject_to_boolean { identifier } {
    #return "TclBoolean.get(interp, $identifier)"
    return "TJC.getBoolean(interp, $identifier)"
}

# Emit for loop start.

proc emitter_container_for_start { init_buffer expr_symbol {incr_buffer ""} } {
    set buffer ""

    set added_indent 0
    set init_str " "
    if {$init_buffer != ""} {
        set init_str " $init_buffer "
    }
    set incr_str " "
    if {$incr_buffer != ""} {
        set incr_str " $incr_buffer "
    }
    append buffer [emitter_indent] \
        "for ($init_str\; $expr_symbol \;$incr_str) \{\n"
    emitter_indent_level +1
    return $buffer
}

# Emit for loop end

proc emitter_container_for_end {} {
    set buffer ""    
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\}\n"
    append buffer [emitter_reset_result]
    return $buffer
}

# Emit expr test at start of for loop.

proc emitter_container_for_expr { tmpsymbol } {
    set buffer ""
    append buffer [emitter_indent] \
        "if ( ! $tmpsymbol ) { break\; }\n"
    return $buffer
}

# Emit a try block that appears around the
# body commands of a loop.

proc emitter_container_for_try_start {} {
    set buffer ""
    append buffer [emitter_indent] \
        "try \{\n"
    return $buffer
}

proc emitter_container_for_try_end { {do_continue 1} } {
    set buffer ""

    append buffer [emitter_indent] \
        "\} catch (TclException ex) \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent] \
        "int type = ex.getCompletionCode()\;\n"
    append buffer [emitter_indent] \
        "if (type == TCL.BREAK) \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent] \
        "break\;\n"
    emitter_indent_level -1
    if {$do_continue} {
    append buffer [emitter_indent] \
        "\} else if (type == TCL.CONTINUE) \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent] \
        "continue\;\n"
    emitter_indent_level -1
    }
    append buffer [emitter_indent] \
        "\} else \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent] \
        "throw ex\;\n"
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\}\n"
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\}\n"

    return $buffer
}

proc emitter_container_for_skip_start { tmpsymbol } {
    set buffer ""
    if {$tmpsymbol == ""} {error "empty tmpsymbol"}

    append buffer [emitter_indent] \
        "if ( $tmpsymbol ) \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent] \
        "$tmpsymbol = false\;\n"
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\} else \{\n"
    emitter_indent_level +1

    return $buffer
}

proc emitter_container_for_skip_end {} {
    set buffer ""

    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\}\n"

    return $buffer
}

# Emit a catch try block open statement.

proc emitter_container_catch_try_start { tmpsymbol } {
    set buffer ""
    append buffer [emitter_indent] \
        "int $tmpsymbol = TCL.OK\;\n"
    append buffer [emitter_indent] \
        "try \{\n"
    return $buffer
}

# Emit a catch try block end.

proc emitter_container_catch_try_end { tmpsymbol } {
    set buffer ""
    append buffer [emitter_indent] \
        "\} catch (TclException ex) \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent] \
        "$tmpsymbol = ex.getCompletionCode()\;\n"
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\}\n"
    return $buffer
}

# Query a variable value.

proc emitter_get_var { p1 is_p1_string p2 is_p2_string iflags } {
    if {$p1 == "" && !$is_p1_string} {
        error "empty string can't be a symbol"
    }
    if {$p2 == "" && !$is_p2_string} {
        error "empty string can't be a symbol"
    }

    if {$is_p1_string} {
        set p1sym "\"[emitter_backslash_tcl_string $p1]\""
    } else {
        set p1sym $p1
    }
    if {$is_p2_string} {
        set p2sym "\"[emitter_backslash_tcl_string $p2]\""
    } else {
        set p2sym $p2
    }
    return "interp.getVar($p1sym, $p2sym, $iflags)"
}

# Query a cache scalar value

proc emitter_get_cache_scalar_var { p1 is_p1_string iflags cache_symbol cache_id } {
    if {$p1 == "" && !$is_p1_string} {
        error "empty string can't be a symbol"
    }
    if {$is_p1_string} {
        set p1sym "\"[emitter_backslash_tcl_string $p1]\""
    } else {
        set p1sym $p1
    }
    return "getVarScalar(interp, $p1sym, $iflags, $cache_symbol, $cache_id)"
}

# Set a variable value.

proc emitter_set_var { p1 is_p1_string p2 is_p2_string value iflags } {
    if {$is_p1_string} {
        set p1 "\"[emitter_backslash_tcl_string $p1]\""
    }
    if {$is_p2_string} {
        set p2 "\"[emitter_backslash_tcl_string $p2]\""
    }
    if {[string index $value 0] == "\n" ||
            [string index $value 0] == " "} {
        # No-op
    } elseif {$value == ""} {
        set value " \"\""
    } else {
        set value " $value"
    }
    return "interp.setVar($p1, $p2,$value, $iflags)"
}

# Set a cache scalar value

proc emitter_set_cache_scalar_var { p1 is_p1_string valsym iflags cache_symbol cache_id } {
    if {$p1 == "" && !$is_p1_string} {
        error "empty string can't be a symbol"
    }
    if {$is_p1_string} {
        set p1sym "\"[emitter_backslash_tcl_string $p1]\""
    } else {
        set p1sym $p1
    }
    return "setVarScalar(interp, $p1sym, $valsym, $iflags, $cache_symbol, $cache_id)"
}

# Emit interp.resetResult() void statement.

proc emitter_reset_result {} {
    return [emitter_statement "interp.resetResult()"]
}

# Emit interp.setResult(...) void statement.

proc emitter_set_result { value value_is_string } {
    if {$value_is_string} {
        set jstr [emitter_backslash_tcl_string $value]
        set value "\"$jstr\""
    }
    set buffer ""
    append buffer [emitter_indent] \
        "interp.setResult($value)\;\n"
    return $buffer
}

# Emit code that will declare a variable that contains
# the length of the list identified by list_symbol.

proc emitter_container_foreach_list_length { list_symbol } {
    set buffer ""
    append buffer [emitter_indent] \
        "final int ${list_symbol}_length =\
            TclList.getLength(interp, $list_symbol)\;\n"
    return $buffer
}

proc emitter_container_foreach_list_preserve { list_symbol } {
    return [emitter_tclobject_preserve $list_symbol]
}

proc emitter_container_foreach_list_release { list_symbol } {
    set buffer ""

    append buffer [emitter_container_if_start "$list_symbol != null"]
    append buffer [emitter_tclobject_release $list_symbol]
    append buffer [emitter_container_if_end]

    return $buffer
}

proc emitter_container_foreach_try_finally_start {} {
    set buffer ""
    append buffer [emitter_indent] \
        "try \{\n"
    emitter_indent_level +1
    return $buffer
}

proc emitter_container_foreach_try_finally {} {
    set buffer ""
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\} finally \{\n"
    emitter_indent_level +1
    return $buffer
}

proc emitter_container_foreach_try_finally_end {} {
    set buffer ""
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\}\n"
    return $buffer
}

proc emitter_container_foreach_var_try_start {} {
    set buffer ""
    append buffer [emitter_indent] \
        "try \{\n"
    emitter_indent_level +1
    return $buffer
}

proc emitter_container_foreach_var_try_end { varname } {
    set buffer ""
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\} catch (TclException ex) \{\n"
    emitter_indent_level +1
    set jstr [emitter_backslash_tcl_string $varname]
    append buffer [emitter_indent] \
        "TJC.foreachVarErr(interp, \"$jstr\")\;\n"
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\}\n"

    return $buffer
}

# Emit a throw statement to keep the compiler from
# raising an error because a TclException can't
# be thrown inside a loop's try block. This throw
# is inside an if ( false ) so it will generate
# no actual code in the class file.

proc emitter_container_fake_tclexception {} {
    set buffer ""
    append buffer [emitter_indent] \
        "if ( false ) \{ throw (TclException) null\; \}\n"
    return $buffer
}

# Emit a break or continue statement inside a loop.
# The if true block is needed so that an unreachable
# statement error is not generated when a command
# appears after a break or continue in a loop.

proc emitter_container_loop_break_continue { statement } {
    set buffer ""
    append buffer [emitter_indent] \
        "if ( true ) \{ $statement\; \}\n"
    return $buffer
}

# Emit code for a return command with no arguments.

proc emitter_control_return {} {
    set buffer ""
    append buffer [emitter_indent] \
        "if ( true ) \{ return\; \}\n"
    return $buffer
}

# Emit code for a return command with a string argument.

proc emitter_control_return_argument { symbol } {
    set buffer ""
    append buffer [emitter_reset_result]
    append buffer [emitter_indent] \
        "interp.setResult( $symbol )\;\n"
    append buffer [emitter_control_return]
    return $buffer
}

# Emit TJC.makeGlobalLinkVar() statement.
# Pass a fully qualified variable name that
# would be passed as an argument to the
# global command.

proc emitter_make_global_link_var { varname } {
    set tail [namespace tail $varname]

    set jstr1 "\"[emitter_backslash_tcl_string $varname]\""
    set jstr2 "\"[emitter_backslash_tcl_string $tail]\""

    return [emitter_statement \
        "TJC.makeGlobalLinkVar(interp, $jstr1, $jstr2)"]
}

# Quote a Tcl string so that it appears as a valid Java
# string. This means it is backslashed and surrounded
# by double quotes.

proc emitter_double_quote_tcl_string { tstr } {
    return "\"[emitter_backslash_tcl_string $tstr]\""
}

