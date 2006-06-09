// These tests check for things that require access the
// the tcl.lang.* package. It is easier to test this
// stuff in one file that to add special access methods
// to the TJC class to support testing.

package tcl.lang;

public class TestTcl {

    public
    static String internalRepToString(TclObject to) {
        InternalRep rep = to.getInternalRep();

        if (rep instanceof TclString) {
            return "TclString";
        } else if (rep instanceof TclBoolean) {
            return "TclBoolean";
        } else if (rep instanceof TclInteger) {
            return "TclInteger";
        } else if (rep instanceof TclDouble) {
            return "TclDouble";
        } else if (rep instanceof TclList) {
            return "TclList";
        } else {
            return "Unknown";
        }
    }

    public
    static String toString(TclObject to) {
        return to.toString();
    }

    public
    static boolean hasNoStringRep(TclObject to) {
        return to.hasNoStringRep();
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

    // Test code that checks the boolean value of an object.
    // A shared object can have its internal rep changed
    // from TclString to TclBoolean and then to TclInteger.

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

    // Use TJC.getBoolean(), this method will change the
    // internal rep from TclString to TclInteger, but
    // nothing is done in this case because the object
    // is already a TclInteger and that is a valid boolean
    // value.

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

    // Use TJC.getBoolean(), this method will change the
    // internal rep from TclString to TclInteger.

    public static String testBoolQuery4(Interp interp) throws TclException {
        StringBuffer results = new StringBuffer(64);
        TclObject ival;
        boolean bval;

        ival = TclString.newInstance("2"); // string internal rep
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

    // Invoking TclBoolean.get() on a TclInteger that has
    // the value 0 or 1 will return a boolean condition
    // but it will not change the internal rep to TclBoolean.

    public static String testBoolQuery5(Interp interp) throws TclException {
        StringBuffer results = new StringBuffer(64);
        TclObject ival;
        boolean bval;

        ival = TclInteger.newInstance(0); // string internal rep
        ival.preserve();
        ival.preserve(); // bump refCount to 2 (shared)

        results.append( ival.getRefCount() );
        results.append( " " );
        results.append( internalRepToString(ival) );
        results.append( " " );

        bval = TclBoolean.get(interp, ival);

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

    // Invoke TclDouble related methods.

    public static String testDouble1(Interp interp) throws TclException {
        TclObject tobj = TclDouble.newInstance(1.0);
        return tobj.toString();
    }

    public static String testDouble2(Interp interp) throws TclException {
        TclObject tobj = TclDouble.newInstance(0);
        return tobj.toString();
    }

    public static String testDouble3(Interp interp) throws TclException {
        StringBuffer results = new StringBuffer();

        TclObject tobj = TclString.newInstance("1");
        double d = TclDouble.get(interp, tobj);

        results.append(d);
        results.append(' ');
        results.append(internalRepToString(tobj));

        return results.toString();
    }

    public static String testDouble4(Interp interp) throws TclException {
        StringBuffer results = new StringBuffer();

        TclObject tobj = TclString.newInstance("0");
        double d = TclDouble.get(interp, tobj);

        results.append(d);
        results.append(' ');
        results.append(internalRepToString(tobj));

        return results.toString();
    }

    public static String testDouble5(Interp interp) throws TclException {
        StringBuffer results = new StringBuffer();

        String srep = "1";

        TclObject tobj = TclString.newInstance(srep);
        double d = TclDouble.get(interp, tobj);

        results.append(d);
        results.append(' ');
        results.append(internalRepToString(tobj));
        results.append(' ');
        // Parse double directly without going through TclDouble logic.
        results.append(Util.getDouble(interp, srep));
        results.append(' ');
        // Parse int directly
        results.append(Util.getInt(interp, srep));

        return results.toString();
    }

    public static String testDouble6(Interp interp) throws TclException {
        StringBuffer results = new StringBuffer();

        // Tcl's behavior related to parsing of integers vs doubles
        // is confusing. Numbers with a leading zero are parsed as
        // an octal integer but the leading zeros are ignored when
        // parsing as a double.

        String srep = "040"; // parsed as int 32 but double 40.0

        TclObject tobj = TclString.newInstance(srep);
        double d = TclDouble.get(interp, tobj);

        results.append(d);
        results.append(' ');
        results.append(internalRepToString(tobj));
        results.append(' ');
        // Parse double directly without going through TclDouble logic.
        results.append(Util.getDouble(interp, srep));
        results.append(' ');
        // Parse int directly
        results.append(Util.getInt(interp, srep));

        return results.toString();
    }

    public static String testDouble7(Interp interp) throws TclException {
        StringBuffer results = new StringBuffer();

        String srep = "0xFF";
        TclObject tobj = TclString.newInstance(srep);

        // Try to parse as a TclDouble
        try {
            double dval = TclDouble.get(interp, tobj);
            results.append(dval);
        } catch (TclException te) {
            results.append("TclException");
        }

        results.append(' ');

        // Try to parse as a TclInteger
        try {
            int ival = TclInteger.get(interp, tobj);
            results.append(ival);
        } catch (TclException te) {
            results.append("TclException");
        }

        return results.toString();
    }

    public static String testDouble8(Interp interp) throws TclException {
        StringBuffer results = new StringBuffer();

        String srep = "1.0";
        TclObject tobj = TclString.newInstance(srep);

        // Try to parse as a TclInteger
        try {
            int ival = TclInteger.get(interp, tobj);
            results.append(ival);
        } catch (TclException te) {
            results.append("TclException");
        }

        results.append(' ');

        // Try to parse as a TclDouble
        try {
            double dval = TclDouble.get(interp, tobj);
            results.append(dval);
        } catch (TclException te) {
            results.append("TclException");
        }

        return results.toString();
    }

    // Test logic related to TclObject.ivalue field. This
    // is either an int value or a bit field based on the
    // internal rep type.

    public static String testIvalueUtil(TclObject tobj) {
        StringBuffer results = new StringBuffer();

        results.append( "isIntegerType" + " " );
        results.append( tobj.isIntegerType() );
        results.append( " " );

        results.append( "isStringType" + " " );
        results.append( tobj.isStringType() );
        results.append( " " );

        results.append( "isDoubleType" + " " );
        results.append( tobj.isDoubleType() );
        results.append( " " );

        results.append( "isListType" + " " );
        results.append( tobj.isListType() );

        return results.toString();
    }

    public static String testIvalue0(Interp interp) throws TclException {
        TclObject tobj = TclInteger.newInstance(0);
        return testIvalueUtil(tobj);
    }

    public static String testIvalue1(Interp interp) throws TclException {
        TclObject tobj = TclInteger.newInstance(1);
        return testIvalueUtil(tobj);
    }

    public static String testIvalue2(Interp interp) throws TclException {
        TclObject tobj = TclInteger.newInstance(2);
        TclObject dup = tobj.duplicate();
        return testIvalueUtil(dup);
    }

    public static String testIvalue3(Interp interp) throws TclException {
        TclObject tobj = TclInteger.newInstance(2);
        // TclInteger -> TclString
        TclString.append(tobj, "");
        return testIvalueUtil(tobj);
    }

    public static String testIvalue4(Interp interp) throws TclException {
        TclObject tobj = TclInteger.newInstance(2);
        // TclInteger -> TclString
        TclString.append(tobj, "");
        TclObject dup = tobj.duplicate();
        return testIvalueUtil(dup);
    }

    public static String testIvalue5(Interp interp) throws TclException {
        TclObject tobj = TclString.newInstance("foo");
        return testIvalueUtil(tobj);
    }

    public static String testIvalue6(Interp interp) throws TclException {
        TclObject tobj = TclString.newInstance("foo");
        TclObject dup = tobj.duplicate();
        return testIvalueUtil(dup);
    }

    public static String testIvalue7(Interp interp) throws TclException {
        TclObject tobj = TclString.newInstance("");
        // TclString -> TclList
        TclList.getLength(interp, tobj);
        return testIvalueUtil(tobj);
    }

    public static String testIvalue8(Interp interp) throws TclException {
        TclObject tobj = TclString.newInstance("1");
        // TclString -> TclInteger
        TclInteger.get(interp, tobj);
        return testIvalueUtil(tobj);
    }

    public static String testIvalue9(Interp interp) throws TclException {
        TclObject tobj = TclString.newInstance("1.0");
        // TclString -> TclDouble
        TclDouble.get(interp, tobj);
        return testIvalueUtil(tobj);
    }

    public static String testIvalue10(Interp interp) throws TclException {
        TclObject tobj = TclDouble.newInstance(1.0);
        // TclDouble -> TclString
        TclString.append(tobj, "");
        return testIvalueUtil(tobj);
    }

    public static String testIvalue11(Interp interp) throws TclException {
        TclObject tobj = TclList.newInstance();
        TclObject dup = tobj.duplicate();
        return testIvalueUtil(dup);
    }

    public static String testIvalue12(Interp interp) throws TclException {
        TclObject tobj = TclDouble.newInstance(1.0);
        TclObject dup = tobj.duplicate();
        return testIvalueUtil(dup);
    }

    public static String testIvalue13(Interp interp) throws TclException {
        byte[] bytes = new byte[1];
        TclObject tba = TclByteArray.newInstance(bytes);

        // Int value matches default ivalue for new TclObject with an internal rep.
        TclObject tobj = TclInteger.newInstance(tba.ivalue);
        return testIvalueUtil(tobj);
    }

    public static String testIvalue14(Interp interp) throws TclException {
        TclObject td = TclDouble.newInstance(1.0);

        // Int value matches default double ivalue.
        TclObject tobj = TclInteger.newInstance(td.ivalue);
        return testIvalueUtil(tobj);
    }

    public static String testIvalue15(Interp interp) throws TclException {
        TclObject tstr = TclString.newInstance("foo");

        // Int value matches default string ivalue.
        TclObject tobj = TclInteger.newInstance(tstr.ivalue);
        return testIvalueUtil(tobj);
    }

    public static String testIvalue16(Interp interp) throws TclException {
        TclObject tlist = TclList.newInstance();

        // Int value matches default list ivalue.
        TclObject tobj = TclInteger.newInstance(tlist.ivalue);
        return testIvalueUtil(tobj);
    }

}

