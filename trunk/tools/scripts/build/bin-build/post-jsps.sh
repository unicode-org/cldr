#!/bin/sh
exit 0
. stbitten-env.sh
WARFILE=UnicodeJsps.war
DIRNAME=jsps
rm -rf ${HOME}/best/${DIRNAME}
mkdir -p ${HOME}/best/${DIRNAME}
if [ -d . ];
then
	cd .
fi
if [ ! -f ${WARFILE} ];
then
	echo Err cant find ${WARFILE} >&2 
	exit 1
fi

#BESTVER=`svnversion /home/st-bitten/best/common/seed`
#echo "CLDR_CURREV=${BESTVER}" > ${HOME}/tomcat/cldr/currev.properties

cp -r .svn ${WARFILE} ${HOME}/best/${DIRNAME}/
