#!/bin/sh
. ${HOME}/bin-build/stbitten-env.sh
. ${HOME}/tomcat/env.sh
if [ -d ${BUILDER_BEST}/common/seed ];
then
	# old svn
	BESTVER=`svnversion ${BUILDER_BEST}/common/seed`
elif [ -f ${BUILDER_BEST}/cldr-apps/currev.txt ];
then
	CLDR_CURREV=`cat ${BUILDER_BEST}/cldr-apps/currev.txt`
else
	CLDR_CURREV=`svnversion .`
fi
echo currev ${CLDR_CURREV}
echo "CLDR_CURREV=${BESTVER}" > ${CATALINA_BASE}/cldr/currev.properties
