import java.lang.reflect.*;
import java.util.*;

public class ReflectedBooleanBug3 {
    public static void main(String[] argv) throws Exception {
	Method m1 = ReflectedBooleanBug3.class.getMethod("trueBooleanObjectMethod", null);
	System.out.println("m1 is " + m1);

	Method m2 = ReflectedBooleanBug3.class.getMethod("falseBooleanObjectMethod", null);
	System.out.println("m2 is " + m2);

	
	ReflectedBooleanBug3 rbb3 = new ReflectedBooleanBug3();

	Boolean tb = (Boolean) m1.invoke(rbb3,null);
	Boolean fb = (Boolean) m2.invoke(rbb3,null);

	if (tb.booleanValue() != true) {
	    throw new Exception("booleanValue() != true");
	}

	if (fb.booleanValue() != false) {
	    throw new Exception("booleanValue() != false");
	}

	System.out.println("OK");
    }

    public boolean trueBooleanObjectMethod() {
	return true;
    }

    public boolean falseBooleanObjectMethod() {
	return false;
    }
}


/*

% kaffe ReflectedBooleanBug3
m1 is public boolean ReflectedBooleanBug3.trueBooleanObjectMethod()
m2 is public boolean ReflectedBooleanBug3.falseBooleanObjectMethod()
java.lang.Exception: booleanValue() != true
        at java.lang.Throwable.<init>(Throwable.java:37)
        at java.lang.Exception.<init>(Exception.java:21)
        at ReflectedBooleanBug3.main(ReflectedBooleanBug3.java:19)

*/
