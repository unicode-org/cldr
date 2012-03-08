<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%
   String title = (ctx==null)?"?":((String)ctx.get("TITLE"));
%>
<!--  st_top.jsp --></head>
<body onload='this.focus(); top.focus(); top.parent.focus(); setTimerOn();'>
	<div id="toparea">
    <img id="stlogo" src='<%= WebContext.context(request, "STLogo"+".png") %>' title="[ST Logo]" alt="[ST Logo]" />
    <h4 id="toptitle">Survey Tool <%= ctx.sm.phase().toString() %> <%= ctx.sm.getNewVersion() %>: <b><%
	    if(ctx!=null && ctx.getLocale() != null) {
	        %><%= ctx.getLocale().getDisplayName(ctx.displayLocale) + " | " %><%
	    }
    %>
    <%= title %>
    </b></h4>
    </div>
<%@ include file="/WEB-INF/tmpl/stnotices.jspf" %>
<!-- end st_top.jsp -->