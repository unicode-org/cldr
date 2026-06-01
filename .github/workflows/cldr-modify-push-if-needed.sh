#!/bin/bash
# this script is called by cldr-modify.yml to push to upstream if needed
set -euo pipefail
# Usage: $0 remote branch path
REMOTE="$1"
BRANCH="$2"
CHECKPATH="$3"
DIFF_FILE=$(mktemp)

# set -x
git fetch -q ${REMOTE}
if git rev-parse --verify ${REMOTE}/${BRANCH} 2>/dev/null;
then
    echo " - Checking if ${REMOTE}/${BRANCH} is up to date"
    git diff --stat HEAD ${REMOTE}/${BRANCH} -- ${CHECKPATH} > "${DIFF_FILE}"
    if [ -s "${DIFF_FILE}" ];
    then
        echo " - ${REMOTE}/${BRANCH} is out of date, will push"
    else
        echo " - ${REMOTE}/${BRANCH} is identical, no need to push"
        exit 0
    fi
else
    echo " - ${REMOTE}/${BRANCH} doesn't exist, we'll create it"
fi

set -x
exec git push  -f ${REMOTE} HEAD:${BRANCH}
