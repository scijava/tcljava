import java.util.*;

public class HashtableBug3 {
    public static void main(String[] argv) throws Exception {
	Hashtable h = new Hashtable();

	for (int i = 0 ; i < 100000 ; i++) {
	    MyClass mc = new MyClass();
	    h.put(mc, "mc");
	}
    }
    
    public static class MyClass {}
}
