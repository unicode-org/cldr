#!/bin/bash

if [[ $# -lt 1 ]];
then
    echo "Usage: $0 <sha> <reftomain>" >&2
    exit 1
fi

rm -f docs/site/changed.txt

# this is meant to run in the cloudflare build.
echo "# Changed files in the site:"
git fetch origin
git diff --name-status ${2:-origin/main} $1 | sed -n '/^[A-Z]\tdocs\/site\/\(.*\)\.md$/s//\1.md/p' | tee docs/site/changed.txt
