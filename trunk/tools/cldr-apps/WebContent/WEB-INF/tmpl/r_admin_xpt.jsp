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


<pre> 

<%
CLDRLocale loc = CLDRLocale.getInstance("sv");
String path = "//ldml/localeDisplayNames/territories/territory[@type=\"HK\"]";
XMLSource src =  ctx.sm.dbsrcfac.getInstance(loc,false);
int xpid = ctx.sm.xpt.getByXpath(path);
int wxpth = ctx.sm.vet.getWinningXPath(xpid,loc);
String wxpthn = ctx.sm.xpt.getById(wxpth);
%>

path= <%= path %>  #<%= xpid %>
wp = <%=src.getWinningPath(path) %>
vwp = <%= src.getValueAtDPath(src.getWinningPath(path)) %>

--
wxpth= #<%= wxpth %>  <%= wxpthn %>

</pre>

<% } %>