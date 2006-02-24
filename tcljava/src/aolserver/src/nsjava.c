/*
 * Copyright (c) 2000 Daniel Wickstrom
 *
 * See also http://www.aolserver.com for details on the AOLserver
 * web server software
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 675 Mass Ave, Cambridge,
 * MA 02139, USA.
 * 
 */

/* 
 * Many ideas were borrowed from Brent Fulgham's excellent PyWX module.
 */

/*
 *  nsjava.c
 *
 *  Provide a java module for use in Aolserver.  The need for this arose from 
 *  porting the ACS web toolkit. See http://openacs.org/.  The main idea is 
 *  to provide the same functionality for Aolserver/postgres/openacs that is 
 *  obtained by using Aolserver/oracle/sqlj/acs.  Though there is nothing 
 *  that should prevent this from becoming a general purpose web development
 *  tool.  Right now this module only supports the calling of java static 
 *  methods from tcl via the jni reflection interface, and the wrapping of 
 *  the aolserver db api functions inside of java via the jni interface. 
 *  A class is also provided for the evaling of tcl from within java.  
 *  Future versions will probably support read/writes to the http connection 
 *  as well, and most of the useful aolserver api functions will probably 
 *  end up being wrapped for use in java via jni interface.  Integrating 
 *  tclblend would be really nice, but it doesn't look to be too easy.
 *
 *  It wasen't easy, but we now have support for tclblend. Dec 2000.
 *
 * -DanW
 */

#include <nsjava.h>

/* 
 * functions loaded from libjava.so at startup
 */

jint (JNICALL *GetDefaultJavaVMInitArgs)(void *) = NULL;
jint (JNICALL *CreateJavaVM)(JavaVM **, JNIEnv **, void *) = NULL;

/*
 * cache for nsjava global variables.
 */

static NsJavaEnvCache    nsjavaEnv;

/*
 * Pointer to java virtual machine.
 */

static JavaVM           *javaVM = NULL;

/*
 * jvm init arguements
 */

static NsJavaVMInitArgs  vm_args;

/*
 * Return code from jvm thread 
 */

static int               invoke_rc;

/*
 * jvm startup thread started from aolserver main thread.
 */

Ns_Thread                jvm_thread;

/* 
 * Global module log handle for outputting module specific debugging 
 * information to the log file.
*/

Ns_ModLogHandle          javaModLogHandle;

/*
 * Module version required by aolserver.
 */

NS_EXPORT int            Ns_ModuleVersion = 1;

/*
 * Routines that are only called from procedures in this file.
 */

static void NsJavaInitializeNsJavaEnv(NsJavaEnvCache *envCache);
static JavaInfo* NsJava_JavaGetCache(void);
static void NsJavaThreadCleanup(ClientData clientData);
static void NsJavaCacheCleanup(JNIEnv *, JavaInfo *);
static void NsJavaStartJvm(void *);
static NsJavaEnvCache *NsJavaGetThreadEnvCache(void);

/*
 *----------------------------------------------------------------------
 *
 * Ns_ModuleInit --
 *
 *	Java Module init routine.
 *
 * Results:
 *	NS_OK if initialized ok, NS_ERROR otherwise.
 *
 * Side effects:
 *	A separate thread is spawned to actually start the jvm.
 *
 *----------------------------------------------------------------------
 */

