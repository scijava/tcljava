/*
 * LsetCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 * Copyright (c) 1998-1999 by Scriptics Corporation.
 * Copyright (c) 2000 Christian Krone.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LsetCmd.java,v 1.2 2009/07/07 10:54:03 rszulgo Exp $
 *
 */

package tcl.lang;

import java.awt.image.IndexColorModel;

/*
 * This class implements the built-in "lset" command in Tcl.
 */

class LsetCmd implements Command {

	/*
	 * --------------------------------------------------------------------------
	 * ---
	 * 
	 * cmdProc --
	 * 
	 * This procedure is invoked to process the "lset" Tcl command. See the user
	 * documentation for details on what it does.
	 * 
	 * Results: None.
	 * 
	 * Side effects: See the user documentation.
	 * --------------------------------
	 * ------------------------------------------ ---
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject[] objv) // Arguments to "lset" command.
			throws TclException {
		
		TclObject list; /* the list being altered. */
		TclObject finalValue; /* Value finally assigned to the variable. */

		/*
		 * Check parameter count.
		 */

		if (objv.length < 3) {
			throw new TclNumArgsException(interp, 1, objv,
					"listVar index ?index ...? value");
		}

		/*
		 * Look up the list variable's value.
		 */
		list = Var.getVar(interp, objv[1].toString(), null, TCL.LEAVE_ERR_MSG);
		if (list == null) {
			throw new TclException(interp, "Cannot find variable name "
					+ objv[1].toString());
		}

		/*
		 * Substitute the value in the value. Return either the value or else an
		 * unshared copy of it.
		 */

		if (objv.length == 4) {
			finalValue = list(interp, list, objv[2], objv[3]);
		} else {
			TclObject[] indices = new TclObject[objv.length - 3];
			// check indices count
			if (objv.length > 4) {
				// many indices
				for (int i=0; i < objv.length - 3; i++) {
					indices[i] = objv[i+2];
				}
			}
			finalValue = flat(interp, list, objv.length - 3, indices,
					objv[objv.length - 1]);
		}

		/*
		 * If substitution has failed, bail out.
		 */

		if (finalValue == null) {
			throw new TclException(interp, "Substitution has failed!");
		}

		/*
		 * Finally, update the variable so that traces fire.
		 */

		list = Var.setVar(interp, objv[1].toString(), (String) null,
				finalValue, TCL.LEAVE_ERR_MSG);
		
		finalValue.release();
		if (list == null) {
			throw new TclException(interp, "Cannot update variable");
		}

		/*
		 * Return the new value of the variable as the interpreter result.
		 */

