import java.io.*;

public class CDTest {
    public static void main(String[] argv) {
	File pwd = new File("..");
	try {
	    pwd = new File(pwd.getCanonicalPath());
	} catch (IOException e) {
	    System.out.println("getCanonicalPath() raised " + e.getMessage());
	}

	System.out.println("PWD is \"" + pwd + "\"");
     }
}


/*
JDK output
PWD is "/tmp/mo/tcljava1.2.1"
*/


/*
Kaffe output
PWD is "/tmp/mo/tcljava1.2.1/bugs/.."
*/
