    <%@ include file="/WEB-INF/jspf/report_top.jspf" %>

<h2>Enter the formatting patterns to be used for GMT formatting.</h2>

<p>When displaying a time containing a time zone value, the GMT format is used in those instances where a specific translation for the time zone name such as "Pacific Standard Time" is not available.  In these cases, the GMT format is used, which shows the number of hours that the particular time zone is ahead or behind GMT.  In English, the GMT format for "Pacific Standard Time" would be "GMT-08:00".</p>

<p>There are two patterns that control GMT formatting.  The first one contains the portion of the pattern that contains the name or abbrevition for "GMT", as well as showing the position of the hour/minute offset relative to the other characters.  The hour and minute offset are replace the {0} in the pattern when formatting.  The second pattern shows how the hours and minutes should be formatted for positive and negative quantities.  For example, if the separator between hours and minutes in your locale was "." instead of ":", you would change the pattern to "+HH.mm;-HH.mm".

<%
//  Copy "x=___"  from input to output URL
subCtx.openTable(); 

subCtx.showXpath( "//ldml/dates/timeZoneNames/gmtFormat");
subCtx.showXpath( "//ldml/dates/timeZoneNames/hourFormat");

subCtx.closeTable();
subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 
%>
