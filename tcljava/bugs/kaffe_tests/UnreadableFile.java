import java.io.*;

public class UnreadableFile {

    public static void main(String[] argv) throws Exception {
	
	// The file "unreadable" needs to have no read permission

	File unreadable = new File("unreadable");

	if (unreadable.exists()) {
	    FileInputStream fin = new FileInputStream(unreadable);
	} else {
	    System.out.println(unreadable + " does not exist");
	}
    }


}


/*

touch unreadable
chmod 200 unreadable



JDK 1.1

java.io.FileNotFoundException: unreadable
        at java.io.FileInputStream.<init>(FileInputStream.java)
        at java.io.FileInputStream.<init>(FileInputStream.java)
        at UnreadableFile.main(UnreadableFile.java:12)


JDK 1.2

java UnreadableFile
exception in thread "main" java.io.FileNotFoundException: unreadable (Permission denied)
        at java.io.FileInputStream.open(Native Method)
        at java.io.FileInputStream.<init>(FileInputStream.java:68)
        at java.io.FileInputStream.<init>(FileInputStream.java:99)
        at UnreadableFile.main(UnreadableFile.java:12)




From the JDK docs.

class FileNotFoundException

Signals that a file could not be found.


*/
