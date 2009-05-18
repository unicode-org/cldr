<%@ include file="stcontext.jspf" %><%-- setup 'ctx' --%>
<%@ include file="report.jspf" %>

<%--
    <%@ include file="report_top.jspf" %>
--%>

<p> Enter the months of Gregorian calendar.</p>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));
   for(int i=1;i<=12;i++) {
// Display a limited range of data
       SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\""+String.valueOf(i)+"\"]");

   }
%>
