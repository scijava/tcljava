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

static char* cmd = "ns_set";

JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1new
(JNIEnv *env, jobject jvm, jstring name) 
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
  argv[1] = "create";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, name, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, name, argv[2]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}


JNIEXPORT jstring  JNICALL Java_nsjava_NsSet__1copy
(JNIEnv *env, jobject jvm, jstring ptr) 
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
  argv[1] = "copy";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}


JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1size
(JNIEnv *env, jobject jvm, jstring ptr) 
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
  argv[1] = "size";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1name
(JNIEnv *env, jobject jvm, jstring ptr) 
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
  argv[1] = "name";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT void JNICALL Java_nsjava_NsSet__1print
(JNIEnv *env, jobject jvm, jstring ptr) 
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
  argv[1] = "print";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  Ns_Free(argv);  
}

JNIEXPORT void JNICALL Java_nsjava_NsSet__1free
(JNIEnv *env, jobject jvm, jstring ptr) 
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
  argv[1] = "free";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  Ns_Free(argv);  
}

JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1find
(JNIEnv *env, jobject jvm, jstring ptr, jstring ky) 
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
  argv[1] = "find";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, ky, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, ky, argv[3]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1get
(JNIEnv *env, jobject jvm, jstring ptr, jstring ky) 
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
  argv[1] = "get";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, ky, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, ky, argv[3]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT void JNICALL Java_nsjava_NsSet__1delete__JLjava_lang_String_2
(JNIEnv *env, jobject jvm, jstring ptr, jstring ky) 
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
  argv[1] = "delete";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, ky, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, ky, argv[3]);
  Ns_Free(argv);  
}

JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1unique
(JNIEnv *env, jobject jvm, jstring ptr, jstring ky) 
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
  argv[1] = "unique";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, ky, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, ky, argv[3]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1ifind
(JNIEnv *env, jobject jvm, jstring ptr, jstring ky) 
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
  argv[1] = "ifind";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, ky, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, ky, argv[3]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1iget
(JNIEnv *env, jobject jvm, jstring ptr, jstring ky) 
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
  argv[1] = "iget";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, ky, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, ky, argv[3]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT void JNICALL Java_nsjava_NsSet__1idelete
(JNIEnv *env, jobject jvm, jstring ptr, jstring ky) 
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
  argv[1] = "idelete";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, ky, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, ky, argv[3]);
  Ns_Free(argv);  
}

JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1iunique
(JNIEnv *env, jobject jvm, jstring ptr, jstring ky) 
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
  argv[1] = "iunique";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, ky, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, ky, argv[3]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1value
(JNIEnv *env, jobject jvm, jstring ptr, jstring idx) 
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
  argv[1] = "value";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, idx, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, idx, argv[3]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1isnull
(JNIEnv *env, jobject jvm, jstring ptr, jstring idx) 
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
  argv[1] = "isnull";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, idx, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, idx, argv[3]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1key
(JNIEnv *env, jobject jvm, jstring ptr, jstring idx) 
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
  argv[1] = "key";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, idx, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, idx, argv[3]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT void JNICALL Java_nsjava_NsSet__1delete__JI
(JNIEnv *env, jobject jvm, jstring ptr, jstring idx) 
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
  argv[1] = "delete";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, idx, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, idx, argv[3]);
  Ns_Free(argv);  
}

JNIEXPORT void JNICALL Java_nsjava_NsSet__1truncate
(JNIEnv *env, jobject jvm, jstring ptr, jstring idx) 
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
  argv[1] = "truncate";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, idx, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, idx, argv[3]);
  Ns_Free(argv);  
}


JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1update
(JNIEnv *env, jobject jvm, jstring ptr, jstring ky, jstring vl) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 5;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "update";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, ky, JNI_FALSE);
  argv[4] = (char*)(*env)->GetStringUTFChars(env, vl, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, ky, argv[3]);
  (*env)->ReleaseStringUTFChars(env, vl, argv[4]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}


JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1icput
(JNIEnv *env, jobject jvm, jstring ptr, jstring ky, jstring vl) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 5;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "icput";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, ky, JNI_FALSE);
  argv[4] = (char*)(*env)->GetStringUTFChars(env, vl, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, ky, argv[3]);
  (*env)->ReleaseStringUTFChars(env, vl, argv[4]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1cput
(JNIEnv *env, jobject jvm, jstring ptr, jstring ky, jstring vl) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 5;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "cput";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, ky, JNI_FALSE);
  argv[4] = (char*)(*env)->GetStringUTFChars(env, vl, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, ky, argv[3]);
  (*env)->ReleaseStringUTFChars(env, vl, argv[4]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsSet__1put
(JNIEnv *env, jobject jvm, jstring ptr, jstring ky, jstring vl) 
{
  Ns_Conn *conn;
  ClientData dummy;
  Tcl_Interp *interp;
  jstring jstr;
  char **argv;
  int argc;

  conn    = Ns_GetConn();
  interp  = NsJava_GetTclInterp(conn);
  argc    = 5;
  argv    = (char**)Ns_Malloc(argc*sizeof(char*));
  argv[0] = cmd;
  argv[1] = "put";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, ptr, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, ky, JNI_FALSE);
  argv[4] = (char*)(*env)->GetStringUTFChars(env, vl, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, ptr, argv[2]);
  (*env)->ReleaseStringUTFChars(env, ky, argv[3]);
  (*env)->ReleaseStringUTFChars(env, vl, argv[4]);
  Ns_Free(argv);  

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT void JNICALL Java_nsjava_NsSet__1merge
(JNIEnv *env, jobject jvm, jstring high, jstring low) 
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
  argv[1] = "merge";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, high, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, low, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, high, argv[2]);
  (*env)->ReleaseStringUTFChars(env, low, argv[3]);
  Ns_Free(argv);  
}

JNIEXPORT void JNICALL Java_nsjava_NsSet__1move
(JNIEnv *env, jobject jvm, jstring to, jstring from) 
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
  argv[1] = "merge";
  argv[2] = (char*)(*env)->GetStringUTFChars(env, to, JNI_FALSE);
  argv[3] = (char*)(*env)->GetStringUTFChars(env, from, JNI_FALSE);

  Tcl_ResetResult(interp);
  NsTclSetCmd(dummy, interp, argc, argv);
  (*env)->ReleaseStringUTFChars(env, to, argv[2]);
  (*env)->ReleaseStringUTFChars(env, from, argv[3]);
  Ns_Free(argv);  
}
