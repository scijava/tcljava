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
 * RCS: @(#) $Id: javaCmd.c,v 1.9.2.8 2000/10/23 01:05:31 mdejong Exp $
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
#include <assert.h>

/*
 * The following pointer is used to keep track of the current Java
 * thread information.  It is set on each entry from Java and is restored
 * to its previous value before returning to Java.  This discipline will
 * handle nested calls between Tcl and Java.  The initial value will
 * be null if the Blend pacakge is initialized from Java, otherwise it
 * will contain the environment for the default thread.
 */
typedef struct ThreadSpecificData {
    /*
     * This flag indicates that thread local data has been initialized for this thread.
     */

    int initialized;

    /*
     * JNI pointer for the current thread, functions invoked throught the env are thread safe.
     */

    JNIEnv* currentEnv;

    /*
     * Cache for class, method, and filed info for this JNIEnv.
     */

    JavaInfo jcache;

} ThreadSpecificData;

static Tcl_ThreadDataKey dataKey;

/* Define this here so that we do not need to include tclInt.h */
#define TCL_TSD_INIT(keyPtr)	(ThreadSpecificData *)Tcl_GetThreadData((keyPtr), sizeof(ThreadSpecificData))


/*
 * The following variable contains the pointer to the current Java VM,
 * if it was created or attached to by Tcl. We only support a single
 * JVM, but this VM can be accessed from multiple Tcl threads. We
 * depend on JNI to access this global in a thread safe way.
 */

static JavaVM *javaVM = NULL;

/*
 * Declarations of functions used only in this file.
 */

static int		ToString(JNIEnv *env, Tcl_Obj *objPtr, jobject obj);
static JNIEnv *		JavaInitEnv(JNIEnv *env, Tcl_Interp *interp);
static int		AddToClassCache(JNIEnv *env, Tcl_Interp *interp, jclass *addr, char *name);
static int		AddToMethodCache(JNIEnv *env, Tcl_Interp *interp, jmethodID *addr,
                            char *name, jclass *class, char *sig, int isStatic);
static int		AddToFieldCache(JNIEnv *env, Tcl_Interp *interp, jfieldID *addr,
                            char *name, jclass *class, char *sig);
static void		DestroyJVM(ClientData clientData);
static void		DetachTclThread(ClientData clientData);
static void		FreeJavaCache(ClientData clientData);

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
    int result;
    jlong lvalue;
    jobject interpObj, local;
    JNIEnv *env;
    JavaInfo* jcache;

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: Tclblend_Init\n");
    fprintf(stderr, "TCLBLEND_DEBUG: CLASSPATH is \"%s\"\n", getenv("CLASSPATH"));
#endif /* TCLBLEND_DEBUG */

    /*
     * Init the JVM, the JNIEnv pointer, and any global data. Pass a
     * NULL JNIEnv pointer to indicate Tcl Blend is being loaded from Tcl.
     */

    if (JavaSetupJava(NULL, interp) != TCL_OK) {
        return TCL_ERROR;
    }

    /*
     * Allocate a new Interp instance to wrap this interpreter.
     */

    env = JavaGetEnv();
    jcache = JavaGetCache();

    *(Tcl_Interp**)&lvalue = interp;
    local = (*env)->NewObject(env, jcache->Interp,
	    jcache->interpC, lvalue);
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

    result = JavaInitBlend(env, interp, interpObj);


#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: Tclblend_Init finished\n");
    fprintf(stderr, "TCLBLEND_DEBUG: JavaInitBlend() returned ");
    if (result == TCL_ERROR) {
      fprintf(stderr, "TCL_ERROR");
    } else if (result == TCL_OK) {
      fprintf(stderr, "TCL_OK");
    } else {
      fprintf(stderr, "%d", result);
    }
    fprintf(stderr, "\n");

#endif /* TCLBLEND_DEBUG */

    return result;
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


#ifdef TCLBLEND_KAFFE
    JavaVMInitArgs vm_args;
#else
    JDK1_1InitArgs vm_args;
#endif

    memset(&vm_args, 0, sizeof(vm_args));
    vm_args.version = 0x00010001;
    JNI_GetDefaultJavaVMInitArgs(&vm_args);
       Tcl_AppendResult(interp,
        "Currently, the CLASSPATH environment variable ",
        "is set to:\n",
        getenv("CLASSPATH"), 
        "\nThe JVM currently is using the following classpath:\n",
        vm_args.classpath, NULL);
#endif
}

