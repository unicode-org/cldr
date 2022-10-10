package org.unicode.cldr.util;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * This interface is for objects which can expose information on which reports have been completed.
 * @param T the type for each voter. Must be an Integer.
 */
public abstract class VoterReportStatus<T> {
    /**
     * Enumeration for the reports. In order.
     */
    public enum ReportId {
        datetime, zones, compact, // aka 'numbers'
    };

    public enum ReportAcceptability {
        notAcceptable, acceptable;

        boolean isAcceptable() {
            return this == acceptable;
        }

        static ReportAcceptability fromPair(boolean isComplete, boolean isAcceptable) {
            if (isAcceptable) {
                return acceptable;
            } else if (isComplete) {
                return notAcceptable;
            } else {
                return null;
            }
        }
    };

    public abstract ReportStatus getReportStatus(T user, CLDRLocale locale);

    public static class ReportStatus {
        public EnumSet<ReportId> completed = EnumSet.noneOf(ReportId.class);
        public EnumSet<ReportId> acceptable = EnumSet.noneOf(ReportId.class);
        public Date date = null;

        public ReportStatus mark(ReportId r, boolean asComplete, boolean asAcceptable) {
            return this.mark(r, asComplete, asAcceptable, null);
        }

        public ReportStatus mark(ReportId r, boolean asComplete, boolean asAcceptable, Date date) {
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
            this.date = date;
            return this;
        }

        public Date getDate() {
            return date;
        }

        /**
         * Return the acceptability enum for this report type
         * @param r report type
         * @return the acceptability enum, or null if there was no entry
         */
        public ReportAcceptability getAcceptability(ReportId r) {
            return ReportAcceptability.fromPair(completed.contains(r), acceptable.contains(r));
        }
    }

    /**
     * Update a Resolver for a particular Report. The resolver will be cleared.
     * Note that T must be an Integer for this to succeed.
     * @param l locale
     * @param r which report
     * @param userList set of users
     * @param status the source
     * @param res which
     * @return vote statistics for each acceptability level
     */
    public Map<ReportAcceptability, Set<Integer>>
    updateResolver(CLDRLocale l, ReportId r,
        Set<T> userList, VoteResolver<ReportAcceptability> res) {
        Map<ReportAcceptability, Set<Integer>> statistics = new HashMap<>();
        res.clear();
        res.setBaileyValue(null);
        // Get the report status for each user
        userList.forEach(id -> {
            // get the report status for this user
            final ReportStatus rs = getReportStatus(id, l);
            // convert the ReportStatus for the specific id, into an enum (or null)
            final ReportAcceptability acc = rs.getAcceptability(r);
            if (acc != null) {
                // if not an abstention, add
                res.add(acc, (Integer) id, null, rs.getDate()); // TODO: Cast because T must be an Integer. Refactor class to not be templatized
                statistics.computeIfAbsent(acc, k -> new HashSet<>())
                    .add((Integer) id);
            }
        });
        return statistics;
    }
}
