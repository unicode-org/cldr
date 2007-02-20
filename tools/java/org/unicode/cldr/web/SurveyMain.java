/*
 ******************************************************************************
 * Copyright (C) 2004-2007, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.web;

import java.io.*;
import java.util.*;
import java.net.InetAddress;
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

import com.ibm.icu.text.DateTimePatternGenerator;
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

/**
 * The main servlet class of Survey Tool
 */
public class SurveyMain extends HttpServlet {

    // phase?
    public static final boolean phaseSubmit = true; 
    public static final boolean phaseVetting = false;
    public static final boolean phaseClosed = false;
    public static final boolean phaseDisputed = false;
	public static final boolean phaseReadonly = false;
    
    public static final boolean phaseBeta = true;

    // Unofficial?
    public static       boolean isUnofficial = true;

    // Special bug numbers.
    public static final String BUG_METAZONE_FOLDER = "data";
    public static final int    BUG_METAZONE = 1262;
    public static final String BUG_ST_FOLDER = "tools";
    public static final int    BUG_ST = 1263;
    
    public static final String URL_HOST = "http://www.unicode.org/";

    public static final String URL_CLDR = URL_HOST+"cldr/";
    public static final String BUG_URL_BASE = URL_CLDR+"bugs/locale-bugs";
    
    
    /**
    * URL prefix for  help
    */
    public static final String CLDR_WIKI_BASE = URL_CLDR+"wiki";
    public static final String CLDR_HELP_LINK = CLDR_WIKI_BASE+"?SurveyToolHelp";
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
    static String fileBase = null; // not static - may change lager
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
    static final String QUERY_DO = "do";
	
    static final String SURVEYTOOL_COOKIE_SESSION = "com.org.unicode.cldr.web.CookieSession.id";
    static final String SURVEYTOOL_COOKIE_NONE = "0";
    static final String PREF_SHOWCODES = "p_codes";
    static final String PREF_SORTMODE = "p_sort";
    static final String PREF_NOPOPUPS = "p_nopopups";
    static final String PREF_SORTMODE_CODE = "code";
    static final String PREF_SORTMODE_ALPHA = "alpha";
    static final String PREF_SORTMODE_WARNING = "interest";
    static final String PREF_SORTMODE_NAME = "name";
    static final String PREF_SORTMODE_DEFAULT = PREF_SORTMODE_WARNING;
    
    static final String  BASELINE_ID = "en";
    static final ULocale BASELINE_LOCALE = new ULocale(BASELINE_ID);
    static final String  BASELINE_NAME = BASELINE_LOCALE.getDisplayName(BASELINE_LOCALE);
    
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
    public SurveyForum  fora = null;
    public LocaleChangeRegistry lcr = new LocaleChangeRegistry();
    
    public static String xSAVE = phaseBeta?"Save [Note: SurveyTool is in BETA]":"Save Changes";
    
    /*private int n = 0;
    synchronized int getN() {
        return n++;
    }*/
                    
    /** status **/
    ElapsedTimer uptime = new ElapsedTimer("ST uptime: {0}");
    ElapsedTimer startupTime = new ElapsedTimer("{0} until first GET/POST");
    public static String isBusted = null;
    ServletConfig config = null;
            
