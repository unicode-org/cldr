<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%-- setup 'ctx' --%>
<%@ include file="/WEB-INF/jspf/report.jspf"  %>
<%@ page import="org.unicode.cldr.util.*" %>


<%
%>
<h3>Review Numbers</h3>
<p>Please read the <a target='CLDR-ST-DOCS' href='http://cldr.unicode.org/translation/review-numbers'>instructions</a> before continuing.</p>
<%
// OLD CLDRFile englishFile = ctx.sm.getSTFactory().getOldFile(CLDRLocale.getInstance("en"));
// NEW
CLDRFile englishFile = ctx.sm.getDiskFactory().make("en",true);
CLDRFile nativeFile = ctx.sm.getSTFactory().make(ctx.getLocale(), true);
org.unicode.cldr.util.VerifyCompactNumbers.showNumbers(nativeFile, true, "EUR", out,ctx.sm.getSTFactory());
/* DateTimeFormats formats = new DateTimeFormats().set(, type);
DateTimeFormats english = new DateTimeFormats().set(englishFile,type);
formats.addTable(english, out);
formats.addDateTable(englishFile, out);
 */
%>