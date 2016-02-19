#!/bin/bash
mkdir -p ${BUILDER_DIR} 2>/dev/null >/dev/null
#cleanup.sh 2>&1 &
. ${HOME}/bin-build/stbitten-env.sh
bitten-slave -f ${BUILDER_CONF}/config.ini --name=${BUILDER_NAME} -u ${BUILDER_USER} -p ${BUILDER_PASS} -d ${BUILDER_DIR}  -l ${BUILDER_DIR}/log.txt http://unicode.org/cldr/trac/builds -v $@
