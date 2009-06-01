<%@ include file="report_top.jspf" %>

<p> Enter the pattern to be used for formatting currency amounts.</p>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));

SurveyForum.printSectionTableOpenShort(subCtx, "//ldml/numbers/currencyFormats/currencyFormatLength/currencyFormat/pattern");

SurveyForum.showXpathShort(subCtx, "//ldml/numbers/currencyFormats/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");

SurveyForum.printSectionTableCloseShort(subCtx, "//ldml/numbers/currencyFormats/currencyFormatLength/currencyFormat/pattern");

%>
