public class Test {

    public static void main(String[] argv) {
	C c = new C();

	Class c_class = C.class;
	Class c_getclass = c.getClass();

	if (c_class != c_getclass) {
	    System.out.println("c_class != c_getclass");
	    System.out.println("c_class is " + c_class);
	    System.out.println("c_getclass is " + c_getclass);
	} else {
	    System.out.println("c_class == c_getclass");
	}

	I i = (I) c;

	Class i_getclass = i.getClass();

	if (i_getclass != c_getclass) {
	    System.out.println("i_getclass != c_getclass");
	    System.out.println("i_getclass is " + i_getclass);
	    System.out.println("c_getclass is " + c_getclass);
	} else {
	    System.out.println("i_getclass == c_getclass");
	}

    }

}
