/* 
 * javaCmd.c --
 *
 *	This file contains the Tcl command procedures for the
 *	TclJava package.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 *
 * RCS: @(#) $Id: javaCmd.c,v 1.2 1999/03/02 15:41:19 hylands Exp $
 */

/*
Portions of this file are

Copyright (c) 1998 The Regents of the University of California.
All rights reserved.

Permission is hereby granted, without written agreement and without
license or royalty fees, to use, copy, modify, and distribute this
software and its documentation for any purpose, provided that the above
copyright notice and the following two paragraphs appear in all copies
of this software.

IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY 
FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES 
ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF 
THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF 
SUCH DAMAGE.

THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
ENHANCEMENTS, OR MODIFICATIONS.

 */

#include "string.h"
#include "java.h"
#include "javaNative.h"
#include <stdlib.h>
#include <stdarg.h>
#include <errno.h>

extern void panic(char *, ...);

/*
 * Exported state variables.
 */

JavaInfo java;		/* Cached class & method ids. */

/*
 * The following pointer is used to keep track of the current Java
 * thread information.  It is set on each entry from Java and is restored
 * to its previous value before returning to Java.  This discipline will
 * handle nested calls between Tcl and Java.  The initial value will
 * be null if the Blend pacakge is initialized from Java, otherwise it
 * will contain the environment for the default thread.
 */

static JNIEnv *currentEnv = NULL; /* JNI pointer for current thread. */

/*
 * The following variable contains the pointer to the current Java VM,
 * if it was created or attached to by Tcl.
 */

static JavaVM *javaVM = NULL;

/*
 * This flag indicates whether the Blend module has been initialized.
 */

static int initialized = 0;

/*
 * The following array contains the class names and jclass pointers for
 * all of the classes used by this module.  It is used to initialize
 * the java structure's jclass slots.
 */

static struct {
    jclass *addr;		/* Where to store jclass. */
    char *name;			/* Name of class to load. */
} classes[] = {
    { &java.Object, "java/lang/Object" },
    { &java.Interp, "tcl/lang/Interp" },
    { &java.Command, "tcl/lang/Command" },
    { &java.TclObject, "tcl/lang/TclObject" },
    { &java.TclException, "tcl/lang/TclException" },
    { &java.CommandWithDispose, "tcl/lang/CommandWithDispose" },
    { &java.CObject, "tcl/lang/CObject" },
    { &java.Extension, "tcl/lang/Extension" },
    { &java.VarTrace, "tcl/lang/VarTrace" },
    { &java.Void, "java/lang/Void" },
    { &java.BlendExtension, "tcl/lang/BlendExtension" },
    { &java.Notifier, "tcl/lang/Notifier" },
    { &java.NativeLock, "tcl/lang/NativeLock" },
    { &java.IdleHandler, "tcl/lang/IdleHandler" },
    { &java.TimerHandler, "tcl/lang/TimerHandler" },
    { NULL, NULL }
};

/*
 * The following array contains the information needed to load the jmethodID
 * pointers that this module uses.
 */

