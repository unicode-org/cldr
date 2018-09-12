About the Survey Tool and the org.unicode.cldr.web.* classes
Steven R. Loomis srl@icu-project.org.    Updated 2008 Jan 25
Copyright 2007-2010 IBM and others. All Rights Reserved
------------------------------------------------------------

Note: this document is somewhat out of date. see 
	http://cldr.unicode.org/development/running-survey-tool

for the latest instructions.

--- (old information below) ----



-1. ABOUT THIS DOCUMENT:
  This document is directed towards the building, compilation
and basic installation of the Survey Tool.  Please see 'stops.html'
in this directory for the operation of survey tool.

  As of January 2008, (before CLDR 1.6 opens), I think this 
documentation is more or less accurate. It hasn't been tried from start
to finish on the current code base.


0. Prerequisite: General CLDR Tools issues
  See the readme at cldr/tools/java/readme.txt

1. Survey Tool Requirements:

* Java 1.5 - 
* Ant 1.7
* Tomcat 6.0.8+  http://tomcat.apache.org 
  - Download and install
  -  place catalina-ant.jar into Ant's lib/ directory
  MD: Read RUNNING.txt for instructions on installing.
  MD: catalina-ant.jar is in apache-tomcat-XXX/server/lib/
  MD: In eclipse, the Ant lib directory is in eclipse/plugins/org.appache.ant_XXX/lib
     Note: doesn't use anything Tomcat-specific, I'm just not familiar with other environments.
* All other requirements are now included.
* Database (If not Apache Derby, which is included, also tested with MySQL)

2. <obviated>

3. Setting up your build environment

 - copy build-sample.properties to build.properties
 - edit build.properties
  - set CATALINA_HOME to your Tomcat install directory
  - edit others as needed

4. Test Building of SurveyTool
 - you can use "ant web" to test compilation  and "ant check" to run unit tests

5. Build cldr-apps.war
 - "ant war" will produce cldr-apps.war 

6. Deploy cldr-apps.war to your server
   http://127.0.0.1:8080/manager/html
   "WAR file to deploy" (2nd item under deploy).. find and feed it the cldr-apps.war next to build.xml
    ( you could undeploy and then post another one to update, or see "ant deploy", below. )

7. setting up your ST deployment environment
 i.  Visit http://127.0.0.1:8080/cldr-apps/survey
 ii. You will see a message about a default cldr directory being created
 iii.Go to http://127.0.0.1:8080/manager/html and Stop the cldr-apps

 - visit the "cldr" directory inside your tomcat directory:
    Install a directory inside cldr/ : 
	common/   (symlink or copy of CLDR common data - or see note about CLDR_COMMON below.
                    On Windows, this may not be a shortcut.)

  - cldr.properties is the config file for the survey tool

       # cldr.properties should contain at least the Vetting Access Password:
       CLDR_VAP=somepassword 

       # if the common data is not in TOMCAT/cldr/common you can set the following
       ##CLDR_COMMON=/work/cldr/common

       # for mail uncomment these:
       ##CLDR_SMTP=  smtp_host.example.com
       ##CLDR_FROM=  your@email.com

    After changing cldr.properties you must Stop the survey tool if it was already running.
   
8. test
    http://127.0.0.1:8080/cldr-apps/survey
     login as "admin@" with password "somepassword"  ( the CLDR_VAP, above )

9. problems

    look under TOMCAT/logs  for log files. 


**********************
**********************
**********************
**********************


ADVANCED

10.  "ant deploy", etc will deploy to a tomcat server if configured correctly.  see build.xml

11. setting up mail: 

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

