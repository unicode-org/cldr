#!/bin/sh
JRE_HOME=/usr/lib/jvm/java-1.7.0/
CATALINA_PID="$CATALINA_BASE/tomcat.pid"
JAVA_OPTS="${JAVA_OPTS} -Dcldr.home=${CATALINA_BASE}"
