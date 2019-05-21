<%@page import="com.ibm.icu.dev.util.ElapsedTimer"%>
<%@page import="org.unicode.cldr.web.*"%>
<%@page import="org.unicode.cldr.util.*,java.util.*"%>
<%@page import="java.io.*"%>
<%@page import="java.sql.*"%>
<%@page import="org.unicode.cldr.test.*"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Update All Files</title>
</head>
<body>

<% if( (request.getParameter("vap")==null)
	   || !request.getParameter("vap").equals(CookieSession.sm.vap)) { %>
	   Not authorized. 
<% return;
	} %>

<%
long start = System.currentTimeMillis();
ElapsedTimer overallTimer = new ElapsedTimer("overall update started " + new java.util.Date());
int numupd = 0;
	OutputFileManager ofm = CookieSession.sm.getOutputFileManager();
%>

Have OFM=<%= ofm %>

<ol>
<%
	Set<CLDRLocale> sortSet = new TreeSet<CLDRLocale>();
	sortSet.addAll(SurveyMain.getLocalesSet());
	Connection conn = null;
	synchronized(OutputFileManager.class) {
	try {
		conn = CookieSession.sm.dbUtils.getDBConnection();
		for (CLDRLocale loc : sortSet) {
	          Timestamp locTime=ofm.getLocaleTime(conn, loc);
%>
			<li><%= loc.getDisplayName() %> - <%= locTime.toLocaleString() %><br/>
	
 <% 				for(OutputFileManager.Kind kind : OutputFileManager.Kind.values()) {
		//if(kind!=OutputFileManager.Kind.vxml) continue;
		boolean nu= ofm.fileNeedsUpdate(locTime,loc,kind.name());   %> 
					<span style=' background-color: <%= nu?"#ff9999":"green" %>; font-weight: <%= nu?"regular":"bold" %>; color: <%= nu?"silver":"black" %>;'><%= kind %><%
						if(nu&&(kind==OutputFileManager.Kind.vxml || kind==OutputFileManager.Kind.pxml)) {
							System.err.println("Writing " + loc.getDisplayName() + ":"+kind);
							ElapsedTimer et = new ElapsedTimer("to write " + loc +":"+kind);
							File f = ofm.getOutputFile(conn, loc, kind.name());
							out.print(" x=" + (f != null && f.exists()));
							numupd++;
							System.err.println(et + " - upd " + numupd+"/"+(sortSet.size()+2));
						}
					 %></span>  &nbsp;
	
	<%  } %>
			</li>
<%
		}
	} finally {
		DBUtils.close(conn);
	}
}
%>
</ol>
<hr>
Total upd: <%= numupd+"/"+(sortSet.size()+2) %>
Total time: <%= overallTimer %> : <%= ((System.currentTimeMillis()-start)/(1000.0*60)) %>min
<%
	System.err.println(overallTimer +  " - updated " + numupd+"/"+(sortSet.size()+2) + " in " + (System.currentTimeMillis()-start)/(1000.0*60) + " min");
%>
</body>
</html>