public class ThrowableException {
    public static void main(String[] argv) throws Exception {
	new MyException();
    }
}


class MyException extends Throwable {

    public MyException() {
	throw new RuntimeException();
    }

}
