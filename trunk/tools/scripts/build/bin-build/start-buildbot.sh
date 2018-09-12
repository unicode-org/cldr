#!/bin/sh
# get the bitten env
. ${HOME}/bin-build/stbitten-env.sh
# get the tomcat env
. ${HOME}/tomcat/env.sh
# startup tomcat
if [ -x ${HOME}/tomcat/CURRENT/bin/startup.sh ];
then
	${HOME}/tomcat/CURRENT/bin/startup.sh
else
	${HOME}/tomcat/bin/startup.sh
fi
# startup bitten
echo Firing up ${BUILDER_NAME} on `hostname`
screen -d -m -S bitten-${BUILDER_NAME} bootloop.sh&
