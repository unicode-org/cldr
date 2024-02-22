package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.unicode.cldr.util.VoteResolver;

/**
 * "Permanent" Voting. There are situations in which we’d like TC to have the ability to vote for an
 * item in such a way that its value will be “locked” forever (this release and subsequent), until
 * and unless countermanded by another TC vote. Only certain voters (TC) are allowed to make
 * "Permanent" votes. If two voters make permanent votes for the same locale, path, and value, and
 * there is a forum entry by one of those voters, the locale+path becomes "locked". If two voters
 * make permanent votes to Abstain for the same locale and path, the locale+path becomes "unlocked".
 *
 * <p>Reference:
 * https://docs.google.com/document/d/1VsJ2y7dp2kq_Iu-zLTjOvCooX4kRfVPui6WO51aFGzE/edit?skip_itp2_check=true#heading=h.trc1g4nsvdb8
 */
public class PermanentVote {
    private static final Logger logger = SurveyLog.forClass(PermanentVote.class);
    private String localeName;
    private int xpathId;
    private String value;
    private boolean didLock, didUnlock, didClean;

    /**
     * A voter has just made a "Permanent" vote for an item, or to abstain. Construct a
     * PermanentVote object and use it to process the vote for purposes of locking/unlocking.
     *
     * <p>The caller will already have partially processed this vote in ways similar to ordinary
     * non-permanent votes. In particular, we assume that the caller will have added the vote to the
     * VOTE_VALUE table.
     *
     * <p>We take care of updating the database here, both VOTE_VALUE and LOCKED_XPATHS tables.
     *
     * <p>The caller, however, is responsible for updating the PerXPathData depending on what we
     * return for didLock(), didUnlock(), and didCleanSlate().
     *
     * @param localeName the locale name
     * @param xpathId the path id
     * @param value the value voted for, or null for Abstain
     */
    PermanentVote(String localeName, int xpathId, String value) {
        this.localeName = localeName;
        this.xpathId = xpathId;
        this.value = value;
        didLock = didUnlock = didClean = false;
        if (value == null) {
            if (isLockedAnyValue() && gotTwo()) {
                unlock();
                cleanSlate();
                didUnlock = didClean = true;
                logger.warning("PermanentVote: unlocked " + localeName + ", " + xpathId);
            }
        } else {
            if (!isLockedThisValue() && gotTwo()) {
                if (isLockedAnyValue()) {
                    unlock(); // do not make didUnlock true!
                }
                lock();
                cleanSlate();
                didLock = didClean = true;
                logger.warning(
                        "PermanentVote: locked " + localeName + ", " + xpathId + ", " + value);
            }
        }
    }

    public boolean didLock() {
        return this.didLock;
    }

    public boolean didUnlock() {
        return this.didUnlock;
    }

    public boolean didCleanSlate() {
        return this.didClean;
    }

    /**
     * Does a lock exist for this locale+path and any value?
     *
     * @return true or false
     */
    private boolean isLockedAnyValue() {
        String tableName = DBUtils.Table.LOCKED_XPATHS.toString();
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE locale=?" + " AND xpath=?";

        Connection conn = null;
        PreparedStatement ps = null;
        int count = 0;
        try {
            conn = DBUtils.getInstance().getAConnection();
            ps = DBUtils.prepareForwardReadOnly(conn, sql);
            ps.setString(1, localeName);
            ps.setInt(2, xpathId);
            count = DBUtils.sqlCount(ps);
        } catch (SQLException e) {
            SurveyLog.logException(e);
        } finally {
            DBUtils.close(ps, conn);
        }
        return count >= 1;
    }

    /**
     * Does a lock exist for this locale+path+value?
     *
     * @return true or false
     */
    private boolean isLockedThisValue() {
        if (value == null) {
            return false;
        }
        String tableName = DBUtils.Table.LOCKED_XPATHS.toString();
        String sql =
                "SELECT COUNT(*) FROM "
                        + tableName
                        + " WHERE locale=?"
                        + " AND xpath=?"
                        + " AND value=?";

        Connection conn = null;
        PreparedStatement ps = null;
        int count = 0;
        try {
            conn = DBUtils.getInstance().getAConnection();
            ps = DBUtils.prepareForwardReadOnly(conn, sql);
            ps.setString(1, localeName);
            ps.setInt(2, xpathId);
            DBUtils.setStringUTF8(ps, 3, value);
            count = DBUtils.sqlCount(ps);
        } catch (SQLException e) {
            SurveyLog.logException(e);
        } finally {
            DBUtils.close(ps, conn);
        }
        return count >= 1;
    }

