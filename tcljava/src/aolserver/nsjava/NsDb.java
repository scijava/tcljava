/*
 * Copyright (c) 2000 Daniel Wickstrom
 *
 * See also http://www.aolserver.com for details on the AOLserver
 * web server software
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 675 Mass Ave, Cambridge,
 * MA 02139, USA.
 * 
 */

package nsjava;

import java.sql.*;


/**
 * <P>NsDb provides a java wrapper around the aolserver db api.  The 
 * interface is similar to the tcl db interface except that the db handle 
 * is maintained in the NsDb object.
 *
 * @author Daniel Wickstrom
 * @see NsLog
 * @see NsPg
 * @see NsSet
 * @see NsTcl
 */
public class NsDb 
{
  protected String handle;
  protected String poolname;
  protected Integer nhandles;
    
  /**
   * Create a new Database connection.  A database handle is obtained 
   * automatically.
   *
   * @exception SQLException if a database access error occurred
   */
  public NsDb() throws SQLException {
    this.nhandles = new Integer(1);
    this.handle   = this.gethandle();
    this.poolname = this.poolname();
  }

  /**
   * Create a new Database connection.  A database handle is obtained 
   * automatically.
   *
   * @param poolname Grab the handle from the specified pool.
   * @exception SQLException if a database access error occurred
   */
  public NsDb(String poolname) throws SQLException {
    this.nhandles = new Integer(1);
    this.poolname = poolname;
    this.handle   = this.gethandle(poolname);
  }

  /**
   * End a database connection - release the handle.
   *
   * @exception SQLException if a database access error occurred
   */
  public void finalize() throws SQLException {
      //if(this.handle != null)  
      //this.releasehandle();
  }

  /**
   * Open a database connection. Not implemented yet.
   *
   * @param handle database handle.
   * @param driver database driver name
   * @param datasource the db datasource.
   * @param user the username.
   * @param password the user's password.
   * @return NoSuchMethodException
   * @exception NoSuchMethodException for any calls to this method.
   */
  protected native String _open(String handle, String driver, String datasource, 
                             String user, String password) 
    throws NoSuchMethodException;

  /**
   * Close a database connection. Not implemented yet.
   *
   * @param handle database handle.
   * @return NoSuchMethodException
   * @exception NoSuchMethodException for any calls to this method.
   */
  protected native void _close(String handle) throws NoSuchMethodException;

  /**
   * Return a list of the poolnames that are available.
   *
   * @return a list of available pools.
   */
  public native String[] pools();

  /**
   * Mark all database handles for the specified pool as stale, and return 
   * to the pool.
   *
   * @param pool the poolname to bounce.
   * @exception SQLException if a database access error occurred
   */
  protected native void bouncepool(String pool) throws SQLException;

  /**
   * Native method for obtaining a db handle.
   *
   * @param timeout timeout in seconds
   * @param poolname poolname to use when getting the handle.
   * @param nhandles the number of handles to get. Only one is supported.
   * @return Reference to tcl database handle.
   * @exception SQLException if a database access error occurred
   */
  protected native String _gethandle(String timeout, String poolname, 
                                   String nhandles) throws SQLException;

  /**
   * Get the reference to the stored database handle.
   *
   * @return Reference to tcl database handle.
   */
  public String getPointer() {
    return this.handle;
  }

  /**
   * Method for obtaining a handle from the default pool.
   *
   * @return Reference to tcl database handle.
   * @exception SQLException if a database access error occurred
   */
  public String gethandle() 
    throws SQLException 
    {
      String poolname;
      if(this.poolname == null) {
        poolname = "undef";
      }
      else {
        poolname = this.poolname;
      }

      return this._gethandle("0",poolname,this.nhandles.toString());
    }

  /**
   * Method for obtaining a handle from the default pool with a timeout 
   * specified.
   *
   * @param timeout timeout in seconds
   * @return Reference to tcl database handle.
   * @exception SQLException if a database access error occurred
   */
  public String gethandle(int timeout) 
    throws SQLException 
    {
      String t = (new Integer(timeout)).toString();
      return this._gethandle(t,"undef",nhandles.toString());
    }

  /**
   * Method for obtaining a handle from the default pool with a timeout 
   * specified.
   *
   * @param timeout timeout in seconds
   * @return Reference to tcl database handle.
   * @exception SQLException if a database access error occurred
   */
  public String gethandle(Integer timeout) 
    throws SQLException 
    {
      String t = timeout.toString();
      return this._gethandle(t,"undef",nhandles.toString());
    }

