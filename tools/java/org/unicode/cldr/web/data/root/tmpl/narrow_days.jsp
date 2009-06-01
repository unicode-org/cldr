    <%@ include file="report_top.jspf" %>

<p> Enter the narrow abbreviations for the names of the days of the week. Narrow abbreviations are often used in making small versions of calendars.  The abbreviations used are usually a single character and need not be unique.</p>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));
SurveyForum.printSectionTableOpenShort(subCtx, thisBaseXpath);

SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"narrow\"]/day[@type=\"sun\"]");
SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"narrow\"]/day[@type=\"mon\"]");
SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"narrow\"]/day[@type=\"tue\"]");
SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"narrow\"]/day[@type=\"wed\"]");
SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"narrow\"]/day[@type=\"thu\"]");
SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"narrow\"]/day[@type=\"fri\"]");
SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"narrow\"]/day[@type=\"sat\"]");

SurveyForum.printSectionTableCloseShort(subCtx, thisBaseXpath);
%>
