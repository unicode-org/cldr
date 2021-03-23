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
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.test.HelpMessages;
import org.unicode.cldr.util.CLDRCacheDir;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfigImpl;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRLocale.CLDRFormatter;
import org.unicode.cldr.util.CLDRLocale.FormatBehavior;
import org.unicode.cldr.util.CLDRURLS;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CoverageInfo;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Factory.DirectoryType;
import org.unicode.cldr.util.Factory.SourceTreeType;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SurveyToolStatus;
import org.unicode.cldr.util.SandboxLocales;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.SpecialLocales;
import org.unicode.cldr.util.SpecialLocales.Type;
import org.unicode.cldr.util.StackTracker;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.web.UserRegistry.User;
import org.unicode.cldr.web.WebContext.HTMLDirection;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

/**
 * The main servlet class of Survey Tool
 */
public class SurveyMain extends HttpServlet implements CLDRProgressIndicator, Externalizable {

    private static final String VURL_LOCALES = "v#locales///";
    private static final String CLDR_OLDVERSION = "CLDR_OLDVERSION";
    private static final String CLDR_NEWVERSION = "CLDR_NEWVERSION";
    private static final String CLDR_LASTVOTEVERSION = "CLDR_LASTVOTEVERSION";
    public static final String CLDR_DIR = "CLDR_DIR"; // accessed by cldr-setup.jsp

    private static final String NEWVERSION_EPOCH = "1970-01-01 00:00:00";

    private static final String CLDR_NEWVERSION_AFTER = "CLDR_NEWVERSION_AFTER";

    public static Stamp surveyRunningStamp = Stamp.getInstance();

    public static final String QUERY_SAVE_COOKIE = "save_cookie";

    /**
     * The "r_" prefix is for r_vetting_json.jsp (Dashboard);
     * also "r_datetime", "r_zones", and "r_compact" -- see ReportMenu.
     */
    private static final String REPORT_PREFIX = "r_";

    /**
     * r_vetting_json.jsp is for the Dashboard
     */
    public static final String R_VETTING_JSON = REPORT_PREFIX + "vetting_json"; // r_vetting_json

    private static final String XML_CACHE_PROPERTIES = "xmlCache.properties";
    private static UnicodeSet supportedNameSet = new UnicodeSet("[a-zA-Z]").freeze();
    static final int TWELVE_WEEKS = 3600 * 24 * 7 * 12;

    public static final String DEFAULT_CONTENT_LINK = "<i><a target='CLDR-ST-DOCS' href='http://cldr.unicode.org/translation/default-content'>default content locale</a></i>";

    private static final long serialVersionUID = -3587451989643792204L;

    /**
     * This class enumerates the current phase of the survey tool.
     * It is distinct from CheckCLDR.Phase, though related.
     *
     * @author srl
     */
    public enum Phase {
        SUBMIT("Data Submission", CheckCLDR.Phase.SUBMISSION),
        VETTING("Vetting", CheckCLDR.Phase.VETTING),
        /*
         * VETTING_CLOSED means, "closed after vetting, while still open for admin tasks"
         */
        VETTING_CLOSED("Vetting Closed", CheckCLDR.Phase.FINAL_TESTING),
        CLOSED("Closed", CheckCLDR.Phase.FINAL_TESTING),
        DISPUTED("Dispute Resolution", CheckCLDR.Phase.VETTING),
        FINAL_TESTING("Final Testing", CheckCLDR.Phase.FINAL_TESTING),
        READONLY("Read-Only", CheckCLDR.Phase.FINAL_TESTING),
        BETA("Beta", CheckCLDR.Phase.SUBMISSION);

        private String what;
        private CheckCLDR.Phase cphase;

        private Phase(String s, CheckCLDR.Phase ph) {
            what = s;
            this.cphase = ph;
        }

        @Override
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
    }

    public enum ReportMenu {
        PRIORITY_ITEMS("Dashboard", SurveyMain.R_VETTING_JSON), DATE_TIME("Date/Time", "r_datetime"), ZONES("Zones", "r_zones"), NUMBERS("Numbers",
            "r_compact");

        private String display;
        private String url;

        private ReportMenu(String d, String u) {
            display = d;
            url = u;
        }

        public String urlStub() {
            return url;
        }

        public String urlQuery() {
            return SurveyMain.QUERY_SECTION + "=" + url;
        }

        /**
         *
         * @param base
         * @param locale
         * @return
         *
         * Called from menu_top.jsp only
         */
        public String urlFull(String base, String locale) {
            return base + "?_=" + locale + "&" + urlQuery();
        }

        public String display() {
            return display;
        }
    }

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
    public static String isBusted = null;
    private static String isBustedStack = null;
    private static ElapsedTimer isBustedTimer = null;
    private static ServletConfig config = null;
    private static OperatingSystemMXBean osmxbean = ManagementFactory.getOperatingSystemMXBean();

    // ===== Special bug numbers.
    private static final String URL_HOST = "http://www.unicode.org/";
    public static final String URL_CLDR = URL_HOST + "cldr/";

    /*
     * TODO: CLDR no longer uses trac; change BUG_URL_BASE to link to github instead
     */
    public static final String BUG_URL_BASE = URL_CLDR + "trac";

    public static final String GENERAL_HELP_URL = "https://sites.google.com/site/cldr/translation";
    public static final String GENERAL_HELP_NAME = "Instructions";

    public static final String ADMIN_HELP_URL = "http://cldr.unicode.org/index/survey-tool/admin";

    // ===== url prefix for help
    public static final String CLDR_HELP_LINK = URL_CLDR + "survey_tool.html" + "#";

    // ===== Hash keys and field values
    public static final String PROPOSED_DRAFT = "proposed-draft";

    /**
     *
     * @param ctx
     * @return
     *
     * Called from st_top.jsp, and locally
     */
    public static String modifyThing(WebContext ctx) {
        return "&nbsp;" + ctx.modifyThing("You are allowed to modify this locale.");
    }

    // ========= SYSTEM PROPERTIES
    public static String vap = System.getProperty("CLDR_VAP"); // Vet Access Password
    public static String testpw = System.getProperty("CLDR_TESTPW"); // Vet Access Password
    private static String vetdata = System.getProperty("CLDR_VET_DATA"); // dir for vetted data
    private File _vetdir = null;

    /**
     * @deprecated use CLDRURLS
     */
    @Deprecated
    private String defaultBase = CLDRURLS.DEFAULT_BASE + "/survey"; /* base URL */
    public static String fileBase = null; // not static - may change later.
    // Common dir
    public static String fileBaseSeed = null; // not static - may change later.

    private static String specialMessage = System.getProperty("CLDR_MESSAGE"); // static
    // - may
    // change
    // later
    private static String lockOut = System.getProperty("CLDR_LOCKOUT"); // static
    // - may
    // change
    // later
    private static long specialTimer = 0; // 0 means off. Nonzero: expiry time of
    // countdown.

    // ======= query fields
    public static final String QUERY_PASSWORD = "pw";
    public static final String QUERY_PASSWORD_ALT = "uid";
    public static final String QUERY_EMAIL = "email";
    public static final String QUERY_SESSION = "s";
    public static final String QUERY_LOCALE = "_";
    public static final String QUERY_SECTION = "x";
    private static final String QUERY_EXAMPLE = "e";
    public static final String QUERY_DO = "do";

    static final String SURVEYTOOL_COOKIE_SESSION = CookieSession.class.getPackage().getName() + ".id";
    static final String SURVEYTOOL_COOKIE_NONE = "0";
    static final String PREF_SORTMODE = "p_sort";
    static final String PREF_NOPOPUPS = "p_nopopups";
    static final String PREF_CODES_PER_PAGE = "p_pager";
    static final String PREF_SORTMODE_CODE = "code";
    static final String PREF_SORTMODE_CODE_CALENDAR = "codecal";
    static final String PREF_SORTMODE_METAZONE = "metazon";
    static final String PREF_SORTMODE_WARNING = "interest";
    static final String PREF_SORTMODE_NAME = "name";
    static final String PREF_SORTMODE_DEFAULT = PREF_SORTMODE_CODE;
    public static final String PREF_NOJAVASCRIPT = "p_nojavascript";
    public static final String PREF_DEBUGJSP = "p_debugjsp"; // debug JSPs?
    public static final String PREF_COVLEV = "p_covlev"; // covlev

