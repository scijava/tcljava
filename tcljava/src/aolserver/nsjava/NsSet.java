/*
 * Copyright (c) 2000 Daniel Wickstrom
 *
 * See also http://www.aolserver.com for details on the AOLserver
 * web server software.
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
 * <P>NsSet provides a wrapper around the aolserver ns_set structures.
 *
 * @author Daniel Wickstrom
 * @see NsDb
 * @see NsLog
 * @see NsPg
 * @see NsTcl
 */
public class NsSet {
  protected String pointer;

  /**
   * Create and instance of an NsSet object.
   *
   * @return an NsSet instance.
   */
  public NsSet() { 
    this.pointer = null;
  }

  /**
   * Create and instance of an NsSet object.
   *
   * @param ptr reference to Ns_Set structure.
   * @return an NsSet instance.
   */
  public NsSet(String ptr) {
    this.pointer = ptr;
  }

  /**
   * Get the Ns_Set reference pointer
   *
   * @param ptr reference to Ns_Set structure.
   * @return an NsSet instance.
   */
  public String getPointer() {
    return this.pointer;
  }

  /**
   * Frees the NsSet object.
   *
   */
  public void finalize() {
      //this.free();
  }

  /**
   * Init the NsSet object.
   *
   * @param name of the NsSet structure.
   */
  protected native String _new(String name);

  /**
   * Init the NsSet object.
   *
   * @param name of the NsSet structure.
   */
  public void create(String name) {
    if(this.pointer != null) {
      this.free();
    }
    this.pointer = this._new(name);
  }

  /**
   * Create a new Ns_Set object.
   *
   * @param ptr to Ns_Set structure.
   * @return reference to new Ns_Set
   */
  protected native String _copy(String ptr);

  /**
   * Create a new Ns_Set object.
   *
   * @return new NsSet instance.
   */
  public NsSet copy() {
    return new NsSet(this._copy(this.getPointer()));
  }

  /**
   * split an NsSet object - not yet implemented.
   *
   * @return new NsSet instance.
   * @exception NoSuchMethodException for any calls to this method.
   */
  public NsSet split() throws NoSuchMethodException {
    throw new NoSuchMethodException("Not implemented yet!");
  }

  /**
   * Get the number of entries in the NsSet object.
   *
   * @param ptr to the Ns_Set structure.
   * @return the number of entries.
   */
  protected native String _size(String ptr);

  /**
   * Get the number of entries in the NsSet object.
   *
   * @return the number of entries.
   */
  public Integer size() {
    return new Integer(this._size(this.getPointer()));
  }

  /**
   * Get the name of the NsSet object.
   *
   * @param ptr to the Ns_Set structure.
   * @return the name.
   */
  protected native String _name(String ptr);

  /**
   * Get the name of the NsSet object.
   *
   * @return the name.
   */
  public String name() {
    return this._name(this.getPointer());
  }

  /**
   * Print the NsSet structure.
   *
   * @param ptr to the Ns_Set structure.
   */
  protected native void _print(String ptr);

  /**
   * Print the NsSet structure.
   *
   */
  public void print() {
    this._print(this.getPointer());
  }

  /**
   * Free the NsSet structure.
   *
   * @param ptr to the Ns_Set structure.
   */
  protected native void _free(String ptr);

  /**
   * Free the NsSet structure.
   *
   */
  public void free() {
    if(this.pointer != null) {
      this._free(this.getPointer());
      this.pointer = null;
    }
  }

  /**
   * Find the index corresponding to the specified key.
   *
   * @param ptr to the Ns_Set structure.
   * @param ky the key name.
   * @return the index.
   */
  protected native String _find(String ptr, String ky);

  /**
   * Find the index corresponding to the specified key.
   *
   * @param key the key name.
   * @return the index.
   */
  public Integer find(String key) {
    return new Integer(this._find(this.getPointer(), key));
  }

  /**
   * Get the entry corresponding to the specified key.
   *
   * @param ptr to the Ns_Set structure.
   * @param ky the key name.
   * @return the value.
   */
  protected native String _get(String ptr, String ky);

  /**
   * Get the entry corresponding to the specified key.
   *
   * @param key the key name.
   * @return the value.
   */
  public String get(String key) {
    return this._get(this.getPointer(), key);
  }

