#!/bin/bash

ABNF_DIR=keyboards/abnf
TEST_DIR=tools/scripts/keyboard-abnf-tests
abnf_check="npx --package=abnf abnf_check"
abnf_test="npx --package=abnf abnf_test"
TEMP=$(mktemp -d)
echo "-- checking ABNF --"

for abnf in ${ABNF_DIR}/*.abnf; do
    echo Validating ${abnf}
    ${abnf_check} ${abnf} || exit 1
done

echo "-- running test suites --"

for abnf in ${ABNF_DIR}/*.abnf; do
    echo Testing ${abnf}
    base=$(basename ${abnf} .abnf)
    # fix for node-abnf issue
    fgrep -v SKIP-NODE-ABNF < ${abnf} > ${TEMP}/${base}.abnf
    abnf=${TEMP}/${base}.abnf
    SUITEDIR=${TEST_DIR}/${base}.d
    if [[ -d ${SUITEDIR} ]];
    then
        echo "  Test suite ${SUITEDIR}"
        for testf in ${SUITEDIR}/*.pass.txt; do
            start=$(basename ${testf} .pass.txt)
            echo "   Testing PASS ${testf} for ${start}"
            while IFS="" read -r str || [ -n "$str" ]
            do
                if echo "${str}" | grep -v -q '^#'; then
                    echo "# '${str}'"
                    (${abnf_test} ${abnf} -t "${str}") 2>&1 >/dev/null || exit 1
                fi
            done <${testf}
        done
        for testf in ${SUITEDIR}/*.fail.txt; do
            start=$(basename ${testf} .fail.txt)
            echo "   Testing FAIL ${testf} for ${start}"
            while IFS="" read -r str || [ -n "$str" ]
            do
                if echo "${str}" | grep -v -q '^#'; then
                    echo "# '${str}'"
                    (${abnf_test} ${abnf} -t "${str}") 2>&1 > /dev/null && (echo ERROR should have failed ; exit 1)
                fi
            done <${testf}
        done
    else
        echo "  Warning: ${SUITEDIR} did not exist"
    fi
    # npx --package=abnf abnf_check ${abnf} || exit 1
done

echo "All OK"
exit 0

