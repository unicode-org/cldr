    <%@ include file="report_top.jspf" %>

<h2> Enter narrow month abbreviations for the Gregorian calendar. </h2>

<p>Narrow month abbreviations are often used to create pocket sized calendars.  Narrow month abbreviations should be a single character and may be identical to the abbreviations for other months. If no value is entered, these will default to be the month numbers 1 through 12 for January through December, respectively.</p>

<%
//  Copy "x=___"  from input to output URL
subCtx.openTable(); 

   for(int i=1;i<=12;i++) {
       subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"stand-alone\"]/monthWidth[@type=\"narrow\"]/month[@type=\""+String.valueOf(i)+"\"]");
   }
   
subCtx.closeTable(); subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 

%>
