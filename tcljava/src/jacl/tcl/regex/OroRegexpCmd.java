// decompiled from OroRegexpCmd.class
// Source file: OroRegexpCmd.java

package tcl.regex;

import java.lang.*;

import com.oroinc.text.regex.MatchResult;
import com.oroinc.text.regex.Pattern;
import com.oroinc.text.regex.PatternCompiler;
import com.oroinc.text.regex.PatternMatcher;
import com.oroinc.text.regex.Perl5Compiler;
import com.oroinc.text.regex.Perl5Matcher;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclInteger;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclString;

public class  OroRegexpCmd
    implements tcl.lang.Command 
{
    private static final int OPT_INDICES= 0;
    private static final int OPT_NOCASE= 1;
    private static final int OPT_LAST= 2;
    private static final String[] validCmds = {"-indices","-nocase","--"};

    public void cmdProc(
        Interp interp, 
        tcl.lang.TclObject[] argv) throws TclException
    {
        int opt;
        String patternArg;
        Pattern pattern1;
        int objc= argv.length - 1; 
        boolean noCase= false; 
        boolean indices= false; 
        boolean last= false; 
        if (argv.length >= 3)
        {
            int currentObjIndex;
            for (currentObjIndex= 1;  ( !  ( (objc <= 0) ||  (  ! (last == false) || (argv[currentObjIndex].toString().startsWith("-") == false))) ) ; currentObjIndex++)
            {
                opt= TclIndex.get(interp, argv[currentObjIndex], validCmds, "switch", 1); 
                switch (opt)
                {

                    default:
                        throw new TclException(interp, "RegexpCmd.cmdProc: bad option " + opt + " index to validCmds"); 

                    case 0:
                        indices= true; 
                        break;

                    case 1:
                        noCase= true; 
                        break;

                    case 2:
                        last= true; 
                        break;
                }
                objc--; 
            }

            if (objc >= 2)
            {
                patternArg= argv[currentObjIndex].toString(); 
                String origStringArg= argv[(currentObjIndex + 1)].toString(); 
                String stringArg= origStringArg; 
                currentObjIndex += 2; 
                if (noCase)
                {
                    patternArg= patternArg.toLowerCase(); 
                    stringArg= origStringArg.toLowerCase(); 
                }
                Perl5Matcher L11= new Perl5Matcher(); 
                Perl5Compiler L12= new Perl5Compiler(); 
                try
                {
                    PatternCompiler compiler = L12;
                    pattern1= compiler.compile(patternArg); 
                }
                catch (com.oroinc.text.regex.MalformedPatternException e)
                {
		    String msg = e.getMessage();

		    //System.out.println("cmdProc() message is \"" + msg + "\"");

		    if (msg.equals("Unmatched parentheses.")) {
			msg = "unmatched ()";
		    } else if (msg.equals("?+* follows nothing in expression")) {
			msg = "?+* follows nothing";
		    }

		    throw new TclException(interp,
		        "couldn't compile regular expression pattern: " + msg); 
                }
                if (L11.contains(stringArg, pattern1) == false)
                {
                    interp.setResult(0); 
                }
                else
                {
                    MatchResult result= L11.getMatch(); 
                    interp.setResult(1); 
                    int g;
                    for (g= 0; (objc > 2); currentObjIndex++)
                    {
                        if (g >= result.groups())
                        {
                            break;
                        }
                        int start= result.beginOffset(g); 
                        if (start != -1)
                        {
                            int end= result.endOffset(g); 
                            if (indices == false)
                            {
                                setMatchStringVar(interp, argv[currentObjIndex].toString(), start, end, origStringArg); 
                            }
                            else
                            {
                                setMatchVar(interp, argv[currentObjIndex].toString(), start, (end - 1)); 
                            }
                        }
                        else if (indices == false)
                        {
                            setEmptyStringVar(interp, argv[currentObjIndex].toString()); 
                        }
                        else
                        {
                            setMatchVar(interp, argv[currentObjIndex].toString(), -1, -1); 
                        }
                        g++; 
                        objc--; 
                    }

                }
                while (objc > 2)
                {
                    if (indices == false)
                    {
                        setEmptyStringVar(interp, argv[currentObjIndex].toString()); 
                    }
                    else
                    {
                        setMatchVar(interp, argv[currentObjIndex].toString(), -1, -1); 
                    }
                    objc--; 
                    currentObjIndex++; 
                    continue;
                }

                return; 
            }
            throw new TclNumArgsException(interp, 1, argv, "?switches? exp string ?matchVar? ?subMatchVar subMatchVar ...?"); 
        }
        throw new TclNumArgsException(interp, 1, argv, "?switches? exp string ?matchVar? ?subMatchVar subMatchVar ...?"); 
    }

    public static boolean match(
        Interp interp, 
        String stringArg, 
        String patternArg) throws TclException
    {
        Pattern pattern1;
        Perl5Matcher L3= new Perl5Matcher(); 
        Perl5Compiler L4= new Perl5Compiler(); 
        try
        {
            PatternCompiler compiler = L4;
            pattern1= compiler.compile(patternArg); 
        }
        catch (com.oroinc.text.regex.MalformedPatternException e)
        {

	    String msg = e.getMessage();

	    //System.out.println("match() message is \"" + msg + "\"");
	    
	    if (msg.equals("Unmatched parentheses.")) {
		msg = "unmatched ()";
	    } else if (msg.equals("?+* follows nothing in expression")) {
		msg = "?+* follows nothing";
	    }

            throw new TclException(interp,
	        "couldn't compile regular expression pattern: " + msg);
        }
        if  ( (patternArg.length() <= 0) || (patternArg.charAt(0) != 94))
        {
            return L3.matches(stringArg, pattern1); 
        }
        if  ( (L3.contains(stringArg, pattern1) == false) || (L3.getMatch().beginOffset(0) != 0))
        {
            return false; 
        }
        return true; 
    }

    private static void setMatchVar(
        Interp interp, 
        String varName, 
        int start, 
        int end) throws TclException
    {
        try
        {
            TclObject indexPairObj= TclList.newInstance(); 
            TclList.append(interp, indexPairObj, TclInteger.newInstance(start)); 
            TclList.append(interp, indexPairObj, TclInteger.newInstance(end)); 
            interp.setVar(varName, indexPairObj, 0); 
        }
        catch (TclException TclException0)
        {
            throw new TclException(interp, "couldn't set variable \"" + varName + "\""); 
        }
    }

    private static void setMatchStringVar(
        Interp interp, 
        String varName, 
        int start, 
        int end, 
        String valueString) throws TclException
    {
        TclObject valueObj;
        if (start != -1)
        {
            valueObj= TclString.newInstance(valueString.substring(start, end)); 
        }
        else
        {
            valueObj= TclString.newInstance(""); 
        }
        try
        {
            interp.setVar(varName, valueObj, 0); 
        }
        catch (TclException TclException0)
        {
            throw new TclException(interp, "couldn't set variable \"" + varName + "\""); 
        }
    }

    private static void setEmptyStringVar(
        Interp interp, 
        String varName) throws TclException
    {
        try
        {
            interp.setVar(varName, TclString.newInstance(""), 0); 
        }
        catch (TclException TclException0)
        {
            throw new TclException(interp, "couldn't set variable \"" + varName + "\""); 
        }
    }

    public OroRegexpCmd()
    {
    }
}

