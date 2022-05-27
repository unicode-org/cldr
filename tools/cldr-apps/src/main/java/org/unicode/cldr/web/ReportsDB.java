package org.unicode.cldr.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.VoterReportStatus;

public class ReportsDB implements VoterReportStatus<Integer>, ReportStatusUpdater<Integer> {
    public static ReportsDB getInstance() {
        return ReportsDBHelper.INSTANCE;
    }
    static final Logger logger = SurveyLog.forClass(ReportsDB.class);
    final String table = DBUtils.Table.VOTE_REPORTS
        .forVersion(SurveyMain.getNewVersion(), false).toString();

    final static class ReportsDBHelper {
        static ReportsDB INSTANCE = new ReportsDB();
    }

    private void setupDB() {
        logger.info("Setting up DB");
        try {
            try (Connection conn = DBUtils.getInstance().getDBConnection();) {
                if (!DBUtils.hasTable(conn, table)) {
                    DBUtils.execSqlWithSubs("cldr-reports-db.sql",
                        // Variable,   Substitution
                        "VOTE_REPORTS", table,
                        "last_mod TIMESTAMP", DBUtils.DB_SQL_LAST_MOD);
                }
            }
        } catch (SQLException | IOException se) {
            SurveyMain.busted("setting up ReportsDB", se);
        }
    }

    ReportsDB() {
        setupDB();
    }

    @Override
    public void markReportComplete(Integer user, CLDRLocale locale, ReportId r, boolean completed, boolean acceptable) {
        try (
            Connection conn = DBUtils.getInstance().getDBConnection();
            PreparedStatement ps = DBUtils.prepareStatementWithArgsUpdateable(conn,
                String.format("INSERT INTO %s (submitter, locale, report, completed, acceptable) "+
                "VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE completed=?,acceptable=?", table),
                user, locale.getBaseName(), r.name(), completed ? 1 : 0, acceptable ? 1 : 0,
                completed ? 1 : 0, acceptable ? 1 : 0);
        ) {
            ps.execute();
            conn.commit();
        } catch (SQLException e) {
            SurveyLog.logException(e, "updating reportStatus for " + user + ":" + locale);
        }
    }

    @Override
    public ReportStatus getReportStatus(Integer user, CLDRLocale locale) {
        ReportStatus status = new ReportStatus();
        try (
            Connection conn = DBUtils.getInstance().getAConnection();
            PreparedStatement ps = DBUtils.prepareStatementWithArgsFRO(conn,
                String.format("SELECT report, completed, acceptable FROM %s WHERE submitter=? AND locale=?", table),
                user, locale.getBaseName());
            ResultSet rs = ps.executeQuery();
        ) {
            while(rs.next()) {
                final String report = rs.getString("report");
                final Boolean completed = rs.getBoolean("completed");
                final Boolean acceptable = rs.getBoolean("acceptable");
                status.mark(ReportId.valueOf(report), completed, acceptable);
            }
        } catch (SQLException e) {
            SurveyLog.logException(e, "fetching reportStatus for " + user + ":" + locale);
        }
        return status;
    }
}
