/*

[Bug Jacl 1.1a1] NullPointerException in strange context

Name:  Harald
email:  kir@iitb.fhg.de
OperatingSystem:  Linux (libc)
Synopsis:  NullPointerException in strange context

ReproducibleScript:
I am sorry that this is a bit lengthy,
but it seems to be necessary to
demonstrate the bug.

Compile this class with jdk1.2beta4
and see how it produces the exception
mentioned below.


*/



/*****************************************************************/
import tcl.lang.*;
import java.util.*;
/*****************************************************************/

/**
  This class implements command `Hashtable' with syntax

    Hashtable <name> command

  It only demonstrates a NullPointerException which happens with
  jdk1.2beta4, but not with jdk1.1.6
*****/



public class ReflectBug implements Command {

  public void cmdProc(Interp ip, TclObject argv[])
    throws TclException
  {
    TclObject parentWrapper;

    /***** 
      There must be exactly three arguments: 
        Hashtable name value
      where value is a script
    *****/

    if( argv.length!=3 ) {
      throw new	TclNumArgsException(ip, 1, argv, "name value");
    }

    /*****
      There should be a parent table available in global variable
      CURRENT.
    *****/

    parentWrapper = ip.getVar("CURRENT", TCL.GLOBAL_ONLY);
    Hashtable parent = (Hashtable) ReflectObject.get(ip, parentWrapper);

    /*****
      If we get here, argv[2] must be an executable script. We prepare
      an empty Hashtable in Tcl-variable CURRENT and then run the
      script.
    *****/

    parentWrapper.preserve(); // Jacl uglyness

    // create a new Hashtable and store it in the parent we found in
    // $CURRENT. 
    Hashtable child = new Hashtable();
    check(child);
    parent.put(argv[1].toString(), child);
    check(child);

    //ReflectObject.dump(ip);

    // wrap the child into a TclObject and store it in tcl-variable CURRENT
    TclObject childWrapper 
      = ReflectObject.newInstance(ip, child.getClass(), child);

    check(child);
    ip.setVar("CURRENT", 
	      childWrapper, 
	      TCL.GLOBAL_ONLY);
    check(child);


    //ReflectObject.dump(ip);

    // Evaluate the script
    ip.eval(argv[2], 0);
    check(child);


    //ReflectObject.dump(ip);

    // clean up, i.e. reset CURRENT to the parent
    ip.setVar("CURRENT", parentWrapper, TCL.GLOBAL_ONLY);
    parentWrapper.release();

    check(child);
  }


  public static void
  main(String argv[])
    throws TclException
  {
    Interp interp = new Interp();
    
    Hashtable h = new Hashtable();
    check(h);

    TclObject wrapper = 
      ReflectObject.newInstance(interp, h.getClass(), h);
    
    interp.setVar("CURRENT", wrapper, 0);
    interp.createCommand("Hashtable", new ReflectBug());
    //ReflectObject.dump(ip);
    interp.eval("Hashtable aaa {\n"+
	    "  Hashtable bbb {\n"+
	    "    puts $CURRENT\n"+
	    "  }\n"+
	    "  puts $CURRENT\n"+
	    "}");
  }

  static void check(Object o) {
      /*
      if (o.hashCode() == 0) {
	  throw new RuntimeException("hash code is 0");
      }
      */
  }

}










