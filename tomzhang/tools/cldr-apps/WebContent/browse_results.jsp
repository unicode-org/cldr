<%@page import="com.ibm.icu.dev.util.ElapsedTimer"%>
<%@page import="org.unicode.cldr.web.SurveyMain"%><%@page 
        import="org.unicode.cldr.web.WebContext, java.util.List"%><%@ page 
        language="java" contentType="text/html; charset=UTF-8"  
        import="com.ibm.icu.util.ULocale,org.unicode.cldr.util.*,org.unicode.cldr.web.*" %><%--
 Copyright (C) 2012 IBM and Others. All Rights Reserved  --%>
    <link rel='stylesheet' type='text/css' href='./surveytool.css' />
 <%
if(!SurveyMain.isSetup || SurveyMain.isBusted()) {
    %><i>Error: Survey Tool is not running.</i><%
    return;
}

String loc = request.getParameter("loc");
String q = WebContext.decodeFieldString(request.getParameter("q").trim());
if(q==null||q.length()==0) return;
ElapsedTimer et = new ElapsedTimer("results in");

if(q.startsWith("\\u") || q.startsWith("=")) {
	String q2 = 	com.ibm.icu.impl.Utility.unescape(q);
	if(q2.startsWith("=")) q2 = q2.substring(1);
	%><h3>XPATH search String match '<%= q %>' ('<%= q2 %>') </h3> <%
	SurveyMain sm = CookieSession.sm;
	int xpc=0;
	for(CLDRLocale loc2 : sm.getLocalesSet()) {
		boolean hdr = false;
		CLDRFile f = null;
		try {
			f = sm.getSTFactory().make(loc2.getBaseName(), false);
		} catch(Throwable t) {
			%><pre><%= t.toString()  %> loading <%= loc2.getDisplayName() %> - <%= loc2 %>
			<% t.printStackTrace(); %></pre><%
		}
		if(f==null) continue;
		for(String xp : f) {
			String str = f.getStringValue(xp);
			xpc++;
			if(str.contains(q2)) {
				if(!hdr) {
					%><h4><%= loc2 %> - <%= loc2.getDisplayName() %></h4>
					<ul><%
					hdr=true;
				}
				%><li><tt class='codebox'><%= xp %></tt> = <span class='value'><%= str %></span></li><%
			}
			if(hdr) {
				%></ul><%
			}
		}
	}
	%><hr><i>end of results - checked <%= xpc %> xpaths in <%= sm.getLocalesSet().size() %> locales - <%= et %></i><%
} else {

StandardCodes sc = StandardCodes.make();
%><h2>Results for <tt><%= q %></tt></h2>
<% q = q.toLowerCase();
 %>
<hr>
<h3>Code or Data Exact Matches</h3>
<%
   for(String type : sc.getAvailableTypes()) {
       for(String code : sc.getAvailableCodes(type)) {
           List<String> v = sc.getFullData(type, code);
           if(v==null||v.isEmpty()) continue;
           if(code.toLowerCase().equals(q)) {
               %><b><%= type %> : <span class='winner'><%= code %></span></b> = 
               <blockquote>
               <%
               if(v.isEmpty()) continue;
                for(String s : v) {
               %>
               <%= s %><br/>
               <% } %>
               </blockquote>
               <%
           } else {
               if(v==null||v.isEmpty()) continue;
               for(String s : v ) {
                   if(s!=null && s.toLowerCase().equals(q)) {
                       %><b><%= type %> : <span class='winner'><%= code %></span></b> = 
                               <blockquote>
                               <%
                                for(String s2 : v) {
                                    if(s2.toLowerCase().equals(q)) {
                               %><span class='winner'><% } else {  %><span><% } %>
                               <%= s %></span><br/>
                               <% } %>
                               </blockquote>
                               <%
                        continue;
                   }
               }
           }
       }
   }
%>
<hr>
<h3>Full Text Matches</h3>
<%
   for(String type : sc.getAvailableTypes()) {
       for(String code : sc.getAvailableCodes(type)) {
           List<String> v = sc.getFullData(type, code);
           StringBuilder allMatch = new StringBuilder();
           if(v!=null && !v.isEmpty()) 
	           for(String s:v) {
	               allMatch.append(s).append("\n");
	           }
           if(!code.toLowerCase().equals(q) && (code.toLowerCase().contains(q)  ||allMatch.toString().toLowerCase().contains(q)) ) {
               %><b><%= type %> : <span class='winner'><%= code %></span></b> = 
               <blockquote>
               <%
                if(v!=null && !v.isEmpty()) for(String s : v) {
               %>
               <%= s %><br/>
               <% } %>
               </blockquote>
               <%
           }
       }
   }
%><hr><i><%= et %></i><%
}
%>
