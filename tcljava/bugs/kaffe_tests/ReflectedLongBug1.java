import java.lang.reflect.*;

public class ReflectedLongBug1 {
    public static void main(String[] argv) throws Exception {
	Method m1 = ReflectedLongBug1.class.getMethod("maxLongObjectMethod", null);
	System.out.println("m1 is " + m1);

	Method m2 = ReflectedLongBug1.class.getMethod("minLongObjectMethod", null);
	System.out.println("m2 is " + m2);


	Long one = (Long) m1.invoke(null,null);
	Long two = (Long) m2.invoke(null,null);

	if (one.longValue() != Long.MAX_VALUE) {
	    throw new Exception("longValue() != Long.MAX_VALUE");
	}

	if (two.longValue() != Long.MIN_VALUE) {
	    throw new Exception("longValue() != Long.MIN_VALUE");
	}

	System.out.println("OK");
    }

    public static long maxLongObjectMethod() {
	return Long.MAX_VALUE;
    }

    public static long minLongObjectMethod() {
	return Long.MIN_VALUE;
    }
}
