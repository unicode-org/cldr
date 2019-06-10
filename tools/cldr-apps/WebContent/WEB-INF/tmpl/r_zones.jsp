<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%-- setup 'ctx' --%>
<%@ include file="/WEB-INF/jspf/report.jspf"  %>
<%@ page import="org.unicode.cldr.util.*" %>


<%
%>
<h3>Review Zones</h3>
<p>Please read the <a target='CLDR-ST-DOCS' href='http://cldr.unicode.org/translation/review-zones'>instructions</a> before continuing.</p>
<%
CLDRFile englishFile = ctx.sm.getDiskFactory().make("en",true);
CLDRFile nativeFile = ctx.sm.getSTFactory().make(ctx.getLocale(), true);
org.unicode.cldr.util.VerifyZones.showZones(null, englishFile, nativeFile, out);
/* DateTimeFormats formats = new DateTimeFormats().set(, type);
DateTimeFormats english = new DateTimeFormats().set(englishFile,type);
formats.addTable(english, out);
formats.addDateTable(englishFile, out);
 */
%>