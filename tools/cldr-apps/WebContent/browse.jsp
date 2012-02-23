<%@page import="org.unicode.cldr.web.SurveyMain"%>
<%@page import="org.unicode.cldr.web.WebContext"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"  import="com.ibm.icu.util.ULocale,org.unicode.cldr.util.*" %>
<!-- Copyright (C) 2012 IBM and Others. All Rights Reserved --> 
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <title>Unicode | CLDR | Browse the Survey Tool</title>
        <link rel='stylesheet' type='text/css' href='./surveytool.css' />
        <script type='text/javascript' src='<%= request.getContextPath()+"/dojoroot/dojo/dojo.js" %>'
            djConfig='parseOnLoad: true, isDebug: false'></script>
    </head>
    
    <body>
                <img src="STLogo.png" align="right" border="0" title="[logo]" alt="[logo]" />

<%
if(!SurveyMain.isSetup || SurveyMain.isBusted()) {
    response.sendRedirect(request.getContextPath()+"?stdown");
    return;
}
java.util.Locale baseLocale   = request.getLocale();
String ovrLoc = request.getParameter("loc");
ULocale pageLocale;
if ( ovrLoc!=null&&ovrLoc.length()>0) {
    pageLocale = ULocale.forLanguageTag(ovrLoc);
} else if(baseLocale!=null) {
    pageLocale = ULocale.forLocale(baseLocale);
} else {
    pageLocale = ULocale.US;
}
CLDRLocale loc = CLDRLocale.getInstance(pageLocale);

%>
<form action="<%= request.getContextPath()+request.getServletPath() %>" method="GET">
<label>Base Locale:
<input name='loc' value='<%= pageLocale.toLanguageTag() %>'></label><input type='submit' value='change'></form> <br/> 

<hr>
<script>
   lookup_whatis = function() {
	   var v = document.getElementById('whatis').value;
	   var r = document.getElementById('whatis_answer');
	   if(v.length==0) { r.innerHTML = ''; return; }
	   r.innerHTML = '<i>Looking up ' + v + '...</i>';
	   dojo.xhrGet({
	        url:"<%= request.getContextPath() %>/browse_results.jsp?loc=<%= pageLocale.getBaseName() %>&q=" + v,
/* 	        handleAs:"text",
 */	        load: function(h){
	        	  r.innerHTML = h;
	        },
	        error: function(err, ioArgs){
	            r.innerHTML="Error: "+err.name + " <br> " + err.message;
	        }
	    });
	};
</script>
What is... <input id='whatis' onchange="lookup_whatis()" style='font-size: x-large;'>

<div id='whatis_answer'>
</div>
<hr>

</body>
</html>