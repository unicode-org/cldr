#!/bin/sh
mkdir icu4c-build
set -x
cd icu4c-build
#export CC="ccache gcc"
#export CXX="ccache g++"
`pwd`/../icu4c/source/configure --disable-extras --disable-samples && make tests
