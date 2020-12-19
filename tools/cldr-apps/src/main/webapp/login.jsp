<%@ page contentType="text/html; charset=UTF-8" import="org.unicode.cldr.web.*" %>
<!DOCTYPE html>
<html>
<head>
<title>SurveyTool: Login</title>
<link rel="stylesheet" href="<%= request.getContextPath() %>/surveytool.css" />
</head>
<body>
<h1>SurveyTool Login</h1>
<img src="<%= request.getContextPath() %>/STLogo.png" align='left'>
<div id='smalllogin' style='position: relative !important; top: inherit !important; left: inherit !important' class='small_login_normal'>
	<div id='small_login_show' class='small_login_hot'>
<%
		if (request.getParameter("operationFailed") != null) {
%>
			<div class="fnotebox">Please login before continuing.</div>
<%		
		}
%>
		<form method='POST' action='<%= request.getContextPath() %>/v' >
			<label for="email"> Email:</label><input id="email" name="email" /> <br/>
			<label for="pw"> Password:</label> <input id="pw" type="password" name="pw" /> <br/>
			<label for="save_cook"> Log me in automatically next time?</label>
			<input id="<%= SurveyMain.QUERY_SAVE_COOKIE %>" type="checkbox" name="save_cookie" /> <br/>
			<button type='button' onclick='history.back();'>Cancel</button>
			<button type="submit"><b>Login</b></button>
			<script>
				function loginclick() {
					document.getElementById("small_login_show").className = "small_login_hot";
					document.getElementById("small_login_shower").className = 'small_login_hidden';
				}
				function exitclick() {
					document.getElementById("small_login_show").className = "small_login_hidden";
					document.getElementById("small_login_shower").className = 'small_login_normal';
				}
			</script>
			<noscript>JavaScript must be enabled to login to the Survey Tool.</noscript>
		</form>
	</div>
</div>
<p><img width=0 height=0 src='loader.gif'><!--  to preload this gif --></p>
<script>
	// append the hash to the target page
	var q = document.getElementsByTagName('form')[0];
	q.setAttribute('action', q.getAttribute('action') + window.location.hash);
</script>
</body>
</html>
