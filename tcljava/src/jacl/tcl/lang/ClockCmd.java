/*
 * ClockCmd.java --
 *
 *	Implements the built-in "clock" Tcl command.
 *
 * Copyright (c) 1998 Christian Krone.
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1995-1997 Sun Microsystems, Inc.
 * Copyright (c) 1992-1995 Karl Lehenbauer and Mark Diekhans.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ClockCmd.java,v 1.1 1999/05/08 05:32:55 dejong Exp $
 *
 */

package tcl.lang;

import java.util.*;
import java.text.*;

/**
 * This class implements the built-in "clock" command in Tcl.
 */

class ClockCmd implements Command {

    static final private String validCmds[] = {
	"clicks",
	"format",
	"scan",
	"seconds"
    };

    static final private int CMD_CLICKS 	= 0;
    static final private int CMD_FORMAT  	= 1;
    static final private int CMD_SCAN		= 2;
    static final private int CMD_SECONDS	= 3;

    static final private String formatOpts[] = {
	"-format",
	"-gmt"
    };

    static final private int OPT_FORMAT_FORMAT 	= 0;
    static final private int OPT_FORMAT_GMT	= 1;

    static final private String scanOpts[] = {
	"-base",
	"-gmt"
    };

    static final private int OPT_SCAN_BASE 	= 0;
    static final private int OPT_SCAN_GMT	= 1;

