package nsjava;

import java.sql.*;
import java.util.*;
import nsjava.*;

public class NsjavaTests {

    public NsjavaTests() 
    {
        NsLog.write("Debug","NsjavaTests Constructor");
    }

  public Vector list_users(String[] fields)
  throws SQLException
    {
      NsPg    db;
      NsSet   selection;
      String  key;
      String  value;
      String  pools[];
      int     i = 0;
      Integer idx1;
      Integer idx2;
      Hashtable one_user;
      Vector users;

      db    = new NsPg();
      pools = db.pools();

      NsLog.write("Debug","inside test_nsjvm::list_users2");

      for(i = 0; i < pools.length; i++) {
          NsLog.write("Debug", "pool[" + i + "] = " + pools[i]);
      }
      
      selection = db.select("select " + fields[0] + "," + fields[1] + " from users");
      idx1       = selection.find(fields[0]);
      idx2       = selection.find(fields[1]);
      users      = new Vector();

      for(i = 0; db.getrow(selection) == true; i++) {

          one_user      = new Hashtable();
          key           = selection.key(idx1);
          value         = selection.value(idx1);
          one_user.put(key,value);
          key           = selection.key(idx2);
          value         = selection.value(idx2);
          one_user.put(key,value);
          users.addElement(one_user);

          NsLog.write("Debug", "query row " + i + ": " + key + " = " + value);
      }

      db.releasehandle();
      selection.free();

      return users;
    }

  public void test_command(String[] message) 
    {
      System.err.println("msg[0] = " + message[0]);
      System.err.println("test_nsjvm.test_command msg: " + message);
      NsLog.write("Debug","test_nsjvm: " + message[0]);

    }


  public void ab(String[] message) 
    {
      String tid = message[0];
      String cnt = message[1];
      NsLog.write("Debug","ab" + cnt + " cnt = " + cnt);
    }

}
