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
 * RCS: @(#) $Id: Expression.java,v 1.1 1998/10/14 21:09:21 cvsadmin Exp $
 *
 */

package tcl.lang;

import java.util.*;

/**
 * This class handles Tcl expressions.
 */
class Expression {

    /*
     * The token types are defined below.  In addition, there is a
     * table associating a precedence with each operator.  The order
     * of types is important.  Consult the code before changing it.
     */

    static final int VALUE	 = 0;
    static final int OPEN_PAREN  = 1;
    static final int CLOSE_PAREN = 2;
    static final int COMMA 	 = 3;
    static final int END 	 = 4;
    static final int UNKNOWN	 = 5;

    /*
     * Binary operators:
     */

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

    /*
     * Unary operators:
     */

    static final int UNARY_MINUS = 28;
    static final int UNARY_PLUS  = 29;
    static final int NOT	 = 30;
    static final int BIT_NOT	 = 31;

    /*
     * Precedence table.  The values for non-operator token types are ignored.
     */

    static int precTable[] = {
	0, 0, 0, 0, 0, 0, 0, 0,
	12, 12, 12,			/* MULT, DIVIDE, MOD */
	11, 11,				/* PLUS, MINUS */
	10, 10,				/* LEFT_SHIFT, RIGHT_SHIFT */
	9, 9, 9, 9,			/* LESS, GREATER, LEQ, GEQ */
	8, 8,				/* EQUAL, NEQ */
	7,				/* BIT_AND */
	6,				/* BIT_XOR */
	5,				/* BIT_OR */
	4,				/* AND */
	3,				/* OR */
	2,				/* QUESTY */
	1,				/* COLON */
	13, 13, 13, 13			/* UNARY_MINUS, UNARY_PLUS, NOT,
					 * BIT_NOT */
    };

    /*
     * Mapping from operator numbers to strings;  used for error messages.
     */

    static String operatorStrings[] = {
        "VALUE", "(", ")", ",", "END", "UNKNOWN", "6", "7",
	"*", "/", "%", "+", "-", "<<", ">>", "<", ">", "<=",
	">=", "==", "!=", "&", "^", "|", "&&", "||", "?", ":",
	"-", "+", "!", "~"
    };

    Hashtable mathFuncTable;

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
     * Evaluate a Tcl expression.
     *
     * @param interp the context in which to evaluate the expression.
     * @param string expression to evaluate.
     * @return the value of the expression.
     * @exception TclException for malformed expressions.
     */

    TclObject eval(Interp interp, String string)
	    throws TclException {
	ExprValue value = ExprTopLevel(interp, string);
	switch (value.type) {
	case ExprValue.INT:
	    return TclInteger.newInstance((int) value.intValue);
	case ExprValue.DOUBLE:
	    return TclDouble.newInstance(value.doubleValue);
	case ExprValue.STRING:
	    return TclString.newInstance(value.stringValue);
	default:
	    throw new TclRuntimeError("internal error: expression, unknown");
	}
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
	ExprValue value = ExprTopLevel(interp, string);
	switch (value.type) {
	case ExprValue.INT:
	    return (value.intValue != 0);
	case ExprValue.DOUBLE:
	    return (value.doubleValue != 0.0);
	case ExprValue.STRING:
	    return Util.getBoolean(interp, value.stringValue);
	default:
	    throw new TclRuntimeError("internal error: expression, unknown");
	}
    }

    /**
     * Constructor.
     */
    Expression() {
	mathFuncTable = new Hashtable();

	/*
	 * rand  -- missing
	 * srand -- missing
	 * hypot -- needs testing
	 * fmod  -- needs testing
	 *              try [expr fmod(4.67, 2.2)]
	 *              the answer should be .27, but I got .2699999999999996
	 */

	mathFuncTable.put("atan2", new Atan2Function());
	mathFuncTable.put("pow",   new PowFunction());
        mathFuncTable.put("acos",  new AcosFunction());
	mathFuncTable.put("asin",  new AsinFunction());
	mathFuncTable.put("atan",  new AtanFunction());
	mathFuncTable.put("ceil",  new CeilFunction());
	mathFuncTable.put("cos",   new CosFunction());
	mathFuncTable.put("cosh",  new CoshFunction());
	mathFuncTable.put("exp",   new ExpFunction());
	mathFuncTable.put("floor", new FloorFunction());
	mathFuncTable.put("fmod",  new FmodFunction());
	mathFuncTable.put("hypot", new HypotFunction());
	mathFuncTable.put("log",   new LogFunction());
	mathFuncTable.put("log10", new Log10Function());
	mathFuncTable.put("rand",  new RandFunction());
	mathFuncTable.put("sin",   new SinFunction());
	mathFuncTable.put("sinh",  new SinhFunction());
	mathFuncTable.put("sqrt",  new SqrtFunction());
	mathFuncTable.put("srand", new SrandFunction());
	mathFuncTable.put("tan",   new TanFunction());
	mathFuncTable.put("tanh",  new TanhFunction());

	mathFuncTable.put("abs",   new AbsFunction());
	mathFuncTable.put("double",new DoubleFunction());
	mathFuncTable.put("int",   new IntFunction());
	mathFuncTable.put("round", new RoundFunction());

	m_expr = null;
	m_ind = 0;
	m_len = 0;
	m_token = UNKNOWN;
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

	/*
	 * Saved the state variables so that recursive calls to expr
	 * can work:
	 *	expr {[expr 1+2] + 3}
	 */

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

    static void DomainError(Interp interp) throws TclException {
	interp.setErrorCode(TclString.newInstance(
		"ARITH DOMAIN {domain error: argument not in valid range}"));
	throw new TclException(interp,
		"domain error: argument not in valid range");
    }

    /**
     * Given a string (such as one coming from command or variable
     * substitution), make a Value based on the string.  The value
     * be a floating-point or integer, if possible, or else it
     * just be a copy of the string.
     *
     * @param interp the context in which to evaluate the expression.
     * @param s the string to parse.
     * @exception TclException for malformed expressions.
     * @return the value of the expression.
     */

    private ExprValue ExprParseString(Interp interp, String s)
	    throws TclException {

	int len = s.length();
	int i;
	if (ExprLooksLikeInt(s, len, 0)) {
	    long value;

	    /*
	     * Note: use strtoul instead of strtol for integer conversions
	     * to allow full-size unsigned numbers, but don't depend on
	     * strtoul to handle sign characters;  it won't in some
	     * implementations.
	     */
    
	    for (i = 0; Character.isWhitespace(s.charAt(i)); i++) {
		/* Empty loop body. */
	    }

	    StrtoulResult res;
	    if (s.charAt(i) == '-') {
		i++;
		res = Util.strtoul(s, i, 0);
		res.value = - res.value;
	    } else if (s.charAt(i) == '+') {
		i++;
		res = Util.strtoul(s, i, 0);
	    } else {
		res = Util.strtoul(s, i, 0);
	    }

	    if (res.index == len) {
		/*
		 * We treat this string as a number only if the number
		 * ends at the end of the string. E.g.: " 1", "1" are
		 * good numbers but "1 " is not.
		 */

		if (res.errno == TCL.INTEGER_RANGE) {
		    IntegerTooLarge(interp);
		} else {
		    m_token = VALUE;
		    return new ExprValue(res.value);
		}
	    }
	} else {
	    StrtodResult res = Util.strtod(s, 0);
	    if (res.index == len) {
		if (res.errno == TCL.DOUBLE_RANGE) {
		    DoubleTooLarge(interp);
		} else {
		    m_token = VALUE;
		    return new ExprValue(res.value);
		}
	    }
	}

	/*
	 * Not a valid number.  Save a string value (but don't do anything
	 * if it's already the value).
	 */
	return new ExprValue(s);
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
	boolean gotOp = false;		/* True means already lexed the
					 * operator (while picking up value
					 * for unary operator).  Don't lex
					 * again. */
	ExprValue value, value2;

        /*
	 * There are two phases to this procedure.  First, pick off an
	 * initial value.  Then, parse (binary operator, value) pairs
	 * until done.
	 */
	value = ExprLex(interp);

	if (m_token == OPEN_PAREN) {
	    /*
	     * Parenthesized sub-expression.
	     */
	    value = ExprGetValue(interp, -1);
	    if (m_token != CLOSE_PAREN) {
		throw new TclException(interp, "unmatched parentheses in " + 
			"expression \"" + m_expr + "\"");
	    }
	} else {
	    if (m_token == MINUS) {
		m_token = UNARY_MINUS;
	    }
	    if (m_token == PLUS) {
		m_token = UNARY_PLUS;
	    }
	    if (m_token >= UNARY_MINUS) {

		/*
		 * Process unary operators.
		 */
		operator = m_token;
		value = ExprGetValue(interp, precTable[m_token]);

		if (interp.noEval == 0) {
		    switch (operator) {
		    case UNARY_MINUS:
			if (value.type == ExprValue.INT) {
			    value.intValue = -value.intValue;
			} else if (value.type == ExprValue.DOUBLE){
			    value.doubleValue = -value.doubleValue;
			} else {
			    IllegalType(interp, value.type, operator);
			} 
			break;
		    case UNARY_PLUS:
			if ((value.type != ExprValue.INT)
				&& (value.type != ExprValue.DOUBLE)) {
			    IllegalType(interp, value.type, operator);
			} 
			break;
		    case NOT:
			if (value.type == ExprValue.INT) {
			    if (value.intValue != 0) {
				value.intValue = 0;
			    } else {
				value.intValue = 1;
			    }
			} else if (value.type == ExprValue.DOUBLE) {
			    if (value.doubleValue == 0.0) {
				value.intValue = 1;
			    } else {
				value.intValue = 0;
			    }
			    value.type = ExprValue.INT;
			} else {
			    IllegalType(interp, value.type, operator);
			}
			break;
		    case BIT_NOT:
			if (value.type == ExprValue.INT) {
			    value.intValue = ~value.intValue;
			} else {
			    IllegalType(interp, value.type, operator);
			}
			break;
		    }
		}
		gotOp = true;
	    } else if (m_token != VALUE) {
		SyntaxError(interp);
	    }
	}
	if (value == null) {
	    SyntaxError(interp);
	}

	/*
	 * Got the first operand.  Now fetch (operator, operand) pairs.
	 */

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

	    /*
	     * If we're doing an AND or OR and the first operand already
	     * determines the result, don't execute anything in the
	     * second operand:  just parse.  Same style for ?: pairs.
	     */

	    if ((operator == AND) || (operator == OR) ||(operator == QUESTY)){

		if (value.type == ExprValue.DOUBLE) {
		    value.intValue = (value.doubleValue != 0) ? 1 : 0;
		    value.type = ExprValue.INT;
		} else if (value.type == ExprValue.STRING) {
		    if (interp.noEval == 0) {
			IllegalType(interp, ExprValue.STRING, operator);
		    }

		    /*
		     * Must set value.intValue to avoid referencing
		     * uninitialized memory in the "if" below;  the actual
		     * value doesn't matter, since it will be ignored.
		     */
		    
		    value.intValue = 0;
		}
		if (((operator == AND) && (value.intValue == 0))
		        || ((operator == OR) && (value.intValue != 0))) {
		    interp.noEval ++;
		    try {
			value2 = ExprGetValue(interp, precTable[operator]);
		    } finally {
			interp.noEval--;
		    }
		    if (operator == OR) {
			value.intValue = 1;
		    }
		    continue;
		} else if (operator == QUESTY) {
		    /*
		     * Special note:  ?: operators must associate right to
		     * left.  To make this happen, use a precedence one lower
		     * than QUESTY when calling ExprGetValue recursively.
		     */

		    if (value.intValue != 0) {
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


	    if ((m_token < MULT) && (m_token != VALUE)
		    && (m_token != END) && (m_token != COMMA)
		    && (m_token != CLOSE_PAREN)) {
		SyntaxError(interp);
	    }

	    if (interp.noEval != 0) {
		continue;
	    }

	    /*
	     * At this point we've got two values and an operator.  Check
	     * to make sure that the particular data types are appropriate
	     * for the particular operator, and perform type conversion
	     * if necessary.
	     */

	    switch (operator) {

	    /*
	     * For the operators below, no strings are allowed and
	     * ints get converted to floats if necessary.
	     */

	    case MULT: case DIVIDE: case PLUS: case MINUS:
		if ((value.type == ExprValue.STRING)
			|| (value2.type == ExprValue.STRING)) {
		    IllegalType(interp, ExprValue.STRING, operator);
		}
		if (value.type == ExprValue.DOUBLE) {
		    if (value2.type == ExprValue.INT) {
			value2.doubleValue = value2.intValue;
			value2.type = ExprValue.DOUBLE;
		    }
		} else if (value2.type == ExprValue.DOUBLE) {
		    if (value.type == ExprValue.INT) {
			value.doubleValue = value.intValue;
			value.type = ExprValue.DOUBLE;
		    }
		}
		break;

	    /*
	     * For the operators below, only integers are allowed.
	     */

	    case MOD: case LEFT_SHIFT: case RIGHT_SHIFT:
	    case BIT_AND: case BIT_XOR: case BIT_OR:
		 if (value.type != ExprValue.INT) {
		     IllegalType(interp, value.type, operator);
		 } else if (value2.type != ExprValue.INT) {
		     IllegalType(interp, value2.type, operator);
		 }
		 break;

	    /*
	     * For the operators below, any type is allowed but the
	     * two operands must have the same type.  Convert integers
	     * to floats and either to strings, if necessary.
	     */

	    case LESS: case GREATER: case LEQ: case GEQ:
	    case EQUAL: case NEQ:
		if (value.type == ExprValue.STRING) {
		    if (value2.type != ExprValue.STRING) {
			ExprMakeString(interp, value2);
		    }
		} else if (value2.type == ExprValue.STRING) {
		    if (value.type != ExprValue.STRING) {
			ExprMakeString(interp, value);
		    }
		} else if (value.type == ExprValue.DOUBLE) {
		    if (value2.type == ExprValue.INT) {
			value2.doubleValue = value2.intValue;
			value2.type = ExprValue.DOUBLE;
		    }
		} else if (value2.type == ExprValue.DOUBLE) {
		     if (value.type == ExprValue.INT) {
			value.doubleValue = value.intValue;
			value.type = ExprValue.DOUBLE;
		    }
		}
		break;

	    /*
	     * For the operators below, no strings are allowed, but
	     * no int->double conversions are performed.
	     */

	    case AND: case OR:
		if (value.type == ExprValue.STRING) {
		    IllegalType(interp, value.type, operator);
		}
		if (value2.type == ExprValue.STRING) {
		    IllegalType(interp, value.type, operator);
		}
		break;

	    /*
	     * For the operators below, type and conversions are
	     * irrelevant:  they're handled elsewhere.
	     */

	    case QUESTY: case COLON:
		break;

	    /*
	     * Any other operator is an error.
	     */
	    default:
		throw new TclException(interp,
			"unknown operator in expression");
	    }

	    /*
	     * Carry out the function of the specified operator.
	     */

	    switch (operator) {
	    case MULT:
		if (value.type == ExprValue.INT) {
		    value.intValue = value.intValue * value2.intValue;
		} else {
		    value.doubleValue *= value2.doubleValue;
		}
		break;
	    case DIVIDE:
	    case MOD:
		if (value.type == ExprValue.INT) {
		    long divisor, quot, rem;
		    boolean negative;

		    if (value2.intValue == 0) {
		        DivideByZero(interp);
		    }

		    /*
		     * The code below is tricky because C doesn't guarantee
		     * much about the properties of the quotient or
		     * remainder, but Tcl does:  the remainder always has
		     * the same sign as the divisor and a smaller absolute
		     * value.
		     */

		    divisor = value2.intValue;
		    negative = false;
		    if (divisor < 0) {
			divisor = -divisor;
			value.intValue = -value.intValue;
			negative = true;
		    }
		    quot = value.intValue / divisor;
		    rem = value.intValue % divisor;
		    if (rem < 0) {
			rem += divisor;
			quot -= 1;
		    }
		    if (negative) {
			rem = -rem;
		    }
		    value.intValue = (operator == DIVIDE) ? quot : rem;
		} else {
		    if (value2.doubleValue == 0.0) {
			DivideByZero(interp);
		    }
		    value.doubleValue /= value2.doubleValue;
		}
		break;
	    case PLUS:
		if (value.type == ExprValue.INT) {
		    value.intValue = value.intValue + value2.intValue;
		} else {
		    value.doubleValue += value2.doubleValue;
		}
		break;
	    case MINUS:
		if (value.type == ExprValue.INT) {
		    value.intValue = value.intValue - value2.intValue;
		} else {
		    value.doubleValue -= value2.doubleValue;
		}
		break;
	    case LEFT_SHIFT:
		value.intValue <<= (int)value2.intValue;
		break;
	    case RIGHT_SHIFT:
		/*
		 * The following code is a bit tricky:  it ensures that
		 * right shifts propagate the sign bit even on machines
		 * where ">>" won't do it by default.
		 */

		if (value.intValue < 0) {
		    value.intValue =
			    ~((~value.intValue) >> value2.intValue);
		} else {
		    value.intValue >>= (int)value2.intValue;
		}
		break;
	    case LESS:
		if (value.type == ExprValue.INT) {
		    value.intValue =
			(value.intValue < value2.intValue) ? 1:0;
		} else if (value.type == ExprValue.DOUBLE) {
		    value.intValue =
			(value.doubleValue < value2.doubleValue) ? 1:0;
		} else {
		    value.intValue = (value.stringValue.compareTo(
			    value2.stringValue) < 0) ? 1:0;
		}
		value.type = ExprValue.INT;
		break;
	    case GREATER:
		if (value.type == ExprValue.INT) {
		    value.intValue =
			(value.intValue > value2.intValue) ? 1:0;
		} else if (value.type == ExprValue.DOUBLE) {
		    value.intValue =
			(value.doubleValue > value2.doubleValue) ? 1:0;
		} else {
		    value.intValue = (value.stringValue.compareTo(
			    value2.stringValue) > 0)? 1:0;
		}
		value.type = ExprValue.INT;
		break;
	    case LEQ:
		if (value.type == ExprValue.INT) {
		    value.intValue =
			(value.intValue <= value2.intValue) ? 1:0;
		} else if (value.type == ExprValue.DOUBLE) {
		    value.intValue =
			(value.doubleValue <= value2.doubleValue) ? 1:0;
		} else {
		    value.intValue = (value.stringValue.compareTo(
			    value2.stringValue) <= 0) ? 1:0;
		}
		value.type = ExprValue.INT;
		break;
	    case GEQ:
		if (value.type == ExprValue.INT) {
		    value.intValue =
			(value.intValue >= value2.intValue) ? 1:0;
		} else if (value.type == ExprValue.DOUBLE) {
		    value.intValue =
			(value.doubleValue >= value2.doubleValue) ? 1:0;
		} else {
		    value.intValue = (value.stringValue.compareTo(
			    value2.stringValue) >= 0) ? 1:0;
		}
		value.type = ExprValue.INT;
		break;
	    case EQUAL:
		if (value.type == ExprValue.INT) {
		    value.intValue =
			(value.intValue == value2.intValue) ? 1:0;
		} else if (value.type == ExprValue.DOUBLE) {
		    value.intValue =
			(value.doubleValue == value2.doubleValue) ? 1:0;
		} else {
		    value.intValue = (value.stringValue.compareTo(
			    value2.stringValue) == 0) ? 1:0;
		}
		value.type = ExprValue.INT;
		break;
	    case NEQ:
		if (value.type == ExprValue.INT) {
		    value.intValue =
			(value.intValue != value2.intValue) ? 1:0;
		} else if (value.type == ExprValue.DOUBLE) {
		    value.intValue =
			(value.doubleValue != value2.doubleValue) ? 1:0;
		} else {
		    value.intValue = (value.stringValue.compareTo(
			    value2.stringValue) != 0) ? 1:0;
		}
		value.type = ExprValue.INT;
		break;
	    case BIT_AND:
		value.intValue &= value2.intValue;
		break;
	    case BIT_XOR:
		value.intValue ^= value2.intValue;
		break;
	    case BIT_OR:
		value.intValue |= value2.intValue;
		break;

	    /*
	     * For AND and OR, we know that the first value has already
	     * been converted to an integer.  Thus we need only consider
	     * the possibility of int vs. double for the second value.
	     */

	    case AND:
		if (value2.type == ExprValue.DOUBLE) {
		    value2.intValue = (value2.doubleValue != 0) ? 1:0;
		    value2.type = ExprValue.INT;
		}
		value.intValue =
			((value.intValue!=0) && (value2.intValue!=0)) ? 1:0;
		break;
	    case OR:
		if (value2.type == ExprValue.DOUBLE) {
		    value2.intValue = (value2.doubleValue != 0) ? 1:0;
		    value2.type = ExprValue.INT;
		}
		value.intValue = 
			((value.intValue!=0) || (value2.intValue!=0)) ? 1:0;
		break;

	    case COLON:
		throw new TclException(interp, 
			"can't have : operator without ? first");
	    }
	}
    }

    /**
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

	while (m_ind < m_len && Character.isWhitespace(m_expr.charAt(m_ind))) {
	    m_ind ++;
	}
	if (m_ind >= m_len) {
	    m_token = END;
	    return null;
	}

	/*
	 * First try to parse the token as an integer or
	 * floating-point number.  Don't want to check for a number if
	 * the first character is "+" or "-".  If we do, we might
	 * treat a binary operator as unary by
	 * mistake, which will eventually cause a syntax error.
	 */
	c = m_expr.charAt(m_ind);
	if (m_ind < m_len-1) {
	    c2 = m_expr.charAt(m_ind+1);
	} else {
	    c2 = '\0';
	}

	if ((c != '+')  && (c != '-')) {
	    if (ExprLooksLikeInt(m_expr, m_len, m_ind)) {
		long value;
		StrtoulResult res = Util.strtoul(m_expr, m_ind, 0);

		if (res.errno == 0) {
		    m_ind = res.index;
		    m_token = VALUE;
		    return new ExprValue(res.value);
		} else {
		    if (res.errno == TCL.INTEGER_RANGE) {
			IntegerTooLarge(interp);
		    }
		}
	    } else {
		StrtodResult res = Util.strtod(m_expr, m_ind);
		if (res.errno == 0) {
		    m_ind = res.index;
		    m_token = VALUE;
		    return new ExprValue(res.value);
		} else {
		    if (res.errno == TCL.DOUBLE_RANGE) {
			DoubleTooLarge(interp);
		    }
		}
	    }
	}

	ParseResult pres;
	m_ind += 1;		// ind is advanced to point to the next token

	switch (c) {
	case '$':
	    m_token = VALUE;
	    pres = ParseAdaptor.parseVar(interp, m_expr, m_ind, m_len);
	    m_ind = pres.nextIndex;

	    if (interp.noEval != 0) {
		return new ExprValue(0);
	    } else {
		return ExprParseString(interp, pres.value.toString());
	    }

	case '[':
	    String str;
	    m_token = VALUE;
	    pres = ParseAdaptor.parseNestedCmd(interp, m_expr, m_ind, m_len);
	    m_ind = pres.nextIndex;
	    str = pres.value.toString();

	    if (interp.noEval != 0) {
		return new ExprValue(0);
	    } else {
		return ExprParseString(interp, str);
	    }

	case '"':
	    m_token = VALUE;

	    pres = ParseAdaptor.parseQuotes(interp, m_expr, m_ind, m_len);
	    m_ind = pres.nextIndex;
	    if (interp.noEval != 0) {
		return new ExprValue(0);
	    } else {
		return ExprParseString(interp, pres.value.toString());
	    }

	case '{':
	    m_token = VALUE;
	    pres = ParseAdaptor.parseBraces(interp, m_expr, m_ind, m_len);
	    m_ind = pres.nextIndex;
	    if (interp.noEval != 0) {
		return new ExprValue(0);
	    } else {
		return ExprParseString(interp, pres.value.toString());
	    }

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

	default:
	    if (Character.isLetter(c)) {
		/*
		 * Oops, re-adjust m_ind so that it points to the beginning
		 * of the function name.
		 */
		m_ind --;
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
	TclObject argv[] = null;
	int numArgs;

	/*
	 * Find the end of the math function's name and lookup the MathFunc
	 * record for the function.  Search until the char at m_ind is not
	 * alphanumeric or '_'
	 */

	for (; m_ind<m_len; m_ind++) {
	    if (!(Util.isLetterOrDigit(m_expr.charAt(m_ind)) ||
		  m_expr.charAt(m_ind) == '_')){
		break;
	    }
	}

	/*
	 * Get the funcName BEFORE calling ExprLex, so the funcName
	 * will not have trailing whitespace.
	 */

	funcName = m_expr.substring(startIdx, m_ind);

	/*
	 * Parse errors are thrown BEFORE unknown function names
	 */

	ExprLex(interp);
	if (m_token != OPEN_PAREN) {
	    SyntaxError(interp);
	}

	/*
	 * Now test for unknown funcName.  Doing the above statements
	 * out of order will cause some tests to fail.
	 */
	mathFunc = (MathFunction)mathFuncTable.get(funcName);
	if (mathFunc == null) {
	    throw new TclException(interp,
		    "unknown math function \"" + funcName + "\"");
	}

	/*
	 * Scan off the arguments for the function, if there are any.
	 */
	numArgs = mathFunc.argTypes.length;

	if (numArgs == 0) {
	    ExprLex(interp);
	    if (m_token != CLOSE_PAREN) {
		SyntaxError(interp);
	    }
	} else {
	    argv = new TclObject[numArgs];

	    for (int i = 0; ; i++) {
		value = ExprGetValue(interp, -1);
		if (value.type == ExprValue.STRING) {
		    throw new TclException(interp,
			"argument to math function didn't have numeric value");
		}
    
		/*
		 * Copy the value to the argument record, converting it if
		 * necessary.
		 */    
		if (value.type == ExprValue.INT) {
		    if (mathFunc.argTypes[i] == MathFunction.DOUBLE) {
			argv[i] = TclDouble.newInstance((int)value.intValue);
		    } else {
			argv[i] = TclInteger.newInstance((int)
				value.intValue);
		    }
		} else {
		    if (mathFunc.argTypes[i] == MathFunction.INT) {
			argv[i] = TclInteger.newInstance((int)
				value.doubleValue);
		    } else {
			argv[i] = TclDouble.newInstance(value.doubleValue);
		    }
		}
    
		/*
		 * Check for a comma separator between arguments or a
		 * close-paren to end the argument list.
		 */
    
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
	    return new ExprValue(0);
	} else {
	    /*
	     * Invoke the function and copy its result back into valuePtr.
	     */
	    return mathFunc.apply(interp, argv);
	}
    }

    /**
     * This procedure decides whether the leading characters of a
     * string look like an integer or something else (such as a
     * floating-point number or string).
     * @return a boolean value indicating if the string looks like an integer.
     */

    private static boolean ExprLooksLikeInt(String s, int len, int i) {
	while (i < len && Character.isWhitespace(s.charAt(i))) {
	    i ++;
	}
	if (i >= len) {
	    return false;
	}
	char c = s.charAt(i);
	if ((c == '+') || (c == '-')) {
	    i++;
	    if (i >= len) {
		return false;
	    }
	    c = s.charAt(i);
	}
	if (!Character.isDigit(c)) {
	    return false;
	}
	while (i < len && Character.isDigit(s.charAt(i))) {
	    i++;
	}
	if (i < len) {
	    c = s.charAt(i);
	    if ((c == '.') || (c == 'e') || (c == 'E')) {
		return false;
	    }
	}
	return true;
    }

    /**
     * Converts a value from int or double representation to a string.
     * @param interp interpreter to use for precision information.
     * @param value Value to be converted.
     */

    static void ExprMakeString(Interp interp, ExprValue value) {
	if (value.type == ExprValue.INT) {
	    value.stringValue = Long.toString(value.intValue);
	} else if (value.type == ExprValue.DOUBLE) {
	    value.stringValue = Double.toString(value.doubleValue);
	}
	value.type = ExprValue.STRING;
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
}

abstract class MathFunction {
    static final int INT    = 0;
    static final int DOUBLE = 1;
    static final int EITHER = 2;

    int argTypes[];

    abstract ExprValue apply(Interp interp, TclObject argv[])
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
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	return new ExprValue(Math.atan2(
		TclDouble.get(interp, argv[0]),
		TclDouble.get(interp, argv[1])));
    }
}

class AbsFunction extends MathFunction {
    AbsFunction() {
	argTypes = new int[1];
	argTypes[0] = EITHER;
    }

    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv[0].getInternalRep() instanceof TclDouble) {
	    double d = TclDouble.get(interp, argv[0]);
	    if (d>0) {
		return new ExprValue(d);
	    } else {
		return new ExprValue(-d);
	    }
	} else {
	    int i = TclInteger.get(interp, argv[0]);
	    if (i>0) {
		return new ExprValue(i);
	    } else {
		return new ExprValue(-i);
	    }
	}
    }
}

class DoubleFunction extends MathFunction {
    DoubleFunction() {
	argTypes = new int[1];
	argTypes[0] = EITHER;
    }

    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	return new ExprValue(TclDouble.get(interp, argv[0]));
    }
}

class IntFunction extends MathFunction {
    IntFunction() {
	argTypes = new int[1];
	argTypes[0] = EITHER;
    }

    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	double d = TclDouble.get(interp, argv[0]);
	Expression.checkIntegerRange(interp, d);
	return new ExprValue((int) d);
    }
}

class RoundFunction extends MathFunction {
    RoundFunction() {
	argTypes = new int[1];
	argTypes[0] = EITHER;
    }

    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv[0].getInternalRep() instanceof TclDouble) {
	    double d = TclDouble.get(interp, argv[0]);
	    if (d < 0) {
		Expression.checkIntegerRange(interp, d-0.5);
		return new ExprValue((int)(d-0.5));
	    } else {
		Expression.checkIntegerRange(interp, d+0.5);
		return new ExprValue((int)(d+0.5));
	    }
	} else {
	    return new ExprValue(TclInteger.get(interp, argv[0]));
	}
    }
}

