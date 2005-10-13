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

Each one of these should have a readme.txt
[none has a readme, except ooo]

When you run any of the tools, you probably want to set up a DTD cache; that will speed things up.
Include the following environment variable to do that.
[pass to command line of java?]
	-DCLDR_DTD_CACHE=C:\cldrcache\
Note: The cache directory should exist before running the tools. Please clear out the 
directory periodically so that the latest DTDs are downloaded to the machine.

-----------------

The tools may use ICU4J code for testing, but should use none of the data in ICU4J. 
[LDML converter uses UnicodeSet which is not in cldrUtil, why have separate utilities.jar?]
We'll be using the ICU4J test framework also (we looked at JUnit, but it would be 
really clumsy for the ways in which we'd have to test).


To Run the Tools:

1. Download and install a JRE with version number greater than or equal to 1.4 from
http://java.sun.com
2. Download the following Jar files:
   ftp://www.unicode.org/Public/cldr/<release number>/utilities.jar
   ftp://www.unicode.org/Public/cldr/<release number>/icu4j.jar
   ftp://www.unicode.org/Public/cldr/<release number>/cldr.jar
[utilities.jar and icu4j.jar do not exist here]
3. Run the tool you are interested in, e.g:
java -cp <dir>/utilities.jar;<dir>/icu4j.jar;<dir>/cldr.jar com.ibm.icu.dev.tool.cldr.LDML2ICUConverter 
-s <dir>/cldr/common/main/ -d . -p <dir>/cldr/icu/main  zh_TW.xml 

To Build the Tools:

1. Download and install a Java SDK with version number greater than or equal to 1.4 from
http://java.sun.com
[only JDK 1.4.x work, JDK 1.5 does not include org.apache.foo]

2. Download and install the Ant build system with version number greate than or equal to 1.6 from
http://ant.apache.org
3. Download the following Jar files:
   ftp://www.unicode.org/Public/cldr/<release number>/utilities.jar
   ftp://www.unicode.org/Public/cldr/<release number>/icu4j.jar 
4. Set ICU4J_CLASSES environment variable to point to the above jar files.
   Windows: set ICU4J_CLASSES=<dir>/icu4j.jar;<dir>/utilities.jar
   Unix: export ICU4J_CLASSES=<dir>/icu4j.jar:<dir>/utilities.jar
5. Build the tools with the following command:
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
[note, currently requires JDK 1.4.x. 1.5 does not include org.apache.foo]

IMPORTANT:
If you are using Eclipse for building CLDR tools and ICU4J, make sure that you
do not make building of CLDR tools dependent on ICU4J project. In Java Perspective
open Package Explorer view > Select CLDR project > Right click > Properties > 
Java Build Path > Projects tab > Uncheck ICU4J. Now go to Libraries tab and click
the Add External Jars button and add the utilities.jar and icu4j.jar that you
have downloaded from the instructions above. If you do not do this, you may be breaking
others when you use classes not in utilities.jar or icu4j.jar and check in the files.
