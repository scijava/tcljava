import java.lang.reflect.*;

public class ReflectedFloatBug1 {
    public static void main(String[] argv) throws Exception {
	Method m1 = ReflectedFloatBug1.class.getMethod("maxFloatObjectMethod", null);
	System.out.println("m1 is " + m1);

	Method m2 = ReflectedFloatBug1.class.getMethod("minFloatObjectMethod", null);
	System.out.println("m2 is " + m2);


	Float one = (Float) m1.invoke(null,null);
	Float two = (Float) m2.invoke(null,null);

	if (one.floatValue() != Float.MAX_VALUE) {
	    throw new Exception("floatValue() != Float.MAX_VALUE");
	}

	if (two.floatValue() != Float.MIN_VALUE) {
	    throw new Exception("floatValue() != Float.MIN_VALUE");
	}

	System.out.println("OK");
    }

    public static float maxFloatObjectMethod() {
	return Float.MAX_VALUE;
    }

    public static float minFloatObjectMethod() {
	return Float.MIN_VALUE;
    }
}
