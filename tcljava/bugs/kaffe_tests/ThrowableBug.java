public class ThrowableBug {
    public static void main(String[] argv) throws Exception {
	Throwable t = new Throwable();

	if (t.equals(t)) {
	    System.out.println("OK");
	} else {
	    System.out.println("ERROR");
	}
    }
}


// no bug found
