<%@ include file="/WEB-INF/jspf/report_top.jspf" %>

<h2> Enter the character used as the decimal separator.</h2>

<%
//  Copy "x=___"  from input to output URL
subCtx.openTable(); 

subCtx.showXpath( "//ldml/numbers/symbols/decimal");

subCtx.closeTable();
subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 

%>
