/* 
 * javaInterp.c --
 *
 *	This file contains the native method implementations for the
 *	tcl.lang.Interp class.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: javaInterp.c,v 1.7.2.4 2000/08/27 05:09:00 mo Exp $
 */

#include "java.h"
#include "javaNative.h"

/*
 * Structure used to hold information about variable traces:
 */

typedef struct {
    int flags;			/* Operations for which Tcl command is
				 * to be invoked. */
    char *errMsg;		/* Error message returned from Tcl command,
				 * or NULL.  Malloc'ed. */
    jobject trace;		/* VarTrace object. */
} JavaTraceInfo;

/*
 * Declaractions for functions used only in this file.
 */

static void		JavaCmdDeleteProc(ClientData clientData);
static int		JavaCmdProc(ClientData clientData, Tcl_Interp *interp,
			    int objc, Tcl_Obj *CONST objv[]);
static char *		JavaTraceProc(ClientData clientData,
			    Tcl_Interp *interp, char *name1, char *name2,
			    int flags);
static void		ThrowNullPointerException(JNIEnv *env, char *msg);


/*
 *----------------------------------------------------------------------
 *
 * ThrowNullPointerException --
 *
 *	Generate a NullPointerException to indicate that a method was
 *	invoked on a dead Interp.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Throws a new exception in the Java VM.  Creates a local
 *	reference so this function can only be called from a native
 *	method implementation.
 *
 *----------------------------------------------------------------------
 */

