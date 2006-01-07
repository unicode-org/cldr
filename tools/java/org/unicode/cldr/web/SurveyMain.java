/*
 ******************************************************************************
 * Copyright (C) 2004-2005, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.web;

import java.io.*;
import java.util.*;
import java.lang.ref.SoftReference;

// logging
import java.util.logging.Level;
import java.util.logging.Logger;

// servlet imports
import javax.servlet.*;
import javax.servlet.http.*;

// DOM imports
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.lang.UCharacter;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.test.util.ElapsedTimer;

import org.unicode.cldr.test.*;
import org.unicode.cldr.util.*;
import org.unicode.cldr.icu.*;

// sql imports
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Properties;


import com.ibm.icu.lang.UCharacter;

public class SurveyMain extends HttpServlet {
    /**
    * URL prefix for  help
    */
    public static final String CLDR_HELP_LINK = "http://bugs.icu-project.org/cgibin/cldrwiki.pl?SurveyToolHelp";
    /** 
    * icon for help
    */
    public static final String CLDR_ALERT_ICON = "/~srloomis/alrt.gif";
    /**
    * hash tag: vetting context
    */
    public static final String SUBVETTING = "//v";
    /**
     * hash tag: new context
     */
    public static final String SUBNEW = "//n"; 
    public static final String NOCHANGE = "nochange";
    public static final String CURRENT = "current";
    public static final String PROPOSED = "proposed";
    public static final String NEW = "new";
    public static final String DRAFT = "draft";
    public static final String UNKNOWNCHANGE = "Click to suggest replacement";
    public static final String DONTCARE = "abstain";
    public static final String CONFIRM = "confirm";
    public static final String CHANGETO = "change to";
    public static final String PROPOSED_DRAFT = "proposed-draft";

    public static final String MODIFY_THING = "<span title='You are allowed to modify this locale.'>\u270D</span>";             // was: F802

    // SYSTEM PROPERTIES
    public static  String vap = System.getProperty("CLDR_VAP"); // Vet Access Password
    public static  String vetdata = System.getProperty("CLDR_VET_DATA"); // dir for vetted data
    public static  String vetweb = System.getProperty("CLDR_VET_WEB"); // dir for web data
    public static  String cldrLoad = System.getProperty("CLDR_LOAD_ALL"); // preload all locales?
    static String fileBase = System.getProperty("CLDR_COMMON") + "/main"; // not static - may change lager
    static String specialMessage = System.getProperty("CLDR_MESSAGE"); //  static - may change later
    static String specialHeader = System.getProperty("CLDR_HEADER"); //  static - may change later
    public static java.util.Properties survprops = null;
    public static String cldrHome = null;

    // Logging
    public static Logger logger = Logger.getLogger("org.unicode.cldr.SurveyMain");
    public static java.util.logging.Handler loggingHandler = null;
            
    public static final String LOGFILE = "cldr.log";        // log file of all changes
    public static final ULocale inLocale = new ULocale("en"); // locale to use to 'localize' things

    static final String QUERY_PASSWORD = "pw";
    static final String QUERY_PASSWORD_ALT = "uid";
    static final String QUERY_EMAIL = "email";
    static final String QUERY_SESSION = "s";
    static final String SURVEYTOOL_COOKIE_SESSION = "com.org.unicode.cldr.web.CookieSession.id";
    static final String SURVEYTOOL_COOKIE_NONE = "0";
    static final String PREF_SHOWCODES = "p_codes";
    static final String PREF_SORTMODE = "p_sort";
    static final String PREF_SORTMODE_CODE = "code";
    static final String PREF_SORTMODE_ALPHA = "alpha";
    static final String PREF_SORTMODE_WARNING = "interest";
    static final String PREF_SORTMODE_NAME = "name";
    static final String PREF_SORTMODE_DEFAULT = PREF_SORTMODE_WARNING;
    //    static final String PREF_SORTMODE_DEFAULT = PREF_SORTMODE_WARNING;
    // types of data
    static final String LOCALEDISPLAYNAMES = "//ldml/localeDisplayNames/";
    public static final String NUMBERSCURRENCIES = LDMLConstants.NUMBERS + "/currencies";
    public static final String CURRENCYTYPE = "//ldml/numbers/currencies/currency[@type='";
    /**
     *  All of the data items under LOCALEDISPLAYNAMES (menu items)
     */
    static final String LOCALEDISPLAYNAMES_ITEMS[] = { 
        LDMLConstants.LANGUAGES, LDMLConstants.SCRIPTS, LDMLConstants.TERRITORIES,
        LDMLConstants.VARIANTS, LDMLConstants.KEYS, LDMLConstants.TYPES
    };
    
    // 
    public static final String OTHERROOTS_ITEMS[] = {
        LDMLConstants.CHARACTERS,
        NUMBERSCURRENCIES,
        LDMLConstants.NUMBERS /*+ "/" */,
        "timeZoneNames",
        LDMLConstants.DATES + "/calendars",
        LDMLConstants.DATES /* + "/" */
    };
    public static final String RAW_MENU_ITEM = "raw";
    public static final String TEST_MENU_ITEM = "test";
    
    
    public static String xMAIN = "General";
    public static String xOTHER = "Misc";
    public static String xNODESET = "NodeSet@"; // pseudo-type used to store nodeSets in the hash
    public static String xREMOVE = "REMOVE";
    public UserRegistry reg = null;
    public XPathTable   xpt = null;
    public LocaleChangeRegistry lcr = new LocaleChangeRegistry();
    
    public static String xREVIEW = "Review and Submit";
    public static String xSAVE = "Add Changes on This Page";
    
    /*private int n = 0;
    synchronized int getN() {
        return n++;
    }*/
                    
    /** status **/
    ElapsedTimer uptime = new ElapsedTimer("ST uptime: {0}");
    ElapsedTimer startupTime = new ElapsedTimer("{0} until first GET/POST");
    public static String isBusted = null;
            
    public final void init(final ServletConfig config)
    throws ServletException {
        super.init(config);
        
        cldrHome = config.getInitParameter("cldr.home");
        
        doStartup();
        //      throw new UnavailableException("Document path not set.");
    }

    public SurveyMain() {
        // null
    }
                    
    /**
    * output MIME header, build context, and run code..
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)    throws IOException, ServletException
    {
        doGet(request,response); // eeh.
    }
                    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
    {
        String startupMsg = null;
        if(startupTime != null) {
            startupMsg = (startupTime.toString());
            logger.info(startupMsg);
            startupTime = null;
        }
        com.ibm.icu.dev.test.util.ElapsedTimer reqTimer = new com.ibm.icu.dev.test.util.ElapsedTimer();
        
        response.setContentType("text/html; charset=utf-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires",0);
        response.setHeader("Pragma","no-cache");
        response.setDateHeader("Max-Age",0);
        pages++;
        
        if(isBusted != null) {
            PrintWriter out = response.getWriter();
            out.println("<html>");
            out.println("<head>");
            out.println("<title>CLDR tool broken</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>CLDR Survey Tool is down, because:</h1>");
            out.println("<tt>" + isBusted + "</tt><br />");
            out.println("<hr />");
            out.println("Please try this link for info: <a href='http://dev.icu-project.org/cgi-bin/cldrwiki.pl?SurveyTool/Status'>http://dev.icu-project.org/cgi-bin/cldrwiki.pl?SurveyTool/Status</a><br />");
            out.println("<hr />");
            out.println("<i>Note: to prevent thrashing, this servlet is down until someone reloads it. " + 
                        " (you are unhappy visitor #" + pages + ")</i>");
            return;        
        }
        
        WebContext ctx = new WebContext(request,response);
        ctx.reqTimer = reqTimer;
        // TODO: ctx.dbsrc..
        ctx.sm = this;
        
        if(ctx.field("dump").equals(vap)) {
            doDump(ctx);
        } else if(ctx.field("sql").equals(vap)) {
            doSql(ctx);
        } else {
            doSession(ctx); // Session-based Survey main
        }
        ctx.close();
    }
                    
    
    /** Admin functions
    */
    private void doSql(WebContext ctx)
    {
        printHeader(ctx, "Raw SQL");
        String q = ctx.field("q");
        ctx.println("<h1>raw sql</h1>");
        ctx.println("<form method=POST action='" + ctx.base() + "'>");
        ctx.println("<input type=hidden name=sql value='" + vap + "'>");
        ctx.println("SQL: <input class='inputbox' name=q size=80 cols=80 value='" + q + "'><br />");
        ctx.println("<label style='border: 1px'><input type=checkbox name=unltd>Show all?</label> ");
        ctx.println("<label style='border: 1px'><input type=checkbox name=isUpdate>U/I/D?</label> ");
        ctx.println("<label style='border: 1px'><input type=checkbox name=isUser>UserDB?</label> ");
        ctx.println("<input type=submit name=do value=Query>");
        ctx.println("</form>");
        if(q.length()>0) {
            logger.severe("Raw SQL: " + q);
            ctx.println("<hr />");
            ctx.println("query: <tt>" + q + "</tt><br /><br />");
            try {
                int i,j;
                
                com.ibm.icu.dev.test.util.ElapsedTimer et = new com.ibm.icu.dev.test.util.ElapsedTimer();
                
                Connection conn = null;
                if(ctx.field("isUser").length()>0) {
                    conn = getU_DBConnection();
                } else {
                    conn = getDBConnection();
                }
                Statement s = conn.createStatement();
                //s.setQueryTimeout(120); // 2 minute timeout. Not supported by derby..
                if(ctx.field("isUpdate").length()>0) {
                    int rc = s.executeUpdate(q);
                    conn.commit();
                    ctx.println("<br />Result: " + rc + " row(s) affected.<br />");
                } else {
                    ResultSet rs = s.executeQuery(q); 
                    conn.commit();
                    
                    ResultSetMetaData rsm = rs.getMetaData();
                    int cc = rsm.getColumnCount();
                    
                    ctx.println("<table class='list' border='2'><tr><th>#</th>");
                    for(i=1;i<=cc;i++) {
                        ctx.println("<th style='font-size: 50%'>"+rsm.getColumnName(i)+ "<br />");
                        int t = rsm.getColumnType(i);
                        switch(t) {
                            case java.sql.Types.VARCHAR: ctx.println("VARCHAR"); break;
                            case java.sql.Types.INTEGER: ctx.println("INTEGER"); break;
                            case java.sql.Types.BLOB: ctx.println("BLOB"); break;
                            default: ctx.println("type#"+t); break;
                        }
                        ctx.println("("+rsm.getColumnDisplaySize(i)+")");
                        ctx.println("</th>");
                    }
                    ctx.println("</tr>");
                    int limit = 30;
                    if(ctx.field("unltd").length()>0) {
                        limit = 99999;
                    }
                    for(j=0;rs.next()&&(j<limit);j++) {
                        ctx.println("<tr><th>" + j + "</th>");
                        for(i=1;i<=cc;i++) {
                            String v = rs.getString(i);
                            if(v != null) {
                                ctx.println("<td>" + v + "</td>");
                            } else {
                                ctx.println("<td style='background-color: gray'></td>");
                            }
                        }
                        ctx.println("</tr>");
                    }
                    
                    ctx.println("</table>");
                }
                
                ctx.println("elapsed time: " + et + "<br />");
                //conn.close(); 
                //     (auto close)
            } catch(SQLException se) {
                String complaint = "SQL err: " + unchainSqlException(se);
                
                ctx.println("<pre style='border: 1px solid red; margin: 1em; padding: 1em;'>" + complaint + "</pre>" );
                logger.severe(complaint);
            } catch(Throwable t) {
                String complaint = t.toString();
                ctx.println("<pre style='border: 1px solid red; margin: 1em; padding: 1em;'>" + complaint + "</pre>" );
                logger.severe("Err in SQL execute: " + complaint);
            }
        }
        printFooter(ctx);
    }
    
    private void doDump(WebContext ctx)
    {
        printHeader(ctx, "Dump System Info");
        ctx.println("<h1>System info</h1>");
        ctx.println("<div class='pager'>");
        ctx.println(uptime + ", " + pages + " pages served.<br/>");
        Runtime r = Runtime.getRuntime();
        double total = r.totalMemory();
        total = total / 1024;
        double free = r.freeMemory();
        free = free / 1024;
        ctx.println("Free memory: " + free + "M / " + total + "M.<br/>");
        r.gc();
        ctx.println("Ran gc();<br/>");
        {
            Hashtable nodeHash = getNodeHash();            
            if(nodeHash != null) {
                ctx.println("Node hash has " + nodeHash.size() + " items");
            } else {
                ctx.println("Node hash is null");
            }
            double avgPuts = ((double)nodeHashPuts)/((double)nodeHashCreates);
            ctx.println(", and was created " +  nodeHashCreates + " times. <br/>");
            ctx.println("(For " + nodeHashPuts + " puts, that's an average of " + avgPuts + ")<br/>");
        }
        ctx.println("String hash has " + stringHash.size() + " items.<br/>");
        ctx.println("xString hash info: " + xpt.statistics() +"<br />");
        ctx.println("</div>");
        ctx.println("<hr/>");
        ctx.println("<h1>Current Sessions</h1>");
        ctx.println("<table border=1><tr><th>id</th><th>age (h:mm:ss)</th><th>user</th><th>what</th></tr>");
        for(Iterator li = CookieSession.getAll();li.hasNext();) {
            CookieSession cs = (CookieSession)li.next();
            ctx.println("<tr><td>" + cs.id + "</td>");
            ctx.println("<td>" + timeDiff(cs.last) + "</td>");
            if(cs.user != null) {
                ctx.println("<td>" + cs.user.email + "<br/>" + 
                            cs.user.name + "<br/>" + 
                            cs.user.org + "</td>");
            } else {
                ctx.println("<td><i>Guest</i></td>");
            }
            ctx.println("<td>");
            Hashtable lh = cs.getLocales();
            Enumeration e = lh.keys();
            if(e.hasMoreElements()) { 
                for(;e.hasMoreElements();) {
                    String k = e.nextElement().toString();
                    ctx.println(new ULocale(k).getDisplayName() + " ");
                }
            }
            ctx.println("</td>");
            
            ctx.println("<td>");
            printLiveUserMenu(ctx, cs);
            if(cs.id.equals(ctx.field("unlink"))) {
                cs.remove();
                ctx.println("<br /><b>Removed.</b>");
            }
            ctx.println("</td>");
            
            ctx.println("</tr>");
        }
        ctx.println("</table>");

        if(ctx.field("loadAll").equals("all")) {
            com.ibm.icu.dev.test.util.ElapsedTimer allTime = new com.ibm.icu.dev.test.util.ElapsedTimer("Time to load all: {0}");
            logger.info("Loading all..");            
            File[] inFiles = getInFiles();
            int nrInFiles = inFiles.length;
            int ti = 0;
            for(int i=0;i<nrInFiles;i++) {
                String localeName = inFiles[i].getName();
                int dot = localeName.indexOf('.');
                if(dot !=  -1) {
                    localeName = localeName.substring(0,dot);
                    if((i>0)&&((i%50)==0)) {
                        logger.info("   "+ i + "/" + nrInFiles + ". " + usedK() + "K mem used");
                        ctx.println("   "+ i + "/" + nrInFiles + ". " + usedK() + "K mem used<br />");
                    }
                    try {
                        ULocale locale = new ULocale(localeName);
                        //                        WebContext xctx = new WebContext(false);
                        //                        xctx.setLocale(locale);
                        makeCLDRFile(makeDBSource(null, locale));
                    } catch(Throwable t) {
                        String complaint = ("Error loading: " + localeName + " - " + t.toString() + " ...");
                        logger.severe("loading all: " + complaint);
                        ctx.println(complaint + "<br />");
                    }
                }
            }
            logger.info("Loaded all. " + allTime);
            ctx.println("Loaded all." + allTime + "<br />");
        } else {
            ctx.println("<br />[<a href='" + ctx.base() + "?dump=" + vap + "&loadAll=all'>Load All (S L O W)</a>]<br />");
        }
        
        printFooter(ctx);
    }
    
    /* 
     * print menu of stuff to 'work with ' a live user session..
     */
    private void printLiveUserMenu(WebContext ctx, CookieSession cs) {
        ctx.println("<a href='" + ctx.base() + "?&s=" + cs.id + "'>" + "become" + "</a> |");
        ctx.println("<a href='" + ctx.base() + "?dump=" + vap + "&unlink=" + cs.id + "'>" + "logout" + "</a>");
    }
    
     
    /**
    * print the header of the thing
    */
    public void printHeader(WebContext ctx, String title)
    {
        ctx.println("<html>");
        ctx.println("<head>");
        ctx.println("<link rel='stylesheet' type='text/css' href='" + ctx.context("surveytool.css") + "' />");
        // + "http://www.unicode.org/webscripts/standard_styles.css'>"
        ctx.println("<title>CLDR Vetting | ");
        if(ctx.locale != null) {
            ctx.print(ctx.locale.getDisplayName() + " | ");
        }
        ctx.println(title + "</title>");
        ctx.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
        ctx.println("</head>");
        ctx.println("<body>");
        if((specialHeader != null) && (specialHeader.length()>0)) {
            ctx.println("<div style='border: 2px solid gray; margin: 0.5 em; padding: 0.5 em;'>" + specialHeader + "</div><br />");
        }
        ctx.println("<script type='text/javascript'><!-- \n" +
                    "function show(what)\n" +
                    "{document.getElementById(what).style.display=\"block\";\ndocument.getElementById(\"h_\"+what).style.display=\"none\";}\n" +
                    "function hide(what)\n" +
                    "{document.getElementById(what).style.display=\"none\";\ndocument.getElementById(\"h_\"+what).style.display=\"block\";}\n" +
                    "--></script>");
        
    }
    
    public void printFooter(WebContext ctx)
    {
        ctx.println("</body>");
        ctx.println("<hr>");
        ctx.println("<div style='float: right; text-size: 60%;'>#" + pages + " served in " + ctx.reqTimer + "</div>");
        ctx.println("<a href='http://www.unicode.org'>Unicode</a> | <a href='http://www.unicode.org/cldr'>Common Locale Data Repository</a> <br/>");
        ctx.println("</html>");
    }
    
    /**
     * process the '_' parameter, if present, and set the locale.
     */
    public void setLocale(WebContext ctx)
    {
        String locale = ctx.field("_");
        if(locale != null) {  // knock out some bad cases
            if((locale.indexOf('.') != -1) ||
               (locale.indexOf('/') != -1)) {
                locale = null;
            }
        }
        if(locale != null && (locale.length()>0)) {
            ctx.setLocale(new ULocale(locale));
        }
    }
    
    /**
    * set the session.
     */
    String setSession(WebContext ctx) {
        String message = null;
        // get the context
        CookieSession mySession = null;
        String myNum = ctx.field(QUERY_SESSION);
        // get the uid
        String password = ctx.field(QUERY_PASSWORD);
        if(password.length()==0) {
            password = ctx.field(QUERY_PASSWORD_ALT);
        }
        String email = ctx.field(QUERY_EMAIL);
        UserRegistry.User user;
        user = reg.get(password,email);
        HttpSession httpSession = ctx.request.getSession(true);
        boolean idFromSession = false;
        if(myNum.equals(SURVEYTOOL_COOKIE_NONE)) {
            httpSession.removeAttribute(SURVEYTOOL_COOKIE_SESSION);
        }
        if(user != null) {
            mySession = CookieSession.retrieveUser(user.email);
            if(mySession != null) {
                message = "<i>Reconnecting to your previous session.</i>";
                myNum = mySession.id;
            }
        }

        // Retreive a number from the httpSession if present
        if((httpSession != null) && (mySession == null) && ((myNum == null) || (myNum.length()==0))) {
            String aNum = (String)httpSession.getAttribute(SURVEYTOOL_COOKIE_SESSION);
            if((aNum != null) && (aNum.length()>0)) {
                myNum = aNum;
                idFromSession = true;
            }
        } 
        
        if((mySession == null) && (myNum != null) && (myNum.length()>0)) {
            mySession = CookieSession.retrieve(myNum);
            if(mySession == null) {
                idFromSession = false;
            }
            if((mySession == null)&&(!myNum.equals(SURVEYTOOL_COOKIE_NONE))) {
                message = "<i>(Sorry, This session has expired. ";
                if(user == null) {
                    message = message + "You may have to log in again. ";
                }
                    message = message + ")</i><br />";
            }
        }
        if((idFromSession==false) && (httpSession!=null) && (mySession!=null)) { // can we elide the 's'?
            String aNum = (String)httpSession.getAttribute(SURVEYTOOL_COOKIE_SESSION);
            if((aNum != null) && (mySession.id.equals(aNum))) {
                idFromSession = true; // it would have matched.
            } else {
       //         ctx.println("[Confused? cs="+aNum +", s=" + mySession.id + "]");
            }
        }
        if(mySession == null) {
            mySession = new CookieSession(user==null);
            if(!myNum.equals(SURVEYTOOL_COOKIE_NONE)) {
//                ctx.println("New session: " + mySession.id + "<br />");
            }
            idFromSession = false;
        }
        ctx.session = mySession;
        
        if(!idFromSession) { // suppress 's' if cookie was valid
            ctx.addQuery(QUERY_SESSION, mySession.id);
        } else {
      //      ctx.println("['s' suppressed]");
        }

        if(httpSession != null) {
            httpSession.setAttribute(SURVEYTOOL_COOKIE_SESSION, mySession.id);
            httpSession.setMaxInactiveInterval(CookieSession.USER_TO / 1000);
        }
        
        if(user != null) {
            ctx.session.setUser(user); // this will replace any existing session by this user.
        } else {
            if( (email !=null) && (email.length()>0)) {
                message = ("<strong>login failed.</strong><br />");
            }
        }
        CookieSession.reap();
        return message;
    }
    
    /**
     * Print menu of choices available to the user.
     */
    public void printUserMenu(WebContext ctx) {
        ctx.println("<b>Welcome " + ctx.session.user.name + " (" + ctx.session.user.org + ") !</b>");
        ctx.println("<a href=\"" + ctx.base() + "?s="+SURVEYTOOL_COOKIE_NONE+"\">[Sign Out (Session remains active)]</a><br/>");
        if(UserRegistry.userIsAdmin(ctx.session.user)) {
            ctx.println("<b>You are an Admin:</b> ");
            ctx.println("<a href='" + ctx.base() + "?dump=" + vap + "'>[Stats]</a>");
            if(ctx.session.user.id == 1) {
                ctx.println("<a href='" + ctx.base() + "?sql=" + vap + "'>[Raw SQL]</a>");
            }
            ctx.println("<br/>");
        }
        if(UserRegistry.userIsTC(ctx.session.user)) {
            ctx.println("You are: <b>A CLDR TC Member:</b> ");
            ctx.println("<a href='" + ctx.jspLink("adduser.jsp") +"'>[Add User]</a> | ");
            ctx.println("<a href='" + ctx.url() + ctx.urlConnector() +"do=list'>[Manage " + ctx.session.user.org + " Users]</a>");
            ctx.println("<br/>");
        } else {
            if(UserRegistry.userIsVetter(ctx.session.user)) {
                ctx.println("You are a: <b>Vetter:</b> ");
                ctx.println("<a href='" + ctx.url() + ctx.urlConnector() +"do=list'>[List " + ctx.session.user.org + " Users]</a>");
                ctx.println("<br/>");
            } else if(UserRegistry.userIsStreet(ctx.session.user)) {
                ctx.println("You are a: <b>Guest Contributor</b> ");
                ctx.println("<br/>");
            } else if(UserRegistry.userIsLocked(ctx.session.user)) {
                ctx.println("<b>LOCKED: Note: your account is currently locked. Please contact " + ctx.session.user.org + "'s CLDR Technical Committee member.</b> ");
                ctx.println("<br/>");
            }
        }
    }
    /**
    * Handle creating a new user
    */
    public void doNew(WebContext ctx) {
        printHeader(ctx, "New User");
        printUserMenu(ctx);
        ctx.printHelpLink("/AddModifyUser");
        ctx.println("<a href='" + ctx.url() + "'><b>SurveyTool main</b></a><hr />");
        
        String new_name = ctx.field("new_name");
        String new_email = ctx.field("new_email");
        String new_locales = ctx.field("new_locales");
        String new_org = ctx.field("new_org");
        int new_userlevel = ctx.fieldInt("new_userlevel",-1);
        
        if(!UserRegistry.userCreateOtherOrgs(ctx.session.user)) {
            new_org = ctx.session.user.org; // if not admin, must create user in the same org
        }
        
        if((new_name == null) || (new_name.length()<=0)) {
            ctx.println("<div class='sterrmsg'>Please fill in a name.. hit the Back button and try again.</div>");
        } else if((new_email == null) ||
                  (new_email.length()<=0) ||
                  ((-1==new_email.indexOf('@'))||(-1==new_email.indexOf('.')))  ) {
            ctx.println("<div class='sterrmsg'>Please fill in an <b>email</b>.. hit the Back button and try again.</div>");
        } else if((new_org == null) || (new_org.length()<=0)) { // for ADMIN
            ctx.println("<div class='sterrmsg'>Please fill in an <b>Organization</b>.. hit the Back button and try again.</div>");
        } else if(new_userlevel<0) {
            ctx.println("<div class='sterrmsg'>Please fill in a <b>user level</b>.. hit the Back button and try again.</div>");
        } else {
            UserRegistry.User u = reg.getEmptyUser();
            
            u.name = new_name;
            u.userlevel = UserRegistry.userCanCreateUserOfLevel(ctx.session.user, new_userlevel);
            u.email = new_email;
            u.org = new_org;
            u.locales = new_locales;
            u.password = UserRegistry.makePassword(u.email+u.org+ctx.session.user.email);
            
            UserRegistry.User registeredUser = reg.newUser(ctx, u);
            
            if(registeredUser == null) {
                if(reg.get(new_email) != null) { // already exists..
                    ctx.println("<div class='sterrmsg'>A user with that email, <tt>" + new_email + "</tt> already exists. Couldn't add user. </div>");                
                } else {
                    ctx.println("<div class='sterrmsg'>Couldn't add user <tt>" + new_email + "</tt> - an unknown error occured.</div>");
                }
            } else {
                ctx.println("<i>user added.</i>");
                registeredUser.printPasswordLink(ctx);
            }
        }
        
        printFooter(ctx);
    }
    
    /**
    * User list manamgement
    */
    static final String LIST_ACTION_SETLEVEL = "set_userlevel_";
    static final String LIST_ACTION_NONE = "-";
    static final String LIST_ACTION_SHOW_PASSWORD = "showpassword_";
    static final String LIST_ACTION_SEND_PASSWORD = "sendpassword_";
    static final String LIST_ACTION_SETLOCALES = "set_locales_";
    static final String LIST_ACTION_DELETE0 = "delete0_";
    static final String LIST_ACTION_DELETE1 = "delete_";
    
    public void doList(WebContext ctx) {
        int n=0;
        printHeader(ctx, "List Users");
        printUserMenu(ctx);
        ctx.printHelpLink("/AddModifyUser");
        ctx.println("<a href='" + ctx.url() + "'><b>SurveyTool main</b></a><hr />");
        String org = ctx.session.user.org;
        if(UserRegistry.userCreateOtherOrgs(ctx.session.user)) {
            org = null; // all
        }
        try { synchronized(reg) {
            java.sql.ResultSet rs = reg.list(org);
            if(rs == null) {
                ctx.println("<i>No results...</i>");
                return;
            }
            if(UserRegistry.userCreateOtherOrgs(ctx.session.user)) {
                org = "ALL"; // all
            }
            ctx.println("<h2>Users for " + org + "</h2>");
            if(UserRegistry.userCanModifyUsers(ctx.session.user)) {
                ctx.println("<div class='warning' style='border: 1px solid black; padding: 1em; margin: 1em'><b><font size='+2'>NOTE: Changing user level or locales while a user is active (below) in will result in  " +
                            " destruction of their session, losing all data.</font></b></div>");
            }
            // Preset box
            boolean preFormed = false;
            if(UserRegistry.userCanModifyUsers(ctx.session.user)) {
                ctx.println("<div class='pager' style='align: right; float: right; margin-left: 4px;'>");
                ctx.println("<form method=POST action='" + ctx.base() + "'>");
                ctx.printUrlAsHiddenFields();
                ctx.println("Set menus:<br /><label>all ");
                ctx.println("<select name='preset_from'>");
                ctx.println("   <option>" + LIST_ACTION_NONE + "</option>");
                for(int i=0;i<UserRegistry.ALL_LEVELS.length;i++) {
                    ctx.println("<option class='user" + UserRegistry.ALL_LEVELS[i] + "' ");
                    ctx.println(" value='"+UserRegistry.ALL_LEVELS[i]+"'>" + UserRegistry.levelToStr(ctx, UserRegistry.ALL_LEVELS[i]) + "</option>");
                }
                ctx.println("</select></label> <br />");
                ctx.println(" <label>to");
                ctx.println("<select name='preset_do'>");
                ctx.println("   <option>" + LIST_ACTION_NONE + "</option>");
                for(int i=0;i<UserRegistry.ALL_LEVELS.length;i++) {
                    ctx.println("<option class='user" + UserRegistry.ALL_LEVELS[i] + "' ");
                    ctx.println(" value='"+LIST_ACTION_SETLEVEL+UserRegistry.ALL_LEVELS[i]+"'>" + UserRegistry.levelToStr(ctx, UserRegistry.ALL_LEVELS[i]) + "</option>");
                }
                ctx.println("   <option>" + LIST_ACTION_NONE + "</option>");
                ctx.println("   <option value='" + LIST_ACTION_DELETE0 +"'>Delete user..</option>");
                ctx.println("   <option>" + LIST_ACTION_NONE + "</option>");
                ctx.println("   <option value='" + LIST_ACTION_SHOW_PASSWORD + "'>Show password URL...</option>");
                ctx.println("   <option value='" + LIST_ACTION_SEND_PASSWORD + "'>Resend password...</option>");
                ctx.println("   <option value='" + LIST_ACTION_SETLOCALES + "'>Set locales...</option>");
                ctx.println("</select></label> <br />");
                ctx.println("<input type='submit' name='do' value='list'></form>");
                if((ctx.field("preset_from").length()>0)&&!ctx.field("preset_from").equals(LIST_ACTION_NONE)) {
                    ctx.println("<hr /><i><b>Menus have been pre-filled. <br /> Confirm your choices and click Change.</b></i>");
                    ctx.println("<form method=POST action='" + ctx.base() + "'>");
                    ctx.println("<input type='submit' name='doBtn' value='Change' />");
                    preFormed=true;
                }
                ctx.println("</div>");
            }
            int preset_fromint = ctx.fieldInt("preset_from", -1);
            String preset_do = ctx.field("preset_do");
            if(preset_do.equals(LIST_ACTION_NONE)) {
                preset_do="nothing";
            }
            if(((UserRegistry.userCanModifyUsers(ctx.session.user))) &&
               !preFormed) { // form was already started, above
                ctx.println("<form method=POST action='" + ctx.base() + "'>");
            }
            if(UserRegistry.userCanModifyUsers(ctx.session.user)) {
                ctx.printUrlAsHiddenFields();
                ctx.println("<input type='hidden' name='do' value='list' />");
                ctx.println("<input type='submit' name='doBtn' value='Change' />");
            }
            ctx.println("<table class='userlist' border='2'>");
            ctx.println(" <tr><th>Level</th><th>Name/Email</th><th>Organization</th><th>Locales</th><th>Actions</th></tr>");
            while(rs.next()) {
                n++;
                int theirId = rs.getInt(1);
                int theirLevel = rs.getInt(2);
                String theirName = rs.getString(3);
                String theirEmail = rs.getString(4);
                String theirOrg = rs.getString(5);
                String theirLocales = rs.getString(6);
                CookieSession theUser = CookieSession.retrieveUserWithoutTouch(theirEmail);
                ctx.println("  <tr class='user" + theirLevel + "'>");
                ctx.println("    <td align='right'>" + UserRegistry.levelToStr(ctx,theirLevel) + "</td>");
                ctx.println("    <td><font size='-1'>#" + theirId + " </font>" +  theirName + "<br />");
                ctx.println("    " + "<a href='mailto:" + theirEmail + "'>" + theirEmail + "</a>" + "</td>");
                ctx.println("    <td>" + theirOrg + "</td>");
                if(theirLevel <= UserRegistry.TC) {
                    ctx.println("   <td>" + UserRegistry.prettyPrintLocale(null) + "</td> ");
                } else {
                    ctx.println("    <td>" + UserRegistry.prettyPrintLocale(theirLocales) + "</td>");
                }
                
                boolean havePermToChange = UserRegistry.userCanModifyUser(ctx.session.user, theirId, theirLevel);
                
                if(havePermToChange) {
                    // Was something requested?
                    String theirTag = theirId + "_" + theirEmail; // ID+email - prevents stale data. (i.e. delete of user 3 if the rows change..)
                    String action = ctx.field(theirTag);
                    ctx.println("    <td><select name='" + theirTag + "'>");
                    // set user to VETTER
                    ctx.println("   <option>" + LIST_ACTION_NONE + "</option>");
                    for(int i=0;i<UserRegistry.ALL_LEVELS.length;i++) {
                        int lev = UserRegistry.ALL_LEVELS[i];
                        doChangeUserOption(ctx, lev, theirLevel,
                                           (preset_fromint==theirLevel)&&preset_do.equals(LIST_ACTION_SETLEVEL + lev) );
                    }
                    ctx.println("   <option>" + LIST_ACTION_NONE + "</option>");
                    
                    
                    
                    ctx.println("   <option ");
                    if((preset_fromint==theirLevel)&&preset_do.equals(LIST_ACTION_SHOW_PASSWORD)) {
                        ctx.println(" SELECTED ");
                    }
                    ctx.println(" value='" + LIST_ACTION_SHOW_PASSWORD + "'>Show password...</option>");
                    
                    
                    
                    ctx.println("   <option ");
                    if((preset_fromint==theirLevel)&&preset_do.equals(LIST_ACTION_SEND_PASSWORD)) {
                        ctx.println(" SELECTED ");
                    }
                    ctx.println(" value='" + LIST_ACTION_SEND_PASSWORD + "'>Send password...</option>");
                    
                    if(theirLevel > UserRegistry.TC) {
                        ctx.println("   <option ");
                        if((preset_fromint==theirLevel)&&preset_do.equals(LIST_ACTION_SETLOCALES)) {
                            ctx.println(" SELECTED ");
                        }
                        ctx.println(" value='" + LIST_ACTION_SETLOCALES + "'>Set locales...</option>");
                    }
                    if(UserRegistry.userCanDeleteUser(ctx.session.user,theirId,theirLevel)) {
                        ctx.println("   <option>" + LIST_ACTION_NONE + "</option>");
                        if((action!=null) && action.equals(LIST_ACTION_DELETE0)) {
                            ctx.println("   <option value='" + LIST_ACTION_DELETE1 +"' SELECTED>Confirm delete</option>");
                        } else {
                            ctx.println("   <option ");
                            if((preset_fromint==theirLevel)&&preset_do.equals(LIST_ACTION_DELETE0)) {
                                ctx.println(" SELECTED ");
                            }
                            ctx.println(" value='" + LIST_ACTION_DELETE0 +"'>Delete user..</option>");
                        }
                    }
                    ctx.println("    </select>");
                    ctx.println(      "</td>");
                    
                    
                    String msg = null;
                    if(ctx.field(LIST_ACTION_SETLOCALES + theirTag).length()>0) {
                        ctx.println("<td>");
                        String newLocales = ctx.field(LIST_ACTION_SETLOCALES + theirTag);
                        msg = reg.setLocales(ctx, theirId, theirEmail, newLocales);
                        ctx.println(msg);
                        if(theUser != null) {
                            ctx.println("<br/><i>Logging out user session " + theUser.id + " and deleting all unsaved changes</i>");
                            theUser.remove();
                        }
                        ctx.println("</td>");
                    } else if((action!=null)&&(action.length()>0)&&(!action.equals(LIST_ACTION_NONE))) {
                        ctx.println("<td>");
                        
                        // check an explicit list. Don't allow random levels to be set.
                        for(int i=0;i<UserRegistry.ALL_LEVELS.length;i++) {
                            if(action.equals(LIST_ACTION_SETLEVEL + UserRegistry.ALL_LEVELS[i])) {
                                msg = reg.setUserLevel(ctx, theirId, theirEmail, UserRegistry.ALL_LEVELS[i]);
                                ctx.println("Setting to level " + UserRegistry.levelToStr(ctx, UserRegistry.ALL_LEVELS[i]));
                                ctx.println(": " + msg);
                                if(theUser != null) {
                                    ctx.println("<br/><i>Logging out user session " + theUser.id + " and deleting all unsaved changes</i>");
                                    theUser.remove();
                                }
                            }
                        }
                        
                        if(action.equals(LIST_ACTION_SHOW_PASSWORD)) {
                            String pass = reg.getPassword(ctx, theirId);
                            if(pass != null) {
                                UserRegistry.printPasswordLink(ctx, theirEmail, pass);
                            }
                        } else if(action.equals(LIST_ACTION_SEND_PASSWORD)) {
                            String pass = reg.getPassword(ctx, theirId);
                            if(pass != null) {
                                UserRegistry.printPasswordLink(ctx, theirEmail, pass);
                                notifyUser(ctx, theirEmail, pass);                                
                            }                            
                        } else if(action.equals(LIST_ACTION_DELETE0)) {
                            ctx.println("Ensure that 'confirm delete' is chosen at left and click Change again to delete..");
                        } else if((UserRegistry.userCanDeleteUser(ctx.session.user,theirId,theirLevel)) && (action.equals(LIST_ACTION_DELETE1))) {
                            msg = reg.delete(ctx, theirId, theirEmail);
                            ctx.println("<strong style='font-color: red'>Deleting...</strong><br />");
                            ctx.println(msg);
                        } else if((UserRegistry.userCanModifyUser(ctx.session.user,theirId,theirLevel)) && (action.equals(LIST_ACTION_SETLOCALES))) {
                            if(theirLocales == null) {
                                theirLocales = "";
                            }
                            ctx.println("<label>Locales: (space separated) <input name='" + LIST_ACTION_SETLOCALES + theirTag + "' value='" + theirLocales + "'></label>"); 
                        }
                        // ctx.println("Change to " + action);
                        ctx.println("</td>");
                    }
                    
                    
                }
                
                // are they logged in?
                if((theUser != null) && UserRegistry.userCanModifyUsers(ctx.session.user)) {
                    if(!havePermToChange) {
                        ctx.println("<td></td>");
                    }
                    ctx.println("<td>");
                    ctx.println("<b>Active " + timeDiff(theUser.last) + " ago</b>");
                    if(UserRegistry.userIsAdmin(ctx.session.user)) {
                        ctx.print("<br/>");
                        printLiveUserMenu(ctx, theUser);
                    }
                    ctx.println("</td>");
                }
                
                ctx.println("  </tr>");
            }
            ctx.println("</table>");
            ctx.println("<div style='font-size: 70%'>Number of users shown: " + n +"</div><br />");
            if(UserRegistry.userCanModifyUsers(ctx.session.user)) {
                ctx.println("<input type='submit' name='doBtn' value='Change'>");
                ctx.println("</form>");
            }
            // #level $name $email $org
            rs.close();
        }/*end synchronized(reg)*/ } catch(SQLException se) {
            logger.log(Level.WARNING,"Query for org " + org + " failed: " + unchainSqlException(se),se);
            ctx.println("<i>Failure: " + unchainSqlException(se) + "</i><br />");
        }
        printFooter(ctx);
    }
    
    private void doChangeUserOption(WebContext ctx, int newLevel, int theirLevel, boolean selected)
    {
        if(UserRegistry.userCanChangeLevel(ctx.session.user, theirLevel, newLevel)) {
            ctx.println("    <option " + (selected?" SELECTED ":"") + "value='" + LIST_ACTION_SETLEVEL + newLevel + "'>Make " +
                        UserRegistry.levelToStr(ctx,newLevel) + "</option>");
        }
    }
    
    public void doSession(WebContext ctx)
    {
        // which 
        String which = ctx.field("x");
        
        setLocale(ctx);
        
        String sessionMessage = setSession(ctx);
        
        // admin things
        if((ctx.session.user != null) && (ctx.field("do").length()>0)) {
            String doWhat = ctx.field("do");
            if(doWhat.equals("list")  && (UserRegistry.userCanDoList(ctx.session.user))  ) {
                doList(ctx);
            } else if(doWhat.equals("new") && (UserRegistry.userCanCreateUsers(ctx.session.user)) ) {
                doNew(ctx);
            } else {
                printHeader(ctx,doWhat + "?");
                ctx.println("<i>some error, try hitting the Back button.</i>");
                ctx.println("<br /> User level : " + ctx.session.user.userlevel);
                printFooter(ctx);
            }
            return;
        }
        
        String title = " - " + which;
        if(ctx.field("submit").length()<=0) {
            printHeader(ctx, title);
        } else {
            if(ctx.field("submit").equals("preview")) {
                printHeader(ctx, "Review changes");
            } else {
                printHeader(ctx, "Submitted changes");
            }
        }
        if(sessionMessage != null) {
            ctx.println(sessionMessage);
        }
        // Not doing vetting admin --------
        
        WebContext baseContext = new WebContext(ctx);
        if((ctx.locale != null) /* && (ctx.field("submit").length()<=0)*/ ) {
            // unless we are submitting - process any pending form data.
            processChanges(ctx, which);
        }
        
        // print 'shopping cart'
        {
            if(ctx.session.user != null) {
                if(ctx.field("submit").length()==0) {            
                    ctx.println("<form method=POST action='" + ctx.base() + "'>");
                }
                printUserMenu(ctx);
            } else {
                ctx.println("You are a <b>Visitor</b>. <a href='" + ctx.jspLink("login.jsp") +"'>Login</a>");
                ctx.println("<br />");
            }
            ctx.println(" &nbsp; <span style='font-size:large;'>");
            ctx.printHelpLink("","Instructions"); // base help
            ctx.println("</span>");
            if(ctx.field("submit").length()==0) {
                Hashtable lh = ctx.session.getLocales();
                Enumeration e = lh.keys();
                if((ctx.locale != null) && UserRegistry.userCanModifyLocale(ctx.session.user,ctx.locale.toString())) { // at least street level
                    if(ctx.field("submit").length()==0) {
                        ctx.println("<div>");
                        ctx.println("<input type=submit value='" + xSAVE + "'>");
                        ctx.println("</div>");
                    }
                }
                if(e.hasMoreElements()) { 
                    ctx.println("<B>Visited locales: </B> ");
                    for(;e.hasMoreElements();) {
                        String k = e.nextElement().toString();
                        boolean canModify = UserRegistry.userCanModifyLocale(ctx.session.user,k);
                        ctx.print("<a href=\"" + baseContext.url() + ctx.urlConnector() +"_=" + k + "\">" + 
                                    new ULocale(k).getDisplayName());
                        if(canModify) {
                            ctx.print(MODIFY_THING);
                        }
                        ctx.println("</a> ");
                    }
                    
                /*   if(  UserRegistry.userCanSubmit(ctx.session.user) ) {
                        ctx.println("<div>");
                        ctx.println("Your changes are <b><font color=red>not</font></b> permanently saved until you: ");
                        ctx.println("<input name=submit type=submit value='" + xREVIEW +"'>");
                        ctx.println("</div>");
                    }*/
                }
            } else {
                ctx.println("<a href='" + ctx.url() + "'><b>List of Locales</b></a>");
            }
            ctx.println("<hr/>");
        }
        
        
        if(ctx.field("submit").length()>0) {
            doSubmit(ctx);
        } else {
            doLocale(ctx, baseContext, which);
        }
    }
    
     //TODO: object
    /**
    * TreeMap of all locales. 
     *
     * localeListMap =  TreeMap
     *     [  (String  langScriptDisplayName)  ,    (String localecode) ]  
     *  subLocales = Hashtable
     *       [ localecode, TreeMap ]
     *         -->   TreeMap [ langScriptDisplayName,   String localeCode ]
     *  example
     *  
     *   localeListMap
     *     English  -> en
     *     Serbian  -> sr
     *     Serbian (Cyrillic) -> sr_Cyrl
     *    sublocales
     *       en ->  
     *           [  "English (US)" -> en_US ],   [ "English (Australia)" -> en_AU ] ...
     *      sr ->
     *           "Serbian (Yugoslavia)" -> sr_YU
     */
    
    TreeMap localeListMap = null;
    Hashtable subLocales = null;
    
    private void addLocaleToListMap(String localeName)
    {
        ULocale u = new ULocale(localeName);
        
        String l = u.getLanguage();
        if((l!=null)&&(l.length()==0)) {
            l = null;
        }
        String s = u.getScript();
        if((s!=null)&&(s.length()==0)) {
            s = null;
        }
        String t = u.getCountry();
        if((t!=null)&&(t.length()==0)) {
            t = null;
        }
        String v = u.getVariant();
        if((v!=null)&&(v.length()==0)) {
            v = null;
        }
        
        if(l==null) {
            return; // no language?? 
        }
        
        String ls = ((s==null)?l:(l+"_"+s)); // language and script
        
        ULocale lsl = new ULocale(ls);
        localeListMap.put(lsl.getDisplayName(),ls);
        
        TreeMap lm = (TreeMap)subLocales.get(ls);
        if(lm == null) {
            lm = new TreeMap();
            subLocales.put(ls, lm); 
        }
        
        if(t != null) {
            if(v == null) {
                lm.put(u.getDisplayCountry(), localeName);
            } else {
                lm.put(u.getDisplayCountry() + " (" + u.getDisplayVariant() + ")", localeName);
            }
        }
    }
    
    private synchronized TreeMap getLocaleListMap()
    {
        if(localeListMap == null) {
            localeListMap = new TreeMap();
            subLocales = new Hashtable();
            File inFiles[] = getInFiles();
            if(inFiles == null) {
                busted("Can't load CLDR data files from " + fileBase);
                throw new RuntimeException("Can't load CLDR data files from " + fileBase);
            }
            int nrInFiles = inFiles.length;
            
            for(int i=0;i<nrInFiles;i++) {
                String localeName = inFiles[i].getName();
                int dot = localeName.indexOf('.');
                if(dot !=  -1) {
                    localeName = localeName.substring(0,dot);
                    if(i != 0) {
                        addLocaleToListMap(localeName);
                    }
                }
            }
        }
        return localeListMap;
    }
    
    void printLocaleLink(WebContext ctx, String localeName, String n) {
        if(n == null) {
            n = new ULocale(localeName).getDisplayName() ;
        }
        String connector = ctx.urlConnector();
        boolean hasDraft = draftSet.contains(localeName);
        ctx.print(hasDraft?"<b>":"") ;
        ctx.print("<a " + (hasDraft?"class='draftl'":"class='nodraft'") 
                  +" title='" + localeName + "' href=\"" + ctx.url() 
                  + connector + "_=" + localeName + "\">" +
                  n);
        boolean canModify = UserRegistry.userCanModifyLocale(ctx.session.user,localeName);
        if(canModify) {
            ctx.print(MODIFY_THING);
        }
        ctx.print("</a>");
        ctx.print(hasDraft?"</b>":"") ;
    }
    
    void doLocaleList(WebContext ctx, WebContext baseContext) {
        boolean showCodes = ctx.prefBool(PREF_SHOWCODES);
        
        {
            WebContext nuCtx = new WebContext(ctx);
            nuCtx.addQuery(PREF_SHOWCODES, !showCodes);
            nuCtx.println("<div class='pager' style='float: right;'>");
            nuCtx.println("<a href='" + nuCtx.url() + "'>" + ((!showCodes)?"Show":"Hide") + " locale codes</a>");
            nuCtx.println("</div>");
        }
        ctx.println("<h1>Locales</h1>");
        ctx.println("<i>Note: The first time a locale is loaded takes up to 20 seconds, please be patient.</i>");
        TreeMap lm = getLocaleListMap();
        if(lm == null) {
            busted("Can't load CLDR data files from " + fileBase);
            throw new RuntimeException("Can't load CLDR data files from " + fileBase);
        }
        ctx.println("<table border=1 class='list'>");
        int n=0;
        for(Iterator li = lm.keySet().iterator();li.hasNext();) {
            n++;
            String ln = (String)li.next();
            String aLocale = (String)lm.get(ln);
            ctx.print("<tr class='row" + (n%2) + "'>");
            ctx.print(" <td>");
            printLocaleLink(baseContext, aLocale, ln);
            ctx.println(" </td>");
            if(showCodes) {
                ctx.print(" <td>");
                ctx.println("<tt>" + aLocale + "</tt>");
                ctx.println(" </td>");
            }
            
            TreeMap sm = (TreeMap)subLocales.get(aLocale);
            
            ctx.println("<td>");
            int j = 0;
            for(Iterator si = sm.keySet().iterator();si.hasNext();) {
                if(j>0) { 
                    ctx.println(", ");
                }
                String sn = (String)si.next();
                String subLocale = (String)sm.get(sn);
                if(subLocale.length()>0) {
                    printLocaleLink(baseContext, subLocale, sn);
                    if(showCodes) {
                        ctx.println("&nbsp;-&nbsp;<tt>" + subLocale + "</tt>");
                    }
                }
                j++;
            }
            ctx.println("</td");
            ctx.println("</tr>");
        }
        ctx.println("</table> ");
        ctx.println("(Locales containing draft items are shown in <b><a class='draftl' href='#'>bold</a></b>)<br/>");
    }
    
    void doLocale(WebContext ctx, WebContext baseContext, String which) {
        String locale = null;
        if(ctx.locale != null) {
            locale = ctx.locale.toString();
        }
        if((locale==null)||(locale.length()<=0)) {
            doLocaleList(ctx, baseContext);            
            ctx.println("<br/>");
        } else {
            showLocale(ctx, which);
        }
        printFooter(ctx);
    }
    
    protected void printMenu(WebContext ctx, String which, String menu) {
        if(menu.equals(which)) {
            ctx.print("<b class='selected'>");
        } else {
            ctx.print("<a class='notselected' href=\"" + ctx.url() + ctx.urlConnector() + "x=" + menu +
                      "\">");
        }
        if(menu.endsWith("/")) {
            ctx.print(menu + "<font size=-1>(other)</font>");
        } else {
            ctx.print(menu);
        }
        if(menu.equals(which)) {
            ctx.print("</b>");
        } else {
            ctx.print("</a>");
        }
    }
    
    void notifyUser(WebContext ctx, String theirEmail, String pass) {
        String requester = ctx.session.user.name + " <" + ctx.session.user.email + ">";
        String body = requester +  " is notifying you of the CLDR vetting account for you.\n" +
        "To access it, visit: \n" +
        "   http://" + ctx.serverName() + ctx.base() + "?"+QUERY_PASSWORD+"=" + pass + "&"+QUERY_EMAIL+"=" + theirEmail + "\n" +
        "\n" +
        " Please keep this link to yourself. Thanks.\n" +
        " \n";
        
        String from = survprops.getProperty("CLDR_FROM","nobody@example.com");
        String smtp = survprops.getProperty("CLDR_SMTP",null);
        if(smtp == null) {
            ctx.println("<i>Not sending mail- SMTP disabled.</i><br/>");
            ctx.println("<hr/><pre>" + body + "</pre><hr/>");
            smtp = "NONE";
        } else {
            MailSender.sendMail(smtp, from, theirEmail, "CLDR Registration for " + theirEmail,
                                body);
            ctx.println("Mail sent to " + theirEmail + " from " + from + " via " + smtp + "<br/>\n");
        }
        logger.info( "Login URL sent to " + theirEmail + "  from " + from + " via " + smtp);
        /* some debugging. */
    }
    
    public void doSubmit(WebContext ctx)
    {
        if((ctx.session.user == null) ||
           (ctx.session.user.email == null)) {  
            ctx.println("Not logged in... please see this help link: ");
            ctx.printHelpLink("/NoUser");
            ctx.println("<p>");
            ctx.println("<i>Note: if you used the 'back' button on your browser, please instead use " + 
                        " the URL link that was emailed to you. </i>");
            printFooter(ctx);
            return;
        }
        UserRegistry.User u = ctx.session.user;
        if(u == null) {
            u = reg.getEmptyUser();
        }
        WebContext subContext = new WebContext(ctx);
        subContext.addQuery("submit","post");
        boolean post = (ctx.field("submit").equals("post"));
        if(post == false) {
            ctx.println("<p class='hang'><B>Please read the following carefully. If there are any errors, hit Back and correct them.  " + 
                        "Your submission will NOT be recorded unless you click the 'Submit'  button on this page!</b></p>");
            {
                subContext.println("<form method=POST action='" + subContext.base() + "'>");
                subContext.printUrlAsHiddenFields();
                subContext.println("<input type=submit value='Submit'>");
                subContext.println("</form>");
            }
        } else {
            ctx.println("<h2>Submitted the following changes:</h2><br/>");
            {
                ctx.println("<form method=GET action='" + ctx.base() + "'>");
                ctx.println("<input type=hidden name=uid value='" + ctx.session.user.id + "'> " + // NULL PTR
                            "<input type=hidden name=email value='" + ctx.session.user.email + "'>");
                ctx.println("<input type=submit value='Login Again'>");
                ctx.println("</form>");
            }
        }
        ctx.println("<hr/>");
        ctx.println("<div class=pager>");
        ctx.println("You:  " + u.name + " &lt;" + u.email + "&gt;<br/>");
        ctx.println("Your sponsor: " + u.org);
        ctx.println("</div>");
        File sessDir = new File(vetweb + "/" + u.email + "/" + ctx.session.id);
        if(post) {
            sessDir.mkdirs();
        }
        String changedList = "";
        Hashtable lh = ctx.session.getLocales();
        Enumeration e = lh.keys();
        String fullBody = "";
        if(e.hasMoreElements()) { 
            for(;e.hasMoreElements();) {
                String k = e.nextElement().toString();
                String displayName = new ULocale(k).getDisplayName();
                ctx.println("<hr/>");
                ctx.println("<H3>" + 
                            displayName+ "</h3>");
                if(!post) {
                    ctx.println("<a class='pager' style='float: right;' href='" + ctx.url() + ctx.urlConnector() + "_=" + k + "&x=" + xREMOVE + "'>[Cancel This Edit]</a>");
                }
                CLDRFile f = createCLDRFile(ctx, k, (Hashtable)lh.get(k));
                
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                f.write(pw);
                String asString = sw.toString();
                fullBody = fullBody + "-------------" + "\n" + k + ".xml - " + displayName + "\n" + 
                    hexXML.transliterate(asString);
                String asHtml = TransliteratorUtilities.toHTML.transliterate(asString);
                ctx.println("<pre>" + asHtml + "</pre>");
                File xmlFile = new File(sessDir, k + ".xml");
                if(post) {
                    try {
                        changedList = changedList + " " + k;
                        PrintWriter pw2 = BagFormatter.openUTF8Writer(sessDir + File.separator, k+".xml");
                        f.write(pw2);
                        pw2.close();
                        ctx.println("<b>File Written.</b><br/>");
                    } catch(Throwable t) {
                        // TODO: log??
                        ctx.println("<b>Couldn't write the file "+ k + ".xml</b> because: <br/>");
                        ctx.println(t.toString());
                        t.printStackTrace();
                        ctx.println("<p>");
                    }
                }
            }
        }
        ctx.println("<hr/>");
        if(post == false) {
            subContext.println("<form method=POST action='" + subContext.base() + "'>");
            subContext.printUrlAsHiddenFields();
            subContext.println("<input type=submit value='Submit'>");
            subContext.println("</form>");
        } else {        
            String body = "User:  " + u.name + " <" + u.email + "> for  " + u.org + "\n" +
            "Submitted data for: " + changedList + "\n" +
            "Session ID: " + ctx.session.id + "\n";
            String smtp = survprops.getProperty("CLDR_SMTP","127.0.0.1");
            if(smtp == null) {
                ctx.println("<i>Not sending mail- SMTP disabled.</i><br/>");
            } else {
                MailSender.sendMail(u.email, "CLDR: Receipt of your data submission ",
                                    "Submission from IP: " + ctx.userIP() + "\n" + body  +
                                    "\n The files submitted are attached below: \n" + fullBody );
                MailSender.sendMail(survprops.getProperty("CLDR_NOTIFY"), "CLDR: from " + u.org + 
                                    "/" + u.email + ": " + changedList,
                                    "URL: " + survprops.getProperty("CLDR_VET_WEB_URL","file:///dev/null") + u.email + "/" + ctx.session.id + "\n" +
                                    body);
                ctx.println("Thank you..   An email has been sent to the CLDR Vetting List and to you at " + u.email + ".<br/>");
            }
            logger.info( "Data submitted: " + u.name + " <" + u.email + "> Sponsor: " + u.org + ": " +
                         changedList + " " + 
                         " (user ID " + u.id + ", session " + ctx.session.id + " )" );
            // destroy session
            {
                ctx.println("<form method=GET action='" + ctx.base() + "'>");
                ctx.println("<input type=hidden name=uid value='" + ctx.session.user.id + "'> " + // NULL PTR
                            "<input type=hidden name=email value='" + ctx.session.user.email + "'>");
                ctx.println("<input type=submit value='Login Again'>");
                ctx.println("</form>");
            }
            ctx.session.remove();
        }
        printFooter(ctx);
    }
    
    /**
      * Append codes to a CLDRFile 
     **/
    private void appendCodeList(WebContext ctx, CLDRFile file, String xpath, String subtype, Hashtable data) {
        if(data == null) {
            return;
        }
        for(Enumeration e = data.keys();e.hasMoreElements();) {
            String k = e.nextElement().toString();
            if(k.endsWith(SUBVETTING)) { // we ONLY care about SUBVETTING items. (
                String type = k.substring(0,k.length()-SUBVETTING.length());
                String vet = (String)data.get(k);
                // Now, what's happening? 
                NodeSet.NodeSetEntry nse = (NodeSet.NodeSetEntry)data.get(type);
                String newxpath;
                if(xpath != null) {
                    newxpath = xpath + subtype + "[@type='" + type + "']";
                    if(nse.key != null) {
                        newxpath = newxpath + "[@key='" + nse.key + "']";
                    }
                } else {
                    newxpath = nse.xpath; 
                    if(type == null) {
                        type = xpath + "/"; // FIXME: don't set the xpath this way!
                    }
                }
                newxpath=newxpath.substring(1); // remove initial /     
                if(vet.equals(DRAFT)) {
                    if((nse.main != null) && nse.mainDraft) {
                        file.add(newxpath, LDMLUtilities.getNodeValue(nse.main));
                    } else {
                        file.addComment(newxpath, "Can't find draft data! " + type, XPathParts.Comments.POSTBLOCK);
                    }
                } else if(vet.equals(CURRENT)) {
                    if(nse.fallback != null)  {
                        file.add(newxpath, LDMLUtilities.getNodeValue(nse.fallback));
                    } else if((nse.main != null)&&!LDMLUtilities.isNodeDraft(nse.main)) {
                        file.add(newxpath, LDMLUtilities.getNodeValue(nse.main));
                    } else {
                        file.add(newxpath, type);
                    }
                } else if(vet.startsWith(PROPOSED)) {
                    String whichProposed = vet.substring(PROPOSED.length());
                    newxpath=newxpath+"[@alt='" + whichProposed + "']"; // append alt if user selected.
                    file.add(newxpath, LDMLUtilities.getNodeValue((Node)nse.alts.get(whichProposed)));
                } else if(vet.equals(NEW)) {
                    String newString = (String)data.get(type + SUBNEW); //type could be xpath here
                    if(newString == null) {
                        newString = "";
                    }
                    if(nse.main != null) { // If there is already an existing main (which might be draft)
                        newxpath = newxpath + "[@alt='proposed-new']";
                    }
                    newxpath = newxpath + "[@draft='true']"; // always draft
                                                             ///*srl*/                 ctx.println("<tt>CLDRFile.add(<b>" + newxpath + "</b>, \"\", blah);</tt><br/>");
                    file.add(newxpath, newString);
                    if(newString.length() ==0) {
                        file.addComment(newxpath, "Item marked as wrong:  " + type, XPathParts.Comments.LINE);
                        ///*srl*/                 ctx.println("<tt>CLDRFile.addComment(<b>" + newxpath + "</b>, blah, LINE);</tt><br/>");
                    }
                } else {
                    // ignored:  current, etc.
                }
}
}
}
    
    /**
        * Convert from the parent to a child type.  i.e. 'languages' -> 'language'
     */
    public static final String typeToSubtype(String type)
    {
        String subtype = type;
        if(type.equals(LDMLConstants.LANGUAGES)) {
            subtype = LDMLConstants.LANGUAGE;
        } else if(type.equals(LDMLConstants.SCRIPTS)) {
            subtype = LDMLConstants.SCRIPT;
        } else if(type.equals(LDMLConstants.TERRITORIES)) {
            subtype = LDMLConstants.TERRITORY;
        } else if(type.equals(LDMLConstants.VARIANTS)) {
            subtype = LDMLConstants.VARIANT;
        } else if(type.equals(LDMLConstants.KEYS)) {
            subtype = LDMLConstants.KEY;
        } else if(type.equals(LDMLConstants.TYPES)) {
            subtype = LDMLConstants.TYPE;
        } /* else if(subtype.endsWith("s")) {
subtype = subtype.substring(0,subtype.length()-1);
        }
        */
        return subtype;
    }
    
    /**
        * Append the codes for a certain hashtable into the CLDRFile 
     */
    private void appendCodes(WebContext ctx, CLDRFile file, String xpath, String type, Hashtable data) {
        String fullXpath = xpath + type;
        Hashtable items = (Hashtable)data.get(fullXpath);
        String subtype = typeToSubtype(type);
        appendCodeList(ctx, file, xpath, subtype, items);
    }
    
    private void appendOtherCodes(WebContext ctx, CLDRFile file, Hashtable data) {
        Hashtable items = (Hashtable)data.get(xOTHER);
        appendCodeList(ctx, file, null, null, items);
    }
    
    private CLDRFile createCLDRFile(WebContext ctx, String locale, Hashtable data) {
        CLDRFile file = CLDRFile.make(locale);
        String cvsVer = (String)docVersions.get(locale);
        if(cvsVer == null) {
            cvsVer = "(unknown)";
        }
        file.setInitialComment(
                               "Date: " + new Date().toString() + "\n" +
                               "From: " + ctx.session.user.name + "\n" +
                               "Email: " + ctx.session.user.email + "\n" +
                               "Sponsor: " + ctx.session.user.org + "\n" +
                               /*    "IP: " + WebContext.userIP() + "\n" + */
                               "Locale: " + locale +"\n" +
                               "CVS Version: " + cvsVer + "\n"
                               );
        
        if(data == null) {
            file.appendFinalComment("No data.");
            return file;
        }
        
        for(int n =0 ; n < LOCALEDISPLAYNAMES_ITEMS.length; n++) {        
            appendCodes(ctx, file, LOCALEDISPLAYNAMES, LOCALEDISPLAYNAMES_ITEMS[n], data);
        }
        appendOtherCodes(ctx, file, data);
        return file;
    }
    
    /**
        * process (convert user field -> hashtable entries) any form items needed.
     * @param ctx context
     * @param which value of 'x' parameter.
     */
    public void processChanges(WebContext ctx, String which)
    {
        if(false) {
            NodeSet ns = getNodeSet(ctx, which);
            // locale display names
            for(int n =0 ; n < LOCALEDISPLAYNAMES_ITEMS.length; n++) {
                if(which.equals(LOCALEDISPLAYNAMES_ITEMS[n])) {
                    processCodeListChanges(ctx, LOCALEDISPLAYNAMES +LOCALEDISPLAYNAMES_ITEMS[n], ns);
                    return;
                }
            }
            
            processOther(ctx, which, ns);
        } else {
            //            logger.severe("NOTE:  NOT doing processChanges()."); // TODO: 0 fix to use sql
        }
    }
    
    /**
        * Parse query fields, update hashes, etc.
     * later, we'll see if we can generalize this function.
     */
    public void processCodeListChanges(WebContext ctx, String xpath, NodeSet mySet) {
        Hashtable changes = (Hashtable)ctx.getByLocale(xpath);
        // prepare a new hashtable
        if(changes==null) { 
            changes = new Hashtable(); 
        }
        // process items..
        for(Iterator e = mySet.iterator();e.hasNext();) {
            NodeSet.NodeSetEntry f = (NodeSet.NodeSetEntry)e.next();
            processUserInput(ctx, changes, xpath, f.type, f); 
        }
        if((changes!=null) && (!changes.isEmpty())) { 
            ctx.putByLocale(xpath,changes); 
        }
    }
    
    /**
        * process one of the 'other' (non localedisplay items)
     */
    void processOther(WebContext ctx, String which, NodeSet mySet) {
        Hashtable changes = (Hashtable)ctx.getByLocale(xOTHER);
        // prepare a new hashtable
        if(changes==null) { 
            changes = new Hashtable(); 
        }
        // process items..
        for(Iterator e = mySet.iterator();e.hasNext();) {
            NodeSet.NodeSetEntry f = (NodeSet.NodeSetEntry)e.next();
            processUserInput(ctx, changes, f.xpath, null, f);
        }
        if((changes!=null) && (!changes.isEmpty())) { 
            ctx.putByLocale(xOTHER,changes); 
        }
    }
    
    /**
        * process user input (form field) for a single data item
     * @param f opaque object to be inserted
     */
    void processUserInput(WebContext ctx, Hashtable changes, String xpath, String type, Object f) {
        // Analyze user input.
        String checked = null;  // What option has the user checked?
        String newString = null;  // What option has the user checked?
        String fieldBase = fieldsToHtml(xpath,type);
        String hashBase;
        if(type == null) {
            type = "";
            hashBase = xpath;
        } else {
            hashBase = type;
        }
        if(changes != null) {
            checked = (String)changes.get(hashBase + SUBVETTING); // fetch VETTING data
            newString = (String)changes.get(hashBase + SUBNEW); // fetch NEW data
        }
        if(checked == null) {
            checked = NOCHANGE;
        }
        
        if(fieldBase == null) {
            ctx.print("<h1>SEVERE ERROR: f2h gave null in " + xpath + "|" + type + "</h1>");
            return;
        }
        
        String formChecked = ctx.field(fieldBase);
        
        if((formChecked != null) && (formChecked.length()>0)) {   
            // Don't consider the 'new text' form, unless we know the 'changes...' checkbox is present.
            // this is because we can't distinguish between an empty and a missing field.
            String formNew = ctx.field(fieldBase + SUBNEW );
            if((formNew.length()>0) && !formNew.equals(UNKNOWNCHANGE)) {
                changes.put(hashBase + SUBNEW, formNew);
                changes.put(hashBase, f); // get the NodeSet in for later use
                newString = formNew;
                if(formChecked.equals(NOCHANGE)) {
                    formChecked = NEW;
                    changes.put(xOTHER + "/" + NEW, fieldBase);
                }
            } else if((newString !=null) && (newString.length()>0)) {
                changes.remove(hashBase + SUBNEW);
                newString = null;
            }
            
            if(!checked.equals(formChecked)) {
                checked = formChecked;
                if(checked.equals(NOCHANGE)) {
                    changes.remove(hashBase + SUBVETTING); // remove 'current' 
                } else {
                    /////*srl*/            ctx.println("<tt>Form: " + fieldBase + " - " + formChecked + " - " + xpath + "</tt><br/>");
                    changes.put(hashBase + SUBVETTING, checked); // set
                    changes.put(hashBase, f);
                }
            }
            
        }
    }
    
    public static final String CHECKCLDR = "_CheckCLDR";  // key for CheckCLDR objects by locale
    public static final String CHECKCLDR_RES = "_CheckCLDR_RES";  // key for CheckCLDR objects by locale
    
    /**
        * show the actual locale data..
     * @param ctx context
     * @param which value of 'x' parameter.
     */
    public void showLocale(WebContext ctx, String which)
    {
        int i;
        int j;
        int n = ctx.docLocale.length;
        if(which.equals(xREMOVE)) {
            ctx.println("<b><a href=\"" + ctx.url() + "\">" + "List of Locales" + "</a></b><br/>");
            ctx.session.getLocales().remove(ctx.field("_"));
            ctx.println("<h2>Your session for " + ctx.field("_") + " has been removed.</h2>");
            doMain(ctx);
            return;
        }
        
        CLDRFile cf = getUserFile(ctx, (ctx.session.user==null)?null:ctx.session.user, ctx.locale);
        if(cf == null) {
            throw new InternalError("CLDRFile is null!");
        }
        CLDRDBSource ourSrc = (CLDRDBSource)ctx.getByLocale(USER_FILE + CLDRDBSRC); // TODO: remove. debuggin'
        if(ourSrc == null) {
            throw new InternalError("oursrc is null! - " + (USER_FILE + CLDRDBSRC) + " @ " + ctx.locale );
        }
        synchronized (ourSrc) {
            // Set up checks
            CheckCLDR checkCldr =  (CheckCLDR)ctx.getByLocale(USER_FILE + CHECKCLDR);
            List checkCldrResult = new ArrayList();
            if (checkCldr == null)  {
                logger.info("Initting tests . . .");
                long t0 = System.currentTimeMillis();
                checkCldr = CheckCLDR.getCheckAll(/* "(?!.*Collision.*).*" */  ".*");
                ctx.putByLocale(USER_FILE + CHECKCLDR, checkCldr);
                {
                    CheckCLDR subCheckCldr = (CheckCLDR)ctx.getByLocale(SurveyMain.USER_FILE + SurveyMain.CHECKCLDR);
                    if(subCheckCldr == null) {
                        throw new InternalError("subCheckCldr == null");
                    }
                }
                
                checkCldr.setDisplayInformation(getEnglishFile());
                checkCldr.setCldrFileToCheck(cf, null, checkCldrResult); // TODO: when does this get updated?
                if(!checkCldrResult.isEmpty()) {
                    ctx.putByLocale(USER_FILE + CHECKCLDR_RES, checkCldrResult); // don't bother if empty . . .
                }
                long t2 = System.currentTimeMillis();
                logger.info("Time to init tests: " + (t2-t0));
            }
            
            // Locale menu
            ctx.println("<table width='95%' border=0><tr><td width='25%'>");
            ctx.println("<b><a href=\"" + ctx.url() + "\">" + "Locales" + "</a></b><br/>");
            for(i=(n-1);i>0;i--) {
                for(j=0;j<(n-i);j++) {
                    ctx.print("&nbsp;&nbsp;");
                }
                boolean canModify = UserRegistry.userCanModifyLocale(ctx.session.user,ctx.docLocale[i]);
                ctx.println("\u2517&nbsp;<a href=\"" + ctx.url() + ctx.urlConnector() +"_=" + ctx.docLocale[i] + 
                    "\">" + ctx.docLocale[i] + "</a> " + new ULocale(ctx.docLocale[i]).getDisplayName() );
                if(canModify) {
                    ctx.print(MODIFY_THING);
                }
                ctx.println("<br/>");
            }
            for(j=0;j<n;j++) {
                ctx.print("&nbsp;&nbsp;");
            }
            boolean canModifyL = UserRegistry.userCanModifyLocale(ctx.session.user,ctx.locale.toString());
            ctx.print("\u2517&nbsp;<font size=+2><b>" + ctx.locale + "</b>");
            if(canModifyL) {
                ctx.print(MODIFY_THING);
            }
            ctx.println("</font> " + 
                ctx.locale.getDisplayName() + "<br/>");
            ctx.println("</td><td>");
            
            if((which == null) ||
               which.equals("")) {
                ////which = RAW_MENU_ITEM;
                //which = LOCALEDISPLAYNAMES_ITEMS[0]; // was xMAIN
                which = xMAIN;
            }
            
            
            WebContext subCtx = new WebContext(ctx);
            subCtx.addQuery("_",ctx.locale.toString());
            printMenu(subCtx, which, xMAIN);
            subCtx.println("<p class='hang'> Locale Display: ");
            for(n =0 ; n < LOCALEDISPLAYNAMES_ITEMS.length; n++) {        
                //if(n>0) ctx.print(", ");
                printMenu(subCtx, which, LOCALEDISPLAYNAMES_ITEMS[n]);
            }
            subCtx.println("</p> <p class='hang'>Other Items: ");
            
            for(n =0 ; n < OTHERROOTS_ITEMS.length; n++) {        
                if(n>0) ctx.print(" ");
                printMenu(subCtx, which, OTHERROOTS_ITEMS[n]);
            }
            ctx.print(" ");
            printMenu(subCtx, which, xOTHER);
            printMenu(subCtx, which, TEST_MENU_ITEM);
            printMenu(subCtx, which, RAW_MENU_ITEM);
            subCtx.println("</td></tr></table>");
            
            if( (!checkCldrResult.isEmpty()) && 
                (/* true || */ (checkCldr != null) && (xMAIN.equals(which))) ) {
                ctx.println("<hr /><h4>Possible problems with locale:</h4>");
                ctx.println("<pre style='border: 1px dashed olive; padding: 1em; background-color: cream; overflow: auto;'>");
                for (Iterator it3 = checkCldrResult.iterator(); it3.hasNext();) {
                    CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();
                    if (!status.getType().equals(status.exampleType)) {
                        ctx.println(status.toString() + "<br />");
                    } else {
                        ctx.println("<i>example available</i><br />");
                    }
                }
                ctx.println("</pre><hr />");
            }
            subCtx.addQuery("x",which);
            for(n =0 ; n < LOCALEDISPLAYNAMES_ITEMS.length; n++) {        
                if(LOCALEDISPLAYNAMES_ITEMS[n].equals(which)) {
                    showLocaleCodeList(subCtx, which);
                    return;
                }
            }
            
            // handle from getNodeSet for these . . .
            for(j=0;j<OTHERROOTS_ITEMS.length;j++) {
                if(OTHERROOTS_ITEMS[j].equals(which)) {
                    if(which.equals("timeZoneNames")) {
                        showZoneList(subCtx);
                    } else {
                        showPathList(subCtx, "//ldml/"+OTHERROOTS_ITEMS[j], null);
                    }
                    return;
                }
            }
            // fall through if wasn't one of the other roots
            if(xOTHER.equals(which)) {
                //                doOtherList(subCtx, which);
            } else if(RAW_MENU_ITEM.equals(which)) {
                doRaw(subCtx);
      /*      } else if((checkCldr != null) && (TEST_MENU_ITEM.equals(which))) { // print the results
                CLDRFile file = cf;
                ctx.println("<pre style='border: 1px dashed olive; padding: 1em; background-color: cream; overflow: auto;'>");
                //            Set locales = ourSrc.getAvailable();
                XPathParts pathParts = new XPathParts(null, null);
                XPathParts fullPathParts = new XPathParts(null, null);
                //            for (Iterator it = locales.iterator(); it.hasNext();) {
                String localeID = ctx.locale.toString();
                ctx.println(checkCldr.getLocaleAndName(localeID));
                //                CLDRFile file = ourSrc.factory.make(localeID, false);
                ctx.println(" <h4>Test Results</h4>");
                for (Iterator it2 = file.iterator(); it2.hasNext();) {
                    String path = (String) it2.next();
                    //System.out.println("P: " + path);
                    
                    String value = file.getStringValue(path);
                    String fullPath = file.getFullXPath(path);
                    checkCldr.check(path, fullPath, value, pathParts, fullPathParts, checkCldrResult);
                    for (Iterator it3 = checkCldrResult.iterator(); it3.hasNext();) {
                        CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();
                        if (!status.getType().equals(status.exampleType)) {
                            ctx.println(status.toString() + "\t" + value + "\t" + fullPath);
                        } //else {
                          //ctx.println("<i>example available</i><br />");
                          //}
                    }            
                }
                
                System.out.println(" done with tests ");
                
                
                //            }
                ctx.println("</pre>");
        */
            } else  {
                doMain(subCtx);
            }
        }
        // ?
//        ctx.removeByLocale(USER_FILE + CHECKCLDR);
//        ctx.removeByLocale(USER_FILE + CHECKCLDR_RES);
}

