    <%@ include file="report_top.jspf" %>

<h2> Enter the names for day periods when using 12-hour clock ( AM and PM ). </h2>

<%
//  Copy "x=___"  from input to output URL
subCtx.openTable(); 

subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/am");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/pm");

subCtx.closeTable();
subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 
%>
