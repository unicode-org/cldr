CLDR Tools ReadMe
-----------------

The tools folder will contain tools, tests, and utilities for dealing with CLDR data. 
The code is very preliminary, so don't expect stability from the APIs (or documentation!), 
since we still have to work out how we want to do the architecture.

The directory structure is:
[./org/unicode/cldr..]
icu  	Tools for generating ICU-format data from CLDR
posix	Tools for generating POSIX-format data from CLDR
test	Test tools for CLDR
tool	Tools for manipulating CLDR files
util	Utilities for handling CLDR files
ooo 	OpenOffice.org tools for : 
			- Converting OpenOffice.org format to LDML
			- CLDR data to OpenOffice.org format 
			- Comparing OpenOffice.org data
web     Web-based code (Survey tool) - for information, see:  data/surveytool/readme.txt

When you run any of the tools, you probably want to set up a DTD cache; that will speed things up.
Include the following environment variable to do that.
[pass to command line of java?]
	-DCLDR_DTD_CACHE=C:\cldrcache\
Note: The cache directory should exist before running the tools. Please clear out the 
directory periodically so that the latest DTDs are downloaded to the machine.

-----------------

The tools may use ICU4J code for testing, but should use none of the data in ICU4J. 
We'll be using the ICU4J test framework also (we looked at JUnit, but it would be 
really clumsy for the ways in which we'd have to test).

To Run the Tools:

1. Download and install a JRE with version number greater than or equal to 1.4 from
http://java.sun.com
2. If using JDK 1.5 or later please download xalan.jar, xercesImpl.jar, xml-apis.jar  (from http://xalan.apache.org )
3. Download the following Jar files:
   ftp://www.unicode.org/Public/cldr/1.4/tools/cldr-utilities-1_4.jar
   ftp://www.unicode.org/Public/cldr/1.4/tools/cldr-1_4.jar
4. Download ICU4J 3.6 jar from:
    http://prdownloads.sourceforge.net/icu/icu4j-3_6.jar?download
5. Run the tool you are interested in, e.g:
java -cp <dir>/utilities.jar;<dir>/icu4j.jar;<dir>/cldr.jar com.ibm.icu.dev.tool.cldr.LDML2ICUConverter 
-s <dir>/cldr/common/main/ -d . -p <dir>/cldr/icu/main  zh_TW.xml 
or
java -cp "<dir>/utilities.jar;<dir>/icu4j.jar;<dir>/cldr.jar;<dir>/xml-apis.jar; <dir>/xercesImpl.jar;<dir>/xalan.jar"
com.ibm.icu.dev.tool.cldr.LDML2ICUConverter  -s <dir>/cldr/common/main/ -d . -p <dir>/cldr/icu/main  zh_TW.xml 

To Build the Tools:

1. Download and install a Java SDK with version number greater than or equal to 1.4 from
http://java.sun.com
2. If using JDK 1.5 or later please download xalan.jar, xercesImpl.jar,xml-apis.jar  (from http://xalan.apache.org )
3. Download and install the Ant build system with version number greater than or equal to 1.6 from
http://ant.apache.org
(Note: if you are using Eclipse, you can use Build Path>Add External Jars
 with eclipse/plugins/org.apache.ant_.../lib/ant.jar)
4. Download the following Jar files:
   ftp://www.unicode.org/Public/cldr/1.4/tools/cldr-utilities-1_4.jar
5. Download ICU4J 3.6 jar from:
    http://prdownloads.sourceforge.net/icu/icu4j-3_6.jar?download
    
6. Set the required environment variables
    o Unix
        export ICU4J_JAR=<dir>/icu4j.jar
        export UTILITIES_JAR=<dir>/utilities.jar
    o Windows
        set XML_APIS_JAR=<dir>xalan.jar    
        set ICU4J_JAR=<dir>\icu4j.jar
        set UTILITIES_JAR=<dir>\utilities.jar
    o Eclipse > Preferences > Ant > Runtime > Properties
        set env.XML_APIS_JAR=<dir>xalan.jar
        set env.ICU4J_JAR=<dir>\icu4j.jar
        set env.UTILITIES_JAR=<dir>\utilities.jar

7. Build the tools with the following command:
   <dir>/bin/ant clean all
   
For a list of build targets use the following command:
   <dir>/bin/ant -projecthelp

To Build utilities.jar and icu4j.jar:
1. Install Java SDK and Ant build system as explained above.
2. Check out the release of ICU4J that you are interested in. 
   The instructions on checking out the source are at: 
   http://www.ibm.com/software/globalization/icu/repository.jsp
3. Build icu4j.jar with the following command:
   <dir>/bin/ant clean core jar
4. Build utilities.jar  with the following command:
   <dir>/bin/ant cldrUtil
   
IMPORTANT:
o If you are using Eclipse for building CLDR tools and ICU4J, make sure that you
do not make building of CLDR tools dependent on ICU4J project. In Java Perspective
open Package Explorer view > Select CLDR project > Right click > Properties > 
Java Build Path > Projects tab > Uncheck ICU4J. Now go to Libraries tab and click
the Add External Jars button and add the utilities.jar and icu4j.jar that you
have downloaded from the instructions above. If you do not do this, you may be breaking
others when you use classes not in utilities.jar or icu4j.jar and check in the files.

o If you are trying to run org.unicode.cldr.test.TestTransforms then you need to make sure
   the classes directory of ICU4J with test classes built is in the class path of run configuration
   of Eclipse
   Run > Run > Select TestTransforms in left hand pane > Classpath tab > Select "user entries" >
   Check "Add Folders" radio button > click ok > navigate to ICU4J classes directory and select it >
   Click OK. 
   
   From the command line
   $ /java/bin/java -cp "classes;$ICU4J_JAR;$UTILITIES_JAR;\work\icu4j\classes" org.unicode.cldr.test.TestTransforms
   
   
Survey Tool
  If you are building the survey tool, see org/unicode/cldr/web/data/readme.txt
   