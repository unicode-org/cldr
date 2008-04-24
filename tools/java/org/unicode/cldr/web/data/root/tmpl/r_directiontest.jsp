<%@ include file="stcontext.jspf" %>
<%@ page import="org.unicode.cldr.util.*" %>
<%@ page import="org.unicode.cldr.test.*" %>
<%@ page import="com.ibm.icu.util.ULocale" %>
<%@ include file="report.jspf" %>



<%
%>

Hello! Here are a bunch o' directions:
<hr>

<table><tr><th>loc</th><th>dir</th><th>html</th><th>name</th></tr>
  <% for(String loc : SurveyMain.getLocales()) { ULocale l = new ULocale(loc); %>
    <tr style='border: 1px solid black;'>
        <th><%= loc %></th><td><%= ctx.sm.getDirectionalityFor(l.getBaseName()) %></td> 
                    <td><%= ctx.sm.getHTMLDirectionFor(l).replaceAll("rtl","<b>RTL</b>") %></td><td><%= l.getDisplayName() %></td>
    </tr>
  <% } %>
</table>

