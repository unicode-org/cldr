The tools folder will contain tools, tests, and utilities for dealing with CLDR data. The code is very preliminary, so don't expect stability from the APIs (or documentation!), since we still have to work out how we want to do the architecture.

The tools may use ICU4J code for testing, but should use none of the data in ICU4J. We'll be using the ICU4J test framework also (we looked at JUnit, but it would be really clumsy for the ways in which we'd have to test).

We will be constructing an Ant script for building everything, but for now a vanilla java build (such as in Eclipse) should work. Just remember the following:

- exclude any files in CVS folders

- for all programs, if you use the java VM settings -DCLDR_DTD_CACHE=C:\cldrcache\ then you will be much faster (since it doesn't have to load the dtd over the net for each file).

- use as a main class whichever you want, such as org.unicode.cldr.test.CLDRTest

- for tests, use parameters like -nothrow -verbose TestThatExemplarsContainAll (see the ICU4J TestFwk documentation for more information).
