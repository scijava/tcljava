/*
 * LappendCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 * Copyright (c) 1998-1999 by Scriptics Corporation.
 * Copyright (c) 1999 Mo DeJong.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LappendCmd.java,v 1.2 1999/08/15 19:38:25 mo Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "lappend" command in Tcl.
 */
class LappendCmd implements Command {
    /**
     *
     * Tcl_LappendObjCmd -> LappendCmd.cmdProc
     *
     * This procedure is invoked to process the "lappend" Tcl command.
     * See the user documentation for details on what it does.
     */

    public void cmdProc(Interp interp, TclObject[] objv)
	    throws TclException {
	TclObject varValue, newValue = null;
	int numElems, i, j;
	boolean createdNewObj, createVar;
	
	if (objv.length < 2) {
	    throw new TclNumArgsException(interp, 1, objv, 
                    "varName ?value value ...?");
        }
	if (objv.length == 2) {
	    try {
		newValue = interp.getVar(objv[1], 0);
	    } catch (TclException e) {
		// The variable doesn't exist yet. Just create it with an empty
		// initial value.

		interp.setVar(objv[1], TclList.newInstance(), 0);
		interp.resetResult();
		return;
	    }
	} else {
	    /*
	    // FIXME : remove this old implementation later
	    TclObject result = null;
	    for (i = 2; i < objv.length; i++) {
		result = interp.setVar(objv[1], objv[i],
			     TCL.APPEND_VALUE | TCL.LIST_ELEMENT);
	    }
	    interp.setResult(result);
	    return;
	    */

	    // We have arguments to append. We used to call Tcl_SetVar2 to
	    // append each argument one at a time to ensure that traces were run
	    // for each append step. We now append the arguments all at once
	    // because it's faster. Note that a read trace and a write trace for
	    // the variable will now each only be called once. Also, if the
	    // variable's old value is unshared we modify it directly, otherwise
	    // we create a new copy to modify: this is "copy on write".

	    createdNewObj = false;
	    createVar = true;
	    
	    try {
		varValue = interp.getVar(objv[1], 0);
	    } catch (TclException e) {
		// We couldn't read the old value: either the var doesn't yet
		// exist or it's an array element. If it's new, we will try to
		// create it with Tcl_ObjSetVar2 below.

		// FIXME : not sure we even need this parse for anything!
		// If we do not need to parse could we at least speed it up a bit

		String varName;
		int nameBytes;

		varName = objv[1].toString();
		nameBytes = varName.length(); // Number of Unicode chars in string
		
		for (i = 0;  i < nameBytes;  i++) {
		    if (varName.charAt(i) == '(') {
			i = nameBytes - 1;
			if (varName.charAt(i) == ')') { // last char is ')' => array ref
			    createVar = false;
			}
			break;
		    }
		}
		varValue = TclList.newInstance();
		createdNewObj = true;
	    }

	    // We only take this branch when the catch branch was not run
            if (createdNewObj == false && varValue.isShared()) {
		/*
		// FIXME : this extra preserve() is a hack to avoid null
		// pointer exception in Var.setVar(). For some reason a
		// takeExclusive() does not seem to work properly.
		varValue.preserve();

		varValue = varValue.takeExclusive();
		createdNewObj = true;
		*/

		// Create a new empty list object and copy the values
		// from the old list into this new one
		
		TclObject[] elems = TclList.getElements(interp, varValue);
		varValue = TclList.newInstance();
		createdNewObj = true;
		for (i = 0; i < elems.length ; i++) {
		    TclList.append(interp, varValue, elems[i]);
		}
	    }

	    // Insert the new elements at the end of the list.

	    for (i = 2; i < objv.length ; i++) {
		TclList.append(interp, varValue, objv[i]);
	    }

	    // Now store the list object back into the variable. If there is an
	    // error setting the new value, decrement its ref count if it
	    // was new and we didn't create the variable.

	    try {
		newValue = interp.setVar(objv[1].toString(), varValue, 0);
	    } catch (TclException e) {
		if (createdNewObj && !createVar) {
		    varValue.release(); // free unneeded obj
		}
		throw e;
	    }
	}

	// Set the interpreter's object result to refer to the variable's value
	// object.

	interp.setResult(newValue);
	return;
    }
}
