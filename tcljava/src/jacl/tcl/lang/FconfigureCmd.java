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
 * RCS: @(#) $Id: FconfigureCmd.java,v 1.8 2001/12/24 21:08:06 mdejong Exp $
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
            TclList.append(interp, list, TclString.newInstance(
                TclIO.getBufferingString(chan.getBuffering())));

            TclList.append(interp, list, TclString.newInstance("-buffersize"));
            TclList.append(interp, list,
                TclInteger.newInstance(chan.getBufferSize()));

	    // -encoding

            TclList.append(interp, list, TclString.newInstance("-encoding"));
            String encoding = chan.getEncoding();
            if (encoding == null)
                encoding = "binary";
            TclList.append(interp, list, TclString.newInstance(encoding));

            // -eofchar

            TclList.append(interp, list, TclString.newInstance("-eofchar"));
            if (chan.isReadOnly()) {
                char eofChar = chan.getInputEofChar();
                TclList.append(interp, list,
                    (eofChar == 0) ?
                    TclString.newInstance("{}") :
                    TclString.newInstance(eofChar));
            } else if (chan.isWriteOnly()) {
                char eofChar = chan.getOutputEofChar();
                TclList.append(interp, list,
                    (eofChar == 0) ?
                    TclString.newInstance("{}") :
                    TclString.newInstance(eofChar));
            } else if (chan.isReadWrite()) {
                char inEofChar = chan.getInputEofChar();
                char outEofChar = chan.getOutputEofChar();

                TclObject eofchar_pair = TclList.newInstance();

                TclList.append(interp, eofchar_pair,
                    (inEofChar == 0) ?
                    TclString.newInstance("") :
                    TclString.newInstance(inEofChar));

                TclList.append(interp, eofchar_pair,
                    (outEofChar == 0) ?
                    TclString.newInstance("") :
                    TclString.newInstance(outEofChar));

                TclList.append(interp, list, eofchar_pair);
            } else {
                throw new TclRuntimeError("Invalid channel mode");
            }

            // -translation

            TclList.append(interp, list, TclString.newInstance("-translation"));

            if (chan.isReadOnly()) {
                TclList.append(interp, list,
                    TclString.newInstance(
                        TclIO.getTranslationString(
                            chan.getInputTranslation())));
            } else if (chan.isWriteOnly()) {
                TclList.append(interp, list,
                    TclString.newInstance(
                        TclIO.getTranslationString(
                            chan.getOutputTranslation())));
            } else if (chan.isReadWrite()) {
                TclObject translation_pair = TclList.newInstance();

                TclList.append(interp, translation_pair,
                    TclString.newInstance(
                        TclIO.getTranslationString(
                            chan.getInputTranslation())));
                TclList.append(interp, translation_pair,
                    TclString.newInstance(
                        TclIO.getTranslationString(
                            chan.getOutputTranslation())));

                TclList.append(interp, list, translation_pair);
            } else {
                throw new TclRuntimeError("Invalid channel mode");
            }

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
                    interp.setResult(
                        TclIO.getBufferingString(
                            chan.getBuffering()));
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
                    if (chan.isReadOnly()) {
                        char eofChar = chan.getInputEofChar();
                        interp.setResult((eofChar == 0) ?
                            TclString.newInstance("{}") :
                            TclString.newInstance(eofChar));
                    } else if (chan.isWriteOnly()) {
                        char eofChar = chan.getOutputEofChar();
                        interp.setResult((eofChar == 0) ?
                            TclString.newInstance("{}") :
                            TclString.newInstance(eofChar));
                    } else if (chan.isReadWrite()) {
                        char inEofChar = chan.getInputEofChar();
                        char outEofChar = chan.getOutputEofChar();

                        TclObject eofchar_pair = TclList.newInstance();

                        TclList.append(interp, eofchar_pair,
                            (inEofChar == 0) ?
                            TclString.newInstance("") :
                            TclString.newInstance(inEofChar));

                        TclList.append(interp, eofchar_pair,
                            (outEofChar == 0) ?
                            TclString.newInstance("") :
                            TclString.newInstance(outEofChar));

                        interp.setResult(eofchar_pair);
                    } else {
                        throw new TclRuntimeError("Invalid channel mode");
                    }

                    break;
                }
                case OPT_TRANSLATION: {    // -translation
                    if (chan.isReadOnly()) {
                        interp.setResult(
                            TclIO.getTranslationString(
                                chan.getInputTranslation()));
                    } else if (chan.isWriteOnly()) {
                        interp.setResult(
                            TclIO.getTranslationString(
                                chan.getOutputTranslation()));
                    } else if (chan.isReadWrite()) {
                        TclObject translation_pair = TclList.newInstance();

                        TclList.append(interp, translation_pair,
                            TclString.newInstance(
                                TclIO.getTranslationString(
                                    chan.getInputTranslation())));
                        TclList.append(interp, translation_pair,
                            TclString.newInstance(
                                TclIO.getTranslationString(
                                    chan.getOutputTranslation())));

                        interp.setResult(translation_pair);
                    } else {
                        throw new TclRuntimeError("Invalid channel mode");
                    }

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
                    int id = TclIO.getBufferingID(argv[i].toString());

                    if (id == -1) {
                        throw new TclException(interp,
                            "bad value for -buffering: must be " +
                            "one of full, line, or none");
                    }

                    chan.setBuffering(id);
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
                    TclList.setListFromAny(interp, argv[i]);
                    int length = TclList.getLength(interp, argv[i]);

                    if (length > 2) {
                        throw new TclException(interp,
                            "bad value for -eofchar: " +
                            "should be a list of zero, one, or two elements");
                    }

                    char inputEofChar, outputEofChar;
                    String s;

                    if (length == 0) {
                        inputEofChar = outputEofChar = 0;
                    } else if (length == 1) {
                        s = TclList.index(interp,argv[i],0).toString();
                        inputEofChar = outputEofChar = s.charAt(0);
                    } else {
                        s = TclList.index(interp,argv[i],0).toString();
                        inputEofChar = s.charAt(0);

                        s = TclList.index(interp,argv[i],1).toString();
                        outputEofChar = s.charAt(0);
                    }

                    chan.setInputEofChar(inputEofChar);
                    chan.setOutputEofChar(outputEofChar);

                    break;
                }
                case OPT_TRANSLATION: {    // -translation
                    TclList.setListFromAny(interp, argv[i]);
                    int length = TclList.getLength(interp, argv[i]);

                    if (length < 1 || length > 2) {
                        throw new TclException(interp,
                            "bad value for -translation: " +
                            "must be a one or two element list");
                    }

                    int inputTranslation, outputTranslation;

                    if (length == 2) {
                        inputTranslation = TclIO.getTranslationID(
                            TclList.index(interp, argv[i], 0).toString());

                        outputTranslation = TclIO.getTranslationID(
                            TclList.index(interp, argv[i], 1).toString());
                    } else {
                        outputTranslation = inputTranslation = 
                            TclIO.getTranslationID(argv[i].toString());
                    }

                    if ((inputTranslation == -1) ||
                            (outputTranslation == -1)) {
                        throw new TclException(interp,
                            "bad value for -translation: " +
                            "must be one of auto, binary, cr, lf, " +
                            "crlf, or platform");
                    }

                    if (outputTranslation == TclIO.TRANS_AUTO)
                        outputTranslation = TclIO.TRANS_PLATFORM;

                    if (chan.isReadOnly()) {
                        chan.setInputTranslation(inputTranslation);
                    } else if (chan.isWriteOnly()) {
                        chan.setOutputTranslation(outputTranslation);
                    } else if (chan.isReadWrite()) {
                        chan.setInputTranslation(inputTranslation);
                        chan.setOutputTranslation(outputTranslation);
                    } else {
                        throw new TclRuntimeError("Invalid channel mode");
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
