import java.util.*;

public class BooleanBug {
    public static void main(String[] argv) throws Exception {

	Boolean tb = new Boolean(true);
	Boolean fb = new Boolean(false);

	if (tb.booleanValue() != true) {
	    throw new Exception("booleanValue() != true");
	}

	if (fb.booleanValue() != false) {
	    throw new Exception("booleanValue() != false");
	}

	System.out.println("OK");
    }
}

// no bug found
