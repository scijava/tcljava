/* 
 * javaObj.c --
 *
 *	This file implements the routines that maintain the correspondence
 *	between TclObject instances and Tcl_Obj * references.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: javaObj.c,v 1.4.2.5 2000/10/25 11:27:09 mdejong Exp $
 */

#include "java.h"
#include "javaNative.h"

static void		DupJavaCmdInternalRep(Tcl_Obj *srcPtr,
			    Tcl_Obj *dupPtr);
static void		DupTclObject(Tcl_Obj *srcPtr, Tcl_Obj *destPtr);
static void		FreeJavaCmdInternalRep(Tcl_Obj *objPtr);
static void		FreeTclObject(Tcl_Obj *objPtr);
static int		SetJavaCmdFromAny(Tcl_Interp *interp, Tcl_Obj *objPtr);
static int		SetTclObject(Tcl_Interp *interp, Tcl_Obj *objPtr);
static void		UpdateTclObject(Tcl_Obj *objPtr);

/*
 * TclObject type information.
 */

Tcl_ObjType tclObjectType = {
     "TclObject",
     FreeTclObject,
     DupTclObject,
     UpdateTclObject,
     SetTclObject
};

/*
 * Pointer to old cmdType information.
 */

static Tcl_ObjType oldCmdType;
static Tcl_ObjType *cmdTypePtr = NULL;

/*
 * Mutex to serialize access to cmdTypePtr.
 */

static Tcl_Mutex cmdTypePtrLock;


/*
 *----------------------------------------------------------------------
 *
 * JavaObjInit --
 *
 *	Initialize the JavaObj module.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Registers the TclObject type and hijacks the cmdName type.
 *
 *----------------------------------------------------------------------
 */

void
JavaObjInit()
{
    /*
     * The JavaObjInit method could get called
     * from multiple threads. We only want to
     * init the object type once.
     */

    Tcl_MutexLock(&cmdTypePtrLock);

    if (cmdTypePtr == NULL) {
        Tcl_RegisterObjType(&tclObjectType);
    
        /*
         * Interpose on the "cmdName" type to preserve 
         * java objects.
         */

        cmdTypePtr = Tcl_GetObjType("cmdName");
        oldCmdType = *cmdTypePtr;
        cmdTypePtr->freeIntRepProc = FreeJavaCmdInternalRep;
        cmdTypePtr->dupIntRepProc = DupJavaCmdInternalRep;
        cmdTypePtr->setFromAnyProc = SetJavaCmdFromAny;
    }

    Tcl_MutexUnlock(&cmdTypePtrLock);
}

/*
 *----------------------------------------------------------------------
 *
 * printString --
 *
 *	Dump the string representation of an object to stdout. This
 *	function is purely for debugging purposes.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

void
printString(
    JNIEnv *env,		/* Java environment. */
    jobject object)
{
    JavaInfo* jcache = JavaGetCache();
    jstring string = (*env)->CallObjectMethod(env, object, jcache->toString);
    const char *str = (*env)->GetStringUTFChars(env, string, NULL);
    printf("toString: %x '%s'\n", (unsigned int) object, str);
    (*env)->ReleaseStringUTFChars(env, string, str);
}

/*
 *----------------------------------------------------------------------
 *
 * DupTclObject --
 *
 *	Copy the internal rep for a TclObject.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Increments the reference count on the TclObject.  Creates a
 *	new global reference to the object.
 *
 *----------------------------------------------------------------------
 */

static void
DupTclObject(
    Tcl_Obj *srcPtr,
    Tcl_Obj *destPtr)
{
    jobject object = (jobject)(srcPtr->internalRep.twoPtrValue.ptr2);
    JNIEnv *env = JavaGetEnv();
    JavaInfo* jcache = JavaGetCache();

    /*
     * Add a global reference to represent the new copy.
     */

    object = (*env)->NewGlobalRef(env, object);
    destPtr->typePtr = srcPtr->typePtr;
    destPtr->internalRep.twoPtrValue.ptr2 = (VOID*) object;
    (*env)->CallVoidMethod(env, object, jcache->preserve);
}

