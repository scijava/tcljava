// Read two zip streams and see where the differences are.

import java.io.*;
import java.util.zip.*;

public class ZipDiff {

    public static void main(String[] argv) throws Exception {
	FileInputStream fin1 = new FileInputStream("zip1.zip");
	FileInputStream fin2 = new FileInputStream("zip2.zip");
	
	ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
	ByteArrayOutputStream baos2 = new ByteArrayOutputStream();

	int data;

	while ((data = fin1.read()) != -1) {
	    baos1.write(data);
	}
	
	while ((data = fin2.read()) != -1) {
	    baos2.write(data);
	}
	
	
	byte[] data1 = baos1.toByteArray();
	byte[] data2 = baos2.toByteArray();

	
	System.out.println("read " + data1.length + " bytes from stream1");
	System.out.println("read " + data2.length + " bytes from stream2");

	int biggest = data1.length;
	if (data2.length > biggest) {
	    biggest = data2.length;
	}

	for (int i=0; i < biggest ; i++) {
	    System.out.print(i);
	    System.out.print("\t");

	    if (i >= data1.length) {
		// Only the second file has more elements
		System.out.print("\t");
		System.out.print(data2[i]);
		System.out.println();
	    } else if (i >= data2.length) {
		// Only the first file has more elements
		System.out.print(data1[i]);
		System.out.print(" \t");
		System.out.println();
	    } else {
		// compare them
		System.out.print(((int)data1[i]));
		System.out.print('\t');
		System.out.print(((int)data2[i]));
		System.out.print('\t');

		// if they differ then print !!! next to them
		
		if (data1[i] != data2[i]) {
		    System.out.print("!!!");
		}
		System.out.println();
	    }
	}
    }
}