NS_EXPORT int 
Ns_ModuleInit(char *hServer, char *hModule)
{
  char   *func = "ModuleInit";
  char   *serverConfigPath; 
  char   *moduleConfigPath; 
  char   *logLevel;
  int     enableJvm;
  int     rc;

  Ns_ModLogRegister(hModule, &javaModLogHandle);
  moduleConfigPath = Ns_ConfigGetPath(hServer, hModule, NULL);
  logLevel = Ns_ConfigGetValue(moduleConfigPath, "LogLevel");

  if (logLevel != NULL) {
    Ns_ModLog(Debug, javaModLogHandle, "%s: log level = %s", func, logLevel);
    Ns_ModLogSetThreshold(javaModLogHandle, NsJava_ConvertSeverity(logLevel));
  }
  else {
    Ns_ModLog(Debug, javaModLogHandle, "%s: log level = Notice", func);
    Ns_ModLogSetThreshold(javaModLogHandle, Notice);
  }

  rc = Ns_ConfigGetBool(moduleConfigPath, "EnableJava", &enableJvm);

  if (rc == NS_FALSE || enableJvm == NS_FALSE) {
    Ns_ModLog(Notice, javaModLogHandle, "%s: Java Module NOT enabled", func);
    return NS_OK;
  }

  Ns_ModLog(Notice, javaModLogHandle, "%s: Java Module ENABLED", func);
  Ns_RegisterShutdown(NsJava_ShutdownJava, NULL);

  /* 
   * Start a separate thread to create the jvm.  There is a signal handling
   * conflict when the jvm is started within aolserver's main thread.
   */

  invoke_rc = NS_ERROR;
  NsJavaInitializeNsJavaEnv(&nsjavaEnv);
  Ns_ModLog(Notice, javaModLogHandle, "%s: starting -jvm- thread", func);

  Ns_MutexLock(&nsjavaEnv.init_lock);
  nsjavaEnv.initCompleteP = NS_FALSE;
  Ns_ThreadCreate(NsJavaStartJvm, NULL, 0, &jvm_thread);

  while(nsjavaEnv.initCompleteP == NS_FALSE) {
    Ns_MutexUnlock(&nsjavaEnv.init_lock);
    sleep(1);
    Ns_MutexLock(&nsjavaEnv.init_lock);
  }

  Ns_MutexUnlock(&nsjavaEnv.init_lock);

  if(invoke_rc == NS_ERROR) {
    Ns_ModLog(Error,javaModLogHandle,"%s: could not initialize JVM.",func);
    return NS_ERROR;
  }

  Ns_ModLog(Notice, javaModLogHandle, "%s: JVM init succeeded!", func);

  Ns_TclRegisterAtCreate(NsJava_RegisterTclJavaFunctions, NULL);

  return NS_OK;
}


/*
 *----------------------------------------------------------------------
 *
 * NsJavaGetData --
 *
 *	Return the per-thread NsJava context structure.
 *
 * Results:
 *	Pointer to ThreadSpecificData.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

void *
NsJavaGetThreadData(void *keyPtr, int size)
{
  char *func = "NsJavaGetData";
  void *tsdPtr;
  Ns_Tls *kp = keyPtr;

  if (*kp == NULL) {
    Ns_MasterLock();
    if (*kp == NULL) {
      Ns_TlsAlloc(kp, NsJavaThreadCleanup);
    }
    Ns_MasterUnlock();
  }
  tsdPtr = Ns_TlsGet(kp);
  if (tsdPtr == NULL) {
    tsdPtr = ns_calloc(1, (size_t)size);
    Ns_TlsSet(kp, tsdPtr);
    Ns_Log(Debug,"%s: keyPtr = %u", func, *kp);
    Ns_Log(Debug, "%s: tsdPtr = %u", func, tsdPtr);
  }

  return tsdPtr;
}


/*
 *----------------------------------------------------------------------
 *
 * NsJavaInitializeJvmCache --
 *
 *     Initialize the jvm environment pointer cache.  
 *
 * Results:
 *	Cache is initialed along with a mutex for global locking.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static void 
NsJavaInitializeNsJavaEnv(NsJavaEnvCache *envCache)
{
  char *func = "InitializeNsJavaEnv";

  Ns_ModLog(Debug, javaModLogHandle, "%s: Initializing nsjava environment", 
            func);

  Ns_MutexInit(&envCache->lock);
  Ns_MutexInit(&envCache->wait_lock);
  Ns_MutexInit(&envCache->init_lock);
  envCache->initCompleteP = NS_FALSE;
  envCache->main_thread_id = 0;
  envCache->shutdownP = NS_FALSE;
}

/*
 *----------------------------------------------------------------------
 *
 * NsJava_SetJdk1_x_Options --
 *
 *  Sets jdk version specific options.  This function is conditionally 
 *  compiled depending on whether it is a version 1.1 or version 1.2 jdk.
 *  The version below is for 1.1 jdk's.
 *
 * Results:
 *	JDK1_1InitArgs structure is filled in.  
 *
 * Side effects:
 *	Memory is allocated for holding the classpath string.
 *
 *----------------------------------------------------------------------
 */


