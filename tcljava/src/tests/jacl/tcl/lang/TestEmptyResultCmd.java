/*
 * TestEmptyResultCmd.java --
 *
 * Copyright (c) 2003 Mo DeJong
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TestEmptyResultCmd.java,v 1.1 2003/01/09 08:42:30 mdejong Exp $
 *
 */

package tcl.lang;

class TestEmptyResultCmd implements Command
{
    public void cmdProc(Interp interp, TclObject objv[])
        throws TclException
    {
        if (objv.length == 1) {
            interp.resetResult();
            interp.setResult(interp.getResult().isShared());
        } else if (objv.length == 2) {
            int refCount;
            interp.resetResult();
            TclObject null_result = interp.getResult();
            refCount = null_result.getRefCount();
            interp.setResult(null_result);
            if (refCount != interp.getResult().getRefCount())
                interp.setResult("setting null result changed ref count");
            interp.setResult("");
            interp.setResult(null_result);
            if (refCount != interp.getResult().getRefCount())
                interp.setResult("setting null result after non-null changed ref count");
            interp.setResult("ok");
        }
    }
}
