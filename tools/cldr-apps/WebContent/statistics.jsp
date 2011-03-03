<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="utf-8"%><%@ page import="org.unicode.cldr.web.*"%><%@ page import="java.sql.*" %><%@ page import="com.ibm.icu.util.ULocale" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%
	boolean doingByLocaleSubmit = false;
	boolean doingByLocaleVote = false;
	boolean doingByUser = false;
	String theSql = null;
	String title = "?";
	String stat = request.getParameter("stat");
	if (stat == null)
		stat = "s";

	if (stat.equals("v")) {
		doingByLocaleVote = true;
		title = "Locales by Vote";
		theSql = "select  locale,count(*) as count from cldr_vet  where submitter is not null group by locale order by count desc ";
	} else {
		theSql = "select  locale,count(*) as count from cldr_data  where submitter is not null group by locale order by count desc ";
		title = "Locales by Submitted Data";
		doingByLocaleSubmit = true;
	}
%>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>SurveyTool Statistics | <%=title%></title>
</head>
<body>

<a href="<%=request.getContextPath()%>/survey">Return to the SurveyTool <img src='STLogo.png' style='float:right;' /></a>

<br>
<!-- 
<hr/>
Switch to: 
  <a href="<%=request.getContextPath() + request.getServletPath()
					+ "?stat=v"%>">Locale By Vote</a>  |
  <a href="<%=request.getContextPath() + request.getServletPath()
					+ "?stat=s"%>">Locale By Submit</a>  |
<br/>
<hr/>
-->

<%
	int totalwidth = 800;
	int totalhei = 600;
	DBUtils dbUtils = DBUtils.getInstance();
	Connection conn = dbUtils.getDBConnection();
	if (conn == null) {
		throw new InternalError(
				"Can't open DB connection. Note, you must use a new JNDI connection for this to work.");
	}
	try {
		String limit = "";
%>
	<h1>SurveyTool Statistics: <%=title%></h1>

	Total New/Changed Submitted: <%=dbUtils
						.sqlQuery(conn,
								"select count(*) from cldr_data where submitter is not null")%> <br/>
	Total Votes Cast: <%=dbUtils
						.sqlQuery(conn,
								"select count(*) from cldr_vet where submitter is not null")%> <br/>



	<%
		String theSqlVet = "select  locale,count(*) as count from cldr_vet  where submitter is not null "
					+ limit + " group by locale ";
			String theSqlData = "select  locale,count(*) as count from cldr_data  where submitter is not null "
					+ limit + " group by locale  ";

			String[][] submitsV = dbUtils.sqlQueryArrayArray(conn,
					theSqlVet);
			String[][] submitsD = dbUtils.sqlQueryArrayArray(conn,
					theSqlData);

			String[][] submits = StatisticsUtils.calcSubmits(submitsV,
					submitsD);
	%>

	<h2><%=title%></h2>
	<table style='border: 1px solid black; cell-padding: 3px;'> 
		<tr><th>Locale</th><th>Data</th><th>Votes</th>
		<%
			for (String[] r : submits) {
		%>
			
	<tr>
		<th style='background-color: #ddd; text-align: left;'><%=r[0] + ": " + new ULocale(r[0]).getDisplayName()%></th>
		<td><%=r[1]%></td>
		<td><%=r[2]%></td>
	</tr>
	<%
		}
	%>
	</table>
	
<%
		if (submits != null && submits.length > 0) {
	%>
	<div id="holder-holder">
	        <div id="holder" style="width: <%=totalwidth%>px; height: <%=totalhei%>px;"></div>
	</div>        
	
	<%
        			int offh = 10;
        					int offv = 10;
        					int wid = totalwidth-20;
        					int hei = 220;
        		%>

<script type='text/javascript' src='js/raphael.js' ></script>
<script type='text/javascript' src='js/g.raphael.js' ></script>
<script type='text/javascript' src='js/g.line.js' ></script>
<script type='text/javascript' src='js/g.bar.js' ></script>
	<script type="text/javascript">
		var r = Raphael("holder");
		var barchart = r.g.barchart(<%=offh%>, <%=offv%>, <%=wid%>, <%=hei%>, 
				[ [ <%for (String[] r : submits) {%><%=r[1]%>, <%}%> ], [ <%for (String[] r : submits) {%><%=r[2]%>, <%}%> ]]);
		<%int ii = 0;
					int ea = wid / submits.length;
					for (String[] r : submits) {
						++ii;
						int h = ((ea) * (ii - 1)) + offh + (ea / 2);
						int v = hei + offv;%>
			var t<%=ii%> = r.text(<%=h%>,<%=v%>, "<%=r[0]%>");
			var n<%=ii%> = r.text(<%=h%>,<%=v + 20%>, "<%=r[1]%>\n<%=r[2]%>");
		<%}%>
		var nxx = r.text(<%=offh-4%>,<%=hei+offv + 20%>, "d\nv");
	</script>
<%
	}
%>

<%
	} finally {
		if (conn != null) {
			DBUtils.closeDBConnection(conn);
		}
	}
%>

<hr/>
<i>Graphs by <a href='http://g.raphaeljs.com/'>gRaphaël</a></i>

<hr>
<a href="<%=request.getContextPath()%>/survey">Return to the SurveyTool</a>

</body>
</html>