# This script will invoke JAVAC with file name arguments
# that have had path names converted from forward
# slashes to backslashes. The JDK 1.1 version of javac
# will generate an pointless error when paths with forward
# slashes are passed to it. This script is only used
# when compilinig under Win32 with JDK 1.1.

RT_SCRIPT='s/\/\([A-Z|a-z]\)\//\1\:\//g'
BS_SCRIPT='s/\//\\\\/g'

# Parse program out from args since we need to pass
# the program name as a unix style path.

CMD=$@
PROG=""
ARGS=""

for index in $CMD ; do
    if test "$PROG" = "" ; then
        PROG=$index
    else
        ARGS="$ARGS $index"
    fi
done

# Replace /d/dir with d:/dir in ARGS
# before doing / -> \ replacement

#echo "ARGS is \"$ARGS\""
ARGS=`echo $ARGS | sed -e $RT_SCRIPT`
#echo "ARGS is \"$ARGS\""
ARGS=`echo $ARGS | sed -e $BS_SCRIPT`
#echo "ARGS is \"$ARGS\""

#echo "now to exec \"$PROG $ARGS\""
eval exec $PROG $ARGS
