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


#include <nsjava.h>

static jobjectArray handleError(JNIEnv *env, Ns_DString *msg);
static char* cmd = "ns_db";

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1open
(JNIEnv *env, jobject jvm, jstring handle, jstring driver, jstring datasource, 
 jstring user, jstring password) 
{
  throwByName(env,"java/lang/NoSuchMethodException",
              "unsupported ns_db method: open", NULL);
  return NULL;
}

JNIEXPORT void JNICALL Java_nsjava_NsDb__1close
(JNIEnv *env, jobject jvm, jstring handle) 
{
  throwByName(env,"java/lang/NoSuchMethodException",
              "unsupported ns_db method: close", NULL);
}

JNIEXPORT jobjectArray JNICALL Java_nsjava_NsDb_pools
(JNIEnv *env, jobject jvm) 
{
  Ns_DString   errmsg;
  jobjectArray jargs;
  jstring      jstr;
  Tcl_Interp  *interp;
  char        *null_str = "";
  char        *pools;
  char        *save_pools;
  int          n_args = 0;
  int          i;

  interp  = NsJava_GetTclInterp(Ns_GetConn());
  pools   = Ns_DbPoolList(Ns_TclInterpServer(interp));

  if (pools != NULL) {

    Ns_DStringInit(&errmsg);
    save_pools = pools;

    for (n_args = 0; *pools != '\0'; pools += strlen(pools) + 1)
      n_args++;

    pools = save_pools;
    jstr  = (*env)->NewStringUTF(env,null_str);
    if (errCheck(jstr == NULL ? 1 : 0, &errmsg, "out of memory.")) {
      return handleError(env, &errmsg);
    }

    jargs = (*env)->NewObjectArray(env, n_args, 
                                   (*env)->FindClass(env, "java/lang/String"),
                                   jstr);
    if (errCheck(jargs == NULL ? 1 : 0, &errmsg, "can't allocate array.")) {
      return handleError(env, &errmsg);
    }
    for(i = 0; i < n_args; i++) {

      jstr  = (*env)->NewStringUTF(env, pools);
      if (errCheck(jstr == NULL ? 1 : 0, &errmsg, "jstr: out of memory.")) {
        return handleError(env, &errmsg);
      }
      (*env)->SetObjectArrayElement(env, jargs, i, jstr);
      if(checkForException(env) == JNI_TRUE) {
        Ns_DStringAppend(&errmsg, "Exception occured");
        return handleError(env, &errmsg);
      }

      pools = pools + strlen(pools) + 1;
    }
    Ns_DStringFree(&errmsg);
  }
  else {
    throwByName(env, "java/sql/SQLException","no pools defined", NULL);
    
    return NULL;
  }  

  return jargs;
}

static jobjectArray handleError(JNIEnv *env, Ns_DString *msg)
{
  throwByName(env, "java/sql/SQLException", msg->string, NULL);
  Ns_DStringFree(msg);
  
  return NULL;
}


JNIEXPORT void JNICALL Java_nsjava_NsDb_bouncepool
(JNIEnv *env, jobject jvm, jstring pl) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;
  
  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "bouncepool";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, pl, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, pl, argv[2]);
  Ns_Free(argv);  
  
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1gethandle
(JNIEnv *env, jobject jvm, jstring timeout, jstring poolname, jstring nh) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 6;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "gethandle";
  argv[2] = "-timeout";
  argv[3] = (char*)(*env)->GetStringUTFChars(env, timeout, JNI_FALSE);
  argv[4] = (char*)(*env)->GetStringUTFChars(env, poolname, JNI_FALSE);
  if(STREQ(argv[4],"undef")) {
    (*env)->ReleaseStringUTFChars(env, poolname, argv[4]);    
    argc = 4;
    argv[4] = NULL;
  }
  else {
    argv[5] = (char*)(*env)->GetStringUTFChars(env, nh, JNI_FALSE);
  }
   
  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, timeout, argv[3]);
  if(argc == 6) {
    (*env)->ReleaseStringUTFChars(env, poolname, argv[4]);
    (*env)->ReleaseStringUTFChars(env, nh, argv[5]);
  }
  Ns_Free(argv);  
  
  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1exception
(JNIEnv *env, jobject jvm, jstring handle) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "exception";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_Free(argv);  
  
  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1poolname
(JNIEnv *env, jobject jvm, jstring handle) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;
 
  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "poolname";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_Free(argv);  
  
  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1password
(JNIEnv *env, jobject jvm, jstring handle) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;
  
  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "password";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_Free(argv);  
  
  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1user
(JNIEnv *env, jobject jvm, jstring handle) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;  

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "user";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_Free(argv);  
  
  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1dbtype
