import java.lang.reflect.*;

public class ReflectedByteBug4 {
    public static void main(String[] argv) throws Exception {
	Method m1 = ReflectedByteBug4.class.getMethod("maxByteObjectMethod", null);
	System.out.println("m1 is " + m1);

	Method m2 = ReflectedByteBug4.class.getMethod("minByteObjectMethod", null);
	System.out.println("m2 is " + m2);


	Byte tb = (Byte) m1.invoke(null,null);
	Byte fb = (Byte) m2.invoke(null,null);

	if (tb.byteValue() != Byte.MAX_VALUE) {
	    throw new Exception("byteValue() != Byte.MAX_VALUE");
	}

	if (fb.byteValue() != Byte.MIN_VALUE) {
	    throw new Exception("byteValue() != Byte.MIN_VALUE");
	}

	System.out.println("OK");
    }

    public static byte maxByteObjectMethod() {
	return Byte.MAX_VALUE;
    }

    public static byte minByteObjectMethod() {
	return Byte.MIN_VALUE;
    }
}



/*
JDK

% java ReflectedByteBug4
m1 is public static byte ReflectedByteBug4.maxByteObjectMethod()
m2 is public static byte ReflectedByteBug4.minByteObjectMethod()
OK

*/


/*
Kaffe before patch

% kaffe ReflectedByteBug4
m1 is public static byte ReflectedByteBug4.maxByteObjectMethod()
m2 is public static byte ReflectedByteBug4.minByteObjectMethod()
java.lang.Exception: byteValue() != Byte.MAX_VALUE
        at java.lang.Throwable.<init>(Throwable.java:37)
        at java.lang.Exception.<init>(Exception.java:21)
        at ReflectedByteBug4.main(ReflectedByteBug4.java:16)


*/


/*
Kaffe after patch

% kaffe ReflectedByteBug4
m1 is public static byte ReflectedByteBug4.maxByteObjectMethod()
m2 is public static byte ReflectedByteBug4.minByteObjectMethod()
OK

*/