static struct {
    jmethodID *addr;		/* Where to store method id. */
    char *name;			/* Name of method to load. */
    jclass *class;		/* Where to find class id. */
    char *sig;			/* Signature of method. */
    int isStatic;		/* 1 if method is static. */
} methods[] = {
    { &java.toString, "toString", &java.Object,
      "()Ljava/lang/String;", 0 },
    { &java.callCommand, "callCommand", &java.Interp,
      "(Ltcl/lang/Command;[Ltcl/lang/TclObject;)I", 0 },
    { &java.dispose, "dispose", &java.Interp, "()V", 0 },
    { &java.interpC, "<init>", &java.Interp, "(J)V", 0 },
    { &java.cmdProc, "cmdProc", &java.Command,
      "(Ltcl/lang/Interp;[Ltcl/lang/TclObject;)V", 0 },
    { &java.disposeCmd, "disposeCmd", &java.CommandWithDispose, "()V", 0 },
    { &java.newCObjectInstance, "newInstance", &java.CObject,
      "(J)Ltcl/lang/TclObject;", 1 },
    { &java.preserve, "preserve", &java.TclObject, "()V", 0},
    { &java.release, "release", &java.TclObject, "()V", 0},
    { &java.getInternalRep, "getInternalRep", &java.TclObject,
      "()Ltcl/lang/InternalRep;", 0},
    { &java.init, "init", &java.Extension, "(Ltcl/lang/Interp;)V", 0},
    { &java.blendC, "<init>", &java.BlendExtension, "()V", 0},
    { &java.traceProc, "traceProc", &java.VarTrace,
      "(Ltcl/lang/Interp;Ljava/lang/String;Ljava/lang/String;I)V", 0},
    { &java.serviceEvent, "serviceEvent", &java.Notifier, "(I)I", 0},
    { &java.hasEvents, "hasEvents", &java.Notifier, "()Z", 0},
    { &java.invokeIdle, "invoke", &java.IdleHandler, "()V", 0},
    { &java.invokeTimer, "invoke", &java.TimerHandler, "()V", 0},
    { NULL, NULL, NULL, NULL, 0 }
};

/*
 * The following array contains signature information for fields used
 * by this module.
 */

static struct {
    jfieldID *addr;		/* Where to store method id. */
    char *name;			/* Name of method to load. */
    jclass *class;		/* Where to find class id. */
    char *sig;			/* Signature of method. */
} fields[] = {
    { &java.interpPtr, "interpPtr", &java.Interp, "J" },
    { &java.objPtr, "objPtr", &java.CObject, "J" },
    { NULL, NULL, NULL, NULL }
};

/*
 * Declarations of functions used only in this file.
 */

static int		ToString(JNIEnv *env, Tcl_Obj *objPtr, jobject obj);

/*
 *----------------------------------------------------------------------
 *
 * DllEntryPoint --
 *
 *	This wrapper function is used by Windows to invoke the
 *	initialization code for the DLL.  If we are compiling
 *	with Visual C++, this routine will be renamed to DllMain.
 *	routine.
 *
 * Results:
 *	Returns TRUE;
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

#ifdef __WIN32__
BOOL APIENTRY
DllEntryPoint(
    HINSTANCE hInst,		/* Library instance handle. */
    DWORD reason,		/* Reason this function is being called. */
    LPVOID reserved)		/* Not used. */
{
    return TRUE;
}
#endif

/*
 *----------------------------------------------------------------------
 *
 * Tclblend_Init --
 *
 *	This procedure initializes the Java package in an unsafe interp.
 *	This is the initial entry point if this module is being loaded
 *	from Tcl.
 *
 * Results:
 *	A standard Tcl result.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

EXPORT(int,Tclblend_Init)(
    Tcl_Interp *interp)
{
    jlong lvalue;
    jobject interpObj, local;
    JNIEnv *env = JavaGetEnv(interp);

    /*
     * Check to see if initialization of the Java VM failed.
     */

    if (env == NULL) {
	return TCL_ERROR;
    }

    /*
     * Make sure the global VM information is properly intialized.
     */

    if (!initialized) {
	if (JavaSetupJava(env, interp) != TCL_OK) {
	    return TCL_ERROR;
	}
	/*
	 * Since we entered from C, we need to grab the global monitor
	 * right away.  We will give it up whenever we enter Java code
	 * in a safe place.
	 */

	(*env)->MonitorEnter(env, java.NativeLock);
    }

    /*
     * Allocate a new Interp instance to wrap this interpreter.
     */

    *(Tcl_Interp**)&lvalue = interp;
    local = (*env)->NewObject(env, java.Interp,
	    java.interpC, lvalue);
    if (!local) {
	Tcl_Obj *obj;
	jobject exception = (*env)->ExceptionOccurred(env);
	if (exception) {
	    (*env)->ExceptionClear(env);
	    obj = Tcl_GetObjResult(interp);
	    ToString(env, obj, exception);
	    (*env)->Throw(env, exception);
	    (*env)->DeleteLocalRef(env, exception);
	}
	return TCL_ERROR;
    }
    interpObj = (*env)->NewGlobalRef(env, local);
    (*env)->DeleteLocalRef(env, local);
    return JavaInitBlend(env, interp, interpObj);
}

