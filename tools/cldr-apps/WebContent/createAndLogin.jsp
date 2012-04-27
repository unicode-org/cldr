<%@ page contentType="text/html; charset=UTF-8"
	import="org.unicode.cldr.web.*,org.unicode.cldr.util.*"%>

<%
String vap = request.getParameter("vap");
if(vap==null ||
    vap.length()==0 ||
    	!SurveyMain.isUnofficial() || 
			(!SurveyMain.vap.equals(vap) && !vap.equals(SurveyMain.testpw))  ) {
	response.sendRedirect("http://cldr.unicode.org");
	return;
}

VoteResolver.Organization orgs[] = VoteResolver.Organization.values();
VoteResolver.Organization anOrg = orgs[(int)Math.rint(Math.random()*(double)(orgs.length-1))];
String myorg = anOrg.name();
	
	
    Cookie c0 = WebContext.getCookie(request,SurveyMain.QUERY_EMAIL);
    if(c0!=null) {
        c0.setValue("");
        c0.setMaxAge(0);
        response.addCookie(c0);
    }
    Cookie c1 = WebContext.getCookie(request,SurveyMain.QUERY_PASSWORD);
    if(c1!=null) {
        c1.setValue("");
        c1.setMaxAge(0);
        response.addCookie(c1);
    }

%>
<html>
<head>
<title>CLDR Web Applications : Create and Login</title>
<link rel='stylesheet' type='text/css' href='./surveytool.css' />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body>
<%@ include file="header.jspf"%>
				<img src="STLogo.png" align="right" border="0" title="[logo]" alt="[logo]" />
<h1>Add a Test Survey Tool user</h1>
<form class="adduser" action='survey'
	method="POST"><input type="hidden" name="dump"
	value='<%=vap%>' /> <input type="hidden"
	name="action" value="new_and_login" />

<table>
	<tr>
		<th><label for="new_name">Real Name (ONE WORD):</label></th>
		<td><input id='real' size="40" value="REALNAME" name="real" /></td>
	</tr>
	<tr class="submit">
		<td colspan="2"><button type="submit">Login</button></td>
	</tr>
	</table>
	<hr>
	

	
	<table id='more' style='display: none;'>
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
</table>
</form>
<hr />
<script>
document.getElementById('real').focus()
</script>
<%-- <a
	href='<%=request.getParameter("a")%>?s=<%=request.getParameter("s")%>'>Cancel,
return to Survey Tool</a>
 --%>|
<a href="./index.jsp">Return to CLDR Applications</a>  | <a href='javascript:document.getElementById("more").style.display="";'>(More Options...)</a>
|
<!-- <a target="_new"
	href="http://dev.icu-project.org/cgi-bin/cldrwiki.pl?SurveyToolHelp/AddModifyUser">Help
on this page (in a new window)</a>
 --></body>
</html>
