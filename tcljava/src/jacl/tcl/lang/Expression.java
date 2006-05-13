/*
 * Expression.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Expression.java,v 1.24 2006/05/13 21:07:15 mdejong Exp $
 *
 */

package tcl.lang;

import java.util.*;

/**
 * This class handles Tcl expressions.
 */
class Expression {

    // The token types are defined below.  In addition, there is a
    // table associating a precedence with each operator.  The order
    // of types is important.  Consult the code before changing it.

    static final int VALUE	 = 0;
    static final int OPEN_PAREN  = 1;
    static final int CLOSE_PAREN = 2;
    static final int COMMA 	 = 3;
    static final int END 	 = 4;
    static final int UNKNOWN	 = 5;

    // Binary operators:

    static final int MULT	 = 8;
    static final int DIVIDE	 = 9;
    static final int MOD	 = 10;
    static final int PLUS	 = 11;
    static final int MINUS	 = 12;
    static final int LEFT_SHIFT	 = 13;
    static final int RIGHT_SHIFT = 14;
    static final int LESS	 = 15;
    static final int GREATER	 = 16;
    static final int LEQ	 = 17;
    static final int GEQ	 = 18;
    static final int EQUAL	 = 19;
    static final int NEQ	 = 20;
    static final int BIT_AND	 = 21;
    static final int BIT_XOR	 = 22;
    static final int BIT_OR	 = 23;
    static final int AND	 = 24;
    static final int OR		 = 25;
    static final int QUESTY	 = 26;
    static final int COLON	 = 27;
    static final int STREQ	 = 28;
    static final int STRNEQ	 = 29;

    // Unary operators:

    static final int UNARY_MINUS = 30;
    static final int UNARY_PLUS  = 31;
    static final int NOT	 = 32;
    static final int BIT_NOT	 = 33;

    // Precedence table.  The values for non-operator token types are ignored.

    static int precTable[] = {
	0, 0, 0, 0, 0, 0, 0, 0,
	12, 12, 12,			// MULT, DIVIDE, MOD
	11, 11,				// PLUS, MINUS
	10, 10,				// LEFT_SHIFT, RIGHT_SHIFT
	9, 9, 9, 9,			// LESS, GREATER, LEQ, GEQ
	8, 8,				// EQUAL, NEQ
	7,				// BIT_AND
	6,				// BIT_XOR
	5,				// BIT_OR
	4,				// AND
	3,				// OR
	2,				// QUESTY
	1,				// COLON
	8, 8,    			// STREQ, STRNEQ
	13, 13, 13, 13			// UNARY_MINUS, UNARY_PLUS, NOT,
					// BIT_NOT
    };

    // Mapping from operator numbers to strings;  used for error messages.

    static String operatorStrings[] = {
        "VALUE", "(", ")", ",", "END", "UNKNOWN", "6", "7",
	"*", "/", "%", "+", "-", "<<", ">>", "<", ">", "<=",
	">=", "==", "!=", "&", "^", "|", "&&", "||", "?", ":", "eq", "ne",
	"-", "+", "!", "~"
    };

    HashMap mathFuncTable;

    /**
     * The entire expression, as originally passed to eval et al.
     */
    private String m_expr;

    /**
     * Length of the expression.
     */
    private int m_len;

    /**
     * Type of the last token to be parsed from the expression.
     * Corresponds to the characters just before expr.
     */
    int m_token;

    /**
     * Position to the next character to be scanned from the expression
     * string.
     */
    private int m_ind;

    /**
     * Cache of ExprValue objects. These are cached on a per-interp
     * basis to speed up most expressions.
     */

    private ExprValue[] cachedExprValue;
    private int cachedExprIndex = 0;
    private static final int cachedExprLength = 20;

    /**
     * Evaluate a Tcl expression and set the interp result to the value.
     *
     * @param interp the context in which to evaluate the expression.
     * @param string expression to evaluate.
     * @return the value of the expression.
     * @exception TclException for malformed expressions.
     */

    void evalSetResult(Interp interp, String string)
	    throws TclException {
	TclObject obj;
	ExprValue value = ExprTopLevel(interp, string);
	switch (value.getType()) {
	case ExprValue.INT:
	    interp.setResult( value.getIntValue() );
	    break;
	case ExprValue.DOUBLE:
	    interp.setResult( value.getDoubleValue() );
	    break;
	case ExprValue.STRING:
	    interp.setResult( value.getStringValue() );
	    break;
	default:
	    throw new TclRuntimeError("internal error: expression, unknown");
	}
	releaseExprValue(value);
	return;
    }

    /**
     * Evaluate an Tcl expression.
     * @param interp the context in which to evaluate the expression.
     * @param string expression to evaluate.
     * @exception TclException for malformed expressions.
     * @return the value of the expression in boolean.
     */
    boolean evalBoolean(Interp interp, String string)
	    throws TclException {
	boolean b;
	ExprValue value = ExprTopLevel(interp, string);
	switch (value.getType()) {
	case ExprValue.INT:
	    b = (value.getIntValue() != 0);
	    break;
	case ExprValue.DOUBLE:
	    b = (value.getDoubleValue() != 0.0);
	    break;
	case ExprValue.STRING:
	    b = Util.getBoolean(interp, value.getStringValue());
	    break;
	default:
	    throw new TclRuntimeError("internal error: expression, unknown");
	}
	releaseExprValue(value);
	return b;
    }

    /**
     * Constructor.
     */
    Expression() {
	mathFuncTable = new HashMap();

	// rand  -- needs testing
	// srand -- needs testing
	// hypot -- needs testing
	// fmod  -- needs testing
	//              try [expr fmod(4.67, 2.2)]
	//              the answer should be .27, but I got .2699999999999996

	registerMathFunction("atan2", new Atan2Function());
	registerMathFunction("pow", new PowFunction());
	registerMathFunction("acos", new AcosFunction());
	registerMathFunction("asin", new AsinFunction());
	registerMathFunction("atan", new AtanFunction());
	registerMathFunction("ceil", new CeilFunction());
	registerMathFunction("cos", new CosFunction());
	registerMathFunction("cosh", new CoshFunction());
	registerMathFunction("exp", new ExpFunction());
	registerMathFunction("floor", new FloorFunction());
	registerMathFunction("fmod", new FmodFunction());
	registerMathFunction("hypot", new HypotFunction());
	registerMathFunction("log", new LogFunction());
	registerMathFunction("log10", new Log10Function());
	registerMathFunction("rand", new RandFunction());
	registerMathFunction("sin", new SinFunction());
	registerMathFunction("sinh", new SinhFunction());
	registerMathFunction("sqrt", new SqrtFunction());
	registerMathFunction("srand", new SrandFunction());
	registerMathFunction("tan", new TanFunction());
	registerMathFunction("tanh", new TanhFunction());

	registerMathFunction("abs", new AbsFunction());
	registerMathFunction("double", new DoubleFunction());
	registerMathFunction("int", new IntFunction());
	registerMathFunction("round", new RoundFunction());

	m_expr = null;
	m_ind = 0;
	m_len = 0;
	m_token = UNKNOWN;

	cachedExprValue = new ExprValue[cachedExprLength];
	for (int i=0; i < cachedExprLength; i++) {
	    cachedExprValue[i] = new ExprValue(0, null);
	}
    }

    /**
     * Provides top-level functionality shared by procedures like ExprInt,
     * ExprDouble, etc.
     * @param interp the context in which to evaluate the expression.
     * @param string the expression.
     * @exception TclException for malformed expressions.
     * @return the value of the expression.
     */
    private final ExprValue ExprTopLevel(Interp interp, String string)
	    throws TclException {

	// Saved the state variables so that recursive calls to expr
	// can work:
	//	expr {[expr 1+2] + 3}

        String m_expr_saved = m_expr;
	int m_len_saved     = m_len;
	int m_token_saved   = m_token;
	int m_ind_saved     = m_ind;

	try {
	    m_expr = string;
	    m_ind = 0;
	    m_len = string.length();
	    m_token = UNKNOWN;

	    ExprValue val = ExprGetValue(interp, -1);
	    if (m_token != END) {
		SyntaxError(interp);
	    }
	    return val;
	} finally {
	    m_expr  = m_expr_saved;
	    m_len   = m_len_saved;
	    m_token = m_token_saved;
	    m_ind   = m_ind_saved;
	}
    }

    static void IllegalType(Interp interp, int badType, int operator)
	    throws TclException {
	throw new TclException(interp, "can't use " +
		((badType == ExprValue.DOUBLE) ?
		 "floating-point value" : "non-numeric string") +
	    	" as operand of \"" + operatorStrings[operator]+ "\"");
    }