static void
ThrowNullPointerException(
    JNIEnv *env,		/* Java environment pointer. */
    char *msg)			/* Message to include in exception. */
{
    jclass nullClass = (*env)->FindClass(env,
	    "java/lang/NullPointerException");
    if (!msg) {
	msg = "Invalid interpreter.";
    }
    (*env)->ThrowNew(env, nullClass, msg);
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_create --
 *
 *	Create a new Tcl interpreter.  This is the primary entry point
 *	if the Blend extension is loaded by Java.
 *
 * Class:     tcl_lang_Interp
 * Method:    create
 * Signature: ()J
 *
 * Results:
 *	Returns the interp pointer, or NULL if initialization failed.
 *
 * Side effects:
 *	May cause the global java structure to be initialized.
 *
 *----------------------------------------------------------------------
 */

jlong JNICALL
Java_tcl_lang_Interp_create(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj)		/* Handle to Interp object. */
{
    jlong lvalue;
    Tcl_Interp *interp;

    interp = Tcl_CreateInterp();
    if (JavaSetupJava(env, interp) != TCL_OK) {
	jclass err = (*env)->FindClass(env, "tcl/lang/TclRuntimeError");
	if (err) {
	    (*env)->ThrowNew(env, err, Tcl_GetStringResult(interp));
	}
	Tcl_DeleteInterp(interp);
	lvalue = 0;
    } else {
	*(Tcl_Interp**)&lvalue = interp;
    }
    return lvalue;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_init --
 *
 *	Initialize the given interpreter by calling Tcl_Init and
 *	Tclblend_Init.
 *
 * Class:     tcl_lang_Interp
 * Method:    init
 * Signature: (J)I
 *
 * Results:
 *	Returns TCL_OK on success, else TCL_ERROR.
 *
 * Side effects:
 *	Creates new commands, loads init.tcl.
 *
 *----------------------------------------------------------------------
 */

jint JNICALL
Java_tcl_lang_Interp_init(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj,		/* Handle to interp object. */
    jlong interpPtr)		/* Tcl_Interp pointer. */
{
    Tcl_Interp *interp = *(Tcl_Interp **)&interpPtr;
    jint result;

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return TCL_ERROR;
    }

    if (Tcl_Init(interp) != TCL_OK) {
	result = TCL_ERROR;
    } else {
	/*
	 * Set up the Blend package.
	 */

	interpObj = (*env)->NewGlobalRef(env, interpObj);
	result = JavaInitBlend(env, interp, interpObj);
    }

    return result;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_doDispose --
 *
 *	Delete the interpeter associated with this Interp class.
 *
 * Class:     tcl_lang_Interp
 * Method:    doDispose
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
Java_tcl_lang_Interp_doDispose(
    JNIEnv *env,		/* Java environment. */
    jclass interpClass,		/* Handle to Interp class. */
    jlong interpPtr)		/* Value of Interp.interpPtr. */
{
    Tcl_Interp *interp = *(Tcl_Interp **)&interpPtr;
    jobject interpObj;

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return;
    }
    /*
     * Release the Interp instance handle and then clear the assoc data
     * to avoid a recursive call when the interpreter is deleted.
     */

    interpObj = (jobject) Tcl_GetAssocData(interp, "java", NULL);
    (*env)->DeleteGlobalRef(env, interpObj);
    Tcl_SetAssocData(interp, "java", NULL, NULL);
    Tcl_DeleteInterp(interp);
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_eval --
 *
 *	Evaluate the given string.
 *
 * Class:     tcl_lang_Interp
 * Method:    eval
 * Signature: (Ljava/lang/String;I)V
 *
 * Results:
 *	A standard Tcl result.
 *
 * Side effects:
 *	Whatever the eval does.
 *
 *----------------------------------------------------------------------
 */

void JNICALL
Java_tcl_lang_Interp_eval(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj,		/* Handle to Interp object. */
    jstring string,		/* String to eval. */
    jint flags)			/* Evaluation flags. */
{
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);
    Tcl_Obj *objPtr;
    int result;
    jobject exception;

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return;
    } else if (!string) {
	ThrowNullPointerException(env, "No string to evaluate.");
	return;
    }

    objPtr = Tcl_NewObj();
    objPtr->bytes = JavaGetString(env, string, &objPtr->length);
    Tcl_IncrRefCount(objPtr);

    if (!flags) {
	result = Tcl_EvalObj(interp, objPtr);
    } else {
	result = Tcl_GlobalEvalObj(interp, objPtr);
    }

    exception = (*env)->ExceptionOccurred(env);
    (*env)->ExceptionClear(env);
    Tcl_DecrRefCount(objPtr);

    /*
     * Check to see if an exception is being thrown.  If so, let it
     * continue to propagate out to Java.  Otherwise convert a normal
     * Tcl error into an exception.
     */

    if (exception) {
	(*env)->Throw(env, exception);
    } else if (result != TCL_OK) {
	JavaThrowTclException(env, interp, result);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_getResult --
 *
 *	Returns the current interpreter result as a TclObject.
 *
 * Class:     tcl_lang_Interp
 * Method:    getResult
 * Signature: ()Ljava/lang/TclObject;
 *
 * Results:
 *	Returns a newly allocated TclObject.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

jobject JNICALL
Java_tcl_lang_Interp_getResult(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj)		/* Handle to Interp object. */
{
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);
    jobject obj;

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return 0;
    }

    obj = JavaGetTclObject(env, Tcl_GetObjResult(interp), NULL);
    return obj;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_setResult --
 *
 *	Sets the current interpreter result.
 *
 * Class:     tcl_lang_Interp
 * Method:    setResult
 * Signature: (Ltcl/lang/TclObject;)V
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
Java_tcl_lang_Interp_setResult(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj,		/* Handle to Interp object. */
    jobject result)		/* New result object. */
{
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return;
    }
    if (!result) {
	ThrowNullPointerException(env, "Invalid result object.");
	return;
    }
    Tcl_SetObjResult(interp, JavaGetTclObj(env, result));
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_resetResult --
 *
 *	Clears the result of the given interpreter.
 *
 * Class:     tcl_lang_Interp
 * Method:    resetResult
 * Signature: ()V
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
Java_tcl_lang_Interp_resetResult(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj)		/* Handle to Interp object. */
{
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return;
    }

    Tcl_ResetResult(interp);
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_setVar --
 *
 *	Set a variable to the given string.
 *
 * Class:     tcl_lang_Interp
 * Method:    setVar
 * Signature:  (Ljava/lang/String;Ljava/lang/String;Ltcl/lang/TclObject;I)Ltcl/lang/TclObject;
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	May create a new Tcl_Obj.
 *
 *----------------------------------------------------------------------
 */

jobject JNICALL
Java_tcl_lang_Interp_setVar(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj,		/* Interp object. */
    jstring part1Str,		/* If part2 is NULL, this is name of scalar
                                 * variable. Otherwise it is the name of
                                 * an array. */
    jstring part2Str,		/* Name of an element within an array, or
				 * NULL. */
    jobject value,		/* New value for variable. */
    jint flags)			/* Various flags that tell how to set value:
				 * any of GLOBAL_ONLY,
				 * NAMESPACE_ONLY, APPEND_VALUE,
				 * LIST_ELEMENT, LEAVE_ERR_MSG, or 
				 * PARSE_PART1. */
{
    Tcl_Obj *part1Ptr, *part2Ptr, *valuePtr, *resultPtr;
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);
    jobject obj;

    /*
     * Throw an exception if any of the objects are null.
     */

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return NULL;
    }
    if (!part1Str || !value) {
	ThrowNullPointerException(env, "setVar");
	return NULL;
    }

    /*
     * Get the Tcl_Obj that corresponds to the given TclObject.
     */

    valuePtr = JavaGetTclObj(env, value);

    part1Ptr = Tcl_NewObj();
    part1Ptr->bytes = JavaGetString(env, part1Str, &part1Ptr->length);
    Tcl_IncrRefCount(part1Ptr);

    if (part2Str) {
	part2Ptr = Tcl_NewObj();
	part2Ptr->bytes = JavaGetString(env, part2Str, &part2Ptr->length);
	Tcl_IncrRefCount(part2Ptr);
    } else {
	part2Ptr = NULL;
    }

    resultPtr = Tcl_ObjSetVar2(interp, part1Ptr, part2Ptr, valuePtr, flags);

    Tcl_DecrRefCount(part1Ptr);
    if (part2Str) {
	Tcl_DecrRefCount(part2Ptr);
    }

    /*
     * If the result is the same as the value, return the same TclObject.
     * Otherwise create a new TclObject for the return value.
     */

    if (!resultPtr) {
	JavaThrowTclException(env, interp, TCL_ERROR);
	obj = NULL;
    } else if (resultPtr == valuePtr) {
	obj = value;
    } else {
	obj = JavaGetTclObject(env, resultPtr, NULL);
    }
    return obj;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_getVar --
 *
 *	Get the value of the given variable.
 *
 * Class:     tcl_lang_Interp
 * Method:    getVar
 * Signature: (Ljava/lang/String;Ljava/lang/String;I)Ltcl/lang/TclObject;
 *
 * Results:
 *	Returns the TclObject that corresponds to the value of the
 *	variable.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

