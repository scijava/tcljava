/*
 * JavaImportCmd.java --
 *
 *	This class implements the java::import command which is used
 *	to indicate which classes will be imported. An imported class
 *	simply means that we can use the class name insteead of the
 *	full pkg.class name.
 *
 * Copyright (c) 1999 Mo DeJong.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: JavaImportCmd.java,v 1.2 2000/01/10 23:43:18 mo Exp $
 *
 */

package tcl.lang;

import java.util.*;


public class JavaImportCmd implements Command {

    /*
     *----------------------------------------------------------------------
     *
     * cmdProc --
     *
     *	This procedure is invoked to process the "java::import" Tcl
     *	comamnd.  See the user documentation for details on what
     *	it does.
     *
     * Results:
     *	None.
     *
     * Side effects:
     *	A standard Tcl result is stored in the interpreter.
     *
     *----------------------------------------------------------------------
     */

    public void
	cmdProc(
		Interp interp,			// Current interpreter.
		TclObject[] objv)		// Argument list.
	throws
	    TclException			// A standard Tcl exception.
    {
	final String usage = "java::import ?-forget? ?-package pkg? ?class ...?";

	Hashtable classTable = interp.importTable[0];
	Hashtable packageTable = interp.importTable[1];

	boolean forget = false;
	String pkg = null;
	TclObject import_list;
	Enumeration search, search2;
	String elem, elem2;
	int startIdx, i;

	// If there are no args simply return all the imported classes
	if (objv.length == 1) {
	    import_list = TclList.newInstance();

	    for (search = classTable.keys(); search.hasMoreElements() ; ) {
		elem = (String) search.nextElement();
		TclList.append(interp, import_list,
		    TclString.newInstance(
		        (String) classTable.get(elem)));
	    }
	    interp.setResult(import_list);
	    return;
	}
	
	// See if there is a -forget argument
	startIdx = 1;
	elem = objv[startIdx].toString();
	if (elem.equals("-forget")) {
	    forget = true;
	    startIdx++;
	}

	// When -forget is given with no arguments, we simply
	// return. This is needed to support the following usage.
	//
	// eval {java::import -forget} [java::import]
	//
	// This happens when java::import returns the empty list

	if (startIdx >= objv.length) {
	    interp.resetResult();
	    return;
	}


	// Figure out if the "-package pkg" arguments are given
	elem = objv[startIdx].toString();
	if (elem.equals("-package")) {
	    startIdx++;

	    // "java::import -package" is not a valid usage
	    // "java::import -forget -package" is not a valid usage
	    if (startIdx >= objv.length) {
		throw new TclException(interp, usage);
	    }
	    
	    pkg = objv[startIdx].toString();
	    if (pkg.length() == 0) {
		throw new TclException(interp, usage);
	    }
	    startIdx++;
	}


	// No additional arguments means we have hit one of 
	// two conditions.
	//
	// "java::import -forget -package pkg"
	// "java::import -package pkg"

	if (startIdx >= objv.length) {
	    if (forget) {
		// We must have "java::import -forget -package pkg"

		// Ceck that it is not "java::import -forget" which is invalid!
		if (pkg == null) {
		    throw new TclException(interp, usage);
		}

		// forget each member of the given package

		boolean found = false;

		for (search = packageTable.keys(); search.hasMoreElements() ; ) {
		    elem = (String) search.nextElement();

		    if (elem.equals(pkg)) {
			// This is the package we are looking for, remove it!
			if (found) {
			    throw new TclRuntimeError("unexpected : found == true");
			}
			found = true;

			// Loop over each class imported from this package
			for (search2 =
				 ((Vector) packageTable.get(elem)).elements();
			     search2.hasMoreElements(); )
			{
			    // Remove imported class from the classTable
			    elem2 = (String) search2.nextElement();
			    classTable.remove(elem2);
			}

			// Remove the package entry
			packageTable.remove(elem);
		    }
		}

		if (!found) {
		    // It is an error to forget a package that
		    // does not have any classes imported from it

		    throw new TclException(interp,
		        "can not forget package \"" + pkg +
			"\", no classes were imported from it");
		}
		
		interp.resetResult();
		return;
	    } else {
		if (pkg == null) {
		    throw new TclRuntimeError(
			      "unexpected : pkg == null");
		}

		// "java::import -package pkg" should return each imported
		// class in the given package

		for (search = packageTable.keys(); search.hasMoreElements() ; ) {
		    elem = (String) search.nextElement();

		    if (elem.equals(pkg)) {
			// This is the package we are looking for.

			import_list = TclList.newInstance();

			// Loop over each class imported from this package
			for (search2 =
				 ((Vector) packageTable.get(elem)).elements();
			     search2.hasMoreElements(); )
			{
			    elem2 = (String) search2.nextElement();

			    TclList.append(interp, import_list,
			        TclString.newInstance(
				    (String) classTable.get(elem2)));
			}
			    
			interp.setResult(import_list);
			return;
		    }
		}

		// If we got this far then we have not imported
		// any classes from the given package, just return

		interp.resetResult();
		return;
	    }
	}


	// Keep track of the classes we will import and forget about

	Vector importClasses = new Vector();
	Vector forgetClasses = new Vector();

	// Get the operation type string to be used in error messages
	String operation = "import";
	if (forget) {
	    operation = "forget";
	}

	TclClassLoader tclClassLoader = new TclClassLoader(interp, null);


	// Start processing class arguments begining at startIdx.

	for (i = startIdx; i < objv.length ; i++) {
	    elem = objv[i].toString();

	    // Check that the class name is not "" or "-forget" or "-package"
	    if ((elem.length() == 0) || elem.equals("-forget")
		|| elem.equals("-package")) {
		throw new TclException(interp, usage);
	    }

	    // Check that the class name does not have the '.'
	    // char in it if the package argument was given

	    if (pkg != null) {
		if (elem.indexOf('.') != -1) {
		    throw new TclException(interp,
			      "class argument must not contain a package specifier" +
			      " when the -package pkg arguments are given");
		}
	    }

	    // Make sure the class is not a primitive type
	    if (elem.equals("int") ||
		elem.equals("boolean") ||
		elem.equals("long") ||
		elem.equals("float") ||
		elem.equals("double") ||
		elem.equals("byte") ||
		elem.equals("short") ||
		elem.equals("char")) {
		throw new TclException(interp,
			  "can not " + operation +
			  " primitive type \"" + elem + "\"");
	    }

	    // Create fully qualified name by combining -package argument
	    // (if it was given) and the class argument

	    String fullyqualified;

	    if (pkg == null) {
		fullyqualified = elem;
	    } else {
		fullyqualified = pkg + "." + elem;
	    }
	
	    // split the fullyqualified name into a package and class
	    // by looking for the last '.' character in the string.
	    // If there is no '.' in the string the class is in the
	    // global package which is not valid.
	    
	    int ind = fullyqualified.lastIndexOf('.');
	    if (ind == -1) {
		throw new TclException(interp,
		    "can not " + operation +
                    " from global package");
	    }

	    String class_package = fullyqualified.substring(0, ind);
	    String class_name    = fullyqualified.substring(ind+1,
				       fullyqualified.length());
	    
	    // Make sure the class is not in the java.lang package
	    if (class_package.equals("java.lang")) {
		throw new TclException(interp,
		    "can not " + operation + " class \"" +
		    fullyqualified + "\", it is in the java.lang package");
	    }

	    if (!forget) {
		// Make sure class is not in the global package
		// We need to test to see if the class exists only
		// when doing an import because doing a -forget will
		// only work if the class had been imported already

		boolean inGlobal = true;

		try {
		    tclClassLoader.loadClass(class_name);
		} catch (ClassNotFoundException e) {
		    inGlobal = false;
		    tclClassLoader.removeCache(class_name);
		}

		if (inGlobal) {
		    throw new TclException(interp,
		        "can not import \"" + fullyqualified +
		        "\" it conflicts with a class with the same name" +
			" in the global package");
		}

		// Make sure the class can be loaded (using the fully qualified name)

		TclException notfound = new TclException(interp,
		        "can not import class \"" +
			fullyqualified +
			"\", it does not exist");

		try {
		    tclClassLoader.loadClass(fullyqualified);
		} catch (ClassNotFoundException e) {
		    throw notfound;
		} catch (SecurityException e) {
		    // The tcljava loader throws a SecurityException when
		    // a class can not be loaded and it is in any of the
		    // java.* packages or tcl.* packages. We catch that
		    // exception here and treat it like ClassNotFoundException

		    throw notfound;
		}

	    }

	    // When processing a -forget argument, make sure the class
	    // was already imported.

	    if (forget) {
		if (classTable.get(class_name) == null) {
		    throw new TclException(interp,
		        "can not forget class \"" + fullyqualified +
                        "\", it was never imported");
		}
	    }

	    // We now know that everything was ok. Add this class
	    // to the import or export vectors for later processing
	
	    if (forget) {
		forgetClasses.addElement(fullyqualified);
	    } else {
		importClasses.addElement(fullyqualified);
	    }
	}


	// We now process the forgetClasses or the importClasses.
	// Only one of these can contain elements.

	if (forgetClasses.size() != 0 &&
	    importClasses.size() != 0) {
	    throw new TclRuntimeError(
                "unexpected : forgetClasses and importClasses are both nonempty");
	}

	final boolean debug = false;

	if (forgetClasses.size() != 0) {
	    if (debug) {
		System.out.print("now to forget { ");

		for (search = forgetClasses.elements(); search.hasMoreElements() ; ) {
		    System.out.print((String) search.nextElement());
		    System.out.print(" ");
		}

		System.out.println("}");
	    }

	    // Loop through each class we want to forget
	    
	    for (search = forgetClasses.elements(); search.hasMoreElements() ; ) {
		String fullyqualified = (String) search.nextElement();
		int ind = fullyqualified.lastIndexOf('.');
		if (ind == -1) {
		    throw new TclRuntimeError("unexpected : no package in forget class");
		}

		String class_package = fullyqualified.substring(0, ind);
		String class_name    = fullyqualified.substring(ind+1,
				           fullyqualified.length());

		// Hash the class package key to the package vector
		Vector class_vector = (Vector) packageTable.get(class_package);

		// Remove the current class from the vector

		if (! class_vector.removeElement(class_name)) {
		    throw new TclRuntimeError(
		        "unexpected : class not found in package vector");
		}

		// If there are no more classes in the vector, remove the package
		if (class_vector.size() == 0) {
		    packageTable.remove(class_package);
		}

		// Remove the name -> fullyqualified entry
		classTable.remove(class_name);
	    }
	}

	if (importClasses.size() != 0) {
	    if (debug) {
		System.out.print("now to import { ");

		for (search = importClasses.elements(); search.hasMoreElements() ; ) {
		    System.out.print((String) search.nextElement());
		    System.out.print(" ");
		}

		System.out.println("}");
	    }


	    // Loop through each class we want to import
	    
	    for (search = importClasses.elements(); search.hasMoreElements() ; ) {
		String fullyqualified = (String) search.nextElement();
		int ind = fullyqualified.lastIndexOf('.');
		if (ind == -1) {
		    throw new TclRuntimeError("unexpected : no package in import class");
		}

		String class_package = fullyqualified.substring(0, ind);
		String class_name    = fullyqualified.substring(ind+1,
				           fullyqualified.length());

		// If this import already exists, just continue on to the next class
		
		if (classTable.get(class_name) != null) {
		    continue;
		} else {
		    // We are adding a new class import
		    
		    classTable.put(class_name, fullyqualified);
		    
		    // Hash the class package key to the package vector
		    Vector class_vector = (Vector) packageTable.get(class_package);

		    if (class_vector == null) {
			// A new package is being added
			class_vector = new Vector();
			packageTable.put(class_package, class_vector);
		    }

		    // Add the name of the class (not fully qualified) to the vector
		    class_vector.addElement(class_name);
		}
	    }
	}

	interp.resetResult();

	if (debug) {
	    System.out.println("finished with method");
	}

	return;
    }


