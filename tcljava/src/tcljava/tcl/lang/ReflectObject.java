/*
 * ReflectObject.java --
 *
 *	Implements the Tcl internal representation of Java
 *	reflection object.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: ReflectObject.java,v 1.2 1999/03/02 15:37:01 hylands Exp $
 *
 */

package tcl.lang;

import java.lang.reflect.*;
import java.util.*;
import java.beans.*;

/*
 * A ReflectObject is used to create and access arbitrary Java objects
 * using the Java Reflection API. It wraps around a Java object (i.e.,
 * an instance of any Java class) and expose it to Tcl scripts. The
 * object is registered inside the interpreter and is given a string
 * name. Tcl scripts can manipulate this object as long as the the
 * reference count of the object is greater than zero.
 */

public class ReflectObject extends InternalRep implements CommandWithDispose {

//The java.lang.Object wrapped by the ReflectObject representation.

Object javaObj;
Class  javaClass;

/*
 * The interpreter in which the java.lang.Object is registered in.
 * ReflectObject's are not shared among interpreters for safety
 * reasons.
 */

Interp ownerInterp;

/*
 * The reference ID of this object.
 */

String refID;

/*
 * This variables records how many TclObject's are using
 * this ReflectObject internal rep. In this example:
 *
 *     set x [new java.lang.Integer 1]
 *     set y [format %s $x]
 *     java::info methods $y
 *
 * The two objects $x and $y share the same ReflectObject instance.
 * useCount is 2 when the java::info command has just translated the
 * string $y into a ReflectObject.
 *
 * useCount is initially 1. It will be more than 1 only when the
 * script tries to refer to the object using its string form, or when
 * the same object is returned by the Reflection API more than once.
 *
 * This variable is called useCount rather than refCount to avoid
 * confusion with TclObject.refCount.
 */

private int useCount;

/*
 * This variable marks whether the object is still considered "valid"
 * in Tcl scripts. An object is no longer valid if its object command
 * has been explicitly deleted from the interpreter.
 */

private boolean isValid;

/*
 * Stores the bindings of this ReflectObject. This member variable is used
 * in the BeanEventMgr class.
 */

Hashtable bindings;


//the string representation of the null reflect object

protected static final String NULL_REP = JavaNullCmd.getNullString();

protected static final String NOCONVERT = "-noconvert";


// Private helper for creating reflected null objects

private static ReflectObject makeNullObject(Interp i) {
    ReflectObject ro = new ReflectObject();

    ro.ownerInterp = i;

    ro.refID = NULL_REP;
    ro.useCount = 1;
    ro.isValid = true;

    ro.javaObj = null;
    ro.javaClass = null;

    return ro;
}
// Private helper used to add a reflect object to the reflect table

private static void addToReflectTable(ReflectObject roRep)
{
    Interp interp = roRep.ownerInterp;
    Class cl = roRep.javaClass;
    Object obj = roRep.javaObj;
    String id = roRep.refID;


    // now we hash a string combination of the class and the identity hash code
    // so that we get a unique string for this pairing of {Class Object}.
    
    StringBuffer ident_buff = new StringBuffer();
    ident_buff.append(JavaInfoCmd.getNameFromClass(cl));
    ident_buff.append('.');
    ident_buff.append(System.identityHashCode(obj));
    String ident = ident_buff.toString();
    
    ReflectObject found = (ReflectObject) interp.reflectObjTable.get(ident);
    
    if (found == null) {
	// there is no reflect object for this java object, just add
	// a single reflect object to the reflectObjTable in the interp
		
	interp.reflectObjTable.put(ident, roRep);
    } else {
	// This should never happen
		
	throw new TclRuntimeError("reflectObjectTable returned null for " + id);
    }
}


// Private helper used to remove a reflected object from the reflect table.

private static void removeFromReflectTable(ReflectObject roRep)
{
  Interp interp = roRep.ownerInterp;
  Class cl = roRep.javaClass;
  Object obj = roRep.javaObj;
  String id = roRep.refID;

  // now we hash a string combination of the class and the identity hash code
  // so that we get a unique string for this pairing of {Class Object}.

  StringBuffer ident_buff = new StringBuffer();
  ident_buff.append(JavaInfoCmd.getNameFromClass(cl));
  ident_buff.append('.');
  ident_buff.append(System.identityHashCode(obj));
  String ident = ident_buff.toString();

  ReflectObject found = (ReflectObject) interp.reflectObjTable.get(ident);

  // This should never happen
  if (found == null) {
      //dump(interp);

      throw new TclRuntimeError("reflectObjectTable returned null for " + id);
  } else {

      // Sanity check
      
      if (found != roRep) {
	  throw new TclRuntimeError("reflect object did not match object in table");
      }

      interp.reflectObjTable.remove(ident);
  }
}



// Find in ReflectTable will search the reflect table for a given
// {Class Object} pair. If the pair exists its ReflectObject
// will be returned. If not, null will be returned.
    
private static ReflectObject findInReflectTable(Interp interp, Class cl, Object obj)
{
    // now we hash a string combination of the class and the identity hash code
    // so that we get a unique string for this pairing of {Class Object}.

    StringBuffer ident_buff = new StringBuffer();
    ident_buff.append(JavaInfoCmd.getNameFromClass(cl));
    ident_buff.append('.');
    ident_buff.append(System.identityHashCode(obj));
    String ident = ident_buff.toString();
    
    ReflectObject found = (ReflectObject) interp.reflectObjTable.get(ident);
    
    if (found == null) {
	return null;
    } else {

	// Sanity check

	if (found.javaClass != cl || found.javaObj != obj || found.ownerInterp != interp) {
	    throw new TclRuntimeError("table entry did not match arguments");
	}

	return found;
    }
}



/*
 *----------------------------------------------------------------------
 *
 * makeReflectObject --
 *
 *	Wraps an Java Object in a ReflectObject. If the same Java
 *	Object has already been wrapped in a ReflectObject, return
 *	that ReflectObject. Otherwise, create a new ReflectObject to
 *	wrap the Java Object.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The object is unregistered (and thus no longer accessible from
 *	Tcl scripts) if no other if no other TclObjects are
 *	still using this internal rep.
 *
 *----------------------------------------------------------------------
 */

private static ReflectObject
makeReflectObject(
    Interp interp,
    Class  cl,
    Object obj)
  throws TclException //if a null class with a non null object is passed in
{

    if (obj == null) {
        //this is the null reflect object case, just call initReflectObject
        //with no hashtable and no key

        //System.out.println("null object");

        return makeNullObject(interp);
    }

    if (cl == null) {
        //we have no way to deal with a non null object that has a
        //null class reference type, we must give up in this case

        throw new TclException(interp,"non null reflect object with null class is not valid");
    }
    
    
    //apply builtin type conversion rules (so int becomes java.lang.Integer)

    if (cl == Integer.TYPE)
      cl = Integer.class;
    else if (cl == Boolean.TYPE)
      cl = Boolean.class;
    else if (cl == Long.TYPE)
      cl = Long.class;
    else if (cl == Float.TYPE)
      cl = Float.class;
    else if (cl == Double.TYPE)
      cl = Double.class;
    else if (cl == Byte.TYPE)
      cl = Byte.class;
    else if (cl == Short.TYPE)
      cl = Short.class;
    else if (cl == Character.TYPE)
      cl = Character.class;    
    else if (cl == Void.TYPE)
      throw new TclException(interp,"void object type can not be reflected");

    // Try to find this {Class Object} pair in the reflect table.
    
    ReflectObject roRep = findInReflectTable(interp, cl, obj);

    if (roRep != null) {
	// If it is already in the table just increment the use count
	roRep.useCount++;
	return roRep;
    } else {
	if (cl.isArray()) {
	    roRep = new ArrayObject();
	} else {
	    roRep = new ReflectObject();
	}

	roRep.ownerInterp = interp;
	roRep.javaObj = obj;
	roRep.javaClass = cl;
    
	// make sure the object can be represented by the given Class
	Class obj_class = roRep.javaObj.getClass();
    
	if (! roRep.javaClass.isAssignableFrom(obj_class)) {
	    throw new TclException(interp,"object of type " +
	        JavaInfoCmd.getNameFromClass(obj_class) +
	        " can not be referenced as type " +
	        JavaInfoCmd.getNameFromClass(roRep.javaClass));
	}

	// Register the object in the interp.
    
	interp.reflectObjCount++; // incr id, the first id used will be 1
	roRep.refID = "java0x" + Long.toHexString(interp.reflectObjCount);
	
	interp.createCommand(roRep.refID,roRep);
	interp.reflectIDTable.put(roRep.refID,roRep);
	
	addToReflectTable(roRep);
	
	roRep.useCount = 1;
	roRep.isValid  = true;	
		
	return roRep;
    }
}


/*
 *----------------------------------------------------------------------
 *
 * dispose --
 *
 *	Called when a TclObject no longers uses this internal rep. We
 *	unregister the java.lang.Object if no other TclObjects are
 *	still using this internal rep.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The object is unregistered (and thus no longer accessible from
 *	Tcl scripts) if no other if no other TclObjects are
 *	still using this internal rep.
 *
 *----------------------------------------------------------------------
 */

protected void
dispose()
{
    useCount--;
    if ((useCount == 0) && (refID != NULL_REP)) {
        //No TclObject is using this internal rep anymore. Free it.

        ownerInterp.deleteCommand(refID);

        ownerInterp.reflectIDTable.remove(refID);

	removeFromReflectTable(this);

	ownerInterp = null;
	javaObj = null;
	javaClass = null;
	bindings = null;
	refID = NULL_REP;
    }
}



//helper function for dispose, it will clean up reprences inside
//the interp.reflectObjTable which currently holds either a
//ReflectObject or a table of reflect objects

private void
disposeOfTableRefrences() {

  Object refOrTable = ownerInterp.reflectObjTable.get(javaObj);

  if (refOrTable instanceof ReflectObject) {
      //this is the easy case we just need to remove the key
      //that maps the java object to this reflect object

      //System.out.println("remove object");

      ownerInterp.reflectObjTable.remove(javaObj);
  } else {
      //in this case the main table hashed to a hash table
      //so we need remove the current ref type from the hashtable

      Hashtable h = (Hashtable) refOrTable;

      //System.out.println("remove typetable entry " + javaClass);

      //remove the entry for the current refrencing class
      h.remove(javaClass);

      //now if there is only one key left in the hashtable
      //we can remap the original table so that it maps
      //the java object to the ReflectObject

      if (h.size() == 1) {
          //get the only value out of the table
	  Object value = h.elements().nextElement();

          //put the original single mapping back into the
	  //interp reflect table which also frees up the
	  //reftype hashtable for garbage collection
      
          ownerInterp.reflectObjTable.put(javaObj,value);

	  //System.out.println("remove typetable");
      }
  }
}



/*
 *----------------------------------------------------------------------
 *
 * duplicate --
 *
 *	Get a copy of this ReflectObject for copy-on-write
 *	operations. We just increment its useCount and return the same
 *	ReflectObject because ReflectObject's cannot be modified, so
 *	they don't need copy-on-write protections.
 *
 * Results:
 *	The same internal rep.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

protected InternalRep
duplicate()
{
    useCount++;
    return this;
}

/*
 *----------------------------------------------------------------------
 *
 * setReflectObjectFromAny --
 *
 *	Called to convert an TclObject's internal rep to ReflectObject.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	When successful, the internal representation of tobj is
 *	changed to ReflectObject, if it is not already so.
 *
 *----------------------------------------------------------------------
 */

private static void
setReflectObjectFromAny(
    Interp interp,		// Current interpreter. Must be non-null.
    TclObject tobj)		// The TclObject to convert.
throws
    TclException		// If the object's internal rep is not
				// already ReflectObject, and the string rep
				// is not the name of a java.lang.Object
				// registered in the given interpreter.
				// Error message is left inside interp.
{
    InternalRep rep = tobj.getInternalRep();
    ReflectObject roRep;

    if (rep instanceof ReflectObject) {
	roRep = (ReflectObject) rep;
	if (roRep.isValid && (roRep.ownerInterp == interp)) {
	    return;
	}
    }

    String s = tobj.toString();
      if (s.startsWith("java")) {
	if (s.equals(NULL_REP)) {
	  tobj.setInternalRep(makeReflectObject(interp,null,null));
	  return;
	} else {
	  roRep = (ReflectObject) interp.reflectIDTable.get(s);
	  if ((roRep != null) && (roRep.isValid)) {
	    roRep.useCount++;
	    tobj.setInternalRep(roRep);
	    return;
	  }
	}
      }

    throw new TclException(interp, "unknown java object \"" + 
	    tobj + "\"");
}

/*
 *----------------------------------------------------------------------
 *
 * newInstance --
 *
 *	Creates a new instance of a TclObject that wraps a
 *	java.lang.Object.
 *
 * Results:
 *	The newly created TclObject.
 *
 * Side effects:
 *	The java.lang.Object will be registered in the interpreter.
 *
 *----------------------------------------------------------------------
 */

public static TclObject
newInstance(
    Interp interp,		// Current interpreter.
    Class cl,                   // class of the reflect instance
    Object obj)			// java.lang.Object to wrap.
throws TclException
{
    return new TclObject(makeReflectObject(interp, cl, obj));
}

/*
 *----------------------------------------------------------------------
 *
 * get --
 *
 *	Returns a java.lang.Object represented by tobj. tobj must have a
 *	ReflectObject internal rep, or its string rep must be one of the
 *	currently registered objects.
 *
 * Results:
 *	The Java object represented by tobj.
 *
 * Side effects:
 *	When successful, the internal representation of tobj is
 *	changed to ReflectObject, if it is not already so.
 *
 *----------------------------------------------------------------------
 */

public static Object
get(
    Interp interp,		// Current interpreter. Must be non-null.
    TclObject tobj)		// The TclObject to query.
throws
    TclException		// If the internal rep of tobj cannot
				// be converted to a ReflectObject. 
				// Error message is left inside interp.
{
    setReflectObjectFromAny(interp, tobj);
    ReflectObject rep = (ReflectObject)(tobj.getInternalRep());
    return rep.javaObj;
}


/*
 *----------------------------------------------------------------------
 *
 * getClass --
 *
 *	Returns a java.lang.Class object that is the ref type of this
 *      reflect object. This is not always the same class as is returned
 *      by a call to ((Object) o).getClass().
 *
 * Results:
 *	The Java class object used to reference tobj.
 *
 * Side effects:
 *	When successful, the internal representation of tobj is
 *	changed to ReflectObject, if it is not already so.
 *
 *----------------------------------------------------------------------
 */

public static Class
getClass(
    Interp interp,		// Current interpreter. Must be non-null.
    TclObject tobj)		// The TclObject to query.
throws
    TclException		// If the internal rep of tobj cannot
				// be converted to a ReflectObject. 
				// Error message is left inside interp.
{
    setReflectObjectFromAny(interp, tobj);
    ReflectObject rep = (ReflectObject)(tobj.getInternalRep());
    return rep.javaClass;
}


/*
 *----------------------------------------------------------------------
 *
 * getReflectObject --
 *
 *	Returns the InternalRep of a the ReflectObject represented by
 *	tobj. Only the java:: commands should call this
 *	method. (java::bind, java::call, etc).
 *
 * Results:
 *	The Java object represented by tobj.
 *
 * Side effects:
 *	When successful, the internal representation of tobj is
 *	changed to ReflectObject, if it is not already so.
 *
 *----------------------------------------------------------------------
 */

static ReflectObject
getReflectObject(
    Interp interp,		// Current interpreter. Must be non-null
    TclObject tobj)		// The TclObject to query.
throws
    TclException		// If the internal rep of tobj cannot
				// be converted to a ReflectObject.
				// Error message is left inside interp.
{
    setReflectObjectFromAny(interp, tobj);
    return (ReflectObject)(tobj.getInternalRep());
}

/*
 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This cmdProc implements the Tcl command used to invoke methods
 *	of the java.lang.Object stored in this ReflectObject internal
 *	rep. For example, this method is called to process the "$v"
 *	command at the second line of this script:
 *
 *	    set v [java::new java.util.Vector]
 *	    $v addElement "foo"
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	If the given method returns a value, it is converted into a
 *	TclObject and stored as the result of the interpreter.
 *
 *----------------------------------------------------------------------
 */

public void
cmdProc(
    Interp interp,			// Current interpreter.
    TclObject argv[])			// Argument list.
throws
    TclException			// Standard Tcl exception;
{
    boolean convert;
    int sigIdx;

    if (argv.length < 2) {
	throw new TclNumArgsException(interp, 1, argv, 
		"?-noconvert? signature ?arg arg ...?");
    }

    String arg1 = argv[1].toString();
    if ((arg1.length() >= 2) && (NOCONVERT.startsWith(arg1))) {
	convert = false;
	sigIdx = 2;
    } else {
	convert = true;
	sigIdx = 1;
    }

    if (argv.length < sigIdx + 1) {
	throw new TclNumArgsException(interp, 1, argv, 
		"?-noconvert? signature ?arg arg ...?");
    }

    int startIdx = sigIdx + 1;
    int count = argv.length - startIdx;

    interp.setResult(JavaInvoke.callMethod(interp, argv[0],
	    argv[sigIdx], argv, startIdx, count, convert));
}

/*
 *----------------------------------------------------------------------
 *
 * disposeCmd --
 *
 * 	This method is called when the object command has been deleted
 * 	from an interpreter. It marks the ReflectObject no longer
 * 	accessible from Tcl scripts.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The ReflectObject is no longer accessible from Tcl scripts.
 *
 *----------------------------------------------------------------------
 */

public void
disposeCmd()
{
    isValid = false;
}

/*
 *----------------------------------------------------------------------
 *
 * toString --
 *
 *	Called to query the string representation of the Tcl
 *	object. This method is called only by TclObject.toString()
 *	when TclObject.stringRep is null.
 *
 * Results:
 * 	Returns the string representation of this ReflectObject.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public String
toString()
{
  //return "java0x" + Integer.toHexString(refID);
  return refID;
}

/*
 *----------------------------------------------------------------------
 *
 * init --
 *
 *	Initializes fields inside the Interp that are used by
 *	ReflectObject's.
 *
 * Results:
 * 	None.
 *
 * Side effects:
 *	The fields are initialized.
 *
 *----------------------------------------------------------------------
 */

static void
init(
    Interp interp)
{
    interp.reflectIDTable = new Hashtable();
    interp.reflectObjTable = new Hashtable();
    interp.reflectObjCount = 0;
}

} // end ReflectObject