    void SyntaxError(Interp interp) throws TclException {
	throw new TclException(interp, "syntax error in expression \"" +
	    m_expr + "\"");
    }

    static void DivideByZero(Interp interp) throws TclException {
	interp.setErrorCode(TclString.newInstance(
		"ARITH DIVZERO {divide by zero}"));
	throw new TclException(interp, "divide by zero");
    }

    static void IntegerTooLarge(Interp interp) throws TclException {
	interp.setErrorCode(TclString.newInstance(
		"ARITH IOVERFLOW {integer value too large to represent}"));
	throw new TclException(interp, "integer value too large to represent");
    }

    static void DoubleTooLarge(Interp interp) throws TclException {
	interp.setErrorCode(TclString.newInstance(		
	   "ARITH OVERFLOW {floating-point value too large to represent}"));
	throw new TclException(interp,
		"floating-point value too large to represent");
    }

    static void DoubleTooSmall(Interp interp) throws TclException {
	interp.setErrorCode(TclString.newInstance(		
	   "ARITH UNDERFLOW {floating-point value too small to represent}"));
	throw new TclException(interp,
		"floating-point value too small to represent");
    }

    static void DomainError(Interp interp) throws TclException {
	interp.setErrorCode(TclString.newInstance(
		"ARITH DOMAIN {domain error: argument not in valid range}"));
	throw new TclException(interp,
		"domain error: argument not in valid range");
    }

    static void EmptyStringOperandError(Interp interp, int operator)
        throws TclException
    {
	throw new TclException(interp, "can't use " +
		"empty string" +
	    	" as operand of \"" + operatorStrings[operator]+ "\"");
    }

    /**
     * Given a TclObject, such as the result of a command or
     * variable evaluation, fill in a ExprValue with the
     * parsed result. If the TclObject already has an
     * internal rep that is a numeric type, then no need to
     * parse from the string rep. If the string rep is parsed
     * into a numeric type, then update the internal rep
     * of the object to the parsed value.
     */

    static void
    ExprParseObject(Interp interp, TclObject obj, ExprValue value)
	    throws TclException
    {
        // If the TclObject already has an integer, boolean,
        // or floating point internal representation, use it.

        InternalRep rep = obj.getInternalRep();

        if (rep instanceof TclInteger) {
            // If the object is a "pure" number, meaning it
            // was created from a primitive type and there
            // is no string rep, then generate a string
            // from the primitive type later on, if needed.

            value.setIntValue(TclInteger.get(interp, obj),
                (obj.hasNoStringRep() ? null : obj.toString()));
            return;
        } else if (rep instanceof TclBoolean) {
            // A "pure" boolean created from a primitive
            // type can be treated as an integer value.
            // If the boolean has a string rep, then
            // check for the special cases of "0" or "1".
            // Otherwise, treat the object as a string
            // value since the expr code can convert
            // to a boolean as needed.

            if (obj.hasNoStringRep()) {
                boolean bval = TclBoolean.get(interp, obj);
                value.setIntValue(bval);
                return;
            } else {
                String srep = obj.toString();
                int slen = srep.length();
                if (slen == 1 && srep.charAt(0) == '0') {
                    value.setIntValue(0);
                    return;
                } else if (slen == 1 && srep.charAt(0) == '1') {
                    value.setIntValue(1);
                    return;
                } else if (slen == 4 && srep.equals("true")) {
                    value.setStringValue("true");
                    return;
                } else if (slen == 5 && srep.equals("false")) {
                    value.setStringValue("false");
                    return;
                }
            }
        } else if (rep instanceof TclDouble) {
            // An object with a double internal rep might
            // actually be an integer that was converted
            // to a double. Check to see if the double
            // looks like an integer to handle this case.
            // A "pure" double with no string rep is
            // always valid.

            double dval = TclDouble.get(interp, obj);
            if (obj.hasNoStringRep()) {
                value.setDoubleValue(dval);
                return;
            }
            String str = obj.toString();
            if (looksLikeInt(str, str.length(), 0, true)) {
                // Convert a double that looks like an
                // integer back into an integer. Note
                // that the integer must be reparsed
                // from the string rep so that an octal
                // like "040" is handled correctly.

                int ival = TclInteger.get(interp, obj);
                value.setIntValue(ival, str);
                return;
            } else {
                value.setDoubleValue(dval, str);
                return;
            }
        }

        // Otherwise, try to parse a numeric value from the
        // object's string rep.

        ExprParseString(interp, obj, value);

        return;
    }

    /**
     * TclParseNumber -> ExprParseString
     *
     * Given a TclObject that contains a String to be parsed (from
     * a command or variable subst), fill in an ExprValue based on
     * the string's numeric value. The value may be a floating-point,
     * an integer, or a string. If the string value is converted to
     * a numeric value, then update the internal rep of the TclObject.
     *
     * @param interp the context in which to evaluate the expression.
     * @param obj the TclObject containing the string to parse.
     * @param value the ExprValue object to save the parsed value in.
     */

    private static void
    ExprParseString(Interp interp, TclObject obj, ExprValue value) {
	char c;
	int ival;
	double dval;
	String s = obj.toString();
	int len = s.length();

	//System.out.println("now to ExprParseString ->" + s +
	//	 "<- of length " + len);

	if (len == 0) {
	    // Take shortcut when string is of length 0, as the
	    // empty string can't represent an int, double, or boolean.

	    value.setStringValue("");
	    return;
	} else if (len == 1) {
	    // Check for really common strings of length 1
	    // that we know will be integers.

	    c = s.charAt(0);
	    switch (c) {
	        case '0':
	        case '1':
	        case '2':
	        case '3':
	        case '4':
	        case '5':
	        case '6':
	        case '7':
	        case '8':
	        case '9':
	            ival = (int) (c - '0');
	            value.setIntValue(ival, s);
	            TclInteger.exprSetInternalRep(obj, ival);
	            return;
	        default:
	            // We know this string can't be parsed
	            // as a number, so just treat it as
	            // a string. A string of length 1 is
	            // very common.

	            value.setStringValue(s);
	            return;
	    }
	} else if (len == 2) {
	    // Check for really common strings of length 2
	    // that we know will be integers.

	    c = s.charAt(0);
	    if (c == '-') {
	        c = s.charAt(1);
	        switch (c) {
	            case '0':
	            case '1':
	            case '2':
	            case '3':
	            case '4':
	            case '5':
	            case '6':
	            case '7':
	            case '8':
	            case '9':
	                ival = (int) -(c - '0');
	                value.setIntValue(ival, s);
	                TclInteger.exprSetInternalRep(obj, ival);
	                return;
	        }
	    }
	} else if (len == 3) {
	    // Check for really common strings of length 3
	    // that we know will be doubles.

	    c = s.charAt(1);
	    if (c == '.') {
	        if (s.equals("0.0")) {
	            dval = 0.0;
	            value.setDoubleValue(dval, s);
	            TclDouble.exprSetInternalRep(obj, dval);
	            return;
	        } else if (s.equals("0.5")) {
	            dval = 0.5;
	            value.setDoubleValue(dval, s);
	            TclDouble.exprSetInternalRep(obj, dval);
	            return;
	        } else if (s.equals("1.0")) {
	            dval = 1.0;
	            value.setDoubleValue(dval, s);
	            TclDouble.exprSetInternalRep(obj, dval);
	            return;
	        } else if (s.equals("2.0")) {
	            dval = 2.0;
	            value.setDoubleValue(dval, s);
	            TclDouble.exprSetInternalRep(obj, dval);
	            return;
	        }
	    }
	}

	if (looksLikeInt(s, len, 0, false)) {
	    //System.out.println("string looks like an int");

	    // Note: the Util.strtoul() method handles 32bit unsigned values
	    // as well as leading sign characters.

	    StrtoulResult res = interp.strtoulResult;
	    Util.strtoul(s, 0, 0, res);
	    //String token = s.substring(i, res.index);
	    //System.out.println("token string from strtoul is \"" + token + "\"");
	    //System.out.println("res.errno is " + res.errno);

            if (res.errno == 0) {
		// We treat this string as a number if all the charcters
		// following the parsed number are a whitespace chars.
		// E.g.: " 1", "1", "1 ", and " 1 "  are all good numbers

                boolean trailing_blanks = true;

	        for (int i = res.index; i < len ; i++) {
                    if ((c = s.charAt(i)) != ' ' &&
                            !Character.isWhitespace(c)) {
                        trailing_blanks = false;
                    }
	        }

                if (trailing_blanks) {
		    ival = (int) res.value;
	            //System.out.println("string is an Integer of value " + ival);
		    value.setIntValue(ival, s);
		    TclInteger.exprSetInternalRep(obj, ival);
		    return;
                } else {
		    //System.out.println("string failed trailing_blanks test, not an integer");
                }
            }
	} else {
	    //System.out.println("string does not look like an int, checking for Double");

	    StrtodResult res = interp.strtodResult;
	    Util.strtod(s, 0, res);

            if (res.errno == 0) {
		// Trailing whitespaces are treated just like the Integer case

                boolean trailing_blanks = true;

	        for (int i = res.index; i < len ; i++) {
                    if ((c = s.charAt(i)) != ' ' &&
                            !Character.isWhitespace(c)) {
                        trailing_blanks = false;
                    }
	        }

                if (trailing_blanks) {
		    dval = res.value;
	            //System.out.println("string is a Double of value " + dval);
		    value.setDoubleValue(dval, s);
		    TclDouble.exprSetInternalRep(obj, dval);
		    return;
                }

            }
	}

	//System.out.println("string is not a valid number, returning as string \"" + s + "\"");

	// Not a valid number.  Save a string value (but don't do anything
	// if it's already the value).

 	value.setStringValue(s);
 	return;
    }

