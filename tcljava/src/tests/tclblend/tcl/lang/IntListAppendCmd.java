/* 
 * IntListAppendCmd.java --
 *
 *      Check internal TclBlend variables to
 *      ensure that TclList objects created
 *      in a Java method are being added
 *      to the special cleanup queue. In
 *      this case a list is duplicated by
 *      the first append method.
 *
 * Copyright (c) 2002 by Mo DeJong
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: IntListAppendCmd.java,v 1.1 2002/12/21 04:04:19 mdejong Exp $
 */

package tcl.lang;

public class IntListAppendCmd implements Command {
    CObject saved = null;

    public void 
    cmdProc(
	Interp interp,
	TclObject[] objv)
    throws TclException
    {
        // Pass an argument to this method to check the
        // cleanup status of the saved CObject
        if (objv.length > 2) {
            if (saved == null)
                interp.setResult(-1);
            else
                interp.setResult(saved.onCleanupQueue?1:0);
            saved = null;
            return;
        }

        TclObject obj = objv[1].takeExclusive();
        if (obj == objv[1])
            throw new TclException(interp, "takeExclusive() did not duplicate");

        TclList.append(interp, obj, TclString.newInstance("blue"));
        TclList.append(interp, obj, TclString.newInstance("green"));

        CObject cobj = (CObject) obj.getInternalRep();
        if (!cobj.onCleanupQueue)
            throw new TclException(interp, "not added to cleanup queue");

        saved = cobj;
        interp.setResult("saved");
        return;
    }
}
