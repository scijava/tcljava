/*
 * InfoCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: InfoCmd.java,v 1.1 1998/10/14 21:09:20 cvsadmin Exp $
 *
 */

package tcl.lang;

import java.util.*;

/**
 * This class implements the built-in "info" command in Tcl.
 */

class InfoCmd implements Command {
    static final private String validCmds[] = {
	"args",
	"body",
	"cmdcount",
	"commands",
	"complete",	
	"default",
	"exists",
	"globals",
	"hostname",
	"level",
	"library",
	"loaded",
	"locals",
	"nameofexecutable",
	"patchlevel",
	"procs",
	"script",
	"sharedlibextension",
	"tclversion",
	"vars"
    };

    static final int OPT_ARGS			= 0;
    static final int OPT_BODY			= 1;
    static final int OPT_CMDCOUNT		= 2;
    static final int OPT_COMMANDS		= 3;
    static final int OPT_COMPLETE		= 4;
    static final int OPT_DEFAULT		= 5;
    static final int OPT_EXISTS			= 6;
    static final int OPT_GLOBALS		= 7;
    static final int OPT_HOSTNAME		= 8;
    static final int OPT_LEVEL			= 9;
    static final int OPT_LIBRARY		= 10;
    static final int OPT_LOADED			= 11;
    static final int OPT_LOCALS			= 12;
    static final int OPT_NAMEOFEXECUTABLE	= 13;
    static final int OPT_PATCHLEVEL		= 14;
    static final int OPT_PROCS			= 15;
    static final int OPT_SCRIPT			= 16;
    static final int OPT_SHAREDLIBEXTENSION	= 17;
    static final int OPT_TCLVERSION		= 18;
    static final int OPT_VARS			= 19;

    /**
     * This procedure is invoked to process the "info" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     * @exception TclException if wrong # of args or invalid argument(s).
     */
    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	Command cmd;
	Procedure proc;

