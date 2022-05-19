package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.lang.UCharacter;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfig.Environment;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.LocaleNormalizer;
import org.unicode.cldr.util.LocaleSet;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.VoteResolver.VoterInfo;


public class DBUserRegistry extends UserRegistry {
    private static final java.util.logging.Logger logger = SurveyLog.forClass(DBUserRegistry.class);


    /**
     * The name of the user sql database
     */
    public static final String CLDR_USERS = "cldr_users";
    public static final String CLDR_INTEREST = "cldr_interest";

    public static final String SQL_insertStmt = "INSERT INTO " + CLDR_USERS
        + "(userlevel,name,org,email,password,locales,lastlogin) " + "VALUES(?,?,?,?,?,?,NULL)";
    public static final String SQL_queryStmt_FRO = "SELECT id,name,userlevel,org,locales,intlocs,lastlogin from " + CLDR_USERS
        + " where email=? AND password=?";
    public static final String SQL_queryIdStmt_FRO = "SELECT name,org,email,userlevel,intlocs,locales,lastlogin,password from "
        + CLDR_USERS + " where id=?";
    public static final String SQL_queryEmailStmt_FRO = "SELECT id,name,userlevel,org,locales,intlocs,lastlogin,password from "
        + CLDR_USERS + " where email=?";
    public static final String SQL_touchStmt = "UPDATE " + CLDR_USERS + " set lastlogin=CURRENT_TIMESTAMP where id=?";
    public static final String SQL_removeIntLoc = "DELETE FROM " + CLDR_INTEREST + " WHERE uid=?";
    public static final String SQL_updateIntLoc = "INSERT INTO " + CLDR_INTEREST + " (uid,forum) VALUES(?,?)";

    private UserSettingsData userSettings;

    @Override
    public
    UserSettingsData getUserSettings() {
        return userSettings;
    }

    /**
     * Called by SM to create the reg
     */
    public static DBUserRegistry createRegistry(SurveyMain theSm) throws SQLException {
        DBUserRegistry reg = new DBUserRegistry(theSm);
        reg.setupDB();
        return reg;
    }

    SurveyMain sm = null; // for notifyuser

    private DBUserRegistry(SurveyMain theSm) {
        this.sm = theSm;
    }

    @Override
    public
    Set<String> getCovGroupsForOrg(String st_org) {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement s = null;
        Set<String> res = new HashSet<>();

        try {
            conn = DBUtils.getInstance().getAConnection();
            s = DBUtils
                .prepareStatementWithArgs(
                    conn,
                    "select distinct cldr_interest.forum from cldr_interest where exists (select * from cldr_users  where cldr_users.id=cldr_interest.uid 	and cldr_users.org=?)",
                    st_org);
            rs = s.executeQuery();
            while (rs.next()) {
                res.add(rs.getString(1));
            }
            return res;
        } catch (SQLException se) {
            SurveyLog.logException(se, "Querying cov groups for org " + st_org, null);
            throw new InternalError("error: " + se.toString());
        } finally {
            DBUtils.close(rs, s, conn);
        }
    }

    @Override
    public Set<CLDRLocale> anyVotesForOrg(String st_org) {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement s = null;
        Set<CLDRLocale> res = new HashSet<>();

        try {
            conn = DBUtils.getInstance().getAConnection();
            s = DBUtils
                .prepareStatementWithArgs(
                    conn,
                    "select distinct " + DBUtils.Table.VOTE_VALUE + ".locale from " + DBUtils.Table.VOTE_VALUE
                        + " where exists (select * from cldr_users	where " + DBUtils.Table.VOTE_VALUE + ".submitter=cldr_users.id and cldr_users.org=?)",
                    st_org);
            rs = s.executeQuery();
            while (rs.next()) {
                res.add(CLDRLocale.getInstance(rs.getString(1)));
            }
            return res;
        } catch (SQLException se) {
            SurveyLog.logException(se, "Querying voter locs for org " + st_org, null);
            throw new InternalError("error: " + se.toString());
        } finally {
            DBUtils.close(rs, s, conn);
        }
    }


