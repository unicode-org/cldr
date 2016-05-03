#!/bin/sh
echo "Shutting down Smoketest tomcat"
ssh st.unicode.org 'sh tomcat/bin/shutdown.sh'&
echo "Shutting down buildbot tomcat"
cd ${HOME}
killall -9 java
killall -9 java
killall -9 java
sleep 2
RUNDIR=${HOME}

sh ${HOME}/tomcat/bin/startup.sh
sleep 10
echo "Deleting unpacked smoketest on st"
ssh st.unicode.org 'rm -rf tomcat/webapps/smoketest'
exit 0
