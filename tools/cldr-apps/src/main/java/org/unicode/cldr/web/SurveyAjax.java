package org.unicode.cldr.web;

import static org.unicode.cldr.web.XPathTable.getStringIDString;

import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.text.UnicodeSet;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckForCopy;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.test.SubmissionLocales;
import org.unicode.cldr.test.TestCache;
import org.unicode.cldr.util.*;
import org.unicode.cldr.util.CLDRInfo.CandidateInfo;
import org.unicode.cldr.util.CLDRInfo.UserInfo;
import org.unicode.cldr.util.DtdData.IllegalByDtdException;
import org.unicode.cldr.util.VoterReportStatus.ReportId;
import org.unicode.cldr.web.BallotBox.InvalidXPathException;
import org.unicode.cldr.web.BallotBox.VoteNotAcceptedException;
import org.unicode.cldr.web.CLDRProgressIndicator.CLDRProgressTask;
import org.unicode.cldr.web.SurveyException.ErrorCode;
import org.unicode.cldr.web.UserRegistry.User;
import org.unicode.cldr.web.WebContext.HTMLDirection;

/**
 * Servlet implementation class SurveyAjax
 *
 * <p>URL/JSON Usage: get/set prefs
 *
 * <p>to get a preference: http://...../SurveyAjax?s=YOURSESSIONID&what=pref&pref=dummy will reply
 * with JSON of {... "pref":"dummy","_v":"true" ...} to set a preference:
 * http://...../SurveyAjax?s=YOURSESSIONID&what=pref&pref=dummy&_v=true ( can also use a POST
 * instead of the _v parameter ) Note, add the preference to the settablePrefsList
 *
 * <p>The @MultipartConfig annotation enables getting request parameters from a POST request that
 * has "multipart/form-data" as its content-type, as needed for WHAT_USER_LIST.
 */
@MultipartConfig
public class SurveyAjax extends HttpServlet {
    static final Logger logger = SurveyLog.forClass(SurveyAjax.class);

    public static final String WHAT_MY_LOCALES = "mylocales";

    private static final long serialVersionUID = 1L;
    public static final String REQ_WHAT = "what";
    public static final String REQ_SESS = "s";
    public static final String WHAT_STATUS = "status";
    public static final String WHAT_GETSIDEWAYS = "getsideways";
    public static final String WHAT_GETXPATH = "getxpath";
    public static final String WHAT_PREF = "pref";
    public static final String WHAT_FORUM_PARTICIPATION = "forum_participation";
    public static final String WHAT_VETTING_PARTICIPATION = "vetting_participation";
    public static final String WHAT_BULK_CLOSE_POSTS = "bulk_close_posts";
    public static final String WHAT_STATS_BYLOC = "stats_byloc";
    public static final String WHAT_STATS_BYDAY = "stats_byday";
    public static final String WHAT_STATS_BYDAYUSERLOC = "stats_bydayuserloc";
    public static final String WHAT_RECENT_ITEMS = "recent_items";
    public static final String WHAT_FORUM_FETCH = "forum_fetch";
    public static final String WHAT_FORUM_COUNT = "forum_count";
    public static final String WHAT_FORUM_POST = "forum_post";
    public static final String WHAT_POSS_PROBLEMS = "possibleProblems";
    public static final String WHAT_GET_MENUS = "menus";
    public static final String WHAT_REPORT = "report";
    public static final String WHAT_SEARCH = "search";
    public static final String WHAT_DASH_HIDE = "dash_hide";
    public static final String WHAT_PARTICIPATING_USERS = "participating_users"; // tc-emaillist.js
    public static final String WHAT_USER_INFO = "user_info"; // usermap.js
    public static final String WHAT_USER_LIST =
            "user_list"; // (old) users.js; (new) cldrListUsers.js
    public static final String WHAT_USER_OLDVOTES = "user_oldvotes"; // users.js
    public static final String WHAT_USER_XFEROLDVOTES = "user_xferoldvotes"; // users.js
    public static final String WHAT_OLDVOTES = "oldvotes"; // cldrLoad.js
    public static final String WHAT_FLAGGED = "flagged"; // cldrLoad.js
    public static final String WHAT_AUTO_IMPORT = "auto_import"; // cldrLoad.js
    public static final String WHAT_ADMIN_PANEL = "admin_panel"; // cldrAdmin.js
    public static final String WHAT_RECENT_ACTIVITY = "recent_activity"; // cldrRecentActivity.js
    public static final String WHAT_ERROR_SUBTYPES = "error_subtypes"; // cldrErrorSubtyes.js

    public static final int oldestVersionForImportingVotes =
            25; // Oldest table is cldr_vote_value_25, as of 2018-05-23.

    String[] settablePrefsList = {SurveyMain.PREF_COVLEV, "dummy"}; // list of prefs OK to get/set

    private final Set<String> prefsList = new HashSet<>();

    /**
     * @see HttpServlet#HttpServlet()
     */
    public SurveyAjax() {
        super();

        Collections.addAll(prefsList, settablePrefsList);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setup(request, response);
        /*
         * TODO: is request.getParameter("value") always sufficient?
         */
        processRequest(request, response, request.getParameter("value"));
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setup(request, response);
        /*
         * TODO: check whether this ...decodeFieldString... is still needed/appropriate; what is QUERY_VALUE_SUFFIX for?
         * processRequest uses val for unrelated (?) purposes WHAT_PREF, WHAT_SEARCH
         * some of which are doPost, and some of which are doGet
         */
        processRequest(
                request,
                response,
                WebContext.decodeFieldString(request.getParameter(SurveyMain.QUERY_VALUE_SUFFIX)));
    }

    private void setup(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
    }

