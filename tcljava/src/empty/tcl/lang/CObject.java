/*
empty implementation of CObject used for compiling in multiple packages
*/

package tcl.lang;

class CObject extends InternalRep {

CObject() {}

CObject(long objPtr) {}

public void dispose() {}

void makeReference(TclObject object) {}

public InternalRep duplicate()
{
  return null;
}

static final void decrRefCount(long objPtr) {}

static final void incrRefCount(long objPtr) {}

static final long
newCObject(String rep)
{
  return 0;
}

} // end CObject
