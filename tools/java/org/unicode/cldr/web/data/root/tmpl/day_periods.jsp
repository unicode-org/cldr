    <%@ include file="report_top.jspf" %>

<h2>Day periods are used with 12 hour time formats: please provide them here.</h2>
<p>If your language can use 12 hour time formats, but doesn't normally use AM/PM, please see below for how to fix it.</i></p>
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
String rules = dayPeriods.toString().replace("<","&lt;");
%>
<p>The day period categories for this locale are currently:</p>
<blockquote><%=rules%></blockquote>
<p>If these are incorrect, please <a target="_blank" href='http://unicode.org/cldr/trac/newticket'>file a ticket</a> to get the categories you need.
For comparison, see the <a target="_blank" href="<%= ctx.base(request)+"?_=de&x=r_steps&step=day_periods" %>">German day periods</a>
or <a target="_blank" href="<%= ctx.base(request)+"?_=zh&x=r_steps&step=day_periods" %>">Chinese day periods</a>.
In your ticket, make sure that the periods need to cover the entire day, from 0:00 to 23:40, and do not overlap. That means that you can have:
<ol>
<li>... earlyMorning ≤ 09:00 ≤ morning ...</li>
<li>... earlyMorning ≤ 09:00 &lt; morning ...</li>
<li>... earlyMorning &lt; 09:00 ≤ morning ...</li>
</ol>
<p>but not:</p>
<ol>
<li>... earlyMorning < 09:00 < morning ... </li>
</ol>
<p>It is easiest to take rules like the German ones, edit them, and include in the ticket</p>