    private void processRequest(
            HttpServletRequest request, HttpServletResponse response, String val)
            throws IOException {
        if (!"UTF-8".equals(request.getCharacterEncoding())) {
            throw new InternalError("Request is not UTF-8 but: " + request.getCharacterEncoding());
        }
        CLDRConfigImpl.setUrls(request);
        final SurveyMain sm = SurveyMain.getInstance();
        PrintWriter out = response.getWriter();
        String what = request.getParameter(REQ_WHAT);
        String sess = request.getParameter(SurveyMain.QUERY_SESSION);
        String rawloc = request.getParameter(SurveyMain.QUERY_LOCALE);
        String xpath = request.getParameter(SurveyForum.F_XPATH);
        CookieSession mySession;

        CLDRLocale l = validateLocale(out, rawloc, sess); // will send error
        if (l == null && (rawloc != null && !rawloc.isEmpty())) {
            return; // validateLocale has already sent an error to the user
        }
        // Always sanitize 'loc'. Do not pass raw to user.
        final String loc = (l == null) ? null : l.toString();
        try {
            if (sm == null) {
                sendNoSurveyMain(out);
            } else if (what == null) {
                sendError(out, "Missing parameter: " + REQ_WHAT, ErrorCode.E_INTERNAL);
            } else if (what.equals(WHAT_REPORT)) {
                generateReport(request, response, out, sm, sess, l);
            } else if (what.equals(WHAT_RECENT_ACTIVITY)) {
                SurveyJSONWrapper r = newJSONStatus(request, sm);
                RecentActivity.getJson(r, request, response);
                send(r, out);
            } else if (what.equals(WHAT_ERROR_SUBTYPES)) {
                SurveyJSONWrapper r = newJSONStatus(request, sm);
                ErrorSubtypes.getJson(r, request);
                send(r, out);
            } else if (what.equals(WHAT_STATS_BYLOC)) {
                SurveyJSONWrapper r = newJSONStatusQuick();
                JSONObject query =
                        DBUtils.queryToCachedJSON(
                                what, 5 * 60 * 1000, StatisticsUtils.QUERY_ALL_VOTES);
                r.put(what, query);
                JSONObject query2 =
                        DBUtils.queryToCachedJSON(
                                what + "_new", 5 * 60 * 1000, StatisticsUtils.QUERY_NEW_VOTES);
                r.put(what + "_new", query2);
                addGeneralStats(r);
                send(r, out);
            } else if (what.equals(WHAT_FLAGGED)) {
                SurveyJSONWrapper r = newJSONStatus(request, sm);
                mySession = CookieSession.retrieve(sess);
                new SurveyFlaggedItems(UserRegistry.userIsTC(mySession.user)).getJson(r);
                send(r, out);
            } else if (what.equals(WHAT_STATS_BYDAYUSERLOC)) {
                String votesAfterString = SurveyMain.getVotesAfterString();
                SurveyJSONWrapper r = newJSONStatus(request, sm);
                final String day =
                        DBUtils.db_Mysql ? "DATE_FORMAT(last_mod, '%Y-%m-%d')" : "last_mod ";
                final String sql =
                        "select submitter,"
                                + day
                                + " as day,locale,count(*) as count from "
                                + DBUtils.Table.VOTE_VALUE
                                + " group by submitter,locale,YEAR(last_mod),MONTH(last_mod),day order by day desc";
                JSONObject query = DBUtils.queryToCachedJSON(what, 5 * 60 * 1000, sql);
                r.put(what, query);
                r.put("after", votesAfterString);
                send(r, out);
                // select submitter,DATE_FORMAT(last_mod, '%Y-%m-%d') as day,locale,count(*) from
                // "+DBUtils.Table.VOTE_VALUE+" group by
                // submitter,locale,YEAR(last_mod),MONTH(last_mod),DAYOFMONTH(last_mod) order by day
                // desc limit 10000;
            } else if (what.equals(WHAT_STATS_BYDAY)) {
                SurveyJSONWrapper r = newJSONStatus(request, sm);
                {
                    final String sql =
                            "select count(*) as count , Date(last_mod) as date from "
                                    + DBUtils.Table.VOTE_VALUE
                                    + " group by date desc";
                    final JSONObject query = DBUtils.queryToCachedJSON(what, (5 * 60 * 1000), sql);
                    r.put("byday", query);
                }
                {
                    // exclude old votes
                    final String sql2 =
                            "select count(*) as count , Date(last_mod) as date from "
                                    + DBUtils.Table.VOTE_VALUE
                                    + " as new_votes where "
                                    + StatisticsUtils.getExcludeOldVotesSql()
                                    + " group by date desc";
                    final JSONObject query2 =
                            DBUtils.queryToCachedJSON(what + "_new", (5 * 60 * 1000), sql2);
                    r.put("byday_new", query2);
                }
                r.put("after", "n/a");
                send(r, out);
            } else if (what.equals(WHAT_GETXPATH)) {
                SurveyJSONWrapper r = newJSONStatus(request, sm);
                try {
                    int xpid;
                    String xpath_path;
                    String xpstrid;
                    if (xpath.startsWith("/")) {
                        xpid = sm.xpt.getByXpath(xpath);
                        xpath_path = xpath;
                        xpstrid = getStringIDString(xpath_path);
                    } else if (xpath.startsWith("#")) {
                        xpid = Integer.parseInt(xpath.substring(1));
                        xpath_path = sm.xpt.getById(xpid);
                        xpstrid = getStringIDString(xpath_path);
                    } else {
                        xpath_path = sm.xpt.getByStringID(xpath);
                        xpid = sm.xpt.getByXpath(xpath_path);
                        xpstrid = xpath;
                    }

                    JSONObject ret = new JSONObject();
                    ret.put("path", xpath_path);
                    ret.put("id", xpid);
                    ret.put("hex", xpstrid);
                    ret.put(
                            "ph",
                            SurveyJSONWrapper.wrap(sm.getSTFactory().getPathHeader(xpath_path)));
                    r.put(what, ret);
                } catch (Throwable t) {
                    sendError(out, t);
                    return;
                }
                send(r, out);
            } else if (what.equals(WHAT_MY_LOCALES)) {
                SurveyJSONWrapper r = newJSONStatus(request, sm);
                String q1 =
                        "select count(*) as count, "
                                + DBUtils.Table.VOTE_VALUE
                                + ".locale as locale from "
                                + DBUtils.Table.VOTE_VALUE
                                + " WHERE "
                                + DBUtils.Table.VOTE_VALUE
                                + ".submitter=? AND "
                                + DBUtils.Table.VOTE_VALUE
                                + ".value is not NULL "
                                + " group by "
                                + DBUtils.Table.VOTE_VALUE
                                + ".locale order by "
                                + DBUtils.Table.VOTE_VALUE
                                + ".locale desc";
                String user = request.getParameter("user");
                UserRegistry.User u = null;
                if (user != null && !user.isEmpty()) {
                    try {
                        u = sm.reg.getInfo(Integer.parseInt(user));
                    } catch (Throwable t) {
                        SurveyLog.logException(logger, t, "Parsing user " + user);
                    }
                }
                /*
                 * Avoid NullPointerException: u can be null if you hand-edit the url, e.g., changing the number in '...myvotes.jsp?user=3...';
                 */
                if (u != null) {
                    JSONObject query = DBUtils.queryToJSON(q1, u.id);
                    r.put("mine", query);
                }
                send(r, out);
            } else if (what.equals(WHAT_RECENT_ITEMS)) {
                SurveyJSONWrapper r = newJSONStatus(request, sm);
                int limit;
                try {
                    limit = Integer.parseInt(request.getParameter("limit"));
                } catch (Throwable t) {
                    limit = 15;
                }
                String q1 =
                        "select "
                                + DBUtils.Table.VOTE_VALUE
                                + ".locale,"
                                + DBUtils.Table.VOTE_VALUE
                                + ".xpath,cldr_users.org, "
                                + DBUtils.Table.VOTE_VALUE
                                + ".value, "
                                + DBUtils.Table.VOTE_VALUE
                                + ".last_mod  from "
                                + DBUtils.Table.VOTE_VALUE
                                + ",cldr_users  where ";
                String q2 =
                        " cldr_users.id="
                                + DBUtils.Table.VOTE_VALUE
                                + ".submitter"
                                + "  order by "
                                + DBUtils.Table.VOTE_VALUE
                                + ".last_mod desc ";
                String user = request.getParameter("user");
                UserRegistry.User u = null;
                if (user != null && !user.isEmpty())
                    try {
                        u = sm.reg.getInfo(Integer.parseInt(user));
                    } catch (Throwable t) {
                        SurveyLog.logException(logger, t, "Parsing user " + user);
                    }
                System.out.println("SQL: " + q1 + q2);
                JSONObject query;
                if (l == null && u == null) {
                    query = DBUtils.queryToJSONLimit(limit, q1 + q2);
                } else if (u == null) {
                    query =
                            DBUtils.queryToJSONLimit(
                                    limit,
                                    q1 + " " + DBUtils.Table.VOTE_VALUE + ".locale=? AND " + q2,
                                    l);
                } else if (l == null) {
                    query =
                            DBUtils.queryToJSONLimit(
                                    limit,
                                    q1 + " " + DBUtils.Table.VOTE_VALUE + ".submitter=? AND " + q2,
                                    u.id);
                } else {
                    query =
                            DBUtils.queryToJSONLimit(
                                    limit,
                                    q1
                                            + " "
                                            + DBUtils.Table.VOTE_VALUE
                                            + ".locale=? AND "
                                            + DBUtils.Table.VOTE_VALUE
                                            + ".submitter=? "
                                            + q2,
                                    l,
                                    u.id);
                }
                r.put("recent", query);
                send(r, out);
            } else if (what.equals(WHAT_STATUS)) {
                SurveyJSONWrapper r2 = newJSONStatus(request, sm);
                setLocaleStatus(sm, loc, r2);

                if (sess != null && !sess.isEmpty()) {
                    CookieSession.checkForExpiredSessions();
                    mySession = CookieSession.retrieve(sess); // or peek?
                    if (mySession != null) {
                        long millisSinceAction =
                                System.currentTimeMillis()
                                        - mySession.getLastActionMillisSinceEpoch();
                        r2.put("millisSinceAction", millisSinceAction);
                        r2.put("millisTillKick", mySession.millisTillKick());
                    } else {
                        r2.put("session_err", "no session");
                    }
                }
                send(r2, out);
            } else if (what.equals(WHAT_DASH_HIDE)) {
                CookieSession.checkForExpiredSessions();
                mySession = CookieSession.retrieve(sess);

                if (mySession == null) {
                    sendError(
                            out,
                            "Missing/Expired Session (idle too long? too many users?): " + sess,
                            ErrorCode.E_SESSION_DISCONNECTED);
                } else {
                    handleDashHideRequest(mySession.user.id, request);
                    mySession.userDidAction();
                }
                send(new SurveyJSONWrapper(), out);
            } else if (sess != null && !sess.isEmpty()) { // this and following:
                // session needed
                CookieSession.checkForExpiredSessions();
                mySession = CookieSession.retrieve(sess);
                if (mySession == null) {
                    sendError(
                            out,
                            "Missing/Expired Session (idle too long? too many users?): " + sess,
                            ErrorCode.E_SESSION_DISCONNECTED);
                } else {
                    if (what.equals(WHAT_ADMIN_PANEL)) {
                        mySession.userDidAction();
                        if (UserRegistry.userIsAdmin(mySession.user)
                                || "create_login".equals(request.getParameter("do"))) {
                            // TODO: temporary exception for createAndLogin
                            // create_login doesn't actually create anything,
                            // just returns metadata.
                            mySession.userDidAction();
                            SurveyJSONWrapper r = newJSONStatus(request, sm);
                            r.put("what", what);
                            new AdminPanel().getJson(r, request, response);
                            send(r, out);
                        } else {
                            sendError(
                                    out,
                                    "Only Admin can access Admin Panel",
                                    ErrorCode.E_NO_PERMISSION);
                        }
                    } else if (what.equals(WHAT_PREF)) {
                        // This is used for setting Coverage level, see cldrMenu.js;
                        // maybe also for PREF_CODES_PER_PAGE but that looks like dead code?
                        mySession.userDidAction();
                        String pref = request.getParameter("pref");

                        SurveyJSONWrapper r = newJSONStatus(request, sm);
                        r.put("pref", pref);

                        if (!prefsList.contains(pref)) {
                            sendError(
                                    out, "Bad or unsupported pref: " + pref, ErrorCode.E_INTERNAL);
                        }

                        if (val != null && !val.isEmpty()) {
                            if (val.equals("null")) {
                                mySession.settings().set(pref, null);
                            } else {
                                mySession.settings().set(pref, val);
                            }
                        }
                        r.put(SurveyMain.QUERY_VALUE_SUFFIX, mySession.settings().get(pref, null));
                        send(r, out);
                    } else if (what.equals(WHAT_FORUM_PARTICIPATION)) {
                        mySession.userDidAction();
                        SurveyJSONWrapper r = newJSONStatus(request, sm);
                        r.put("what", what);
                        if (UserRegistry.userCanMonitorForum(mySession.user)) {
                            String org = mySession.user.org;
                            new SurveyForumParticipation(org).getJson(r);
                        }
                        send(r, out);
                    } else if (what.equals(WHAT_VETTING_PARTICIPATION)) {
                        mySession.userDidAction();
                        SurveyJSONWrapper r = newJSONStatus(request, sm);
                        r.put("what", what);
                        if (UserRegistry.userIsVetter(mySession.user)) {
                            String org = mySession.user.org;
                            if (UserRegistry.userCreateOtherOrgs(mySession.user)) {
                                org = null; // all
                            }
                            new SurveyVettingParticipation(org, sm).getJson(r);
                        }
                        send(r, out);
                    } else if (what.equals(WHAT_BULK_CLOSE_POSTS)) {
                        mySession.userDidAction();
                        SurveyJSONWrapper r = newJSONStatus(request, sm);
                        r.put("what", what);
                        if (UserRegistry.userIsAdmin(mySession.user)) {
                            boolean execute = "true".equals(request.getParameter("execute"));
                            new SurveyBulkClosePosts(sm, execute).getJson(r);
                        }
                        send(r, out);
                    } else if (what.equals(WHAT_FORUM_COUNT)) {
                        mySession.userDidAction();
                        SurveyJSONWrapper r = newJSONStatus(request, sm);
                        r.put("what", what);
                        CLDRLocale locale = CLDRLocale.getInstance(loc);
                        int id = Integer.parseInt(xpath);
                        r.put(what, sm.fora.postCountFor(locale, id));
                        send(r, out);
                    } else if (what.equals(WHAT_FORUM_FETCH)) {
                        SurveyJSONWrapper r = newJSONStatus(request, sm);
                        CLDRLocale locale = CLDRLocale.getInstance(loc);
                        int id = Integer.parseInt(xpath);
                        if (mySession.user == null) {
                            r.put("err", "Not logged in.");
                            r.put("err_code", ErrorCode.E_NOT_LOGGED_IN.name());
                        } else if (!UserRegistry.userCanAccessForum(mySession.user, locale)) {
                            r.put("err", "You can’t access this forum.");
                            r.put("err_code", ErrorCode.E_NO_PERMISSION.name());
                        } else {
                            mySession.userDidAction();
                            r.put("what", what);
                            r.put("loc", loc);
                            r.put("xpath", xpath);
                            r.put("ret", sm.fora.toJSON(mySession, locale, id, 0));
                        }
                        send(r, out);
                    } else if (what.equals(WHAT_FORUM_POST)) {
                        mySession.userDidAction();
                        postToForum(request, out, xpath, l, mySession, sm);
                    } else if (what.equals("mail")) {
                        mySession.userDidAction();
                        SurveyJSONWrapper r = newJSONStatus(request, sm);
                        if (mySession.user == null) {
                            r.put("err", "Not logged in.");
                            r.put("err_code", ErrorCode.E_NOT_LOGGED_IN.name());
                        } else {

                            String fetchAll = request.getParameter("fetchAll");
                            int markRead = -1;
                            if (request.getParameter("markRead") != null) {
                                markRead = Integer.parseInt(request.getParameter("markRead"));
                            }

                            if (fetchAll != null) {
                                r.put(
                                        "mail",
                                        MailSender.getInstance().getMailFor(mySession.user.id));
                            } else if (markRead != -1) {
                                if (MailSender.getInstance().setRead(mySession.user.id, markRead)) {
                                    r.put("mail", "true");
                                } else {
                                    r.put("mail", "false"); // failed to mark
                                }
                            }
                        }
                        send(r, out);
                    } else if (what.equals(WHAT_GET_MENUS)) {

                        mySession.userDidAction();
                        SurveyJSONWrapper r = newJSONStatus(request, sm);
                        r.put("what", what);

                        SurveyMenus menus = sm.getSTFactory().getSurveyMenus();

                        if (loc != null && !loc.isEmpty()) {
                            r.put("covlev_org", mySession.getOrgCoverageLevel(loc));
                            r.put(
                                    "covlev_user",
                                    mySession.settings().get(SurveyMain.PREF_COVLEV, null));
                            CLDRLocale locale = CLDRLocale.getInstance(loc);
                            r.put("loc", loc);
                            r.put("menus", menus.toJSON(locale));

                            // add the report menu
                            JSONArray reports = new JSONArray();
                            for (SurveyMain.ReportMenu m : SurveyMain.ReportMenu.values()) {
                                JSONObject report = new JSONObject();
                                report.put("url", m.urlStub());
                                reports.put(report);
                            }
                            for (final ReportId m : ReportId.getReportsAvailable()) {
                                JSONObject report = new JSONObject();
                                report.put("url", "r_" + m.name());
                                reports.put(report);
                            }

                            r.put("reports", reports);
                        }

                        if ("true".equals(request.getParameter("locmap"))) {
                            r.put("locmap", getJSONLocMap(sm));

                            // list of modifyable locales
                            JSONArray modifyableLocs = new JSONArray();
                            Set<CLDRLocale> rolocs = SurveyMain.getReadOnlyLocales();
                            for (CLDRLocale al : SurveyMain.getLocales()) {
                                if (rolocs.contains(al)) continue;
                                if (UserRegistry.userCanModifyLocale(mySession.user, al)) {
                                    modifyableLocs.put(al.getBaseName());
                                }
                            }
                            if (modifyableLocs.length() > 0) {
                                r.put("canmodify", modifyableLocs);
                            }
                            /*
                             * If this user's old winning votes can be imported, and haven't already been imported,
                             * inform the client that we "canAutoImport". Client will send a new WHAT_AUTO_IMPORT request.
                             * Do not automatically import TC votes.
                             */
                            if (mySession.user != null
                                    && mySession.user.canImportOldVotes()
                                    && !UserRegistry.userIsTC(mySession.user)
                                    && !alreadyAutoImportedVotes(mySession.user.id, "ask")) {
                                r.put("canAutoImport", "true");
                            }
                        }
                        send(r, out);
                    } else if (what.equals(WHAT_AUTO_IMPORT)) {
                        mySession.userDidAction();
                        SurveyJSONWrapper r = newJSONStatus(request, sm);
                        r.put("what", what);
                        doAutoImportOldWinningVotes(r, mySession.user, sm);
                        send(r, out);
                    } else if (what.equals(WHAT_POSS_PROBLEMS)) {
                        mySession.userDidAction();
                        SurveyJSONWrapper r = newJSONStatus(request, sm);
                        r.put("what", what);

                        CLDRLocale locale = CLDRLocale.getInstance(loc);
                        r.put("loc", loc);
                        if (locale == null) {
                            r.put("err", "Bad locale: " + loc);
                            r.put("err_code", ErrorCode.E_BAD_LOCALE);
                            send(r, out);
                            return;
                        }
                        // Note: possibleProblems is always empty, as became clear after removal of
                        // dead code;
                        // this code is only called by possibleProblems.jsp, if ever
                        r.put("possibleProblems", new JSONArray());
                        send(r, out);
                    } else if (what.equals(WHAT_OLDVOTES)) {
                        mySession.userDidAction();
                        CookieSession.sm.getSTFactory().setupDB();
                        boolean isSubmit = (request.getParameter("doSubmit") != null);
                        String confirmList = request.getParameter("confirmList");
                        SurveyJSONWrapper r = newJSONStatus(request, sm);
                        importOldVotes(r, mySession.user, sm, isSubmit, confirmList, loc);
                        send(r, out);
                    } else if (what.equals(WHAT_GETSIDEWAYS) && l != null) {
                        mySession.userDidAction();
                        final SurveyJSONWrapper r = newJSONStatusQuick();
                        getSidewaysLocales(r, sm, l, xpath);
                        send(r, out);
                    } else if (what.equals(WHAT_SEARCH)) {
                        mySession.userDidAction();
                        final SurveyJSONWrapper r = newJSONStatusQuick();
                        r.put("what", what);
                        r.put("loc", loc);
                        r.put("q", val);

                        JSONArray results = searchResults(val, l);

                        r.put("results", results);

                        send(r, out);
                    } else if (what.equals(WHAT_PARTICIPATING_USERS)) {
                        assertHasUser(mySession);
                        assertIsTC(mySession);
                        SurveyJSONWrapper r = newJSONStatusQuick();
                        final String sql =
                                "select cldr_users.id as id, cldr_users.email as email, cldr_users.org as org from cldr_users, "
                                        + DBUtils.Table.VOTE_VALUE
                                        + " where "
                                        + DBUtils.Table.VOTE_VALUE
                                        + ".submitter = cldr_users.id and "
                                        + DBUtils.Table.VOTE_VALUE
                                        + ".submitter is not null group by email order by cldr_users.email";
                        JSONObject query =
                                DBUtils.queryToCachedJSON(what, 3600 * 1000, sql); // update hourly
                        r.put(what, query);
                        addGeneralStats(r);
                        send(r, out);
                    } else if (mySession.user != null) {
                        mySession.userDidAction();
                        switch (what) {
                            case WHAT_USER_INFO:
                                {
                                    String u = request.getParameter("u");
                                    if (u == null)
                                        throw new SurveyException(
                                                ErrorCode.E_INTERNAL, "Missing parameter 'u'");
                                    int userid = Integer.parseInt(u);

                                    final SurveyJSONWrapper r = newJSONStatusQuick();
                                    r.put("what", what);
                                    r.put("id", userid);

                                    UserRegistry.User them = sm.reg.getInfo(userid);
                                    if (them != null
                                            && ((them.id == mySession.user.id)
                                                    || // it's me
                                                    UserRegistry.userIsTC(mySession.user)
                                                    || (UserRegistry.userIsExactlyManager(
                                                                    mySession.user)
                                                            && (them.getOrganization()
                                                                    == mySession.user
                                                                            .getOrganization())))) {
                                        r.put("user", SurveyJSONWrapper.wrap(them));
                                    } else {
                                        r.put("err", "No permission to view this user's info");
                                        r.put("err_code", ErrorCode.E_NO_PERMISSION);
                                    }
                                    send(r, out);
                                }
                                break;
                            case WHAT_USER_LIST:
                                {
                                    final SurveyJSONWrapper r = newJSONStatusQuick();
                                    new UserList(request, response, mySession, sm).getJson(r);
                                    send(r, out);
                                }
                                break;
                            case WHAT_USER_OLDVOTES:
                                {
                                    final SurveyJSONWrapper r = newJSONStatusQuick();
                                    viewOldVoteStats(r, request, sm, mySession.user);
                                    send(r, out);
                                }
                                break;
                            case WHAT_USER_XFEROLDVOTES:
                                {
                                    final SurveyJSONWrapper r = newJSONStatusQuick();
                                    transferOldVotes(r, request, sm, mySession.user);
                                    send(r, out);
                                }
                                break;
                            default:
                                sendError(
                                        out,
                                        "Unknown User Session-based Request: " + what,
                                        ErrorCode.E_INTERNAL);
                        }
                    } else {
                        sendError(
                                out,
                                "Unknown Session-based Request: " + what,
                                ErrorCode.E_INTERNAL);
                    }
                }
            } else if (what.equals("locmap")) {
                final SurveyJSONWrapper r = newJSONStatusQuick();
                r.put("locmap", getJSONLocMap(sm));
                send(r, out);
            } else {
                sendError(out, "Unknown Request: " + what, ErrorCode.E_INTERNAL);
            }
        } catch (Throwable e) {
            SurveyLog.logException(
                    logger, e, "Error in SurveyAjax?what=" + what + ": " + e.getMessage());
            sendError(out, e);
        }
    }

