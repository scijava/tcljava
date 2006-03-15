// These tests check for things that require access the
// the tcl.lang.* package. It is easier to test this
// stuff in one file that to add special access methods
// to the TJC class to support testing.

package tcl.lang;

public class TestTcl {

    static String internalRepToString(TclObject to) {
        InternalRep rep = to.getInternalRep();

        if (rep instanceof TclBoolean) {
            return "TclBoolean";
        } else if (rep instanceof TclInteger) {
            return "TclInteger";
        } else if (rep instanceof TclString) {
            return "TclString";
        } else if (rep instanceof TclDouble) {
            return "TclDouble";
        } else {
            return "Unknown";
        }
    }

    // Test code that checks the boolean value of an object.
    // In this case, the object is not shared so we can
    // toss out the string rep and regenerate it from the
    // internal boolean rep.

    public static String testBoolQuery1(Interp interp) throws TclException {
        StringBuffer results = new StringBuffer(64);
        TclObject ival;
        boolean bval;

        ival = TclInteger.newInstance(2); // int internal rep
        ival.preserve(); // bump refCount to 1

        results.append( ival.toString() );
        results.append( " " );
        results.append( "refCount" );
        results.append( " " );
        results.append( ival.getRefCount() );
        results.append( " " );
        results.append( internalRepToString(ival) );
        results.append( " " );

        bval = TclBoolean.get(interp, ival);
        // The object is not shared, so pretend we
        // changed it and throw out the string rep
        ival.invalidateStringRep();

        results.append( ival.getRefCount() );
        results.append( " " );
        results.append( internalRepToString(ival) );
        results.append( " " );
        results.append( bval );
        results.append( " " );
        results.append( ival.toString() );
        results.append( " " );

        int ival2 = TclInteger.get(interp, ival);

        results.append( internalRepToString(ival) );

        results.append( " " );

        results.append( ival2 );

        return results.toString();
    }

    // Test code that checks the boolean value of an object. We
    // can't go changing the internal rep from some type to
    // boolean if the object is shared.

    public static String testBoolQuery2(Interp interp) throws TclException {
        StringBuffer results = new StringBuffer(64);
        TclObject ival;
        boolean bval;

        ival = TclInteger.newInstance(2); // int internal rep
        ival.preserve(); // hold refCount at 1
        ival.preserve(); // bump refCount to 2 (shared)
        ival.toString(); // generate string rep from integer

        results.append( ival.toString() );
        results.append( " " );
        results.append( "refCount" );
        results.append( " " );
        results.append( ival.getRefCount() );
        results.append( " " );
        results.append( internalRepToString(ival) );
        results.append( " " );

        bval = TclBoolean.get(interp, ival);
        //ival.invalidateStringRep(); // Can't invalidate with refCount == 2

        results.append( ival.getRefCount() );
        results.append( " " );
        results.append( internalRepToString(ival) );
        results.append( " " );
        results.append( bval );
        results.append( " " );
        results.append( ival.toString() );
        results.append( " " );

        // The string rep is valid at this point, so the
        // string should ba parsed back to 2 here.
        int ival2 = TclInteger.get(interp, ival);

        results.append( internalRepToString(ival) );
        results.append( " " );
        results.append( ival2 );

        return results.toString();
    }

    // Use TJC method that does not change the internal rep.

    public static String testBoolQuery3(Interp interp) throws TclException {
        StringBuffer results = new StringBuffer(64);
        TclObject ival;
        boolean bval;

        ival = TclInteger.newInstance(2); // int internal rep
        ival.preserve(); // hold refCount at 1

        results.append( ival.toString() );
        results.append( " " );
        results.append( ival.getRefCount() );
        results.append( " " );
        results.append( internalRepToString(ival) );
        results.append( " " );

        // Use TJC.getBoolean() instead of TclBoolean.get()
        //bval = TclBoolean.get(interp, ival);
        bval = TJC.getBoolean(interp, ival);

        results.append( ival.getRefCount() );
        results.append( " " );
        results.append( internalRepToString(ival) );
        results.append( " " );
        results.append( ival.toString() );
        results.append( " " );
        results.append( TclInteger.get(interp, ival) );
        results.append( " " );
        results.append( bval );

        return results.toString();
    }

}

