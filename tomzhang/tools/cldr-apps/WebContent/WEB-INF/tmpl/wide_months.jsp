    <%@ include file="/WEB-INF/jspf/report_top.jspf" %>

<h2> Enter the months of Gregorian calendar.</h2>

<%
subCtx.openTable(); 

   for(int i=1;i<=12;i++) {
       subCtx.showXpath( "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\""+String.valueOf(i)+"\"]");

   }
   
subCtx.closeTable(); subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 

%>
