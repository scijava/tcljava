package test.tjc;

import tcl.lang.*;

public class TJCExtension extends Extension {
    public void init(Interp interp)
            throws TclException
    {
        interp.eval("set val OK");
    }
}

