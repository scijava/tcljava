public class SwitchBug {
    public static void main(String[] argv) {
	switch (1) {
	case 1:
	    int num = 1;
	case 2:
	    int num = 2;
	}
    }
}


/*

% jikes SwitchBug.java      
Found 1 semantic error and compiling "SwitchBug.java":
     7.             int num = 2;
                        <->
*** Error[61]: Duplicate declaration of local variable "num"



% /soft/java/JDK-1.1.6/bin/javac SwitchBug.java
SwitchBug.java:7: Variable 'num' is already defined in this method.
            int num = 2;
                ^
1 error



% /tmp/mo/install_kaffe/bin/javac SwitchBug.java
(no error)


*/
