studio::port in choiceInput
studio::port out choiceValue -transfer stringToBasic -location east

studio::bind choiceInput {
    global choiceInput
    global choiceValue
    
    set value [lindex [$choiceInput toString] end]

    if { [catch {set choiceValue $value} msg } {
        puts $msg
    }
}
