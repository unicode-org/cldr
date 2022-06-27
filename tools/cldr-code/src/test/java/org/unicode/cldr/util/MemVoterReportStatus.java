package org.unicode.cldr.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Trivial, in-memory implementation of a VoterReportStatus<> and ReportStatusUpdater<>
 */
class MemVoterReportStatus<T extends Comparable<T>> extends VoterReportStatus<T> implements ReportStatusUpdater<T> {
    Map<Pair<T, CLDRLocale>, ReportStatus> data = new HashMap<>();

    @Override
    public ReportStatus getReportStatus(T user, CLDRLocale locale) {
        return data.computeIfAbsent(Pair.of(user, locale), k -> new ReportStatus());
    }

    @Override
    public void markReportComplete(T user, CLDRLocale locale, ReportId r, boolean marked, boolean acceptable) {
        getReportStatus(user, locale).mark(r, marked, acceptable);
    }
}