#if (JVM_VERSION == JVM1_1)
static void 
NsJava_SetJdk1_x_Options(NsJavaVMInitArgs *vm_args)
{
  char        *func = "NsJava_SetJdk1_1_Options";
  char        *moduleConfigPath;
  char        *classpath;
  char       **props;
  Ns_DString   path;
  int          flag;
  int          rc;

  vm_args->classpath = NULL;

  moduleConfigPath = Ns_ConfigGetPath(Ns_ConnServer(NULL), "nsjava", NULL);
  rc = Ns_ConfigGetBool(moduleConfigPath, "VerboseJvm", &flag);
  vm_args->verbose = (rc == NS_TRUE) ? flag : 0;

  rc = Ns_ConfigGetBool(moduleConfigPath, "DisableJITCompiler", &flag);
  if(rc == NS_TRUE && flag == NS_TRUE) {

    props               = (char**)Ns_Malloc(sizeof(char*));
    props[0]            = "java.compiler=NONE";
    vm_args->properties = props;    
  }

  /*
   * Add the classpath as a prefix to the default classpath.
   */

  Ns_DStringInit(&path);

  classpath = Ns_ConfigGetValue(moduleConfigPath, "ClassPath");
  if(classpath != NULL) {
    Ns_DStringAppend(&path, classpath);
  }

  if (Ns_DStringLength(&path) > 0 && vm_args->classpath != NULL) {
    Ns_DStringAppend(&path, (char*)PATH_SEPARATOR);
    Ns_DStringAppend(&path, vm_args->classpath);
    vm_args->classpath = Ns_DStringExport(&path);
  } else if (Ns_DStringLength(&path) > 0) {
    vm_args->classpath = Ns_DStringExport(&path);
  }

  Ns_ModLog(Debug, javaModLogHandle, "%s: CLASSPATH = %s", func,
            vm_args->classpath);

  Ns_DStringFree(&path);

}

/* else JVM_VERSION == JVM1_1 */
#else 

/*
 *----------------------------------------------------------------------
 *
 * NsJava_SetJdk1_x_Options -- (same as preceding function except this is 
 *                              jdk 1.2 version).
 *
 *  Sets jdk version specific options.  This function is conditionally 
 *  compiled depending on whether it is a version 1.1 or version 1.2 jdk.
 *  The version below is for 1.2 jdk's.
 *
 * Results:
 *	JavaVMInitArgs and JavaVMOption structures are filled in.  
 *
 * Side effects:
 *	Memory is allocated for holding the classpath string.
 *
 *----------------------------------------------------------------------
 */


static void 
NsJava_SetJdk1_x_Options(NsJavaVMInitArgs *vm_args)
{
  char           *func = "NsJava_SetJdk1_2_Options";
  char           *moduleConfigPath;
  char           *classpath;
  char          **props;
  Ns_DString      opt;
  NsJavaVMOption *options;
  int             flag;
  int             rc;

  vm_args->version = JNI_VERSION_1_2;
  options = (NsJavaVMOption *) Ns_Calloc(3, sizeof(NsJavaVMOption));
  vm_args->options = options;
  vm_args->ignoreUnrecognized = JNI_TRUE;
  vm_args->nOptions = 0;

  Ns_DStringInit(&opt);

  moduleConfigPath = Ns_ConfigGetPath(Ns_ConnServer(NULL), "nsjava", NULL);
  classpath        = Ns_ConfigGetValue(moduleConfigPath, "ClassPath");
  if(classpath != NULL) {
    Ns_DStringAppend(&opt, "-Djava.class.path=");
    Ns_DStringAppend(&opt, classpath);
    options[vm_args->nOptions].optionString = Ns_DStringExport(&opt);
    options[vm_args->nOptions].extraInfo = NULL;
    Ns_ModLog(Debug, javaModLogHandle, "%s: CLASSPATH = %s", func, classpath);
    vm_args->nOptions++;
  }

  Ns_DStringTrunc(&opt, 0);
  rc = Ns_ConfigGetBool(moduleConfigPath, "DisableJITCompiler", &flag);
  if(rc == NS_TRUE && flag == NS_TRUE) {
    Ns_DStringAppend(&opt, "-Djava.compiler=NONE");
    options[vm_args->nOptions].optionString = Ns_DStringExport(&opt);
    options[vm_args->nOptions].extraInfo = NULL;
    vm_args->nOptions++;
  }

  rc = Ns_ConfigGetBool(moduleConfigPath, "VerboseJvm", &flag);
  if(rc == NS_TRUE && flag == NS_TRUE) {
    options[vm_args->nOptions].optionString = "-verbose";
    options[vm_args->nOptions].extraInfo = NULL;
    vm_args->nOptions++;
  }

  Ns_DStringFree(&opt);
}
/* endif JVM_VERSION == JVM1_1 */
#endif

