#!/bin/bash
#
# To use:
#   run 'bash check_redirects.sh'
#
# requires curl

CURL=curl
CURL_OPTS="-s -D -"
URL=http://www.unicode.org
DTD2VERSION=./dtdToVersion.sh
ERR=`tput smso`error`tput rmso`
TMPF=${0}.$$.tmp

echo "# Checking redirects on ${URL}"
${CURL} -s "${URL}" >/dev/null || ( echo "Error: You don't seem to have a working ${CURL} command" '($CURL)' ", or ${URL} is down.. Goodbyer." ; exit 1 )
echo "# ${CURL} ${URL} - OK."

FAIL=0

#HTTP/1.1 302 Found
#Date: Tue, 11 Jun 2013 15:55:54 GMT
#Server: Apache
#Location: http://www.unicode.org/cldr/charts/22/index.html
#Content-Length: 232
#Content-Type: text/html; charset=iso-8859-1

DTDLOC=common/dtd/ldml.dtd
VLATEST=`${CURL} -s "${URL}/repos/cldr/tags/latest/${DTDLOC}" | sh ${DTD2VERSION}`
VDEV=`${CURL} -s "${URL}/repos/cldr/trunk/${DTDLOC}" | sh ${DTD2VERSION}`

echo "CLDR latest=${VLATEST} trunk=${VDEV}"


verify_redirect()
{
    EXPECTSTATUS="$1"
    FROMURL="$2"
    TOURL="$3"
    echo "# TEST: ${FROMURL} -> ${TOURL} [ ${EXPECTSTATUS} ]"
    ${CURL} ${CURL_OPTS} "${FROMURL}"  > ${TMPF} || ( echo "Error fetching ${FROMURL}!" ; FAIL=1 )

    STATUS=`head -1 "${TMPF}" | cut -d' ' -f2`
    LOCATION=`grep '^Location: ' "${TMPF}" | head -1 | cut -d' ' -f2- | tr -d '\r'`
    echo "#  GOT: ${FROMURL} -> ${LOCATION} [ ${STATUS} ]"

    if [[ "$STATUS" -ne "${EXPECTSTATUS}" ]];
    then
        echo "${ERR} - expected status ${EXPECTSTATUS} not ${STATUS}"
        FAIL=1
    elif [[ "${LOCATION}" = "${TOURL}" ]];
    then
        echo "# OK."
    else
        echo "${ERR}: got: ${LOCATION}"
        echo "# expected: ${TOURL}"
        FAIL=1
    fi
}

verify_content()
{
    EXPECTSTATUS="$1"
    FROMURL="$2"
    TOTXT="$3"
    echo "# TEST: ${FROMURL} == ${TOTXT} [ ${EXPECTSTATUS} ]"
    ${CURL} ${CURL_OPTS} "${FROMURL}"  > ${TMPF} || ( echo "Error fetching ${FROMURL}!" ; FAIL=1 )

    STATUS=`head -1 "${TMPF}" | cut -d' ' -f2`
    LOCATION=`grep '^Location: ' "${TMPF}" | head -1 | cut -d' ' -f2- | tr -d '\r'`
    echo "#  GOT: ${FROMURL} -> ${LOCATION} [ ${STATUS} ]"

    if [[ $STATUS -ne ${EXPECTSTATUS} ]];
    then
        echo "${ERR} - expected status ${EXPECTSTATUS} not ${STATUS}"
        FAIL=1
    elif [[ "${LOCATION}" != "" ]];
    then
        echo "${ERR}: got: ${LOCATION}"
        FAIL=1
    elif grep -q -- "${TOTXT}" "${TMPF}";
    then
        echo "# OK - got ${TOTXT}"
    else
        ln "${TMPF}" /tmp/failx
        echo "Error - did not get ${TOTXT}";
        FAIL=1
    fi
}

#### now, check

# google docs - can't test

# data (zip)
SOMEFILE=shoesize.zip
verify_redirect 302 "${URL}/Public/cldr/latest" "${URL}/Public/cldr/${VLATEST}"
verify_redirect 302 "${URL}/Public/cldr/latest/${SOMEFILE}" "${URL}/Public/cldr/${VLATEST}/${SOMEFILE}"

# Charts
verify_redirect 302 "${URL}/cldr/charts/dev" "${URL}/cldr/charts/dev/"
verify_content  200 "${URL}/cldr/charts/${VDEV}/" "Version ${VDEV}"

verify_redirect 302 "${URL}/cldr/charts/latest" "${URL}/cldr/charts/latest/"
verify_redirect 302 "${URL}/cldr/charts/${VLATEST}" "${URL}/cldr/charts/${VLATEST}/"
verify_content  200 "${URL}/cldr/charts/${VLATEST}/" "Version ${VLATEST}"

verify_content  200 "${URL}/cldr/charts/latest/summary/bs_Cyrl.html" "CLDR Version ${VLATEST}"
verify_content  200 "${URL}/cldr/charts/dev/summary/bs_Cyrl.html" "CLDR Version ${VDEV}"

# add 'latest' if missing
verify_redirect 302 "${URL}/cldr/charts/summary/bs_Cyrl.html" "${URL}/cldr/charts/latest/summary/bs_Cyrl.html"

# static check
verify_redirect 302 "${URL}/cldr/charts/22" "${URL}/cldr/charts/22/"
verify_redirect 302 "${URL}/cldr/charts/22.1" "${URL}/cldr/charts/22.1/"

# catch all
verify_redirect 302 "${URL}/cldr/charts" "http://cldr.unicode.org/index/charts"

# reports
verify_redirect 302 "${URL}/cldr/changes/dev" "http://unicode.org/cldr/trac/report/63"
verify_redirect 302 "${URL}/cldr/changes/latest" "http://unicode.org/cldr/trac/report/62"

# svn (Raw)
verify_redirect 302 "${URL}/cldr/dev" "http://unicode.org/repos/cldr/trunk"
verify_redirect 302 "${URL}/cldr/latest" "http://unicode.org/repos/cldr/tags/latest"
verify_redirect 302 "${URL}/cldr/data" "http://unicode.org/repos/cldr/trunk"

#### clean up
rm -f "${TMPF}"
if [[ ${FAIL} = 1 ]];
then
    echo "## FAIL"
    exit 1
else
    echo "## All OK!"
    exit 0
fi


# Emacs Local Variables: #
# Emacs compile-command: "bash check_redirects.sh" #
# Emacs End: #
