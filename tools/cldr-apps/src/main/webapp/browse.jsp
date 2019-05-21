<%@page import="org.unicode.cldr.web.SurveyMain"%>
<%@page import="org.unicode.cldr.web.WebContext"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"  import="com.ibm.icu.util.ULocale,org.unicode.cldr.util.*" %>
<!-- Copyright (C) 2012 IBM and Others. All Rights Reserved --> 
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <title>Unicode | CLDR | Browse the Survey Tool</title>
        <link rel='stylesheet' type='text/css' href='./surveytool.css' />
        <script type='text/javascript' src='//ajax.googleapis.com/ajax/libs/dojo/1.10.4/dojo/dojo.js'
            djConfig='parseOnLoad: true, isDebug: false'></script>
    </head>
    
    <body>
    <a href="survey">Return to the SurveyTool</a> |     <a href="index.jsp">CLDR Web Applications</a>
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
<script type="application/javascript">
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
	
	   lookup_xpath = function(from) {
	       var v = document.getElementById(from).value;
	       if(v.length==0) { return; }
	       var r = document.getElementById('xpath_answer');
	       r.innerHTML = '<i>Looking up xpath ' + v + '...</i>';
	       dojo.xhrGet({
	            url:"<%= request.getContextPath() %>/xpath_results.jsp?from="+ from + "&q=" + v,
	           handleAs:"json",
	           load: function(h){
	                  r.innerHTML = h.err;

	                  function updateIf(id, txt) {
	                	    var something = document.getElementById(id);
	                	    if(!txt || txt==-1) {
	                	    	txt = '';
	                	    }
	                	    if(something != null) {
	                	    	something.value = txt;
//	                	        something.innerHTML = txt;  // TODO shold only use for plain text
	                	    }
	                	}

	                  if(h.paths) {
                          updateIf('xpathid', h.paths.xpathid);
                          updateIf('xpath', h.paths.xpath);
                          updateIf('strid', h.paths.strid);
                          updateIf('pathheader', h.paths.pathheader);
	                  }
	            },
	            error: function(err, ioArgs){
	                r.innerHTML="Error: "+err.name + " <br> " + err.message;
	            }
	        });
	    };
	    
	
	
</script>
<table>
<tr>
<td style='border-right: 1px solid gray'>
What is... <input id='whatis' onchange="lookup_whatis()" style='font-size: x-large;'>
</td>
<td>
<b>xpath calculator - </b><br>
<label>XPath:<input id='xpath' onchange="lookup_xpath('xpath')"  size=160></label><br />
<label>PathHeader:<input id='pathheader' size=160 disabled=true></label><br />
<label>XPath strid:<input id='strid' onchange="lookup_xpath('strid')" size=32></label>
<label>XPathID (dec#)<input id='xpathid' onchange="lookup_xpath('xpathid')"  size=8></label>
<div id='xpath_answer' style='font-style: italic'>
enter a value and hit the tab key to begin
</div>
</td>
</tr>
</table>

<div id='whatis_answer'>
</div>
<hr>

<div class="helpHtml" style='margin: 2em'>
<h4>Instructions:</h4>
<b>What Is...</b>:  Enter a code or a portion of a name in the "What Is" field, such as "jgo" or "English", and press the Tab key.  A list of matching codes will be shown.

<p>

<b>XPath Calculator</b>:  

Enter an XPath, such as <tt>//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator</tt> into the XPath field, and press the Tab key. 
Or, enter an XPath strid, such as <tt>1d142c4be7841aa7</tt> into the XPath strid field and press the Tab key.  

The other fields (if applicable) will be filled in.


</div>

</body>
</html>