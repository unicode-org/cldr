<%@ include file="stcontext.jspf" %><%-- setup 'ctx' --%>
<!-- <%@ include file="report.jspf" %>


<p class='hang'>Reports: 

<% 
    for(int i=0;i<reports.length;i++) {
%>
    <%= ((i>0)?" | ":"") %>
    <%= ctx.sm.getMenu(subCtx, section, reports[i], "Step "+(i+1)) %>
<%
    }
%>

</p>
-->