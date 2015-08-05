#!/bin/sh
# Copyright (C) 2013-2015, International Business Machines Corporation
#  and others.  All Rights Reserved.
#

if [ "x${CLDR_DIR}" = "x" ];
then
   echo "You must set the CLDR_DIR environment variable"
   exit 1
fi

cd ${CLDR_DIR}
find common exemplars keyboards seed specs tools/java tools/cldr-unittest -type f -print | while read filename
do
   echo "Processing... ${filename}"
   ext=`echo ${filename} | cut -f2 -d'.'`
   if [ "x${ext}" = "xxml" ];
   then
      svn propset -q svn:eol-style native ${filename}
      svn propset -q svn:keywords "Author Id Revision" ${filename}
      svn propset -q svn:mime-type text/xml ${filename}
   fi
   if [ "x${ext}" = "xjava" ];
   then
      svn propset -q svn:eol-style native ${filename}
      svn propset -q svn:mime-type "text/plain;charset=utf-8" ${filename}
   fi
   if [ "x${ext}" = "xtxt" ];
   then
      svn propset -q svn:eol-style native ${filename}
      svn propset -q svn:mime-type "text/plain" ${filename}
   fi
   if [ "x${ext}" = "xcss" ];
   then
      svn propset -q svn:eol-style native ${filename}
      svn propset -q svn:mime-type "text/css" ${filename}
   fi
   if [ "x${ext}" = "xhtml" ];
   then
      svn propset -q svn:eol-style native ${filename}
      svn propset -q svn:mime-type "text/html" ${filename}
   fi
done
