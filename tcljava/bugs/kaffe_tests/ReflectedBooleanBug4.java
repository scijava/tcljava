import java.lang.reflect.*;

public class ReflectedBooleanBug4 {
    public static void main(String[] argv) throws Exception {
	Method m1 = ReflectedBooleanBug4.class.getMethod("trueBooleanObjectMethod", null);
	System.out.println("m1 is " + m1);

	Method m2 = ReflectedBooleanBug4.class.getMethod("falseBooleanObjectMethod", null);
	System.out.println("m2 is " + m2);


	Boolean tb = (Boolean) m1.invoke(null,null);
	Boolean fb = (Boolean) m2.invoke(null,null);

	if (tb.booleanValue() != true) {
	    throw new Exception("booleanValue() != true");
	}

	if (fb.booleanValue() != false) {
	    throw new Exception("booleanValue() != false");
	}

	System.out.println("OK");
    }

    public static boolean trueBooleanObjectMethod() {
	return true;
    }

    public static boolean falseBooleanObjectMethod() {
	return false;
    }
}

/*

% kaffe ReflectedBooleanBug4
m1 is public static boolean ReflectedBooleanBug4.trueBooleanObjectMethod()
m2 is public static boolean ReflectedBooleanBug4.falseBooleanObjectMethod()
java.lang.Exception: booleanValue() != true
        at java.lang.Throwable.<init>(Throwable.java:37)
        at java.lang.Exception.<init>(Exception.java:21)
        at ReflectedBooleanBug4.main(ReflectedBooleanBug4.java:17)

*/
