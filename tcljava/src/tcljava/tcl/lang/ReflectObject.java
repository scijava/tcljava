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
 * RCS: @(#) $Id: ReflectObject.java,v 1.1 1998/10/14 21:09:14 cvsadmin Exp $
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

        return initReflectObject(interp,null,null,null,null);
    }

    if (cl == null) {
        //we have no way to deal with a non null object that has a
        //null class reference type, we must give up in this case

        throw new TclException(interp,"non null reflect object with null class is not valid");
    }
    
    
    //apply builtin type conversion rules

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



    ReflectObject roRep = null;

    //now we hash the object id to find the ReflectObject or 
    //ReflectObjects that represent this object. The case where
    //a single object is referenced as only one type will be common
    //so the object we hash to can be either a ReflectObject or a
    //Hashtable. At runtime we determine which one it is. While
    //this may take a little longer it will save a lot of space
    //because an object will not need to have a hashtable
    //allocated to it unless it is getting referenced multiple
    //time and by different object reference types.
    
    Object refOrTable = interp.reflectObjTable.get(obj);


    if (refOrTable == null) {
      //there is no reflect object for this java object, just add
      //a single reflect object to the reflectObjTable in the interp
      
      //System.out.println("new object for " + cl);

      return initReflectObject(interp,cl,obj,interp.reflectObjTable,obj);
      
    } else {

      //found something in the table and now we must find out
      //if it is a ReflectObject or a HashTable

      if (refOrTable instanceof ReflectObject) {
	//we found a single reflect object, if the new type is the
	//same as the current ref type then we are ok. If not
	//then we need to make a hashtable for the ReflectObjects
	
	roRep = (ReflectObject) refOrTable;
	
	//the easy types match case, just return the ReflectObject
	if (roRep.javaClass == cl) {
	    //System.out.println("type match for " + cl);
	    roRep.useCount++;
	    return roRep;
	} else {
	  //we need to allocate a hashtable and add the old ref
	  //in with the new ref that will be added to the hash table
	  
	  //System.out.println("type mismatch allocating typetable for " + cl);

	  Hashtable h = new Hashtable(3);
	  
	  //make new hashtable into the interp reflect hash for the object
	  interp.reflectObjTable.put(obj,h);

	  //hash the existing class to the existing ReflectObject
	  h.put(roRep.javaClass,roRep);
	  
	  //now init a new ReflectObject and add it to the hashtable
	  //we just created with the new reference class as the key
	  
	  return initReflectObject(interp,cl,obj,h,cl);
	}
	
      } else {
	//it must be a hash table if it is not a ReflectObject
	//we now hash the class type into this hashtable to
	//find the ReflectObject for the ref type we care about
		
	Hashtable h = (Hashtable) refOrTable;
	
	roRep = (ReflectObject) h.get(cl);

	//add a new reflect object if it is not in the table
	//map the class to the ReflectObject in the table

	if (roRep == null) {
	    //System.out.println("type mismatch in typetable " + cl);
	    return initReflectObject(interp,cl,obj,h,cl);
	} else {
	    //System.out.println("type match in typetable for " + cl);
	    //if the rep is already in the table then just return it
	    roRep.useCount++;
	    return roRep;
	}

      }
    }
}



/*
 *----------------------------------------------------------------------
 *
 * initReflectObject --
 *
 *      Helper for makeReflectObject, it prepares a ReflectObject so
 *      that it can be used inside the interp.
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
initReflectObject(
    Interp interp,          //the interp we are running inside of
    Class  cl,              //the class of the object to reflect
    Object obj,             //the java object to reflect
    Hashtable addTable,     //the hashtable we should add the ReflectObject to
    Object    addKey)       //the key that the ReflectObject is added under
throws TclException
{
  ReflectObject roRep;

  if ((cl != null) && (cl.isArray())) {
    roRep = new ArrayObject();
  } else {
    roRep = new ReflectObject();
  }

  roRep.ownerInterp = interp;
  roRep.javaObj = obj;
  roRep.javaClass = cl;

  if (roRep.javaObj != null) {

      //make sure the object can be represented by the given Class
      Class obj_class = roRep.javaObj.getClass();
      if (! roRep.javaClass.isAssignableFrom(obj_class)) {
	throw new TclException(interp,"object of type " + obj_class.getName()
	   + " can not be referenced as type " + roRep.javaClass.getName());
      }


      //Register the object in the interp.
	    
      interp.reflectObjCount++;
      roRep.refID = "java0x" + Long.toHexString(interp.reflectObjCount);

      interp.createCommand(roRep.refID,roRep);
      interp.reflectIDTable.put(roRep.refID,roRep);
      
      //interp.reflectObjTable.put(obj, roRep);
      addTable.put(addKey,roRep);

  } else {
      //The null object is not registered (hence it can't be deleted).  
      roRep.refID = NULL_REP;
  }

  roRep.useCount = 1;
  roRep.isValid = true;
  
  return roRep;
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

	disposeOfTableRefrences();

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

