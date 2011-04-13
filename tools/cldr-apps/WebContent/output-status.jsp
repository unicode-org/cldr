<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="org.unicode.cldr.util.*,java.util.*,java.sql.Connection"
    %><%@ page import="org.unicode.cldr.web.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Output Status</title>
</head>
<body>

<table>
<%
int count=0;
	SurveyMain sm = CookieSession.sm;
	Set<CLDRLocale> sortSet = new TreeSet<CLDRLocale>();
	sortSet.addAll(SurveyMain.getLocalesSet());
	Connection conn = null;
	try {
		conn = sm.dbUtils.getDBConnection();
		for (CLDRLocale loc : sortSet) {
			boolean nu = sm.fileNeedsUpdate(conn, loc, "vxml");
			%>
			<% if(nu){
				count++;
				%>
			<tr>
			<th>
			<%=loc%>
			</th>
			<td style='background-color: <%= nu?"red":"green" %>'>
			needsUpdate=<%= nu %>
			</td>
			</tr>
			<% } %>
			<%
		}
	} finally {
		DBUtils.close(conn);
	}
%>
</table>

Total locales needing update: <%= count %>

</body>
</html>