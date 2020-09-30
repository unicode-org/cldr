<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><!--  menu_top.jspf begin -->

<%
    /* TODO: is this entire file dead code? */
	WebContext subCtx = ctx;
	int n;
	String which = (String) subCtx.get("which");
	subCtx.addQuery(SurveyMain.QUERY_LOCALE, ctx.getLocale().toString());
%>
<%!
// writeMenu: 4 args, no relation to SurveyMain.writeMenu()
static void writeMenu(JspWriter jout, WebContext wCtx, SurveyMenus.Section sec, int covlev)  throws java.io.IOException {
    String which = (String) wCtx.get("which");
    PathHeader.PageId pageId = wCtx.getPageId();
    List<SurveyMenus.Section.Page> pages = new ArrayList<SurveyMenus.Section.Page>();
    for(SurveyMenus.Section.Page p : sec) {
        pages.add(p);
    }

    boolean any = false;
       if(pageId!=null&& pageId.getSectionId() == sec.getSection()) {
           any = true;
       }
    jout.println("<label class='"
            + (any ? "menutop-active" : "menutop-other") + "' >");

    if(!any) {
        jout.println("<a href='"+wCtx.vurl(wCtx.getLocale(), pages.get(0).getKey(), null, null)+"' style='text-decoration: none;'>");
    }
    jout.println(sec.toString());
    if(!any) {
        jout.println("</a>");
    }

    jout.println("<select class='"
            + (any ? "menutop-active" : "menutop-other")
            + "' onchange='window.location=this.value'>");
    if (!any) {
        jout.println("<option selected value=\"\">Jump to...</option>");
    }
    for (int i = 0; i < pages.size(); i++) {
        String key = pages.get(i).getKey().name();
        WebContext ssc = new WebContext(wCtx);
        jout.print("<option ");
        if(pages.get(i).getCoverageLevel(wCtx.getLocale())>covlev) {
            jout.print(" disabled ");
        }
        if (key.equals(which)) {
            jout.print(" selected ");
        } else {
            jout.print("value=\"" +wCtx.vurl(wCtx.getLocale(), pages.get(i).getKey(), null, null) + "\" ");
        }
        jout.print(">" + pages.get(i).toString());
        jout.println("</option>");
    }
    jout.println("</select>");
    jout.println("</label>");
}

%>
	<b>Sections:</b>
<%
       String covlev = ctx.getCoverageSetting();
       Level coverage = Level.COMPREHENSIVE;
       if(covlev!=null && covlev.length()>0) {
           coverage = Level.get(covlev);
       }
       String effectiveCoverageLevel = ctx
               .getEffectiveCoverageLevel(ctx.getLocale().toString());
       int workingCoverageValue = Level.get(effectiveCoverageLevel)
               .getLevel();


        for(SurveyMenus.Section sec : ctx.sm.getSTFactory().getSurveyMenus()) {
            writeMenu(out,ctx, sec, workingCoverageValue); // in this file, menu_top.jsp, above
    	}
		out.flush();
		ctx.flush();
%>
	      <br><b>Review:</b>
	      <% for (SurveyMain.ReportMenu m : SurveyMain.ReportMenu.values()) {
	    	  final String theClass = request.getQueryString().contains(m.urlQuery())?"selected":"notselected";
              String url = ctx.context()+"/v#"+m.urlStub()+"/"+ctx.getLocale();
		      %>
		      <label class='menutop-other'><a href="<%= url %>" class="<%= theClass %>"><%= m.display() %></a></label>
	      <% } %>
        <br />
        <%
        	/* END NON JAVASCRIPT */
			out.flush();
			ctx.flush();
%>
<!--  menu_top.jspf end -->