    /**
     * Parse a "value" from the remainder of the expression.
     *
     * @param interp the context in which to evaluate the expression.
     * @param prec treat any un-parenthesized operator with precedence
     *     <= this as the end of the expression.
     * @exception TclException for malformed expressions.
     * @return the value of the expression.
     */
    private ExprValue ExprGetValue(Interp interp, int prec)
	    throws TclException {
	int operator;
	boolean gotOp = false;		// True means already lexed the
					// operator (while picking up value
					// for unary operator).  Don't lex
					// again.
	ExprValue value, value2 = null;

	// There are two phases to this procedure.  First, pick off an
	// initial value.  Then, parse (binary operator, value) pairs
	// until done.

	value = ExprLex(interp);

	if (m_token == OPEN_PAREN) {

	    // Parenthesized sub-expression.

	    value = ExprGetValue(interp, -1);
	    if (m_token != CLOSE_PAREN) {
		SyntaxError(interp);
	    }
	} else {
	    if (m_token == MINUS) {
		m_token = UNARY_MINUS;
	    }
	    if (m_token == PLUS) {
		m_token = UNARY_PLUS;
	    }
	    if (m_token >= UNARY_MINUS) {

		// Process unary operators.

		operator = m_token;
		value = ExprGetValue(interp, precTable[m_token]);

		if (interp.noEval == 0) {
		    evalUnaryOperator(interp, operator, value);
		}
		gotOp = true;
	    } else if (m_token == CLOSE_PAREN) {
	        // Caller needs to deal with close paren token.
	        return null;
	    } else if (m_token != VALUE) {
		SyntaxError(interp);
	    }
	}
	if (value == null) {
	    SyntaxError(interp);
	}

	// Got the first operand.  Now fetch (operator, operand) pairs.

	if (!gotOp) {
	    value2 = ExprLex(interp);
	}

	while (true) {
	    operator = m_token;
	    if ((operator < MULT) || (operator >= UNARY_MINUS)) {
		if ((operator == END) || (operator == CLOSE_PAREN)
		        || (operator == COMMA)) {
		    return value; // Goto Done
		} else {
		    SyntaxError(interp);
		}
	    }
	    if (precTable[operator] <= prec) {
		return value;	// (goto done)
	    }

	    // If we're doing an AND or OR and the first operand already
	    // determines the result, don't execute anything in the
	    // second operand:  just parse.  Same style for ?: pairs.

	    if ((operator == AND) || (operator == OR) ||(operator == QUESTY)){

		if (value.isDoubleType()) {
		    value.setIntValue( value.getDoubleValue() != 0.0 );
		} else if (value.isStringType()) {
                   try {
                       boolean b = Util.getBoolean(interp, value.getStringValue());
                       value.setIntValue(b);
                   } catch (TclException e) {
                       if (interp.noEval == 0) {
                           throw e;
                       }

                       // Must set value.intValue to avoid referencing
                       // uninitialized memory in the "if" below;  the actual
                       // value doesn't matter, since it will be ignored.

                       value.setIntValue(0);
                   }
		}
		if (((operator == AND) && (value.getIntValue() == 0))
		        || ((operator == OR) && (value.getIntValue() != 0))) {
		    interp.noEval ++;
		    try {
			value2 = ExprGetValue(interp, precTable[operator]);
		    } finally {
			interp.noEval--;
		    }
		    if (operator == OR) {
			value.setIntValue(1);
		    }
		    continue;
		} else if (operator == QUESTY) {
		    // Special note:  ?: operators must associate right to
		    // left.  To make this happen, use a precedence one lower
		    // than QUESTY when calling ExprGetValue recursively.

		    if (value.getIntValue() != 0) {
			value = ExprGetValue(interp, precTable[QUESTY] - 1);
			if (m_token != COLON) {
			    SyntaxError(interp);
			}

			interp.noEval++;
			try {
			    value2 = ExprGetValue(interp, precTable[QUESTY]-1);
			} finally {
			    interp.noEval--;
			}
		    } else {
			interp.noEval++;
			try {
			    value2 = ExprGetValue(interp, precTable[QUESTY]-1);
			} finally {
			    interp.noEval--;
			}
			if (m_token != COLON) {
			    SyntaxError(interp);
			}
			value = ExprGetValue(interp, precTable[QUESTY] - 1);
		    }
		    continue;
		} else {
		    value2 = ExprGetValue(interp, precTable[operator]);
		}
	    } else {
		value2 = ExprGetValue(interp, precTable[operator]);
	    }

	    if (value2 == null) {
		SyntaxError(interp);
	    }

	    if ((m_token < MULT) && (m_token != VALUE)
		    && (m_token != END) && (m_token != COMMA)
		    && (m_token != CLOSE_PAREN)) {
		SyntaxError(interp);
	    }

	    if (interp.noEval != 0) {
		continue;
	    }

	    if (operator == COLON) {
	        SyntaxError(interp);
	    }
	    evalBinaryOperator(interp, operator, value, value2);
	    releaseExprValue(value2);
        } // end of while(true) loop
    }

    // Evaluate the result of a unary operator ( - + ! ~)
    // when it is applied to a value. The passed in value
    // contains the result.

    static
    void evalUnaryOperator(
        Interp interp,
        int operator,
        ExprValue value)
            throws TclException
    {
        switch (operator) {
	    case UNARY_MINUS:
		if (value.isIntType()) {
		    value.setIntValue( value.getIntValue() * -1 );
		} else if (value.isDoubleType()) {
		    value.setDoubleValue( value.getDoubleValue() * -1.0 );
		} else {
		    IllegalType(interp, value.getType(), operator);
		}
		break;
	    case UNARY_PLUS:
		if (!value.isIntOrDoubleType()) {
		    IllegalType(interp, value.getType(), operator);
		}
		// Unary + operator on for numeric type is a no-op
		break;
	    case NOT:
		if (value.isIntType()) {
		    value.setIntValue( value.getIntValue() == 0 );
		} else if (value.isDoubleType()) {
		    value.setIntValue( value.getDoubleValue() == 0.0 );
		} else if (value.isStringType()) {
		    String s = value.getStringValue();
		    int s_len = s.length();
		    if ( s_len == 0 ) {
		        EmptyStringOperandError(interp, operator);
		    }
		    String tok = getBooleanToken(s);
		    // Reject a string like "truea"
		    if ( tok != null && tok.length() == s_len) {
		        if ( "true".startsWith(tok) ||
		                "on".startsWith(tok) ||
		                "yes".startsWith(tok) ) {
		            value.setIntValue(0);
		        } else {
		            value.setIntValue(1);
		        }
		    } else {
		        IllegalType(interp, value.getType(), operator);
		    }
		} else {
		    IllegalType(interp, value.getType(), operator);
		}
		break;
	    case BIT_NOT:
		if (value.isIntType()) {
		    value.setIntValue( ~ value.getIntValue() );
		} else {
		    IllegalType(interp, value.getType(), operator);
		}
		break;
	    default:
		throw new TclException(interp,
			"unknown operator in expression");
        }
    }

