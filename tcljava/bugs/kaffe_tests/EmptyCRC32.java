import java.util.zip.*;

public class EmptyCRC32 {

    public static void main(String[] argv) throws Exception {
	CRC32 crc = new CRC32();
	System.out.println("empty CRC32 value is " + crc.getValue());
    }

}


// 0 for both Kaffe and JDK
