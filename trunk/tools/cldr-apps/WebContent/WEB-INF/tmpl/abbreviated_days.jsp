    <%@ include file="/WEB-INF/jspf/report_top.jspf" %>

<h2> Enter the abbreviated forms for days of the week.  </h2>

<p>If your language does not use abbreviations for weekday names, then enter the same values that you did for the wide forms.</p>

<%
//  Copy "x=___"  from input to output URL
subCtx.openTable(); 


subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day[@type=\"sun\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day[@type=\"mon\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day[@type=\"tue\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day[@type=\"wed\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day[@type=\"thu\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day[@type=\"fri\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day[@type=\"sat\"]");

subCtx.closeTable();
subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 
%>
