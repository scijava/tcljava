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
 * RCS: @(#) $Id: RegsubCmd.java,v 1.5 2009/08/05 22:23:03 rszulgo Exp $
 */

package tcl.lang;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class implements the built-in "regsub" command in Tcl.
 */

class RegsubCmd implements Command {

	private static final String validOpts[] = { "-all", "-expanded", "-line",
			"-linestop", "-lineanchor", "-nocase", "-start", "--" };

	private static final int OPT_ALL = 0;
	private static final int OPT_EXPANDED = 1;
	private static final int OPT_LINE = 2;
	private static final int OPT_LINESTOP = 3;
	private static final int OPT_LINEANCHOR = 4;
	private static final int OPT_NOCASE = 5;
	private static final int OPT_START = 6;
	private static final int OPT_LAST = 7;

	/*
	 * --------------------------------------------------------------------------
	 * ---
	 * 
	 * cmdProc --
	 * 
	 * This procedure is invoked to process the "regsub" Tcl command. See the
	 * user documentation for details on what it does.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: See the user documentation.
	 * --------------------------------------------------------------------------
	 * ---
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject argv[]) // Arguments to "regsub" command.
			throws TclException {
		boolean all = false;
		boolean last = false;
		int flags = 0;
		int offset = 0;
		String result;

		// default flags
		flags |= Pattern.MULTILINE | Pattern.UNIX_LINES;

		try {
			int i = 1;
			while (!last && argv[i].toString().startsWith("-")) {
				int index = TclIndex.get(interp, argv[i], validOpts, "switch",
						0);
				i++;
				switch (index) {
				case OPT_ALL: 
					all = true;
					break;	
				case OPT_EXPANDED:
					flags |= Pattern.COMMENTS;
					break;
				case OPT_LINE:
					/* Falls through! */

				case OPT_LINESTOP:
					/* Falls through! */

				case OPT_LINEANCHOR:
					flags |= Pattern.DOTALL;
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
			}

			// get cmd's params
			TclObject exp = argv[i++];
			String string = argv[i++].toString();
			String subSpec = argv[i++].toString();
			String varName = null;

			if (argv.length - i > 0) {
				varName = argv[i++].toString();
			}

			if (i != argv.length) {
				throw new IndexOutOfBoundsException();
			}

			Regex reg;
			try {
				// we use the substring of string at the specified offset
				reg = new Regex(exp.toString(), string, offset, flags);
			} catch (PatternSyntaxException ex) {
				interp
						.setResult("couldn't compile regular expression pattern: "
								+ ex.getMessage());
				interp.setErrorCode(TclInteger.newInstance(TCL.ERROR));
				return;
			}

			/*
			 * Parse a subSpec param from Tcl's to Java's form. 
			 * 
			 * If subSpec contains a ``&'' or ``\0'', then it is replaced 
			 * in the substitution with the portion of string that matched exp. 
			 * 
			 * If subSpec contains a ``\n'', where n is a digit between 1 and 9, 
			 * then it is replaced in the substitution with the portion of string 
			 * that matched the n-th parenthesized subexpression of exp. Additional 
			 * backslashes may be used in subSpec to prevent special interpretation 
			 * of ``&'' or ``\0'' or ``\n'' or backslash.
			 * 
			 * In Java instead of '&' there is '$0' and instead of '\0' or '\1' there
			 * is '$0' and '$1', respectively. 
			 */
			
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
					"?switches? exp string subSpec varName");
		}
	}
} // end RegsubCmd
