studio::port in inspectClassVar -portname inspectClass

studio::bind inspectClassVar inspectClassProc

proc inspectClassProc {} {
    global inspectClassVar

    if {[catch {
	puts "-------------------------------------------------"
	puts "ClassName:  [java::info class $inspectClassVar]"
	puts "Fields:     [java::info fields $inspectClassVar]"
	puts "Methods:    [java::info methods $inspectClassVar]"
	puts "Properties: [java::info properties $inspectClassVar]"
	puts "Superclass: [java::info superclass $inspectClassVar]"
	puts "-------------------------------------------------"
	puts ""
    } msg]} {
	puts $msg
    }
}
