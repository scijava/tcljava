public class echo2 {

    public static void main(String[] argv) throws Exception {
	System.out.println("Please begin typing, type quit to stop");
	
	while (true) {
	    int avail = System.in.available();

	    if (avail < 0) {
		throw new Exception("System.in.available() returned " + avail);
	    } else if (avail == 0) {
		Thread.currentThread().sleep(100);
	    } else {
		byte[] buff = new byte[avail];
		int c;
		int i = 0;
                
                while (avail-- > 0) {
                    c = System.in.read();
		    buff[i++] = (byte) c;
                }

		// quit if they typed "quit"
		if ((buff.length == 5 || buff.length == 6) &&
			buff[0] == 'q' && buff[1] == 'u' &&
			buff[2] == 'i' && buff[3] == 't') {System.exit(0);}

		
		// echo back what was just typed
		System.out.println("-----ECHO BEGIN-----");
		System.out.write(buff);
		System.out.println("-----ECHO END-----");
		System.out.println();
	    }
	}
    }
}
