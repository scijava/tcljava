/*
 * JavaInvoke.java --
 *
 *	This class implements the common routines used by the java::*
 *	commands to access the Java Reflection API.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: JavaInvoke.java,v 1.15 2002/12/27 22:19:11 mdejong Exp $
 *
 */

package tcl.lang;

import tcl.lang.reflect.*;
import java.lang.reflect.*;
import java.util.*;
import java.beans.*;

/**
 * This class implements the common routines used by the java::*
 * commands to create Java objects, call Java methods and access fields
 * and properties. It also has auxiliary routines for converting between
 * TclObject's and Java Object's.
 */

class JavaInvoke {

// We need to use empty array Object[0] a lot. We keep a static copy
// and re-use it to avoid garbage collection.

static private Object EMPTY_ARGS[] = new Object[0];


/*
 *-----------------------------------------------------------------------------
 *
 * newInstance --
 *
 *	Call the specified constructor.
 *
 * Results:
 *	When successful, the object created by the constructor.
 *
 * Side effects:
 *	The constructor can cause arbitrary side effects.
 *
 *-----------------------------------------------------------------------------
 */

static TclObject
newInstance(
    Interp interp,              // Current interpreter.
    TclObject signature,	// Constructor signature.
    TclObject argv[],		// Arguments.
    int startIdx,		// Index of the first argument in argv[] to
				// pass to the constructor.
    int count)			// Number of arguments to pass to the
				// constructor.
throws
    TclException		// Standard Tcl exception.
{
    FuncSig sig = FuncSig.get(interp, null, signature, argv, startIdx,
	    count, false);

    Object javaObj = call(interp, sig.pkgInvoker, signature, sig.func,
	    null, argv, startIdx, count);

    return ReflectObject.newInstance(interp, sig.targetCls, javaObj);
}

/*
 *-----------------------------------------------------------------------------
 *
 * callMethod --
 *
 *	Call the specified instance or static method of the given object.
 *
 * Results:
 *      When successful, this method returns the Java object that the
 *      Java method would have returned. If the Java method has a void
 *      return type then null is returned.
 *
 * Side effects:
 *	The method can cause arbitrary side effects.
 *
 *-----------------------------------------------------------------------------
 */

static TclObject
callMethod(
    Interp interp,              // Current interpreter.
    TclObject reflectObj,	// The object whose method to invoke.
    TclObject signature,	// Method signature.
    TclObject argv[],		// Arguments.
    int startIdx,		// Index of the first argument in argv[] to
				// pass to the method.
    int count,			// Number of arguments to pass to the
				// method.
    boolean convert)		// Whether the value should be converted
				// into Tcl objects of the closest types.
throws
    TclException
{
    Object javaObj = ReflectObject.get(interp, reflectObj);
    Class  javaCl  = ReflectObject.getClass(interp, reflectObj);
    FuncSig sig = FuncSig.get(interp, javaCl, signature, argv,
                                               startIdx, count, false);
    Method method = (Method) sig.func;

    if (!PkgInvoker.isAccessible(method.getReturnType())) {
	throw new TclException(interp, "Return type \"" +
	        method.getReturnType().getName() +
	        "\" is not accessible");
    }

    Object result = call(interp, sig.pkgInvoker, signature, method, javaObj,
	    argv, startIdx, count);

    if (method.getReturnType() == Void.TYPE) {
	return null;
    } else {
	return wrap(interp, method.getReturnType(), result, convert);
    }
}

/*
 *-----------------------------------------------------------------------------
 *
 * callStaticMethod --
 *
 *	Call the specified static method of the given object.
 *
 * Results:
 *      When successful, this method returns the Java object that the
 *      Java method would have returned. If the Java method has a void
 *      return type then null is returned.
 *
 * Side effects:
 *	The method can cause arbitrary side effects.
 *
 *-----------------------------------------------------------------------------
 */

static TclObject
callStaticMethod(
    Interp interp,		// Current interpreter.	
    TclObject classObj,		// Class whose static method to invoke.
    TclObject signature,	// Method signature.
    TclObject argv[],		// Arguments.
    int startIdx,		// Index of the first argument in argv[] to
				// pass to the method.
    int count,			// Number of arguments to pass to the
				// method.
    boolean convert)		// Whether the value should be converted
				// into Tcl objects of the closest types.
throws
    TclException
{
    Class cls = ClassRep.get(interp, classObj);
    FuncSig sig = FuncSig.get(interp, cls, signature, argv,
	    startIdx, count, true);

    Method method = (Method) sig.func;

    if (!PkgInvoker.isAccessible(method.getReturnType())) {
	throw new TclException(interp, "Return type \"" +
	        method.getReturnType().getName() +
	        "\" is not accessible");
    }

    Object result = call(interp, sig.pkgInvoker, signature, method, null,
	    argv, startIdx, count);

    if (method.getReturnType() == Void.TYPE) {
	return null;
    } else {
	return wrap(interp, method.getReturnType(), result, convert);
    }
}

/*
 *-----------------------------------------------------------------------------
 *
 * call --
 *
 *	Call the constructor, instance method, or static method with
 *	the given parameters. Check the parameter types and perform
 *	TclObject to JavaObject conversion.
 *
 * Results:
 *	The object created by the constructor, or the return value
 *	of the method call.
 *
 * Side effects:
 *	The constructor/method call may have arbitrary side effects.
 *
 *-----------------------------------------------------------------------------
 */

static Object
call(
    Interp interp,
    PkgInvoker invoker,		// The PkgInvoked used to invoke the
				// method or constructor.
    TclObject signature,	// For formatting error message.
    Object func,		// The Constructor or Method to call.
    Object obj,			// The object associated with an instace
				// method call. Should be null for
				// constructor calls and static method
				// calls.
    TclObject argv[],		// Argument list.
    int startIdx,		// Index of the first argument in argv[] to
				// pass to the method or constructor.
    int count)			// Number of arguments to pass to the
				// method or constructor.
throws
    TclException		// Standard Tcl exception.
{
    Class paramTypes[];
    Constructor cons = null;
    Method method = null;
    int i;
    boolean isConstructor = (func instanceof Constructor);

    if (isConstructor) {
	cons = (Constructor) func;
	paramTypes = cons.getParameterTypes();
    } else {
	method = (Method) func;
	paramTypes = method.getParameterTypes();
    }

    if (count != paramTypes.length) {
	throw new TclException(interp, "wrong # args for calling " +
		(isConstructor ? "constructor" : "method") +
		" \"" + signature + "\"");
    }

    Object args[];

    if (count == 0) {
	args = EMPTY_ARGS;
    } else {
	args = new Object[count];
	for (i = 0; i < count; i++) {
	    args[i] = convertTclObject(interp, paramTypes[i],
		    argv[i + startIdx]);
	}
    }

    try {
	final boolean debug = false;
	Object result;

	if (isConstructor) {
	    result = invoker.invokeConstructor(cons, args);
	}  else {
	    result = invoker.invokeMethod(method, obj, args);
	}

	if (debug) {
	    System.out.println("result object from invocation is \""
			       + result + "\"");
	}

	return result;
    } catch (InstantiationException e) {
        throw new TclRuntimeError("unexpected abstract class: " +
                e.getMessage());
    } catch (IllegalAccessException e) {
        throw new TclRuntimeError("unexpected inaccessible ctor or method: " +
                e.getMessage());
    } catch (IllegalArgumentException e) {
        throw new TclRuntimeError("unexpected IllegalArgumentException: " +
                e.getMessage());
    } catch (Exception e) {
	throw new ReflectException(interp, e);
    }
}

/*
 *-----------------------------------------------------------------------------
 *
 * getField --
 *
 *	Returns the value of a field in the given object.
 *
 * Results:
 *	When successful, returns an array: Object result[2]. result[0]
 *	is the value of the field; result[1] is the type of the field.
 *
 * Side effects:
 *	None.
 *
 *-----------------------------------------------------------------------------
 */

static final TclObject
getField(
    Interp interp,		// Current interpreter.
    TclObject classOrObj,	// Class or object whose field to get.
    TclObject signature,	// Signature of the field.
    boolean convert)		// Whether the value should be converted
				// into Tcl objects of the closest types.
throws
    TclException		// Standard Tcl exception.
{
    return getsetField(interp, classOrObj, signature, null, convert, true);
}

/*
 *-----------------------------------------------------------------------------
 *
 * setField --
 *
 *	Sets the value of a field in the given object.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	When successful, the field is set to the given value.
 *
 *-----------------------------------------------------------------------------
 */

static final void
setField(
    Interp interp,		// Current interpreter.
    TclObject classOrObj,	// Class or object whose field to get.
    TclObject signature,	// Signature of the field.
    TclObject value)		// New value for the field.
throws
    TclException		// Standard Tcl exception.
{
    getsetField(interp, classOrObj, signature, value, false, false);
}

/*
 *-----------------------------------------------------------------------------
 *
 * getsetField --
 *
 *	Gets or sets the field in the given object.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	When successful, the field is set to the given value if isget
 *	is false.
 *
 *-----------------------------------------------------------------------------
 */

static TclObject
getsetField(
    Interp interp,		// Current interpreter.
    TclObject classOrObj,	// Class or object whose field to get.
    TclObject signature,	// Signature of the field.
    TclObject value,		// New value for the field.
    boolean convert,		// Whether the value should be converted
				// into Tcl objects of the closest types.
    boolean isget)
throws
    TclException		// Standard Tcl exception.
{
    Class cls = null;
    Object obj = null;
    boolean isStatic = false;

    try {
	obj = ReflectObject.get(interp, classOrObj);
    } catch (TclException e) {
	try {
	    cls = ClassRep.get(interp, classOrObj);
	} catch (TclException e1) {
	    throw new TclException(interp, "unknown class or object \"" + 
		classOrObj  + "\"");
	}
	isStatic = true;

	if (!PkgInvoker.isAccessible(cls)) {
	    throw new TclException(interp, "Class \"" + cls.getName() +
	            "\" is not accessible");
	}
    }

    if (!isStatic) {
	if (obj == null) {
	    throw new TclException(interp,
		"can't access fields in a null object reference");
	}
	cls = ReflectObject.getClass(interp, classOrObj);
    }

    // Check for the special case where the field is named "class"
    // which has a special meaning and is enforced by the javac compiler.
    // If found, return the java.lang.Class object for the named class.

    if (isStatic && isget && signature.toString().equals("class")) {
	return wrap(interp, Class.class, cls, false);
    }

    FieldSig sig = FieldSig.get(interp, signature, cls);
    Field field = sig.field;
    if (isStatic && (!(Modifier.isStatic(field.getModifiers())))) {
	throw new TclException(interp,
		"can't access an instance field without an object");
    }

    if (!PkgInvoker.isAccessible(field.getType())) {
	throw new TclException(interp, "Field type \"" +
	        field.getType().getName() +
	        "\" is not accessible");
    }

    if (!isget && Modifier.isFinal(field.getModifiers())) {
	throw new TclException(interp, "can't set final field \"" +
	        signature + "\"");
    }

    try {
	if (isget) {
	    return wrap(interp, field.getType(),
		    sig.pkgInvoker.getField(field, obj), convert);
	} else {
	    Object javaValue = convertTclObject(interp, field.getType(),
		    value);
	    sig.pkgInvoker.setField(field, obj, javaValue);
	    return null;
	}
    } catch (TclException e1) {
	throw e1;
    } catch (IllegalArgumentException e) {
	throw new TclRuntimeError("unexpected IllegalArgumentException: " +
	        e.getMessage());
    } catch (IllegalAccessException e) {
	throw new TclRuntimeError("unexpected IllegalAccessException: " +
	        e.getMessage());
    } catch (Exception e) {
	throw new ReflectException(interp, e);
    }
}

/*
 *-----------------------------------------------------------------------------
 *
 * getProperty --
 *
 *	Returns the value of a property in the given object.
 *
 * Results:
 *	When successful, returns a the value of the property inside
 *	a TclObject
 *
 * Side effects:
 *	None.
 *
 *-----------------------------------------------------------------------------
 */

static TclObject
getProperty(
    Interp interp,		// Current interpreter.
    TclObject reflectObj,	// The object whose property to query.
    TclObject propName,		// The name of the property to query.
    boolean convert)		// Whether the value should be converted
				// into Tcl objects of the closest types.
throws
    TclException		// A standard Tcl exception.
{
    Object javaObj = ReflectObject.get(interp, reflectObj);
    if (javaObj == null) {
	throw new TclException(interp, "can't get property from null object");
    }

    Class javaClass = ReflectObject.getClass(interp, reflectObj);
    PropertySig sig = PropertySig.get(interp, javaClass, propName);

    Method readMethod = sig.desc.getReadMethod();

    if (readMethod == null) {
	throw new TclException(interp, "can't get write-only property \"" +
		propName + "\"");
    }

    try {
	return wrap(interp, readMethod.getReturnType(),
		sig.pkgInvoker.invokeMethod(readMethod, javaObj,
		EMPTY_ARGS), convert);
    } catch (IllegalAccessException e) {
	throw new TclRuntimeError("unexpected inaccessible readMethod: " +
	        e.getMessage());
    } catch (IllegalArgumentException e) {
	throw new TclRuntimeError("unexpected IllegalArgumentException: " +
	        e.getMessage());
    } catch (Exception e) {
	throw new ReflectException(interp, e);
    }

}

/*
 *-----------------------------------------------------------------------------
 *
 * setProperty --
 *
 *	Returns the value of a property in the given object.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	When successful, the property will have the new value.
 *
 *-----------------------------------------------------------------------------
 */

static void
setProperty(
    Interp interp,		// Current interpreter.
    TclObject reflectObj,	// The object whose property to query.
    TclObject propName,		// The name of the property to query.
    TclObject value)		// Whether the value should be converted
				// into Tcl objects of the closest types.
throws
    TclException		// A standard Tcl exception.
{
    Object javaObj = ReflectObject.get(interp, reflectObj);
    if (javaObj == null) {
	throw new TclException(interp, "can't set property in null object");
    }

    Class  javaClass = ReflectObject.getClass(interp, reflectObj);
    PropertySig sig = PropertySig.get(interp,javaClass,propName);


    Method writeMethod = sig.desc.getWriteMethod();
    Class type = sig.desc.getPropertyType();

    if (writeMethod == null) {
	throw new TclException(interp, "can't set read-only property \"" +
		propName + "\"");
    }

    Object args[] = new Object[1];
    args[0] = convertTclObject(interp, type, value);

    try {
	sig.pkgInvoker.invokeMethod(writeMethod, javaObj, args);
    } catch (IllegalAccessException e) {
	throw new TclRuntimeError("unexpected inaccessible writeMethod: " +
	        e.getMessage());
    } catch (IllegalArgumentException e) {
	throw new TclRuntimeError("unexpected IllegalArgumentException: " +
	        e.getMessage());
    } catch (Exception e) {
	throw new ReflectException(interp, e);
    }
}

/*
 *-----------------------------------------------------------------------------
 *
 * getClassByName --
 *
 *	Returns Class object identified by the string name. We allow
 *	abbreviation of the java.lang.* class if there is no ambiguity:
 *	e.g., if there is no class whose fully qualified name is "String",
 *	then "String" means java.lang.String.
 *
 * Results:
 *	If successful, The Class object identified by the string name.
 *
 * Side effects:
 *	None.
 *
 *-----------------------------------------------------------------------------
 */

static Class
getClassByName(
    Interp interp,                      // Interp used by TclClassLoader
    String clsName)			// String name of the class.
throws
    TclException			// If the class cannot be found.
{
    Class result = null;
    int dimension;

    // If the string is of the form className[][]..., strip out the trailing
    // []s and record the dimension of the array.

    StringBuffer prefix_buf = new StringBuffer();
    StringBuffer suffix_buf = new StringBuffer();
    StringBuffer clsName_buf = new StringBuffer(clsName);

    int clsName_len;
    for (dimension = 0; true ; dimension++) {
        clsName_len = clsName_buf.length();

        if ((clsName_len > 2) &&
            (clsName_buf.charAt(clsName_len - 2) == '[') &&
            (clsName_buf.charAt(clsName_len - 1) == ']')) {

            clsName_buf.setLength(clsName_len - 2);
            prefix_buf.append('[');
        } else {
            break;
        }
    }

    try {
        clsName = clsName_buf.toString(); // Use shortened form of name

        // Search for the char '.' in the name. If '.' is in
        // the name then we know it is not a builtin type

	if (clsName.indexOf('.') == -1) {
	    if (dimension > 0) {
		if (clsName.equals("int")) {
		    prefix_buf.append('I');
		    return Class.forName(prefix_buf.toString());
		} else if (clsName.equals("boolean")) {
		    prefix_buf.append('Z');
		    return Class.forName(prefix_buf.toString());
		} else if (clsName.equals("long")) {
		    prefix_buf.append('J');
		    return Class.forName(prefix_buf.toString());
		} else if (clsName.equals("float")) {
		    prefix_buf.append('F');
		    return Class.forName(prefix_buf.toString());
		} else if (clsName.equals("double")) {
		    prefix_buf.append('D');
		    return Class.forName(prefix_buf.toString());
		} else if (clsName.equals("byte")) {
		    prefix_buf.append('B');
		    return Class.forName(prefix_buf.toString());
		} else if (clsName.equals("short")) {
		    prefix_buf.append('S');
		    return Class.forName(prefix_buf.toString());
		} else if (clsName.equals("char")) {
		    prefix_buf.append('C');
		    return Class.forName(prefix_buf.toString());
		} else {
		    prefix_buf.append('L');
		    suffix_buf.append(';');
		}
	    } else {
		if (clsName.equals("int")) {
		    return Integer.TYPE;
		} else if (clsName.equals("boolean")) {
		    return Boolean.TYPE;
		} else if (clsName.equals("long")) {
		    return Long.TYPE;
		} else if (clsName.equals("float")) {
		    return Float.TYPE;
		} else if (clsName.equals("double")) {
		    return Double.TYPE;
		} else if (clsName.equals("byte")) {
		    return Byte.TYPE;
		} else if (clsName.equals("short")) {
		    return Short.TYPE;
		} else if (clsName.equals("char")) {
		    return Character.TYPE;
		}
	    }
	   
	    TclClassLoader tclClassLoader = new TclClassLoader(interp, null);

	    try {
		result = tclClassLoader.loadClass(prefix_buf + clsName + suffix_buf);
	    } catch (ClassNotFoundException e) {
		// If the class loader can not find the class then check with
		// the "import" feature to see if the given clsName maps to
		// a fully qualified class name.

		String fullyqualified = JavaImportCmd.getImport(interp, clsName);

		// If we do not find a fully qualified name in the import table
		// then try to fully qualify the class with the java.lang prefix 
		
		if (fullyqualified == null) {
		    fullyqualified = "java.lang." + clsName;
		}

		// If the system class loader cannot resolve the class, than a
		// SecurityException is thrown, catch this and 
		// throw the standard error.

		try {
		    result = tclClassLoader.loadClass(prefix_buf +
			        fullyqualified +  suffix_buf);
		} catch (SecurityException e2) {
		    result = null;
		}
	    }
	} else {
	    TclClassLoader tclClassLoader = new TclClassLoader(interp, null);
	
	    if (dimension > 0) {
		clsName = prefix_buf + "L" + clsName + ";";
	    }
	    result = tclClassLoader.loadClass(clsName);
	}
    } catch (ClassNotFoundException e) {
	result = null;
    } catch (SecurityException e) {
	throw new TclException(interp, 
		"cannot load new class into java or tcl package");
    }

    if (result == null) {
	throw new TclException(interp, "unknown class \"" + clsName_buf + "\"");
    }

    return result;
}

/*
 *----------------------------------------------------------------------
 *
 *  convertJavaObject --
 *
 *	Converts the java.lang.Object into a Tcl object and return
 *	TclObject that holds the reult. Primitive data types
 *	are converted into primitive Tcl data types. Otherwise,
 *	a ReflectObject wrapper is created for the object so that it
 *	can be later accessed with the Reflection API.
 *
 * Results:
 *	The TclObject representation of the Java object.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static TclObject
convertJavaObject(
    Interp interp,	// Current interpreter.
    Class cls,		// The class of the Java Object
    Object javaObj)	// The java.lang.Object to convert to a TclObject.
throws TclException
{
    if (javaObj == null) {
	if (cls == String.class) {
	    return TclString.newInstance("");
	} else {
	    return ReflectObject.newInstance(interp, cls, javaObj);
	}

    } else if ((cls == Integer.TYPE) || (cls == Integer.class)) {
	return TclInteger.newInstance(((Integer) javaObj).intValue());

    } else if ((cls == Long.TYPE) || (cls == Long.class)) {
	// A long can not be represented as a TclInteger
	return TclString.newInstance(javaObj.toString());

    } else if ((cls == Short.TYPE) || (cls == Short.class)) {
	return TclInteger.newInstance(((Short) javaObj).intValue());

    } else if ((cls == Byte.TYPE) || (cls == Byte.class)) {
	return TclInteger.newInstance(((Byte) javaObj).intValue());

    } else if ((cls == Double.TYPE) || (cls == Double.class)) {
	return TclDouble.newInstance(((Double) javaObj).doubleValue());

    } else if ((cls == Float.TYPE) || (cls == Float.class)) {
	return TclDouble.newInstance(((Float) javaObj).doubleValue());

    } else if ((cls == Boolean.TYPE) || (cls == Boolean.class)) {
	return TclBoolean.newInstance(((Boolean) javaObj).booleanValue());

    } else if ((cls == Character.TYPE) || (cls == Character.class)) {
	return TclString.newInstance(((Character) javaObj).toString());

    } else if (cls == String.class) {
	return TclString.newInstance((String)javaObj);

    } else {
	return ReflectObject.newInstance(interp, cls, javaObj);
    }
}

/*
 *-----------------------------------------------------------------------------
 *
 * convertTclObject --
 *
 *	Converts a Tcl object to a Java Object of the required type.
 *
 * Results:
 *	An Object of the required type.
 *
 * Side effects:
 *	None.
 *
 *-----------------------------------------------------------------------------
 */

static final Object
convertTclObject(
    Interp interp,		// Current interpreter.
    Class type,			// Convert to this type.
    TclObject tclObj)		// From this Tcl object.
throws
    TclException		// If conversion fails.
{
    Object javaObj = null;
    boolean isReflectObj = false;

    try {
	javaObj = ReflectObject.get(interp, tclObj);
	isReflectObj = true;
    } catch (TclException e) {
	interp.resetResult();
    }


    if (! isReflectObj) {
	// tclObj a Tcl "primitive" value. We try convert it to the 
	// corresponding primitive value in Java.
	//
	// To optimize performance, the following "if" statements are
	// arranged according to (my guesstimation of) the frequency
	// that a certain type is used.

	if (type == String.class) {
	    return tclObj.toString();

	} else if (type == Object.class) {
	    return tclObj.toString();

	} else if ((type == Integer.TYPE) || (type == Integer.class)) {
	    return new Integer(TclInteger.get(interp, tclObj));

	} else if ((type == Boolean.TYPE) || (type == Boolean.class)) {
	    return new Boolean(TclBoolean.get(interp, tclObj));

	} else if ((type == Long.TYPE) || (type == Long.class)) {
	    // A tcl integer can be converted a long (widening conversion)
	    // and a Java long may be represented as a tcl integer if it
	    // is small enogh, so we try to convert the string to a
	    // tcl integer and if that fails we try to convert to a
	    // java Long object. If both of these fail throw original error.

	    try {
	        return new Long(TclInteger.get(interp, tclObj));
	    } catch (TclException e1) {
	        try {
	            return new Long( tclObj.toString() );
	        } catch (NumberFormatException e2) {
	            throw e1;
	        }
	    }

	} else if ((type == Float.TYPE) || (type == Float.class)) {
	    return new Float((float) TclDouble.get(interp, tclObj));

	} else if ((type == Double.TYPE) || (type == Double.class)) {
	    return new Double(TclDouble.get(interp, tclObj));

	} else if ((type == Byte.TYPE) || (type == Byte.class)) {
	    int i = TclInteger.get(interp, tclObj);
	    if ((i < Byte.MIN_VALUE) || (i > Byte.MAX_VALUE)) {
		throw new TclException(interp,
		   "integer value too large to represent in a byte");
	    }
	    return new Byte((byte) i);

	} else if ((type == Short.TYPE) || (type == Short.class)) {
	    int i = TclInteger.get(interp, tclObj);
	    if ((i < Short.MIN_VALUE) || (i > Short.MAX_VALUE)) {
		throw new TclException(interp,
		    "integer value too large to represent in a short");
	    }
	    return new Short((short) i);

	} else if ((type == Character.TYPE) || (type == Character.class)) {
	    String str = tclObj.toString();

	    if (str.length() == 1) {
		return new Character(str.charAt(0));
	    } else {
	        throw new TclException(interp, "expected character but got \""
		    + tclObj + "\"");
	    }
	} else {
	    throw new TclException(interp, "\"" + tclObj +
		    "\" is not an object handle of class \"" +
                     JavaInfoCmd.getNameFromClass(type) +
		    "\"");
	}
    } else {
	// The TclObject is a ReflectObject that contains javaObj. We
	// check to see if javaObj can be converted to the required
	// type. If javaObj is a wrapper for a primitive type then
	// we check to see if the object is an instanceof the type.

	if (javaObj == null) {
	    return null;
	}

	if (type.isInstance(javaObj)) {
	    return javaObj;
	}

	if (type.isPrimitive()) {
	    if (type == Boolean.TYPE) {
	        if (javaObj instanceof Boolean) {
	            return javaObj;
	        }
	    }
	    else if (type == Character.TYPE) {
	        if (javaObj instanceof Character) {
	            return javaObj;
	        }
	    }
	    else if (type == Byte.TYPE) {
	        if (javaObj instanceof Byte) {
	            return javaObj;
	        }
	    }
	    else if (type == Short.TYPE) {
	        if (javaObj instanceof Short) {
	            return javaObj;
	        }
	    }
	    else if (type == Integer.TYPE) {
	        if (javaObj instanceof Integer) {
	            return javaObj;
	        }
	    }
	    else if (type == Long.TYPE) {
	        if (javaObj instanceof Long) {
	            return javaObj;
	        }
	    }
	    else if (type == Float.TYPE) {
	        if (javaObj instanceof Float) {
	            return javaObj;
	        }
	    }
	    else if (type == Double.TYPE) {
	        if (javaObj instanceof Double) {
	            return javaObj;
	        }
	    }
	    else if (type == Void.TYPE) {
	        // void is not a valid type for conversions
	    }
	}

	throw new TclException(interp, "expected object of type " +
                JavaInfoCmd.getNameFromClass(type) +
		" but got \"" + tclObj + "\" (" +
                JavaInfoCmd.getNameFromClass(
        	    ReflectObject.getClass(interp, tclObj) ) +
                ")");
    }
}

/*
 *-----------------------------------------------------------------------------
 *
 * wrap --
 *
 *	Wraps a Java Object into a TclObject according to whether the
 *	convert flag is set.
 *
 * Results:
 *	The TclObject that wraps the Java Object.
 *
 * Side effects:
 *	None.
 *
 *-----------------------------------------------------------------------------
 */

private static final TclObject
wrap(
    Interp interp,	// Current interpreter.
    Class cls,		// The class of the Java Object (we can't use
			// javaObj.getClass because javaObj may be null.)
    Object javaObj,	// The Java Object to wrap.
    boolean convert)	// Whether the value should be converted
			// into Tcl objects of the closest types. 
throws TclException
{
    if (convert) {
	return convertJavaObject(interp, cls, javaObj);
    } else {
	return ReflectObject.newInstance(interp, cls, javaObj);
    }
}

} // end JavaInvoke

