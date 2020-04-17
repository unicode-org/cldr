//
//  SurveyForum.java
//
//  Created by Steven R. Loomis on 27/10/2006.
//  Copyright 2006-2013 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRURLS;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.web.DBUtils.Table;
import org.unicode.cldr.web.SurveyException.ErrorCode;
import org.unicode.cldr.web.UserRegistry.LogoutException;
import org.unicode.cldr.web.UserRegistry.User;

import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.UCharacterIterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * This class implements a discussion forum per language (ISO code)
 */
public class SurveyForum {
    private static final String FLAGGED_FOR_REVIEW_HTML = " <p>[This item was flagged for CLDR TC review.]";

    private static UnicodeSet VALID_FOR_XML = new UnicodeSet( // from http://www.w3.org/TR/xml/#charsets
        0x09, 0x0A,
        0x0D, 0x0D,
        0x20, 0xD7FF,
        0xe000, 0xfffd,
        0x10000, 0x10ffff).freeze();

    private static java.util.logging.Logger logger;

    public static String DB_FORA = "sf_fora"; // forum name -> id
    public static String DB_READERS = "sf_readers"; //

    public static String DB_LOC2FORUM = "sf_loc2forum"; // locale -> forum.. for
    // selects.

    /* --------- FORUM ------------- */
    static final String F_FORUM = "forum";
    public static final String F_XPATH = "xpath";
    static final String F_PATH = "path";
    static final String F_DO = "d";
    static final String F_LIST = "list";
    static final String F_VIEW = "view";
    static final String F_ADD = "add";
    static final String F_REPLY = "reply";
    static final String F_POST = "post";

    static final String POST_SPIEL = "Post a comment to other vetters. (Don't use this to report SurveyTool issues or propose data changes: use the bug forms.)";

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

    private static final int GOOD_FORUM = 0; // 0 or greater
    private static final int BAD_FORUM = -1;
    private static final int NO_FORUM = -2;

    /**
     * A post with this value for "parent" is the first post in its
     * thread; that is, it has no real parent post.
     */
    public static final int NO_PARENT = -1;

    /**
     * May return
     * @param forum
     * @return forum number, or BAD_FORUM or NO_FORUM
     */
    private synchronized int getForumNumber(String forum) {
        if (forum.length() == 0) {
            return NO_FORUM; // all forums
        }
        // make sure it is a valid src!
        if ((forum == null) || (forum.indexOf('_') >= 0) || !sm.isValidLocale(CLDRLocale.getInstance(forum))) {
            return BAD_FORUM;
        }

        // now with that out of the way..
        Integer i = (Integer) nameToNum.get(forum);
        if (i == null) {
            return createForum(forum);
        } else {
            return i.intValue();
        }
    }

