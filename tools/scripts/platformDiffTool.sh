#!/bin/sh
# Copyright (C) 2004, International Business Machines Corporation
#  and others.  All Rights Reserved.
#
# Run this in locale/tools and it will create an InterimVettingChart in ../vetting/main

# Prefix - will only show files which match the prefix.
MATCHIT=
#MATCHIT=ar
#MATCHIT=fi
ROOT=..
COMMON=${ROOT}/common

backup_tree()
{
    TREE=$1
    rm -rf ${TREE}.new
    mkdir ${TREE}.new
    cp -r ${TREE}/CVS ${TREE}.new/
    rm -rf ${TREE}.backup1
    mv ${TREE}.backup0 ${TREE}.backup1
    mv ${TREE} ${TREE}.backup1
    mv ${TREE}.new ${TREE}
}

compare_tree()
{
    TREE=$1
    for file in `cd ${COMMON}/${TREE} ; ls ${MATCHIT}*.xml | fgrep -v supplementalData`;
      do
      what=""
      for plat in ibmjdk sunjdk windows open_office aix linux solaris hp;
        do
        if [ -f ${ROOT}/${plat}/${TREE}/${file} ]; then
            what="${what} -${plat} ${ROOT}/${plat}/${TREE}/${file}"
        fi
      done
      echo ${what}
      LDMLComparator -d ./${TREE} -common:gold ${COMMON}/${TREE}/${file} ${what}
          
#-ibmjdk ${ROOT}/ibmjdk/${TREE}/${file}
#-sunjdk ${ROOT}/sunjdk/${TREE}/${file}
#-windows ${ROOT}/windows/${TREE}/${file}
#-open_office ${ROOT}/open_office/${TREE}/${file}
#-aix ${ROOT}/aix/${TREE}/${file}
#-linux ${ROOT}/linux/${TREE}/${file}
#-solaris ${ROOT}/solaris/${TREE}/${file}
#-hp ${ROOT}/hp/${TREE}/${file}

    done
}

mkdir -p ../diff
cd ../diff
if [ ! -d ${COMMON}/main ];
then
    echo `basename $0` ":## Error: run me from locale/tools."
    exit 1
fi

echo "INFO: Starting in ../diff/"

#backup_tree collation
#compare_tree collation
backup_tree main
compare_tree main
echo "INFO: Done with ../diff/"
