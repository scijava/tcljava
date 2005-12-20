package test.tjcext;

import tcl.lang.*;

public class TJCExtension extends Extension {
    public void init(Interp interp)
            throws TclException
    {
        String init_file = "f1.tcl";
        String[] files = {
            "f1.tcl",
            "f2.tcl",
            "f3.tcl"
        };
        String prefix = "/test/tjcext/library/";

        TJC.sourceInitFile(interp, init_file, files, prefix);     
    }
}