public void doRaw(WebContext ctx) {
    CLDRFile file = (CLDRFile)ctx.getByLocale(USER_FILE);
    //        ctx.println("<h3>list output of the locale's CLDRFile</h3>");
    //        ctx.println("<pre style='border: 2px solid olive; margin: 1em;'>");
    
    //        ctx.println("</pre>");
    
    ctx.println("<h3>Raw output of the locale's CLDRFile</h3>");
    ctx.println("<pre style='border: 2px solid olive; margin: 1em;'>");
    //        ((CLDRFile)ctx.getByLocale(USER_FILE)).show(ctx.out);
    
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    file.write(pw);
    String asString = sw.toString();
    //                fullBody = fullBody + "-------------" + "\n" + k + ".xml - " + displayName + "\n" + 
    //                    hexXML.transliterate(asString);
    String asHtml = TransliteratorUtilities.toHTML.transliterate(asString);
    ctx.println(asHtml);
    ctx.println("</pre>");
}

/**
* Show the 'main info about this locale' panel.
 */
public void doMain(WebContext ctx) {
    String ver = LDMLUtilities.getCVSVersion(fileBase, ctx.locale.toString() + ".xml"); // just get ver of the latest file.
    ctx.println("<hr/><p><p>");
    ctx.println("<h3>Basic information about the Locale</h3>");
    
    if(ver != null) {
        ctx.println( LDMLUtilities.getCVSLink(ctx.locale.toString(), ver) + "CVS version #" + ver + "</a><br/>");
    }    
}

