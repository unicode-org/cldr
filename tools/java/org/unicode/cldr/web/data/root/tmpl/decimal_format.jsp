<%@ include file="report_top.jspf" %>

<p> Enter the pattern to be used for formatting regular numbers.</p>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));

SurveyForum.printSectionTableOpenShort(subCtx, "//ldml/numbers/decimalFormats/decimalFormatLength/decimalFormat/pattern");

SurveyForum.showXpathShort(subCtx, "//ldml/numbers/decimalFormats/decimalFormatLength/decimalFormat/pattern");

SurveyForum.printSectionTableCloseShort(subCtx, "//ldml/numbers/decimalFormats/decimalFormatLength/decimalFormat/pattern");

%>