    // Evaluate the result of a binary operator (* / + - % << >> ...)
    // when applied to a pair of values. The result is returned in
    // the first (left hand) value. This method will check data
    // types and perform conversions if needed before executing
    // the operation. The value2 argument (right hand) value will
    // not be released, the caller should release it.

    static
    void evalBinaryOperator(
        Interp interp,
        int operator,
        ExprValue value,    // value on left hand side
        ExprValue value2)   // value on right hand side
            throws TclException
    {
	switch (operator) {
	    // For the operators below, no strings are allowed and
	    // ints get converted to floats if necessary.

	    case MULT: case DIVIDE: case PLUS: case MINUS:
		if (value.isStringType() || value2.isStringType()) {
		    if ((value.getStringValue().length() == 0) ||
		        (value2.getStringValue().length() == 0)) {
		        EmptyStringOperandError(interp, operator);
		    }
		    IllegalType(interp, ExprValue.STRING, operator);
		}
		if (value.isDoubleType()) {
		    if (value2.isIntType()) {
			value2.setDoubleValue( (double) value2.getIntValue() );
		    }
		} else if (value2.isDoubleType()) {
		    if (value.isIntType()) {
			value.setDoubleValue( (double) value.getIntValue() );
		    }
		}
		break;

	    // For the operators below, only integers are allowed.

	    case MOD: case LEFT_SHIFT: case RIGHT_SHIFT:
	    case BIT_AND: case BIT_XOR: case BIT_OR:
		 if (value.getType() != ExprValue.INT) {
		     if (value.getStringValue().length() == 0) {
		         EmptyStringOperandError(interp, operator);
		     }
		     IllegalType(interp, value.getType(), operator);
		 } else if (value2.getType() != ExprValue.INT) {
		     if (value2.getStringValue().length() == 0) {
		         EmptyStringOperandError(interp, operator);
		     }
		     IllegalType(interp, value2.getType(), operator);
		 }

		 break;

	    // For the operators below, any type is allowed but the
	    // two operands must have the same type.  Convert integers
	    // to floats and either to strings, if necessary.

	    case LESS: case GREATER: case LEQ: case GEQ:
	    case EQUAL: case NEQ:
		if (value.getType() == value2.getType()) {
		    // No-op, both operators are already the same type
		} else if (value.isStringType()) {
		    if (!value2.isStringType()) {
			ExprMakeString(interp, value2);
		    }
		} else if (value2.isStringType()) {
		    if (!value.isStringType()) {
			ExprMakeString(interp, value);
		    }
		} else if (value.isDoubleType()) {
		    if (value2.isIntType()) {
			value2.setDoubleValue( (double) value2.getIntValue() );
		    }
		} else if (value2.isDoubleType()) {
		     if (value.isIntType()) {
			value.setDoubleValue( (double) value.getIntValue() );
		    }
		}
		break;

	    // For the 2 operators below, string comparison is always
            // done.

	    case STREQ: case STRNEQ:
		// No-op
		break;

	    // For the operators below, no strings are allowed, but
	    // no int->double conversions are performed.

	    case AND: case OR:
		if (value.isStringType()) {
		    IllegalType(interp, value.getType(), operator);
		}
		if (value2.isStringType()) {
		    boolean b = Util.getBoolean(interp, value2.getStringValue());
		    value2.setIntValue(b);
		}
		break;

	    // For the operators below, type and conversions are
	    // irrelevant:  they're handled elsewhere.

	    case QUESTY: case COLON:
		break;

	    // Any other operator is an error.

	    default:
		throw new TclException(interp,
			"unknown operator in expression");
	}

	// Carry out the function of the specified operator.

	switch (operator) {
	    case MULT:
		if (value.isIntType()) {
		    value.setIntValue( value.getIntValue() * value2.getIntValue() );
		} else {
		    value.setDoubleValue( value.getDoubleValue() * value2.getDoubleValue() );
		}
		break;
	    case DIVIDE:
		if (value.isIntType()) {
		    int dividend, divisor, quotient;

		    if (value2.getIntValue() == 0) {
		        DivideByZero(interp);
		    }

		    // quotient  = dividend / divisor
		    //
		    // When performing integer division, protect
		    // against integer overflow. Round towards zero
		    // when the quotient is positive, otherwise
		    // round towards -Infinity.

		    dividend = value.getIntValue();
		    divisor = value2.getIntValue();

		    if (dividend == Integer.MIN_VALUE && divisor == -1) {
		        // Avoid integer overflow on (Integer.MIN_VALUE / -1)
		        quotient = Integer.MIN_VALUE;
		    } else {
		        quotient = dividend / divisor;
		        // Round down to a smaller negative number if
		        // there is a remainder and the quotient is
		        // negative or zero and the signs don't match.
		        if (((quotient < 0) ||
		                ((quotient == 0) &&
		                    (((dividend < 0) && (divisor > 0)) ||
		                    ((dividend > 0) && (divisor < 0))))) &&
		                ((quotient * divisor) != dividend)) {
		            quotient -= 1;
		        }
		    }
		    value.setIntValue(quotient);
		} else {
		    double divisor = value2.getDoubleValue();
		    if (divisor == 0.0) {
			DivideByZero(interp);
		    } 
		    value.setDoubleValue(value.getDoubleValue() / divisor);
		}
		break;
	    case MOD:
		int dividend, divisor, remainder;
		boolean neg_divisor = false;

		if (value2.getIntValue() == 0) {
		    DivideByZero(interp);
		}

		// remainder = dividend % divisor
		//
		// In Tcl, the sign of the remainder must match
		// the sign of the divisor. The absolute value of
		// the remainder must be smaller than the divisor.
		//
		// In Java, the remainder can be negative only if
		// the dividend is negative. The remainder will
		// always be smaller than the divisor.
		//
		// See: http://mindprod.com/jgloss/modulus.html

		dividend = value.getIntValue();
		divisor = value2.getIntValue();

		if ( dividend == Integer.MIN_VALUE && divisor == -1 ) {
		    // Avoid integer overflow on (Integer.MIN_VALUE % -1)
		    remainder = 0;
		} else {
		    if (divisor < 0) {
		        divisor = -divisor;
		        dividend = -dividend; // Note: -Integer.MIN_VALUE == Integer.MIN_VALUE
		        neg_divisor = true;
		    }
		    remainder = dividend % divisor;

		    // remainder is (remainder + divisor) when the
		    // remainder is negative. Watch out for the
		    // special case of a Integer.MIN_VALUE dividend
		    // and a negative divisor. Don't add the divisor
		    // in that case because the remainder should
		    // not be negative.

		    if (remainder < 0 && !(neg_divisor && (dividend == Integer.MIN_VALUE))) {
		        remainder += divisor;
		    }
		}
		if ((neg_divisor && (remainder > 0)) ||
		        (!neg_divisor && (remainder < 0))) {
		    remainder = -remainder;
		}
		value.setIntValue(remainder);
		break;
	    case PLUS:
		if (value.isIntType()) {
		    value.setIntValue( value.getIntValue() + value2.getIntValue() );
		} else {
		    value.setDoubleValue( value.getDoubleValue() + value2.getDoubleValue() );
		}
		break;
	    case MINUS:
		if (value.isIntType()) {
		    value.setIntValue( value.getIntValue() - value2.getIntValue() );
		} else {
		    value.setDoubleValue( value.getDoubleValue() - value2.getDoubleValue() );
		}
		break;
	    case LEFT_SHIFT:
		// In Java, a left shift operation will shift bits from 0
		// to 31 places to the left. For an int left operand
		// the right operand value is implicitly (value & 0x1f),
		// so a negative shift amount is in the 0 to 31 range.

		int left_shift_num = value.getIntValue();
		int left_shift_by = value2.getIntValue();
		if (left_shift_by >= 32) {
		    left_shift_num = 0;
		} else {
		    left_shift_num <<= left_shift_by;
		}
		value.setIntValue(left_shift_num);
		break;
	    case RIGHT_SHIFT:
		// In Java, a right shift operation will shift bits from 0
		// to 31 places to the right and propagate the sign bit.
		// For an int left operand, the right operand is implicitly
		// (value & 0x1f), so a negative shift is in the 0 to 31 range.

		int right_shift_num = value.getIntValue();
		int right_shift_by = value2.getIntValue();
		if (right_shift_by >= 32) {
		    if (right_shift_num < 0) {
		        right_shift_num = -1;
		    } else {
		        right_shift_num = 0;
		    }
		} else {
		    right_shift_num >>= right_shift_by;
		}
		value.setIntValue(right_shift_num);
		break;
	    case LESS:
		if (value.isIntType()) {
		    value.setIntValue( value.getIntValue() < value2.getIntValue() );
		} else if (value.isDoubleType()) {
		    value.setIntValue( value.getDoubleValue() < value2.getDoubleValue() );
		} else {
		    value.setIntValue( value.getStringValue().compareTo(
			    value2.getStringValue()) < 0 );
		}
		break;
	    case GREATER:
		if (value.isIntType()) {
		    value.setIntValue( value.getIntValue() > value2.getIntValue() );
		} else if (value.isDoubleType()) {
		    value.setIntValue( value.getDoubleValue() > value2.getDoubleValue() );
		} else {
		    value.setIntValue( value.getStringValue().compareTo(
			    value2.getStringValue()) > 0 );
		}
		break;
	    case LEQ:
		if (value.isIntType()) {
		    value.setIntValue( value.getIntValue() <= value2.getIntValue() );
		} else if (value.isDoubleType()) {
		    value.setIntValue( value.getDoubleValue() <= value2.getDoubleValue() );
		} else {
		    value.setIntValue( value.getStringValue().compareTo(
			    value2.getStringValue()) <= 0 );
		}
		break;
	    case GEQ:
		if (value.isIntType()) {
		    value.setIntValue( value.getIntValue() >= value2.getIntValue() );
		} else if (value.isDoubleType()) {
		    value.setIntValue( value.getDoubleValue() >= value2.getDoubleValue() );
		} else {
		    value.setIntValue( value.getStringValue().compareTo(
			    value2.getStringValue()) >= 0 );
		}
		break;
	    case EQUAL:
		if (value.isIntType()) {
		    value.setIntValue( value.getIntValue() == value2.getIntValue() );
		} else if (value.isDoubleType()) {
		    value.setIntValue( value.getDoubleValue() == value2.getDoubleValue() );
		} else {
		    value.setIntValue( value.getStringValue().equals(
			    value2.getStringValue()) );
		}
		break;
	    case NEQ:
		if (value.isIntType()) {
		    value.setIntValue( value.getIntValue() != value2.getIntValue() );
		} else if (value.isDoubleType()) {
		    value.setIntValue( value.getDoubleValue() != value2.getDoubleValue() );
		} else {
		    value.setIntValue( ! value.getStringValue().equals(
			    value2.getStringValue()) );
		}
		break;
	    case STREQ:
		// Will compare original String values from token or variable
		value.setIntValue( value.getStringValue().equals(
		    value2.getStringValue()) );
		break;
	    case STRNEQ:
		value.setIntValue( ! value.getStringValue().equals(
		    value2.getStringValue()) );
		break;
	    case BIT_AND:
		value.setIntValue( value.getIntValue() & value2.getIntValue() );
		break;
	    case BIT_XOR:
		value.setIntValue( value.getIntValue() ^ value2.getIntValue() );
		break;
	    case BIT_OR:
		value.setIntValue( value.getIntValue() | value2.getIntValue() );
		break;

	    // For AND and OR, we know that the first value has already
	    // been converted to an integer.  Thus we need only consider
	    // the possibility of int vs. double for the second value.

	    case AND:
		if (value2.isDoubleType()) {
		    value2.setIntValue( value2.getDoubleValue() != 0.0 );
		}
		value.setIntValue(
			((value.getIntValue()!=0) && (value2.getIntValue()!=0)) );
		break;
	    case OR:
		if (value2.isDoubleType()) {
		    value2.setIntValue( value2.getDoubleValue() != 0.0 );
		}
		value.setIntValue(
			((value.getIntValue()!=0) || (value2.getIntValue()!=0)) );
		break;

	}

	return;
    }

