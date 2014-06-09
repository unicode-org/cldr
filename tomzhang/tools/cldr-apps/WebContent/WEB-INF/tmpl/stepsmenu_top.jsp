<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><!--  menu_top.jspf begin -->

<%! 
static String CALENDARS_ITEMS[] = SurveyMain.CALENDARS_ITEMS;
static String METAZONES_ITEMS[] = SurveyMain.METAZONES_ITEMS;
%><%
        WebContext subCtx = ctx;
        int n;
        String which = (String)subCtx.get("which");
        String forum = ctx.getLocale().getLanguage();
        subCtx.addQuery(SurveyMain.QUERY_LOCALE,ctx.getLocale().toString());
%>
<a href='<%= subCtx.url() %>' class='notselected'>Exit Easy Steps and return to <%= ctx.getLocale().getDisplayName(ctx.displayLocale) %></a>
<hr />