    public final void init(final ServletConfig config)
    throws ServletException {
        new com.ibm.icu.text.SimpleDateFormat();
    
        super.init(config);
        
        cldrHome = config.getInitParameter("cldr.home");
        this.config = config;
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
        
        /* 
            prevent spidering of the site
        */
        if(request.getRemoteAddr().equals("66.154.103.161") ||
		   request.getRemoteAddr().equals("203.148.64.17") ||
           request.getRemoteAddr().equals("38.98.120.72") || // 38.98.120.72 - feb 7, 2007-  lots of connections
           false) {
            response.sendRedirect(URL_CLDR);
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
                out.println("<link rel='stylesheet' type='text/css' href='"+ request.getContextPath() + "/" + "surveytool.css" + "'>");
                out.println("</head>");
                out.println("<body>");
                showSpecialHeader(out);
                out.println("<h1>CLDR Survey Tool is down, because:</h1>");
                out.println("<pre class='ferrbox'>" + isBusted + "</pre><br>");
                out.println("<hr>");
                out.println("Please try this link for info: <a href='"+CLDR_WIKI_BASE+"?SurveyTool/Status'>"+CLDR_WIKI_BASE+"?SurveyTool/Status</a><br>");
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
        printHeader(ctx, "SQL Console@"+localhost());
        String q = ctx.field("q");
        boolean tblsel = false;        
        printAdminMenu(ctx, "/AdminSql");
        ctx.println("<h1>SQL Console</h1>");
        if(q.length() == 0) {
            q = "select tablename from SYS.SYSTABLES where tabletype='T'";
            tblsel = true;
        } else {
            ctx.println("<a href='" + ctx.base() + "?sql=" + vap + "'>[List of Tables]</a>");
        }
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
                    
                    ctx.println("<table summary='SQL Results' class='sqlbox' border='2'><tr><th>#</th>");
                    for(i=1;i<=cc;i++) {
                        ctx.println("<th>"+rsm.getColumnName(i)+ "<br>");
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
                    if(tblsel) {
                        ctx.println("<th>Info</th><th>Rows</th>");
                    }
                    ctx.println("</tr>");
                    int limit = 30;
                    if(ctx.field("unltd").length()>0) {
                        limit = 9999999;
                    }
                    for(j=0;rs.next()&&(j<limit);j++) {
                        ctx.println("<tr class='r"+(j%2)+"'><th>" + j + "</th>");
                        for(i=1;i<=cc;i++) {
                            String v = rs.getString(i);
                            if(v != null) {
                                    ctx.println("<td>" + v + "</td>");
                                if(tblsel == true) {
                                    ctx.println("<td>");
                                    ctx.println("<form method=POST action='" + ctx.base() + "'>");
                                    ctx.println("<input type=hidden name=sql value='" + vap + "'>");
                                    ctx.println("<input type=hidden name=q value='" + "select * from "+v+" where 1 = 0'>");
                                    ctx.println("<input type=image src='"+ctx.context("zoom"+".png")+"' value='Info'></form>");
                                    ctx.println("</td><td>");
                                    int count = sqlCount(ctx, conn, "select COUNT(*) from " + v);
                                    ctx.println(count+"</td>");
                                }
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
                
                ctx.println("<pre class='ferrbox'>" + complaint + "</pre>" );
                logger.severe(complaint);
            } catch(Throwable t) {
                String complaint = t.toString();
                ctx.println("<pre class='ferrbox'>" + complaint + "</pre>" );
                logger.severe("Err in SQL execute: " + complaint);
            }
            
            try {
                s.close();
            } catch(SQLException se) {
                String complaint = "in s.closing: SQL err: " + unchainSqlException(se);
                
                ctx.println("<pre class='ferrbox'> " + complaint + "</pre>" );
                logger.severe(complaint);
            } catch(Throwable t) {
                String complaint = t.toString();
                ctx.println("<pre class='ferrbox'> " + complaint + "</pre>" );
                logger.severe("Err in SQL close: " + complaint);
            }

/*  auto closed - dont manually close.
            try {
                conn.close();
            } catch(SQLException se) {
                String complaint = "in conn.closing: SQL err: " + unchainSqlException(se);
                
                ctx.println("<pre class='ferrbox'> " + complaint + "</pre>" );
                logger.severe(complaint);
            } catch(Throwable t) {
                String complaint = t.toString();
                ctx.println("<pre class='ferrbox'> " + complaint + "</pre>" );
                logger.severe("Err in SQL close: " + complaint);
            }
*/

        }
        printFooter(ctx);
    }
    
    public static String freeMem() {
        Runtime r = Runtime.getRuntime();
        double total = r.totalMemory();
        total = total / 1024000.0;
        double free = r.freeMemory();
        free = free / 1024000.0;
        return "Free memory: " + (int)free + "M / " + total + "M";
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
    
	void printAdminMenu(WebContext ctx, String helpLink) {
    
        boolean isDump = ctx.hasField("dump");
        boolean isSql = ctx.hasField("sql");
    
        ctx.print("<div style='float: right'><a class='notselected' href='" + ctx.base() + "'><b>[SurveyTool main]</b></a> | ");
        ctx.print("<a class='"+(isDump?"":"not")+"selected' href='" + ctx.base() + "?dump="+vap+"'>Admin</a>");
        ctx.print(" | ");
        ctx.print("<a class='"+(isSql?"":"not")+"selected' href='" + ctx.base() + "?sql="+vap+"'>SQL</a>");
        ctx.print("<br>");
        ctx.printHelpLink(helpLink, "Admin Help", true);
        ctx.println("</div>");
    }
    
    private void doDump(WebContext ctx)
    {
        String action = ctx.field("action");
        printHeader(ctx, "ST Admin@"+localhost() + " | " + action);
        printAdminMenu(ctx, "/AdminDump");
        ctx.println("<h1>SurveyTool Administration</h1>");
        ctx.println("<hr>");
        
        
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
        printMenu(actionCtx, action, "srl", "EXPERT-ADMIN-use-only", "action");  
		
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
            /*
            printMenu(actionCtx, action, "srl_vet_nag", "MAIL: vet nag [weekly]", "action");       
            actionCtx.println(" | ");
            printMenu(actionCtx, action, "srl_vet_upd", "MAIL: vote change [daily]", "action");       
            actionCtx.println(" | ");
            */
            /*
            printMenu(actionCtx, action, "srl_db_update", "Update <tt>base_xpath</tt>", "action");       
            actionCtx.println(" | ");
            */
            printMenu(actionCtx, action, "srl_vet_wash", "Clear out old votes", "action");       
            actionCtx.println(" | ");
            
            printMenu(actionCtx, action, "srl_twiddle", "twiddle params", "action");       
            ctx.println("</div></ul>");
        }
        actionCtx.println("<br>");
        
		/* Begin sub pages */
		
        if(action.equals("stats")) {
            ctx.println("<div class='pager'>");
            ctx.println("DB version " + dbInfo+ ",  ICU " + com.ibm.icu.util.VersionInfo.ICU_VERSION+
                ", Container: " + config.getServletContext().getServerInfo()+"<br>");
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
            
            ctx.println("<a class='notselected' href='" + ctx.jspLink("about.jsp") +"'>More version information...</a><br/>");
            
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
			
			
        } else if(action.equals("srl_vet_wash")) {
            WebContext subCtx = new WebContext(ctx);
            actionCtx.addQuery("action",action);
            ctx.println("<br>");
            String what = actionCtx.field("srl_vet_wash");
            if(what.equals("ALL")) {
                ctx.println("<h4>Remove Old Votes. (in preparation for a new CLDR - do NOT run this after start of vetting)</h4>");
                ElapsedTimer et = new ElapsedTimer();
                int n = vet.washVotes();
                ctx.println("Done washing "+n+" vote results in: " + et + "<br>");
                int stup = vet.updateStatus();
                ctx.println("Updated " + stup + " statuses.<br>");
            } else {
                ctx.println("All: [ <a href='"+actionCtx.url()+actionCtx.urlConnector()+action+"=ALL'>Wash all</a> ]<br>");
                if(what.length()>0) {
                    ctx.println("<h4>Wash "+what+"</h4>");
                    ElapsedTimer et = new ElapsedTimer();
                    int n = vet.washVotes(what);
                    ctx.println("Done washing "+n+" vote results in: " + et + "<br>");
                    int stup = vet.updateStatus();
                    ctx.println("Updated " + stup + " statuses.<br>");
                }
            }
            actionCtx.println("<form method='POST' action='"+actionCtx.url()+"'>");
            actionCtx.printUrlAsHiddenFields();
            actionCtx.println("Update just: <input name='"+action+"' value='"+what+"'><input type='submit' value='Wash'></form>");
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
            resetLocaleCaches();
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
		} else if(action.equals("srl_vxport")) {
			System.err.println("vxport");
			File inFiles[] = getInFiles();
			int nrInFiles = inFiles.length;
			boolean found = false;
			String theLocale = null;
			File outdir = new File("./xport/");
			for(int i=0;(!found) && (i<nrInFiles);i++) {
			 try{
				String localeName = inFiles[i].getName();
				int dot = localeName.indexOf('.');
				theLocale = localeName.substring(0,dot);
				System.err.println("#vx "+theLocale);
				CLDRDBSource dbSource = makeDBSource(null, new ULocale(theLocale), true);
				CLDRFile file = makeCLDRFile(dbSource);
				  OutputStream files = new FileOutputStream(new File(outdir,localeName),false); // Append
//				  PrintWriter pw = new PrintWriter(files);
	//            file.write(WebContext.openUTF8Writer(response.getOutputStream()));
				PrintWriter ow;
				file.write(ow=WebContext.openUTF8Writer(files));
				ow.close();
//				pw.close();
				files.close();
				
				} catch(IOException exception){
				  System.err.println(exception);
				  // TODO: log this ... 
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
                com.ibm.icu.dev.test.util.ElapsedTimer allTime = new com.ibm.icu.dev.test.util.ElapsedTimer("Time to load all: {0}");
                logger.info("Loading all..");            
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
        ctx.println("<title>CLDR Vetting | ");
        if(ctx.locale != null) {
            ctx.print(ctx.locale.getDisplayName(ctx.displayLocale) + " | ");
        }
        ctx.println(title + "</title>");
        ctx.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
        ctx.println("</head>");
        ctx.println("<body>");
        if(isUnofficial) {
            ctx.print("<div title='Not an official SurveyTool' style='text-align: center; margin: 0; background-color: goldenrod;'>");
            ctx.printHelpLink("/Unofficial",ctx.iconThing("warn","Unofficial Site")+"Unofficial");
            ctx.println("</div>");
        }
        if(phaseBeta) {
            ctx.print("<div title='Survey Tool is in Beta' style='text-align: center; margin: 0; background-color: yellow;'>");
            ctx.printHelpLink("/Beta",ctx.iconThing("warn","beta")+"SurveyTool is in Beta");
            ctx.println("</div>");
        }
        showSpecialHeader(ctx);
        ctx.println("<script type='text/javascript'><!-- \n" +
                    "function show(what)\n" +
                    "{document.getElementById(what).style.display=\"block\";\ndocument.getElementById(\"h_\"+what).style.display=\"none\";}\n" +
                    "function hide(what)\n" +
                    "{document.getElementById(what).style.display=\"none\";\ndocument.getElementById(\"h_\"+what).style.display=\"block\";}\n" +
                    "--></script>");
        
    }
    
    void showSpecialHeader(WebContext ctx) {
        showSpecialHeader(ctx, ctx.getOut());
    }
    
    void showSpecialHeader(PrintWriter out) {
        showSpecialHeader(null, out);
    }    
    
    /**
     * print the top news banner. this must callable by non-context functions.
     * @param out output stream - must be set
     * @param ctx context - optional. 
     */
    void showSpecialHeader(WebContext ctx, PrintWriter out) {
        if((specialHeader != null) && (specialHeader.length()>0)) {
            out.println("<div class='specialHeader'>");
            if(ctx != null) {
                ctx.printHelpLink("/BannerMessage","News",true,false);
                out.println(": &nbsp; ");
            }
            out.println(specialHeader);
            if(specialTimer != 0) {
                long t0 = System.currentTimeMillis();
                out.print("<br><b>Timer:</b> ");
                if(t0 > specialTimer) {
                    out.print("<b>The countdown time has arrived.</b>");
                } else {
                    out.print("The countdown timer has " + timeDiff(t0,specialTimer) +" remaining on it.");
                }
            }
            out.print("<br>");
            out.println("</div><br>");
        }
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
        ctx.println("<a href='http://www.unicode.org'>Unicode</a> | <a href='"+URL_CLDR+"'>Common Locale Data Repository</a>");
        try {
            Map m = new TreeMap(ctx.request.getParameterMap());
            m.remove("sql");
            m.remove("pw");
            m.remove("email");
            m.remove("dump");
            m.remove("s");
            m.remove("udump");
            String u = "";
            for(Enumeration e=ctx.request.getParameterNames();e.hasMoreElements();)  {
                String k = e.nextElement().toString();
                String v;
                if(k.equals("sql")||k.equals("pw")||k.equals("email")||k.equals("dump")||k.equals("s")||k.equals("udump")) {
                    v = "";
                } else {
                    v = ctx.request.getParameterValues(k)[0];
                }
                u=u+"|"+k+"="+v;
            }
            ctx.println("| <a href='" + bugReplyUrl(BUG_ST_FOLDER, BUG_ST, "Feedback on ?" + u)+"'>Feedback</a>");
        } catch (Throwable t) {
            System.err.println(t.toString());
            t.printStackTrace();
        }
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

    /* print a user table without any extra help in it */
    public void printUserTable(WebContext ctx) {
        printUserTableWithHelp(ctx, null, null);
    }

    public void printUserTableWithHelp(WebContext ctx, String helpLink) {
        printUserTableWithHelp(ctx, helpLink, null);
    }
    
    public void printUserTableWithHelp(WebContext ctx, String helpLink, String helpName) {
        printUserTableBegin(ctx);
        if(helpLink != null) {
            ctx.println(" | ");
            if(helpName != null) {
                ctx.printHelpLink(helpLink, helpName);
            } else {
                ctx.printHelpLink(helpLink, "Page&nbsp;Instructions");
            }
        }
        printUserTableMiddle(ctx);
        printUserTableEnd(ctx);
    }
    
    /***
     * print table that holds the menu of choices
     */
    public void printUserTableBegin(WebContext ctx) {
            ctx.println("<table summary='header' border='0' cellpadding='0' cellspacing='0' style='border-collapse: collapse' "+
                        " width='100%' bgcolor='#EEEEEE'>"); //bordercolor='#111111'
            ctx.println("<tr><td>");
            ctx.printHelpLink("","General&nbsp;Instructions"); // base help
    }
    
    public void printUserTableMiddle(WebContext ctx) {
            ctx.println("</td><td align='right'>");
            printUserMenu(ctx);
    }
    public void printUserTableEnd(WebContext ctx) {
            ctx.println("</td></tr></table>");
    }
    /**
     * Print menu of choices available to the user.
     */
    public void printUserMenu(WebContext ctx) {
        String doWhat = ctx.field(QUERY_DO);
        if(ctx.session.user == null)  {
            ctx.println("You are a <b>Visitor</b>. <a class='notselected' href='" + ctx.jspLink("login.jsp") +"'>Login</a><br>");
            
            printMenu(ctx, doWhat, "disputed", "Disputed", QUERY_DO);
            ctx.print(" | ");
            printMenu(ctx, doWhat, "options", "My Options", QUERY_DO);
            ctx.println("<br>");
        } else {
            ctx.println("<b>Welcome " + ctx.session.user.name + " (" + ctx.session.user.org + ") !</b>");
            ctx.println("<a class='notselected' href='" + ctx.base() + "?do=logout'>Logout</a><br>");
            printMenu(ctx, doWhat, "disputed", "Disputed", QUERY_DO);
            ctx.print(" | ");
            printMenu(ctx, doWhat, "options", "My Options", QUERY_DO);
            ctx.print(" | ");
            printMenu(ctx, doWhat, "listu", "My Account", QUERY_DO);
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
                printMenu(ctx, doWhat, "list", "Manage " + ctx.session.user.org + " Users", QUERY_DO);
                ctx.print(" | ");
                printMenu(ctx, doWhat, "coverage", "Coverage", QUERY_DO);                    
            } else {
                if(UserRegistry.userIsVetter(ctx.session.user)) {
                    if(UserRegistry.userIsExpert(ctx.session.user)) {
                        ctx.print("You are an: <b>Expert Vetter:</b> ");
                    } else {
                        ctx.println("You are a: <b>Vetter:</b> ");
                    }
                    printMenu(ctx, doWhat, "list", "List " + ctx.session.user.org + " Users", QUERY_DO);
                    ctx.print(" | ");
                    printMenu(ctx, doWhat, "coverage", "Coverage", QUERY_DO);                    
                } else if(UserRegistry.userIsStreet(ctx.session.user)) {
                    ctx.println("You are a: <b>Guest Contributor</b> ");
                } else if(UserRegistry.userIsLocked(ctx.session.user)) {
                    ctx.println("<b>LOCKED: Note: your account is currently locked. Please contact " + ctx.session.user.org + "'s CLDR Technical Committee member.</b> ");
                }
            }
            if(SurveyMain.phaseReadonly) {
				ctx.println("(The SurveyTool is in a read-only state, no changes may be made.)");
			} else if(SurveyMain.phaseVetting 
                && UserRegistry.userIsStreet(ctx.session.user)
                && !UserRegistry.userIsExpert(ctx.session.user)) {
                ctx.println(" (Note: in the Vetting phase, you may not submit new data.) ");
            } else if(SurveyMain.phaseClosed && !UserRegistry.userIsTC(ctx.session.user)) {
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
        printUserTableWithHelp(ctx, "/AddModifyUser");
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
                nuCtx.addQuery(QUERY_DO,"list");
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

    public static void showCoverageLanguage(WebContext ctx, String group, String lang) {
        ctx.print("<tt style='border: 1px solid gray; margin: 1px; padding: 1px;' class='codebox'>"+lang+"</tt> ("+new ULocale(lang).getDisplayName(ctx.displayLocale)+":<i>"+group+"</i>)</tt>" );
    }
    
    public void doCoverage(WebContext ctx) {
        boolean showCodes = false; //ctx.prefBool(PREF_SHOWCODES);
        printHeader(ctx, "Locale Coverage");

        printUserTableWithHelp(ctx, "/LocaleCoverage");

        ctx.println("<a href='" + ctx.jspLink("adduser.jsp") +"'>[Add User]</a> |");
//        ctx.println("<a href='" + ctx.url()+ctx.urlConnector()+"do=list'>[List Users]</a>");
        ctx.print("<br>");
        ctx.println("<a href='" + ctx.url() + "'><b>SurveyTool main</b></a><hr>");
        String org = ctx.session.user.org;
        if(UserRegistry.userCreateOtherOrgs(ctx.session.user)) {
            org = null; // all
        }
        
        StandardCodes sc = StandardCodes.make();

        WebContext subCtx = new WebContext(ctx);
        subCtx.setQuery(QUERY_DO,"coverage");
        boolean participation = showTogglePref(subCtx, "cov_participation", "Participation Shown");
        String missingLocalesForOrg = org;
        if(missingLocalesForOrg == null) {
            missingLocalesForOrg = showListPref(subCtx,PREF_COVTYP, "Coverage Type", ctx.getLocaleTypes(), true);
        }
        if(missingLocalesForOrg == null || missingLocalesForOrg.length()==0 || missingLocalesForOrg.equals("default")) {
            missingLocalesForOrg = "default"; // ?!
        }
        
        ctx.println("<h4>Showing coverage for: " + org + "</h4>");
        if(missingLocalesForOrg != org) {
            ctx.println("<h4> (and missing locales for " + missingLocalesForOrg +")</h4>");
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
			
				
                if(participation && (conn!=null)) {
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
                       Hashtable theHash = localeStatus;
                       String userInfo = nameLink+" ";
					   if(participation && conn != null) {
							psnSubmit.setString(2,theLocale);
							psnVet.setString(2,theLocale);
							
							int nSubmit=sqlCount(ctx,conn,psnSubmit);
							int nVet=sqlCount(ctx,conn,psnVet);
							
							if((nSubmit+nVet)==0) {
								theHash = nullStatus; // vetter w/ no work done
							}
                        
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
                        }
                        Hashtable oldStr = (Hashtable)theHash.get(theLocale);
                        
                        
                        if(oldStr==null) {
                            oldStr = new Hashtable();
                            theHash.put(theLocale,oldStr);
                        } else {
                        //     // oldStr = oldStr+"<br>\n";
                        }

                        oldStr.put(new Integer(theirId), userInfo + "<!-- " + theLocale + " -->");
					   
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
            ctx.println("<B>Warning: locale designations not matching locales in CLDR:</b> ");
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
        
        // Now, calculate coverage of requested locales for this organization
        //sc.getGroup(locale,missingLocalesForOrg);
        Set languagesNotInCLDR = new TreeSet();
        Set languagesMissing = new HashSet();
        Set allLanguages = sc.getAvailableCodes("language");
        for(Iterator li = allLanguages.iterator();li.hasNext();) {
            String lang = (String)(li.next());
            String group = sc.getGroup(lang, missingLocalesForOrg);
            if(group != null ) {
                //System.err.println("getGroup("+lang+", " + missingLocalesForOrg + ") = " + group);
                if(!isValidLocale(lang)) {
                    //System.err.println("-> not in lm: " + lang);
                    languagesNotInCLDR.add(lang);
                } else {
                    if(!s.contains(lang)) {
                     //   System.err.println("-> not in S: " + lang);
                        languagesMissing.add(lang);
                    } else {
                       // System.err.println("in lm && s: " + lang);
                    }
                }
            }
        }

        if(!languagesNotInCLDR.isEmpty()) {
            ctx.println("<p class='hang'>"+ctx.iconThing("stop","locales missing from CLDR")+"<B>Required by " + missingLocalesForOrg + " but not in CLDR: </b>");
            boolean first=true;
            for(Iterator li = languagesNotInCLDR.iterator();li.hasNext();) {
                if(first==false) {
                    ctx.print(", ");
                } else {
                    first = false;
                }
                String lang = li.next().toString();
                showCoverageLanguage(ctx,sc.getGroup(lang, missingLocalesForOrg), lang);
            }
            ctx.println("<br>");
			ctx.printHelpLink("/LocaleMissing","Locale is missing from CLDR");
            ctx.println("</p><br>");
        }

        if(!languagesMissing.isEmpty()) {
            ctx.println("<p class='hang'>"+ctx.iconThing("stop","locales without vetters")+
                "<B>Required by " + missingLocalesForOrg + " but no vetters: </b>");
            boolean first=true;
            for(Iterator li = languagesMissing.iterator();li.hasNext();) {
                if(first==false) {
                    ctx.print(", ");
                } else {
                    first = false;
                }
                String lang = li.next().toString();
                showCoverageLanguage(ctx, sc.getGroup(lang, missingLocalesForOrg), lang);
            }
            ctx.println("<br>");
			//ctx.printHelpLink("/LocaleMissing","Locale is missing from CLDR");
            ctx.println("</p><br>");            
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
            if(languagesMissing.contains(aLocale)) {
                ctx.println("<br>"+ctx.iconThing("stop","No " + missingLocalesForOrg + " vetters")+ "<i>(coverage: "+ sc.getGroup(aLocale, missingLocalesForOrg)+")</i>");
            }

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
			if(participation && nullStatus!=null && !nullStatus.isEmpty()) {
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
            
            TreeMap sm = (TreeMap)subLocales.get(aLocale);  // sub locales 
            
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
					if(participation && nullStatus!=null && !nullStatus.isEmpty()) {
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
            if(participation) {
                ctx.println("Selected users have submitted " + totalSubmit + " items, and voted for " + totalVet + " items (including implied votes).<br>");
            }
            int totalResult=sqlCount(ctx,conn,"select COUNT(*) from CLDR_RESULT");
            int totalData=sqlCount(ctx,conn,"select COUNT(id) from CLDR_DATA");
            ctx.println("In all, the SurveyTool has " + totalData + " items (including proposed) and " + totalResult + " items that may need voting.<br>");
			
            if(participation) {
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
        String doWhat = ctx.field(QUERY_DO);
        boolean justme = false; // "my account" mode
        String listName = "list";
        if(just.length()==0) {
            just = null;
        } else {
            just = change40ToAt(just);
            justme = ctx.session.user.email.equals(just);
        }
        if(doWhat.equals("listu")) {
            listName = "listu";
            just = ctx.session.user.email;
            justme = true;
        }
        if(justme) {
            printHeader(ctx, "My Account");
        } else {
            printHeader(ctx, "List Users" + ((just==null)?"":(" - " + just)));
        }
        printUserTableWithHelp(ctx, "/AddModifyUser");
        if(reg.userCanCreateUsers(ctx.session.user)) {
            ctx.println("<a href='" + ctx.jspLink("adduser.jsp") +"'>[Add User]</a> |");
        }
//        ctx.println("<a href='" + ctx.url()+ctx.urlConnector()+"do=coverage'>[Locale Coverage Reports]</a>");
        ctx.print("<br>");
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
                ctx.println("<input type='submit' name='do' value='"+listName+"'></form>");
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
                ctx.println("<input type='hidden' name='do' value='"+listName+"'>");
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
                    ctx.println("<b>active: " + timeDiff(theUser.last) + " ago</b>");
                    if(UserRegistry.userIsAdmin(ctx.session.user)) {
                        ctx.print("<br/>");
                        printLiveUserMenu(ctx, theUser);
                    }
                    ctx.println("</td>");
                } else if(theirLast != null) {
                    ctx.println("<td>");
                    ctx.println("<b>seen: " + timeDiff(theirLast.getTime()) + " ago</b>");
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
                            ctx.println("<div class='odashbox'>"+
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
                        ctx.print("<a href='"+ctx.url()+ctx.urlConnector()+"do=listu&"+LIST_JUST+"="+changeAtTo40(ctx.session.user.email)+
                            "&intlocs_change=b' >[Change this]</a>");
                    }
                    ctx.println("</ul>");
                    
                } // end intlocs
                ctx.println("<br>");
                //ctx.println("<input type='submit' disabled value='Reset Password'>"); /* just mean to show it as disabled */
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
        printHeader(ctx, "Disputed Items Page");
        printUserTableWithHelp(ctx, "/DisputedItems");
        
        ctx.println("<a href='"+ctx.url()+"'>Return to SurveyTool</a><hr>");
        ctx.addQuery(QUERY_DO,"disputed");
        ctx.println("<h2>DisputedItems</h2>");

        vet.doDisputePage(ctx);
        
        printFooter(ctx);
    } 
	
    void doOptions(WebContext ctx) {
        printHeader(ctx, "My Options");
        printUserTableWithHelp(ctx, "/MyOptions");
        
        ctx.println("<a href='"+ctx.url()+"'>Return to SurveyTool</a><hr>");
        ctx.addQuery(QUERY_DO,"options");
        ctx.println("<h2>My Options</h2>");
        ctx.println("<h4>Advanced Options</h4>");
        boolean adv = showTogglePref(ctx, PREF_ADV, "Show Advanced Options");

        // no advanced prefs, now

        showTogglePref(ctx, PREF_NOPOPUPS, "Reduce popup windows");

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

    /**
     * Print the opening form for a 'shopping cart' (one or more showPathList operations)
     * @see showPathList
     * @see printPathListClose
     */
    void printPathListOpen(WebContext ctx) {
        if(ctx.session.user != null) {
            ctx.println("<form name='"+STFORM+"' method=POST action='" + ctx.base() + "'>");
        }
    }
    
    /**
     * Print the closing form for a 'shopping cart' (one or more showPathList operations)
     * @see showPathList
     * @see printPathListOpen
     */
    void printPathListClose(WebContext ctx) {
        if(ctx.session.user != null) {
            ctx.println("</form>");
        }
    }
    
    public void doSession(WebContext ctx) throws IOException
    {
        // which 
        String which = ctx.field(QUERY_SECTION);
        
        setLocale(ctx);
        
        String sessionMessage = setSession(ctx);
        
        if(ctx.hasField(SurveyForum.F_FORUM)) {
            fora.doForum(ctx, sessionMessage);
            return;
        }
        
        // Redirect references to language locale
        if(ctx.field("x").equals("references")) {
            String langLocale = ctx.locale.getLanguage();
            if(!langLocale.equals(ctx.locale.toString())) {
                ctx.redirect(ctx.base()+"?_="+langLocale+"&x=references");
                return;
            }
        }
        
        // TODO: untangle this
        // admin things
        if((ctx.field(QUERY_DO).length()>0)) {
            String doWhat = ctx.field(QUERY_DO);

            // could be user or non-user items
            if(doWhat.equals("options")) {
                doOptions(ctx);
                return;
            } else if(doWhat.equals("disputed")) {
                doDisputed(ctx);
                return;
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
            }
            
            // these items are only for users.
            if(ctx.session.user != null) {
                if((doWhat.equals("list")||doWhat.equals("listu")) && (UserRegistry.userCanDoList(ctx.session.user))  ) {
                    doList(ctx);
                    return;
                } else if(doWhat.equals("coverage")  && (UserRegistry.userCanDoList(ctx.session.user))  ) {
                    doCoverage(ctx);
                    return;
                } else if(doWhat.equals("new") && (UserRegistry.userCanCreateUsers(ctx.session.user)) ) {
                    doNew(ctx);
                    return;
                }
            }

            // Option wasn't found
            sessionMessage = ("<i>Problem trying to do '"+doWhat+"'. You may need to be logged in first.</i>");
        }
        
        String title = " " + which;
        if(ctx.hasField(QUERY_EXAMPLE))  {
            title = title + " Example"; 
        }
        printHeader(ctx, title);
        if(sessionMessage != null) {
            ctx.println(sessionMessage);
        }
        
        WebContext baseContext = new WebContext(ctx);
        
        // print 'shopping cart'
        if(!ctx.hasField(QUERY_EXAMPLE))  {
            
            if((which.length()==0) && (ctx.locale!=null)) {
                which = xMAIN;
            }
            if(which.length()>0) {
                printUserTableWithHelp(ctx, "/"+which.replaceAll(" ","_"));
            } else {
                printUserTable(ctx);
            }

            
            Hashtable lh = ctx.session.getLocales();
            Enumeration e = lh.keys();
            if(e.hasMoreElements()) { 
                ctx.println("<p align='center'><B>Recent locales: </B> ");
                for(;e.hasMoreElements();) {
                    String k = e.nextElement().toString();
                    boolean canModify = UserRegistry.userCanModifyLocale(ctx.session.user,k);
                    ctx.print("<a href=\"" + baseContext.url() + ctx.urlConnector() + QUERY_LOCALE+"=" + k + "\">" + 
                                new ULocale(k).getDisplayName(ctx.displayLocale));
                    if(canModify) {
                        ctx.print(modifyThing(ctx));
                    }
                    ctx.println("</a> ");
                }
                
                ctx.println("</p>");
            }
            ctx.println("<hr/>");
        }
        
        doLocale(ctx, baseContext, which);
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
        localeListMap.put(lsl.getDisplayName(BASELINE_LOCALE),ls);
        
        TreeMap lm = (TreeMap)subLocales.get(ls);
        if(lm == null) {
            lm = new TreeMap();
            subLocales.put(ls, lm); 
        }
        
        if(t != null) {
            if(v == null) {
                lm.put(u.getDisplayCountry(BASELINE_LOCALE), localeName);
            } else {
                lm.put(u.getDisplayCountry(BASELINE_LOCALE) + " (" + u.getDisplayVariant(BASELINE_LOCALE) + ")", localeName);
            }
        }
    }
    
    protected synchronized TreeMap getLocaleListMap()
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
            n = new ULocale(localeName).getDisplayName(ctx.displayLocale) ;
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

    /*
     * show a list of locales that fall into this interest group.
     */
    void printListFromInterestGroup(WebContext ctx, String intgroup) {
        TreeMap lm = getLocaleListMap();
        if(lm == null) {
            throw new RuntimeException("Can't load CLDR data files");
        }
        ctx.println("<table summary='Locale List' border=1 class='list'>");
        int n=0;
        for(Iterator li = lm.keySet().iterator();li.hasNext();) {
            String ln = (String)li.next();
            String aLocale = (String)lm.get(ln);
            
            ULocale uLocale = new ULocale(aLocale);
            if(!intgroup.equals(uLocale.getLanguage())) {
                continue;
            }
            
            n++;
            ctx.print("<tr class='row" + (n%2) + "'>");
            ctx.print(" <td nowrap valign='top'>");
            printLocaleLink(ctx, aLocale, ln);
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
                    printLocaleLink(ctx, subLocale, sn);
                }
                j++;
            }
            ctx.println("</td");
            ctx.println("</tr>");
        }
        ctx.println("</table> ");
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
        printPathListOpen(ctx);
        if((locale==null)||(locale.length()<=0)) {
            doLocaleList(ctx, baseContext);            
            ctx.println("<br/>");
        } else {
            showLocale(ctx, which);
        }
        printPathListClose(ctx);
        printFooter(ctx);
    }
    
    /**
     * Print out a menu item
     * @param ctx the context
     * @param which the ID of "this" item
     * @param menu the ID of the current item
     */
    protected void printMenu(WebContext ctx, String which, String menu) {
        printMenu(ctx,which,menu,menu, QUERY_SECTION);
    }
    /**
     * Print out a menu item
     * @param ctx the context
     * @param which the ID of "this" item
     * @param menu the ID of the current item
     * @param anchor the #anchor to link to
     */
    protected void printMenuWithAnchor(WebContext ctx, String which, String menu, String anchor) {
        printMenu(ctx,which,menu,menu, QUERY_SECTION, anchor);
    }
    /**
     * Print out a menu item
     * @param ctx the context
     * @param which the ID of "this" item
     * @param menu the ID of the current item
     * @param title the Title of this menu
     */
    protected void printMenu(WebContext ctx, String which, String menu, String title) {
        printMenu(ctx,which,menu,title,QUERY_SECTION);
    }
    /**
     * Print out a menu item
     * @param ctx the context
     * @param which the ID of "this" item
     * @param menu the ID of the current item
     * @param title the Title of this menu
     * @param key the URL field to use (such as 'x')
     */
    protected void printMenu(WebContext ctx, String which, String menu, String title, String key) {
		printMenu(ctx,which,menu,title,key,null);
	}
    /**
     * Print out a menu item
     * @param ctx the context
     * @param which the ID of "this" item
     * @param menu the ID of the current item
     * @param title the Title of this menu
     * @param key the URL field to use (such as 'x')
     * @param anchor the #anchor to link to
     */
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
        String from = survprops.getProperty("CLDR_FROM","nobody@example.com");
        mailUser(ctx, getRequesterEmail(ctx), getRequesterName(ctx), theirEmail, subject, message);
    }
    
    void mailUser(WebContext ctx, String mailFromAddress, String mailFromName, String theirEmail, String subject, String message) {
        String requester = getRequester(ctx);
        String from = survprops.getProperty("CLDR_FROM","nobody@example.com");
        String smtp = survprops.getProperty("CLDR_SMTP",null);
        
        if(smtp == null) {
            ctx.println("<i>Not sending mail- SMTP disabled.</i><br/>");
            ctx.println("<hr/><pre>" + message + "</pre><hr/>");
            smtp = "NONE";
        } else {
            MailSender.sendMail(smtp, mailFromAddress, mailFromName,  from, theirEmail, subject,
                                message);
            ctx.println("Mail sent to " + theirEmail + " from " + from + " via " + smtp + "<br/>\n");
        }
        logger.info( "Mail sent to " + theirEmail + "  from " + from + " via " + smtp + " - "+subject);
        /* some debugging. */
    }
    
    String getRequesterEmail(WebContext ctx) {
        String cleanEmail = ctx.session.user.email;
        if(cleanEmail.equals("admin@")) {
            cleanEmail = "surveytool@unicode.org";
        }
        return cleanEmail;
    }
    
    String getRequesterName(WebContext ctx) {
        return ctx.session.user.name;
    }
    
    String getRequester(WebContext ctx) {
        String requester = getRequesterName(ctx) + " <" + getRequesterEmail(ctx) + ">";
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
        mailUser(ctx, theirEmail,subject,body);
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

            if(!ctx.hasField(QUERY_EXAMPLE)) {
                ctx.println("<table summary='locale info' width='95%' border=0><tr><td width='25%'>");
                ctx.println("<b><a href=\"" + ctx.url() + "\">" + "Locales" + "</a></b><br/>");
                for(i=(n-1);i>0;i--) {
                    for(j=0;j<(n-i);j++) {
                        ctx.print("&nbsp;&nbsp;");
                    }
                    boolean canModify = UserRegistry.userCanModifyLocale(ctx.session.user,ctx.docLocale[i]);
                    ctx.print("\u2517&nbsp;<a title='"+ctx.docLocale[i]+"' class='notselected' href=\"" + ctx.url() + ctx.urlConnector() +QUERY_LOCALE+"=" + ctx.docLocale[i] + 
                        "\">");
                    printLocaleStatus(ctx, ctx.docLocale[i], new ULocale(ctx.docLocale[i]).getDisplayName(ctx.displayLocale), "");
                    ctx.println("</a> ");
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
                ctx.print("<span title='"+ctx.locale+"' style='font-size: 120%'>");
                printMenu(subCtx, which, xMAIN, 
                    getLocaleStatus(ctx.locale.toString(), ctx.locale.getDisplayName(ctx.displayLocale), "") );
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
                    if(OTHERROOTS_ITEMS[n].equals("references")) {
                        if(!ctx.locale.getLanguage().equals(ctx.locale.toString())) {
                            ctx.println("<b>(note: will change to a parent locale)</b>");
                        }
                    }
                    ctx.print(" ");
                }
                ctx.println("| <a class='notselected' href='http://unicode.org/cldr/data/charts/supplemental/language_territory_information.html#"+
                    ctx.locale.getLanguage()+"'>supplemental</a>");
                
                ctx.print("</p>");
    //            ctx.print(" ");
    //            printMenu(subCtx, which, xOTHER);
                boolean canModify = UserRegistry.userCanModifyLocale(subCtx.session.user, subCtx.locale.toString());
                if(canModify) {
                    subCtx.println("<p class='hang'>Forum: ");
                    String forum = ctx.locale.getLanguage();
                    subCtx.println("<strong><a href='"+fora.forumUrl(subCtx, forum)+"'>Forum: "+forum+"</a></strong>");
                    subCtx.println("</p>");
                }
                if(ctx.prefBool(PREF_ADV)) {
                    subCtx.println("<p class='hang'>Advanced Items: ");
    //                printMenu(subCtx, which, TEST_MENU_ITEM);
                    printMenu(subCtx, which, RAW_MENU_ITEM);
                    subCtx.println("</p>");
                }
                subCtx.println("</td></tr></table>");
            } else { // end in-locale nav
                ctx.println("<h3>"+ctx.locale+" "+ctx.locale.getDisplayName(ctx.displayLocale)+" / " + which + " Example</h3>");
            }
            
            // check for errors
            {
                List checkCldrResult = (List)uf.hash.get(CHECKCLDR_RES+ctx.defaultPtype());

                if((checkCldrResult != null) &&  (!checkCldrResult.isEmpty()) && 
                    (/* true || */ (checkCldr != null) && (xMAIN.equals(which))) ) {
                    ctx.println("<div style='border: 1px dashed olive; padding: 0.2em; background-color: cream; overflow: auto;'>");
                    ctx.println("<b>Possible problems with locale:</b><br>");   
                    for (Iterator it3 = checkCldrResult.iterator(); it3.hasNext();) {
                        CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();
                        try{ 
                            if (!status.getType().equals(status.exampleType)) {
                                String cls = shortClassName(status.getCause());
                                ctx.printHelpLink("/"+cls,"<!-- help with -->"+cls, true);
                                ctx.println(": "+ status.toString() + "<br>");
                            } else {
                                ctx.println("<i>example available</i><br>");
                            }
                        } catch(Throwable t) {
                            ctx.println("Error reading status item: <br><font size='-1'>"+status.toString()+"<br> - <br>" + t.toString()+"<hr><br>");
                        }
                    }
                    ctx.println("</div>");
                }
            }
            
            
            // Find which pod they want, and show it.
            // NB keep these sections in sync with DataPod.xpathToPodBase() 
            subCtx.addQuery(QUERY_SECTION,which);
            for(n =0 ; n < LOCALEDISPLAYNAMES_ITEMS.length; n++) {        
                if(LOCALEDISPLAYNAMES_ITEMS[n].equals(which)) {
                    if(which.equals(CURRENCIES)) {
                        showPathList(subCtx, "//ldml/"+NUMBERSCURRENCIES, null);
                    } else if(which.equals(TIMEZONES)) {
                        showTimeZones(subCtx);
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
            ctx.println("<li><a href='"+fileName+"'>"+fileName+"</a> " + new ULocale(localeName).getDisplayName(ctx.displayLocale) +
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

private static CLDRFile gBaselineFile = null;
private static ExampleGenerator gBaselineExample = null;

private CLDRFile.Factory gFactory = null;

private synchronized CLDRFile.Factory getFactory() {
    if(gFactory == null) {
        gFactory = CLDRFile.Factory.make(fileBase,".*");
    }
    return gFactory;
}

public synchronized CLDRFile getBaselineFile(/*CLDRDBSource ourSrc*/) {
    if(gBaselineFile == null) {
        CLDRFile file = getFactory().make(BASELINE_LOCALE.toString(), true);
        file.freeze(); // so it can be shared.
        gBaselineFile = file;
    }
    return gBaselineFile;
}

public synchronized ExampleGenerator getBaselineExample() {
    if(gBaselineExample == null) {
        gBaselineExample = new ExampleGenerator(getBaselineFile());
    }
    return gBaselineExample;
}

public synchronized String getDirectionFor(ULocale locale) {
    // TODO: use orientation.
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
            
            checkCldr.setDisplayInformation(getBaselineFile());
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
 * reset the "list of locales".  
 * call this if you're resetting the in-db view of what's on disk.  
 * it will reset things like the locale list map, the metazone map, the interest map, ...
 */
private synchronized void resetLocaleCaches() {
    localeListMap = null;
    allMetazones = null;
    localeListSet = null;
}

/**
 * Master list of metazones. Don't access this directly.
 */
private static Set allMetazones = null;

/**
 * Maintain a master list of metazones, culled from root.
 */
public synchronized Set getMetazones() {
    if(allMetazones == null) {
        ElapsedTimer et = new ElapsedTimer();    
        XPathParts parts = new XPathParts(null,null);
        CLDRDBSource mySrc = makeDBSource(null, new ULocale("root"));
        Set aset = new HashSet();
        for(Iterator i = mySrc.iterator("//ldml/"+"dates/timeZoneNames/zone");i.hasNext();) {
            String xpath = i.next().toString();
            parts.set(xpath);
            String mzone = parts.getAttributeValue(-1,"mzone");
            if(mzone != null) {
                aset.add(mzone);
            }
        }
        allMetazones = aset;
        System.err.println("sm.getMetazones:" + allMetazones.size()+" metazones found in " + et.toString());
    }
    return allMetazones;
}


/**
* show the webpage for one of the 'locale codes' items.. 
 * @param ctx the web context
 * @param xpath xpath to the root of the structure
 * @param tx the texter to use for presentation of the items
 * @param fullSet the set of tags denoting the expected full-set, or null if none.
 */
public void showLocaleCodeList(WebContext ctx, String which) {
    showPathList(ctx, LOCALEDISPLAYNAMES+which, typeToSubtype(which), true /* simple */);
}

public void showPathListExample(WebContext ctx, String xpath, String lastElement,
        String e, String fullThing, CLDRFile cf) {
    DataPod oldPod =  ctx.getExistingPod(fullThing);
    DataPod.ExampleEntry ee = null;
    if(oldPod != null) {
        ee = oldPod.getExampleEntry(e); // retrieve the info out of the hash.. 
    }
    if(ee != null) {
        ctx.println("<form method=POST action='" + ctx.base() + "'>");
        ctx.printUrlAsHiddenFields();   
        String cls = shortClassName(ee.status.getCause());
        ctx.printHelpLink("/"+cls+"-example","Help with this "+cls+" example", true);
        ctx.println("<hr>");
        ctx.addQuery(QUERY_EXAMPLE,e);
        ctx.println("<input type='hidden' name='"+QUERY_EXAMPLE+"' value='"+e+"'>");
        
        // keep the Demo with the user. for now.
        CheckCLDR.SimpleDemo d = (CheckCLDR.SimpleDemo)ctx.getByLocale(e);
        if(d==null) {        
            d = ee.status.getDemo();
            ctx.putByLocale(e,d);
        }
            
        Map mapOfArrays = ctx.getParameterMap();
        Map m = new TreeMap();
        for(Iterator i = mapOfArrays.keySet().iterator();i.hasNext();) {
            String k = i.next().toString();
//            String[] v = (String[])mapOfArrays.get(k);  // We dont care about the broken, undecoded contents here..
            m.put(k,ctx.field(k,null)); //  .. use our vastly improved field() function
        }
        
        if(d != null) {
        
            try {
                String path = ee.pod.xpath(ee.pea);
                String fullPath = cf.getFullXPath(path);
                String value = ee.item.value;
                String html = d.getHTML(m);
                ctx.println(html);
            } catch (Exception ex) {
                ctx.println("<br><b>Error: </b> " + ex.toString() +"<br>");
            }
        }
        ctx.println("</form>");
    } else {
        ctx.println("<P><P><P><blockquote><i>That example seems to have expired. Perhaps the underlying data has changed? Try reloading the parent page, and clicking the Example link again.</i></blockquote>");
    }
}

public SupplementalData supplemental = null;

/**
 * for showing the list of zones to the user
 */

public void showTimeZones(WebContext ctx) {
    String z = ctx.field("z");
    WebContext subCtx = new WebContext(ctx);
    WebContext worldCtx = new WebContext(ctx);
    worldCtx.setQuery("z","001");

    String whichMZone = ctx.field("mzone"); // mzone user has chosen
    if(whichMZone.equals("")) {
        whichMZone = null;
    }

    // were we given a zone but no territory?
    if((z==null)||("".equals(z))) {
        if(ctx.hasField("zz")) {
            System.err.println("noz, but " + ctx.field("zz"));
            z = (String)(supplemental.getZoneToTerritory().get(ctx.field("zz")));
            System.err.println("z is now " + z);
        }
    }

    // try again
    if((z==null)||("".equals(z))) {
        String t = ctx.locale.getCountry(); // if the locale we're on has a country, use it.
        if(t!=null && t.length()>0) {
            z = t;
        } else {
//            z = "001"; // default to 'zones by territory'
            z = "full"; // default to 'all zones'
        }
    }
    
    try {
        ctx.println("<div style='float: right'>TZ Version:"+supplemental.getOlsonVersion()+"</div>");
    } catch(Throwable t) {
        ctx.println("<div style='float: right' class='warning'>TZ Version: "+ t.toString()+"</div>");
    }
    
    printMenu(ctx, z, "full", "All Zones", "z");
    ctx.println(" | ");
    printMenu(ctx, z, "meta", "All Metazones", "z");
    ctx.println(" | ");
    {
        String fakez = z; // show any region as "territory timezone list"
        if(!"full".equals(z) && !"meta".equals(z)) {
            fakez = "001";
        }
        printMenu(ctx, fakez, "001", "Zones by Territory", "z");
    }
    ctx.println("<br>");
    
    if("full".equals(z)) {
        subCtx.setQuery("z", z);
        showPathList(subCtx, "//ldml/dates/timeZoneNames/zone", null);
    } else if("meta".equals(z)) {
        subCtx.setQuery("z", z);
        showPathList(subCtx, "//ldml/"+"dates/timeZoneNames/metazone", null);
    } else {
        ULocale zLocale = new ULocale("und",z);
        
        /* Fetch the parent locales, and output them in reverse order. */
        Vector v = new Vector();
        
        String parLoc = z;
        
        while(parLoc!=null) {
            v.add(parLoc);
            if(parLoc.equals("001")) {
                parLoc = null;
            } else {
                parLoc = supplemental.getContainingTerritory(parLoc);
            }
        }
        
        WebContext nCtx = new WebContext(subCtx);
        
        ctx.println("<hr><p class='hang'>");
        
        // output in reverse order
        for(int j=v.size();j>1;j--) {
            String s = (String)v.get(j-1);
            ULocale nLocale = new ULocale("und",s);
            nCtx.setQuery("z",s);
            nCtx.println("<a class='notselected' href='"+nCtx.url()+"'>"+nLocale.getDisplayCountry(nCtx.displayLocale)+
                "</a> ");
            if(j > 1) {
                nCtx.println(" \u2192 ");
            }
        }
            
        nCtx.println("<b class='selected'>"+zLocale.getDisplayCountry(nCtx.displayLocale)+"</b></p>");
        
        String[] containsList = supplemental.getContainedTerritories(z);
        ///

        Set ot = supplemental.getObsoleteTerritories();
        
        if(containsList != null) {
            ctx.println("<ul>");
            for(int i=0;i<containsList.length;i++) {
                if(ot.contains(containsList[i])) { // don't show obsolete territory codes.
                    continue;
                }
                ULocale nLocale = new ULocale("und",containsList[i]);
                nCtx.setQuery("z",containsList[i]);
                nCtx.println(" \u2192 <a href='"+nCtx.url()+"'>"+nLocale.getDisplayCountry(nCtx.displayLocale)+
                    "</a><br>");
            }
            ctx.println("</ul>");
        }
        
        String zz = nCtx.field("zz");
        String zoneToShow = null;
        // does it contain any zones?
        Vector zones = (Vector)(supplemental.getTerritoryToZones().get(z));
        if(zones != null && !("001".equals(z))) {
            WebContext zCtx = new WebContext(nCtx);
            zCtx.setQuery("z",z);
            zCtx.println("<p class='hang'>Zones: ");
            if(zones.size()==1) {
                // only one zone. 
                // select it for 'em.
                zz = zones.get(0).toString();
                zCtx.setQuery("zz",zz);
                nCtx.setQuery("zz",zz);
            }
            String menuzz = zz;
            if(whichMZone != null) {
                menuzz = ""; // don't show a zone as selected if there is a metazone selected.
            }
            for(int j=0;j<zones.size();j++) {
                String s = zones.get(j).toString();
                printMenu(zCtx,menuzz, s,s, "zz");
//                oCtx.println(s+"<br>");
                nCtx.println(" ");
                
                if(s.equals(zz)) { // make sure the zone actually exists
                    zoneToShow = s;
                }
            }
            nCtx.println("</p>");
        }
        
        // Now, if zz isn't empty, display it.
        if(zoneToShow != null) {
            WebContext zCtx = new WebContext(nCtx);
            zCtx.setQuery("z",z);
            zCtx.setQuery("zz",zoneToShow);
            if(whichMZone != null) {
                zCtx.setQuery("mzone",whichMZone);
            }
            showOneZone(zCtx, zoneToShow);
        }
    }
}

void showOneZone(WebContext ctx, String zone) {
    SurveyMain sm = this;
    String whichMZone = ctx.field("mzone"); // mzone user has chosen
    if(whichMZone.equals("")) {
        whichMZone = null;
    }

    SurveyMain.UserLocaleStuff uf = sm.getUserFile(ctx, (ctx.session.user==null)?null:ctx.session.user, ctx.locale);
    CLDRFile cf = uf.cldrfile;
    CLDRFile resolvedFile = new CLDRFile(uf.dbSource,true);
    CLDRFile engFile = ctx.sm.getBaselineFile();
    //  showPathList(subCtx, "//ldml/"+"dates/timeZoneNames/zone[@type=\""+s+"\"]", null);
    //  private void showXpath(WebContext baseCtx, String xpath, int base_xpath, ULocale loc) {
    //WebContext ctx = new WebContext(baseCtx);
    //ctx.setLocale(loc);
    // Show the Pod in question:
    //  ctx.println("<hr> \n This post Concerns:<p>");
    boolean canModify = (UserRegistry.userCanModifyLocale(ctx.session.user,ctx.locale.toString()));
    String xpath =  "//ldml/"+"dates/timeZoneNames/zone";
    String ourSuffix = "[@type=\""+zone+"\"]";
    String base_xpath = xpath+ourSuffix;
    String podBase = DataPod.xpathToPodBase(xpath);
    
    // first, check metazone status
    String useMetazone = null;
    XPathParts parts = new XPathParts(null, null);
    // Look for Metazone
    Map metaMap = new TreeMap();
    
    WebContext mzContext = new WebContext(ctx);
    
    Iterator mzit = resolvedFile.iterator(podBase+ourSuffix+"/usesMetazone");
    if(mzit.hasNext()) {
        ctx.println("<table class='tzbox'><tr><th>from</th><th>to</th><th>MetaZone</th></tr>");
        
        for(;mzit.hasNext();) {
            String ameta = (String)mzit.next();
            String mfullPath = resolvedFile.getFullXPath(ameta);
            parts.set(mfullPath);
            String mzone = parts.getAttributeValue(-1,"mzone");
            String from = parts.getAttributeValue(-1,"from");
            if(from==null) {
                from = "1970-01-01";
            }
            String to = parts.getAttributeValue(-1,"to");
            String contents[] = { from, to, mzone };
            metaMap.put(from,contents);
        }
        int n = 0;
        for(Iterator it = metaMap.entrySet().iterator();it.hasNext();) {
            n++;
            Map.Entry e = (Map.Entry)it.next();
            String contents[] = (String[])e.getValue();
            String from = contents[0];
            String to = contents[1];
            String mzone = contents[2];
            // OK, now that we are unpacked..
            mzContext.setQuery("mzone",mzone);
            String mzClass="";
            if(to == null) {
                useMetazone = mzone;
                mzClass="currentZone";
            }
            mzClass = "r"+(n%2)+mzClass; //   r0 r1 r0 r1 r1currentZone    ( or r0currentZone )
            ctx.println("<tr class='"+mzClass+"'><td>");
            if(from != null) {
                ctx.println(from);
            }
            ctx.println("</td><td>");
            if(to != null) {
                ctx.println(to);
            } else {
                ctx.println("<i>now</i>");
            }
            ctx.println("</td><td>");
            ctx.println("<tt class='codebox'>");
            
            ctx.print("<a class='"+(mzone.equals(whichMZone)?"selected":"notselected")+"' href='"+mzContext.url()+"'>");
            ctx.print(mzone);
            ctx.println("</a>");
            ctx.println("</td></tr>");
            
        }
        ctx.println("</table>");
    }
    
    // now, print the normal zone stuff.
    
    sm.printPathListOpen(ctx);
    String showZoneOverride = ctx.field("szo");
    boolean exemplarCityOnly  = false;

    if(whichMZone != null) {
        ctx.println("<h2>MetaZone: "+whichMZone+"</h2>");
        // Re-map things pretty drastically to be metazone based.
        zone = whichMZone;
        xpath =  "//ldml/"+"dates/timeZoneNames/metazone";
        ourSuffix = "[@type=\""+zone+"\"]";
        base_xpath = xpath+ourSuffix;
        podBase = DataPod.xpathToPodBase(xpath);
    } else {
        ctx.println("<h2>"+zone+"</h2>");
        if(useMetazone != null) {
            mzContext.setQuery("mzone",useMetazone);
            ctx.print("<i>Note: the metazone <b>");
            ctx.print("<a class='"+(useMetazone.equals(whichMZone)?"selected":"notselected")+"' href='"+mzContext.url()+"'>");
            ctx.print(useMetazone+"</a></b> is active for this zone. <a href=\""+
                bugReplyUrl(BUG_METAZONE_FOLDER, BUG_METAZONE, ctx.locale+":"+ zone + ":" + useMetazone + " incorrect")+
                "\">Click Here to report Metazone problems</a> .</i>");
            if(!"y".equals(showZoneOverride)) {
                ctx.setQuery("szo","y");
                ctx.println("<hr><a href='"+ctx.url()+"'>Click Here</a> to show this zone for overriding.<br>");
                
                // show the exemplarCity
                exemplarCityOnly = true;
            }
        }
    }
    
    if(cf == null) {
        throw new InternalError("CLDRFile is null!");
    }
    if(canModify) {
        /* hidden fields for that */
        ctx.printUrlAsHiddenFields();

        ctx.println("<input  type='submit' value='" + sm.xSAVE + "'>"); // style='float:right'
        synchronized (ctx.session) { // session sync
            // first, do submissions.
            DataPod oldPod = ctx.getExistingPod(podBase);
            
            // *** copied from SurveyMain.showLocale() ... TODO: refactor
            CLDRDBSource ourSrc = uf.dbSource; // TODO: remove. debuggin'
            
            if(ourSrc == null) {
                throw new InternalError("oursrc is null! - " + (SurveyMain.USER_FILE + SurveyMain.CLDRDBSRC) + " @ " + ctx.locale );
            }
            synchronized(ourSrc) { 
                // Set up checks
                CheckCLDR checkCldr = (CheckCLDR)uf.getCheck(ctx); //make it happen
            
                if(sm.processPeaChanges(ctx, oldPod, cf, ourSrc)) {
                    int j = sm.vet.updateResults(oldPod.locale); // bach 'em
                    ctx.println("<br> You submitted data or vote changes, and " + j + " results were updated. As a result, your items may show up under the 'priority' or 'proposed' categories.<br>");
                }
            }
        }
    } else {
        //ctx.println("<br>cannot modify " + ctx.locale + "<br>");
    }
    
    DataPod pod = ctx.getPod(podBase);

    SurveyMain.printPodTableOpen(ctx, pod, true);
    
    if(!exemplarCityOnly) {
        sm.showPeas(ctx, pod, canModify, base_xpath, true);
    } else {
        // only show the exemplarCity
        sm.showPeas(ctx, pod, canModify, base_xpath + "/exemplarCity", true);

       //or, could use this with a numeric xpath; void showPeas(WebContext ctx, DataPod pod, boolean canModify, int only_base_xpath, boolean zoomedIn)
    }
    SurveyMain.printPodTableClose(ctx, pod);
    // TODO: no alts for now, on the time zones..
/*
    if(canModify) {
        ctx.println("<hr>");
        String warnHash = "add_an_alt";
        ctx.println("<div id='h_"+warnHash+"'><a href='javascript:show(\"" + warnHash + "\")'>" + 
                    "<b>+</b> Add Alternate..</a></div>");
        ctx.println("<!-- <noscript>Warning: </noscript> -->" + 
                    "<div style='display: none' class='pager' id='" + warnHash + "'>" );
        ctx.println("<a href='javascript:hide(\"" + warnHash + "\")'>" + 
                    "(<b>-</b>)</a>");
        ctx.println("<label>To add a new alternate, type it in here and modify any item above:<br> <input name='new_alt'></label><br>");                        
        ctx.println("</div>");

    }
*/
    if(showZoneOverride.length()>0) {
        ctx.println("<input type=hidden name=szo value=y>");
    }
    sm.printPathListClose(ctx);
}

/**
* This is the main function for showing lists of items (pods).
 */
public void showPathList(WebContext ctx, String xpath, String lastElement) {
    showPathList(ctx,xpath,lastElement,false);
}

public void showPathList(WebContext ctx, String xpath, String lastElement, boolean simple) {
    /* all simple */
    simple = true;

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
            showPathListExample(ctx, xpath, lastElement, e, fullThing, cf);
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
            DataPod pod = ctx.getPod(fullThing); // we load a new pod here - may be invalid by the modifications above.
            pod.simple=simple;
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

static void printPodTableOpen(WebContext ctx, DataPod pod, boolean zoomedIn) {
    ctx.println("<table summary='Data Items for "+ctx.locale.toString()+" " + pod.xpathPrefix + "' class='list' border='0'>");
    
    if(!zoomedIn) {
        ctx.println("<tr class='headingb'>\n"+
                    " <th>St.</th>\n"+                  // 1
                    " <th>Code</th>\n"+                 // 2
                    " <th colspan='2'>"+BASELINE_NAME+"</th>\n"+              // 3
                    " <th colspan='2'>Current</th>\n"+  // 6
                    " <th colspan='2'>Proposed</th>\n"+ // 7
                    " <th colspan='2' width='20%'>Change</th>\n"+               // 8
                    " <th>Rf</th>\n"+           // 9
                    " <th>n/a</th>\n"+                   // 5
                    " <th>Zm.</th>\n"+                  // 4
                    "</tr>");
    } else {
        ctx.println("<tr class='heading'>\n"+
                    "<td></td>\n"+ // row title
                    " <th colspan=2>\n"+
                    " </th>\n"+
                    "<th> action</th>\n"+
                    "<th colspan=1> data</th>\n"+
                    "<th> examples</th>\n"+
                    "<th> comments</th>\n"+
                    "</tr>");
    }
}

static void printPodTableClose(WebContext ctx, DataPod pod) {
    ctx.println("</table>");
}

void showPeas(WebContext ctx, DataPod pod, boolean canModify) { 
    showPeas(ctx, pod, canModify, -1, false);
}

void showPeas(WebContext ctx, DataPod pod, boolean canModify, int only_base_xpath, boolean zoomedIn) {
    showPeas(ctx, pod, canModify, only_base_xpath, null, zoomedIn);
}

void showPeas(WebContext ctx, DataPod pod, boolean canModify, String only_prefix_xpath, boolean zoomedIn) {
    showPeas(ctx, pod, canModify, -1, only_prefix_xpath, zoomedIn);
}

void showPeas(WebContext ctx, DataPod pod, boolean canModify, int only_base_xpath, String only_prefix_xpath, boolean zoomedIn) {
    int count = 0;
    int dispCount = 0;
    int total = 0;
    int skip = 0; // where the index points to
    int oskip = ctx.fieldInt("skip",0); // original skip from user.
    
    boolean partialPeas = ((only_base_xpath!=-1)||(only_prefix_xpath!=null));
    
    //       total = mySet.count();
    //        boolean sortAlpha = (sortMode.equals(PREF_SORTMODE_ALPHA));
    UserLocaleStuff uf = getUserFile(ctx, ctx.session.user, ctx.locale);
    CLDRFile cf = uf.cldrfile;
        //    CLDRFile engf = getBaselineFile();
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
        WebContext refCtx = new WebContext(ctx);
        refCtx.setLocale(new ULocale(ctx.locale.getLanguage())); // ensure it is from the language
        DataPod refPod = refCtx.getPod("//ldml/references");
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
    int moveSkip=-1;  // move the "skip" marker?
    int xfind = ctx.fieldInt("xfind");
    if((xfind != -1) && !partialPeas) {
        // see if we can find this base_xpath somewhere..
        int pn = 0;
        for(Iterator i = peas.iterator();(moveSkip==-1)&&i.hasNext();) {
            DataPod.Pea p = (DataPod.Pea)i.next();
            if(p.base_xpath == xfind) {
                moveSkip = pn;
            }
            pn++;
        }
        if(moveSkip != -1) {
            oskip = (moveSkip / CODES_PER_PAGE)*CODES_PER_PAGE; // make it fall on a page boundary.
        }
    }
    // -----
    if(!partialPeas) {
        skip = showSkipBox(ctx, peas.size(), pod.getDisplaySet(sortMode), true, sortMode, oskip);
    } else {
        skip = 0;
    }
    
    if(!partialPeas) {
        ctx.printUrlAsHiddenFields();   
    }
	
    if(disputedOnly==true){
        ctx.println("(<b>Disputed Only</b>)<br><input type='hidden' name='only' value='disputed'>");
    }

    if(!partialPeas) {
        ctx.println("<input type='hidden' name='skip' value='"+ctx.field("skip")+"'>");
        printPodTableOpen(ctx, pod, zoomedIn);
    }

				
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

    boolean exemplarCityOnly = (!partialPeas && pod.xpathPrefix.equals("//ldml/dates/timeZoneNames/zone"));
    
    
    for(ListIterator i = peas.listIterator(peaStart);(partialPeas||(count<peaCount))&&i.hasNext();count++) {
        DataPod.Pea p = (DataPod.Pea)i.next();
        
///*srl*/        System.err.println(pod.xpath(p));

        if(partialPeas) { // are we only showing some peas?
            if((only_base_xpath != -1) && (only_base_xpath != p.base_xpath)) { // print only this one xpath
                continue; 
            }
            
            if((only_prefix_xpath!=null) && !pod.xpath(p).startsWith(only_prefix_xpath)) {
                continue;
//            } else {
//                System.err.println("P[["+only_prefix_xpath+"]], t[["+pod.xpath(p)+"]]");
            }
        }
        if(exemplarCityOnly) {
            if(pod.xpath(p).indexOf("exemplarCity")==-1) {
                continue;
            }
        }
        
        if((!partialPeas) && checkPartitions) {
            for(int j = 0;j<dSet.partitions.length;j++) {
                if((dSet.partitions[j].name != null) && (count+skip) == dSet.partitions[j].start) {
                    if(zoomedIn) {
                        ctx.println("<tr class='heading'><th class='partsection' align='left' colspan='5'>" +
                            "<a name='" + dSet.partitions[j].name + "'>" +
                            dSet.partitions[j].name + "</a>" +
                            "</th></tr>");
                    } else {
                        ctx.println("<tr class='heading'><th class='partsection' align='left' colspan='13'>" +
                            "<a name='" + dSet.partitions[j].name + "'>" +
                            dSet.partitions[j].name + "</a>" +
                            "</th></tr>");
                    }
                }
            }
        }
        
        try {
            showPea(ctx, pod, p, ourDir, cf, ourSrc, canModify,showFullXpaths,refs,checkCldr, zoomedIn);
            if(exemplarCityOnly) {
                String zone = p.type;
                int n = zone.lastIndexOf('/');
                if(n!=-1) {
                    zone = zone.substring(0,n); //   blahblah/default/foo   ->  blahblah/default   ('foo' is lastType and will show up as the value)
                }
            
                ctx.println("<tr><td colspan=4><a href='"+ctx.base()+"?"+
                        "_="+ctx.locale+"&amp;x=timezones&amp;zz="+zone+"' class='notselected'>"+zone+"</a></td></tr>");
            }
        } catch(Throwable t) {
            // failed to show pea. 
            ctx.println("<tr class='topbar'><td colspan='8'><b>"+pod.xpath(p)+"</b><br>");
            ctx.print(t);
            ctx.print("</td></tr>");
        }
        if(p.subPeas != null) {
            for(Iterator e = p.subPeas.values().iterator();e.hasNext();) {
                DataPod.Pea subPea = (DataPod.Pea)e.next();
                try {
                    showPea(ctx, pod, subPea, ourDir, cf, ourSrc, canModify, showFullXpaths,refs,checkCldr, zoomedIn);
                } catch(Throwable t) {
                    // failed to show sub-pea.
                    ctx.println("<tr class='topbar'><td colspan='8'>sub pea: <b>"+pod.xpath(subPea)+"."+subPea.altType+"</b><br>");
                    ctx.print(t);
                    ctx.print("</td></tr>");
                }
            }
        }
    }
    if(!partialPeas) {
        printPodTableClose(ctx, pod);
    
        if(canModify && 
            pod.xpathPrefix.indexOf("references")!=-1) {
            ctx.println("<hr>");
            ctx.println("<table class='listb' summary='New Reference Box'>");
            ctx.println("<tr><th colspan=2 >Add New Reference ( click '"+xSAVE+"' after filling in the fields.)</th></tr>");
            ctx.println("<tr><td align='right'>Reference: </th><td><input size='80' name='"+MKREFERENCE+"_v'></td></tr>");
            ctx.println("<tr><td align='right'>URI: </th><td><input size='80' name='"+MKREFERENCE+"_u'></td></tr>");
            ctx.println("</table>");
        }
        
        
        /*skip = */ showSkipBox(ctx, peas.size(), pod.getDisplaySet(sortMode), false, sortMode, oskip);
        
        if(!canModify) {
            ctx.println("<hr> <i>You are not authorized to make changes to this locale.</i>");
        }
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
    String altType = p.altType;
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
        choice_v=""; // so that the value is ignored, as it is not changing
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
                    // reject the value
                    if(!choice.equals(REMOVE)) {
                        ctx.temporaryStuff.put(fieldHash+"_v", choice_v);
                    }
                    return false;
                } else {
                    ctx.println("<tt class='codebox'>"+ p.displayName +"</tt> - <i>Note, differs only in references</i> ("+theirReferences+")<br>");
                }
            }
        }
        String altPrefix = null;
        // handle FFT
                
        if(p.type.equals(DataPod.FAKE_FLEX_THING)) {
            throw new RuntimeException("Missing fake flex thing");
        /*
               DateTimePatternGenerator dateTimePatternGenerator = DateTimePatternGenerator.newInstance();
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
            */
        } else {
            altPrefix =         XPathTable.altProposedPrefix(ctx.session.user.id);
        }
        // user requested a new alternate.
        if(ctx.field("new_alt").trim().length()>0) {
            altType = ctx.field("new_alt").trim();
        }
        String aDisplayName = p.displayName;
        if(pod.xpath(p).startsWith("//ldml/characters") && 
            ((aDisplayName == null) || "null".equals(aDisplayName))) {
            aDisplayName = "standard";
        }
        ctx.print("<tt class='codebox'>" + aDisplayName + "</tt> <b>change: " + choice_v +"</b> : ");
        // Test: is the value valid?
        {
            /* cribbed from elsewhere */
            UserLocaleStuff uf = getUserFile(ctx, (ctx.session.user==null)?null:ctx.session.user, ctx.locale);
            /*CLDRFile cf = uf.cldrfile;
            if(cf == null) {
                throw new InternalError("CLDRFile is null!");
            }*/
            /*CLDRDBSource ourSrc = uf.dbSource; // TODO: remove. debuggin'
            if(ourSrc == null) {
                throw new InternalError("oursrc is null! - " + (USER_FILE + CLDRDBSRC) + " @ " + ctx.locale );
            }*/
            // Set up checks
            CheckCLDR checkCldr = (CheckCLDR)uf.getCheck(ctx); //make it happen
            List checkCldrResult = new ArrayList();
            
            checkCldr.handleCheck(fullPathMinusAlt, fullPathMinusAlt, choice_v, ctx.getOptionsMap(), checkCldrResult);  // they get the full course
            
            if(!checkCldrResult.isEmpty()) {
                boolean doFail = false;
                for (Iterator it3 = checkCldrResult.iterator(); it3.hasNext();) {
                    CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();
                    if(status.getType().equals(status.errorType)) {
                        doFail = true;
                    }
                    try{ 
                        if (!status.getType().equals(status.exampleType)) {
                            String cls = shortClassName(status.getCause());
                            ctx.printHelpLink("/"+cls,"<!-- help with -->"+cls, true);
                            ctx.print(ctx.iconThing("stop",cls));
                            ctx.println(": "+ status.toString() + "<br>");
                        }/* else {
                            ctx.println("<i>example available</i><br>");
                        }*/
                    } catch(Throwable t) {
                        ctx.println("Error reading status item: <br><font size='-1'>"+status.toString()+"<br> - <br>" + t.toString()+"<hr><br>");
                    }
                }
                if(doFail) {
                    // reject the value
                    if(!choice.equals(REMOVE)) {
                        ctx.temporaryStuff.put(fieldHash+"_v", choice_v);
                    }
                    ctx.println("<b>This item was not accepted because of test failures.</b><hr>");
                    return false;
                }
            }
        }
        
        String newProp = ourSrc.addDataToNextSlot(cf, pod.locale, fullPathMinusAlt, altType, 
            altPrefix, ctx.session.user.id, choice_v, choice_r);
        // update implied vote
        ctx.print(" : <b>" + newProp+"</b>");
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

void showPea(WebContext ctx, DataPod pod, DataPod.Pea p, String ourDir, CLDRFile cf, 
    CLDRDBSource ourSrc, boolean canModify, boolean showFullXpaths, String refs[], CheckCLDR checkCldr)  {
    showPea(ctx,pod,p,ourDir,cf,ourSrc,canModify,showFullXpaths,refs,checkCldr,false);
}

// TODO: trim unused params
void showPea(WebContext ctx, DataPod pod, DataPod.Pea p, String ourDir, CLDRFile cf, 
    CLDRDBSource ourSrc, boolean canModify, boolean showFullXpaths, String refs[], CheckCLDR checkCldr, boolean zoomedIn) {
    
    // if it is not zoomed in - go elsewhere.
    if(!zoomedIn) {
        showPeaZoomedout(ctx,pod,p,ourDir,cf,ourSrc,canModify,showFullXpaths,refs,checkCldr);
        return;
    }
    
    //if(p.type.equals(DataPod.FAKE_FLEX_THING) && !UserRegistry.userIsTC(ctx.session.user)) {
    //    return;
    //}
    
    boolean canSubmit = UserRegistry.userCanSubmitAnyLocale(ctx.session.user)  // formally able to submit
        || (canModify&&p.hasProps); // or, there's modified data.

    String localeLangName = ctx.locale.getDisplayLanguage(ctx.displayLocale);

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
    ctx.println("<th class='rowinfo'>Code / " + BASELINE_NAME + "</th>"); // ##0 title
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
        ctx.println("<th nowrap style='padding-left: 4px;' colspan='3' valign='top' align='right' class='botgray'>");
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
    ctx.println("</th> ");
    // Example
    String baseExample = getBaselineExample().getExampleHtml(fullPathFull, p.displayName, ExampleGenerator.Zoomed.IN);
    if(baseExample != null) {
        ctx.print("<td align='left' valign='top' class='generatedexample' nowrap>"+ baseExample + "</td>");
    } else {
        ctx.print("<td></td>");
    }
    ctx.println("</tr>");
    
    
    if((p.hasInherited == true) && (p.type != null) && (p.inheritFrom == null)) { // by code
        String pClass = "class='warnrow'";
        ctx.print("<tr " + pClass + ">");
        ctx.println("<th class='rowinfo'>"+localeLangName+"</th>"); // ##0 title
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
    if(p.pathWhereFound != null) { // is the pea tagged with another path?
        // middle common string match
        String a = fullPathFull;
        String b = p.pathWhereFound;
        
        // run them through the wringer..
        a = xpt.getById(xpt.xpathToBaseXpathId(a));   // full path (source)
        b = xpt.getById(xpt.xpathToBaseXpathId(b));   // full path (target)
        
        
        int alen = a.length();
        int blen = b.length();
        
        int prefixSize;
        int suffixSize;
        
        // find a common prefix
        for(prefixSize=0;a.substring(0,prefixSize).equals(b.substring(0,prefixSize));prefixSize++)
            ;
        int maxlen = (alen>blen)?alen:blen;
        
        /* System.err.println("A:"+a);
        System.err.println("B:"+b); */
        
        // find a common suffix
        for(suffixSize=0;((suffixSize+prefixSize)<maxlen)&&
            a.substring(alen-suffixSize,alen).equals(b.substring(blen-suffixSize,blen));suffixSize++)
            ;
//                    System.err.println("SUFF"+suffixSize+": ["+a+"] -> "+a.substring(alen-suffixSize,alen));
        
        // at least the slash (/) is a prefix
        if(prefixSize==0) {
            prefixSize=1;
        }
        
        String xa = a.substring(prefixSize-1,alen-suffixSize+1);
        String xb = b.substring(prefixSize-1,blen-suffixSize+1);
        
        if((xa.length()+xb.length())>0) {
            // Aliases: from xa to xb
            ctx.println("<tr>");
            ctx.println("<th class='rowinfo'>"+localeLangName+" <i>Alias</i></th>"); // ##0 title
            ctx.print("<td colspan='3'><i><tt span='codebox'>" + xb + "</tt> \u2192 <tt span='codebox'>" + xa + "</tt></i>");
            ctx.printHelpLink("/AliasedFrom","Help",true,false);
            ctx.println("</td></tr>");
        } else {
            // Aliased - no further information
            ctx.println("<tr>");
            ctx.println("<th class='rowinfo'>"+localeLangName+" <i>Alias</i></th>"); // ##0 title
            ctx.println("<td colspan='3'>");
            ctx.printHelpLink("/AliasedFrom","Help",true,false);
            ctx.println("</td></tr>");
        }
    }
    /* else */ if(isAlias) {   
//        String shortStr = fullPathFull.substring(fullPathFull.indexOf("/alias")/*,fullPathFull.length()*/);
//<tt class='codebox'>["+shortStr+"]</tt>
        ctx.println("<tr>");
        ctx.println("<th class='rowinfo'>"+localeLangName+"</th>"); // ##0 title
        ctx.println("<td colspan='3'><i>Alias</i></td></tr>");
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
            ctx.println("<th class='rowinfo'>"+localeLangName+"</th>"); // ##0 title
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

    } else if (p.items.isEmpty()) {
        // there weren't any normal items to show. show an example anyways.
        ctx.print("<tr>");
        ctx.println("<th class='rowinfo'>"+localeLangName+"</th>"); // ##0 title
        ctx.print("<td colspan='5' align='right' valign='top'></td>");
        // example
        String itemExample = pod.exampleGenerator.getExampleHtml(fullPathFull, null, ExampleGenerator.Zoomed.IN);
        if(itemExample != null) {
            ctx.print("<td class='generatedexample' nowrap align='left' valign='top'>"+ /* item.xpath+"//"+ */ itemExample + "</td>");
        } else {
            ctx.print("<td></td>");
        }
        ctx.println("</tr>");
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
        ctx.print("<tr>");
        ctx.println("<th class='rowinfo'>"+localeLangName+"<!-- "+pClass+" --></th>"); // ##0 title
        ctx.print("<td colspan='1' align='right' valign='top'>");
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
        
        ctx.print("<td nowrap class='botgray' valign='top' dir='" + ourDir +"'>");
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
        
        // example
        String itemExample = pod.exampleGenerator.getExampleHtml(item.xpath, item.value, ExampleGenerator.Zoomed.IN);
        if(itemExample != null) {
            ctx.print("<td class='generatedexample' align='left' valign='top'>"+ /* item.xpath+"//"+ */ itemExample + "</td>");
        } else {
            ctx.print("<td></td>");
        }
        
        
        if((item.tests != null) || (item.examples != null)) {
            if(item.tests != null) {
                ctx.println("<td class='warncell'>");
                for (Iterator it3 = item.tests.iterator(); it3.hasNext();) {
                    CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();
                    if (!status.getType().equals(status.exampleType)) {
                        ctx.println("<span class='warning'>");
                        String cls = shortClassName(status.getCause());
                        if(status.getType().equals(status.errorType)) {
                            ctx.print(ctx.iconThing("stop","Error: "+cls));
                        } else {
                            ctx.print(ctx.iconThing("warn","Warning: "+cls));
                        }
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
                        String theMenu = SurveyMain.xpathToMenu(xpt.getById(p.superPea.base_xpath));
                        if(theMenu==null) {
                            theMenu="raw";
                        }
                        ctx.print("<a "+ctx.atarget("st:Ex:"+ctx.locale)+" href='"+ctx.url()+ctx.urlConnector()+"_="+ctx.locale+"&amp;x="+theMenu+"&amp;"+ QUERY_EXAMPLE+"="+e.hash+"'>");
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
                    ctx.print("<b><a "+ctx.atarget("ref_"+ctx.locale)+" href='"+refCtx.url()+"#REF_" + references[i] + "'>"+references[i]+"</a></b>");
                }
                ctx.print(")</td>");
            }
        }
        
        ctx.println("</tr>");
    }
    
    if(true && !isAlias) {
        String pClass = "";
         // dont care
        ctx.print("<tr " + pClass + ">");
        ctx.println("<th class='rowinfo'>"+localeLangName+"<!-- changeto --></th>"); // ##0 title
        ctx.print("<td nowrap valign='top' align='right'>");
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

            if(!zoomedIn) {
                //if(canModify && canSubmit) {
                    fora.showForumLink(ctx, pod, p, p.superPea.base_xpath);
                //}
            } else {
                ctx.print("<span class='"+boxClass+"'>" + CHANGETO + "</span>");
                if(canModify && canSubmit ) {
                    ctx.print("<input name='"+fieldHash+"' id='"+fieldHash+"_ch' value='"+CHANGETO+"' type='radio' >");
                } else {
                    ctx.print("<input type='radio' disabled>");
                }
            }
        }
        ctx.println("</td>");
        if(zoomedIn && canSubmit && canModify && !p.confirmOnly) {
            String oldValue = (String)ctx.temporaryStuff.get(fieldHash+"_v");
            String fClass = "inputbox";
            ctx.print("<td colspan='2'>");
            boolean badInputBox = false;
            if(oldValue==null) {
                oldValue="";
            } else {
                badInputBox = true;
                ctx.print("<span class='ferrbox'>"+ctx.iconThing("stop","this item was not accepted."));
                fClass = "badinputbox";
            }
            ctx.print("<input onfocus=\"document.getElementById('"+fieldHash+"_ch').click()\" name='"+fieldHash+"_v' value='"+oldValue+"' class='"+fClass+"'>");
            if(badInputBox) {
                ctx.print("</span>");
            }
            ctx.println("</td>");
            // references
            if(refs.length>0) {
                ctx.print("<td nowrap><label>");
                ctx.print("<a "+ctx.atarget("ref_"+ctx.locale)+" href='"+refCtx.url()+"'>Ref:</a>");
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

/**
 * show a pea, in a simplified state. This is used in the zoomed out mode.
 * we expect types here.
 */
void showPeaZoomedout(WebContext ctx, DataPod pod, DataPod.Pea p, String ourDir, CLDRFile cf, 
    CLDRDBSource ourSrc, boolean canModify, boolean showFullXpaths, String refs[], CheckCLDR checkCldr) {

    boolean zoomedIn = false; // not a param
    String ourAlign = "left";
    if(ourDir.equals("rtl")) {
        ourAlign = "right";
    }
    
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
    boolean noWinner = false;
    if(resultXpath_id != -1) {
        resultXpath = xpt.getById(resultXpath_id); 
    } else {
        noWinner = true;
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
    // Mark the line as disputed or insufficient, depending.
    String rclass = "vother";
    boolean foundError = p.hasErrors;
    boolean foundWarning = p.hasWarnings;

    // calculate the class of data items
    String statusIcon="";
    {
        int s = resultType[0];
        
        if(foundError) {
//            rclass = "warning";
            statusIcon = ctx.iconThing("stop","Errors - please zoom in");            
        } else if(foundWarning) {
            rclass = "okay";
            statusIcon = ctx.iconThing("warn","Warnings - please zoom in");            

            /*
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
            */
            
        } else if((s & Vetting.RES_DISPUTED)>0) {
            rclass= "disputedrow";
            statusIcon = (ctx.iconThing("ques","Unconfirmed: disputed"));
        } else if ((s&(Vetting.RES_INSUFFICIENT|Vetting.RES_NO_VOTES))>0) {
            rclass = "insufficientrow";
            statusIcon = (ctx.iconThing("ques","Unconfirmed: insufficient"));            
        } else if((s&(Vetting.RES_BAD_MASK)) == 0) {
            statusIcon = (ctx.iconThing("okay","ok"));
            rclass = "okay";
        } else {
            rclass = "vother";
        }
        if(noWinner && XMLSource.CODE_FALLBACK_ID.equals(p.inheritFrom)) {
            rclass = "insufficientrow";
        }
    }
    
    ctx.println("<tr class='topbar'>");
    String baseInfo = "#"+base_xpath+", w["+Vetting.typeToStr(resultType[0])+"]:" + resultXpath_id;
    
     
    ctx.print("<th class='"+rclass+"' valign='top'>");
    if(!zoomedIn) {
        fora.showForumLink(ctx,pod,p,p.superPea.base_xpath,statusIcon);
    } else {
        ctx.print(statusIcon);
    }
    ctx.println("</th>");
    
    // ##2 code
    ctx.print("<th class='botgray' colspan='1' valign='top' align='left'>");
    //if(p.displayName != null) { // have a display name - code gets its own box
    int xfind = ctx.fieldInt("xfind");
    if(xfind==base_xpath) {
        ctx.print("<a name='x"+xfind+"'>");
    }

    {
        ctx.print("<tt class='hangsml' title='"+baseInfo+"' >");
        String typeShown = p.type.replaceAll("/","/\u200b");
        if(!zoomedIn) {
            fora.showForumLink(ctx,pod,p,p.superPea.base_xpath,typeShown);
        } else {
            ctx.print(typeShown);
        }
        ctx.print("</tt>");
    }
    
    if(p.altType != null) {
        ctx.print("<br> ("+p.altType+")");
    }
    if(xfind==base_xpath) {
        ctx.print("</a>"); // for the <a name..>
    }
    ctx.println("</th>");
    
    // ##3 display / Baseline
    ctx.println("<th nowrap style='padding-left: 4px;' colspan='1' valign='top' align='left' class='botgray'>");
    if(p.displayName != null) {
        ctx.println(p.displayName); // ##3 display/Baseline
    }
/*
    if(showFullXpaths) {
        ctx.println("<br>"+fullPathFull);
    }

    if(true==false) {
        ctx.println("<br>"+"hasTests:"+p.hasTests+", props:"+p.hasProps+", hasInh:"+p.hasInherited);
    }
*/
    ctx.println("</th>");
    
    // ##2 and a half - baseline sample
    String baseExample = getBaselineExample().getExampleHtml(fullPathFull, p.displayName, ExampleGenerator.Zoomed.OUT);
    if(baseExample != null) {
        ctx.print("<td align='left' valign='top' class='generatedexample'>"+ baseExample + "</td>");
    } else {
        ctx.print("<td></td>");
    }
    
    // ##5 current control
    ctx.print("<td nowrap colspan='1' class='"+/*rclass+*/"' dir='"+ourDir+"' align='"+ourAlign+"' valign='top'>");

    if(isAlias || (p.pathWhereFound != null))  {
        ctx.println("<i dir='ltr'>(Alias - Zoom In for details)</i>");
    }

    // go find the 'current' item
    DataPod.Pea.Item current = null;
    for(Iterator j = p.items.iterator();(current==null)&&j.hasNext();) {
        DataPod.Pea.Item item = (DataPod.Pea.Item)j.next();
        if(item.altProposed == null) {
            printPeaItem(ctx, p, item, fieldHash, resultXpath, ourVoteXpath, canModify);
            current = item;
           // ctx.println("<br>");
        }
    }
    ctx.println("</td>");
    ctx.print("<td class='generatedexample' valign='top' align='left'>");
    if(current != null) {
        String itemExample = pod.exampleGenerator.getExampleHtml(current.xpath, current.value,
            ExampleGenerator.Zoomed.OUT);
        if(itemExample!=null) {
            ctx.print(itemExample);
        }
    }
    ctx.println("</td>");

    
    // ##6 proposed
    ctx.print("<td nowrap colspan='1' class='propcolumn' align='"+ourAlign+"' dir='"+ourDir+"' valign='top'>");
    // go find the 'current' item
    for(Iterator j = p.items.iterator();j.hasNext();) {
        DataPod.Pea.Item item = (DataPod.Pea.Item)j.next();
        if(item.altProposed != null) {
            printPeaItem(ctx, p, item, fieldHash, resultXpath, ourVoteXpath, canModify);
            ctx.println("<br>");
        } 
    }
    ctx.println("</td>");
    ctx.print("<td class='generatedexample' valign='top' align='left'>");
    for(Iterator j = p.items.iterator();j.hasNext();) {
        DataPod.Pea.Item item = (DataPod.Pea.Item)j.next();
        if(item.altProposed != null) {
            String itemExample = pod.exampleGenerator.getExampleHtml(item.xpath, item.value,
                ExampleGenerator.Zoomed.OUT);
            if(itemExample!=null) {
                ctx.print(itemExample);
            }
        }
    }
    ctx.println("</td>");
    
    if(phaseSubmit==true) {
        String changetoBox = "<td valign='top'>";
        // ##7 Change
        if(canModify && canSubmit ) {
            changetoBox = changetoBox+("<input name='"+fieldHash+"' id='"+fieldHash+"_ch' value='"+CHANGETO+"' type='radio' >");
        } else {
            //changetoBox = changetoBox+("<input type='radio' disabled>"); /* don't show the empty input box */
        }
        changetoBox=changetoBox+("</td>");
        
        if(!"rtl".equals(ourDir)) {
            ctx.println(changetoBox);
        }
        
        ctx.print("<td valign='top'>");
        
        boolean badInputBox = false;
        
        if(canSubmit && canModify && !p.confirmOnly) {
            String oldValue = (String)ctx.temporaryStuff.get(fieldHash+"_v");
            String fClass = "inputbox";
            if(oldValue==null) {
                oldValue="";
            } else {
                ctx.print("<span class='ferrbox'>"+ctx.iconThing("stop","this item was not accepted."));
                fClass = "badinputbox";
                badInputBox = true;
            }
            ctx.print("<input onfocus=\"document.getElementById('"+fieldHash+"_ch').click()\" name='"+fieldHash+"_v' value='"+oldValue+"' class='"+fClass+"'>");
            // references
            if(badInputBox) {
                ctx.print("</span>");
            }
            ctx.println("</td>");
        } else  {
            ctx.println("<td></td>");
        }

        if("rtl".equals(ourDir)) {
            ctx.println(changetoBox);
        }

    } else {
        ctx.println("<td colspan='2'></td>");
    }
    // ##8 References
    ctx.print("<td>");
    if(refs.length>0) {
        String refHash = fieldHash;
        ctx.print("<span id='h_ref"+refHash+"'>");
        ctx.print("<a style='text-decoration: none;' href='javascript:show(\"ref" + refHash + "\")'>" + "[refs]" /* right arrow with tail */ +"</a></span>");
        ctx.print("<!-- <noscript> </noscript> -->" + 
                    "<span style='display: none' id='ref" + refHash + "'>");
        ctx.print("<label>");
        ctx.print("<a style='text-decoration: none;' href='javascript:hide(\"ref" + refHash + "\")'>" + "[hide]" +"</a>&nbsp;");
        ctx.print("<a "+ctx.atarget("ref_"+ctx.locale)+" href='"+refCtx.url()+"'>Ref:</a>");
        if(phaseSubmit && canSubmit && canModify && !p.confirmOnly) {
            ctx.print("&nbsp;<select name='"+fieldHash+"_r'>");
            ctx.print("<option value='' SELECTED></option>");
            for(int i=0;i<refs.length;i++) {
                ctx.print("<option value='"+refs[i]+"'>"+refs[i]+"</option>");
            }
            ctx.println("</select>");
        }
        ctx.print("</label>");
        ctx.print("</span>");
    }
    ctx.println("</td>");

    // NV
    ctx.print("<td>");
    if(canModify) {
        ctx.print("<input name='"+fieldHash+"' value='"+DONTCARE+"' type='radio' "
            +((ourVoteXpath==null)?"CHECKED":"")+" >");
    }
    ctx.print("</td>");

    // zoom
    if(!zoomedIn) {
        if(foundError || foundWarning) {
            ctx.println("<td title='Errors or Warnings- please zoom in' class='warning'>");
        } else {
            ctx.println("<td>");
        }
        //if(canModify && canSubmit) {
        fora.showForumLink(ctx, pod, p, p.superPea.base_xpath);
        //}
        ctx.println("</td>");
    } else {
        ctx.println("<td></td>");
    }
    
    ctx.println("</tr>");
}

void printPeaItem(WebContext ctx, DataPod.Pea p, DataPod.Pea.Item item, String fieldHash, String resultXpath, String ourVoteXpath, boolean canModify) {
//ctx.println("<div style='border: 2px dashed red'>altProposed="+item.altProposed+", inheritFrom="+item.inheritFrom+", confirmOnly="+new Boolean(p.confirmOnly)+"</div><br>");
    boolean winner = 
        ((resultXpath!=null)&&
        (item.xpath!=null)&&
        (item.xpath.equals(resultXpath)));
        

    String pClass ="";
    if(winner) {
        pClass = "class='winner' title='Winning item.'";
    } else if ((item.inheritFrom != null) &&(p.inheritFrom==null)) {
        pClass = "class='fallback' title='Fallback from "+item.inheritFrom+"'";
    } else if(item.altProposed != null) {
        pClass = "class='loser' title='proposed, losing item'";
    } else if(p.inheritFrom != null) {
        pClass = "class='missing'";
    } else {
        pClass = "class='loser'";
    }

    ctx.println("<span "+pClass+">");
    if(canModify) {      
        boolean checkThis = 
            ((ourVoteXpath!=null)&&
            (item.xpath!=null)&&
            (item.xpath.equals(ourVoteXpath)));
        
        ctx.print("<input title='#"+item.xpathId+"' name='"+fieldHash+"'  value='"+
            ((item.altProposed!=null)?item.altProposed:CONFIRM)+"' "+(checkThis?"CHECKED":"")+"  type='radio'>");
    } else {
        ctx.print("<input title='#"+item.xpathId+"' type='radio' disabled>");
    }
    
    if(item.value.length()!=0) {
        ctx.print(item.value);
    } else {
        ctx.print("<i dir='ltr'>(empty)</i>");
    }
    ctx.println("</span>");
    /*

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
  */
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

public int showSkipBox(WebContext ctx, int total, DataPod.DisplaySet displaySet,
    boolean showSearchMode, String sortMode, int skip) {
showSearchMode = true;// all
    List displayList = null;
    if(displaySet != null) {
        displayList = displaySet.displayPeas;
    }
    ctx.println("<div class='pager' style='margin: 2px'>");
    if((ctx.locale != null) && UserRegistry.userCanModifyLocale(ctx.session.user,ctx.locale.toString())) { // at least street level
        if((ctx.field(QUERY_SECTION).length()>0) && !ctx.field(QUERY_SECTION).equals(xMAIN)) {
            ctx.println("<input  type='submit' value='" + xSAVE + "'>"); // style='float:left'
        }
    }
    if(showSearchMode) {
        ctx.println("<p style='float: right; margin-left: 3em;'> " + 
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
        ctx.println("</p>");
    }

    // TODO: replace with ctx.fieldValue("skip",-1)
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

private void createBasicCldr(File homeFile) {
    System.err.println("Attempting to create /cldr  dir at " + homeFile.getAbsolutePath());

    try {
        homeFile.mkdir();
        File propsFile = new File(homeFile, "cldr.properties");
        OutputStream file = new FileOutputStream(propsFile, false); // Append
        PrintWriter pw = new PrintWriter(file);

        pw.println("## autogenerated cldr.properties config file");
        pw.println("## generated on " + localhost() + " at "+new Date());
        pw.println("## see the readme at \n## "+URL_CLDR+"data/tools/java/org/unicode/cldr/web/data/readme.txt ");
        pw.println("## make sure these settings are OK,\n## and comment out CLDR_MESSAGE for normal operation");
        pw.println("##");
        pw.println("## SurveyTool must be reloaded, or the web server restarted, \n## for these to take effect.");
        pw.println();
        pw.println("## your password. Login as user 'admin@' and this password for admin access.");
        pw.println("CLDR_VAP="+UserRegistry.makePassword("admin@"));
        pw.println();
        pw.println("## Special message shown to users as to why survey tool is down.");
        pw.println("## Comment out for normal start-up.");
        pw.println("CLDR_MESSAGE=Welcome to SurveyTool@"+localhost()+". Please edit "+homeFile.getAbsolutePath()+"/cldr.properties. Comment out CLDR_MESSAGE to continue normal startup.");
        pw.println();
        pw.println("## Special message shown to users.");
        pw.println("CLDR_HEADER=Welcome to SurveyTool@"+localhost()+". Please edit "+homeFile.getAbsolutePath()+"/cldr.properties to change CLDR_HEADER (to change this message), or comment it out entirely.");
        pw.println();
        pw.println("## CLDR common data. Default value shown, uncomment to override");
        pw.println("CLDR_COMMON="+homeFile.getAbsolutePath()+"/common");
        pw.println();
        pw.println("## SMTP server. Mail is disabled by default.");
        pw.println("#CLDR_SMTP=127.0.0.1");
        pw.println();
        pw.println("## FROM address for mail. Don't be a bad administrator, change this.");
        pw.println("#CLDR_FROM=bad_administrator@"+localhost());
        pw.println();
        pw.println("# That's all!");
        pw.close();
        file.close();
    }
    catch(IOException exception){
      System.err.println("While writing "+homeFile.getAbsolutePath()+" props: "+exception);
      exception.printStackTrace();
    }
}
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
                createBasicCldr(homeFile); // attempt to create
            }
            
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
    File vetdir = new File(vetdata);
    if(!vetdir.isDirectory()) {
        vetdir.mkdir();
        System.err.println("## creating empty vetdir: " + vetdir.getAbsolutePath());
    }
    if(!vetdir.isDirectory()) {
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
    if("yes".equals(survprops.getProperty("CLDR_OFFICIAL"))) {
        isUnofficial = false;
    }
    vetweb = survprops.getProperty("CLDR_VET_WEB",cldrHome+"/vetdata"); // dir for web data
    cldrLoad = survprops.getProperty("CLDR_LOAD_ALL"); // preload all locales?
    // System.getProperty("CLDR_COMMON") + "/main" is ignored.
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

    supplemental = new SupplementalData(fileBase + "/../supplemental/");

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

private static Set localeListSet = null;

static synchronized protected Set getLocalesSet() {
    if(localeListSet == null ) {
        File inFiles[] = getInFiles();
        int nrInFiles = inFiles.length;
        Set s = new HashSet();
        for(int i=0;i<nrInFiles;i++) {
            String fileName = inFiles[i].getName();
            int dot = fileName.indexOf('.');
            if(dot !=  -1) {
                String locale = fileName.substring(0,dot);
                s.add(locale);
            }
        }
        localeListSet = s;
    }
    return localeListSet;
}

static protected String[] getLocales() {
    return (String[])getLocalesSet().toArray(new String[0]);
}

/**
 * Returns a Map of all interest groups.
 * en -> en, en_US, en_MT, ...
 * fr -> fr, fr_BE, fr_FR, ...
 */
static protected Map getIntGroups() {
    // TODO: rewrite as iterator
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

public boolean isValidLocale(String locale) {
    return getLocalesSet().contains(locale);
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
}


public static final com.ibm.icu.text.Transliterator hexXML
    = com.ibm.icu.text.Transliterator.getInstance(
    "[^\\u0009\\u000A\\u0020-\\u007E\\u00A0-\\u00FF] Any-Hex/XML");

// like above, but quote " 
public static final com.ibm.icu.text.Transliterator quoteXML 
    = com.ibm.icu.text.Transliterator.getInstance(
     "[^\\u0009\\u000A\\u0020-\\u0021\\u0023-\\u007E\\u00A0-\\u00FF] Any-Hex/XML");

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
    }
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
        return;
    }
    catch (Throwable t)
    {
        busted("Some error on DB startup: " + t.toString());
        t.printStackTrace();
        return;
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
        return;
    }
    try {
        xpt = XPathTable.createTable(logger, getDBConnection(), !doesExist, this);
    } catch (SQLException e) {
        busted("On XPathTable startup: " + unchainSqlException(e));
        return;
    }
    // note: make xpt before CLDRDBSource..
    try {
        CLDRDBSource.setupDB(logger, getDBConnection(), !doesExist, this);
    } catch (SQLException e) {
        busted("On CLDRDBSource startup: " + unchainSqlException(e));
        return;
    }
    try {
        vet = Vetting.createTable(logger, getDBConnection(), this);
    } catch (SQLException e) {
        busted("On Vetting startup: " + unchainSqlException(e));
        return;
    }
    try {
        fora = SurveyForum.createTable(logger, getDBConnection(), this);
    } catch (SQLException e) {
        busted("On Fora startup: " + unchainSqlException(e));
        return;
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
            if(isBusted != null)  {
                return;
            }
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
                        checkCldr.setDisplayInformation(sm.getBaselineFile());
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
        return timeDiff(a,System.currentTimeMillis());
    }
    
    public static final String timeDiff(long a, long b) {
        final long ONE_DAY = 86400*1000;
        final long A_LONG_TIME = ONE_DAY*3;
        if((b-a)>(A_LONG_TIME)) {
            double del = (b-a);
            del /= ONE_DAY;
            int days = (int)del;
            return days + " days";
        } else {
            return ElapsedTimer.elapsedTime(a,b);
        }
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
    
    /**
     * get the local host 
     */
    public static String localhost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    public static String bugReplyUrl(String folder, int number, String subject) {
        return BUG_URL_BASE + "/"+folder+"?compose="+number+"&subject="+java.net.URLEncoder.encode(subject);
    }
    

}
