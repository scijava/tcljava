import java.io.*;
import java.util.zip.*;


public class ZipEntryError {

    public static void main(String[] argv) throws Exception {

	boolean compressed = true;

	if (argv.length != 0)
	    compressed = false;

	StringBufferInputStream sbis =
	    new StringBufferInputStream("file contents");

	ByteArrayOutputStream baos = new ByteArrayOutputStream();

	ZipOutputStream zout = new ZipOutputStream(baos);
	
	ZipEntry ze = new ZipEntry("temp");

	if (compressed) {
	    ze.setMethod(ZipEntry.DEFLATED);
	} else {
	    ze.setMethod(ZipEntry.STORED);

	    // HACK!, we have no way to know these at this point, but
	    // not setting them will generate this error in putNextEntry().
	    // java.util.zip.ZipException:
	    //     STORED entry missing size, compressed size, or crc-32

	    ze.setSize( 0 );
	    ze.setCrc( 0 );
	}

	zout.putNextEntry(ze);
	

	// Read data from input stream to output stream

	int data;
	int bytes = 0;

	while ((data = sbis.read()) != -1) {
	    zout.write(data);
	    bytes++;
	}

	// Update the size of the entry after reading the stream
	ze.setSize( bytes );


	// Close the current ZipEntry
	zout.closeEntry();

	// Close the entire Zip stream
	zout.close();


	// Find out how many bytes were output

	byte[] output = baos.toByteArray();

	System.out.println("output array has " + output.length + " bytes");
    }
}



/*

JDK 1.1 (with compression)

% java ZipEntryError
output array has 137 bytes


JDK 1.2 (with compression)

% java ZipEntryError
output array has 137 bytes


JDK 1.1 (no compression)

java ZipEntryError 1
java.util.zip.ZipException: attempt to write past end of STORED entry
    at java.util.zip.ZipOutputStream.write(ZipOutputStream.java:259)


JDK 1.2 (no compression)

% java ZipEntryError 1
Exception in thread "main" java.util.zip.ZipException: attempt to write past end of STORED entry
    at java.util.zip.ZipOutputStream.write(Compiled Code)


Kaffe (with compression)

% kaffe ZipEntryError
output array has 121 bytes


Kaffe (no compression)

% kaffe ZipEntryError 1
java.util.zip.ZipException: compress size set incorrectly
    at java.util.zip.ZipOutputStream.closeEntry(ZipOutputStream.java:117)

*/
