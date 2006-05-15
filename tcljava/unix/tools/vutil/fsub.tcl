proc file.regsub { file pat subspec } {
    set lines {}
    set changed 0
    puts $file
    set fd [open $file r]
    while {! [eof $fd] } {
	if {[gets $fd line] == -1} {break}

	if { [regsub -all -- $pat $line $subspec line2] } {	    
	    puts "-$line"
	    puts "+$line2"
            set changed 1
	} 
	lappend lines $line2
    }
    close $fd

    if {$changed} {
        set fd [open $file w]
        fconfigure $fd -translation lf
        foreach line $lines {
            puts $fd $line
        }
        close $fd
    }
}

if {[llength $argv] < 3} {
	puts stderr "usage : fsub pat spec file file ..."
	exit 1
}

set pat [lindex $argv 0]
set rep [lindex $argv 1]
set argv [lrange $argv 2 end]

if {0} {
puts "pat = \"$pat\""
puts "rep = \"$rep\""
puts "argv = \"$argv\""
}

# subst a tab for \t in the pattern
set pat [subst -nocommands -novariables $pat]


foreach file $argv {
  if {! [file exists $file] || [file isdirectory $file]} {
      puts stderr "bad file $file"
      exit -1
  }

  file.regsub $file $pat $rep
}
