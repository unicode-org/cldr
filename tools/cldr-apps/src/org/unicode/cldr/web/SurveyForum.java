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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * Map post id to PostType
     */
    private ConcurrentHashMap<Integer, PostType> allPostType = new ConcurrentHashMap<Integer, PostType>();

    private static String DB_FORA = "sf_fora"; // forum name -> id

    private static String DB_LOC2FORUM = "sf_loc2forum"; // locale -> forum.. for selects.

    private static final String F_FORUM = "forum";

    public static final String F_XPATH = "xpath";

    /**
     * prepare text for posting
     */
    static private String preparePostText(String intext) {
        return intext.replaceAll("\r", "").replaceAll("\n", "<p>");
    }

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

    private Hashtable<String, Integer> nameToNum = new Hashtable<String, Integer>();

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
        Integer i = (Integer) nameToNum.get(forum);
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
        Set<Integer> cc_emails = new HashSet<Integer>();
        Set<Integer> bcc_emails = new HashSet<Integer>();

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
     * Respond to the user making a new forum post. Save the post in the database.
     * 
     * @param base_xpath
     * @param replyTo
     * @param locale
     * @param subj
     * @param text
     * @param postTypeStr the PostType name like "Close", or null
     * @param couldFlagOnLosing
     * @param user
     * @return the new post id, or <= 0 for failure
     *
     * @throws SurveyException
     *
     * Called by STFactory.PerLocaleData.voteForValue (for "Flag Removed" only) as well as locally by doPost
     */
    private Integer doPostInternal(int base_xpath, int replyTo, final CLDRLocale locale, String subj, String text,
            String postTypeStr, final boolean couldFlagOnLosing, final UserRegistry.User user) throws SurveyException {

        PostType postType = PostType.fromName(postTypeStr, null);
        if (postType == null || !userCanUsePostType(user, postType, replyTo)) {
            return 0;
        }
        int postId = savePostToDb(user, subj, text, replyTo, locale, base_xpath, couldFlagOnLosing);

        setPostType(postId, postType);

        emailNotify(user, locale, base_xpath, subj, text, postId);

        return postId;
    }

    /**
     * Save a new post to the FORUM_POSTS table
     *
     * @param user
     * @param subj
     * @param text
     * @param replyTo
     * @param locale
     * @param base_xpath
     * @param couldFlagOnLosing
     * @return the new post id, or <= 0 for failure
     * @throws SurveyException
     */
    private int savePostToDb(User user, String subj, String text, int replyTo, final CLDRLocale locale,
            int base_xpath, boolean couldFlagOnLosing) throws SurveyException {

        int postId = 0;

        final int forumNumber = getForumNumber(locale);

        try {
            Connection conn = null;
            PreparedStatement pAdd = null;
            try {
                conn = sm.dbUtils.getDBConnection();
                pAdd = prepare_pAdd(conn);
                pAdd.setInt(1, user.id);
                DBUtils.setStringUTF8(pAdd, 2, subj);
                DBUtils.setStringUTF8(pAdd, 3, preparePostText(text));
                pAdd.setInt(4, forumNumber);
                pAdd.setInt(5, replyTo); // record parent
                pAdd.setString(6, locale.toString()); // real locale of item, not furm #
                pAdd.setInt(7, base_xpath);
                pAdd.setString(8, SurveyMain.getNewVersion()); // version

                int n = pAdd.executeUpdate();
                if (couldFlagOnLosing) {
                    sm.getSTFactory().setFlag(conn, locale, base_xpath, user);
                    System.out.println("NOTE: flag was set on " + locale + " " + base_xpath + " by " + user.toString());
                }
                conn.commit();
                postId = DBUtils.getLastId(pAdd);

                if (n != 1) {
                    throw new RuntimeException("Couldn't post to " + locale + " - update failed.");
                }
            } finally {
                DBUtils.close(pAdd, conn);
            }
        } catch (SQLException se) {
            String complaint = "SurveyForum:  Couldn't add post to " + locale + " - " + DBUtils.unchainSqlException(se)
                + " - pAdd";
            SurveyLog.logException(se, complaint);
            throw new SurveyException(ErrorCode.E_INTERNAL, complaint);
        }
        return postId;
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
     * Add a row to the FORUM_TYPES db table for the given post and PostType
     * and add it to allPostType, unless this PostType doesn't belong in the table
     *
     * @param postId the post id
     * @param postType the PostType, or null
     * @throws SurveyException
     */
    private void setPostType(int postId, PostType postType) throws SurveyException {

        if (postType == null || !postType.belongsInTable()) {
            return;
        }
        int postTypeId = postType.toInt();
        allPostType.put(postId, postType);
        try {
            Connection conn = null;
            PreparedStatement pAdd = null;
            try {
                conn = sm.dbUtils.getDBConnection();
                pAdd = prepare_pAddStatus(conn);
                pAdd.setInt(1, postId);
                pAdd.setInt(2, postTypeId);
                int n = pAdd.executeUpdate();
                conn.commit();
                if (n != 1) {
                    throw new RuntimeException("Couldn't add type for post.");
                }
            } finally {
                DBUtils.close(pAdd, conn);
            }
        } catch (SQLException se) {
            String complaint = "SurveyForum: Couldn't add type for post - " + DBUtils.unchainSqlException(se);
            SurveyLog.logException(se, complaint);
            throw new SurveyException(ErrorCode.E_INTERNAL, complaint);
        }
    }

    /**
     * Read the PostType table and fill in the allPostType hash
     */
    private void getallPostTypeFromTable() {
        Connection conn = null;
        PreparedStatement pList = null;
        try {
            conn = sm.dbUtils.getDBConnection();
            pList = DBUtils.prepareStatement(conn, "pList", "SELECT id,type FROM " + DBUtils.Table.FORUM_TYPES.toString());
            ResultSet rs = pList.executeQuery();
            while (rs.next()) {
                int id = rs.getInt(1);
                int si = rs.getInt(2);
                PostType postType = PostType.fromInt(si, PostType.CLOSE);
                if (postType != PostType.CLOSE) {
                    allPostType.put(id, postType);
                }
            }
        } catch (SQLException se) {
            String complaint = "SurveyForum: Could not get type from table " + DBUtils.unchainSqlException(se);
            logger.severe(complaint);
            throw new RuntimeException(complaint);
        } finally {
            DBUtils.close(pList, conn);
        }
    }

    /**
     * Get the PostType of the post with the given id
     *
     * @param postId
     * @return the PostType
     */
    private PostType getPostType(int postId) {
        PostType postType = allPostType.get(postId);
        if (postType == null) {
            return PostType.CLOSE;
        }
        return postType;
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
            /* Create a new forum table.
             * This code might only be executed under exceptional circumstances, such as in testing.
             * New forum tables are no longer created for each CLDR version.
             * FORUM_POSTS.isVersioned == FORUM_POSTS.hasBeta == false; now there is only one
             * permanent name for the table, per https://unicode.org/cldr/trac/ticket/10935
             * In addition to the name change, one column (first_time) has been removed and
             * one column (version) has been added. 
             * Alternative table-creation code is in a script
             *   https://unicode.org/cldr/trac/raw-attachment/ticket/10935/cldr-make-forum.sql
             * made for the one-time operation to merge the old cldr_forum_posts_28, cldr_forum_posts_30,
             * cldr_forum_posts_32, and cldr_forum_posts_33, creating new cldr_forum_posts.
             * On comparison of mysqldump output, the only difference in effect between
             * cldr-make-forum.sql and the following code, except for backticks, is
             * "CHARSET=utf8mb4 COLLATE=utf8mb4_bin" (in cldr-make-forum.sql) versus "CHARSET=latin1".
             */
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
                + " version VARCHAR(122)" // CLDR version
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
        if (!DBUtils.hasTable(conn, DBUtils.Table.FORUM_TYPES.toString())) {
            /*
             * Create a new FORUM_TYPES table.
             */
            Statement s = conn.createStatement();
            sql = "CREATE TABLE " + DBUtils.Table.FORUM_TYPES + " (id INT NOT NULL, type INT NOT NULL)";
            s.execute(sql);
            sql = "CREATE UNIQUE INDEX " + DBUtils.Table.FORUM_TYPES + "_id ON " + DBUtils.Table.FORUM_TYPES + " (id)";
            s.execute(sql);
            s.close();
            conn.commit();
        }
        reloadLocales(conn);
        getallPostTypeFromTable();
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
     * Called only by doPostInternal
     */
    private static PreparedStatement prepare_pAdd(Connection conn) throws SQLException {
        return DBUtils.prepareStatement(conn, "pAdd", "INSERT INTO " + DBUtils.Table.FORUM_POSTS.toString()
            + " (poster,subj,text,forum,parent,loc,xpath,version) values (?,?,?,?,?,?,?,?)");
    }

    /**
     * Prepare a statement for adding a new post to the FORUM_TYPES table.
     *
     * @param conn the Connection
     * @return the PreparedStatement
     * @throws SQLException
     *
     * Called only by addStatusToTable
     */
    private static PreparedStatement prepare_pAddStatus(Connection conn) throws SQLException {
        return DBUtils.prepareStatement(conn, "pAdd", "INSERT INTO " + DBUtils.Table.FORUM_TYPES.toString()
            + " (id,type) values (?,?)");
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

                        if (lastDate.after(oldOnOrBefore)) {
                            JSONObject post = new JSONObject();
                            post.put("poster", poster)
                                .put("subject", subj2)
                                .put("text", text2)
                                .put("postType", getPostType(id).toName())
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
            + forumPosts + ".version";
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
    public int doPost(CookieSession mySession, String xpath, CLDRLocale l, String subj, String text, String postTypeStr, int replyTo) throws SurveyException {
        assertCanAccessForum(mySession, l);
        int base_xpath;
        if (replyTo < 0) {
            replyTo = NO_PARENT;
            base_xpath = sm.xpt.getXpathIdOrNoneFromStringID(xpath);
        } else {
            base_xpath = DBUtils.sqlCount("select xpath from " + DBUtils.Table.FORUM_POSTS + " where id=?", replyTo); // default to -1
        }
        final boolean couldFlagOnLosing = couldFlagOnLosing(mySession.user, sm.xpt.getById(base_xpath), l) && !sm.getSTFactory().getFlag(l, base_xpath);

        if (couldFlagOnLosing) {
            text = text + FLAGGED_FOR_REVIEW_HTML;
        }
        return doPostInternal(base_xpath, replyTo, l, subj, text, postTypeStr, couldFlagOnLosing, mySession.user);
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
    public int postFlagRemoved(int xpathId, CLDRLocale locale, User user) throws SurveyException {
        return doPostInternal(xpathId, -1, locale, "Flag Removed", "(The flag was removed.)", PostType.CLOSE.toName(), false, user);
    }

    /**
     * Status values associated with forum posts and threads
     */
    private enum PostType {
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

        /**
         * Does the given PostType belong in the FORUM_TYPES db table?
         *
         * Keep the table smaller by not storing rows in it for CLOSE.
         *
         * @return true or false
         */
        public boolean belongsInTable() {
            return this != CLOSE;
        }
    }
}
