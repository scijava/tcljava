import java.io.*;
import java.util.zip.*;

public class InflaterTest {

    public static void main(String[] argv) {
	
        InflaterInputStream iis;
	ByteArrayInputStream bais = new ByteArrayInputStream(new byte[100]);
	Inflater inf = new Inflater();

	try {
	    iis = new InflaterInputStream(null);
	} catch (NullPointerException e) {
	    System.out.println("1 OK");
	}

	try {
	    iis = new InflaterInputStream(bais, null);
	} catch (NullPointerException e) {
	    System.out.println("2 OK");
	}
	
       	try {
	    iis = new InflaterInputStream(bais, inf, 0);
	} catch (IllegalArgumentException e) {
	    System.out.println("3 OK");
	}

    }

}



/*
JDK

% java InflaterTest
1 OK
2 OK
3 OK
*/


/*
Kaffe

% kaffe InflaterTest

*/


/*
Kaffe with my patch

% kaffe InflaterTest
1 OK
2 OK
3 OK

*/
