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


JNIEXPORT jstring JNICALL Java_nsjava_NsPg__1db
(JNIEnv *env, jobject jvm, jstring handlePtr)
{
  Ns_DString argv;
  Tcl_Interp *interp;
  char *handle;
  jstring jstr;
  int result;

  handle = (char*)(*env)->GetStringUTFChars(env, handlePtr, JNI_FALSE);
  interp = NsJava_GetTclInterp(Ns_GetConn());

  Ns_DStringInit(&argv);
  Ns_DStringVarAppend(&argv, "ns_pg db ", handle, NULL);

  result = Tcl_Eval(interp, argv.string);
  (*env)->ReleaseStringUTFChars(env, handlePtr, handle);
  Ns_DStringFree(&argv);

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsPg__1host
(JNIEnv *env, jobject jvm, jstring handlePtr)
{
  Ns_DString argv;
  Tcl_Interp *interp;
  char *handle;
  jstring jstr;
  int result;

  handle = (char*)(*env)->GetStringUTFChars(env, handlePtr, JNI_FALSE);
  interp = NsJava_GetTclInterp(Ns_GetConn());

  Ns_DStringInit(&argv);
  Ns_DStringVarAppend(&argv, "ns_pg host ", handle, NULL);

  result = Tcl_Eval(interp, argv.string);
  (*env)->ReleaseStringUTFChars(env, handlePtr, handle);
  Ns_DStringFree(&argv);

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsPg__1options
(JNIEnv *env, jobject jvm, jstring handlePtr)
{
  Ns_DString argv;
  Tcl_Interp *interp;
  char *handle;
  jstring jstr;
  int result;

  handle = (char*)(*env)->GetStringUTFChars(env, handlePtr, JNI_FALSE);
  interp = NsJava_GetTclInterp(Ns_GetConn());

  Ns_DStringInit(&argv);
  Ns_DStringVarAppend(&argv, "ns_pg options ", handle, NULL);

  result = Tcl_Eval(interp, argv.string);
  (*env)->ReleaseStringUTFChars(env, handlePtr, handle);
  Ns_DStringFree(&argv);

  return (*env)->NewStringUTF(env, interp->result);
}


JNIEXPORT jstring JNICALL Java_nsjava_NsPg__1port
(JNIEnv *env, jobject jvm, jstring handlePtr)
{
  Ns_DString argv;
  Tcl_Interp *interp;
  char *handle;
  jstring jstr;
  int result;

  handle = (char*)(*env)->GetStringUTFChars(env, handlePtr, JNI_FALSE);
  interp = NsJava_GetTclInterp(Ns_GetConn());

  Ns_DStringInit(&argv);
  Ns_DStringVarAppend(&argv, "ns_pg port ", handle, NULL);

  result = Tcl_Eval(interp, argv.string);
  (*env)->ReleaseStringUTFChars(env, handlePtr, handle);
  Ns_DStringFree(&argv);

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsPg__1number
(JNIEnv *env, jobject jvm, jstring handlePtr)
{
  Ns_DString argv;
  Tcl_Interp *interp;
  char *handle;
  jstring jstr;
  int result;

  handle = (char*)(*env)->GetStringUTFChars(env, handlePtr, JNI_FALSE);
  interp = NsJava_GetTclInterp(Ns_GetConn());

  Ns_DStringInit(&argv);
  Ns_DStringVarAppend(&argv, "ns_pg number ", handle, NULL);

  result = Tcl_Eval(interp, argv.string);
  (*env)->ReleaseStringUTFChars(env, handlePtr, handle);
  Ns_DStringFree(&argv);

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsPg__1error
(JNIEnv *env, jobject jvm, jstring handlePtr)
{
  Ns_DString argv;
  Tcl_Interp *interp;
  char *handle;
  jstring jstr;
  int result;

  handle = (char*)(*env)->GetStringUTFChars(env, handlePtr, JNI_FALSE);
  interp = NsJava_GetTclInterp(Ns_GetConn());

  Ns_DStringInit(&argv);
  Ns_DStringVarAppend(&argv, "ns_pg error ", handle, NULL);

  result = Tcl_Eval(interp, argv.string);
  (*env)->ReleaseStringUTFChars(env, handlePtr, handle);
  Ns_DStringFree(&argv);

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsPg__1status
(JNIEnv *env, jobject jvm, jstring handlePtr)
{
  Ns_DString argv;
  Tcl_Interp *interp;
  char *handle;
  jstring jstr;
  int result;

  handle = (char*)(*env)->GetStringUTFChars(env, handlePtr, JNI_FALSE);
  interp = NsJava_GetTclInterp(Ns_GetConn());

  Ns_DStringInit(&argv);
  Ns_DStringVarAppend(&argv, "ns_pg status ", handle, NULL);

  result = Tcl_Eval(interp, argv.string);
  (*env)->ReleaseStringUTFChars(env, handlePtr, handle);
  Ns_DStringFree(&argv);

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT jstring JNICALL Java_nsjava_NsPg__1ntuples
(JNIEnv *env, jobject jvm, jstring handlePtr)
{
  Ns_DString argv;
  Tcl_Interp *interp;
  char *handle;
  jstring jstr;
  int result;

  handle = (char*)(*env)->GetStringUTFChars(env, handlePtr, JNI_FALSE);
  interp = NsJava_GetTclInterp(Ns_GetConn());

  Ns_DStringInit(&argv);
  Ns_DStringVarAppend(&argv, "ns_pg ntuples ", handle, NULL);

  result = Tcl_Eval(interp, argv.string);
  (*env)->ReleaseStringUTFChars(env, handlePtr, handle);
  Ns_DStringFree(&argv);

  return (*env)->NewStringUTF(env, interp->result);
}

JNIEXPORT void JNICALL Java_nsjava_NsPg__1blob_1dml_1file
(JNIEnv *env, jobject jvm, jstring handlePtr, jstring blob_id, 
 jstring filename)
{
  Ns_DString argv;
  Tcl_Interp *interp;
  jstring jstr;
  char *handle;
  char *id;
  char *fname;
  int result;

  handle = (char*)(*env)->GetStringUTFChars(env, handlePtr, JNI_FALSE);
  id     = (char*)(*env)->GetStringUTFChars(env, blob_id, JNI_FALSE);
  fname  = (char*)(*env)->GetStringUTFChars(env, filename, JNI_FALSE);
  interp = NsJava_GetTclInterp(Ns_GetConn());

  Ns_DStringInit(&argv);
  Ns_DStringVarAppend(&argv, "ns_pg blob_dml_file ", handle, NULL);
  Ns_DStringVarAppend(&argv, " ", id, " ", fname, NULL);

  result = Tcl_Eval(interp, argv.string);
  (*env)->ReleaseStringUTFChars(env, handlePtr, handle);
  (*env)->ReleaseStringUTFChars(env, blob_id, id);
  (*env)->ReleaseStringUTFChars(env, filename, fname);
  Ns_DStringFree(&argv);
}

JNIEXPORT void JNICALL Java_nsjava_NsPg__1blob_1select_1file
(JNIEnv *env, jobject jvm, jstring handlePtr, jstring blob_id, 
 jstring filename)
{
  Ns_DString argv;
  Tcl_Interp *interp;
  jstring jstr;
  char *handle;
  char *id;
  char *fname;
  int result;

  handle = (char*)(*env)->GetStringUTFChars(env, handlePtr, JNI_FALSE);
  id     = (char*)(*env)->GetStringUTFChars(env, blob_id, JNI_FALSE);
  fname  = (char*)(*env)->GetStringUTFChars(env, filename, JNI_FALSE);
  interp = NsJava_GetTclInterp(Ns_GetConn());

  Ns_DStringInit(&argv);
  Ns_DStringVarAppend(&argv, "ns_pg blob_select_file ", handle, NULL);
  Ns_DStringVarAppend(&argv, " ", id, " ", fname, NULL);

  result = Tcl_Eval(interp, argv.string);
  (*env)->ReleaseStringUTFChars(env, handlePtr, handle);
  (*env)->ReleaseStringUTFChars(env, blob_id, id);
  (*env)->ReleaseStringUTFChars(env, filename, fname);
  Ns_DStringFree(&argv);
}


JNIEXPORT jstring JNICALL Java_nsjava_NsPg__1ns_1column
(JNIEnv *env, jobject jvm, jstring handlePtr, jstring command, jstring table, 
 jstring column, jstring key)
{
  Ns_DString argv;
  Tcl_Interp *interp;
  jstring jstr;
  char *handle;
  char *cmd;
  char *tbl;
  char *col;
  char *ky;
  int result;

  handle = (char*)(*env)->GetStringUTFChars(env, handlePtr, JNI_FALSE);
  cmd    = (char*)(*env)->GetStringUTFChars(env, command, JNI_FALSE);
  tbl    = (char*)(*env)->GetStringUTFChars(env, table, JNI_FALSE);
  col    = (char*)(*env)->GetStringUTFChars(env, column, JNI_FALSE);
  ky     = (char*)(*env)->GetStringUTFChars(env, key, JNI_FALSE);
  interp = NsJava_GetTclInterp(Ns_GetConn());

  Ns_DStringInit(&argv);
  Ns_DStringVarAppend(&argv, "ns_column ", handle, NULL);
  Ns_DStringVarAppend(&argv, " ", cmd, " ", tbl, " ", col, " ", ky, NULL);

  result = Tcl_Eval(interp, argv.string);
  (*env)->ReleaseStringUTFChars(env, handlePtr, handle);
  (*env)->ReleaseStringUTFChars(env, command, cmd);
  (*env)->ReleaseStringUTFChars(env, table, tbl);
  (*env)->ReleaseStringUTFChars(env, column, col);
  (*env)->ReleaseStringUTFChars(env, key, ky);
  Ns_DStringFree(&argv);

  return (*env)->NewStringUTF(env, interp->result);
} 




JNIEXPORT jstring JNICALL Java_nsjava_NsPg__1ns_1table
(JNIEnv *env, jobject jvm, jstring handlePtr, jstring command, jstring table,
                                            jstring key)
{
  Ns_DString argv;
  Tcl_Interp *interp;
  char *handle;
  char *cmd;
  char *tbl;
  char *ky;
  jstring jstr;
  int result;

  handle = (char*)(*env)->GetStringUTFChars(env, handlePtr, JNI_FALSE);
  cmd    = (char*)(*env)->GetStringUTFChars(env, command, JNI_FALSE);
  tbl    = (char*)(*env)->GetStringUTFChars(env, table, JNI_FALSE);
  ky     = (char*)(*env)->GetStringUTFChars(env, key, JNI_FALSE);
  interp = NsJava_GetTclInterp(Ns_GetConn());

  Ns_DStringInit(&argv);
  Ns_DStringVarAppend(&argv, "ns_table ", handle, NULL);
  Ns_DStringVarAppend(&argv, " ", cmd, " ", tbl, " ", ky, NULL);

  result = Tcl_Eval(interp, argv.string);
  (*env)->ReleaseStringUTFChars(env, handlePtr, handle);
  (*env)->ReleaseStringUTFChars(env, command, cmd);
  (*env)->ReleaseStringUTFChars(env, table, tbl);
  (*env)->ReleaseStringUTFChars(env, key, ky);
  Ns_DStringFree(&argv);

  return (*env)->NewStringUTF(env, interp->result);
} 

