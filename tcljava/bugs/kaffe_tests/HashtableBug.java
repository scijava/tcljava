import java.lang.reflect.*;
import java.util.*;


public class HashtableBug {
    public static void main(String[] argv) throws Exception {
	Class[] sig = {String.class};

	Hashtable h = new Hashtable();

	Method m1 =  HashtableBug.class.getMethod("m1", sig);
	Method m2 =  HashtableBug.class.getMethod("m2", sig);

	h.put(m1,"m1");
	h.put(m2,"m2");

	String m;

	m = (String) h.get(m1);
	System.out.println("m is " + m);

	m = (String) h.get(m2);
	System.out.println("m is " + m);
	
    }

    public void m1(String m) {}
    public void m2(String m) {}
}


/*
JDK output

% java HashtableBug
m is m1
m is m2

*/


/*
Kaffe output

% java HashtableBug
java.lang.NullPointerException
        at java/lang/reflect/Method.equals(41)
        at java/util/Hashtable.put(167)
        at HashtableBug.main(14)

*/


/*
Kaffe output with my patch applied

% java HashtableBug
m is m1
m is m2

*/



/*
The patch

*** cvs/kaffe/libraries/javalib/java/lang/reflect/Method.java   Wed Dec  9 17:20:20 1998
--- Method.java Thu Jan 14 04:15:53 1999
***************
*** 25,33 ****
  
  public boolean equals(Object obj)
        {
!       // Catch the simple case wher they're really the same
!       if ((Object)this == obj) {
                return (true);
        }
  
        Method mobj;
--- 25,37 ----
  
  public boolean equals(Object obj)
        {
!       // Catch the simple case where they're really the same
!       if (this == obj) {
                return (true);
+       }
+       //if obj is null then they are not the same
+       if (null == obj) {
+               return (false);
        }
  
        Method mobj;

*/
