import java.lang.reflect.*;
import java.util.*;

public class Hashtable12 {
    public static void main(String[] argv) throws Exception {
	Hashtable h = new Hashtable();

	/*
	Object h1 = new Object();
	Object h2 = new Object();
	Object h3 = new Object();
	*/

	Object h1 = new Hashtable();
	Object h2 = new Hashtable();
	Object h3 = new Hashtable();


	System.out.println("h1 hashCode() is " + h1.hashCode());
	System.out.println("h2 hashCode() is " + h2.hashCode());
	System.out.println("h3 hashCode() is " + h3.hashCode());

	if ((h1 == h2) || (h2 == h3) || (h3 == h1)) {
	    throw new RuntimeException("hashes are equal");
	}


	// hash em

	h.put("h1", h1);
	h.put("h2", h2);
	h.put("h3", h3);


	// get em

	Object tmp;

	tmp = (Object) h.get("h1");
	if (tmp == null || tmp != h1) {
	    throw new RuntimeException("h1 hashed to " + h1);
	}

	tmp = (Object) h.get("h2");
	if (tmp == null || tmp != h2) {
	    throw new RuntimeException("h2 hashed to " + h2);
	}

	tmp = (Object) h.get("h3");
	if (tmp == null || tmp != h3) {
	    throw new RuntimeException("h3 hashed to " + h3);
	}	
    }
}


/*
JDK output

*/


/*
Kaffe output

*/

