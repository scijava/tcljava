// empty implementation of CObject used for compiling in multiple packages
// only those functions called from tcljava classes are listed here.

package tcl.lang;

class CObject implements InternalRep {

public void dispose() {}

public InternalRep duplicate() { return null; }

void makeReference(TclObject object) {}

} // end CObject