/*
 *----------------------------------------------------------------------
 *
 * JavaGetEnv --
 *
 *	Retrieve the JNI environment for the current thread. This method
 *	is concurrent safe.
 *
 * Results:
 *	Returns the JNIEnv pointer for the current thread.
 *	This method  must be called after JavaInitEnv has been called.
 *
 * Side effects:
 *
 *----------------------------------------------------------------------
 */

TCLBLEND_EXTERN JNIEnv *
JavaGetEnv()
{
    ThreadSpecificData *tsdPtr = TCL_TSD_INIT(&dataKey);

    assert(tsdPtr->initialized);

    return tsdPtr->currentEnv;
}

/*
 *----------------------------------------------------------------------
 *
 * JavaGetCache --
 *
 *	Retrieve the JNI class, method, and field cache for the
 *	current thread.
 *
 * Results:
 *	Returns the JavaInfo pointer for the current thread.
 *	This method  must be called after JavaSetupJava has been called.
 *
 * Side effects:
 *
 *----------------------------------------------------------------------
 */

TCLBLEND_EXTERN JavaInfo*
JavaGetCache()
{
    ThreadSpecificData *tsdPtr = TCL_TSD_INIT(&dataKey);

    assert(tsdPtr->initialized);

    return &(tsdPtr->jcache);
}

/*
 *----------------------------------------------------------------------
 *
 * JavaInitEnv --
 *
 *	Init the JNIEnv for this thread.
 *
 * Results:
 *	Returns the JNIEnv pointer for the current thread.  Returns
 *	NULL on error with a message left in the interpreter result.
 *
 * Side effects:
 *	If Tcl Blend is loaded into Tcl and this is the first thread
 *	to load Tcl Blend, a new JVM will be created. If another
 *	Tcl thread loads Tcl Blend, that thread will be attached to
 *	the existing JVM.
 *----------------------------------------------------------------------
 */

