    <%@ include file="report_top.jspf" %>

<h2>Day periods are used with 12 hour time formats: please provide them here.</h2>
<p>CLDR now has flexible day periods, and is not limited to just AM/PM. Note that if the categories are not AM/PM, the English will not be applicable ("null").
If your language may use 12 hour clocks, please translate the following, or see below for how to change the categories.</i></p>
<%
//  Copy "x=___"  from input to output URL

subCtx.openTable(); 

CLDRFile file = subCtx.cldrFile();

SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(file.getSupplementalDirectory());
DayPeriodInfo dayPeriods = supplementalData.getDayPeriods(file.getLocaleID());
LinkedHashSet<DayPeriodInfo.DayPeriod> items = new LinkedHashSet(dayPeriods.getPeriods());
String prefix = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"";

for (DayPeriodInfo.DayPeriod dayPeriod : items) {
    subCtx.showXpath(prefix + dayPeriod + "\"]");
}

subCtx.closeTable();
subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 

String rules = dayPeriods.toString().replace("<","&lt;").replace("\n","<br>");
%>
<p>The day period categories for this locale are:</p>
<blockquote><b>&nbsp; <%=rules%></b></blockquote>
<p>If these are incorrect, please <a target="_blank" href='http://unicode.org/cldr/trac/newticket'>file a ticket</a> to get the categories you need, as soon as possible.
For comparison, see the <a target="_blank" href="<%= ctx.base(request)+"?_=de&x=r_steps&step=day_periods" %>">German day periods</a>
or <a target="_blank" href="<%= ctx.base(request)+"?_=zh&x=r_steps&step=day_periods" %>">Chinese day periods</a>.
In your ticket, make sure that the periods need to cover the entire day in order, from 0:00 to 24:00, and do not overlap. That means that you can have cases like:
<ul>
<li>... earlyMorning &#x2264; 09:00 &#x2264; morning ...</li>
<li>... earlyMorning &#x2264; 09:00 &lt; morning ...</li>
<li>... earlyMorning &lt; 09:00 &#x2264; morning ...</li>
</ul>
<p>but <b>not</b> two &lt; signs around the same time, as in:</p>
<ul>
<li>... earlyMorning &lt; 09:00 &lt; morning ... </li>
</ul>
<p>It is easiest to take categories like the German ones, edit them, and include the modified version in the ticket.</p>
