<%@ page language="java" contentType="text/html; charset=UTF-8"
import="org.unicode.cldr.web.*"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>SurveyTool File Upload</title>
		<link rel='stylesheet' type='text/css' href='./surveytool.css' />
</head>
<body>

<% request.setAttribute("BULK_STAGE", "upload"); %>
<%@include file="/WEB-INF/jspf/bulkinfo.jspf" %>

<a href="<%=request.getContextPath()%>/survey">Return to the SurveyTool <img src='STLogo.png' style='float:right;' /></a>
<hr/>


<%
String sid = request.getParameter("s");
// use a variable to store the state whether to redirect
boolean doRedirectToSurvey=false;
CookieSession cs=null;
if (sid == null || sid.isEmpty()) {
	// null SID -> redirect
	doRedirectToSurvey = true;
} else {
	// SID is not null or empty -> retrieve the session
	cs = CookieSession.retrieve(sid);
	doRedirectToSurvey = (CookieSession.sm == null ||
		cs == null ||
		cs.user == null);
}
if (cs != null && cs.user != null) {
	cs.userDidAction(); // mark user as not idle
}
if (doRedirectToSurvey) {
	response.sendRedirect(request.getContextPath() + "/survey");
	return;
}
%>

<div class='helpHtml'>
<p>
	Welcome to the CLDR Bulk Upload tool. This tool will let you upload an XML file and submit it as your vote, or as the vote
	of a user in your organization.
	<br/>
For help, see: <a target='CLDR-ST-DOCS' href='http://cldr.unicode.org/index/survey-tool/upload'>Using Bulk Upload</a> 
</p>
</div>
<h1>Bulk: 1. Upload files</h1>
<h3>Logged in as: <%= cs.user.name %> </h3>


<% 

String email = request.getParameter("email");

if(SurveyMain.isUnofficial() && email==null) {
	email = cs.user.email;
	%>	<%-- 
		    <div class='unofficial' title='Not an official SurveyTool' >
		        <%= WebContext.iconHtml(request,"warn","Unofficial Site") %>Unofficial
		    </div>
 --%>	<%
}
%>


<%

if(email==null) email="";

if(request.getParameter("emailbad")!=null) { %>
<div class='ferrbox'><%= WebContext.iconHtml(request, "stop", "error") %> Invalid address or access denied: <address><%= email %></address></div>
<% } else if(request.getParameter("filebad")!=null) { %>
<div class='ferrbox'><%= WebContext.iconHtml(request, "stop", "error") %> No file was uploaded, or a file error occured.</div>
<% } 

if(request.getParameter("s")==null) { %>
<div class='ferrbox'><%= WebContext.iconHtml(request, "stop", "error") %> Error, not logged in.</div>
<% } else { %>
<form method="POST" action="./check.jsp" enctype="multipart/form-data">
<input type="hidden" name="s" value="<%= request.getParameter("s") %>" />
<div class='helpHtml'>
	The account name must be a valid email address. 
              Use your own address, <i><%= cs.user.email %></i> to vote as yourself. <br/>
     See the help for information on formatting the XML file.
</div>
<div class='bulkInputForm'>
<div>
<label for='email'>
	Account that will be voting:
</label>	
    <input  name="email" size='40' value="<%= email %>" />
</div>
<div>
<label for='file'>XML file to upload:</label>
<!-- or a ZIP file containing multiple XML files -->
<input name="file" type="file" size="40"/>
</div>
</div>
<div class='helpHtml'>
	In this tool, the "NEXT" button is always located in the bottom-right corner of the screen. The navigation area at the bottom of the screen will show you where you are in the process.
</div>


<!--  <input type="submit" name="bulk" value="Upload as Bulk Data (multiple vetters for which I am TC)"/><br/> -->
<input class='bulkNextButton' type="submit" name="submit" value="NEXT: Upload as my Submission/Vetting choices"/><br/>
</form>

<% }  %>
</body>
</html>