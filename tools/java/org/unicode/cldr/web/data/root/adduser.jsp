<%@ page contentType="text/html; charset=UTF-8" import="org.unicode.cldr.web.*" %>

<% 
    String myorg = WebContext.decodeFieldString(request.getParameter("defaultorg"));
    if(myorg != null) {
        myorg = SurveyForum.HTMLSafe(myorg);
    } else {
        myorg = "";
    }
 %>
<html>
	<head>
		<title>CLDR Web Applications : Add a user</title>
		<link rel='stylesheet' type='text/css' href='./surveytool.css' />
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	</head>
	<body>
		<h1>Add a Survey Tool user</h1>
		<form action="<%= request.getParameter("a") %>" method="POST">
            <input type="hidden" name="s" value="<%= request.getParameter("s") %>" />
            <input type="hidden" name="do" value="new" />
			<label>Name: <input size=40 name="new_name" /></label><br/>
			<label>Email: <input size=40 name="new_email" /></label><br/>
			<label>Organization: <input name="new_org"  value="<%= myorg %>"/></label>   (note: can leave blank if same as yours)<br/>
            <label>Userlevel: 
                <select name="new_userlevel">
                    <% for(int lev : UserRegistry.ALL_LEVELS) { %>
                        <option value="<%= lev %>"  <%= (lev==UserRegistry.VETTER)?"selected":"" %>  ><%= lev %> (<%= UserRegistry.levelAsStr(lev) %>)</option>
                    <% } %>
                </select>
             </label><br>
<!--             <label>Userlevel: <input name="new_userlevel" value="5" /></label>    (1=TC, 5=Vetter, 10=Street, ...) <br/> -->
			<label>Languages responsible: <input name="new_locales" value="" /></label>   (Space separated. Examples: "en de fr" ( Don't specify sublocales such as <strike>"zh_Hant"</strike> or <strike>"de_CH"</strike>. )  )<br/>
			<input type="submit" value="Add" />
		</form>
		<hr/>
		<a href="<%= request.getParameter("a") %>?s=<%= request.getParameter("s") %>">Cancel, return to Survey Tool</a> |
		<a href="./index.jsp">Return to CLDR Applications</a> |
		<a target="_new" href="http://dev.icu-project.org/cgi-bin/cldrwiki.pl?SurveyToolHelp/AddModifyUser">Help on this page (in a new window)</a>
	</body>
</html>
