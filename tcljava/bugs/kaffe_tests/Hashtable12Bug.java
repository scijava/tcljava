import java.util.*;

public class Hashtable12Bug {
    public static void main(String[] argv) throws Exception {
	Hashtable h = new Hashtable();

	if (h.hashCode() == 0) {
	    throw new RuntimeException("h.hashCode() == 0");
	}

	System.out.println("OK");
    }
}


/*
JDK 1.1
OK

*/


/*
JDK 1.2
Exception in thread "main" java.lang.RuntimeException: h.hashCode() == 0
        at Hashtable12Bug.main(Compiled Code)

*/
