# Cross platform init script for Tcl Blend. Known to work on unix and windows.
# Author:  Christopher Hylands, Mo Dejong
# RCS: @(#) $Id: pkgIndex.tcl,v 1.14 1999/09/24 04:53:38 redman Exp $

variable debug_loadtclblend 0

proc loadtclblend {dir} {
    global tcl_platform env tclblend_init

    # Set to true to get extra debug output
    set ::debug_loadtclblend 0

    # Turn on debug messages if tclblend_init is set to debug
    if { [info exists tclblend_init] && "$tclblend_init" == "debug" } {
	set ::debug_loadtclblend 1
    }

    if {$::debug_loadtclblend} {
	puts ""
	puts "called loadtclblend \"$dir\""
    }

    switch $tcl_platform(platform) {
        java {
            # This can happend when jacl reads the same tcl lib path, ignore it

            if {$::debug_loadtclblend} {
                puts "tclblend's pkgIndex.tcl file read in jacl, ignoring".
            }

            return
        }
        unix {
            set pre_lib lib
	    set post_lib [info sharedlibextension]
            set path_sep :

	    set tclblend_shlib ${pre_lib}tclblend${post_lib}
        }
        windows {
            set pre_lib ""
	    set post_lib [info sharedlibextension]
            set path_sep \;
            # Expand the pathname in case it is something like
            # c:/Progra~1/Tcl/lib/tclblend
            # Without this expansion we have problems loading tclblend.dll

            if {$post_lib != ".dll"} {
                error "the windows shared lib extension is not .dll,\
			 it was \"[info sharedlibextension]\""
            }

            set dir [file attributes $dir -longname]
	    if {$::debug_loadtclblend} {
		if {"$dir" != [pwd] && [file exists [pwd]/tclblend.dll]} {
		    puts "Warning: [pwd]/tclblend.dll exists.\n\
			    Under Windows, this could cause Tcl to\
			    crash\nif we load $dir/tclblend.dll"
		}
	    }

	    # JDK1.2 requires that tclblend.dll either be in the
            # user's path or that we use an absolute pathname.
            # So, if we know the full path name then use it.

            if {[file exists $dir/tclblend.dll]} {
		set tclblend_shlib $dir/tclblend.dll
            } else {
		set tclblend_shlib tclblend.dll
	    }

        }
        mac -
        default {
            error "unsupported platform \"$tcl_platform(platform)\""	
        }
    }


    if {$::debug_loadtclblend} {
	puts "tclblend_shlib is $tclblend_shlib"
    }

    # Search for the java libs from the $dir directory

    set tclblend_files [list \
	        [file nativename [file join $dir tclblend.jar]] \
	        [file nativename [file join $dir lib tclblend.jar]] \
                [file nativename [file join $dir .. tclblend.jar]]]

    set found 0

    foreach f $tclblend_files {
	if {[file exists $f]} {
	    if {$found} {
		if {$::debug_loadtclblend} {
		    puts "Warning: more than one tclblend.jar file found:"
		    puts "'$tclblend_file' and '$f'"
		}
	    } else {
		set found 1
		set tclblend_file $f
	    }
	}
    }

    if {!$found} {
	error "could not find tclblend.jar in directory $dir"
    }

    if {$::debug_loadtclblend} {
	puts "found tclblend.jar at $tclblend_file"
    }


    set tcljava_files [list \
	        [file nativename [file join $dir tcljava.jar]] \
	        [file nativename [file join $dir lib tcljava.jar]] \
                [file nativename [file join $dir .. tcljava.jar]]]

    set found 0

    foreach f $tcljava_files {
	if {[file exists $f]} {
	    if {$found} {
		if {$::debug_loadtclblend} {
		    puts "Warning: more than one tcljava.jar file found:"
		    puts "'$tcljava_file' and '$f'"
		}
	    } else {
		set found 1
		set tcljava_file $f
	    }
	}
    }

    if {!$found} {
	error "could not find tcljava.jar in directory $dir"
    }


    if {$::debug_loadtclblend} {
	puts "found tcljava.jar at $tcljava_file"
    }

    # Now scan the env array looking for an env(CLASSPATH) var
    # with the incorrect case. A user might make an error
    # like setting env(ClassPath) so we tell them what happened.
    
    foreach name [array names env] {
	if {$name == "CLASSPATH"} {
	    continue
	}
	if {[string toupper $name] == "CLASSPATH"} {
	    error "found invalid variable env($name), should be env(CLASSPATH)"
	}
    }

    if {! [info exists env(CLASSPATH)] } {
        if {$::debug_loadtclblend} {
	    puts "setting env(CLASSPATH) to {}"
        }

	set env(CLASSPATH) {}
    }



    # now we need to search on the CLASSPATH to see if tclblend.jar
    # or tcljava.jar are already located on the CLASSPATH. If one
    # of these two files is already on the CLASSPATH then we must not
    # change the CLASSPATH because it should already be correct.
    # this can heppend in two cases. First the user could set the
    # CLASSPATH to use a custom tclblend.jar or tcljava.jar so it should
    # not be overridden. Second, if another interp loads tclblend
    # and then the current interp loads tclblend we will run into
    # a huge bug in Tcl 8.0 which ends up deleting values in the env
    # array. This bug has been fixed in tcl8.1 but not in 8.0.4!


    foreach file [split $env(CLASSPATH) ${path_sep}] {
	if {[file tail $file] == "tclblend.jar"} {
	
	    # If this happened the user would have gotten a confusing
            # error because the tcl.lang.Interp from jacl would get
            # loaded before tclblend's tcl.lang.interp class

	    if {[info exists found_jacl]} {
		error "jacl.jar found on env(CLASSPATH) before tclblend.jar"
	    }
	    
	    if {! [info exists found_tclblend]} {
		set found_tclblend $file
	    } else {
		if {$::debug_loadtclblend} {
		    puts "Warning: multiple tclblend.jar files found on env(CLASSPATH), found at $found_tclblend then $file"
		}
	    }
	}
	
	if {[file tail $file] == "tcljava.jar"} {
	    if {! [info exists found_tcljava]} {
		set found_tcljava $file
	    } else {
		if {$::debug_loadtclblend} {
		    puts "Warning: multiple tcljava.jar files found on env(CLASSPATH), found at $found_tcljava then $file"
		}
	    }
	}
	
	if {[file tail $file] == "jacl.jar"} {
	    if {! [info exists found_jacl]} {
		set found_jacl $file
	    }
	}
	
    }


    if {$::debug_loadtclblend} {
    
	if {[info exists found_jacl]} {
	    puts "found jacl.jar on env(CLASSPATH) at $found_jacl"
	}
	if {[info exists found_tcljava]} {
	    puts "found tcljava.jar on env(CLASSPATH) at $found_tcljava"
	}
	if {[info exists found_tclblend]} {
	    puts "found tclblend.jar on env(CLASSPATH) at $found_tclblend"
	}

	set saved_classpath $env(CLASSPATH)
    }
    
    
    # prepend tclblend.jar to the CLASSPATH if it is not already on
    # the CLASSPATH. If jacl.jar is already on the CLASSPATH then this will
    # correctly load the tclblend.jar files instead of those in jacl.jar

    if {! [info exists found_tclblend]} {
	if {$::debug_loadtclblend} {
	    puts "prepending ${tclblend_file} onto env(CLASSPATH)"
	}

	set tmp $env(CLASSPATH)
	set env(CLASSPATH) ${tclblend_file}${path_sep}
	append env(CLASSPATH) $tmp
    }

    # prepend tcljava.jar to the CLASSPATH if it is not already there.

    if {! [info exists found_tcljava]} {
	if {$::debug_loadtclblend} {
	    puts "prepending ${tcljava_file} onto env(CLASSPATH)"
	}

	set tmp $env(CLASSPATH)
	set env(CLASSPATH) ${tcljava_file}${path_sep}
	append env(CLASSPATH) $tmp
    }


    if {$::debug_loadtclblend} {
	if {$saved_classpath != $env(CLASSPATH)} {
	  puts "before jar prepend env(CLASSPATH) was \"$env(CLASSPATH)\""
	  puts "after  jar prepend env(CLASSPATH) was \"$env(CLASSPATH)\""
        } else {
	  puts "before \"load $tclblend_shlib\", env(CLASSPATH) was \"$env(CLASSPATH)\""
        }
    }

    catch {unset found_jacl}
    catch {unset found_tcljava}
    catch {unset found_tclblend}


    # Define proc that searches for shared libs on the path
    
    proc shlib_search { shlibs envvar searchdirs } {
	global env
	
	if {[llength $shlibs] == 0} {
	    error "no shlib names provided"
	}
	
	# iterate over shlibs to set up the location array
	
	foreach shlib $shlibs {
	    if {$shlib == ""} {
		error "empty shlib name"
	    }
	    set shlibloc($shlib) ""
	}
	
	foreach dir $searchdirs {
	    if {$dir == {}} {
		continue
	    }
	    if {! [file isdirectory $dir]} {
		if {$::debug_loadtclblend} {
		    puts "directory \"$dir\" from $envvar does not exist"
		}
		continue
	    }
	    
	    foreach shlib $shlibs {
		set file [file join $dir $shlib]
		
		if {[file exists $file]} {
		    if {$shlibloc($shlib) == ""} {
			set shlibloc($shlib) $file
		    } else {
			if {$::debug_loadtclblend} {
			    puts "found duplicate $shlib on $envvar at\
				    \"$file\", first was at $shlibloc($shlib)"
			}
		    }
		}
	    }
	}
	
	foreach shlib $shlibs {
	    if {$shlibloc($shlib) == ""} {
		error "could not find $shlib, you may need to add the\
			directory where $shlib lives to your $envvar\
			environmental variable."
	    } else {
		if {$::debug_loadtclblend} {
		    puts "found $shlib on $envvar at \"$shlibloc($shlib)\"."
		}
	    }
	}
    }



    switch $tcl_platform(platform) {
	unix {
	    # on a UNIX box shared libs can be found using the
	    # LD_LIBRARY_PATH environmental variable or they can be
	    # defined a ldconfig config file somewhere. We are only
	    # able to check the LD_LIBRARY_PATH here.
	    
	    set VAR LD_LIBRARY_PATH
	    set shlibdir lib
	    
	    # of course HP does it differently
	    if {$tcl_platform(os) == "HP-UX"} {
		if {! [info exists env($VAR)]} {
                    set VAR SHLIB_PATH
		}
	    }
	    
	}
	windows {
	    # on a Windows box the PATH env var is used to find dlls
	    # look on the PATH and see if we can find tclblend.dll
	    
	    set VAR PATH
	    set shlibdir bin

	    if {$::debug_loadtclblend} {
		puts "JDK 1.1 users should have a directory like\
			C:\\jdk1.1.6\\bin in their PATH."
		puts "JDK 1.2 users should have directories like\
			C:\\jdk1.2\\jre\\bin and\
			C:\\jdk1.2\\jre\\bin\\classic in their PATH."
	    }
	}
	mac -
	default {
	    error "unsupported platform \"$tcl_platform(platform)\""	
	}
    }
    
    set shlibs [list ${pre_lib}tclblend${post_lib}]
    
    if {[info exists env(TCLBLEND_SHLIB_NAMES)]} {
	foreach lib $env(TCLBLEND_SHLIB_NAMES) {
	    lappend shlibs $lib
	}
    } else {
	lappend shlibs ${pre_lib}java${post_lib}
    }


    # Load the tclblend native lib after the .jar files are on the CLASSPATH.
    # If loading of the shared libs fails try to figure out why it failed.

    if {[catch {load $tclblend_shlib} errMsg]} {

	if {$::debug_loadtclblend} {
	    puts "Attempting to figure out why \"load $tclblend_shlib\" failed"
	}

        # Look for the shared libs that tclblend needs. The only
        # tricky part here is that Windows users will not have
        # the TCLBLEND_SHLIB_NAMES set so just check for "java".

	if {$::debug_loadtclblend} {
	    puts "currently $VAR is set to \n\"$env($VAR)\""
	}

        shlib_search $shlibs $VAR [split $env($VAR) $path_sep]
        rename shlib_search {}


        error "\"load $tclblend_shlib\" failed:\n $errMsg"
    }



    # export the java commands out of the java namespace
    namespace eval ::java {
	namespace export bind call cast defineclass event field \
	    getinterp instanceof lock new null prop throw try unlock
    }


    # See src/tcljava/tcl/lang/BlendExtension.java
    # for other places the version info is hardcoded

    package provide java 1.2.5

    # Delete proc from interp, if other interps do a package require
    # they will source this file again anyway

    rename loadtclblend {}
}

package ifneeded java 1.2.5 [list loadtclblend $dir]

