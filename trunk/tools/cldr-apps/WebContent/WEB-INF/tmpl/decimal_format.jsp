<%@ include file="/WEB-INF/jspf/report_top.jspf" %>

<h2> Enter the pattern to be used for formatting regular numbers.</h2>

<%
//  Copy "x=___"  from input to output URL
subCtx.openTable(); 

subCtx.showXpath( "//ldml/numbers/decimalFormats/decimalFormatLength/decimalFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");

subCtx.closeTable();
subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 

%>
