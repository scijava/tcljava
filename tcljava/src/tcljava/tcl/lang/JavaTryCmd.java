/*
 * JavaTryCmd.java --
 *
 *	Implements the built-in "java::try" command.
 *
 * Copyright (c) 1998 by Moses DeJong
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: JavaTryCmd.java,v 1.2 1999/08/03 03:25:42 mo Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "java::try" command. The command
 * provides access to the java's try-catch-finally construct.
 */

public class JavaTryCmd implements Command
{

/**

 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *      This procedure is invoked as part of the Command interface to
 *      process the "java::try" Tcl command.  See the user documentation
 *      for details on what it does.
 *
 * Results:
 *      None.
 *
 * Side effects:
 *      See the user documentation.
 *
 *----------------------------------------------------------------------
 */

    public void cmdProc(Interp interp, TclObject[] argv)
	throws TclException {

	// (argv.length % 3) == 1 is a try+catch+finally block
	// (argv.length % 3) == 2 is a try+catch block

	int argv_length_mod = argv.length % 3;
	boolean try_catch_finally = false;
	boolean try_catch = false;

	if (argv_length_mod == 1) {
	    try_catch_finally = true;
	} else if (argv_length_mod == 2) {
	    try_catch = true;
	}

	if (argv.length < 4 || (!try_catch && !try_catch_finally)) {
	    throw new TclNumArgsException(interp,1,argv,
		      "script ?catch exception_pair script? ?finally script?");
	}



	// Eval the first arguent to the procedure

	Throwable t = null;
	boolean reflect_exception = false;

	try {
            interp.eval(argv[1],0);
	} catch (ReflectException e) {
	    if (debug) {
	        System.out.println("catching ReflectException");
	    }

	    reflect_exception = true;
	} catch (TclException e) {

	    if (debug) {
	        System.out.println("catching TclException");
	    }

	    t = e;

            // Figure out if the exception was really an error.
            // It might have been caused by a break, continue, or return.

            int code = e.getCompletionCode();

	    if (code == TCL.RETURN) {
	        // If the caught exception is a regular return
	        // then no error really occured and we ignore it
	        t = null;
	    }

            // If the code was TCL.ERROR, TCL.BREAK, or TCL.CONTINUE
            // then we just process it like and other exception

	}



        if (t != null || reflect_exception == true) {
            // grab the value of the errorCode variable

	    TclObject errorCode = interp.getVar("errorCode", null,
						TCL.GLOBAL_ONLY);
	    if (errorCode == null) {
	        throw new TclException(interp, "null errorCode");
            }

            if (debug) {
	        System.out.println("errorCode is \""
                           + errorCode.toString() + "\"");
	    }

	    TclObject desc = TclList.index(interp, errorCode, 0);
	    if (desc == null) {
	        throw new TclException(interp, "null errorCode index 0");
	    }

            if (debug) {
	        System.out.println("errorCode index 0 is \""
                           + desc.toString() + "\"");
	    }

	    if (reflect_exception) {
                // if we caught a ReflectException we must make
                // sure that the errorCode type is JAVA

 	        if (! desc.toString().equals("JAVA")) {
	            throw new TclException(interp, "errorCode index 0 was \"" +
	            desc.toString() + "\", expected \"JAVA\"");
 	        }
	    } else {
                // if we caught a TclException the error type might
                // be anything. If it is a JAVA error we process it

 	        if (desc.toString().equals("JAVA")) {
                    reflect_exception = true;
 	        }
            }

	    if (reflect_exception) {
	        // in this case a Java exception has been wrapped
	        // inside of a Tcl Exception. Grab the actual Java
                // exception out of the errorCode variable
	        // global var errorCode so that we can catch it here

	        if (debug) {
	        System.out.println("unwrapping Java Exception in TclException");
	        }

	        try {

		TclObject java_exception = TclList.index(interp, errorCode, 1);
		
		if (java_exception == null) {
		    throw new TclException(interp, "null errorCode index 1");
		}

		Object obj = ReflectObject.get(interp, java_exception);

		if (obj == null) {
		    throw new TclException(interp,
                      "null ReflectObject at errorCode index 1");
		}

		if (! (obj instanceof Throwable)) {
		    throw new TclException(interp, 
		      "bad errorCode index 1, not instance of Throwable");
		}

		t = (Throwable) obj;

	        } catch (TclException e2) {
                throw new TclException(interp, "unexpected TclException " + e2);
                }
	    }

        }






	// now if there was an error we need to go through the
	// catch clauses and check the error, we also need to
	// make sure that and code we evaluate in the catch blocks
	// does not raise and error that we do not handle because
	// then our finally block would not get run. This of
	// course does not include any problems that might come up
	// if they did not call the method with the correct arguments
	// if that happened an error will be raised while processing
	// the catch clauses or when the finally clause is processed

	if (t != null) {

	    if (debug) {
	    System.out.println("Exception raised inside body block, type = " +
                t.getClass().getName() + ", msg = \"" + t.getMessage() + "\"");
	    }

	    int end_loop = argv.length;

	    // If there is a finally block then do not check it
	    // in ths catch block processing loop
	    if (try_catch_finally) {
		end_loop -= 2;
	    }

	    for (int i=2; i < end_loop; i+=3) {
		TclObject catch_clause = argv[i];
		TclObject exception_pair = argv[i+1];
		TclObject exception_script = argv[i+2];

		if (! catch_clause.toString().equals("catch")) {
		    throw new TclException(interp,"invalid catch clause \"" +
					   catch_clause.toString() + "\"");
		}

		boolean exception_pair_problem = false;
		TclObject type = null;
		TclObject var = null;

		try {
		    if (TclList.getLength(interp,exception_pair) != 2) {
			exception_pair_problem = true;
		    } else {
			type =  TclList.index(interp,exception_pair,0);
			var  =  TclList.index(interp,exception_pair,1);

			// double check to make sure that these TclObjects
			// are not lists (they can only have one element)

			if (TclList.getLength(interp,type) != 1 ||
			    TclList.getLength(interp,var) != 1) {
			    exception_pair_problem = true;
			}
		    }
		} catch (TclException e) {
		    exception_pair_problem = true;
		}
		

		// if we could not parse the exception pair into a two
		// element list then we raise an error

		if (exception_pair_problem) {
		    throw new TclException(interp,"invalid exception_pair \"" +
					   exception_pair.toString() + "\"");
		}


	        if (debug) {
		System.out.println("type is \"" + type.toString() + "\"");
		System.out.println("var is \"" + var.toString() + "\"");
		System.out.println("script is \"" + exception_script.toString() + "\"");
	        }


		// now we need to "match" the name of the exception
		// to the name of the "type" of the exception_pair
		// we first try to match the long form of the name
		// (ie "java.lang.Exception" matches "java.lang.Exception")
		// but if that does not work then we try the short form
		// (ie "java.lang.Exception" matches "Exception")
		// We also wathc for the special case where the identifier
		// is "TclException". In this case we match any exception
		// generated by a Tcl command. Any exception derived
		// from TclException will also be matched by this case

		
		// this will hold the Class that was matched
		// after the search is done
		Class matched_class = null;

		String type_name = type.toString();
		int    type_len  = type_name.length();

	        if (debug) {
		System.out.println("type_name is \"" + type_name + "\"");
	        }

		Class ex_type = t.getClass();

		// this should never happen
		if (ex_type == null) {
		    throw new TclRuntimeError("null Exception class");
		}

		String ex_type_name;


		// check for special case where the Exception is derived
                // from TclException and the type_name is "TclException"

		if (type_name.equals("TclException") || 
		    (t instanceof TclException)) {

		    if (type_name.equals("TclException") &&
		        (t instanceof TclException)) {
		        matched_class = TclException.class;

	                if (debug) {
			System.out.println("match for \"" +
					   matched_class.getName() +
					   "\" == \"" + type_name + "\"");
	                }
		    } else {
		        // If the exception name is "TclException" but the
		        // exception is not derived from TclException or
		        // if the type is derived from TclException but
		        // the type name is not "TclException", then 
		        // just go onto the next catch block

	                if (debug) {
			System.out.println("skipping catch block because " +
			 "the of TclException mismatch");
			System.out.println("exception name  = \""
                            + type_name + "\"");
			System.out.println("instanceof TclException  = " +
		         (t instanceof TclException) );
	                }

		        continue; //for loop
		    }
		} else {
	
		  while (ex_type != null) {
		      ex_type_name = ex_type.getName();

	              if (debug) {
		      System.out.println("ex_type_name is \"" + ex_type_name + "\"");
	              }

		      if (ex_type_name.equals( type_name )) {
			  matched_class = ex_type; //full name match
		      } else {
			  // try to match based on the "short" name of the class
			  // so "Exception" would match "java.lang.Exception"
			  // Watch out!
			  // so "Exception" does not match "java.lang.Exception2"
		    
			  int last = ex_type_name.lastIndexOf('.');

			  /*
	                  if (debug) {
			  System.out.println("last is " + last);
			  System.out.println((last != -1) + " && " +
				((ex_type_name.length() - (last+1)) == type_len));

			  System.out.println("regionmatch is " +
			      ex_type_name.regionMatches(last+1,type_name,
							   0,type_len));
	                  }
			  */

			  if ((last != -1) &&
			      ((ex_type_name.length() - (last+1)) == type_len) &&
			      ex_type_name.regionMatches(last+1,type_name,0,type_len)) {

			      matched_class = ex_type;
			  }
		      }


		    if (matched_class != null) {
	                if (debug) {
			System.out.println("match for \"" +
					   matched_class.getName() +
					   "\" == \"" + type_name + "\"");
	                }
			break; // end this while loop when match is found
		    }



		      // java.lang.Throwable is the highest a catch can go
		      if (ex_type == Throwable.class) {
			  ex_type = null;
		      } else {
			  ex_type = ex_type.getSuperclass();
		      }

		  } // end while lop

                } // end else




		// if we found a match on this catch block then eval that code
		if (matched_class != null) {

		    // Set the value of the variable named in the exception_pair
		    // I think it would be best to just write over the value
		    // of the exception variable. We really do not want to
		    // quit if it already defined and addind a "scope" to
		    // an exception handler seems to go against Tcl constructs

		    // Case 1: The exception type matched is a TclException
		    // In this case the value of the exception variable is
		    // the error result of the command.

		    if (matched_class == TclException.class) {

	              if (debug) {
		      System.out.println("TclException result getting saved in exception varaible");
	              }

		      TclObject res = interp.getResult();
                      res.preserve();

		      try {
			interp.setVar(var.toString(), res, 0);
		      } catch (TclException e) {
			//this should never happen
			throw new TclRuntimeError(
			  "could not reflect or set exception variable");
		      }

                      res.release();

		    } else {

		    // Case 2: The exception type matched is a Java exception
		    // that is not a subclass of TclException. In this case
		    // the value of the exception variable is the exception
		    // object reflection handle (java0x1 or something).

	              if (debug) {
		      System.out.println("JavaException result getting saved in exception varaible");
	              }

		      try {
			interp.setVar(var.toString(),
			  ReflectObject.newInstance(interp,
			    matched_class,t)
                          ,0);
		      } catch (TclException e) {
			// this should never happen
			throw new TclRuntimeError(
			  "could not reflect or set exception variable");
		      }

		    }



		    // eval the exception handler, catching all errors that
		    // could be raised in the process because we also need
		    // to be able to run our finally handler

		    TclException t_after_catch = null;

		    try {
			interp.eval(exception_script,0);
		    } catch (TclException e) {
			// catch every possible error that could happen
			// and record the type of the error for later
			
	                if (debug) {
			System.out.println("Exception raised inside exception handler, type = " + e.getClass().getName() + ", msg = \"" + e.getMessage() + "\"");
	                }

			t_after_catch = e;
		    }

		    // set t (the main exception variable) to the result
		    // of an error in the catch script evaluation

		    t = t_after_catch;
			            
		    break; // break out of the enclosing for loop
		           // this will stop catch block processing
		}

	    } // end for loop

	} // end if block




	// check and run the finally block if there is one

	if (try_catch_finally) {
	    
	    TclObject finally_clause = argv[argv.length - 2];
	    TclObject finally_script = argv[argv.length - 1];

	    if (! finally_clause.toString().equals("finally")) {
		throw new TclException(interp,"invalid finally clause \"" +
				       finally_clause.toString() + "\"");
	    }


	    if (debug) {
	    System.out.println("finally script is \"" +
			       finally_script.toString() + "\"");
	    }


	    // the finally script might change the interpreter result
            // so we need to save the current result so that it can
	    // restored after running the finally script

	    TclObject res = interp.getResult();
	    res.preserve();

	    // evaluate the finally scipt and make sure that errors
	    // in the finally script are caught and then thrown at
	    // the very end of this method

	    boolean finally_catch = false;

	    try {
		interp.eval(finally_script,0);
	    } catch (TclException e) {
		// catch every possible error that could happen
		// and record the type of the error for later
		
		t = e;
	        finally_catch = true;
	    }

	    // restore the interpreter result as long as the
	    // finally script did not raise an error of its own
	    if (finally_catch == false) {
	        interp.setResult(res);
            }
	    res.release();
 
	}



	// if we were in the middle of managing an error then the
	// varliable t is non null and we need to raise that exception
	// if a catch block handled the exception then t should be null

	if (t != null) {
	    // now if there is still an exception that needs to keep
	    // going up the stack then we need to rewrap it so that
	    // it looks like a TclException to the Java exception system

	    // the only trick here is that if it was not a Java exception
	    // then we just need to rethrow the original TclException


	    if (debug) {
	    System.out.println("throwing end of try method exception, type = "
                + t.getClass().getName() + ", msg = \"" +
                  t.getMessage() + "\"");
	    }


	    // if it is a regular Tcl exception or a wrapped Java
	    // exception then just rethrow it
	    
	    if (t instanceof TclException) {
	        if (debug) {
		System.out.println("rethrowing TclException " + t);
	        }
		throw (TclException) t;
	    }


	    // if it is an actual Java Exception (ie it is derived from
	    // the java class Throwable) then we need to wrap it as a
	    // TclException and then rethrow it. This can happen
	    // if no exception routine handled a Java exception

	    if (debug) {
	    System.out.println("wrapping and rethrowing Exception " + t);
	    }

	    throw new ReflectException(interp,t);

	}

	return;
    }

    private static final boolean debug = false; // enables debug output

} // end of JavaTryCmd class