/*
 *----------------------------------------------------------------------
 *
 * FreeTclObject --
 *
 *	Free the internal representation for a TclObject.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Decrements the reference count of the TclObject and frees the
 *	global object reference.
 *
 *----------------------------------------------------------------------
 */

static void
FreeTclObject(
    Tcl_Obj *objPtr)		/* Object to free. */
{
    jobject object = (jobject)(objPtr->internalRep.twoPtrValue.ptr2);
    JNIEnv *env = JavaGetEnv();
    JavaInfo* jcache = JavaGetCache();

    (*env)->CallVoidMethod(env, object, jcache->release);
    (*env)->DeleteGlobalRef(env, object);
}

/*
 *----------------------------------------------------------------------
 *
 * SetTclObject --
 *
 *	No conversion to a TclObject is possible.
 *
 * Results:
 *	Always returns TCL_ERROR.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static int
SetTclObject(
    Tcl_Interp *interp,
    Tcl_Obj *objPtr)
{
    if (interp) {
	Tcl_ResetResult(interp);
	Tcl_SetStringObj(Tcl_GetObjResult(interp),
		"cannot convert to TclObject", -1);
    }
    return TCL_ERROR;
}

/*
 *----------------------------------------------------------------------
 *
 * UpdateTclObject --
 *
 *	Retrieve the string representation from the TclObject.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Updates the string representation of the Tcl_Obj.
 *
 *----------------------------------------------------------------------
 */

static void
UpdateTclObject(Tcl_Obj *objPtr)
{
    jstring string;
    jobject object = (jobject)(objPtr->internalRep.twoPtrValue.ptr2);
    JNIEnv *env = JavaGetEnv();
    JavaInfo* jcache = JavaGetCache();

    string = (*env)->CallObjectMethod(env, object, jcache->toString);
    objPtr->bytes = JavaGetString(env, string, &objPtr->length);
    (*env)->DeleteLocalRef(env, string);
}

/*
 *----------------------------------------------------------------------
 *
 * JavaGetTclObj --
 *
 *	Retrieve the Tcl_Obj that corresponds to the given Java
 *	TclObject. Creates a new Tcl_Obj of type TclObject with an internal
 *	representation that points at the Java object.
 *
 * Results:
 *	Returns the Tcl_Obj that corresponds to the TclObject.
 *
 * Side effects:
 *	Adds a reference to the TclObject.
 *
 *----------------------------------------------------------------------
 */

