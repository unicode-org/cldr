<%@ page import="com.ibm.icu.text.*" %>
<%@ page import="com.ibm.icu.util.*" %>
<%

/*
' SampleNumber.jsp
' --------
'
' Print the name that is passed in the
' 'name' GET parameter in a sentence
*/

response.setContentType("text/plain");
response.setCharacterEncoding("UTF-8");
ULocale loc = new ULocale("en-u-nu-"+request.getParameter("name"));
NumberFormat nf = NumberFormat.getInstance(loc);
double num = 1234567890.123;
out.write(nf.format(num));
%>
