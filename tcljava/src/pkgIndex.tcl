# Cross platform init script for Tcl Blend. Known to work on unix and windows.
# 
# Author:  Christopher Hylands
# RCS: @(#) $Id: pkgIndex.tcl,v 1.3 1998/11/24 04:25:08 hylands Exp $
#
# Copyright (c) 1997-1998 The Regents of the University of California.
# 	All Rights Reserved.
#
# Permission is hereby granted, without written agreement and without
# license or royalty fees, to use, copy, modify, and distribute this
# software and its documentation for any purpose, provided that the
# above copyright notice and the following two paragraphs appear in all
# copies of this software.
# 
# IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
# FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
# ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
# THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
# SUCH DAMAGE.
# 
# THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
# INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
# PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
# CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
# ENHANCEMENTS, OR MODIFICATIONS.
# 
# 						PT_COPYRIGHT_VERSION_2
# 						COPYRIGHTENDKEY

proc loadtclblend {dir} {
    global tcl_platform env
    switch $tcl_platform(platform) {
        java {
            # This can happend when jacl reads the same tcl lib path, ignore it
            return
        }
        unix {
            set pre_lib "lib"
            set sep ":"
        }
        windows {
            set pre_lib ""
	    # JDK1.2 requires that tclblend.dll either be in the users's path
	    # or that we use an absolute pathname. 
            if [ file exists "$dir/tclblend[info sharedlibextension]"] {
		set pre_lib "$dir/"
            }
            set sep ";"
            # Expand the pathname in case it is something like
            # c:/Progra~1/Tcl/lib/tclblend1.1
            # Without this expansion we have problems loading tclblend.dll
            set dir [file attributes $dir -longname]
            if {"$dir" != [pwd] && \
                    [file exists tclblend[info sharedlibextension]]} {
                puts stderr "Warning: [pwd]/tclblend[info sharedlibextension]\
                        exists.\nUnder Windows, this could cause Tcl to\
                        crash\nif we load\
                        $dir/tclblend[info sharedlibextension]"
            }
        }
        mac {
            set pre_lib "lib"
            set sep "|"
        }
        default {
            error "unsupported platform \"$tcl_platform(platform)\""	
        }
    }


    if {! [info exists env(CLASSPATH)] } {
	set env(CLASSPATH) ""
    }


    set native_lib ${pre_lib}tclblend[info sharedlibextension]

    # Search for the java libs from the $dir directory

    set tclblend_files [list \
	        [file nativename [file join $dir tclblend.jar]] \
	        [file nativename [file join $dir lib tclblend.jar]] \
                [file nativename [file join $dir .. tclblend.jar]]]

    set found 0

    foreach f $tclblend_files {
	if {[file exists $f]} {
	    if {$found} {
		puts "Warning: more than one tclblend.jar file found:"
		puts "'$tclblend_file' and '$f'"
	    } else {
		set found 1
		set tclblend_file $f
	    }
	}
    }

    if {!$found} {
	error "could not find tclblend.jar in directory $dir"
    }



    set tcljava_files [list \
	        [file nativename [file join $dir tcljava.jar]] \
	        [file nativename [file join $dir lib tcljava.jar]] \
                [file nativename [file join $dir .. tcljava.jar]]]

    set found 0

    foreach f $tcljava_files {
	if {[file exists $f]} {
	    if {$found} {
		puts "Warning: more than one tcljava.jar file found:"
		puts "'$tcljava_file' and '$f'"
	    } else {
		set found 1
		set tcljava_file $f
	    }
	}
    }

    if {!$found} {
	error "could not find tcljava.jar in directory $dir"
    }


    foreach f [list $tclblend_file $tcljava_file] {
	# Prepend to classpath so that tclblend.jar and jacl.jar
	# will not crash out when in the same classpath!
	    
	set tmp $env(CLASSPATH)
	set env(CLASSPATH) ${f}${sep}
	append env(CLASSPATH) $tmp
    }


    # Load the tclblend native lib after the .class files are loaded.
    if [catch {load $native_lib} errMsg] {
	switch $tcl_platform(platform) {
        windows {
    	    error "Loading '$native_lib' failed.\n\
	    This can happen if the appropriate Java .dlls are not in your\
	    path.\nUnder JDK1.1, tclblend uses javai.dll, which is usually\n\
	    found in c:\\jdk1.1.6\\bin.\n\
	    Under JDK1.2, tclblend uses jvm.lib, which is usually\n\
	    found in c:\\jdk1.1.2\\jre\\bin\\classic.\n\
	    You may need to add the appropriate directory to your PATH.\n\
	    The error was:\n $errMsg"
	}
	default {
	    error "Loading '$native_lib' failed:\n $errMsg"
	} 
	}
    }

    # The version is also set in
    # win/makefile.vc
    # unix/configure.in
    # src/tcljava/tcl/lang/BlendExtension.java

    package provide java 1.1
}

package ifneeded java 1.1 [list loadtclblend $dir]

