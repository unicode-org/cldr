package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;

//save hidden line
public class ReviewHide {

    private HashMap<String, List<Integer>> hiddenField;

    public ReviewHide() {
        this.hiddenField = new HashMap<String, List<Integer>>();
    }

    //create the table (path, locale, type of notifications as key to get unique line)
    public static void createTable(Connection conn) throws SQLException {
        String sql = null;
        Statement s = null;
        if (!DBUtils.hasTable(DBUtils.Table.REVIEW_HIDE.toString())) {
            try {
                s = conn.createStatement();
                s.execute(sql = "CREATE TABLE " + DBUtils.Table.REVIEW_HIDE + " (id int not null " + DBUtils.DB_SQL_IDENTITY
                    + ", path int not null, choice varchar(20) not null, user_id int not null, locale varchar(20) not null)");
                s.execute(sql = "CREATE UNIQUE INDEX " + DBUtils.Table.REVIEW_HIDE + "_id ON " + DBUtils.Table.REVIEW_HIDE + " (id) ");

                try {
                    s.execute(sql = "ALTER TABLE " + DBUtils.Table.REVIEW_HIDE + " ADD CONSTRAINT review_hide_fk FOREIGN KEY (user_id) REFERENCES "
                        + UserRegistry.CLDR_USERS + "(id) ON DELETE CASCADE");
                } catch (SQLException se) {
                    // This seems to require InnoDB.
                    System.err.println("Warning: could not add Foreign Key constraint to " + DBUtils.Table.REVIEW_HIDE + " - skipping.  SQL was " + sql
                        + ", err was " + DBUtils.unchainSqlException(se));
                    SurveyLog
                        .logException(se, "Warning: could not add Foreign Key constraint to " + DBUtils.Table.REVIEW_HIDE + " - skipping.  SQL was " + sql);
                }
                sql = null;

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
    public HashMap<String, List<Integer>> getHiddenField(int userId, String locale) {
        if (this.hiddenField.isEmpty()) {
            Connection conn = null;
            ResultSet rs = null;
            PreparedStatement s = null;
            try {
                try {
                    conn = DBUtils.getInstance().getDBConnection();
                    s = conn.prepareStatement("SELECT * FROM " + DBUtils.Table.REVIEW_HIDE + " WHERE user_id=? AND locale=?");
                    s.setInt(1, userId);
                    s.setString(2, locale);
                    rs = s.executeQuery();
                    while (rs.next()) {
                        String choice = rs.getString("choice");
                        List<Integer> paths = this.hiddenField.get(choice);
                        if (paths == null)
                            paths = new ArrayList<Integer>();
                        paths.add(rs.getInt("path"));

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

    //get the hidden field as JSON
    public JSONObject getJSONReviewHide(int userId, String locale) {
        JSONObject notification = new JSONObject();
        for (Entry<String, List<Integer>> entry : this.getHiddenField(userId, locale).entrySet()) {
            try {
                notification.accumulate(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return notification;
    }

    //insert or delete a line to hide/show
    public void toggleItem(String choice, int path, int user, String locale) {
        try {
            Connection conn = null;
            ResultSet rs = null;
            PreparedStatement ps = null, updateQuery = null;
            try {
                conn = DBUtils.getInstance().getDBConnection();
                ps = conn.prepareStatement("SELECT * FROM " + DBUtils.Table.REVIEW_HIDE + " WHERE path=? AND user_id=? AND choice=? AND locale=?");

                ps.setInt(1, path);
                ps.setInt(2, user);
                ps.setString(3, choice);
                ps.setString(4, locale);
                rs = ps.executeQuery();

                if (!rs.next()) {
                    //the item is currently shown, not in the table, we can hide it
                    updateQuery = conn.prepareStatement("INSERT INTO " + DBUtils.Table.REVIEW_HIDE + " (path, user_id,choice,locale) VALUES(?,?,?,?)");
                } else {
                    updateQuery = conn.prepareStatement("DELETE FROM " + DBUtils.Table.REVIEW_HIDE + " WHERE path=? AND user_id=? AND choice=? AND locale=?");
                }

                updateQuery.setInt(1, path);
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
