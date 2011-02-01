<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><!--  menu_top.jspf begin -->

<%!static String CALENDARS_ITEMS[] = SurveyMain.CALENDARS_ITEMS;
	static String METAZONES_ITEMS[] = SurveyMain.METAZONES_ITEMS;%><%
	WebContext subCtx = ctx;
	int n;
	String which = (String) subCtx.get("which");
	String forum = ctx.getLocale().getLanguage();
	subCtx.addQuery(SurveyMain.QUERY_LOCALE, ctx.getLocale().toString());
%>
<%!static void writeMenu(JspWriter jout, WebContext wCtx, String title,
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
	}%>
<%
	if (!ctx.prefBool(SurveyMain.PREF_NOJAVASCRIPT)) {
		writeMenu(out, ctx, "Code Lists",
				PathUtilities.LOCALEDISPLAYNAMES_ITEMS);
		writeMenu(out, ctx, "Calendars", CALENDARS_ITEMS);
		writeMenu(out, ctx, "Time Zones", METAZONES_ITEMS);
		writeMenu(out, ctx, "Other Items", SurveyMain.OTHERROOTS_ITEMS);
		out.flush();
		ctx.flush();
		// commenting out easy steps until we have time to work on it more
		/* ctx.includeFragment("report_menu.jsp");  don't use JSP include, because of variables */
%>
   <%
   	} else {
   		/* NON JAVASCRIPT VERSION */
   %>
<p class='hang'> Code Lists: 
<%
	for (n = 0; n < PathUtilities.LOCALEDISPLAYNAMES_ITEMS.length; n++) {
			if (n > 0) {
%> | <%
	}
			out.flush();
			subCtx.sm.printMenu(subCtx, which,
					PathUtilities.LOCALEDISPLAYNAMES_ITEMS[n]);
		}
		subCtx.println("<p class='hang'> Calendars: ");
		for (n = 0; n < CALENDARS_ITEMS.length; n++) {
			if (n > 0) {
%> | <%
	}
			subCtx.sm.printMenu(subCtx, which, CALENDARS_ITEMS[n]);
		}

		subCtx.println("<p class='hang'> Metazones: ");
		for (n = 0; n < METAZONES_ITEMS.length; n++) {
			if (n > 0) {
%> | <%
	}
			subCtx.sm.printMenu(subCtx, which, METAZONES_ITEMS[n]);
		}

		subCtx.println("</p> <p class='hang'>Other Items: ");

		for (n = 0; n < SurveyMain.OTHERROOTS_ITEMS.length; n++) {
			if (!(SurveyMain.OTHERROOTS_ITEMS[n].equals("references") && // don't show the 'references' tag if not in a lang locale.
			!ctx.getLocale().getLanguage()
					.equals(ctx.getLocale().toString()))) {
				if (n > 0) {
%> | <%
	}
				subCtx.sm.printMenu(subCtx, which,
						SurveyMain.OTHERROOTS_ITEMS[n]);
%> <%
 	}
 		}
 %>| <a <%=ctx.atarget("st:supplemental")%> class='notselected' 
            href='http://unicode.org/cldr/data/charts/supplemental/language_territory_information.html#<%=
            ctx.getLocale().getLanguage() %>'>supplemental</a>

        </p>
        <%
        	out.flush();
        		ctx.flush();
                // commenting out easy steps until we have time to work on it more

        		/* ctx.includeFragment("report_menu.jsp");  don't use JSP include, because of variables */
        %><%
        	if (canModify) {
        %><p class='hang'>Forum: <%=forum%>
                <strong><a href='<%=SurveyForum.forumUrl(subCtx, forum)%>'>Forum: <%=forum%></a></strong>
             <br> <%=SurveyForum.forumFeedIcon(subCtx, forum)%>
            </p>
        <%
        	}
        %><%
        	/* END NON JAVASCRIPT */
        	}
        %>
<!--  menu_top.jspf end -->