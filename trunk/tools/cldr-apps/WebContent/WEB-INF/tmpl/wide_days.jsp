    <%@ include file="/WEB-INF/jspf/report_top.jspf" %>

<h2> Enter the names of the days of the week. </h2>

<%
subCtx.openTable(); 

subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"sun\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"mon\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"tue\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"wed\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"thu\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"fri\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"sat\"]");

subCtx.closeTable(); subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 

%>
