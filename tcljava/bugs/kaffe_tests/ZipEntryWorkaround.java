import java.io.*;
import java.util.zip.*;

public class ZipEntryWorkaround {

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

	    // set the uncompressed size first

	    ze.setSize( sbis.available() );

	    // set a fake CRC

	    ze.setCrc( 0 );
	}

	zout.putNextEntry(ze);
	

	// Read data from input stream to output stream

	int data;
	int bytes = 0;

	CRC32 crc = new CRC32();
	CheckedInputStream checked = new CheckedInputStream(sbis, crc);

	while ((data = checked.read()) != -1) {
	    zout.write(data);
	    bytes++;
	}

	if (! compressed) {
	    // Update the CRC in the ZipEntry for a STORED entry
	    ze.setCrc( crc.getValue() );
	}
	
	// Close the current ZipEntry
	zout.closeEntry();

	// Close the entire Zip stream
	zout.close();


	// Find out how many bytes were output

	byte[] output = baos.toByteArray();

	System.out.println("output array has " + output.length + " bytes");
    }
}
