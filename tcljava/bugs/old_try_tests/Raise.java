import tcl.lang.*;

public class Raise {
 static int call = 0;
 public static boolean foo(Interp interp) throws Throwable {
   call++;
   if (call == 1)
     throw new NullPointerException();
   else if (call == 2)
     throw new Throwable("msg");
   else if (call == 3)
     throw new TclException(interp,"msg");
   else
     return true;
 }
}
