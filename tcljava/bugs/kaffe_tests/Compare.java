import java.util.*;

public class Compare {
  private Hashtable thing = new Hashtable();

  public Compare() {}

  public void empty() {
    thing = null;
  }

  public Object get1() {
    return thing;
  }

  public Dictionary get2() {
    return thing;
  }

  public Hashtable get3() {
    return thing;
  }

  public boolean compare(Object o1, Object o2) {
    return (o1 == o2);
  }

  public static void main(String[] argv) {
    Compare c = new Compare();
    Object r1 = c.get1();
    Dictionary r2 = c.get2();
    Hashtable r3 = c.get3();

    System.out.println("(r1 == r2) is " + (r1 == r2));
    System.out.println("(r2 == r3) is " + (r2 == r3));
  }

}