    private void handleDashHideRequest(int userId, HttpServletRequest request) {
        String localeId = request.getParameter("locale");
        String subtype = request.getParameter("subtype");
        String xpstrid = request.getParameter("xpstrid");
        String value = request.getParameter("value");
        ReviewHide review = new ReviewHide(userId, localeId);
        review.toggleItem(subtype, xpstrid, value);
    }

    /**
     * Make a new forum post using data submitted by the client, save it to the database, and send
     * the client a json version of the post
     *
     * @param request the HttpServletRequest
     * @param out the PrintWriter
     * @param xpath the path, or null
     * @param l the CLDRLocale
     * @param mySession the CookieSession
     * @param sm the SurveyMain
     * @throws SurveyException
     * @throws JSONException
     */
    private void postToForum(
            HttpServletRequest request,
            PrintWriter out,
            String xpath,
            CLDRLocale l,
            CookieSession mySession,
            SurveyMain sm)
            throws SurveyException, JSONException {

        SurveyJSONWrapper r = newJSONStatusQuick();
        r.put("what", WHAT_FORUM_POST);

        final String subj = SurveyForum.HTMLSafe(request.getParameter("subj"));
        final String text = SurveyForum.HTMLSafe(request.getParameter("text"));
        final String postTypeStr = SurveyForum.HTMLSafe(request.getParameter("postType"));
        final String openStr = SurveyForum.HTMLSafe(request.getParameter("open"));
        final String value = SurveyForum.HTMLSafe(request.getParameter("value"));

        final int replyTo = getIntParameter(request, "replyTo", SurveyForum.NO_PARENT);
        final int root = getIntParameter(request, "root", SurveyForum.NO_PARENT);

        final boolean open = !"false".equals(openStr);

        SurveyForum.PostInfo postInfo = sm.fora.new PostInfo(l, postTypeStr, text);
        postInfo.setSubj(subj);
        postInfo.setPathString(xpath);
        postInfo.setReplyTo(replyTo);
        postInfo.setUser(mySession.user);
        postInfo.setRoot(root);
        postInfo.setOpen(open);
        if (value != null) {
            postInfo.setValue(value);
        }

        final int postId = sm.fora.doPost(mySession, postInfo);

        r.put("postId", postId);
        if (postId > 0) {
            /*
             * TODO: explain XPathTable.NO_XPATH here.
             * toJSON is called for WHAT_FORUM_FETCH as toJSON(mySession, locale, id, 0)
             */
            r.put("ret", sm.fora.toJSON(mySession, l, XPathTable.NO_XPATH, postId));
        }
        send(r, out);
    }

    /**
     * Get an integer parameter, with a default
     *
     * @param request
     * @param fieldName
     * @param defVal
     * @return
     */
    private int getIntParameter(HttpServletRequest request, String fieldName, int defVal) {
        final Integer v = getIntParameter(request, fieldName);
        if (v == null) {
            return defVal;
        } else {
            return v;
        }
    }

    /**
     * Get an integer value. Return null if missing.
     *
     * @param request
     * @param fieldName
     * @return
     */
    private Integer getIntParameter(HttpServletRequest request, final String fieldName) {
        final String replyToString = request.getParameter(fieldName);
        if (!replyToString.isEmpty()) {
            return Integer.parseInt(replyToString);
        }
        return null;
    }

    /**
     * Throw an exception if the user isn't TC level
     *
     * @param mySession
     * @throws SurveyException
     */
    public void assertIsTC(CookieSession mySession) throws SurveyException {
        if (!UserRegistry.userIsTC(mySession.user)) {
            throw new SurveyException(ErrorCode.E_NO_PERMISSION);
        }
    }

    /**
     * Throw an exception if the user isn't logged in.
     *
     * @param mySession
     * @throws SurveyException
     */
    public void assertHasUser(CookieSession mySession) throws SurveyException {
        if (mySession.user == null) {
            throw new SurveyException(ErrorCode.E_NOT_LOGGED_IN);
        }
    }

    /**
     * @param r
     */
    public void addGeneralStats(SurveyJSONWrapper r) {
        r.put("total_items", StatisticsUtils.getTotalItems());
        r.put("total_new_items", StatisticsUtils.getTotalNewItems());
        r.put("total_submitters", StatisticsUtils.getTotalSubmitters());
        r.put("time_now", System.currentTimeMillis());
    }

    private JSONArray searchResults(String q, CLDRLocale l) {
        JSONArray results = new JSONArray();

        if (q != null) {
            if (l == null) {
                searchLocales(results, q);
            } else {
                // ElapsedTimer et = new ElapsedTimer("search for " + q);
                // try as xpath
                searchXPath(results, q);

                // try PH substring
                searchPathheader(results, l, q);
                // System.err.println("Done searching for " + et);
            }
        }

        return results;
    }

    private void searchLocales(JSONArray results, String q) {
        for (CLDRLocale l : SurveyMain.getLocales()) {
            if (l.getBaseName().equalsIgnoreCase(q)
                    || l.getDisplayName().toLowerCase().contains(q.toLowerCase())
                    || l.toLanguageTag().toLowerCase().equalsIgnoreCase(q)) {
                try {
                    results.put(new JSONObject().put("loc", l.getBaseName()));
                } catch (JSONException e) {
                    //
                }
            }
        }
    }

    private void searchPathheader(JSONArray results, CLDRLocale l, String q) {
        if (l == null) {
            return; // don't search with no locale
        }
        try {
            PathHeader.PageId page = PathHeader.PageId.valueOf(q);
            results.put(
                    new JSONObject()
                            .put("page", page.name())
                            .put("section", page.getSectionId().name()));
        } catch (Throwable t) {
            //
        }
        try {
            PathHeader.SectionId section = PathHeader.SectionId.valueOf(q);
            results.put(new JSONObject().put("section", section.name()));
        } catch (Throwable t) {
            //
        }

        // substring search
        Set<PathHeader> resultPh = new TreeSet<>();

        if (new UnicodeSet("[:Letter:]").containsSome(q)) {
            // check English
            Set<String> retrievedPaths = new HashSet<>();
            SurveyMain sm = CookieSession.sm;
            sm.getEnglishFile().getPathsWithValue(q, "", null, retrievedPaths);
            final STFactory stFactory = sm.getSTFactory();
            stFactory.make(l, true).getPathsWithValue(q, "", null, retrievedPaths);
            for (String xp : retrievedPaths) {
                PathHeader ph = stFactory.getPathHeader(xp);
                if (ph != null) {
                    resultPh.add(ph);
                }
            }
        }
        // add any others
        CoverageInfo covInfo = CLDRConfig.getInstance().getCoverageInfo();
        for (PathHeader ph : resultPh) {
            try {
                final String originalPath = ph.getOriginalPath();
                if (ph.getSectionId() != PathHeader.SectionId.Special
                        && covInfo.getCoverageLevel(originalPath, l.getBaseName()).getLevel()
                                <= 100) {
                    results.put(
                            new JSONObject()
                                    .put("xpath", originalPath)
                                    .put("strid", getStringIDString(originalPath))
                                    .put("ph", SurveyJSONWrapper.wrap(ph)));
                }
            } catch (JSONException e) {
                //
            }
        }
    }

    private void searchXPath(JSONArray results, String q) {
        // is it a stringid?
        try {
            long l = Long.parseLong(q, 16);
            if (Long.toHexString(l).equalsIgnoreCase(q)) {
                SurveyMain sm = CookieSession.sm;
                String x = sm.xpt.getByStringID(q);
                if (x != null) {
                    results.put(
                            new JSONObject()
                                    .put("xpath", x)
                                    .put("strid", getStringIDString(x))
                                    .put(
                                            "ph",
                                            SurveyJSONWrapper.wrap(
                                                    sm.getSTFactory().getPathHeader(x))));
                }
            }
        } catch (Throwable t) {
            //
        }

        // is it a full XPath?
        try {
            final String xp = XPathTable.xpathToBaseXpath(q);
            SurveyMain sm = CookieSession.sm;
            if (sm.xpt.peekByXpath(xp) != XPathTable.NO_XPATH) {
                PathHeader ph = sm.getSTFactory().getPathHeader(xp);
                if (ph != null) {
                    results.put(
                            new JSONObject()
                                    .put("xpath", xp)
                                    .put("strid", getStringIDString(xp))
                                    .put("ph", SurveyJSONWrapper.wrap(ph)));
                }
            }
        } catch (Throwable t) {
            //
        }
    }

    /**
     * Validate a locale. Prints standardized error if not found.
     *
     * @param out the PrintWriter for reporting errors
     * @param loc the locale string; if for a sublocale, it can have either - or _ as in "fr-CA" or
     *     "fr_CA"
     * @param sess the session string, in case we need the user for "USER" wildcard
     * @return the CLDRLocale, or null for failure
     * @throws IOException
     */
    public static CLDRLocale validateLocale(PrintWriter out, String loc, String sess)
            throws IOException {
        if (loc == null || loc.isEmpty()) {
            return null;
        }
        CLDRLocale ret = null;
        if (CookieSession.sm == null || !SurveyMain.isSetup) {
            sendNoSurveyMain(out);
            return null;
        }
        /*
         * loc can have either - or _; normalize to _
         */
        loc = loc.replace('-', '_');
        /*
         * Support "USER" as a "wildcard" for locale name, replacing it with a locale suitable for the
         * current user, or "fr" (French) as a fallback.
         */
        if ("USER".equals(loc) && sess != null && !sess.isEmpty()) {
            loc = "fr"; // fallback
            CookieSession.checkForExpiredSessions();
            CookieSession mySession = CookieSession.retrieve(sess);
            if (mySession.user != null) {
                CLDRLocale exLoc = mySession.user.exampleLocale();
                if (exLoc != null) {
                    loc = exLoc.getBaseName();
                }
            }
        }
        if (loc != null && !loc.isEmpty()) {
            ret = CLDRLocale.getInstance(loc);
        }
        if (ret == null) {
            SurveyJSONWrapper r = newJSON();
            r.put("err", "Missing/unusable locale code:" + loc);
            r.put("loc", loc);
            r.put("err_code", ErrorCode.E_BAD_LOCALE);
            send(r, out);
            return null; // failed
        } else if (!SurveyMain.getLocalesSet().contains(ret)) {
            // Here we validate that the locale is a "Survey Tool Locale".
            // Ideally, the caller should be refactored for cases where
            // a locale is not needed.
            // For now, the client should pass in 'root' in lieu of a locale.
            SurveyJSONWrapper r = newJSON();
            r.put("err", "Not in the CLDR locale set:" + loc);
            r.put("loc", loc);
            r.put("err_code", ErrorCode.E_BAD_LOCALE);
            send(r, out);
            return null; // failed
        } else {
            return ret;
        }
    }

