<%@ include file="/WEB-INF/jspf/stcontext.jspf" %>
<%@ page import="org.unicode.cldr.util.*" %>
<%@ page import="org.unicode.cldr.test.*" %>
<%@ page import="com.ibm.icu.util.ULocale" %>
<%@ include file="/WEB-INF/jspf/report.jspf" %>



<%
%>

Hello! Here are a bunch o' directions:
<hr>

<table><tr><th>loc</th><th>dir</th><th>html</th><th>name</th></tr>
  <% for(CLDRLocale loc : SurveyMain.getLocales()) { ULocale l = loc.toULocale(); %>
    <tr style='border: 1px solid black;'>
        <th><%= loc %></th><td><%= ctx.sm.getDirectionalityFor(loc) %></td> 
                    <td><%= ctx.sm.getHTMLDirectionFor(loc).replaceAll("rtl","<b>RTL</b>") %></td><td><%= l.getDisplayName() %></td>
    </tr>
  <% } %>
</table>

