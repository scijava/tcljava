import java.lang.reflect.*;
import java.beans.*;
import java.util.*;

public class PropertyDescriptorBug {
    public static void main(String[] argv) throws Exception {

	Class cls = java.awt.Button.class;

	BeanInfo beanInfo = Introspector.getBeanInfo(cls);

	PropertyDescriptor descriptors[] = beanInfo.getPropertyDescriptors();
	
	for (int i = 0; i < descriptors.length; i++) {
	    if (descriptors[i] == null) {
		throw new NullPointerException("descriptor is null");
	    }
	    if (descriptors[i].getName() == null) {
		throw new NullPointerException("descriptor.getName() is null");
	    }

	    System.out.println("prop name is " + descriptors[i].getName());
	}

    }
}





/*

JDK output

prop name is enabled
prop name is actionCommand
prop name is foreground
prop name is label
prop name is visible
prop name is background
prop name is font
prop name is name

*/



/*

Kaffe output

% java PropertyDescriptorBug
java.lang.NullPointerException: descriptor.getName() is null
        at java/lang/Throwable.<init>(Throwable.java:37)
        at java/lang/Exception.<init>(Exception.java:21)
        at java/lang/RuntimeException.<init>(RuntimeException.java:21)
        at java/lang/NullPointerException.<init>(NullPointerException.java:21)
        at PropertyDescriptorBug.main(PropertyDescriptorBug.java:19)

*/


/*

Kaffe output with setName() patch added

% kaffe PropertyDescriptorBug
prop name is minimumSize
prop name is peer
prop name is enabled
prop name is foreground
prop name is class
prop name is treeLock
prop name is preferredSize
prop name is focusTraversable
prop name is bounds
prop name is visible
prop name is toolkit
prop name is actionCommand
prop name is graphics
prop name is valid
prop name is label
prop name is maximumSize
prop name is doubleBuffered
prop name is locationOnScreen
prop name is name
prop name is font
prop name is cursor
prop name is background
prop name is alignmentY
prop name is parent
prop name is size
prop name is alignmentX
prop name is location
prop name is showing
prop name is locale

*/





/*

% cvs diff -r1.1 PropertyDescriptor.java 
Index: PropertyDescriptor.java
===================================================================
RCS file: /home/cvspublic/kaffe/libraries/javalib/java/beans/PropertyDescriptor.java,v
retrieving revision 1.1
diff -u -r1.1 PropertyDescriptor.java
--- PropertyDescriptor.java     1998/07/14 17:02:00     1.1
+++ PropertyDescriptor.java     1999/02/04 09:28:39
@@ -28,6 +28,7 @@
 
 public PropertyDescriptor(String propertyName, Class beanClass, String getterName, String setterName) throws IntrospectionException
 {
+        setName(propertyName);
        this.getter = null;
        this.setter = null;
        this.rettype = null;
@@ -51,6 +52,7 @@
 
 public PropertyDescriptor(String propertyName, Method getter, Method setter) throws IntrospectionException
 {
+        setName(propertyName);
        this.getter = getter;
        this.setter = setter;
        rettype = null;

*/
