/*
Write out an empty jar file that has a Manifest, tests for bug in Kaffe
*/

import java.io.*;
import java.util.zip.*;
import java.util.jar.*;

public class EmptyJar {

    public static void main(String[] argv) throws Exception {
	String fname = "zip1.zip";

	if ((new File(fname)).exists()) {
	    fname = "zip2.zip";
	}
	
	FileOutputStream fos = new FileOutputStream(fname);

	JarOutputStream zout = new JarOutputStream(fos, new Manifest());

	zout.close();

	File f = new File(fname);
	System.out.println("Wrote \"" + fname + "\"," +
			   " file size = " + f.length() );
    }
}




/*


Index: ZipOutputStream.java
===================================================================
RCS file: /home/cvspublic/kaffe/libraries/javalib/java/util/zip/ZipOutputStream.java,v
retrieving revision 1.3
diff -u -r1.3 ZipOutputStream.java
--- ZipOutputStream.java        1999/02/13 09:02:58     1.3
+++ ZipOutputStream.java        1999/02/16 07:19:23
@@ -218,8 +218,8 @@
        put16(lh, LOC_METHOD, ze.method);
        put16(lh, LOC_TIME, 0);
        put16(lh, LOC_DATE, 0);
-       put32(lh, LOC_CRC, (int)ze.crc);
 
+       put32(lh, LOC_CRC, (ze.crc == -1) ? 0 : (int)ze.crc);
        put32(lh, LOC_COMPRESSEDSIZE, ze.csize == -1 ? 0 : (int)ze.csize);
        put32(lh, LOC_UNCOMPRESSEDSIZE, ze.size == -1 ? 0 : (int)ze.size);

*/
