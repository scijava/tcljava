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
     * A string which holds the message of the last exception thrown.
     **/

    private String errorMsg;

    /**
     * Constructor - creates a new SocketChannel object with the given
     * options. Also creates an underlying Socket object, and Input and
     * Output Streams.
     **/

    public SocketChannel(Interp interp, int mode, String localAddr,
            int localPort, boolean async, String address, int port)
        throws TclException
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
        try
        {
            if ((localAddress != null) && (localPort != 0))
                sock = new Socket(addr, port, localAddress, localPort);
            else
                sock = new Socket(addr, port);
        }
        catch (IOException ex)
        {
            throw new TclException(interp, ex.getMessage());
        }

        // Get the Input and Output streams
        try
        {
            reader = new BufferedReader(
                    new InputStreamReader(sock.getInputStream()));
        }
        catch (IOException ex)
        {
            throw new TclException(interp, ex.getMessage());
        }

        try
        {
            writer = new BufferedWriter(
                    new OutputStreamWriter(sock.getOutputStream()));
        }
        catch (IOException ex)
        {
            throw new TclException(interp, ex.getMessage());
        }

        // If we got this far, then the socket has been created.
        // Create the channel name
        setChanName(getNextDescriptor(interp, "sock"));
        errorMsg = new String();
    }

    /**
     * Constructor for making SocketChannel objects from connections
     * made to a ServerSocket.
     **/

    public SocketChannel(Interp interp, Socket s) throws TclException
    {
        this.mode = (TclIO.RDWR|TclIO.RDONLY|TclIO.WRONLY);
        this.sock = s;
        // Get the Input and Output streams
        try
        {
            reader = new BufferedReader(
                    new InputStreamReader(sock.getInputStream()));
        }
        catch (IOException ex)
        {
            throw new TclException(interp, ex.getMessage());
        }

        try
        {
            writer = new BufferedWriter(
                    new OutputStreamWriter(sock.getOutputStream()));
        }
        catch (IOException ex)
        {
            throw new TclException(interp, ex.getMessage());
        }

        // If we got this far, then the socket has been created.
        // Create the channel name
        setChanName(getNextDescriptor(interp, "sock"));
        errorMsg = new String();
    }

        
    /**
     * Perform a read on a SocketChannel.
     *
     * @param interp is used for TclExceptions.
     * @param readType is used to specify the type of read (line, all, etc).
     * @param numBytes is the number of bytes to read (if applicable).
     * @return String of data that was read from the Channel (can not be null)
     * @exception TclExceptiom is thrown if read occurs on WRONLY channel.
     * @exception IOException is thrown when an IO error occurs that was not
     *                  correctly tested for. Most cases should be caught.
     */

    String read(Interp interp, int readType, int numBytes)
        throws IOException, TclException
    {
        // Check mode.
        if ((mode & (TclIO.RDONLY|TclIO.RDWR)) == 0)
            throw new TclException(interp, "channel " + getChanName() +
                    " wasn't opened for reading");

        if ((numBytes < 1) || (readType == TclIO.READ_ALL))
            numBytes = BUF_SIZE;

        char[] buf = new char[numBytes]; // Buffer to read into
        int ret = 0;

        // FIXME: This needs to be a StringBuffer for += all case!
        String returnStr = new String();

        // FIXME: Where does eofCond get set to false before a read?

        try
        {
            // Decide what type of read we are doing.
            switch (readType) {
                case TclIO.READ_ALL:
                    // Try and read the whole stream at once.
                    while ((ret = reader.read(buf, 0, BUF_SIZE)) != -1)
                        returnStr += new String(buf);
                    eofCond = true; // We read to eof, so this must be true
                    break;
                case TclIO.READ_LINE:
                    // Try and read a line. Must assume character data
                    // and that this stream is configured non-binary.
                    returnStr = reader.readLine();
                    break;
                case TclIO.READ_N_BYTES:
                    // Read the specified number of bytes.
                    int total = 0;
                    while ((total += reader.read(buf,total,numBytes))
                        != numBytes) ;
                    returnStr = new String(buf);
                    break;
                default:
                    // Broken?
                    throw new TclException(interp, "unknown read mode: "
                        + readType);
            }
        }
        catch (EOFException e)
        {
            eofCond = true;
            errorMsg = e.getMessage();
            returnStr = new String("");
        }
        catch (IOException e)
        {
            errorMsg = e.getMessage();
            returnStr = new String("");
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage());
        }

        if (ret == -1)
            eofCond = true;
        if (returnStr == null)
        {
            eofCond = true;
            returnStr = new String("");
        }
        
        return returnStr;
    }

    /**
     * Write data to the Socket.
     *
     * @param interp is used for TclExceptions.
     * @param outStr the String to write to the Socket.
     */

    void write(Interp interp, String outStr)
        throws IOException, TclException
    {
        if ((mode & (TclIO.WRONLY|TclIO.RDWR)) == 0)
            throw new TclException(interp, "channel " + getChanName() +
                    " wasn't opened for writing.");
        // Write to the Socket
        try
        {
            writer.write(outStr, 0, outStr.length());
        }
        catch (EOFException e)
        {
            eofCond = true;
            errorMsg = e.getMessage();
        }
        catch (IOException e)
        {
            errorMsg = e.getMessage();
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage());
        }
    }
        
    /**
     * Close the SocketChannel. The channel is only closed, it is
     * the responsibility of the "caller" to remove the channel from
     * the channel table.
     */

    void close() throws IOException
    {
        IOException ex = null;

        try { sock.close(); } catch (IOException e) { ex = e; }
        try { super.close(); } catch (IOException e) { ex = e; }

        if (ex != null)
            throw new IOException(ex.getMessage());
    }

    /**
     * Flush the socket.
     *
     * @exception TclException is thrown when attempting to flush a
     *              read only channel.
     * @exception IOException is thrown for all other flush errors.
     */

    void flush(Interp interp) throws IOException, TclException
    {
        try
        {
            super.flush(interp);
        }
        catch (IOException e)
        {
            errorMsg = e.getMessage();
            throw e;
        }
    }

    /**
     * Move the current Channel pointer.
     * Undefined behaviour for sockets.
     *
     * @param offset The number of bytes to move the pointer.
     * @param mode where to begin incrementing the file pointer; beginning,
     *              current, end.
     */

    void seek(long offset, int mode) throws IOException
    {
        throw new IOException("seek is not supported for socket channels");
    }

    /**
     * Tell the value of the Channel pointer.
     * Undefined in sockets.
     **/

    long tell() throws IOException
    {
        throw new IOException("tell is not supported for socket channels");
    }
}
