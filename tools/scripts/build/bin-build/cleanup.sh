#!/bin/sh
/usr/bin/find ${BUILDER_DIR} -maxdepth 1 -name 'build_*' -mtime +7 -ls -exec echo rm -rf {} \;
#/usr/bin/find /tmp/ -name 'ant_log*' -mtime +7 -exec /bin/rm -rf {} \;
