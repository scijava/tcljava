import java.io.*;
import java.util.zip.*;

public class DeflaterTest {

    public static void main(String[] argv) {
	
	DeflaterOutputStream dos;
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	Deflater def = new Deflater();

	try {
	    dos = new DeflaterOutputStream(null);
	} catch (NullPointerException e) {
	    System.out.println("1 OK");
	}

	try {
	    dos = new DeflaterOutputStream(baos, null);
	} catch (NullPointerException e) {
	    System.out.println("2 OK");
	}
	
       	try {
	    dos = new DeflaterOutputStream(baos, def, 0);
	} catch (IllegalArgumentException e) {
	    System.out.println("3 OK");
	}

    }

}



/*
JDK

% java DeflaterTest
1 OK
2 OK
3 OK
*/


/*
Kaffe

% kaffe DeflaterTest

*/


/*
Kaffe with my patch

% kaffe DeflaterTest
1 OK
2 OK
3 OK

*/




/*

Index: DeflaterOutputStream.java
===================================================================
RCS file: /home/cvspublic/kaffe/libraries/javalib/java/util/zip/DeflaterOutputStream.java,v
retrieving revision 1.2
diff -u -r1.2 DeflaterOutputStream.java
--- DeflaterOutputStream.java   1998/09/30 23:20:20     1.2
+++ DeflaterOutputStream.java   1999/02/18 06:18:20
@@ -30,7 +30,13 @@
 
 public DeflaterOutputStream(OutputStream out, Deflater defx, int size) {
        super(out);
+       if (out == null)
+           throw new NullPointerException("out");
+       if (defx == null)
+           throw new NullPointerException("def");
        def = defx;
+       if (size < 1)
+           throw new IllegalArgumentException("size < 1");
        buf = new byte[size];
 }

*/
