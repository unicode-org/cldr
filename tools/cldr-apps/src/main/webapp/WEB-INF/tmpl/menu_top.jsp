<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><!--  menu_top.jspf begin -->

<%!


    static String CALENDARS_ITEMS[] = SurveyMain.CALENDARS_ITEMS;
	static String METAZONES_ITEMS[] = SurveyMain.METAZONES_ITEMS;%><%
	WebContext subCtx = ctx;
	int n;
	String which = (String) subCtx.get("which");
	subCtx.addQuery(SurveyMain.QUERY_LOCALE, ctx.getLocale().toString());
%>
<%!
static void writeMenu(JspWriter jout, WebContext wCtx, SurveyMenus.Section sec, int covlev)  throws java.io.IOException {
    String which = (String) wCtx.get("which");
    PathHeader.PageId pageId = wCtx.getPageId();
    List<SurveyMenus.Section.Page> pages = new ArrayList<SurveyMenus.Section.Page>();
  //  jout.println("covlev="+covlev+"<br>");
    for(SurveyMenus.Section.Page p : sec) {
        // if coverage..
        if(true || p.getCoverageLevel(wCtx.getLocale())<=covlev) {
                    pages.add(p);
                   // jout.println(p.getKey() + " = " + p.getCoverageLevel(wCtx.getLocale()) + "<br>");
        }
    }
    
    boolean any = false;
       if(pageId!=null&& pageId.getSectionId() == sec.getSection()) {
           any = true;
       }
/*     for (int i = 0; !any && (i < pages.size()); i++) {
        if (pages.get(i).toString().equals(which))
            any = true;
    }
 */
    jout.println("<label class='"
            + (any ? "menutop-active" : "menutop-other") + "' >");
    
    if(!any) {
//        WebContext ssc = new WebContext(wCtx);
//        ssc.setQuery(SurveyMain.QUERY_SECTION, pages.get(0).toString());
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
        //ssc.setQuery(SurveyMain.QUERY_SECTION, key);
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
//        jout.print( " c="+pages.get(i).getCoverageLevel(wCtx.getLocale()));
        jout.println("</option>");
    }
    jout.println("</select>");
    jout.println("</label>");
}
static void writeMenu(JspWriter jout, WebContext wCtx, String title,
			String items[]) throws java.io.IOException {
		String which = (String) wCtx.get("which");
		boolean any = false;
		for (int i = 0; !any && (i < items.length); i++) {
			if (items[i].equals(which))
				any = true;
		}

		jout.println("<label class='"
				+ (any ? "menutop-active" : "menutop-other") + "' >");
		
		if(!any) {
			WebContext ssc = new WebContext(wCtx);
			ssc.setQuery(SurveyMain.QUERY_SECTION, items[0]);
			jout.println("<a href='"+ssc.url()+"' style='text-decoration: none;'>");
		}
		jout.println(title);
		if(!any) {
			jout.println("</a>");
		}

		jout.println("<select class='"
				+ (any ? "menutop-active" : "menutop-other")
				+ "' onchange='window.location=this.value'>");
		if (!any) {
			jout.println("<option selected value=\"\">Jump to...</option>");
		}
		for (int i = 0; i < items.length; i++) {
			WebContext ssc = new WebContext(wCtx);
			ssc.setQuery(SurveyMain.QUERY_SECTION, items[i]);
			jout.print("<option ");
			if (items[i].equals(which)) {
				jout.print(" selected ");
			} else {
				jout.print("value=\"" + ssc.url() + "\" ");
			}
			jout.print(">" + items[i]);
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
        	writeMenu(out,ctx, sec, workingCoverageValue);
    	}
		out.flush();
		ctx.flush();
		// commenting out easy steps until we have time to work on it more
		/* ctx.includeFragment("report_menu.jsp");  don't use JSP include, because of variables */
%>
	      <br><b>Review:</b> 
	      <% for (SurveyMain.ReportMenu m : SurveyMain.ReportMenu.values()) { 
	    	  final String theClass = request.getQueryString().contains(m.urlQuery())?"selected":"notselected";
	      	  String url;
	    	  if(m.hasQuery()) {
	      		  url = m.urlFull(ctx.base(), ctx.getLocale().getBaseName()).replace("&", "&amp;");
	    	  } else {
	    		  url = ctx.context()+"/v#"+m.urlStub()+"/"+ctx.getLocale();
	    	  }
		      %>
		      <label class='menutop-other'><a href="<%= url %>" class="<%= theClass %>"><%= m.display() %></a></label> 
	      <% } %>
        <br />
        <%
        	/* END NON JAVASCRIPT */
			out.flush();
			ctx.flush();
			// commenting out easy steps until we have time to work on it more
			
			/* ctx.includeFragment("report_menu.jsp");  don't use JSP include, because of variables */
			
%>
<!--  menu_top.jspf end -->