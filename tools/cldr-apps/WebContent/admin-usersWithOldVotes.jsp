<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" import="org.unicode.cldr.util.*,org.unicode.cldr.web.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <link rel='stylesheet' type='text/css' href='surveytool.css'>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Users With Old Votes</title>
</head>
<body>
<%@ include file="/WEB-INF/tmpl/stnotices.jspf" %>
<span id="visitors"></span><!-- needed for stnotices/ajax status -->
<hr>

<%@ include file="/WEB-INF/tmpl/ajax_status.jsp" %>

<%
if(!SurveyMain.vap.equals(request.getParameter("vap"))) {
	%>
	Goodbye.
	<%
	return;
}
%>

<h1>users with old votes</h1>

<%
SurveyMain sm = CookieSession.sm;

// List from multiple old tables, not only one previous table.
// Group together votes from each old table, list groups in reverse chronological order. 
int ver = Integer.parseInt(SurveyMain.getNewVersion());
while (--ver >= SurveyAjax.oldestVersionForImportingVotes) {
	String oldVotesTable = DBUtils.Table.VOTE_VALUE.forVersion(new Integer(ver).toString(), false).toString();
	if (DBUtils.hasTable(oldVotesTable)) {
		String sql = "select " + oldVotesTable
			+".submitter as submitter, cldr_users.id as id, cldr_users.email as email, cldr_users.password as password, count(*) as count from "+oldVotesTable
			+",cldr_users where "+oldVotesTable+".value is not null and "+oldVotesTable+".submitter=cldr_users.id  group by "+oldVotesTable+".submitter order by "+oldVotesTable+".submitter";
		java.util.Map<String, Object> rows[] = DBUtils.queryToArrayAssoc(sql); 
		%>

		<h2><%= oldVotesTable %></h2>
		<ol>

		<%
		for (java.util.Map<String, Object> m : rows) {
			UserRegistry.User u = sm.reg.getInfo((Integer) m.get("id"));
			%>

			<li>
			<%= u.toHtml() %> user #<%= m.get("submitter") %>,  
			<a href='<%= request.getContextPath() %>/v?email=<%= u.email %>&amp;pw=<%= u.password %>#oldvotes'><b>get old votes</b></a> 
			<a href='<%= request.getContextPath() %>/v?email=<%= u.email %>&amp;pw=<%= u.password %>#/mt'><b>Malti</b></a> 
			<a href='<%= request.getContextPath() %>/survey?email=<%= u.email %>&amp;pw=<%= u.password %>'><b>ST</b></a>             
			<%= m.get("count") %> items
			</li>
		<%
		} // end for
		%>

		</ol>

	<%
	} // end if
} // end while
%>

</body>
</html>
