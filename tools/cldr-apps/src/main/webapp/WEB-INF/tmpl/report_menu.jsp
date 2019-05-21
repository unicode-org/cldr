<%@ include file="/WEB-INF/jspf/stcontext.jspf" %>
<%
	/* if(ctx.session.user != null) { */   // uncomment to restrict to logged in users
%>

<p class='hang'>New Locale Generator
<%
WebContext subCtx = new WebContext(ctx);
subCtx.setQuery(SurveyMain.QUERY_LOCALE,ctx.localeString());
%>
<%=  subCtx.sm.getMenu(subCtx,subCtx.field(SurveyMain.QUERY_SECTION),	"r_steps","Enter/Edit Basic Locale Information") %>

</p>
<%
	/* } */   // uncomment to restrict to logged in users

%>
