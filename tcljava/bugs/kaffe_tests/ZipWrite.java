/*
Writes an uncompressed data stream to a zip file using the java
zip classes.
*/

import java.io.*;
import java.util.zip.*;

public class ZipWrite {

    public static void main(String[] argv) throws Exception {
	String fname = "zip1.zip";

	if ((new File(fname)).exists()) {
	    fname = "zip2.zip";
	}
	
	FileOutputStream fos = new FileOutputStream(fname);

	ZipOutputStream zout = new ZipOutputStream(fos);
	
	ZipEntry ze;

	int total = 10;

	byte[] bytes1 = new byte[total];
	byte[] bytes2 = new byte[total];
	byte[] empty  = new byte[0];
	
	for (int i=0; i < total; i++) {
	    byte num =(byte)
		(((i+1) > Byte.MAX_VALUE) ? ((i+1) % Byte.MAX_VALUE) : (i+1));
	    bytes1[i] = num;
	    bytes2[total - i - 1] = num;
	}

	addEntry("data1", bytes1, zout);
	addEntry("data2", bytes2, zout);
	/*
	addEntry("data3", bytes1, zout);
	addEntry("data4", bytes2, zout);
	addEntry("dir1/", empty, zout);
	addEntry("dir1/data5", bytes1, zout);
	*/

	zout.close();

	File f = new File(fname);
	System.out.println("Wrote \"" + fname + "\"," +
			   " file size = " + f.length() );
    }



    public static void addEntry(String name, byte[] bytes, ZipOutputStream zout)
	throws Exception
    {
	ZipEntry ze = new ZipEntry(name);

	ze.setMethod(ZipEntry.STORED);
	ze.setSize( bytes.length );
	ze.setCrc( 0 );

	zout.putNextEntry(ze);

	zout.write(bytes);
	
	CRC32 crc = new CRC32();
	crc.update(bytes);	
	ze.setCrc( crc.getValue() );

	zout.closeEntry();

	System.out.println("Wrote \"" + name + "\", data size = " + bytes.length);
    }
}