    static final String TRANS_HINT_ID = "en_ZZ"; // Needs to be en_ZZ as per cldrbug #2918; must match TRANS_HINT_ID in JavaScript
    public static final ULocale TRANS_HINT_LOCALE = new ULocale(TRANS_HINT_ID);
    // TRANS_HINT_LANGUAGE_NAME needs to match TRANS_HINT_LANGUAGE_NAME in JavaScript ("English")
    public static final String TRANS_HINT_LANGUAGE_NAME = TRANS_HINT_LOCALE.getDisplayLanguage(TRANS_HINT_LOCALE); // Note:
    // Only
    // shows
    // language.

    // ========== lengths
    /**
     * @see WebContext#prefCodesPerPage()
     */
    static final int CODES_PER_PAGE = 1024; // This is only a default.

    public static String xMAIN = "general";

    public static final String SHOWHIDE_SCRIPT = "<script><!-- \n"
        + "function show(what)\n"
        + "{document.getElementById(what).style.display=\"block\";\ndocument.getElementById(\"h_\"+what).style.display=\"none\";}\n"
        + "function hide(what)\n"
        + "{document.getElementById(what).style.display=\"none\";\ndocument.getElementById(\"h_\"+what).style.display=\"block\";}\n"
        + "--></script>";

    private static HelpMessages surveyToolSystemMessages = null;
    private static String CLDR_SURVEYTOOL_HASH = null;

