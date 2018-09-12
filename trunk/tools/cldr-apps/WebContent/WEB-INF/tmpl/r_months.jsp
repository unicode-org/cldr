    <%@ include file="/WEB-INF/jspf/report_top.jspf" %>

<p> Enter the months of Gregorian calendar.</p>

<%
subCtx.openTable(); 

   for(int i=1;i<=12;i++) {
       subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\""+String.valueOf(i)+"\"]");

   }
   
subCtx.closeTable(); subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 

%>