jobject JNICALL
Java_tcl_lang_Interp_getVar(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj,		/* Interp object. */
    jstring part1Str,		/* If part2 is NULL, this is name of scalar
                                 * variable. Otherwise it is the name of
                                 * an array. */
    jstring part2Str,		/* Name of an element within an array, or
				 * NULL. */
    jint flags)			/* Various flags that tell how to get value:
				 * any of GLOBAL_ONLY,
				 * NAMESPACE_ONLY, LEAVE_ERR_MSG, or 
				 * PARSE_PART1. */
{
    Tcl_Obj *part1Ptr, *part2Ptr, *valuePtr;
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);
    jobject obj;

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return NULL;
    }
    if (!part1Str) {
	ThrowNullPointerException(env, "getVar");
	return NULL;
    }

    part1Ptr = Tcl_NewObj();
    part1Ptr->bytes = JavaGetString(env, part1Str, &part1Ptr->length);
    Tcl_IncrRefCount(part1Ptr);

    if (part2Str) {
	part2Ptr = Tcl_NewObj();
	part2Ptr->bytes = JavaGetString(env, part2Str, &part2Ptr->length);
	Tcl_IncrRefCount(part2Ptr);
    } else {
	part2Ptr = NULL;
    }

    valuePtr = Tcl_ObjGetVar2(interp, part1Ptr, part2Ptr, flags);

    Tcl_DecrRefCount(part1Ptr);
    if (part2Str) {
	Tcl_DecrRefCount(part2Ptr);
    }

    if (!valuePtr) {
	JavaThrowTclException(env, interp, TCL_ERROR);
	obj = NULL;
    } else {
	obj = JavaGetTclObject(env, valuePtr, NULL);
    }

    return obj;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_unsetVar --
 *
 *	Unset the given variable.
 *
 * Class:     tcl_lang_Interp
 * Method:    unsetVar
 * Signature: (Ljava/lang/String;Ljava/lang/String;I)V
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
Java_tcl_lang_Interp_unsetVar(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj,		/* Interp object. */
    jstring part1Str,		/* If part2 is NULL, this is name of scalar
                                 * variable. Otherwise it is the name of
                                 * an array. */
    jstring part2Str,		/* Name of an element within an array, or
				 * NULL. */
    jint flags)			/* Various flags that tell how to get value:
				 * any of GLOBAL_ONLY,
				 * NAMESPACE_ONLY, LEAVE_ERR_MSG, or 
				 * PARSE_PART1. */
{
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);
    char *part1, *part2;
    int result;

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return;
    }
    if (!part1Str) {
	ThrowNullPointerException(env, "unsetVar");
	return;
    }

    part1 = JavaGetString(env, part1Str, NULL);
    part2 = (part2Str) ? JavaGetString(env, part2Str, NULL) : NULL;

    result = Tcl_UnsetVar2(interp, part1, part2, flags);

    ckfree(part1);
    if (part2) {
	ckfree(part2);
    }
    if (result != TCL_OK) {
	JavaThrowTclException(env, interp, result);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_traceVar --
 *
 *	Add a trace to a variable.
 *
 * Class:     tcl_lang_Interp
 * Method:    traceVar
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ltcl/lang/VarTrace;I)V
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
Java_tcl_lang_Interp_traceVar(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj,		/* Interp object. */
    jstring part1Str,		/* Name of scalar variable or array. */
    jstring part2Str,		/* Name of element within array;  null means
				 * trace applies to scalar variable or array
				 * as-a-whole. */
    jobject trace,		/* Object to notify when specified ops are
				 * invoked upon varName. */
    jint flags)			/* OR-ed collection of bits, including any
				 * of TCL_TRACE_READS, TCL_TRACE_WRITES,
				 * TCL_TRACE_UNSETS, TCL_GLOBAL_ONLY,
				 * TCL_NAMESPACE_ONLY and
				 * TCL_PARSE_PART1. */
{
    int result;
    JavaTraceInfo *traceInfo;
    char *part1, *part2;
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return;
    }
    if (!part1Str || !trace) {
	ThrowNullPointerException(env, "traceVar");
	return;
    }
    
    /*
     * Get the variable name.
     */

    part1 = JavaGetString(env, part1Str, NULL);
    part2 = (part2Str) ? JavaGetString(env, part2Str, NULL) : NULL;

    /*
     * Set the trace.  Note that we have to set TCL_TRACE_UNSETS in order
     * to get notification when the trace is deleted by the interpreter.
     */

    traceInfo = (JavaTraceInfo *) ckalloc(sizeof(JavaTraceInfo));
    traceInfo->flags = flags;
    traceInfo->errMsg = NULL;
    traceInfo->trace = (*env)->NewGlobalRef(env, trace);
    
    flags |= TCL_TRACE_UNSETS;

    result = Tcl_TraceVar2(interp, part1, part2, flags, JavaTraceProc,
	    (ClientData) traceInfo);

    /*
     * Release the strings and throw an exception if necessary.
     */

    ckfree(part1);
    if (part2) {
	ckfree(part2);
    }
    if (result != TCL_OK) {
	(*env)->DeleteGlobalRef(env, traceInfo->trace);
	ckfree((char *)traceInfo);
	JavaThrowTclException(env, interp, result);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_untraceVar --
 *
 *	Remove a trace from a variable.
 *
 * Class:     tcl_lang_Interp
 * Method:    untraceVar
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ltcl/lang/VarTrace;I)V
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
Java_tcl_lang_Interp_untraceVar(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj,		/* Interp object. */
    jstring part1Str,		/* Name of scalar variable or array. */
    jstring part2Str,		/* Name of element within array;  null means
				 * trace applies to scalar variable or array
				 * as-a-whole. */
    jobject trace,		/* Object associated with trace. */
    jint flags)			/* OR-ed collection of bits describing current
				 * trace, including any of TCL_TRACE_READS,
				 * TCL_TRACE_WRITES, TCL_TRACE_UNSETS,
				 * TCL_GLOBAL_ONLY, TCL_NAMESPACE_ONLY and
				 * TCL_PARSE_PART1. */
{
    char *part1, *part2;
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);
    ClientData clientData;
    JavaTraceInfo *tPtr;

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return;
    }
    if (!part1Str || !trace) {
	ThrowNullPointerException(env, "untraceVar");
	return;
    }
    
    /*
     * Get the variable name.
     */

    part1 = JavaGetString(env, part1Str, NULL);
    part2 = (part2Str) ? JavaGetString(env, part2Str, NULL) : NULL;

    /*
     * Search through all of our traces on this variable to
     * see if there's one with the given trace object.  If so, then
     * delete the first one that matches.
     */

    clientData = 0;
    while ((clientData = Tcl_VarTraceInfo2(interp, part1, part2, flags,
	    JavaTraceProc, clientData)) != 0) {
	tPtr = (JavaTraceInfo *) clientData;
	if ((*env)->IsSameObject(env, tPtr->trace, trace)) {
	    Tcl_UntraceVar2(interp, part1, part2, flags | TCL_TRACE_UNSETS,
		    JavaTraceProc, (ClientData) tPtr);
	    if (tPtr->errMsg != NULL) {
		ckfree(tPtr->errMsg);
	    }
	    (*env)->DeleteGlobalRef(env, tPtr->trace);
	    ckfree((char *) tPtr);
	}
    }

    /*
     * Release the strings.
     */

    ckfree(part1);
    if (part2) {
	ckfree(part2);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * JavaTraceProc --
 *
 *	This routine is the wrapper for any trace procedure implemented
 *	in Java.  It converts the command arguments into TclObject values
 *	and then invokes the cmdProc method on the appropriate object.
 *
 * Results:
 *	If no error occurred, returns NULL.
 *
 * Side effects:
 *	Whatever the trace does.
 *
 *----------------------------------------------------------------------
 */

static char *
JavaTraceProc(
    ClientData clientData,	/* Object handle. */
    Tcl_Interp *interp,		/* Current interpreter. */
    char *name1,		/* Name of scalar or array variable. */
    char *name2,		/* Element name or NULL. */
    int flags)			/* Operation flags. */
{
    JavaTraceInfo *tPtr = (JavaTraceInfo *) clientData;
    char *result;
    jstring name1Str, name2Str;
    jobject exception, interpObj;
    Tcl_SavedResult state;
    JNIEnv *env = JavaGetEnv();
    JavaInfo* jcache = JavaGetCache();

    result = NULL;
    if (tPtr->errMsg != NULL) {
	ckfree(tPtr->errMsg);
	tPtr->errMsg = NULL;
    }

    if ((tPtr->flags & flags) && !(flags & TCL_INTERP_DESTROYED)) {

	interpObj = (jobject) Tcl_GetAssocData(interp, "java", NULL);

	name1Str = (*env)->NewStringUTF(env, name1);
	name2Str = (name2 ? (*env)->NewStringUTF(env, name2)
		: NULL);

	/*
	 * Execute the command.  Be careful to save and restore both the
	 * string and object results from the interpreter passed to the
	 * trace object.  We discard any result generated by the object.
	 */

	Tcl_SaveResult(interp, &state);
	
	/*
	 * Invoke the command and check for an exception.
	 */

	(*env)->CallVoidMethod(env, tPtr->trace, jcache->traceProc,
		interpObj, name1Str, name2Str, flags);
	exception = (*env)->ExceptionOccurred(env);
	(*env)->ExceptionClear(env);

	(*env)->DeleteLocalRef(env, name1Str);
	if (name2Str) {
	    (*env)->DeleteLocalRef(env, name2Str);
	}

	if (exception) {
	    (*env)->DeleteLocalRef(env, exception);
	    (void) Tcl_GetStringResult(interp);
	    tPtr->errMsg = (char *)
		    ckalloc((unsigned) (strlen(interp->result) + 1));
	    strcpy(tPtr->errMsg, interp->result);
	    result = tPtr->errMsg;
	}

	Tcl_RestoreResult(interp, &state);
    }
    if (flags & TCL_TRACE_DESTROYED) {
	result = NULL;
	if (tPtr->errMsg != NULL) {
	    ckfree(tPtr->errMsg);
	}
	(*env)->DeleteGlobalRef(env, tPtr->trace);
	ckfree((char *) tPtr);
    }
    return result;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_createCommand --
 *
 *	Create a new Tcl command that is implemented by a Java object.
 *
 * Class:     tcl_lang_Interp
 * Method:    createCommand
 * Signature: (Ljava/lang/String;Ltcl/lang/Command;)V
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Creates a new JavaCommand object to wrap the Java object.
 *
 *----------------------------------------------------------------------
 */

void JNICALL
Java_tcl_lang_Interp_createCommand(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj,		/* Interp object. */
    jstring nameStr,		/* Name of command to create. */
    jobject cmd)		/* Object that implements the command. */
{
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);
    const char *name;

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return;
    }
    if (!nameStr || !cmd) {
	ThrowNullPointerException(env, "createCommand");
	return;
    }

    name = (*env)->GetStringUTFChars(env, nameStr, NULL);
    cmd = (*env)->NewGlobalRef(env, cmd);

    Tcl_CreateObjCommand(interp, (/*UNCONST*/ char *) name, JavaCmdProc,
	    (ClientData) cmd, JavaCmdDeleteProc);

    (*env)->ReleaseStringUTFChars(env, nameStr, name);
}

