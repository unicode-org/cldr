/*
 ******************************************************************************
 * Copyright (C) 2004-2014, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.web;

import java.io.BufferedReader;
import java.io.Externalizable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.test.HelpMessages;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfigImpl;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRLocale.CLDRFormatter;
import org.unicode.cldr.util.CLDRLocale.FormatBehavior;
import org.unicode.cldr.util.CLDRURLS;
import org.unicode.cldr.util.CachingEntityResolver;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CoverageInfo;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Factory.DirectoryType;
import org.unicode.cldr.util.Factory.SourceTreeType;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SurveyToolStatus;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.SpecialLocales;
import org.unicode.cldr.util.SpecialLocales.Type;
import org.unicode.cldr.util.StackTracker;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.web.SurveyAjax.AjaxType;
import org.unicode.cldr.web.SurveyThread.SurveyTask;
import org.unicode.cldr.web.UserRegistry.InfoType;
import org.unicode.cldr.web.WebContext.HTMLDirection;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * The main servlet class of Survey Tool
 */
public class SurveyMain extends HttpServlet implements CLDRProgressIndicator, Externalizable {

    private static final String VURL_LOCALES = "v#locales///";
    public static final String CLDR_OLDVERSION = "CLDR_OLDVERSION";
    public static final String CLDR_NEWVERSION = "CLDR_NEWVERSION";
    public static final String CLDR_LASTVOTEVERSION = "CLDR_LASTVOTEVERSION";
    public static final String CLDR_DIR = "CLDR_DIR";
    public static final String CLDR_DIR_REPOS = "http://unicode.org/repos/cldr";

    public static final String NEWVERSION_EPOCH = "1970-01-01 00:00:00";

    public static final String CLDR_NEWVERSION_AFTER = "CLDR_NEWVERSION_AFTER";

    public static Stamp surveyRunningStamp = Stamp.getInstance();

    public static final String QUERY_SAVE_COOKIE = "save_cookie";

    private static final String STEPSMENU_TOP_JSP = "stepsmenu_top.jsp";

    private static final String REPORT_PREFIX = "r_";

    private static final String R_STEPS = REPORT_PREFIX + "steps";
    public static final String R_VETTING = REPORT_PREFIX + "vetting";
    public static final String R_VETTING_JSON = REPORT_PREFIX + "vetting_json";

    public static final String SURVEYMAIN_REVISION = "SurveyMain.java $Revision$";

    //private static final String CLDR_BULK_DIR = "CLDR_BULK_DIR";
    //private static String bulkStr = null;
    static final String ACTION_DEL = "_del";
    static final String ACTION_UNVOTE = "_unvote";
    private static final String XML_CACHE_PROPERTIES = "xmlCache.properties";
    static UnicodeSet supportedNameSet = new UnicodeSet("[a-zA-Z]").freeze();
    static final int TWELVE_WEEKS = 3600 * 24 * 7 * 12;

    public static final String DEFAULT_CONTENT_LINK = "<i><a target='CLDR-ST-DOCS' href='http://cldr.unicode.org/translation/default-content'>default content locale</a></i>";

    /**
     *
     */
    private static final long serialVersionUID = -3587451989643792204L;

    /**
     * This class enumerates the current phase of the survey tool
     *
     * @author srl
     *
     */
    public enum Phase {
        SUBMIT("Data Submission", CheckCLDR.Phase.SUBMISSION), // SUBMISSION
        VETTING("Vetting", CheckCLDR.Phase.VETTING), VETTING_CLOSED("Vetting Closed", CheckCLDR.Phase.FINAL_TESTING), // closed
        // after
        // vetting
        // -
        // open
        // for
        // admin
        CLOSED("Closed", CheckCLDR.Phase.FINAL_TESTING), // closed
        DISPUTED("Dispute Resolution", CheckCLDR.Phase.VETTING), FINAL_TESTING("Final Testing", CheckCLDR.Phase.FINAL_TESTING), // FINAL_TESTING
        READONLY("Read-Only", CheckCLDR.Phase.FINAL_TESTING), BETA("Beta", CheckCLDR.Phase.SUBMISSION);

        private String what;
        private CheckCLDR.Phase cphase;

        private Phase(String s, CheckCLDR.Phase ph) {
            what = s;
            this.cphase = ph;
        }

        public String toString() {
            return what;
        }

        /**
         * Get the CheckCLDR.Phase equivalent
         *
         * @return
         */
        public CheckCLDR.Phase getCPhase() {
            return cphase;
        }
    };

    public enum ReportMenu {
        PRIORITY_ITEMS("Dashboard", SurveyMain.R_VETTING_JSON), DATE_TIME("Date/Time", "r_datetime"), ZONES("Zones", "r_zones"), NUMBERS("Numbers",
            "r_compact");

        private String display;
        private String url;
        private String query;

        private ReportMenu(String d, String u) {
            display = d;
            url = u;
            query = null;
        }

        private ReportMenu(String d, String u, String q) {
            display = d;
            url = u;
            query = q;
        }

        public String urlStub() {
            return url;
        }

        public String urlQuery() {
            if (query == null) {
                return SurveyMain.QUERY_SECTION + "=" + url;
            } else {
                return SurveyMain.QUERY_SECTION + "=" + url + "&" + query;
            }
        }

        public boolean hasQuery() {
            return query != null;
        }

        public String urlFull(String base, String locale) {
            return base + "?_=" + locale + "&" + urlQuery();
        }

        public String urlFull(String base) {
            return base + "?" + urlQuery();
        }

        public String display() {
            return display;
        }
    };

    // ===== Configuration state
    private static Phase currentPhase = Phase.VETTING;
    /** set by CLDR_PHASE property. **/
    private static String oldVersion = "OLDVERSION";
    private static String lastVoteVersion = "LASTVOTEVERSION";
    private static String newVersion = "NEWVERSION";

    public static boolean isConfigSetup = false;

    /**
     * @return the isUnofficial. - will return true (even in production) until configfile is setup
     * @see CLDRConfig#getEnvironment()
     */
    public static final boolean isUnofficial() {
        if (!isConfigSetup) {
            return true; //
        }
        return !(CLDRConfig.getInstance().getEnvironment() == CLDRConfig.Environment.PRODUCTION);
    }

    /** set to true for all but the official installation of ST. **/

    // ==== caches and general state

    public UserRegistry reg = null;
    public XPathTable xpt = null;
    public SurveyForum fora = null;
    static ElapsedTimer uptime = new ElapsedTimer("uptime: {0}");
    ElapsedTimer startupTime = new ElapsedTimer("{0} until first GET/POST");
    public static String isBusted = null;
    private static String isBustedStack = null;
    //private static Throwable isBustedThrowable = null;
    private static ElapsedTimer isBustedTimer = null;
    static ServletConfig config = null;
    public static OperatingSystemMXBean osmxbean = ManagementFactory.getOperatingSystemMXBean();
    static double nProcs = osmxbean.getAvailableProcessors();

    /**
     * Is the CPU essentially busy?
     *
     * @return
     */
    public static final boolean hostBusy() {
        return (osmxbean.getSystemLoadAverage() * 2) >= osmxbean.getAvailableProcessors();
    }

    // ===== UI constants
    static final String CURRENT_NAME = "Others";

    // ===== Special bug numbers.
    // public static final String BUG_METAZONE_FOLDER = "data";
    // public static final int BUG_METAZONE = 1262;
    // public static final String BUG_ST_FOLDER = "tools";
    // public static final int BUG_ST = xxxx; // was: umbrella bug
    public static final String URL_HOST = "http://www.unicode.org/";
    public static final String URL_CLDR = URL_HOST + "cldr/";
    public static final String BUG_URL_BASE = URL_CLDR + "trac";
    public static final String GENERAL_HELP_URL = URL_CLDR + "survey_tool.html";
    public static final String GENERAL_HELP_NAME = "Instructions";

    // ===== url prefix for help
    // public static final String CLDR_WIKI_BASE = URL_CLDR+"wiki";
    // public static final String CLDR_HELP_LINK =
    // CLDR_WIKI_BASE+"?SurveyToolHelp";
    // public static final String CLDR_HELP_LINK_EDIT = CLDR_HELP_LINK;
    public static final String CLDR_HELP_LINK = GENERAL_HELP_URL + "#";

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
    /** < Experimental support for 'remove' */
    public static final String REMOVE = "remove";
    public static final String CONFIRM = "confirm";
    public static final String INHERITED_VALUE = "inherited-value";
    public static final String CHANGETO = "change to";
    public static final String PROPOSED_DRAFT = "proposed-draft";
    public static final String MKREFERENCE = "enter-reference";
    public static final String STFORM = "stform";

    // public static final String MODIFY_THING =
    // "<span title='You are allowed to modify this locale.'>\u270D</span>"; //
    // was: F802
    // public static final String MODIFY_THING =
    // "<img src='' title='You are allowed to modify this locale.'>"; // was:
    // F802
    public static String modifyThing(WebContext ctx) {
        return "&nbsp;" + ctx.modifyThing("You are allowed to modify this locale.");
    }

    // ========= SYSTEM PROPERTIES
    public static String vap = System.getProperty("CLDR_VAP"); // Vet Access
    // Password
    public static String testpw = System.getProperty("CLDR_TESTPW"); // Vet
    // Access
    // Password
    public static String vetdata = System.getProperty("CLDR_VET_DATA"); // dir
    // for
    // vetted
    // data
    File _vetdir = null;

    /**
     * @deprecated use CLDRURLS
     */
    private String defaultBase = CLDRURLS.DEFAULT_BASE + "/survey"; /* base URL */
    public static String vetweb = System.getProperty("CLDR_VET_WEB"); // dir for
    // web
    // data
    public static String cldrLoad = System.getProperty("CLDR_LOAD_ALL"); // preload
    // all
    // locales?
    public static String fileBase = null; // not static - may change later.
    // Common dir
    public static String fileBaseSeed = null; // not static - may change later.
    // Seed dir
    private static String fileBaseOld = null; // fileBase + oldVersion
    static String specialMessage = System.getProperty("CLDR_MESSAGE"); // static
    // - may
    // change
    // later
    public static String lockOut = System.getProperty("CLDR_LOCKOUT"); // static
    // - may
    // change
    // later
    static long specialTimer = 0; // 0 means off. Nonzero: expiry time of
    // countdown.

    // public static final String LOGFILE = "cldr.log"; // log file of all
    // changes

    // ======= query fields
    public static final String QUERY_TERRITORY = "territory";
    public static final String QUERY_ZONE = "zone";
    public static final String QUERY_PASSWORD = "pw";
    public static final String QUERY_PASSWORD_ALT = "uid";
    public static final String QUERY_EMAIL = "email";
    public static final String QUERY_SESSION = "s";
    public static final String QUERY_LOCALE = "_";
    public static final String QUERY_SECTION = "x";
    public static final String QUERY_EXAMPLE = "e";
    public static final String QUERY_DO = "do";

    static final String SURVEYTOOL_COOKIE_SESSION = CookieSession.class.getPackage().getName() + ".id";
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
    static final String PREF_SORTMODE_CODE_CALENDAR = "codecal";
    static final String PREF_SORTMODE_METAZONE = "metazon";
    // static final String PREF_SORTMODE_ALPHA = "alpha";
    static final String PREF_SORTMODE_WARNING = "interest";
    static final String PREF_SORTMODE_NAME = "name";
    static final String PREF_SORTMODE_DEFAULT = PREF_SORTMODE_CODE;
    // static final String PREF_SHOW_VOTING_ALTS = "p_vetting_details";
    static final String PREF_NOSHOWDELETE = "p_nodelete";
    static final String PREF_DELETEZOOMOUT = "p_deletezoomout";
    public static final String PREF_NOJAVASCRIPT = "p_nojavascript";
    static final String PREF_ADV = "p_adv"; // show advanced prefs?
    static final String PREF_XPATHS = "p_xpaths"; // show xpaths?
    public static final String PREF_LISTROWS = "p_listrows";
    public static final String PREF_DEBUGJSP = "p_debugjsp"; // debug JSPs?
    public static final String PREF_COVLEV = "p_covlev"; // covlev
    public static final String PREF_COVTYP = "p_covtyp"; // covtyp
    // static final String PREF_SORTMODE_DEFAULT = PREF_SORTMODE_WARNING;

    static final String BASELINE_ID = "en_ZZ"; // Needs to be en_ZZ as per
    // cldrbug #2918
    public static final ULocale BASELINE_LOCALE = new ULocale(BASELINE_ID);
    public static final String BASELINE_LANGUAGE_NAME = BASELINE_LOCALE.getDisplayLanguage(BASELINE_LOCALE); // Note:
    // Only
    // shows
    // language.

    public static final String METAZONE_EPOCH = "1970-01-01";

    // ========== lengths
    /**
     * @see WebContext#prefCodesPerPage()
     */
    static final int CODES_PER_PAGE = 1024; // This is only a default.
    static final int PAGER_SHORTEN_WIDTH = 25; // # of chars in the 'pager' list
    // before they are shortened
    static final int REFS_SHORTEN_WIDTH = 120;

    static final String MEASNAME = "measurementSystemName";
    static final String CODEPATTERN = "codePattern";
    public static final String CURRENCYTYPE = "//ldml/" + PathUtilities.NUMBERSCURRENCIES + "/currency[@type='";
    public static String xMAIN = "general";
    public static String xREMOVE = "REMOVE";

    //
    public static String CALENDARS_ITEMS[] = PathUtilities.getCalendarsItems();
    public static String METAZONES_ITEMS[] = PathUtilities.getMetazonesItems();

    public static final String OTHERROOTS_ITEMS[] = { LDMLConstants.CHARACTERS, LDMLConstants.NUMBERS,
        LDMLConstants.LOCALEDISPLAYPATTERN, "units", PathUtilities.xOTHER };

    public static final String GREGO_XPATH = "//ldml/dates/" + LDMLConstants.CALENDARS + "/" + LDMLConstants.CALENDAR
        + "[@type=\"gregorian\"]";
    public static final String RAW_MENU_ITEM = "raw";
    public static final String TEST_MENU_ITEM = "test";

    public static final String SHOWHIDE_SCRIPT = "<script type='text/javascript'><!-- \n"
        + "function show(what)\n"
        + "{document.getElementById(what).style.display=\"block\";\ndocument.getElementById(\"h_\"+what).style.display=\"none\";}\n"
        + "function hide(what)\n"
        + "{document.getElementById(what).style.display=\"none\";\ndocument.getElementById(\"h_\"+what).style.display=\"block\";}\n"
        + "--></script>";

    static HelpMessages surveyToolSystemMessages = null;

    static String sysmsg(String msg) {
        try {
            if (surveyToolSystemMessages == null) {
                surveyToolSystemMessages = new HelpMessages("st_sysmsg.html");
            }
            return surveyToolSystemMessages.find(msg);
        } catch (Throwable t) {
            SurveyLog.logger.warning("Err " + t.toString() + " while trying to load sysmsg " + msg);
            return "[MISSING MSG: " + msg + "]";
        }
    }

    /**
     * Servlet initializer
     */

    public static SurveyMain getInstance(HttpServletRequest req) {
        if (config == null)
            return null; // not initialized.
        return (SurveyMain) config.getServletContext().getAttribute(SurveyMain.class.getName());
    }

    private void setInstance(HttpServletRequest req) {
        config.getServletContext().setAttribute(SurveyMain.class.getName(), this);
    }

    public final void init(final ServletConfig config) throws ServletException {
        System.out.println("\n\n\n------------------- SurveyMain.init() ------------ " + uptime);
        try {
            new com.ibm.icu.text.SimpleDateFormat(); // Ensure that ICU is
            // available before we get
            // any farther
            super.init(config);
            CLDRConfigImpl.setCldrHome(config.getInitParameter("cldr.home"));
            SurveyMain.config = config;

            // verify config sanity
            CLDRConfig cconfig = CLDRConfigImpl.getInstance();
            isConfigSetup = true; // we have a CLDRConfig - so config is setup.

            stopIfMaintenance();

            cconfig.getSupplementalDataInfo(); // will fail if CLDR_DIR is broken.

            PathHeader.PageId.forString(PathHeader.PageId.Africa.name()); // Make
            // sure
            // cldr-tools
            // is
            // functioning.
            startupThread.addTask(new SurveyThread.SurveyTask("startup") {
                public void run() throws Throwable {
                    doStartup();
                }
            });
        } catch (Throwable t) {
            SurveyLog.logException(t, "Initializing SurveyTool");
            SurveyMain.busted("Error initializing SurveyTool.", t);
            return;
        }

        try {
            dbUtils = DBUtils.getInstance();
        } catch (Throwable t) {
            SurveyLog.logException(t, "Starting up database");

            String dbBroken = DBUtils.getDbBrokenMessage();

            SurveyMain.busted("Error starting up database - " + dbBroken, t);
            return;
        }

        try {
            startupThread.start();
            SurveyLog.logger.warning("Startup thread launched");
        } catch (Throwable t) {
            SurveyLog.logException(t, "Starting up startupThread");
            SurveyMain.busted("Error starting up startupThread", t);
            return;
        }
    }

    public SurveyMain() {
        super();
        CookieSession.sm = this;
    }

    /**
     * output MIME header, build context, and run code..
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }

    public static String defaultServletPath = null;
    /**
     * IP blacklist
     */
    static Hashtable<String, Object> BAD_IPS = new Hashtable<String, Object>();
    public static String fileBaseA;
    public static String fileBaseASeed;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        CLDRConfigImpl.setUrls(request);

        if (!ensureStartup(request, response)) {
            return;
        }

        if (!isBusted()) {
            if (startupTime != null) {
                //String startupMsg = null;
                //startupMsg = (startupTime.toString());
                // logger.info(startupMsg); // log how long startup took
                startupTime = null;
            }

            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Max-Age", 0);
            response.setHeader("Robots", "noindex,nofollow");

            // handle raw xml
            try {
                if (getOutputFileManager().doRawXml(request, response)) {
                    // not counted.
                    xpages++;
                    return;
                }
            } catch (Throwable t) {
                SurveyLog.logException(t, "raw XML");
                SurveyLog.logger.warning("Error on doRawXML: " + t.toString());
                t.printStackTrace();
                response.setContentType("text/plain");
                ServletOutputStream os = response.getOutputStream();
                os.println("Error processing raw XML:\n\n");
                t.printStackTrace(new PrintStream(os));
                xpages++;
                return;
            }
            pages++;

            if ((pages % 100) == 0) {
                freeMem(pages, xpages);
            }
        }
        com.ibm.icu.dev.util.ElapsedTimer reqTimer = new com.ibm.icu.dev.util.ElapsedTimer();

        /**
         * Busted: unrecoverable error, do not attempt to go on.
         */
        if (isBusted()) {
            String pi = request.getParameter("sql"); // allow sql
            if ((pi == null) || (!pi.equals(vap))) {
                response.setContentType("text/html; charset=utf-8");
                PrintWriter out = response.getWriter();
                out.println("<html>");
                out.println("<head>");
                out.println("<title>CLDR Survey Tool offline</title>");
                out.println("<link rel='stylesheet' type='text/css' href='" + request.getContextPath() + "/" + "surveytool.css"
                    + "'>");
                showOfflinePage(request, response, out);
                return;
            }
        }

        /**
         * User database request
         *
         */
        if (request.getParameter("udump") != null && request.getParameter("udump").equals(vap)) { // XML.
            response.setContentType("application/xml; charset=utf-8");
            WebContext xctx = new WebContext(request, response);
            doUDump(xctx);
            xctx.close();
            return;
        }

        // rest of these are HTML
        response.setContentType("text/html; charset=utf-8");

        // set up users context object

        WebContext ctx = new WebContext(request, response);
        ctx.reqTimer = reqTimer;
        ctx.sm = this;
        if (defaultServletPath == null) {
            defaultServletPath = ctx.request.getServletPath();
            //defaultBase   = ctx.base();
        }
        /*
         * String theIp = ctx.userIP(); if(theIp.equals("66.154.103.161") //
         * gigablast ) { try { Thread.sleep(98765); } catch(InterruptedException
         * ie) { } }
         */

        String baseThreadName = Thread.currentThread().getName();

