#!/bin/sh
# Copyright (C) 2004, International Business Machines Corporation
#  and others.  All Rights Reserved.
#
# Run this in locale/tools and it will create a sidewaysview in ../vetting/sideways

backup_tree()
{
    TREE=$1
    rm -rf ${TREE}.backup1
    mv ${TREE}.backup0 ${TREE}.backup1
    mv ${TREE} ${TREE}.backup1
}

mkdir ../vetting
cd ../vetting
backup_tree sideways
mkdir sideways
mkdir sideways/by_type
#MATCH="-m '.*[mM][tT].*'"
#MATCH="-m '.*[mM][tT].*'"
GenerateSidewaysView -z "x-cockney" -s ../common/main/ -d sideways/ -t ../../../J/icu4j/src/com/ibm/icu/dev/tool/cldr/
