/*
 ******************************************************************************
 * Copyright (C) 2004-2006, International Business Machines Corporation and   *
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
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

import java.util.Properties;


import com.ibm.icu.lang.UCharacter;

public class SurveyMain extends HttpServlet {

    // phase?
    public static final boolean phaseSubmit = false; 
    public static final boolean phaseVetting = false;
    public static final boolean phaseClosed = true;
    public static final boolean phaseDisputed = true;
    /**
    * URL prefix for  help
    */
    public static final String CLDR_HELP_LINK = "http://www.unicode.org/cldr/wiki?SurveyToolHelp";
//   public static final String CLDR_HELP_LINK_EDIT = "http://bugs.icu-project.org/cgibin/cldr/cldrwiki.pl?SurveyToolHelp";
    public static final String CLDR_HELP_LINK_EDIT = CLDR_HELP_LINK;
     
    public static final String SLOW_PAGE_NOTICE = ("<i>Note: The first time a page is loaded it may take some time, please be patient.</i>");    
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
    public static final String REMOVE = "remove";
    public static final String CONFIRM = "confirm";
    public static final String CHANGETO = "change to";
    public static final String PROPOSED_DRAFT = "proposed-draft";
    public static final String MKREFERENCE = "enter-reference";
    
    public static final String STFORM = "stform";

