/* 
 * TclClassLoader.java --
 *
 *	Implements the Class Loader for dynamically loading
 *      Tcl packages.  When attempting to resolve and load a
 *      new Package thr loader looks in four places to find
 *      the class.  In order they are:
 *
 *          1) The unique cache, "classes", inside the TclClassLoader class.
 *          2) Using the system class loader.
 *          3) Any paths passed into the constructor via the pathList variable.
 *          4) Any path in the interps env(TCL_CLASSPATH) variable.
 *
 *      The class will be found if it is any of the above paths
 *      or if it is in a jar file located in one of the paths.
 *
 * TclClassLoader.java --
 *
 *      A class that helps filter directory listings when
 *      for jar/zip files during the class resolution stage.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: TclClassLoader.java,v 1.1 1998/10/14 21:09:15 cvsadmin Exp $
 */


package tcl.lang;
import java.util.*;
import java.util.zip.*;
import java.io.*;

class TclClassLoader extends ClassLoader
{

/*
 * Cache this value because a couple of functions use it.
 */

private static String pathSep = System.getProperty("file.separator");

/*
 * Caches the classes already loaded by this Loader.  It's static
 * so new instances dont load redundant classes.
 */

private static Hashtable classes = new Hashtable();

/*
 * Each instance can have a list of additional paths to search.  This
 * needs to be stored on a per instance basis because classes may be
 * resolved at later times.  classpath is passed into the constructor,
 * and loadpath is extracted from the env(TCL_CLASSPATH) interp variable.
 */

private String[] classpath;
private String[] loadpath;

/*
 *----------------------------------------------------------------------
 *
 * TclClassLoader --
 *
 *	TclClassLoader stores the values to classpath and env(TCL_CLASSPATH)
 *      on a per object basis.  This is necessary because classes 
 *      may not be loaded immediately, but classpath and loadpath
 *      may change over time, or from object to to object.
 *
 *      The list of paths in pathList and env(TCL_CLASSPATH) can be relative
 *      to the current interp dir.  The full path names are resolved,
 *      before they are stored.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Creates a new TclClassLoader object.
 *
 *----------------------------------------------------------------------
 */

TclClassLoader(
    Interp interp,       	// Used to get env(TCL_CLASSPATH) and current
				// working dir
    TclObject pathList)		// List of additional paths to search
{
    TclObject[] elem;
    int i;

    try {
	if (pathList != null) {
	    elem = TclList.getElements(interp, pathList);
	    classpath = new String[elem.length];
	    for (i = 0; i < elem.length; i++) {
		classpath[i] = absolutePath(interp, elem[i].toString());
	    }
	}
	
	if ((elem = getEnvTclClasspath(interp)) != null) {
	    loadpath = new String[elem.length];
	    for (i = 0; i < elem.length; i++) {
		loadpath[i] = absolutePath(interp, elem[i].toString());  
	    }
	}
    } catch (TclException e) {
    }
}

/*
 *----------------------------------------------------------------------
 *
 * loadClass --
 *
 *	Resolves the specified name to a Class. The method loadClass() is 
 *      called by the JavaLoadCmd.
 *
 * Results:
 *	the resolved Class, or null if it was not found.
 *
 * Side effects:
 *	ClassNotFoundException  if the class loader cannot find
 *      a definition for the class.
 *
 *----------------------------------------------------------------------
 */

protected Class
loadClass(
    String className,       // The name of the desired Class.
    boolean resolveIt)      // If true, then resolve all referenced classes.
throws
    ClassNotFoundException, // The class could not be found.
    SecurityException       // Attempt to dynamically load tcl/java package.
{
    Class result;           // The Class that is loaded.             
    byte  classData[];      // The bytes that compose the class file.
    
    /* 
     * Check our local cache of classes 
     */
    
    result = (Class)classes.get(className);
    if (result != null) {
	return result;
    }
    
    /*
     * Check with the primordial class loader 
     */
    
    try {
	result = Class.forName(className);
	return result;
    } catch (ClassNotFoundException e) {
    } catch (IllegalArgumentException e) {
    } catch (NoClassDefFoundError e) {
    } catch (IncompatibleClassChangeError e) {
    }
    
    /*
     * Protect against attempts to load a class that contains the 'java'
     * or 'tcl' prefix, but is not in the corresponding file structure.
     */
    
    if ((className.startsWith("java.")) 
	    || (className.startsWith("tcl."))) {
	throw new SecurityException("Java loader failed to load the class " +
                "and the Tcl Java loader is not permitted to " +
                "load classes in the tcl or java package at runtime, " +
                "check your CLASSPATH.");
    }

    /* 
     * Try to load it from one of the -classpath paths 
     */
    
    classData = getClassFromPath(classpath, className);		
    if (classData == null) {					
	/*							
	 * The class couldnt be found by the system or in one	
	 * of the paths specified by -classpath.  Last attempt	
	 * is to search the env(TCL_CLASSPATH) paths.			
	 */

	classData = getClassFromPath(loadpath, className);	
    }								
    
    if (classData == null) {
	throw new ClassNotFoundException(className);
    }
	
    /*
     * Define it (parse the class file)
     */
    
    result = defineClass(className, classData, 0, classData.length);
    if (result == null) {
	throw new ClassFormatError();
    }
    if (resolveIt) {
	resolveClass(result);
    }

    /*
     * Store it in our local cache
     */
	
    classes.put(className, result);
    return result;
}

/*
 *----------------------------------------------------------------------
 *
 * defineClass --
 *
 *	Given an array of bytes that define a class, create the Class.
 *      If the className is null, we are creating a lambda class.
 *      Otherwise cache the className and definition in the loaders
 *      cache.
 *
 * Results:
 *	A Class object or null if it could not be defined.
 *
 * Side effects:
 *	Cache the Class object in the classes Hashtable.
 *
 *----------------------------------------------------------------------
 */

Class
defineClass(
    String className,         // Name of the class, possibly null.
    byte[] classData)         // Binary data of the class structure.
{
    Class result = null;      // The Class object defined by classData.

    /*
     * Create a class from the array of bytes
     */
    
    try {
	result = defineClass(null, classData, 0, classData.length);
    } catch (ClassFormatError e) {
    }

    if (result != null) {
	/*
	 * If the name of the class is null, extract the className
	 * from the Class object, and use that as the key.
	 */
	
	if (className == null) {
	    className = result.getName();
	}
	
	/*
	 * If a class was created,  then store the class
	 * in the loaders cache.
	 */
	
	classes.put(className, result);
    }
    
    return(result);
}

/*
 *----------------------------------------------------------------------
 *
 * getClassFromPath --
 *
 *	At this point, the class wasn't found in the cache or by the
 *      primordial loader.  Search through 'classpath' list and the 
 *      Tcl environment TCL_CLASSPATH to see if the class file can be 
 *      found and resolved.  If ".jar" or ".zip" files are found, 
 *      search them for the class as well.
 *
 * Results:
 *	an array of bytes that is the content of the className
 *      file.  null is returned if the class could not be 
 *      found or resolved (e.g. permissions error).
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private byte[]
getClassFromPath(
    String[] paths,
    String className)    // the name of the class trying to be resolved
{
    int i = 0;
    byte[] classData = null;       // The bytes that compose the class file.
    String curDir;       	// The directory to search for the class file.
    File file;           	// The class file.                            

    /*
     * Search through the list of "paths" for the className.  
     * ".jar" or ".zip" files found in the path will also be 
     * searched.  Yhe first occurence found is returned.
     */

