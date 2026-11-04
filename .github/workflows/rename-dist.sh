#!/bin/bash
if [[ $# -ne 1 ]]
then
    echo "$0: no args, exitting"
    exit 0
fi

TAG="$1"
shift

if [ ! -d "./dist" ]
then
    echo "$0: No ./dist - exitting"
    exit 0
fi

# usage: fixrel 48.2.1-beta6
function fixrel()
{
    REL=$1
    shift

    for f in $(cd dist; ls *.zip);
    do
        BASE=$(basename $f .zip)
        mv -v dist/${BASE}.zip dist/${BASE}-${REL}.zip
    done

    for f in $(cd dist; ls *.jar);
    do
        BASE=$(basename $f .jar)
        mv -v dist/${BASE}.jar dist/${BASE}-${REL}.jar
    done
}

case "$TAG" in
    production/*)
        echo "$0: Ignoring tag $TAG"
        exit 0
        ;;
    release-*)
        # release-48-2 => 48.2,  release-30-0-1 => 30.0.1, release-29-beta-1 => 29-beta-1
        REL=$(echo $TAG | cut -d- -f2- | sed -e 's%\([0-9][0-9]*\)-%\1.%g' -e 's%\.\([a-z]\)%-\1%g')
        echo "$0: Found release tag ${REL}"
        fixrel ${REL}
        ;;
    *)
        echo "$0: Unrecognized tag $TAG, not of the form 'release-*'"
        exit 0
        ;;
esac
