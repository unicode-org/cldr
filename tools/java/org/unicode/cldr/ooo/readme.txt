# OpenOffice.org Tools readme

# Last Updated : 10/10/2005

# There are 3 tools available in the org.unicode.cldr.ooo package :
# 1. ConvertOOLocale   : Converts OpenOffice.org data to CLDR's LDML format.
# 2. LDMLToOOConverter : Converts CLDR's LDML format to OpenOffice.org data.
# 3. OOComparator      : Compares OpenOffice.org XML data.

# A 4th tool from the org.unicode.cldr.util package is also useful : 
# 4. XMLValidator      : Validates XML data against the DTD

# This readme explains how to run these tools. The tools are built as part of cldr.jar.
# For instructions on how to build cldr.jar , see readme.txt in cldr/tools/java folder.

# On UNIX systems you can use this readme as a batch file for running the tools.

# For Help : run any program with no arguments


################# SET ENVIRONMENT ##########################

# Set the following environment variables :

#define the caching dir for caching the CLDR dtds
CER_DIR=/CER

#define the source dir of OpenOffice.org XML data
OO_DATA_IN_DIR=

#define the dir where CLDR is checked out to 
CLDR_DIR=

#define the destination dir where OpenOffice.org data is written in LDML format
DEST_DIR=./ldml

#define the destination dir where CLDR is written to OpenOffice.org format
OO_DATE_OUT_DIR=./ooo_out

#define proxy host and port
PROXY=
PROXY_PORT=8080

#define ICU4J jar
ICU4J_JAR=../../../../icu4j.jar

#define xalan.jar (needed if using jdk 1.5+)
XALAN_JAR=/usr/share/lib/xalan.jar

#define  cldr.jar
CLDR_JAR=../../../../cldr.jar



############## SET UP ##################################

# create dtd cache dir
mkdir $CER_DIR

#remove old cached DTDs to keep DTD up to date
rm  $CER_DIR/*.dtd

# create dirs for storing tools output
mkdir $DEST_DIR
mkdir $OO_DATE_OUT_DIR

cp $OO_DATA_IN_DIR/locale.dtd $OO_DATE_OUT_DIR/locale.dtd

export CLASSPATH=$ICU4J_JAR:$CLDR_JAR:$XALAN_JAR



############### EXAMPLES OF HOW TO RUN TOOLS ##################################

# Comment the commands below which you don't wish to run. 

# perform these 5 steps to migrate OpenOffice.org data to CLDR
echo "-----1. Converting Bulk OpenOffice.org data to LDML Format------"
java org.unicode.cldr.ooo.ConvertOOLocale -bulk $OO_DATA_IN_DIR -dest_dir $DEST_DIR

echo "-----2. Validating the LDML data created in the previous step -------"
java -Dhttp.proxyHost=$PROXY -Dhttp.proxyPort=$PROXY_PORT -DCLDR_DTD_CACHE=$CER_DIR org.unicode.cldr.util.XMLValidator $DEST_DIR/*.xml

echo "-----3. Converting Bulk LDML to OpenOffice.org format-----"
java -Dhttp.proxyHost=$PROXY -Dhttp.proxyPort=$PROXY_PORT -DCLDR_DTD_CACHE=$CER_DIR  org.unicode.cldr.ooo.LDMLToOOConverter -s $CLDR_DIR/common/supplemental -c $CLDR_DIR/common/main -t $OO_DATE_OUT_DIR -o $DEST_DIR 

echo "-----4. Validating the OpenOffice.org data  created in the previous step------"
java -Dhttp.proxyHost=$PROXY -Dhttp.proxyPort=$PROXY_PORT -DCLDR_DTD_CACHE=$CER_DIR org.unicode.cldr.util.XMLValidator $OO_DATE_OUT_DIR/*.xml

echo "------5. Comparing OpenOffice.org files-------"
java -Dhttp.proxyHost=$PROXY -Dhttp.proxyPort=$PROXY_PORT -DCLDR_DTD_CACHE=$CER_DIR org.unicode.cldr.ooo.OOComparator -bulk $OO_DATA_IN_DIR $OO_DATE_OUT_DIR



# perform this step to generate brand new locale from CLDR (will not contain OpenOffice.org specific elements)
#echo "-----Generating new OpenOffice.org locale from CLDR-----"
# java -Dhttp.proxyHost=$PROXY -Dhttp.proxyPort=$PROXY_PORT -DCLDR_DTD_CACHE=$CER_DIR  org.unicode.cldr.ooo.LDMLToOOConverter -s $CLDR_DIR/cldr/common/supplemental -l ga_IE -c $CLDR_DIR/cldr/common/main -t $OO_DATE_OUT_DIR 




