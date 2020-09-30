package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.web.SurveyAjax.JSONWriter;

public class SurveyForumParticipation {
    private enum Cell {
        LOCALE("Locale"),
        FORUM_TOTAL("Posts in this release"),
        FORUM_ORG("Posts by my org."),
        FORUM_REQUEST("Open Requests"),
        FORUM_DISCUSS("Open Discussions"),
        FORUM_ACT("Needing action"),
        ;

        private String title;

        private Cell(String title) {
            this.title = title;
        }
    }

    public class ForumStats { // stats for one locale
        Map<Cell, Integer> map = new HashMap<>();
    }

    private static final String forumTable = DBUtils.Table.FORUM_POSTS.toString();
    private static final String userTable = UserRegistry.CLDR_USERS;

    private static final int typeDiscuss = SurveyForum.PostType.DISCUSS.toInt();

    private String org;
    private Set<String> orgLocales;
    private Map<String, ForumStats> locForumStats = new HashMap<>(); // for each locale
    private Connection conn = null;

    public SurveyForumParticipation(String org) {
        this.org = org;
        orgLocales = StandardCodes.make().getLocaleCoverageLocales(org);
    }

    public void getJson(JSONWriter r) throws JSONException {
        r.put("org", org);

        JSONArray headers = new JSONArray();
        for (Cell cell : Cell.values()) {
            headers.put(cell.title);
        }
        r.put("headers", headers);

        JSONArray rows = new JSONArray();
        for (String loc : orgLocales) {
            if ("*".equals(loc)) {
                continue;
            }
            JSONArray cells = new JSONArray();
            for (Cell cell : Cell.values()) {
                String s = getCell(loc, cell);
                cells.put(s);
            }
            rows.put(cells);
        }
        r.put("rows", rows);
    }

    private String getCell(String loc, Cell cell) {
        if (cell == Cell.LOCALE) {
            return loc;
        } else {
            return getForumCell(loc, cell);
        }
    }

    private String getForumCell(String loc, Cell cell) {
        ForumStats stats = locForumStats.get(loc);
        if (stats == null) {
            stats = getForumStats(loc);
            locForumStats.put(loc, stats);
        }
        Integer count = stats.map.get(cell);
        return (count == null) ? "?" : count.toString();
    }

    private ForumStats getForumStats(String loc) {
        ForumStats stats = new ForumStats();
        try {
            conn = DBUtils.getInstance().getDBConnection();
            for (Cell cell: Cell.values()) {
                int count = queryForumDb(cell, loc);
                stats.map.put(cell, count);
            }
        } catch (SQLException e) {
            SurveyLog.logException(e, "getStats " + loc + ", " + org);
        } finally {
            DBUtils.close(conn);
        }
        return stats;
    }

    private int queryForumDb(Cell cell, String loc) throws SQLException {
        if (cell == Cell.FORUM_ACT) {
            return queryForumDbAct(loc);
        }
        PreparedStatement ps = null;
        Integer count = -1;
        try {
            if (cell == Cell.FORUM_TOTAL) {
                ps = prepareForumAllQuery(loc);
            } else if (cell == Cell.FORUM_ORG) {
                ps = prepareForumOrgQuery(loc);
            } else if (cell == Cell.FORUM_REQUEST) {
                ps = prepareRequestOrDiscussQuery(loc, SurveyForum.PostType.REQUEST.toInt());
            } else if (cell == Cell.FORUM_DISCUSS) {
                ps = prepareRequestOrDiscussQuery(loc, SurveyForum.PostType.DISCUSS.toInt());
            } else {
                return -1;
            }
            count = DBUtils.sqlCount(null, conn, ps);
        } finally {
            DBUtils.close(ps);
        }
        return count;
    }

    /**
     * "Forum: Needing action (Requests and Discussions my organization has not responded to)"
     *
     * "Needs action" means:
     *    - (Open request from other org AND (my org haven’t agreed or declined))
     *    - OR (open discuss-only thread AND the last poster is not in my org)
     *
     * Compare JavaScript "passIfNeedingAction". Different on server since not "I" but "We" = "anybody in my org".
     *
     * @param conn
     * @param loc
     * @return the count
     * @throws SQLException
     *
     * TODO: make this faster; it's slow
     */
    private int queryForumDbAct(String loc) throws SQLException {
        PreparedStatement ps = null;
        Integer count = -1;
        try {
            ps = prepareForumActQuery(loc);
            ResultSet rs = ps.executeQuery();
            count = 0;
            while (rs.next()) {
                int forumId = rs.getInt(1);
                int postType =rs.getInt(2);
                /*
                 * For Discuss: look at the LAST reply in the thread, and skip the item if our org was its poster
                 * For Request: look at each reply in the thread, and skip the item if our org agreed or declined
                 */
                if (postType == typeDiscuss) {
                    if (!ourOrgPostedLast(forumId)) {
                        ++count;
                    }
                } else { // Request
                    if (!ourOrgAgreedOrDeclined(forumId)) {
                        ++count;
                    }
                }
            }
        } finally {
            DBUtils.close(ps);
        }
        return count;
    }

