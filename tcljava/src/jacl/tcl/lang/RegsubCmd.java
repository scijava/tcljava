/*
 * RegsubCmd.java
 *
 * 	This contains the Jacl implementation of the built-in Tcl
 *	"regsub" command.
 *
 * Copyright (c) 1997-1999 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: RegsubCmd.java,v 1.11 2009/10/01 03:29:08 mdejong Exp $
 */

package tcl.lang;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class implements the built-in "regsub" command in Tcl.
 */

class RegsubCmd implements Command {

private static final String validOpts[] = {
    "-all",
    "-nocase",
    "-expanded",
    "-line",
    "-linestop",
    "-lineanchor",
    "-start",
    "--" };

private static final int OPT_ALL = 0;
private static final int OPT_NOCASE = 1;
private static final int OPT_EXPANDED = 2;
private static final int OPT_LINE = 3;
private static final int OPT_LINESTOP = 4;
private static final int OPT_LINEANCHOR = 5;
private static final int OPT_START = 6;
private static final int OPT_LAST = 7;

/*
 *-----------------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked to process the "regsub" Tcl command.
 *	See the user documentation for details on what it does.
 *
 * Results:
 *	A standard Tcl result.
 *
 * Side effects:
 *	See the user documentation.
 *
 *-----------------------------------------------------------------------------
 */

public void
cmdProc(
    Interp interp,   			// Current interpreter. 
    TclObject argv[])			// Arguments to "regsub" command.
throws TclException 
{
    boolean all = false;
    boolean last = false;
    int flags;
    int offset = 0;
    String result;

    // Default regexp behavior is to assume that '.' will match newline
    // characters and that only \n is seen as a newline. Support for
    // newline sensitive matching must be enabled, it is off by default.

    flags = Pattern.DOTALL | Pattern.UNIX_LINES;

    try {
        int i = 1;
        while (!last && argv[i].toString().startsWith("-")) {
            int index = TclIndex.get(interp, argv[i], validOpts, "switch", 0);

            i++;
            switch (index) {
                case OPT_ALL: 
                    all = true;
                    break;	
                case OPT_EXPANDED:
                    flags |= Pattern.COMMENTS;
                    break;
                case OPT_LINESTOP:
                    flags &= ~Pattern.DOTALL; // Don't match . to newline character
                    break;
                case OPT_LINEANCHOR:
                    flags |= Pattern.MULTILINE; // Use line sensitive matching
                    break;
                case OPT_LINE:
                    flags |= Pattern.MULTILINE; // Use line sensitive matching
                    flags &= ~Pattern.DOTALL; // Don't match . to newline character
                    break;
                case OPT_NOCASE:
                    flags |= Pattern.CASE_INSENSITIVE;
                    break;
                case OPT_START:
                    if (i >= argv.length) {
                        // break the switch, the index out of bounds exception
                        // will be caught later

                        break;
                    }

                    offset = TclInteger.get(interp, argv[i++]);

                    if (offset < 0) {
                        offset = 0;
                    }
                    break;
                case OPT_LAST:
                    last = true;
                    break;
            }
        }

        // get cmd's params

        String exp = argv[i++].toString();
        String string = argv[i++].toString();
        String subSpec = argv[i++].toString();
        String varName = null;

        if ((argv.length - i) > 0) {
            varName = argv[i++].toString();
        }

        if (i != argv.length) {
            throw new IndexOutOfBoundsException();
        }

        Regex reg;
        try {
            // we use the substring of string at the specified offset
            reg = new Regex(exp, string, offset, flags);
        } catch (PatternSyntaxException ex) {
            throw new TclException(interp,
                Regex.getPatternSyntaxMessage(ex));
        }

        // Parse a subSpec param from Tcl's to Java's form. 

        subSpec = Regex.parseSubSpec(subSpec);

        // do the replacement process

        if (!all) {
            result = reg.replaceFirst(subSpec);
        } else {
            result = reg.replaceAll(subSpec);
        }

        // set results

        TclObject obj = TclString.newInstance(result);

        try {
            if (varName != null) {
                interp.setResult(reg.getCount());
                interp.setVar(varName, obj, 0);
            } else {
                interp.setResult(obj);
            }
        } catch (TclException e) {
            throw new TclException(interp, "couldn't set variable \""
                + varName + "\"");
        }
    } catch (IndexOutOfBoundsException e) {
        throw new TclNumArgsException(interp, 1, argv,
            "?switches? exp string subSpec ?varName?");
    }
}

} // end class RegsubCmd

