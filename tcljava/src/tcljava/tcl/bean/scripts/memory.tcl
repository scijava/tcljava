# memory.tcl --
#
# This script allows you to create a memory bean that works just like
# the Memory bean bundled with Java Studio.
#
# RCS: @(#) $Id: memory.tcl,v 1.1 1998/10/14 21:09:17 cvsadmin Exp $
#
# Copyright (c) 1998 Sun Microsystems, Inc.
#
# See the file "license.terms" for information on usage and redistribution
# of this file, and for a DISCLAIMER OF ALL WARRANTIES.

# The following variable holds a list of names that will be used as
# memory locations.  You can modifiy the list to include as many memory
# locations as you desire.  A location  with a name like "foo" will create
# two pins "foo input" and "foo output".  Setting the Trigger pin will
# move all input values to the putput pins.

set memory_locations {store1 store2}


# You shouldn't need to change anything below this line

foreach x $memory_locations {
    studio::port in [concat $x input] -transfer dynamic -location west
    studio::port out [concat $x output] -transfer dynamic -location east
}

studio::port in Trigger -location north -transfer trigger

studio::bind Trigger {
    foreach x $memory_locations {
        upvar #0 [concat $x input] input
        upvar #0 [concat $x output] output
        if {[info exists input]} {
            set output $input
        }
    }
}
