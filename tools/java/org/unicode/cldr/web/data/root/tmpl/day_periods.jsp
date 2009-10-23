    <%@ include file="report_top.jspf" %>

<h2> First, we need to get the characters used to write your language </h2>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));

SurveyForum.printSectionTableOpenShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods");

CLDRFile file = SurveyForum.getCLDRFile(subCtx);

SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(file.getSupplementalDirectory());
DayPeriodInfo dayPeriods = supplementalData.getDayPeriods(file.getLocaleID());
LinkedHashSet<DayPeriodInfo.DayPeriod> items = new LinkedHashSet(dayPeriods.getPeriods());
String prefix = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"";

for (DayPeriodInfo.DayPeriod dayPeriod : items) {
    SurveyForum.showXpathShort(subCtx, "//ldml/characters/exemplarCharacters");
}

SurveyForum.printSectionTableCloseShort(subCtx, "//ldml/characters/exemplarCharacters");
%>
<p>The main exemplar characters are the ones most people would recognize as being the ones "in your language".
The Index characters are the ones that you would see as the index in a contact list, for example. For more information, see <a href='http://kwanyin.unicode.org:8080/cldr-apps/survey?_=hi&xpath=//ldml/characters/exemplarCharacters[@type=%22index%22]'>Exemplar Details</a></p>
