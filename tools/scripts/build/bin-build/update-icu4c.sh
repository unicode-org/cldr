#!/bin/sh
set -x
CLDR_DIR=`pwd`
export CLDR_DIR
ant -f icu4c/source/data/build.xml clean all
