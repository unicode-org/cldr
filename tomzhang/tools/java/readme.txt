CLDR Tools ReadMe
-----------------

The tools folder will contain tools, tests, and utilities for dealing with CLDR data.
The code is very preliminary, so don't expect stability from the APIs (or documentation!),
since we still have to work out how we want to do the architecture.

See: http://cldr.unicode.org/development/new-cldr-developers

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

   ant clean all jar

For a list of build targets use the following command:

   ant -projecthelp

4. For running automated and console tests, you will want to create a 'build.properties' file.

    If you checked out CLDR as one directory (i.e. there is a ../../common relative to this readme),
    then create build.properties containing:

              CLDR_DIR=../..

    Otherwise, if inside of an eclipse workspace (i.e. there is a ../common relative to this readme),
    this may work for you:

              CLDR_DIR=..

5. Run the tool you are interested in, e.g:

   java -jar cldr.jar com.ibm.icu.dev.tool.cldr.LDML2ICUConverter  -s <dir>/cldr/common/main/ -d . -p <dir>/cldr/icu/main  zh_TW.xml

CLDR Tool Development on Eclipse IDE

   Eclipse project files are available for CLDR Tools development.
   To set up the environment on Eclipse IDE, see the link:

   http://cldr.unicode.org/development/new-cldr-developers
