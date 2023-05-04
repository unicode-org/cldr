package org.unicode.cldr.util;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class VoterProgress {

    /**
     * The number of paths for which this user is expected to vote (in this locale, limited by the
     * coverage level)
     */
    private int votablePathCount = 0;

    /**
     * The number of paths for which this user already has voted (in this locale, limited by the
     * coverage level)
     */
    private int votedPathCount = 0;

    /**
     * The number of paths for which this user already has voted, broken down by specific VoteType
     */
    private Map<VoteType, Integer> votedTypeCount = null;

    /*
     * These "get" methods are called automatically by the API to produce json
     */
    public int getVotablePathCount() {
        return votablePathCount;
    }

    public int getVotedPathCount() {
        return votedPathCount;
    }

    public Map<String, Integer> getTypeCount() {
        if (votedTypeCount == null) {
            return null;
        }
        // JSON serialization requires String (not VoteType) for key
        Map<String, Integer> map = new TreeMap();
        for (VoteType voteType : votedTypeCount.keySet()) {
            map.put(voteType.name(), votedTypeCount.get(voteType));
        }
        return map;
    }

    public void incrementVotablePathCount() {
        votablePathCount++;
    }

    public void incrementVotedPathCount(VoteType voteType) {
        if (voteType == null || voteType == VoteType.NONE) {
            throw new IllegalArgumentException("null/NONE not allowed for incrementVotedPathCount");
        }
        votedPathCount++;
        if (votedTypeCount == null) {
            votedTypeCount = new HashMap<>();
        }
        votedTypeCount.put(voteType, votedTypeCount.getOrDefault(voteType, 0) + 1);
    }
}
