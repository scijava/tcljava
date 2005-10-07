# Driver script to support running tclbench in Jacl
set debug 0

# Symbolic name of supported interps, passed as first argument to script.
# You should edit these path names so that they point to the interps
# that should be tested.

array set imap {
    tcl C:/msys/home/Mo/install/memdbg_tcltk84/bin/tclsh84g.exe
    jacl132 C:/msys/home/Mo/install/jacl132/bin/jaclsh.bat
    jacl1 C:/msys/home/Mo/install/cvs_jacl1/bin/jaclsh.bat
    jacl2 C:/msys/home/Mo/install/cvs_jacl2/bin/jaclsh.bat
}
set inames [array names imap]


if {[llength $argv] < 2} {
    puts "usage: runbench.tcl NAME TESTFILES"
    puts "usage: NAME is one of $inames"
    puts "usage: TESTFILES is all or a set of .bench file names"
    exit 0
}

set iname [lindex $argv 0]
if {[lsearch -exact $inames $iname] == -1} {
    puts "unknown NAME $iname, must be one of [join $inames {, }]"
    exit 0
}
set interp $imap($iname)
if {$debug} {
    puts "Using interp $interp"
}

if {[llength $argv] == 2 && [lindex $argv 1] == "all"} {
    set files [lsort -dictionary [glob *.bench]]
} else {
    set files [lrange $argv 1 end]
}


set cmd [list $interp libbench.tcl \
    -interp $interp \
    ]

foreach file $files {
  if {$debug} {
  puts "exec $cmd $file"
  }
  if {[catch {eval exec $cmd $file} output]} {
    puts stderr $output
  } else {
    if {$debug} {
        puts "output is \"$output\""
    } else {
        puts $output
    }
  }
}

puts "done"
exit 0

