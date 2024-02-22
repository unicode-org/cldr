//  Copyright 2006-2008 IBM. All rights reserved.

package org.unicode.cldr.web;

import java.util.Map;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.VoteStatus;
import org.unicode.cldr.web.UserRegistry.User;

/**
 * This class represents a particular item that can be voted for, a single "contest" if you will.
 *
 * <p>It is currently a wrapper on the BallotBox interface.
 */
public class Race {
    BallotBox<User> ballotBox;
    String xpath;
    VoteResolver<String> resolver;

    public Race(BallotBox<User> ballotBoxForLocale, String xpath) {
        this.ballotBox = ballotBoxForLocale;
        this.xpath = xpath;
        resolver = ballotBox.getResolver(xpath);
    }

    // All votes for a particular item
    class Chad implements Comparable<Chad> {
        String value;

        @Override
        public String toString() {
            return value;
        }

        public Chad(String s) {
            value = s;
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) return true;
            if (!(other instanceof Chad)) return false;
            return (compareTo((Chad) other) == 0);
        }

        @Override
        public int compareTo(Chad o) {
            if (o == this) {
                return 0;
            }
            return value.compareTo(o.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    /** Get a map of xpath to score for this org. */
    public Map<String, Long> getOrgToVotes(Organization org) {
        return resolver.getOrgToVotes(org);
    }

    /** Get the baseline xpath */
    public String getBaselineValue() {
        return resolver.getBaselineValue();
    }

    /** Get the baseline status */
    public org.unicode.cldr.util.VoteResolver.Status getBaselineStatus() {
        return resolver.getBaselineStatus();
    }

    public String resolverToString() {
        return resolver.toString()
                + "\n"
                + "WinningXpath: "
                + resolver.getWinningValue()
                + "#"
                + resolver.getWinningValue()
                + " "
                + resolver.getWinningStatus()
                + "\n";
    }

    public String getOrgVote(Organization org) {
        return resolver.getOrgVote(org);
    }

    public boolean isOrgDispute(Organization org) {
        return resolver.getConflictedOrganizations().contains(org);
    }

    public VoteStatus getStatusForOrganization(Organization orgOfUser) {
        return resolver.getStatusForOrganization(orgOfUser);
    }
}
