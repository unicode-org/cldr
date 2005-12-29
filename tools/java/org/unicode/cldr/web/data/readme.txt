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

TO DO MAIL:
Stuff to put in shared/lib:
 mail.jar,  activation.jar  (from javamail and java beans activation framework)

Stuff to add to catalina.policy:
    grant codeBase "file:${catalina.home}/shared/lib/activation.jar" {
       permission java.io.FilePermission "file:${catalina.home}/shared/lib/mail.jar","read";
    };


SETUP:
under your tomcat home directory (sibling to 'bin') create a 'cldr' directory. Inside it, place:
	common   (symlink or copy of CLDR common data)
	vetdata   (vetting dir )
	vetdata/cldrvet.txt  (vetting registry - can be an empty file)
	vetweb    (output dir for web pages. can be an empty dir)
	cldr.properties  (see below)

(If not on tomcat, see surveytool.xml or the web.xml in the WAR.) 
(Note on Windows: you may have to modify the owner and permissions of cldr to allow tomcat to create dirs and files there.)


# cldr.properties should contain at least the Vetting Access Password:
CLDR_VAP=password 

for mail (above):
CLDR_SMTP=  smtp_host.example.com
CLDR_FROM=  your@email.com

