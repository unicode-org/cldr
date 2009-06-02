    <%@ include file="report_top.jspf" %>

<h2> Enter the formatting patterns to be used for dates (full, long, medium, and short).</h2>

<p>Times are formatted using a series of strings called patterns.  In each pattern, a series of letters is used to represent a portion of the time, such as the hours, minutes, seconds, am/pm, or time zone.  You can include additional characters from your language in the pattern if necessary.  However, if these additional characters are ASCII letters, they must be enclosed in single quote marks ( apostrophe ).  When specifying hours, use "H" or "HH" to denote use of a 24-hour clock, or use "h" or "hh" together with "a" (AM/PM) to denote use of a 12-hour clock.  The number of "H" or "h" characters used in the pattern denotes the minimum number of digits to use for the hours.  For example, at nine o&apos;clock AM, the pattern "h:mm" formats as "9:00", while the pattern "hh:mm" formats as "09:00".</p>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));
SurveyForum.printSectionTableOpenShort(subCtx, thisBaseXpath);

SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"full\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");

SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"long\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");

SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"medium\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");

SurveyForum.showXpathShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"short\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");

SurveyForum.printSectionTableCloseShort(subCtx, thisBaseXpath);
%>
