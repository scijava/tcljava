import java.util.*;

public class HashtableBug2 {
    public static void main(String[] argv) throws Exception {
	MyClass mc1 = new MyClass();
	MyClass mc2 = new MyClass();
	MyClass mc3 = new MyClass();
	MyClass mc4 = new MyClass();

	Hashtable h = new Hashtable();

	h.put(mc1, "mc1");
	h.put(mc2, "mc2");
	h.put(mc3, "mc3");
	h.put(mc4, "mc4");

	String m;

	m = (String) h.get(mc1);
	System.out.println("m is " + m);

	m = (String) h.get(mc2);
	System.out.println("m is " + m);

	m = (String) h.get(mc3);
	System.out.println("m is " + m);

	m = (String) h.get(mc4);
	System.out.println("m is " + m);
    }
}


class MyClass {}
