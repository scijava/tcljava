import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.jar.*;

public class JarWrite {

    public static void main(String[] argv) throws Exception {
	String fname = "zip1.zip";

	if ((new File(fname)).exists()) {
	    fname = "zip2.zip";
	}
	
	FileOutputStream fos = new FileOutputStream(fname);
	//JarOutputStream zout = new JarOutputStream(fos);
	JarOutputStream zout = new JarOutputStream(fos, new Manifest());

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

	//addEntry("data1", bytes1, zout);

	//addEntry("data2", bytes2, zout);

	zout.close();

	File f = new File(fname);
	System.out.println("Wrote \"" + fname + "\"," +
			   " file size = " + f.length() );
    }



    public static void addEntry(String name, byte[] bytes, ZipOutputStream zout)
	throws Exception
    {
	ZipEntry ze = new ZipEntry(name);

	ze.setTime((new Date(1999, 1, 1)).getTime());

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
