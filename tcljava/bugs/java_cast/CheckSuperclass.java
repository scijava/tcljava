import java.util.*;

public class CheckSuperclass {

    public static void main(String[] argv) {
	Class c_class = C.class;
	Class i_class = I.class;
	Class o_class = Object.class;
	

	Class tmp;

	tmp = o_class.getSuperclass();

	if (tmp != null) {
	    System.out.println("getSuperclass() for Object should have returned null, it returned " + tmp);
	}

	tmp = c_class.getSuperclass();

	if (tmp != o_class) {
	    System.out.println("getSuperclass() for C should have returned Object, it returned " + tmp);
	}


	tmp = i_class.getSuperclass();
	
	if (tmp != null) {
	    System.out.println("getSuperclass() for I should have returned null, it returned " + tmp);
	}

    }


}
