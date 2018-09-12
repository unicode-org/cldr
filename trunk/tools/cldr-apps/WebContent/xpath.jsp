<%@ page contentType="text/html; charset=UTF-8" import="org.unicode.cldr.web.*" %>

<% 
String loc = WebContext.decodeFieldString(request.getParameter("_"));
if(loc != null) {
    loc = SurveyForum.HTMLSafe(loc);
} else {
    loc = "";
}
String xpath = WebContext.decodeFieldString(request.getParameter("xpath"));
if(xpath != null) {
    xpath = SurveyForum.HTMLSafe(xpath);
} else {
    xpath = "";
}
String retAction = WebContext.decodeFieldString(request.getParameter("a"));
if(retAction != null) {
	retAction = SurveyForum.HTMLSafe(retAction);
} else {
	retAction = request.getContextPath()+"/survey";
}
String sess = WebContext.decodeFieldString(request.getParameter("s"));
if(sess != null) {
	sess = SurveyForum.HTMLSafe(sess);
} else {
	sess = "";
}
String msg = WebContext.decodeFieldString(request.getParameter("msg"));
 %>
<html>
	<head>
		<title>CLDR Web Applications : View XPath </title>
		<link rel='stylesheet' type='text/css' href='./surveytool.css' />
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	</head>
	<body>
		<h1>View XPath</h1>
		<form action="<%= retAction %>" method="GET">
            <input type="hidden" name="s" value="<%= sess %>" />
			<label>Locale: <input size=40 name="_" value="<%= loc %>" /></label><br/>
			<label>Xpath: <input size=80 name="xpath" value="<%= xpath %>" /></label><br/>
		<% if(msg!=null) { %>
			<div class='sterrmsg'><%= msg %></div>
		<% }  %>
			<input type="submit" value="Go" />
		</form>
		<hr/>
		The 'xpath' field will accept an xpath id, xpath string, or (possibly) prettypath.
		<hr/>
		<a href="<%= retAction %>?s=<%= sess %>">Cancel, return to Survey Tool</a> |
		<a href="./index.jsp">Return to CLDR Applications</a> |
	</body>
</html>
