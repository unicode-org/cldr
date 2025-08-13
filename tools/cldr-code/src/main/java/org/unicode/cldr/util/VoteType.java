package org.unicode.cldr.util;

/**
 * Enum for types of votes Note: This is used within SurveyTool, so the integer values need to fit
 * in a TINYINT
 */
public enum VoteType {
    /** The user did not vote */
    NONE(-1),

    /** The user voted but we don't know exactly how */
    UNKNOWN(0),

    /** The user voted directly in the Survey Tool interface, the usual way */
    DIRECT(1),

    /** The user's (winning) vote from an earlier version was automatically imported */
    AUTO_IMPORT(2),

    /**
     * The user manually imported their vote from an earlier version -- Currently such votes are
     * losing (otherwise they would be auto-imported), and they are imported "anonymously" (not in
     * the name of the actual user) with weight zero.
     */
    MANUAL_IMPORT(3),

    /** The user submitted their vote using the bulk xml upload method */
    BULK_UPLOAD(4),

    /** special TC vote for 'missing' */
    VOTE_FOR_MISSING(100);

    private final int integerId;

    VoteType(int id) {
        this.integerId = id;
    }

    public static VoteType fromId(int id) {
        for (VoteType voteType : values()) {
            if (voteType.integerId == id) {
                return voteType;
            }
        }
        return NONE;
    }

    /**
     * Get an integer version of the type, for compact database storage as TINYINT
     *
     * @return the vote type integer id
     */
    public int id() {
        return integerId;
    }

    public boolean isImported() {
        return this == AUTO_IMPORT || this == MANUAL_IMPORT;
    }
}