    /**
     * internal - called to setup db
     */
    private void setupDB() throws SQLException {
        // must be set up first.
        userSettings = UserSettingsData.getInstance(sm);

        String sql = null;
        Connection conn = DBUtils.getInstance().getDBConnection();
        try {
            synchronized (conn) {
                boolean hadUserTable = DBUtils.hasTable(conn, CLDR_USERS);
                if (!hadUserTable) {
                    sql = createUserTable(conn);
                    conn.commit();
                } else if (!DBUtils.db_Derby) {
                    /* update table to DATETIME instead of TIMESTAMP */
                    Statement s = conn.createStatement();
                    sql = "alter table cldr_users change lastlogin lastlogin DATETIME";
                    s.execute(sql);
                    s.close();
                    conn.commit();
                }

                //create review and post table
                sql = "(see ReviewHide.java)";
                ReviewHide.createTable(conn);
                boolean hadInterestTable = DBUtils.hasTable(conn, CLDR_INTEREST);
                if (!hadInterestTable) {
                    Statement s = conn.createStatement();

                    sql = ("create table " + CLDR_INTEREST + " (uid INT NOT NULL , " + "forum  varchar(256) not null " + ")");
                    s.execute(sql);
                    sql = "CREATE  INDEX " + CLDR_INTEREST + "_id_loc ON " + CLDR_INTEREST + " (uid) ";
                    s.execute(sql);
                    sql = "CREATE  INDEX " + CLDR_INTEREST + "_id_for ON " + CLDR_INTEREST + " (forum) ";
                    s.execute(sql);
                    SurveyLog.debug("DB: created " + CLDR_INTEREST);
                    sql = null;
                    s.close();
                    conn.commit();
                }

                myinit(); // initialize the prepared statements

                if (!hadInterestTable) {
                    setupIntLocs(); // set up user -> interest table mapping
                }

            }
        } catch (SQLException se) {
            se.printStackTrace();
            System.err.println("SQL err: " + DBUtils.unchainSqlException(se));
            System.err.println("Last SQL run: " + sql);
            throw se;
        } finally {
            DBUtils.close(conn);
        }
    }

    private void myinit() throws SQLException {
    }

    /**
     * @param conn
     * @return
     * @throws SQLException
     */
    private String createUserTable(Connection conn) throws SQLException {
        String sql;
        Statement s = conn.createStatement();

        sql = ("create table " + CLDR_USERS + "(id INT NOT NULL " + DBUtils.DB_SQL_IDENTITY + ", " + "userlevel int not null, "
            + "name " + DBUtils.DB_SQL_UNICODE + " not null, " + "email varchar(128) not null UNIQUE, "
            + "org varchar(256) not null, " + "password varchar(100) not null, " + "audit varchar(1024) , "
            + "locales varchar(1024) , " +
            // "prefs varchar(1024) , " + /* deprecated Dec 2010. Not used
            // anywhere */
            "intlocs varchar(1024) , " + // added apr 2006: ALTER table
            // CLDR_USERS ADD COLUMN intlocs
            // VARCHAR(1024)
            "lastlogin " + DBUtils.DB_SQL_TIMESTAMP0 + // added may 2006:
            // alter table
            // CLDR_USERS ADD
            // COLUMN lastlogin
            // TIMESTAMP
            (!DBUtils.db_Mysql ? ",primary key(id)" : "") + ")");
        s.execute(sql);
        sql = ("INSERT INTO " + CLDR_USERS + "(userlevel,name,org,email,password) " + "VALUES(" + ADMIN + "," + "'admin',"
            + "'SurveyTool'," + "'" + ADMIN_EMAIL + "'," + "'" + SurveyMain.vap + "')");
        s.execute(sql);
        sql = null;
        SurveyLog.debug("DB: added user Admin");

        s.close();
        return sql;
    }

    public UserRegistry.User getInfo(int id) {
        if (id < 0) {
            return null;
        }
        synchronized (infoArray) {
            User ret = null;
            try {
                ret = infoArray[id];
            } catch (IndexOutOfBoundsException ioob) {
                ret = null; // not found
            }

            if (ret == null) { // synchronized(conn) {
                ResultSet rs = null;
                PreparedStatement pstmt = null;
                Connection conn = DBUtils.getInstance().getAConnection();
                try {
                    pstmt = DBUtils.prepareForwardReadOnly(conn, SQL_queryIdStmt_FRO);
                    pstmt.setInt(1, id);
                    // First, try to query it back from the DB.
                    rs = pstmt.executeQuery();
                    if (!rs.next()) {
                        return null;
                    }
                    User u = new UserRegistry.User(id);
                    // from params:
                    u.name = DBUtils.getStringUTF8(rs, 1);
                    u.org = rs.getString(2);
                    u.getOrganization(); // verify

                    u.email = rs.getString(3);
                    u.userlevel = rs.getInt(4);
                    u.intlocs = rs.getString(5);
                    u.locales = LocaleNormalizer.normalizeQuietly(rs.getString(6));
                    u.last_connect = rs.getTimestamp(7);
                    u.password = rs.getString(8);
                    ret = u; // let it finish..

                    if (id >= arraySize) {
                        int newchunk = (((id + 1) / CHUNKSIZE) + 1) * CHUNKSIZE;
                        infoArray = new UserRegistry.User[newchunk];
                        arraySize = newchunk;
                    }
                    infoArray[id] = u;
                    // good so far..
                    if (rs.next()) {
                        // dup returned!
                        throw new InternalError("Dup user id # " + id);
                    }
                } catch (SQLException se) {
                    logger.log(java.util.logging.Level.SEVERE,
                        "UserRegistry: SQL error trying to get #" + id + " - " + DBUtils.unchainSqlException(se), se);
                    throw new InternalError("UserRegistry: SQL error trying to get #" + id + " - "
                        + DBUtils.unchainSqlException(se));
                } catch (Throwable t) {
                    logger.log(java.util.logging.Level.SEVERE, "UserRegistry: some error trying to get #" + id, t);
                    throw new InternalError("UserRegistry: some error trying to get #" + id + " - " + t.toString());
                } finally {
                    // close out the RS
                    DBUtils.close(rs, pstmt, conn);
                } // end try
            }
            return ret;
        } // end synch array
    }