static
JNIEnv*
JavaInitEnv(
    JNIEnv *env,        /* JNIEnv pointer, NULL if loaded from Tcl Blend */
    Tcl_Interp *interp	/* Interp for error reporting. */
)
{
    jsize nVMs;
    char *path, *newPath;
    int oldSize, size, maxOptions = 2;
#ifdef JDK1_2
    JavaVMOption *options;
    JavaVMInitArgs vm_args;
#elif defined TCLBLEND_KAFFE /* FIXME: Can we pass options to Kaffe ?? */
    JavaVMInitArgs vm_args;
#else
    JDK1_1InitArgs vm_args;
#endif /* JDK1_2 */

    ThreadSpecificData *tsdPtr = TCL_TSD_INIT(&dataKey);

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: JavaInitEnv for %s JVM\n",
#ifdef JDK1_2
        "JDK1_2"
#elif defined TCLBLEND_KAFFE
        "Kaffe"
#else
        "JDK1_1"
#endif
);
#endif /* TCLBLEND_DEBUG */

    /*
     * If we were called with a non-NULL JNIEnv argument, it means
     * Tcl Blend was loaded from Java. In this case, the JNIEnv is
     * already attached to the JVM because it was created in Java.
     * Since we do not need to create a JVM and we do not need to
     * attach the current thread, we just set currentEnv and return.
     */

    if (env) {

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: setting currentEnv from Java\n");
#endif /* TCLBLEND_DEBUG */

        return (tsdPtr->currentEnv = env);
    }

    /*
     * From this point on, deal with the case where Tcl Blend is loaded from Tcl.
     * Check to see if the current process already has a Java VM.  If so, attach
     * the current thread to it, otherwise create a new JVM (automatic thread attach).
     */

    if (JNI_GetCreatedJavaVMs(&javaVM, 1, &nVMs) < 0) {

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: JNI_GetCreatedJavaVMs failed\n");
#endif /* TCLBLEND_DEBUG */

	Tcl_AppendResult(interp, "JNI_GetCreatedJavaVMs failed", NULL);
	goto error;
    }

    if (nVMs == 0) {

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: No JVM, creating one\n");
#endif /* TCLBLEND_DEBUG */

        memset(&vm_args, 0, sizeof(vm_args));
#ifdef JDK1_2
        options = (JavaVMOption *) ckalloc(sizeof(JavaVMOption) * maxOptions);
        vm_args.version = 0x00010002;
        vm_args.options = options;
        vm_args.ignoreUnrecognized= 1;
        vm_args.nOptions = 0;
#else
        vm_args.version = 0x00010001;
        JNI_GetDefaultJavaVMInitArgs(&vm_args); /* FIXME: For 1.1 only ?? */
#endif /* JDK1_2 */

#ifdef TCLBLEND_INCREASE_STACK_SIZE
        vm_args.nativeStackSize = vm_args.nativeStackSize *4;
        vm_args.javaStackSize = vm_args.javaStackSize *4;
        vm_args.minHeapSize = vm_args.minHeapSize *4;
        vm_args.maxHeapSize = vm_args.maxHeapSize *4;
#ifdef TCLBLEND_DEBUG
        fprintf(stderr,"TCLBLEND_DEBUG: vm_args: "
                "nativeStackSize = %d  javaStackSize = %d\n"
                "minHeapSize = %d      maxHeapSize = %d\n",
                vm_args.nativeStackSize, vm_args.javaStackSize,
                vm_args.minHeapSize, vm_args.maxHeapSize);
#endif /* TCLBLEND_DEBUG */
#endif /* TCLBLEND_INCREASE_STACK_SIZE */


	/*
	 * Add the classpath as a prefix to the default classpath.
	 * Under JDK 1.2, we can just pass a -D option. Under JDK
	 * 1.1, we need to append to the vm_args.classpath.
	 */

	path = getenv("CLASSPATH");

#ifdef JDK1_2
# define JAVA_CLASS_PATH_ARG "-Djava.class.path="
	if (path) {
	    size = strlen(path) + strlen(JAVA_CLASS_PATH_ARG);
            options[0].optionString = ckalloc(size+2);
            vm_args.nOptions++;
	    strcpy(options[0].optionString, JAVA_CLASS_PATH_ARG);
	    strcat(options[0].optionString, path);
	    options[0].extraInfo = (void *)NULL;
	}
#else
	if (path && vm_args.classpath) {
	    oldSize = strlen(path);
	    size = oldSize + strlen(vm_args.classpath);
	    newPath = ckalloc(size+2);
	    strcpy(newPath, path);
# ifdef __WIN32__
	    newPath[oldSize] = ';';
# else
	    newPath[oldSize] = ':';
# endif /*  __WIN32__ */
	    strcpy(newPath+oldSize+1, vm_args.classpath);
	    vm_args.classpath = newPath;
	} else if (path) {
	    vm_args.classpath = path;
	}
#endif /* JDK1_2 */

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: CLASSPATH is \"%s\"\n",
#ifdef JDK1_2
        options[0].optionString);
#else
        vm_args.classpath);
#endif /* JDK1_2 */
#endif /* TCLBLEND_DEBUG */


#ifdef JDK1_2
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
                goto error;
            }
	    options[vm_args.nOptions].extraInfo = (void *)NULL;
            vm_args.nOptions++;

