/*
Extract classes from a zip archive.
*/


import java.io.*;
import java.util.zip.*;

public class ZipExtract {

    public static void main(String[] argv) throws Exception {
	final boolean debug = true;
	String fname;

	if (argv.length != 1) {
	    System.err.println("usage : java ZipExtract zipfile");
	    System.exit(-1);
	}

	fname = argv[0];
	
	FileInputStream fis = new FileInputStream(fname);

	ZipInputStream zin = new ZipInputStream(fis);
	
	ZipEntry entry;

	while ((entry = zin.getNextEntry()) != null) {
	    String name = entry.getName();

	    if (entry.isDirectory()) {
		File dir = new File(name);
		if (! dir.exists()) {
		    if (debug) {
			System.out.println("making directory \"" + 
					   dir.getPath() + "\"");
		    }
		    dir.mkdirs();
		}
		continue;
	    }
	    
	    if (debug) {
		System.out.println("opening output file \"" +
				   name + "\"");
	    }
	    
	    FileOutputStream fos = new FileOutputStream(name);
	    
	    try {
		readwriteStreams(zin, fos);
	    } finally {
		fos.close();
	    }
	}

	zin.close();
    }



    // This method is used to transfer the contents of input stream to
    // and output stream. The input stream will be read until EOF is
    // returned by the read() method.

    static void readwriteStreams(InputStream in, OutputStream out)
	throws IOException
    {
	final boolean debug = true;

	int numRead;
	int numWritten = 0;

	byte[] buffer = new byte[8094];


	if (debug) {
	    System.out.println("read/write buffer size is " +
			       buffer.length + " bytes");
	}
	
	while ((numRead = in.read(buffer,0,buffer.length)) != -1) {
	    if (debug) {
		System.out.println("read " + numRead +
				   " bytes, writing ...");
	    }
	    
	    out.write(buffer,0,numRead);
	    numWritten += numRead;
	}

	if (debug) {
	    System.out.println("wrote a total of " + numWritten + " bytes.");
	}
    }

}
