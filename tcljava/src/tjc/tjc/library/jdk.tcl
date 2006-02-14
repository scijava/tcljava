#
#  Copyright (c) 2005 Advanced Micro Devices, Inc.
#
#  See the file "license.amd" for information on usage and
#  redistribution of this file, and for a DISCLAIMER OF ALL
#   WARRANTIES.
#
#  RCS: @(#) $Id: jdk.tcl,v 1.2 2006/02/14 04:13:27 mdejong Exp $
#
#

# Process list of command line argments and assign
# values to the _cmdline array.

proc jdk_config_parse { buffer } {
    global _jdk_config

    set debug 0

    set lines [split $buffer \n]
    set parsed 0
    set linenum 1

    foreach line $lines {
        if {$debug} {
            puts "processing line \"$line\""
        }

        # Skip empty lines
        if {[string trim $line] == ""} {
            incr linenum
            continue
        }
        # Comment line if # is first character
        if {[string index $line 0] == "#"} {
            incr linenum
            continue
        }
        # Variable pattern
        set pat {^([A-Z]+) *= *(.*)$}
        if {[regexp $pat $line whole sub1 sub2]} {
            set varname $sub1
            set varvalue $sub2
            if {![jdk_config_isvar $varname]} {
                error "Invalid config spec at\
                    line $linenum: $varname is not a valid variable name"
            }
            incr parsed
            # If value is double quoted, remove the quotes.
            if {[string index $varvalue 0] == "\"" &&
                [string index $varvalue end] == "\""} {
                set varvalue [string range $varvalue 1 end-1]
            }
            set _jdk_config($varname) $varvalue
        } else {
            error "Invalid config spec at line $linenum"
        }
        incr linenum
    }

    return $parsed
}

# Return 1 if this variable is a valid jdk config var

proc jdk_config_isvar { name } {
    switch -exact $name {
        "JAVAC" -
        "JAR" -
        "CLASSPATH" {
            return 1
        }
        default {
            return 0
        }
    }
}

# Return current value of config variable. If the variable
# is not set the empty string will be returned.

proc jdk_config_var { name } {
    global _jdk_config

    if {![jdk_config_isvar $name]} {
        error "var \"$name\" is not a jdk config variable name"
    }

    if {![info exists _jdk_config($name)]} {
        return ""
    } else {
        return $_jdk_config($name)
    }
}

proc jdk_config_parse_file { filename } {
    set buffer [tjc_util_file_read $filename]

    if {[catch {
    set parsed [jdk_config_parse $buffer]
    } err]} {
        return [list 0 $err]
    }

    set valid 1
    set reason ""

    set javac [jdk_config_var JAVAC]
    set jar [jdk_config_var JAR]
    set classpath [jdk_config_var CLASSPATH]

    set javac_exists 0
    if {[file exists $javac]} {
        set javac_exists 1
    } else {
        set ext [file extension $javac]
        if {$ext == "" && [file exists ${javac}.exe]} {
            set javac_exists 1
        }
    }
    set jar_exists 0
    if {[file exists $jar]} {
        set jar_exists 1
    } else {
        set ext [file extension $jar]
        if {$ext == "" && [file exists ${jar}.exe]} {
            set jar_exists 1
        }
    }

    if {$javac == ""} {
        set reason "JAVAC not found or is empty"
        set valid 0
    } elseif {!$javac_exists} {
        set reason "JAVAC path is not valid"
        set valid 0
    } elseif {$jar == ""} {
        set reason "JAR not found or is empty"
        set valid 0
    } elseif {!$jar_exists} {
        set reason "JAR path is not valid"
        set valid 0
    } elseif {$classpath == ""} {
        set reason "CLASSPATH not found or is not valid"
        set valid 0
    }

    if {$valid} {
        # Check that tcljava.jar, jacl.jar, and tjc.jar appear on CLASSPATH
        # and that they all live in the same directory.
        set found_tcljava 0
        set found_jacl 0
        set found_tjc 0
        set libdir ""
        set reason ""

        foreach path [split $classpath \;] {
            if {$path == {}} {
                continue
            }
            if {[file tail $path] == "tcljava.jar"} {
                set dir [file dirname $path]
                if {$libdir == ""} {
                    set libdir $dir
                } else {
                    if {$libdir != $dir} {
                        set reason "Jars tcljava.jar, jacl.jar, and tjc.jar must exist in same dir"
                    }
                }
                if {$reason == ""} {
                    set found_tcljava 1
                }
            }
            if {[file tail $path] == "jacl.jar"} {
                set dir [file dirname $path]
                if {$libdir == ""} {
                    set libdir $dir
                } else {
                    if {$libdir != $dir} {
                        set reason "Jars tcljava.jar, jacl.jar, and tjc.jar must exist in same dir"
                    }
                }
                if {$reason == ""} {
                    set found_jacl 1
                }
            }
            if {[file tail $path] == "tjc.jar"} {
                set dir [file dirname $path]
                if {$libdir == ""} {
                    set libdir $dir
                } else {
                    if {$libdir != $dir} {
                        set reason "Jars tcljava.jar, jacl.jar, and tjc.jar must exist in same dir"
                    }
                }
                if {$reason == ""} {
                    set found_tjc 1
                }
            }
        }
        if {!$found_tcljava || !$found_jacl || !$found_tjc} {
            if {$reason == ""} {
                set reason "CLASSPATH is not valid: tcljava.jar, jacl.jar, and tjc.jar must appear"
            }
            set valid 0
        }
    }

    # Set CLASSPATH now that it is validated
    set ::env(CLASSPATH) $classpath

    return [list $valid $reason]
}


