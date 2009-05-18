<%@ include file="stcontext.jspf" %><%-- setup 'ctx' --%>
<%@ include file="report.jspf" %>

<%--
    <%@ include file="report_top.jspf" %>
--%>

<p> First, we need to get the characters used to write your language </p>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));

// Display a limited range of data
SurveyForum.showXpathShort(subCtx, "//ldml/characters/exemplarCharacters");

%>
