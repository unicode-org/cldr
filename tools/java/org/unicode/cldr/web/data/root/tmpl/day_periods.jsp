    <%@ include file="report_top.jspf" %>

<h2>Day periods are used with 12 hour time formats: please provide them here.</h2>
<p>If your language can use 12 hour time formats, but doesn't normally use AM/PM, please
<a target="_blank" href='http://unicode.org/cldr/trac/newticket'>file a ticket</a> to get the categories you need.</i> 
For comparison see the <a target="_blank" href="<%= ctx.base(request)+"?_=de&x=r_steps&step=day_periods" %>">German day periods</a>. 
Each period needs to have an associate time span (such as 9:00 to 12:00).</p>
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
    SurveyForum.showXpathShort(subCtx, prefix + dayPeriod + "\"]");
}

SurveyForum.printSectionTableCloseShort(subCtx, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods");
%>