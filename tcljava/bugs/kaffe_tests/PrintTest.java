public class PrintTest {
    public static void main(String[] argv) {
	//char c = '\u0030';
	char c = (int) 30;

	if (c == ' ') {
	    System.out.println(((int) c) + " is the space char");
	}


	System.out.println("unicode value is " + ((int) c));
	System.out.println("printed value is '" + c + "'");

	System.out.println("isSpaceChar() = " + Character.isSpaceChar(c));
    }
}
