if {0} {
RCS: @(#) $Id: rcsver.tcl,v 1.1 1999/05/08 05:43:40 dejong Exp $
}

# This program will extract a RCS id from a file and return the
# id with up to three brach id numbers (two dots in the version num).

if {[llength $argv] > 1} {
  puts stderr "usage : rcsver file"
  exit -1
}


set file [lindex $argv 0]
set fd [open $file r]
set contents [read $fd 1000]
close $fd

if {[regexp -- {RCS: .*,v ([0-9]+(\.[0-9]+)*) [0-9]} $contents whole sub1]} {
  #puts "whole is \"$whole\""
  #puts "sub1 is \"$sub1\""

  # Return max of 3 version numbers
  set nums [split $sub1 .]
  if {[llength $nums] > 3} {
     set nums [lrange $nums 0 2]
     set sub1 [join $nums .]
  }

  puts $sub1

  exit 0
} else {
  exit -1
}
