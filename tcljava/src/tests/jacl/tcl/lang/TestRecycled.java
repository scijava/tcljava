/*
 * TestRecycled.java --
 *
 * Copyright (c) 2006 Mo DeJong
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TestRecycled.java,v 1.1 2006/06/23 20:53:40 mdejong Exp $
 *
 */

package tcl.lang;

class TestRecycled
{
    // Test interp result set operation when the value
    // to be set is not a common value. This case
    // can be handled with a recycled TclObject so
    // that a new is avoided inside setResult(int).

    public static String testRecycledInt0(Interp interp) throws TclException {
        StringBuffer sb = new StringBuffer();

        // Interp result set to empty shared constant
        interp.resetResult();

        // Set interp result to int result that is not
        // a common value.

        interp.setResult(100);
        TclObject recycled = interp.getResult();

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Now set interp result to another value
        // that is not a common value, this should
        // reuse the recycled TclObject.

        interp.setResult(200);

        // Test that the recycled object was reused
        // by getting the int value out of the
        // original ref. Also check that the
        // interp result is set to the same
        // TclObject.

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");
        sb.append("was_recycled " + (recycled == interp.getResult()));

        return sb.toString();
    }

    public static String testRecycledInt1(Interp interp) throws TclException {
        StringBuffer sb = new StringBuffer();

        // Interp result set to empty shared constant
        interp.resetResult();

        // Set interp result to int result that is not
        // a common value.

        interp.setResult(100);
        TclObject recycled = interp.getResult();

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Now increment the ref count of recycled
        // so that it will be shared even after
        // the interp result is reset.

        recycled.preserve();

        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Reset the result, this drops the second ref to
        // recycled but it is still shared because of the
        // preserve call that was just made.

        interp.resetResult();

        sb.append("refCount " + recycled.getRefCount());

        // Release the held ref
        recycled.release();

        return sb.toString();
    }

    public static String testRecycledInt2(Interp interp) throws TclException {
        StringBuffer sb = new StringBuffer();

        // Interp result set to empty shared constant
        interp.resetResult();

        // Set interp result to int result that is not
        // a common value.

        interp.setResult(100);
        TclObject recycled = interp.getResult();

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Now increment the ref count of recycled
        // so that it will be shared when the
        // next call to setResult(int) is made.

        recycled.preserve();

        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        interp.setResult(200);

        // The recycled value should have been
        // set to a new TclObject. This new
        // value is then set as the interp
        // result.

        sb.append("new_recycled " + (recycled != interp.getResult()));
        sb.append(" ");
        sb.append("refCount " + interp.getResult().getRefCount());
        sb.append(" ");

        // The refCount of recycled should now be 1.
        // The only ref held at this point is the
        // one from this code. The interp result
        // was is released when the new TclObject
        // is set as the interp result. The ref held
        // inside Interp on the recycled object
        // was also released just before the
        // new TclObject was allocated.

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());

        // Deallocated old recycled TclObject
        recycled.release();

