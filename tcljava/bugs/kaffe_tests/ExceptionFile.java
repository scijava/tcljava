public class ExceptionFile
{
    public static void main(String[] argv) throws Exception {
	SomeClass sc = new SomeClass();
	Thread t = new Thread(sc);
	t.start();
    }
}

class SomeClass implements Runnable {
    public void run() {
	try {
	    SomeClass.foo();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static void foo() throws Exception {

	// Throwable -> Exception -> IOException -> EOFException

	throw new java.io.EOFException();
    }
}



/*
JDK output with jit

% java ExceptionFile
java.io.EOFException
        at java.lang.Throwable.<init>(Compiled Code)
        at java.lang.Exception.<init>(Compiled Code)
        at java.io.IOException.<init>(Compiled Code)
        at java.io.EOFException.<init>(Compiled Code)
        at SomeClass.foo(Compiled Code)
        at SomeClass.run(Compiled Code)
        at java.lang.Thread.run(Compiled Code)

*/

/*
JDK output with no jit

% java ExceptionFile
java.io.EOFException
        at SomeClass.foo(ExceptionFile.java:23)
        at SomeClass.run(ExceptionFile.java:13)
        at java.lang.Thread.run(Thread.java)

*/


/*
Kaffe output

% kaffe ExceptionFile
java.io.EOFException
        at java.lang.Throwable.<init>(Throwable.java:31)
        at java.lang.Exception.<init>(Exception.java:17)
        at java.io.IOException.<init>(IOException.java:18)
        at java.io.EOFException.<init>(EOFException.java:18)
        at SomeClass.foo(ExceptionFile.java:23)
        at SomeClass.run(ExceptionFile.java:13)
        at java.lang.Thread.run(Thread.java:245)

*/


/*
Kaffe output with my patch

% kaffe ExceptionFile
java.io.EOFException
        at SomeClass.foo(ExceptionFile.java:23)
        at SomeClass.run(ExceptionFile.java:13)
        at java.lang.Thread.run(Thread.java:245)

*/




