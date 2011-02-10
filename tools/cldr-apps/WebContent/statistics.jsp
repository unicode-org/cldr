<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="utf-8"%><%@ page import="org.unicode.cldr.web.*, java.sql.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
<script type='text/javascript' src='<%= request.getContextPath() %>/js/raphael.js' ></script>
<script type='text/javascript' src='<%= request.getContextPath() %>/js/g.raphael.js' ></script>
<script type='text/javascript' src='<%= request.getContextPath() %>/js/g.line.js' ></script>
<script type='text/javascript' src='<%= request.getContextPath() %>/js/g.bar.js' ></script>
</head>
<body>


<%
	DBUtils dbUtils = DBUtils.getInstance();
	Connection conn = dbUtils.getDBConnection(null);
	if(conn==null) {
		throw new InternalError("Can't open DB connection. Note, you must use a new JNDI connection for this to work.");
	}
	try {
	
%>
	<h1>SurveyTool Statistics.</h1>

	Total Data Rows Submitted: <%= dbUtils.sqlQuery(conn,"select count(*) from cldr_data where submitter is not null") %> <br/>
	Total Vetting Submitted: <%= dbUtils.sqlQuery(conn,"select count(*) from cldr_vet where submitter is not null") %> <br/>

	<%
		String[][] submits = dbUtils.sqlQueryArrayArray(conn,"select distinct locale as loc,count(*) as count from cldr_data where submitter is not null order by count desc");
	%>

	<h2>Top Locales with Submitted Data</h2>
	<table style='border: 1px solid black; cell-padding: 3px;'> 
		<tr><th>Locale</th><th>Count</th>
		<% for(String[] r:submits) { %>
			<tr><th style='background-color: #ddd;'><%= r[0] %></th><td><%= r[1] %></td></tr>
		<% } %>
	</table>


<%
	} finally {
		if(conn!=null) {
			DBUtils.closeDBConnection(conn);
		}
	}
%>

<!-- 
<i>Graphs by <a href='http://g.raphaeljs.com/'>gRaphaël</a></i>
-->

<hr>
<a href="<%=  request.getContextPath() %>/survey">Return to the SurveyTool</a>

</body>
</html>