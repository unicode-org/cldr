#!/bin/sh
#PHASE=build
PHASE=final_testing
echo PHASE ${PHASE}
set -x
#echo skipped
#exit 0
if [ ! -d common ];
then
	echo Unknown dir common >&2
	exit 1
fi
rm -rf ${BUILDER_DIR}/cldr-tmp 2>/dev/null
#env JAVA_OPTS="-DCLDR_DIR=. -Dfile.encoding=UTF-8 -DSHOW_FILES -Xmx1000M" ~/bin-build/ConsoleCheckCLDR -g -c minimal -e '-t((?!.*Check(Coverage|Attribute)).*)' -z final_testing  
rm -f .failure
( env JAVA_OPTS="-DCLDR_DIR=. -Dfile.encoding=UTF-8 -DSHOW_FILES -Xmx3000M" ConsoleCheckCLDR  -e -z ${PHASE} 2>&1 || touch .failure ) | tee testlog.txt
if [ -f .failure ];
then
	echo "FAIL"
	exit 1
fi


