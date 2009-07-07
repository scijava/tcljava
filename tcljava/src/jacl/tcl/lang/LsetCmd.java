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
 * RCS: @(#) $Id: LsetCmd.java,v 1.1 2009/07/07 09:15:48 rszulgo Exp $
 *
 */

package tcl.lang;

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
		
		int index = 0;
		int result; 
		int len;
		int currentIdx = 0;
	    TclObject subList;
	    TclObject retValue;
	    TclObject chain;

	    /*
	     * If there are no indices, simply return the new value.
	     * (Without indices, [lset] is a synonym for [set].
	     */

	    if (indexCount == 0) {
	    	return value;
	    }
	    
	    /*
	     * If the list is shared, make a copy we can modify (copy-on-write).
	     * We use Tcl_DuplicateObj() instead of TclListObjCopy() for a few
	     * reasons: 
	     * 1) we have not yet confirmed listPtr is actually a list;
	     * 2) We make a verbatim copy of any existing string rep, and when
	     * we combine that with the delayed invalidation of string reps of
	     * modified Tcl_Obj's implemented below, the outcome is that any
	     * error condition that causes this routine to return NULL, will
	     * leave the string rep of listPtr and all elements to be unchanged.
	     */

	    subList = list.isShared() ? list.duplicate() : list;

	    /*
	     * Anchor the linked list of Tcl_Obj's whose string reps must be
	     * invalidated if the operation succeeds.
	     */

	    retValue = subList;
	    chain = null;
	    
	    /*
	     * Loop through all the index arguments, and for each one dive
	     * into the appropriate sublist.
	     */

	    do {
			TclObject parentList;
			TclObject[] elems;
			
			
			/* Check for the possible error conditions... */
			result = TCL.ERROR;
			elems = TclList.getElements(interp, subList);
			if (elems == null) {
			    /* ...the sublist we're indexing into isn't a list at all. */
			    break;
			}
	
			try {
				index = Util.getIntForIndex(interp, indexArray[currentIdx], elems.length - 1);
			} catch (TclException e) {
				currentIdx++;
			    /* ...the index we're trying to use isn't an index at all. */
				break;
			}
			
			currentIdx++;
			
			if (index < 0 || index > elems.length) {
			    /* ...the index points outside the sublist. */
			    interp.setResult(TclString.newInstance("list index out of range"));
			    break;
			}

			/*
			 * No error conditions.  As long as we're not yet on the last
			 * index, determine the next sublist for the next pass through
			 * the loop, and take steps to make sure it is an unshared copy,
			 * as we intend to modify it.
			 */

			result = TCL.OK;
			if (--indexCount != 0) {
			    parentList = subList;
			    
			    if (index == elems.length) {
			    	subList = TclList.newInstance();
			    } else {
			    	subList = elems[index];
			    }
			    
			    if (subList.isShared()) {
			    	subList = subList.duplicate();
			    }

			    /*
			     * Replace the original elemPtr[index] in parentList with a copy
			     * we know to be unshared.  This call will also deal with the
			     * situation where parentList shares its intrep with other
			     * TclObject's.  Dealing with the shared intrep case can cause
			     * subListPtr to become shared again, so detect that case and
			     * make and store another copy.
			     */

			    if (index == elems.length) {
			    	TclList.append(null, parentList, subList);
			    } else {
			    	TclList.setElement(null, parentList, index, subList);
			    }
			    
			    if (subList.isShared()) {
					subList = subList.duplicate();
					TclList.setElement(null, parentList, index, subList);
			    }

			    /*
			     * The TclListObjSetElement() calls do not spoil the string
			     * rep of parentList, and that's fine for now, since all we've
			     * done so far is replace a list element with an unshared copy.
			     * The list value remains the same, so the string rep. is still
			     * valid, and unchanged, which is good because if this whole
			     * routine returns NULL, we'd like to leave no change to the
			     * value of the lset variable.  Later on, when we set valuePtr
			     * in its proper place, then all containing lists will have
			     * their values changed, and will need their string reps spoiled.
			     * We maintain a list of all those Tcl_Obj's (via a little intrep
			     * surgery) so we can spoil them at that time.
			     */
			    
			    if (chain != null) {
			    	parentList.internalRep = chain.internalRep;
			    } 
			    chain = parentList;
			}
		    } while (indexCount > 0);
	    
	    /*
	     * Either we've detected and error condition, and exited the loop
	     * with result == TCL_ERROR, or we've successfully reached the last
	     * index, and we're ready to store valuePtr.  In either case, we
	     * need to clean up our string spoiling list of Tcl_Obj's.
	     */

	    
	  //  while (chain != null) {
	    	
	    	chain.disposeObject();
//	    	TclObject obj = chain;

//	    	if (result == TCL.OK) {
			    /*
			     * We're going to store valuePtr, so spoil string reps
			     * of all containing lists.
			     */
	
//			    obj.invalidateStringRep();
//	    	}

	    	/* Clear away our intrep surgery mess */
//	    	chain.internalRep = obj.internalRep;
//	    	obj.internalRep = null;
	 //   }
	    
	    
	    if (result != TCL.OK) {
			/*
			 * Error return; message is already in interp. Clean up
			 * any excess memory.
			 */
	    	if (retValue != list) {
	    		retValue.release();
	    	}
	    	
	    	return null;
	    }

	    //subList.release();

	    /* Store valuePtr in proper sublist and return */
	    len = TclList.getLength(null, subList);
	    
	    if (index == len) {
	    	TclList.append(null, subList, value);
	    } else {
	    	TclList.setElement(null, subList, index, value);
	    }

	    retValue = subList;
	    subList.invalidateStringRep();
	    retValue.preserve();
	    
	    return retValue;
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

		TclObject[] indices = null;/* Vector of indices in the index list. */
		TclObject retValue; /* The list to be returned. */
		TclObject indexListCopy;

		/*
		 * Determine whether the index arg designates a list or a single index.
		 * We have to be careful about the order of the checks to avoid repeated
		 * shimmering; see TIP #22 and #23 for details.
		 */

		if (!indexArg.isListType()) {
			/*
			 * indexArg designates a single index.
			 */
			try {
				Util.getIntForIndex(null, indexArg, 0);
				return flat(interp, list, 1, TclList.getElements(interp, indexArg), value);
			} catch (TclException e) {
				/* falls through */
			}
		}

		indexListCopy = TclList.copy(interp, indexArg);

		if (indexListCopy == null) {
			/*
			 * indexArg designates something that is neither an index nor a
			 * well formed list. Report the error via TclLsetFlat.
			 */

			
			return flat(interp, list, 1, TclList.getElements(interp, indexArg), value);
		}

		indices = TclList.getElements(null, indexArg);

		/*
		 * Let TclLsetFlat handle the actual lset'ting.
		 */

		retValue = flat(interp, list, indices.length, indices, value);

		return retValue;
	}
} // end LsearchCmd
