public class LoadBug {
    public static void main(String[] argv) throws Exception {
	try {
	    Class.forName("[[[LInteger;");
	} catch (ClassNotFoundException e) {
	    System.out.println("caught 1");
	}

	try {
	    Class.forName("[int");
	} catch (IllegalArgumentException e) { // JDK 1.1
	    System.out.println("caught 2");
	} catch (ClassNotFoundException e) { // JDK 1.2
	    System.out.println("caught 2");
	} 
    }
}


/*
JDK

caught 1
caught 2
*/


/*
Kaffe

% kaffe LoadBug
caught 1
java.lang.NullPointerException
        at LoadBug.main(LoadBug.java:10)

*/
