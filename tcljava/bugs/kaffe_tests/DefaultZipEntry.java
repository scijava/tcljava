import java.io.*;
import java.util.zip.*;

//import java.util.jar.*;

public class DefaultZipEntry {

    public static void main(String[] argv) throws Exception {
	File outfile = new File("tmp.jar");
	
	FileOutputStream fout = new FileOutputStream(outfile);

	ZipOutputStream out = new ZipOutputStream(fout);

	ZipEntry ze = new ZipEntry("temp");
	
	// comment out to get failure in kaffe
	//ze.setMethod(ZipEntry.DEFLATED);

	out.putNextEntry(ze);

	out.close();

	System.out.println("worked");
    }
}




/*

with ze.setMethod(ZipEntry.DEFLATED)



JDK 1.2

% java DefaultZipEntry
worked


Kaffe

% kaffe DefaultZipEntry
worked




with ze.setMethod((ZipEntry.DEFLATED) commented out


JDK 1.2

% java DefaultZipEntry
worked



Kaffe

% kaffe DefaultZipEntry
java.util.zip.ZipException: size not set in stored entry



So this makes me think that the default compression method
is DEFLATED in the JDK but it is STORED in Kaffe.





From the JDK docs for class ZipOutputStream.


public void setMethod(int method)

Sets the default compression method for subsequent entries.
This default will be used whenever the compression
method is not specified for an individual ZIP file entry,
and is initially set to DEFLATED.


Now looking at the implementation of Kaffes ZipOutputStream

(line 25)
private int method = STORED;



(line 202)
        if (ze.method == -1) {
		ze.method = method;
	}


So it looks like this default is incorrect.






Kaffe after changing the default method.

% kaffe DefaultZipEntry
worked


This also fixes Sun's jar program when running in default
(compressed) mode under Kaffe.



Index: ZipEntry.java
===================================================================
RCS file: /home/cvspublic/kaffe/libraries/javalib/java/util/zip/ZipOutputStream.java,v
retrieving revision 1.2
diff -u -r1.2 ZipOutputStream.java
--- ZipOutputStream.java        1998/10/01 17:25:49     1.2
+++ ZipOutputStream.java        1999/02/13 06:58:07
@@ -22,7 +22,7 @@

 private static final int ZIPVER = 0x000a;

-private int method = STORED;
+private int method = DEFLATED;
 private int level = Deflater.DEFAULT_COMPRESSION;
 private byte[] lh = new byte[LOC_RECSZ];
 private byte[] ch = new byte[CEN_RECSZ];


*/
