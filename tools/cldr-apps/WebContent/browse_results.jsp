<%@page import="org.unicode.cldr.web.SurveyMain"%><%@page 
        import="org.unicode.cldr.web.WebContext, java.util.List"%><%@ page 
        language="java" contentType="text/html; charset=UTF-8"  
        import="com.ibm.icu.util.ULocale,org.unicode.cldr.util.*" %><%--
 Copyright (C) 2012 IBM and Others. All Rights Reserved  --%>
    <link rel='stylesheet' type='text/css' href='./surveytool.css' />
 <%
if(!SurveyMain.isSetup || SurveyMain.isBusted()) {
    %><i>Error: Survey Tool is not running.</i><%
    return;
}

String loc = request.getParameter("loc");
String q = request.getParameter("q").trim();
if(q.length()==0) return;
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
%>
