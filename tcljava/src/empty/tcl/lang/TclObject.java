// empty implementation of TclObject used for compiling in multiple packages
// only those functions called from tcljava classes are listed here.

package tcl.lang;

import java.util.Hashtable;

public final class TclObject extends TclObjectBase {

static final boolean saveObjRecords = TclObjectBase.saveObjRecords;
static Hashtable objRecordMap = TclObjectBase.objRecordMap;

public TclObject(InternalRep rep) {
    super(rep);
}

protected TclObject(TclString rep, String s) {
    super(rep, s);
}

public final void preserve() {}

public final void release() {}

} // end TclObject
