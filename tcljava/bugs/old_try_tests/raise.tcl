set env(TCL_CLASSPATH) .

for {set i 0} {$i < 3} {incr i} {

java::try {
  java::call Raise foo [java::getinterp]
} catch {TclException err} {
  puts "Caught TclException"
  puts $err
} catch {Exception err} {
  puts "Caught Exception"
  puts $err
}

}