/*
 *----------------------------------------------------------------------
 *
 * getVmArgs --
 *
 *  Get the equivalent command line args for the jvm form the server init
 *  file.
 *
 * Results:
 *	JDK1_1InitArgs structure is filled in.  
 *
 * Side effects:
 *	Memory is allocated for holding the classpath string.
 *
 *----------------------------------------------------------------------
 */


static int
getVmArgs(NsJavaVMInitArgs *vm_args, void *handle) 
{
  char        *func = "getVmArgs";

  GetDefaultJavaVMInitArgs = dlsym(handle, GET_JVM_DEFAULT_ARGS_FUNC);

  if(GetDefaultJavaVMInitArgs == NULL) {
    return NS_ERROR;
  }

  /*
   * Setup the jvm input arguments.
   */

  vm_args->version = JVM_VERSION;

  if(GetDefaultJavaVMInitArgs(vm_args) < 0) {
    Ns_ModLog(Debug, javaModLogHandle, "%s: %s failed",
              func, GET_JVM_DEFAULT_ARGS_FUNC);
    return NS_ERROR;
  }

  NsJava_SetJdk1_x_Options(vm_args);

  return NS_OK;
}

/*
 *----------------------------------------------------------------------
 *
 *  NsJavaStartJvm --
 *
 *  This is function runs in a separate thread and starts the jvm.
 *
 * Results:
 *	jvm is started in the aolserver process.
 *
 * Side effects:
 *	This thread suspends until shutdown is signaled.
 *
 *----------------------------------------------------------------------
 */


