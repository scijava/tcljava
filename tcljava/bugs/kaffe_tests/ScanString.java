public class ScanString {

    public static void main(String[] argv) throws Exception {
	String s = String.valueOf('\0');
	if ('\0' != s.charAt(0)) {
	    throw new Exception("char not 0");
	}
    }

}
