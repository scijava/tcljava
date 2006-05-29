if {[llength $argv] != 2} {
    puts stderr "compare OUTFILE1 OUTFILE2"
}

# Return list of {NAME NUMBER}, for each
# TIMING result found in the outfile.

proc read_timings { filename } {
    set fd [open $filename r]
    set data [read -nonewline $fd]
    set lines [split $data "\n"]

    set timings [list]
    foreach line $lines {
        if {[string match *TIMING* $line]} {
            lappend timings [lrange [split $line " "] 1 end]
        }
    }
    return $timings  
}

set f1 [lindex $argv 0]
set f2 [lindex $argv 1]

set t1 [read_timings $f1]
set t2 [read_timings $f2]

# Make sure each test id matches, then write
# out a text file separated by spaces.

set output "TEST,[file tail $f1],[file tail $f2]\n"

foreach e1 $t1 e2 $t2 {
    set name1 [lindex $e1 0]
    set name2 [lindex $e2 0]
    if {![string equal $name1 $name2]} {
        error "test names \"$name1\" and \"$name2\" don't match"
    }

    set time1 [lindex $e1 1]
    set time2 [lindex $e2 1]
    append output "$name1,$time1,$time2\n"
}

set outfile compare.csv

set fd [open $outfile w]
puts $fd $output
close $fd

puts -nonewline "wrote $outfile"
exit 0

