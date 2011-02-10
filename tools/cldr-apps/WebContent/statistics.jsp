<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="utf-8"%><%@ page import="org.unicode.cldr.web.*, java.sql.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
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

	Total New/Changed Submitted: <%= dbUtils.sqlQuery(conn,"select count(*) from cldr_data where submitter is not null") %> <br/>
	Total Votes Cast: <%= dbUtils.sqlQuery(conn,"select count(*) from cldr_vet where submitter is not null") %> <br/>

	<%
		String[][] submits = dbUtils.sqlQueryArrayArray(conn,"select  locale,count(*) from cldr_data  where submitter is not null group by locale");
	%>

	<h2>Top Locales with Submitted Data</h2>
	<table style='border: 1px solid black; cell-padding: 3px;'> 
		<tr><th>Locale</th><th>Count</th>
		<% for(String[] r:submits) { %>
			<tr><th style='background-color: #ddd;'><%= r[0] %></th><td><%= r[1] %></td></tr>
		<% } %>
	</table>
	
	<div id="holder-holder">
	        <div id="holder" style="width: 640px; height: 480px;"></div>
	</div>        
	
	<%
		int offh = 10;
		int offv = 10;
		int wid = 600;
		int hei=220;
	%>

<script type='text/javascript' src='js/raphael.js' ></script>
<script type='text/javascript' src='js/g.raphael.js' ></script>
<script type='text/javascript' src='js/g.line.js' ></script>
<script type='text/javascript' src='js/g.bar.js' ></script>
	<script type="text/javascript">
		var r = Raphael("holder");
		var barchart = r.g.barchart(<%= offh %>, <%= offv %>, <%= wid %>, <%= hei %>, [[ <%
		for(String[] r:submits) {
			%><%= r[1] %>, <%
		}
		%> ]]);
		<%
		int ii=0;
		int ea = wid/submits.length;
		for(String[] r:submits) {
			++ii;
			int h = ((ea)*(ii-1))+offh+(ea/2);
			int v=hei+offv;
			%>
			var t<%= ii %> = r.text(<%= h %>,<%= v %>, "<%= r[0] %>");
			var n<%= ii %> = r.text(<%= h %>,<%= v+20 %>, "<%= r[1] %>");
		<% } %>
	</script>
	

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