    static final int EPOCH_YEAR = 1970;

/**
 *----------------------------------------------------------------------
 *
 * cmdProc --
 *
 *	This procedure is invoked as part of the Command interface to
 *	process the "clock" Tcl command.  See the user documentation
 *	for details on what it does.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	See the user documentation.
 *
 *----------------------------------------------------------------------
 */

public void
cmdProc(
    Interp interp,		// Current interpreter.
    TclObject argv[])		// Argument list.
throws 
    TclException 		// A standard Tcl exception.
{
    int clockVal;		// Time value as seconds of epoch.
    String dateString;		// Time value as string.
    int argIx;			// Counter over arguments.
    String format = null;	// User specified format string.
    boolean useGmt = false;	// User specified flag to use gmt.
    TclObject baseObj = null;	// User specified raw value of baseClock.
    Date baseClock;		// User specified time value.
    Date date;			// Parsed date value.

    if (argv.length < 2) {
	throw new TclNumArgsException(interp, 1, argv, "option ?arg ...?");
    }
    int cmd = TclIndex.get(interp, argv[1], validCmds, "option", 0);

    switch (cmd) {
	case CMD_CLICKS: {
	    if (argv.length != 2) {
		throw new TclNumArgsException(interp, 2, argv, null);
	    }
	    //long millis = new java.util.Date().getTime();
	    long millis = System.currentTimeMillis();
	    int clicks = (int)(millis%Integer.MAX_VALUE);
	    interp.setResult(clicks);
	    break;
	}

	case CMD_FORMAT: {
	    if ((argv.length < 3) || (argv.length > 7)) {
		throw new TclNumArgsException(interp, 2, argv,
		    "clockval ?-format string? ?-gmt boolean?");
	    }
	    clockVal = TclInteger.get(interp, argv[2]);

	    for (argIx = 3; argIx+1 < argv.length; argIx += 2) {
	        int formatOpt = TclIndex.get(interp, argv[argIx],
				    formatOpts, "switch", 0);
	        switch (formatOpt) {
		    case OPT_FORMAT_FORMAT: {
		        format = argv[argIx+1].toString();
		        break;
		    }
		    case OPT_FORMAT_GMT: {
		        useGmt = TclBoolean.get(interp, argv[argIx+1]);
		        break;
		    }
		}
	    }
	    if (argIx < argv.length) {
		throw new TclNumArgsException(interp, 2, argv,
		    "clockval ?-format string? ?-gmt boolean?");
	    }
	    FormatClock(interp, clockVal, useGmt, format);
	    break;
	}

	case CMD_SCAN: {
	    if ((argv.length < 3) || (argv.length > 7)) {
		throw new TclNumArgsException(interp, 2, argv,
		    "dateString ?-base clockValue? ?-gmt boolean?");
	    }
	    dateString = argv[2].toString();

	    for (argIx = 3; argIx+1 < argv.length; argIx += 2) {
	        int scanOpt = TclIndex.get(interp, argv[argIx],
				  scanOpts, "switch", 0);
	        switch (scanOpt) {
		    case OPT_SCAN_BASE: {
		        baseObj = argv[argIx+1];
		        break;
		    }
		    case OPT_SCAN_GMT: {
		        useGmt = TclBoolean.get(interp, argv[argIx+1]);
		        break;
		    }
		}
	    }
	    if (argIx < argv.length) {
		throw new TclNumArgsException(interp, 2, argv,
		    "clockval ?-format string? ?-gmt boolean?");
	    }
	    if (baseObj != null) {
	        baseClock = new Date(TclInteger.get(interp, baseObj)*1000);
	    } else {
	        baseClock = new Date();
	    }

	    date = GetDate(dateString, baseClock, useGmt);
	    if (date == null) {
	        throw new TclException(interp,
			      "unable to convert date-time string \"" +
			      dateString + "\"");
	    }

	    int seconds = (int)(date.getTime()/1000);
	    interp.setResult(seconds);
	    break;
	}

	case CMD_SECONDS: {
	    if (argv.length != 2) {
		throw new TclNumArgsException(interp, 2, argv, null);
	    }
	    long millis = new java.util.Date().getTime();
	    int seconds = (int)(millis/1000);
	    interp.setResult(seconds);
	    break;
	}
    }
}

/**
 *-----------------------------------------------------------------------------
 *
 * FormatClock --
 *
 *      Formats a time value based on seconds into a human readable
 *	string.
 *
 * Results:
 *      None.
 *
 * Side effects:
 *      The interpreter will contain the formatted string as result.
 *
 *-----------------------------------------------------------------------------
 */

private void
FormatClock(
    Interp interp,		// Current interpreter.
    int clockVal,	       	// Time in seconds.
    boolean useGMT,		// Boolean
    String format)		// Format string
throws 
    TclException 		// A standard Tcl exception.
{
    Date date = new Date((long)clockVal*1000);
    Calendar calendar = Calendar.getInstance();
    SimpleDateFormat fmt, locFmt;
    FieldPosition fp = new FieldPosition(0);

    if (format == null) {
	format = new String ("%a %b %d %H:%M:%S %Z %Y");
    }

    calendar.setTime(date);
    if (useGMT) {
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    fmt = new SimpleDateFormat("mm.dd.yy", Locale.US);
    fmt.setCalendar(calendar);
	
    StringBuffer result = new StringBuffer();
    for (int ix = 0; ix < format.length(); ix++) {
        if (format.charAt(ix) == '%' && ix+1 < format.length()) {
	    switch (format.charAt(++ix)) {
	        case '%': // Insert a %. 
		    result.append('%');
		    break;
	        case 'a': // Abbreviated weekday name (Mon, Tue, etc.). 
		    fmt.applyPattern("EEE");
		    fmt.format(date, result, fp);
		    break;
	        case 'A': // Full weekday name (Monday, Tuesday, etc.). 
		    fmt.applyPattern("EEEE");
		    fmt.format(date, result, fp);
		    break;
	        case 'b': case 'h': // Abbreviated month name (Jan,Feb,etc.). 
		    fmt.applyPattern("MMM");
		    fmt.format(date, result, fp);
		    break;
	        case 'B': // Full month name. 
		    fmt.applyPattern("MMMM");
		    fmt.format(date, result, fp);
		    break;
	        case 'c': // Locale specific date and time. 
		    locFmt = (SimpleDateFormat)DateFormat.getDateTimeInstance(
			          DateFormat.SHORT, DateFormat.SHORT);
		    locFmt.setCalendar(calendar);
		    locFmt.format(date, result, fp);
		    break;
	        case 'C': // Century (00 - 99).
		    int century = calendar.get(Calendar.YEAR)/100;
		    result.append((century < 10 ? "0" : "") + century);
		    break;
	        case 'd': // Day of month (01 - 31). 
		    fmt.applyPattern("dd");
		    fmt.format(date, result, fp);
		    break;
	        case 'D': // Date as %m/%d/%y. 
		    fmt.applyPattern("MM/dd/yy");
		    fmt.format(date, result, fp);
		    break;
	        case 'e': // Day of month (1 - 31), no leading zeros. 
		    fmt.applyPattern("d");
		    String day = fmt.format(date);
		    result.append((day.length() < 2 ? " " : "") + day);
		    break;
	        case 'H': // Hour in 24-hour format (00 - 23). 
		    fmt.applyPattern("HH");
		    fmt.format(date, result, fp);
		    break;
	        case 'I': // Hour in 12-hour format (01 - 12). 
		    fmt.applyPattern("hh");
		    fmt.format(date, result, fp);
		    break;
	        case 'j': // Day of year (001 - 366). 
		    fmt.applyPattern("DDD");
		    fmt.format(date, result, fp);
		    break;
	        case 'k': // Hour in 24-hour format (0 - 23), no leading zeros. 
		    fmt.applyPattern("H");
		    String h24 = fmt.format(date);
		    result.append((h24.length() < 2 ? " " : "") + h24);
		    break;
	        case 'l': // Hour in 12-hour format (1 - 12), no leading zeros. 
		    fmt.applyPattern("h");
		    String h12 = fmt.format(date);
		    result.append((h12.length() < 2 ? " " : "") + h12);
		    break;
	        case 'm': // Month number (01 - 12). 
		    fmt.applyPattern("MM");
		    fmt.format(date, result, fp);
		    break;
	        case 'M': // Minute (00 - 59). 
		    fmt.applyPattern("mm");
		    fmt.format(date, result, fp);
		    break;
	        case 'n': // Insert a newline. 
		    result.append('\n');
		    break;
	        case 'p': // AM/PM indicator. 
		    fmt.applyPattern("aa");
		    fmt.format(date, result, fp);
		    break;
	        case 'r': // Time as %I:%M:%S %p. 
		    fmt.applyPattern("KK:mm:ss aaaa");
		    fmt.format(date, result, fp);
		    break;
	        case 'R': // Time as %H:%M. 
		    fmt.applyPattern("hh:mm");
		    fmt.format(date, result, fp);
		    break;
	        case 's': // seconds since epoch. 
		    long millis = calendar.getTime().getTime();
		    if (useGMT) {
		        Calendar localCalendar = Calendar.getInstance();
			localCalendar.setTime(calendar.getTime());
			millis -= localCalendar.get(Calendar.ZONE_OFFSET)
			  	+ localCalendar.get(Calendar.DST_OFFSET);
		    }
		    result.append((int)(millis/1000));
		    break;
	        case 'S': // Seconds (00 - 59). 
		    fmt.applyPattern("ss");
		    fmt.format(date, result, fp);
		    break;
	        case 't': // Insert a tab. 
		    result.append('\t');
		    break;
	        case 'T': // Time as %H:%M:%S. 
		    fmt.applyPattern("hh:mm:ss");
		    fmt.format(date, result, fp);
		    break;
	        case 'u': // Weekday number (1 - 7) Sunday = 7. 
		    int dayOfWeek17 = calendar.get(Calendar.DAY_OF_WEEK);
		    if (dayOfWeek17 == calendar.SUNDAY) {
		        result.append(7);
		    } else {
		        result.append(dayOfWeek17 - Calendar.SUNDAY);
		    }
		    break;
	        case 'U': // Week of year (01-52), Sunday is first day.
		    int weekS = GetWeek(calendar, Calendar.SUNDAY, false);
		    result.append((weekS < 10 ? "0" : "") + weekS);
		    break;
	        case 'V': // ISO 8601 Week Of Year (01 - 53). 
		    int isoWeek = GetWeek(calendar, Calendar.MONDAY, true);
		    result.append((isoWeek < 10 ? "0" : "") + isoWeek);
		    break;
	        case 'w': // Weekday number (0 - 6) Sunday = 0. 
		    int dayOfWeek06 = calendar.get(Calendar.DAY_OF_WEEK);
		    result.append(dayOfWeek06-calendar.SUNDAY);
		    break;
	        case 'W': // Week of year (01-52), Monday is first day. 
		    int weekM = GetWeek(calendar, Calendar.MONDAY, false);
		    result.append((weekM < 10 ? "0" : "") + weekM);
		    break;
	        case 'x': // Locale specific date format. 
		    locFmt = (SimpleDateFormat)DateFormat.getDateInstance(
						   DateFormat.SHORT);
		    locFmt.setCalendar(calendar);
		    locFmt.format(date, result, fp);
		    break;
	        case 'X': // Locale specific time format. 
		    locFmt = (SimpleDateFormat)DateFormat.getTimeInstance(
						   DateFormat.SHORT);
		    locFmt.setCalendar(calendar);
		    locFmt.format(date, result, fp);
		    break;
	        case 'y': // Year without century (00 - 99). 
		    fmt.applyPattern("yy");
		    fmt.format(date, result, fp);
		    break;
	        case 'Y': // Year with century (e.g. 1990) 
		    fmt.applyPattern("yyyy");
		    fmt.format(date, result, fp);
		    break;
	        case 'Z': // Time zone name. 
		    fmt.applyPattern("zzz");
		    fmt.format(date, result, fp);
		    break;
	        default:
		    result.append(format.charAt(ix));
		    break;
	    }
	} else {
	  result.append(format.charAt(ix));
	}
    }
    interp.setResult(result.toString());
}

/**
 *-----------------------------------------------------------------------------
 *
 * GetWeek --
 *
 *      Returns the week_of_year of the given date.
 *	The weekday considered as start of the week is given as argument.
 *	Specify iso as true to get the week_of_year accourding to ISO.
 *
 * Results:
 *      Day of the week .
 *
 * Side effects:
 *      The interpreter will contain the formatted string as result.
 *
 *-----------------------------------------------------------------------------
 */

private int
GetWeek(
    Calendar calendar,		// Calendar containing Date.
    int firstDayOfWeek,		// this day starts a week (MONDAY/SUNDAY).
    boolean iso			// evaluate according to ISO?
)
{
    if (iso) {
        firstDayOfWeek = Calendar.MONDAY;
    }

    // After changing the firstDayOfWeek, we have to set the time value anew,
    // so that the fields of the calendar are recalculated.


    calendar.setFirstDayOfWeek(firstDayOfWeek);
    calendar.setMinimalDaysInFirstWeek(iso ? 4 : 7);
    calendar.setTime(calendar.getTime());
    int week = calendar.get(Calendar.WEEK_OF_YEAR);

    if (!iso) {
	// The week for the first days of the year may be 52 or 53.
	// But here we have to return 0, if we don't compute ISO week.
	// So any bigger than 50th week in January will become 00.

	if (calendar.get(Calendar.MONTH) == Calendar.JANUARY && week > 50) {
	    week = 0;
	}
    }

    return week;
}

/**
 *-----------------------------------------------------------------------------
 *
 * GetDate --
 *
 *      Scan a human readable date string and construct a Date.
 *
 * Results:
 *      The scanned date (or null, if an error occured).
 *
 * Side effects:
 *      None.
 *
 *-----------------------------------------------------------------------------
 */

private Date
GetDate(
    String dateString,		// Date string to scan
    Date baseDate,		// Date to use as base
    boolean useGMT)		// Boolean
{
    Calendar calendar = Calendar.getInstance();
    Calendar now = Calendar.getInstance();
    now.setTime(baseDate);
    calendar.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH),
	now.get(Calendar.DAY_OF_MONTH), 0, 0 ,0);
    if (useGMT) {
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    ClockToken[] dt = GetTokens(dateString, false);

    ParsePosition parsePos = new ParsePosition(0);
    ClockRelTimespan diff = new ClockRelTimespan();
    int hasTime = 0;
    int hasZone = 0;
    int hasDate = 0;
    int hasDay = 0;
    int hasRel = 0;

    while (parsePos.getIndex() < dt.length) {
        if (ParseTime(dt, parsePos, calendar)) {
	    hasTime++;
	} else if (ParseZone(dt, parsePos, calendar)) {
	    hasZone++;
	} else if (ParseDate(dt, parsePos, calendar)) {
	    hasDate++;
	} else if (ParseDay(dt, parsePos, calendar)) {
	    hasDay++;
	} else if (ParseRel(dt, parsePos, diff)) {
	    hasRel++;
	} else if (ParseNumber(dt, parsePos, calendar,
			       hasDate > 0 && hasTime > 0 && hasRel == 0)) {
	    if (hasDate == 0 || hasTime == 0 || hasRel > 0) {
	        hasTime++;
	    }
	} else {
	    return null;
	}
    }

    if (hasTime > 1 || hasZone > 1 || hasDate > 1 || hasDay > 1) {
        return null;
    }

    // The following line handles years that are specified using
    // only two digits.  The line of code below implements a policy
    // defined by the X/Open workgroup on the millinium rollover.
    // Note: some of those dates may not actually be valid on some
    // platforms.  The POSIX standard startes that the dates 70-99
    // shall refer to 1970-1999 and 00-38 shall refer to 2000-2038.
    // This later definition should work on all platforms.

    int thisYear = calendar.get(Calendar.YEAR);
    if (thisYear < 100) {
        if (thisYear >= 69) {
	    calendar.set(Calendar.YEAR, thisYear+1900);
	} else {
	    calendar.set(Calendar.YEAR, thisYear+2000);
	}
    }

    if (hasRel > 0) {
        if (hasTime == 0 && hasDate == 0 && hasDay == 0) {
	    calendar.setTime(baseDate);
	}
	calendar.add(Calendar.SECOND, diff.getSeconds());
	calendar.add(Calendar.MONTH, diff.getMonths());
    }

    return calendar.getTime();
}

/**
 *-----------------------------------------------------------------------------
 *
 * ParseTime --
 *
 *      Parse a time string and sets the Calendar.
 *	A time string is valid, if it confirms to the following yacc rule:
 *	time    : tUNUMBER tMERIDIAN
 *	        | tUNUMBER ':' tUNUMBER o_merid
 *	        | tUNUMBER ':' tUNUMBER tSNUMBER
 *	        | tUNUMBER ':' tUNUMBER ':' tUNUMBER o_merid
 *	        | tUNUMBER ':' tUNUMBER ':' tUNUMBER tSNUMBER
 *	        ;
 *
 * Results:
 *      True, if a time was read (parsePos was incremented and calendar
 *	was set according to the read time); false otherwise.
 *
 * Side effects:
 *      None.
 *
 *-----------------------------------------------------------------------------
 */

private boolean
ParseTime (
    ClockToken[] dt,		// Input as scanned array of tokens
    ParsePosition parsePos,	// Current position in input
    Calendar calendar		// calendar object to set
)
{
    int pos = parsePos.getIndex();

    if (pos+5 < dt.length &&
	dt[pos].isUNumber() &&
	dt[pos+1].is(':') &&
	dt[pos+2].isUNumber() &&
	dt[pos+3].is(':') &&
	dt[pos+4].isUNumber() &&
	dt[pos+5].isSNumber()) {
	calendar.set(Calendar.HOUR, dt[pos].getInt());
	calendar.set(Calendar.MINUTE, dt[pos+2].getInt());
	calendar.set(Calendar.SECOND, dt[pos+4].getInt());
        parsePos.setIndex(pos+6);
        return true;
    }
    if (pos+4 < dt.length &&
	dt[pos].isUNumber() &&
	dt[pos+1].is(':') &&
	dt[pos+2].isUNumber() &&
	dt[pos+3].is(':') &&
	dt[pos+4].isUNumber()) {
        parsePos.setIndex(pos+5);
	ParseMeridianAndSetHour(dt, parsePos, calendar, dt[pos].getInt());
	calendar.set(Calendar.MINUTE, dt[pos+2].getInt());
	calendar.set(Calendar.SECOND, dt[pos+4].getInt());
        return true;
    } 
    if (pos+3 < dt.length &&
	dt[pos].isUNumber() &&
	dt[pos+1].is(':') &&
	dt[pos+2].isUNumber() &&
	dt[pos+3].isSNumber()) {
	calendar.set(Calendar.HOUR, dt[pos].getInt());
	calendar.set(Calendar.MINUTE, dt[pos+2].getInt());
        parsePos.setIndex(pos+4);
        return true;
    }
    if (pos+2 < dt.length &&
	dt[pos].isUNumber() &&
	dt[pos+1].is(':') &&
	dt[pos+2].isUNumber()) {
        parsePos.setIndex(pos+3);
	ParseMeridianAndSetHour(dt, parsePos, calendar, dt[pos].getInt());
	calendar.set(Calendar.MINUTE, dt[pos+2].getInt());
        return true;
    }
    if (pos+1 < dt.length &&
	dt[pos].isUNumber() &&
	dt[pos+1].is(ClockToken.MERIDIAN)) {
        parsePos.setIndex(pos+1);
	ParseMeridianAndSetHour(dt, parsePos, calendar, dt[pos].getInt());
        return true;
    }
    return false;
}

/**
 *-----------------------------------------------------------------------------
 *
 * ParseZone --
 *
 *      Parse a timezone string and sets the Calendar.
 *	A timezone string is valid, if it confirms to the following yacc rule:
 *	zone    : tZONE tDST
 *	        | tZONE
 *	        | tDAYZONE
 *	        ;
 *
 * Results:
 *      True, if a timezone was read (parsePos was incremented and calendar
 *	was set according to the read timezone); false otherwise.
 *
 * Side effects:
 *      None.
 *
 *-----------------------------------------------------------------------------
 */

private boolean
ParseZone (
    ClockToken[] dt,		// Input as scanned array of tokens
    ParsePosition parsePos,	// Current position in input
    Calendar calendar		// calendar object to set
)
{
    int pos = parsePos.getIndex();

    if (pos+1 < dt.length &&
	dt[pos].is(ClockToken.ZONE) &&
	dt[pos+1].is(ClockToken.DST)) {
        calendar.setTimeZone(dt[pos].getZone());
        parsePos.setIndex(pos+2);
        return true;
    }
    if (pos < dt.length &&
	dt[pos].is(ClockToken.ZONE)) {
        calendar.setTimeZone(dt[pos].getZone());
        parsePos.setIndex(pos+1);
        return true;
    }
    if (pos < dt.length &&
	dt[pos].is(ClockToken.DAYZONE)) {
        calendar.setTimeZone(dt[pos].getZone());
        parsePos.setIndex(pos+1);
        return true;
    }
    return false;
}

/**
 *-----------------------------------------------------------------------------
 *
 * ParseDay --
 *
 *      Parse a day string and sets the Calendar.
 *	A day string is valid, if it confirms to the following yacc rule:
 *	day     : tDAY
 *	        | tDAY ','
 *	        | tUNUMBER tDAY
 *	        ;
 *
 * Results:
 *      True, if a day was read (parsePos was incremented and calendar
 *	was set according to the read day); false otherwise.
 *
 * Side effects:
 *      None.
 *
 *-----------------------------------------------------------------------------
 */

private boolean
ParseDay (
    ClockToken[] dt,		// Input as scanned array of tokens
    ParsePosition parsePos,	// Current position in input
    Calendar calendar		// calendar object to set
)
{
    int pos = parsePos.getIndex();

    if (pos+1 < dt.length &&
	dt[pos].is(ClockToken.DAY) &&
	dt[pos+1].is(',')) {
	calendar.set(Calendar.DAY_OF_MONTH, dt[pos].getInt());
        parsePos.setIndex(pos+2);
        return true;
    }
    if (pos+1 < dt.length &&
	dt[pos].isUNumber() &&
	dt[pos+1].is(ClockToken.DAY)) {
	calendar.set(Calendar.DAY_OF_MONTH, dt[pos+1].getInt());
        parsePos.setIndex(pos+2);
        return true;
    }
    if (pos < dt.length &&
	dt[pos].is(ClockToken.DAY)) {
	calendar.set(Calendar.DAY_OF_MONTH, dt[pos].getInt());
        parsePos.setIndex(pos+1);
        return true;
    }
    return false;
}

/**
 *-----------------------------------------------------------------------------
 *
 * ParseDate --
 *
 *      Parse a date string and sets the Calendar.
 *	A date string is valid, if it confirms to the following yacc rule:
 *	date	: tUNUMBER '/' tUNUMBER
 *		| tUNUMBER '/' tUNUMBER '/' tUNUMBER
 *		| tMONTH tUNUMBER
 *		| tMONTH tUNUMBER ',' tUNUMBER
 *		| tUNUMBER tMONTH
 *		| tEPOCH
 *		| tUNUMBER tMONTH tUNUMBER
 *		;
 *
 * Results:
 *      True, if a date was read (parsePos was incremented and calendar
 *	was set according to the read day); false otherwise.
 *
 * Side effects:
 *      None.
 *
 *-----------------------------------------------------------------------------
 */

private boolean
ParseDate (
    ClockToken[] dt,		// Input as scanned array of tokens
    ParsePosition parsePos,	// Current position in input
    Calendar calendar		// calendar object to set
)
{
    int pos = parsePos.getIndex();

    if (pos+4 < dt.length &&
	dt[pos].isUNumber() &&
	dt[pos+1].is('/') &&
	dt[pos+2].isUNumber() &&
	dt[pos+3].is('/') &&
	dt[pos+4].isUNumber()) {
	calendar.set(Calendar.DAY_OF_MONTH, dt[pos+2].getInt());
	calendar.set(Calendar.MONTH, dt[pos].getInt()-1);
	calendar.set(Calendar.YEAR, dt[pos+4].getInt());
        parsePos.setIndex(pos+5);
        return true;
    }
    if (pos+3 < dt.length &&
	dt[pos].is(ClockToken.MONTH) &&
	dt[pos+1].isUNumber() &&
	dt[pos+2].is(',') &&
	dt[pos+3].isUNumber()) {
	calendar.set(Calendar.DAY_OF_MONTH, dt[pos+1].getInt());
	calendar.set(Calendar.MONTH, dt[pos].getInt());
	calendar.set(Calendar.YEAR, dt[pos+3].getInt());
        parsePos.setIndex(pos+4);
        return true;
    }
    if (pos+2 < dt.length &&
	dt[pos].isUNumber() &&
	dt[pos+1].is('/') &&
	dt[pos+2].isUNumber()) {
	calendar.set(Calendar.DAY_OF_MONTH, dt[pos+2].getInt());
	calendar.set(Calendar.MONTH, dt[pos].getInt()-1);
        parsePos.setIndex(pos+3);
        return true;
    }
    if (pos+2 < dt.length &&
	dt[pos].isUNumber() &&
	dt[pos+1].is(ClockToken.MONTH) &&
	dt[pos+2].isUNumber()) {
	calendar.set(Calendar.DAY_OF_MONTH, dt[pos].getInt());
	calendar.set(Calendar.MONTH, dt[pos+1].getInt());
	calendar.set(Calendar.YEAR, dt[pos+2].getInt());
        parsePos.setIndex(pos+3);
        return true;
    }
    if (pos+1 < dt.length &&
	dt[pos].is(ClockToken.MONTH) &&
	dt[pos+1].isUNumber()) {
	calendar.set(Calendar.DAY_OF_MONTH, dt[pos+1].getInt());
	calendar.set(Calendar.MONTH, dt[pos].getInt());
        parsePos.setIndex(pos+2);
        return true;
    }
    if (pos+1 < dt.length &&
	dt[pos].isUNumber() &&
	dt[pos+1].is(ClockToken.MONTH)) {
	calendar.set(Calendar.DAY_OF_MONTH, dt[pos].getInt());
	calendar.set(Calendar.MONTH, dt[pos+1].getInt());
        parsePos.setIndex(pos+2);
        return true;
    }
    if (pos < dt.length &&
	dt[pos].is(ClockToken.EPOCH)) {
	calendar.set(Calendar.DAY_OF_MONTH, 1);
	calendar.set(Calendar.MONTH, 0);
	calendar.set(Calendar.YEAR, EPOCH_YEAR);
        parsePos.setIndex(pos+1);
        return true;
    }
    return false;
}

/**
 *-----------------------------------------------------------------------------
 *
 * ParseNumber --
 *
 *      Parse a number and sets the Calendar.
 *	If argument mayBeYear is true, this number is conidered as year,
 *	otherwise it is date and time in the form HHMM.
 *
 * Results:
 *      True, if a number was read (parsePos was incremented and calendar
 *	was set according to the read day); false otherwise.
 *
 * Side effects:
 *      None.
 *
 *-----------------------------------------------------------------------------
 */

private boolean
ParseNumber (
    ClockToken[] dt,		// Input as scanned array of tokens
    ParsePosition parsePos,	// Current position in input
    Calendar calendar,		// calendar object to set
    boolean mayBeYear		// number is considered to be year?
)
{
    int pos = parsePos.getIndex();

    if (pos < dt.length &&
	dt[pos].isUNumber()) {
        parsePos.setIndex(pos+1);
        if (mayBeYear) {
	    calendar.set(Calendar.YEAR, dt[pos].getInt());
	} else {
	  calendar.set(Calendar.HOUR_OF_DAY, dt[pos].getInt()/100);
	  calendar.set(Calendar.MINUTE, dt[pos].getInt()%100);
	  calendar.set(Calendar.SECOND, 0);
	}
        return true;
    }
    return false;
}

/**
 *-----------------------------------------------------------------------------
 *
 * ParseRel --
 *
 *      Parse a relative time specification and sets the time difference.
 *	A relative time specification is valid, if it confirms to the
 *	following yacc rule:
 *	rel	: relunit tAGO
 *		| relunit
 *		;
 *
 * Results:
 *      True, if a relative time specification was read (parsePos was
 *	incremented and the time difference was set according to the read
 *	relative time specification); false otherwise.
 *
 * Side effects:
 *      None.
 *
 *-----------------------------------------------------------------------------
 */

private boolean
ParseRel (
    ClockToken[] dt,		// Input as scanned array of tokens
    ParsePosition parsePos,	// Current position in input
    ClockRelTimespan diff	// time difference to evaluate
)
{
    if (ParseRelUnit(dt, parsePos, diff)) {
        int pos = parsePos.getIndex();
        if (pos < dt.length &&
	    dt[pos].is(ClockToken.AGO)) {
	    diff.negate();
	    parsePos.setIndex(pos+1);
	}
        return true;
    }
    return false;
}

/**
 *-----------------------------------------------------------------------------
 *
 * ParseRelUnit --
 *
 *      Parse a relative time unit and sets the time difference.
 *	A relative time unit is valid, if it confirms to the
 *	following yacc rule:
 *	relunit : tUNUMBER tMINUTE_UNIT
 *		| tSNUMBER tMINUTE_UNIT
 *		| tMINUTE_UNIT
 *		| tSNUMBER tSEC_UNIT
 *		| tUNUMBER tSEC_UNIT
 *		| tSEC_UNIT
 *		| tSNUMBER tMONTH_UNIT
 *		| tUNUMBER tMONTH_UNIT
 *		| tMONTH_UNIT
 *		;
 *
 * Results:
 *      True, if a relative time unit was read (parsePos was incremented and
 *	the time difference was set according to the read relative time unit);
 *	false otherwise.
 *
 * Side effects:
 *      None.
 *
 *-----------------------------------------------------------------------------
 */

private boolean
ParseRelUnit (
    ClockToken[] dt,		// Input as scanned array of tokens
    ParsePosition parsePos,	// Current position in input
    ClockRelTimespan diff	// time difference to evaluate
)
{
    int pos = parsePos.getIndex();

    if (pos+1 < dt.length &&
	(dt[pos].isUNumber() || dt[pos].isSNumber()) &&
	dt[pos+1].is(ClockToken.MINUTE_UNIT)) {
        diff.addSeconds(dt[pos].getInt()*dt[pos+1].getInt()*60);
        parsePos.setIndex(pos+2);
        return true;
    } else if (pos+1 < dt.length &&
	(dt[pos].isUNumber() || dt[pos].isSNumber()) &&
	dt[pos+1].is(ClockToken.SEC_UNIT)) {
        diff.addSeconds(dt[pos].getInt());
        parsePos.setIndex(pos+2);
        return true;
    } else if (pos+1 < dt.length &&
	(dt[pos].isUNumber() || dt[pos].isSNumber()) &&
	dt[pos+1].is(ClockToken.MONTH_UNIT)) {
        diff.addMonths(dt[pos].getInt()*dt[pos+1].getInt());
        parsePos.setIndex(pos+2);
        return true;
    } else if (pos < dt.length &&
	dt[pos].is(ClockToken.MINUTE_UNIT)) {
        diff.addSeconds(dt[pos].getInt()*60);
        parsePos.setIndex(pos+1);
        return true;
    } else if (pos < dt.length &&
	dt[pos].is(ClockToken.SEC_UNIT)) {
        diff.addSeconds(1);
        parsePos.setIndex(pos+1);
        return true;
    } else if (pos < dt.length &&
	dt[pos].is(ClockToken.MONTH_UNIT)) {
        diff.addMonths(dt[pos].getInt());
        parsePos.setIndex(pos+1);
        return true;
    }
    return false;
}

/**
 *-----------------------------------------------------------------------------
 *
 * ParseMeridianAndSetHour --
 *
 *      Parse a meridian and sets the hour field of the calendar.
 *	A meridian is valid, if it confirms to the following yacc rule:
 *	o_merid : // NULL
 *		| tMERIDIAN
 *		;
 *
 * Results:
 *      None; parsePos was incremented and the claendar was set according
 *	to the read meridian.
 *
 * Side effects:
 *      None.
 *
 *-----------------------------------------------------------------------------
 */

private void
ParseMeridianAndSetHour(
    ClockToken[] dt,		// Input as scanned array of tokens
    ParsePosition parsePos,	// Current position in input
    Calendar calendar,		// calendar object to set
    int hour			// hour value (1-12 or 0-23) to set.
)
{
    int pos = parsePos.getIndex();
    int hourField;

    if (pos < dt.length &&
	dt[pos].is(ClockToken.MERIDIAN)) {
        calendar.set(Calendar.AM_PM, dt[pos].getInt());
        parsePos.setIndex(pos+1);
	hourField = Calendar.HOUR;
    } else {
	hourField = Calendar.HOUR_OF_DAY;
    }

    if (hourField == Calendar.HOUR && hour == 12) {
        hour = 0;
    }
    calendar.set(hourField, hour);
}

/**
 *-----------------------------------------------------------------------------
 *
 * GetTokens --
 *
 *      Lexical analysis of the input string.
 *
 * Results:
 *      An array of ClockToken, representing the input string.
 *
 * Side effects:
 *      None.
 *
 *-----------------------------------------------------------------------------
 */

private ClockToken[]
GetTokens (
    String in,		// String to parse
    boolean debug	// Send the generated token list to stderr?
)
{
    ParsePosition parsePos = new ParsePosition(0);
    ClockToken dt;
    Vector tokenVector = new Vector(in.length());

    while ((dt = GetNextToken(in, parsePos)) != null) {
        tokenVector.addElement(dt);
    }

    ClockToken[] tokenArray = new ClockToken[tokenVector.size()];
    tokenVector.copyInto(tokenArray);

    if (debug) {
        for (int ix = 0; ix < tokenArray.length; ix++) {
	    if (ix != 0) {
	        System.err.print(",");
	    }
	    System.err.print(tokenArray[ix].toString());
	}
	System.err.println("");
    }

    return tokenArray;
}

/**
 *-----------------------------------------------------------------------------
 *
 * GetNextToken --
 *
 *      Lexical analysis of the next token of input string.
 *
 * Results:
 *      A ClockToken representing the next token of the input string,
 *	(parsePos was incremented accordingly), if one was found.
 *	null otherwise (e.g. at end of input).
 *
 * Side effects:
 *      None.
 *
 *-----------------------------------------------------------------------------
 */

private ClockToken
GetNextToken (
    String in,			// String to parse
    ParsePosition parsePos	// Current position in input
)
{
    int pos = parsePos.getIndex();
    int sign;

    while (true) {
        while (pos < in.length() && Character.isSpaceChar(in.charAt(pos))) {
	    pos++;
	}
	if (pos >= in.length()) {
	    break;
	}

	char c = in.charAt(pos);
	if (Character.isDigit(c) || c == '-' || c == '+') {
	    if (c == '-' || c == '+') {
	        sign = c == '-' ? -1 : 1;
		if (!Character.isDigit(in.charAt(++pos))) {
		    // skip the '-' sign
		  continue;
		}
	    } else {
	        sign = 0;
	    }
	    int number = 0;
	    while (pos < in.length() 
		   && Character.isDigit(c = in.charAt(pos))) {
	        number = 10 * number + c - '0';
		pos++;
	    }
	    if (sign < 0) {
	        number = -number;
	    }
	    parsePos.setIndex(pos);
	    return new ClockToken(number, sign != 0);
	}
	if (Character.isLetter(c)) {
	    int beginPos = pos;
	    while (++pos < in.length()) {
	        c = in.charAt(pos);
		if (!Character.isLetter(c) && c != '.') {
		    break;
		}
	    }
	    parsePos.setIndex(pos);
	    return LookupWord(in.substring(beginPos, pos));
	}
	parsePos.setIndex(pos+1);
	return new ClockToken(in.charAt(pos));
    }
    parsePos.setIndex(pos+1);
    return null;
}

/**
 *-----------------------------------------------------------------------------
 *
 * LookupWord --
 *
 *      Construct a ClockToken for the given word.
 *
 * Results:
 *      A ClockToken representing the given word.
 *
 * Side effects:
 *      None.
 *
 *-----------------------------------------------------------------------------
 */

private ClockToken LookupWord(
    String word			// word to lookup
)
{
    int ix;
    String names[];
    String zones[][];

    if (word.equalsIgnoreCase("am") || word.equalsIgnoreCase("a.m.")) {
        return new ClockToken(ClockToken.MERIDIAN, Calendar.AM);
    }
    if (word.equalsIgnoreCase("pm") || word.equalsIgnoreCase("p.m.")) {
        return new ClockToken(ClockToken.MERIDIAN, Calendar.PM);
    }

    // See if we have an abbreviation for a day or month.

    boolean abbrev;
    if (word.length() == 3) {
        abbrev = true;
    } else if (word.length() == 4 && word.charAt(3) == '.') {
        abbrev = true;
        word = word.substring(0, 3);
    } else {
        abbrev = false;
    }

    DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
    if (abbrev) {
        names = symbols.getShortMonths();
    } else {
        names = symbols.getMonths();
    }
    for (ix = 0; ix < names.length; ix++) {
        if (word.equalsIgnoreCase(names[ix])) {
	    return new ClockToken(ClockToken.MONTH, ix);
	}
    }
    if (abbrev) {
        names = symbols.getShortWeekdays();
    } else {
        names = symbols.getWeekdays();
    }
    for (ix = 0; ix < names.length; ix++) {
        if (word.equalsIgnoreCase(names[ix])) {
	    return new ClockToken(ClockToken.DAY, ix);
	}
    }

    // Drop out any periods and try the timezone table.

    StringBuffer withoutDotsBuf = new StringBuffer(word.length());
    for (ix = 0; ix < word.length(); ix++) {
        if (word.charAt(ix) != '.') {
	    withoutDotsBuf.append(word.charAt(ix));
	}
    }

    String withoutDots = new String(withoutDotsBuf);
    zones = symbols.getZoneStrings();

    for (ix = 0; ix < zones.length; ix++) {
        if (withoutDots.equalsIgnoreCase(zones[ix][2]) ||
	    withoutDots.equalsIgnoreCase(zones[ix][4])) {
 	    TimeZone zone = TimeZone.getTimeZone(zones[ix][0]);
	    return new ClockToken(ClockToken.ZONE, zone);
	}
    }
    if (withoutDots.equalsIgnoreCase("dst")) {
	return new ClockToken(ClockToken.DST, null);
    }

    // Strip off any plural and try the units.

    String singular;
    if (word.endsWith("s")) {
        singular = word.substring(0, word.length()-1);
    } else {
        singular = word;
    }
    if (singular.equalsIgnoreCase("year")) {
	return new ClockToken(ClockToken.MONTH_UNIT, 12);
    } else if (singular.equalsIgnoreCase("month")) {
	return new ClockToken(ClockToken.MONTH_UNIT, 1);
    } else if (singular.equalsIgnoreCase("fortnight")) {
	return new ClockToken(ClockToken.MINUTE_UNIT, 14*24*60);
    } else if (singular.equalsIgnoreCase("week")) {
	return new ClockToken(ClockToken.MINUTE_UNIT, 7*24*60);
    } else if (singular.equalsIgnoreCase("day")) {
	return new ClockToken(ClockToken.MINUTE_UNIT, 24*60);
    } else if (singular.equalsIgnoreCase("hour")) {
	return new ClockToken(ClockToken.MINUTE_UNIT, 60);
    } else if (singular.equalsIgnoreCase("minute")) {
	return new ClockToken(ClockToken.MINUTE_UNIT, 1);
    } else if (singular.equalsIgnoreCase("min")) {
	return new ClockToken(ClockToken.MINUTE_UNIT, 1);
    } else if (singular.equalsIgnoreCase("second")) {
	return new ClockToken(ClockToken.SEC_UNIT, 1);
    } else if (singular.equalsIgnoreCase("sec")) {
	return new ClockToken(ClockToken.SEC_UNIT, 1);
    }

    if (singular.equalsIgnoreCase("tomorrow")) {
	return new ClockToken(ClockToken.MINUTE_UNIT, 1*24*60);
    } else if (singular.equalsIgnoreCase("yesterday")) {
	return new ClockToken(ClockToken.MINUTE_UNIT, -1*24*60);
    } else if (singular.equalsIgnoreCase("today")) {
	return new ClockToken(ClockToken.MINUTE_UNIT, 0);
    } else if (singular.equalsIgnoreCase("now")) {
	return new ClockToken(ClockToken.MINUTE_UNIT, 0);
    } else if (singular.equalsIgnoreCase("last")) {
	return new ClockToken(-1, false);
    } else if (singular.equalsIgnoreCase("this")) {
	return new ClockToken(ClockToken.MINUTE_UNIT, 0);
    } else if (singular.equalsIgnoreCase("next")) {
	return new ClockToken(2, false);
    } else if (singular.equalsIgnoreCase("ago")) {
	return new ClockToken(ClockToken.AGO, 1);
    } else if (singular.equalsIgnoreCase("epoch")) {
	return new ClockToken(ClockToken.EPOCH, 0);
    }

    // Ignore military timezones.

    return new ClockToken(word);
}

} // end ClockCmd

