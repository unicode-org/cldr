package org.unicode.cldr.unittest;

public class VoteEntries {

    int voter;
    String value;

    public VoteEntries(int voter, String value) {
        this.voter = voter;
        this.value = value;
    }

    public int getVoter() {
        return voter;
    }

    public String getValue() {
        return value;
    }

}