#ifdef TCLBLEND_DEBUG
            {
            int i;
            fprintf(stderr, "TCLBLEND_DEBUG: tclblend_init set\n"
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
#endif /* TCLBLEND_DEBUG */
           
        }
#endif /* JDK1_2 */

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: JNI_CreateJavaVM\n");
#endif /* TCLBLEND_DEBUG */

	if (JNI_CreateJavaVM(&javaVM,
#ifdef JDK1_2
            (void **)
#endif /* JDK1_2 */
            &tsdPtr->currentEnv, &vm_args) < 0) {

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: JNI_CreateJavaVM failed\n");
#endif /* TCLBLEND_DEBUG */

	    Tcl_AppendResult(interp, "JNI_CreateJavaVM failed",
                                     "Perhaps your CLASSPATH includes a "
                                     "classes.zip file for a version other"
                                     "than the one Tcl Blend was compiled with?\n", 
                                     NULL);
            appendClasspathMessage(interp);
            goto error;
	}

        /* Create a thread exit handler that will destroy the JVM */

	Tcl_CreateThreadExitHandler(DestroyJVM, NULL);

    } else {

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: JVM in process, attaching\n");
#endif /* TCLBLEND_DEBUG */

	if ((*javaVM)->AttachCurrentThread(javaVM,
#ifdef JDK1_2
            (void **)

#endif /* JDK1_2 */
            &tsdPtr->currentEnv, NULL) != 0) {

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: attach failed\n");
#endif /* TCLBLEND_DEBUG */

	    Tcl_AppendResult(interp, "AttachCurrentThread failed", NULL);
	    goto error;
	}
	
	/* Create a thread exit handler to detach this Tcl thread from the JVM */
	
	Tcl_CreateThreadExitHandler(DetachTclThread, NULL);
    }

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: JavaInitEnv returning successfully\n");
#endif /* TCLBLEND_DEBUG */

    return tsdPtr->currentEnv;

    error:

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: JavaInitEnv returning NULL\n");
#endif /* TCLBLEND_DEBUG */

    return NULL;
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
    Tcl_Obj *obj;
    jobject blend, exception;
    int result;
    JavaInfo* jcache = JavaGetCache();

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: called JavaInitBlend\n");
#endif /* TCLBLEND_DEBUG */
    
    /*
     * Associate the interpreter data with the interp object.
     */
	
    Tcl_SetAssocData(interp, "java", JavaInterpDeleted,
	    (ClientData)interpObj);

    /*
     * Initialize the BlendExtension.
     */

    blend = (*env)->NewObject(env, jcache->BlendExtension, jcache->blendC);
    (*env)->CallVoidMethod(env, blend, jcache->init, interpObj);
    if (exception = (*env)->ExceptionOccurred(env)) {
      (*env)->ExceptionDescribe(env);
      (*env)->ExceptionClear(env);
      obj = Tcl_GetObjResult(interp);
      ToString(env, obj, exception);

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: Exception in init() method\n");
#endif /* TCLBLEND_DEBUG */

	result = TCL_ERROR;
    } else {
	result = TCL_OK;
    }
    (*env)->DeleteLocalRef(env, blend);


#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: JavaInitBlend returning\n");
#endif /* TCLBLEND_DEBUG */

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
    JNIEnv *env = JavaGetEnv();
    JavaInfo* jcache = JavaGetCache();

    /*
     * Set the Interp.interpPtr field to 0 so any further attempts to use
     * this interpreter from Java will fail and so Interp.dispose() won't
     * try to delete the interpreter again.  
     */

    (*env)->SetLongField(env, interpObj, jcache->interpPtr, 0);

    /*
     * Call Interp.dispose() to release any state kept in Java.
     */

    (*env)->CallVoidMethod(env, interpObj, jcache->dispose);
    (*env)->DeleteGlobalRef(env, interpObj);

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: called JavaInterpDeleted\n");
#endif /* TCLBLEND_DEBUG */
}

/*
 *----------------------------------------------------------------------
 *
 * DestroyJVM --
 *
 *	This method will be called when the "main" Tcl thread (the
 *	one that first loaded the JVM) is getting finalized.
 *	Note that this method would never get called in the case
 *	where Tcl Blend is loaded into an existing JVM.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */
