package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ReviewHide {
    /**
     * Notifications that the user has chosen to hide for this locale
     */
    private final HiddenNotifications hiddenNotifications;

    private final int userId;
    private final String localeId;

    public ReviewHide(int userId, String localeId) {
        this.userId = userId;
        this.localeId = localeId;
        this.hiddenNotifications = new HiddenNotifications();
    }

    // "VALUE" is a reserved word in mysql, so use "val" instead

    private static final String CREATE_TABLE_SQL = "CREATE TABLE " +
            DBUtils.Table.DASH_HIDE +
            " (id INT NOT NULL " + DBUtils.DB_SQL_IDENTITY +
            ", user_id INT NOT NULL" +
            ", locale VARCHAR(20) NOT NULL" +
            ", subtype VARCHAR(255) NOT NULL" +
            ", xpstrid VARCHAR(20) NOT NULL" +
            ", val " + DBUtils.DB_SQL_UNICODE + " NOT NULL)";

    private static final String CREATE_INDEX_SQL = "CREATE UNIQUE INDEX " +
            DBUtils.Table.DASH_HIDE + "_id ON " +
            DBUtils.Table.DASH_HIDE + " (id) ";

    private static final String GET_LIST_SQL = "SELECT * FROM " +
            DBUtils.Table.DASH_HIDE +
            " WHERE user_id=? AND locale=?";

    private static final String GET_ITEM_SQL = "SELECT * FROM " +
            DBUtils.Table.DASH_HIDE +
            " WHERE user_id=? AND locale=? AND subtype=? AND xpstrid=? AND val=?";

    private static final String INSERT_ITEM_SQL = "INSERT INTO " +
            DBUtils.Table.DASH_HIDE +
            " (user_id,locale,subtype,xpstrid,val) VALUES(?,?,?,?,?)";

    private static final String DELETE_ITEM_SQL = "DELETE FROM " +
            DBUtils.Table.DASH_HIDE +
            " WHERE user_id=? AND locale=? AND subtype=? AND xpstrid=? AND val=?";

    public static void createTable(Connection conn) throws SQLException {
        String sql = null;
        Statement s = null;
        if (!DBUtils.hasTable(DBUtils.Table.DASH_HIDE.toString())) {
            try {
                s = conn.createStatement();
                s.execute(sql = CREATE_TABLE_SQL);
                s.execute(sql = CREATE_INDEX_SQL);
                s.close();
                s = null;
                conn.commit();
                sql = null;
            } finally {
                DBUtils.close(s);
                if (sql != null) {
                    System.err.println("Last SQL: " + sql);
                }
            }
        }
    }

    /**
     * Get a map of all the notifications this user has chosen to hide in the Dashboard
     * in this locale
     *
     * @return the HiddenNotifications
     */
    public HiddenNotifications get() {
        if (this.hiddenNotifications.needsData()) {
            getData();
        }
        return this.hiddenNotifications;
    }

    private void getData() {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement s = null;
        try {
            conn = DBUtils.getInstance().getAConnection();
            s = conn.prepareStatement(GET_LIST_SQL);
            s.setInt(1, userId);
            s.setString(2, localeId);
            rs = s.executeQuery();
            while (rs.next()) {
                final String subtype = rs.getString("subtype");
                final String xpstrid = rs.getString("xpstrid");
                final String val = DBUtils.getStringUTF8(rs, "val");
                this.hiddenNotifications.put(subtype, xpstrid, val);
            }
        } catch (SQLException sqe) {
            SurveyLog.logException(sqe, "Getting hidden notifications for uid#" + userId + " in " + localeId, null);
            throw new InternalError("Error getting hidden notifications: " + sqe.getMessage());
        } finally {
            DBUtils.close(rs, s, conn);
        }
    }

    // insert or delete a line to hide/show
    public void toggleItem(String subtype, String xpstrid, String val) {
        try {
            Connection conn = null;
            ResultSet rs = null;
            PreparedStatement ps = null, updateQuery = null;
            try {
                conn = DBUtils.getInstance().getDBConnection();
                ps = conn.prepareStatement(GET_ITEM_SQL);
                setArgs(ps, userId, localeId, subtype, xpstrid, val);
                rs = ps.executeQuery();
                if (!rs.next()) {
                    //the item is currently shown, not in the table, we can hide it
                    updateQuery = conn.prepareStatement(INSERT_ITEM_SQL);
                } else {
                    updateQuery = conn.prepareStatement(DELETE_ITEM_SQL);
                }
                setArgs(updateQuery, userId, localeId, subtype, xpstrid, val);
                updateQuery.executeUpdate();
                conn.commit();
            } finally {
                DBUtils.close(updateQuery, rs, ps, conn);
            }
        } catch (SQLException sqe) {
            SurveyLog.logException(sqe, "Setting hidden notifications for uid#" + userId + " in " + localeId, null);
            throw new InternalError("Error setting hidden notifications: " + sqe.getMessage());
        }
    }

    private void setArgs(PreparedStatement ps, int userId, String localeId,
                         String subtype, String xpstrid, String val) throws SQLException {
        ps.setInt(1, userId);
        ps.setString(2, localeId);
        ps.setString(3, subtype);
        ps.setString(4, xpstrid);
        DBUtils.setStringUTF8(ps, 5, val);
    }
}
