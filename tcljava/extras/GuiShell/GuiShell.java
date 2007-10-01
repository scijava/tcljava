import tcl.lang.*;

import java.io.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;

public class GuiShell {

  private static String pre_swing = "javax.swing";

  public GuiShell() {

    //init the platform defined look and feel if there is one

    try {
      if (true || System.getProperty("os.name").equalsIgnoreCase("mac os")) {
	UIManager.setLookAndFeel(pre_swing + ".plaf.mac.MacLookAndFeel");
      } else {
	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
    }
    catch (ClassNotFoundException e) {}
    catch (UnsupportedLookAndFeelException e) {}
    catch (IllegalAccessException e) {}
    catch (InstantiationException e) {}
    
    
    
    frame = new JFrame("GuiShell");
    frame.setSize(500,350);
    frame.setLocation(100,100);
    frame.addWindowListener(closer);

    edit = new Editor();

    edit.setEditable(true);

    edit.setFont( new Font("ariel",Font.PLAIN,16) );

    doc = edit.getDocument();

    append("% ");

    JScrollPane scroller = new JScrollPane(edit,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);


    frame.getContentPane().add("Center", scroller);

    frame.setVisible(true);
  }
  




  public void setupInterp() {

    try {
      interp.setVar("argv0", TclString.newInstance("tcl.lang.Shell"), TCL.GLOBAL_ONLY);
      interp.setVar("tcl_interactive", TclString.newInstance("1"), TCL.GLOBAL_ONLY);

      interp.setVar("argv", TclList.newInstance(),  TCL.GLOBAL_ONLY);
      interp.setVar("argc", TclString.newInstance("0"), TCL.GLOBAL_ONLY);

    } catch (TclException e) {
	throw new TclRuntimeError("unexpected TclException: " + e);
    }

    ThreadedTclEventLoop tsr = new ThreadedTclEventLoop();
    Thread t = new Thread(tsr);
    t.setPriority(Thread.MIN_PRIORITY);
    t.setDaemon(true);
    t.start();
  }

  public class ThreadedTclEventLoop implements Runnable {
    
    public void run() {
      Notifier notifier = interp.getNotifier();
      while (true) {
	Thread.yield();
	Thread.yield();
	notifier.doOneEvent(TCL.ALL_EVENTS);
      }
    }
    
  }

  public void setupStreamReaders() {

    try {
      // Save original stdout and stderr
      _stdout = System.out;
      _stderr = System.err;

      pout = new PipedOutputStream();
      ps = new PrintStream(pout);

      // Connect the PipedInputStream to the PipedOutputStream,
      // so that a read on the pin instance will read data
      // from the pout instance.

      pin  = new PipedInputStream();
      pin.connect(pout);

      // Use Jacl's IO redirection API to send stdout and stderr
      // to the PipedOutputStream.

      StdChannel.setOut(ps);
      StdChannel.setErr(ps);

    } catch (java.io.IOException e) {
      e.printStackTrace(_stderr);
    }

    ThreadedStreamReader tsr = new ThreadedStreamReader();
    Thread t = new Thread(tsr);
    //t.setPriority(Thread.MIN_PRIORITY);
    t.setDaemon(true);
    t.start();
  }
  
  // This class reads from a PipedInputStream, it is connected
  // to a PipedOutputStream that has been set as the output
  // channel for both stdout and stderr in the Jacl interpreter.
  // The result is that a command like "puts HELLO" will result
  // in the string "HELLO" being read by this method.

  public class ThreadedStreamReader implements Runnable {

    public void run() {
      final boolean debug = false;

      if (debug) {
          _stderr.println("Entered ThreadedStreamReader.run()");
      }

      BufferedReader br =
	new BufferedReader(new InputStreamReader(pin));

      String nextline = null;

      while (true) {
	try {
	  nextline = br.readLine() + "\n";
          if (debug) {
              _stderr.println("readLine() -> \"" + nextline + "\"");
          }
	  doc.insertString(doc.getLength(), nextline, null);
	  edit.setCaretPosition(doc.getLength());
	}  catch (IOException e) {
	  e.printStackTrace(_stderr);
	} catch (BadLocationException e) {
	  e.printStackTrace(_stderr);
	}
      }

    }

  }
  
  


  public static void main(String s[]) {
    GuiShell gs = new GuiShell();

    gs.setupInterp(); //create thread for interp

    gs.setupStreamReaders(); //create thread for stream readers

  }



  public boolean isCommandComplete() {
        
    String cmd = cmd_buff.toString();
      
    //find out if it a complete Tcl command yet
    
    //System.out.println("cmd is \"" + cmd + "\"");

    return Interp.commandComplete(cmd);
  }



  public void processCommand() {
    final boolean debug = false;
    
    if (debug) {
        System.out.println("now to process event");
    }

    if (cmd_buff.length() == 0) {
      return;
    }


    ConsoleEvent evt = new ConsoleEvent();
    interp.getNotifier().queueEvent(evt, TCL.QUEUE_TAIL);
    evt.sync();
    
    if (evt.evalResult != null) {
      String s = evt.evalResult.toString();
      if (s.length() > 0) {
	append(s);
	append("\n");
      }
    } else {
      TclException e = evt.evalException;
      int code = e.getCompletionCode();
      switch (code) {
	case TCL.ERROR:
	  append(interp.getResult().toString());
	  append("\n");
	  break;
	case TCL.BREAK:
	  append("invoked \"break\" outside of a loop\n");
	  break;
	case TCL.CONTINUE:
	  append("invoked \"continue\" outside of a loop\n");
	  break;
	default:
	  append("command returned bad code: " + code + "\n");
      }
    }

    if (debug) {
        System.out.println("done processing event");
    }    

    Thread.yield();
    Thread.yield();

  }

  

  public class ConsoleEvent extends TclEvent {
    TclObject evalResult;
    TclException evalException;
    
    public ConsoleEvent() {
      evalResult = null;
      evalException = null;
    }
    
    public int processEvent(int flags)
    {
      try {
	interp.eval(cmd_buff.toString(), 0);
	evalResult = interp.getResult();
	evalResult.preserve();
      } catch (TclException e) {
	evalException = e;
      }
      
      return 1;
    } 
  }



  // append this string into the text widget

  public void append(String str) {

    try {
      doc.insertString(doc.getLength(),str,null);
      edit.setCaretPosition(doc.getLength());
    } catch (BadLocationException e) {
      throw new RuntimeException(e.toString());
    }

  }

  //delete char from the end of the widget
  public void removeLastChar() {

    try {
      doc.remove(doc.getLength()-1,1);
      edit.setCaretPosition(doc.getLength());
    } catch (BadLocationException e) {
      throw new RuntimeException(e.toString());
    }

  }


  public class Editor extends JEditorPane {

    private static final boolean debug = false; 

    protected void processComponentKeyEvent(KeyEvent e) {

      if (false && debug) {
          System.out.println("processComponentKeyEvent " + e);
      }

      if (e.getKeyCode() != 0) {
        // Ignore KEY_PRESSED and KEY_RELEASED, only process KEY_TYPED
        // events in this method.
      
	return;
      }

      char c = e.getKeyChar();

      if (debug) {
          System.out.println("char is '" + c + "' = " + ((int) c));
      }


      // if they pressed Return

      if (c == KeyEvent.VK_ENTER) {

        if (debug) {
	    System.out.println("added newline");
        }

	cmd_buff.append("\n");
	//append("\n");

	if (isCommandComplete()) {

	  processCommand();

	  cmd_buff.setLength(0);

	  append("% ");

	} else {

	  append("> ");
	}

	cur_line_chars = 0;

      }


      //if they pressed Delete or Backspace

      else if (c == KeyEvent.VK_BACK_SPACE ||
	  c ==  KeyEvent.VK_DELETE) {

	// letter deleted

        if (debug) {
	    System.out.println("letter deleted");
        }

	// stop deleting at front of this line

	if (cur_line_chars != 0) {
	  cmd_buff.setLength(cmd_buff.length() - 1);
	  cur_line_chars--;
	} else {
          if (debug) {
	      System.out.println("begin of cur_line");
          }
	}

      }

      else {
	cmd_buff.append(c);
	cur_line_chars++;
      }

      if (debug) {
          System.out.println("after event process");
          System.out.println("cmd_buff is \"" + cmd_buff + "\"");
          System.out.println("cmd_buff len is " + cmd_buff.length());
          System.out.println("cur_line_chars is " + cur_line_chars);
      }

      edit.setCaretPosition(doc.getLength());
    }

  }


  

  public static class WindowClosingHandler extends WindowAdapter {
    public void windowClosing(WindowEvent e) {
      System.exit(0);
    }
  }


  public static WindowListener closer = new WindowClosingHandler();

  private transient JFrame frame;
  private transient Editor edit;
  private transient Document doc;

  private transient PipedInputStream pin    = null;
  private transient PipedOutputStream pout  = null;
  private transient PrintStream ps          = null;

  private transient PrintStream _stdout      = null;
  private transient PrintStream _stderr      = null;
  private transient Thread messageThread    = null;

  private transient StringBuffer cmd_buff = new StringBuffer(200);
  private transient int cur_line_chars = 0;
  private transient int cmd_start_index = 0;

  private transient Interp interp = new Interp();

}
