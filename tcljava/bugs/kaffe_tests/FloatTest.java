public class FloatTest {
    public static void main(String[] argv) {
	Float f1 = Float.valueOf("7.0");
	System.out.println(f1);

	Float f2 = new Float("7.0");
	System.out.println(f2);
    }
}


/*

JDK
7.0
7.0

*/


/*

Kaffe
7
7

*/
