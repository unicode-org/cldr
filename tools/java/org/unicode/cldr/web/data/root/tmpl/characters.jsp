    <%@ include file="report_top.jspf" %>

<h2> First, we need to get the characters used to write your language </h2>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));

SurveyForum.printSectionTableOpenShort(subCtx, "//ldml/characters/exemplarCharacters");
// Display a limited range of data
SurveyForum.showXpathShort(subCtx, "//ldml/characters/exemplarCharacters");

SurveyForum.printSectionTableCloseShort(subCtx, "//ldml/characters/exemplarCharacters");
%>
