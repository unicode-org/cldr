<%@ page language="java" contentType="text/html; charset=UTF-8"
	import='java.sql.Connection'
    pageEncoding="UTF-8"%><%@ include file="/WEB-INF/jspf/session.jspf" %>
<% if(cs.user.userlevel > UserRegistry.TC) { response.sendRedirect(surveyUrl); return; } %>
<html>
	<head>
		<title>Unicode | Submitter Emails</title>
		<link rel='stylesheet' type='text/css' href='./surveytool.css' />
	</head>
    
    <body>
<%

Connection conn = null;
	try {
		conn = cs.sm.dbUtils.getDBConnection();

		String l[][] = cs.sm.dbUtils.sqlQueryArrayArray(conn,"select email from cldr_users where exists (select * from "+DBUtils.Table.VOTE_VALUE+" where "+DBUtils.Table.VOTE_VALUE+".submitter = cldr_users.id) and (cldr_users.userlevel < "+UserRegistry.LOCKED+") order by email");
%>
<a href="<%=request.getContextPath()%>/survey?do=list">Return to the SurveyTool <img src='STLogo.png' style='float:right;' /></a>
<hr/>
<h2><%= l.length %> Users who have Voted This  <%=  cs.sm.getNewVersion() %> Release</h2>

<textarea rows="10" cols="50">
<%		
		for(String r[] : l) {
			if(r[0].equals("admin@")) continue;
			%><%= r[0] %>,<%
		}
		
	} finally {
		DBUtils.close(conn);
	}

%>
</textarea>

</body>
</html>