  /**
   * Method for obtaining a handle from the named pool with a timeout 
   * specified.
   *
   * @param timeout timeout in seconds
   * @param poolname poolname to use when getting the handle.
   * @return Reference to tcl database handle.
   * @exception SQLException if a database access error occurred
   */
  public String gethandle(int timeout, String poolname) 
    throws SQLException 
    {
      String t = (new Integer(timeout)).toString();
      return this._gethandle(t,poolname,this.nhandles.toString());
    }

  /**
   * Method for obtaining a handle from the named pool with a timeout 
   * specified.
   *
   * @param timeout timeout in seconds
   * @param poolname poolname to use when getting the handle.
   * @return Reference to tcl database handle.
   * @exception SQLException if a database access error occurred
   */
  public String gethandle(Integer timeout, String poolname) 
    throws SQLException 
    {
      String t = timeout.toString();
      return this._gethandle(t,poolname,this.nhandles.toString());
    }


  /**
   * Method for obtaining a handle from the named pool.
   *
   * @param poolname poolname to use when getting the handle.
   * @return Reference to tcl database handle.
   * @exception SQLException if a database access error occurred
   */
  public String gethandle(String poolname) 
    throws SQLException 
    {
      return this._gethandle("0",poolname,this.nhandles.toString());
    }

  /**
   * Method for obtaining a db handle.
   *
   * @param timeout timeout in seconds
   * @param poolname poolname to use when getting the handle.
   * @param nhands the number of handles to get. Currently, the getting of 
   * more than one handle is not supported.  This will change in the future
   * so that more than one handle can be obtained.
   * @return Reference to tcl database handle.
   * @exception SQLException if a database access error occurred
   */
  public String gethandle(int timeout, String poolname, int nhands) 
    throws SQLException 
    {        
      String t = (new Integer(timeout)).toString();
      String n = (new Integer(nhands)).toString();
      return this._gethandle(t,poolname,n);
    }

  /**
   * Method for obtaining a db handle.
   *
   * @param timeout timeout in seconds
   * @param poolname poolname to use when getting the handle.
   * @param nhands the number of handles to get. Currently, the getting of 
   * more than one handle is not supported.  This will change in the future
   * so that more than one handle can be obtained.
   * @return Reference to tcl database handle.
   * @exception SQLException if a database access error occurred
   */
  public String gethandle(Integer timeout, String poolname, int nhands) 
    throws SQLException 
    {        
      String t = timeout.toString();
      String n = (new Integer(nhands)).toString();
      return this._gethandle(t,poolname,n);
    }

  /**
   * Native method for obtaining the last db exception message.
   *
   * @param handle the reference to the database handle.
   * @return Exception message.
   * @exception SQLException if a database access error occurred
   */
  protected native String _exception(String handle) throws SQLException;

  /**
   * Method for obtaining the last db exception message.
   *
   * @return Exception message.
   * @exception SQLException if a database access error occurred
   */
  public String exception() throws SQLException {
    if(this.handle == null) {
      return new String("no handles are active");
    }
    return this._exception(this.getPointer());
  }

  /**
   * Native method for obtaining the poolname associated with the db handle.
   *
   * @param handle the database handle.
   * @return poolname
   * @exception SQLException if a database access error occurred
   */
  protected native String _poolname(String handle) throws SQLException;

  /**
   * Method for obtaining the poolname associated with the db handle.
   *
   * @return Exception message.
   * @exception SQLException if a database access error occurred
   */
  public String poolname() throws SQLException {
    if(this.handle == null) {
      return null;
    }
    return this._poolname(this.getPointer());
  }

  /**
   * Native method for obtaining the password associated with the db handle.
   *
   * @param handle the database handle.
   * @return the password.
   * @exception SQLException if a database access error occurred
   */
  protected native String _password(String handle) throws SQLException;

  /**
   * Method for obtaining the password associated with the db handle.
   *
   * @return the password.
   * @exception SQLException if a database access error occurred
   */
  public String password() throws SQLException {
    if(this.handle == null) {
      return null;
    }
    return this._password(this.getPointer());
  }

  /**
   * Native method for obtaining the user associated with the db handle.
   *
   * @param handle the database handle.
   * @return the user.
   * @exception SQLException if a database access error occurred
   */
  protected native String _user(String handle) throws SQLException;