    if (paths != null) {
	/*
	 * When the class being loaded implements other classes that are
	 * not yet loaded, the TclClassLoader will recursively call this
	 * procedure.  However the format of the class name is 
	 * foo.bar.name and it needs to be foo/bar/name.  Convert to 
	 * proper format.
	 */
	  
	while ((i = className.indexOf(".", i)) != -1) {
	    className = className.substring(0, i) + pathSep +
		    className.substring(i + 1);
	}
	className = className + ".class";
	
	for (i = 0; i < paths.length; i++) {
	    curDir = paths[i].toString();
	    try {
		if ((curDir.endsWith(".jar")) || (curDir.endsWith(".zip"))) {
		    /*
		     * If curDir points to a jar file, search it
		     * for the class.  If classData is not null
		     * then the class was found in the jar file.
		     */
		    
		    classData = extractClassFromJar(curDir, className);
		    if (classData != null) {
			return(classData);
		    }
		} else {		
		    /*
		     * If curDir and className point to an existing file,
		     * then the class is found.  Extract the bytes from 
		     * the file.
		     */
		    
		    file = new File(curDir, className);
		    if (file.exists()) {
			FileInputStream fi = new FileInputStream(file);
			classData = new byte[fi.available()];
			fi.read(classData);
			return(classData);
		    }
		}
	    } catch (Exception e) {
		/*
		 * No error thrown, because the class may be found
		 * in subsequent paths.
		 */
	    }
	}
	for (i = 0; i < paths.length; i++) {
	    curDir = paths[i].toString();
	    try {
		/*
		 * The class was not found in the paths list.
		 * Search all the directories in paths for 
		 * any jar files, in an attempt to locate
		 * the class inside a jar file.
		 */

		classData = getClassFromJar(curDir, className);
		if (classData != null) {
		    return(classData);
		}
	    } catch (Exception e) {
		/*
		 * No error thrown, because the class may be found
		 * in subsequent paths.
		 */
	    }
	}
    }

