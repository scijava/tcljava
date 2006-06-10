/*
 * TclList.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclList.java,v 1.15 2006/06/10 04:15:59 mdejong Exp $
 *
 */

package tcl.lang;

import java.util.ArrayList;

/**
 * This class implements the list object type in Tcl.
 */
public class TclList implements InternalRep {

    /**
     * Internal representation of a list value.
     */
    private ArrayList alist;

    /**
     * Create a new empty Tcl List.
     */
    private TclList() {
	alist = new ArrayList();

	if (TclObject.saveObjRecords) {
	    String key = "TclList";
	    Integer num = (Integer) TclObject.objRecordMap.get(key);
	    if (num == null) {
	        num = new Integer(1);
	    } else {
	        num = new Integer(num.intValue() + 1);
	    }
	    TclObject.objRecordMap.put(key, num);
	}
    }

    /**
     * Create a new empty Tcl List, with the list pre-allocated to
     * the given size.
     *
     * @param size the number of slots pre-allocated in the alist.
     */
    private TclList(int size) {
	alist = new ArrayList(size);

	if (TclObject.saveObjRecords) {
	    String key = "TclList";
	    Integer num = (Integer) TclObject.objRecordMap.get(key);
	    if (num == null) {
	        num = new Integer(1);
	    } else {
	        num = new Integer(num.intValue() + 1);
	    }
	    TclObject.objRecordMap.put(key, num);
	}
    }

    /**
     * Called to free any storage for the type's internal rep.
     */
    public void dispose() {
	final int size = alist.size();
	for (int i=0; i<size; i++) {
	    ((TclObject) alist.get(i)).release();
	}
	alist.clear();
    }

    /**
     * DupListInternalRep -> duplicate
     *
     * Returns a dupilcate of the current object.
     *
     */
    public InternalRep duplicate() {
	int size = alist.size();
	TclList newList = new TclList(size);

	for (int i=0; i<size; i++) {
	    TclObject tobj = (TclObject) alist.get(i);
	    tobj.preserve();
	    newList.alist.add(tobj);
	}

	if (TclObject.saveObjRecords) {
	    String key = "TclList.duplicate()";
	    Integer num = (Integer) TclObject.objRecordMap.get(key);
	    if (num == null) {
	        num = new Integer(1);
	    } else {
	        num = new Integer(num.intValue() + 1);
	    }
	    TclObject.objRecordMap.put(key, num);
	}

	return newList;
    }

    /**
     * Called to query the string representation of the Tcl object. This
     * method is called only by TclObject.toString() when
     * TclObject.stringRep is null.
     *
     * @return the string representation of the Tcl object.
     */
    public String toString() {
	final int size = alist.size();
	if (size == 0) {
	    return "";
	}
	int est = size * 4;

	StringBuffer sbuf = new StringBuffer((est > 64) ? est : 64);
	try {
	    for (int i=0; i<size; i++) {
		Object elm = alist.get(i);
		if (elm != null) {
		    Util.appendElement(null, sbuf, elm.toString());
		} else {
		    Util.appendElement(null, sbuf, "");
		}
	    }
	} catch (TclException e) {
	    throw new TclRuntimeError("unexpected TclException: " + e);
	}

	return sbuf.toString();
    }

    /**
     * Creates a new instance of a TclObject with a TclList internal
     * rep.
     *
     * @return the TclObject with the given list value.
     */

    public static TclObject newInstance() {
	return new TclObject(new TclList());
    }

    /**
     * Called to convert the other object's internal rep to list.
     *
     * @param interp current interpreter.
     * @param tobj the TclObject to convert to use the List internal rep.
     * @exception TclException if the object doesn't contain a valid list.
     */
    private
    static void setListFromAny(Interp interp, TclObject tobj)
	    throws TclException {
	TclList tlist = new TclList();
	splitList(interp, tlist.alist, tobj.toString());
	tobj.setInternalRep(tlist);

	if (TclObject.saveObjRecords) {
	    String key = "TclString -> TclList";
	    Integer num = (Integer) TclObject.objRecordMap.get(key);
	    if (num == null) {
	        num = new Integer(1);
	    } else {
	        num = new Integer(num.intValue() + 1);
	    }
	    TclObject.objRecordMap.put(key, num);
	}
    }