static void
DestroyJVM(ClientData clientData)
{
    ThreadSpecificData *tsdPtr = TCL_TSD_INIT(&dataKey);

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: called DestroyJVM\n");
#endif /* TCLBLEND_DEBUG */

    /*
     * The DestroyJavaVM() JNI method has never worked
     * properly, and in some cases it can crash.
     * For this reason, we do not actually call it here.
     * One of the enhancements made to JNI in 1.2 was to
     * support detaching of the "main" thread, so use it.
     */

#ifdef JDK1_2
    if ((*javaVM)->DetachCurrentThread(javaVM) != 0) {
#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: error calling jvm->DetachCurrentThread()\n");
#endif /* TCLBLEND_DEBUG */
    }
#else
    /*
     * This method always returns an error. It does nothing
     * and can generate an illegal instruction if called
     * from a second Tcl thread (not the "main" thread in 1.1)
     */

    /* (*javaVM)->DestroyJavaVM(javaVM); */
#endif

    /*
     * One should not call JavaGetEnv() or JavaGetCache()
     * after this method has finished. Setting initialized
     * to zero will raise an assert on such a call.
     */

    tsdPtr->initialized = 0;
}

/*
 *----------------------------------------------------------------------
 *
 * DetachTclThread --
 *
 *	This method will be called when a Tcl thread that was attached
 *	to a JVM is getting finalized. Note that this method is not
 *	called for a thread that originated in the JVM, because the
 *	JVM would handle attaching and detaching of such a thread.
 *	Also note that this method is not called for the "main"
 *	thread, meaning the Tcl thread that first loaded the JVM.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */
static void
DetachTclThread(ClientData clientData)
{
    ThreadSpecificData *tsdPtr = TCL_TSD_INIT(&dataKey);

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: called DetachTclThread\n");
#endif /* TCLBLEND_DEBUG */

    if ((*javaVM)->DetachCurrentThread(javaVM) != 0) {
#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: error calling jvm->DetachCurrentThread()\n");
#endif /* TCLBLEND_DEBUG */
    }

    /*
     * After detaching this Tcl thread from the JVM, one can not
     * call JavaGetEnv() or JavaGetCache(). Setting initialized
     * to zero will raise an assert on such a call.
     */

    tsdPtr->initialized = 0;
}

/*
 *----------------------------------------------------------------------
 *
 * FreeJavaCache --
 *
 *	This method will be called when a Tcl or Java thread is finished.
 *	It needs to remove any global cache references so that the
 *	classes and methods can be cleaned up by the JVM.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */
static void
FreeJavaCache(ClientData clientData)
{
    JNIEnv* env = JavaGetEnv();
    JavaInfo* jcache = JavaGetCache();

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: called FreeJavaCache\n");
#endif /* TCLBLEND_DEBUG */

    /* We need to delete any global refs to Java classes */
    
    (*env)->DeleteGlobalRef(env, jcache->Object);
    (*env)->DeleteGlobalRef(env, jcache->Interp);
    (*env)->DeleteGlobalRef(env, jcache->Command);
    (*env)->DeleteGlobalRef(env, jcache->TclObject);
    (*env)->DeleteGlobalRef(env, jcache->TclException);
    (*env)->DeleteGlobalRef(env, jcache->CommandWithDispose);
    (*env)->DeleteGlobalRef(env, jcache->CObject);
    (*env)->DeleteGlobalRef(env, jcache->Extension);
    (*env)->DeleteGlobalRef(env, jcache->VarTrace);
    (*env)->DeleteGlobalRef(env, jcache->Void);
    (*env)->DeleteGlobalRef(env, jcache->BlendExtension);
    (*env)->DeleteGlobalRef(env, jcache->Notifier);
    (*env)->DeleteGlobalRef(env, jcache->IdleHandler);
    (*env)->DeleteGlobalRef(env, jcache->TimerHandler);

     /* FIXME : we dont add or release a global ref for jcache->Void
        or jcache->VoidTYPE class, should we ? */
}