/*
 *----------------------------------------------------------------------
 *
 * JavaCmdDeleteProc --
 *
 *	Invokes the dispose() method on the command object if the
 *	object implements the Disposable interfaces. 
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Frees the global reference for the command object.
 *
 *----------------------------------------------------------------------
 */

static void
JavaCmdDeleteProc(
    ClientData clientData)
{
    jobject cmd = (jobject)clientData;
    JNIEnv *env = JavaGetEnv();
    JavaInfo* jcache = JavaGetCache();

    if ((*env)->IsInstanceOf(env, cmd, jcache->CommandWithDispose)) {
	(*env)->CallVoidMethod(env, cmd, jcache->disposeCmd);
    }
    (*env)->DeleteGlobalRef(env, (jobject)clientData);
}

/*
 *----------------------------------------------------------------------
 *
 * JavaCmdProc --
 *
 *	This routine is the wrapper for any Tcl command implemented in
 *	Java.  It converts the command arguments into TclObject values
 *	and then invokes the cmdProc method on the appropriate object.
 *
 * Results:
 *	A standard Tcl result.
 *
 * Side effects:
 *	Whatever the command does.
 *
 *----------------------------------------------------------------------
 */

static int
JavaCmdProc(
    ClientData clientData,	/* Command object handle. */
    Tcl_Interp *interp,		/* Current interpreter. */
    int objc,			/* Number of arguments. */
    Tcl_Obj *CONST objv[])	/* Argument objects. */
{
    jobject cmd = (jobject)clientData;
    jarray args;
    jobject value, exception, interpObj;
    int i, result;
    JNIEnv *env = JavaGetEnv();
    JavaInfo* jcache = JavaGetCache();

    interpObj = (jobject) Tcl_GetAssocData(interp, "java", NULL);

    /*
     * Construct the argument array.
     */

    args = (*env)->NewObjectArray(env, objc, jcache->TclObject, NULL);
    for (i = 0; i < objc; i++) {
	int isLocal;

	value = JavaGetTclObject(env, objv[i], &isLocal);
	(*env)->SetObjectArrayElement(env, args, i, value);

	/*
	 * Delete a newly created local ref expliticly since it may have
	 * been created outside of the VM and so will never be garbage
	 * collected.
	 */

	if (isLocal) {
	    (*env)->DeleteLocalRef(env, value);
	}
    }

    /*
     * Invoke the command by calling Interp.callCommand().  Be sure to
     * leave the monitor since we are assuming nothing about the state
     * of the world after this call.
     */

    result = (*env)->CallIntMethod(env, interpObj,
	    jcache->callCommand, cmd, args);
    exception = (*env)->ExceptionOccurred(env);

    if (exception) {
	(*env)->ExceptionClear(env);
    }

    (*env)->DeleteLocalRef(env, args);

    if (exception) {
	result = TCL_ERROR;
	(*env)->Throw(env, exception);
    }

    return result;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_deleteCommand --
 *
 *	Delete the given command.
 *
 * Class:     tcl_lang_Interp
 * Method:    deleteCommand
 * Signature: (Ljava/lang/String;)I
 *
 * Results:
 *	Returns -1 if no command was found, else returns 0.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

jint JNICALL
Java_tcl_lang_Interp_deleteCommand(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj,		/* Interp object. */
    jstring nameStr)		/* Name of command to create. */
{
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);
    const char *name;
    int result;

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return -1;
    }
    if (!nameStr) {
	ThrowNullPointerException(env, "deleteCommand");
	return -1;
    }

    name = (*env)->GetStringUTFChars(env, nameStr, NULL);
    result = Tcl_DeleteCommand(interp, (/*UNCONST*/ char*) name);
    (*env)->ReleaseStringUTFChars(env, nameStr, name);

    return (jint) result;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_getCommand --
 *
 *	Return a tcl.lang.Command object for a given Tcl command.
 *	This currently only works for Tcl commands implemented in Java.
 *
 * Class:     tcl_lang_Interp
 * Method:    getCommand
 * Signature: (Ljava/lang/String;)Ltcl/lang/Command;
 *
 * Results:
 *	The command procedure of the given command, or null if
 *      the command doesn't exist.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

