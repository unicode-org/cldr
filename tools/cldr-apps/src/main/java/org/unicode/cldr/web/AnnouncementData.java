package org.unicode.cldr.web;

import com.ibm.icu.dev.util.ElapsedTimer;
import java.sql.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.unicode.cldr.util.*;
import org.unicode.cldr.web.api.Announcements;

public class AnnouncementData {
    private static final java.util.logging.Logger logger =
            SurveyLog.forClass(AnnouncementData.class);

    static boolean dbIsSetUp = false;

    public static void get(
            UserRegistry.User user, List<Announcements.Announcement> announcementList) {
        makeSureDbSetup();
        AnnouncementFilter aFilter = new AnnouncementFilter(user);
        final String sql = "SELECT * FROM " + DBUtils.Table.ANNOUNCE + " ORDER BY last_time DESC";
        try {
            Connection conn = null;
            try {
                conn = CookieSession.sm.dbUtils.getAConnection();
                Object[][] o = DBUtils.sqlQueryArrayArrayObj(conn, sql);
                for (Object[] objects : o) {
                    Announcements.Announcement a = makeAnnouncementFromDbObject(objects);
                    if (aFilter.passes(a)) {
                        a.setChecked(getChecked(a.id, user.id));
                        announcementList.add(a);
                    }
                }
            } finally {
                DBUtils.close(conn);
            }
        } catch (SQLException se) {
            String complaint =
                    "Error getting announcements from database - "
                            + DBUtils.unchainSqlException(se);
            logger.severe(complaint);
            throw new RuntimeException(complaint);
        }
    }

    private static Announcements.Announcement makeAnnouncementFromDbObject(Object[] objects) {
        int id = (Integer) objects[0];
        int poster = (Integer) objects[1];
        String subject = (String) objects[2];
        String body = (String) objects[3];
        Timestamp lastDate = (Timestamp) objects[4];
        String locs = (String) objects[5];
        String orgs = (String) objects[6];
        String audience = (String) objects[7];

        String date = lastDate.toString();
        // long date_long = lastDate.getTime();
        Announcements.Announcement a =
                new Announcements.Announcement(id, poster, date, subject, body);
        a.setFilters(locs, orgs, audience);
        return a;
    }

    public static void submit(
            Announcements.SubmissionRequest request,
            Announcements.AnnouncementSubmissionResponse response,
            UserRegistry.User user)
            throws SurveyException {
        makeSureDbSetup();
        try {
            int announcementId = savePostToDb(request, user);
            logger.fine("Saved announcement, id = " + announcementId);
            response.id = announcementId;
            response.ok = true;
        } catch (SurveyException e) {
            response.err = "An exception occured: " + e;
            logger.severe(e.getMessage());
        }
    }

    private static int savePostToDb(Announcements.SubmissionRequest request, UserRegistry.User user)
            throws SurveyException {
        int announcementId;
        try {
            Connection conn = null;
            PreparedStatement pAdd = null;
            try {
                conn = CookieSession.sm.dbUtils.getAConnection();
                pAdd = prepare_pAdd(conn);
                pAdd.setInt(1, user.id); // "poster"
                DBUtils.setStringUTF8(pAdd, 2, request.subject); // "subj"
                DBUtils.setStringUTF8(pAdd, 3, request.body); // "text"
                pAdd.setString(4, request.locs);
                pAdd.setString(5, request.orgs);
                pAdd.setString(6, request.audience);
                int n = pAdd.executeUpdate();
                if (n != 1) {
                    throw new RuntimeException("Couldn't post announcement, update failed.");
                }
                announcementId = DBUtils.getLastId(pAdd);
                emailNotify(request, user, announcementId);
            } finally {
                DBUtils.close(pAdd, conn);
            }
        } catch (SQLException se) {
            String complaint =
                    "Couldn't save announcement post to db - " + DBUtils.unchainSqlException(se);
            SurveyLog.logException(logger, se, complaint);
            throw new SurveyException(SurveyException.ErrorCode.E_INTERNAL, complaint);
        }
        return announcementId;
    }