        try {

            // process any global redirects here.

            if (isUnofficial()) {
                boolean waitASec = twidBool("SurveyMain.twoSecondPageDelay");
                if (waitASec) {
                    ctx.println("<h1>twoSecondPageDelay</h1>");
                    Thread.sleep(2000);
                }
            }

            if (isUnofficial() && (ctx.hasTestPassword() || ctx.hasAdminPassword())
                && ctx.field("action").equals("new_and_login")) { // accessed from createAndLogin.jsp
                ctx.println("<hr>");
                String real = ctx.field("real").trim();
                if (real.isEmpty() || real.equals("REALNAME")) {
                    ctx.println(ctx.iconHtml("stop", "fail")
                        + "<b>Please go <a href='javascript:window.history.back();'>Back</a> and fill in your real name.</b>");
                } else {
                    final boolean autoProceed = ctx.hasField("new_and_login_autoProceed");
                    final boolean stayLoggedIn = ctx.hasField("new_and_login_stayLoggedIn");
                    ctx.println("<div style='margin: 2em;'>");
                    if (autoProceed) {
                        ctx.println("<img src='loader.gif' align='right'>");
                    } else {
                        ctx.println("<img src='STLogo.png' align='right'>");
                    }
                    UserRegistry.User u = reg.getEmptyUser();
                    StringBuffer myRealName = new StringBuffer(real.trim());
                    StringBuilder newRealName = new StringBuilder();
                    for (int j = 0; j < myRealName.length(); j++) {
                        if (supportedNameSet.contains(myRealName.charAt(j))) {
                            newRealName.append(myRealName.charAt(j));
                        }
                    }
                    u.org = ctx.field("new_org").trim();
                    String randomEmail = UserRegistry.makePassword(null) + "@" + UserRegistry.makePassword(null).substring(0, 4).replace('.', '0')
                        + "." + u.org.replaceAll("_", "-") + ".example.com";
                    // String randomName =
                    // UserRegistry.makePassword(null).substring(0,5);
                    String randomPass = UserRegistry.makePassword(null);
                    u.name = newRealName.toString() + "_TESTER_";
                    u.email = newRealName + "." + randomEmail.trim();
                    String newLocales = ctx.field("new_locales").trim();
                    newLocales = UserRegistry.normalizeLocaleList(newLocales);
                    if (newLocales.isEmpty()) newLocales = "und";
                    u.locales = newLocales;
                    u.password = randomPass;
                    u.userlevel = ctx.fieldInt("new_userlevel", -1);
                    if (u.userlevel <= 0) {
                        u.userlevel = 999; // nice try
                    }
                    UserRegistry.User registeredUser = reg.newUser(ctx, u);
                    ctx.println("<i>" + ctx.iconHtml("okay", "added") + "'" + u.name
                        + "'. <br>Email: " + u.email + "  <br>Password: " + u.password + " <br>userlevel: " + u.getLevel() + "<br>");
                    if (autoProceed) {
                        ctx.print("You should be logged in shortly, otherwise click this link:");
                    } else {
                        ctx.print("You will be logged in when you click this link:");
                    }
                    ctx.print("</i>");
                    ctx.println("<br>");
                    registeredUser.printPasswordLink(ctx);
                    ctx.println("<br><br><br><br><i>Note: this is a test account, and may be removed at any time.</i>");
                    if (stayLoggedIn) {
                        ctx.addCookie(QUERY_EMAIL, u.email, TWELVE_WEEKS);
                        ctx.addCookie(QUERY_PASSWORD, u.password, TWELVE_WEEKS);
                    } else {
                        ctx.removeLoginCookies(request, response);
                    }
                    if (autoProceed) {
                        ctx.println("<script>window.setTimeout(function(){document.location = '" + ctx.base() + "/v?email=" + u.email + "&pw=" + u.password
                            + "';},3000);</script>");
                    }
                    ctx.println("</div>");
                }
            } else if (ctx.hasAdminPassword()) {
                ctx.response.sendRedirect(ctx.context("AdminPanel.jsp") + "?vap=" + vap);
                return;
            } else if (ctx.field("sql").equals(vap)) {
                Thread.currentThread().setName(baseThreadName + " ST sql");
                doSql(ctx); // SQL interface
            } else {
                Thread.currentThread().setName(baseThreadName + " ST ");
                doSession(ctx); // Session-based Survey main
            }
        } catch (Throwable t) { // should be THrowable
            t.printStackTrace();
            SurveyLog.logException(t, ctx);
            ctx.println("<div class='ferrbox'><h2>Error processing session: </h2><pre>" + t.toString() + "</pre></div>");
            SurveyLog.logger.warning("Failure with user: " + t);
        } finally {
            Thread.currentThread().setName(baseThreadName);
            ctx.close();
        }
    }

    /**
     * @param request
     * @param response
     * @param out
     * @throws ServletException
     * @throws IOException
     */
    public void showOfflinePage(HttpServletRequest request, HttpServletResponse response, PrintWriter out) throws ServletException, IOException {
        out.println(SHOWHIDE_SCRIPT);
        SurveyAjax.includeAjaxScript(request, response, SurveyAjax.AjaxType.STATUS);
        out.println("<script type=\"text/javascript\">timerSpeed = 60080;</script>"); // don't
        // flood
        // server
        // if
        // busted-
        // check
        // every
        // minute.
        out.print("<div id='st_err'><!-- for ajax errs --></div><span id='progress'>");
        out.print(getTopBox());
        out.println("</span>");
        // showSpecialHeader(out);
        // out.println("<h1>The CLDR Survey Tool is offline</h1>");
        // out.println("<div class='ferrbox'><pre>" + isBusted
        // +"</pre><hr>");
        // out.println("\n");
        // out.println(getShortened((SurveyForum.HTMLSafe(isBustedStack).replaceAll("\t",
        // "&nbsp;&nbsp;&nbsp;").replaceAll("\n", "<br>"))));
        // out.println("</div><br>");

        out.println("<hr>");
        // if(!isUnofficial) {
        // out.println("Please try this link for info: <a href='"+CLDR_WIKI_BASE+"?SurveyTool/Status'>"+CLDR_WIKI_BASE+"?SurveyTool/Status</a><br>");
        // out.println("<hr>");
        // }
        out.println("<p class='ferrbox'>An Administrator must intervene to bring the Survey Tool back online.");
        if (isUnofficial() || !isConfigSetup) {
            final File maintFile = getHelperFile();
            if (!maintFile.exists() && request != null) {
                try {
                    writeHelperFile(request, maintFile);
                } catch (IOException e) {
                    SurveyLog.warnOnce("Trying to write helper file " + maintFile.getAbsolutePath() + " - " + e.toString());
                }
            }
            if (maintFile.exists()) {
                out.println("<br/>If you are the administrator, try opening <a href='file://" + maintFile.getAbsolutePath() + "'>"
                    + maintFile.getAbsolutePath() + "</a> to choose setup mode.");
            } else {
                out.println("<br/>If you are the administrator, try loading the main SurveyTool page to create <a style='color: gray' href='file://"
                    + maintFile.getAbsolutePath() + "'>" + maintFile.getAbsolutePath() + "</a>");
            }
        } else {
            out.println("<br/> See: <a href='http://cldr.unicode.org/index/survey-tool#TOC-FAQ-Known-Bugs'>FAQ and Known Bugs</a>");
        }
        out.println("</p> <br> "
            + " <i>This message has been viewed " + pages + " time(s), SurveyTool has been down for " + isBustedTimer
            + "</i>");

        // if(false) { // dump static tables.
        // response.setContentType("application/xml; charset=utf-8");
        // WebContext xctx = new WebContext(request,response);
        // xctx.staticInfo();
        // xctx.close();
        // }
    }

    /**
     * Make sure we're started up, otherwise tell 'em, "please wait.."
     *
     * @param request
     * @param response
     * @return true if started, false if we are not (on false, get out, we're
     *         done printing..)
     * @throws IOException
     * @throws ServletException
     */
    private boolean ensureStartup(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        setInstance(request);
        if (!isSetup) {

            stopIfMaintenance(request);

            boolean isGET = "GET".equals(request.getMethod());
            int sec = 600; // was 4
            if (isBusted != null) {
                sec = 300;
            }
            String base = WebContext.base(request);
            String loadOnOk = base;
            if (isGET) {
                String qs = "";
                String pi = "";
                if (request.getPathInfo() != null && request.getPathInfo().length() > 0) {
                    pi = request.getPathInfo();
                }
                if (request.getQueryString() != null && request.getQueryString().length() > 0) {
                    qs = "?" + request.getQueryString();
                }
                loadOnOk = base + pi + qs;
                response.setHeader("Refresh", sec + "; " + loadOnOk);
            } else {
                loadOnOk = base + "?sorryPost=1";
            }
            response.setContentType("text/html; charset=utf-8");
            PrintWriter out = response.getWriter();
            out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\"><html><head>");
            out.println("<title>" + sysmsg("startup_title") + "</title>");
            out.println("<link rel='stylesheet' type='text/css' href='" + base + "/../surveytool.css'>");
            SurveyAjax.includeAjaxScript(request, response, SurveyAjax.AjaxType.STATUS);
            if (isUnofficial()) {
                out.println("<script type=\"text/javascript\">timerSpeed = 2500;</script>");
            } else {
                out.println("<script type=\"text/javascript\">timerSpeed = 10000;</script>");
            }
            // todo: include st_top.jsp instead
            out.println("</head><body>");
            if (isUnofficial()) {
                out.print("<div class='topnotices'><p class='unofficial' title='Not an official SurveyTool' >");
                out.print("Unofficial");
                out.println("</p></div>");
            }
            if (isMaintenance()) {
                final File maintFile = getHelperFile();
                final String maintMessage = getMaintMessage(maintFile, request);
                out.println("<h2>Setting up the SurveyTool</h2>");
                out.println("<div class='st_setup'>");
                out.println(maintMessage); // TODO
                out.println("</div>");
                out.println("<hr>");
            } else if (isBusted != null) {
                showOfflinePage(request, response, out);
            } else {
                out.print(sysmsg("startup_header"));

                out.print("<div id='st_err'><!-- for ajax errs --></div><span id='progress'>" + getTopBox() + "</span>");

                // String threadInfo = startupThread.htmlStatus();
                // if(threadInfo!=null) {
                // out.println("<b>Processing:"+threadInfo+"</b><br>");
                // }
                // if(progressWhat != null) {
                // out.println(getProgress()+"<br><hr><br>");
                // }

                out.print(sysmsg("startup_wait"));
            }
            out.println("<br><i id='uptime'> " + getGuestsAndUsers() + "</i><br>");
            // TODO: on up, goto <base>

            out.println("<script type=\"text/javascript\">loadOnOk = '" + loadOnOk + "';</script>");
            out.println("<script type=\"text/javascript\">clickContinue = '" + loadOnOk + "';</script>");
            if (!isMaintenance()) {
                if (!isGET) {
                    out.println("(Sorry,  we can't automatically retry your " + request.getMethod()
                        + " request - you may attempt Reload in a few seconds " + "<a href='" + base + "'>or click here</a><br>");
                } else {
                    out.println("If this page does not load in " + sec + " seconds, you may <a href='" + base
                        + "'>click here to go to the main Survey Tool page</a>");
                }
            }
            out.println("<noscript><h1>JavaScript is required for logging into the SurveyTool.</h1></noscript>");
            out.print(sysmsg("startup_footer"));
            out.println("<span id='visitors'></span>");
            out.print(getCurrev());
            // Per CLDR TC Meeting 2014-05-14 - Turn off Google Analytics.  If we decide we need it, we can add it back.
            //if (!SurveyMain.isUnofficial()) {
            //    out.println(CldrUtility.ANALYTICS);
            //}
            out.print("</body></html>");
            return false;
        } else {
            return true;
        }
    }

    /**
     * @return the fileBase
     */
    public static String getFileBase() {
        if (fileBase == null) {
            CLDRConfig survprops = CLDRConfig.getInstance();
            File base = survprops.getCldrBaseDirectory();
            fileBase = new File(base, "common/main").getAbsolutePath();
            fileBaseSeed = new File(base, "seed/main").getAbsolutePath();
            File commonAnnotations = new File(base, "common/annotations");
            fileBaseA = commonAnnotations.getAbsolutePath();
            commonAnnotations.mkdirs(); // make sure this exists
            File seedAnnotations = new File(base, "seed/annotations");
            seedAnnotations.mkdirs(); // make sure this exists
            fileBaseASeed = seedAnnotations.getAbsolutePath();
        }
        if (fileBase == null)
            throw new NullPointerException("fileBase==NULL");
        return fileBase;
    }

    /**
     * Get all of the file bases as an array
     * @return
     */
    public static File[] getFileBases() {
        getFileBase(); // load these
        File files[] = { new File(getFileBase()),
            new File(getFileBaseSeed()),
            new File(fileBaseA),
            new File(fileBaseASeed)
        };
        return files;
    }

    /**
     * @return
     */
    public static String getSurveyHome() {
        String cldrHome;
        CLDRConfig survprops = CLDRConfig.getInstance();

        if (!(survprops instanceof CLDRConfigImpl)) {
            File tmpHome = new File("testing_cldr_home");
            if (!tmpHome.isDirectory()) {
                if (!tmpHome.mkdir()) {
                    throw new InternalError("Couldn't create " + tmpHome.getAbsolutePath());
                }
            }
            cldrHome = tmpHome.getAbsolutePath();
            System.out.println("NOTE:  not inside of web process, using temporary CLDRHOME " + cldrHome);
        } else {
            cldrHome = survprops.getProperty("CLDRHOME");
        }
        if (cldrHome == null)
            throw new NullPointerException("CLDRHOME==null");
        return cldrHome;
    }

    /**
     * @return the fileBaseSeed
     */
    public static String getFileBaseSeed() {
        if (fileBaseSeed == null) {
            getFileBase();
        }
        if (fileBaseSeed == null)
            throw new NullPointerException("fileBaseSeed==NULL");
        return fileBaseSeed;
    }

    /**
     * SQL Console
     */
    private void doSql(WebContext ctx) {
        printHeader(ctx, "SQL Console@" + localhost());
        ctx.println("<script type=\"text/javascript\">timerSpeed = 6000;</script>");
        String q = ctx.field("q");
        boolean tblsel = false;
        printAdminMenu(ctx, "/AdminSql");
        ctx.println("<h1>SQL Console (" + DBUtils.getDBKind() + ")</h1>");

        ctx.println("<i style='font-size: small; color: silver;'>" + DBUtils.getInstance().getDBInfo() + "</i><br/>");

        if (isBusted != null) { // This may or may
            // not work. Survey
            // Tool is busted,
            // can we attempt
            // to get in via
            // SQL?
            ctx.println("<h4>ST not currently started, attempting to make SQL available</h4>");
            ctx.println("<pre>");
            specialMessage = "<b>SurveyTool is in an administrative mode- please log off.</b>";
            try {
                doStartupDB();
            } catch (Throwable t) {
                SurveyLog.logException(t, ctx);
                ctx.println("Caught: " + t.toString() + "\n");
            }
            ctx.println("</pre>");
        }

        if (q.length() == 0) {
            q = DBUtils.DB_SQL_ALLTABLES;
            tblsel = true;
        } else {
            ctx.println("<a href='" + ctx.base() + "?sql=" + vap + "'>[List of Tables]</a>");
        }
        ctx.println("<form method=POST action='" + ctx.base() + "'>");
        ctx.println("<input type=hidden name=sql value='" + vap + "'>");
        ctx.println("SQL: <input class='inputbox' name=q size=80 cols=80 value=\"" + q + "\"><br>");
        ctx.println("<label style='border: 1px'><input type=checkbox name=unltd>Show all?</label> ");
        ctx.println("<label style='border: 1px'><input type=checkbox name=isUpdate>U/I/D?</label> ");
        // ctx.println("<label style='border: 1px'><input type=checkbox name=isUser>UserDB?</label> ");
        ctx.println("<input type=submit name=do value=Query>");
        ctx.println("</form>");

        if (q.length() > 0) {
            SurveyLog.logger.severe("Raw SQL: " + q);
            ctx.println("<hr>");
            ctx.println("query: <tt>" + q + "</tt><br><br>");
            Connection conn = null;
            Statement s = null;
            try {
                int i, j;

                com.ibm.icu.dev.util.ElapsedTimer et = new com.ibm.icu.dev.util.ElapsedTimer();

                // if(ctx.field("isUser").length()>0) {
                // conn = getU_DBConnection();
                // } else {
                conn = dbUtils.getDBConnection();
                // }
                s = conn.createStatement();
                // s.setQueryTimeout(120); // 2 minute timeout. Not supported by
                // derby..
                if (ctx.field("isUpdate").length() > 0) {
                    int rc = s.executeUpdate(q);
                    conn.commit();
                    ctx.println("<br>Result: " + rc + " row(s) affected.<br>");
                } else {
                    ResultSet rs = s.executeQuery(q);
                    conn.commit();

                    ResultSetMetaData rsm = rs.getMetaData();
                    int cc = rsm.getColumnCount();

                    ctx.println("<table summary='SQL Results' class='sqlbox' border='2'><tr><th>#</th>");
                    for (i = 1; i <= cc; i++) {
                        ctx.println("<th>" + rsm.getColumnName(i) + "<br>");
                        int t = rsm.getColumnType(i);
                        switch (t) {
                        case java.sql.Types.VARCHAR:
                            ctx.println("VARCHAR");
                            break;
                        case java.sql.Types.INTEGER:
                            ctx.println("INTEGER");
                            break;
                        case java.sql.Types.BLOB:
                            ctx.println("BLOB");
                            break;
                        case java.sql.Types.TIMESTAMP:
                            ctx.println("TIMESTAMP");
                            break;
                        case java.sql.Types.BINARY:
                            ctx.println("BINARY");
                            break;
                        case java.sql.Types.LONGVARBINARY:
                            ctx.println("LONGVARBINARY");
                            break;
                        default:
                            ctx.println("type#" + t);
                            break;
                        }
                        ctx.println("(" + rsm.getColumnDisplaySize(i) + ")");
                        ctx.println("</th>");
                    }
                    if (tblsel) {
                        ctx.println("<th>Info</th><th>Rows</th>");
                    }
                    ctx.println("</tr>");
                    int limit = 30;
                    if (ctx.field("unltd").length() > 0) {
                        limit = 9999999;
                    }
                    for (j = 0; rs.next() && (j < limit); j++) {
                        ctx.println("<tr class='r" + (j % 2) + "'><th>" + j + "</th>");
                        for (i = 1; i <= cc; i++) {
                            String v;
                            try {
                                v = rs.getString(i);
                            } catch (SQLException se) {
                                if (se.getSQLState().equals("S1009")) {
                                    v = "0000-00-00 00:00:00";
                                } else {
                                    v = "(Err:" + DBUtils.unchainSqlException(se) + ")";
                                }
                            } catch (Throwable t) {
                                t.printStackTrace();
                                v = "(Err:" + t.toString() + ")";
                            }
                            if (v != null) {
                                ctx.println("<td>");
                                if (rsm.getColumnType(i) == java.sql.Types.LONGVARBINARY) {
                                    String uni = DBUtils.getStringUTF8(rs, i);
                                    ctx.println(uni + "<br>");
                                    byte bytes[] = rs.getBytes(i);
                                    for (byte b : bytes) {
                                        ctx.println(Integer.toHexString(((int) b) & 0xFF));
                                    }
                                    // } else
                                    // if(rsm.getColumnType(i)==java.sql.Types.TIMESTAMP)
                                    // {
                                    // String out="(unknown)";
                                    // try {
                                    // out=v.t
                                    // }
                                } else {
                                    ctx.println(v);
                                    // ctx.println("<br>"+rsm.getColumnTypeName(i));
                                }
                                ctx.print("</td>");
                                if (tblsel == true) {
                                    ctx.println("<td>");
                                    ctx.println("<form method=POST action='" + ctx.base() + "'>");
                                    ctx.println("<input type=hidden name=sql value='" + vap + "'>");
                                    ctx.println("<input type=hidden name=q value='" + "select * from " + v + " where 1 = 0'>");
                                    ctx.println("<input type=image src='" + ctx.context("zoom" + ".png")
                                        + "' value='Info'></form>");
                                    ctx.println("</td><td>");
                                    int count = DBUtils.sqlCount(ctx, conn, "select COUNT(*) from " + v);
                                    ctx.println(count + "</td>");
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
            } catch (SQLException se) {
                SurveyLog.logException(se, ctx);
                String complaint = "SQL err: " + DBUtils.unchainSqlException(se);

                ctx.println("<pre class='ferrbox'>" + complaint + "</pre>");
                SurveyLog.logger.severe(complaint);
            } catch (Throwable t) {
                SurveyLog.logException(t, ctx);
                String complaint = t.toString();
                t.printStackTrace();
                ctx.println("<pre class='ferrbox'>" + complaint + "</pre>");
                SurveyLog.logger.severe("Err in SQL execute: " + complaint);
            } finally {
                try {
                    s.close();
                } catch (SQLException se) {
                    SurveyLog.logException(se, ctx);
                    String complaint = "in s.closing: SQL err: " + DBUtils.unchainSqlException(se);

                    ctx.println("<pre class='ferrbox'> " + complaint + "</pre>");
                    SurveyLog.logger.severe(complaint);
                } catch (Throwable t) {
                    SurveyLog.logException(t, ctx);
                    String complaint = t.toString();
                    ctx.println("<pre class='ferrbox'> " + complaint + "</pre>");
                    SurveyLog.logger.severe("Err in SQL close: " + complaint);
                }
                DBUtils.closeDBConnection(conn);
            }
        }
        printFooter(ctx);
    }

    /**
     * @return memory statistics as a string
     */
    public static String freeMem() {
        Runtime r = Runtime.getRuntime();
        double total = r.totalMemory();
        total = total / 1024000.0;
        double free = r.freeMemory();
        free = free / 1024000.0;
        double used = total - free;
        return "Free memory: " + (int) free + "M / Used: " + (int) used + "M /: total: " + total + "M";
    }

    private static final void freeMem(int pages, int xpages) {
        SurveyLog.logger.warning("pages: " + pages + "+" + xpages + ", " + freeMem() + ".<br/>");
    }

    /**
     * Hash of twiddlable (toggleable) parameters
     *
     */
    Hashtable<String, Boolean> twidHash = new Hashtable<String, Boolean>();

    boolean showToggleTwid(WebContext ctx, String pref, String what) {
        String qKey = "twidb_" + pref;
        String nVal = ctx.field(qKey);
        if (nVal.length() > 0) {
            twidPut(pref, new Boolean(nVal).booleanValue());
            ctx.println("<div style='float: right;'><b class='disputed'>changed</b></div>");
        }
        boolean val = twidGetBool(pref, false);
        WebContext nuCtx = (WebContext) ctx.clone();
        nuCtx.addQuery(qKey, new Boolean(!val).toString());
        // nuCtx.println("<div class='pager' style='float: right;'>");
        nuCtx.println("<a href='" + nuCtx.url() + "'>" + what + ": "
            + ((val) ? "<span class='selected'>TRUE</span>" : "<span class='notselected'>false</span>") + "</a><br>");
        // nuCtx.println("</div>");
        return val;
    }

    private boolean twidGetBool(String key, boolean defVal) {
        Boolean b = (Boolean) twidHash.get(key);
        if (b == null) {
            return defVal;
        } else {
            return b.booleanValue();
        }
    }

    private boolean twidGetBool(String key) {
        return twidGetBool(key, false);
    }

    public void twidPut(String key, boolean val) {
        twidHash.put(key, new Boolean(val));
    }

    /* twiddle: these are params settable at runtime. */
    boolean twidBool(String x) {
        return twidBool(x, false);
    }

    synchronized boolean twidBool(String x, boolean defVal) {
        boolean ret = twidGetBool(x, defVal);
        twidPut(x, ret);
        return ret;
    }

    /**
     * Admin panel
     *
     * @param ctx
     * @param helpLink
     */
    void printAdminMenu(WebContext ctx, String helpLink) {

        boolean isDump = ctx.hasField("dump");
        boolean isSql = ctx.hasField("sql");

        ctx.print("<div style='float: right'><a class='notselected' href='" + ctx.base() + "'><b>[SurveyTool main]</b></a> | ");
        ctx.print("<a class='notselected' href='" + ctx.base() + "?letmein=" + vap
            + "&amp;email=admin@'><b>Login as admin@</b></a> | ");
        ctx.print("<a class='" + (isDump ? "" : "not") + "selected' href='" + ctx.context("AdminPanel.jsp") + "?vap=" + vap
            + "'>Admin</a>");
        ctx.print(" | ");
        ctx.print("<a class='" + (isSql ? "" : "not") + "selected' href='" + ctx.base() + "?sql=" + vap + "'>SQL</a>");
        ctx.print("<br>");
        ctx.printHelpLink(helpLink, "Admin Help", true);
        ctx.println("</div>");
    }

    private void doAdminPanel(WebContext ctx) {
        String action = ctx.field("action");

        printHeader(ctx, "Admin@" + localhost() + " | " + action);
        ctx.println("<script type=\"text/javascript\">timerSpeed = 6000;</script>");
        printAdminMenu(ctx, "/AdminDump");
        ctx.println("<h1>SurveyTool Administration</h1>");
        ctx.println("<hr>");

        ctx.includeFragment("adminpanel.jsp");

        printFooter(ctx);
    }

    public static void appendMemoryInfo(StringBuffer buf, boolean inline) {
        Runtime r = Runtime.getRuntime();
        double total = r.totalMemory();
        total = total / 1024000.0;
        double free = r.freeMemory();
        free = free / 1024000.0;

        String info = ("Free memory: " + free + "M / " + total + "M.");
        if (inline) {
            buf.append("<span title='" + info + "'>");
        }
        SurveyProgressManager.appendProgressBar(buf, total - free, total);
        if (inline) {
            buf.append("</span>");
        } else {
            buf.append(info + "<br/>");
        }
    }

    /* print a warning the first time. */

    public static class OnceWarner {
        boolean warnOnce;

        public OnceWarner(boolean warnOnce) {
            this.warnOnce = warnOnce;
        }

        public OnceWarner() {
            this.warnOnce = true;
        }

        private class OnceEntry {
            /**
             * @return # of times we've been nudged, inclusively
             */
            public int nudge() {
                return count++;
            }

            public OnceEntry(String desc) {
                this.desc = desc;
            }

            public int count() {
                return count;
            }

            public String desc() {
                return desc;
            }

            String desc;
            int count = 0;
        }

        private Map<String, OnceEntry> theMap = new TreeMap<String, OnceEntry>();

        private OnceEntry peekEntry(String key) {
            return theMap.get(key);
        }

        private OnceEntry getEntry(String key, String desc) {
            OnceEntry ent = peekEntry(key);
            if (ent == null) {
                theMap.put(key, (ent = new OnceEntry(desc)));
            }
            return ent;
        }

        public int count(String key) {
            OnceEntry ent = peekEntry(key);
            if (ent != null) {
                return ent.count();
            } else {
                return 0;
            }
        }

        public int warnOnce(WebContext ctx, String key, String icon, String desc, String longDesc) {
            int n = getEntry(key, desc).nudge();
            if (n == 1 || !warnOnce) {
                ctx.println(ctx.iconHtml(icon, "counter") + " " + desc + (warnOnce ? " (this message prints only once)" : "")
                    + "|" + longDesc + "<br>");
            }
            return n;
        }

        public int warnOnce(WebContext ctx, String key, String icon, String desc) {
            return warnOnce(ctx, key, icon, desc, "");
        }

        public void summarize(WebContext ctx) {
            int n = 0;
            if (theMap.isEmpty())
                return;
            ctx.println("<table class='tzbox'><tr><th>var</th><th>#</th><th>desc</th></tr>");
            for (Map.Entry<String, OnceEntry> e : theMap.entrySet()) {
                String k = e.getKey();
                OnceEntry v = e.getValue();

                ctx.println("<tr class='r" + (n % 2) + "'><th>" + k + "</th><td>" + v.count() + "</td><td>" + v.desc()
                    + "</td></tr>");
            }
            ctx.println("</table>");
        }
    }

    private static void throwIfBadLocale(String test0) {
        if (!SurveyMain.getLocalesSet().contains(CLDRLocale.getInstance(test0))) {
            throw new InternalError("Bad locale: " + test0);
        }
    }

    private void loadAllLocales(WebContext ctx, SurveyTask surveyTask) {
        CLDRLocale[] locales = getLocales();
        int nrInFiles = locales.length;
        com.ibm.icu.dev.util.ElapsedTimer allTime = new com.ibm.icu.dev.util.ElapsedTimer("Time to load all: {0}");
        SurveyLog.logger.info("Loading all..");
        // Connection connx = null;
        //int ti = 0;
        CLDRProgressTask progress = null;
        if (surveyTask != null && surveyTask.running()) {
            progress = surveyTask.openProgress("Loading All Locales", nrInFiles);
        }
        try {
            for (int i = 0; (null == SurveyMain.isBusted) && i < nrInFiles && (surveyTask == null || surveyTask.running()); i++) {
                CLDRLocale locale = locales[i];
                String localeName = locale.getBaseName();
                if (progress != null) {
                    progress.update(i, localeName);
                }
                if ((i > 0) && ((i % 50) == 0)) {
                    SurveyLog.logger.info("   " + i + "/" + nrInFiles + ". " + usedK() + "K mem used");
                    // if(ctx!=null) ctx.println("   "+ i + "/" + nrInFiles +
                    // ". " + usedK() + "K mem used<br>");
                }
                try {
                    getSTFactory().make(localeName, false);
                } catch (Throwable t) {
                    SurveyLog.logException(t, ctx);
                    t.printStackTrace();
                    String complaint = ("Error loading: " + localeName + " - " + t.toString() + " ...");
                    SurveyLog.logger.severe("loading all: " + complaint);
                    throw new InternalError("loading all: " + complaint);
                }
            }
            if (surveyTask != null && !surveyTask.running()) {
                SurveyLog.logger.warning("LoadAll: no longer running!\n");
            }
            if (surveyTask != null && surveyTask.running()) {
            }
            // closeDBConnection(connx);
            SurveyLog.logger.info("Loaded all. " + allTime);
            // if(ctx!=null) ctx.println("Loaded all." + allTime + "<br>");
            // if(ctx!=null) ctx.println("Updated "+n+"." + allTime + "<br>");
        } finally {
            if (progress != null) {
                progress.close();
            }
        }
    }

    /*
     * print menu of stuff to 'work with' a live user session..
     */
    private void printLiveUserMenu(WebContext ctx, CookieSession cs) {
        ctx.println("<a href='" + ctx.base() + "?dump=" + vap + "&amp;see=" + cs.id + "'>"
            + ctx.iconHtml("zoom", "SEE this user") + "see" + "</a> |");
        ctx.println("<a href='" + ctx.base() + "?&amp;s=" + cs.id + "'>" + "be" + "</a> |");
        ctx.println("<a href='" + ctx.base() + "?dump=" + vap + "&amp;unlink=" + cs.id + "'>" + "kick" + "</a>");
    }

    // static boolean showedComplaint = false;
    /**
     * print the header of the thing
     */
    public void printHeader(WebContext ctx, String title) {
        ctx.includeFragment("st_header.jsp");
        title = UCharacter.toTitleCase(SurveyMain.BASELINE_LOCALE.toLocale(), title, null);
        /*
         * if(showedComplaint == false) { showedComplaint = true;
         * SurveyLog.logger.warning("**** noindex,nofollow is disabled"); }
         */
        ctx.println("<META NAME=\"ROBOTS\" CONTENT=\"NOINDEX,NOFOLLOW\"> "); // NO
        // index
        ctx.println("<meta name='robots' content='noindex,nofollow'>");
        ctx.println("<meta name=\"gigabot\" content=\"noindex\">");
        ctx.println("<meta name=\"gigabot\" content=\"noarchive\">");
        ctx.println("<meta name=\"gigabot\" content=\"nofollow\">");
        ctx.println("<link rel='stylesheet' type='text/css' href='" + ctx.context("surveytool.css") + "'>");
        ctx.includeAjaxScript(AjaxType.STATUS);
        ctx.println("<title>CLDR " + getNewVersion() + " Survey Tool: ");
        if (ctx.getLocale() != null) {
            ctx.print(ctx.getLocale().getDisplayName() + " | ");
        }
        ctx.println(title + "</title>");
        ctx.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");

        if (true || isUnofficial() || ctx.prefBool(PREF_GROTTY)) { // no RSS on
            // the
            // official
            // site- for
            // now
            if (ctx.getLocale() != null) {
                ctx.println(fora.forumFeedStuff(ctx));
            } else {
                if (!ctx.hasField("x") && !ctx.hasField("do") && !ctx.hasField("sql") && !ctx.hasField("dump")) {
                    ctx.println(fora.mainFeedStuff(ctx));
                }
            }
        }

        ctx.put("TITLE", title);
        ctx.includeFragment("st_top.jsp");
        ctx.no_js_warning();
    }

    void showSpecialHeader(WebContext ctx) {
        showSpecialHeader(ctx, ctx.getOut());
    }

    void showSpecialHeader(PrintWriter out) {
        showSpecialHeader(null, out);
    }

    /**
     * print the top news banner. this must callable by non-context functions.
     *
     * @param out
     *            output stream - must be set
     * @param ctx
     *            context - optional.
     */
    void showSpecialHeader(WebContext ctx, PrintWriter out) {
        out.print(getSpecialHeader(ctx));
    }

    public String getSpecialHeader() {
        return getSpecialHeader(null);
    }

    public String getSpecialHeader(WebContext ctx) {
        StringBuffer out = new StringBuffer();
        String specialHeader = getSpecialHeaderText();
        if ((specialHeader != null) && (specialHeader.length() > 0)) {
            out.append("<div class='specialHeader'>");
            out.append(specialHeader);
            if (specialTimer != 0) {
                long t0 = System.currentTimeMillis();
                out.append("<br><b>Timer:</b> ");
                if (t0 > specialTimer) {
                    out.append("<b>The countdown time has arrived.</b>");
                } else {
                    out.append("The countdown timer has " + timeDiff(t0, specialTimer) + " remaining on it.");
                }
            }
            out.append("<br>");
            String threadInfo = startupThread.htmlStatus();
            if (threadInfo != null) {
                out.append("<b>Processing:" + threadInfo + "</b><br>");
            }
            out.append(getProgress());
            out.append("</div><br>");
        } else {
            String threadInfo = startupThread.htmlStatus();
            if (threadInfo != null) {
                out.append("<b>Processing:" + threadInfo + "</b><br>");
            }
            out.append(getProgress());
        }
        return out.toString();
    }

    /**
     * @return
     */
    public String getSpecialHeaderText() {
        String specialHeader = CLDRConfig.getInstance().getProperty("CLDR_HEADER");
        return specialHeader;
    }

    /*
     */
    public JSONObject statusJSON() throws JSONException {
        Runtime r = Runtime.getRuntime();
        double total = r.totalMemory();
        total = total / 1024000.0;
        double free = r.freeMemory();
        free = free / 1024000.0;

        double load = osmxbean.getSystemLoadAverage();
        CLDRConfig config = CLDRConfig.getInstance();
        return new JSONObject().put("isBusted", isBusted).put("lockOut", lockOut != null).put("isSetup", isSetup)
            .put("isUnofficial", isUnofficial()).put("environment", config.getEnvironment().name())
            .put("specialHeader", config.getProperty("CLDR_HEADER"))
            .put("specialTimerRemaining", specialTimer != 0 ? timeDiff(System.currentTimeMillis(), specialTimer) : null)
            .put("processing", startupThread.htmlStatus()).put("guests", CookieSession.getGuestCount())
            .put("users", CookieSession.getUserCount()).put("uptime", uptime).put("surveyRunningStamp", surveyRunningStamp.current())
            .put("memfree", free).put("memtotal", total).put("pages", pages).put("uptime", uptime).put("phase", phase())
            .put("currev", SurveyMain.getCurrevStr())
            .put("newVersion", newVersion).put("sysload", load).put("sysprocs", nProcs).put("dbopen", DBUtils.db_number_open)
            .put("dbused", DBUtils.db_number_used);
    }

    /**
     * Return the entire top 'box' including progress bars, busted notices, etc.
     *
     * @return
     */
    public String getTopBox() {
        return getTopBox(true);
    }

    /**
     * Return the entire top 'box' including progress bars, busted notices, etc.
     *
     * @return
     */
    public String getTopBox(boolean shorten) {
        StringBuffer out = new StringBuffer();
        if (isBusted != null) {
            out.append("<h1>The CLDR Survey Tool is offline</h1>");
            out.append("<div class='ferrbox'><pre>" + isBusted + "</pre><hr>");
            String stack = SurveyForum.HTMLSafe(isBustedStack).replaceAll("\t", "&nbsp;&nbsp;&nbsp;").replaceAll("\n", "<br>");
            if (shorten) {
                out.append(getShortened(stack));
            } else {
                out.append(stack);
            }
            out.append("</div><br>");
        }
        if (lockOut != null) {
            out.append("<h1>The CLDR Survey Tool is Locked for Maintenance</h1>");
        }
        out.append(getSpecialHeader());
        return out.toString();
    }

    public static final int PROGRESS_WID = 100;

    /** Progress bar width **/

    /*
     * (non-Javadoc)
     *
     * @see
     * org.unicode.cldr.web.CLDRProgressIndicator#openProgress(java.lang.String)
     */
    @Override
    public CLDRProgressTask openProgress(String what) {
        return openProgress(what, -100);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.unicode.cldr.web.CLDRProgressIndicator#openProgress(java.lang.String,
     * int)
     */
    @Override
    public CLDRProgressTask openProgress(String what, int max) {
        return progressManager.openProgress(what, max);
    }

    /**
     * Print out the progress
     *
     * @param ctx
     */
    public void showProgress(WebContext ctx) {
        ctx.print(getProgress());
    }

    /**
     * Return the current progress indicator.
     *
     * @return
     */
    public String getProgress() {
        return progressManager.getProgress();
    }

    public static String getCurrev() {
        String currev = CldrUtility.getProperty("CLDR_CURREV", null);
        if (currev != null) {
            return ("<a class='notselected' href='http://unicode.org/cldr/trac/changeset/" + currev + "'>r" + currev + "</a> \u00b7");
        } else {
            return "<span style='color: #ddd'>" + SURVEYMAIN_REVISION + "</span> \u00b7";
        }
    }

    public static String getCurrevStr() {
        return CldrUtility.getProperty("CLDR_CURREV", SURVEYMAIN_REVISION);
    }

    public void printFooter(WebContext ctx) {
        ctx.includeFragment("st_footer.jsp");
    }

    public static String getGuestsAndUsers() {
        StringBuffer out = new StringBuffer();
        int guests = CookieSession.getGuestCount();
        int users = CookieSession.getUserCount();
        if ((guests + users) > 0) { // ??
            out.append("~");
            if (users > 0) {
                out.append(users + " users");
            }
            if (guests > 0) {
                if (users > 0) {
                    out.append(", ");
                }
                out.append(" " + guests + " guests");
            }
        }
        out.append(", " + pages + "pg/" + uptime);
        double procs = osmxbean.getAvailableProcessors();
        double load = osmxbean.getSystemLoadAverage();
        if (load > 0.0) {
            int n = 256 - (int) Math.floor((load / procs) * 256.0);
            String asTwoHexString = Integer.toHexString(n);
            out.append("/<span title='Total System Load' style='background-color: #ff");
            if (asTwoHexString.length() == 1) {
                out.append("0");
                out.append(asTwoHexString);
                out.append("0");
                out.append(asTwoHexString);
            } else {
                out.append(asTwoHexString);
                out.append(asTwoHexString);
            }
            out.append("'>load:" + (int) Math.floor(load * 100.0) + "%</span>");
        }
        {
            DBUtils theDb = DBUtils.peekInstance();
            if (theDb != null) {
                try {
                    out.append(" <span title='DB Connections/Max Connections'>db:");
                    theDb.statsShort(out);
                    out.append("</span>");
                } catch (IOException e) {
                    // e.printStackTrace();
                }
            }
        }
        return out.toString();
    }

    /**
     * process the '_' parameter, if present, and set the locale.
     */
    public void setLocale(WebContext ctx) {
        String locale = ctx.field(QUERY_LOCALE);
        if (locale != null) { // knock out some bad cases
            if ((locale.indexOf('.') != -1) || (locale.indexOf('/') != -1)) {
                locale = null;
            }
        }
        // knock out nonexistent cases.
        if (locale != null && (locale.length() > 0)) {
            CLDRLocale l = CLDRLocale.getInstance(locale);
            if (getLocalesSet().contains(l)) {
                CLDRLocale theDefaultContent = getSupplementalDataInfo().getBaseFromDefaultContent(l);
                if (theDefaultContent != null) {
                    l = theDefaultContent;
                }
                ctx.setLocale(l);
            }
        }
    }

    // protected void printMenu(WebContext ctx, String which, String menu,
    // String title, String key) {

    /* print a user table without any extra help in it */
    public void printUserTable(WebContext ctx) {
        printUserTableWithHelp(ctx, null, null);
    }

    public void printUserTableWithHelp(WebContext ctx, String helpLink) {
        printUserTableWithHelp(ctx, helpLink, null);
    }

    public void printUserTableWithHelp(WebContext ctx, String helpLink, String helpName) {
        ctx.put("helpLink", helpLink);
        ctx.put("helpName", helpName);
        ctx.includeFragment("usermenu.jsp");
    }

    public static final String REDO_FIELD_LIST[] = { QUERY_LOCALE, QUERY_SECTION, QUERY_DO, "forum" };

    /**
     * Handle creating a new user
     */
    public void doNew(WebContext ctx) {
        printHeader(ctx, "New User");
        printUserTableWithHelp(ctx, "/AddModifyUser");
        if (UserRegistry.userCanCreateUsers(ctx.session.user)) {
            showAddUser(ctx);
        }
        ctx.println("<a href='" + ctx.url() + "'><b>Main SurveyTool Page</b></a><hr>");

        String new_name = ctx.field("new_name");
        String new_email = ctx.field("new_email");
        String new_locales = ctx.field("new_locales");
        new_locales = UserRegistry.normalizeLocaleList(new_locales);
        if (new_locales.isEmpty()) new_locales = "und";
        String new_org = ctx.field("new_org");
        int new_userlevel = ctx.fieldInt("new_userlevel", -1);

        if (!UserRegistry.userCreateOtherOrgs(ctx.session.user)) {
            new_org = ctx.session.user.org; // if not admin, must create user in
            // the same org
        }

        boolean newOrgOk = false;
        try {
            Organization newOrgEnum = Organization.fromString(new_org);
            newOrgOk = true;
        } catch (IllegalArgumentException iae) {
            newOrgOk = false;
        }

        if ((new_name == null) || (new_name.length() <= 0)) {
            ctx.println("<div class='sterrmsg'>" + ctx.iconHtml("stop", "Could not add user")
                + "Please fill in a name.. hit the Back button and try again.</div>");
        } else if ((new_email == null) || (new_email.length() <= 0)
            || ((-1 == new_email.indexOf('@')) || (-1 == new_email.indexOf('.')))) {
            ctx.println("<div class='sterrmsg'>" + ctx.iconHtml("stop", "Could not add user")
                + "Please fill in an <b>email</b>.. hit the Back button and try again.</div>");
        } else if (newOrgOk == false) {
            ctx.println("<div class='sterrmsg'>"
                + ctx.iconHtml("stop", "Could not add user")
                + "That Organization (<b>"
                + new_org
                + "</b>) is not valid. Either it is not spelled properly, or someone must update VoteResolver.Organization in VoteResolver.java</div>");
        } else if ((new_org == null) || (new_org.length() <= 0)) { // for ADMIN
            ctx.println("<div class='sterrmsg'>" + ctx.iconHtml("stop", "Could not add user")
                + "Please fill in an <b>Organization</b>.. hit the Back button and try again.</div>");
        } else if (new_userlevel < 0) {
            ctx.println("<div class='sterrmsg'>" + ctx.iconHtml("stop", "Could not add user")
                + "Please fill in a <b>user level</b>.. hit the Back button and try again.</div>");
        } else if (new_userlevel == UserRegistry.EXPERT && ctx.session.user.userlevel != UserRegistry.ADMIN) {
            ctx.println("<div class='sterrmsg'>" + ctx.iconHtml("stop", "Could not add user")
                + "Only Admin can create EXPERT users.. hit the Back button and try again.</div>");
        } else {
            UserRegistry.User u = reg.getEmptyUser();

            u.name = new_name;
            u.userlevel = UserRegistry.userCanCreateUserOfLevel(ctx.session.user, new_userlevel);
            u.email = new_email;
            u.org = new_org;
            u.locales = new_locales;
            u.password = UserRegistry.makePassword(u.email + u.org + ctx.session.user.email);

            SurveyLog.debug("UR: Attempt newuser by " + ctx.session.user.email + ": of " + u.email + " @ " + ctx.userIP());
            UserRegistry.User registeredUser = reg.newUser(ctx, u);

            if (registeredUser == null) {
                if (reg.get(new_email) != null) { // already exists..
                    ctx.println("<div class='sterrmsg'>"
                        + ctx.iconHtml("stop", "Could not add user")
                        + "A user with that email already exists. If you have permission, you may be able to edit this user: <tt>");
                    printUserZoomLink(ctx, new_email, new_email);
                    ctx.println("</tt> </div>");
                } else {
                    ctx.println("<div class='sterrmsg'>" + ctx.iconHtml("stop", "Could not add user") + "Couldn't add user <tt>"
                        + new_email + "</tt> - an unknown error occured.</div>");
                }
            } else {
                ctx.println("<i>" + ctx.iconHtml("okay", "added") + "user added.</i>");
                new_email = registeredUser.email.toLowerCase();
                WebContext nuCtx = (WebContext) ctx.clone();
                nuCtx.addQuery(QUERY_DO, "list");
                nuCtx.addQuery(LIST_JUST, URLEncoder.encode(new_email));
                ctx.println("" + "<form action='" + ctx.base() + "' method='POST'>");
                ctx.print("<input name='s' type='hidden' value='" + ctx.session.id + "'/>"
                    + "<input name='justu' type='hidden' value='" + new_email + "'/>"
                    + "<input name='do' type='hidden' value='list'/>" + "<input name='" + registeredUser.id + "_" + new_email
                    + "' type='hidden' value='sendpassword_'/>"
                    + "<label><input type='submit' value='Send Password Email to " + new_email + "'/>"
                    + ctx.iconHtml("warn", "Note..")
                    + "The password is not sent to the user automatically. <b>You must click this button!!</b></label>"
                    + "</form>" +

                    "<br>Click here to manage this user: '<b><a href='" + nuCtx.url() + "#u_" + u.email + "'>"
                    + ctx.iconHtml("zoom", "Zoom in on user") + "manage " + new_name + "</a></b>' page.</p>");
                ctx.print("<br>Their login link is: ");
                registeredUser.printPasswordLink(ctx);
                ctx.println(" (clicking this will log you in as them.)<br>");
            }
        }

        printFooter(ctx);
    }

    public static void showCoverageLanguage(WebContext ctx, String group, String lang) {
        ctx.print("<tt style='border: 1px solid gray; margin: 1px; padding: 1px;' class='codebox'>" + lang + "</tt> ("
            + CLDRLocale.getInstance(lang).getDisplayName() + ":<i>" + group + "</i>)</tt>");
    }

    public void showAddUser(WebContext ctx) {
        reg.setOrgList(); // setup the list of orgs
        String defaultorg = "";

        if (!UserRegistry.userIsAdmin(ctx.session.user)) {
            defaultorg = URLEncoder.encode(ctx.session.user.org);
        }

        ctx.println("<br><a href='" + ctx.jspLink("adduser.jsp") + "&amp;defaultorg=" + defaultorg + "'>Add User</a> |");
    }

    public void doCoverage(WebContext ctx) {
        boolean showCodes = false; // ctx.prefBool(PREF_SHOWCODES);
        printHeader(ctx, "Locale Coverage");

        if (!UserRegistry.userIsVetter(ctx.session.user)) {
            ctx.print("Not authorized.");
            return;
        }

        printUserTableWithHelp(ctx, "/LocaleCoverage");

        showAddUser(ctx);

        ctx.println("        <i>Showing only votes in the current release</i><br/>");
        ctx.print("<br>");
        ctx.println("<a href='" + ctx.url() + "'><b>SurveyTool in</b></a><hr>");
        String org = ctx.session.user.org;
        if (UserRegistry.userCreateOtherOrgs(ctx.session.user)) {
            org = null; // all
        }

        StandardCodes sc = StandardCodes.make();

        LocaleTree tree = getLocaleTree();

        WebContext subCtx = (WebContext) ctx.clone();
        subCtx.setQuery(QUERY_DO, "coverage");
        boolean participation = showTogglePref(subCtx, "cov_participation", "Participation Shown (click to toggle)");
        String missingLocalesForOrg = org;
        if (missingLocalesForOrg == null) {
            missingLocalesForOrg = showListPref(subCtx, PREF_COVTYP, "Coverage Type", WebContext.getLocaleCoverageOrganizations(), true);
        }
        if (missingLocalesForOrg == null || missingLocalesForOrg.length() == 0 || missingLocalesForOrg.equals("default")) {
            missingLocalesForOrg = "default"; // ?!
        }

        if (org == null) {
            ctx.println("<h4>Showing coverage for all organizations</h4>");
        } else {
            ctx.println("<h4>Showing coverage for: " + org + "</h4>");
        }

        if (missingLocalesForOrg != org) {
            ctx.println("<h4> (and missing locales for " + missingLocalesForOrg + ")</h4>");
        }

        File inFiles[] = getInFiles();
        //int nrInFiles = inFiles.length;
        // String localeList[] = new String[nrInFiles];
        Set<CLDRLocale> allLocs = SurveyMain.getLocalesSet();
        /*
         * for(int i=0;i<nrInFiles;i++) { String localeName =
         * inFiles[i].getName(); int dot = localeName.indexOf('.'); if(dot !=
         * -1) { localeName = localeName.substring(0,dot); }
         * localeList[i]=localeName; allLocs.add(localeName); }
         */
        int totalUsers = 0;
        int allUsers = 0; // users with all

        int totalSubmit = 0;
        int totalVet = 0;

        Map<CLDRLocale, Set<CLDRLocale>> intGroups = getIntGroups();

        Connection conn = null;
        Map<String, String> userMap = null;
        Map<String, String> nullMap = null;
        Hashtable<CLDRLocale, Hashtable<Integer, String>> localeStatus = null;
        Hashtable<CLDRLocale, Hashtable<Integer, String>> nullStatus = null;

        {
            userMap = new TreeMap<String, String>();
            nullMap = new TreeMap<String, String>();
            localeStatus = new Hashtable<CLDRLocale, Hashtable<Integer, String>>();
            nullStatus = new Hashtable<CLDRLocale, Hashtable<Integer, String>>();
        }

        Set<CLDRLocale> s = new TreeSet<CLDRLocale>();
        Set<CLDRLocale> badSet = new TreeSet<CLDRLocale>();
        PreparedStatement psMySubmit = null;
        PreparedStatement psnSubmit = null;

        try {
            conn = dbUtils.getDBConnection();
            psMySubmit = conn.prepareStatement("select COUNT(submitter) from " + DBUtils.Table.VOTE_VALUE + " where submitter=?",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            psnSubmit = conn.prepareStatement(
                "select COUNT(submitter) from " + DBUtils.Table.VOTE_VALUE + " where submitter=? and locale=?",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

            synchronized (reg) {
                java.sql.ResultSet rs = reg.list(org, conn);
                if (rs == null) {
                    ctx.println("<i>No results...</i>");
                    return;
                }
                if (UserRegistry.userCreateOtherOrgs(ctx.session.user)) {
                    org = "ALL"; // all
                }
                while (rs.next()) {
                    // n++;
                    int theirId = rs.getInt(1);
                    int theirLevel = rs.getInt(2);
                    String theirName = DBUtils.getStringUTF8(rs, 3);// rs.getString(3);
                    String theirEmail = rs.getString(4);
                    //String theirOrg = rs.getString(5);
                    String theirLocaleList = rs.getString(6);

                    String nameLink = "<a href='" + ctx.url() + ctx.urlConnector() + "do=list&" + LIST_JUST + "="
                        + URLEncoder.encode(theirEmail) + "' title='More on this user...'>" + theirName + " </a>";
                    // setup

                    if (participation && (conn != null)) {
                        psMySubmit.setInt(1, theirId);
                        psnSubmit.setInt(1, theirId);

                        int mySubmit = DBUtils.sqlCount(ctx, conn, psMySubmit);

                        String userInfo = "<tr><td>" + nameLink + "</td><td>" + "submits/votes: " + mySubmit + "</td></tr>";
                        if ((mySubmit) == 0) {
                            nullMap.put(theirName, userInfo);
                            // userInfo =
                            // "<span class='disabledbox' style='color:#888; border: 1px dashed red;'>"
                            // + userInfo + "</span>";
                        } else {
                            userMap.put(theirName, userInfo);
                        }

                        totalSubmit += mySubmit;
                    }
                    // String theirIntLocs = rs.getString(7);
                    // timestamp(8)
                    if ((theirLevel > 10) || (theirLevel <= 1)) {
                        continue;
                    }
                    totalUsers++;

                    // CookieSession theUser =
                    // CookieSession.retrieveUserWithoutTouch(theirEmail);
                    // ctx.println("   <td>" +
                    // UserRegistry.prettyPrintLocale(null) + "</td> ");
                    // ctx.println("    <td>" +
                    // UserRegistry.prettyPrintLocale(theirLocales) + "</td>");
                    if ((theirLocaleList == null) || theirLocaleList.length() == 0) {
                        allUsers++;
                        continue;
                    }
                    if (UserRegistry.isAllLocales(theirLocaleList)) {
                        // all.
                        allUsers++;
                    } else {
                        CLDRLocale theirLocales[] = UserRegistry.tokenizeCLDRLocale(theirLocaleList);
                        // int hitList[] = new int[theirLocales.length]; // # of
                        // times each is used
                        Set<CLDRLocale> theirSet = new HashSet<CLDRLocale>(); // set
                        // of
                        // locales
                        // this
                        // vetter
                        // has
                        // access
                        // to
                        for (int j = 0; j < theirLocales.length; j++) {
                            Set<CLDRLocale> subSet = intGroups.get(theirLocales[j]); // Is
                            // it
                            // an
                            // interest
                            // group?
                            // (de,
                            // fr,
                            // ..)
                            if (subSet != null) {
                                theirSet.addAll(subSet); // add all sublocs
                            } else if (allLocs.contains(theirLocales[j])) {
                                theirSet.add(theirLocales[j]);
                            } else {
                                badSet.add(theirLocales[j]);
                            }
                        }
                        for (CLDRLocale theLocale : theirSet) {
                            s.add(theLocale);
                            // hitList[j]++;
                            // ctx.println("user " + theirEmail + " with " +
                            // theirLocales[j] + " covers " + theLocale +
                            // "<br>");
                            Hashtable<CLDRLocale, Hashtable<Integer, String>> theHash = localeStatus; // to
                            // the
                            // 'status'
                            // field
                            String userInfo = nameLink + " ";
                            if (participation && conn != null) {
                                psnSubmit.setString(2, theLocale.getBaseName());

                                int nSubmit = DBUtils.sqlCount(ctx, conn, psnSubmit);

                                if ((nSubmit) == 0) {
                                    theHash = nullStatus; // vetter w/ no work
                                    // done
                                }

                                if (nSubmit > 0) {
                                    userInfo = userInfo + " submits: " + nSubmit + " ";
                                }

                                // if((nSubmit+nVet)==0) {
                                // userInfo =
                                // "<span class='disabledbox' style='color:#888; border: 1px dashed red;'>"
                                // + userInfo + "</span>";
                                // userInfo = "<strike>"+userInfo+"</strike>";
                                // }
                                // userInfo = userInfo +
                                // ", file: "+theLocale+", th: " +
                                // theirLocales[j];
                            }
                            Hashtable<Integer, String> oldStr = theHash.get(theLocale);

                            if (oldStr == null) {
                                oldStr = new Hashtable<Integer, String>();
                                theHash.put(theLocale, oldStr);
                            } else {
                                // // oldStr = oldStr+"<br>\n";
                            }

                            oldStr.put(new Integer(theirId), userInfo + "<!-- " + theLocale + " -->");

                        }
                        /*
                         * for(int j=0;j<theirLocales.length;j++) {
                         * if(hitList[j]==0) { badSet.add(theirLocales[j]); } }
                         */
                    }
                }
                // #level $name $email $org
                rs.close();
            } /* end synchronized(reg) */
        } catch (SQLException se) {
            SurveyLog.logger.log(java.util.logging.Level.WARNING,
                "Query for org " + org + " failed: " + DBUtils.unchainSqlException(se), se);
            ctx.println("<i>Failure: " + DBUtils.unchainSqlException(se) + "</i><br>");
        } finally {
            DBUtils.close(psMySubmit, psnSubmit, conn);
        }

        // Map<String, CLDRLocale> lm = getLocaleListMap();
        // if(lm == null) {
        // busted("Can't load CLDR data files from " + fileBase);
        // throw new RuntimeException("Can't load CLDR data files from " +
        // fileBase);
        // }

        // if (!badSet.isEmpty()) {
        // ctx.println("<B>Locales not in CLDR but assigned to vetters:</b> <br>");
        // int n = 0;
        // boolean first = true;
        // Set<CLDRLocale> subSet = new TreeSet<CLDRLocale>();
        // for (CLDRLocale li : badSet) {
        // if (li.toString().indexOf('_') >= 0) {
        // n++;
        // subSet.add(li);
        // continue;
        // }
        // if (first == false) {
        // ctx.print(" ");
        // } else {
        // first = false;
        // }
        // ctx.print("<tt style='border: 1px solid gray; margin: 1px; padding: 1px;' class='codebox'>"
        // + li.toString()
        // + "</tt>");
        // }
        // ctx.println("<br>");
        // if (n > 0) {
        // ctx.println("Note: "
        // + n
        // +
        // " locale(s) were specified that were sublocales. This is no longer supported, specify the top level locale (en, not en_US or en_Shaw): <br><font size=-1>");
        // if (isUnofficial())
        // for (CLDRLocale li : subSet) {
        // ctx.print(" <tt style='border: 1px solid gray; margin: 1px; padding: 1px;' class='codebox'><font size=-1>"
        // + li.toString() + "</font></tt>");
        // }
        // ctx.println("</font><br>");
        // }
        // }

        // Now, calculate coverage of requested locales for this organization
        // sc.getGroup(locale,missingLocalesForOrg);
        Set<CLDRLocale> languagesNotInCLDR = new TreeSet<CLDRLocale>();
        Set<CLDRLocale> languagesMissing = new HashSet<CLDRLocale>();
        Set<CLDRLocale> allLanguages = new TreeSet<CLDRLocale>();
        {
            for (String code : sc.getAvailableCodes("language")) {
                allLanguages.add(CLDRLocale.getInstance(code));
            }
        }
        for (Iterator<CLDRLocale> li = allLanguages.iterator(); li.hasNext();) {
            CLDRLocale lang = (CLDRLocale) (li.next());
            String group = sc.getGroup(lang.getBaseName(), missingLocalesForOrg);
            if ((group != null) &&
            // Not sure why we did this... jce (!"basic".equals(group)) && //
            // exclude it for being basic
                (null == getSupplementalDataInfo().getBaseFromDefaultContent(CLDRLocale.getInstance(group)))) {
                // SurveyLog.logger.warning("getGroup("+lang+", " +
                // missingLocalesForOrg + ") = " + group);
                if (!isValidLocale(lang)) {
                    // SurveyLog.logger.warning("-> not in lm: " + lang);
                    languagesNotInCLDR.add(lang);
                } else {
                    if (!s.contains(lang)) {
                        // SurveyLog.logger.warning("-> not in S: " + lang);
                        languagesMissing.add(lang);
                    } else {
                        // SurveyLog.logger.warning("in lm && s: " + lang);
                    }
                }
            }
        }

        // if (!languagesNotInCLDR.isEmpty()) {
        // ctx.println("<p class='hang'>" + ctx.iconHtml("stop",
        // "locales missing from CLDR") + "<B>Required by "
        // + missingLocalesForOrg + " but not in CLDR: </b>");
        // boolean first = true;
        // for (Iterator<CLDRLocale> li = languagesNotInCLDR.iterator();
        // li.hasNext();) {
        // if (first == false) {
        // ctx.print(", ");
        // } else {
        // first = false;
        // }
        // String lang = li.next().toString();
        // showCoverageLanguage(ctx, sc.getGroup(lang, missingLocalesForOrg),
        // lang);
        // }
        // ctx.println("<br>");
        // ctx.printHelpLink("/LocaleMissing", "Locale is missing from CLDR");
        // ctx.println("</p><br>");
        // }
        //
        // if (!languagesMissing.isEmpty()) {
        // ctx.println("<p class='hang'>" + ctx.iconHtml("stop",
        // "locales without vetters") + "<B>Required by "
        // + missingLocalesForOrg + " but no vetters: </b>");
        // boolean first = true;
        // for (Iterator<CLDRLocale> li = languagesMissing.iterator();
        // li.hasNext();) {
        // if (first == false) {
        // ctx.print(", ");
        // } else {
        // first = false;
        // }
        // String lang = li.next().toString();
        // showCoverageLanguage(ctx, sc.getGroup(lang, missingLocalesForOrg),
        // lang);
        // }
        // ctx.println("<br>");
        // // ctx.printHelpLink("/LocaleMissing","Locale is missing from CLDR");
        // ctx.println("</p><br>");
        // }

        ctx.println("Locales in <b>bold</b> have assigned vetters.<br><table summary='Locale Coverage' border=1 class='list'>");
        int n = 0;
        for (String ln : tree.getTopLocales()) {
            n++;
            CLDRLocale aLocale = tree.getLocaleCode(ln);
            ctx.print("<tr class='row" + (n % 2) + "'>");
            ctx.print(" <td valign='top'>");
            boolean has = (s.contains(aLocale));
            if (has) {
                ctx.print("<span class='selected'>");
            } else {
                ctx.print("<span class='disabledbox' style='color:#888'>");
            }
            // ctx.print(aLocale);
            // ctx.print("<br><font size='-1'>"+new
            // ULocale(aLocale).getDisplayName()+"</font>");
            ctx.print(decoratedLocaleName(aLocale, ln.toString(), aLocale.toString()));
            ctx.print("</span>");
            if (languagesMissing.contains(aLocale)) {
                ctx.println("<br>" + ctx.iconHtml("stop", "No " + missingLocalesForOrg + " vetters") + "<i>(coverage: "
                    + sc.getGroup(aLocale.toString(), missingLocalesForOrg) + ")</i>");
            }

            if (showCodes) {
                ctx.println("<br><tt>" + aLocale + "</tt>");
            }
            if (localeStatus != null && !localeStatus.isEmpty()) {
                Hashtable<Integer, String> what = localeStatus.get(aLocale);
                if (what != null) {
                    ctx.println("<ul>");
                    for (Iterator<String> i = what.values().iterator(); i.hasNext();) {
                        ctx.println("<li>" + i.next() + "</li>");
                    }
                    ctx.println("</ul>");
                }
            }
            boolean localeIsDefaultContent = getSupplementalDataInfo().isDefaultContent(aLocale);
            if (localeIsDefaultContent) {
                ctx.println(" (<i>default content</i>)");
            } else if (participation && nullStatus != null && !nullStatus.isEmpty()) {
                Hashtable<Integer, String> what = nullStatus.get(aLocale);
                if (what != null) {
                    ctx.println("<br><blockquote> <b>Did not participate:</b> ");
                    for (Iterator<String> i = what.values().iterator(); i.hasNext();) {
                        ctx.println(i.next().toString());
                        if (i.hasNext()) {
                            ctx.println(", ");
                        }
                    }
                    ctx.println("</blockquote>");
                }
            }
            ctx.println(" </td>");

            Map<String, CLDRLocale> sm = tree.getSubLocales(aLocale); // sub
            // locales

            ctx.println("<td valign='top'>");
            int j = 0;
            for (Iterator<String> si = sm.keySet().iterator(); si.hasNext();) {
                String sn = si.next().toString();
                CLDRLocale subLocale = sm.get(sn);
                // if(subLocale.length()>0) {

                has = (s.contains(subLocale));

                if (j > 0) {
                    if (localeStatus == null) {
                        ctx.println(", ");
                    } else {
                        ctx.println("<br>");
                    }
                }

                if (has) {
                    ctx.print("<span class='selected'>");
                } else {
                    ctx.print("<span class='disabledbox' style='color:#888'>");
                }
                // ctx.print(subLocale);
                // ctx.print("&nbsp;<font size='-1'>("+new
                // ULocale(subLocale).getDisplayName()+")</font>");

                ctx.print(decoratedLocaleName(CLDRLocale.getInstance(subLocale.toString()), sn, subLocale.toString()));
                // if(has) {
                ctx.print("</span>");
                // }

                // printLocaleLink(baseContext, subLocale, sn);
                if (showCodes) {
                    ctx.println("&nbsp;-&nbsp;<tt>" + subLocale + "</tt>");
                }
                boolean isDc = getSupplementalDataInfo().isDefaultContent(subLocale);

                if (localeStatus != null && !nullStatus.isEmpty()) {
                    Hashtable<Integer, String> what = localeStatus.get(subLocale);
                    if (what != null) {
                        ctx.println("<ul>");
                        for (Iterator<String> i = what.values().iterator(); i.hasNext();) {
                            ctx.println("<li>" + i.next() + "</li>");
                        }
                        ctx.println("</ul>");
                    }
                }
                if (isDc) {
                    ctx.println(" (<i>default content</i>)");
                } // else if(participation && nullStatus!=null &&
                  // !nullStatus.isEmpty()) {
                  // Hashtable<Integer, String> what =
                  // nullStatus.get(subLocale);
                  // if(what!=null) {
                  // ctx.println("<br><blockquote><b>Did not participate:</b> ");
                  // for(Iterator<String> i =
                  // what.values().iterator();i.hasNext();) {
                  // ctx.println(i.next().toString() );
                  // if(i.hasNext()) {
                  // ctx.println(", ");
                  // }
                  // }
                  // ctx.println("</blockquote>");
                  // }
                  // }
                  // }
                j++;
            }
            ctx.println("</td>");
            ctx.println("</tr>");
        }
        ctx.println("</table> ");
        ctx.println(totalUsers + "  users, including " + allUsers + " with 'all' privs (not counted against the locale list)<br>");

        if (conn != null) {
            if (participation) {
                ctx.println("Selected users have submitted " + totalSubmit + " items, and voted for " + totalVet
                    + " items (including implied votes).<br>");
            }
            // int
            // totalResult=sqlCount(ctx,conn,"select COUNT(*) from CLDR_RESULT");
            // int
            // totalData=sqlCount(ctx,conn,"select COUNT(id) from CLDR_DATA");
            // ctx.println("In all, the SurveyTool has " + totalResult +
            // " items.<br>");

            if (participation) {
                ctx.println("<hr>");
                ctx.println("<h4>Participated: " + userMap.size() + "</h4><table border='1'>");
                for (Iterator<String> i = userMap.values().iterator(); i.hasNext();) {
                    String which = (String) i.next();
                    ctx.println(which);
                }
                ctx.println("</table><h4>Did Not Participate at all: " + nullMap.size() + "</h4><table border='1'>");
                for (Iterator<String> i = nullMap.values().iterator(); i.hasNext();) {
                    String which = (String) i.next();
                    ctx.println(which);
                }
                ctx.println("</table>");
            }
            DBUtils.closeDBConnection(conn);
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

    public static final String PREF_SHCOVERAGE = "showcov";

    public void doUDump(WebContext ctx) {
        ctx.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        // ctx.println("<!DOCTYPE ldml SYSTEM \"http://.../.../stusers.dtd\">");
        ctx.println("<users host=\"" + ctx.serverHostport() + "\">");
        String org = null;
        Connection conn = null;
        try {
            conn = dbUtils.getDBConnection();
            synchronized (reg) {
                java.sql.ResultSet rs = reg.list(org, conn);
                if (rs == null) {
                    ctx.println("\t<!-- No results -->");
                    return;
                }
                while (rs.next()) {
                    int theirId = rs.getInt(1);
                    int theirLevel = rs.getInt(2);
                    String theirName = DBUtils.getStringUTF8(rs, 3);// rs.getString(3);
                    String theirEmail = rs.getString(4);
                    String theirOrg = rs.getString(5);
                    String theirLocales = rs.getString(6);

                    ctx.println("\t<user id=\"" + theirId + "\" email=\"" + theirEmail + "\">");
                    ctx.println("\t\t<level n=\"" + theirLevel + "\" type=\"" + UserRegistry.levelAsStr(theirLevel) + "\"/>");
                    ctx.println("\t\t<name>" + theirName + "</name>");
                    ctx.println("\t\t<org>" + theirOrg + "</org>");
                    ctx.println("\t\t<locales type=\"edit\">");
                    Set<CLDRLocale> locs = UserRegistry.tokenizeValidCLDRLocale(theirLocales);
                    for (CLDRLocale loc : locs) {
                        ctx.println("\t\t\t<locale id=\"" + loc.getBaseName() + "\"/>");
                    }
                    ctx.println("\t\t</locales>");
                    ctx.println("\t</user>");
                }
            } /* end synchronized(reg) */
        } catch (SQLException se) {
            SurveyLog.logger.log(java.util.logging.Level.WARNING,
                "Query for org " + org + " failed: " + DBUtils.unchainSqlException(se), se);
            ctx.println("<!-- Failure: " + DBUtils.unchainSqlException(se) + " -->");
        } finally {
            DBUtils.close(conn);
        }
        ctx.println("</users>");
    }

    /**
     * List Users
     *
     * @param ctx
     */
    public void doList(WebContext ctx) {
        int n = 0;
        String just = ctx.field(LIST_JUST);
        String doWhat = ctx.field(QUERY_DO);
        boolean justme = false; // "my account" mode
        String listName = "list";
        if (just.length() == 0) {
            just = null;
        } else {
            justme = ctx.session.user.email.equals(just);
        }
        if (doWhat.equals("listu")) {
            listName = "listu";
            just = ctx.session.user.email;
            justme = true;
        }
        WebContext subCtx = new WebContext(ctx);
        subCtx.setQuery(QUERY_DO, doWhat);
        if (justme) {
            printHeader(ctx, "My Account");
        } else {
            printHeader(ctx, "List Users" + ((just == null) ? "" : (" - " + just)));
        }

        printUserTableWithHelp(ctx, "/AddModifyUser");
        ctx.print(" | ");
        printMenu(ctx, doWhat, "coverage", "Show Vetting Participation", QUERY_DO);

        if (UserRegistry.userIsTC(ctx.session.user)) {
            ctx.println("| <a class='notselected' href='v#tc-emaillist'>Email Address of Users Who Participated</a>");
            ctx.print(" | ");
        }

        if (UserRegistry.userCanCreateUsers(ctx.session.user)) {
            showAddUser(ctx);
            // ctx.println("<a href='" + ctx.jspLink("adduser.jsp")
            // +"'>[Add User]</a> |");
        }
        // ctx.println("<a href='" +
        // ctx.url()+ctx.urlConnector()+"do=coverage'>[Locale Coverage Reports]</a>");
        ctx.print("<br>");
        ctx.println("<a href='" + ctx.url() + "'><b>SurveyTool main</b></a><hr>");
        String org = ctx.session.user.org;
        if (just != null) {
            ctx.println("<a href='" + ctx.url() + ctx.urlConnector() + "do=list&p_justorg='>\u22d6 Show all users</a><br>");
        }
        if (UserRegistry.userIsAdmin(ctx.session.user)) {
            if (just == null) { // show a filter
                // WebContext subsubCtx = new WebContext(subCtx);
                // subsubCtx.put(string, object)
                String list0[] = UserRegistry.getOrgList();
                String list1[] = new String[list0.length + 1];
                System.arraycopy(list0, 0, list1, 1, list0.length);
                list1[0] = "Show All";
                org = showListSetting(subCtx, "p_justorg", "Filter Organization", list1, true);
                if (org.equals(list1[0])) {
                    org = null;
                }
            } else {
                org = null; // all
            }
        }
        String cleanEmail = null;
        String sendWhat = ctx.field(LIST_MAILUSER_WHAT);
        boolean areSendingMail = false;
        boolean didConfirmMail = false;
        boolean showLocked = ctx.prefBool(PREF_SHOWLOCKED);
        boolean areSendingDisp = (ctx.field(LIST_MAILUSER + "_d").length()) > 0; // sending
        // a
        // dispute
        // note?
        String mailBody = null;
        String mailSubj = null;
        boolean hideUserList = false;
        if (UserRegistry.userCanEmailUsers(ctx.session.user)) {
            cleanEmail = ctx.session.user.email;
            if (cleanEmail.equals("admin@")) {
                cleanEmail = "surveytool@unicode.org";
            }
            if (ctx.field(LIST_MAILUSER_CONFIRM).equals(cleanEmail)) {
                ctx.println("<h1>sending mail to users...</h4>");
                didConfirmMail = true;
                mailBody = "SurveyTool Message ---\n" + sendWhat
                    + "\n--------\n\nSurvey Tool: http://st.unicode.org" + /*
                                                                           * ctx
                                                                           * .
                                                                           * serverHostport
                                                                           * (
                                                                           * )
                                                                           * +
                                                                           */ctx.base() + "\n\n";
                mailSubj = "CLDR SurveyTool message from " + ctx.session.user.name;
                if (!areSendingDisp) {
                    areSendingMail = true; // we are ready to go ahead and
                    // mail..
                }
            } else if (ctx.hasField(LIST_MAILUSER_CONFIRM)) {
                ctx.println("<h1 class='ferrbox'>" + ctx.iconHtml("stop", "emails did not match")
                    + " not sending mail - you did not confirm the email address. See form at bottom of page." + "</h1>");
            }

            if (!areSendingMail && !areSendingDisp && ctx.hasField(LIST_MAILUSER)) {
                hideUserList = true; // hide the user list temporarily.
            }
        }
        Connection conn = null;
        try {
            conn = dbUtils.getDBConnection();
            synchronized (reg) {
                java.sql.ResultSet rs = reg.list(org, conn);
                if (rs == null) {
                    ctx.println("<i>No results...</i>");
                    return;
                }
                if (org == null) {
                    org = "ALL"; // all
                }
                if (justme) {
                    ctx.println("<h2>My Account</h2>");
                } else {
                    ctx.println("<h2>Users for " + org + "</h2>");
                    if (UserRegistry.userIsTC(ctx.session.user)) {
                        showTogglePref(subCtx, PREF_SHOWLOCKED, "Show locked users:");
                    }
                    ctx.println("<br>");
                    if (UserRegistry.userCanModifyUsers(ctx.session.user)) {
                        ctx.println("<div class='fnotebox'>"
                            + "Changing user level or locales while a user is active will result in  "
                            + " destruction of their session. Check if they have been working recently.</div>");
                    }
                }
                // Preset box
                boolean preFormed = false;

                if (hideUserList) {
                    String warnHash = "userlist";
                    ctx.println("<div id='h_" + warnHash + "'><a href='javascript:show(\"" + warnHash + "\")'>"
                        + "<b>+</b> Click here to show the user list...</a></div>");
                    ctx.println("<!-- <noscript>Warning: </noscript> -->" + "<div style='display: none' id='" + warnHash + "'>");
                    ctx.println("<a href='javascript:hide(\"" + warnHash + "\")'>" + "(<b>- hide userlist</b>)</a><br>");

                }

                if ((just == null) && UserRegistry.userCanModifyUsers(ctx.session.user) && !justme) {
                    ctx.println("<div class='pager' style='align: right; float: right; margin-left: 4px;'>");
                    ctx.println("<form method=POST action='" + ctx.base() + "'>");
                    ctx.printUrlAsHiddenFields();
                    ctx.println("Set menus:<br><label>all ");
                    ctx.println("<select name='preset_from'>");
                    ctx.println("   <option>" + LIST_ACTION_NONE + "</option>");
                    for (int i = 0; i < UserRegistry.ALL_LEVELS.length; i++) {
                        ctx.println("<option class='user" + UserRegistry.ALL_LEVELS[i] + "' ");
                        ctx.println(" value='" + UserRegistry.ALL_LEVELS[i] + "'>"
                            + UserRegistry.levelToStr(ctx, UserRegistry.ALL_LEVELS[i]) + "</option>");
                    }
                    ctx.println("</select></label> <br>");
                    ctx.println(" <label>to");
                    ctx.println("<select name='preset_do'>");
                    ctx.println("   <option>" + LIST_ACTION_NONE + "</option>");

//                    for(int i=0;i<UserRegistry.ALL_LEVELS.length;i++) {
//                        if(i != UserRegistry.LOCKED) continue; // only show 'locked'
//                        ctx.println("<option class='user" +
//                            UserRegistry.ALL_LEVELS[i] + "' ");
//                        ctx.println(" value='"
//                            +LIST_ACTION_SETLEVEL+UserRegistry.ALL_LEVELS[i]+"'>" +
//                            UserRegistry.levelToStr(ctx, UserRegistry.ALL_LEVELS[i])
//                            + "</option>");
//                    }
//                    ctx.println("   <option>" +
//                        LIST_ACTION_NONE + "</option>");
//                    ctx.println("   <option value='" + LIST_ACTION_DELETE0
//                        +"'>Delete user..</option>"); ctx.println("   <option>" +
//                            LIST_ACTION_NONE + "</option>");

                    ctx.println("   <option value='" + LIST_ACTION_SHOW_PASSWORD + "'>Show password URL...</option>");
                    ctx.println("   <option value='" + LIST_ACTION_SEND_PASSWORD + "'>Resend password...</option>");
                    // ctx.println("   <option value='" + LIST_ACTION_SETLOCALES
                    // + "'>Set locales...</option>");
                    ctx.println("</select></label> <br>");
                    if (just != null) {
                        ctx.print("<input type='hidden' name='" + LIST_JUST + "' value='" + just + "'>");
                    }
                    ctx.println("<input type='submit' name='do' value='" + listName + "'></form>");
                    if ((ctx.field("preset_from").length() > 0) && !ctx.field("preset_from").equals(LIST_ACTION_NONE)) {
                        ctx.println("<hr><i><b>Menus have been pre-filled. <br> Confirm your choices and click Change.</b></i>");
                        ctx.println("<form method=POST action='" + ctx.base() + "'>");
                        ctx.println("<input type='submit' name='doBtn' value='Change'>");
                        preFormed = true;
                    }
                    ctx.println("</div>");
                }
                int preset_fromint = ctx.fieldInt("preset_from", -1);
                String preset_do = ctx.field("preset_do");
                if (preset_do.equals(LIST_ACTION_NONE)) {
                    preset_do = "nothing";
                }
                if (/* (just==null)&& */((UserRegistry.userCanModifyUsers(ctx.session.user))) && !preFormed) { // form
                    // was
                    // already
                    // started,
                    // above
                    ctx.println("<form method=POST action='" + ctx.base() + "'>");
                }
                if (just != null) {
                    ctx.print("<input type='hidden' name='" + LIST_JUST + "' value='" + just + "'>");
                }
                if (justme || UserRegistry.userCanModifyUsers(ctx.session.user)) {
                    ctx.printUrlAsHiddenFields();
                    ctx.println("<input type='hidden' name='do' value='" + listName + "'>");
                    ctx.println("<input type='submit' name='doBtn' value='Do Action'>");
                }
                ctx.println("<table id='userListTable' summary='User List' class='userlist' border='2'>");
                ctx.println(
                    "<thead> <tr><th></th><th>Organization / Level</th><th>Name/Email</th><th>Action</th><th>Locales</th><th>Seen</th></tr></thead><tbody>");
                String oldOrg = null;
                int locked = 0;
                JSONArray shownUsers = new JSONArray();
                while (rs.next()) {
                    int theirId = rs.getInt(1);
                    int theirLevel = rs.getInt(2);
                    if (!showLocked
                        && theirLevel >= UserRegistry.LOCKED
                        && just == null /* if only one user, show regardless of lock state. */) {
                        locked++;
                        continue;
                    }
                    String theirName = DBUtils.getStringUTF8(rs, 3);// rs.getString(3);
                    String theirEmail = rs.getString(4);
                    String theirOrg = rs.getString(5);
                    String theirLocales = rs.getString(6);
                    java.sql.Timestamp theirLast = rs.getTimestamp(8);
                    boolean havePermToChange = ctx.session.user.isAdminFor(reg.getInfo(theirId));

                    String theirTag = theirId + "_" + theirEmail; // ID+email -
                    // prevents
                    // stale
                    // data.
                    // (i.e.
                    // delete of
                    // user 3 if
                    // the rows
                    // change..)
                    String action = ctx.field(theirTag);
                    CookieSession theUser = CookieSession.retrieveUserWithoutTouch(theirEmail);

                    if (just != null && !just.equals(theirEmail)) {
                        continue;
                    }
                    n++;

                    shownUsers.put(reg.getInfo(theirId));

                    if ((just == null) && (!justme) && (!theirOrg.equals(oldOrg))) {
                        ctx.println("<tr class='heading' ><th class='partsection' colspan='6'><a name='" + theirOrg + "'><h4>"
                            + theirOrg + "</h4></a></th></tr>");
                        oldOrg = theirOrg;
                    }

                    ctx.println("  <tr id='u@" + theirId + "' class='user" + theirLevel + "'>");

                    if (areSendingMail && (theirLevel < UserRegistry.LOCKED)) {
                        ctx.print("<td class='framecell'>");
                        MailSender.getInstance().queue(ctx.userId(), theirId, mailSubj, mailBody);
                        ctx.println("(queued)</td>");
                    }
                    // first: DO.

                    if (havePermToChange) { // do stuff

                        String msg = null;
                        if (ctx.field(LIST_ACTION_SETLOCALES + theirTag).length() > 0) {
                            ctx.println("<td class='framecell' >");
                            String newLocales = ctx.field(LIST_ACTION_SETLOCALES + theirTag);
                            msg = reg.setLocales(ctx, theirId, theirEmail, newLocales);
                            ctx.println(msg);
                            theirLocales = newLocales; // MODIFY
                            if (theUser != null) {
                                ctx.println("<br/><i>Logging out user session " + theUser.id
                                    + " and deleting all unsaved changes</i>");
                                theUser.remove();
                            }
                            UserRegistry.User newThem = reg.getInfo(theirId);
                            if (newThem != null) {
                                theirLocales = newThem.locales; // update
                            }
                            ctx.println("</td>");
                        } else if ((action != null) && (action.length() > 0) && (!action.equals(LIST_ACTION_NONE))) { // other
                            // actions
                            ctx.println("<td class='framecell'>");

                            // check an explicit list. Don't allow random levels
                            // to be set.
                            for (int i = 0; i < UserRegistry.ALL_LEVELS.length; i++) {
                                if (action.equals(LIST_ACTION_SETLEVEL + UserRegistry.ALL_LEVELS[i])) {
                                    if ((just == null) && (UserRegistry.ALL_LEVELS[i] <= UserRegistry.TC)) {
                                        ctx.println("<b>Must be zoomed in on a user to promote them to TC</b>");
                                    } else {
                                        msg = reg.setUserLevel(ctx, theirId, theirEmail, UserRegistry.ALL_LEVELS[i]);
                                        ctx.println("Set user level to "
                                            + UserRegistry.levelToStr(ctx, UserRegistry.ALL_LEVELS[i]));
                                        ctx.println(": " + msg);
                                        theirLevel = UserRegistry.ALL_LEVELS[i];
                                        if (theUser != null) {
                                            ctx.println("<br/><i>Logging out user session " + theUser.id + "</i>");
                                            theUser.remove();
                                        }
                                    }
                                }
                            }

                            if (action.equals(LIST_ACTION_SHOW_PASSWORD)) {
                                String pass = reg.getPassword(ctx, theirId);
                                if (pass != null) {
                                    UserRegistry.printPasswordLink(ctx, theirEmail, pass);
                                    ctx.println(" <tt class='winner'>" + pass + "</tt>");
                                }
                            } else if (action.equals(LIST_ACTION_SEND_PASSWORD)) {
                                String pass = reg.getPassword(ctx, theirId);
                                if (pass != null && theirLevel < UserRegistry.LOCKED) {
                                    UserRegistry.printPasswordLink(ctx, theirEmail, pass);
                                    notifyUser(ctx, theirEmail, pass);
                                }
                            } else if (action.equals(LIST_ACTION_DELETE0)) {
                                ctx.println("Ensure that 'confirm delete' is chosen at right and click Do Action to delete..");
                            } else if ((UserRegistry.userCanDeleteUser(ctx.session.user, theirId, theirLevel))
                                && (action.equals(LIST_ACTION_DELETE1))) {
                                msg = reg.delete(ctx, theirId, theirEmail);
                                ctx.println("<strong style='font-color: red'>Deleting...</strong><br>");
                                ctx.println(msg);
                            } else if ((UserRegistry.userCanModifyUser(ctx.session.user, theirId, theirLevel))
                                && (action.equals(LIST_ACTION_SETLOCALES))) {
                                if (theirLocales == null) {
                                    theirLocales = "";
                                }
                                ctx.println("<label>Locales: (space separated) <input id='" + LIST_ACTION_SETLOCALES + theirTag + "' name='"
                                    + LIST_ACTION_SETLOCALES + theirTag
                                    + "' value='" + theirLocales + "'></label>");
                                ctx.println("<button onclick=\"{document.getElementById('" + LIST_ACTION_SETLOCALES + theirTag
                                    + "').value='*'; return false;}\" >All Locales</button>");
                            } else if (UserRegistry.userCanDeleteUser(ctx.session.user, theirId, theirLevel)) {
                                // change of other stuff.
                                UserRegistry.InfoType type = UserRegistry.InfoType.fromAction(action);

                                if (UserRegistry.userIsAdmin(ctx.session.user) && type == UserRegistry.InfoType.INFO_PASSWORD) {
                                    String what = "password";

                                    String s0 = ctx.field("string0" + what);
                                    String s1 = ctx.field("string1" + what);
                                    if (s0.equals(s1) && s0.length() > 0) {
                                        ctx.println("<h4>Change " + what + " to <tt class='codebox'>" + s0 + "</tt></h4>");
                                        action = ""; // don't popup the menu
                                        // again.

                                        msg = reg.updateInfo(ctx, theirId, theirEmail, type, s0);
                                        ctx.println("<div class='fnotebox'>" + msg + "</div>");
                                        ctx.println("<i>click Change again to see changes</i>");
                                    } else {
                                        ctx.println("<h4>Change " + what + "</h4>");
                                        if (s0.length() > 0) {
                                            ctx.println("<p class='ferrbox'>Both fields must match.</p>");
                                        }
                                        ctx.println(
                                            "<p role='alert' style='font-size: 1.5em;'><em>PASSWORDS MAY BE VISIBLE AS PLAIN TEXT. USE OF A RANDOM PASSWORD (as suggested) IS STRONGLY RECOMMENDED.</em></p>");
                                        ctx.println("<label><b>New " + what + ":</b><input type='password' name='string0" + what
                                            + "' value='" + s0 + "'></label><br>");
                                        ctx.println("<label><b>New " + what + ":</b><input type='password' name='string1" + what
                                            + "'> (confirm)</label>");

                                        ctx.println("<br><br>");
                                        ctx.println("(Suggested random password: <tt>" + UserRegistry.makePassword(theirEmail)
                                            + "</tt> )");
                                    }
                                } else if (type != null) {
                                    String what = type.toString();

                                    String s0 = ctx.field("string0" + what);
                                    String s1 = ctx.field("string1" + what);
                                    if (type == InfoType.INFO_ORG)
                                        s1 = s0; /* ignore */
                                    if (s0.equals(s1) && s0.length() > 0) {
                                        ctx.println("<h4>Change " + what + " to <tt class='codebox'>" + s0 + "</tt></h4>");
                                        action = ""; // don't popup the menu
                                        // again.

                                        msg = reg.updateInfo(ctx, theirId, theirEmail, type, s0);
                                        ctx.println("<div class='fnotebox'>" + msg + "</div>");
                                        ctx.println("<i>click Change again to see changes</i>");
                                    } else {
                                        ctx.println("<h4>Change " + what + "</h4>");
                                        if (s0.length() > 0) {
                                            ctx.println("<p class='ferrbox'>Both fields must match.</p>");
                                        }
                                        if (type == InfoType.INFO_ORG) {
                                            ctx.println("<select name='string0" + what + "'>");
                                            ctx.println("<option value='' >Choose...</option>");
                                            for (String o : UserRegistry.getOrgList()) {
                                                ctx.print("<option value='" + o + "' ");
                                                if (o.equals(theirOrg)) {
                                                    ctx.print(" selected='selected' ");
                                                }
                                                ctx.println(">" + o + "</option>");
                                            }
                                            ctx.println("</select>");
                                        } else {
                                            ctx.println("<label><b>New " + what + ":</b><input name='string0" + what
                                                + "' value='" + s0 + "'></label><br>");
                                            ctx.println("<label><b>New " + what + ":</b><input name='string1" + what
                                                + "'> (confirm)</label>");
                                        }
                                    }
                                }
                            } else if (theirId == ctx.session.user.id) {
                                ctx.println("<i>You can't change that setting on your own account.</i>");
                            } else {
                                ctx.println("<i>No changes can be made to this user.</i>");
                            }
                            // ctx.println("Change to " + action);
                        } else {
                            ctx.print("<td>");
                        }
                    } else {
                        ctx.print("<td>");
                    }

                    if (just == null) {
                        printUserZoomLink(ctx, theirEmail, "");
                    }

                    ctx.println("</td>");

                    // org, level
                    ctx.println("    <td>" + theirOrg + "<br>" + "&nbsp; <span style='font-size: 80%' align='right'>"
                        + UserRegistry.levelToStr(ctx, theirLevel).replaceAll(" ", "&nbsp;") + "</span></td>");

                    ctx.println("    <td valign='top'><font size='-1'>#" + theirId + " </font> <a name='u_" + theirEmail + "'>"
                        + theirName + "</a>");
                    ctx.println("    <a href='mailto:" + theirEmail + "'>" + theirEmail + "</a>");
                    ctx.print("</td><td>");
                    if (havePermToChange) {
                        // Was something requested?

                        { // PRINT MENU
                            ctx.print("<select name='" + theirTag + "'  ");
                            if (just != null) {
                                ctx.print(" onchange=\"this.form.submit()\" ");
                            }
                            ctx.print(">");

                            // set user to VETTER
                            ctx.println("   <option value=''>" + LIST_ACTION_NONE +

                                "</option>");
//                            if (just != null) {
                            for (int i = 0; i < UserRegistry.ALL_LEVELS.length; i++) {
                                int lev = UserRegistry.ALL_LEVELS[i];
                                if (just == null && lev != UserRegistry.LOCKED) continue; // only allow mass LOCK (for now)
                                doChangeUserOption(
                                    ctx,
                                    lev,
                                    theirLevel,
                                    false && (preset_fromint == theirLevel)
                                        && preset_do.equals(LIST_ACTION_SETLEVEL + lev));
                            }
                            ctx.println("   <option disabled>" + LIST_ACTION_NONE + "</option>");
//                            }
                            ctx.println("   <option ");
                            if ((preset_fromint == theirLevel) && preset_do.equals(LIST_ACTION_SHOW_PASSWORD)) {
                                ctx.println(" SELECTED ");
                            }
                            ctx.println(" value='" + LIST_ACTION_SHOW_PASSWORD + "'>Show password...</option>");
                            ctx.println("   <option ");
                            if ((preset_fromint == theirLevel) && preset_do.equals(LIST_ACTION_SEND_PASSWORD)) {
                                ctx.println(" SELECTED ");
                            }
                            ctx.println(" value='" + LIST_ACTION_SEND_PASSWORD + "'>Send password...</option>");

                            if (just != null) {
                                if (havePermToChange) {
                                    ctx.println("   <option ");
                                    if ((preset_fromint == theirLevel) && preset_do.equals(LIST_ACTION_SETLOCALES)) {
                                        // ctx.println(" SELECTED ");
                                    }
                                    ctx.println(" value='" + LIST_ACTION_SETLOCALES + "'>Set locales...</option>");
                                }
                                if (UserRegistry.userCanDeleteUser(ctx.session.user, theirId, theirLevel)) {
                                    ctx.println("   <option>" + LIST_ACTION_NONE + "</option>");
                                    if ((action != null) && action.equals(LIST_ACTION_DELETE0)) {
                                        ctx.println("   <option value='" + LIST_ACTION_DELETE1
                                            + "' SELECTED>Confirm delete</option>");
                                    } else {
                                        ctx.println("   <option ");
                                        if ((preset_fromint == theirLevel) && preset_do.equals(LIST_ACTION_DELETE0)) {
                                            // ctx.println(" SELECTED ");
                                        }
                                        ctx.println(" value='" + LIST_ACTION_DELETE0 + "'>Delete user..</option>");
                                    }
                                }
                                if (just != null) { // only do these in 'zoomin'
                                    // view.
                                    ctx.println("   <option disabled>" + LIST_ACTION_NONE + "</option>");

                                    InfoType current = InfoType.fromAction(action);
                                    for (InfoType info : InfoType.values()) {
                                        if (info == InfoType.INFO_ORG && !(ctx.session.user.userlevel == UserRegistry.ADMIN)) {
                                            continue;
                                        }
                                        ctx.print(" <option ");
                                        if (info == current) {
                                            ctx.print(" SELECTED ");
                                        }
                                        ctx.println(" value='" + info.toAction() + "'>Change " + info.toString() + "...</option>");
                                    }
                                }
                            }
                            ctx.println("    </select>");
                        } // end menu
                    }
                    if (ctx.session.user.isAdminFor(reg.getInfo(theirId))) {
                        ctx.println("<br><a href='" + ctx.context("upload.jsp?s=" + ctx.session.id + "&email=" + theirEmail)
                            + "'>Upload XML...</a>");
                    }
                    ctx.println("<br><a class='recentActivity' href='" + ctx.context("myvotes.jsp?user=" + theirId) + "'>User Activity</a>");
                    ctx.println("</td>");

                    if (theirLevel <= UserRegistry.MANAGER) {
                        ctx.println(" <td>" + UserRegistry.prettyPrintLocale(null) + "</td> ");
                    } else {
                        ctx.println(" <td>" + UserRegistry.prettyPrintLocale(theirLocales) + "</td>");
                    }

                    // are they logged in?
                    if ((theUser != null) && UserRegistry.userCanModifyUsers(ctx.session.user)) {
                        ctx.println("<td>");
                        ctx.println("<b>active: " + timeDiff(theUser.last) + " ago</b>");
                        if (UserRegistry.userIsAdmin(ctx.session.user)) {
                            ctx.print("<br/>");
                            printLiveUserMenu(ctx, theUser);
                        }
                        ctx.println("</td>");
                    } else if (theirLast != null) {
                        ctx.println("<td>");
                        ctx.println("<b>seen: " + timeDiff(theirLast.getTime()) + " ago</b>");
                        ctx.print("<br/><font size='-2'>");
                        ctx.print(theirLast.toString());
                        ctx.println("</font></td>");
                    }

                    ctx.println("  </tr>");
                }
                ctx.println("</tbody></table>");

                // now, serialize the list..
                ctx.println("<script>var shownUsers = " + shownUsers.toString() + ";\r\nshowUserActivity(shownUsers, 'userListTable');\r\n</script>\n");

                if (hideUserList) {
                    ctx.println("</div>");
                }
                if (!justme) {
                    ctx.println("<div style='font-size: 70%'>Number of users shown: " + n + "</div><br>");

                    if (n == 0 && just != null && !just.isEmpty()) {
                        UserRegistry.User u = reg.get(just);
                        if (u == null) {
                            ctx.println("<h3 class='ferrbox'>" + ctx.iconHtml("stop", "Not Found Error") + " User '" + just
                                + "' does not exist.</h3>");
                        } else {
                            ctx.println("<h3 class='ferrbox'>" + ctx.iconHtml("stop", "Not Found Error") + " User '" + just
                                + "' from organization " + u.org + " is not visible to you. Ask an administrator.</h3>");
                        }
                    }

                    if ((UserRegistry.userIsExactlyManager(ctx.session.user) || UserRegistry.userIsTC(ctx.session.user))
                        && locked > 0) {
                        showTogglePref(subCtx, PREF_SHOWLOCKED, "Show " + locked + " locked users:");
                    }
                }
                if (!justme && UserRegistry.userCanModifyUsers(ctx.session.user)) {
                    if ((n > 0) && UserRegistry.userCanEmailUsers(ctx.session.user)) { // send
                        // a
                        // mass
                        // email
                        // to
                        // users
                        if (ctx.field(LIST_MAILUSER).length() == 0) {
                            ctx.println("<label><input type='checkbox' value='y' name='" + LIST_MAILUSER
                                + "'>Check this box to compose a message to these " + n
                                + " users (excluding LOCKED users).</label>");
                        } else {
                            ctx.println("<p><div class='pager'>");
                            ctx.println("<h4>Mailing " + n + " users</h4>");
                            if (didConfirmMail) {
                                if (areSendingDisp) {
                                    throw new InternalError("Not implemented - see DisputePageManager");
                                } else {
                                    ctx.println("<b>Mail sent.</b><br>");
                                }
                            } else { // dont' allow resend option
                                // if (sendWhat.length() > 0) {
                                // ctx.println("<label><input type='checkbox' value='y' name='"
                                // + LIST_MAILUSER
                                // +
                                // "_d'>Check this box to send a Dispute complaint with this email.</label><br>");
                                // } else {
                                // ctx.println("<i>On the next page you will be able to choose a Dispute Report.</i><br>");
                                // }
                                ctx.println("<input type='hidden' name='" + LIST_MAILUSER + "' value='y'>");
                            }
                            ctx.println("From: <b>" + cleanEmail + "</b><br>");
                            if (sendWhat.length() > 0) {
                                ctx.println("<div class='odashbox'>"
                                    + TransliteratorUtilities.toHTML.transliterate(sendWhat).replaceAll("\n", "<br>")
                                    + "</div>");
                                if (!didConfirmMail) {
                                    ctx.println("<input type='hidden' name='" + LIST_MAILUSER_WHAT + "' value='"
                                        + sendWhat.replaceAll("&", "&amp;").replaceAll("'", "&quot;") + "'>");
                                    if (!ctx.field(LIST_MAILUSER_CONFIRM).equals(cleanEmail)
                                        && (ctx.field(LIST_MAILUSER_CONFIRM).length() > 0)) {
                                        ctx.println("<strong>" + ctx.iconHtml("stop", "email did not match")
                                            + "That email didn't match. Try again.</strong><br>");
                                    }
                                    ctx.println("To confirm sending, type the email address <tt class='codebox'>" + cleanEmail
                                        + "</tt> in this box : <input name='" + LIST_MAILUSER_CONFIRM + "'>");
                                }
                            } else {
                                ctx.println("<textarea NAME='" + LIST_MAILUSER_WHAT
                                    + "' id='body' ROWS='15' COLS='85' style='width:100%'></textarea>");
                            }
                            ctx.println("</div>");
                        }

                    }
                }
                // #level $name $email $org
                rs.close();

                // more 'My Account' stuff
                if (justme) {
                    ctx.println("<hr>");
                    // Is the 'interest locales' list relevant?
                    if (ctx.session.user.userlevel <= UserRegistry.EXPERT) {
                        boolean intlocs_change = (ctx.field("intlocs_change").length() > 0);

                        ctx.println("<h4>Notify me about these locale groups (just the language names, no underscores or dashes):</h4>");

                        if (intlocs_change) {
                            if (ctx.field("intlocs_change").equals("t")) {
                                String newIntLocs = ctx.field("intlocs");

                                String msg = reg.setLocales(ctx, ctx.session.user.id, ctx.session.user.email, newIntLocs, true);

                                if (msg != null) {
                                    ctx.println(msg + "<br>");
                                }
                                UserRegistry.User newMe = reg.getInfo(ctx.session.user.id);
                                if (newMe != null) {
                                    ctx.session.user.intlocs = newMe.intlocs; // update
                                }
                            }

                            ctx.println("<input type='hidden' name='intlocs_change' value='t'>");
                            ctx.println("<label>Locales: <input name='intlocs' ");
                            if (ctx.session.user.intlocs != null) {
                                ctx.println("value='" + ctx.session.user.intlocs.trim() + "' ");
                            }
                            ctx.println("</input></label>");
                            if (ctx.session.user.intlocs == null) {
                                ctx.println(
                                    "<br><i>List languages only, separated by spaces.  Example: <tt class='codebox'>en fr zh</tt>. leave blank for 'all locales'.</i>");
                            }
                            // ctx.println("<br>Note: changing interest locales is currently unimplemented. Check back later.<br>");
                        }

                        ctx.println("<ul><tt class='codebox'>" + UserRegistry.prettyPrintLocale(ctx.session.user.intlocs)
                            + "</tt>");
                        if (!intlocs_change) {
                            ctx.print("<a href='" + ctx.url() + ctx.urlConnector() + "do=listu&" + LIST_JUST + "="
                                + URLEncoder.encode(ctx.session.user.email) + "&intlocs_change=b' >[Change this]</a>");
                        }
                        ctx.println("</ul>");

                    } // end intlocs
                    ctx.println("<br>");
                    // ctx.println("<input type='submit' disabled value='Reset Password'>");
                    // /* just mean to show it as disabled */
                }
                if (justme || UserRegistry.userCanModifyUsers(ctx.session.user)) {
                    ctx.println("<br>");
                    ctx.println("<input type='submit' name='doBtn' value='Do Action'>");
                    ctx.println("</form>");

                    if (!justme && UserRegistry.userCanModifyUsers(ctx.session.user)) {
                        WebContext subsubCtx = new WebContext(ctx);
                        subsubCtx.addQuery("s", ctx.session.id);
                        if (org != null) {
                            subsubCtx.addQuery("org", org);
                        }
                        subsubCtx.addQuery("do", "list");
                        subsubCtx.println("<hr><form method='POST' action='" + subsubCtx.context("DataExport.jsp") + "'>");
                        subsubCtx.printUrlAsHiddenFields();
                        subsubCtx.print("<input type='submit' class='csvDownload' value='Download .csv (including LOCKED)'>");
                        subsubCtx.println("</form>");
                    }
                }
            } /* end synchronized(reg) */
        } catch (SQLException se) {
            SurveyLog.logger.log(java.util.logging.Level.WARNING,
                "Query for org " + org + " failed: " + DBUtils.unchainSqlException(se), se);
            ctx.println("<i>Failure: " + DBUtils.unchainSqlException(se) + "</i><br>");
        } finally {
            DBUtils.close(conn);
        }
        if (just != null) {
            ctx.println("<a href='" + ctx.url() + ctx.urlConnector() + "do=list'>\u22d6 Show all users</a><br>");
        }
        printFooter(ctx);
    }

    /**
     * @param ctx
     * @param userEmail
     * @param text
     *            TODO
     */
    private void printUserZoomLink(WebContext ctx, String userEmail, String text) {
        ctx.print("<a href='" + ctx.url() + ctx.urlConnector() + "do=list&" + LIST_JUST + "=" + URLEncoder.encode(userEmail) + "' >"
            + ctx.iconHtml("zoom", "More on this user..") + text + "</a>");
    }

    private void doChangeUserOption(WebContext ctx, int newLevel, int theirLevel, boolean selected) {
        if (ctx.session.user.getLevel().canCreateOrSetLevelTo(VoteResolver.Level.fromSTLevel(newLevel))) {
            ctx.println("    <option " + /* (selected?" SELECTED ":"") + */"value='" + LIST_ACTION_SETLEVEL + newLevel
                + "'>Make " + UserRegistry.levelToStr(ctx, newLevel) + "</option>");
        } else {
            ctx.println("    <option disabled " + /*
                                                  * (selected?" SELECTED ":"")
                                                  * +
                                                  */">Make " + UserRegistry.levelToStr(ctx, newLevel) + "</option>");
        }
    }

    /**
     * Show a toggleable preference
     *
     * @param ctx
     * @param pref
     *            which preference
     * @param what
     *            description of preference
     */
    public boolean showTogglePref(WebContext ctx, String pref, String what) {
        boolean val = ctx.prefBool(pref);
        WebContext nuCtx = (WebContext) ctx.clone();
        nuCtx.addQuery(pref, !val);
        // nuCtx.println("<div class='pager' style='float: right;'>");
        nuCtx.println("<a href='" + nuCtx.url() + "'>" + what + " is currently ");
        ctx.println(((val) ? "<span class='selected'>On</span>" : "<span style='color: #ddd' class='notselected'>On</span>")
            + "&nbsp;/&nbsp;"
            + ((!val) ? "<span class='selected'>Off</span>" : "<span style='color: #ddd' class='notselected'>Off</span>"));
        ctx.println("</a><br>");
        // nuCtx.println("</div>");
        return val;
    }

    String showListPref(WebContext ctx, String pref, String what, String[] list) {
        return showListPref(ctx, pref, what, list, false);
    }

    String showListPref(WebContext ctx, String pref, String what, String[] list, boolean doDef) {
        String val = ctx.pref(pref, doDef ? "default" : list[0]);
        ctx.println("<b>" + what + "</b>: ");
        // ctx.println("<select name='"+pref+"'>");
        if (doDef) {
            WebContext nuCtx = (WebContext) ctx.clone();
            nuCtx.addQuery(pref, "default");
            ctx.println("<a href='" + nuCtx.url() + "' class='" + (val.equals("default") ? "selected" : "notselected") + "'>"
                + "default" + "</a> ");
        }
        for (int n = 0; n < list.length; n++) {
            // ctx.println("    <option " +
            // (val.equals(list[n])?" SELECTED ":"") + "value='" + list[n] +
            // "'>"+list[n] +"</option>");
            WebContext nuCtx = (WebContext) ctx.clone();
            nuCtx.addQuery(pref, list[n]);
            ctx.println("<a href='" + nuCtx.url() + "' class='" + (val.equals(list[n]) ? "selected" : "notselected") + "'>"
                + list[n] + "</a> ");
        }
        // ctx.println("</select></label><br>");
        ctx.println("<br>");
        return val;
    }

    /*
     * ctx.println("(settings type: " +
     * ctx.settings().getClass().getName()+")<br/>");
     *
     * // If a pref (URL) is set, use that, otherwise if there is a setting use
     * that, otherwise false. boolean dummySetting =
     * ctx.prefBool("dummy",ctx.settings().get("dummy",false)); // always set.
     * ctx.settings().set("dummy",dummySetting);
     *
     * showTogglePref(ctx, "dummy", "A Dummy Setting");
     */
    /**
     * @deprecated JSP only
     * @param ctx
     * @param pref
     * @param list
     * @return
     */
    String getListSetting(WebContext ctx, String pref, String[] list) {
        return getListSetting(ctx, pref, list, false);
    }

    boolean getBoolSetting(WebContext ctx, String pref) {
        return ctx.prefBool(pref, ctx.settings().get(pref, false));
    }

    String getListSetting(WebContext ctx, String pref, String[] list, boolean doDef) {
        String defaultVal = doDef ? "default" : list[0];
        String settingsSet = defaultVal; // do NOT persist!>>
        // ctx.settings().get(pref, default);
        String val = ctx.pref(pref, settingsSet);
        return val;
    }

    String getListSetting(UserSettings settings, String pref, String[] list, boolean doDef) {
        return settings.get(pref, doDef ? "default" : list[0]);
    }

    static void writeMenu(WebContext jout, String title, String field, String current, String items[]) {
        writeMenu(jout, title, field, current, items, null);
    }

    static void writeMenu(WebContext jout, String title, String field, String current, String items[], String rec) {
        String which = current;
        boolean any = false;
        for (int i = 0; !any && (i < items.length); i++) {
            if (items[i].equals(which))
                any = true;
        }

        String hash = "menu_" + field;
        String theTitle = "";
        if (rec != null && !rec.isEmpty()) {
            theTitle = "(* denotes default value)";
        }

        jout.println("<label id='m_" + hash + "' class='" + (!current.equals(items[0]) ? "menutop-active" : "menutop-other")
            + "' title='" + theTitle + "' >");

        // if(!any) {
        // WebContext ssc = new WebContext(jout);
        // ssc.setQuery(field, items[0]);
        // //jout.println("<a href='"+ssc.url()+"' style='text-decoration: none;'>");
        // }
        jout.println(title);
        // if(!any) {
        // //jout.println("</a>");
        // }

        jout.println("<select class='" + (any ? "menutop-active" : "menutop-other") + "' onchange='window.location=this.value;'>"); // document.getElementById(\"m_"+hash+"\").innerHTML+=\"<i>Changing
        // to
        // \"
        // +
        // this.text
        // +
        // \"...</i>\";
        if (!any) {
            jout.println("<option selected value=\"\">Change...</option>");
        }
        for (int i = 0; i < items.length; i++) {
            boolean isOptional = (items[i].equals(Level.OPTIONAL.toString()));

            if (isOptional && !SurveyMain.isUnofficial())
                continue;
            WebContext ssc = new WebContext(jout);
            ssc.setQuery(field, items[i]);
            String sty = "";
            if (rec != null && rec.equals(items[i])) {
                // jout.print("<optgroup label=\"Recommended\">");
                sty = "font-weight: bold;";
            }

            jout.print("<option style='" + sty + "' ");
            if (items[i].equals(which)) {
                jout.print(" selected ");
            } else {
                jout.print("value=\"" + ssc.url() + "\" ");
            }
            jout.print(">" + items[i]);
            if (rec != null && rec.equals(items[i])) {
                jout.print("*");
            }
            if (isOptional) {
                jout.println(" [only available in SmokeTest]");
            }
            jout.println("</option>");

            // if(rec!=null&&rec.equals(items[i])) {
            // jout.print("</optgroup>");
            // }

        }
        jout.println("</select>");

        jout.println("<span id='info_" + hash + "'/>");

        jout.println("</label>");
    }

    String showListSetting(WebContext ctx, String pref, String what, String[] list) {
        return showListSetting(ctx, pref, what, list, false);
    }

    String showListSetting(WebContext ctx, String pref, String what, String[] list, boolean doDef) {
        return showListSetting(ctx, pref, what, list, doDef, null);
    }

    String showListSetting(WebContext ctx, String pref, String what, String[] list, String rec) {
        return showListSetting(ctx, pref, what, list, false, rec);
    }

    String showListSetting(WebContext ctx, String pref, String what, String[] list, boolean doDef, String rec) {
        String val = getListSetting(ctx, pref, list, doDef);
        ctx.settings().set(pref, val);

        boolean no_js = ctx.prefBool(SurveyMain.PREF_NOJAVASCRIPT);

        if (no_js) {
            ctx.println("<b>" + what + "</b>: ");
            if (doDef) {
                WebContext nuCtx = (WebContext) ctx.clone();
                nuCtx.addQuery(pref, "default");
                ctx.println("<a href='" + nuCtx.url() + "' class='" + (val.equals("default") ? "selected" : "notselected") + "'>"
                    + "default" + "</a> ");
            }
            for (int n = 0; n < list.length; n++) {
                WebContext nuCtx = (WebContext) ctx.clone();
                nuCtx.addQuery(pref, list[n]);
                if (rec != null && rec.equals(list[n])) {
                    ctx.print("<b>");
                }
                ctx.println("<a href='" + nuCtx.url() + "' class='" + (val.equals(list[n]) ? "selected" : "notselected") + "'>"
                    + list[n] + "</a> ");
                if (rec != null && rec.equals(list[n])) {
                    ctx.print("*</b>");
                }
            }
            ctx.println("<br>");
        } else {
            writeMenu(ctx, what, pref, val, list, rec);
        }

        return val;
    }

    void doOptions(WebContext ctx) {
        WebContext subCtx = new WebContext(ctx);
        subCtx.removeQuery(QUERY_DO);
        printHeader(ctx, "Manage");
        printUserTableWithHelp(ctx, "/MyOptions");

        ctx.println("<a href='" + ctx.url() + "'>Locales</a><hr>");
        printRecentLocales(subCtx, ctx);
        ctx.addQuery(QUERY_DO, "options");
        ctx.println("<h2>Manage</h2>");

        ctx.includeFragment("manage.jsp");

        /*        ctx.print("<hr>");
        
                if (UserRegistry.userIsTC(ctx.session.user)) {
                    showTogglePref(ctx, PREF_DELETEZOOMOUT, "Show delete controls when not zoomed in:");
                    showTogglePref(ctx, PREF_SHOWUNVOTE, "Show controls for removing votes:");
                }
        
                ctx.println("<h4>Advanced Options</h4>");
                ctx.print("<blockquote>");
                boolean adv = showToggleSetting(ctx, PREF_ADV, "Show Advanced Options");
                ctx.println("</blockquote>");
        
                if (adv == true) {
                    ctx.println("<div class='ferrbox'><i>Do not enable these items unless instructed.</i><br>");
                    showTogglePref(ctx, PREF_NOPOPUPS, "Reduce popup windows");
                    showTogglePref(ctx, PREF_XPID, "show XPATH ids");
                    showTogglePref(ctx, PREF_GROTTY, "show obtuse items");
                    showTogglePref(ctx, PREF_XPATHS, "Show full XPaths");
                    showToggleSetting(ctx, PREF_SHCOVERAGE, "Show Coverage Levels");
                    showTogglePref(ctx, PREF_NOSHOWDELETE, "Suppress controls for deleting unused items in zoomed-in view:");
                    showTogglePref(ctx, PREF_NOJAVASCRIPT, "Reduce the use of JavaScript (unimplemented)");
        
                    ctx.println("</div>");
                }
         */
        //
        // // Dummy, pointless boolean toggle.
        // if(isUnofficial) {
        // ctx.println("(settings type: " +
        // ctx.settings().getClass().getName()+")<br/>");
        // showToggleSetting(ctx, "dummy","A Dummy Setting");
        // }

        printFooter(ctx);
    }

    public boolean showToggleSetting(WebContext ctx, String field, String title) {
        // If a pref (URL) is set, use that, otherwise if there is a setting use
        // that, otherwise false.
        boolean someSetting = getBoolSetting(ctx, field);
        // always set.
        ctx.settings().set(field, someSetting);

        showTogglePref(ctx, field, title);

        return someSetting;
    }

    /**
     * Print the opening form for a 'shopping cart' (one or more showPathList
     * operations)
     *
     * @see showPathList
     * @see printPathListClose
     */
    public void printPathListOpen(WebContext ctx) {
        // ctx.println("<form name='"+STFORM+"' method=POST action='" +
        // ctx.base() + "'>");
    }

    /**
     * Print the closing form for a 'shopping cart' (one or more showPathList
     * operations)
     *
     * @see showPathList
     * @see printPathListOpen
     */
    public void printPathListClose(WebContext ctx) {
        // ctx.println("</form>");
    }

    /**
     * Do session.
     * 
     * @param ctx
     * @throws IOException
     * @throws SurveyException
     * 
     * Called only by doGet. Called when user logs in or logs out, also when choose Settings from gear menu.
     */
    public void doSession(WebContext ctx) throws IOException, SurveyException {
        // which
        String which = ctx.field(QUERY_SECTION); // may be empty string ""

        setLocale(ctx);

        String sessionMessage = ctx.setSession();

        if (ctx.session == null) {

            printHeader(ctx, "Survey Tool");
            if (sessionMessage == null) {
                sessionMessage = "Could not create your user session.";
            }
            ctx.println("<p><img src='stop.png' width='16'>" + sessionMessage + "</p>");
            ctx.println("<hr><a href='" + ctx.context("login.jsp") + "' class='notselected'>Login as another user...</a>");
            printFooter(ctx);
            return;
        } else {
            ctx.session.userDidAction(); // always true for this
        }

        if (lockOut != null) {
            if (ctx.field("unlock").equals(lockOut)) {
                ctx.session.put("unlock", lockOut);
            } else {
                String unlock = (String) ctx.session.get("unlock");
                if ((unlock == null) || (!unlock.equals(lockOut))) {
                    printHeader(ctx, "Locked for Maintenance");
                    ctx.print("<hr><div class='ferrbox'>Sorry, the Survey Tool has been locked for maintenance work. Please try back later.</div>");
                    printFooter(ctx);
                    return;
                }
            }
        }

        // setup thread name
        if (ctx.session.user != null) {
            Thread.currentThread().setName(
                Thread.currentThread().getName() + " " + ctx.session.user.id + ":" + ctx.session.user.toString());

        }
        // only do forum for logged in user
        if (ctx.hasField(SurveyForum.F_FORUM) || ctx.hasField(SurveyForum.F_XPATH)) {
            fora.doForum(ctx, sessionMessage); // TODO: does this ever happen? If so, when??
            return;
        }

        // locale REDIRECTS ------------------------------
        // looking for a stringid?
        String strid = ctx.field("strid");
        String whyBad = "(unknown problem)";
        if (!strid.isEmpty() && ctx.hasField("_")) {
            try {
                final String xpath = xpt.getByStringID(strid);
                if (xpath != null) {
                    // got one.
                    PathHeader ph = getSTFactory().getPathHeader(xpath);
                    if (ph == null) {
                        whyBad = "NULL from PathHeader";
                    } else if (ph.getSurveyToolStatus() == SurveyToolStatus.HIDE
                        || ph.getSurveyToolStatus() == SurveyToolStatus.DEPRECATED) {
                        whyBad = "This item's PathHeader status is: " + ph.getSurveyToolStatus().name();
                    } else {
                        ctx.response.sendRedirect(ctx.vurl(CLDRLocale.getInstance(ctx.field("_")), ph.getPageId(), strid, null));
                        return; // exit
                        // }
                    }
                } else {
                    whyBad = "not a valid StringID";
                }
                SurveyLog.logException(null, "Bad StringID" + strid + " " + whyBad, ctx);
            } catch (Throwable t) {
                SurveyLog.logException(t, "Exception processing StringID " + strid + " - " + whyBad, ctx);
            }
        }

        // END REDIRECTS -------------------------

        // TODO: untangle this
        // admin things
        if ((ctx.field(QUERY_DO).length() > 0)) {
            String doWhat = ctx.field(QUERY_DO);

            // could be user or non-user items
            if (doWhat.equals("options")) {
                doOptions(ctx);
                return;
            } else if (doWhat.equals("disputed")) {
                DisputePageManager.doDisputed(ctx);
                return;
            } else if (doWhat.equals("logout")) {
                ctx.logout();
                try {
                    ctx.response.sendRedirect(ctx.jspLink("?logout=1"));
                    ctx.out.close();
                    ctx.close();
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe.toString() + " while redirecting to logout");
                }
                return;
            }

            // these items are only for users.
            if (ctx.session.user != null) {
                if ((doWhat.equals("list") || doWhat.equals("listu")) && (UserRegistry.userCanDoList(ctx.session.user))) {
                    doList(ctx);
                    return;
                } else if (doWhat.equals("coverage") && (UserRegistry.userCanDoList(ctx.session.user))) {
                    doCoverage(ctx);
                    return;
                } else if (doWhat.equals("new") && (UserRegistry.userCanCreateUsers(ctx.session.user))) {
                    doNew(ctx);
                    return;
                }
            }
            // Option wasn't found
            sessionMessage = ("<i id='sessionMessage'>Could not do the action '" + doWhat + "'. You may need to be logged in first.</i>");
        }

        String title = " ";
        PageId pageId = ctx.getPageId();
        if (pageId != null) {
            title = ""; // pageId.getSectionId().toString() + " | " +
            // pageId.toString();
        } else if (ctx.hasField(QUERY_EXAMPLE)) {
            title = title + " Example";
        } else if (which == null || which.isEmpty()) {
            if (ctx.getLocale() == null) {
                ctx.redirect(ctx.context(VURL_LOCALES));
                ctx.redirectToVurl(ctx.context(VURL_LOCALES)); // may blink.
                return;
            } else {
                title = ""; // general";
            }
        } else if (which.equals(R_STEPS)) {
            title = " Basic Locale Information";
        }
        printHeader(ctx, title);
        if (sessionMessage != null) {
            ctx.println(sessionMessage);
        }

        WebContext baseContext = (WebContext) ctx.clone();

        // Don't spin up a factory here.

        // print 'shopping cart'
        if (!shortHeader(ctx)) {

            if ((which.length() == 0) && (ctx.getLocale() != null) || (pageId == null && !which.startsWith(REPORT_PREFIX)) /*
                                                                                                                           * unrecognized
                                                                                                                           * page
                                                                                                                           * id
                                                                                                                           */) {
                which = xMAIN;
            }
            // if(false&&(which.length()>0)) {
            // printUserTableWithHelp(ctx, "/"+which.replaceAll(" ","_")); //
            // Page Help
            // } else {
            printUserTable(ctx);
            // }
            printRecentLocales(baseContext, ctx);
        }

        if ((ctx.getLocale() != null) && (!shortHeader(ctx))) { // don't show
            // these
            // warnings for
            // example/easy
            // steps pages.
            // if (dcChild != null && (!which.equals(xMAIN))) {
            // String dcChildDisplay = ctx.getLocaleDisplayName(dcChild);
            // ctx.println("<div class='fnotebox'>" +
            // DEFAULT_CONTENT_LINK +
            // " for <b>"+
            // dcChildDisplay + "</b></div>");
            // }
            CLDRLocale aliasTarget = isLocaleAliased(ctx.getLocale());
            if (aliasTarget != null) {
                // the alias might be a default content locale. Save some clicks
                // here.
                CLDRLocale dcParent = getSupplementalDataInfo().getBaseFromDefaultContent(aliasTarget);
                if (dcParent == null) {
                    dcParent = aliasTarget;
                }
                ctx.println("<div class='ferrbox'>This locale is aliased to <b>" + getLocaleLink(ctx, aliasTarget, null)
                    + "</b>. You cannot modify it. Please make all changes in <b>" + getLocaleLink(ctx, dcParent, null)
                    + "</b>.<br>");
                ctx.printHelpLink("/AliasedLocale", "Help with Aliased Locale");
                ctx.print("</div>");

                ctx.println("<div class='ferrbox'><h1>"
                    + ctx.iconHtml("stop", null)
                    + "We apologise for the inconvenience, but there is currently an error with how these aliased locales are resolved.  Kindly ignore this locale for the time being. You must make all changes in <b>"
                    + getLocaleLink(ctx, dcParent, null) + "</b>.</h1>");
                ctx.print("</div>");

            }
        }
        doLocale(ctx, baseContext, which, whyBad);
    }

    private void printRecentLocales(WebContext baseContext, WebContext ctx) {
        Hashtable<String, Hashtable<String, Object>> lh = ctx.session.getLocales();
        Enumeration<String> e = lh.keys();
        if (e.hasMoreElements()) {
            boolean shownHeader = false;
            for (; e.hasMoreElements();) {
                String k = e.nextElement().toString();
                if ((ctx.getLocale() != null) && (ctx.getLocale().toString().equals(k))) {
                    continue;
                }
                if (!shownHeader) {
                    ctx.println("<p align='right'><B>Recent locales: </B> ");
                    shownHeader = true;
                }
                ctx.print(getLocaleLink(ctx, k, null));
            }
            if (shownHeader) {
                ctx.println("</p>");
            }
        }
    }

    private static boolean shortHeader(WebContext ctx) {
        // TODO Auto-generated method stub
        return ctx.hasField(QUERY_EXAMPLE) || R_STEPS.equals(ctx.field(SurveyMain.QUERY_SECTION));
    }

    LocaleTree localeTree = null;

    // protected Map<String,CLDRLocale> getLocaleListMap()
    // {
    // return getLocaleTree().getMap();
    // }

    public CLDRLocale getLocaleCode(String localeName) {
        return getLocaleTree().getLocaleCode(localeName);
    }

    public synchronized LocaleTree getLocaleTree() {
        if (localeTree == null) {
            CLDRFormatter defaultFormatter = setDefaultCLDRLocaleFormatter();
            LocaleTree newLocaleTree = new LocaleTree(defaultFormatter);
            File inFiles[] = getInFiles();
            if (inFiles == null) {
                busted("Can't load CLDR data files from " + fileBase);
                throw new RuntimeException("Can't load CLDR data files from " + fileBase);
            }
            int nrInFiles = inFiles.length;

            for (int i = 0; i < nrInFiles; i++) {
                String localeName = inFiles[i].getName();
                int dot = localeName.indexOf('.');
                if (dot != -1) {
                    localeName = localeName.substring(0, dot);
                    CLDRLocale loc = CLDRLocale.getInstance(localeName);

                    // but, is it just an alias?
                    CLDRLocale aliasTo = isLocaleAliased(loc);
                    if (aliasTo == null) {
                        newLocaleTree.add(loc);
                    }
                }
            }
            localeTree = newLocaleTree;
        }
        return localeTree;
    }

    /**
     * @return
     */
    public CLDRFormatter setDefaultCLDRLocaleFormatter() {
        CLDRFormatter defaultFormatter = new CLDRLocale.CLDRFormatter(getEnglishFile(), FormatBehavior.replace);
        CLDRLocale.setDefaultFormatter(defaultFormatter);
        return defaultFormatter;
    }

    /**
     * Get all related locales, given a 'top' (highestNonrootParent) locale.   Example:  ar ->  ar, ar_EG ...     skips readonly locales.
     * @see CLDRLocale#getHighestNonrootParent()
     * @param topLocale
     * @return the resulting set, unmodifiable
     */
    public synchronized Collection<CLDRLocale> getRelatedLocs(CLDRLocale topLocale) {
        Set<CLDRLocale> cachedSet = relatedLocales.get(topLocale);
        if (cachedSet == null) {
            final LocaleTree lt = getLocaleTree();
            final Set<CLDRLocale> set = new HashSet<CLDRLocale>();
            set.add(topLocale); // add the top locale itself
            for (CLDRLocale atopLocale : lt.getTopCLDRLocales()) { // add each of the top locales that has the same "highest nonroot parent"
                if (atopLocale.getHighestNonrootParent() == topLocale) {
                    final Collection<CLDRLocale> topLocales = lt.getSubLocales(atopLocale).values();
                    if (topLocales != null) {
                        set.addAll(topLocales);
                    }
                }
            }
            cachedSet = Collections.unmodifiableSet(set);
            relatedLocales.put(topLocale, cachedSet);
        }
        return cachedSet;
    }

    private Map<CLDRLocale, Set<CLDRLocale>> relatedLocales = new HashMap<CLDRLocale, Set<CLDRLocale>>();

    public static String getLocaleDisplayName(CLDRLocale locale) {
        return locale.getDisplayName();
    }

    public static String decoratedLocaleName(CLDRLocale localeName, String str, String explanation) {
        String rv = "";
        // int s = vet.status(localeName);
        // if(s==-1) {
        // if(explanation.length()>0) {
        // rv = rv + ("<span title='"+explanation+"'>");
        // }
        // rv = rv + (str);
        // if(explanation.length()>0) {
        // rv = rv + ("</span>");
        // }
        // return rv;
        // }
        // if((s&Vetting.RES_DISPUTED)>0) {
        // rv = rv +
        // ("<span style='margin: 1px; padding: 1px;' class='disputed'>");
        // if(explanation.length() >0) {
        // explanation = explanation + ", ";
        // }
        // explanation = explanation + "disputed";
        // } else {
        // if ((s&(Vetting.RES_INSUFFICIENT|Vetting.RES_NO_VOTES))>0) {
        // rv = rv +
        // ("<span style='margin: 1px; padding: 1px;' class='insufficient'>");
        // if(explanation.length() >0) {
        // explanation = explanation + ", ";
        // }
        // explanation = explanation + "insufficient votes";
        // }
        // }
        if (explanation.length() > 0) {
            rv = rv + ("<span title='" + explanation + "'>");
        }
        rv = rv + (str);
        if (explanation.length() > 0) {
            rv = rv + ("</span>");
        }
        // if((s&Vetting.RES_DISPUTED)>0) {
        // rv = rv + ("</span>");
        // } else if ((s&(Vetting.RES_INSUFFICIENT|Vetting.RES_NO_VOTES))>0) {
        // rv = rv + ("</span>");
        // }
        return rv;
    }

    String getLocaleLink(WebContext ctx, String locale) {
        return getLocaleLink(ctx, CLDRLocale.getInstance(locale));
    }

    String getLocaleLink(WebContext ctx, CLDRLocale locale) {
        return getLocaleLink(ctx, locale, null);
    }

    String getLocaleLink(WebContext ctx, String locale, String n) {
        return getLocaleLink(ctx, CLDRLocale.getInstance(locale), n);
    }

    public String getLocaleLink(WebContext ctx, CLDRLocale locale, String n) {
        if (n == null) {
            n = locale.getDisplayName();
        }
        boolean isDefaultContent = getSupplementalDataInfo().isDefaultContent(locale);
        // boolean hasDraft = draftSet.contains(localeName);
        // ctx.print(hasDraft?"<b>":"") ;
        String title = locale.toString();
        String classstr = "";
        String localeUrl = ctx.urlForLocale(locale);
        if (isDefaultContent) {
            classstr = "class='dcLocale'";
            localeUrl = null; // ctx.urlForLocale(defaultContentToParent(locale));
            title = "Default Content: Please view and/or propose changes in "
                + getSupplementalDataInfo().getBaseFromDefaultContent(locale).getDisplayName() + ".";
        }
        String rv = ("<a " + classstr + " title='" + title + "' " + (localeUrl != null ? ("href=\"" + localeUrl + "\"") : "") + " >");
        rv = rv + decoratedLocaleName(locale, n, title);
        boolean canModify = !isDefaultContent && UserRegistry.userCanModifyLocale(ctx.session.user, locale);
        if (canModify) {
            rv = rv + (modifyThing(ctx));
            int odisp = 0;
            if ((SurveyMain.phase() == Phase.VETTING || SurveyMain.phase() == Phase.SUBMIT || isPhaseVettingClosed())
                && ((odisp = DisputePageManager.getOrgDisputeCount(ctx)) > 0)) {
                rv = rv + ctx.iconHtml("disp", "(" + odisp + " org disputes)");
            }
        }
        if (!isDefaultContent && getReadOnlyLocales().contains(locale)) {
            String comment = SpecialLocales.getComment(locale);
            if (comment == null) {
                comment = "This locale is read-only due to SurveyTool configuration.";
            }
            rv = rv + ctx.iconHtml("lock", comment);
        }
        rv = rv + ("</a>");
        // ctx.print(hasDraft?"</b>":"") ;

        return rv;
    }

    void printLocaleLink(WebContext ctx, CLDRLocale localeName, String n) {
        ctx.print(getLocaleLink(ctx, localeName, n));
    }

    /*
     * show a list of locales that fall into this interest group.
     */
    void printListFromInterestGroup(WebContext ctx, String intgroup) {
        LocaleTree lm = getLocaleTree();
        if (lm == null) {
            throw new RuntimeException("Can't load CLDR data files");
        }
        ctx.println("<table summary='Locale List' border=1 class='list'>");
        int n = 0;
        for (String ln : lm.getTopLocales()) {
            CLDRLocale aLocale = lm.getLocaleCode(ln);

            if (aLocale == null)
                throw new InternalError("printListFromInterestGroup: can't find intgroup for locale " + ln);
            //ULocale uLocale = aLocale.toULocale();
            if (!intgroup.equals(aLocale.getLanguage())) {
                continue;
            }

            n++;
            ctx.print("<tr class='row" + (n % 2) + "'>");
            ctx.print(" <td valign='top'>");
            printLocaleLink(ctx, (aLocale), ln.toString());
            ctx.println(" </td>");

            Map<String, CLDRLocale> sm = lm.getSubLocales(aLocale);

            ctx.println("<td valign='top'>");
            int j = 0;
            for (Iterator<String> si = sm.keySet().iterator(); si.hasNext();) {
                if (j > 0) {
                    ctx.println(", ");
                }
                String sn = si.next();
                CLDRLocale subLocale = sm.get(sn);
                // if(subLocale.length()>0) {
                printLocaleLink(ctx, (subLocale), sn);
                // }
                j++;
            }
            ctx.println("</td");
            ctx.println("</tr>");
        }
        ctx.println("</table> ");
    }

    void doLocaleList(WebContext ctx, WebContext baseContext) {
        boolean showCodes = ctx.prefBool(PREF_SHOWCODES);

        ctx.println(fora.mainFeedIcon(ctx) + "<br>");
        ctx.println("<h1>Locales</h1>");
        {
            WebContext nuCtx = (WebContext) ctx.clone();
            nuCtx.addQuery(PREF_SHOWCODES, !showCodes);
            nuCtx.println("<div style='text-align: right; '>");
            nuCtx.println("<input type='checkbox' " + (showCodes ? "CHECKED" : "") + " onclick=\"window.location='" + nuCtx.url()
                + "';\" />");
            nuCtx.println("<a href='" + nuCtx.url() + "'>" + "Show locale codes</a>");
            nuCtx.println("</div>");
        }

        LocaleTree lm = getLocaleTree();
        // if(lm == null) {
        // busted("Can't load CLDR data files from " + fileBase);
        // throw new RuntimeException("Can't load CLDR data files from " +
        // fileBase);
        // }

        ctx.println("<table summary='Locale List' border=1 class='list'>");
        int n = 0;
        for (String ln : lm.getTopLocales()) {
            n++;
            CLDRLocale aLocale = lm.getLocaleCode(ln);
            ctx.print("<tr class='row" + (n % 2) + "'>");
            ctx.print(" <td valign='top'>");
            printLocaleLink(baseContext, (aLocale), ln);
            if (showCodes) {
                ctx.println("<br><tt>" + aLocale + "</tt>");
            }
            ctx.println(" </td>");

            Map<String, CLDRLocale> sm = lm.getSubLocales(aLocale);

            ctx.println("<td valign='top'>");
            int j = 0;
            if (sm != null) {
                for (String sn : sm.keySet()) {
                    if (j > 0) {
                        ctx.println(", ");
                    }
                    CLDRLocale subLocale = sm.get(sn);
                    // if(subLocale.length()>0) {
                    printLocaleLink(baseContext, (subLocale), sn);
                    if (showCodes) {
                        ctx.println("&nbsp;-&nbsp;<tt>" + subLocale + "</tt>");
                    }
                    // }
                    j++;
                }
            } else {
                ctx.println("<i>no sublocales</i>");
            }
            ctx.println("</td>");
            ctx.println("</tr>");
        }
        ctx.println("</table> ");
        // ctx.println("(Locales containing draft items are shown in <b><a class='draftl' href='#'>bold</a></b>)<br/>");
        // TODO: reenable this
    }

    void doLocale(WebContext ctx, WebContext baseContext, String which, String whyBad) {
        String locale = null;
        if (ctx.getLocale() != null) {
            locale = ctx.getLocale().toString();
        }
        if (!shortHeader(ctx)) {
            printPathListOpen(ctx);
        }
        if ((locale == null) || (locale.length() <= 0)) {
            ctx.println("<i>Loading locale list...</i>");
            ctx.flush();
            ctx.redirectToVurl(ctx.context(VURL_LOCALES)); // may blink.
            return;
            //doLocaleList(ctx, baseContext);
            //ctx.println("<br/>");
        } else {
            showLocale(ctx, which, whyBad);
        }
        if (!shortHeader(ctx)) {
            printPathListClose(ctx);
        }
        printFooter(ctx);
    }

    /**
     * Print out a menu item
     *
     * @param ctx
     *            the context
     * @param which
     *            the ID of "this" item
     * @param menu
     *            the ID of the current item
     */
    public static void printMenu(WebContext ctx, String which, String menu) {
        printMenu(ctx, which, menu, menu, QUERY_SECTION);
    }

    /**
     * Print out a menu item
     *
     * @param ctx
     *            the context
     * @param which
     *            the ID of "this" item
     * @param menu
     *            the ID of the current item
     * @param anchor
     *            the #anchor to link to
     */
    public static void printMenuWithAnchor(WebContext ctx, String which, String menu, String anchor) {
        printMenu(ctx, which, menu, menu, QUERY_SECTION, anchor);
    }

    /**
     * Print out a menu item
     *
     * @param ctx
     *            the context
     * @param which
     *            the ID of "this" item
     * @param menu
     *            the ID of the current item
     * @param title
     *            the Title of this menu
     */
    public static void printMenu(WebContext ctx, String which, String menu, String title) {
        printMenu(ctx, which, menu, title, QUERY_SECTION);
    }

    /**
     * Print out a menu item
     *
     * @param ctx
     *            the context
     * @param which
     *            the ID of "this" item
     * @param menu
     *            the ID of the current item
     * @param title
     *            the Title of this menu
     * @param key
     *            the URL field to use (such as 'x')
     */
    public static void printMenu(WebContext ctx, String which, String menu, String title, String key) {
        printMenu(ctx, which, menu, title, key, null);
    }

    /**
     * Print out a menu item
     *
     * @param ctx
     *            the context
     * @param which
     *            the ID of "this" item
     * @param menu
     *            the ID of the current item
     * @param title
     *            the Title of this menu
     * @param key
     *            the URL field to use (such as 'x')
     * @param anchor
     *            the #anchor to link to
     */
    public static void printMenu(WebContext ctx, String which, String menu, String title, String key, String anchor) {
        ctx.print(getMenu(ctx, which, menu, title, key, anchor));
    }

    /**
     * Print out a menu as a form
     *
     * @param ctx
     * @param which
     * @param menu
     * @param title
     * @param key
     */
    private static void printMenuButton(WebContext ctx, String which, String menu, String title, String key, String buttonName) {
        ctx.print("<form action='" + ctx.url() + "#" + key + "=" + menu + "' method='POST'>");
        ctx.printUrlAsHiddenFields();
        ctx.println("\n<input type='hidden' name='" + key + "' value='" + menu + "' />");
        ctx.println("<label class='" + (menu.equals(which) ? "selected" : "notselected") + "'>" + buttonName);
        ctx.println("<input type='submit' value='" + title + "' />");
        ctx.println("</label>");
        ctx.println("</form>");
    }

    public static String getMenu(WebContext ctx, String which, String menu, String title) {
        return getMenu(ctx, which, menu, title, QUERY_SECTION);
    }

    public static String getMenu(WebContext ctx, String which, String menu, String title, String key) {
        return getMenu(ctx, which, menu, title, key, null);
    }

    public static String getMenu(WebContext ctx, String which, String menu, String title, String key, String anchor) {
        StringBuffer buf = new StringBuffer();
        if (menu.equals(which)) {
            buf.append("<b class='selected'>");
        } else {
            buf.append("<a class='notselected' href=\"" + ctx.url() + ctx.urlConnector() + key + "=" + menu
                + ((anchor != null) ? ("#" + anchor) : "") + "\">");
        }
        if (menu.endsWith("/")) {
            buf.append(title + "<font size=-1>(other)</font>");
        } else {
            buf.append(title);
        }
        if (menu.equals(which)) {
            buf.append("</b>");
        } else {
            buf.append("</a>");
        }
        return buf.toString();
    }

    void notifyUser(WebContext ctx, String theirEmail, String pass) {
        UserRegistry.User u = reg.get(theirEmail);
        String whySent;
        String subject = "CLDR Registration for " + theirEmail;
        Integer fromId;
        if (ctx != null) {
            fromId = ctx.userId();
            whySent = "You are being notified of the CLDR vetting account for you.\n";
        } else {
            fromId = null;
            whySent = "Your CLDR vetting account information is being sent to you\r\n\r\n";
        }
        String body = whySent + "To access it, visit: \n<"
            + defaultBase + "?" + QUERY_PASSWORD + "=" + pass + "&"
            + QUERY_EMAIL + "=" + theirEmail
            + ">\n"
            +
            // // DO NOT ESCAPE THIS AMPERSAND.
            "\n" + "Or you can visit\n   <" + defaultBase + ">\n    username: " + theirEmail
            + "\n    password: " + pass + "\n" + "\n" + " Please keep this link to yourself. Thanks.\n"
            + " Follow the 'Instructions' link on the main page for more help.\n" +
            "As a reminder, please do not re-use this password on other web sites.\n\n";
        MailSender.getInstance().queue(fromId, u.id, subject, body);
    }

    /**
     * Convert from the parent to a child type. i.e. 'languages' -> 'language'
     */
    public static final String typeToSubtype(String type) {
        String subtype = type;
        if (type.equals(LDMLConstants.LANGUAGES)) {
            subtype = LDMLConstants.LANGUAGE;
        } else if (type.equals(LDMLConstants.SCRIPTS)) {
            subtype = LDMLConstants.SCRIPT;
        } else if (type.equals(PathUtilities.MEASNAMES)) {
            subtype = MEASNAME;
        } else if (type.equals(PathUtilities.CODEPATTERNS)) {
            subtype = CODEPATTERN;
        } else if (type.equals(LDMLConstants.TERRITORIES)) {
            subtype = LDMLConstants.TERRITORY;
        } else if (type.equals(LDMLConstants.VARIANTS)) {
            subtype = LDMLConstants.VARIANT;
        } else if (type.equals(LDMLConstants.KEYS)) {
            subtype = LDMLConstants.KEY;
        } else if (type.equals(LDMLConstants.TYPES)) {
            subtype = LDMLConstants.TYPE;
        } /*
          * else if(subtype.endsWith("s")) { subtype =
          * subtype.substring(0,subtype.length()-1); }
          */
        return subtype;
    }

    public static final String CHECKCLDR = "CheckCLDR_"; // key for CheckCLDR
    // objects by locale
    public static final String CHECKCLDR_RES = "CheckCLDR_RES_"; // key for

    // CheckCLDR
    // objects by
    // locale

    void printLocaleTreeMenu(WebContext ctx, String which) {

        WebContext subCtx = (WebContext) ctx.clone();
        subCtx.addQuery(QUERY_LOCALE, ctx.getLocale().toString());

        ctx.println("<div id='sectionmenu'>");

        boolean canModify = UserRegistry.userCanModifyLocale(subCtx.session.user, subCtx.getLocale());
        subCtx.put("which", which);
        subCtx.put(WebContext.CAN_MODIFY, canModify);
        subCtx.includeFragment("menu_top.jsp"); // ' code lists .. ' etc

        // not in the jsp- advanced idems.
        if (ctx.prefBool(PREF_ADV)) {
            subCtx.println("<p class='hang'>Advanced Items: ");
            // printMenu(subCtx, which, TEST_MENU_ITEM);
            printMenu(subCtx, which, RAW_MENU_ITEM);
            subCtx.println("</p>");
        }

        subCtx.println("</div>");
    }

    /**
     * show the actual locale data..
     *
     * @param ctx
     *            context
     * @param which
     *            value of 'x' parameter.
     */
    public void showLocale(WebContext ctx, String which, String whyBad) {
        PageId pageId = ctx.getPageId();
        if (HAVE_REMOVE && which.equals(xREMOVE)) {
            ctx.println("<b><a href=\"" + ctx.url() + "\">" + "List of Locales" + "</a></b><br/>");
            ctx.session.getLocales().remove(ctx.field(QUERY_LOCALE));
            ctx.println("<h2>Your session for " + ctx.field(QUERY_LOCALE) + " has been removed.</h2>");
            doMain(ctx);
            return;
        }
        synchronized (ctx.session) {
            // Set up checks

            if (ctx.hasField(QUERY_EXAMPLE)) {
                ctx.println("<h3>" + ctx.getLocale() + " " + ctx.getLocale().getDisplayName() + " / " + which + " Example</h3>");
            } else if (which.equals(R_STEPS)) {
                // short menu.
                ctx.includeFragment(STEPSMENU_TOP_JSP);
            } else {
                // does not need check
                printLocaleTreeMenu(ctx, which);
            }

            // check for errors
            ctx.includeFragment("possibleProblems.jsp");

            // Find which pod they want, and show it.
            // NB keep these sections in sync with DataPod.xpathToPodBase()
            WebContext subCtx = (WebContext) ctx.clone();
            subCtx.addQuery(QUERY_LOCALE, ctx.getLocale().toString());
            subCtx.addQuery(QUERY_SECTION, which);
            // looking for a stringid? Should have redirected by now.
            if (ctx.hasField("strid")) {
                String xpath = "(unknown StringID)";
                String strid = ctx.field("strid");
                try {
                    xpath = xpt.getByStringID(strid);
                    if (xpath == null) {
                        xpath = "(not a valid StringID)";
                    }
                } catch (Throwable t) {
                    // SurveyLog.logException(t, ctx);
                }
                ctx.println("<div class='ferrbox'> " + ctx.iconHtml("stop", "bad xpath")
                    + " Sorry, the string ID in your URL can't be shown: <span class='loser' title='" + xpath + " " + whyBad + "'>" + strid
                    + "</span><br>The XPath involved is: <tt>" + xpath + "</tt><br> and the reason is: " + whyBad + ".</div>");
                which = xMAIN;
                return;
            }

            if (which.startsWith(REPORT_PREFIX)) {
                if (!subCtx.doReport(which)) {
                    doMain(ctx);
                }
            } else if (pageId != null && !which.equals(xMAIN)) {
                showPathList(subCtx, which, pageId);
            } else if (RAW_MENU_ITEM.equals(which)) {
                getOutputFileManager().doRaw(subCtx);
            } else {
                which = xMAIN;
                doMain(subCtx);
            }
        }
    }

    /**
     * @param ctx
     * @param uf
     * @return
     */
    private CheckCLDR getCheck(WebContext ctx, UserLocaleStuff uf) {
        return (CheckCLDR) uf.getCheck(ctx.getEffectiveCoverageLevel(ctx.getLocale().toString()),
            ctx.getOptionsMap());
    }

    /**
     * @param localeName
     * @return
     */
    CLDRLocale fileNameToLocale(String localeName) {
        String theLocale;
        int dot = localeName.indexOf('.');
        theLocale = localeName.substring(0, dot);
        return CLDRLocale.getInstance(theLocale);
    }

    /**
     * Show the 'main info about this locale' (General) panel.
     */
    public void doMain(WebContext ctx) {

        ctx.includeFragment("generalinfo.jsp");

    }

    public static String getAttributeValue(Document doc, String xpath, String attribute) {
        if (doc != null) {
            Node n = LDMLUtilities.getNode(doc, xpath);
            if (n != null) {
                return LDMLUtilities.getAttributeValue(n, attribute);
            }
        }
        return null;
    }

    /**
     * private SoftReference nodeHashReference = null; int nodeHashPuts = 0;
     * private final Hashtable getNodeHash() { Hashtable nodeHash = null;
     * if((nodeHashReference == null) ||
     * ((nodeHash=(Hashtable)nodeHashReference.get())==null)) { return null; }
     * return nodeHash; }
     **/

    public static final String USER_FILE = "UserFile";
    public static final String USER_FILE_KEY = "UserFileKey";
    public static final String CLDRDBSRC = "_source";

    private static CLDRFile gBaselineFile = null;
    private static ExampleGenerator gBaselineExample = null;

    private Factory gFactory = null;

    /**
     * Return the factory that corresponds to trunk
     *
     * @return
     */
    public synchronized Factory getDiskFactory() {
        if (gFactory == null) {
            final File list[] = getFileBases();
            CLDRConfig config = CLDRConfig.getInstance();
            // may fail at server startup time- should do this through setup mode
            ensureOrCheckout(null, "CLDR_DIR", config.getCldrBaseDirectory(), CLDR_DIR_REPOS);
            // verify readable
            File root = new File(config.getCldrBaseDirectory(), "common/main");
            if (!root.isDirectory()) {
                throw new InternalError("Not a dir:  " + root.getAbsolutePath() + " - check the value of " + "CLDR_DIR"
                    + " in cldr.properties.");
            }

            gFactory = SimpleFactory.make(list, ".*");
        }
        return gFactory;
    }

    public void ensureOrCheckout(JspWriter o, final String param, final File dir, final String url) {
        if (dir == null) {
            busted("Configuration Error: " + param + " is not set.");
        } else if (!dir.isDirectory()) {

            if (o == null) {
                busted("Not able to checkout " + dir.getAbsolutePath() + " for " + param + " - go into setup mode.");
                return; /* NOTREACHED */
            }
            try {
                ElapsedTimer et = new ElapsedTimer();

                if (true) {
                    throw new InternalError("Please run this manually:  'svn checkout " + url + " " + dir.getAbsolutePath()
                        + "' - and restart the server. TODO- this will be fixed by the step-by-step install.");
                }

                o.println(param + " directory " + dir.getAbsolutePath() + " did not exist - checking out " + url
                    + " - if this is not desired, modify " + param + " in cldr.properties. THIS MAY TAKE A WHILE!");

                long res;
                res = getOutputFileManager().svnCheckout(dir, url, SVNRevision.UNDEFINED, SVNRevision.HEAD,
                    SVNDepth.INFINITY, true);
//                Vector<File> toUpdate = new Vector<File>();
//                File commonFile = new File(dir, "common");
//                File commonMainFile = new File(commonFile, "main");
//                /*
//                String addLocales[] = { "root.xml", "en.xml", "en_US.xml" };
//                for (String s : addLocales) {
//                    toUpdate.add(new File(commonMainFile, s));
//                }
//                */
//                toUpdate.add(commonMainFile);
//                String addDirs[] = { "dtd", "supplemental" };
//                for (String s : addDirs) {
//                    toUpdate.add(new File(commonFile, s));
//                }
//                File seedFile = new File(dir, "seed");
//                File seedMainFile = new File(seedFile, "main");
//                /*
//                String addSeedLocales[] = { BASELINE_ID + ".xml", "und.xml", "und_ZZ.xml" };
//                for (String s : addSeedLocales) {
//                    toUpdate.add(new File(seedMainFile, s));
//                }
//                */
//                toUpdate.add(seedMainFile);
//                File fileArray[] = toUpdate.toArray(new File[0]);
//                long res2[] = getOutputFileManager().svnUpdate(fileArray, SVNRevision.create(res), SVNDepth.INFINITY,
//                        true, true);
                o.println("Checked out " + url + " r " + res + " to " + dir.getAbsolutePath() + " - see the value of "
                    + param + " if you want to have a different location.  Took: " + et);
//                for(int i=0;i<res2.length;i++) {
//                    o.println(res2[i]+ " " + fileArray[i]);
//                }
            } catch (SVNException e) {
                final String msg = "Checking out " + url + " into " + dir.getAbsolutePath();
                SurveyLog.logException(e, msg);
                busted(msg, e);
                throw new InternalError(msg);
            } catch (IOException e) {
                final String msg = "Checking out " + url + " into " + dir.getAbsolutePath();
                SurveyLog.logException(e, msg);
                busted(msg, e);
                throw new InternalError(msg);
            }

            if (!dir.isDirectory()) {
                String msg = ("Could not read disk data  " + dir.getAbsolutePath() + ":  check " + param + "  cldr.properties  parameters. ");
                // svn export http://unicode.org/repos/cldr/tags/release-1-8 1.8

                busted(msg);
                throw new InternalError(msg);
            }
        }
    }

    private STFactory gSTFactory = null;

    /**
     * Get the factory corresponding to the current snapshot.
     *
     * @return
     */
    public final synchronized STFactory getSTFactory() {
        if (gSTFactory == null) {
            gSTFactory = new STFactory(this);
        }
        return gSTFactory;
    }

    /**
     * destroy the ST Factory - testing use only!
     *
     * @internal
     */
    public final synchronized STFactory TESTING_removeSTFactory() {
        STFactory oldFactory = gSTFactory;
        gSTFactory = null;
        return oldFactory;
    }

    private Factory gOldFactory = null;
    private Set<CLDRLocale> gOldAvailable;

    /**
     * Return the actual XML file on disk
     *
     * @param loc
     * @return
     */
    public File getBaseFile(CLDRLocale loc) {
        File base = getDiskFactory().getSourceDirectoryForLocale(loc.getBaseName());
        if (base == null)
            return null;
        return new File(base, loc.getBaseName() + ".xml");
    }

    /**
     * Get an "old" (previous CLDR version) locale file.
     * May return null if the locale wasn't available in that version.
     * @param locale
     * @param resolved
     * @return
     */
    public CLDRFile getOldFile(String locale, boolean resolved) {
        return getOldFile(CLDRLocale.getInstance(locale), resolved);
    }

    /**
     * Get an "old" (previous CLDR version) locale file.
     * May return null if the locale wasn't available in that version.
     * @param locale
     * @param resolved
     * @return
     */
    public CLDRFile getOldFile(CLDRLocale locale, boolean resolved) {
        Factory f = getOldFactory();
        if (gOldAvailable.contains(locale)) {
            return f.make(locale.getBaseName(), resolved);
        }
        return null; // not available
    }

    /**
     * Get the factory corresponding to the old release version.
     *
     * @return
     */
    public synchronized Factory getOldFactory() {
        final String repository = CLDRConfig.getInstance().getProperty("CLDR_REPOS", CLDR_DIR_REPOS);
        if (gOldFactory == null) {
            File oldBase = new File(getFileBaseOld());
            File oldCommon = new File(oldBase, "common/main");
            String verAsMilestone = "release-" + oldVersion.replaceAll("\\.", "-");
            if (!oldCommon.isDirectory()) {
                final String url = repository + "/tags/" + verAsMilestone + "/common";
                try {
                    getOutputFileManager().svnExport(oldCommon.getParentFile(), url);
                } catch (SVNException e) {
                    final String msg = "Exporting" + url + " into " + oldCommon.getAbsolutePath();
                    SurveyLog.logException(e, msg);
                    busted(msg, e);
                    throw new InternalError(msg);
                }
                if (!oldCommon.isDirectory()) {
                    String msg = ("Could not read old data - " + oldCommon.getAbsolutePath() + ": you might do 'svn export " + repository + "/tags/"
                        + verAsMilestone + "/common "
                        + oldBase.getAbsolutePath() + "/common' - or check " + getOldVersionParam() + " and CLDR_OLDVERSION parameters. ");
                    // svn export http://unicode.org/repos/cldr/tags/release-1-8
                    // 1.8

                    busted(msg);
                    throw new InternalError(msg);
                }
            }
            File oldSeed = new File(oldBase, "seed/main");
            if (!oldSeed.isDirectory()) {
                final String url = repository + "/tags/" + verAsMilestone + "/seed";
                try {
                    getOutputFileManager().svnExport(oldSeed.getParentFile(), url);
                } catch (SVNException e) {
                    final String msg = "Exporting" + url + " into " + oldSeed.getAbsolutePath();
                    SurveyLog.logException(e, msg);
                    busted(msg, e);
                    throw new InternalError(msg);
                }

                if (!oldSeed.isDirectory()) {
                    String msg = ("Could not read old seed data - " + oldSeed.getAbsolutePath()
                        + ": you might do 'svn export " + repository + "/tags/" + verAsMilestone + "/seed "
                        + oldBase.getAbsolutePath() + "/seed' - or check " + getOldVersionParam() + " and CLDR_OLDVERSION parameters. ");
                    // svn export http://unicode.org/repos/cldr/tags/release-1-8
                    // 1.8

                    busted(msg);
                    throw new InternalError(msg);
                }
            }
            File oldCommonA = new File(oldBase, "common/main");
            File oldSeedA = new File(oldBase, "seed/main");
            oldCommonA.mkdirs(); // may not exist
            oldSeedA.mkdirs(); // may not exist

            File roots[] = { oldCommon, oldSeed, oldCommonA, oldSeedA };
            gOldFactory = SimpleFactory.make(roots, ".*");
            gOldAvailable = Collections.unmodifiableSet(gOldFactory.getAvailableCLDRLocales());
        }
        return gOldFactory;
    }

    /**
     * This is the BASELINE FILE. (en_ZZ) - thus it contains 'translation hints'.
     * @see {@link #BASELINE_ID}
     * @see {@link #getEnglishFile()}
     * @return
     */
    public synchronized CLDRFile getBaselineFile(/* CLDRDBSource ourSrc */) {
        if (gBaselineFile == null) {
            try {
                CLDRFile file = getDiskFactory().make(BASELINE_LOCALE.toString(), true);
                file.setSupplementalDirectory(getSupplementalDirectory()); // so the
                // icuServiceBuilder
                // doesn't
                // blow up.
                file.freeze(); // so it can be shared.
                gBaselineFile = file;

                // propagate it.
                CheckCLDR.setDisplayInformation(gBaselineFile);
                CLDRFormatter defaultFormatter = setDefaultCLDRLocaleFormatter();
            } catch (Throwable t) {
                busted("Could not load baseline locale " + BASELINE_LOCALE, t);
            }
        }
        return gBaselineFile;
    }

    HashMap<String, String> gBaselineHash = new HashMap<String, String>();

    Set<UserLocaleStuff> allUserLocaleStuffs = new HashSet<UserLocaleStuff>();

    /* Sentinel value indicating that there was no baseline string available. */

    public static final String DATAROW_JSP = "datarow_jsp"; // context tag for
    // which datarow jsp
    // to use

    public static final String DATAROW_JSP_DEFAULT = "datarow_short.jsp";

    public static final String QUERY_VALUE_SUFFIX = "_v";

    public synchronized ExampleGenerator getBaselineExample() {
        if (gBaselineExample == null) {
            CLDRFile baselineFile = getBaselineFile();
            gBaselineExample = new ExampleGenerator(baselineFile, baselineFile, fileBase + "/../supplemental/");
        }
        gBaselineExample.setVerboseErrors(twidBool("ExampleGenerator.setVerboseErrors"));
        return gBaselineExample;
    }

    public synchronized WebContext.HTMLDirection getHTMLDirectionFor(CLDRLocale locale) {
        String dir = getDirectionalityFor(locale);
        return HTMLDirection.fromCldr(dir);
    }

    public synchronized String getDirectionalityFor(CLDRLocale id) {
        final boolean DDEBUG = false;
        if (DDEBUG)
            SurveyLog.logger.warning("Checking directionality for " + id);
        if (aliasMap == null) {
            checkAllLocales();
        }
        while (id != null) {
            // TODO use iterator
            CLDRLocale aliasTo = isLocaleAliased(id);
            if (DDEBUG)
                SurveyLog.logger.warning("Alias -> " + aliasTo);
            if (aliasTo != null && !aliasTo.equals(id)) { // prevent loops
                id = aliasTo;
                if (DDEBUG)
                    SurveyLog.logger.warning(" -> " + id);
                continue;
            }
            String dir = directionMap.get(id);
            if (DDEBUG)
                SurveyLog.logger.warning(" dir:" + dir);
            if (dir != null) {
                return dir;
            }
            id = id.getParent();
            if (DDEBUG)
                SurveyLog.logger.warning(" .. -> :" + id);
        }
        if (DDEBUG)
            SurveyLog.logger.warning("err: could not get directionality of root");
        return "left-to-right"; // fallback
    }

    /**
     * Returns the current basic options map.
     *
     * @return the map
     * @see org.unicode.cldr.test.CheckCoverage#check(String, String, String,
     *      Map, List)
     */
    public static final org.unicode.cldr.test.CheckCLDR.Phase getTestPhase() {
        return phase().getCPhase();
    }

    public CheckCLDR createCheck() {
        CheckCLDR checkCldr;

        // logger.info("Initting tests . . . - "+ctx.getLocale()+"|" + (
        // CHECKCLDR+":"+ctx.defaultPtype()) + "@"+ctx.session.id);
        // long t0 = System.currentTimeMillis();

        // make sure CLDR has the latest display information.
        // if(phaseVetting) {
        // checkCldr =
        // CheckCLDR.getCheckAll("(?!.*(DisplayCollisions|CheckCoverage).*).*"
        // /* ".*" */);
        // } else {
        checkCldr = CheckCLDR.getCheckAll(getSTFactory(), "(?!.*(CheckCoverage).*).*");
        // checkCldr = CheckCLDR.getCheckAll("(?!.*DisplayCollisions.*).*" /*
        // ".*" */);
        // }

        CheckCLDR.setDisplayInformation(getBaselineFile());

        return checkCldr;
    }

    public CheckCLDR createCheckWithoutCollisions() {
        CheckCLDR checkCldr;

        // logger.info("Initting tests . . . - "+ctx.getLocale()+"|" + (
        // CHECKCLDR+":"+ctx.defaultPtype()) + "@"+ctx.session.id);
        // long t0 = System.currentTimeMillis();

        // make sure CLDR has the latest display information.
        // if(phaseVetting) {
        // checkCldr =
        // CheckCLDR.getCheckAll("(?!.*(DisplayCollisions|CheckCoverage).*).*"
        // /* ".*" */);
        // } else {
        if (false) { // show ALL ?
            checkCldr = CheckCLDR.getCheckAll(getSTFactory(), ".*");
        } else {
            checkCldr = CheckCLDR.getCheckAll(getSTFactory(), "(?!.*(DisplayCollisions|CheckCoverage).*).*" /* ".*" */);
        }

        CheckCLDR.setDisplayInformation(getBaselineFile());

        return checkCldr;
    }

    public static boolean CACHE_VXML_FOR_TESTS = false;

    // public static boolean CACHE_VXML_FOR_EXAMPLES = false;

    /**
     * Any user of this should be within session sync.
     *
     * @author srl
     *
     */
    public class UserLocaleStuff {
        public CLDRFile cldrfile = null;
        private CLDRFile cachedCldrFile = null; /*
                                                * If not null: use this for
                                                * tests. Readonly.
                                                */
        public XMLSource dbSource = null;
        public XMLSource resolvedSource = null;
        public Hashtable<String, Object> hash = new Hashtable<String, Object>();
        private ExampleGenerator exampleGenerator = null;
        private int use;
        CLDRFile resolvedFile = null;
        CLDRFile baselineFile;

        public void open() {
            use++;
            if (SurveyLog.isDebug())
                SurveyLog.logger.warning("uls: open=" + use);
        }

        public ExampleGenerator getExampleGenerator() {
            if (exampleGenerator == null) {
                // if(CACHE_VXML_FOR_EXAMPLES) {
                // fileForGenerator = getCLDRFileCache().getCLDRFile(locale,
                // true);
                // } else {
                // SurveyLog.logger.warning("!CACHE_VXML_FOR_EXAMPLES");
                // }

                if (resolvedFile == null) {
                    SurveyLog.logger.warning("Err: fileForGenerator is null for " + dbSource);
                }

                if (resolvedFile.getSupplementalDirectory() == null) {
                    throw new InternalError("?!!! resolvedFile has no supplemental dir.");
                }
                if (baselineFile.getSupplementalDirectory() == null) {
                    throw new InternalError("?!!! baselineFile has no supplemental dir.");
                }

                exampleGenerator = new ExampleGenerator(resolvedFile, baselineFile, fileBase + "/../supplemental/");
                exampleGenerator.setVerboseErrors(twidBool("ExampleGenerator.setVerboseErrors"));
            }
            return exampleGenerator;
        }

        private String closeStack = null;

        //private CLDRLocale locale;

        public void close() {
            final boolean DEBUG = CldrUtility.getProperty("TEST", false);
            if (use <= 0) {
                throw new InternalError("Already closed! use=" + use + ", closeStack:" + closeStack);
            }
            use--;
            closeStack = DEBUG ? StackTracker.currentStack() : null;
            if (SurveyLog.isDebug())
                SurveyLog.logger.warning("uls: close=" + use);
            if (use > 0) {
                return;
            }
            internalClose();
            // TODO Auto-generated method stub
            synchronized (allUserLocaleStuffs) {
                allUserLocaleStuffs.remove(this);
            }
        }

        public void internalClose() {
            this.dbSource = null;
        }

        public boolean isClosed() {
            return this.dbSource == null;
        }

        public UserLocaleStuff(CLDRLocale locale) {
            //this.locale = locale;
            // SurveyLog.logger.warning("Adding ULS:"+locale);
            synchronized (allUserLocaleStuffs) {
                allUserLocaleStuffs.add(this);
            }

            // TODO: refactor.
            if (cldrfile == null) {
                resolvedSource = getSTFactory().makeSource(locale.getBaseName(), true);
                dbSource = resolvedSource.getUnresolving();
                cldrfile = getSTFactory().make(locale, true).setSupplementalDirectory(getSupplementalDirectory());
                // cachedCldrFile = makeCachedCLDRFile(dbSource);
                resolvedFile = cldrfile;
                // XMLSource baseSource =
                // makeDBSource(CLDRLocale.getInstance(BASELINE_LOCALE), false,
                // true);
                baselineFile = getBaselineFile();
            }
        }

        public void clear() {
            hash.clear();
            // TODO: try just kicking these instead of clearing?
            cldrfile = null;
            dbSource = null;
            hash.clear();
        }

        public CheckCLDR getCheck(String ptype, CheckCLDR.Options options) {
            CheckCLDR checkCldr = (CheckCLDR) hash.get(CHECKCLDR + ptype);
            if (checkCldr == null) {
                List<CheckStatus> checkCldrResult = new ArrayList<CheckStatus>();

                checkCldr = createCheck();

                // long t0 = System.currentTimeMillis();
                checkCldr.setCldrFileToCheck(cldrfile, options, checkCldrResult);
                // logger.info("fileToCheck set . . . on "+
                // checkCldr.toString());
                hash.put(CHECKCLDR + ptype, checkCldr);
                if (!checkCldrResult.isEmpty()) {
                    hash.put(CHECKCLDR_RES + ptype, checkCldrResult);
                }
                // long t2 = System.currentTimeMillis();
                // logger.info("Time to init tests: " + (t2-t0));
            }
            return checkCldr;
        }
    };

    /**
     * Any user of this should be within session sync
     *
     * @param ctx
     * @return
     */
    UserLocaleStuff getOldUserFile(CookieSession session, CLDRLocale locale) {
        UserLocaleStuff uf = (UserLocaleStuff) session.getByLocale(USER_FILE_KEY, locale.toString());
        return uf;
    }

    /**
     * Return the UserLocaleStuff for the current context. Any user of this
     * should be within session sync (ctx.session) and must be balanced with
     * calls to close();
     *
     * @param ctx
     * @param user
     * @param locale
     * @see UserLocaleStuff#close()
     * @see WebContext#getUserFile()
     */
    public UserLocaleStuff getUserFile(CookieSession session, CLDRLocale locale) {
        UserLocaleStuff uf = null;
        uf = new UserLocaleStuff(locale); // always open a new
        uf.open(); // incr count.

        return uf;
    }

    XMLSource makeDBSource(CLDRLocale locale) {
        return makeDBSource(locale, true, false);
    }

    XMLSource makeDBSource(CLDRLocale locale, boolean finalData) {
        return makeDBSource(locale);
    }

    XMLSource makeDBSource(CLDRLocale locale, boolean finalData, boolean resolved) {
        return getSTFactory().makeSource(locale.getBaseName(), resolved);
    }

    /**
     * reset the "list of locales". call this if you're resetting the in-db view
     * of what's on disk. it will reset things like the locale list map, the
     * metazone map, the interest map, ...
     */
    private synchronized void resetLocaleCaches() {
        localeTree = null;
        localeListSet = null;
        aliasMap = null;
        gBaselineFile = null;
        gBaselineHash = null;
        try {
            Connection conn = null;
            try {
                conn = dbUtils.getDBConnection();
                this.fora.reloadLocales(conn);
            } finally {
                DBUtils.close(conn);
            }
        } catch (SQLException se) {
            SurveyLog.logger.warning("On resetLocaleCaches().reloadLocales: " + DBUtils.unchainSqlException(se));
            SurveyMain.busted("trying to reset locale caches @ fora", se);
        }
    }

    private static Hashtable<CLDRLocale, CLDRLocale> aliasMap = null;
    private static Hashtable<CLDRLocale, String> directionMap = null;

    /**
     * "Hash" a file to a string, including mod time and size
     *
     * @param f
     * @return
     */
    private static String fileHash(File f) {
        return ("[" + f.getAbsolutePath() + "|" + f.length() + "|" + f.hashCode() + "|" + f.lastModified() + "]");
    }

    private synchronized void checkAllLocales() {
        if (aliasMap != null)
            return;

        boolean useCache = isUnofficial(); // NB: do NOT use the cache if we are
        // in official mode. Parsing here
        // doesn't take very long (about
        // 16s), but
        // we want to save some time during development iterations.
        // In production, we want the files to be more carefully checked every time.

        Hashtable<CLDRLocale, CLDRLocale> aliasMapNew = new Hashtable<CLDRLocale, CLDRLocale>();
        Hashtable<CLDRLocale, String> directionMapNew = new Hashtable<CLDRLocale, String>();
        Set<CLDRLocale> locales = getLocalesSet();
        ElapsedTimer et = new ElapsedTimer();
        CLDRProgressTask progress = openProgress("Parse locales from XML", locales.size());
        try {
            File vetdir = getVetdir();
            File xmlCache = new File(vetdir, XML_CACHE_PROPERTIES);
            File xmlCacheBack = new File(vetdir, XML_CACHE_PROPERTIES + ".backup");
            Properties xmlCacheProps = new java.util.Properties();
            Properties xmlCachePropsNew = new java.util.Properties();
            if (useCache && xmlCache.exists())
                try {
                java.io.FileInputStream is = new java.io.FileInputStream(xmlCache);
                xmlCacheProps.load(is);
                is.close();
                } catch (java.io.IOException ioe) {
                /* throw new UnavailableException */
                SurveyLog.logger.log(java.util.logging.Level.SEVERE, "Couldn't load XML Cache file from '" + "(home)" + "/"
                    + XML_CACHE_PROPERTIES + ": ", ioe);
                busted("Couldn't load XML Cache file from '" + "(home)" + "/" + XML_CACHE_PROPERTIES + ": ", ioe);
                return;
                }

            int n = 0;
            int cachehit = 0;
            SurveyLog.logger.warning("Parse " + locales.size() + " locales from XML to look for aliases or errors...");

            Set<CLDRLocale> failedSuppTest = new TreeSet<CLDRLocale>();

            // Initialize CoverageInfo outside the loop.
            CoverageInfo covInfo = CLDRConfig.getInstance().getCoverageInfo();
            for (File f : getInFiles()) {
                CLDRLocale loc = fileNameToLocale(f.getName());

                try {
                    covInfo.getCoverageValue("//ldml", loc.getBaseName());
                } catch (Throwable t) {
                    SurveyLog.logException(t, "checking SDI for " + loc);
                    failedSuppTest.add(loc);
                }
                String locString = loc.toString();
                // ULocale uloc = new ULocale(locString);
                progress.update(n++, loc.toString() /*
                                                    * + " - " +
                                                    * uloc.getDisplayName(uloc)
                                                    */);
                try {
                    // String fileName = fileBase+"/"+loc.toString()+".xml";
                    String fileHash = fileHash(f);
                    String aliasTo = null;
                    String direction = null;
                    // SurveyLog.logger.warning(fileHash);

                    String oldHash = xmlCacheProps.getProperty(locString);
                    if (useCache && oldHash != null && oldHash.equals(fileHash)) {
                        // cache hit! load from cache
                        aliasTo = xmlCacheProps.getProperty(locString + ".a", null);
                        direction = xmlCacheProps.getProperty(locString + ".d", null);
                        cachehit++;
                    } else {
                        Document d = LDMLUtilities.parse(f.getAbsolutePath(), false);

                        // look for directionality
                        Node directionalityItem = LDMLUtilities.getNode(d, "//ldml/layout/orientation/characterOrder");
                        if (directionalityItem != null) {
                            direction = LDMLUtilities.getNodeValue(directionalityItem);
                            if (direction != null && direction.length() > 0) {
                            } else {
                                direction = null;
                            }
                        }

                        Node[] aliasItems = LDMLUtilities.getNodeListAsArray(d, "//ldml/alias");

                        if ((aliasItems == null) || (aliasItems.length == 0)) {
                            aliasTo = null;
                        } else if (aliasItems.length > 1) {
                            throw new InternalError("found " + aliasItems.length + " items at " + "//ldml/alias"
                                + " - should have only found 1");
                        } else {
                            aliasTo = LDMLUtilities.getAttributeValue(aliasItems[0], "source");
                        }
                    }

                    // now, set it into the new map
                    xmlCachePropsNew.put(locString, fileHash);
                    if (direction != null) {
                        directionMapNew.put((loc), direction);
                        xmlCachePropsNew.put(locString + ".d", direction);
                    }
                    if (aliasTo != null) {
                        aliasMapNew.put((loc), CLDRLocale.getInstance(aliasTo));
                        xmlCachePropsNew.put(locString + ".a", aliasTo);
                    }
                } catch (Throwable t) {
                    SurveyLog.logger.warning("isLocaleAliased: Failed load/validate on: " + loc + " - " + t.toString());
                    t.printStackTrace();
                    busted("isLocaleAliased: Failed load/validate on: " + loc + " - ", t);
                    throw new InternalError("isLocaleAliased: Failed load/validate on: " + loc + " - " + t.toString());
                }
            }

            if (useCache)
                try {
                // delete old stuff
                if (xmlCacheBack.exists()) {
                xmlCacheBack.delete();
                }
                if (xmlCache.exists()) {
                xmlCache.renameTo(xmlCacheBack);
                }
                java.io.FileOutputStream os = new java.io.FileOutputStream(xmlCache);
                xmlCachePropsNew.store(os, "YOU MAY DELETE THIS CACHE. Cache updated at " + new Date());
                progress.update(n++, "Loading configuration..");
                os.close();
                } catch (java.io.IOException ioe) {
                /* throw new UnavailableException */
                SurveyLog.logger.log(java.util.logging.Level.SEVERE, "Couldn't write " + xmlCache + " file from '" + cldrHome
                    + "': ", ioe);
                busted("Couldn't write " + xmlCache + " file from '" + cldrHome + "': ", ioe);
                return;
                }

            if (!failedSuppTest.isEmpty()) {
                busted("Supplemental Data Test failed on startup for: " + ListFormatter.getInstance().format(failedSuppTest));
            }

            SurveyLog.logger.warning("Finished verify+alias check of " + locales.size() + ", " + aliasMapNew.size()
                + " aliased locales (" + cachehit + " in cache) found in " + et.toString());
            aliasMap = aliasMapNew;
            directionMap = directionMapNew;
        } finally {
            progress.close();
        }
    }

    /**
     * Is this locale fully aliased? If true, returns what it is aliased to.
     */
    public synchronized CLDRLocale isLocaleAliased(CLDRLocale id) {
        if (aliasMap == null) {
            checkAllLocales();
        }
        return aliasMap.get(id);
    }

    // public ULocale isLocaleAliased(ULocale id) {
    // String aliasTo = isLocaleAliased(id.getBaseName()).toString();
    // if(aliasTo!=null) {
    // return new ULocale(aliasTo);
    // } else {
    // return null;
    // }
    // }

    /**
     * Maintain a master list of metazones, culled from root.
     */
    public final Set<String> getMetazones() {
        return getSupplementalDataInfo().getAllMetazones();
    }

    public Set<String> getMetazones(String subclass) {
        Set<String> subSet = new TreeSet<String>();
        SupplementalDataInfo supplementalDataInfo = getSupplementalDataInfo();
        for (String zone : supplementalDataInfo.getAllMetazones()) {
            if (subclass.equals(supplementalDataInfo.getMetazoneToContinentMap().get(zone))) {
                subSet.add(zone);
            }
        }
        return subSet;
    }

    public String getMetazoneContinent(String xpath) {
        XPathParts parts = new XPathParts(null, null);
        SupplementalDataInfo mySupp = getSupplementalDataInfo();
        parts.set(xpath);
        String thisMetazone = parts.getAttributeValue(3, "type");
        return mySupp.getMetazoneToContinentMap().get(thisMetazone);
    }

    /**
     * show the webpage for one of the 'locale codes' items..
     *
     * @param ctx
     *            the web context
     * @param which
     *            menu item to use
     */
    public void showLocaleCodeList(WebContext ctx, String which) {
        showPathList(ctx, PathUtilities.LOCALEDISPLAYNAMES + which, typeToSubtype(which), true /* simple */);
    }

    /**
     * This is the bottleneck function for all "main" display pages.
     * @param ctx session (contains locale and coverage level, etc)
     * @param xpath xpath to use
     * @param typeToSubtype (ignored)
     * @param b (ignored)
     */
    private void showPathList(WebContext ctx, String xpath, String typeToSubtype, boolean b) {
        String vurl = ctx.vurl(ctx.getLocale(), ctx.getPageId(), null, null);
        // redirect to /v#...
        ctx.redirectToVurl(vurl);
    }

    private void showPathList(WebContext ctx, String xpath, PageId pageId) {
        // use the pageid as the xpath
        showPathList(ctx, pageId.name(), null, false);
    }

    public void showPathListExample(WebContext ctx, String xpath, String lastElement, String e, String fullThing, CLDRFile cf) {
        DataSection oldSection = ctx.getExistingSection(fullThing);
        DataSection.ExampleEntry ee = null;
        if (oldSection != null) {
            ee = oldSection.getExampleEntry(e); // retrieve the info out of the
            // hash..
        }
        if (ee != null)
            synchronized (ctx.session) {
                ctx.println("<form method=POST action='" + ctx.base() + "'>");
                ctx.printUrlAsHiddenFields();
                String cls = shortClassName(ee.status.getCause());
                ctx.printHelpLink("/" + cls + "-example", "Help with this " + cls + " example", true);
                ctx.println("<hr>");
                ctx.addQuery(QUERY_EXAMPLE, e);
                ctx.println("<input type='hidden' name='" + QUERY_EXAMPLE + "' value='" + e + "'>");

                // keep the Demo with the user. for now.
                CheckCLDR.SimpleDemo d = (CheckCLDR.SimpleDemo) ctx.getByLocale(e);
                if (d == null) {
                    d = ee.status.getDemo();
                    ctx.putByLocale(e, d);
                }

                Map<?, ?> mapOfArrays = ctx.getParameterMap();
                Map<String, String> m = new TreeMap<String, String>();
                for (Iterator i = mapOfArrays.keySet().iterator(); i.hasNext();) {
                    String k = i.next().toString();
                    // String[] v = (String[])mapOfArrays.get(k); // We dont
                    // care about the broken, undecoded contents here..
                    m.put(k, ctx.field(k, null)); // .. use our vastly improved
                    // field() function
                }

                if (d != null) {

                    try {
                        String path = ee.dataRow.getXpath();
                        String fullPath = cf.getFullXPath(path);
                        String value = ee.item.getValue();
                        String html = d.getHTML(m);
                        ctx.println(html);
                    } catch (Exception ex) {
                        ctx.println("<br><b>Error: </b> " + ex.toString() + "<br>");
                    }
                }
                ctx.println("</form>");
            }
        else {
            ctx.println(
                "<P><P><P><blockquote><i>That example seems to have expired. Perhaps the underlying data has changed? Try reloading the parent page, and clicking the Example link again.</i></blockquote>");
        }
    }

    private SupplementalDataInfo supplementalDataInfo = null;

    public synchronized final SupplementalDataInfo getSupplementalDataInfo() {
        if (supplementalDataInfo == null) {
            supplementalDataInfo = SupplementalDataInfo.getInstance(getSupplementalDirectory());
            supplementalDataInfo.setAsDefaultInstance();
        }
        return supplementalDataInfo;
    }

    public File getSupplementalDirectory() {
        return getDiskFactory().getSupplementalDirectory();
    }

    public void showMetazones(WebContext ctx, String continent) {
        showPathList(ctx, "//ldml/dates/timeZoneNames/metazone" + DataSection.CONTINENT_DIVIDER + continent, (String) null);
    }

    /**
     * parse the metazone list for this zone. Map will contain enries with key
     * String and value String[3] of the form: from : { from, to, metazone } for
     * example: "1970-01-01" : { "1970-01-01", "1985-03-08", "Australia_Central"
     * } the 'to' will be null if it does not have an ending time.
     *
     * @param metaMap
     *            an 'out' parameter which will be cleared, and populated with
     *            the contents of the metazone
     * @return the active metazone ( where to=null ) if any, or null
     */
    public String zoneToMetaZone(WebContext ctx, String zone, Map<String, String[]> metaMap) {
        //SurveyMain sm = this;
        // String returnZone = null;
        String current = null;
        XPathParts parts = new XPathParts(null, null);
        synchronized (ctx.session) { // TODO: redundant sync?
            SurveyMain.UserLocaleStuff uf = ctx.getUserFile();
            // CLDRFile cf = uf.cldrfile;
            CLDRFile resolvedFile = uf.resolvedFile;
            // CLDRFile engFile = ctx.sm.getBaselineFile();

            String xpath = "//ldml/" + "dates/timeZoneNames/zone";
            String ourSuffix = "[@type=\"" + zone + "\"]";
            // String base_xpath = xpath+ourSuffix;
            String podBase = DataSection.xpathToSectionBase(xpath);

            metaMap.clear();

            Iterator<String> mzit = resolvedFile.iterator(podBase + ourSuffix + "/usesMetazone");

            for (; mzit.hasNext();) {
                String ameta = (String) mzit.next();
                String mfullPath = resolvedFile.getFullXPath(ameta);
                parts.set(mfullPath);
                String mzone = parts.getAttributeValue(-1, "mzone");
                String from = parts.getAttributeValue(-1, "from");
                if (from == null) {
                    from = METAZONE_EPOCH;
                }
                String to = parts.getAttributeValue(-1, "to");
                String contents[] = { from, to, mzone };
                metaMap.put(from, contents);

                if (to == null) {
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
        showPathList(ctx, DataSection.EXEMPLAR_PARENT, null, false);
    }

    /**
     * This is the main function for showing lists of items (pods).
     */
    public void showPathList(WebContext ctx, String xpath, String lastElement) {
        showPathList(ctx, xpath, lastElement, false);

    }

    /**
     * Holds session lock
     *
     * @param ctx
     * @param xpath
     *            the 'base xpath' such as '//ldml/characters'
     * @param lastElement
     *            the 'final element' of each item, such as 'exemplarCharacters'
     * @param simple
     *            (ignored)
     */
    public void showPathListOld(WebContext ctx, String xpath, String lastElement, boolean simple) {
        // old way (nonDOM)
        showPathList(ctx, xpath, null, lastElement);

    }

    public void showPathList(WebContext ctx, String xpath, XPathMatcher matcher, String lastElement) {
        /* all simple */

        // synchronized(ctx.session) {
        // UserLocaleStuff uf = getUserFile(ctx.session, ctx.getLocale());
        // XMLSource ourSrc = uf.dbSource;
        // CLDRFile cf = uf.cldrfile;
        Thread curThread = Thread.currentThread();
        String threadName = curThread.getName();
        if (matcher != null) {
            curThread.setName(threadName + ":" + ctx.getLocale() + ":" + matcher.toString());
        } else if (xpath != null) {
            curThread.setName(threadName + ":" + ctx.getLocale() + ":" + xpt.getPrettyPath(xpath));
        } else {
            curThread.setName(threadName + ":" + ctx.getLocale());
        }
        try {
            CLDRFile cf = getSTFactory().make(ctx.getLocale().getBaseName(), false, DraftStatus.unconfirmed);
            // entry = getDBSourceFactory().openEntry(ourSrc);

            String fullThing = xpath + "/" + lastElement;
            // boolean isTz = xpath.equals("timeZoneNames");
            if (lastElement == null) {
                fullThing = xpath;
            }

            boolean canModify = ctx.getCanModify();

            {
                // TODO: move this into showExample. . .
                String e = ctx.field(QUERY_EXAMPLE);
                if (e.length() > 0) {
                    showPathListExample(ctx, xpath, lastElement, e, fullThing, cf);
                } else {
                    DataSection section = ctx.getSection(fullThing); // we load
                    // a new DataSection here - may be invalid by the modifications above.
                    section.showSection(ctx, canModify, matcher, false);
                }
            }
            // }
        } finally {
            curThread.setName(threadName);
        }
    }

    /**
     *  width, in columns, of the typical data table
     */
    static int PODTABLE_WIDTH = 13;

    /**
     * section may be null.
     *
     * @param ctx
     * @param section
     */
    static void printSectionTableOpenShort(WebContext ctx, DataSection section) {
        ctx.println("<a name='st_data'></a>");
        ctx.print("<table ");
        if (section != null) {
            ctx.print(" summary='Data Items for " + ctx.getLocale().toString() + " " + section.xpathPrefix + "' ");
        }
        ctx.println("class='data' border='1'>");
        ctx.println("<tr class='headingb'>\n" + " <th colspan='1' width='50%'>[" + BASELINE_LOCALE + "] "
            + BASELINE_LANGUAGE_NAME + "</th>\n" + // 3
            " <th colspan='2' width='50%'>Your Language</th>\n"); // 8

        ctx.println("</tr>");

    }

    /**
     * section may be null.
     *
     * @param ctx
     * @param section
     */
    static void printSectionTableOpenCode(WebContext ctx) {
        ctx.includeFragment("datarow_open_table_code.jsp");
        ctx.flush();
    }

    /**
     * Print closing table
     *
     * @param ctx
     */
    static void printSectionTableCloseCode(WebContext ctx) {
        ctx.includeFragment("datarow_close_table_code.jsp");
        ctx.flush();
    }

    /**
     * Section may be null.
     *
     * @param ctx
     * @param section
     */
    void printSectionTableClose(WebContext ctx, DataSection section, boolean canModify) {
        int table_width = (section != null && section.hasExamples) ? 13 : 10;
        if (!canModify) {
            table_width -= 4; // No vote, change, or no opinion columns
        }

        List<String> refsList = (List<String>) ctx.temporaryStuff.get("references");
        if (section != null && (refsList != null) && (!refsList.isEmpty())) {
            ctx.println("<tr></tr>");
            ctx.println("<tr class='heading'><th class='partsection' align='left' colspan='" + table_width
                + "'>References</th></tr>");
            int n = 0;

            Hashtable<String, DataSection.DataRow> refsHash = (Hashtable<String, DataSection.DataRow>) ctx.temporaryStuff
                .get("refsHash");
            Hashtable<String, DataSection.DataRow.CandidateItem> refsItemHash = (Hashtable<String, DataSection.DataRow.CandidateItem>) ctx.temporaryStuff
                .get("refsItemHash");

            for (String ref : refsList) {
                n++;
                ctx.println("<tr class='referenceRow'><th><img src='http://unicode.org/cldr/data/dropbox/misc/images/reference.jpg'>#"
                    + n + "</th>");
                ctx.println("<td colspan='" + 1 + "'>" + ref + "</td>");
                ctx.print("<td colspan='" + (table_width - 2) + "'>");
                if (refsHash != null) {
                    DataSection.DataRow refDataRow = refsHash.get(ref);
                    DataSection.DataRow.CandidateItem refDataRowItem = refsItemHash.get(ref);
                    if ((refDataRowItem != null) && (refDataRow != null)) {
                        ctx.print(refDataRowItem.getValue());
                        if (refDataRow.getDisplayName() != null) {
                            ctx.println("<br>" + refDataRow.getDisplayName());
                        }
                        // ctx.print(refPea.displayName);
                    } else {
                        ctx.print("<i>unknown reference</i>");
                    }
                }
                ctx.print("</td>");
                ctx.println("</tr>");
            }

        }
        if (refsList != null) {
            ctx.temporaryStuff.remove("references");
            ctx.temporaryStuff.remove("refsHash");
            ctx.temporaryStuff.remove("refsItemHash");
        }

        ctx.println("</table>");
    }

    public void updateLocale(CLDRLocale locale) {
        SurveyLog.logger.warning("updateLocale:" + locale.toString());
    }

    static final UnicodeSet CallOut = new UnicodeSet("[\\u200b-\\u200f]");

    public static int pages = 0;
    public static int xpages = 0;
    /**
     * Main setup
     */
    static public boolean isSetup = false;

    private static ScheduledExecutorService surveyTimer = null;

    // private List<PeriodicTask> periodicTasks = null;

    public static synchronized ScheduledExecutorService getTimer() {
        if (surveyTimer == null) {
            surveyTimer = Executors.newScheduledThreadPool(2);
        }
        return surveyTimer;
    }

    /**
     * Periodic task for file output
     * @param task
     * @return
     */
    public static ScheduledFuture<?> addPeriodicTask(Runnable task) {
        final boolean CLDR_QUICK_DAY = CldrUtility.getProperty("CLDR_QUICK_DAY", false);
        int firstTime = isUnofficial() ? 15 : 30;
        int eachTime = isUnofficial() ? 15 : 15;

        if (CLDR_QUICK_DAY && isUnofficial()) {
            firstTime = 1;
            eachTime = 3;
        }
        return getTimer().scheduleWithFixedDelay(task, firstTime, eachTime, TimeUnit.MINUTES);
    }

    public static ScheduledFuture<?>[] addDailyTask(Runnable task) {
        long now = System.currentTimeMillis();
        long next = now;
        long period = 24 * 60 * 60 * 1000; // 1 day
        Calendar c = com.ibm.icu.util.Calendar.getInstance(TimeZone.getTimeZone(CldrUtility.getProperty("CLDR_TZ",
            "America/Los_Angeles")));

        final boolean CLDR_QUICK_DAY = CldrUtility.getProperty("CLDR_QUICK_DAY", false);

        if (CLDR_QUICK_DAY && isUnofficial()) {
            // c.add(Calendar.HOUR_OF_DAY, 1);
            // c.add(Calendar.MINUTE, 1);
            // c.set(Calendar.SECOND, 0);
            c.add(Calendar.SECOND, 85); // right away!!
            period = 15 * 60 * 1000; // 15 min
        } else {
            c.add(Calendar.DATE, 1);
            c.set(Calendar.HOUR_OF_DAY, 2);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
        }
        next = c.getTimeInMillis();
        System.err.println("DailyTask- next time is " + ElapsedTimer.elapsedTime(now, next) + " and period is "
            + ElapsedTimer.elapsedTime(now, now + period));

        ScheduledFuture<?> o[] = { null, null };
        o[0] = getTimer().schedule(task, 5, TimeUnit.MINUTES); // run one soon
        // after startup
        o[1] = getTimer().scheduleAtFixedRate(task, next - now, period, TimeUnit.MILLISECONDS);
        return o;
    }

    /**
     * Class to startup ST in background and perform background operations.
     */
    public transient SurveyThread startupThread = new SurveyThread(this);

    /**
     * Progress bar manager
     */
    SurveyProgressManager progressManager = new SurveyProgressManager();

    private String cldrHome;

    static File supplementalDataDir;

    /**
     * Startup function. Called from another thread.
     *
     * @throws ServletException
     */
    public synchronized void doStartup() {
        if (isSetup == true) {
            return;
        }
        ElapsedTimer setupTime = new ElapsedTimer();
        CLDRProgressTask progress = openProgress("Main Startup");
        try {
            // set up CheckCLDR
            // CheckCLDR.SHOW_TIMES=true;

            progress.update("Initializing Properties");

            CLDRConfig survprops = CLDRConfig.getInstance();

            isConfigSetup = true;

            cldrHome = survprops.getProperty("CLDRHOME");

            System.err.println("CLDRHOME=" + cldrHome + ", maint mode=" + isMaintenance());

            stopIfMaintenance();

            progress.update("Setup DB config");
            // set up DB properties
            dbUtils.setupDBProperties(this, survprops);
            progress.update("Setup phase..");

            // phase
            {
                Phase newPhase = null;
                String phaseString = survprops.getProperty("CLDR_PHASE", null);
                try {
                    if (phaseString != null) {
                        newPhase = (Phase.valueOf(phaseString));
                    }
                } catch (IllegalArgumentException iae) {
                    SurveyLog.logger.warning("Error trying to parse CLDR_PHASE: " + iae.toString());
                }
                if (newPhase == null) {
                    StringBuffer allValues = new StringBuffer();
                    for (Phase v : Phase.values()) {
                        allValues.append(v.name());
                        allValues.append(' ');
                    }
                    busted("Could not parse CLDR_PHASE - should be one of ( " + allValues + ") but instead got " + phaseString);
                }
                currentPhase = newPhase;
            }
            System.out.println("Phase: " + phase() + ", cPhase: " + phase().getCPhase() + ", " + getCurrevStr());
            progress.update("Setup props..");
            newVersion = survprops.getProperty(CLDR_NEWVERSION, CLDR_NEWVERSION);
            oldVersion = survprops.getProperty(CLDR_OLDVERSION, CLDR_OLDVERSION);
            lastVoteVersion = survprops.getProperty(CLDR_LASTVOTEVERSION, oldVersion);
            progress.update("Setup dirs..");

            getVetdir();

            progress.update("Setup vap and message..");
            testpw = survprops.getProperty("CLDR_TESTPW"); // Vet Access
            // Password
            vap = survprops.getProperty("CLDR_VAP"); // Vet Access Password
            if ((vap == null) || (vap.length() == 0)) {
                /* throw new UnavailableException */
                busted("No vetting password set. (CLDR_VAP in cldr.properties)");
                return;
            }
            if ("yes".equals(survprops.getProperty("CLDR_OFFICIAL"))) {
                survprops.setEnvironment(CLDRConfig.Environment.PRODUCTION);
            } else {
                survprops.getEnvironment();
            }
            vetweb = survprops.getProperty("CLDR_VET_WEB", cldrHome + "/vetdata"); // dir
            // for
            // web
            // data
            cldrLoad = survprops.getProperty("CLDR_LOAD_ALL"); // preload all
            // locales?
            // System.getProperty("CLDR_COMMON") + "/main" is ignored.
            getFileBase();
            getFileBaseSeed();
            setFileBaseOld(survprops.getProperty(getOldVersionParam(), cldrHome + "/" + oldVersion)); // not
            // static
            // -
            // may
            // change
            // lager
            specialMessage = survprops.getProperty("CLDR_MESSAGE"); // not
            // static -
            // may
            // change
            // lager

            lockOut = survprops.getProperty("CLDR_LOCKOUT");

            if (!new File(fileBase).isDirectory()) {
                busted("CLDR_COMMON isn't a directory: " + fileBase);
                return;
            }
            if (!new File(fileBaseSeed).isDirectory()) {
                busted("CLDR_SEED isn't a directory: " + fileBaseSeed);
                return;
            }

            progress.update("Get CLDR " + oldVersion + " data (svn export if necessary)");
            getOldFactory(); // check old version
            if (!new File(vetweb).isDirectory()) {
                busted("CLDR_VET_WEB isn't a directory: " + vetweb);
                return;
            }
            progress.update("Setup cache..");

            File cacheDir = new File(cldrHome, "cache");
            // logger.info("Cache Dir: " + cacheDir.getAbsolutePath() +
            // " - creating and emptying..");
            CachingEntityResolver.setCacheDir(cacheDir.getAbsolutePath());
            CachingEntityResolver.createAndEmptyCacheDir();

            progress.update("Setup supplemental..");
            getSupplementalDataInfo();

            try {
                // spin up the gears
                CLDRLocale dcParent = getSupplementalDataInfo().getBaseFromDefaultContent(CLDRLocale.getInstance("mt_MT"));
            } catch (InternalError ie) {
                SurveyLog.logger.warning("can't do SupplementalData.defaultContentToParent() - " + ie);
                ie.printStackTrace();
                busted("can't do SupplementalData.defaultContentToParent() - " + ie, ie);
            }
            progress.update("Checking if startup completed..");

            // int status = 0;
            // logger.info(" ------------------ " + new Date().toString() +
            // " ---------------");
            if (isBusted != null) {
                return; // couldn't write the log
            }
            if ((specialMessage != null) && (specialMessage.length() > 0)) {
                SurveyLog.logger.warning("SurveyTool with CLDR_MESSAGE: " + specialMessage);
                busted("message: " + specialMessage);
            }
            /*
             * SurveyMain m = new SurveyMain(); ???
             *
             * if(!m.reg.read()) { busted(
             * "Couldn't load user registry [at least put an empty file there]   - exiting"
             * ); return; }
             */
            progress.update("Setup warnings..");
            if (!readWarnings()) {
                // already busted
                return;
            }

            progress.update("Setup baseline file..");

            // load baseline file
            getBaselineFile();

            progress.update("Setup baseline example..");

            // and example
            getBaselineExample();

            progress.update("Wake up the database..");

            doStartupDB(); // will take over progress 50-60

            progress.update("Making your Survey Tool happy..");

            if (isBusted == null) { // don't do these if we are already busted
                MailSender.getInstance();
                if (!CldrUtility.getProperty("CLDR_NOUPDATE", false)) {
                    getOutputFileManager().addUpdateTasks();
                }
            } else {
                progress.update("Not loading mail or output file manager- - SurveyTool already busted.");
            }

        } catch (Throwable t) {
            t.printStackTrace();
            SurveyLog.logException(t, "StartupThread");
            busted("Error on startup: ", t);
        } finally {
            progress.close();
        }

        /**
         * Cause locale alias to be checked.
         */
        if (!isBusted()) {
            isLocaleAliased(CLDRLocale.ROOT);
        }

        {
            CLDRConfig cconfig = CLDRConfig.getInstance();
            SurveyLog.logger
                .info("Phase: " + cconfig.getPhase() + " " + getNewVersion() + ",  environment: " + cconfig.getEnvironment() + " " + getCurrevStr());
        }
        if (!isBusted()) {
            SurveyLog.logger.info("------- SurveyTool ready for requests after " + setupTime + "/" + uptime + ". Memory in use: " + usedK()
                + "----------------------------\n\n\n");
            isSetup = true;
        } else {
            SurveyLog.logger.info("------- SurveyTool FAILED TO STARTUP, " + setupTime + "/" + uptime + ". Memory in use: " + usedK()
                + "----------------------------\n\n\n");
        }
    }

    public static void stopIfMaintenance() {
        stopIfMaintenance(null);
    }

    public static void stopIfMaintenance(HttpServletRequest request) {
        final File maintFile = getHelperFile();
        final String maintMessage = getMaintMessage(maintFile, request);
        if (isMaintenance()) {
            if (!maintFile.exists()) {
                busted(
                    "SurveyTool is in setup mode. Please view the main page such as http://127.0.0.1:8080/cldr-apps/survey/ so we can generate a helper file.");
            } else {
                isBusted = null; // reset busted notice
                busted(maintMessage);
            }
        }
    }

    private static String getMaintMessage(final File maintFile, HttpServletRequest request) {
        if (!maintFile.exists() && request != null) {
            try {
                writeHelperFile(request, maintFile);
            } catch (IOException e) {
                busted("Trying to write helper file " + maintFile.getAbsolutePath(), e);
            }
        }
        if (maintFile.exists()) {
            final String maintMessage = "SurveyTool is in setup mode. <br><b>Administrator</b>: Please open the file <a href='file://"
                + maintFile.getAbsolutePath() + "'>" + maintFile.getAbsolutePath() + "</a>"
                + " for more instructions. <br><b>Users:</b> you must wait until the SurveyTool is back online.";
            return maintMessage;
        } else {
            return null;
        }
    }

    public static synchronized void writeHelperFile(HttpServletRequest request, File maintFile) throws IOException {
        CLDRConfigImpl.getInstance().writeHelperFile(request.getScheme() + "://" + request.getServerName() + ":" +
            request.getServerPort() + request.getContextPath() + "/", maintFile);
    }

    public static File getHelperFile() {
        File maintFile = new File(getSurveyHome(), "admin.html");
        return maintFile;
    }

    public static boolean isMaintenance() {
        if (!isConfigSetup) return false; // avoid access to CLDRConfig before setup.
        CLDRConfig survprops = CLDRConfig.getInstance();
        return survprops.getProperty("CLDR_MAINTENANCE", false);
    }

    public synchronized File getVetdir() {
        if (_vetdir == null) {
            CLDRConfig survprops = CLDRConfig.getInstance();
            vetdata = survprops.getProperty("CLDR_VET_DATA", SurveyMain.getSurveyHome() + "/vetdata"); // dir
            // for
            // vetted
            // data
            File v = new File(vetdata);
            if (!v.isDirectory()) {
                v.mkdir();
                SurveyLog.logger.warning("## creating empty vetdir: " + v.getAbsolutePath());
            }
            if (!v.isDirectory()) {
                busted("CLDR_VET_DATA isn't a directory: " + v);
                throw new InternalError("CLDR_VET_DATA isn't a directory: " + v);
            }
            _vetdir = v;
        }
        return _vetdir;
    }

    public File makeDataDir(String kind) throws IOException {
        File vetdir = getVetdir();
        if (vetdir == null) {
            throw new InternalError("vetdir is null.");
        }
        File dataDir = new File(vetdir, kind);
        if (!dataDir.exists()) {
            if (!dataDir.mkdirs()) {
                throw new IOException("Couldn't create " + dataDir.getAbsolutePath());
            }
            getOutputFileManager().svnAddOrWarn(dataDir);
        }
        return dataDir;
    }

    private File makeDataDir(String kind, CLDRLocale loc) throws IOException {
        File dataDir = makeDataDir(kind); // get the parent dir.

        // rest of this function is just to determine which subdir (common or
        // seed)

        Factory f = getDiskFactory();
        //String l = loc.getBaseName();
        File sourceDir = f.getSourceDirectoryForLocale(loc.getBaseName());

        SourceTreeType sourceType = Factory.getSourceTreeType(sourceDir);
        DirectoryType dirType = Factory.getDirectoryType(sourceDir);
        File subDir = new File(dataDir, sourceType.name());
        if (!subDir.exists()) {
            if (!subDir.mkdirs()) {
                throw new IOException("Couldn't create " + subDir.getAbsolutePath());
            }
            getOutputFileManager().svnAddOrWarn(subDir);
        }
        File subSubDir = new File(subDir, dirType.name());
        if (!subSubDir.exists()) {
            if (!subSubDir.mkdirs()) {
                throw new IOException("Couldn't create " + subSubDir.getAbsolutePath());
            }
            getOutputFileManager().svnAddOrWarn(subSubDir);
        }
        return subSubDir;
    }

    public File getDataDir(String kind, CLDRLocale loc) throws IOException {
        return getDataFile(kind, loc).getParentFile();
    }

    Map<Pair<String, CLDRLocale>, File> dirToFile = new HashMap<Pair<String, CLDRLocale>, File>();

    /**
     * Just get the File. Don't write it.
     *
     * @param kind
     * @param loc
     * @return
     * @throws IOException
     */
    public synchronized File getDataFile(String kind, CLDRLocale loc) throws IOException {
        Pair<String, CLDRLocale> k = new Pair<String, CLDRLocale>(kind, loc);
        File f = dirToFile.get(k);
        if (f == null) {
            f = makeDataFile(kind, loc);
            if (f != null) {
                dirToFile.put(k, f);
            }
        }
        return f;
    }

    private File makeDataFile(String kind, CLDRLocale loc) throws IOException {
        return new File(makeDataDir(kind, loc), loc.toString() + ".xml");
    }

    public OutputFileManager outputFileManager = null;

    public synchronized OutputFileManager getOutputFileManager() {
        if (outputFileManager == null) {
            outputFileManager = new OutputFileManager(this);
        }
        return outputFileManager;
    }

    private String getOldVersionParam() {
        return "CLDR_COMMON" + oldVersion;
    }

    public static boolean isBusted() {
        return (isBusted != null);
    }

    public void destroy() {
        ElapsedTimer destroyTimer = new ElapsedTimer("SurveyTool destroy()");
        CLDRProgressTask progress = openProgress("shutting down");
        try {
            SurveyLog.logger.warning("SurveyTool shutting down.. r" + getCurrevStr());
            if (startupThread != null) {
                progress.update("Attempting clean shutdown...");
                startupThread.attemptCleanShutdown();
            }
            progress.update("shutting down mail... " + destroyTimer);
            MailSender.shutdown();
            if (surveyTimer != null) {
                progress.update("Shutting down timer...");
                int patience = 20;
                surveyTimer.shutdown();
                Thread.yield();
                while (surveyTimer != null && !surveyTimer.isTerminated()) {
                    try {
                        System.err.println("Still Shutting down timer.. " + surveyTimer.toString() + destroyTimer);
                        if (surveyTimer.awaitTermination(2, TimeUnit.SECONDS)) {
                            System.err.println("Timer thread is down." + destroyTimer);
                            surveyTimer = null;
                        } else {
                            System.err.println("Timer thread is still running. Attempting TerminateNow." + destroyTimer);
                            surveyTimer.shutdownNow();
                        }
                        Thread.yield();
                        if (--patience < 0) {
                            System.err.println("=========== patience exceeded. ignoring errant surveyTimer. ==========\n");
                            surveyTimer = null;
                        }
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                surveyTimer = null;
                System.err.println("Timer thread cancelled." + destroyTimer);
                Thread.yield();
            }
            progress.update("Shutting down database..." + destroyTimer);
            doShutdownDB();
            progress.update("Shutting down SVN..." + destroyTimer);
            getOutputFileManager().svnShutdown();
            outputFileManager = null;
            progress.update("Destroying servlet..." + destroyTimer);
            if (isBusted != null)
                isBusted = "servlet destroyed + destroyTimer";
            super.destroy();
            SurveyLog.shutdown();
        } finally {
            progress.close();
            System.out.println("------------------- end of SurveyMain.destroy() ------------" + uptime + destroyTimer);
        }
    }

    protected void startCell(WebContext ctx, String background) {
        ctx.println("<td bgcolor=\"" + background + "\">");
    }

    protected void endCell(WebContext ctx) {
        ctx.println("</td>");
    }

    protected void doCell(WebContext ctx, String type, String value) {
        startCell(ctx, "#FFFFFF");
        ctx.println(value);
        endCell(ctx);
    }

    protected void doDraftCell(WebContext ctx, String type, String value) {
        startCell(ctx, "#DDDDDD");
        ctx.println("<i>Draft</i><br/>");
        ctx.println(value);
        endCell(ctx);
    }

    protected void doPropCell(WebContext ctx, String type, String value) {
        startCell(ctx, "#DDFFDD");
        ctx.println("<i>Proposed:</i><br/>");
        ctx.println(value);
        endCell(ctx);
    }

    // utils
    private Node getVettedNode(Node context, String resToFetch) {
        NodeList list = LDMLUtilities.getChildNodes(context, resToFetch);
        Node node = null;
        if (list != null) {
            for (int i = 0; i < list.getLength(); i++) {
                node = list.item(i);
                if (LDMLUtilities.isNodeDraft(node)) {
                    continue;
                }
                /*
                 * if(isAlternate(node)){ continue; }
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
                return (!f.isDirectory() && n.endsWith(".xml") && !n.startsWith(".") && !n.startsWith("supplementalData") // not
                // a
                // locale
                /* &&!n.startsWith("root") */); // root is implied, will be
                // included elsewhere.
            }
        };
    }

    /**
     * Internal function to get all input files.
     * Most functions should use getLocalesSet, etc.
     * @return
     */
    static protected File[] getInFiles() {
        //        ElapsedTimer et = SurveyLog.DEBUG ? new ElapsedTimer("getInFiles()") : null;
        Set<File> s = new HashSet<File>();
        for (File f : getInFiles(fileBase)) {
            s.add(f);
        }
        for (File f : getInFiles(fileBaseSeed)) {
            s.add(f);
        }
        File arr[] = s.toArray(new File[s.size()]);
        //        SurveyLog.debug(et);
        return arr;
    }

    /**
     * Only to be used by getInFiles.
     * @param base
     * @return
     */
    static private File[] getInFiles(String base) {
        File baseDir = new File(base);
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
        String theLocale = localeName.substring(0, dot);
        return CLDRLocale.getInstance(theLocale);
    }

    private static Set<CLDRLocale> localeListSet = null;
    private static Set<CLDRLocale> openLocales = null;
    private static Set<CLDRLocale> roLocales = null;
    private static Set<CLDRLocale> topLocalesSet = null;
    static STFactory.LocaleMaxSizer localeSizer;

    /**
     * Get the list of locales 'open' for submission. Should be all locales that
     * aren't either read-only or in the "only locales" list.
     *
     * @return
     */
    static final synchronized public Set<CLDRLocale> getOpenLocales() {
        if (openLocales == null)
            loadLocalesSet();
        return openLocales;
    }

    /**
     * Get the list of locales which are read only for some reason. These won't
     * be generated, and will be shown with a lock symbol.
     *
     * @return
     */
    static final synchronized public Set<CLDRLocale> getReadOnlyLocales() {
        if (roLocales == null)
            loadLocalesSet();
        return roLocales;
    }

    /**
     * Get the list of locales that we have seen anywhere. Static set generated
     * from {@link #getInFiles()}
     *
     * @return
     */
    static final synchronized public Set<CLDRLocale> getLocalesSet() {
        if (localeListSet == null)
            loadLocalesSet();
        return localeListSet;
    }

    /**
     * Get a list of all top level locales.
     *
     * @return
     */
    public static final synchronized Set<CLDRLocale> getTopLocalesSet() {
        if (topLocalesSet == null) {
            Set<CLDRLocale> s = new HashSet<CLDRLocale>();
            for (CLDRLocale l : getLocalesSet()) {
                CLDRLocale par = l.getParent();
                if (par == null || // root
                    par.getParent() == null) { // child of root
                    s.add(l);
                }
            }
            topLocalesSet = Collections.unmodifiableSet(s);
        }
        return topLocalesSet;
    }

    /**
     * Set up the list of open vs read-only locales, and the full set.
     */
    private static synchronized void loadLocalesSet() {
        File inFiles[] = getInFiles();
        int nrInFiles = inFiles.length;
        Set<CLDRLocale> s = new TreeSet<CLDRLocale>();
        Set<CLDRLocale> ro = new TreeSet<CLDRLocale>();
        Set<CLDRLocale> w = new TreeSet<CLDRLocale>();
        STFactory.LocaleMaxSizer lms = new STFactory.LocaleMaxSizer();

        String onlyLocales = CLDRConfig.getInstance().getProperty("CLDR_ONLY_LOCALES", null);
        Set<String> onlySet = null;

        if (onlyLocales != null && !onlyLocales.isEmpty()) {
            onlySet = new TreeSet<String>();
            for (String ol : onlyLocales.split("[ \t]")) {
                onlySet.add(ol);
            }
        }

        for (int i = 0; i < nrInFiles; i++) {
            String fileName = inFiles[i].getName();
            int dot = fileName.indexOf('.');
            if (dot != -1) {
                String locale = fileName.substring(0, dot);
                CLDRLocale l = CLDRLocale.getInstance(locale);
                s.add(l); // all
                SpecialLocales.Type t = (SpecialLocales.getType(l));
                if (t == Type.scratch) {
                    w.add(l); // always added
                } else if (t == Type.readonly || (onlySet != null && !onlySet.contains(locale))) {
                    ro.add(l); // readonly
                } else {
                    w.add(l); // writeable
                }
                lms.add(l);
            }
        }
        localeListSet = Collections.unmodifiableSet(s);
        roLocales = Collections.unmodifiableSet(ro);
        openLocales = Collections.unmodifiableSet(w);
        localeSizer = lms;
    }

    /**
     * Array of locales - calculated from {@link #getLocalesSet()}
     *
     * @return
     */
    static public CLDRLocale[] getLocales() {
        return (CLDRLocale[]) getLocalesSet().toArray(new CLDRLocale[0]);
    }

    /**
     * Returns a Map of all interest groups. en -> en, en_US, en_MT, ... fr ->
     * fr, fr_BE, fr_FR, ...
     */
    static protected Map<CLDRLocale, Set<CLDRLocale>> getIntGroups() {
        // TODO: rewrite as iterator
        CLDRLocale[] locales = getLocales();
        Map<CLDRLocale, Set<CLDRLocale>> h = new HashMap<CLDRLocale, Set<CLDRLocale>>();
        for (int i = 0; i < locales.length; i++) {
            CLDRLocale locale = locales[i];
            CLDRLocale group = locale;
            int dash = locale.toString().indexOf('_');
            if (dash != -1) {
                group = CLDRLocale.getInstance(locale.toString().substring(0, dash));
            }
            Set<CLDRLocale> s = h.get(group);
            if (s == null) {
                s = new HashSet<CLDRLocale>();
                h.put(group, s);
            }
            s.add(locale);
        }
        return h;
    }

    public boolean isValidLocale(CLDRLocale locale) {
        return getLocalesSet().contains(locale);
    }

    //
    // /*
    // * returns a map of String localegroup -> Set [ User interestedUser, ... ]
    // */
    // protected Map getIntUsers(Map intGroups) {
    // Map m = new HashMap();
    // Connection conn = null;
    // try {
    // conn = dbUtils.getDBConnection();
    // synchronized (reg) {
    // java.sql.ResultSet rs = reg.list(null, conn);
    // if (rs == null) {
    // return m;
    // }
    // while (rs.next()) {
    // int theirLevel = rs.getInt(2);
    // if (theirLevel > UserRegistry.VETTER) {
    // continue; // will not receive notice.
    // }
    //
    // int theirId = rs.getInt(1);
    // UserRegistry.User u = reg.getInfo(theirId);
    // // String theirName = rs.getString(3);
    // // String theirEmail = rs.getString(4);
    // // String theirOrg = rs.getString(5);
    // String theirLocales = rs.getString(6);
    // String theirIntlocs = rs.getString(7);
    //
    // String localeArray[] = UserRegistry.tokenizeLocale(theirLocales);
    //
    // if ((theirId <= UserRegistry.TC) || (localeArray.length == 0)) { // all
    // // locales
    // localeArray = UserRegistry.tokenizeLocale(theirIntlocs);
    // }
    //
    // if (localeArray.length == 0) {
    // for (Iterator li = intGroups.keySet().iterator(); li.hasNext();) {
    // String group = (String) li.next();
    // Set v = (Set) m.get(group);
    // if (v == null) {
    // v = new HashSet();
    // m.put(group, v);
    // }
    // v.add(u);
    // // SurveyLog.logger.warning(group + " - " + u.email
    // // +
    // // " (ALL)");
    // }
    // } else {
    // for (int i = 0; i < localeArray.length; i++) {
    // String group = localeArray[i];
    // Set v = (Set) m.get(group);
    // if (v == null) {
    // v = new HashSet();
    // m.put(group, v);
    // }
    // v.add(u);
    // // SurveyLog.logger.warning(group + " - " + u.email
    // // + "");
    // }
    // }
    // }
    // }
    // } catch (SQLException se) {
    // busted("SQL error querying users for getIntUsers - " +
    // DBUtils.unchainSqlException(se));
    // throw new RuntimeException("SQL error querying users for getIntUsers - "
    // + DBUtils.unchainSqlException(se));
    //
    // } finally {
    // DBUtils.close(conn);
    // }
    // return m;
    // }

    public static final com.ibm.icu.text.Transliterator hexXML = com.ibm.icu.text.Transliterator
        .getInstance("[^\\u0009\\u000A\\u0020-\\u007E\\u00A0-\\u00FF] Any-Hex/XML");

    // like above, but quote "
    public static final com.ibm.icu.text.Transliterator quoteXML = com.ibm.icu.text.Transliterator
        .getInstance("[^\\u0009\\u000A\\u0020-\\u0021\\u0023-\\u007E\\u00A0-\\u00FF] Any-Hex/XML");

    public static int usedK() {
        Runtime r = Runtime.getRuntime();
        double total = r.totalMemory();
        total = total / 1024;
        double free = r.freeMemory();
        free = free / 1024;
        return (int) (Math.floor(total - free));
    }

    public static void busted(String what) {
        busted(what, null, null);
    }

    /**
     * Report an error with a SQLException
     *
     * @param what
     *            the error
     * @param se
     *            the SQL Exception
     */
    protected static void busted(String what, SQLException se) {
        busted(what, se, DBUtils.unchainSqlException(se));
    }

    protected static void busted(String what, Throwable t) {
        if (t instanceof SQLException) {
            busted(what, (SQLException) t);
        } else {
            busted(what, t, getThrowableStack(t));
        }
    }

    /**
     * mark as busted, with no special logging. This is called by the SurveyLog to make sure an out of memory marks things as down.
     * @param t
     */
    public static void markBusted(Throwable t) {
        markBusted(t.toString(), t, StackTracker.stackToString(t.getStackTrace(), 0));
    }

    /**
     * log that the survey tool is down.
     * @param what
     * @param t
     * @param stack
     */
    protected static void busted(String what, Throwable t, String stack) {
        if (t != null) {
            SurveyLog.logException(t, what /* , ignore stack - fetched from exception */);
        }
        SurveyLog.logger.warning("SurveyTool " + SurveyMain.getCurrevStr() + " busted: " + what + " ( after " + pages + "html+" + xpages
            + "xml pages served,  "
            + getGuestsAndUsers() + ")");
        System.err.println("Busted at stack: \n" + StackTracker.currentStack());
        markBusted(what, t, stack);
        SurveyLog.logger.severe(what);
    }

    /**
     * Mark busted, but don't log it
     * @param what
     * @param t
     * @param stack
     */
    public static void markBusted(String what, Throwable t, String stack) {
        SurveyLog.warnOnce("******************** SurveyTool is down (busted) ********************");
        if (!isBusted()) { // Keep original failure message.
            isBusted = what;
            if (stack == null) {
                if (t != null) {
                    stack = StackTracker.stackToString(t.getStackTrace(), 0);
                } else {
                    stack = "(no stack)\n";
                }
            }
            isBustedStack = stack + "\n" + "[" + new Date().toGMTString() + "] ";
            //isBustedThrowable = t;
            isBustedTimer = new ElapsedTimer();
        } else {
            SurveyLog.warnOnce("[was already busted, not overriding old message.]");
        }
    }

    private void appendLog(WebContext ctx, String what) {
        String ipInfo = ctx.userIP();
        appendLog(what, ipInfo);
    }

    public void appendLog(String what) {
        SurveyLog.logger.info(what);
    }

    public synchronized void appendLog(String what, String ipInfo) {
        SurveyLog.logger.info(what + " [@" + ipInfo + "]");
    }

    TreeMap<String, String> allXpaths = new TreeMap<String, String>();
    //public static Set draftSet = Collections.synchronizedSet(new HashSet());

    static final public String[] distinguishingAttributes = { "key", "registry", "alt", "iso4217", "iso3166", "type", "default",
        "measurementSystem", "mapping", "abbreviationFallback", "preferenceOrdering" };

    static int xpathCode = 0;
    //
    //    /**
    //     * convert a XPATH:TYPE form to an html field. if type is null, means: hash
    //     * the xpath
    //     */
    //    String fieldsToHtml(String xpath, String type) {
    //        if (type == null) {
    //            String r = (String) allXpaths.get(xpath);
    //            if (r == null)
    //                synchronized (allXpaths) {
    //                    // we've found a totally new xpath. Mint a new key.
    //                    r = CookieSession.j + "Y" + CookieSession.cheapEncode(xpathCode++);
    //                    allXpaths.put(xpt.poolx(xpath), r);
    //                }
    //            return r;
    //        } else {
    //            return xpath + "/" + type;
    //        }
    //    }

    static long shortN = 0;
    static final int MAX_CHARS = 100;
    static final int LARGER_MAX_CHARS = 256;
    static final String SHORT_A = "(Click to show entire message.)";
    static final String SHORT_B = "(hide.)";

    public static final String QUERY_FIELDHASH = "fhash";

    public static final String QUERY_XFIND = "xfind";

    static void printShortened(WebContext ctx, String str) {
        ctx.println(getShortened(str));
    }

    private static void printShortened(WebContext ctx, String str, int max) {
        ctx.println(getShortened(str, max));
    }

    private static String getShortened(String str) {
        return getShortened(str, MAX_CHARS);
    }

    private static synchronized String getShortened(String str, int max) {
        if (str.length() < (max + 1 + SHORT_A.length())) {
            return (str);
        } else {
            int cutlen = max;
            String key = CookieSession.cheapEncode(shortN++);
            int newline = str.indexOf('\n');
            if ((newline > 2) && (newline < cutlen)) {
                cutlen = newline;
            }
            newline = str.indexOf("Exception:");
            if ((newline > 2) && (newline < cutlen)) {
                cutlen = newline;
            }
            newline = str.indexOf("Message:");
            if ((newline > 2) && (newline < cutlen)) {
                cutlen = newline;
            }
            newline = str.indexOf("<br>");
            if ((newline > 2) && (newline < cutlen)) {
                cutlen = newline;
            }
            newline = str.indexOf("<p>");
            if ((newline > 2) && (newline < cutlen)) {
                cutlen = newline;
            }
            return getShortened(str.substring(0, cutlen), str, key);
        }
    }

    private void printShortened(WebContext ctx, String shortStr, String longStr, String warnHash) {
        ctx.println(getShortened(shortStr, longStr, warnHash));
    }

    private static String getShortened(String shortStr, String longStr, String warnHash) {
        return ("<span id='h_ww" + warnHash + "'>" + shortStr + "... ")
            + ("<a href='javascript:show(\"ww" + warnHash + "\")'>" + SHORT_A + "</a></span>")
            + ("<!-- <noscript>Warning: </noscript> -->" + "<span style='display: none'  id='ww" + warnHash + "'>" + longStr
                + "<a href='javascript:hide(\"ww" + warnHash + "\")'>" + SHORT_B + "</a></span>");
    }

    Hashtable<String, String> xpathWarnings = new Hashtable<String, String>();

    String getWarning(String locale, String xpath) {
        return (String) xpathWarnings.get(locale + " " + xpath);
    }

    boolean readWarnings() {
        //int lines = 0;
        try {
            BufferedReader in = FileUtilities.openUTF8Reader(cldrHome, "surveyInfo.txt");
            String line;
            while ((line = in.readLine()) != null) {
                //lines++;
                if ((line.length() <= 0) || (line.charAt(0) == '#')) {
                    continue;
                }
                String[] result = line.split("\t");
                // result[0]; locale
                // result[1]; xpath
                // result[2]; warning
                xpathWarnings.put(result[0] + " /" + result[1], result[2]);
            }
        } catch (java.io.FileNotFoundException t) {
            // SurveyLog.logger.warning(t.toString());
            // t.printStackTrace();
            // logger.warning("Warning: Can't read xpath warnings file.  " +
            // cldrHome +
            // "/surveyInfo.txt - To remove this warning, create an empty file there.");
            return true;
        } catch (java.io.IOException t) {
            SurveyLog.logger.warning(t.toString());
            t.printStackTrace();
            busted("Error: trying to read xpath warnings file.  " + cldrHome + "/surveyInfo.txt");
            return true;
        }

        // SurveyLog.logger.warning("xpathWarnings" + ": " + lines +
        // " lines read.");
        return true;
    }

    private static Hashtable<String, String> stringHash = new Hashtable<String, String>();

    static int stringHashIdentity = 0; // # of x==y hits
    static int stringHashHit = 0;

    static final String pool(String x) {
        if (x == null) {
            return null;
        }
        String y = (String) stringHash.get(x);
        if (y == null) {
            stringHash.put(x, x);
            return x;
        } else {
            return y;
        }
    }

    public DBUtils dbUtils = null;

    private void doStartupDB() {
        if (isMaintenance()) {
            throw new InternalError("SurveyTool is in setup mode.");
        }
        CLDRProgressTask progress = openProgress("Database Setup");
        try {
            progress.update("begin.."); // restore
            dbUtils.startupDB(this, progress);
            // now other tables..
            progress.update("Setup databases "); // restore
            try {
                progress.update("Setup  " + UserRegistry.CLDR_USERS); // restore
                progress.update("Create UserRegistry  " + UserRegistry.CLDR_USERS); // restore
                reg = UserRegistry.createRegistry(SurveyLog.logger, this);
            } catch (SQLException e) {
                busted("On UserRegistry startup", e);
                return;
            }
            progress.update("Create XPT"); // restore
            try {
                xpt = XPathTable.createTable(dbUtils.getDBConnection(), this);
            } catch (SQLException e) {
                busted("On XPathTable startup", e);
                return;
            }

            progress.update("Load XPT");
            System.err.println("XPT ready with " + xpt.statistics());
            xpt.loadXPaths(getDiskFactory().makeSource(BASELINE_ID)); // grind
            // over
            // baseline
            System.err.println("XPT spun up with " + xpt.statistics());
            progress.update("Create fora"); // restore
            try {
                fora = SurveyForum.createTable(SurveyLog.logger, dbUtils.getDBConnection(), this);
            } catch (SQLException e) {
                busted("On Fora startup", e);
                return;
            }
            progress.update(" DB setup complete."); // restore
        } finally {
            progress.close();
        }
    }

    public static final String getThrowableStack(Throwable t) {
        try {
            StringWriter asString = new StringWriter();
            t.printStackTrace(new PrintWriter(asString));
            return asString.toString();
        } catch (Throwable tt) {
            tt.printStackTrace();
            return ("[[unable to get stack: " + tt.toString() + "]]");
        }
    }

    void doShutdownDB() {
        //boolean gotSQLExc = false;

        try {

            closeOpenUserLocaleStuff(true);

            // shut down other connections
            try {
                CookieSession.shutdownDB();
            } catch (Throwable t) {
                t.printStackTrace();
                SurveyLog.logger.warning("While shutting down cookiesession ");
            }
            try {
                if (reg != null)
                    reg.shutdownDB();
            } catch (Throwable t) {
                t.printStackTrace();
                SurveyLog.logger.warning("While shutting down reg ");
            }
            try {
                if (xpt != null)
                    xpt.shutdownDB();
            } catch (Throwable t) {
                t.printStackTrace();
                SurveyLog.logger.warning("While shutting down xpt ");
            }

            if (dbUtils != null) {
                dbUtils.doShutdown();
            }
            dbUtils = null;
        } catch (SQLException se) {
            //gotSQLExc = true;
            SurveyLog.logger.info("DB: while shutting down: " + se.toString());
        }
    }

    private void closeOpenUserLocaleStuff(boolean closeAll) {
        if (allUserLocaleStuffs.isEmpty())
            return;
        SurveyLog.logger.warning("Closing " + allUserLocaleStuffs.size() + " user files.");
        for (UserLocaleStuff uf : allUserLocaleStuffs) {
            if (!uf.isClosed()) {
                uf.internalClose();
            }
        }
    }

    // public static void main(String args[]) {}
    // ====== Utility Functions
    public static final String timeDiff(long a) {
        return timeDiff(a, System.currentTimeMillis());
    }

    public static final String durationDiff(long a) {
        return timeDiff(System.currentTimeMillis() - a);
    }

    public static final String timeDiff(long a, long b) {
        final long ONE_DAY = 86400 * 1000;
        final long A_LONG_TIME = ONE_DAY * 3;
        if ((b - a) > (A_LONG_TIME)) {
            double del = (b - a);
            del /= ONE_DAY;
            int days = (int) del;
            return days + " days";
        } else {
            // round to even second, to avoid ElapsedTimer bug
            a -= (a % 1000);
            b -= (b % 1000);
            return ElapsedTimer.elapsedTime(a, b);
        }
    }

    public static String shortClassName(Object o) {
        try {
            String cls = o.getClass().toString();
            int io = cls.lastIndexOf(".");
            if (io != -1) {
                cls = cls.substring(io + 1, cls.length());
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

    /*
     * public static String bugReplyUrl(String folder, int number, String
     * subject) { return bugFeedbackUrl(subject); }
     */
    public static String bugFeedbackUrl(String subject) {
        return BUG_URL_BASE + "/newticket?component=survey&amp;summary=" + java.net.URLEncoder.encode(subject);
    }

    // ============= Following have to do with "external errors file", an
    // interim error handling mechanism

    /**
     * errors file
     */
    private Hashtable<CLDRLocale, Set<Integer>> externalErrorSet = null;
    private long externalErrorLastMod = -1;
    private long externalErrorLastCheck = -1;
    private boolean externalErrorFailed = false;

    private synchronized boolean externalErrorRead() {
        if (externalErrorFailed)
            return false;
        String externalErrorName = cldrHome + "/" + "count.txt";

        long now = System.currentTimeMillis();

        if ((now - externalErrorLastCheck) < 8000) {
            // SurveyLog.logger.warning("Not rechecking errfile- only been " +
            // (now-externalErrorLastCheck) + " ms");
            if (externalErrorSet != null) {
                return true;
            } else {
                return false;
            }
        }

        externalErrorLastCheck = now;

        try {
            File extFile = new File(externalErrorName);

            if (!extFile.isFile() && !extFile.canRead()) {
                SurveyLog.logger.warning("Can't read counts file: " + externalErrorName);
                externalErrorFailed = true;
                return false;
            }

            long newMod = extFile.lastModified();

            if (newMod == externalErrorLastMod) {
                // SurveyLog.logger.warning("** e.e. file did not change");
                return true;
            }

            // ok, now read it
            BufferedReader in = new BufferedReader(new FileReader(extFile));
            String line;
            int lines = 0;
            Hashtable<CLDRLocale, Set<Integer>> newSet = new Hashtable<CLDRLocale, Set<Integer>>();
            while ((line = in.readLine()) != null) {
                lines++;
                if ((line.length() <= 0) || (line.charAt(0) == '#')) {
                    continue;
                }
                try {
                    String[] result = line.split("\t");
                    CLDRLocale loc = CLDRLocale.getInstance(result[0].split(";")[0]);
                    String what = result[1];
                    String val = result[2];

                    Set<Integer> aSet = newSet.get(loc);
                    if (aSet == null) {
                        aSet = new HashSet<Integer>();
                        newSet.put(loc, aSet);
                    }

                    if (what.equals("path:")) {
                        aSet.add(xpt.getByXpath(val));
                    } else if (what.equals("count:")) {
                        int theirCount = new Integer(val).intValue();
                        if (theirCount != aSet.size()) {
                            SurveyLog.logger.warning(loc + " - count says " + val + ", we got " + aSet.size());
                        }
                    } else {
                        throw new IllegalArgumentException("Unknown parameter: " + what);
                    }
                } catch (Throwable t) {
                    SurveyLog.logger.warning("** " + externalErrorName + ":" + lines + " -  " + t.toString());
                    externalErrorFailed = true;
                    t.printStackTrace();
                    return false;
                }
            }
            SurveyLog.logger.warning(externalErrorName + " - " + lines + " and " + newSet.size() + " locales loaded.");

            externalErrorSet = newSet;
            externalErrorLastMod = newMod;
            externalErrorFailed = false;
            return true;
        } catch (IOException ioe) {
            SurveyLog.logger.warning("Reading externalErrorFile: " + "count.txt - " + ioe.toString());
            ioe.printStackTrace();
            externalErrorFailed = true;
            return false;
        }
    }

    public int externalErrorCount(CLDRLocale loc) {
        synchronized (this) {
            if (externalErrorRead()) {
                Set<Integer> errs = externalErrorSet.get(loc);
                if (errs != null) {
                    return errs.size();
                } else {
                    return 0;
                }
            }
        }
        return -1;
    }

    public String externalErrorUrl(String groupName) {
        return "http://unicode.org/cldr/data/dropbox/gen/errors/" + groupName + ".html";
    }

    // ============= Following have to do with phases

    public static boolean isPhaseSubmit() {
        return phase() == Phase.SUBMIT || phase() == Phase.BETA;
    }

    public static String getSaveButtonText() {
        return (phase() == Phase.BETA) ? "Save [Note: SurveyTool is in BETA]" : "Save Changes";
    }

    public static boolean isPhaseVetting() {
        return phase() == Phase.VETTING;
    }

    public static boolean isPhaseVettingClosed() {
        return phase() == Phase.VETTING_CLOSED;
    }

    public static boolean isPhaseClosed() {
        return (phase() == Phase.CLOSED) || (phase() == Phase.VETTING_CLOSED);
    }

    public static boolean isPhaseDisputed() {
        return phase() == Phase.DISPUTED;
    }

    public static boolean isPhaseFinalTesting() {
        return phase() == Phase.FINAL_TESTING;
    }

    public static boolean isPhaseReadonly() {
        return phase() == Phase.READONLY;
    }

    public static boolean isPhaseBeta() {
        return phase() == Phase.BETA;
    }

    public static final Phase phase() {
        return currentPhase;
    }

    public static String getOldVersion() {
        return oldVersion;
    }

    /**
     * The last version where there was voting. CLDR_LASTVOTEVERSION
     * @return
     */
    public static String getLastVoteVersion() {
        return lastVoteVersion;
    }

    public static String getNewVersion() {
        return newVersion;
    }

    public static String getProposedName() {
        return "Proposed&nbsp;" + getNewVersion();
    }

    /**
     * Return a string, in SQL date format, that describes a day *prior* to the
     * start of vetting for the current release. This can be the last day of the
     * 'beta' phase. Votes on or before this day (midnight, DB server time) will
     * be ignored for statistics/coverage purposes. Defaults to 1970-01-01 if
     * "CLDR_NEWVERSION_AFTER" is not set. Result will be wrapped in either
     * TIMESTAMP("...") or DATETIME("...") as appropriate for the current DB
     * server
     *
     * @return
     */
    public static String getSQLVotesAfter() {
        return DBUtils.DB_SQL_LAST_MOD_TYPE + ("('") + getVotesAfterString() + ("')");
    }

    public static String getVotesAfterString() {
        return CLDRConfig.getInstance().getProperty(SurveyMain.CLDR_NEWVERSION_AFTER, SurveyMain.NEWVERSION_EPOCH);
    }

    public static Date getVotesAfterDate() {
        return new Date(Timestamp.valueOf(getVotesAfterString()).getTime());
    }

    Pattern ALLOWED_EMPTY = PatternCache.get("//ldml/fallback(?![a-zA-Z])");

    // TODO move to central location

    boolean choiceNotEmptyOrAllowedEmpty(String choice_r, String path) {
        return choice_r.length() > 0 || ALLOWED_EMPTY.matcher(path).matches();
    }

    static String xmlescape(String str) {
        if (str.indexOf('&') >= 0) {
            return str.replaceAll("&", "\\&amp;");
        } else {
            return str;
        }
    }

    public static ULocale iso8601Locale = ULocale.forLanguageTag("en-US-POSIX-u-ca-iso8601");

    public static DateFormat getISODateFormat() {
        // return
        // Calendar.getInstance(iso8601Locale).getDateTimeFormat(DateFormat.SHORT,
        // DateFormat.SHORT, iso8601Locale);
        Calendar c = Calendar.getInstance(iso8601Locale);
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        sd.setCalendar(c);
        sd.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sd;
    }

    private static DateFormat myDateFormat = getISODateFormat();

    public synchronized static String formatDate(Date date) {
        return myDateFormat.format(date);
    }

    public static String formatDate() {
        return formatDate(new Date());
    }

    /**
     * @return the fileBaseOld
     */
    static String getFileBaseOld() {
        return fileBaseOld;
    }

    /**
     * @param fileBaseOld
     *            the fileBaseOld to set
     */
    public static void setFileBaseOld(String fileBaseOld) {
        SurveyMain.fileBaseOld = fileBaseOld;
    }

    @Override
    public void readExternal(ObjectInput arg0) throws IOException, ClassNotFoundException {
        STFactory.unimp(); // do not call
    }

    @Override
    public void writeExternal(ObjectOutput arg0) throws IOException {
        STFactory.unimp(); // do not call
    }

    /**
     * Format and display the system's default timezone.
     * @return
     */
    public static String defaultTimezoneInfo() {
        return new SimpleDateFormat("VVVV: ZZZZ", SurveyMain.BASELINE_LOCALE).format(System.currentTimeMillis());
    }

    private static CLDRFile gEnglishFile = null;

    /**
     * Get exactly the "en" disk file.
     * @see #getBaselineFile()
     * @return
     */
    public CLDRFile getEnglishFile() {
        if (gEnglishFile == null) synchronized (this) {
            CLDRFile english = getDiskFactory().make(ULocale.ENGLISH.getBaseName(), true);
            english.setSupplementalDirectory(getSupplementalDirectory());
            english.freeze();
            gEnglishFile = english;
        }
        return gEnglishFile;
    }
}
