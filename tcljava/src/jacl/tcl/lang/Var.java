/*
 * Var.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Var.java,v 1.1 1998/10/14 21:09:21 cvsadmin Exp $
 *
 */
package tcl.lang;

import java.util.*;

/**
 * Implements variables in Tcl.
 */
class Var {
    static final int ARRAY	  = 1;
    static final int UPVAR	  = 2;
    static final int UNDEFINED	  = 4;
    static final int TRACE_ACTIVE = 0x10;

    /**
     * Construct a variable and initialize its fields.
     */

    Var() {
	value    = null;
	traces   = null;
	flags    = UNDEFINED;
	table    = null;
	hashKey  = null;
	refCount = 0;
    }

    /**
     * Stores the "value" of the variable. It stored different information
     * depending on the type of the variable: <ul>
     *  <li>Scalar variable - (TclObject)value is the object stored in the
     *	   variable.
     *	<li> Array variable - (Hashtable)value is the hashtable that stores
     *     all the elements. <p>
     *  <li> Upvar - (Var)value is the variable associated by this upvar.
     * </ul>
     */

    Object value;

    Vector traces;

    Vector sidVec;

    /**
     * Miscellaneous bits of information about variable.
     *
     * @see Var#ARRAY
     * @see Var#UPVAR
     * @see Var#UNDEFINED
     * @see Var#TRACE_ACTIVE
     */       

    int flags;

    /**
     * The table that stores this variable.
     */

    Hashtable table;

    /**
     * The key under which this variable is stored int the hash table.
     */

    String hashKey;

    int refCount;

    /**
     * Used by ArrayCmd to create a unique searchId string.  If the 
     * sidVec Vector is empty then simply return 1.  Else return 1 
     * plus the SearchId.index value of the last Object in the vector.
     * 
     * @param None
     * @return The int value for unique SearchId string.
     */

    protected synchronized int getNextIndex() {
        if(sidVec.size() == 0) {
	    return 1;
	}
	SearchId sid = (SearchId)sidVec.lastElement();
	return (sid.getIndex()+1);
    }

    /**
     * Find the SearchId that in the sidVec Vector that is equal the 
     * unique String s and returns the enumeration associated with
     * that SearchId.
     *
     * @param s String that ia a unique identifier for a SearchId object
     * @return Enumeration if a match is found else null.
     */

    protected Enumeration getSearch(String s) {
        SearchId sid;
        for(int i=0; i<sidVec.size(); i++) {
	    sid = (SearchId)sidVec.elementAt(i);
	    if(sid.equals(s)){
	        return sid.getEnum();
	    }
	}
	return null;
     }
        

    /**
     * Find the SearchId object in the sidVec Vector and remove it.
     *
     * @param s String that ia a unique identifier for a SearchId object.
     */

    protected boolean removeSearch(String sid) {
        SearchId curSid;

        for(int i=0; i<sidVec.size(); i++) {
	    curSid = (SearchId)sidVec.elementAt(i);
	    if(curSid.equals(sid)){
	        sidVec.removeElementAt(i);
		return true;
	    }
	} 
	return false;
    }
}
