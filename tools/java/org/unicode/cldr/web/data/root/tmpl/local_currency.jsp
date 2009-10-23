    <%@ include file="report_top.jspf" %>

<%@ page import="com.ibm.icu.util.Currency" %>
<%@ page import="com.ibm.icu.util.ULocale" %>

<h2>Enter the name of the currency and the currency symbol used in this locale.</h2>
<p>Afterwards go to the each of following to fill out the priority items (marked with an alert icon):</p>
<ol>
<li><a target="_blank" href="<%= ctx.base(request)+"?_="+ctx.getLocale()+"&x=currencies" %>">currencies</a></li>
</ol>

<%
//  Copy "x=___"  from input to output URL
Currency myCurr = Currency.getInstance(ULocale.addLikelySubtags(ctx.getLocale().toULocale()));
String currencyCode;
if (myCurr == null)
   currencyCode = "XXX";
else
   currencyCode = myCurr.getCurrencyCode();

subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));
SurveyForum.printSectionTableOpenShort(subCtx, thisBaseXpath);

SurveyForum.showXpathShort(subCtx, "//ldml/numbers/currencies/currency[@type=\""+currencyCode+"\"]/displayName");
SurveyForum.showXpathShort(subCtx, "//ldml/numbers/currencies/currency[@type=\""+currencyCode+"\"]/symbol");

SurveyForum.printSectionTableCloseShort(subCtx, thisBaseXpath);
%>
