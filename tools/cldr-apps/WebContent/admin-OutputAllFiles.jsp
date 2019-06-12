<%@page import="com.ibm.icu.dev.util.ElapsedTimer"%>
<%@page import="org.unicode.cldr.web.*"%>
<%@page import="org.unicode.cldr.util.*,java.util.*"%>
<%@page import="java.io.*"%>
<%@page import="java.sql.*"%>
<%@page import="org.unicode.cldr.test.*"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Update All Files</title>
</head>
<body>
<%
	OutputFileManager.outputAndVerifyAllFiles(request, out);
%>
</body>
</html>