/*
 *----------------------------------------------------------------------
 *
 * appendClasspathMessage --
 *
 * Append information about the CLASSPATH to the Tcl results.
 * Call this function when something goes wrong so that the user
 * can attempt a fix.
 */
static void 
appendClasspathMessage(
    Tcl_Interp *interp)
{
#ifdef JDK1_2
   if (getenv("CLASSPATH")) {
       Tcl_AppendResult(interp,
                        "Currently, the CLASSPATH environment variable ",
                        "is set to:\n",
                        getenv("CLASSPATH"), NULL);
  } else {
       Tcl_AppendResult(interp,      
                        "Currently, the CLASSPATH environment variable ",
                        "is not set.", NULL);
  }
#else
    JDK1_1InitArgs vm_args;
    memset(&vm_args, 0, sizeof(vm_args));
    vm_args.version = 0x00010001;
    JNI_GetDefaultJavaVMInitArgs(&vm_args);
       Tcl_AppendResult(interp,
        "Currently, the CLASSPATH environment variable ",
        "is set to:\n",
        getenv("CLASSPATH"), 
        "\nThe JVM currently is using the following classpath:",
        vm_args.classpath, NULL);
#endif
}
/*
 *----------------------------------------------------------------------
 *
 * JavaGetEnv --
 *
 *	Retrieve the JNI environment for the current thread.
 *
 * Results:
 *	Returns the JNIEnv pointer for the current thread.  Returns
 *	NULL on error with a message left in the interpreter result.
 *
 * Side effects:
 *	May create or attach to a new JavaVM if this is the first time
 *	the JNIEnv is fetched from outside of a Java callback.
 *
 *----------------------------------------------------------------------
 */

#ifdef JDK1_2


#define JAVA_CLASS_PATH_ARG "-Djava.class.path="

