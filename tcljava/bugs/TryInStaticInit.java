public class TryInStaticInit {

    static final boolean cond;

    static {
	try
	{
	    cond = true;
	}
        catch(Exception e) {
	    // do nothing
	}
    }

}

