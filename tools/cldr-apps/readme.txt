About the Survey Tool and the org.unicode.cldr.web.* classes
Steven R. Loomis srl@icu-project.org.    Updated 2019 Oct 29
Copyright 1991-2019 Unicode, Inc.
All Rights Reserved. Terms of use: http://www.unicode.org/copyright.html
------------------------------------------------------------

Note: this document is somewhat out of date. see 
	http://cldr.unicode.org/development/running-survey-tool
for the latest instructions, other than maven instructions below:

** BUILDING WITH MAVEN

0. You will need maven (mvn), probably version 3.

1. build CLDR: go to ../tools and run 'ant jar'

2. Import CLDR (and ICU) jars: run 'install-cldr-jars.sh'.

(You will need to repeat steps 1 and 2 every time CLDR code changes,
until CLDR itself is in Maven.)

3. 'mvn package' will now run tests, and output 'target/cldr-apps.war'


Yup, that's it.

UPDATING

Note that the 'pom.xml' has dependencies such as Guava, etc.
If CLDR uses a different guava version, simply update the pom.xml to
specify a different version of Guava.


OLD STUFF
==============
--- [old stuff below]

Stuff to add to catalina.policy:

   // for JavaMail / Activation.  Used by CLDR SurveyTool
    grant codeBase "file:${catalina.home}/shared/lib/activation.jar" {
       permission java.io.FilePermission "file:${catalina.home}/shared/lib/mail.jar","read";
    };



12. fun startup options

Tomcat startup:
I put this in startup.sh:
export CATALINA_OPTS=-Dderby.system.home=/xsrl/derby\ -Dderby.storage.fileSyncTransactionLog=true\ -Djava.util.logging.FileHandler.formatter=java.util.logging.SimpleFormatter
  derby.system.home: a home dir for derby and its logs and properties file (see below)
  derby.storage.**: workaround for a known MacOSX JDK 1.5 bug
  java.util.logging*:  use a simplified log file format instead of XML.

13. SQL modifications:
------
* the 'raw sql' panel is quite powerful/dangerous.  Derby docs: http://db.apache.org/derby/docs/
also: http://db.apache.org/derby/faq.html
Here are some things that can be done:
 - schema updates.   for example,   ALTER TABLE CLDR_USERS ALTER locales SET DATA TYPE VARCHAR(1024)
   to update an existing database. You'll need to check UserDB for this one because it is on the user db side.
By default, derby timesout fairly quickly. I added the following to derby.properties ( in the derby home dir):

derby.locks.waitTimeout=240
derby.locks.deadlockTimeout=120

Most normal user operations shouldn't need these timeouts.
Symptom: "could not aquire lock in the specified time"