        return sb.toString();
    }

    public static String testRecycledInt3(Interp interp) throws TclException {
        StringBuffer sb = new StringBuffer();

        // Interp result set to empty shared constant
        interp.resetResult();

        // Set interp result to int result that is not
        // a common value.

        interp.setResult(100);
        TclObject recycled = interp.getResult();

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Now reset the interp result, this should
        // drop the refCount back down to 1.

        interp.resetResult();

        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Set a new int result that is not a common
        // value. This should use the recycled object
        // again since the refCount is 1. This will
        // also set the interp result.

        interp.setResult(200);

        sb.append("was_recycled " + (recycled == interp.getResult()));
        sb.append(" ");
        sb.append("getResult " + interp.getResult().toString());
        sb.append(" ");
        sb.append("refCount " + interp.getResult().getRefCount());

        return sb.toString();
    }

    public static String testRecycledInt4(Interp interp) throws TclException {
        StringBuffer sb = new StringBuffer();

        interp.resetResult();

        // Get recycled int ref

        interp.setResult(100);        
        TclObject recycled = interp.getResult();

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Set var "v" to an int value. This operation
        // will reuse the recycled value and save it
        // in the variable. A side effect of this
        // operation will be to change the object
        // that is current the interp result, but
        // that is ok. If the caller did not want
        // the result object to change then the
        // refCount should have been incremented.

        interp.setVar("v", null, 200, 0);

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Reset the interp result, this will drop
        // the ref held by the interp result.

        interp.resetResult();

        sb.append("refCount " + recycled.getRefCount());

        return sb.toString();
    }

    // Recycled double values

    public static String testRecycledDouble0(Interp interp) throws TclException {
        StringBuffer sb = new StringBuffer();

        // Interp result set to empty shared constant
        interp.resetResult();

        // Set interp result to double result that is not
        // a common value.

        interp.setResult(100.0);
        TclObject recycled = interp.getResult();

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Now set interp result to another value
        // that is not a common value, this should
        // reuse the recycled TclObject.

        interp.setResult(200.0);

        // Test that the recycled object was reused
        // by getting the double value out of the
        // original ref. Also check that the
        // interp result is set to the same
        // TclObject.

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");
        sb.append("was_recycled " + (recycled == interp.getResult()));

        return sb.toString();
    }

    public static String testRecycledDouble1(Interp interp) throws TclException {
        StringBuffer sb = new StringBuffer();

        // Interp result set to empty shared constant
        interp.resetResult();

        // Set interp result to int result that is not
        // a common value.

        interp.setResult(100.0);
        TclObject recycled = interp.getResult();

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Now increment the ref count of recycled
        // so that it will be shared even after
        // the interp result is reset.

        recycled.preserve();

        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Reset the result, this drops the second ref to
        // recycled but it is still shared because of the
        // preserve call that was just made.

        interp.resetResult();

        sb.append("refCount " + recycled.getRefCount());

        // Release the held ref
        recycled.release();

        return sb.toString();
    }

    public static String testRecycledDouble2(Interp interp) throws TclException {
        StringBuffer sb = new StringBuffer();

        // Interp result set to empty shared constant
        interp.resetResult();

        // Set interp result to double result that is not
        // a common value.

        interp.setResult(100.0);
        TclObject recycled = interp.getResult();

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Now increment the ref count of recycled
        // so that it will be shared when the
        // next call to setResult(double) is made.

        recycled.preserve();

        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        interp.setResult(200.0);

        // The recycled value should have been
        // set to a new TclObject. This new
        // value is then set as the interp
        // result.

        sb.append("new_recycled " + (recycled != interp.getResult()));
        sb.append(" ");
        sb.append("refCount " + interp.getResult().getRefCount());
        sb.append(" ");

        // The refCount of recycled should now be 1.
        // The only ref held at this point is the
        // one from this code. The interp result
        // was is released when the new TclObject
        // is set as the interp result. The ref held
        // inside Interp on the recycled object
        // was also released just before the
        // new TclObject was allocated.

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());

        // Deallocated old recycled TclObject
        recycled.release();

        return sb.toString();
    }

    public static String testRecycledDouble3(Interp interp) throws TclException {
        StringBuffer sb = new StringBuffer();

        // Interp result set to empty shared constant
        interp.resetResult();

        // Set interp result to double result that is not
        // a common value.

        interp.setResult(100.0);
        TclObject recycled = interp.getResult();

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Now reset the interp result, this should
        // drop the refCount back down to 1.

        interp.resetResult();

        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Set a new int result that is not a common
        // value. This should use the recycled object
        // again since the refCount is 1. This will
        // also set the interp result.

        interp.setResult(200.0);

        sb.append("was_recycled " + (recycled == interp.getResult()));
        sb.append(" ");
        sb.append("getResult " + interp.getResult().toString());
        sb.append(" ");
        sb.append("refCount " + interp.getResult().getRefCount());

        return sb.toString();
    }

    public static String testRecycledDouble4(Interp interp) throws TclException {
        StringBuffer sb = new StringBuffer();

        interp.resetResult();

        // Get recycled double ref

        interp.setResult(100.0);        
        TclObject recycled = interp.getResult();

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Set var "v" to a double value. This operation
        // will reuse the recycled value and save it
        // in the variable. A side effect of this
        // operation will be to change the object
        // that is current the interp result, but
        // that is ok. If the caller did not want
        // the result object to change then the
        // refCount should have been incremented.

        interp.setVar("v", null, 200.0, 0);

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Reset the interp result, this will drop
        // the ref held by the interp result.

        interp.resetResult();

        sb.append("refCount " + recycled.getRefCount());

        return sb.toString();
    }

    // Recycled String values

    public static String testRecycledString0(Interp interp) throws TclException {
        StringBuffer sb = new StringBuffer();

        // Interp result set to empty shared constant
        interp.resetResult();

        // Set interp result to String result that is not
        // a common value.

        interp.setResult("100");
        TclObject recycled = interp.getResult();

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Now set interp result to another value
        // that is not a common value, this should
        // reuse the recycled TclObject.

        interp.setResult("200");

        // Test that the recycled object was reused
        // by getting the double value out of the
        // original ref. Also check that the
        // interp result is set to the same
        // TclObject.

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");
        sb.append("was_recycled " + (recycled == interp.getResult()));

        return sb.toString();
    }

    public static String testRecycledString1(Interp interp) throws TclException {
        StringBuffer sb = new StringBuffer();

        // Interp result set to empty shared constant
        interp.resetResult();

        // Set interp result to int result that is not
        // a common value.

        interp.setResult("100");
        TclObject recycled = interp.getResult();

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Now increment the ref count of recycled
        // so that it will be shared even after
        // the interp result is reset.

        recycled.preserve();

        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Reset the result, this drops the second ref to
        // recycled but it is still shared because of the
        // preserve call that was just made.

        interp.resetResult();

        sb.append("refCount " + recycled.getRefCount());

        // Release the held ref
        recycled.release();

        return sb.toString();
    }

    public static String testRecycledString2(Interp interp) throws TclException {
        StringBuffer sb = new StringBuffer();

        // Interp result set to empty shared constant
        interp.resetResult();

        // Set interp result to double result that is not
        // a common value.

        interp.setResult("100");
        TclObject recycled = interp.getResult();

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Now increment the ref count of recycled
        // so that it will be shared when the
        // next call to setResult(double) is made.

        recycled.preserve();

        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        interp.setResult("200");

        // The recycled value should have been
        // set to a new TclObject. This new
        // value is then set as the interp
        // result.

        sb.append("new_recycled " + (recycled != interp.getResult()));
        sb.append(" ");
        sb.append("refCount " + interp.getResult().getRefCount());
        sb.append(" ");

        // The refCount of recycled should now be 1.
        // The only ref held at this point is the
        // one from this code. The interp result
        // was is released when the new TclObject
        // is set as the interp result. The ref held
        // inside Interp on the recycled object
        // was also released just before the
        // new TclObject was allocated.

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());

        // Deallocated old recycled TclObject
        recycled.release();

        return sb.toString();
    }

    public static String testRecycledString3(Interp interp) throws TclException {
        StringBuffer sb = new StringBuffer();

        // Interp result set to empty shared constant
        interp.resetResult();

        // Set interp result to double result that is not
        // a common value.

        interp.setResult("100");
        TclObject recycled = interp.getResult();

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Now reset the interp result, this should
        // drop the refCount back down to 1.

        interp.resetResult();

        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Set a new int result that is not a common
        // value. This should use the recycled object
        // again since the refCount is 1. This will
        // also set the interp result.

        interp.setResult("200");

        sb.append("was_recycled " + (recycled == interp.getResult()));
        sb.append(" ");
        sb.append("getResult " + interp.getResult().toString());
        sb.append(" ");
        sb.append("refCount " + interp.getResult().getRefCount());

        return sb.toString();
    }

    public static String testRecycledString4(Interp interp) throws TclException {
        StringBuffer sb = new StringBuffer();

        interp.resetResult();

        // Get recycled double ref

        interp.setResult("100");
        TclObject recycled = interp.getResult();

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Set var "v" to a double value. This operation
        // will reuse the recycled value and save it
        // in the variable. A side effect of this
        // operation will be to change the object
        // that is current the interp result, but
        // that is ok. If the caller did not want
        // the result object to change then the
        // refCount should have been incremented.

        interp.setVar("v", null, "200", 0);

        sb.append("recycled " + recycled.toString());
        sb.append(" ");
        sb.append("refCount " + recycled.getRefCount());
        sb.append(" ");

        // Reset the interp result, this will drop
        // the ref held by the interp result.

        interp.resetResult();

        sb.append("refCount " + recycled.getRefCount());

        return sb.toString();
    }

}