(JNIEnv *env, jobject jvm, jstring handle) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "dbtype";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1driver
(JNIEnv *env, jobject jvm, jstring handle) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "driver";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_Free(argv);  
  
  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1datasource
(JNIEnv *env, jobject jvm, jstring handle) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "datasource";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_Free(argv);    

  return (*env)->NewStringUTF(env, interp->result); 
}

JNIEXPORT void JNICALL Java_NsDb__1disconnect
(JNIEnv *env, jobject jvm, jstring handle) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "disconnect";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_Free(argv);    
}

JNIEXPORT void JNICALL Java_nsjava_NsDb__1flush
(JNIEnv *env, jobject jvm, jstring handle) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "flush";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_Free(argv);    
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1bindrow
(JNIEnv *env, jobject jvm, jstring handle) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;
  
  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "bindrow";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_Free(argv);  
  
  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT void JNICALL Java_nsjava_NsDb__1releasehandle
(JNIEnv *env, jobject jvm, jstring handle) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;
  
  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "releasehandle";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_Free(argv);    
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1resethandle
(JNIEnv *env, jobject jvm, jstring handle) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;
  
  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "resethandle";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_Free(argv);  
  
  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1cancel
(JNIEnv *env, jobject jvm, jstring handle) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;
  
  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "cancel";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_Free(argv);  
  
  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1connected
(JNIEnv *env, jobject jvm, jstring handle) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;
  
  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "connected";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_Free(argv);  
  
  return (*env)->NewStringUTF(env, interp->result);
}


JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1sp_1exec
(JNIEnv *env, jobject jvm, jstring handle) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;
  
  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 3;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "sp_exec";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_Free(argv);  
  
  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1sp_1returncode
(JNIEnv *env, jobject jvm, jstring handle) 
{
  throwByName(env,"java/lang/NoSuchMethodException",
              "unsupported ns_db method: sp_returncode", NULL);
  return NULL;
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1sp_1getparams
(JNIEnv *env, jobject jvm, jstring handle) 
{
  throwByName(env,"java/lang/NoSuchMethodException",
              "unsupported ns_db method: sp_getparams", NULL);
  return NULL;
}


JNIEXPORT void JNICALL Java_nsjava_NsDb__1dml
(JNIEnv *env, jobject jvm, jstring handle, jstring jsql) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;
  
  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 4;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "dml";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, jsql, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  (*env)->ReleaseStringUTFChars(env, jsql, argv[3]);
  Ns_Free(argv);    
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1select_10or1row
(JNIEnv *env, jobject jvm, jstring handle, jstring jsql) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 4;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "0or1row";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, jsql, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  (*env)->ReleaseStringUTFChars(env, jsql, argv[3]);
  Ns_Free(argv);  
  
  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1select_11row
(JNIEnv *env, jobject jvm, jstring handle, jstring jsql) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;
  
  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 4;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "1row";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, jsql, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  (*env)->ReleaseStringUTFChars(env, jsql, argv[3]);
  Ns_Free(argv);  
  
  return (*env)->NewStringUTF(env, interp->result);
}


JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1select
(JNIEnv *env, jobject jvm, jstring handle, jstring jsql) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;  

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 4;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "select";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, jsql, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  (*env)->ReleaseStringUTFChars(env, jsql, argv[3]);
  Ns_Free(argv);  
  
  return (*env)->NewStringUTF(env, interp->result);
}


static int
BadArgs(Tcl_Interp *interp, char **argv, char *args)
{
    Tcl_AppendResult(interp, "wrong # of args: should be \"",
        argv[0], " ", argv[1], " ", args, "\"", NULL);

    return TCL_ERROR;
}

static int
DbGetHandle(Tcl_Interp *interp, char *handleId, Ns_DbHandle **handle,
	    Tcl_HashEntry **phe)
{
    Tcl_HashEntry  *he;
    TclData *tdPtr;

    tdPtr = NsTclGetData(interp);
    he = Tcl_FindHashEntry(&tdPtr->dbs, handleId);
    if (he == NULL) {
	Tcl_AppendResult(interp, "invalid database id:  \"", handleId, "\"",
	    NULL);
	return TCL_ERROR;
    }

    *handle = (Ns_DbHandle *) Tcl_GetHashValue(he);
    if (phe != NULL) {
	*phe = he;
    }

    return TCL_OK;
}

static int
DbFail(Tcl_Interp *interp, Ns_DbHandle *handle, char *cmd)
{
    Tcl_AppendResult(interp, "Database operation \"", cmd, "\" failed", NULL);
    if (handle->cExceptionCode[0] != '\0') {
        Tcl_AppendResult(interp, " (exception ", handle->cExceptionCode,
			 NULL);
        if (handle->dsExceptionMsg.length > 0) {
            Tcl_AppendResult(interp, ", \"", handle->dsExceptionMsg.string,
			     "\"", NULL);
        }
        Tcl_AppendResult(interp, ")", NULL);
    }

    return TCL_ERROR;
}

