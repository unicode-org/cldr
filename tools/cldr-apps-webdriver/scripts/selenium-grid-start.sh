#!/bin/bash
VER=4.32.0
URL=https://github.com/SeleniumHQ/selenium/releases/download/selenium-${VER}/selenium-server-${VER}.jar
FILE=selenium-server.jar
if [ ! -f ${FILE} ];
then
    echo "Fetching ${URL}"
    curl -LJ -o"${FILE}" "${URL}" || exit 1
fi
java -jar ${FILE} standalone --selenium-manager true &
echo "Start server ${VER} with PID $?"
