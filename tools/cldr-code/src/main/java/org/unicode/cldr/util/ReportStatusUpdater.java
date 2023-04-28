package org.unicode.cldr.util;

/** Interface for an object that can update report status */
public interface ReportStatusUpdater<T> {
    /** Update report status */
    public void markReportComplete(
            T user,
            CLDRLocale locale,
            VoterReportStatus.ReportId r,
            boolean marked,
            boolean acceptable);
}