  /**
   * Method for obtaining the user associated with the db handle.
   *
   * @return the user.
   * @exception SQLException if a database access error occurred
   */
  public String user() throws SQLException {
    if(this.handle == null) {
      return null;
    }
    return this._user(this.getPointer());
  }

  /**
   * Native method for obtaining the dbtype associated with the db handle.
   *
   * @param handle the database handle.
   * @return the dbtype.
   * @exception SQLException if a database access error occurred
   */
  protected native String _dbtype(String handle) throws SQLException;

  /**
   * Method for obtaining the dbtype associated with the db handle.
   *
   * @return the dbtype.
   * @exception SQLException if a database access error occurred
   */
  public String dbtype() throws SQLException {
    if(this.handle == null) {
      return null;
    }
    return this._dbtype(this.getPointer());
  }

  /**
   * Native method for obtaining the driver associated with the db handle.
   *
   * @param handle the database handle.
   * @return the driver.
   * @exception SQLException if a database access error occurred
   */
  protected native String _driver(String handle) throws SQLException;

  /**
   * Method for obtaining the driver associated with the db handle.
   *
   * @return the driver.
   * @exception SQLException if a database access error occurred
   */
  public String driver() throws SQLException {
    if(this.handle == null) {
      throw new SQLException("no handles allocated for driver cmd");
    }
    return this._driver(this.getPointer());
  }

  /**
   * Native method for obtaining the datasource associated with the db handle.
   *
   * @param handle the database handle.
   * @return the datasource.
   * @exception SQLException if a database access error occurred
   */
  protected native String _datasource(String handle) throws SQLException;

  /**
   * Method for obtaining the datasource associated with the db handle.
   *
   * @return the datasource.
   * @exception SQLException if a database access error occurred
   */
  public String datasource() throws SQLException {
    if(this.handle == null) {
      return null;
    }
    return this._datasource(this.getPointer());
  }

  /**
   * Native method for disconnecting associated handle.
   *
   * @param handle the database handle.
   */
  protected native void _disconnect(String handle);

  /**
   * Method for disconnecting associated handle.
   *
   */
  public void disconnect() {
    this._disconnect(this.getPointer());
  }

  /**
   * Native method for flushing the associated handle.
   *
   * @param handle the database handle.
   * @exception SQLException if a database access error occurred
   */
  protected native void _flush(String handle) throws SQLException;

  /**
   * Method for flushing the associated handle.
   *
   * @exception SQLException if a database access error occurred
   */
  public void flush() throws SQLException {
    if(this.handle == null) {
      throw new SQLException("attempt to flush non-existant handle");
    }
    this._flush(this.getPointer());
  }

  /**
   * Native method for binding the results of an exec statements..
   *
   * @param handle the database handle.
   * @return the ns_set reference
   * @exception SQLException if a database access error occurred
   */
  protected native String _bindrow(String handle) throws SQLException;

  /**
   * Method for binding the results of an exec statements..
   *
   * @return the NsSet object.
   * @exception SQLException if a database access error occurred
   */
  public NsSet bindrow() 
    throws SQLException 
    {
      return new NsSet(this._bindrow(this.getPointer()));
    }

  /**
   * Native method for releasing the database handle.
   *
   * @param handle the database handle.
   */
  protected native void _releasehandle(String handle);

  /**
   * Method for releasing the database handle.
   *
   * @exception SQLException if a database access error occurred
   */
  public void releasehandle() throws SQLException {
    if(this.handle == null) {
      throw new SQLException("attempt to release non-existant handle");
    }
    this._releasehandle(this.getPointer());
    this.handle = null;
  }

  /**
   * Native method for resetting the database handle.
   *
   * @param handle the database handle.
   * @exception SQLException if a database access error occurred
   */
  protected native String _resethandle(String handle) throws SQLException;

  /**
   * Method for resetting the database handle.
   *
   * @exception SQLException if a database access error occurred
   */
  public Integer resethandle() throws SQLException {
    return new Integer(this._resethandle(this.getPointer()));
  }

  /**
   * Native method for cancelling the current operation.
   *
   * @param handle the database handle.
   * @exception SQLException if a database access error occurred
   */
  protected native String _cancel(String handle) throws SQLException;

  /**
   * Method for cancelling the current operation.
   *
   * @exception SQLException if a database access error occurred
   */
  public void cancel() throws SQLException {
    if(this.handle == null) {
      throw new SQLException("cancel attempted with null handle");
    }
    this._cancel(this.getPointer());
  }

