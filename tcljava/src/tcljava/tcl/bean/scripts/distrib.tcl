# distrib.tcl --
#
# This script allows you to create a distrib bean that works just like
# the Distrib bean bundled with Java Studio.
#
# RCS: @(#) $Id: distrib.tcl,v 1.1 1998/10/14 21:09:17 cvsadmin Exp $
#
# Copyright (c) 1998 Sun Microsystems, Inc.
#
# See the file "license.terms" for information on usage and redistribution
# of this file, and for a DISCLAIMER OF ALL WARRANTIES.


#
# This script allows you to create a simple merge & dirstribute bean.
# In the variables below set the names of the input pins and the names
# of the output pins.  Any data on an input pin will be sent to every
# output pin.  If there is only one output pin this acts like the Merger
# bean.  If there is only one input pin then this is just like the 
# Distributer bean.
#

puts 1

set input_pins {input}
set output_pins {output1 output2 foo bar cat}

# You shouldn't need to change anything below this line
puts 2

foreach x $input_pins {
    studio::port in $x -transfer dynamic
    studio::bind $x "distribData $x"
}
puts 3

foreach x $output_pins {
    studio::port out $x -transfer dynamic
}
puts 4

proc distribData {name} {
    global output_pins
    upvar #0 $name input

    foreach x $output_pins {
        upvar #0 $x output
        set output $input
    }
}
