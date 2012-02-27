<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%
   String title = (String)ctx.get("TITLE");
%>
</head>
<body onload='this.focus(); top.focus(); top.parent.focus(); setTimerOn();'>
	<div id="toparea">
    <img id="stlogo" src='<%= WebContext.context(request, "STLogo"+".png") %>' title="[ST Logo]" alt="[ST Logo]" />
    <h4 id="toptitle">Survey Tool <%= ctx.sm.phase().toString() %> <%= ctx.sm.getNewVersion() %>: <b><%
	    if(ctx.getLocale() != null) {
	        %><%= ctx.getLocale().getDisplayName(ctx.displayLocale) + " | " %><%
	    }
    %>
    <%= title %>
    </b></h4>
    </div>
<%@ include file="/WEB-INF/tmpl/stnotices.jspf" %>
<span id='progress'>
    <%= ctx.sm.getSpecialHeader(ctx) %>
</span>
