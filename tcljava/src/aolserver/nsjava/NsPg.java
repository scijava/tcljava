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
 * <P>NsPg extends the basic NsDb class to provide postgresql specific driver
 * functionality.
 *
 * @author Daniel Wickstrom
 * @see NsDb
 * @see NsLog
 * @see NsSet
 * @see NsTcl
 */
public class NsPg extends NsDb {

  /**
   * Create a postgresql specific database handle.
   *
   * @return instance of a postgresql database handle.
   * @exception SQLException for any database errors.
   */
  public NsPg() throws SQLException {
    super();
  }

  /**
   * Create a postgresql specific database handle.
   *
   * @param poolname the poolname to use when getting the handle.
   * @return instance of a postgresql database handle.
   * @exception SQLException for any database errors.
   */
  public NsPg(String poolname) throws SQLException {
    super(poolname);
  }

  /**
   * Cleanup and release database handle.
   *
   * @exception SQLException for any database errors.
   */
  public void finalize() throws SQLException {
    super.finalize();
  }

  /**
   * Not sure what this is.  Need to check on this.
   *
   * @param handlePtr database handle.
   * @return driver db result.
   * @exception SQLException for any database errors.
   */
  protected native String _db(String handlePtr) throws SQLException;

  /**
   * Not sure what this is.  Need to check on this.
   *
   * @return driver db command result.
   * @exception SQLException for any database errors.
   */
  public String db() throws SQLException {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    return this._db(this.getPointer());
  }

  /**
   * Return the host for the postgresql driver.
   *
   * @param handlePtr database handle.
   * @return driver host command result.
   * @exception SQLException for any database errors.
   */
  protected native String _host(String handlePtr) throws SQLException;

  /**
   * Return the host for the postgresql driver.
   *
   * @return driver host command result.
   * @exception SQLException for any database errors.
   */
  public String host() throws SQLException {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    return this._host(this.getPointer());
  }

  /**
   * Return the options for the postgresql driver.
   *
   * @param handlePtr the database handle.
   * @return driver options command result.
   * @exception SQLException for any database errors.
   */
  protected native String _options(String handlePtr) throws SQLException;

  /**
   * Return the options for the postgresql driver.
   *
   * @return driver options command result.
   * @exception SQLException for any database errors.
   */
  public String options() throws SQLException {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    return this._options(this.getPointer());
  }

  /**
   * Return the port for the postgresql driver.
   *
   * @param handlePtr the database handle.
   * @return driver port command result.
   * @exception SQLException for any database errors.
   */
  protected native String _port(String handlePtr) throws SQLException;

  /**
   * Return the port for the postgresql driver.
   *
   * @return driver port command result.
   * @exception SQLException for any database errors.
   */
  public String port() throws SQLException {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    return this._port(this.getPointer());
  }

  /**
   * Return the number of connections for the postgresql driver.
   *
   * @param handlePtr the database handle.
   * @return driver number command result.
   * @exception SQLException for any database errors.
   */
  protected native String _number(String handlePtr) throws SQLException;

  /**
   * Return the number of connections for the postgresql driver.
   *
   * @return driver number command result.
   * @exception SQLException for any database errors.
   */
  public String number() throws SQLException {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    return this._number(this.getPointer());
  }

  /**
   * Return the last error for the postgresql driver.
   *
   * @param handlePtr the database handle.
   * @return driver error command result.
   * @exception SQLException for any database errors.
   */
  protected native String _error(String handlePtr) throws SQLException;

  /**
   * Return the last error for the postgresql driver.
   *
   * @return driver error command result.
   * @exception SQLException for any database errors.
   */
  public String error() throws SQLException {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    return this._error(this.getPointer());
  }

  /**
   * Return the status for the postgresql driver.
   *
   * @param handlePtr the database handle.
   * @return driver status command result.
   * @exception SQLException for any database errors.
   */
  protected native String _status(String handlePtr) throws SQLException;

  /**
   * Return the status for the postgresql driver.
   *
   * @return driver status command result.
   * @exception SQLException for any database errors.
   */
  public String status() throws SQLException {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    return this._status(this.getPointer());
  }

  /**
   * Return the number of tuples returned for the last command.
   *
   * @param handlePtr the database handle.
   * @return the number of tuples.
   * @exception SQLException for any database errors.
   */
  protected native String _ntuples(String handlePtr) throws SQLException;

  /**
   * Return the number of tuples returned for the last command.
   *
   * @return the number of tuples.
   * @exception SQLException for any database errors.
   */
  public Integer ntuples() throws SQLException {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    return new Integer(this._ntuples(this.getPointer()));
  }

  /**
   * Insert into a large object using a file as a datasource.
   *
   * @param handlePtr the database handle.
   * @param blob_id for the blob that will receive the insert or update.
   * @param filename to read binary data from.
   * @exception SQLException for any database errors.
   */
  protected native void _blob_dml_file(String handlePtr, String blob_id, 
                                       String filename) throws SQLException;

  /**
   * Insert into a large object using a file as a datasource.
   *
   * @param blob_id for the blob that will receive the insert or update.
   * @param filename to read binary data from.
   * @exception SQLException for any database errors.
   */
  public void blob_dml_file(String blob_id, String filename) 
    throws SQLException 
  {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    this._blob_dml_file(this.getPointer(),blob_id,filename);
  }

