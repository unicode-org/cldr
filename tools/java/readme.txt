CLDR Tools ReadMe
-----------------

The tools folder will contain tools, tests, and utilities for dealing with CLDR data. 
The code is very preliminary, so don't expect stability from the APIs (or documentation!), 
since we still have to work out how we want to do the architecture.

The tools may use ICU4J code for testing, but should use none of the data in ICU4J. 
We'll be using the ICU4J test framework also (we looked at JUnit, but it would be 
really clumsy for the ways in which we'd have to test).


For Running the tools:

1. Download and install a JRE with version number greater than or equal to 1.4 from
http://java.sun.com
2. Download the following Jar files:
   ftp://www.unicode.org/Public/cldr/<release number>/utilities.jar
   ftp://www.unicode.org/Public/cldr/<release number>/icu4j.jar
   ftp://www.unicode.org/Public/cldr/<release number>/cldr.jar
3. Run the tool you are interested in, e.g:
java -cp <dir>/utilities.jar;<dir>/icu4j.jar;<dir>/cldr.jar com.ibm.icu.dev.tool.cldr.LDML2ICUConverter 
-s <dir>/cldr/common/main/ -d . -p <dir>/cldr/icu/main  zh_TW.xml 

For Building the tools:
1. Download and install a Java SDK with version number greater than or equal to 1.4 from
http://java.sun.com
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

For building utilities.jar and icu4j.jar:
1. Install Java SDK and Ant build system as explained above.
2. Check out the release of ICU4J that you are interested in. 
   The instructions on checking out the source are at: 
   http://www-306.ibm.com/software/globalization/icu/repository.jsp
3. Build icu4j.jar with the following command:
   <dir>/bin/ant clean core jar
4. Build utilities.jar with the following command:
   <dir>/bin/ant cldrUtil
