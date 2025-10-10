#!/bin/bash

function msg() {
    if [[ ! -z "${GITHUB_STEP_SUMMARY}" ]];
    then
        echo "$@" >> "${GITHUB_STEP_SUMMARY}"
    fi
    echo "% $@"
}


if [[ $# -ne 2 ]];
then
    msg "X Internal Error"
    echo >&2 "Error: usage: $0 ref1 ref2"
    exit 1
fi

CHANGED=$(git diff --name-status $2...$1 | grep '^[MA]' | cut -c2- | grep 'docs/ldml/tr35.*\.md')

if [[ -z "${CHANGED}" ]];
then
    msg "∅ No spec changes."
    exit 0
fi

function list_changed() {
    msg ""
    msg "## List of changes:"
    for file in ${CHANGED}; do
        msg "- ${file}"
    done
    msg ""
}

if echo "${CHANGED}" | fgrep -s '/tr35-modifications.md';
then
    msg "✅ Spec changes: tr35-modifications.md was updated"
    list_changed
    exit 0
else
    msg "⚠️ Spec changes but tr35-modifications.md section was not updated!"
    msg "For non-substantive changes that do not require an item in modifications such as fixing typos, add an HTML comment such as:"
    msg ""
    msg "     <!-- updated whitespace -->"
    msg ""
    msg "to the file."
    list_changed
    exit 1
fi