//    public static final String MODIFY_THING = "<span title='You are allowed to modify this locale.'>\u270D</span>";             // was: F802
    //public static final String MODIFY_THING = "<img src='' title='You are allowed to modify this locale.'>";             // was: F802
    static String modifyThing(WebContext ctx) {
        return "&nbsp;"+ctx.modifyThing("You are allowed to modify this locale.");
    }
    
    // SYSTEM PROPERTIES
    public static  String vap = System.getProperty("CLDR_VAP"); // Vet Access Password
    public static  String vetdata = System.getProperty("CLDR_VET_DATA"); // dir for vetted data
    public static  String vetweb = System.getProperty("CLDR_VET_WEB"); // dir for web data
    public static  String cldrLoad = System.getProperty("CLDR_LOAD_ALL"); // preload all locales?
    static String fileBase = System.getProperty("CLDR_COMMON") + "/main"; // not static - may change lager
    static String specialMessage = System.getProperty("CLDR_MESSAGE"); //  static - may change later
    static String specialHeader = System.getProperty("CLDR_HEADER"); //  static - may change later
    static long specialTimer = 0; // 0 means off.  Nonzero:  expiry time of countdown.
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
    static final String QUERY_LOCALE = "_";
    static final String QUERY_SECTION = "x";
    static final String QUERY_EXAMPLE = "e";
	
    static final String SURVEYTOOL_COOKIE_SESSION = "com.org.unicode.cldr.web.CookieSession.id";
    static final String SURVEYTOOL_COOKIE_NONE = "0";
    static final String PREF_SHOWCODES = "p_codes";
    static final String PREF_SORTMODE = "p_sort";
    static final String PREF_SORTMODE_CODE = "code";
    static final String PREF_SORTMODE_ALPHA = "alpha";
    static final String PREF_SORTMODE_WARNING = "interest";
    static final String PREF_SORTMODE_NAME = "name";
    static final String PREF_SORTMODE_DEFAULT = PREF_SORTMODE_WARNING;
    
    static final int CODES_PER_PAGE = 80;  // was 51

    // more global prefs
    static final String PREF_ADV = "p_adv"; // show advanced prefs?
    static final String PREF_XPATHS = "p_xpaths"; // show xpaths?
    public static final String PREF_COVLEV = "p_covlev"; // covlev
    public static final String PREF_COVTYP = "p_covtyp"; // covtyp
    //    static final String PREF_SORTMODE_DEFAULT = PREF_SORTMODE_WARNING;
    // types of data
    static final String LOCALEDISPLAYNAMES = "//ldml/localeDisplayNames/";
    static final String CURRENCIES = "currencies";
    static final String TIMEZONES = "timezones";
    static final String MEASNAMES = "measurementSystemNames";
    static final String MEASNAME = "measurementSystemName";
    public static final String NUMBERSCURRENCIES = LDMLConstants.NUMBERS + "/"+CURRENCIES;
    public static final String CURRENCYTYPE = "//ldml/"+NUMBERSCURRENCIES+"/currency[@type='";
    /**
     *  All of the data items under LOCALEDISPLAYNAMES (menu items)
     */
    static final String LOCALEDISPLAYNAMES_ITEMS[] = { 
        LDMLConstants.LANGUAGES, LDMLConstants.SCRIPTS, LDMLConstants.TERRITORIES,
        LDMLConstants.VARIANTS, LDMLConstants.KEYS, LDMLConstants.TYPES,
        CURRENCIES,
        TIMEZONES,
        MEASNAMES
    };
    public static String xMAIN = "general";
    public static String xOTHER = "misc";
    public static String xREMOVE = "REMOVE";

    public static final String GREGORIAN_CALENDAR = "gregorian calendar";
    public static final String OTHER_CALENDARS = "other calendars";
    // 
    public static final String OTHERROOTS_ITEMS[] = {
        LDMLConstants.CHARACTERS,
        LDMLConstants.NUMBERS,
        GREGORIAN_CALENDAR,
        OTHER_CALENDARS,
        "references",
        xOTHER
    };
    public static final String GREGO_XPATH = "//ldml/dates/"+LDMLConstants.CALENDARS+"/"+LDMLConstants.CALENDAR+"[@type=\"gregorian\"]";
    public static final String OTHER_CALENDARS_XPATH = "//ldml/dates/calendars/calendar";
    public static final String RAW_MENU_ITEM = "raw";
    public static final String TEST_MENU_ITEM = "test";
    
    
    public UserRegistry reg = null;
    public XPathTable   xpt = null;
    public Vetting      vet = null;
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
        new com.ibm.icu.text.SimpleDateFormat();
    
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
        if(startupTime != null) {
            String startupMsg = null;
            startupMsg = (startupTime.toString());
//            logger.info(startupMsg);
            startupTime = null;
        }
        
        if(request.getRemoteAddr().equals("66.154.103.161") ||
		   request.getRemoteAddr().equals("203.148.64.17") ) {
            response.sendRedirect("http://www.unicode.org/cldr");
        }
        
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires",0);
        response.setHeader("Pragma","no-cache");
        response.setDateHeader("Max-Age",0);
        
        // handle raw xml
        if(doRawXml(request,response)) {
            // not counted.
            xpages++;
            return; 
        }
        pages++;
        
        if((pages % 100)==0) {
            freeMem(pages,xpages);        
        }
        
        com.ibm.icu.dev.test.util.ElapsedTimer reqTimer = new com.ibm.icu.dev.test.util.ElapsedTimer();
                
        if(isBusted != null) {
            String pi = request.getParameter("sql");
            if((pi==null) || (!pi.equals(vap))) {
                response.setContentType("text/html; charset=utf-8");
                PrintWriter out = response.getWriter();
                out.println("<html>");
                out.println("<head>");
                out.println("<title>CLDR tool broken</title>");
                out.println("</head>");
                out.println("<body>");
                out.println("<h1>CLDR Survey Tool is down, because:</h1>");
                out.println("<tt>" + isBusted + "</tt><br>");
                out.println("<hr>");
                out.println("Please try this link for info: <a href='http://dev.icu-project.org/cgi-bin/cldrwiki.pl?SurveyTool/Status'>http://dev.icu-project.org/cgi-bin/cldrwiki.pl?SurveyTool/Status</a><br>");
                out.println("<hr>");
                out.println("<i>Note: to prevent thrashing, this servlet is down until someone reloads it. " + 
                            " (you are unhappy visitor #" + pages + ")</i>");

/** SRL **/
                if(false) { // dump static tables.
                    response.setContentType("application/xml; charset=utf-8");
                    WebContext xctx = new WebContext(request,response);
                    xctx.staticInfo();
                    xctx.close();
                }
/** srl **/
                return;        
            }
        }
        
        if(request.getParameter("udump") != null &&
            request.getParameter("udump").equals(vap)) {  // XML.
            response.setContentType("application/xml; charset=utf-8");
            WebContext xctx = new WebContext(request,response);
            doUDump(xctx);
            xctx.close();
            return;
        }
        
        // rest of these are HTML
        response.setContentType("text/html; charset=utf-8");

        WebContext ctx = new WebContext(request,response);
        ctx.reqTimer = reqTimer;
        // TODO: ctx.dbsrc..
        ctx.sm = this;
        
        /*
        String theIp = ctx.userIP();
        if(theIp.equals("66.154.103.161") // gigablast
          ) {
            try {
                Thread.sleep(98765);
            } catch(InterruptedException ie) {
            }
        }*/

                    
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
        printHeader(ctx, "SQL Console");
        String q = ctx.field("q");
        ctx.println("<div align='right'><a href='" + ctx.base() + "'><b>[SurveyTool main]</b></a> | "+
        "<b><a href='" + ctx.base() + "?dump="+vap+"'>Admin</a></b></div><br>");
        ctx.println("<h1>SQL Console</h1>");
        ctx.printHelpLink("/AdminSql","Help with this SQL page", true);
        ctx.println("<form method=POST action='" + ctx.base() + "'>");
        ctx.println("<input type=hidden name=sql value='" + vap + "'>");
        ctx.println("SQL: <input class='inputbox' name=q size=80 cols=80 value=\"" + q + "\"><br>");
        ctx.println("<label style='border: 1px'><input type=checkbox name=unltd>Show all?</label> ");
        ctx.println("<label style='border: 1px'><input type=checkbox name=isUpdate>U/I/D?</label> ");
        ctx.println("<label style='border: 1px'><input type=checkbox name=isUser>UserDB?</label> ");
        ctx.println("<input type=submit name=do value=Query>");
        ctx.println("</form>");
        if(q.length()>0) {
            logger.severe("Raw SQL: " + q);
            ctx.println("<hr>");
            ctx.println("query: <tt>" + q + "</tt><br><br>");
            Connection conn = null;
            Statement s = null;
            try {
                int i,j;
                
                com.ibm.icu.dev.test.util.ElapsedTimer et = new com.ibm.icu.dev.test.util.ElapsedTimer();
                
                if(ctx.field("isUser").length()>0) {
                    conn = getU_DBConnection();
                } else {
                    conn = getDBConnection();
                }
                s = conn.createStatement();
                //s.setQueryTimeout(120); // 2 minute timeout. Not supported by derby..
                if(ctx.field("isUpdate").length()>0) {
                    int rc = s.executeUpdate(q);
                    conn.commit();
                    ctx.println("<br>Result: " + rc + " row(s) affected.<br>");
                } else {
                    ResultSet rs = s.executeQuery(q); 
                    conn.commit();
                    
                    ResultSetMetaData rsm = rs.getMetaData();
                    int cc = rsm.getColumnCount();
                    
                    ctx.println("<table summary='SQL Results' class='list' border='2'><tr><th>#</th>");
                    for(i=1;i<=cc;i++) {
                        ctx.println("<th style='font-size: 50%'>"+rsm.getColumnName(i)+ "<br>");
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
                    rs.close();
                }
                
                ctx.println("elapsed time: " + et + "<br>");
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
            
            try {
                s.close();
                conn.close();
            } catch(SQLException se) {
                String complaint = "in closing: SQL err: " + unchainSqlException(se);
                
                ctx.println("<pre style='border: 1px solid red; margin: 1em; padding: 1em;'>in closing: " + complaint + "</pre>" );
                logger.severe(complaint);
            } catch(Throwable t) {
                String complaint = t.toString();
                ctx.println("<pre style='border: 1px solid red; margin: 1em; padding: 1em;'>in closing: " + complaint + "</pre>" );
                logger.severe("Err in SQL close: " + complaint);
            }
        }
        printFooter(ctx);
    }
    
    public static String freeMem() {
        Runtime r = Runtime.getRuntime();
        double total = r.totalMemory();
        total = total / 1024000.0;
        double free = r.freeMemory();
        free = free / 1024000.0;
        return "Free memory: " + free + "M / " + total + "M";
    }
    
    private static final void freeMem(int pages, int xpages) {
        System.err.println("pages: " + pages+"+"+xpages + ", "+freeMem()+".<br/>");
    }
	
	Hashtable twidHash = new Hashtable();

    boolean showToggleTwid(WebContext ctx, String pref, String what) {
		String qKey = "twidb_"+pref;
		String nVal = ctx.field(qKey);
		if(nVal.length()>0) {
			twidPut(pref,new Boolean(nVal).booleanValue());
			ctx.println("<div style='float: right;'><b class='disputed'>changed</b></div>");
		}
        boolean val = twidGetBool(pref, false);
        WebContext nuCtx = new WebContext(ctx);
        nuCtx.addQuery(qKey, new Boolean(!val).toString());
//        nuCtx.println("<div class='pager' style='float: right;'>");
        nuCtx.println("<a href='" + nuCtx.url() + "'>" + what + ": "+
            ((val)?"<span class='selected'>TRUE</span>":"<span class='notselected'>false</span>") + "</a><br>");
//        nuCtx.println("</div>");
        return val;
    }
	
	private boolean twidGetBool(String key, boolean defVal) {
        Boolean b = (Boolean)twidHash.get(key);
        if(b == null) {
            return defVal;
        } else {
            return b.booleanValue();
        }
	}
	
	void twidPut(String key, boolean val) {
		twidHash.put(key, new Boolean(val));
	}
	

	/* twiddle: these are params settable at runtime. */
    boolean twidBool(String x) {
        return twidBool(x,false);
    }
	
    synchronized boolean twidBool(String x, boolean defVal)
    {
        boolean ret = twidGetBool(x, defVal);
        twidPut(x, ret);
        return ret;
    }
    
	
    
    private void doDump(WebContext ctx)
    {
        printHeader(ctx, "ST Admin");
        ctx.println("<h1>SurveyTool Administration</h1>");
        ctx.println("<div align='right'><a href='" + ctx.base() + "'><b>[SurveyTool main]</b></a> | "+
        "<b><a href='" + ctx.base() + "?sql="+vap+"'>SQL</a></b></div><br>");
        ctx.printHelpLink("/AdminDump", "Help with this Admin page", true);
        ctx.println("<hr/>");
        
        
        String action = ctx.field("action");
        if(action.equals("")) {
            action = "sessions";
        }
        WebContext actionCtx = new WebContext(ctx);
        actionCtx.addQuery("dump",vap);
		WebContext actionSubCtx = new WebContext(actionCtx);
		actionSubCtx.addQuery("action",action);

        printMenu(actionCtx, action, "sessions", "Sessions", "action");    
            actionCtx.println(" | ");
        printMenu(actionCtx, action, "stats", "Stats", "action");       
            actionCtx.println(" | ");
        printMenu(actionCtx, action, "statics", "Statics", "action");       
            actionCtx.println(" | ");
        printMenu(actionCtx, action, "specialmsg", "Update Special Message", "action");       
            actionCtx.println(" | ");
        printMenu(actionCtx, action, "upd_src", "Manage Sources", "action");       
            actionCtx.println(" | ");
        printMenu(actionCtx, action, "load_all", "Load all locales", "action");       
            actionCtx.println(" | ");
        printMenu(actionCtx, action, "srl", "SRL-use-only", "action");  
		
        if(action.startsWith("srl")) {
            actionCtx.println(" | ");
            printMenu(actionCtx, action, "srl_vet_imp", "Update Implied Vetting", "action");       
            actionCtx.println(" | ");
            printMenu(actionCtx, action, "srl_vet_res", "Update Results Vetting", "action");       
            actionCtx.println(" | ");
            printMenu(actionCtx, action, "srl_vet_sta", "Update Vetting Status", "action");       
            actionCtx.println(" | ");
            printMenu(actionCtx, action, "srl_vet_nag", "MAIL: vet nag [weekly]", "action");       
            actionCtx.println(" | ");
            printMenu(actionCtx, action, "srl_vet_upd", "MAIL: vote change [daily]", "action");       
            actionCtx.println(" | ");
            
            printMenu(actionCtx, action, "srl_db_update", "Update <tt>base_xpath</tt>", "action");       
            actionCtx.println(" | ");
            
            printMenu(actionCtx, action, "srl_twiddle", "twiddle params", "action");       
        }
        actionCtx.println("<br>");
        
		/* Begin sub pages */
		
        if(action.equals("stats")) {
            ctx.println("<div class='pager'>");
            ctx.println("DB version " + dbInfo+ ",  ICU " + com.ibm.icu.util.VersionInfo.ICU_VERSION+"<br>");
            ctx.println(uptime + ", " + pages + " pages and "+xpages+" xml pages served.<br/>");
            Runtime r = Runtime.getRuntime();
            double total = r.totalMemory();
            total = total / 1024000.0;
            double free = r.freeMemory();
            free = free / 1024000.0;
            ctx.println("Free memory: " + free + "M / " + total + "M.<br/>");
    //        r.gc();
    //        ctx.println("Ran gc();<br/>");
            
            ctx.println("String hash has " + stringHash.size() + " items.<br/>");
            ctx.println("xString hash info: " + xpt.statistics() +"<br>");
            ctx.println("CLDRFile.distinguishedXPathStats(): " + CLDRFile.distinguishedXPathStats() + "<br>");
            ctx.println("</div>");
        } else if(action.equals("statics")) {
            ctx.println("<h1>Statics</h1>");
            ctx.staticInfo();
        } else if(action.equals("sessions"))  {
            ctx.println("<h1>Current Sessions</h1>");
            ctx.println("<table summary='User list' border=1><tr><th>id</th><th>age (h:mm:ss)</th><th>user</th><th>what</th></tr>");
            for(Iterator li = CookieSession.getAll();li.hasNext();) {
                CookieSession cs = (CookieSession)li.next();
                ctx.println("<tr><td><tt style='font-size: 72%'>" + cs.id + "</tt></td>");
                ctx.println("<td>" + timeDiff(cs.last) + "</td>");
                if(cs.user != null) {
                    ctx.println("<td>" + cs.user.email + "<br/>" + 
                                cs.user.name + "<br/>" + 
                                cs.user.org + "</td>");
                } else {
                    ctx.println("<td><i>Guest</i><br>"+cs.ip+"</td>");
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
                    ctx.println("<br><b>Removed.</b>");
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
        } else if(action.equals("srl_vet_imp")) {
            WebContext subCtx = new WebContext(ctx);
            subCtx.addQuery("dump",vap);
            ctx.println("<br>");
            ElapsedTimer et = new ElapsedTimer();
            int n = vet.updateImpliedVotes();
            ctx.println("Done updating "+n+" implied votes in: " + et + "<br>");
        } else if(action.equals("srl_vet_sta")) {
            ElapsedTimer et = new ElapsedTimer();
            int n = vet.updateStatus();
            ctx.println("Done updating "+n+" statuses [locales] in: " + et + "<br>");
		} else if(action.equals("srl_dis_nag")) {
			vet.doDisputeNag("asdfjkl;", null);
			ctx.println("\u3058\u3083\u3001\u3057\u3064\u308c\u3044\u3057\u307e\u3059\u3002<br/>");
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
            WebContext subCtx = new WebContext(ctx);
            actionCtx.addQuery("action",action);
            ctx.println("<br>");
            String what = actionCtx.field("srl_vet_res");
            if(what.equals("ALL")) {
                ctx.println("<h4>Update All</h4>");
                ElapsedTimer et = new ElapsedTimer();
                int n = vet.updateResults();
                ctx.println("Done updating "+n+" vote results in: " + et + "<br>");
            } else {
                ctx.println("All: [ <a href='"+actionCtx.url()+actionCtx.urlConnector()+"srl_vet_res=ALL'>Update all</a> ]<br>");
                if(what.length()>0) {
                    ctx.println("<h4>Update "+what+"</h4>");
                    ElapsedTimer et = new ElapsedTimer();
                    int n = vet.updateResults(what);
                    ctx.println("Done updating "+n+" vote results in: " + et + "<br>");
                }
            }
            actionCtx.println("<form method='POST' action='"+actionCtx.url()+"'>");
            actionCtx.printUrlAsHiddenFields();
            actionCtx.println("Update just: <input name='srl_vet_res' value='"+what+"'><input type='submit' value='Update'></form>");
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
            WebContext subCtx = new WebContext(ctx);
            actionCtx.addQuery("action",action);
            ctx.println("<br>");
            CLDRDBSource mySrc = makeDBSource(null, new ULocale("root"));
            localeListMap = null;
            mySrc.manageSourceUpdates(actionCtx, this); // What does this button do?
            ctx.println("<br>");
        } else if(action.equals("srl_db_update")) {
            WebContext subCtx = new WebContext(ctx);
            subCtx.addQuery("dump",vap);
            subCtx.addQuery("action","srl_db_update");
            ctx.println("<br>");
            CLDRDBSource mySrc = makeDBSource(null, new ULocale("root"));
            ElapsedTimer aTimer = new ElapsedTimer();
            mySrc.doDbUpdate(subCtx, this); // What does this button do?
            ctx.println("<br>(dbupdate took " + aTimer+")");
            ctx.println("<br>");
        } else if(action.equals("load_all")) {
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
                        ctx.println("   "+ i + "/" + nrInFiles + ". " + usedK() + "K mem used<br>");
                    }
                    try {
                        ULocale locale = new ULocale(localeName);
                        //                        WebContext xctx = new WebContext(false);
                        //                        xctx.setLocale(locale);
                        makeCLDRFile(makeDBSource(null, locale));
                    } catch(Throwable t) {
                        t.printStackTrace();
                        String complaint = ("Error loading: " + localeName + " - " + t.toString() + " ...");
                        logger.severe("loading all: " + complaint);
                        ctx.println(complaint + "<br>" + "<pre>");
                        ctx.print(t);
                        ctx.println("</pre>");
                    }
                }
            }
            logger.info("Loaded all. " + allTime);
            ctx.println("Loaded all." + allTime + "<br>");
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
            ctx.print("<input type='submit' value='set'>");
            ctx.print("</form>");
            // OGM---
        }
        
        printFooter(ctx);
    }
    
    /* 
     * print menu of stuff to 'work with' a live user session..
     */
    private void printLiveUserMenu(WebContext ctx, CookieSession cs) {
        ctx.println("<a href='" + ctx.base() + "?dump=" + vap + "&amp;see=" + cs.id + "'>" + "see" + "</a> |");
        ctx.println("<a href='" + ctx.base() + "?&amp;s=" + cs.id + "'>" + "be" + "</a> |");
        ctx.println("<a href='" + ctx.base() + "?dump=" + vap + "&amp;unlink=" + cs.id + "'>" + "kick" + "</a>");
    }
    
    static boolean showedComplaint = false;
     
    /**
    * print the header of the thing
    */
    public void printHeader(WebContext ctx, String title)
    {
        ctx.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
        ctx.println("<html>");
        ctx.println("<head>");
/*
        if(showedComplaint == false) {
            showedComplaint = true;
            System.err.println("**** noindex,nofollow is disabled");
        }
        */
        ctx.println("<meta name='robots' content='noindex,nofollow'>");
        ctx.println("<meta name=\"gigabot\" content=\"noindex\">");
        ctx.println("<meta name=\"gigabot\" content=\"noarchive\">");
        ctx.println("<meta name=\"gigabot\" content=\"nofollow\">");

        ctx.println("<link rel='stylesheet' type='text/css' href='"+ ctx.schemeHostPort()  + ctx.context("surveytool.css") + "'>");
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
            ctx.println("<div style='border: 2px solid gray; margin: 0.5em; padding: 0.5em;'>");
            ctx.printHelpLink("/BannerMessage","News",true,false);
            ctx.println(": &nbsp; ");
            ctx.println(specialHeader);
            if(specialTimer != 0) {
                long t0 = System.currentTimeMillis();
                ctx.print("<br><b>Timer:</b> ");
                if(t0 > specialTimer) {
                    ctx.print("<b>The countdown time has arrived.</b>");
                } else {
                    ctx.print("The countdown timer has " + timeDiff(t0,specialTimer) +" remaining on it.");
                }
            }
            ctx.print("<br>");
            ctx.println("</div><br>");
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
        ctx.println("<hr>");
        ctx.print("<div style='float: right; font-size: 60%;'>");
        ctx.print("<span class='notselected'>valid <a href='http://jigsaw.w3.org/css-validator/check/referer'>css</a>, "+
            "<a href='http://validator.w3.org/check?uri=referer'>html</a></span>");
        ctx.print(" \u00b7 ");
        int guests = CookieSession.nGuests;
        int users = CookieSession.nUsers;
        if((guests+users)>0) { // ??
            ctx.print("approx. ");
            if(users>0) {
                ctx.print(users+" logged in");
            }
            if(guests>0) {
                if(users>0) {
                    ctx.print(" and");
                }
                ctx.print(" "+guests+" visiting");
            }
            ctx.print(" \u00b7 ");
        }
        ctx.print("#" + pages + " served in " + ctx.reqTimer + "</div>");
        ctx.println("<a href='http://www.unicode.org'>Unicode</a> | <a href='http://www.unicode.org/cldr'>Common Locale Data Repository</a> <br/>");
        ctx.println("</body>");
        ctx.println("</html>");
    }
    
    /**
     * process the '_' parameter, if present, and set the locale.
     */
    public void setLocale(WebContext ctx)
    {
        String locale = ctx.field(QUERY_LOCALE);
        if(locale != null) {  // knock out some bad cases
            if((locale.indexOf('.') != -1) ||
               (locale.indexOf('/') != -1)) {
                locale = null;
            }
        }
        // knock out nonexistent cases.
        if(locale != null) {
            File inFiles[] = getInFiles();
            int nrInFiles = inFiles.length;
            boolean found = false;
            
            for(int i=0;(!found) && (i<nrInFiles);i++) {
                String localeName = inFiles[i].getName();
                int dot = localeName.indexOf('.');
                if(dot !=  -1) {
                    localeName = localeName.substring(0,dot);
                }
                if(localeName.equals(locale)) {
                    found = true;
                }
            }
            if(!found) {
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
        user = reg.get(password,email,ctx.userIP());
        HttpSession httpSession = ctx.request.getSession(true);
        boolean idFromSession = false;
        if(myNum.equals(SURVEYTOOL_COOKIE_NONE)) {
            httpSession.removeAttribute(SURVEYTOOL_COOKIE_SESSION);
        }
        if(user != null) {
            mySession = CookieSession.retrieveUser(user.email);
            if(mySession != null) {
                if(null == CookieSession.retrieve(mySession.id)) {
                    mySession = null; // don't allow dead sessions to show up via the user list.
                } else {
                    message = "<i>Reconnecting to your previous session.</i>";
                    myNum = mySession.id;
                }
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
                    message = message + ")</i><br>";
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
        // Can go from anon -> logged in.
        // can NOT go from one logged in account to another.
        if((mySession!=null) &&
           (mySession.user != null) &&
           (user!=null) &&
           (mySession.user.id != user.id)) {
            mySession = null; // throw it out.
        }
        
        if(mySession == null) {
            mySession = new CookieSession(user==null);
            if(user==null) {
                mySession.setIp(ctx.userIP());
            }
            if(!myNum.equals(SURVEYTOOL_COOKIE_NONE)) {
//                ctx.println("New session: " + mySession.id + "<br>");
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
            ctx.session.user.ip = ctx.userIP();
        } else {
            if( (email !=null) && (email.length()>0)) {
                message = ("<strong>login failed.</strong><br>");
            }
        }
        CookieSession.reap();
        return message;
    }
    
	//    protected void printMenu(WebContext ctx, String which, String menu, String title, String key) {

    /**
     * Print menu of choices available to the user.
     */
    public void printUserMenu(WebContext ctx) {
        if(ctx.session.user == null)  {
            ctx.println("You are a <b>Visitor</b>. <a href='" + ctx.jspLink("login.jsp") +"'>Login</a> ");
            ctx.println(" | <a href='"+ctx.url()+ctx.urlConnector()+"do=disputed"+"'>Disputed</a>");
            ctx.println(" | <a href='"+ctx.url()+ctx.urlConnector()+"do=options"+"'>My options</a>");
            ctx.println("<br>");
        } else {
                
            ctx.println("<b>Welcome " + ctx.session.user.name + " (" + ctx.session.user.org + ") !</b>");
            ctx.println("<a href='" + ctx.base() + "?do=logout'>Logout</a><br>");
            ctx.println(" <a href='"+ctx.url()+ctx.urlConnector()+"do=options"+"'>My options</a> | ");
            ctx.println(" <a href='"+ctx.url()+ctx.urlConnector()+"do=disputed"+"'>Disputed</a>");
            ctx.print(" | <a href='"+ctx.url()+ctx.urlConnector()+"do=list&"+LIST_JUST+"="+changeAtTo40(ctx.session.user.email)+
                "' >My account</a>");
            ctx.println(" | <a class='deactivated' _href='"+ctx.url()+ctx.urlConnector()+"do=mylocs"+"'>My locales</a>");
            ctx.println("<br/>");
            if(UserRegistry.userIsAdmin(ctx.session.user)) {
                ctx.println("<b>You are an Admin:</b> ");
                ctx.println("<a href='" + ctx.base() + "?dump=" + vap + "'>[Admin]</a>");
                if(ctx.session.user.id == 1) {
                    ctx.println("<a href='" + ctx.base() + "?sql=" + vap + "'>[Raw SQL]</a>");
                }
                ctx.println("<br/>");
            }
            if(UserRegistry.userIsTC(ctx.session.user)) {
                ctx.println("You are: <b>A CLDR TC Member:</b> ");
                ctx.println("<a href='" + ctx.url() + ctx.urlConnector() +"do=list'>[Manage " + ctx.session.user.org + " Users]</a>");
            } else {
                if(UserRegistry.userIsVetter(ctx.session.user)) {
                    if(UserRegistry.userIsExpert(ctx.session.user)) {
                        ctx.print("You are an: <b>Expert Vetter:</b> ");
                    } else {
                        ctx.println("You are a: <b>Vetter:</b> ");
                    }
                    ctx.println("<a href='" + ctx.url() + ctx.urlConnector() +"do=list'>[List " + ctx.session.user.org + " Users]</a>");
                } else if(UserRegistry.userIsStreet(ctx.session.user)) {
                    ctx.println("You are a: <b>Guest Contributor</b> ");
                } else if(UserRegistry.userIsLocked(ctx.session.user)) {
                    ctx.println("<b>LOCKED: Note: your account is currently locked. Please contact " + ctx.session.user.org + "'s CLDR Technical Committee member.</b> ");
                }
            }
            if(SurveyMain.phaseVetting 
                && UserRegistry.userIsStreet(ctx.session.user)
                && !UserRegistry.userIsExpert(ctx.session.user)) {
                ctx.println(" (Note: in the Vetting phase, you may not submit new data.) ");
            } else if(SurveyMain.phaseClosed || !UserRegistry.userIsTC(ctx.session.user)) {
                ctx.println("(SurveyTool is closed to vetting and data submissions.)");
            }
            ctx.println("<br/>");
        }
    }
    /**
    * Handle creating a new user
    */
    public void doNew(WebContext ctx) {
        printHeader(ctx, "New User");
        printUserMenu(ctx);
        ctx.printHelpLink("/AddModifyUser");
        ctx.println("<a href='" + ctx.url() + "'><b>SurveyTool main</b></a><hr>");
        
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
                WebContext nuCtx = new WebContext(ctx);
                nuCtx.addQuery("do","list");
                nuCtx.addQuery(LIST_JUST, changeAtTo40(new_email));
                ctx.println("<p>The password wasn't emailed to this user. You can do so in the '<b><a href='"+nuCtx.url()+"#u_"+u.email+"'>manage users</a></b>' page.</p>");
            }
        }
        
        printFooter(ctx);
    }
    
    
    private static int sqlCount(WebContext ctx, Connection conn, String sql) {
        int rv = -1;
        try {
            Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery(sql);
            if(rs.next()) {
                rv = rs.getInt(1);
            }
            rs.close();
            s.close();
        } catch ( SQLException se ) {
            String complaint = " Couldn't query count - " + SurveyMain.unchainSqlException(se) + " - " + sql;
            System.err.println(complaint);
            ctx.println("<hr><font color='red'>ERR: "+complaint+"</font><hr>");
        }
        return rv;
    }

    private static int sqlCount(WebContext ctx, Connection conn, PreparedStatement ps) {
        int rv = -1;
        try {
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                rv = rs.getInt(1);
            }
            rs.close();
        } catch ( SQLException se ) {
            String complaint = " Couldn't query count - " + SurveyMain.unchainSqlException(se) + " -  ps";
            System.err.println(complaint);
            ctx.println("<hr><font color='red'>ERR: "+complaint+"</font><hr>");
        }
        return rv;
    }

    
    
    public void doCoverage(WebContext ctx) {
        boolean showCodes = false; //ctx.prefBool(PREF_SHOWCODES);
        printHeader(ctx, "Locale Coverage");
        printUserMenu(ctx);
        ctx.println("<a href='" + ctx.jspLink("adduser.jsp") +"'>[Add User]</a> |");
        ctx.println("<a href='" + ctx.url()+ctx.urlConnector()+"do=list'>[List Users]</a>");
        ctx.print("<br>");
        ctx.printHelpLink("/LocaleCoverage");
        ctx.println("<a href='" + ctx.url() + "'><b>SurveyTool main</b></a><hr>");
        String org = ctx.session.user.org;
        if(UserRegistry.userCreateOtherOrgs(ctx.session.user)) {
            org = null; // all
        }
        File inFiles[] = getInFiles();
        int nrInFiles = inFiles.length;
        String localeList[] = new String[nrInFiles];
        Set allLocs = new HashSet();
        for(int i=0;i<nrInFiles;i++) {
            String localeName = inFiles[i].getName();
            int dot = localeName.indexOf('.');
            if(dot !=  -1) {
                localeName = localeName.substring(0,dot);
            }
            localeList[i]=localeName;
			allLocs.add(localeName);
        }

        int totalUsers = 0;
        int allUsers = 0; // users with all
        
        int totalSubmit=0;
        int totalVet=0;
        
		Map intGroups = getIntGroups();
		
        Connection conn = null;
        Map userMap = null;
        Map nullMap = null;
        Hashtable localeStatus = null;
        Hashtable nullStatus = null;
        
        if(UserRegistry.userIsTC(ctx.session.user)) {
            conn = getDBConnection();
            userMap = new TreeMap();
            nullMap = new TreeMap();
            localeStatus = new Hashtable();
            nullStatus = new Hashtable();
        }
        
        Set s = new TreeSet();
        Set badSet = new TreeSet();
        try {
			PreparedStatement psMySubmit = conn.prepareStatement("select COUNT(id) from CLDR_DATA where submitter=?",ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
			PreparedStatement psMyVet = conn.prepareStatement("select COUNT(id) from CLDR_VET where submitter=?",ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
			PreparedStatement psnSubmit = conn.prepareStatement("select COUNT(id) from CLDR_DATA where submitter=? and locale=?",ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
			PreparedStatement psnVet = conn.prepareStatement("select COUNT(id) from CLDR_VET where submitter=? and locale=?",ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);

			synchronized(reg) {
            java.sql.ResultSet rs = reg.list(org);
            if(rs == null) {
                ctx.println("<i>No results...</i>");
                return;
            }
            if(UserRegistry.userCreateOtherOrgs(ctx.session.user)) {
                org = "ALL"; // all
            }
            ctx.println("<h4>Showing coverage for: " + org + "</h4>");
            while(rs.next()) {
              //  n++;
                int theirId = rs.getInt(1);
                int theirLevel = rs.getInt(2);
                String theirName = rs.getString(3);
                String theirEmail = rs.getString(4);
                String theirOrg = rs.getString(5);
                String theirLocaleList = rs.getString(6);
                
				String nameLink =  "<a href='"+ctx.url()+ctx.urlConnector()+"do=list&"+LIST_JUST+"="+changeAtTo40(theirEmail)+
                        "' title='More on this user...'>"+theirName +" </a>";
				// setup
			
				
                if(conn!=null) {
					psMySubmit.setInt(1,theirId);
					psMyVet.setInt(1,theirId);
					psnSubmit.setInt(1,theirId);
					psnVet.setInt(1,theirId);
					
                   int mySubmit=sqlCount(ctx,conn,psMySubmit);
                    int myVet=sqlCount(ctx,conn,psMyVet);
                
                    String userInfo = "<tr><td>"+nameLink + "</td><td>" +"submits: "+ mySubmit+"</td><td>vets: "+myVet+"</td></tr>";
					if((mySubmit+myVet)==0) {
						nullMap.put(theirName, userInfo);
//						userInfo = "<span class='disabledbox' style='color:#888; border: 1px dashed red;'>" + userInfo + "</span>";
					} else {
						userMap.put(theirName, userInfo);
					}
                
                    totalSubmit+= mySubmit;
                    totalVet+= myVet;
                }
//                String theirIntLocs = rs.getString(7);
//timestamp(8)
                if((theirLevel > 10)||(theirLevel <= 1)) {
                    continue;
                }
                totalUsers++;

//                CookieSession theUser = CookieSession.retrieveUserWithoutTouch(theirEmail);
//                    ctx.println("   <td>" + UserRegistry.prettyPrintLocale(null) + "</td> ");
//                    ctx.println("    <td>" + UserRegistry.prettyPrintLocale(theirLocales) + "</td>");
                if((theirLocaleList == null) || 
                    theirLocaleList.length()==0) {
                    allUsers++;
                    continue;
                }
                String theirLocales[] = UserRegistry.tokenizeLocale(theirLocaleList);
                if((theirLocales==null)||(theirLocales.length==0)) {
                    // all.
                    allUsers++;
                } else {
//                    int hitList[] = new int[theirLocales.length]; // # of times each is used
					Set theirSet = new HashSet(); // set of locales this vetter has access to
					for(int j=0;j<theirLocales.length;j++) { 
						Set subSet = (Set)intGroups.get(theirLocales[j]); // Is it an interest group? (de, fr, ..)
						if(subSet!=null) {
							theirSet.addAll(subSet); // add all sublocs
						} else if(allLocs.contains(theirLocales[j])) {
							theirSet.add(theirLocales[j]);
						} else {
							badSet.add(theirLocales[j]);
						}
					}
					for(Iterator i = theirSet.iterator();i.hasNext();) {
						String theLocale = (String)i.next();
						s.add(theLocale);
                          //      hitList[j]++;
                               // ctx.println("user " + theirEmail + " with " + theirLocales[j] + " covers " + theLocale + "<br>");
                               
					   if(conn != null) {
							psnSubmit.setString(2,theLocale);
							psnVet.setString(2,theLocale);
							
							int nSubmit=sqlCount(ctx,conn,psnSubmit);
							int nVet=sqlCount(ctx,conn,psnVet);
							
							Hashtable theHash = localeStatus;
							if((nSubmit+nVet)==0) {
								theHash = nullStatus; // vetter w/ no work done
							}
							
							Hashtable oldStr = (Hashtable)theHash.get(theLocale);
							if(oldStr==null) {
								oldStr = new Hashtable();
								theHash.put(theLocale,oldStr);
							} else {
//                                       // oldStr = oldStr+"<br>\n";
							}
							
							String userInfo = nameLink+" ";
							if(nSubmit>0) {
								userInfo = userInfo + " submits: "+ nSubmit+" ";
							}
							if(nVet > 0) {
								userInfo = userInfo + " vets: "+nVet;
							}
							
							if((nSubmit+nVet)==0) {
//										userInfo = "<span class='disabledbox' style='color:#888; border: 1px dashed red;'>" + userInfo + "</span>";
								userInfo = "<strike>"+userInfo+"</strike>";
							}
//									userInfo = userInfo + ", file: "+theLocale+", th: " + theirLocales[j];
							oldStr.put(new Integer(theirId), userInfo + "<!-- " + theLocale + " -->");
					   }
                    }
					/*
                    for(int j=0;j<theirLocales.length;j++) {
                        if(hitList[j]==0) {
                            badSet.add(theirLocales[j]);
                        }
                    }*/
                }
            }
            // #level $name $email $org
            rs.close();
        }/*end synchronized(reg)*/ } catch(SQLException se) {
            logger.log(Level.WARNING,"Query for org " + org + " failed: " + unchainSqlException(se),se);
            ctx.println("<i>Failure: " + unchainSqlException(se) + "</i><br>");
        }

        TreeMap lm = getLocaleListMap();
        if(lm == null) {
            busted("Can't load CLDR data files from " + fileBase);
            throw new RuntimeException("Can't load CLDR data files from " + fileBase);
        }
        
        if(!badSet.isEmpty()) {
            ctx.println("<B>Warning: locale designations not matching any real locales:</b> ");
            boolean first=true;
            for(Iterator li = badSet.iterator();li.hasNext();) {
                if(first==false) {
                    ctx.print(", ");
                } else {
                    first = false;
                }
                ctx.print("<tt style='border: 1px solid gray; margin: 1px; padding: 1px;' class='codebox'>"+li.next().toString()+"</tt>" );
            }
            ctx.println("<br>");
        }

        ctx.println("Locales in <b>bold</b> have assigned vetters.<br><table summary='Locale Coverage' border=1 class='list'>");
        int n=0;
        for(Iterator li = lm.keySet().iterator();li.hasNext();) {
            n++;
            String ln = (String)li.next();
            String aLocale = (String)lm.get(ln);
            ctx.print("<tr class='row" + (n%2) + "'>");
            ctx.print(" <td nowrap valign='top'>");
            boolean has = (s.contains(aLocale));
            if(has) {
                ctx.print("<span class='selected'>");
            } else {
                ctx.print("<span class='disabledbox' style='color:#888'>");
            }
//            ctx.print(aLocale);            
            //ctx.print("<br><font size='-1'>"+new ULocale(aLocale).getDisplayName()+"</font>");
			printLocaleStatus(ctx, aLocale, ln, aLocale);
            ctx.print("</span>");

            if(showCodes) {
                ctx.println("<br><tt>" + aLocale + "</tt>");
            }
			if(localeStatus!=null && !localeStatus.isEmpty()) {
				Hashtable what = (Hashtable)localeStatus.get(aLocale);
				if(what!=null) {
					ctx.println("<ul>");
					for(Iterator i = what.values().iterator();i.hasNext();) {
						ctx.println("<li>"+i.next()+"</li>");
					}
					ctx.println("</ul>");
				}
			}
			if(nullStatus!=null && !nullStatus.isEmpty()) {
				Hashtable what = (Hashtable)nullStatus.get(aLocale);				
				if(what!=null) {
					ctx.println("<br><blockquote> <b>Did not participate:</b> ");
					for(Iterator i = what.values().iterator();i.hasNext();) {
						ctx.println(i.next().toString()	);
						if(i.hasNext()) {
							ctx.println(", ");
						}
					}
					ctx.println("</blockquote>");
				}
			}
            ctx.println(" </td>");
            
            TreeMap sm = (TreeMap)subLocales.get(aLocale);
            
            ctx.println("<td valign='top'>");
            int j = 0;
            for(Iterator si = sm.keySet().iterator();si.hasNext();) {
                String sn = (String)si.next();
                String subLocale = (String)sm.get(sn);
                if(subLocale.length()>0) {

					has = (s.contains(subLocale));

					if(j>0) { 
						if(localeStatus==null) {
							ctx.println(", ");
						} else {
							ctx.println("<br>");
						}
					}

                    if(has) {
                        ctx.print("<span class='selected'>");
                    } else {
                        ctx.print("<span class='disabledbox' style='color:#888'>");
                    }
                  //  ctx.print(subLocale);           
//                    ctx.print("&nbsp;<font size='-1'>("+new ULocale(subLocale).getDisplayName()+")</font>");
    
					printLocaleStatus(ctx, subLocale, sn, subLocale);
                   // if(has) {
                        ctx.print("</span>");
                   // }


//                    printLocaleLink(baseContext, subLocale, sn);
                    if(showCodes) {
                        ctx.println("&nbsp;-&nbsp;<tt>" + subLocale + "</tt>");
                    }
					if(localeStatus!=null&&!nullStatus.isEmpty()) {
						Hashtable what = (Hashtable)localeStatus.get(subLocale);
						if(what!=null) {
							ctx.println("<ul>");
							for(Iterator i = what.values().iterator();i.hasNext();) {
								ctx.println("<li>"+i.next()+"</li>");
							}
							ctx.println("</ul>");
						}
					}
					if(nullStatus!=null && !nullStatus.isEmpty()) {
						Hashtable what = (Hashtable)nullStatus.get(subLocale);				
						if(what!=null) {
							ctx.println("<br><blockquote><b>Did not participate:</b> ");
							for(Iterator i = what.values().iterator();i.hasNext();) {
								ctx.println(i.next().toString()	);
								if(i.hasNext()) {
									ctx.println(", ");
								}
							}
							ctx.println("</blockquote>");
						}
					}
                }
                j++;
            }
            ctx.println("</td>");
            ctx.println("</tr>");
        }
        ctx.println("</table> ");
        ctx.println(totalUsers + "  users, including " + allUsers + " with 'all' privs (not counted against the locale list)<br>");
    
        if(conn!=null) {
            int totalResult=sqlCount(ctx,conn,"select COUNT(*) from CLDR_RESULT");
            int totalData=sqlCount(ctx,conn,"select COUNT(id) from CLDR_DATA");
            ctx.println("These users have submitted " + totalSubmit + " items, and voted for " + totalVet + " items (including implied votes).<br>");
            ctx.println("In all, the SurveyTool has " + totalData + " items (including proposed) and " + totalResult + " items that may need voting.<br>");
			
			ctx.println("<hr>");
			ctx.println("<h4>Participated: "+userMap.size()+"</h4><table border='1'>");
			for(Iterator i = userMap.values().iterator();i.hasNext();) {
				String which = (String)i.next();
				ctx.println(which);
			}
			ctx.println("</table><h4>Did Not Participate at all: "+nullMap.size()+"</h4><table border='1'>");
			for(Iterator i = nullMap.values().iterator();i.hasNext();) {
				String which = (String)i.next();
				ctx.println(which);
			}
			ctx.println("</table>");

		}

        printFooter(ctx);
    }
    
    /**
    * User list management
    */
    static final String LIST_ACTION_SETLEVEL = "set_userlevel_";
    static final String LIST_ACTION_NONE = "-";
    static final String LIST_ACTION_SHOW_PASSWORD = "showpassword_";
    static final String LIST_ACTION_SEND_PASSWORD = "sendpassword_";
    static final String LIST_ACTION_SETLOCALES = "set_locales_";
    static final String LIST_ACTION_DELETE0 = "delete0_";
    static final String LIST_ACTION_DELETE1 = "delete_";
    static final String LIST_JUST = "justu";
    static final String LIST_MAILUSER = "mailthem";
    static final String LIST_MAILUSER_WHAT = "mailthem_t";
    static final String LIST_MAILUSER_CONFIRM = "mailthem_c";
    
    public static final String changeAtTo40(String s) {
        return s.replaceAll("@","%40");
    }
    
    public static final String change40ToAt(String s) {
        return s.replaceAll("%40","@");
    }

    public void doUDump(WebContext ctx) {
        ctx.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
//        ctx.println("<!DOCTYPE ldml SYSTEM \"http://.../.../stusers.dtd\">");
        ctx.println("<users host=\""+ctx.serverHostport()+"\">");
        String org = null;
        try { synchronized(reg) {
            java.sql.ResultSet rs = reg.list(org);
            if(rs == null) {
                ctx.println("\t<!-- No results -->");
                return;
            }
            while(rs.next()) {
                int theirId = rs.getInt(1);
                int theirLevel = rs.getInt(2);
                String theirName = rs.getString(3);
                String theirEmail = rs.getString(4);
                String theirOrg = rs.getString(5);
                String theirLocales = rs.getString(6);
                
                ctx.println("\t<user id=\""+theirId+"\" email=\""+theirEmail+"\">");
                ctx.println("\t\t<level n=\""+theirLevel+"\" type=\""+UserRegistry.levelAsStr(theirLevel)+"\"/>");
                ctx.println("\t\t<name>"+theirName+"</name>");
                ctx.println("\t\t<org>"+theirOrg+"</org>");
                ctx.println("\t\t<locales type=\"edit\">");
                String theirLocalesList[] = UserRegistry.tokenizeLocale(theirLocales);
                for(int i=0;i<theirLocalesList.length;i++) {
                    ctx.println("\t\t\t<locale id=\""+theirLocalesList[i]+"\"/>");
                }
                ctx.println("\t\t</locales>");
                ctx.println("\t</user>");
            }            
        }/*end synchronized(reg)*/ } catch(SQLException se) {
            logger.log(Level.WARNING,"Query for org " + org + " failed: " + unchainSqlException(se),se);
            ctx.println("<!-- Failure: " + unchainSqlException(se) + " -->");
        }
        ctx.println("</users>");
    }
    
    public void doList(WebContext ctx) {
        int n=0;
        String just = ctx.field(LIST_JUST);
        boolean justme = false; // "my account" mode
        if(just.length()==0) {
            just = null;
        } else {
            just = change40ToAt(just);
            justme = ctx.session.user.email.equals(just);
        }
        if(justme) {
            printHeader(ctx, "My Account");
        } else {
            printHeader(ctx, "List Users" + ((just==null)?"":(" - " + just)));
        }
        printUserMenu(ctx);
        if(reg.userCanCreateUsers(ctx.session.user)) {
            ctx.println("<a href='" + ctx.jspLink("adduser.jsp") +"'>[Add User]</a> |");
        }
        ctx.println("<a href='" + ctx.url()+ctx.urlConnector()+"do=coverage'>[Locale Coverage Reports]</a>");
        ctx.print("<br>");
        ctx.printHelpLink("/AddModifyUser");
        ctx.println("<a href='" + ctx.url() + "'><b>SurveyTool main</b></a><hr>");
        String org = ctx.session.user.org;
        if(just!=null) {
            ctx.println("<a href='"+ctx.url()+ctx.urlConnector()+"do=list'>\u22d6 Show all users</a><br>");
        }
        if(UserRegistry.userCreateOtherOrgs(ctx.session.user)) {
            org = null; // all
        }
        String cleanEmail = null;
        String sendWhat = ctx.field(LIST_MAILUSER_WHAT);
        boolean areSendingMail = false;
		boolean didConfirmMail = false;
		boolean areSendingDisp = (ctx.field(LIST_MAILUSER+"_d").length())>0;
        String mailBody = null;
        String mailSubj = null;
        if(UserRegistry.userCanEmailUsers(ctx.session.user)) {
            cleanEmail = ctx.session.user.email;
            if(cleanEmail.equals("admin@")) {
                cleanEmail = "surveytool@unicode.org";
            }
            if(ctx.field(LIST_MAILUSER_CONFIRM).equals(cleanEmail)) {
                ctx.println("<h4>begin sending mail...</h4>");
				didConfirmMail=true;
                mailBody = "Message from " + getRequester(ctx) + ":\n--------\n"+sendWhat+
                    "\n--------\n\nSurvey Tool: http://" + ctx.serverHostport() + ctx.base()+"\n\n";
                mailSubj = "CLDR SurveyTool message from " +getRequester(ctx);
				if(!areSendingDisp) {
					areSendingMail= true;
				}
            }
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
            if(justme) {
                ctx.println("<h2>My Account</h2>");
            } else {
                ctx.println("<h2>Users for " + org + "</h2>");
                if(UserRegistry.userCanModifyUsers(ctx.session.user)) {
                    ctx.println("<div class='warning' style='border: 1px solid black; padding: 1em; margin: 1em'><b><font size='+2'>NOTE: Changing user level or locales while a user is active (below) will result in  " +
                                " destruction of their session, don't do that if they have been working recently.</font></b></div>");
                }
            }
            // Preset box
            boolean preFormed = false;
            
            if(/*(just==null) 
                  &&*/ UserRegistry.userCanModifyUsers(ctx.session.user) && !justme) {
                ctx.println("<div class='pager' style='align: right; float: right; margin-left: 4px;'>");
                ctx.println("<form method=POST action='" + ctx.base() + "'>");
                ctx.printUrlAsHiddenFields();
                ctx.println("Set menus:<br><label>all ");
                ctx.println("<select name='preset_from'>");
                ctx.println("   <option>" + LIST_ACTION_NONE + "</option>");
                for(int i=0;i<UserRegistry.ALL_LEVELS.length;i++) {
                    ctx.println("<option class='user" + UserRegistry.ALL_LEVELS[i] + "' ");
                    ctx.println(" value='"+UserRegistry.ALL_LEVELS[i]+"'>" + UserRegistry.levelToStr(ctx, UserRegistry.ALL_LEVELS[i]) + "</option>");
                }
                ctx.println("</select></label> <br>");
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
                ctx.println("</select></label> <br>");
                if(just!=null) {
                    ctx.print("<input type='hidden' name='"+LIST_JUST+"' value='"+just+"'>");
                }
                ctx.println("<input type='submit' name='do' value='list'></form>");
                if((ctx.field("preset_from").length()>0)&&!ctx.field("preset_from").equals(LIST_ACTION_NONE)) {
                    ctx.println("<hr><i><b>Menus have been pre-filled. <br> Confirm your choices and click Change.</b></i>");
                    ctx.println("<form method=POST action='" + ctx.base() + "'>");
                    ctx.println("<input type='submit' name='doBtn' value='Change'>");
                    preFormed=true;
                }
                ctx.println("</div>");
            }
            int preset_fromint = ctx.fieldInt("preset_from", -1);
            String preset_do = ctx.field("preset_do");
            if(preset_do.equals(LIST_ACTION_NONE)) {
                preset_do="nothing";
            }
            if(/*(just==null)&& */((UserRegistry.userCanModifyUsers(ctx.session.user))) &&
               !preFormed) { // form was already started, above
                ctx.println("<form method=POST action='" + ctx.base() + "'>");
            }
            if(just!=null) {
                ctx.print("<input type='hidden' name='"+LIST_JUST+"' value='"+just+"'>");
            }
            if(justme || UserRegistry.userCanModifyUsers(ctx.session.user)) {
                ctx.printUrlAsHiddenFields();
                ctx.println("<input type='hidden' name='do' value='list'>");
                ctx.println("<input type='submit' name='doBtn' value='Change'>");
            }
            ctx.println("<table summary='User List' class='userlist' border='2'>");
            ctx.println(" <tr><th></th><th>Organization / Level</th><th>Name/Email</th><th>Locales</th></tr>");
            while(rs.next()) {
                int theirId = rs.getInt(1);
                int theirLevel = rs.getInt(2);
                String theirName = rs.getString(3);
                String theirEmail = rs.getString(4);
                String theirOrg = rs.getString(5);
                String theirLocales = rs.getString(6);                
                String theirIntlocs = rs.getString(7);
                java.sql.Timestamp theirLast = rs.getTimestamp(8);
                boolean havePermToChange = UserRegistry.userCanModifyUser(ctx.session.user, theirId, theirLevel);
                String theirTag = theirId + "_" + theirEmail; // ID+email - prevents stale data. (i.e. delete of user 3 if the rows change..)
                String action = ctx.field(theirTag);
                CookieSession theUser = CookieSession.retrieveUserWithoutTouch(theirEmail);
                
                if(just!=null && !just.equals(theirEmail)) {
                    continue;
                }
                n++;
                
                ctx.println("  <tr class='user" + theirLevel + "'>");
                
            
                if(areSendingMail && (theirLevel < UserRegistry.LOCKED) ) {
                    ctx.print("<td class='framecell'>");
                    mailUser(ctx,theirEmail,mailSubj,mailBody);
                    ctx.println("</td>");
                }
                // first:  DO.
                
                if(havePermToChange) {  // do stuff
                    
                    String msg = null;
                    if(ctx.field(LIST_ACTION_SETLOCALES + theirTag).length()>0) {
                        ctx.println("<td class='framecell' >");
                        String newLocales = ctx.field(LIST_ACTION_SETLOCALES + theirTag);
                        msg = reg.setLocales(ctx, theirId, theirEmail, newLocales);
                        ctx.println(msg);
                        theirLocales = newLocales; // MODIFY
                        if(theUser != null) {
                            ctx.println("<br/><i>Logging out user session " + theUser.id + " and deleting all unsaved changes</i>");
                            theUser.remove();
                        }
                        ctx.println("</td>");
                    } else if((action!=null)&&(action.length()>0)&&(!action.equals(LIST_ACTION_NONE))) { // other actions
                        ctx.println("<td class='framecell'>");
                        
                        // check an explicit list. Don't allow random levels to be set.
                        for(int i=0;i<UserRegistry.ALL_LEVELS.length;i++) {
                            if(action.equals(LIST_ACTION_SETLEVEL + UserRegistry.ALL_LEVELS[i])) {
                                msg = reg.setUserLevel(ctx, theirId, theirEmail, UserRegistry.ALL_LEVELS[i]);
                                ctx.println("Setting to level " + UserRegistry.levelToStr(ctx, UserRegistry.ALL_LEVELS[i]));
                                ctx.println(": " + msg);
                                theirLevel = UserRegistry.ALL_LEVELS[i];
                                if(theUser != null) {
                                    ctx.println("<br/><i>Logging out user session " + theUser.id + "</i>");
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
                            ctx.println("Ensure that 'confirm delete' is chosen at right and click Change again to delete..");
                        } else if((UserRegistry.userCanDeleteUser(ctx.session.user,theirId,theirLevel)) && (action.equals(LIST_ACTION_DELETE1))) {
                            msg = reg.delete(ctx, theirId, theirEmail);
                            ctx.println("<strong style='font-color: red'>Deleting...</strong><br>");
                            ctx.println(msg);
                        } else if((UserRegistry.userCanModifyUser(ctx.session.user,theirId,theirLevel)) && (action.equals(LIST_ACTION_SETLOCALES))) {
                            if(theirLocales == null) {
                                theirLocales = "";
                            }
                            ctx.println("<label>Locales: (space separated) <input name='" + LIST_ACTION_SETLOCALES + theirTag + "' value='" + theirLocales + "'></label>"); 
                        }
                        // ctx.println("Change to " + action);
                    } else {
                        ctx.print("<td>");
                    }
                } else {
                    ctx.print("<td>");
                }
                
                if(just==null) {
                    ctx.print("<a href='"+ctx.url()+ctx.urlConnector()+"do=list&"+LIST_JUST+"="+changeAtTo40(theirEmail)+
                        "' title='More on this user...'>\u22d7</a>");
                }
                ctx.println("</td>");
                
                // org, level
                ctx.println("    <td>" + theirOrg + "<br>" +
                    "&nbsp; <span style='font-size: 80%' align='right'>" + UserRegistry.levelToStr(ctx,theirLevel).replaceAll(" ","&nbsp;") + "</span></td>");

                ctx.println("    <td valign='top'><font size='-1'>#" + theirId + " </font> <a name='u_"+theirEmail+"'>" +  theirName + "</a>");                
                ctx.println("    <a href='mailto:" + theirEmail + "'>" + theirEmail + "</a>");
                if(havePermToChange) {
                    // Was something requested?
                    ctx.println("<br>");
                    
                    { // PRINT MENU
                        ctx.print("<select name='" + theirTag + "'>");
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
                    } // end menu
                }
                ctx.println("</td>");
                
                if(theirLevel <= UserRegistry.TC) {
                    ctx.println("   <td>" + UserRegistry.prettyPrintLocale(null) + "</td> ");
                } else {
                    ctx.println("    <td>" + UserRegistry.prettyPrintLocale(theirLocales) + "</td>");
                }
                
                // are they logged in?
                if((theUser != null) && UserRegistry.userCanModifyUsers(ctx.session.user)) {
                    ctx.println("<td>");
                    ctx.println("<b>Active " + timeDiff(theUser.last) + " ago</b>");
                    if(UserRegistry.userIsAdmin(ctx.session.user)) {
                        ctx.print("<br/>");
                        printLiveUserMenu(ctx, theUser);
                    }
                    ctx.println("</td>");
                } else if(theirLast != null) {
                    ctx.println("<td>");
                    ctx.println("<b>Last Login:" + timeDiff(theirLast.getTime()) + " ago</b>");
                    ctx.print("<br/><font size='-2'>");
                    ctx.print(theirLast.toString());
                    ctx.println("</font></td>");
                }
                
                ctx.println("  </tr>");
            }
            ctx.println("</table>");
            if(!justme) {
                ctx.println("<div style='font-size: 70%'>Number of users shown: " + n +"</div><br>");
            }
            if(!justme && UserRegistry.userCanModifyUsers(ctx.session.user)) {
                if((n>0) && UserRegistry.userCanEmailUsers(ctx.session.user)) { // send a mass email to users
                    if(ctx.field(LIST_MAILUSER).length()==0) {
                        ctx.println("<label><input type='checkbox' value='y' name='"+LIST_MAILUSER+"'>Check this box to mass-mail these " + n + " users (except locked).</label>");
                    } else {
                        ctx.println("<p><div class='pager'>");
                        ctx.println("<h4>Mailing "+n+" users</h4>");
                        if(didConfirmMail) {
							if(areSendingDisp) {
								int nm = vet.doDisputeNag(mailBody,UserRegistry.userIsAdmin(ctx.session.user)?null:ctx.session.user.org);
								ctx.println("<b>dispute note sent: " + nm + " emails sent.</b><br>");
							} else {
								ctx.println("<b>Mail sent.</b><br>");
							}
                        } else { // dont' allow resend option
							if(sendWhat.length()>0) {
								ctx.println("<label><input type='checkbox' value='y' name='"+LIST_MAILUSER+"_d'>Check this box to send a Dispute complaint to your vetters. (Otherwise it is just a normal email.)</label><br>");
							} else {
								ctx.println("<i>On the next page you will be able to choose a Dispute Report.</i><br>");
							}
                            ctx.println("<input type='hidden' name='"+LIST_MAILUSER+"' value='y'>");
                        }
                        ctx.println("From: "+cleanEmail+"<br>");
                        if(sendWhat.length()>0) {
                            ctx.println("<div style='border: 3px dashed olive; margin: 1em; padding: 1em;'>"+
                                TransliteratorUtilities.toHTML.transliterate(sendWhat).replaceAll("\n","<br>")+
                                "</div>");
                            if(!didConfirmMail) {
                                ctx.println("<input type='hidden' name='"+LIST_MAILUSER_WHAT+"' value='"+
                                        sendWhat.replaceAll("&","&amp;").replaceAll("'","&quot;")+"'>");
                                if(!ctx.field(LIST_MAILUSER_CONFIRM).equals(cleanEmail) && (ctx.field(LIST_MAILUSER_CONFIRM).length()>0)) {
                                    ctx.println("<strong>That email didn't match. Try again.</strong><br>");
                                }
                                ctx.println("To confirm sending, type your email address (just as it is above): <input name='"+LIST_MAILUSER_CONFIRM+
                                    "'>");
                            }
                        } else {
                            ctx.println("<textarea NAME='"+LIST_MAILUSER_WHAT+"' id='body' ROWS='15' COLS='85' style='width:100%'></textarea>");
                        }
                        ctx.println("</div>");
                    }
                    
                    
                }
            }
            // #level $name $email $org
            rs.close();
            
            // more 'My Account' stuff
            if(justme) {
                ctx.println("<hr>");
                // Is the 'interest locales' list relevant?
                String mainLocs[] = UserRegistry.tokenizeLocale(ctx.session.user.locales);
                if(mainLocs.length == 0) {
                    boolean intlocs_change = (ctx.field("intlocs_change").length()>0);
                                
                    ctx.println("<h4>Notify me about these locale groups:</h4>");
                    
                    if(intlocs_change) {
                        if(ctx.field("intlocs_change").equals("t")) {
                            String newIntLocs = ctx.field("intlocs").trim();
                            String msg = reg.setLocales(ctx, ctx.session.user.id, ctx.session.user.email, newIntLocs, true);
                            
                            if(msg == null) {
                                ctx.println("Interest Locales set.<br>");
                                ctx.session.user.intlocs = newIntLocs;
                            } else {
                                ctx.println(msg);
                            }
                        }
                    
                    
                        ctx.println("<input type='hidden' name='intlocs_change' value='t'>");
                        ctx.println("<input name='intlocs' ");
                        if(ctx.session.user.intlocs != null) {
                            ctx.println("value='"+ctx.session.user.intlocs.trim()+"' ");
                        }
                        ctx.println("</input>");
                        if(ctx.session.user.intlocs == null) {
                            ctx.println("<br><i>List languages only, separated by spaces.  Example: <tt class='codebox'>en fr zh</tt>. leave blank for 'all locales'.</i>");
                        }
                        //ctx.println("<br>Note: changing interest locales is currently unimplemented. Check back later.<br>");
                    }
                    
                    ctx.println("<ul><tt class='codebox'>"+UserRegistry.prettyPrintLocale(ctx.session.user.intlocs) + "</tt>");
                    if(!intlocs_change) {
                        ctx.print("<a href='"+ctx.url()+ctx.urlConnector()+"do=list&"+LIST_JUST+"="+changeAtTo40(ctx.session.user.email)+
                            "&intlocs_change=b' >[Change this]</a>");
                    }
                    ctx.println("</ul>");
                    
                } // end intlocs
                ctx.println("<br>");
                ctx.println("<input type='submit' disabled value='Reset Password'>");
            }
            if(justme || UserRegistry.userCanModifyUsers(ctx.session.user)) {
                ctx.println("<br>");
                ctx.println("<input type='submit' name='doBtn' value='Change'>");
                ctx.println("</form>");
            }
        }/*end synchronized(reg)*/ } catch(SQLException se) {
            logger.log(Level.WARNING,"Query for org " + org + " failed: " + unchainSqlException(se),se);
            ctx.println("<i>Failure: " + unchainSqlException(se) + "</i><br>");
        }
        if(just!=null) {
            ctx.println("<a href='"+ctx.url()+ctx.urlConnector()+"do=list'>\u22d6 Show all users</a><br>");
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

    boolean showTogglePref(WebContext ctx, String pref, String what) {
        boolean val = ctx.prefBool(pref);
        WebContext nuCtx = new WebContext(ctx);
        nuCtx.addQuery(pref, !val);
//        nuCtx.println("<div class='pager' style='float: right;'>");
        nuCtx.println("<a href='" + nuCtx.url() + "'>" + what + ": "+
            ((val)?"<span class='selected'>Yes</span>":"<span class='notselected'>No</span>") + "</a><br>");
//        nuCtx.println("</div>");
        return val;
    }
    String showListPref(WebContext ctx, String pref, String what, String[] list) {
        return showListPref(ctx,pref,what,list,false);
    }
    String showListPref(WebContext ctx, String pref, String what, String[] list, boolean doDef) {
        String val = ctx.pref(pref, doDef?"default":list[0]);
        ctx.println("<b>"+what+"</b>: ");
//        ctx.println("<select name='"+pref+"'>");
        if(doDef) {
            WebContext nuCtx = new WebContext(ctx);
            nuCtx.addQuery(pref, "default");
            ctx.println("<a href='"+nuCtx.url()+"' class='"+(val.equals("default")?"selected":"notselected")+"'>"+"default"+"</a> ");
        }
        for(int n=0;n<list.length;n++) {
//            ctx.println("    <option " + (val.equals(list[n])?" SELECTED ":"") + "value='" + list[n] + "'>"+list[n] +"</option>");
            WebContext nuCtx = new WebContext(ctx);
            nuCtx.addQuery(pref, list[n]);
            ctx.println("<a href='"+nuCtx.url()+"' class='"+(val.equals(list[n])?"selected":"notselected")+"'>"+list[n]+"</a> ");
        }
//    ctx.println("</select></label><br>");
        ctx.println("<br>");
        return val;
    }
    public static final String PREF_COVLEV_LIST[] = { "default","comprehensive","modern","moderate","basic" };
	
	void doDisputed(WebContext ctx){
		if(ctx.session.user != null) {
			printHeader(ctx, "Disputed Items Page");
			printUserMenu(ctx);
		}
        ctx.printHelpLink("/DisputedItems");
        
        ctx.println("<a href='"+ctx.url()+"'>Return to SurveyTool</a><hr>");
        ctx.addQuery("do","disputed");
        ctx.println("<h2>DisputedItems</h2>");

		vet.doDisputePage(ctx);
        
        printFooter(ctx);
    } 
	
    void doOptions(WebContext ctx) {
		if(ctx.session.user != null) {
			printHeader(ctx, "My Options");
			printUserMenu(ctx);
		}
        ctx.printHelpLink("/MyOptions");
        
        ctx.println("<a href='"+ctx.url()+"'>Return to SurveyTool</a><hr>");
        ctx.addQuery("do","options");
        ctx.println("<h2>My Options</h2>");
        ctx.println("<h4>Advanced Options</h4>");
        boolean adv = showTogglePref(ctx, PREF_ADV, "Show Advanced Options");

        showTogglePref(ctx, PREF_XPATHS, "Show full XPaths");

        ctx.println("<br>");
        String lev = showListPref(ctx, PREF_COVLEV, "Coverage Level", PREF_COVLEV_LIST);

        if(lev.equals("default")) {
            ctx.print("&nbsp;");
            ctx.print("&nbsp;");
            showListPref(ctx,PREF_COVTYP, "Coverage Type", ctx.getLocaleTypes(), true);
        }
        ctx.println("(Current effective coverage level: <tt class='codebox'>" + ctx.defaultPtype()+"</tt>)<p>");
        
        printFooter(ctx);
    }
    
    public void doSession(WebContext ctx)
    {
        // which 
        String which = ctx.field(QUERY_SECTION);
        
        setLocale(ctx);
        
        String sessionMessage = setSession(ctx);
        
        
        // TODO: untangle this
        // admin things
        if((ctx.session.user != null) && (ctx.field("do").length()>0)) {
            String doWhat = ctx.field("do");
            if(doWhat.equals("list")  && (UserRegistry.userCanDoList(ctx.session.user))  ) {
                doList(ctx);
            } else if(doWhat.equals("coverage")  && (UserRegistry.userCanDoList(ctx.session.user))  ) {
                doCoverage(ctx);
            } else if(doWhat.equals("new") && (UserRegistry.userCanCreateUsers(ctx.session.user)) ) {
                doNew(ctx);
            } else if(doWhat.equals("logout")) {
                ctx.session.remove(); // zap!
                HttpSession httpSession = ctx.request.getSession(false);
                if(httpSession != null) {
                    httpSession.removeAttribute(SURVEYTOOL_COOKIE_SESSION);
                }

                try {
                    ctx.response.sendRedirect(ctx.jspLink("?logout=1"));
                    ctx.out.close();
                    ctx.close();
                } catch(IOException ioe) {
                    throw new RuntimeException(ioe.toString() + " while redirecting to logout");
                }
                return;
            } else if(doWhat.equals("options")) {
                doOptions(ctx);
            } else if(doWhat.equals("disputed")) {
                doDisputed(ctx);
            } else {
                printHeader(ctx,doWhat + "?");
                ctx.println("<i>some error, try hitting the Back button.</i>");
                printFooter(ctx);
            }
            return;
        } else { // non-user options
            //
        }
        
        String title = " " + which;
        if(ctx.field(QUERY_EXAMPLE).length()!=0)  {
            title = title + " Example"; 
        }
        printHeader(ctx, title);
        if(sessionMessage != null) {
            ctx.println(sessionMessage);
        }
        
        WebContext baseContext = new WebContext(ctx);
        
        // print 'shopping cart'
        if(ctx.field(QUERY_EXAMPLE).length()==0)  {
            if(ctx.session.user != null) {
                ctx.println("<form name='"+STFORM+"' method=POST action='" + ctx.base() + "'>");
            }
            ctx.println("<table summary='header' border='0' cellpadding='0' cellspacing='0' style='border-collapse: collapse' "+
                        " width='100%' bgcolor='#EEEEEE'>"); //bordercolor='#111111'
            ctx.println("<tr><td>");
                ctx.printHelpLink("","General&nbsp;Instructions"); // base help
                if((which.length()==0) && (ctx.locale!=null)) {
                    which = xMAIN;
                }
                if(which.length()>0) {
                    ctx.println(" | ");
                    ctx.printHelpLink("/"+which.replaceAll(" ","_"),"Page&nbsp;Instructions"); // base help
                }
            ctx.println("</td><td align='right'>");
                printUserMenu(ctx);
            ctx.println("</td></tr></table>");

            
            Hashtable lh = ctx.session.getLocales();
            Enumeration e = lh.keys();
            if(e.hasMoreElements()) { 
                ctx.println("<p align='center'><B>Visited locales: </B> ");
                for(;e.hasMoreElements();) {
                    String k = e.nextElement().toString();
                    boolean canModify = UserRegistry.userCanModifyLocale(ctx.session.user,k);
                    ctx.print("<a href=\"" + baseContext.url() + ctx.urlConnector() + QUERY_LOCALE+"=" + k + "\">" + 
                                new ULocale(k).getDisplayName());
                    if(canModify) {
                        ctx.print(modifyThing(ctx));
                    }
                    ctx.println("</a> ");
                }
                
            /*   if(  UserRegistry.userCanSubmit(ctx.session.user) ) {
                    ctx.println("<div>");
                    ctx.println("Your changes are <b><font color=red>not</font></b> permanently saved until you: ");
                    ctx.println("<input name=submit type=submit value='" + xREVIEW +"'>");
                    ctx.println("</div>");
                }*/
                ctx.println("</p>");
            }
            ctx.println("<hr/>");
        }
        
        String doWhat = ctx.field("do");
        if(doWhat.equals("options")) {
            doOptions(ctx);
		} else if(doWhat.equals("disputed")) {
			doDisputed(ctx);
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
    
    void printLocaleStatus(WebContext ctx, String localeName, String str, String explanation) {
        ctx.print(getLocaleStatus(localeName,str,explanation));
    }
    
    String getLocaleStatus(String localeName, String str, String explanation) {
        String rv = "";
        int s = vet.status(localeName);
        if(s==-1) {
            if(explanation.length()>0) {
                rv = rv + ("<span title='"+explanation+"'>");
            }
            rv = rv + (str);
            if(explanation.length()>0) {
                rv = rv + ("</span>");
            }
            return rv;
        }
        if((s&Vetting.RES_DISPUTED)>0) {
            rv = rv + ("<span style='margin: 1px; padding: 1px;' class='disputed'>");
            if(explanation.length() >0) {
                explanation = explanation + ", ";
            }
            explanation = explanation + "disputed";
        } else {
			if ((s&(Vetting.RES_INSUFFICIENT|Vetting.RES_NO_VOTES))>0) {
				rv = rv + ("<span style='margin: 1px; padding: 1px;' class='insufficient'>");
				if(explanation.length() >0) {
					explanation = explanation + ", ";
				}
				explanation = explanation + "insufficient votes";
			}
		}
        if(explanation.length()>0) {
            rv = rv + ("<span title='"+explanation+"'>");
        }
        rv = rv + (str);
        if(explanation.length()>0) {
            rv = rv + ("</span>");
        }
        if((s&Vetting.RES_DISPUTED)>0) {
            rv = rv + ("</span>");
        } else if ((s&(Vetting.RES_INSUFFICIENT|Vetting.RES_NO_VOTES))>0) {
            rv = rv + ("</span>");
        }
        return rv;
    }
    
    void printLocaleLink(WebContext ctx, String localeName, String n) {
        if(n == null) {
            n = new ULocale(localeName).getDisplayName() ;
        }
        String connector = ctx.urlConnector();
//        boolean hasDraft = draftSet.contains(localeName);
//        ctx.print(hasDraft?"<b>":"") ;
        ctx.print("<a "  /* + (hasDraft?"class='draftl'":"class='nodraft'")  */
                  +" title='" + localeName + "' href=\"" + ctx.url() 
                  + connector + QUERY_LOCALE+"=" + localeName + "\">");
        printLocaleStatus(ctx, localeName, n, localeName);
        boolean canModify = UserRegistry.userCanModifyLocale(ctx.session.user,localeName);
        if(canModify) {
            ctx.print(modifyThing(ctx));
        }
        ctx.print("</a>");
//        ctx.print(hasDraft?"</b>":"") ;
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
				
        ctx.println(SLOW_PAGE_NOTICE);
        TreeMap lm = getLocaleListMap();
        if(lm == null) {
            busted("Can't load CLDR data files from " + fileBase);
            throw new RuntimeException("Can't load CLDR data files from " + fileBase);
        }
        ctx.println("<table summary='Locale List' border=1 class='list'>");
        int n=0;
        for(Iterator li = lm.keySet().iterator();li.hasNext();) {
            n++;
            String ln = (String)li.next();
            String aLocale = (String)lm.get(ln);
            ctx.print("<tr class='row" + (n%2) + "'>");
            ctx.print(" <td nowrap valign='top'>");
            printLocaleLink(baseContext, aLocale, ln);
            if(showCodes) {
                ctx.println("<br><tt>" + aLocale + "</tt>");
            }
            ctx.println(" </td>");
            
            TreeMap sm = (TreeMap)subLocales.get(aLocale);
            
            ctx.println("<td valign='top'>");
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
//        ctx.println("(Locales containing draft items are shown in <b><a class='draftl' href='#'>bold</a></b>)<br/>");
// TODO: reenable this        
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
        if(ctx.session.user != null) {
            ctx.println("</form>");
        }
        printFooter(ctx);
    }
    
    protected void printMenu(WebContext ctx, String which, String menu) {
        printMenu(ctx,which,menu,menu, QUERY_SECTION);
    }
    protected void printMenuWithAnchor(WebContext ctx, String which, String menu, String anchor) {
        printMenu(ctx,which,menu,menu, QUERY_SECTION, anchor);
    }
    protected void printMenu(WebContext ctx, String which, String menu, String title) {
        printMenu(ctx,which,menu,title,QUERY_SECTION);
    }
    protected void printMenu(WebContext ctx, String which, String menu, String title, String key) {
		printMenu(ctx,which,menu,title,key,null);
	}
    protected void printMenu(WebContext ctx, String which, String menu, String title, String key, String anchor) {
        if(menu.equals(which)) {
            ctx.print("<b class='selected'>");
        } else {
            ctx.print("<a class='notselected' href=\"" + ctx.url() + ctx.urlConnector() + key+"=" + menu +
					((anchor!=null)?("#"+anchor):"") +
                      "\">");
        }
        if(menu.endsWith("/")) {
            ctx.print(title + "<font size=-1>(other)</font>");
        } else {
            ctx.print(title);
        }
        if(menu.equals(which)) {
            ctx.print("</b>");
        } else {
            ctx.print("</a>");
        }
    }
    
    void mailUser(WebContext ctx, String theirEmail, String subject, String message) {
        String requester = getRequester(ctx);
        String from = survprops.getProperty("CLDR_FROM","nobody@example.com");
        String smtp = survprops.getProperty("CLDR_SMTP",null);
        
        if(smtp == null) {
            ctx.println("<i>Not sending mail- SMTP disabled.</i><br/>");
            ctx.println("<hr/><pre>" + message + "</pre><hr/>");
            smtp = "NONE";
        } else {
            MailSender.sendMail(smtp, from, theirEmail, subject,
                                message);
            ctx.println("Mail sent to " + theirEmail + " from " + from + " via " + smtp + "<br/>\n");
        }
        logger.info( "Mail sent to " + theirEmail + "  from " + from + " via " + smtp + " - "+subject);
        /* some debugging. */
    }
    
    String getRequester(WebContext ctx) {
        String cleanEmail = ctx.session.user.email;
        if(cleanEmail.equals("admin@")) {
            cleanEmail = "surveytool@unicode.org";
        }
        String requester = ctx.session.user.name + " <" + cleanEmail + ">";
        return requester;
    }
    
    void notifyUser(WebContext ctx, String theirEmail, String pass) {
        String body = getRequester(ctx) +  " is notifying you of the CLDR vetting account for you.\n" +
        "To access it, visit: \n" +
        "   http://" + ctx.serverHostport() + ctx.base() + "?"+QUERY_PASSWORD+"=" + pass + "&"+QUERY_EMAIL+"=" + theirEmail + "\n" +
        //                                                                          // DO NOT ESCAPE THIS AMPERSAND.
        "\n" +
        "Or you can visit\n   http://" + ctx.serverHostport() + ctx.base() + "\n    username: " + theirEmail + "\n    password: " + pass + "\n" +
        "\n" +
        " Please keep this link to yourself. Thanks.\n" +
        " Follow the 'Instructions' link on the main page for more help.\n" +
        " \n";
        String subject = "CLDR Registration for " + theirEmail;
        mailUser(ctx,theirEmail,subject,body);
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
        } else if(type.equals(MEASNAMES)) {
            subtype = MEASNAME;
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

    public static final String CHECKCLDR = "CheckCLDR_";  // key for CheckCLDR objects by locale
    public static final String CHECKCLDR_RES = "CheckCLDR_RES_";  // key for CheckCLDR objects by locale
    
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
            ctx.session.getLocales().remove(ctx.field(QUERY_LOCALE));
            ctx.println("<h2>Your session for " + ctx.field(QUERY_LOCALE) + " has been removed.</h2>");
            doMain(ctx);
            return;
        }
        synchronized (ctx.session) { // session sync
            UserLocaleStuff uf = getUserFile(ctx, (ctx.session.user==null)?null:ctx.session.user, ctx.locale);
            CLDRFile cf = uf.cldrfile;
            if(cf == null) {
                throw new InternalError("CLDRFile is null!");
            }
            CLDRDBSource ourSrc = uf.dbSource; // TODO: remove. debuggin'
            if(ourSrc == null) {
                throw new InternalError("oursrc is null! - " + (USER_FILE + CLDRDBSRC) + " @ " + ctx.locale );
            }
            // Set up checks
            CheckCLDR checkCldr = (CheckCLDR)uf.getCheck(ctx); //make it happen
            
            // Locale menu
            if((which == null) ||
               which.equals("")) {
               which = xMAIN;
            }
            
            
            WebContext subCtx = new WebContext(ctx);
            subCtx.addQuery(QUERY_LOCALE,ctx.locale.toString());

            if(ctx.field(QUERY_EXAMPLE).length()==0) {
                ctx.println("<table summary='locale info' width='95%' border=0><tr><td width='25%'>");
                ctx.println("<b><a href=\"" + ctx.url() + "\">" + "Locales" + "</a></b><br/>");
                for(i=(n-1);i>0;i--) {
                    for(j=0;j<(n-i);j++) {
                        ctx.print("&nbsp;&nbsp;");
                    }
                    boolean canModify = UserRegistry.userCanModifyLocale(ctx.session.user,ctx.docLocale[i]);
                    ctx.println("\u2517&nbsp;<a class='notselected' href=\"" + ctx.url() + ctx.urlConnector() +QUERY_LOCALE+"=" + ctx.docLocale[i] + 
                        "\">");
                    printLocaleStatus(ctx, ctx.docLocale[i], new ULocale(ctx.docLocale[i]).getDisplayName(), "");
                    ctx.println(" <tt  style='font-size: 60%' class='codebox'>"+ctx.docLocale[i]+"</tt>" + "</a> ");
                    if(canModify) {
                        ctx.print(modifyThing(ctx));
                    }
                    ctx.println("<br/>");
                }
                for(j=0;j<n;j++) {
                    ctx.print("&nbsp;&nbsp;");
                }
                boolean canModifyL = UserRegistry.userCanModifyLocale(ctx.session.user,ctx.locale.toString());
                ctx.print("\u2517&nbsp;");
                ctx.print("<span style='font-size: 120%'>");
                printMenu(subCtx, which, xMAIN, 
                    getLocaleStatus(ctx.locale.toString(), ctx.locale.getDisplayName(), "") 
                        +" <tt style='font-size: 60%' class='codebox'>"+ctx.locale+"</tt>");
                if(canModifyL) {
                    ctx.print(modifyThing(ctx));
                }
                ctx.print("</span>");
                ctx.println("<br/>");
                ctx.println("</td><td>");
                
                subCtx.println("<p class='hang'> Code Lists: ");
                for(n =0 ; n < LOCALEDISPLAYNAMES_ITEMS.length; n++) {        
                    if(n>0) {
                        ctx.print(" | ");
                    }
                    printMenu(subCtx, which, LOCALEDISPLAYNAMES_ITEMS[n]);
                }
                subCtx.println("</p> <p class='hang'>Other Items: ");
                
                for(n =0 ; n < OTHERROOTS_ITEMS.length; n++) {        
                    if(n>0) {
                        ctx.print(" | ");
                    }
                    printMenu(subCtx, which, OTHERROOTS_ITEMS[n]);
                    ctx.print(" ");
                }
    //            ctx.print(" ");
    //            printMenu(subCtx, which, xOTHER);
                subCtx.println("</p>");
                if(ctx.prefBool(PREF_ADV)) {
                    subCtx.println("<p class='hang'>Advanced Items: ");
    //                printMenu(subCtx, which, TEST_MENU_ITEM);
                    printMenu(subCtx, which, RAW_MENU_ITEM);
                    subCtx.println("</p>");
                }
                subCtx.println("</td></tr></table>");
            } else { // end in-locale nav
                ctx.println("<h3>"+ctx.locale+" "+ctx.locale.getDisplayName()+" / " + which + " Example</h3>");
            }
            
            {
                List checkCldrResult = (List)uf.hash.get(CHECKCLDR_RES+ctx.defaultPtype());

                if((checkCldrResult != null) &&  (!checkCldrResult.isEmpty()) && 
                    (/* true || */ (checkCldr != null) && (xMAIN.equals(which))) ) {
                    ctx.println("<hr><h4>Possible problems with locale:</h4>");
                    ctx.println("<div style='border: 1px dashed olive; padding: 1em; background-color: cream; overflow: auto;'>");
                    for (Iterator it3 = checkCldrResult.iterator(); it3.hasNext();) {
                        CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();
                        try{ 
                            if (!status.getType().equals(status.exampleType)) {
                                ctx.println(status.getCause().getClass().toString() +": "+ status.toString() + "<br>");
                            } else {
                                ctx.println("<i>example available</i><br>");
                            }
                        } catch(Throwable t) {
                            ctx.println("Error reading status item: <br><font size='-1'>"+status.toString()+"<br> - <br>" + t.toString()+"<hr><br>");
                        }
                    }
                    ctx.println("</div><hr>");
                }
            }
            subCtx.addQuery(QUERY_SECTION,which);
            for(n =0 ; n < LOCALEDISPLAYNAMES_ITEMS.length; n++) {        
                if(LOCALEDISPLAYNAMES_ITEMS[n].equals(which)) {
                    if(which.equals(CURRENCIES)) {
                        showPathList(subCtx, "//ldml/"+NUMBERSCURRENCIES, null);
                    } else if(which.equals(TIMEZONES)) {
                        showPathList(subCtx, "//ldml/"+"dates/timeZoneNames/zone", null);
                    } else {
                        showLocaleCodeList(subCtx, which);
                    }
                    return;
                }
            }
            
            for(j=0;j<OTHERROOTS_ITEMS.length;j++) {
                if(OTHERROOTS_ITEMS[j].equals(which)) {
                    if(which.equals(GREGORIAN_CALENDAR)) {
                        showPathList(subCtx, GREGO_XPATH, null);
                    } else if(which.equals(OTHER_CALENDARS)) {
                        showPathList(subCtx, OTHER_CALENDARS_XPATH, null);
                    } else if(xOTHER.equals(which)) {
                        showPathList(subCtx, "//ldml", null);
                    } else {
                        showPathList(subCtx, "//ldml/"+OTHERROOTS_ITEMS[j], null);
                    }
                    return;
                }
            }
            // fall through if wasn't one of the other roots
            if(RAW_MENU_ITEM.equals(which)) {
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
                          //ctx.println("<i>example available</i><br>");
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
    ctx.println("raw not supported currently. ");
/*
    CLDRFile file = (CLDRFile)ctx.getByLocale(USER_FILE);
    
    ctx.println("<h3>Raw output of the locale's CLDRFile</h3>");
    ctx.println("<pre style='border: 2px solid olive; margin: 1em;'>");
    
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    file.write(pw);
    String asString = sw.toString();
    //                fullBody = fullBody + "-------------" + "\n" + k + ".xml - " + displayName + "\n" + 
    //                    hexXML.transliterate(asString);
    String asHtml = TransliteratorUtilities.toHTML.transliterate(asString);
    ctx.println(asHtml);
    ctx.println("</pre>");*/
}

public static final String XML_PREFIX="/xml/main";
public static final String VXML_PREFIX="/vxml/main";

public boolean doRawXml(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {
    String s = request.getPathInfo();
    if((s==null)||!(s.startsWith(XML_PREFIX)||s.startsWith(VXML_PREFIX))) {
        return false;
    }

    boolean finalData = false;
    
    if(s.startsWith(VXML_PREFIX)) {
        finalData = true;

        if(s.equals(VXML_PREFIX)) {
            WebContext ctx = new WebContext(request,response);
            response.sendRedirect(ctx.schemeHostPort()+ctx.base()+VXML_PREFIX+"/");
            return true;
        }
        s = s.substring(VXML_PREFIX.length()+1,s.length()); //   "foo.xml"
    } else {
        if(s.equals(XML_PREFIX)) {
            WebContext ctx = new WebContext(request,response);
            response.sendRedirect(ctx.schemeHostPort()+ctx.base()+XML_PREFIX+"/");
            return true;
        }
        s = s.substring(XML_PREFIX.length()+1,s.length()); //   "foo.xml"
    }
    
    if(s.length() == 0) {
        WebContext ctx = new WebContext(request,response);
        response.setContentType("text/html; charset=utf-8");
        if(finalData) {
            ctx.println("<title>CLDR Data | All Locales - Vetted Data</title>");
        } else {
            ctx.println("<title>CLDR Data | All Locales</title>");
        }
        ctx.println("<a href='"+ctx.base()+"'>Return to SurveyTool</a><p>");
        ctx.println("<h4>Locales</h4>");
        ctx.println("<ul>");
        File inFiles[] = getInFiles();
        int nrInFiles = inFiles.length;
        for(int i=0;i<nrInFiles;i++) {
            String fileName = inFiles[i].getName();
            int dot = fileName.indexOf('.');
            String localeName = fileName.substring(0,dot);
            ctx.println("<li><a href='"+fileName+"'>"+fileName+"</a> " + new ULocale(localeName).getDisplayName() +
                "</li>");
        }
        ctx.println("</ul>");
        ctx.println("<hr>");
        ctx.println("<a href='"+ctx.base()+"'>Return to SurveyTool</a><p>");
        ctx.close();
    } else if(!s.endsWith(".xml")) {
        WebContext ctx = new WebContext(request,response);
        response.sendRedirect(ctx.schemeHostPort()+ctx.base()+XML_PREFIX+"/");
    } else {
        File inFiles[] = getInFiles();
        int nrInFiles = inFiles.length;
        boolean found = false;
        String theLocale = null;
        for(int i=0;(!found) && (i<nrInFiles);i++) {
            String localeName = inFiles[i].getName();
            if(s.equals(localeName)) {
                found=true;
                int dot = localeName.indexOf('.');
                theLocale = localeName.substring(0,dot);
            }
        }
        if(!found) {
            throw new InternalError("No such locale: " + s);
        } else {
            response.setContentType("application/xml; charset=utf-8");
            CLDRDBSource dbSource = makeDBSource(null, new ULocale(theLocale), finalData);
            CLDRFile file = makeCLDRFile(dbSource);
//            file.write(WebContext.openUTF8Writer(response.getOutputStream()));
            file.write(response.getWriter());
        }
    }
    return true;
}

public static String xpathToMenu(String path) {                    
	String theMenu=null;
	if(path.startsWith(LOCALEDISPLAYNAMES)) {
		for(int i=0;i<LOCALEDISPLAYNAMES_ITEMS.length;i++) {
			if(path.startsWith(LOCALEDISPLAYNAMES+LOCALEDISPLAYNAMES_ITEMS[i])) {
				theMenu=LOCALEDISPLAYNAMES_ITEMS[i];
			}
		}
	} else if(path.startsWith(GREGO_XPATH)) {
		theMenu=GREGORIAN_CALENDAR;
	} else if(path.startsWith(OTHER_CALENDARS_XPATH)) {
		theMenu=OTHER_CALENDARS;
	} else if(path.startsWith("//ldml/"+NUMBERSCURRENCIES)) {
		theMenu=CURRENCIES;
	} else if(path.startsWith( "//ldml/"+"dates/timeZoneNames/zone")){
		theMenu=TIMEZONES;
	} else if(path.startsWith( "//ldml/"+LDMLConstants.CHARACTERS)) {
		theMenu = LDMLConstants.CHARACTERS;
	} else if(path.startsWith( "//ldml/"+LDMLConstants.NUMBERS)) {
		theMenu = LDMLConstants.NUMBERS;
	} else if(path.startsWith( "//ldml/"+LDMLConstants.REFERENCES)) {
		theMenu = LDMLConstants.REFERENCES;
	} else {
		theMenu=xOTHER;
		// other?
	}
	return theMenu;
}

/**
* Show the 'main info about this locale' (General) panel.
 */
public void doMain(WebContext ctx) {
    String diskVer = LDMLUtilities.getCVSVersion(fileBase, ctx.locale.toString() + ".xml"); // just get ver of the latest file.
    String dbVer = makeDBSource(null,ctx.locale).getSourceRevision();
    
    // what should users be notified about?
    int vetStatus = vet.status(ctx.locale.toString());
    if((UserRegistry.userIsVetter(ctx.session.user))&&((vetStatus & Vetting.RES_BAD_MASK)>0)) {
        ctx.println("<hr>");
        int numNoVotes = vet.countResultsByType(ctx.locale.toString(),Vetting.RES_NO_VOTES);
        int numInsufficient = vet.countResultsByType(ctx.locale.toString(),Vetting.RES_INSUFFICIENT);
        int numDisputed = vet.countResultsByType(ctx.locale.toString(),Vetting.RES_DISPUTED);
//            rv = rv + ("");
     
        Hashtable insItems = new Hashtable();
        Hashtable disItems = new Hashtable();
        synchronized(vet.conn) { 
            try { // moderately expensive.. since we are tying up vet's connection..
                ResultSet rs = vet.listBadResults(ctx.locale.toString());
                while(rs.next()) {
                    int xp = rs.getInt(1);
					int type = rs.getInt(2);
					
                    String path = xpt.getById(xp);
                    
					String theMenu = xpathToMenu(path);
					
                    if(theMenu != null) {
						if(type == Vetting.RES_DISPUTED) {
							disItems.put(theMenu, "");// what goes here?
						} else {
							insItems.put(theMenu, "");
						}
					}
                }
                rs.close();
            } catch (SQLException se) {
                throw new RuntimeException("SQL error listing bad results - " + SurveyMain.unchainSqlException(se));
            }
        }
        WebContext subCtx = new WebContext(ctx);
        //subCtx.addQuery(QUERY_LOCALE,ctx.locale.toString());
        subCtx.removeQuery(QUERY_SECTION);

        if((numNoVotes+numInsufficient)>0) {
            ctx.print("<h4><span style='padding: 1px;' class='insufficient'>"+(numNoVotes+numInsufficient)+" insufficient</span> </h4>");
			for(Iterator li = insItems.keySet().iterator();li.hasNext();) {
				String item = (String)li.next();
				printMenu(subCtx, "", item);
				if(li.hasNext() ) {
					subCtx.print(" | ");
				}
			}
        }
        if(numDisputed>0) {
            ctx.print("<h4><span style='padding: 1px;' class='disputed'>" +numDisputed+" disputed</span> </h4>");
			for(Iterator li = disItems.keySet().iterator();li.hasNext();) {
				String item = (String)li.next();
				printMenu(subCtx, "", item, item, "only=disputed&x", DataPod.CHANGES_DISPUTED);
				if(li.hasNext() ) {
					subCtx.print(" | ");
				}
			}
        }
        
    }
    ctx.println("<hr/><p><p>");
    ctx.println("<h3>Basic information about the Locale</h3>");
    
    ctx.print("  <p><i><font size='+1' color='red'>Important Notes:</font></i></p>  <ul>    <li><font size='4'><i>W</i></font><i><font size='4'>"+
                "hen you navigate away from any page, any     data changes you've made will be lost <b>unless</b> you hit the"+
                " <b>"+xSAVE+"</b> button!</font></i></li>    <li><i><font size='4'>"+
                            SLOW_PAGE_NOTICE+
                "</font></i></li>    <li><i><font size='4'>Be sure to read </font>    "+
//                "<a href='http://www.unicode.org/cldr/wiki?SurveyToolHelp'>"
                "<font size='4'>");
    ctx.printHelpLink("","General&nbsp;Instructions");
    ctx.print("</font>"+
                "<font size='4'>     once before going further.</font></i></li>   "+
                " <li><font size='4'><i>Consult the Page Instructions if you have questions on any page.</i></font>"+
                "</li>  </ul>");
    
    if(dbVer != null) {
        ctx.println( LDMLUtilities.getCVSLink(ctx.locale.toString(), dbVer) + "CVS version #" + dbVer + "</a>");
        if((diskVer != null)&&(!diskVer.equals(dbVer))) {
            ctx.println( " " + LDMLUtilities.getCVSLink(ctx.locale.toString(), dbVer) + "(Note: " + diskVer + " may be installed soon.)</a>");
        }
    }    
    ctx.println(SLOW_PAGE_NOTICE);
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
**/

public static final String USER_FILE = "UserFile";
public static final String USER_FILE_KEY = "UserFileKey";
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
    String script = locale.getScript();
    if(locStr.startsWith("ps") ||
       locStr.startsWith("fa") ||
       locStr.startsWith("ar") ||
       locStr.startsWith("syr") ||
       locStr.startsWith("he") ||
       "Arab".equals(script)) {
        return "rtl";
    } else {
        return "ltr";
    }
}

public class UserLocaleStuff extends Registerable {
    public CLDRFile cldrfile = null;
    public CLDRDBSource dbSource = null;
    public Hashtable hash = new Hashtable();
    
    public UserLocaleStuff(String locale) {
        super(lcr, locale);
//System.err.println("Adding ULS:"+locale);
    }
    
    public void clear() {
        hash.clear();        
        // TODO: try just kicking these instead of clearing?
        cldrfile=null;
        dbSource=null;
        setValid();
    }
    
    public CheckCLDR getCheck(WebContext ctx) {
        CheckCLDR checkCldr = (CheckCLDR)hash.get(CHECKCLDR+ctx.defaultPtype());
        if (checkCldr == null)   {
            List checkCldrResult = new ArrayList();
//                logger.info("Initting tests . . . - "+ctx.locale+"|" + ( CHECKCLDR+":"+ctx.defaultPtype()) + "@"+ctx.session.id);
//            long t0 = System.currentTimeMillis();
            checkCldr = CheckCLDR.getCheckAll(/* "(?!.*Collision.*).*" */  ".*");
            
            checkCldr.setDisplayInformation(getEnglishFile());
            if(cldrfile==null) {
                throw new InternalError("cldrfile was null.");
            }
            checkCldr.setCldrFileToCheck(cldrfile, ctx.getOptionsMap(), checkCldrResult);
//            logger.info("fileToCheck set . . . on "+ checkCldr.toString());
            hash.put(CHECKCLDR+ctx.defaultPtype(), checkCldr);
            if(!checkCldrResult.isEmpty()) {
                hash.put(CHECKCLDR_RES+ctx.defaultPtype(), checkCldrResult);
            }
//            long t2 = System.currentTimeMillis();
//            logger.info("Time to init tests: " + (t2-t0));
        }
        return checkCldr;
    }
};

UserLocaleStuff getOldUserFile(WebContext ctx) {
    UserLocaleStuff uf = (UserLocaleStuff)ctx.getByLocale(USER_FILE_KEY);
    return uf;
}

UserLocaleStuff getUserFile(WebContext ctx, UserRegistry.User user, ULocale locale) {
    // has this locale been invalidated?
    UserLocaleStuff uf = getOldUserFile(ctx);
    
    if(uf == null) {
        uf = new UserLocaleStuff(locale.toString());
        ctx.putByLocale(USER_FILE_KEY, uf);
        uf.register(); // register with lcr
    } else if(!uf.isValid()) {
//        System.err.println("Invalid, clearing: "+ uf.toString());
        uf.clear();
        uf.register(); // reregister
    }
    
    if(uf.cldrfile == null) {
        uf.dbSource = makeDBSource(user, locale);
        uf.cldrfile = makeCLDRFile(uf.dbSource);
    }
    return uf;
}
CLDRDBSource makeDBSource(UserRegistry.User user, ULocale locale) {
    CLDRDBSource dbSource = CLDRDBSource.createInstance(fileBase, xpt, locale,
                                                        getDBConnection(), user);
    return dbSource;
}
CLDRDBSource makeDBSource(UserRegistry.User user, ULocale locale, boolean finalData) {
    CLDRDBSource dbSource = CLDRDBSource.createInstance(fileBase, xpt, locale,
                                                        getDBConnection(), user, finalData);
    return dbSource;
}
static CLDRFile makeCLDRFile(CLDRDBSource dbSource) {
    return new CLDRFile(dbSource,false);
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


/**
* This is the main function for showing lists of items (pods).
 */
public void showPathList(WebContext ctx, String xpath, String lastElement) {
    UserLocaleStuff uf = getUserFile(ctx, ctx.session.user, ctx.locale);
    CLDRDBSource ourSrc = uf.dbSource;
    CLDRFile cf =  uf.cldrfile;
    String fullThing = xpath + "/" + lastElement;
    boolean isTz = xpath.equals("timeZoneNames");
    if(lastElement == null) {
        fullThing = xpath;
    }
    
    boolean canModify = (UserRegistry.userCanModifyLocale(ctx.session.user,ctx.locale.toString()));
    
    synchronized(ourSrc) { // because it has a connection..
                           // Podder
        // TODO: move this into showExample. . .
        String e = ctx.field(QUERY_EXAMPLE);
        if(e.length() > 0) {
            DataPod oldPod =  ctx.getExistingPod(fullThing);
            DataPod.ExampleEntry ee = null;
            if(oldPod != null) {
                ee = oldPod.getExampleEntry(e);
            }
            if(ee != null) {
                ctx.println("<form method=POST action='" + ctx.base() + "'>");
                ctx.printUrlAsHiddenFields();   
                String cls = shortClassName(ee.status.getCause());
                ctx.printHelpLink("/"+cls+"-example","Help with this "+cls+" example", true);
                ctx.addQuery(QUERY_EXAMPLE,e);
                ctx.println("<input type='hidden' name='"+QUERY_EXAMPLE+"' value='"+e+"'>");
                ctx.println(ee.status.toString());
                ctx.println("<hr width='10%'>");
                ctx.println(ee.status.getHTMLMessage());
                ctx.println("<hr width='10%'>");
                CheckCLDR.SimpleDemo d = ee.status.getDemo();
                Map m = new TreeMap();
                m.putAll(ctx.getParameterMap());

                if(d != null) {
                    d.processPost(m);
                
                    try {
                        String path = ee.pod.xpath(ee.pea);
                        String fullPath = cf.getFullXPath(path);
                        String value = ee.item.value;
                        String html = d.getHTML(m);
                        ctx.println(html);
                    } catch (Exception ex) {
                        ctx.println("<br><b>Error: </b> " + ex.toString() +"<br>");
                    }
//                    if(d.processPost(m)) {
//                        ctx.println("<hr>"+m);
//                    }
                }
                ctx.println("</form>");
            } else {
                ctx.println("<P><P><P><blockquote><i>That example seems to have expired. Perhaps the underlying data has changed? Try reloading the parent page, and clicking the Example link again.</i></blockquote>");
            }
        } else {
            // first, do submissions.
            if(canModify) {
                DataPod oldPod = ctx.getExistingPod(fullThing);
                if(processPeaChanges(ctx, oldPod, cf, ourSrc)) {
                    int j = vet.updateResults(oldPod.locale); // bach 'em
                    ctx.println("<br> You submitted data or vote changes, and " + j + " results were updated. As a result, your items may show up under the 'priority' or 'proposed' categories.<br>");
                }
            }
    //        System.out.println("Pod's full thing: " + fullThing);
            DataPod pod = ctx.getPod(fullThing);
            
            showPeas(ctx, pod, canModify);
        }
    }
}

String getSortMode(WebContext ctx, DataPod pod) {
    return getSortMode(ctx, pod.xpathPrefix);
}

String getSortMode(WebContext ctx, String prefix) {
    String sortMode = null;
    if(prefix.indexOf("timeZone")!=-1 ||
        prefix.indexOf("localeDisplayName")!=-1) {
        sortMode = ctx.pref(PREF_SORTMODE, phaseVetting?PREF_SORTMODE_WARNING:PREF_SORTMODE_DEFAULT);
    } else {
        sortMode = ctx.pref(PREF_SORTMODE, phaseVetting?PREF_SORTMODE_WARNING:/*PREF_SORTMODE_CODE*/PREF_SORTMODE_WARNING); // all the rest get Code Mode
    }
    return sortMode;
}

void showPeas(WebContext ctx, DataPod pod, boolean canModify) {
    int count = 0;
    int dispCount = 0;
    int total = 0;
    int skip = 0;
    
    //       total = mySet.count();
    //        boolean sortAlpha = (sortMode.equals(PREF_SORTMODE_ALPHA));
    UserLocaleStuff uf = getUserFile(ctx, ctx.session.user, ctx.locale);
    CLDRFile cf = uf.cldrfile;
        //    CLDRFile engf = getEnglishFile();
    CLDRDBSource ourSrc = uf.dbSource; // TODO: remove. debuggin'
    CheckCLDR checkCldr = (CheckCLDR)uf.getCheck(ctx);
    XPathParts pathParts = new XPathParts(null, null);
    XPathParts fullPathParts = new XPathParts(null, null);
    List checkCldrResult = new ArrayList();
    Iterator theIterator = null;
    String sortMode = getSortMode(ctx, pod);
	boolean disputedOnly = false;
	if(ctx.field("only").equals("disputed")) {
		disputedOnly=true;
		sortMode = PREF_SORTMODE_WARNING; // so that disputed shows up on top
	}
    List peas = pod.getList(sortMode);
    String refs[] = new String[0];
    String ourDir = getDirectionFor(ctx.locale);
    ctx.println("<hr>");
    boolean showFullXpaths = ctx.prefBool(PREF_XPATHS);
    // calculate references
    if(pod.xpathPrefix.indexOf("references")==-1) {
        Set refsSet = new HashSet();
        DataPod refPod = ctx.getPod("//ldml/references");
        List refPeas = refPod.getList(PREF_SORTMODE_CODE);
        for(Iterator i = refPeas.iterator();i.hasNext();) {
            DataPod.Pea p = (DataPod.Pea)i.next();
            for(Iterator j = p.items.iterator();j.hasNext();) {
                DataPod.Pea.Item item = (DataPod.Pea.Item)j.next();
                if(item.inheritFrom == null) {
                        refsSet.add(p.type);
                }
            }
        }
        if(!refsSet.isEmpty()) {
            refs = (String[])refsSet.toArray((Object[]) refs);
        }
    }
    DataPod.DisplaySet dSet = pod.getDisplaySet(sortMode);  // contains 'peas' and display list
    boolean checkPartitions = (dSet.partitions.length > 0) && (dSet.partitions[0].name != null); // only check if more than 0 named partitions
    // -----
    skip = showSkipBox(ctx, peas.size(), pod.getDisplaySet(sortMode), true, sortMode);
    
    ctx.printUrlAsHiddenFields();   
	
	if(disputedOnly==true){
		ctx.println("(<b>Disputed Only</b>)<br><input type='hidden' name='only' value='disputed'>");
	}
	
    ctx.println("<input type='hidden' name='skip' value='"+ctx.field("skip")+"'>");
    ctx.println("<table summary='Data Items for "+ctx.locale.toString()+" " + pod.xpathPrefix + "' class='list' border='0'>");
    
    ctx.println("<tr class='heading'>\n"+
                " <th colspan=2>\n"+
                " </th>\n"+
                "<th> action</th>\n"+
                "<th> data</th>\n"+
                "<th> alerts</th>\n"+
                "</tr>");
				
	int peaStart = skip; // where should it start?
	int peaCount = CODES_PER_PAGE; // hwo many to show?
	
	if(disputedOnly) {
		for(int j = 0;j<dSet.partitions.length;j++) {
			if(dSet.partitions[j].name.equals(DataPod.CHANGES_DISPUTED)) {
				peaStart=dSet.partitions[j].start;
				peaCount=(dSet.partitions[j].limit - peaStart);
			}
		}
	}
	
    
    for(ListIterator i = peas.listIterator(peaStart);(count<peaCount)&&i.hasNext();count++) {
        DataPod.Pea p = (DataPod.Pea)i.next();
        
        if(checkPartitions) {
            for(int j = 0;j<dSet.partitions.length;j++) {
                if((dSet.partitions[j].name != null) && (count+skip) == dSet.partitions[j].start) {
                    ctx.println("<tr class='heading'><th style='border-top: 2px solid black' align='left' colspan='5'><i>" +
                        "<a name='" + dSet.partitions[j].name + "'>" +
                        dSet.partitions[j].name + "</a>" +
                        "</i></th></tr>");
                }
            }
        }
        
        try {
            showPea(ctx, pod, p, ourDir, cf, ourSrc, canModify,showFullXpaths,refs,checkCldr);
        } catch(Throwable t) {
            ctx.println("<tr class='topbar'><td colspan='8'><b>"+pod.xpath(p)+"</b><br>");
            ctx.print(t);
            ctx.print("</td></tr>");
        }
        if(p.subPeas != null) {
            for(Iterator e = p.subPeas.values().iterator();e.hasNext();) {
                DataPod.Pea subPea = (DataPod.Pea)e.next();
                try {
                    showPea(ctx, pod, subPea, ourDir, cf, ourSrc, canModify, showFullXpaths,refs,checkCldr);
                } catch(Throwable t) {
                    ctx.println("<tr class='topbar'><td colspan='8'>sub pea: <b>"+pod.xpath(subPea)+"."+subPea.altType+"</b><br>");
                    ctx.print(t);
                    ctx.print("</td></tr>");
                }
            }
        }
    }
    
    ctx.println("</table>");
    
    if(canModify && 
        pod.xpathPrefix.indexOf("references")!=-1) {
        ctx.println("<hr>");
        ctx.println("<table style='border: 1px solid black' class='list' summary='New Reference Box'>");
        ctx.println("<tr><th colspan=2 >Add New Reference ( click '"+xSAVE+"' after filling in the fields.)</th></tr>");
        ctx.println("<tr><td align='right'>Reference: </th><td><input size='80' name='"+MKREFERENCE+"_v'></td></tr>");
        ctx.println("<tr><td align='right'>URI: </th><td><input size='80' name='"+MKREFERENCE+"_u'></td></tr>");
        ctx.println("</table>");
    }
    
    
    /*skip = */ showSkipBox(ctx, peas.size(), pod.getDisplaySet(sortMode), false, sortMode);
    
    if(!canModify) {
        ctx.println("<hr> <i>You are not authorized to make changes to this locale.</i>");
    }
}

boolean processPeaChanges(WebContext ctx, DataPod oldPod, CLDRFile cf, CLDRDBSource ourSrc) {
    String ourDir = getDirectionFor(ctx.locale);
    boolean someDidChange = false;
    if(oldPod != null) {
        for(Iterator i = oldPod.getAll().iterator();i.hasNext();) {
            DataPod.Pea p = (DataPod.Pea)i.next();
            someDidChange = processPeaChanges(ctx, oldPod, p, ourDir, cf, ourSrc) || someDidChange;
            if(p.subPeas != null) {
                for(Iterator e = p.subPeas.values().iterator();e.hasNext();) {
                    DataPod.Pea subPea = (DataPod.Pea)e.next();
                    someDidChange = processPeaChanges(ctx, oldPod, subPea, ourDir, cf, ourSrc) || someDidChange;
                }
            }
        }            
        if(oldPod.xpathPrefix.indexOf("references")!=-1) {
            String newRef = ctx.field(MKREFERENCE+"_v");
            String uri = ctx.field(MKREFERENCE+"_u");
            if(newRef.length()>0) {
                String dup = null;
                for(Iterator i = oldPod.getAll().iterator();i.hasNext();) {
                    DataPod.Pea p = (DataPod.Pea)i.next();
                    /*
                    someDidChange = processPeaChanges(ctx, oldPod, p, ourDir, cf, ourSrc) || someDidChange;
                    if(p.subPeas != null) {
                        for(Iterator e = p.subPeas.values().iterator();e.hasNext();) {
                            DataPod.Pea subPea = (DataPod.Pea)e.next();
                            */
                            DataPod.Pea subPea = p;
                            for(Iterator j = p.items.iterator();j.hasNext();) {
                                DataPod.Pea.Item item = (DataPod.Pea.Item)j.next();
                                if(item.value.equals(newRef)) {
                                    dup = p.type;
                                } else {
                                }
                            }
                            /*
                        }
                    }*/
                }            
                ctx.print("<b>Adding new reference...</b> ");
                if(dup != null) {
                    ctx.println("Ref already exists, not adding: <tt class='codebox'>"+dup+"</tt> " + newRef + "<br>");
                } else {
                    String newType =  ourSrc.addReferenceToNextSlot(cf, ctx.locale.toString(), ctx.session.user.id, newRef, uri);
                    if(newType == null) {
                        ctx.print("<i>Error.</i>");
                    } else {
                        ctx.print("<tt class='codebox'>"+newType+"</tt>");
                        ctx.print(" added.. " + newRef +" @ " +uri);
                        someDidChange=true;
                    }
                }
            }
        }
    }
	if(someDidChange) {
		lcr.invalidateLocale(oldPod.locale);
	}
    return someDidChange;
}

boolean processPeaChanges(WebContext ctx, DataPod pod, DataPod.Pea p, String ourDir, CLDRFile cf, CLDRDBSource ourSrc) {
    String fieldHash = pod.fieldHash(p);
    String choice = ctx.field(fieldHash); // checkmark choice
    if(choice.length()==0) {
        return false; // nothing to see..
    }
    String fullPathFull = pod.xpath(p);
    int base_xpath = xpt.xpathToBaseXpathId(fullPathFull);
    int oldVote = vet.queryVote(pod.locale, ctx.session.user.id, base_xpath);
    // do modification here. 
    String choice_v = ctx.field(fieldHash+"_v"); // choice + value
    String choice_r = ctx.field(fieldHash+"_r"); // choice + value
    String choice_refDisplay = ""; // display value for ref
    boolean canSubmit = UserRegistry.userCanSubmitAnyLocale(ctx.session.user) || p.hasProps;
    
    if(choice_r.length()>0) {
        choice_refDisplay = " Ref: "+choice_r;
    }
    if(!choice.equals(CHANGETO)) {
        choice_v=""; // so that it is ignored
    }
    if(choice.equals(CHANGETO)&& choice_v.length()==0) {
        ctx.println("<tt class='codebox'>"+ p.displayName +"</tt> - value was left empty. Use 'remove' to request removal.<br>");
    } else if( (choice.equals(CHANGETO) && choice_v.length()>0) ||
         choice.equals(REMOVE) ) {
        if(!canSubmit) {
            ctx.println("You are not allowed to submit data at this time.<br>");
            return false;
        }
        String fullPathMinusAlt = XPathTable.removeAlt(fullPathFull);
        for(Iterator j = p.items.iterator();j.hasNext();) {
            DataPod.Pea.Item item = (DataPod.Pea.Item)j.next();
            if(choice_v.equals(item.value)  && 
                !((item.altProposed==null) && (p.inheritFrom!=null) &&  XMLSource.CODE_FALLBACK_ID.equals(p.inheritFrom))) { // OK to override code fallbacks
                String theirReferences = item.references;
                if(theirReferences == null) {
                    theirReferences="";
                }
                if(theirReferences.equals(choice_r)) {
                    ctx.println("<tt class='codebox'>" + p.displayName +"</tt>  - Not accepting value, as it is already present under " + 
                        ((item.altProposed==null)?" non-draft item ":(" <tt>"+item.altProposed+"</tt><br> ")));
                    return false;
                } else {
                    ctx.println("<tt class='codebox'>"+ p.displayName +"</tt> - <i>Note, differs only in references</i> ("+theirReferences+")<br>");
                }
            }
        }
        String altPrefix = null;
        // handle FFT
                
        if(p.type.equals(DataPod.FAKE_FLEX_THING)) {
               DateTimePatternGenerator dateTimePatternGenerator = new DateTimePatternGenerator();
               //String id = (String) attributes.get("id");
               String id = null;
               //String oldID = id;
               try {
                    id = dateTimePatternGenerator.getSkeleton(choice_v);
               } catch (RuntimeException e) {
                    ctx.println("<i>Sorry, had an error when trying to process flex data <tt><b>"+choice_v+"</b></tt>.. Could be a bad pattern. Try again<br>Error was: ");
                    printShortened(ctx,e.toString());
                    ctx.println("<br>");
                    return false;
               }
               
                altPrefix =         XPathTable.altProposedPrefix(ctx.session.user.id);
                String idStr="[@id=\""+id+"\"]";
                fullPathMinusAlt=DataPod.FAKE_FLEX_XPATH+idStr;
                ctx.println("<tt>"+fullPathMinusAlt+"</tt><br>");
            // no alt prefix
        } else {
            altPrefix =         XPathTable.altProposedPrefix(ctx.session.user.id);
        }
        String newProp = ourSrc.addDataToNextSlot(cf, pod.locale, fullPathMinusAlt, p.altType, 
            altPrefix, ctx.session.user.id, choice_v, choice_r);
        // update implied vote
        ctx.print("<tt class='codebox'>" + p.displayName + "</tt> <b>change: " + choice_v + " : " + newProp+"</b>");
        if(choice.equals(REMOVE)) {
            ctx.print(" <i>(removal)</i>");
        }
        doUnVote(ctx, pod.locale, base_xpath);
        int n = vet.updateImpliedVotes(pod.locale);
        ctx.println("updated " + n + " implied votes due to new data submission.");
        ctx.println("<br>");
        return true;
    } else if(choice.equals(CONFIRM)) {
        if(oldVote != base_xpath) {
            ctx.println("Registering vote for "+base_xpath+" - "+pod.locale+":" + base_xpath+" (base_xpath) replacing " + oldVote + "<br>");
            return doVote(ctx, pod.locale, base_xpath); // vote with xpath
        }
    } else if (choice.equals(DONTCARE)) {
        if(oldVote != -1) {
            ctx.println("Registering vote for "+base_xpath+" - "+pod.locale+":-1 replacing " + oldVote + "<br>");
            return doVote(ctx, pod.locale, -1, base_xpath);
        }
    } else {
        for(Iterator j = p.items.iterator();j.hasNext();) {
            DataPod.Pea.Item item = (DataPod.Pea.Item)j.next();
            if(choice.equals(item.altProposed)) {
                if(oldVote != item.xpathId) {
                    ctx.println("Registering vote for "+base_xpath+" - "+pod.locale+":" + item.xpathId+" replacing " + oldVote + "<br>");
                    return doVote(ctx, pod.locale, item.xpathId);
                } else {
                    return false; // existing vote.
                }
            }
        }
        ctx.println("<tt title='"+pod.locale+":"+base_xpath+"' class='codebox'>" + p.displayName + "</tt> Note: <i>" + choice + "</i> not supported yet. <br>");
    }
    return false;
}

boolean doVote(WebContext ctx, String locale, int xpath) {
    int base_xpath = xpt.xpathToBaseXpathId(xpath);
    return doVote(ctx, locale, xpath, base_xpath);
}

boolean doVote(WebContext ctx, String locale, int xpath, int base_xpath) {
    // TODO: checks
    vet.vote( locale,  base_xpath, ctx.session.user.id, xpath, Vetting.VET_EXPLICIT);
    lcr.invalidateLocale(locale); // throw out this pod next time, cause '.votes' are now wrong.
    return true;
}

int doUnVote(WebContext ctx, String locale, int base_xpath) {
    // TODO: checks
    int rs = vet.unvote( locale,  base_xpath, ctx.session.user.id);
    lcr.invalidateLocale(locale); // throw out this pod next time, cause '.votes' are now wrong.
    return rs;
}

// TODO: trim unused params
void showPea(WebContext ctx, DataPod pod, DataPod.Pea p, String ourDir, CLDRFile cf, 
    CLDRDBSource ourSrc, boolean canModify, boolean showFullXpaths, String refs[], CheckCLDR checkCldr) {
    //if(p.type.equals(DataPod.FAKE_FLEX_THING) && !UserRegistry.userIsTC(ctx.session.user)) {
    //    return;
    //}
    
    boolean canSubmit = UserRegistry.userCanSubmitAnyLocale(ctx.session.user)  // formally able to submit
        || (canModify&&p.hasProps); // or, there's modified data.

    boolean showedRemoveButton = false; 
    String fullPathFull = pod.xpath(p); 
    String boxClass = canModify?"actionbox":"disabledbox";
    boolean isAlias = (fullPathFull.indexOf("/alias")!=-1);
    WebContext refCtx = new WebContext(ctx);
    refCtx.setQuery(QUERY_SECTION,"references");
    //            ctx.println("<tr><th colspan='3' align='left'><tt>" + p.type + "</tt></th></tr>");

    String fieldHash = pod.fieldHash(p);
    
    // voting stuff
    int ourVote = -1;
    String ourVoteXpath = null;
    boolean somethingChecked = false; // have we presented a 'vote' yet?
    int base_xpath = xpt.xpathToBaseXpathId(fullPathFull);
    int resultType[] = new int[1];
    int resultXpath_id =  vet.queryResult(pod.locale, base_xpath, resultType);
    String resultXpath = null;
    if(resultXpath_id != -1) {
        resultXpath = xpt.getById(resultXpath_id); 
    }
    if(ctx.session.user != null) {
        ourVote = vet.queryVote(pod.locale, ctx.session.user.id, 
            base_xpath);
        
        //System.err.println(base_xpath + ": vote is " + ourVote);
        if(ourVote != -1) {
            ourVoteXpath = xpt.getById(ourVote);
        }
    }
    
    /*  TOP BAR */
    ctx.println("<tr class='topbar'>");
    String baseInfo = "#"+base_xpath+", w["+Vetting.typeToStr(resultType[0])+"]:" + resultXpath_id;
    if(p.displayName != null) { // have a display name - code gets its own box
        ctx.println("<th nowrap class='botgray' colspan='1' valign='top' align='left'>");
        ctx.println("<tt title='"+baseInfo+"' class='codebox'>"
                    + p.type + 
                    "</tt>");
    } else { // no display name - same box for code+alt
        ctx.println("<th nowrap class='botgray' colspan='5' valign='top' align='left'>");
    }
    if(p.altType != null) {
        ctx.println(" ("+p.altType+")");
    }
    if(p.displayName != null) {
        ctx.println("</th>");
        ctx.println("<th nowrap style='padding-left: 4px;' colspan='4' valign='top' align='left' class='botgray'>");
        ctx.println(p.displayName);
    } else {
        ctx.println("<tt title='"+baseInfo+"' >"+p.type+"</tt>");

    }
    if(showFullXpaths) {
        ctx.println("<br>"+fullPathFull);
    }
    
    if(true==false) {
        ctx.println("<br>"+"hasTests:"+p.hasTests+", props:"+p.hasProps+", hasInh:"+p.hasInherited);
    }
///*srl*/    ctx.println("<div style='float: right; font-size: 200%; font-family: Georgia; color: white; text-shadow: green 0px 0px 8px;'>"+p.reservedForSort+"</div>");
    ctx.println("</th> </tr>");
    
    
    if((p.hasInherited == true) && (p.type != null) && (p.inheritFrom == null)) { // by code
        String pClass = "class='warnrow'";
        ctx.print("<tr " + pClass + ">");
        ctx.print("<td nowrap colspan='3' valign='top' align='right'>");
        ctx.print("<span class='actionbox'>missing</span>");
        if(canModify) {
            ctx.print("<input name='"+fieldHash+"' value='"+"0"+"' type='radio'>");
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
    if(p.pathWhereFound != null) {
        // middle common string match
        String a = fullPathFull;
        String b = p.pathWhereFound;
        
        // run them through the wringer..
        a = xpt.getById(xpt.xpathToBaseXpathId(a));  
        b = xpt.getById(xpt.xpathToBaseXpathId(b));
        
        
        int alen = a.length();
        int blen = b.length();
        
        int prefixSize;
        int suffixSize;
        for(prefixSize=0;a.substring(0,prefixSize).equals(b.substring(0,prefixSize));prefixSize++)
            ;
        int maxlen = (alen>blen)?alen:blen;
        
        /* System.err.println("A:"+a);
        System.err.println("B:"+b); */
        
        for(suffixSize=0;((suffixSize+prefixSize)<maxlen)&&a.substring(alen-suffixSize,alen).equals(b.substring(blen-suffixSize,blen));suffixSize++)
            ;
//                    System.err.println("SUFF"+suffixSize+": ["+a+"] -> "+a.substring(alen-suffixSize,alen));
        
        if(prefixSize==0) {
            prefixSize=1;
        }
        
        String xa = a.substring(prefixSize-1,alen-suffixSize+1);
        String xb = b.substring(prefixSize-1,blen-suffixSize+1);
        
        if((xa.length()+xb.length())>0) {
            ctx.println("<tr><td colspan='3'><i>Alias: <tt span='codebox'>" + xb + "</tt> \u2192 <tt span='codebox'>" + xa + "</tt></i>");
            ctx.printHelpLink("/AliasedFrom","Help",true,false);
            ctx.println("</td></tr>");
        } else {
            ctx.println("<tr><td colspan='3'><i>Aliased: </i>");
            ctx.printHelpLink("/AliasedFrom","Help",true,false);
            ctx.println("</td></tr>");
        }
    }
    /* else */ if(isAlias) {   
//        String shortStr = fullPathFull.substring(fullPathFull.indexOf("/alias")/*,fullPathFull.length()*/);
//<tt class='codebox'>["+shortStr+"]</tt>
        ctx.println("<tr><td colspan='3'><i>Alias</i></td></tr>");
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
            ctx.println("<tr>");
            if(item.inheritFrom != null) {
                ctx.println("<td class='fallback'>Inherited from: " + item.inheritFrom+"</td>");
            } else {
                ctx.print("<td " + pClass + " >");
                if(item.altProposed!=null) {
                    ctx.print(item.altProposed);
                }
                ctx.print("</td>");
            }
            ctx.print("<td colspan='4'><tt>"+item.value+"</tt></td></tr>");
        }

    } else for(Iterator j = p.items.iterator();j.hasNext();) { // non alias
        DataPod.Pea.Item item = (DataPod.Pea.Item)j.next();
        String pClass ="";
        if((item.inheritFrom != null)&&(p.inheritFrom==null)) {
            pClass = "class='fallback'";
        } else if(item.altProposed != null) {
            pClass = "class='proposed'";
        } else if(p.inheritFrom != null) {
            pClass = "class='missing'";
        } 
        ctx.print("<tr><td colspan='1' align='right' valign='top'>");
//ctx.println("<div style='border: 2px dashed red'>altProposed="+item.altProposed+", inheritFrom="+item.inheritFrom+", confirmOnly="+new Boolean(p.confirmOnly)+"</div><br>");
        if(((item.altProposed==null)&&(item.inheritFrom==null)&&!p.confirmOnly)
            || (!j.hasNext()&&!showedRemoveButton)) {
            showedRemoveButton = true;
            ctx.print("<span class='"+boxClass+"'>"+REMOVE+"</span>");
            if(canModify&&canSubmit) {
                ctx.print("<input name='"+fieldHash+"' value='"+REMOVE+"' type='radio'>");
            } else {
                ctx.print("<input type='radio' disabled>");
            }
        }
        ctx.print("</td>");
        
                
        ctx.print("<td nowrap "+" colspan='2' valign='top' align='right'><span " + pClass + " >");
        if(item.altProposed != null) {
        
            // NB: don't show user under 'alt' for now. just show votes.
            
            int uid = XPathTable.altProposedToUserid(item.altProposed);
            UserRegistry.User theU = null;
            if(uid != -1) {
                theU = reg.getInfo(uid);
            }
            String aTitle = null;
            if((theU!=null)&&
               (ctx.session.user!=null)&&
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
//                boolean sameOrg = (ctx.session.user.org.equals(theU.org));
                aTitle = "From: &lt;"+theU.email+"&gt; " + theU.name + " (" + theU.org + ")";
            }
            
            ctx.print("<span ");
            if(aTitle != null) {
                ctx.print("title='"+aTitle+"' ");
            }
            ctx.print("class='"+boxClass+"'>"+CONFIRM+" " + item.altProposed + "</span>");
            
            if(item.inheritFrom != null) {
                ctx.println("<br><span class='fallback'>Inherited from: " + item.inheritFrom+"</span>");
            }
        } else {
            if(p.inheritFrom != null) {
                if(p.inheritFrom.equals(XMLSource.CODE_FALLBACK_ID)) {
                    ctx.print("<span class='"+boxClass+"'>"+CONFIRM+" " + p.inheritFrom + "</span>");
                } else {
                    ctx.print("<span class='"+boxClass+"'>"+CONFIRM+" inherited [" + p.inheritFrom + "]</span>");
                }
            } else {
                ctx.print("<span class='"+boxClass+"'>" + CONFIRM + "</span>");
            }
        }
        if(canModify) {
            boolean checkThis = !somethingChecked 
                            && (ourVoteXpath!=null) 
                            && (item.xpath!=null)
                            &&  ourVoteXpath.equals(item.xpath);
            if(checkThis) {
                somethingChecked = true;
            } else {
                /*if(ourVoteXpath != null) {
                    System.err.println("FAIL:  " + item.xpath + " != " + ourVoteXpath);
                }*/
            }
            ctx.print("<input title='#"+item.xpathId+"' name='"+fieldHash+"'  value='"+
                ((item.altProposed!=null)?item.altProposed:CONFIRM)+"' "+(checkThis?"CHECKED":"")+"  type='radio'>");
        } else {
            ctx.print("<input title='#"+item.xpathId+"' type='radio' disabled>");
        }
        if(item.votes != null) {
            ctx.println("<br>");
            //ctx.println("Showing votes for " + item.altProposed + " - " + item.xpath);
            ctx.print("<span title='vote count' class='votes'>");
            if(UserRegistry.userIsVetter(ctx.session.user)) {
                for(Iterator iter=item.votes.iterator();iter.hasNext();) {
                    UserRegistry.User theU  = (UserRegistry.User) iter.next();
                    if(theU != null) {
                        ctx.print("\u221A<a href='mailto:"+theU.email+"'>" + theU.name + "</a> (" + theU.org + ")");
                        if(iter.hasNext()) {
                            ctx.print("<br>");
                        }
                    } else {
                        ctx.println("<!-- null user -->");
                    }
                }
            } else {
                int n = item.votes.size();
                ctx.print("\u221A&nbsp;"+n+" vote"+((n>1)?"s":""));
            }
            ctx.print("</span>");
        }
        ctx.print("</span>");
        ctx.println("</td>");
        boolean winner = 
            ((resultXpath!=null)&&
            (item.xpath!=null)&&
            (item.xpath.equals(resultXpath)));
        
        ctx.print("<td  class='botgray' valign='top' dir='" + ourDir +"'>");
        String itemSpan = null;
        
        if(winner) {
            itemSpan = "approved";
        } else if((item.votes!=null)&&(resultType[0]==Vetting.RES_INSUFFICIENT)) {
            itemSpan = "insufficient";
        } else if(resultType[0]==Vetting.RES_DISPUTED) {
            itemSpan = "disputed";      
        }
        
        if(itemSpan != null) {
            ctx.print("<span class='"+itemSpan+"'>");
        }
        if(item.value.length()!=0) {
            ctx.print(item.value);
        } else {
            ctx.print("<i>(empty)</i>");
        }
        if(itemSpan != null) {
            ctx.print("</span>");
        }
		if(winner && resultType[0]==Vetting.RES_ADMIN) {
			ctx.println("<br>");
			ctx.printHelpLink("/AdminOverride","Admin Override");
		}
        ctx.print("</td>");
        if((item.tests != null) || (item.examples != null)) {
            if(item.tests != null) {
                ctx.println("<td class='warncell'>");
                for (Iterator it3 = item.tests.iterator(); it3.hasNext();) {
                    CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();
                    if (!status.getType().equals(status.exampleType)) {
                        
                        ctx.println("<span class='warning'>");
                        String cls = shortClassName(status.getCause());
                        printShortened(ctx,status.toString());
                        ctx.println("</span>");
                        if(cls != null) {
                            ctx.printHelpLink("/"+cls+"","Help");
                        }
                        ctx.print("<br>");
                    }
                }
            } else {
                ctx.println("<td class='examplecell'>");
            }
            
            if(item.examples != null) {
            /*
                if(true) { throw new InternalError("had Old examples?  Not supposed to happen.."); }
            }

            List examples = new ArrayList();
	public final CheckCLDR getExamples(String path, String fullPath, String value, Map options, List result) {

            {       
            */         
//                ctx.println("Examples: ");
                boolean first = true;
                for(Iterator it4 = item.examples.iterator(); it4.hasNext();) {
                    DataPod.ExampleEntry e = (DataPod.ExampleEntry)it4.next();
                    if(first==false) {
                    }
                    String cls = shortClassName(e.status.getCause());
                    if(e.status.getType().equals(e.status.exampleType)) {
                        printShortened(ctx,e.status.toString());
                        if(cls != null) {
                            ctx.printHelpLink("/"+cls+"","Help");
                        }
                        ctx.println("<br>");
                    } else {                        
                        ctx.print("<a target='_blank' href='"+ctx.url()+ctx.urlConnector()+ QUERY_EXAMPLE+"="+e.hash+"'>");
                        ctx.print(cls);
                        ctx.print("</a>");
                    }
                    first = false;
                }
//                    ctx.println(status.getHTMLMessage()+"<br>");
            }
            ctx.println("</td>");
        }
        
        // Print 'em, For now.
        if(item.references != null) {
           String references[] = UserRegistry.tokenizeLocale(item.references); //TODO: rename to 'tokenize string'
           if(references != null) {
                ctx.print("<td style='border-left: 1px solid gray;margin: 3px; padding: 2px;'>(Ref:");           
                for(int i=0;i<references.length;i++) {
                    if(i!=0) {
                        ctx.print(", ");
                    }
                    ctx.print("<b><a target='ref_"+ctx.locale+"' href='"+refCtx.url()+"#REF_" + references[i] + "'>"+references[i]+"</a></b>");
                }
                ctx.print(")</td>");
            }
        }
        
        ctx.println("</tr>");
    }
    
    if(true && !isAlias) {
        String pClass = "";
         // dont care
        ctx.print("<tr " + pClass + "><td nowrap valign='top' align='right'>");
        ctx.print("<span class='"+boxClass+"'>" + DONTCARE + "</span>");
        if(canModify) {
            ctx.print("<input name='"+fieldHash+"' value='"+DONTCARE+"' type='radio' "
                +((!somethingChecked /*ourVoteXpath==null*/)?"CHECKED":"")+" >");
            somethingChecked = true;
        } else {
            ctx.print("<input type='radio' disabled>");
        }
        ctx.println("</td>");
        ctx.println("<td></td>");
        // change
        ctx.print("<td nowrap valign='top' align='right'>");
        if(!p.confirmOnly) {
            ctx.print("<span class='"+boxClass+"'>" + CHANGETO + "</span>");
            if(canModify && canSubmit ) {
                ctx.print("<input name='"+fieldHash+"' id='"+fieldHash+"_ch' value='"+CHANGETO+"' type='radio' >");
            } else {
                ctx.print("<input type='radio' disabled>");
            }
        }
        ctx.println("</td>");
        if(canSubmit && canModify && !p.confirmOnly) {
            ctx.println("<td colspan='2'><input onfocus=\"document.getElementById('"+fieldHash+"_ch').click()\" name='"+fieldHash+"_v'  class='inputbox'></td>");
            if(refs.length>0) {
                ctx.print("<td nowrap><label>");
                ctx.print("<a target='ref_"+ctx.locale+"' href='"+refCtx.url()+"'>Ref:</a>");
                ctx.print("&nbsp;<select name='"+fieldHash+"_r'>");
                ctx.print("<option value='' SELECTED></option>");
                for(int i=0;i<refs.length;i++) {
                    ctx.print("<option value='"+refs[i]+"'>"+refs[i]+"</option>");
                }
                ctx.println("</select></label></td>");
            }
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

int showSkipBox(WebContext ctx, int total, DataPod.DisplaySet displaySet, boolean showSearchMode, String sortMode) {
showSearchMode = true;// all
    List displayList = null;
    if(displaySet != null) {
        displayList = displaySet.displayPeas;
    }
    int skip;
    ctx.println("<div class='pager' style='margin: 2px'>");
    if((ctx.locale != null) && UserRegistry.userCanModifyLocale(ctx.session.user,ctx.locale.toString())) { // at least street level
        // TODO: move to pager
        if((ctx.field(QUERY_SECTION).length()>0) && !ctx.field(QUERY_SECTION).equals(xMAIN)) {
            ctx.println("<input style='float:right' type='submit' value='" + xSAVE + "'>");
        }
    }
    if(showSearchMode) {
        ctx.println(/*"<p style='float: right; margin-left: 3em;'> " + */
            "<b>Sorted:</b>  ");
        {
            boolean sortAlpha = (sortMode.equals(PREF_SORTMODE_ALPHA));
            
            //          showSkipBox_menu(ctx, sortMode, PREF_SORTMODE_ALPHA, "Alphabetically");
            showSkipBox_menu(ctx, sortMode, PREF_SORTMODE_CODE, "Code");
            showSkipBox_menu(ctx, sortMode, PREF_SORTMODE_WARNING, "Priority");
            if(displaySet.canName) {
                showSkipBox_menu(ctx, sortMode, PREF_SORTMODE_NAME, "Name");
            }
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

    // calculate nextSkip
    int from = skip+1;
    int to = from + CODES_PER_PAGE-1;
    if(to >= total) {
        to = total;
    }

    // Print navigation
    if(showSearchMode) {
        ctx.println("<br/>");
        ctx.println("Displaying items " + from + " to " + to + " of " + total);        

        if(total>=(CODES_PER_PAGE)) {
            int prevSkip = skip - CODES_PER_PAGE;
            if(prevSkip<0) {
                prevSkip = 0;
            }
            ctx.print("<div style='float: right'>");
            if(skip<=0) {
                ctx.print("<span class='pagerl_inactive'>\u2190&nbsp;prev"/* + CODES_PER_PAGE */ + "" +
                        "</span>&nbsp;");  
            } else {
                ctx.print("<a class='pagerl_active' href=\"" + ctx.url() + 
                        ctx.urlConnector() + "skip=" + new Integer(prevSkip) + "\">" +
                        "\u2190&nbsp;prev"/* + CODES_PER_PAGE*/ + "");
                ctx.print("</a>&nbsp;");
            }
            int nextSkip = skip + CODES_PER_PAGE; 
            if(nextSkip >= total) {
                nextSkip = -1;
                if(total>=(CODES_PER_PAGE)) {
                    ctx.println(" <span class='pagerl_inactive' >" +
                                "next&nbsp;"/* + CODES_PER_PAGE*/ + "\u2192" +
                                "</span>");
                }
            } else {
                ctx.println(" <a class='pagerl_active' href=\"" + ctx.url() + 
                            ctx.urlConnector() +"skip=" + new Integer(nextSkip) + "\">" +
                            "next&nbsp;"/* + CODES_PER_PAGE*/ + "\u2192" +
                            "</a>");
            }
            ctx.print("</div>");
        }
        ctx.println("<br/>");
    }

    if(total>=(CODES_PER_PAGE)) {
        if(displaySet.partitions.length > 1) {
            ctx.println("<table summary='navigation box' style='border-collapse: collapse'><tr valign='top'><td>");
        }
        if(skip>0) {
            if(skip>=total) {
                skip = 0;
            }
        }
        if(displaySet.partitions.length > 1) {
            ctx.println("</td>");
        }
        for(int j=0;j<displaySet.partitions.length;j++) {
            if(j>0) { 
                ctx.println("<tr valign='top'><td></td>");
            }
            if(displaySet.partitions[j].name != null) {
                ctx.print("<td  class='pagerln' align='left'><p style='margin-top: 2px; margin-bottom: 2px;' class='hang'><b>" + displaySet.partitions[j].name + ":</b>"
                    /*+ "</td><td class='pagerln'>"*/);
            }
            int ourStart = displaySet.partitions[j].start;
            int ourLimit = displaySet.partitions[j].limit;
            for(int i=ourStart;i<ourLimit;i = (i - (i%CODES_PER_PAGE)) + CODES_PER_PAGE) {
                int pageStart = i - (i%CODES_PER_PAGE); // pageStart is the skip at the top of this page. should be == ourStart unless on a boundary.
                int end = pageStart + CODES_PER_PAGE-1;
                if(end>=ourLimit) {
                    end = ourLimit-1;
                }
    // \u2013 = --
    // \u2190 = <--
    // \u2026 = ...
                boolean isus = (pageStart == skip);
                if(isus) {
                    if(((i!=pageStart) || (i==0)) && (displaySet.partitions[j].name != null)) {
                        ctx.print(" <b><a class='selected' style='text-decoration:none' href='#"+displaySet.partitions[j].name+"'>");
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
                        iString = iString.substring(0,20)+"\u2026";
                        endString="";
                    }
                    if(endString.length()>20) {
                        endString = endString.substring(0,20)+"\u2026";
                    }
                    if(end>i) {
                        ctx.print(iString+"\u2013"+endString);
                    } else {
                        ctx.print(iString);
                    }
                } else {
                    ctx.print( ""+(i+1) );
                    ctx.print( "\u2013" + (end+1));
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
            if(displaySet.partitions.length > 1) {
                ctx.print("</p>");
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
            ctx.println("<br><b>Items:</b><ul>");
            for(int j=0;j<displaySet.partitions.length;j++) {
                ctx.print("<b><a class='selected' style='text-decoration:none' href='#"+displaySet.partitions[j].name+"'>");
                ctx.print(displaySet.partitions[j].name + "</a></b> ");
                if(j<displaySet.partitions.length-1) {
                    ctx.println("<br>");
                }
            }
            ctx.println("</ul>");
        }
    }
    ctx.println("</div>");
    return skip;
}

public static int pages=0;
public static int xpages=0;
/**
* Main setup
 */
static  boolean isSetup = false;
public synchronized void doStartup() throws ServletException {
        if(isSetup == true) {
            return;
        }
        
        // set up CheckCLDR
        //CheckCLDR.SHOW_TIMES=true;
        
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

        logger.info("SurveyTool starting up. root=" + new File(cldrHome).getAbsolutePath());
        
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
    specialHeader = survprops.getProperty("CLDR_HEADER"); // not static - may change lager

    if(!new File(fileBase).isDirectory()) {
        busted("CLDR_COMMON isn't a directory: " + fileBase);
        return;
    }

    if(!new File(vetweb).isDirectory()) {
        busted("CLDR_VET_WEB isn't a directory: " + vetweb);
        return;
    }

    File cacheDir = new File(cldrHome, "cache");
//    logger.info("Cache Dir: " + cacheDir.getAbsolutePath() + " - creating and emptying..");
    CachingEntityResolver.setCacheDir(cacheDir.getAbsolutePath());
    CachingEntityResolver.createAndEmptyCacheDir();


    isSetup = true;



    int status = 0;
//    logger.info(" ------------------ " + new Date().toString() + " ---------------");
    if(isBusted != null) {
        return; // couldn't write the log
    }
    if ((specialMessage!=null)&&(specialMessage.length()>0)) {
        logger.warning("SurveyTool with CLDR_MESSAGE: " + specialMessage);
        busted("message: " + specialMessage);
    }
    /*
     SurveyMain m = new SurveyMain();  ???
     
     if(!m.reg.read()) {
         busted("Couldn't load user registry [at least put an empty file there]   - exiting");
         return;
     }
     */
    if(!readWarnings()) {
        // already busted
        return;
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
                   &&!n.startsWith(".")
                   &&!n.startsWith("supplementalData") // not a locale
                   /*&&!n.startsWith("root")*/); // root is implied, will be included elsewhere.
        }
    };
    File baseDir = new File(fileBase);
    return baseDir.listFiles(myFilter);
}

static protected String[] getLocales() {
    File inFiles[] = getInFiles();
    int nrInFiles = inFiles.length;
    Vector v = new Vector();
    for(int i=0;i<nrInFiles;i++) {
        String fileName = inFiles[i].getName();
        int dot = fileName.indexOf('.');
        if(dot !=  -1) {
            String locale = fileName.substring(0,dot);
            v.add(locale);
        }
    }
    return (String[])v.toArray(new String[0]);
}

/**
 * Returns a Map of all interest groups.
 * en -> en, en_US, en_MT, ...
 * fr -> fr, fr_BE, fr_FR, ...
 */
static protected Map getIntGroups() {
    String[] locales = getLocales();
    Map h = new HashMap();
    for(int i=0;i<locales.length;i++) {
        String locale = locales[i];
        String group = locale;
        int dash = locale.indexOf('_');
        if(dash !=  -1) {
            group = locale.substring(0,dash);
        }
        Set s = (Set)h.get(group);
        if(s == null) {
            s = new HashSet();
            h.put(group,s);
        }
        s.add(locale);
    }
    return h;
}

/* returns a map of String localegroup -> Set [ User interestedUser,  ... ]
*/ 
protected Map getIntUsers(Map intGroups) {
    Map m = new HashMap();
    try {
        synchronized(reg) {
            java.sql.ResultSet rs = reg.list(null);
            if(rs == null) {
                return m;
            }
            while(rs.next()) {
                int theirLevel = rs.getInt(2);
                if(theirLevel > UserRegistry.VETTER) {
                    continue; // will not receive notice.
                }
                
                int theirId = rs.getInt(1);
                UserRegistry.User u = reg.getInfo(theirId);
                //String theirName = rs.getString(3);
                //String theirEmail = rs.getString(4);
                //String theirOrg = rs.getString(5);
                String theirLocales = rs.getString(6);                
                String theirIntlocs = rs.getString(7);
                
                String localeArray[] = UserRegistry.tokenizeLocale(theirLocales);
                
                if((theirId <= UserRegistry.TC) || (localeArray.length == 0)) { // all locales
                    localeArray = UserRegistry.tokenizeLocale(theirIntlocs);
                }
                
                if(localeArray.length == 0) {
                    for(Iterator li = intGroups.keySet().iterator();li.hasNext();) {
                        String group = (String)li.next();
                        Set v = (Set)m.get(group);
                        if(v == null) {
                            v=new HashSet();
                            m.put(group, v);
                        }
                        v.add(u);
                    //    System.err.println(group + " - " + u.email + " (ALL)");
                    }
                } else {
                    for(int i=0;i<localeArray.length;i++) {
                        String group= localeArray[i];
                        Set v = (Set)m.get(group);
                        if(v == null) {
                            v=new HashSet();
                            m.put(group, v);
                        }
                        v.add(u);
                 //       System.err.println(group + " - " + u.email + "");
                    }
                }
            }
        }
    } catch (SQLException se) {
        throw new RuntimeException("SQL error querying users for getIntUsers - " + SurveyMain.unchainSqlException(se));
    }
    return m;
}


void writeRadio(WebContext ctx,String xpath,String type,String value,String checked) {
    writeRadio(ctx, xpath, type, value, checked.equals(value));        
}

void writeRadio(WebContext ctx,String xpath,String type,String value,boolean checked) {
    ctx.println("<input type=radio name='" + fieldsToHtml(xpath,type) + "' value='" + value + "' " +
                (checked?" CHECKED ":"") + "/>");
    ///*srl*/        ctx.println("<sup><tt>" + fieldsToHtml(xpath,type) + "</tt></sup>");
}


public static final com.ibm.icu.text.Transliterator hexXML
    = com.ibm.icu.text.Transliterator.getInstance(
    "[^\\u0009\\u000A\\u0020-\\u007E\\u00A0-\\u00FF] Any-Hex/XML");

// like above, but quote " 
public static final com.ibm.icu.text.Transliterator quoteXML 
    = com.ibm.icu.text.Transliterator.getInstance(
     "[^\\u0009\\u000A\\u0020-\\u0021\\u0023-\\u007E\\u00A0-\\u00FF] Any-Hex/XML");

// cache of documents
static Hashtable docTable = new Hashtable();
static Hashtable docVersions = new Hashtable();


public static int usedK() {
    Runtime r = Runtime.getRuntime();
    double total = r.totalMemory();
    total = total / 1024;
    double free = r.freeMemory();
    free = free / 1024;
    return (int)(Math.floor(total-free));
}


private void busted(String what) {
    System.err.println("SurveyTool busted: " + what + " ( after " +pages +"html+"+xpages+"xml pages served, uptime " + uptime.toString()  + ")");
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

/**
* convert a XPATH:TYPE form to an html field.
 * if type is null, means:  hash the xpath
 */
String fieldsToHtml(String xpath, String type)
{
    if(type == null) {
        String r = (String)allXpaths.get(xpath);
        if(r == null) synchronized(allXpaths) {
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
static final String SHORT_A = "(Click to show entire warning.)";
static final String SHORT_B = "(hide.)";
private synchronized void printShortened(WebContext ctx, String str) {
    if(str.length()<(MAX_CHARS+1+SHORT_A.length())) {
        ctx.println(str);
    } else {
        String key = CookieSession.cheapEncode(shortN++);
        printShortened(ctx,str.substring(0,MAX_CHARS), str, key); 
    }
}

private void printShortened(WebContext ctx, String shortStr, String longStr, String warnHash ) {
        ctx.println("<span id='h_ww"+warnHash+"'>" + shortStr + "... ");
        ctx.print("<a href='javascript:show(\"ww" + warnHash + "\")'>" + 
                    SHORT_A+"</a></span>");
        ctx.println("<!-- <noscript>Warning: </noscript> -->" + 
                    "<span style='display: none'  id='ww" + warnHash + "'>" +
                    longStr + "<a href='javascript:hide(\"ww" + warnHash + "\")'>" + 
                    SHORT_B+"</a></span>");
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
static String dbInfo = null;

private void doStartupDB()
{
    dbDir = new File(cldrHome,CLDR_DB);
    dbDir_u = new File(cldrHome,CLDR_DB_U);
    boolean doesExist = dbDir.isDirectory();
///*U*/	    boolean doesExist_u = dbDir_u.isDirectory();
    
//    logger.info("SurveyTool setting up database.. " + dbDir.getAbsolutePath());
    try
    { 
        Object o = Class.forName(db_driver).newInstance();
        try {
            java.sql.Driver drv = (java.sql.Driver)o;
            dbInfo = "v"+drv.getMajorVersion()+"."+drv.getMinorVersion();
         //   dbInfo = dbInfo + " " +org.apache.derby.tools.sysinfo.getProductName()+" " +org.apache.derby.tools.sysinfo.getVersionString();
        } catch(Throwable t) {
            dbInfo = "unknown";
        }
//        logger.info("loaded driver " + o + " - " + dbInfo);
        Connection conn = getDBConnection(";create=true");
//        logger.info("Connected to database " + cldrdb);
///*U*/        Connection conn_u = getU_DBConnection(";create=true");
//        logger.info("Connected to user database " + cldrdb_u);
        
        // set up our main tables.
        if(doesExist == false) {
            // nothing to setup.
        }
        conn.commit();        
        conn.close(); 
        
///*U*/        conn_u.commit();
///*U*/        conn_u.close();
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
        Connection uConn = getDBConnection(); ///*U*/ was:  getU_DBConnection
        boolean doesExist_u = hasTable(uConn, UserRegistry.CLDR_USERS);
        reg = UserRegistry.createRegistry(logger, uConn, !doesExist_u);
        if(!doesExist_u) { // only import users on first run through..
            reg.importOldUsers(vetdata);
        }
		
		String doMigrate = survprops.getProperty("CLDR_MIGRATE");
		if((doMigrate!=null) && (doMigrate.length()>0)) {
			System.err.println("** User DB migrate");
			reg.migrateFrom(getU_DBConnection());
		}
		
    } catch (SQLException e) {
        busted("On UserRegistry startup: " + unchainSqlException(e));
    }
    try {
        xpt = XPathTable.createTable(logger, getDBConnection(), !doesExist, this);
    } catch (SQLException e) {
        busted("On XPathTable startup: " + unchainSqlException(e));
    }
    // note: make xpt before CLDRDBSource..
    try {
        CLDRDBSource.setupDB(logger, getDBConnection(), !doesExist);
    } catch (SQLException e) {
        busted("On CLDRDBSource startup: " + unchainSqlException(e));
    }
    try {
        vet = Vetting.createTable(logger, getDBConnection(), this);
    } catch (SQLException e) {
        busted("On Vetting startup: " + unchainSqlException(e));
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
        String stackStr = "\n unknown Stack";
        try {
            StringWriter asString = new StringWriter();
            e.printStackTrace(new PrintWriter(asString));
            stackStr = "\n Stack: \n " + asString.toString();
        } catch ( Throwable tt ) {
            // ...
        }
        return echain + stackStr;
    }

    public boolean hasTable(Connection conn, String table) {
        try {
            DatabaseMetaData dbmd = conn.getMetaData();
            ResultSet rs = dbmd.getTables(null, null, table.toUpperCase(), null);

            if(rs.next()==true) {
                rs.close();
//                System.err.println("table " + table + " did exist.");
                return true;
            } else {
                System.err.println("table " + table + " did not exist.");
                return false;
            }
        } catch (SQLException se)
        {
            busted("While looking for table '" + table + "': " + unchainSqlException(se));
            return false; // NOTREACHED
        }
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
            System.out.println("sm created.");
            sm.doStartup();
            System.out.println("sm started.");
            sm.doStartupDB();
            System.out.println("DB started.");
            /*  sm.smok(4);
            sm.smok(3);
            sm.smok(2);
            sm.smok(1);
            sm.smok(33);
            sm.smok(333);
            sm.smok(1333);  */
            String ourXpath = "//ldml/numbers";
            
            System.out.println("xpath xpt.getByXpath("+ourXpath+") = " + sm.xpt.getByXpath(ourXpath));
/*            
            
            if(arg.length>0) {
                WebContext xctx = new WebContext(false);
                xctx.sm = sm;
                xctx.session=new CookieSession(true);
                for(int i=0;i<arg.length;i++) {
                    System.out.println("loading stuff for " + arg[i]);
                    xctx.setLocale(new ULocale(arg[i]));
                    
                    WebContext ctx = xctx;
                    System.out.println("  - loading CLDRFile and stuff");
                    UserLocaleStuff uf = sm.getUserFile(...
                    CLDRFile cf = sm.getUserFile(ctx, (ctx.session.user==null)?null:ctx.session.user, ctx.locale);
                    if(cf == null) {
                        throw new InternalError("CLDRFile is null!");
                    }
                    CLDRDBSource ourSrc = (CLDRDBSource)ctx.getByLocale(USER_FILE + CLDRDBSRC); // TODO: remove. debuggin'
                    if(ourSrc == null) {
                        throw new InternalError("oursrc is null! - " + (USER_FILE + CLDRDBSRC) + " @ " + ctx.locale );
                    }
                    CheckCLDR checkCldr =  (CheckCLDR)ctx.getByLocale(USER_FILE + CHECKCLDR+":"+ctx.defaultPtype());
                    if (checkCldr == null)  {
                        List checkCldrResult = new ArrayList();
                        System.err.println("Initting tests . . .");
                        long t0 = System.currentTimeMillis();
        */
                      //  checkCldr = CheckCLDR.getCheckAll(/* "(?!.*Collision.*).*" */  ".*");
        /*                
                        checkCldr.setDisplayInformation(sm.getEnglishFile());
                        if(cf==null) {
                            throw new InternalError("cf was null.");
                        }
                        checkCldr.setCldrFileToCheck(cf, ctx.getOptionsMap(), checkCldrResult);
                        System.err.println("fileToCheck set . . . on "+ checkCldr.toString());
                        ctx.putByLocale(USER_FILE + CHECKCLDR+":"+ctx.defaultPtype(), checkCldr);
                        {
                            // sanity check: can we get it back out
                            CheckCLDR subCheckCldr = (CheckCLDR)ctx.getByLocale(SurveyMain.USER_FILE + SurveyMain.CHECKCLDR+":"+ctx.defaultPtype());
                            if(subCheckCldr == null) {
                                throw new InternalError("subCheckCldr == null");
                            }
                        }
                        if(!checkCldrResult.isEmpty()) {
                            ctx.putByLocale(USER_FILE + CHECKCLDR_RES+":"+ctx.defaultPtype(), checkCldrResult); // don't bother if empty . . .
                        }
                        long t2 = System.currentTimeMillis();
                        System.err.println("Time to init tests " + arg[i]+": " + (t2-t0));
                    }
                    System.err.println("getPod:");
                    xctx.getPod("//ldml/numbers");
*/
                
                /*
                    System.out.println("loading dbsource for " + arg[i]);
                    CLDRDBSource dbSource = CLDRDBSource.createInstance(sm.fileBase, sm.xpt, new ULocale(arg[i]),
                                                                        sm.getDBConnection(), null);            
                    System.out.println("dbSource created for " + arg[i]);
                    CLDRFile my = new CLDRFile(dbSource,false);
                    System.out.println("file created ");
                    CheckCLDR check = CheckCLDR.getCheckAll("(?!.*Collision.*).*");
                    System.out.println("check created");
                    List result = new ArrayList();
                    Map options = null;
                    check.setCldrFileToCheck(my, options, result); 
                    System.out.println("file set .. done with " + arg[i]);
                */
    /*
                }
            } else {
                System.out.println("No locales listed");
            }
    */
            
            System.out.println("done...");
            sm.doShutdownDB();
            System.out.println("DB shutdown.");
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
   public static     String shortClassName(Object o) {
        try {
            String cls = o.getClass().toString();
            int io = cls.lastIndexOf(".");
            if(io!=-1) {
                cls = cls.substring(io+1,cls.length());
            }  
            return cls;
        } catch (NullPointerException n) {
            return null;
        }
    }

}
