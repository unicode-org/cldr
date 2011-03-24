<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%-- setup 'ctx' --%>
<%@ include file="/WEB-INF/jspf/report.jspf"  %>
<%@ page import="org.unicode.cldr.util.*" %>
</form> <!--  close the ST form -->
<%
/* set the '_' parameter, to the current locale */
subCtx.setQuery(SurveyMain.QUERY_LOCALE,ctx.localeString());
/* flush output (sync between subCtx' stream and JSP stream ) */
subCtx.flush();
%>
<%@ include file="/WEB-INF/jspf/debug_jsp.jspf" %>
<%!
	VettingViewerQueue vvq = new VettingViewerQueue();
%>

<%--
    <%@ include file="/WEB-INF/jspf/report_top.jspf" %>
--%>
<%
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
	%>
		<form action="<%= topCtx.url() %>" method="POST">
			<input type='hidden' value='t' name='VVFORCERESTART'/>
			<label>To regenerate this page, click <input type='submit' value='Force Restart'/></label>
		</form>
	<%
	
	VettingViewerQueue.Status status[] = new VettingViewerQueue.Status[1];
	boolean forceRestart = subCtx.hasField("VVFORCERESTART");
	subCtx.println(vvq.getVettingViewerOutput(subCtx,subCtx.getLocale(),status, forceRestart));
	subCtx.println("<br/> Status: " + status[0]);
	if(status[0]==VettingViewerQueue.Status.PROCESSING) {
		out.println("<meta http-equiv=\"refresh\" content=\"5\">");
	}
}

%>
<hr/>

<form> <!--  re-open the ST form (not used) -->