    private static JSONObject createJSONLocMap(SurveyMain sm) throws JSONException {
        JSONObject locmap = new JSONObject();
        // locales will have info about each locale, including name
        JSONObject locales = new JSONObject();
        SupplementalDataInfo sdi = sm.getSupplementalDataInfo();

        Factory disk = sm.getDiskFactory();

        for (CLDRLocale loc : SurveyMain.getLocales()) {
            JSONObject locale = new JSONObject();

            locale.put("name", loc.getDisplayName());
            if (loc.getCountry() != null) {
                locale.put("name_rgn", loc.getDisplayRegion());
            }
            if (loc.getVariant() != null) {
                locale.put("name_var", loc.getDisplayVariant());
            }
            locale.put("bcp47", loc.toLanguageTag());

            HTMLDirection dir = sm.getHTMLDirectionFor(loc);
            if (!dir.toString().equals("ltr")) {
                locale.put("dir", dir);
            }

            CLDRLocale dcParent = sdi.getBaseFromDefaultContent(loc);
            CLDRLocale dcChild = sdi.getDefaultContentFromBase(loc);
            locale.put("parent", loc.getParent());
            locale.put("highestParent", loc.getHighestNonrootParent());
            locale.put("dcParent", dcParent);
            locale.put("dcChild", dcChild);
            locale.put(
                    "type",
                    Factory.getSourceTreeType(disk.getSourceDirectoryForLocale(loc.getBaseName())));
            if (SurveyMain.getReadOnlyLocales().contains(loc)) {
                locale.put("readonly", true);
            } else if (dcParent != null) {
                locale.put("readonly", true);
            } else {
                // Readonly if in limited
                if (CheckCLDR.LIMITED_SUBMISSION
                        && !SubmissionLocales.LOCALES_ALLOWED_IN_LIMITED.contains(
                                loc.getBaseName())) {
                    locale.put("readonly", true);
                    // Readonly due to limited submission
                    locale.put("readonly_in_limited", true);
                }
            }
            final SpecialLocales.Type localeType = SpecialLocales.getType(loc);
            if (localeType != null) {
                locale.put("special_type", localeType.name());
                String comment = SpecialLocales.getComment(loc);
                locale.put("special_comment", comment);
                String commentraw = SpecialLocales.getCommentRaw(loc);
                locale.put("special_comment_raw", commentraw);
            }

            JSONArray subLocales = new JSONArray();
            Map<String, CLDRLocale> subLocList = sm.getLocaleTree().getSubLocales(loc);
            if (subLocList != null && !subLocList.isEmpty()) {
                for (CLDRLocale l : subLocList.values()) {
                    subLocales.put(l.getBaseName());
                }
                if (subLocales.length() > 0) {
                    locale.put("sub", subLocales);
                }
            }
            locales.put(loc.getBaseName(), locale); // note, this is in sorted (baseline) order.
        }

        locmap.put("locales", locales);
        locmap.put("surveyBaseLocale", SurveyMain.TRANS_HINT_LOCALE);
        JSONArray topLocales = new JSONArray();
        for (CLDRLocale l : sm.getLocaleTree().getTopCLDRLocales()) {
            topLocales.put(l.getBaseName());
        }
        locmap.put("topLocales", topLocales);

        return locmap;
    }

    private static JSONObject gLocMap = null;

    private static synchronized JSONObject getJSONLocMap(SurveyMain sm) throws JSONException {
        if (gLocMap == null) {
            ElapsedTimer et =
                    new ElapsedTimer("SurveyAjax.getJSONLocMap: created JSON locale map ");
            gLocMap = createJSONLocMap(sm);
            logger.info(et + " - serializes to: " + gLocMap.toString().length() + "chars.");
        }
        return gLocMap;
    }