  /**
   * Is the key unique within this NsSet?
   *
   * @param ptr to the Ns_Set structure.
   * @param ky the key name.
   * @return true/false
   */
  protected native String _unique(String ptr, String ky);

  /**
   * Is the key unique within this NsSet?
   *
   * @param key the key name.
   * @return true/false
   */
  public boolean unique(String key) {
      if(this._unique(this.getPointer(), key).equals("1")) {
          return true;
      }
      else {
          return false;
      }
  }

  /**
   * Find the index corresponding to the specified key (ignore case).
   *
   * @param ptr to the Ns_Set structure.
   * @param ky the key name.
   * @return the index.
   */
  protected native String _ifind(String ptr, String ky);

  /**
   * Find the index corresponding to the specified key (ignore case).
   *
   * @param key the key name.
   * @return the index.
   */
  public Integer ifind(String key) {
    return new Integer(this._ifind(this.getPointer(), key));
  }

  /**
   * Get the entry corresponding to the specified key (ignore case).
   *
   * @param ptr to the Ns_Set structure.
   * @param ky the key name.
   * @return the value.
   */
  protected native String _iget(String ptr, String ky);

  /**
   * Get the entry corresponding to the specified key (ignore case).
   *
   * @param key the key name.
   * @return the value.
   */
  public String iget(String key) {
    return this._iget(this.getPointer(), key);
  }

  /**
   * Delete the entry corresponding to the specified key (ignore case).
   *
   * @param ptr to the Ns_Set structure.
   * @param ky the key name.
   */
  protected native void _idelete(String ptr, String ky);

  /**
   * Delete the entry corresponding to the specified key (ignore case).
   *
   * @param key the key name.
   */
  public void idelkey(String key) {
    this._idelete(this.getPointer(), key);
  }

  /**
   * Is the key unique within this NsSet(ignore case)?
   *
   * @param ptr to the Ns_Set structure.
   * @param ky the key name.
   * @return true/false
   */
  protected native String _iunique(String ptr, String ky);

  /**
   * Is the key unique within this NsSet(ignore case)?
   *
   * @param key the key name.
   * @return true/false
   */
  public boolean iunique(String key) {
      if(this._iunique(this.getPointer(), key).equals("1")) {
          return true;
      }
      else {
          return false;
      }
  }

  /**
   * Get the value corresponding to the specified index.
   *
   * @param ptr to the Ns_Set structure.
   * @param idx the index of the value.
   * @return the value.
   */
  protected native String _value(String ptr, String idx);

  /**
   * Get the value corresponding to the specified index.
   *
   * @param index the index of the value.
   * @return the value.
   */
  public String value(int index) {
    return this._value(this.getPointer(), (new Integer(index)).toString());
  }

  /**
   * Get the value corresponding to the specified index.
   *
   * @param index the index of the value.
   * @return the value.
   */
  public String value(Integer index) {
    return this._value(this.getPointer(), index.toString());
  }

  /**
   * Is the value corresponding to the specified index null?
   *
   * @param ptr to the Ns_Set structure.
   * @param idx the index of the value.
   * @return 1/0
   */
  protected native String _isnull(String ptr, String idx);