    /**
     * Do at least two permanent votes exist for this locale+path+value in the VOTE_VALUE table?
     *
     * <p>These are Abstain votes if value is null.
     *
     * @return true or false
     */
    private boolean gotTwo() {
        /*
         * Example:
         * SELECT COUNT(*) FROM cldr_vote_value_37 WHERE vote_override = 1000 AND locale = 'fr' AND xpath = 683828 AND value = 'signe de la main'
         */
        String tableName = DBUtils.Table.VOTE_VALUE.toString();
        String sql =
                "SELECT COUNT(*) FROM "
                        + tableName
                        + " WHERE vote_override="
                        + VoteResolver.Level.PERMANENT_VOTES
                        + " AND locale=?"
                        + " AND xpath=?"
                        + " AND value"
                        + ((value == null) ? " IS NULL" : "=?");

        Connection conn = null;
        PreparedStatement ps = null;
        int count = 0;
        try {
            conn = DBUtils.getInstance().getAConnection();
            ps = DBUtils.prepareForwardReadOnly(conn, sql);
            ps.setString(1, localeName);
            ps.setInt(2, xpathId);
            if (value != null) {
                DBUtils.setStringUTF8(ps, 3, value);
            }
            count = DBUtils.sqlCount(ps);
        } catch (SQLException e) {
            SurveyLog.logException(e);
        } finally {
            DBUtils.close(ps, conn);
        }
        return count >= 2;
    }

    /** Add a "lock" to the LOCKED_XPATHS table for this locale+path+value */
    private void lock() {
        String tableName = DBUtils.Table.LOCKED_XPATHS.toString();
        String sql =
                "INSERT INTO "
                        + tableName
                        + "(locale,xpath,value,last_mod) VALUES(?,?,?,CURRENT_TIMESTAMP)";
        try (Connection conn = DBUtils.getInstance().getDBConnection();
                PreparedStatement ps = DBUtils.prepareForwardReadOnly(conn, sql); ) {
            ps.setString(1, localeName);
            ps.setInt(2, xpathId);
            DBUtils.setStringUTF8(ps, 3, value);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            SurveyLog.logException(e);
        }
    }

    /** Remove a "lock" from the LOCKED_XPATHS table for this locale+path */
    private void unlock() {
        String tableName = DBUtils.Table.LOCKED_XPATHS.toString();
        Connection conn = null;
        PreparedStatement ps = null;
        String sql = "DELETE FROM " + tableName + " WHERE locale=?" + " AND xpath=?";
        try {
            conn = DBUtils.getInstance().getDBConnection();
            ps = DBUtils.prepareForwardReadOnly(conn, sql);
            ps.setString(1, localeName);
            ps.setInt(2, xpathId);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            SurveyLog.logException(e);
        } finally {
            DBUtils.close(ps, conn);
        }
    }

    /**
     * Clean slate for TC “permanent” votes when lock or unlock
     *
     * <p>Remove all permanent votes from the VOTE_VALUE table for this locale+path
     */
    private void cleanSlate() {
        String tableName = DBUtils.Table.VOTE_VALUE.toString();
        Connection conn = null;
        PreparedStatement ps = null;
        String sql =
                "DELETE FROM "
                        + tableName
                        + " WHERE locale=?"
                        + " AND xpath=?"
                        + " AND vote_override="
                        + VoteResolver.Level.PERMANENT_VOTES;
        try {
            conn = DBUtils.getInstance().getDBConnection();
            ps = DBUtils.prepareForwardReadOnly(conn, sql);
            ps.setString(1, localeName);
            ps.setInt(2, xpathId);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            SurveyLog.logException(e);
        } finally {
            DBUtils.close(ps, conn);
        }
    }
}
