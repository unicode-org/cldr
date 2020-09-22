//
//  SurveyForum.java
//
//  Created by Steven R. Loomis on 27/10/2006.
//  Copyright 2006-2013 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRURLS;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.web.SurveyException.ErrorCode;
import org.unicode.cldr.web.UserRegistry.User;

import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.ULocale;

/**
 * This class implements a discussion forum per language (ISO code)
 */
public class SurveyForum {

    private static final String FLAGGED_FOR_REVIEW_HTML = " <p>[This item was flagged for CLDR TC review.]";

    private static java.util.logging.Logger logger;

    private static String DB_FORA = "sf_fora"; // forum name -> id

    private static String DB_LOC2FORUM = "sf_loc2forum"; // locale -> forum.. for selects.

    private static final String F_FORUM = "forum";

    public static final String F_XPATH = "xpath";

    /**
     * Make an "html-safe" version of the given string
     *
     * @param s
     * @return the possibly-modified string
     */
    public static String HTMLSafe(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;");
    }

    /**
     * Oops. HTML escaped into the DB
     *
     * @param s
     * @return the possibly-modified string
     */
    private static String HTMLUnsafe(String s) {
        return s.replaceAll("<p>", "\n")
            .replaceAll("&quot;", "\"")
            .replaceAll("&gt;", ">")
            .replaceAll("&lt;", "<")
            .replaceAll("&amp;", "&");
    }

    private Hashtable<String, Integer> nameToNum = new Hashtable<>();

    private static final int BAD_FORUM = -1;
    private static final int NO_FORUM = -2;

    /**
     * A post with this value for "parent" is the first post in its
     * thread; that is, it has no real parent post.
     */
    public static final int NO_PARENT = -1;

    private synchronized int getForumNumber(CLDRLocale locale) {
        String forum = localeToForum(locale);
        if (forum.length() == 0) {
            return NO_FORUM; // all forums
        }
        // make sure it is a valid src!
        if ((forum == null) || (forum.indexOf('_') >= 0) || !sm.isValidLocale(CLDRLocale.getInstance(forum))) {
            return BAD_FORUM;
        }
        Integer i = nameToNum.get(forum);
        if (i == null) {
            return createForum(forum);
        } else {
            return i.intValue();
        }
    }

    private int getForumNumberFromDB(String forum) {
        try {
            Connection conn = null;
            PreparedStatement fGetByLoc = null;
            try {
                conn = sm.dbUtils.getDBConnection();
                fGetByLoc = prepare_fGetByLoc(conn);
                fGetByLoc.setString(1, forum);
                ResultSet rs = fGetByLoc.executeQuery();
                if (!rs.next()) {
                    rs.close();
                    return BAD_FORUM;
                } else {
                    int j = rs.getInt(1);
                    rs.close();
                    return j;
                }
            } finally {
                DBUtils.close(fGetByLoc, conn);
            }
        } catch (SQLException se) {
            String complaint = "SurveyForum:  Couldn't add forum " + forum + " - " + DBUtils.unchainSqlException(se)
                + " - fGetByLoc";
            logger.severe(complaint);
            throw new RuntimeException(complaint);
        }
    }

    /**
     *
     * @param forum
     * @return the forum number
     *
     * Called only by getForumNumber.
     */
    private int createForum(String forum) {
        int num = getForumNumberFromDB(forum);
        if (num == BAD_FORUM) {
            try {
                Connection conn = null;
                PreparedStatement fAdd = null;
                try {
                    conn = sm.dbUtils.getDBConnection();
                    fAdd = prepare_fAdd(conn);
                    fAdd.setString(1, forum);
                    fAdd.executeUpdate();
                    conn.commit();
                } finally {
                    DBUtils.close(fAdd, conn);
                }
            } catch (SQLException se) {
                String complaint = "SurveyForum:  Couldn't add forum " + forum + " - " + DBUtils.unchainSqlException(se)
                    + " - fAdd";
                logger.severe(complaint);
                throw new RuntimeException(complaint);
            }
            num = getForumNumberFromDB(forum);
        }

        if (num == BAD_FORUM) {
            throw new RuntimeException("Couldn't query ID for forum " + forum);
        }
        // Add to list
        Integer i = new Integer(num);
        nameToNum.put(forum, i);
        return num;
    }

    private int gatherInterestedUsers(String forum, Set<Integer> cc_emails, Set<Integer> bcc_emails) {
        int emailCount = 0;
        try {
            Connection conn = null;
            PreparedStatement pIntUsers = null;
            try {
                conn = sm.dbUtils.getDBConnection();
                pIntUsers = prepare_pIntUsers(conn);
                pIntUsers.setString(1, forum);

                ResultSet rs = pIntUsers.executeQuery();

                while (rs.next()) {
                    int uid = rs.getInt(1);

                    UserRegistry.User u = sm.reg.getInfo(uid);
                    if (u != null && u.email != null && u.email.length() > 0
                            && !(UserRegistry.userIsLocked(u) || UserRegistry.userIsExactlyAnonymous(u))) {
                        if (UserRegistry.userIsVetter(u)) {
                            cc_emails.add(u.id);
                        } else {
                            bcc_emails.add(u.id);
                        }
                        emailCount++;
                    }
                }
            } finally {
                DBUtils.close(pIntUsers, conn);
            }
        } catch (SQLException se) {
            String complaint = "SurveyForum:  Couldn't gather interested users for " + forum + " - "
                + DBUtils.unchainSqlException(se) + " - pIntUsers";
            logger.severe(complaint);
            throw new RuntimeException(complaint);
        }

        return emailCount;
    }

