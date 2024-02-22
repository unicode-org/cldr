<%@page import="java.io.PrintWriter"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@page import="org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype"%>
<%@page import="org.unicode.cldr.web.SubtypeToURLMap"%>
<%@page import="java.util.TreeSet"%>
<%@page import="java.net.URL"%>
<%@page import="org.unicode.cldr.web.HttpStatusCache" %>
<%@page import="org.unicode.cldr.util.CLDRConfig" %>
<%@page import="org.unicode.cldr.util.StackTracker" %>
<%@page import="org.unicode.cldr.util.CLDRURLS" %>
<%
final String BASE = request.getContextPath() + request.getServletPath();
// allow ?flush=true - redirect 
if(request.getParameter("flush") != null) {
	final String recheck = request.getParameter("flush");
	%>
		<%
		if(recheck.startsWith("MAP")) {
			try {
				%><h1>Reloading URL map... </h1> <%
				SubtypeToURLMap map = SubtypeToURLMap.reload();
				if(map == null) {
					out.println("FAILED. Check for errors.");
				} else {
					out.println("SUCCESS!");
				}
			} catch(Throwable t) {
				%> 
					<a href="<%= BASE %>?flush=MAP">🔄 Try Again</a> | 
					<a href="<%= BASE %>">Cancel</a> (cache may work)
					<h1>Reload FAILED with stack:</h1><pre><%
				t.printStackTrace(new PrintWriter(out));
				out.println("</pre>");
				return; // do not auto refresh.
			}
		} else if(recheck.startsWith("http")) {
			%><h1>Flushing <%= recheck %> from cache..</h1> <%
			HttpStatusCache.flush(new URL(recheck));
		} else {
			%><h1>Flushing cache..</h1> <%
			HttpStatusCache.flush(null);
		}
		%>
	    <meta http-equiv="refresh" content="2;URL='<%= request.getContextPath() %><%= request.getServletPath() %>'" />    
		<p>(redirect in a couple seconds)</p>
		<img src="./loader.gif" alt="reloading.." />
	<%
	return;
}
%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>All Errors</title>
<link href="./surveytool.css" rel="stylesheet">
</head>
<body>


<h1>All error subtypes</h1>

<p>
	
</p>

<b>CLDR_SUBTYPE_URL</b> <%= CLDRURLS.toHTML(SubtypeToURLMap.getDefaultUrl()) %>
<br>

<a href="<%= BASE %>?flush=MAP">🔄 Reload Map</a>

<%
SubtypeToURLMap map = SubtypeToURLMap.getInstance();

if( map==null ) {
	out.println("<b>Could not load map.</b>");
	return;
} else {
	out.println("Map OK! (may be cached)<br>");
}
%>

<P>
	<i>Instructions</i>:  This shows the status of the subtype to URL mapping data.
	Each line here shows the CLDR error subtypes.<br />
			<b>Code</b> - this is the code <br/>
			<b>url</b> - this is the URL specified in the subtypeMapping.txt file <br/>
			<b>Status</b> - this shows whether the URL was fetched successfully. (200 indicates success.)
				Click the 'reload'  🔄 button to re-check the URL.
</P>

<a href="<%= BASE %>?flush=true">🔄Recheck all URLs </a>

<hr />

<div class="subtypemap">
<pre><%= SubtypeToURLMap.COMMENT + " " + SubtypeToURLMap.BEGIN_MARKER %></pre>

<%
for(final String u : map.getUrls()) {
        Integer checkStatus;
        checkStatus = HttpStatusCache.check(new URL(u));
    %>
<pre>#------------------</pre>
<pre><a title="HTTP:<%= checkStatus %>" href="<%= u %>"><%= u %></a></pre>
    <%
        if(! HttpStatusCache.isGoodStatus(checkStatus)) { %>
# URL failed to fetch: <%= checkStatus %> <a href="<%= BASE+"?flush="+u %>" title="flush">🔄</a><br/>
<% } else { %>
<!-- # URL OK! <%= checkStatus %> <a href="<%= BASE+"?flush="+u %>" title="flush">🔄</a><br/> -->
<% }
        for(final Subtype s : map.getSubtypesForUrl(u)) {
            %>
<!-- # <%= s.toString() %> -->
<b><pre title="<%= s.toString() %>"><%= s.name() %>,</pre></b> <%
        }
}

if(map.getUnhandledTypes().isEmpty()) {
%>
<pre>#------------------</pre>
<p><b># All types handled!</b></p>
<% } else { %>
<p>
<pre>#------------------</pre>
<h2># Missing these subtypes:</h2>
    
<% for(final Subtype sub : map.getUnhandledTypes()) {    %>
<pre  title="<%= sub.toString() %>"># <%= sub.name() %>,</pre>
<%
    }	
}
%>

<pre><%= SubtypeToURLMap.COMMENT + " " + SubtypeToURLMap.END_MARKER %></pre>


</body>
</html>