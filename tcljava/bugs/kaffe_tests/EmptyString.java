public class EmptyString {

    public static void main(String[] argv) throws Exception {
	String s = "\0";
	char c = s.charAt(0);

	if (c != '\0') {
	    throw new Exception("bad char (" + ((int) c) + ")");
	} else {
	    System.out.println("OK");
	}
    }

}




/*
Kaffe output when compiled with JDK's javac

% kaffe EmptyString
OK

*/


/*
Kaffe output when compiled kaffe's javac (Pizza)

% kaffe EmptyString
java.lang.StringIndexOutOfBoundsException
        at java/lang/Throwable.<init>(31)
        at java/lang/Exception.<init>(17)
        at java/lang/RuntimeException.<init>(17)
        at java/lang/IndexOutOfBoundsException.<init>(17)
        at java/lang/StringIndexOutOfBoundsException.<init>(17)
        at java/lang/String.charAt(110)
        at EmptyString.main(5)
*/



/*
JDK output when compiled with JDK's javac

% java EmptyString
OK

*/


/*
JDK output when compiled with Kaffe's javac

% java EmptyString
java.lang.StringIndexOutOfBoundsException: String index out of range: 0
        at java.lang.String.charAt(String.java)
        at EmptyString.main(EmptyString.java:5)

*/



/*
JDK output when compiled with Pizza under the JDK

% java EmptyString
java.lang.StringIndexOutOfBoundsException: String index out of range: 0
        at java.lang.String.charAt(String.java)
        at EmptyString.main(EmptyString.java:5)

*/
