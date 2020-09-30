<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="utf-8"%><%@ page import="org.unicode.cldr.web.*"%><%@ page import="java.sql.*" %><%@ page import="com.ibm.icu.util.ULocale" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%
	String title = "?";
	String user = request.getParameter("user");
	String s = request.getParameter("s");

	if (user==null) {
		title = "Invalid";
		response.sendRedirect("survey");
		return;
	} else {
		title = "Submitted data for user " + user;
	}
	
%>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>SurveyTool My Votes | <%=title%></title>
<link rel='stylesheet' type='text/css' href='./surveytool.css' />
</head>
<body style='padding: 1em;'>
<%@ include file="/WEB-INF/tmpl/stnotices.jspf" %>
<span id="visitors"></span>
<hr>

<%@ include file="/WEB-INF/tmpl/ajax_status.jsp" %>

<a href="<%=request.getContextPath()%>/survey">Return to the SurveyTool <img src='STLogo.png' style='float:right;' /></a>
<!-- 	| <a class='notselected' href="statistics-org.jsp">by Organization</a> -->

<br>
<i>All items shown are for the current release, CLDR <%= SurveyMain.getNewVersion() %>. Votes before <%= SurveyMain.getVotesAfterDate() %> are not shown.</i>
<hr>
<h3>The most recently submitted items for user <%= user %></h3>

<div id='submitItems'>
</div>
...

<script>
showRecent('submitItems',null,'<%= user %>');
</script>

<hr>
<h3>All active locales for user <%= user %></h3>

<div id='allMyItems'>
</div>
...

<script>
showAllItems('allMyItems','<%= user %>');
</script>

<%
if(s!=null) { %>
	<hr>
<form method='POST' action='DataExport.jsp'>
	<input type='hidden' name='s' value='<%= s %>'>
	<input type='hidden' name='user' value='<%= user %>'>
	<input type='hidden' name='do' value='mydata'>
	<input type='submit' class='csvDownload' value='Download all of my votes as .csv'>
</form>
	

<% } %>
<hr>
<a href="<%=request.getContextPath()%>/survey">Return to the SurveyTool</a>

</body>
</html>
