package org.unicode.cldr.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.MemVoterReportStatus;
import org.unicode.cldr.util.ReportStatusUpdater;
import org.unicode.cldr.util.VoterReportStatus;

public class ReportsDB extends VoterReportStatus<Integer> implements ReportStatusUpdater<Integer> {
    public static ReportsDB getInstance() {
        return ReportsDBHelper.INSTANCE;
    }

    static final Logger logger = SurveyLog.forClass(ReportsDB.class);
    final String table =
            DBUtils.Table.VOTE_REPORTS.forVersion(SurveyMain.getNewVersion(), false).toString();

    static final class ReportsDBHelper {
        static ReportsDB INSTANCE = new ReportsDB();
    }

    private void setupDB() {
        logger.info("Setting up DB");
        try {
            try (Connection conn = DBUtils.getInstance().getDBConnection(); ) {
                if (!DBUtils.hasTable(table)) {
                    DBUtils.execSqlWithSubs(
                            "cldr-reports-db.sql",
                            // Variable,   Substitution
                            "VOTE_REPORTS",
                            table,
                            "last_mod TIMESTAMP",
                            DBUtils.DB_SQL_LAST_MOD);
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
    public void markReportComplete(
            Integer user, CLDRLocale locale, ReportId r, boolean completed, boolean acceptable) {
        try (Connection conn = DBUtils.getInstance().getDBConnection();
                PreparedStatement ps =
                        DBUtils.prepareStatementWithArgsUpdateable(
                                conn,
                                String.format(
                                        "INSERT INTO %s (submitter, locale, report, completed, acceptable, last_mod) "
                                                + "VALUES(?,?,?,?,?, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE completed=?,acceptable=?,last_mod=CURRENT_TIMESTAMP",
                                        table),
                                user,
                                locale.getBaseName(),
                                r.name(),
                                completed ? 1 : 0,
                                acceptable ? 1 : 0,
                                completed ? 1 : 0,
                                acceptable ? 1 : 0); ) {
            ps.execute();
            conn.commit();
        } catch (SQLException e) {
            SurveyLog.logException(e, "updating reportStatus for " + user + ":" + locale);
        }
    }

    @Override
    public ReportStatus getReportStatus(Integer user, CLDRLocale locale) {
        ReportStatus status = new ReportStatus();
        try (Connection conn = DBUtils.getInstance().getAConnection();
                PreparedStatement ps =
                        DBUtils.prepareStatementWithArgsFRO(
                                conn,
                                String.format(
                                        "SELECT report, completed, acceptable FROM %s WHERE submitter=? AND locale=? order by last_mod asc",
                                        table),
                                user,
                                locale.getBaseName());
                ResultSet rs = ps.executeQuery(); ) {
            while (rs.next()) {
                final String reportStr = rs.getString("report");
                ReportId report = null;
                try {
                    report = ReportId.valueOf(reportStr);
                } catch (IllegalArgumentException iae) {
                    continue; // skip illegal enum values. may be a 'retired' enum
                }
                final Boolean completed = rs.getBoolean("completed");
                final Boolean acceptable = rs.getBoolean("acceptable");
                status.mark(report, completed, acceptable);
            }
        } catch (SQLException e) {
            SurveyLog.logException(e, "fetching reportStatus for " + user + ":" + locale);
        }
        return status;
    }

    /**
     * @param onlyId optional user id to restrict the report to
     * @param onlyLoc optional locale to restrict the report to
     */
    public UserReport[] getAllReports(Integer onlyId, CLDRLocale onlyLoc) throws SQLException {
        Map<Integer, UserReport> l = new HashMap<>();

        try (Connection conn = DBUtils.getInstance().getAConnection();
                PreparedStatement ps = getAllReportsStatement(conn, onlyId, onlyLoc);
                ResultSet rs = ps.executeQuery(); ) {
            while (rs.next()) {
                final int user = rs.getInt("submitter");
                final String report = rs.getString("report");
                ReportId reportId;
                try {
                    reportId = ReportId.valueOf(report);
                } catch (IllegalArgumentException iae) {
                    continue; // skip illegal enum values. may be a 'retired' enum
                }
                final String locale = rs.getString("locale");
                final Boolean completed = rs.getBoolean("completed");
                final Boolean acceptable = rs.getBoolean("acceptable");
                final java.sql.Timestamp last_mod = rs.getTimestamp("last_mod");

                // now update it
                UserReport userReport = l.computeIfAbsent(user, (i) -> new UserReport(i));
                userReport.update(
                        locale, reportId, completed, acceptable, new Date(last_mod.getTime()));
            }
        }
        return l.values().toArray(new UserReport[l.size()]);
    }

    /**
     * Take a snapshot of the reports DB. This reduces the number of SQL calls to be made against
     * that snapshot.
     *
     * @param onlyId Restrict to one id
     * @param onlyLoc Restrict to one locale
     * @return
     * @throws SQLException
     */
    public VoterReportStatus<Integer> clone(Integer onlyId, CLDRLocale onlyLoc)
            throws SQLException {
        MemVoterReportStatus<Integer> copy = new MemVoterReportStatus<Integer>();
        try (Connection conn = DBUtils.getInstance().getAConnection();
                PreparedStatement ps = getAllReportsStatement(conn, onlyId, onlyLoc);
                ResultSet rs = ps.executeQuery(); ) {
            while (rs.next()) {
                final int user = rs.getInt("submitter");
                final String report = rs.getString("report");
                ReportId reportId;
                try {
                    reportId = ReportId.valueOf(report);
                } catch (IllegalArgumentException iae) {
                    continue; // skip illegal enum values. may be a 'retired' enum
                }
                final String locale = rs.getString("locale");
                final Boolean completed = rs.getBoolean("completed");
                final Boolean acceptable = rs.getBoolean("acceptable");
                final java.sql.Timestamp last_mod = rs.getTimestamp("last_mod");

                // now update it
                copy.markReportComplete(
                        user,
                        CLDRLocale.getInstance(locale),
                        reportId,
                        completed,
                        acceptable,
                        new Date(last_mod.getTime()));
            }
        }
        return copy;
    }

    private PreparedStatement getAllReportsStatement(
            Connection conn, Integer onlyId, CLDRLocale onlyLoc) throws SQLException {
        if (onlyId != null && onlyLoc != null) {
            return DBUtils.prepareStatementWithArgsFRO(
                    conn,
                    String.format(
                            "SELECT * FROM %s WHERE submitter=? and locale=? order by last_mod a",
                            table),
                    onlyId,
                    onlyLoc);
        } else if (onlyId != null) {
            return DBUtils.prepareStatementWithArgsFRO(
                    conn,
                    String.format(
                            "SELECT * FROM %s WHERE submitter=? order by last_mod asc", table),
                    onlyId);
        } else if (onlyLoc != null) {
            return DBUtils.prepareStatementWithArgsFRO(
                    conn,
                    String.format("SELECT * FROM %s WHERE locale=? order by last_mod asc", table),
                    onlyLoc);
        } else {
            return DBUtils.prepareStatementWithArgsFRO(
                    conn, String.format("SELECT * FROM %s order by last_mod asc", table));
        }
    }

    public static final class UserReport {
        public UserReport(int id) {
            this.id = id;
        }

        private Date lastMod = null;

        @Schema(description = "the latest date that something was changed, or null")
        public String getLastMod() {
            return DBUtils.toISOString(lastMod);
        }

        @Schema(description = "User ID")
        public int id;

        @Schema(description = "User’s status for all reports")
        public Map<String, VoterReportStatus.ReportStatus> statuses = new TreeMap<>();

        public void update(
                String locale, ReportId valueOf, Boolean completed, Boolean acceptable, Date date) {
            VoterReportStatus.ReportStatus status =
                    statuses.computeIfAbsent(locale, l -> new VoterReportStatus.ReportStatus());
            status.mark(valueOf, completed, acceptable);
            if (lastMod == null || date.after(lastMod)) {
                lastMod = date;
            }
        }
    }
}
