# Utils commands used while testing the JDK module.
# These methods could also be used to test compilation
# of generated Java files.

proc test_jdk_load_config {} {
    global env _tjc

    set debug 0

    # If running tests from the build directory, then load
    # jkd.cfg from there. Otherwise, assume that this is
    # an installed shell and load from the install location.

    if {$debug} {
        set exists [info exists env(TJC_LIBRARY)]
        puts "env(TJC_LIBRARY) exists is $exists"
        if {$exists} {
            puts "env(TJC_LIBRARY) is \"$env(TJC_LIBRARY)\""
        }
        set exists [info exists env(TJC_BUILD_DIR)]
        puts "env(TJC_BUILD_DIR) exists is $exists"
        if {$exists} {
            puts "env(TJC_BUILD_DIR) is \"$env(TJC_BUILD_DIR)\""
        }
    }

    if {[info exists env(TJC_LIBRARY)] && [info exists env(TJC_BUILD_DIR)]} {
        if {![file exists $env(TJC_BUILD_DIR)/jdk.cfg]} {
            puts stderr "expected jdk.cfg in build dir, skipping jdk tests..."
            return
        }
        jdk_config_parse_file $env(TJC_BUILD_DIR)/jdk.cfg
    } else {
        jdk_config_parse_file [file join $_tjc(root) bin jdk.cfg]
    }
}

# Given a class name as the data contained in it, compile
# the class and return OK if it compiled.

proc test_jdk_compile_buffer { cname buffer } {
    jdk_tool_cleanup
    set fname [jdk_tool_javac_save $cname $buffer]
    set classfile [jdk_tool_javac [list $fname]]
    if {[lindex $classfile 0] == "ERROR"} {
        error [lindex $classfile 1]
    }
    return OK
}


