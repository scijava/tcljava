import java.lang.reflect.*;
import java.util.*;

public class ReflectedBooleanBug2 {
    public static void main(String[] argv) throws Exception {
	Method m1 = ReflectedBooleanBug2.class.getMethod("trueBooleanObjectMethod", null);
	System.out.println("m1 is " + m1);

	Method m2 = ReflectedBooleanBug2.class.getMethod("falseBooleanObjectMethod", null);
	System.out.println("m2 is " + m2);

	
	ReflectedBooleanBug2 rbb2 = new ReflectedBooleanBug2();

	Boolean tb = (Boolean) m1.invoke(rbb2,null);
	Boolean fb = (Boolean) m2.invoke(rbb2,null);

	if (tb.booleanValue() != true) {
	    throw new Exception("booleanValue() != true");
	}

	if (fb.booleanValue() != false) {
	    throw new Exception("booleanValue() != false");
	}

	System.out.println("OK");
    }

    public Boolean trueBooleanObjectMethod() {
	return new Boolean(true);
    }

    public Boolean falseBooleanObjectMethod() {
	return new Boolean(false);
    }
}



// no bug found
