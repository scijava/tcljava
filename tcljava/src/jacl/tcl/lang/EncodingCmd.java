/*
 * EncodingCmd.java --
 *
 * Copyright (c) 2001 Bruce A. Johnson
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: EncodingCmd.java,v 1.1 2002/01/02 18:40:53 mdejong Exp $
 *
 */

package tcl.lang;

import java.util.Hashtable;
import java.io.UnsupportedEncodingException;

/**
 * This class implements the built-in "encoding" command in Tcl.
 */

class EncodingCmd implements Command
{
    // FIXME: Make sure this is a global property and not a per-interp
    // property!
    static String systemTclEncoding = "iso8859-1";
    static String systemJavaEncoding = "ISO8859_1";

    static String tclNames[] = {
        "identity",
        "utf-8",
        "utf-16",
        "unicode",
        "ascii",
        "big5",
        "cp1250",
        "cp1251",
        "cp1252",
        "cp1253",
        "cp1254",
        "cp1255",
        "cp1256",
        "cp1257",
        "cp1258",
        "cp437",
        "cp737",
        "cp775",
        "cp850",
        "cp852",
        "cp855",
        "cp857",
        "cp860",
        "cp861",
        "cp862",
        "cp863",
        "cp864",
        "cp865",
        "cp866",
        "cp869",
        "cp874",
        "cp932",
        "cp936",
        "cp949",
        "cp950",
        "euc-cn",
        "euc-jp",
        "euc-kr",
        "iso2022-jp",
        "iso2022-kr",
        "iso20221",
        "iso8859-1",
        "iso8859-2",
        "iso8859-3",
        "iso8859-4",
        "iso8859-5",
        "iso8859-6",
        "iso8859-7",
        "iso8859-8",
        "iso8859-9",
        "jis0201",
        "jis0208",
        "jis0212",
        "koi8-r",
        "macCentEuro",
        "macCroatian",
        "macCyrillic",
        "macDingbats",
        "macGreek",
        "macIceland",
        "macJapan",
        "macRoman",
        "macRomania",
        "macThai",
        "macTurkish",
        "macUkraine",
        "shiftjis"
    };

    static final String javaNames[] = {
        "identity",
        "UTF8",
        "UTF16",
        "ISO-10646-UCS-2",
        "ASCII",
        "Big5",
        "Cp1250",
        "Cp1251",
        "Cp1252",
        "Cp1253",
        "Cp1254",
        "Cp1255",
        "Cp1256",
        "Cp1257",
        "Cp1258",
        "Cp437",
        "Cp737",
        "Cp775",
        "Cp850",
        "Cp852",
        "Cp855",
        "Cp857",
        "Cp860",
        "Cp861",
        "Cp862",
        "Cp863",
        "Cp864",
        "Cp865",
        "Cp866",
        "Cp869",
        "Cp874",
        "Cp932",
        "Cp936",
        "Cp949",
        "Cp950",
        "EUC_cn",
        "EUC_jp",
        "EUC_kr",
        "ISO2022JP",
        "ISO2022KR",
        "ISO2022",
        "ISO8859_1",
        "ISO8859_2",
        "ISO8859_3",
        "ISO8859_4",
        "ISO8859_5",
        "ISO8859_6",
        "ISO8859_7",
        "ISO8859_8",
        "ISO8859_9",
        "JIS0201",
        "JIS0208",
        "JIS0212",
        "KOI8_r",
        "MacCentEuro",
        "MacCroatian",
        "MacCyrillic",
        "MacDingbats",
        "MacGreek",
        "MacIceland",
        "MacJapan",
        "MacRoman",
        "MacRomania",
        "MacThai",
        "MacTurkish",
        "MacUkraine",
        "SJIS"
    };

    static int bytesPerChar[] = {
        1,
        1,
        2,
        2,
        1,
        0,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        -1,
        -1,
        -1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        2,
        2,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        0,
        1,
        1,
        1,
        1,
        1,
        0
    };

    static Hashtable encodeHash = null;

    static final private String validCmds[] = {
        "convertfrom",
        "convertto",
        "names",
        "system",
    };

    static final int OPT_CONVERTFROM = 0;
    static final int OPT_CONVERTTO = 1;
    static final int OPT_NAMES = 2;
    static final int OPT_SYSTEM = 3;