static void
NsJavaStartJvm(void *context) 
{
  extern NsJavaVMInitArgs vm_args;

  char               *func = "StartJvm";
  char               *modConfigPath;
  void               *handle;
  ThreadSpecificData *tsdPtr;
  NsJavaEnvCache     *envCache;
  JNIEnv             *env;
  int                 DestroyJvmP;
  int                 flag;
  int                 rc;
  int                 tid;

  Ns_ThreadSetName("-jvm-");

  handle = dlopen(LIBJAVA, RTLD_NOW | RTLD_GLOBAL);
  if(handle == NULL) {
    Ns_ModLog(Error, javaModLogHandle, 
              "%s: unable to get %s library handle", func, LIBJAVA);
    invoke_rc = NS_ERROR;
    return;
  }

  Ns_ModLog(Debug, javaModLogHandle, "%s: 0 JVMs in proc. make one.", func);
  if(getVmArgs(&vm_args, handle) == NS_ERROR) {
    Ns_ModLog(Error, javaModLogHandle, "%s: error in getVmArgs", func);
    invoke_rc = NS_ERROR;
    return;
  }

  /* 
   * The vm args are now ready, let's create a jvm.  javaVM and env
   * will both be initialized.
   */

  CreateJavaVM = dlsym(handle, CREATE_JVM_FUNC);
  dlclose(handle);

  if(CreateJavaVM == NULL) {
    Ns_ModLog(Error, javaModLogHandle, 
              "%s: failed to look-up %s", func, CREATE_JVM_FUNC);
    invoke_rc = NS_ERROR;
    return;
  }

  if (CreateJavaVM(&javaVM, &env, &vm_args) < 0) {
    Ns_ModLog(Error, javaModLogHandle, "%s: CreateJavaVM failed", func);
    invoke_rc = NS_ERROR;
    return;
  }

  if (env == NULL || javaVM == NULL) {
    Ns_ModLog(Error,javaModLogHandle,"%s: JVM init failed,", func);
    invoke_rc = NS_ERROR;
    return;
  }

  Ns_ModLog(Debug, javaModLogHandle, "%s: CreateJavaVM worked", func);

  /* 
   * once the jvm is started, the main thread id and the jvm env pointer are 
   * saved for future reference.  Java's main thread is different than 
   * aolserver's main thread to prevent signal handling conflicts between 
   * the two.
   */

  tid = Ns_GetThreadId();

  Ns_ModLog(Debug, javaModLogHandle, "%s: save env = %u, tid = %u", 
            func, env, tid);

  envCache = NsJavaGetThreadEnvCache();
  tsdPtr   = TCL_TSD_INIT(&javaDataKey);

  tsdPtr->currentEnv = env;
  Ns_ModLog(Debug, javaModLogHandle, "%s: init jcache", func);

  if(JavaInitializeJcache(NULL, env, &tsdPtr->jcache) == TCL_ERROR) {
    Ns_ModLog(Error,javaModLogHandle,"%s: JCACHE initialization error,", func);
    invoke_rc = NS_ERROR;
    return;
  }

  /*
   * Register the TclObject type
   */

  JavaObjInit();

  tsdPtr->initialized = NS_TRUE;
  tsdPtr->do_cleanup  = NS_FALSE;

  Ns_MutexLock(&envCache->lock);
  envCache->main_thread_id = tid;
  Ns_UnlockMutex(&envCache->lock);

  /*
   * The jvm is in place. Signal the aolserver startup thread that it 
   * can continue.
   */

  invoke_rc = NS_OK;
  Ns_MutexLock(&envCache->init_lock);
  envCache->initCompleteP = NS_TRUE;
  Ns_MutexUnlock(&envCache->init_lock);
  Ns_ThreadYield();

  /*
   * Check to see if the jvm should be destroyed on exit.  1.1 jvm's segfault.
   */

  modConfigPath = Ns_ConfigGetPath(Ns_ConnServer(NULL), "nsjava", NULL);
  rc            = Ns_ConfigGetBool(modConfigPath, "DestroyJvm", &flag);
  DestroyJvmP   = (rc == NS_TRUE) ? flag : NS_FALSE;

  /* 
   * Idle this thread until a shutdown is signalled.  Yes this is a kludge,
   * but because of signal handling conflicts between the jvm's garbage 
   * collector and aolserver's blocking of signals in the main thread, we
   * must start the jvm in another thread besides aolserver's main thread.
   * We also need to use a separate thread for the jvm because for 1.1 jdk's
   * the jvm thread must be destroyed in the thread that created it.
   */

  Ns_ModLog(Debug, javaModLogHandle, "%s: thread done, waiting", func);
  Ns_MutexLock(&envCache->wait_lock);
  while(envCache->shutdownP == NS_FALSE) {
    Ns_MutexUnlock(&envCache->wait_lock);
    sleep(1);
    Ns_MutexLock(&envCache->wait_lock);
  }

  Ns_MutexUnlock(&envCache->wait_lock);

  if(javaVM != NULL && DestroyJvmP == NS_TRUE) {

    Ns_ModLog(Notice, javaModLogHandle, "%s: Destroying jvm", func);
    (*javaVM)->DestroyJavaVM(javaVM);
    javaVM = NULL;
  }
}

/*
 *----------------------------------------------------------------------
 *
 *  NsJava_ShutdownJava --
 *
 *  Called at server shutdown.
 *
 * Results:
 *	Sends a signal to the jvm main thread so that it will destroy the
 *      jvm.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

NS_EXPORT void 
NsJava_ShutdownJava(void *context) 
{
  char           *func = "ShutdownJava";
  NsJavaEnvCache *envCache;

  envCache = NsJavaGetThreadEnvCache();

  /* 
   * Signal the main the jvm thread to destroy the jvm.
   */

  Ns_ModLog(Debug, javaModLogHandle, "%s: signalling shutdown", func);
  Ns_MutexLock(&envCache->wait_lock);
  envCache->shutdownP = NS_TRUE;

  Ns_MutexUnlock(&envCache->wait_lock);
  Ns_ModLog(Debug, javaModLogHandle, "%s: shutdown complete", func);
}

