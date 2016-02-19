#!/bin/sh
if [ -f tools/java/cldr.jar ];
then
	cd tools/java
fi
if [ ! -f cldr.jar ];
then
	echo ERR cannot find  cldr.jar >&2
	pwd >&2
	exit 1
fi

rm -rf ${BUILDER_BEST}/cldr-tools && mkdir -p ${BUILDER_BEST}/cldr-tools && cp -r libs cldr.jar classes ${BUILDER_BEST}/cldr-tools/
