<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="org.unicode.cldr.web.*"
    %>
<%
String sid = request.getParameter("s");
CookieSession cs;

if(!SurveyMain.isSetup || 
		sid==null ||
		(cs=CookieSession.retrieve(sid))==null || 
		cs.user==null || 
		(cs.user.userlevel>UserRegistry.TC) ) {
	response.sendRedirect(request.getContextPath()+"/survey?msg=login_first");
	return;
}

cs.put("BASE_URL","http://unicode.org/cldr/apps/survey");

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>SurveyTool | Vetting Summary</title>
		<link rel='stylesheet' type='text/css' href='./surveytool.css' />
<%@ include file="/WEB-INF/tmpl/ajax_status.jsp" %>
<% /* calls: org.unicode.cldr.util.VettingViewer.getHeaderStyles() */ %>
</head>
<body>

<a href="<%=request.getContextPath()%>/survey">Return to the SurveyTool <img src='STLogo.png' style='float:right;' /></a>
<h2>Vetting Summary | <%= cs.user.name  %> | <%= cs.user.org %></h2>


<%
	VettingViewerQueue vvq = VettingViewerQueue.getInstance();
%>

		<form action="<%= request.getContextPath()+request.getServletPath() %>" method="POST">
			<input type='hidden' value='t' name='VVFORCERESTART'/>
			<input type='hidden' value='s' name='<%= sid %>'/>
			<label>To regenerate this page, click <input type='submit' value='Refresh Values'/></label>
		</form>
	<%
	
	VettingViewerQueue.Status status[] = new VettingViewerQueue.Status[1];
	boolean forceRestart = request.getParameter("VVFORCERESTART")!=null;
	
	%>
	<div id='vvupd'>
<%
    VettingViewerQueue.getInstance().writeVettingViewerOutput(null,cs,VettingViewerQueue.SUMMARY_LOCALE,status, 
				forceRestart?VettingViewerQueue.LoadingPolicy.FORCERESTART:VettingViewerQueue.LoadingPolicy.START,out);
	out.println("<br/> Status: " + status[0]);
	out.println("</div>");
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
                updateIf('vvupd',"<a href='<%= (request.getServletPath()+request.getContextPath()) %>&amp;vloaded=t'><i>Redirecting to your Vetting View...</i></a>");
                window.status= ('Done loading VV');
                clearInterval(vvId);
                vvId=-1;
                document.location = "<%= (request.getServletPath()+request.getContextPath()).replaceAll("&amp;","\\&") %>&vloaded=t";
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
		if(request.getParameter("vloaded")!=null) {
		    %>
		        vvId = setInterval(updateVv, 1000*5);
		        window.status = 'Checking on status of VV...';
		<%
		} /* end '!vloaded' */

	} /* end 'still waiting' */
%>

</script>


</body>
</html>