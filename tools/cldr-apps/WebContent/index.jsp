<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/2001/REC-xhtml11-20010531/DTD/xhtml11-flat.dtd">
<%@ page contentType="text/html; charset=UTF-8" %>
<html>
	<head>
		<title>CLDR Web Applications</title>
<!--        <link rel="stylesheet" type="text/css" 
        href="http://www.unicode.org/webscripts/standard_styles.css"> -->
		<link rel='stylesheet' type='text/css' href='./surveytool.css' />
	</head>
	<body style='padding: 1em'>

    <% if(request.getParameter("logout")!=null) { %>
    <p>
    	<i>
    		You have been logged out. Thank you for using the Survey
    		Tool.
    	</i>
    </p>
    <% } %>

		<h1>CLDR Web Applications</h1>
		<ul>
			<li><strong><a href="survey/">CLDR Survey Tool</a></strong> - <a href="http://www.unicode.org/cldr/wiki?SurveyToolHelp">(Help)</a><br /></li>
		    <li><strong><a href="about.jsp">About this Installation…</a></strong></li>
		</ul>
        
        <hr />
        <p><a href="http://www.unicode.org">Unicode</a> | <a href="http://www.unicode.org/cldr">CLDR</a></p>
        <div style='float: right; font-size: 60%;'><span class='notselected'>valid <a href='http://jigsaw.w3.org/css-validator/check/referer'>css</a>,
            <a href='http://validator.w3.org/check?uri=referer'>xhtml 1.1</a></span></div>
	</body>
</html>
