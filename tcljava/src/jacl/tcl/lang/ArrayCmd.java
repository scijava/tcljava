/*
 * ArrayCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ArrayCmd.java,v 1.2 1999/05/08 23:51:17 dejong Exp $
 *
 */

package tcl.lang;
import java.util.*;

/**
 * This class implements the built-in "array" command in Tcl.
 */

class ArrayCmd implements Command {
    static Class procClass = null;

    static final private String validCmds[] = {
	"anymore",
	"donesearch",
	"exists",
	"get",
	"names",
	"nextelement",
	"set",
	"size",
	"startsearch"
    };

    static final int OPT_ANYMORE 	= 0;
    static final int OPT_DONESEARCH 	= 1;
    static final int OPT_EXISTS 	= 2;
    static final int OPT_GET 		= 3;
    static final int OPT_NAMES 		= 4;
    static final int OPT_NEXTELEMENT 	= 5;
    static final int OPT_SET 		= 6;
    static final int OPT_SIZE 		= 7;
    static final int OPT_STARTSEARCH 	= 8;

    /**
     * This procedure is invoked to process the "array" Tcl command.
     * See the user documentation for details on what it does.
     */

    public void cmdProc(Interp interp, TclObject argv[])
            throws TclException {
	if (argv.length < 3) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "option arrayName ?arg ...?");
	}

	boolean notArray = false;
	Var var = (Var)interp.varFrame.varTable.get(argv[2].toString());
	if (var != null && (var.flags & Var.UNDEFINED) != 0) {
	    var = null;
	}
	if (var != null && (var.flags & Var.UPVAR) != 0) {
	    var = (Var)var.value;
	}
	if (var == null || (var.flags & Var.ARRAY) == 0) {
	    notArray = true;
	}

	int index = TclIndex.get(interp, argv[1], validCmds, "option", 0);

	switch (index) {
	    case OPT_ANYMORE: {
	        if (argv.length != 4) {
		    throw new TclNumArgsException(interp, 1, argv, 
		            "anymore arrayName searchId");
	        }
	        if (notArray) {
	            errorNotArray(interp, argv[2].toString());
	        }

	        if (var.sidVec == null) {
	            errorIllegalSearchId(interp, argv[2].toString(),
                            argv[3].toString());
	        }

	        Enumeration e = var.getSearch(argv[3].toString());
	        if (e == null) {
	            errorIllegalSearchId(interp, argv[2].toString(),
                            argv[3].toString());
	        }

	        if (e.hasMoreElements()) {
	            interp.setResult("1");
	        } else {
	            interp.setResult("0");
	        }
		break;
	    }
	    case OPT_DONESEARCH: {

	        if (argv.length != 4) {
		    throw new TclNumArgsException(interp, 1, argv, 
		            "donesearch arrayName searchId");
	        }
	        if (notArray) {
	            errorNotArray(interp, argv[2].toString());
	        }

		boolean rmOK = true;
	        if (var.sidVec != null) {
		  rmOK = (var.removeSearch(argv[3].toString()));
		}
		if ((var.sidVec == null) || !rmOK) {
	            errorIllegalSearchId(interp, argv[2].toString(),
                            argv[3].toString());
		}
		break;
	    }
	    case OPT_EXISTS: {

                if (argv.length != 3) {
		    throw new TclNumArgsException(interp, 1, argv, 
		            "exists arrayName");
	        }
	        interp.setResult(!notArray);
		break;
	    }
	    case OPT_GET: {

	        /*
		 * Due to the differences in the hashtable implementation 
		 * from the Tcl core and Java, the output will be rearranged.
		 * This is not a negative side effect, however, test results 
		 * will differ.
		 */

	        if ((argv.length != 3) && (argv.length != 4) ) {
		    throw new TclNumArgsException(interp, 1, argv, 
		            "get arrayName ?pattern?");
	        }
		if (notArray) {
	            return;
	        }

	        String pattern = null;
	        if (argv.length == 4) {
	            pattern = argv[3].toString();
	        }
	    
		Hashtable table = (Hashtable)var.value;
	        TclObject tobj = TclList.newInstance();
	        String arrayName = argv[2].toString();
	        String key, strValue;
		Var elem;

 	        /*
		 * Go through each key in the hash table.  If there is a 
		 * pattern, test for a match.  Each valid key and its value 
		 * is written into sbuf, which is returned.
		 */

		for (Enumeration e = ((Hashtable)var.value).keys();
			e.hasMoreElements(); ) {

 	            key = (String)e.nextElement();
		    elem = (Var)table.get(key);
		    if ((elem.flags & Var.UNDEFINED) == 0) {
		        strValue = interp.getVar(arrayName, key, 0).toString();
			if (pattern != null) {
			    if (!Util.stringMatch(key, pattern)) {
			        continue;
			    }
			}
			TclList.append(interp, tobj, 
                                TclString.newInstance(key));
			TclList.append(interp, tobj, 
			        TclString.newInstance(strValue));
		    }
		}
		interp.setResult(tobj);
		break;
	    }
	    case OPT_NAMES: {

	        if ((argv.length != 3) && (argv.length != 4)) {
		    throw new TclNumArgsException(interp, 1, argv, 
		            "names arrayName ?pattern?");
	        }
	        if (notArray) {
	            return;
	        }

	        String pattern = null;
	        if (argv.length == 4) {
	            pattern = argv[3].toString();
	        }

		Hashtable table = (Hashtable)var.value;
	        TclObject tobj = TclList.newInstance();
	        String key;

 	        /*
		 * Go through each key in the hash table.  If there is a 
		 * pattern, test for a match. Each valid key and its value 
		 * is written into sbuf, which is returned.
		 */

		for (Enumeration e = table.keys(); e.hasMoreElements(); ) {
 	            key = (String)e.nextElement();
		    Var elem = (Var)table.get(key);
		    if ((elem.flags & Var.UNDEFINED) == 0) {
			if (pattern != null) {
			    if (!Util.stringMatch(key, pattern)) {
				continue;
			    }
			}
			TclList.append(interp, tobj,
				TclString.newInstance(key));
		    }
		}
		interp.setResult(tobj);
		break;
	    }
	    case OPT_NEXTELEMENT: {

                if (argv.length != 4) {
		    throw new TclNumArgsException(interp, 1, argv, 
		            "nextelement arrayName searchId");
	        }
	        if (notArray) {
	            errorNotArray(interp, argv[2].toString());
	        }

	        if (var.sidVec == null) {
	            errorIllegalSearchId(interp, argv[2].toString(),
                            argv[3].toString());
	        }

	        Enumeration e = var.getSearch(argv[3].toString());
	        if (e == null) {
	            errorIllegalSearchId(interp, argv[2].toString(),
                            argv[3].toString());
	        }
	        if (e.hasMoreElements()) {
		    Hashtable table = (Hashtable)var.value;
 	            String key = (String)e.nextElement();
		    Var elem = (Var)table.get(key);

		    if ((elem.flags & Var.UNDEFINED) == 0) {
		        interp.setResult(key);
		    } else {
		        interp.setResult("");
		    }
		}
		break;
	    }
	    case OPT_SET: {

                if (argv.length != 4) {
		    throw new TclNumArgsException(interp, 1, argv, 
		            "set arrayName list");
	        }
	        int size = TclList.getLength(interp, argv[3]);
	        if ( size%2 != 0 ) {
	            throw new TclException(interp, 
                            "list must have an even number of elements");
	        }
	    
		int i;
		String name1 = argv[2].toString();
		String name2, strValue;

		/*
		 * Set each of the array variable names in the interp
		 */

		for ( i=0; i<size; i++) {
		    name2 = TclList.index(interp, argv[3], i++).toString();
	            strValue = TclList.index(interp, argv[3], i).toString();
		    interp.setVar(name1, name2, 
                            TclString.newInstance(strValue), 0); 
	        }
		break;
	    }
	    case OPT_SIZE: {

	        if (argv.length != 3) {
		    throw new TclNumArgsException(interp, 1, argv, 
		            "size arrayName");
	        }
	        if (notArray) {
		    interp.setResult(0);
	        } else {
		    Hashtable table = (Hashtable)var.value;
		    int size = 0;
		    for (Enumeration e = table.keys(); e.hasMoreElements(); ) {
			Var elem = (Var)table.get((String)e.nextElement());
			if ((elem.flags & Var.UNDEFINED) == 0) {
			    size++;
			}
		    }
	            interp.setResult(size);
	        }
		break;
	    }
	    case OPT_STARTSEARCH: {

                if (argv.length != 3) {
		    throw new TclNumArgsException(interp, 1, argv, 
		            "startsearch arrayName");
	        }
	        if (notArray) {
	            errorNotArray(interp, argv[2].toString());
	        }
	    
		if (var.sidVec == null) {
	            var.sidVec = new Vector();
		}

		/*
		 * Create a SearchId Object:
		 * To create a new SearchId object, a unique string
		 * identifier needs to be composed and we need to
		 * create an Enumeration of the array keys.  The
		 * unique string identifier is created from three
		 * strings:
		 *
		 *     "s-"   is the default prefix
		 *     "i"    is a unique number that is 1+ the greatest
		 *	      SearchId index currently on the ArrayVar.
		 *     "name" is the name of the array
		 *
		 * Once the SearchId string is created we construct a
		 * new SearchId object using the string and the
		 * Enumeration.  From now on the string is used to
		 * uniquely identify the SearchId object.
		 */

		int i = var.getNextIndex();
		String s = "s-" + i  + "-" + argv[2].toString();
		Enumeration e = ((Hashtable)var.value).keys();
		var.sidVec.addElement(new SearchId(e,s,i));
		interp.setResult(s);
		break;
	    }
	}
    }

    /**
     * Error meassage thrown when an invalid identifier is used
     * to access an array.
     *
     * @param interp currrent interpreter.
     * @param String var is the string representation of the 
     *     variable that was passed in.
     */

    private static void errorNotArray(Interp interp, String var)
            throws TclException {
        throw new TclException(interp, "\"" + var + "\" isn't an array");
    }


    /**
     * Error message thrown when an invalid SearchId is used.  The 
     * string used to reference the SearchId is parced to determine
     * the reason for the failure. 
     * 
     * @param interp currrent interpreter.
     * @param String sid is the string represenation of the 
     *     SearchId that was passed in.
     */

    static void errorIllegalSearchId(Interp interp, String varName, 
	    String sid) throws TclException {  

       int val = validSearchId(sid.toCharArray(), varName);

       if (val == 1) {
	   throw new TclException(interp, "couldn't find search \"" +
		    sid + "\"");
       } else if (val == 0) {
	   throw new TclException(interp, "illegal search identifier \"" +
		    sid + "\"");
       } else {
	   throw new TclException(interp, "search identifier \"" + sid +
                    "\" isn't for variable \"" + varName +"\"");
       }
    }

    /**
     * A valid SearchId is represented by the format s-#-arrayName.  If
     * the SearchId string does not match this format than it is illegal,
     * else we cannot find it.  This method is used by the 
     * ErrorIllegalSearchId method to determine the type of error message.
     *
     * @param char pattern[] is the string use dto identify the SearchId
     * @return 1 if its a valid searchID; 0 if it is not a valid searchId, 
     * but it is for the array, -1 if it is not a valid searchId and NOT 
     * for the array.
     */

    private static int validSearchId(char pattern[], String varName) {
        int i;

        if ((pattern[0] != 's') || (pattern[1] != '-') || 
	        (pattern[2]<'0') || (pattern[2]>'9')) {
	    return 0;
	}
    	for (i = 3; ( i < pattern.length && pattern[i] != '-'); i++) {
	  if(pattern[i]<'0' || pattern[i]>'9') {
	    return 0;
	  }
	}
	if (++i >= pattern.length) {
	    return 0;
	}
	if ( varName.equals(new String(pattern, i, (pattern.length-i)))) {
	    return 1;
	} else {
	    return -1;

	}
    }
}

