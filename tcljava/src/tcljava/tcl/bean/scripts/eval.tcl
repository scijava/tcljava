# eval.tcl --
#
# This script allows you to create a memory bean that works just like
# the Memory bean bundled with Java Studio.
#
# RCS: @(#) $Id: eval.tcl,v 1.1 1998/10/14 21:09:17 cvsadmin Exp $
#
# Copyright (c) 1997 Sun Microsystems, Inc.
#
# See the file "license.terms" for information on usage and redistribution
# of this file, and for a DISCLAIMER OF ALL WARRANTIES.

studio::port in eval_text -transfer basicToString
studio::port out result_output -transfer stringToBasic

studio::bind eval_text {
    if [catch {set result [eval [$eval_text toString]]} msg] {
      puts " error: $msg"
    } else {      
      set result_output "$result\n"
    }
}