    private static PreparedStatement prepare_pAdd(Connection conn) throws SQLException {
        String sql =
                "INSERT INTO "
                        + DBUtils.Table.ANNOUNCE
                        + " (poster,subj,text,locs,orgs,audience)"
                        + " values (?,?,?,?,?,?)";
        return DBUtils.prepareStatement(conn, "pAddAnnouncement", sql);
    }

    private static void emailNotify(
            Announcements.SubmissionRequest request, UserRegistry.User poster, int announcementId) {
        ElapsedTimer et = new ElapsedTimer("Sending email for announcement " + announcementId);
        Set<Integer> recipients = new HashSet<>();
        gatherRecipients(request, poster, recipients);
        logger.fine(
                et
                        + ": Announcement notify: u#"
                        + poster.id
                        + " announcement:"
                        + announcementId
                        + " queueing:"
                        + recipients.size());
        if (recipients.size() == 0) {
            return;
        }
        String subject = "CLDR Survey Tool announcement: " + request.subject;
        String body =
                "From: "
                        + poster.name
                        + "\n"
                        + "To: "
                        + request.audience
                        + "\n"
                        + "Organization(s): "
                        + request.orgs
                        + "\n"
                        + "Locale(s): "
                        + request.locs
                        + "\n"
                        + request.body
                        + "\n\n"
                        + "Do not reply to this message, instead please go to the Survey Tool.\n\n"
                        + "https://st.unicode.org";

        MailSender mailSender = MailSender.getInstance();
        for (int recipient : recipients) {
            mailSender.queue(poster.id, recipient, subject, body);
        }
    }

    private static synchronized void gatherRecipients(
            Announcements.SubmissionRequest request,
            UserRegistry.User poster,
            Set<Integer> recipients) {
        // If ORGS_MINE, filter by org in the query for efficiency; otherwise filter in
        // addRecipientIfPasses
        String orgName =
                Announcements.ORGS_MINE.equals(request.orgs)
                        ? poster.getOrganization().name()
                        : null;
        Connection conn = null;
        PreparedStatement ps = null;
        java.sql.ResultSet rs = null;
        try {
            conn = CookieSession.sm.dbUtils.getAConnection();
            ps = CookieSession.sm.reg.list(orgName, conn); // orgName = null to list all
            rs = ps.executeQuery();
            if (rs == null) {
                return;
            }
            while (rs.next()) {
                addRecipientIfPasses(rs, request, poster, recipients);
            }
        } catch (SQLException se) {
            logger.log(
                    java.util.logging.Level.WARNING,
                    "Query for orgs " + orgName + " failed: " + DBUtils.unchainSqlException(se),
                    se);
        } finally {
            DBUtils.close(conn, ps, rs);
        }
    }

    private static void addRecipientIfPasses(
            ResultSet rs,
            Announcements.SubmissionRequest request,
            UserRegistry.User poster,
            Set<Integer> recipients)
            throws SQLException {
        int id = rs.getInt(1);
        if (id == poster.id) {
            return; // don't email the poster
        }
        int level = rs.getInt(2);
        if (level == UserRegistry.ANONYMOUS || level >= UserRegistry.LOCKED) {
            return; // don't email anonymous or locked users
        }
        UserRegistry.User user = CookieSession.sm.reg.getInfo(id);
        AnnouncementFilter aFilter = new AnnouncementFilter(user);
        Announcements.Announcement a =
                new Announcements.Announcement(0, poster.id, null, null, null);
        a.setFilters(request.locs, request.orgs, request.audience);
        if (aFilter.passes(a)) {
            logger.fine("In AnnouncementData.addRecipientIfPasses, adding recipient: " + user.id);
            recipients.add(user.id);
        }
    }

    private static boolean getChecked(int announcementId, int userId) {
        String table = DBUtils.Table.ANNOUNCE_READ.toString();
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE announce_id=? AND user_id=?";

        Connection conn = null;
        PreparedStatement ps = null;
        int count;
        try {
            conn = CookieSession.sm.dbUtils.getAConnection();
            if (conn == null) {
                logger.severe("Connection failed in getChecked");
                return false;
            }
            ps = DBUtils.prepareForwardReadOnly(conn, sql);
            ps.setInt(1, announcementId);
            ps.setInt(2, userId);
            count = DBUtils.sqlCount(ps);
        } catch (SQLException e) {
            logger.severe("getChecked: " + e);
            return false;
        } finally {
            DBUtils.close(ps, conn);
        }
        return count >= 1;
    }

