<%@ include file="report_top.jspf" %>

<h2>Enter the character used as the grouping separator.</h2>

<%
//  Copy "x=___"  from input to output URL
subCtx.openTable(); 

subCtx.showXpath( "//ldml/numbers/symbols/group");

subCtx.closeTable();
subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 

%>