    /**
     * Send email notification to a set of users
     *
     * @param ctx
     * @param forum
     * @param base_xpath
     * @param subj
     * @param text
     * @param postId
     *
     * Called by doPostInternal
     */
    private void emailNotify(UserRegistry.User user, CLDRLocale locale, int base_xpath, String subj, String text, Integer postId) {
        String forum = localeToForum(locale);
        ElapsedTimer et = new ElapsedTimer("Sending email to " + forum);
        // Do email-
        Set<Integer> cc_emails = new HashSet<>();
        Set<Integer> bcc_emails = new HashSet<>();

        // Collect list of users to send to.
        gatherInterestedUsers(forum, cc_emails, bcc_emails);

        String subject = "CLDR forum post (" + locale.getDisplayName() + " - " + locale + "): " + subj;

        String body = "Do not reply to this message, instead go to <"
            + CLDRConfig.getInstance().absoluteUrls().forSpecial(CLDRURLS.Special.Forum, locale, (String) null, Integer.toString(postId))

            + ">\n====\n\n"
            + text;

        if (MailSender.getInstance().DEBUG) {
            System.out.println(et + ": Forum notify: u#" + user.id + " x" + base_xpath + " queueing cc:" + cc_emails.size() + " and bcc:" + bcc_emails.size());
        }

        MailSender.getInstance().queue(user.id, cc_emails, bcc_emails, HTMLUnsafe(subject), HTMLUnsafe(body), locale, base_xpath, postId);
    }

    /**
     * Is the current user allowed to post with the given PostType in this context?
     * This was already checked on the client, but don't trust the client too much.
     * Check on server as well, at least to prevent someone closing a post who shouldn't be allowed to.
     *
     * @param user the current user
     * @param postType the PostType
     * @param replyTo the post id of the parent, or NO_PARENT
     * @return true or false
     *
     * @throws SurveyException
     */
    private boolean userCanUsePostType(User user, PostType postType, int replyTo) throws SurveyException {
        if (SurveyMain.isPhaseReadonly()) {
            return false;
        }
        if (postType != PostType.CLOSE) {
            return true;
        }
        if (replyTo == NO_PARENT) {
            return false; // first post can't begin as closed
        }
        if (getFirstPosterInThread(replyTo) == user.id) {
            return true;
        }
        if (UserRegistry.userIsTC(user)) {
            return true;
        }
        return false;
    }

    /**
     * Get the user id of the first poster in the thread containing this post
     *
     * @param postId
     * @return the user id, or UserRegistry.NO_USER
     *
     * @throws SurveyException
     */
    private int getFirstPosterInThread(int postId) throws SurveyException {
        int posterId = UserRegistry.NO_USER;
        Connection conn = null;
        PreparedStatement pList = null;
        try {
            conn = sm.dbUtils.getDBConnection();
            pList = DBUtils.prepareStatement(conn, "pList", "SELECT parent,poster FROM " + DBUtils.Table.FORUM_POSTS.toString()
                + " WHERE id=?");
            for (;;) {
                pList.setInt(1, postId);
                ResultSet rs = pList.executeQuery();
                int parentId = NO_PARENT;
                while (rs.next()) {
                    parentId = rs.getInt(1);
                    posterId = rs.getInt(2);
                }
                if (parentId == NO_PARENT) {
                    break;
                }
                postId = parentId;
            }
         } catch (SQLException se) {
            String complaint = "SurveyForum: Couldn't get parent for post - " + DBUtils.unchainSqlException(se);
            SurveyLog.logException(se, complaint);
            throw new SurveyException(ErrorCode.E_INTERNAL, complaint);
        } finally {
            DBUtils.close(pList, conn);
        }
        return posterId;
    }

    /**
     * If this user posts to the forum, will it cause this xpath+locale to be flagged
     * (if not already flagged)?
     *
     * Return true if the user has made a losing vote, and the VoteResolver.canFlagOnLosing
     * (i.e., path is locked and/or requires VoteResolver.HIGH_BAR votes).
     *
     * @param user
     * @param xpath
     * @param locale
     * @return true or false
     */
    private boolean couldFlagOnLosing(UserRegistry.User user, String xpath, CLDRLocale locale) {
        BallotBox<User> bb = sm.getSTFactory().ballotBoxForLocale(locale);
        if (bb.userDidVote(user, xpath)) {
            String userValue = bb.getVoteValue(user, xpath);
            if (userValue != null) {
                VoteResolver<String> vr = bb.getResolver(xpath);
                if (!userValue.equals(vr.getWinningValue())) {
                    return vr.canFlagOnLosing();
                }
            }
        }
        return false;
    }

    /**
     * Called by SM to create the reg
     *
     * @param xlogger the logger to use
     * @param ourConn the conn to use
     * @return the SurveyForum
     */
    public static SurveyForum createTable(java.util.logging.Logger xlogger, Connection ourConn, SurveyMain sm)
        throws SQLException {
        SurveyForum reg = new SurveyForum(xlogger, sm);
        try {
            reg.setupDB(ourConn); // always call - we can figure it out.
        } finally {
            DBUtils.closeDBConnection(ourConn);
        }
        return reg;
    }

    private SurveyForum(java.util.logging.Logger xlogger, SurveyMain ourSm) {
        logger = xlogger;
        sm = ourSm;
    }

    private Date oldOnOrBefore = null;

