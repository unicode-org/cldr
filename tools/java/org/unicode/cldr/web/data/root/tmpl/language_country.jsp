    <%@ include file="report_top.jspf" %>

<%@ page import="com.ibm.icu.util.Currency" %>
<%@ page import="com.ibm.icu.util.ULocale" %>

<h2>Enter the name of the language and name of the country in your own language.</h2>
<p>Afterwards go to the each of following to fill out the priority items (marked with an alert icon):</p>
<ol>
<li><a target="_blank" href="<%= ctx.urlToSection("languages") %>">languages</a></li>
<li><a target="_blank" href="<%= ctx.urlToSection("scripts") %>">scripts</a></li>
<li><a target="_blank" href="<%= ctx.urlToSection("territories") %>">territories</a></li>
</ol>
<%
//  Copy "x=___"  from input to output URL
ULocale myLoc = ULocale.addLikelySubtags(ctx.getLocale().toULocale());

subCtx.openTable();
subCtx.showXpath(new String[] 
                  {"//ldml/localeDisplayNames/languages/language[@type=\""+myLoc.getLanguage()+"\"]",
	               "//ldml/localeDisplayNames/territories/territory[@type=\""+myLoc.getCountry()+"\"]"});

subCtx.closeTable();

subCtx.doneWithXpaths();

%>
