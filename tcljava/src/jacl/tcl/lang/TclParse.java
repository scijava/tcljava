/* 
 * TclParse.java --
 *
 * 	A Class of the following type is filled in by Parser.parseCommand.
 * 	It describes a single command parsed from an input string.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: TclParse.java,v 1.3 2005/10/19 23:37:38 mdejong Exp $
 */

package tcl.lang;

class TclParse {

// The original command string passed to Parser.parseCommand. 

char[] string;		

// Index into 'string' that is the character just after the last 
// one in the command string.

int endIndex;
 
// Index into 'string' that is the # that begins the first of 
// one or more comments preceding the command. 

int commentStart;
 
// Number of bytes in comments (up through newline character 
// that terminates the last comment).  If there were no
// comments, this field is 0.

int commentSize;
 
// Index into 'string' that is the first character in first 
// word of command.

int commandStart;

// Number of bytes in command, including first character of 
// first word, up through the terminating newline, close 
// bracket, or semicolon. 

int commandSize;

// Total number of words in command.  May be 0. 

int numWords;

// Stores the tokens that compose the command.

TclToken[] tokenList;

// Total number of tokens in command. 

int numTokens;

//  Total number of tokens available at token.

int tokensAvailable;	

// One of the parsing error types defined in Parser class.

int errorType;

/*
 *----------------------------------------------------------------------
 *
 * The fields below are intended only for the private use of the
 * parser.  They should not be used by procedures that invoke
 * Tcl_ParseCommand.
 *
 *----------------------------------------------------------------------
 */

// Interpreter to use for error reporting, or null.

Interp interp;

// Name of file from which script came, or null.  Used for error
// messages.

String fileName;

// Line number corresponding to first character in string. 

int lineNum;
 
// Points to character in string that terminated most recent token. 
// Filled in by Parser.parseTokens.  If an error occurs, points to
// beginning of region where the error occurred (e.g. the open brace
// if the close brace is missing).

int termIndex;

// This field is set to true by Parser.parseCommand if the command
// appears to be incomplete.  This information is used by 
// Parser.commandComplete.

boolean incomplete;

// When a TclParse is the return value of a method, result is set to
// a standard Tcl result, indicating the return of the method.


int result;

// Default size of the tokenList array.

private static final int INITIAL_NUM_TOKENS = 20;
  private static final int MAX_CACHED_TOKENS = 50; //my tests show 50 is best


/*
 *----------------------------------------------------------------------
 *
 * TclParse --
 *
 *	Construct a TclParse object with default values.  The interp and
 *	fileName arguments may be null.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */
TclParse(
    Interp interp,		// Interpreter to use for error reporting;
				// if null, then no error message is
				// provided.  Can be null.
    char[] string,		// The command being parsed.
    int endIndex,		// Points to the char after the last valid
				// command character.
    String fileName,		// Name of file being executed, or null.
    int lineNum)		// Line number of file; used for error 
				// messages so it may be invalid.
{
    this.interp = interp;
    this.string = string;
    this.endIndex = endIndex;
    this.fileName = fileName;
    this.lineNum = lineNum;
    this.tokenList = new TclToken[INITIAL_NUM_TOKENS];
    this.tokensAvailable = INITIAL_NUM_TOKENS;
    this.numTokens = 0;
    this.numWords = 0;
    this.commentStart = -1;
    this.commentSize = 0;
    this.commandStart = -1;
    this.commandSize = 0;
    this.termIndex = endIndex;
    this.incomplete = false;
    this.errorType = Parser.TCL_PARSE_SUCCESS;
}

/*
 *----------------------------------------------------------------------
 *
 * getToken --
 *
 *	Gets the token from tokenList at the specified index.  If the 
 * 	index is greater than tokensAvailable, then increase the size 
 *	of tokenList.  If the object at index is null create a new
 * 	TclToken.
 *
 * Results:
 *	Returns the TclToken object referenced by tokenList[index].
 *
 * Side effects:
 *	The tokenList size may be expanded and/or a new TclToken 
 *	created.
 *
 *----------------------------------------------------------------------
 */


TclToken
getToken(
    int index)			// The index into tokenList.
{
    if (index >= tokensAvailable) {
      expandTokenArray(index);
    }

    if (tokenList[index] == null) {
      tokenList[index] = grabToken();
    }
    return tokenList[index];
}


// Release internal resources that this TclParser object might have allocated

void
release() {
  for (int index=0; index < tokensAvailable; index++) {
    if (tokenList[index] != null) {
      releaseToken( tokenList[index] );
      tokenList[index] = null;
    }
  }
}




// Creating an interpreter will cause this init method to be called

static void init(Interp interp) {
  TclToken[] TOKEN_CACHE = new TclToken[MAX_CACHED_TOKENS];
  for (int i=0; i < MAX_CACHED_TOKENS ; i++) {
    TOKEN_CACHE[i] = new TclToken();
  }
  
  interp.parserTokens = TOKEN_CACHE;
  interp.parserTokensUsed = 0;
}


private TclToken grabToken() {
  if (interp == null || interp.parserTokensUsed == MAX_CACHED_TOKENS) {
    // either we do not have a cache because the interp is null or we have already
    // used up all the open cache slots, we just allocate a new one in this case
    return new TclToken();
  } else {
    // the cache has an avaliable slot so grab it
    return interp.parserTokens[interp.parserTokensUsed++];
  }
}

private void releaseToken(TclToken token) {
  if (interp != null && interp.parserTokensUsed > 0) {
    // if cache is not full put the object back in the cache
    interp.parserTokensUsed -= 1;
    interp.parserTokens[interp.parserTokensUsed] = token;
  }
}


/*
//uncommenting these methods will disable caching

static void init(Interp interp) {}
private TclToken grabToken() {return new TclToken();}
private void releaseToken(TclToken token) {}

*/




/*
 *----------------------------------------------------------------------
 *
 * expandTokenArray --
 *
 *	If the number of TclTokens in tokenList exceeds tokensAvailable,
 * 	the double the number number of available tokens, allocate a 
 *	new array, and copy all the TclToken over.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Variable tokensAvailable doubles as well as the size of 
 * 	tokenList.
 *
 *----------------------------------------------------------------------
 */
void
expandTokenArray(int needed)
{
    // Make sure there is at least enough room for needed tokens
    while (needed >= tokensAvailable) {
      tokensAvailable *= 2;
    }
    
    TclToken[] newList = new TclToken[tokensAvailable];
    System.arraycopy(tokenList, 0, newList, 0, tokenList.length);
    tokenList = newList;
}


/*
 *----------------------------------------------------------------------
 *
 * toString --
 *
 *	Generate debug info on the structure of this Class
 *
 * Results:
 *	A String containing the debug info.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public String
toString() 
{
    return (get().toString());
}


/*
 *----------------------------------------------------------------------
 *
 * get --
 *
 *      get a TclObject that has a string representation of this object
 *
 * Results:
 *	|>None.<|
 *
 * Side effects:
 *	|>None.<|
 *
 *----------------------------------------------------------------------
 */

TclObject
get()
{
    TclObject obj;
    TclToken token;
    String typeString;
    int nextIndex;
    String cmd;
    int i;
   

    final boolean debug = false;
    
    if (debug) {
	System.out.println();
	System.out.println("Entered TclParse.get()");
	System.out.println("numTokens is " + numTokens);
    }



    obj = TclList.newInstance();
    try {
	if (commentSize > 0) {
	    TclList.append(interp, obj, 
		    TclString.newInstance(
			new String(string, commentStart, commentSize)));
	} else {
	    TclList.append(interp, obj, TclString.newInstance("-"));
	}
	
	if (commandStart >= (endIndex + 1)) {
	    commandStart = endIndex;
	}
	cmd = new String(string, commandStart, commandSize);
	TclList.append(interp, obj, TclString.newInstance(cmd));
	TclList.append(interp, obj, TclInteger.newInstance(numWords));
	
	for (i = 0; i < numTokens; i++) {
	    if (debug) {
		System.out.println("processing token " + i);
	    }


	    token = tokenList[i];
	    switch (token.type) {
		case Parser.TCL_TOKEN_WORD:
		    typeString = "word";
		    break;
		case Parser.TCL_TOKEN_SIMPLE_WORD:
		    typeString = "simple";
		    break;
		case Parser.TCL_TOKEN_TEXT:
		    typeString = "text";
		    break;
		case Parser.TCL_TOKEN_BS:
		    typeString = "backslash";
		    break;
		case Parser.TCL_TOKEN_COMMAND:
		    typeString = "command";
		    break;
		case Parser.TCL_TOKEN_VARIABLE:
		    typeString = "variable";
		    break;
		default:
		    typeString = "??";
		    break;
	    }

	    if (debug) {
		System.out.println("typeString is " + typeString);
	    }

	    TclList.append(interp, obj, TclString.newInstance(typeString));
	    TclList.append(interp, obj, 
		    TclString.newInstance(token.getTokenString()));
	    TclList.append(interp, obj, 
		    TclInteger.newInstance(token.numComponents));
	}
	nextIndex = commandStart + commandSize;
	TclList.append(interp, obj, TclString.newInstance(
	        new String(string, nextIndex, (endIndex-nextIndex))));

    } catch (TclException e) {
	// Do Nothing.
    }

    return obj;
}
} // end TclParse
