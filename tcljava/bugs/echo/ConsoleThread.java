import java.io.*;


class ConsoleThread extends Thread {

StringBuffer sbuf = new StringBuffer(100);

// set to true to get extra debug output
private static final boolean debug = true;

// used to keep track of System.in's blocking status
private static boolean sysInBlocks = false;

static {
    try {
	// There is no way to tell whether System.in will block AWT
	// threads, so we assume it does block if we can use
	// System.in.available().

	System.in.available();
	sysInBlocks = true;
    } catch (Exception e) {
	// If System.in.available() causes an exception -- it's probably
	// no supported on this platform (e.g. MS Java SDK). We assume
	// sysInBlocks is false and let the user suffer ...
    }

    if (debug) {
        System.out.println("sysInBlocks = " + sysInBlocks);
    }

}

public static void main(String[] argv) throws Exception {
    ConsoleThread ct = new ConsoleThread();
    
    //ct.run();

    ct.setDaemon(true);
    ct.start();

    while (true) {
       Thread.currentThread().sleep(200);
       Thread.yield();
    }

}

public synchronized void
run()
{
    if (debug) {
        System.out.println("entered ConsoleThread run() method");
    }

    put("% ");

    while (true) {
        getLine();

        if (debug) {
            System.out.println("got line from console");
            System.out.println("\"" + sbuf + "\"");
        }

	sbuf.setLength(0); // empty out sbuf
    }
}

private void getLine() {
    // On Unix platforms, System.in.read() will block the delivery of
    // of AWT events. We must make sure System.in.available() is larger
    // than zero before attempting to read from System.in. Since
    // there is no asynchronous IO in Java, we must poll the System.in
    // every 100 milliseconds.

    int availableBytes = -1;

    if (sysInBlocks) {
	try {
	    // Wait until there are inputs from System.in. On Unix,
	    // this usually means the user has pressed the return key.
            availableBytes = 0;

	    while (availableBytes == 0) {
                availableBytes = System.in.available();

/*
                if (debug) {
                    System.out.println(availableBytes +
                        " bytes can be read from System.in");
                }
*/

		Thread.currentThread().sleep(100);
            }
	} catch (InterruptedException e) {
	    System.exit(0);
	} catch (EOFException e) {
	    System.exit(0);
	} catch (IOException e) {
	    e.printStackTrace();
	    System.exit(0);
	}
    }

    // Loop until user presses return or EOF is reached.
    char c2 = ' ';
    char c = ' ';

    if (debug) {
        System.out.println("now to read from System.in");
        System.out.println("availableBytes = " + availableBytes);
    }

    while (availableBytes != 0) {
	try {
	    int i = System.in.read();

	    if (i == -1) {
		if (sbuf.length() == 0) {
		    System.exit(0);
		} else {
		    return;
		}
	    }
	    c = (char) i;
            availableBytes--;

            if (debug) {
                System.out.print("(" + (availableBytes+1) + ") ");
                System.out.print("'" + c + "', ");
            }

	    // Temporary hack until Channel drivers are complete.  Convert
	    // the Windows \r\n to \n.

	    if (c == '\r') {
	        i = System.in.read();
	        if (i == -1) {
		    if (sbuf.length() == 0) {
		        System.exit(0);
		    } else {
		        return;
		    }
		}
		c2 = (char) i; 
		if (c2 == '\n') {
		  c = c2;
		} else {
		  sbuf.append(c);
		  c = c2;
		}
	    }
	} catch (IOException e) {
	    // IOException shouldn't happen when reading from
	    // System.in. The only exceptional state is the EOF event,
	    // which is indicated by a return value of -1.

	    e.printStackTrace();
	    System.exit(0);
	}

	sbuf.append(c);

        //System.out.println("appending char '" + c + "' to sbuf");

	if (c == '\n') {
	    return;
	}
    }
}

private void
putLine(
    String s)			// The string to print.
{
    put(s);
    put("\n");
}

private
void put(
    String s)			// The string to print.
{
    try {
	System.out.print(s);
    } catch (Exception  e) {
	throw new RuntimeException("unexpected Exception " + e);
    }
}

} // end of class ConsoleThread
