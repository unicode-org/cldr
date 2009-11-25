    <%@ include file="report_top.jspf" %>

<h2> First, we need to get the characters used to write your language </h2>

<%
//  Copy "x=___"  from input to output URL
subCtx.openTable(); 

// Display a limited range of data
subCtx.showXpath( "//ldml/characters/exemplarCharacters");
subCtx.showXpath( "//ldml/characters/exemplarCharacters[@type=\"auxiliary\"]");
subCtx.showXpath( "//ldml/characters/exemplarCharacters[@type=\"index\"]");

subCtx.closeTable();
subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 

%>
<p>The main exemplar characters are the ones most people would recognize as being the ones "in your language".
The Index characters are the ones that you would see as the index in a contact list, for example. 
For more information, see <a target="_blank" href='http://kwanyin.unicode.org:8080/cldr-apps/survey?_=hi&xpath=//ldml/characters/exemplarCharacters[@type=%22index%22]'>Exemplar Details</a></p>
