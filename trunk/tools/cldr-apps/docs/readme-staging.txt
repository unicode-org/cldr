-- $Id$
-- 2008 March 13 - steven r. loomis
--

This document describes the staging area for the Survey Tool.
The normal update (new code) is described under "NORMAL UPDATE", below.

The generic SurveyTool readme file and ops pages are at:  
   http://unicode.org/cldr/data/tools/java/org/unicode/cldr/web/data/readme.txt
   http://unicode.org/cldr/data/tools/java/org/unicode/cldr/web/data/stops.html
This file is located at:
   http://unicode.org/cldr/data/tools/java/org/unicode/cldr/web/data/readme.txt

  TOC:   Directories and Pieces.  Building.   
         Updating. Testing. Deploying to unicode.org.

-
DIRECTORIES AND PIECES

 apache-tomcat-#.##.##/     the server root for the tomcat install
 tomcat                    a symlink to the above dir

 src/cldr                  a complete CLDR CVS workspace, including:
 src/cldr/common           The common directory used by kwanyin's surveytool
 src/cldr/tools/java       The Java work area
 
 src/tools-cldr            symlink to src/cldr/tools/java

 src/icu4j                 Some "unicode" library, whatever that is
                            (checked out via SVN over anonymous HTTP)
 
 ~/.cldrrc                 Glue: sets up some variables that ant will use
   export ICU4J_HOME=${HOME}/src/icu4j
   export ICU4J_CLASSES=${ICU4J_HOME}/classes
   export CATALINA_HOME=${HOME}/tomcat
   export UTILITIES_JAR=${ICU4J_HOME}/utilities.jar
   export ICU4J_JAR=${ICU4J_HOME}/icu4j.jar
   export XML_APIS_JAR=${CATALINA_HOME}/lib/xalan.jar

 ~/.bash_profile           Prints out the notice message, and loads .cldrrc

-
INITIAL BUILDING (and setup from scratch) - FYI Only.

1. Download and unpack tomcat into the account root  - create symlink "tomcat" - create admin user(s) - boot server
2. Install needed jars (see below) into tomcat/lib
3. Checkout and build icu4j in src/icu4j - "ant jar cldrUtil"
4. Checkout all of CLDR into src/cldr and setup symlinks
5. Build CLDR "ant all web war "
6. Create build.properties file in src/tools-cldr with "username=xxxx" and
"password=yyyyy"  to match tomcat/conf/tomcat-users.xml admin user
7. "ant deploy" should install /cldr-apps module on server.   Now you can
use "ant undeploy ; ant deploy" or, "ant redeploy" for short.


-
UPDATING

Updating Libraries and Infrastructure
1. If tomcat is updated make sure to preserve any configuration, such as
passwords. Other jars such as derby, etc can be updated as needed.
2. ICU should not need to be updated, normally. But, it can be updated with
svn, and re-run "ant jar cldrUtil" to build icu4j.jar and utilities.jar
3. "sh update-libs-on-unicode.sh" will post new jars - may need to restart server then.
  The current list of jars updated is: JARS="icu4j.jar utilities.jar
  jdom.jar derby.jar mysql-connector-java-5.0.jar mail.jar activation.jar "


* ================ NORMAL UPDATE ================
*. The utility "restart-st-on-unicode.sh" will restart survey tool on
unicode.org. Also, any updates to survey tool code will restart it.
Use the admin panel to make sure nobody is using it when you do an update.

Updating Survey Tool with New Code
1. Run the utility "update-code.sh" - it will do the following:
   i. CVS update of src/cldr/common - to pick up the latest locale files
        (manually check for conflicts or errors)
   ii. CVS update of src/cldr/tools/java - to get the latest code
        (manually check for conflicts or errors)
   iii. Build and Redeploy to local server.  
         (Tomcat must be running.. "cd tomcat/bin ; sh startup.sh" )
        
2. Scroll up to see if there were any CVS errors.
  Usually, you can just rm the offending file and run cvs update again,
  if it was just a simple conflict

3. Scroll down a bit further to see if there were any build errors. You
can run "ant " to build core CLDR, "ant web" to build the web stuff,
"ant war" to build cldr-apps.war or "ant redeploy" (or deploy/undeploy as
needed) to update local server. In rare cases, "ant clean" might be needed.

4. Now, login to the staging server, and test it out. You might watch the
console to make sure everything is happy.

5. Now, you are ready to post to unicode.org.   Make sure someone isn't
already logged in and working on metazone 499 of 500 before you kick them off.

    You can use the "update special message" section of the survey tool to
warn users of impending restart. For example, you could set a message,
"We need to update, please finish and log off before the countdown finishes."
Then, set the timer to, say, 3600 seconds (60 minutes). You will need to
refresh your own page to see the countdown in progress. 

  DO login to unicode.org and run "tail -f tomcat/logs/catalina.out" to
make sure nothing blows up.

 To update, run the shell script: "update-st.sh" - it will sync the code,
and soon enough, the survey tool should reload. You can help it by:
    a.) loading a ST web page (note, you may get partial contents if ST
	reloads in the middle of a page load)
    b.) or, you can actually restart the server.. running the
	restart-st-on-unicode.sh will do this.

