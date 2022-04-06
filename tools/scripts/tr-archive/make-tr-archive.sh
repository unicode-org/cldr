#!/bin/bash

npm i
set -x
# Run the Toc
npm run fix-tocs || exit 1

# setup dist/js with javascript module(s)
mkdir -p dist dist/js
cp node_modules/anchor-js/anchor.min.js dist/js

# copy the source .md and other stuff into dist
cp -vR ../../../docs/ldml/* ./dist/

# Generate output .html, and zip it all up.
node archive.js && zip -r tr35.zip dist/*.html dist/images dist/js
