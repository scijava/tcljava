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
import java.util.Hashtable;

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
     * The InputStream associated with this socket.
     **/

    private BufferedReader in;

    /**
     * The OutputStream associated with this socket.
     **/

    private BufferedWriter out;

    /**
     * The eof flag is set if a EOFException is thrown during IO.
     * The flag is returned from the eof() call.
     **/

    private boolean eof;

    /**
     * A string which holds the message of the last exception thrown.
     **/

    private String errorMsg;

    /**
     * The main interp.
     **/

    private Interp interp;

    /**
     * Constructor - creates a new SocketChannel object with the given
     * options. Also creates an underlying Socket object, and Input and
     * Output Streams.
     **/

    public SocketChannel(Interp interp, int mode, String localAddr,
            int localPort, boolean async, String address, int port)
        throws TclException
    {
        this.interp = interp;
        InetAddress localAddress = null;
        InetAddress addr = null;

        if (async)
            throw new TclException(interp,
                "Asynchronous socket connection not " +
                "currently implemented", TCL.ERROR);

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
                    + localAddr, TCL.ERROR);
            }
        }

        try
        {
            addr = InetAddress.getByName(address);
        }
        catch (UnknownHostException e)
        {
            throw new TclException(interp, "host unknown: "
                + address, TCL.ERROR);
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
            throw new TclException(interp, ex.getMessage(), TCL.ERROR);
        }

        // Get the Input and Output streams
        try
        {
            in = new BufferedReader(
                    new InputStreamReader(sock.getInputStream()));
        }
        catch (IOException ex)
        {
            throw new TclException(interp, ex.getMessage(), TCL.ERROR);
        }

        try
        {
            out = new BufferedWriter(
                    new OutputStreamWriter(sock.getOutputStream()));
        }
        catch (IOException ex)
        {
            throw new TclException(interp, ex.getMessage(), TCL.ERROR);
        }

        // If we got this far, then the socket has been created.
        // Create the channel name
        setChanName("sock" + getNextSockNum());
        errorMsg = new String();
    }

    /**
     * Constructor for making SocketChannel objects from connections
     * made to a ServerSocket.
     **/

    public SocketChannel(Interp interp, Socket s) throws TclException
    {
        this.interp = interp;
        this.mode = (TclIO.RDWR|TclIO.RDONLY|TclIO.WRONLY);
        this.sock = s;
        // Get the Input and Output streams
        try
        {
            in = new BufferedReader(
                    new InputStreamReader(sock.getInputStream()));
        }
        catch (IOException ex)
        {
            throw new TclException(interp, ex.getMessage(), TCL.ERROR);
        }

        try
        {
            out = new BufferedWriter(
                    new OutputStreamWriter(sock.getOutputStream()));
        }
        catch (IOException ex)
        {
            throw new TclException(interp, ex.getMessage(), TCL.ERROR);
        }

        // If we got this far, then the socket has been created.
        // Create the channel name
        setChanName("sock" + getNextSockNum());
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
                    " wasn't opened for reading", TCL.ERROR);

        if ((numBytes < 1) || (readType == TclIO.READ_ALL))
            numBytes = 4096; // 4k buffer

        char[] buf = new char[numBytes]; // Buffer to read into
        int ret = 0;

        // FIXME: This needs to be a StringBuffer for += all case!
        String returnStr = new String();

        try
        {
            // Decide what type of read we are doing.
            switch (readType) {
                case TclIO.READ_ALL:
                    // Try and read the whole stream at once.
                    while ((ret = in.read(buf, 0, 4096)) != -1)
                        returnStr += new String(buf);
                    eof = true; // We read to eof, so this must be true
                    break;
                case TclIO.READ_LINE:
                    // Try and read a line. Must assume character data
                    // and that this stream is configured non-binary.
                    returnStr = in.readLine();
                    break;
                case TclIO.READ_N_BYTES:
                    // Read the specified number of bytes.
                    int total = 0;
                    while ((total += in.read(buf,total,numBytes))
                        != numBytes) ;
                    returnStr = new String(buf);
                    break;
                default:
                    // Broken?
                    throw new TclException(interp, "unknown read mode: "
                        + readType, TCL.ERROR);
            }
        }
        catch (EOFException e)
        {
            eof = true;
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
            eof = true;
        if (returnStr == null)
        {
            eof = true;
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
                    " wasn't opened for writing.", TCL.ERROR);
        // Write to the Socket
        try
        {
            out.write(outStr, 0, outStr.length());
        }
        catch (EOFException e)
        {
            eof = true;
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
        boolean thrown = false;
        // Close the socket
        try { in.close(); } catch (IOException e) { 
            errorMsg = e.getMessage();
            thrown = true;
        }
        try { out.close(); } catch (IOException e) { 
            errorMsg = e.getMessage(); 
            thrown = true;
        }
        try { sock.close(); } catch (IOException e) { 
            errorMsg = e.getMessage(); 
            thrown = true;
        }
        if (thrown)
            throw new IOException(errorMsg);
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
        // Check mode.
        if ((mode & (TclIO.WRONLY|TclIO.RDWR)) == 0)
            throw new TclException(interp, "channel " + getChanName() +
                    " not opened for writing", TCL.ERROR);
        try
        {
            out.flush();
        }
        catch (EOFException e)
        {
            eof = true;
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

    /**
     * Returns true if the last read reached EOF.
     */

    boolean eof()
    {
        return eof;
    }

    /**
     * Returns the next free channel number
     **/

    private int getNextSockNum()
    {
        int i;
        Hashtable htbl = TclIO.getInterpChanTable(interp);
        for (i = 0; (htbl.get("sock" + i)) != null; i++)
        {
            // Do nothing
        }
        return i;
    }

}