    /**
     * Update the ANNOUNCE_READ table to indicate whether the user has read a particular
     * announcement.
     *
     * <p>If id+user is present in the table, that means the user has read (checked) it -- absent
     * means the user has not read it
     *
     * @param announcementId the id for the announcement
     * @param checked true if the user has read the announcement
     * @param response the CheckReadResponse
     * @param user the current user (not necessarily the poster)
     */
    public static void checkRead(
            int announcementId,
            boolean checked,
            Announcements.CheckReadResponse response,
            UserRegistry.User user) {
        makeSureDbSetup();
        if (checked == getChecked(announcementId, user.id)) {
            return; // nothing needs to be done
        }
        response.ok =
                checked
                        ? addCheckRow(announcementId, user.id)
                        : deleteCheckRow(announcementId, user.id);
        if (!response.ok) {
            response.err = "Failure to update database with checkmark; log has details";
        }
    }

    private static boolean addCheckRow(int announcementId, int userId) {
        String table = DBUtils.Table.ANNOUNCE_READ.toString();
        String sql = "INSERT INTO " + table + " VALUES (?,?)";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = CookieSession.sm.dbUtils.getAConnection();
            if (conn == null) {
                logger.severe("Connection failed in addCheckRow");
                return false;
            }
            ps = DBUtils.prepareStatementWithArgs(conn, sql);
            ps.setInt(1, announcementId);
            ps.setInt(2, userId);
            int count = ps.executeUpdate();
            if (count < 1) {
                logger.severe("Update failed in addCheckRow");
                return false;
            }
        } catch (SQLException e) {
            logger.severe("addCheckRow: " + e);
            return false;
        } finally {
            DBUtils.close(ps, conn);
        }
        return true;
    }

    private static boolean deleteCheckRow(int announcementId, int userId) {
        String table = DBUtils.Table.ANNOUNCE_READ.toString();
        String sql = "DELETE FROM " + table + " WHERE announce_id=? AND user_id=?";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = CookieSession.sm.dbUtils.getAConnection();
            if (conn == null) {
                logger.severe("Connection failed in deleteCheckRow");
                return false;
            }
            ps = DBUtils.prepareStatementWithArgs(conn, sql);
            ps.setInt(1, announcementId);
            ps.setInt(2, userId);
            int count = ps.executeUpdate();
            if (count < 1) {
                logger.severe("Delete failed in deleteCheckRow");
                return false;
            }
        } catch (SQLException e) {
            logger.severe("deleteCheckRow: " + e);
            return false;
        } finally {
            DBUtils.close(ps, conn);
        }
        return true;
    }

    private static synchronized void makeSureDbSetup() {
        if (!dbIsSetUp) {
            try {
                setupDB();
                dbIsSetUp = true;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static synchronized void setupDB() throws SQLException {
        boolean haveAnnounceTable = DBUtils.hasTable(DBUtils.Table.ANNOUNCE.toString());
        boolean haveAnnounceReadTable = DBUtils.hasTable(DBUtils.Table.ANNOUNCE_READ.toString());
        if (haveAnnounceTable && haveAnnounceReadTable) {
            return;
        }
        try {
            Connection conn = null;
            try {
                conn = DBUtils.getInstance().getDBConnection();
                if (conn == null) {
                    logger.severe("Connection failed in setupDB");
                    return;
                }
                if (!haveAnnounceTable) {
                    createAnnounceTable(conn);
                }
                if (!haveAnnounceReadTable) {
                    createAnnounceReadTable(conn);
                }
            } finally {
                DBUtils.close(conn);
            }
        } catch (SQLException se) {
            se.printStackTrace();
            System.err.println("SQL err: " + DBUtils.unchainSqlException(se));
            throw se;
        }
    }

    private static void createAnnounceTable(Connection conn) throws SQLException {
        String sql =
                "CREATE TABLE "
                        + DBUtils.Table.ANNOUNCE
                        + " ( "
                        + " id INT NOT NULL "
                        + DBUtils.DB_SQL_IDENTITY
                        + ", "
                        + " poster INT NOT NULL, "
                        + " subj "
                        + DBUtils.DB_SQL_UNICODE
                        + " NOT NULL, "
                        + " text "
                        + DBUtils.DB_SQL_UNICODE
                        + " NOT NULL, "
                        + " last_time TIMESTAMP NOT NULL "
                        + DBUtils.DB_SQL_WITHDEFAULT
                        + " CURRENT_TIMESTAMP, "
                        + " locs VARCHAR(122), "
                        + " orgs VARCHAR(122), "
                        + " audience VARCHAR(122)"
                        + " )";
        try {
            Statement s = conn.createStatement();
            s.execute(sql);
            s.close();
            conn.commit();
        } catch (SQLException se) {
            System.err.println("Last SQL run: " + sql);
            throw se;
        }
    }

    private static void createAnnounceReadTable(Connection conn) throws SQLException {
        String sql =
                "CREATE TABLE "
                        + DBUtils.Table.ANNOUNCE_READ
                        + " ( "
                        + " announce_id INT NOT NULL "
                        + ", "
                        + " user_id INT NOT NULL "
                        + ", PRIMARY KEY (announce_id,user_id)"
                        + " )";
        try {
            Statement s = conn.createStatement();
            s.execute(sql);
            s.close();
            conn.commit();
        } catch (SQLException se) {
            System.err.println("Last SQL run: " + sql);
            throw se;
        }
    }

    private static class AnnouncementFilter {
        private final UserRegistry.User user;
        private final VoteResolver.Level userLevel;
        private final boolean userHasAllLocales;
        private LocaleSet intLoc = null, authLoc = null;

        public AnnouncementFilter(UserRegistry.User user) {
            this.user = user;
            this.userLevel = user.getLevel();
            userHasAllLocales = userLevel.isManagerOrStronger();
            if (!userHasAllLocales) {
                this.intLoc = user.getInterestLocales().combineRegionalVariants();
                this.authLoc = user.getAuthorizedLocaleSet().combineRegionalVariants();
            }
        }

        public boolean passes(Announcements.Announcement a) {
            if (a.poster == user.id) {
                // A user can always view their own post (for example, even a manager
                // who sends a post to AUDIENCE_TC). They won't get an email, though (see
                // addRecipientIfPasses).
                return true;
            }
            return matchOrg(a.orgs, a.poster) && inAudience(a.audience) && matchLocales(a.locs);
        }

        private boolean matchOrg(String orgs, int posterId) {
            if (Announcements.ORGS_ALL.equals(orgs)) {
                return true;
            } else if (Announcements.ORGS_TC.equals(orgs)) {
                return user.getOrganization().isTCOrg();
            } else if (Announcements.ORGS_MINE.equals(orgs)) {
                UserRegistry.User posterUser = CookieSession.sm.reg.getInfo(posterId);
                return posterUser != null && posterUser.isSameOrg(user);
            } else {
                logger.severe("Unrecognized orgs: " + orgs);
                return false;
            }
        }

        private boolean matchLocales(String locs) {
            if (userHasAllLocales || locs == null || locs.isEmpty()) {
                return true;
            }
            // assume locs (from the announcement) is already normalized and combined by language
            LocaleSet set = LocaleNormalizer.setFromStringQuietly(locs, null);
            return set.intersectionNonEmpty(intLoc) || set.intersectionNonEmpty(authLoc);
        }

        private boolean inAudience(String audience) {
            if (Announcements.AUDIENCE_EVERYONE.equals(audience)) {
                return true;
            } else if (Announcements.AUDIENCE_VETTERS.equals(audience)) {
                return userLevel.isVetter();
            } else if (Announcements.AUDIENCE_MANAGERS.equals(audience)) {
                return userLevel.isManagerOrStronger();
            } else if (Announcements.AUDIENCE_TC.equals(audience)) {
                return userLevel.isTC();
            } else {
                logger.severe("Unrecognized audience: " + audience);
                return false;
            }
        }
    }
}
