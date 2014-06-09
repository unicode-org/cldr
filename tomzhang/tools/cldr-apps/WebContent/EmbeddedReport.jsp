<%@page import="com.ibm.icu.dev.util.ElapsedTimer"%>
<%@page import="java.io.PrintWriter,org.unicode.cldr.web.*"%><%@ page
	language="java" contentType="text/html; charset=UTF-8"
	import="com.ibm.icu.util.ULocale,org.unicode.cldr.util.*,org.json.*"%>
	<%--  Copyright (C) 2012-2014 IBM and Others. All Rights Reserved  --%>
<%
	WebContext ctx = new WebContext(request, response);
	ElapsedTimer et = new ElapsedTimer();
	String what = request.getParameter(SurveyAjax.REQ_WHAT);
	String sess = request.getParameter(SurveyMain.QUERY_SESSION);
	String loc = request.getParameter(SurveyMain.QUERY_LOCALE);

	CookieSession cs = CookieSession.retrieve(sess);
	
	if(cs==null) {
		%><b>Invalid or expired session (try reloading the page)</b><%
		return;
	}
	
	ctx.session = cs;
	ctx.sm = cs.sm;

	//locale can have either - or _
	loc = (loc == null) ? null : loc.replace("-", "_");

	CLDRLocale l = SurveyAjax.validateLocale(new PrintWriter(out), loc);
	if (l == null)
		return;
	ctx.setLocale(l);
%>	
<%
	out.flush();
	request.setAttribute(WebContext.CLDR_WEBCONTEXT, ctx);
	ctx.doReport(request.getParameter("x"));
	ctx.flush();
%>
