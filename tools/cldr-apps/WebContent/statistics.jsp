<%@page import="com.ibm.icu.text.CompactDecimalFormat"%>
<%@page import="com.ibm.icu.dev.util.ElapsedTimer"%>
<%@page import="com.ibm.icu.text.DecimalFormat,com.ibm.icu.text.NumberFormat"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="utf-8"%><%@ page import="org.unicode.cldr.web.*"%><%@ page import="java.sql.*" %><%@ page import="org.json.*" %><%@ page import="com.ibm.icu.util.ULocale" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%


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
	final String newVersion = SurveyMain.getNewVersion();
	final String oldVersion = SurveyMain.getOldVersion();
	final String newVersionText = (SurveyMain.isPhaseBeta())?(newVersion+"BETA"):newVersion;
%>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>SurveyTool <%= newVersionText %> (Old) Statistics | <%=title%></title>
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

<h2><a href='v#statistics'>(Switch to the NEW statistics page)</a></h2>

<hr>

<a href="<%=request.getContextPath()%>/survey">Return to the SurveyTool <img src='STLogo.png' style='float:right;' /></a>
<!-- 	| <a class='notselected' href="statistics-org.jsp">by Organization</a> -->
<i>This page does not auto-refresh, and is only calculated every few minutes. <br>Also, the translated names may be out of date, as they are currently using the previous-version data.</i>
<br>

<%
	int totalwidth = 800;
	int totalhei = 600;
	DBUtils dbUtils = DBUtils.getInstance();
	
    int totalItems = StatisticsUtils.getTotalItems();
    int totalNewItems = StatisticsUtils.getTotalNewItems();
    int totalSubmitters = StatisticsUtils.getTotalSubmitters();
	Connection conn = dbUtils.getDBConnection();
	if (conn == null) {
		throw new InternalError(
				"Can't open DB connection. Note, you must use a new JNDI connection for this to work.");
	}
	try {
%>
	<h1>SurveyTool | CLDR <%= newVersionText %> Statistics: <%=title%></h1>
	<br/>
    <%
        String theSqlVet = StatisticsUtils.QUERY_ALL_VOTES;
            String theSqlData = theSqlVet;

            final String[][] submitsV = dbUtils.sqlQueryArrayArray(conn, theSqlData);
            final String[][] submitsD = submitsV;

            String[][] submits = StatisticsUtils.calcSubmits(submitsV,
                    submitsD);
            
            NumberFormat fmt = DecimalFormat.getInstance(DecimalFormat.NUMBERSTYLE);
    %>


	Total Items Submitted for <%= newVersionText %>: <%= fmt.format( totalItems) %> (<%= fmt.format(totalNewItems) %> new) in <%= fmt.format(submits.length) %> locales by <%= fmt.format(totalSubmitters)  %> submitters. <br/>



	<h2><%=title%></h2>
	<table id='statisticsTable'>
		<thead>
			<tr>
				<th>#</th>
				<th>ID</th>
				<th colspan='2'>Locale</th>
				<th>Items</th>
			</tr>
		</thead>
		<tbody>
			<%
			ElapsedTimer localeTimer = new ElapsedTimer();
				int rank = 0;
			    for (String[] r : submits) {
			    	CLDRLocale thisLoc = CLDRLocale.getInstance(r[0]);
			    	Pair<String,String> name = CookieSession.sm.outputFileManager.statGetLocaleDisplayName(thisLoc);
			    	String baseName = name.getFirst(); /* thisLoc.getDisplayName( false, null) */
			    	String selfName =  name.getSecond(); //CookieSession.sm.getDiskFactory().make(r[0], true).getName(r[0])
			    	
			    	// Removing this since it's the "old" statistics page, and the languages page has split...
			    	// StringBuilder selfLink = WebContext.appendContextVurl(new StringBuilder(request.getContextPath()), thisLoc, PathHeader.PageId.Languages, 
			    	//		  CookieSession.sm.xpt.getStringIDString("//ldml/localeDisplayNames/languages/language[@type=\""+thisLoc.getLanguage()+"\"]"),
			    	//		  "");
			%>
			<tr class='r<%= rank%2 %>'>
				<td class='rank'><%=fmt.format(++rank) %></td>
				<td class='locid'><%= r[0] %></td>
				<td class='locname'>
				    <a
					href='survey?_=<%=r[0]%>'><%= baseName %></a>
					           </td>
                               <td dir='<%= CookieSession.sm.getHTMLDirectionFor(thisLoc) %>' class='selfname dir<%= CookieSession.sm.getHTMLDirectionFor(thisLoc)  %>'><a><%= selfName %></a></td>
				<td class='count'><%=fmt.format(Integer.parseInt(r[1]))%></td>
			</tr>
			<%
		}
	   if(SurveyMain.isUnofficial()) System.err.println("Locale list calculated  " + localeTimer);
	%>
		</tbody>
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
	<h3>Submits by day:   NEW/CHANGED | OLD</h3>
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