    /**
     * Re-create DB_LOC2FORUM table from scratch, called at start-up
     *
     * @param conn
     * @throws SQLException
     */
    private void reloadLocales(Connection conn) throws SQLException {
        String sql = "";
        synchronized (conn) {
            Statement s = conn.createStatement();
            if (!DBUtils.hasTable(conn, DB_LOC2FORUM)) { // user attribute
                sql = "CREATE TABLE " + DB_LOC2FORUM + " ( " + " locale VARCHAR(255) NOT NULL, "
                    + " forum VARCHAR(255) NOT NULL" + " )";
                s.execute(sql);
                sql = "CREATE UNIQUE INDEX " + DB_LOC2FORUM + "_loc ON " + DB_LOC2FORUM + " (locale) ";
                s.execute(sql);
                sql = "CREATE INDEX " + DB_LOC2FORUM + "_f ON " + DB_LOC2FORUM + " (forum) ";
                s.execute(sql);
            } else {
                s.executeUpdate("delete from " + DB_LOC2FORUM);
            }
            s.close();

            PreparedStatement initbl = DBUtils.prepareStatement(conn, "initbl", "INSERT INTO " + DB_LOC2FORUM
                + " (locale,forum) VALUES (?,?)");
            int errs = 0;
            for (CLDRLocale l : SurveyMain.getLocalesSet()) {
                initbl.setString(1, l.toString());
                String forum = localeToForum(l);
                initbl.setString(2, forum);
                try {
                    initbl.executeUpdate();
                } catch (SQLException se) {
                    if (errs == 0) {
                        System.err.println("While updating " + DB_LOC2FORUM + " -  " + DBUtils.unchainSqlException(se)
                            + " - " + l + ":" + forum + ",  [This and further errors, ignored]");
                    }
                    errs++;
                }
            }
            initbl.close();
            conn.commit();
        }
    }

    /**
     * internal - called to setup db
     */
    private void setupDB(Connection conn) throws SQLException {
        String onOrBefore = CLDRConfig.getInstance().getProperty("CLDR_OLD_POSTS_BEFORE", "12/31/69");
        DateFormat sdf = DateFormat.getDateInstance(DateFormat.SHORT, ULocale.US);
        try {
            oldOnOrBefore = sdf.parse(onOrBefore);
        } catch (Throwable t) {
            System.err.println("Error in parsing CLDR_OLD_POSTS_BEFORE : " + onOrBefore + " - err " + t.toString());
            t.printStackTrace();
            oldOnOrBefore = null;
        }
        if (oldOnOrBefore == null) {
            oldOnOrBefore = new Date(0);
        }
        System.err.println("CLDR_OLD_POSTS_BEFORE: date: " + sdf.format(oldOnOrBefore) + " (format: mm/dd/yy)");
        String sql = null;
        String locindex = "loc";
        if (DBUtils.db_Mysql) {
            locindex = "loc(122)";
        }

        if (!DBUtils.hasTable(conn, DB_FORA)) { // user attribute
            Statement s = conn.createStatement();
            sql = "CREATE TABLE " + DB_FORA + " ( " + " id INT NOT NULL " + DBUtils.DB_SQL_IDENTITY
                + ", "
                + " loc VARCHAR(122) NOT NULL, "
                + // interest locale
                " first_time " + DBUtils.DB_SQL_TIMESTAMP0 + " NOT NULL " + DBUtils.DB_SQL_WITHDEFAULT + " "
                + DBUtils.DB_SQL_CURRENT_TIMESTAMP0 + ", " + " last_time TIMESTAMP NOT NULL " + DBUtils.DB_SQL_WITHDEFAULT
                + " CURRENT_TIMESTAMP" + " )";
            s.execute(sql);
            sql = "";
            s.close();
            conn.commit();
        }
        if (!DBUtils.hasTable(conn, DBUtils.Table.FORUM_POSTS.toString())) {
            Statement s = conn.createStatement();
            sql = "CREATE TABLE " + DBUtils.Table.FORUM_POSTS + " ( " + " id INT NOT NULL "
                + DBUtils.DB_SQL_IDENTITY + ", "
                + " forum INT NOT NULL, " // which forum (DB_FORA), i.e. de
                + " poster INT NOT NULL, " + " subj " + DBUtils.DB_SQL_UNICODE + ", " + " text " + DBUtils.DB_SQL_UNICODE
                + " NOT NULL, " + " parent INT " + DBUtils.DB_SQL_WITHDEFAULT
                + " -1, "
                + " loc VARCHAR(122), " // specific locale, i.e. de_CH
                + " xpath INT, " // base xpath
                + " last_time TIMESTAMP NOT NULL " + DBUtils.DB_SQL_WITHDEFAULT + " CURRENT_TIMESTAMP, "
                + " version VARCHAR(122), " // CLDR version
                + " root INT NOT NULL,"
                + " type INT NOT NULL,"
                + " is_open BOOLEAN NOT NULL,"
                + " value " + DBUtils.DB_SQL_UNICODE
                + " )";
            s.execute(sql);
            sql = "CREATE UNIQUE INDEX " + DBUtils.Table.FORUM_POSTS + "_id ON " + DBUtils.Table.FORUM_POSTS + " (id) ";
            s.execute(sql);
            sql = "CREATE INDEX " + DBUtils.Table.FORUM_POSTS + "_ut ON " + DBUtils.Table.FORUM_POSTS + " (poster, last_time) ";
            s.execute(sql);
            sql = "CREATE INDEX " + DBUtils.Table.FORUM_POSTS + "_utt ON " + DBUtils.Table.FORUM_POSTS + " (id, last_time) ";
            s.execute(sql);
            sql = "CREATE INDEX " + DBUtils.Table.FORUM_POSTS + "_chil ON " + DBUtils.Table.FORUM_POSTS + " (parent) ";
            s.execute(sql);
            sql = "CREATE INDEX " + DBUtils.Table.FORUM_POSTS + "_loc ON " + DBUtils.Table.FORUM_POSTS + " (" + locindex + ") ";
            s.execute(sql);
            sql = "CREATE INDEX " + DBUtils.Table.FORUM_POSTS + "_x ON " + DBUtils.Table.FORUM_POSTS + " (xpath) ";
            s.execute(sql);
            sql = "";
            s.close();
            conn.commit();
        }
        reloadLocales(conn);
    }