# Invoke the JAVAC compiler with 1 to N Java filenames
# as the arguments. The filenames is a list of arguments
# or file name patterns that would be expanded by javac.

proc jdk_tool_javac { filenames } {
    global _tjc

    set debug 0
    if {[info exists _tjc(debug)] && $_tjc(debug)} {
        set debug 1
    }

    set TJC [file join [pwd] [jdk_tjc_rootdir]]

    set TJC_source [file join $TJC source]
    set TJC_build [file join $TJC build]

    if {[llength $filenames] == 0} {
        error "empty filenames argument"
    }

    set javac [jdk_config_var JAVAC]
    if {$javac == ""} {
        error "JAVAC not defined"
    }
    # Optional compiler configuration
    if {[info exists _tjc(compiler)] && $_tjc(compiler) != "javac"} {
        set compiler $_tjc(compiler)
        # UGH! Really should add a JAVA
        # config element here.
        #set java [jdk_config_var JAVA]
        set jar [jdk_config_var JAR]
        set dir [file dir $jar]
        set java [file join $dir java]

        set classpath $::env(CLASSPATH)
        if {$::tcl_platform(host_platform) == "windows"} {
            set sep \;
        } else {
            set sep :
        }

        if {$compiler == "pizza"} {
            append classpath $sep $_tjc(jardir)/pizza-1.1.jar

            set javac [list $java \
                -classpath $classpath \
                net.sf.pizzacompiler.compiler.Main \
                ]
        } elseif {$compiler == "janino"} {
            append classpath $sep $_tjc(jardir)/janino.jar

            set javac [list $java \
                -classpath $classpath \
                org.codehaus.janino.Compiler \
                -classpath $classpath \
                ]
        } else {
            error "unsupported compiler \"$compiler\""
        }
    }

    if {![file exists $TJC_build]} {
        file mkdir $TJC_build
    }

    cd $TJC

    # Passing a whole lot of filename arguments on the command
    # line could eventually overflow the OS command line length.
    # This method is typically used to compile a bunch of Java
    # files in the same package, so check for a common directory
    # and pass a *.java filename pattern in that case. It is
    # really unlikely that an extra file not named on the command
    # line would get slipped into the compile this way.

    if {[llength $filenames] > 100} {
        set first [lindex $filenames 0]
        set dirname [file dirname $first]
        set all_in_same_package 1
        foreach filename $filenames {
            set d [file dirname $filename]
            if {$d ne $dirname} {
                set all_in_same_package 0
            }
        }
        if {$all_in_same_package} {
            set javac_filenames [file join $dirname *.java]
        } else {
            set javac_filenames $filenames
        }
    } else {
        set javac_filenames $filenames
    }

    set caught 0
    set javac_flags -g

    if {$debug} {
        puts "JAVAC exec: $javac $javac_flags -d $TJC_build $javac_filenames"
    }

    if {[catch {eval exec $javac {$javac_flags -d $TJC_build} $javac_filenames} err]} {
        puts stderr $err
        set caught 1
    }

    cd ..

    if {$caught} {
        return [list ERROR $err]
    } else {
        # If filenames is a single filename, then check for
        # a compiled .class file with that name.
        # If filenames is a pattern or more than one file,
        # then just return "OK".

        if {[llength $filenames] == 1 && [string first "*" [lindex $filenames 0]] == -1} {
            set filename [lindex $filenames 0]
            set classfile [jdk_tool_javac_classfile $filename]
            if {$debug} {
                puts "checking for class file \"$classfile\""
            }
            if {[file exists $classfile]} {
                return $classfile
            } else {
                error "compiled class file \"$classfile\" not found"
            }
        } else {
            return OK
        }
    }
}


