<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%-- setup 'ctx' --%><%
    if(!ctx.hasAdminPassword()) return;
%>

<h1>Statics</h1>
      <% ctx.staticInfo();  ctx.flush(); %>

<%--
        
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        long deadlockedThreads[] = threadBean.findDeadlockedThreads();

        
        if(action.equals("")) {
            action = "sessions";
        }
        WebContext actionCtx = (WebContext)ctx.clone();
        actionCtx.addQuery("dump",vap);
        WebContext actionSubCtx = (WebContext)actionCtx.clone();
        actionSubCtx.addQuery("action",action);

        actionCtx.println("Click here to update data: ");
        printMenuButton(actionCtx, action, "upd_1", "Easy Data Update", "action", "Update:");       
        actionCtx.println(" <br> ");
        printMenu(actionCtx, action, "sessions", "User Sessions", "action");    
        actionCtx.println(" | ");
        printMenu(actionCtx, action, "stats", "Internal Statistics", "action");       
        actionCtx.println(" | ");
        printMenu(actionCtx, action, "tasks", "Tasks and Threads"+
                                    ((deadlockedThreads!=null)?actionCtx.iconHtml("warn","deadlock"):
                                                              actionCtx.iconHtml("okay","no deadlock")),
                                                        "action");       
        actionCtx.println(" | ");
        printMenu(actionCtx, action, "statics", "Static Data", "action");       
        actionCtx.println(" | ");
        printMenu(actionCtx, action, "specialusers", "Specialusers", "action");       
        actionCtx.println(" | ");
        printMenu(actionCtx, action, "specialmsg", "Update Header Message", "action");       
        actionCtx.println(" | ");
        printMenu(actionCtx, action, "upd_src", "Manage Sources", "action");       
        actionCtx.println(" | ");
        printMenu(actionCtx, action, "load_all", "Load all locales", "action");       
        actionCtx.println(" | ");
        printMenu(actionCtx, action, "add_locale", "Add a locale", "action");       
        actionCtx.println(" | ");
        printMenu(actionCtx, action, "bulk_submit", "Bulk Data Submit", "action");       
        actionCtx.println(" | ");
        printMenu(actionCtx, action, "srl", "Dangerous Options...", "action");  // Dangerous items

        if(action.startsWith("srl")) {
            ctx.println("<br><ul><div class='ferrbox'>");
            if(action.equals("srl")) {
                ctx.println("<b>These menu items are dangerous and may have side effects just by clicking on them.</b><br>");
            }
            actionCtx.println(" | ");
            printMenu(actionCtx, action, "srl_vet_imp", "Update Implied Vetting", "action");       
            actionCtx.println(" | ");
            printMenu(actionCtx, action, "srl_vet_res", "Update Results Vetting", "action");       
            actionCtx.println(" | ");
            printMenu(actionCtx, action, "srl_vet_sta", "Update Vetting Status", "action");       
            actionCtx.println(" | ");
            printMenu(actionCtx, action, "srl_vet_nag", "MAIL: send out vetting reminder", "action");       
            actionCtx.println(" | ");
/*            printMenu(actionCtx, action, "srl_vet_upd", "MAIL: vote change [daily]", "action");       
            actionCtx.println(" | "); */
            /*
            printMenu(actionCtx, action, "srl_db_update", "Update <tt>base_xpath</tt>", "action");       
            actionCtx.println(" | ");
            */
            printMenu(actionCtx, action, "srl_vet_wash", "Clear out old votes", "action");       
            actionCtx.println(" | ");
            printMenu(actionCtx, action, "srl_output", "Output Vetting Data", "action");       
            actionCtx.println(" | ");
            printMenu(actionCtx, action, "srl_crash", "Bust Survey Tool", "action");       
            actionCtx.println(" | ");
            
            
            printMenu(actionCtx, action, "srl_twiddle", "twiddle params", "action");       
            ctx.println("</div></ul>");
        }
        actionCtx.println("<br>");
        
        /* Begin sub pages */
        
        if(action.equals("stats")) {
            ctx.println("<div class='pager'>");
            ctx.println("DB version " + dbUtils.dbInfo+ ",  ICU " + com.ibm.icu.util.VersionInfo.ICU_VERSION+
                    ", Container: " + config.getServletContext().getServerInfo()+"<br>");
            ctx.println(uptime + ", " + pages + " pages and "+xpages+" xml pages served.<br/>");
            //        r.gc();
            //        ctx.println("Ran gc();<br/>");

            ctx.println("String hash has " + stringHash.size() + " items.<br/>");
            ctx.println("xString hash info: " + xpt.statistics() +"<br>");
            if(gBaselineHash != null) {
                ctx.println("baselinecache info: " + (gBaselineHash.size()) + " items."  +"<br>");
            }
            ctx.println("CLDRFile.distinguishedXPathStats(): " + CLDRFile.distinguishedXPathStats() + "<br>");
            
            try {
//              getDBSourceFactory().stats(ctx).append("<br>");
                dbUtils.stats(ctx).append("<br>");
            } catch (IOException e) {
                SurveyLog.logException(e, ctx);
                ctx.println("Error " + e + " loading other stats<br/>");
                e.printStackTrace();
                
            }
            ctx.println("Open user files: " + allUserLocaleStuffs.size()+"<br/>");
            ctx.println("</div>");

            StringBuffer buf = new StringBuffer();
            ctx.println("<h4>Memory</h4>");

            appendMemoryInfo(buf, false);

            ctx.print(buf.toString());
            buf.delete(0, buf.length());

            ctx.println("<a class='notselected' href='" + ctx.jspLink("about.jsp") +"'>More version information...</a><br/>");

        } else if(action.equals("statics")) {
            ctx.println("<h1>Statics</h1>");
            ctx.staticInfo();
        } else if(action.equals("tasks")) {
            WebContext subCtx = (WebContext)ctx.clone();
            actionCtx.addQuery("action",action);


        if(isUnofficial) {
            ctx.println("<form style='float: right;' method='POST' action='"+actionCtx.url()+"'>");
            actionCtx.printUrlAsHiddenFields();
            ctx.println("<input type=submit value='Do Nothing For Ten Seconds' name='10s'></form>");
            
            ctx.println("<form style='float: right;' method='POST' action='"+actionCtx.url()+"'>");
            actionCtx.printUrlAsHiddenFields();
            ctx.println("<input type=submit value='Do Nothing For Ten Minutes' name='10m'></form>");
            
            ctx.println("<form style='float: right;' method='POST' action='"+actionCtx.url()+"'>");
            actionCtx.printUrlAsHiddenFields();
            ctx.println("<input type=submit value='Do Nothing Every Ten Seconds' name='p10s'></form>");
        }


        String fullInfo = startupThread.toString();

        ctx.println("<h1 title='"+fullInfo+"'>Tasks</h1>");
        
        if(!startupThread.mainThreadRunning()) {        
        ctx.println("<i>Main thread is not running.</i><br>");
        }

            SurveyThread.SurveyTask acurrent = startupThread.current;

            if(acurrent!=null) {
        ctx.println("<hr>");
        ctx.println("<table border=0><tr><th>Active Task:</th>");
        ctx.println("<td>"+acurrent.toString()+"</td>");
        
        ctx.println("<td><a href='#currentTask'>"+ctx.iconHtml("zoom","Zoom in on task..")+"</a></td>");

        ctx.println("<td>");
        ctx.println("<form method='POST' action='"+actionCtx.url()+"'>");
        actionCtx.printUrlAsHiddenFields();
        ctx.println("<input type=submit value='Stop Active Task' name='tstop'></form>");
        ctx.println("<form method='POST' action='"+actionCtx.url()+"'>");
        actionCtx.printUrlAsHiddenFields();
        ctx.println("<input type=submit value='Kill Active Task' name='tkill'></form>");
        ctx.println("</td></tr></table>");
            }
        if(startupThread.tasksRemaining()>1) {
        ctx.println("<i>"+startupThread.tasksRemaining()+" total tasks remaining.</i><br>");
        }
            
           
        if(ctx.hasField("10s")) {
            startupThread.addTask(new SurveyTask("Waste 10 Seconds")
            {
                public void run() throws Throwable {
                    CLDRProgressTask task = this.openProgress("Waste 10 Seconds",10);
                    try {
                        for(int i=0;i<10;i++) {
                            task.update(i);
                            Thread.sleep(1000);
                        }
                    } finally {
                        task.close();
                    }
                }
            });
            ctx.println("10s task added.\n");
        } else if(ctx.hasField("10m")) {
                startupThread.addTask(new SurveyTask("Waste 10 Minutes")
                {
                    public void run() throws Throwable {
                        CLDRProgressTask task = this.openProgress("Waste 10 Minutes",10);
                        try {
                            for(int i=0;i<10;i++) {
                                task.update(i);
                                Thread.sleep(1000*60);
                            }
                        } finally {
                            task.close();
                        }
                    }
                });
                ctx.println("10m task added.\n");
//      } else if(ctx.hasField("p10s")) {
//          addPeriodicTask(new TimerTask()
//          {
//              @Override
//              public void run() throws Throwable {
//                  CLDRProgressTask task = openProgress("P:Waste 3 Seconds",10);
//                  try {
//                      for(int i=0;i<3;i++) {
//                          task.update(i);
//                          Thread.sleep(1000);
//                      }
//                  } finally {
//                      task.close();
//                  }
//              }
//          });
//          ctx.println("p10s3s task added.\n");
        } else  if(ctx.hasField("tstop")) {
                if(acurrent!=null) {
            acurrent.stop();
            ctx.println(acurrent + " stopped");
                }
            } else if(ctx.hasField("tkill")) {
                if(acurrent!=null) {
            acurrent.kill();
            ctx.println(acurrent + " killed");
                }
            }
            
            ctx.println("<hr>");
            ctx.println("<h2>Threads</h2>");
            Map<Thread, StackTraceElement[]> s = Thread.getAllStackTraces();
            ctx.print("<ul id='threadList'>");
            
            Set<Thread> threadSet = new TreeSet<Thread>(new Comparator<Thread>() {
                @Override
                public int compare(Thread o1, Thread o2) {
                    int rc = 0;
                    rc = o1.getState().compareTo(o2.getState());
                    if(rc==0) {
                        rc = o1.getName().compareTo(o2.getName());
                    }
                    return rc;
                }
            });
            threadSet.addAll(s.keySet());
            
            for(Thread t : threadSet) {
                ctx.println("<li class='"+t.getState().toString()+"'><a href='#"+t.getId()+"'>"+t.getName()+"</a>  - "+t.getState().toString());
                ctx.println("</li>");
            }
            ctx.println("</ul>");
            // detect deadlocks
            if(deadlockedThreads != null) {
                ctx.println("<h2>"+ctx.iconHtml("stop", "deadlocks")+" deadlocks</h2>");
                
                ThreadInfo deadThreadInfo[] = threadBean.getThreadInfo(deadlockedThreads, true, true);
                for(ThreadInfo deadThread : deadThreadInfo) {
                    ctx.println("<b>Name: " + deadThread.getThreadName()+" / #"+deadThread.getThreadId()+"</b><br>");
                    ctx.println("<pre>"+deadThread.toString()+"</pre>");
                }
            } else {
                ctx.println("<i>no deadlocked threads</i>");
            }
            
            
            // show all threads
            for(Thread t : threadSet) {
            if(t == startupThread) { 
            ctx.println("<a name='currentTask'></a>");
            }
            ctx.println("<a name='"+t.getId()+"'><h3>"+t.getName()+"</h3></a> - "+t.getState().toString());
                
                if(t.getName().indexOf(" ST ")>0) {
                    ctx.println("<form method='POST' action='"+actionCtx.url()+"'>");
                    actionCtx.printUrlAsHiddenFields();
                    ctx.println("<input type=hidden name=killtid value='"+t.getId()+"'>");
                    ctx.println("<input type=submit value='Kill Thread #"+t.getId()+"'></form>");
                    
                    if(ctx.fieldLong("killtid") == (t.getId())) {
                        ctx.println(" <br>(interrupt and stop called..)<br>\n");
                        try {
                            t.interrupt();
                            t.stop(new InternalError("Admin wants you to stop"));
                        } catch(Throwable tt) {
                            SurveyLog.logException(tt, ctx);
                            ctx.println("[caught exception " + tt.toString()+"]<br>");
                        }
                    }
                    
                }
                
                
                StackTraceElement[] elem = s.get(t);
                ctx.print("<pre>");
                for(StackTraceElement el : elem) {
                    ctx.println(el.toString());
                }
                ctx.print("</pre>");
            }
            
        } else if(action.equals("sessions"))  {
            ctx.println("<h1>Current Sessions</h1>");
            ctx.println("<table class='list' summary='User list'><tr class='heading'><th>age</th><th>user</th><th>what</th><th>action</th></tr>");
            int rowc = 0;
            for(Iterator li = CookieSession.getAll();li.hasNext();) {
                CookieSession cs = (CookieSession)li.next();
                ctx.println("<tr class='row"+(rowc++)%2+"'><!-- <td><tt style='font-size: 72%'>" + cs.id + "</tt></td> -->");
                ctx.println("<td>" + timeDiff(cs.last) + "</td>");
                if(cs.user != null) {
                    ctx.println("<td><tt>" + cs.user.email + "</tt><br/>" + 
                                "<b>"+cs.user.name + "</b><br/>" + 
                                cs.user.org + "</td>");
                } else {
                    ctx.println("<td><i>Guest</i><br><tt>"+cs.ip+"<tt></td>");
                }
                ctx.println("<td>");
                Hashtable lh = cs.getLocales();
                Enumeration e = lh.keys();
                if(e.hasMoreElements()) { 
                    for(;e.hasMoreElements();) {
                        String k = e.nextElement().toString();
                        ctx.println(new ULocale(k).getDisplayName(ctx.displayLocale) + " ");
                    }
                }
                ctx.println("</td>");
                
                ctx.println("<td>");
                printLiveUserMenu(ctx, cs);
                if(cs.id.equals(ctx.field("unlink"))) {
                    cs.remove();
                    ctx.println("<br><b>Removed.</b>");
                }
                ctx.println(  " | <a class='notselected' href='"+actionCtx.url()+"&amp;banip="+URLEncoder.encode(cs.ip)+"'>Ban</a>");
                if(cs.ip.equals(ctx.field("banip"))) {
                    ctx.println("<b> Banned:</b> " + cs.banIn(BAD_IPS) + "<hr/> and Kicked.");
                    cs.remove();
                }
                ctx.println("</td>");
                
                ctx.println("</tr>");
                
                if(cs.id.equals(ctx.field("see"))) {
                    ctx.println("<tr><td colspan=5>");
                    ctx.println("Stuff: " + cs.toString() + "<br>");
                    ctx.staticInfo_Object(cs.stuff);
                    ctx.println("<hr>Prefs: <br>");
                    ctx.staticInfo_Object(cs.prefs);
                    ctx.println("</td></tr>");
                }
            }
            ctx.println("</table>");
            String rmip = ctx.field("rmip");
            if(rmip !=null && !rmip.isEmpty()) {
                BAD_IPS.remove(rmip);
                ctx.println("<b>Removed bad IP:"+rmip+"</b>");
            }
            if(!BAD_IPS.isEmpty()) {
                ctx.println("<h3>Bad IPs</h3>");
                ctx.println("<table class='list'><tr class='heading'><th>IP</th><th>Info</th><th>(delete)</th></tr>");
                rowc=0;
                for(Entry<String, Object> e : BAD_IPS.entrySet()) {
                    ctx.println("<tr class='row"+(rowc++)%2+"'>");
                    ctx.println( "<th>"+e.getKey() + "</th>");
                    ctx.println( "<td>" + e.getValue() + "</td>");
                    ctx.println(  "<td><a class='notselected' href='"+actionCtx.url()+"&amp;rmip="+URLEncoder.encode(e.getKey())+"'>(remove)</td>");
                    ctx.println("</tr>");
                }
                ctx.println("</table>");
            }
        } else if(action.equals("upd_1")) {
            WebContext subCtx = (WebContext)ctx.clone();
            actionCtx.addQuery("action",action);
            ctx.println("<h2>1-click vetting update</h2>");
            // 1: update all sources

            try {
                final WebContext fakeContext = new WebContext(true);
                fakeContext.sm = this;

                startupThread.addTask(new SurveyThread.SurveyTask("Updating All Data") {
                    public void run() throws Throwable {
                        CLDRProgressTask progress = openProgress("Data Update");
                        try {
                            String baseName = this.name;
                            int cnt = 0;
                            progress.update(" reset caches and update all");
                            try {
                                //                CLDRDBSource mySrc = makeDBSource(conn, null, CLDRLocale.ROOT);
                                resetLocaleCaches();
                            } finally {
                                //                SurveyMain.closeDBConnection(conn);
                            }
                            // 2: load all locales
                            progress.update("load all locales");
                            loadAllLocales(fakeContext, this);
                            // 3: update impl votes
                            progress.update("Update implied votes");
                            ElapsedTimer et = new ElapsedTimer();
                            int n = vet.updateImpliedVotes();
                            //ctx.println("Done updating "+n+" implied votes in: " + et + "<br>");
                            // 4: UpdateAll
                            //ctx.println("<h4>Update All</h4>");
                            progress.update("Update All");

                            et = new ElapsedTimer();
                            n = vet.updateResults(false); // don't RE update.
                            //ctx.println("Done updating "+n+" vote results in: " + et + "<br>");
                            progress.update(" Invalidate ROOT");
                            // 5: update status
                            progress.update(" Update Status");
                            et = new ElapsedTimer();
                            SurveyLog.logger.warning("Done updating "+n+" statuses [locales] in: " + et + "<br>");
                            SurveyMain.specialHeader = "Data update done! Please log off. Administrator: Please restart your Survey Tool.";
                        } finally {
                            progress.close();
                        }
                    }
                });
            } catch (IOException e) {
                SurveyLog.logException(e, ctx);
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new InternalError("Couldn't create fakeContext for administration.");
            }
//        } else if(action.equals("bulk_submit")) {
//            WebContext subCtx = (WebContext)ctx.clone();
//            actionCtx.addQuery("action",action);
//
//            String aver=newVersion; // TODO: should be from 'new_version'
//            ctx.println("<h2>Bulk Data Submission Updating for "+aver+"</h2><br/>\n");
//
//            Set<UserRegistry.User> updUsers = new HashSet<UserRegistry.User>();
//
//            
//            if(true) { ctx.println("Nope, not until you fix http://unicode.org/cldr/trac/ticket/3656#comment:3 in this code."); return; }
//
//            // from config 
//            String bulkStrOrig = survprops.getProperty(CLDR_BULK_DIR,"");
//            // from query if there, else config
//            if(ctx.hasField(CLDR_BULK_DIR)) {
//                bulkStr = ctx.field(CLDR_BULK_DIR);
//            }
//            if(bulkStr == null || bulkStr.length()==0) {
//                bulkStr = bulkStrOrig;
//            }
//            File bulkDir = null;
//            if(bulkStr!=null&&bulkStr.length()>0) {
//                bulkDir = new File(bulkStr);
//            }
//            // dir change form
//            ctx.println("<form method='POST' action='"+actionCtx.url()+"'>");
//            actionCtx.printUrlAsHiddenFields();
//            ctx.println("<label>Bulk Dir: " +
//                    "<input name='"+CLDR_BULK_DIR+"' value='"+bulkStr+"' size='60'>" +
//                    "</label> <input type=submit value='Set'><br>" +
//            "</form><hr/>");
//            if(bulkDir==null||!bulkDir.exists()||!bulkDir.isDirectory()) {
//                ctx.println(ctx.iconHtml("stop","could not load bulk data")+"The bulk data dir "+CLDR_BULK_DIR+"="+bulkStr+" either doesn't exist or isn't set in cldr.properties. (Server requires reboot for this parameter to take effect)</i>");
//            } else try {
//
//                ctx.println("<h3>Bulk dir: "+bulkDir.getAbsolutePath()+"</h3>");
//                boolean doimpbulk = ctx.hasField("doimpbulk");
//                boolean istrial = ctx.hasField("istrial");
//                ctx.println("<form method='POST' action='"+actionCtx.url()+"'>");
//                actionCtx.printUrlAsHiddenFields();
//                ctx.println("<input type=submit value='Accept all implied votes' name='doimpbulk'></form>");
//                ctx.println("<form method='POST' action='"+actionCtx.url()+"'>");
//                actionCtx.printUrlAsHiddenFields();
//                ctx.println("<input type=submit value='Do a trial run' name='istrial'></form>");
//                if(istrial) {
//                    ctx.print("<i>trial run. press the button to accept these votes.</i>");
//                } else if(doimpbulk) {
//                    ctx.print("<i>Real run.</i>");
//                } else {
//                    ctx.print("Press one of the buttons to begin.");
//                }
//
//                Set<File> files = new TreeSet<File>(Arrays.asList(getInFiles(bulkDir)));
//
//                ctx.print("Jump to: ");
//                for(File file : files) {
//                    ctx.print("<a href=\"#"+getLocaleOf(file)+"\">"+file.getName()+"</a> ");
//                }
//                ctx.println("<br>");
//
//                Set<CLDRLocale> toUpdate = new HashSet<CLDRLocale>();
//                int wouldhit=0;
//                
//                if(!istrial && !doimpbulk) return;
//
//                CLDRProgressTask progress = openProgress("bulk data import", files.size());
//                int nn=0;
//                try { for(File file : files ) {
//                    /*synchronized(vet) */ {
//                        CLDRLocale loc = getLocaleOf(file);
//                        DisplayAndInputProcessor processor = new DisplayAndInputProcessor(loc.toULocale());
//                        ctx.println("<a name=\""+loc+"\"><h2>"+file.getName()+" - "+loc.getDisplayName(ctx.displayLocale)+"</h2></a>");
//                        CLDRFile c = SimpleFactory.makeFile(file.getPath(), loc.getBaseName(), CLDRFile.DraftStatus.unconfirmed);
//                        XPathParts xpp = new XPathParts(null,null);
//
//                        OnceWarner warner = new OnceWarner();
//                        XMLSource stSource = getSTFactory().makeSource(loc, false);
//
//                        progress.update(nn++, loc.toString());
//                        for(String x : c) {
//                            String full = c.getFullXPath(x);
//                            String alt = XPathTable.getAlt(full, xpp);
//                            String val0 = c.getStringValue(x);
//                            Exception exc[] = new Exception[1];
//                            String val = processor.processInput(x, val0, exc);
//                            if(alt==null||alt.length()==0) {
//                                if(!full.startsWith("//ldml/identity")) {
//                                    warner.warnOnce(ctx, "countNoAlt", "warn", "Xpath with no 'alt' tag: " + full);
//                                }
//                                continue;
//                            }
//                            String altPieces[] = LDMLUtilities.parseAlt(alt);
//                            if(altPieces[1]==null) {
//                                warner.warnOnce(ctx, "countNoAlt", "warn", "Xpath with no 'alt-proposed' tag: " + full);
//                                continue;
//                            }
//                            /*
//                      if(alt.equals("XXXX")) {
//                          alt = "proposed-u1-implicit1.7";
//                          x = XPathTable.removeAlt(x, xpp);
//                      }*/
//                            int n = XPathTable.altProposedToUserid(altPieces[1]);
//                            if(n<0) {
//                                warner.warnOnce(ctx, "countNoUser", "warn", "Xpath with no userid in 'alt' tag: " + full);
//                                continue;
//                            }
//                            User ui = null;
//                            if(n>=0) ui = reg.getInfo(n);
//                            if(ui==null) {
//                                warner.warnOnce(ctx, "countBadUser", "warn", "Bad userid '"+n+"': " + full);
//                                continue;
//                            }
//                            updUsers.add(ui);
//                            String base_xpath = xpt.xpathToBaseXpath(x);
//                            int base_xpath_id = xpt.getByXpath(base_xpath);
//                            int vet_type[] = new int[1];
//                            int j = vet.queryVote(loc, n, base_xpath_id, vet_type);
//                            //int dpathId = xpt.getByXpath(xpathStr);
//                            // now, find the ID to vote for.
//                            Set<String> resultPaths = new HashSet<String>();
//                            String baseNoAlt = xpt.removeAlt(base_xpath);
//                            if(true) throw new InternalError("Nope, not until you fix http://unicode.org/cldr/trac/ticket/3656#comment:3 in this code.");
//                            if(altPieces[0]==null) {
//                                stSource.getPathsWithValue(val, base_xpath, resultPaths);
//                            } else {
//                                Set<String> lotsOfPaths = new HashSet<String>();
//                                stSource.getPathsWithValue(val, baseNoAlt, lotsOfPaths);
////                                SurveyLog.logger.warning("pwv["+val+","+baseNoAlt+",)="+lotsOfPaths.size());
//                                if(!lotsOfPaths.isEmpty()) {
//                                    for(String s : lotsOfPaths) {
//                                        String alt2 = XPathTable.getAlt(s, xpp);
//                                        if(alt2 != null) {
//                                            String altPieces2[] = LDMLUtilities.parseAlt(alt2);
//                                            if(altPieces2[0]!=null && altPieces[0].equals(altPieces[0])) {
//                                                resultPaths.add(s);
////                                                SurveyLog.logger.warning("==match: " + s);
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//
//                            String resultPath = null;
//
//                            //                                 if(warner.count("countAdd")==0 && warner.count("countVoteMain")==0) {
//                            //                                     ctx.println("<tr><th>");               
//                            //                                     ctx.println("<a href='"+ctx.base()+"?_="+loc+"&xpath="+base_xpath_id+"'>");
//                            //                                     ctx.println(base_xpath_id+"</a></th>");
//                            //                                     ctx.println("<td>" + j+"</td><td>"+val+"</td>");
//                            //                                     ctx.println("<td>");
//                            //                                 }
//
//                            if(exc[0]!=null) {
//                                ctx.println("Exceptions on DAIP: ");
//                                for(Exception ex : exc) { 
//                                    ctx.print(ex.toString()+" ");
//                                    ex.printStackTrace();
//                                }
//                                ctx.println("<br>");
//                            }
//
//
//                            if(resultPaths.isEmpty()) {
//                                warner.warnOnce(ctx, "countAdd", "zoom", "Value must be added", xpt.getPrettyPath(base_xpath)+": " + val );
//                                if(!doimpbulk) {
//                                    warner.warnOnce(ctx, "countReady", "okay","<i>Ready to update.</i>");
//                                    ctx.println("</td></tr>");
//                                    continue; // don't try to add.
//                                }
//                                /* NOW THE FUN PART */
//                                for(int i=0;(resultPath==null)&&i<1000;i++) {
//                                    String proposed = "proposed-u"+n+"-b"+i;
//                                    String newAlt = LDMLUtilities.formatAlt(altPieces[0], proposed);
//                                    String newxpath = baseNoAlt+"[@alt=\"" + newAlt + "\"]";
//                                    String newoxpath = newxpath+"[@draft=\"unconfirmed\"]";
//
//                                    if(stSource.hasValueAtDPath(newxpath)) continue;
//
//                                    /* Write! */
//                                    stSource.putValueAtPath(newoxpath, val);
//                                    toUpdate.add(loc);
//
//                                    resultPath = newxpath;
//                                    if(warner.count("countAdd")<2) {
//                                        Set<String> nresultPaths = new HashSet<String>();
//                                        stSource.getPathsWithValue(val, base_xpath, nresultPaths);
//                                        ctx.println(">> now have " + nresultPaths.size() + " paths with value: <tt>"+base_xpath+"</tt> <ol>");
//                                        for(String res : nresultPaths) { 
//                                            ctx.println(" <li>"+res+"</li>\n");
//                                        }
//                                        ctx.println("</ol>\n");
//                                        String nr = stSource.getValueAtDPath(newxpath);
//                                        if(nr==null) {
//                                            ctx.println("Couldn't get valueatdpath "+ newxpath+"<br>\n");
//                                        } else if(nr.equals(val)) {
//                                            //    ctx.println("RTT ok with " + newxpath + " !<br>\n");
//                                        } else {
//                                            ctx.println("RTT not ok!!!!<br>\n");
//                                        }
//                                    }
//                                }
//
//
//                            } else if(resultPaths.size()>1) {
//                                /* ok, more than one result. stay cool.. */
//                                /* #1 - look for the base xpath. */
//                                for(String path : resultPaths) {
//                                    if(path.equals(base_xpath)) {
//                                        resultPath = path;
//                                        warner.warnOnce(ctx, "countVoteMain", "squo", "Using base xpath for " + base_xpath);
//                                    }
//                                }
//                                /* #2 look for something with a vote */
//                                if(resultPath==null) {
//                                    String winPath = stSource.getWinningPath(base_xpath);
//                                    String winDpath = CLDRFile.getDistinguishingXPath(winPath, null, true);
//                                    for(String path : resultPaths) {
//                                        String aDPath = CLDRFile.getDistinguishingXPath(path, null, true);    
//                                        if(aDPath.equals(winDpath)) {
//                                            if(false)                     ctx.println("Using winning dpath " + aDPath +"<br>");
//                                            resultPath = aDPath;
//                                        }
//                                    }
//                                }
//                                /* #3 just take the first one */
//                                if(resultPath==null) {
//                                    resultPath = resultPaths.toArray(new String[0])[0];
//                                    //ctx.println("Using [0] path " + resultPath);
//                                }
//                                if(resultPath==null) {
//                                    ctx.println(ctx.iconHtml("stop", "more than one result!")+"More than one result!<br>");
//                                    if(true) ctx.println(loc+" " + xpt.getPrettyPath(base_xpath) + " / "+ alt + " (#"+n+" - " + ui +")<br/>");
//                                    ctx.println("</td></tr>");
//                                    continue;
//                                }
//                            } else{
//                                resultPath = resultPaths.toArray(new String[0])[0];
//                            }
//
//                            /*temp*/if(resultPath == null) {
//                                ctx.println("</td></tr>");
//                                continue; 
//                            }
//
//                            String xpathStr = CLDRFile.getDistinguishingXPath(resultPath, null, false);
//                            int dpathId = xpt.getByXpath(xpathStr);
//                            if(false) ctx.println(loc+" " + xpt.getPrettyPath(base_xpath) + " / "+ alt + " (#"+n+" - " + ui +"/" + dpathId+" <br/>");
//
//
//
//
//                            if(dpathId == j) {
//                                warner.warnOnce(ctx, "countAlready", "squo", "Vote already correct");
//                                //                            ctx.println(" "+ctx.iconHtml("squo","current")+" ( == current vote ) <br>");
//                                //                              already++;
//                            } else {
//                                if(j>-1) {
//                                    if(vet_type[0]==Vetting.VET_IMPLIED) {
//                                        warner.warnOnce(ctx, "countOther", "okay", "Changing existing implied vote");
//                                    } else {
//                                        warner.warnOnce(ctx, "countExplicit", "warn", "NOT changing existing different vote");
//                                        ctx.println("</td></tr>");
//                                        continue;
//                                    }
//                                    //                              ctx.println(" "+ctx.iconHtml("warn","already")+"Current vote: "+j+"<br>");
//                                    //                              different++;
//                                }
//                                if(doimpbulk) {
//                                    vet.vote(loc, base_xpath_id, n, dpathId, Vetting.VET_IMPLIED);
//                                    toUpdate.add(loc);
//                                    warner.warnOnce(ctx, "countReady", "okay","<i>Updating.</i>");
//                                } else {
//                                    warner.warnOnce(ctx, "countReady", "okay","<i>Ready to update.</i>");
//                                    /*  wouldhit++;
//                                toUpdate.add(loc);*/
//                                }
//                            }
//                            ctx.println("</td></tr>");
//
//                        } /* end xpath */
//                        //            ctx.println("</table>");
//                        ctx.println("<hr>");
//                        /*if(already>0 ) {
//                        ctx.println(" "+ctx.iconHtml("squo","current")+""+already+" items already had the correct vote.<br>");
//                    }
//                    if(different>0) {
//                        ctx.println(" "+ctx.iconHtml("warn","different")+" " + different + " items had a different vote already cast.<br>");
//                    }
//                    if(doimpbulk && !toUpdate.isEmpty()) {
//                        ctx.println("<h3>"+wouldhit+" Locale Updates in " + toUpdate.size() + " locales ..</h3>");
//                        for(CLDRLocale l : toUpdate) {
//                            vet.deleteCachedLocaleData(l);
//                            dbsrcfac.needUpdate(l);
//                            ctx.print(l+"...");
//                        }
//                        ctx.println("<br>");
//                        int upd = dbsrcfac.update();
//                        ctx.println(" Updated. "+upd + " deferred updates done.<br>");
//                    } else if(wouldhit>0) {
//                        ctx.println("<h3>Ready to update "+wouldhit+" Locale Updates in " + toUpdate.size() + " locales ..</h3>");
//                        ctx.println("<form method='POST' action='"+actionCtx.url()+"'>");
//                        actionCtx.printUrlAsHiddenFields();
//                        ctx.println("<input type=submit value='Accept all implied votes' name='doimpbulk'></form>");
//                    }*/
//                        if(!toUpdate.isEmpty()) {
//                            ctx.println("Updating: ");
//                            for(CLDRLocale toul : toUpdate) {
//                                this.updateLocale(toul);
//                                getSTFactory().needUpdate(toul);
//                                ctx.println(toul+" ");
//                            }
//                            ctx.println("<br>");
//                            ctx.println("Clearing cache: #"+getSTFactory().update()+"<br>");
//                        }
//                        toUpdate.clear();
//
//                        warner.summarize(ctx);
//
//                    } /* end sync */
//                } /* end outer loop */
//                } finally {
//                    progress.close();
//                }
//                ctx.println("<hr>");
//                ctx.println("<h3>Users involved:</h3>");
//                for(User auser : updUsers) {
//                    ctx.println(auser.toString());
//                    ctx.println("<br>");
//                }
//                /*
//            } catch (SQLException t) {
//                 t.printStackTrace();
//                 ctx.println("<b>Err in bulk import </b> <pre>" + unchainSqlException(t)+"</pre>");
//                 */
//            } catch (Throwable t) {
//              SurveyLog.logException(t, ctx);
//                t.printStackTrace();
//                ctx.println("Err : " + t.toString() );
//            }
//        
        } else if(action.equals("srl")) {
            ctx.println("<h1>"+ctx.iconHtml("warn", "warning")+"Please be careful!</h1>");
        } else if(action.equals("srl_vet_imp")) {
            WebContext subCtx = (WebContext)ctx.clone();
            subCtx.addQuery("dump",vap);
            ctx.println("<br>");
            ElapsedTimer et = new ElapsedTimer();
            int n = vet.updateImpliedVotes();
            ctx.println("Done updating "+n+" implied votes in: " + et + "<br>");
        } else if(action.equals("srl_vet_sta")) {
            ElapsedTimer et = new ElapsedTimer();
            int n = vet.updateStatus();
            ctx.println("Done updating "+n+" statuses [locales] in: " + et + "<br>");
        ///} else if(action.equals("srl_dis_nag")) {
        /// vet.doDisputeNag("asdfjkl;", null);
        /// ctx.println("\u3058\u3083\u3001\u3057\u3064\u308c\u3044\u3057\u307e\u3059\u3002<br/>"); // ??
        } else if(action.equals("srl_vet_nag")) {
            if(ctx.field("srl_vet_nag").length()>0) {
                ElapsedTimer et = new ElapsedTimer();
                vet.doNag();
                ctx.println("Done nagging in: " + et + "<br>");
            }else{
                actionCtx.println("<form method='POST' action='"+actionCtx.url()+"'>");
                actionCtx.printUrlAsHiddenFields();
                actionCtx.println("Send Nag Email? <input type='hidden' name='srl_vet_nag' value='Yep'><input type='hidden' name='action' value='srl_vet_nag'><input type='submit' value='Nag'></form>");
            }
//        } else if(action.equals("srl_vet_upd")) {
//            ElapsedTimer et = new ElapsedTimer();
//            int n = vet.updateStatus();
//            ctx.println("Done updating "+n+" statuses [locales] in: " + et + "<br>");
        } else if(action.equals("srl_vet_res")) {
            WebContext subCtx = (WebContext)ctx.clone();
            actionCtx.addQuery("action",action);
            ctx.println("<br>");
            String what = actionCtx.field("srl_vet_res");
            
            Set<CLDRLocale> locs = new TreeSet<CLDRLocale>();
            
            if(what.length()>0) {
                String whats[] = UserRegistry.tokenizeLocale(what);
                
                for(String l : whats) {
                    CLDRLocale loc = CLDRLocale.getInstance(l);
                    locs.add(loc);
                }
            }
            
            final boolean reupdate = actionCtx.hasField("reupdate");

            if(what.equals("ALL")) {
                ctx.println("<h4>Update All (delete first: "+reupdate+")</h4>");
                
        startupThread.addTask(new SurveyThread.SurveyTask("UpdateAll, Delete:"+reupdate) {
            public void run() throws Throwable {
            ElapsedTimer et = new ElapsedTimer();
            int n = vet.updateResults(reupdate);
            SurveyLog.logger.warning("Done updating "+n+" vote results in: " + et + "<br>");
//          lcr.invalidateLocale(CLDRLocale.ROOT);
            ElapsedTimer zet = new ElapsedTimer();
            int zn = vet.updateStatus();
            SurveyLog.logger.warning("Done updating "+zn+" statuses [locales] in: " + zet + "<br>");
            
            }
            });
               
        ctx.println("<h2>Task Queued.</h2>");
        
            } else {
                ctx.println("<h4>Update All</h4>");
                ctx.println("Locs: ");
                for(CLDRLocale loc : locs ) {
                    ctx.println("("+loc+") ");
                }
                ctx.println("<br>");
                ctx.println("* <a href='"+actionCtx.url()+actionCtx.urlConnector()+"srl_vet_res=ALL'>Update all (routine update)</a><p>  ");
                ctx.println("* <a href='"+actionCtx.url()+actionCtx.urlConnector()+"srl_vet_res=ALL&reupdate=reupdate'><b>REupdate:</b> " +
                "Delete all old results, and recount everything. Takes a long time.</a><br>");
                if(what.length()>0) {
                    try {
                        Connection conn = null;
                        PreparedStatement rmResultLoc = null;
                        try {
                            conn = dbUtils.getDBConnection();
                            rmResultLoc = Vetting.prepare_rmResultLoc(conn);
                            for(CLDRLocale loc : locs) {
                                ctx.println("<h2>"+loc+"</h2>");
                                if(reupdate) {
                                    try {
                                        synchronized(vet) {
                                            rmResultLoc.setString(1,loc.toString());
                                            int del = DBUtils.sqlUpdate(ctx, conn, rmResultLoc);
                                            ctx.println("<em>"+del+" results of "+loc+" locale removed</em><br>");
                                            SurveyLog.logger.warning("update: "+del+" results of "+loc+" locale " +
                                            "removed");
                                        }
                                    } catch(SQLException se) {
                                        SurveyLog.logException(se, ctx);
                                        se.printStackTrace();
                                        ctx.println("<b>Err while trying to delete results for " + loc + ":</b> <pre>" + DBUtils.unchainSqlException(se)+"</pre>");
                                    }
                                }

                                ctx.println("<h4>Update just "+loc+"</h4>");
                                ElapsedTimer et = new ElapsedTimer();
                                int n = vet.updateResults(loc,conn);
                                ctx.println("Done updating "+n+" vote results for " + loc + " in: " + et + "<br>");
//                              lcr.invalidateLocale(loc);
                            }
                            ElapsedTimer zet = new ElapsedTimer();
                            int zn = vet.updateStatus(conn);
                            ctx.println("Done updating "+zn+" statuses ["+locs.size()+" locales] in: " + zet + "<br>");
                        } finally {
                            DBUtils.close(rmResultLoc,conn);
                        }
                    } catch (SQLException se) {
                        SurveyLog.logException(se, ctx);
                        se.printStackTrace();
                        ctx.println("<b>Err while trying to delete results :</b> <pre>" + DBUtils.unchainSqlException(se)+"</pre>");
                    }
                } else {
                    vet.stopUpdating = true;            
                }
            }
            actionCtx.println("<hr><h4>Update just certain locales</h4>");
            actionCtx.println("<form method='POST' action='"+actionCtx.url()+"'>");
            actionCtx.printUrlAsHiddenFields();
            actionCtx.println("<label><input type='checkbox' name='reupdate' value='reupdate'>Delete old results before update?</label><br>");
            actionCtx.println("Update just this locale: <input name='srl_vet_res' value='"+what+"'><input type='submit' value='Update'></form>");
        } else if(action.equals("srl_twiddle")) {
            ctx.println("<h3>Parameters. Please do not click unless you know what you are doing.</h3>");
            
            for(Iterator i = twidHash.keySet().iterator();i.hasNext();) {
                String k = (String)i.next();
                Object o = twidHash.get(k);
                if(o instanceof Boolean) {  
                    boolean adv = showToggleTwid(actionSubCtx, k, "Boolean "+k);
                } else {
                    actionSubCtx.println("<h4>"+k+"</h4>");
                }
            }
            
            
//        } else if(action.equals("srl_vet_wash")) {
//          WebContext subCtx = (WebContext)ctx.clone();
//          actionCtx.addQuery("action",action);
//          ctx.println("<br>");
//          String what = actionCtx.field("srl_vet_wash");
//          if(what.equals("ALL")) {
//              ctx.println("<h4>Remove Old Votes. (in preparation for a new CLDR - do NOT run this after start of vetting)</h4>");
//              ElapsedTimer et = new ElapsedTimer();
//              int n = vet.washVotes();
//              ctx.println("Done washing "+n+" vote results in: " + et + "<br>");
//              int stup = vet.updateStatus();
//              ctx.println("Updated " + stup + " statuses.<br>");
//          } else {
//              ctx.println("All: [ <a href='"+actionCtx.url()+actionCtx.urlConnector()+action+"=ALL'>Wash all</a> ]<br>");
//              if(what.length()>0) {
//                  ctx.println("<h4>Wash "+what+"</h4>");
//                  ElapsedTimer et = new ElapsedTimer();
//                  int n = vet.washVotes(CLDRLocale.getInstance(what));
//                  ctx.println("Done washing "+n+" vote results in: " + et + "<br>");
//                  int stup = vet.updateStatus();
//                  ctx.println("Updated " + stup + " statuses.<br>");
//              }
//          }
//          actionCtx.println("<form method='POST' action='"+actionCtx.url()+"'>");
//          actionCtx.printUrlAsHiddenFields();
//          actionCtx.println("Update just: <input name='"+action+"' value='"+what+"'><input type='submit' value='Wash'></form>");
        } else if(action.equals("srl_twiddle")) {
            ctx.println("<h3>Parameters. Please do not click unless you know what you are doing.</h3>");
            
            for(Iterator i = twidHash.keySet().iterator();i.hasNext();) {
                String k = (String)i.next();
                Object o = twidHash.get(k);
                if(o instanceof Boolean) {  
                    boolean adv = showToggleTwid(actionSubCtx, k, "Boolean "+k);
                } else {
                    actionSubCtx.println("<h4>"+k+"</h4>");
                }
            }
            
            
        } else if(action.equals("upd_src")) {
            WebContext subCtx = (WebContext)ctx.clone();
            actionCtx.addQuery("action",action);
            ctx.println("<br>(locale caches reset..)<br>");
//            Connection conn = this.getDBConnection();
            try {
//                CLDRDBSource mySrc = makeDBSource(conn, null, CLDRLocale.ROOT);
                resetLocaleCaches();
                getSTFactory().manageSourceUpdates(actionCtx, this); // What does this button do?
                ctx.println("<br>");
            } finally {
//                SurveyMain.closeDBConnection(conn);
            }
            
        } else if (action.equals("srl_crash")) {
            this.busted("User clicked 'Crash Survey Tool'");
        } else if(action.equals("srl_output")) {
            WebContext subCtx = (WebContext)ctx.clone();
            subCtx.addQuery("dump",vap);
            subCtx.addQuery("action",action);

            final String output = actionCtx.field("output");

            boolean isImmediate = actionCtx.hasField("immediate");
            
            int totalLocs = getLocales().length;
            int need[] = new int[CacheableKinds.values().length];
            
            // calculate
            if(!isImmediate) {
                try {
                    Connection conn = null;
                    try {
                        conn = dbUtils.getDBConnection();
                        for (CLDRLocale loc : getLocales()) {
                            Timestamp locTime = getLocaleTime(conn, loc);
                            for(SurveyMain.CacheableKinds kind : SurveyMain.CacheableKinds.values()) {
                                boolean nu = fileNeedsUpdate(locTime,loc,kind.name());
                                if(nu) need[kind.ordinal()]++;
                            }
                        }
                    } finally {
                        DBUtils.close(conn);
                    }
                } catch (IOException e) {
                    SurveyLog.logException(e, ctx);
                    ctx.println("<i>err getting locale counts: " + e +"</i><br>");
                    e.printStackTrace();
                } catch(SQLException se) {
                    SurveyLog.logException(se, ctx);
                    ctx.println("<i>err getting locale counts: " + dbUtils.unchainSqlException(se)+"</i><br>");
                }
            }

            ctx.println("<br>");
            ctx.print("<b>Output: (/"+totalLocs+")</b> ");
            printMenu(subCtx, output, "xml", "XML ("+need[CacheableKinds.xml.ordinal()]+"/)", "output");
            subCtx.print(" | ");
            printMenu(subCtx, output, "vxml", "VXML ("+need[CacheableKinds.vxml.ordinal()]+"/)", "output");
            subCtx.print(" | ");
            printMenu(subCtx, output, "rxml", "RXML (SLOW!) ("+need[CacheableKinds.rxml.ordinal()]+"/)", "output");
            subCtx.print(" | ");
            printMenu(subCtx, output, "sql", "SQL", "output");
            subCtx.print(" | ");
            printMenu(subCtx, output, "misc", "MISC", "output");
            subCtx.print(" | ");
            printMenu(subCtx, output, "daily", "DAILY", "output");
            subCtx.print(" | ");

            ctx.println("<br>");

            ElapsedTimer aTimer = new ElapsedTimer();
            int files = 0;
            final boolean daily = output.equals("daily");

            if(daily && !isImmediate) {
                startupThread.addTask(new SurveyThread.SurveyTask("daily xml output")
                {
                    final String dailyList[] = {
//                          "xml",
//                          "vxml",
                            "users",
                            "usersa",
                            "translators",
                            "txml",
                    };          
                    public void run() throws Throwable 
                    {
                        CLDRProgressTask progress = openProgress("xml output", dailyList.length);
                        try {

                            String origName = name;
                            for(int i=0;running()&&(i<dailyList.length);i++) {
                                String what = dailyList[i];
                                progress.update(i,what);
                                doOutput(what);
                            }
                        } finally {
                            progress.close();
                        }
                    }
                });
                ctx.println("<h2>Task Queued.</h2>");
            } else {
                if(isImmediate) {
                    if(daily || output.equals("xml")) {
                        files += doOutput("xml");
                        ctx.println("xml" + "<br>");
                    }
                    if(daily || output.equals("vxml")) {
                        files += doOutput("vxml");
                        ctx.println("vxml" + "<br>");
                    }
                    if(output.equals("rxml")) {
                        files += doOutput("rxml");
                        ctx.println("rxml" + "<br>");
                    }
                    if(output.equals("sql")) {
                        files += doOutput("sql");
                        ctx.println("sql" + "<br>");
                    }
                    if(daily || output.equals("misc")) {
                        files += doOutput("users");
                        ctx.println("users" + "<br>");
                        files += doOutput("usersa");
                        ctx.println("usersa" + "<br>");
                        files += doOutput("translators");
                        ctx.println("translators" + "<br>");
                    }

                    if(output.length()>0) {
                        ctx.println("<hr>"+output+" completed with " + files + " files in "+aTimer+"<br>");
                    }
                } else {
                    startupThread.addTask(new SurveyThread.SurveyTask("admin output")
                    {
                        public void run() throws Throwable 
                        {
                            int files=0;
                            CLDRProgressTask progress = openProgress("admin output", 9);
                            try {
                                progress.update("xml?");
                                if(daily || output.equals("xml")) {
                                    files += doOutput("xml");
                                }
                                progress.update("vxml?");
                                if(daily || output.equals("vxml")) {
                                    files += doOutput("vxml");
                                }
                                progress.update("rxml?");
                                if(output.equals("rxml")) {
                                    files += doOutput("rxml");
                                }
                                progress.update("sql?");
                                if(output.equals("sql")) {
                                    files += doOutput("sql");
                                }
                                progress.update("misc?");
                                if(daily || output.equals("misc")) {
                                    progress.update("users?");
                                    files += doOutput("users");
                                    progress.update("usersa?");
                                    files += doOutput("usersa");
                                    progress.update("translators?");
                                    files += doOutput("translators");
                                }
                                progress.update("finishing");
                            } finally {
                                progress.close();
                            }
                        }
                    });
                }      
            }
        } else if(action.equals("srl_db_update")) {
            WebContext subCtx = (WebContext)ctx.clone();
            subCtx.addQuery("dump",vap);
            subCtx.addQuery("action","srl_db_update");
            ctx.println("<br>");
//            XMLSource mySrc = makeDBSource(ctx, null, CLDRLocale.ROOT);
            ElapsedTimer aTimer = new ElapsedTimer();
            getSTFactory().doDbUpdate(subCtx, this); 
            ctx.println("<br>(dbupdate took " + aTimer+")");
            ctx.println("<br>");
        } else if(action.equals("srl_vxport")) {
            SurveyLog.logger.warning("vxport");
            File inFiles[] = getInFiles();
            int nrInFiles = inFiles.length;
            boolean found = false;
            String theLocale = null;
            File outdir = new File("./xport/");
            for(int i=0;(!found) && (i<nrInFiles);i++) {
             try{
                String localeName = inFiles[i].getName();
                theLocale = fileNameToLocale(localeName).getBaseName();
                SurveyLog.logger.warning("#vx "+theLocale);
                XMLSource dbSource = makeDBSource(CLDRLocale.getInstance(theLocale), true);
                CLDRFile file = makeCLDRFile(dbSource).setSupplementalDirectory(supplementalDataDir);
                  OutputStream files = new FileOutputStream(new File(outdir,localeName),false); // Append
//                PrintWriter pw = new PrintWriter(files);
    //            file.write(WebContext.openUTF8Writer(response.getOutputStream()));
                PrintWriter ow;
                file.write(ow=WebContext.openUTF8Writer(files));
                ow.close();
//              pw.close();
                files.close();
                
                } catch(IOException exception){
                    SurveyLog.logException(exception, ctx);
                }
            }
        } else if(action.equals("add_locale")) {
            actionCtx.addQuery("action", action);
            ctx.println("<hr><br><br>");
            String loc = actionCtx.field("loc");
            
            ctx.println("<div class='ferrbox'><B>Note:</B> before using this interface, you must read <a href='http://cldr.unicode.org/development/adding-locales'>This Page</a> especially about adding core data.</div>");

            ctx.println("This interface lets you create a new locale, and its parents.  Before continuing, please make sure you have done a" +
                    " SVN update to make sure the file doesn't already exist." +
                    " After creating the locale, it should be added to SVN as well.<hr>");
            
            ctx.print("<form action='"+actionCtx.base()+"'>");
            ctx.print("<input type='hidden' name='action' value='"+action+"'>");
            ctx.print("<input type='hidden' name='dump' value='"+vap+"'>");         
            ctx.println("<label>Add Locale: <input name='loc' value='"+loc+"'></label>");
            ctx.println("<input type=submit value='Check'></form>");
            
            
            
            if(loc.length()>0) {
                ctx.println("<hr>");
                CLDRLocale cloc = CLDRLocale.getInstance(loc);
                Set<CLDRLocale> locs = this.getLocalesSet();
                
                int numToAdd=0;
                String reallyAdd = ctx.field("doAdd");
                boolean doAdd = reallyAdd.equals(loc);
                
                for(CLDRLocale aloc : cloc.getParentIterator()) {
                    ctx.println("<b>"+aloc.toString()+"</b> : " + aloc.getDisplayName(ctx.displayLocale)+"<br>");
                    ctx.print("<blockquote>");
                    try {
                        if(locs.contains(aloc)) { 
                            ctx.println(
                                    ctx.iconHtml("squo", "done with this locale")+
                                    "... already installed.<br>");
                            continue;
                        }
                        File baseDir = new File(fileBase);
                        File xmlFile = new File(baseDir,aloc.getBaseName()+".xml");
                        if(xmlFile.exists()) { 
                            ctx.println(
                                    ctx.iconHtml("ques", "done with this locale")+
                                    "... file ( " + xmlFile.getAbsolutePath() +" ) exists!. [consider update]<br>");
                            continue;
                        }
                        
                        if(!doAdd) {
                            ctx.println(ctx.iconHtml("star", "ready to add!")+
                                    " ready to add " + xmlFile.getName() +"<br>");
                            numToAdd++;
                        } else {
                            CLDRFile emptyFile = SimpleFactory.makeFile(aloc.getBaseName());
                            try {
                                PrintWriter utf8OutStream = new PrintWriter(
                                    new OutputStreamWriter(
                                        new FileOutputStream(xmlFile), "UTF8"));
                                emptyFile.write(utf8OutStream);
                                utf8OutStream.close();
                                ctx.println(ctx.iconHtml("okay", "Added!")+
                                        " Added " + xmlFile.getName() +"<br>");
                                numToAdd++;
                                //            } catch (UnsupportedEncodingException e) {
                                //                throw new InternalError("UTF8 unsupported?").setCause(e);
                            } catch (IOException e) {
                                SurveyLog.logException(e, ctx);
                                SurveyLog.logger.warning("While adding "+xmlFile.getAbsolutePath());
                                e.printStackTrace();
                                ctx.println(ctx.iconHtml("stop","err")+" Error While adding "+xmlFile.getAbsolutePath()+" - " + e.toString()+"<br><pre>");
                                ctx.print(e);
                                ctx.print("</pre><br>");
                            }

                            
                        }
                        
                    } finally {
                        ctx.print("</blockquote>");
                    }
                }
                if(!doAdd && numToAdd>0) {
                    ctx.print("<form action='"+actionCtx.base()+"'>");
                    ctx.print("<input type='hidden' name='action' value='"+action+"'>");
                    ctx.print("<input type='hidden' name='dump' value='"+vap+"'>");         
                    ctx.print("<input type='hidden' name='loc' value='"+loc+"'></label>");
                    ctx.print("<input type='hidden' name='doAdd' value='"+loc+"'></label>");
                    ctx.print("<input type=submit value='Add these "+numToAdd+" file(s) for "+loc+"!'></form>");
                } else if(doAdd) {
                    ctx.print("<br>Added " + numToAdd +" files.<br>");
                    this.resetLocaleCaches();
                    ctx.print("<br>Locale caches reset. Remember to check in the file(s).<br>");
                } else {
                    ctx.println("(No files would be added.)<br>");
                }
            }
        } else if(action.equals("load_all")) {
            File[] inFiles = getInFiles();
            int nrInFiles = inFiles.length;

            actionCtx.addQuery("action",action);
            ctx.println("<hr><br><br>");
            if(!actionCtx.hasField("really_load")) {
                actionCtx.addQuery("really_load","y");
                ctx.println("<b>Really Load "+nrInFiles+" locales?? <a class='ferrbox' href='"+actionCtx.url()+"'>YES</a><br>");
            } else {
                
                startupThread.addTask(new SurveyThread.SurveyTask("load all locales")
                {
                    public void run() throws Throwable 
                    {
                        loadAllLocales(null,this);
                        ElapsedTimer et = new ElapsedTimer();
                        int n = vet.updateStatus();
                        SurveyLog.logger.warning("Done updating "+n+" statuses [locales] in: " + et + "<br>");
                    }
                });
        ctx.println("<h2>Task Queued.</h2>");
            }
            
        } else if(action.equals("specialusers")) {
            ctx.println("<hr>Re-reading special users list...<br>");
            Set<UserRegistry.User> specials = reg.getSpecialUsers(true); // force reload
            if(specials==null) {
                ctx.println("<b>No users are special.</b> To make them special (allowed to vet during closure) <code>specialusers.txt</code> in the cldr directory. Format as follows:<br> "+
                            "<blockquote><code># this is a comment<br># The following line makes user 295 special<br>295<br></code>"+
                            "</code></blockquote><br>");
            } else {
                ctx.print("<br><hr><i>Users allowed special access:</i>");
                ctx.print("<table class='list' border=1 summary='special access users'>");
                ctx.print("<tr><th>"+specials.size()+" Users</th></tr>");
                int nn=0;
                for(UserRegistry.User u : specials) {
                    ctx.println("<tr class='row"+(nn++ % 2)+"'>");
                    ctx.println("<td>"+u.toString()+"</td>");
                    ctx.println("</tr>");
                }
                ctx.print("</table>");
            }
        } else if(action.equals("specialmsg")) {
            ctx.println("<hr>");
            
            // OGM---
            // seconds
            String timeQuantity = "seconds";
            long timeInMills = (1000);
            /*
            // minutes
             String timeQuantity = "minutes";
             long timeInMills = (1000)*60;
            */
            ctx.println("<h4>Set outgoing message (leave blank to unset)</h4>");
            long now = System.currentTimeMillis();
            if(ctx.field("setogm").equals("1")) {
                specialHeader=ctx.field("ogm");
                if(specialHeader.length() ==0) {
                    specialTimer = 0;
                } else {
                    long offset = ctx.fieldInt("ogmtimer",-1);
                    if(offset<0) {
                        // no change.
                    } else if(offset == 0) {
                        specialTimer = 0; // clear
                    } else {
                        specialTimer = (timeInMills * offset) + now;
                    }
                }
                String setlockout = ctx.field("setlockout");
                if(lockOut != null) {
                    if(setlockout.length()==0) {
                        ctx.println("Lockout: <b>cleared</b><br>");
                        lockOut = null;
                    } else if(!lockOut.equals(setlockout)) {
                        lockOut = setlockout;
                        ctx.println("Lockout changed to: <tt class='codebox'>"+lockOut+"</tt><br>");
                    }
                } else {
                    if(setlockout.length()>0) {
                        lockOut = setlockout;
                        ctx.println("Lockout set to: <tt class='codebox'>"+lockOut+"</tt><br>");
                    }
                }
            }
            if((specialHeader != null) && (specialHeader.length()>0)) {
                ctx.println("<div style='border: 2px solid gray; margin: 0.5em; padding: 0.5em;'>" + specialHeader + "</div><br>");
                if(specialTimer == 0) {
                    ctx.print("Timer is <b>off</b>.<br>");
                } else if(now>specialTimer) {
                    ctx.print("Timer is <b>expired</b><br>");
                } else {
                    ctx.print("Timer remaining: " + timeDiff(now,specialTimer));
                }
            } else {
                ctx.println("<i>none</i><br>");
            }
            ctx.print("<form action='"+actionCtx.base()+"'>");
            ctx.print("<input type='hidden' name='action' value='"+"specialmsg"+"'>");
            ctx.print("<input type='hidden' name='dump' value='"+vap+"'>");
            ctx.print("<input type='hidden' name='setogm' value='"+"1"+"'>");
            ctx.print("<label>Message: <input name='ogm' value='"+((specialHeader==null)?"":specialHeader.replaceAll("'","\"").replaceAll(">","&gt;"))+
                    "' size='80'></label><br>");
            ctx.print("<label>Timer: (use '0' to clear) <input name='ogmtimer' size='10'>"+timeQuantity+"</label><br>");
            ctx.print("<label>Lockout Password:  [unlock=xxx] <input name='setlockout' value='"+
                ((lockOut==null)?"":lockOut)+"'></label><br>");
            ctx.print("<input type='submit' value='set'>");
            ctx.print("</form>");
            // OGM---
        } else if(action.equals("srl_test0")) {
            String test0 = ctx.field("test0");
            
            ctx.print("<h1>test0 over " + test0 + "</h1>");
            ctx.print("<i>Note: xpt statistics: " + xpt.statistics() +"</i><hr>");
            SurveyMain.throwIfBadLocale(test0);
            ctx.print(new ElapsedTimer("Time to do nothing: {0}").toString()+"<br>");
            
            // collect paths
            ElapsedTimer et = new ElapsedTimer("Time to collect xpaths from " + test0 + ": {0}");
            Set<Integer> paths = new HashSet<Integer>();
            String sql = "SELECT xpath from CLDR_DATA where locale=\""+test0+"\"";
            try {
                Connection conn = dbUtils.getDBConnection();
                Statement s = conn.createStatement();
                ResultSet rs = s.executeQuery(sql);
                while(rs.next()) {
                    paths.add(rs.getInt(1));
                }
                rs.close();
                s.close();
            } catch ( SQLException se ) {
                SurveyLog.logException(se, ctx);
                String complaint = " Couldn't query xpaths of " + test0 +" - " + DBUtils.unchainSqlException(se) + " - " + sql;
                SurveyLog.logger.warning(complaint);
                ctx.println("<hr><font color='red'>ERR: "+complaint+"</font><hr>");
            }
            ctx.print("Collected "+paths.size()+" paths, " + et + "<br>");
            
            // Load paths
            et = new ElapsedTimer("load time: {0}");
            for(int xp : paths) {
                xpt.getById(xp);
            }
            ctx.print("Load "+paths.size()+" paths from " + test0 + " : " + et+"<br>");
            
            final int TEST_ITER=100000;
            et = new ElapsedTimer("Load " + TEST_ITER+"*"+paths.size()+"="+(TEST_ITER*paths.size())+" xpaths: {0}");
            for(int j=0;j<TEST_ITER;j++) {
                for(int xp : paths) {
                    xpt.getById(xp);
                }
            }
            ctx.print("Test: " + et+ "<br>");
       } else if(action.length()>0) {
            ctx.print("<h4 class='ferrbox'>Unknown action '"+action+"'.</h4>");
        }
--%>