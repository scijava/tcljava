import java.lang.reflect.*;

public class CheckField {

    public static void main(String[] argv) throws Exception {

	Class c_class = C.class;
	
	Field f = c_class.getDeclaredField("class");

	Class c_field = (Class) f.get(null);

	if (c_class != c_field) {
	    System.out.println("c_class != c_filed");
	    System.out.println("c_class is " + c_class);
	    System.out.println("c_filed is " + c_field);
	} else {
	    System.out.println("c_class == c_filed");
	}

    }

}