    private SurveyMain sm = null;

    private static PreparedStatement prepare_fGetByLoc(Connection conn) throws SQLException {
        return DBUtils.prepareStatement(conn, "fGetByLoc", "SELECT id FROM " + DB_FORA + " where loc=?");
    }

    private static PreparedStatement prepare_fAdd(Connection conn) throws SQLException {
        return DBUtils.prepareStatement(conn, "fAdd", "INSERT INTO " + DB_FORA + " (loc) values (?)");
    }

    /**
     * Prepare a statement for adding a new post to the forum table.
     *
     * @param conn the Connection
     * @return the PreparedStatement
     * @throws SQLException
     *
     * Called only by savePostToDb
     */
    private static PreparedStatement prepare_pAdd(Connection conn) throws SQLException {
        return DBUtils.prepareStatement(conn, "pAdd", "INSERT INTO "
            + DBUtils.Table.FORUM_POSTS.toString()
            + " (poster,subj,text,forum,parent,loc,xpath,version,root,type,is_open,value)"
            + " values (?,?,?,?,?,?,?,?,?,?,?,?)");
    }

    /**
     * Prepare a statement for closing a thread of posts in the forum table.
     *
     * @param conn the Connection
     * @return the PreparedStatement
     * @throws SQLException
     *
     * Called only by savePostToDb
     */
    private static PreparedStatement prepare_pCloseThread(Connection conn) throws SQLException {
        return DBUtils.prepareStatement(conn, "pAdd", "UPDATE "
            + DBUtils.Table.FORUM_POSTS.toString()
            + " SET is_open=false WHERE id=? OR root=?");
    }

    private static PreparedStatement prepare_pIntUsers(Connection conn) throws SQLException {
        return DBUtils.prepareStatement(conn, "pIntUsers", "SELECT uid from " + UserRegistry.CLDR_INTEREST + " where forum=?");
    }

    private static String localeToForum(ULocale locale) {
        return locale.getLanguage();
    }

    private static String localeToForum(CLDRLocale locale) {
        return localeToForum(locale.toULocale());
    }

    /**
     *
     * @param ctx
     * @param forum
     * @return
     *
     * Possibly called by tmpl/usermenu.jsp -- maybe dead code?
     */
    public static String forumLink(WebContext ctx, String forum) {
        String url = ctx.base() + "?" + F_FORUM + "=" + forum;
        return "<a " + ctx.atarget(WebContext.TARGET_DOCS) + " class='forumlink' href='" + url + "' >" // title='"+title+"'
            + "Forum" + "</a>";
    }

    /**
     * How many forum posts are there for the given locale and xpath?
     *
     * @param locale
     * @param xpathId
     * @return the number of posts
     *
     * Called by STFactory.PerLocaleData.voteForValue and SurveyAjax.processRequest (WHAT_FORUM_COUNT)
     */
    public int postCountFor(CLDRLocale locale, int xpathId) {
        Connection conn = null;
        PreparedStatement ps = null;
        String tableName = DBUtils.Table.FORUM_POSTS.toString();
        try {
            conn = DBUtils.getInstance().getDBConnection();

            ps = DBUtils.prepareForwardReadOnly(conn, "select count(*) from " + tableName + " where loc=? and xpath=?");
            ps.setString(1, locale.getBaseName());
            ps.setInt(2, xpathId);

            return DBUtils.sqlCount(null, conn, ps);
        } catch (SQLException e) {
            SurveyLog.logException(e, "postCountFor for " + tableName + " " + locale + ":" + xpathId);
            return 0;
        } finally {
            DBUtils.close(ps, conn);
        }
    }