public static String getAttributeValue(Document doc, String xpath, String attribute) {
    if(doc != null) {
        Node n = LDMLUtilities.getNode(doc, xpath);
        if(n != null) {
            return LDMLUtilities.getAttributeValue(n, attribute);
        }
    }
    return null;
}

/**
* Get a texter for a specific data type
 */
private static final NodeSet.NodeSetTexter getLanguagesTexter(ULocale l) {
    final ULocale inLocale = l;
    return new NodeSet.NodeSetTexter() { 
        public String text(NodeSet.NodeSetEntry e) {
            return new ULocale(e.type).getDisplayLanguage(inLocale);
        }
    };
}

/**
* Get a texter for a specific data type
 */
private static final NodeSet.NodeSetTexter getScriptsTexter(ULocale l) {
    final ULocale inLocale = l;
    return new NodeSet.NodeSetTexter() { 
        public String text(NodeSet.NodeSetEntry e) {
            return new ULocale("_"+e.type).getDisplayScript(inLocale);
        }
    };
}

/**
* Get a texter for a specific data type
 */
private static final NodeSet.NodeSetTexter getTerritoriesTexter(ULocale l) {
    final ULocale inLocale = l;
    return new NodeSet.NodeSetTexter() { 
        public String text(NodeSet.NodeSetEntry e) {
            return new ULocale("_"+e.type).getDisplayCountry(inLocale);
        }
    };
}

