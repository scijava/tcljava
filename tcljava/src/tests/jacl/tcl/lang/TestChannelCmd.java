/*
 * TestChannelCmd.java --
 *
 * Copyright (c) 2001 Bruce A. Johnson
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TestChannelCmd.java,v 1.3 2005/01/13 06:21:14 mdejong Exp $
 *
 */

package tcl.lang;

import java.util.Hashtable;
import java.util.Enumeration;
import java.io.IOException;

/**
 * This class implements the built-in "testchannel" command in Tcl.
 */

class TestChannelCmd implements Command
{
    // FIXME: The C implementation in Tcl 8.4 seems to support
    // a number of additional options

    static final private String validCmds[] = {
        "info",
        "inputbuffered",
        "name",
        "open",
        "outputbuffered",
        "queuedcr",
        "refcount",
        "type"
    };

    static final int OPT_INFO = 0;
    static final int OPT_INPUTBUFFERED = 1;
    static final int OPT_NAME = 2;
    static final int OPT_OPEN = 3;
    static final int OPT_OUTPUTBUFFERED = 4;
    static final int OPT_QUEUEDCR = 5;
    static final int OPT_REFCOUNT = 6;
    static final int OPT_TYPE = 7;


    /**
     * This procedure is invoked to process the "testchannel" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     */

    public void cmdProc(Interp interp, TclObject argv[])
        throws TclException
    {
        Channel chan;

        if (argv.length == 2) {
            if (!argv[1].toString().equals("open")) {
                throw new TclNumArgsException(interp, 1, argv,
                    "only option open can have two args");
            }
        } else if (argv.length != 3) {
            throw new TclNumArgsException(interp, 1, argv,
                "command channelId");
        }

        if (argv.length == 2) {
            // return list of all name/value pairs for this channelId
            Hashtable chanTable = TclIO.getInterpChanTable(interp);
            Enumeration e = chanTable.elements();

            TclObject list = TclList.newInstance();
            while (e.hasMoreElements()) {
                chan = (Channel) e.nextElement();
                TclList.append(interp, list,
                    TclString.newInstance(chan.getChanName()));
            }
            interp.setResult(list);
            return;
        }
        chan = TclIO.getChannel(interp, argv[2].toString());
        if (chan == null) {
            throw new TclException(interp, "can not find channel named \"" +
                argv[2].toString() + "\"");
        }

        int index = TclIndex.get(interp, argv[1], validCmds, "option", 0);
        switch (index) {
        case OPT_INFO:
            {
                TclObject list = TclList.newInstance();
                TclList.append(interp, list, argv[2]);     // 0
                TclList.append(interp, list,
                    TclString.newInstance(chan.getChanType()));     // 1

                if (chan.isReadOnly() || chan.isReadWrite()) {
                    TclList.append(interp, list,
                        TclString.newInstance("read"));     // 2
                } else {
                    TclList.append(interp, list,
                        TclString.newInstance(""));
                }

                if (chan.isWriteOnly() || chan.isReadWrite()) {
                    TclList.append(interp, list,
                        TclString.newInstance("write"));     // 3
                } else {
                    TclList.append(interp, list,
                        TclString.newInstance(""));
                }

                if (chan.getBlocking()) {
                    TclList.append(interp, list,
                        TclString.newInstance("blocking"));     // 4
		} else {
                    TclList.append(interp, list,
                        TclString.newInstance("nonblocking"));
                }

                if (chan.getBuffering() == TclIO.BUFF_FULL) {
                    TclList.append(interp, list,
                        TclString.newInstance("full"));        // 5
                } else if (chan.getBuffering() == TclIO.BUFF_LINE) {
                    TclList.append(interp, list,
                        TclString.newInstance("line"));
                } else {
                    TclList.append(interp, list,
                        TclString.newInstance("none"));
                }

                if (chan.isBgFlushScheduled()) {
                    TclList.append(interp, list,
                        TclString.newInstance("async_flush")); // 6
                } else {
                    TclList.append(interp, list,
                        TclString.newInstance(""));
                }

                if (chan.eof()) {
                    TclList.append(interp, list,
                        TclString.newInstance("eof"));     // 7
                } else {
                    TclList.append(interp, list,
                        TclString.newInstance(""));
                }

                if (chan.isBlocked(interp)) {
                    TclList.append(interp, list,
                        TclString.newInstance("blocked")); // 8
                } else {
                    TclList.append(interp, list,
                        TclString.newInstance("unblocked"));
                }

                int translation = chan.getInputTranslation();

                if (translation == TclIO.TRANS_AUTO) {
                    TclList.append(interp, list,
                        TclString.newInstance("auto"));     // 9

                    TclList.append(interp, list,
                        TclString.newInstance(
                            chan.inputSawCR() ?
                                "queued_cr" : ""));     // 10
                } else if (translation == TclIO.TRANS_LF) {
                    TclList.append(interp, list,
                        TclString.newInstance("lf"));
                    TclList.append(interp, list,
                        TclString.newInstance(""));
                } else if (translation == TclIO.TRANS_CR) {
                    TclList.append(interp, list,
                        TclString.newInstance("cr"));
                    TclList.append(interp, list,
                        TclString.newInstance(""));
                } else if (translation == TclIO.TRANS_CRLF) {
                    TclList.append(interp, list,
                        TclString.newInstance("crlf"));
                    TclList.append(interp, list,
                        TclString.newInstance(""));
                }

                translation = chan.getOutputTranslation();

                if (translation == TclIO.TRANS_AUTO) {
                    TclList.append(interp, list,
                        TclString.newInstance("auto"));     // 11
                } else if (translation == TclIO.TRANS_LF) {
                    TclList.append(interp, list,
                        TclString.newInstance("lf"));
                } else if (translation == TclIO.TRANS_CR) {
                    TclList.append(interp, list,
                        TclString.newInstance("cr"));
                } else if (translation == TclIO.TRANS_CRLF) {
                    TclList.append(interp, list,
                        TclString.newInstance("crlf"));
		}

                TclList.append(interp, list,
                    TclInteger.newInstance(
                        chan.getNumBufferedInputBytes()));     // 12

                TclList.append(interp, list,
                    TclInteger.newInstance(
                        chan.getNumBufferedOutputBytes()));     // 13

                try {
                    TclList.append(interp, list,
                        TclInteger.newInstance((int)chan.tell())); // 14
                } catch (IOException e) {
                    throw new TclException(interp, e.toString());
                }	

                TclList.append(interp, list,
                    TclInteger.newInstance(chan.getRefCount())); // 15

		interp.setResult(list);
		break;
            }
        case OPT_INPUTBUFFERED:
            {
                interp.setResult(chan.getNumBufferedInputBytes());
                break;
            }
        case OPT_NAME:
            {
                interp.setResult(chan.getChanName());
                break;
            }
        case OPT_OUTPUTBUFFERED:
            {
                interp.setResult(chan.getNumBufferedOutputBytes());
                break;
            }
        case OPT_QUEUEDCR:
            {
                interp.setResult(chan.inputSawCR());
                break;
            }
        case OPT_REFCOUNT:
            {
                interp.setResult(chan.getRefCount());
                break;
            }
        case OPT_TYPE:
            {
                interp.setResult(chan.getChanType());
                break;
            }
        default:
            {
                throw new TclRuntimeError(
                    "TestChannel.cmdProc() error: " +
                    "incorrect index returned from TclIndex.get()");
            }
        }
    }
}
