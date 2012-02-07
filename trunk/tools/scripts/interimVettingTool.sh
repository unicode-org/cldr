#!/bin/sh
# Copyright (C) 2004, International Business Machines Corporation
#  and others.  All Rights Reserved.
#
# Run this in locale/tools and it will create an InterimVettingChart in ../vetting/main

# Prefix - will only show files which match the prefix.
MATCHIT=
#MATCHIT=ar
#MATCHIT=fi
COMMON=../../common

write_index()
{
    TREE=$1
    OUTF=${TREE}/index.html
    
    echo "INFO: Writing index file " ${OUTF}
    cat > ${OUTF} <<"EOF"
<html>
<head>
 <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
 <title>Draft/ALT for CLDR</title>
 <style>
         <!--
         table        { border-spacing: 0; border-collapse: collapse;  
                        border: 1px solid black }
         td, th       { border-spacing: 0; border-collapse: collapse;  color: black; 
                        vertical-align: top; border: 1px solid black }
         -->
     </style> 
 </head><body bgcolor="white">
<h1>CLDR 1.2a - Drafts and Alts</h1>
EOF

    date >> ${OUTF}
    echo '<br/>' >> ${OUTF}
    echo '<h3><a href="http://www.jtcsv.com/cgibin/cldrwiki.pl?InterimVettingCharts">What is this? Click here!</a></h3>' >> ${OUTF}
    echo '<br/>' >> ${OUTF}
    cat >> ${OUTF} <<EOF
<table border=1>
 <tr><th>Locale</th><th>Name</th><th># of changes</th><th>CVS</th></tr>
EOF

    cat ${TREE}/*.idx >> ${OUTF}
    rm ${TREE}/*.idx

    cat >> ${OUTF} <<EOF
</table>
<p>
EOF
    
    echo '<h3><a href="http://www.jtcsv.com/cgibin/cldrwiki.pl?InterimVettingCharts">What is this? Click here!</a></h3>' >> ${OUTF}
    echo '<br/>' >> ${OUTF}
    echo '<i>Interim page - subject to change.</i>   ' >> ${OUTF}
    date >> ${OUTF}    
    cat >> ${OUTF} <<EOF
</body>
</html>
EOF

}

backup_tree()
{
    TREE=$1
    rm -rf ${TREE}.backup1
    mv ${TREE}.backup0 ${TREE}.backup1
    mv ${TREE} ${TREE}.backup1
}

compare_tree()
{
    TREE=$1
    mkdir ${TREE}
    for file in `cd ${COMMON}/${TREE} ; ls ${MATCHIT}*.xml | fgrep -v supplementalData`;
      do
#      echo ${TREE} _ ${file}
      LDMLComparator -d ./${TREE} -vetting -common:gold ${COMMON}/${TREE}/${file}
    done
}

mkdir -p ../diff/vetting
cd ../diff/vetting
if [ ! -d ${COMMON}/main ];
then
    echo `basename $0` ":## Error: run me from locale/tools."
    exit 1
fi

echo "INFO: Starting in ../diff/vetting"

backup_tree main
compare_tree main
write_index main
echo "INFO: Done with ../diff/vetting"
