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
    
    if(false) { // HIDE THE REFRESH BUTTON
	%>
		<form action="<%= topCtx.url() %>" method="POST">
			<input type='hidden' value='t' name='VVFORCERESTART'/>
			<label>To regenerate this page, click <input type='submit' value='Refresh Values'/> <i>You must do this to reflect changes to your coverage level.</i></label>
		</form>
	<%
           } // END THE REFRESH BUTTON
    
	VettingViewerQueue.Status status[] = new VettingViewerQueue.Status[1];
	boolean forceRestart = subCtx.hasField("VVFORCERESTART");
	
	
	subCtx.println("<div id='vvupd'>");
        
       if(false) {    
        VettingViewerQueue.getInstance().writeVettingViewerOutput(subCtx,null,subCtx.getLocale(),status, 
                                    forceRestart?VettingViewerQueue.LoadingPolicy.FORCERESTART:VettingViewerQueue.LoadingPolicy.START,subCtx);


        subCtx.println("<br/> Status: " + status[0]);
       } else {
            StringBuffer sb = new StringBuffer();
            VettingViewerQueue.getInstance().writeVettingViewerOutput(subCtx.getLocale(), sb, subCtx, subCtx.session, false);
            subCtx.println(sb.toString());
       }
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
                window.status= ('Error loading VV content');
               st_err.innerHTML=json.err;
               st_err.className = "ferrbox";
	           	clearInterval(vvId);
	        	vvId=-1;
            }
            
            if(json.status == "READY" ) {
                updateIf('vvupd',"<a href='<%= topCtx.url() %>&amp;vloaded=t'><i>Redirecting to your Vetting View...</i></a>");
                window.status= ('Done loading VV');
                clearInterval(vvId);
                vvId=-1;
                document.location = "<%= topCtx.url().replaceAll("&amp;","\\&") %>&vloaded=t";
            } else {
                updateIf('vvupd',json.ret);
                window.status = ('VV still processing..');
            }
        },
        error: function(err, ioArgs){
            window.status = ('Temporary error loading VV - will retry');
        	updateIf('vverr','Couldn\'t load progress (Will retry): '+err.name + " <br> " + err.message);
//        	clearInterval(vvId);
///        	vvId=-1;
        }
    });
}

<% 
if(!ctx.hasField("vloaded")) {
    %>
        vvId = setInterval(updateVv, 1000*5);
        window.status = 'Checking on status of VV...';
<%
}
%>

</script>

<hr/> 
<% if(ctx.sm.isUnofficial()) { %>
<a href='<%= theUrl %>'><%= theUrl %></a>
<% }%>
<div id='vverr'></div>
<%
	}
}

%>
<hr/>

<form> <!--  re-open the ST form (not used) -->