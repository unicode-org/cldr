#!/bin/sh
# Copyright (C) 2004, International Business Machines Corporation
#  and others.  All Rights Reserved.
#
# Run this in locale/tools and it will create an InterimVettingChart in ../vetting/main

write_index()
{
    TREE=$1
    OUTF=${TREE}/index.html
    
    echo "INFO: Writing index file " ${OUTF}
    echo "<h1>CLDR 1.2a - Drafts and Alts</h1>" > ${OUTF}
    date >> ${OUTF}
    echo '<br/>' >> ${OUTF}
    echo '<h3><a href="http://www.jtcsv.com/cgibin/cldrwiki.pl?InterimVettingCharts">What is this? Click here!</a></h3>' >> ${OUTF}
    echo '<br/>' >> ${OUTF}
    echo "<ul>" >> ${OUTF}
    for file in `cd ${TREE} ; ls *.html | fgrep -v index`;
      do
      fileb=`basename ${file} .html`
      echo "  <li><a href=\"${file}\">${fileb}</a></li>" >> ${OUTF}
    done
    
    echo "</ul>" >> ${OUTF}
    echo "<hr>" >> ${OUTF}
    echo '<h3><a href="http://www.jtcsv.com/cgibin/cldrwiki.pl?InterimVettingCharts">What is this? Click here!</a></h3>' >> ${OUTF}
    echo '<br/>' >> ${OUTF}
    echo '<i>Interim page - subject to change.</i>   ' >> ${OUTF}
    date >> ${OUTF}    
}

compare_tree()
{
    TREE=$1
    mv ${TREE}.backup0 ${TREE}.backup1
    mv ${TREE} ${TREE}.backup1
    mkdir ${TREE}
    for file in `cd ../common/${TREE} ; ls *.xml | fgrep -v supplementalData`;
      do
#      echo ${TREE} _ ${file}
      LDMLComparator -d ./${TREE} -vetting -common:gold ../common/${TREE}/${file}
    done
}

if [ ! -d ../common/main ];
then
    echo `basename $0` ":## Error: run me from locale/tools."
    exit 1
fi

mkdir ../vetting
cd ../vetting
echo "INFO: Starting in ../vetting"
compare_tree main
write_index main
echo "INFO: Done with ../vetting"
