<%@page import="com.ibm.icu.text.DateFormat"%>
<%@page import="java.util.EnumSet"%>
<%@page import="com.ibm.icu.text.SimpleDateFormat"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="org.unicode.cldr.util.*,java.util.*,java.sql.Connection,java.sql.Timestamp"
    %><%@ page import="org.unicode.cldr.web.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Output Status</title>
		<link rel='stylesheet' type='text/css' href='./surveytool.css' />

</head>
<body>

<%!
EnumSet<OutputFileManager.Kind> showLinksFor = EnumSet.of(OutputFileManager.Kind.vxml, OutputFileManager.Kind.pxml);
%>
<h1>Status of File Output</h1>

<p class='helpContent'>
This page lists which output files are updated and which are not.
The links take you directly to the XML.


Bold = available, Shaded = missing.

(timezone is <%= SurveyMain.defaultTimezoneInfo() %>)<br/>
</p>
<b>Types:</b>
<ul>
	<% for (OutputFileManager.Kind k : OutputFileManager.Kind.values()) { %>
		<li><tt><%= k %></tt>: <%= k.getDesc() %></li>
	<% } %>
</ul>
<a href="<%=request.getContextPath()%>/survey">Return to the SurveyTool <img src='STLogo.png' style='float:right;' /></a>
<%
DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, SurveyMain.BASELINE_LOCALE);

%>


<%
SurveyMain sm = CookieSession.sm;
if(sm==null || !sm.isSetup) {
	out.println("<i>Not booted yet, come back later.</i>");
	return;
}
int totals[] = new int[OutputFileManager.Kind.values().length];
%>

<%
if(sm.outputFileManager.outputDisabled) {
%>
<h1>Note: Output is temporarily disabled! Check the log for exceptions. </h1>
<%
}
%>

<div style='display: table-row;'>
<table class='sqlbox' style='display: table-cell; font-size: 140%;'>
<%


	for(int i=0;i<totals.length;i++) {totals[i]=0;}
int count=0;
int k=0;
boolean flip=false;
	Set<CLDRLocale> sortSet = new TreeSet<CLDRLocale>();
	sortSet.addAll(SurveyMain.getLocalesSet());
	Connection conn = null;
	synchronized(OutputFileManager.class) {
	try {
		conn = sm.dbUtils.getDBConnection();
		for (CLDRLocale loc : sortSet) {
			k++;
			if(k>(sortSet.size()/2)&&flip==false) {
				flip=true;
				%></table><p style='display:table-cell;padding: 1em;'>&nbsp;</p><table class='sqlbox' style='display: table-cell; font-size: 140%;'>
				<%
			}
			
	          Timestamp locTime=sm.outputFileManager.getLocaleTime(conn, loc);
			%>
			<tr>
			<th>
			<%=loc%>
			</th>
			<td><%= df.format(locTime) %></td>
			<%
			    int j=0;
							for(OutputFileManager.Kind kind : OutputFileManager.Kind.values()) {
							String dir = sm.getDataDir(kind.name(), loc).getName(); // name of the parent dir - so "main"
							//if(kind!=OutputFileManager.Kind.vxml) continue;
							boolean nu= sm.outputFileManager.fileNeedsUpdate(locTime,loc,kind.name());
							if(nu) totals[j]++;
							j++;
//                            org.tmatesoft.svn.core.wc.SVNInfo i = sm.outputFileManager.svnInfo(sm.getDataFile(kind.name(), loc));
                       //     org.tmatesoft.svn.core.wc.SVNStatus s = sm.outputFileManager.svnStatus(sm.getDataFile(kind.name(), loc));
			%>
					<td style=' background-color: <%= nu?"#ff9999":"green" %>; font-weight: <%= nu?"regular":"bold" %>; color: <%= nu?"silver":"black" %>;'>
						
						<% if (showLinksFor.contains(kind)) { %>
							<a href='survey/<%= kind %>/<%= dir %>/<%= loc %>.xml'>
						<% } else { %> 
						 	<a>  
					 	<% } %>
							<%= kind %>
							</a>
					<%--
					   : <%= s.getNodeStatus() %>  
					   --%>
					   
					</td>
				
			<% } %>
			</tr>
			<%
		}
	} catch (NullPointerException npe) {
		%> <p><i>NPE - SurveyTool may not be started yet.</i></p><%
	} finally {
		DBUtils.close(conn);
	}
}

%>
<tr>
	<th>TOTAL</th>
	<th><%= sortSet.size() %></th>
	<% for(int i=0;i<totals.length;i++) { %>
		<th><%= totals[i] %></th>
	<% } %>
</tr>
</table>
</div>

</body>
</html>