  /**
   * Native method to test if the connection is made.
   *
   * @param handle the database handle.
   */
  protected native String _connected(String handle);

  /**
   * Method to test if the connection is made.
   *
   */
  public boolean connected() {
    if(this.handle == null) {
      return false;
    }
    if(this._connected(this.getPointer()).equals("1")) {
        return true;
    }
    else {
        return false;
    }
  }

  /**
   * Native method to exec stored procedures.
   *
   * @param handle the database handle.
   * @exception SQLException if a database access error occurred
   */
  protected native String _sp_exec(String handle) throws SQLException;

  /**
   * Method to exec stored procedures.
   *
   * @exception SQLException if a database access error occurred
   */
  public String sp_exec() throws SQLException {
    return this._sp_exec(this.getPointer());
  }

  /**
   * Native method to get the stored procedure returncode.
   *
   * @param handle the database handle.
   * @return the return code.
   * @exception NoSuchMethodException method is not implemented yet.
   */
  protected native String _sp_returncode(String handle) 
      throws NoSuchMethodException;

  /**
   * Method to get the stored procedure returncode.
   *
   * @return the return code.
   * @exception NoSuchMethodException Method is not implemented yet.
   */
  public String sp_returncode() throws NoSuchMethodException {
    return this._sp_returncode(this.getPointer());
  }

  /**
   * Native method which currently throws an exception if used.
   *
   * @param handle the database handle.
   * @exception NoSuchMethodException Method is not implemented yet.
   */
  protected native String _sp_getparams(String handle) 
      throws NoSuchMethodException;

  /**
   * Method which currently throws an exception if used.
   *
   * @exception NoSuchMethodException Method is not implemented yet.
   */
  public String sp_getparams() throws NoSuchMethodException {
    return this._sp_getparams(this.getPointer());
  }

  /**
   * Native method which performs dml operations.
   *
   * @param handle the database handle.
   * @param jsql sql for dml statement.
   * @exception SQLException if a database access error occurred
   */
  protected native void _dml(String handle, String jsql) throws SQLException;

  /**
   * Method which performs dml operations.
   *
   * @param sql sql for dml statement.
   * @exception SQLException if a database access error occurred
   */
  public void dml(String sql) throws SQLException {
    if(this.handle == null) {
      this.handle = this.gethandle(this.poolname);
    }
    this._dml(this.getPointer(),sql);
  }

  /**
   * Native method which performs 0or1row operation.
   *
   * @param handle the database handle.
   * @param jsql sql for select statement.
   * @return reference to NsSet object from select operation.
   * @exception SQLException if a database access error occurred
   */
  protected native String _select_0or1row(String handle, String jsql) 
    throws SQLException;

  /**
   * Method which performs 0or1row operation.
   *
   * @param sql sql for select statement.
   * @return NsSet object from select operation.
   * @exception SQLException if a database access error occurred
   */
  public NsSet select_0or1row(String sql) throws SQLException {
    if(this.handle == null) {
      this.handle = this.gethandle(this.poolname);
    }
    return new NsSet(this._select_0or1row(this.getPointer(),sql));
  }

  /**
   * Native method which performs 1row operation.
   *
   * @param handle the database handle.
   * @param jsql sql for select statement.
   * @return reference to NsSet object from select operation.
   * @exception SQLException if a database access error occurred
   */
  protected native String _select_1row(String handle, String jsql) 
    throws SQLException;

  /**
   * Method which performs 1row operation.
   *
   * @param sql sql for select statement.
   * @return NsSet object from select operation.
   * @exception SQLException if a database access error occurred
   */
  public NsSet select_1row(String sql) throws SQLException {
    if(this.handle == null) {
      this.handle = this.gethandle(this.poolname);
    }
    return new NsSet(this._select_1row(this.getPointer(),sql));
  }

  /**
   * Native method which performs select operation.
   *
   * @param handle the database handle.
   * @param jsql sql for select statement.
   * @return reference to NsSet object from select operation.
   * @exception SQLException if a database access error occurred
   */
  protected native String _select(String handle, String jsql) 
      throws SQLException;

  /**
   * Method which performs select operation.
   *
   * @param sql sql for select statement.
   * @return NsSet object from select operation.
   * @exception SQLException if a database access error occurred
   */
  public NsSet select(String sql) throws SQLException {
    if(this.handle == null) {
      this.handle = this.gethandle(this.poolname);
    }
    return new NsSet(this._select(this.handle,sql));
  }

