<%@ page contentType="text/html; charset=UTF-8"
	import="org.unicode.cldr.web.*,org.unicode.cldr.util.*"%>
<%
String vap = request.getParameter("vap");
if(vap==null ||
	(SurveyMain.vap==null||SurveyMain.vap.isEmpty()) ||
    vap.length()==0 ||
    	!SurveyMain.isUnofficial || 
			(!SurveyMain.vap.equals(vap) )  ) {
	response.sendRedirect("http://cldr.unicode.org"); // Get out.
	return;
}

String lmi = request.getContextPath()+"/survey?letmein="+vap+"&amp;email=admin@";
String sql = request.getContextPath()+"/survey?sql="+vap+"";
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Survey Tool Administration | <%= SurveyMain.localhost() %></title>
<link rel='stylesheet' type='text/css' href='./surveytool.css' />
</head>
<body class='admin'>
				<a id='gotoSt' href='<%= lmi %>'><img src="STLogo.png" align="right" border="0" title="[logo]" alt="[logo]" /></a>
<h1>Survey Tool Administration | <%= SurveyMain.localhost() %></h1>
<a href='<%= lmi %>'>SurveyTool as Admin</a> | <a href='<%= sql %>'>Raw SQL</a>

<hr>
<%@ include file="/WEB-INF/tmpl/stnotices.jspf" %>
<span id="visitors"></span>
<hr>

<%@ include file="/WEB-INF/tmpl/ajax_status.jsp" %>
<script>
var vap='<%= vap %>';
dojo.ready(loadAdminPanel);
</script>

<div id='adminStuff'></div>



</body>
</html>