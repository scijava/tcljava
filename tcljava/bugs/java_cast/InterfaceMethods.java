import java.util.*;
import java.lang.reflect.*;


public class InterfaceMethods {

    public static void main(String[] argv) {
	printMethods(C.class);
	printMethods(I.class);

    }



    public static void printMethods(Class c) {
	int i;
	Method[] ms = getAllDeclaredMethods(c);

	System.out.println("printing Methods for " + c);

	for (i=0 ; i < ms.length ; i++) {
	    System.out.println(ms[i]);
	}
    }




    static Method[] getAllDeclaredMethods(Class cls)
    {
	Method methods[] = null;
	if (methods != null) {
	    return methods;
	}
	
	Vector vec = new Vector();
	
	for (Class c = cls; c != null; ) {
	    mergeMethods(c, c.getDeclaredMethods(), vec);
	    
	    Class interfaces[] = c.getInterfaces();
	    for (int i = 0; i < interfaces.length; i++) {
		mergeMethods(interfaces[i], interfaces[i].getMethods(), vec);
	    }

	    if (! c.isInterface()) {
		c = c.getSuperclass();
	    } else {
		c = Object.class; // if cls is an interface add Object methods
	    }
	}
	
	methods = new Method[vec.size()];
	vec.copyInto(methods);
	
	return methods;
    }


    static void mergeMethods(
		     Class c,
		     Method methods[],
		     Vector vec)
    {
	for (int i=0; i < methods.length; i++) {
	    Method newMeth = methods[i];

	    vec.addElement(newMeth);
	}
    }



}