# Convert the source .java file name into a .class
# file name where the compiled file would have been
# placed. This method assumes that the .java and
# .class file would be inside the TJC dir.

proc jdk_tool_javac_classfile { sourcefile } {
    set names [file split $sourcefile]
    set ind [lsearch -exact $names [jdk_tjc_rootdir]]
    if {$ind == -1} {
        error "TJC dir not found in \{$names\}"
    }
    set src_names [lrange $names [expr {$ind + 2}] end]
    set src_file [lindex $names end]
    set src_ext [file extension $src_file]
    if {$src_ext != ".java"} {
        error "expected .java file, got \"$src_file\""
    }
    if {[llength $src_names] == 2 && [lindex $src_names 0] == "default"} {
        set default 1
        set src_name [lindex $src_names 1]
        set class_name [string map {.java .class} $src_name]
        set class_file $class_name
    } else {
        set default 0
        set src_name [lindex $names end]
        set class_name [string map {.java .class} $src_name]
        set class_file [eval file join [lrange $src_names 0 end-1] $class_name]
    }
    return [eval {file join} [lrange $names 0 $ind] {build $class_file}]
}


# Save cdata in a .java file based on the fully
# qualified class name. If the class is in the
# default package then no '.' characters will
# appear in the class name.

proc jdk_tool_javac_save { cname cdata } {
    set debug 0

    set TJC [file join [pwd] [jdk_tjc_rootdir]]

    if {[string first "." $cname] == -1} {
        set pkg_dir [file join $TJC source default]
        set fname [file join $pkg_dir $cname.java]
    } else {
        set names [split $cname .]
        set pkg_dir [eval {file join $TJC source} [lrange $names 0 end-1]]
        set fname [file join $pkg_dir [lindex $names end].java]
    }
    if {$debug} {
        puts "pkg_dir is \"$pkg_dir\""
        puts "fname is \"$fname\""
    }

    # Create directory if it does not already exist

    if {![file exists $pkg_dir]} {
        file mkdir $pkg_dir
    } elseif {[file exists $pkg_dir] && ![file isdirectory $pkg_dir]} {
        error "mkdir \"$pkg_dir\", file exists with that name"
    }

    # Save the class data as a Java file.
    tjc_util_file_saveas $fname $cdata

    return $fname
}

# Create a jar file with the given name that contains
# all of the files in the TJC/build subdirectory.
# If the symbol "SOURCE" is passed in the loc argument,
# the the contents of the source directory will be
# added to the jar instead.

proc jdk_tool_jar { jarname {location BUILD} } {
    set debug 0

    set TJC [file join [pwd] [jdk_tjc_rootdir]]
    set TJC_BUILD [file join $TJC build]
    set TJC_SOURCE [file join $TJC source]
    set TJC_JAR [file join $TJC jar]

    set jar [jdk_config_var JAR]
    if {$jar == ""} {
        error "JAR not defined"
    }

    file mkdir $TJC_JAR

    if {$location == "BUILD"} {
        cd $TJC_BUILD
    } else {
        cd $TJC_SOURCE
    }

    # Get names of each toplevel directory and pass em to jar
    set rdirs [glob *]

    if {$debug} {
        puts "JAR exec: $jar -cf $TJC_JAR/$jarname $rdirs"
    }

    set caught 0

    if {[catch {eval {exec $jar -cf $TJC_JAR/$jarname} $rdirs} err]} {
        puts stderr $err
        set caught 1
    }

    cd ../..

    if {$caught} {
        return [list ERROR $err]
    } else {
        # Check that jar file was created, and return name.
        set jarfile [file join $TJC_JAR/$jarname]

        if {[file exists $jarfile]} {
            return $jarfile
        } else {
            return [ERROR "jar file TJC_JAR/$jarname not found"]
        }
    }
}

# Cleanup temp files created while using JDK tools

proc jdk_tool_cleanup {} {
    file delete -force [jdk_tjc_rootdir]
}

# Given a Java package, return the path name
# to the associated library directory. The
# library directory is where Tcl files
# would be found.

proc jdk_package_library { pkg in_srcjar } {
    if {$pkg == "default"} {
        if {$in_srcjar} {
            return "default/library"
        } else {
            return "library"
        }
    } else {
        set elems [split $pkg .]
        return [eval {file join} $elems library]
    }
}

# Return the directory name used for the toplevel
# directory that will hold generated Java sources
# and class files.

proc jdk_tjc_rootdir {} {
    return "bldTJC"
}

