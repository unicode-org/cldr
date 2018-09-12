#!/bin/sh
NAME=common
if [ ! -d ${NAME} ];
then
	echo no dir ${NAME} >&2
	exit 1
fi

rm -rf ${BUILDER_BEST}/${NAME}
mkdir -p ${BUILDER_BEST}/${NAME}
cp -r ${NAME}  seed ${BUILDER_BEST}/${NAME}
