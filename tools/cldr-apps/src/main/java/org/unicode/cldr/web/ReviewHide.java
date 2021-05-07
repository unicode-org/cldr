package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//save hidden line
public class ReviewHide {

    private HashMap<String, List<String>> hiddenField;

    public ReviewHide() {
        this.hiddenField = new HashMap<>();
    }

    //create the table (path, locale, type of notifications as key to get unique line)
    public static void createTable(Connection conn) throws SQLException {
        String sql = null;
        Statement s = null;
        if (!DBUtils.hasTable(DBUtils.Table.DASH_HIDE.toString())) {
            try {
                s = conn.createStatement();
                s.execute(sql = "CREATE TABLE " + DBUtils.Table.DASH_HIDE + " (id int not null " + DBUtils.DB_SQL_IDENTITY
                    + ", path varchar(20) not null, choice varchar(20) not null, user_id int not null, locale varchar(20) not null)");
                s.execute(sql = "CREATE UNIQUE INDEX " + DBUtils.Table.DASH_HIDE + "_id ON " + DBUtils.Table.DASH_HIDE + " (id) ");
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

    //get all the field for an user and locale
    public HashMap<String, List<String>> getHiddenField(int userId, String locale) {
        if (this.hiddenField.isEmpty()) {
            Connection conn = null;
            ResultSet rs = null;
            PreparedStatement s = null;
            try {
                try {
                    conn = DBUtils.getInstance().getAConnection();
                    s = conn.prepareStatement("SELECT * FROM " + DBUtils.Table.DASH_HIDE + " WHERE user_id=? AND locale=?");
                    s.setInt(1, userId);
                    s.setString(2, locale);
                    rs = s.executeQuery();
                    while (rs.next()) {
                        String choice = rs.getString("choice");
                        List<String> paths = this.hiddenField.get(choice);
                        if (paths == null)
                            paths = new ArrayList<>();
                        paths.add(rs.getString("path"));

                        this.hiddenField.put(choice, paths);
                    }
                } finally {
                    DBUtils.close(rs, s, conn);
                }
            } catch (SQLException sqe) {
                SurveyLog.logException(sqe, "Getting hidden fields for uid#" + userId + " in " + locale, null);
                throw new InternalError("Error getting hidden fields: " + sqe.getMessage());
            }
        }

        return this.hiddenField;
    }

    //insert or delete a line to hide/show
    public void toggleItem(String choice, String xpathHexId, int user, String locale) {
        try {
            Connection conn = null;
            ResultSet rs = null;
            PreparedStatement ps = null, updateQuery = null;
            try {
                conn = DBUtils.getInstance().getDBConnection();
                ps = conn.prepareStatement("SELECT * FROM " + DBUtils.Table.DASH_HIDE + " WHERE path=? AND user_id=? AND choice=? AND locale=?");

                ps.setString(1, xpathHexId);
                ps.setInt(2, user);
                ps.setString(3, choice);
                ps.setString(4, locale);
                rs = ps.executeQuery();

                if (!rs.next()) {
                    //the item is currently shown, not in the table, we can hide it
                    updateQuery = conn.prepareStatement("INSERT INTO " + DBUtils.Table.DASH_HIDE + " (path, user_id,choice,locale) VALUES(?,?,?,?)");
                } else {
                    updateQuery = conn.prepareStatement("DELETE FROM " + DBUtils.Table.DASH_HIDE + " WHERE path=? AND user_id=? AND choice=? AND locale=?");
                }

                updateQuery.setString(1, xpathHexId);
                updateQuery.setInt(2, user);
                updateQuery.setString(3, choice);
                updateQuery.setString(4, locale);
                updateQuery.executeUpdate();
                conn.commit();
            } finally {
                DBUtils.close(updateQuery, rs, ps, conn);
            }
        } catch (SQLException sqe) {
            SurveyLog.logException(sqe, "Setting hidden fields for uid#" + user + " in " + locale, null);
            throw new InternalError("Error setting hidden fields: " + sqe.getMessage());
        }
    }
}
