package tcl.lang;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The <code>Regex</code> class can be used to match a pattern against a string
 * and optionally replace the matched parts with new strings.
 * <p>
 * Regular expressions are handled by java.util.regex package.
 * <hr>
 * Here is an example of how to use Regex Engine
 * 
 * <pre>
 * 
 * public static void main(String[] args) throws Exception {
 * 	Regex re;
 * 	String s;
 *  int group = 1;
 * 	/*
 * 	 * A regular expression to match the first line of a HTTP request.
 * 	 *
 * 	 * 1. &circ;               - starting at the beginning of the line
 * 	 * 2. ([A-Z]+)        - match and remember some upper case characters
 * 	 * 3. [ \t]+          - skip blank space
 * 	 * 4. ([&circ; \t]*)       - match and remember up to the next blank space
 * 	 * 5. [ \t]+          - skip more blank space
 * 	 * 6. (HTTP/1\\.[01]) - match and remember HTTP/1.0 or HTTP/1.1
 * 	 * 7. $		      - end of string - no chars left.
 * 	 &#42;/
 * 	s = &quot;GET http://a.b.com:1234/index.html HTTP/1.1&quot;;
 *  
 *  // Get the Regex object - compiled and matched	
 *  re = new Regex(&quot;&circ;([A-Z]+)[ \t]+([&circ; \t]+)[ \t]+(HTTP/1\\.[01])$&quot;, s, 0, 0);
 * 	
 *  while (re.match() {
 * 	if (group <= re.groupCount()) {
 * 		System.out.println(&quot;METHOD  &quot; + re.group(group++));
 * 		System.out.println(&quot;URL     &quot; + re.group(group++));
 * 		System.out.println(&quot;VERSION &quot; + re.group(group++));
 * 	}
 * 	/*
 * 	 * A regular expression to extract some simple comma-separated data,
 * 	 * reorder some of the columns, and discard column 2.
 * 	 &#42;/
 * 	s = &quot;abc,def,ghi,klm,nop,pqr&quot;;
 * 	re = new Regexp(&quot;&circ;([&circ;,]+),([&circ;,]+),([&circ;,]+),(.*)&quot;, s, 0, 0);
 * 	System.out.println(re.replaceFist(&quot;$3,$1,$4&quot;));
 * }
 * </pre>
 * 
 * @author Radoslaw Szulgo (radoslaw@szulgo.pl)
 * @version 1.0, 2009/08/05
 * 
 * @see java.util.regex.Matcher
 * @see java.util.regex.Pattern
 */
public class Regex {

	/*
	 * Regular expression of the '&' character
	 */
	private static final String SUBS_MATCHED = "^&|([^\\\\])&|&$";

	/*
	 * Regular expression of the '\n', where n is a digit between 1 and 9
	 */
	private static final String SUBS_GROUP = "(([^\\\\])?\\\\)([0-9])";

	/* Expressions that indicate use of the boundary matcher '^' */
	private static final String REGEX_START1 = "^";
	private static final String REGEX_START2 = "|^";
	private static final String REGEX_START3 = "(^";

	/*
	 * Pattern object
	 */
	private Pattern pattern;

	/*
	 * Matcher object
	 */
	private Matcher matcher;

	/*
	 * Flags of Pattern object
	 */
	private int flags;

	/*
	 * Regular Expression string
	 */
	private String regexp;

	/*
	 * Input string
	 */
	private String string;

	/*
	 * Count of matches
	 */
	private int count;

	/*
	 * Offset of the input string
	 */
	private int offset;

	/**
	 * Constructor. It stores params in object, compiles given regexp and
	 * matches input string. Additional param 'flags' sets flags of Pattern
	 * object that compiles regexp.
	 * 
	 * @param regexp
	 *            regular expression
	 * @param string
	 *            input string
	 * @param offset
	 *            offset of the input string
	 * @param flags
	 *            flags of pattern object that compiles regexp
	 * @throws PatternSyntaxException
	 *             thrown when there is an error during regexp compilation
	 */
	public Regex(String regexp, String string, int offset, int flags)
			throws PatternSyntaxException {
		this.flags = flags;
		count = 0;
		this.regexp = regexp;
		this.string = string;
		this.offset = offset;

		try {
			pattern = Pattern.compile(regexp, flags);
		} catch (PatternSyntaxException ex) {
			// handling exception by caller
			throw ex;
		}

		matcher = pattern.matcher(string);
	}

	/**
	 * Constructor. It stores params in object, compiles given regexp and
	 * matches input string.
	 * 
	 * @param regexp
	 *            regular expression
	 * @param string
	 *            input string
	 * @param offset
	 *            offset of the input string
	 * @throws PatternSyntaxException
	 *             thrown when there is an error during regexp compilation
	 */
	public Regex(String regexp, String string, int offset)
			throws PatternSyntaxException {
		flags = 0;
		count = 0;
		this.regexp = regexp;
		this.string = string;
		this.offset = offset;

		try {
			pattern = Pattern.compile(regexp);
		} catch (PatternSyntaxException ex) {
			// handling exception by caller
			throw ex;
		}

		matcher = pattern.matcher(string);
	}

	/**
	 * 
	 * @return
	 */
	public boolean match() {
		// if offset is a non-zero value,
		// and regex has '^', it will surely not match

		if ((pattern.flags() & Pattern.MULTILINE) == 0
				&& (offset != 0)
				&& (regexp.startsWith(REGEX_START1)
						|| regexp.indexOf(REGEX_START2) != -1 || regexp
						.indexOf(REGEX_START3) != -1)) {
			return false;
		} else {

			// check if offset is in boundaries of string length
			if (offset > string.length()) {
				offset = string.length();
			}

			return matcher.find(offset);
		}
	}

	/**
	 * Replaces the first subsequence of the input sequence that matches the
	 * pattern with the given replacement string.
	 * 
	 * @param subSpec
	 *            replacement string
	 * @return The string constructed by replacing the first matching
	 *         subsequence by the replacement string, substituting captured
	 *         subsequences as needed
	 */
	public String replaceFirst(String subSpec) {
		String result;

		boolean matches = matcher.find();
		
		// we replace first matched occurence in the substring of the input string
		result = pattern.matcher(string.substring(offset)).replaceFirst(subSpec);

		// if offset is set then we must join the substring that was
		// removed ealier (during matching)
		if (offset != 0) {
			result = string.substring(0, offset) + result;
		}
		// hack for Java's backslash interpretation e.g. '\\' is printed
		// as '\' - we don't want that
		if (result.indexOf("\\") != -1) {
			int start = result.indexOf("\\");
			result = result.substring(0, start) + "\\\\"
					+ result.substring(start + 1, result.length());
		}

		if (result == null || result.length() == 0 || !matches) {
			// if no match, return non-changed string
			result = string;
		} else {
			// if a replacement was done, increment count of matches
			count++;
		}

		return result;
	}

	/**
	 * Replaces every subsequence of the input sequence that matches the pattern
	 * with the given replacement string.
	 * 
	 * @param subSpec
	 *            the replacement string
	 * @return The string constructed by replacing each matching subsequence by
	 *         the replacement string, substituting captured subsequences as
	 *         needed
	 */
	public String replaceAll(String subSpec) {
		String result = null;
		StringBuffer sb = new StringBuffer();
		int i = offset;
		// we replace first matched occurence in the substring of the input string
		Matcher tempMatcher = pattern.matcher(string.substring(offset));
		while (tempMatcher.find()) {
			count++;
			tempMatcher.appendReplacement(sb, subSpec);
			i += tempMatcher.end();
			tempMatcher = pattern.matcher(string.substring(i));
		}

		tempMatcher.appendTail(sb);

		// if offset is set then we must join the substring that was
		// removed ealier (during matching)
		if (offset != 0) {
			result = string.substring(0, offset) + sb.toString();
		} else {
			result = sb.toString();
		}

		return result;

	}

	/**
	 * Returns a list containing information about the regular expression. The
	 * first element of the list is a subexpression count. The second element is
	 * a list of property names that describe various attributes of the regular
	 * expression. Actually, properties are flags of Pattern object used in
	 * regexp.
	 * 
	 * Primarily intended for debugging purposes.
	 * 
	 * @param interp
	 *            current Jacl interpreter object
	 * @return A list containing information about the regular expression.
	 * @throws TclException
	 */
	public TclObject getInfo(Interp interp) throws TclException {
		TclObject props = TclList.newInstance();
		String groupCount = String.valueOf(matcher.groupCount());
		int f = pattern.flags();

		try {
			TclList.append(interp, props, TclString.newInstance(groupCount));

			if ((f | Pattern.CANON_EQ) != 0) {
				TclList
						.append(interp, props, TclString
								.newInstance("CANON_EQ"));
			}

			if ((f | Pattern.CASE_INSENSITIVE) != 0) {
				TclList.append(interp, props, TclString
						.newInstance("CASE_INSENSITIVE"));
			}

			if ((f | Pattern.COMMENTS) != 0) {
				TclList
						.append(interp, props, TclString
								.newInstance("COMMENTS"));
			}

			if ((f | Pattern.DOTALL) != 0) {
				TclList.append(interp, props, TclString.newInstance("DOTALL"));
			}

			if ((f | Pattern.MULTILINE) != 0) {
				TclList.append(interp, props, TclString
						.newInstance("MULTILINE"));
			}

			if ((f | Pattern.UNICODE_CASE) != 0) {
				TclList.append(interp, props, TclString
						.newInstance("UNICODE_CASE"));
			}

			if ((f | Pattern.UNIX_LINES) != 0) {
				TclList.append(interp, props, TclString
						.newInstance("UNIX_LINES"));
			}

		} catch (TclException e) {
			// handling exception by caller
			throw e;
		}

		return props;
	}

	/**
	 * Parses the replacement string (subSpec param) which is in Tcl's form.
	 * This method replaces Tcl's '&' and '\n' where 'n' is a number 0-9. to
	 * Java's reference characters:
	 * 
	 * The replacement string (subSpec param) may contain references to
	 * subsequences captured during the previous match: Each occurrence of $g
	 * will be replaced by the result of evaluating group(g). The first number
	 * after the $ is always treated as part of the group reference. Subsequent
	 * numbers are incorporated into g if they would form a legal group
	 * reference. Only the numerals '0' through '9' are considered as potential
	 * components of the group reference. If the second group matched the string
	 * "foo", for example, then passing the replacement string "$2bar" would
	 * cause "foobar" to be appended to the string buffer. A dollar sign ($) may
	 * be included as a literal in the replacement string by preceding it with a
	 * backslash (\$).
	 * 
	 * @param subSpec
	 *            The replacement string
	 * @return The replacement string in Java's form
	 */
	public static String parseSubSpec(String subSpec) {
		Pattern pattern = Pattern.compile(SUBS_MATCHED);
		Matcher matcher = pattern.matcher(subSpec);
		StringBuffer sb = new StringBuffer();

		while (matcher.find()) {
			String temp = matcher.group(1);
			matcher.appendReplacement(sb, (temp != null ? temp : "") + "\\$0");
		}
		matcher.appendTail(sb);
		pattern = Pattern.compile(SUBS_GROUP);
		matcher = pattern.matcher(sb.toString());
		sb = new StringBuffer();

		while (matcher.find()) {
			String temp = matcher.group(2);
			matcher.appendReplacement(sb, (temp != null ? temp : "") + "\\$"
					+ matcher.group(3));
		}
		matcher.appendTail(sb);

		return sb.toString();
	}

	/**
	 * Returns the number of capturing groups in this matcher's pattern.
	 * 
	 * @return Returns the number of capturing groups in this matcher's pattern.
	 * @see java.util.regex.Matcher#groupCount()
	 * 
	 */
	public int groupCount() {
		return matcher.groupCount();
	}

	/**
	 * Returns the start index of the previous match.
	 * 
	 * @return The index of the first character matched
	 * @see java.util.regex.Matcher#start()
	 * 
	 */
	public int start() {
		return matcher.start();
	}

	/**
	 * Returns the start index of the subsequence captured by the given group
	 * during the previous match operation.
	 * 
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 * @return The index of the first character matched
	 * @see java.util.regex.Matcher#start(int)
	 * 
	 */
	public int start(int group) {
		return matcher.start(group);
	}

	/**
	 * Returns the index of the last character matched, plus one.
	 * 
	 * @return The index of the last character matched, plus one
	 * @see java.util.regex.Matcher#end()
	 */
	public int end() {
		return matcher.end();
	}

	/**
	 * Returns the index of the last character, plus one, of the subsequence
	 * captured by the given group during the previous match operation.
	 * 
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 * @return The index of the last character captured by the group, plus one,
	 *         or -1 if the match was successful but the group itself did not
	 *         match anything
	 * @see java.util.regex.Matcher#end(int)
	 */
	public int end(int group) {
		return matcher.end(group);
	}

	/**
	 * Returns the input subsequence matched by the previous match.
	 * 
	 * @return The (possibly empty) subsequence matched by the previous match,
	 *         in string form
	 * @see java.util.regex.Matcher#group()
	 */
	public String group() {
		return matcher.group();
	}

	/**
	 * Returns the input subsequence captured by the given group during the
	 * previous match operation.
	 * 
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 * @return The (possibly empty) subsequence captured by the group during the
	 *         previous match, or null if the group failed to match part of the
	 *         input
	 * @see java.util.regex.Matcher#group(int)
	 */
	public String group(int group) {
		return matcher.group(group);
	}

	/**
	 * @return the pattern object
	 */
	public Pattern getPattern() {
		return pattern;
	}

	/**
	 * @return the matcher object
	 */
	public Matcher getMatcher() {
		return matcher;
	}

	/**
	 * @return the flags of the pattern object
	 */
	public int getFlags() {
		return flags;
	}

	/**
	 * @return the regexp string
	 */
	public String getRegexp() {
		return regexp;
	}

	/**
	 * @return the input string
	 */
	public String getString() {
		return string;
	}

	/**
	 * Returns the count of correctly matched subsequences of the input string
	 * 
	 * @return the count of correctly matched subsequences of the input string
	 */
	public int getCount() {
		return count;
	}

	/**
	 * @return the offset of the input string
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * @param offset
	 *            the offset to set
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}
}