  /**
   * Native method which performs exec operation.
   *
   * @param handle the database handle.
   * @param jsql sql for select statement.
   * @return NS_ROW or NS_DML
   * @exception SQLException if a database access error occurred
   */
  protected native String _exec(String handle, String jsql) 
      throws SQLException;

  /**
   * Method which performs exec operation.
   *
   * @param jsql sql for select statement.
   * @return NS_ROW or NS_DML
   * @exception SQLException if a database access error occurred
   */
  public String exec(String sql) throws SQLException {
    if(this.handle == null) {
      this.handle = this.gethandle(this.poolname);
    }
    return this._exec(this.getPointer(),sql);
  }

  /**
   * Native method which is not yet implemented.
   *
   * @param handle the database handle.
   * @param jsql sql for select statement.
   * @exception NoSuchMethodException if this method is called.
   */
  protected native String _interpretsqlfile(String handle, String jsql) 
    throws NoSuchMethodException;

  /**
   * Method which is not yet implemented.
   *
   * @param jsql sql for select statement.
   * @exception NoSuchMethodException if this method is called.
   */
  public String interpretsqlfile(String sql) throws NoSuchMethodException {
    return this._interpretsqlfile(this.getPointer(),sql);
  }

  /**
   * Native method which is not yet implemented.
   *
   * @param handle the database handle.
   * @param jsql sql for select statement.
   * @exception NoSuchMethodException if this method is called.
   */
  protected native String _sp_start(String handle, String jsql) 
    throws NoSuchMethodException;

  /**
   * Method which is not yet implemented.
   *
   * @param sql sql for select statement.
   * @exception NoSuchMethodException if this method is called.
   */
  public String sp_start(String sql) throws NoSuchMethodException {
    return this._sp_start(this.getPointer(),sql);
  }

  /**
   * Native method which gets the next result row and places in the
   * referenced NsSet object.
   *
   * @param handle the database handle.
   * @param setptr reference to NsSet object obtained by a previous select.
   * @exception SQLException if a database access error occurred
   */
  protected native String _getrow(String handle, String setptr) 
    throws SQLException;

  /**
   * Method which gets the next result row and places in the
   * referenced NsSet object.
   *
   * @param set NsSet object obtained by a previous select.
   * @exception SQLException if a database access error occurred
   */
  public boolean getrow(NsSet set) throws SQLException {
      if(this._getrow(this.getPointer(),set.getPointer()).equals("1")) {
          return true;
      }
      else {
          return false;
      }
  }

  /**
   * Native method which sets verbose mode to on/off.
   *
   * @param handle the database handle.
   * @param on_off verbose mode to use.
   * @return the selected mode.
   */
  protected native String _verbose(String handle, String on_off);

  /**
   * Method which sets verbose mode to on/off.
   *
   * @param state verbose mode to use.
   * @return the selected mode.
   */
  public boolean verbose(boolean state) {
      String s;

      if(state == true) {
          s = "on";
      }
      else {
          s = "off";
      }
      if(this._verbose(this.getPointer(),s).equals("1")) {
          return true;
      }
      else {
          return false;
      }
  }

  /**
   * Method which gets the current verbose mode.
   *
   * @return the selected mode.
   */
  public boolean verbose() {
      if(this._verbose(this.getPointer(),null).equals("1")) {
          return true;
      }
      else {
          return false;
      }
  }

  /**
   * Native method which is not yet implemented.
   *
   * @param handle the database handle.
   * @param jsql sql for select statement.
   * @exception NoSuchMethodException if this method is called.
   */
  protected native String _setexception(String handle, String jsql) 
    throws NoSuchMethodException;

  /**
   * Method which is not yet implemented.
   *
   * @param sql sql for select statement.
   * @exception NoSuchMethodException if this method is called.
   */
  public String setexception(String sql) throws NoSuchMethodException {
    return this._setexception(this.getPointer(),sql);
  }

  /**
   * Native method which is not yet implemented.
   *
   * @param handle the database handle.
   * @param jsql sql for select statement.
   * @exception NoSuchMethodException if this method is called.
   */
  protected native String _sp_setparam(String handle, String jsql) 
    throws NoSuchMethodException;

  /**
   * Method which is not yet implemented.
   *
   * @param sql sql for select statement.
   * @exception NoSuchMethodException if this method is called.
   */
  public String sp_setparam(String sql) throws NoSuchMethodException {
    return this._sp_setparam(this.getPointer(),sql);
  }
  static {
    System.loadLibrary("tclblend");
  }  
}
