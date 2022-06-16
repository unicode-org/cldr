package org.unicode.cldr.util;

public class VoterProgress {

    /**
     * The number of paths for which this user is expected to vote
     * (in this locale, limited by the coverage level)
     */
    private int votablePathCount = 0;

    /**
     * The number of paths for which this user already has voted
     * (in this locale, limited by the coverage level)
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
}
