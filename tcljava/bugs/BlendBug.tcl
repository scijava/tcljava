# begin code

package require java
java::load tcl.lang.DetachCall

# make a frame
set frame [java::new javax.swing.JFrame "Test Frame"]
$frame setSize 400 400

set pane [$frame getContentPane]
# $pane setLayout [java::new java.awt.FlowLayout]
$pane setLayout [java::new java.awt.GridLayout 0 1]

set button [java::new javax.swing.JButton "Modal dialog"]
java::bind $button java.awt.event.ActionListener.actionPerformed \
"java::call javax.swing.JOptionPane showConfirmDialog \
[java::null] screwed"
$pane add $button

set button [java::new javax.swing.JButton "Modal dialog with DetachCall"]
java::bind $button java.awt.event.ActionListener.actionPerformed \
"catch {java_detachcall javax.swing.JOptionPane showConfirmDialog \
[java::null] Works!}"
$pane add $button

set button [java::new javax.swing.JButton "Modal Tk"]

if {0} {
java::bind $button java.awt.event.ActionListener.actionPerformed \
    {tk_messageBox -icon error -type retrycancel -default retry \
	 -message Tk!}
} else {
java::bind $button java.awt.event.ActionListener.actionPerformed \
    {after 0 "tk_messageBox -icon error -type retrycancel -default retry \
	 -message Tk!"}
}

$pane add $button


# show frame
button .a -text "Frame" -command "$frame show"

# modal popup without hack - doesn't repaint tk windows
button .c -text "Modal" -command \
"java::call javax.swing.JOptionPane showConfirmDialog \
[java::null] screwed"

# test modal fix - doesn't block other button events
button .e -text "Modal dialog with DetachCall" -command \
        "catch {java_detachcall javax.swing.JOptionPane showConfirmDialog \
        [java::null] Works?}"


button .f -text "Modal Tk box" -command \
"tk_messageBox -icon error -type retrycancel -default retry \
 -message Tk!"


pack .a .c .e .f

# end code
