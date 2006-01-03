<html>
	<head>
		<title>CLDR Web Applications : ST Login</title>
		<link rel='stylesheet' type='text/css' href='./surveytool.css' />
	</head>
	<body>
		<h1>Survey Tool Login</h1>
		<form action="<%= request.getParameter("a") %>" method="POST">
			<label>Email: <input name="email" /></label><br/>
			<label>Password: <input type="password" name="pw" /></label><br/>
			<input type="submit" value="Login" />
		</form>
		<hr/>
		<a href="./index.jsp">Return to CLDR Applications</a>
	</body>
</html>
