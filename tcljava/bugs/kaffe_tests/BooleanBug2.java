import java.util.*;

public class BooleanBug2 {
    public static void main(String[] argv) throws Exception {
	boolean tb = trueBooleanObjectMethod();
	boolean fb = falseBooleanObjectMethod();
    }

    public static boolean trueBooleanObjectMethod() {
	return true;
    }

    public static boolean falseBooleanObjectMethod() {
	return false;
    }

}

// no bug found