private static final NodeSet.NodeSetTexter getXpathTexter(NodeSet.NodeSetTexter tx) {
    final NodeSet.NodeSetTexter txl = tx;
    if(tx == null)  {
        return new NodeSet.NodeSetTexter() { 
            public String text(NodeSet.NodeSetEntry e) {
                return e.xpath;
            }
        };
    } else {
        return new NodeSet.NodeSetTexter() { 
            public String text(NodeSet.NodeSetEntry e) {
                return txl.text(e) + "|" + e.xpath;
            }
        };
    }
}

/**
* Get a texter for a specific data type
 */
private static final NodeSet.NodeSetTexter getVariantsTexter(ULocale l) {
    final ULocale inLocale = l;
    return new NodeSet.NodeSetTexter() { 
        public String text(NodeSet.NodeSetEntry e) {
            return new ULocale("__"+e.type).getDisplayVariant(inLocale);
        }
    };
}

/**
* Get the appropriate texter for the type
 */
NodeSet.NodeSetTexter getTexter(WebContext ctx, String which) {
    if(LDMLConstants.LANGUAGES.equals(which)) {
        return new StandardCodeTexter(which);
    } else if(LDMLConstants.SCRIPTS.equals(which)) {
        return new StandardCodeTexter(which);
    } else if(LDMLConstants.TERRITORIES.equals(which)) {        
        return new StandardCodeTexter(which);
    } else if(LDMLConstants.VARIANTS.equals(which)) {
        return getVariantsTexter(inLocale);  // no default variant list
    } else if(LDMLConstants.KEYS.equals(which)) {
        return new StandardCodeTexter(which);  // no default  list
    } else if(LDMLConstants.TYPES.equals(which)) {
        return new StandardCodeTexter(which);  // no default  list
    } else {
        return null;
    }
}

/** 
* Fetch the NodeSet  [ set of resolved items ] from the cache if possible
*/
private SoftReference nodeHashReference = null;
int nodeHashPuts = 0;
private final Hashtable getNodeHash() {
    Hashtable nodeHash = null;
    if((nodeHashReference == null) ||
       ((nodeHash=(Hashtable)nodeHashReference.get())==null)) {
        return null;
    }
    return nodeHash;
}

int nodeHashCreates = 0;

public static final String USER_FILE = "UserFile";
public static final String CLDRDBSRC = "_source";

private static CLDRFile gEnglishFile = null;

private CLDRFile.Factory gFactory = null;

private synchronized CLDRFile.Factory getFactory() {
    if(gFactory == null) {
        gFactory = CLDRFile.Factory.make(fileBase,".*");
    }
    return gFactory;
}

public synchronized CLDRFile getEnglishFile(/*CLDRDBSource ourSrc*/) {
    if(gEnglishFile == null) {
        gEnglishFile = getFactory().make("en", false);
        gEnglishFile.freeze(); // so it can be shared
    }
    return gEnglishFile;
}

public synchronized String getDirectionFor(ULocale locale) {
    // Hackness:
    String locStr = locale.toString();
    if(locStr.startsWith("ps") ||
       locStr.startsWith("fa") ||
       locStr.startsWith("ar") ||
       locStr.startsWith("syr") ||
       locStr.startsWith("he") ||
       locStr.startsWith("uz_Arab")) {
        return "rtl";
    } else {
        return "ltr";
    }
}

CLDRFile getUserFile(WebContext ctx, UserRegistry.User user, ULocale locale) {
    CLDRFile file = (CLDRFile)ctx.getByLocale(USER_FILE);
    if(file == null) {
        CLDRDBSource dbSource = makeDBSource(user, locale);
        file = makeCLDRFile(dbSource);
        
        ctx.putByLocale(USER_FILE,file); 
        String srcKey = USER_FILE + CLDRDBSRC;
        ctx.putByLocale(srcKey,dbSource);  // TODO: remove. for debugging.
    }
    return file;
}
CLDRDBSource makeDBSource(UserRegistry.User user, ULocale locale) {
    CLDRDBSource dbSource = CLDRDBSource.createInstance(fileBase, xpt, locale,
                                                        getDBConnection(), user);
    return dbSource;
}
static CLDRFile makeCLDRFile(CLDRDBSource dbSource) {
    return new CLDRFile(dbSource,false);
}

NodeSet getNodeSet(WebContext ctx, String which) {
    NodeSet ns = null;
    
    {
        Hashtable nodeHash = getNodeHash();
        if(nodeHash != null) {
            ns = (NodeSet)nodeHash.get(ctx.locale.toString() + "/" + which);
        } 
    }
    
    if(ns != null) {
        return ns;
    }
    
    StandardCodes standardCodes = StandardCodes.make();
    Set defaultSet = standardCodes.getAvailableCodes(typeToSubtype(which));
    
    // handle the 'locale display names' cases
    for(int n =0 ; n < LOCALEDISPLAYNAMES_ITEMS.length; n++) {    
        if(LOCALEDISPLAYNAMES_ITEMS[n].equals(which)) {
            ns = getNodeSet(ctx, LOCALEDISPLAYNAMES + LOCALEDISPLAYNAMES_ITEMS[n], 
                            null /* no texter */ , defaultSet);  // no default  list
        }
    }
    if(ns == null) {
        ns = getNodeSetOther(ctx, which);            
    }
    if(ns != null) {
        Hashtable nodeHash = getNodeHash();
        if(nodeHash == null) {
            // create the nodehash
            nodeHashReference = new SoftReference(nodeHash = new Hashtable());   
            ++nodeHashCreates;
            //             double avgPuts = ((double)nodeHashPuts)/((double)nodeHashCreates);
            // /*srl*/            System.err.println("Created nodeHash [#" + nodeHashCreates + "] - avg p/h " + avgPuts + "\n");
        }
        ++nodeHashPuts;
        nodeHash.put(ctx.locale.toString() + "/" + which, ns);
    }
    return ns;
}