    /**
     * Splits a list (in string rep) up into its constituent fields.
     *
     * @param interp current interpreter.
     * @param alist store the list elements in this ArraryList.
     * @param s the string to convert into a list.
     * @exception TclException if the object doesn't contain a valid list.
     */
    private static final void splitList(Interp interp, ArrayList alist, String s)
	    throws TclException {
	int len = s.length();
	int i = 0;
	FindElemResult res = new FindElemResult();

	while (i < len) {
	    if (!Util.findElement(interp, s, i, len, res)) {
		break;
	    } else {
		TclObject tobj = TclString.newInstance(res.elem);
		tobj.preserve();
		alist.add(tobj);
	    }
	    i = res.elemEnd;
	}
    }


    /**
     * Tcl_ListObjAppendElement -> TclList.append()
     * 
     * Appends a TclObject element to a list object.
     *
     * @param interp current interpreter.
     * @param tobj the TclObject to append an element to.
     * @param elemObj the element to append to the object.
     * @exception TclException if tobj cannot be converted into a list.
     */
    public static final void append(Interp interp, TclObject tobj,
	    TclObject elemObj)
	    throws TclException {
	if (tobj.isShared()) {
	    throw new TclRuntimeError("TclList.append() called with shared object");
	}
	if (! tobj.isListType()) {
	    setListFromAny(interp, tobj);
	}
	tobj.invalidateStringRep();

	elemObj.preserve();
	((TclList) tobj.getInternalRep()).alist.add(elemObj);
    }

    /**
     * TclList.append()
     *
     * Appends multiple TclObject elements to a list object.
     *
     * @param interp current interpreter.
     * @param tobj the TclObject to append elements to.
     * @param objv array containing elements to append.
     * @param startIdx index to start appending values from
     * @param endIdx index to stop appending values at
     * @exception TclException if tobj cannot be converted into a list.
     */
    static final void append(Interp interp, TclObject tobj,
	    TclObject[] objv,
            final int startIdx, final int endIdx)
            throws TclException {
	if (tobj.isShared()) {
	    throw new TclRuntimeError("TclList.append() called with shared object");
	}
	if (! tobj.isListType()) {
	    setListFromAny(interp, tobj);
	}
	tobj.invalidateStringRep();

	ArrayList alist = ((TclList) tobj.getInternalRep()).alist;

	for (int i = startIdx ; i < endIdx ; i++) {
	    TclObject elemObj = objv[i];
	    elemObj.preserve();
	    alist.add(elemObj);
	}
    }

    /**
     * Queries the length of the list. If tobj is not a list object,
     * an attempt will be made to convert it to a list.
     *
     * @param interp current interpreter.
     * @param tobj the TclObject to use as a list.
     * @return the length of the list.
     * @exception TclException if tobj is not a valid list.
     */
    public static final int getLength(Interp interp, TclObject tobj)
	    throws TclException {
	if (! tobj.isListType()) {
	    setListFromAny(interp, tobj);
	}

	TclList tlist = (TclList) tobj.getInternalRep();
	return tlist.alist.size();
    }

    /**
     * Returns a TclObject array of the elements in a list object.  If
     * tobj is not a list object, an attempt will be made to convert
     * it to a list. <p>
     *
     * The objects referenced by the returned array should be treated
     * as readonly and their ref counts are _not_ incremented; the
     * caller must do that if it holds on to a reference.
     *
     * @param interp the current interpreter.
     * @param tobj the list to sort.
     * @return a TclObject array of the elements in a list object.
     * @exception TclException if tobj is not a valid list.
     */
    public static TclObject[] getElements(Interp interp, TclObject tobj)
	    throws TclException {
	if (! tobj.isListType()) {
	    setListFromAny(interp, tobj);
	}
	TclList tlist = (TclList) tobj.getInternalRep();

	int size = tlist.alist.size();
	TclObject objArray[] = new TclObject[size];
	for (int i=0; i<size; i++) {
	    objArray[i] = (TclObject) tlist.alist.get(i);
	}
	return objArray;
    }

    /**
     * This procedure returns a pointer to the index'th object from
     * the list referenced by tobj. The first element has index
     * 0. If index is negative or greater than or equal to the number
     * of elements in the list, a null is returned. If tobj is not a
     * list object, an attempt will be made to convert it to a list.
     *
     * @param interp current interpreter.
     * @param tobj the TclObject to use as a list.
     * @param index the index of the requested element.
     * @return the the requested element.
     * @exception TclException if tobj is not a valid list.
     */
    public static final TclObject index(Interp interp, TclObject tobj,
	    int index) throws TclException {
	if (! tobj.isListType()) {
	    setListFromAny(interp, tobj);
	}

	TclList tlist = (TclList) tobj.getInternalRep();
	if (index < 0 || index >= tlist.alist.size()) {
	    return null;
	} else {
	    return (TclObject) tlist.alist.get(index);
	}
    }