    /**
     * GetLexeme -> ExprLex
     *
     * Lexical analyzer for expression parser:  parses a single value,
     * operator, or other syntactic element from an expression string.
     *
     * Size effects: the "m_token" member variable is set to the value of
     *		     the current token.
     *
     * @param interp the context in which to evaluate the expression.
     * @exception TclException for malformed expressions.
     * @return the value of the expression.
     */
    private ExprValue ExprLex(Interp interp) throws TclException {
	char c, c2;

	while (m_ind < m_len && (((c = m_expr.charAt(m_ind)) == ' ') ||
                Character.isWhitespace(c))) {
	    m_ind++;
	}
	if (m_ind >= m_len) {
	    m_token = END;
	    return null;
	}

	// First try to parse the token as an integer or
	// floating-point number.  Don't want to check for a number if
	// the first character is "+" or "-".  If we do, we might
	// treat a binary operator as unary by
	// mistake, which will eventually cause a syntax error.

	c = m_expr.charAt(m_ind);
	if (m_ind < m_len-1) {
	    c2 = m_expr.charAt(m_ind+1);
	} else {
	    c2 = '\0';
	}

	if ((c != '+') && (c != '-')) {
	    if (m_ind == m_len - 1) {
	        // Integer shortcut when only 1 character left
	        switch (c) {
	            case '0':
	            case '1':
	            case '2':
	            case '3':
	            case '4':
	            case '5':
	            case '6':
	            case '7':
	            case '8':
	            case '9':
	                m_ind++;
	                m_token = VALUE;
	                ExprValue value = grabExprValue();
	                value.setIntValue(c - '0',
	                    String.valueOf(c));
	                return value;
	        }
	    }
	    final boolean startsWithDigit = Character.isDigit(c);
	    if (startsWithDigit && looksLikeInt(m_expr, m_len, m_ind, false)) {
		StrtoulResult res = interp.strtoulResult;
		Util.strtoul(m_expr, m_ind, 0, res);

		if (res.errno == 0) {
		    String token = m_expr.substring(m_ind, res.index);
		    m_ind = res.index;
		    m_token = VALUE;
		    ExprValue value = grabExprValue();
		    value.setIntValue((int) res.value, token);
		    return value;
		} else {
		    if (res.errno == TCL.INTEGER_RANGE) {
			IntegerTooLarge(interp);
		    }
		}
	    } else if (startsWithDigit || (c == '.')
		    || (c == 'n') || (c == 'N')) {
		StrtodResult res = interp.strtodResult;
		Util.strtod(m_expr, m_ind, res);
		if (res.errno == 0) {
                    String token = m_expr.substring(m_ind, res.index);
		    m_ind = res.index;
		    m_token = VALUE;
		    ExprValue value = grabExprValue();
		    value.setDoubleValue(res.value, token);
		    return value;
		} else {
		    if (res.errno == TCL.DOUBLE_RANGE) {
			if (res.value != 0) {
			    DoubleTooLarge(interp);
			} else {
			    DoubleTooSmall(interp);
			}
		    }
		}
	    }
	}

	ParseResult pres;
	ExprValue retval;
	m_ind += 1;		// ind is advanced to point to the next token

	switch (c) {
	case '$':
	    m_token = VALUE;
	    pres = ParseAdaptor.parseVar(interp, m_expr, m_ind, m_len);
	    m_ind = pres.nextIndex;

	    if (interp.noEval != 0) {
		retval = grabExprValue();
		retval.setIntValue(0);
	    } else {
		retval = grabExprValue();
		ExprParseObject(interp, pres.value, retval);
	    }
	    pres.release();
	    return retval;
	case '[':
	    m_token = VALUE;
	    pres = ParseAdaptor.parseNestedCmd(interp, m_expr, m_ind, m_len);
	    m_ind = pres.nextIndex;

	    if (interp.noEval != 0) {
		retval = grabExprValue();
		retval.setIntValue(0);
	    } else {
		retval = grabExprValue();
		ExprParseObject(interp, pres.value, retval);
	    }
	    pres.release();
	    return retval;
	case '"':
	    m_token = VALUE;


	    //System.out.println("now to parse from ->" + m_expr + "<- at index "
	//	+ m_ind);

	    pres = ParseAdaptor.parseQuotes(interp, m_expr, m_ind, m_len);
	    m_ind = pres.nextIndex;

	 //   System.out.println("after parse next index is " + m_ind);

	    if (interp.noEval != 0) {
	  //      System.out.println("returning noEval zero value");
		retval = grabExprValue();
		retval.setIntValue(0);
	    } else {
	   //     System.out.println("returning value string ->" + pres.value.toString() + "<-" );
		retval = grabExprValue();
		ExprParseObject(interp, pres.value, retval);
	    }
	    pres.release();
	    return retval;
	case '{':
	    m_token = VALUE;
	    pres = ParseAdaptor.parseBraces(interp, m_expr, m_ind, m_len);
	    m_ind = pres.nextIndex;
	    if (interp.noEval != 0) {
		retval = grabExprValue();
		retval.setIntValue(0);
	    } else {
		retval = grabExprValue();
		ExprParseObject(interp, pres.value, retval);
	    }
	    pres.release();
	    return retval;
	case '(':
	    m_token = OPEN_PAREN;
	    return null;

	case ')':
	    m_token = CLOSE_PAREN;
	    return null;

	case ',':
	    m_token = COMMA;
	    return null;

	case '*':
	    m_token = MULT;
	    return null;

	case '/':
	    m_token = DIVIDE;
	    return null;

	case '%':
	    m_token = MOD;
	    return null;

	case '+':
	    m_token = PLUS;
	    return null;

	case '-':
	    m_token = MINUS;
	    return null;

	case '?':
	    m_token = QUESTY;
	    return null;

	case ':':
	    m_token = COLON;
	    return null;

	case '<':
	    switch (c2) {
	    case '<':
		m_ind += 1;
		m_token = LEFT_SHIFT;
		break;
	    case '=':
		m_ind += 1;
		m_token = LEQ;
		break;
	    default:
		m_token = LESS;
		break;
	    }
	    return null;

	case '>':
	    switch (c2) {
	    case '>':
		m_ind += 1;
		m_token = RIGHT_SHIFT;
		break;
	    case '=':
		m_ind += 1;
		m_token = GEQ;
		break;
	    default:
		m_token = GREATER;
		break;
	    }
	    return null;

	case '=':
	    if (c2 == '=') {
		m_ind += 1;
		m_token = EQUAL;
	    } else {
		m_token = UNKNOWN;
	    }
	    return null;

	case '!':
	    if (c2 == '=') {
		m_ind += 1;
		m_token = NEQ;
	    } else {
		m_token = NOT;
	    }
	    return null;

	case '&':
	    if (c2 == '&') {
		m_ind += 1;
		m_token = AND;
	    } else {
		m_token = BIT_AND;
	    }
	    return null;

	case '^':
	    m_token = BIT_XOR;
	    return null;

	case '|':
	    if (c2 == '|') {
		m_ind += 1;
		m_token = OR;
	    } else {
		m_token = BIT_OR;
	    }
	    return null;

	case '~':
	    m_token = BIT_NOT;
	    return null;

	case 'e':
	case 'n':
	    if (c == 'e' && c2 == 'q') {
	        m_ind += 1;
	        m_token = STREQ;
	        return null;
	    } else if (c == 'n' && c2 == 'e') {
	        m_ind += 1;
	        m_token = STRNEQ;
	        return null;
	    }
            // Fall through to default

	default:
	    if (Character.isLetter(c)) {
		// Oops, re-adjust m_ind so that it points to the beginning
		// of the function name or literal.

		m_ind--;

		//
		// Check for boolean literals (true, false, yes, no, on, off)
		// This is kind of tricky since we don't want to pull a
		// partial boolean literal "f" off of the front of a function
		// invocation like expr {floor(1.1)}.
		//

		String substr = m_expr.substring(m_ind);
		boolean is_math_func = false;

		//System.out.println("default char '" + c + "' str is \"" +
		//    m_expr + "\" and m_ind " + m_ind + " substring is \"" +
		//    substr + "\"");

		final int max = substr.length();
		int i;
		for (i=0; i < max ; i++) {
		    c = substr.charAt(i);
		    if (! (Character.isLetterOrDigit(c) || c == '_')) {
		        break;
		    }
		}
		// Skip any whitespace characters too
		for (; i < max; i++) {
		    c = substr.charAt(i);
		    if (c == ' ' || Character.isWhitespace(c)) {
		        continue;
		    } else {
		        break;
		    }
		}
		if ((i < max) && (substr.charAt(i) == '(')) {
		    //System.out.println("known to be a math func, char is '" +
		    //    substr.charAt(i) + "'");
		    is_math_func = true;
		}

		if (!is_math_func) {
		    String tok = getBooleanToken(substr);
		    if (tok != null) {
		        m_ind += tok.length();
		        m_token = VALUE;
		        ExprValue value = grabExprValue();
		        value.setStringValue(tok);
		        return value;
		    }
		}

		return mathFunction(interp);
	    }
	    m_token = UNKNOWN;
	    return null;
	}
    }