/*
 *----------------------------------------------------------------------
 *
 *  NsJava_RegisterTclJavaFunctions --
 *
 *  Called at server startup to register the ns_java command..
 *
 * Results:
 *	Returns NS_OK
 *
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

NS_EXPORT int
NsJava_RegisterTclJavaFunctions(Tcl_Interp *interp, void *context)
{
  char     *func = "RegisterTclJavaFunctions";
  ThreadSpecificData *tsdPtr;
  JNIEnv   *env;
  jlong     lvalue;
  jlong     tid;
  jobject   interpObj;
  jobject   local;
  JavaInfo *jcache;
  int       result;
  Tcl_Obj  *resultPtr;

  Ns_ModLog(Debug, javaModLogHandle, "%s: registering tclblend cmds", func);

  env    = NsJava_GetThreadEnv();
  jcache = NsJava_JavaGetCache();

  /*
   * setup thread-specific data for thread-cleanup
   */

  tsdPtr             = TCL_TSD_INIT(&javaDataKey);
  tsdPtr->do_cleanup = NS_FALSE;

  Ns_ModLog(Debug, javaModLogHandle, "%s: env = %u", func, env);
  Ns_ModLog(Debug, javaModLogHandle, "%s: jcache = %u", func, jcache);
  Ns_ModLog(Debug, javaModLogHandle, "%s: interp = %u", func, interp);
  
  /*
   * Initialize the java tcl interpreter object
   */

  *(Tcl_Interp**)&lvalue = interp;
  local = (*env)->NewObject(env, jcache->Interp,
                            jcache->interpC, lvalue);

  Ns_ModLog(Debug, javaModLogHandle, "%s: local = %u", func, local);
  

  if (!local) {

    jobject exception = (*env)->ExceptionOccurred(env);
    if (exception) { 
      (*env)->ExceptionDescribe(env);
      (*env)->ExceptionClear(env);
      (*env)->Throw(env, exception);
      (*env)->DeleteLocalRef(env, exception);
    }
    Ns_ModLog(Debug, javaModLogHandle, "%s: failed to init tclblend", func);

    return TCL_ERROR;
  }

  interpObj = (*env)->NewGlobalRef(env, local);
  (*env)->DeleteLocalRef(env, local);

  /*
   * Initialize the tclblend package - registers the tclblend commands
   */

  result = JavaInitBlend(env, interp, interpObj);

  return result;
}

/*
 *----------------------------------------------------------------------
 *
 * NsJavaThreadCleanup --
 *
 *	This method will be called when a Tcl thread is getting finalized.
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
NsJavaThreadCleanup(ClientData clientData)
{
  char               *func = "NsJavaThreadCleanup";
  JNIEnv             *env;
  JavaInfo           *jcache;
  ThreadSpecificData *tsdPtr = (ThreadSpecificData *)clientData;

  Ns_Log(Debug, "%s: currentEnv = %u, jcache = %u, initialized = %u",
         func, tsdPtr->currentEnv, tsdPtr->jcache, tsdPtr->initialized);

  /* 
   * check to see if we are being cleaned-up up before the interpreter?
   */

  if(tsdPtr->do_cleanup == NS_FALSE) {

    Ns_Log(Debug, "%s: do_cleanup == 0 -> resetting tsd data", func);
    Ns_Log(Debug, "%s: currentEnv = %u, jcache = %u, initialized = %u",
           func, tsdPtr->currentEnv, tsdPtr->jcache, tsdPtr->initialized);
    /*
     * re-set the thread-local-storage so that we get cleaned up after the 
     * interpreter.
     */

    Ns_TlsSet((Ns_Tls*)&javaDataKey, tsdPtr);
    return;
  }
  else {

    /*
     * interpreter is already cleaned up, so we can allow this thread-local-
     * storage to be cleaned up too.
     */

    Ns_Log(Debug, "%s: do_cleanup = 1 -> cleaning jcache", func);
    Ns_Log(Debug, "%s: currentEnv = %u, jcache = %u, initialized = %u",
           func, tsdPtr->currentEnv, tsdPtr->jcache, tsdPtr->initialized);
    Ns_ModLog(Debug, javaModLogHandle, "%s: cleaning cache", func);
    NsJavaCacheCleanup(tsdPtr->currentEnv, &tsdPtr->jcache);
    ns_free(tsdPtr);
    tsdPtr  = NULL;
  }


  Ns_ModLog(Debug, javaModLogHandle, "%s: detaching jvm thread", func);
  (*javaVM)->DetachCurrentThread(javaVM);

  return;
}


