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
web     ... is no longer here. Look at the 'cldr-apps' project parallel to this one.
-----------------

The tools may use ICU4J code for testing, but should use none of the data in ICU4J. 
We'll be using the ICU4J test framework also (we looked at JUnit, but it would be 
really clumsy for the ways in which we'd have to test).


To Build and Run the Tools:

1. Download and install a Java SDK with version number greater than or equal to 1.5 from
http://java.sun.com

2. Download and install the Ant build system with version number greater than or equal to 1.6 from
http://ant.apache.org

3. Build the tools with the following command:

   ant clean all
   
For a list of build targets use the following command:

   ant -projecthelp

4. Run the tool you are interested in, e.g:

   java -cp <dir>/utilities.jar;<dir>/icu4j.jar;<dir>/cldr.jar com.ibm.icu.dev.tool.cldr.LDML2ICUConverter 
   -s <dir>/cldr/common/main/ -d . -p <dir>/cldr/icu/main  zh_TW.xml 

   or

   java -cp "<dir>/utilities.jar;<dir>/icu4j.jar;<dir>/cldr.jar;<dir>/xml-apis.jar; <dir>/xercesImpl.jar;<dir>/xalan.jar"
   com.ibm.icu.dev.tool.cldr.LDML2ICUConverter  -s <dir>/cldr/common/main/ -d . -p <dir>/cldr/icu/main  zh_TW.xml 

   Note: When you run any of the tools, you probably want to set up a DTD cache; that will speed
   things up. To specify a local DTD cache directory, pass the location using system property
   CLDR_DTD_CACHE. The cache directory should exist before running the tools. Please clear out the 
   directory periodically so that the latest DTDs are downloaded to the machine.

   For example,

   java -DCLDR_DTD_CACHE=C:\cldrcashe\ -cp <cldr tool classpath> <cldr tool class>


CLDR Tool Development on Eclipse IDE:

   Eclipse project files are available for CLDR Tools development.
   To set up the environment on Eclipse IDE, see the link:
   http://sites.google.com/site/cldr/development/building-cldr-tools/cldr-java-tool-development-environment-on-eclipse-ide

Survey Tool:

  If you are building the survey tool, see org/unicode/cldr/web/data/readme.txt
