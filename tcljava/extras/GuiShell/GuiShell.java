package tcl.lang;

// We can not compile outside of the tcl.lang package
// the perms inside jacl need to be fixed before this will work
//import tcl.lang.*;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

// we need to incluse swingempty.jar in CLASSPATH so we
// can compile with swing 1.0 and 1.1

import com.sun.java.swing.*;
import com.sun.java.swing.text.*;
import com.sun.java.swing.event.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;


public class GuiShell {

  private static String pre_swing;

  static {
    Class c = UIManager.class;
    String c_name = c.getName();

    pre_swing = "com.sun.java.swing";
    if (c_name.startsWith(pre_swing)) {
        pre_swing = "javax.swing";
    }

  }

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
    
    
    
    frame = new JFrame("Main");
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
      interp.setVar("argv0", "tcl.lang.Shell", TCL.GLOBAL_ONLY);
      interp.setVar("tcl_interactive", "1", TCL.GLOBAL_ONLY);

      interp.setVar("argv", TclList.newInstance(),  TCL.GLOBAL_ONLY);
      interp.setVar("argc", "0", TCL.GLOBAL_ONLY);

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
      stdout = System.out;
      stderr = System.err;
      pin  = new PipedInputStream();
      pout = new PipedOutputStream(pin);
      ps = new PrintStream(pout);
      System.setOut(ps);
      System.setErr(ps);
    } catch (java.io.IOException e) {
      e.printStackTrace(stderr);
    }

    
    ThreadedStreamReader tsr = new ThreadedStreamReader();
    Thread t = new Thread(tsr);    
    //t.setPriority(Thread.MIN_PRIORITY);
    t.setDaemon(true);
    t.start();

  }



  public class ThreadedStreamReader implements Runnable {
    
    public void run() {
      BufferedReader br =
	new BufferedReader(new InputStreamReader(pin));
      
      String nextline = null;
      
      while (true) {
	try {
	nextline = br.readLine() + "\n";
	doc.insertString(doc.getLength(), nextline, null);
	edit.setCaretPosition(doc.getLength());
	
	}  catch (IOException e) {
	  e.printStackTrace(stderr);
	} catch (BadLocationException e) {
	  e.printStackTrace(stderr);
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
    
    //System.out.println("now to process event");
    

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
      
      check_code: {
	if (code == TCL.RETURN) {
	  code = interp.updateReturnInfo();
	  if (code == TCL.OK) {
	    break check_code;
	  }
	}
	
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
    }
    

    //System.out.println("done processing event");
    

    //try to read any data still floating around in stdout or stderr

    System.out.flush();
    System.err.flush();

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



  //append this string into the text widget
  public void append(String str) {

    try {
      doc.insertString(doc.getLength(),str,null);
    } catch (BadLocationException e) {
      throw new RuntimeException(e.toString());
    }

  }


  
  //delete char from the end of the widget
  public void removeLastChar() {

    try {
      doc.remove(doc.getLength()-1,1);
    } catch (BadLocationException e) {
      throw new RuntimeException(e.toString());
    }

  }
  


  

  public class Editor extends JEditorPane {
   
    private final boolean debug = false; 

    protected void processComponentKeyEvent(KeyEvent e) {

      if (debug) {
          //System.out.println("processing " + e);
      }

      
      if (e.getKeyCode() != 0) {
	return; //only process key typed commands
      }

      char c = e.getKeyChar();

      if (debug) {
          System.out.println("char is '" + c + "' = " + ((int) c));
      }


      //if they pressed Return

      if (c == KeyEvent.VK_ENTER) {

        if (debug) {
	    System.out.println("added newline");
        }

	cmd_buff.append("\n");
	append("\n");

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

	//letter deleted

        if (debug) {
	    System.out.println("letter deleted");
        }

	//stop deleting at front of this line

	if (cur_line_chars != 0) {
	  cmd_buff.setLength(cmd_buff.length() - 1);
	  cur_line_chars--;
	  removeLastChar();
	} else {
          if (debug) {
	      System.out.println("begin of cur_line");
          }
	}

      }

      else {
	//for any other letter

	cmd_buff.append(c);
	cur_line_chars++;
	append(String.valueOf(c));

      }



      /*
      if (debug) {
          System.out.println("after event process");
          System.out.println("cmd_buff is \"" + cmd_buff + "\"");
          System.out.println("cmd_buff len is " + cmd_buff.length());
          System.out.println("cur_line_chars is " + cur_line_chars);
      }
      */


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

  private transient PrintStream stdout      = null;
  private transient PrintStream stderr      = null;
  private transient Thread messageThread    = null;

  private transient StringBuffer cmd_buff = new StringBuffer(200);
  private transient int cur_line_chars = 0;
  private transient int cmd_start_index = 0;

  private transient Interp interp = new Interp();

}
