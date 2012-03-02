<%@ page contentType="text/html; charset=UTF-8"
	import="org.unicode.cldr.web.*,org.unicode.cldr.util.*"%>

<%
String vap = request.getParameter("vap");
if(!SurveyMain.isUnofficial || !SurveyMain.vap.equals(vap)) {
	response.sendRedirect("http://cldr.unicode.org");
	return;
}

VoteResolver.Organization orgs[] = VoteResolver.Organization.values();
VoteResolver.Organization anOrg = orgs[(int)Math.rint(Math.random()*(double)(orgs.length-1))];
String myorg = anOrg.name();
	
	String randomEmail = UserRegistry.makePassword(null)+"@"+UserRegistry.makePassword(null).substring(0,4)+"."+myorg+".example.com";
	String randomName = UserRegistry.makePassword(null).substring(0,5);
	String randomPass = UserRegistry.makePassword(null);
%>
<html>
<head>
<title>CLDR Web Applications : Create and Login</title>
<link rel='stylesheet' type='text/css' href='./surveytool.css' />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body>
<%@ include file="header.jspf"%>
<h1>Add a Survey Tool user</h1>
<form class="adduser" action='survey'
	method="POST"><input type="hidden" name="dump"
	value='<%=vap%>' /> <input type="hidden"
	name="action" value="new_and_login" />

<table>
	<tr>
		<th><label for="new_name">Real Name (ONE WORD):</label></th>
		<td><input size="40" value="REALNAME" name="real" /></td>
	</tr>
	</table>
	<hr>
	<table>
	<tr>
		<th><label for="new_name">Fake Name:</label></th>
		<td><input size="40" value="<%= randomName %>" name="new_name" /></td>
	</tr>
	<tr>
		<th><label for="new_email">E-mail:</label></th>
		<td><input value='<%= randomEmail %>' size="40" name="new_email" /></td>
	</tr>
	<tr>
		<th><label for="new_password">Password:</label></th>
		<td><input value='<%= randomPass %>' size="40" name="new_password" /></td>
	</tr>
	<tr>
		<th><label for="new_org">Organization:</label></th>
		
			<td><input name="new_org"  value="<%=myorg%>" /></td>
	</tr>
	<tr>
		<th><label for="new_userlevel">Userlevel:</label></th>
		<td><select name="new_userlevel">
			<%
				for (int lev : UserRegistry.ALL_LEVELS) {
			%>
			<option value="<%=lev%>"
				<%=(lev == UserRegistry.TC) ? "selected" : ""%>><%=lev%>
			(<%=UserRegistry.levelAsStr(lev)%>)</option>
			<%
				}
			%>
		</select> <!--      <br>       <label>Userlevel: <input name="new_userlevel" value="5" /></label>    (1=TC, 5=Vetter, 10=Street, ...) <br/> -->
		</td>
	</tr>
	<tr>
		<th><label for="new_locales">Languages responsible:</label></th>
		<td><input name="new_locales" value="" /> <br>
		(Space separated. Examples: "en de fr"  but not <strike>"zh_Hant"</strike> or <strike>"de_CH"</strike>. )</td>
	</tr>
	<tr class="submit">
		<td colspan="2"><input type="submit" value="Login" /></td>
	</tr>
</table>
</form>
<hr />
<a
	href='<%=request.getParameter("a")%>?s=<%=request.getParameter("s")%>'>Cancel,
return to Survey Tool</a>
|
<a href="./index.jsp">Return to CLDR Applications</a>
|
<a target="_new"
	href="http://dev.icu-project.org/cgi-bin/cldrwiki.pl?SurveyToolHelp/AddModifyUser">Help
on this page (in a new window)</a>
</body>
</html>
