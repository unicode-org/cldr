<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
        <% if(org.unicode.cldr.web.SurveyMain.isUnofficial()) {
        	final String CLDR_TESTPW = org.unicode.cldr.util.CLDRConfig.getInstance().getProperty("CLDR_TESTPW", "");
			if(CLDR_TESTPW !=null && CLDR_TESTPW.length()>0) { %>
        	<div class='infobox' style='display: block'>
        	<hr>
        	<i>This is an unofficial SurveyTool.<br> Enter the value of CLDR_TESTPW to create a test user:</i>
        	<form action='<%= request.getContextPath() %>/createAndLogin.jsp' method='GET'>
        		<label>CLDR_TESTPW:<input name='vap' /></label>
        		<input type='submit' />
        	</form>    
        	</div>    	
        	<% } else { %>
		<%  	} 
			} else {  %>
			   <!-- <a href='http://cldr.unicode.org/index/survey-tool/accounts'>I need a new account.</a> -->
		<%  } %>
