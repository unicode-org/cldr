<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%-- setup 'ctx' --%>
<%@ include file="/WEB-INF/jspf/report.jspf"  %>
<%@ page import="org.unicode.cldr.util.*" %>

<%
if(!SurveyMain.isUnofficial) {
	return;
}
String xp = ctx.field("xp", "//ldml");
%>
hi, session=<%= ctx.session.id %>
<hr>
<a href='<%= request.getContextPath() %>/RefreshRow.jsp?_=<%= ctx.getLocale() %>&s=<%= ctx.session.id %>&xpath=<%= xp %>&json=t'>Clicky.</a>


<hr>

<%--
<%
 DataSection section2 = ctx.getSection(XPathTable.xpathToBaseXpath(xp),Level.COMPREHENSIVE.toString(),WebContext.LoadingShow.dontShowLoading);
%>
 --%>
<%--  <%= section2.toJSONString() %> --%>

<div id='DataSection'>
</div>

<script type='text/javascript'>
showRows('DataSection', '<%= xp %>', '<%= ctx.session.id %>');
</script>