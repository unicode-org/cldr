    <%@ include file="report_top.jspf" %>

<h2> Enter the months of Gregorian calendar.</h2>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));
SurveyForum.printSectionTableOpenShort(subCtx, thisBaseXpath);
   for(int i=1;i<=12;i++) {
       SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\""+String.valueOf(i)+"\"]");

   }
SurveyForum.printSectionTableCloseShort(subCtx, thisBaseXpath);
%>
