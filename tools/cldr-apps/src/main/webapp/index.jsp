<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/2001/REC-xhtml11-20010531/DTD/xhtml11-flat.dtd">
<%@page import="org.unicode.cldr.test.HelpMessages"%>
<%@ page contentType="text/html; charset=UTF-8" import="org.unicode.cldr.web.*" %>
<%@ page import="javax.servlet.http.Cookie" %>
<html>
	<head>
<meta name="google-site-verification" content="srvwuSyUz9Z1IqUdRzS9fKqc928itVA9OeLxh60vnDM" />
		<title>CLDR Web Applications</title>
<!--        <link rel="stylesheet" type="text/css"
        href="http://www.unicode.org/webscripts/standard_styles.css"> -->
		<link rel='stylesheet' type='text/css' href='./surveytool.css' />
	</head>
	<body style='padding: 1em'>
	<p>
		<img width=0 height=0 src='loader.gif'></img><!--  to preload this gif -->
	</p>
	<% if(request.getParameter("logout")!=null) {
        WebContext.logout(request,response);
    %>
    <p>
    	<i>
    		You have been logged out. Thank you for using the Survey
    		Tool.
    	</i>
    </p>
    <% } else if(!SurveyMain.isMaintenance() && !SurveyMain.isBusted()) {
		// In almost all cases, this page is invisible, just send users to /v
		response.sendRedirect("v");
	} else if(request.getParameter("stdown")!=null && !SurveyMain.isSetup) { %>
        <div class='ferrbox'>
            <i><%= WebContext.iconHtml(request, "warn", "ST is down") %>The page you tried to access can't be loaded until the <a href='survey/'>Survey Tool</a> has been started. </i>
        </div>
    <% } %>

				<img src="STLogo.png" align="right" border="0" title="[logo]" alt="[logo]" />

		<h1>CLDR Web Applications</h1>
		<ul>
			<li><strong><a href="survey/">CLDR Survey Tool
			</a></strong> - <a href='<%= SurveyMain.GENERAL_HELP_URL %>'>(Instructions)</a>
			<% if(SurveyMain.isMaintenance()) { %>
			  <span style='color: red;'>setup mode</span>
			<% } else if(SurveyMain.isBusted()) {		%>
			  <span style='color: red;'>offline</span>
            <% } else if(!SurveyMain.isSetup) { %>
              <span style='color: goldenrod;'>starting..</span>
            <% } %>
			<br />
			         </li>
		</ul>

        <hr />
        <p><a href="http://www.unicode.org">Unicode</a> | <a href="http://www.unicode.org/cldr">CLDR</a></p>
        <span style='font-size: 60%;'>Version: <%= SurveyMain.getCurrev(true) %></span>
        <div style='float: right; font-size: 60%;'><span class='notselected'>valid <a href='http://jigsaw.w3.org/css-validator/check/referer'>css</a>,
            <a href='http://validator.w3.org/check?uri=referer'>xhtml 1.1</a></span></div>

	</body>
</html>