class PowFunction extends BinaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	double d = Math.pow(
		TclDouble.get(interp, argv[0]),
		TclDouble.get(interp, argv[1]));
	Expression.checkDoubleRange(interp, d);
	return new ExprValue(d);
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
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	double d = TclDouble.get(interp, argv[0]);
	if ((d < -1) || (d > 1)) {
	    Expression.DomainError(interp);
	}
	return new ExprValue(Math.acos(d));
    }
}

class AsinFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	return new ExprValue(Math.asin(TclDouble.get(interp, argv[0])));
    }
}

class AtanFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	return new ExprValue(Math.atan(TclDouble.get(interp, argv[0])));
    }
}


class CeilFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	return new ExprValue(Math.ceil(TclDouble.get(interp, argv[0])));
    }
}


class CosFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	return new ExprValue(Math.cos(TclDouble.get(interp, argv[0])));
    }
}


class CoshFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
        double x = TclDouble.get(interp, argv[0]);
	double d1 = Math.pow(Math.E, x);
	double d2 = Math.pow(Math.E,-x);

	Expression.checkDoubleRange(interp, d1);
	Expression.checkDoubleRange(interp, d2);
	return new ExprValue((d1+d2)/2);
    }
}

class ExpFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	double d = Math.exp(TclDouble.get(interp, argv[0]));
	if ((d == Double.NaN) ||
		(d == Double.NEGATIVE_INFINITY) ||
		(d == Double.POSITIVE_INFINITY)) {
	    Expression.DoubleTooLarge(interp);
	}
	return new ExprValue(d);
    }
}


class FloorFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	return new ExprValue(Math.floor(TclDouble.get(interp, argv[0])));
    }
}


class FmodFunction extends BinaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
 	return new ExprValue(Math.IEEEremainder(TclDouble.get(interp, 
                argv[0]), TclDouble.get(interp, argv[1])));
    }
}

class HypotFunction extends BinaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	double x = TclDouble.get(interp, argv[0]);
	double y = TclDouble.get(interp, argv[1]);
	return new ExprValue(Math.sqrt( ((x * x) + (y * y)) )); 
    }
}


class LogFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	return new ExprValue(Math.log(TclDouble.get(interp, argv[0])));
    }
}


class Log10Function extends UnaryMathFunction {
   private static final double log10 = Math.log(10);
   ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	return new ExprValue(Math.log(TclDouble.get(interp, argv[0])) 
                / log10);
                
   }
}


class SinFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	return new ExprValue(Math.sin(TclDouble.get(interp, argv[0])));
    }
}


class SinhFunction extends UnaryMathFunction {
   ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	double x = TclDouble.get(interp, argv[0]);

	double d1 = Math.pow(Math.E, x);
	double d2 = Math.pow(Math.E,-x);

	Expression.checkDoubleRange(interp, d1);
	Expression.checkDoubleRange(interp, d2);

