/*
 * ExecCmd.java --
 *
 *	This file contains the Jacl implementation of the built-in Tcl "exec"
 *	command. The exec command is not available on the Mac.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ExecCmd.java,v 1.8 2002/01/19 00:15:01 mdejong Exp $
 */

package tcl.lang;

import java.util.*;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;


/*
 * This class implements the built-in "exec" command in Tcl.
 */

class ExecCmd implements Command {

/**
 * Reference to Runtime.exec, null when JDK < 1.3
 */
private static Method execMethod;

static {
    // Runtime.exec(String[] cmdArr, String[] envArr, File currDir)
    Class[] parameterTypes = {String[].class, String[].class, File.class };
    try {
        execMethod = Runtime.getRuntime().getClass().getMethod("exec",
            parameterTypes);
    } catch (NoSuchMethodException e) {
        execMethod = null;
    }
}


/*
 *----------------------------------------------------------------------
 *
 * CmdProc --
 *
 *	This procedure is invoked to process the "exec" Tcl command.
 *	See the user documentation for details on what it does.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	See the user documentation.
 *
 *----------------------------------------------------------------------
 */

public void
  cmdProc(
    Interp interp, 	// The current interpreter.
    TclObject argv[])	// The arguments to exec.
throws 
    TclException 	// A standard Tcl exception.
{
    int      firstWord;             /* Index to the first non-switch arg */
    int      argLen = argv.length;  /* No of args to copy to argStrs     */
    int      exit;                  /* denotes exit status of process    */
    int      errorBytes;            /* number of bytes of process stderr */
    boolean  background  = false;   /* Indicates a bg process            */
    boolean  keepNewline = false;   /* Retains newline in pipline output */
    Process  p;                     /* The exec-ed process               */
    String   argStr;                /* Conversion of argv to a string    */
    StringBuffer sbuf;

    /*
     * Check for a leading "-keepnewline" argument.
     */

    for (firstWord = 1; firstWord < argLen; firstWord++) {
	argStr = argv[firstWord].toString();
	if ((argStr.length() > 0) && (argStr.charAt(0) == '-')) {
	    if (argStr.equals("-keepnewline")) {
		keepNewline = true;
	    } else if (argStr.equals("--")) {
		firstWord++;
		break;
	    } else {
		throw new TclException(interp, "bad switch \"" +
			argStr + "\": must be -keepnewline or --");
	    }
	} else {
	    break;
	}
    }

    if (argLen <= firstWord) {
	    throw new TclNumArgsException(interp, 1, argv,
		"?switches? arg ?arg ...?");
    }


    /*
     * See if the command is to be run in background.
     * Currently this does nothing, it is just for compatibility
     */

    if (argv[argLen-1].toString().equals("&")) {
	argLen--;
	background = true;
    }

    try {
	/*
	 * It is necessary to perform system specific 
	 * operations before calling exec.  For now Solaris
	 * and Windows execs are somewhat supported, in all other cases
	 * we simply call exec and give it our "best shot"
	 */

        if (execMethod != null) {
	    p = execReflection(interp, argv, firstWord, argLen);
        } else if (Util.isUnix()) {
	    p = execUnix(interp, argv, firstWord, argLen);
	} else if (Util.isWindows()) {
	    p = execWin(interp, argv, firstWord, argLen);
	} else {
	    p = execDefault(interp, argv, firstWord, argLen);
	}


	//note to self : buffer reading should be done in
	//a separate thread and not by calling waitFor()
	//because a process that is waited for can block

	
	//Wait for the process to finish running,
	exit = p.waitFor();


	//Make buffer for the results of the subprocess execution
	sbuf = new StringBuffer();

	//read data on stdout stream into  result buffer
	readStreamIntoBuffer(p.getInputStream(),sbuf);


	//if there is data on the stderr stream then append
	//this data onto the result StringBuffer
	//check for the special case where there is no error
	//data but the process returns an error result

	errorBytes = readStreamIntoBuffer(p.getErrorStream(),sbuf);

	if ((errorBytes == 0) && (exit != 0)) {
	  sbuf.append("child process exited abnormally");
	}

	//If the last character of the result buffer is a newline, then 
	//remove the newline character (the newline would just confuse 
	//things).  Finally, we set pass the result to the interpreter.
	
	int length = sbuf.length();
	if (!keepNewline && (length > 0) &&
		(sbuf.charAt(length - 1) == '\n')) {
	    sbuf.setLength(length - 1);
	}

	// Tcl supports lots of child status conditions.
	// Unfortunately, we can only find the child's
	// exit status using the Java API
        
	if (exit != 0) {
	    TclObject childstatus = TclList.newInstance();
	    TclList.append(interp, childstatus,
	        TclString.newInstance("CHILDSTATUS"));

            // We don't know how to find the child's pid
	    TclList.append(interp, childstatus,
	        TclString.newInstance("?PID?"));

	    TclList.append(interp, childstatus,
	        TclInteger.newInstance(exit));

            interp.setErrorCode( childstatus );
	}

	//when the subprocess writes to its stderr stream or returns
	//a non zero result we generate an error
	if ((exit != 0) || (errorBytes != 0)) {
	  throw new TclException(interp, sbuf.toString());
	}
	
	//otherwise things went well so set the result
	interp.setResult(sbuf.toString());
	
    } catch (IOException e) {
        //if exec fails we end up catching the exception here

	throw new TclException(interp, "couldn't execute \"" +
		argv[firstWord].toString() + "\": no such file or directory");

    } catch (InterruptedException e) {
	/*
	 * Do Nothing...
	 */
    }
}



/*
 *----------------------------------------------------------------------
 *
 * readStreamIntoBuffer --
 *
 *	This utility function will read the contents of an InputStream
 *	into a StringBuffer. When done it returns the number of bytes
 *	read from the InputStream. The assumption is an unbuffered stream
 *
 * Results:
 *	Returns the number of bytes read from the stream to the buffer
 *
 * Side effects:
 *	Data is read from the InputStream.
 *
 *----------------------------------------------------------------------
 */

static int readStreamIntoBuffer(InputStream in, StringBuffer sbuf) {
    int numRead = 0;
    BufferedReader br = new BufferedReader(new InputStreamReader(in));

    try {    
      String line = br.readLine();

      while (line != null) {
        sbuf.append(line);
        numRead += line.length();
        sbuf.append('\n');
        numRead++;
        line = br.readLine();
      }
    } catch (IOException e) {
      //do nothing just return numRead
    } finally {
      try {br.close();} catch (IOException e) {} //ignore IO error
    }

    return numRead;
}



/*
 *----------------------------------------------------------------------
 *
 * escapeWinString --
 *
 *	This utility function takes a String and returns the same String
 *      with any % replaced by %%. This is needed so that environemental
 *      variable substitution is not done when a windows program is called
 *      with the exec command that uses .bat files
 *
 * Results:
 *	Returns the same String is no % is in the string, otherwise the
 *      String is returned with %% for every %.
 *
 * Side effects:
 *	none.
 *
 *----------------------------------------------------------------------
 */

static String escapeWinString(String str) {
    if (str.indexOf('%') == -1)
      return str;

    char[] arr = str.toCharArray();
    StringBuffer sb = new StringBuffer(50);

    for (int i=0; i < arr.length; i++) {
      if (arr[i] == '%') {
        sb.append('%');
      }
      sb.append(arr[i]);
    }

    return sb.toString();
}



/*
 *----------------------------------------------------------------------
 *
 * execUnix --
 *
 *	This procedure is invoked to process system specific "exec" calls for
 *	Unix.  Since new Process dosent know the current tclsh directory, we
 *	exec the command in a Unix shell, but we force the Unix shell to cd to
 *	the current dir first. 
 *
 * Results:
 *	Returns the Process object returned from the exec call.
 *
 * Side effects:
 *	See the user documentation.
 *
 *----------------------------------------------------------------------
 */

private Process 
execUnix (Interp interp, TclObject argv[], int first, int last) 
        throws IOException {
    String[] argStrs = new String[3];

    argStrs[0] = "sh";
    argStrs[1] = "-c";

    StringBuffer sbuf = new StringBuffer();

    sbuf.append("cd \'");
    sbuf.append(interp.getWorkingDir().toString());
    sbuf.append("\'; ");
    
    for (int i = first; i < last; i++) {
      sbuf.append('\'');
      sbuf.append(argv[i].toString());
      sbuf.append('\'');
      sbuf.append(' ');
    }

    //trim off the last space char
    sbuf.setLength(sbuf.length() - 1);

    argStrs[2] = sbuf.toString();

    //System.out.println("now to unix exec \"" + argStrs[2] + "\"");

    return Runtime.getRuntime().exec(argStrs);
}


/*
 *----------------------------------------------------------------------
 *
 * execWin --
 *
 *	This procedure is invoked to process system specific "exec" calls for
 *	Windows.  Since new Process dosent know the current tclsh directory, a
 *	file has to be created that cds to the current interp dir, and executes
 *	the command there. 
 *
 * Results:
 *	Returns the new process.
 *
 * Side effects:
 *	See the user documentation.
 *
 *----------------------------------------------------------------------
 */

private Process 
 execWin (Interp interp, TclObject argv[], int first, int last) 
    throws IOException {

    // when running on NT we need to write out two files
    // the first is in C:/TEMP/jacl1.bat and the second is C:/TEMP/jacl2.bat
    // we exec command.com on jacl1.bat which will invoke cmd.exe on jacl2.bat

    File tmp = new File("C:\\TEMP");
    if (! tmp.exists()) {
	if (! tmp.mkdirs()) {
	    throw new IOException("could not create C:\\TEMP");
	}
    }
    
    // if we are not running under NT then we just write out the jacl1.bat file
    // and invoke that with command.com which should work on 95

    String jacl1 = "C:\\TEMP\\jacl1.bat";
    String jacl2 = "C:\\TEMP\\jacl2.bat";
    
    boolean isNT = System.getProperty("os.name").toLowerCase().equals("windows nt") ||
                   System.getProperty("os.name").toLowerCase().equals("windows 2000");
    
    File jacl1_file = new File(jacl1);
    File jacl2_file = new File(jacl2);


    // if we are running the NT version then we need to write out to jacl2 but
    // if not then we write out to jacl, we also need to check to make sure jacl
    // exists before each exec on NT because it might get removed by the user 

    File out_file;

    if (isNT) {

	if ( jacl1_file.exists() ) {
	    jacl1_file.delete();
	}
	
	PrintWriter jacl_out = new PrintWriter(new BufferedWriter(
					       new FileWriter( jacl1_file )));
	
	jacl_out.println("@echo off");
	jacl_out.println("cmd.exe /C " + jacl2);
	jacl_out.close();
	
	out_file = jacl2_file;
    } else {
	out_file = jacl1_file;
    }


    if ( out_file.exists() ) {
	out_file.delete();
    }

    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter( out_file )));


    // now we write out the .BAT script to the current output file and run it

    // remove .bat file cmd echoing
    out.println("@echo off");

    // from the path we get the "DRIVE" and the current directory
    String path = interp.getWorkingDir().toString();
    
    // write out the drive id
    out.println( path.substring(0,2) );

    // write out the path without double quotes but with % subst
    out.println( "cd " + escapeWinString(path.substring(2)) );


    // we must take special care not to quote the program name
    // we must also take care to ensure that each "%" becomes a "%%"
    out.print( escapeWinString(argv[first].toString()) );
    out.print(' ');


    for (int i = (first + 1); i < last; i++) {
      out.print('"');
      out.print( escapeWinString(argv[i].toString()) );
      out.print('"');
      out.print(' ');
    }

    out.println();
    out.close();

    String[] argStrs = {"command.com", "/C", jacl1};

    //System.out.println("now to win exec " + argv[first]);

    return Runtime.getRuntime().exec(argStrs);
}