    /**
     * Parses a math function from an expression string, carry out the
     * function, and return the value computed.
     *
     * @param interp current interpreter.
     * @return the value computed by the math function.
     * @exception TclException if any error happens.
     */
    ExprValue mathFunction(Interp interp) throws TclException {
	int startIdx = m_ind;
	ExprValue value;
	String funcName;
	MathFunction mathFunc;
	ExprValue[] values = null;
	int numArgs;

	// Find the end of the math function's name and lookup the MathFunc
	// record for the function.  Search until the char at m_ind is not
	// alphanumeric or '_'

	for (; m_ind<m_len; m_ind++) {
	    if (!(Character.isLetterOrDigit(m_expr.charAt(m_ind)) ||
		  m_expr.charAt(m_ind) == '_')){
		break;
	    }
	}

	// Get the funcName BEFORE calling ExprLex, so the funcName
	// will not have trailing whitespace.

	funcName = m_expr.substring(startIdx, m_ind);

	// Parse errors are thrown BEFORE unknown function names

	ExprLex(interp);
	if (m_token != OPEN_PAREN) {
	    SyntaxError(interp);
	}

	// Now test for unknown funcName.  Doing the above statements
	// out of order will cause some tests to fail.

	mathFunc = (MathFunction) mathFuncTable.get(funcName);
	if (mathFunc == null) {
	    throw new TclException(interp,
		    "unknown math function \"" + funcName + "\"");
	}

	// Scan off the arguments for the function, if there are any.

	numArgs = mathFunc.argTypes.length;

	if (numArgs == 0) {
	    ExprLex(interp);
	    if (m_token != CLOSE_PAREN) {
		SyntaxError(interp);
	    }
	} else {
	    values = new ExprValue[numArgs];

	    for (int i = 0; ; i++) {
		value = ExprGetValue(interp, -1);

		// Handle close paren with no value
		// % expr {srand()}

		if ((value == null) && (m_token == CLOSE_PAREN)) {
		    if (i == numArgs)
		        break;
                    else
		        throw new TclException(interp,
		            "too few arguments for math function");
		}

		values[i] = value;

		// Check for a comma separator between arguments or a
		// close-paren to end the argument list.

		if (i == (numArgs-1)) {
		    if (m_token == CLOSE_PAREN) {
			break;
		    }
		    if (m_token == COMMA) {
			throw new TclException(interp,
				"too many arguments for math function");
		    } else {
			SyntaxError(interp);
		    }
		}
		if (m_token != COMMA) {
		    if (m_token == CLOSE_PAREN) {
			throw new TclException(interp,
				"too few arguments for math function");
		    } else {
			SyntaxError(interp);
		    }
		}
	    }
	}

	m_token = VALUE;
	if (interp.noEval != 0) {
	    ExprValue rvalue = grabExprValue();
	    rvalue.setIntValue(0);
	    return rvalue;
	} else {
	    // Invoke the function and copy its result back into valuePtr.
	    return evalMathFunction(interp, funcName, mathFunc, values);
	}
    }

    /**
     * This procedure will lookup and invoke a math function
     * given the name of the function and an array of ExprValue
     * arguments. Each ExprValue is released before the function
     * exits. This method is intended to be used by other modules
     * that may need to invoke a math function at runtime. It is
     * assumed that the caller has checked the number of arguments,
     * the type of the arguments will be adjusted before invocation
     * if needed.
     *
     * The values argument can be null when there are no args to pass.
     */

    ExprValue
    evalMathFunction(Interp interp, String funcName, ExprValue[] values)
        throws TclException
    {
        MathFunction mathFunc = (MathFunction) mathFuncTable.get(funcName);
        if (mathFunc == null) {
            throw new TclException(interp,
        	    "unknown math function \"" + funcName + "\"");
        }
        return evalMathFunction(interp, funcName, mathFunc, values);
    }

    /**
     * This procedure implement a math function invocation.
     * See the comments for the function above, note that
     * this method is used when the math function pointer
     * has already been looked up.
     *
     * The values argument can be null when there are no args to pass.
     */

