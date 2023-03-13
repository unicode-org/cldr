package org.unicode.cldr.util;

public enum VoteType {
    UNKNOWN(0), DIRECT(1), AUTO_IMPORT (2) , MANUAL_IMPORT(3), BULK_UPLOAD(4);

    private final int integerId;

    VoteType(int id) {
        this.integerId = id;
    }

    /**
     * Get an integer version of the type, for compact database storage as TINYINT
     *
     * @return the vote type integer id
     */
    public int id() {
        return integerId;
    }
}
