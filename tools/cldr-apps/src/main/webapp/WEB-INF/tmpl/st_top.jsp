<%@page import="com.ibm.icu.lang.UCharacter"%>
<!--  st_top.jsp -->
<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%
   String title;
   String bodyClass;
   boolean isScrollingContent = false;
   if(ctx==null) {
	   title = "?";
	   bodyClass = "stUnknown";
   } else {
       title = (String) ctx.get("TITLE");
	   if(ctx.getPageId()!=null) {
		   bodyClass = "claro";
		   isScrollingContent = true;
	   } else {
		   bodyClass = "stNormalPage";
		   isScrollingContent = false;
	   }
   }
%>
</head>
<body class='<%= bodyClass %>'>
<%
if(ctx!=null) {
%>
	<div id="toparea">
    <img id="stlogo" width="44" height="48" src='<%= WebContext.context(request, "STLogo"+".png") %>' title="[ST Logo]" alt="[ST Logo]" />
    <div id="toptitle" title='Phase: <%= ctx.sm.phase().toString() %>'>
        <span class='title-cldr'>CLDR <%= ctx.sm.getNewVersion() %> Survey Tool
        <%=  (ctx.sm.phase()!=SurveyMain.Phase.SUBMIT)?ctx.sm.phase().toString():"" %>
        : </span>

    <% CLDRLocale toplocale = ctx.getLocale();
        if(toplocale!=null) { 
            WebContext subCtx2 = (WebContext)ctx.clone();
            subCtx2.addQuery(SurveyMain.QUERY_LOCALE,toplocale.toString());
        %>

        <a class='locales' href="<%= ctx.base() %>">Locales</a> &nbsp;

        <%
        int n = ctx.docLocale.length; // how many levels deep
        int i,j;
         for(i=(n-1);i>0;i--) {
            boolean canModifyL = ctx!=null && ctx.session!=null && ctx.session.user != null && UserRegistry.userCanModifyLocale(ctx.session.user,ctx.docLocale[i]);
            ctx.print("&raquo;&nbsp; <a title='"+ctx.docLocale[i]+"' class='notselected' href=\"" + ctx.vurl(ctx.docLocale[i]) + 
                "\">");
            ctx.print(SurveyMain.decoratedLocaleName(ctx.docLocale[i],ctx.docLocale[i].getDisplayName(),""));
            ctx.print("</a> ");
        }
        boolean canModifyL = false&&UserRegistry.userCanModifyLocale(ctx.session.user,ctx.getLocale());
        %>&raquo;&nbsp;
        <span title='<%= ctx.getLocale() %>' class='curLocale'>
            <a href='<%= ctx.vurl(ctx.getLocale()) %>' class='notselected' ><%= ctx.getLocale().getDisplayName()+(canModifyL?SurveyMain.modifyThing(ctx):"") %></a>
        </span>
<%
        CLDRLocale dcParent = ctx.sm.getSupplementalDataInfo().getBaseFromDefaultContent(toplocale);
        CLDRLocale dcChild = ctx.sm.getSupplementalDataInfo().getDefaultContentFromBase(ctx.getLocale());
        if (dcChild != null) {
            String dcChildDisplay = ctx.getLocaleDisplayName(dcChild);
            ctx.println("<span class='dcbox'>" +
            "= "+dcChildDisplay+
                    "<a class='dchelp' target='CLDR-ST-DOCS' href='http://cldr.unicode.org/translation/default-content'>content</a>" 
                 + "</span>");
        }

    	if(ctx.sm.getReadOnlyLocales().contains(ctx.getLocale())) {
    		String comment = SpecialLocales.getComment(ctx.getLocale());
    		if(comment==null) comment = "Editing of this locale has been disabled by the SurveyTool administrators.";
    		ctx.println(ctx.iconHtml("lock", comment));
    	}
    	
        if(title!=null&&!title.trim().isEmpty()) {%>
        |
    <% } } %>

        <span class='normal-title'><%= title %></span>
    </div>
    </div>
<%
}
%>
<%@ include file="/WEB-INF/tmpl/stnotices.jspf" %>
<!-- end st_top.jsp -->