	return new ExprValue((d1-d2)/2);
    }
}


class SqrtFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	return new ExprValue(Math.sqrt(TclDouble.get(interp, argv[0])));
    }
}


class TanFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	return new ExprValue(Math.tan(TclDouble.get(interp, argv[0])));
    }
}

class TanhFunction extends UnaryMathFunction {
    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
	double x = TclDouble.get(interp, argv[0]);
	if (x == 0) {
	    return new ExprValue(0.0);
	}

	double d1 = Math.pow(Math.E, x);
	double d2 = Math.pow(Math.E,-x);

	Expression.checkDoubleRange(interp, d1);
	Expression.checkDoubleRange(interp, d2);

	return new ExprValue( (d1 - d2) / (d1 + d2));
    }
}

class RandFunction extends NoArgMathFunction {
    /*
     * Generate the random number using the linear congruential
     * generator defined by the following recurrence:
     *		seed = ( IA * seed ) mod IM
     * where IA is 16807 and IM is (2^31) - 1.  In order to avoid
     * potential problems with integer overflow, the  code uses
     * additional constants IQ and IR such that
     *		IM = IA*IQ + IR
     * For details on how this algorithm works, refer to the following
     * papers: 
     *
     *	S.K. Park & K.W. Miller, "Random number generators: good ones
     *	are hard to find," Comm ACM 31(10):1192-1201, Oct 1988
     *
     *	W.H. Press & S.A. Teukolsky, "Portable random number
     *	generators," Computers in Physics 6(5):522-524, Sep/Oct 1992.
     */

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

    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {
        return(statApply(interp, argv));
    }