    /**
     * This procedure inserts the elements in elements[] into the list at
     * the given index. If tobj is not a list object, an attempt will
     * be made to convert it to a list.
     *
     * @param interp current interpreter.
     * @param tobj the TclObject to use as a list.
     * @param index the starting index of the insertion operation. <=0 means
     *   the beginning of the list. >= TclList.getLength(tobj) means
     *   the end of the list.
     * @param elements the element(s) to insert.
     * @param from insert elements starting from elements[from] (inclusive)
     * @param to insert elements up to elements[to] (inclusive)
     * @exception TclException if tobj is not a valid list.
     */
    static final void insert(Interp interp, TclObject tobj, int index,
	    TclObject elements[], int from, int to)
	    throws TclException {
	if (tobj.isShared()) {
	    throw new TclRuntimeError("TclList.insert() called with shared object");
	}
	replace(interp, tobj, index, 0, elements, from, to);
    }

    /**
     * This procedure replaces zero or more elements of the list
     * referenced by tobj with the objects from an TclObject array.
     * If tobj is not a list object, an attempt will be made to
     * convert it to a list.
     *
     * @param interp current interpreter.
     * @param tobj the TclObject to use as a list.
     * @param index the starting index of the replace operation. <=0 means
     *   the beginning of the list. >= TclList.getLength(tobj) means
     *   the end of the list.
     * @param count the number of elements to delete from the list. <=0 means
     *   no elements should be deleted and the operation is equivalent to
     *   an insertion operation.
     * @param elements the element(s) to insert.
     * @param from insert elements starting from elements[from] (inclusive)
     * @param to insert elements up to elements[to] (inclusive)
     * @exception TclException if tobj is not a valid list.
     */
    public static final void replace(Interp interp, TclObject tobj, int index,
	    int count, TclObject elements[], int from, int to)
	    throws TclException {
	if (tobj.isShared()) {
	    throw new TclRuntimeError("TclList.replace() called with shared object");
	}
	if (! tobj.isListType()) {
	    setListFromAny(interp, tobj);
	}
	tobj.invalidateStringRep();
	TclList tlist = (TclList) tobj.getInternalRep();

	int size = tlist.alist.size();
	int i;

	if (index >= size) {
	    // Append to the end of the list. There is no need for deleting
	    // elements.
	    index = size;
	} else {
	    if (index < 0) {
		index = 0;
	    }
	    if (count > size - index) {
		count = size - index;
	    }
	    for (i=0; i<count; i++) {
		TclObject obj = (TclObject) tlist.alist.get(index);
		obj.release();
		tlist.alist.remove(index);
	    }
	}
	for (i=from; i<=to; i++) {
	    elements[i].preserve();
	    tlist.alist.add(index++, elements[i]);
	}
    }

    /**
     * Sorts the list according to the sort mode and (optional) sort command.
     * If tobj is not a list object, an attempt will be made to
     * convert it to a list.
     *
     * @param interp the current interpreter.
     * @param tobj the list to sort.
     * @param sortMode the sorting mode.
     * @param sortIncreasing true if to sort the elements in increasing order.
     * @param command the command to compute the order of two elements.
     * @exception TclException if tobj is not a valid list.
     */

    static void sort(Interp interp, TclObject tobj, int sortMode,
	    int sortIndex, boolean sortIncreasing, String command)
	    throws TclException {
	if (! tobj.isListType()) {
	    setListFromAny(interp, tobj);
	}
	tobj.invalidateStringRep();
	TclList tlist = (TclList) tobj.getInternalRep();

	int size = tlist.alist.size();

	if (size <= 1) {
	    return;
	}

	TclObject objArray[] = new TclObject[size];
	for (int i=0; i<size; i++) {
	    objArray[i] = (TclObject) tlist.alist.get(i);
	}

	QSort s = new QSort();
	s.sort(interp, objArray, sortMode, sortIndex, sortIncreasing, command);

	for (int i=0; i<size; i++) {
	    tlist.alist.set(i, objArray[i]);
	    objArray[i] = null;
	}
    }
}
