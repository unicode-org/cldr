#!/bin/bash
set -x
cd ../dist && shasum -a 512 cldr-*.{jar,zip} | tee SHASUM512.txt
