    <%@ include file="report_top.jspf" %>

<h2> Enter the abbreviated forms for months of Gregorian calendar.</h2>

<p>If your language does not use abbreviations for month names, then enter the same values that you did for the wide month names.</p>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));
SurveyForum.printSectionTableOpenShort(subCtx, thisBaseXpath);
   for(int i=1;i<=12;i++) {
       SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\""+String.valueOf(i)+"\"]");

   }
SurveyForum.printSectionTableCloseShort(subCtx, thisBaseXpath);
%>
