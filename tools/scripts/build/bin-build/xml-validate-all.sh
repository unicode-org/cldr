#!/bin/sh
# now we have -q optiuon
#find . -name '*.xml' | xargs ~/bin-build/XMLValidator 2>&1 | tee validate.log | fgrep -v 'Processing file '
XMLValidator -q seed common 2>&1 | tee validate.log 
fgrep -q 'Exception in thread' validate.log && exit 1
fgrep -q ERROR validate.log  || exit 0
echo `fgrep -c ERROR validate.log` errors. >&2 
exit 1
