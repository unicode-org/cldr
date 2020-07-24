package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.util.StandardCodes;

public class SurveyForumParticipation {
    private enum Cell {
        LOCALE("Locale"),
        FORUM_TOTAL("Posts in this release"),
        FORUM_ORG("Posts by my org."),
        FORUM_REQUEST("Open Requests"),
        FORUM_DISCUSS("Open Discussions"),
        ;

        private String title;

        private Cell(String title) {
            this.title = title;
        }
    }

    public class ForumStats { // stats for one locale
        Map<Cell, Integer> map = new HashMap<>();
    }

    private static final String tableId = "participationTable";
    private static final String fileName = "participation.csv";
    private static final String onclick = "cldrStCsvFromTable.downloadCsv("
        + "\"" + tableId + "\""
        + ", "
        + "\"" + fileName + "\""
        + ")";

    private static final String forumTable = DBUtils.Table.FORUM_POSTS.toString();
    private static final String userTable = UserRegistry.CLDR_USERS;

    private String org;
    private Set<String> orgLocales;
    private Map<String, ForumStats> locForumStats = new HashMap<>(); // for each locale
    private Connection conn = null;

    public SurveyForumParticipation(String org) {
        this.org = org;
        orgLocales = StandardCodes.make().getLocaleCoverageLocales(org);
    }

    public String getHtml() {
        String html = "<p>Organization: " + org + "</p>\n";
        html += "<p><a onclick='" + onclick + "'>Download CSV</a></p>\n";
        html += makeHtmlTable();
        return html;
    }

    private String makeHtmlTable() {
        String html = "";
        try {
            conn = DBUtils.getInstance().getDBConnection();
            html = "<table border='1' id='" + tableId + "'>\n";
            html += "<tr>\n";
            for (Cell cell : Cell.values()) {
                html += "<th>" + cell.title + "</th>\n";
            }
            html += "</tr>\n";
            for (String loc : orgLocales) {
                html += "<tr>\n";
                for (Cell cell : Cell.values()) {
                    String s = getCell(loc, cell);
                    html += "<td>" + s + "</td>\n";
                }
                html += "</tr>\n";
            }
            html += "</table>\n";
        } finally {
            DBUtils.close(conn);
        }
        return html;
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
            + " AND " + forumTable + ".open=true"
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
