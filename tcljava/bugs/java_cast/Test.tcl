set c [java::new C]

set c_class [java::call Class forName C]
set c_getclass [$c getClass]

if {$c_class != $c_getclass} {
    puts "c_class != c_getclass"
    puts "c_class is [$c_class toString]"
    puts "c_getclass is [$c_getclass toString]"
} else {
    puts "c_class == c_getclass"
}



set i [java::cast I $c]
set i_getclass [$i getClass]

if {$i_getclass != $c_getclass} {
    puts "i_getclass != c_getclass"
    puts "i_getclass is [$i_getclass toString]"
    puts "c_getclass is [$c_getclass toString]"
} else {
    puts "i_getclass == c_getclass"
}