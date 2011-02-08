<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%
   String title = (String)ctx.get("TITLE");
%>
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
    </head>
<body onload='this.focus(); top.focus(); top.parent.focus(); setTimerOn();'>
<div class='topnotices'>
<% 
//        if(/*!isUnofficial && */ 
//            ((ctx.session!=null && ctx.session.user!=null && UserRegistry.userIsAdmin(ctx.session.user))||
//                false)) {
//            ctx.print("<span class='admin' title='You're an admin!'>");
//            ctx.printHelpLink("/Admin",ctx.iconHtml("warn","Admin!")+"Administrator");
//            ctx.println("</span>");
//        }%><%
        if(SurveyMain.isUnofficial) { %>
		    <div class='unofficial' title='Not an official SurveyTool' >
		        <%= ctx.iconHtml("warn","Unofficial Site") %>Unofficial
		    </div>
       <% } %>
       <% if(SurveyMain.isPhaseBeta()) { %>
                <div class='beta' title='Survey Tool is in Beta' >
                    <%= ctx.iconHtml("warn","beta") %>
                    SurveyTool is in Beta -  Any data added here will NOT go into CLDR.
                </div>
       <% } %>
</div>
<div id='st_err'><!-- for ajax errs --></div>
<span id='progress'>
    <%= ctx.sm.getSpecialHeader(ctx) %>
</span>
<%= SurveyMain.SHOWHIDE_SCRIPT %>
