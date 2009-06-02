    <%@ include file="report_top.jspf" %>

<h2> Enter the formatting patterns to be used for dates (full, long, medium, and short).</h2>

<p>Dates are formatted using a series of strings called patterns.  In each pattern, a series of letters is used to represent a portion of the date, such as the month name, day number, day of the week, etc.  You can include additional characters from your language in the pattern if necessary.  However, if these additional characters are ASCII letters, they must be enclosed in single quote marks ( apostrophe ).</p>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));
SurveyForum.printSectionTableOpenShort(subCtx, thisBaseXpath);

SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"full\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");

SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"long\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");

SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");

SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"short\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");

SurveyForum.printSectionTableCloseShort(subCtx, thisBaseXpath);
%>
