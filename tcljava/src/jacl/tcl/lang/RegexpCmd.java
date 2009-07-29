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
 * RCS: @(#) $Id: RegexpCmd.java,v 1.4 2009/07/29 12:00:21 rszulgo Exp $
 */

package tcl.lang;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class implements the built-in "regexp" command in Tcl.
 */

class RegexpCmd implements Command {

	private static final String validOpts[] = { "-about", "-expanded",
			"-indices", "-line", "-linestop", "-lineanchor", "-nocase", "-all",
			"-inline", "-start", "--" };

	private static final int OPT_ABOUT = 0;
	private static final int OPT_EXPANDED = 1;
	private static final int OPT_INDICES = 2;
	private static final int OPT_LINE = 3;
	private static final int OPT_LINESTOP = 4;
	private static final int OPT_LINEANCHOR = 5;
	private static final int OPT_NOCASE = 6;
	private static final int OPT_ALL = 7;
	private static final int OPT_INLINE = 8;
	private static final int OPT_START = 9;
	private static final int OPT_LAST = 10;

	/* Expressions that indicate use of the boundary matcher '^' */
	private static final String REGEX_START1 = "^";
	private static final String REGEX_START2 = "|^";
	private static final String REGEX_START3 = "(^";

	/*
	 * --------------------------------------------------------------------------
	 * ---
	 * 
	 * init --
	 * 
	 * This procedure is invoked to connect the regexp and regsub commands to
	 * the CmdProc method of the RegexpCmd and RegsubCmd classes, respectively.
	 * Avoid the AutoloadStub class because regexp and regsub need a stub with a
	 * switch to check for the existence of the tcl.regexp package.
	 * 
	 * Results: None.
	 * 
	 * Side effects: The regexp and regsub s]commands are now connected to the
	 * CmdProc method of the RegexpCmd and RegsubCmd classes, respectively.
	 * --------------------------------------------------------------------------
	 * ---
	 */

	static void init(Interp interp) // Current interpreter.
	{
		interp.createCommand("regexp", new tcl.lang.RegexpCmd());
		interp.createCommand("regsub", new tcl.lang.RegsubCmd());
	}

	/*
	 * --------------------------------------------------------------------------
	 * ---
	 * 
	 * cmdProc --
	 * 
	 * This procedure is invoked to process the "regexp" Tcl command. See the
	 * user documentation for details on what it does.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: See the user documentation.
	 * 
	 * --------------------------------------------------------------------------
	 * ---
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject argv[]) // Arguments to "regexp" command.
			throws TclException {
		boolean indices = false;
		boolean doinline = false;
		int all = 0;
		int flags = 0;
		int offset = 0; // the index offset of the string to start matching the
		int objc = 0;
		int nsubs = 0;
		// regular expression at
		TclObject result;

		try {
			int i = 1;
			opts: while (argv[i].toString().startsWith("-")) {
				int index = TclIndex.get(interp, argv[i], validOpts, "switch",
						0);
				i++;
				switch (index) {
				case OPT_ABOUT:
					break;
				case OPT_EXPANDED:
					break;
				case OPT_INDICES:
					indices = true;
					break;
				case OPT_LINE:
					break;
				case OPT_LINESTOP:
					break;
				case OPT_LINEANCHOR:
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
					break opts;

				}
			}

			if (doinline && argv.length - i - 2 != 0) {
				/*
				 * User requested -inline, but specified match variables - a
				 * no-no.
				 */
				interp
						.appendElement("regexp match variables not allowed when using -inline");
				interp.setErrorCode(TclInteger.newInstance(TCL.ERROR));
				return;
			}

			String exp = argv[i++].toString();
			String string = argv[i].toString();
			Pattern pattern = null;
			result = TclInteger.newInstance(0);
			
			try {
				pattern = Pattern.compile(exp, flags);
			} catch (PatternSyntaxException ex) {
				interp
						.setResult("couldn't compile regular expression pattern: "
								+ ex.getMessage());
				interp.setErrorCode(TclInteger.newInstance(TCL.ERROR));
				return;
			}

			Matcher matcher = pattern.matcher(string);

			boolean matched;

			/*
			 * The following loop is to handle multiple matches within the same
			 * source string; each iteration handles one match. If "-all" hasn't
			 * been specified then the loop body only gets executed once. We
			 * terminate the loop when the starting offset is past the end of
			 * the string.
			 */

			while (true) {
				int group = 0;

				// if offset was changed via -start switch to non-zero value, and
				// regex has '^', it will surely not match
				if ((offset != 0)
						&& (exp.startsWith(REGEX_START1)
								|| exp.contains(REGEX_START2) || exp
								.contains(REGEX_START3))) {
					matched = false;
				} else {

					// check if offset is in boundaries of string length
					if (offset > string.length()) {
						offset = string.length();
					}

					matched = matcher.find(offset);
				}
				if (!matched) {
					/*
					 * We want to set the value of the intepreter result only
					 * when this is the first time through the loop.
					 */
					if (all <= 1) {
						/*
						 * If inlining, set the interpreter's object result to
						 * an empty list, otherwise set it to an integer object
						 * w/ value 0.
						 */
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
				    /*
				     * It's the number of substitutions, plus one for the matchVar
				     * at index 0
				     */
				    objc = matcher.groupCount() + 1;
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

							if (group <= matcher.groupCount()) {
								start =  matcher.start(group);
								end = matcher.end(group++);

								if (end >= offset) {
									end--;
								}
							}

							obj = TclList.newInstance();

							TclList.append(interp, obj, TclInteger
									.newInstance(start));
							TclList.append(interp, obj, TclInteger
									.newInstance(end));
						} else {
							if (matcher.groupCount() > 0) {
								if (group <= matcher.groupCount()) {
									obj = TclString.newInstance(matcher.group(group++));
								} else {
									obj = TclList.newInstance();
								}
							} else {
								obj = TclString.newInstance(string.substring(matcher.start(), matcher.end()));
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
						//TODO: Handle exception
						return;
					}
				}

				if (all == 0) {
					break;
				}

				/*
				 * Adjust the offset to the character just after the last one in
				 * the matchVar and increment all to count how many times we are
				 * making a match. We always increment the offset by at least
				 * one to prevent endless looping (as in the case: regexp -all
				 * {a*} a). Otherwise, when we match the NULL string at the end
				 * of the input string, we will loop indefinitely (because the
				 * length of the match is 0, so offset never changes).
				 */
				if (matcher.end() - matcher.start() == 0) {
					offset++;
				} else {
					offset = matcher.end();
				}
				all++;
				if (offset >= string.length()) {
					break;
				}
			}
			/*
			 * Set the interpreter's object result to an integer object with
			 * value 1 if -all wasn't specified, otherwise it's all-1 (the
			 * number of times through the while - 1). Get the resultPtr again
			 * as the Tcl_ObjSetVar2 above may have cause the result to change.
			 * [Patch #558324] (watson).
			 */

			if (!doinline) {
				//TclObject resultPtr = interp.getResult();
				TclInteger.set(result, (all != 0 ? all - 1 : 1));
				interp.setResult(result);
			}

		} catch (IndexOutOfBoundsException e) {
			throw new TclNumArgsException(interp, 1, argv,
					"?switches? exp string ?matchVar? ?subMatchVar subMatchVar ...?");
		}
	}
} // end RegexpCmd