    public static boolean has(List<CheckStatus> result, CheckStatus.Type type) {
        if (result != null) {
            for (CheckStatus s : result) {
                if (s.getType().equals(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasWarnings(List<CheckStatus> result) {
        return (has(result, CheckStatus.warningType));
    }

    public static boolean hasErrors(List<CheckStatus> result) {
        return (has(result, CheckStatus.errorType));
    }

    /**
     * @param sm
     * @param locale
     * @param r
     */
    private void setLocaleStatus(SurveyMain sm, String locale, SurveyJSONWrapper r) {
        if (locale != null
                && locale.length() > 0
                && SurveyMain.isBusted == null
                && SurveyMain.isSetup) {
            CLDRLocale loc = CLDRLocale.getInstance(locale);
            if (loc != null && SurveyMain.getLocalesSet().contains(loc)) {
                r.put("localeStampName", loc.getDisplayName());
                r.put("localeStampId", loc);
                r.put("localeStamp", sm.getSTFactory().mintLocaleStamp(loc).current());
            }
        }
    }

    /**
     * Add the wasInitCalled param.
     *
     * @see {@link SurveyMain#wasInitCalled()}
     * @param r
     */
    private static void setupWasInitCalled(SurveyJSONWrapper r) {
        r.put(
                "wasInitCalled",
                SurveyMain.wasInitCalled()); // if false: we are extremely early in Server setup
        r.put(
                "triedToStartUp",
                SurveyMain.triedToStartUp()); // if false: need to call GET /cldr-apps/survey
    }

    private void setupStatus(HttpServletRequest request, SurveyMain sm, SurveyJSONWrapper r) {
        r.put("SurveyOK", "1");
        try {
            r.put("status", sm.statusJSON(request));
        } catch (JSONException e) {
            SurveyLog.logException(logger, e, "getting status");
        }
    }

    /**
     * Create a new SurveyJSONWrapper and setup its status with the given SurveyMain. This is used
     * to initialize JSON responses so that callers know whether the ST is actually running or not.
     *
     * @param sm the SurveyMain
     * @return the SurveyJSONWrapper
     */
    private SurveyJSONWrapper newJSONStatus(HttpServletRequest request, SurveyMain sm) {
        SurveyJSONWrapper r = newJSON();
        setupStatus(request, sm, r);
        return r;
    }

    /**
     * This is used to initialize JSON responses. It gives a very brief (succinct and speedy)
     * SurveyTool status.
     *
     * @return
     */
    private SurveyJSONWrapper newJSONStatusQuick() {
        SurveyJSONWrapper r = newJSON();
        if (SurveyMain.isSetup && !SurveyMain.isBusted()) {
            r.put("SurveyOK", "1");
            r.put("isSetup", "1");
            r.put("isBusted", "0");
        }
        return r;
    }

    private static SurveyJSONWrapper newJSON() {
        SurveyJSONWrapper r = new SurveyJSONWrapper();
        r.put("visitors", "");
        r.put("uptime", "");
        r.put("err", "");
        r.put("SurveyOK", "0");
        r.put("isSetup", "0");
        r.put("isBusted", "0");
        setupWasInitCalled(r);
        return r;
    }

    private static void sendNoSurveyMain(PrintWriter out) {
        SurveyJSONWrapper r = newJSON();
        r.put("SurveyOK", "0");
        r.put("err", "The SurveyTool has not yet started.");
        r.put("err_code", ErrorCode.E_NOT_STARTED);
        send(r, out);
    }

    private void sendError(PrintWriter out, String message, ErrorCode errCode) {
        SurveyJSONWrapper r = newJSON();
        r.put("SurveyOK", "0");
        r.put("err", message);
        r.put("err_code", errCode);
        send(r, out);
    }

    private void sendError(PrintWriter out, SurveyException e) {
        SurveyJSONWrapper r = newJSON();
        r.put("SurveyOK", "0");
        r.put("err", e.getMessage());
        r.put("err_code", e.getErrCode());
        try {
            e.addDataTo(r);
        } catch (JSONException e1) {
            SurveyLog.logException(logger, e1, "While processing " + e);
            r.put("err", e.getMessage() + " - and JSON error " + e1);
        }
        send(r, out);
    }

    private void sendError(PrintWriter out, Throwable e) {
        if (e instanceof SurveyException) {
            sendError(out, (SurveyException) e);
        } else {
            SurveyJSONWrapper r = newJSON();
            r.put("SurveyOK", "0");
            r.put("err", e.toString());
            r.put("err_code", SurveyException.ErrorCode.E_INTERNAL);
            send(r, out);
        }
    }

    private static void send(SurveyJSONWrapper r, PrintWriter out) {
        out.print(r.toString());
    }

    /**
     * Import old votes.
     *
     * @param r the SurveyJSONWrapper in which to write
     * @param user the User (see UserRegistry.java)
     * @param sm the SurveyMain instance
     * @param isSubmit false when just showing options to user, true when user clicks "Vote for
     *     selected Winning/Losing Votes"
     * @param confirmList the list of xpaths to import when isSubmit is true, else null
     * @param loc the String request.getParameter(SurveyMain.QUERY_LOCALE); empty string when first
     *     called for "Import Old Votes", non-empty when user later clicks the link for a particular
     *     locale, then it's like "aa"
     */
    private void importOldVotes(
            SurveyJSONWrapper r,
            User user,
            SurveyMain sm,
            boolean isSubmit,
            String confirmList,
            String loc)
            throws IOException, JSONException, SQLException {
        r.put("what", WHAT_OLDVOTES);
        if (user == null) {
            r.put("err", "Must be logged in");
            r.put("err_code", ErrorCode.E_NOT_LOGGED_IN);
        } else if (!user.canImportOldVotes()) {
            r.put("err", "No permission to do this (may not be the right SurveyTool phase)");
            r.put("err_code", ErrorCode.E_NO_PERMISSION);
        } else {
            importOldVotesForValidatedUser(r, user, sm, isSubmit, confirmList, loc);
        }
    }

    /**
     * Import old votes, having confirmed user has permission.
     *
     * @param r the SurveyJSONWrapper in which to write
     * @param user the User
     * @param sm the SurveyMain instance
     * @param isSubmit false when just showing options to user, true when user clicks "Vote for
     *     selected Winning/Losing Votes"
     * @param confirmList
     * @param loc the locale as a string, empty when first called for "Import Old Votes", non-empty
     *     when user later clicks the link for a particular locale, then it's like "aa"
     *     <p>Three ways this function is called:
     *     <p>(1) loc == null, isSubmit == false: list locales to choose (2) loc == 'aa', isSubmit
     *     == false: show winning/losing votes available for import (3) loc == 'aa', isSubmit ==
     *     true: update db based on vote
     */
    private void importOldVotesForValidatedUser(
            SurveyJSONWrapper r,
            User user,
            SurveyMain sm,
            boolean isSubmit,
            String confirmList,
            String loc)
            throws IOException, JSONException, SQLException {

        JSONObject oldvotes = new JSONObject();
        final String newVotesTable = DBUtils.Table.VOTE_VALUE.toString();

        if (loc == null || loc.isEmpty()) {
            listLocalesForImportOldVotes(user, sm, newVotesTable, oldvotes);
        } else {
            CLDRLocale locale = CLDRLocale.getInstance(loc);
            oldvotes.put("locale", locale);
            oldvotes.put("localeDisplayName", locale.getDisplayName());
            HTMLDirection dir = sm.getHTMLDirectionFor(locale);
            oldvotes.put("dir", dir); // e.g., LEFT_TO_RIGHT
            if (isSubmit) {
                submitOldVotes(user, sm, locale, confirmList, newVotesTable, oldvotes);
            } else {
                viewOldVotes(user, sm, loc, locale, newVotesTable, oldvotes);
            }
        }
        r.put("oldvotes", oldvotes);
        r.put("TRANS_HINT_LANGUAGE_NAME", SurveyMain.TRANS_HINT_LANGUAGE_NAME);
        r.put("TRANS_HINT_ID", SurveyMain.TRANS_HINT_ID);
    }

    /**
     * List locales that have old votes available to be imported.
     *
     * @param user the User (this function only uses user.id)
     * @param sm the SurveyMain instance
     * @param newVotesTable the String for the table name like "cldr_vote_value_34"
     * @param oldvotes the JSONObject to be added to
     */
    public void listLocalesForImportOldVotes(
            User user, SurveyMain sm, final String newVotesTable, JSONObject oldvotes)
            throws IOException, JSONException, SQLException {

        /* Loop thru multiple old votes tables in reverse chronological order. */
        int ver = Integer.parseInt(SurveyMain.getNewVersion());
        Map<String, Long> localeCount = new HashMap<>();
        Map<String, String> localeName = new HashMap<>();
        while (--ver >= oldestVersionForImportingVotes) {
            String oldVotesTable =
                    DBUtils.Table.VOTE_VALUE
                            .forVersion(Integer.valueOf(ver).toString(), false)
                            .toString();
            if (DBUtils.hasTable(oldVotesTable)) {
                String sql =
                        "select locale,count(*) as count from "
                                + oldVotesTable
                                + " where submitter=? "
                                + " and value is not null "
                                + " and not exists (select * from "
                                + newVotesTable
                                + " where "
                                + oldVotesTable
                                + ".locale="
                                + newVotesTable
                                + ".locale "
                                + " and "
                                + oldVotesTable
                                + ".xpath="
                                + newVotesTable
                                + ".xpath and "
                                + newVotesTable
                                + ".submitter="
                                + oldVotesTable
                                + ".submitter )"
                                + "group by locale order by locale";
                /* DBUtils.queryToJSON was formerly used here and would return something like this:
                 * {"data":[["aa",2,"Afar"],["af",2,"Afrikaans"]],"header":{"LOCALE":0,"COUNT":1,"LOCALE_NAME":2}}
                 * We're no longer using queryToJSON here, due to the use of multiple tables.
                 * Assemble that same structure using multiple queries and queryToArrayAssoc.
                 */
                Map<String, Object>[] rows = DBUtils.queryToArrayAssoc(sql, user.id);
                for (Map<String, Object> m : rows) {
                    String loc = m.get("locale").toString(); // like "pt" or "pt_PT"
                    Long count = (Long) m.get("count"); // like 1616
                    if (localeCount.containsKey(loc)) {
                        localeCount.put(loc, count + localeCount.get(loc));
                    } else {
                        localeCount.put(loc, count);
                        /* Complication: the rows do not include the unabbreviated locale name.
                         * In queryToJSON it's added specially:
                         * locale_name = CLDRLocale.getInstance(v).getDisplayName();
                         * Here we do the same.
                         */
                        localeName.put(loc, CLDRLocale.getInstance(loc).getDisplayName());
                    }
                }
            }
        }
        /*
         * On the front end, the json is used like this:
         *  var data = json.oldvotes.locales.data;
         *  var header = json.oldvotes.locales.header;
         *  The header is then used for header.LOCALE_NAME, header.LOCALE, and header.COUNT.
         *  The header is always {"LOCALE":0,"COUNT":1,"LOCALE_NAME":2} here, since
         *  0, 1, and 2 are the indexes of the three elements in each array like ["aa",2,"Afar"].
         */
        JSONObject header = new JSONObject().put("LOCALE", 0).put("COUNT", 1).put("LOCALE_NAME", 2);
        JSONArray data = new JSONArray();
        /*
         * Create the data array. Revise the vote count for each locale, to match how viewOldVotes
         * will assemble the data, since viewOldVotes may filter out some votes. Call viewOldVotes here
         * with null for its oldvotes parameter, meaning just count the votes.
         */
        JSONObject oldVotesNull = null;
        localeCount.forEach(
                (loc, count) -> {
                    if (skipLocForImportingVotes(loc)) {
                        return;
                    }
                    /*
                     * We may get realCount <= count due to filtering. If we catch an exception here thrown in
                     * viewOldVotes, use the value already in count, still enabling the user to select the locale.
                     */
                    long realCount = count;
                    try {
                        CLDRLocale locale = CLDRLocale.getInstance(loc);
                        if (locale != null) {
                            realCount =
                                    viewOldVotes(
                                            user, sm, loc, locale, newVotesTable, oldVotesNull);
                        }
                    } catch (Throwable t) {
                        SurveyLog.logException(
                                logger, t, "listLocalesForImportOldVotes: loc = " + loc);
                    }
                    if (realCount > 0) {
                        data.put(new JSONArray().put(loc).put(realCount).put(localeName.get(loc)));
                    }
                });
        JSONObject j = new JSONObject().put("header", header).put("data", data);
        oldvotes.put("locales", j);
        oldvotes.put("lastVoteVersion", SurveyMain.getLastVoteVersion());
    }

    /**
     * View old votes available to be imported for the given locale; or, if the oldvotes argument is
     * null, just return the number of old votes available.
     *
     * <p>Add to the given "oldvotes" object an array of "contested" (losing) votes, and possibly
     * also an array of "uncontested" (winning) votes, to get displayed by survey.js.
     *
     * <p>In general, both winning (uncontested) and losing (contested) votes may be present in the
     * db. Normally, for non-TC users, winning votes are imported automatically. Therefore, don't
     * count or list winning votes unless user is TC.
     *
     * @param user the User, for user.id and userIsTC
     * @param sm the SurveyMain instance, used for sm.xpt, sm.getEnglishFile, and sm.getDiskFactory
     * @param loc the non-empty String for the locale like "aa"
     * @param locale the CLDRLocale matching loc
     * @param newVotesTable the String for the table name like "cldr_vote_value_34"
     * @param oldvotes the JSONObject to be added to; if null, just return the count
     * @return the number of entries that will be viewable for this locale in the Import Old Votes
     *     interface.
     *     <p>Called locally by importOldVotesForValidatedUser and listLocalesForImportOldVotes
     */
    private long viewOldVotes(
            User user,
            SurveyMain sm,
            String loc,
            CLDRLocale locale,
            final String newVotesTable,
            JSONObject oldvotes)
            throws IOException, JSONException, SQLException {

        Map<String, Object>[] rows = getOldVotesRows(newVotesTable, locale, user.id);
        STFactory stFac = sm.getSTFactory();
        Factory diskFac = sm.getDiskFactory();
        // extract the pathheaders
        for (Map<String, Object> m : rows) {
            int xp = (Integer) m.get("xpath");
            String xpathString = sm.xpt.getById(xp);
            m.put("pathHeader", stFac.getPathHeader(xpathString));
        }

        // sort by pathheader
        Arrays.sort(rows, Comparator.comparing(o -> ((PathHeader) o.get("pathHeader"))));

        boolean useWinningVotes = UserRegistry.userIsTC(user);

        /* Some variables are only used if oldvotes != null; otherwise leave them null. */
        JSONArray contested = null; // contested = losing
        JSONArray uncontested = null; // uncontested = winning
        CLDRFile englishFile = null;
        if (oldvotes != null) {
            contested = new JSONArray();
            if (useWinningVotes) {
                uncontested = new JSONArray();
            }
            englishFile = sm.getEnglishFile();
        }
        CLDRFile cldrFile = diskFac.make(loc, true, true);
        XMLSource diskData = cldrFile.getResolvingDataSource();
        CLDRFile cldrUnresolved = cldrFile.getUnresolved();

        Set<String> validPaths = stFac.getPathsForFile(locale);
        CoverageInfo covInfo = CLDRConfig.getInstance().getCoverageInfo();
        long viewableVoteCount = 0;
        for (Map<String, Object> m : rows) {
            try {
                String value = m.get("value").toString();
                if (value == null) {
                    continue; // ignore unvotes.
                }
                PathHeader pathHeader = (PathHeader) m.get("pathHeader");
                if (!pathHeader.canReadAndWrite()) {
                    continue; // skip these
                }
                int xp = (Integer) m.get("xpath");
                String xpathString = sm.xpt.getById(xp);
                if (!validPaths.contains(xpathString)) {
                    continue;
                }
                if (covInfo.getCoverageValue(xpathString, loc) > Level.COMPREHENSIVE.getLevel()) {
                    continue; // out of coverage
                }
                if (CheckForCopy.sameAsCode(value, xpathString, cldrUnresolved, cldrFile)) {
                    continue; // not allowed
                }
                String curValue = diskData.getValueAtDPath(xpathString);
                boolean isWinning =
                        cldrFile.equalsOrInheritsCurrentValue(value, curValue, xpathString);
                if (oldvotes != null) {
                    String xpathStringHash = sm.xpt.getStringIDString(xp);
                    JSONObject aRow =
                            new JSONObject()
                                    .put("strid", xpathStringHash)
                                    .put("myValue", value)
                                    .put("winValue", curValue)
                                    .put("baseValue", englishFile.getStringValue(xpathString))
                                    .put("pathHeader", pathHeader.toString());
                    if (isWinning) {
                        if (uncontested != null) {
                            uncontested.put(aRow); // uncontested = winning
                            ++viewableVoteCount;
                        }
                    } else {
                        if (contested != null) {
                            contested.put(aRow); // contested = losing
                            ++viewableVoteCount;
                        }
                    }
                } else {
                    if (isWinning) {
                        if (useWinningVotes) {
                            ++viewableVoteCount;
                        }
                    } else { // always count losing votes
                        ++viewableVoteCount;
                    }
                }
            } catch (Exception e) {
                /*
                 * Skip old votes that generate exceptions -- they're invalid; neither winning nor
                 * losing, but invisible as far as import is concerned.
                 *
                 * Log any exception that we don't want to ignore completely.
                 * InvalidXPathException and VoteNotAcceptedException would be OK to ignore, but
                 * they can't happen here.
                 */
                SurveyLog.logException(logger, e, "Viewing old votes");
            }
        }
        if (oldvotes != null) {
            oldvotes.put("lastVoteVersion", SurveyMain.getLastVoteVersion());
            if (contested != null) {
                oldvotes.put("contested", contested); // contested = losing
            }
            if (uncontested != null) {
                oldvotes.put("uncontested", uncontested); // uncontested = winning
            }
        }
        return viewableVoteCount;
    }

    /**
     * Submit the selected old votes to be imported.
     *
     * @param user the User
     * @param sm the SurveyMain instance, used for sm.xpt and sm.reg
     * @param locale the CLDRLocale
     * @param confirmList the list, as a string like "7b8ee7884f773afa,1234567890abcdef"
     * @param newVotesTable the String for the table name like "cldr_vote_value_34"
     * @param oldvotes the JSONObject to be added to
     * @throws IOException
     * @throws JSONException
     * @throws SQLException
     *     <p>Called locally by importOldVotesForValidatedUser
     */
    private void submitOldVotes(
            User user,
            SurveyMain sm,
            CLDRLocale locale,
            String confirmList,
            final String newVotesTable,
            JSONObject oldvotes)
            throws IOException, JSONException, SQLException {

        if (SurveyMain.isUnofficial()) {
            System.out.println(
                    "User "
                            + user.toString()
                            + "  is migrating old votes in "
                            + locale.getDisplayName());
        }
        STFactory stFac = sm.getSTFactory();
        BallotBox<User> box = stFac.ballotBoxForLocale(locale);

        Set<String> confirmSet = new HashSet<>();
        for (String s : confirmList.split(",")) {
            if (!s.isEmpty()) {
                confirmSet.add(s);
            }
        }

        Map<String, Object>[] rows = getOldVotesRows(newVotesTable, locale, user.id);

        DisplayAndInputProcessor daip = DisplayAndInputProcessorFactory.make(locale);
        Exception[] exceptionList = new Exception[1];
        for (Map<String, Object> m : rows) {
            String value = m.get("value").toString();
            /*
             * Skip abstentions (null value) and votes for inheritance.
             * For inheritance, a candidate item is provided anyway when appropriate.
             */
            if (value == null || value.equals(CldrUtility.INHERITANCE_MARKER)) {
                continue;
            }
            int xp = (Integer) m.get("xpath");
            String xpathString = sm.xpt.getById(xp);
            try {
                String strid = sm.xpt.getStringIDString(xp);
                if (confirmSet.contains(strid)) {
                    String processedValue = daip.processInput(xpathString, value, exceptionList);
                    importAnonymousOldLosingVote(
                            box, locale, xpathString, xp, value, processedValue, sm.reg);
                }
            } catch (InvalidXPathException ix) {
                SurveyLog.logException(
                        logger, ix, "Bad XPath: Trying to import for " + xpathString);

            } catch (VoteNotAcceptedException ix) {
                SurveyLog.logException(
                        logger, ix, "Vote not accepted: Trying to import for " + xpathString);
            }
        }
        oldvotes.put("ok", true);
    }

    /**
     * Import the given old losing value as an "anonymous" vote.
     *
     * @param box the BallotBox, specific to the locale
     * @param locale the CLDRLocale
     * @param xpathString the path
     * @param unprocessedValue the old losing value to be imported, before daip.processInput
     * @param processedValue the old losing value to be imported, after daip.processInput
     * @param reg the UserRegistry
     * @throws InvalidXPathException
     * @throws VoteNotAcceptedException
     * @throws SQLException
     *     <p>Reference: https://unicode.org/cldr/trac/ticket/11517
     */
    private void importAnonymousOldLosingVote(
            BallotBox<User> box,
            CLDRLocale locale,
            String xpathString,
            int xpathId,
            String unprocessedValue,
            String processedValue,
            UserRegistry reg)
            throws InvalidXPathException, VoteNotAcceptedException, SQLException {
        /*
         * If there is already an anonymous vote for this locale+path+value, do not add
         * another one, simply return.
         */
        Set<User> voters = box.getVotesForValue(xpathString, processedValue);
        if (voters != null) {
            for (User user : voters) {
                if (UserRegistry.userIsExactlyAnonymous(user)) {
                    return;
                }
            }
        }
        /*
         * Find an anonymous user to be the submitter, which must be unique for this locale+path.
         */
        User anonUser = getFreshAnonymousUser(box, xpathString, reg);
        if (anonUser == null) {
            return;
        }
        /*
         * Submit the anonymous vote.
         */
        box.voteForValueWithType(anonUser, xpathString, processedValue, VoteType.MANUAL_IMPORT);
        /*
         * Add a row to the IMPORT table, to avoid importing the same value repeatedly.
         * For this we need unprocessedValue, to match what occurs for the original votes in the
         * old votes tables.
         */
        addRowToImportTable(locale, xpathId, unprocessedValue);
    }

    /**
     * Get an anonymous user who has not already voted for the given xpath in this locale
     *
     * @param box the BallotBox, specific to the locale
     * @param xpathString the path
     * @param reg the UserRegistry
     * @return the anonymous user, or null if there are none who haven't voted
     */
    private User getFreshAnonymousUser(BallotBox<User> box, String xpathString, UserRegistry reg) {

        for (User user : reg.getAnonymousUsers()) {
            if (!box.userDidVote(user, xpathString)) {
                return user;
            }
        }
        /*
         * The pool of anonymous voters has run out. See UserRegistry.ANONYMOUS_USER_COUNT.
         * We could dynamically add a new anonymous user here. The current assumption is
         * that ANONYMOUS_USER_COUNT is larger than will be needed in practice. A larger
         * number of old losing votes for the same path and locale is not to be imported.
         */
        return null;
    }

    /**
     * Add a row to the IMPORT table, so we can avoid importing the same value repeatedly. For this
     * we need unprocessedValue, to match what occurs for the original votes in the old votes
     * tables.
     *
     * @param locale the locale
     * @param xpathId the xpath id
     * @param unprocessedValue the value
     * @throws SQLException
     */
    private void addRowToImportTable(CLDRLocale locale, int xpathId, String unprocessedValue)
            throws SQLException {

        int newVer = Integer.parseInt(SurveyMain.getNewVersion());
        String importTable =
                DBUtils.Table.IMPORT
                        .forVersion(Integer.valueOf(newVer).toString(), false)
                        .toString();
        Connection conn = null;
        PreparedStatement ps = null;
        // IGNORE: don't throw an exception if the row already exists
        String sql = "INSERT IGNORE INTO " + importTable + "(locale,xpath,value) VALUES(?,?,?)";
        try {
            conn = DBUtils.getInstance().getDBConnection();
            /*
             * arg 1 = locale (ASCII) use prepareStatementWithArgs
             * arg 2 = xpath (int) use prepareStatementWithArgs
             * arg 3 = value (UTF-8) use setStringUTF8 (can't use prepareStatementWithArgs)
             */
            ps = DBUtils.prepareStatementWithArgs(conn, sql, locale.getBaseName(), xpathId);
            DBUtils.setStringUTF8(ps, 3, unprocessedValue);
            ps.executeUpdate();
            conn.commit();
        } finally {
            DBUtils.close(ps, conn);
        }
    }

    /**
     * Make an array of maps for importing old votes.
     *
     * @param newVotesTable
     * @param locale
     * @param id the user ID
     * @return the array of maps
     * @throws SQLException
     * @throws IOException
     *     <p>Called by viewOldVotes and submitOldVotes.
     */
    private static Map<String, Object>[] getOldVotesRows(
            final String newVotesTable, CLDRLocale locale, int id)
            throws SQLException, IOException {

        /* Loop thru multiple old votes tables in reverse chronological order.
         * Use "union" to combine into a single sql query.
         */
        int newVer = Integer.parseInt(SurveyMain.getNewVersion());
        String importTable =
                DBUtils.Table.IMPORT
                        .forVersion(Integer.valueOf(newVer).toString(), false)
                        .toString();
        String sql = "";
        int tableCount = 0;
        int ver = newVer;
        while (--ver >= oldestVersionForImportingVotes) {
            String oldVotesTable =
                    DBUtils.Table.VOTE_VALUE
                            .forVersion(Integer.valueOf(ver).toString(), false)
                            .toString();
            if (DBUtils.hasTable(oldVotesTable)) {
                if (!sql.isEmpty()) {
                    sql += " UNION ALL ";
                }
                sql +=
                        "(select xpath,value from "
                                + oldVotesTable
                                + " where locale=? and submitter=? and value is not null"
                                /*
                                 * ... and same submitter hasn't voted for a different value for this locale+xpath in current version:
                                 */
                                + " and not exists (select * from "
                                + newVotesTable
                                + " where "
                                + oldVotesTable
                                + ".locale="
                                + newVotesTable
                                + ".locale"
                                + " and "
                                + oldVotesTable
                                + ".xpath="
                                + newVotesTable
                                + ".xpath"
                                + " and "
                                + oldVotesTable
                                + ".submitter="
                                + newVotesTable
                                + ".submitter"
                                + " and "
                                + newVotesTable
                                + ".value is not null)"
                                /*
                                 * ... and no vote for the same locale+path+value has been anonymously imported into the current version:
                                 */
                                + " and not exists (select * from "
                                + importTable
                                + " where "
                                + oldVotesTable
                                + ".value="
                                + importTable
                                + ".value"
                                + " and "
                                + oldVotesTable
                                + ".locale="
                                + importTable
                                + ".locale"
                                + " and "
                                + oldVotesTable
                                + ".xpath="
                                + importTable
                                + ".xpath))";
                ++tableCount;
            }
        }
        Object[] args = new Object[2 * tableCount];
        for (int i = 0; i < 2 * tableCount; i += 2) {
            args[i] = locale; // one for each locale=? in the query
            args[i + 1] = id; // one for each submitter=? in the query
        }
        Map<String, Object>[] rows = DBUtils.queryToArrayAssoc(sql, args);
        return filterDowngradePaths(rows, locale.getBaseName());
    }

    private static Map<String, Object>[] filterDowngradePaths(
            Map<String, Object>[] rows, String localeId) {
        ArrayList<Map<String, Object>> al = new ArrayList<>();
        for (Map<String, Object> m : rows) {
            int xp = (Integer) m.get("xpath");
            Object obj = m.get("value");
            String value = (obj == null) ? null : obj.toString();
            String xpathString = CookieSession.sm.xpt.getById(xp);
            if (!DowngradePaths.lookingAt(localeId, xpathString, value)) {
                al.add(m);
            }
        }
        return al.toArray(new Map[0]);
    }

    /**
     * Import all old winning votes for this user, without GUI interaction other than a dialog when
     * finished: "Your old winning votes for locales ... have been imported." "OK".
     *
     * <p>Caller already checked (user != null && user.canImportOldVotes() &&
     * !UserRegistry.userIsTC(user).
     *
     * <p>See https://unicode.org/cldr/trac/ticket/11056 AND
     * https://unicode.org/cldr/trac/ticket/11123
     *
     * <p>Caller has already verified user.canImportOldVotes().
     *
     * <p>Skip the GUI interactions of importOldVotesForValidatedUser for listing locales, and
     * viewOldVotes for choosing which votes to import. Instead, gather the needed information as
     * though the user had chosen to select all their old winning votes in viewOldVotes, and submit
     * them as in submitOldVotes.
     *
     * @param r the SurveyJSONWrapper in which to write
     * @param user the User, or null (do nothing)
     * @param sm the SurveyMain instance
     * @throws IOException
     * @throws JSONException
     * @throws SQLException
     */
    private void doAutoImportOldWinningVotes(SurveyJSONWrapper r, User user, SurveyMain sm)
            throws IOException, JSONException, SQLException {

        if (user == null || alreadyAutoImportedVotes(user.id, "ask")) {
            return;
        }
        alreadyAutoImportedVotes(user.id, "set");
        CookieSession.sm.getSTFactory().setupDB();
        final String newVotesTable =
                DBUtils.Table.VOTE_VALUE.toString(); // the table name like "cldr_vote_value_34" or
        // "cldr_vote_value_34_beta"
        JSONObject oldvotes = new JSONObject();

        /* Loop thru multiple old votes tables in reverse chronological order:
         *  cldr_vote_value_33, cldr_vote_value_32, cldr_vote_value_31, ..., cldr_vote_value_25.
         *  If user voted for an item in version N, then ignore votes for the same item in versions before N
         *  (see box.getVoteValue in importAllOldWinningVotes).
         */
        int ver = Integer.parseInt(SurveyMain.getNewVersion());
        int confirmations = 0;
        while (--ver >= oldestVersionForImportingVotes) {
            String oldVotesTable =
                    DBUtils.Table.VOTE_VALUE
                            .forVersion(Integer.valueOf(ver).toString(), false)
                            .toString();
            if (DBUtils.hasTable(oldVotesTable)) {
                // SurveyLog.warnOnce("Old Votes table present: " + oldVotesTable);
                int count =
                        DBUtils.sqlCount(
                                "select  count(*) as count from "
                                        + oldVotesTable
                                        + " where submitter=?",
                                user.id);
                if (count > 0) { // may be -1 on error
                    if (SurveyMain.isUnofficial()) {
                        System.out.println(
                                "Old Votes remaining: " + user + " oldVotesCount = " + count);
                    }
                    confirmations +=
                            importAllOldWinningVotes(user, sm, oldVotesTable, newVotesTable);
                }
            } else {
                SurveyLog.warnOnce(logger, "Old Votes table missing: " + oldVotesTable);
            }
        }
        oldvotes.put("ok", true);
        r.put("what", WHAT_OLDVOTES);
        r.put("oldvotes", oldvotes);
        if (confirmations > 0) {
            r.put("autoImportedOldWinningVotes", confirmations);
        }
    }

    /**
     * Ask, or set, whether the user has already had their old winning votes automatically imported.
     *
     * <p>The versioned table IMPORT_AUTO has a row for the user if and only if auto-import has been
     * done for this user for this session.
     *
     * <p>If the action is "ask", return true or false to indicate whether or not the table has a
     * row for this user. If the action is "set", add a row to the table for this user. If the
     * action is "clear", remove from the table the row for this user (if any) (used after transfer
     * votes)
     *
     * @param userId the user id
     * @param action "ask", "set", or "clear"
     * @return true or false (only used for "ask")
     */
    private boolean alreadyAutoImportedVotes(int userId, String action) {
        final String autoImportTable =
                DBUtils.Table.IMPORT_AUTO.toString(); // the table name like "cldr_import_auto_35"
        int count = 0;
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            if ("ask".equals(action)) {
                count =
                        DBUtils.sqlCount(
                                "SELECT count(*) AS count FROM "
                                        + autoImportTable
                                        + " WHERE userid="
                                        + userId);
            } else if ("set".equals(action)) {
                conn = DBUtils.getInstance().getDBConnection();
                ps =
                        DBUtils.prepareStatementWithArgs(
                                conn,
                                "INSERT INTO " + autoImportTable + " VALUES (" + userId + ")");
                count = ps.executeUpdate();
                conn.commit();
            } else if ("clear".equals(action)) {
                conn = DBUtils.getInstance().getDBConnection();
                ps =
                        DBUtils.prepareStatementWithArgs(
                                conn, "DELETE FROM " + autoImportTable + " WHERE userid=" + userId);
                count = ps.executeUpdate();
                conn.commit();
            }
        } catch (SQLException e) {
            SurveyLog.logException(logger, e, "SQL exception: " + autoImportTable);
        } finally {
            DBUtils.close(ps, conn);
        }
        return count > 0;
    }

    /**
     * Import all old winning votes for this user in the specified old table.
     *
     * @param user the User
     * @param sm the SurveyMain instance
     * @param oldVotesTable the String for the table name like "cldr_vote_value_33"
     * @param newVotesTable the String for the table name like "cldr_vote_value_34"
     * @return how many votes imported
     *     <p>Called locally by doAutoImportOldWinningVotes.
     */
    private int importAllOldWinningVotes(
            User user, SurveyMain sm, final String oldVotesTable, final String newVotesTable)
            throws IOException, SQLException {
        STFactory stFac = sm.getSTFactory();
        Factory diskFac = sm.getDiskFactory();

        /*
         * Different from similar queries elsewhere: since we're doing ALL locales for this user,
         * here we have "where submitter=?", not "where locale=? and submitter=?";
         * and we have "select xpath,value,locale" since we need each locale for fac.ballotBoxForLocale(locale).
         */
        String sqlStr =
                "select xpath,value,locale from "
                        + oldVotesTable
                        + " where submitter=?"
                        + " and not exists (select * from "
                        + newVotesTable
                        + " where "
                        + oldVotesTable
                        + ".locale="
                        + newVotesTable
                        + ".locale  and "
                        + oldVotesTable
                        + ".xpath="
                        + newVotesTable
                        + ".xpath "
                        + "and "
                        + oldVotesTable
                        + ".submitter="
                        + newVotesTable
                        + ".submitter)";
        Map<String, Object>[] rows = DBUtils.queryToArrayAssoc(sqlStr, user.id);

        int confirmations = 0;
        for (Map<String, Object> m : rows) {
            Object obj = m.get("value");
            String value = (obj == null) ? null : obj.toString();
            int xp = (Integer) m.get("xpath");
            String xpathString = sm.xpt.getById(xp);
            String loc = m.get("locale").toString();
            if (skipLocForImportingVotes(loc)
                    || DowngradePaths.lookingAt(loc, xpathString, value)) {
                continue;
            }
            CLDRLocale locale = CLDRLocale.getInstance(loc);
            if (locale == null) {
                continue;
            }
            CLDRFile cldrFile = diskFac.make(loc, true);
            XMLSource diskData = cldrFile.getResolvingDataSource();
            DisplayAndInputProcessor daip = new DisplayAndInputProcessor(locale, false);
            if (value != null) {
                value = daip.processInput(xpathString, value, null);
            }
            try {
                String curValue = diskData.getValueAtDPath(xpathString);
                if (curValue == null) {
                    continue;
                }
                if (valueCanBeAutoImported(value, curValue, cldrFile, xpathString, loc)) {
                    BallotBox<User> box = stFac.ballotBoxForLocale(locale);
                    /*
                     * Only import the most recent vote (or abstention) for the given user and xpathString.
                     * Skip if user already has a vote for this xpathString (with ANY value).
                     * Since we're going through tables in reverse chronological order, "already" here implies
                     * "for a later version".
                     */
                    if (box.getVoteValue(user, xpathString) == null) {
                        box.voteForValueWithType(user, xpathString, value, VoteType.AUTO_IMPORT);
                        confirmations++;
                    }
                }
            } catch (InvalidXPathException ix) {
                /* Silently catch InvalidXPathException, otherwise logs grow too fast with useless warnings */
            } catch (VoteNotAcceptedException ix) {
                /* Silently catch VoteNotAcceptedException, otherwise logs grow too fast with useless warnings */
            } catch (IllegalByDtdException ix) {
                /* Silently catch IllegalByDtdException, otherwise logs grow too fast with useless warnings */
            }
        }
        return confirmations;
    }

    /**
     * Can the value be auto-imported?
     *
     * <p>Import if the value is null (abstain), or is winning (equalsOrInheritsCurrentValue) and is
     * not a code-copy failure (sameAsCode).
     *
     * <p>By importing null (abstain) votes, we fix a problem where, for example, the user voted for
     * a value "x" in one old version, then voted to abstain in a later version, and then the "x"
     * still got imported into an even later version.
     *
     * @param value the value in question
     * @param curValue the current value, that is, getValueAtDPath(xpathString)
     * @param cldrFile the resolved disk-based CLDRFile for equalsOrInheritsCurrentValue and
     *     CheckForCopy.sameAsCode
     * @param xpathString the path identifier
     * @param loc the locale string
     * @return true if OK to import, else false
     */
    private boolean valueCanBeAutoImported(
            String value, String curValue, CLDRFile cldrFile, String xpathString, String loc) {
        if (skipLocForImportingVotes(loc)) {
            return false;
        }
        if (value == null) {
            return true;
        }
        if (!cldrFile.equalsOrInheritsCurrentValue(value, curValue, xpathString)) {
            return false;
        }
        if (CheckForCopy.sameAsCode(value, xpathString, cldrFile.getUnresolved(), cldrFile)) {
            return false;
        }
        return true;
    }

    private boolean skipLocForImportingVotes(String loc) {
        if ("und".equals(loc) || "root".equals(loc)) {
            return true; // skip
        }
        return false;
    }

    /**
     * Transfer all the old votes from one user to another, for all old votes tables.
     *
     * <p>Called when the user presses button "Transfer Old Votes" in the Users page which may be
     * reached by a URL such as .../cldr-apps/v#users///
     *
     * <p>users.js uses "user_xferoldvotes" as follows:
     *
     * <p>var xurl3 = contextPath +
     * "/SurveyAjax?&s="+surveySessionId+"&what=user_xferoldvotes&from_user_id="+oldUser.data.id+"&from_locale="+oldLocale+"&to_user_id="+u.data.id+"&to_locale="+newLocale;
     *
     * <p>Message displayed in dialog in response to button press: "First, pardon the modality.
     * Next, do you want to import votes to '#1921 u_1921@adlam.example.com' FROM another user's old
     * votes? Enter their email address below:"
     *
     * @param request the HttpServletRequest, for parameters from_user_id, to_user_id, from_locale,
     *     to_locale
     * @param sm the SurveyMain, for sm.reg and newJSONStatusQuick
     * @param user the current User, who needs admin rights
     * @param r the SurveyJSONWrapper to be written to
     * @throws SurveyException
     * @throws JSONException
     */
    private void transferOldVotes(
            SurveyJSONWrapper r, HttpServletRequest request, SurveyMain sm, UserRegistry.User user)
            throws SurveyException, JSONException {
        Integer from_user_id = getIntParameter(request, "from_user_id");
        Integer to_user_id = getIntParameter(request, "to_user_id");
        String from_locale = request.getParameter("from_locale");
        String to_locale = request.getParameter("to_locale");
        if (from_user_id == null
                || to_user_id == null
                || from_locale == null
                || to_locale == null) {
            throw new SurveyException(ErrorCode.E_INTERNAL, "Missing parameter");
        }
        final User toUser = sm.reg.getInfo(to_user_id);
        final User fromUser = sm.reg.getInfo(from_user_id);
        if (toUser == null || fromUser == null) {
            throw new SurveyException(ErrorCode.E_INTERNAL, "Invalid user parameter");
        }
        /* TODO: replace deprecated isAdminFor with ...? */
        if (user.isAdminForOrg(user.org) && user.isAdminFor(toUser)) {
            transferOldVotesGivenUsersAndLocales(
                    r, from_user_id, to_user_id, from_locale, to_locale);
            alreadyAutoImportedVotes(to_user_id, "clear"); // repeat auto-import as needed
        } else {
            throw new SurveyException(
                    ErrorCode.E_NO_PERMISSION, "You do not have permission to do this.");
        }
    }

    /**
     * Transfer all the old votes from one user to another, having validated the users and locales.
     *
     * <p>Loop thru multiple old votes tables in reverse chronological order.
     *
     * @param r the SurveyJSONWrapper to be written to
     * @param from_user_id the id of the user from which to transfer
     * @param to_user_id the id of the user to which to transfer
     * @param from_locale the locale from which to transfer
     * @param to_locale the locale to which to transfer
     * @throws JSONException
     */
    private void transferOldVotesGivenUsersAndLocales(
            SurveyJSONWrapper r,
            Integer from_user_id,
            Integer to_user_id,
            String from_locale,
            String to_locale)
            throws JSONException {
        int totalVotesTransferred = 0;
        String oldVotesTableList = "";
        int ver = Integer.parseInt(SurveyMain.getNewVersion());
        while (--ver >= oldestVersionForImportingVotes) {
            String oldVotesTable =
                    DBUtils.Table.VOTE_VALUE
                            .forVersion(Integer.valueOf(ver).toString(), false)
                            .toString();
            if (DBUtils.hasTable(oldVotesTable)) {
                Connection conn = null;
                PreparedStatement ps = null;
                try {
                    conn = DBUtils.getInstance().getDBConnection();
                    String add0 = "";
                    if (DBUtils.db_Mysql) {
                        add0 = "IGNORE"; // ignore duplicate errs, we want to transfer as much as
                        // we can
                    }
                    ps =
                            DBUtils.prepareStatementWithArgs(
                                    conn,
                                    "INSERT "
                                            + add0
                                            + " INTO "
                                            + oldVotesTable
                                            + " (locale, xpath, submitter, value, last_mod) "
                                            + " SELECT ? as locale, "
                                            + oldVotesTable
                                            + ".xpath as xpath, ? as submitter, "
                                            + oldVotesTable
                                            + ".value as value, "
                                            + oldVotesTable
                                            + ".last_mod as last_mod "
                                            + "FROM "
                                            + oldVotesTable
                                            + " WHERE ?="
                                            + oldVotesTable
                                            + ".submitter AND "
                                            + oldVotesTable
                                            + ".locale=?",
                                    to_locale,
                                    to_user_id,
                                    from_user_id,
                                    from_locale);
                    int count = ps.executeUpdate();
                    if (count > 0) {
                        totalVotesTransferred += count;
                        if (!oldVotesTableList.isEmpty()) {
                            oldVotesTableList += ", ";
                        }
                        oldVotesTableList += oldVotesTable;
                    }
                    conn.commit();
                } catch (SQLException e) {
                    // "Duplicate entry" may occur. Catch here rather than abort entire transfer.
                    SurveyLog.logException(
                            logger, e, "SQL exception: transferring votes in " + oldVotesTable);
                } finally {
                    DBUtils.close(ps, conn);
                }
            }
        }
        final JSONObject o = new JSONObject();
        o.put("from_user_id", from_user_id);
        o.put("from_locale", from_locale);
        o.put("to_user_id", to_user_id);
        o.put("to_locale", to_locale);
        o.put("result_count", totalVotesTransferred);
        o.put("tables", oldVotesTableList);
        r.put(WHAT_USER_XFEROLDVOTES, o);
    }

    /**
     * View stats for old votes for the user whose id is specified in the request.
     *
     * <p>We get here when the user presses button "View Old Vote Stats" in the Users page which may
     * be reached by a URL such as .../cldr-apps/v#users///
     *
     * <p>users.js uses "user_oldvotes" as follows:
     *
     * <p>var xurl2 = contextPath +
     * "/SurveyAjax?&s="+surveySessionId+"&what=user_oldvotes&old_user_id="+u.data.id;
     *
     * @param request the HttpServletRequest, for parameter old_user_id
     * @param sm the SurveyMain, for sm.reg and newJSONStatusQuick
     * @param user the current User, who needs admin rights
     * @param r the SurveyJSONWrapper to be written to
     * @throws SQLException
     * @throws JSONException
     * @throws SurveyException
     * @throws IOException
     */
    private void viewOldVoteStats(
            SurveyJSONWrapper r, HttpServletRequest request, SurveyMain sm, User user)
            throws SQLException, JSONException, SurveyException, IOException {

        String u = request.getParameter("old_user_id");
        if (u == null) {
            throw new SurveyException(ErrorCode.E_INTERNAL, "Missing parameter 'old_user_id'");
        }
        int old_user_id = Integer.parseInt(u);
        if (user.isAdminForOrg(user.org) && user.isAdminFor(sm.reg.getInfo(old_user_id))) {
            viewOldVoteStatsForOldUserId(r, old_user_id);
        } else {
            throw new SurveyException(
                    ErrorCode.E_NO_PERMISSION, "You do not have permission to list users.");
        }
    }

    /**
     * View stats for old votes for the user whose id is specified.
     *
     * <p>Check multiple old votes tables.
     *
     * @param r the SurveyJSONWrapper to be written to
     * @throws SQLException
     * @throws JSONException
     * @throws IOException
     */
    private void viewOldVoteStatsForOldUserId(SurveyJSONWrapper r, Integer old_user_id)
            throws SQLException, JSONException, IOException {

        JSONObject tables = new JSONObject();
        int ver = Integer.parseInt(SurveyMain.getNewVersion());
        while (--ver >= oldestVersionForImportingVotes) {
            String oldVotesTable =
                    DBUtils.Table.VOTE_VALUE.forVersion(Integer.toString(ver), false).toString();
            if (DBUtils.hasTable(oldVotesTable)) {
                JSONObject o =
                        DBUtils.queryToJSON(
                                "select COUNT(xpath), locale from "
                                        + oldVotesTable
                                        + " where submitter=? group by locale order by locale",
                                old_user_id);
                String d = o.getString("data");
                if (!d.equals("[]")) {
                    tables.put("v" + ver, d);
                }
            }
        }
        JSONObject data = new JSONObject();
        if (tables.length() > 0) {
            data.put("data", tables);
        }
        r.put("user_oldvotes", data); // WHAT_USER_OLDVOTES
        r.put("old_user_id", old_user_id);
    }

    /**
     * Handle bulk submission upload when user chooses "Upload XML" from the gear menu.
     *
     * <p>Compare submitVoteOrAbstention which is called when user makes an individual vote.
     *
     * @param request the HttpServletRequest
     * @param response the HttpServletResponse
     * @param out the Writer
     * @throws IOException
     * @throws JSONException
     * @throws VoteNotAcceptedException
     * @throws InvalidXPathException
     *     <p>Some code was moved here from submit.jsp Reference:
     *     https://unicode-org.atlassian.net/browse/CLDR-11877
     *     <p>TODO: separate web-specific code (that uses HttpServletRequest, HttpServletResponse,
     *     SurveyMain, ...) (to keep here in cldr-apps) from html-producing code (to move into
     *     util/XMLUploader.java), for encapsulation and to enable unit testing for html-producing
     *     code.
     *     <p>CAUTION: this may be called by jsp
     */
    public static void handleBulkSubmit(
            HttpServletRequest request, HttpServletResponse response, Writer out)
            throws IOException, JSONException, InvalidXPathException, VoteNotAcceptedException {
        String contextPath = request.getContextPath();
        String sid = request.getParameter("s");
        if (!request.getMethod().equals("POST") || (sid == null)) {
            response.sendRedirect(contextPath + "/upload.jsp");
            return;
        }

        String email = request.getParameter("email");
        final CookieSession cs = CookieSession.retrieve(sid);
        if (cs == null || cs.user == null) {
            response.sendRedirect(contextPath + "/survey");
            return;
        }
        cs.userDidAction(); // mark user as not idle
        final SurveyMain sm = CookieSession.sm;
        UserRegistry.User theirU = sm.reg.get(email.trim());
        if (theirU == null || (!theirU.equals(cs.user) && !cs.user.isAdminFor(theirU))) {
            response.sendRedirect(
                    contextPath
                            + "/upload.jsp?s="
                            + sid
                            + "&email="
                            + email.trim()
                            + "&emailbad=t");
            return;
        }
        final String submitButtonText = "NEXT: Submit as " + theirU.email;

        String ident = "";
        if (theirU.id != cs.user.id) {
            ident = "&email=" + theirU.email + "&pw=" + sm.reg.getPassword(null, theirU.id);
        }

        boolean doFinal = (request.getParameter("dosubmit") != null);

        String title = "Submitted as " + theirU.email;

        if (!doFinal) {
            title = title + " <i>(Trial)</i>";
        }

        CLDRFile cf = (CLDRFile) cs.get("SubmitLocale");
        out.write("<html>\n<head>\n");
        out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n");
        out.write("<title>SurveyTool File Submission | " + title + "</title>\n");
        out.write("<link rel='stylesheet' type='text/css' href='./surveytool.css' />\n");

        SurveyTool.includeJavaScript(request, out);

        out.write("</head>\n<body style='padding: 0.5em'>\n");
        out.write(
                "<a href=\"upload.jsp?s="
                        + sid
                        + "&email="
                        + theirU.email
                        + "\">Re-Upload File/Try Another</a>");
        out.write(" | ");
        out.write(
                "<a href=\""
                        + contextPath
                        + "v#upload\">Return to the SurveyTool <img src='STLogo.png' style='float: right;' />");
        out.write("</a>\n");
        out.write("<hr />");
        out.write("<h3>\n");
        out.write("SurveyTool File Submission");
        out.write(" | ");
        out.write(title);
        out.write(" | ");
        out.write(theirU.name);
        out.write("</h3>\n");

        out.write("<i>Checking upload...</i>");

        final CLDRLocale loc = CLDRLocale.getInstance(cf.getLocaleID());
        if (!ident.isEmpty()) {
            out.write("<div class='fnotebox'>");
            out.write("Note: Clicking the following links will switch to the user " + theirU.email);
            out.write("</div>");
        }

        out.write("<h3>\n");
        out.write("Locale:" + loc + " - " + loc.getDisplayName(SurveyMain.TRANS_HINT_LOCALE));
        out.write("</h3>\n");

        STFactory stf = sm.getSTFactory();
        CLDRFile baseFile = stf.make(loc.getBaseName(), false);

        Set<String> all = new TreeSet<>();
        for (String x : cf) {
            if (x.startsWith("//ldml/identity")) {
                continue;
            }
            all.add(x);
        }
        int updCnt = 0;
        out.write("<h4>\n");
        out.write("Please review these " + all.size() + " entries.");
        out.write("</h4>\n");

        final String bulkStage = doFinal ? "submit" : "test";
        XMLUploader.writeBulkInfoHtml(bulkStage, out);

        if (doFinal) {
            out.write("<div class='bulkNextButton'>\n");
            out.write("<b>Submitted!</b><br/>\n");
            out.write(
                    "<a href=\"upload.jsp?s="
                            + sid
                            + "&email="
                            + theirU.email
                            + "\">Another?</a>\n");
            out.write("</div>\n");
        }

        out.write("<div class='helpHtml'>\n");
        if (!doFinal) {
            out.write(
                    "Please review these items carefully. The \"NEXT\" button will not appear until the page fully loads. Pressing NEXT will submit these votes.\n");
        } else {
            out.write("Items listed have been submitted as " + theirU.email);
        }
        out.write("<br>\n");
        out.write(
                "For help, see: <a target='CLDR-ST-DOCS' href='https://cldr.unicode.org/index/survey-tool/bulk-data-upload'>Using Bulk Upload</a>\n");
        out.write("</div>\n");

        out.write("<table class='data'>\n");
        out.write("<thead>\n");
        out.write("<tr>\n");
        out.write("<th>xpath</th>\n");
        out.write("<th>My Value</th>\n");
        out.write("<th>Comment</th>\n");
        out.write("</tr>\n");
        out.write("</thead>\n");

        DisplayAndInputProcessor processor = DisplayAndInputProcessorFactory.make(loc);
        BallotBox<UserRegistry.User> ballotBox = stf.ballotBoxForLocale(loc);

        int r = 0;
        final List<CheckCLDR.CheckStatus> checkResult = new ArrayList<>();
        TestCache.TestResultBundle cc = stf.getTestResult(loc, DataPage.getOptions(cs, loc));
        UserRegistry.User u = theirU;
        CheckCLDR.Phase cPhase = CLDRConfig.getInstance().getPhase();
        Set<String> allValidPaths = stf.getPathsForFile(loc);
        CLDRProgressTask progress = sm.openProgress("Bulk:" + loc, all.size());
        CLDRFile cldrUnresolved = cf.getUnresolved();
        try {
            for (String x : all) {
                String full = cf.getFullXPath(x);
                XPathParts xppMine =
                        XPathParts.getFrozenInstance(full)
                                .cloneAsThawed(); // not frozen, for xPathPartsToBase
                String valOrig = cf.getStringValue(x);
                Exception[] exc = new Exception[1];
                final String val0 = processor.processInput(x, valOrig, exc);
                XPathTable.xPathPartsToBase(xppMine);
                xppMine.removeAttribute(-1, LDMLConstants.DRAFT);
                String base = xppMine.toString();
                int base_xpath_id = sm.xpt.getByXpath(base);

                String valb = baseFile.getWinningValue(base);

                String style;
                if (valb == null) {
                    style = "background-color: #bfb;";
                } else if (!val0.equals(valb)) {
                    style = "font-weight: bold; background-color: #bfb;";
                } else {
                    style = "opacity: 0.9;";
                }

                XPathParts xpp =
                        XPathParts.getFrozenInstance(base)
                                .cloneAsThawed(); // not frozen, for removeAttribute
                xpp.removeAttribute(-1, LDMLConstants.ALT);

                String result;
                String resultStyle = "";
                String resultIcon = "okay";

                PathHeader ph = stf.getPathHeader(base);

                if (!allValidPaths.contains(base)) {
                    result = "Item is not a valid XPath.";
                    resultIcon = "stop";
                } else if (ph == null) {
                    result = "Item is not a SurveyTool-visible LDML entity.";
                    resultIcon = "stop";
                } else {
                    checkResult.clear();
                    cc.check(base, checkResult, val0);

                    DataPage page = DataPage.make(null, cs, loc, base, null, null);
                    page.setUserForVotelist(cs.user);

                    DataPage.DataRow pvi = page.getDataRow(base);
                    CheckCLDR.StatusAction showRowAction =
                            pvi.getStatusAction(CheckCLDR.InputMethod.BULK);

                    if (CheckForCopy.sameAsCode(val0, x, cldrUnresolved, baseFile)) {
                        showRowAction = CheckCLDR.StatusAction.FORBID_CODE;
                    }
                    if (showRowAction.isForbidden()) {
                        result = "Item may not be modified. (" + showRowAction + ")";
                        resultIcon = "stop";
                    } else {
                        CandidateInfo ci;
                        if (val0 == null) {
                            ci = null; // abstention
                        } else {
                            ci = pvi.getItem(val0); // existing item?
                            if (ci == null) { // no, new item
                                ci =
                                        new CandidateInfo() {
                                            @Override
                                            public String getValue() {
                                                return val0;
                                            }

                                            @Override
                                            public Collection<UserInfo> getUsersVotingOn() {
                                                return Collections
                                                        .emptyList(); // No users voting - yet.
                                            }

                                            @Override
                                            public List<CheckCLDR.CheckStatus>
                                                    getCheckStatusList() {
                                                return checkResult;
                                            }
                                        };
                            }
                        }
                        CheckCLDR.StatusAction status =
                                cPhase.getAcceptNewItemAction(
                                        ci, pvi, CheckCLDR.InputMethod.BULK, ph, cs.user);

                        if (status != CheckCLDR.StatusAction.ALLOW) {
                            result = "Item will be skipped. (" + status + ")";
                            resultIcon = "stop";
                        } else {
                            if (doFinal) {
                                ballotBox.voteForValueWithType(u, base, val0, VoteType.BULK_UPLOAD);
                                result = "Vote accepted";
                                resultIcon = "vote";
                            } else {
                                result = "Ready to submit.";
                            }
                            updCnt++;
                        }
                    }
                }

                out.write("<tr class='r" + (r) % 2 + "'>\n");
                out.write(
                        "<th title='"
                                + base
                                + " #"
                                + base_xpath_id
                                + "'"
                                + " style='text-align: left; font-size: smaller;'>"
                                + "<a target='"
                                + WebContext.TARGET_ZOOMED
                                + "'"
                                + "href='"
                                + contextPath
                                + "/survey?_="
                                + loc
                                + "&strid="
                                + sm.xpt.getStringIDString(base_xpath_id)
                                + ident
                                + "'>"
                                + ph.toString()
                                + "</a>");
                out.write("<br>");
                out.write("<tt>" + base + "</tt></th>\n");

                out.write("<td style='" + style + "'>" + val0 + "\n");
                if (!val0.equals(valOrig)) {
                    out.write("<div class='graybox' title='original text'>" + valOrig + "</div>\n");
                }
                out.write("</td>\n");
                out.write("<td title='vote:' style='" + resultStyle + "'>\n");
                if (!checkResult.isEmpty()) {
                    out.write("<script>\n");
                    String testsToHtml = "testsToHtml";
                    out.write(
                            "document.write("
                                    + testsToHtml
                                    + "("
                                    + SurveyJSONWrapper.wrap(checkResult)
                                    + ")");
                    out.write("</script>\n");
                }
                out.write(WebContext.iconHtml(request, resultIcon, result) + result);
                out.write("</tr>\n");
            }
        } finally {
            progress.close();
        }

        out.write("</table>\n");
        out.write("<hr />\n");
        if (doFinal) {
            out.write("Voted on " + updCnt + " vote(s).");
        } else if (updCnt > 0) {
            out.write("Ready to submit " + updCnt + " vote(s).");
        } else {
            out.write("<div class='helpHtml'>No votes were eligible for bulk submit. ");
            out.write(
                    "<a href=\"upload.jsp?s="
                            + sid
                            + "&email="
                            + theirU.email
                            + "\">Re-Upload File/Try Another</a>");
            out.write("</div>");
        }
        if (!doFinal && updCnt > 0) {
            out.write(
                    "<form action='"
                            + contextPath
                            + request.getServletPath()
                            + "' method='POST'>\n");
            out.write("<input type='hidden' name='s' value='" + sid + "' />\n");
            out.write("<input type='hidden' name='email' value='" + email + "' />");
            out.write(
                    "<input class='bulkNextButton' type='submit' name='dosubmit' value='"
                            + submitButtonText
                            + "' />");
            out.write("</form>\n");
        }
        out.write("</body>\n</html>\n");
    }

    /**
     * Generate a report, such as Date/Times or Dashboard.
     *
     * @param request the HttpServletRequest
     * @param response the HttpServletResponse
     * @param out the Writer
     * @param sm the SurveyMain
     * @param sess the session id
     * @param l the CLDRLocale
     * @throws IOException
     *     <p>Some code was moved to this function and those it calls, from EmbeddedReport.jsp and
     *     other jsp files. Reference: https://unicode-org.atlassian.net/browse/CLDR-13152
     */
    private static void generateReport(
            HttpServletRequest request,
            HttpServletResponse response,
            Writer out,
            SurveyMain sm,
            String sess,
            CLDRLocale l)
            throws IOException {

        CookieSession cs = CookieSession.retrieve(sess);
        if (cs == null) {
            response.setContentType("text/html");
            out.write("<b>Invalid or expired session (try reloading the page)</b>");
            return;
        }
        String which = request.getParameter("x");

        response.setContentType("text/html");
        generateReport(which, out, sm, l);
    }

    /** Hook to generate one of the 'old three' reports. */
    public static void generateReport(final String which, Writer out, SurveyMain sm, CLDRLocale l)
            throws IOException {
        if ("r_datetime".equals(which)) {
            generateDateTimesReport(out, sm, l);
        } else if ("r_zones".equals(which)) {
            generateZonesReport(out, sm, l);
        } else if ("r_compact".equals(which)) {
            generateNumbersReport(out, sm, l);
        } else {
            out.write("<i>Unrecognized report name: " + which + "</i><br/>\n");
        }
    }

    /**
     * Generate the Date/Times report, as html
     *
     * @param out the Writer
     * @param sm the SurveyMain
     * @param l the CLDRLocale
     * @throws IOException
     */
    private static void generateDateTimesReport(Writer out, SurveyMain sm, CLDRLocale l)
            throws IOException {

        final String calendarType = "gregorian";
        final String title =
                com.ibm.icu.lang.UCharacter.toTitleCase(
                        SurveyMain.TRANS_HINT_LOCALE.toLocale(), calendarType, null);

        out.write("<h3>Calendar : " + title + "</h3>");

        STFactory fac = sm.getSTFactory();
        CLDRFile englishFile = fac.make("en", true);
        CLDRFile nativeFile = fac.make(l, true);

        DateTimeFormats formats = new DateTimeFormats().set(nativeFile, calendarType);
        DateTimeFormats english = new DateTimeFormats().set(englishFile, calendarType);

        formats.addTable(english, out);
        formats.addDateTable(englishFile, out);
        formats.addDayPeriods(englishFile, out);
    }

    /**
     * Generate the Zones report, as html
     *
     * @param out the Writer
     * @param sm the SurveyMain
     * @param l the CLDRLocale
     * @throws IOException
     */
    private static void generateZonesReport(Writer out, SurveyMain sm, CLDRLocale l)
            throws IOException {
        CLDRFile englishFile = sm.getDiskFactory().make("en", true);
        CLDRFile nativeFile = sm.getSTFactory().make(l, true);

        org.unicode.cldr.util.VerifyZones.showZones(null, englishFile, nativeFile, out);
    }

    /**
     * Generate the Numbers report, as html
     *
     * @param out the Writer
     * @param sm the SurveyMain
     * @param l the CLDRLocale
     */
    private static void generateNumbersReport(Writer out, SurveyMain sm, CLDRLocale l) {
        STFactory fac = sm.getSTFactory();
        CLDRFile nativeFile = fac.make(l, true);

        org.unicode.cldr.util.VerifyCompactNumbers.showNumbers(nativeFile, true, "EUR", out, fac);
    }

    /**
     * Serve information about the values for the given path in locales related to ("sideways from")
     * this locale. Locales may be related or "sideways" if, for example, they are all sublocales of
     * the same locale.
     *
     * @param r the SurveyJSONWrapper to be filled in with the information
     * @param sm the SurveyMain
     * @param locale the CLDRLocale of interest
     * @param xpath the path of interest
     * @throws JSONException
     */
    private void getSidewaysLocales(
            SurveyJSONWrapper r, SurveyMain sm, CLDRLocale locale, String xpath)
            throws JSONException {

        final String xpathString = sm.xpt.getByStringID(xpath);
        if (xpathString == null) {
            throw new IllegalArgumentException("could not find strid: " + xpath);
        }
        JSONObject others = new JSONObject(); // values
        JSONArray novalue = new JSONArray(); // no value
        final CLDRLocale topLocale = locale.getHighestNonrootParent();
        if (topLocale != null) {
            final Collection<CLDRLocale> relatedLocs =
                    sm.getRelatedLocs(topLocale); // sublocales of the 'top' locale
            for (CLDRLocale ol : relatedLocs) {
                String baseName = ol.getBaseName();
                CLDRFile src = sm.getSTFactory().make(baseName, true /* resolved */);
                String ov = src.getStringValue(xpathString);
                if (ov != null) {
                    JSONArray other;
                    if (others.has(ov)) {
                        other = others.getJSONArray(ov);
                    } else {
                        other = new JSONArray();
                        others.put(ov, other);
                    }
                    other.put(baseName);
                } else {
                    novalue.put(baseName);
                }
            }
        }
        r.put("what", WHAT_GETSIDEWAYS);
        r.put("loc", locale.toString());
        r.put("xpath", xpath);
        r.put("topLocale", topLocale);
        r.put("others", others);
        r.put("novalue", novalue);
    }
}