/*
 *----------------------------------------------------------------------
 *
 * execDefault --
 *
 *	This procedure is invoked to process the "exec" call for an unknown
 *	a system. This happens when we do not have special exec code for a
 *      system that the code is running under. It should never get used as
 *      unix and windows are supported and Mac has no exec.
 *
 * Results:
 *	Returns the new process.
 *
 * Side effects:
 *	See the user documentation.
 *
 *----------------------------------------------------------------------
 */

private Process 
execDefault (Interp interp, TclObject argv[], int first, int last) 
        throws IOException {

    //we can not do anything to change the current directory in Java


    String[] strv = new String[last - first];

    //System.out.println("def alloc space for " + (last - first) + " Strings");

    for (int i=first, j=0; i < last; j++, i++) {
        strv[j] = argv[i].toString();
	//System.out.println("default exec arg = ->" + strv[j] + "<-");
    }

    return Runtime.getRuntime().exec(strv);
}


/*
 *----------------------------------------------------------------------
 *
 * execReflection --
 *
 *	This procedure is invoked to process the "exec" call using the
 *	new Runtime.exec( String[] cmdArr, String[] envArr, File currDir )
 *      call in JDK1.3. The method invocation is done using Reflection to
 *      allow for JDK1.1/1.2 compatibility.
 *
 * Results:
 *	Returns the new process.
 *
 * Side effects:
 *	See the user documentation.
 *
 *----------------------------------------------------------------------
 */

private Process 
execReflection (Interp interp, TclObject argv[], int first, int last) 
        throws IOException {

    String[] strv = new String[last - first];

    for (int i=first, j=0; i < last; j++, i++) {
        strv[j] = argv[i].toString();
    }

    Object[] methodArgs = new Object[3];
    methodArgs[0] = strv; // exec command arguments
    methodArgs[1] = null;  // inherit all environment variables
    methodArgs[2] = interp.getWorkingDir();

    try
    {
        return (Process)
            execMethod.invoke(Runtime.getRuntime(), methodArgs);
    } catch (IllegalAccessException ex) {
        throw new TclRuntimeError("IllegalAccessException in execReflection");
    } catch (IllegalArgumentException ex) {
        throw new TclRuntimeError("IllegalArgumentException in execReflection");
    } catch (InvocationTargetException ex) {
        Throwable t = ex.getTargetException();

        if (t instanceof Error) {
            throw (Error) t;
        } else if (t instanceof IOException) {
            throw (IOException) t;
        } else {
           throw new TclRuntimeError("unexected exception in execReflection"); 
        }
    }
}

} // end ExecCmd
