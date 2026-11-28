#!/bin/bash
set -x
echo "$0: Obsolete, use npm run build"
npm i && exec node build.mjs
