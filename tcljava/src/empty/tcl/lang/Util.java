/*
empty implementation of Util used for compiling in multiple packages
*/

package tcl.lang;

import tcl.lang.Interp;
import tcl.lang.TclException;

public class Util {

static final boolean getBoolean(Interp interp, String s) throws TclException
{
  return true;
}

static final int getInt(Interp interp, String s) throws TclException
{
  return 0;
}

static final double getDouble(Interp interp, String s) throws TclException
{
  return 0.0;
}

static final String printDouble(double number)
{
  return null;
}

static final String getCwd()
{
  return null;
}

static public final boolean
stringMatch(String string, String pattern)
{
  return true;
}

static boolean
isJacl() {
    return false;
}

static boolean
looksLikeInt(String s) {
    return false;
}

static long getWideInt(Interp interp, String str) throws TclException
{
	return 0;
}

} // end Util
