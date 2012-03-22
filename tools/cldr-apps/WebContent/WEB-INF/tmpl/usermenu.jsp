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
    	if(helpLink != null) {
    		ctx.println(" | ");
    		if(helpName != null) {
    			ctx.printHelpLink(helpLink, helpName);
    		} else {
    			ctx.printHelpLink(helpLink, "Page&nbsp;Instructions");
    		}
    	}
    	ctx.println("</td><td align='right'>");
    	String doWhat = ctx.field(SurveyMain.QUERY_DO);

    	//        ctx.println("The SurveyTool is in phase <b><span title='"+phase().name()+"'>"+phase().toString()+"</span></b> for version <b>"+getNewVersion()+"</b><br>" );

    	if(ctx.session.user == null)  {
    		//ctx.println("<a class='notselected' href='" + ctx.jspLink("login.jsp") +"'>Login</a>");
    		ctx.println("<form id='login' method='POST' action='"+ctx.base()+"'>");
    		%><%@ include file="/WEB-INF/tmpl/small_login.jsp" %><%
    		ctx.println("</form>");
    		//            if(this.phase()==Phase.VETTING || this.phase() == Phase.SUBMIT) {
    		//                printMenu(ctx, doWhat, "disputed", "Disputed", QUERY_DO);
    		//                ctx.print(" | ");
    		//            }
    		ctx.print(" | ");
    		ctx.sm.printMenu(ctx, doWhat, "options", "My Options", SurveyMain.QUERY_DO);

    		String curSetting = ctx.getCoverageSetting();
    		if(!curSetting.equals(WebContext.COVLEV_RECOMMENDED)) {
    			ctx.println("<span style='border: 2px solid blue'>");
    		}
    		if(!(ctx.hasField("xpath")||ctx.hasField("forum")) && ( ctx.hasField(SurveyMain.QUERY_LOCALE) || ctx.field(SurveyMain.QUERY_DO).equals("disputed"))) {
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
    		} else {
    			ctx.print(" <smaller>Coverage Level: "+curSetting+"</smaller>");
    		}
    		if(!curSetting.equals(WebContext.COVLEV_RECOMMENDED)) {
    			ctx.println("</span>");
    		}

    	} else {
    		boolean haveCookies = (ctx.getCookie(SurveyMain.QUERY_EMAIL)!=null&&ctx.getCookie(SurveyMain.QUERY_PASSWORD)!=null);
    		ctx.println(ctx.session.user.name + " (" + ctx.session.user.org + ") ");
    		if(!haveCookies && !ctx.hasField(SurveyMain.QUERY_SAVE_COOKIE)) {
    			ctx.println(" <a class='notselected' href='"+ctx.url()+ctx.urlConnector()+SurveyMain.QUERY_SAVE_COOKIE+"=iva'><b>Remember Me!</b></a>");
    		}
    		ctx.print(" | ");
    		String cookieMessage = haveCookies?" and Forget Me":"";
    		ctx.println("<a class='notselected' href='" + ctx.base() + "?do=logout'>Logout"+cookieMessage+"</a> | ");
    		//            if(this.phase()==Phase.VETTING || this.phase() == Phase.SUBMIT || isPhaseVettingClosed()) {
    		//                ctx.sm.printMenu(ctx, doWhat, "disputed", "Disputed", QUERY_DO);
    		//                ctx.print(" | ");
    		//            }
    		ctx.sm.printMenu(ctx, doWhat, "options", "My Options", SurveyMain.QUERY_DO);
    		String curSetting = ctx.getCoverageSetting();
    		if(!curSetting.equals(WebContext.COVLEV_RECOMMENDED)) {
    			ctx.println("<span style='border: 2px solid blue'>");
    		}
    		if(!(ctx.hasField("xpath")||ctx.hasField("forum")) && ( ctx.hasField(SurveyMain.QUERY_LOCALE) || ctx.field(SurveyMain.QUERY_DO).equals("disputed"))) {
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
    		} else {
    			ctx.print(" <smaller>Coverage Level: "+curSetting+"</smaller>");
    		}
    		if(!curSetting.equals(WebContext.COVLEV_RECOMMENDED)) {
    			ctx.println("</span>");
    		}
    		ctx.print(" | ");
    		ctx.sm.printMenu(ctx, doWhat, "listu", "My Account", SurveyMain.QUERY_DO);
    		//ctx.println(" | <a class='deactivated' _href='"+ctx.url()+ctx.urlConnector()+"do=mylocs"+"'>My locales</a>");
    		if(UserRegistry.userIsAdmin(ctx.session.user)) {
    			ctx.println("| <a href='" + ctx.context("AdminPanel.jsp") + "?vap=" + ctx.sm.vap + "'>[Admin Panel]</a>");
    			if(ctx.session.user.id == 1) {
    				ctx.println(" | <a href='" + ctx.base() + "?sql=" + ctx.sm.vap + "'>[Raw SQL]</a>");
    			}
    		}
    		if(UserRegistry.userIsTC(ctx.session.user)) {
    			ctx.print(" | ");
    			ctx.sm.printMenu(ctx, doWhat, "list", "Manage " + ctx.session.user.org + " Users", SurveyMain.QUERY_DO);
    			ctx.print(" | ");
    			//              if(this.phase()==Phase.VETTING || this.phase() == Phase.SUBMIT) {
    		} else {
    			if(UserRegistry.userIsVetter(ctx.session.user)) {
    				ctx.print(" | ");
    				ctx.sm.printMenu(ctx, doWhat, "list", "List " + ctx.session.user.org + " Users", ctx.sm.QUERY_DO);
    			} else if(UserRegistry.userIsLocked(ctx.session.user)) {
    				ctx.println("<b>LOCKED: Note: your account is currently locked. Please contact " + ctx.session.user.org + "'s CLDR Technical Committee member.</b> ");
    			}
    		}
    		ctx.sm.printMenu(ctx, doWhat, "disputed", "Dispute Resolution", SurveyMain.QUERY_DO); 
    		if(SurveyMain.isPhaseReadonly()) {
    			ctx.println("<br>(The SurveyTool is in a read-only state, no changes may be made.)");
    		} else if(SurveyMain.isPhaseVetting() 
    				&& UserRegistry.userIsStreet(ctx.session.user)
    				&& !UserRegistry.userIsExpert(ctx.session.user)) {
    			ctx.println("<br> (Note: in the Vetting phase, you may not submit new data.) ");
    		} else if(SurveyMain.isPhaseClosed() && !UserRegistry.userIsTC(ctx.session.user)) {
    			ctx.println("<br>(SurveyTool is closed to vetting and data submissions.)");
    		}
    		ctx.println("<br/>");
    		if((ctx.session != null) && (ctx.session.user != null) && (SurveyMain.isPhaseVettingClosed() && ctx.session.user.userIsSpecialForCLDR15(null))) {
    			ctx.println("<b class='selected'> you have been granted extended privileges for the CLDR "+ctx.sm.getNewVersion()+" vetting period.</b><br>");
    		}
    	}
    	if(ctx.sm.dbUtils.hasDataSource()) {
    		ctx.println(" | <a class='notselected' href='"+ctx.jspUrl("statistics.jsp")+"'>Statistics</a>");
    	}
    	if(ctx.sm.isUnofficial && (ctx.session!=null&&ctx.session.user!=null)) {
    		ctx.println(" | <i class='scary'>Experimental:</i>&nbsp;");
    		ctx.println("<a class='notselected' href='"+ctx.jspUrl("upload.jsp"  )+ "&amp;s=" + ctx.session.id+"'>Upload XML</a>");
    		if(ctx.session.user.userlevel<=UserRegistry.TC) {
    			ctx.println("| <a class='notselected' href='"+ctx.jspUrl("vsummary.jsp"  ) +"'>Vetting Summary</a>");
    		}
    	}
    	ctx.println("</td></tr></table>");

    	ctx.flush();
 %>