<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><!--  manage.jsp begin -->

<h3>Subpages</h3>

<%
String helpLink = ctx.getString("helpLink");
String helpName = ctx.getString("helpName");

        String doWhat = ctx.field(SurveyMain.QUERY_DO);

        //        ctx.println("The SurveyTool is in phase <b><span title='"+phase().name()+"'>"+phase().toString()+"</span></b> for version <b>"+getNewVersion()+"</b><br>" );

        if(ctx.session.user == null)  {
            boolean haveCookies = (ctx.getCookie(SurveyMain.QUERY_EMAIL)!=null&&ctx.getCookie(SurveyMain.QUERY_PASSWORD)!=null);
            ctx.println(ctx.session.user.name + " (" + ctx.session.user.org + ") ");
            if(!haveCookies && !ctx.hasField(SurveyMain.QUERY_SAVE_COOKIE)) {
                ctx.println(" <a class='notselected' href='"+ctx.url()+ctx.urlConnector()+SurveyMain.QUERY_SAVE_COOKIE+"=iva'><b>Remember Me!</b></a>");
            }
            ctx.print(" | ");
            ctx.sm.printMenu(ctx, doWhat, "listu", "My Account", SurveyMain.QUERY_DO);
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
        ctx.flush();
 %>
<!--  manage.jsp ends -->