    ExprValue
    evalMathFunction(Interp interp, String funcName, MathFunction mathFunc, ExprValue[] values)
        throws TclException
    {
        if (mathFunc.argTypes == null) {
            throw new TclRuntimeError("math function \"" + funcName + "\" has null argTypes");
        }

        // Ensure that arguments match the int/double
        // expectations of the math function.

        int numArgs = mathFunc.argTypes.length;
        int expectedArgs = 0;
        if (values != null) {
            expectedArgs = values.length;
        }

        if (numArgs != expectedArgs) {
            if ((expectedArgs > 0) && (expectedArgs < numArgs)) {
                throw new TclException(interp,
                    "too few arguments for math function");
            } else {
                throw new TclException(interp,
                    "too many arguments for math function");
            }
        }

        if (values != null) {
            for (int i=0; i < values.length ; i++) {
                ExprValue value = values[i];
                if (value.isStringType()) {
                    throw new TclException(interp,
                        "argument to math function didn't have numeric value");
                } else if (value.isIntType()) {
                    if (mathFunc.argTypes[i] == MathFunction.DOUBLE) {
                        value.setDoubleValue((double) value.getIntValue());
                    }
                } else {
                    if (mathFunc.argTypes[i] == MathFunction.INT) {
                        value.setIntValue((int) value.getDoubleValue());
                    }
                }
            }
        }

        ExprValue rval = mathFunc.apply(interp, values);
        if (values != null) {
            // Release ExprValue elements in values array
            for (int i=0; i < values.length ; i++) {
                releaseExprValue(values[i]);
            }
        }
        return rval;
    }

    /**
     * This procedure will register a math function by
     * adding it to the table of available math functions.
     * This methods is used when regression testing the
     * expr command.
     */

    void
    registerMathFunction(String name, MathFunction mathFunc)
    {
	mathFuncTable.put(name, mathFunc);
    }

    /**
     * This procedure decides whether the leading characters of a
     * string look like an integer or something else (such as a
     * floating-point number or string). If the whole flag is
     * true then the entire string must look like an integer.
     * @return a boolean value indicating if the string looks like an integer.
     */

    static boolean
    looksLikeInt(String s, int len, int i, boolean whole) {
	char c;
	while (i < len && (((c = s.charAt(i)) == ' ') ||
                Character.isWhitespace(c))) {
	    i++;
	}
	if (i >= len) {
	    return false;
	}
	c = s.charAt(i);
	if ((c == '+') || (c == '-')) {
	    i++;
	    if (i >= len) {
		return false;
	    }
	    c = s.charAt(i);
	}
	if (! Character.isDigit(c)) {
	    return false;
	}
	while (i < len && Character.isDigit(s.charAt(i))) {
	    //System.out.println("'" + s.charAt(i) + "' is a digit");
	    i++;
	}
	if (i >= len) {
            return true;
	}

        c = s.charAt(i);

        if (!whole && (c != '.') && (c != 'E') && (c != 'e') ) {
            return true;
        }
        if (c == 'e' || c == 'E') {
            // Could be a double like 1e6 or 1e-1 but
            // it could also be 1eq2. If the next
            // character is not a digit or a + or -,
            // then this must not be a double. If the
            // whole string must look like an integer
            // then we know this is not an integer.
            if (whole) {
                return false;
            } else if (i+1 >= len) {
                return true;
            }
            c = s.charAt(i+1);
            if (c != '+' && c != '-' && !Character.isDigit(c)) {
                // Does not look like "1e1", "1e+1", or "1e-1"
                // so strtoul would parse the text leading up
                // to the 'e' as an integer.
                return true;
            }
        }
        if (whole) {
            while (i < len && (((c = s.charAt(i)) == ' ') ||
                    Character.isWhitespace(c))) {
                i++;
            }
            if (i >= len) {
                return true;
            }
        }

	return false;
    }

    /**
     * Converts a value from int or double representation to a string.
     * @param interp interpreter to use for precision information.
     * @param value Value to be converted.
     */

    static void ExprMakeString(Interp interp, ExprValue value) {
	if (value.isIntOrDoubleType()) {
	    value.toStringType();
	}
    }

    static void checkIntegerRange(Interp interp,double d) throws TclException {
	if (d < 0) {
	    if (d < ((double) TCL.INT_MIN)) {
		Expression.IntegerTooLarge(interp);
	    }
	} else {
	    if (d > ((double) TCL.INT_MAX)) {
		Expression.IntegerTooLarge(interp);
	    }
	}
    }

    static void checkDoubleRange(Interp interp, double d) throws TclException {
	if ((d == Double.NaN) ||
		(d == Double.NEGATIVE_INFINITY) ||
		(d == Double.POSITIVE_INFINITY)) {
	    Expression.DoubleTooLarge(interp);
	}
    }

    // If the string starts with a boolean token, then
    // return the portion of the string that matched
    // a boolean token. Otherwise, return null.

    static String getBooleanToken(String tok) {
        int length = tok.length();
        if ( length == 0 ) {
            return null;
        }
        char c = tok.charAt(0);
        switch (c) {
            case 'f':
                if (tok.startsWith("false")) {
                    return "false";
                }
                if (tok.startsWith("fals")) {
                    return "fals";
                }
                if (tok.startsWith("fal")) {
                    return "fal";
                }
                if (tok.startsWith("fa")) {
                    return "fa";
                }
                if (tok.startsWith("f")) {
                    return "f";
                }
            case 'n':
                if (tok.startsWith("no")) {
                    return "no";
                }
                if (tok.startsWith("n")) {
                    return "n";
                }
            case 'o':
                if (tok.startsWith("off")) {
                    return "off";
                }
                if (tok.startsWith("of")) {
                    return "of";
                }
                if (tok.startsWith("on")) {
                    return "on";
                }
            case 't':
                if (tok.startsWith("true")) {
                    return "true";
                }
                if (tok.startsWith("tru")) {
                    return "tru";
                }
                if (tok.startsWith("tr")) {
                    return "tr";
                }
                if (tok.startsWith("t")) {
                    return "t";
                }
            case 'y':
                if (tok.startsWith("yes")) {
                    return "yes";
                }
                if (tok.startsWith("ye")) {
                    return "ye";
                }
                if (tok.startsWith("y")) {
                    return "y";
                }
        }
        return null;
    }

    // Get an ExprValue object out of the cache
    // of ExprValues. These values will be released
    // later on by a call to releaseExprValue. Don't
    // bother with release on syntax or other errors
    // since the exprValueCache will refill itself.

    final ExprValue grabExprValue() {
        if (cachedExprIndex == cachedExprLength) {
            // Allocate new ExprValue if cache is empty
            return new ExprValue(0, null);
        } else {
            return cachedExprValue[cachedExprIndex++];
        }
    }

    final void releaseExprValue(ExprValue val) {
        if (cachedExprIndex > 0) {
            // Cache is not full, return value to cache
            cachedExprValue[--cachedExprIndex] = val;
        }

        // Debug check for duplicate values in range > cachedExprIndex
        if (false) {
        if (cachedExprIndex < 0) {
            throw new TclRuntimeError("cachedExprIndex is " + cachedExprIndex);
        }
        for (int i=cachedExprIndex; i < cachedExprLength ; i++) {
            for (int j=cachedExprIndex; j < cachedExprLength ; j++) {
                if ((j == i) || (cachedExprValue[i] == null)) {
                    continue;
                }
                if (cachedExprValue[i] == cachedExprValue[j]) {
                    throw new TclRuntimeError(
                        "same object at " + i + " and " + j);
                }
            }
        }
        }
    }
}

abstract class MathFunction {
    static final int INT    = 0;
    static final int DOUBLE = 1;
    static final int EITHER = 2;

    int[] argTypes;

    abstract ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException;
}

abstract class UnaryMathFunction extends MathFunction {
    UnaryMathFunction() {
	argTypes = new int[1];
	argTypes[0] = DOUBLE;
    }
}

abstract class BinaryMathFunction extends MathFunction {
    BinaryMathFunction() {
	argTypes = new int[2];
	argTypes[0] = DOUBLE;
	argTypes[1] = DOUBLE;
    }
}


abstract class NoArgMathFunction extends MathFunction {
    NoArgMathFunction() {
	argTypes = new int[0];
    }
}


class Atan2Function extends BinaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(Math.atan2(
		values[0].getDoubleValue(),
		values[1].getDoubleValue()));
	return value;
    }
}