Tcl_Obj*
JavaGetTclObj(
    JNIEnv *env,		/* Java environment. */
    jobject object)		/* TclObject. */
{
    Tcl_Obj *objPtr;
    jobject internalRep;
    jlong objRef;
    JavaInfo* jcache = JavaGetCache();
    
    internalRep = (*env)->CallObjectMethod(env, object, jcache->getInternalRep);

    if ((*env)->IsInstanceOf(env, internalRep, jcache->CObject) == JNI_TRUE) {
	/*
	 * This object is a C reference so we extract the Tcl_Obj* from the
	 * internal representation.
	 */

	objRef = (*env)->GetLongField(env, internalRep, jcache->objPtr);
	objPtr = *(Tcl_Obj**)&objRef;

    } else {
	/*
	 * This object is of an unknown type so we create a new Tcl object to
	 * hold the object reference.
	 */

	objPtr = Tcl_NewObj();
	objPtr->bytes = NULL;
	objPtr->typePtr = &tclObjectType;
	objPtr->internalRep.twoPtrValue.ptr2
	    = (VOID*) (*env)->NewGlobalRef(env, object);

	/*
	 * Increment the reference count on the TclObject.
	 */

	(*env)->CallVoidMethod(env, object, jcache->preserve);
    }
    (*env)->DeleteLocalRef(env, internalRep);
    return objPtr;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_CObject_getString --
 *
 *	Retrieve the string representation for an object.
 *
 * Class:     tcl_lang_CObject
 * Method:    getString
 * Signature: (J)Ljava/lang/String;
 *
 * Results:
 *	Returns a new Java string.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

jstring JNICALL
Java_tcl_lang_CObject_getString(
    JNIEnv *env,		/* Java environment. */
    jclass class,		/* Handle to CObject class. */
    jlong obj)			/* Value of CObject.objPtr. */
{
    Tcl_Obj *objPtr = *(Tcl_Obj **) &obj;
    jchar *buf;
    char *str;
    jstring result;
    int length;
    char *p, *end;
    Tcl_UniChar *w;

    if (!objPtr) {
	jclass nullClass = (*env)->FindClass(env,
		"java/lang/NullPointerException");
	(*env)->ThrowNew(env, nullClass, "Invalid CObject.");
    }

    /*
     * Convert the string rep into a Unicode string.
     */

    str = Tcl_GetStringFromObj(objPtr, &length);
    if (length > 0) {
        buf = (jchar*) ckalloc(length*sizeof(jchar));

	w = buf;
	end = str + length;
	for (p = str; p < end; ) {
	  /*
	  fprintf(stderr, "UTF index %d is %d -> '%c'\n",
		  ((int) (p - str)), ((int) *p), *p);
	  */

	  p += Tcl_UtfToUniChar(p, w);
	  /*
	  if (((unsigned int) *w) > ((unsigned int) 254)) {
	    fprintf(stderr, "unicode char %d added\n", *w);
	  }
	  */
	  w++;
	}

	/*
	 * The UTF-8 encoded string length could be larger
	 * than the unicode version (in chars not bytes),
	 * so we need to set the length to the number of
	 * unicode chars that were converted from UTF-8
	 */

	length = (w - buf);
	result = (*env)->NewString(env, buf, length);
	ckfree((char*) buf);
    } else {
	result = (*env)->NewString(env, NULL, 0);
    }
    return result;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_CObject_incrRefCount --
 *
 *	Increment the reference count of the given object.
 *
 * Class:     tcl_lang_CObject
 * Method:    incrRefCount
 * Signature: (J)V
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

void JNICALL
Java_tcl_lang_CObject_incrRefCount(
    JNIEnv *env,		/* Java environment. */
    jclass class,		/* Handle to CObject class. */
    jlong obj)			/* Value of CObject.objPtr. */
{
    Tcl_Obj *objPtr = *(Tcl_Obj **) &obj;

    if (!objPtr) {
	jclass nullClass = (*env)->FindClass(env,
		"java/lang/NullPointerException");
	(*env)->ThrowNew(env, nullClass, "Invalid CObject.");
    }
    Tcl_IncrRefCount(objPtr);
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_CObject_decrRefCount --
 *
 *	Decrement the reference count for the given object.
 *
 * Class:     tcl_lang_CObject
 * Method:    decrRefCount
 * Signature: (J)V
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

void JNICALL Java_tcl_lang_CObject_decrRefCount(
    JNIEnv *env,		/* Java environment. */
    jclass class,		/* Handle to CObject class. */
    jlong obj)			/* Value of CObject.objPtr. */
{
    Tcl_Obj *objPtr = *(Tcl_Obj **) &obj;

    if (!objPtr) {
	jclass nullClass = (*env)->FindClass(env,
		"java/lang/NullPointerException");
	(*env)->ThrowNew(env, nullClass, "Invalid CObject.");
    }
    Tcl_DecrRefCount(objPtr);
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_CObject_makeRef --
 *
 *	Convert the Tcl_Obj into a TclObject.
 *
 * Class:     tcl_lang_CObject
 * Method:    makeRef
 * Signature: (JLtcl/lang/TclObject;)V
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Modifies the internal representation of the object, and
 *	adds a reference to the TclObject.
 *
 *----------------------------------------------------------------------
 */

void JNICALL
Java_tcl_lang_CObject_makeRef(
    JNIEnv *env,		/* Java environment. */
    jclass class,		/* Handle to CObject class. */
    jlong obj,			/* Value of CObject.objPtr. */
    jobject object)		/* Handle to the TclObject. */
{
    Tcl_Obj *objPtr = *(Tcl_Obj **) &obj;
    Tcl_ObjType *oldTypePtr;
    JavaInfo* jcache = JavaGetCache();

    if (!objPtr) {
	jclass nullClass = (*env)->FindClass(env,
		"java/lang/NullPointerException");
	(*env)->ThrowNew(env, nullClass, "Invalid CObject.");
    }

    /*
     * Free the old internalRep before setting the new one.
     */

    oldTypePtr = objPtr->typePtr;
    if ((oldTypePtr != NULL) && (oldTypePtr->freeIntRepProc != NULL)) {
	oldTypePtr->freeIntRepProc(objPtr);
    }

    objPtr->typePtr = &tclObjectType;
    objPtr->internalRep.twoPtrValue.ptr2
	= (VOID*) (*env)->NewGlobalRef(env, object);

    /*
     * Increment the reference count on the TclObject since this object
     * now represents and additional reference.
     */

    (*env)->CallVoidMethod(env, object, jcache->preserve);
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_CObject_newCObject --
 *
 *	Allocate a new Tcl_Obj with the given string rep.
 *
 * Class:     tcl_lang_CObject
 * Method:    newCObject
 * Signature: (Ljava/lang/String;)J
 *
 * Results:
 *	Returns the address of the new Tcl_Obj with refcount of 0.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

jlong JNICALL
Java_tcl_lang_CObject_newCObject(
    JNIEnv *env,		/* Java environment. */
    jclass class,		/* Handle to CObject class. */
    jstring string)		/* Initial string rep. */
{
    Tcl_Obj *objPtr;
    jlong obj;

    objPtr = Tcl_NewObj();
    if (string) {
	objPtr->bytes = JavaGetString(env, string, &objPtr->length);
    }
    *(Tcl_Obj **)&obj = objPtr;
    return obj;	
}

/*
 *----------------------------------------------------------------------
 *
 * JavaGetTclObject --
 *
 *	Retrieve the Java TclObject that shadows the given Tcl_Obj.
 *	Creates a new TclObject of type CObject that refers to the
 *	given Tcl_Obj, unless the Tcl_Obj is of type TclObject already.
 *
 * Results:
 *	Returns the TclObject associated with the Tcl_Obj.
 *
 * Side effects:
 *	May add a reference to the Tcl_Obj.  May allocate a new local
 *	reference.  Note that if this routine is not called as
 *	a result of a native method invocation, the caller is responsible
 *	for deleting the local reference explicitly.
 *
 *----------------------------------------------------------------------
 */

jobject
JavaGetTclObject(
    JNIEnv *env,
    Tcl_Obj *objPtr,		/* Object to get jobject for. */
    int *isLocalPtr)		/* 1 if returned handle is a local ref. */
{
    jobject object;
    jlong lvalue;
    JavaInfo* jcache = JavaGetCache();

    if (!objPtr) {
	return NULL;
    }

    if ((objPtr->typePtr == &tclObjectType)
	    || ((objPtr->typePtr == cmdTypePtr) &&
		    (objPtr->internalRep.twoPtrValue.ptr2) != NULL)) {
	/*
	 * This object is a reference to a TclObject, so we extract the
	 * jobject.
	 */

	object = (jobject)(objPtr->internalRep.twoPtrValue.ptr2);
	if (isLocalPtr) {
	    *isLocalPtr = 0;
	}
    } else {
	/*
	 * This object is of an unknown type, so we create a new TclObject
	 * with an internal rep of CObject that points to the Tcl_Obj *.
	 *
	 * Calls : TclObject tobj = CObject.newInstance(long objPtr);
	 */

	*(Tcl_Obj **)&lvalue = objPtr;
	object = (*env)->CallStaticObjectMethod(env, jcache->CObject,
		jcache->newCObjectInstance, lvalue);
	if (isLocalPtr) {
	    *isLocalPtr = 1;
	}
    }
    return object;    
}

/*
 *----------------------------------------------------------------------
 *
 * FreeJavaCmdInternalRep --
 *
 *	Free the internal rep for a java object.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Decrements the refcount on a java object, and frees it if
 *	the last reference is gone.
 *
 *----------------------------------------------------------------------
 */

static void
FreeJavaCmdInternalRep(
    Tcl_Obj *objPtr)
{
    jobject jobj = (jobject) objPtr->internalRep.twoPtrValue.ptr2;

    if (jobj) {
	FreeTclObject(objPtr);
    }
    (oldCmdType.freeIntRepProc)(objPtr);
}

/*
 *----------------------------------------------------------------------
 *
 * DupJavaCmdInternalRep --
 *
 *	Initialize the internal representation of a java Tcl_Obj to a
 *	copy of the internal representation of an existing java object. 
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	"dupPtr"s internal rep is set to the java object corresponding to
 *	"srcPtr"s internal rep and the refcount on the java object
 *	in incremented.
 *
 *----------------------------------------------------------------------
 */

static void
DupJavaCmdInternalRep(
    Tcl_Obj *srcPtr,
    Tcl_Obj *dupPtr)
{
    jobject jobj = (jobject) srcPtr->internalRep.twoPtrValue.ptr2;
    (oldCmdType.dupIntRepProc)(srcPtr, dupPtr);
    dupPtr->internalRep.twoPtrValue.ptr2 = jobj;
    if (jobj) {
	DupTclObject(srcPtr, dupPtr);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * SetJavaCmdFromAny --
 *
 *	Attempt to generate command object from an arbitrary type.
 *	This routine is a wrapper around the standard cmdName setFromAny
 *	procedure.  If the old type was TclObject, it copies the handle
 *	into the command type so that it can be restored later, and so
 *	the object can't be garbage collected.
 *
 * Results:
 *	The return value is a standard object Tcl result. If an error occurs
 *	during conversion, an error message is left in the interpreter's
 *	result unless "interp" is NULL.
 *
 * Side effects:
 *	If no error occurs, an object reference is stored as "objPtr"s
 *	internal representation, and the object's refcount is increased.
 *
 *----------------------------------------------------------------------
 */

static int
SetJavaCmdFromAny(
    Tcl_Interp *interp,
    Tcl_Obj *objPtr)
{
    int result;

    /*
     * Invoke the normal command type routine, but make sure
     * it doesn't free the java object.  Note that we have to
     * restore the ptr2 value after the conversion, since it gets
     * set to NULL by the setFromAnyProc.
     */

    if (objPtr->typePtr == &tclObjectType) {
	VOID *ptr2;
	if (objPtr->bytes == NULL) {
	    UpdateTclObject(objPtr);	
	}
	objPtr->typePtr = NULL;
	ptr2 = objPtr->internalRep.twoPtrValue.ptr2;
	result = (oldCmdType.setFromAnyProc)(interp, objPtr);
	objPtr->internalRep.twoPtrValue.ptr2 = ptr2;
    } else if ((objPtr->typePtr == cmdTypePtr)
	    && (objPtr->internalRep.twoPtrValue.ptr2 != NULL)) {
	jobject object = (jobject)(objPtr->internalRep.twoPtrValue.ptr2);
	JNIEnv *env = JavaGetEnv();
	JavaInfo* jcache = JavaGetCache();

	/*
	 * If we are converting from something that is already a java command
	 * reference we need to preserve the object handle so that it doesn't
	 * get freed as a side effect of updating the command cache.  Note
	 * that the new command may not refer to a java object, but we will
	 * maintain the extra reference anyway since it is difficult to
	 * detect this case.  The object reference will still be cleaned up
	 * when the Tcl object is free.
	 */

	object = (*env)->NewGlobalRef(env, object);
	(*env)->CallVoidMethod(env, object, jcache->preserve);
	result = (oldCmdType.setFromAnyProc)(interp, objPtr);
	if (result != TCL_OK) {
	    (*env)->CallVoidMethod(env, object, jcache->release);
	    (*env)->DeleteGlobalRef(env, object);
	} else {
	    objPtr->internalRep.twoPtrValue.ptr2 = (VOID*) object;
	}
    } else {
	result = (oldCmdType.setFromAnyProc)(interp, objPtr);

	/*
	 * Ensure that the ptr2 is null for non-java commands.
	 */

	if (result == TCL_OK) {
	    objPtr->internalRep.twoPtrValue.ptr2 = NULL;
	}
    }
    return result;
}

