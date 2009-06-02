    <%@ include file="report_top.jspf" %>

<h2> Enter the abbreviated names for Gregorian calendar eras. </h2>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));
SurveyForum.printSectionTableOpenShort(subCtx, thisBaseXpath);

SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraAbbr/era[@type=\"0\"]");
SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraAbbr/era[@type=\"1\"]");

SurveyForum.printSectionTableCloseShort(subCtx, thisBaseXpath);
%>