  /**
   * Insert into a large object using a file as a datasource.
   *
   * @param blob_id for the blob that will receive the insert or update.
   * @param filename to read binary data from.
   * @exception SQLException for any database errors.
   */
  public void blob_dml_file(Integer blob_id, String filename) 
    throws SQLException {
      this.blob_dml_file(blob_id.toString(),filename);
  }

  /**
   * Insert into a large object using a file as a datasource.
   *
   * @param blob_id for the blob that will receive the insert or update.
   * @param filename to read binary data from.
   * @exception SQLException for any database errors.
   */
  public void blob_dml_file(int blob_id, String filename) throws SQLException {
      Integer id = new Integer(blob_id);
      this.blob_dml_file(id.toString(),filename);
  }

  /**
   * Select a large object into a file.
   *
   * @param handlePtr the database handle.
   * @param blob_id for the blob that will be selected from.
   * @param filename to write binary data into.
   * @exception SQLException for any database errors.
   */
  protected native void _blob_select_file(String handlePtr, String blob_id, 
                                          String filename) throws SQLException;

  /**
   * Select a large object into a file.
   *
   * @param blob_id for the blob that will be selected from.
   * @param filename to write binary data into.
   * @exception SQLException for any database errors.
   */
  public void blob_select_file(String blob_id, String filename) 
    throws SQLException 
  {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    this._blob_select_file(this.getPointer(),blob_id,filename);
  }

  /**
   * Select a large object into a file.
   *
   * @param blob_id for the blob that will be selected from.
   * @param filename to write binary data into.
   * @exception SQLException for any database errors.
   */
  public void blob_select_file(Integer blob_id, String filename) 
    throws SQLException {
      this.blob_select_file(blob_id.toString(), filename);
  }

  /**
   * Select a large object into a file.
   *
   * @param blob_id for the blob that will be selected from.
   * @param filename to write binary data into.
   * @exception SQLException for any database errors.
   */
  public void blob_select_file(int blob_id, String filename) 
    throws SQLException {
      Integer id = new Integer(blob_id);
      this.blob_select_file(id.toString(),filename);
  }

  /**
   * Implementation of the ns_column command.
   *
   * @param handlePtr the database handle.
   * @param cmd column command to perform.
   * @param tbl to reference for column table command
   * @param col column name/index
   * @param ky key value
   * @return the requested command results.
   * @exception SQLException for any database errors.
   */
  protected native String _ns_column(String handlePtr, String cmd, 
                                     String tbl, String col, String ky)
                                     throws SQLException;

  /**
   * Implementation of the ns_column command.
   *
   * @param command column command to perform.
   * @param table to reference for column table command
   * @return the requested command results.
   * @exception SQLException for any database errors.
   */
  public String column(String command, String table) 
    throws SQLException 
    {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    return this._ns_column(this.getPointer(),command,table,null,null);
  }

  /**
   * Implementation of the ns_column command.
   *
   * @param command column command to perform.
   * @param table to reference for column table command
   * @param col column name/index
   * @return the requested command results.
   * @exception SQLException for any database errors.
   */
  public String column(String command, String table, String col) 
    throws SQLException {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    return this._ns_column(this.getPointer(),command,table,col,null);
  }

  /**
   * Implementation of the ns_column command.
   *
   * @param command column command to perform.
   * @param table to reference for column table command
   * @param col column name/index
   * @param key key value
   * @return the requested command results.
   * @exception SQLException for any database errors.
   */
  public String column(String command, String table, String col, String key) 
    throws SQLException {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    return this._ns_column(this.getPointer(), command, table, col, key);
  }

  /**
   * Implementation of the ns_table command.
   *
   * @param handlePtr the database handle.
   * @param cmd table command to perform.
   * @param tbl to reference for the table command
   * @param ky key value
   * @return the requested command results.
   * @exception SQLException for any database errors.
   */
  protected native String _ns_table(String handlePtr, String cmd ,
                                    String tbl, String ky) 
      throws SQLException;

  /**
   * Implementation of the ns_table command.
   *
   * @param command table command to perform.
   * @return the requested command results.
   * @exception SQLException for any database errors.
   */
  public String table(String command) throws SQLException {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    return this._ns_table(this.getPointer(), command, null, null);
  }

  /**
   * Implementation of the ns_table command.
   *
   * @param command table command to perform.
   * @param table to reference for the table command
   * @return the requested command results.
   * @exception SQLException for any database errors.
   */
  public String table(String command, String table) throws SQLException {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    return this._ns_table(this.getPointer(), command, table, null);
  }

  /**
   * Implementation of the ns_table command.
   *
   * @param command column command to perform.
   * @param table to reference for column table command
   * @param key key value
   * @return the requested command results.
   * @exception SQLException for any database errors.
   */
  public String table(String command, String table, String key) 
    throws SQLException {
    if(this.handle.equals(null)) {
      this.handle = this.gethandle(this.poolname);
    }
    return this._ns_table(this.getPointer(), command, table, key);
  }

  static {
    System.loadLibrary("tclblend");
  }  
}
