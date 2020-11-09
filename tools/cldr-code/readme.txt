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
json    Tools for creating JSON data from CLDR
tool	Tools for manipulating CLDR files
util	Utilities for handling CLDR files
ooo 	OpenOffice.org tools for :
			- Converting OpenOffice.org format to LDML
			- CLDR data to OpenOffice.org format
			- Comparing OpenOffice.org data
-----------------

The tools may use ICU4J code for testing, but should use none of the data in ICU4J.
We'll be using the ICU4J test framework also (we looked at JUnit, but it would be
really clumsy for the ways in which we'd have to test).


TO BUILD AND RUN THE TOOLS:

  See http://cldr.unicode.org/tools

ADVANCED USAGE:

1.  Build the tools with the following command:

   ant clean all jar

2. For a list of build targets use the following command:

   ant -projecthelp

3. For running automated and console tests, you will want to create a 'build.properties' file.

    If you checked out CLDR as one directory (i.e. there is a ../../common relative to this readme),
    then create build.properties containing:

              CLDR_DIR=../..

    Otherwise, if inside of an eclipse workspace (i.e. there is a ../common relative to this readme),
    this may work for you:

              CLDR_DIR=..

CLDR TOOL DEVELOPMENT ON ECLIPSE IDE

   Eclipse project files are available for CLDR Tools development.
   To set up the environment on Eclipse IDE, see the link:

   http://cldr.unicode.org/development/new-cldr-developers