    /*
    public static void debugPrint(Interp interp) {
	Hashtable classTable   = interp.importTable[0];
	Hashtable packageTable = interp.importTable[1];

	Enumeration search;
	String elem;

	for (search = classTable.keys(); search.hasMoreElements() ; ) {
	    elem = (String) search.nextElement();
	    System.out.println("imported class \"" + elem + "\" -> \"" +
				   classTable.get(elem) + "\"");
	}

	for (search = packageTable.keys(); search.hasMoreElements() ; ) {
	    elem = (String) search.nextElement();
	    System.out.println("package " + elem + " " + packageTable.get(elem));
	}

	System.out.println("Done with debug print");
    }
    */


    /*
     *----------------------------------------------------------------------
     *
     * getImport --
     *
     *	This method is invoked can be invoked to query the import
     *	system to find the fully qualified name of a class.
     *	See the user documentation for details on what it does.
     *
     * Results:
     *	Returns the fully qualified name if it was imported. If the
     *	class was not imported then null will be returned.
     *
     * Side effects:
     *	None.
     *
     *----------------------------------------------------------------------
     */

    public static String
	getImport(
		Interp interp,			// Current interpreter.
		String name)                    // Class name to qualify
    {
	Hashtable classTable   = interp.importTable[0];
	return (String) classTable.get(name);
    }


}
