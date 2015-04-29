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
String oldVoteTable = STFactory.getLastVoteTable();
%>


<%
SurveyMain sm = CookieSession.sm;
//String xp = "http://st.unicode.org/cldr-apps/survey?_=fr&x=Numbers&stdebug=true;#x@a1ef41eaeb6982d";
//int xpid = sm.xpt.getByXpath(xp);
 java.util.Map rows[] =    DBUtils.queryToArrayAssoc( "select  "+oldVoteTable
		 +".submitter as submitter, cldr_users.id as id, cldr_users.email as email, cldr_users.password as password, count(*) as count from "+oldVoteTable
		 +",cldr_users where "+oldVoteTable+".value is not null and "+oldVoteTable+".submitter=cldr_users.id  group by "+oldVoteTable+".submitter order by "+oldVoteTable+".submitter");

  
%>

<h2><%= oldVoteTable %></h2>
<ol>
    <% for(java.util.Map m : rows) {
    	UserRegistry.User u = sm.reg.getInfo((Integer)m.get("id"));
    	%>
        <li>
            <%= u.toHtml() %> user #<%= m.get("submitter") %>,  
            <a href='<%= request.getContextPath() %>/v?email=<%= u.email %>&amp;pw=<%= u.password %>#oldvotes'><b>get old votes</b></a> 
            <a href='<%= request.getContextPath() %>/v?email=<%= u.email %>&amp;pw=<%= u.password %>#/mt'><b>Malti</b></a> 
            <a href='<%= request.getContextPath() %>/survey?email=<%= u.email %>&amp;pw=<%= u.password %>'><b>ST</b></a> 
            
            <%= m.get("count") %> items
        </li>
    <% } %>
</ol>



</body>
</html>

