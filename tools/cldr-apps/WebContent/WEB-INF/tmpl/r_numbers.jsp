<%@ include file="/WEB-INF/jspf/report_top.jspf" %>

<p> Enter the character used as the decimal separator.</p>

<%
subCtx.openTable(); 

subCtx.showXpath( "//ldml/numbers/symbols/decimal");

SurveyForum.printSectionTableCloseShort(subCtx, "//ldml/numbers/symbols/decimal");

%>
