<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%><%@ include file="/WEB-INF/jspf/stcontext.jspf" %>

<!--  this is generalinfo.jsp  -->


 <li><i><font size='4'>Be sure to read </font>
<font size='4'><a href='<%= SurveyMain.GENERAL_HELP_URL %>'><%= SurveyMain.GENERAL_HELP_NAME %></a>
</font><font size='4'>     once before going further.</font></i></li> 
<!-- <li> <font size='4'><i>Consult the Page Instructions if you have questions on any page.</i></font> 
</li> --> </ul>


<%-- 
// what should users be notified about?
         if(isPhaseSubmit() || isPhaseVetting() || isPhaseVettingClosed()) {
             CLDRLocale localeName = ctx.getLocale();
             String groupName = localeName.getLanguage();
             
             int genCount = externalErrorCount(localeName);
             if(genCount > 0) {
                 ctx.println("<h2>Errors which need your attention:</h2><a href='"+externalErrorUrl(groupName)+"'>Error Count ("+genCount+")</a><p>");
             } else if(genCount < 0) {
                 ctx.println("<!-- (error reading the counts file.) -->");
             }
             int orgDisp = 0;
             if(ctx.session.user != null) {
                 orgDisp = vet.getOrgDisputeCount(ctx.session.user.voterOrg(),localeName);
                 
                 if(orgDisp > 0) {
                     
                     ctx.print("<h4><span style='padding: 1px;' class='disputed'>"+(orgDisp)+" items with conflicts among "+ctx.session.user.org+" vetters.</span> "+ctx.iconHtml("disp","Vetter Dispute")+"</h4>");
                     
                     Set<String> disputePaths = vet.getOrgDisputePaths(ctx.session.user.voterOrg(), localeName);
                     Map<String,Set<String>> odItems = new TreeMap<String,Set<String>>();
                     for(String path : disputePaths) {
                         String theMenu = PathUtilities.xpathToMenu(path);
                         if(theMenu != null) {
                             Set<String> paths = odItems.get(theMenu);
                             if(paths==null) {
                                 paths=new TreeSet<String>();
                                 odItems.put(theMenu,paths);
                             }
                             paths.add(path);
                         }
                     }
                     WebContext subCtx = (WebContext)ctx.clone();
                     //subCtx.addQuery(QUERY_LOCALE,ctx.getLocale().toString());
                     subCtx.removeQuery(QUERY_SECTION);
                     for(Map.Entry<String,Set<String>> e : odItems.entrySet()) {
                         //printMenu(subCtx, "", e.getKey());
                         ctx.println("<h3>"+e.getKey()+"</h3>");
                         ctx.println("<ol>");
                         for(String path:e.getValue()) {
                             ctx.println("<li>"+
                                         "<a "+ctx.atarget()+" href='"+
                                             fora.forumUrl(subCtx, ctx.getLocale().toString(), xpt.getByXpath(path))
                                         +"'>" +
                                             xpt.getPrettyPath(path) +
                                             ctx.iconHtml("disp","Vetter Dispute")
                                         +"</a>" +
                                         "</li>");
                         }
                         ctx.println("</ol>");
                     }
                     ctx.println("<br>");
                 }
             }
             
             vet.doDisputePage(ctx);
             
             /*  OLD 'disputed need attention' page. */
             if(false && (UserRegistry.userIsVetter(ctx.session.user))&&((vetStatus & Vetting.RES_BAD_MASK)>0)) {
                 //int numNoVotes = vet.countResultsByType(ctx.getLocale().toString(),Vetting.RES_NO_VOTES);
                 int numInsufficient = vet.countResultsByType(ctx.getLocale(),Vetting.RES_INSUFFICIENT);
                 int numDisputed = vet.countResultsByType(ctx.getLocale(),Vetting.RES_DISPUTED);
                 int numErrors =  0;//vet.countResultsByType(ctx.getLocale().toString(),Vetting.RES_ERROR);
              
                 Hashtable<String,Integer> insItems = new Hashtable<String,Integer>();
                 Hashtable<String,Integer> disItems = new Hashtable<String,Integer>();

                 try { // moderately expensive.. since we are tying up vet's connection..
                     Connection conn = null;
                     PreparedStatement listBadResults = null;
                     try {
                         conn= dbUtils.getDBConnection();
                         listBadResults = vet.prepare_listBadResults(conn);
                         listBadResults.setString(1, ctx.getLocale().getBaseName());
                         ResultSet rs = listBadResults.executeQuery();
                         while(rs.next()) {
                             int xp = rs.getInt(1);
                             int type = rs.getInt(2);

                             String path = xpt.getById(xp);

                             String theMenu = PathUtilities.xpathToMenu(path);

                             if(theMenu != null) {
                                 if(type == Vetting.RES_DISPUTED) {
                                     Integer n = disItems.get(theMenu);
                                     if(n==null) {
                                         n = 1;
                                     }
                                     disItems.put(theMenu, n+1);// what goes here?
                                 } else if (type == Vetting.RES_ERROR) {
                                     //disItems.put(theMenu, "");
                                 } else {
                                     Integer n = insItems.get(theMenu);
                                     if(n==null) {
                                         n = 1;
                                     }
                                     insItems.put(theMenu, n+1);// what goes here?
                                 }
                             }
                         }
                         rs.close();
                     } finally  {
                         DBUtils.close(listBadResults,conn);
                     }
                 } catch (SQLException se) {
                     throw new RuntimeException("SQL error listing bad results - " + DBUtils.unchainSqlException(se));
                 }
                 // et.tostring

                 WebContext subCtx = (WebContext)ctx.clone();
                 //subCtx.addQuery(QUERY_LOCALE,ctx.getLocale().toString());
                 subCtx.removeQuery(QUERY_SECTION);

                if(false && (this.phase()==Phase.VETTING || isPhaseVettingClosed()) == true) {
                     
                     if((numDisputed>0)||(numErrors>0)) {
                         ctx.print("<h2>Disputed items that need your attention:</h2>");
                         ctx.print("<b>total: "+numDisputed+ " - </b>");
                         for(Iterator li = disItems.keySet().iterator();li.hasNext();) {
                             String item = (String)li.next();
                             int count = disItems.get(item);
                             printMenu(subCtx, "", item, item + "("+count+")", "only=disputed&x", DataSection.CHANGES_DISPUTED);
                             if(li.hasNext() ) {
                                 subCtx.print(" | ");
                             }
                         }
                         ctx.println("<br>");
                     }
                     if((/*numNoVotes+*/numInsufficient)>0) {
                         ctx.print("<h2>Unconfirmed items (insufficient votes). Please do if possible.</h2>");
                         ctx.print("<b>total: "+numInsufficient+ " - </b>");
                         for(Iterator li = insItems.keySet().iterator();li.hasNext();) {
                             String item = (String)li.next();
                             int count = insItems.get(item);
                             printMenu(subCtx, "", item, item + "("+count+")");
                             if(li.hasNext() ) {
                                 subCtx.print(" | ");
                             }
                         }
                         ctx.println("<br>");
                     }
                 }
             }
         }
         
         --%>