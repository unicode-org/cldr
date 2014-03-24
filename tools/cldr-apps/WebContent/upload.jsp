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
if (doRedirectToSurvey) {
	response.sendRedirect(request.getContextPath() + "/survey");
	return;
}
%>

<div class='helpHtml'>
	Upload an XML file.
	<br>
	For help, see: <a target='CLDR-ST-DOCS' href='http://cldr.unicode.org/index/survey-tool/upload'>Using Bulk Upload</a> 
</div>

<h3>Upload files... |  <%= cs.user.name %> </h3>
<% 

String email = request.getParameter("email");

if(SurveyMain.isUnofficial() && email==null) {
	email = cs.user.email;
	%>	
		    <div class='unofficial' title='Not an official SurveyTool' >
		        <%= WebContext.iconHtml(request,"warn","Unofficial Site") %>Unofficial
		    </div>
	<%
}

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
<label>
    Enter the account name (a valid email address) which will be voting. 
     
         Use your own address, <address><%= cs.user.email %></address> to vote as yourself.
    <input  name="email" value="<%= email %>" />
    
</label>
<label>Upload a single XML file
<!-- or a ZIP file containing multiple XML files -->
:<input name="file" type="file" size="40"/></label><br/>
<!--  <input type="submit" name="bulk" value="Upload as Bulk Data (multiple vetters for which I am TC)"/><br/> -->
<input type="submit" name="submit" value="Upload as my Submission/Vetting choices"/><br/>
</form>

<% }  %>
</body>
</html>