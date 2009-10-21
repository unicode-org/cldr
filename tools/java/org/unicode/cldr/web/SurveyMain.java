/*
 ******************************************************************************
 * Copyright (C) 2004-2009, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.test.ExampleGenerator.ExampleContext;
import org.unicode.cldr.test.ExampleGenerator.ExampleType;
import org.unicode.cldr.test.ExampleGenerator.HelpMessages;
import org.unicode.cldr.tool.ShowData;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CachingEntityResolver;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalData;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.web.CLDRDBSourceFactory.CLDRDBSource;
import org.unicode.cldr.web.DataSection.DataRow;
import org.unicode.cldr.web.DataSection.DataRow.CandidateItem;
import org.unicode.cldr.web.SurveyThread.SurveyTask;
import org.unicode.cldr.web.UserRegistry.User;
import org.unicode.cldr.web.Vetting.DataSubmissionResultHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.ElapsedTimer;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

/**
 * The main servlet class of Survey Tool
 */
public class SurveyMain extends HttpServlet {
	public static final String SURVEYMAIN_REVISION = "$Revision$";

    private static final String CLDR_BULK_DIR = "CLDR_BULK_DIR";
	private static final String ACTION_DEL = "_del";
    private static final String ACTION_UNVOTE = "_unvote";
    private static final String XML_CACHE_PROPERTIES = "xmlCache.properties";
    /**
     * 
     */
    private static final long serialVersionUID = -3587451989643792204L;

    /**
     * This class enumerates the current phase of the survey tool
     * @author srl
     *
     */
	public enum Phase { 
        SUBMIT("Data Submission"),           // SUBMISSION
        VETTING("Vetting"),
        VETTING_CLOSED("Vetting Closed"),   // closed after vetting - open for admin
        CLOSED("Closed"),           // closed
        DISPUTED("Dispute Resolution"),
        FINAL_TESTING("Final Testing"),    //FINAL_TESTING
        READONLY("Read-Only"),
        BETA("Beta");
        
        private String what;
        private Phase(String s) {
            what = s;
        }
        public String toString() {
            return what;
        }
    }; 
    
    // ===== Configuration state
    private static Phase currentPhase = null; /** set by CLDR_PHASE property. **/
    private static String oldVersion;
    private static String newVersion;
    public static       boolean isUnofficial = true;  /** set to true for all but the official installation of ST. **/

    // ==== caches and general state
    
    public UserRegistry reg = null;
    public XPathTable   xpt = null;
    public Vetting      vet = null;
    public SurveyForum  fora = null;
    public CLDRDBSourceFactory dbsrcfac = null;
    public LocaleChangeRegistry lcr = new LocaleChangeRegistry();
    static ElapsedTimer uptime = new ElapsedTimer("ST uptime: {0}");
    ElapsedTimer startupTime = new ElapsedTimer("{0} until first GET/POST");
    public static String isBusted = null;
    private static String isBustedStack = null;
    private static Throwable isBustedThrowable = null;
    private static ElapsedTimer isBustedTimer = null;
    ServletConfig config = null;

    
    // ===== UI constants
    static final String CURRENT_NAME="Others";

    // ===== Special bug numbers.
//    public static final String BUG_METAZONE_FOLDER = "data";
//    public static final int    BUG_METAZONE = 1262;
    //public static final String BUG_ST_FOLDER = "tools";
    //public static final int    BUG_ST = xxxx; // was: umbrella bug
    public static final String URL_HOST = "http://www.unicode.org/";
    public static final String URL_CLDR = URL_HOST+"cldr/";
    public static final String BUG_URL_BASE = URL_CLDR+"bugs/locale-bugs";
    public static final String GENERAL_HELP_URL = URL_CLDR+"survey_tool.html";
    public static final String GENERAL_HELP_NAME = "General&nbsp;Instructions";
    
    // ===== url prefix for help
//    public static final String CLDR_WIKI_BASE = URL_CLDR+"wiki";
//    public static final String CLDR_HELP_LINK = CLDR_WIKI_BASE+"?SurveyToolHelp";
//    public static final String CLDR_HELP_LINK_EDIT = CLDR_HELP_LINK;
    public static final String CLDR_HELP_LINK = GENERAL_HELP_URL+"#";
     
    public static final String SLOW_PAGE_NOTICE = ("<i>Note: The first time a page is loaded it may take some time, please be patient.</i>");    

    // ===== Hash keys and field values
    public static final String SUBVETTING = "//v";
    public static final String SUBNEW = "//n"; 
    public static final String NOCHANGE = "nochange";
    public static final String CURRENT = "current";
    public static final String PROPOSED = "proposed";
    public static final String NEW = "new";
    public static final String DRAFT = "draft";
    public static final String UNKNOWNCHANGE = "Click to suggest replacement";
    public static final String DONTCARE = "abstain";
    public static final boolean HAVE_REMOVE = false;
    public static final String REMOVE = "remove";
    public static final String CONFIRM = "confirm";
    public static final String INHERITED_VALUE = "inherited-value";
    public static final String CHANGETO = "change to";
    public static final String PROPOSED_DRAFT = "proposed-draft";
    public static final String MKREFERENCE = "enter-reference";
    public static final String STFORM = "stform";
    //public static final String MODIFY_THING = "<span title='You are allowed to modify this locale.'>\u270D</span>";             // was: F802
    //public static final String MODIFY_THING = "<img src='' title='You are allowed to modify this locale.'>";             // was: F802
    static String modifyThing(WebContext ctx) {
        return "&nbsp;"+ctx.modifyThing("You are allowed to modify this locale.");
    }
    
    // ========= SYSTEM PROPERTIES
    public static  String vap = System.getProperty("CLDR_VAP"); // Vet Access Password
    public static  String vetdata = System.getProperty("CLDR_VET_DATA"); // dir for vetted data
    File vetdir = null;
    public static  String vetweb = System.getProperty("CLDR_VET_WEB"); // dir for web data
    public static  String cldrLoad = System.getProperty("CLDR_LOAD_ALL"); // preload all locales?
    static String fileBase = null; // not static - may change lager
    static String specialMessage = System.getProperty("CLDR_MESSAGE"); //  static - may change later
    static String specialHeader = System.getProperty("CLDR_HEADER"); //  static - may change later
    static String lockOut = System.getProperty("CLDR_LOCKOUT"); //  static - may change later
    static long specialTimer = 0; // 0 means off.  Nonzero:  expiry time of countdown.
    static int progressMax = 0;  // "an operation is in progress"
    static int progressCount = 0;
    static String progressSub = null; 
    static String progressWhat = null;
    
    public static java.util.Properties survprops = null;
    public static String cldrHome = null;
    public static File homeFile = null;

    // Logging
    public static Logger logger = Logger.getLogger("org.unicode.cldr.SurveyMain");
    public static java.util.logging.Handler loggingHandler = null;
            
//    public static final String LOGFILE = "cldr.log";        // log file of all changes
    public static final ULocale inLocale = new ULocale("en"); // locale to use to 'localize' things

    // ======= query fields
    public static final String QUERY_TERRITORY = "territory";
    public static final String QUERY_ZONE = "zone";
    static final String QUERY_PASSWORD = "pw";
    static final String QUERY_PASSWORD_ALT = "uid";
    static final String QUERY_EMAIL = "email";
    static final String QUERY_SESSION = "s";
    public static final String QUERY_LOCALE = "_";
    public static final String QUERY_SECTION = "x";
    static final String QUERY_EXAMPLE = "e";
    static final String QUERY_DO = "do";
	
    static final String SURVEYTOOL_COOKIE_SESSION = CookieSession.class.getPackage().getName()+".id";
    static final String SURVEYTOOL_COOKIE_NONE = "0";
    static final String PREF_SHOWCODES = "p_codes";
    static final String PREF_SORTMODE = "p_sort";
    static final String PREF_SHOWUNVOTE = "p_unvote";
    static final String PREF_SHOWLOCKED = "p_showlocked";
    static final String PREF_NOPOPUPS = "p_nopopups";
    static final String PREF_CODES_PER_PAGE = "p_pager";
    static final String PREF_XPID = "p_xpid";
    static final String PREF_GROTTY = "p_grotty";
    static final String PREF_SORTMODE_CODE = "code";
    static final String PREF_SORTMODE_ALPHA = "alpha";
    static final String PREF_SORTMODE_WARNING = "interest";
    static final String PREF_SORTMODE_NAME = "name";
    static final String PREF_SORTMODE_DEFAULT = PREF_SORTMODE_WARNING;
//	static final String PREF_SHOW_VOTING_ALTS = "p_vetting_details";
    static final String PREF_NOSHOWDELETE = "p_nodelete";
    static final String PREF_DELETEZOOMOUT = "p_deletezoomout";
    static final String PREF_NOJAVASCRIPT = "p_nojavascript";
    static final String PREF_JAVASCRIPT = PREF_NOJAVASCRIPT;
    static final String PREF_ADV = "p_adv"; // show advanced prefs?
    static final String PREF_XPATHS = "p_xpaths"; // show xpaths?
    public static final String PREF_COVLEV = "p_covlev"; // covlev
    public static final String PREF_COVTYP = "p_covtyp"; // covtyp
    //    static final String PREF_SORTMODE_DEFAULT = PREF_SORTMODE_WARNING;
    static final String  BASELINE_ID = "en";
    static final ULocale BASELINE_LOCALE = new ULocale(BASELINE_ID);
    static final String  BASELINE_NAME = BASELINE_LOCALE.getDisplayName(BASELINE_LOCALE);
    public static final String METAZONE_EPOCH = "1970-01-01";
  
    // ========== lengths
    static final int CODES_PER_PAGE = 80;  // This is only a default.
    static final int PAGER_SHORTEN_WIDTH = 25   ; // # of chars in the 'pager' list before they are shortened
    static final int REFS_SHORTEN_WIDTH = 120;


    static final String MEASNAME = "measurementSystemName";
    static final String CODEPATTERN = "codePattern";
    public static final String CURRENCYTYPE = "//ldml/"+PathUtilities.NUMBERSCURRENCIES+"/currency[@type='";
    public static String xMAIN = "general";
    public static String xREMOVE = "REMOVE";

    public static final String GREGORIAN_CALENDAR = "gregorian calendar";
    public static final String OTHER_CALENDARS = "other calendars";
    // 
    public String CALENDARS_ITEMS[];
    public String METAZONES_ITEMS[];

    public static final String OTHERROOTS_ITEMS[] = {
        LDMLConstants.CHARACTERS,
        LDMLConstants.NUMBERS,
        LDMLConstants.LOCALEDISPLAYPATTERN,
        "units",
        PathUtilities.xOTHER,
        "references"
    };

    public static final String GREGO_XPATH = "//ldml/dates/"+LDMLConstants.CALENDARS+"/"+LDMLConstants.CALENDAR+"[@type=\"gregorian\"]";
    public static final String RAW_MENU_ITEM = "raw";
    public static final String TEST_MENU_ITEM = "test";
    
    public static final String SHOWHIDE_SCRIPT = "<script type='text/javascript'><!-- \n" +
                                                "function show(what)\n" +
                                                "{document.getElementById(what).style.display=\"block\";\ndocument.getElementById(\"h_\"+what).style.display=\"none\";}\n" +
                                                "function hide(what)\n" +
                                                "{document.getElementById(what).style.display=\"none\";\ndocument.getElementById(\"h_\"+what).style.display=\"block\";}\n" +
                                                "--></script>";
    
    static final HelpMessages surveyToolSystemMessages = new HelpMessages("st_sysmsg.html");
    
    static String sysmsg(String msg) {
        try {
            return surveyToolSystemMessages.find(msg);
        } catch(Throwable t) {
            System.err.println("Err " + t.toString() + " while trying to load sysmsg " + msg);
            return "[MISSING MSG: " + msg+"]";
        }
    }
    /**
     * Servlet initializer
     */
    public final void init(final ServletConfig config)
    throws ServletException {
        new com.ibm.icu.text.SimpleDateFormat(); // Ensure that ICU is available before we get any farther.
        super.init(config);
        cldrHome = config.getInitParameter("cldr.home");
        this.config = config;
        
        startupThread.addTask(new SurveyThread.SurveyTask("startup") {
			public void run() throws Throwable{
				doStartup();
			}
        });
        
        startupThread.start();
        System.err.println("Startup thread launched");
    }

    public SurveyMain() {
    }

    /**
    * output MIME header, build context, and run code..
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)    throws IOException, ServletException
    {
        doGet(request,response);
    }
                    
    /**
     * IP blacklist
     */
    static final private String BAD_IPS [] = {
   /*             "199.89.199.82",
                "66.154.103.161",
                "66.249.66.5", // googlebot
                "203.148.64.17",
				"209.249.11.4",
                "65.55.212.189", // MSFT search.live.com
                "38.98.120.72", // 38.98.120.72 - feb 7, 2007-  lots of connections
                "124.129.175.245",  // NXDOMAIN @ sdjnptt.net.cn
                //"128.194.135.94", // crawler4.irl.cs.tamu.edu
                 */
                /*
                209.249.11.4		see | be | kick
10:32	Guest
64.5.245.50	 German (Liechtenstein)	see | be | kick
11:32	Guest
64.5.245.50	 German (Liechtenstein)	see | be | kick
11:11	Guest
209.249.11.4		see | be | kick
12:32	Guest
64.5.245.50	 German (Liechtenstein)	see | be | kick
12:02	Guest
209.249.11.4		see | be | kick
12:12	Guest
209.249.11.4		see | be | kick
13:32	Guest
*/
		"66.249.67.196",
                 };
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
    {
        if(!ensureStartup(request, response)) {
            return;
        }
        
        if(isBusted==null) {
            if(startupTime != null) {
                String startupMsg = null;
                startupMsg = (startupTime.toString());
    //            logger.info(startupMsg);  // log how long startup took
                startupTime = null;
            }
            
            String remoteIP = request.getRemoteAddr();
            
            for( String badIP : BAD_IPS ) {  // no spiders, please.
                if(badIP.equals(remoteIP)) {
                    response.sendRedirect(URL_CLDR);
                }
            }
            
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires",0);
            response.setHeader("Pragma","no-cache");
            response.setDateHeader("Max-Age",0);
            response.setHeader("Robots", "noindex,nofollow");
            
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
        }
        com.ibm.icu.dev.test.util.ElapsedTimer reqTimer = new com.ibm.icu.dev.test.util.ElapsedTimer();
        
        /**
         * Busted: unrecoverable error, do not attempt to go on.
         */
        if(isBusted != null) {
            String pi = request.getParameter("sql"); // allow sql
            if((pi==null) || (!pi.equals(vap))) {
                response.setContentType("text/html; charset=utf-8");
                PrintWriter out = response.getWriter();
                out.println("<html>");
                out.println("<head>");
                out.println("<title>CLDR Survey Tool offline</title>");
                out.println("<link rel='stylesheet' type='text/css' href='"+ request.getContextPath() + "/" + "surveytool.css" + "'>");
                out.println(SHOWHIDE_SCRIPT);
                out.println("</head>");
                out.println("<body>");
                showSpecialHeader(out);
                out.println("<h1>The CLDR Survey Tool is offline</h1>");
                out.println("<div class='ferrbox'><pre>" + isBusted +"</pre><hr>");
                out.println("\n");
                out.println(getShortened((SurveyForum.HTMLSafe(isBustedStack).replaceAll("\t", "&nbsp;&nbsp;&nbsp;").replaceAll("\n", "<br>"))));
                out.println("</div><br>");
                
                
                out.println("<hr>");
//                if(!isUnofficial) {
//                    out.println("Please try this link for info: <a href='"+CLDR_WIKI_BASE+"?SurveyTool/Status'>"+CLDR_WIKI_BASE+"?SurveyTool/Status</a><br>");
//                    out.println("<hr>");
//                }
                out.println("An Administrator must intervene to bring the Survey Tool back online. <br> " + 
                            " <i>This message has been viewed " + pages + " time(s), SurveyTool has been down for " + isBustedTimer + "</i>");

//                if(false) { // dump static tables.
//                    response.setContentType("application/xml; charset=utf-8");
//                    WebContext xctx = new WebContext(request,response);
//                    xctx.staticInfo();
//                    xctx.close();
//                }
                return;        
            }
        }
        
        /** User database request
         * 
         */
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

        // set up users context object
        WebContext ctx = new WebContext(request,response);
        ctx.reqTimer = reqTimer;
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
        
        String baseThreadName = Thread.currentThread().getName();
        
        try {
	
	                    
	        if(ctx.field("dump").equals(vap)) {
	        	Thread.currentThread().setName(baseThreadName+" ST admin");
	            doAdminPanel(ctx); // admin interface
	        } else if(ctx.field("sql").equals(vap)) {
	        	Thread.currentThread().setName(baseThreadName+" ST sql");
	            doSql(ctx); // SQL interface
	        } else {
	        	Thread.currentThread().setName(baseThreadName+" ST ");
	            doSession(ctx); // Session-based Survey main
	        }
	        ctx.close();
        } finally {
        	Thread.currentThread().setName(baseThreadName);
        }
    }
    
