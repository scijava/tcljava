public class BadRef {
    public static void main(String[] argv) throws Exception {
	Object o = o = null;
	System.out.println("o is " + o);
    }
}
