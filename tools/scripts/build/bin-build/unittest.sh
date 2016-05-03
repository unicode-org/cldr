#!/bin/sh
set -x
case `uname -p` in
    i586)
    SIZES="-DJVM_EXTRA_OPTIONS=-Xmx2100m"
    ;;

    powerpc)
    SIZES="-DJVM_EXTRA_OPTIONS=-Xmx5000m"
    ;;

    x86_64)
    SIZES="-DJVM_EXTRA_OPTIONS=-Xmx4500m"
    ;;

    *)
    SIZES="-DJVM_EXTRA_OPTIONS=-Xmx2000m"
    ;;
esac
if [ -d tools/cldr-unittest ];
then
	# from r10119 unittests are in tools/cldr-unittest
	cd tools/cldr-unittest
elif [ -d tools/java ];
then
	cd tools/java
else
	echo Cant find test dir 
	pwd
	exit 1
fi
exec ant -f build.xml -DCLDR_DIR=../.. ${SIZES}  check

