// Test Kaffe Zip impl to make sure error is raised when
// no ZipEntry objects are added to a ZipOutputStream

import java.io.*;
import java.util.zip.*;

public class EmptyZip {
    public static void main(String[] argv) throws Exception {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	ZipOutputStream zout = new ZipOutputStream(baos);
	zout.close();
    }
}


/*
JDK 1.2

% java EmptyZip
Exception in thread "main" java.util.zip.ZipException: ZIP file must have at least one entry
        at java.util.zip.ZipOutputStream.finish(ZipOutputStream.java:291)
        at java.util.zip.ZipOutputStream.close(ZipOutputStream.java:309)
        at EmptyZip.main(EmptyZip.java:11)

*/



/*
Kaffe

% kaffe EmptyZip (no error generated)


*/


/*
Kaffe with my patch added

% kaffe EmptyZip
java.util.zip.ZipException: ZIP file must have at least one entry
        at java.lang.Throwable.<init>(Throwable.java:37)
        at java.lang.Exception.<init>(Exception.java:21)
        at java.io.IOException.<init>(IOException.java:22)
        at java.util.zip.ZipException.<init>(ZipException.java:24)
        at java.util.zip.ZipOutputStream.finish(ZipOutputStream.java:178)
        at java.util.zip.ZipOutputStream.close(ZipOutputStream.java:99)
        at EmptyZip.main(EmptyZip.java:11)
*/




/*

The Patch.


Index: ZipOutputStream.java
===================================================================
RCS file: /home/cvspublic/kaffe/libraries/javalib/java/util/zip/ZipOutputStream.java,v
retrieving revision 1.3
diff -u -r1.3 ZipOutputStream.java
--- ZipOutputStream.java	1999/02/13 09:02:58	1.3
+++ ZipOutputStream.java	1999/02/16 05:48:49
@@ -172,6 +172,12 @@
 		size += CEN_RECSZ + ze.name.length();
 	}
 
+	// Flag error if no entries were written.
+
+	if (count == 0) {
+	        throw new ZipException("ZIP file must have at least one entry");
+	}
+
 	put32(ce, END_SIGNATURE, (int)END_ENDSIG);
 	put16(ce, END_DISKNUMBER, 0);
 	put16(ce, END_CENDISKNUMBER, 0);



*/
