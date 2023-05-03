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

# Generate output .html
node archive.js || exit 1

# Generate .pdf for each page
for file in dist/tr35-keyboards.html; do # TODO: dist/*.html for all
    base=$(basename "${file}" .html)
    npx electron-pdf -m 0 file://$(pwd)/"${file}" dist/"${base}".pdf
done

# zip it up
exec zip -r tr35.zip dist/*.html dist/*.pdf dist/images dist/js
