    <%@ include file="/WEB-INF/jspf/report_top.jspf" %>

<h2> Enter the abbreviated names for Gregorian calendar eras. </h2>

<%
//  Copy "x=___"  from input to output URL
subCtx.openTable(); 

subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraAbbr/era[@type=\"0\"]");
subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraAbbr/era[@type=\"1\"]");

subCtx.closeTable();
subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 
%>
