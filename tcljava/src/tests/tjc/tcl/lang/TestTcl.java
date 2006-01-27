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

    // Test Variable lookup

    public static String testVarLookup1(Interp interp) throws TclException {
        // Lookup scalar local variable named "var"
        String name = "var";
        int flags = 0;
        Var lvar;

        lvar = TJC.resolveVarScalar(interp, name, flags);
        if (lvar == TJC.VAR_NO_CACHE) {
            // Var was resolved, but it could not be cached
            return "VAR_NO_CACHE";
        }
        // Var was resolved and it can be cached
        return "OK_CACHE";
    }

    public static String testVarLookup2(Interp interp) throws TclException {
        // Lookup scalar local variable named "var"
        String name = "var";
        int flags = 0;
        Var lvar;

        lvar = TJC.resolveVarScalar(interp, name, flags);
        if (lvar == TJC.VAR_NO_CACHE) {
            // Var was resolved, but it could not be cached
            return "VAR_NO_CACHE";
        }
        // Var was resolved and it can be cached
        if (!isVarScalarValid(lvar)) {
            return "NOT_VALID";
        }
        TclObject oval = getVarScalar(lvar);
        return oval.toString();
    }

    public static String testVarLookup3(Interp interp) throws TclException {
        // Lookup scalar local variable named "var"
        String name = "var";
        int flags = 0;
        Var lvar;

        lvar = TJC.resolveVarScalar(interp, name, flags);
        if (lvar == TJC.VAR_NO_CACHE) {
            // Var was resolved, but it could not be cached
            return "VAR_NO_CACHE";
        }
        // Var was resolved and it can be cached
        if (!isVarScalarValid(lvar)) {
            return "NOT_VALID1";
        }
        // Now unset the var in the local frame and check
        // that the variable is marked as invalid.
        interp.eval("unset var");
        if (!isVarScalarValid(lvar)) {
            return "NOT_VALID2";
        }
        return "UNEXPECTED_RETURN";
    }

    public static String testVarLookup4(Interp interp) throws TclException {
        // Lookup scalar global variable named "var"
        String name = "var";
        int flags = TCL.GLOBAL_ONLY;
        Var lvar;

        lvar = TJC.resolveVarScalar(interp, name, flags);
        if (lvar == TJC.VAR_NO_CACHE) {
            // Var was resolved, but it could not be cached
            return "VAR_NO_CACHE";
        }
        // Var was resolved and it can be cached
        if (!isVarScalarValid(lvar)) {
            return "NOT_VALID";
        }
        TclObject oval = getVarScalar(lvar);
        return oval.toString();
    }

    public static String testVarLookup5(Interp interp) throws TclException {
        // Use updateVarCache() and getVarScalar() methods to
        // test variable cache logic.
        updateVarCache(interp, 0);

        TclObject tobj = getVarScalar(interp, "var", 0, varcache5_1, 1);
        return tobj.toString();
    }

    static
    Var varcache5_1 = null;
    static
    Var varcache5_2 = null;

    static
    Var updateVarCache(
        Interp interp,
        int cacheId)
    {
        String part1;
        String part2 = null;
        int flags = 0;
        Var lvar;

        switch ( cacheId ) {
            case 0: {
                varcache5_1 = null;
                varcache5_2 = null;
                return null;
            }
            case 1: {
                part1 = "var";
                break;
            }
            case 2: {
                part1 = "x";
                break;
            }
            default: {
                throw new TclRuntimeError("default: cacheId " + cacheId);
            }
        }

        lvar = TJC.resolveVarScalar(interp, part1, flags);

        switch ( cacheId ) {
            case 1: {
                varcache5_1 = lvar;
                break;
            }
            case 2: {
                varcache5_2 = lvar;
                break;
            }
        }
        return lvar;
    }

    static
    private final
    TclObject getVarScalar(
        Interp interp,
        String name,
        int flags,
        Var var,
        int cacheId)
            throws TclException
    {
        if (var == null ||
                ((var != TJC.VAR_NO_CACHE) &&
                var.isVarCacheInvalid())) {
            var = updateVarCache(interp, cacheId);
        }

        if (var == TJC.VAR_NO_CACHE) {
            return interp.getVar(name, null, flags);
        } else {
            return getVarScalar(var);
        }
    }

    static
    private final
    TclObject setVarScalar(
        Interp interp,
        String name,
        TclObject value,
        int flags,
        Var var,
        int cacheId)
            throws TclException
    {
        TclObject retValue;
        boolean update = false;

        if (var == null ||
                ((var != TJC.VAR_NO_CACHE) &&
                var.isVarCacheInvalid())) {
            update = true;
        }

        if (update || (var == TJC.VAR_NO_CACHE)) {
            retValue = interp.setVar(name, null, value, flags);
        } else {
            retValue = TJC.setVarScalar(var, value);
        }

        if (update) {
            updateVarCache(interp, cacheId);
        }

        return retValue;
    }

    private static boolean isVarScalarValid(Var var) {
        return (!var.isVarCacheInvalid());
    }

    private static TclObject getVarScalar(Var var) {
        return (TclObject) var.value;
    }

    public static String testVarLookup6(Interp interp) throws TclException {
        // Set a cached variable value and then look it up
        updateVarCache(interp, 0);

        TclObject tvalue = TclString.newInstance("VALUE10");
        setVarScalar(interp, "var", tvalue, 0, varcache5_1, 1);

        TclObject tobj = getVarScalar(interp, "var", 0, varcache5_1, 1);
        return tobj.toString();
    }

    public static String testVarLookup7(Interp interp) throws TclException {
        // Set a cached variable value and then look it up
        updateVarCache(interp, 0);

        StringBuffer results = new StringBuffer();
        TclObject tobj;
        TclObject tvalue;

        tvalue = TclString.newInstance("A");
        tobj = setVarScalar(interp, "var", tvalue, 0, varcache5_1, 1);
        results.append(tobj.toString());
        results.append(" ");

        tobj = getVarScalar(interp, "var", 0, varcache5_1, 1);
        results.append(tobj.toString());
        results.append(" ");

        tvalue = TclString.newInstance("B");
        tobj = setVarScalar(interp, "var", tvalue, 0, varcache5_1, 1);
        results.append(tobj.toString());
        results.append(" ");

        tobj = getVarScalar(interp, "var", 0, varcache5_1, 1);
        results.append(tobj.toString());
        results.append(" ");

        tvalue = TclString.newInstance("C");
        tobj = setVarScalar(interp, "var", tvalue, 0, varcache5_1, 1);
        results.append(tobj.toString());
        results.append(" ");

        tobj = getVarScalar(interp, "var", 0, varcache5_1, 1);
        results.append(tobj.toString());
        results.append(" ");

        updateVarCache(interp, 0);
        results.append(interp.getVar("var", null, 0));

        return results.toString();
    }

}