jobject JNICALL
Java_tcl_lang_Interp_getCommand(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj,		/* Interp object. */
    jstring nameStr)		/* Name of command to look for. */
{
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);
    const char *name;
    jobject cmd = NULL;
    Tcl_CmdInfo cmdInfo;

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return NULL;
    }
    if (!nameStr) {
	ThrowNullPointerException(env, "getCommand");
	return NULL;
    }

    name = (*env)->GetStringUTFChars(env, nameStr, NULL);    
    if (Tcl_GetCommandInfo(interp, (/*UNCONST*/ char*) name, &cmdInfo)) {
        /* If the command exists, find out if it is a Tcl command implemeneted in Java */
	
        if (cmdInfo.isNativeObjectProc &&
                cmdInfo.objProc == JavaCmdProc) {
            cmd = (jobject) cmdInfo.objClientData;
	}

        /*
	 * FIXME: Figure out some way to wrap a Tcl command
	 * in a tcl.lang.Command interface so that it can be
	 * invoked directly from Java. We would need to deal
	 * with deletion of commands which might be tricky
	 */
    }
    (*env)->ReleaseStringUTFChars(env, nameStr, name);
    return cmd;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_commandComplete --
 *
 *	Determine if the String cmd is a complete command.
 *
 * Class:     tcl_lang_Interp
 * Method:    commandComplete
 * Signature: (Ljava/lang/String;)Z
 *
 * Results:
 *	Returns true if the command is complete
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

