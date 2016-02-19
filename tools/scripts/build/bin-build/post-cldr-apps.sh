#!/bin/sh
if [ ! -d "${BUILDER_BEST}" ];
then
	echo $0 BUILDER_BEST is not a dir "${BUILDER_BEST}" - env problem?
	exit 1
fi

rm -rf ${BUILDER_BEST}/cldr-apps
mkdir -p ${BUILDER_BEST}/cldr-apps
if [ -d tools/cldr-apps ];
then
	cd tools/cldr-apps
fi
if [ ! -f cldr-apps.war ];
then
	echo Err cant find cldr-apps.war >&2 
	exit 1
fi

# update-currev.sh updates the revision
cp -r cldr-apps.war ${BUILDER_BEST}/cldr-apps/
svnversion . > ${BUILDER_BEST}/cldr-apps/currev.txt
if [ -x ${HOME}/bitten-conf/post-to-smoketest.sh ];
then
    echo "posting to smoketest"
    ${HOME}/bitten-conf/post-to-smoketest.sh
fi
