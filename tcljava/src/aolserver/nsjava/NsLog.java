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

/**
 * <P>NsLog provides a java wrapper around the aolserver Ns_ModLog command. 
 *
 * @author Daniel Wickstrom
 * @see NsDb
 * @see NsPg
 * @see NsSet
 * @see NsTcl
 */
public class NsLog {


  /**
   * Write a message to the aolserver log file with an associated severity 
   * level.
   *
   * @param severity can be one of Notice, Warning, Error, Fatal, Bug, 
   * or Debug.
   * @param message message to output to the log file.
   */
    static public native void write(String severity, String message);
  
    static {
        System.loadLibrary("tclblend");
    }  
}
