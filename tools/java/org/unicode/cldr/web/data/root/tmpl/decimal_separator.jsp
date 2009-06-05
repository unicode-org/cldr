<%@ include file="report_top.jspf" %>

<h2> Enter the character used as the decimal separator.</h2>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));

SurveyForum.printSectionTableOpenShort(subCtx, "//ldml/numbers/symbols/decimal");

SurveyForum.showXpathShort(subCtx, "//ldml/numbers/symbols/decimal");

SurveyForum.printSectionTableCloseShort(subCtx, "//ldml/numbers/symbols/decimal");

%>
