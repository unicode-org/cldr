About the Survey Tool and the org.unicode.cldr.web.* classes
------------------------------------------------------------

Very briefly:
* requires Tomcat, Derby, and ICU. May require Xalan/Xerces jars to deploy.
** Note: doesn't use anything Tomcat-specific, I'm just not familiar with other environments.
* (see  ./jars/readme.txt for more about jar dependencies)
* ant targets:   "ant war" will build cldr-apps.war .  "ant deploy", etc will deploy to a tomcat server if configured correctly.


Stuff to put into your server's lib directory: (common/lib on tomcat):
 * xalan.jar, xercesImpl.jar,xml-apis.jar  (from http://xalan.apache.org )
 * derby.jar ( from http://derby.apache.org )
 * icu4j.jar, utilities.jar ( to speed up loading) 

SETUP:
under your tomcat home directory (sibling to 'bin') create a 'cldr' directory. Inside it, place:
	common   (symlink or copy of CLDR common data)
	vetdata   (vetting dir )
	vetdata/cldrvet.txt  (vetting registry - can be an empty file)
	vetweb    (output dir for web pages. can be an empty dir)
	cldr.properties  (see below)


# cldr.properties should contain at least the Vetting Access Password:
CLDR_VAP=password 