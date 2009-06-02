    <%@ include file="report_top.jspf" %>

<h2> Enter the names for day periods when using 12-hour clock ( AM and PM ). </h2>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));
SurveyForum.printSectionTableOpenShort(subCtx, thisBaseXpath);

SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/am");
SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/pm");

SurveyForum.printSectionTableCloseShort(subCtx, thisBaseXpath);
%>
