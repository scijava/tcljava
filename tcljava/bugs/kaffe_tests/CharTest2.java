public class CharTest2 {
    public static void main(String[] argv) {
	char c;
	int i;
	for (i = 0; i < 200 ; i++) {
	    c = (char) i;
	    System.out.println(i + " = " + Character.isLetterOrDigit(c));
	}
    }
}
