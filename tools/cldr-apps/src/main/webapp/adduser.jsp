<%@ page contentType="text/html; charset=UTF-8"
	import="org.unicode.cldr.web.*"%>

<%
	String myorg = WebContext.decodeFieldString(request
			.getParameter("defaultorg"));
	if (myorg != null) {
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
<%@ include file="header.jspf"%>
<h1>Add a Survey Tool user</h1>
<form class="adduser" action='<%=request.getParameter("a")%>'
	method="POST"><input type="hidden" name="s"
	value='<%=request.getParameter("s")%>' /> <input type="hidden"
	name="do" value="new" />

<table>
	<tr>
		<th><label for="new_name">Name:</label></th>
		<td><input size="40" name="new_name" /></td>
	</tr>
	<tr>
		<th><label for="new_email">E-mail:</label></th>
		<td><input size="40" name="new_email" /></td>
	</tr>
	<tr>
		<th><label for="new_org">Organization:</label></th>
		
		<% if(myorg.length()==0) { %>
			<td><input id='new_org' name="new_org" value="<%=myorg%>" /></td>
			<td>
				<select onchange="document.getElementById('new_org').value=this.value">
					<option value='' selected="selected">Choose...</option>
					<%
						for(String o : UserRegistry.getOrgList()) {
					%>
							<option value='<%= o %>'><%= o %></option>
					<%
						}
					%>
				</select>
			</td>
		<% } else { %>
			<td><input name="new_org" disabled="disabled" value="<%=myorg%>" /></td>
		<% }  %>
	</tr>
	<tr>
		<th><label for="new_userlevel">Userlevel:</label></th>
		<td><select name="new_userlevel">
			<%
				for (int lev : UserRegistry.ALL_LEVELS) {
			%>
			<option value="<%=lev%>"
				<%=(lev == UserRegistry.VETTER) ? "selected" : ""%>><%=lev%>
			(<%=UserRegistry.levelAsStr(lev)%>)</option>
			<%
				}
			%>
		</select> <!--      <br>       <label>Userlevel: <input name="new_userlevel" value="5" /></label>    (1=TC, 5=Vetter, 10=Street, ...) <br/> -->
		</td>
	</tr>
	<tr>
		<th><label for="new_locales">Languages responsible:</label></th>
		<td><input id="new_locales" name="new_locales" value="und" /> <button onclick='{document.getElementById("new_locales").value="*"; return false;}' >All Locales</button><br>
		(Space separated. Examples: "en de de_CH fr zh_Hant".  Use the All Locales button to grant access to all locales.  )
		</td>
	</tr>
	<tr class="submit">
		<td colspan="2"><input type="submit" value="Add" /></td>
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
	href="http://cldr.unicode.org/index/survey-tool/managing-users">Help
on this page (in a new window)</a>
</body>
</html>
