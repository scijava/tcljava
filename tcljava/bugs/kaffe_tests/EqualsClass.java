public class EqualsClass {

    public EqualsClass() {}

    public boolean equals(Object obj) {
	System.out.println("this is " + this);
	if (this == null) {
	    throw new RuntimeException("this is null");
	} else {
	    System.out.println("this.hashCode() is " + this.hashCode());
	}
	
	System.out.println("obj is " + obj);

	if (obj == null) {
	    System.out.println("obj is null");
	} else {
	    System.out.println("obj.hashCode() is " + obj.hashCode());
	}

	System.out.println("(this == obj) is " + (this == obj));

	return (this == obj);
    }
}