/*


(1.1.6)
% java ReflectBug
object will be reflected as class java.util.Hashtable
new reflect object for class java.util.Hashtable
reflect object java0x1 of type java.util.Hashtable registered

object will be reflected as class java.util.Hashtable
new reflect object for class java.util.Hashtable
reflect object java0x2 of type java.util.Hashtable registered

object will be reflected as class java.util.Hashtable
new reflect object for class java.util.Hashtable
reflect object java0x3 of type java.util.Hashtable registered

java0x3
dispose called for reflect object java0x3
reflect object java0x3 is no longer being used
instance comamnd java0x3 was removed from the interp
removing single entry typetable containing java0x3

java0x2
dispose called for reflect object java0x2
reflect object java0x2 is no longer being used
instance comamnd java0x2 was removed from the interp
removing single entry typetable containing java0x2


(1.2)
java0x3
dispose called for reflect object java0x3
reflect object java0x3 is no longer being used
instance comamnd java0x3 was removed from the interp
removing single entry typetable containing java0x3
java0x2
dispose called for reflect object java0x2
reflect object java0x2 is no longer being used
instance comamnd java0x2 was removed from the interp
Exception in thread "main" java.lang.NullPointerException:

reflectObjectTable returned null for java0x2
        at tcl.lang.ReflectObject.disposeOfTableRefrences(ReflectObject.java:428)








removing single entry typetable containing java0x3
java0x2
dispose called for reflect object java0x2
reflect object java0x2 is no longer being used
ReflectObject instance java0x2 was disposed()
BEGIN DUMP -------------------------------
interp.reflectObjCount = 3
interp.reflectIDTable.size() = 1
interp.reflectObjTable.size() = 2

refID = "java0x1"
javaObj.hashCode() = "1123"
javaClass = "java.util.Hashtable"
useCount = "2"
isValid = "true"
typetable is null

Exception in thread "main" java.lang.NullPointerException: reflectObjectTable returned null for java0x2
        at tcl.lang.ReflectObject.disposeOfTableRefrences(ReflectObject.java:431)




























(1.1)



% java ReflectBug
object will be reflected as class java.util.Hashtable
new reflect object for class java.util.Hashtable
reflect object java0x1 of type java.util.Hashtable registered
BEGIN DUMP -------------------------------
interp.reflectObjCount = 1
interp.reflectIDTable.size() = 1
interp.reflectObjTable.size() = 1

refID = "java0x1"
javaObj.hashCode() = "499524354"
javaClass = "java.util.Hashtable"
useCount = "2"
isValid = "true"
typetable is a single entry of type "java.util.Hashtable"

object will be reflected as class java.util.Hashtable
new reflect object for class java.util.Hashtable
reflect object java0x2 of type java.util.Hashtable registered
object will be reflected as class java.util.Hashtable
new reflect object for class java.util.Hashtable
reflect object java0x3 of type java.util.Hashtable registered

java0x3
dispose called for reflect object java0x3
reflect object java0x3 is no longer being used
ReflectObject instance java0x3 was disposed()
removing single entry typetable containing java0x3

java0x2
dispose called for reflect object java0x2
reflect object java0x2 is no longer being used
ReflectObject instance java0x2 was disposed()
removing single entry typetable containing java0x2









(1.2)


object will be reflected as class java.util.Hashtable
new reflect object for class java.util.Hashtable
reflect object java0x1 of type java.util.Hashtable registered
BEGIN DUMP -------------------------------
interp.reflectObjCount = 1
interp.reflectIDTable.size() = 1
interp.reflectObjTable.size() = 1

refID = "java0x1"
javaObj.hashCode() = "0"
javaClass = "java.util.Hashtable"
useCount = "2"
isValid = "true"
typetable is a single entry of type "java.util.Hashtable"



object will be reflected as class java.util.Hashtable
new reflect object for class java.util.Hashtable
reflect object java0x2 of type java.util.Hashtable registered

object will be reflected as class java.util.Hashtable
new reflect object for class java.util.Hashtable
reflect object java0x3 of type java.util.Hashtable registered

java0x3
dispose called for reflect object java0x3
reflect object java0x3 is no longer being used
ReflectObject instance java0x3 was disposed()
removing single entry typetable containing java0x3

java0x2
dispose called for reflect object java0x2
reflect object java0x2 is no longer being used
ReflectObject instance java0x2 was disposed()
BEGIN DUMP -------------------------------
interp.reflectObjCount = 3
interp.reflectIDTable.size() = 1
interp.reflectObjTable.size() = 2

refID = "java0x1"
javaObj.hashCode() = "1123"
javaClass = "java.util.Hashtable"
useCount = "3"
isValid = "true"
typetable is null

Exception in thread "main" java.lang.NullPointerException: reflectObjectTable returned null for java0x2
        at tcl.lang.ReflectObject.disposeOfTableRefrences(ReflectObject.java:431)
        at tcl.lang.ReflectObject.dispose(ReflectObject.java:405)
        at tcl.lang.TclObject.release(TclObject.java:212)
        at tcl.lang.CallFrame.setVar(CallFrame.java:477)





*/