/*
 *----------------------------------------------------------------------
 *
 * JavaSetupJava --
 *
 *	This is the entry point for a Tcl interpreter created from Java.
 *	This method will save the JVM JNIEnv pointer by calling JavaInitEnv
 *	if this was the first time JavaSetupJava was called for the current
 *	thread. It will also set up the cache of class and method ids if this
 *	was the first time JavaSetupJava was called in this process.
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
    Tcl_Interp *interp)		/* Interpreter to use for reporting errors. Can be NULL */
{
    jfieldID field;
    JavaInfo* jcache;
    ThreadSpecificData *tsdPtr = TCL_TSD_INIT(&dataKey);

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: called JavaSetupJava\n");
#endif /* TCLBLEND_DEBUG */

    /*
     * Check to see if the thread local data has already been
     * initialized for this thread. Do nothing if it has been.
     */

    if (tsdPtr->initialized) {

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: thread specific data has already been initialized\n");
#endif /* TCLBLEND_DEBUG */

        goto ok;
    }

    /*
     * If Tcl Blend is getting loaded from Tcl, then the env argument
     * would be passed as NULL.
     */

    if ((env = JavaInitEnv(env, interp)) == NULL) {
	goto error;
    }

#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: initializing jcache\n");
#endif /* TCLBLEND_DEBUG */

    jcache = &(tsdPtr->jcache);
    memset(jcache, 0, sizeof(JavaInfo));

    /*
     * Load the classes needed by this module.
     */

    if (AddToClassCache(env, interp, &jcache->Object, "java/lang/Object") ||
        AddToClassCache(env, interp, &jcache->Interp, "tcl/lang/Interp") ||
        AddToClassCache(env, interp, &jcache->Command, "tcl/lang/Command") ||
        AddToClassCache(env, interp, &jcache->TclObject, "tcl/lang/TclObject") ||
        AddToClassCache(env, interp, &jcache->TclException, "tcl/lang/TclException") ||
        AddToClassCache(env, interp, &jcache->CommandWithDispose, "tcl/lang/CommandWithDispose") ||
        AddToClassCache(env, interp, &jcache->CObject, "tcl/lang/CObject") ||
        AddToClassCache(env, interp, &jcache->Extension, "tcl/lang/Extension") ||
        AddToClassCache(env, interp, &jcache->VarTrace, "tcl/lang/VarTrace") ||
        AddToClassCache(env, interp, &jcache->Void, "java/lang/Void") ||
        AddToClassCache(env, interp, &jcache->BlendExtension, "tcl/lang/BlendExtension") ||
        AddToClassCache(env, interp, &jcache->Notifier, "tcl/lang/Notifier") ||
        AddToClassCache(env, interp, &jcache->IdleHandler, "tcl/lang/IdleHandler") ||
        AddToClassCache(env, interp, &jcache->TimerHandler, "tcl/lang/TimerHandler")) {
        goto error;
    }

    /*
     * Get the Void.TYPE class.
     */

    field = (*env)->GetStaticFieldID(env, jcache->Void, "TYPE", "Ljava/lang/Class;");
    jcache->voidTYPE = (*env)->GetStaticObjectField(env, jcache->Void, field);

    /*
     * Create a thread exit handler that will clean up the cache.
     */

    Tcl_CreateThreadExitHandler(FreeJavaCache, NULL);

    /*
     * Load methods needed by this module.
     */

    if (AddToMethodCache(env, interp, &jcache->toString, "toString",
                                      &jcache->Object, "()Ljava/lang/String;", 0) ||
	AddToMethodCache(env, interp, &jcache->callCommand, "callCommand",
                                      &jcache->Interp, "(Ltcl/lang/Command;[Ltcl/lang/TclObject;)I", 0) ||
	AddToMethodCache(env, interp, &jcache->dispose, "dispose",
                                      &jcache->Interp, "()V", 0) ||
	AddToMethodCache(env, interp, &jcache->interpC, "<init>",
                                      &jcache->Interp, "(J)V", 0) ||
	AddToMethodCache(env, interp, &jcache->tclexceptionC, "<init>",
                                      &jcache->TclException, "(Ltcl/lang/Interp;Ljava/lang/String;I)V", 0) ||
	AddToMethodCache(env, interp, &jcache->cmdProc, "cmdProc",
                                      &jcache->Command, "(Ltcl/lang/Interp;[Ltcl/lang/TclObject;)V", 0) ||
	AddToMethodCache(env, interp, &jcache->disposeCmd, "disposeCmd",
                                      &jcache->CommandWithDispose, "()V", 0) ||
	AddToMethodCache(env, interp, &jcache->newCObjectInstance, "newInstance",
                                      &jcache->CObject, "(J)Ltcl/lang/TclObject;", 1) ||
	AddToMethodCache(env, interp, &jcache->preserve, "preserve",
                                      &jcache->TclObject, "()V", 0) ||
	AddToMethodCache(env, interp, &jcache->release, "release",
                                      &jcache->TclObject, "()V", 0) ||
	AddToMethodCache(env, interp, &jcache->getInternalRep, "getInternalRep",
                                      &jcache->TclObject, "()Ltcl/lang/InternalRep;", 0) ||
	AddToMethodCache(env, interp, &jcache->init, "init",
                                      &jcache->Extension, "(Ltcl/lang/Interp;)V", 0) ||
	AddToMethodCache(env, interp, &jcache->blendC, "<init>",
                                      &jcache->BlendExtension, "()V", 0) ||
	AddToMethodCache(env, interp, &jcache->traceProc, "traceProc",
                                      &jcache->VarTrace,
				      "(Ltcl/lang/Interp;Ljava/lang/String;Ljava/lang/String;I)V", 0) ||
	AddToMethodCache(env, interp, &jcache->serviceEvent, "serviceEvent",
                                      &jcache->Notifier, "(I)I", 0) ||
	AddToMethodCache(env, interp, &jcache->hasEvents, "hasEvents",
                                      &jcache->Notifier, "()Z", 0) ||
	AddToMethodCache(env, interp, &jcache->invokeIdle, "invoke",
                                      &jcache->IdleHandler, "()V", 0) ||
	AddToMethodCache(env, interp, &jcache->invokeTimer, "invoke",
                                      &jcache->TimerHandler, "()V", 0)) {
        goto error;
    }
    
    /*
     * Load fields needed by this module.
     */

    if (AddToFieldCache(env, interp, &jcache->interpPtr, "interpPtr", &jcache->Interp, "J") ||
        AddToFieldCache(env, interp, &jcache->objPtr, "objPtr", &jcache->CObject, "J")) {
	goto error;
    }

    /*
     * Register the Java object types.
     */

    JavaObjInit();

    tsdPtr->initialized = 1;

    ok:
#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: JavaSetupJava returning successfully\n");
#endif /* TCLBLEND_DEBUG */

    return TCL_OK;

    error:
#ifdef TCLBLEND_DEBUG
    fprintf(stderr, "TCLBLEND_DEBUG: JavaSetupJava returning TCL_ERROR\n");
#endif /* TCLBLEND_DEBUG */

    if (env) {
        (*env)->ExceptionClear(env);
    }
    return TCL_ERROR;
}

