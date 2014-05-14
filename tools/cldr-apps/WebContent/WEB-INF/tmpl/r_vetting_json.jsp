<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONWriter"%>
<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%-- setup 'ctx' --%>
<%@ include file="/WEB-INF/jspf/report.jspf"  %>
<%@ page import="org.unicode.cldr.util.*" %>
<%
try {
	/* set the '_' parameter, to the current locale */
	subCtx.setQuery(SurveyMain.QUERY_LOCALE,ctx.localeString());
	/* flush output (sync between subCtx' stream and JSP stream ) */
	subCtx.flush();
	
	
	
	VettingViewerQueue vvq = new VettingViewerQueue();
	subCtx.flush();
	out.flush();
	    // set up the 'x' parameter to the current secrtion (r_steps, etc)
	subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));
	ctx.setQuery(SurveyMain.QUERY_LOCALE,subCtx.field(SurveyMain.QUERY_LOCALE));
	/**
	 * Set query fields to be propagated to the individual steps 
	 */
	WebContext topCtx = (WebContext) request.getAttribute(WebContext.CLDR_WEBCONTEXT);
	topCtx.setQuery(SurveyMain.QUERY_SECTION, subCtx.field(SurveyMain.QUERY_SECTION));
	topCtx.setQuery(SurveyMain.QUERY_LOCALE, subCtx.field(SurveyMain.QUERY_LOCALE));
	
	if(subCtx.userId() == UserRegistry.NO_USER) {
	    out.println("<i>You must be logged in to use this function.</i>");
	} else 
	{
	    
	    
	    
		VettingViewerQueue.Status status[] = new VettingViewerQueue.Status[1];
		boolean forceRestart = subCtx.hasField("VVFORCERESTART");
	    StringBuffer sb = new StringBuffer();
	    VettingViewerQueue.getInstance().writeVettingViewerOutput(subCtx.getLocale(), sb, subCtx, subCtx.session, true);
	    subCtx.println(sb.toString());
	}
} catch (Throwable t) { // catch ALL errors, because we need to return JSON
	SurveyLog.logException(t, "when loading the Dashboard (r_vetting_json)", ctx);
	new JSONWriter(out).object().key("err").value("Exception: " + t.getMessage() + " while loading the Dashboard").key("err_code").value("E_INTERNAL").endObject();
	return; // cut off output
}
%>