    /**
     * Gather forum post information into a JSONArray, in preparation for
     * displaying it to the user.
     *
     * @param session
     * @param locale
     * @param base_xpath Base XPath of the item being viewed, if positive; or XPathTable.NO_XPATH
     * @param ident If nonzero - select only this item. If zero, select all items.
     * @return the JSONArray
     * @throws JSONException
     * @throws SurveyException
     */
    public JSONArray toJSON(CookieSession session, CLDRLocale locale, int base_xpath, int ident) throws JSONException, SurveyException {
        assertCanAccessForum(session, locale);

        JSONArray ret = new JSONArray();

        int forumNumber = getForumNumber(locale);

        try {
            Connection conn = null;
            try {
                conn = sm.dbUtils.getDBConnection();
                Object[][] o = null;
                final String forumPosts = DBUtils.Table.FORUM_POSTS.toString();
                if (ident == 0) {
                    if (base_xpath == 0) {
                        // all posts
                        o = DBUtils.sqlQueryArrayArrayObj(conn, "select " + getPallresultfora(forumPosts) + "  FROM " + forumPosts
                            + " WHERE (" + forumPosts + ".forum =? ) ORDER BY "
                            + forumPosts
                            + ".last_time DESC", forumNumber);
                    } else {
                        // all posts for xpath
                        o = DBUtils.sqlQueryArrayArrayObj(conn, "select " + getPallresultfora(forumPosts) + "  FROM " + forumPosts
                            + " WHERE (" + forumPosts + ".forum =? AND " + forumPosts + " .xpath =? and "
                            + forumPosts + ".loc=? ) ORDER BY "
                            + forumPosts
                            + ".last_time DESC", forumNumber, base_xpath, locale);
                    }
                } else {
                    // specific POST
                    if (base_xpath <= 0) {
                        o = DBUtils.sqlQueryArrayArrayObj(conn, "select " + getPallresultfora(forumPosts) + "  FROM " + forumPosts
                                + " WHERE (" + forumPosts + ".forum =? AND "
                                + forumPosts + " .id =?) ORDER BY "
                                + forumPosts
                                + ".last_time DESC",
                            forumNumber, /* base_xpath,*/ident);
                    } else {
                        // just a restriction - specific post, specific xpath
                        o = DBUtils.sqlQueryArrayArrayObj(conn, "select " + getPallresultfora(forumPosts) + "  FROM " + forumPosts
                            + " WHERE (" + forumPosts + ".forum =? AND " + forumPosts + " .xpath =? AND "
                            + forumPosts + " .id =?) ORDER BY " + forumPosts
                            + ".last_time DESC", forumNumber, base_xpath, ident);
                    }
                }
                if (o != null) {
                    for (int i = 0; i < o.length; i++) {
                        int poster = (Integer) o[i][0];
                        String subj2 = (String) o[i][1];
                        String text2 = (String) o[i][2];
                        Timestamp lastDate = (Timestamp) o[i][3];
                        int id = (Integer) o[i][4];
                        int parent = (Integer) o[i][5];
                        int xpath = (Integer) o[i][6];
                        String loc = (String) o[i][7];
                        String version = (String) o[i][8];
                        int root = (int) o[i][9];
                        int typeInt = (int) o[i][10];
                        boolean open = (boolean) o[i][11];
                        String value = (String) o[i][12];

                        PostType type = PostType.fromInt(typeInt, PostType.DISCUSS);

                        if (lastDate.after(oldOnOrBefore)) {
                            JSONObject post = new JSONObject();
                            post.put("poster", poster)
                                .put("subject", subj2)
                                .put("text", text2)
                                .put("postType", type.toName())
                                .put("date", lastDate)
                                .put("date_long", lastDate.getTime())
                                .put("id", id)
                                .put("parent", parent);
                            if (loc != null) {
                                post.put("locale", loc);
                            }
                            if (version != null) {
                                post.put("version", version);
                            }
                            if (value != null) {
                                post.put("value", value);
                            }
                            post.put("open", open);
                            post.put("root", root);
                            post.put("xpath_id", xpath);
                            if (xpath > 0) {
                                post.put("xpath", sm.xpt.getStringIDString(xpath));
                            }
                            UserRegistry.User posterUser = sm.reg.getInfo(poster);
                            if (posterUser != null) {
                                JSONObject posterInfoJson = SurveyAjax.JSONWriter.wrap(posterUser);
                                if (posterInfoJson != null) {
                                    post.put("posterInfo", posterInfoJson);
                                }
                            }
                            ret.put(post);
                        }
                    }
                }
                return ret;
            } finally {
                DBUtils.close(conn);
            }
        } catch (SQLException se) {
            // When query fails, set breakpoint here and look at se.detailMessage for clues
            String complaint = "SurveyForum:  Couldn't show posts in forum "
                + locale
                + " - " + DBUtils.unchainSqlException(se)
                + " - fGetByLoc";
            logger.severe(complaint);
            throw new RuntimeException(complaint);
        }
    }

    private void assertCanAccessForum(CookieSession session, CLDRLocale locale) throws SurveyException {
        if (session == null || session.user == null) {
            throw new SurveyException(ErrorCode.E_NOT_LOGGED_IN);
        }
        assertCanAccessForum(session.user, locale);
    }

    private void assertCanAccessForum(UserRegistry.User user, CLDRLocale locale) throws SurveyException {
        boolean canModify = (UserRegistry.userCanAccessForum(user, locale));
        if (!canModify) {
            throw new SurveyException(ErrorCode.E_NO_PERMISSION, "You do not have permission to access that locale");
        }
    }

    /**
     * Construct a portion of an sql query for getting all needed columns from the forum posts table.
     *
     * @param forumPosts the table name
     * @return the string to be used as part of a query
     */
    private static String getPallresultfora(String forumPosts) {
        return forumPosts + ".poster,"
            + forumPosts + ".subj,"
            + forumPosts + ".text,"
            + forumPosts + ".last_time,"
            + forumPosts + ".id,"
            + forumPosts + ".parent,"
            + forumPosts + ".xpath, "
            + forumPosts + ".loc,"
            + forumPosts + ".version,"
            + forumPosts + ".root,"
            + forumPosts + ".type,"
            + forumPosts + ".is_open,"
            + forumPosts + ".value";
    }

    /**
     * Respond when the user adds a new forum post.
     *
     * @param mySession the CookieSession
     * @param xpath of the form "stringid" or "#1234"
     * @param l the CLDRLocale
     * @param subj the subject of the post
     * @param text the text of the post
     * @param postTypeStr the PostType string such as "Close", or null
     * @param replyTo the id of the post to which this is a reply; {@link #NO_PARENT} if there is no parent
     * @return the post id
     *
     * @throws SurveyException
     */
    public int doPost(CookieSession mySession, PostInfo postInfo) throws SurveyException {
        CLDRLocale locale = postInfo.getLocale();
        assertCanAccessForum(mySession, locale);
        int replyTo = postInfo.getReplyTo();
        int base_xpath;
        if (replyTo < 0) {
            replyTo = NO_PARENT;
            base_xpath = sm.xpt.getXpathIdOrNoneFromStringID(postInfo.getPathStr());
        } else {
            base_xpath = DBUtils.sqlCount("select xpath from " + DBUtils.Table.FORUM_POSTS + " where id=?", replyTo); // default to -1
        }
        postInfo.setPath(base_xpath);
        final boolean couldFlag = couldFlagOnLosing(postInfo.getUser(), sm.xpt.getById(base_xpath), locale)
                && !sm.getSTFactory().getFlag(locale, base_xpath);
        postInfo.setCouldFlagOnLosing(couldFlag);
        if (couldFlag) {
            postInfo.setText(postInfo.getText() + FLAGGED_FOR_REVIEW_HTML);
        }
        return doPostInternal(postInfo);
    }

