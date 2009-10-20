    <%@ include file="report_top.jspf" %>

<h2> First, we need to get the characters used to write your language </h2>

<%
//  Copy "x=___"  from input to output URL
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));

SurveyForum.printSectionTableOpenShort(subCtx, "//ldml/characters/exemplarCharacters");
// Display a limited range of data
SurveyForum.showXpathShort(subCtx, "//ldml/characters/exemplarCharacters");
SurveyForum.showXpathShort(subCtx, "//ldml/characters/exemplarCharacters[@type=\"auxiliary\"]");
SurveyForum.showXpathShort(subCtx, "//ldml/characters/exemplarCharacters[@type=\"index\"]");

SurveyForum.printSectionTableCloseShort(subCtx, "//ldml/characters/exemplarCharacters");
%>
<p>The main exemplar characters are the ones most people would recognize as being the ones "in your language".
The Index characters are the ones that you would see as the index in a contact list, for example. For more information, see <a href='http://kwanyin.unicode.org:8080/cldr-apps/survey?_=hi&xpath=//ldml/characters/exemplarCharacters[@type=%22index%22]'>Exemplar Details</a></p>
