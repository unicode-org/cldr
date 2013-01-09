<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="utf-8"%><%@ page import="org.unicode.cldr.web.*"%><%@ page import="java.sql.*" %><%@ page import="com.ibm.icu.util.ULocale" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%

	String votesAfter = SurveyMain.getSQLVotesAfter();


	boolean doingByLocaleSubmit = false;
	boolean doingByLocaleVote = false;
	boolean doingByUser = false;
//	String theSql = null;
	String title = "?";
	String stat = request.getParameter("stat");
	if (stat == null)
		stat = "s";

	title = "Locales by Submitted Data";
	doingByLocaleSubmit = true;
%>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>SurveyTool Statistics | <%=title%></title>
<link rel='stylesheet' type='text/css' href='./surveytool.css' />
<script type='text/javascript' src='js/raphael.js' ></script>
<script type='text/javascript' src='js/g.raphael.js' ></script>
<script type='text/javascript' src='js/g.line.js' ></script>
<script type='text/javascript' src='js/g.bar.js' ></script>
</head>
<body>
<%@ include file="/WEB-INF/tmpl/stnotices.jspf" %>
<span id="visitors"></span>
<hr>

<%@ include file="/WEB-INF/tmpl/ajax_status.jsp" %>

<a href="<%=request.getContextPath()%>/survey">Return to the SurveyTool <img src='STLogo.png' style='float:right;' /></a>
<!-- 	| <a class='notselected' href="statistics-org.jsp">by Organization</a> -->

<br>

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
	<i>Showing only votes cast after <%= SurveyMain.getVotesAfterDate() %></i><br/>

	Total Items Submitted: <%=dbUtils
						.sqlQuery(conn,
								"select count(*) from cldr_votevalue where submitter is not null and last_mod > " + votesAfter + " ")%> <br/>



	<%
		String theSqlVet = "select  locale,count(*) as count from cldr_votevalue  where submitter is not null and last_mod > " + votesAfter + " "
					+ limit + " group by locale ";
			String theSqlData = theSqlVet;

			String[][] submitsV = dbUtils.sqlQueryArrayArray(conn,
					theSqlVet);
			String[][] submitsD = dbUtils.sqlQueryArrayArray(conn,
					theSqlData);

			String[][] submits = StatisticsUtils.calcSubmits(submitsV,
					submitsD);
	%>

	<h2><%=title%></h2>
	<table style='border: 1px solid black; cell-padding: 3px;'> 
		<tr><th>Locale</th><th>Data</th>
		<%
			for (String[] r : submits) {
		%>
			
	<tr>
		<th style='background-color: #ddd; text-align: left;'><a href='survey?_=<%=r[0]%>'><%=r[0] + ": " + new ULocale(r[0]).getDisplayName()%></a></th>
		<td><%=r[1]%></td>
	</tr>
	<%
		}
	%>
	</table>
	
<%
		if (submits != null && submits.length > 0) {
			
			if(submits.length>20) { %><hr/><i>(Note: Top 20 of <%= submits.length %> shown on graph.)</i><% 
			
				submits = StatisticsUtils.calcSubmits(submitsV, submitsD, 20);
			}

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

<hr>
	<script type="text/javascript">
	var wid = <%= wid %>;
	var hei = <%= hei %>;
		var r = Raphael("holder");
		var barchart = r.g.barchart(<%=offh%>, <%=offv%>, wid,hei, 
				[ [ <%for (String[] r : submits) {%><%=r[1]%>, <%}%> ]]);
		<%int ii = 0;
					int ea = wid / submits.length;
					for (String[] r : submits) {
						++ii;
						int h = ((ea) * (ii - 1)) + offh + (ea / 2);
						int v = hei + offv;%>
			var t<%=ii%> = r.text(<%=h%>,<%=v%>, "<%=r[0]%>");
			var n<%=ii%> = r.text(<%=h%>,<%=v + 20%>, "<%=r[1]%>");
		<%}%>
		var nxx = r.text(<%=offh-4%>,<%=hei+offv + 20%>, "v");
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

<p>
 Note: Sort is by D, the total number of new proposed data items submitted.
 there can be more submissions than votes (if multiple proposals are made for a single item), and there may be more votes than submissions (if multiple people voted for a single proposal).
</p>
<hr>

<%-- OLD CRUFT ABOVE. 
  NEW STUFF...  ----------------------------------------------------- --%>
	<h3>Submits by day</h3>
	<div id="dholder-holder">
	        <div id="dholder" style="width: 600px; height: 800px;"></div>
	</div>        
	
<script>
showstats("dholder");
</script>



<i>Graphs by <a href='http://g.raphaeljs.com/'>gRaphaël</a></i>
<hr>
<h3>Recently submitted items</h3>

<div id='submitItems'>
</div>
...

<script>
showRecent('submitItems')
</script>

<hr>
<a href="<%=request.getContextPath()%>/survey">Return to the SurveyTool</a>

</body>
</html>