/**
 *-----------------------------------------------------------------------------
 *
 * CLASS ClockToken --
 *
 *      An object of this class represents a lexical unit of the human
 *	readable date string. It can be one of the following variants:
 *
 *	- signed number,
 *	  = occurence can be asked by isSNumber(),
 *	  = value can be retrieved by means of getInt();
 *	- unsigned number,
 *	  = occurence can be asked by isUNumber(),
 *	  = value can be retrieved by means of getInt();
 *	- a single character (delimiters like ':' or '/'),
 *	  = occurence can be asked by is(), e.g. is('/');
 *	- a word (like "January" or "DST")
 *	  = occurence can be asked by is(), e.g. is(ClockToken.AGO);
 *	  = value can be retrieved by means of getInt() or getZone().
 *
 *-----------------------------------------------------------------------------
 */

class ClockToken {
    final static int SNUMBER     = 1;
    final static int UNUMBER     = 2;
    final static int WORD        = 3;
    final static int CHAR        = 4;
    final static int MONTH       = 5;
    final static int DAY         = 6;
    final static int MONTH_UNIT  = 7;
    final static int MINUTE_UNIT = 8;
    final static int SEC_UNIT    = 9;
    final static int AGO         = 10;
    final static int EPOCH       = 11;
    final static int ZONE        = 12;
    final static int DAYZONE     = 13;
    final static int DST         = 14;
    final static int MERIDIAN    = 15;

