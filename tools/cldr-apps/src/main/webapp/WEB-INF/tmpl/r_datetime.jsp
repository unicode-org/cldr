<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%-- setup 'ctx' --%>
<%@ include file="/WEB-INF/jspf/report.jspf"  %>
<%@ page import="org.unicode.cldr.util.*" %>


<%
String type = ctx.field("calendar", "gregorian");
%>
<h3>Review Date/Times : <%=         com.ibm.icu.lang.UCharacter.toTitleCase(SurveyMain.BASELINE_LOCALE.toLocale(), type, null)  %></h3>
<p>Please read the <a target='CLDR-ST-DOCS' href='http://cldr.unicode.org/translation/date-time-review'>instructions</a> before continuing.</p>
<%
// OLD CLDRFile englishFile = ctx.sm.getSTFactory().getOldFile(CLDRLocale.getInstance("en"));
// NEW
CLDRFile englishFile = ctx.sm.getDiskFactory().make("en",true);
DateTimeFormats formats = new DateTimeFormats().set(ctx.sm.getSTFactory().make(ctx.getLocale(), true), type);
DateTimeFormats english = new DateTimeFormats().set(englishFile,type);
formats.addTable(english, out);
formats.addDateTable(englishFile, out);
formats.addDayPeriods(englishFile, out);
%>