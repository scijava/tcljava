#!/bin/sh

TCLSH=tclsh
CUR=`pwd`/current
RCSVER=`pwd`/rcsver.tcl
FSUB=`pwd`/fsub.tcl

# go to the root dir in the dist
cd ../../..


# double check that this is the correct directory

if test ! -f changes.txt; then
    echo "`pwd`/changes.txt does not exist!, please check vsub.sh"
    exit -1
fi


TCLJAVA_VERSION=`$TCLSH $RCSVER changes.txt`
#TCLJAVA_VERSION=1.2.1

# Find out if we need to update the version numbers by checking to
# see if the one we got differs from the one in the file current.

if env grep "TCLJAVA_VERSION=$TCLJAVA_VERSION" $CUR > /dev/null; then
    # If they are the same we do not need to update
    exit 0
else
    # Otherwise we do need to update
    echo "Updating version numbers in the source files to $TCLJAVA_VERSION"
fi



# Update version numbers of the files with package statements in them

FILES="src/pkgIndex.tcl"
FILES="$FILES src/jacl/tcl/lang/Interp.java"
FILES="$FILES src/tcljava/tcl/lang/BlendExtension.java"

$TCLSH $FSUB '(package +[a-z]* +java +)[0-9]+(\.[0-9]+)*' \
	"\\1$TCLJAVA_VERSION" $FILES


# Update version numbers of the files with TCLJAVA_VERSION in them

FILES="unix/configure.in"
FILES="$FILES unix/configure"
FILES="$FILES win/makefile.vc"
FILES="$FILES $CUR"

$TCLSH $FSUB '(TCLJAVA_VERSION[ |\t]*=[ |\t]*)[0-9]+(\.[0-9]+)*' \
	"\\1$TCLJAVA_VERSION" $FILES


echo "done with version substitutions"
exit 0