TCLBLEND_EXTERN JNIEnv *
JavaGetEnv(
    Tcl_Interp *interp)		/* Interp for error reporting. */
{
    jsize nVMs;
    char *path;
    int size, maxOptions = 2;
    JavaVMOption *options;
    JavaVMInitArgs vm_args;

    if (currentEnv != NULL) {
	return currentEnv;
    }

    /*
     * Check to see if the current process already has a Java VM.  If so,
     * attach to it, otherwise create a new one.
     */

    if (JNI_GetCreatedJavaVMs(&javaVM, 1, &nVMs) < 0) {
	Tcl_AppendResult(interp, "JNI_GetCreatedJavaVMs failed", NULL);
	return NULL;
    }

    if (nVMs == 0) {

        options = (JavaVMOption *) ckalloc(sizeof(JavaVMOption) * maxOptions);
        vm_args.version = 0x00010002;
        vm_args.options = options;
        vm_args.ignoreUnrecognized= 1;
        vm_args.nOptions = 0;

	/*
	 * Add the classpath as a prefix to the default classpath.
	 */
         
	path = getenv("CLASSPATH");
	if (path) {
	    size = strlen(path) + strlen(JAVA_CLASS_PATH_ARG);
            options[0].optionString = ckalloc(size+2);
            vm_args.nOptions++;
	    strcpy(options[0].optionString, JAVA_CLASS_PATH_ARG);
	    strcat(options[0].optionString, path);
	    options[0].extraInfo = (void *)NULL;
	}

        
        /*
         * If the global tcl_variable tclblend_init is set, then pass its
         * value to the JVM. 
         * If the value of the string is "help", then initialization
         * of the JVM does not occur and a usage string is returned.
         */

        if ((options[vm_args.nOptions].optionString = 
                 (void *) Tcl_GetVar(interp, "tclblend_init",
                            TCL_GLOBAL_ONLY)) != NULL) {
            if ( !strcmp((char *)options[vm_args.nOptions].optionString, "help")) {
                Tcl_AppendResult(interp,
    "The value of the global tcl variable 'tclblend_init' is passed to the\n "
    "Java virtual machine upon initialization.\n "
    "Example values include:\n"
    "  -Djava.compiler=NONE   - disable Just In Time Compiler\n"
    "  -Djava.library.path=c:\\jdk\\lib\\tools.jar - set native library path\n"
    "  -verbose:jni           - print debugging messages\n"
    "\nFor -verbose, the value should be a string with one or\n"
    "more comma separated names (i.e. class,jni).  In JDK1.2,\n"
    "the standard names are: class, gc, jni\n"
    "To see what other options are available, run 'java -help'.\n"
    "Tcl Blend only: If the value is 'help', then JVM initialization stop\n",
    "and this message is returned.",
                                NULL);
                return NULL;
            }
	    options[vm_args.nOptions].extraInfo = (void *)NULL;
            vm_args.nOptions++;

#ifdef DEBUG
            {
            int i;
            fprintf(stderr, "Tcl Blend debug: java_debug set. javaCmd.c: \n"
                " vm_args.version: %x\n"
                " vm_args.nOptions: %d\n",
                vm_args.version, vm_args.nOptions);
            for( i = 0; i < maxOptions; i++) {
                fprintf(stderr, 
                    " options[%d].optionString = '%s', "
                    " options.[%d].extraInfo = '%s'\n",
                    i, options[i].optionString, i, 
                    options[i].extraInfo ? (char *)options[i].extraInfo : "NULL");
            }
            }
#endif
           
        }       

	if (JNI_CreateJavaVM(&javaVM, (void **) &currentEnv, &vm_args) < 0){
	    Tcl_AppendResult(interp, "JNI_CreateJavaVM failed", NULL);
	    return NULL;
	}
        
    } else {
	if ((*javaVM)->AttachCurrentThread(javaVM, (void **) &currentEnv, (long)NULL) != 0) {
	    Tcl_AppendResult(interp, "AttachCurrentThread failed", NULL);
	    return NULL;
	}
    }
    return currentEnv;
}
#else /* JDK1_2 */
TCLBLEND_EXTERN JNIEnv *
JavaGetEnv(
    Tcl_Interp *interp)		/* Interp for error reporting. */
{
    jsize nVMs;
    char *path, *newPath;
    int oldSize, size;
    JDK1_1InitArgs vm_args;

    if (currentEnv != NULL) {
	return currentEnv;
    }

    /*
     * Check to see if the current process already has a Java VM.  If so,
     * attach to it, otherwise create a new one.
     */

    if (JNI_GetCreatedJavaVMs(&javaVM, 1, &nVMs) < 0) {
	Tcl_AppendResult(interp, "JNI_GetCreatedJavaVMs failed", NULL);
	return NULL;
    }

    if (nVMs == 0) {
	    
	memset(&vm_args, 0, sizeof(vm_args));
        vm_args.version = 0x00010001;
	JNI_GetDefaultJavaVMInitArgs(&vm_args);
	
#ifdef INCREASE_STACK_SIZE
        vm_args.nativeStackSize = vm_args.nativeStackSize *4;
        vm_args.javaStackSize = vm_args.javaStackSize *4;
        vm_args.minHeapSize = vm_args.minHeapSize *4;
        vm_args.maxHeapSize = vm_args.maxHeapSize *4;
        fprintf(stderr,"Tcl Blend debug: vm_args: "
                "nativeStackSize = %d  javaStackSize = %d\n"
                "minHeapSize = %d      maxHeapSize = %d\n",
                vm_args.nativeStackSize, vm_args.javaStackSize,
                vm_args.minHeapSize, vm_args.maxHeapSize);
#endif
	/*
	 * Add the classpath as a prefix to the default classpath.
	 */
	path = getenv("CLASSPATH");
	if (path && vm_args.classpath) {
	    oldSize = strlen(path);
	    size = oldSize + strlen(vm_args.classpath);
	    newPath = ckalloc(size+2);
	    strcpy(newPath, path);
#ifdef __WIN32__
	    newPath[oldSize] = ';';
#else
	    newPath[oldSize] = ':';
#endif
	    strcpy(newPath+oldSize+1, vm_args.classpath);
	    vm_args.classpath = newPath;
	} else if (path) {
	    vm_args.classpath = path;
	}
#ifdef JDK1_2
	if (JNI_CreateJavaVM(&javaVM, (void **) &currentEnv, &vm_args) < 0) {
#else
	if (JNI_CreateJavaVM(&javaVM, &currentEnv, &vm_args) < 0) {
#endif
	    Tcl_AppendResult(interp, "JNI_CreateJavaVM() failed.\n"
                                     "Perhaps your CLASSPATH includes a "
                                     "classes.zip file for a version other\n"
                                     "the one Tcl Blend was compiled with?\n", 
                                     NULL);
            appendClasspathMessage(interp);
	    return NULL;
	}
    } else {
#ifdef JDK1_2
	if ((*javaVM)->AttachCurrentThread(javaVM, (void **)&currentEnv, (long) NULL) != 0) {
#else
	if ((*javaVM)->AttachCurrentThread(javaVM, &currentEnv, NULL) != 0) {
#endif
	    Tcl_AppendResult(interp, "AttachCurrentThread failed", NULL);
	    return NULL;
	}
    }
    return currentEnv;
}
#endif /* JDK1_2 */

/*
 *----------------------------------------------------------------------
 *
 * JavaSetEnv --
 *
 *	Update the currentEnv pointer.  This routine should be called
 *	whenever a native method is invoked in order to guarantee that
 *	the right JNI pointer is used for creating objects and
 *	throwing exceptions.  The returned value should be restored
 *	before returning control to Java.
 *
 * Results:
 *	Returns the previous value of the currentEnv pointer.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

TCLBLEND_EXTERN JNIEnv *
JavaSetEnv(
    JNIEnv *newEnv)		/* New JNI handle. */
{
    JNIEnv *oldEnv = currentEnv;
    currentEnv = newEnv;
    return oldEnv;
}

/*
 *----------------------------------------------------------------------
 *
 * JavaInitBlend --
 *
 *	Create the commands in the Blend extension.
 *
 * Results:
 *	Returns TCL_OK on success, else TCL_ERROR.
 *
 * Side effects:
 *	Invokes BlendExtension.init() and adds assoc data.
 *
 *----------------------------------------------------------------------
 */

TCLBLEND_EXTERN int
JavaInitBlend(
    JNIEnv *env,		/* Java environment. */
    Tcl_Interp *interp,		/* Interpreter to intialize. */
    jobject interpObj)		/* Handle to Interp object. */
{
    jobject blend;
    int result;
    
    /*
     * Associate the interpreter data with the interp object.
     */
	
    Tcl_SetAssocData(interp, "java", JavaInterpDeleted,
	    (ClientData)interpObj);

    /*
     * Initialize the BlendExtension.
     */

    blend = (*env)->NewObject(env, java.BlendExtension, java.blendC);
    (*env)->CallVoidMethod(env, blend, java.init, interpObj);
    if ((*env)->ExceptionOccurred(env)) {
	result = TCL_ERROR;
    } else {
	result = TCL_OK;
    }
    (*env)->DeleteLocalRef(env, blend);
    return result;
}

/*
 *----------------------------------------------------------------------
 *
 * JavaInterpDeleted --
 *
 *	This routine is called when an interpreter that is using
 *	this module is deleted.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Removes the internal global reference and sets the
 *	Interp.interpPtr to 0.
 *
 *----------------------------------------------------------------------
 */

void
JavaInterpDeleted(
    ClientData clientData,	/* Pointer to Java info. */
    Tcl_Interp *interp)		/* Interpreter being deleted. */
{
    jobject interpObj = (jobject) clientData;
    JNIEnv *env = JavaGetEnv(interp);

    /*
     * Set the Interp.interpPtr field to 0 so any further attempts to use
     * this interpreter from Java will fail and so Interp.dispose() won't
     * try to delete the interpreter again.  
     */

    (*env)->SetLongField(env, interpObj, java.interpPtr, 0);

    /*
     * Call Interp.dispose() to release any state kept in Java.
     */

    (*env)->CallVoidMethod(env, interpObj, java.dispose);
    (*env)->DeleteGlobalRef(env, interpObj);
}

/*
 *----------------------------------------------------------------------
 *
 * JavaSetupJava --
 *
 *	Set up the cache of class and method ids.
 *
 * Results:
 *	A standard Tcl result.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

int
JavaSetupJava(
    JNIEnv *env,		/* JNI pointer for current thread. */
    Tcl_Interp *interp)		/* Interpreter to use for reporting errors. */
{
    Tcl_Obj *resultPtr;
    jfieldID field;
    int i;

    if (initialized) {
	return TCL_OK;
    }
    
    memset(&java, 0, sizeof(java));

    /*
     * Load the classes needed by this module.
     */

    for (i = 0; classes[i].addr != NULL; i++) {
	jclass classid;
	classid = (*env)->FindClass(env, classes[i].name);
	if (classid == NULL) {
            Tcl_Obj *obj;
            jobject exception = (*env)->ExceptionOccurred(env);
            if (exception) {
                (*env)->ExceptionDescribe(env);
                obj = Tcl_GetObjResult(interp);
                (*env)->ExceptionClear(env);
                /* We can't call ToString() here, we might not have
                 * java.toString yet.
                 */
                (*env)->Throw(env, exception);
                (*env)->DeleteLocalRef(env, exception);
            }

	    if (interp) {
     		Tcl_AppendStringsToObj(Tcl_GetObjResult(interp),
			"could not find class ", classes[i].name, ".\n",
#ifdef __WIN32__
                        "Check that your path includes the directory where ",
                        "tclblend.dll resides.\n",
#else
                        "Check that your LD_LIBRARY_PATH environment ",
                        "variable includes ",
                        "the directory where libtclblend.so resides.\n",
#endif
                        "Try looking in the directories under the value of ",
                        "tcl_library,\ncurrently: ",
                        Tcl_GetVar(interp, "tcl_library",TCL_GLOBAL_ONLY),
                        "\n",NULL);
                appendClasspathMessage(interp);
	    }
	    goto error;
	} else {
	    *(classes[i].addr) = (jclass) (*env)->NewGlobalRef(env,
		    (jobject)classid);
	    (*env)->DeleteLocalRef(env, classid);
	}
    }

    /*
     * Load methods needed by this module.
     */

    for (i = 0; methods[i].addr != NULL; i++) {
	jmethodID id;
	if (methods[i].isStatic) {
	    id = (*env)->GetStaticMethodID(env, *(methods[i].class),
		    methods[i].name, methods[i].sig);
	} else {
	    id = (*env)->GetMethodID(env, *(methods[i].class),
		    methods[i].name, methods[i].sig);
	}
	if (id == NULL) {
	    if (interp) {
		resultPtr = Tcl_GetObjResult(interp);
		Tcl_AppendStringsToObj(resultPtr, "could not find method ",
			methods[i].name, " in ", NULL);
		ToString(env, resultPtr, *(methods[i].class));
	    }
	    goto error;
	}
	*(methods[i].addr) = id;
    }
    
    /*
     * Load fields needed by this module.
     */

    for (i = 0; fields[i].addr != NULL; i++) {
	field = (*env)->GetFieldID(env,
		*(fields[i].class), fields[i].name, fields[i].sig);
	if (field == NULL) {
	    if (interp) {
		resultPtr = Tcl_GetObjResult(interp);
		Tcl_AppendStringsToObj(resultPtr, "could not find field ",
			fields[i].name, " in ", NULL);
		ToString(env, resultPtr, *(fields[i].class));
	    }
	    goto error;
	}
	*(fields[i].addr) = field;
    }

    /*
     * Get the Void.TYPE value.
     */

    field = (*env)->GetStaticFieldID(env, java.Void, "TYPE",
	    "Ljava/lang/Class;");
    java.voidTYPE = (*env)->GetStaticObjectField(env, java.Void,
	    field);
        
    /*
     * Register the Java object types.
     */

    JavaObjInit();

    initialized = 1;
    return TCL_OK;

    error:
    (*env)->ExceptionClear(env);
    return TCL_ERROR;
}

/*
 *----------------------------------------------------------------------
 *
 * ToString --
 *
 *	Invoke the toString() method on an object
 *
 * Results:
 *	A standard Tcl result.
 *
 * Side effects:
 *	Places the string in the specified object.
 *
 *----------------------------------------------------------------------
 */

static int
ToString(
    JNIEnv *env,		/* Java environment pointer. */
    Tcl_Obj *objPtr,		/* Object that will hold the string. */
    jobject obj)		/* Object whose string should be retrieved. */
{
    jstring str;
    int length;
    char *buf;
    jobject exc;

    str = (*env)->CallObjectMethod(env, obj, java.toString);
    exc = (*env)->ExceptionOccurred(env);
    if (exc) {
	(*env)->ExceptionClear(env);
	(*env)->DeleteLocalRef(env, exc);
	return TCL_ERROR;
    }
    if (!str) {
	return TCL_ERROR;
    }

    buf = JavaGetString(env, str, &length);

    Tcl_AppendToObj(objPtr, buf, length);
    (*env)->DeleteLocalRef(env, str);
    ckfree(buf);
    return TCL_OK;
}

/*
 *----------------------------------------------------------------------
 *
 * JavaThrowTclException --
 *
 *	Generate a TclException with the given result code.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Throws a new exception in the Java VM.
 *
 *----------------------------------------------------------------------
 */

void
JavaThrowTclException(
    JNIEnv *env,		/* Java environment pointer. */
    Tcl_Interp *interp,		/* Interp to get result from, or NULL. */
    int result)			/* One of TCL_ERROR, etc. */
{
    jmethodID init= (*env)->GetMethodID(env, java.TclException, "<init>",
	    "(Ltcl/lang/Interp;Ljava/lang/String;I)V");
    jobject exc;
    jstring msg;

    if (!interp) {
	msg = NULL;
    } else {
	msg = (*env)->NewStringUTF(env,
		Tcl_GetStringFromObj(Tcl_GetObjResult(interp), NULL));
    }
    exc = (*env)->NewObject(env, java.TclException, init, NULL, msg,
	    result);
    (*env)->Throw(env, exc);
    if (msg) {
	(*env)->DeleteLocalRef(env, msg);
    }
    (*env)->DeleteLocalRef(env, exc);
}

/*
 *----------------------------------------------------------------------
 *
 * JavaGetString --
 *
 *	Convert a Java string into a Tcl string.
 *
 * Results:
 *	Returns a newly allocated C string and size of string.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

char *
JavaGetString(
    JNIEnv *env,		/* Java environment pointer. */
    jstring str,		/* String to convert. */
    int *lengthPtr)		/* Pointer to where length should be stored,
				 * or NULL. */
{
    const jchar *ustr;
    jsize length;
    char *buf;
    int i;

    ustr = (*env)->GetStringChars(env, str, NULL);
    length = (*env)->GetStringLength(env, str);

    /*
     * Copy the Unicode characters into an 8-bit character array
     * by stripping off the upper byte of the character.
     */

    buf = ckalloc(length+1);
    for (i = 0; i < length; i++) {
	buf[i] = ustr[i] & 0xff;
    }
    buf[length] = 0;

    (*env)->ReleaseStringChars(env, str, ustr);

    if (lengthPtr != NULL) {
	*lengthPtr = length;
    }
    return buf;
}