    /*
     * No matching classes found.
     */

    return null;
}

/*
 *----------------------------------------------------------------------
 *
 * getClassFromJar --
 *
 *	Given a directory and a class to be found, get a list of 
 *      ".jar" or ".zip" files in the current directory.  Call
 *      extractClassFromJar to search the Jar file and extract 
 *      the class if a match is found.
 *
 * Results:
 *	An array of bytes that is the content of the className
 *      file.  null is returned if the class could not be
 *      resolved or found. 
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private byte[]
getClassFromJar(
    String curDir,      // An absoulte path for a directory to search 
    String className)   // The name of the class to extract from the jar file.
throws IOException
{
    byte[] result = null;         // The bytes that compose the class file.
    String[] jarFiles;            // The list of files in the curDir.     
    JarFilenameFilter jarFilter;  // Filter the jarFiles list by only     
                                  // accepting ".jar" or ".zip"
    
    jarFilter = new JarFilenameFilter();
    jarFiles = (new File(curDir)).list(jarFilter);

    for (int i = 0; i < jarFiles.length; i++) {
	result = extractClassFromJar(
	    curDir + pathSep + jarFiles[i], className);
	if (result != null) {
	    break;
	}
    }
    return(result);
}


/*
 *----------------------------------------------------------------------
 *
 * extractClassFromJar --
 *
 *	Look inside the jar file, jarName, for a ZipEntry that
 *      matches the className.  If a match is found extract the
 *      bytes from the input stream.
 *
 * Results:
 *	An array of bytes that is the content of the className
 *      file.  null is returned if the class could not be
 *      resolved or found.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private byte[]
extractClassFromJar(
    String jarName,     // An absoulte path for a jar file to search.
    String className)   // The name of the class to extract from the jar file.
throws IOException
{
    ZipInputStream zin;        // The jar file input stream.
    ZipEntry       entry;      // A file contained in the jar file.
    byte[]         result;     // The bytes that compose the class file.
    int            size;       // Uncompressed size of the class file.
    int            total;      // Number of bytes read from class file.

    zin = new ZipInputStream(new FileInputStream(jarName));

    try {
	while ((entry = zin.getNextEntry()) != null) { 
	    /*
	     * see if the current ZipEntry's name equals 
	     * the file we want to extract.  If equal
	     * get the extract and return.
	     */
	      
	    if (className.equals(entry.getName())) {
		size = getEntrySize(jarName, className);
		result = new byte[size];
		total = zin.read(result);
		while (total != size) {
		    total += zin.read(result, total, 
			    (size - total));
		}
		return result;
	    }
	}
	return(null);
    } finally {
	zin.close();
    }
}