  /**
   * Is the value corresponding to the specified index null?
   *
   * @param index the index of the value.
   * @return true/false
   */
  public boolean isnull(int index) {
    if(this._isnull(this.getPointer(), (new Integer(index)).toString()).equals("1")) {
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Is the value corresponding to the specified index null?
   *
   * @param index the index of the value.
   * @return true/false
   */
  public boolean isnull(Integer index) {
    if(this._isnull(this.getPointer(), index.toString()).equals("1")) {
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * The key of the corresponding index.
   *
   * @param ptr to the Ns_Set structure.
   * @param idx the index of the value.
   * @return the key
   */
  protected native String _key(String ptr, String idx);

  /**
   * The key of the corresponding index.
   *
   * @param idx the index of the value.
   * @return the key
   */
  public String key(int index) {
    return this._key(this.getPointer(), (new Integer(index)).toString());
  }

  /**
   * The key of the corresponding index.
   *
   * @param idx the index of the value.
   * @return the key
   */
  public String key(Integer index) {
    return this._key(this.getPointer(), index.toString());
  }

  /**
   * Delete the entry corresponding to the specified index.
   *
   * @param ptr to the Ns_Set structure.
   * @param idx the key name.
   */
  protected native void _delete(String ptr, String idx);

  /**
   * Delete the entry corresponding to the specified index.
   *
   * @param index the key name.
   */
  public void delete(int index) {
    this._delete(this.getPointer(), (new Integer(index)).toString());
  }

  /**
   * Delete the entry corresponding to the specified index.
   *
   * @param index the key name.
   */
  public void delete(Integer index) {
    this._delete(this.getPointer(), index.toString());
  }

  /**
   * Truncate the NsSet structure.
   *
   * @param ptr to the Ns_Set structure.
   * @param idx index of the last entry.
   */
  protected native void _truncate(String ptr, String idx);

  /**
   * Truncate the NsSet structure.
   *
   * @param index index of the last entry.
   */
  public void truncate(int index) {
    this._truncate(this.getPointer(), (new Integer(index)).toString());
  }

  /**
   * Truncate the NsSet structure.
   *
   * @param index index of the last entry.
   */
  public void truncate(Integer index) {
    this._truncate(this.getPointer(), index.toString());
  }

  /**
   * update the entry corresponding to the key.
   *
   * @param ptr to the Ns_Set structure.
   * @param ky the key
   * @param vl the value
   * @return index of updated entry.
   */
  protected native String _update(String ptr, String ky, String vl);

  /**
   * update the entry corresponding to the key.
   *
   * @param key the key
   * @param val the value
   * @return index of updated entry.
   */
  public Integer update(String key, String val) {
    return new Integer(this._update(this.getPointer(), key, val));
  }

  /**
   * put the new value in the set if the key doesn't exist already(ignore case)
   *
   * @param ptr to the Ns_Set structure.
   * @param ky the key
   * @param vl the value
   * @return index of updated entry.
   */
  protected native String _icput(String ptr, String ky, String vl);

  /**
   * put the new value in the set if the key doesn't exist already(ignore case)
   *
   * @param key the key
   * @param val the value
   * @return index of updated entry.
   */
  public Integer icput(String key, String val) {
    return new Integer(this._icput(this.getPointer(), key, val));
  }

  /**
   * put the new value in the set if the key doesn't exist already.
   *
   * @param ptr to the Ns_Set structure.
   * @param ky the key 
   * @param vl the value
   * @return index of updated entry.
   */
  protected native String _cput(String ptr, String ky, String vl);

  /**
   * put the new value in the set if the key doesn't exist already.
   *
   * @param key the key
   * @param val the value
   * @return index of updated entry.
   */
  public Integer cput(String key, String val) {
    return new Integer(this._cput(this.getPointer(), key, val));
  }

  /**
   * append the new value in the set.
   *
   * @param ptr to the Ns_Set structure.
   * @param ky the key
   * @param vl the value
   * @return index of updated entry.
   */
  protected native String _put(String ptr, String ky, String vl);

  /**
   * append the new value in the set.
   *
   * @param key the key
   * @param val the value
   * @return index of updated entry.
   */
  public Integer put(String key, String val) {
    return new Integer(this._put(this.getPointer(), key, val));
  }

  /**
   * merge another set with this one.
   *
   * @param high pointer to this Ns_Set structure.
   * @param low pointer to the other Ns_Set structure.
   */
  protected native void _merge(String high, String low);

  /**
   * merge another set with this one.
   *
   * @param low pointer to the other Ns_Set structure.
   */
  public void merge(NsSet low) {
    this._merge(this.getPointer(), low.getPointer());
  }

  /**
   * move another set to this one.
   *
   * @param to pointer to this Ns_Set structure.
   * @param from pointer to the other Ns_Set structure.
   */
  protected native void _move(String to, String from);

  /**
   * move another set to this one.
   *
   * @param from the other Ns_Set structure.
   */
  public void move(NsSet from) {
    this._move(this.getPointer(), from.getPointer());
  }
  static {
    System.loadLibrary("tclblend");
  }  
}
