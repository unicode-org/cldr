<%@ page contentType="text/html; charset=UTF-8" %>
<html>
	<head>
		<title>CLDR Web Applications : ST Login</title>
		<link rel='stylesheet' type='text/css' href='./surveytool.css' />
	</head>
	<body>
		<h1>Survey Tool Login</h1>
		<form action="<%= request.getParameter("a") %>" method="POST">
        <table border='0'>
          <tr><td align='right'>
			<label>Email: <input name="email" /></label></td></tr>
          <tr><td align='right'>
			<label>Password: <input type="password" name="pw" /></label></td><td>
			<input type="submit" value="Login" />
			
			<!--  detect javascript. Not a problem, just figure out if we can rely on it or no. -->
			<script type="text/javascript">
			<!--
			 document.write("<input type='hidden' name='p_nojavascript' value='f'>");
			// -->
			</script>
            <noscript><input name='p_nojavascript' type='hidden' value='t'></noscript>
            </td></tr>
          </table>
		</form>
        <hr/>
        <p><b>Note</b>, you need an account to be able to submit data. See <a href='http://www.unicode.org/cldr/wiki?SurveyToolHelp'>this page</a> for more details.</p>
		<hr/>
		<a href="./index.jsp">Return to CLDR Applications</a>
	</body>
</html>
