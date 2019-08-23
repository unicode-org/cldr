#!/bin/sh

set -x

# after David Beaumont

mvn install:install-file \
   -DgroupId=org.unicode.cldr \
   -DartifactId=cldr-api \
   -Dversion=0.1-SNAPSHOT \
   -Dpackaging=jar \
   -DgeneratePom=true \
   -Dfile=../java/cldr.jar

mvn install:install-file \
   -DgroupId=com.ibm.icu \
   -DartifactId=icu-utilities \
   -Dversion=0.1-SNAPSHOT \
   -Dpackaging=jar \
   -DgeneratePom=true \
   -Dfile=../java/libs/utilities.jar
