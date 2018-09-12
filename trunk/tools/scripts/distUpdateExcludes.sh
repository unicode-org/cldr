#!/bin/bash

if [[ ! -f $(basename "${0}") || ! -d "../scripts" ]];
then
    echo "Error - run this script from the tools/scripts dir."
    exit 1
fi

DISTFILE=tools/dist.conf/distExcludes.txt
cd ../..
> "${DISTFILE}"
for item in $(svn status --no-ignore  | grep -v '^M' | cut -c9-);
do
    if [[ "${item}" == "tools/java/cldr.jar" ]]; # allow this
    then
        true
    elif [[ -d "${item}" ]];
    then
        echo "${item}/" >> "${DISTFILE}"
    else
        echo "${item}" >> "${DISTFILE}"
    fi
done

echo "# updated ${DISTFILE}"
