/*
 * RegexpCmd.java --
 *
 * 	This file contains the Jacl implementation of the built-in Tcl
 *	"regexp" command. 
 *
 * Copyright (c) 1997-1999 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: RegexpCmd.java,v 1.9 2009/09/20 00:09:44 mdejong Exp $
 */

package tcl.lang;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class implements the built-in "regexp" command in Tcl.
 */

class RegexpCmd implements Command {

// switches for regexp command

private static final String validOpts[] = {
    "-all",
    "-about",
    "-indices",
    "-inline",
    "-expanded",
    "-line",
    "-linestop",
    "-lineanchor",
    "-nocase",
    "-start",
    "--" };

private static final int OPT_ALL = 0;
private static final int OPT_ABOUT = 1;
private static final int OPT_INDICES = 2;
private static final int OPT_INLINE = 3;
private static final int OPT_EXPANDED = 4;
private static final int OPT_LINE = 5;
private static final int OPT_LINESTOP = 6;
private static final int OPT_LINEANCHOR = 7;
private static final int OPT_NOCASE = 8;
private static final int OPT_START = 9;
private static final int OPT_LAST = 10;

/*
 *-----------------------------------------------------------------------------
 *
 * init --
 *
 *	This procedure is invoked to connect the regexp and regsub commands to
 *	the CmdProc method of the RegexpCmd and RegsubCmd classes,
 *	respectively.  Avoid the AutoloadStub class because regexp and regsub
 *	need a stub with a switch to check for the existence of the tcl.regexp
 *	package.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The regexp and regsub commands are now connected to the CmdProc
 *	method of the RegexpCmd and RegsubCmd classes, respectively.
 *
 *-----------------------------------------------------------------------------
 */

static void
init(
    Interp interp)  			// Current interpreter. 
{
    interp.createCommand("regexp", new tcl.lang.RegexpCmd());
    interp.createCommand("regsub", new tcl.lang.RegsubCmd());
}

/*
 *-----------------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked to process the "regexp" Tcl command.
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
    TclObject argv[])			// Arguments to "regexp" command.
throws TclException
{
    boolean indices = false;
    boolean doinline = false;
    boolean about = false;
    boolean lineAnchor = false;
    boolean lineStop = false;
    boolean last = false;
    int all = 0;
    int flags = 0;
    int offset = 0; // the index offset of the string to start matching the
    int objc = 0;
    // regular expression at
    TclObject result;

    flags |= Pattern.MULTILINE;

    try {
        int i = 1;
        while ((i < argv.length) && !last && argv[i].toString().startsWith("-")) {
            int index = TclIndex.get(interp, argv[i], validOpts, "switch", 0);

            i++;
            switch (index) {
                case OPT_ABOUT:
                    about = true;
                    break;
                case OPT_EXPANDED:
                    flags |= Pattern.COMMENTS;
                    break;
                case OPT_INDICES:
                    indices = true;
                    break;
                case OPT_LINE:
                    flags |= Pattern.DOTALL;
                    break;
                case OPT_LINESTOP:
                    flags |= Pattern.DOTALL;
                    lineStop = true;
                    break;
                case OPT_LINEANCHOR:
                    flags |= Pattern.DOTALL;
                    lineAnchor = true;
                    break;
                case OPT_NOCASE:
                    flags |= Pattern.CASE_INSENSITIVE;
                    break;
                case OPT_ALL:
                    all = 1;
                    break;
                case OPT_INLINE:
                    doinline = true;
                    break;
                case OPT_START:
                    if (i >= argv.length) {
                        // break the switch, the index out of bounds exception
                        // will be caught later
                        break;
                    }

                    try {
                        offset = TclInteger.get(interp, argv[i++]);
                    } catch (TclException e) {
                        throw e;
                    }

                    if (offset < 0) {
                        offset = 0;
                    }

                    break;
                case OPT_LAST:
                    last = true;
                    break;
                }
            } // end of switch block

            if (doinline && ((argv.length - i - 2) != 0)) {
                // User requested -inline, but specified match variables - a
                // no-no.

                throw new TclException(interp,
                    "regexp match variables not allowed when using -inline");
            }

            String exp = argv[i++].toString();
            String string;

            if (about) {
                string = "";
            } else {
                string = argv[i].toString();
            }

            Regex reg;
            result = TclInteger.newInstance(0);

            try {
                reg = new Regex(exp, string, offset, flags);
            } catch (PatternSyntaxException ex) {
                throw new TclException(interp,
                    Regex.getPatternSyntaxMessage(ex));
            }

            // If about switch was enabled, return info about regexp

            if (about) {
                TclObject props = TclList.newInstance();
                props = reg.getInfo(interp);
                interp.appendElement(props.toString());
                return;
            }

            boolean matched;

            // The following loop is to handle multiple matches within the
            // same source string; each iteration handles one match. If
            // "-all" hasn't been specified then the loop body only gets
            // executed once. We terminate the loop when the starting offset
            // is past the end of the string.

            while (true) {
                int group = 0;

                matched = reg.match();

                if (!matched) {
                    // We want to set the value of the intepreter result only
                    // when this is the first time through the loop.

                    if (all <= 1) {
                        // If inlining, set the interpreter's object result
                        // to an empty list, otherwise set it to an integer
                        // object w/ value 0.

                        if (doinline) {
                            result = TclList.newInstance();
                        } else {
                            TclInteger.set(result, 0);
                        }
                        interp.setResult(result);
                        return;
                    }

                    break;
                }

                if (doinline) {
                    // It's the number of substitutions, plus one for the
                    //  matchVar at index 0

                    objc = reg.groupCount() + 1;
                } else {
                    objc = argv.length - i - 1;
                }

                // loop for each of a variable that stores the result

                for (int j = 0; j < objc; j++) {
                    TclObject obj;
                    try {
                        if (indices) {
                            int start = -1;
                            int end = -1;

                            if (group <= reg.groupCount()) {
                                start = reg.start(group);
                                end = reg.end(group++);

                                if (end >= reg.getOffset()) {
                                    end--;
                                }
                            }

                            obj = TclList.newInstance();
                            TclList.append(interp, obj,
                                TclInteger.newInstance(start));
                            TclList.append(interp, obj,
                                TclInteger.newInstance(end));
                        } else {
                            if (reg.groupCount() > 0) {
                                if (group <= reg.groupCount()) {
                                    obj = TclString.newInstance(
                                        string.substring(
                                            reg.start(group), reg.end(group)));
                                    group++;
                                } else {
                                    obj = TclList.newInstance();
                                }
                            } else {
                                obj = TclString.newInstance(string.substring(
                                    reg.start(), reg.end()));
                            }
                        }

                        if (doinline) {
                            interp.appendElement(obj.toString());
                        } else {
                            try {
                                interp.setVar(argv[++i].toString(), obj, 0);
                            } catch (TclException e) {
                                throw new TclException(interp,
                                    "couldn't set variable \"" + argv[i]
                                        + "\"");
                            }
                        }
                    } catch (IndexOutOfBoundsException e) {
                        // TODO: Handle exception
                        return;
                    }
                } // end of for loop

                if (all == 0) {
                    break;
                }

                // Adjust the offset to the character just after the last one
                // in the matchVar and increment all to count how many times
                // we are making a match. We always increment the offset by
                // at least one to prevent endless looping (as in the case:
                // regexp -all {a*} a). Otherwise, when we match the NULL
                // string at the end of the input string, we will loop
                // indefinitely (because the length of the match is 0, so
                // the offset never changes).

                if (reg.end() - reg.start() == 0) {
                    int temp = reg.getOffset();
                    reg.setOffset(++temp);
                } else {
                    reg.setOffset(reg.end());
                }
                all++;
                if (reg.getOffset() >= string.length()) {
                    break;
                }
            } // eof of while loop

            // Set the interpreter's object result to an integer object with
            // value 1 if -all wasn't specified, otherwise it's all-1 (the
            // number of times through the while - 1). Get the resultPtr again
            // as the Tcl_ObjSetVar2 above may have cause the result to change.
            // [Patch #558324] (watson).

            if (!doinline) {
                interp.setResult(all != 0 ? all - 1 : 1);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new TclNumArgsException(interp, 1, argv,
                "?switches? exp string ?matchVar?" +
                " ?subMatchVar subMatchVar ...?");
        }
}
} // end RegexpCmd