/*
 *----------------------------------------------------------------------
 *
 * getEntrySize --
 *
 *	For some reason, using ZipInputStreams, the ZipEntry returned
 *      by getNextEntry() dosen't contain a valid uncompressed size, so
 *      there is no way to determine how much to read.  Using the 
 *      ZipFile object will return useful values for the size, but
 *      the inputStream returned dosent work.  The solution was to use
 *      both methods to ultimtely extract the class, which results in 
 *      an order n^2 algorithm.  Hopefully this will change...
 *
 * Results:
 *	 The size of the uncompressed class file.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private int
getEntrySize(
    String jarName,
    String className) 
throws IOException
{
    ZipInputStream zin;        // The jar file input stream.           
    ZipEntry       entry;      // A file contained in the jar file.    
    ZipFile        zip;        // Used to get the enum of ZipEntries.  
    Enumeration    enum;       // List of the contents of the jar file.

    zip = new ZipFile(jarName);
    enum = zip.entries();

    while (enum.hasMoreElements()) { 
	/*
	 * see if the current ZipEntry's
	 * name equals the file we want to extract.
	 */
	  
	entry = (ZipEntry)enum.nextElement();
	if (className.equals(entry.getName())) {
	    zip.close();
	    return((int)entry.getSize());
	}
    }
    return(-1);
}

/*
 *----------------------------------------------------------------------
 *
 * getEnvTclClasspath --
 *
 *	Converts the env(TCL_CLASSPATH) to a list and returns an array
 *      of TclObjects.
 *
 * Results:
 *	A list of all the paths in env(TCL_CLASSPATH)
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private static TclObject[]
getEnvTclClasspath(
    Interp interp) 
throws TclException
{
    TclObject tobj = interp.getVar("env", "TCL_CLASSPATH",
             TCL.DONT_THROW_EXCEPTION | TCL.GLOBAL_ONLY);
    if (tobj != null) {
	return(TclList.getElements(interp, tobj));
    }
    return(null);
}

/*
 *----------------------------------------------------------------------
 *
 * absolutePath --
 *
 *	Given a String, construct a File object.  If it is not an absoulte
 *      path, then prepend the interps current working directory, to the
 *      dirName.  
 *
 * Results:
 *	The absolute path of dirName
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private static String
absolutePath(
    Interp interp,    // the current Interp
    String dirName)   // name of directory to be qualified
throws
    TclException
{
    File dir;
    String newName;

    dir = new File(dirName);
    if (!dir.isAbsolute()) {
	newName = interp.getWorkingDir().toString() +
	    System.getProperty("file.separator") + dirName;
	dir = new File(newName);
    }
    return(dir.toString());
}

/*
 *----------------------------------------------------------------------
 *
 * removeCache --
 *
 *	|>description<|
 *
 * Results:
 *	|>None.<|
 *
 * Side effects:
 *	|>None.<|
 *
 *----------------------------------------------------------------------
 */

void
removeCache(
    String key)
{
    classes.remove(key);
}

} // end TclClassLoader


/*
 *
 * TclClassLoader.java --
 *
 *      A class that helps filter directory listings when
 *      for jar/zip files during the class resolution stage.
 *
 */

class JarFilenameFilter implements FilenameFilter {


/*
 *----------------------------------------------------------------------
 *
 * accept --
 *
 *	Used by the getClassFromJar method.  When list returns a list
 *      of files in a directory, the list will only be of jar or zip
 *      files.
 *
 * Results:
 *	True if the file ends with .jar or .zip
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */
    
public boolean
accept(
    File dir,
    String name)
{
    if (name.endsWith(".jar") || name.endsWith(".zip")) {
	return(true);
    } else {
	return(false);
    }
}

} // end JarFilenameFilter


