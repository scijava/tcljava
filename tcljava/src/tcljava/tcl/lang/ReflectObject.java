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
 * RCS: @(#) $Id: ReflectObject.java,v 1.3 1999/05/09 22:41:16 dejong Exp $
 *
 */

package tcl.lang;

import java.lang.reflect.*;
import java.util.*;
import java.beans.*;

/**
 * A ReflectObject is used to create and access arbitrary Java objects
 * using the Java Reflection API. It wraps around a Java object (i.e.,
 * an instance of any Java class) and expose it to Tcl scripts. The
 * object is registered inside the interpreter and is given a string
 * name. Tcl scripts can manipulate this object as long as the the
 * reference count of the object is greater than zero.
 */

public class ReflectObject extends InternalRep implements CommandWithDispose {

// The java.lang.Object wrapped by the ReflectObject representation.

Object javaObj;
Class  javaClass;

// The interpreter in which the java.lang.Object is registered in.
// ReflectObject's are not shared among interpreters for safety
// reasons.

Interp ownerInterp;

// The reference ID of this object.

String refID;

// This variables records how many TclObject's are using
// this ReflectObject internal rep. In this example:
//
//     set x [new java.lang.Integer 1]
//     set y [format %s $x]
//     java::info methods $y
//
// The two objects $x and $y share the same ReflectObject instance.
// useCount is 2 when the java::info command has just translated the
// string $y into a ReflectObject.
//
// useCount is initially 1. It will be more than 1 only when the
// script tries to refer to the object using its string form, or when
// the same object is returned by the Reflection API more than once.
//
// This variable is called useCount rather than refCount to avoid
// confusion with TclObject.refCount.

private int useCount;

// This variable marks whether the object is still considered "valid"
// in Tcl scripts. An object is no longer valid if its object command
// has been explicitly deleted from the interpreter.

private boolean isValid;

// Stores the bindings of this ReflectObject. This member variable is used
// in the BeanEventMgr class.

Hashtable bindings;


// the string representation of the null reflect object

private static final String NULL_REP = "java0x0";

// this really should be final but there is a bug in Sun's javac which
// incorrectly flags this as a "final not initialized" error
//private static final ReflectObject NULL_OBJECT;

private static ReflectObject NULL_OBJECT;


// Allocate single object used to represent the untyped null java
// Object. A null object is not registered (hence it can't be deleted).  
static {
	NULL_OBJECT = makeNullObject(null, null);
}

protected static final String NOCONVERT = "-noconvert";

protected static final String CMD_PREFIX = "java0x";

// set to true to see extra output

private static final boolean debug = false;





// Private helper for creating reflected null objects

private static ReflectObject makeNullObject(Interp i, Class c) {
    ReflectObject ro = new ReflectObject();

    ro.ownerInterp = i;

    ro.refID = NULL_REP;
    ro.useCount = 1;
    ro.isValid = true;

    ro.javaObj = null;
    ro.javaClass = c;

    return ro;
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
  throws TclException // if a null class with a non null object is passed in
{
    //final boolean debug = true;

    if (obj == null) {
        // this is the null reflect object case

        if (debug) {
            System.out.println("null object");
        }

	if (debug && (cl != null)) {
	    System.out.println("non null class with null object");
	    System.out.println("non null class was " + cl);
	}

	return makeNullObject(interp,cl);
    }

    if (cl == null) {
        // we have no way to deal with a non null object that has a
        // null class reference type, we must give up in this case

        throw new TclException(interp,"non null reflect object with null class is not valid");
    }
    
   
    // apply builtin type conversion rules

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

    if (debug) {
        System.out.println("object will be reflected as " + cl);
    }


    ReflectObject roRep = null;

    // now we hash the object id to find the ReflectObject or 
    // ReflectObjects that represent this object. The case where
    // a single object is referenced as only one type will be common
    // so the object we hash to can be either a ReflectObject or a
    // Hashtable. At runtime we determine which one it is. While
    // this may take a little longer it will save a lot of space
    // because an object will not need to have a hashtable
    // allocated to it unless it is getting referenced multiple
    // time and by different object reference types.
    
    Object refOrTable = interp.reflectObjTable.get(obj);


    if (refOrTable == null) {
      // there is no reflect object for this java object, just add
      // a single reflect object to the reflectObjTable in the interp
      
      if (debug) {
          System.out.println("new reflect object for " + cl);
      }

      return initReflectObject(interp,cl,obj,interp.reflectObjTable,obj);
      
    } else {

      // found something in the table and now we must find out
      // if it is a ReflectObject or a HashTable

      if (refOrTable instanceof ReflectObject) {
	// we found a single reflect object, if the new type is the
	// same as the current ref type then we are ok. If not
	// then we need to make a hashtable for the ReflectObjects
	
	roRep = (ReflectObject) refOrTable;

        if (debug) {
	    System.out.println("typetable with single reflect object found");
        }

	
	// the easy types match case, just return the ReflectObject
	if (roRep.javaClass == cl) {
            if (debug) {
	        System.out.println(cl + " already in typetable for " +
			roRep.refID);
            }
	    roRep.useCount++;
	    return roRep;
	} else {
	  // we need to allocate a hashtable and add the old ref
	  // in with the new ref that will be added to the hash table
	  
          if (debug) {
	      System.out.println(cl + " not found, allocating typetable for "
                  +  roRep.refID);
          }

	  Hashtable h = new Hashtable(3);
	  
	  // make new hashtable into the interp reflect hash for the object
	  interp.reflectObjTable.put(obj,h);

	  // hash the existing class to the existing ReflectObject
	  h.put(roRep.javaClass,roRep);
	  
	  // now init a new ReflectObject and add it to the hashtable
	  // we just created with the new reference class as the key
	  
	  return initReflectObject(interp,cl,obj,h,cl);
	}
	
      } else {
	// it must be a hash table if it is not a ReflectObject
	// we now hash the class type into this hashtable to
	// find the ReflectObject for the ref type we care about
		
	Hashtable h = (Hashtable) refOrTable;

        if (debug) {
	    System.out.println("typetable with " + h.size() +
                " entires found");
        }

	
	roRep = (ReflectObject) h.get(cl);

	// add a new reflect object if it is not in the table
	// map the class to the ReflectObject in the table

	if (roRep == null) {
            if (debug) {
	        System.out.println(cl + " not found in typetable");
            }

	    return initReflectObject(interp,cl,obj,h,cl);
	} else {
            if (debug) {
	        System.out.println(cl + " already in typetable for "
                    + roRep.refID);
            }

	    // if the rep is already in the table then just return it
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
    Interp interp,          // the interp we are running inside of
    Class  cl,              // the class of the object to reflect
    Object obj,             // the java object to reflect
    Hashtable addTable,     // the hashtable we should add the ReflectObject to
    Object    addKey)       // the key that the ReflectObject is added under
throws TclException
{
  ReflectObject roRep;

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
    throw new TclException(interp,"object of type " + obj_class.getName()
        + " can not be referenced as type " + roRep.javaClass.getName());
  }


  if (debug) {
      System.out.println("PRE REGISTER DUMP");
      dump(interp);
  }


  // Register the object in the interp.
	    
  interp.reflectObjCount++; // incr id, the first id used will be 1
  roRep.refID = CMD_PREFIX + Long.toHexString(interp.reflectObjCount);

  interp.createCommand(roRep.refID,roRep);
  interp.reflectIDTable.put(roRep.refID,roRep);
      
  addTable.put(addKey,roRep);

  if (debug) {
     System.out.println("reflect object " + roRep.refID +
         " of type " + roRep.javaClass.getName() + " registered");
  }
  
  roRep.useCount = 1;
  roRep.isValid  = true;


  if (debug) {
      System.out.println("POST REGISTER DUMP");
      dump(interp);
  }


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
    if (debug) {
        System.out.println("dispose called for reflect object " + refID);
    }

    useCount--;
    if ((useCount == 0) && (refID != NULL_REP)) {
        // No TclObject is using this internal rep anymore. Free it.

        if (debug) {
            System.out.println("reflect object " + refID +
                " is no longer being used");
        }

	if (debug) {
	    System.out.println("PRE DELETE DUMP");
	    dump(ownerInterp);
	}

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



// helper function for dispose, it will clean up reprences inside
// the interp.reflectObjTable which currently holds either a
// ReflectObject or a table of reflect objects

private void
disposeOfTableRefrences() {

  Object refOrTable = ownerInterp.reflectObjTable.get(javaObj);

  // This should never happen
  if (refOrTable == null) {
      //dump(ownerInterp);

      throw new NullPointerException("reflectObjectTable returned null for " +
          refID);
  }

  if (refOrTable instanceof ReflectObject) {
      // this is the easy case, we just need to remove the key
      // that maps the java object to this reflect object

      if (debug) {
          System.out.println("removing single entry typetable containing " +
              ((ReflectObject) refOrTable).refID );
      }

      ownerInterp.reflectObjTable.remove(javaObj);
  } else {
      // in this case the main table hashed to a hash table
      // so we need remove the current ref type from the hashtable

      Hashtable h = (Hashtable) refOrTable;

      if (debug) {
          System.out.println("removing typetable entry for " + javaClass);
      }

      // remove the entry for the current refrencing class
      h.remove(javaClass);

      // now if there is only one key left in the hashtable
      // we can remap the original table so that it maps
      // the java object to the ReflectObject

      if (h.size() == 1) {
          // get the only value out of the table
	  Object value = h.elements().nextElement();

          // put the original single mapping back into the
	  // interp reflect table which also frees up the
	  // reftype hashtable for garbage collection
      
          ownerInterp.reflectObjTable.put(javaObj,value);

          if (debug) {
	      System.out.println("removing typetable");
          }
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
        if ((roRep.isValid && (roRep.ownerInterp == interp))) {
	    return;
	}
    }

    String s = tobj.toString();
    if (s.startsWith(CMD_PREFIX)) {
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
    ReflectObject rep = (ReflectObject) tobj.getInternalRep();
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
    ReflectObject rep = (ReflectObject) tobj.getInternalRep();
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
    return (ReflectObject) tobj.getInternalRep();
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
    if (debug) {
        System.out.println("ReflectObject instance " + refID + " -> disposedCmd()");
    }

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



// This method is only used for debugging, it will dump the contents of the
// reflect table in a human readable form. The dump is to stdout.

public static void dump(
    Interp interp)
{ 
    try {
    System.out.println("BEGIN DUMP -------------------------------");
    System.out.println("interp.reflectObjCount = " + interp.reflectObjCount);
    System.out.println("interp.reflectIDTable.size() = " + interp.reflectIDTable.size());
    System.out.println("interp.reflectObjTable.size() = " + interp.reflectObjTable.size());



    for (Enumeration keys = interp.reflectIDTable.keys() ; keys.hasMoreElements() ;) {
        System.out.println();
        String refID = (String) keys.nextElement();
        ReflectObject roRep = (ReflectObject) interp.reflectIDTable.get(refID);

        // do sanity check
        if (roRep == null) {
          System.out.println("Table refID \"" + refID + "\" hashed to the null object");
        }

        // do sanity check
        if (! refID.equals(roRep.refID)) {
          System.out.println("Table refID \"" + refID + "\" does not match object refId \"" +
          roRep.refID + "\"");
        }

        // do sanity check
        if (roRep.ownerInterp != interp) {
          System.out.println("roRep.ownerInterp not the same as current interp");
        }

        System.out.println("refID = \"" + roRep.refID + "\"");
        System.out.println("javaObj.hashCode() = \"" + roRep.javaObj.hashCode()  + "\"");

        System.out.println("javaClass = \"" + roRep.javaClass.getName() + "\"");

        // do sanity check
        TclObject tobj = TclString.newInstance(roRep.refID);
        Class  tclass;
        try {
            tclass = ReflectObject.getClass(interp, tobj);
        } catch (TclException e) {
            tclass = null;
        }
        if (roRep.javaClass != tclass) {
            System.out.println("javaClass is not the same the reflect class type \"" +
                tclass.getName() + "\" in the interp");
        }


        System.out.println("useCount = \"" + roRep.useCount + "\"");
        System.out.println("isValid = \"" + roRep.isValid + "\"");


        // do sanity check
        Command command;
        try {
            command = interp.getCommand(roRep.refID);
        } catch (TclRuntimeError e) { //Tcl Blend did not have this implemented
            command = null;
        }
        if (command == null) {
            System.out.println("could not find command named \"" + roRep.refID + "\"");
        }



        // use the Java Object to lookup the ReflectObject typetable
        Object refOrTable = interp.reflectObjTable.get(roRep.javaObj);

        if (refOrTable == null) {
            System.out.println("typetable is null");
        } else {

            if (refOrTable instanceof ReflectObject) {
                // sanity check
                if (roRep != refOrTable) {
                    System.out.println("reflect object in typetable is not the same as the reflect object used to lookup the typetable");
                }

	        roRep = (ReflectObject) refOrTable;

                System.out.println("typetable is a single entry of type \""  +
                    roRep.javaClass.getName() +  "\"");
            } else {
                // the typetable is made up of a Hashtable
	        Hashtable h = (Hashtable) refOrTable;

                System.out.println("typetable has " + h.size() + " entries");
	
                for (Enumeration typetable_keys = h.keys() ;
                    typetable_keys.hasMoreElements() ;) {
                    System.out.println();
                    Class key = (Class) typetable_keys.nextElement();
	            roRep = (ReflectObject) h.get(key);

                    System.out.println("typetable entry refID is \""  +
                        roRep.refID + "\"");

                    System.out.println("typetable entry type is \""  +
                        roRep.javaClass.getName() +  "\"");
                }

            }

        }


    }
    System.out.println();


   // dump the table from a Tcl/Java shell like this
   // java::call tcl.lang.ReflectObject dump [java::getinterp]


    } catch (Throwable e) { e.printStackTrace(System.out); }

}

} // end ReflectObject

