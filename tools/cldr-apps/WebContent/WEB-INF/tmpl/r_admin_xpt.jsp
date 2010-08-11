<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%-- setup 'ctx' --%>
<%@ include file="/WEB-INF/jspf/report.jspf"  %>


<%@ page import="org.unicode.cldr.util.*" %>
<%@ page import="org.unicode.cldr.test.*" %>
<%@ page import="org.unicode.cldr.web.*" %>
<%@ page import="com.ibm.icu.util.ULocale" %>



<%
if(ctx.session==null||ctx.session.user==null||!(ctx.session.user.userlevel<=UserRegistry.ADMIN)) {

%>
<h1>Access denied. Admin use only.</h1>
<% } else { %>

XPathTable stuff.

XPT.count =
<%= ctx.sm.xpt.count() %>

Test XPT to the max, within SurveyTool.


<pre> 

FreeMem=<%= ctx.sm.freeMem() %>
<%
IntHash<String> ih = new IntHash<String>();
%>
max=<%= ih.MAX_SIZE %>

<%
for(int j=0;j<ih.MAX_SIZE;j++) {
    ih.put(j,CookieSession.cheapEncode(j));
    if(j%1000==0) {
        %> .. j=<%= j %> - ih.get(j)=<%= ih.get(j) %>   
        <%
    }
}
%>

ih.stats() = <%= ih.stats() %>
FreeMem=<%= ctx.sm.freeMem() %>

Test.

<%

for(int j=0;j<ih.MAX_SIZE;j++) {
    String expect = CookieSession.cheapEncode(j);
    String got=ih.get(j);
    if(!expect.equals(got)) {
        %>Error at <%= j %> expect <%= expect %> but got <%= got %>
        <%
    }
}
%>

Verified OK otherwise.

</pre>

<% } %>