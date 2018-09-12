    <%@ include file="/WEB-INF/jspf/report_top.jspf" %>

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

String myLanguage = myLoc.getLanguage();
String myLanguageXpath = "//ldml/localeDisplayNames/languages/language[@type=\""+myLoc.getLanguage()+"\"]";
String myCountry = myLoc.getCountry();
String myCountryXpath = "//ldml/localeDisplayNames/territories/territory[@type=\""+myLoc.getCountry()+"\"]";

CLDRFile file = subCtx.resolvedFile();

if(myCountry!=null&&myCountry.length()>0&&null==file.getStringValue(myCountryXpath)) {
     %>	 <%= subCtx.iconHtml("stop",null) %> <i>The Survey Tool doesn't have any data for your territory or region, <tt><%= myCountry %></tt>.
	Please report this as a problem using the ' Report Problem in Tool' link at the 
	bottom of the page.</i>
	<hr>
     <%
	myCountry = null;
}

if(myLanguage!=null&&myLanguage.length()>0&&null==file.getStringValue(myLanguageXpath)) {
     %>	 <%= subCtx.iconHtml("stop",null) %> <i>The Survey Tool doesn't have any data for your language, <tt><%= myLanguage %></tt>.
	Please report this as a problem using the ' Report Problem in Tool' link at the 
	bottom of the page.</i>
	<hr>
     <%
	myLanguage = null;
}
     	



subCtx.openTable();
if(myLanguage==null||myLanguage.length()==0) {
	/* no language */
} else {
subCtx.showXpath(myLanguageXpath);
}
if(myCountry==null||myCountry.length()==0) {
	/* no territory */
} else {
subCtx.showXpath(myCountryXpath);
}
subCtx.closeTable();

subCtx.doneWithXpaths();

%>