/**
* Get the node set for an xpath
 */
NodeSet getNodeSet(WebContext ctx, String xpath, 
                   NodeSet.NodeSetTexter texter, Set defaultSet) {
    ///*srl*/        ctx.println("<tt>load: " + xpath + ", " + defaultSet.size() + "</tt><br/>");
    return NodeSet.loadFromPath(ctx, xpath, defaultSet);
}

/**
* Get the node set for an 'other' type. 
 */
NodeSet getNodeSetOther(WebContext ctx, String which) {
    // handle 'other'
    final String myPrefix = "//ldml/" + which;
    NodeSet.XpathFilter myFilter = null;
    if(which.equals(xOTHER)) {
        myFilter = new NodeSet.XpathFilter() {
            public boolean okay(String path) {
                if(path.startsWith(LOCALEDISPLAYNAMES)) {
                    return false;
                }
                for(int i=0;i<OTHERROOTS_ITEMS.length;i++) {
                    if(path.startsWith("//ldml/"+OTHERROOTS_ITEMS[i])) {
                        return false;
                    }
                }
                return true;
            }
        };
    } else {
        if(myPrefix.endsWith("/")) { // filter out all other prefixes..
            myFilter = new NodeSet.XpathFilter() {
                public boolean okay(String path) {
                    if(!path.startsWith(myPrefix)) {
                        return false;
                    }
                    for(int i=0;i<OTHERROOTS_ITEMS.length;i++) {
                        String aPath = "//ldml/"+OTHERROOTS_ITEMS[i];
                        if(!myPrefix.equals(aPath)) {
                            if(path.startsWith(aPath)) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
            };
        } else {
            myFilter = new NodeSet.XpathFilter() {
                public boolean okay(String path) {
                    return path.startsWith(myPrefix); 
                }
            };
        }
    }
    NodeSet ns = NodeSet.loadFromXpaths(ctx, allXpaths, myFilter);
    if(which.equals(NUMBERSCURRENCIES)) { // not using defaultSet because the default expansion is more complex..
        StandardCodes standardCodes = StandardCodes.make();
        Set s = standardCodes.getAvailableCodes("currency");
        for(Iterator e = s.iterator();e.hasNext();) {
            String f = (String)e.next();
            ns.addXpath(ctx,"//ldml/" + which + "/currency[@type='" + f + "']/displayName", f);
                ns.addXpath(ctx,"//ldml/" + which + "/currency[@type='" + f + "']/symbol", f);
        }
    } else if(which.equals("dates/timeZoneNames")) { // not using defaultSet because the default expansion is more complex..
        StandardCodes standardCodes = StandardCodes.make();
        Set s = standardCodes.getAvailableCodes("tzid");
        for(Iterator e = s.iterator();e.hasNext();) {
            String f = (String)e.next();
            ns.addXpath(ctx,"//ldml/" + which + "/zone[@type='" + f + "']/exemplarCity", f);
        }
    }
    return ns;
}


/**
* show the webpage for one of the 'locale codes' items.. 
 * @param ctx the web context
 * @param xpath xpath to the root of the structure
 * @param tx the texter to use for presentation of the items
 * @param fullSet the set of tags denoting the expected full-set, or null if none.
 */
public void showLocaleCodeList(WebContext ctx, String which) {
    showPathList(ctx, LOCALEDISPLAYNAMES+which, typeToSubtype(which));
}

final int CODES_PER_PAGE = 80;  // was 51


/**
* @ really deprecated
 */
public void showZoneList(WebContext ctx) {
    int count = 0;
    int dispCount = 0;
    int total = 0;
    int skip = 0;
    
    final String TZ_MOST = "//ldml/dates/timeZoneNames/zone/";
        final String TZ_LENGTHS[] = { "short", "long" };
        final String TZ_TYPES[] = { "generic", "standard", "daylight" };
        final String TZ_EXEMPLAR = "exemplarCity";
        
        boolean isTz = true;
        //       total = mySet.count();
        String sortMode = ctx.pref(PREF_SORTMODE);
        boolean sortAlpha = (sortMode.equals(PREF_SORTMODE_ALPHA));
        CLDRFile cf = getUserFile(ctx, ctx.session.user, ctx.locale);
        CLDRDBSource ourSrc = (CLDRDBSource)ctx.getByLocale(USER_FILE + CLDRDBSRC); // TODO: remove. debuggin'
        CheckCLDR checkCldr = (CheckCLDR)ctx.getByLocale(USER_FILE + CHECKCLDR);
        XPathParts pathParts = new XPathParts(null, null);
        XPathParts fullPathParts = new XPathParts(null, null);
        List checkCldrResult = new ArrayList();
        
        Iterator theIterator = null;
        
        synchronized(ourSrc) { // because it has a connection..
            StandardCodes standardCodes = StandardCodes.make();
            Set defaultSet = standardCodes.getAvailableCodes("tzid");
            
            theIterator = defaultSet.iterator();
            // NAVIGATION .. calculate skips.. 
            skip = showSkipBox(ctx, defaultSet.size());
            //            if(changes.get(xOTHER + "/" + NEW)!=null) {
            //                changes.remove(xOTHER + "/" + NEW);
            //                ctx.println("<div class='missing'><b>Warning: Remember to click the 'change' radio button after typing in a change.  Please check the status of change boxes below.</b></div><br/>");
            //            }
            
            
            //       ctx.println(" Printing dump for " + fullThing + ", #" + xpt.getByXpath(fullThing) + "<br />");
            
            
            // Form: 
            ctx.printUrlAsHiddenFields();
            ctx.println("<table class='list' border=1>");
            ctx.println("<tr>");
            ctx.println(" <th class='heading' bgcolor='#DDDDDD' colspan=2>Name<br/><div style='border: 1px solid gray; width: 6em;' align=left><tt>Code</tt></span></th>");
            ctx.println(" <th class='heading' bgcolor='#DDDDDD' colspan=1>Best<br/>");
            ctx.printHelpLink("/Best");
            ctx.println("</th>");
            ctx.println(" <th class='heading' bgcolor='#DDDDDD' colspan=1>Contents</th>");
            ctx.println("</tr>");
            
            
            for(Iterator e = theIterator;e.hasNext();) {
                String type = (String)e.next();
                
                count++;
                if(skip > 0) {
                    --skip;
                    continue;
                }
                dispCount++;
                
                String locale = ctx.locale.toString();
                boolean first = true;
                //                int isExemplar
                for(int whichType = 0;whichType < TZ_TYPES.length;whichType++) {
                    for(int whichLength = 0;whichLength < TZ_LENGTHS.length;whichLength++) {
                        String theLocale = locale;
                        String fullThing = TZ_MOST + TZ_LENGTHS[whichLength] + "/" + TZ_TYPES[whichType];
                        boolean gotNonDraft = false;
                        
                        do {
                            java.sql.ResultSet rs = ourSrc.listForType(xpt.getByXpath(fullThing), type, theLocale);
                            
                            try {
                                while(rs.next()) {
                                    if(first) {
                                        ctx.println("<tr><th colspan='3' valign='left'>" + type + "</th></tr>");
                                        first = false;
                                    }
                                    int submitId = rs.getInt(4);
                                    if(rs.wasNull()) submitId = -1; //  null submitter
                                    
                                    String at = rs.getString(2);
                                    String ap = rs.getString(3);
                                    String val = rs.getString(1);
                                    String subXpath = xpt.getById(rs.getInt(5));
                                    String fullPath = xpt.getById(rs.getInt(6));
                                    
                                    if((theLocale==locale)||((at==null)&&(ap==null))) {
                                        String rowclass="current";
                                        if((at!=null)&&(at.equals("proposed"))) {
                                            rowclass = "proposed";
                                        } else if(theLocale != locale) {
                                            rowclass = "fallback";
                                        }
                                        ctx.println("<tr class='" + rowclass + "'><td>" +
                                                    "<font size='-2'>"+TZ_LENGTHS[whichLength] + "/" + TZ_TYPES[whichType] + "</font><br/>" +
                                                    val + "</td>" +
                                                    "<td>"+ ((at!=null)?at:"") + "<br />" + 
                                                    ((ap!=null)?ap:"") + "</td>" + 
                                                    "");
                                        if(theLocale != locale) {
                                            ctx.println("<td><b>" + theLocale + "</b></td>");
                                        }
                                        
                                        ctx.println("<td>");
                                        pathParts.clear();
                                        fullPathParts.clear();
                                        checkCldrResult.clear();
//                                        checkCldr.check(subXpath, fullPath, val, pathParts, fullPathParts, checkCldrResult);
                                        for (Iterator it3 = checkCldrResult.iterator(); it3.hasNext();) {
                                            CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();
                                            if (!status.getType().equals(status.exampleType)) {
                                                ctx.println(status.toString() + "\t" + val + "\t" + fullPath);
                                            } else {
                                                ctx.println("<i>example available</i><br />");
                                            }
                                        }                                            
                                        ctx.println("</td>");
                                        
                                        
                                        ctx.println("</tr>");
                                        
                                        
                                        if((at==null)&&(at==null)) {
                                            gotNonDraft=true;
                                        }
                                    }
                                }
                                rs.close();
                            } catch(SQLException se) {
                                ctx.println("err in showPathList 0: " + unchainSqlException(se));
                            }
                            theLocale = WebContext.getParent(theLocale);
                        } while((theLocale != null)&&(gotNonDraft==false));
                    }
                } /* end type and len */
                if(dispCount >= CODES_PER_PAGE) {
                    break;
                }
                
            } // end loop
        } // end synch
        
        ctx.println("</table>");
        // skip =
        if(ctx.session.user != null) {
            ctx.println("<div style='margin-top: 1em;' align=right><input type=submit value='" + xSAVE + " for " + 
                        ctx.locale.getDisplayName() +"'></div>");
        }
        ctx.println("</form>");
        
}

/**
 NHTML
 
<tr class='heading'>
    <th colspan=2>
    </th>
    <th >action</th>
    <th >data</th>
    <th >alerts</th>
</tr>

<tr class='topbar'>
    <th colspan='1' align='left'  >
        <tt class='codebox'>af</tt>
                <!-- class='codebox' -->
    </th>
    <th colspan='4' align='left' class='botgray' >
        Afrikaans
    </th>
</tr>
<tr>
    <th colspan='2'>
    </th>
    <td colspan="1" align="right">
        <input type='radio'>
    </td>
    <td dir='ltr'>
         Afrikaans
    </td>
    <td  class='warncell'>
        I don't like it.
    </td>
</tr>

<tr class='proposed' >
    <td colspan=3 align="right">			
                <span class='dashbox' >proposed-srl</span>
        <input type="radio">
    </td>
    <td dir="ltr">Farik-aniz</td>
    <td></td>
</tr>

<tr class='warnrow'>
    <td colspan='3' align="right" >
        <span class='dashbox' >inherited</span>
        <input type="radio"
    </td>
    <td dir='ltr'>
        <tt>af</tt>
    </td>
    <td class='warncell'>
            Inherited code.
    </td>
</tr>
<tr>
    <td>Don't care<input type='radio' checked></td>
    <td bgcolor='gray'></td>
    <td>change<input type='radio'></td>
    <td colspan='2'><input size=20'></td>
</tr>


**/

/**
* This is the main function for showing lists of items (pods).
 */
public void showPathList(WebContext ctx, String xpath, String lastElement) {
    CLDRDBSource ourSrc = (CLDRDBSource)ctx.getByLocale(USER_FILE + CLDRDBSRC); // TODO: remove. debuggin'
    CLDRFile cf = getUserFile(ctx, ctx.session.user, ctx.locale);
    String fullThing = xpath + "/" + lastElement;
    boolean isTz = xpath.equals("timeZoneNames");
    if(lastElement == null) {
        fullThing = xpath;
    }
    
    boolean canModify = (UserRegistry.userCanModifyLocale(ctx.session.user,ctx.locale.toString()));
    
    synchronized(ourSrc) { // because it has a connection..
                           // Podder
        // first, do submissions.
        if(canModify) {
            DataPod oldPod = ctx.getExistingPod(fullThing);
            processPeaChanges(ctx, oldPod, cf, ourSrc);
        }
//        System.out.println("Pod's full thing: " + fullThing);
        DataPod pod = ctx.getPod(fullThing);
        
        showPeas(ctx, pod, canModify);
    }
}

void showPeas(WebContext ctx, DataPod pod, boolean canModify) {
    int count = 0;
    int dispCount = 0;
    int total = 0;
    int skip = 0;
    
    //       total = mySet.count();
    //        boolean sortAlpha = (sortMode.equals(PREF_SORTMODE_ALPHA));
    CLDRFile cf = getUserFile(ctx, ctx.session.user, ctx.locale);
    //    CLDRFile engf = getEnglishFile();
    CLDRDBSource ourSrc = (CLDRDBSource)ctx.getByLocale(USER_FILE + CLDRDBSRC); // TODO: remove. debuggin'
    CheckCLDR checkCldr = (CheckCLDR)ctx.getByLocale(USER_FILE + CHECKCLDR);
    XPathParts pathParts = new XPathParts(null, null);
    XPathParts fullPathParts = new XPathParts(null, null);
    List checkCldrResult = new ArrayList();
    Iterator theIterator = null;
    String sortMode = ctx.pref(PREF_SORTMODE, PREF_SORTMODE_DEFAULT);
    List peas = pod.getList(sortMode);
    String ourDir = getDirectionFor(ctx.locale);
    ctx.println("<hr />");
    
    DataPod.DisplaySet dSet = pod.getDisplaySet(sortMode);  // contains 'peas' and display list
    boolean checkPartitions = dSet.partitions.length > 1; // only check if more than one partition    
    // -----
    skip = showSkipBox(ctx, peas.size(), pod.getDisplaySet(sortMode));
    
    ctx.printUrlAsHiddenFields();   
    ctx.println("<input type='hidden' name='skip' value='"+ctx.field("skip")+"' />");
    ctx.println("<table class='list' border='0'>");
    
    ctx.println("<tr class='heading'>\n"+
                " <th colspan=2>\n"+
                " </th>\n"+
                "<th> action</th>\n"+
                "<th> data</th>\n"+
                "<th> alerts</th>\n"+
                "</tr>");
    
    for(ListIterator i = peas.listIterator(skip);(count<CODES_PER_PAGE)&&i.hasNext();count++) {
        DataPod.Pea p = (DataPod.Pea)i.next();
        
        if(checkPartitions) {
            for(int j = 0;j<dSet.partitions.length;j++) {
                if((dSet.partitions[j].name != null) && (count+skip) == dSet.partitions[j].start) {
                    ctx.println("<tr style='font-size: 200%' class='heading'><th colspan='2'><i>" +
                        "<a name='" + dSet.partitions[j].name + "'>" +
                        dSet.partitions[j].name + "</a>" +
                        "</i></th></tr>");
                }
            }
        }
        
        showPea(ctx, pod, p, ourDir, cf, ourSrc, canModify);
        if(p.subPeas != null) {
            for(Iterator e = p.subPeas.values().iterator();e.hasNext();) {
                DataPod.Pea subPea = (DataPod.Pea)e.next();
                showPea(ctx, pod, subPea, ourDir, cf, ourSrc, canModify);
            }
        }
    }
    
    ctx.println("</table>");
    /*skip = */ showSkipBox(ctx, peas.size(), pod.getDisplaySet(sortMode), false);
    if(!canModify) {
        ctx.println("<hr> <i>You are not authorized to make changes to this locale.</i>");
    }
}

void processPeaChanges(WebContext ctx, DataPod oldPod, CLDRFile cf, CLDRDBSource ourSrc) {
    String ourDir = getDirectionFor(ctx.locale);
    if(oldPod != null) {
        for(Iterator i = oldPod.getAll().iterator();i.hasNext();) {
            DataPod.Pea p = (DataPod.Pea)i.next();
            processPeaChanges(ctx, oldPod, p, ourDir, cf, ourSrc);
            if(p.subPeas != null) {
                for(Iterator e = p.subPeas.values().iterator();e.hasNext();) {
                    DataPod.Pea subPea = (DataPod.Pea)e.next();
                    processPeaChanges(ctx, oldPod, subPea, ourDir, cf, ourSrc);
                }
            }
        }            
    }
}

void processPeaChanges(WebContext ctx, DataPod pod, DataPod.Pea p, String ourDir, CLDRFile cf, CLDRDBSource ourSrc) {
    String fieldHash = pod.fieldHash(p);
    // do modification here. 
    String choice = ctx.field(fieldHash); // checkmark choice
    String choice_v = ctx.field(fieldHash+"_v"); // choice + value
    
    // TODO: load.. 
    if(choice.length()==0) {
        return; // nothing to see..
    }
    
    if(choice.equals(CHANGETO) &&
        choice_v.length()>0) {
        String fullPathFull = pod.xpath(p);
        String fullPathMinusAlt = XPathTable.removeAlt(fullPathFull);
        for(Iterator j = p.items.iterator();j.hasNext();) {
            DataPod.Pea.Item item = (DataPod.Pea.Item)j.next();
            if(choice_v.equals(item.value)  && 
                !((item.altProposed==null) && (p.inheritFrom!=null) &&  XMLSource.CODE_FALLBACK_ID.equals(p.inheritFrom))) { // OK to override code fallbacks
                ctx.println("<tt class='codebox'>" + p.displayName + "</tt>  - Not accepting value, as it is already present under " + 
                    ((item.altProposed==null)?" non-draft item ":(" <tt>"+item.altProposed+"</tt> ")));
                return;
            }
        }
        String newProp = ourSrc.addDataToNextSlot(cf, pod.locale, fullPathMinusAlt, p.altType, 
            XPathTable.altProposedPrefix(ctx.session.user.id), ctx.session.user.id, choice_v);
        ctx.println("<tt class='codebox'>" + p.displayName + "</tt> <b>change: " + choice_v + " : " + newProp + "<br />");
        lcr.invalidateLocale(pod.locale); // throw out this pod next time.
    } else if(!choice.equals(DONTCARE)) {
        ctx.println("<tt class='codebox'>" + p.displayName + "</tt> Note: <i>" + choice + "</i> not supported yet. <br />");
    }
}

// TODO: trim unused params
void showPea(WebContext ctx, DataPod pod, DataPod.Pea p, String ourDir, CLDRFile cf, 
    CLDRDBSource ourSrc, boolean canModify) {
    //            ctx.println("<tr><th colspan='3' align='left'><tt>" + p.type + "</tt></th></tr>");

    String fieldHash = pod.fieldHash(p);
    /*  TOP BAR */
    ctx.println("<tr class='topbar'>");
    ctx.println("<th nowrap colspan='1' valign='top' align='left'>");
    if(p.displayName != null) {
        ctx.println("<tt class='codebox'>"
                    + p.type + 
                    "</tt>");
    }
    if(p.altType != null) {
        ctx.println(" ("+p.altType+")");
    }
    ctx.println("</th>");
    ctx.println("<th nowrap colspan='4' valign='top' align='left' class='botgray'>");
    if(p.displayName != null) {
        ctx.println(p.displayName);
    } else {
        ctx.println("<tt>"+p.type+"</tt>");

    }
///*srl*/        String fullPathFull = pod.xpath(p); ctx.println("<br />"+fullPathFull);
    ctx.println("</th> </tr>");
    
    
    if((p.hasInherited == true) && (p.type != null) && (p.inheritFrom == null)) { // by code
        String pClass = "class='warnrow'";
        ctx.print("<tr " + pClass + "><td nowrap colspan='3' valign='top' align='right'>");
        ctx.print("<span class='actionbox'>missing</span>");
        if(canModify) {
            ctx.print("<input name='"+fieldHash+"' value='"+"0"+"' type='radio' />");
        } else {
            ctx.print("<input type='radio' disabled>");
        }
        ctx.print("<tt>");
        ctx.println("</td>");
        ctx.println("<td dir='" + ourDir +"'>");
        ctx.println(p.type + "</tt></td>");
        ctx.println("<td class='warncell'>Missing, using code.</td>");
        ctx.println("</tr>");
    }
    for(Iterator j = p.items.iterator();j.hasNext();) {
        DataPod.Pea.Item item = (DataPod.Pea.Item)j.next();
        String pClass ="";
        if((item.inheritFrom != null)&&(p.inheritFrom==null)) {
            pClass = "class='fallback'";
        } else if(item.altProposed != null) {
            pClass = "class='proposed'";
        } else if(p.inheritFrom != null) {
            pClass = "class='missing'";
        } 
        ctx.print("<tr><td colspan='2'></td><td nowrap "+pClass+" colspan='1' valign='top' align='right'>");
        if(item.altProposed != null) {
            int uid = XPathTable.altProposedToUserid(item.altProposed);
            UserRegistry.User theU = null;
            if(uid != -1) {
                theU = reg.getInfo(uid);
            }
            if((theU!=null)&& 
                    ((uid==ctx.session.user.id) ||   //if it's us or..
                    (UserRegistry.userIsTC(ctx.session.user) ||  //or  TC..
                    (UserRegistry.userIsVetter(ctx.session.user) && (canModify ||  // approved vetter or ..
                                                    ctx.session.user.org.equals(theU.org)))))) { // vetter&same org
                if((ctx.session.user==null)||(ctx.session.user.org == null)) {
                    throw new InternalError("null: c.s.u.o");
                }
                if((theU!=null)&&(theU.org == null)) {
                    throw new InternalError("null: theU.o");
                }
                boolean sameOrg = (ctx.session.user.org.equals(theU.org));
                if(sameOrg) {
                    ctx.print("<b>");
                }
                ctx.print("<span class='actionbox'>" + item.altProposed + "<br />");
                ctx.print("<a href='mailto:"+theU.email+"'>" + theU.name + "</a> (" + theU.org + ")");
                ctx.print("</span>");
                if(sameOrg) {
                    ctx.print("</b>");
                }
            } else {
                ctx.print("<span class='actionbox'>" + item.altProposed + "</span>");
            }
            if(item.inheritFrom != null) {
                ctx.println("<br /><span class='fallback'>Inherited from: " + item.inheritFrom+"</span>");
            }
        } else {
            if(p.inheritFrom != null) {
                ctx.print("<span class='actionbox'>inherited " + p.inheritFrom + "</span>");
            } else {
                ctx.print("<span class='actionbox'>" + CONFIRM + "</span>");
            }
        }
        if(canModify) {
            ctx.print("<input name='"+fieldHash+"'  value='"+((item.altProposed!=null)?item.altProposed:"0")+"' type='radio' />");
        } else {
            ctx.print("<input type='radio' disabled />");
        }
        ctx.println("</td>");
        ctx.println("<td valign='top' nowrap dir='" + ourDir +"'>");
        ctx.println(item.value + "</td>");
        if(item.tests != null) {
            ctx.println("<td class='warncell'>");
            for (Iterator it3 = item.tests.iterator(); it3.hasNext();) {
                CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();
                if (!status.getType().equals(status.exampleType)) {
                    
                    ctx.println("<span class='warning'>");
                    printShortened(ctx,status.toString());
                    ctx.println("</span><br />");
                } else {
                    ctx.println("<pre>"+status.toString()+"</pre><br />");
                }
            }                                                                            
            ctx.println("</td>");
        }
        ctx.println("</tr>");
    }
    if(true) {
        String pClass = "";
         // dont care
        ctx.print("<tr " + pClass + "><td nowrap colspan>");
        if(canModify) {
            ctx.print("<span class='actionbox'>" + DONTCARE + "</span>");
            ctx.print("<input name='"+fieldHash+"' value='"+DONTCARE+"' type='radio' CHECKED />");
        }
        ctx.println("</td>");
        ctx.println("<td bgcolor='gray'></td>");
        // change
        ctx.print("<td nowrap valign='top' align='right'>");
        if(canModify && !p.confirmOnly) {
            ctx.print("<span class='actionbox'>" + CHANGETO + "</span>");
            ctx.print("<input name='"+fieldHash+"' value='"+CHANGETO+"' type='radio'  />");
        } else {
            ctx.print("<span class='disabledbox'>" + CHANGETO + "</span>");
            ctx.print("<input type='radio' disabled />");
        }
        ctx.println("</td>");
        if(canModify) {
            ctx.println("<td colspan='2'><input name='"+fieldHash+"_v'  class='inputbox'></td>");
        }
        ctx.println("</tr>");
    }
}

void showSkipBox_menu(WebContext ctx, String sortMode, String aMode, String aDesc) {
    WebContext nuCtx = new WebContext(ctx);
    nuCtx.addQuery(PREF_SORTMODE, aMode);
    if(!sortMode.equals(aMode)) {
        nuCtx.print("<a class='notselected' href='" + nuCtx.url() + "'>");
    } else {
        nuCtx.print("<span class='selected'>");
    }
    nuCtx.print(aDesc);
    if(!sortMode.equals(aMode)) {
        nuCtx.println("</a>");
    } else {
        nuCtx.println("</span>");
    }
    
    nuCtx.println(" ");
}
// TODO: remove this. 
int showSkipBox(WebContext ctx, int total, Map m, NodeSet.NodeSetTexter tx) {
    return showSkipBox(ctx, total);
}
int showSkipBox(WebContext ctx, int total) {
    return showSkipBox(ctx, total, null);
}
int showSkipBox(WebContext ctx, int total, DataPod.DisplaySet displaySet) {
    return showSkipBox(ctx, total, displaySet, true);
}
int showSkipBox(WebContext ctx, int total, DataPod.DisplaySet displaySet, boolean showSearchMode) {
    List displayList = null;
    if(displaySet != null) {
        displayList = displaySet.displayPeas;
    }
    int skip;
    ctx.println("<div class='pager' style='margin: 2px'>");
    if(showSearchMode) {
        ctx.println(/*"<p style='float: right; margin-left: 3em;'> " + */
            "<b>Sorted:</b>  ");
        {
            String sortMode = ctx.pref(PREF_SORTMODE, PREF_SORTMODE_DEFAULT);
            boolean sortAlpha = (sortMode.equals(PREF_SORTMODE_ALPHA));
            
            //          showSkipBox_menu(ctx, sortMode, PREF_SORTMODE_ALPHA, "Alphabetically");
            showSkipBox_menu(ctx, sortMode, PREF_SORTMODE_CODE, "Code");
            showSkipBox_menu(ctx, sortMode, PREF_SORTMODE_WARNING, "Priority");
            showSkipBox_menu(ctx, sortMode, PREF_SORTMODE_NAME, "Name");
        }
    }

    // TODO: replace with ctx.fieldValue("skip",-1)
    String str = ctx.field("skip");
    if((str!=null)&&(str.length()>0)) {
        skip = new Integer(str).intValue();
    } else {
        skip = 0;
    }
    if(skip<=0) {
        skip = 0;
    } 

    // calculate nextSkip\
    int from = skip+1;
    int to = from + CODES_PER_PAGE-1;
    if(to >= total) {
        to = total;
    }

    // Print navigation
    if(showSearchMode) {
        ctx.println("Displaying items " + from + " to " + to + " of " + total);        

        if(total>=(CODES_PER_PAGE)) {
            ctx.println("<br/>");
        }
    }

    if(total>=(CODES_PER_PAGE)) {
        if(displaySet.partitions.length > 1) {
            ctx.println("<table border='0'><tr><td>");
        }
        if(skip>0) {
            int prevSkip = skip - CODES_PER_PAGE;
            if(prevSkip<0) {
                prevSkip = 0;
            }
            ctx.println("<a href=\"" + ctx.url() + 
                        ctx.urlConnector() + "skip=" + new Integer(prevSkip) + "\">" +
                        "\u2190 prev " + CODES_PER_PAGE + "");
            ctx.println("</a> &nbsp;");
            if(skip>=total) {
                skip = 0;
            }
        } else if(total>=(CODES_PER_PAGE)) {
            ctx.println("<span>\u2190 prev " + CODES_PER_PAGE + "" +
                        "</span> &nbsp;");        
        }
        if(displaySet.partitions.length > 1) {
            ctx.println("</td>");
        }
        for(int j=0;j<displaySet.partitions.length;j++) {
            if(j>0) { 
                ctx.println("<tr><td></td>");
            }
            if(displaySet.partitions[j].name != null) {
                ctx.print("<td align='right'><b>" + displaySet.partitions[j].name + ":</b></td><td>");
            }
            int ourStart = displaySet.partitions[j].start;
            int ourLimit = displaySet.partitions[j].limit;
            for(int i=ourStart;i<ourLimit;i = (i - (i%CODES_PER_PAGE)) + CODES_PER_PAGE) {
                int pageStart = i - (i%CODES_PER_PAGE); // pageStart is the skip at the top of this page. should be == ourStart unless on a boundary.
                int end = pageStart + CODES_PER_PAGE-1;
                if(end>=ourLimit) {
                    end = ourLimit-1;
                }
                boolean isus = (pageStart == skip);
                if(isus) {
                    if(((i!=pageStart) || (i==0)) && (displaySet.partitions[j].name != null)) {
                        ctx.print("<b><a class='selected' style='text-decoration:none' href='#"+displaySet.partitions[j].name+"'>");
                    } else {
                        ctx.println(" <b class='selected'>");
                    }
                } else {
                    ctx.print(" <a class='notselected' href=\"" + ctx.url() + 
                                ctx.urlConnector() +"skip=" + pageStart);
                    if((i!=pageStart) && (displaySet.partitions[j].name != null)) {
                        ctx.println("#"+displaySet.partitions[j].name);
                    }
                    ctx.println("\">"); // skip to the pageStart
                }
                if(displayList != null) {
                    String iString = displayList.get(i).toString();
                    String endString = displayList.get(end).toString();
                    
                    if(iString.length() > 20) {
                        iString = iString.substring(0,20);
                        endString="";
                    }
                    if(endString.length()>20) {
                        endString = endString.substring(0,20);
                    }
                    
                    ctx.print(iString+"\u2026"+endString);
                } else {
                    ctx.print( ""+(i+1) );
                    ctx.print( "-" + (end+1));
                }
                if(isus) {
                    if(((i!=pageStart) || (i==0)) && (displaySet.partitions[j].name != null)) {
                        ctx.print("</a></b> ");
                    } else {
                        ctx.println("</b> ");
                    }
                } else {
                    ctx.println("</a> ");
                }
            }
            // Next>> stuff
            if(j==0) {
                if(displaySet.partitions.length > 1) {
                    ctx.println("</td><td>");
                }
                int nextSkip = skip + CODES_PER_PAGE; 
                if(nextSkip >= total) {
                    nextSkip = -1;
                    if(total>=(CODES_PER_PAGE)) {
                        ctx.println(" <span >" +
                                    "next " + CODES_PER_PAGE + "\u2192" +
                                    "</span>");
                    }
                } else {
                    ctx.println(" <a href=\"" + ctx.url() + 
                                ctx.urlConnector() +"skip=" + new Integer(nextSkip) + "\">" +
                                "next " + CODES_PER_PAGE + "\u2192" +
                                "</a>");
                }
            }
            if(displaySet.partitions.length > 1) {
                ctx.println("</td></tr>");
            }
        }
        if(displaySet.partitions.length > 1) {
            ctx.println("</table>");
        }
    } // no multiple pages
    else {
        if(displaySet.partitions.length > 1) {
            ctx.println("<br /><b>Items:</b> ");
            for(int j=0;j<displaySet.partitions.length;j++) {
                    ctx.print("<b><a class='selected' style='text-decoration:none' href='#"+displaySet.partitions[j].name+"'>");
                    ctx.print(displaySet.partitions[j].name + "</a></b> ");
            }
            ctx.println("<br />");
        }
    }
    //        ctx.println("</p>");
    ctx.println("</div>");
    return skip;
}

public static int pages=0;
/**
* Main setup
 */
static  boolean isSetup = false;
public synchronized void doStartup() throws ServletException {
    
    if(isSetup == true) {
        return;
    }
    
    survprops = new java.util.Properties(); 
    if(cldrHome == null) {
        cldrHome = System.getProperty("catalina.home");
        if(cldrHome == null) {  
            busted("no $(catalina.home) set - please use it or set a servlet parameter cldr.home");
            return;
        } 
        File homeFile = new File(cldrHome, "cldr");
        if(!homeFile.isDirectory()) {
            busted("$(catalina.home)/cldr isn't working as a CLDR home. Not a directory: " + homeFile.getAbsolutePath());
            return;
        }
        cldrHome = homeFile.getAbsolutePath();
    }
    
    
    try {
        java.io.FileInputStream is = new java.io.FileInputStream(new java.io.File(cldrHome, "cldr.properties"));
        survprops.load(is);
        is.close();
    } catch(java.io.IOException ioe) {
        /*throw new UnavailableException*/
        logger.log(Level.SEVERE, "Couldn't load cldr.properties file from '" + cldrHome + "/cldr.properties': ",ioe);
        busted("Couldn't load cldr.properties file from '" + cldrHome + "/cldr.properties': "
               + ioe.toString()); /* .initCause(ioe);*/
               return;
    }

vetdata = survprops.getProperty("CLDR_VET_DATA", cldrHome+"/vetdata"); // dir for vetted data
if(!new File(vetdata).isDirectory()) {
    busted("CLDR_VET_DATA isn't a directory: " + vetdata);
    return;
}
if(false && (loggingHandler == null)) { // TODO: switch? Java docs seem to be broken.. following doesn't work.
    try {
        loggingHandler = new java.util.logging.FileHandler(vetdata + "/"+LOGFILE,0,1,true);
        loggingHandler.setFormatter(new java.util.logging.SimpleFormatter());
        logger.addHandler(loggingHandler);
        logger.setUseParentHandlers(false);
    } catch(Throwable ioe){
        busted("Couldn't add log handler for logfile (" + vetdata+"/"+LOGFILE +"): " + ioe.toString());
        return;
    }
}

vap = survprops.getProperty("CLDR_VAP"); // Vet Access Password
if((vap==null)||(vap.length()==0)) {
    /*throw new UnavailableException*/
    busted("No vetting password set. (CLDR_VAP in cldr.properties)");
    return;
}
vetweb = survprops.getProperty("CLDR_VET_WEB",cldrHome+"/vetdata"); // dir for web data
cldrLoad = survprops.getProperty("CLDR_LOAD_ALL"); // preload all locales?
fileBase = survprops.getProperty("CLDR_COMMON",cldrHome+"/common") + "/main"; // not static - may change lager
specialMessage = survprops.getProperty("CLDR_MESSAGE"); // not static - may change lager
specialHeader = survprops.getProperty("CLDR_MESSAGE"); // not static - may change lager

if(!new File(fileBase).isDirectory()) {
    busted("CLDR_COMMON isn't a directory: " + fileBase);
    return;
}

if(!new File(vetweb).isDirectory()) {
    busted("CLDR_VET_WEB isn't a directory: " + vetweb);
    return;
}

File cacheDir = new File(cldrHome, "cache");
logger.info("Cache Dir: " + cacheDir.getAbsolutePath() + " - creating and emptying..");
CachingEntityResolver.setCacheDir(cacheDir.getAbsolutePath());
CachingEntityResolver.createAndEmptyCacheDir();


isSetup = true;



int status = 0;
logger.info(" ------------------ " + new Date().toString() + " ---------------");
if(isBusted != null) {
    return; // couldn't write the log
}
logger.info("SurveyTool starting up. root=" + new File(cldrHome).getAbsolutePath());
if ((specialMessage!=null)&&(specialMessage.length()>0)) {
    logger.warning("SurveyTool with CLDR_MESSAGE: " + specialMessage);
    busted("message: " + specialMessage);
}
SurveyMain m = new SurveyMain();
/*
 if(!m.reg.read()) {
     busted("Couldn't load user registry [at least put an empty file there]   - exiting");
     return;
 }
 */
if(!readWarnings()) {
    // already busted
    return;
}
if((cldrLoad != null) && cldrLoad.length()>0) {
    m.loadAll();
}
doStartupDB();

logger.info("SurveyTool ready for requests. Memory in use: " + usedK());
}

public void destroy() {
    logger.warning("SurveyTool shutting down..");
    doShutdownDB();
    busted("servlet destroyed");
    super.destroy();
}

protected void startCell(WebContext ctx, String background) {
    ctx.println("<td bgcolor=\"" + background + "\">");
}

protected void endCell(WebContext ctx) {
    ctx.println("</td>");
}

protected void doCell(WebContext ctx, String type, String value) {
    startCell(ctx,"#FFFFFF");
    ctx.println(value);
    endCell(ctx);
}

protected void doDraftCell(WebContext ctx, String type, String value) {
    startCell(ctx,"#DDDDDD");
    ctx.println("<i>Draft</i><br/>");
    ctx.println(value);
    endCell(ctx);
}

protected void doPropCell(WebContext ctx, String type, String value) {
    startCell(ctx,"#DDFFDD");
    ctx.println("<i>Proposed:</i><br/>");
    ctx.println(value);
    endCell(ctx);
}

// utils
private Node getVettedNode(Node context, String resToFetch){
    NodeList list = LDMLUtilities.getChildNodes(context, resToFetch);
    Node node =null;
    if(list!=null){
        for(int i =0; i<list.getLength(); i++){
            node = list.item(i);
            if(LDMLUtilities.isNodeDraft(node)){
                continue;
            }
            /*
             if(isAlternate(node)){
                 continue;
             }
             */
            return node;
        }
    }
    return null;
}

static protected File[] getInFiles() {
    // 1. get the list of input XML files
    FileFilter myFilter = new FileFilter() {
        public boolean accept(File f) {
            String n = f.getName();
            return(!f.isDirectory()
                   &&n.endsWith(".xml")
                   &&!n.startsWith("supplementalData") // not a locale
                   /*&&!n.startsWith("root")*/); // root is implied, will be included elsewhere.
        }
    };
    File baseDir = new File(fileBase);
    return baseDir.listFiles(myFilter);
}


void writeRadio(WebContext ctx,String xpath,String type,String value,String checked) {
    writeRadio(ctx, xpath, type, value, checked.equals(value));        
}

void writeRadio(WebContext ctx,String xpath,String type,String value,boolean checked) {
    ctx.println("<input type=radio name='" + fieldsToHtml(xpath,type) + "' value='" + value + "' " +
                (checked?" CHECKED ":"") + "/>");
    ///*srl*/        ctx.println("<sup><tt>" + fieldsToHtml(xpath,type) + "</tt></sup>");
}


public static final com.ibm.icu.text.Transliterator hexXML = com.ibm.icu.text.Transliterator.getInstance(
                                                                                                         "[^\\u0009\\u000A\\u0020-\\u007E\\u00A0-\\u00FF] Any-Hex/XML");

// like above, but quote " 
public static final com.ibm.icu.text.Transliterator quoteXML = com.ibm.icu.text.Transliterator.getInstance(
                                                                                                           "[^\\u0009\\u000A\\u0020-\\u0021\\u0023-\\u007E\\u00A0-\\u00FF] Any-Hex/XML");

// cache of documents
static Hashtable docTable = new Hashtable();
static Hashtable docVersions = new Hashtable();

public Document fetchDoc( String locale) {
    Document doc = null;
    locale = pool(locale);
    doc = (Document)docTable.get(locale);
    if(doc!=null) {
        return doc;
    }
    String fileName = fileBase + File.separator + locale + ".xml";
    File f = new File(fileName);
    boolean ex  = f.exists();
    boolean cr  = f.canRead();
    String res  = null; /* request.getParameter("res"); */ /* ALWAYS resolve */
    String ver = LDMLUtilities.getCVSVersion(fileBase, locale + ".xml");
    if(ver != null) {
        docVersions.put(locale, pool(ver));
    }
    if((res!=null)&&(res.length()>0)) {
        // throws exception
        doc = LDMLUtilities.getFullyResolvedLDML(fileBase, locale, 
                                                 false, false, false, false);
    } else {
        doc = LDMLUtilities.parse(fileName, false);
    }
    if(doc != null) {
        // add to cache
        docTable.put(locale, doc);
    }
    collectXpaths(doc, locale, "/");
    if((cldrLoad != null) && (cldrLoad.length()>0) && !cldrLoad.startsWith("u")) {
        System.err.print('\b');
        System.err.print('x'); 
        System.err.flush();
    }
    return doc;
}

static int usedK() {
    Runtime r = Runtime.getRuntime();
    double total = r.totalMemory();
    total = total / 1024;
    double free = r.freeMemory();
    free = free / 1024;
    return (int)(Math.floor(total-free));
}


void loadAll() {   
    boolean ultra = cldrLoad.startsWith("u");
    System.err.println("Pre-Loading cache... " + usedK() + "K used so far.\n");
    logger.info("SurveyTool pre-loading cache.");
    if(ultra) {
        System.err.println("Ultra Mode [loading ALL nodesets]");
    }
    File[] inFiles = getInFiles();
    int nrInFiles = inFiles.length;
    if(inFiles == null) {
        busted("Can't load CLDR data files from " + fileBase);
        return;
    }
    int ti = 0;
    for(int i=0;i<nrInFiles;i++) {
        String localeName = inFiles[i].getName();
        if(ultra) {
            System.err.print(i + "/" + nrInFiles + ":           " + localeName + "           ");
        }
        int dot = localeName.indexOf('.');
        if(dot !=  -1) {
            localeName = localeName.substring(0,dot);
            if(!ultra && (i>0)&&((i%50)==0)) {
                System.err.println("   "+ i + "/" + nrInFiles + ". " + usedK() + "K mem used");
            }
            System.err.print('.');
            System.err.flush();
            try {
                fetchDoc(localeName);
                
                if(ultra) {
                    int j;
                    WebContext ctx = new WebContext(false);
                    ctx.setLocale(new ULocale(localeName));
                    for(j =0 ; j < LOCALEDISPLAYNAMES_ITEMS.length; j++) {                                
                        NodeSet ns = getNodeSet(ctx, LOCALEDISPLAYNAMES_ITEMS[j]);
                        ti += ns.count();
                        System.err.print("l");
                        System.err.flush();
                    }
                    for(j =0 ; j < OTHERROOTS_ITEMS.length; j++) {                                
                        NodeSet ns = getNodeSet(ctx, OTHERROOTS_ITEMS[j]);
                        ti += ns.count();
                        System.err.print("o");
                        System.err.flush();
                    }
                    NodeSet ns = getNodeSet(ctx, xOTHER);
                    ti += ns.count();
                    System.err.print("O");
                    System.err.flush();
                    String nodeHashInfo;
                    {
                        Hashtable nodeHash = getNodeHash();            
                        if(nodeHash != null) {
                            nodeHashInfo = "nh" + nodeHash.size();
                        } else {
                            nodeHashInfo = "nhNULL";
                        }
                    }
                    System.err.print("           " + ti + " nodes loaded.. (sc" + stringHash.size() + 
                                     ///*srl*/                            "[-" + stringHashHit + "=" + stringHashIdentity + "]" + 
                                     ", " + 
                                     "xsc" + xpt.xstringHash.size() + ", " + nodeHashInfo + ", nhc" + nodeHashCreates + ")         \r");
                    //ctx.close();
                    /* if you want to print otu the string cache for any reason, this is probably as good a place as any. */
                    /*  {                        
                        Enumeration e = stringHash.keys();
                    for(;e.hasMoreElements();) {
                        String k = e.nextElement().toString();
                        System.err.print(k +",");
                    }
                    return; 
                    } */
                }
            } catch(Throwable t) {
                System.err.println();
                System.err.println(localeName + " - err: " + t.toString());
                t.printStackTrace();
                System.err.println(localeName + " - skipped!");
            }
        }
    }
    System.err.println();
    System.err.println("Done. Fetched " + nrInFiles + " files.");
    System.err.println(" " + usedK() + "K used so far.\n");
    {
        String nodeHashInfo;
        {
            Hashtable nodeHash = getNodeHash();            
            if(nodeHash != null) {
                nodeHashInfo = "nh" + nodeHash.size();
            } else {
                nodeHashInfo = "nhNULL";
            }
        }
        System.err.println("Caches:  (sc" + stringHash.size() + 
                           ///*srl*/                            "[-" + stringHashHit + "=" + stringHashIdentity + "]" + 
                           ", " + 
                           "xsc" + xpt.xstringHash.size() + ", " + nodeHashInfo + ", nhc" + nodeHashCreates + ")");
    }
}

private void busted(String what) {
    System.err.println("SurveyTool busted: " + what);
    isBusted = what;
    logger.severe(what);
}

private void appendLog(WebContext ctx, String what) {
    String ipInfo =  ctx.userIP();
    appendLog(what, ipInfo);
}

public void appendLog(String what) {
    logger.info(what);
}

public synchronized void appendLog(String what, String ipInfo) {
    logger.info(what + " [@" + ipInfo + "]");
}

TreeMap allXpaths = new TreeMap();    
public static Set draftSet = Collections.synchronizedSet(new HashSet());


static final public String[] distinguishingAttributes =  { "key", "registry", "alt", "iso4217", "iso3166", "type", "default",
    "measurementSystem", "mapping", "abbreviationFallback", "preferenceOrdering" };

static int xpathCode = 0;
void collectXpaths(Node root, String locale, String xpath) {
    for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
        if(node.getNodeType()!=Node.ELEMENT_NODE){
            continue;
        }
        String nodeName = node.getNodeName();
        String newPath = xpath + "/" + nodeName;
        for(int i=0;i<distinguishingAttributes.length;i++) {
            String da = distinguishingAttributes[i];
            String nodeAtt = LDMLUtilities.getAttributeValue(node,da);
            if((nodeAtt != null) && 
               !(da.equals(LDMLConstants.ALT)
                 /* &&nodeAtt.equals(LDMLConstants.PROPOSED) */ )) { // no alts for now
                newPath = newPath + "[@"+da+"='" + nodeAtt + "']";
            }
        }
        String draft=LDMLUtilities.getAttributeValue(node,LDMLConstants.DRAFT);
        if((draft != null)&&(draft.equals("true"))) {
            draftSet.add(locale);
        }
        
        allXpaths.put(xpt.poolx(newPath), CookieSession.j + "X" + CookieSession.cheapEncode(xpathCode++));
        collectXpaths(node, locale, newPath);
    }
}

/**
* convert a XPATH:TYPE form to an html field.
 * if type is null, means:  hash the xpath
 */
String fieldsToHtml(String xpath, String type)
{
    if(type == null) {
        String r = (String)allXpaths.get(xpath);
        if(r == null) {
            // we've found a totally new xpath. Mint a new key.
            r = CookieSession.j + "Y" + CookieSession.cheapEncode(xpathCode++);
            allXpaths.put(xpt.poolx(xpath), r);
        }
        return r;
    } else {
        return xpath + "/" + type;
    }
}

private void printWarning(WebContext ctx, String myxpath) {
    if(myxpath == null) {
        return;
    }
    String warnHash = "W"+fieldsToHtml(myxpath,null);
    String warning = getWarning(ctx.locale.toString(), myxpath);
    if(warning != null) {
        String warningTitle = quoteXML.transliterate(warning);
        ctx.println("<a href='javascript:show(\"" + warnHash + "\")'>" + 
                    "<img border=0 align=right src='" + CLDR_ALERT_ICON + "' width=16 height=16 alt=\"" +   
                    warningTitle + "\" title=\"" + warningTitle + "\"></a>");
        ctx.println("<!-- <noscript>Warning: </noscript> -->" + 
                    "<div style='display: none' class='warning' id='" + warnHash + "'>" +
                    warningTitle + "</div>");
    } // else {
      ///*srl*/                ctx.println("[<tt>" + ctx.locale + " " + myxpath + "</tt>]");
      //      }
}

static long shortN = 0;
static final int MAX_CHARS = 100;

private synchronized void printShortened(WebContext ctx, String str) {
    if(str.length()<MAX_CHARS) {
        ctx.println(str);
    } else {
        String key = CookieSession.cheapEncode(shortN++);
        printShortened(ctx,str.substring(0,MAX_CHARS), str, key); 
    }
}

private void printShortened(WebContext ctx, String shortStr, String longStr, String warnHash ) {
        ctx.println("<span id='h_"+warnHash+"'>" + shortStr + "... ");
        ctx.print("<a href='javascript:show(\"" + warnHash + "\")'>" + 
                    "(Click to show entire warning.)</a></span>");
        ctx.println("<!-- <noscript>Warning: </noscript> -->" + 
                    "<span style='display: none'  id='" + warnHash + "'>" +
                    longStr + "<a href='javascript:hide(\"" + warnHash + "\")'>" + 
                    "(hide.)</a></span>");
}


Hashtable xpathWarnings = new Hashtable();

String getWarning(String locale, String xpath) {
    return (String)xpathWarnings.get(locale+" "+xpath);
}


boolean readWarnings() {
    int lines  = 0;
    try {
        BufferedReader in
        = BagFormatter.openUTF8Reader(cldrHome, "surveyInfo.txt");
        String line;
        while ((line = in.readLine())!=null) {
            lines++;
            if((line.length()<=0) ||
               (line.charAt(0)=='#')) {
                continue;
            }
            String[] result = line.split("\t");
            // result[0];  locale
            // result[1];  xpath
            // result[2];  warning
            xpathWarnings.put(result[0] + " /" + result[1], result[2]);
        }
    } catch (java.io.FileNotFoundException t) {
        //            System.err.println(t.toString());
        //            t.printStackTrace();
        logger.warning("Warning: Can't read xpath warnings file.  " + cldrHome + "/surveyInfo.txt - To remove this warning, create an empty file there.");
        return true;
    }  catch (java.io.IOException t) {
        System.err.println(t.toString());
        t.printStackTrace();
        busted("Error: trying to read xpath warnings file.  " + cldrHome + "/surveyInfo.txt");
        return true;
    }
    
    //        System.err.println("xpathWarnings" + ": " + lines + " lines read.");
    return true;
}

private static Hashtable stringHash = new Hashtable();

static int stringHashIdentity = 0; // # of x==y hits
static int stringHashHit = 0;

static final String pool(String x) {
    if(x==null) {
        return null;
    }
    String y = (String)stringHash.get(x);
    if(y==null) {
        stringHash.put(x,x);
        return x;
    } else {
        ///*srl*/            if(x==y) { stringHashIdentity++; throw new RuntimeException("pool(x)==x"); }
        ///*srl*/            stringHashHit++;
        return y;
    }
}


// DB stuff
public String db_driver = "org.apache.derby.jdbc.EmbeddedDriver";
public String db_protocol = "jdbc:derby:";
public static final String CLDR_DB_U = "cldrdb_user";
public String cldrdb_u = null;
public static final String CLDR_DB = "cldrdb";
public String cldrdb = null;

private Connection getDBConnection()
{
    return getDBConnection("");
}

private Connection getU_DBConnection()
{
    return getU_DBConnection("");
}


private Connection getDBConnection(String options)
{
    try{ 
        Properties props = new Properties();
        props.put("user", "cldr_user");
        props.put("password", "cldr_password");
        cldrdb =  dbDir.getAbsolutePath();
        Connection nc =  DriverManager.getConnection(db_protocol +
                                                     cldrdb + options, props);
        nc.setAutoCommit(false);
        return nc;
    } catch (SQLException se) {
        busted("Fatal in getDBConnection: " + unchainSqlException(se));
        return null;
    }
}

private Connection getU_DBConnection(String options)
{
    try{ 
        Properties props = new Properties();
        props.put("user", "cldr_user");
        props.put("password", "cldr_password");
        cldrdb_u =  dbDir_u.getAbsolutePath();
        Connection nc =  DriverManager.getConnection(db_protocol +
                                                     cldrdb_u + options, props);
        nc.setAutoCommit(false);
        return nc;
    } catch (SQLException se) {
        busted("Fatal in getDBConnection: " + unchainSqlException(se));
        return null;
    }
}

File dbDir = null;
File dbDir_u = null;

private void doStartupDB()
{
    dbDir = new File(cldrHome,CLDR_DB);
    dbDir_u = new File(cldrHome,CLDR_DB_U);
    boolean doesExist = dbDir.isDirectory();
    boolean doesExist_u = dbDir_u.isDirectory();
    
    logger.info("SurveyTool setting up database.. " + dbDir.getAbsolutePath());
    try
    {
        Class.forName(db_driver).newInstance();
        Connection conn = getDBConnection(";create=true");
        logger.info("Connected to database " + cldrdb);
        Connection conn_u = getU_DBConnection(";create=true");
        logger.info("Connected to user database " + cldrdb_u);
        
        // set up our main tables.
        if(doesExist == false) {
            // nothing to setup.
        }
        conn.commit();        
        conn.close(); 
        
        conn_u.commit();
        conn_u.close();
    }
    catch (SQLException e)
    {
        busted("On DB startup: " + unchainSqlException(e));
    }
    catch (Throwable t)
    {
        busted("Some error on DB startup: " + t.toString());
        t.printStackTrace();
    }
    // now other tables..
    try {
        reg = UserRegistry.createRegistry(logger, getU_DBConnection(), !doesExist_u);
        if(!doesExist_u) { // only import users on first run through..
            reg.importOldUsers(vetdata);
        }
    } catch (SQLException e) {
        busted("On UserRegistry startup: " + unchainSqlException(e));
    }
    try {
        xpt = XPathTable.createTable(logger, getDBConnection(), !doesExist);
    } catch (SQLException e) {
        busted("On XPathTable startup: " + unchainSqlException(e));
    }
    // note: make xpt before CLDRDBSource..
    try {
        CLDRDBSource.setupDB(logger, getDBConnection(), !doesExist);
    } catch (SQLException e) {
        busted("On CLDRDBSource startup: " + unchainSqlException(e));
    }
    if(!doesExist) {
        logger.info("all dbs setup");
    }
}    

public static final String unchainSqlException(SQLException e) {
    String echain = "SQL exception: \n ";
    while(e!=null) {
        echain = echain + " -\n " + e.toString();
        e = e.getNextException();
    }
    return echain;
}

    void doShutdownDB() {
        boolean gotSQLExc = false;
        
        try
        {
            // shut down other connections
            reg.shutdownDB();
            xpt.shutdownDB();
            
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        }
        catch (SQLException se)
        {
            gotSQLExc = true;
            logger.info("DB: while shutting down: " + se.toString());
        }
        
        if (!gotSQLExc)
        {
            logger.warning("Database did not shut down normally");
        }
        else
        {
            logger.info("Database shut down normally");
        }
    }
/*
 private void smok(int i) {
     System.out.println("#" + i + " = " + xpt.getById(i));
 }*/

    public static void main(String arg[]) {
        System.out.println("Starting some test of SurveyTool locally....");
        try{
            cldrHome="/data/jakarta/cldr";
            vap="NO_VAP";
            SurveyMain sm=new SurveyMain();
            sm.doStartup();
            sm.doStartupDB();
            /*  sm.smok(4);
            sm.smok(3);
            sm.smok(2);
            sm.smok(1);
            sm.smok(33);
            sm.smok(333);
            sm.smok(1333);  */
            
            if(arg.length>0) {
                CLDRDBSource dbSource = CLDRDBSource.createInstance(sm.fileBase, sm.xpt, new ULocale(arg[0]),
                                                                    sm.getDBConnection(), null);            
                System.out.println("dbSource created.");
                CLDRFile my = new CLDRFile(dbSource,false);
                System.out.println("file created ");
                CheckCLDR check = CheckCLDR.getCheckAll("(?!.*Collision.*).*");
                System.out.println("check created");
                List result = new ArrayList();
                check.setCldrFileToCheck(my, null, result); // TODO: when does this get updated?
                System.out.println("file set");
            }
            
            sm.doShutdownDB();
        } catch(Throwable t) {
            System.out.println("Something bad happened.");
            System.out.println(t.toString());
            t.printStackTrace();
        }
    }
    
    // ====== Utility Functions
    public static final String timeDiff(long a) {
        return ElapsedTimer.elapsedTime(a);
    }
    
    public static final String timeDiff(long a, long b) {
        return ElapsedTimer.elapsedTime(a,b);
    }

}


// DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED
// STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF 
// DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED 
// STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF DELETED STUFF 

// ******* ShowPathList
/*
 if(true) { return; } // =====================
 else {
     StandardCodes standardCodes = StandardCodes.make();
     Set defaultSet = null;
     if(lastElement != null) {
         defaultSet = standardCodes.getAvailableCodes(lastElement); // TODO: 2 nonstandard types?
     } else if(isTz) {
         defaultSet = standardCodes.getAvailableCodes("tzid");
         fullThing = "//ldml/dates/timeZoneNames/zone/short/standard";
     }
     
     if(defaultSet == null ) {
         // ctcx.println("<hr/><i>Err: 0 items in defaultSet</i><hr/>");
         defaultSet = new HashSet();
         theIterator = cf.iterator(fullThing);
     } else { 
         theIterator = defaultSet.iterator();
     }
     // NAVIGATION .. calculate skips.. 
     skip = showSkipBox(ctx, defaultSet.size());
     //            if(changes.get(xOTHER + "/" + NEW)!=null) {
     //                changes.remove(xOTHER + "/" + NEW);
     //                ctx.println("<div class='missing'><b>Warning: Remember to click the 'change' radio button after typing in a change.  Please check the status of change boxes below.</b></div><br/>");
     //            }
     
     
     ctx.println(" Printing dump for " + fullThing + ", #" + xpt.getByXpath(fullThing) + "<br />");
     
     
     // Form: 
     ctx.printUrlAsHiddenFields();
     ctx.println("<table class='list' border=1>");
     ctx.println("<tr>");
     ctx.println(" <th class='heading' bgcolor='#DDDDDD' colspan=2>Name<br/><div style='border: 1px solid gray; width: 6em;' align=left><tt>Code</tt></span></th>");
     ctx.println(" <th class='heading' bgcolor='#DDDDDD' colspan=1>Best<br/>");
     ctx.printHelpLink("/Best");
     ctx.println("</th>");
     ctx.println(" <th class='heading' bgcolor='#DDDDDD' colspan=1>Contents</th>");
     ctx.println("</tr>");
     
     
     for(Iterator e = theIterator;e.hasNext();) {
         String type = (String)e.next();
         
         count++;
         if(skip > 0) {
             --skip;
             continue;
         }
         dispCount++;
         
         boolean first = true;
         
         boolean gotNonDraft = false;
         
         String locale = ctx.locale.toString();
         String theLocale = locale;
         
         do {
             java.sql.ResultSet rs = ourSrc.listForType(xpt.getByXpath(fullThing), type, theLocale);
             
             try {
                 while(rs.next()) {
                     if(first) {
                         ctx.println("<tr><th colspan='3' valign='left'>" + type + "</th></tr>");
                         first = false;
                     }
                     int submitId = rs.getInt(4);
                     if(rs.wasNull()) submitId = -1; //  null submitter
                     
                     String at = rs.getString(2);
                     String ap = rs.getString(3);
                     String val = rs.getString(1);
                     String subXpath = xpt.getById(rs.getInt(5));
                     String fullPath = xpt.getById(rs.getInt(6));
                     
                     if((theLocale==locale)||((at==null)&&(ap==null))) {
                         String rowclass="current";
                         if((at!=null)&&(at.equals("proposed"))) {
                             rowclass = "proposed";
                         } else if(theLocale != locale) {
                             rowclass = "fallback";
                         }
                         ctx.println("<tr class='" + rowclass + "'><td>" + val + "</td>" +
                                     "<td>"+ ((at!=null)?at:"") + "<br />" + 
                                     ((ap!=null)?ap:"") + "</td>" + 
                                     "");
                         if(theLocale != locale) {
                             ctx.println("<td><b>" + theLocale + "</b></td>");
                         }
                         
                         ctx.println("<td>");
                         pathParts.clear();
                         fullPathParts.clear();
                         if(checkCldr != null)  {
                             checkCldrResult.clear();
                             checkCldr.check(subXpath, fullPath, val, pathParts, fullPathParts, checkCldrResult);
                             for (Iterator it3 = checkCldrResult.iterator(); it3.hasNext();) {
                                 CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();
                                 if (!status.getType().equals(status.exampleType)) {
                                     ctx.println(status.toString() + "\t" + val + "\t" + fullPath);
                                 } else {
                                     ctx.println("<i>example available</i><br />");
                                 }
                             }                                                                            
                         }
                         ctx.println("</td>");
                         
                         
                         ctx.println("</tr>"); */
/*
 if(theLocale!=locale) {
     ctx.println(" ("+ theLocale+") ");
 }
 
 ctx.println("at:" + at + "," +
             "ap:" + ap + "," + 
             "sub:" + submitId + 
             "  = <span style='border: 1px solid gray'>" + val+"</span><br />");*/

/*                            if(f.xpath != null) {
ctx.println("<tr class='xpath'><td colspan=4>"+  f.xpath + "</td></tr>");
}*/

/*           if(f.isAlias == true) {
ctx.println("<tr class='alias'><td colspan=4>" + f.type +
            "<a href=\"" + ctx.base() + "?x=" + ctx.field("x") + "&_=" + f.fallbackLocale + "\">" + 
            new ULocale(f.fallbackLocale).getDisplayName() +                        
            "</a>");
ctx.println("</td></tr>");
continue;
}*/


/*
 String base = ((xpath==null)?f.xpath:type);
 String main = val;
 String mainFallback = null;
 int mainDraft = 0; // count
                    //            if(f.mainDraft) {
                    //                mainDraft = 1; // for now: one draft
                    //            }
 String fieldName = ((xpath!=null)?xpath:f.xpath);
 String fieldPath = ((xpath!=null)?type:null);
 int nRows = 1;         // the 'new' line (user edited)
 nRows ++; // 'main'
           // are we showing a fallback locale in the 'current' slot?
 if( (f.fallback != null) && // if we have a fallback
     ( (mainDraft > 0) || (f.main == null) ) ) {
     mainFallback = f.fallbackLocale;
     if(mainDraft > 0) {
         nRows ++; // fallback
     }
 }else if (mainDraft > 0) {
     nRows ++; // for the Draft entry
 }
 if(f.alts != null) {
     nRows += f.alts.size();
 }
 
 // Analyze user input.
 String checked = null;  // What option has the user checked?
 String newString = null;  // What option has the user checked?
 if(changes != null) {
     checked = (String)changes.get(base + SUBVETTING); // fetch VETTING data
     newString = (String)changes.get(base + SUBNEW); // fetch NEW data
                                                     ////               ctx.println(":"+base+SUBVETTING + ":");
 }
 if(checked == null) {
     checked = NOCHANGE;
 }
 */
/*
 ctx.println("<tr>");
 // 1. name/code
 String tx_text = type;//tx.text(f)
 ctx.println("<th valign=top align=left class='name' colspan=2 rowspan=" + (nRows-1) + ">" + tx_text + "");
 if(type != null) {
     ctx.println("<br/><tt>(" + type + ")</tt>");
 }
 */
/*
 // see if there's a warning.
 String myxpath = f.xpath;
 if(myxpath == null) {
     myxpath = xpath;
     if(f.main != null) {
         myxpath = myxpath + "/" + f.main.getNodeName();
     } else {
         //                    ctx.println("<B>problem - missing item</B>");
         myxpath = null;
     }
     if(f.type != null) {
         myxpath = myxpath + "[@type='" + f.type + "']";
     }
     if(f.key != null) {
         myxpath = myxpath + "[@key='" + f.key + "']";
     }
     
     //                ctx.println("[<tt>" + ctx.locale + " " + myxpath + "</tt>]");
 }
 */

//            ctx.println("</th>");

/*
 // Now there are a pair of columns for each of the following. 
 // 2. fallback
 if(mainFallback != null) {
     ctx.println("<td align=right class='fallback'>");
     ctx.println("from " + 
                 "<a href=\"" + ctx.base() + "?x=" + ctx.field("x") + "&_=" + mainFallback + "\">" + 
                 new ULocale(mainFallback).getDisplayName() +                        
                 "</a>");
     writeRadio(ctx,fieldName,fieldPath,CURRENT,checked);
     ctx.println("</td>");
     ctx.println("<td class='fallback'>");
     ctx.println(LDMLUtilities.getNodeValue(f.fallback));
     ctx.println("</td>");
 } else if((main!=null)&&(mainDraft==0)) {
     ctx.println("<td align=right class='current'>");
     ctx.println("current");
     writeRadio(ctx,fieldName,fieldPath,CURRENT,checked);
     ctx.println("</td>");
     ctx.println("<td class='current'>");
     printWarning(ctx, myxpath);
     ctx.println(main);
     ctx.println("</td>");
 } else //  if(main == null) 
 {
     ctx.println("<td align=right class='missing'>");
     
     if(f.fallbackLocale != null) {
         ctx.println("see " + 
                     "<a href=\"" + ctx.base() + "?x=" + ctx.field("x") + "&_=" + f.fallbackLocale + "\">" + 
                     new ULocale(f.fallbackLocale).getDisplayName() +                        
                     "</a>");
     } else {
         ctx.println("<i>current</i>");
         writeRadio(ctx,fieldName,fieldPath,CURRENT,checked);
     }
     ctx.println("</td>");
     ctx.println("<td title=\"Data missing - raw code shown.\" class='missing'><tt>");
     ctx.println(type); // in typewriter <tt> to show that it is a code.
     ctx.println("</tt></td>");
 }
 ctx.println("</tr>");
 
 ctx.println("<tr>");
 
 // Draft item
 if(mainDraft > 0) {
     ctx.println("<td align=right class='draft'>");
     ctx.println("draft");
     writeRadio(ctx,fieldName,fieldPath,DRAFT,checked);
     ctx.println("</td>");
     ctx.println("<td class='draft'>");
     ctx.println(main);
     ctx.println("</td>");
     ctx.println("</tr>");
     ctx.println("<tr>");
 }
 
 // Proposed item
 if(f.alts != null) {
     Enumeration ee = f.alts.keys();
     for(;ee.hasMoreElements();) {
         String k = (String)(ee.nextElement());
         ctx.println("<td align=right class='proposed'>");
         ctx.println(k);
         writeRadio(ctx,fieldName,fieldPath,PROPOSED + k,checked);
         ctx.println("</td>");
         ctx.println("<td class='proposed'>");
         printWarning(ctx, myxpath+"[@alt='" + k + "']");
         ctx.println(LDMLUtilities.getNodeValue((Node)f.alts.get(k)));
         ctx.println("</td>");
         ctx.println("</tr>");
         ctx.println("<tr>");
     }
 }
 
 //'nochange' and type
 ctx.println("<th class='type'>");
 if(type == null) {
     type = "NULL??";
 }
 int lastSlash = type.lastIndexOf('/');
 String showType;
 if(lastSlash != -1) {
     showType = type.substring(lastSlash+1);
 } else {
     showType = type;
 }
 ctx.println("<tt>" + showType + "</tt>");
 ctx.println("</th>");
 ctx.println("<td class='nochange'>");
 ctx.println("Don't care");
 writeRadio(ctx,fieldName,fieldPath,NOCHANGE,checked);
 ctx.println("</td>");
 
 // edit text
 ctx.println("<td align=right class='new'>");
 ctx.println(((mainDraft>0) || ((f.alts)!=null))?"change":"incorrect");
 writeRadio(ctx,fieldName,fieldPath,NEW,checked);
 ctx.println("</td>");
 ctx.println("<td class='new'>");
 String change = "";
 if(changes != null) {
     //     change = (String)changes.get(type + "//n");
 }
 if((mainDraft>0) || ((f.alts)!=null)) {
     // this is supposed to Automatically check the 'change' button when user types something in.
     // but it doesn't work.
     String blurCheckScript = ""; //" else {  document.forms[0].elements['" + fieldsToHtml(fieldName,fieldPath) + "'][3].checked=true; "
     ctx.print("<input size=50 class='inputbox' ");
     ctx.print("onblur=\"if (value == '') {value = '" + UNKNOWNCHANGE + "'} " + blurCheckScript + " }\" onfocus=\"if (value == '" + 
               UNKNOWNCHANGE + "') {value =''}\" ");
     ctx.print("value=\"" + 
               (  (newString!=null) ? newString : UNKNOWNCHANGE )
               + "\" name=\"" + fieldsToHtml(fieldName,fieldPath) + SUBNEW + "\">");
 } else {
     ctx.print("Item is incorrect.");
 }
 ctx.println("</td>");
 
 //if((newString != null) && ((checked!=null)&&(!checked.equals(NEW)))) {
 //    ctx.println("<td class='pager'><b>Don't forget to click \"change\" when submitting a change.</b></td>");
 //}
 
 ctx.println("</tr>");
 
 ctx.println("<tr class='sep'><td class='sep' colspan=4 bgcolor=\"#CCCCDD\"></td></tr>");
 */                                             

/*
 if((at==null)&&(at==null)) {
     gotNonDraft=true;
 }
                     }
                 }
 rs.close();
             } catch(SQLException se) {
                 ctx.println("err in showPathList 0: " + unchainSqlException(se));
             }
 theLocale = WebContext.getParent(theLocale);
         } while((theLocale != null)&&(gotNonDraft==false));
 
 if(dispCount >= CODES_PER_PAGE) {
     break;
 }
 
     } // end loop
 } //end false
        } // end synch
 
 ctx.println("</table>");
 // skip =
 //showSkipBox(ctx, total, sortedMap, tx);
 if(ctx.session.user != null) {
     ctx.println("<div style='margin-top: 1em;' align=right><input type=submit value='" + xSAVE + " for " + 
                 ctx.locale.getDisplayName() +"'></div>");
 }
 ctx.println("</form>");
 */
/*        
java.sql.ResultSet rs = ourSrc.listPrefix(xpath);
Set s = new HashSet();
try {
    while(rs.next()) {
        //XPathTable.CLDR_XPATHS+".xpath," +CLDR_DATA+".value,"+CLDR_DATA+".type,"+CLDR_DATA+".alt_type,"+CLDR_DATA+".alt_proposed,"+CLDR_DATA+".submitter" +
        
        int txpath = rs.getInt(1);
        String atxpath = xpt.getById(txpath);
        s.add(atxpath);
        
        String apath = rs.getString(1);
        String value = rs.getString(2);
        String type = rs.getString(3);
        String alt_type = rs.getString(4);
        String alt_proposed = rs.getString(5);
        int submitter = rs.getInt(6);
        
        ctx.println("<tt>" + apath + "</tt><br />");
        ctx.println("  t:" + type + ", v:" + value +"<br />");
        
    }
    for(Iterator e = s.iterator();e.hasNext();) {
        String f = (String)e.next();
        ctx.println(f + "<br />");
    }
    rs.close();
} catch(SQLException se) {
    ctx.println("err in showPathList: " + unchainSqlException(se));
}

*/

/**
* show the list for an 'other' item, i.e. not a locale data item
 */
/*
 void doOtherList(WebContext ctx, String which) {
     showNodeList(ctx, null, getNodeSet(ctx,which), new NodeSet.NodeSetTexter() {
         StandardCodes standardCodes = StandardCodes.make();
         
         public String text(NodeSet.NodeSetEntry e) {
             if(e.xpath.startsWith(CURRENCYTYPE)) {
                 return standardCodes.getData("currency", e.type);
             } else {
                 return e.type;
             }
         }
     });
 }*/



/**
* @param xpath null if 'individual paths'
 */
/*
 public void showNodeList(WebContext ctx, String xpath, NodeSet mySet, NodeSet.NodeSetTexter tx) {
     int count = 0;
     int dispCount = 0;
     int total = 0;
     int skip = 0;
     total = mySet.count();
     String sortMode = ctx.pref(PREF_SORTMODE);
     boolean sortAlpha = (sortMode.equals(PREF_SORTMODE_ALPHA));
     NodeSet.NodeSetTexter sortTexter;
     
     if(sortAlpha) {
         if(xpath == null) {
             sortTexter = getXpathTexter(tx);
         } else {
             sortTexter = tx;
         }
     } else {
         sortTexter = new DraftFirstTexter(tx);
     }
     
     Map sortedMap = mySet.getSorted(sortTexter);
     String hashName = (xpath != null)?xpath:xOTHER;
     Hashtable changes = (Hashtable)ctx.getByLocale(hashName);
     
     if(tx == null) {
         tx = new NullTexter();
     }
     
     // prepare a new hashtable
     if(changes==null) {
         changes = new Hashtable();  // ?? TODO: do we need to create a hashtable here?
     }
     
     // NAVIGATION .. calculate skips.. 
     skip = showSkipBox(ctx, total, sortedMap, tx);
     if(changes.get(xOTHER + "/" + NEW)!=null) {
         changes.remove(xOTHER + "/" + NEW);
         ctx.println("<div class='missing'><b>Warning: Remember to click the 'change' radio button after typing in a change.  Please check the status of change boxes below.</b></div><br/>");
     }
     
     
     
     // Form: 
     ctx.printUrlAsHiddenFields();
     ctx.println("<table class='list' border=1>");
     ctx.println("<tr>");
     ctx.println(" <th class='heading' bgcolor='#DDDDDD' colspan=2>Name<br/><div style='border: 1px solid gray; width: 6em;' align=left><tt>Code</tt></span></th>");
     ctx.println(" <th class='heading' bgcolor='#DDDDDD' colspan=1>Best<br/>");
     ctx.printHelpLink("/Best");
     ctx.println("</th>");
     ctx.println(" <th class='heading' bgcolor='#DDDDDD' colspan=1>Contents</th>");
     ctx.println("</tr>");
     
     // process items..
     for(Iterator e = sortedMap.values().iterator();e.hasNext();) {
         NodeSet.NodeSetEntry f = (NodeSet.NodeSetEntry)e.next();
         count++;
         if(skip > 0) {
             --skip;
             continue;
         }
         dispCount++;
         
         if(f.xpath != null) {
             ctx.println("<tr class='xpath'><td colspan=4>"+  f.xpath + "</td></tr>");
         }
         
         if(f.isAlias == true) {
             ctx.println("<tr class='alias'><td colspan=4>" + f.type +
                         "<a href=\"" + ctx.base() + "?x=" + ctx.field("x") + "&_=" + f.fallbackLocale + "\">" + 
                         new ULocale(f.fallbackLocale).getDisplayName() +                        
                         "</a>");
             ctx.println("</td></tr>");
             continue;
         }
         
         
         String type = f.type;
         String base = ((xpath==null)?f.xpath:type);
         String main = null;
         String mainFallback = null;
         int mainDraft = 0; // count
         if(f.main != null) {
             main = LDMLUtilities.getNodeValue(f.main);
             if(f.mainDraft) {
                 mainDraft = 1; // for now: one draft
             }
         }
         String fieldName = ((xpath!=null)?xpath:f.xpath);
         String fieldPath = ((xpath!=null)?type:null);
         int nRows = 1;         // the 'new' line (user edited)
         nRows ++; // 'main'
                   // are we showing a fallback locale in the 'current' slot?
         if( (f.fallback != null) && // if we have a fallback
             ( (mainDraft > 0) || (f.main == null) ) ) {
             mainFallback = f.fallbackLocale;
             if(mainDraft > 0) {
                 nRows ++; // fallback
             }
         }else if (mainDraft > 0) {
             nRows ++; // for the Draft entry
         }
         if(f.alts != null) {
             nRows += f.alts.size();
         }
         
         // Analyze user input.
         String checked = null;  // What option has the user checked?
         String newString = null;  // What option has the user checked?
         if(changes != null) {
             checked = (String)changes.get(base + SUBVETTING); // fetch VETTING data
             newString = (String)changes.get(base + SUBNEW); // fetch NEW data
                                                             //                ctx.println(":"+base+SUBVETTING + ":");
         }
         if(checked == null) {
             checked = NOCHANGE;
         }
         
         ctx.println("<tr>");
         // 1. name/code
         ctx.println("<th valign=top align=left class='name' colspan=2 rowspan=" + (nRows-1) + ">" + tx.text(f) + "");
         if(f.key != null) {
             ctx.println("<br/><tt>(" + f.key + ")</tt>");
         }
         
         // see if there's a warning.
         String myxpath = f.xpath;
         if(myxpath == null) {
             myxpath = xpath;
             if(f.main != null) {
                 myxpath = myxpath + "/" + f.main.getNodeName();
             } else {
                 //                    ctx.println("<B>problem - missing item</B>");
                 myxpath = null;
             }
             if(f.type != null) {
                 myxpath = myxpath + "[@type='" + f.type + "']";
             }
             if(f.key != null) {
                 myxpath = myxpath + "[@key='" + f.key + "']";
             }
             
             //               ctx.println("[<tt>" + ctx.locale + " " + myxpath + "</tt>]");
         }
         
         ctx.println("</th>");
         
         // Now there are a pair of columns for each of the following. 
         // 2. fallback
         if(mainFallback != null) {
             ctx.println("<td align=right class='fallback'>");
             ctx.println("from " + 
                         "<a href=\"" + ctx.base() + "?x=" + ctx.field("x") + "&_=" + mainFallback + "\">" + 
                         new ULocale(mainFallback).getDisplayName() +                        
                         "</a>");
             writeRadio(ctx,fieldName,fieldPath,CURRENT,checked);
             ctx.println("</td>");
             ctx.println("<td class='fallback'>");
             ctx.println(LDMLUtilities.getNodeValue(f.fallback));
             ctx.println("</td>");
         } else if((main!=null)&&(mainDraft==0)) {
             ctx.println("<td align=right class='current'>");
             ctx.println("current");
             writeRadio(ctx,fieldName,fieldPath,CURRENT,checked);
             ctx.println("</td>");
             ctx.println("<td class='current'>");
             printWarning(ctx, myxpath);
             ctx.println(main);
             ctx.println("</td>");
         } else // {
             ctx.println("<td align=right class='missing'>");
         
         if(f.fallbackLocale != null) {
             ctx.println("see " + 
                         "<a href=\"" + ctx.base() + "?x=" + ctx.field("x") + "&_=" + f.fallbackLocale + "\">" + 
                         new ULocale(f.fallbackLocale).getDisplayName() +                        
                         "</a>");
         } else {
             ctx.println("<i>current</i>");
             writeRadio(ctx,fieldName,fieldPath,CURRENT,checked);
         }
         ctx.println("</td>");
         ctx.println("<td title=\"Data missing - raw code shown.\" class='missing'><tt>");
         ctx.println(type); // in typewriter <tt> to show that it is a code.
         ctx.println("</tt></td>");
         }
     ctx.println("</tr>");
     
     ctx.println("<tr>");
     
     // Draft item
     if(mainDraft > 0) {
         ctx.println("<td align=right class='draft'>");
         ctx.println("draft");
         writeRadio(ctx,fieldName,fieldPath,DRAFT,checked);
         ctx.println("</td>");
         ctx.println("<td class='draft'>");
         ctx.println(main);
         ctx.println("</td>");
         ctx.println("</tr>");
         ctx.println("<tr>");
     }
     
     // Proposed item
     if(f.alts != null) {
         Enumeration ee = f.alts.keys();
         for(;ee.hasMoreElements();) {
             String k = (String)(ee.nextElement());
             ctx.println("<td align=right class='proposed'>");
             ctx.println(k);
             writeRadio(ctx,fieldName,fieldPath,PROPOSED + k,checked);
             ctx.println("</td>");
             ctx.println("<td class='proposed'>");
             printWarning(ctx, myxpath+"[@alt='" + k + "']");
             ctx.println(LDMLUtilities.getNodeValue((Node)f.alts.get(k)));
             ctx.println("</td>");
             ctx.println("</tr>");
             ctx.println("<tr>");
         }
     }
     
     //'nochange' and type
     ctx.println("<th class='type'>");
     if(type == null) {
         type = "NULL??";
     }
     int lastSlash = type.lastIndexOf('/');
     String showType;
     if(lastSlash != -1) {
         showType = type.substring(lastSlash+1);
     } else {
         showType = type;
     }
     ctx.println("<tt>" + showType + "</tt>");
     ctx.println("</th>");
     ctx.println("<td class='nochange'>");
     ctx.println("Don't care");
     writeRadio(ctx,fieldName,fieldPath,NOCHANGE,checked);
     ctx.println("</td>");
     
     // edit text
     ctx.println("<td align=right class='new'>");
     ctx.println(((mainDraft>0) || ((f.alts)!=null))?"change":"incorrect");
     writeRadio(ctx,fieldName,fieldPath,NEW,checked);
     ctx.println("</td>");
     ctx.println("<td class='new'>");
     String change = "";
     if(changes != null) {
         //     change = (String)changes.get(type + "//n");
     }
     if((mainDraft>0) || ((f.alts)!=null)) {
         // this is supposed to Automatically check the 'change' button when user types something in.
         // but it doesn't work.
         String blurCheckScript = ""; //" else {  document.forms[0].elements['" + fieldsToHtml(fieldName,fieldPath) + "'][3].checked=true; "
         ctx.print("<input size=50 class='inputbox' ");
         ctx.print("onblur=\"if (value == '') {value = '" + UNKNOWNCHANGE + "'} " + blurCheckScript + " }\" onfocus=\"if (value == '" + 
                   UNKNOWNCHANGE + "') {value =''}\" ");
         ctx.print("value=\"" + 
                   (  (newString!=null) ? newString : UNKNOWNCHANGE )
                   + "\" name=\"" + fieldsToHtml(fieldName,fieldPath) + SUBNEW + "\">");
     } else {
         ctx.print("Item is incorrect.");
     }
     ctx.println("</td>");*/
/*
 if((newString != null) && ((checked!=null)&&(!checked.equals(NEW)))) {
     ctx.println("<td class='pager'><b>Don't forget to click \"change\" when submitting a change.</b></td>");
 }
 *//*
 ctx.println("</tr>");
  
  ctx.println("<tr class='sep'><td class='sep' colspan=4 bgcolor=\"#CCCCDD\"></td></tr>");
  
  // -----
  
  if(dispCount >= CODES_PER_PAGE) {
      break;
  }
     }
  ctx.println("</table>");*/
 /* skip = *//* showSkipBox(ctx, total, sortedMap, tx);
 if(ctx.session.user != null) {
     ctx.println("<div style='margin-top: 1em;' align=right><input type=submit value='" + xSAVE + " for " + 
                 ctx.locale.getDisplayName() +"'></div>");
 }
 ctx.println("</form>");*/
  
     /*
     * Recursively output a DOM tree. Not used. 
     */
    /*
    private static void dumpIt(WebContext ctx, Node root, int level)
    {
        ctx.println("<br>"); // <li>
        ctx.println(root.getNodeName());
        
        NamedNodeMap attr = root.getAttributes();
        if((attr!=null) && attr.getLength()>0){ //TODO: make this a fcn
                                                // add an element for each attribute different for each attribute
            for(int i=0; i<attr.getLength(); i++){
                Node item = attr.item(i);
                String attrName =item.getNodeName();
                String attrValue = item.getNodeValue();
                ctx.println(attrName + "=\u201c" + attrValue + "\u201d ");
            }
        }
        String value = null;
        Node firstChild = root.getFirstChild();
        if(firstChild != null) {
            value = firstChild.getNodeValue();
        }
        if((value!=null)&&(value.length()>0)) {
            ctx.println("<tt>" + value + "</tt><br/>");
        }
        ctx.println("<br>\n"); // <ul>
        for(Node node=root.getFirstChild();node!=null;node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            //            dumpIt(out, node, level+1);
        }
        ctx.println("<hr/>"); // </ul>
        ctx.println("<br/>"); // </li>
    }*/
