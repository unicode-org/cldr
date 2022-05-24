package org.unicode.cldr.web;

import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.VoterReportStatus.ReportId;

/**
 * Utilities for managing report status in settings
 */
public class Reports {
    // Timestamp representing not complete
    private static final long NOT_COMPLETE = 0L;

    /**
     * Update the completion status.
     * Error if called with complete=false and acceptable=true
     * @param settings settings to set
     * @param r which report to set
     * @param complete true if user has completed
     * @param acceptable true if values were acceptable, false if user has voted for all acceptable values
     */
    public static void markReportComplete(UserSettings settings, ReportId r, boolean complete, boolean acceptable) {
        if (!complete && acceptable) {
            throw new IllegalArgumentException("Cannot be unmarked and acceptable");
        }
        setCompletion(settings, r, complete);
        setAcceptable(settings, r, acceptable);
    }

    /**
     * Retrieve a set of all acceptable reports (user was found data as-is acceptable)
     */
    public static EnumSet<ReportId> getAcceptability(UserSettings settings) {
        final Set<ReportId> have = new TreeSet<ReportId>();
        for (ReportId r : ReportId.values()) {
            // the 'setting' has the time of completion, but is not needed here.
            if (getAcceptable(settings, r)) {
                have.add(r);
            }
        }
        if (have.isEmpty()) {
            return EnumSet.noneOf(ReportId.class);
        }
        return EnumSet.copyOf(have);
    }

    /**
     * Retrieve a set of all complete reports (user has finished)
     */
    public static EnumSet<ReportId> getCompletion(UserSettings settings) {
        final Set<ReportId> have = new TreeSet<ReportId>();
        for (ReportId r : ReportId.values()) {
            // the 'setting' has the time of completion, but is not needed here.
            if (getCompletionTime(settings, r) > NOT_COMPLETE) {
                have.add(r);
            }
        }
        if (have.isEmpty()) {
            return EnumSet.noneOf(ReportId.class);
        }
        return EnumSet.copyOf(have);
    }

    private static String getSettingsName(ReportId r) {
        return "report_" + r + "_v" + SurveyMain.getNewVersion();
    }

    private static String getSettingsNameAcceptable(ReportId r) {
        return "report_" + r + "_v" + SurveyMain.getNewVersion() + "_acceptable";
    }

    /**
     * Returns the timestamp for when this report was complete
     * @param settings settings to query
     * @param r report to query
     * @return time (as millis) or NOT_COMPLETE
     */
    private static long getCompletionTime(UserSettings settings, ReportId r) {
        return settings.get(getSettingsName(r), NOT_COMPLETE);
    }

    private static boolean getAcceptable(UserSettings settings, ReportId r) {
        return settings.get(getSettingsNameAcceptable(r), false);
    }

    private static void setAcceptable(UserSettings settings, ReportId r, boolean acceptable) {
        settings.set(getSettingsNameAcceptable(r), acceptable);
    }

    private static void setCompletion(UserSettings settings, ReportId r, boolean complete) {
        long timeToSet = complete ? System.currentTimeMillis() : NOT_COMPLETE;
        settings.set(getSettingsName(r), timeToSet);
    }
}
