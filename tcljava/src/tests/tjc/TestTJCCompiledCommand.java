// default package

import tcl.lang.*;

public class TestTJCCompiledCommand extends TJC.CompiledCommand {
    public void cmdProc(Interp interp, TclObject[] objv)
            throws TclException
    {
        interp.setResult("OK from " + wcmd.ns.fullName);
    }
}

