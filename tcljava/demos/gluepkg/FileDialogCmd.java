/*
 * FileDialogCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: FileDialogCmd.java,v 1.1 1998/10/14 21:09:23 cvsadmin Exp $
 *
 */

import tcl.lang.*;
import java.awt.*;
import java.io.*;

/*
 * This class implements the "filedialog" demo command.
 */

class FileDialogCmd implements Command {
  
    /*
     * cmdProc --
     *
     * This procedure is invoked to process the FileDialogApp application.
     * If no arguments are supplied then the filedialog directory begins
     * in the current directory, otherwise it begins in the supplied dir.
     * If dir and file are valid the result is set to the valid absolute path.
     */

    public void cmdProc(Interp interp, TclObject argv[])
            throws TclException {
        FileDialogApp fileDialog;      /* The FileDialog widget         */
	String        dirName = null;  /* The dir to start with, if any */


	if ((argv.length != 1) && (argv.length != 3)) {
	    throw new TclNumArgsException(interp, 1, argv,
		    "?-directory dirName?");
	}

	if ((argv.length == 3)) {
	    if("-directory".startsWith(argv[1].toString())) {
	        dirName = argv[2].toString();
	    } else {
	        throw new TclException(interp, "bad filedialog option \""
                        + argv[1].toString() + "\": must be -directory");
	    }
	}

	/*
	 * Run the "application".  This blocks until the window has been
	 * removed, either by pressing 'OK' or 'Cancel' or destroying the 
	 * window.
	 */ 

	fileDialog = new FileDialogApp();
	if (dirName != null) {
	    fileDialog.setDirectory(dirName);
	}

	/*
	 * Pull out the dir and filename from the fileDialog object
	 */

	String dir  = fileDialog.getDirectory();
	String file = fileDialog.getFile();

	/*
	 * If a dir and filename are not null then a valid path
	 * been choosen (i.e. The Cancel Button wasnt pressed)
	 */

	if ((dir != null) && (file != null) && (!file.equals(""))) {
	    interp.setResult(dir + file);
	}
    }
}