    private int getForumNumber(CLDRLocale locale) {
        return getForumNumber(localeToForum(locale));
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

    public int gatherInterestedUsers(String forum, Set<Integer> cc_emails, Set<Integer> bcc_emails) {
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
     * 
     * @param replyTo
     * @return
     * 
     * Called by doXpathPost and by doPost
     */
    private int getXpathForPost(int replyTo) {
        int base_xpath;
        base_xpath = DBUtils.sqlCount("select xpath from " + DBUtils.Table.FORUM_POSTS + " where id=?", replyTo); // default to -1
        return base_xpath;
    }

    /**
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
     * @param statusStr the status string like "Open", or null
     * @param couldFlagOnLosing
     * @param user
     * @param postId
     * @return
     * @throws SurveyException
     *
     * Called by STFactory.PerLocaleData.voteForValue (for "Flag Removed" only) as well as locally by doPost
     */
    public Integer doPostInternal(int base_xpath, int replyTo, final CLDRLocale locale, String subj, String text,
            String status, final boolean couldFlagOnLosing, final UserRegistry.User user) throws SurveyException {

        final int forumNumber = getForumNumber(locale);
        int postId;

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

        addStatusToTable(postId, status);

        emailNotify(user, locale, base_xpath, subj, text, postId);
        return postId;
    }

    /**
     * Add a row to the FORUM_STATUS db table for the given post and status,
     * unless this status doesn't belong in the table
     *
     * @param postId the post id
     * @param status the string representing a ForumStatus, or null
     * @throws SurveyException
     */
    private void addStatusToTable(int postId, String status) throws SurveyException {

        ForumStatus forumStatus = ForumStatus.fromName(status, ForumStatus.OPEN);
        if (!forumStatus.belongsInTable()) {
            return;
        }
        try {
            Connection conn = null;
            PreparedStatement pAdd = null;
            try {
                conn = sm.dbUtils.getDBConnection();
                pAdd = prepare_pAddStatus(conn);
                pAdd.setInt(1, postId);
                pAdd.setInt(2, forumStatus.toInt());
                int n = pAdd.executeUpdate();
                conn.commit();
                if (n != 1) {
                    throw new RuntimeException("Couldn't add status for post.");
                }
            } finally {
                DBUtils.close(pAdd, conn);
            }
        } catch (SQLException se) {
            String complaint = "SurveyForum: Couldn't add status for post - " + DBUtils.unchainSqlException(se);
            SurveyLog.logException(se, complaint);
            throw new SurveyException(ErrorCode.E_INTERNAL, complaint);
        }
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
     *
     * @param me
     * @param uid
     * @return
     *
     * Called only by doFeed, for RSS???
     */
    private String getNameTextFromUid(UserRegistry.User me, int uid) {
        UserRegistry.User theU = null;
        theU = sm.reg.getInfo(uid);
        String aLink = null;
        if ((theU != null) && (me != null) && ((uid == me.id) || // if it's us
        // or..
            (UserRegistry.userIsTC(me) || // or TC..
                (UserRegistry.userIsVetter(me))))) { // vetter&same org
            if ((me == null) || (me.org == null)) {
                throw new InternalError("null: c.s.u.o");
            }
            if ((theU != null) && (theU.org == null)) {
                throw new InternalError("null: theU.o");
            }
            aLink = theU.name + " (" + theU.org + ")";
        } else if (theU != null) {
            aLink = "(" + theU.org + " vetter #" + uid + ")";
        } else {
            aLink = "(#" + uid + ")";
        }

        return aLink;
    }

    /**
     * Called by SM to create the reg
     *
     * @param xlogger
     *            the logger to use
     * @param ourConn
     *            the conn to use
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
        if (!DBUtils.hasTable(conn, DBUtils.Table.FORUM_STATUS.toString())) {
            /*
             * Create a new forum-status table.
             */
            Statement s = conn.createStatement();
            sql = "CREATE TABLE " + DBUtils.Table.FORUM_STATUS + " (id INT NOT NULL, status INT NOT NULL)";
            s.execute(sql);
            sql = "CREATE UNIQUE INDEX " + DBUtils.Table.FORUM_STATUS + "_id ON " + DBUtils.Table.FORUM_STATUS + " (id)";
            s.execute(sql);
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

    private static PreparedStatement prepare_pList(Connection conn) throws SQLException {
        return DBUtils.prepareStatement(conn, "pList", "SELECT poster,subj,text,id,last_time,loc,xpath FROM " + DBUtils.Table.FORUM_POSTS.toString()
            + " WHERE (forum = ?) ORDER BY last_time DESC ");
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
     * Prepare a statement for adding a new post to the forum status table.
     *
     * @param conn the Connection
     * @return the PreparedStatement
     * @throws SQLException
     *
     * Called only by addStatusToTable
     */
    private static PreparedStatement prepare_pAddStatus(Connection conn) throws SQLException {
        return DBUtils.prepareStatement(conn, "pAdd", "INSERT INTO " + DBUtils.Table.FORUM_STATUS.toString()
            + " (id,status) values (?,?)");
    }

    private static PreparedStatement prepare_pAll(Connection conn) throws SQLException {
        return DBUtils.prepareStatement(conn, "pAll", "SELECT " + getPallresult() + " FROM " + DBUtils.Table.FORUM_POSTS + "," + DB_FORA + " WHERE ("
            + DBUtils.Table.FORUM_POSTS + ".forum = " + DB_FORA + ".id) ORDER BY " + DBUtils.Table.FORUM_POSTS + ".last_time DESC");
    }

    private static PreparedStatement prepare_pForMe(Connection conn) throws SQLException {
        return DBUtils.prepareStatement(conn, "pForMe", "SELECT " + getPallresult() + " FROM " + DBUtils.Table.FORUM_POSTS.toString()
            + ","
            + DB_FORA
            + " " // same as pAll
            + " where (" + DBUtils.Table.FORUM_POSTS + ".forum=" + DB_FORA + ".id) AND exists ( select " + UserRegistry.CLDR_INTEREST
            + ".forum from " + UserRegistry.CLDR_INTEREST + "," + DB_FORA + " where " + UserRegistry.CLDR_INTEREST
            + ".uid=? AND " + UserRegistry.CLDR_INTEREST + ".forum=" + DB_FORA + ".loc AND " + DB_FORA + ".id=" + DBUtils.Table.FORUM_POSTS.toString()
            + ".forum) ORDER BY " + DBUtils.Table.FORUM_POSTS + ".last_time DESC");
    }

    private static PreparedStatement prepare_pIntUsers(Connection conn) throws SQLException {
        return DBUtils.prepareStatement(conn, "pIntUsers", "SELECT uid from " + UserRegistry.CLDR_INTEREST + " where forum=?");
    }

    private static String localeToForum(String locale) {
        return localeToForum(new ULocale(locale));
    }

    private static String localeToForum(ULocale locale) {
        return locale.getLanguage();
    }

    private static String localeToForum(CLDRLocale locale) {
        return localeToForum(locale.toULocale());
    }

    private static String forumUrl(WebContext ctx, String forum) {
        return (ctx.base() + "?" + F_FORUM + "=" + forum);
    }

    /**
     *
     * @param ctx
     * @param forum
     * @return
     *
     * Possibly called by tmpl/usermenu.jsp
     */
    public static String forumLink(WebContext ctx, String forum) {
        return "<a " + ctx.atarget(WebContext.TARGET_DOCS) + " class='forumlink' href='" + forumUrl(ctx, forum) + "' >" // title='"+title+"'
            + "Forum" + "</a>";
    }

    // XML/RSS

    private static void sendErr(HttpServletRequest request, HttpServletResponse response, String err) throws IOException {
        response.setContentType("text/html; charset=utf-8");
        WebContext xctx = new WebContext(request, response);
        xctx.println("Error: " + err);
        xctx.close();
        return;
    }

    /**
     * Respond to an RSS "feed" request?
     *
     * TODO: is this used, and if so, how?
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws ServletException
     *
     * Called by OutputFileManager.doRawXml
     */
    public boolean doFeed(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        response.setContentType("text/xml; charset=utf-8");

        String feedType = request.getParameter("feed");
        if (feedType == null) {
            feedType = "rss_0.94";
        }

        String email = request.getParameter("email");
        String pw = request.getParameter("pw");

        if (email == null || pw == null) {
            sendErr(request, response, "URL error.");
            return true;
        }

        UserRegistry.User user = null;
        try {
            user = sm.reg.get(pw, email, "RSS@" + WebContext.userIP(request));
        } catch (LogoutException e) {
            e.printStackTrace();
        }

        if (user == null) {
            sendErr(request, response, "authentication err");
            return true;
        }

        String base = "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath()
            + request.getServletPath();
        String loc = request.getParameter("_");

        if ((loc != null) && (!UserRegistry.userCanModifyLocale(user, CLDRLocale.getInstance(loc)))) {
            sendErr(request, response, "permission denied for locale " + loc);
            return true;
        }

        try {
            SyndFeed feed = new SyndFeedImpl();
            feed.setFeedType(feedType);
            List<SyndEntry> entries = new ArrayList<SyndEntry>();
            feed.setLink(base);

            if (loc != null) {
                feed.setTitle("CLDR Feed for " + loc);
                feed.setDescription("test feed");

                SyndEntry entry;
                SyndContent description;

                try {
                    Connection conn = null;
                    PreparedStatement pList = null;
                    try {
                        conn = sm.dbUtils.getDBConnection();
                        pList = prepare_pList(conn);

                        int forumNumber = getForumNumberFromDB(loc);
                        if (forumNumber >= GOOD_FORUM) {
                            pList.setInt(1, forumNumber);
                            ResultSet rs = pList.executeQuery();
                            while (rs.next()) {
                                int poster = rs.getInt(1);
                                String subj = rs.getString(2);
                                String text = rs.getString(3);
                                int id = rs.getInt(4);
                                java.sql.Timestamp lastDate = rs.getTimestamp(5);

                                String nameLink = getNameTextFromUid(user, poster);

                                entry = new SyndEntryImpl();
                                entry.setTitle(subj);
                                entry.setAuthor(nameLink);
                                entry.setLink(base + "?forum=" + loc + "&amp;" + F_DO + "=" + F_VIEW + "&amp;id=" + id
                                    + "&amp;email=" + email + "&amp;pw=" + pw);
                                entry.setPublishedDate(lastDate); // dateParser.parse("2004-06-08"));
                                description = new SyndContentImpl();
                                description.setType("text/html");
                                description.setValue("From: " + nameLink + "<br><hr>" + shortenText(text));
                                entry.setDescription(description);
                                entries.add(entry);
                            }
                        }
                    } finally {
                        DBUtils.close(pList, conn);
                    }
                } catch (SQLException se) {
                    String complaint = "SurveyForum:  Couldn't use forum " + loc + " - " + DBUtils.unchainSqlException(se)
                        + " - fGetByLoc";
                    logger.severe(complaint);
                    throw new RuntimeException(complaint);
                }

            } else { /* loc is null */
                feed.setTitle("CLDR Feed for " + user.email);
                String locs[] = user.getInterestList();
                if (locs == null) {
                    feed.setDescription("All CLDR locales.");
                } else if (locs.length == 0) {
                    feed.setDescription("No locales.");
                } else {
                    String locslist = null;
                    for (String l : locs) {
                        if (locslist == null) {
                            locslist = l;
                        } else {
                            locslist = locslist + " " + l;
                        }
                    }
                    feed.setDescription("Your locales: " + locslist);
                }

                SyndEntry entry;
                SyndContent description;
                // not a specific locale, but ALL [of my locales]

                try {
                    Connection conn = null;
                    PreparedStatement pAll = null;
                    try {
                        conn = sm.dbUtils.getDBConnection();
                        // first, articles.
                        ResultSet rs = null; // list of articles.

                        if (locs == null) {
                            pAll = prepare_pAll(conn);
                            rs = pAll.executeQuery();
                        } else {
                            pAll = prepare_pForMe(conn);
                            pAll.setInt(1, user.id);
                            rs = pAll.executeQuery();
                        }

                        while (rs.next() && true) {
                            int poster = rs.getInt(1);
                            String subj = DBUtils.getStringUTF8(rs, 2);
                            String text = DBUtils.getStringUTF8(rs, 3);
                            java.sql.Timestamp lastDate = rs.getTimestamp(4); // TODO:
                            // timestamp

                            subj = validateForXML(subj);
                            text = validateForXML(text);
                            int id = rs.getInt(5);
                            //String forum = rs.getString(6);
                            String ploc = rs.getString(7);

                            String forumText = localeToForum(ploc);
                            String nameLink = getNameTextFromUid(user, poster);

                            entry = new SyndEntryImpl();
                            if (subj.startsWith(forumText)) {
                                entry.setTitle(subj);
                            } else {
                                entry.setTitle(forumText + ":" + subj);
                            }
                            entry.setAuthor(nameLink);
                            entry.setLink(base + "?forum=" + forumText + "&amp;" + F_DO + "=" + F_VIEW + "&amp;id=" + id
                                + "&amp;email=" + email + "&amp;pw=" + pw);
                            entry.setPublishedDate(lastDate); // dateParser.parse("2004-06-08"));
                            description = new SyndContentImpl();
                            description.setType("text/html");
                            description.setValue("From: " + nameLink + "<br><hr>" + shortenText(text));
                            entry.setDescription(description);
                            entries.add(entry);
                        }
                    } finally {
                        DBUtils.close(pAll, conn);
                    }
                } catch (SQLException se) {
                    String complaint = "SurveyForum:  Couldn't use forum s for RSS- " + DBUtils.unchainSqlException(se)
                        + " - fGetByLoc";
                    logger.severe(complaint);
                    throw new RuntimeException(complaint);
                }
            }
            feed.setEntries(entries);

            Writer writer = response.getWriter();
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed, writer);
        } catch (Throwable ie) {
            SurveyLog.logException(ie, "getting RSS feed for " + loc + " and user " + user.toString());
            System.err.println("Error getting RSS feed: " + ie.toString());
            ie.printStackTrace();
        }
        return true;
    }

    /**
     * Make sure the string is valid XML.
     * @param str
     * @return
     */
    private final String validateForXML(String str) {
        if (VALID_FOR_XML.containsAll(str)) {
            return str;
        } else {
            UnicodeSet tmpSet = new UnicodeSet(VALID_FOR_XML)
                .complement()
                .retainAll(str);
            StringBuilder sb = new StringBuilder(str.length());
            sb.append("((INVALID CHARS: " + tmpSet.toString() + " )) ");
            int cp;
            for (UCharacterIterator ui = UCharacterIterator.getInstance(str); (cp = ui.next()) != UCharacterIterator.DONE;) {
                if (VALID_FOR_XML.contains(cp)) {
                    sb.append(Character.toChars(cp));
                } else {
                    sb.append("\\x{" + Integer.toHexString(cp) + "}");
                }
            }
            return sb.toString();
        }
    }

    private static String shortenText(String s) {
        if (s.length() < 100) {
            return s;
        } else {
            return s.substring(0, 100) + "...";
        }
    }

    /**
     * RSS???
     *
     * @param ctx
     * @return
     *
     * Maybe called by SurveyMain.printHeader
     */
    String forumFeedStuff(WebContext ctx) {
        if (ctx.session == null || ctx.session.user == null || !UserRegistry.userIsStreet(ctx.session.user)) {
            return "";
        }
        String feedUrl = ctx.schemeHostPort()
            + ctx.base()
            + ("/feed?_=" + localeToForum(ctx.getLocale()) + "&amp;email=" + ctx.session.user.email + "&amp;pw="
                + ctx.session.user.password + "&amp;");
        return
        /*
         * "<link rel=\"alternate\" type=\"application/atom+xml\" title=\"Atom 1.0\" href=\""
         * +feedUrl+"&feed=atom_1.0\">" +
         */
        "<link rel=\"alternate\" type=\"application/rdf+xml\" title=\"RSS 1.0\" href=\"" + feedUrl + "&feed=rss_1.0\">"
            + "<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS 2.0\" href=\"" + feedUrl + "&feed=rss_2.0\">";
    }

    /**
     * RSS???
     *
     * @param ctx
     * @param forum
     * @return
     *
     * Maybe called from usermenu.jsp as well as locally
     */
    public static String forumFeedIcon(WebContext ctx, String forum) {
        if (ctx.session == null || ctx.session.user == null || !UserRegistry.userIsStreet(ctx.session.user)) {
            return "";
        }
        String feedUrl = ctx.schemeHostPort()
            + ctx.base()
            + ("/feed?_=" + localeToForum(ctx.getLocale()) + "&amp;email=" + ctx.session.user.email + "&amp;pw="
                + ctx.session.user.password + "&amp;");

        return " <a href='" + feedUrl + "&feed=rss_2.0" + "'>" + ctx.iconHtml("feed", "RSS 2.0") + "<!-- Forum&nbsp;rss --></a>";
    }

    /**
     * RSS???
     *
     * @param ctx
     * @return
     *
     * Maybe called by SurveyMain.printHeader
     */
    String mainFeedStuff(WebContext ctx) {
        if (ctx.session == null || ctx.session.user == null || !UserRegistry.userIsStreet(ctx.session.user)) {
            return "";
        }

        String feedUrl = ctx.schemeHostPort() + ctx.base()
            + ("/feed?email=" + ctx.session.user.email + "&amp;pw=" + ctx.session.user.password + "&amp;");
        return "<link rel=\"alternate\" type=\"application/atom+xml\" title=\"Atom 1.0\" href=\"" + feedUrl + "&feed=atom_1.0\">"
            + "<link rel=\"alternate\" type=\"application/rdf+xml\" title=\"RSS 1.0\" href=\"" + feedUrl + "&feed=rss_1.0\">"
            + "<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS 2.0\" href=\"" + feedUrl + "&feed=rss_2.0\">";
    }

    /**
     * RSS???
     *
     * @param ctx
     * @return
     *
     * Maybe called by SurveyMain.doLocaleList
     */
    public String mainFeedIcon(WebContext ctx) {
        if (ctx.session == null || ctx.session.user == null || !UserRegistry.userIsStreet(ctx.session.user)) {
            return "";
        }
        String feedUrl = ctx.schemeHostPort() + ctx.base()
            + ("/feed?email=" + ctx.session.user.email + "&amp;pw=" + ctx.session.user.password + "&amp;");

        return "<a href='" + feedUrl + "&feed=rss_2.0" + "'>" + ctx.iconHtml("feed", "RSS 2.0") + "RSS 2.0</a>";
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
     * displaying it to the user (which is done by parseForumContent in survey.js).
     *
     * @param session
     * @param locale
     * @param base_xpath Base XPath of the item being viewed, if positive
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
                final CharSequence forumPosts = DBUtils.Table.FORUM_POSTS.toString();
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
                /*
                 * TODO: get the ForumStatus for each post. Probably should do a join on the two tables FORUM_POSTS and FORUM_STATUS above.
                 */
                ForumStatus forumStatus = ForumStatus.OPEN;
                if (o != null) {
                    /* Gather the post data. Note that showPost is not called here.
                     * The formatting is done by parseForumContent in survey.js.
                     */
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
                                .put("forumStatus", forumStatus)
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
            String complaint = "SurveyForum:  Couldn't show posts in forum " + locale + " - " + DBUtils.unchainSqlException(se)
                + " - fGetByLoc";
            logger.severe(complaint);
            throw new RuntimeException(complaint);
        }
    }

    private void assertCanAccessForum(CookieSession session, CLDRLocale locale) throws SurveyException {
        if (session == null || session.user == null) throw new SurveyException(ErrorCode.E_NOT_LOGGED_IN);
        assertCanAccessForum(session.user, locale);
    }

    private void assertCanAccessForum(UserRegistry.User user, CLDRLocale locale) throws SurveyException {
        boolean canModify = (UserRegistry.userCanAccessForum(user, locale));
        if (!canModify)
            throw new SurveyException(ErrorCode.E_NO_PERMISSION, "You do not have permission to access that locale");
    }

    private static String getPallresult() {
        Table forumPosts = DBUtils.Table.FORUM_POSTS;
        return getPallresult(forumPosts.toString());
    }

    private static String getPallresult(String forumPosts) {
        return forumPosts + ".poster," + forumPosts + ".subj," + forumPosts + ".text,"
            + forumPosts.toString()
            + ".last_time," + forumPosts + ".id," + forumPosts + ".forum, " + forumPosts + ".loc ";
    }

    /**
     * Construct a portion of an sql query for getting all needed columns from the forum posts table.
     * 
     * @param forumPosts the table name
     * @return the string to be used as part of a query
     */
    private static String getPallresultfora(final CharSequence forumPosts) {
        return forumPosts + ".poster," + forumPosts + ".subj," + forumPosts + ".text,"
            + forumPosts.toString()
            + ".last_time," + forumPosts + ".id," + forumPosts + ".parent," + forumPosts + ".xpath, "
            + forumPosts + ".loc," + forumPosts + ".version";
    }

    /**
     * Respond when the user adds a new forum post.
     *
     * @param mySession the CookieSession
     * @param xpath of the form "stringid" or "#1234"
     * @param l the CLDRLocale
     * @param subj the subject of the post
     * @param text the text of the post
     * @param status the status string such as "Open", or null
     * @param replyTo could be {@link #NO_PARENT} if there is no parent
     * @return the post id
     *
     * @throws SurveyException
     */
    public int doPost(CookieSession mySession, String xpath, CLDRLocale l, String subj, String text, String status, int replyTo) throws SurveyException {
        assertCanAccessForum(mySession, l);
        int base_xpath;
        if (replyTo < 0) {
            replyTo = NO_PARENT;
            base_xpath = sm.xpt.getXpathIdOrNoneFromStringID(xpath);
        } else {
            base_xpath = getXpathForPost(replyTo); // base_xpath is ignored on replies.
        }
        final boolean couldFlagOnLosing = couldFlagOnLosing(mySession.user, sm.xpt.getById(base_xpath), l) && !sm.getSTFactory().getFlag(l, base_xpath);

        if (couldFlagOnLosing) {
            text = text + FLAGGED_FOR_REVIEW_HTML;
        }
        return sm.fora.doPostInternal(base_xpath, replyTo, l, subj, text, status, couldFlagOnLosing, mySession.user);
    }

    /**
     * Status values associated with forum posts and threads
     */
    private enum ForumStatus {
        CLOSED(0, "Closed"),
        OPEN(1, "Open"),
        DISPUTED(2, "Disputed"),
        AGREED(3, "Agreed");

        ForumStatus(int id, String name) {
            this.id = id;
            this.name = name;
        }

        private final int id;
        private final String name;

        /**
         * Get the integer id for this ForumStatus
         *
         * @return the id
         */
        public int toInt() {
            return id;
        }

        /**
         * Get a ForumStatus value from its name, or if the name is not associated with
         * a ForumStatus value, use the given default ForumStatus
         *
         * @param name
         * @param defaultStatus
         * @return the ForumStatus
         */
        public static ForumStatus fromName(String name, ForumStatus defaultStatus) {
            if (name != null) {
                for (ForumStatus s : ForumStatus.values()) {
                    if (s.name.equals(name)) {
                        return s;
                    }
                }
            }
            return defaultStatus;
        }

        /**
         * Does the given ForumStatus belong in the FORUM_STATUS db table?
         *
         * Keep the table smaller by not storing rows in it for status CLOSED.
         *
         * @return true or false
         */
        public boolean belongsInTable() {
            return this != CLOSED;
        }
    }
}
