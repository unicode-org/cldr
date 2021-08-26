#!/bin/bash
npm i
set -x
# copy this locally
if [ ! -f ./reports-v2.css ];
then
    wget -c 'http://www.unicode.org/reports/reports-v2.css'
fi
# copy this locally
if [ ! -f ./logo60s2.gif ];
then
    wget -c 'https://www.unicode.org/reports/logo60s2.gif'
fi
mkdir -p dist
cp -vR ../../../docs/ldml/ ./dist/
node archive.js && zip -r tr35.zip dist/*.html dist/images