    /**
     * Update the forum as appropriate after a vote has been accepted
     *
     * @param locale
     * @param user
     * @param distinguishingXpath
     * @param xpathId
     * @param value
     * @param didClearFlag
     */
    public void doForumAfterVote(CLDRLocale locale, User user, String distinguishingXpath, int xpathId,
            String value, boolean didClearFlag) {
        if (didClearFlag) {
            try {
                int newPostId = postFlagRemoved(xpathId, locale, user);
                System.out.println("NOTE: flag was removed from "
                    + locale + " " + distinguishingXpath + " - post ID=" + newPostId
                    + " by " + user.toString());
            } catch (SurveyException e) {
                SurveyLog.logException(e, "Error trying to post that a flag was removed from "
                    + locale + " " + distinguishingXpath);
            }
        }
        final boolean ENABLE_AUTO_POSTING = true;
        if (ENABLE_AUTO_POSTING) {
            if (value != null) {
                autoPostAgree(locale, user, xpathId, value);
            }
            autoPostDecline(locale, user, xpathId, value);
            autoPostClose(locale, user, xpathId, value);
        }
    }

    /**
     * Make a special post for flag removal
     *
     * @param xpathId
     * @param locale
     * @param user
     * @return the post id
     *
     * @throws SurveyException
     */
    private int postFlagRemoved(int xpathId, CLDRLocale locale, User user) throws SurveyException {
        PostInfo postInfo = new PostInfo(locale, PostType.CLOSE.toName(), "(The flag was removed.)");
        postInfo.setSubj("Flag Removed");
        postInfo.setPath(xpathId);
        postInfo.setUser(user);
        return doPostInternal(postInfo);
    }

    /**
     * Auto-post Agree for each open Request post for this locale+path+value by other users
     *
     * @param locale
     * @param user
     * @param xpathId
     * @param value
     */
    private void autoPostAgree(CLDRLocale locale, User user, int xpathId, String value) {
        Connection conn = null;
        PreparedStatement pList = null;
        String tableName = DBUtils.Table.FORUM_POSTS.toString();
        Map<Integer, String> posts = new HashMap<>();
        try {
            conn = sm.dbUtils.getDBConnection();
            pList = DBUtils.prepareStatement(conn, "pList",
                "SELECT id,subj FROM " + tableName
                + " WHERE is_open=true AND type=? AND loc=? AND xpath=? AND value=? AND NOT poster=?");
            pList.setInt(1, PostType.REQUEST.toInt());
            pList.setString(2, locale.toString());
            pList.setInt(3, xpathId);
            DBUtils.setStringUTF8(pList, 4, value);
            pList.setInt(5, user.id);
            ResultSet rs = pList.executeQuery();
            while (rs.next()) {
                posts.put(rs.getInt(1), DBUtils.getStringUTF8(rs, 2));
            }
            posts.forEach((root, subject) -> autoPostReplyAgree(root, subject, locale, user, xpathId, value));
         } catch (SQLException se) {
            String complaint = "SurveyForum: autoPostAgree - " + DBUtils.unchainSqlException(se);
            SurveyLog.logException(se, complaint);
        } finally {
            DBUtils.close(pList, conn);
        }
    }

    private void autoPostReplyAgree(int root, String subject, CLDRLocale locale, User user,
            int xpathId, String value) {
        String text = "(Auto-generated:) I voted for “" + value + "”";
        PostInfo postInfo = new PostInfo(locale, PostType.AGREE.toName(), text);
        postInfo.setSubj(subject);
        postInfo.setPath(xpathId);
        postInfo.setUser(user);
        postInfo.setReplyTo(root /* replyTo */);
        postInfo.setRoot(root);
        postInfo.setValue(value);
        postInfo.setSendEmail(false);
        try {
            doPostInternal(postInfo);
        } catch (SurveyException e) {
            SurveyLog.logException(e, "SurveyForum: autoPostReplyAgree root " + root);
        }
    }

    /**
     * For each open AGREE post by this user, for this locale+path, where
     * the given value is NOT the same as the requested value, generate a new DECLINE post.
     *
     * @param locale
     * @param user
     * @param xpathId
     * @param value
     */
    private void autoPostDecline(CLDRLocale locale, User user, int xpathId, String value) {
        String dbValue = value == null ? "" : value;
        Connection conn = null;
        PreparedStatement pList = null;
        String tableName = DBUtils.Table.FORUM_POSTS.toString();
        Map<Integer, String> posts = new HashMap<>();
        try {
            conn = sm.dbUtils.getDBConnection();
            pList = DBUtils.prepareStatement(conn, "pList",
                "SELECT root,subj FROM " + tableName
                + " WHERE is_open=true AND type=? AND loc=? AND xpath=? AND poster=? AND NOT value=?");
            pList.setInt(1, PostType.AGREE.toInt());
            pList.setString(2, locale.toString());
            pList.setInt(3, xpathId);
            pList.setInt(4, user.id);
            DBUtils.setStringUTF8(pList, 5, dbValue);
            ResultSet rs = pList.executeQuery();
            while (rs.next()) {
                posts.put(rs.getInt(1), DBUtils.getStringUTF8(rs, 2));
            }
            posts.forEach((root, subject) -> autoPostReplyDecline(root, subject, locale, user, xpathId, value));
         } catch (SQLException se) {
            String complaint = "SurveyForum: autoPostDecline - " + DBUtils.unchainSqlException(se);
            SurveyLog.logException(se, complaint);
        } finally {
            DBUtils.close(pList, conn);
        }
    }

