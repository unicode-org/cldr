    <%@ include file="report_top.jspf" %>

<%@ page import="com.ibm.icu.util.Currency" %>

<h2>Enter the name of the currency and the currency symbol used in this locale.</h2>

<%
//  Copy "x=___"  from input to output URL
Currency myCurr = Currency.getInstance(ctx.getLocale().toULocale());
String currencyCode = myCurr.getCurrencyCode();

subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));
SurveyForum.printSectionTableOpenShort(subCtx, thisBaseXpath);

SurveyForum.showXpathShort(subCtx, "//ldml/numbers/currencies/currency[@type=\""+currencyCode+"\"]/displayName");
SurveyForum.showXpathShort(subCtx, "//ldml/numbers/currencies/currency[@type=\""+currencyCode+"\"]/symbol");

SurveyForum.printSectionTableCloseShort(subCtx, thisBaseXpath);
%>