    private PreparedStatement prepareForumActQuery(String loc) throws SQLException {
        String sql = "SELECT " + forumTable + ".id, " +  forumTable + ".type"
            + " FROM " + forumTable
            + " JOIN " + userTable
            + " ON " + forumTable + ".poster=" + userTable + ".id"
            + " WHERE " + forumTable + ".parent=-1"
            + " AND " + forumTable + ".is_open=true"
            + " AND " + forumTable + ".loc=?"
            + " AND (" + forumTable + ".type=?"
            + " OR NOT " + userTable + ".org=?)";

        PreparedStatement ps = DBUtils.prepareForwardReadOnly(conn, sql);
        ps.setString(1, loc);
        ps.setInt(2, typeDiscuss);
        ps.setString(3, org);
        return ps;
    }

    /**
     * Does the poster who made the last post in this thread belong to this org?
     *
     * @param conn the Connection
     * @param forumRootId the id of the root (initial) forum post of the thread
     * @return true or false
     * @throws SQLException
     */
    private boolean ourOrgPostedLast(int forumRootId) throws SQLException {
        boolean result = false;
        PreparedStatement ps = null;
        String sql = "SELECT " + userTable + ".org "
            + " FROM " + forumTable
            + " JOIN " + userTable
            + " ON " + forumTable + ".poster=" + userTable + ".id"
            + " WHERE " + forumTable + ".root=?"
            + " ORDER BY " + forumTable + ".last_time DESC LIMIT 1";
        try {
            ps = DBUtils.prepareForwardReadOnly(conn, sql);
            ps.setInt(1, forumRootId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String lastPostOrg = rs.getString(1);
                result = (lastPostOrg == this.org);
            }
        } finally {
            DBUtils.close(ps);
        }
        return result;
    }

    /**
     * Has any member of our org made an AGREE or DECLINE post to this thread?
     *
     * @param forumRootId the id of the root (initial) forum post of the thread
     * @return true or false
     * @throws SQLException
     */
    private boolean ourOrgAgreedOrDeclined(int forumRootId) throws SQLException {
        boolean result = false;
        PreparedStatement ps = null;
        String sql = "SELECT COUNT(*)"
            + " FROM " + forumTable
            + " JOIN " + userTable
            + " ON " + forumTable + ".poster=" + userTable + ".id"
            + " WHERE " + forumTable + ".root=?"
            + " AND " + userTable + ".org=?"
            + " AND (" + forumTable + ".type=?"
            + " OR " + forumTable + ".type=?)";
        try {
            ps = DBUtils.prepareForwardReadOnly(conn, sql);
            ps.setInt(1, forumRootId);
            ps.setString(2, this.org);
            ps.setInt(3, SurveyForum.PostType.AGREE.toInt());
            ps.setInt(4, SurveyForum.PostType.DECLINE.toInt());
            int count = DBUtils.sqlCount(null, conn, ps);
            result = (count > 0);
        } finally {
            DBUtils.close(ps);
        }
        return result;
    }

    private PreparedStatement prepareForumAllQuery(String loc) throws SQLException {
        String sql = "SELECT COUNT(*)"
            + " FROM " + forumTable
            + " JOIN " + userTable
            + " ON " + forumTable + ".poster=" + userTable + ".id"
            + " WHERE " + forumTable + ".parent=-1"
            + " AND " + forumTable + ".loc=?"
            + " AND " + forumTable + ".version=?";

        PreparedStatement ps = DBUtils.prepareForwardReadOnly(conn, sql);
        ps.setString(1, loc);
        ps.setString(2, SurveyMain.getNewVersion());
        return ps;
    }

    private PreparedStatement prepareForumOrgQuery(String loc) throws SQLException {
        String sql = "SELECT COUNT(*)"
            + " FROM " + forumTable
            + " JOIN " + userTable
            + " ON " + forumTable + ".poster=" + userTable + ".id"
            + " WHERE " + forumTable + ".parent=-1"
            + " AND " + forumTable + ".loc=?"
            + " AND " + userTable + ".org=?"
            + " AND " + forumTable + ".version=?";

        PreparedStatement ps = DBUtils.prepareForwardReadOnly(conn, sql);
        ps = DBUtils.prepareForwardReadOnly(conn, sql);
        ps.setString(1, loc);
        ps.setString(2, org);
        ps.setString(3, SurveyMain.getNewVersion());
        return ps;
    }

    private PreparedStatement prepareRequestOrDiscussQuery(String loc, int type) throws SQLException {
        String sql = "SELECT COUNT(*)"
            + " FROM " + forumTable
            + " JOIN " + userTable
            + " ON " + forumTable + ".poster=" + userTable + ".id"
            + " WHERE " + forumTable + ".parent=-1"
            + " AND " + forumTable + ".is_open=true"
            + " AND " + forumTable + ".loc=?"
            + " AND " + userTable + ".org=?"
            + " AND " + forumTable + ".type=?";

        PreparedStatement ps = DBUtils.prepareForwardReadOnly(conn, sql);
        ps = DBUtils.prepareForwardReadOnly(conn, sql);
        ps.setString(1, loc);
        ps.setString(2, org);
        ps.setInt(3, type);
        return ps;
    }
}
