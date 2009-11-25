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

subCtx.openTable(); 

subCtx.showXpath( "//ldml/numbers/currencies/currency[@type=\""+currencyCode+"\"]/displayName");
subCtx.showXpath( "//ldml/numbers/currencies/currency[@type=\""+currencyCode+"\"]/symbol");

subCtx.closeTable(); subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 

%>
