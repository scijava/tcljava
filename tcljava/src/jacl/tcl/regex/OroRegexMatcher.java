// decompiled from OroRegexMatcher.class
// Source file: OroRegexMatcher.java

package tcl.regex;

import java.lang.*;

import tcl.lang.Interp;

public class  OroRegexMatcher
    implements tcl.lang.RegexMatcher 
{
    public OroRegexMatcher()
    {
    }

    public boolean match(
        Interp interp, 
        String string, 
        String pattern) throws tcl.lang.TclException
    {
        return OroRegexpCmd.match(interp, string, pattern); 
    }

}