/*
 * This is here for debugging only.  It will go away when I'm satisfied that
 * everything is stable.
 */
JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1selecta
(JNIEnv *env, jobject jvm, jstring handle, jstring jsql) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Ns_DbHandle *handlePtr;
  Tcl_HashEntry  *hPtr;
  Tcl_Interp *interp;
  jstring jstr;
  Ns_Set *rowPtr;
  char **argv;
  int argc;
  int retval;  

  Ns_ModLog(Debug, javaModLogHandle, "enter select");

  conn    = Ns_GetConn();
  //interp  = Ns_GetConnInterp(conn);
  interp  = Ns_TclAllocateInterp("ignore");
  argc    = 4;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "select";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, jsql, JNI_FALSE);

  Ns_ModLog(Debug, javaModLogHandle, "starting select");
  Tcl_ResetResult(interp);
  //NsTclDbCmd(dummy, interp, argc, argv);

  Ns_ModLog(Debug, javaModLogHandle, "check args");
  if (argc < 3) {
    retval = BadArgs(interp, argv, "dbId ?args?");
  }
  Ns_ModLog(Debug, javaModLogHandle, "get handle");
  if (DbGetHandle(interp, argv[2], &handlePtr, &hPtr) != TCL_OK) {
    retval = TCL_ERROR;
  }
  Ns_DStringFree(&handlePtr->dsExceptionMsg);
  handlePtr->cExceptionCode[0] = '\0';

  Ns_ModLog(Debug, javaModLogHandle, "select");
  rowPtr = Ns_DbSelect(handlePtr, argv[3]);
  if (rowPtr == NULL) {
    retval = DbFail(interp, handlePtr, argv[1]);
  }
  Ns_ModLog(Debug, javaModLogHandle, "enter set, interp = %u, rowPtr = %u",
            interp, rowPtr);
  Ns_TclEnterSet(interp, rowPtr, 0);

  retval = TCL_OK;

  Ns_ModLog(Debug, javaModLogHandle, "back from tcl");
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  Ns_ModLog(Debug, javaModLogHandle, "released handle string");
  (*env)->ReleaseStringUTFChars(env, jsql, argv[3]);
  Ns_ModLog(Debug, javaModLogHandle, "released sql string");
  Ns_Free(argv);  
  Ns_ModLog(Debug, javaModLogHandle, "freed argv array");
  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1exec
(JNIEnv *env, jobject jvm, jstring handle, jstring jsql) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;
  
  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 4;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "exec   ";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, jsql, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  (*env)->ReleaseStringUTFChars(env, jsql, argv[3]);
  Ns_Free(argv);  
  
  return (*env)->NewStringUTF(env, interp->result);
}


JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1interpretsqlfile
(JNIEnv *env, jobject jvm, jstring handle, jstring jsql) 
{
  throwByName(env,"java/lang/NoSuchMethodException",
              "unsupported ns_db method: interpretsqlfile", NULL);
  return NULL;
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1sp_1start
(JNIEnv *env, jobject jvm, jstring handle, jstring jsql) 
{
  throwByName(env,"java/lang/NoSuchMethodException",
              "unsupported ns_db method: sp_start", NULL);
  return NULL;
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1getrow
(JNIEnv *env, jobject jvm, jstring handle, jstring setptr)
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 4;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "getrow";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, setptr, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  (*env)->ReleaseStringUTFChars(env, setptr, argv[3]);
  Ns_Free(argv);  
  

  return (*env)->NewStringUTF(env, interp->result);
} 

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1verbose
(JNIEnv *env, jobject jvm, jstring handle, jstring on_off)
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;     

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 4;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "verbose";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, handle, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, on_off, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclDbCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, handle, argv[2]);
  (*env)->ReleaseStringUTFChars(env, on_off, argv[3]);
  Ns_Free(argv);    

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jobject JNICALL Java_nsjava_NsDb__1setexception
(JNIEnv *env, jobject jvm, jstring handle, jstring jsql) 
{
  throwByName(env,"java/lang/NoSuchMethodException",
              "unsupported ns_db method: setexception", NULL);
  return NULL;
}

JNIEXPORT jstring JNICALL Java_nsjava_NsDb__1sp_1setparam
(JNIEnv *env, jobject jvm, jstring handle, jstring jsql) 
{
  throwByName(env,"java/lang/NoSuchMethodException",
              "unsupported ns_db method: sp_setparam", NULL);
  return NULL;
}