/*
 *----------------------------------------------------------------------
 *
 * AddToClassCache --
 *
 *	This method will add a jclass token to
 *      the thread local cache. This method must only
 *      be called from inside JavaSetupJava.
 *
 * Results:
 *	Zero if the class was added, Non-zero on error.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static
int
AddToClassCache(
    JNIEnv *env,		/* JNI pointer for current thread. */
    Tcl_Interp *interp,		/* Interpreter to use for reporting errors. Can be NULL */
    jclass *addr,		/* Where to store jclass. */
    char *name)			/* Name of class to load. */
{
    jclass classid;

    classid = (*env)->FindClass(env, name);
    if (classid == NULL) {
        Tcl_Obj *obj;
        jobject exception = (*env)->ExceptionOccurred(env);
        if (exception) {
            (*env)->ExceptionDescribe(env);
            obj = Tcl_GetObjResult(interp);
            (*env)->ExceptionClear(env);
            /*
             * We can't call ToString() here, we might not have access
             * to the java.lang.String.toString() method yet.
             */
            (*env)->Throw(env, exception);
            (*env)->DeleteLocalRef(env, exception);
        }

        if (interp) {
            Tcl_AppendStringsToObj(Tcl_GetObjResult(interp),
                "could not find class \"", name, "\".\n",
                "Check your CLASSPATH settings.\n", NULL);
                appendClasspathMessage(interp);
	}
        return 1;
    } else {
        *(addr) = (jclass) (*env)->NewGlobalRef(env, (jobject)classid);

        (*env)->DeleteLocalRef(env, classid);
        return 0;
    }
}

