import java.lang.reflect.*;

public class ReflectedDoubleBug1 {
    public static void main(String[] argv) throws Exception {
	Method m1 = ReflectedDoubleBug1.class.getMethod("maxDoubleObjectMethod", null);
	System.out.println("m1 is " + m1);

	Method m2 = ReflectedDoubleBug1.class.getMethod("minDoubleObjectMethod", null);
	System.out.println("m2 is " + m2);


	Double one = (Double) m1.invoke(null,null);
	Double two = (Double) m2.invoke(null,null);

	if (one.doubleValue() != Double.MAX_VALUE) {
	    throw new Exception("doubleValue() != Double.MAX_VALUE");
	}

	if (two.doubleValue() != Double.MIN_VALUE) {
	    throw new Exception("doubleValue() != Double.MIN_VALUE");
	}

	System.out.println("OK");
    }

    public static double maxDoubleObjectMethod() {
	return Double.MAX_VALUE;
    }

    public static double minDoubleObjectMethod() {
	return Double.MIN_VALUE;
    }
}