		interp.setResult(list);
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * flat (TclLsetFlat) --
	 * 
	 * Core engine of the 'lset' command.
	 * 
	 * Results: Returns the new value of the list variable, or NULL if an error
	 * occurred. The returned object includes one reference count for the
	 * pointer returned.
	 * 
	 * Side effects:
	 * 
	 * Surgery is performed on the unshared list value to produce the result.
	 * flat (TclLsetFlat) maintains a linked list of Tcl_Obj's whose string
	 * representations must be spoilt by threading via 'ptr2' of the two-pointer
	 * internal representation. On entry to flat (TclLsetFlat), the values of
	 * 'ptr2' are immaterial; on exit, the 'ptr2' field of any TclObject that
	 * has been modified is set to null.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private static final TclObject flat(
			Interp interp,		 /* Tcl interpreter. */
			TclObject list, 	 /* The list being modified. */
			int indexCount,		 /* Number of index args. */
			TclObject[] indexArray,/* Index args. */
			TclObject value) 	 /* Value arg to 'lset'. */
		throws TclException {
		
	    int duplicated;		/* Flag == 1 if the obj has been duplicated, 0 otherwise */
	    TclObject retValue;	/* The list to be returned */
	    int elemCount;		/* Length of one sublist being changed */
	    TclObject[] elems;	/* The elements of a sublist */
	    TclObject subList;	/* The current sublist */
	    int result = 0;
	    int index = 0;			/* Index of the element to replace in the current sublist */
	    TclObject chain;	/* The enclosing list of the current sublist. */
	    TclObject ptr2;
	    
	    /*
	     * If there are no indices, then simply return the new value,
	     * counting the returned pointer as a reference
	     */

	    if ( indexCount == 0 ) {
	    	value.preserve();
	    	return value;
	    }
	    
	    /*
	     * If the list is shared, make a private copy.
	     */

	    if ( list.isShared() ) {
	    	duplicated = 1;
	    	list = list.duplicate();
			list.preserve();
	    } else {
	    	duplicated = 0;
	    }
	    
	    /*
	     * Anchor the linked list of Tcl_Obj's whose string reps must be
	     * invalidated if the operation succeeds.
	     */

	    retValue = list;
	    chain = null;
	    
	    /*
	     * Handle each index arg by diving into the appropriate sublist
	     */

	    for (int i = 0; ; ++i ) {
			/*
			 * Take the sublist apart.
			 */
	    	try {
		    	elems = TclList.getElements(interp, list);
				elemCount = elems.length;
	    	} catch (TclException e) {
	    		break;
	    	}
	    	
	    	ptr2 = chain;
	    	
			/*
			 * Determine the index of the requested element.
			 */
	    	
	    	try {
	    		index = Util.getIntForIndex(interp, indexArray[i], (elemCount - 1));
			} catch (TclException e) {
				break;
			}
		
			/*
			 * Check that the index is in range.
			 */

			if (index < 0 || index >= elemCount) {
			    interp.setResult(TclString.newInstance("list index out of range"));
			    result = TCL.ERROR;
			    break;
			}

			/*
			 * Break the loop after extracting the innermost sublist
			 */
	
			if ( i >= indexCount-1 ) {
			    result = TCL.OK;
			    break;
			}
		
			/*
			 * Extract the appropriate sublist, and make sure that it is unshared.
			 */
	
			subList = elems[index];
			if (subList.isShared()) {
			    subList = subList.duplicate();
			    try {
			    	TclList.setElement(interp, list, index, subList);
			    } catch (TclException e) {
				    /* 
					 * We actually shouldn't be able to get here.
					 * If we do, it would result in leaking subListPtr,
					 * but everything's been validated already; the error
					 * exit from TclList.setElement should never happen.
					 */
					break;
			    }
			}

			/* 
			 * Chain the current sublist onto the linked list of Tcl_Obj's
			 * whose string reps must be spoilt.
			 */
	
			chain = list;
			list = subList;

	    }
	    
	    /* Store the result in the list element */

	    if (result == TCL.OK) {
	    	try {
	    		TclList.setElement(interp, list, index, value);
	    		ptr2 = chain;
	    		
	    		/* Spoil all the string reps */
	    		
	    		while (list != null) {
	    		    subList = ptr2;
	    		    list.invalidateStringRep();
	    		    ptr2 = null;
	    		    list = subList;
	    		}
	    		
	    		/* Return the new list if everything worked. */
	    		
	    		if (duplicated == 0) {
	    		    retValue.preserve();
	    		}
	    		
	    		return retValue;
	    	} catch (TclException e) {
	    		
	    	}
	    }

	    /* Clean up the one dangling reference otherwise */

	    if (duplicated != 0) {
	    	retValue.release();
	    }
	    
	    return null;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * list (TclLsetList) --
	 * 
	 * Core of the 'lset' command when objc == 4. Objv[2] may be either a scalar
	 * index or a list of indices.
	 * 
	 * Results: Returns the new value of the list variable, or NULL if there was
	 * an error. The returned object includes one reference count for the
	 * pointer returned.
	 * 
	 * Side effects: None.
	 * 
	 * Notes: This procedure is implemented entirely as a wrapper around flat
	 * (TclLsetFlat). All it does is reconfigure the argument format into the
	 * form required by flat (TclLsetFlat), while taking care to manage
	 * shimmering in such a way that we tend to keep the most useful intreps
	 * and/or avoid the most expensive conversions.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private static final TclObject list(Interp interp, /* Tcl interpreter. */
	TclObject list, /* the list being modified. */
	TclObject indexArg, /* Index or index-list arg to 'lset'. */
	TclObject value) /* Value arg to 'lset'. */
	throws TclException {

		int indexCount;		/* Number of indices in the index list */
	    TclObject[] indices;/* Vector of indices in the index list*/
	    int duplicated;		/* Flag == 1 if the obj has been duplicated, 0 otherwise */
	    TclObject retValue;	/* The list to be returned */
	    int index = 0;			/* Current index in the list - discarded */
	    int result = 0;		/* Status return from library calls */
	    TclObject subList;	/* The current sublist */
	    int elemCount;		/* Count of elements in the current sublist */
	    TclObject[] elems;	/* Elements of current sublist  */
	    TclObject chain;	/* The enclosing sublist of the current sublist */
	    int i;
	    TclObject ptr2;
	    /*
	     * Determine whether the index arg designates a list or a single
	     * index.  We have to be careful about the order of the checks to
	     * avoid repeated shimmering; see TIP #22 and #23 for details.
	     */

	    if (!indexArg.isListType()) {
	    	try {
	    		index = Util.getIntForIndex(null, indexArg, 0);
	    		return flat( interp, list, 1, TclList.getElements(interp, indexArg), value );
	    	} catch (TclException e) {
	    		try {
			    	indices = TclList.getElements(null, indexArg);
			    	indexCount = indices.length;
		    	} catch (TclException ex) {
		    		/*
		    		 * indexArgPtr designates something that is neither an index nor a
		    		 * well formed list.  Report the error via TclLsetFlat.
		    		 */
		    		return flat( interp, list, 1, TclList.getElements(interp, indexArg), value );
		    	}
	    	}
	    }

		/*
		 * indexArg designates a single index.
		 */
	    else {
	    	try {
		    	indices = TclList.getElements(null, indexArg);
		    	indexCount = indices.length;
	    	} catch (TclException e) {
	    		/*
	    		 * indexArgPtr designates something that is neither an index nor a
	    		 * well formed list.  Report the error via TclLsetFlat.
	    		 */
	    		return flat( interp, list, 1, TclList.getElements(interp, indexArg), value );
	    	}
	    }

	    /*
	     * At this point, we know that arg designates a well formed list,
	     * and the 'else if' above has parsed it into indexCount and indices.
	     * If there are no indices, simply return 'valuePtr', counting the
	     * returned pointer as a reference.
	     */

	    if (indexCount == 0) {
	    	value.preserve();
	    	return value;
	    }

	    /*
	     * Duplicate the list arg if necessary.
	     */

	    if (list.isShared()) {
	    	duplicated = 1;
	    	list = list.duplicate();
	    	list.preserve();
	    } else {
	    	duplicated = 0;
	    }

	    /*
	     * It would be tempting simply to go off to TclLsetFlat to finish the
	     * processing.  Alas, it is also incorrect!  The problem is that
	     * 'indexArgPtr' may designate a sublist of 'listPtr' whose value
	     * is to be manipulated.  The fact that 'listPtr' is itself unshared
	     * does not guarantee that no sublist is.  Therefore, it's necessary
	     * to replicate all the work here, expanding the index list on each
	     * trip through the loop.
	     */

	    /*
	     * Anchor the linked list of Tcl_Obj's whose string reps must be
	     * invalidated if the operation succeeds.
	     */

	    retValue = list;
	    chain = null;

	    /*
	     * Handle each index arg by diving into the appropriate sublist
	     */

	    for (i = 0; ; ++i ) {

			/*
			 * Take the sublist apart.
			 */
	    	try {
	    		elems = TclList.getElements(interp, list);
	    		elemCount = elems.length;
	    	} catch (TclException e) {
			    break;
			}
			
	    	ptr2 = chain;

			/*
			 * Reconstitute the index array
			 */

			try {
				indices = TclList.getElements(interp, indexArg);
				indexCount = indices.length;
			} catch (TclException e) {
			    /* 
			     * Shouldn't be able to get here, because we already
			     * parsed the thing successfully once.
			     */
			    break;
			}
			
			/*
			 * Determine the index of the requested element.
			 */
			try {
				index = Util.getIntForIndex(interp, indices[i], (elemCount - 1));
			} catch (TclException e) {
			    break;
			}
		
			/*
			 * Check that the index is in range.
			 */
	
			if (index < 0 || index >= elemCount) {
			    interp.setResult(TclString.newInstance("list index out of range"));
			    throw new TclException(interp, "list index out of range");
			}

			/*
			 * Break the loop after extracting the innermost sublist
			 */
			if ( i >= indexCount-1 ) {
			    result = TCL.OK;
			    break;
			}
		
			/*
			 * Extract the appropriate sublist, and make sure that it is unshared.
			 */
			subList = elems[index];
			if (subList.isShared()) {
			    subList = subList.duplicate();
			    try {
			    	TclList.setElement(interp, list, index, subList);
			    } catch (TclException e) {
					/* 
					 * We actually shouldn't be able to get here, because
					 * we've already checked everything that TclListObjSetElement
					 * checks. If we were to get here, it would result in leaking
					 * subListPtr.
					 */
					break;
			    }
			}

			/* 
			 * Chain the current sublist onto the linked list of Tcl_Obj's
			 * whose string reps must be spoilt.
			 */
			chain = list;
			list = subList;
	    }
	    
	    /*
	     * Store the new element into the correct slot in the innermost sublist.
	     */
	    if ( result == TCL.OK ) {
	    	try {
	    		TclList.setElement(interp, list, index, value);
	    		ptr2 = chain;
				
				/* Spoil all the string reps */
				while (list != null) {
				    subList = ptr2;
				    list.invalidateStringRep();
				    ptr2 = null;
				    list = subList;
				}

				/* Return the new list if everything worked. */
				
				if (duplicated == 0 ) {
				    retValue.preserve();
				}
				
				return retValue;
				
	    	} catch (TclException e) {
	    		
	    	}
	    }

	    /* Clean up the one dangling reference otherwise */

	    if (duplicated != 0) {
	    	retValue.release();
	    }
	    
	    return null;
	}
} // end LsearchCmd