/*
 *----------------------------------------------------------------------
 *
 * AddToMethodCache --
 *
 *	This method will add a jmethodID token to
 *      the thread local cache. This method must only
 *      be called from inside JavaSetupJava.
 *
 * Results:
 *	Zero if the method was added, Non-zero on error.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static
int
AddToMethodCache(
    JNIEnv *env,		/* JNI pointer for current thread. */
    Tcl_Interp *interp,		/* Interpreter to use for reporting errors. Can be NULL */
    jmethodID *addr,		/* Where to store jmethodID. */
    char *name,			/* Name of method to load. */
    jclass *class,		/* Where to find class id. */
    char *sig,			/* Signature of method. */
    int isStatic)		/* 1 if method is static. */
{
    Tcl_Obj *resultPtr;
    jmethodID id;

    if (isStatic) {
        id = (*env)->GetStaticMethodID(env, *(class), name, sig);
    } else {
        id = (*env)->GetMethodID(env, *(class), name, sig);
    }
    if (id == NULL) {
        if (interp) {
            resultPtr = Tcl_GetObjResult(interp);
            Tcl_AppendStringsToObj(resultPtr, "could not find method ",
                name, " in ", NULL);
            ToString(env, resultPtr, *(class));
        }
        return 1;
    }
    *(addr) = id;
    return 0;
}

/*
 *----------------------------------------------------------------------
 *
 * AddToFieldCache --
 *
 *	This method will add a jfieldID token to
 *      the thread local cache. This method must only
 *      be called from inside JavaSetupJava.
 *
 * Results:
 *	Zero if the method was added, Non-zero on error.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static
int
AddToFieldCache(
    JNIEnv *env,		/* JNI pointer for current thread. */
    Tcl_Interp *interp,		/* Interpreter to use for reporting errors. Can be NULL */
    jfieldID *addr,		/* Where to store jfieldID. */
    char *name,			/* Name of method to load. */
    jclass *class,		/* Where to find class id. */
    char *sig)			/* Signature of method. */
{
    Tcl_Obj *resultPtr;
    jfieldID field;

    field = (*env)->GetFieldID(env, *(class), name, sig);

    if (field == NULL) {
        if (interp) {
            resultPtr = Tcl_GetObjResult(interp);
            Tcl_AppendStringsToObj(resultPtr, "could not find field ",
                name, " in ", NULL);
            ToString(env, resultPtr, *(class));
        }
	return 1;
    }
    *(addr) = field;
    return 0;
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
    JavaInfo* jcache = JavaGetCache();

    str = (*env)->CallObjectMethod(env, obj, jcache->toString);
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
    jobject exc;
    jstring msg;
    JavaInfo* jcache = JavaGetCache();

    if (!interp) {
	msg = NULL;
    } else {
	msg = (*env)->NewStringUTF(env,
		Tcl_GetStringFromObj(Tcl_GetObjResult(interp), NULL));
    }
    exc = (*env)->NewObject(env, jcache->TclException, jcache->tclexceptionC, NULL, msg,
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
    char *p;
    Tcl_DString ds;

    ustr = (*env)->GetStringChars(env, str, NULL);
    length = (*env)->GetStringLength(env, str);

    /*
     * Convert the Unicode string into a UTF-8 encoded string. This
     * could require a buffer larger than the number of unicode chars.
     */

    Tcl_DStringInit(&ds);
    Tcl_UniCharToUtfDString(ustr, length, &ds);

    /*
     * Now get the UTF-8 encoded string from the DString
     * along with the number of encoded bytes (the length).
     */

    p = Tcl_DStringValue(&ds);
    length = Tcl_DStringLength(&ds);

    buf = ckalloc(length+1);
    
    /*
     * Copy the UTF-8 chars from the DString into the newely
     * allocated buffer and make sure it is null terminated.
     */

    memcpy((VOID *) buf, (VOID *) p, (size_t) (length * sizeof(char)));
    buf[length] = 0;

    Tcl_DStringFree(&ds);

    (*env)->ReleaseStringChars(env, str, ustr);

    if (lengthPtr != NULL) {
	*lengthPtr = length;
    }
    return buf;
}

