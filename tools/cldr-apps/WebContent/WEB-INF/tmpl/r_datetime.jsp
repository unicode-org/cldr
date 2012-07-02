<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%-- setup 'ctx' --%>
<%@ include file="/WEB-INF/jspf/report.jspf"  %>
<%@ page import="org.unicode.cldr.util.*" %>


<%
String type = ctx.field("calendar", "gregorian");
%>
<h3>Date/Time Review : <%=         com.ibm.icu.lang.UCharacter.toTitleCase(SurveyMain.BASELINE_LOCALE.toLocale(), type, null)  %></h3>
<%
CLDRFile englishFile = ctx.sm.getBaselineFile();
DateTimeFormats formats = new DateTimeFormats().set(ctx.sm.getSTFactory().make(ctx.getLocale(), true), type);
DateTimeFormats english = new DateTimeFormats().set(englishFile,type);
formats.addTable(english, out);
//formats.addDateTable(englishFile, out);
%>