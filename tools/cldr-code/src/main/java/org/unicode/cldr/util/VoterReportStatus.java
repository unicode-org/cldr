package org.unicode.cldr.util;

import java.util.EnumSet;

/**
 * This interface is for objects which can expose information on which reports have been completed.
 */
public interface VoterReportStatus {
    /**
     * Enumeration for the reports. In order.
     */
    public enum ReportId {
        datetime,
        zones,
        compact,  // aka 'numbers'
    };

    /**
     * Return a set of which reports the user has completed,
     * either as all acceptable or with votes entered.
     */
    public EnumSet<ReportId> getCompletedReports();

    /**
     * Return a set of which reports the user has marked as acceptable,
     * implies that the user has completed the report
     */
    public EnumSet<ReportId> getAcceptableReports();
}
