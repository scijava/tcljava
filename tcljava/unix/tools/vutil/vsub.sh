#!/bin/sh

if test "${TCLSH}" = ""; then
    TCLSH=tclsh
fi
CUR=`pwd`/current
RCSVER=`pwd`/rcsver.tcl
FSUB=`pwd`/fsub.tcl


# go to the root dir in the dist
cd ../../..


# get first argument passed into this shell

TCLJAVA_VERSION=${1}
if test "${TCLJAVA_VERSION}" = "" ; then
    echo "must pass version number as argument"
    exit 1
fi


# Update version numbers of the files with package statements in them

FILES="src/pkgIndex.tcl"
FILES="$FILES src/jacl/tcl/lang/Interp.java"
FILES="$FILES src/tcljava/tcl/lang/BlendExtension.java"

$TCLSH $FSUB '(package +[a-z]* +java +)[0-9]+(\.[0-9]+)*' \
	"\\1$TCLJAVA_VERSION" $FILES


# Update version numbers of the files with TCLJAVA_VERSION in them

FILES=""
FILES="$FILES configure.in"
FILES="$FILES win/makefile.vc"
FILES="$FILES $CUR"

$TCLSH $FSUB '(TCLJAVA_VERSION[ |\t]*=[ |\t]*)[0-9]+(\.[0-9]+)*' \
	"\\1$TCLJAVA_VERSION" $FILES


echo "done with version substitutions"
exit 0

