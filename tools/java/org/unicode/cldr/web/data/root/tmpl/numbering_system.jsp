<%@ include file="report_top.jspf" %>

<p> Select the default numbering system for your locale.  Most locales use the "Latn" system,
which uses the ASCII digits 0 through 9 to represent numbers.</p>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));

SurveyForum.printSectionTableOpenShort(subCtx, "//ldml/numbers/defaultNumberingSystem");

SurveyForum.showXpathShort(subCtx, "//ldml/numbers/defaultNumberingSystem");

SurveyForum.printSectionTableCloseShort(subCtx, "//ldml/numbers/defaultNumberingSystem");

%>
