import java.lang.reflect.*;
import java.util.*;

public class ReflectedBooleanBug1 {
    public static void main(String[] argv) throws Exception {
	Method m1 = ReflectedBooleanBug1.class.getMethod("trueBooleanObjectMethod", null);
	System.out.println("m1 is " + m1);

	Method m2 = ReflectedBooleanBug1.class.getMethod("falseBooleanObjectMethod", null);
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

    public static Boolean trueBooleanObjectMethod() {
	return new Boolean(true);
    }

    public static Boolean falseBooleanObjectMethod() {
	return new Boolean(false);
    }
}


// no bug found
