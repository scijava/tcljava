public class InstanceOfBug {
    public static void main(String[] argv) throws Exception {
	Object s = "one";

	if (s instanceof String) {
	    System.out.println("#1 ok");
	} else {
	    System.out.println("#1 error");
	}

	if (s instanceof Object) {
	    System.out.println("#2 ok");
	} else {
	    System.out.println("#2 error");
	}

	Class oc = Object.class;
	Class sc = String.class;

	if (sc.isInstance(s)) {
	    System.out.println("#3 ok");
	} else {
	    System.out.println("#3 error");
	}

	if (oc.isInstance(s)) {
	    System.out.println("#4 ok");
	} else {
	    System.out.println("#4 error");
	}

	if (oc.isAssignableFrom(sc)) {
	    System.out.println("#5 ok");
	} else {
	    System.out.println("#5 error");
	}
	
	if (oc.isAssignableFrom(oc)) {
	    System.out.println("#6 ok");
	} else {
	    System.out.println("#6 error");
	}

    }
}