    private static String sysmsg(String msg) {
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
     * Initialize servlet. Will fail (with null) if startup was not attempted.
     *
     * @param req
     * @return the SurveyMain instance
     * @see {@link #triedToStartUp()
     */
    public static SurveyMain getInstance(HttpServletRequest req) {
        if (config == null) {
            return null; // not initialized.
        }
        return (SurveyMain) config.getServletContext().getAttribute(SurveyMain.class.getName());
    }

    private void setInstance(HttpServletRequest req) {
        config.getServletContext().setAttribute(SurveyMain.class.getName(), this);
    }

    private static boolean initCalled = false;
    /**
     * Was init() called on the servlet? This is called very early in server startup,
     * but should be noted here.
     * @see {@link #init(ServletConfig)
     * @return
     */
    public static boolean wasInitCalled() {
        return initCalled;
    }
    private static boolean didTryToStartUp = false;
    /**
     * Did SurveyMain try to startup? Need to call GET /cldr-apps/survey for this to happen
     * if GET has not been called, then we don't have a SurveyMain instance yet and getInstance() will fail.
     * @see {@link #ensureStartup(HttpServletRequest, HttpServletResponse)}
     * @see {@link #getInstance(HttpServletRequest)n
     * @return
     */
    public static boolean triedToStartUp() {
        return didTryToStartUp;
    }

    /**
     * This function overrides GenericServlet.init.
     * Called by StandardWrapper.initServlet automatically.
     * Never called for cldr-apps TestAll.java.
     */
    @Override
    public final void init(final ServletConfig config) throws ServletException {
        initCalled = true;
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
            try(InputStream is = config.getServletContext().getResourceAsStream(JarFile.MANIFEST_NAME)) {
                Manifest mf = new Manifest(is);
                String s = mf.getMainAttributes().getValue("CLDR-Apps"+"-Git-Commit");
                if(s != null && !s.isEmpty()) {
                    SurveyMain.CLDR_SURVEYTOOL_HASH  = s;
                    ((CLDRConfigImpl)cconfig).setCldrAppsHash(s);
                    System.err.println("Updated CLDR_APPS_HASH to " + s);
                } else {
                    System.err.println("CLDR_APPS_HASH = unknown (no value in manifest)");
                }
            } catch(Throwable t) {
                System.err.println("CLDR_APPS_HASH = unknown - " + t.toString());
            }
            isConfigSetup = true; // we have a CLDRConfig - so config is setup.

            stopIfMaintenance();

            cconfig.getSupplementalDataInfo(); // will fail if CLDR_DIR is broken.

            // make sure cldr-tools is functioning
            PathHeader.PageId.forString(PathHeader.PageId.Africa.name());

            SurveyThreadManager.getExecutorService().submit(() -> doStartup());
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
    }

    public SurveyMain() {
        super();
        CookieSession.sm = this;
    }

    /**
     * output MIME header, build context, and run code..
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }

    public static String defaultServletPath = null;
    /**
     * IP exclusion list
     */
    static Hashtable<String, Object> BAD_IPS = new Hashtable<>();
    public static String fileBaseA;
    public static String fileBaseASeed;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (respondToBogusRequest(request, response)) {
            return;
        }
        CLDRConfigImpl.setUrls(request);

        if (!ensureStartup(request, response)) {
            return;
        }

        if (!isBusted()) {
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
        }

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

            if (isUnofficial() && ctx.field("action").equals("new_and_login") &&
                    (ctx.hasTestPassword() || ctx.hasAdminPassword() || requestIsByAdmin(request))) {
                // accessed from createAndLogin.jsp or cldrCreateLogin.js
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
                    String randomPass = UserRegistry.makePassword(null);
                    u.name = newRealName.toString() + "_TESTER_";
                    u.email = newRealName + "." + randomEmail.trim();
                    String newLocales = ctx.field("new_locales").trim();
                    newLocales = UserRegistry.normalizeLocaleList(newLocales);
                    u.locales = newLocales;
                    u.setPassword(randomPass);
                    u.userlevel = ctx.fieldInt("new_userlevel", -1);
                    if (u.userlevel <= 0) {
                        u.userlevel = UserRegistry.LOCKED; // nice try
                    }
                    UserRegistry.User registeredUser = reg.newUser(ctx, u);
                    ctx.println("<i>" + ctx.iconHtml("okay", "added") + "'" + u.name
                        + "'. <br>Email: " + u.email + "  <br>Password: " + u.getPassword() + " <br>userlevel: " + u.getLevel() + "<br>");
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
                        ctx.addCookie(QUERY_PASSWORD, u.getPassword(), TWELVE_WEEKS);
                    } else {
                        WebContext.removeLoginCookies(request, response);
                    }
                    if (autoProceed) {
                        ctx.println("<script>window.setTimeout(function(){document.location = '" + ctx.base() + "/v?email=" + u.email + "&pw=" + u.getPassword()
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

    private boolean requestIsByAdmin(HttpServletRequest request) {
        String sess = request.getParameter(SurveyMain.QUERY_SESSION);
        if (sess != null) {
            CookieSession.checkForExpiredSessions();
            CookieSession mySession = CookieSession.retrieve(sess);
            if (mySession != null && UserRegistry.userIsAdmin(mySession.user)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Avoid wasting time on response, or clogging logs with exceptions, if request is bogus.
     * Respond to bogus requests with SC_NOT_FOUND.
     *
     * "Bogus" (for now) means the request to SurveyMain includes obsolete "x=r_...".
     * Note that the remaining non-bogus requests for "x=r_..." are all to SurveyAjax, not SurveyMain.
     *
     * st.unicode.org receives many
     * requests with "x=r_steps" from web-crawling robots. Sample July 2019 from /var/log/nginx/access.log:
     * "GET /cldr-apps/survey?_=ar_AE&s__=93A...&step=time_formats&x=r_steps HTTP/1.1"
     * 200 5284 "-" "Mozilla/5.0 (compatible; SemrushBot/3~bl; +http://www.semrush.com/bot.html)"
     *
     * Since r_vetting.jsp was removed, we also get bogus requests for "r_vetting.jsp".
     *
     * Reference: https://unicode-org.atlassian.net/browse/CLDR-13135, https://unicode-org.atlassian.net/browse/CLDR-13764
     *
     * @param request the HttpServletRequest
     * @param response the HttpServletResponse
     * @return true if the request is bogus, else false
     *
     * @throws IOException
     */
    private boolean respondToBogusRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String x = request.getParameter("x");
        if (x != null && x.startsWith("r_")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404
            return true;
        }
        return false;
    }

    /**
     * @param request
     * @param response
     * @param out
     * @throws ServletException
     * @throws IOException
     */
    private void showOfflinePage(HttpServletRequest request, HttpServletResponse response, PrintWriter out) throws ServletException, IOException {
        out.println(SHOWHIDE_SCRIPT);
        try {
            SurveyTool.includeJavaScript(request, out);
        } catch (JSONException e) {
            SurveyLog.logException(e, "SurveyMain.showOfflinePage calling SurveyTool.includeJavaScript");
            out.println("<p>Error, including JavaScript failed!</p>");
        }
        // don't flood server if busted- check every minute.
        out.println("<script>timerSpeed = 60080;</script>");
        out.print("<div id='st_err'><!-- for ajax errs --></div><span id='progress'>");
        // This next is for the DB Busted page, so we can show the MySQL configurator.
        out.print("<script src='"+ request.getContextPath()+request.getServletPath() + "/../js/cldr-setup.js" + "'></script>");
        out.print(getTopBox());
        out.println("</span>");
        out.println("<hr>");
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
        didTryToStartUp = true;
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
            boolean jsOK = false;
            try {
                SurveyTool.includeJavaScript(request, out);
                jsOK = true;
            } catch (JSONException e) {
                SurveyLog.logException(e, "SurveyMain.ensureStartup calling SurveyTool.includeJavaScript");
            }
            if (isUnofficial()) {
                out.println("<script>timerSpeed = 2500;</script>");
            } else {
                out.println("<script>timerSpeed = 10000;</script>");
            }
            // todo: include st_top.jsp instead
            out.println("</head><body>");
            if (!jsOK) {
                out.println("<p>Error, including JavaScript failed!</p>");
            }
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
                response.sendRedirect("v#retry");
            }
            out.println("<br><i id='uptime'> " + getGuestsAndUsers() + "</i><br>");
            // TODO: on up, goto <base>

            out.println("<script>loadOnOk = '" + loadOnOk + "';</script>");
            out.println("<script>clickContinue = '" + loadOnOk + "';</script>");
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
            out.print(getCurrev(true));
            out.print("</body></html>");
            return false;
        } else {
            return true;
        }
    }

    /**
     * @return the fileBase
     */
    private static String getFileBase() {
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

    private static SandboxLocales sandbox = null;

    /**
     * Get all of the file bases as an array
     * @return
     */
    private static File[] getFileBases() {
        SandboxLocales sandbox = getSandbox();
        getFileBase(); // load these
        File files[] = {
            new File(getFileBase()),
            new File(getFileBaseSeed()),
            new File(fileBaseA),
            new File(fileBaseASeed),
            sandbox.getMainDir(), // regular sandbox
            sandbox.getAnnotationsDir(), // annotation sandbox
        };
        return files;
    }

    private static SandboxLocales getSandbox() {
        synchronized(SurveyMain.class) {
            if(sandbox == null) {
                try {
                    sandbox = new SandboxLocales(CLDRCacheDir.getInstance(CLDRCacheDir.CacheType.sandbox).getEmptyDir());
                } catch (IOException e) {
                    SurveyMain.busted("Could not initialize sandbox locales", e);
                    /** NOTREACHED **/
                    throw new RuntimeException(e);
                }
            }
            return sandbox;
        }
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
    private static String getFileBaseSeed() {
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
        ctx.println("<script>timerSpeed = 6000;</script>");
        String q = ctx.field("q");
        boolean tblsel = false;
        printAdminMenu(ctx);
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

                conn = dbUtils.getDBConnection();
                s = conn.createStatement();
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
                                        ctx.println(Integer.toHexString((b) & 0xFF));
                                    }
                                } else {
                                    ctx.println(v);
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
    Hashtable<String, Boolean> twidHash = new Hashtable<>();

    private boolean twidGetBool(String key, boolean defVal) {
        Boolean b = twidHash.get(key);
        if (b == null) {
            return defVal;
        } else {
            return b.booleanValue();
        }
    }

    public void twidPut(String key, boolean val) {
        twidHash.put(key, new Boolean(val));
    }

    /* twiddle: these are params settable at runtime.
     * TODO: clarify, can the params change during a run of Survey Tool? How and when does that happen? */
    private boolean twidBool(String x) {
        return twidBool(x, false);
    }

    private synchronized boolean twidBool(String x, boolean defVal) {
        boolean ret = twidGetBool(x, defVal);
        twidPut(x, ret);
        return ret;
    }

    /**
     * Admin panel
     *
     * @param ctx
     */
    private void printAdminMenu(WebContext ctx) {

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
        ctx.print("<a href=\"" + SurveyMain.ADMIN_HELP_URL + "\">Admin Help</a>");
        ctx.println("</div>");
    }

    /**
     * print the header of the thing
     * @throws IOException
     */
    public void printHeader(WebContext ctx, String title) {
        ctx.includeFragment("st_header.jsp");
        title = UCharacter.toTitleCase(SurveyMain.TRANS_HINT_LOCALE.toLocale(), title, null);

        ctx.println("<META NAME=\"ROBOTS\" CONTENT=\"NOINDEX,NOFOLLOW\"> "); // NO
        // index
        ctx.println("<meta name='robots' content='noindex,nofollow'>");
        ctx.println("<meta name=\"gigabot\" content=\"noindex\">");
        ctx.println("<meta name=\"gigabot\" content=\"noarchive\">");
        ctx.println("<meta name=\"gigabot\" content=\"nofollow\">");
        ctx.println("<link rel='stylesheet' type='text/css' href='" + ctx.context("surveytool.css") + "'>");

        try {
            SurveyTool.includeJavaScript(ctx.request, ctx.out);
        } catch (JSONException | IOException e) {
            SurveyLog.logException(e, "SurveyMain.printHeader calling SurveyTool.includeJavaScript");
        }

        ctx.println("<title>CLDR " + getNewVersion() + " Survey Tool: ");
        if (ctx.getLocale() != null) {
            ctx.print(ctx.getLocale().getDisplayName() + " | ");
        }
        ctx.println(title + "</title>");
        ctx.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
        ctx.put("TITLE", title);
        ctx.includeFragment("st_top.jsp");
        ctx.no_js_warning();
    }

    private String getSpecialHeader() {
        return getSpecialHeader(null);
    }

    private String getSpecialHeader(WebContext ctx) {
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
            out.append(getProgress());
            out.append("</div><br>");
        } else {
            out.append(getProgress());
        }
        return out.toString();
    }

    public String getSpecialHeaderText() {
        String specialHeader = CLDRConfig.getInstance().getProperty("CLDR_HEADER");
        if (specialHeader == null) {
            return "";
        }
        return specialHeader;
    }

    /**
     * Return the entire top 'box' including progress bars, busted notices, etc.
     *
     * @return
     */
    private String getTopBox() {
        StringBuffer out = new StringBuffer();
        if (isBusted != null) {
            out.append("<h1>The CLDR Survey Tool is offline</h1>");
            out.append("<div class='ferrbox'><pre>" + isBusted + "</pre><hr>");
            String stack = SurveyForum.HTMLSafe(isBustedStack).replaceAll("\t", "&nbsp;&nbsp;&nbsp;").replaceAll("\n", "<br>");
            out.append(getShortened(stack));
            out.append("</div><br>");
        }
        if (lockOut != null) {
            out.append("<h1>The CLDR Survey Tool is Locked for Maintenance</h1>");
        }
        out.append(getSpecialHeader());
        return out.toString();
    }

    /**
     * Progress bar width
     */
    public static final int PROGRESS_WID = 100;

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
     * Return the current progress indicator.
     *
     * @return
     */
    public String getProgress() {
        return progressManager.getProgress();
    }

    /**
     * Get the current source revision, as HTML
     * @return
     *
     * Called from jsp files as well as locally
     */
    public static String getCurrev() {
        return getCurrev(true);
    }

    static final Pattern HASH_PATTERN = Pattern.compile("^CLDR_([A-Z]+)_HASH$");
    /**
     * Get the current source revision
     * @param asLink true if HTML, false if plaintext
     * @return
     *
     * Called from jsp files as well as locally
     */
    public static String getCurrev(boolean asLink) {
        JSONObject currev = getCurrevJSON();
        StringBuilder output = new StringBuilder();
        final Set<String> resultSet = new HashSet<>(); // If only one result ,return a single string.

        for(Iterator<String> i = currev.keys(); i.hasNext();) {
            final String k = i.next().toString();
            String v;
            output.append(' ');
            if(asLink) {
                final String friendly = HASH_PATTERN.matcher(k).replaceFirst("$1").toLowerCase();
                output.append("<span title='"+k+"'>"+friendly+"</span>"); // use more friendly name
            } else {
                output.append(k);
            }
            output.append('=');
            try {
                v = currev.getString(k);
                if(asLink) {
                    String link = CLDRURLS.gitHashToLink(v);
                    output.append(link);
                    resultSet.add(link);
                } else {
                    output.append(v);
                    resultSet.add(v);
                }
            } catch (JSONException e) {
                String message = "(exception: " + e.getMessage()+")";
                output.append(message);
                resultSet.add(message);
            }
        }
        // If it is unanimous, return a single string.
        if(resultSet.size() == 1) {
            return resultSet.toArray()[0].toString(); // Return the single result.
        }
        return output.toString();
    }


    /**
     * Get the git hash for cldr-apps, statically.
     * Use this to avoid dependency on a loaded CLDRConfig.
     * @return
     * @deprecated use getCurrevJSON
     */
    @Deprecated
    public static String getCurrevCldrApps() {
        if (CLDR_SURVEYTOOL_HASH == null) {
            CLDR_SURVEYTOOL_HASH = CLDRConfigImpl.getGitHashForSlug("CLDR_APPS_HASH");
        }
        return CLDR_SURVEYTOOL_HASH;
    }

    /**
     * Get the current source revision, as a JSON object
     * This will either be a single string '(unknown)' or '1234568'
     * or, it will include error conditions: '12345678 CLDR_TOOLS_HASH=00bad000'
     * if one component is out of sync.
     * @return
     */
    public static JSONObject getCurrevJSON() {
        final CLDRConfigImpl instance = CLDRConfigImpl.getInstance();
        JSONObject jo = new JSONObject();
        for(final String p : CLDRConfigImpl.ALL_GIT_HASHES) {
            try {
                jo.put(p, instance.getProperty(p, CLDRURLS.UNKNOWN_REVISION));
            } catch (JSONException e) {
            }
        }
        return jo;
    }

    /**
     *
     * @param ctx
     *
     * Called from DisptePageManager.java and generalinfo.jsp
     */
    public void printFooter(WebContext ctx) {
        ctx.includeFragment("st_footer.jsp");
    }

    /**
     *
     * @return
     *
     * Called from jsp files as well as locally
     */
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
        return out.toString();
    }

    /**
     * process the '_' parameter, if present, and set the locale.
     */
    private void setLocale(WebContext ctx) {
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

    /* print a user table without any extra help in it */
    private void printUserTable(WebContext ctx) {
        printUserTableWithHelp(ctx, null, null);
    }

    /**
     *
     * @param ctx
     * @param helpLink
     *
     * Called by DisputePageManager as well as locally
     */
    public void printUserTableWithHelp(WebContext ctx, String helpLink) {
        printUserTableWithHelp(ctx, helpLink, null);
    }

    /**
     * Display information about one more users
     *
     * @param ctx
     * @param helpLink
     * @param helpName
     *
     * Called, for example, when the user chooses "Settings" under "My Account" in the gear menu
     */
    private void printUserTableWithHelp(WebContext ctx, String helpLink, String helpName) {
        ctx.put("helpLink", helpLink);
        ctx.put("helpName", helpName);
        ctx.includeFragment("usermenu.jsp");
    }

    /**
     * Accessed from usermenu.jsp
     */
    public static final String REDO_FIELD_LIST[] = { QUERY_LOCALE, QUERY_SECTION, QUERY_DO, "forum" };

    private void doUDump(WebContext ctx) {
        ctx.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        ctx.println("<users host=\"" + ctx.serverHostport() + "\">");
        String org = null;
        Connection conn = null;
        PreparedStatement ps = null;
        java.sql.ResultSet rs = null;
        try {
            conn = dbUtils.getDBConnection();
            synchronized (reg) {
                ps = reg.list(org, conn);
                rs = ps.executeQuery();
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
            DBUtils.close(conn, ps, rs);
        }
        ctx.println("</users>");
    }

    /**
     * Show a toggleable preference
     *
     * @param ctx
     * @param pref
     *            which preference
     * @param what
     *            description of preference
     *
     * Called from debug_jsp.jspf as well as locally
     */
    public boolean showTogglePref(WebContext ctx, String pref, String what) {
        boolean val = ctx.prefBool(pref);
        WebContext nuCtx = (WebContext) ctx.clone();
        nuCtx.addQuery(pref, !val);
        nuCtx.println("<a href='" + nuCtx.url() + "'>" + what + " is currently ");
        ctx.println(((val) ? "<span class='selected'>On</span>" : "<span style='color: #ddd' class='notselected'>On</span>")
            + "&nbsp;/&nbsp;"
            + ((!val) ? "<span class='selected'>Off</span>" : "<span style='color: #ddd' class='notselected'>Off</span>"));
        ctx.println("</a><br>");
        return val;
    }

    String getListSetting(WebContext ctx, String pref, String[] list, boolean doDef) {
        String defaultVal = doDef ? "default" : list[0];
        String settingsSet = defaultVal; // do NOT persist!>>
        String val = ctx.pref(pref, settingsSet);
        return val;
    }

    String getListSetting(UserSettings settings, String pref, String[] list, boolean doDef) {
        return settings.get(pref, doDef ? "default" : list[0]);
    }

    private static void writeMenu(WebContext jout, String title, String field, String current, String items[], String rec) {
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
        jout.println(title);
        jout.println("<select class='" + (any ? "menutop-active" : "menutop-other") + "' onchange='window.location=this.value;'>");

        if (!any) {
            jout.println("<option selected value=\"\">Change...</option>");
        }
        for (int i = 0; i < items.length; i++) {
            boolean isOptional = (items[i].equals(Level.COMPREHENSIVE.toString()));

            if (isOptional && !SurveyMain.isUnofficial())
                continue;
            WebContext ssc = new WebContext(jout);
            ssc.setQuery(field, items[i]);
            String sty = "";
            if (rec != null && rec.equals(items[i])) {
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

    private void doOptions(WebContext ctx) {
        WebContext subCtx = new WebContext(ctx);
        subCtx.removeQuery(QUERY_DO);
        printHeader(ctx, "Manage");
        printUserTableWithHelp(ctx, "/MyOptions");

        ctx.println("<a href='" + ctx.url() + "'>Locales</a><hr>");
        printRecentLocales(subCtx, ctx);
        ctx.addQuery(QUERY_DO, "options");
        ctx.println("<h2>Manage</h2>");

        ctx.includeFragment("manage.jsp");
        printFooter(ctx);
    }

    /**
     * Do session.
     *
     * @param ctx
     * @throws IOException
     * @throws SurveyException
     *
     * Called only by doGet. Called when user logs in or logs out, also when choose some commands
     * from gear menu, such as "Settings" and "List [org] Users"
     */
    private void doSession(WebContext ctx) throws IOException, SurveyException {
        String which = ctx.field(QUERY_SECTION); // may be empty string ""

        setLocale(ctx);

        ctx.setSessionMessage(null); // ??
        ctx.setSession();

        if (ctx.session == null) {

            printHeader(ctx, "Survey Tool");
            if (ctx.getSessionMessage() == null) {
                ctx.setSessionMessage("Could not create your user session.");
            }
            ctx.println("<p><img src='stop.png' width='16'>" + ctx.getSessionMessage() + "</p>");
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
            // Option wasn't found
            ctx.setSessionMessage("<i id='sessionMessage'>Could not do the action '" + doWhat
                    + "'. You may need to be logged in first.</i>");
        }

        String title = " ";
        PageId pageId = ctx.getPageId();
        if (pageId != null) {
            title = "";
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
        }
        /*
         * TODO: all of this function from here on might be dead code; if dead, delete
         */
        printHeader(ctx, title);
        String s = ctx.getSessionMessage();
        if (s != null) {
            ctx.println(s);
        }

        WebContext baseContext = (WebContext) ctx.clone();

        // Don't spin up a factory here.

        // print 'shopping cart'
        if (!shortHeader(ctx)) {

            if ((which.length() == 0) && (ctx.getLocale() != null) || (pageId == null && !which.startsWith(REPORT_PREFIX))) {
                /*
                 * unrecognized page id
                 */
                which = xMAIN;
            }
            printUserTable(ctx);
            printRecentLocales(baseContext, ctx);
        }

        /*
         * Don't show these warnings for example pages.
         */
        if ((ctx.getLocale() != null) && (!shortHeader(ctx))) {
            CLDRLocale aliasTarget = isLocaleAliased(ctx.getLocale());
            if (aliasTarget != null) {
                /*
                 * The alias might be a default content locale. Save some clicks here.
                 */
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
        return ctx.hasField(QUERY_EXAMPLE);
    }

    private LocaleTree localeTree = null;

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
    private CLDRFormatter setDefaultCLDRLocaleFormatter() {
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
            final Set<CLDRLocale> set = new HashSet<>();
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

    private Map<CLDRLocale, Set<CLDRLocale>> relatedLocales = new HashMap<>();

    /**
     *
     * @param localeName
     * @param str
     * @param explanation
     * @return
     *
     * Called from st_top.jsp and locally
     */
    public static String decoratedLocaleName(CLDRLocale localeName, String str, String explanation) {
        String rv = "";
        if (explanation.length() > 0) {
            rv = rv + ("<span title='" + explanation + "'>");
        }
        rv = rv + (str);
        if (explanation.length() > 0) {
            rv = rv + ("</span>");
        }
        return rv;
    }

    private String getLocaleLink(WebContext ctx, String locale, String n) {
        return getLocaleLink(ctx, CLDRLocale.getInstance(locale), n);
    }

    /**
     *
     * @param ctx
     * @param locale
     * @param n
     * @return
     *
     * Called from generalinfo.jsp and locally
     */
    public String getLocaleLink(WebContext ctx, CLDRLocale locale, String n) {
        if (n == null) {
            n = locale.getDisplayName();
        }
        boolean isDefaultContent = getSupplementalDataInfo().isDefaultContent(locale);
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

    /**
     *
     * @param ctx
     * @param baseContext
     * @param which
     * @param whyBad
     *
     * Called by doSession -- but possibly never-reached dead code?
     */
    private void doLocale(WebContext ctx, WebContext baseContext, String which, String whyBad) {
        String locale = null;
        if (ctx.getLocale() != null) {
            locale = ctx.getLocale().toString();
        }
        if ((locale == null) || (locale.length() <= 0)) {
            ctx.println("<i>Loading locale list...</i>");
            ctx.flush();
            ctx.redirectToVurl(ctx.context(VURL_LOCALES)); // may blink.
            return;
        } else {
            showLocale(ctx, which, whyBad);
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
     * @param title
     *            the Title of this menu
     * @param key
     *            the URL field to use (such as 'x')
     *
     * Called by doList and WebContext.showCoverageLevel, and from jsp
     */
    public static void printMenu(WebContext ctx, String which, String menu, String title, String key) {
        ctx.print(getMenu(ctx, which, menu, title, key));
    }

    /**
     *
     * @param ctx
     * @param which
     * @param menu
     * @param title
     * @param key
     * @return
     *
     * Called by printMenu above; and from menu.tag
     */
    public static String getMenu(WebContext ctx, String which, String menu, String title, String key) {
        StringBuffer buf = new StringBuffer();
        if (menu.equals(which)) {
            buf.append("<b class='selected'>");
        } else {
            buf.append("<a class='notselected' href=\"" + ctx.url() + ctx.urlConnector() + key + "=" + menu
                + "\">");
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
            whySent = "Your CLDR vetting account information is being sent to you\n\n";
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

    public static final String CHECKCLDR = "CheckCLDR_"; // key for CheckCLDR objects by locale
    public static final String CHECKCLDR_RES = "CheckCLDR_RES_"; // key for CheckCLDR objects by locale

    /**
     *
     * @param ctx
     * @param which
     *
     * TODO: is this dead/unreachable? Called only by showLocale
     */
    private void printLocaleTreeMenu(WebContext ctx, String which) {

        WebContext subCtx = (WebContext) ctx.clone();
        subCtx.addQuery(QUERY_LOCALE, ctx.getLocale().toString());

        ctx.println("<div id='sectionmenu'>");

        boolean canModify = UserRegistry.userCanModifyLocale(subCtx.session.user, subCtx.getLocale());
        subCtx.put("which", which);
        subCtx.put(WebContext.CAN_MODIFY, canModify);
        subCtx.includeFragment("menu_top.jsp"); // ' code lists .. ' etc
        subCtx.println("</div>");
    }

    /**
     * show the actual locale data..
     *
     * @param ctx
     *            context
     * @param which
     *            value of 'x' parameter.
     *
     * Called by doLocale -- but possibly never-reached dead code?
     */
    private void showLocale(WebContext ctx, String which, String whyBad) {
        PageId pageId = ctx.getPageId();
        synchronized (ctx.session) {
            // Set up checks
            if (ctx.hasField(QUERY_EXAMPLE)) {
                ctx.println("<h3>" + ctx.getLocale() + " " + ctx.getLocale().getDisplayName() + " / " + which + " Example</h3>");
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

            if (pageId != null && !which.equals(xMAIN)) {
                showPathList(subCtx, which, pageId);
            } else {
                which = xMAIN;
                doMain(subCtx); // TODO: does this ever happen? Or is doMain effectively dead code?
            }
        }
    }

    /**
     * @param localeName
     * @return
     */
    private CLDRLocale fileNameToLocale(String localeName) {
        String theLocale;
        int dot = localeName.indexOf('.');
        theLocale = localeName.substring(0, dot);
        return CLDRLocale.getInstance(theLocale);
    }

    /**
     * Show the 'main info about this locale' (General) panel.
     */
    private void doMain(WebContext ctx) {
        ctx.includeFragment("generalinfo.jsp");
    }

    private static CLDRFile gTranslationHintsFile = null;
    private static ExampleGenerator gTranslationHintsExample = null;

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
            ensureOrCheckout(null, "CLDR_DIR", config.getCldrBaseDirectory(), CLDRURLS.CLDR_REPO_ROOT);
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

    private void ensureOrCheckout(JspWriter o, final String param, final File dir, final String url) {
        if (dir == null) {
            busted("Configuration Error: " + param + " is not set.");
        } else if (!dir.isDirectory()) {
            if (o == null) {
                busted("Not able to checkout " + dir.getAbsolutePath() + " for " + param + " - go into setup mode.");
                return; /* NOTREACHED */
            }
            throw new InternalError("Please checkout " + url + " " + dir.getAbsolutePath()
                + "' - and restart the server. TODO- this will be fixed by the step-by-step install.");
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

    /**
     * This is the TRANSLATION HINTS FILE (en_ZZ) - thus it contains 'translation hints'.
     * @see {@link #TRANS_HINT_ID}
     * @see {@link #getEnglishFile()}
     * @return
     */
    public synchronized CLDRFile getTranslationHintsFile() {
        if (gTranslationHintsFile == null) {
            try {
                CLDRFile file = getDiskFactory().make(TRANS_HINT_LOCALE.toString(), true);
                file.setSupplementalDirectory(getSupplementalDirectory()); // so the icuServiceBuilder doesn't blow up.
                file.freeze(); // so it can be shared.
                gTranslationHintsFile = file;

                // propagate it.
                CheckCLDR.setDisplayInformation(gTranslationHintsFile);
                setDefaultCLDRLocaleFormatter();
            } catch (Throwable t) {
                busted("Could not load translation hints locale " + TRANS_HINT_LOCALE, t);
            }
        }
        return gTranslationHintsFile;
    }

    private Set<UserLocaleStuff> allUserLocaleStuffs = new HashSet<>();

    public static final String QUERY_VALUE_SUFFIX = "_v";

    /**
     *
     * @return
     *
     * Called by DataSection.DataRow.toJSONString, and from helpHtml.jsp, and locally by doStartup
     */
    public synchronized ExampleGenerator getTranslationHintsExample() {
        if (gTranslationHintsExample == null) {
            CLDRFile translationHintsFile = getTranslationHintsFile();
            gTranslationHintsExample = new ExampleGenerator(translationHintsFile, translationHintsFile, fileBase + "/../supplemental/");
        }
        /*
         * TODO: to improve performance, move the following line inside the above "if" block, or explain why that can't be done.
         * Why would we need to check this more than once? Can the return value of twidBool change during a run of Survey Tool?
         */
        gTranslationHintsExample.setVerboseErrors(twidBool("ExampleGenerator.setVerboseErrors"));
        return gTranslationHintsExample;
    }

    public synchronized WebContext.HTMLDirection getHTMLDirectionFor(CLDRLocale locale) {
        String dir = getDirectionalityFor(locale);
        return HTMLDirection.fromCldr(dir);
    }

    private synchronized String getDirectionalityFor(CLDRLocale id) {
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
        checkCldr = CheckCLDR.getCheckAll(getSTFactory(), "(?!.*(CheckCoverage).*).*");

        CheckCLDR.setDisplayInformation(getTranslationHintsFile());

        return checkCldr;
    }

    /**
     * Any user of this should be within session sync.
     *
     * @author srl
     *
     */
    public class UserLocaleStuff implements AutoCloseable{
        public CLDRFile cldrfile = null;
        public XMLSource dbSource = null;
        public XMLSource resolvedSource = null;
        public Hashtable<String, Object> hash = new Hashtable<>();
        private int use;
        CLDRFile resolvedFile = null;
        CLDRFile translationHintsFile;

        public void open() {
            use++;
            if (SurveyLog.isDebug())
                SurveyLog.logger.warning("uls: open=" + use);
        }

        private String closeStack = null;

        @Override
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
            synchronized (allUserLocaleStuffs) {
                allUserLocaleStuffs.add(this);
            }

            // TODO: refactor.
            if (cldrfile == null) {
                resolvedSource = getSTFactory().makeSource(locale.getBaseName(), true);
                dbSource = resolvedSource.getUnresolving();
                cldrfile = getSTFactory().make(locale, true).setSupplementalDirectory(getSupplementalDirectory());
                resolvedFile = cldrfile;
                translationHintsFile = getTranslationHintsFile();
            }
        }

        public void clear() {
            hash.clear();
            // TODO: try just kicking these instead of clearing?
            cldrfile = null;
            dbSource = null;
            hash.clear();
        }
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

        Hashtable<CLDRLocale, CLDRLocale> aliasMapNew = new Hashtable<>();
        Hashtable<CLDRLocale, String> directionMapNew = new Hashtable<>();
        Set<CLDRLocale> locales = getLocalesSet();
        ElapsedTimer et = new ElapsedTimer();
        CLDRProgressTask progress = openProgress("Parse locales from XML", locales.size());
        try {
            File xmlCacheDir = CLDRCacheDir.getInstance(CLDRCacheDir.CacheType.xmlCache).getEmptyDir();
            File xmlCache = new File(xmlCacheDir, XML_CACHE_PROPERTIES);
            File xmlCacheBack = new File(xmlCacheDir, XML_CACHE_PROPERTIES + ".backup");
            Properties xmlCacheProps = new java.util.Properties();
            Properties xmlCachePropsNew = new java.util.Properties();
            if (useCache && xmlCache.exists()) {
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
            }

            int n = 0;
            int cachehit = 0;
            SurveyLog.logger.warning("Parse " + locales.size() + " locales from XML to look for aliases or errors...");

            Set<CLDRLocale> failedSuppTest = new TreeSet<>();

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
                progress.update(n++, loc.toString());
                try {
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

            if (useCache) {
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

    public Set<String> getMetazones(String subclass) {
        Set<String> subSet = new TreeSet<>();
        SupplementalDataInfo supplementalDataInfo = getSupplementalDataInfo();
        for (String zone : supplementalDataInfo.getAllMetazones()) {
            if (subclass.equals(supplementalDataInfo.getMetazoneToContinentMap().get(zone))) {
                subSet.add(zone);
            }
        }
        return subSet;
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

    /**
     *
     * @param ctx
     * @param xpath
     * @param pageId
     *
     * Called only by showLocale
     */
    private void showPathList(WebContext ctx, String xpath, PageId pageId) {
        // use the pageid as the xpath
        showPathList(ctx, pageId.name(), null, false);
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

    private static int pages = 0;
    private static int xpages = 0;

    /**
     * Main setup flag. Should use startupFuture, but it is called by many JSPs.
     * @deprecated see {@link #startupFuture}
     */
    @Deprecated
    public static boolean isSetup = false;

    /**
     * Class to startup ST in background and perform background operations.
     */
    public transient SurveyThreadManager startupThread = new SurveyThreadManager();

    /**
     * Progress bar manager
     */
    private SurveyProgressManager progressManager = new SurveyProgressManager();

    private String cldrHome;

    /**
     * Startup function. Called in a separate thread.
     *
     * @throws ServletException
     */
    private void doStartup() {
        ElapsedTimer setupTime = new ElapsedTimer();
        CLDRProgressTask progress = openProgress("Main Startup");
        try {
            // set up CheckCLDR

            progress.update("Initializing Properties");

            CLDRConfig survprops = CLDRConfig.getInstance();

            isConfigSetup = true;

            cldrHome = survprops.getProperty("CLDRHOME");

            System.err.println("CLDRHOME=" + cldrHome + ", maint mode=" + isMaintenance());

            stopIfMaintenance();

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
            System.out.println("Phase: " + phase() + ", cPhase: " + phase().getCPhase() + ", " + getCurrevCldrApps());
            progress.update("Setup props..");
            newVersion = survprops.getProperty(CLDR_NEWVERSION, CLDR_NEWVERSION);
            oldVersion = survprops.getProperty(CLDR_OLDVERSION, CLDR_OLDVERSION);
            lastVoteVersion = survprops.getProperty(CLDR_LASTVOTEVERSION, oldVersion);

            setupHomeDir(progress);

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

            getFileBase();
            getFileBaseSeed();

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
            progress.update("Setup supplemental..");
            verifySupplementalData();

            progress.update("Checking if startup completed..");

            if (isBusted != null) {
                return; // couldn't write the log
            }
            if ((specialMessage != null) && (specialMessage.length() > 0)) {
                SurveyLog.logger.warning("SurveyTool with CLDR_MESSAGE: " + specialMessage);
                busted("message: " + specialMessage);
            }
            progress.update("Setup warnings..");
            if (!readWarnings()) {
                // already busted
                return;
            }

            progress.update("Setup translation-hints file..");

            // load translation-hints file
            getTranslationHintsFile();

            progress.update("Setup translation-hints example..");

            // and example
            getTranslationHintsExample();

            progress.update("Wake up the database..");

            doStartupDB(); // will take over progress 50-60

            progress.update("Making your Survey Tool happy..");

            if (isBusted == null) {
                MailSender.getInstance();
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
                .info("Phase: " + cconfig.getPhase() + " " + getNewVersion() + ",  environment: " + cconfig.getEnvironment() + " " + getCurrev(false));
        }
        if (!isBusted()) {
            SurveyLog.logger.info("------- SurveyTool ready for requests after " + setupTime + "/" + uptime + ". Memory in use: " + usedK()
                + "----------------------------\n\n\n");
            // TODO: use a Future instead
            isSetup = true;
        } else {
            SurveyLog.logger.info("------- SurveyTool FAILED TO STARTUP, " + setupTime + "/" + uptime + ". Memory in use: " + usedK()
                + "----------------------------\n\n\n");
        }
    }

    /*
     * Make sure that the singleton SupplementalDataInfo
     * gets constructed and is functional.
     */
    private void verifySupplementalData() {
        try {
            getSupplementalDataInfo().getBaseFromDefaultContent(CLDRLocale.getInstance("mt_MT"));
        } catch (InternalError ie) {
            SurveyLog.logger.warning("can't do SupplementalData.defaultContentToParent() - " + ie);
            ie.printStackTrace();
            busted("can't do SupplementalData.defaultContentToParent() - " + ie, ie);
        }
    }

    /**
     * Setup things that are dependent on the CLDR home directory being set.
     * @param progress
     */
    private void setupHomeDir(CLDRProgressTask progress) {
        progress.update("Setup the Home dir..");

        // load abstracts in a separate thread.
        SurveyThreadManager.getExecutorService().submit(
            () -> AbstractCacheManager.getInstance().setup());

        // we could setup the url subtype mapper here, but instead we leave that
        // to be lazily loaded.

        // Ensure that the vetdata/ directory is present.
        progress.update("Setup vetdata/..");
        getVetdir();
    }

    private static void stopIfMaintenance() {
        stopIfMaintenance(null);
    }

    private static void stopIfMaintenance(HttpServletRequest request) {
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

    /**
     *
     * @param request
     * @param maintFile
     * @throws IOException
     *
     * Called from cldr-setup.jsp and locally
     */
    public static synchronized void writeHelperFile(HttpServletRequest request, File maintFile) throws IOException {
        CLDRConfigImpl.getInstance().writeHelperFile(request.getScheme() + "://" + request.getServerName() + ":" +
            request.getServerPort() + request.getContextPath() + "/", maintFile);
    }

    /**
     *
     * @return
     *
     * Called from cldr-setup.jsp and locally
     */
    public static File getHelperFile() {
        File maintFile = new File(getSurveyHome(), "admin.html");
        return maintFile;
    }

    /**
    *
    * @return
    *
    * Called from jsp and locally
    */
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
        }
        return dataDir;
    }

    private File makeDataDir(String kind, CLDRLocale loc) throws IOException {
        File dataDir = makeDataDir(kind); // get the parent dir.

        // rest of this function is just to determine which subdir (common or
        // seed)

        Factory f = getDiskFactory();
        File sourceDir = f.getSourceDirectoryForLocale(loc.getBaseName());

        SourceTreeType sourceType = Factory.getSourceTreeType(sourceDir);
        DirectoryType dirType = Factory.getDirectoryType(sourceDir);
        File subDir = new File(dataDir, sourceType.name());
        if (!subDir.exists()) {
            if (!subDir.mkdirs()) {
                throw new IOException("Couldn't create " + subDir.getAbsolutePath());
            }
        }
        File subSubDir = new File(subDir, dirType.name());
        if (!subSubDir.exists()) {
            if (!subSubDir.mkdirs()) {
                throw new IOException("Couldn't create " + subSubDir.getAbsolutePath());
            }
        }
        return subSubDir;
    }

    /**
     *
     * @param kind
     * @param loc
     * @return
     * @throws IOException
     *
     * Called from output-status.jsp
     */
    public File getDataDir(String kind, CLDRLocale loc) throws IOException {
        return getDataFile(kind, loc).getParentFile();
    }

    private Map<Pair<String, CLDRLocale>, File> dirToFile = new HashMap<>();

    /**
     * Just get the File. Don't write it.
     *
     * @param kind
     * @param loc
     * @return
     * @throws IOException
     */
    public synchronized File getDataFile(String kind, CLDRLocale loc) throws IOException {
        Pair<String, CLDRLocale> k = new Pair<>(kind, loc);
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

    private OutputFileManager outputFileManager = null;

    public synchronized OutputFileManager getOutputFileManager() {
        if (outputFileManager == null) {
            outputFileManager = new OutputFileManager(this);
        }
        return outputFileManager;
    }

    public static boolean isBusted() {
        return (isBusted != null);
    }

    @Override
    public void destroy() {
        ElapsedTimer destroyTimer = new ElapsedTimer("SurveyTool destroy()");
        CLDRProgressTask progress = openProgress("shutting down");
        try {
            SurveyLog.logger.warning("SurveyTool shutting down.. r" + getCurrevCldrApps());
            progress.update("shutting down mail... " + destroyTimer);
            MailSender.shutdown();
            progress.update("shutting down SurveyThreadManager... " + destroyTimer);
            startupThread.shutdown();
            progress.update("Shutting down database..." + destroyTimer);
            doShutdownDB();
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
        initCalled=false;
    }

    private static FileFilter getXmlFileFilter() {
        return new FileFilter() {
            @Override
            public boolean accept(File f) {
                String n = f.getName();
                return (!f.isDirectory() && n.endsWith(".xml") && !n.startsWith(".") && !n.startsWith("supplementalData"));
                // root is implied, will be included elsewhere.
            }
        };
    }

    /**
     * Internal function to get all input files.
     * Most functions should use getLocalesSet, etc.
     * @return
     */
    private static File[] getInFiles() {
        Set<File> s = new HashSet<>();
        for(final File fileBase : getFileBases()) {
            for (File f : getInFiles(fileBase)) {
                s.add(f);
            }
        }
        File arr[] = s.toArray(new File[s.size()]);
        return arr;
    }

    /**
     * Only to be used by getInFiles.
     * @param base
     * @return
     */
    private static File[] getInFiles(File baseDir) {
        // get the list of input XML files
        FileFilter myFilter = getXmlFileFilter();
        return baseDir.listFiles(myFilter);
    }

    protected static CLDRLocale getLocaleOf(String localeName) {
        int dot = localeName.indexOf('.');
        String theLocale = localeName.substring(0, dot);
        return CLDRLocale.getInstance(theLocale);
    }

    private static Set<CLDRLocale> localeListSet = null;
    private static Set<CLDRLocale> roLocales = null;

    protected static STFactory.LocaleMaxSizer localeSizer;

    /**
     * Get the list of locales which are read only for some reason. These won't
     * be generated, and will be shown with a lock symbol.
     *
     * @return
     */
    public static final synchronized Set<CLDRLocale> getReadOnlyLocales() {
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
    public static final synchronized Set<CLDRLocale> getLocalesSet() {
        if (localeListSet == null)
            loadLocalesSet();
        return localeListSet;
    }

    /**
     * Set up the list of open vs read-only locales, and the full set.
     */
    private static synchronized void loadLocalesSet() {
        File inFiles[] = getInFiles();
        int nrInFiles = inFiles.length;
        Set<CLDRLocale> s = new TreeSet<>();
        Set<CLDRLocale> ro = new TreeSet<>();
        Set<CLDRLocale> w = new TreeSet<>();
        STFactory.LocaleMaxSizer lms = new STFactory.LocaleMaxSizer();

        String onlyLocales = CLDRConfig.getInstance().getProperty("CLDR_ONLY_LOCALES", null);
        Set<String> onlySet = null;

        if (onlyLocales != null && !onlyLocales.isEmpty()) {
            onlySet = new TreeSet<>();
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
        localeSizer = lms;
    }

    /**
     * Array of locales - calculated from {@link #getLocalesSet()}
     *
     * @return
     */
    public static CLDRLocale[] getLocales() {
        return getLocalesSet().toArray(new CLDRLocale[0]);
    }

    /**
     * Returns a Map of all interest groups. en -> en, en_US, en_MT, ... fr ->
     * fr, fr_BE, fr_FR, ...
     */
    static Map<CLDRLocale, Set<CLDRLocale>> getIntGroups() {
        // TODO: rewrite as iterator
        CLDRLocale[] locales = getLocales();
        Map<CLDRLocale, Set<CLDRLocale>> h = new HashMap<>();
        for (int i = 0; i < locales.length; i++) {
            CLDRLocale locale = locales[i];
            CLDRLocale group = locale;
            int dash = locale.toString().indexOf('_');
            if (dash != -1) {
                group = CLDRLocale.getInstance(locale.toString().substring(0, dash));
            }
            Set<CLDRLocale> s = h.get(group);
            if (s == null) {
                s = new HashSet<>();
                h.put(group, s);
            }
            s.add(locale);
        }
        return h;
    }

    public boolean isValidLocale(CLDRLocale locale) {
        return getLocalesSet().contains(locale);
    }

    private static int usedK() {
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
    private static void busted(String what, Throwable t, String stack) {
        if (t != null) {
            SurveyLog.logException(t, what /* , ignore stack - fetched from exception */);
        }
        SurveyLog.logger.warning("SurveyTool " + SurveyMain.getCurrevCldrApps() + " busted: " + what + " ( after " + pages + "html+" + xpages
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
            isBustedTimer = new ElapsedTimer();
        } else {
            SurveyLog.warnOnce("[was already busted, not overriding old message.]");
        }
    }

    private static long shortN = 0;
    /**
     * Only used by about.jsp to know whether it's safe to call DBUtils.getInstance()
     */
    public static boolean isDbSetup = false;
    private static final int MAX_CHARS = 100;
    private static final String SHORT_A = "(Click to show entire message.)";
    private static final String SHORT_B = "(hide.)";

    public static final String QUERY_FIELDHASH = "fhash";

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

    private static String getShortened(String shortStr, String longStr, String warnHash) {
        return ("<span id='h_ww" + warnHash + "'>" + shortStr + "... ")
            + ("<a href='javascript:show(\"ww" + warnHash + "\")'>" + SHORT_A + "</a></span>")
            + ("<!-- <noscript>Warning: </noscript> -->" + "<span style='display: none'  id='ww" + warnHash + "'>" + longStr
                + "<a href='javascript:hide(\"ww" + warnHash + "\")'>" + SHORT_B + "</a></span>");
    }

    private Hashtable<String, String> xpathWarnings = new Hashtable<>();

    private boolean readWarnings() {
        try {
            BufferedReader in = FileUtilities.openUTF8Reader(cldrHome, "surveyInfo.txt");
            String line;
            while ((line = in.readLine()) != null) {
                if ((line.length() <= 0) || (line.charAt(0) == '#')) {
                    continue;
                }
                String[] result = line.split("\t");
                xpathWarnings.put(result[0] + " /" + result[1], result[2]);
            }
        } catch (java.io.FileNotFoundException t) {
            return true;
        } catch (java.io.IOException t) {
            SurveyLog.logger.warning(t.toString());
            t.printStackTrace();
            busted("Error: trying to read xpath warnings file.  " + cldrHome + "/surveyInfo.txt");
            return true;
        }
        return true;
    }

    public DBUtils dbUtils = null;

    /**
     * Setup some Database items.
     */
    private void doStartupDB() {
        if (isMaintenance()) {
            throw new InternalError("SurveyTool is in setup mode.");
        }
        CLDRProgressTask progress = openProgress("Database Setup");
        try {
            progress.update("begin.."); // restore
            dbUtils.validateDatasourceExists(this, progress);
            SurveyMain.isDbSetup  = true;
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
            try (Connection conn = dbUtils.getAConnection()) {
                xpt = XPathTable.createTable(conn);
            } catch (SQLException e) {
                busted("On XPathTable startup", e);
                return;
            }

            progress.update("Load XPT");
            System.err.println("XPT ready with " + xpt.statistics());
            xpt.loadXPaths(getDiskFactory().makeSource(TRANS_HINT_ID));
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

    private static final String getThrowableStack(Throwable t) {
        try {
            StringWriter asString = new StringWriter();
            t.printStackTrace(new PrintWriter(asString));
            return asString.toString();
        } catch (Throwable tt) {
            tt.printStackTrace();
            return ("[[unable to get stack: " + tt.toString() + "]]");
        }
    }

    private void doShutdownDB() {
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
            if (dbUtils != null) {
                dbUtils.doShutdown();
            }
            dbUtils = null;
        } catch (SQLException se) {
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

    // ====== Utility Functions

    /**
     *
     * @param a
     * @return
     *
     * Called from AdminAjax.jsp and locally
     */
    public static final String timeDiff(long a) {
        return timeDiff(a, System.currentTimeMillis());
    }

    public static final String durationDiff(long a) {
        return timeDiff(System.currentTimeMillis() - a);
    }

    private static final String timeDiff(long a, long b) {
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

    public static String bugFeedbackUrl(String subject) {
        return BUG_URL_BASE + "/newticket?component=survey&amp;summary=" + java.net.URLEncoder.encode(subject);
    }

    // ============= Following have to do with phases

    public static boolean isPhaseVetting() {
        return phase() == Phase.VETTING;
    }

    public static boolean isPhaseVettingClosed() {
        return phase() == Phase.VETTING_CLOSED;
    }

    public static boolean isPhaseClosed() {
        return (phase() == Phase.CLOSED) || (phase() == Phase.VETTING_CLOSED);
    }

    public static boolean isPhaseReadonly() {
        return phase() == Phase.READONLY;
    }

    public static boolean isPhaseBeta() {
        return phase() == Phase.BETA;
    }

    public static boolean isPhaseFinalTesting() {
        return phase() == Phase.FINAL_TESTING;
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

    public static String getVotesAfterString() {
        return CLDRConfig.getInstance().getProperty(SurveyMain.CLDR_NEWVERSION_AFTER, SurveyMain.NEWVERSION_EPOCH);
    }

    public static Date getVotesAfterDate() {
        return new Date(Timestamp.valueOf(getVotesAfterString()).getTime());
    }

    static String xmlescape(String str) {
        if (str.indexOf('&') >= 0) {
            return str.replaceAll("&", "\\&amp;");
        } else {
            return str;
        }
    }

    @Override
    public void readExternal(ObjectInput arg0) throws IOException, ClassNotFoundException {
        STFactory.unimp(); // do not call
    }

    @Override
    public void writeExternal(ObjectOutput arg0) throws IOException {
        STFactory.unimp(); // do not call
    }

    private static CLDRFile gEnglishFile = null;

    /**
     * Get exactly the "en" disk file.
     * @see #getTranslationHintsFile()
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

    public JSONObject statusJSON(HttpServletRequest request) throws JSONException {
        return new StatusForFrontEnd(this, request).toJSONObject();
    }

    private class StatusForFrontEnd implements JSONString {
        private String contextPath = null;
        private int dbopen = DBUtils.db_number_open;
        private int dbused = DBUtils.db_number_used;
        private int guests = CookieSession.getGuestCount();
        private String isBusted = SurveyMain.isBusted;
        private boolean isPhaseBeta = isPhaseBeta();
        private boolean isSetup = SurveyMain.isSetup;
        private boolean isUnofficial = SurveyMain.isUnofficial();
        private String newVersion = SurveyMain.newVersion;
        private String organizationName = null;
        private int pages = SurveyMain.pages;
        private Object permissions = null;
        private Phase phase = phase();
        private String sessionId = null;
        private String specialHeader = getSpecialHeaderText();
        private long surveyRunningStamp = SurveyMain.surveyRunningStamp.current();
        private double sysload = osmxbean.getSystemLoadAverage();
        private ElapsedTimer uptime = SurveyMain.uptime;
        private User user = null;
        private int users = CookieSession.getUserCount();
        private String sessionMessage = null;

        private JSONObject toJSONObject() throws JSONException {
            /*
             * This is tedious, should be a one-liner?
             * Doc for JSONObject.put(string, object), says: "It [object] should be of one of these types:
             * Boolean, Double, Integer, JSONArray, JSONObject, Long, String, or the JSONObject.NULL object."
             */
            return new JSONObject()
                .put("contextPath", contextPath)
                .put("dbopen", dbopen)
                .put("dbused", dbused)
                .put("guests", guests)
                .put("isBusted", isBusted)
                .put("isPhaseBeta", isPhaseBeta)
                .put("isSetup", isSetup)
                .put("isUnofficial", isUnofficial)
                .put("newVersion", newVersion)
                .put("organizationName", organizationName)
                .put("pages", pages)
                .put("permissions", permissions)
                .put("phase", phase)
                .put("sessionId", sessionId)
                .put("sessionMessage", sessionMessage)
                .put("specialHeader", specialHeader)
                .put("surveyRunningStamp", surveyRunningStamp)
                .put("sysload", sysload)
                .put("uptime", uptime)
                .put("user", user) // allowed since User implements JSONString?
                .put("users", users)
            ;
        }

        @Override
        public String toJSONString() throws JSONException {
            return toJSONObject().toString();
        }

        public StatusForFrontEnd(SurveyMain sm, HttpServletRequest request) throws JSONException {
            this.contextPath = request.getContextPath();
            setSessionIdAndUser(request);
            if (user != null) {
                this.organizationName = user.getOrganization().getDisplayName();
                this.permissions = user.getPermissionsJson();
            }
        }

        private void setSessionIdAndUser(HttpServletRequest request) {
            sessionId = request.getParameter("s");
            if (sessionId == null) {
                HttpSession hsession = request.getSession(false);
                if (hsession != null) {
                    sessionId = hsession.getId();
                }
            }
            CookieSession mySession = null;
            if (sessionId != null) {
                mySession = CookieSession.retrieveWithoutTouch(sessionId);
            }
            if (mySession == null) {
                sessionId = null;
            } else {
                sessionId = mySession.id;
                user = mySession.user;
                sessionMessage = mySession.getMessage();
            }
        }
    }
}
