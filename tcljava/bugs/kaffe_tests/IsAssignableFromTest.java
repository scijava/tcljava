public class IsAssignableFromTest {
    public static void main(String[] argv) {
        Class c;
        boolean res;

        c = Object.class;
        try {
        res = c.isAssignableFrom(null);
        } catch (NullPointerException e) {
        System.out.println("catch 1");
        }

        c = Integer.TYPE;
        res = c.isAssignableFrom(Object.class);
        System.out.println("result 1 is " + res);

        c = Integer.TYPE;
        res = c.isAssignableFrom(c);
        System.out.println("result 2 is " + res);

        c = Integer.TYPE;
        res = c.isAssignableFrom(Double.class);
        System.out.println("result 3 is " + res);

        c = Integer.TYPE;
        res = c.isAssignableFrom(String.class);
        System.out.println("result 4 is " + res);
    }
}
