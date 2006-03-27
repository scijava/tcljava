/* 
 * TestVarFrameCmd.java --
 *
 *	This procedure implements the "testvarframe" command.  It is
 *	used for testing the local var frame and compiled local
 *	var frame implementation in Jacl.
 *
 * Copyright (c) 2006 by Mo DeJong
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: TestVarFrameCmd.java,v 1.3 2006/03/27 00:06:42 mdejong Exp $
 */

package tcl.lang;

import java.util.*;

public class TestVarFrameCmd implements Command {

/*
 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	Iterate over the entries in a variable frame for a
 *	compiled procedure and return a buffer indicating
 *	contents of the var frame.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public void 
cmdProc(
    Interp interp,		// Current interpreter.
    TclObject[] objv)		// The argument objects.
throws
    TclException
{
    if (objv.length != 1) {
	throw new TclNumArgsException(interp, 1, objv, "");
    }

    CallFrame varFrame = interp.varFrame;

    if (varFrame == null) {
        throw new TclException(interp, "can't be invoked from global scope");
    }

    HashMap localTable = varFrame.varTable;
    Var[] compiledLocals = varFrame.compiledLocals;
    String[] compiledLocalsNames = varFrame.compiledLocalsNames;
    Var clocal;
    Var var;
    StringBuffer results = new StringBuffer(128);

    // Print contents of varFrame hashtable

    if (localTable == null) {
        results.append("localTable is null\n");
    } else {
        // Iterate over Var entries in the local table.
        results.append("localTable is {\n");

	for (Iterator iter = localTable.entrySet().iterator(); iter.hasNext() ;) {
	    Map.Entry entry = (Map.Entry) iter.next();
	    var = (Var) entry.getValue();
            varInfo(interp, results, var, false);
	}

        results.append("}\n");
    }

    // Print contents of compiledLocals array

    if (compiledLocals == null) {
        results.append("compiledLocals is null\n");
    } else {
        // Iterate over Var entries in the local table.
        results.append("compiledLocals is {\n");

	for (int i=0; i < compiledLocals.length; i++) {
	    clocal = compiledLocals[i];

            if (clocal == null) {
                results.append("null\n");
                continue;
            }

            if (! clocal.hashKey.equals(compiledLocalsNames[i])) {
                throw new TclException(interp,
                    "compiledLocal[" + i + "] varname \"" +
                    clocal.hashKey +
                    "\" does not match compiledLocalsNames varname \"" +
                    compiledLocalsNames[i] + "\"");
            }

	    var = clocal;
            if (var.isVarNonLocal()) {
                results.append("Non-Local ");
            }
            varInfo(interp, results, var, false);
	}

        results.append("}\n");
    }

    // Print info about the resolved refs in the compiled local array.

    if (compiledLocals == null) {
        results.append("compiledLocals.resolved is null\n");
    } else {
        // Iterate over Var entries in the local table.
        results.append("compiledLocals.resolved is {\n");

	for (int i=0; i < compiledLocals.length; i++) {
	    clocal = compiledLocals[i];
            if (clocal == null) {
                results.append("null\n");
                continue;
            }

            Var resolved = clocal;
            if (resolved.isVarLink()) {
                resolved = (Var) resolved.value;
            }
            if (resolved.isVarScalar()) {
                resolved = Var.resolveScalar(resolved);
            } else if (resolved.isVarArray()) {
                resolved = Var.resolveArray(resolved);
            }

            varInfo(interp, results, resolved, true);
	}

        results.append("}\n");
    }

    interp.setResult(results.toString());
    return;
}

static
void varInfo(Interp interp, StringBuffer results, Var var, boolean resolved) {
    // Compiled local entries can be null

    if (var == null) {
        results.append("null\n");
        return;
    }

    Var linkto = var;

    if (linkto.isVarLink()) {
        linkto = (Var) linkto.value;

        // After resolve, Var can't be a link
        if (linkto.isVarLink()) {
            throw new TclRuntimeError(
                "var is still a link var after resolve");
        }
    }

    if ((var == linkto) && var.isVarUndefined()) {
        results.append("UNDEFINED ");
    } else if (linkto.isVarUndefined()) {
        results.append("UNDEFINED-> ");
    }

    if (linkto.isVarScalar()) {
        results.append(var.hashKey);
    } else if (linkto.isVarArray()) {
        results.append(var.hashKey);
        results.append("()");
    } else if (linkto.isVarArrayElement()) {
        results.append("(");
        results.append(linkto.hashKey);
        results.append(")");
    } else {
        results.append("?");
    }

    // Print link info if the variable is
    // linked into another frame.

    if (var.isVarLink()) {
        results.append(" -> ");
        String fullName = Var.getVariableFullName(interp, linkto);
        if (fullName.length() == 0 && linkto.isVarArrayElement()) {
            results.append("?()");
        } else {
            results.append(fullName);
        }
    }

    // The NO_CACHE flag would be set for a local
    // or linked var when the linked to var has
    // a trace set.

    if (resolved && !linkto.isVarUndefined() &&
            linkto.isVarCacheInvalid()) {
        results.append(" NO_CACHE");
    }

    results.append('\n');
}

}

