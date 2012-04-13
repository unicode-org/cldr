<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
 import="org.unicode.cldr.web.*,org.unicode.cldr.util.*,java.util.*"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
</head>
<body>

<%
//StringBuffer sb = new StringBuffer(request.getParameter("q"));
CLDRFile english = CookieSession.sm.getBaselineFile();
XPathTable xpt = CookieSession.sm.xpt;

int n = 0;
int  m = 0;
com.ibm.icu.dev.test.util.ElapsedTimer et = new com.ibm.icu.dev.test.util.ElapsedTimer();
for(String s : english ) {
    n++;
//    out.println(s );
//    out.println(" - ");
    String id = xpt.getStringIDString(s);
    int nid = xpt.getByXpath(s);
    out.println(id + ":"+nid);
    String back = xpt.getByStringID(id);
    out.println("="+back);
    if(!s.equals(back)) {
        out.println("<b>!="+s+"</b>");
        m++;
    }
    out.println("<br>");
    out.flush();
}
out.println(et.toString()+ " [[[]]]"+"count="+n+", fail="+m+"..");

%>

</body>
</html>