    <%@ include file="report_top.jspf" %>

<h2> Enter the abbreviated forms for months of Gregorian calendar.</h2>

<p>If your language does not use abbreviations for month names, then enter the same values that you did for the wide month names.</p>

<%
//  Copy "x=___"  from input to output URL
subCtx.openTable(); 

   for(int i=1;i<=12;i++) {
       subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\""+String.valueOf(i)+"\"]");

   }
   
subCtx.closeTable();
subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 
%>
