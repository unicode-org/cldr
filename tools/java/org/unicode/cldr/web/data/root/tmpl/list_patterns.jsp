    <%@ include file="report_top.jspf" %>

<%@ page import="com.ibm.icu.util.Currency" %>
<%@ page import="com.ibm.icu.util.ULocale" %>

<h2>List Patterns are also new in 1.8</h2>
<p>These are used to represent lists. The <em>2</em> form is for a list of two items, like '<em>A</em> and <em>B</em>'.
The <em style="background-color: #F99">start</em>, <em style="background-color: #9F9">middle</em>, and <em style="background-color: #99F">end</em>
forms are used for 3 or more items, such as
'<em>A</em><span style="background-color: #F99">, </span><em>B</em><span style="background-color: #9F9">, </span><em>C</em><span style="background-color: #99F">, and </span><em>D</em>'.
The <em>start</em> form connects the first two items; the <em>end</em> connects the last two, and the <em>middle</em> connects successive middle ones
(for lists of four or more items). <i>If your language needs special forms for 3, 4, or other cases, please
<a target="_blank" href='http://unicode.org/cldr/trac/newticket'>file a ticket</a> to add them.</i></p>
<%
//  Copy "x=___"  from input to output URL

subCtx.openTable(); 

subCtx.showXpath(subCtx, "//ldml/listPatterns/listPattern/listPatternPart[@type=\"2\"]");
subCtx.showXpath(subCtx, "//ldml/listPatterns/listPattern/listPatternPart[@type=\"start\"]");
subCtx.showXpath(subCtx, "//ldml/listPatterns/listPattern/listPatternPart[@type=\"middle\"]");
subCtx.showXpath(subCtx, "//ldml/listPatterns/listPattern/listPatternPart[@type=\"end\"]");

subCtx.closeTable();
subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 

%>