jboolean JNICALL
Java_tcl_lang_Interp_commandComplete(
    JNIEnv *env,		/* Java environment. */
    jclass interpClass,		/* Handle to Interp class. */
    jstring cmdStr)		/* Command string to test */
{
    const char *cmd;
    jboolean result;

    if (!cmdStr) {
	ThrowNullPointerException(env, NULL);
	return JNI_FALSE;
    }

    cmd = (*env)->GetStringUTFChars(env, cmdStr, NULL);
    result = (Tcl_CommandComplete((/*UNCONST*/ char*) cmd)
	    ? JNI_TRUE : JNI_FALSE);
    (*env)->ReleaseStringUTFChars(env, cmdStr, cmd);

    return result;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_setErrorCode
 *
 *	Set the errorCode variable to the given string.
 *
 * Class:     tcl_lang_Interp
 * Method:    setErrorCode
 * Signature: (Ltcl/lang/TclObject;)V
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
Java_tcl_lang_Interp_setErrorCode(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj,		/* Handle to Interp object. */
    jobject code)		/* TclObject to use as errorCode value. */
{
    Tcl_Obj *errorObjPtr;
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return;
    }
    if (!code) {
	ThrowNullPointerException(env, "setErrorCode");
	return;
    }

    errorObjPtr = JavaGetTclObj(env, code);
    Tcl_SetObjErrorCode(interp, errorObjPtr);
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_addErrorInfo --
 *
 *	Add the given string to the errorInfo.
 *
 * Class:     tcl_lang_Interp
 * Method:    addErrorInfo
 * Signature: (Ljava/lang/String;)V
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
Java_tcl_lang_Interp_addErrorInfo(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj,		/* Handle to Interp object. */
    jstring string)		/* String to add to errorInfo. */
{
    char *str;
    int length;
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return;
    }
    if (!string) {
	ThrowNullPointerException(env, "addErrorInfo");
	return;
    }

    str = JavaGetString(env, string, &length);
    Tcl_AddObjErrorInfo(interp, str, length);
    ckfree(str);
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_backgroundError --
 *
 *	This procedure is invoked to handle errors that occur in Tcl
 *	commands that are invoked in "background" (e.g. from event or
 *	timer bindings) and are implemented in Java.
 *
 * Class:     tcl_lang_Interp
 * Method:    backgroundError
 * Signature: ()V
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The comamnd "bgerror" is invoked later as an idle handler to
 *	process the error, passing it the error message.  If that fails,
 *	then an error message is output on stderr.
 *
 *----------------------------------------------------------------------
 */

void JNICALL
Java_tcl_lang_Interp_backgroundError(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj)		/* Handle to Interp object. */
{
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return;
    }
    Tcl_BackgroundError(interp);
}