    private void autoPostReplyDecline(int root, String subject, CLDRLocale locale, User user,
            int xpathId, String value) {
        String text = (value == null)
                ? "(Auto-generated:) I changed my vote to Abstain, and will reconsider my vote."
                : "(Auto-generated:) I changed my vote to “" + value + "”, which now disagrees with the request.";
        PostInfo postInfo = new PostInfo(locale, PostType.DECLINE.toName(), text);
        postInfo.setSubj(subject);
        postInfo.setPath(xpathId);
        postInfo.setUser(user);
        postInfo.setReplyTo(root /* replyTo */);
        postInfo.setRoot(root);
        postInfo.setValue(value == null ? "Abstain" : value); /* NOT the same as the requested value */
        try {
            doPostInternal(postInfo);
        } catch (SurveyException e) {
            SurveyLog.logException(e, "SurveyForum: autoPostReplyDecline root " + root);
        }
    }

    /**
     * I request a vote for “X”, then I change to “Y” (or abstain)
     * Auto-generated: I changed my vote to “Y”, disagreeing with my request. This topic is being closed. ⇒ Close
     *
     * @param locale
     * @param user
     * @param xpathId
     * @param value
     */
    private void autoPostClose(CLDRLocale locale, User user, int xpathId, String value) {
        String dbValue = value == null ? "" : value;
        Connection conn = null;
        PreparedStatement pList = null;
        String tableName = DBUtils.Table.FORUM_POSTS.toString();
        Map<Integer, String> posts = new HashMap<>();
        try {
            conn = sm.dbUtils.getDBConnection();
            pList = DBUtils.prepareStatement(conn, "pList",
                "SELECT id,subj FROM " + tableName
                + " WHERE is_open=true AND type=? AND loc=? AND xpath=? AND poster=? AND NOT value=?");
            pList.setInt(1, PostType.REQUEST.toInt());
            pList.setString(2, locale.toString());
            pList.setInt(3, xpathId);
            pList.setInt(4, user.id);
            DBUtils.setStringUTF8(pList, 5, dbValue);
            ResultSet rs = pList.executeQuery();
            while (rs.next()) {
                posts.put(rs.getInt(1), DBUtils.getStringUTF8(rs, 2));
            }
            posts.forEach((root, subject) -> autoPostReplyClose(root, subject, locale, user, xpathId, value));
         } catch (SQLException se) {
            String complaint = "SurveyForum: autoPostClose - " + DBUtils.unchainSqlException(se);
            SurveyLog.logException(se, complaint);
        } finally {
            DBUtils.close(pList, conn);
        }
    }

    private void autoPostReplyClose(int root, String subject, CLDRLocale locale, User user,
            int xpathId, String value) {
        String abstainOrQuotedValue = value == null ? "Abstain" : "“" + value + "”";
        String text = "(Auto-generated:) I changed my vote to " + abstainOrQuotedValue
                + ", disagreeing with my request. This topic is being closed.";
        PostInfo postInfo = new PostInfo(locale, PostType.CLOSE.toName(), text);
        postInfo.setSubj(subject);
        postInfo.setPath(xpathId);
        postInfo.setUser(user);
        postInfo.setReplyTo(root /* replyTo */);
        postInfo.setRoot(root);
        postInfo.setValue(value == null ? "Abstain" : value); /* NOT the same as the requested value */
        postInfo.setSendEmail(false);
        try {
            doPostInternal(postInfo);
        } catch (SurveyException e) {
            SurveyLog.logException(e, "SurveyForum: autoPostReplyClose root " + root);
        }
    }

    /**
     * Respond to the user making a new forum post. Save the post in the database,
     * and send an email if appropriate.
     *
     * @param postInfo the post info
     *
     * @return the new post id, or <= 0 for failure
     * @throws SurveyException
     */
    private Integer doPostInternal(PostInfo postInfo) throws SurveyException {
        if (!postInfo.isValid()) {
            SurveyLog.errln("Invalid postInfo in SurveyForum.doPostInternal");
            return 0;
        }
        PostType postType = postInfo.getType();
        User user = postInfo.getUser();
        if (!userCanUsePostType(user, postType, postInfo.getReplyTo())) {
            SurveyLog.errln("Post not allowed in SurveyForum.doPostInternal");
            return 0;
        }
        int postId = savePostToDb(postInfo);

        if (postInfo.getSendEmail()) {
            emailNotify(user, postInfo.getLocale(), postInfo.getPath(), postInfo.getSubj(), postInfo.getText(), postId);
        }
        return postId;
    }

