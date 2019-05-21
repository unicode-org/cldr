// cldr-setup.js  -Copyright (C) 2014 IBM Corporation and Others. All Rights Reserved.


/**
 * @module cldr-setup.js - JavaScript stuff for cldr-setup.jsp
 */
 



function mysqlhelp(ee) {
	function stStopPropagation(e) {
		if(!e) {
			return false;
		} else if(e.stopPropagation) {
			return e.stopPropagation();
		} else if(e.cancelBubble) {
			return e.cancelBubble();
		} else {
			// hope for the best
			return false;
		}
	}
	
	function doHelp() {
		var values = {};

		// IVR-ish voice not included.
		var prompts = [
		               	{ 									               		
		               		prompt:"Hello! I'm going to try to help you setup the ST on MySQL. Just hit cancel to stop this process. First question: what is the MySQL driver name? You probably want the default!",
		               		value:"com.mysql.jdbc.Driver",
		               		name: "driver"
		               	},
		               	{
		               		prompt:"You're doing great. Now, what is the DataSource type? You probably want the default here also!",
		               		value:"javax.sql.DataSource",
		               		name: "type"
		               	},
		               	{
		               		prompt:"Last of the boring questions.. what is the DB URL scheme? Should be mysql for mysql..",
		               		value:"mysql",
		               		name: "scheme"
		               	},
		               	{
		               		prompt:"Excellent. Now, let's make it a little more personal. What is the host:port your database is located on?",
		               				value:"localhost:3306",
		               				name:"hostport"
		               	},
		               	{
		               		prompt:"Sounds good. What is the database name? If it doesn't exist, you might create it now!",
		               		value:"cldrdb",
		               		name:"database"
		               	},
		               	{
		               		prompt:"So, what user can access this database? You might create this user now also.",
		               		value:"surveytool",
		               		name:"user"
		               	},
		               	{
		               		prompt:"Great. We have to ask these things, what is the password for this user?",
		               		value:"hunter42",
		               		name:"password"
		               	}
		               ];
		for(var k=0;k<prompts.length;k++) {
			var promptstr = "Step "+(k+1)+" of " +(prompts.length)+": " + prompts[k].name+"\n\n"+prompts[k].prompt;
			var newValue = prompt(promptstr,prompts[k].value);
			if(newValue==null) return false;
			if(newValue === '') return false;
			values[prompts[k].name]=newValue;
		}

		var theText = '<Resource name="jdbc/SurveyTool"\n  auth="Container" type="'+values.type+'"\n  username="'+values.user+'"\n  ' +
					     'password="'+values.password+'"\n  driverClassName="'+values.driver+'"\n  '+
					     'url="jdbc:'+values.scheme+'://'+values.hostport+'/'+values.database+'"\n  '+
					     'maxActive="8" maxIdle="4" removeAbandoned="true" removeAbandonedTimeout="60" logAbandoned="true" defaultAutoCommit="false"\n  '+
					     'poolPreparedStatements="true" maxOpenPreparedStatements="150"\n/>';
		prompt('Okay.  copy this text, and paste it in the middle of your "context.xml" file in the tomcat server directory. On eclipse, you will find context.xml in the workspace under Servers. Then, restart the server and reload this page.',
				theText);
	}
	
	doHelp();
	
	return stStopPropagation(ee);
}
