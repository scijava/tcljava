/*
 * ServerSocketChannel.java
 *
 * Implements a server side socket channel for the Jacl
 * interpreter.
 */
package tcl.lang;
import java.io.*;
import java.net.*;
import java.util.Hashtable;

/**
 * The ServerSocketChannel class implements a channel object for
 * ServerSocket connections, created using the socket command.
 **/

public class ServerSocketChannel extends Channel {

    /**
     * The java ServerSocket object associated with this Channel.
     **/

    private ServerSocket sock;

    /**
     * The interpreter to evaluate the callback in, when a connection
     * is made.
     **/

    private Interp cbInterp;

    /**
     * The script to evaluate in the interpreter.
     **/

    private TclObject callback;

    /**
     * The thread which listens for new connections.
     **/

    private AcceptThread acceptThread;

    /**
     * A string which holds the message of the last exception thrown.
     **/

    String errorMsg;

    /**
     * Creates a new ServerSocketChannel object with the given options.
     * Creates an underlying ServerSocket object, and a thread to handle
     * connections to the socket.
     **/

    public ServerSocketChannel(Interp interp, String localAddr,
            int port, TclObject callback) throws TclException
    {
        InetAddress localAddress = null;

        // Resolve address (if given)
        if (!localAddr.equals(""))
        {
            try
            {
                localAddress = InetAddress.getByName(localAddr);
            }
            catch (UnknownHostException e)
            {
                throw new TclException(interp, "host unkown: "
                    + localAddr, TCL.ERROR);
            }
        }
        this.mode = TclIO.CREAT; // Allow no reading or writing on channel
        this.callback = callback;
        this.cbInterp = interp;

        // Create the server socket.
        try
        {
            if (localAddress == null)
                sock = new ServerSocket(port);
            else
                sock = new ServerSocket(port, 0, localAddress);
        }
        catch (IOException ex)
        {
            throw new TclException(interp, ex.getMessage(), TCL.ERROR);
        }

        acceptThread = new AcceptThread(sock, this);
        
        setChanName("sock" + getNextSockNum());
        errorMsg = new String();
        acceptThread.start();
    }

    synchronized void addConnection(Socket s)
    {
        // Create an event which executes the callback TclString with
        // the arguments sock, addr, port added.
        // First, create a SocketChannel for this Socket object.
        SocketChannel sChan = null;
        try
        {
            sChan = new SocketChannel(cbInterp, s);
            // Register this channel in the channel tables.
            TclIO.registerChannel(cbInterp, sChan);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        SocketConnectionEvent evt = new SocketConnectionEvent(cbInterp,
                callback, sChan.getChanName(),
                s.getInetAddress().getHostAddress(),
                s.getPort());
        cbInterp.getNotifier().queueEvent((TclEvent)evt,TCL.QUEUE_TAIL);
    }
                
        

    /**
     * Perform a read on the ServerSocket - not allowed.
     **/

    String read(Interp interp, int readType, int numBytes)
        throws IOException, TclException
    {
        throw new TclException(interp, "cannot read from a server socket",
            TCL.ERROR);
    }

    /**
     * Write to the channel - not allowed.
     **/

    void write(Interp interp, String outStr)
        throws IOException, TclException
    {
        throw new TclException(interp, "cannot write to a server socket",
            TCL.ERROR);
    }

    void close() throws IOException
    {
        // Stop the event handler thread.
        // this might not happen for up to a minute!
        acceptThread.pleaseStop();
        // Close the socket
        sock.close();
    }

    void flush(Interp interp) throws IOException, TclException
    {
        throw new TclException(interp, "cannot flush a server socket",
            TCL.ERROR);
    }
    void seek(long offset, int mode) throws IOException
    {
        throw new IOException("seek is not supported for socket channels");
    }
    long tell() throws IOException
    {
        throw new IOException("tell is not supported for socket channels");
    }

    boolean eof()
    {
        return false;
    }

    /**
     * Returns the next free channel number
     **/

    private int getNextSockNum()
    {
        int i;
        Hashtable htbl = TclIO.getInterpChanTable(cbInterp);
        for (i = 0; (htbl.get("sock" + i)) != null; i++)
        {
            // Do nothing
        }
        return i;
    }

}



class AcceptThread extends Thread {

    private ServerSocket sock;
    private ServerSocketChannel sschan;
    boolean keepRunning;

    public AcceptThread(ServerSocket s1, ServerSocketChannel s2)
    {
        sock = s1;

        // Every 10 seconds, we check to see if this socket has been closed:
        try {
            sock.setSoTimeout(10000);
        } catch (SocketException e) {}

        sschan = s2;
        keepRunning = true;
    }

    public void run()
    {
        try
        {
            while(keepRunning)
            {
                Socket s = null;
                try {
                    s = sock.accept();
                } 
                catch (InterruptedIOException ex)
                {
                    // Timeout
                    continue;
                }
                catch (IOException ex)
                {
                    // Socket closed
                    break;
                }
                // Get a connection
                sschan.addConnection(s);
            }
        }
        catch (Exception e)
        {
            // Something went wrong.
            e.printStackTrace();
        }
    }

    public void pleaseStop()
    {
        keepRunning = false;
    }
}

