    <%@ include file="/WEB-INF/jspf/report_top.jspf" %>

<h2> Enter the narrow abbreviations for the names of the days of the week. </h2>

<p>Narrow abbreviations are often used in making small versions of calendars.  The abbreviations used are usually a single character and need not be unique.</p>

<%
subCtx.openTable(); 

subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"narrow\"]/day[@type=\"sun\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"narrow\"]/day[@type=\"mon\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"narrow\"]/day[@type=\"tue\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"narrow\"]/day[@type=\"wed\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"narrow\"]/day[@type=\"thu\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"narrow\"]/day[@type=\"fri\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"stand-alone\"]/dayWidth[@type=\"narrow\"]/day[@type=\"sat\"]");

subCtx.closeTable(); subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 

%>
