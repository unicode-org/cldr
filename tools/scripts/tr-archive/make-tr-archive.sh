#!/bin/bash
npm i
set -x
# copy this locally
if [ ! -f ./reports.css ];
then
    wget -c 'https://www.unicode.org/reports/tr35/reports.css'
fi
mkdir -p dist
cp -vR ../../../docs/ldml/ ./dist/
node archive.js
zip -r tr35.zip dist/*.html dist/images
