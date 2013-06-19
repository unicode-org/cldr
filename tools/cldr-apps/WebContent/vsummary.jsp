<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="org.unicode.cldr.web.*"
    %><%
String sid = request.getParameter("s");
CookieSession cs;

if(!SurveyMain.isSetup || 
		sid==null ||
		(cs=CookieSession.retrieve(sid))==null || 
		cs.user==null || 
		(cs.user.userlevel>=UserRegistry.LOCKED) ) {
	response.sendRedirect(request.getContextPath()+"/survey?msg=login_first");
	return;
}

if(!UserRegistry.userCanUseVettingSummary(cs.user)) {
    response.sendRedirect(request.getContextPath()+"/survey?msg=no_access");
    return;
}

String doneurl = (request.getContextPath()+request.getServletPath())+"?s="+sid;
String vloaded = doneurl+"&vloaded=t";
boolean isVloaded = request.getParameter("vloaded")!=null;
boolean needLoad = !isVloaded;
String BASE_URL= request.getContextPath() + "/survey";
cs.put("BASE_URL",BASE_URL);

%><!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>CLDR SurveyTool | Priority Items Summary</title>

<% if(!isVloaded) { %>
		<link rel='stylesheet' type='text/css' href='./surveytool.css' />
<%@ include file="/WEB-INF/tmpl/ajax_status.jsp" %>
<% } else { %>
		<link rel='stylesheet' type='text/css' href='http://unicode.org/repos/cldr/trunk/tools/cldr-apps/WebContent/surveytool.css' >
<% } %>

<% /* calls: org.unicode.cldr.util.VettingViewer.getHeaderStyles() */ %>
</head>
<% if(!isVloaded) { %>
<body onload='setTimerOn();' style='margin-top: 2em; padding-top: 2em;'>
<%@ include file="/WEB-INF/tmpl/stnotices.jspf" %>
<% } else { %>
<body>
<% } %>

<a href="<%=(isVloaded?(BASE_URL):(request.getContextPath()+"/survey"))%>">Return to the SurveyTool <img alt='(Survey Tool Logo)' src='http://unicode.org/repos/cldr/trunk/tools/cldr-apps/WebContent/STLogo.png' style='float:right;'></a>
<h2>Priority Items | <%= new java.util.Date() %></h2>


<%
	VettingViewerQueue vvq = VettingViewerQueue.getInstance();
%>
<% if(!isVloaded) { %>
	<div  class='pager'>
		<form action="<%= request.getContextPath()+request.getServletPath() %>" method="POST">
			<input type='hidden' value='t' name='VVFORCERESTART'/>
			<input type='hidden' name='s' value='<%= sid %>'/>
			<label>To regenerate this page, click <input type='submit' value='Refresh Values'/></label>
		</form>
	</div>
<% } %>
	<%
	
	VettingViewerQueue.Status status[] = new VettingViewerQueue.Status[1];
	boolean forceRestart = request.getParameter("VVFORCERESTART")!=null;
	
	%>
	<div id='vvupd'>
<%
    VettingViewerQueue.getInstance().writeVettingViewerOutput(null,cs,VettingViewerQueue.SUMMARY_LOCALE,status, 
				forceRestart?VettingViewerQueue.LoadingPolicy.FORCERESTART:VettingViewerQueue.LoadingPolicy.START,out);
	if(!isVloaded) out.println("<br/> Status: " + status[0]);
	 %>
	 </div>
	 <%
	if(status[0]==VettingViewerQueue.Status.PROCESSING || status[0]==VettingViewerQueue.Status.WAITING ) {
	//	out.println("<meta http-equiv=\"refresh\" content=\"5\">");
	String theUrl = request.getContextPath() +"/SurveyAjax?what=vettingviewer&_="+VettingViewerQueue.SUMMARY_LOCALE+"&s="+sid;
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
                //updateIf('vvupd',"<a href='<%= (request.getServletPath()+request.getContextPath()) %>&amp;vloaded=t'><i>Redirecting to your Priority Items View...</i></a>");
                window.status= ('Done loading VV');
                clearInterval(vvId);
                vvId=-1;
                document.location = "<%= (doneurl).replaceAll("&amp;","\\&") %>#done";
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
		if(needLoad) {
		    %>
		        vvId = setInterval(updateVv, 1000*5);
		        window.status = 'Checking on status of VV...';
		<%
		} /* end '!vloaded' */
%>
</script>
	<div id='vverr'></div>
	<div id='progress'></div>
	<div id='uptime'></div>
	<span id='visitors'></span>
	<div id='st_err'></div>
<% 
	} /* end 'still waiting' */
	else  if(!isVloaded) {
%>
	<%--  <h1><a href='<%= vloaded.replaceAll("&","&amp;") %>'>LGTM, clean it up.</a></h1> --%>
    <span id='visitors'></span>
<%
	}
%>




</body>
</html>