    /**
     * info = name/email/org immutable info, keep it in a separate list for
     * quick lookup.
     */
    public final static int CHUNKSIZE = 128;
    int arraySize = 0;
    UserRegistry.User infoArray[] = new UserRegistry.User[arraySize];


    @Override
    public void userModified(int id) {
        synchronized (infoArray) {
            try {
                infoArray[id] = null;
            } catch (IndexOutOfBoundsException ioob) {
                // nothing to do
            }
        }
        userModified(); // do this if any users are modified
    }

    /**
     * Mark the UserRegistry as changed, purging the VoterInfo map
     *
     * @see #getVoterToInfo()
     */
    protected void userModified() {
        voterInfo = null;
    }

    @Override
    public void touch(int id)  {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();
            pstmt = conn.prepareStatement(SQL_touchStmt);
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            conn.commit();
        } catch (SQLException se) {
            logger.log(java.util.logging.Level.SEVERE,
                "UserRegistry: SQL error trying to touch " + id + " - " + DBUtils.unchainSqlException(se), se);
            throw new InternalError("UserRegistry: SQL error trying to touch " + id + " - " + DBUtils.unchainSqlException(se));
        } finally {
            DBUtils.close(pstmt, conn);
        }
    }

    @Override
    public UserRegistry.User get(String pass, String email, String ip, boolean letmein) throws LogoutException {
        if ((email == null) || (email.length() <= 0)) {
            return null; // nothing to do
        }
        if (((pass != null && pass.length() <= 0)) && !letmein) {
            return null; // nothing to do
        }

        email = normalizeEmail(email);

        ResultSet rs = null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtils.getInstance().getAConnection();
            if ((pass != null) && !letmein) {
                pstmt = DBUtils.prepareForwardReadOnly(conn, SQL_queryStmt_FRO);
                pstmt.setString(1, email);
                pstmt.setString(2, pass);
            } else {
                pstmt = DBUtils.prepareForwardReadOnly(conn, SQL_queryEmailStmt_FRO);
                pstmt.setString(1, email);
            }
            // First, try to query it back from the DB.
            rs = pstmt.executeQuery();
            if (!rs.next()) { // user was not found.
                throw new UserRegistry.LogoutException();
            }
            User u = new UserRegistry.User(rs.getInt(1));

            // from params:
            u.password = pass;
            if (letmein) {
                u.password = rs.getString(8);
            }
            u.email = normalizeEmail(email);
            // from db: (id,name,userlevel,org,locales)
            u.name = DBUtils.getStringUTF8(rs, 2);// rs.getString(2);
            u.userlevel = rs.getInt(3);
            u.org = rs.getString(4);
            u.locales = rs.getString(5);
            u.intlocs = rs.getString(6);
            u.last_connect = rs.getTimestamp(7);

            // good so far..

            if (rs.next()) {
                // dup returned!
                logger.severe("Duplicate user for " + email + " - ids " + u.id + " and " + rs.getInt(1));
                return null;
            }
            return u;
        } catch (SQLException se) {
            logger.log(java.util.logging.Level.SEVERE,
                "UserRegistry: SQL error trying to get " + email + " - " + DBUtils.unchainSqlException(se), se);
            throw new InternalError("UserRegistry: SQL error trying to get " + email + " - " + DBUtils.unchainSqlException(se));
        } catch (LogoutException le) {
            if (pass != null) {
                // only log this if they were actually trying to login.
                logger.log(java.util.logging.Level.SEVERE, "AUTHENTICATION FAILURE; email=" + email + "; ip=" + ip);
            }
            throw le; // bubble
        } catch (Throwable t) {
            logger.log(java.util.logging.Level.SEVERE, "UserRegistry: some error trying to get " + email, t);
            throw new InternalError("UserRegistry: some error trying to get " + email + " - " + t.toString());
        } finally {
            // close out the RS
            DBUtils.close(rs, pstmt, conn);
        } // end try
    } // end get

    // ------- special things for "list" mode:

    public java.sql.PreparedStatement list(String organization, Connection conn) throws SQLException {
        if (organization == null) {
            return DBUtils.prepareStatementForwardReadOnly(conn, "listAllUsers", "SELECT id,userlevel,name,email,org,locales,intlocs,lastlogin FROM " + CLDR_USERS + " ORDER BY org,userlevel,name ");
        } else {
            PreparedStatement ps = DBUtils.prepareStatementWithArgsFRO(conn,
                    "SELECT id,userlevel,name,email,org,locales,intlocs,lastlogin FROM " + CLDR_USERS + " WHERE org=? ORDER BY org,userlevel,name");
            ps.setString(1,  organization);
            return ps;
        }
    }

    public java.sql.ResultSet listPass(Connection conn) throws SQLException {
        ResultSet rs = null;
        Statement s = null;
        final String ORDER = " ORDER BY id ";
        s = conn.createStatement();
        rs = s.executeQuery("SELECT id,userlevel,name,email,org,locales,intlocs, password FROM " + CLDR_USERS + ORDER);
        return rs;
    }

    private void setupIntLocs() throws SQLException {
        Connection conn = DBUtils.getInstance().getDBConnection();
        PreparedStatement removeIntLoc = null;
        PreparedStatement updateIntLoc = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            removeIntLoc = conn.prepareStatement(SQL_removeIntLoc);
            updateIntLoc = conn.prepareStatement(SQL_updateIntLoc);
            ps = list(null, conn);
            rs = ps.executeQuery();
            ElapsedTimer et = new ElapsedTimer();
            int count = 0;
            while (rs.next()) {
                int user = rs.getInt(1);
                updateIntLocs(user, false, conn, removeIntLoc, updateIntLoc);
                count++;
            }
            conn.commit();
            SurveyLog.debug("update:" + count + " user's locales updated " + et);
        } finally {
            DBUtils.close(removeIntLoc, updateIntLoc, conn, ps, rs);
        }
    }

    /**
     * assumes caller has a lock on conn
     */
    private String updateIntLocs(int user, Connection conn) throws SQLException {
        PreparedStatement removeIntLoc = null;
        PreparedStatement updateIntLoc = null;
        try {
            removeIntLoc = conn.prepareStatement(SQL_removeIntLoc);
            updateIntLoc = conn.prepareStatement(SQL_updateIntLoc);
            return updateIntLocs(user, true, conn, removeIntLoc, updateIntLoc);
        } finally {
            DBUtils.close(removeIntLoc, updateIntLoc);
        }
    }

    /**
     * assumes caller has a lock on conn
     */
    private String updateIntLocs(int id, boolean doCommit, Connection conn, PreparedStatement removeIntLoc, PreparedStatement updateIntLoc)
        throws SQLException {

        User user = getInfo(id);
        if (user == null) {
            return "";
        }
        logger.finer("uil: remove for user " + id);
        removeIntLoc.setInt(1, id);
        removeIntLoc.executeUpdate();

        LocaleSet intLocSet = user.getInterestLocales();
        logger.finer("uil: intlocs " + id + " = " + intLocSet.toString());
        if (intLocSet != null && !intLocSet.isAllLocales() && !intLocSet.isEmpty()) {
            /*
             * Simplify locales. For example simplify "pt_PT" to "pt" with loc.getLanguage().
             * Avoid adding duplicates to the table. For example, if the same user has both
             * "pt" and "pt_PT", only add one row, for "pt".
             */
            Set<String> languageSet = new TreeSet<>();
            for (CLDRLocale loc : intLocSet.getSet()) {
                languageSet.add(loc.getLanguage());
            }
            for (String lang : languageSet) {
                logger.finer("uil: intlocs " + id + " + " + lang);
                updateIntLoc.setInt(1, id);
                updateIntLoc.setString(2, lang);
                updateIntLoc.executeUpdate();
            }
        }

        if (doCommit) {
            conn.commit();
        }
        return "";
    }

    @Override
    public String setUserLevel(User me, User them, int newLevel) {
        if (!canSetUserLevel(me, them, newLevel)) {
            return ("[Permission Denied]");
        }
        String orgConstraint = null;
        String msg = "";
        if (me.userlevel == ADMIN) {
            orgConstraint = ""; // no constraint
        } else {
            orgConstraint = " AND org='" + me.org + "' ";
        }
        Connection conn = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();
            Statement s = conn.createStatement();
            String theSql = "UPDATE " + CLDR_USERS + " SET userlevel=" + newLevel + " WHERE id=" + them.id + " AND email='"
                + them.email + "' " + orgConstraint;
            logger.info("Attempt user update by " + me.email + ": " + theSql);
            int n = s.executeUpdate(theSql);
            conn.commit();
            userModified(them.id);
            if (n == 0) {
                msg = msg + " [Error: no users were updated!] ";
                logger.severe("Error: 0 records updated.");
            } else if (n != 1) {
                msg = msg + " [Error in updating users!] ";
                logger.severe("Error: " + n + " records updated!");
            } else {
                msg = msg + " [user level set]";
                msg = msg + updateIntLocs(them.id, conn);
            }
        } catch (SQLException se) {
            msg = msg + " exception: " + DBUtils.unchainSqlException(se);
        } catch (Throwable t) {
            msg = msg + " exception: " + t.toString();
        } finally {
            DBUtils.closeDBConnection(conn);
        }

        return msg;
    }


    @Override
    public String setLocales(CookieSession session, User user, String newLocales, boolean intLocs) {
        int theirId = user.id;
        String theirEmail = user.email;

        // make sure other user is at or below userlevel
        if (!intLocs && !session.user.isAdminFor(getInfo(theirId))) {
            return ("[Permission Denied]");
        }
        String msg = "";
        if (!intLocs) {
            final LocaleNormalizer locNorm = new LocaleNormalizer();
            newLocales = locNorm.normalizeForSubset(newLocales, user.getOrganization().getCoveredLocales());
            if (locNorm.hasMessage()) {
                msg = locNorm.getMessageHtml() + "<br />";
            }
        } else {
            newLocales = LocaleNormalizer.normalizeQuietly(newLocales);
        }
        String orgConstraint = null;
        if (session.user.userlevel == ADMIN) {
            orgConstraint = ""; // no constraint
        } else {
            orgConstraint = " AND org='" + session.user.org + "' ";
        }
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            final String normalizedLocales = newLocales;
            conn = DBUtils.getInstance().getDBConnection();
            String theSql = "UPDATE " + CLDR_USERS + " SET " + (intLocs ? "intlocs" : "locales") + "=? WHERE id=" + theirId
                + " AND email='" + theirEmail + "' " + orgConstraint;
            ps = conn.prepareStatement(theSql);
            logger.info("Attempt user locales update by " + session.user.email + ": " + theSql + " - " + newLocales);
            ps.setString(1, normalizedLocales);
            int n = ps.executeUpdate();
            conn.commit();
            userModified(theirId);
            if (n == 0) {
                msg += " [Error: no users were updated!] ";
                logger.severe("Error: 0 records updated.");
            } else if (n != 1) {
                msg += " [Error in updating users!] ";
                logger.severe("Error: " + n + " records updated!");
            } else {
                msg += " [locales set]" + updateIntLocs(theirId, conn);
            }
        } catch (SQLException se) {
            msg = msg + " exception: " + DBUtils.unchainSqlException(se);
        } catch (Throwable t) {
            msg = msg + " exception: " + t.toString();
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException se) {
                logger.log(java.util.logging.Level.SEVERE,
                    "UserRegistry: SQL error trying to close. " + DBUtils.unchainSqlException(se), se);
            }
        }
        return msg;
    }

    @Override
    public
    String delete(WebContext ctx, int theirId, String theirEmail) {
        if (!ctx.session.user.isAdminFor(getInfo(theirId))) {
            return ("[Permission Denied]");
        }

        String orgConstraint = null; // keep org constraint in place
        String msg = "";
        if (ctx.session.user.userlevel == ADMIN) {
            orgConstraint = ""; // no constraint
        } else {
            orgConstraint = " AND org='" + ctx.session.user.org + "' ";
        }
        Connection conn = null;
        Statement s = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();
            s = conn.createStatement();
            String theSql = "DELETE FROM " + CLDR_USERS + " WHERE id=" + theirId + " AND email='" + theirEmail + "' "
                + orgConstraint;
            logger.info("Attempt user DELETE by " + ctx.session.user.email + ": " + theSql);
            int n = s.executeUpdate(theSql);
            conn.commit();
            userModified(theirId);
            if (n == 0) {
                msg = msg + " [Error: no users were removed!] ";
                logger.severe("Error: 0 users removed.");
            } else if (n != 1) {
                msg = msg + " [Error in removing users!] ";
                logger.severe("Error: " + n + " records removed!");
            } else {
                msg = msg + " [removed OK]";
            }
        } catch (SQLException se) {
            msg = msg + " exception: " + DBUtils.unchainSqlException(se);
        } catch (Throwable t) {
            msg = msg + " exception: " + t.toString();
        } finally {
            DBUtils.close(s, conn);
        }
        return msg;
    }


    @Override
    String updateInfo(WebContext ctx, int theirId, String theirEmail, InfoType type, String value) {
        if (type == InfoType.INFO_ORG && ctx.session.user.userlevel > ADMIN) {
            return ("[Permission Denied]");
        }

        if (type == InfoType.INFO_EMAIL) {
            value = normalizeEmail(value);
        } else {
            value = value.trim();
        }

        if (!ctx.session.user.isAdminFor(getInfo(theirId))) {
            return ("[Permission Denied]");
        }

        String msg = "";
        Connection conn = null;
        PreparedStatement updateInfoStmt = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();

            updateInfoStmt = conn.prepareStatement("UPDATE " + CLDR_USERS + " set " + type.field() + "=? WHERE id=? AND email=?");
            if (type == UserRegistry.InfoType.INFO_NAME) { // unicode treatment
                DBUtils.setStringUTF8(updateInfoStmt, 1, value);
            } else {
                updateInfoStmt.setString(1, value);
            }
            updateInfoStmt.setInt(2, theirId);
            updateInfoStmt.setString(3, theirEmail);

            logger.info("Attempt user UPDATE by " + ctx.session.user.email + ": " + type.toString() + " = "
                + ((type != InfoType.INFO_PASSWORD) ? value : "********"));
            int n = updateInfoStmt.executeUpdate();
            conn.commit();
            userModified(theirId);
            if (n == 0) {
                msg = msg + " [Error: no users were updated!] ";
                logger.severe("Error: 0 users updated.");
            } else if (n != 1) {
                msg = msg + " [Error in updated users!] ";
                logger.severe("Error: " + n + " updated removed!");
            } else {
                msg = msg + " [updated OK]";
            }
        } catch (SQLException se) {
            msg = msg + " exception: " + DBUtils.unchainSqlException(se);
        } catch (Throwable t) {
            msg = msg + " exception: " + t.toString();
        } finally {
            DBUtils.close(updateInfoStmt, conn);
        }
        return msg;
    }


    @Override
    public String resetPassword(String forEmail, String ip) {
        String msg = "";
        String newPassword = CookieSession.newId(false);
        if (newPassword.length() > 10) {
            newPassword = newPassword.substring(0, 10);
        }
        Connection conn = null;
        PreparedStatement updateInfoStmt = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();

            updateInfoStmt = DBUtils.prepareStatementWithArgs(conn, "UPDATE " + CLDR_USERS + " set password=? ,  audit=? WHERE email=? AND userlevel <"
                + LOCKED + "  AND userlevel >= " + TC, newPassword,
                "Reset: " + new Date().toString() + " by " + ip,
                forEmail);

            logger.info("** Attempt password reset " + forEmail + " from " + ip);
            int n = updateInfoStmt.executeUpdate();

            conn.commit();
            userModified(forEmail);
            if (n == 0) {
                msg = msg + "Error: no valid accounts found";
                logger.severe("Error in password reset:: 0 users updated.");
            } else if (n != 1) {
                msg = msg + " [Error in updated users!] ";
                logger.severe("Error in password reset: " + n + " updated removed!");
            } else {
                msg = msg + "OK";
                sm.notifyUser(null, forEmail, newPassword);
            }
        } catch (SQLException se) {
            SurveyLog.logException(logger, se, "Resetting password for user " + forEmail + " from " + ip);
            msg = msg + " exception";
        } catch (Throwable t) {
            SurveyLog.logException(logger, t, "Resetting password for user " + forEmail + " from " + ip);
            msg = msg + " exception: " + t.toString();
        } finally {
            DBUtils.close(updateInfoStmt, conn);
        }
        return msg;
    }

    @Override
    public String lockAccount(String forEmail, String reason, String ip) {
        String msg = "";
        User u = this.get(forEmail);
        logger.info("** Attempt LOCK " + forEmail + " from " + ip + " reason " + reason);
        String newPassword = CookieSession.newId(false);
        if (newPassword.length() > 10) {
            newPassword = newPassword.substring(0, 10);
        }
        if (reason.length() > 500) {
            reason = reason.substring(0, 500) + "...";
        }
        Connection conn = null;
        PreparedStatement updateInfoStmt = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();

            updateInfoStmt = DBUtils.prepareStatementWithArgs(conn, "UPDATE " + CLDR_USERS + " SET password=?,userlevel=" + LOCKED
                + ", audit=? WHERE email=? AND userlevel <" + LOCKED + " AND userlevel >= " + TC,
                newPassword,
                "Lock: " + new Date().toString() + " by " + ip + ":" + reason,
                forEmail);

            int n = updateInfoStmt.executeUpdate();

            conn.commit();
            userModified(forEmail);
            if (n == 0) {
                msg = "Error: no valid accounts found";
                logger.severe("Error in LOCK:: 0 users updated.");
            } else if (n != 1) {
                msg = "[Error in updated users!] ";
                logger.severe("Error in LOCK: " + n + " updated removed!");
            } else {
                msg = "OK"; /* must be exactly "OK"! */
                MailSender.getInstance().queue(null, 1, "User Locked: " + forEmail, "User account locked: " + ip + " reason=" + reason + " - " + u);
            }
        } catch (SQLException se) {
            SurveyLog.logException(logger, se, "Locking account for user " + forEmail + " from " + ip);
            msg += " SQL Exception";
        } catch (Throwable t) {
            SurveyLog.logException(logger, t, "Locking account for user " + forEmail + " from " + ip);
            msg += " Exception: " + t.toString();
        } finally {
            DBUtils.close(updateInfoStmt, conn);
        }
        return msg;
    }


    @Override
    public String getPassword(WebContext ctx, int theirId) {
        ResultSet rs = null;
        Statement s = null;
        String result = null;
        Connection conn = null;
        if (ctx != null) {
            logger.info("UR: Attempt getPassword by " + ctx.session.user.email + ": of #" + theirId);
        }
        try {
            conn = DBUtils.getInstance().getAConnection();
            s = conn.createStatement();
            rs = s.executeQuery("SELECT password FROM " + CLDR_USERS + " WHERE id=" + theirId);
            if (!rs.next()) {
                if (ctx != null)
                    ctx.println("Couldn't find user.");
                return null;
            }
            result = rs.getString(1);
            if (rs.next()) {
                if (ctx != null) {
                    ctx.println("Matched duplicate user (?)");
                }
                return null;
            }
        } catch (SQLException se) {
            logger.severe("UR:  exception: " + DBUtils.unchainSqlException(se));
            if (ctx != null)
                ctx.println(" An error occured: " + DBUtils.unchainSqlException(se));
        } catch (Throwable t) {
            logger.severe("UR:  exception: " + t.toString());
            if (ctx != null)
                ctx.println(" An error occured: " + t.toString());
        } finally {
            DBUtils.close(s, conn);
        }
        return result;
    }


    @Override
    public User newUser(WebContext ctx, User u) {
        final boolean hushUserMessages = CLDRConfig.getInstance().getEnvironment() == Environment.UNITTEST;
        u.email = normalizeEmail(u.email);
        // prepare quotes
        u.email = u.email.replace('\'', '_').toLowerCase();
        u.org = u.org.replace('\'', '_');
        u.name = u.name.replace('\'', '_');
        u.locales = (u.locales == null) ? "" : u.locales.replace('\'', '_');
        u.locales = LocaleNormalizer.normalizeQuietly(u.locales);

        Connection conn = null;
        PreparedStatement insertStmt = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();
            insertStmt = conn.prepareStatement(SQL_insertStmt);
            insertStmt.setInt(1, u.userlevel);
            DBUtils.setStringUTF8(insertStmt, 2, u.name);
            insertStmt.setString(3, u.org);
            insertStmt.setString(4, u.email);
            insertStmt.setString(5, u.getPassword());
            insertStmt.setString(6, u.locales);
            if (!insertStmt.execute()) {
                if (!hushUserMessages) logger.info("Added.");
                conn.commit();
                if (ctx != null)
                    ctx.println("<p>Added user.<p>");
                User newu = get(u.getPassword(), u.email, FOR_ADDING); // throw away
                // old user
                updateIntLocs(newu.id, conn);
                resetOrgList(); // update with new org spelling.
                notify(newu);
                return newu;
            } else {
                if (ctx != null)
                    ctx.println("Couldn't add user.");
                conn.commit();
                return null;
            }
        } catch (SQLException se) {
            SurveyLog.logException(logger, se, "Adding User");
            logger.severe("UR: Adding " + u.toString() + ": exception: " + DBUtils.unchainSqlException(se));
        } catch (Throwable t) {
            SurveyLog.logException(logger, t, "Adding User");
            logger.severe("UR: Adding  " + u.toString() + ": exception: " + t.toString());
        } finally {
            userModified(); // new user
            DBUtils.close(insertStmt, conn);
        }

        return null;
    }

    @Override
    public synchronized Map<Integer, VoterInfo> getVoterToInfo() {
        synchronized(DBUserRegistry.class) {
            if (voterInfo == null) {
                Map<Integer, VoterInfo> map = new TreeMap<>();

                ResultSet rs = null;
                PreparedStatement ps = null;
                Connection conn = null;
                try {
                    conn = DBUtils.getInstance().getAConnection();
                    ps = list(null, conn);
                    rs = ps.executeQuery();
                    // id,userlevel,name,email,org,locales,intlocs,lastlogin
                    while (rs.next()) {
                        // We don't go through the cache, because not all users may
                        // be loaded.

                        User u = new UserRegistry.User(rs.getInt(1));
                        // from params:
                        u.userlevel = rs.getInt(2);
                        u.name = DBUtils.getStringUTF8(rs, 3);
                        u.email = rs.getString(4);
                        u.org = rs.getString(5);
                        u.locales = rs.getString(6);
                        if (LocaleNormalizer.isAllLocales(u.locales)) {
                            u.locales = LocaleNormalizer.ALL_LOCALES;
                        }
                        u.intlocs = rs.getString(7);
                        u.last_connect = rs.getTimestamp(8);

                        // now, map it to a UserInfo
                        VoterInfo v = u.createVoterInfo();

                        map.put(u.id, v);
                    }
                    voterInfo = map;
                } catch (SQLException se) {
                    logger.log(java.util.logging.Level.SEVERE,
                        "UserRegistry: SQL error trying to  update VoterInfo - " + DBUtils.unchainSqlException(se), se);
                } catch (Throwable t) {
                    logger.log(java.util.logging.Level.SEVERE,
                        "UserRegistry: some error trying to update VoterInfo - " + t.toString(), t);
                } finally {
                    // close out the RS
                    DBUtils.close(rs, conn);
                } // end try
            }
            return voterInfo;
        }
    }

    private Map<Integer, VoterInfo> voterInfo = null;


    /**
     * The list of organizations
     *
     * It is necessary to call resetOrgList to initialize this list
     */
    private String[] orgList = new String[0];

    /**
     * Get the list of organization names
     *
     * @return the String array
     */
    @Override
    public String[] getOrgList() {
        if (orgList.length == 0) {
            resetOrgList();
        }
        return orgList;
    }

    private void resetOrgList() {
        // get all orgs in use...
        Set<String> orgs = new TreeSet<>();
        Connection conn = null;
        Statement s = null;
        try {
            conn = DBUtils.getInstance().getAConnection();
            s = conn.createStatement();
            ResultSet rs = s.executeQuery("SELECT distinct org FROM " + CLDR_USERS + " order by org");
            while (rs.next()) {
                String org = rs.getString(1);
                orgs.add(org);
            }
        } catch (SQLException se) {
            System.err.println("UserRegistry: SQL error trying to get orgs resultset for: VI " + " - "
                + DBUtils.unchainSqlException(se)/* ,se */);
        } finally {
            // close out the RS
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    DBUtils.closeDBConnection(conn);
                }
            } catch (SQLException se) {
                System.err.println("UserRegistry: SQL error trying to close out: "
                    + DBUtils.unchainSqlException(se));
            }
        } // end try

        // get all possible VR orgs..
        Set<Organization> allvr = new HashSet<>();
        for (Organization org : Organization.values()) {
            allvr.add(org);
        }
        // Subtract out ones already in use
        for (String org : orgs) {
            allvr.remove(UserRegistry.computeVROrganization(org));
        }
        // Add back any ones not yet in use
        for (Organization org : allvr) {
            String orgName = org.name();
            orgName = UCharacter.toTitleCase(orgName, null);
            orgs.add(orgName);
        }

        orgList = orgs.toArray(orgList);
    }

    @Override
    protected Set<User> getAnonymousUsersFromDb() {
        Set<User> set = new HashSet<>();
        ResultSet rs = null;
        PreparedStatement ps = null;
        Connection conn = null;
        try {
            conn = DBUtils.getInstance().getAConnection();
            ps = list(null, conn);
            rs = ps.executeQuery();
            // id,userlevel,name,email,org,locales,intlocs,lastlogin
            while (rs.next()) {
                int userlevel = rs.getInt(2);
                if (userlevel == ANONYMOUS) {
                    User u = new User(rs.getInt(1));
                    u.userlevel = userlevel;
                    u.name = DBUtils.getStringUTF8(rs, 3);
                    u.email = rs.getString(4);
                    u.org = rs.getString(5);
                    u.locales = rs.getString(6);
                    if (LocaleNormalizer.isAllLocales(u.locales)) {
                        u.locales = LocaleNormalizer.ALL_LOCALES;
                    }
                    u.intlocs = rs.getString(7);
                    u.last_connect = rs.getTimestamp(8);
                    set.add(u);
                }
            }
        } catch (SQLException se) {
            logger.log(java.util.logging.Level.SEVERE,
                "UserRegistry: SQL error getting anonymous users - " + DBUtils.unchainSqlException(se), se);
        } catch (Throwable t) {
            logger.log(java.util.logging.Level.SEVERE,
                "UserRegistry: some error getting anonymous users - " + t.toString(), t);
        } finally {
            DBUtils.close(rs, ps, conn);
        }
        return set;
    }

    @Override
    protected void createAnonymousUsers(int existingCount, int desiredCount) {
        Connection conn = null;
        Statement s = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();
            s = conn.createStatement();
            for (int i = existingCount + 1; i <= desiredCount; i++) {
                /*
                 * Don't specify the user id; a new unique id will be assigned automatically.
                 * Names are like "anon#3"; emails are like "anon3@example.org".
                 */
                String sql = "INSERT INTO " + CLDR_USERS
                    + "(userlevel, name, email, org, password, locales) VALUES("
                    + ANONYMOUS + ","                // userlevel
                    + "'anon#" + i + "',"            // name
                    + "'anon" + i + "@example.org'," // email
                    + "'cldr',"                      // org
                    + "'',"                          // password
                    + "'" + LocaleNormalizer.ALL_LOCALES + "')";      // locales
                s.execute(sql);
            }
            conn.commit();
        } catch (SQLException se) {
            logger.log(java.util.logging.Level.SEVERE,
                "UserRegistry: SQL error creating anonymous users - " + DBUtils.unchainSqlException(se), se);
        } catch (Throwable t) {
            logger.log(java.util.logging.Level.SEVERE,
                "UserRegistry: some error creating anonymous users - " + t.toString(), t);
        } finally {
            DBUtils.close(s, conn);
        }
    }

}