	if (argv.length < 2) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "option ?arg arg ...?");
	}
	int index = TclIndex.get(interp, argv[1], validCmds, "option", 0);

	switch (index) {
	    case OPT_ARGS:
		if (argv.length != 3) {
		    throw new TclNumArgsException(interp, 2, argv, 
			    "procname");
		}
		cmd = (Command) interp.cmdTable.get(argv[2].toString());
		if ((cmd == null) || !(cmd instanceof Procedure)) {
		    throw new TclException(interp, "\"" + argv[2] + 
			    "\" isn't a procedure");
		}
		proc = (Procedure) cmd;
		TclObject list = TclList.newInstance();
		for (int i = 0; i < proc.argList.length; i++) {
		    TclObject s = TclString.newInstance(proc.argList[i][0]);
		    TclList.append(interp, list, s);
		}
		interp.setResult(list);
		return;
	    case OPT_BODY:
		if (argv.length != 3) {
		    throw new TclNumArgsException(interp, 2, argv, 
			    "procname");
		}
		cmd = (Command) interp.cmdTable.get(argv[2].toString());
		if ((cmd == null) || !(cmd instanceof Procedure)) {
		    throw new TclException(interp, "\"" + argv[2] + 
			    "\" isn't a procedure");
		}
		proc = (Procedure) cmd;
		TclObject s = TclString.newInstance(proc.body);
		interp.setResult(s);
		return;
	    case OPT_CMDCOUNT:
		if (argv.length != 2) {
		    throw new TclNumArgsException(interp, 2, argv, null);
		}
		interp.setResult(interp.cmdCount);
		return;
	    case OPT_COMMANDS:
		if (argv.length != 2 && argv.length != 3) {
		    throw new TclNumArgsException(interp, 2, argv, 
			    "?pattern?");
		}
		if (argv.length == 2) {
		    matchAndAppend(interp, interp.cmdTable.keys(), null);
		} else {
		    matchAndAppend(interp, interp.cmdTable.keys(),
			    argv[2].toString());
		}
		return;
	    case OPT_COMPLETE:
		if (argv.length != 3) {
		    throw new TclNumArgsException(interp, 2, argv, 
			    "command");
		}
		interp.setResult(TclBoolean.newInstance(
			interp.commandComplete(argv[2].toString())));
		return;
	    case OPT_DEFAULT:
		if (argv.length != 5) {
		    throw new TclNumArgsException(interp, 2, argv, 
			    "procname arg varname");
		}

		String procName = argv[2].toString();
		String argName = argv[3].toString();
		cmd = (Command) interp.cmdTable.get(argv[2].toString());
		if ((cmd == null) || !(cmd instanceof Procedure)) {
		    throw new TclException(interp, "\"" + argv[2] + 
			    "\" isn't a procedure");
		}

		proc = (Procedure) cmd;
		for (int i = 0; i < proc.argList.length; i++) {
		    if (argName.equals(proc.argList[i][0].toString())) {
			String varName = argv[4].toString();
			try {
			    if (proc.argList[i][1] != null) {
				interp.setVar(varName, proc.argList[i][1], 0);
				interp.setResult(1);
			    } else {
				s = TclString.newInstance("");
				interp.setVar(varName, s, 0);
				interp.setResult(0);
			    }
			} catch (TclException excp) {
			    throw new TclException(interp, 
				 "couldn't store default value in variable \""
				 + varName + "\"");
			}
			return;
		    }
		}
		throw new TclException(interp, "procedure \"" + procName +
			"\" doesn't have an argument \"" + argName + "\"");
	    case OPT_EXISTS:
		if (argv.length != 3) {
		    throw new TclNumArgsException(interp, 2, argv, 
			    "varName");
		}

		boolean exists = interp.varFrame.exists(argv[2].toString());
		interp.setResult(TclBoolean.newInstance(exists));
		return;
	    case OPT_GLOBALS:
		if (argv.length != 2 && argv.length != 3) {
		    throw new TclNumArgsException(interp, 2, argv, 
			    "?pattern?");
		}

		if (argv.length == 2) {
		    matchAndAppend(interp, interp.globalFrame.getVarNames(), 
			    null);
		} else {
		    matchAndAppend(interp, interp.globalFrame.getVarNames(),
			    argv[2].toString());
		}
		return;
	    case OPT_HOSTNAME:
		if (argv.length != 2) {
		    throw new TclNumArgsException(interp, 2, argv, null);
		}
		interp.setResult("no host info");
		return;
	    case OPT_LEVEL:
		if (argv.length != 2 && argv.length != 3) {
		    throw new TclNumArgsException(interp, 2, argv, 
			    "?number?");
		}
		if (argv.length == 2) {
		    if (interp.varFrame == null) {
			interp.setResult(0);
		    } else {
			interp.setResult(interp.varFrame.m_level);
		    }
		    return;
		} else {
		    int level = TclInteger.get(interp, argv[2]);
		    if (level > 0) {
			if (interp.varFrame == null) {
			    throw new TclException(interp, "bad level \""
				    + level + "\"");
			}
			level -= interp.varFrame.m_level;
		    }

		    CallFrame currentFrame = interp.varFrame;
		    int counter = level;
		    while (counter != 0) {
			if (currentFrame == null) {
			    break;
			}
			currentFrame = currentFrame.callerVar;
			counter++;
		    }
		    if ((currentFrame == null) ||
			    (currentFrame.m_argv == null)) {
			throw new TclException(interp, "bad level \"" +
				level + "\"");
		    }
		    list = TclList.newInstance();
		    for (int i = 0; i < currentFrame.m_argv.length; i++) {
			s = TclString.newInstance(currentFrame.m_argv[i]);
			TclList.append(interp, list, s);
		    }
		    interp.setResult(list);
		    return;
		}
	    case OPT_LIBRARY:
		if (argv.length != 2) {
		    throw new TclNumArgsException(interp, 2, argv, null);
		}
		TclObject obj = interp.getVar("tcl_library",
			TCL.GLOBAL_ONLY | TCL.DONT_THROW_EXCEPTION);
		if (obj != null) {
		    interp.setResult(obj);
		} else {
		    throw new TclException(interp,
			    "no library has been specified for Tcl");
		}
		return;
	    case OPT_LOADED:
		if (argv.length != 2 && argv.length != 3) {
		    throw new TclNumArgsException(interp, 2, argv, 
			    "?interp?");
		}
		throw new TclException(interp,
			"info loaded not implemented");
	    case OPT_LOCALS:
		if (argv.length != 2 && argv.length != 3) {
		    throw new TclNumArgsException(interp, 2, argv, 
			    "?pattern?");
		}

		if (interp.varFrame == interp.globalFrame) {
		    return;
		}

		if (argv.length == 2) {
		    matchAndAppend(interp, interp.varFrame.getLocalVarNames(), 
			    null);
		} else {
		    matchAndAppend(interp, interp.varFrame.getLocalVarNames(),
			    argv[2].toString());
		}
		return;
	    case OPT_NAMEOFEXECUTABLE:
		if (argv.length != 2) {
		    throw new TclNumArgsException(interp, 2, argv, null);
		}
		interp.setResult(TclString.newInstance(""));
		return;
	    case OPT_PATCHLEVEL:
		if (argv.length != 2) {
		    throw new TclNumArgsException(interp, 2, argv, null);
		}
		interp.setResult(interp.getVar("tcl_patchLevel", 
			TCL.GLOBAL_ONLY));
		return;
	    case OPT_PROCS:
		String pattern = null;
		if (argv.length != 2 && argv.length != 3) {
		    throw new TclNumArgsException(interp, 2, argv, 
			    "?pattern?");
		}
		if (argv.length == 3) {
		    pattern = argv[2].toString();
		}

		StringBuffer sbuf = new StringBuffer();
		for (Enumeration e = interp.cmdTable.keys();
		     e.hasMoreElements(); ) {
		    String key = (String)e.nextElement();

		    if (pattern != null) {
			if (!Util.stringMatch(key, pattern)) {
			    continue;
			}
		    }

		    cmd = (Command)interp.cmdTable.get(key);
		    if (cmd instanceof Procedure) {
			Util.appendElement(interp, sbuf, key);
		    }
		}

		interp.setResult(TclString.newInstance(sbuf));
		return;
	    case OPT_SCRIPT:
		if (argv.length != 2) {
		    throw new TclNumArgsException(interp, 2, argv, null);
		}
		interp.setResult(TclString.newInstance(interp.scriptFile));
		return;
	    case OPT_SHAREDLIBEXTENSION:
		if (argv.length != 2) {
		    throw new TclNumArgsException(interp, 2, argv, null);
		}
		interp.setResult(".class");
		return;
	    case OPT_TCLVERSION:
		if (argv.length != 2) {
		    throw new TclNumArgsException(interp, 2, argv, null);
		}
		interp.setResult(interp.getVar("tcl_version", 
			TCL.GLOBAL_ONLY));
		return;
	    case OPT_VARS:
		if (argv.length != 2 && argv.length != 3) {
		    throw new TclNumArgsException(interp, 2, argv, 
			    "?pattern?");
		}

		if (argv.length == 2) {
		    matchAndAppend(interp, interp.varFrame.getVarNames(),null);
		} else {
		    matchAndAppend(interp, interp.varFrame.getVarNames(),
			    argv[2].toString());
		}
		return;
	    default:
		throw new TclException(interp, argv[0] + " " + argv[1] + 
			" function not yet implemented");
	}
    }

    /**
     * Set to interp.result a Tcl list that contains all the elements in the
     * Enumeration that match the pattern.
     *
     * @param interp current interpreter.
     * @param e the enumeration, usually the keys of a hashtable.
     * @param pattern a "glob" pattern to match the element with.
     */

    private void matchAndAppend(Interp interp, Enumeration e,
	    String pattern) {
	StringBuffer sbuf = new StringBuffer();
	while (e.hasMoreElements()) {
	    String key = (String)e.nextElement();

	    if (pattern != null) {
		if (!Util.stringMatch(key, pattern)) {
		    continue;
		}
	    }

	    try {
		Util.appendElement(interp, sbuf, key);
	    } catch (TclException excp) {
		throw new TclRuntimeError("unexpected TclException: " + excp);
	    }
	}

	interp.setResult(TclString.newInstance(sbuf));
    }

    /**
     * Set to interp.result a Tcl list that contains all the elements in the
     * Vector that match the pattern.
     *
     * @param interp current interpreter.
     * @param v the vector that contains the elements.
     * @param pattern a "glob" pattern to match the element with.
     */

    private void matchAndAppend(Interp interp, Vector v,
	    String pattern) {

	StringBuffer sbuf = new StringBuffer();
	int length = v.size();
	for (int i=0; i<length; i++) {
	    String key = (String)v.elementAt(i);

	    if (pattern != null) {
		if (!Util.stringMatch(key, pattern)) {
		    continue;
		}
	    }

	    try {
		Util.appendElement(interp, sbuf, key);
	    } catch (TclException excp) {
		throw new TclRuntimeError("unexpected TclException: " + excp);
	    }
	}

	interp.setResult(TclString.newInstance(sbuf));
    }
}

