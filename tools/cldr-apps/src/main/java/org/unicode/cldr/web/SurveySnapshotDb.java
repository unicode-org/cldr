package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class SurveySnapshotDb implements SurveySnapshot {

    private static final java.util.logging.Logger logger = SurveyLog.forClass(SurveySnapshotDb.class);

    @Override
    public void put(String snapshotId, String json) {
        if (!ensureTableExists()) {
            return;
        }
        String sql = "INSERT INTO " + DBUtils.Table.SUMMARY_SNAPSHOTS + " VALUES (?,?)";
        try (Connection conn = DBUtils.getInstance().getAConnection();
             PreparedStatement ps = DBUtils.prepareForwardReadOnly(conn, sql)) {
            ps.setString(1, snapshotId);
            DBUtils.setStringUTF8(ps, 2, json);
            ps.executeUpdate();
        } catch (SQLException se) {
            String complaint = "SurveySnapshotDb: Couldn't put snapshot - " + DBUtils.unchainSqlException(se);
            SurveyLog.logException(logger, se, complaint);
        }
    }

    @Override
    public String get(String snapshotId) {
        if (!ensureTableExists()) {
            return null;
        }
        String data = null;
        String sql = "SELECT data FROM " + DBUtils.Table.SUMMARY_SNAPSHOTS
                + " WHERE stamp=?";
        try (Connection conn = DBUtils.getInstance().getAConnection();
            PreparedStatement ps = DBUtils.prepareStatementWithArgsFRO(conn, sql, snapshotId)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                data = rs.getString(1);
            }
        } catch (SQLException se) {
            String complaint = "SurveySnapshotDb: Couldn't get snapshot - " + DBUtils.unchainSqlException(se);
            SurveyLog.logException(logger, se, complaint);
        }
        return data;
    }

    @Override
    public String[] list() {
        if (!ensureTableExists()) {
            return null;
        }
        ArrayList<String> snapshotIdList = new ArrayList<>();
        String sql = "SELECT stamp FROM " + DBUtils.Table.SUMMARY_SNAPSHOTS;
        try (Connection conn = DBUtils.getInstance().getAConnection();
             PreparedStatement ps = DBUtils.prepareStatement(conn, "snapList", sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                snapshotIdList.add(rs.getString(1));
            }
        } catch (SQLException se) {
            String complaint = "SurveySnapshotDb: Couldn't list snapshots - " + DBUtils.unchainSqlException(se);
            SurveyLog.logException(logger, se, complaint);
        }
        String[] keys = new String[snapshotIdList.size()];
        return snapshotIdList.toArray(keys);
    }

    private boolean ensureTableExists() {
        try (Connection conn = DBUtils.getInstance().getAConnection()) {
            String tableName = DBUtils.Table.SUMMARY_SNAPSHOTS.toString();
            if (DBUtils.hasTable(conn, tableName)) {
                return true;
            }
            Statement s = conn.createStatement();
            /*
             * Two columns: stamp = a timestamp like 2022-01-18T12:34:56.789Z;
             * data = json string matching what front end receives for Priority Items Summary
             */
            String sql = "CREATE TABLE " + tableName
                    + "(stamp VARCHAR(255), data LONGBLOB, PRIMARY KEY (stamp))"
                    + DBUtils.DB_SQL_BINCOLLATE;
            s.execute(sql);
            s.close();
        } catch (SQLException se) {
            String complaint = "SurveySnapshotDb: Couldn't create snapshot table - " + DBUtils.unchainSqlException(se);
            SurveyLog.logException(logger, se, complaint);
            return false;
        }
        return true;
    }
}
