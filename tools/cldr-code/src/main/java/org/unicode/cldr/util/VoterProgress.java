package org.unicode.cldr.util;

// TODO: rename this to VettingProgress, since now it is used not only for
// "voter completion" which is user-specific, but also for "locale completion" which isn't
public class VoterProgress {
    /**
     * The number of paths for which this user is expected to vote
     * (in this locale, limited by the coverage level)
     *
     * More generally (as for locale completion), the number of tasks
     * that are expected to be completed
     */
    private int votablePathCount = 0;

    /**
     * The number of paths for which this user already has voted
     * (in this locale, limited by the coverage level)
     *
     * More generally (as for locale completion), the number of tasks
     * that have already been completed (normally <= votablePathCount)
     */
    private int votedPathCount = 0;

    public int getVotablePathCount() {
        return votablePathCount;
    }

    public int getVotedPathCount() {
        return votedPathCount;
    }

    public void incrementVotablePathCount() {
        votablePathCount++;
    }

    public void incrementVotedPathCount() {
        votedPathCount++;
    }

    public int friendlyPercent() {
        if (votablePathCount <= 0) {
            // The task is finished since nothing needed to be done
            // Do not divide by zero (0 / 0 = NaN%)
            return 100;
        }
        if (votedPathCount <= 0) {
            return 0;
        }
        // Do not round 99.9 up to 100
        final int floor = (int) Math.floor((100 * (float) votedPathCount) / (float) votablePathCount);
        if (floor == 0) {
            // Do not round 0.001 down to zero
            // Instead, provide indication of slight progress
            return 1;
        }
        if (floor > 100) {
            return 100;
        }
        return floor;
    }
}
