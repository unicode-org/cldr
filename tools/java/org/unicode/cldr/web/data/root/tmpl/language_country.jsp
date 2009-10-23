    <%@ include file="report_top.jspf" %>

<%@ page import="com.ibm.icu.util.Currency" %>
<%@ page import="com.ibm.icu.util.ULocale" %>

<h2>Enter the name of the language and name of the country in your own language.</h2>
<p>Afterwards go to the each of following to fill out the priority items (marked with an alert icon):</p>
<ol>
<li><a href="<%= ctx.base(request)+"?_="+ctx.getLocale()+"&x=languages" %>">languages</a></li>
<li><a href="<%= ctx.base(request)+"?_="+ctx.getLocale()+"&x=scripts" %>">scripts</a></li>
<li><a href="<%= ctx.base(request)+"?_="+ctx.getLocale()+"&x=languages" %>">territories</a></li>
</ol>
<%
//  Copy "x=___"  from input to output URL
ULocale myLoc = ULocale.addLikelySubtags(ctx.getLocale().toULocale());

subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));
SurveyForum.printSectionTableOpenShort(subCtx, thisBaseXpath);

SurveyForum.showXpathShort(subCtx, "//ldml/localeDisplayNames/languages/language[@type=\""+myLoc.getLanguage()+"\"]");
SurveyForum.showXpathShort(subCtx, "//ldml/localeDisplayNames/territories/territory[@type=\""+myLoc.getCountry()+"\"]");

SurveyForum.printSectionTableCloseShort(subCtx, thisBaseXpath);
%>