    /**
     * Save a new post to the FORUM_POSTS table; if it's a CLOSE post,
     * also set is_open=false for all posts in this thread
     *
     * @param PostInfo the post info
     * @return the new post id, or <= 0 for failure
     * @throws SurveyException
     */
    private int savePostToDb(PostInfo postInfo) throws SurveyException {
        int postId = 0;
        final CLDRLocale locale = postInfo.getLocale();
        final String localeStr = locale.toString();
        final int forumNumber = getForumNumber(locale);
        final User user = postInfo.getUser();
        final PostType type = postInfo.getType();
        final boolean open = (type == PostType.CLOSE) ? false : postInfo.getOpen();
        final int root = postInfo.getRoot();
        final String text = postInfo.getText().replaceAll("\r", "").replaceAll("\n", "<p>");
        try {
            Connection conn = null;
            PreparedStatement pAdd = null, pCloseThread = null;
            try {
                conn = sm.dbUtils.getDBConnection();
                if (type == PostType.CLOSE) {
                    pCloseThread = prepare_pCloseThread(conn);
                    pCloseThread.setInt(1, root);
                    pCloseThread.setInt(2, root);
                    pCloseThread.executeUpdate();
                }
                pAdd = prepare_pAdd(conn);
                pAdd.setInt(1, user.id);
                DBUtils.setStringUTF8(pAdd, 2, postInfo.getSubj());
                DBUtils.setStringUTF8(pAdd, 3, text);
                pAdd.setInt(4, forumNumber);
                pAdd.setInt(5, postInfo.getReplyTo()); // record parent
                pAdd.setString(6, localeStr); // real locale of item, not forum #
                pAdd.setInt(7, postInfo.getPath());
                pAdd.setString(8, SurveyMain.getNewVersion()); // version
                pAdd.setInt(9, root);
                pAdd.setInt(10, type.toInt());
                pAdd.setBoolean(11, open);
                DBUtils.setStringUTF8(pAdd, 12, postInfo.getValue());

                int n = pAdd.executeUpdate();
                if (postInfo.couldFlagOnLosing()) {
                    sm.getSTFactory().setFlag(conn, locale, postInfo.getPath(), user);
                    System.out.println("NOTE: flag was set on " + localeStr + " by " + user.toString());
                }

                conn.commit();
                postId = DBUtils.getLastId(pAdd);

                if (n != 1) {
                    throw new RuntimeException("Couldn't post to " + localeStr + " - update failed.");
                }
            } finally {
                DBUtils.close(pAdd, pCloseThread, conn);
            }
        } catch (SQLException se) {
            String complaint = "SurveyForum:  Couldn't add post to " + localeStr + " - " + DBUtils.unchainSqlException(se)
                + " - pAdd";
            SurveyLog.logException(se, complaint);
            throw new SurveyException(ErrorCode.E_INTERNAL, complaint);
        }
        return postId;
    }

    public class PostInfo {
        private final CLDRLocale locale;
        private PostType type;
        private String text;
        private int xpathId = XPathTable.NO_XPATH;
        private String pathString = null;
        private int replyTo = NO_PARENT;
        private int root = NO_PARENT;
        private String subj = null;
        private String value = null;
        private boolean open = true;
        private boolean couldFlag = false;
        private UserRegistry.User user = null;
        private boolean sendEmail = true;

        public PostInfo(CLDRLocale locale, String postTypeStr, String text) {
            this.locale = locale;
            this.type = PostType.fromName(postTypeStr, null);
            this.text = text;
        }

        public boolean isValid() {
            if (locale == null
                || type == null
                || text == null
                || subj == null
                || user == null) {
                return false;
            }
            if ((replyTo == NO_PARENT) != (root == NO_PARENT)) {
                return false;
            }
            if (value == null && (type == PostType.REQUEST || type == PostType.AGREE || type == PostType.DECLINE)) {
                return false;
            }
            return true;
        }

        /*
         * Getters
         */

        public String getPathStr() {
            return pathString;
        }

        public String getValue() {
            return value;
        }

        public boolean getOpen() {
            return open;
        }

        public PostType getType() {
            return type;
        }

        public int getRoot() {
            return root;
        }

        public CLDRLocale getLocale() {
            return locale;
        }

        public boolean couldFlagOnLosing() {
            return couldFlag;
        }

        public int getPath() {
            return xpathId;
        }

        public String getText() {
            return text;
        }

        public String getSubj() {
            return subj;
        }

        public int getReplyTo() {
            return replyTo;
        }

        public User getUser() {
            return user;
        }

        public boolean getSendEmail() {
            return sendEmail;
        }

        /*
         * Setters
         */

        public void setRoot(int root) {
            this.root = root;
        }

        public void setSubj(String subj) {
            this.subj = subj;
        }

        public void setPathString(String xpathStr) {
            this.pathString = xpathStr;
        }

        public void setReplyTo(int replyTo) {
            this.replyTo = replyTo;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public void setPath(int base_xpath) {
            this.xpathId = base_xpath;
        }

        public void setCouldFlagOnLosing(boolean couldFlag) {
            this.couldFlag = couldFlag;
        }

        public void setText(String text) {
            this.text = text;
        }

        public void setOpen(boolean open) {
            this.open = open;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public void setSendEmail(boolean sendEmail) {
            this.sendEmail = sendEmail;
        }
    }

    /**
     * Status values associated with forum posts and threads
     */
    enum PostType {
        CLOSE(0, "Close"),
        DISCUSS(1, "Discuss"),
        REQUEST(2, "Request"),
        AGREE(3, "Agree"),
        DECLINE(4, "Decline");

        PostType(int id, String name) {
            this.id = id;
            this.name = name;
        }

        private final int id;
        private final String name;

        /**
         * Get the integer id for this PostType
         *
         * @return the id
         */
        public int toInt() {
            return id;
        }

        /**
         * Get the name for this PostType
         *
         * @return the name
         */
        public String toName() {
            return name;
        }

        /**
         * Get a PostType value from its name, or if the name is not associated with
         * a PostType value, use the given default PostType
         *
         * @param i
         * @param defaultStatus
         * @return the PostType
         */
        public static PostType fromInt(int i, PostType defaultStatus) {
            for (PostType s : PostType.values()) {
                if (s.id == i) {
                    return s;
                }
            }
            return defaultStatus;
        }

        /**
         * Get a PostType value from its name, or if the name is not associated with
         * a PostType value, use the given default PostType
         *
         * @param name
         * @param defaultStatus
         * @return the PostType
         */
        public static PostType fromName(String name, PostType defaultStatus) {
            if (name != null) {
                for (PostType s : PostType.values()) {
                    if (s.name.equals(name)) {
                        return s;
                    }
                }
            }
            return defaultStatus;
        }
    }
}
