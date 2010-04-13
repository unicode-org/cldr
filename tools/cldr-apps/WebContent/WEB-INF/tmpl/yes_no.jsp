    <%@ include file="/WEB-INF/jspf/report_top.jspf" %>

<h2>Enter the words for "yes" and "no" in your language. </h2>

<p>If your language has a single letter abbreviation for yes or no, you can enter that after the full word for yes or no, separating the two with a colon ":".  For languages that have upper and lower case letters, use only the lower case.
<%
//  Copy "x=___"  from input to output URL
subCtx.openTable(); 

subCtx.showXpath( "//ldml/posix/messages/yesstr");
subCtx.showXpath( "//ldml/posix/messages/nostr");

subCtx.closeTable(); subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 

%>
