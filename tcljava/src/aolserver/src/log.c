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

/*
 *----------------------------------------------------------------------
 *
 * Java_NsLog_write --
 *
 *	JNI method for writing aolserver log entries from within java..
 *
 * Results:
 *	Message is written to the log file using the module log format.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static void handleErr(JNIEnv *env, Ns_DString *msg);

JNIEXPORT void JNICALL Java_nsjava_NsLog_write
(JNIEnv *env, jobject jvm, jstring svrty, jstring msg) 
{
  Ns_LogSeverity  log_severity;
  Ns_DString      errmsg;
  char           *severity;
  char           *message;

  Ns_DStringInit(&errmsg);
  severity = (char*)((*env)->GetStringUTFChars(env, svrty, JNI_FALSE));
  if(errCheck(severity == NULL ? 1 : 0, &errmsg, "couldn't get input arg.")) {
    handleErr(env, &errmsg);   
    return;
  }

  message  = (char*)((*env)->GetStringUTFChars(env, msg, JNI_FALSE));
  if(errCheck(message == NULL ? 1 : 0, &errmsg, "couldn't get input arg.")) {
    handleErr(env, &errmsg);   
    return;
  }

  log_severity = NsJava_ConvertSeverity(severity);
  Ns_ModLog(log_severity, javaModLogHandle, message);

  (*env)->ReleaseStringUTFChars(env, svrty, severity);
  (*env)->ReleaseStringUTFChars(env, msg, message);
  Ns_DStringFree(&errmsg);
}

static void handleErr(JNIEnv *env, Ns_DString *msg) 
{
  checkForException(env);
  throwByName(env, "java/lang/RuntimeException", msg->string, NULL);
  Ns_ModLog(Error, javaModLogHandle, msg->string);
  Ns_DStringFree(msg);  
}

//static int severityRank[Dev + 1] = { 
//   4, /* Notice    - enum value = 0 */
//   3, /* Warning   - enum value = 1 */
//   2, /* Error     - enum value = 2 */
//   0, /* Fatal     - enum value = 3 */
//   1, /* Bug       - enum value = 4 */
//   5, /* Debug     - enum value = 5 */
//   6  /* Dev       -      value = 6 - for developers - defed in nsd/nsd.h */
//};


Ns_LogSeverity NsJava_ConvertSeverity(char *severity) 
{
  Ns_LogSeverity  log_severity;

  if (STRIEQ("Notice", severity)) {
    log_severity = Notice;
  } 
  else if (STRIEQ("Warning", severity)) {
    log_severity = Warning;
  }
  else if (STRIEQ("Error", severity)) {
    log_severity = Error;
  }
  else if (STRIEQ("Fatal", severity)) {
    log_severity = Fatal;
  }
  else if (STRIEQ("Bug", severity)) {
    log_severity = Bug;
  }
  else if (STRIEQ("Debug", severity)) {
    log_severity = Debug;
  }
  else {
    log_severity = Bug;
  }

  return log_severity;
}


/*
 * aolserver versions greater than 3.0 don't support the modlog capability.
 */

#ifdef NSVERSION_3_0_PLUS
extern void ns_serverLog(Ns_LogSeverity severity, char *fmt, va_list *vaPtr);

void
Ns_ModLog(Ns_LogSeverity severity, Ns_ModLogHandle handle, char *fmt, ...)
{
    va_list ap;

    va_start(ap, fmt);
    ns_serverLog(severity, fmt, &ap);
    va_end(ap);

}

void 
Ns_ModLogRegister(char *realm, Ns_ModLogHandle *handle)
{

}

void
Ns_ModLogSetThreshold(Ns_ModLogHandle handle, Ns_LogSeverity severity)
{
}

#endif