    static ExprValue statApply(Interp interp, TclObject argv[])  
	    throws TclException {

	double dResult;
	int tmp;

	if (!(interp.randSeedInit)) {
	    interp.randSeedInit = true;
	    interp.randSeed = (int)date.getTime();
	}

	if (interp.randSeed == 0) {
	    /*
	     * Don't allow a 0 seed, since it breaks the generator.  Shift
	     * it to some other value.
	     */

	    interp.randSeed = 123459876;
	}

	tmp = (int)(interp.randSeed / randIQ);
	interp.randSeed = ((randIA * (interp.randSeed - tmp * randIQ))
                - randIR*tmp);
	    
	if (interp.randSeed < 0) {
	    interp.randSeed += randIM;
	}

	return new ExprValue( interp.randSeed * (1.0/randIM) );
    }
}


class SrandFunction extends UnaryMathFunction {

    ExprValue apply(Interp interp, TclObject argv[])
	    throws TclException {

        /*
	 * Reset the seed.
	 */
	interp.randSeedInit = true;
	interp.randSeed     = (long)TclDouble.get(interp, argv[0]);
	
	/*
	 * To avoid duplicating the random number generation code we simply
	 * call the static random number generator in the RandFunction 
	 * class.
	 */
	return (RandFunction.statApply(interp, null));
    }
}
