<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%-- setup 'ctx' --%><%
String helpLink = ctx.getString("helpLink");
String helpName = ctx.getString("helpName");

%>
<table id='usertable' summary='header' border='0' cellpadding='0' cellspacing='0' style='border-collapse: collapse' "+
    			" width='100%' bgcolor='#EEEEEE'>
	<tr>
		<td>
			<a id='generalHelpLink' class='notselected'  href='<%= SurveyMain.GENERAL_HELP_URL %>'><%= SurveyMain.GENERAL_HELP_NAME %></a>
<%
        ctx.println("</td><td align='right'>");
    	String doWhat = ctx.field(SurveyMain.QUERY_DO);

    	if(ctx.session.user == null)  {
    		ctx.println("<form id='login' method='POST' action='"+ctx.base()+"'>");
    		%><%@ include file="/WEB-INF/tmpl/small_login.jsp" %><%
    		ctx.println("</form>");

    		String curSetting = ctx.getCoverageSetting();
    		if(curSetting!=null && !curSetting.equals(WebContext.COVLEV_RECOMMENDED)) {
    			ctx.println("<span style='border: 2px solid blue'>");
    		}
    		if(!(ctx.hasField("xpath")||ctx.hasField("forum")) && ( ctx.hasField(SurveyMain.QUERY_LOCALE) || ctx.field(SurveyMain.QUERY_DO).equals("disputed"))) {
    			WebContext subCtx = new WebContext(ctx);
    			for(String field : SurveyMain.REDO_FIELD_LIST) {
    				if(ctx.hasField(field)) {
    					subCtx.addQuery(field, ctx.field(field));
    				}
    			}
    			if(ctx.getLocale()!=null) {
    				subCtx.showCoverageSettingForLocale();	
    			} else {
    				subCtx.showCoverageSetting();
    			}
    		} else {
    			ctx.print(" <smaller>Coverage Level: "+curSetting+"</smaller>");
    		}
    		if(curSetting!=null && !curSetting.equals(WebContext.COVLEV_RECOMMENDED)) {
    			ctx.println("</span>");
    		}

            ctx.print(" | ");

            SurveyMain.printMenu(ctx, doWhat, "options", "Manage", SurveyMain.QUERY_DO);
    	} else {
    		boolean haveCookies = (ctx.getCookie(SurveyMain.QUERY_EMAIL)!=null&&ctx.getCookie(SurveyMain.QUERY_PASSWORD)!=null);
    		ctx.println("<span class='user_email'>&lt;"+ctx.session.user.email + "&gt;</span> <span class='user_name'>"+ctx.session.user.name +
    		        "</span> <span class='user_org'>(" + ctx.session.user.org + ")</span> ");
    		if(!haveCookies && !ctx.hasField(SurveyMain.QUERY_SAVE_COOKIE)) {
    			ctx.println(" <a class='notselected' href='"+ctx.url()+ctx.urlConnector()+SurveyMain.QUERY_SAVE_COOKIE+"=iva'><b>Log me in automatically next time</b></a>");
    		}
    		ctx.print(" | ");
    		String cookieMessage = haveCookies?"<!-- and Forget Me-->":"";
    		ctx.println("<a class='notselected' href='" + ctx.base() + "?do=logout'>Logout"+cookieMessage+"</a> | ");
    		String curSetting = ctx.getCoverageSetting();
    		if(!(ctx.hasField("xpath")||ctx.hasField("forum")) && ( ctx.hasField(SurveyMain.QUERY_LOCALE) || ctx.field(SurveyMain.QUERY_DO).equals("disputed"))) {
                // TODO: is this code ever run? Or is it dead code, including SurveyForum.forumLink?
                if(!curSetting.equals(WebContext.COVLEV_RECOMMENDED)) {
                    ctx.println("<span style='border: 2px solid blue'>");
                }
    			WebContext subCtx = new WebContext(ctx);
    			for(String field : SurveyMain.REDO_FIELD_LIST) {
    				if(ctx.hasField(field)) {
    					subCtx.addQuery(field, ctx.field(field));
    				}
    			}
    			if(ctx.hasField(SurveyMain.QUERY_LOCALE)) {
    				subCtx.showCoverageSettingForLocale();	
    			} else {
    				subCtx.showCoverageSetting();
    			}
                if(!curSetting.equals(WebContext.COVLEV_RECOMMENDED)) {
                    ctx.println("</span>");
                }
                if(ctx.getLocale()!=null) {
                                    String forum = ctx.getLocale().getLanguage();
                                    %><%=SurveyForum.forumLink(subCtx,forum)%><%
                }
    		} else {
    			ctx.print(" <smaller>Coverage: "+curSetting+"</smaller>");
    		}
            /*
             * It's possible to reach here, for example by going to http://localhost:8080/cldr-apps/survey?do=list
             * after logging in as admin
             */
    		ctx.print(" | ");
            if(ctx.session != null && ctx.session.user != null && UserRegistry.userIsTC(ctx.session.user) &&  ctx.sm.getSTFactory().haveFlags()) { 
            	ctx.println(ctx.iconHtml("flag", "(flagged items"));
            }
            ctx.sm.printMenu(ctx, doWhat, "options", "Manage", SurveyMain.QUERY_DO);
    		if(UserRegistry.userIsAdmin(ctx.session.user)) {
    			ctx.println("| <a href='" + ctx.context("AdminPanel.jsp") + "?vap=" + ctx.sm.vap + "'>[Admin Panel]</a>");
    			if(ctx.session.user.id == 1) {
    				ctx.println(" | <a href='" + ctx.base() + "?sql=" + ctx.sm.vap + "'>[Raw SQL]</a>");
    			}
    		}
    	}
    	ctx.println("</td></tr></table>");

    	ctx.flush();
 %>