/*
 *----------------------------------------------------------------------
 *
 *  NsJava_GetThreadEnv --
 *
 *  Gets the current jvm environment pointer, by either attaching to the jvm
 *  of by pulling out the previously saved value from a cache.
 *
 *
 * Results:
 *      returns the jvm environment pointer.
 *
 *
 * Side effects:
 *	jvm is attached to a thread.
 *
 *----------------------------------------------------------------------
 */

NS_EXPORT JNIEnv *
NsJava_GetThreadEnv()		
{
  char               *func = "GetThreadEnv";
  NsJavaEnvCache     *envCache;
  JNIEnv             *env;
  NsJavaThreadEnv    *thrEnv;
  ThreadSpecificData *tsdPtr;
  int                 tid;
  int                 cnt;

  envCache = NsJavaGetThreadEnvCache();
  tid      = Ns_GetThreadId();
  tsdPtr   = TCL_TSD_INIT(&javaDataKey);

  /* 
   *  Check to see if the are any cached environments.  If there are, check
   *  for a cached environment that corresponds to the current thread id.
   */

  if(tsdPtr->initialized == 1) {

    env = tsdPtr->currentEnv;
    Ns_ModLog(Debug,javaModLogHandle,"%s: got env %u from cache", func, env);

    return env;    
  }

  /*
   * O.k. there are no cached environments for this thread, let's attach
   * to the jvm and save the environment pointer for later use in this 
   * thread.
   */

  Ns_ModLog(Debug, javaModLogHandle, "%s: attach to JVM ", func);
  if ((*javaVM)->AttachCurrentThread(javaVM,(NsJava_JNIEnv*)&env,NULL) != 0) 
  {
    Ns_ModLog(Error, javaModLogHandle, "%s: attach to JVM failed", func);
    return NULL;
  }

  Ns_ModLog(Debug, javaModLogHandle, "%s: attach to JVM worked", func);
  Ns_ModLog(Debug, javaModLogHandle, "%s: save env %u tid = %d", 
            func, env, tid);

  tsdPtr->currentEnv = env;

  if(JavaInitializeJcache(NULL,env,&tsdPtr->jcache) == TCL_ERROR) {
    Ns_ModLog(Error,javaModLogHandle,"%s: JCACHE initialization error,", func);
    return NULL;
  }

  /*
   * Indicate that this thread was successfully initialized..
   */

  tsdPtr->initialized = 1;

  return env;
}


/*
 *----------------------------------------------------------------------
 *
 *  NsJavaGetThreadEnvCache --
 *
 *  Return a pointer to the global environment cache.  This cache holds all
 *  of the env pointers for all of the currently active java threads that are
 *  attached to aolserver threads.
 *
 * Results:
 *      pointer to global jvm env cache.
 *
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static NsJavaEnvCache *
NsJavaGetThreadEnvCache(void)
{
  extern NsJavaEnvCache nsjavaEnv;

  return &nsjavaEnv;
}


/*
 *----------------------------------------------------------------------
 *
 * NsJava_JavaGetCache --
 *
 *	Retrieve the JNI class, method, and field cache for the
 *	current thread.
 *
 * Results:
 *	Returns the JavaInfo pointer for the current thread.
 *	This method  must be called after the jvm env pointer is initialized
 *      for the current thread..
 *
 * Side effects:
 *
 *----------------------------------------------------------------------
 */

NS_EXPORT JavaInfo*
NsJava_JavaGetCache()
{
    ThreadSpecificData *tsdPtr = TCL_TSD_INIT(&javaDataKey);

    assert(tsdPtr->initialized);

    return &(tsdPtr->jcache);
}



/*
 *----------------------------------------------------------------------
 *
 * NsJavaCacheCleanup --
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
NsJavaCacheCleanup(JNIEnv *env, JavaInfo *jcache)
{

    /* 
     * FIXME: need to add code to check for case where NsJavaThreadCleanup is 
     * called first 
     */

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