    /**
     * Make sure we're started up, otherwise tell 'em, "please wait.."
     * @param request
     * @param response
     * @return true if started, false if we are not (on false, get out, we're done printing..)
     * @throws IOException
     */
    private boolean ensureStartup(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if(!isSetup) {
            boolean isGET = "GET".equals(request.getMethod());
            int sec = 4;
            if(isBusted != null) {
		sec = 120;
            }
            String base = WebContext.base(request);
            if(isGET) {
                String qs  = "";
                String pi = "";
                if(request.getPathInfo()!=null&&request.getPathInfo().length()>0) {
                    pi = request.getPathInfo();
                }
                if(request.getQueryString()!=null&&request.getQueryString().length()>0) {
                    qs = "?"+request.getQueryString();
                }
                response.setHeader("Refresh", sec+"; "+base+pi+qs);
            }
            response.setContentType("text/html; charset=utf-8");
            PrintWriter out = response.getWriter();
            out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\"><html><head>");
            out.println("<title>"+sysmsg("startup_title")+"</title>");
            out.println("<link rel='stylesheet' type='text/css' href='"+base+"/../surveytool.css'>");
            out.println("</head><body>");
            if(isUnofficial) {
                out.print("<div class='topnotices'><p class='unofficial' title='Not an official SurveyTool' >");
                out.print("Unofficial");
                out.println("</p></div>");
            }
            if(isBusted != null) {
                out.println(SHOWHIDE_SCRIPT);
                out.println("</head>");
                out.println("<body>");
                showSpecialHeader(out);
                out.println("<h1>The CLDR Survey Tool is offline</h1>");
                out.println("<div class='ferrbox'><pre>" + isBusted +"</pre><hr>");
                out.println("\n");
                out.println(getShortened((SurveyForum.HTMLSafe(isBustedStack).replaceAll("\t", "&nbsp;&nbsp;&nbsp;").replaceAll("\n", "<br>"))));
                out.println("</div><br>");


                out.println("<hr>");
//                if(!isUnofficial) {
//                    out.println("Please try this link for info: <a href='"+CLDR_WIKI_BASE+"?SurveyTool/Status'>"+CLDR_WIKI_BASE+"?SurveyTool/Status</a><br>");
//                    out.println("<hr>");
//                }
                out.println("An Administrator must intervene to bring the Survey Tool back online. <br> " +
                            " <i>This message has been viewed " + pages + " time(s), SurveyTool has been down for " + isBustedTimer + "</i>");
            } else {
            out.print(sysmsg("startup_header"));
            String threadInfo = startupThread.htmlStatus();
            if(threadInfo!=null) {
            	out.println("<b>Processing:"+threadInfo+"</b><br>");
            }
            if(progressWhat != null) {
                out.println(getProgress()+"<br><hr><br>");
            }
            out.print(sysmsg("startup_wait"));
        }
            out.println("<br><i> "+uptime+"</i><br>");
            if(!isGET) {
                out.println("(Sorry,  we can't automatically retry your "+request.getMethod()+" request - you may attempt Reload in a few seconds "+
                            "<a href='"+base+"'>or click here</a><br>");
            } else {
                out.println("We will <a href='"+base+"'>reload this page in "+sec+" seconds, or you may click here.</a>");
            }
            out.print(sysmsg("startup_footer"));
            if(!SurveyMain.isUnofficial) {
            	out.println(ShowData.ANALYTICS);
            }
            out.print("</body></html>");
            return false;
        } else {
            return true;
        }
    }

    /** SQL Console
    */
    private void doSql(WebContext ctx)
    {
        printHeader(ctx, "SQL Console@"+localhost());
        String q = ctx.field("q");
        boolean tblsel = false;        
        printAdminMenu(ctx, "/AdminSql");
        ctx.println("<h1>SQL Console</h1>");
        
        if((dbDir == null) || (isBusted != null)) { // This may or may not work. Survey Tool is busted, can we attempt to get in via SQL?
            ctx.println("<h4>ST busted, attempting to make SQL available via " + cldrHome + "</h4>");
            ctx.println("<pre>");
            specialMessage = "<b>SurveyTool is in an administrative mode- please log off.</b>";
            try {
                if(cldrHome == null) {
                    cldrHome = System.getProperty("catalina.home");
                    if(cldrHome == null) {  
                        busted("no $(catalina.home) set - please use it or set a servlet parameter cldr.home");
                        return;
                    } 
                    File homeFile = new File(cldrHome, "cldr");
                    
                    if(!homeFile.exists()) {
                        throw new InternalError("CLDR basic does not exist- delete parent and start over.");
//                        createBasicCldr(homeFile); // attempt to create
                    }
                    
                    if(!homeFile.exists()) {
                        busted("$(catalina.home)/cldr isn't working as a CLDR home. Not a directory: " + homeFile.getAbsolutePath());
                        return;
                    }
                    cldrHome = homeFile.getAbsolutePath();
                }
                ctx.println("home: " + cldrHome);
                doStartupDB();
            } catch(Throwable t) {
                ctx.println("Caught: " + t.toString()+"\n");
            }
            ctx.println("</pre>");
        }
        
        if(q.length() == 0) {
            q = DB_SQL_ALLTABLES;
            tblsel = true;
        } else {
            ctx.println("<a href='" + ctx.base() + "?sql=" + vap + "'>[List of Tables]</a>");
        }
        ctx.println("<form method=POST action='" + ctx.base() + "'>");
        ctx.println("<input type=hidden name=sql value='" + vap + "'>");
        ctx.println("SQL: <input class='inputbox' name=q size=80 cols=80 value=\"" + q + "\"><br>");
        ctx.println("<label style='border: 1px'><input type=checkbox name=unltd>Show all?</label> ");
        ctx.println("<label style='border: 1px'><input type=checkbox name=isUpdate>U/I/D?</label> ");
//        ctx.println("<label style='border: 1px'><input type=checkbox name=isUser>UserDB?</label> ");
        ctx.println("<input type=submit name=do value=Query>");
        ctx.println("</form>");

        Connection conn = null;

        if(q.length()>0) try {
            logger.severe("Raw SQL: " + q);
            ctx.println("<hr>");
            ctx.println("query: <tt>" + q + "</tt><br><br>");
            Statement s = null;
            try {
                int i,j;
                
                com.ibm.icu.dev.test.util.ElapsedTimer et = new com.ibm.icu.dev.test.util.ElapsedTimer();
                
//                if(ctx.field("isUser").length()>0) {
//                    conn = getU_DBConnection();
//                } else {
                    conn = getDBConnection();
//                }
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
                            case java.sql.Types.TIMESTAMP: ctx.println("TIMESTAMP"); break;
                            case java.sql.Types.BINARY: ctx.println("BINARY"); break;
                            case java.sql.Types.LONGVARBINARY: ctx.println("LONGVARBINARY"); break;
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
                            String v;
                            try {
                                v = rs.getString(i);
                            } catch(SQLException se) {
                                if(se.getSQLState().equals("S1009")) {
                                    v="0000-00-00 00:00:00";
                                } else {
                                    v = "(Err:"+unchainSqlException(se)+")";
                                }
                            } catch (Throwable t) {
                                t.printStackTrace();
                                v = "(Err:"+t.toString()+")";
                            }
                            if(v != null) {
                                ctx.println("<td>"  );
                                if(rsm.getColumnType(i)==java.sql.Types.LONGVARBINARY) {
                                    String uni = SurveyMain.getStringUTF8(rs, i);
                                    ctx.println(uni+"<br>");
                                    byte bytes[] = rs.getBytes(i);
                                    for(byte b : bytes) {
                                        ctx.println(Integer.toHexString(((int)b)&0xFF));
                                    }
//                                } else if(rsm.getColumnType(i)==java.sql.Types.TIMESTAMP) {
//                                    String out="(unknown)";
//                                    try {
//                                        out=v.t
//                                    }
                                } else {
                                    ctx.println(v );
//                                  ctx.println("<br>"+rsm.getColumnTypeName(i));
                                }
                                ctx.print("</td>");
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
            } catch(SQLException se) {
                String complaint = "SQL err: " + unchainSqlException(se);
                
                ctx.println("<pre class='ferrbox'>" + complaint + "</pre>" );
                logger.severe(complaint);
            } catch(Throwable t) {
                String complaint = t.toString();
				t.printStackTrace();
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
        } finally {
            SurveyMain.closeDBConnection(conn);
        }
        printFooter(ctx);
    }
    
    /**
     * Print memory stats
     * @return
     */
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
	
    /** Hash of twiddlable (toggleable) parameters
     * 
     */
	Hashtable twidHash = new Hashtable();

    boolean showToggleTwid(WebContext ctx, String pref, String what) {
		String qKey = "twidb_"+pref;
		String nVal = ctx.field(qKey);
		if(nVal.length()>0) {
			twidPut(pref,new Boolean(nVal).booleanValue());
			ctx.println("<div style='float: right;'><b class='disputed'>changed</b></div>");
		}
        boolean val = twidGetBool(pref, false);
        WebContext nuCtx = (WebContext)ctx.clone();
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

	private boolean twidGetBool(String key) {
        return twidGetBool(key,false);
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
    
    /**
     * Admin panel
     * @param ctx
     * @param helpLink
     */
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
    
    private void doAdminPanel(WebContext ctx)
    {
        String action = ctx.field("action");
        printHeader(ctx, "ST Admin@"+localhost() + " | " + action);
        printAdminMenu(ctx, "/AdminDump");
        ctx.println("<h1>SurveyTool Administration</h1>");
        ctx.println("<hr>");
        
        
        if(action.equals("")) {
            action = "sessions";
        }
        WebContext actionCtx = (WebContext)ctx.clone();
        actionCtx.addQuery("dump",vap);
		WebContext actionSubCtx = (WebContext)actionCtx.clone();
		actionSubCtx.addQuery("action",action);

		actionCtx.println("Click here to update data: ");
        printMenu(actionCtx, action, "upd_1", "Easy Data Update", "action");       
        actionCtx.println(" <br> ");
        printMenu(actionCtx, action, "sessions", "Sessions", "action");    
            actionCtx.println(" | ");
            printMenu(actionCtx, action, "stats", "Stats", "action");       
            actionCtx.println(" | ");
            printMenu(actionCtx, action, "tasks", "Tasks", "action");       
            actionCtx.println(" | ");
        printMenu(actionCtx, action, "statics", "Statics", "action");       
            actionCtx.println(" | ");
        printMenu(actionCtx, action, "specialusers", "Specialusers", "action");       
            actionCtx.println(" | ");
        printMenu(actionCtx, action, "specialmsg", "Update Special Message", "action");       
        actionCtx.println(" | ");
        printMenu(actionCtx, action, "upd_src", "Manage Sources", "action");       
        actionCtx.println(" | ");
        printMenu(actionCtx, action, "load_all", "Load all locales", "action");       
        actionCtx.println(" | ");
        printMenu(actionCtx, action, "add_locale", "Add a locale", "action");       
        actionCtx.println(" | ");
        printMenu(actionCtx, action, "bulk_submit", "Bulk Data Submit", "action");       
            actionCtx.println(" | ");
        printMenu(actionCtx, action, "srl", "EXPERT-ADMIN-use-only", "action");  // Dangerous items
		
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
            if(gBaselineHash != null) {
                ctx.println("baselinecache info: " + (gBaselineHash.size()) + " items."  +"<br>");
            }
            ctx.println("CLDRFile.distinguishedXPathStats(): " + CLDRFile.distinguishedXPathStats() + "<br>");
            ctx.println("</div>");
            
            ctx.println("<a class='notselected' href='" + ctx.jspLink("about.jsp") +"'>More version information...</a><br/>");
            
        } else if(action.equals("statics")) {
            ctx.println("<h1>Statics</h1>");
            ctx.staticInfo();
        } else if(action.equals("tasks")) {
            WebContext subCtx = (WebContext)ctx.clone();
            actionCtx.addQuery("action",action);
            ctx.println("<h1>Tasks</h1>");
            ctx.println("Thread: "+startupThread+"<br>");
            ctx.println("<hr>");
            
            /*
            ctx.println("<form method='POST' action='"+actionCtx.url()+"'>");
            actionCtx.printUrlAsHiddenFields();
            ctx.println("<input type=submit value='Do Nothing For Ten Seconds' name='10s'></form>");
             	*/
            SurveyThread.SurveyTask acurrent = startupThread.current;

            if(acurrent!=null) {
	            ctx.println("<form method='POST' action='"+actionCtx.url()+"'>");
	            actionCtx.printUrlAsHiddenFields();
	            ctx.println("<input type=submit value='Stop Active Task' name='tstop'></form>");
	            ctx.println("<form method='POST' action='"+actionCtx.url()+"'>");
	            actionCtx.printUrlAsHiddenFields();
	            ctx.println("<input type=submit value='Kill Active Task' name='tkill'></form>");
            }
            ctx.println("<hr>");
            
            
           /* if(ctx.hasField("10s")) {
            	startupThread.addTask(new SurveyTask("Waste 10 Seconds")
            	{
            		public void run() throws Throwable {
            			Thread.sleep(10000);
            		}
            	});
            	ctx.println("10s task added.\n");
            } else */ if(ctx.hasField("tstop")) {
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
            ctx.print("<ul>");
            for(Thread t : s.keySet()) {
            	ctx.println("<li><a href='#"+t.getId()+"'>"+t.getName()+"</a>  - "+t.getState().toString()+"</li>");
            }
            ctx.println("</ul>");
           	for(Thread t : s.keySet()) {
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
            ctx.println("<table summary='User list' border=1><tr><th>age</th><th>user</th><th>what</th><th>action</th></tr>");
            for(Iterator li = CookieSession.getAll();li.hasNext();) {
                CookieSession cs = (CookieSession)li.next();
                ctx.println("<tr><!-- <td><tt style='font-size: 72%'>" + cs.id + "</tt></td> -->");
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
        } else if(action.equals("upd_1")) {
            WebContext subCtx = (WebContext)ctx.clone();
            actionCtx.addQuery("action",action);
            ctx.println("<h2>1-click vetting update</h2>");
            // 1: update all sources
            ctx.println("<h3>1. update all sources</h3>");
//            Connection conn = this.getDBConnection();
            try {
//                CLDRDBSource mySrc = makeDBSource(conn, null, CLDRLocale.ROOT);
                resetLocaleCaches();
                ctx.println("Update count: " + dbsrcfac.manageSourceUpdates(actionCtx, this, true)); // do a quiet 'update all'
                ctx.println("<hr>");
            } finally {
//                SurveyMain.closeDBConnection(conn);
            }
            // 2: load all locales
            loadAllLocales(ctx, null);
            // 3: update impl votes
            ctx.println("<br>");
            ElapsedTimer et = new ElapsedTimer();
            int n = vet.updateImpliedVotes();
            ctx.println("Done updating "+n+" implied votes in: " + et + "<br>");
            // 4: UpdateAll
            ctx.println("<h4>Update All</h4>");
            
            et = new ElapsedTimer();
            n = vet.updateResults(false); // don't RE update.
            ctx.println("Done updating "+n+" vote results in: " + et + "<br>");
            lcr.invalidateLocale(CLDRLocale.ROOT);
            // 5: update status
            et = new ElapsedTimer();
            n = vet.updateStatus();
            ctx.println("Done updating "+n+" statuses [locales] in: " + et + "<br>");
            ctx.println("<hr><h1>Data update done! Restart may be needed if 'root' or '"+this.BASELINE_ID+"' was involved.</h1>");
        } else if(action.equals("bulk_submit")) {
            WebContext subCtx = (WebContext)ctx.clone();
            actionCtx.addQuery("action",action);
            
            String aver="1.7"; // TODO: should be from 'new_version'
            ctx.println("<h2>Bulk Data Submission Updating for "+aver+"</h2><br/>\n");
           
            Set<UserRegistry.User> updUsers = new HashSet<UserRegistry.User>();
            
            String bulkStr = survprops.getProperty(CLDR_BULK_DIR);
            File bulkDir = null;
            if(bulkStr!=null&&bulkStr.length()>0) {
            	bulkDir = new File(bulkStr);
            }
            if(bulkDir==null||!bulkDir.exists()||!bulkDir.isDirectory()) {
            	ctx.println(ctx.iconHtml("stop","could not load bulk data")+"The bulk data dir "+CLDR_BULK_DIR+"="+bulkStr+" either doesn't exist or isn't set in cldr.properties. (Server requires reboot for this parameter to take effect)</i>");
            } else {
            	ctx.println("<h3>Bulk dir: "+bulkDir.getAbsolutePath()+"</h3>");
	            boolean doimpbulk = ctx.hasField("doimpbulk");
	            ctx.println("<form method='POST' action='"+actionCtx.url()+"'>");
	            actionCtx.printUrlAsHiddenFields();
	            ctx.println("<input type=submit value='Accept all implied votes' name='doimpbulk'></form>");
	            if(!doimpbulk) {
	            	ctx.print("<i>trial run. press the button to accept these votes.</i>");
	            }
	            
	            Set<File> files = new TreeSet<File>(Arrays.asList(getInFiles(bulkDir)));
	            
	            ctx.print("Jump to: ");
	            for(File file : files) {
	            	ctx.print("<a href=\"#"+getLocaleOf(file)+"\">"+file.getName()+"</a> ");
	            }
	            ctx.println("<br>");
	            
	            Set<CLDRLocale> toUpdate = new HashSet<CLDRLocale>();
            	int wouldhit=0;
            	

	            for(File file : files ) {
              	  synchronized(vet.conn) {
	            	CLDRLocale loc = getLocaleOf(file);
	            	ctx.println("<a name=\""+loc+"\"><h2>"+file.getName()+" - "+loc.getDisplayName(ctx.displayLocale)+"</h2></a>");
	            
	            	CLDRFile c = new CLDRFile(null, false);
	            	c.loadFromFile(file, loc.getBaseName(), CLDRFile.DraftStatus.unconfirmed);
	            	XPathParts xpp = new XPathParts(null,null);
	            	
	            	int countNoAlt = 0;
	            	int countNoUser = 0;
	            	int countBadUser = 0;
	            	int countAlready = 0;
	            	int countExplicit = 0;
	            	int countOther = 0;
	            	int countReady = 0;
	            	int countDone = 0;
	            	int countChange =0;
	            	int countVoteMain= 0;
	            	int countAdd = 0;
	            	
	            	for(String x : c) {
	            		String full = c.getFullXPath(x);
	            		String alt = XPathTable.getAlt(full, xpp);
	            		String val = c.getStringValue(x);
	            		if(alt==null||alt.length()==0) {
	            			if(!full.startsWith("//ldml/identity")) {
	            				countNoAlt = addAndWarnOnce(ctx, countNoAlt, "warn", "Xpath with no 'alt' tag: " + full);
	            			}
	            			continue;
	            		}
	            		String altPieces[] = LDMLUtilities.parseAlt(alt);
	            		if(altPieces[1]==null) {
            				countNoAlt = addAndWarnOnce(ctx, countNoAlt, "warn", "Xpath with no 'alt-proposed' tag: " + full);
	            			continue;
	            		}
	            		/*
	            		if(alt.equals("XXXX")) {
	            			alt = "proposed-u1-implicit1.7";
	            			x = XPathTable.removeAlt(x, xpp);
	            		}*/
	            		int n = XPathTable.altProposedToUserid(altPieces[1]);
	            		if(n<0) {
	            			countNoUser = addAndWarnOnce(ctx, countNoUser, "warn", "Xpath with no userid in 'alt' tag: " + full);
	            			continue;
	            		}
	            		User ui = null;
             		    if(n>=0) ui = reg.getInfo(n);
	            		if(ui==null) {
	            			countBadUser = addAndWarnOnce(ctx, countBadUser, "warn", "Bad userid '"+n+"': " + full);
	            			continue;
	            		}
	            		updUsers.add(ui);
	            		XMLSource stSource = dbsrcfac.getInstance(loc);
	            		String base_xpath = xpt.xpathToBaseXpath(x);
	            		int base_xpath_id = xpt.getByXpath(base_xpath);
	            		int vet_type[] = new int[1];
	            		int j = vet.queryVote(loc, n, base_xpath_id, vet_type);
	                    //int dpathId = xpt.getByXpath(xpathStr);
	            		// now, find the ID to vote for.
	            		Set<String> resultPaths = new HashSet<String>();
	            		stSource.getPathsWithValue(val, base_xpath, resultPaths);
	            		
	            		String resultPath = null;

	            		if(resultPaths.isEmpty()) {
	            			countAdd = addAndWarnOnce(ctx, countAdd, "zoom", "Value must be added under "+base_xpath+": " + val + "");
	            			if(!doimpbulk) {
	            				countReady = addAndWarnOnce(ctx, countReady, "okay","<i>Ready to update.</i>");
	            				continue; // don't try to add.
	            			}
	            			/* NOW THE FUN PART */
	            			if(altPieces[0]!=null) {
	            				throw new InternalError("alt besides proposed not supported, Go Bug Steven ");
	            			}
	            			
	            			for(int i=0;(resultPath==null)&&i<1000;i++) {
	            				String proposed = "proposed-u"+n+"-b"+i;
	            				String newAlt = LDMLUtilities.formatAlt(altPieces[0], proposed);
	            				String newxpath = base_xpath+"[@alt=\"" + newAlt + "\"]";
	            				String newoxpath = newxpath+"[@draft=\"unconfirmed\"]";

	            				if(stSource.hasValueAtDPath(newxpath)) continue;
	            				
	            				/* Write! */
	            				stSource.putValueAtPath(newoxpath, val);
	            				  toUpdate.add(loc);
	            				
	            				resultPath = newxpath;
	            			}
	            			
	            			
	            		} else if(resultPaths.size()>1) {
	            			/* ok, more than one result. stay cool.. */
	            			/* #1 - look for the base xpath. */
	            			for(String path : resultPaths) {
	            				if(path.equals(base_xpath)) {
	            					resultPath = path;
	            					countVoteMain = addAndWarnOnce(ctx, countVoteMain, "squo", "Using base xpath for " + base_xpath);
	            				}
	            			}
	            			/* #2 look for something with a vote */
	            			if(resultPath==null) {
	            				String winPath = stSource.getWinningPath(base_xpath);
	            				String winDpath = CLDRFile.getDistinguishingXPath(winPath, null, true);
	            				for(String path : resultPaths) {
		            				String aDPath = CLDRFile.getDistinguishingXPath(path, null, true);	
		            				if(aDPath.equals(winDpath)) {
		          if(false)  					ctx.println("Using winning dpath " + aDPath +"<br>");
	            						resultPath = aDPath;
		            				}
	            				}
	            			}
	            			/* #3 just take the first one */
	            			if(resultPath==null) {
		            			resultPath = resultPaths.toArray(new String[0])[0];
		            			//ctx.println("Using [0] path " + resultPath);
	            			}
	            			if(resultPath==null) {
	            				ctx.println(ctx.iconHtml("stop", "more than one result!")+"More than one result!<br>");
	            				if(true) ctx.println(loc+" " + xpt.getPrettyPath(base_xpath) + " / "+ alt + " (#"+n+" - " + ui +")<br/>");
	            				continue;
	            			}
	            		} else{
	            			resultPath = resultPaths.toArray(new String[0])[0];
	            		}

	            		/*temp*/if(resultPath == null) {
	            			continue; 
	            		}
	            		
	                    String xpathStr = CLDRFile.getDistinguishingXPath(resultPath, null, false);
	            		int dpathId = xpt.getByXpath(xpathStr);
	            		if(false) ctx.println(loc+" " + xpt.getPrettyPath(base_xpath) + " / "+ alt + " (#"+n+" - " + ui +"/" + dpathId+" <br/>");

	            		
	            		
	            		
	                     if(dpathId == j) {
	                    	countAlready = addAndWarnOnce(ctx, countAlready, "squo", "Vote already correct");
//	                    	ctx.println(" "+ctx.iconHtml("squo","current")+" ( == current vote ) <br>");
//	                    	  already++;
	                      } else {
	                          if(j>-1) {
	                        	  if(vet_type[0]==Vetting.VET_IMPLIED) {
	                        		  countOther = addAndWarnOnce(ctx, countOther, "okay", "Changing existing implied vote");
	                        	  } else {
	                        		  countExplicit = addAndWarnOnce(ctx, countExplicit, "warn", "NOT changing existing different vote");
	                        		  continue;
	                        	  }
//	            			  ctx.println(" "+ctx.iconHtml("warn","already")+"Current vote: "+j+"<br>");
//	            			  different++;
	                          }
	            			  if(doimpbulk) {
	            				  vet.vote(loc, base_xpath_id, n, dpathId, Vetting.VET_IMPLIED);
	            				  toUpdate.add(loc);
	            				  countReady = addAndWarnOnce(ctx, countReady, "okay","<i>Updating.</i>");
	            			  } else {
	            				  countReady = addAndWarnOnce(ctx, countReady, "okay","<i>Ready to update.</i>");
	            				/*  wouldhit++;
	            				  toUpdate.add(loc);*/
	            			  }
	            		  }
	            		  
	            	  } /* end xpath */
	            	  ctx.println("<hr>");
	            	  /*if(already>0 ) {
	            		  ctx.println(" "+ctx.iconHtml("squo","current")+""+already+" items already had the correct vote.<br>");
	            	  }
	            	  if(different>0) {
	            		  ctx.println(" "+ctx.iconHtml("warn","different")+" " + different + " items had a different vote already cast.<br>");
	            	  }
	            	  if(doimpbulk && !toUpdate.isEmpty()) {
	            		  ctx.println("<h3>"+wouldhit+" Locale Updates in " + toUpdate.size() + " locales ..</h3>");
	            		  for(CLDRLocale l : toUpdate) {
	        				  vet.deleteCachedLocaleData(l);
	            			  dbsrcfac.needUpdate(l);
	            			  ctx.print(l+"...");
	            		  }
	            		  ctx.println("<br>");
	            		  int upd = dbsrcfac.update();
	            		  ctx.println(" Updated. "+upd + " deferred updates done.<br>");
	            	  } else if(wouldhit>0) {
	            		  ctx.println("<h3>Ready to update "+wouldhit+" Locale Updates in " + toUpdate.size() + " locales ..</h3>");
	                      ctx.println("<form method='POST' action='"+actionCtx.url()+"'>");
	                      actionCtx.printUrlAsHiddenFields();
	                      ctx.println("<input type=submit value='Accept all implied votes' name='doimpbulk'></form>");
	            	  }*/
	            	  for(CLDRLocale toul : toUpdate) {
	            		  this.updateLocale(toul);
	            	  }
	            	  toUpdate.clear();
              	    } /* end sync */
	              } /* end outer loop */
	            ctx.println("<hr>");
	            ctx.println("<h3>Users involved:</h3>");
	            for(User auser : updUsers) {
	            	ctx.println(auser.toString());
	            	ctx.println("<br>");
	            }
            }
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
		///	vet.doDisputeNag("asdfjkl;", null);
		///	ctx.println("\u3058\u3083\u3001\u3057\u3064\u308c\u3044\u3057\u307e\u3059\u3002<br/>"); // ??
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
            
            boolean reupdate = actionCtx.hasField("reupdate");

            if(what.equals("ALL")) {
                ctx.println("<h4>Update All (delete first: "+reupdate+")</h4>");
                
                ElapsedTimer et = new ElapsedTimer();
                int n = vet.updateResults(reupdate);
                ctx.println("Done updating "+n+" vote results in: " + et + "<br>");
                lcr.invalidateLocale(CLDRLocale.ROOT);
                ElapsedTimer zet = new ElapsedTimer();
                int zn = vet.updateStatus();
                ctx.println("Done updating "+zn+" statuses [locales] in: " + zet + "<br>");
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
                	for(CLDRLocale loc : locs) {
                		ctx.println("<h2>"+loc+"</h2>");
	                    if(reupdate) {
	                        try {
	                            synchronized(vet.conn) {
	                                vet.rmResultLoc.setString(1,loc.toString());
	                                int del = sqlUpdate(ctx, vet.conn, vet.rmResultLoc);
	                                ctx.println("<em>"+del+" results of "+loc+" locale removed</em><br>");
	                                System.err.println("update: "+del+" results of "+loc+" locale removed");
	                            }
	                        } catch(SQLException se) {
	                            se.printStackTrace();
	                            ctx.println("<b>Err while trying to delete results for " + loc + ":</b> <pre>" + unchainSqlException(se)+"</pre>");
	                        }
	                    }
	                    
	                    ctx.println("<h4>Update just "+loc+"</h4>");
	                    ElapsedTimer et = new ElapsedTimer();
	                    int n = vet.updateResults(loc);
	                    ctx.println("Done updating "+n+" vote results for " + loc + " in: " + et + "<br>");
	                    lcr.invalidateLocale(loc);
                	}
                    ElapsedTimer zet = new ElapsedTimer();
                    int zn = vet.updateStatus();
                    ctx.println("Done updating "+zn+" statuses ["+locs.size()+" locales] in: " + zet + "<br>");
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
			
			
        } else if(action.equals("srl_vet_wash")) {
            WebContext subCtx = (WebContext)ctx.clone();
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
                    int n = vet.washVotes(CLDRLocale.getInstance(what));
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
            WebContext subCtx = (WebContext)ctx.clone();
            actionCtx.addQuery("action",action);
            ctx.println("<br>(locale caches reset..)<br>");
//            Connection conn = this.getDBConnection();
            try {
//                CLDRDBSource mySrc = makeDBSource(conn, null, CLDRLocale.ROOT);
                resetLocaleCaches();
                dbsrcfac.manageSourceUpdates(actionCtx, this); // What does this button do?
                ctx.println("<br>");
            } finally {
//                SurveyMain.closeDBConnection(conn);
            }
            
        } else if(action.equals("srl_output")) {
            WebContext subCtx = (WebContext)ctx.clone();
            subCtx.addQuery("dump",vap);
            subCtx.addQuery("action",action);
            
            String output = actionCtx.field("output");
            
            ctx.println("<br>");
            ctx.print("<b>Output:</b> ");
            printMenu(subCtx, output, "xml", "XML", "output");
            subCtx.print(" | ");
            printMenu(subCtx, output, "vxml", "VXML", "output");
            subCtx.print(" | ");
            printMenu(subCtx, output, "rxml", "RXML", "output");
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
            boolean daily = false;
            if(output.equals("daily")) {
                daily = true;
            }
            
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
            
        } else if(action.equals("srl_db_update")) {
            WebContext subCtx = (WebContext)ctx.clone();
            subCtx.addQuery("dump",vap);
            subCtx.addQuery("action","srl_db_update");
            ctx.println("<br>");
//            XMLSource mySrc = makeDBSource(ctx, null, CLDRLocale.ROOT);
            ElapsedTimer aTimer = new ElapsedTimer();
            dbsrcfac.doDbUpdate(subCtx, this); 
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
				XMLSource dbSource = makeDBSource(ctx, null, CLDRLocale.getInstance(theLocale), true);
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
		} else if(action.equals("add_locale")) {
			actionCtx.addQuery("action", action);
			ctx.println("<hr><br><br>");
			String loc = actionCtx.field("loc");

			ctx.println("This interface lets you create a new locale, and its parents.  Before continuing, please make sure you have done a" +
					" CVS update to make sure the file doesn't already exist." +
					" After creating the locale, it should be added to CVS as well.<hr>");
			
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
							CLDRFile emptyFile = CLDRFile.make(aloc.getBaseName());
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
		                    	System.err.println("While adding "+xmlFile.getAbsolutePath());
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
	                    System.err.println("Done updating "+n+" statuses [locales] in: " + et + "<br>");
	                }
            	});
            	ctx.println("... load all locales task queued<br>");
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
                Connection conn = getDBConnection();
                Statement s = conn.createStatement();
                ResultSet rs = s.executeQuery(sql);
                while(rs.next()) {
                    paths.add(rs.getInt(1));
                }
                rs.close();
                s.close();
            } catch ( SQLException se ) {
                String complaint = " Couldn't query xpaths of " + test0 +" - " + SurveyMain.unchainSqlException(se) + " - " + sql;
                System.err.println(complaint);
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
                
        printFooter(ctx);
    }
    
    /* print a warning the first time. */
    private int addAndWarnOnce(WebContext ctx, int count, String icon,
			String desc) {
    	if(count==0) {
    		 ctx.println(ctx.iconHtml(icon, "counter")+" "+desc+" (this message will only print once)<br>");
    	}
    	return ++count;
	}
	private static void throwIfBadLocale(String test0) {
        if(!SurveyMain.getLocalesSet().contains(test0)) {
            throw new InternalError("Bad locale: "+test0);
        }
    }
    private void loadAllLocales(WebContext ctx, SurveyTask surveyTask) {
        File[] inFiles = getInFiles();
        int nrInFiles = inFiles.length;
        com.ibm.icu.dev.test.util.ElapsedTimer allTime = new com.ibm.icu.dev.test.util.ElapsedTimer("Time to load all: {0}");
        logger.info("Loading all..");            
//        Connection connx = null;
        int ti = 0;
        for(int i=0;(null==this.isBusted)&&i<nrInFiles&&(surveyTask==null||surveyTask.running());i++) {
            String localeName = inFiles[i].getName();
            int dot = localeName.indexOf('.');
            if(dot !=  -1) {
                localeName = localeName.substring(0,dot);
                if((i>0)&&((i%50)==0)) {
                    logger.info("   "+ i + "/" + nrInFiles + ". " + usedK() + "K mem used");
                   if(ctx!=null) ctx.println("   "+ i + "/" + nrInFiles + ". " + usedK() + "K mem used<br>");
                }
                try {
//                    if(connx == null) {
//                        connx = this.getDBConnection();
//                    }
        
                    CLDRLocale locale = CLDRLocale.getInstance(localeName);
                    //                        WebContext xctx = new WebContext(false);
                    //                        xctx.setLocale(locale);
                    //makeCLDRFile(makeDBSource(connx, null, locale));  // orphan result
                    dbsrcfac.getInstance(locale);
                } catch(Throwable t) {
                    t.printStackTrace();
                    String complaint = ("Error loading: " + localeName + " - " + t.toString() + " ...");
                    logger.severe("loading all: " + complaint);
                    if(ctx!=null) {
                    		ctx.println(complaint + "<br>" + "<pre>");
                    		ctx.print(t);
                    		ctx.println("</pre>");
                    }
//                    try {
//                    	closeDBConnection(connx);
//                    } catch(Throwable t2) {
//                        t2.printStackTrace();
//                        String complaint2 = ("Error closing down connection: " + localeName + " - " + t2.toString() + " ...");
//                        logger.severe("closing conn: " + complaint2);
//                        ctx.println(complaint2 + "<br>" + "<pre>");
//                        ctx.print(t2);
//                        ctx.println("</pre>");
//                    }
//                    connx=null;
                }
            }
        }
        if(surveyTask!=null&&!surveyTask.running()) {
        	System.err.println("LoadAll: no longer running!\n");
        }
//        closeDBConnection(connx);
        logger.info("Loaded all. " + allTime);
        if(ctx!=null) ctx.println("Loaded all." + allTime + "<br>");
        int n = dbsrcfac.update();
        logger.info("Updated "+n+". " + allTime);
        if(ctx!=null) ctx.println("Updated "+n+"." + allTime + "<br>");
    }
    
    /* 
     * print menu of stuff to 'work with' a live user session..
     */
    private void printLiveUserMenu(WebContext ctx, CookieSession cs) {
        ctx.println("<a href='" + ctx.base() + "?dump=" + vap + "&amp;see=" + cs.id + "'>" + ctx.iconHtml("zoom","SEE this user") + "see" + "</a> |");
        ctx.println("<a href='" + ctx.base() + "?&amp;s=" + cs.id + "'>"  +"be" + "</a> |");
        ctx.println("<a href='" + ctx.base() + "?dump=" + vap + "&amp;unlink=" + cs.id + "'>" +  "kick" + "</a>");
    }
    
//    static boolean showedComplaint = false;
     
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
        ctx.println("<META NAME=\"ROBOTS\" CONTENT=\"NOINDEX,NOFOLLOW\"> "); // NO index
        ctx.println("<meta name='robots' content='noindex,nofollow'>");
        ctx.println("<meta name=\"gigabot\" content=\"noindex\">");
        ctx.println("<meta name=\"gigabot\" content=\"noarchive\">");
        ctx.println("<meta name=\"gigabot\" content=\"nofollow\">");

        ctx.println("<link rel='stylesheet' type='text/css' href='"+ ctx.schemeHostPort()  + ctx.context("surveytool.css") + "'>");
        ctx.println("<title>CLDR Vetting | ");
        if(ctx.getLocale() != null) {
            ctx.print(ctx.getLocale().getDisplayName(ctx.displayLocale) + " | ");
        }
        ctx.println(title + "</title>");
        ctx.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
        
        if(true || isUnofficial || 
                ctx.prefBool(PREF_GROTTY)) {  // no RSS on the official site- for now
            if(ctx.getLocale() != null) {
                ctx.println(fora.forumFeedStuff(ctx));
            } else {
                if(!ctx.hasField("x")&&!ctx.hasField("do")&&!ctx.hasField("sql")&&!ctx.hasField("dump")) {
                    ctx.println(fora.mainFeedStuff(ctx));
                }
            }
        }
        
        ctx.println("</head>");
        ctx.println("<body onload='this.focus(); top.focus(); ContextWindow.focus(); top.parent.focus(); '>");
        ctx.print("<div class='topnotices'>");
        if(/*!isUnofficial && */ 
            ((ctx.session!=null && ctx.session.user!=null && UserRegistry.userIsAdmin(ctx.session.user))||
                false)) {
            ctx.print("<p class='admin' title='You're an admin!'>");
            ctx.printHelpLink("/Admin",ctx.iconHtml("stop","Admin!")+"Be careful, you are an administrator!");
            ctx.println("</p>");
        }
        if(isUnofficial) {
            ctx.print("<p class='unofficial' title='Not an official SurveyTool' >");
            ctx.printHelpLink("/Unofficial",ctx.iconHtml("warn","Unofficial Site")+"Unofficial");
            ctx.println("</p>");
        }
        if(isPhaseBeta()) {
            ctx.print("<p class='beta' title='Survey Tool is in Beta' >");
            ctx.printHelpLink("/Beta",ctx.iconHtml("warn","beta")+"SurveyTool is in Beta. Any data added here will NOT go into CLDR.");
            ctx.println("</p>");
        }
        ctx.print("</div>");
        showSpecialHeader(ctx);
        ctx.println(SHOWHIDE_SCRIPT);
        
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
            String threadInfo = startupThread.htmlStatus();
            if(threadInfo!=null) {
            	out.println("<b>Processing:"+threadInfo+"</b><br>");
            }
            if(ctx != null && progressWhat != null) {
                showProgress(ctx);
            }
            out.println("</div><br>");
        } else {
            String threadInfo = startupThread.htmlStatus();
            if(threadInfo!=null) {
            	out.println("<b>Processing:"+threadInfo+"</b><br>");
            }
            if(progressWhat != null) {
                out.print("<div class='specialHeader'>SurveyTool may be busy: <br>");
                out.println(getProgress());
                out.print("</div><br>");
            }
        }
    }
    
    public static final int PROGRESS_WID=100; /** Progress bar width **/
        
    
    /**
     * Done with progress, reset to nothing.
     */
    public void clearProgress() {
        progressWhat = null;
        progressMax = 0;
        progressCount = 0;
        progressSub = null;
    }
    
    /**
     * Initialize a progress that will not show the actual count, but treat the count as a number from 0-100
     * @param what the user-visible string
     */
    public void setProgress(String what) {
        setProgress(what, -100);
    }
    
    /**
     * Initialize a Progress. 
     * @param what what is progressing
     * @param max the max count, or <0 if it is an un-numbered percentage (0-100 without specific numbers)
     */
    public void setProgress(String what, int max) {
            progressWhat = what;
            progressMax = max;
            progressCount = 0;
            progressSub = null;
    }
    
    /**
     * Update on the progress
     * @param count current count - up to Max
     */
    public void updateProgress(int count) {
        progressCount = count;
    }
    
    public void updateProgress(int count, String what) {
        progressCount = count;
        progressSub = what;
    }
    
    /**
     * Print out the progress
     * @param ctx
     */
    public void showProgress(WebContext ctx) {
        ctx.print(getProgress());
    }
    
    public String getProgress() {
        StringBuffer buf = new StringBuffer();
                
        double max = progressMax;
        if(max < 0) { // negative: assume it is a percentage
            max = 100.0;
        }
        if(max<=0) return "";

        buf.append("<b>"+progressWhat+"</b> in progress:");

        double cur = progressCount;
        if(cur>max) {
            cur = max;
        }
        if(cur<0) {
            cur = 0;
        }
        double pct = (cur/max);
        
        double barWid = (cur/max)*(double)PROGRESS_WID;
        
        int barX = (int)barWid;
        int remainX = PROGRESS_WID-barX;
        
        buf.append("<table border=0 style='margin: 5px; padding: 0; border-collapse: collapse; border: 2px solid black;'><tr height='12'>");
        if(barX > 0) {
            buf.append("<td width='"+barX+"' style='padding: 0; margin: 0; border-left: 2px solid black; border-right: none; background-color: #6c31fe;'>");
            buf.append("</td>");
        }
        if(remainX >0) {
            buf.append("<td width='"+remainX+"' style='padding: 0; margin: 0; border-left: none; border-right: 2px solid black; background-color: #ddd;'>");
            buf.append("</td>");
        }
        buf.append("</table>");
        NumberFormat pctFmt=  NumberFormat.getPercentInstance();
        if(progressMax>=0) { //  only show the actual number if >0
            buf.append(progressCount+" of "+progressMax+" &mdash; ");
        }
        buf.append( pctFmt.format(pct));
        if(progressSub != null) {
            buf.append(" &mdash; "+progressSub);
        }
        return buf.toString();
    }
    
    public void printFooter(WebContext ctx)
    {
        ctx.println("<hr>");
        ctx.print("<div style='float: right; font-size: 60%;'>");
        ctx.print("<span style='color: #ddd'> "+SURVEYMAIN_REVISION+" \u00b7 </span>");
        ctx.print("<span class='notselected'>validate <a href='http://jigsaw.w3.org/css-validator/check/referer'>css</a>, "+
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
        if(ctx.request != null) try {
            Map m = new TreeMap(ctx.getParameterMap());
            m.remove("sql");
            m.remove("pw");
            m.remove(QUERY_PASSWORD_ALT);
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
            ctx.println("| <a href='" + bugFeedbackUrl("Feedback on URL ?" + u)+"'>Report Problem in Tool</a>");
        } catch (Throwable t) {
            System.err.println(t.toString());
            t.printStackTrace();
        }
        if(!SurveyMain.isUnofficial) {
        	ctx.println(ShowData.ANALYTICS);
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
            ctx.setLocale(CLDRLocale.getInstance(locale));
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
//        /*srl*/ System.err.println("isBusted: " + isBusted + ", reg: " + reg);
        user = reg.get(password,email,ctx.userIP());

        if(ctx.request == null && ctx.session != null) {
            return "using canned session"; // already set - for testing
        }

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
//            ctx.printHelpLink("","General&nbsp;Instructions"); // base help
            ctx.println("(<a href='"+GENERAL_HELP_URL+"'>"+GENERAL_HELP_NAME+"</a>)"); // base help
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
        
        ctx.println("The SurveyTool is in phase <b><span title='"+phase().name()+"'>"+phase().toString()+"</span></b> for version <b>"+getNewVersion()+"</b><br>" );
        
        if(ctx.session.user == null)  {
            ctx.println("You are a <b>Visitor</b>. <a class='notselected' href='" + ctx.jspLink("login.jsp") +"'>Login</a><br>");
            
//            if(this.phase()==Phase.VETTING || this.phase() == Phase.SUBMIT) {
//                printMenu(ctx, doWhat, "disputed", "Disputed", QUERY_DO);
//                ctx.print(" | ");
//            }
            printMenu(ctx, doWhat, "options", "My Options", QUERY_DO);
            ctx.println("<br>");
        } else {
            ctx.println("<b>Welcome " + ctx.session.user.name + " (" + ctx.session.user.org + ") !</b>");
            ctx.println("<a class='notselected' href='" + ctx.base() + "?do=logout'>Logout</a><br>");
//            if(this.phase()==Phase.VETTING || this.phase() == Phase.SUBMIT || isPhaseVettingClosed()) {
//                printMenu(ctx, doWhat, "disputed", "Disputed", QUERY_DO);
//                ctx.print(" | ");
//            }
            printMenu(ctx, doWhat, "options", "My Options", QUERY_DO);
            ctx.print(" | ");
            printMenu(ctx, doWhat, "listu", "My Account", QUERY_DO);
            //ctx.println(" | <a class='deactivated' _href='"+ctx.url()+ctx.urlConnector()+"do=mylocs"+"'>My locales</a>");
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
                ctx.print(" | ");
//              if(this.phase()==Phase.VETTING || this.phase() == Phase.SUBMIT) {
                printMenu(ctx, doWhat, "disputed", "Disputed (Approximate)", QUERY_DO);
//              ctx.print(" | ");
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
            if(SurveyMain.isPhaseReadonly()) {
				ctx.println("(The SurveyTool is in a read-only state, no changes may be made.)");
			} else if(SurveyMain.isPhaseVetting() 
                && UserRegistry.userIsStreet(ctx.session.user)
                && !UserRegistry.userIsExpert(ctx.session.user)) {
                ctx.println(" (Note: in the Vetting phase, you may not submit new data.) ");
            } else if(SurveyMain.isPhaseClosed() && !UserRegistry.userIsTC(ctx.session.user)) {
                ctx.println("(SurveyTool is closed to vetting and data submissions.)");
            }
            ctx.println("<br/>");
            if((ctx.session != null) && (ctx.session.user != null) && (SurveyMain.isPhaseVettingClosed() && ctx.session.user.userIsSpecialForCLDR15(null))) {
                ctx.println("<b class='selected'>"+ctx.session.user.name+", you have been granted extended privileges for the CLDR "+getNewVersion()+" vetting period.</b><br>");
            }
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
        
        boolean newOrgOk = false;
        try {
        	VoteResolver.Organization newOrgEnum = VoteResolver.Organization.fromString(new_org);
        	newOrgOk = true;
        } catch(IllegalArgumentException iae) {
        	newOrgOk = false;
        }
        
        if((new_name == null) || (new_name.length()<=0)) {
            ctx.println("<div class='sterrmsg'>"+ctx.iconHtml("stop","Could not add user")+"Please fill in a name.. hit the Back button and try again.</div>");
        } else if((new_email == null) ||
                  (new_email.length()<=0) ||
                  ((-1==new_email.indexOf('@'))||(-1==new_email.indexOf('.')))  ) {
            ctx.println("<div class='sterrmsg'>"+ctx.iconHtml("stop","Could not add user")+"Please fill in an <b>email</b>.. hit the Back button and try again.</div>");
        } else if(newOrgOk == false) {
        	ctx.println("<div class='sterrmsg'>"+ctx.iconHtml("stop","Could not add user")+"That Organization (<b>"+new_org+"</b>) is not valid. Either it is not spelled properly, or someone must update VoteResolver.Organization in VoteResolver.java</div>");
        } else if((new_org == null) || (new_org.length()<=0)) { // for ADMIN
            ctx.println("<div class='sterrmsg'>"+ctx.iconHtml("stop","Could not add user")+"Please fill in an <b>Organization</b>.. hit the Back button and try again.</div>");
        } else if(new_userlevel<0) {
            ctx.println("<div class='sterrmsg'>"+ctx.iconHtml("stop","Could not add user")+"Please fill in a <b>user level</b>.. hit the Back button and try again.</div>");
        } else if(new_userlevel==UserRegistry.EXPERT && ctx.session.user.userlevel!=UserRegistry.ADMIN) {
            ctx.println("<div class='sterrmsg'>"+ctx.iconHtml("stop","Could not add user")+"Only Admin can create EXPERT users.. hit the Back button and try again.</div>");
        } else {
            UserRegistry.User u = reg.getEmptyUser();
            
            u.name = new_name;
            u.userlevel = UserRegistry.userCanCreateUserOfLevel(ctx.session.user, new_userlevel);
            u.email = new_email;
            u.org = new_org;
            u.locales = new_locales;
            u.password = UserRegistry.makePassword(u.email+u.org+ctx.session.user.email);
            
            UserRegistry.User registeredUser = registeredUser = reg.newUser(ctx, u);
            
            if(registeredUser == null) {
                if(reg.get(new_email) != null) { // already exists..
                    ctx.println("<div class='sterrmsg'>"+ctx.iconHtml("stop","Could not add user")+"A user with that email, <tt>" + new_email + "</tt> already exists. Couldn't add user. </div>");                
                } else {
                    ctx.println("<div class='sterrmsg'>"+ctx.iconHtml("stop","Could not add user")+"Couldn't add user <tt>" + new_email + "</tt> - an unknown error occured.</div>");
                }
            } else {
                ctx.println("<i>"+ctx.iconHtml("okay","added")+"user added.</i>");
                registeredUser.printPasswordLink(ctx);
                WebContext nuCtx = (WebContext)ctx.clone();
                nuCtx.addQuery(QUERY_DO,"list");
                nuCtx.addQuery(LIST_JUST, changeAtTo40(new_email));
                ctx.println("<p>"+ctx.iconHtml("warn","Note..")+"The password is not sent to the user automatically. You can do so in the '<b><a href='"+nuCtx.url()+"#u_"+u.email+"'>"+ctx.iconHtml("zoom","Zoom in on user")+"manage "+new_name+"</a></b>' page.</p>");
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

    private static int sqlUpdate(WebContext ctx, Connection conn, PreparedStatement ps) {
        int rv = -1;
        try {
            rv = ps.executeUpdate();
        } catch ( SQLException se ) {
            String complaint = " Couldn't sqlUpdate  - " + SurveyMain.unchainSqlException(se) + " -  ps";
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

        if(!UserRegistry.userIsVetter(ctx.session.user)) {
            ctx.print("Not authorized.");
        }
        
        
        printUserTableWithHelp(ctx, "/LocaleCoverage");

        ctx.println("<a href='" + ctx.jspLink("adduser.jsp") 
                + "&amp;defaultorg="+URLEncoder.encode(ctx.session.user.org)
                +"'>[Add User]</a> |");
//        ctx.println("<a href='" + ctx.url()+ctx.urlConnector()+"do=list'>[List Users]</a>");
        ctx.print("<br>");
        ctx.println("<a href='" + ctx.url() + "'><b>SurveyTool main</b></a><hr>");
        String org = ctx.session.user.org;
        if(UserRegistry.userCreateOtherOrgs(ctx.session.user)) {
            org = null; // all
        }
        
        StandardCodes sc = StandardCodes.make();
        
        LocaleTree tree = getLocaleTree();

        WebContext subCtx = (WebContext)ctx.clone();
        subCtx.setQuery(QUERY_DO,"coverage");
        boolean participation = showTogglePref(subCtx, "cov_participation", "Participation Shown (click to toggle)");
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
        //String localeList[] = new String[nrInFiles];
        Set<CLDRLocale> allLocs = this.getLocalesSet();
        /*
        for(int i=0;i<nrInFiles;i++) {
            String localeName = inFiles[i].getName();
            int dot = localeName.indexOf('.');
            if(dot !=  -1) {
                localeName = localeName.substring(0,dot);
            }
            localeList[i]=localeName;
			allLocs.add(localeName);
        }
*/
        int totalUsers = 0;
        int allUsers = 0; // users with all
        
        int totalSubmit=0;
        int totalVet=0;
        
        Map<CLDRLocale,Set<CLDRLocale>> intGroups = getIntGroups();
		
        Connection conn = null;
        Map<String, String> userMap = null;
        Map<String, String> nullMap = null;
        Hashtable<CLDRLocale,Hashtable<Integer,String>> localeStatus = null;
        Hashtable<CLDRLocale,Hashtable<Integer,String>> nullStatus = null;
        
        {
            conn = getDBConnection();
            userMap = new TreeMap<String, String>();
            nullMap = new TreeMap<String, String>();
            localeStatus = new Hashtable<CLDRLocale,Hashtable<Integer,String>>();
            nullStatus = new Hashtable<CLDRLocale,Hashtable<Integer,String>>();
        }
        
        Set<CLDRLocale> s = new TreeSet<CLDRLocale>();
        Set<CLDRLocale> badSet = new TreeSet<CLDRLocale>();
        try {
			PreparedStatement psMySubmit = conn.prepareStatement("select COUNT(id) from cldr_data where submitter=?",ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
			PreparedStatement psMyVet = conn.prepareStatement("select COUNT(id) from cldr_vet where submitter=?",ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
			PreparedStatement psnSubmit = conn.prepareStatement("select COUNT(id) from cldr_data where submitter=? and locale=?",ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
			PreparedStatement psnVet = conn.prepareStatement("select COUNT(id) from cldr_vet where submitter=? and locale=?",ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);

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
                String theirName = SurveyMain.getStringUTF8(rs, 3);//rs.getString(3);
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
					Set<CLDRLocale> theirSet = new HashSet<CLDRLocale>(); // set of locales this vetter has access to
					for(int j=0;j<theirLocales.length;j++) { 
						Set<CLDRLocale> subSet = intGroups.get(CLDRLocale.getInstance(theirLocales[j])); // Is it an interest group? (de, fr, ..)
						if(subSet!=null) {
							theirSet.addAll(subSet); // add all sublocs
						} else if(allLocs.contains(theirLocales[j])) {
							theirSet.add(CLDRLocale.getInstance(theirLocales[j]));
						} else {
							badSet.add(CLDRLocale.getInstance(theirLocales[j]));
						}
					}
					for(CLDRLocale theLocale : theirSet) {
						s.add(theLocale);
                          //      hitList[j]++;
                               // ctx.println("user " + theirEmail + " with " + theirLocales[j] + " covers " + theLocale + "<br>");
                       Hashtable<CLDRLocale,Hashtable<Integer,String>> theHash = localeStatus; // to the 'status' field
                       String userInfo = nameLink+" ";
					   if(participation && conn != null) {
							psnSubmit.setString(2,theLocale.getBaseName());
							psnVet.setString(2,theLocale.getBaseName());
							
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
                        Hashtable<Integer,String> oldStr = theHash.get(theLocale);
                        
                        
                        if(oldStr==null) {
                            oldStr = new Hashtable<Integer, String>();
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
            logger.log(java.util.logging.Level.WARNING,"Query for org " + org + " failed: " + unchainSqlException(se),se);
            ctx.println("<i>Failure: " + unchainSqlException(se) + "</i><br>");
        }

//        Map<String, CLDRLocale> lm = getLocaleListMap();
//        if(lm == null) {
//            busted("Can't load CLDR data files from " + fileBase);
//            throw new RuntimeException("Can't load CLDR data files from " + fileBase);
//        }
        
        if(!badSet.isEmpty()) {
            ctx.println("<B>Locales not in CLDR but assigned to vetters:</b> <br>");
            int n=0;
            boolean first=true;
            Set<CLDRLocale> subSet = new TreeSet<CLDRLocale>();
            for(CLDRLocale li : badSet) {
            	if(li.toString().indexOf('_')>=0) {
            		n++;
            		subSet.add(li);
           			continue;
            	}
                if(first==false) {
                    ctx.print(" ");
                } else {
                    first = false;
                }
                ctx.print("<tt style='border: 1px solid gray; margin: 1px; padding: 1px;' class='codebox'>"+li.toString()+"</tt>" );
            }
            ctx.println("<br>");
            if(n>0) {
            	ctx.println("Note: "+n+" locale(s) were specified that were sublocales. This is no longer supported, specify the top level locale (en, not en_US or en_Shaw): <br><font size=-1>");
            	if(isUnofficial) for(CLDRLocale li:subSet) {
                    ctx.print(" <tt style='border: 1px solid gray; margin: 1px; padding: 1px;' class='codebox'><font size=-1>"+li.toString()+"</font></tt>" );
            	}
            	ctx.println("</font><br>");
            }
        }
        
        // Now, calculate coverage of requested locales for this organization
        //sc.getGroup(locale,missingLocalesForOrg);
        Set<CLDRLocale> languagesNotInCLDR = new TreeSet<CLDRLocale>();
        Set<CLDRLocale> languagesMissing = new HashSet<CLDRLocale>();
        Set<CLDRLocale> allLanguages = new TreeSet<CLDRLocale>();
        {
        	for(String code : sc.getAvailableCodes("language")) {
        		allLanguages.add(CLDRLocale.getInstance(code));
        	}
        }
        for(Iterator<CLDRLocale> li = allLanguages.iterator();li.hasNext();) {
        	CLDRLocale lang = (CLDRLocale)(li.next());
            String group = sc.getGroup(lang.getBaseName(), missingLocalesForOrg);
            if((group != null) &&
                (!"basic".equals(group)) && // exclude it for being basic
                (null==supplemental.defaultContentToParent(group)) ) {
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
            ctx.println("<p class='hang'>"+ctx.iconHtml("stop","locales missing from CLDR")+"<B>Required by " + missingLocalesForOrg + " but not in CLDR: </b>");
            boolean first=true;
            for(Iterator<CLDRLocale> li = languagesNotInCLDR.iterator();li.hasNext();) {
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
            ctx.println("<p class='hang'>"+ctx.iconHtml("stop","locales without vetters")+
                "<B>Required by " + missingLocalesForOrg + " but no vetters: </b>");
            boolean first=true;
            for(Iterator<CLDRLocale> li = languagesMissing.iterator();li.hasNext();) {
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
        for(String ln:tree.getTopLocales()) {
            n++;
            CLDRLocale aLocale = tree.getLocaleCode(ln);
            ctx.print("<tr class='row" + (n%2) + "'>");
            ctx.print(" <td valign='top'>");
            boolean has = (s.contains(aLocale));
            if(has) {
                ctx.print("<span class='selected'>");
            } else {
                ctx.print("<span class='disabledbox' style='color:#888'>");
            }
//            ctx.print(aLocale);            
            //ctx.print("<br><font size='-1'>"+new ULocale(aLocale).getDisplayName()+"</font>");
			printLocaleStatus(ctx, aLocale, ln.toString(), aLocale.toString());
            ctx.print("</span>");
            if(languagesMissing.contains(aLocale)) {
                ctx.println("<br>"+ctx.iconHtml("stop","No " + missingLocalesForOrg + " vetters")+ "<i>(coverage: "+ sc.getGroup(aLocale.toString(), missingLocalesForOrg)+")</i>");
            }

            if(showCodes) {
                ctx.println("<br><tt>" + aLocale + "</tt>");
            }
			if(localeStatus!=null && !localeStatus.isEmpty()) {
				Hashtable<Integer,String> what = localeStatus.get(aLocale);
				if(what!=null) {
					ctx.println("<ul>");
					for(Iterator<String> i = what.values().iterator();i.hasNext();) {
						ctx.println("<li>"+i.next()+"</li>");
					}
					ctx.println("</ul>");
				}
			}
            boolean localeIsDefaultContent = (null!=supplemental.defaultContentToParent(aLocale.toString()));
			if(localeIsDefaultContent) {
                        ctx.println(" (<i>default content</i>)");
            } else if(participation && nullStatus!=null && !nullStatus.isEmpty()) {
				Hashtable<Integer, String> what = nullStatus.get(aLocale);				
				if(what!=null) {
					ctx.println("<br><blockquote> <b>Did not participate:</b> ");
					for(Iterator<String> i = what.values().iterator();i.hasNext();) {
						ctx.println(i.next().toString()	);
						if(i.hasNext()) {
							ctx.println(", ");
						}
					}
					ctx.println("</blockquote>");
				}
			}
            ctx.println(" </td>");
            
            Map<String,CLDRLocale> sm = tree.getSubLocales(aLocale);  // sub locales 
            
            ctx.println("<td valign='top'>");
            int j = 0;
            for(Iterator<String> si = sm.keySet().iterator();si.hasNext();) {
                String sn = si.next().toString();
                CLDRLocale subLocale = sm.get(sn);
               // if(subLocale.length()>0) {

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
    
					printLocaleStatus(ctx, CLDRLocale.getInstance(subLocale.toString()), sn, subLocale.toString());
                   // if(has) {
                        ctx.print("</span>");
                   // }


//                    printLocaleLink(baseContext, subLocale, sn);
                    if(showCodes) {
                        ctx.println("&nbsp;-&nbsp;<tt>" + subLocale + "</tt>");
                    }
                    boolean isDc = (null!=supplemental.defaultContentToParent(subLocale.toString()));
                    
					if(localeStatus!=null&&!nullStatus.isEmpty()) {
						Hashtable<Integer, String> what = localeStatus.get(subLocale);
						if(what!=null) {
							ctx.println("<ul>");
							for(Iterator<String> i = what.values().iterator();i.hasNext();) {
								ctx.println("<li>"+i.next()+"</li>");
							}
							ctx.println("</ul>");
						}
					}
                    if(isDc) {
                        ctx.println(" (<i>default content</i>)");
                    } else if(participation && nullStatus!=null && !nullStatus.isEmpty()) {
						Hashtable<Integer, String> what = nullStatus.get(subLocale);				
						if(what!=null) {
							ctx.println("<br><blockquote><b>Did not participate:</b> ");
							for(Iterator<String> i = what.values().iterator();i.hasNext();) {
								ctx.println(i.next().toString()	);
								if(i.hasNext()) {
									ctx.println(", ");
								}
							}
							ctx.println("</blockquote>");
						}
					}
                //}
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
            //int totalResult=sqlCount(ctx,conn,"select COUNT(*) from CLDR_RESULT");
            //int totalData=sqlCount(ctx,conn,"select COUNT(id) from CLDR_DATA");
            //ctx.println("In all, the SurveyTool has " + totalResult + " items.<br>");
			
            if(participation) {
                ctx.println("<hr>");
                ctx.println("<h4>Participated: "+userMap.size()+"</h4><table border='1'>");
                for(Iterator<String> i = userMap.values().iterator();i.hasNext();) {
                    String which = (String)i.next();
                    ctx.println(which);
                }
                ctx.println("</table><h4>Did Not Participate at all: "+nullMap.size()+"</h4><table border='1'>");
                for(Iterator<String> i = nullMap.values().iterator();i.hasNext();) {
                    String which = (String)i.next();
                    ctx.println(which);
                }
                ctx.println("</table>");
            }
            SurveyMain.closeDBConnection(conn);
		}

        printFooter(ctx);
    }
    
    // ============= User list management
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

    static final String LIST_ACTION_CHANGE_EMAIL = "setemail_";
    static final String LIST_ACTION_CHANGE_NAME = "setname_";
    static final String LIST_ACTION_CHANGE_PASSWORD = "setpass_";
    
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
                String theirName = SurveyMain.getStringUTF8(rs, 3);//rs.getString(3);
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
            logger.log(java.util.logging.Level.WARNING,"Query for org " + org + " failed: " + unchainSqlException(se),se);
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
        WebContext subCtx = new WebContext(ctx);
        subCtx.setQuery(QUERY_DO, doWhat);
        if(justme) {
            printHeader(ctx, "My Account");
        } else {
            printHeader(ctx, "List Users" + ((just==null)?"":(" - " + just)));
        }
        printUserTableWithHelp(ctx, "/AddModifyUser");
        if(reg.userCanCreateUsers(ctx.session.user)) {
            ctx.println("<a href='" + ctx.jspLink("adduser.jsp") 
                    + "&amp;defaultorg="+URLEncoder.encode(ctx.session.user.org)
                    +"'>[Add User]</a> |");
//            ctx.println("<a href='" + ctx.jspLink("adduser.jsp") +"'>[Add User]</a> |");
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
		boolean showLocked = ctx.prefBool(PREF_SHOWLOCKED);
		boolean areSendingDisp = (ctx.field(LIST_MAILUSER+"_d").length())>0; // sending a dispute note?
        String mailBody = null;
        String mailSubj = null;
        boolean hideUserList = false;
        if(UserRegistry.userCanEmailUsers(ctx.session.user)) {
            cleanEmail = ctx.session.user.email;
            if(cleanEmail.equals("admin@")) {
                cleanEmail = "surveytool@unicode.org";
            }
            if(ctx.field(LIST_MAILUSER_CONFIRM).equals(cleanEmail)) {
                ctx.println("<h1>sending mail to users...</h4>");
				didConfirmMail=true;
                mailBody = "Message from " + getRequester(ctx) + ":\n--------\n"+sendWhat+
                    "\n--------\n\nSurvey Tool: http://" + ctx.serverHostport() + ctx.base()+"\n\n";
                mailSubj = "CLDR SurveyTool message from " +getRequester(ctx);
				if(!areSendingDisp) {
					areSendingMail= true; // we are ready to go ahead and mail..
				}
            } else if(ctx.hasField(LIST_MAILUSER_CONFIRM)) {
                ctx.println("<h1 class='ferrbox'>"+ctx.iconHtml("stop","emails did not match")+" not sending mail - you did not confirm the email address. See form at bottom of page."+"</h1>");
            }
            
            if(!areSendingMail && !areSendingDisp && ctx.hasField(LIST_MAILUSER)) {
                hideUserList = true; // hide the user list temporarily.
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
                if(UserRegistry.userIsTC(ctx.session.user)) {
                    showTogglePref(subCtx, PREF_SHOWLOCKED, "Show locked users:");
                }
                ctx.println("<br>");
                if(UserRegistry.userCanModifyUsers(ctx.session.user)) {
                    ctx.println("<div class='fnotebox'>"+ctx.iconHtml("warn","warning")+"Changing user level or locales while a user is active will result in  " +
                                " destruction of their session. Check if they have been working recently.</div>");
                }
            }
            // Preset box
            boolean preFormed = false;
            
            if(hideUserList) {
                String warnHash = "userlist";
                ctx.println("<div id='h_"+warnHash+"'><a href='javascript:show(\"" + warnHash + "\")'>" + 
                            "<b>+</b> Click here to show the user list...</a></div>");
                ctx.println("<!-- <noscript>Warning: </noscript> -->" + 
                            "<div style='display: none' id='" + warnHash + "'>" );
                ctx.println("<a href='javascript:hide(\"" + warnHash + "\")'>" + "(<b>- hide userlist</b>)</a><br>");

            }
            
            if((just==null) 
                  && UserRegistry.userCanModifyUsers(ctx.session.user) && !justme) {
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
        /*
                for(int i=0;i<UserRegistry.ALL_LEVELS.length;i++) {
                    ctx.println("<option class='user" + UserRegistry.ALL_LEVELS[i] + "' ");
                    ctx.println(" value='"+LIST_ACTION_SETLEVEL+UserRegistry.ALL_LEVELS[i]+"'>" + UserRegistry.levelToStr(ctx, UserRegistry.ALL_LEVELS[i]) + "</option>");
                }
                ctx.println("   <option>" + LIST_ACTION_NONE + "</option>");
                ctx.println("   <option value='" + LIST_ACTION_DELETE0 +"'>Delete user..</option>");
                ctx.println("   <option>" + LIST_ACTION_NONE + "</option>");
        */
                ctx.println("   <option value='" + LIST_ACTION_SHOW_PASSWORD + "'>Show password URL...</option>");
                ctx.println("   <option value='" + LIST_ACTION_SEND_PASSWORD + "'>Resend password...</option>");
//                ctx.println("   <option value='" + LIST_ACTION_SETLOCALES + "'>Set locales...</option>");
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
                ctx.println("<input type='submit' name='doBtn' value='Do Action'>");
            }
            ctx.println("<table summary='User List' class='userlist' border='2'>");
            ctx.println(" <tr><th></th><th>Organization / Level</th><th>Name/Email</th><th>Action</th><th>Locales</th><th>Seen</th></tr>");
            String oldOrg = null;
            int locked = 0;
            while(rs.next()) {
                int theirId = rs.getInt(1);
                int theirLevel = rs.getInt(2);
                if(!showLocked && theirLevel >= UserRegistry.LOCKED) {
                    locked++;
                    continue;
                }
                String theirName = SurveyMain.getStringUTF8(rs, 3);//rs.getString(3);
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
                
                if((just==null)&&(!justme)&&(!theirOrg.equals(oldOrg))) {
                    ctx.println("<tr class='heading' ><th class='partsection' colspan='6'><a name='"+theirOrg+"'><h4>"+theirOrg+"</h4></a></th></tr>");
                    oldOrg = theirOrg;
                }
                
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
                        UserRegistry.User newThem = reg.getInfo(theirId);
                        if(newThem != null) {
                            theirLocales = newThem.locales; // update
                        }
                        ctx.println("</td>");
                    } else if((action!=null)&&(action.length()>0)&&(!action.equals(LIST_ACTION_NONE))) { // other actions
                        ctx.println("<td class='framecell'>");
                        
                        // check an explicit list. Don't allow random levels to be set.
                        for(int i=0;i<UserRegistry.ALL_LEVELS.length;i++) {
                            if(action.equals(LIST_ACTION_SETLEVEL + UserRegistry.ALL_LEVELS[i])) {
                                if((just==null)&&(UserRegistry.ALL_LEVELS[i]<=UserRegistry.TC)) {
                                    ctx.println("<b>Must be zoomed in on a user to promote them to TC</b>");
                                } else {
                                    msg = reg.setUserLevel(ctx, theirId, theirEmail, UserRegistry.ALL_LEVELS[i]);
                                    ctx.println("Set user level to " + UserRegistry.levelToStr(ctx, UserRegistry.ALL_LEVELS[i]));
                                    ctx.println(": " + msg);
                                    theirLevel = UserRegistry.ALL_LEVELS[i];
                                    if(theUser != null) {
                                        ctx.println("<br/><i>Logging out user session " + theUser.id + "</i>");
                                        theUser.remove();
                                    }
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
                        } else if(UserRegistry.userCanDeleteUser(ctx.session.user,theirId,theirLevel)) {
                            // change of other stuff.
                            if(UserRegistry.userIsAdmin(ctx.session.user) && action.equals(LIST_ACTION_CHANGE_PASSWORD)) {
                                String what = "password";
                                UserRegistry.InfoType type = UserRegistry.InfoType.INFO_PASSWORD;
                                
                                String s0 = ctx.field("string0"+what);
                                String s1 = ctx.field("string1"+what);
                                if(s0.equals(s1)&&s0.length()>0) {
                                    ctx.println("<h4>Change "+what+" to <tt class='codebox'>"+s0+"</tt></h4>");
                                    action = ""; // don't popup the menu again.
                                    
                                    msg = reg.updateInfo(ctx, theirId, theirEmail, type, s0);
                                    ctx.println("<div class='fnotebox'>"+msg+"</div>");
                                    ctx.println("<i>click Change again to see changes</i>");                                    
                                } else {
                                    ctx.println("<h4>Change " + what+"</h4>");
                                    if(s0.length()>0) {
                                        ctx.println("<p class='ferrbox'>Both fields must match.</p>");
                                    }
                                    ctx.println("<label><b>New "+what+ ":</b><input type='password' name='string0"+what+"' value='"+s0+"'></label><br>");
                                    ctx.println("<label><b>New "+what+ ":</b><input type='password' name='string1"+what+"'> (confirm)</label>");
                                    
                                    ctx.println("<br><br>");
                                    ctx.println("(Suggested password: <tt>"+UserRegistry.makePassword(theirEmail)+"</tt> )");
                                }
                            } else if(action.equals(LIST_ACTION_CHANGE_EMAIL) ||
                                 action.equals(LIST_ACTION_CHANGE_NAME)) {
                                String what = action.equals(LIST_ACTION_CHANGE_EMAIL)?"Email":"Name";
                                UserRegistry.InfoType type = action.equals(LIST_ACTION_CHANGE_EMAIL)?UserRegistry.InfoType.INFO_EMAIL:UserRegistry.InfoType.INFO_NAME;
                                
                                String s0 = ctx.field("string0"+what);
                                String s1 = ctx.field("string1"+what);
                                if(s0.equals(s1)&&s0.length()>0) {
                                    ctx.println("<h4>Change "+what+" to <tt class='codebox'>"+s0+"</tt></h4>");
                                    action = ""; // don't popup the menu again.
                                    
                                    msg = reg.updateInfo(ctx, theirId, theirEmail, type, s0);
                                    ctx.println("<div class='fnotebox'>"+msg+"</div>");
                                    ctx.println("<i>click Change again to see changes</i>");                                    
                                } else {
                                    ctx.println("<h4>Change " + what+"</h4>");
                                    if(s0.length()>0) {
                                        ctx.println("<p class='ferrbox'>Both fields must match.</p>");
                                    }
                                    ctx.println("<label><b>New "+what+ ":</b><input name='string0"+what+"' value='"+s0+"'></label><br>");
                                    ctx.println("<label><b>New "+what+ ":</b><input name='string1"+what+"'> (confirm)</label>");
                                }
                            }
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
                        "' >"+ctx.iconHtml("zoom","More on this user..")+"</a>");
                }
                ctx.println("</td>");
                
                // org, level
                ctx.println("    <td>" + theirOrg + "<br>" +
                    "&nbsp; <span style='font-size: 80%' align='right'>" + UserRegistry.levelToStr(ctx,theirLevel).replaceAll(" ","&nbsp;") + "</span></td>");

                ctx.println("    <td valign='top'><font size='-1'>#" + theirId + " </font> <a name='u_"+theirEmail+"'>" +  theirName + "</a>");                
                ctx.println("    <a href='mailto:" + theirEmail + "'>" + theirEmail + "</a>");
                ctx.print("</td><td>");
                if(havePermToChange) {
                    // Was something requested?
                    
                    { // PRINT MENU
                        ctx.print("<select name='" + theirTag + "'>");
                        
                        // set user to VETTER
                        ctx.println("   <option value=''>" + LIST_ACTION_NONE + "</option>");
                        if(just != null)  {
                            for(int i=0;i<UserRegistry.ALL_LEVELS.length;i++) {
                                int lev = UserRegistry.ALL_LEVELS[i];
                                if((just==null)&&(lev<=UserRegistry.TC)) {
                                    continue; // no promotion to TC from zoom out
                                }
                                doChangeUserOption(ctx, lev, theirLevel,
                                                  false&&(preset_fromint==theirLevel)&&preset_do.equals(LIST_ACTION_SETLEVEL + lev) );
                            }
                            ctx.println("   <option disabled>" + LIST_ACTION_NONE + "</option>");                                                            
                        }
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

                        if(just != null)  {
                            if(theirLevel > UserRegistry.TC) {
                                ctx.println("   <option ");
                                if((preset_fromint==theirLevel)&&preset_do.equals(LIST_ACTION_SETLOCALES)) {
                              //      ctx.println(" SELECTED ");
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
                                    //    ctx.println(" SELECTED ");
                                    }
                                    ctx.println(" value='" + LIST_ACTION_DELETE0 +"'>Delete user..</option>");
                                }
                            }
                            if(just != null) { // only do these in 'zoomin' view.
                                ctx.println("   <option disabled>" + LIST_ACTION_NONE + "</option>");     
                                
                                                                                                                                              
                                // CHANGE EMAIL
                                ctx.print (" <option ");
                                if(LIST_ACTION_CHANGE_EMAIL.equals(action)) {
                                    ctx.print (" SELECTED ");
                                }
                                ctx.println(" value='" + LIST_ACTION_CHANGE_EMAIL + "'>Change Email...</option>");
                                
                                
                                // CHANGE NAME
                                ctx.print(" <option  ");
                                if(LIST_ACTION_CHANGE_NAME.equals(action)) {
                                    ctx.print (" SELECTED ");
                                }
                                ctx.println(" value='" + LIST_ACTION_CHANGE_NAME + "'>Change Name...</option>");

                                // CHANGE PASSWORD
                                if(UserRegistry.userIsAdmin(ctx.session.user)) {
                                    ctx.print(" <option  ");
                                    if(LIST_ACTION_CHANGE_PASSWORD.equals(action)) {
                                        ctx.print (" SELECTED ");
                                    }
                                    ctx.println(" value='" + LIST_ACTION_CHANGE_PASSWORD+ "'>Change Password...</option>");
                                }
                            
                            }
                        }
                        ctx.println("    </select>");
                    } // end menu
                }
                ctx.println("</td>");
                
                if(theirLevel <= UserRegistry.TC) {
                    ctx.println(" <td>" + UserRegistry.prettyPrintLocale(null) + "</td> ");
                } else {
                    ctx.println(" <td>" + UserRegistry.prettyPrintLocale(theirLocales) + "</td>");
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
            
            if(hideUserList ) {
                ctx.println("</div>");
            }
            if(!justme) {
                ctx.println("<div style='font-size: 70%'>Number of users shown: " + n +"</div><br>");
                if(UserRegistry.userIsTC(ctx.session.user) && locked > 0) {
                    showTogglePref(subCtx, PREF_SHOWLOCKED, "Show "+locked+" locked users:");
                }
            }
            if(!justme && UserRegistry.userCanModifyUsers(ctx.session.user)) {
                if((n>0) && UserRegistry.userCanEmailUsers(ctx.session.user)) { // send a mass email to users
                    if(ctx.field(LIST_MAILUSER).length()==0) {
                        ctx.println("<label><input type='checkbox' value='y' name='"+LIST_MAILUSER+"'>Check this box to compose a message to these " + n + " users (excluding LOCKED users).</label>");
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
								ctx.println("<label><input type='checkbox' value='y' name='"+LIST_MAILUSER+"_d'>Check this box to send a Dispute complaint with this email.</label><br>");
							} else {
								ctx.println("<i>On the next page you will be able to choose a Dispute Report.</i><br>");
							}
                            ctx.println("<input type='hidden' name='"+LIST_MAILUSER+"' value='y'>");
                        }
                        ctx.println("From: <b>"+cleanEmail+"</b><br>");
                        if(sendWhat.length()>0) {
                            ctx.println("<div class='odashbox'>"+
                                TransliteratorUtilities.toHTML.transliterate(sendWhat).replaceAll("\n","<br>")+
                                "</div>");
                            if(!didConfirmMail) {
                                ctx.println("<input type='hidden' name='"+LIST_MAILUSER_WHAT+"' value='"+
                                        sendWhat.replaceAll("&","&amp;").replaceAll("'","&quot;")+"'>");
                                if(!ctx.field(LIST_MAILUSER_CONFIRM).equals(cleanEmail) && (ctx.field(LIST_MAILUSER_CONFIRM).length()>0)) {
                                    ctx.println("<strong>"+ctx.iconHtml("stop","email did not match")+"That email didn't match. Try again.</strong><br>");
                                }
                                ctx.println("To confirm sending, type the email address <tt class='codebox'>"+cleanEmail+"</tt> in this box : <input name='"+LIST_MAILUSER_CONFIRM+
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
                            String newIntLocs = ctx.field("intlocs");
                            
                            String msg = reg.setLocales(ctx, ctx.session.user.id, ctx.session.user.email, newIntLocs, true);
                            
                            if(msg != null) {
                                ctx.println(msg+"<br>");
                            }
                            UserRegistry.User newMe = reg.getInfo(ctx.session.user.id);
                            if(newMe != null) {
                                ctx.session.user.intlocs = newMe.intlocs; // update
                            }
                        }
                    
                    
                        ctx.println("<input type='hidden' name='intlocs_change' value='t'>");
                        ctx.println("<label>Locales: <input name='intlocs' ");
                        if(ctx.session.user.intlocs != null) {
                            ctx.println("value='"+ctx.session.user.intlocs.trim()+"' ");
                        }
                        ctx.println("</input></label>");
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
                ctx.println("<input type='submit' name='doBtn' value='Do Action'>");
                ctx.println("</form>");
            }
        }/*end synchronized(reg)*/ } catch(SQLException se) {
            logger.log(java.util.logging.Level.WARNING,"Query for org " + org + " failed: " + unchainSqlException(se),se);
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
            ctx.println("    <option " + /* (selected?" SELECTED ":"") + */ "value='" + LIST_ACTION_SETLEVEL + newLevel + "'>Make " +
                        UserRegistry.levelToStr(ctx,newLevel) + "</option>");
        }
    }

    boolean showTogglePref(WebContext ctx, String pref, String what) {
        boolean val = ctx.prefBool(pref);
        WebContext nuCtx = (WebContext)ctx.clone();
        nuCtx.addQuery(pref, !val);
//        nuCtx.println("<div class='pager' style='float: right;'>");
        nuCtx.println("<a href='" + nuCtx.url() + "'>" + what + " is currently ");
		ctx.println(
			((val)?"<span class='selected'>On</span>":"<span style='color: #ddd' class='notselected'>On</span>") + 
				"&nbsp;/&nbsp;" +
			((!val)?"<span class='selected'>Off</span>":"<span style='color: #ddd' class='notselected'>Off</span>") );
		ctx.println("</a><br>");
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
            WebContext nuCtx = (WebContext)ctx.clone();
            nuCtx.addQuery(pref, "default");
            ctx.println("<a href='"+nuCtx.url()+"' class='"+(val.equals("default")?"selected":"notselected")+"'>"+"default"+"</a> ");
        }
        for(int n=0;n<list.length;n++) {
//            ctx.println("    <option " + (val.equals(list[n])?" SELECTED ":"") + "value='" + list[n] + "'>"+list[n] +"</option>");
            WebContext nuCtx = (WebContext)ctx.clone();
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

        vet.doOrgDisputePage(ctx);

        ctx.addQuery(QUERY_DO,"disputed");

        ctx.println("<h2>Disputed Items</h2>");

        vet.doDisputePage(ctx);
        
        printFooter(ctx);
    } 
	
    void doOptions(WebContext ctx) {
        printHeader(ctx, "My Options");
        printUserTableWithHelp(ctx, "/MyOptions");
        
        ctx.println("<a href='"+ctx.url()+"'>Return to SurveyTool</a><hr>");
        ctx.addQuery(QUERY_DO,"options");
        ctx.println("<h2>My Options</h2>");
  
  
      if(isPhaseSubmit()) {
            ctx.println("<h4>Coverage</h4>");
            ctx.print("<blockquote>");
            ctx.println("<p class='hang'>For more information on coverage, see "+
                "<a href='http://www.unicode.org/cldr/data/docs/web/survey_tool.html#Coverage'>Coverage Help</a></p>");
            String lev = showListPref(ctx, PREF_COVLEV, "Coverage Level", PREF_COVLEV_LIST);
            
            if(lev.equals("default")) {
                ctx.print("&nbsp;");
                ctx.print("&nbsp;");
                showListPref(ctx,PREF_COVTYP, "Coverage Type", ctx.getLocaleTypes(), true);
            } else {
                ctx.print("&nbsp;");
                ctx.print("&nbsp;<span class='deactivated'>Coverage Type: <b>n/a</b></span><br>");
            }
            ctx.println("<br>(Current effective coverage level: <tt class='codebox'>" + ctx.defaultPtype()+"</tt>)<p>");
            
            ctx.println("</blockquote>");
        }
        
        ctx.print("<hr>");

		if(UserRegistry.userIsTC(ctx.session.user)) {
		    showTogglePref(ctx, PREF_DELETEZOOMOUT, "Show delete controls when not zoomed in:");
		    showTogglePref(ctx, PREF_SHOWUNVOTE, "Show controls for removing votes:");
		}
		
        ctx.println("<h4>Advanced Options</h4>");
        ctx.print("<blockquote>");
        boolean adv = showTogglePref(ctx, PREF_ADV, "Show Advanced Options");
        ctx.println("</blockquote>");

        
        if(adv == true) {
            ctx.println("<div class='ferrbox'><i>Do not enable these items unless instructed.</i><br>");
            showTogglePref(ctx, PREF_NOPOPUPS, "Reduce popup windows");
            showTogglePref(ctx, PREF_XPID, "show XPATH ids");
            showTogglePref(ctx, PREF_GROTTY, "show obtuse items");
            showTogglePref(ctx, PREF_XPATHS, "Show full XPaths");
            showTogglePref(ctx, PREF_NOSHOWDELETE, "Suppress controls for deleting unused items in zoomed-in view:");
            showTogglePref(ctx, PREF_NOJAVASCRIPT, "Reduce the use of JavaScript (unimplemented)");
            ctx.println("</div>");
        }

        
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
        
        
        if(lockOut != null) {
            if(ctx.field("unlock").equals(lockOut)) {
                ctx.session.put("unlock",lockOut);
            } else {
                String unlock = (String)ctx.session.get("unlock");
                if((unlock==null) || (!unlock.equals(lockOut))) {
                    showSpecialHeader(ctx);
                    ctx.print("<hr><div class='ferrbox'>Sorry, the Survey Tool has been locked for maintenance work. Please try back later.</div>");
                    printFooter(ctx);
                    return;
                }
            }
        }
        
        // setup thread name
        if(ctx.session.user != null) {
        	Thread.currentThread().setName(Thread.currentThread().getName()+" "+ctx.session.user.id+":"+ctx.session.user.toString());        	
        }
        if(ctx.hasField(SurveyForum.F_FORUM) || ctx.hasField(SurveyForum.F_XPATH)) {
            fora.doForum(ctx, sessionMessage);
            return;
        }
        
        // Redirect references to language locale
        if(ctx.field("x").equals("references")) {
            if(ctx.getLocale() != null) {
                String langLocale = ctx.getLocale().getLanguage();
                if(!langLocale.equals(ctx.getLocale().toString())) {
                    ctx.redirect(ctx.base()+"?_="+langLocale+"&x=references");
                    return;
                }
            } else {
                ctx.redirect(ctx.base());
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
            sessionMessage = ("<i>Could not do the action '"+doWhat+"'. You may need to be logged in first.</i>");
        }
        
        String title = " " + which;
        if(ctx.hasField(QUERY_EXAMPLE))  {
            title = title + " Example"; 
        }
        printHeader(ctx, title);
        if(sessionMessage != null) {
            ctx.println(sessionMessage);
        }
        
        WebContext baseContext = (WebContext)ctx.clone();
        

        // print 'shopping cart'
        if(!ctx.hasField(QUERY_EXAMPLE))  {
            
            if((which.length()==0) && (ctx.getLocale()!=null)) {
                which = xMAIN;
            }
         //   if(false&&(which.length()>0)) {
         // printUserTableWithHelp(ctx, "/"+which.replaceAll(" ","_")); // Page Help
         //   } else {
                printUserTable(ctx);
         //   }

            Hashtable lh = ctx.session.getLocales();
            Enumeration e = lh.keys();
            if(e.hasMoreElements()) {
                boolean shownHeader = false;
                for(;e.hasMoreElements();) {
                    String k = e.nextElement().toString();
                    if((ctx.getLocale()!=null)&&(ctx.getLocale().toString().equals(k))) {
                        continue;
                    }
                    if(!shownHeader) {
                        ctx.println("<p align='right'><B>Recent locales: </B> ");
                        shownHeader = true;
                    }
                    boolean canModify = UserRegistry.userCanModifyLocale(ctx.session.user,CLDRLocale.getInstance(k));
                    ctx.print("<a href=\"" + baseContext.url() + ctx.urlConnector() + QUERY_LOCALE+"=" + k + "\">" + 
                                new ULocale(k).getDisplayName(ctx.displayLocale));
                    if(canModify) {
                        ctx.print(modifyThing(ctx));
                    }
                    ctx.println("</a> ");
                }
                if(shownHeader) {
                    ctx.println("</p>");
                }
            }
        }
        
        if((ctx.getLocale() != null) && 
            (!ctx.hasField(QUERY_EXAMPLE)) ) { // don't show these warnings for example pages.
            CLDRLocale dcParent = CLDRLocale.getInstance(supplemental.defaultContentToParent(ctx.getLocale().toString()));
            String dcChild = supplemental.defaultContentToChild(ctx.getLocale().toString());
            if(dcParent != null) {
                ctx.println("<b><a href=\"" + ctx.url() + "\">" + "Locales" + "</a></b><br/>");
                ctx.println("<h1>"+ctx.getLocale().getDisplayName(ctx.displayLocale)+"</h1>");
                ctx.println("<div class='ferrbox'>This locale is the default content for <b>"+
                    getLocaleLink(ctx,dcParent,null)+
                    "</b>; thus editing and viewing is disabled. Please view and/or propose changes in <b>"+
                    getLocaleLink(ctx,dcParent,null)+
                    "</b> instead. <br>");
                ctx.printHelpLink("/DefaultContent","Help with Default Content");
                ctx.print("</div>");
                
                //printLocaleTreeMenu(ctx, which);
                printFooter(ctx);
                return; // Disable viewing of default content
                
            } else if (dcChild != null) {
                String dcChildDisplay = new ULocale(dcChild).getDisplayName(ctx.displayLocale);
                ctx.println("<div class='fnotebox'>This locale supplies the default content for <b>"+
                    dcChildDisplay+
                    "</b>. Please make sure that all the changes that you make here are appropriate for <b>"+
                    dcChildDisplay+
                    "</b>. If you add any changes that are inappropriate for other sublocales, be sure to override their values.<br>");
                ctx.printHelpLink("/DefaultContent","Help with Default Content");
                ctx.print("</div>");
            }
            CLDRLocale aliasTarget = isLocaleAliased(ctx.getLocale());
            if(aliasTarget != null) {
                // the alias might be a default content locale. Save some clicks here. 
                dcParent = CLDRLocale.getInstance(supplemental.defaultContentToParent(aliasTarget.toString()));
                if(dcParent == null) {
                    dcParent = aliasTarget;
                }
                ctx.println("<div class='ferrbox'>This locale is aliased to <b>"+
                    getLocaleLink(ctx,aliasTarget,null)+
                    "</b>. You cannot modify it. Please make all changes in <b>"+
                    getLocaleLink(ctx,dcParent,null)+
                    "</b>.<br>");
                ctx.printHelpLink("/AliasedLocale","Help with Aliased Locale");
                ctx.print("</div>");
                

                ctx.println("<div class='ferrbox'><h1>"+ctx.iconHtml("stop",null)+"We apologise for the inconvenience, but there is currently an error with how these aliased locales are resolved.  Kindly ignore this locale for the time being. You must make all changes in <b>"+
                    getLocaleLink(ctx,dcParent,null)+
                    "</b>.</h1>");
                ctx.print("</div>");
                
                
            }
        }
        
        doLocale(ctx, baseContext, which);
    }
    
    LocaleTree localeTree = null;
//    protected Map<String,CLDRLocale> getLocaleListMap()
//    {
//        return getLocaleTree().getMap();
//    }

    public CLDRLocale getLocaleCode(String localeName) {
        return getLocaleTree().getLocaleCode(localeName);
    }

    protected synchronized LocaleTree getLocaleTree() {
        if(localeTree == null) {
            LocaleTree newLocaleTree = new LocaleTree(BASELINE_LOCALE);
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
                    newLocaleTree.add(CLDRLocale.getInstance(localeName));
                }
            }
            localeTree = newLocaleTree;
        }
        return localeTree;
    }
    
    public  String getLocaleDisplayName(CLDRLocale locale) {
        return locale.getDisplayName(BASELINE_LOCALE);
    }


    
    void printLocaleStatus(WebContext ctx, CLDRLocale localeName, String str, String explanation) {
        ctx.print(getLocaleStatus(localeName,str,explanation));
    }
    
    String getLocaleStatus(CLDRLocale localeName, String str, String explanation) {
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
    
    String getLocaleLink(WebContext ctx, CLDRLocale aliasTarget, String n) {
        if(n == null) {
            n = aliasTarget.getDisplayName(ctx.displayLocale) ;
        }
        String connector = ctx.urlConnector();
//        boolean hasDraft = draftSet.contains(localeName);
//        ctx.print(hasDraft?"<b>":"") ;
        String rv = 
            ("<a "  /* + (hasDraft?"class='draftl'":"class='nodraft'")  */
                  +" title='" + aliasTarget + "' href=\"" + ctx.url() 
                  + connector + QUERY_LOCALE+"=" + aliasTarget + "\">");
        rv = rv + getLocaleStatus(aliasTarget, n, aliasTarget.toString());
        boolean canModify = UserRegistry.userCanModifyLocale(ctx.session.user,aliasTarget);
        if(canModify) {
            rv = rv + (modifyThing(ctx));
            int odisp = 0;
            if((this.phase()==Phase.VETTING || this.phase() == Phase.SUBMIT || isPhaseVettingClosed()) && ((odisp=vet.getOrgDisputeCount(ctx.session.user.voterOrg(),aliasTarget))>0)) {
                rv = rv + ctx.iconHtml("disp","("+odisp+" org disputes)");
            }
        }
        rv = rv + ("</a>");
//        ctx.print(hasDraft?"</b>":"") ;

        return rv;
    }
    
    void printLocaleLink(WebContext ctx, CLDRLocale localeName, String n) {
        ctx.print(getLocaleLink(ctx,localeName,n));
    }


    /*
     * show a list of locales that fall into this interest group.
     */
    void printListFromInterestGroup(WebContext ctx, String intgroup) {
        LocaleTree lm = getLocaleTree();
        if(lm == null) {
            throw new RuntimeException("Can't load CLDR data files");
        }
        ctx.println("<table summary='Locale List' border=1 class='list'>");
        int n=0;
        for(String ln : lm.getTopLocales()) {
            CLDRLocale aLocale = lm.getLocaleCode(ln);
            
            if(aLocale==null) throw new InternalError("printListFromInterestGroup: can't find intgroup for locale "+ln);
            ULocale uLocale = aLocale.toULocale();
            if(!intgroup.equals(aLocale.getLanguage())) {
                continue;
            }
            
            n++;
            ctx.print("<tr class='row" + (n%2) + "'>");
            ctx.print(" <td valign='top'>");
            printLocaleLink(ctx, (aLocale), ln.toString());
            ctx.println(" </td>");
            
            Map<String, CLDRLocale> sm = lm.getSubLocales(aLocale);
            
            ctx.println("<td valign='top'>");
            int j = 0;
            for(Iterator si = sm.keySet().iterator();si.hasNext();) {
                if(j>0) { 
                    ctx.println(", ");
                }
                String sn = (String)si.next();
                CLDRLocale subLocale = sm.get(sn);
//                if(subLocale.length()>0) {
                    printLocaleLink(ctx, (subLocale), sn);
//                }
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
            WebContext nuCtx = (WebContext)ctx.clone();
            nuCtx.addQuery(PREF_SHOWCODES, !showCodes);
            nuCtx.println("<div class='pager' style='float: right;'>");
            nuCtx.println("<a href='" + nuCtx.url() + "'>" + ((!showCodes)?"Show":"Hide") + " locale codes</a>");
            nuCtx.println("</div>");
        }
        ctx.println("<h1>Locales</h1>");

        ctx.print(fora.mainFeedIcon(ctx)+"<br>");
				
        ctx.println(SLOW_PAGE_NOTICE);
        LocaleTree lm = getLocaleTree();
//        if(lm == null) {
//            busted("Can't load CLDR data files from " + fileBase);
//            throw new RuntimeException("Can't load CLDR data files from " + fileBase);
//        }

        ctx.println("<table summary='Locale List' border=1 class='list'>");
        int n=0;
        for(String ln:lm.getTopLocales()) {
            n++;
            CLDRLocale aLocale = lm.getLocaleCode(ln);
            ctx.print("<tr class='row" + (n%2) + "'>");
            ctx.print(" <td valign='top'>");
            printLocaleLink(baseContext, (aLocale), ln);
            if(showCodes) {
                ctx.println("<br><tt>" + aLocale + "</tt>");
            }
            ctx.println(" </td>");
            
            Map<String,CLDRLocale> sm = lm.getSubLocales(aLocale);
            
            ctx.println("<td valign='top'>");
            int j = 0;
            for(Iterator si = sm.keySet().iterator();si.hasNext();) {
                if(j>0) { 
                    ctx.println(", ");
                }
                String sn = (String)si.next();
                CLDRLocale subLocale = sm.get(sn);
//                if(subLocale.length()>0) {
                    printLocaleLink(baseContext, (subLocale), sn);
                    if(showCodes) {
                        ctx.println("&nbsp;-&nbsp;<tt>" + subLocale + "</tt>");
                    }
//                }
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
        if(ctx.getLocale() != null) {
            locale = ctx.getLocale().toString();
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
    public void printMenu(WebContext ctx, String which, String menu, String title) {
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
    public void printMenu(WebContext ctx, String which, String menu, String title, String key, String anchor) {
        ctx.print(getMenu(ctx,which, menu, title, key, anchor));
    }
    
    public String getMenu(WebContext ctx, String which, String menu, String title) {
        return getMenu(ctx,which,menu,title,QUERY_SECTION);
    }

    
    public String getMenu(WebContext ctx, String which, String menu, String title, String key) {
        return getMenu(ctx,which,menu,title,key,null);
    }

    
    public String getMenu(WebContext ctx, String which, String menu, String title, String key, String anchor) {
        StringBuffer buf = new StringBuffer();
        if(menu.equals(which)) {
            buf.append("<b class='selected'>");
        } else {
            buf.append("<a class='notselected' href=\"" + ctx.url() + ctx.urlConnector() + key+"=" + menu +
					((anchor!=null)?("#"+anchor):"") +
                      "\">");
        }
        if(menu.endsWith("/")) {
            buf.append(title + "<font size=-1>(other)</font>");
        } else {
            buf.append(title);
        }
        if(menu.equals(which)) {
            buf.append("</b>");
        } else {
            buf.append("</a>");
        }
        return buf.toString();
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
            ctx.println(ctx.iconHtml("okay","mail sent")+"<i>Not sending mail- SMTP disabled.</i><br/>");
            ctx.println("<hr/><pre>" + message + "</pre><hr/>");
            smtp = "NONE";
        } else {
            MailSender.sendMail(smtp, mailFromAddress, mailFromName,  from, theirEmail, subject,
                                message);
            ctx.println("<br>"+ctx.iconHtml("okay","mail sent")+"Mail sent to " + theirEmail + " from " + from + " via " + smtp + "<br/>\n");
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
        } else if(type.equals(PathUtilities.MEASNAMES)) {
            subtype = MEASNAME;
        } else if(type.equals(PathUtilities.CODEPATTERNS)) {
            subtype = CODEPATTERN;
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
    
    
    void printLocaleTreeMenu(WebContext ctx, String which) {
        int n = ctx.docLocale.length;
        int i,j;
        WebContext subCtx = (WebContext)ctx.clone();
        subCtx.addQuery(QUERY_LOCALE,ctx.getLocale().toString());

        ctx.println("<table summary='locale info' width='95%' border=0><tr><td >"); // width='25%'
        ctx.println("<b><a href=\"" + ctx.url() + "\">" + "Locales" + "</a></b><br/>");
        for(i=(n-1);i>0;i--) {
            for(j=0;j<(n-i);j++) {
                ctx.print("&nbsp;&nbsp;");
            }
            boolean canModify = UserRegistry.userCanModifyLocale(ctx.session.user,ctx.docLocale[i]);
            ctx.print("\u2517&nbsp;<a title='"+ctx.docLocale[i]+"' class='notselected' href=\"" + ctx.url() + ctx.urlConnector() +QUERY_LOCALE+"=" + ctx.docLocale[i] + 
                "\">");
            printLocaleStatus(ctx, ctx.docLocale[i], ctx.docLocale[i].toULocale().getDisplayName(ctx.displayLocale), "");
            if(canModify) {
                ctx.print(modifyThing(ctx));
            }
            ctx.print("</a> ");
            ctx.print("<br/>");
        }
        for(j=0;j<n;j++) {
            ctx.print("&nbsp;&nbsp;");
        }
        boolean canModifyL = UserRegistry.userCanModifyLocale(ctx.session.user,ctx.getLocale());
        ctx.print("\u2517&nbsp;");
        ctx.print("<span title='"+ctx.getLocale()+"' style='font-size: 120%'>");
        printMenu(subCtx, which, xMAIN, 
            getLocaleStatus(ctx.getLocale(), ctx.getLocale().getDisplayName(ctx.displayLocale)+(canModifyL?modifyThing(ctx):""), "") );
        ctx.print("</span>");
        ctx.println("<br/>");
        ctx.println("</td><td style='padding-left: 1em;'>");
        
        subCtx.println("<p class='hang'> Code Lists: ");
        for(n =0 ; n < PathUtilities.LOCALEDISPLAYNAMES_ITEMS.length; n++) {        
            if(n>0) {
                ctx.print(" | ");
            }
            printMenu(subCtx, which, PathUtilities.LOCALEDISPLAYNAMES_ITEMS[n]);
        }
        subCtx.println("<p class='hang'> Calendars: ");
        if ( CALENDARS_ITEMS == null )
           CALENDARS_ITEMS = PathUtilities.getCalendarsItems();
        for(n =0 ; n < CALENDARS_ITEMS.length; n++) {        
            if(n>0) {
                ctx.print(" | ");
            }
            printMenu(subCtx, which, CALENDARS_ITEMS[n]);
        }

        subCtx.println("<p class='hang'> Metazones: ");
        if ( METAZONES_ITEMS == null )
           METAZONES_ITEMS = getMetazonesItems();
        for(n =0 ; n < METAZONES_ITEMS.length; n++) {        
            if(n>0) {
                ctx.print(" | ");
            }
            printMenu(subCtx, which, METAZONES_ITEMS[n]);
        }

        subCtx.println("</p> <p class='hang'>Other Items: ");
        
        for(n =0 ; n < OTHERROOTS_ITEMS.length; n++) {        
            if(!(OTHERROOTS_ITEMS[n].equals("references")&&  // don't show the 'references' tag if not in a lang locale.
                 !ctx.getLocale().getLanguage().equals(ctx.getLocale().toString()))) 
            {
                if(n>0) {
                    ctx.print(" | ");
                }
                printMenu(subCtx, which, OTHERROOTS_ITEMS[n]);
                ctx.print(" ");
            }
        }
        ctx.println("| <a " + ctx.atarget("st:supplemental") + " class='notselected' href='http://unicode.org/cldr/data/charts/supplemental/language_territory_information.html#"+
            ctx.getLocale().getLanguage()+"'>supplemental</a>");
        
        ctx.print("</p>");
        
        ctx.includeFragment("report_menu.jsp");
        
        boolean canModify = UserRegistry.userCanModifyLocale(subCtx.session.user, subCtx.getLocale());
        if(canModify) {
            subCtx.println("<p class='hang'>Forum: ");
            String forum = ctx.getLocale().getLanguage();
            subCtx.println("<strong><a href='"+fora.forumUrl(subCtx, forum)+"'>Forum: "+forum+"</a></strong>");
            subCtx.println(" <br> " + fora.forumFeedIcon(subCtx, forum));
            subCtx.println("</p>");
        }
        if(ctx.prefBool(PREF_ADV)) {
            subCtx.println("<p class='hang'>Advanced Items: ");
//                printMenu(subCtx, which, TEST_MENU_ITEM);
            printMenu(subCtx, which, RAW_MENU_ITEM);
            subCtx.println("</p>");
        }
        subCtx.println("</td></tr></table>");
    }
    
    public String[] getMetazonesItems()
    {

        String defaultMetazonesItems= "Africa America Antarctica Asia Australia Europe Atlantic Indian Pacific";
        return ( defaultMetazonesItems.split(" ") );

    }
    /**
        * show the actual locale data..
     * @param ctx context
     * @param which value of 'x' parameter.
     */
    public void showLocale(WebContext ctx, String which)
    {
        if(HAVE_REMOVE&&which.equals(xREMOVE)) {
            ctx.println("<b><a href=\"" + ctx.url() + "\">" + "List of Locales" + "</a></b><br/>");
            ctx.session.getLocales().remove(ctx.field(QUERY_LOCALE));
            ctx.println("<h2>Your session for " + ctx.field(QUERY_LOCALE) + " has been removed.</h2>");
            doMain(ctx);
            return;
        }
        synchronized (ctx.session) { // session sync
            UserLocaleStuff uf = getUserFile(ctx, (ctx.session.user==null)?null:ctx.session.user, ctx.getLocale());
            CLDRFile cf = uf.cldrfile;
            if(cf == null) {
                throw new InternalError("CLDRFile is null!");
            }
            XMLSource ourSrc = uf.dbSource; // TODO: remove. debuggin'
            if(ourSrc == null) {
                throw new InternalError("oursrc is null! - " + (USER_FILE + CLDRDBSRC) + " @ " + ctx.getLocale() );
            }
            // Set up checks
            CheckCLDR checkCldr = (CheckCLDR)uf.getCheck(ctx); //make it happen
            
            // Locale menu
            if((which == null) ||
               which.equals("")) {
               which = xMAIN;
            }
            
            

            if(!ctx.hasField(QUERY_EXAMPLE)) {
                printLocaleTreeMenu(ctx, which);
            } else { // end in-locale nav
                ctx.println("<h3>"+ctx.getLocale()+" "+ctx.getLocale().getDisplayName(ctx.displayLocale)+" / " + which + " Example</h3>");
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
                                ctx.println(": ");
                                printShortened(ctx, status.toString(), LARGER_MAX_CHARS);
                                ctx.print("<br>");
                            } else {
                                ctx.println("<i>example available</i><br>");
                            }
                        } catch(Throwable t) {
                            String result;
                            try {
                                result = status.toString();
                            } catch(Throwable tt) {
                                tt.printStackTrace();
                                result = "(Error reading error: " + tt+")";
                            }
                            ctx.println("Error reading status item: <br><font size='-1'>"+result+"<br> - <br>" + t.toString()+"<hr><br>");
                        }
                    }
                    ctx.println("</div>");
                }
            }
            
            
            // Find which pod they want, and show it.
            // NB keep these sections in sync with DataPod.xpathToPodBase() 
            WebContext subCtx = (WebContext)ctx.clone();
            subCtx.addQuery(QUERY_LOCALE,ctx.getLocale().toString());
            subCtx.addQuery(QUERY_SECTION,which);
            for(int n =0 ; n < PathUtilities.LOCALEDISPLAYNAMES_ITEMS.length; n++) {        
                if(PathUtilities.LOCALEDISPLAYNAMES_ITEMS[n].equals(which)) {
                    if(which.equals(PathUtilities.CURRENCIES)) {
                        showPathList(subCtx, "//ldml/"+PathUtilities.NUMBERSCURRENCIES, null);
                    } else if(which.equals(PathUtilities.TIMEZONES)) {
                        showTimeZones(subCtx);
                    } else {
                        showLocaleCodeList(subCtx, which);
                    }
                    return;
                }
            }

            for(int j=0;j<CALENDARS_ITEMS.length;j++) {
                if(CALENDARS_ITEMS[j].equals(which)) {
                    String CAL_XPATH = "//ldml/dates/calendars/calendar[@type=\""+which+"\"]";
                    showPathList(subCtx, CAL_XPATH, null);
                    return;
                }
            }

            for(int j=0;j<METAZONES_ITEMS.length;j++) {
                if(METAZONES_ITEMS[j].equals(which)) {
                    showMetazones(subCtx,which);
                    return;
                }
            }
            
            for(int j=0;j<OTHERROOTS_ITEMS.length;j++) {
                if(OTHERROOTS_ITEMS[j].equals(which)) {
                    if(which.equals(GREGORIAN_CALENDAR)) {
                        showPathList(subCtx, GREGO_XPATH, null);
                    } else if(which.equals(OTHER_CALENDARS)) {
                        showPathList(subCtx, PathUtilities.OTHER_CALENDARS_XPATH, null);
                    } else if(which.equals(LDMLConstants.LOCALEDISPLAYPATTERN)) {
                        showPathList(subCtx, PathUtilities.LOCALEDISPLAYPATTERN_XPATH, null);
                    } else if(which.equals("units")) {
                        showPathList(subCtx, "//ldml/units", null);
                    } else if(PathUtilities.xOTHER.equals(which)) {
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
            } else if(which.startsWith("r_")) {
                doReport(subCtx, which);
            } else {
                doMain(subCtx);
            }
        }
    }
    
    private static Pattern reportSuffixPattern = Pattern.compile("^[0-9a-z]([0-9a-z]*)$");
    /**
     * Is this a legal report suffix? Must contain one or more of [0-9a-z].
     * @param suffix The suffix (not including r_)
     * @return true if legal.
     */
    public static final boolean isLegalReportSuffix(String suffix) {
        return reportSuffixPattern.matcher(suffix).matches();
    }
    
    /**
     * Show a 'report' template (r_)
     * @param ctx context- preset with Locale
     * @param which current section
     */
    public void doReport(WebContext ctx, String which) {
        if(isLegalReportSuffix(which.substring(2))) {
            ctx.includeFragment(which+".jsp");
        } else {
            doMain(ctx);
        }
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
    public static final String ZXML_PREFIX="/zxml/main";
    public static final String ZVXML_PREFIX="/zvxml/main";
    public static final String VXML_PREFIX="/vxml/main";
    public static final String TXML_PREFIX="/txml/main";
    public static final String RXML_PREFIX="/rxml/main";
    public static final String FEED_PREFIX="/feed";
    
    private int doOutput(String kind) {
        boolean vetted=false;
        boolean resolved=false;
        boolean users=false;
        boolean translators=false;
        boolean sql = false;
        boolean data=false;
        String lastfile = null;
        String ourDate = new Date().toString(); // canonical date
        int nrOutFiles = 0;
        if(kind.equals("sql")) {
            sql= true;
        } else if(kind.equals("xml")) {
            vetted = false;
            data=true;
            resolved = false;
        } else if(kind.equals("vxml")) {
            vetted = true;
            data=true;
            resolved = false;
        } else if(kind.equals("rxml")) {
            vetted = true;
            data=true;
            resolved = true;
        } else if(kind.equals("users")) {
            users = true;
        } else if(kind.equals("usersa")) {
            users = true;
        } else if(kind.equals("translators")) {
            translators = true;
        } else {
            throw new IllegalArgumentException("unknown output: " + kind);
        }
        File outdir = new File(vetdir, kind);
        File voteDir = null;
        if(data) voteDir = new File(outdir, "votes");
        if(outdir.exists() && outdir.isDirectory()) {
            File backup = new File(vetdir, kind+".old");
            
            // delete backup
            if(backup.exists() && backup.isDirectory()) {
                File backupVoteDir = new File(backup, "votes");
                if(backupVoteDir.exists()) {
                    File cachedBFiles[] = backupVoteDir.listFiles();
                    if(cachedBFiles != null) {
                        for(File f : cachedBFiles) {
                            if(f.isFile()) {
                                f.delete();
                            }
                        }
                    }
                    backupVoteDir.delete();
                }
                File cachedFiles[] = backup.listFiles();
                if(cachedFiles != null) {
                    for(File f : cachedFiles) {
                        if(f.isFile()) {
                            f.delete();
                        }
                    }
                }
                if(!backup.delete()) {
                    throw new InternalError("Can't delete backup: " + backup.getAbsolutePath());
                }
            }
            
            if(!outdir.renameTo(backup)) {
                throw new InternalError("Can't move outdir " + outdir.getAbsolutePath() + " to backup " + backup);
            }
        }
        
        if(!outdir.mkdir()) {
            throw new InternalError("Can't create outdir " + outdir.getAbsolutePath());
        }
        if(voteDir!=null && !voteDir.mkdir()) {
            throw new InternalError("Can't create voteDir " + voteDir.getAbsolutePath());
        }
        
        System.err.println("Writing " + kind);
        long lastTime = System.currentTimeMillis();
        long countStart = lastTime;

        if(sql) {
            File outFile = new File(outdir,"cldrdb.sql");
            try { 
                int nn=0;
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(
                            new FileOutputStream(outFile), "UTF8"));

                out.println("-- dump of CLDR data from " + cldrdb);
                out.println("-- mysql bias mode");
                out.println("-- "+ourDate);
                out.println();
                
                synchronized(reg) {
                    String table = UserRegistry.CLDR_USERS;
                    out.println("-- "+table);
                    out.println("DELETE FROM "+table+";");
                    
                    out.println("INSERT INTO "+table+" (id,userlevel,name,org,email,password,locales,intLocs) VALUES ");

                    java.sql.ResultSet rs = reg.listPass();
                    if(rs == null) {
                        out.print("-- no rows ");
                    } else while(rs.next()) {
                        if(nn>0) {
                            out.println(',');
                        }
                        nn++;
                        int theirId = rs.getInt(1);
                        int theirLevel = rs.getInt(2);
                        String theirName = SurveyMain.getStringUTF8(rs, 3);//rs.getString(3);
                        String theirEmail = rs.getString(4);
                        String theirOrg = rs.getString(5);
                        String theirLocales = rs.getString(6); // nbsp -> sp
                        String theirIntLocs = rs.getString(7); // nbsp -> sp
                        String theirPassword = rs.getString(8); // nbsp -> sp
                        if(theirLocales!=null) {
                            theirLocales = theirLocales.replace((char)0x00a0, ' ');
                        }
                        if(theirIntLocs!=null) {
                            theirIntLocs = theirIntLocs.replace((char)0x00a0, ' ');
                        }
                        String escapeName = escapeForMysqlUtf8(theirName);
                        if(escapeName.length()>(theirName.length()+2)) {
                            out.println("-- \""+theirName+"\"");
                        }
                        out.print("("+theirId+","+theirLevel+","+ escapeName+","+escapeForMysql(theirOrg)+","+
                                    escapeForMysql(theirEmail)+","+escapeForMysql(theirPassword)+","+
                                    escapeForMysql(theirLocales)+","+escapeForMysql(theirIntLocs)+")");
                    }
                    if(nn>0) out.println(";");
                    
                    
//                    conn = this.getDBConnection();
//                    
//                    String tables[] = { "CLDR_USERS" };
//                    
//                    for(String table : tables ) {
//                        
//                    }
                }
                    
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new InternalError("writing " + kind + " - IO Exception "+e.toString());
            } catch(SQLException se) {
                logger.log(java.util.logging.Level.WARNING,"Query for org " + null + " failed: " + unchainSqlException(se),se);
                //out.println("-- Failure: " + unchainSqlException(se) + " --");
            } finally {
//                SurveyMain.closeDBConnection(conn);
            }
            nrOutFiles++;
         } else if(users) {
            boolean obscured = kind.equals("usersa");
            File outFile = new File(outdir,kind+".xml");
            try {
                PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(
                        new FileOutputStream(outFile), "UTF8"));
    //            } catch (UnsupportedEncodingException e) {
    //                throw new InternalError("UTF8 unsupported?").setCause(e);
                out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        //        ctx.println("<!DOCTYPE ldml SYSTEM \"http://.../.../stusers.dtd\">");
                out.println("<users generated=\""+ourDate+"\" host=\""+localhost()+"\" obscured=\""+obscured+"\">");
                String org = null;
                try { synchronized(reg) {
                    java.sql.ResultSet rs = reg.list(org);
                    if(rs == null) {
                        out.println("\t<!-- No results -->");
                        return 0;
                    }
                    while(rs.next()) {
                        int theirId = rs.getInt(1);
                        int theirLevel = rs.getInt(2);
                        String theirName = obscured?("#"+theirId):SurveyMain.getStringUTF8(rs, 3);//rs.getString(3);
                        String theirEmail = obscured?"?@??.??":rs.getString(4);
                        String theirOrg = rs.getString(5);
                        String theirLocales = rs.getString(6);
                        
                        String orgMunged = theirOrg;
                        try {
                        	orgMunged = VoteResolver.Organization.fromString(theirOrg).name();
                        } catch(IllegalArgumentException iae) {
                        	// illegal org
                        }
                        if(orgMunged==null || orgMunged.length()<=0) {
                        	orgMunged = theirOrg;
                        }
                        
                        out.println("\t<user id=\""+theirId+"\" email=\""+theirEmail+"\">");
                        out.println("\t\t<level n=\""+theirLevel+"\" type=\""+UserRegistry.levelAsStr(theirLevel)+"\"/>");
                        out.println("\t\t<name>"+theirName+"</name>");
                        out.println("\t\t<org>"+theirOrg+"</org> <!-- type=\""+orgMunged+"\" -->");
                        out.println("\t\t<locales type=\"edit\">");
                        String theirLocalesList[] = UserRegistry.tokenizeLocale(theirLocales);
                        for(int i=0;i<theirLocalesList.length;i++) {
                            out.println("\t\t\t<locale id=\""+theirLocalesList[i]+"\"/>");
                        }
                        out.println("\t\t</locales>");
                        out.println("\t</user>");
                    }            
                }/*end synchronized(reg)*/ } catch(SQLException se) {
                    logger.log(java.util.logging.Level.WARNING,"Query for org " + org + " failed: " + unchainSqlException(se),se);
                    out.println("<!-- Failure: " + unchainSqlException(se) + " -->");
                }
                out.println("</users>");
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new InternalError("writing " + kind + " - IO Exception "+e.toString());
            }
            nrOutFiles++;
        } else if(translators) {
            File outFile = new File(outdir,"cldr-translators.txt");
            try {
                PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(
                        new FileOutputStream(outFile), "UTF8"));
    //        ctx.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
    //        ctx.println("<!DOCTYPE ldml SYSTEM \"http://.../.../stusers.dtd\">");
    //        ctx.println("<users host=\""+ctx.serverHostport()+"\">");
                String org = null;
                try { synchronized(reg) {
                    java.sql.ResultSet rs = reg.list(org);
                    if(rs == null) {
                        out.println("# No results");
                        return 0;
                    }
                    while(rs.next()) {
                        int theirId = rs.getInt(1);
                        int theirLevel = rs.getInt(2);
                        String theirName = SurveyMain.getStringUTF8(rs, 3);//rs.getString(3);
                        String theirEmail = rs.getString(4);
                        String theirOrg = rs.getString(5);
                        String theirLocales = rs.getString(6);
                        
                        if(theirLevel >= UserRegistry.LOCKED) {
                            continue;
                        }
                        
                        out.println(theirEmail);//+" : |NOPOST|");
                      /*
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
                       */
                    }            
                } } catch(SQLException se) {
                    logger.log(java.util.logging.Level.WARNING,"Query for org " + org + " failed: " + unchainSqlException(se),se);
                    out.println("# Failure: " + unchainSqlException(se) + " -->");
                }
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new InternalError("writing " + kind + " - IO Exception "+e.toString());
            }
            nrOutFiles++;
        } else {
            try {
                File inFiles[] = getInFiles();
                int nrInFiles = inFiles.length;
                progressWhat = "writing " +kind;
                progressMax = nrInFiles+1;
                //Set<Integer> xpathSet = new TreeSet<Integer>();
                boolean xpathSet[] = new boolean[0];
                Connection conn = getDBConnection();
                for(int i=0;i<nrInFiles;i++) {
                    progressCount = i;
                    String fileName = inFiles[i].getName();
                    int dot = fileName.indexOf('.');
                    String localeName = fileName.substring(0,dot);
                    lastfile = fileName;
                    File outFile = new File(outdir, fileName);
                    
                    XMLSource dbSource = makeDBSource(conn, null, CLDRLocale.getInstance(localeName), vetted);
                    CLDRFile file;
                    
                    if(resolved == false) {
                        file = makeCLDRFile(dbSource);
                    } else { 
                        file = new CLDRFile(dbSource,true);
                    }

                    long nextTime = System.currentTimeMillis();
                    if((nextTime - lastTime) > 10000) { // denote, every 10 seconds
                        lastTime = nextTime;
                        System.err.println("output: " + kind + " / " + localeName + ": #"+i+"/"+nrInFiles+", or "+
                            (((double)(System.currentTimeMillis()-countStart))/i)+"ms per.");
                    }
                    
                    try {
                        PrintWriter utf8OutStream = new PrintWriter(
                            new OutputStreamWriter(
                                new FileOutputStream(outFile), "UTF8"));
                        synchronized(this.vet.conn) {
                        	file.write(utf8OutStream);
                        }
                        nrOutFiles++;
                        utf8OutStream.close();
                        lastfile = null;
        //            } catch (UnsupportedEncodingException e) {
        //                throw new InternalError("UTF8 unsupported?").setCause(e);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new InternalError("IO Exception "+e.toString());
                    } finally {
                        if(lastfile != null) {
                            System.err.println("Last file written: " + kind + " / " + lastfile);
                        }
                    }
                    lastfile = fileName + " - vote data";
                    // write voteFile
                    File voteFile = new File(voteDir,fileName);
                    try {
                        PrintWriter utf8OutStream = new PrintWriter(
                            new OutputStreamWriter(
                                new FileOutputStream(voteFile), "UTF8"));
                        xpathSet = this.vet.writeVoteFile(utf8OutStream, conn, dbSource, file, ourDate, xpathSet);
                        nrOutFiles++;
                        utf8OutStream.close();
                        lastfile = null;
        //            } catch (UnsupportedEncodingException e) {
        //                throw new InternalError("UTF8 unsupported?").setCause(e);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new InternalError("IO Exception on vote file "+e.toString());
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        throw new InternalError("SQL Exception on vote file "+SurveyMain.unchainSqlException(e));
                    } finally {
                        if(lastfile != null) {
                            System.err.println("Last  vote file written: " + kind + " / " + lastfile);
                        }
                    }

                }
                SurveyMain.closeDBConnection(conn);

                progressWhat = "writing " +"xpathTable";
                lastfile = "xpathTable.xml" + " - xpath table";
                // write voteFile
                File xpathFile = new File(voteDir,"xpathTable.xml");
                try {
                    PrintWriter utf8OutStream = new PrintWriter(
                        new OutputStreamWriter(
                            new FileOutputStream(xpathFile), "UTF8"));
                    this.vet.writeXpaths(utf8OutStream, ourDate, xpathSet);
                    nrOutFiles++;
                    utf8OutStream.close();
                    lastfile = null;
    //            } catch (UnsupportedEncodingException e) {
    //                throw new InternalError("UTF8 unsupported?").setCause(e);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new InternalError("IO Exception on vote file "+e.toString());
                } finally {
                    if(lastfile != null) {
                        System.err.println("Last  vote file written: " + kind + " / " + lastfile);
                    }
                }
                
                
            } finally {
                progressWhat = null;
            }
        }
        System.err.println("output: " + kind + " - DONE, files: " + nrOutFiles);
        return nrOutFiles;
    }

    public synchronized boolean doRawXml(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        String s = request.getPathInfo();
        
        if((s==null)||!(s.startsWith(XML_PREFIX)||s.startsWith(ZXML_PREFIX)||s.startsWith(ZVXML_PREFIX)||s.startsWith(VXML_PREFIX)||s.startsWith(RXML_PREFIX)||s.startsWith(TXML_PREFIX)||s.startsWith(FEED_PREFIX))) {
            return false;
        }
        
        if(s.startsWith(FEED_PREFIX)) {
            return fora.doFeed(request, response);
        }

        boolean finalData = false;
        boolean resolved = false;
        boolean voteData = false;
        boolean cached = false;
        
        if(s.startsWith(VXML_PREFIX)) {
            finalData = true;

            if(s.equals(VXML_PREFIX)) {
                WebContext ctx = new WebContext(request,response);
                response.sendRedirect(ctx.schemeHostPort()+ctx.base()+VXML_PREFIX+"/");
                return true;
            }
            s = s.substring(VXML_PREFIX.length()+1,s.length()); //   "foo.xml"
        } else if(s.startsWith(RXML_PREFIX)) {
            finalData = true;
            resolved=true;

            if(s.equals(RXML_PREFIX)) {
                WebContext ctx = new WebContext(request,response);
                response.sendRedirect(ctx.schemeHostPort()+ctx.base()+RXML_PREFIX+"/");
                return true;
            }
            s = s.substring(RXML_PREFIX.length()+1,s.length()); //   "foo.xml"
        } else if(s.startsWith(TXML_PREFIX)) {
            finalData = true;
            resolved=false;
            voteData = true;

            if(s.equals(TXML_PREFIX)) {
                WebContext ctx = new WebContext(request,response);
                response.sendRedirect(ctx.schemeHostPort()+ctx.base()+TXML_PREFIX+"/");
                return true;
            }
            s = s.substring(TXML_PREFIX.length()+1,s.length()); //   "foo.xml"
        } else if(s.startsWith(ZXML_PREFIX)) {
            finalData = false;
            resolved=false;
            voteData = false;
            cached = true;
            s = s.substring(ZXML_PREFIX.length()+1,s.length()); //   "foo.xml"
        } else if(s.startsWith(ZVXML_PREFIX)) {
            finalData = true;
            resolved=false;
            voteData = false;
            cached = true;
            s = s.substring(ZVXML_PREFIX.length()+1,s.length()); //   "foo.xml"
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
            } else synchronized(this.vet.conn) {
		String doKvp = request.getParameter("kvp");
		boolean isKvp = (doKvp!=null && doKvp.length()>0);

		if(isKvp)  {
			response.setContentType("text/plain; charset=utf-8");
		} else {
                	response.setContentType("application/xml; charset=utf-8");
		}
                Connection conn = null;
                CLDRLocale locale = CLDRLocale.getInstance(theLocale);
                XMLSource dbSource = null; 
                CLDRFile file;
                if(cached == true) {
                    if(finalData) {
                        file = this.getCLDRFileCache().getVettedCLDRFile(locale);
                    } else { 
                        file = this.getCLDRFileCache().getCLDRFile(locale, resolved);
                    }
                } else {
//                    conn = getDBConnection();
                    dbSource = makeDBSource(locale, finalData);
                    if(resolved == false) {
                        file= makeCLDRFile(dbSource);
                    } else { 
                        file = new CLDRFile(dbSource,true);
                    }
                }
    //            file.write(WebContext.openUTF8Writer(response.getOutputStream()));
                if(voteData) {
                    try {
                        vet.writeVoteFile(response.getWriter(), conn, dbSource, file, new Date().toString(), null);
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        System.err.println("<!-- exception: "+e+" -->");
                    }
                } else {
			if(!isKvp) {
                    		file.write(response.getWriter());
			} else {
				// full xpath tab value
				java.io.Writer w = response.getWriter();
				//PrintWriter pw = new PrintWriter(w);
				for(String str : file) {
					String xo = file.getFullXPath(str);
					String v = file.getStringValue(str);

					w.write(xo+"\t"+v+"\n");

				}
						
			}
	
                }
                if(conn!=null) 
                    SurveyMain.closeDBConnection(conn);
            }
        }
        return true;
    }

    /**
    * Show the 'main info about this locale' (General) panel.
     */
    public void doMain(WebContext ctx) {
        //SLOW: String diskVer = LDMLUtilities.loadFileRevision(fileBase, ctx.getLocale().toString() + ".xml"); // just get ver of the latest file.
        String dbVer = dbsrcfac.getSourceRevision(ctx.getLocale());
        
        // what should users be notified about?
        if(isPhaseSubmit() || isPhaseVetting() || isPhaseVettingClosed()) {
            CLDRLocale localeName = ctx.getLocale();
            String groupName = localeName.getLanguage();
            int vetStatus = vet.status(localeName);
            
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
                    
                    Set<String> odItems = new TreeSet<String>();
                    synchronized(vet.conn) { 
                        try { // moderately expensive.. since we are tying up vet's connection..
                            vet.orgDisputePaths.setString(1,ctx.session.user.voterOrg());
                            vet.orgDisputePaths.setString(2,localeName.toString());
                            ResultSet rs = vet.orgDisputePaths.executeQuery();
                            while(rs.next()) {
                                int xp = rs.getInt(1);                               
                                String path = xpt.getById(xp);                               
                                String theMenu = PathUtilities.xpathToMenu(path);
                                if(theMenu != null) {
                                    odItems.add(theMenu);
                                }
                            }
                        } catch (SQLException se) {
                            throw new RuntimeException("SQL error listing OD results - " + SurveyMain.unchainSqlException(se));
                        }
                    }
                    WebContext subCtx = (WebContext)ctx.clone();
                    //subCtx.addQuery(QUERY_LOCALE,ctx.getLocale().toString());
                    subCtx.removeQuery(QUERY_SECTION);
                    for(String item : odItems) {
                        printMenu(subCtx, "", item);
                        subCtx.print(" | ");
                    }
                    ctx.println("<br>");
                }
            }
            
            
            if((UserRegistry.userIsVetter(ctx.session.user))&&((vetStatus & Vetting.RES_BAD_MASK)>0)) {
                //int numNoVotes = vet.countResultsByType(ctx.getLocale().toString(),Vetting.RES_NO_VOTES);
                int numInsufficient = vet.countResultsByType(ctx.getLocale(),Vetting.RES_INSUFFICIENT);
                int numDisputed = vet.countResultsByType(ctx.getLocale(),Vetting.RES_DISPUTED);
                int numErrors =  0;//vet.countResultsByType(ctx.getLocale().toString(),Vetting.RES_ERROR);
             
                Hashtable<String,Integer> insItems = new Hashtable<String,Integer>();
                Hashtable<String,Integer> disItems = new Hashtable<String,Integer>();

                synchronized(vet.conn) { 
                    try { // moderately expensive.. since we are tying up vet's connection..
                        ResultSet rs = vet.listBadResults(ctx.getLocale());
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
                    } catch (SQLException se) {
                        throw new RuntimeException("SQL error listing bad results - " + SurveyMain.unchainSqlException(se));
                    }
                    // et.tostring
                }
                
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
        
        ctx.println("<hr/><p><p>");
        ctx.println("<h3>Basic information about the Locale</h3>");
        
        // coverage level
        {
            org.unicode.cldr.test.CoverageLevel.Level itsLevel = 
                    StandardCodes.make().getLocaleCoverageLevel(ctx.getEffectiveLocaleType(ctx.getChosenLocaleType()), ctx.getLocale().toString()) ;
        
            String def = ctx.pref(SurveyMain.PREF_COVLEV,"default");
            if(def.equals("default")) {
                ctx.print("Coverage Level: <tt class='codebox'>"+itsLevel.toString()+"</tt><br>");
            } else {
                ctx.print("Coverage Level: <tt class='codebox'>"+def+"</tt>  (overriding <tt>"+itsLevel.toString()+"</tt>)<br>");
            }
            ctx.print("<ul><li>To change your coverage level, see ");
            printMenu(ctx, "", "options", "My Options", QUERY_DO);
            ctx.println("</li></ul>");
        }
    
        
        ctx.print("  <p><i><font size='+1' color='red'>Important Notes:</font></i></p>  <ul>    <li><font size='4'><i>W</i></font><i><font size='4'>"+
                    "hen you navigate away from any page, any     data changes you've made will be lost <b>unless</b> you hit the"+
                    " <b>"+getSaveButtonText()+"</b> button!</font></i></li>    <li><i><font size='4'>"+
                                SLOW_PAGE_NOTICE+
                    "</font></i></li>    <li><i><font size='4'>Be sure to read </font>    "+
    //                "<a href='http://www.unicode.org/cldr/wiki?SurveyToolHelp'>"
                    "<font size='4'>");
        ctx.println("<a href='"+GENERAL_HELP_URL+"'>"+GENERAL_HELP_NAME+"</a>"); // base help
        ctx.print("</font>"+
                    "<font size='4'>     once before going further.</font></i></li>   "+
                    " <!-- <li> <font size='4'><i>Consult the Page Instructions if you have questions on any page.</i></font> "+
                    "</li> --> </ul>");
        
        if(dbVer != null) {
            ctx.println( LDMLUtilities.getCVSLink(ctx.getLocale().toString(), dbVer) + "version #" + dbVer + "</a>");
//            if((diskVer != null)&&(!diskVer.equals(dbVer))) {
//                ctx.println( " " + LDMLUtilities.getCVSLink(ctx.getLocale().toString(), dbVer) + "(Note: version " + diskVer + " is available to the administrator.)</a>");
//            }
        }    
        ctx.println(SLOW_PAGE_NOTICE);
        if(ctx.session.user!=null) {
        	ctx.println("<br>");
        	ctx.println("<a href='"+ctx.jspLink("xpath.jsp")+"&_="+ctx.getLocale().toString()+"'>Go to XPath...</a><br>");
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
            gFactory = CLDRFile.SimpleFactory.make(fileBase,".*");
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
    
    HashMap<String,String> gBaselineHash = new HashMap<String,String>();

    /* Sentinel value indicating that there was no baseline string available. */
    private static final String NULL_STRING = "";

    public synchronized String baselineFileGetStringValue(String xpath) {
        String res = gBaselineHash.get(xpath);
        if(res == null) {
            res = getBaselineFile().getStringValue(xpath);
            if(res == null) {
                res = NULL_STRING;
            }
            gBaselineHash.put(xpath,res);
        }
        if(res == NULL_STRING) {
            return null;
        } else {
            return res;
        }
    }

    public synchronized ExampleGenerator getBaselineExample() {
        if(gBaselineExample == null) {
            gBaselineExample = new ExampleGenerator(getBaselineFile(), fileBase + "/../supplemental/");
        }
        gBaselineExample.setVerboseErrors(twidBool("ExampleGenerator.setVerboseErrors"));
        return gBaselineExample;
    }

    public synchronized String getHTMLDirectionFor(CLDRLocale locale) {
        String dir = getDirectionalityFor(locale);
        if(dir.equals("left-to-right")) {
            return "ltr";
        } else if(dir.equals("right-to-left")) {
            return "rtl";
        } else if(dir.equals("top-to-bottom")) {
            return "ltr"; // !
        } else {
            System.err.println("Could not get directionality for " + locale + "- got "+dir + " - assuming ltr");
            return "ltr";
        }
//        // TODO: use orientation.
//        String locStr = locale.toString();
//        String script = locale.getScript();
//        if(locStr.startsWith("ps") ||
//           locStr.startsWith("fa") ||
//           locStr.startsWith("ar") ||
//           locStr.startsWith("syr") ||
//           locStr.startsWith("he") ||
//           "Arab".equals(script)) {
//            return "rtl";
//        } else {
//            return "ltr";
//        }
    }
    
    public synchronized String getDirectionalityFor(CLDRLocale id) {
        final boolean DDEBUG=false;
        if (DDEBUG) System.err.println("Checking directionality for " + id);
        if(aliasMap==null) {
            checkAllLocales();
        }
        while(id != null) {
            // TODO use iterator
            CLDRLocale aliasTo = isLocaleAliased(id);
            if (DDEBUG) System.err.println("Alias -> "+aliasTo);
            if(aliasTo != null 
                    && !aliasTo.equals(id)) { // prevent loops
                id = aliasTo;
                if (DDEBUG) System.err.println(" -> "+id);
                continue;
            }
            String dir = directionMap.get(id);
            if (DDEBUG) System.err.println(" dir:"+dir);
            if(dir!=null) {
                return dir;
            }
            id = id.getParent();
            if (DDEBUG) System.err.println(" .. -> :"+id);
        }
        if (DDEBUG) System.err.println("err: could not get directionality of root");
        return "left-to-right"; //fallback
    }

    
    public Map basicOptionsMap() {
        Map options = new HashMap();
        
        // the following is highly suspicious. But, CheckCoverage seems to require it.
        options.put("submission", "true");
        options.put("CheckCoverage.requiredLevel", "minimal");
        
        // pass in the current ST phase
        if(isPhaseVetting() || isPhaseVettingClosed()) {
            options.put("phase", "vetting");
        } else if(isPhaseSubmit()) {
            options.put("phase", "submission");
        } else if(isPhaseFinalTesting()) {
            options.put("phase", "final_testing");
        }
        
        return options;
    }

    public CheckCLDR createCheck() {
            CheckCLDR checkCldr;
            
//                logger.info("Initting tests . . . - "+ctx.getLocale()+"|" + ( CHECKCLDR+":"+ctx.defaultPtype()) + "@"+ctx.session.id);
//            long t0 = System.currentTimeMillis();
            
            // make sure CLDR has the latest display information.
            //if(phaseVetting) {
            //    checkCldr = CheckCLDR.getCheckAll("(?!.*(DisplayCollisions|CheckCoverage).*).*" /*  ".*" */);
            //} else {
            checkCldr = CheckCLDR.getCheckAll(".*");
//                checkCldr = CheckCLDR.getCheckAll("(?!.*DisplayCollisions.*).*" /*  ".*" */);
            //}

            checkCldr.setDisplayInformation(getBaselineFile());
            
            return checkCldr;
    }

    public CheckCLDR createCheckWithoutCollisions() {
            CheckCLDR checkCldr;
            
//                logger.info("Initting tests . . . - "+ctx.getLocale()+"|" + ( CHECKCLDR+":"+ctx.defaultPtype()) + "@"+ctx.session.id);
//            long t0 = System.currentTimeMillis();
            
            // make sure CLDR has the latest display information.
            //if(phaseVetting) {
            //    checkCldr = CheckCLDR.getCheckAll("(?!.*(DisplayCollisions|CheckCoverage).*).*" /*  ".*" */);
            //} else {
            if(false) {  // show ALL ?
                checkCldr = CheckCLDR.getCheckAll(".*");
            } else {
                checkCldr = CheckCLDR.getCheckAll("(?!.*DisplayCollisions.*).*" /*  ".*" */);
            }

            checkCldr.setDisplayInformation(getBaselineFile());
            
            return checkCldr;
    }

    public static boolean CACHE_VXML_FOR_TESTS = false;
    public static boolean CACHE_VXML_FOR_EXAMPLES = false;

    /**
     * Any user of this should be within session sync.
     * @author srl
     *
     */
    public class UserLocaleStuff extends Registerable {
        public CLDRFile cldrfile = null;
        private CLDRFile cachedCldrFile = null; /* If not null: use this for tests. Readonly. */
        public XMLSource dbSource = null;
        public Hashtable hash = new Hashtable();
        private ExampleGenerator exampleGenerator = null;
        private Registerable exampleIsValid = new Registerable(lcr, locale);
        
        public ExampleGenerator getExampleGenerator() {
            if(exampleGenerator==null || !exampleIsValid.isValid() ) {
                CLDRFile fileForGenerator = null;
                if(CACHE_VXML_FOR_EXAMPLES) {
                    fileForGenerator = getCLDRFileCache().getCLDRFile(locale, true);
                } else {
                 //   System.err.println("!CACHE_VXML_FOR_EXAMPLES");
                    fileForGenerator = new CLDRFile(dbSource,true);
                }
                exampleGenerator = new ExampleGenerator(fileForGenerator, fileBase + "/../supplemental/");
                exampleGenerator.setVerboseErrors(twidBool("ExampleGenerator.setVerboseErrors"));
                //System.err.println("-revalid exgen-"+locale + " - " + exampleIsValid + " in " + this);
                exampleIsValid.setValid();
                //System.err.println(" >> "+locale + " - " + exampleIsValid + " in " + this);
                exampleIsValid.register();
                //System.err.println(" >>> "+locale + " - " + exampleIsValid + " in " + this);
            }
            return exampleGenerator;
        }
        
        public UserLocaleStuff(CLDRLocale locale) {
            super(lcr, locale);
            exampleIsValid.register();
//    System.err.println("Adding ULS:"+locale);
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
            if (checkCldr == null)  {
                List checkCldrResult = new ArrayList();
                
                checkCldr = createCheck();
                
                CLDRFile fileForChecks = null;
                
                if(cachedCldrFile!=null) {
                    fileForChecks = cachedCldrFile;
                } else {
                    fileForChecks = cldrfile;
                }
                
                //cldrfile.write(new PrintWriter(System.out));
                
                if(cldrfile==null) {
                    throw new InternalError("cldrfile was null.");
                }
                //long t0 = System.currentTimeMillis();
                checkCldr.setCldrFileToCheck(fileForChecks, ctx.getOptionsMap(basicOptionsMap()), checkCldrResult);
             //   logger.info("fileToCheck set . . . on "+ checkCldr.toString());
                hash.put(CHECKCLDR+ctx.defaultPtype(), checkCldr);
                if(!checkCldrResult.isEmpty()) {
                    hash.put(CHECKCLDR_RES+ctx.defaultPtype(), checkCldrResult);
                }
                //long t2 = System.currentTimeMillis();
                //logger.info("Time to init tests: " + (t2-t0));
            }
            return checkCldr;
        }
        
        
        CLDRFile makeCachedCLDRFile(XMLSource dbSource) {
            if(CACHE_VXML_FOR_TESTS) {
                return getCLDRFileCache().getCLDRFile(locale());
            } else {
//                System.err.println(" !CACHE_VXML_FOR_TESTS");
                return null;
            }
        }

        /**
         * 
         * @param ctx
         * @param user
         * @param locale
         */
        public void complete(WebContext ctx, User user, CLDRLocale locale) {
            // TODO: refactor.
            UserLocaleStuff uf = this;
            if(uf.cldrfile == null) {
                uf.dbSource = makeDBSource(ctx, user, locale); // use context's connection.
                uf.cldrfile = makeCLDRFile(uf.dbSource);
                uf.cachedCldrFile = uf.makeCachedCLDRFile(uf.dbSource);
            }
        }
    };

    /**
     * Any user of this should be within session sync
     * @param ctx
     * @return
     */
    UserLocaleStuff getOldUserFile(WebContext ctx) {
        UserLocaleStuff uf = (UserLocaleStuff)ctx.getByLocale(USER_FILE_KEY);
        return uf;
    }

    //private CLDRFileCache cldrFileCache = null; // LOCAL to UserFile.
    
    synchronized CLDRFileCache getCLDRFileCache() {
//        if(cldrFileCache == null) {
//        
//            Connection conn = getDBConnection();
//            XMLSource dbSource = makeDBSource(conn, null, CLDRLocale.ROOT, false);
//            XMLSource dbSourceV = makeDBSource(conn, null, CLDRLocale.ROOT, true);
//            cldrFileCache = new CLDRFileCache(dbSource, dbSourceV, new File(homeFile, "vxpt"), this);
//        }
//        return null;
        throw new InternalError("getCLDRFileCache: not imp");
//        return dbsrcfac.getCLDRFileCache();
    }

    /**
     * Any user of this should be within session sync (ctx.session)
     * @param ctx
     * @param user
     * @param locale
     * @return
     */
    public UserLocaleStuff getUserFile(WebContext ctx, UserRegistry.User user, CLDRLocale locale) {
        // has this locale been invalidated?
        UserLocaleStuff uf = getOldUserFile(ctx);
        //UserLocaleStuff uf = null;
        if(uf == null) {
            uf = new UserLocaleStuff(locale);
            ctx.putByLocale(USER_FILE_KEY, uf);
            uf.register(); // register with lcr
        } else if(!uf.isValid()) {
    //        System.err.println("Invalid, clearing: "+ uf.toString());
            uf.clear();
            uf.register(); // reregister
        }
        
        uf.complete(ctx, user, locale);
        int n = dbsrcfac.update();
        if(n>0) System.err.println("getUserFile() updated " + n + " locales.");
        return uf;
    }
    XMLSource makeDBSource(WebContext ctx, UserRegistry.User user, CLDRLocale locale) {
        return makeDBSource(ctx.session.db(this), user, locale);
    }
    XMLSource makeDBSource(WebContext ctx, UserRegistry.User user, CLDRLocale locale, boolean finalData) {
        return makeDBSource(ctx.session.db(this), user, locale, finalData);
    }
    XMLSource makeDBSource(Connection conn, UserRegistry.User user, CLDRLocale locale) {
        XMLSource dbSource = dbsrcfac.getInstance(locale);
        return dbSource;
    }
    /**
     * @deprecated  drop the conn/user
     * @param conn
     * @param user
     * @param locale
     * @param finalData
     * @return
     */
    XMLSource makeDBSource(Connection conn, UserRegistry.User user, CLDRLocale locale, boolean finalData) {
        XMLSource dbSource = dbsrcfac.getInstance(locale, finalData);
        return dbSource;
    }
    XMLSource makeDBSource(CLDRLocale locale, boolean finalData) {
        XMLSource dbSource = dbsrcfac.getInstance(locale, finalData);
        return dbSource;
    }
    static CLDRFile makeCLDRFile(XMLSource dbSource) {
        return new CLDRFile(dbSource,false);
    }

    /**
     * reset the "list of locales".  
     * call this if you're resetting the in-db view of what's on disk.  
     * it will reset things like the locale list map, the metazone map, the interest map, ...
     */
    private synchronized void resetLocaleCaches() {
        localeTree = null;
        allMetazones = null;
        metazoneContinentMap = null;
        localeListSet = null;
        aliasMap = null;
        gBaselineFile=null;
        gBaselineHash=null;
        try {
         this.fora.reloadLocales();
        } catch(SQLException se) {
        	System.err.println("On resetLocaleCaches().reloadLocales: " + this.unchainSqlException(se));
        	this.busted("trying to reset locale caches @ fora", se);
        }
    }
    
    
    private static Hashtable<CLDRLocale,CLDRLocale> aliasMap = null;
    private static Hashtable<CLDRLocale,String> directionMap = null;
    
    /**
     * "Hash" a file to a string, including mod time and size
     * @param f
     * @return
     */
    private static String fileHash(File f) {
        return("["+f.getAbsolutePath()+"|"+f.length()+"|"+f.hashCode()+"|"+f.lastModified()+"]");
    }

    private synchronized void checkAllLocales() {
        if(aliasMap!=null) return;
        
        boolean useCache = isUnofficial; // NB: do NOT use the cache if we are in unofficial mode.  Parsing here doesn't take very long (about 16s), but 
        // we want to save some time during development iterations.
        
        Hashtable<CLDRLocale,CLDRLocale> aliasMapNew = new Hashtable<CLDRLocale,CLDRLocale>();
        Hashtable<CLDRLocale,String> directionMapNew = new Hashtable<CLDRLocale,String>();
        Set<CLDRLocale> locales  = getLocalesSet();
        ElapsedTimer et = new ElapsedTimer();
        setProgress("Parse locales from XML", locales.size());
        int nn=0;
        File xmlCache = new File(vetdir, XML_CACHE_PROPERTIES);
        File xmlCacheBack = new File(vetdir, XML_CACHE_PROPERTIES+".backup");
        Properties xmlCacheProps = new java.util.Properties(); 
        Properties xmlCachePropsNew = new java.util.Properties(); 
        if(useCache && xmlCache.exists()) try {
            java.io.FileInputStream is = new java.io.FileInputStream(xmlCache);
            xmlCacheProps.load(is);
            is.close();
        } catch(java.io.IOException ioe) {
            /*throw new UnavailableException*/
            logger.log(java.util.logging.Level.SEVERE, "Couldn't load cldr.properties file from '" + cldrHome + "/cldr.properties': ",ioe);
            busted("Couldn't load cldr.properties file from '" + cldrHome + "/cldr.properties': ", ioe);
            return;
        }
        
        int n=0;
        int cachehit=0;
        System.err.println("Parse " + locales.size() + " locales from XML to look for aliases or errors...");
        for(CLDRLocale loc : locales) {
            String locString = loc.toString();
//            ULocale uloc = new ULocale(locString);
            updateProgress(n++, loc.toString() /* + " - " + uloc.getDisplayName(uloc) */);
            try {
                File  f = new File(fileBase, loc.toString()+".xml");
//                String fileName = fileBase+"/"+loc.toString()+".xml";
                String fileHash = fileHash(f);
                String aliasTo = null;
                String direction = null;
                //System.err.println(fileHash);
                
                String oldHash = xmlCacheProps.getProperty(locString);
                if(useCache && oldHash != null && oldHash.equals(fileHash)) {
                    // cache hit! load from cache
                    aliasTo = xmlCacheProps.getProperty(locString+".a",null);
                    direction = xmlCacheProps.getProperty(locString+".d",null);
                    cachehit++;
                } else {
                    Document d = LDMLUtilities.parse(f.getAbsolutePath(), false);
                    
                    // look for directionality
                    Node[] directionalityItems = 
                        LDMLUtilities.getNodeListAsArray(d,"//ldml/layout/orientation");
                    if(directionalityItems!=null&&directionalityItems.length>0) {
                        direction = LDMLUtilities.getAttributeValue(directionalityItems[0], LDMLConstants.CHARACTERS);
                        if(direction != null&& direction.length()>0) {
                        } else {
                            direction = null;
                        }
                    }
                    
                    
                    Node[] aliasItems = 
                                LDMLUtilities.getNodeListAsArray(d,"//ldml/alias");
                                
                    if((aliasItems==null) || (aliasItems.length==0)) {
                        aliasTo=null;
                    } else if(aliasItems.length>1) {
                        throw new InternalError("found " + aliasItems + " items at " + "//ldml/alias" + " - should have only found 1");
                    } else {
                        aliasTo = LDMLUtilities.getAttributeValue(aliasItems[0],"source");
                    }
                }
                
                // now, set it into the new map
                xmlCachePropsNew.put(locString, fileHash);
                if(direction != null) {
                    directionMapNew.put((loc), direction);
                    xmlCachePropsNew.put(locString+".d", direction);
                }
                if(aliasTo!=null) {
                    aliasMapNew.put((loc),CLDRLocale.getInstance(aliasTo));
                    xmlCachePropsNew.put(locString+".a", aliasTo);
                }
            } catch (Throwable t) {
                System.err.println("isLocaleAliased: Failed load/validate on: " + loc + " - " + t.toString());
                t.printStackTrace();
                busted("isLocaleAliased: Failed load/validate on: " + loc + " - ", t);
                throw new InternalError("isLocaleAliased: Failed load/validate on: " + loc + " - " + t.toString());
            }
        }
        
        if(useCache) try {
            // delete old stuff
            if(xmlCacheBack.exists()) { 
                xmlCacheBack.delete();
            }
            if(xmlCache.exists()) {
                xmlCache.renameTo(xmlCacheBack);
            }
            java.io.FileOutputStream os = new java.io.FileOutputStream(xmlCache);
            xmlCachePropsNew.store(os, "YOU MAY DELETE THIS CACHE. Cache updated at " + new Date());
            updateProgress(nn++, "Loading configuration..");
            os.close();
        } catch(java.io.IOException ioe) {
            /*throw new UnavailableException*/
            logger.log(java.util.logging.Level.SEVERE, "Couldn't write "+xmlCache+" file from '" + cldrHome + "': ",ioe);
            busted("Couldn't write "+xmlCache+" file from '" + cldrHome+"': ", ioe);
            return;
        }
        
        System.err.println("Finished verify+alias check of " + locales.size()+ ", " + aliasMapNew.size() + " aliased locales ("+cachehit+" in cache) found in " + et.toString());
        aliasMap = aliasMapNew;
        directionMap = directionMapNew;
        clearProgress();
    }
    
    /**
     * Is this locale fully aliased? If true, returns what it is aliased to.
     */
    public synchronized CLDRLocale isLocaleAliased(CLDRLocale id) {
        if(aliasMap==null) {
            checkAllLocales();
        }
        return aliasMap.get(id);
    }

//    public ULocale isLocaleAliased(ULocale id) {
//        String aliasTo = isLocaleAliased(id.getBaseName()).toString();
//        if(aliasTo!=null) {
//            return new ULocale(aliasTo);
//        } else {
//            return null;
//        }
//    }

    /**
     * Master list of metazones. Don't access this directly.
     */
    private static Set allMetazones = null;
    public static Map<String,String> metazoneContinentMap = null;

    /**
     * Maintain a master list of metazones, culled from root.
     */
    public synchronized Set getMetazones() {
        if(allMetazones == null) {
            ElapsedTimer et = new ElapsedTimer();    
            XPathParts parts = new XPathParts(null,null);
            Set aset = new TreeSet();
            CLDRFile mySupp = getFactory().make("supplementalData",false);
            for(Iterator i = mySupp.iterator("//supplementalData/timezoneData/mapTimezones[@type=\"metazones\"]/mapZone");i.hasNext();) {
                String xpath = i.next().toString();
                parts.set(xpath);
                String mzone = parts.getAttributeValue(-1,"other");
                if(mzone != null) {
                    aset.add(mzone);
                }
            }
            allMetazones = aset;
            System.err.println("sm.getMetazones:" + allMetazones.size()+" metazones found in " + et.toString());
        }
        return allMetazones;
    }

    public Set getMetazones(String subclass) {
        ElapsedTimer et = new ElapsedTimer();    
        XPathParts parts = new XPathParts(null,null);
        Set aset = new TreeSet();
        CLDRFile mySupp = getFactory().make("supplementalData",false);
        for(Iterator i = mySupp.iterator("//supplementalData/timezoneData/mapTimezones[@type=\"metazones\"]/mapZone");i.hasNext();) {
            String xpath = i.next().toString();
            parts.set(xpath);
            String mzone = parts.getAttributeValue(-1,"other");
            String type = parts.getAttributeValue(-1,"type");
            String territory = parts.getAttributeValue(-1,"territory");
            if(mzone != null && territory.equals("001") && type.startsWith(subclass)) {
                aset.add(mzone);
            }
        }
        return aset;
    }

    public String getMetazoneContinent(String xpath) {
       XPathParts parts = new XPathParts(null,null);
       // Build the metazone -> continent map from supplemental & cache it.
       if ( metazoneContinentMap == null ) {
           metazoneContinentMap = new HashMap<String,String>();
           CLDRFile mySupp = getFactory().make("supplementalData",false);
           for(Iterator i = mySupp.iterator("//supplementalData/timezoneData/mapTimezones[@type=\"metazones\"]/mapZone");i.hasNext();) {
               String suppxpath = i.next().toString();
               parts.set(suppxpath);
               String mzone = parts.getAttributeValue(-1,"other");
               String type = parts.getAttributeValue(-1,"type");
               String territory = parts.getAttributeValue(-1,"territory");
               if(mzone != null && territory.equals("001")) {
                  metazoneContinentMap.put(mzone,type.substring(0,type.indexOf("/")));
               }
           }
        }

       parts.set(xpath);
       String thisMetazone = parts.getAttributeValue(3,"type");
       return metazoneContinentMap.get(thisMetazone);
    }
    /**
    * show the webpage for one of the 'locale codes' items.. 
     * @param ctx the web context
     * @param xpath xpath to the root of the structure
     * @param tx the texter to use for presentation of the items
     * @param fullSet the set of tags denoting the expected full-set, or null if none.
     */
    public void showLocaleCodeList(WebContext ctx, String which) {
        showPathList(ctx, PathUtilities.LOCALEDISPLAYNAMES+which, typeToSubtype(which), true /* simple */);
    }

    public void showPathListExample(WebContext ctx, String xpath, String lastElement,
            String e, String fullThing, CLDRFile cf) {
        DataSection oldSection =  ctx.getExistingSection(fullThing);
        DataSection.ExampleEntry ee = null;
        if(oldSection != null) {
            ee = oldSection.getExampleEntry(e); // retrieve the info out of the hash.. 
        }
        if(ee != null) synchronized (ctx.session) {
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
            Map<String,String> m = new TreeMap<String,String>();
            for(Iterator i = mapOfArrays.keySet().iterator();i.hasNext();) {
                String k = i.next().toString();
    //            String[] v = (String[])mapOfArrays.get(k);  // We dont care about the broken, undecoded contents here..
                m.put(k,ctx.field(k,null)); //  .. use our vastly improved field() function
            }
            
            if(d != null) {
            
                try {
                    String path = ee.section.xpath(ee.dataRow);
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


    public void showMetazones(WebContext ctx, String continent) {
        showPathList(ctx, "//ldml/dates/timeZoneNames/metazone_"+continent,null);
    }

    /**
     * parse the metazone list for this zone.
     * Map will contain enries with key String and value  String[3] of the form:
     *    from : { from, to, metazone }
     * for example:
     *    "1970-01-01" :  { "1970-01-01", "1985-03-08", "Australia_Central" }
     * the 'to' will be null if it does not have an ending time.
     * @param metaMap  an 'out' parameter which will be cleared, and populated with the contents of the metazone
     * @return the active metazone ( where to=null ) if any, or null
     */
    public String zoneToMetaZone(WebContext ctx, String zone, Map metaMap) {
        SurveyMain sm = this;
       // String returnZone = null;
        String current = null;
        XPathParts parts = new XPathParts(null, null);
        synchronized(ctx.session) { // TODO: redundant sync?
            SurveyMain.UserLocaleStuff uf = sm.getUserFile(ctx, (ctx.session.user==null)?null:ctx.session.user, ctx.getLocale());
            //CLDRFile cf = uf.cldrfile;
            CLDRFile resolvedFile = new CLDRFile(uf.dbSource,true);
            //CLDRFile engFile = ctx.sm.getBaselineFile();
    
            String xpath =  "//ldml/"+"dates/timeZoneNames/zone";
            String ourSuffix = "[@type=\""+zone+"\"]";
            //String base_xpath = xpath+ourSuffix;
            String podBase = DataSection.xpathToSectionBase(xpath);
            
            metaMap.clear();
    
            Iterator mzit = resolvedFile.iterator(podBase+ourSuffix+"/usesMetazone");
                 
            for(;mzit.hasNext();) {
                String ameta = (String)mzit.next();
                String mfullPath = resolvedFile.getFullXPath(ameta);
                parts.set(mfullPath);
                String mzone = parts.getAttributeValue(-1,"mzone");
                String from = parts.getAttributeValue(-1,"from");
                if(from==null) {
                    from = METAZONE_EPOCH;
                }
                String to = parts.getAttributeValue(-1,"to");
                String contents[] = { from, to, mzone };
                metaMap.put(from,contents);
                
                if(to==null) {
                    current = mzone;
                }
            }
        }
        return current;
    }


    /**
     * for showing the list of zones to the user
     */

    public void showTimeZones(WebContext ctx) {
        String zone = ctx.field(QUERY_ZONE);

        try {
            ctx.println("<div style='float: right'>TZ Version:"+supplemental.getOlsonVersion()+"</div>");
        } catch(Throwable t) {
            ctx.println("<div style='float: right' class='warning'>TZ Version: "+ t.toString()+"</div>");
        }

        // simple case - show the list of zones.
        if((zone == null)||(zone.length()==0)) {
            showPathList(ctx, DataSection.EXEMPLAR_ONLY, null);
            return;
        }

        boolean canModify = (UserRegistry.userCanModifyLocale(ctx.session.user,ctx.getLocale()));
        
        WebContext subCtx = (WebContext)ctx.clone();
        subCtx.setQuery(QUERY_ZONE, zone);
        
        Map metaMap = new TreeMap();
        
        String currentMetaZone = zoneToMetaZone(ctx, zone, metaMap);

        String territory = (String)(supplemental.getZoneToTerritory().get(zone));
        String displayTerritory = null;
        String displayZone = null;
        if((territory != null) && (territory.length()>0)) {
            displayTerritory = new ULocale("und_"+territory).getDisplayCountry(BASELINE_LOCALE);
            if((displayTerritory == null)||(displayTerritory.length()==0)) {
                displayTerritory = territory;
            }
            displayZone = displayTerritory + " : <a class='selected'>"+ zone + "</a>";
        } else {
            displayZone = zone;
        }

        ctx.println("<h2>"+ displayZone + "</h2>"); // couldn't find the territory.


        printPathListOpen(ctx);
        ctx.println("<input type='hidden' name='_' value='"+ctx.getLocale().toString()+"'>");
        ctx.println("<input type='hidden' name='x' value='timezones'>");
        ctx.println("<input type='hidden' name='zone' value='"+zone+"'>");
        if(canModify) { 
            ctx.println("<input  type='submit' value='" + getSaveButtonText() + "'>"); // style='float:right'
        }
        
        String zonePodXpath =  "//ldml/"+"dates/timeZoneNames/zone";
        String zoneSuffix = "[@type=\""+zone+"\"]";
        String zoneXpath = zonePodXpath+zoneSuffix;
        String podBase = DataSection.xpathToSectionBase(zonePodXpath);

        
        String metazonePodXpath =  "//ldml/"+"dates/timeZoneNames/metazone";
        String metazoneSuffix = "[@type=\""+currentMetaZone+"\"]";
        String metazoneXpath = metazonePodXpath+metazoneSuffix;
        String metazonePodBase = DataSection.xpathToSectionBase(metazonePodXpath);

        if(canModify) {
            vet.processPodChanges(ctx, podBase);
            if(currentMetaZone != null) {
                vet.processPodChanges(ctx, metazonePodBase);
            }
        }
        
        DataSection section = ctx.getSection(podBase);
        DataSection metazoneSection = null;
        
        if(currentMetaZone != null) {
            metazoneSection = ctx.getSection(metazonePodBase);
        }

        
        // #1 exemplar city
        
        ctx.println("<h3>Exemplar City</h3>");
        
        printSectionTableOpen(ctx, section, true);
        showPeas(ctx, section, canModify, zoneXpath+"/exemplarCity", true);
        printSectionTableClose(ctx, section);
        
        ctx.printHelpHtml(zoneXpath+"/exemplarCity");

        if(currentMetaZone != null) {
            // #2 there's a MZ active. Explain it.
            ctx.println("<hr><h3>Metazone "+currentMetaZone+"</h3>");

            printSectionTableOpen(ctx, metazoneSection, true);
            showPeas(ctx, metazoneSection, canModify, metazoneXpath, true);
            printSectionTableClose(ctx, metazoneSection);
            if(canModify) {
                ctx.println("<input  type='submit' value='" + getSaveButtonText() + "'>"); // style='float:right'
            }
            
            ctx.printHelpHtml(metazoneXpath);

            // show the table of active zones
            ctx.println("<h4>Metazone History</h4>");
            ctx.println("<table class='tzbox'>");
            ctx.println("<tr><th>from</th><th>to</th><th>Metazone</th></tr>");
            int n = 0;
            for(Iterator it = metaMap.entrySet().iterator();it.hasNext();) {
                n++;
                Map.Entry e = (Map.Entry)it.next();
                String contents[] = (String[])e.getValue();
                String from = contents[0];
                String to = contents[1];
                String mzone = contents[2];
                // OK, now that we are unpacked..
                //mzContext.setQuery("mzone",mzone);
                String mzClass="";
                if(mzone.equals(currentMetaZone)) {
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
                
                ctx.print("<span class='"+(mzone.equals(currentMetaZone)?"selected":"notselected")+"' '>"); // href='"+mzContext.url()+"
                ctx.print(mzone);
                ctx.println("</a>");
                ctx.println("</td></tr>");
                
            }
            ctx.println("</table>");

            ctx.println("<h3>"+ displayZone + " Overrides</h3>"); // couldn't find the territory.
            ctx.print("<i>The Metazone <b>"+currentMetaZone+"</b> is active for this zone. " +
                    //<a href=\""+
                //bugReplyUrl(BUG_METAZONE_FOLDER, BUG_METAZONE, ctx.getLocale()+":"+ zone + ":" + currentMetaZone + " incorrect")+
                //"\">Click Here to report Metazone problems</a>"+
                " Please report any Metazone problems. </i>");
        } else {
            ctx.println("<h3>Zone Contents</h3>"); // No metazone - this is just the contents.
        }
                
        printSectionTableOpen(ctx, section, true);
        showPeas(ctx, section, canModify, zoneXpath+DataSection.EXEMPLAR_EXCLUDE, true);
        printSectionTableClose(ctx, section);
        if(canModify) {
            ctx.println("<input  type='submit' value='" + getSaveButtonText() + "'>"); // style='float:right'
        }
        
        ctx.printHelpHtml(zoneXpath);
        
        printPathListClose(ctx);
    }
        

    /**
    * This is the main function for showing lists of items (pods).
     */
    public void showPathList(WebContext ctx, String xpath, String lastElement) {
        showPathList(ctx,xpath,lastElement,false);
    }

    /**
     * Holds session lock
     * @param ctx
     * @param xpath
     * @param lastElement
     * @param simple
     */
    public void showPathList(WebContext ctx, String xpath, String lastElement, boolean simple) {
        /* all simple */
        simple = true;
        
        synchronized(ctx.session) {
            UserLocaleStuff uf = getUserFile(ctx, ctx.session.user, ctx.getLocale());
            XMLSource ourSrc = uf.dbSource;
            CLDRFile cf =  uf.cldrfile;
            String fullThing = xpath + "/" + lastElement;
        //    boolean isTz = xpath.equals("timeZoneNames");
            if(lastElement == null) {
                fullThing = xpath;
            }    
            boolean exemplar_only = false;
            if(fullThing.equals(DataSection.EXEMPLAR_ONLY)) {
                fullThing = DataSection.EXEMPLAR_PARENT;
                exemplar_only=true;
            }
                
            boolean canModify = (UserRegistry.userCanModifyLocale(ctx.session.user,ctx.getLocale()));
            
            {
                // TODO: move this into showExample. . .
                String e = ctx.field(QUERY_EXAMPLE);
                if(e.length() > 0) {
                    showPathListExample(ctx, xpath, lastElement, e, fullThing, cf);
                } else {
                    // first, do submissions.
                    if(canModify) {
                        DataSection oldSection = ctx.getExistingSection(fullThing);
                        if(processPeaChanges(ctx, oldSection, cf, ourSrc, new DefaultDataSubmissionResultHandler(ctx))) {
                            int j = vet.updateResults(oldSection.locale); // bach 'em
                            int d = this.dbsrcfac.update(); // then the fac so it can update
                            System.err.println("sm:ppc:dbsrcfac: "+d+" deferred updates done.");
                            ctx.println("<br> You submitted data or vote changes, <!-- and " + j + " results were updated. As a result, --> your items may show up under the 'priority' or 'proposed' categories.<br>");
                        }
                    }
            //        System.out.println("Pod's full thing: " + fullThing);
                    DataSection section = ctx.getSection(fullThing); // we load a new pod here - may be invalid by the modifications above.
//                    section.simple=simple;
                    if(exemplar_only) {
                        showPeas(ctx, section, canModify, DataSection.EXEMPLAR_ONLY, false);
                    } else {
                        showPeas(ctx, section, canModify);
                    }
                }
            }
        }
    }

    String getSortMode(WebContext ctx, DataSection section) {
        return getSortMode(ctx, section.xpathPrefix);
    }

    String getSortMode(WebContext ctx, String prefix) {
        String sortMode = null;
        if(prefix.indexOf("timeZone")!=-1 ||
            prefix.indexOf("localeDisplayName")!=-1) {
            sortMode = ctx.pref(PREF_SORTMODE, false&&(isPhaseVetting() || isPhaseVettingClosed())?PREF_SORTMODE_WARNING:PREF_SORTMODE_DEFAULT);
        } else {
            sortMode = ctx.pref(PREF_SORTMODE, false&&(isPhaseVetting() || isPhaseVettingClosed())?PREF_SORTMODE_WARNING:/*PREF_SORTMODE_CODE*/PREF_SORTMODE_WARNING); // all the rest get Code Mode
        }
        return sortMode;
    }

    static int PODTABLE_WIDTH = 13; /** width, in columns, of the typical data table **/

    static void printSectionTableOpen(WebContext ctx, DataSection section, boolean zoomedIn) {
        ctx.println("<a name='st_data'></a>");
        ctx.println("<table summary='Data Items for "+ctx.getLocale().toString()+" " + section.xpathPrefix + "' class='data' border='0'>");

        if(/* !zoomedIn */ true) {
            ctx.println("<tr><td colspan='"+PODTABLE_WIDTH+"'>");
            // dataitems_header.jspf
            // some context
            ctx.put(WebContext.DATA_SECTION, section);
            ctx.put(WebContext.ZOOMED_IN, new Boolean(zoomedIn));
            ctx.includeFragment("dataitems_header.jsp");
            ctx.println("</td></tr>");
        }
        if(!section.xpathPrefix.equals("//ldml/references")) {
            ctx.println("<tr class='headingb'>\n"+
                        " <th>St.</th>\n"+                  // 1
                        " <th>Code</th>\n"+                 // 2
                        " <th colspan='1'>"+BASELINE_NAME+"</th>\n"+              // 3
                        " <th title='"+BASELINE_NAME+" Example'><i>Ex</i></th>\n" + 
                        " <th colspan='2'>"+getProposedName()+"</th>\n"+ // 7
                        " <th title='Proposed Example'><i>Ex</i></th>\n" + 
                        " <th colspan='2'>"+CURRENT_NAME+"</th>\n"+  // 6
                        " <th title='Current Example'><i>Ex</i></th>\n" + 
                        " <th colspan='2' width='20%'>Change</th>\n");  // 8
        } else {
            ctx.println("<tr class='headingb'>\n"+
                        " <th>St.</th>\n"+                  // 1
                        " <th>Code</th>\n"+                 // 2
                        " <th colspan='2'>"+"URI"+"</th>\n"+              // 3
                        " <th colspan='3'>"+getProposedName()+"</th>\n"+ // 7
                        " <th colspan='3'>"+CURRENT_NAME+"</th>\n"+  // 6
                        " <th colspan='2' width='20%'>Change</th>\n");  // 8
        }
        ctx.println( "<th width='1%' title='No Opinion'>n/o</th>\n"+                   // 5
                    "</tr>");
        if(zoomedIn) {
            List<String> refsList = new ArrayList<String>();
            ctx.temporaryStuff.put("references", refsList);
        }
    }

    static void printSectionTableOpenShort(WebContext ctx, DataSection section) {
        ctx.println("<a name='st_data'></a>");
        ctx.println("<table summary='Data Items for "+ctx.getLocale().toString()+" " + section.xpathPrefix + "' class='data' border='1'>");
            ctx.println("<tr class='headingb'>\n"+
                        " <th colspan='1' width='50%'>"+BASELINE_NAME+"</th>\n"+              // 3
                        " <th colspan='2' width='50%'>Your Language</th>\n");  // 8

        ctx.println("</tr>");
    }

    void printSectionTableClose(WebContext ctx, DataSection section) {
        List<String> refsList = (List<String>) ctx.temporaryStuff.get("references");
        if((refsList != null) && (!refsList.isEmpty())) {
            ctx.println("<tr></tr>");
            ctx.println("<tr class='heading'><th class='partsection' align='left' colspan='"+PODTABLE_WIDTH+"'>References</th></tr>");
            int n = 0;
            
            Hashtable<String,DataSection.DataRow> refsHash = (Hashtable<String, DataSection.DataRow>)ctx.temporaryStuff.get("refsHash");
            Hashtable<String,DataSection.DataRow.CandidateItem> refsItemHash = (Hashtable<String, DataSection.DataRow.CandidateItem>)ctx.temporaryStuff.get("refsItemHash");
            
            for(String ref: refsList) {
                n++;
                ctx.println("<tr class='referenceRow'><th><img src='http://unicode.org/cldr/data/dropbox/misc/images/reference.jpg'>#"+n+"</th>");
                ctx.println("<td colspan='"+1+"'>"+ref+"</td>");
                ctx.print("<td colspan='"+(PODTABLE_WIDTH-2)+"'>");
                if(refsHash != null) {
                    DataSection.DataRow refDataRow = refsHash.get(ref);
                    DataSection.DataRow.CandidateItem refDataRowItem = refsItemHash.get(ref);
                    if((refDataRowItem != null)&&(refDataRow!=null)) {
                        ctx.print(refDataRowItem.value);
                        if(refDataRow.displayName != null) {
                            ctx.println("<br>"+refDataRow.displayName);
                        }
                        //ctx.print(refPea.displayName);
                    } else {
                        ctx.print("<i>unknown reference</i>");
                    }
                }
                ctx.print("</td>");
                ctx.println("</tr>");
            }
            
        }
        if(refsList != null) {
            ctx.temporaryStuff.remove("references");
            ctx.temporaryStuff.remove("refsHash");
            ctx.temporaryStuff.remove("refsItemHash");
        }
        
        ctx.println("</table>");
    }

    void showPeas(WebContext ctx, DataSection section, boolean canModify) { 
        showPeas(ctx, section, canModify, -1, false);
    }

    void showPeas(WebContext ctx, DataSection section, boolean canModify, int only_base_xpath, boolean zoomedIn) {
        showPeas(ctx, section, canModify, only_base_xpath, null, zoomedIn);
    }

    void showPeas(WebContext ctx, DataSection section, boolean canModify, String only_prefix_xpath, boolean zoomedIn) {
        showPeas(ctx, section, canModify, -1, only_prefix_xpath, zoomedIn);
    }

    /**
     * Show a single item, in a very limited view.
     * @param ctx
     * @param section 
     * @param item_xpath xpath of the one item to show
     */
	public void showPeasShort(WebContext ctx, DataSection section,
			int item_xpath) {
		DataRow row = section.getDataRow(item_xpath);
		if(row!=null) {
			showDataRowShort(ctx, row);
		} else {
			//if(this.isUnofficial) {
				ctx.println("<tr><td colspan='2'>"+ctx.iconHtml("stop", "internal error")+"<i>internal error: nothing to show for xpath "+item_xpath+"," +
						" "+xpt.getById(item_xpath)+"</i></td></tr>");
			//}
		}
	}

    /**
     * Caller must hold session sync
     */
    void showPeas(WebContext ctx, DataSection section, boolean canModify, int only_base_xpath, String only_prefix_xpath, boolean zoomedIn) {
        int count = 0;
//        int dispCount = 0;
//        int total = 0;
        int skip = 0; // where the index points to
        int oskip = ctx.fieldInt("skip",0); // original skip from user.
        
        boolean partialPeas = ((only_base_xpath!=-1)||(only_prefix_xpath!=null));
        
        //       total = mySet.count();
        //        boolean sortAlpha = (sortMode.equals(PREF_SORTMODE_ALPHA));
        synchronized(ctx.session) {
            UserLocaleStuff uf = getUserFile(ctx, ctx.session.user, ctx.getLocale());
            CLDRFile cf = uf.cldrfile;
                //    CLDRFile engf = getBaselineFile();
            XMLSource ourSrc = uf.dbSource; // TODO: remove. debuggin'
            CheckCLDR checkCldr = (CheckCLDR)uf.getCheck(ctx);
//            XPathParts pathParts = new XPathParts(null, null);
//            XPathParts fullPathParts = new XPathParts(null, null);
//            List checkCldrResult = new ArrayList();
//            Iterator theIterator = null;
            String sortMode = getSortMode(ctx, section);
            boolean disputedOnly = false;
            if(ctx.field("only").equals("disputed")) {
                disputedOnly=true;
                sortMode = PREF_SORTMODE_WARNING; // so that disputed shows up on top
            }
            List peas = section.getList(sortMode);
    
        //    boolean exemplarCityOnly = (!partialPeas && pod.exemplarCityOnly);
            boolean exemplarCityOnly = (only_prefix_xpath!=null) && (only_prefix_xpath.equals(DataSection.EXEMPLAR_ONLY));
            if(exemplarCityOnly) {
                only_prefix_xpath = null; // special prefix
                partialPeas = false;
            }
            boolean exemplarCityExclude = (only_prefix_xpath!=null) && (only_prefix_xpath.endsWith(DataSection.EXEMPLAR_EXCLUDE));
            if(exemplarCityExclude) {
                only_prefix_xpath = only_prefix_xpath.replaceAll(DataSection.EXEMPLAR_EXCLUDE,""); // special prefix
            }
    
            String refs[] = new String[0];
            Hashtable<String,DataSection.DataRow> refsHash = new Hashtable<String, DataSection.DataRow>();
            Hashtable<String,DataSection.DataRow.CandidateItem> refsItemHash = new Hashtable<String, DataSection.DataRow.CandidateItem>();
            
            String ourDir = getHTMLDirectionFor(ctx.getLocale());
    //        boolean showFullXpaths = ctx.prefBool(PREF_XPATHS);
            // calculate references
            if(section.xpathPrefix.indexOf("references")==-1) {
                Set refsSet = new TreeSet();
                WebContext refCtx = (WebContext)ctx.clone();
                refCtx.setQuery("_",ctx.getLocale().getLanguage());
                refCtx.setLocale(CLDRLocale.getInstance(ctx.getLocale().getLanguage())); // ensure it is from the language
                DataSection refSection = refCtx.getSection("//ldml/references");
                List refPeas = refSection.getList(PREF_SORTMODE_CODE);
                //int rType[] = new int[1];
                for(Iterator i = refPeas.iterator();i.hasNext();) {
                    DataSection.DataRow p = (DataSection.DataRow)i.next();
                    // look for winning item
                    int vetResultId =  vet.getWinningXPath(p.base_xpath, refSection.locale);
                    DataSection.DataRow.CandidateItem winner = null;
                    DataSection.DataRow.CandidateItem someItem = null;
                    for(Iterator j = p.items.iterator();j.hasNext();) {
                        DataSection.DataRow.CandidateItem item = (DataSection.DataRow.CandidateItem)j.next();
                        if(item.inheritFrom == null) {
                            refsSet.add(p.type);
                            refsHash.put(p.type, p);
                            if(item.xpathId == vetResultId) {
                                winner = item;
                            }
                            someItem = item;
                        }
                    }
                    if(winner == null) {
                        winner = someItem; // pick a random item. 
                    }
                    if(winner != null) {
                        refsItemHash.put(p.type, winner);
                    }
                }
                if(!refsSet.isEmpty()) {
                    refs = (String[])refsSet.toArray((Object[]) refs);
                    ctx.temporaryStuff.put("refsHash",refsHash);
                    ctx.temporaryStuff.put("refsItemHash",refsItemHash);
                }
            }
            DataSection.DisplaySet dSet = null;
    
            if(exemplarCityOnly) {
                dSet = section.getDisplaySet(sortMode, Pattern.compile(".*exemplarCity.*"));
            } else if(exemplarCityExclude) {
                dSet = section.getDisplaySet(sortMode, Pattern.compile(".*/((short)|(long))/.*"));
            } else {
                dSet = section.getDisplaySet(sortMode);  // contains 'peas' and display list
            }
            
            boolean checkPartitions = (dSet.partitions.length > 0) && (dSet.partitions[0].name != null); // only check if more than 0 named partitions
            int moveSkip=-1;  // move the "skip" marker?
            int xfind = ctx.fieldInt("xfind");
            if((xfind != -1) && !partialPeas) {
                // see if we can find this base_xpath somewhere..
                int pn = 0;
                for(Iterator i = peas.iterator();(moveSkip==-1)&&i.hasNext();) {
                    DataSection.DataRow p = (DataSection.DataRow)i.next();
                    if(p.base_xpath == xfind) {
                        moveSkip = pn;
                    }
                    pn++;
                }
                if(moveSkip != -1) {
                    oskip = (moveSkip / ctx.prefCodesPerPage())*ctx.prefCodesPerPage(); // make it fall on a page boundary.
                }
            }
            // -----
            if(!partialPeas) {
                skip = showSkipBox(ctx, dSet.size(), dSet, true, sortMode, oskip);
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
                printSectionTableOpen(ctx, section, zoomedIn);
            }
    
                        
            int peaStart = skip; // where should it start?
            int peaCount = ctx.prefCodesPerPage(); // hwo many to show?
            
            if(disputedOnly) {            
                // we want to go from VETTING_PROBLEMS_LIST[0]..VETTING_PROBLEMS_LIST[n] in range.
                for(int j = 0;j<dSet.partitions.length;j++) {
                    for(String part : DataSection.VETTING_PROBLEMS_LIST) {
                        if(dSet.partitions[j].name.equals(part)) {
                            if(peaStart != skip) { // set once
                                peaStart=dSet.partitions[j].start;
                            }
                            peaCount=(dSet.partitions[j].limit - peaStart); // keep setting this.
                        }
                    }
                }
            }
    
            
            for(ListIterator i = dSet.rows.listIterator(peaStart);(partialPeas||(count<peaCount))&&i.hasNext();count++) {
                DataSection.DataRow p = (DataSection.DataRow)i.next();
                
                if(partialPeas) { // are we only showing some peas?
                    if((only_base_xpath != -1) && (only_base_xpath != p.base_xpath)) { // print only this one xpath
                        continue; 
                    }
                    
                    if((only_prefix_xpath!=null) && !section.xpath(p).startsWith(only_prefix_xpath)) {
                        continue;
                    }
                }
                
                if((!partialPeas) && checkPartitions) {
                    for(int j = 0;j<dSet.partitions.length;j++) {
                        if((dSet.partitions[j].name != null) && (count+skip) == dSet.partitions[j].start) {
                            ctx.println("<tr class='heading'><th class='partsection' align='left' colspan='"+PODTABLE_WIDTH+"'>" +
                                "<a name='" + dSet.partitions[j].name + "'>" +
                                dSet.partitions[j].name + "</a>" +
                                "</th></tr>");
                        }
                    }
                }
                
                try {
                    if(exemplarCityOnly) {
                        String zone = p.type;
                        int n = zone.lastIndexOf('/');
                        if(n!=-1) {
                            zone = zone.substring(0,n); //   blahblah/default/foo   ->  blahblah/default   ('foo' is lastType and will show up as the value)
                        }
                        showDataRow(ctx, section, p, ourDir, uf, cf, ourSrc, canModify,ctx.base()+"?"+
                                "_="+ctx.getLocale()+"&amp;x=timezones&amp;zone="+zone,refs,checkCldr, zoomedIn);                
                    } else {
                        showDataRow(ctx, section, p, ourDir, uf, cf, ourSrc, canModify,null,refs,checkCldr, zoomedIn);
                    }
                } catch(Throwable t) {
                    // failed to show pea. 
                    ctx.println("<tr class='topbar'><td colspan='8'><b>"+section.xpath(p)+"</b><br>");
                    ctx.print(t);
                    ctx.print("</td></tr>");
                }
                if(p.subRows != null) {
                    for(Iterator e = p.subRows.values().iterator();e.hasNext();) {
                        DataSection.DataRow subDataRow = (DataSection.DataRow)e.next();
                        try {
                            showDataRow(ctx, section, subDataRow, ourDir, uf, cf, ourSrc, canModify, null,refs,checkCldr, zoomedIn);
                        } catch(Throwable t) {
                            // failed to show sub-pea.
                            ctx.println("<tr class='topbar'><td colspan='8'>sub pea: <b>"+section.xpath(subDataRow)+"."+subDataRow.altType+"</b><br>");
                            ctx.print(t);
                            ctx.print("</td></tr>");
                        }
                    }
                }
            }
            if(!partialPeas) {
                printSectionTableClose(ctx, section);
            
                if(canModify && 
                    section.xpathPrefix.indexOf("references")!=-1) {
                    ctx.println("<hr>");
                    ctx.println("<table class='listb' summary='New Reference Box'>");
                    ctx.println("<tr><th colspan=2 >Add New Reference ( click '"+getSaveButtonText()+"' after filling in the fields.)</th></tr>");
                    ctx.println("<tr><td align='right'>Reference: </th><td><input size='80' name='"+MKREFERENCE+"_v'></td></tr>");
                    ctx.println("<tr><td align='right'>URI: </th><td><input size='80' name='"+MKREFERENCE+"_u'></td></tr>");
                    ctx.println("</table>");
                }
                
                
                /*skip = */ showSkipBox(ctx, dSet.size(), dSet, false, sortMode, oskip);
                
                if(!canModify) {
                    ctx.println("<hr> <i>You are not authorized to make changes to this locale.</i>");
                }
            }
        }
    }

    /**
     * Call from within session lock
     * @param ctx
     * @param oldSection
     * @param cf
     * @param ourSrc
     * @param dsrh 
     * @return
     */
    boolean processPeaChanges(WebContext ctx, DataSection oldSection, CLDRFile cf, XMLSource ourSrc, DataSubmissionResultHandler dsrh) {
        String ourDir = getHTMLDirectionFor(ctx.getLocale());
        boolean someDidChange = false;
        if(oldSection != null) {
            for(Iterator i = oldSection.getAll().iterator();i.hasNext();) {
                DataSection.DataRow p = (DataSection.DataRow)i.next();
                someDidChange = processDataRowChanges(ctx, oldSection, p, ourDir, cf, ourSrc, dsrh) || someDidChange;
                if(p.subRows != null) {
                    for(Iterator e = p.subRows.values().iterator();e.hasNext();) {
                        DataSection.DataRow subDataRow = (DataSection.DataRow)e.next();
                        someDidChange = processDataRowChanges(ctx, oldSection, subDataRow, ourDir, cf, ourSrc, dsrh) || someDidChange;
                    }
                }
            }            
            if(oldSection.xpathPrefix.indexOf("references")!=-1) {
                String newRef = ctx.field(MKREFERENCE+"_v");
                String uri = ctx.field(MKREFERENCE+"_u");
                if(newRef.length()>0) {
                    String dup = null;
                    for(Iterator i = oldSection.getAll().iterator();i.hasNext();) {
                        DataSection.DataRow p = (DataSection.DataRow)i.next();
                        DataSection.DataRow subDataRow = p;
                        for(Iterator j = p.items.iterator();j.hasNext();) {
                            DataSection.DataRow.CandidateItem item = (DataSection.DataRow.CandidateItem)j.next();
                            if(item.value.equals(newRef)) {
                                dup = p.type;
                            } else {
                            }
                        }
                    }            
                    ctx.print("<b>Adding new reference...</b> ");
                    if(dup != null) {
                        ctx.println("Ref already exists, not adding: <tt class='codebox'>"+dup+"</tt> " + newRef + "<br>");
                    } else {
                        String newType =  DataSection.addReferenceToNextSlot(ourSrc, cf, ctx.getLocale().toString(), ctx.session.user.id, newRef, uri);
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
        	System.err.println("SomeDidChange: " + oldSection.locale());
            updateLocale(oldSection.locale());
        }
        return someDidChange;
    }

    private void updateLocale(CLDRLocale locale) {
        lcr.invalidateLocale(locale);
        int n = vet.updateImpliedVotes(locale); // first implied votes
        System.err.println("updateLocale:"+locale.toString()+":  vet_imp:"+n);
    }
    /**
     * Call from within  session lock
     * @param ctx
     * @param section
     * @param p
     * @param ourDir
     * @param cf
     * @param ourSrc
     * @param dsrh 
     * @return
     */
    boolean processDataRowChanges(WebContext ctx, DataSection section, DataSection.DataRow p, String ourDir, CLDRFile cf, XMLSource ourSrc, DataSubmissionResultHandler dsrh) {
        String fieldHash = section.fieldHash(p);
        String altType = p.altType;
        String choice = ctx.field(fieldHash); // checkmark choice
        
//        System.err.println("CH["+fieldHash+"] -> " + choice);
        
        if(choice.length()==0) {
            return false; // nothing to see..
        }
        
        String fullPathFull = section.xpath(p);
        int base_xpath = xpt.xpathToBaseXpathId(fullPathFull);
        int oldVote = vet.queryVote(section.locale, ctx.session.user.id, base_xpath);
        // do modification here. 
        String choice_v = ctx.field(fieldHash+"_v"); // choice + value
        String choice_r = ctx.field(fieldHash+"_r"); // choice + value
        String choice_refDisplay = ""; // display value for ref
        boolean canSubmit = UserRegistry.userCanSubmitAnyLocale(ctx.session.user) || p.hasProps || p.hasErrors;
        
        //NOT a toggle.. proceed 'normally'
        
        if(choice_r.length()>0) {
            choice_refDisplay = " Ref: "+choice_r;
        }
        if(!choice.equals(CHANGETO)) {
            choice_v=""; // so that the value is ignored, as it is not changing
        } else if (choiceNotEmptyOrAllowedEmpty(choice_v, fullPathFull)) {
            choice_v = ctx.processor.processInput(xpt.getById(p.base_xpath),choice_v, null);
        }
        
//        System.err.println("choice="+choice+", v="+choice_v);
        
        /* handle inherited value */
        if(choice.equals(INHERITED_VALUE)) {
            if(p.inheritedValue == null) {
                throw new InternalError(p+" has no inherited value!");
            }
            
            // remap. Will cause the 2nd if branch to be followed, below.se
            choice = CHANGETO;
            choice_v = p.inheritedValue.value;
         //   System.err.println("inherit -> CHANGETO");
        }
        
        // . . .
        DataSection.DataRow.CandidateItem voteForItem = null;
        Set<DataSection.DataRow.CandidateItem> deleteItems = null; // remove item
		String[] deleteAlts = ctx.fieldValues(fieldHash+ACTION_DEL);
		Set<String> deleteAltsSet = null;
		
		if(deleteAlts.length > 0) {
			deleteAltsSet = new HashSet<String>();
			deleteItems = new HashSet<DataSection.DataRow.CandidateItem>();
			for(String anAlt : deleteAlts) {
				deleteAltsSet.add(anAlt);
			}
		}
		Set<DataSection.DataRow.CandidateItem> unvoteItems = null; // remove item
		String[] unvoteAlts = ctx.fieldValues(fieldHash+ACTION_UNVOTE);
		Set<String> unvoteAltsSet = null;
		
		if(unvoteAlts.length > 0) {
		    unvoteAltsSet = new HashSet<String>();
		    unvoteItems = new HashSet<DataSection.DataRow.CandidateItem>();
		    for(String anAlt : unvoteAlts) {
		        unvoteAltsSet.add(anAlt);
		    }
		}
        
		/* find the item.  Could fail if the HTML is stale. */
        for(Iterator j = p.items.iterator();j.hasNext();) {
            DataSection.DataRow.CandidateItem item = (DataSection.DataRow.CandidateItem)j.next();
            if(choice.equals(item.altProposed)) {
                voteForItem = item;
			} else if(choice.equals(CONFIRM)&&item.altProposed==null) {
				voteForItem = item; // default item.
			}
			
			if((item.altProposed != null) && 
			   (deleteAltsSet != null) &&
			   (deleteAltsSet.contains(item.altProposed))) {
				deleteItems.add(item);
			}
			if((item.altProposed != null) && 
			        (unvoteAltsSet != null) &&
			        (unvoteAltsSet.contains(item.altProposed))) {
			    unvoteItems.add(item);
			}
			
        }
        
        if(voteForItem!=null) {
        	if(voteForItem.inheritFrom!=null || voteForItem.isFallback || voteForItem.isParentFallback || (voteForItem.pathWhereFound!=null&&voteForItem.pathWhereFound.length()>0)) {
                // remap as a NEW ITEM.
                choice = CHANGETO;
//                System.err.println("v4i"+choice+" -> CHANGETO "+voteForItem.value);
                choice_v = voteForItem.value;
                choice_r = voteForItem.references;
                if(choice_r==null) choice_r="";
        	}
        }
        
        // OBSOLETE
//        if(p.attributeChoice != null) {
//            // it's not a toggle.. but it is an attribute choice.
//
//            String altPrefix =         XPathTable.altProposedPrefix(ctx.session.user.id);
//            boolean unvote = false;
//            String voteForChoice = null;
//            boolean hadChange = false;
//            
//            if(voteForItem != null) {
//                voteForChoice = voteForItem.value;
//            } else if(choice.equals(DONTCARE)) {
//                unvote = true;
//            } else if(choice.equals(CHANGETO) && (choice_v.length()>0)) {
//                voteForChoice = choice_v;
//                for(Iterator j = p.items.iterator();j.hasNext();) {
//                    DataSection.DataRow.CandidateItem item = (DataSection.DataRow.CandidateItem)j.next();
//                    if(item.value.equals(voteForChoice)) {
//                        voteForItem = item; // found an item
//                    }
//                }
//            } else {
//                return false;
//            }
//            
//            if(unvote) {
//                for(String v : p.valuesList) {
//                    String unvoteXpath = p.attributeChoice.xpathForChoice(v);
//                    int unvoteXpathId = xpt.xpathToBaseXpathId(unvoteXpath);                    
//                    
//                    int oldNoVote = vet.queryVote(section.locale, ctx.session.user.id, unvoteXpathId);
//                    
//                    if(oldNoVote != -1) {
//                        doVote(ctx, section.locale, -1, unvoteXpathId);
//                        hadChange = true;
//                    }
//                }
//                if(hadChange) {
//                    ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>: Removing vote <br>");
//                    return true;
//                }
//            } else {
//                // vote for item
//                boolean updateImpliedVotes = false;
//                // yes vote first
//                String yesPath = p.attributeChoice.xpathForChoice(voteForChoice);
//                int yesId = xpt.xpathToBaseXpathId(yesPath);
//                if(voteForItem == null) {
//                    // no item existed - create it.
//                    String yesPathMinusAlt = XPathTable.removeAlt(yesPath);
//                    String newProp = ourSrc.addDataToNextSlot(cf, section.locale, yesPathMinusAlt, p.altType, 
//                        altPrefix, ctx.session.user.id, voteForChoice, choice_r); // removal
//                    doUnVote(ctx, section.locale, yesId); // remove explicit vote - the implied votes will pick up the vote
//                    updateImpliedVotes = true;
//                    hadChange = true;
//                    ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>:creating new item "+voteForChoice+" <br>");
//                } else {
//                    // voting for an existing item.
//                    int oldYesVote = vet.queryVote(section.locale, ctx.session.user.id, yesId);
//                    if(oldYesVote != voteForItem.xpathId) {
//                        doVote(ctx, section.locale, voteForItem.xpathId, yesId);
//                        hadChange = true;
//                    }
//                }
//                
//                // vote REMOVE on the rest
//                for(String v : p.valuesList) {
//                    if(v.equals(voteForChoice)) continue; // skip the new item
//                    String noPath = p.attributeChoice.xpathForChoice(v);
//                    int noId = xpt.xpathToBaseXpathId(noPath);
//                    
//                    int oldNoVote = vet.queryVote(section.locale, ctx.session.user.id, noId);
//                    // remove vote
//                    
//                    if(oldNoVote != -1) {
//                        hadChange = true;
//                        doVote(ctx, section.locale, -1, noId);
//                    }
//                }
//
//
//                if(hadChange) {
//                    ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>: vote succeeded <br>");
//                    return true;
//                }
//            }            
//
//            return false;
//        }
        
        // handle a change of REFERENCE.
        // If there was a reference - see if they MIGHT be changing reference
//        if(choice_r.length()>0) {
//        }
        
		
		boolean didSomething = false;  // Was any item modified?
		
		if(deleteItems != null && ctx.session.user!=null) {
			for(DataSection.DataRow.CandidateItem item : deleteItems) {
			    boolean adminOrRelevantTc = UserRegistry.userIsAdmin(ctx.session.user); // ctx.session.user.isAdminFor(reg.getInfo(item.submitter));
				if((item.submitter != -1) && 
					!( (item.pathWhereFound != null) || item.isFallback || (item.inheritFrom != null) /*&&(p.inheritFrom==null)*/) && // not an alias / fallback / etc
						( (item.getVotes() == null) || UserRegistry.userIsTC(ctx.session.user) ||  // nobody voted for it, or
							((item.getVotes().size()==1)&& item.getVotes().contains(ctx.session.user) )) && // only user voted for it
						( (item.submitter>0&&UserRegistry.userIsTC(ctx.session.user)) || (item.submitter == ctx.session.user.id) ) ) // user is TC or user is submitter
				{
					dsrh.handleRemoveItem(p, item, (voteForItem==item));
					if(voteForItem == item) {
						choice = DONTCARE;
						voteForItem = null;
					}
					ourSrc.removeValueAtDPath(xpt.getById(item.xpathId));
					didSomething = true;
				} else {
					dsrh.handleNoPermission(p, item, "remove");
				}
			}
		}
		if(unvoteItems != null && UserRegistry.userIsTC(ctx.session.user)) {
		    for(DataSection.DataRow.CandidateItem item : unvoteItems) {
		        if (item.getVotes()==null) continue;
		        for(UserRegistry.User voter : item.getVotes()) {
		            if(voter.org.equals(ctx.session.user.org)) {
		                
                        boolean did = doAdminRemoveVote(ctx, ctx.getLocale(), p.base_xpath, voter.id);
                        if(did) {
                        	dsrh.handleRemoveVote(p, voter, item);
    	                    didSomething = true;
                        }
		            }
		        }
		    }
		}
		
        
        if(choice.equals(CHANGETO)&& !choiceNotEmptyOrAllowedEmpty(choice_v, fullPathFull)) {
        	dsrh.handleEmptyChangeto(p);
            ctx.temporaryStuff.put(fieldHash+"_v", choice_v);  // mark it for "this item not accepted"
        } else if( (choice.equals(CHANGETO) && choiceNotEmptyOrAllowedEmpty(choice_v, fullPathFull)) ||
             (HAVE_REMOVE&&choice.equals(REMOVE)) ) {
            if(!canSubmit) {
            	dsrh.handleNoPermission(p, null, "submit");
//                ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>: ");
//                ctx.println(ctx.iconHtml("stop","empty value")+" You are not allowed to submit data at this time.<br>");
                return didSomething;
            }
            String fullPathMinusAlt = XPathTable.removeAlt(fullPathFull);
            if(fullPathMinusAlt.indexOf("/alias")!=-1) {
            	dsrh.handleNoPermission(p, null, "modify an alias");
//                ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>: ");
//                ctx.println(ctx.iconHtml("stop","alias")+" You are not allowed to submit data against this alias item. Contact your CLDR-TC representative.<br>");
                return didSomething;
            }
            for(Iterator j = p.items.iterator();j.hasNext();) {
                DataSection.DataRow.CandidateItem item = (DataSection.DataRow.CandidateItem)j.next();
                if(choice_v.equals(item.value)  && 
                		(item.pathWhereFound==null) &&
                    !((item.altProposed==null) && (item.inheritFrom!=null) &&  XMLSource.CODE_FALLBACK_ID.equals(item.inheritFrom))) { // OK to override code fallbacks
                    String theirReferences = item.references;
                    if(theirReferences == null) {
                        theirReferences="";
                    }
                    if(theirReferences.equals(choice_r)) {
                        if(oldVote != item.xpathId) {
                        	dsrh.warnAcceptedAsVoteFor(p, item);
                            return doVote(ctx, section.locale, item.xpathId) || didSomething;
                        } else {
                        	dsrh.warnAlreadyVotingFor(p, item);
							return didSomething;
                        }
                    } else {
//                        ctx.println(ctx.iconHtml("warn","duplicate")+"<i>Note, differs only in references</i> ("+theirReferences+")<br>");
                    }
                }
            }
            String altPrefix = null;
            // handle FFT
                    
            if(p.type.equals(DataSection.FAKE_FLEX_THING)) {
                throw new RuntimeException("Missing fake flex thing");
            } else {
                altPrefix =         XPathTable.altProposedPrefix(ctx.session.user.id);
            }
            // user requested a new alternate.
            String newAlt = ctx.field(fieldHash+"_alt").trim();
            if(newAlt.length()>0) {
                altType = newAlt;
            }
            String aDisplayName = p.displayName;
            if(section.xpath(p).startsWith("//ldml/characters") && 
                ((aDisplayName == null) || "null".equals(aDisplayName))) {
                aDisplayName = "standard";
            }
            
            dsrh.handleProposedValue(p, choice_v);
//            dsrh.handleNewValue(p, choice_v);
            // don't print 'new value' here until it is successful.

            boolean doFail = false;

            // Test: is the value valid?
            {
                /* cribbed from elsewhere */
                // The enclosion function already holds session sync
                UserLocaleStuff uf = getUserFile(ctx, (ctx.session.user==null)?null:ctx.session.user, ctx.getLocale());
                // Set up checks
                CheckCLDR checkCldr = (CheckCLDR)uf.getCheck(ctx); //make it happen
                List checkCldrResult = new ArrayList();
                
                checkCldr.check(fullPathMinusAlt, fullPathMinusAlt, choice_v, ctx.getOptionsMap(basicOptionsMap()), checkCldrResult);  // they get the full course
                
                if(!checkCldrResult.isEmpty()) {
//                    boolean hadWarnings = false;
                    for (Iterator it3 = checkCldrResult.iterator(); it3.hasNext();) {
                        CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();
                        if(status.getType().equals(status.errorType)) {
                            if(status.getCause() instanceof org.unicode.cldr.test.CheckCoverage) {
                                // coverage error, ignored
                            } else {
                                doFail = true;
                            }
                        }
                        try{ 
                            if (!(status.getCause() instanceof org.unicode.cldr.test.CheckCoverage) &&
                                    !status.getType().equals(status.exampleType)) {
//                                if(!hadWarnings) {
//                                    hadWarnings = true;
//                                    ctx.print("<br>");
//                                }
                            	dsrh.handleError(p, status, choice_v);
                            	if(dsrh.rejectErrorItem(p)) {
                            		return false;
                            	}
                            }
                        } catch(Throwable t) {
                            ctx.println("Error reading status item: <br><font size='-1'>"+status.toString()+"<br> - <br>" + t.toString()+"<hr><br>");
                        }
                    }
                    if(doFail) {
                    	if(dsrh.rejectErrorItem(p)) {
                    		return false;
                    	}
                        // reject the value
                        //if(!(HAVE_REMOVE&&choice.equals(REMOVE))) {
                        //    ctx.temporaryStuff.put(fieldHash+"_v", choice_v);  // mark it 
                        //}
                        //return false;
                    }
                }
            }
            
            String newProp = DataSection.addDataToNextSlot(ourSrc, cf, section.locale, fullPathMinusAlt, altType, 
                altPrefix, ctx.session.user.id, choice_v, choice_r);
            // update implied vote
//            ctx.print(" &nbsp;&nbsp; <tt class='proposed'>" + newProp+"</tt>");
            if(HAVE_REMOVE&&choice.equals(REMOVE)) {
            	dsrh.handleRemoved(p);
            } else {
            	dsrh.handleNewValue(p, choice_v, doFail);
            }
            doUnVote(ctx, section.locale, base_xpath);
            //ctx.println("updated " + n + " implied votes due to new data submission.");
            return true;
        } else if(choice.equals(CONFIRM)) {
            if(oldVote != base_xpath) {
            	dsrh.handleVote(p, oldVote, base_xpath);
                return doVote(ctx, section.locale, base_xpath) || didSomething; // vote with xpath
            }
        } else if (choice.equals(DONTCARE)) {
            if(oldVote != -1) {
            	dsrh.handleVote(p, oldVote, -1);
                return doVote(ctx, section.locale, -1, base_xpath) || didSomething;
            }
        } else if(voteForItem != null)  {
            DataSection.DataRow.CandidateItem item = voteForItem;
            if(oldVote != item.xpathId) {
            	dsrh.handleVote(p, oldVote, item.xpathId);
                return doVote(ctx, section.locale, item.xpathId) || didSomething;
            } else {
                return didSomething; // existing vote.
            }
		} else {
			dsrh.handleUnknownChoice(p, choice);
        }
        return didSomething;
    }

    boolean doVote(WebContext ctx, CLDRLocale locale, int xpath) {
        int base_xpath = xpt.xpathToBaseXpathId(xpath);
        return doVote(ctx, locale, xpath, base_xpath);
    }

    boolean doVote(WebContext ctx, CLDRLocale locale, int xpath, int base_xpath) {
        return doVote(ctx, locale, xpath, base_xpath, ctx.session.user.id);
    }

    boolean doVote(WebContext ctx, CLDRLocale locale, int xpath, int base_xpath, int id) {
        vet.vote( locale,  base_xpath, id, xpath, Vetting.VET_EXPLICIT);
      //  lcr.invalidateLocale(locale); // throw out this pod next time, cause '.votes' are now wrong.
        return true;
    }

    boolean doAdminRemoveVote(WebContext ctx, CLDRLocale locale, int base_xpath, int id) {
        vet.vote( locale,  base_xpath, id, -1, Vetting.VET_ADMIN);
     //   lcr.invalidateLocale(locale); // throw out this pod next time, cause '.votes' are now wrong.
        return true;
    }

    int doUnVote(WebContext ctx, CLDRLocale locale, int base_xpath) {
        return doUnVote(ctx, locale, base_xpath, ctx.session.user.id);
    }
    
    int doUnVote(WebContext ctx, CLDRLocale locale, int base_xpath, int submitter) {
        int rs = vet.unvote( locale,  base_xpath, submitter);
      //  lcr.invalidateLocale(locale); // throw out this pod next time, cause '.votes' are now wrong.
        return rs;
    }

    void showDataRow(WebContext ctx, DataSection section, DataSection.DataRow p, String ourDir, UserLocaleStuff uf, CLDRFile cf, 
        XMLSource ourSrc, boolean canModify, String specialUrl, String refs[], CheckCLDR checkCldr)  {
        showDataRow(ctx,section,p,ourDir,uf, cf,ourSrc,canModify,specialUrl,refs,checkCldr,false);
    }

    /**
     * Show a row of limited data.
     * @param ctx
     * @param row
     */
    void showDataRowShort(WebContext ctx, DataRow row) {
        ctx.put(WebContext.DATA_ROW, row);
        
        ctx.includeFragment("datarow_short.jsp");
    }

    /**
     * Show a single pea
     */
    void showDataRow(WebContext ctx, DataSection section, DataSection.DataRow p, String ourDir, UserLocaleStuff uf, CLDRFile cf, 
        XMLSource ourSrc, boolean canModify, String specialUrl, String refs[], CheckCLDR checkCldr, boolean zoomedIn) {

        String ourAlign = "left";
        if(ourDir.equals("rtl")) {
            ourAlign = "right";
        }
        
        boolean canSubmit = UserRegistry.userCanSubmitAnyLocale(ctx.session.user)  // formally able to submit
            || (canModify/*&&p.hasProps*/); // or, there's modified data.

        boolean showedRemoveButton = false; 
        String fullPathFull = section.xpath(p); 
        String boxClass = canModify?"actionbox":"disabledbox";
        boolean isAlias = (fullPathFull.indexOf("/alias")!=-1);
        WebContext refCtx = (WebContext)ctx.clone();
        refCtx.setQuery("_",ctx.getLocale().getLanguage());
        refCtx.setQuery(QUERY_SECTION,"references");
        //            ctx.println("<tr><th colspan='3' align='left'><tt>" + p.type + "</tt></th></tr>");

        String fieldHash = section.fieldHash(p);
        
        // voting stuff
        int ourVote = -1;
        String ourVoteXpath = null;
        boolean somethingChecked = false; // have we presented a 'vote' yet?
        int base_xpath = xpt.xpathToBaseXpathId(fullPathFull);
//        String resultXpath = null;
//        boolean noWinner = false;
 /*
  *        int resultXpath_id =  vet.queryResult(section.locale, base_xpath, resultType);
        if(resultXpath_id != -1) {
            resultXpath = xpt.getById(resultXpath_id); 
        } else {
  //          noWinner = true;
        }
 */
        if(ctx.session.user != null) {
            ourVote = vet.queryVote(section.locale, ctx.session.user.id, 
                base_xpath);
            
            //System.err.println(base_xpath + ": vote is " + ourVote);
            if(ourVote != -1) {
                ourVoteXpath = xpt.getById(ourVote);
            }
        }
        
        boolean inheritedValueHasTestForCurrent = false; // if (no current items) && (inheritedValue.hastests) 

        
        // let's see what's inside.
        // TODO: move this into the DataPod itself?
        List<DataSection.DataRow.CandidateItem> currentItems = p.getCurrentItems();
        List<DataSection.DataRow.CandidateItem> proposedItems = p.getProposedItems();
        
        
        // Does the inheritedValue contain a test that we need to display?
        if(proposedItems.isEmpty() && p.inheritedValue!=null && p.inheritedValue.value==null && p.inheritedValue.tests!=null) {
            inheritedValueHasTestForCurrent = true;
        }
        
        // calculate the max height of the current row.
        int rowSpan = Math.max(proposedItems.size(),currentItems.size()); // what is the rowSpan needed for general items?
        rowSpan = Math.max(rowSpan,1);
        
        /*  TOP BAR */
        // Mark the line as disputed or insufficient, depending.
        String rclass = "vother";
        boolean foundError = p.hasErrors;
        boolean foundWarning = p.hasWarnings;
        
        List<DataSection.DataRow.CandidateItem> numberedItemsList = new ArrayList<DataSection.DataRow.CandidateItem>();
        List<String> refsList = (List<String>) ctx.temporaryStuff.get("references");
        
        // todo: move into DataRow
        String okayIcon;
        if(p.confirmStatus == Vetting.Status.APPROVED) {
            okayIcon = ctx.iconHtml("okay", "Approved Item");
        } else {
            okayIcon = ctx.iconHtml("ques", p.confirmStatus + " Item");
        }

        // calculate the class of data items
        String statusIcon="";
        {
            int s = p.getResultType();
            
            if(foundError) {
    //            rclass = "warning";
                statusIcon = ctx.iconHtml("stop","Errors - please zoom in");            
            } else if(foundWarning) {
                rclass = "okay";
                statusIcon = ctx.iconHtml("warn","Warnings - please zoom in");            

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
                statusIcon = (ctx.iconHtml("ques","Unconfirmed: disputed"));
            } else if((s & Vetting.RES_ERROR)>0) {
                rclass= "disputedrow";
                statusIcon = (ctx.iconHtml("stop","Error in Proposals"));
            } else if ((s&(Vetting.RES_INSUFFICIENT))>0) {
                rclass = "insufficientrow";
                statusIcon = (ctx.iconHtml("ques","Unconfirmed: insufficient"));            
            } else if ((s&(Vetting.RES_NO_VOTES))>0) {
                statusIcon = (ctx.iconHtml("squo","No Votes"));            
            } else if((s&(Vetting.RES_BAD_MASK)) == 0) {
                if((!p.hasInherited&&!p.hasMultipleProposals) 
                            || (s==0) || (s&Vetting.RES_NO_CHANGE) > 0) {
                    if(p.confirmStatus == Vetting.Status.INDETERMINATE) {
                        statusIcon = ctx.iconHtml("squo", "No New Values");
                    } else if(p.confirmStatus != Vetting.Status.APPROVED) {
                        statusIcon = (ctx.iconHtml("ques-2","Not Approved, but no alternatives"));
                    } else {
                        statusIcon = ctx.iconHtml("okay", "Approved Item");
                    }
                } else {
                    if(!p.hasMultipleProposals && (p.confirmStatus != Vetting.Status.APPROVED)) {
                        statusIcon = (ctx.iconHtml("ques-2","Not Approved, but no alternatives"));
                    } else {
                        statusIcon = okayIcon;
                    }
                }
                rclass = "okay";
            } else {
                rclass = "vother";
            }
    /*        if(noWinner && XMLSource.CODE_FALLBACK_ID.equals(p.inheritFrom)) {
                rclass = "insufficientrow";
            }*/
        }
        
        ctx.println("<tr class='topbar'>");
      //  String baseInfo = "#"+base_xpath+", w["+Vetting.typeToStr(resultType[0])+"]:" + resultXpath_id;
        
         
        ctx.print("<th rowspan='"+rowSpan+"' class='"+rclass+"' valign='top'>");
        if(!zoomedIn) {
            if(specialUrl != null) {
                ctx.print("<a class='notselected' target='"+ctx.atarget("n:"+ctx.getLocale().toString())+"' href='"+specialUrl+"'>"+statusIcon+"</a>");
            } else {
                fora.showForumLink(ctx,section,p,p.parentRow.base_xpath,statusIcon);
            }
        } else {
            ctx.print(statusIcon);
        }
        ctx.println("</th>");
        
        // ##2 code
        ctx.print("<th rowspan='"+rowSpan+"' class='botgray' colspan='1' valign='top' align='left'>");
        //if(p.displayName != null) { // have a display name - code gets its own box
        int xfind = ctx.fieldInt("xfind");
        if(xfind==base_xpath) {
            ctx.print("<a name='x"+xfind+"'>");
        }
        
        printItemTypeName(ctx, p, canModify, zoomedIn, specialUrl);

        
        if(p.altType != null) {
            ctx.print("<br> ("+p.altType+" alternative)");
        }
        if(xfind==base_xpath) {
            ctx.print("</a>"); // for the <a name..>
        }
        ctx.println("</th>");
        
        // ##3 display / Baseline
        ExampleContext exampleContext = new ExampleContext();
        
        // calculate winner
        // ##5 current control ---
        DataSection.DataRow.CandidateItem topCurrent = null;
        if(currentItems.size() > 0) {
            topCurrent = currentItems.get(0);
        }
        if((topCurrent == null) && inheritedValueHasTestForCurrent) { // bring in the inheritedValue if it has a meaningful test..
            topCurrent = p.inheritedValue;
        }
        
        // Prime the Pump  - Native must be called first.
        if(topCurrent != null) {
            /* ignored */ uf.getExampleGenerator().getExampleHtml(topCurrent.xpath, topCurrent.value,
                    zoomedIn?ExampleGenerator.Zoomed.IN:ExampleGenerator.Zoomed.OUT, exampleContext, ExampleType.NATIVE);
        } else {
            // no top item, so use NULL
            /* ignored */ uf.getExampleGenerator().getExampleHtml(section.xpath(p), null,
                    zoomedIn?ExampleGenerator.Zoomed.IN:ExampleGenerator.Zoomed.OUT, exampleContext, ExampleType.NATIVE);
        }
        

//        String baseExample = getBaselineExample().getExampleHtml(fullPathFull, p.displayName, zoomedIn?ExampleGenerator.Zoomed.IN:ExampleGenerator.Zoomed.OUT);
        String baseExample = getBaselineExample().getExampleHtml(fullPathFull, p.displayName, zoomedIn?ExampleGenerator.Zoomed.IN:ExampleGenerator.Zoomed.OUT,
                exampleContext, ExampleType.ENGLISH);
        int baseCols = 1;

        ctx.println("<th rowspan='"+rowSpan+"'  style='padding-left: 4px;' colspan='"+baseCols+"' valign='top' align='left' class='botgray'>");
        if(p.displayName != null) {
			if(p.uri != null) {
				ctx.print("<a class='refuri' href='"+p.uri+"'>");
			}
            ctx.print(p.displayName); // ##3 display/Baseline
			if(p.uri != null) {
				ctx.print("</a>");
			}
        }
    /*
        if(specialUrl) {
            ctx.println("<br>"+fullPathFull);
        }

        if(true==false) {
            ctx.println("<br>"+"hasTests:"+p.hasTests+", props:"+p.hasProps+", hasInh:"+p.hasInherited);
        }
    */
        ctx.println("</th>");
        
        // ##2 and a half - baseline sample
        if(baseExample != null) {
            ctx.print("<td rowspan='"+rowSpan+"' align='left' valign='top' class='generatedexample'>"+ 
                baseExample.replaceAll("\\\\","\u200b\\") + "</td>");
        } else {
            ctx.print("<td rowspan='"+rowSpan+"' ></td>"); // empty box for baseline
        }
        
//        if(topCurrent != null) {
            printCells(ctx,section,p,topCurrent,fieldHash,p.getResultXpath(),ourVoteXpath,canModify,ourAlign,ourDir,uf,zoomedIn, numberedItemsList, refsList, exampleContext);
//        } else {
//            printEmptyCells(ctx, section, p, ourAlign, ourDir, zoomedIn);
//        }

        // ## 6.1, 6.2 - Print the top proposed item. Can be null if there aren't any.
        DataSection.DataRow.CandidateItem topProposed = null;
        if(proposedItems.size() > 0) {
            topProposed = proposedItems.get(0);
        }
        if(topProposed != null) {
            printCells(ctx,section,p,topProposed,fieldHash,p.getResultXpath(),ourVoteXpath,canModify,ourAlign,ourDir,uf,zoomedIn, numberedItemsList, refsList, exampleContext);
        } else {
            printEmptyCells(ctx, section, p, ourAlign, ourDir, zoomedIn);
        }

        boolean areShowingInputBox = (canSubmit && !isAlias && canModify && !p.confirmOnly && (zoomedIn||!p.zoomOnly));

        // submit box
        if((isPhaseSubmit()==true)
			|| UserRegistry.userIsTC(ctx.session.user)
			|| ( UserRegistry.userIsVetter(ctx.session.user) && ctx.session.user.userIsSpecialForCLDR15(section.locale))
            || ((isPhaseVetting() || isPhaseVettingClosed()) && ( p.hasErrors  ||
                                  p.hasProps ||  (p.getResultType()== Vetting.RES_DISPUTED) ))) {
            String changetoBox = "<td width='1%' class='noborder' rowspan='"+rowSpan+"' valign='top'>";
            // ##7 Change
            if(canModify && canSubmit && (zoomedIn||!p.zoomOnly)) {
                changetoBox = changetoBox+("<input name='"+fieldHash+"' id='"+fieldHash+"_ch' value='"+CHANGETO+"' type='radio' >");
            } else {
                //changetoBox = changetoBox+("<input type='radio' disabled>"); /* don't show the empty input box */
            }
            
            changetoBox=changetoBox+("</td>");
            
            if(!"rtl".equals(ourDir)) {
                ctx.println(changetoBox);
            }
            
            ctx.print("<td class='noborder' rowspan='"+rowSpan+"' valign='top'>");
            
            boolean badInputBox = false;
            
            
            if(areShowingInputBox) {
                String oldValue = (String)ctx.temporaryStuff.get(fieldHash+"_v");
                String fClass = "inputbox";
                if(oldValue==null) {
                    oldValue="";
                } else {
                    ctx.print(ctx.iconHtml("stop","this item was not accepted.")+"this item was not accepted.<br>");
                    //fClass = "badinputbox";
                    badInputBox = true;
                }
                /* if((p.toggleWith != null)&&(p.toggleValue == true)) {
                    ctx.print("<select onclick=\"document.getElementById('"+fieldHash+"_ch').click()\" name='"+fieldHash+"_v'>");
                    ctx.print("  <option value=''></option> ");
                    ctx.print("  <option value='true'>true</option> ");
                    ctx.print("  <option value='false'>false</option> ");
                    ctx.println("</select>");
                } else */ if(p.valuesList != null) {
                    ctx.print("<select onclick=\"document.getElementById('"+fieldHash+"_ch').click()\" name='"+fieldHash+"_v'>");
                    ctx.print("  <option value=''></option> ");
                    for(String s : p.valuesList ) {
                        ctx.print("  <option value='"+s+"'>"+s+"</option> ");
                    }
                    ctx.println("</select>");
                } else {
                    ctx.print("<input onfocus=\"document.getElementById('"+fieldHash+"_ch').click()\" name='"+fieldHash+"_v' value='"+oldValue+"' class='"+fClass+"'>");
                }
                // references
                if(badInputBox) {
                   // ctx.print("</span>");
                }

                if(canModify && zoomedIn && (p.altType == null) && UserRegistry.userIsTC(ctx.session.user)) {  // show 'Alt' popup for zoomed in main items
                    ctx.println ("<br> ");
                    ctx.println ("<span id='h_alt"+fieldHash+"'>");
                    ctx.println ("<input onclick='javascript:show(\"alt" + fieldHash + "\")' type='button' value='Create Alternate'></span>");
                    ctx.println ("<!-- <noscript> </noscript> -->" + 
                                "<span style='display: none' id='alt" + fieldHash + "'>");
                    String[] altList = supplemental.getValidityChoice("$altList");
                    ctx.println ("<label>Alt: <select name='"+fieldHash+"_alt'>");
                    ctx.println ("  <option value=''></option>");
                    for(String s : altList) {
                        ctx.println ("  <option value='"+s+"'>"+s+"</option>");
                    }
                    ctx.println ("</select></label></span>");
                }
               
                  
                ctx.println("</td>");
            } else  {
                if(!zoomedIn && p.zoomOnly) {
                    ctx.println("<i>Must zoom in to edit</i>");
                }
                ctx.println("</td>");
            }

            if("rtl".equals(ourDir)) {
                ctx.println(changetoBox);
            }

        } else {
            areShowingInputBox = false;
            ctx.println("<td rowspan='"+rowSpan+"' colspan='2'></td>");  // no 'changeto' cells.
        }

        // No/Opinion.
        ctx.print("<td colspan='"+
            (zoomedIn?1:2)+"' rowspan='"+rowSpan+"'>");
        if(canModify) {
            ctx.print("<input name='"+fieldHash+"' value='"+DONTCARE+"' type='radio' "
                +((ourVoteXpath==null)?"CHECKED":"")+" >");
        }
        ctx.print("</td>");

        
        ctx.println("</tr>");
        
        // Were there any straggler rows we need to go back and show
        
        if(rowSpan > 1) {
            for(int row=1;row<rowSpan;row++){
                // do the rest of the rows -  ONLY those which did not have rowSpan='rowSpan'. 
                
                ctx.print("<tr>");

                DataSection.DataRow.CandidateItem item = null;

                // current item
                if(currentItems.size() > row) {
                    item = currentItems.get(row);
                    printCells(ctx,section, p,item,fieldHash,p.getResultXpath(),ourVoteXpath,canModify,ourAlign,ourDir,uf,zoomedIn, numberedItemsList, refsList, exampleContext);
                } else {
                    item = null;
                    printEmptyCells(ctx, section, p, ourAlign, ourDir, zoomedIn);
                }

                // #6.1, 6.2 - proposed items
                if(proposedItems.size() > row) {
                    item = proposedItems.get(row);
                    printCells(ctx,section, p,item,fieldHash,p.getResultXpath(),ourVoteXpath,canModify,ourAlign,ourDir,uf,zoomedIn, numberedItemsList, refsList, exampleContext);
                } else {
                    item = null;
                    printEmptyCells(ctx, section, p, ourAlign, ourDir, zoomedIn);
                }
                                
                ctx.println("</tr>");
            }
        }

        // REFERENCE row
        if(areShowingInputBox) {
			ctx.print("<tr><td class='botgray' colspan="+PODTABLE_WIDTH+">");
            // references
            if(zoomedIn) {
                if((refs.length>0) && zoomedIn) {
                    String refHash = fieldHash;
                    Hashtable<String,DataSection.DataRow.CandidateItem> refsItemHash = (Hashtable<String, DataSection.DataRow.CandidateItem>)ctx.temporaryStuff.get("refsItemHash");
                    ctx.print("<label>");
                    ctx.print("<a "+ctx.atarget("ref_"+ctx.getLocale())+" href='"+refCtx.url()+"'>Add/Lookup References</a>");
                    if(areShowingInputBox) {
                        ctx.print("&nbsp;<select name='"+fieldHash+"_r'>");
                        ctx.print("<option value='' SELECTED>(pick reference)</option>");
                        for(int i=0;i<refs.length;i++) {
                            String refValue = null;
                            // 1: look for a confirmed value
                            
                            DataSection.DataRow.CandidateItem refItem = refsItemHash.get(refs[i]);
                            String refValueFull = "";
                            if(refItem != null) {
                                refValueFull = refValue = refItem.value;
                            }
                            if(refValue == null) {
                                refValueFull = refValue = " (null) ";
                            } else {
                                if(refValue.length()>((REFS_SHORTEN_WIDTH*2)-1)) {
                                    refValue = refValue.substring(0,REFS_SHORTEN_WIDTH)+"\u2026"+
                                                refValue.substring(refValue.length()-REFS_SHORTEN_WIDTH); // "..."
                                }
                            }
                            
                            ctx.print("<option title='"+refValueFull+"' value='"+refs[i]+"'>"+refs[i]+": " + refValue+"</option>");
                        }
                        ctx.println("</select>");
                    }
                    ctx.print("</label>");
                } else {
                    ctx.print("<a "+ctx.atarget("ref_"+ctx.getLocale())+" href='"+refCtx.url()+"'>Add Reference</a>");
                }
            }
            ctx.println("</td></tr>");
        }
        
        // now, if the warnings list isn't empty.. warnings rows
        if(!numberedItemsList.isEmpty()) {
            int mySuperscriptNumber =0;
            for(DataSection.DataRow.CandidateItem item : numberedItemsList) {
                mySuperscriptNumber++;
		if(item==null) continue; /* ?! */
                if(item.tests==null && item.examples==null) 
                	continue; /* skip rows w/o anything */
                ctx.println("<tr class='warningRow'><td class='botgray'><span class='warningTarget'>#"+mySuperscriptNumber+"</span></td>");
                if(item.tests != null) {
                    ctx.println("<td colspan='" + (PODTABLE_WIDTH-1) + "' class='warncell'>");
                    for (Iterator it3 = item.tests.iterator(); it3.hasNext();) {
                        CheckCLDR.CheckStatus status = (CheckCLDR.CheckStatus) it3.next();
                        if (!status.getType().equals(status.exampleType)) {
                            ctx.println("<span class='warningLine'>");
                            String cls = shortClassName(status.getCause());
                            if(status.getType().equals(status.errorType)) {
                                ctx.print(ctx.iconHtml("stop","Error: "+cls));
                            } else {
                                ctx.print(ctx.iconHtml("warn","Warning: "+cls));
                            }
                            ctx.print(status.toString());
                            ctx.println("</span>");
                            if(cls != null) {
                                ctx.printHelpLink("/"+cls+"","Help");
                            }
                            ctx.print("<br>");
                        }
                    }
                } else {
                    ctx.println("<td colspan='" + (PODTABLE_WIDTH-1) + "' class='examplecell'>");
                }
                if(item.examples != null) {
                    boolean first = true;
                    for(Iterator it4 = item.examples.iterator(); it4.hasNext();) {
                        DataSection.ExampleEntry e = (DataSection.ExampleEntry)it4.next();
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
                            String theMenu = PathUtilities.xpathToMenu(xpt.getById(p.parentRow.base_xpath));
                            if(theMenu==null) {
                                theMenu="raw";
                            }
                            ctx.print("<a "+ctx.atarget("st:Ex:"+ctx.getLocale())+" href='"+ctx.url()+ctx.urlConnector()+"_="+ctx.getLocale()+"&amp;x="+theMenu+"&amp;"+ QUERY_EXAMPLE+"="+e.hash+"'>");
                            ctx.print(ctx.iconHtml("zoom","Zoom into "+cls)+cls);
                            ctx.print("</a>");
                        }
                        first = false;
                    }
                }
                ctx.println("</td>");
                ctx.println("</tr>");
            }
        }
        
		

        if(zoomedIn && !isPhaseSubmit() && !isPhaseBeta()) {
	    
	    long totals[] = new long[numberedItemsList.size()];
	    for(int j=0;j<totals.length;j++) {
	    	totals[j]=0;
	    }

	    ctx.print("<tr>");
	    ctx.print("<th colspan=2>Votes</th>");
	    ctx.print("<td colspan="+(PODTABLE_WIDTH-2)+">");
			
	    try {
		Race r =  vet.getRace(section.locale, p.base_xpath);
		ctx.println("<i>Voting results by organization:</i><br>");
                ctx.print("<table class='list' border=1 summary='voting results by organization'>");
                ctx.print("<tr class='heading'><th>Organization</th><th>Organization's Vote</th><th>Item</th><th>Score</th><th>Conflicting Votes</th></tr>");
                int onn=0;
		for(Race.Organization org : r.orgVotes.values()) {
		    Race.Chad orgVote = r.getOrgVote(org.name);				    
		    Map<Integer,Long> o2v = r.getOrgToVotes(org.name);
/*
		    System.err.println("org:"+org.name);
		    for(int i : o2v.keySet()) {
			long l = o2v.get(i);
		    	System.err.println(" "+i+"->"+l);
		     }
					
*/
                    ctx.println("<tr class='row"+(onn++ % 2)+"'>");
		    long score=0;

		    CandidateItem oitem = null;
		    int on = -1;
		    if(orgVote != null)
                	{
			    int nn=0;
	                    for(CandidateItem citem : numberedItemsList) {
				nn++;
				if(citem==null) continue;
				if(orgVote.xpath==citem.xpathId) {
				    oitem = citem;
				    on=nn;
				}
	                    }
			    if(oitem!=null) {
				Long l = o2v.get(oitem.xpathId);
				if(l != null) {
				    score = l;
//				    System.err.println(org.name+": ox " + oitem.xpathId + " -> l " + l + ", nn="+nn);
				    if(on>=0) {
					totals[on-1]+=score;
				    }
				}
			    }
			}
                    
		    ctx.print("<th>"+org.name+"</th>");
		    ctx.print("<td>");
		    if(orgVote == null) {
			ctx.print("<i>(No vote.)</i>");
			if(org.dispute) {
			    ctx.print("<br>");
			    if((ctx.session.user != null) && (org.name.equals(ctx.session.user.org))) {
				ctx.print(ctx.iconHtml("disp","Vetter Dispute"));
			    }
			    ctx.print(" (Dispute among "+org.name+" vetters) ");
			}
		    } else {
			String theValue = orgVote.value;
			if(theValue == null) {  
			    if(orgVote.xpath == r.base_xpath) {
				theValue = "<i>(Old Vote for Status Quo)</i>";
			    } else {
				theValue = "<strike>(Old Vote for Other Item)</strike>";
			    }
			}
			if(orgVote.disqualified) {
			    ctx.print("<strike>");
			}
			ctx.print("<span dir='"+ourDir+"'>");
			//						ctx.print(VoteResolver.getOrganizationToMaxVote(section.locale).
			//						            get(VoteResolver.Organization.valueOf(org.name)).toString().replaceAll("street","guest")
			ctx.print(ctx.iconHtml("vote","#"+orgVote.xpath)+theValue+"</span>");
			if(orgVote.disqualified) {
			    ctx.print("</strike>");
			}
			if(org.votes.isEmpty()/* && (r.winner.orgsDefaultFor!=null) && (r.winner.orgsDefaultFor.contains(org))*/) {
			    ctx.print(" (default vote)");
			}
			if(true||UserRegistry.userIsTC(ctx.session.user)) {
			    ctx.print("<br>");
			    for(UserRegistry.User u:orgVote.voters) {
				if(!u.voterOrg().equals(org.name)) continue;
				ctx.print(u.toHtml(ctx.session.user)+", ");
			    }
			}
		    }
		    ctx.print("</td>");
		    
		    ctx.print("<td class='warningReference'>#"+on+"</td> ");
		    
		    ctx.print("<td>"+score+"</td>");
		    
		    ctx.print("<td>");
		    if(!org.votes.isEmpty()) for(Race.Chad item : org.votes) {
			    if(item==orgVote) continue;
			    
			    String theValue = item.value;
			    if(theValue == null) {  
				if(item.xpath == r.base_xpath) {
				    theValue = "<i>(Old Vote for Status Quo)</i>";
				} else {
				    theValue = "<strike>(Old Vote for Other Item"+")</strike>";
				}
			    }
			    if(item.disqualified) {
				ctx.print("<strike>");
			    }
			    ctx.print("<span dir='"+ourDir+"' class='notselected' title='#"+item.xpath+"'>"+theValue+"</span>");
			    if(item.disqualified) {
				ctx.print("</strike>");
			    }
			    ctx.print("<br>");
			    for(UserRegistry.User u : item.voters)  { 
				if(!u.voterOrg().equals(org.name)) continue;
				ctx.print(u.toHtml(ctx.session.user)+", ");
			    }
			    ctx.println("<hr>");
			}
		    ctx.println("</td>");
		    ctx.print("</tr>");
		}
		ctx.print("</table>"); // end of votes-by-organization
		
		if(isUnofficial || UserRegistry.userIsTC(ctx.session.user)) {
		    ctx.println("<pre style='border: 1px solid gray; margin: 3px;'>"+r.resolverToString());
		    ctx.print("</pre>");
		}
		
                  
		if((r.nexthighest > 0) && (r.winner!=null)&&(r.winner.score==0)) {
		    // This says that the optimal value was NOT the numeric winner.
		    ctx.print("<i>not enough votes to overturn approved item</i><br>");
		} else if(!r.disputes.isEmpty()) {
		    ctx.print(" "+ctx.iconHtml("warn","Warning")+"Disputed with: ");
		    for(Race.Chad disputor : r.disputes) {
			ctx.print("<span title='#"+disputor.xpath+"'>"+disputor.value+"</span> ");
		    }
		    ctx.print("");
		    ctx.print("<br>");
		} else if(r.hadDisqualifiedWinner) {
		    ctx.print("<br><b>"+ctx.iconHtml("warn","Warning")+"Original winner of votes was disqualified due to errors.</b><br>");
		}
		if(isUnofficial && r.hadOtherError) {
		    ctx.print("<br><b>"+ctx.iconHtml("warn","Warning")+"Had Other Error.</b><br>");
		}
                
		ctx.print("<br><hr><i>Voting results by item:</i>");
		ctx.print("<table class='list' border=1 summary='voting results by item'>");
		ctx.print("<tr class='heading'><th>Value</th><th>Item</th><th>Score</th><th>O/N</th><th>Status 1.6</th><th>Status 1.7</th></tr>");
		int nn=0;

		int lastReleaseXpath = r.getLastReleaseXpath();
		String lastReleaseStatus = r.getLastReleaseStatus().toString();

		for(CandidateItem citem : numberedItemsList) {
		    ctx.println("<tr class='row"+(nn++ % 2)+"'>");
		    if(citem==null) {ctx.println("</tr>"); continue; } 
		    String theValue = citem.value;
		    String title="X#"+citem.xpathId;
                    
		    // find Chad item that matches citem
                	
                    long score = -1;
		    Race.Chad item = null;
		    if(citem.inheritFrom==null) {
			for(Race.Chad anitem : r.chads.values()) {
			    if(anitem.xpath==citem.xpathId) {
				item = anitem;
				title="#"+item.xpath;
			    }
			}
		    }
                	
		    //for(Race.Chad item : r.chads.values()) {
		    if(item!=null&&theValue == null) {  
			if(item.xpath == r.base_xpath) {
			    theValue = "<i>(Old Vote for Status Quo)</i>";
			} else {
			    theValue = "<strike>(Old Vote for Other Item"+")</strike>";
			}
		    }
                    
		    ctx.print("<td>");
		    if(item!=null) {
			if(item == r.winner) {
			    ctx.print("<b>");
			}
			if(item.disqualified) {
			    ctx.print("<strike>");
			}
		    }
		    ctx.print("<span dir='"+ourDir+"' title='"+title+"'>"+theValue+"</span> ");
		    if(item!=null) {
			if(item.disqualified) {
			    ctx.print("</strike>");
			}
			if(item == r.winner) {
			    ctx.print("</b>");
			}
			if(item == r.existing) {
			    ctx.print(ctx.iconHtml("star","existing item"));
			}
		    }
		    ctx.print("</td>");

		    ctx.println("<th class='warningReference'>#"+nn+"</th>");
		    if(item!=null) {
                    	ctx.print("<td>"+ totals[nn-1] +"</td>");
			if(item == r.Ochad) {
			    ctx.print("<td>O</td>");
			} else if(item == r.Nchad) {
			    ctx.print("<td>N</td>");
			} else {
			    ctx.print("<td></td>");
			}
			if(item.xpath == lastReleaseXpath) {
			    ctx.print("<td>"+lastReleaseStatus.toString().toLowerCase()+"</td>");
			} else {
			    ctx.print("<td></td>");
			}
			if(item == r.winner) {
			    ctx.print("<td>"+r.vrstatus.toString().toLowerCase()+"</td>");
			} else {
			    ctx.print("<td></td>");
			}
                    	
		    } else {
			/* no item */
			if(citem.inheritFrom!=null) {
			    ctx.println("<td colspan=4><i>Inherited from "+citem.inheritFrom+"</i></td>");
			} else {
			    ctx.println("<td colspan=4><i>Item not found!</i></td>");
			}
		    }
		    ctx.print("</tr>");
		}
		ctx.print("</table>");
		//if(UserRegistry.userIsTC(ctx.session.user)) {
		if(r.winner != null ) {
		    CandidateItem witem = null;
		    int wn = -1;
		    nn=0;
		    for(CandidateItem citem : numberedItemsList) {
			nn++;
			if(citem == null) continue;
			if(r.winner.xpath==citem.xpathId) {
			    witem = citem;
			    wn=nn;
			}
		    }
		    ctx.print("<b class='selected'>Optimal field</b>: #"+wn+" <span dir='"+ourDir+"' class='winner' title='#"+r.winner.xpath+"'>"+r.winner.value+"</span>, " + r.vrstatus + ", <!-- score: "+r.winner.score +" -->");
		}
		                
		ctx.println("For more information, see <a href='http://cldr.unicode.org/index/process#Voting_Process'>Voting Process</a><br>");
	    } catch (SQLException se) {
		ctx.println("<div class='ferrbox'>Error fetching vetting results:<br><pre>"+se.toString()+"</pre></div>");
	    }
            
	    ctx.println("</td></tr>");
            
            if(p.aliasFromLocale != null) {
                ctx.print("<tr class='topgray'>");
                ctx.print("<th class='topgray' colspan=2>Alias Items</th>");
                ctx.print("<td class='topgray' colspan="+(PODTABLE_WIDTH-2)+">");
                
                
                String theURL = fora.forumUrl(ctx, p.aliasFromLocale, p.aliasFromXpath);
                String thePath = xpt.getPrettyPath(p.aliasFromXpath);
                String theLocale = p.aliasFromLocale;

                ctx.println("Note: Before making changes here, first verify:<br> ");
                ctx.print("<a class='alias' href='"+theURL+"'>");
                ctx.print(thePath);
                if(!p.aliasFromLocale.equals(section.locale)) {
                    ctx.print(" in " + new ULocale(theLocale).getDisplayName(ctx.displayLocale));
                }
                ctx.print("</a>");
                ctx.print("");
                
                ctx.println("</td></tr>");
            }
		}
    }
    
    /**
     * Print empty cells. Not the equivalent of printCells(..., null), which will still print an example.
     * @param ctx
     * @param section
     * @param p
     */
    void printEmptyCells(WebContext ctx, DataSection section, DataSection.DataRow p, String ourAlign, String ourDir, boolean zoomedIn) {
        int colspan = zoomedIn?1:2;
        boolean haveTests = false;
        boolean haveReferences = false; // no item
        ctx.print("<td  colspan='"+colspan+"' class='propcolumn' align='"+ourAlign+"' dir='"+ourDir+"' valign='top'>");
        ctx.println("</td>");    
        if(zoomedIn) {
            ctx.println("<td></td>"); // no tests, no references
        }
        // ##6.2 example column. always present
        ctx.println("<td></td>");
    }

    /**
     *  print the cells which have to do with the item
     * this may be called with NULL if there isn't a proposed item for this slot.
     * it will be called once in the 'main' row, and once for any extra rows needed for proposed items
     * @param numberedItemsList All items are added here.
     */ 
    void printCells(WebContext ctx, DataSection section, DataSection.DataRow p, DataSection.DataRow.CandidateItem item, String fieldHash, String resultXpath, String ourVoteXpath,
        boolean canModify, String ourAlign, String ourDir, UserLocaleStuff uf, boolean zoomedIn, List<DataSection.DataRow.CandidateItem> numberedItemsList, List<String> refsList,
        ExampleContext exampleContext) {
        // ##6.1 proposed - print the TOP item
        
        int colspan = zoomedIn?1:2;
        String itemExample = null;
        boolean haveTests = false;
        boolean haveReferences = (item != null) && (item.references!=null) && (refsList != null);
        
        if(item != null) {
            itemExample = uf.getExampleGenerator().getExampleHtml(item.xpath, item.value,
                        zoomedIn?ExampleGenerator.Zoomed.IN:ExampleGenerator.Zoomed.OUT, exampleContext, ExampleType.NATIVE);
            if((item.tests != null) || (item.examples != null)) {
                haveTests = true;
            }
        } else {
            itemExample = uf.getExampleGenerator().getExampleHtml(section.xpath(p), null,
                    zoomedIn?ExampleGenerator.Zoomed.IN:ExampleGenerator.Zoomed.OUT, exampleContext, ExampleType.NATIVE);
        }
        
        ctx.print("<td  colspan='"+colspan+"' class='propcolumn' align='"+ourAlign+"' dir='"+ourDir+"' valign='top'>");
        if((item != null)&&(item.value != null)) {
            printDataRowItem(ctx, p, item, fieldHash, resultXpath, ourVoteXpath, canModify, zoomedIn, exampleContext);
        }
        ctx.println("</td>");    
        // 6.3 - If we are zoomed in, we WILL have an additional column withtests and/or references.
        if(zoomedIn) {
            if(true || haveTests || haveReferences) {
                if(true || item.tests != null) {
                    ctx.println("<td nowrap class='warncell'>");
                    ctx.println("<span class='warningReference'>");
                    //ctx.print(ctx.iconHtml("warn","Warning"));
                } else {
                    ctx.println("<td nowrap class='examplecell'>");
                    ctx.println("<span class='warningReference'>");
                }
                if(item!=null /* always number -- was haveTests*/) {
                    numberedItemsList.add(item);
                    int mySuperscriptNumber = numberedItemsList.size();  // which # is this item?
                    ctx.println("#"+mySuperscriptNumber+"</span>");
                    if(haveReferences) {
                        ctx.println("<br>");
                    }
                }
                if(haveReferences) {
                    int myNumber = refsList.indexOf(item.references);
                    if(myNumber == -1) {                
                        myNumber = refsList.size();
                        refsList.add(item.references);
                    }
                    myNumber++; // 1 based
                    ctx.print("<span class='referenceReference'><img src='http://unicode.org/cldr/data/dropbox/misc/images/reference.jpg'>#"+myNumber+"</span>");
                }
                ctx.println("</td>");
            } else {
                ctx.println("<td></td>"); // no tests, no references
            }
        }

        // ##6.2 example column. always present
        if(itemExample!=null) {
            ctx.print("<td class='generatedexample' valign='top' align='"+ourAlign+"' dir='"+ourDir+"' >");
            ctx.print(itemExample.replaceAll("\\\\","\u200b\\\\")); // \u200bu
            ctx.println("</td>");
        } else {
            ctx.println("<td></td>");
        }
    }
    
    void printItemTypeName(WebContext ctx, DataRow p, boolean canModify, boolean zoomedIn)  {
    	printItemTypeName(ctx, p, canModify, zoomedIn, null);
    }
    void printItemTypeName(WebContext ctx, DataRow p, boolean canModify, boolean zoomedIn, String specialUrl)  {
        String disputeIcon = "";
        if(canModify) {
            if(vet.queryOrgDispute(ctx.session.user.voterOrg(), p.getLocale(), p.base_xpath)) {
                disputeIcon = ctx.iconHtml("disp","Vetter Dispute");
            }
        }
        ctx.print("<tt title='"+xpt.getPrettyPath(p.base_xpath)+"' >");
        String typeShown = p.type.replaceAll("/","/\u200b");
        if(!zoomedIn) {
            if(specialUrl != null) {
                ctx.print("<a class='notselected' target='"+ctx.atarget("n:"+ctx.getLocale().toString())+"' href='"+specialUrl+"'>"+typeShown+disputeIcon+"</a>");
            } else {
                fora.showForumLink(ctx,p,p.parentRow.base_xpath,typeShown+disputeIcon);
            }
        } else {
            ctx.print(typeShown+disputeIcon);
        }
        ctx.print("</tt>");
    }

    

    static final UnicodeSet CallOut = new UnicodeSet("[\\u200b-\\u200f]");

    void printDataRowItem(WebContext ctx, DataSection.DataRow p, DataSection.DataRow.CandidateItem item, String fieldHash, 
            String resultXpath, String ourVoteXpath, boolean canModify, boolean zoomedIn, ExampleContext exampleContext) {
    if(false)ctx.println("<div style='border: 2px dashed red'>altProposed="+item.altProposed+", inheritFrom="+item.inheritFrom+", pathWhereFound="+item.pathWhereFound+", confirmOnly="+new Boolean(p.confirmOnly)+"</div><br>");
        boolean winner = 
            ((resultXpath!=null)&&
            (item.xpath!=null)&&
            (item.xpath.equals(resultXpath))&& // todo: replace string.equals with comparison to item.xpathId . .
            !item.isFallback);
        boolean fallback = false;

        String pClass ="";
        if(winner) {
            if(p.confirmStatus == Vetting.Status.APPROVED) {
                pClass = "class='winner' title='Winning item.'";
            } else if(p.confirmStatus != Vetting.Status.INDETERMINATE) {
                pClass = "title='"+p.confirmStatus+"' ";
            }
        } else if(item.pathWhereFound != null) {
            fallback = true;
            pClass = "class='alias' title='alias from "+xpt.getPrettyPath(item.pathWhereFound)+"'";
        } else if (item.isFallback || (item.inheritFrom != null) /*&&(p.inheritFrom==null)*/) {
            fallback = true; // this item is nver 'in' this localel
            if(XMLSource.CODE_FALLBACK_ID.equals(item.inheritFrom)) {
                pClass = "class='fallback_code' title='Untranslated Code'";
            } else if("root".equals(item.inheritFrom)) {
                pClass = "class='fallback_root' title='Fallback from Root'";
            } else {
                pClass = "class='fallback' title='Translated in "+new ULocale(item.inheritFrom).getDisplayName(ctx.displayLocale)+" and inherited here.'";
            }
        } else if(item.altProposed != null) {
            pClass = "class='loser' title='proposed, losing item'";
    /*    } else if(p.inheritFrom != null) {
            pClass = "class='missing'"; */
        } else {
            pClass = "class='loser'";
        }


        if(true /* !item.itemErrors */) {  // exclude item from voting due to errors?
            if(canModify) {      
                boolean checkThis = 
                    ((ourVoteXpath!=null)&&
                    (item.xpath!=null)&&
                    (item.xpath.equals(ourVoteXpath)));
                
                if(!item.isFallback) {
                    ctx.print("<input title='#"+item.xpathId+"' name='"+fieldHash+"'  value='"+
                        ((item.altProposed!=null)?item.altProposed:CONFIRM)+"' "+(checkThis?"CHECKED":"")+"  type='radio'>");
                } else {
                    ctx.print("<input title='#"+item.xpathId+"' name='"+fieldHash+"'  value='"+INHERITED_VALUE+"' type='radio'>");
                }
            } else {
                ctx.print("<input title='#"+item.xpathId+"' type='radio' disabled>");
            }
        }

        if(zoomedIn && (item.getVotes() != null)) {
            int n = item.getVotes().size();
            String title=""+ n+" vote"+((n>1)?"s":"");
            if(canModify&&UserRegistry.userIsVetter(ctx.session.user)) {
                title=title+": ";
				boolean first = true;
                for(UserRegistry.User theU : item.getVotes()) {
                    if(theU != null) {
                        String add= theU.name + " of " + theU.org;
                        title = title + add.replaceAll("'","\u2032"); // quote quotes
                        if(first) {
                            title = title+", ";
                        } else {
							first = false;
						}
                    }
                }
            }
            ctx.print("<span class='notselected' >"+ctx.iconHtml("vote",title)+"</span>"); // ballot box symbol
        }

        ctx.print("<span "+pClass+">");
        String processed = null;
        if(item.value.length()!=0) {
            processed = ctx.processor.processForDisplay(xpt.getById(p.base_xpath),item.value);
            ctx.print(processed);
        } else {
            ctx.print("<i dir='ltr'>(empty)</i>");
            processed = null;
        }
        ctx.print("</span>");
        if((!fallback||((p.previousItem==null)&&item.isParentFallback)||  // it's either: not inherited OR not a "shim"  and..
            (item.pathWhereFound != null && !item.isFallback )) &&    // .. or it's an alias (that is still the 1.4 item)
            item.xpathId == p.base_xpath) {   // its xpath is the base xpath.
            ctx.print(ctx.iconHtml("star","CLDR "+getOldVersion()+" item"));
        } else if (isUnofficial && item.isParentFallback) {
//            ctx.print(ctx.iconHtml("okay","parent fallback"));
        }
        
        if((ctx.session.user!=null)&& (zoomedIn || ctx.prefBool(PREF_DELETEZOOMOUT))) {
            boolean adminOrRelevantTc = UserRegistry.userIsAdmin(ctx.session.user); //  ctx.session.user.isAdminFor(reg.getInfo(item.submitter));
            if( (item.getVotes() == null) ||  adminOrRelevantTc || // nobody voted for it, or
                ((item.getVotes().size()==1)&& item.getVotes().contains(ctx.session.user) ))  { // .. only this user voted for it
                boolean deleteHidden = ctx.prefBool(PREF_NOSHOWDELETE);
                if(!deleteHidden && canModify && (item.submitter != -1) &&
                    !( (item.pathWhereFound != null) || item.isFallback || (item.inheritFrom != null) /*&&(p.inheritFrom==null)*/) && // not an alias / fallback / etc
                    ( adminOrRelevantTc || (item.submitter == ctx.session.user.id) ) ) {
                        ctx.println(" <label nowrap class='deletebox' style='padding: 4px;'>"+ "<input type='checkbox' title='#"+item.xpathId+
                            "' value='"+item.altProposed+"' name='"+fieldHash+ACTION_DEL+"'>" +"Delete&nbsp;item</label>");
                }
            }
        }
        if(UserRegistry.userIsTC(ctx.session.user) && ctx.prefBool(PREF_SHOWUNVOTE) &&item.votesByMyOrg(ctx.session.user)) {
            ctx.println(" <label nowrap class='unvotebox' style='padding: 4px;'>"+ "<input type='checkbox' title='#"+item.xpathId+
                    "' value='"+item.altProposed+"' name='"+fieldHash+ACTION_UNVOTE+"'>" +"Unvote&nbsp;item</label>");
        }
        if(zoomedIn) {
            if(processed != null && /* ourDir.equals("rtl")&& */ CallOut.containsSome(processed)) {
                String altProcessed = processed.replaceAll("\u200F","\u200F<b dir=rtl>RLM</b>\u200F")
                                               .replaceAll("\u200E","\u200E<b>LRM</b>\u200E");
                ctx.print("<br><span class=marks>" + altProcessed + "</span>");
            }
        }
        
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
        WebContext nuCtx = (WebContext)ctx.clone();
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

    public int showSkipBox(WebContext ctx, int total, DataSection.DisplaySet displaySet,
        boolean showSearchMode, String sortMode, int skip) {
    showSearchMode = true;// all
        List displayList = null;
        if(displaySet != null) {
            displayList = displaySet.displayRows;
        }
        ctx.println("<div class='pager' style='margin: 2px'>");
        if((ctx.getLocale() != null) && UserRegistry.userCanModifyLocale(ctx.session.user,ctx.getLocale())) { // at least street level
            if((ctx.field(QUERY_SECTION).length()>0) && !ctx.field(QUERY_SECTION).equals(xMAIN)) {
                ctx.println("<input  type='submit' value='" + getSaveButtonText() + "'>"); // style='float:left'
            }
        }
        // TODO: replace with ctx.fieldValue("skip",-1)
        if(skip<=0) {
            skip = 0;
        } 

        // calculate nextSkip
        int from = skip+1;
        int to = from + ctx.prefCodesPerPage()-1;
        if(to >= total) {
            to = total;
        }

        if(showSearchMode) {
            ctx.println("<div style='float: right;'>Items " + from + " to " + to + " of " + total+"</div>");        
            ctx.println("<p class='hang' > " +  //float: right; tyle='margin-left: 3em;'
                "<b>Sorted:</b>  ");
            {
                boolean sortAlpha = (sortMode.equals(PREF_SORTMODE_ALPHA));
                
                //          showSkipBox_menu(ctx, sortMode, PREF_SORTMODE_ALPHA, "Alphabetically");
                showSkipBox_menu(ctx, sortMode, PREF_SORTMODE_CODE, "Code");
                showSkipBox_menu(ctx, sortMode, PREF_SORTMODE_WARNING, "Priority");
                if(displaySet.canName) {
                    showSkipBox_menu(ctx, sortMode, PREF_SORTMODE_NAME, this.BASELINE_NAME+"-"+"Name");
                }
            }
            
            {
                WebContext subCtx = (WebContext)ctx.clone();
                if(skip > 0) {
                    subCtx.setQuery("skip",new Integer(skip).toString());
                }
            }
            
            
            ctx.println("</p>");
        }

        // Print navigation
        if(showSearchMode) {

            if(total>=(ctx.prefCodesPerPage())) {
                int prevSkip = skip - ctx.prefCodesPerPage();
                if(prevSkip<0) {
                    prevSkip = 0;
                }
                ctx.print("<p class='hang'>");
                if(skip<=0) {
                    ctx.print("<span class='pagerl_inactive'>\u2190&nbsp;prev"/* + ctx.prefCodesPerPage() */ + "" +
                            "</span>&nbsp;");  
                } else {
                    ctx.print("<a class='pagerl_active' href=\"" + ctx.url() + 
                            ctx.urlConnector() + "skip=" + new Integer(prevSkip) + "\">" +
                            "\u2190&nbsp;prev"/* + ctx.prefCodesPerPage()*/ + "");
                    ctx.print("</a>&nbsp;");
                }
                int nextSkip = skip + ctx.prefCodesPerPage(); 
                if(nextSkip >= total) {
                    nextSkip = -1;
                    if(total>=(ctx.prefCodesPerPage())) {
                        ctx.println(" <span class='pagerl_inactive' >" +
                                    "next&nbsp;"/* + ctx.prefCodesPerPage()*/ + "\u2192" +
                                    "</span>");
                    }
                } else {
                    ctx.println(" <a class='pagerl_active' href=\"" + ctx.url() + 
                                ctx.urlConnector() +"skip=" + new Integer(nextSkip) + "\">" +
                                "next&nbsp;"/* + ctx.prefCodesPerPage()*/ + "\u2192" +
                                "</a>");
                }
                ctx.print("</p>");
            }
//            ctx.println("<br/>");
        }

        if(total>=(ctx.prefCodesPerPage())) {
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
                for(int i=ourStart;i<ourLimit;i = (i - (i%ctx.prefCodesPerPage())) + ctx.prefCodesPerPage()) {
                    int pageStart = i - (i%ctx.prefCodesPerPage()); // pageStart is the skip at the top of this page. should be == ourStart unless on a boundary.
                    int end = pageStart + ctx.prefCodesPerPage()-1;
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
                        if(iString.length() > PAGER_SHORTEN_WIDTH) {
                            iString = iString.substring(0,PAGER_SHORTEN_WIDTH)+"\u2026";
                        }
                        ctx.print(iString);
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
            pw.println("## Current SurveyTool phase ");
            pw.println("CLDR_PHASE="+Phase.BETA.name());
            pw.println();
            pw.println("## 'old' (previous) version");
            pw.println("CLDR_OLDVERSION=CLDR_OLDVERSION");
            pw.println();
            pw.println("## 'new'  version");
            pw.println("CLDR_NEWVERSION=CLDR_NEWVERSION");
            pw.println();
            pw.println("## Current SurveyTool phase ");
            pw.println("CLDR_PHASE="+Phase.BETA.name());
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
    
    /**
     * Class to startup ST in background and perform background operations.
     */
    SurveyThread startupThread = new SurveyThread(this);
    

    /**
     * Startup function. Called from another thread.
     * @throws ServletException
     */
    public synchronized void doStartup() throws ServletException {
        if(isSetup == true) {
            return;
        }
        
        ElapsedTimer setupTime = new ElapsedTimer();

        try {
            setProgress("Main Startup");
            // set up CheckCLDR
            //CheckCLDR.SHOW_TIMES=true;
    
            updateProgress(0);
            int nn = progressCount;
            
            updateProgress(nn++, "Initializing..");
            
            survprops = new java.util.Properties(); 
    
            if(cldrHome == null) {
                cldrHome = System.getProperty("catalina.home");
                if(cldrHome == null) {  
                    busted("no $(catalina.home) set - please use it or set a servlet parameter cldr.home");
                    return;
                } 
                homeFile = new File(cldrHome, "cldr");
                File propFile = new java.io.File(homeFile, "cldr.properties");
    
                if(!propFile.exists()) {
                    System.err.println("Does not exist: "+propFile.getAbsolutePath());
                    createBasicCldr(homeFile); // attempt to create
                }
    
                if(!homeFile.exists()) {
                    busted("$(catalina.home)/cldr isn't working as a CLDR home. Not a directory: " + homeFile.getAbsolutePath());
                    return;
                }
                cldrHome = homeFile.getAbsolutePath();
            }
    
            logger.info("SurveyTool starting up. root=" + new File(cldrHome).getAbsolutePath());
            updateProgress(nn++, "Loading configuration");
    
            try {
                java.io.FileInputStream is = new java.io.FileInputStream(new java.io.File(cldrHome, "cldr.properties"));
                survprops.load(is);
                updateProgress(nn++, "Loading configuration..");
                is.close();
            } catch(java.io.IOException ioe) {
                /*throw new UnavailableException*/
                logger.log(java.util.logging.Level.SEVERE, "Couldn't load cldr.properties file from '" + cldrHome + "/cldr.properties': ",ioe);
                busted("Couldn't load cldr.properties file from '" + cldrHome + "/cldr.properties': ", ioe);
                return;
            }
            
            updateProgress(nn++, "Setup DB config");
            // set up DB properties
            setupDBProperties(survprops);
            updateProgress(nn++, "Setup phase..");
            
            // phase
            {
                String phaseString = survprops.getProperty("CLDR_PHASE",null);
                try {
                    if(phaseString!=null) {
                        currentPhase = (Phase.valueOf(phaseString));
                    }
                } catch(IllegalArgumentException iae) {
                    System.err.println("Error trying to parse CLDR_PHASE: " + iae.toString());
                }
                if(currentPhase == null) {
                    StringBuffer allValues = new StringBuffer();
                    for (Phase v : Phase.values()) {
                         allValues.append(v.name());
                         allValues.append(' ');
                    }
                    busted("Could not parse CLDR_PHASE - should be one of ( "+allValues+") but instead got "+phaseString);
                }
            }
            updateProgress(nn++, "Setup props..");
            newVersion=survprops.getProperty("CLDR_NEWVERSION","CLDR_NEWVERSION");
            oldVersion=survprops.getProperty("CLDR_OLDVERSION","CLDR_OLDVERSION");
    
            vetdata = survprops.getProperty("CLDR_VET_DATA", cldrHome+"/vetdata"); // dir for vetted data
            updateProgress(nn++, "Setup dirs.."); 
            vetdir = new File(vetdata);
            if(!vetdir.isDirectory()) {
                vetdir.mkdir();
                System.err.println("## creating empty vetdir: " + vetdir.getAbsolutePath());
            }
            if(!vetdir.isDirectory()) {
                busted("CLDR_VET_DATA isn't a directory: " + vetdata);
                return;
            }
    //        if(false && (loggingHandler == null)) { // TODO: switch? Java docs seem to be broken.. following doesn't work.
    //            try {
    //                loggingHandler = new java.util.logging.FileHandler(vetdata + "/"+LOGFILE,0,1,true);
    //                loggingHandler.setFormatter(new java.util.logging.SimpleFormatter());
    //                logger.addHandler(loggingHandler);
    //                logger.setUseParentHandlers(false);
    //            } catch(Throwable ioe){
    //                busted("Couldn't add log handler for logfile (" + vetdata+"/"+LOGFILE +"): " + ioe.toString());
    //                return;
    //            }
    //        }
    
            updateProgress(nn++, "Setup vap and message..");
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
            
            lockOut = survprops.getProperty("CLDR_LOCKOUT");
            
            if(!new File(fileBase).isDirectory()) {
                busted("CLDR_COMMON isn't a directory: " + fileBase);
                return;
            }
    
            if(!new File(vetweb).isDirectory()) {
                busted("CLDR_VET_WEB isn't a directory: " + vetweb);
                return;
            }
            updateProgress(nn++, "Setup cache..");
    
            File cacheDir = new File(cldrHome, "cache");
       //     logger.info("Cache Dir: " + cacheDir.getAbsolutePath() + " - creating and emptying..");
            CachingEntityResolver.setCacheDir(cacheDir.getAbsolutePath());
            CachingEntityResolver.createAndEmptyCacheDir();

            updateProgress(nn++, "Setup supplemental..");

            supplemental = new SupplementalData(fileBase + "/../supplemental/");
    
    
    
    
          //  int status = 0;
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
            updateProgress(nn++, "Setup warnings..");
            if(!readWarnings()) {
                // already busted
                return;
            }
            
            updateProgress(nn++, "Setup baseline file..");
            
            // load baseline file
            getBaselineFile();
            
            updateProgress(nn++, "Setup baseline example..");
            
            // and example
            getBaselineExample();

            updateProgress(nn++, "Wake up the database..");
            
            
            doStartupDB(); // will take over progress 50-60
	} catch(Throwable t) {
	    busted("Error on startup: ", t);
        } finally {
            clearProgress(); // at least clear the progress bar.
        }
        
        /** 
         * Cause locale alias to be checked.
         */
//        if(!SurveyMain.isUnofficial) { // only do this for official
            isLocaleAliased(CLDRLocale.ROOT);
       // }
        
        logger.info("SurveyTool ready for requests after "+setupTime+". Memory in use: " + usedK());
        isSetup = true;
    }

    public void destroy() {
        logger.warning("SurveyTool shutting down..");
        if(startupThread!=null) {
        	startupThread.attemptCleanShutdown();
        }
        doShutdownDB();
        if(isBusted!=null) isBusted="servlet destroyed";
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
    
    static FileFilter getXmlFileFilter() {
    	return new FileFilter() {
            public boolean accept(File f) {
                String n = f.getName();
                return(!f.isDirectory()
                       &&n.endsWith(".xml")
                       &&!n.startsWith(".")
                       &&!n.startsWith("supplementalData") // not a locale
                       /*&&!n.startsWith("root")*/); // root is implied, will be included elsewhere.
            }
        };
    }

    static protected File[] getInFiles() {
    	return getInFiles(fileBase);
    }
    
    static protected File[] getInFiles(String base) {
        File baseDir = new File(fileBase);
        return getInFiles(baseDir);
    }
    
    static protected File[] getInFiles(File baseDir) {
        // 1. get the list of input XML files
        FileFilter myFilter = getXmlFileFilter();
        return baseDir.listFiles(myFilter);
    }
    
    static protected CLDRLocale getLocaleOf(File localeFile) {
		String localeName = localeFile.getName();
		return getLocaleOf(localeName);
    }
    
    static protected CLDRLocale getLocaleOf(String localeName) {
		int dot = localeName.indexOf('.');
		String theLocale = localeName.substring(0,dot);
		return CLDRLocale.getInstance(theLocale);
    }
    
    private static Set<CLDRLocale> localeListSet = null;

    static synchronized public Set<CLDRLocale> getLocalesSet() {
        if(localeListSet == null ) {
            File inFiles[] = getInFiles();
            int nrInFiles = inFiles.length;
            Set<CLDRLocale> s = new HashSet<CLDRLocale>();
            for(int i=0;i<nrInFiles;i++) {
                String fileName = inFiles[i].getName();
                int dot = fileName.indexOf('.');
                if(dot !=  -1) {
                    String locale = fileName.substring(0,dot);
                    s.add(CLDRLocale.getInstance(locale));
                }
            }
            localeListSet = s;
        }
        return localeListSet;
    }

    static public CLDRLocale[] getLocales() {
        return (CLDRLocale[])getLocalesSet().toArray(new CLDRLocale[0]);
    }

    /**
     * Returns a Map of all interest groups.
     * en -> en, en_US, en_MT, ...
     * fr -> fr, fr_BE, fr_FR, ...
     */
    static protected Map<CLDRLocale,Set<CLDRLocale>> getIntGroups() {
        // TODO: rewrite as iterator
        CLDRLocale[] locales = getLocales();
        Map<CLDRLocale,Set<CLDRLocale>> h = new HashMap<CLDRLocale,Set<CLDRLocale>>();
        for(int i=0;i<locales.length;i++) {
            CLDRLocale locale = locales[i];
            CLDRLocale group = locale;
            int dash = locale.toString().indexOf('_');
            if(dash !=  -1) {
                group = CLDRLocale.getInstance(locale.toString().substring(0,dash));
            }
            Set<CLDRLocale> s = h.get(group);
            if(s == null) {
                s = new HashSet<CLDRLocale>();
                h.put(group,s);
            }
            s.add(locale);
        }
        return h;
    }

    public boolean isValidLocale(CLDRLocale locale) {
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
            busted("SQL error querying users for getIntUsers - " + SurveyMain.unchainSqlException(se));
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


    protected static void busted(String what) {
        busted(what, null, null);
    }
    
    /**
     * Report an error with a SQLException
     * @param what the error
     * @param se the SQL Exception
     */
    protected static void busted(String what, SQLException se) {
        busted(what, se, unchainSqlException(se));
    }
    
    protected static void busted(String what, Throwable t) {
        if(t instanceof SQLException) {
            busted(what, (SQLException)t);
        } else {
            busted(what, t, getThrowableStack(t));
        }
    }
    
    protected static void busted(String what, Throwable t, String stack) {
        System.err.println("SurveyTool busted: " + what + " ( after " +pages +"html+"+xpages+"xml pages served, uptime " + uptime.toString()  + ")");
        try {
            throw new InternalError("broke here");
        } catch(InternalError e) {
            e.printStackTrace();
        }
        if(isBusted==null) { // Keep original failure message.
            isBusted = what;
            if(stack == null) {
                stack = "(no stack)\n";
            }
            isBustedStack = stack + "\n"+"["+new Date().toGMTString()+"] "; 
            isBustedThrowable = t;
            isBustedTimer = new ElapsedTimer();
        } else { 
            System.err.println("[was already busted, not overriding old message.]");
        }
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

    static long shortN = 0;
    static final int MAX_CHARS = 100;
    static final int LARGER_MAX_CHARS = 256;
    static final String SHORT_A = "(Click to show entire message.)";
    static final String SHORT_B = "(hide.)";
    private static void printShortened(WebContext ctx, String str) {
        ctx.println(getShortened(str));
    }
    
    private static void printShortened(WebContext ctx, String str, int max) {
        ctx.println(getShortened(str, max));
    }
    
    private static String getShortened(String str) {
        return getShortened(str, MAX_CHARS);        
    }
    
    private static synchronized String  getShortened(String str, int max) {
        if(str.length()<(max+1+SHORT_A.length())) {
            return (str);
        } else {
            int cutlen = max;
            String key = CookieSession.cheapEncode(shortN++);
            int newline = str.indexOf('\n');
            if((newline > 2) && (newline < cutlen)) {
                cutlen = newline;
            }
            newline = str.indexOf("Exception:");
            if((newline > 2) && (newline < cutlen)) {
                cutlen = newline;
            }
            newline = str.indexOf("Message:");
            if((newline > 2) && (newline < cutlen)) {
                cutlen = newline;
            }
            newline = str.indexOf("<br>");
            if((newline > 2) && (newline < cutlen)) {
                cutlen = newline;
            }
            newline = str.indexOf("<p>");
            if((newline > 2) && (newline < cutlen)) {
                cutlen = newline;
            }
            return getShortened(str.substring(0,cutlen), str, key); 
        }
    }

    private void printShortened(WebContext ctx, String shortStr, String longStr, String warnHash ) {
            ctx.println(getShortened(shortStr, longStr, warnHash));
    }

    private static String getShortened(String shortStr, String longStr, String warnHash ) {
        return  ("<span id='h_ww"+warnHash+"'>" + shortStr + "... ") +
                ("<a href='javascript:show(\"ww" + warnHash + "\")'>" + 
                    SHORT_A+"</a></span>") +
                ("<!-- <noscript>Warning: </noscript> -->" + 
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
            //logger.warning("Warning: Can't read xpath warnings file.  " + cldrHome + "/surveyInfo.txt - To remove this warning, create an empty file there.");
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
    public String db_driver = null;
    public String db_protocol = null;
    public static String CLDR_DB_U = null;
    public static String CLDR_DB_P = null;
    public String cldrdb_u = null;
    public static String CLDR_DB;
    public String cldrdb = null;
    public String CLDR_DB_CREATESUFFIX=null;
    public String CLDR_DB_SHUTDOWNSUFFIX=null;
    static boolean db_Derby = false;
    static boolean db_Mysql = false;
    
    // === DB workarounds :(
    public String DB_SQL_IDENTITY = "GENERATED ALWAYS AS IDENTITY";
    public String DB_SQL_VARCHARXPATH = "varchar(1024)";
    public String DB_SQL_WITHDEFAULT = "WITH DEFAULT";
    public String DB_SQL_TIMESTAMP0 = "TIMESTAMP";
    public String DB_SQL_CURRENT_TIMESTAMP0 = "CURRENT_TIMESTAMP";
    public String DB_SQL_MIDTEXT   = "VARCHAR(1024)";
    public String DB_SQL_BIGTEXT   = "VARCHAR(16384)";
    public String DB_SQL_UNICODE   = "VARCHAR(16384)"; // unicode type string
    public String DB_SQL_ALLTABLES = "select tablename from SYS.SYSTABLES where tabletype='T'";
    public String DB_SQL_BINCOLLATE = "";
    public String DB_SQL_BINTRODUCER = "";

    private void setupDBProperties(Properties cldrprops) {
        db_driver   =cldrprops.getProperty("CLDR_DB_DRIVER", "org.apache.derby.jdbc.EmbeddedDriver");
        db_protocol =cldrprops.getProperty("CLDR_DB_PROTOCOL", "jdbc:derby:");
        if(db_protocol.indexOf("derby")>=0) {
            db_Derby=true;
        } else if(db_protocol.indexOf("mysql")>=0) {
            System.err.println("Note: mysql mode");
            db_Mysql=true;
            DB_SQL_IDENTITY = "AUTO_INCREMENT PRIMARY KEY";
            DB_SQL_BINCOLLATE=" COLLATE latin1_bin ";
            DB_SQL_VARCHARXPATH="TEXT(1000) CHARACTER SET latin1 " + DB_SQL_BINCOLLATE;
            DB_SQL_BINTRODUCER = "_latin1";
            DB_SQL_WITHDEFAULT="DEFAULT";
            DB_SQL_TIMESTAMP0 = "DATETIME";
            DB_SQL_CURRENT_TIMESTAMP0 = "'1999-12-31 23:59:59'"; // NOW?
            DB_SQL_MIDTEXT   = "TEXT(1024)";
            DB_SQL_BIGTEXT   = "TEXT(16384)";
            DB_SQL_UNICODE   = "BLOB";
            DB_SQL_ALLTABLES = "show tables";
        } else {
            System.err.println("WARNING: Don't know what kind of database is "+db_protocol+" - might be interesting!");
        }
        CLDR_DB_U   =cldrprops.getProperty("CLDR_DB_U","cldr_user");
        CLDR_DB_P   =cldrprops.getProperty("CLDR_DB_P","cldr_pass");
        CLDR_DB     =cldrprops.getProperty("CLDR_DB","cldrdb");
        dbDir = new File(cldrHome,CLDR_DB);
        cldrdb      =cldrprops.getProperty("CLDR_DB_LOCATION", dbDir.getAbsolutePath());
        CLDR_DB_CREATESUFFIX = cldrprops.getProperty("CLDR_DB_CREATESUFFIX",";create=true");
        CLDR_DB_SHUTDOWNSUFFIX = cldrprops.getProperty("CLDR_DB_SHUTDOWNSUFFIX","jdbc:derby:;shutdown=true");
    }

    public Connection getDBConnection()
    {
        return getDBConnection("");
    }

//    private Connection getU_DBConnection()
//    {
//        return getU_DBConnection("");
//    }
    
    static int db_number_cons=0;
    static int db_number_pool_cons=0;

    public static void closeDBConnection(Connection aconn) {
        if(aconn!=null) {
            try {
                aconn.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                System.err.println(SurveyMain.unchainSqlException(e));
                e.printStackTrace();
            }
            db_number_cons--;
            System.err.println("SQL -conns: " + db_number_cons);
        }
    }
    private Connection getDBConnection(String options)
    {
        try{ 
            db_number_cons++;
            
            if(datasource != null) {
                db_number_pool_cons++;
                System.err.println("SQL  +conns: " + db_number_cons+" Pconns: " + db_number_pool_cons);
                return datasource.getConnection();
            }
            
            System.err.println("SQL +conns: " + db_number_cons);
//            if(db_number_cons >= 12) {
//                throw new InternalError("too many..");
//            }
            Properties props = new Properties();
            props.put("user", CLDR_DB_U);
            props.put("password", CLDR_DB_P);
            Connection nc =  DriverManager.getConnection(db_protocol +
                                                         cldrdb + options, props);
            nc.setAutoCommit(false);
            return nc;
        } catch (SQLException se) {
            se.printStackTrace();
            busted("Fatal in getDBConnection", se);
            return null;
        }
    }

//    private Connection getU_DBConnection(String options)
//    {
//        try{ 
//            Properties props = new Properties();
//            props.put("user", "cldr_user");
//            props.put("password", "cldr_password");
//            cldrdb_u =  dbDir_u.getAbsolutePath();
//            Connection nc =  DriverManager.getConnection(db_protocol +
//                                                         cldrdb_u + options, props);
//            nc.setAutoCommit(false);
//            return nc;
//        } catch (SQLException se) {
//            busted("Fatal in getDBConnection: "", se);
//            return null;
//        }
//    }

    File dbDir = null;
    private DataSource datasource = null;
//    File dbDir_u = null;
    static String dbInfo = null;

    private void doStartupDB()
    {
        int nn = progressCount; // save
        setProgress("Database Setup");
        updateProgress(nn += 5, "begin.."); // restore
        //dbDir_u = new File(cldrHome,CLDR_DB_U);
        //boolean doesExist = dbDir.isDirectory();
    ///*U*/	    boolean doesExist_u = dbDir_u.isDirectory();
        
    //    logger.info("SurveyTool setting up database.. " + dbDir.getAbsolutePath());
        try
        { 
            if(false) { // pooling listener disabled by default
               updateProgress(nn++, "Load pool "+STPoolingListener.ST_ATTRIBUTE); // restore
               datasource = (DataSource) getServletContext().getAttribute(STPoolingListener.ST_ATTRIBUTE);
            }
            updateProgress(nn++, "Load "+db_driver); // restore
            Object o = Class.forName(db_driver).newInstance();
            try {
                java.sql.Driver drv = (java.sql.Driver)o;
                updateProgress(nn++, "Check "+db_driver); // restore
                dbInfo = "v"+drv.getMajorVersion()+"."+drv.getMinorVersion();
             //   dbInfo = dbInfo + " " +org.apache.derby.tools.sysinfo.getProductName()+" " +org.apache.derby.tools.sysinfo.getVersionString();
            } catch(Throwable t) {
                dbInfo = "unknown";
            }
            logger.info("loaded "+db_driver+" driver " + o + " - " + dbInfo);
            updateProgress(nn++, "Create DB"); // restore
            Connection conn = getDBConnection(CLDR_DB_CREATESUFFIX);
    //        logger.info("Connected to database " + cldrdb);
    ///*U*/        Connection conn_u = getU_DBConnection(";create=true");
    //        logger.info("Connected to user database " + cldrdb_u);
            
            // set up our main tables.
            updateProgress(nn++, "Commit DB"); // restore
            conn.commit();        
            conn.close(); 
            
    ///*U*/        conn_u.commit();
    ///*U*/        conn_u.close();
        }
        catch (SQLException e)
        {
            busted("On database startup", e);
            return;
        }
        catch (Throwable t)
        {
            busted("Other error on database startup",t);
            t.printStackTrace();
            return;
        }
        // now other tables..
        updateProgress(nn++, "Setup databases "); // restore
        try {
            updateProgress(nn++, "Setup  "+UserRegistry.CLDR_USERS); // restore
            Connection uConn = getDBConnection(); ///*U*/ was:  getU_DBConnection
            boolean doesExist_u = hasTable(uConn, UserRegistry.CLDR_USERS);
            updateProgress(nn++, "Create UserRegistry  "+UserRegistry.CLDR_USERS); // restore
            reg = UserRegistry.createRegistry(logger, uConn, this);
            if(!doesExist_u) { // only import users on first run through..
                updateProgress(nn++, "Import old users"); // restore
                reg.importOldUsers(vetdata);
            }
            
//            String doMigrate = survprops.getProperty("CLDR_MIGRATE");
//            if((doMigrate!=null) && (doMigrate.length()>0)) {
//                System.err.println("** User DB migrate");
//                reg.migrateFrom(getU_DBConnection());
//            }
            
        } catch (SQLException e) {
            busted("On UserRegistry startup", e);
            return;
        }
        updateProgress(nn++, "Create XPT"); // restore
        try {
            xpt = XPathTable.createTable(logger, getDBConnection(), this);
        } catch (SQLException e) {
            busted("On XPathTable startup", e);
            return;
        }
        // note: make xpt before CLDRDBSource..
        updateProgress(nn++, "Create CLDR_DATA"); // restore
        try {
            dbsrcfac = new CLDRDBSourceFactory(this, fileBase, logger, new File(homeFile, "vxpt"));
        } catch (SQLException e) {
            busted("On CLDRDBSource startup", e);
            return;
        }
        updateProgress(nn++, "Create Vetting"); // restore
        try {
            vet = Vetting.createTable(logger, getDBConnection(), this);
        } catch (SQLException e) {
            e.printStackTrace();
            busted("On Vetting startup", e);
            return;
        }
        updateProgress(nn++, "Tell DBFac the Vetter is Ready"); // restore
        try {
            dbsrcfac.vetterReady();
        } catch (Throwable e) {
            e.printStackTrace();
            busted("On Tell DBFac the Vetter is Ready startup", e);
            return;
        }
        updateProgress(nn++, "Create fora"); // restore
        try {
            fora = SurveyForum.createTable(logger, getDBConnection(), this);
        } catch (SQLException e) {
            busted("On Fora startup", e);
            return;
        }
        updateProgress(nn++, " DB setup complete."); // restore
    }    
    
    public static final String getThrowableStack(Throwable t) {
        try {
            StringWriter asString = new StringWriter();
            t.printStackTrace(new PrintWriter(asString));
            return asString.toString();
        } catch ( Throwable tt ) {
        	tt.printStackTrace();
            return("[[unable to get stack: "+tt.toString()+"]]");
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
    
    String escapeForMysql(String what) throws UnsupportedEncodingException {
        if(what==null) {
            return "NULL";
        } else if(what.length()==0) {
            return "\"\"";
        } else {
            return escapeForMysql(what.getBytes("ASCII"));
        }
    }
    
    String escapeForMysqlUtf8(String what) throws UnsupportedEncodingException {
        if(what==null) {
            return "NULL";
        } else if(what.length()==0) {
            return "\"\"";
        } else {
            return escapeForMysql(what.getBytes("UTF-8"));
        }
    }
    
    public static final boolean escapeIsBasic(char c) {
        return ((c>='a'&&c<='z')||
                (c>='A'&&c<='Z')||
                (c>='0'&&c<='9')||
                (c==' ' || c=='.' || c=='/' || c=='[' || c==']' || c=='=' ||
                 c=='@' || c=='_' || c==',' || c=='&' || 
                 c=='-' || c=='(' || c==')' || c=='#' || c=='$' || c=='!'));
    }
    
    public static final boolean escapeIsEscapeable(char c) {
        return(c==0 || c=='\'' || c=='"' || c=='\b' || c=='\n' || c=='\r' || c=='\t' || c==26 || c=='\\');
    }
    
    public static final String escapeForMysql(byte what[]) {
       boolean hasEscapeable    = false;
       boolean hasNonEscapeable = false;
       for(byte b : what) {
           int j =  ((int)b)&0xff;
           char c = (char)j;
           if(escapeIsBasic(c)) {
               continue;
           } else if(escapeIsEscapeable(c)) {
               hasEscapeable=true;
           } else {
               hasNonEscapeable=true;
           }
       }
       if(hasNonEscapeable) {
           return escapeHex(what);
       } else if(hasEscapeable) {
           return escapeLiterals(what);
       } else {
           return escapeBasic(what);
       }
    }
    
    public static final String escapeHex(byte what[]) {
        StringBuffer out=new StringBuffer("x'");
        for(byte b : what) {
            int j =  ((int)b)&0xff;
            if(j<0x10) {
                out.append('0');
            }
            out.append(Integer.toHexString(j));
        }
        out.append("'");
        return out.toString();
    }
    
    public static final String escapeLiterals(byte what[]) {
        StringBuffer out=new StringBuffer("'");
        for(byte b : what) {
            int j =  ((int)b)&0xff;
            char c = (char)j;
            switch(c) {
            case 0: out.append("\\0"); break;
            case '\'': out.append("'"); break;
            case '"': out.append("\\"); break;
            case '\b': out.append("\\b"); break;
            case '\n': out.append("\\n"); break;
            case '\r': out.append("\\r"); break;
            case '\t': out.append("\\t"); break;
            case 26: out.append("\\z"); break;
            case '\\': out.append("\\\\"); break;
            default: out.append(c);
            }
        }
        out.append("'");
        return out.toString();
    }
    public static final String escapeBasic(byte what[]) {
        return escapeLiterals(what);
    }
    
    // fix the UTF-8 fail
    public static final String getStringUTF8(ResultSet rs, int which) throws SQLException {
        if(db_Derby) { // unicode
            return rs.getString(which);
        }
        byte rv[] = rs.getBytes(which);
        if(rv != null) {
            String unicode;
            try {
                unicode = new String(rv, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new InternalError(e.toString());
            }
            return unicode;
        } else {
            return null;
        }
    }
    
    public static final void setStringUTF8(PreparedStatement s, int which, String what) throws SQLException {
        if(db_Derby) {
            s.setString(which, what);
        } else {
            byte u8[];
            if(what == null) {
                u8 = null;
            } else {
                try {
                    u8 = what.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    throw new InternalError(e.toString());
                }
            }
            s.setBytes(which, u8);
        }
    }

    public boolean hasTable(Connection conn, String table) {
        String canonName = db_Derby?table.toUpperCase():table;
        try {
            ResultSet rs;
            
            if(SurveyMain.db_Derby) {
                DatabaseMetaData dbmd = conn.getMetaData();
                rs = dbmd.getTables(null, null, canonName, null);
            } else {
                Statement s = conn.createStatement();
                rs = s.executeQuery("show tables like '"+canonName+"'");
            }
            
            if(rs.next()==true) {
                rs.close();
                System.err.println("table " + canonName + " did exist.");
                return true;
            } else {
                System.err.println("table " + canonName + " did not exist.");
                return false;
            }
        } catch (SQLException se)
        {
            busted("While looking for table '" + table + "': ", se);
            return false; // NOTREACHED
        }
    }

    void doShutdownDB() {
        boolean gotSQLExc = false;
        
        try
        {
            // shut down other connections
            try {
                CookieSession.shutdownDB();
            } catch(Throwable t) {
                t.printStackTrace();
                System.err.println("While shutting down cookiesession ");
            }
            try {
                if(reg!=null) reg.shutdownDB();
            } catch(Throwable t) {
                t.printStackTrace();
                System.err.println("While shutting down reg ");
            }
            try {
                if(xpt!=null) xpt.shutdownDB();
            } catch(Throwable t) {
                t.printStackTrace();
                System.err.println("While shutting down xpt ");
            }
            try {
                if(vet!=null) vet.shutdownDB();
            } catch(Throwable t) {
                t.printStackTrace();
                System.err.println("While shutting down vet ");
            }
            
            if(db_Derby) {
                DriverManager.getConnection(CLDR_DB_SHUTDOWNSUFFIX);
            }
        }
        catch (SQLException se)
        {
            gotSQLExc = true;
            logger.info("DB: while shutting down: " + se.toString());
        }
    }

    public static void main(String args[]) {
        System.out.println("Starting some test of SurveyTool locally....");
        try{
            cldrHome="/xsrl/T/cldr";
            vap="testingvap";
            SurveyMain sm=new SurveyMain();
            System.out.println("sm created.");
            sm.doStartup();
            System.out.println("sm started.");
            sm.doStartupDB();
            System.out.println("DB started.");
            if(isBusted != null)  {
                System.err.println("Survey Tool is down: " + isBusted);
                return;
            }
            
            System.err.println("--- Starting processing of requests ---");
            CookieSession cs = new CookieSession(true);
            for ( String arg : args ) {
                if(arg.equals("-wait")) {
                    try {
                        System.err.println("*** WAITING ***");
                        System.in.read();
                    } catch(Throwable t) {}
                    continue;
                }
                com.ibm.icu.dev.test.util.ElapsedTimer reqTimer = new com.ibm.icu.dev.test.util.ElapsedTimer();
                System.err.println("***********\n* "+arg);
                WebContext xctx = new URLWebContext("http://127.0.0.1:8080/cldr-apps/survey" + arg);
                xctx.sm = sm;
                xctx.session=cs;

                xctx.reqTimer = reqTimer;
                            
                if(xctx.field("dump").equals(vap)) {
                    sm.doAdminPanel(xctx);
                } else if(xctx.field("sql").equals(vap)) {
                    sm.doSql(xctx);
                } else {
                    sm.doSession(xctx); // Session-based Survey main
                }
                //xctx.close();
                System.err.println("\n\n"+reqTimer+" for " + arg);
            }
            System.err.println("--- Ending processing of requests ---");

            /*
            String ourXpath = "//ldml/numbers";
            
            System.out.println("xpath xpt.getByXpath("+ourXpath+") = " + sm.xpt.getByXpath(ourXpath));
            */
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
                    CLDRFile cf = sm.getUserFile(ctx, (ctx.session.user==null)?null:ctx.session.user, ctx.getLocale());
                    if(cf == null) {
                        throw new InternalError("CLDRFile is null!");
                    }
                    CLDRDBSource ourSrc = (CLDRDBSource)ctx.getByLocale(USER_FILE + CLDRDBSRC); // TODO: remove. debuggin'
                    if(ourSrc == null) {
                        throw new InternalError("oursrc is null! - " + (USER_FILE + CLDRDBSRC) + " @ " + ctx.getLocale() );
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
                        checkCldr.setCldrFileToCheck(cf, ctx.getOptionsMap(basicOptionsMap()), checkCldrResult);
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
        return BUG_URL_BASE + "/"+folder+"?compose="+number+"&amp;subject="+java.net.URLEncoder.encode(subject)+"&amp;locksubj=y";
    }

    public static String bugFeedbackUrl(String subject) {
        return BUG_URL_BASE +"?newbug=incoming&amp;subject="+java.net.URLEncoder.encode(subject)+"&amp;locksubj=y";
    }
    
    
    // =============  Following have to do with "external errors file", an interim error handling mechanism
    
    /**
     * errors file 
     */
    private Hashtable<CLDRLocale, Set<Integer>> externalErrorSet = null;
    private long externalErrorLastMod = -1;
    private long externalErrorLastCheck = -1;
    private boolean externalErrorFailed = false;
         
    private synchronized boolean externalErrorRead() {
        if(externalErrorFailed) return false;
        String externalErrorName = cldrHome + "/" + "count.txt";

        long now = System.currentTimeMillis();
        
        if((now-externalErrorLastCheck) < 8000) {
            //System.err.println("Not rechecking errfile- only been " + (now-externalErrorLastCheck) + " ms");
            if(externalErrorSet != null) {
                return true;
            } else {
                return false;
            }
        }

        externalErrorLastCheck = now;
        
        try {
            File extFile = new File(externalErrorName);
            
            if(!extFile.isFile() && !extFile.canRead()) {
                System.err.println("Can't read counts file: " + externalErrorName);
                externalErrorFailed = true;
                return false;
            }
            
            long newMod = extFile.lastModified();
            
            if(newMod == externalErrorLastMod) {
                //System.err.println("** e.e. file did not change");
                return true;
            }
            
            // ok, now read it
            BufferedReader in
               = new BufferedReader(new FileReader(extFile));
            String line;
            int lines=0;
            Hashtable<CLDRLocale, Set<Integer>> newSet = new Hashtable<CLDRLocale, Set<Integer>>();
            while ((line = in.readLine())!=null) {
                lines++;
                if((line.length()<=0) ||
                  (line.charAt(0)=='#')) {
                    continue;
                }
                try {
                    String[] result = line.split("\t");
                    CLDRLocale loc = CLDRLocale.getInstance(result[0].split(";")[0]);
                    String what = result[1];
                    String val = result[2];
                    
                    Set<Integer> aSet = newSet.get(loc);
                    if(aSet == null) {
                        aSet = new HashSet<Integer>();
                        newSet.put(loc, aSet);
                    }
                    
                    if(what.equals("path:")) {
                        aSet.add(xpt.getByXpath(val));
                    } else if(what.equals("count:")) {
                        int theirCount = new Integer(val).intValue();
                        if(theirCount != aSet.size()) {
                            System.err.println(loc + " - count says " + val + ", we got " + aSet.size());
                        }
                    } else {
                        throw new IllegalArgumentException("Unknown parameter: " + what);
                    }
                } catch(Throwable t) {
                    System.err.println("** " + externalErrorName +":"+ lines + " -  " + t.toString());
                    externalErrorFailed = true;
                    t.printStackTrace();
                    return false;  
                }
            }
            System.err.println(externalErrorName + " - " + lines + " and " + newSet.size() + " locales loaded.");
            
            externalErrorSet = newSet;
            externalErrorLastMod = newMod;
            externalErrorFailed = false;
            return true;
        } catch(IOException ioe) {
            System.err.println("Reading externalErrorFile: "  + "count.txt - " + ioe.toString());
            ioe.printStackTrace();
            externalErrorFailed = true;
            return false;
        }
    }
        
    public int externalErrorCount(CLDRLocale loc) {
        synchronized(this) {
            if(externalErrorRead()) {
                Set<Integer> errs =  externalErrorSet.get(loc);
                if(errs != null) {
                    return errs.size();
                } else {
                    return 0;
                }
            }
        }
        return -1;
    }
    
    public String externalErrorUrl(String groupName) {
        return "http://unicode.org/cldr/data/dropbox/gen/errors/"+groupName+".html";
    }

    // =============  Following have to do with phases

    public static boolean isPhaseSubmit() {
        return phase()==Phase.SUBMIT || phase()==Phase.BETA;
    }
    
    public static String getSaveButtonText() {
        return (phase()==Phase.BETA)?"Save [Note: SurveyTool is in BETA]":"Save Changes";
    }

    public static boolean isPhaseVetting() {
        return phase()==Phase.VETTING;
    }

    public static boolean isPhaseVettingClosed() {
        return phase()==Phase.VETTING_CLOSED;
    }

    public static boolean isPhaseClosed() {
        return (phase()==Phase.CLOSED) || (phase()==Phase.VETTING_CLOSED);
    }

    public static boolean isPhaseDisputed() {
        return phase()==Phase.DISPUTED;
    }

    public static boolean isPhaseFinalTesting() {
        return phase()==Phase.FINAL_TESTING;
    }

    public static boolean isPhaseReadonly() {
        return phase()==Phase.READONLY;
    }

    public static boolean isPhaseBeta() {
        return phase()==Phase.BETA;
    }

    public static Phase phase() {
        return currentPhase;
    }

    public static String getOldVersion() {
        return oldVersion;
    }

    public static String getNewVersion() {
        return newVersion;
    }

    public static String getProposedName() {
        return "Proposed&nbsp;"+getNewVersion();
    }
    
    Pattern ALLOWED_EMPTY = Pattern.compile("//ldml/fallback(?![a-zA-Z])");
    // TODO move to central location
    
    private boolean choiceNotEmptyOrAllowedEmpty(String choice_r, String path) {
      return choice_r.length()>0 || ALLOWED_EMPTY.matcher(path).matches();
    }
}