/*
 *----------------------------------------------------------------------
 *
 * JavaGetInterp --
 *
 *	Retrieve the C interpreter pointer from a Java Interp object.
 *
 * Results:
 *	Returns the Tcl_Interp *, or NULL.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

Tcl_Interp *
JavaGetInterp(
    JNIEnv *env,		/* Java VM environment. */
    jobject interpObj)		/* Interp object. */
{
    jlong interpPtr;
    Tcl_Interp *interp;
    JavaInfo* jcache = JavaGetCache();

    if (!interpObj) {
	return NULL;
    }

    /*
     * Get the Tcl_Interp * from the Interp object.
     */

    interpPtr = (*env)->GetLongField(env, interpObj, jcache->interpPtr);

    /*
     * Copy the pointer out of the jlong.  We have to do it this way since the
     * jlong may be a structure.
     */

    interp = *(Tcl_Interp**)&interpPtr;
    return interp;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_pkgProvide --
 *
 *	Declare a particular version of a package is present
 *	in the interp.
 *
 * Class:     tcl_lang_Interp
 * Method:    pkgProvide
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 *
 * Results:
 *	Normally does nothing; if there is already another version
 *	of the package loaded then an error is raised.
 *
 * Side effects:
 *	The interpreter remembers that this package is available,
 *	so that no other version of the package may be provided for
 *	the interpreter.
 *
 *----------------------------------------------------------------------
 */

void JNICALL
Java_tcl_lang_Interp_pkgProvide(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj,		/* Interp object. */
    jstring nameStr,
    jstring versionStr)
{
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);
    char *namePtr;
    char *versionPtr;
    int result;
    
    /*
     * Throw an exception if any of the objects are null.
     */

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return;
    }
    if (!nameStr || !versionStr) {
	ThrowNullPointerException(env, "pkgProvide");
	return;
    }

    namePtr = JavaGetString(env, nameStr, NULL);
    versionPtr = JavaGetString(env, versionStr, NULL);
    
    /*
     * Call the Tcl function.
     */

    result = Tcl_PkgProvide(interp, namePtr, versionPtr);

    ckfree(namePtr);
    ckfree(versionPtr);

    if (result != TCL_OK) {
	JavaThrowTclException(env, interp, result);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Interp_pkgRequire --
 *
 *	Invokes a Tcl script to provide a package.
 *
 * Class:     tcl_lang_Interp
 * Method:    pkgRequire
 * Signature: (Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;
 *
 * Results:
 *	If successful, returns the version string for the currently
 *	provided version of the package, or generates a TclException
 *	if the package couldn't be loaded.
 *
 * Side effects:
 *	The script from some previous "package ifneeded" command may
 *	be invoked to provide the package.
 *
 *----------------------------------------------------------------------
 */

jstring JNICALL
Java_tcl_lang_Interp_pkgRequire(
    JNIEnv *env,		/* Java environment. */
    jobject interpObj,		/* Interp object. */
    jstring nameStr,
    jstring versionStr,
    jboolean exact)
{
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);
    char *namePtr;
    char *versionPtr;
    char *resultPtr;
    int flag;
    jstring string;

    /*
     * Throw an exception if any of the objects are null.
     */

    if (!interp) {
	ThrowNullPointerException(env, NULL);
	return NULL;
    }
    if (!nameStr || !versionStr) {
	ThrowNullPointerException(env, "pkgRequire");
	return NULL;
    }

    namePtr = JavaGetString(env, nameStr, NULL);
    versionPtr = JavaGetString(env, versionStr, NULL);
    flag = (exact == JNI_TRUE) ? 1 : 0;
    
    /*
     * Call the Tcl function.
     */

    resultPtr = Tcl_PkgRequire(interp, namePtr, versionPtr, flag);

    ckfree(namePtr);
    ckfree(versionPtr);
    
    if (resultPtr == NULL) {
	JavaThrowTclException(env, interp, TCL_ERROR);
	string = NULL;
    } else {
	string = (*env)->NewStringUTF(env, resultPtr);
    }
    return string;
}