    /**
     * This procedure is invoked to process the "encoding" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     */

    public void cmdProc (Interp interp, TclObject argv[])
        throws TclException
    {
        if (argv.length < 2) 
        {
          throw new TclNumArgsException (interp, 1, argv,
              "option ?arg ...?");
        }

        int index = TclIndex.get (interp, argv[1], validCmds, "option", 0);

        switch (index)
        {
            case OPT_CONVERTTO:
            case OPT_CONVERTFROM:
	    {
                String tclEncoding, javaEncoding;
                TclObject data;

                if (argv.length == 3) {
                    tclEncoding = systemTclEncoding;
                    data = argv[2];
                } else if (argv.length == 4) {
                    tclEncoding = argv[2].toString();
                    data = argv[3];
                } else {
                    throw new TclNumArgsException(interp, 2, argv,
                        "?encoding? data");
                }

                javaEncoding = getJavaName(tclEncoding);

                if (javaEncoding == null) {
                    throw new TclException(interp,
                        "unknown encoding \"" + tclEncoding + "\"");
                }

                try {
                    if (index == OPT_CONVERTFROM) {
                        // Treat the string as binary data
                        byte[] bytes = TclByteArray.getBytes(interp, data);
                        interp.setResult(new String(bytes, javaEncoding));
                    } else {
                        // Store the result as binary data
                        byte[] bytes = data.toString().getBytes(javaEncoding);
                        interp.setResult(TclByteArray.newInstance(bytes));
                    }

                } catch (UnsupportedEncodingException ex) {
                    throw new TclRuntimeError("Encoding.cmdProc() error: " +
                        "unsupported java encoding \"" + javaEncoding + "\"");
                }

                break;
            }
            case OPT_NAMES:
            {
                if (argv.length > 2) {
                    throw new TclNumArgsException(interp, 2, argv, null);
                }

                TclObject list = TclList.newInstance();
                for (int i=0; i < tclNames.length; i++) {
                    TclList.append(interp, list,
                        TclString.newInstance(tclNames[i]));
                }
                interp.setResult(list);
                break;
            }
            case OPT_SYSTEM:
            {
                if (argv.length > 3)
      		    throw new TclNumArgsException(interp, 2, argv,
                        "?encoding?");

                if (argv.length  == 2) {
                    interp.setResult(systemTclEncoding);
                } else {
                    String tclEncoding = argv[2].toString();
                    String javaEncoding = EncodingCmd.getJavaName(
                        tclEncoding);

                    if (javaEncoding == null) {
                        throw new TclException(interp,
                            "unknown encoding \"" + tclEncoding + "\"");
                    }

                    systemTclEncoding = tclEncoding;
                    systemJavaEncoding = javaEncoding;
                }

                break;
            }
            default:
            {
                throw new TclRuntimeError("Encoding.cmdProc() error: " +
                    "incorrect index returned from TclIndex.get()");
            }
        }
    }

    private static void initEncodeHash() {
        if ((tclNames.length != javaNames.length) ||
                (tclNames.length != bytesPerChar.length)) {
            throw new RuntimeException("encoding array sizes do not match");
        }
        encodeHash = new Hashtable();
        for (int i=0; i < tclNames.length; i++) {
            Integer obj = new Integer(i);
            encodeHash.put(tclNames[i], obj);
            encodeHash.put(javaNames[i], obj);
        }
    }

    static int getBytesPerChar(String name)
    {
        if (encodeHash == null) {
            initEncodeHash();
        }
        Integer indexInt = (Integer) encodeHash.get(name);
        if (indexInt == null) {
	    throw new RuntimeException("Invalid encoding \"" + name + "\"");
        }
        int index = indexInt.intValue();
        return bytesPerChar[index];
    }

    static String getJavaName(String name)
    {
        if (encodeHash == null) {
            initEncodeHash();
        }
        Integer indexInt = (Integer) encodeHash.get(name);
        if (indexInt == null) {
            return null;
        }
        int index = indexInt.intValue();
        return javaNames[index];
    }

    static String getTclName(String name)
    {
        if (encodeHash == null) {
            initEncodeHash();
        }
        Integer indexInt = (Integer) encodeHash.get(name);
        if (indexInt == null) {
            return null;
        }
        int index = indexInt.intValue();
        return tclNames[index];
    }
}
