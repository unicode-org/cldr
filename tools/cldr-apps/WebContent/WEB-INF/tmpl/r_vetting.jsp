<%@page import="javax.net.ssl.SSLEngineResult.Status"%>
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
			<label>To regenerate this page, click <input type='submit' value='Refresh Values'/></label>
		</form>
	<%
	
	VettingViewerQueue.Status status[] = new VettingViewerQueue.Status[1];
	boolean forceRestart = subCtx.hasField("VVFORCERESTART");
	
	
	subCtx.println("<div id='vvupd'>");
	subCtx.println(VettingViewerQueue.getInstance().getVettingViewerOutput(subCtx,null,subCtx.getLocale(),status, 
				forceRestart?VettingViewerQueue.LoadingPolicy.FORCERESTART:VettingViewerQueue.LoadingPolicy.START));
	subCtx.println("<br/> Status: " + status[0]);
	subCtx.println("</div>");
	if(status[0]==VettingViewerQueue.Status.PROCESSING || status[0]==VettingViewerQueue.Status.WAITING ) {
	//	out.println("<meta http-equiv=\"refresh\" content=\"5\">");
	String theUrl = request.getContextPath() +"/SurveyAjax?what=vettingviewer&_="+subCtx.getLocale()+"&s="+ctx.session.id;
	 %>
	 <noscript>
	 	<i>You must manually reload this page.</i>
	 </noscript>
<script type='text/javascript'>

var vvId = -1;
function updateVv() {
	if(vvId != -1) 
    dojo.xhrGet({
        url: "<%=  theUrl %>",
        handleAs:"json",
        load: function(json){
            var st_err =  document.getElementById('vverr');
            if(json.err.length > 0) {
               st_err.innerHTML=json.err;
               st_err.className = "ferrbox";
	           	clearInterval(vvId);
	        	vvId=-1;
            }
            
            updateIf('vvupd',json.ret);
            if(json.status == "READY" ) {
            	clearInterval(vvId);
            	vvId=-1;
            	updateIf('vverr',"Loaded.");
            }
        },
        error: function(err, ioArgs){
        	updateIf('vverr','Viewer failed (reload to retry): '+err.name + " <br> " + err.message);
        	clearInterval(vvId);
        	vvId=-1;
        }
    });
}

vvId = setInterval(updateVv, 1000*5);


</script>

<hr/> 
<% if(ctx.sm.isUnofficial) { %>
<a href='<%= theUrl %>'><%= theUrl %></a>
<% }%>
<div id='vverr'></div>
<%
	}
}

%>
<hr/>

<form> <!--  re-open the ST form (not used) -->