class AbsFunction extends MathFunction {
    AbsFunction() {
	argTypes = new int[1];
	argTypes[0] = EITHER;
    }

    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	ExprValue value = interp.expr.grabExprValue();
	if (values[0].isDoubleType()) {
	    double d = values[0].getDoubleValue();
	    if (d>0) {
		value.setDoubleValue(d);
	    } else {
		value.setDoubleValue(-d);
	    }
	} else {
	    int i = values[0].getIntValue();
	    if (i>0) {
		value.setIntValue(i);
	    } else {
		value.setIntValue(-i);
	    }
	}
        return value;
    }
}

class DoubleFunction extends MathFunction {
    DoubleFunction() {
	argTypes = new int[1];
	argTypes[0] = EITHER;
    }

    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	ExprValue value = interp.expr.grabExprValue();
	if (values[0].isIntType()) {
	    value.setDoubleValue( (double) values[0].getIntValue() );
	} else {
	    value.setDoubleValue( values[0].getDoubleValue() );
	}
	return value;
    }
}

class IntFunction extends MathFunction {
    IntFunction() {
	argTypes = new int[1];
	argTypes[0] = EITHER;
    }

    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	ExprValue value = interp.expr.grabExprValue();
	if (values[0].isIntType()) {
	    value.setIntValue( values[0].getIntValue() );
	} else {
	    double d = values[0].getDoubleValue();
	    Expression.checkIntegerRange(interp, d);
	    value.setIntValue( (int) d );
	}
	return value;
    }
}

class RoundFunction extends MathFunction {
    RoundFunction() {
	argTypes = new int[1];
	argTypes[0] = EITHER;
    }

    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	ExprValue value = interp.expr.grabExprValue();
	if (values[0].isDoubleType()) {
	    double d = values[0].getDoubleValue();
	    double i = (d < 0.0 ? Math.ceil(d) : Math.floor(d));
	    double f = d - i;
	    if (d < 0.0) {
		if (f <= -0.5) {
		    i += -1.0;
		}
		Expression.checkIntegerRange(interp, i);
		value.setIntValue((int) i);
	    } else {
		if (f >= 0.5) {
		    i += 1.0;
		}
		Expression.checkIntegerRange(interp, i);
		value.setIntValue((int) i);
	    }
	} else {
	    value.setIntValue( values[0].getIntValue() );
	}
        return value;
    }
}

class PowFunction extends BinaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	double d = Math.pow(
		values[0].getDoubleValue(),
		values[1].getDoubleValue());
	Expression.checkDoubleRange(interp, d);
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(d);
	return value;
    }
}

/*
 * The following section is generated by this script.
 *
 set missing {fmod}
 set byhand {atan2 pow}


 foreach func {Acos Asin Atan Ceil Cos Exp Floor Log Sin
         Sqrt Tan} {
     puts "
class $func\Function extends UnaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv\[\])
	    throws TclException {
	return new ExprValue(Math.[string tolower $func](TclDouble.get(interp, argv\[0\])));
    }
}
"
 }

 */

class AcosFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	double d = values[0].getDoubleValue();
	if ((d < -1) || (d > 1)) {
	    Expression.DomainError(interp);
	}
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(Math.acos(d));
	return value;
    }
}

class AsinFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(Math.asin(values[0].getDoubleValue()));
	return value;
    }
}

class AtanFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(Math.atan(values[0].getDoubleValue()));
	return value;
    }
}


class CeilFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(Math.ceil(values[0].getDoubleValue()));
	return value;
    }
}


class CosFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(Math.cos(values[0].getDoubleValue()));
	return value;
    }
}


class CoshFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	double x = values[0].getDoubleValue();
	double d1 = Math.pow(Math.E, x);
	double d2 = Math.pow(Math.E,-x);

	Expression.checkDoubleRange(interp, d1);
	Expression.checkDoubleRange(interp, d2);
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue((d1+d2)/2);
	return value;
    }
}

class ExpFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	double d = Math.exp(values[0].getDoubleValue());
	if ((d == Double.NaN) ||
		(d == Double.NEGATIVE_INFINITY) ||
		(d == Double.POSITIVE_INFINITY)) {
	    Expression.DoubleTooLarge(interp);
	}
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(d);
	return value;
    }
}


class FloorFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(Math.floor(values[0].getDoubleValue()));
	return value;
    }
}


class FmodFunction extends BinaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	double d1 = values[0].getDoubleValue();
	double d2 = values[1].getDoubleValue();
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(Math.IEEEremainder(d1, d2));
	return value;
    }
}

class HypotFunction extends BinaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	double x = values[0].getDoubleValue();
	double y = values[1].getDoubleValue();
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(Math.sqrt(((x * x) + (y * y)))); 
	return value;
    }
}


class LogFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(Math.log(values[0].getDoubleValue())); 
	return value;
    }
}


class Log10Function extends UnaryMathFunction {
   private static final double log10 = Math.log(10);
   ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	double d = values[0].getDoubleValue();
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(Math.log(d / log10)); 
	return value;
   }
}


class SinFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	double d = values[0].getDoubleValue();
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(Math.sin(d));
	return value;
    }
}


class SinhFunction extends UnaryMathFunction {
   ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	double x = values[0].getDoubleValue();

	double d1 = Math.pow(Math.E, x);
	double d2 = Math.pow(Math.E,-x);

	Expression.checkDoubleRange(interp, d1);
	Expression.checkDoubleRange(interp, d2);

	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue((d1-d2)/2);
	return value;
    }
}


class SqrtFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	double d = values[0].getDoubleValue();
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(Math.sqrt(d));
	return value;
    }
}


class TanFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	double d = values[0].getDoubleValue();
	ExprValue value = interp.expr.grabExprValue();
	value.setDoubleValue(Math.tan(d));
	return value;
    }
}

class TanhFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
	double x = values[0].getDoubleValue();
	ExprValue value = interp.expr.grabExprValue();
	if (x == 0) {
	    value.setDoubleValue(0.0);
	    return value;
	}

	double d1 = Math.pow(Math.E, x);
	double d2 = Math.pow(Math.E,-x);

	Expression.checkDoubleRange(interp, d1);
	Expression.checkDoubleRange(interp, d2);

	value.setDoubleValue((d1 - d2) / (d1 + d2));
	return value;
    }
}

class RandFunction extends NoArgMathFunction {

    // Generate the random number using the linear congruential
    // generator defined by the following recurrence:
    //		seed = ( IA * seed ) mod IM
    // where IA is 16807 and IM is (2^31) - 1.  In order to avoid
    // potential problems with integer overflow, the  code uses
    // additional constants IQ and IR such that
    //		IM = IA*IQ + IR
    // For details on how this algorithm works, refer to the following
    // papers: 
    //
    //	S.K. Park & K.W. Miller, "Random number generators: good ones
    //	are hard to find," Comm ACM 31(10):1192-1201, Oct 1988
    //
    //	W.H. Press & S.A. Teukolsky, "Portable random number
    //	generators," Computers in Physics 6(5):522-524, Sep/Oct 1992.


    private static final int randIA   = 16807;
    private static final int randIM   = 2147483647;
    private static final int randIQ   = 127773;
    private static final int randIR   = 2836;
    private static final Date date     = new Date();

    /**
     * Srand calls the main algorithm for rand after it sets the seed.
     * To facilitate this call, the method is static and can be used
     * w/o creating a new object.  But we also need to maintain the
     * inheritance hierarchy, thus the dynamic apply() calls the static 
     * statApply().
     */

    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {
        return(statApply(interp, values));
    }


    static ExprValue statApply(Interp interp, ExprValue[] values)
	    throws TclException {

	int tmp;
	ExprValue value = interp.expr.grabExprValue();

	if (!(interp.randSeedInit)) {
	    interp.randSeedInit = true;
	    interp.randSeed = (int)date.getTime();
	}

	if (interp.randSeed == 0) {
	    // Don't allow a 0 seed, since it breaks the generator.  Shift
	    // it to some other value.

	    interp.randSeed = 123459876;
	}

	tmp = (int) (interp.randSeed / randIQ);
	interp.randSeed = ((randIA * (interp.randSeed - tmp * randIQ))
                - randIR*tmp);
	    
	if (interp.randSeed < 0) {
	    interp.randSeed += randIM;
	}

	value.setDoubleValue( interp.randSeed * (1.0/randIM) );
	return value;
    }
}


class SrandFunction extends UnaryMathFunction {

    ExprValue apply(Interp interp, ExprValue[] values)
	    throws TclException {

	// Reset the seed.

	interp.randSeedInit = true;
	interp.randSeed     = (long) values[0].getDoubleValue();

	// To avoid duplicating the random number generation code we simply
	// call the static random number generator in the RandFunction 
	// class.

	return (RandFunction.statApply(interp, null));
    }
}
