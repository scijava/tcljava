import java.lang.reflect.*;
import java.util.*;

public class MethodBug {
    public static void main(String[] argv) throws Exception {
	Method m1 = MethodBug.class.getMethod("m1", null);
	System.out.println("m1 is " + m1);

	Method m2 = MethodBug.class.getMethod("m2", null);
	System.out.println("m2 is " + m2);
    }

    public void m1() {}
    public String m2() {return null;}
}
