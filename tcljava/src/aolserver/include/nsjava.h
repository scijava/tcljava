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

#ifndef _NSJAVA
#define _NSJAVA
#include <ns.h>
#include <nsd.h>
#include <tcl.h>
#define _UINT64_T
#define _UINT32_T
#include <jni.h>
#include <dlfcn.h>
#include <java.h>
#include <nsjava_NsLog.h>
#include <nsjava_NsDb.h>
#include <nsjava_NsPg.h>
#include <nsjava_NsSet.h>
#include <javaNative.h>

#define PATH_SEPARATOR ":"

#ifdef USE_MONITOR
#define ENTER_MONITOR enterMonitor(env);
#define EXIT_MONITOR exitMonitor(env);
#else
#define ENTER_MONITOR
#define EXIT_MONITOR
#endif



#ifdef NSVERSION_3_0_PLUS
//aolserver 3.1 deleted modlog functionality

typedef int Ns_ModLogHandle;
void Ns_ModLog(Ns_LogSeverity severity, Ns_ModLogHandle handle, char *fmt, ...);
void Ns_ModLogRegister(char *realm, Ns_ModLogHandle *handle);
void Ns_ModLogSetThreshold(Ns_ModLogHandle handle, Ns_LogSeverity severity);
#endif

/* dynamic loading symbols for jdk 1.1 or jdk 1.2 */

#define CREATE_JVM_FUNC "JNI_CreateJavaVM"
#define GET_JVM_DEFAULT_ARGS_FUNC "JNI_GetDefaultJavaVMInitArgs"

#ifdef USING_JDK1_2
#define LIBJAVA "libjvm.so"
typedef JavaVMInitArgs NsJavaVMInitArgs;
typedef JavaVMOption NsJavaVMOption;
typedef void* NsJava_JNIEnv;
#define JVM1_2 0x00010002
#define JVM_VERSION JVM1_2
#else
#define LIBJAVA "libjava.so"
typedef JDK1_1InitArgs NsJavaVMInitArgs;
typedef struct {
  char *optionString;
  void *extraInfo;
} NsJavaVMOption;
typedef JNIEnv* NsJava_JNIEnv;
#define JVM1_1 0x00010001
#define JVM_VERSION JVM1_1
#endif

/* envcache.c */

typedef struct {
  int          thread_Id;
  JNIEnv      *env;
} NsJavaThreadEnv;


typedef struct {
  int       main_thread_id;
  Ns_List  *threadEnv;
  Ns_Mutex  lock;
  Ns_Mutex  init_lock;
  int       initCompleteP;
  Ns_Mutex  wait_lock;
  int       shutdownP;
} NsJavaEnvCache;


void NsJava_InitializeJvmCache(NsJavaEnvCache*);
NsJavaThreadEnv *NsJava_JvmCachedEnvFromThreadId(NsJavaEnvCache *, int);
void NsJava_SaveCurrentThreadEnvInCache(NsJavaEnvCache *, JNIEnv *, int);
int NsJava_DeleteCurrentThreadEnvFromCache(NsJavaEnvCache *, int);
void NsJava_DestroyEnvCache(NsJavaEnvCache *);
void NsJava_RemoveCar(Ns_List *);
void NsJava_FreeNsJavaThreadEnv(NsJavaThreadEnv *);
int NsJava_JvmEnvCacheSize(NsJavaEnvCache *);

/* nsjava.c */

extern Ns_ModLogHandle javaModLogHandle;
extern NsJavaEnvCache jvmEnv;

JNIEnv *NsJava_GetThreadEnv();
void NsJava_ShutdownJava(void *);
int NsJava_RegisterTclJavaFunctions(Tcl_Interp *, void *);
void  NsJava_CleanupInterp(Tcl_Interp *, void *);
JNIEnv* NsJava_JvmGetThreadEnv(NsJavaEnvCache *);
Ns_Tls* NsJava_GetDataKey(void);
void* NsJavaGetData(Ns_Tls *keyPtr, int size);
	
/* tclcalljava.c */

int NsJava_JavaEvalCmd(ClientData, Tcl_Interp *, int, char**);
Tcl_Interp *NsJava_GetTclInterp(Ns_Conn *);

/* methods.c */

void throwByName(JNIEnv *, const char *, ...);
int checkForException(JNIEnv *);
char *toString(JNIEnv *, jobject);
int errCheck(int, Ns_DString *, char *);
void enterMonitor(JNIEnv *);
void exitMonitor(JNIEnv *);

/* log.c */

Ns_LogSeverity NsJava_ConvertSeverity(char *); 

#endif /* _NSJAVA */
