# This script will convert a UNIX path to a Win32 native path
UPATH=$1
if test "$UPATH" = ""; then
    echo EMPTY
    exit 1
fi
#echo "INPUT IS \"$UPATH\"" >&2
if test -d $UPATH ; then
    cd $UPATH
    WPATH=`pwd -W`
else
    cd `dirname $UPATH`
    WPATH=`pwd -W`/`basename $UPATH`
fi
#echo "OUTPUT IS \"$WPATH\"" >&2
echo $WPATH
exit 0

