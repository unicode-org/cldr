<%@ include file="/WEB-INF/jspf/report_top.jspf" %>

<h2> Select the default numbering system for your locale.  </h2>

<p>Most locales use the "Latn" system, which uses the ASCII digits 0 through 9 to represent numbers.</p>

<%
subCtx.openTable(); 

subCtx.showXpath( "//ldml/numbers/defaultNumberingSystem");

SurveyForum.printSectionTableCloseShort(subCtx, "//ldml/numbers/defaultNumberingSystem");

%>
