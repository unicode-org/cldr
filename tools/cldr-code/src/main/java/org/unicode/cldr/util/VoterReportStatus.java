package org.unicode.cldr.util;

import java.util.EnumSet;

/**
 * This interface is for objects which can expose information on which reports have been completed.
 */
public interface VoterReportStatus<T> {
    /**
     * Enumeration for the reports. In order.
     */
    public enum ReportId {
        datetime, zones, compact, // aka 'numbers'
    };

    public ReportStatus getReportStatus(T user, CLDRLocale locale);

    public static class ReportStatus {
        public EnumSet<ReportId> completed = EnumSet.noneOf(ReportId.class);
        public EnumSet<ReportId> acceptable = EnumSet.noneOf(ReportId.class);

        public ReportStatus mark(ReportId r, boolean asComplete, boolean asAcceptable) {
            if (!asComplete && asAcceptable) {
                throw new IllegalArgumentException("Cannot be !complete&&acceptable");
            }
            if (asComplete) {
                completed.add(r);
            } else {
                completed.remove(r);
            }
            if (asAcceptable) {
                acceptable.add(r);
            } else {
                acceptable.remove(r);
            }
            return this;
        }
    }
}
