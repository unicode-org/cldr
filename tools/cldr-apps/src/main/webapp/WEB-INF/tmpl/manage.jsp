<%@ include file="/WEB-INF/jspf/stcontext.jspf"%><!--  manage.jsp begin -->

<%
WebContext subCtx = new WebContext(ctx);
subCtx.removeQuery("do");

String helpLink = subCtx.getString("helpLink");
String helpName = subCtx.getString("helpName");

        String doWhat = subCtx.field(SurveyMain.QUERY_DO);

        //        subCtx.println("The SurveyTool is in phase <b><span title='"+phase().name()+"'>"+phase().toString()+"</span></b> for version <b>"+getNewVersion()+"</b><br>" );
%>

    <div class='manageSubpages'>
<%
        if(subCtx.session.user != null)  {
           %> <h3>My Account</h3>
           <div id='myUser'></div>
           <script>
            dojo.byId('myUser').appendChild(createUser( <%= subCtx.session.user.toJSONString() %> ));
           </script>
           
           <%
           boolean haveCookies = (subCtx.getCookie(SurveyMain.QUERY_EMAIL)!=null&&subCtx.getCookie(SurveyMain.QUERY_PASSWORD)!=null);
            if(false && !haveCookies && !subCtx.hasField(SurveyMain.QUERY_SAVE_COOKIE)) {
                subCtx.println(" <a class='notselected' href='"+subCtx.url()+subCtx.urlConnector()+SurveyMain.QUERY_SAVE_COOKIE+"=iva'>Log me in automatically next time</a>");
            }
            %>
            <a href='?do=listu'>My Account Settings</a>
              <a href='<%= request.getContextPath()   %>/lock.jsp?email=<%= subCtx.session.user.email %>'>Permanently disable my account! (account lock)</a>
              
              <h3>My Votes</h3>
              <% if(subCtx.session.user.canImportOldVotes()) { %>
                  <a href='<%= request.getContextPath() %>/v#oldvotes' >Import my Old Votes (from CLDR <%= SurveyMain.getOldVersion() %> and prior) </a>
              <% } %>
              <a href='<%= ctx.context("myvotes.jsp?user="+subCtx.session.user.id)+"&s="+subCtx.session.id %>'>See My&nbsp;Recent&nbsp;Activity</a>
                <a  href='<%= subCtx.jspUrl("upload.jsp"  )+ "&amp;s=" + subCtx.session.id %>'>Upload an XML file as my votes (bulk upload)</a>
            <h3>My Organization (<%= subCtx.session.user.getOrganization().getDisplayName() %>)</h3>
            <% if(UserRegistry.userCanUseVettingSummary(subCtx.session.user)) { %>
	            <a class="notselected" href='<%= subCtx.jspUrl("vsummary.jsp"  )+ "&amp;s=" + subCtx.session.id %>'>Priority Items Summary</a>
	        <% } 
            if(UserRegistry.userIsTC(subCtx.session.user)) {
                SurveyMain.printMenu(subCtx, doWhat, "list", "Manage&nbsp;Users", SurveyMain.QUERY_DO);
                // subCtx.print(" | ");
                //              if(this.phase()==Phase.VETTING || this.phase() == Phase.SUBMIT) {
            } else {
                if(UserRegistry.userIsVetter(subCtx.session.user)) {
                    SurveyMain.printMenu(subCtx, doWhat, "list", "List " + subCtx.session.user.org + " Users", subCtx.sm.QUERY_DO);
                } else if(UserRegistry.userIsLocked(subCtx.session.user)) {
                    subCtx.println("<b>LOCKED: Note: your account is currently locked. Please contact " + subCtx.session.user.org + "'s CLDR Technical Committee member.</b> ");
                }
            }
            // SurveyMain.printMenu(subCtx, doWhat, "disputed", "Dispute Resolution", SurveyMain.QUERY_DO); 
            if(SurveyMain.isPhaseReadonly()) {
                subCtx.println("<br>(The SurveyTool is in a read-only state, no changes may be made.)");
            } else if(SurveyMain.isPhaseVetting() 
                    && UserRegistry.userIsStreet(subCtx.session.user)
                    && !UserRegistry.userIsExpert(subCtx.session.user)) {
                subCtx.println("<br> (Note: in the Vetting phase, you may not submit new data.) ");
            } else if(SurveyMain.isPhaseClosed() && !UserRegistry.userIsTC(subCtx.session.user)) {
                subCtx.println("<br>(SurveyTool is closed to vetting and data submissions.)");
            }
            if((subCtx.session != null) && (subCtx.session.user != null) && (SurveyMain.isPhaseVettingClosed() && subCtx.session.user.userIsSpecialForCLDR15(null))) {
                subCtx.println("<br/><b class='selected'> you have been granted extended privileges for the CLDR "+subCtx.sm.getNewVersion()+" vetting period.</b><br>");
            }
            %>
         <h3>Forum</h3>
              <% if(ctx.session != null && ctx.session.user != null && UserRegistry.userIsTC(ctx.session.user) &&  ctx.sm.getSTFactory().haveFlags()) { %>
              	<a href="<%= subCtx.context("tc-flagged.jsp?s="+ctx.session.id) %>"><img src='flag.png' alt='flag' border='0' /> View Flagged Entries</a> 
              <% }  else { %>
              	<i>(no flagged items.)</i><br/>
              <% } %>
              <%= ctx.sm.fora.mainFeedIcon(ctx) %>
          <%
        }
          subCtx.flush();
         %>
         
         <h3>Informational</h3>
<%     if(subCtx.sm.dbUtils.hasDataSource()) { %>
            <a href="<%= subCtx.context("statistics.jsp") %>"/>Overall SurveyTool Statistics</a>
         <% } %>
            <a href="<%= subCtx.context("about.jsp") %>"/>About the SurveyTool Installation</a>
            <a href="<%= subCtx.context("browse.jsp") %>"/>Lookup a code or an xpath</a>
      </div>  
     

<!--  manage.jsp ends -->
