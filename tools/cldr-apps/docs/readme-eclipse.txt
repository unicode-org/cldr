setting up in eclipse:

- get to build
- get about.jsp to RUN
    You can check the ICU and JVM versions here..
- get index.jsp to run - click Survey Tool

- you will get a message that CLDR_COMMON is not a directory. But at the top of the page, it says:

   Welcome to SurveyTool@mobot. Please edit /home/srl/src/apache-tomcat-6.0.26/cldr/cldr.properties to change CLDR_HEADER (to change this message), or comment it out entirely. 

  This means /home/srl/src/apache-tomcat-6.0.26/cldr is your cldr-apps work area.
  
  Edit /home/srl/src/apache-tomcat-6.0.26/cldr/cldr.properties, and at least 
    - set:
        	CLDR_COMMON=<path to cldr/common>
    - comment out: 
          CLDR_MESSAGE
    - Note:
           CLDR_VAP is your 'admin@' password. 
  	      
  (many more fun things can be set.)
 
 - restart the server
 - try index.jsp again
 - click Login 
      user:  'admin@'
      pass:  <the CLDR_VAP generated password>
      