    ClockToken(int number, boolean signed) {
        this.kind = signed ? SNUMBER : UNUMBER;
	this.number = number;
    }
    ClockToken(int kind, int number) {
        this.kind = kind;
	this.number = number;
    }
    ClockToken(int kind, TimeZone zone) {
        this.kind = kind;
	this.zone = zone;
    }
    ClockToken(String word) {
        this.kind = WORD;
	this.word = word;
    }
    ClockToken(char c) {
        this.kind = CHAR;
	this.c = c;
    }

    public boolean isSNumber() {
        return kind == SNUMBER;
    }
    public boolean isUNumber() {
        return kind == UNUMBER;
    }
    public boolean is(char c) {
        return this.kind == CHAR && this.c == c;
    }
    public boolean is(int kind) {
        return this.kind == kind;
    }    

    int getInt() {
        return number;
    }
    TimeZone getZone() {
        return zone;
    }

    public String toString() {
        if (isSNumber()) {
	    return "S"+Integer.toString(getInt());
	} else if (isUNumber()) {
	    return "U"+Integer.toString(getInt());
	} else if (kind == WORD) {
	    return word;
	} else if (kind == CHAR) {
	    return new Character(c).toString();
	} else if (kind == ZONE || kind == DAYZONE) {
	    return zone.getID();
	} else {
	    return "("+kind+","+getInt()+")";
	}
    }

    private int kind;
    private int number;
    private String word;
    private char c;
    private TimeZone zone;
} // end ClockToken

/**
 *-----------------------------------------------------------------------------
 *
 * CLASS ClockRelTimespan --
 *
 *      An object of this class can be used to track the time difference during
 *	the analysis of a relative time specification.
 *
 *	It has two read only properties 'seconds' and 'months', which are set
 *	to 0 during initialization and which can be modified by means of the
 *	addSeconds(), addMonths() and negate() methods.
 *
 *-----------------------------------------------------------------------------
 */

class ClockRelTimespan {
    ClockRelTimespan() {
        seconds = 0;
	months = 0;
    }
    void addSeconds(int s) {
        seconds += s;
    }
    void addMonths(int m) {
        months += m;
    }
    void negate() {
        seconds = -seconds;
        months = -months;
    }
    int getSeconds() {
        return seconds;
    }
    int getMonths() {
        return months;
    }
    private int seconds;
    private int months;
}
