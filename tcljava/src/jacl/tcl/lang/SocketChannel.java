/*
 * SocketChannel.java
 *
 * Implements a socket channel.
 */
package tcl.lang;
import java.io.*;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The SocketChannel class implements a channel object for Socket
 * connections, created using the socket command.
 **/

public class SocketChannel extends Channel {

    /**
     * The java Socket object associated with this Channel
     **/

    private Socket sock;

    /**
     * Constructor - creates a new SocketChannel object with the given
     * options. Also creates an underlying Socket object, and Input and
     * Output Streams.
     **/

    public SocketChannel(Interp interp, int mode, String localAddr,
            int localPort, boolean async, String address, int port)
        throws IOException, TclException
    {
        InetAddress localAddress = null;
        InetAddress addr = null;

        if (async)
            throw new TclException(interp,
                "Asynchronous socket connection not " +
                "currently implemented");

        // Resolve addresses
        if (!localAddr.equals(""))
        {
            try
            {
                localAddress = InetAddress.getByName(localAddr);
            }
            catch (UnknownHostException e)
            {
                throw new TclException(interp, "host unknown: "
                    + localAddr);
            }
        }

        try
        {
            addr = InetAddress.getByName(address);
        }
        catch (UnknownHostException e)
        {
            throw new TclException(interp, "host unknown: " + address);
        }


        // Set the mode of this socket.
        this.mode = mode;

        // Create the Socket object

        if ((localAddress != null) && (localPort != 0))
            sock = new Socket(addr, port, localAddress, localPort);
        else
            sock = new Socket(addr, port);

        // Get the Input and Output streams
        reader = new BufferedReader(
            new InputStreamReader(sock.getInputStream()));

        writer = new BufferedWriter(
            new OutputStreamWriter(sock.getOutputStream()));

        // If we got this far, then the socket has been created.
        // Create the channel name
        setChanName(TclIO.getNextDescriptor(interp, "sock"));
    }

    /**
     * Constructor for making SocketChannel objects from connections
     * made to a ServerSocket.
     **/

    public SocketChannel(Interp interp, Socket s)
        throws IOException, TclException
    {
        this.mode = TclIO.RDWR;
        this.sock = s;

        reader = new BufferedReader(
            new InputStreamReader(sock.getInputStream()));

        writer = new BufferedWriter(
            new OutputStreamWriter(sock.getOutputStream()));

        // If we got this far, then the socket has been created.
        // Create the channel name
        setChanName(TclIO.getNextDescriptor(interp, "sock"));
    }

    /**
     * Close the SocketChannel.
     */

    void close() throws IOException
    {
        // Invoke super.close() first since it might write an eof char
        try {
            super.close();
        } finally {
            sock.close();
        }
    }
}
