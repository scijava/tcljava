import java.lang.reflect.*;

public class ReflectedIntBug4 {
    public static void main(String[] argv) throws Exception {
	Method m1 = ReflectedIntBug4.class.getMethod("trueIntObjectMethod", null);
	System.out.println("m1 is " + m1);

	Method m2 = ReflectedIntBug4.class.getMethod("falseIntObjectMethod", null);
	System.out.println("m2 is " + m2);


	Integer tb = (Integer) m1.invoke(null,null);
	Integer fb = (Integer) m2.invoke(null,null);

	if (tb.intValue() != 1) {
	    throw new Exception("tb.intValue() != 1");
	}

	if (fb.intValue() != 0) {
	    throw new Exception("fb.intValue() != 0");
	}

	System.out.println("OK");
    }

    public static int trueIntObjectMethod() {
	return 1;
    }

    public static int falseIntObjectMethod() {
	return 0;
    }
}

