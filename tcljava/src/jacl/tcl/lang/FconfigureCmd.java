/*
 * FconfigureCmd.java --
 *
 * Copyright (c) 2001 Bruce A. Johnson
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: FconfigureCmd.java,v 1.5 2001/11/27 18:05:25 mdejong Exp $
 *
 */

package tcl.lang;
import java.util.*;

/**
 * This class implements the built-in "fconfigure" command in Tcl.
 */

class FconfigureCmd implements Command {

    static final private String validCmds[] = { 
        "-blocking",
        "-buffering",
        "-buffersize",
        "-encoding",
        "-eofchar",
        "-translation",
    };

    static final int OPT_BLOCKING	= 0;
    static final int OPT_BUFFERING	= 1;
    static final int OPT_BUFFERSIZE	= 2;
    static final int OPT_ENCODING	= 3;
    static final int OPT_EOFCHAR	= 4;
    static final int OPT_TRANSLATION	= 5;


    /**
     * This procedure is invoked to process the "fconfigure" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     */

    public void cmdProc(Interp interp, TclObject argv[])
            throws TclException {

        Channel chan;        // The channel being operated on this method

        if ((argv.length < 2) || (((argv.length % 2) == 1) && 
                (argv.length != 3))) {
            throw new TclNumArgsException(interp, 1, argv, 
                "channelId ?optionName? ?value? ?optionName value?...");
        }

        chan = TclIO.getChannel(interp, argv[1].toString());
        if (chan == null) {
            throw new TclException(interp, "can not find channel named \""
                + argv[1].toString() + "\"");
        }

        if (argv.length == 2) {
            // return list of all name/value pairs for this channelId
            TclObject list = TclList.newInstance();

            TclList.append(interp, list, TclString.newInstance("-blocking"));
            TclList.append(interp, list,
                TclBoolean.newInstance(chan.getBlocking()));

            TclList.append(interp, list, TclString.newInstance("-buffering"));
            switch (chan.getBuffering()) {
                case TclIO.BUFF_FULL:
                    TclList.append(interp, list,
                        TclString.newInstance("full"));
                    break;
                case TclIO.BUFF_LINE:
                    TclList.append(interp, list,
                        TclString.newInstance("line"));
                    break;
                case TclIO.BUFF_NONE:
                    TclList.append(interp, list,
                        TclString.newInstance("none"));
                    break;
            }

            TclList.append(interp, list, TclString.newInstance("-buffersize"));
            TclList.append(interp, list,
                TclInteger.newInstance(chan.getBufferSize()));

            TclList.append(interp, list, TclString.newInstance("-encoding"));
            String encoding = chan.getEncoding();
            if (encoding == null)
                encoding = "binary";
            TclList.append(interp, list, TclString.newInstance(encoding));

            TclList.append(interp, list, TclString.newInstance("-eofchar"));
            TclList.append(interp, list, TclString.newInstance(""));

            TclList.append(interp, list, TclString.newInstance("-translation"));
            TclList.append(interp, list, TclString.newInstance("auto"));

            interp.setResult(list);
        }
        if (argv.length == 3) {
            // return value for supplied name

            int index = TclIndex.get(interp, argv[2], validCmds, 
                "option", 0);

            switch (index) {
                case OPT_BLOCKING: {    // -blocking
                    interp.setResult(chan.getBlocking());
                    break;
                }
                case OPT_BUFFERING: {    // -buffering
                    switch (chan.getBuffering()) {
                        case TclIO.BUFF_FULL:
                            interp.setResult("full");
                            break;
                        case TclIO.BUFF_LINE:
                            interp.setResult("line");
                            break;
                        case TclIO.BUFF_NONE:
                            interp.setResult("none");
                            break;
                    }
                    break;
                }
                case OPT_BUFFERSIZE: {    // -buffersize
                    interp.setResult(chan.getBufferSize());
                    break;
                }
                case OPT_ENCODING: {    // -encoding
                    String encoding = chan.getEncoding();
                    interp.setResult((encoding == null)
                        ? "binary" : encoding);
                    break;
                }
                case OPT_EOFCHAR: {    // -eofchar
                    break;
                }
                case OPT_TRANSLATION: {    // -translation
                    break;
                }
                default: {
                    throw new TclRuntimeError("Fconfigure.cmdProc() error: " +
                        "incorrect index returned from TclIndex.get()");
                }
            }
        }
        for (int i = 3; i < argv.length; i += 2) {
            // Iterate through the list setting the name with the 
            // corresponding value.

            int index = TclIndex.get(interp, argv[i-1], validCmds, 
                "option", 0);

            switch (index) {
                case OPT_BLOCKING: {    // -blocking
                    chan.setBlocking(TclBoolean.get(interp, argv[i]));
                    break;
                }
                case OPT_BUFFERING: {    // -buffering
                    String arg = argv[i].toString();
                    if (arg.equals("full")) {
                        chan.setBuffering(TclIO.BUFF_FULL);
                    } else if (arg.equals("line")) {
                        chan.setBuffering(TclIO.BUFF_LINE);
                    } else if (arg.equals("none")) {
                        chan.setBuffering(TclIO.BUFF_NONE);
                    } else {
                        throw new TclException(interp, 
                            "bad value for -buffering: must be " +
                            "one of full, line, or none");
                    }
                    break;
                }
                case OPT_BUFFERSIZE: {    // -buffersize
                    chan.setBufferSize(TclInteger.get(interp,argv[i]));
                    break;
                }
                case OPT_ENCODING: {    // -encoding
                    chan.setEncoding(argv[i].toString());
                    break;
                }
                case OPT_EOFCHAR: {    // -eofchar
                    break;
                }
                case OPT_TRANSLATION: {    // -translation
                    String arg = argv[i].toString();
                    if (arg.equals("auto")) {

                    } else if (arg.equals("binary")) {

                    } else if (arg.equals("cr")) {

                    } else if (arg.equals("crlf")) {

                    } else if (arg.equals("lf")) {

                    } else {
                        throw new TclException(interp,
                            "bad value for -translation: must be one " +
                            "of auto, binary, cr, lf, crlf, or platform");
                    }
                    break;
                }
                default: {
                    throw new TclRuntimeError("Fconfigure.cmdProc() error: " +
                        "incorrect index returned from TclIndex.get()");
                }
            }
        }
    }
}
