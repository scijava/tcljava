public class ExceptionLineNumber
{
    public static void main(String[] argv) throws Exception {
	f1();
    }

    public static void f1() {
	f2();
    }

    public static
	void
	f2() {
	f3();
    }

    /*


    */


    public static void f3() {
	f4();
    }

    public static void f4() {
	throw new RuntimeException();
    }
}


// no bug found
